package com.yunki.lessonpt.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank @Size(max = 200) String name
) {
}
