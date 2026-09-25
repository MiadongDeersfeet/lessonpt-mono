package com.yunki.lessonpt.relationship.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendStudentOtp(String email, String otp, StudentOtpPurpose purpose) {
        log.info("[LESSONPT OTP] purpose={} email={} otp={}", purpose, email, otp);
    }
}
