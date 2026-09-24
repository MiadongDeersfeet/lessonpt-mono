package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgress;
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgressView;

@ExtendWith(MockitoExtension.class)
class ProgressQueryServiceTest {

    @Mock
    private StudentCurriculumMapper studentCurriculumMapper;

    @Mock
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Mock
    private TeacherStudentMapper teacherStudentMapper;

    @Mock
    private CurriculumMapper curriculumMapper;

    @Mock
    private ProgressQueryMapper progressQueryMapper;

    private ProgressQueryService progressQueryService;

    @BeforeEach
    void setUp() {
        progressQueryService = new ProgressQueryService(
                studentCurriculumMapper,
                teacherStudentLocationMapper,
                teacherStudentMapper,
                curriculumMapper,
                progressQueryMapper);
    }

    @Test
    void calculatesOneDecimalPercentage() {
        assertPercentage(0, 4, "0.0");
        assertPercentage(1, 4, "25.0");
        assertPercentage(2, 4, "50.0");
        assertPercentage(1, 3, "33.3");
        assertPercentage(2, 3, "66.7");
        assertPercentage(3, 3, "100.0");
    }

    @Test
    void omitsProgressWhenThereIsNoActiveContent() {
        stubOwnedEnrollment();
        when(progressQueryMapper.selectProgressByStudentCurriculumId(90L)).thenReturn(view(90L, 0, 0));

        assertThat(progressQueryService.getProgress(8L, 90L)).isEmpty();
    }

    @Test
    void rejectsInconsistentCounts() {
        stubOwnedEnrollment();
        when(progressQueryMapper.selectProgressByStudentCurriculumId(90L)).thenReturn(view(90L, 5, 4));
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> progressQueryService.getProgress(8L, 90L));

