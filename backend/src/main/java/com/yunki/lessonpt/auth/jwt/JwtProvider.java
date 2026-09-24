package com.yunki.lessonpt.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Access와 Refresh 서명을 한곳에서 만든다.
 * 화면과 서비스가 토큰 문자열을 직접 해석하지 않게 한다.
 */
@Component
public class JwtProvider {

    static final String ACCESS = "access";
    static final String REFRESH = "refresh";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final Clock clock;

    @Autowired
    public JwtProvider(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken createAccessToken(Long teacherId) {
        return issue(teacherId, ACCESS, properties.getAccessTokenTtl());
    }

    public IssuedToken createAccessToken(Long teacherId, Duration ttl) {
        return issue(teacherId, ACCESS, ttl);
    }

    public IssuedToken createRefreshToken(Long teacherId) {
        return issue(teacherId, REFRESH, properties.getRefreshTokenTtl());
    }

    public IssuedToken createRefreshToken(Long teacherId, Duration ttl) {
        return issue(teacherId, REFRESH, ttl);
    }

    public VerifiedToken parseAccessToken(String token) {
        return parse(token, ACCESS);
    }

    public VerifiedToken parseRefreshToken(String token) {
        return parse(token, REFRESH);
    }

    public Duration accessTokenTtl() {
        return properties.getAccessTokenTtl();
    }

    private IssuedToken issue(Long teacherId, String tokenType, Duration ttl) {
        Instant expiresAt = clock.instant().plus(ttl);
        String jti = UUID.randomUUID().toString();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(teacherId))
                .jwtID(jti)
                .claim("token_type", tokenType)
                .issueTime(Date.from(clock.instant()))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secretBytes()));
        } catch (JOSEException exception) {
            throw new IllegalStateException("JWT를 서명하지 못했다.", exception);
        }
        return new IssuedToken(jwt.serialize(), jti, LocalDateTime.ofInstant(expiresAt, SEOUL));
    }

    private VerifiedToken parse(String token, String expectedType) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secretBytes()))) {
                throw new JwtVerificationException("서명이 올바르지 않다.");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration == null || !expiration.toInstant().isAfter(clock.instant())) {
                throw new JwtVerificationException("만료된 토큰이다.");
            }
            if (!expectedType.equals(claims.getStringClaim("token_type"))) {
                throw new JwtVerificationException("토큰 종류가 다르다.");
            }
            return new VerifiedToken(Long.valueOf(claims.getSubject()), claims.getJWTID());
        } catch (JwtVerificationException exception) {
            throw exception;
        } catch (RuntimeException | java.text.ParseException | JOSEException exception) {
            throw new JwtVerificationException("토큰을 확인할 수 없다.");
        }
    }

    private byte[] secretBytes() {
        String secret = properties.getSecret() == null ? "" : properties.getSecret();
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret이 설정되지 않았다.");
        }
        return bytes;
    }
}
