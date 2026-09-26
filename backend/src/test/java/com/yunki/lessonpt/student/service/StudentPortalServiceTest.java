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
import com.yunki.lessonpt.student.query.StudentPortalCategoryRow;
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
        verify(studentPortalMapper, never()).selectActiveCategories(org.mockito.ArgumentMatchers.anyList());
        verify(studentPortalMapper, never()).selectActiveMonitoringContents(org.mockito.ArgumentMatchers.anyList());
        verify(progressQueryMapper, never()).selectProgressByStudentCurriculumIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void assemblesPublicLearningTreeOncePerBatch() {
        when(studentPortalMapper.selectActiveIdentity(90L)).thenReturn(identity());
        when(studentPortalMapper.selectActiveEnrollments(90L)).thenReturn(List.of(enrollment(5L, 8L, "드럼")));
        when(studentPortalMapper.selectActiveCategories(List.of(8L))).thenReturn(List.of(
                category(8L, 1L, "기초", 3),
                category(8L, 2L, "응용", 5),
                category(8L, 3L, "빈칸", 0)));
        StudentPortalMonitoringRow single = published(5L, 1L, 11L, 501L, "싱글");
        single.setCurrentBpm(100);
        single.setProgressStatus(ProgressStatus.IN_PROGRESS);
        single.setTargetBpm(120);
        single.setYoutubeUrl("video");
        when(studentPortalMapper.selectActiveMonitoringContents(List.of(5L))).thenReturn(List.of(
                single,
                published(5L, 1L, 12L, 502L, "더블")));
        StudentPortalHomeworkRow homework = new StudentPortalHomeworkRow();
        homework.setHomeworkId(77L);
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
        assertThat(response.curriculums().get(0).categories()).hasSize(3);
        assertThat(response.curriculums().get(0).categories().get(0).name()).isEqualTo("기초");
        assertThat(response.curriculums().get(0).categories().get(0).totalContentCount()).isEqualTo(3);
        assertThat(response.curriculums().get(0).categories().get(0).contents()).hasSize(2);
        assertThat(response.curriculums().get(0).categories().get(1).name()).isEqualTo("응용");
        assertThat(response.curriculums().get(0).categories().get(1).totalContentCount()).isEqualTo(5);
        assertThat(response.curriculums().get(0).categories().get(1).contents()).isEmpty();
        assertThat(response.curriculums().get(0).categories().get(2).name()).isEqualTo("빈칸");
        assertThat(response.curriculums().get(0).categories().get(2).totalContentCount()).isEqualTo(0);
        assertThat(response.curriculums().get(0).categories().get(2).contents()).isEmpty();
        StudentContentView withMonitoring = response.curriculums().get(0).categories().get(0).contents().get(0);
        assertThat(withMonitoring.monitoringId()).isEqualTo(501L);
        assertThat(withMonitoring.name()).isEqualTo("싱글");
        assertThat(withMonitoring.currentBpm()).isEqualTo(100);
        assertThat(withMonitoring.progressStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(withMonitoring.homeworks()).hasSize(1);
        assertThat(withMonitoring.homeworks().get(0).homeworkId()).isEqualTo(77L);
        assertThat(response.curriculums().get(0).categories().stream()
                .flatMap(category -> category.contents().stream())
                .map(StudentContentView::name)
                .toList()).containsExactly("싱글", "더블");
        verify(contentResourceMapper).selectActiveByContentDetailIds(List.of(11L, 12L));
        verify(studentPortalMapper).selectActiveCategories(List.of(8L));
        verify(studentPortalMapper).selectActiveMonitoringContents(List.of(5L));
        verify(studentPortalMapper).selectActiveHomework(List.of(5L));
    }

    @Test
    void omitsProgressWhenDenominatorIsZeroAndIgnoresStoppedAsIncomplete() {
        when(studentPortalMapper.selectActiveIdentity(90L)).thenReturn(identity());
        when(studentPortalMapper.selectActiveEnrollments(90L)).thenReturn(List.of(enrollment(5L, 8L, "드럼")));
        when(studentPortalMapper.selectActiveCategories(List.of(8L))).thenReturn(List.of());
        when(studentPortalMapper.selectActiveMonitoringContents(List.of(5L))).thenReturn(List.of());
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

    private StudentPortalCategoryRow category(
            Long curriculumId, Long categoryId, String categoryName, int totalContentCount) {
        StudentPortalCategoryRow row = new StudentPortalCategoryRow();
        row.setCurriculumId(curriculumId);
        row.setCategoryId(categoryId);
        row.setCategoryName(categoryName);
        row.setTotalContentCount(totalContentCount);
        return row;
    }

    private StudentPortalMonitoringRow published(
            Long studentCurriculumId, Long categoryId, Long contentDetailId, Long monitoringId, String contentName) {
        StudentPortalMonitoringRow row = new StudentPortalMonitoringRow();
        row.setStudentCurriculumId(studentCurriculumId);
        row.setCategoryId(categoryId);
        row.setContentDetailId(contentDetailId);
        row.setMonitoringId(monitoringId);
        row.setContentName(contentName);
        row.setProgressStatus(ProgressStatus.YET);
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
