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
import com.yunki.lessonpt.relationship.service.OtpGenerator;
import com.yunki.lessonpt.relationship.service.StudentEmailVerificationService;
import com.yunki.lessonpt.relationship.service.TeacherStudentAccessService;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;
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
        studentEmailVerificationService.verify(key, student.getEmail(), "222222");
        StudentEmailVerification consumed = studentEmailVerificationMapper.selectLatestByAccessId(
                access.getTeacherStudentAccessId());
        assertThat(consumed.getVerificationStatus()).isEqualTo(StudentEmailVerificationStatus.CONSUMED);
        assertThatThrownBy(() -> studentEmailVerificationService.verify(key, student.getEmail(), "222222"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);

        teacherStudentAccessService.revokeAccess(teacher.getTeacherId(), student.getStudentId());
        assertThatThrownBy(() -> studentEmailVerificationService.issue(key, student.getEmail()))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
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
