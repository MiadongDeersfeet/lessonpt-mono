package com.yunki.lessonpt.relationship.query;

import java.math.BigDecimal;

/**
 * 수강 배정 하나의 진행률이다.
 * 활성 내용이 없으면 이 결과를 만들지 않는다.
 */
public record StudentCurriculumProgress(
        Long studentCurriculumId,
        Integer completedCount,
        Integer totalCount,
        BigDecimal percentage) {
}
