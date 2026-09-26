package com.yunki.lessonpt.resource.dto;

import java.time.LocalDateTime;

import com.yunki.lessonpt.resource.domain.ContentResource;
import com.yunki.lessonpt.resource.domain.ResourceType;

public record ContentResourceResponse(
        Long resourceId,
        ResourceType resourceType,
        String originalFileName,
        String contentType,
        Long fileSize,
        LocalDateTime createdAt) {

    public static ContentResourceResponse from(ContentResource resource) {
        if (resource == null) {
            return null;
        }
        return new ContentResourceResponse(
                resource.getResourceId(),
                resource.getResourceType(),
                resource.getOriginalFileName(),
                resource.getContentType(),
                resource.getFileSize(),
                resource.getCreatedAt());
    }
}
