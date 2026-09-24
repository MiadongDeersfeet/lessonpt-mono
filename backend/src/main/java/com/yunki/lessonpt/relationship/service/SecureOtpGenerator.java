package com.yunki.lessonpt.relationship.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class SecureOtpGenerator implements OtpGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return "%06d".formatted(random.nextInt(1_000_000));
    }
}
