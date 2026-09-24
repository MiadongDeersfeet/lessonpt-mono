package com.yunki.lessonpt.curriculum.api;

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
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.dto.CategoryCreateRequest;
import com.yunki.lessonpt.curriculum.dto.CategoryResponse;
import com.yunki.lessonpt.curriculum.dto.CategoryUpdateRequest;
import com.yunki.lessonpt.curriculum.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/curriculums/{curriculumId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @Valid @RequestBody CategoryCreateRequest request) {
        Category created = categoryService.createCategory(principal.teacherId(), curriculumId, request.name());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{categoryId}")
                .buildAndExpand(created.getCategoryId())
                .toUri();
        return ResponseEntity.created(uri).body(toResponse(created));
    }

    @GetMapping
    public List<CategoryResponse> list(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId) {
        return categoryService.getCategories(principal.teacherId(), curriculumId).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId) {
        return toResponse(categoryService.getCategory(principal.teacherId(), curriculumId, categoryId));
    }

    @PatchMapping("/{categoryId}")
    public CategoryResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return toResponse(categoryService.updateCategory(principal.teacherId(), curriculumId, categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId) {
        categoryService.deleteCategory(principal.teacherId(), curriculumId, categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{categoryId}/restore")
    public CategoryResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long curriculumId,
            @PathVariable Long categoryId) {
        return toResponse(categoryService.restoreCategory(principal.teacherId(), curriculumId, categoryId));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getCategoryId(), category.getName(), category.getDisplayOrder());
    }
}
