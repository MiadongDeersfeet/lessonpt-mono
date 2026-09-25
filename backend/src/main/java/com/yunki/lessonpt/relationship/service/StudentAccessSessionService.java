package com.yunki.lessonpt.relationship.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.StudentAccessSession;
import com.yunki.lessonpt.relationship.domain.StudentAccessSessionStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;
import com.yunki.lessonpt.relationship.mapper.StudentAccessSessionMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentAccessSessionService {

    static final Duration IDLE = Duration.ofDays(30);
    static final Duration ABSOLUTE = Duration.ofDays(180);
    static final Duration RENEW_WITHIN = Duration.ofDays(7);
    static final Duration LAST_ACCESS_INTERVAL = Duration.ofHours(24);
    private final StudentAccessSessionMapper studentAccessSessionMapper;
    private final TeacherStudentAccessMapper teacherStudentAccessMapper;
    private final TeacherStudentMapper teacherStudentMapper;
    private final StudentMapper studentMapper;
    private final StudentSessionTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    /**
     * 같은 access의 활성 세션을 모두 폐기하고 새 세션 하나만 만든다.
     * access 행 잠금이 이미 잡혀 있어야 한다.
     */
    public IssuedStudentSession openAfterOtp(Long teacherStudentAccessId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long studentId = requireStudentId(teacherStudentAccessId);
        studentAccessSessionMapper.revokeActiveByAccessId(teacherStudentAccessId, now, now);
        String rawToken = tokenGenerator.generate();
        StudentAccessSession created = newSession(studentId, teacherStudentAccessId, rawToken, now);
        try {
            if (studentAccessSessionMapper.insertStudentAccessSession(created) != 1
                    || teacherStudentAccessMapper.updateLastVerifiedAt(teacherStudentAccessId, now) != 1) {
                throw new BusinessException(ErrorCode.COMMON_CONFLICT);
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        return new IssuedStudentSession(rawToken, created.getExpiresAt(), created.getAbsoluteExpiresAt());
    }

    /**
     * 학생만 확정된 세션이다. 수업 범위는 아직 고르지 않는다.
     */
    @Transactional
    public IssuedStudentSession openForStudent(Long studentId) {
        LocalDateTime now = LocalDateTime.now(clock);
        String rawToken = tokenGenerator.generate();
        StudentAccessSession created = newSession(studentId, null, rawToken, now);
        try {
            if (studentAccessSessionMapper.insertStudentAccessSession(created) != 1) {
                throw new BusinessException(ErrorCode.COMMON_CONFLICT);
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        return new IssuedStudentSession(rawToken, created.getExpiresAt(), created.getAbsoluteExpiresAt());
    }

    @Transactional
    public void selectScope(String rawToken, Long teacherStudentAccessId) {
        StudentAccessSession session = requireUsableSession(rawToken);
        if (!ownsAccess(session.getStudentId(), teacherStudentAccessId)) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        session.setTeacherStudentAccessId(teacherStudentAccessId);
        session.setUpdatedAt(LocalDateTime.now(clock));
        if (studentAccessSessionMapper.updateSelectedAccess(session) != 1) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    @Transactional
    public void clearScope(String rawToken) {
        StudentAccessSession session = requireUsableSession(rawToken);
        session.setTeacherStudentAccessId(null);
        session.setUpdatedAt(LocalDateTime.now(clock));
        if (studentAccessSessionMapper.updateSelectedAccess(session) != 1) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    @Transactional
    public void revokeActiveByAccessId(Long teacherStudentAccessId) {
        LocalDateTime now = LocalDateTime.now(clock);
        studentAccessSessionMapper.revokeActiveByAccessId(teacherStudentAccessId, now, now);
    }

    @Transactional
    public void revokeActiveByTeacherStudentId(Long teacherStudentId) {
        LocalDateTime now = LocalDateTime.now(clock);
        studentAccessSessionMapper.revokeActiveByTeacherStudentId(teacherStudentId, now, now);
    }

    @Transactional
    public void revokeActiveByStudentId(Long studentId) {
        LocalDateTime now = LocalDateTime.now(clock);
        studentAccessSessionMapper.revokeActiveByStudentId(studentId, now, now);
    }

    /**
     * 쿠키 토큰이 유효하면 principal을 반환하고, 필요할 때만 만료와 마지막 접근을 한 번에 갱신한다.
     */
    @Transactional
    public Optional<StudentPrincipal> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        StudentAccessSession session = studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(rawToken));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!usable(session, now) || session.getStudentId() == null) {
            return Optional.empty();
        }
        Student student = studentMapper.selectActiveStudentById(session.getStudentId());
        if (student == null) {
            return Optional.empty();
        }
        if (session.getTeacherStudentAccessId() == null) {
            applySliding(session, now);
            return Optional.of(new StudentPrincipal(null, null, student.getStudentId()));
        }
        TeacherStudentAccess access = teacherStudentAccessMapper.lockTeacherStudentAccessById(
                session.getTeacherStudentAccessId());
        if (access == null
                || access.getStatus() != RecordStatus.ACTIVE
                || access.getRevokedAt() != null) {
            return Optional.empty();
        }
        TeacherStudent relation = teacherStudentMapper.selectTeacherStudentById(access.getTeacherStudentId());
        if (relation == null
                || relation.getStatus() != RecordStatus.ACTIVE
                || relation.getDeletedAt() != null
                || !session.getStudentId().equals(relation.getStudentId())) {
            return Optional.empty();
        }
        applySliding(session, now);
        return Optional.of(new StudentPrincipal(
                access.getTeacherStudentAccessId(), relation.getTeacherStudentId(), student.getStudentId()));
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        StudentAccessSession session = studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(rawToken));
        if (session == null || session.getSessionStatus() != StudentAccessSessionStatus.ACTIVE) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        studentAccessSessionMapper.revokeBySessionId(session.getStudentAccessSessionId(), now, now);
    }

    private boolean usable(StudentAccessSession session, LocalDateTime now) {
        return session != null
                && session.getSessionStatus() == StudentAccessSessionStatus.ACTIVE
                && session.getRevokedAt() == null
                && session.getExpiresAt() != null
                && session.getAbsoluteExpiresAt() != null
                && session.getExpiresAt().isAfter(now)
                && session.getAbsoluteExpiresAt().isAfter(now);
    }

    private void applySliding(StudentAccessSession session, LocalDateTime now) {
        boolean extend = !session.getExpiresAt().isAfter(now.plus(RENEW_WITHIN));
        boolean touch = session.getLastAccessedAt() == null
                || !session.getLastAccessedAt().plus(LAST_ACCESS_INTERVAL).isAfter(now);
        if (!extend && !touch) {
            return;
        }
        if (extend) {
            LocalDateTime idle = now.plus(IDLE);
            session.setExpiresAt(idle.isAfter(session.getAbsoluteExpiresAt())
                    ? session.getAbsoluteExpiresAt()
                    : idle);
        }
        if (touch) {
            session.setLastAccessedAt(now);
        }
        session.setUpdatedAt(now);
        studentAccessSessionMapper.updateSlidingWindow(session);
    }

    private Long requireStudentId(Long teacherStudentAccessId) {
        TeacherStudentAccess access = teacherStudentAccessMapper.lockTeacherStudentAccessById(teacherStudentAccessId);
        if (access == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        TeacherStudent relation = teacherStudentMapper.selectTeacherStudentById(access.getTeacherStudentId());
        if (relation == null || relation.getStudentId() == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return relation.getStudentId();
    }

    private boolean ownsAccess(Long studentId, Long teacherStudentAccessId) {
        TeacherStudentAccess access = teacherStudentAccessMapper.lockTeacherStudentAccessById(teacherStudentAccessId);
        if (access == null || access.getStatus() != RecordStatus.ACTIVE || access.getRevokedAt() != null) {
            return false;
        }
        TeacherStudent relation = teacherStudentMapper.selectTeacherStudentById(access.getTeacherStudentId());
        if (relation == null || relation.getStatus() != RecordStatus.ACTIVE || relation.getDeletedAt() != null) {
            return false;
        }
        Student student = studentMapper.selectActiveStudentById(relation.getStudentId());
        return student != null && studentId.equals(student.getStudentId());
    }

    private StudentAccessSession requireUsableSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        StudentAccessSession session = studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(rawToken));
        if (!usable(session, LocalDateTime.now(clock)) || session.getStudentId() == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        return session;
    }

    private StudentAccessSession newSession(Long studentId, Long accessId, String rawToken, LocalDateTime now) {
        LocalDateTime absolute = now.plus(ABSOLUTE);
        LocalDateTime idle = now.plus(IDLE);
        StudentAccessSession created = new StudentAccessSession();
        created.setStudentId(studentId);
        created.setTeacherStudentAccessId(accessId);
        created.setSessionTokenHash(tokenHasher.hash(rawToken));
        created.setSessionStatus(StudentAccessSessionStatus.ACTIVE);
        created.setExpiresAt(idle.isAfter(absolute) ? absolute : idle);
        created.setAbsoluteExpiresAt(absolute);
        created.setLastAccessedAt(now);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        return created;
    }
}
