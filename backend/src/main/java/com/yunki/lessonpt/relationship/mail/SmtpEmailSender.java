package com.yunki.lessonpt.relationship.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender javaMailSender;

    public SmtpEmailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendStudentOtp(String email, String otp, StudentOtpPurpose purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("LessonPT 인증번호");
        message.setText(body(otp, purpose));
        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            log.error("student otp mail delivery failed purpose={}", purpose);
            throw new EmailDeliveryException();
        }
    }

    private static String body(String otp, StudentOtpPurpose purpose) {
        String label = purpose == StudentOtpPurpose.INVITATION_ACCESS ? "수업 초대" : "로그인";
        return "LessonPT " + label + " 인증번호는 " + otp + "입니다.\n인증번호는 10분 동안 유효합니다.";
    }
}
