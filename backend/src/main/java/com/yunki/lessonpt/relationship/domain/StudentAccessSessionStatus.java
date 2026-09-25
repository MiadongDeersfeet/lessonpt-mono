package com.yunki.lessonpt.relationship.domain;

public enum StudentAccessSessionStatus {
    ACTIVE,
    REVOKED;

    public String dbValue() {
        return name();
    }

    public static StudentAccessSessionStatus fromDbValue(String raw) {
        if (raw == null) {
            return null;
        }
        return StudentAccessSessionStatus.valueOf(raw.trim());
    }
}
