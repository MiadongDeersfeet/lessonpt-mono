package com.yunki.lessonpt.student.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.student.dto.StudentLearningResponse;
import com.yunki.lessonpt.student.dto.StudentMeResponse;
import com.yunki.lessonpt.student.dto.StudentRelationshipResponse;
import com.yunki.lessonpt.student.service.StudentPortalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentPortalService studentPortalService;

    @GetMapping("/me")
    public StudentMeResponse me(@AuthenticationPrincipal StudentPrincipal principal) {
        return studentPortalService.me(principal);
    }

    @GetMapping("/relationships")
    public List<StudentRelationshipResponse> relationships(@AuthenticationPrincipal StudentPrincipal principal) {
        return studentPortalService.relationships(principal);
    }

    @GetMapping("/learning")
    public StudentLearningResponse learning(@AuthenticationPrincipal StudentPrincipal principal) {
        return studentPortalService.learning(principal);
    }
}
