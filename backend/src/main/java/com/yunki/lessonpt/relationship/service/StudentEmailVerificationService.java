package com.yunki.lessonpt.relationship.service;

import java.security.MessageDigest;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.StudentEmailVerification;
import com.yunki.lessonpt.relationship.domain.StudentEmailVerificationStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;
import com.yunki.lessonpt.relationship.mapper.StudentEmailVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentEmailVerificationService {

    static final int OTP_TTL_MINUTES = 10;
    static final int RESEND_COOLDOWN_SECONDS = 60;
    static final int MAX_FAILURES = 5;
    static final int LOCK_MINUTES = 10;

    private final TeacherStudentAccessMapper teacherStudentAccessMapper;
    private final TeacherStudentMapper teacherStudentMapper;
    private final StudentMapper studentMapper;
    private final StudentEmailVerificationMapper studentEmailVerificationMapper;
    private final OtpGenerator otpGenerator;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    @Transactional(noRollbackFor = BusinessException.class)
    public void issue(String publicAccessKey, String email) {
        Owned owned = lockedActiveAccess(publicAccessKey);
        String normalizedEmail = requireMatchingEmail(owned.relation(), email);
        TeacherStudentAccess access = owned.access();
        LocalDateTime now = LocalDateTime.now(clock);
        StudentEmailVerification latest = studentEmailVerificationMapper.selectLatestByAccessId(
                access.getTeacherStudentAccessId());
        if (latest != null && isWithinCooldown(latest, now)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        if (latest != null && isIssueLocked(latest, now)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        invalidatePending(access.getTeacherStudentAccessId(), now);
        StudentEmailVerification created = new StudentEmailVerification();
        created.setTeacherStudentAccessId(access.getTeacherStudentAccessId());
        created.setEmailHash(tokenHasher.hash(normalizedEmail));
        created.setCodeHash(tokenHasher.hash(otpGenerator.generate()));
        created.setVerificationStatus(StudentEmailVerificationStatus.PENDING);
        created.setFailedAttemptCount(0);
        created.setCreatedAt(now);
        created.setExpiresAt(now.plusMinutes(OTP_TTL_MINUTES));
        created.setUpdatedAt(now);
        expectOne(studentEmailVerificationMapper.insertStudentEmailVerification(created));
    }

    /**
     * OTP가 맞으면 그 검증 행을 소비한다.
     * 조회 세션은 만들지 않는다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(String publicAccessKey, String email, String otp) {
        Owned owned = lockedActiveAccess(publicAccessKey);
        String normalizedEmail = requireMatchingEmail(owned.relation(), email);
        TeacherStudentAccess access = owned.access();
        LocalDateTime now = LocalDateTime.now(clock);
        StudentEmailVerification latest = studentEmailVerificationMapper.selectLatestByAccessId(
                access.getTeacherStudentAccessId());
        if (latest != null && isIssueLocked(latest, now)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        StudentEmailVerification pending = studentEmailVerificationMapper.selectCurrentPendingByAccessId(
                access.getTeacherStudentAccessId());
        if (pending == null || !pending.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        if (!hashesMatch(tokenHasher.hash(normalizedEmail), pending.getEmailHash())
                || !hashesMatch(tokenHasher.hash(otp), pending.getCodeHash())) {
            registerFailure(pending, now);
        }
        pending.setConsumedAt(now);
        pending.setUpdatedAt(now);
        expectOne(studentEmailVerificationMapper.markConsumed(pending));
    }

    private Owned lockedActiveAccess(String publicAccessKey) {
        TeacherStudentAccess found = teacherStudentAccessMapper.selectByPublicAccessKey(publicAccessKey);
        if (found == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        TeacherStudentAccess locked = lockAccess(found.getTeacherStudentAccessId());
        if (locked.getStatus() != RecordStatus.ACTIVE || locked.getRevokedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        TeacherStudent relation = teacherStudentMapper.selectTeacherStudentById(locked.getTeacherStudentId());
        if (relation == null
                || relation.getStatus() != RecordStatus.ACTIVE
                || relation.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return new Owned(locked, relation);
    }

    private String requireMatchingEmail(TeacherStudent relation, String email) {
        Student student = studentMapper.selectStudentById(relation.getStudentId());
        if (student == null || student.getStatus() != RecordStatus.ACTIVE || student.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        String stored = normalizeEmail(student.getEmail());
        String submitted = normalizeEmail(email);
        if (stored == null || submitted == null || !stored.equals(submitted)) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return stored;
    }

    private void invalidatePending(Long teacherStudentAccessId, LocalDateTime now) {
        StudentEmailVerification change = new StudentEmailVerification();
        change.setTeacherStudentAccessId(teacherStudentAccessId);
        change.setInvalidatedAt(now);
        change.setUpdatedAt(now);
        studentEmailVerificationMapper.invalidatePendingByAccessId(change);
    }

    private void registerFailure(StudentEmailVerification pending, LocalDateTime now) {
        int next = pending.getFailedAttemptCount() + 1;
        pending.setUpdatedAt(now);
        if (next >= MAX_FAILURES) {
            pending.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            expectOne(studentEmailVerificationMapper.markLocked(pending));
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        pending.setFailedAttemptCount(next);
        expectOne(studentEmailVerificationMapper.incrementFailureCount(pending));
        throw new BusinessException(ErrorCode.AUTH_FAILED);
    }

    private boolean isWithinCooldown(StudentEmailVerification latest, LocalDateTime now) {
        return latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now);
    }

    private boolean isIssueLocked(StudentEmailVerification latest, LocalDateTime now) {
        return latest.getVerificationStatus() == StudentEmailVerificationStatus.LOCKED
                && latest.getLockedUntil() != null
                && latest.getLockedUntil().isAfter(now);
    }

    private TeacherStudentAccess lockAccess(Long teacherStudentAccessId) {
        try {
            TeacherStudentAccess locked = teacherStudentAccessMapper.lockTeacherStudentAccessById(teacherStudentAccessId);
            if (locked == null) {
                throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
            }
            return locked;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.COMMON_CONFLICT);
            }
            throw exception;
        }
    }

    private boolean isLockTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CannotAcquireLockException) {
                return true;
            }
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == 30006) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("ORA-30006")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void expectOne(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private record Owned(TeacherStudentAccess access, TeacherStudent relation) {
    }

    private boolean hashesMatch(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(actual.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
