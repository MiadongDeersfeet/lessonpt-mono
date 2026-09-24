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
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.dto.StudentCurriculumCreateRequest;
import com.yunki.lessonpt.relationship.dto.StudentCurriculumResponse;
import com.yunki.lessonpt.relationship.dto.StudentCurriculumUpdateRequest;
import com.yunki.lessonpt.relationship.service.StudentCurriculumChange;
import com.yunki.lessonpt.relationship.service.StudentCurriculumService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student-locations/{teacherStudentLocationId}/curriculums")
@RequiredArgsConstructor
public class StudentCurriculumController {

    private final StudentCurriculumService studentCurriculumService;

    @PostMapping
    public ResponseEntity<StudentCurriculumResponse> assign(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long teacherStudentLocationId,
            @Valid @RequestBody StudentCurriculumCreateRequest request) {
        StudentCurriculum assigned = studentCurriculumService.assignStudentCurriculum(
                principal.teacherId(), teacherStudentLocationId, request.curriculumId(), request.memo());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{studentCurriculumId}")
                .buildAndExpand(assigned.getStudentCurriculumId())
                .toUri();
        return ResponseEntity.created(uri).body(toResponse(assigned));
    }

    @GetMapping
    public List<StudentCurriculumResponse> list(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long teacherStudentLocationId) {
        return studentCurriculumService.getStudentCurriculums(principal.teacherId(), teacherStudentLocationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{studentCurriculumId}")
    public StudentCurriculumResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long teacherStudentLocationId,
            @PathVariable Long studentCurriculumId) {
        return toResponse(studentCurriculumService.getStudentCurriculum(
                principal.teacherId(), teacherStudentLocationId, studentCurriculumId));
    }

    @PatchMapping("/{studentCurriculumId}")
    public StudentCurriculumResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long teacherStudentLocationId,
            @PathVariable Long studentCurriculumId,
            @Valid @RequestBody StudentCurriculumUpdateRequest request) {
        return toResponse(studentCurriculumService.updateStudentCurriculum(
                principal.teacherId(), teacherStudentLocationId, studentCurriculumId, toChange(request)));
    }

    @DeleteMapping("/{studentCurriculumId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long teacherStudentLocationId,
            @PathVariable Long studentCurriculumId) {
        studentCurriculumService.deleteStudentCurriculum(
                principal.teacherId(), teacherStudentLocationId, studentCurriculumId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentCurriculumId}/restore")
    public StudentCurriculumResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long teacherStudentLocationId,
            @PathVariable Long studentCurriculumId) {
        return toResponse(studentCurriculumService.restoreStudentCurriculum(
                principal.teacherId(), teacherStudentLocationId, studentCurriculumId));
    }

    private StudentCurriculumChange toChange(StudentCurriculumUpdateRequest request) {
        StudentCurriculumChange change = new StudentCurriculumChange();
        if (request.isMemoSpecified()) {
            change.setMemo(request.getMemo());
        }
        return change;
    }

    private StudentCurriculumResponse toResponse(StudentCurriculum studentCurriculum) {
        return new StudentCurriculumResponse(
                studentCurriculum.getStudentCurriculumId(),
                studentCurriculum.getCurriculumId(),
                studentCurriculum.getReenrolled(),
                studentCurriculum.getMemo());
    }
}
