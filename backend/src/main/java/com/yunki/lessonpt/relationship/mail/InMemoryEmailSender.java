package com.yunki.lessonpt.relationship.mail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "context"})
public class InMemoryEmailSender implements EmailSender {

    private final List<SentStudentOtp> sent = new CopyOnWriteArrayList<>();

    @Override
    public void sendStudentOtp(String email, String otp, StudentOtpPurpose purpose) {
        sent.add(new SentStudentOtp(email, otp, purpose));
    }

    public List<SentStudentOtp> sent() {
        return List.copyOf(sent);
    }

    public record SentStudentOtp(String email, String otp, StudentOtpPurpose purpose) {
    }
}
