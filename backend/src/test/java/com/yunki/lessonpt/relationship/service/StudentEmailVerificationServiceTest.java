package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.mail.EmailDeliveryException;
import com.yunki.lessonpt.relationship.mail.EmailSender;
import com.yunki.lessonpt.relationship.mail.StudentOtpPurpose;
import com.yunki.lessonpt.relationship.domain.StudentEmailVerification;
import com.yunki.lessonpt.relationship.domain.StudentEmailVerificationStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;
import com.yunki.lessonpt.relationship.mapper.StudentEmailVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;

@ExtendWith(MockitoExtension.class)
class StudentEmailVerificationServiceTest {

    private static final Instant START = Instant.parse("2026-09-24T10:00:00Z");

    @Mock
    private TeacherStudentAccessMapper teacherStudentAccessMapper;

    @Mock
    private TeacherStudentMapper teacherStudentMapper;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private StudentEmailVerificationMapper studentEmailVerificationMapper;

    @Mock
    private OtpGenerator otpGenerator;

    @Mock
    private StudentAccessSessionService studentAccessSessionService;

    @Mock
    private EmailSender emailSender;

    private final TokenHasher tokenHasher = new TokenHasher();

    private StudentEmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(START);
    }

    @Test
    void issueStoresHashesAfterLockingAccess() {
        stubActiveChain("student@lessonpt.local");
        when(otpGenerator.generate()).thenReturn("123456");
        when(studentEmailVerificationMapper.insertStudentEmailVerification(any())).thenReturn(1);

        service.issue("public-key", "  Student@LessonPT.local ");

        ArgumentCaptor<StudentEmailVerification> captor = ArgumentCaptor.forClass(StudentEmailVerification.class);
        InOrder order = inOrder(teacherStudentAccessMapper, teacherStudentMapper, studentMapper, studentEmailVerificationMapper);
        order.verify(teacherStudentAccessMapper).selectByPublicAccessKey("public-key");
        order.verify(teacherStudentAccessMapper).lockTeacherStudentAccessById(90L);
        order.verify(teacherStudentMapper).selectTeacherStudentById(72L);
        order.verify(studentMapper).selectStudentById(41L);
        order.verify(studentEmailVerificationMapper).selectLatestByAccessId(90L);
        order.verify(studentEmailVerificationMapper).invalidatePendingByAccessId(any());
        order.verify(studentEmailVerificationMapper).insertStudentEmailVerification(captor.capture());
        StudentEmailVerification stored = captor.getValue();
        assertThat(stored.getCodeHash()).isEqualTo(tokenHasher.hash("123456")).isNotEqualTo("123456");
        assertThat(stored.getEmailHash()).isEqualTo(tokenHasher.hash("student@lessonpt.local"))
                .isNotEqualTo("  Student@LessonPT.local ");
        assertThat(stored.getVerificationStatus()).isEqualTo(StudentEmailVerificationStatus.PENDING);
        assertThat(stored.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 9, 24, 10, 10));
        assertThat(stored.getFailedAttemptCount()).isZero();
        verify(emailSender).sendStudentOtp("student@lessonpt.local", "123456", StudentOtpPurpose.INVITATION_ACCESS);
    }

    @Test
    void issueDoesNotSucceedWhenMailDeliveryFails() {
        stubActiveChain("student@lessonpt.local");
        when(otpGenerator.generate()).thenReturn("123456");
        when(studentEmailVerificationMapper.insertStudentEmailVerification(any())).thenReturn(1);
        doThrow(new EmailDeliveryException()).when(emailSender)
                .sendStudentOtp("student@lessonpt.local", "123456", StudentOtpPurpose.INVITATION_ACCESS);

        assertThatThrownBy(() -> service.issue("public-key", "student@lessonpt.local"))
                .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    void issueHidesMissingRevokedInactiveAndMismatchedEmail() {
        assertThatThrownBy(() -> service.issue("missing", "student@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);

        TeacherStudentAccess revoked = access();
        revoked.setStatus(RecordStatus.INACTIVE);
        revoked.setRevokedAt(LocalDateTime.of(2026, 9, 24, 9, 0));
        when(teacherStudentAccessMapper.selectByPublicAccessKey("revoked")).thenReturn(null);
        assertThatThrownBy(() -> service.issue("revoked", "student@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);

        stubActiveChain("student@lessonpt.local");
        TeacherStudent inactiveRelation = relation();
        inactiveRelation.setStatus(RecordStatus.INACTIVE);
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(inactiveRelation);
        assertThatThrownBy(() -> service.issue("public-key", "student@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);

        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(relation());
        Student inactiveStudent = student("student@lessonpt.local");
        inactiveStudent.setStatus(RecordStatus.INACTIVE);
        when(studentMapper.selectStudentById(41L)).thenReturn(inactiveStudent);
        assertThatThrownBy(() -> service.issue("public-key", "student@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);

        when(studentMapper.selectStudentById(41L)).thenReturn(student(null));
        assertThatThrownBy(() -> service.issue("public-key", "student@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);

        when(studentMapper.selectStudentById(41L)).thenReturn(student("student@lessonpt.local"));
        assertThatThrownBy(() -> service.issue("public-key", "other@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(studentEmailVerificationMapper, never()).insertStudentEmailVerification(any());
    }

    @Test
    void resendRejectsBeforeSixtySecondsAndReplacesPendingAfter() {
        stubActiveChain("student@lessonpt.local");
        when(studentEmailVerificationMapper.selectLatestByAccessId(90L)).thenReturn(pending("hash", 0));
        assertThatThrownBy(() -> serviceAt(START.plusSeconds(59)).issue("public-key", "student@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        verify(studentEmailVerificationMapper, never()).invalidatePendingByAccessId(any());

        when(otpGenerator.generate()).thenReturn("654321");
        when(studentEmailVerificationMapper.insertStudentEmailVerification(any())).thenReturn(1);
        serviceAt(START.plusSeconds(60)).issue("public-key", "student@lessonpt.local");
        verify(studentEmailVerificationMapper).invalidatePendingByAccessId(any());
        verify(studentEmailVerificationMapper).insertStudentEmailVerification(any());
    }

    @Test
    void verifyConsumesMatchingOtpAndRejectsReuseExpiryAndWrongCode() {
        stubActiveChain("student@lessonpt.local");
        StudentEmailVerification pending = pending(tokenHasher.hash("123456"), 0);
        pending.setEmailHash(tokenHasher.hash("student@lessonpt.local"));
        when(studentEmailVerificationMapper.selectCurrentPendingByAccessId(90L)).thenReturn(pending);
        when(studentEmailVerificationMapper.markConsumed(pending)).thenReturn(1);

        service.verify("public-key", "student@lessonpt.local", "123456");
        assertThat(pending.getConsumedAt()).isEqualTo(LocalDateTime.of(2026, 9, 24, 10, 0));
        verify(studentEmailVerificationMapper).markConsumed(pending);
        verify(studentAccessSessionService).openAfterOtp(90L);

        when(studentEmailVerificationMapper.selectCurrentPendingByAccessId(90L)).thenReturn(null);
        assertThatThrownBy(() -> service.verify("public-key", "student@lessonpt.local", "123456"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        verify(studentEmailVerificationMapper, never()).insertStudentEmailVerification(any());
    }

    @Test
    void verifyRejectsExpiredOtpWithoutIncreasingFailures() {
        stubActiveChain("student@lessonpt.local");
        StudentEmailVerification pending = pending(tokenHasher.hash("123456"), 0);
        pending.setExpiresAt(LocalDateTime.of(2026, 9, 24, 10, 10));
        pending.setEmailHash(tokenHasher.hash("student@lessonpt.local"));
        when(studentEmailVerificationMapper.selectCurrentPendingByAccessId(90L)).thenReturn(pending);
        when(studentEmailVerificationMapper.markConsumed(pending)).thenReturn(1);

        serviceAt(START.plusSeconds(9 * 60 + 59)).verify("public-key", "student@lessonpt.local", "123456");
        assertThatThrownBy(() -> serviceAt(START.plusSeconds(600)).verify("public-key", "student@lessonpt.local", "123456"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        verify(studentEmailVerificationMapper, never()).incrementFailureCount(any());
        verify(studentEmailVerificationMapper, never()).markLocked(any());
    }

    @Test
    void fifthFailureLocksAndBlocksIssueUntilLockEnds() {
        stubActiveChain("student@lessonpt.local");
        StudentEmailVerification pending = pending(tokenHasher.hash("123456"), 4);
        pending.setEmailHash(tokenHasher.hash("student@lessonpt.local"));
        when(studentEmailVerificationMapper.selectCurrentPendingByAccessId(90L)).thenReturn(pending);
        when(studentEmailVerificationMapper.markLocked(pending)).thenReturn(1);

        assertThatThrownBy(() -> service.verify("public-key", "student@lessonpt.local", "000000"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        assertThat(pending.getLockedUntil()).isEqualTo(LocalDateTime.of(2026, 9, 24, 10, 10));

        StudentEmailVerification locked = pending(tokenHasher.hash("123456"), 5);
        locked.setVerificationStatus(StudentEmailVerificationStatus.LOCKED);
        locked.setLockedUntil(LocalDateTime.of(2026, 9, 24, 10, 10));
        when(studentEmailVerificationMapper.selectLatestByAccessId(90L)).thenReturn(locked);
        assertThatThrownBy(() -> serviceAt(START.plusSeconds(599))
                .issue("public-key", "student@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);

        when(otpGenerator.generate()).thenReturn("111111");
        when(studentEmailVerificationMapper.insertStudentEmailVerification(any())).thenReturn(1);
        serviceAt(START.plusSeconds(600)).issue("public-key", "student@lessonpt.local");
        verify(studentEmailVerificationMapper).insertStudentEmailVerification(any());
    }

    @Test
    void wrongOtpIncrementsFailureCount() {
        stubActiveChain("student@lessonpt.local");
        StudentEmailVerification pending = pending(tokenHasher.hash("123456"), 1);
        pending.setEmailHash(tokenHasher.hash("student@lessonpt.local"));
        when(studentEmailVerificationMapper.selectCurrentPendingByAccessId(90L)).thenReturn(pending);
        when(studentEmailVerificationMapper.incrementFailureCount(pending)).thenReturn(1);

        assertThatThrownBy(() -> service.verify("public-key", "student@lessonpt.local", "000000"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
        assertThat(pending.getFailedAttemptCount()).isEqualTo(2);
        verify(studentEmailVerificationMapper).incrementFailureCount(pending);
        verify(studentEmailVerificationMapper, never()).markConsumed(any());
    }

    @Test
    void issueMapsLockTimeoutToConflict() {
        when(teacherStudentAccessMapper.selectByPublicAccessKey("public-key")).thenReturn(access());
        doThrow(new CannotAcquireLockException("ORA-30006"))
                .when(teacherStudentAccessMapper).lockTeacherStudentAccessById(90L);

        assertThatThrownBy(() -> service.issue("public-key", "student@lessonpt.local"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        verify(studentEmailVerificationMapper, never()).insertStudentEmailVerification(any());
    }

    private StudentEmailVerificationService serviceAt(Instant instant) {
        return new StudentEmailVerificationService(
                teacherStudentAccessMapper,
                teacherStudentMapper,
                studentMapper,
                studentEmailVerificationMapper,
                otpGenerator,
                tokenHasher,
                Clock.fixed(instant, ZoneOffset.UTC),
                studentAccessSessionService,
                emailSender);
    }

    private void stubActiveChain(String email) {
        when(teacherStudentAccessMapper.selectByPublicAccessKey("public-key")).thenReturn(access());
        when(teacherStudentAccessMapper.lockTeacherStudentAccessById(90L)).thenReturn(access());
        when(teacherStudentMapper.selectTeacherStudentById(72L)).thenReturn(relation());
        when(studentMapper.selectStudentById(41L)).thenReturn(student(email));
    }

    private TeacherStudentAccess access() {
        TeacherStudentAccess access = new TeacherStudentAccess();
        access.setTeacherStudentAccessId(90L);
        access.setTeacherStudentId(72L);
        access.setPublicAccessKey("public-key");
        access.setStatus(RecordStatus.ACTIVE);
        return access;
    }

    private TeacherStudent relation() {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherStudentId(72L);
        relation.setStudentId(41L);
        relation.setStatus(RecordStatus.ACTIVE);
        return relation;
    }

    private Student student(String email) {
        Student student = new Student();
        student.setStudentId(41L);
        student.setEmail(email);
        student.setStatus(RecordStatus.ACTIVE);
        return student;
    }

    private StudentEmailVerification pending(String codeHash, int failures) {
        StudentEmailVerification verification = new StudentEmailVerification();
        verification.setVerificationId(7L);
        verification.setTeacherStudentAccessId(90L);
        verification.setCodeHash(codeHash);
        verification.setVerificationStatus(StudentEmailVerificationStatus.PENDING);
        verification.setFailedAttemptCount(failures);
        verification.setCreatedAt(LocalDateTime.of(2026, 9, 24, 10, 0));
        verification.setExpiresAt(LocalDateTime.of(2026, 9, 24, 10, 10));
        return verification;
    }
}
