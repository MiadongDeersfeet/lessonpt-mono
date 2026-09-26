package com.yunki.lessonpt.student.dto;

import java.util.List;

import com.yunki.lessonpt.common.model.ProgressStatus;

public record StudentContentView(
        String name,
        Integer targetBpm,
        Integer currentBpm,
        ProgressStatus progressStatus,
        String youtubeUrl,
        com.yunki.lessonpt.resource.dto.ContentResourceResponse sheet,
        com.yunki.lessonpt.resource.dto.ContentResourceResponse audio,
        List<StudentHomeworkView> homeworks) {
}
