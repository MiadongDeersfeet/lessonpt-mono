package com.yunki.lessonpt.teacher.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 비밀번호는 저장값과 같은지만 본다.
 * 가입 규칙과 다르더라도 여기서 다시 거절하지 않는다.
 */
public record TeacherLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
