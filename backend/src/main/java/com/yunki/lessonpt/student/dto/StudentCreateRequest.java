package com.yunki.lessonpt.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentCreateRequest(
        @NullOrNotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String name,
        @NullOrNotBlank @Size(max = 30) String phone
) {
}
