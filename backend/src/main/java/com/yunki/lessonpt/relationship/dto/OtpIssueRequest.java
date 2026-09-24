package com.yunki.lessonpt.relationship.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpIssueRequest(
        @NotBlank @Email @Size(max = 255) String email
) {
}
