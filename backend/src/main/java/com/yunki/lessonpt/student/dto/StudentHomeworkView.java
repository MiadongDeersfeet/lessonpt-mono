package com.yunki.lessonpt.student.dto;

import java.time.LocalDateTime;

public record StudentHomeworkView(String content, LocalDateTime deadline, Boolean completed, String feedback) {
}
