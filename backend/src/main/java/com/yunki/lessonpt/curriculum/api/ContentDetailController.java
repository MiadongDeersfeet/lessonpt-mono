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
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.dto.ContentDetailCreateRequest;
import com.yunki.lessonpt.curriculum.dto.ContentDetailResponse;
import com.yunki.lessonpt.curriculum.dto.ContentDetailUpdateRequest;
import com.yunki.lessonpt.curriculum.service.ContentDetailChange;
import com.yunki.lessonpt.curriculum.service.ContentDetailService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/curriculums/{curriculumId}/categories/{categoryId}/content-details")
@RequiredArgsConstructor
public class ContentDetailController {

    private final ContentDetailService contentDetailService;

    @PostMapping
    public ResponseEntity<ContentDetailResponse> create(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @Valid @RequestBody ContentDetailCreateRequest request) {
        ContentDetail created = contentDetailService.createContentDetail(
                principal.teacherId(), curriculumId, categoryId, toChange(request));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{contentDetailId}")
                .buildAndExpand(created.getContentDetailId())
                .toUri();
        return ResponseEntity.created(uri).body(toResponse(created));
    }

    @GetMapping
    public List<ContentDetailResponse> list(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId) {
        return contentDetailService.getContentDetails(principal.teacherId(), curriculumId, categoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{contentDetailId}")
    public ContentDetailResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId) {
        return toResponse(contentDetailService.getContentDetail(
                principal.teacherId(), curriculumId, categoryId, contentDetailId));
    }

    @PatchMapping("/{contentDetailId}")
    public ContentDetailResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId,
            @Valid @RequestBody ContentDetailUpdateRequest request) {
        return toResponse(contentDetailService.updateContentDetail(
                principal.teacherId(), curriculumId, categoryId, contentDetailId, toChange(request)));
    }

    @DeleteMapping("/{contentDetailId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId) {
        contentDetailService.deleteContentDetail(principal.teacherId(), curriculumId, categoryId, contentDetailId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{contentDetailId}/restore")
    public ContentDetailResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId) {
        return toResponse(contentDetailService.restoreContentDetail(
                principal.teacherId(), curriculumId, categoryId, contentDetailId));
    }

    private ContentDetailChange toChange(ContentDetailCreateRequest request) {
        ContentDetailChange change = new ContentDetailChange();
        change.setName(request.name());
        change.setMemo(request.memo());
        change.setTargetBpm(request.targetBpm());
        change.setEvaluationMemo(request.evaluationMemo());
        change.setSheetUrl(request.sheetUrl());
        change.setYoutubeUrl(request.youtubeUrl());
        change.setAudioUrl(request.audioUrl());
        return change;
    }

    private ContentDetailChange toChange(ContentDetailUpdateRequest request) {
        ContentDetailChange change = new ContentDetailChange();
        if (request.isNameSpecified()) {
            change.setName(request.getName());
        }
        if (request.isMemoSpecified()) {
            change.setMemo(request.getMemo());
        }
        if (request.isTargetBpmSpecified()) {
            change.setTargetBpm(request.getTargetBpm());
        }
        if (request.isEvaluationMemoSpecified()) {
            change.setEvaluationMemo(request.getEvaluationMemo());
        }
        if (request.isSheetUrlSpecified()) {
            change.setSheetUrl(request.getSheetUrl());
        }
        if (request.isYoutubeUrlSpecified()) {
            change.setYoutubeUrl(request.getYoutubeUrl());
        }
        if (request.isAudioUrlSpecified()) {
            change.setAudioUrl(request.getAudioUrl());
        }
        return change;
    }

    private ContentDetailResponse toResponse(ContentDetail contentDetail) {
        return new ContentDetailResponse(
                contentDetail.getContentDetailId(),
                contentDetail.getName(),
                contentDetail.getDisplayOrder(),
                contentDetail.getMemo(),
                contentDetail.getTargetBpm(),
                contentDetail.getEvaluationMemo(),
                contentDetail.getSheetUrl(),
                contentDetail.getYoutubeUrl(),
                contentDetail.getAudioUrl());
    }
}
