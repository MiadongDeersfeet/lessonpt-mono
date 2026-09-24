package com.yunki.lessonpt.relationship.dto;

public record StudentCurriculumResponse(
        Long studentCurriculumId,
        Long curriculumId,
        Boolean reenrolled,
        String memo
) {
}
