package com.yunki.lessonpt.auth.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yunki.lessonpt.auth.dto.AuthTokenResponse;
import com.yunki.lessonpt.auth.dto.RefreshTokenRequest;
import com.yunki.lessonpt.auth.service.TeacherAuthService;
import com.yunki.lessonpt.teacher.dto.TeacherLoginRequest;
import com.yunki.lessonpt.teacher.dto.TeacherSignupRequest;
import com.yunki.lessonpt.teacher.dto.TeacherSignupResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class TeacherAuthController {

    private final TeacherAuthService teacherAuthService;

    public TeacherAuthController(TeacherAuthService teacherAuthService) {
        this.teacherAuthService = teacherAuthService;
    }

    @PostMapping("/signup")
    public ResponseEntity<TeacherSignupResponse> signup(@Valid @RequestBody TeacherSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherAuthService.signup(request));
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody TeacherLoginRequest request) {
        return teacherAuthService.login(request);
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return teacherAuthService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        Object details = authentication == null ? null : authentication.getDetails();
        String accessJti = details instanceof String value ? value : null;
        teacherAuthService.logout(accessJti);
        return ResponseEntity.noContent().build();
    }
}
