package com.yunki.lessonpt.relationship.dto;

import java.math.BigDecimal;

public record ProgressSummary(int completedCount, int totalCount, BigDecimal percentage) {
}
