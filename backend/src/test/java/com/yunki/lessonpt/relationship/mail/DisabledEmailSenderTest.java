package com.yunki.lessonpt.relationship.mail;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisabledEmailSenderTest {

    @Test
    void rejectsDeliveryWithoutSendingMail() {
        DisabledEmailSender sender = new DisabledEmailSender();
        assertThatThrownBy(() -> sender.sendStudentOtp("student@example.invalid", "123456", StudentOtpPurpose.GENERAL_LOGIN))
                .isInstanceOf(EmailDeliveryException.class);
    }
}
