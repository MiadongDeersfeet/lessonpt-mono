package com.yunki.lessonpt.curriculum.dto;

public record ContentDetailResponse(
        Long contentDetailId,
        String name,
        Integer displayOrder,
        String memo,
        Integer targetBpm,
        String evaluationMemo,
        String youtubeUrl,
        com.yunki.lessonpt.resource.dto.ContentResourceResponse sheet,
        com.yunki.lessonpt.resource.dto.ContentResourceResponse audio
) {
}
