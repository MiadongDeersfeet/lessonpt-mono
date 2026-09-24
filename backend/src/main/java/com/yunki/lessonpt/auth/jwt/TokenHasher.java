package com.yunki.lessonpt.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Refresh Token은 조회용 해시만 저장한다.
 * BCrypt는 같은 입력도 해시가 달라서 세션 조회 키로 쓸 수 없다.
 */
@Component
public class TokenHasher {

    public String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없다.", exception);
        }
    }
}
