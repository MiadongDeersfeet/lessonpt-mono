package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.StudentLoginVerification;
import com.yunki.lessonpt.relationship.mail.EmailDeliveryException;
import com.yunki.lessonpt.relationship.mail.EmailSender;
import com.yunki.lessonpt.relationship.mail.StudentOtpPurpose;
import com.yunki.lessonpt.relationship.mapper.StudentLoginVerificationMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;

@ExtendWith(MockitoExtension.class)
class StudentLoginVerificationServiceTest {

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private StudentPortalMapper studentPortalMapper;

    @Mock
    private StudentLoginVerificationMapper studentLoginVerificationMapper;

    @Mock
    private OtpGenerator otpGenerator;

    @Mock
    private StudentAccessSessionService studentAccessSessionService;

    @Mock
    private EmailSender emailSender;

    private final TokenHasher tokenHasher = new TokenHasher();

    private StudentLoginVerificationService service;

    @BeforeEach
    void setUp() {
        service = new StudentLoginVerificationService(
                studentMapper,
                studentPortalMapper,
                studentLoginVerificationMapper,
                otpGenerator,
                tokenHasher,
                Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneOffset.UTC),
                studentAccessSessionService,
                emailSender);
    }

    @Test
    void issueSendsGeneralLoginOtpAndStoresOnlyTheHash() {
        Student student = new Student();
        student.setStudentId(41L);
        student.setEmail("student@lessonpt.local");
        student.setStatus(RecordStatus.ACTIVE);
        when(studentMapper.selectStudentByEmail("student@lessonpt.local")).thenReturn(student);
        when(studentPortalMapper.countActiveAccesses(41L)).thenReturn(1);
        when(otpGenerator.generate()).thenReturn("654321");
        when(studentLoginVerificationMapper.insertStudentLoginVerification(any())).thenReturn(1);

        service.issue(" Student@LessonPT.local ");

        ArgumentCaptor<StudentLoginVerification> captor = ArgumentCaptor.forClass(StudentLoginVerification.class);
        verify(studentLoginVerificationMapper).insertStudentLoginVerification(captor.capture());
        assertThat(captor.getValue().getCodeHash()).isEqualTo(tokenHasher.hash("654321")).isNotEqualTo("654321");
        verify(emailSender).sendStudentOtp("student@lessonpt.local", "654321", StudentOtpPurpose.GENERAL_LOGIN);
    }

    @Test
    void issueDoesNotSucceedWhenMailDeliveryFails() {
        Student student = new Student();
        student.setStudentId(41L);
        student.setEmail("student@lessonpt.local");
        student.setStatus(RecordStatus.ACTIVE);
        when(studentMapper.selectStudentByEmail("student@lessonpt.local")).thenReturn(student);
        when(studentPortalMapper.countActiveAccesses(41L)).thenReturn(1);
        when(otpGenerator.generate()).thenReturn("654321");
        when(studentLoginVerificationMapper.insertStudentLoginVerification(any())).thenReturn(1);
        doThrow(new EmailDeliveryException()).when(emailSender)
                .sendStudentOtp("student@lessonpt.local", "654321", StudentOtpPurpose.GENERAL_LOGIN);

        assertThatThrownBy(() -> service.issue("student@lessonpt.local"))
                .isInstanceOf(EmailDeliveryException.class);
    }
}
