package com.yunki.lessonpt.curriculum.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
import com.yunki.lessonpt.resource.domain.ContentResource;
import com.yunki.lessonpt.resource.dto.ResourcePair;
import com.yunki.lessonpt.resource.service.ContentResourceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/curriculums/{curriculumId}/categories/{categoryId}/content-details")
@RequiredArgsConstructor
public class ContentDetailController {

    private final ContentDetailService contentDetailService;
    private final ContentResourceService contentResourceService;

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
        return ResponseEntity.created(uri).body(toResponse(created, resourcesOf(List.of(created))));
    }

    @GetMapping
    public List<ContentDetailResponse> list(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId) {
        List<ContentDetail> details = contentDetailService.getContentDetails(
                principal.teacherId(), curriculumId, categoryId);
        Map<Long, ResourcePair> resources = resourcesOf(details);
        return details.stream()
                .map(detail -> toResponse(detail, resources))
                .toList();
    }

    @GetMapping("/{contentDetailId}")
    public ContentDetailResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId) {
        ContentDetail detail = contentDetailService.getContentDetail(
                principal.teacherId(), curriculumId, categoryId, contentDetailId);
        return toResponse(detail, resourcesOf(List.of(detail)));
    }

    @PatchMapping("/{contentDetailId}")
    public ContentDetailResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId,
            @Valid @RequestBody ContentDetailUpdateRequest request) {
        ContentDetail updated = contentDetailService.updateContentDetail(
                principal.teacherId(), curriculumId, categoryId, contentDetailId, toChange(request));
        return toResponse(updated, resourcesOf(List.of(updated)));
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
        ContentDetail restored = contentDetailService.restoreContentDetail(
                principal.teacherId(), curriculumId, categoryId, contentDetailId);
        return toResponse(restored, resourcesOf(List.of(restored)));
    }

    private ContentDetailChange toChange(ContentDetailCreateRequest request) {
        ContentDetailChange change = new ContentDetailChange();
        change.setName(request.name());
        change.setMemo(request.memo());
        change.setTargetBpm(request.targetBpm());
        change.setEvaluationMemo(request.evaluationMemo());
        change.setYoutubeUrl(request.youtubeUrl());
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
        if (request.isYoutubeUrlSpecified()) {
            change.setYoutubeUrl(request.getYoutubeUrl());
        }
        return change;
    }

    private Map<Long, ResourcePair> resourcesOf(List<ContentDetail> details) {
        List<Long> ids = details.stream().map(ContentDetail::getContentDetailId).toList();
        return ResourcePair.byContentDetail(contentResourceService.activeForContentDetails(ids));
    }

    private ContentDetailResponse toResponse(ContentDetail contentDetail, Map<Long, ResourcePair> resources) {
        ResourcePair pair = ResourcePair.of(resources, contentDetail.getContentDetailId());
        return new ContentDetailResponse(
                contentDetail.getContentDetailId(),
                contentDetail.getName(),
                contentDetail.getDisplayOrder(),
                contentDetail.getMemo(),
                contentDetail.getTargetBpm(),
                contentDetail.getEvaluationMemo(),
                contentDetail.getYoutubeUrl(),
                pair.sheet(),
                pair.audio());
    }
}
