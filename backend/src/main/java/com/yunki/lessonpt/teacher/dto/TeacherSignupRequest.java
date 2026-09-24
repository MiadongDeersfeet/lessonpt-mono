package com.yunki.lessonpt.teacher.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TeacherSignupRequest(
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "비밀번호는 영문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.")
        String password,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 30) String phone
) {
}
