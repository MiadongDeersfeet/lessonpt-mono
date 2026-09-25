package com.yunki.lessonpt.relationship.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.relationship.dto.ProgressSummary;
import com.yunki.lessonpt.relationship.dto.StudentLearningCurriculumResponse;
import com.yunki.lessonpt.relationship.dto.StudentLearningDetailResponse;
import com.yunki.lessonpt.relationship.dto.StudentLearningHomeworkResponse;
import com.yunki.lessonpt.relationship.dto.StudentLearningLocationResponse;
import com.yunki.lessonpt.relationship.dto.StudentLearningMonitoringResponse;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLearningQueryMapper;
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgressView;
import com.yunki.lessonpt.relationship.query.StudentLearningCurriculumRow;
import com.yunki.lessonpt.relationship.query.StudentLearningHomeworkRow;
import com.yunki.lessonpt.relationship.query.StudentLearningLocationRow;
import com.yunki.lessonpt.relationship.query.StudentLearningMonitoringRow;
import com.yunki.lessonpt.relationship.query.StudentLearningStudentRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentLearningQueryService {

    private final StudentLearningQueryMapper studentLearningQueryMapper;
    private final ProgressQueryMapper progressQueryMapper;
    private final ProgressQueryService progressQueryService;

    @Transactional(readOnly = true)
    public StudentLearningDetailResponse learning(Long teacherId, Long studentId) {
        StudentLearningStudentRow student = studentLearningQueryMapper.selectActiveStudent(teacherId, studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        List<StudentLearningLocationRow> locations = studentLearningQueryMapper.selectActiveLocations(
                teacherId, student.getTeacherStudentId());
        if (locations.isEmpty()) {
            return response(student, List.of());
        }
        List<Long> locationIds = locations.stream().map(StudentLearningLocationRow::getTeacherStudentLocationId).toList();
        List<StudentLearningCurriculumRow> curriculums = studentLearningQueryMapper.selectActiveCurriculums(
                teacherId, locationIds);
        List<Long> enrollmentIds = curriculums.stream().map(StudentLearningCurriculumRow::getStudentCurriculumId).toList();
        List<StudentLearningMonitoringRow> monitorings = enrollmentIds.isEmpty()
                ? List.of()
                : studentLearningQueryMapper.selectActiveMonitorings(enrollmentIds);
        List<Long> monitoringIds = monitorings.stream().map(StudentLearningMonitoringRow::getMonitoringId).toList();
        List<StudentLearningHomeworkRow> homeworks = monitoringIds.isEmpty()
                ? List.of()
                : studentLearningQueryMapper.selectActiveHomeworks(monitoringIds);
        Map<Long, ProgressSummary> progress = new LinkedHashMap<>();
        if (!enrollmentIds.isEmpty()) {
            for (StudentCurriculumProgressView view : progressQueryMapper.selectProgressByStudentCurriculumIds(enrollmentIds)) {
                progress.put(view.getStudentCurriculumId(), progressQueryService.summarize(view));
            }
        }
        return response(student, assemble(locations, curriculums, monitorings, homeworks, progress));
    }

    private StudentLearningDetailResponse response(
            StudentLearningStudentRow student,
            List<StudentLearningLocationResponse> locations) {
        return new StudentLearningDetailResponse(
                student.getStudentId(), student.getName(), student.getEmail(), student.getPhone(), locations);
    }

    private List<StudentLearningLocationResponse> assemble(
            List<StudentLearningLocationRow> locations,
            List<StudentLearningCurriculumRow> curriculums,
            List<StudentLearningMonitoringRow> monitorings,
            List<StudentLearningHomeworkRow> homeworks,
            Map<Long, ProgressSummary> progress) {
        Map<Long, List<StudentLearningHomeworkResponse>> homeworkByMonitoring = new LinkedHashMap<>();
        for (StudentLearningHomeworkRow row : homeworks) {
            homeworkByMonitoring.computeIfAbsent(row.getMonitoringId(), ignored -> new ArrayList<>())
                    .add(new StudentLearningHomeworkResponse(
                            row.getHomeworkId(),
                            row.getHomeworkContent(),
                            row.getDeadline(),
                            row.getCompleted(),
                            row.getFeedback()));
        }
        Map<Long, List<StudentLearningMonitoringResponse>> monitoringByEnrollment = new LinkedHashMap<>();
        for (StudentLearningMonitoringRow row : monitorings) {
            monitoringByEnrollment.computeIfAbsent(row.getStudentCurriculumId(), ignored -> new ArrayList<>())
                    .add(new StudentLearningMonitoringResponse(
                            row.getMonitoringId(),
                            row.getContentDetailId(),
                            row.getContentDetailName(),
                            row.getDisplayOrder(),
                            row.getTargetBpm(),
                            row.getCurrentBpm(),
                            row.getProgressStatus(),
                            row.getMemo(),
                            homeworkByMonitoring.getOrDefault(row.getMonitoringId(), List.of())));
        }
        Map<Long, List<StudentLearningCurriculumResponse>> curriculumByLocation = new LinkedHashMap<>();
        for (StudentLearningCurriculumRow row : curriculums) {
            curriculumByLocation.computeIfAbsent(row.getTeacherStudentLocationId(), ignored -> new ArrayList<>())
                    .add(new StudentLearningCurriculumResponse(
                            row.getStudentCurriculumId(),
                            row.getCurriculumId(),
                            row.getCurriculumName(),
                            row.getReenrolled(),
                            row.getMemo(),
                            progress.get(row.getStudentCurriculumId()),
                            monitoringByEnrollment.getOrDefault(row.getStudentCurriculumId(), List.of())));
        }
        List<StudentLearningLocationResponse> assembled = new ArrayList<>();
        for (StudentLearningLocationRow location : locations) {
            assembled.add(new StudentLearningLocationResponse(
                    location.getTeacherStudentLocationId(),
                    location.getLocationId(),
                    location.getLocationName(),
                    location.getAddress(),
                    curriculumByLocation.getOrDefault(location.getTeacherStudentLocationId(), List.of())));
        }
        return assembled;
    }
}
