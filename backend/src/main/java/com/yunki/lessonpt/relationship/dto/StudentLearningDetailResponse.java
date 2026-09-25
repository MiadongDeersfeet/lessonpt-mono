package com.yunki.lessonpt.relationship.dto;

import java.util.List;

public record StudentLearningDetailResponse(
        Long studentId,
        String name,
        String email,
        String phone,
        List<StudentLearningLocationResponse> locations) {
}
