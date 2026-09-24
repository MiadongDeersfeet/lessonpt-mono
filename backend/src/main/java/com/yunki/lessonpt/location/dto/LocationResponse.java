package com.yunki.lessonpt.location.dto;

import java.time.LocalDateTime;

public record LocationResponse(
        Long locationId,
        String name,
        Integer displayOrder,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
