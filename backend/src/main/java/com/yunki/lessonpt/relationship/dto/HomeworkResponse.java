package com.yunki.lessonpt.relationship.dto;

import java.time.LocalDateTime;

public record HomeworkResponse(
        Long homeworkId,
        String homeworkContent,
        LocalDateTime deadline,
        Boolean completed,
        String feedback
) {
}
