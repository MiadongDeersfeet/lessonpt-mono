package com.yunki.lessonpt.curriculum.dto;

public record CurriculumResponse(
        Long curriculumId,
        String name,
        Integer displayOrder
) {
}
