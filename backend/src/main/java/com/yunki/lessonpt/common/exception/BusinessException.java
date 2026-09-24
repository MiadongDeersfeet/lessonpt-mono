package com.yunki.lessonpt.common.exception;

/**
 * 서비스가 의도적으로 던지는 업무 예외다.
 *
 * 없는 대상, 상태 충돌처럼 호출자가 처리할 수 있는 실패는
 * 이 예외와 ErrorCode로 올리고, HTTP 변환은 한곳에서 한다.
 * TeacherNotFound 같은 개별 예외는 기능을 만들 때 이 타입을 확장한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message == null || message.isBlank() ? errorCode.message() : message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
