package com.yunki.lessonpt.relationship.dto;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

public record TeacherStudentAccessResponse(
        Long teacherStudentAccessId,
        Long teacherStudentId,
        String publicAccessKey,
        LocalDateTime createdAt,
        RecordStatus status
) {
}
