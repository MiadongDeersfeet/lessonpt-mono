package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import com.yunki.lessonpt.relationship.domain.Homework;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;

@ExtendWith(MockitoExtension.class)
class HomeworkServiceTest {

    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 10, 1, 18, 0);

    @Mock
    private HomeworkMapper homeworkMapper;

    @Mock
    private StudentMonitoringMapper studentMonitoringMapper;

    @Mock
    private StudentCurriculumMapper studentCurriculumMapper;

    @Mock
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Mock
    private TeacherStudentMapper teacherStudentMapper;

    private HomeworkService homeworkService;

    @BeforeEach
    void setUp() {
        homeworkService = new HomeworkService(
                homeworkMapper,
                studentMonitoringMapper,
                studentCurriculumMapper,
                teacherStudentLocationMapper,
                teacherStudentMapper);
    }

    @Test
    void createStoresIncompleteHomeworkAndAllowsAnotherOnTheSameMonitoring() {
        stubOwnedMonitoring();
        stubParentLock();
        when(homeworkMapper.insertHomework(any())).thenAnswer(invocation -> {
            Homework created = invocation.getArgument(0);
            created.setHomeworkId(created.getFeedback() == null ? 90L : 91L);
            return 1;
        });
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L))
                .thenReturn(saved(90L, 70L, "싱글 스트로크", null, false, null));
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(91L, 70L))
                .thenReturn(saved(91L, 70L, "더블 스트로크", DEADLINE, false, "메모"));

        Homework first = homeworkService.createHomework(8L, 70L, "싱글 스트로크", null, null);
        Homework second = homeworkService.createHomework(8L, 70L, "더블 스트로크", DEADLINE, "메모");

        ArgumentCaptor<Homework> captor = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper, org.mockito.Mockito.times(2)).insertHomework(captor.capture());
        assertThat(captor.getAllValues()).extracting(Homework::getCompleted).containsExactly(false, false);
        assertThat(captor.getAllValues().get(0).getDeadline()).isNull();
        assertThat(captor.getAllValues().get(0).getFeedback()).isNull();
        assertThat(first.getHomeworkId()).isEqualTo(90L);
        assertThat(second.getHomeworkId()).isEqualTo(91L);
        verify(studentMonitoringMapper, never()).updateStudentMonitoring(any());
        verify(studentCurriculumMapper, never()).lockStudentCurriculumById(any());
    }

    @Test
    void createRejectsBlankContentAndBadInsertCount() {
        stubOwnedMonitoring();
        assertCode(ErrorCode.COMMON_INVALID_INPUT, () -> homeworkService.createHomework(8L, 70L, null, null, null));
        assertCode(ErrorCode.COMMON_INVALID_INPUT, () -> homeworkService.createHomework(8L, 70L, "  ", null, null));
        stubParentLock();
        when(homeworkMapper.insertHomework(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> homeworkService.createHomework(8L, 70L, "연습", null, null));
        verify(homeworkMapper, never()).insertHomework(argWithBlankContent());
    }

    @Test
    void createRejectsMissingOrForeignMonitoring() {
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(70L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> homeworkService.createHomework(8L, 70L, "연습", null, null));

        when(studentMonitoringMapper.selectActiveStudentMonitoringById(70L)).thenReturn(monitoring(50L));
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(50L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> homeworkService.createHomework(8L, 70L, "연습", null, null));

        when(studentCurriculumMapper.selectActiveStudentCurriculumById(50L)).thenReturn(enrollment());
        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> homeworkService.createHomework(8L, 70L, "연습", null, null));

        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(location());
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.INACTIVE));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> homeworkService.createHomework(8L, 70L, "연습", null, null));

        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(9L, RecordStatus.ACTIVE));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> homeworkService.createHomework(8L, 70L, "연습", null, null));
        verify(homeworkMapper, never()).insertHomework(any());
        verify(studentMonitoringMapper, never()).lockStudentMonitoringById(any());
    }

    @Test
    void createAllowsUpToThreeActiveHomeworksAfterTheMonitoringLock() {
        stubOwnedMonitoring();
        stubParentLock();
        when(homeworkMapper.countActiveHomeworksByMonitoringId(70L)).thenReturn(0, 1, 2);
        when(homeworkMapper.insertHomework(any())).thenAnswer(invocation -> {
            invocation.<Homework>getArgument(0).setHomeworkId(90L);
            return 1;
        });
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L))
                .thenReturn(saved(90L, 70L, "연습", null, false, null));

        homeworkService.createHomework(8L, 70L, "연습", null, null);
        homeworkService.createHomework(8L, 70L, "연습", null, null);
        homeworkService.createHomework(8L, 70L, "연습", null, null);

        InOrder order = inOrder(studentMonitoringMapper, homeworkMapper);
        order.verify(studentMonitoringMapper).lockStudentMonitoringById(70L);
        order.verify(homeworkMapper).countActiveHomeworksByMonitoringId(70L);
        order.verify(homeworkMapper).insertHomework(any());
        verify(homeworkMapper, org.mockito.Mockito.times(3)).insertHomework(any());
    }

    @Test
    void createRejectsAFourthActiveHomework() {
        stubOwnedMonitoring();
        stubParentLock();
        when(homeworkMapper.countActiveHomeworksByMonitoringId(70L)).thenReturn(3);

        assertCode(ErrorCode.COMMON_CONFLICT, () -> homeworkService.createHomework(8L, 70L, "연습", null, null));

        when(homeworkMapper.countActiveHomeworksByMonitoringId(70L)).thenReturn(4);
        assertCode(ErrorCode.COMMON_CONFLICT, () -> homeworkService.createHomework(8L, 70L, "연습", null, null));
        verify(homeworkMapper, never()).insertHomework(any());
        verify(homeworkMapper, never()).selectActiveHomeworkByIdAndMonitoringId(any(), any());
    }

    @Test
    void createMapsMonitoringLockTimeoutToOrderConflict() {
        stubOwnedMonitoring();
        doThrow(new CannotAcquireLockException("ORA-30006"))
                .when(studentMonitoringMapper).lockStudentMonitoringById(70L);

        assertCode(ErrorCode.ORDER_CONFLICT, () -> homeworkService.createHomework(8L, 70L, "연습", null, null));
        verify(homeworkMapper, never()).countActiveHomeworksByMonitoringId(any());
        verify(homeworkMapper, never()).insertHomework(any());
    }

    @Test
    void readsHomeworksInsideTheMonitoring() {
        stubOwnedMonitoring();
        when(homeworkMapper.selectActiveHomeworksByMonitoringId(70L)).thenReturn(List.of(saved(90L, 70L, "연습", null, false, null)));
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L))
                .thenReturn(saved(90L, 70L, "연습", null, false, null));
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(91L, 70L)).thenReturn(null);

        assertThat(homeworkService.getHomeworks(8L, 70L)).hasSize(1);
        when(homeworkMapper.selectActiveHomeworksByMonitoringId(70L)).thenReturn(List.of());
        assertThat(homeworkService.getHomeworks(8L, 70L)).isEmpty();
        assertThat(homeworkService.getHomework(8L, 70L, 90L).getHomeworkId()).isEqualTo(90L);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> homeworkService.getHomework(8L, 70L, 91L));
    }

    @Test
    void updateChangesSpecifiedFieldsAndLeavesMonitoringProgress() {
        stubOwnedMonitoring();
        Homework current = saved(90L, 70L, "기존", DEADLINE, false, "기존 피드백");
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L)).thenReturn(current);
        when(homeworkMapper.lockHomeworkById(90L)).thenReturn(current);
        when(homeworkMapper.updateHomework(current)).thenReturn(1);

        HomeworkChange change = new HomeworkChange();
        change.setHomeworkContent("수정");
        change.setDeadline(null);
        change.setCompleted(true);
        change.setFeedback(null);
        Homework updated = homeworkService.updateHomework(8L, 70L, 90L, change);

        assertThat(updated.getHomeworkContent()).isEqualTo("수정");
        assertThat(updated.getDeadline()).isNull();
        assertThat(updated.getCompleted()).isTrue();
        assertThat(updated.getFeedback()).isNull();
        assertThat(updated.getMonitoringId()).isEqualTo(70L);

        HomeworkChange backward = new HomeworkChange();
        backward.setCompleted(false);
        backward.setDeadline(DEADLINE);
        backward.setFeedback("다시");
        Homework reverted = homeworkService.updateHomework(8L, 70L, 90L, backward);
        assertThat(reverted.getCompleted()).isFalse();
        assertThat(reverted.getDeadline()).isEqualTo(DEADLINE);
        assertThat(reverted.getFeedback()).isEqualTo("다시");
        verify(studentMonitoringMapper, never()).updateStudentMonitoring(any());
    }

    @Test
    void updateRejectsInvalidContentOrCompletedAndBadWriteCount() {
        stubOwnedMonitoring();
        Homework current = saved(90L, 70L, "기존", null, false, null);
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L)).thenReturn(current);
        when(homeworkMapper.lockHomeworkById(90L)).thenReturn(current);

        HomeworkChange blank = new HomeworkChange();
        blank.setHomeworkContent(" ");
        assertCode(ErrorCode.COMMON_INVALID_INPUT, () -> homeworkService.updateHomework(8L, 70L, 90L, blank));
        HomeworkChange missing = new HomeworkChange();
        missing.setHomeworkContent(null);
        assertCode(ErrorCode.COMMON_INVALID_INPUT, () -> homeworkService.updateHomework(8L, 70L, 90L, missing));
        HomeworkChange completed = new HomeworkChange();
        completed.setCompleted(null);
        assertCode(ErrorCode.COMMON_INVALID_INPUT, () -> homeworkService.updateHomework(8L, 70L, 90L, completed));

        HomeworkChange valid = new HomeworkChange();
        valid.setHomeworkContent("유지");
        when(homeworkMapper.updateHomework(current)).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> homeworkService.updateHomework(8L, 70L, 90L, valid));
    }

    @Test
    void deleteSoftDeletesWithoutTouchingMonitoring() {
        stubOwnedMonitoring();
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L)).thenReturn(saved(90L, 70L, "연습", null, false, null));
        when(homeworkMapper.lockHomeworkById(90L)).thenReturn(saved(90L, 70L, "연습", null, false, null));
        when(homeworkMapper.softDeleteHomework(90L, 70L)).thenReturn(1);

        homeworkService.deleteHomework(8L, 70L, 90L);

        verify(homeworkMapper).softDeleteHomework(90L, 70L);
        verify(studentMonitoringMapper, never()).updateStudentMonitoring(any());
        verify(studentMonitoringMapper, never()).softDeleteStudentMonitoring(any(), any());

        when(homeworkMapper.softDeleteHomework(90L, 70L)).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> homeworkService.deleteHomework(8L, 70L, 90L));
    }

    @Test
    void restoreKeepsStoredFieldsAndRejectsActiveOrForeignRows() {
        stubOwnedMonitoring();
        stubParentLock();
        Homework inactive = saved(90L, 70L, "싱글 스트로크 10분", DEADLINE, true, "좋음");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(homeworkMapper.selectHomeworkById(90L)).thenReturn(inactive);
        when(homeworkMapper.lockHomeworkById(90L)).thenReturn(inactive);
        when(homeworkMapper.restoreHomework(inactive)).thenReturn(1);
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L))
                .thenReturn(saved(90L, 70L, "싱글 스트로크 10분", DEADLINE, true, "좋음"));

        Homework restored = homeworkService.restoreHomework(8L, 70L, 90L);

        assertThat(inactive.getHomeworkContent()).isEqualTo("싱글 스트로크 10분");
        assertThat(inactive.getDeadline()).isEqualTo(DEADLINE);
        assertThat(inactive.getCompleted()).isTrue();
        assertThat(inactive.getFeedback()).isEqualTo("좋음");
        assertThat(restored.getHomeworkContent()).isEqualTo("싱글 스트로크 10분");
        verify(homeworkMapper, never()).updateHomework(any());

        when(homeworkMapper.selectHomeworkById(91L)).thenReturn(saved(91L, 70L, "활성", null, false, null));
        assertCode(ErrorCode.COMMON_CONFLICT, () -> homeworkService.restoreHomework(8L, 70L, 91L));

        Homework other = saved(92L, 71L, "다른", null, false, null);
        other.setStatus(RecordStatus.INACTIVE);
        when(homeworkMapper.selectHomeworkById(92L)).thenReturn(other);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> homeworkService.restoreHomework(8L, 70L, 92L));

        when(studentMonitoringMapper.selectActiveStudentMonitoringById(70L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> homeworkService.restoreHomework(8L, 70L, 90L));
        verify(homeworkMapper, org.mockito.Mockito.times(1)).restoreHomework(any());
    }

    @Test
    void restoreRejectsBadWriteCount() {
        stubOwnedMonitoring();
        stubParentLock();
        Homework inactive = saved(90L, 70L, "연습", DEADLINE, true, "좋음");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(homeworkMapper.selectHomeworkById(90L)).thenReturn(inactive);
        when(homeworkMapper.lockHomeworkById(90L)).thenReturn(inactive);
        when(homeworkMapper.restoreHomework(inactive)).thenReturn(0);

        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> homeworkService.restoreHomework(8L, 70L, 90L));
        assertThat(inactive.getCompleted()).isTrue();
    }

    @Test
    void mapsHomeworkLockTimeoutToOrderConflict() {
        stubOwnedMonitoring();
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L))
                .thenReturn(saved(90L, 70L, "연습", null, false, null));
        doThrow(new CannotAcquireLockException("ORA-30006")).when(homeworkMapper).lockHomeworkById(90L);

        HomeworkChange change = new HomeworkChange();
        change.setFeedback("수정");
        assertCode(ErrorCode.ORDER_CONFLICT, () -> homeworkService.updateHomework(8L, 70L, 90L, change));
        assertCode(ErrorCode.ORDER_CONFLICT, () -> homeworkService.deleteHomework(8L, 70L, 90L));
        verify(homeworkMapper, never()).updateHomework(any());
        verify(homeworkMapper, never()).softDeleteHomework(any(), any());
    }

    @Test
    void restoreAllowsAnInactiveHomeworkWhenFewerThanThreeAreActive() {
        stubOwnedMonitoring();
        stubParentLock();
        Homework inactive = saved(90L, 70L, "연습", null, false, null);
        inactive.setStatus(RecordStatus.INACTIVE);
        when(homeworkMapper.selectHomeworkById(90L)).thenReturn(inactive);
        when(homeworkMapper.countActiveHomeworksByMonitoringId(70L)).thenReturn(2);
        when(homeworkMapper.lockHomeworkById(90L)).thenReturn(inactive);
        when(homeworkMapper.restoreHomework(inactive)).thenReturn(1);
        when(homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(90L, 70L))
                .thenReturn(saved(90L, 70L, "연습", null, false, null));

        assertThat(homeworkService.restoreHomework(8L, 70L, 90L).getHomeworkId()).isEqualTo(90L);

        InOrder order = inOrder(studentMonitoringMapper, homeworkMapper);
        order.verify(studentMonitoringMapper).lockStudentMonitoringById(70L);
        order.verify(homeworkMapper).countActiveHomeworksByMonitoringId(70L);
        order.verify(homeworkMapper).restoreHomework(inactive);
    }

    @Test
    void restoreRejectsWhenThreeHomeworksAreAlreadyActive() {
        stubOwnedMonitoring();
        stubParentLock();
        Homework inactive = saved(90L, 70L, "연습", null, false, null);
        inactive.setStatus(RecordStatus.INACTIVE);
        when(homeworkMapper.selectHomeworkById(90L)).thenReturn(inactive);
        when(homeworkMapper.countActiveHomeworksByMonitoringId(70L)).thenReturn(3);

        assertCode(ErrorCode.COMMON_CONFLICT, () -> homeworkService.restoreHomework(8L, 70L, 90L));
        verify(homeworkMapper, never()).restoreHomework(any());
        verify(homeworkMapper, never()).lockHomeworkById(any());
    }

    private void stubParentLock() {
        when(studentMonitoringMapper.lockStudentMonitoringById(70L)).thenReturn(monitoring(50L));
    }

    private void stubOwnedMonitoring() {
        when(studentMonitoringMapper.selectActiveStudentMonitoringById(70L)).thenReturn(monitoring(50L));
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(50L)).thenReturn(enrollment());
        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(location());
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.ACTIVE));
    }

    private StudentMonitoring monitoring(Long studentCurriculumId) {
        StudentMonitoring monitoring = new StudentMonitoring();
        monitoring.setMonitoringId(70L);
        monitoring.setStudentCurriculumId(studentCurriculumId);
        monitoring.setStatus(RecordStatus.ACTIVE);
        return monitoring;
    }

    private StudentCurriculum enrollment() {
        StudentCurriculum enrollment = new StudentCurriculum();
        enrollment.setStudentCurriculumId(50L);
        enrollment.setTeacherStudentLocationId(30L);
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

    private Homework saved(
            Long homeworkId,
            Long monitoringId,
            String content,
            LocalDateTime deadline,
            boolean completed,
            String feedback) {
        Homework homework = new Homework();
        homework.setHomeworkId(homeworkId);
        homework.setMonitoringId(monitoringId);
        homework.setHomeworkContent(content);
        homework.setDeadline(deadline);
        homework.setCompleted(completed);
        homework.setFeedback(feedback);
        homework.setStatus(RecordStatus.ACTIVE);
        return homework;
    }

    private Homework argWithBlankContent() {
        return org.mockito.ArgumentMatchers.argThat(homework -> homework.getHomeworkContent() == null
                || homework.getHomeworkContent().isBlank());
    }

    private void assertCode(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(errorCode);
    }
}
