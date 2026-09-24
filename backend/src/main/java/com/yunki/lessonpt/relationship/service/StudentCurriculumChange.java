package com.yunki.lessonpt.relationship.service;

/**
 * 수강 메모 수정 입력이다.
 * setter가 호출된 경우만 변경한다. 명시적 null은 메모를 비운다.
 */
public class StudentCurriculumChange {

    private String memo;
    private boolean memoSpecified;

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
