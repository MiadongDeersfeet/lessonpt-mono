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
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.student.query.StudentPortalContentRow;
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
        List<StudentPortalContentRow> contents = studentPortalMapper.selectActiveContents(curriculumIds);
        List<StudentPortalMonitoringRow> monitoring = studentPortalMapper.selectActiveMonitoring(enrollmentIds);
        List<StudentPortalHomeworkRow> homework = studentPortalMapper.selectActiveHomework(enrollmentIds);
        Map<Long, StudentCurriculumProgressView> progress = new LinkedHashMap<>();
        for (StudentCurriculumProgressView view : progressQueryMapper.selectProgressByStudentCurriculumIds(enrollmentIds)) {
            progress.put(view.getStudentCurriculumId(), view);
        }
        return new StudentLearningResponse(assemble(enrollments, contents, monitoring, homework, progress));
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
            List<StudentPortalContentRow> contents,
            List<StudentPortalMonitoringRow> monitoring,
            List<StudentPortalHomeworkRow> homework,
            Map<Long, StudentCurriculumProgressView> progress) {
        Map<Long, List<StudentPortalContentRow>> contentsByCurriculum = new LinkedHashMap<>();
        for (StudentPortalContentRow row : contents) {
            contentsByCurriculum.computeIfAbsent(row.getCurriculumId(), ignored -> new ArrayList<>()).add(row);
        }
        Map<String, StudentPortalMonitoringRow> monitoringByKey = new LinkedHashMap<>();
        for (StudentPortalMonitoringRow row : monitoring) {
            monitoringByKey.putIfAbsent(key(row.getStudentCurriculumId(), row.getContentDetailId()), row);
        }
        Map<String, List<StudentHomeworkView>> homeworkByKey = new LinkedHashMap<>();
        for (StudentPortalHomeworkRow row : homework) {
            homeworkByKey.computeIfAbsent(key(row.getStudentCurriculumId(), row.getContentDetailId()), ignored -> new ArrayList<>())
                    .add(new StudentHomeworkView(row.getHomeworkContent(), row.getDeadline(), row.getCompleted(), row.getFeedback()));
        }
        List<StudentCurriculumView> views = new ArrayList<>();
        for (StudentPortalEnrollment enrollment : enrollments) {
            views.add(new StudentCurriculumView(
                    enrollment.getCurriculumName(),
                    progressQueryService.summarize(progress.get(enrollment.getStudentCurriculumId())),
                    categories(enrollment.getStudentCurriculumId(),
                            contentsByCurriculum.getOrDefault(enrollment.getCurriculumId(), List.of()),
                            monitoringByKey,
                            homeworkByKey)));
        }
        return views;
    }

    private List<StudentCategoryView> categories(
            Long studentCurriculumId,
            List<StudentPortalContentRow> rows,
            Map<String, StudentPortalMonitoringRow> monitoringByKey,
            Map<String, List<StudentHomeworkView>> homeworkByKey) {
        Map<Long, StudentCategoryView> categories = new LinkedHashMap<>();
        Map<Long, List<StudentContentView>> contents = new LinkedHashMap<>();
        for (StudentPortalContentRow row : rows) {
            categories.putIfAbsent(row.getCategoryId(), new StudentCategoryView(row.getCategoryName(), List.of()));
            if (row.getContentDetailId() == null) {
                contents.putIfAbsent(row.getCategoryId(), new ArrayList<>());
                continue;
            }
            StudentPortalMonitoringRow learned = monitoringByKey.get(key(studentCurriculumId, row.getContentDetailId()));
            contents.computeIfAbsent(row.getCategoryId(), ignored -> new ArrayList<>())
                    .add(new StudentContentView(
                            row.getContentName(),
                            row.getTargetBpm(),
                            learned == null ? null : learned.getCurrentBpm(),
                            learned == null ? null : learned.getProgressStatus(),
                            row.getSheetUrl(),
                            row.getYoutubeUrl(),
                            row.getAudioUrl(),
                            homeworkByKey.getOrDefault(key(studentCurriculumId, row.getContentDetailId()), List.of())));
        }
        List<StudentCategoryView> ordered = new ArrayList<>();
        for (Map.Entry<Long, StudentCategoryView> entry : categories.entrySet()) {
            ordered.add(new StudentCategoryView(
                    entry.getValue().name(),
                    List.copyOf(contents.getOrDefault(entry.getKey(), List.of()))));
        }
        return ordered;
    }

    private String key(Long studentCurriculumId, Long contentDetailId) {
        return studentCurriculumId + ":" + contentDetailId;
    }
}
