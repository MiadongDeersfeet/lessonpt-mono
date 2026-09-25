package com.yunki.lessonpt.relationship.dto;

import java.util.List;

public record StudentLearningLocationResponse(
        Long teacherStudentLocationId,
        Long locationId,
        String locationName,
        String address,
        List<StudentLearningCurriculumResponse> studentCurriculums) {
}
