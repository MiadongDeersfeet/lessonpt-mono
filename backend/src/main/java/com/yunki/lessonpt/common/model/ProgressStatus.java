package com.yunki.lessonpt.common.model;

/**
 * 수강 내용의 진행 상태다.
 * 완료 여부와 BPM은 연결되지 않고, 상태 전이를 제한하지 않는다.
 */
public enum ProgressStatus {
    YET("Yet"),
    IN_PROGRESS("InProgress"),
    COMPLETED("Completed"),
    STOPPED("Stopped");

    private final String dbValue;

    ProgressStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static ProgressStatus fromDbValue(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        for (ProgressStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("PROGRESS_STATUS 값이 Yet, InProgress, Completed, Stopped가 아니다.");
    }
}
