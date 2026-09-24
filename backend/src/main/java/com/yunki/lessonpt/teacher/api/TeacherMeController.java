package com.yunki.lessonpt.teacher.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yunki.lessonpt.auth.security.TeacherPrincipal;
import com.yunki.lessonpt.teacher.dto.TeacherMeResponse;

/**
 * 이후 Location 생성처럼 강사 번호는 로그인 결과에서만 가져온다.
 * 요청 본문이나 쿼리의 teacherId는 쓰지 않는다.
 */
@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherMeController {

    @GetMapping("/me")
    public TeacherMeResponse me(@AuthenticationPrincipal TeacherPrincipal principal) {
        return new TeacherMeResponse(principal.teacherId(), principal.email());
    }
}
