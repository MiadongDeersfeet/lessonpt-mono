package com.yunki.lessonpt.student.dto;

import java.util.List;

public record StudentRelationshipResponse(
        Long teacherStudentAccessId,
        Long teacherStudentId,
        String teacherName,
        List<StudentRelationshipLocationResponse> locations) {
}
