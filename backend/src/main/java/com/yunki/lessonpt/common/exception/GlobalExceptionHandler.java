package com.yunki.lessonpt.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

/**
 * REST API에서 발생한 예외를 동일한 형식으로 변환한다.
 *
 * 화면이나 클라이언트마다 예외 구조를 따로 해석하지 않도록 하고,
 * traceId를 함께 내려 장애 문의 시 서버 로그와 연결할 수 있게 한다.
 * 인증 실패도 나중에 이 응답 형식을 재사용할 수 있게, Security 설정은 여기서 바꾸지 않는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = representativeFieldMessage(exception);
        logClientError(request, ErrorCode.COMMON_INVALID_INPUT, message);
        return respond(ErrorCode.COMMON_INVALID_INPUT, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        String message = representativeConstraintMessage(exception);
        logClientError(request, ErrorCode.COMMON_INVALID_INPUT, message);
        return respond(ErrorCode.COMMON_INVALID_INPUT, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        String message = "요청 본문을 해석할 수 없습니다.";
        logClientError(request, ErrorCode.COMMON_INVALID_INPUT, message);
        return respond(ErrorCode.COMMON_INVALID_INPUT, message, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = exception.errorCode();
        if (errorCode.status().is5xxServerError()) {
            log.error("traceId={} code={} path={}", currentTraceId(), errorCode.name(), request.getRequestURI(), exception);
        } else {
            logClientError(request, errorCode, exception.getMessage());
        }
        return respond(errorCode, exception.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.COMMON_METHOD_NOT_ALLOWED;
        logClientError(request, errorCode, errorCode.message());
        return respond(errorCode, errorCode.message(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("traceId={} path={}", currentTraceId(), request.getRequestURI(), exception);
        ErrorCode errorCode = ErrorCode.COMMON_INTERNAL_ERROR;
        return respond(errorCode, errorCode.message(), request);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(errorCode, message, request.getRequestURI(), currentTraceId());
        return ResponseEntity.status(errorCode.status()).body(body);
    }

    private void logClientError(HttpServletRequest request, ErrorCode errorCode, String message) {
        log.warn("traceId={} code={} path={} message={}",
                currentTraceId(), errorCode.name(), request.getRequestURI(), message);
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        return traceId == null ? "" : traceId;
    }

    /**
     * 여러 필드가 동시에 실패해도 대표 오류 하나만 본문에 담는다.
     * 필드 목록이 필요해지면 이 선택 지점만 바꾸면 된다.
     */
    private String representativeFieldMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            return ErrorCode.COMMON_INVALID_INPUT.message();
        }
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String representativeConstraintMessage(ConstraintViolationException exception) {
        ConstraintViolation<?> violation = exception.getConstraintViolations().stream().findFirst().orElse(null);
        if (violation == null) {
            return ErrorCode.COMMON_INVALID_INPUT.message();
        }
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        String field = separator >= 0 ? path.substring(separator + 1) : path;
        return field + ": " + violation.getMessage();
    }
}
