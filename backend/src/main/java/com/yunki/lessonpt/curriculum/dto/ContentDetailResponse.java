package com.yunki.lessonpt.curriculum.dto;

public record ContentDetailResponse(
        Long contentDetailId,
        String name,
        Integer displayOrder,
        String memo,
        Integer targetBpm,
        String evaluationMemo,
        String sheetUrl,
        String youtubeUrl,
        String audioUrl
) {
}
