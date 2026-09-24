package com.yunki.lessonpt.common.exception;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 클라이언트가 받는 공통 오류 본문이다.
 *
 * fieldErrors는 검증 실패가 아니면 빈 배열이다.
 * 시각은 문의 시각과 맞추려고 Asia/Seoul 오프셋으로 찍는다.
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        List<ValidationFieldError> fieldErrors
) {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public ErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String path, String traceId) {
        return of(errorCode, message, path, traceId, List.of());
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            String message,
            String path,
            String traceId,
            List<ValidationFieldError> fieldErrors) {
        return new ErrorResponse(
                OffsetDateTime.now(SEOUL),
                errorCode.status().value(),
                errorCode.name(),
                message,
                path,
                traceId,
                fieldErrors);
    }
}
