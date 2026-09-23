package com.yunki.lessonpt.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 지금 단계의 보안 설정이다.
 *
 * Teacher JWT는 아직 없다. 로그인 폼과 기본 메모리 사용자도 만들지 않는다.
 * 기본 사용자는 application.yml에서 UserDetailsService 자동 설정을 빼서 막는다.
 *
 * /actuator/health 는 인증 없이 열어서 Oracle 연결 상태를 볼 수 있게 한다.
 * 세션 로그인 폼을 쓰지 않으므로 CSRF도 꺼 둔다.
 * 그 외 경로는 일단 모두 허용해 두고, 인증 규칙은 Teacher Auth를 넣을 때 잠근다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().permitAll());
        return http.build();
    }
}
