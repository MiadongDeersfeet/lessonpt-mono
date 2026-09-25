package com.yunki.lessonpt.student.dto;

import java.util.List;

import com.yunki.lessonpt.relationship.dto.ProgressSummary;

public record StudentCurriculumView(String name, ProgressSummary progress, List<StudentCategoryView> categories) {
}
