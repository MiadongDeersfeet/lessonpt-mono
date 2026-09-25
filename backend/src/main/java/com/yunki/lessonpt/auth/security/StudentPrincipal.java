package com.yunki.lessonpt.auth.security;

public record StudentPrincipal(Long teacherStudentAccessId, Long teacherStudentId, Long studentId) {
}
