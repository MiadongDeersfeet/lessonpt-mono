package com.yunki.lessonpt.teacher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    private static final String RAW_PASSWORD = "Abcdef1!";
    private static final String EMAIL = "teacher@lessonpt.local";

    @Mock
    private TeacherMapper teacherMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        teacherService = new TeacherService(teacherMapper, passwordEncoder);
    }

    @Test
    void createTeacherStoresEncodedPasswordAndDefaults() {
        when(teacherMapper.selectTeacherByEmail(EMAIL)).thenReturn(null);
        AtomicReference<Teacher> stored = new AtomicReference<>();
        when(teacherMapper.insertTeacher(any())).thenAnswer(invocation -> {
            Teacher teacher = invocation.getArgument(0);
            teacher.setTeacherId(4L);
            stored.set(teacher);
            return 1;
        });
        when(teacherMapper.selectTeacherById(4L)).thenAnswer(invocation -> stored.get());

        Teacher created = teacherService.createTeacher(EMAIL, RAW_PASSWORD, "김강사", null);

        assertThat(created.getTeacherId()).isEqualTo(4L);
        assertThat(created.getRole()).isEqualTo("TEACHER");
        assertThat(created.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(created.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(created.getPasswordHash()).doesNotContain(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, created.getPasswordHash())).isTrue();
    }

    @Test
    void createTeacherRejectsDuplicateEmail() {
        when(teacherMapper.selectTeacherByEmail(EMAIL)).thenReturn(teacher(4L));

        assertThatThrownBy(() -> teacherService.createTeacher(EMAIL, RAW_PASSWORD, "김강사", null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
        verify(teacherMapper, never()).insertTeacher(any());
    }

    @Test
    void getTeacherReturnsActiveTeacher() {
        Teacher active = teacher(4L);
        when(teacherMapper.selectActiveTeacherById(4L)).thenReturn(active);

        assertThat(teacherService.getTeacher(4L)).isSameAs(active);
    }

    @Test
    void getTeacherRejectsMissingTeacher() {
        when(teacherMapper.selectActiveTeacherById(4L)).thenReturn(null);

        assertThatThrownBy(() -> teacherService.getTeacher(4L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void updateTeacherChangesNameAndPhone() {
        Teacher active = teacher(4L);
        when(teacherMapper.selectActiveTeacherById(4L)).thenReturn(active);

        teacherService.updateTeacher(4L, "새이름", "010");

        ArgumentCaptor<Teacher> captor = ArgumentCaptor.forClass(Teacher.class);
        verify(teacherMapper).updateTeacher(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("새이름");
        assertThat(captor.getValue().getPhone()).isEqualTo("010");
    }

    @Test
    void updateTeacherRejectsMissingTeacher() {
        when(teacherMapper.selectActiveTeacherById(4L)).thenReturn(null);

        assertThatThrownBy(() -> teacherService.updateTeacher(4L, "새이름", null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(teacherMapper, never()).updateTeacher(any());
    }

    @Test
    void deleteTeacherSoftDeletesActiveTeacher() {
        when(teacherMapper.selectActiveTeacherById(4L)).thenReturn(teacher(4L));

        teacherService.deleteTeacher(4L);

        verify(teacherMapper).softDeleteTeacher(4L);
    }

    @Test
    void deleteTeacherRejectsMissingTeacher() {
        when(teacherMapper.selectActiveTeacherById(4L)).thenReturn(null);

        assertThatThrownBy(() -> teacherService.deleteTeacher(4L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(teacherMapper, never()).softDeleteTeacher(any());
    }

    @Test
    void restoreTeacherRestoresInactiveTeacher() {
        Teacher inactive = teacher(4L);
        inactive.setStatus(RecordStatus.INACTIVE);
        when(teacherMapper.selectTeacherById(4L)).thenReturn(inactive);

        teacherService.restoreTeacher(4L);

        verify(teacherMapper).restoreTeacher(4L);
    }

    @Test
    void restoreTeacherRejectsMissingTeacher() {
        when(teacherMapper.selectTeacherById(4L)).thenReturn(null);

        assertThatThrownBy(() -> teacherService.restoreTeacher(4L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(teacherMapper, never()).restoreTeacher(any());
    }

    private Teacher teacher(Long teacherId) {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(teacherId);
        teacher.setEmail(EMAIL);
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }
}
