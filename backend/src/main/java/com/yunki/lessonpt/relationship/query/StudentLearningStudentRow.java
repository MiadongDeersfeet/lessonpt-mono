package com.yunki.lessonpt.relationship.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentLearningStudentRow {
    private Long teacherStudentId;
    private Long studentId;
    private String name;
    private String email;
    private String phone;
}
