package com.yunki.lessonpt.relationship.service;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.StudentEmailVerificationStatus;
import com.yunki.lessonpt.relationship.domain.StudentLoginVerification;
import com.yunki.lessonpt.relationship.mapper.StudentLoginVerificationMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentLoginVerificationService {

    static final int OTP_TTL_MINUTES = 10;
    static final int RESEND_COOLDOWN_SECONDS = 60;
    static final int MAX_FAILURES = 5;
    static final int LOCK_MINUTES = 10;

    private final StudentMapper studentMapper;
    private final StudentPortalMapper studentPortalMapper;
    private final StudentLoginVerificationMapper studentLoginVerificationMapper;
    private final OtpGenerator otpGenerator;
    private final TokenHasher tokenHasher;
    private final Clock clock;
    private final StudentAccessSessionService studentAccessSessionService;

    @Transactional(noRollbackFor = BusinessException.class)
    public void issue(String email) {
        Student student = requireAccessibleStudent(email);
        LocalDateTime now = LocalDateTime.now(clock);
        StudentLoginVerification latest = studentLoginVerificationMapper.selectLatestByStudentId(student.getStudentId());
        if (latest != null && isWithinCooldown(latest, now)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        if (latest != null && isIssueLocked(latest, now)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        invalidatePending(student.getStudentId(), now);
        StudentLoginVerification created = new StudentLoginVerification();
        created.setStudentId(student.getStudentId());
        created.setEmailHash(tokenHasher.hash(normalizeEmail(email)));
        created.setCodeHash(tokenHasher.hash(otpGenerator.generate()));
        created.setVerificationStatus(StudentEmailVerificationStatus.PENDING);
        created.setFailedAttemptCount(0);
        created.setCreatedAt(now);
        created.setExpiresAt(now.plusMinutes(OTP_TTL_MINUTES));
        created.setUpdatedAt(now);
        expectOne(studentLoginVerificationMapper.insertStudentLoginVerification(created));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public IssuedStudentSession verify(String email, String otp) {
        Student student = requireAccessibleStudent(email);
        LocalDateTime now = LocalDateTime.now(clock);
        StudentLoginVerification latest = studentLoginVerificationMapper.selectLatestByStudentId(student.getStudentId());
        if (latest != null && isIssueLocked(latest, now)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        StudentLoginVerification pending = studentLoginVerificationMapper.selectCurrentPendingByStudentId(student.getStudentId());
        if (pending == null || !pending.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        String normalized = normalizeEmail(email);
        if (!hashesMatch(tokenHasher.hash(normalized), pending.getEmailHash())
                || !hashesMatch(tokenHasher.hash(otp), pending.getCodeHash())) {
            registerFailure(pending, now);
        }
        pending.setConsumedAt(now);
        pending.setUpdatedAt(now);
        expectOne(studentLoginVerificationMapper.markConsumed(pending));
        return studentAccessSessionService.openForStudent(student.getStudentId());
    }

    private Student requireAccessibleStudent(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        Student student = studentMapper.selectStudentByEmail(normalized);
        if (student == null || student.getStatus() != RecordStatus.ACTIVE || student.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (studentPortalMapper.countActiveAccesses(student.getStudentId()) < 1) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return student;
    }

    private void invalidatePending(Long studentId, LocalDateTime now) {
        StudentLoginVerification change = new StudentLoginVerification();
        change.setStudentId(studentId);
        change.setInvalidatedAt(now);
        change.setUpdatedAt(now);
        studentLoginVerificationMapper.invalidatePendingByStudentId(change);
    }

    private void registerFailure(StudentLoginVerification pending, LocalDateTime now) {
        int next = pending.getFailedAttemptCount() + 1;
        pending.setUpdatedAt(now);
        if (next >= MAX_FAILURES) {
            pending.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            expectOne(studentLoginVerificationMapper.markLocked(pending));
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        pending.setFailedAttemptCount(next);
        expectOne(studentLoginVerificationMapper.incrementFailureCount(pending));
        throw new BusinessException(ErrorCode.AUTH_FAILED);
    }

    private boolean isWithinCooldown(StudentLoginVerification latest, LocalDateTime now) {
        return latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now);
    }

    private boolean isIssueLocked(StudentLoginVerification latest, LocalDateTime now) {
        return latest.getVerificationStatus() == StudentEmailVerificationStatus.LOCKED
                && latest.getLockedUntil() != null
                && latest.getLockedUntil().isAfter(now);
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

    private boolean hashesMatch(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(actual.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
