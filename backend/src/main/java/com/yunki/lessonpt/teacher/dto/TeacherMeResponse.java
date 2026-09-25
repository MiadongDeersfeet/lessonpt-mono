package com.yunki.lessonpt.teacher.dto;

public record TeacherMeResponse(
        Long teacherId,
        String email,
        String name,
        String phone
) {
}
