package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.yunki.lessonpt.common.model.ProgressStatus;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;

@ExtendWith(MockitoExtension.class)
class StudentMonitoringServiceTest {

    @Mock
    private StudentMonitoringMapper studentMonitoringMapper;

    @Mock
    private StudentCurriculumMapper studentCurriculumMapper;

    @Mock
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Mock
    private TeacherStudentMapper teacherStudentMapper;

    @Mock
    private ContentDetailMapper contentDetailMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CurriculumMapper curriculumMapper;

    private StudentMonitoringService studentMonitoringService;

    @BeforeEach
    void setUp() {
        studentMonitoringService = new StudentMonitoringService(
                studentMonitoringMapper,
                studentCurriculumMapper,
                teacherStudentLocationMapper,
                teacherStudentMapper,
                contentDetailMapper,
                categoryMapper,
                curriculumMapper);
    }

    @Test
    void assignInsertsFirstMonitoringAsYetAfterEnrollmentLock() {
        stubOwnedEnrollment(50L, 40L);
        stubAssignableContent(40L, 70L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        when(studentMonitoringMapper.selectByStudentCurriculumIdAndContentDetailId(50L, 70L)).thenReturn(null);
        when(studentMonitoringMapper.selectMaxDisplayOrderByStudentCurriculumId(50L)).thenReturn(null);
        when(studentMonitoringMapper.insertStudentMonitoring(any())).thenAnswer(invocation -> {
            invocation.<StudentMonitoring>getArgument(0).setMonitoringId(90L);
            return 1;
        });
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L)).thenReturn(saved(90L, 50L, 70L, 1));

