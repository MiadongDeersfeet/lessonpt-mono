package com.yunki.lessonpt.student.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgressView;
import com.yunki.lessonpt.student.dto.StudentCategoryView;
import com.yunki.lessonpt.student.dto.StudentContentView;
import com.yunki.lessonpt.student.dto.StudentCurriculumView;
import com.yunki.lessonpt.student.dto.StudentHomeworkView;
import com.yunki.lessonpt.student.dto.StudentLearningResponse;
import com.yunki.lessonpt.student.dto.StudentMeResponse;
import com.yunki.lessonpt.student.dto.StudentRelationshipLocationResponse;
import com.yunki.lessonpt.student.dto.StudentRelationshipResponse;
import com.yunki.lessonpt.relationship.service.ProgressQueryService;
import com.yunki.lessonpt.resource.dto.ResourcePair;
import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.student.query.StudentPortalCategoryRow;
import com.yunki.lessonpt.student.query.StudentPortalEnrollment;
import com.yunki.lessonpt.student.query.StudentPortalHomeworkRow;
import com.yunki.lessonpt.student.query.StudentPortalIdentity;
import com.yunki.lessonpt.student.query.StudentPortalMonitoringRow;
import com.yunki.lessonpt.student.query.StudentPortalRelationshipRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentPortalService {

    private final StudentPortalMapper studentPortalMapper;
    private final ProgressQueryMapper progressQueryMapper;
    private final ProgressQueryService progressQueryService;
    private final ContentResourceMapper contentResourceMapper;

    @Transactional(readOnly = true)
    public StudentMeResponse me(StudentPrincipal principal) {
        requireStudent(principal);
        String name = studentPortalMapper.selectActiveStudentName(principal.studentId());
        if (name == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        return new StudentMeResponse(name);
    }

    @Transactional(readOnly = true)
    public List<StudentRelationshipResponse> relationships(StudentPrincipal principal) {
        requireStudent(principal);
        return groupRelationships(studentPortalMapper.selectActiveRelationships(principal.studentId()));
    }

    @Transactional(readOnly = true)
    public StudentLearningResponse learning(StudentPrincipal principal) {
        requireStudent(principal);
        if (principal.teacherStudentAccessId() == null) {
            throw new BusinessException(ErrorCode.STUDENT_SCOPE_REQUIRED);
        }
        requireIdentity(principal);
        List<StudentPortalEnrollment> enrollments = studentPortalMapper.selectActiveEnrollments(
                principal.teacherStudentAccessId());
        if (enrollments.isEmpty()) {
            return new StudentLearningResponse(List.of());
        }
        List<Long> curriculumIds = enrollments.stream().map(StudentPortalEnrollment::getCurriculumId).distinct().toList();
        List<Long> enrollmentIds = enrollments.stream().map(StudentPortalEnrollment::getStudentCurriculumId).toList();
        List<StudentPortalCategoryRow> categories = studentPortalMapper.selectActiveCategories(curriculumIds);
        List<StudentPortalMonitoringRow> published = studentPortalMapper.selectActiveMonitoringContents(enrollmentIds);
        Map<Long, ResourcePair> resources = ResourcePair.byContentDetail(
                contentResourceMapper.selectActiveByContentDetailIds(publishedContentDetailIds(published)));
        List<StudentPortalHomeworkRow> homework = studentPortalMapper.selectActiveHomework(enrollmentIds);
        Map<Long, StudentCurriculumProgressView> progress = new LinkedHashMap<>();
        for (StudentCurriculumProgressView view : progressQueryMapper.selectProgressByStudentCurriculumIds(enrollmentIds)) {
            progress.put(view.getStudentCurriculumId(), view);
        }
        return new StudentLearningResponse(assemble(enrollments, categories, published, homework, progress, resources));
    }

    private void requireStudent(StudentPrincipal principal) {
        if (principal == null || principal.studentId() == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
    }

    private List<StudentRelationshipResponse> groupRelationships(List<StudentPortalRelationshipRow> rows) {
        Map<Long, StudentRelationshipResponse> grouped = new LinkedHashMap<>();
        Map<Long, List<StudentRelationshipLocationResponse>> locations = new LinkedHashMap<>();
        for (StudentPortalRelationshipRow row : rows) {
            grouped.putIfAbsent(row.getTeacherStudentAccessId(), new StudentRelationshipResponse(
                    row.getTeacherStudentAccessId(),
                    row.getTeacherStudentId(),
                    row.getTeacherName(),
                    List.of()));
            if (row.getLocationId() == null) {
                locations.putIfAbsent(row.getTeacherStudentAccessId(), new ArrayList<>());
                continue;
            }
            locations.computeIfAbsent(row.getTeacherStudentAccessId(), ignored -> new ArrayList<>())
                    .add(new StudentRelationshipLocationResponse(row.getLocationId(), row.getLocationName()));
        }
        List<StudentRelationshipResponse> result = new ArrayList<>();
        for (Map.Entry<Long, StudentRelationshipResponse> entry : grouped.entrySet()) {
            StudentRelationshipResponse current = entry.getValue();
            result.add(new StudentRelationshipResponse(
                    current.teacherStudentAccessId(),
                    current.teacherStudentId(),
                    current.teacherName(),
                    List.copyOf(locations.getOrDefault(entry.getKey(), List.of()))));
        }
        return result;
    }

    private StudentPortalIdentity requireIdentity(StudentPrincipal principal) {
        if (principal.teacherStudentAccessId() == null) {
            throw new BusinessException(ErrorCode.STUDENT_SCOPE_REQUIRED);
        }
        StudentPortalIdentity identity = studentPortalMapper.selectActiveIdentity(principal.teacherStudentAccessId());
        if (identity == null
                || !identity.getTeacherStudentId().equals(principal.teacherStudentId())
                || !identity.getStudentId().equals(principal.studentId())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        return identity;
    }

    private List<StudentCurriculumView> assemble(
            List<StudentPortalEnrollment> enrollments,
            List<StudentPortalCategoryRow> categories,
            List<StudentPortalMonitoringRow> published,
            List<StudentPortalHomeworkRow> homework,
            Map<Long, StudentCurriculumProgressView> progress,
            Map<Long, ResourcePair> resources) {
        Map<Long, List<StudentPortalCategoryRow>> categoriesByCurriculum = new LinkedHashMap<>();
        for (StudentPortalCategoryRow row : categories) {
            categoriesByCurriculum.computeIfAbsent(row.getCurriculumId(), ignored -> new ArrayList<>()).add(row);
        }
        Map<String, List<StudentPortalMonitoringRow>> publishedByCategory = new LinkedHashMap<>();
        for (StudentPortalMonitoringRow row : published) {
            publishedByCategory.computeIfAbsent(key(row.getStudentCurriculumId(), row.getCategoryId()), ignored -> new ArrayList<>())
                    .add(row);
        }
        Map<String, List<StudentHomeworkView>> homeworkByKey = new LinkedHashMap<>();
        for (StudentPortalHomeworkRow row : homework) {
            homeworkByKey.computeIfAbsent(key(row.getStudentCurriculumId(), row.getContentDetailId()), ignored -> new ArrayList<>())
                    .add(new StudentHomeworkView(
                            row.getHomeworkId(),
                            row.getHomeworkContent(),
                            row.getDeadline(),
                            row.getCompleted(),
                            row.getFeedback()));
        }
        List<StudentCurriculumView> views = new ArrayList<>();
        for (StudentPortalEnrollment enrollment : enrollments) {
            views.add(new StudentCurriculumView(
                    enrollment.getCurriculumName(),
                    progressQueryService.summarize(progress.get(enrollment.getStudentCurriculumId())),
                    categoryViews(enrollment.getStudentCurriculumId(),
                            categoriesByCurriculum.getOrDefault(enrollment.getCurriculumId(), List.of()),
                            publishedByCategory,
                            homeworkByKey,
                            resources)));
        }
        return views;
    }

    private List<StudentCategoryView> categoryViews(
            Long studentCurriculumId,
            List<StudentPortalCategoryRow> rows,
            Map<String, List<StudentPortalMonitoringRow>> publishedByCategory,
            Map<String, List<StudentHomeworkView>> homeworkByKey,
            Map<Long, ResourcePair> resources) {
        List<StudentCategoryView> ordered = new ArrayList<>();
        for (StudentPortalCategoryRow row : rows) {
            List<StudentContentView> contents = new ArrayList<>();
            for (StudentPortalMonitoringRow learned : publishedByCategory.getOrDefault(
                    key(studentCurriculumId, row.getCategoryId()), List.of())) {
                contents.add(new StudentContentView(
                        learned.getMonitoringId(),
                        learned.getContentName(),
                        learned.getTargetBpm(),
                        learned.getCurrentBpm(),
                        learned.getProgressStatus(),
                        learned.getYoutubeUrl(),
                        ResourcePair.of(resources, learned.getContentDetailId()).sheet(),
                        ResourcePair.of(resources, learned.getContentDetailId()).audio(),
                        homeworkByKey.getOrDefault(key(studentCurriculumId, learned.getContentDetailId()), List.of())));
            }
            int totalContentCount = row.getTotalContentCount() == null ? 0 : row.getTotalContentCount();
            ordered.add(new StudentCategoryView(row.getCategoryName(), totalContentCount, List.copyOf(contents)));
        }
        return ordered;
    }

    private List<Long> publishedContentDetailIds(List<StudentPortalMonitoringRow> published) {
        List<Long> ids = new ArrayList<>();
        for (StudentPortalMonitoringRow row : published) {
            if (row.getContentDetailId() != null && !ids.contains(row.getContentDetailId())) {
                ids.add(row.getContentDetailId());
            }
        }
        return ids;
    }

    private String key(Long studentCurriculumId, Long contentDetailId) {
        return studentCurriculumId + ":" + contentDetailId;
    }
}
