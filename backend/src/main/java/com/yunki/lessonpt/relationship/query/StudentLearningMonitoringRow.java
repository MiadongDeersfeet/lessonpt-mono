package com.yunki.lessonpt.relationship.query;

import com.yunki.lessonpt.common.model.ProgressStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentLearningMonitoringRow {
    private Long studentCurriculumId;
    private Long monitoringId;
    private Long contentDetailId;
    private String contentDetailName;
    private Integer displayOrder;
    private Integer targetBpm;
    private Integer currentBpm;
    private ProgressStatus progressStatus;
    private String memo;
}
