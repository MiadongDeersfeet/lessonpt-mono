package com.yunki.lessonpt.relationship.dto;

import jakarta.validation.constraints.NotNull;

public record StudentCurriculumCreateRequest(
        @NotNull Long curriculumId,
        String memo
) {
}
