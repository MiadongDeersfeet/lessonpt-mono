package com.yunki.lessonpt.relationship.mail;

public interface EmailSender {

    void sendStudentOtp(String email, String otp, StudentOtpPurpose purpose);
}
