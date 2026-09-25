package com.yunki.lessonpt.relationship.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yunki.lessonpt.relationship.dto.OtpIssueRequest;
import com.yunki.lessonpt.relationship.dto.OtpVerificationResponse;
import com.yunki.lessonpt.relationship.dto.OtpVerifyRequest;
import com.yunki.lessonpt.relationship.service.IssuedStudentSession;
import com.yunki.lessonpt.relationship.service.StudentLoginVerificationService;
import com.yunki.lessonpt.relationship.service.StudentSessionCookieWriter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student-login/otp")
@RequiredArgsConstructor
public class StudentLoginOtpController {

    private final StudentLoginVerificationService studentLoginVerificationService;
    private final StudentSessionCookieWriter studentSessionCookieWriter;

    @PostMapping
    public ResponseEntity<Void> issue(@Valid @RequestBody OtpIssueRequest request) {
        studentLoginVerificationService.issue(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify")
    public OtpVerificationResponse verify(
            @Valid @RequestBody OtpVerifyRequest request,
            HttpServletResponse response) {
        IssuedStudentSession issued = studentLoginVerificationService.verify(request.email(), request.otp());
        studentSessionCookieWriter.write(response, issued);
        return new OtpVerificationResponse(true);
    }
}
