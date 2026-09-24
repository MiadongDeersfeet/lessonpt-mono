package com.yunki.lessonpt.relationship.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.yunki.lessonpt.auth.security.TeacherPrincipal;
import com.yunki.lessonpt.relationship.domain.Homework;
import com.yunki.lessonpt.relationship.dto.HomeworkCreateRequest;
import com.yunki.lessonpt.relationship.dto.HomeworkResponse;
import com.yunki.lessonpt.relationship.dto.HomeworkUpdateRequest;
import com.yunki.lessonpt.relationship.service.HomeworkChange;
import com.yunki.lessonpt.relationship.service.HomeworkService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/monitorings/{monitoringId}/homeworks")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    @PostMapping
    public ResponseEntity<HomeworkResponse> create(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long monitoringId,
            @Valid @RequestBody HomeworkCreateRequest request) {
        Homework created = homeworkService.createHomework(
                principal.teacherId(),
                monitoringId,
                request.homeworkContent(),
                request.deadline(),
                request.feedback());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{homeworkId}")
                .buildAndExpand(created.getHomeworkId())
                .toUri();
        return ResponseEntity.created(uri).body(toResponse(created));
    }

    @GetMapping
    public List<HomeworkResponse> list(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long monitoringId) {
        return homeworkService.getHomeworks(principal.teacherId(), monitoringId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{homeworkId}")
    public HomeworkResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long monitoringId,
            @PathVariable Long homeworkId) {
        return toResponse(homeworkService.getHomework(principal.teacherId(), monitoringId, homeworkId));
    }

    @PatchMapping("/{homeworkId}")
    public HomeworkResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long monitoringId,
            @PathVariable Long homeworkId,
            @Valid @RequestBody HomeworkUpdateRequest request) {
        return toResponse(homeworkService.updateHomework(
                principal.teacherId(), monitoringId, homeworkId, toChange(request)));
    }

    @DeleteMapping("/{homeworkId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long monitoringId,
            @PathVariable Long homeworkId) {
        homeworkService.deleteHomework(principal.teacherId(), monitoringId, homeworkId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{homeworkId}/restore")
    public HomeworkResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long monitoringId,
            @PathVariable Long homeworkId) {
        return toResponse(homeworkService.restoreHomework(principal.teacherId(), monitoringId, homeworkId));
    }

    private HomeworkChange toChange(HomeworkUpdateRequest request) {
        HomeworkChange change = new HomeworkChange();
        if (request.isHomeworkContentSpecified()) {
            change.setHomeworkContent(request.getHomeworkContent());
        }
        if (request.isDeadlineSpecified()) {
            change.setDeadline(request.getDeadline());
        }
        if (request.isCompletedSpecified()) {
            change.setCompleted(request.getCompleted());
        }
        if (request.isFeedbackSpecified()) {
            change.setFeedback(request.getFeedback());
        }
        return change;
    }

    private HomeworkResponse toResponse(Homework homework) {
        return new HomeworkResponse(
                homework.getHomeworkId(),
                homework.getHomeworkContent(),
                homework.getDeadline(),
                homework.getCompleted(),
                homework.getFeedback());
    }
}
