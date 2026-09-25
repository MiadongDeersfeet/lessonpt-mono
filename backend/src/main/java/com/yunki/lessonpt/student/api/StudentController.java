package com.yunki.lessonpt.student.api;

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
import com.yunki.lessonpt.relationship.dto.StudentLearningDetailResponse;
import com.yunki.lessonpt.relationship.service.StudentLearningQueryService;
import com.yunki.lessonpt.student.dto.StudentCreateRequest;
import com.yunki.lessonpt.student.dto.StudentResponse;
import com.yunki.lessonpt.student.dto.StudentUpdateRequest;
import com.yunki.lessonpt.student.service.StudentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentLearningQueryService studentLearningQueryService;

    @PostMapping
    public ResponseEntity<StudentResponse> create(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @Valid @RequestBody StudentCreateRequest request) {
        StudentResponse body = studentService.createStudent(principal.teacherId(), request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{studentId}")
                .buildAndExpand(body.studentId())
                .toUri();
        return ResponseEntity.created(uri).body(body);
    }

    @GetMapping
    public List<StudentResponse> list(@AuthenticationPrincipal TeacherPrincipal principal) {
        return studentService.getStudents(principal.teacherId());
    }

    @GetMapping("/{studentId}/learning")
    public StudentLearningDetailResponse learning(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId) {
        return studentLearningQueryService.learning(principal.teacherId(), studentId);
    }

    @GetMapping("/{studentId}")
    public StudentResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId) {
        return studentService.getStudent(principal.teacherId(), studentId);
    }

    @PatchMapping("/{studentId}")
    public StudentResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId,
            @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.updateStudent(principal.teacherId(), studentId, request);
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> release(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId) {
        studentService.releaseStudent(principal.teacherId(), studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentId}/restore")
    public StudentResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId) {
        return studentService.restoreStudent(principal.teacherId(), studentId);
    }
}
