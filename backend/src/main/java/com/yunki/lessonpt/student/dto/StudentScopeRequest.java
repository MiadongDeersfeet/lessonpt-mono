package com.yunki.lessonpt.student.dto;

import jakarta.validation.constraints.NotNull;

public record StudentScopeRequest(@NotNull Long teacherStudentAccessId) {
}
