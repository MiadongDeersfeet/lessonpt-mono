package com.yunki.lessonpt.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인과 무관한 공통 오류 코드다.
 *
 * Teacher, Student 같은 기능 오류는 각 기능을 만들 때 따로 추가한다.
 * 여기서는 HTTP 상태와 사용자에게 보여 줄 기본 문구만 고정해,
 * 화면마다 오류 문장을 다시 해석하지 않게 한다.
 */
public enum ErrorCode {

    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    AUTH_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
    STUDENT_SCOPE_REQUIRED(HttpStatus.CONFLICT, "수업 범위를 먼저 선택해 주세요."),
    ORDER_CONFLICT(HttpStatus.CONFLICT, "다른 요청이 장소 순서를 변경 중입니다. 잠시 후 다시 시도해 주세요."),
    RESOURCE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 자료 종류입니다."),
    RESOURCE_INVALID_FILE(HttpStatus.BAD_REQUEST, "올릴 수 있는 파일이 아닙니다."),
    RESOURCE_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기가 허용 한도를 넘습니다."),
    RESOURCE_STORAGE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "저장 용량 한도에 도달해 파일을 올릴 수 없습니다."),
    RESOURCE_STORAGE_CONFLICT(HttpStatus.CONFLICT, "다른 업로드가 저장 용량을 확인 중입니다. 잠시 후 다시 시도해 주세요."),
    RESOURCE_RANGE_NOT_SATISFIABLE(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "요청한 파일 범위를 읽을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청 방식입니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
