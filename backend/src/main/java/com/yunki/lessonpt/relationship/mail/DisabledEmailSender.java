package com.yunki.lessonpt.relationship.mail;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Render teacher-only preview: never log or bypass student OTP verification. */
@Component
@Profile("render")
public class DisabledEmailSender implements EmailSender {
    @Override
    public void sendStudentOtp(String email, String otp, StudentOtpPurpose purpose) {
        throw new EmailDeliveryException();
    }
}
