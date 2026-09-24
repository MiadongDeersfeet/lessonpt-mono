package com.yunki.lessonpt.curriculum.dto;

public record CategoryResponse(
        Long categoryId,
        String name,
        Integer displayOrder
) {
}
