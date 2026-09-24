package com.yunki.lessonpt.relationship.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StudentCurriculumProgressBatchRequest(
        @NotNull List<@NotNull @Positive Long> studentCurriculumIds
) {
}
