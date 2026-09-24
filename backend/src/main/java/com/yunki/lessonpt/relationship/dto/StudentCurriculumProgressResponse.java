package com.yunki.lessonpt.relationship.dto;

import java.math.BigDecimal;

public record StudentCurriculumProgressResponse(
        Long studentCurriculumId,
        Integer completedCount,
        Integer totalCount,
        BigDecimal percentage
) {
}
