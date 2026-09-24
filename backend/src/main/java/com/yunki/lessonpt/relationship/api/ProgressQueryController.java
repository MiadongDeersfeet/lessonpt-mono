package com.yunki.lessonpt.relationship.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yunki.lessonpt.auth.security.TeacherPrincipal;
import com.yunki.lessonpt.relationship.dto.StudentCurriculumProgressBatchRequest;
import com.yunki.lessonpt.relationship.dto.StudentCurriculumProgressResponse;
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgress;
import com.yunki.lessonpt.relationship.service.ProgressQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student-curriculums")
@RequiredArgsConstructor
public class ProgressQueryController {

    private final ProgressQueryService progressQueryService;

    @GetMapping("/{studentCurriculumId}/progress")
    public ResponseEntity<StudentCurriculumProgressResponse> getProgress(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentCurriculumId) {
        return progressQueryService.getProgress(principal.teacherId(), studentCurriculumId)
                .map(progress -> ResponseEntity.ok(toResponse(progress)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/progress/query")
    public List<StudentCurriculumProgressResponse> queryProgress(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @Valid @RequestBody StudentCurriculumProgressBatchRequest request) {
        return progressQueryService.getProgresses(principal.teacherId(), request.studentCurriculumIds()).stream()
                .map(this::toResponse)
                .toList();
    }

    private StudentCurriculumProgressResponse toResponse(StudentCurriculumProgress progress) {
        return new StudentCurriculumProgressResponse(
                progress.studentCurriculumId(),
                progress.completedCount(),
                progress.totalCount(),
                progress.percentage());
    }
}
