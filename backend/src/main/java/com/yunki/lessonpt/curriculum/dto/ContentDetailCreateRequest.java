package com.yunki.lessonpt.curriculum.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentDetailCreateRequest(
        @NotBlank @Size(max = 200) String name,
        String memo,
        @Min(60) @Max(240) Integer targetBpm,
        String evaluationMemo,
        @Size(max = 2000) String sheetUrl,
        @Size(max = 2000) String youtubeUrl,
        @Size(max = 2000) String audioUrl
) {
}
