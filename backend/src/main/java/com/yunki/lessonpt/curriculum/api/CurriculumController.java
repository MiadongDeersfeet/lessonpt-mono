package com.yunki.lessonpt.curriculum.api;

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
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.dto.CurriculumCreateRequest;
import com.yunki.lessonpt.curriculum.dto.CurriculumResponse;
import com.yunki.lessonpt.curriculum.dto.CurriculumUpdateRequest;
import com.yunki.lessonpt.curriculum.service.CurriculumService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/curriculums")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @PostMapping
    public ResponseEntity<CurriculumResponse> create(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @Valid @RequestBody CurriculumCreateRequest request) {
        Curriculum created = curriculumService.createCurriculum(principal.teacherId(), request.name());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{curriculumId}")
                .buildAndExpand(created.getCurriculumId())
                .toUri();
        return ResponseEntity.created(uri).body(toResponse(created));
    }

    @GetMapping
    public List<CurriculumResponse> list(@AuthenticationPrincipal TeacherPrincipal principal) {
        return curriculumService.getCurriculums(principal.teacherId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{curriculumId}")
    public CurriculumResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId) {
        return toResponse(curriculumService.getCurriculum(principal.teacherId(), curriculumId));
    }

    @PatchMapping("/{curriculumId}")
    public CurriculumResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @Valid @RequestBody CurriculumUpdateRequest request) {
        return toResponse(curriculumService.updateCurriculum(principal.teacherId(), curriculumId, request));
    }

    @DeleteMapping("/{curriculumId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId) {
        curriculumService.deleteCurriculum(principal.teacherId(), curriculumId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{curriculumId}/restore")
    public CurriculumResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId) {
        return toResponse(curriculumService.restoreCurriculum(principal.teacherId(), curriculumId));
    }

    private CurriculumResponse toResponse(Curriculum curriculum) {
        return new CurriculumResponse(
                curriculum.getCurriculumId(),
                curriculum.getName(),
                curriculum.getDisplayOrder());
    }
}
