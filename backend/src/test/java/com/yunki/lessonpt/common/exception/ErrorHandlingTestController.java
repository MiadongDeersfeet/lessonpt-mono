package com.yunki.lessonpt.common.exception;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공통 예외 처리만 확인하는 테스트 전용 입구다.
 * 운영 API가 아니므로 테스트 클래스패스에만 둔다.
 */
@RestController
@RequestMapping("/api/test/errors")
public class ErrorHandlingTestController {

    @PostMapping("/validation")
    public void validation(@RequestBody @jakarta.validation.Valid SampleRequest request) {
    }

    @GetMapping("/not-found")
    public void notFound() {
        throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
    }

    @GetMapping("/conflict")
    public void conflict() {
        throw new BusinessException(ErrorCode.COMMON_CONFLICT, "같은 순서가 이미 있습니다.");
    }

    @GetMapping("/unexpected")
    public void unexpected() {
        throw new RuntimeException("DB password is abc...");
    }

    public record SampleRequest(
            @NotBlank @Email(message = "이메일 형식이 올바르지 않습니다.") String email
    ) {
    }
}
