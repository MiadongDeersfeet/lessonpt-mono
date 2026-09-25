package com.yunki.lessonpt.student.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentPortalIdentity {

    private Long teacherStudentId;
    private Long studentId;
    private String name;
}
