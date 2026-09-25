package com.yunki.lessonpt.common.exception;

import java.util.List;

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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
    private static final String VALIDATION_MESSAGE = "입력값을 확인해주세요.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ValidationFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationFieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        logClientError(request, ErrorCode.COMMON_INVALID_INPUT, VALIDATION_MESSAGE);
        return respond(ErrorCode.COMMON_INVALID_INPUT, VALIDATION_MESSAGE, request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<ValidationFieldError> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationFieldError(fieldName(violation), violation.getMessage()))
                .toList();
        logClientError(request, ErrorCode.COMMON_INVALID_INPUT, VALIDATION_MESSAGE);
        return respond(ErrorCode.COMMON_INVALID_INPUT, VALIDATION_MESSAGE, request, fieldErrors);
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.RESOURCE_FILE_TOO_LARGE;
        logClientError(request, errorCode, errorCode.message());
        return respond(errorCode, errorCode.message(), request);
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
        return respond(errorCode, message, request, List.of());
    }

    private ResponseEntity<ErrorResponse> respond(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request,
            List<ValidationFieldError> fieldErrors) {
        ErrorResponse body = ErrorResponse.of(
                errorCode, message, request.getRequestURI(), currentTraceId(), fieldErrors);
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

    private String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }
}
