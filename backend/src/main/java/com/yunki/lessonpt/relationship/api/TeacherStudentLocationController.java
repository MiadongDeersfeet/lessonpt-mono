package com.yunki.lessonpt.relationship.api;

import java.net.URI;
import java.util.List;

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
import com.yunki.lessonpt.relationship.dto.TeacherStudentLocationResponse;
import com.yunki.lessonpt.relationship.dto.TeacherStudentLocationView;
import com.yunki.lessonpt.relationship.service.TeacherStudentLocationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/students/{studentId}/locations")
@RequiredArgsConstructor
public class TeacherStudentLocationController {

    private final TeacherStudentLocationService teacherStudentLocationService;

    @PostMapping("/{locationId}")
    public ResponseEntity<TeacherStudentLocationResponse> assign(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long locationId) {
        TeacherStudentLocationView assigned = teacherStudentLocationService.assignLocation(
                principal.teacherId(), studentId, locationId);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(uri).body(toResponse(assigned));
    }

    @GetMapping
    public List<TeacherStudentLocationResponse> list(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId) {
        return teacherStudentLocationService.getStudentLocations(principal.teacherId(), studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> release(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long locationId) {
        teacherStudentLocationService.releaseLocation(principal.teacherId(), studentId, locationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{locationId}/restore")
    public TeacherStudentLocationResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long locationId) {
        return toResponse(teacherStudentLocationService.restoreLocation(principal.teacherId(), studentId, locationId));
    }

    private TeacherStudentLocationResponse toResponse(TeacherStudentLocationView view) {
        return new TeacherStudentLocationResponse(
                view.getTeacherStudentLocationId(),
                view.getLocationId(),
                view.getLocationName(),
                view.getAddress());
    }
}
