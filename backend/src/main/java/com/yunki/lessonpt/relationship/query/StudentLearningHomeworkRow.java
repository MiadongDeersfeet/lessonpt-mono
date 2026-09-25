package com.yunki.lessonpt.relationship.query;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentLearningHomeworkRow {
    private Long monitoringId;
    private Long homeworkId;
    private String homeworkContent;
    private LocalDateTime deadline;
    private Boolean completed;
    private String feedback;
}
