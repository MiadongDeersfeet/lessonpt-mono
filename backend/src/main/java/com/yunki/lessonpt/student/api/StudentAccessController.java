package com.yunki.lessonpt.student.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.yunki.lessonpt.auth.security.TeacherPrincipal;
import com.yunki.lessonpt.relationship.dto.TeacherStudentAccessResponse;
import com.yunki.lessonpt.relationship.service.TeacherStudentAccessService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/students/{studentId}/access")
@RequiredArgsConstructor
public class StudentAccessController {

    private final TeacherStudentAccessService teacherStudentAccessService;

    @PostMapping
    public ResponseEntity<TeacherStudentAccessResponse> create(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId) {
        TeacherStudentAccessResponse body = teacherStudentAccessService.createAccess(principal.teacherId(), studentId);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(uri).body(body);
    }

    @GetMapping
    public TeacherStudentAccessResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId) {
        return teacherStudentAccessService.getAccess(principal.teacherId(), studentId);
    }

    @DeleteMapping
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId) {
        teacherStudentAccessService.revokeAccess(principal.teacherId(), studentId);
        return ResponseEntity.noContent().build();
    }
}