        when(progressQueryMapper.selectProgressByStudentCurriculumId(90L)).thenReturn(view(90L, -1, 4));
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> progressQueryService.getProgress(8L, 90L));

        when(progressQueryMapper.selectProgressByStudentCurriculumId(90L)).thenReturn(view(90L, 1, -1));
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> progressQueryService.getProgress(8L, 90L));

        when(progressQueryMapper.selectProgressByStudentCurriculumId(90L)).thenReturn(view(90L, null, 4));
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> progressQueryService.getProgress(8L, 90L));

        when(progressQueryMapper.selectProgressByStudentCurriculumId(90L)).thenReturn(view(90L, 1, null));
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> progressQueryService.getProgress(8L, 90L));
    }

    @Test
    void rejectsMissingProgressRowAfterOwnership() {
        stubOwnedEnrollment();
        when(progressQueryMapper.selectProgressByStudentCurriculumId(90L)).thenReturn(null);

        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> progressQueryService.getProgress(8L, 90L));
    }

    @Test
    void rejectsUnownedEnrollment() {
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgress(8L, 90L));

        StudentCurriculum inactive = enrollment();
        inactive.setStatus(RecordStatus.INACTIVE);
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgress(8L, 90L));

        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L)).thenReturn(enrollment());
        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(location());
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(9L, RecordStatus.ACTIVE, null));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgress(8L, 90L));

        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgress(8L, 90L));

        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(location());
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.INACTIVE, null));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgress(8L, 90L));

        when(teacherStudentMapper.selectTeacherStudentById(20L))
                .thenReturn(relation(8L, RecordStatus.ACTIVE, LocalDateTime.parse("2026-09-24T10:00:00")));
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgress(8L, 90L));

        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.ACTIVE, null));
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgress(8L, 90L));

        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgress(8L, 90L));
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumId(any());
    }

    @Test
    void returnsEmptyListWithoutQueryingWhenIdsAreEmpty() {
        assertThat(progressQueryService.getProgresses(8L, List.of())).isEmpty();
        verify(progressQueryMapper, never()).selectOwnedActiveStudentCurriculumIds(any(), any());
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumIds(any());
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumId(any());
    }

    @Test
    void calculatesBatchProgressOnceAndOmitsZeroTotals() {
        when(progressQueryMapper.selectOwnedActiveStudentCurriculumIds(8L, List.of(10L, 20L)))
                .thenReturn(List.of(10L, 20L));
        when(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of(10L, 20L)))
                .thenReturn(List.of(view(20L, 1, 4), view(10L, 0, 0)));

        List<StudentCurriculumProgress> progresses = progressQueryService.getProgresses(8L, List.of(20L, 10L, 10L));

        assertThat(progresses).containsExactly(progress(20L, 1, 4, "25.0"));
        verify(progressQueryMapper).selectOwnedActiveStudentCurriculumIds(8L, List.of(10L, 20L));
        verify(progressQueryMapper).selectProgressByStudentCurriculumIds(List.of(10L, 20L));
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumId(any());
        verify(studentCurriculumMapper, never()).selectActiveStudentCurriculumById(any());
    }

    @Test
    void rejectsBatchWhenAnyEnrollmentIsUnowned() {
        when(progressQueryMapper.selectOwnedActiveStudentCurriculumIds(8L, List.of(10L, 20L)))
                .thenReturn(List.of(10L));

        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> progressQueryService.getProgresses(8L, List.of(10L, 20L)));
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumIds(any());
    }

    @Test
    void rejectsBatchWhenAProgressRowIsMissingOrInconsistent() {
        when(progressQueryMapper.selectOwnedActiveStudentCurriculumIds(8L, List.of(10L, 20L)))
                .thenReturn(List.of(10L, 20L));
        when(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of(10L, 20L)))
                .thenReturn(List.of(view(10L, 1, 4)));
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> progressQueryService.getProgresses(8L, List.of(10L, 20L)));

        when(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of(10L, 20L)))
                .thenReturn(List.of(view(10L, 1, 4), view(20L, 5, 4)));
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> progressQueryService.getProgresses(8L, List.of(10L, 20L)));
    }

    private void assertPercentage(int completedCount, int totalCount, String percentage) {
        stubOwnedEnrollment();
        when(progressQueryMapper.selectProgressByStudentCurriculumId(90L))
                .thenReturn(view(90L, completedCount, totalCount));

        Optional<StudentCurriculumProgress> progress = progressQueryService.getProgress(8L, 90L);

        assertThat(progress).contains(progress(90L, completedCount, totalCount, percentage));
        assertThat(progress.orElseThrow().percentage().scale()).isEqualTo(1);
    }

    private void stubOwnedEnrollment() {
        when(studentCurriculumMapper.selectActiveStudentCurriculumById(90L)).thenReturn(enrollment());
        when(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(30L)).thenReturn(location());
        when(teacherStudentMapper.selectTeacherStudentById(20L)).thenReturn(relation(8L, RecordStatus.ACTIVE, null));
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(new Curriculum());
    }

    private static StudentCurriculum enrollment() {
        StudentCurriculum enrollment = new StudentCurriculum();
        enrollment.setStudentCurriculumId(90L);
        enrollment.setTeacherStudentLocationId(30L);
        enrollment.setCurriculumId(40L);
        enrollment.setStatus(RecordStatus.ACTIVE);
        return enrollment;
    }

    private static TeacherStudentLocation location() {
        TeacherStudentLocation location = new TeacherStudentLocation();
        location.setTeacherStudentLocationId(30L);
        location.setTeacherStudentId(20L);
        location.setStatus(RecordStatus.ACTIVE);
        return location;
    }

    private static TeacherStudent relation(Long teacherId, RecordStatus status, LocalDateTime deletedAt) {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherStudentId(20L);
        relation.setTeacherId(teacherId);
        relation.setStatus(status);
        relation.setDeletedAt(deletedAt);
        return relation;
    }

    private static StudentCurriculumProgressView view(Long studentCurriculumId, Integer completedCount, Integer totalCount) {
        StudentCurriculumProgressView view = new StudentCurriculumProgressView();
        view.setStudentCurriculumId(studentCurriculumId);
        view.setCompletedCount(completedCount);
        view.setTotalCount(totalCount);
        return view;
    }

    private static StudentCurriculumProgress progress(
            Long studentCurriculumId, int completedCount, int totalCount, String percentage) {
        return new StudentCurriculumProgress(
                studentCurriculumId, completedCount, totalCount, new BigDecimal(percentage));
    }

    private static void assertCode(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(errorCode);
    }
}
