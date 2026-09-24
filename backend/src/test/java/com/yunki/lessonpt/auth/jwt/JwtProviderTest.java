package com.yunki.lessonpt.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private final JwtProvider provider = provider(Clock.systemUTC());

    @Test
    void accessTokenCarriesTeacherIdAndJti() {
        IssuedToken token = provider.createAccessToken(15L);

        VerifiedToken verified = provider.parseAccessToken(token.value());

        assertThat(verified.teacherId()).isEqualTo(15L);
        assertThat(verified.jti()).isEqualTo(token.jti());
    }

    @Test
    void rejectsTamperedToken() {
        IssuedToken token = provider.createAccessToken(15L);
        String tampered = token.value().substring(0, token.value().length() - 2) + "aa";

        assertThatThrownBy(() -> provider.parseAccessToken(tampered))
                .isInstanceOf(JwtVerificationException.class);
    }

    @Test
    void accessTokenExpiresInAboutSixtyMinutes() throws ParseException {
        Instant now = Instant.parse("2026-09-24T00:00:00Z");
        IssuedToken token = provider(Clock.fixed(now, ZoneOffset.UTC)).createAccessToken(15L);

        assertThat(lifetimeSeconds(token.value())).isBetween(3600L - 5, 3600L + 5);
    }

    @Test
    void refreshTokenExpiresInAboutFourteenDays() throws ParseException {
        Instant now = Instant.parse("2026-09-24T00:00:00Z");
        IssuedToken token = provider(Clock.fixed(now, ZoneOffset.UTC)).createRefreshToken(15L);

        long fourteenDays = Duration.ofDays(14).toSeconds();
        assertThat(lifetimeSeconds(token.value())).isBetween(fourteenDays - 5, fourteenDays + 5);
    }

    @Test
    void rejectsExpiredToken() {
        JwtProvider expired = provider(Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneOffset.UTC));
        IssuedToken token = expired.createAccessToken(15L, Duration.ofSeconds(-30));

        assertThatThrownBy(() -> expired.parseAccessToken(token.value()))
                .isInstanceOf(JwtVerificationException.class);
    }

    private long lifetimeSeconds(String token) throws ParseException {
        var claims = SignedJWT.parse(token).getJWTClaimsSet();
        return Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant()).toSeconds();
    }

    private JwtProvider provider(Clock clock) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenTtl(Duration.ofMinutes(60));
        properties.setRefreshTokenTtl(Duration.ofDays(14));
        return new JwtProvider(properties, clock);
    }
}
