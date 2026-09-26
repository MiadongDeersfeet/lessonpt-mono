package com.yunki.lessonpt.student.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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

import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.ProgressStatus;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.service.ProgressQueryService;
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgressView;
import com.yunki.lessonpt.student.dto.StudentContentView;
import com.yunki.lessonpt.student.dto.StudentLearningResponse;
import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.student.query.StudentPortalContentRow;
import com.yunki.lessonpt.student.query.StudentPortalEnrollment;
import com.yunki.lessonpt.student.query.StudentPortalHomeworkRow;
import com.yunki.lessonpt.student.query.StudentPortalIdentity;
import com.yunki.lessonpt.student.query.StudentPortalMonitoringRow;

@ExtendWith(MockitoExtension.class)
class StudentPortalServiceTest {

    private static final StudentPrincipal PRINCIPAL = new StudentPrincipal(90L, 72L, 41L);

    @Mock
    private StudentPortalMapper studentPortalMapper;

    @Mock
    private ProgressQueryMapper progressQueryMapper;

    @Mock
    private ContentResourceMapper contentResourceMapper;

    private StudentPortalService studentPortalService;

    @BeforeEach
    void setUp() {
        studentPortalService = new StudentPortalService(
                studentPortalMapper,
                progressQueryMapper,
                new ProgressQueryService(null, null, null, null, null),
                contentResourceMapper);
    }

    @Test
    void returnsOwnNameAndHidesContactFields() {
        when(studentPortalMapper.selectActiveStudentName(41L)).thenReturn("학생");

        assertThat(studentPortalService.me(PRINCIPAL).name()).isEqualTo("학생");
        assertThat(studentPortalService.me(new StudentPrincipal(null, null, 41L)).name()).isEqualTo("학생");
    }

    @Test
    void learningWithoutScopeIsNotAnAuthenticationFailure() {
        assertThatThrownBy(() -> studentPortalService.learning(new StudentPrincipal(null, null, 41L)))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.STUDENT_SCOPE_REQUIRED);
        verify(studentPortalMapper, never()).selectActiveEnrollments(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsBrokenAccessChain() {
        when(studentPortalMapper.selectActiveIdentity(90L)).thenReturn(null);
        assertThatThrownBy(() -> studentPortalService.learning(PRINCIPAL))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);

        StudentPortalIdentity other = identity();
        other.setStudentId(99L);
        when(studentPortalMapper.selectActiveIdentity(90L)).thenReturn(other);
        assertThatThrownBy(() -> studentPortalService.learning(PRINCIPAL))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);

