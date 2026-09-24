package com.yunki.lessonpt.relationship.dto;

import com.yunki.lessonpt.common.model.ProgressStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StudentMonitoringCreateRequest(
        @NotNull Long contentDetailId,
        @Min(60) @Max(240) Integer currentBpm,
        ProgressStatus progressStatus,
        String memo
) {
}
