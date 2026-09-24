package com.yunki.lessonpt.teacher.dto;

public record TeacherSignupResponse(
        Long teacherId,
        String email,
        String name,
        String phone,
        String role
) {
}
