package com.yunki.lessonpt.relationship.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * prod에서 SMTP 호스트가 없을 때 쓴다. 메일을 보내지 않는다.
 * havingValue는 빈 문자열이 되면 Spring Boot가 “아무 값이나”로 해석하므로 실제 호스트와 겹치지 않는 값을 둔다.
 */
@Component
@Profile("prod")
@ConditionalOnProperty(name = "spring.mail.host", havingValue = "__none__", matchIfMissing = true)
public class DisabledEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(DisabledEmailSender.class);

    @Override
    public void sendStudentOtp(String email, String otp, StudentOtpPurpose purpose) {
        log.warn("student otp mail is disabled purpose={}", purpose);
        throw new EmailDeliveryException();
    }
}
