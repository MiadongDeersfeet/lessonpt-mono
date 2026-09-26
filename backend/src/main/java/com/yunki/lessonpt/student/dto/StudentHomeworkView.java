package com.yunki.lessonpt.student.dto;

import java.time.LocalDateTime;

public record StudentHomeworkView(
        Long homeworkId, String content, LocalDateTime deadline, Boolean completed, String feedback) {
}
