package com.yunki.lessonpt.relationship.mail;

/**
 * 인증번호 메일을 보내지 못했다.
 * BusinessException이 아니므로 OTP 저장 트랜잭션은 롤백된다.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException() {
        super("학생 인증번호를 보내지 못했습니다.");
    }
}
