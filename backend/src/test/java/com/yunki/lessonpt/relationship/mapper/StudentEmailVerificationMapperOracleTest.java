package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.StudentEmailVerification;
import com.yunki.lessonpt.relationship.domain.StudentEmailVerificationStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;
import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.relationship.domain.StudentAccessSession;
import com.yunki.lessonpt.relationship.domain.StudentAccessSessionStatus;
import com.yunki.lessonpt.relationship.service.IssuedStudentSession;
import com.yunki.lessonpt.relationship.service.OtpGenerator;
import com.yunki.lessonpt.relationship.service.StudentAccessSessionService;
import com.yunki.lessonpt.relationship.service.StudentEmailVerificationService;
import com.yunki.lessonpt.relationship.service.TeacherStudentAccessService;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.dto.StudentUpdateRequest;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.student.service.StudentService;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class StudentEmailVerificationMapperOracleTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-24T10:00:00Z"));

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private OtpGenerator otpGenerator;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherStudentMapper teacherStudentMapper;

    @Autowired
    private TeacherStudentAccessMapper teacherStudentAccessMapper;

    @Autowired
    private StudentEmailVerificationMapper studentEmailVerificationMapper;

    @Autowired
    private TeacherStudentAccessService teacherStudentAccessService;

    @Autowired
    private StudentEmailVerificationService studentEmailVerificationService;

    @Autowired
    private StudentAccessSessionMapper studentAccessSessionMapper;

    @Autowired
    private StudentAccessSessionService studentAccessSessionService;

    @Autowired
    private StudentService studentService;

    private final TokenHasher tokenHasher = new TokenHasher();

    @BeforeEach
    void fixClockAndOtp() {
        when(clock.instant()).thenAnswer(invocation -> now.get());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(otpGenerator.generate()).thenReturn("123456");
    }

    @Test
    void issuesResendsLocksAndConsumesWithoutAffectingAnotherAccess() {
        Teacher teacher = teacher();
        teacherMapper.insertTeacher(teacher);
        Student student = studentWithEmail();
        studentMapper.insertStudent(student);
        TeacherStudent relation = relation(teacher.getTeacherId(), student.getStudentId());
        teacherStudentMapper.insertTeacherStudent(relation);
        String key = teacherStudentAccessService.createAccess(teacher.getTeacherId(), student.getStudentId()).publicAccessKey();
        TeacherStudentAccess access = teacherStudentAccessMapper.selectActiveByTeacherStudentId(relation.getTeacherStudentId());

        studentEmailVerificationService.issue(key, student.getEmail());
        StudentEmailVerification first = studentEmailVerificationMapper.selectCurrentPendingByAccessId(access.getTeacherStudentAccessId());
        assertThat(first.getVerificationStatus()).isEqualTo(StudentEmailVerificationStatus.PENDING);
        assertThat(first.getCodeHash()).isNotEqualTo("123456");
        assertThat(first.getEmailHash()).doesNotContain("@");

        assertThatThrownBy(() -> studentEmailVerificationService.issue(key, student.getEmail()))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);

        now.set(now.get().plusSeconds(60));
        when(otpGenerator.generate()).thenReturn("654321");
        studentEmailVerificationService.issue(key, student.getEmail());
        assertThat(studentEmailVerificationMapper.selectByVerificationId(first.getVerificationId()).getVerificationStatus())
                .isEqualTo(StudentEmailVerificationStatus.INVALIDATED);
        StudentEmailVerification second = studentEmailVerificationMapper.selectCurrentPendingByAccessId(
                access.getTeacherStudentAccessId());
        assertThat(second.getVerificationId()).isNotEqualTo(first.getVerificationId());

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> studentEmailVerificationService.verify(key, student.getEmail(), "000000"))
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.AUTH_FAILED);
        }
        StudentEmailVerification locked = studentEmailVerificationMapper.selectLatestByAccessId(access.getTeacherStudentAccessId());
        assertThat(locked.getVerificationStatus()).isEqualTo(StudentEmailVerificationStatus.LOCKED);
        assertThat(locked.getFailedAttemptCount()).isEqualTo(5);
        assertThat(locked.getLockedUntil()).isNotNull();

        now.set(now.get().plusSeconds(30));
        assertThatThrownBy(() -> studentEmailVerificationService.issue(key, student.getEmail()))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);

        now.set(locked.getLockedUntil().toInstant(ZoneOffset.UTC));
        when(otpGenerator.generate()).thenReturn("111111");
        studentEmailVerificationService.issue(key, student.getEmail());
        StudentEmailVerification afterLock = studentEmailVerificationMapper.selectCurrentPendingByAccessId(
                access.getTeacherStudentAccessId());
        assertThat(afterLock.getVerificationStatus()).isEqualTo(StudentEmailVerificationStatus.PENDING);

        now.set(afterLock.getExpiresAt().toInstant(ZoneOffset.UTC));
        assertThatThrownBy(() -> studentEmailVerificationService.verify(key, student.getEmail(), "111111"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);

        now.set(afterLock.getCreatedAt().toInstant(ZoneOffset.UTC).plusSeconds(60));
        when(otpGenerator.generate()).thenReturn("222222");
        studentEmailVerificationService.issue(key, student.getEmail());
        IssuedStudentSession issued = studentEmailVerificationService.verify(key, student.getEmail(), "222222");
        StudentEmailVerification consumed = studentEmailVerificationMapper.selectLatestByAccessId(
                access.getTeacherStudentAccessId());
        assertThat(consumed.getVerificationStatus()).isEqualTo(StudentEmailVerificationStatus.CONSUMED);
        assertThatThrownBy(() -> studentEmailVerificationService.verify(key, student.getEmail(), "222222"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);

        StudentAccessSession created = studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(issued.rawToken()));
        assertThat(created.getSessionStatus()).isEqualTo(StudentAccessSessionStatus.ACTIVE);
        assertThat(created.getSessionTokenHash()).isNotEqualTo(issued.rawToken());
        teacherStudentAccessService.revokeAccess(teacher.getTeacherId(), student.getStudentId());
        assertThat(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(issued.rawToken())).getSessionStatus())
                .isEqualTo(StudentAccessSessionStatus.REVOKED);
        String rotated = teacherStudentAccessService.createAccess(teacher.getTeacherId(), student.getStudentId())
                .publicAccessKey();
        assertThat(rotated).isNotEqualTo(key);
        assertThat(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(issued.rawToken())).getSessionStatus())
                .isEqualTo(StudentAccessSessionStatus.REVOKED);
        assertThat(studentAccessSessionService.authenticate(issued.rawToken())).isEmpty();
        assertThatThrownBy(() -> studentEmailVerificationService.issue(key, student.getEmail()))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void sessionSurvivesOnlyWhileAccessRelationAndEmailStayValid() {
        Teacher teacher = teacher();
        teacherMapper.insertTeacher(teacher);
        Student student = studentWithEmail();
        studentMapper.insertStudent(student);
        TeacherStudent relation = relation(teacher.getTeacherId(), student.getStudentId());
        teacherStudentMapper.insertTeacherStudent(relation);
        String key = teacherStudentAccessService.createAccess(teacher.getTeacherId(), student.getStudentId()).publicAccessKey();
        studentEmailVerificationService.issue(key, student.getEmail());
        IssuedStudentSession issued = studentEmailVerificationService.verify(key, student.getEmail(), "123456");
        StudentAccessSession stored = studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(issued.rawToken()));
        assertThat(stored.getExpiresAt()).isAfter(stored.getCreatedAt());
        assertThat(stored.getAbsoluteExpiresAt()).isAfter(stored.getExpiresAt().minusSeconds(1));

        StudentAccessSession duplicate = new StudentAccessSession();
        duplicate.setTeacherStudentAccessId(stored.getTeacherStudentAccessId());
        duplicate.setSessionTokenHash(stored.getSessionTokenHash());
        duplicate.setSessionStatus(StudentAccessSessionStatus.ACTIVE);
        duplicate.setExpiresAt(stored.getExpiresAt());
        duplicate.setAbsoluteExpiresAt(stored.getAbsoluteExpiresAt());
        duplicate.setLastAccessedAt(stored.getLastAccessedAt());
        duplicate.setCreatedAt(stored.getCreatedAt());
        duplicate.setUpdatedAt(stored.getUpdatedAt());
        assertThatThrownBy(() -> studentAccessSessionMapper.insertStudentAccessSession(duplicate))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);

        now.set(now.get().plus(java.time.Duration.ofHours(24)));
        assertThat(studentAccessSessionService.authenticate(issued.rawToken())).isPresent();
        assertThat(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(issued.rawToken())).getLastAccessedAt())
                .isEqualTo(java.time.LocalDateTime.ofInstant(now.get(), ZoneOffset.UTC));

        now.set(now.get().plus(java.time.Duration.ofDays(31)));
        assertThat(studentAccessSessionService.authenticate(issued.rawToken())).isEmpty();

        studentEmailVerificationService.issue(key, student.getEmail());
        IssuedStudentSession renewed = studentEmailVerificationService.verify(key, student.getEmail(), "123456");
        studentAccessSessionService.logout(renewed.rawToken());
        assertThat(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(renewed.rawToken())).getSessionStatus())
                .isEqualTo(StudentAccessSessionStatus.REVOKED);

        now.set(now.get().plusSeconds(61));
        studentEmailVerificationService.issue(key, student.getEmail());
        IssuedStudentSession emailSession = studentEmailVerificationService.verify(key, student.getEmail(), "123456");
        StudentUpdateRequest same = new StudentUpdateRequest();
        same.setEmail(student.getEmail());
        studentService.updateStudent(teacher.getTeacherId(), student.getStudentId(), same);
        assertThat(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(emailSession.rawToken())).getSessionStatus())
                .isEqualTo(StudentAccessSessionStatus.ACTIVE);
        StudentUpdateRequest cleared = new StudentUpdateRequest();
        cleared.setEmail(null);
        studentService.updateStudent(teacher.getTeacherId(), student.getStudentId(), cleared);
        assertThat(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(emailSession.rawToken())).getSessionStatus())
                .isEqualTo(StudentAccessSessionStatus.REVOKED);

        student.setEmail("again." + UUID.randomUUID() + "@lessonpt.local");
        studentMapper.updateStudent(student);
        now.set(now.get().plusSeconds(61));
        studentEmailVerificationService.issue(key, student.getEmail());
        IssuedStudentSession releaseSession = studentEmailVerificationService.verify(key, student.getEmail(), "123456");
        studentService.releaseStudent(teacher.getTeacherId(), student.getStudentId());
        assertThat(studentAccessSessionMapper.selectByTokenHash(tokenHasher.hash(releaseSession.rawToken())).getSessionStatus())
                .isEqualTo(StudentAccessSessionStatus.REVOKED);
    }

    private Teacher teacher() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.otp." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }

    private Student studentWithEmail() {
        Student student = new Student();
        student.setName("임시학생");
        student.setEmail("student." + UUID.randomUUID() + "@lessonpt.local");
        student.setStatus(RecordStatus.ACTIVE);
        return student;
    }

    private TeacherStudent relation(Long teacherId, Long studentId) {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherId(teacherId);
        relation.setStudentId(studentId);
        relation.setStatus(RecordStatus.ACTIVE);
        return relation;
    }
}
