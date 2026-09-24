package com.yunki.lessonpt.relationship.domain;

/**
 * TB_STUDENT_EMAIL_VERIFICATION.VERIFICATION_STATUS 값이다.
 */
public enum StudentEmailVerificationStatus {
    PENDING,
    CONSUMED,
    INVALIDATED,
    LOCKED;

    public String dbValue() {
        return name();
    }

    public static StudentEmailVerificationStatus fromDbValue(String raw) {
        if (raw == null) {
            return null;
        }
        return StudentEmailVerificationStatus.valueOf(raw.trim());
    }
}
