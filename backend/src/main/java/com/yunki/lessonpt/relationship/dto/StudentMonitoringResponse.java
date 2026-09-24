package com.yunki.lessonpt.relationship.dto;

import com.yunki.lessonpt.common.model.ProgressStatus;

public record StudentMonitoringResponse(
        Long monitoringId,
        Long contentDetailId,
        Integer displayOrder,
        Integer currentBpm,
        ProgressStatus progressStatus,
        String memo
) {
}