        when(studentPortalMapper.selectActiveStudentName(41L)).thenReturn(null);
        assertThatThrownBy(() -> studentPortalService.me(PRINCIPAL))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_FAILED);
    }

    @Test
    void emptyEnrollmentSkipsBatchQueries() {
        when(studentPortalMapper.selectActiveIdentity(90L)).thenReturn(identity());
        when(studentPortalMapper.selectActiveEnrollments(90L)).thenReturn(List.of());

        assertThat(studentPortalService.learning(PRINCIPAL).curriculums()).isEmpty();
        verify(studentPortalMapper, never()).selectActiveContents(org.mockito.ArgumentMatchers.anyList());
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void assemblesPublicLearningTreeOncePerBatch() {
        when(studentPortalMapper.selectActiveIdentity(90L)).thenReturn(identity());
        when(studentPortalMapper.selectActiveEnrollments(90L)).thenReturn(List.of(enrollment(5L, 8L, "드럼")));
        when(studentPortalMapper.selectActiveContents(List.of(8L))).thenReturn(List.of(
                content(8L, 1L, "기초", 11L, "싱글"),
                content(8L, 2L, "응용", 12L, "파라디들")));
        StudentPortalMonitoringRow learned = new StudentPortalMonitoringRow();
        learned.setStudentCurriculumId(5L);
        learned.setContentDetailId(11L);
        learned.setCurrentBpm(100);
        learned.setProgressStatus(ProgressStatus.IN_PROGRESS);
        when(studentPortalMapper.selectActiveMonitoring(List.of(5L))).thenReturn(List.of(learned));
        StudentPortalHomeworkRow homework = new StudentPortalHomeworkRow();
        homework.setStudentCurriculumId(5L);
        homework.setContentDetailId(11L);
        homework.setHomeworkContent("메트로놈");
        homework.setDeadline(LocalDateTime.of(2026, 10, 1, 0, 0));
        homework.setCompleted(false);
        homework.setFeedback("좋아요");
        when(studentPortalMapper.selectActiveHomework(List.of(5L))).thenReturn(List.of(homework));
        when(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of(5L))).thenReturn(List.of(progress(5L, 1, 4)));

        StudentLearningResponse response = studentPortalService.learning(PRINCIPAL);

        assertThat(response.curriculums()).hasSize(1);
        assertThat(response.curriculums().get(0).progress().percentage()).isEqualByComparingTo(new BigDecimal("25.0"));
        assertThat(response.curriculums().get(0).categories()).hasSize(2);
        StudentContentView withMonitoring = response.curriculums().get(0).categories().get(0).contents().get(0);
        assertThat(withMonitoring.currentBpm()).isEqualTo(100);
        assertThat(withMonitoring.progressStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(withMonitoring.homeworks()).hasSize(1);
        StudentContentView withoutMonitoring = response.curriculums().get(0).categories().get(1).contents().get(0);
        assertThat(withoutMonitoring.currentBpm()).isNull();
        assertThat(withoutMonitoring.progressStatus()).isNull();
        assertThat(withoutMonitoring.homeworks()).isEmpty();
        verify(studentPortalMapper).selectActiveContents(List.of(8L));
        verify(studentPortalMapper).selectActiveMonitoring(List.of(5L));
        verify(studentPortalMapper).selectActiveHomework(List.of(5L));
    }

    @Test
    void omitsProgressWhenDenominatorIsZeroAndIgnoresStoppedAsIncomplete() {
        when(studentPortalMapper.selectActiveIdentity(90L)).thenReturn(identity());
        when(studentPortalMapper.selectActiveEnrollments(90L)).thenReturn(List.of(enrollment(5L, 8L, "드럼")));
        when(studentPortalMapper.selectActiveContents(List.of(8L))).thenReturn(List.of());
        when(studentPortalMapper.selectActiveMonitoring(List.of(5L))).thenReturn(List.of());
        when(studentPortalMapper.selectActiveHomework(List.of(5L))).thenReturn(List.of());
        when(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of(5L))).thenReturn(List.of(progress(5L, 0, 0)));

        assertThat(studentPortalService.learning(PRINCIPAL).curriculums().get(0).progress()).isNull();

        when(progressQueryMapper.selectProgressByStudentCurriculumIds(List.of(5L))).thenReturn(List.of(progress(5L, 1, 4)));
        assertThat(studentPortalService.learning(PRINCIPAL).curriculums().get(0).progress().completedCount()).isEqualTo(1);
    }

    private StudentPortalIdentity identity() {
        StudentPortalIdentity identity = new StudentPortalIdentity();
        identity.setTeacherStudentId(72L);
        identity.setStudentId(41L);
        identity.setName("학생");
        return identity;
    }

    private StudentPortalEnrollment enrollment(Long studentCurriculumId, Long curriculumId, String name) {
        StudentPortalEnrollment enrollment = new StudentPortalEnrollment();
        enrollment.setStudentCurriculumId(studentCurriculumId);
        enrollment.setCurriculumId(curriculumId);
        enrollment.setCurriculumName(name);
        enrollment.setCurriculumDisplayOrder(1);
        return enrollment;
    }

    private StudentPortalContentRow content(
            Long curriculumId, Long categoryId, String categoryName, Long contentId, String contentName) {
        StudentPortalContentRow row = new StudentPortalContentRow();
        row.setCurriculumId(curriculumId);
        row.setCategoryId(categoryId);
        row.setCategoryName(categoryName);
        row.setContentDetailId(contentId);
        row.setContentName(contentName);
        row.setTargetBpm(120);
        row.setYoutubeUrl("video");
        return row;
    }

    private StudentCurriculumProgressView progress(Long id, int completed, int total) {
        StudentCurriculumProgressView view = new StudentCurriculumProgressView();
        view.setStudentCurriculumId(id);
        view.setCompletedCount(completed);
        view.setTotalCount(total);
        return view;
    }
}
