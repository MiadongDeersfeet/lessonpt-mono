package com.yunki.lessonpt.relationship.dto;

import java.util.List;

public record StudentLearningCurriculumResponse(
        Long studentCurriculumId,
        Long curriculumId,
        String curriculumName,
        Boolean reenrolled,
        String memo,
        ProgressSummary progress,
        List<StudentLearningMonitoringResponse> monitorings) {
}
