package com.yunki.lessonpt.curriculum.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;
import com.yunki.lessonpt.auth.jwt.IssuedToken;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.curriculum.dto.CategoryUpdateRequest;
import com.yunki.lessonpt.curriculum.service.CategoryService;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = {
        "spring.profiles.active=context",
        "lessonpt.jwt.secret=01234567890123456789012345678901"
})
@AutoConfigureMockMvc
class CategoryControllerSecurityTest {

    private static final String BASE = "/api/v1/curriculums/40/categories";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private TeacherMapper teacherMapper;

    @MockitoBean
    private TeacherAuthSessionMapper sessionMapper;

    @MockitoBean
    private LocationMapper locationMapper;

    @MockitoBean
    private StudentMapper studentMapper;

    @MockitoBean
    private TeacherStudentMapper teacherStudentMapper;

    @MockitoBean
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CurriculumMapper curriculumMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private ContentDetailMapper contentDetailMapper;

    @MockitoBean
    private StudentCurriculumMapper studentCurriculumMapper;

    @MockitoBean
    private StudentMonitoringMapper studentMonitoringMapper;

    @MockitoBean
    private HomeworkMapper homeworkMapper;

    private String bearerToken;

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"준비"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/70")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(BASE + "/70").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"개정"}
                """)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/70")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/70/restore")).andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsCreatedCategoryForPrincipal() throws Exception {
        authenticate(21L);
        when(categoryService.createCategory(21L, 40L, "준비")).thenReturn(category(70L, "준비", 1));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"준비"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString(BASE + "/70")))
                .andExpect(jsonPath("$.categoryId").value(70))
                .andExpect(jsonPath("$.name").value("준비"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.teacherId").doesNotExist())
                .andExpect(jsonPath("$.curriculumId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        verify(categoryService).createCategory(21L, 40L, "준비");
    }

    @Test
    void createRejectsBlankAndTooLongName() throws Exception {
        authenticate(21L);

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "가".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void listReturnsCategoriesOrEmptyArray() throws Exception {
        authenticate(21L);
        when(categoryService.getCategories(21L, 40L)).thenReturn(List.of(category(70L, "준비", 1)));

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(70))
                .andExpect(jsonPath("$[0].name").value("준비"))
                .andExpect(jsonPath("$[0].displayOrder").value(1));

        when(categoryService.getCategories(21L, 40L)).thenReturn(List.of());
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void detailMapsNotFoundToErrorResponse() throws Exception {
        authenticate(21L);
        when(categoryService.getCategory(21L, 40L, 70L)).thenReturn(category(70L, "준비", 1));

        mockMvc.perform(get(BASE + "/70").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("준비"));

        when(categoryService.getCategory(21L, 40L, 71L)).thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(get(BASE + "/71").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(BASE + "/71"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updateSendsPrincipalAndRejectsBlankOrNullName() throws Exception {
        authenticate(21L);
        when(categoryService.updateCategory(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.eq(40L),
                org.mockito.ArgumentMatchers.eq(70L),
                org.mockito.ArgumentMatchers.any())).thenReturn(category(70L, "준비운동", 1));

        mockMvc.perform(patch(BASE + "/70")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"준비운동"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("준비운동"))
                .andExpect(jsonPath("$.displayOrder").value(1));

        org.mockito.ArgumentCaptor<CategoryUpdateRequest> captor = org.mockito.ArgumentCaptor.forClass(CategoryUpdateRequest.class);
        verify(categoryService).updateCategory(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.eq(40L),
                org.mockito.ArgumentMatchers.eq(70L),
                captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getName()).isEqualTo("준비운동");

        mockMvc.perform(patch(BASE + "/70")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        mockMvc.perform(patch(BASE + "/70")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        when(categoryService.updateCategory(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.eq(41L),
                org.mockito.ArgumentMatchers.eq(70L),
                org.mockito.ArgumentMatchers.any())).thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(patch("/api/v1/curriculums/41/categories/70")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"다른과정"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        authenticate(21L);

        mockMvc.perform(delete(BASE + "/70").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(categoryService).deleteCategory(21L, 40L, 70L);
    }

    @Test
    void restoreReturnsCategoryAndMapsConflict() throws Exception {
        authenticate(21L);
        when(categoryService.restoreCategory(21L, 40L, 70L)).thenReturn(category(70L, "준비", 4));

        mockMvc.perform(post(BASE + "/70/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(70))
                .andExpect(jsonPath("$.displayOrder").value(4))
                .andExpect(jsonPath("$.teacherId").doesNotExist());

        when(categoryService.restoreCategory(21L, 40L, 71L)).thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post(BASE + "/71/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"))
                .andExpect(jsonPath("$.path").value(BASE + "/71/restore"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private Category category(Long categoryId, String name, int displayOrder) {
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setCurriculumId(40L);
        category.setName(name);
        category.setDisplayOrder(displayOrder);
        category.setStatus(RecordStatus.ACTIVE);
        return category;
    }

    private void authenticate(Long teacherId) {
        IssuedToken token = jwtProvider.createAccessToken(teacherId);
        bearerToken = "Bearer " + token.value();
        Teacher teacher = new Teacher();
        teacher.setTeacherId(teacherId);
        teacher.setEmail("teacher@lessonpt.local");
        teacher.setStatus(RecordStatus.ACTIVE);
        TeacherAuthSession session = new TeacherAuthSession();
        session.setTeacherId(teacherId);
        session.setAccessJti(token.jti());
        when(sessionMapper.selectActiveSessionByAccessJti(token.jti())).thenReturn(session);
        when(teacherMapper.selectActiveTeacherById(teacherId)).thenReturn(teacher);
    }
}
