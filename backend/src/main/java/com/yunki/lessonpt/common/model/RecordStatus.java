package com.yunki.lessonpt.common.model;

/**
 * 레코드가 아직 유효한지 나타낸다.
 *
 * DB의 Y/N은 완료 여부가 아니라 Soft Delete와 함께 쓰는 상태다.
 * 그래서 Boolean으로 받지 않는다.
 */
public enum RecordStatus {
    ACTIVE,
    INACTIVE
}
