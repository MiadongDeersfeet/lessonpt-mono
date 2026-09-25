package com.yunki.lessonpt.student.dto;

import java.util.List;

import com.yunki.lessonpt.common.model.ProgressStatus;

public record StudentContentView(
        String name,
        Integer targetBpm,
        Integer currentBpm,
        ProgressStatus progressStatus,
        String sheetUrl,
        String youtubeUrl,
        String audioUrl,
        List<StudentHomeworkView> homeworks) {
}
