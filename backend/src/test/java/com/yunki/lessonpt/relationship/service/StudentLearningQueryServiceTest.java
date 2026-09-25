package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.ProgressStatus;
import com.yunki.lessonpt.relationship.dto.StudentLearningCurriculumResponse;
import com.yunki.lessonpt.relationship.dto.StudentLearningDetailResponse;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLearningQueryMapper;
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgressView;
import com.yunki.lessonpt.relationship.query.StudentLearningCurriculumRow;
import com.yunki.lessonpt.relationship.query.StudentLearningHomeworkRow;
import com.yunki.lessonpt.relationship.query.StudentLearningLocationRow;
import com.yunki.lessonpt.relationship.query.StudentLearningMonitoringRow;
import com.yunki.lessonpt.relationship.query.StudentLearningStudentRow;

@ExtendWith(MockitoExtension.class)
class StudentLearningQueryServiceTest {

    @Mock
    private StudentLearningQueryMapper studentLearningQueryMapper;

    @Mock
    private ProgressQueryMapper progressQueryMapper;

    private StudentLearningQueryService studentLearningQueryService;

    @BeforeEach
    void setUp() {
        studentLearningQueryService = new StudentLearningQueryService(
                studentLearningQueryMapper,
                progressQueryMapper,
                new ProgressQueryService(null, null, null, null, null));
    }

    @Test
    void rejectsStudentOutsideTheAuthenticatedTeacher() {
        when(studentLearningQueryMapper.selectActiveStudent(8L, 41L)).thenReturn(null);

        assertThatThrownBy(() -> studentLearningQueryService.learning(8L, 41L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(studentLearningQueryMapper, never()).selectActiveLocations(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumIds(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void assemblesBatchRowsWithoutPerRowMapperCalls() {
        when(studentLearningQueryMapper.selectActiveStudent(8L, 41L)).thenReturn(student());
        when(studentLearningQueryMapper.selectActiveLocations(8L, 20L)).thenReturn(List.of(location(30L), location(31L)));
        when(studentLearningQueryMapper.selectActiveCurriculums(8L, List.of(30L, 31L)))
                .thenReturn(List.of(curriculum(30L, 90L, "기초"), curriculum(31L, 91L, "빈 커리큘럼")));
        when(studentLearningQueryMapper.selectActiveMonitorings(List.of(90L, 91L)))
                .thenReturn(List.of(monitoring(90L, 70L), monitoring(90L, 71L)));
        when(studentLearningQueryMapper.selectActiveHomeworks(List.of(70L, 71L))).thenReturn(List.of(homework(70L)));
        when(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of(90L, 91L)))
                .thenReturn(List.of(progress(90L, 1, 4), progress(91L, 0, 0)));

        StudentLearningDetailResponse response = studentLearningQueryService.learning(8L, 41L);

        assertThat(response.studentId()).isEqualTo(41L);
        assertThat(response.locations()).hasSize(2);
        StudentLearningCurriculumResponse withMonitoring = response.locations().get(0).studentCurriculums().get(0);
        assertThat(withMonitoring.curriculumName()).isEqualTo("기초");
        assertThat(withMonitoring.memo()).isEqualTo("강사 메모");
        assertThat(withMonitoring.progress().completedCount()).isEqualTo(1);
        assertThat(withMonitoring.progress().totalCount()).isEqualTo(4);
        assertThat(withMonitoring.progress().percentage()).isEqualByComparingTo(new BigDecimal("25.0"));
        assertThat(withMonitoring.monitorings()).hasSize(2);
        assertThat(withMonitoring.monitorings().get(0).homeworks()).hasSize(1);
        assertThat(withMonitoring.monitorings().get(0).homeworks().get(0).homeworkContent()).isEqualTo("메트로놈");
        assertThat(withMonitoring.monitorings().get(0).memo()).isEqualTo("모니터링 메모");
        assertThat(withMonitoring.monitorings().get(1).homeworks()).isEmpty();

        StudentLearningCurriculumResponse withoutMonitoring = response.locations().get(1).studentCurriculums().get(0);
        assertThat(withoutMonitoring.studentCurriculumId()).isEqualTo(91L);
        assertThat(withoutMonitoring.monitorings()).isEmpty();
        assertThat(withoutMonitoring.progress()).isNull();

        verify(studentLearningQueryMapper, times(1)).selectActiveStudent(8L, 41L);
        verify(studentLearningQueryMapper, times(1)).selectActiveLocations(8L, 20L);
        verify(studentLearningQueryMapper, times(1)).selectActiveCurriculums(8L, List.of(30L, 31L));
        verify(studentLearningQueryMapper, times(1)).selectActiveMonitorings(List.of(90L, 91L));
        verify(studentLearningQueryMapper, times(1)).selectActiveHomeworks(List.of(70L, 71L));
        verify(progressQueryMapper, times(1)).selectProgressByStudentCurriculumIds(List.of(90L, 91L));
    }

    private static StudentLearningStudentRow student() {
        StudentLearningStudentRow row = new StudentLearningStudentRow();
        row.setTeacherStudentId(20L);
        row.setStudentId(41L);
        row.setName("학생");
        row.setEmail("student@lessonpt.local");
        row.setPhone(null);
        return row;
    }

    private static StudentLearningLocationRow location(Long teacherStudentLocationId) {
        StudentLearningLocationRow row = new StudentLearningLocationRow();
        row.setTeacherStudentLocationId(teacherStudentLocationId);
        row.setLocationId(teacherStudentLocationId + 100);
        row.setLocationName("연습실");
        row.setAddress("서울");
        return row;
    }

    private static StudentLearningCurriculumRow curriculum(Long locationId, Long studentCurriculumId, String name) {
        StudentLearningCurriculumRow row = new StudentLearningCurriculumRow();
        row.setTeacherStudentLocationId(locationId);
        row.setStudentCurriculumId(studentCurriculumId);
        row.setCurriculumId(studentCurriculumId + 10);
        row.setCurriculumName(name);
        row.setReenrolled(false);
        row.setMemo("강사 메모");
        return row;
    }

    private static StudentLearningMonitoringRow monitoring(Long studentCurriculumId, Long monitoringId) {
        StudentLearningMonitoringRow row = new StudentLearningMonitoringRow();
        row.setStudentCurriculumId(studentCurriculumId);
        row.setMonitoringId(monitoringId);
        row.setContentDetailId(monitoringId + 10);
        row.setContentDetailName("싱글");
        row.setDisplayOrder(1);
        row.setTargetBpm(120);
        row.setCurrentBpm(100);
        row.setProgressStatus(ProgressStatus.IN_PROGRESS);
        row.setMemo("모니터링 메모");
        return row;
    }

    private static StudentLearningHomeworkRow homework(Long monitoringId) {
        StudentLearningHomeworkRow row = new StudentLearningHomeworkRow();
        row.setMonitoringId(monitoringId);
        row.setHomeworkId(80L);
        row.setHomeworkContent("메트로놈");
        row.setDeadline(LocalDateTime.of(2026, 10, 1, 0, 0));
        row.setCompleted(false);
        row.setFeedback("좋아요");
        return row;
    }

    private static StudentCurriculumProgressView progress(Long id, int completed, int total) {
        StudentCurriculumProgressView view = new StudentCurriculumProgressView();
        view.setStudentCurriculumId(id);
        view.setCompletedCount(completed);
        view.setTotalCount(total);
        return view;
    }
}
