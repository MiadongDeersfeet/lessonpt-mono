package com.yunki.lessonpt.student.dto;

public record StudentResponse(
        Long studentId,
        String email,
        String name,
        String phone,
        Long teacherStudentId
) {
}
