package com.yunki.lessonpt.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.dto.CurriculumUpdateRequest;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {

    @Mock
    private CurriculumMapper curriculumMapper;

    @Mock
    private TeacherMapper teacherMapper;

    private CurriculumService curriculumService;

    @BeforeEach
    void setUp() {
        curriculumService = new CurriculumService(curriculumMapper, teacherMapper);
    }

    @Test
    void createAssignsOrderOneWhenNoneExist() {
        stubActiveTeacher();
        when(curriculumMapper.selectMaxDisplayOrderByTeacherId(8L)).thenReturn(null);
        when(curriculumMapper.insertCurriculum(any())).thenAnswer(invocation -> {
            invocation.<Curriculum>getArgument(0).setCurriculumId(50L);
            return 1;
        });
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 8L)).thenReturn(saved(50L, 8L, 1, "입문"));

        Curriculum created = curriculumService.createCurriculum(8L, "입문");

        ArgumentCaptor<Curriculum> captor = ArgumentCaptor.forClass(Curriculum.class);
        InOrder order = inOrder(teacherMapper, curriculumMapper);
        order.verify(teacherMapper).selectActiveTeacherById(8L);
        order.verify(teacherMapper).lockTeacherById(8L);
        order.verify(curriculumMapper).selectMaxDisplayOrderByTeacherId(8L);
        order.verify(curriculumMapper).insertCurriculum(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
        assertThat(captor.getValue().getTeacherId()).isEqualTo(8L);
        assertThat(created.getCurriculumId()).isEqualTo(50L);
    }

    @Test
    void createAppendsAfterExistingMax() {
        stubActiveTeacher();
        when(curriculumMapper.selectMaxDisplayOrderByTeacherId(8L)).thenReturn(3);
        when(curriculumMapper.insertCurriculum(any())).thenAnswer(invocation -> {
            invocation.<Curriculum>getArgument(0).setCurriculumId(51L);
            return 1;
        });
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(51L, 8L)).thenReturn(saved(51L, 8L, 4, "심화"));

        curriculumService.createCurriculum(8L, "심화");

        ArgumentCaptor<Curriculum> captor = ArgumentCaptor.forClass(Curriculum.class);
        verify(curriculumMapper).insertCurriculum(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(4);
    }

    @Test
    void createRejectsMissingTeacherAndLockTimeout() {
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> curriculumService.createCurriculum(8L, "입문"));
        verify(teacherMapper, never()).lockTeacherById(any());

        stubActiveTeacher();
        when(teacherMapper.lockTeacherById(8L)).thenThrow(new CannotAcquireLockException("ORA-30006"));
        assertCode(ErrorCode.ORDER_CONFLICT, () -> curriculumService.createCurriculum(8L, "입문"));
        verify(curriculumMapper, never()).insertCurriculum(any());
    }

    @Test
    void getReturnsOwnActiveCurriculumAndHidesOthers() {
        when(curriculumMapper.selectActiveCurriculumsByTeacherId(8L)).thenReturn(List.of(saved(50L, 8L, 1, "입문")));
        assertThat(curriculumService.getCurriculums(8L)).extracting(Curriculum::getName).containsExactly("입문");

        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 8L)).thenReturn(saved(50L, 8L, 1, "입문"));
        assertThat(curriculumService.getCurriculum(8L, 50L).getName()).isEqualTo("입문");

        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 9L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> curriculumService.getCurriculum(9L, 50L));
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(99L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> curriculumService.getCurriculum(8L, 99L));
    }

    @Test
    void updateChangesNameForTheOwner() {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 8L)).thenReturn(saved(50L, 8L, 1, "입문"));
        when(curriculumMapper.lockCurriculumById(50L)).thenReturn(saved(50L, 8L, 1, "입문"));
        when(curriculumMapper.updateCurriculum(any())).thenReturn(1);

        curriculumService.updateCurriculum(8L, 50L, updateName("입문 개정"));

        ArgumentCaptor<Curriculum> captor = ArgumentCaptor.forClass(Curriculum.class);
        verify(curriculumMapper).updateCurriculum(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("입문 개정");
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void updateRejectsAnotherTeacherAndUnexpectedRowCount() {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 9L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> curriculumService.updateCurriculum(9L, 50L, updateName("다른이름")));
        verify(curriculumMapper, never()).updateCurriculum(any());

        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 8L)).thenReturn(saved(50L, 8L, 1, "입문"));
        when(curriculumMapper.lockCurriculumById(50L)).thenReturn(saved(50L, 8L, 1, "입문"));
        when(curriculumMapper.updateCurriculum(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> curriculumService.updateCurriculum(8L, 50L, updateName("입문 개정")));
    }

    @Test
    void deleteSoftDeletesAndCompressesOnlyThatTeacher() {
        stubActiveTeacher();
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 8L)).thenReturn(saved(50L, 8L, 1, "입문"));
        when(curriculumMapper.softDeleteCurriculum(50L, 8L)).thenReturn(1);

        curriculumService.deleteCurriculum(8L, 50L);

        InOrder order = inOrder(teacherMapper, curriculumMapper);
        order.verify(teacherMapper).lockTeacherById(8L);
        order.verify(curriculumMapper).selectActiveCurriculumByIdAndTeacherId(50L, 8L);
        order.verify(curriculumMapper).softDeleteCurriculum(50L, 8L);
        order.verify(curriculumMapper).shiftActiveDisplayOrdersDown(8L, 1);
        verify(curriculumMapper, never()).shiftActiveDisplayOrdersDown(org.mockito.ArgumentMatchers.eq(9L), any());
    }

    @Test
    void deleteRejectsAnotherTeacherAndUnexpectedRowCount() {
        stubActiveTeacher();
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> curriculumService.deleteCurriculum(8L, 50L));
        verify(curriculumMapper, never()).softDeleteCurriculum(any(), any());

        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 8L)).thenReturn(saved(50L, 8L, 1, "입문"));
        when(curriculumMapper.softDeleteCurriculum(50L, 8L)).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> curriculumService.deleteCurriculum(8L, 50L));
        verify(curriculumMapper, never()).shiftActiveDisplayOrdersDown(any(), any());
    }

    @Test
    void restoreAppendsInactiveCurriculum() {
        stubActiveTeacher();
        Curriculum inactive = saved(50L, 8L, 1, "입문");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(curriculumMapper.selectCurriculumById(50L)).thenReturn(inactive);
        when(curriculumMapper.selectMaxDisplayOrderByTeacherId(8L)).thenReturn(2);
        when(curriculumMapper.restoreCurriculum(any())).thenReturn(1);
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(50L, 8L)).thenReturn(saved(50L, 8L, 3, "입문"));

        Curriculum restored = curriculumService.restoreCurriculum(8L, 50L);

        ArgumentCaptor<Curriculum> captor = ArgumentCaptor.forClass(Curriculum.class);
        InOrder order = inOrder(teacherMapper, curriculumMapper);
        order.verify(teacherMapper).lockTeacherById(8L);
        order.verify(curriculumMapper).selectMaxDisplayOrderByTeacherId(8L);
        order.verify(curriculumMapper).restoreCurriculum(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
        assertThat(restored.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void restoreRejectsActiveOtherTeacherInactiveTeacherAndBadRowCount() {
        stubActiveTeacher();
        when(curriculumMapper.selectCurriculumById(50L)).thenReturn(saved(50L, 8L, 1, "입문"));
        assertCode(ErrorCode.COMMON_CONFLICT, () -> curriculumService.restoreCurriculum(8L, 50L));

        Curriculum other = saved(51L, 9L, 1, "다른강사");
        other.setStatus(RecordStatus.INACTIVE);
        when(curriculumMapper.selectCurriculumById(51L)).thenReturn(other);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> curriculumService.restoreCurriculum(8L, 51L));

        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> curriculumService.restoreCurriculum(8L, 50L));

        stubActiveTeacher();
        Curriculum inactive = saved(52L, 8L, 1, "입문");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(curriculumMapper.selectCurriculumById(52L)).thenReturn(inactive);
        when(curriculumMapper.selectMaxDisplayOrderByTeacherId(8L)).thenReturn(null);
        when(curriculumMapper.restoreCurriculum(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> curriculumService.restoreCurriculum(8L, 52L));
        verify(curriculumMapper, never()).restoreCurriculum(org.mockito.ArgumentMatchers.argThat(
                curriculum -> curriculum.getTeacherId() != null && curriculum.getTeacherId().equals(9L)));
    }

    private CurriculumUpdateRequest updateName(String name) {
        CurriculumUpdateRequest request = new CurriculumUpdateRequest();
        request.setName(name);
        return request;
    }

    private void stubActiveTeacher() {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(8L);
        teacher.setStatus(RecordStatus.ACTIVE);
        when(teacherMapper.selectActiveTeacherById(8L)).thenReturn(teacher);
        when(teacherMapper.lockTeacherById(8L)).thenReturn(teacher);
    }

    private Curriculum saved(Long curriculumId, Long teacherId, int order, String name) {
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumId(curriculumId);
        curriculum.setTeacherId(teacherId);
        curriculum.setName(name);
        curriculum.setDisplayOrder(order);
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }

    private void assertCode(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(errorCode);
    }
}
