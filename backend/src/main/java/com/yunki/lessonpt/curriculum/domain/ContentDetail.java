package com.yunki.lessonpt.curriculum.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 하나의 카테고리에 속한 내용이다.
 * 표시 순서는 같은 카테고리 안에서만 의미를 가진다.
 * TARGET_BPM은 완료 여부와 연결되지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ContentDetail {

    private Long contentDetailId;
    private Long categoryId;
    private String name;
    private Integer displayOrder;
    private String memo;
    private Integer targetBpm;
    private String evaluationMemo;
    private String youtubeUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
