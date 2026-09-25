package com.yunki.lessonpt.student.query;

import com.yunki.lessonpt.common.model.ProgressStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentPortalMonitoringRow {

    private Long studentCurriculumId;
    private Long contentDetailId;
    private Integer currentBpm;
    private ProgressStatus progressStatus;
}
