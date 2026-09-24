package com.yunki.lessonpt.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String address
) {
}
