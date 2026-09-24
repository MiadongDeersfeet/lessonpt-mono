package com.yunki.lessonpt.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.exception.ErrorResponse;
import com.yunki.lessonpt.common.exception.TraceIdFilter;

/**
 * 필터 단계에서 막힌 인증도 공통 오류 JSON으로 내려 준다.
 * 토큰이나 비밀번호 내용은 본문에 넣지 않는다.
 */
@Component
public class AuthErrorWriter {

    private final JsonMapper jsonMapper;

    public AuthErrorWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        ErrorResponse body = ErrorResponse.of(
                ErrorCode.AUTH_FAILED,
                ErrorCode.AUTH_FAILED.message(),
                request.getRequestURI(),
                traceId == null ? "" : traceId);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
