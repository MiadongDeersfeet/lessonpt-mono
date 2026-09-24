package com.yunki.lessonpt.relationship.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public record HomeworkCreateRequest(
        @NotBlank String homeworkContent,
        LocalDateTime deadline,
        String feedback
) {
}
