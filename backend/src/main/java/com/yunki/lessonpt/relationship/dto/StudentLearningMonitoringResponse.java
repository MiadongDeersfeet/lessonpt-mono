package com.yunki.lessonpt.relationship.dto;

import java.util.List;

import com.yunki.lessonpt.common.model.ProgressStatus;

public record StudentLearningMonitoringResponse(
        Long monitoringId,
        Long contentDetailId,
        String contentDetailName,
        Integer displayOrder,
        Integer targetBpm,
        Integer currentBpm,
        ProgressStatus progressStatus,
        String memo,
        List<StudentLearningHomeworkResponse> homeworks) {
}
