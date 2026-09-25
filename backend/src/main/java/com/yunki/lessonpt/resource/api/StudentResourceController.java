package com.yunki.lessonpt.resource.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.resource.service.ContentResourceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student/resources")
@RequiredArgsConstructor
public class StudentResourceController {

    private final ContentResourceService contentResourceService;

    @GetMapping("/{resourceId}/content")
    public ResponseEntity<StreamingResponseBody> content(
            @AuthenticationPrincipal StudentPrincipal principal,
            @PathVariable Long resourceId,
            @RequestParam(name = "disposition", required = false) String disposition,
            @RequestHeader(name = HttpHeaders.RANGE, required = false) String range) {
        return ResourceContentResponses.write(
                contentResourceService.openForStudent(principal, resourceId, range, disposition));
    }
}