        StudentMonitoring created = studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null);

        ArgumentCaptor<StudentMonitoring> captor = ArgumentCaptor.forClass(StudentMonitoring.class);
        InOrder order = inOrder(studentCurriculumMapper, studentMonitoringMapper);
        order.verify(studentCurriculumMapper).lockStudentCurriculumById(50L);
        order.verify(studentMonitoringMapper).selectByStudentCurriculumIdAndContentDetailId(50L, 70L);
        order.verify(studentMonitoringMapper).selectMaxDisplayOrderByStudentCurriculumId(50L);
        order.verify(studentMonitoringMapper).insertStudentMonitoring(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
        assertThat(captor.getValue().getCurrentBpm()).isNull();
        assertThat(captor.getValue().getProgressStatus()).isEqualTo(ProgressStatus.YET);
        assertThat(captor.getValue().getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(created.getMonitoringId()).isEqualTo(90L);
        verify(studentMonitoringMapper, never()).restoreStudentMonitoring(any());
    }

    @Test
    void assignAppendsAfterCurrentMaxAndKeepsRequestedValues() {
        stubOwnedEnrollment(50L, 40L);
        stubAssignableContent(40L, 70L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        when(studentMonitoringMapper.selectByStudentCurriculumIdAndContentDetailId(50L, 70L)).thenReturn(null);
        when(studentMonitoringMapper.selectMaxDisplayOrderByStudentCurriculumId(50L)).thenReturn(3);
        when(studentMonitoringMapper.insertStudentMonitoring(any())).thenAnswer(invocation -> {
            invocation.<StudentMonitoring>getArgument(0).setMonitoringId(91L);
            return 1;
        });
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(91L))
                .thenReturn(monitoring(91L, 50L, 70L, 4, 60, ProgressStatus.COMPLETED, "메모"));

        studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, 60, ProgressStatus.COMPLETED, "메모");
        studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, 240, ProgressStatus.STOPPED, null);

        ArgumentCaptor<StudentMonitoring> captor = ArgumentCaptor.forClass(StudentMonitoring.class);
        verify(studentMonitoringMapper, org.mockito.Mockito.times(2)).insertStudentMonitoring(captor.capture());
        assertThat(captor.getAllValues()).extracting(StudentMonitoring::getDisplayOrder).containsExactly(4, 4);
        assertThat(captor.getAllValues()).extracting(StudentMonitoring::getCurrentBpm).containsExactly(60, 240);
    }

    @Test
    void assignRejectsBpmOutsideRange() {
        assertCode(ErrorCode.COMMON_INVALID_INPUT,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, 59, null, null));
        assertCode(ErrorCode.COMMON_INVALID_INPUT,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, 241, null, null));
        verifyNoInteractions(studentCurriculumMapper);
    }

    @Test
    void assignRejectsActiveDuplicate() {
        stubOwnedEnrollment(50L, 40L);
        stubAssignableContent(40L, 70L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        when(studentMonitoringMapper.selectByStudentCurriculumIdAndContentDetailId(50L, 70L))
                .thenReturn(saved(90L, 50L, 70L, 1));

        assertCode(ErrorCode.COMMON_CONFLICT,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, 80, ProgressStatus.YET, "새"));
        verify(studentMonitoringMapper, never()).insertStudentMonitoring(any());
        verify(studentMonitoringMapper, never()).restoreStudentMonitoring(any());
    }

    @Test
    void assignRestoresInactiveRowWithoutOverwritingProgress() {
        stubOwnedEnrollment(50L, 40L);
        stubAssignableContent(40L, 70L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        StudentMonitoring inactive = monitoring(90L, 50L, 70L, 1, 130, ProgressStatus.COMPLETED, "완료");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentMonitoringMapper.selectByStudentCurriculumIdAndContentDetailId(50L, 70L)).thenReturn(inactive);
        when(studentMonitoringMapper.selectMaxDisplayOrderByStudentCurriculumId(50L)).thenReturn(2);
        when(studentMonitoringMapper.restoreStudentMonitoring(inactive)).thenReturn(1);
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L))
                .thenReturn(monitoring(90L, 50L, 70L, 3, 130, ProgressStatus.COMPLETED, "완료"));

        StudentMonitoring restored = studentMonitoringService.assignStudentMonitoring(
                8L, 50L, 70L, 80, ProgressStatus.YET, "새 메모");

        InOrder order = inOrder(studentCurriculumMapper, studentMonitoringMapper);
        order.verify(studentCurriculumMapper).lockStudentCurriculumById(50L);
        order.verify(studentMonitoringMapper).selectByStudentCurriculumIdAndContentDetailId(50L, 70L);
        order.verify(studentMonitoringMapper).selectMaxDisplayOrderByStudentCurriculumId(50L);
        order.verify(studentMonitoringMapper).restoreStudentMonitoring(inactive);
        assertThat(inactive.getDisplayOrder()).isEqualTo(3);
        assertThat(inactive.getCurrentBpm()).isEqualTo(130);
        assertThat(inactive.getProgressStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(inactive.getMemo()).isEqualTo("완료");
        assertThat(restored.getDisplayOrder()).isEqualTo(3);
        verify(studentMonitoringMapper, never()).insertStudentMonitoring(any());
        verify(studentMonitoringMapper, never()).updateStudentMonitoring(any());
    }

    @Test
    void assignRejectsMissingInactiveOrForeignEnrollment() {
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(50L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));

        when(studentCurriculumMapper.selectActiveStudentCurriculumById(50L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));

        stubOwnedEnrollment(50L, 40L);
        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));

        stubOwnedEnrollment(50L, 40L);
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(9L, RecordStatus.ACTIVE));
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));

        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.INACTIVE));
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));
        verify(studentCurriculumMapper, never()).lockStudentCurriculumById(any());
    }

    @Test
    void assignRejectsContentOutsideTheEnrollmentCurriculum() {
        stubOwnedEnrollment(50L, 40L);
        when(contentDetailMapper.selectActiveContentDetailById(70L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));

        when(contentDetailMapper.selectActiveContentDetailById(70L)).thenReturn(content(70L, 60L));
        when(categoryMapper.selectActiveCategoryById(60L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));

        when(categoryMapper.selectActiveCategoryById(60L)).thenReturn(category(60L, 40L));
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));

        when(categoryMapper.selectActiveCategoryById(60L)).thenReturn(category(60L, 41L));
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(41L, 8L)).thenReturn(curriculum(41L));
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));
        verify(studentCurriculumMapper, never()).lockStudentCurriculumById(any());
    }

    @Test
    void assignMapsEnrollmentLockTimeoutToOrderConflict() {
        stubOwnedEnrollment(50L, 40L);
        stubAssignableContent(40L, 70L);
        doThrow(new CannotAcquireLockException("ORA-30006"))
                .when(studentCurriculumMapper).lockStudentCurriculumById(50L);

        assertCode(ErrorCode.ORDER_CONFLICT,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));
        verify(studentMonitoringMapper, never()).selectByStudentCurriculumIdAndContentDetailId(any(), any());
    }

    @Test
    void assignRejectsBadWriteCount() {
        stubOwnedEnrollment(50L, 40L);
        stubAssignableContent(40L, 70L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        when(studentMonitoringMapper.selectByStudentCurriculumIdAndContentDetailId(50L, 70L)).thenReturn(null);
        when(studentMonitoringMapper.selectMaxDisplayOrderByStudentCurriculumId(50L)).thenReturn(null);
        when(studentMonitoringMapper.insertStudentMonitoring(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));

        StudentMonitoring inactive = saved(90L, 50L, 70L, 1);
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentMonitoringMapper.selectByStudentCurriculumIdAndContentDetailId(50L, 70L)).thenReturn(inactive);
        when(studentMonitoringMapper.restoreStudentMonitoring(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> studentMonitoringService.assignStudentMonitoring(8L, 50L, 70L, null, null, null));
    }

    @Test
    void readsActiveMonitoringsInsideTheEnrollment() {
        stubOwnedEnrollment(50L, 40L);
        when(studentMonitoringMapper.selectActiveStudentMonitoringsByStudentCurriculumId(50L))
                .thenReturn(List.of(saved(90L, 50L, 70L, 1)));
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L)).thenReturn(saved(90L, 50L, 70L, 1));
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(91L)).thenReturn(saved(91L, 51L, 70L, 1));

        assertThat(studentMonitoringService.getStudentMonitorings(8L, 50L)).hasSize(1);
        assertThat(studentMonitoringService.getStudentMonitoring(8L, 50L, 90L).getMonitoringId()).isEqualTo(90L);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentMonitoringService.getStudentMonitoring(8L, 50L, 91L));
    }

    @Test
    void updateChangesSpecifiedFieldsAndClearsNullableValues() {
        stubOwnedEnrollment(50L, 40L);
        StudentMonitoring current = monitoring(90L, 50L, 70L, 2, 100, ProgressStatus.YET, "기존");
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L)).thenReturn(current);
        when(studentMonitoringMapper.lockStudentMonitoringById(90L)).thenReturn(current);
        when(studentMonitoringMapper.updateStudentMonitoring(current)).thenReturn(1);

        StudentMonitoringChange change = new StudentMonitoringChange();
        change.setCurrentBpm(120);
        change.setProgressStatus(ProgressStatus.IN_PROGRESS);
        change.setMemo("수정");
        studentMonitoringService.updateStudentMonitoring(8L, 50L, 90L, change);

        change = new StudentMonitoringChange();
        change.setCurrentBpm(null);
        change.setMemo(null);
        StudentMonitoring updated = studentMonitoringService.updateStudentMonitoring(8L, 50L, 90L, change);

        assertThat(updated.getCurrentBpm()).isNull();
        assertThat(updated.getMemo()).isNull();
        assertThat(updated.getProgressStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(updated.getDisplayOrder()).isEqualTo(2);
        assertThat(updated.getContentDetailId()).isEqualTo(70L);
    }

    @Test
    void updateAllowsAnyProgressStatusAndRejectsExplicitNull() {
        stubOwnedEnrollment(50L, 40L);
        StudentMonitoring current = saved(90L, 50L, 70L, 1);
        current.setProgressStatus(ProgressStatus.COMPLETED);
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L)).thenReturn(current);
        when(studentMonitoringMapper.lockStudentMonitoringById(90L)).thenReturn(current);
        when(studentMonitoringMapper.updateStudentMonitoring(current)).thenReturn(1);

        StudentMonitoringChange backward = new StudentMonitoringChange();
        backward.setProgressStatus(ProgressStatus.YET);
        assertThat(studentMonitoringService.updateStudentMonitoring(8L, 50L, 90L, backward).getProgressStatus())
                .isEqualTo(ProgressStatus.YET);

        StudentMonitoringChange cleared = new StudentMonitoringChange();
        cleared.setProgressStatus(null);
        assertCode(ErrorCode.COMMON_INVALID_INPUT,
                () -> studentMonitoringService.updateStudentMonitoring(8L, 50L, 90L, cleared));
        verify(studentMonitoringMapper, org.mockito.Mockito.times(1)).updateStudentMonitoring(any());
    }

    @Test
    void updateRejectsBadBpmAndBadWriteCount() {
        stubOwnedEnrollment(50L, 40L);
        StudentMonitoring current = saved(90L, 50L, 70L, 1);
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L)).thenReturn(current);
        when(studentMonitoringMapper.lockStudentMonitoringById(90L)).thenReturn(current);

        StudentMonitoringChange low = new StudentMonitoringChange();
        low.setCurrentBpm(59);
        assertCode(ErrorCode.COMMON_INVALID_INPUT,
                () -> studentMonitoringService.updateStudentMonitoring(8L, 50L, 90L, low));

        StudentMonitoringChange valid = new StudentMonitoringChange();
        valid.setCurrentBpm(80);
        when(studentMonitoringMapper.updateStudentMonitoring(current)).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> studentMonitoringService.updateStudentMonitoring(8L, 50L, 90L, valid));
    }

    @Test
    void deleteCompressesOrderInsideTheSameEnrollment() {
        stubOwnedEnrollment(50L, 40L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L)).thenReturn(saved(90L, 50L, 70L, 2));
        when(studentMonitoringMapper.softDeleteStudentMonitoring(90L, 50L)).thenReturn(1);

        studentMonitoringService.deleteStudentMonitoring(8L, 50L, 90L);

        InOrder order = inOrder(studentCurriculumMapper, studentMonitoringMapper);
        order.verify(studentCurriculumMapper).lockStudentCurriculumById(50L);
        order.verify(studentMonitoringMapper).selectActiveStudentMonitoringById(90L);
        order.verify(studentMonitoringMapper).softDeleteStudentMonitoring(90L, 50L);
        order.verify(studentMonitoringMapper).shiftActiveDisplayOrdersDown(50L, 2);
        verifyNoInteractions(contentDetailMapper);
        verify(studentMonitoringMapper, never()).shiftActiveDisplayOrdersDown(org.mockito.ArgumentMatchers.eq(51L), any());
    }

    @Test
    void deleteAllowsInactiveContentDetailAndRejectsBadWriteCount() {
        stubOwnedEnrollment(50L, 40L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L)).thenReturn(saved(90L, 50L, 70L, 1));
        when(studentMonitoringMapper.softDeleteStudentMonitoring(90L, 50L)).thenReturn(1);
        studentMonitoringService.deleteStudentMonitoring(8L, 50L, 90L);
        verifyNoInteractions(contentDetailMapper);

        when(studentMonitoringMapper.softDeleteStudentMonitoring(90L, 50L)).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> studentMonitoringService.deleteStudentMonitoring(8L, 50L, 90L));
    }

    @Test
    void restoreAppendsAndKeepsStoredProgress() {
        stubOwnedEnrollment(50L, 40L);
        stubAssignableContent(40L, 70L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        StudentMonitoring inactive = monitoring(90L, 50L, 70L, 1, 130, ProgressStatus.COMPLETED, "완료");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentMonitoringMapper.selectStudentMonitoringById(90L)).thenReturn(inactive);
        when(studentMonitoringMapper.selectMaxDisplayOrderByStudentCurriculumId(50L)).thenReturn(4);
        when(studentMonitoringMapper.restoreStudentMonitoring(inactive)).thenReturn(1);
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(90L))
                .thenReturn(monitoring(90L, 50L, 70L, 5, 130, ProgressStatus.COMPLETED, "완료"));

        StudentMonitoring restored = studentMonitoringService.restoreStudentMonitoring(8L, 50L, 90L);

        InOrder order = inOrder(studentCurriculumMapper, studentMonitoringMapper, contentDetailMapper);
        order.verify(studentCurriculumMapper).lockStudentCurriculumById(50L);
        order.verify(studentMonitoringMapper).selectStudentMonitoringById(90L);
        order.verify(contentDetailMapper).selectActiveContentDetailById(70L);
        order.verify(studentMonitoringMapper).selectMaxDisplayOrderByStudentCurriculumId(50L);
        order.verify(studentMonitoringMapper).restoreStudentMonitoring(inactive);
        assertThat(inactive.getCurrentBpm()).isEqualTo(130);
        assertThat(inactive.getProgressStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(inactive.getMemo()).isEqualTo("완료");
        assertThat(inactive.getContentDetailId()).isEqualTo(70L);
        assertThat(restored.getDisplayOrder()).isEqualTo(5);
        verify(studentMonitoringMapper, never()).updateStudentMonitoring(any());
    }

    @Test
    void restoreRejectsActiveRowAndInactiveContentHierarchy() {
        stubOwnedEnrollment(50L, 40L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        when(studentMonitoringMapper.selectStudentMonitoringById(90L)).thenReturn(saved(90L, 50L, 70L, 1));
        assertCode(ErrorCode.COMMON_CONFLICT, () -> studentMonitoringService.restoreStudentMonitoring(8L, 50L, 90L));

        StudentMonitoring inactive = monitoring(90L, 50L, 70L, 1, 130, ProgressStatus.COMPLETED, "완료");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentMonitoringMapper.selectStudentMonitoringById(90L)).thenReturn(inactive);
        when(contentDetailMapper.selectActiveContentDetailById(70L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentMonitoringService.restoreStudentMonitoring(8L, 50L, 90L));

        when(contentDetailMapper.selectActiveContentDetailById(70L)).thenReturn(content(70L, 60L));
        when(categoryMapper.selectActiveCategoryById(60L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentMonitoringService.restoreStudentMonitoring(8L, 50L, 90L));

        when(categoryMapper.selectActiveCategoryById(60L)).thenReturn(category(60L, 40L));
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentMonitoringService.restoreStudentMonitoring(8L, 50L, 90L));

        when(categoryMapper.selectActiveCategoryById(60L)).thenReturn(category(60L, 41L));
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(41L, 8L)).thenReturn(curriculum(41L));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentMonitoringService.restoreStudentMonitoring(8L, 50L, 90L));
        verify(studentMonitoringMapper, never()).restoreStudentMonitoring(any());
    }

    @Test
    void restoreRejectsBadWriteCount() {
        stubOwnedEnrollment(50L, 40L);
        stubAssignableContent(40L, 70L);
        when(studentCurriculumMapper.lockStudentCurriculumById(50L)).thenReturn(enrollment(50L, 40L));
        StudentMonitoring inactive = saved(90L, 50L, 70L, 9);
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentMonitoringMapper.selectStudentMonitoringById(90L)).thenReturn(inactive);
        when(studentMonitoringMapper.selectMaxDisplayOrderByStudentCurriculumId(50L)).thenReturn(null);
        when(studentMonitoringMapper.restoreStudentMonitoring(inactive)).thenReturn(0);

        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> studentMonitoringService.restoreStudentMonitoring(8L, 50L, 90L));
        assertThat(inactive.getDisplayOrder()).isEqualTo(1);
    }

    private void stubOwnedEnrollment(Long studentCurriculumId, Long curriculumId) {
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(studentCurriculumId))
                .thenReturn(enrollment(studentCurriculumId, curriculumId));
        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(location());
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.ACTIVE));
    }

    private void stubAssignableContent(Long curriculumId, Long contentDetailId) {
        when(contentDetailMapper.selectActiveContentDetailById(contentDetailId)).thenReturn(content(contentDetailId, 60L));
        when(categoryMapper.selectActiveCategoryById(60L)).thenReturn(category(60L, curriculumId));
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(curriculumId, 8L)).thenReturn(curriculum(curriculumId));
    }

    private StudentCurriculum enrollment(Long studentCurriculumId, Long curriculumId) {
        StudentCurriculum enrollment = new StudentCurriculum();
        enrollment.setStudentCurriculumId(studentCurriculumId);
        enrollment.setTeacherStudentLocationId(30L);
        enrollment.setCurriculumId(curriculumId);
        enrollment.setStatus(RecordStatus.ACTIVE);
        return enrollment;
    }

    private TeacherStudentLocation location() {
        TeacherStudentLocation location = new TeacherStudentLocation();
        location.setTeacherStudentLocationId(30L);
        location.setTeacherStudentId(20L);
        location.setStatus(RecordStatus.ACTIVE);
        return location;
    }

    private TeacherStudent relation(Long teacherId, RecordStatus status) {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherStudentId(20L);
        relation.setTeacherId(teacherId);
        relation.setStatus(status);
        return relation;
    }

    private ContentDetail content(Long contentDetailId, Long categoryId) {
        ContentDetail contentDetail = new ContentDetail();
        contentDetail.setContentDetailId(contentDetailId);
        contentDetail.setCategoryId(categoryId);
        contentDetail.setStatus(RecordStatus.ACTIVE);
        return contentDetail;
    }

    private Category category(Long categoryId, Long curriculumId) {
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setCurriculumId(curriculumId);
        category.setStatus(RecordStatus.ACTIVE);
        return category;
    }

    private Curriculum curriculum(Long curriculumId) {
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumId(curriculumId);
        curriculum.setTeacherId(8L);
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }

    private StudentMonitoring saved(Long monitoringId, Long studentCurriculumId, Long contentDetailId, int displayOrder) {
        return monitoring(monitoringId, studentCurriculumId, contentDetailId, displayOrder, null, ProgressStatus.YET, null);
    }

    private StudentMonitoring monitoring(
            Long monitoringId,
            Long studentCurriculumId,
            Long contentDetailId,
            int displayOrder,
            Integer currentBpm,
            ProgressStatus progressStatus,
            String memo) {
        StudentMonitoring monitoring = new StudentMonitoring();
        monitoring.setMonitoringId(monitoringId);
        monitoring.setStudentCurriculumId(studentCurriculumId);
        monitoring.setContentDetailId(contentDetailId);
        monitoring.setDisplayOrder(displayOrder);
        monitoring.setCurrentBpm(currentBpm);
        monitoring.setProgressStatus(progressStatus);
        monitoring.setMemo(memo);
        monitoring.setStatus(RecordStatus.ACTIVE);
        return monitoring;
    }

    private void assertCode(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(errorCode);
    }
}
