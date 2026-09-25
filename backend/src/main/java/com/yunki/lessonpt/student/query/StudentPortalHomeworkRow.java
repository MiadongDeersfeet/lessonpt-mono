package com.yunki.lessonpt.student.query;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentPortalHomeworkRow {

    private Long studentCurriculumId;
    private Long contentDetailId;
    private String homeworkContent;
    private LocalDateTime deadline;
    private Boolean completed;
    private String feedback;
}
