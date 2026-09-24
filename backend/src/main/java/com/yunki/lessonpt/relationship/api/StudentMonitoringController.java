package com.yunki.lessonpt.relationship.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.yunki.lessonpt.auth.security.TeacherPrincipal;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.dto.StudentMonitoringCreateRequest;
import com.yunki.lessonpt.relationship.dto.StudentMonitoringResponse;
import com.yunki.lessonpt.relationship.dto.StudentMonitoringUpdateRequest;
import com.yunki.lessonpt.relationship.service.StudentMonitoringChange;
import com.yunki.lessonpt.relationship.service.StudentMonitoringService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student-curriculums/{studentCurriculumId}/monitorings")
@RequiredArgsConstructor
public class StudentMonitoringController {

    private final StudentMonitoringService studentMonitoringService;

    @PostMapping
    public ResponseEntity<StudentMonitoringResponse> assign(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentCurriculumId,
            @Valid @RequestBody StudentMonitoringCreateRequest request) {
        StudentMonitoring assigned = studentMonitoringService.assignStudentMonitoring(
                principal.teacherId(),
                studentCurriculumId,
                request.contentDetailId(),
                request.currentBpm(),
                request.progressStatus(),
                request.memo());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{monitoringId}")
                .buildAndExpand(assigned.getMonitoringId())
                .toUri();
        return ResponseEntity.created(uri).body(toResponse(assigned));
    }

    @GetMapping
    public List<StudentMonitoringResponse> list(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentCurriculumId) {
        return studentMonitoringService.getStudentMonitorings(principal.teacherId(), studentCurriculumId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{monitoringId}")
    public StudentMonitoringResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentCurriculumId,
            @PathVariable Long monitoringId) {
        return toResponse(studentMonitoringService.getStudentMonitoring(
                principal.teacherId(), studentCurriculumId, monitoringId));
    }

    @PatchMapping("/{monitoringId}")
    public StudentMonitoringResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentCurriculumId,
            @PathVariable Long monitoringId,
            @Valid @RequestBody StudentMonitoringUpdateRequest request) {
        return toResponse(studentMonitoringService.updateStudentMonitoring(
                principal.teacherId(), studentCurriculumId, monitoringId, toChange(request)));
    }

    @DeleteMapping("/{monitoringId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentCurriculumId,
            @PathVariable Long monitoringId) {
        studentMonitoringService.deleteStudentMonitoring(principal.teacherId(), studentCurriculumId, monitoringId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{monitoringId}/restore")
    public StudentMonitoringResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentCurriculumId,
            @PathVariable Long monitoringId) {
        return toResponse(studentMonitoringService.restoreStudentMonitoring(
                principal.teacherId(), studentCurriculumId, monitoringId));
    }

    private StudentMonitoringChange toChange(StudentMonitoringUpdateRequest request) {
        StudentMonitoringChange change = new StudentMonitoringChange();
        if (request.isCurrentBpmSpecified()) {
            change.setCurrentBpm(request.getCurrentBpm());
        }
        if (request.isProgressStatusSpecified()) {
            change.setProgressStatus(request.getProgressStatus());
        }
        if (request.isMemoSpecified()) {
            change.setMemo(request.getMemo());
        }
        return change;
    }

    private StudentMonitoringResponse toResponse(StudentMonitoring studentMonitoring) {
        return new StudentMonitoringResponse(
                studentMonitoring.getMonitoringId(),
                studentMonitoring.getContentDetailId(),
                studentMonitoring.getDisplayOrder(),
                studentMonitoring.getCurrentBpm(),
                studentMonitoring.getProgressStatus(),
                studentMonitoring.getMemo());
    }
}
