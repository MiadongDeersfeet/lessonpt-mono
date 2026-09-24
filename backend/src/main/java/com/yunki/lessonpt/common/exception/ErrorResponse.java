package com.yunki.lessonpt.common.exception;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 클라이언트가 받는 공통 오류 본문이다.
 *
 * 필드 오류 목록은 아직 넣지 않는다. 대표 메시지 하나로 충분하고,
 * 나중에 필드를 늘려도 이 형식을 깨지 않게 record로 둔다.
 * 시각은 문의 시각과 맞추려고 Asia/Seoul 오프셋으로 찍는다.
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId
) {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public static ErrorResponse of(ErrorCode errorCode, String message, String path, String traceId) {
        return new ErrorResponse(
                OffsetDateTime.now(SEOUL),
                errorCode.status().value(),
                errorCode.name(),
                message,
                path,
                traceId);
    }
}
