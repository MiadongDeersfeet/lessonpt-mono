package com.yunki.lessonpt.relationship.service;

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
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;

@ExtendWith(MockitoExtension.class)
class StudentCurriculumServiceTest {

    @Mock
    private StudentCurriculumMapper studentCurriculumMapper;

    @Mock
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Mock
    private TeacherStudentMapper teacherStudentMapper;

    @Mock
    private CurriculumMapper curriculumMapper;

    private StudentCurriculumService studentCurriculumService;

    @BeforeEach
    void setUp() {
        studentCurriculumService = new StudentCurriculumService(
                studentCurriculumMapper, teacherStudentLocationMapper, teacherStudentMapper, curriculumMapper);
    }

    @Test
    void assignInsertsFirstEnrollmentAfterParentLocks() {
        stubOwnedParents(30L, 40L);
        when(studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(30L, 40L)).thenReturn(null);
        when(studentCurriculumMapper.insertStudentCurriculum(any())).thenAnswer(invocation -> {
            invocation.<StudentCurriculum>getArgument(0).setStudentCurriculumId(90L);
            return 1;
        });
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, false, "첫 수강"));

        StudentCurriculum created = studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, "첫 수강");

        ArgumentCaptor<StudentCurriculum> captor = ArgumentCaptor.forClass(StudentCurriculum.class);
        InOrder order = inOrder(
                teacherStudentLocationMapper, teacherStudentMapper, curriculumMapper, studentCurriculumMapper);
        order.verify(teacherStudentLocationMapper).selectActiveTeacherStudentLocationById(30L);
        order.verify(teacherStudentMapper).selectTeacherStudentById(20L);
        order.verify(curriculumMapper).selectActiveCurriculumByIdAndTeacherId(40L, 8L);
        order.verify(teacherStudentLocationMapper).lockTeacherStudentLocationById(30L);
        order.verify(teacherStudentMapper).selectTeacherStudentById(20L);
        order.verify(curriculumMapper).lockCurriculumById(40L);
        order.verify(studentCurriculumMapper).selectByTeacherStudentLocationIdAndCurriculumId(30L, 40L);
        order.verify(studentCurriculumMapper).insertStudentCurriculum(captor.capture());
        assertThat(captor.getValue().getReenrolled()).isFalse();
        assertThat(captor.getValue().getMemo()).isEqualTo("첫 수강");
        assertThat(captor.getValue().getCurriculumId()).isEqualTo(40L);
        assertThat(created.getStudentCurriculumId()).isEqualTo(90L);
        verify(studentCurriculumMapper, never()).lockStudentCurriculumById(any());
    }

    @Test
    void assignRejectsBadInsertCount() {
        stubOwnedParents(30L, 40L);
        when(studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(30L, 40L)).thenReturn(null);
        when(studentCurriculumMapper.insertStudentCurriculum(any())).thenReturn(0);

        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null));
    }

    @Test
    void assignRejectsActiveDuplicate() {
        stubOwnedParents(30L, 40L);
        when(studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(30L, 40L))
                .thenReturn(saved(90L, 30L, 40L, false, "기존"));

        assertCode(ErrorCode.COMMON_CONFLICT, () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, "새"));
        verify(studentCurriculumMapper, never()).insertStudentCurriculum(any());
        verify(studentCurriculumMapper, never()).restoreStudentCurriculum(any());
    }

    @Test
    void assignRestoresInactivePairWithoutChangingMemo() {
        stubOwnedParents(30L, 40L);
        StudentCurriculum inactive = saved(90L, 30L, 40L, false, "기존 메모");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(30L, 40L)).thenReturn(inactive);
        when(studentCurriculumMapper.restoreStudentCurriculum(90L)).thenReturn(1);
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, true, "기존 메모"));

        StudentCurriculum restored = studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, "새 메모");

        verify(studentCurriculumMapper, never()).insertStudentCurriculum(any());
        verify(studentCurriculumMapper, never()).updateStudentCurriculum(any());
        assertThat(restored.getReenrolled()).isTrue();
        assertThat(restored.getMemo()).isEqualTo("기존 메모");
    }

    @Test
    void assignRejectsBadRestoreCount() {
        stubOwnedParents(30L, 40L);
        StudentCurriculum inactive = saved(90L, 30L, 40L, false, null);
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(30L, 40L)).thenReturn(inactive);
        when(studentCurriculumMapper.restoreStudentCurriculum(90L)).thenReturn(0);

        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null));
    }

    @Test
    void assignRejectsMissingOrInactiveParents() {
        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null));

        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(location(30L));
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(9L, RecordStatus.ACTIVE));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null));

        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.INACTIVE));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null));
        verify(teacherStudentLocationMapper, never()).lockTeacherStudentLocationById(any());

        stubOwnedLocation(30L);
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null));
        verify(curriculumMapper, never()).lockCurriculumById(any());
    }

    @Test
    void assignMapsParentLockTimeoutToOrderConflict() {
        stubOwnedLocation(30L);
        org.mockito.Mockito.doThrow(new CannotAcquireLockException("ORA-30006"))
                .when(teacherStudentLocationMapper).lockTeacherStudentLocationById(30L);
        stubOwnedCurriculum(40L);
        assertCode(ErrorCode.ORDER_CONFLICT, () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null));
        verify(curriculumMapper, never()).lockCurriculumById(any());

        org.mockito.Mockito.doReturn(location(30L))
                .when(teacherStudentLocationMapper).lockTeacherStudentLocationById(30L);
        org.mockito.Mockito.doThrow(new CannotAcquireLockException("ORA-30006"))
                .when(curriculumMapper).lockCurriculumById(40L);
        assertCode(ErrorCode.ORDER_CONFLICT, () -> studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null));
        verify(studentCurriculumMapper, never()).insertStudentCurriculum(any());
    }

    @Test
    void assignAllowsSameCurriculumAtAnotherLocation() {
        stubOwnedLocation(30L);
        stubOwnedLocation(31L);
        org.mockito.Mockito.doReturn(location(30L))
                .when(teacherStudentLocationMapper).lockTeacherStudentLocationById(30L);
        org.mockito.Mockito.doReturn(location(31L))
                .when(teacherStudentLocationMapper).lockTeacherStudentLocationById(31L);
        stubOwnedCurriculum(40L);
        org.mockito.Mockito.doReturn(curriculum(40L)).when(curriculumMapper).lockCurriculumById(40L);
        when(studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(30L, 40L)).thenReturn(null);
        when(studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(31L, 40L)).thenReturn(null);
        when(studentCurriculumMapper.insertStudentCurriculum(any())).thenAnswer(invocation -> {
            StudentCurriculum created = invocation.getArgument(0);
            created.setStudentCurriculumId(created.getTeacherStudentLocationId() == 30L ? 90L : 91L);
            return 1;
        });
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, false, null));
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(91L))
                .thenReturn(saved(91L, 31L, 40L, false, null));

        studentCurriculumService.assignStudentCurriculum(8L, 30L, 40L, null);
        studentCurriculumService.assignStudentCurriculum(8L, 31L, 40L, null);

        verify(studentCurriculumMapper).selectByTeacherStudentLocationIdAndCurriculumId(30L, 40L);
        verify(studentCurriculumMapper).selectByTeacherStudentLocationIdAndCurriculumId(31L, 40L);
    }

    @Test
    void getReturnsOwnRowsAndHidesAnotherLocation() {
        stubOwnedLocation(30L);
        when(studentCurriculumMapper.selectActiveStudentCurriculumsByTeacherStudentLocationId(30L))
                .thenReturn(List.of(saved(90L, 30L, 40L, false, "메모")));
        assertThat(studentCurriculumService.getStudentCurriculums(8L, 30L))
                .extracting(StudentCurriculum::getMemo)
                .containsExactly("메모");

        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, false, "메모"));
        assertThat(studentCurriculumService.getStudentCurriculum(8L, 30L, 90L).getCurriculumId()).isEqualTo(40L);

        when(studentCurriculumMapper.selectActiveStudentCurriculumById(91L))
                .thenReturn(saved(91L, 31L, 40L, false, "다른장소"));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentCurriculumService.getStudentCurriculum(8L, 30L, 91L));
    }

    @Test
    void updateChangesMemoAndKeepsRelationKeys() {
        stubOwnedLocation(30L);
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, false, "기존"));
        when(studentCurriculumMapper.lockStudentCurriculumById(90L)).thenReturn(saved(90L, 30L, 40L, false, "기존"));
        when(studentCurriculumMapper.updateStudentCurriculum(any())).thenReturn(1);

        StudentCurriculumChange change = new StudentCurriculumChange();
        change.setMemo(null);
        studentCurriculumService.updateStudentCurriculum(8L, 30L, 90L, change);

        ArgumentCaptor<StudentCurriculum> captor = ArgumentCaptor.forClass(StudentCurriculum.class);
        verify(studentCurriculumMapper).updateStudentCurriculum(captor.capture());
        assertThat(captor.getValue().getMemo()).isNull();
        assertThat(captor.getValue().getCurriculumId()).isEqualTo(40L);
        assertThat(captor.getValue().getReenrolled()).isFalse();
        verify(curriculumMapper, never()).lockCurriculumById(any());
    }

    @Test
    void updateRejectsBadRowCount() {
        stubOwnedLocation(30L);
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, false, "기존"));
        when(studentCurriculumMapper.lockStudentCurriculumById(90L)).thenReturn(saved(90L, 30L, 40L, false, "기존"));
        when(studentCurriculumMapper.updateStudentCurriculum(any())).thenReturn(0);

        StudentCurriculumChange change = new StudentCurriculumChange();
        change.setMemo("변경");
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> studentCurriculumService.updateStudentCurriculum(8L, 30L, 90L, change));
    }

    @Test
    void deleteSoftDeletesWithoutOrderOrMonitoring() {
        stubOwnedLocation(30L);
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, false, "메모"));
        when(studentCurriculumMapper.lockStudentCurriculumById(90L)).thenReturn(saved(90L, 30L, 40L, false, "메모"));
        when(studentCurriculumMapper.softDeleteStudentCurriculum(90L, 30L)).thenReturn(1);

        studentCurriculumService.deleteStudentCurriculum(8L, 30L, 90L);

        verify(studentCurriculumMapper).softDeleteStudentCurriculum(90L, 30L);
        verify(studentCurriculumMapper, never()).restoreStudentCurriculum(any());
        verify(curriculumMapper, never()).lockCurriculumById(any());
    }

    @Test
    void deleteRejectsBadRowCount() {
        stubOwnedLocation(30L);
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, false, "메모"));
        when(studentCurriculumMapper.lockStudentCurriculumById(90L)).thenReturn(saved(90L, 30L, 40L, false, "메모"));
        when(studentCurriculumMapper.softDeleteStudentCurriculum(90L, 30L)).thenReturn(0);

        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> studentCurriculumService.deleteStudentCurriculum(8L, 30L, 90L));
    }

    @Test
    void restoreReenrollsInactiveRowAfterParentLocks() {
        stubOwnedParents(30L, 40L);
        StudentCurriculum inactive = saved(90L, 30L, 40L, false, "기존");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentCurriculumMapper.selectStudentCurriculumById(90L)).thenReturn(inactive);
        when(studentCurriculumMapper.restoreStudentCurriculum(90L)).thenReturn(1);
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L))
                .thenReturn(saved(90L, 30L, 40L, true, "기존"));

        StudentCurriculum restored = studentCurriculumService.restoreStudentCurriculum(8L, 30L, 90L);

        InOrder order = inOrder(teacherStudentLocationMapper, curriculumMapper, studentCurriculumMapper);
        order.verify(teacherStudentLocationMapper).lockTeacherStudentLocationById(30L);
        order.verify(curriculumMapper).lockCurriculumById(40L);
        order.verify(studentCurriculumMapper).restoreStudentCurriculum(90L);
        assertThat(restored.getReenrolled()).isTrue();
    }

    @Test
    void restoreRejectsActiveRowInactiveCurriculumOtherLocationAndBadCount() {
        stubOwnedParents(30L, 40L);
        when(studentCurriculumMapper.selectStudentCurriculumById(90L)).thenReturn(saved(90L, 30L, 40L, false, "기존"));
        assertCode(ErrorCode.COMMON_CONFLICT, () -> studentCurriculumService.restoreStudentCurriculum(8L, 30L, 90L));
        verify(studentCurriculumMapper, never()).restoreStudentCurriculum(any());

        StudentCurriculum inactive = saved(91L, 30L, 41L, false, null);
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentCurriculumMapper.selectStudentCurriculumById(91L)).thenReturn(inactive);
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(41L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentCurriculumService.restoreStudentCurriculum(8L, 30L, 91L));

        when(studentCurriculumMapper.selectStudentCurriculumById(92L)).thenReturn(saved(92L, 31L, 40L, false, null));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> studentCurriculumService.restoreStudentCurriculum(8L, 30L, 92L));

        when(studentCurriculumMapper.selectStudentCurriculumById(90L)).thenReturn(inactiveRow(90L));
        when(studentCurriculumMapper.restoreStudentCurriculum(90L)).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> studentCurriculumService.restoreStudentCurriculum(8L, 30L, 90L));
    }

    private void stubOwnedParents(Long teacherStudentLocationId, Long curriculumId) {
        stubOwnedLocation(teacherStudentLocationId);
        org.mockito.Mockito.doReturn(location(teacherStudentLocationId))
                .when(teacherStudentLocationMapper).lockTeacherStudentLocationById(teacherStudentLocationId);
        stubOwnedCurriculum(curriculumId);
        org.mockito.Mockito.doReturn(curriculum(curriculumId))
                .when(curriculumMapper).lockCurriculumById(curriculumId);
    }

    private void stubOwnedLocation(Long teacherStudentLocationId) {
        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(teacherStudentLocationId))
                .thenReturn(location(teacherStudentLocationId));
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.ACTIVE));
    }

    private void stubOwnedCurriculum(Long curriculumId) {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(curriculumId, 8L)).thenReturn(curriculum(curriculumId));
    }

    private TeacherStudentLocation location(Long teacherStudentLocationId) {
        TeacherStudentLocation location = new TeacherStudentLocation();
        location.setTeacherStudentLocationId(teacherStudentLocationId);
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

    private Curriculum curriculum(Long curriculumId) {
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumId(curriculumId);
        curriculum.setTeacherId(8L);
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }

    private StudentCurriculum saved(
            Long studentCurriculumId, Long teacherStudentLocationId, Long curriculumId, boolean reenrolled, String memo) {
        StudentCurriculum studentCurriculum = new StudentCurriculum();
        studentCurriculum.setStudentCurriculumId(studentCurriculumId);
        studentCurriculum.setTeacherStudentLocationId(teacherStudentLocationId);
        studentCurriculum.setCurriculumId(curriculumId);
        studentCurriculum.setReenrolled(reenrolled);
        studentCurriculum.setMemo(memo);
        studentCurriculum.setStatus(RecordStatus.ACTIVE);
        return studentCurriculum;
    }

    private StudentCurriculum inactiveRow(Long studentCurriculumId) {
        StudentCurriculum studentCurriculum = saved(studentCurriculumId, 30L, 40L, false, "기존");
        studentCurriculum.setStatus(RecordStatus.INACTIVE);
        return studentCurriculum;
    }

    private void assertCode(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(errorCode);
    }
}
