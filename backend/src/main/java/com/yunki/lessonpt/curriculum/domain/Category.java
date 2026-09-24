package com.yunki.lessonpt.curriculum.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 하나의 커리큘럼에 속한 분류다.
 * 표시 순서는 같은 커리큘럼 안에서만 의미를 가진다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Category {

    private Long categoryId;
    private Long curriculumId;
    private String name;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
