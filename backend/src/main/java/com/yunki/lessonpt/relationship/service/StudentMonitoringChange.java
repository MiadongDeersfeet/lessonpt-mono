package com.yunki.lessonpt.relationship.service;

import com.yunki.lessonpt.common.model.ProgressStatus;

/**
 * 모니터링 수정 입력이다.
 * setter가 호출된 필드만 변경한다.
 * currentBpm과 memo의 명시적 null은 값을 비운다.
 * progressStatus의 명시적 null은 허용하지 않는다.
 */
public class StudentMonitoringChange {

    private Integer currentBpm;
    private boolean currentBpmSpecified;
    private ProgressStatus progressStatus;
    private boolean progressStatusSpecified;
    private String memo;
    private boolean memoSpecified;

    public boolean isCurrentBpmSpecified() {
        return currentBpmSpecified;
    }

    public Integer getCurrentBpm() {
        return currentBpm;
    }

    public void setCurrentBpm(Integer currentBpm) {
        this.currentBpmSpecified = true;
        this.currentBpm = currentBpm;
    }

    public boolean isProgressStatusSpecified() {
        return progressStatusSpecified;
    }

    public ProgressStatus getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(ProgressStatus progressStatus) {
        this.progressStatusSpecified = true;
        this.progressStatus = progressStatus;
    }

    public boolean isMemoSpecified() {
        return memoSpecified;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memoSpecified = true;
        this.memo = memo;
    }
}
