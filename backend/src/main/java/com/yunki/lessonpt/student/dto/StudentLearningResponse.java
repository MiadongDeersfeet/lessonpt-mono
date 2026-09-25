package com.yunki.lessonpt.student.dto;

import java.util.List;

public record StudentLearningResponse(List<StudentCurriculumView> curriculums) {
}
