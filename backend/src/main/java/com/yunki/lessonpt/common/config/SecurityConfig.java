package com.yunki.lessonpt.common.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.yunki.lessonpt.auth.jwt.JwtProperties;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.auth.security.AuthErrorWriter;
import com.yunki.lessonpt.auth.security.JwtAuthenticationFilter;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 가입, 로그인, 재발급만 인증 없이 연다.
 *
 * JWT 필터는 Security 체인 안에서 UsernamePasswordAuthenticationFilter보다 먼저 실행한다.
 * traceId 필터는 서블릿 필터로 그 앞에 있으므로 여기서 다시 넣지 않는다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            ObjectProvider<TeacherMapper> teacherMapper,
            ObjectProvider<TeacherAuthSessionMapper> sessionMapper,
            JwtProvider jwtProvider,
            AuthErrorWriter authErrorWriter) {
        return new JwtAuthenticationFilter(teacherMapper, sessionMapper, jwtProvider, authErrorWriter);
    }

    @Bean
    @Order(0)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthErrorWriter authErrorWriter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(
                        (request, response, exception) -> authErrorWriter.writeUnauthorized(request, response)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
