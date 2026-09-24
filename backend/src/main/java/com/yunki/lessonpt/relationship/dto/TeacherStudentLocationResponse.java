package com.yunki.lessonpt.relationship.dto;

public record TeacherStudentLocationResponse(
        Long teacherStudentLocationId,
        Long locationId,
        String locationName,
        String address
) {
}
