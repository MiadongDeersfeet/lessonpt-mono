package com.yunki.lessonpt.resource.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.yunki.lessonpt.auth.security.TeacherPrincipal;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.resource.domain.ResourceType;
import com.yunki.lessonpt.resource.dto.ContentResourceResponse;
import com.yunki.lessonpt.resource.service.ContentResourceService;
import com.yunki.lessonpt.resource.service.ContentResourceService.UploadResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/curriculums/{curriculumId}/categories/{categoryId}/content-details/{contentDetailId}/resources")
@RequiredArgsConstructor
public class ContentResourceController {

    private final ContentResourceService contentResourceService;

    @PostMapping("/{resourceType}")
    public ResponseEntity<ContentResourceResponse> save(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId,
            @PathVariable String resourceType,
            @RequestPart("file") MultipartFile file) {
        UploadResult result = contentResourceService.save(
                principal.teacherId(),
                curriculumId,
                categoryId,
                contentDetailId,
                requireType(resourceType),
                file);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(201);
        if (result.storageWarning()) {
            response.header("X-Lessonpt-Storage-Warning", "true");
        }
        return response.body(result.resource());
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId,
            @PathVariable Long resourceId) {
        contentResourceService.delete(
                principal.teacherId(), curriculumId, categoryId, contentDetailId, resourceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{resourceId}/content")
    public ResponseEntity<StreamingResponseBody> content(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @PathVariable Long contentDetailId,
            @PathVariable Long resourceId,
            @RequestParam(name = "disposition", required = false) String disposition,
            @RequestHeader(name = HttpHeaders.RANGE, required = false) String range) {
        return ResourceContentResponses.write(contentResourceService.openForTeacher(
                principal.teacherId(),
                curriculumId,
                categoryId,
                contentDetailId,
                resourceId,
                range,
                disposition));
    }

    private ResourceType requireType(String resourceType) {
        ResourceType type = ResourceType.fromPath(resourceType);
        if (type == null) {
            throw new BusinessException(ErrorCode.RESOURCE_INVALID_TYPE);
        }
        return type;
    }
}
