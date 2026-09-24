package com.yunki.lessonpt.curriculum.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사 소유의 커리큘럼이다.
 * 장소에는 묶이지 않고, 표시 순서는 같은 강사 안에서만 의미를 가진다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Curriculum {

    private Long curriculumId;
    private Long teacherId;
    private String name;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
