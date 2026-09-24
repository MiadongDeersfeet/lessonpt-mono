package com.yunki.lessonpt.common.exception;

/**
 * Validation 오류에서 어떤 입력 필드가 실패했는지 전달한다.
 *
 * 일반 오류에서도 fieldErrors를 빈 배열로 유지해
 * 클라이언트가 오류 유형마다 응답 구조를 다르게 해석하지 않도록 한다.
 */
public record ValidationFieldError(
        String field,
        String message
) {
}
