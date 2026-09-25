package com.yunki.lessonpt.curriculum.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
import org.mockito.ArgumentCaptor;
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
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.StudentEmailVerificationMapper;
import com.yunki.lessonpt.relationship.mapper.StudentAccessSessionMapper;
import com.yunki.lessonpt.student.mapper.StudentPortalMapper;
import com.yunki.lessonpt.relationship.mapper.StudentLearningQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.curriculum.service.ContentDetailChange;
import com.yunki.lessonpt.curriculum.service.ContentDetailService;
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
class ContentDetailControllerSecurityTest {

    private static final String BASE = "/api/v1/curriculums/40/categories/50/content-details";

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

    @MockitoBean
    private ProgressQueryMapper progressQueryMapper;

    @MockitoBean
    private TeacherStudentAccessMapper teacherStudentAccessMapper;

    @MockitoBean
    private StudentEmailVerificationMapper studentEmailVerificationMapper;

    @MockitoBean
    private StudentAccessSessionMapper studentAccessSessionMapper;

    @MockitoBean
    private StudentPortalMapper studentPortalMapper;

    @MockitoBean
    private StudentLearningQueryMapper studentLearningQueryMapper;

    @MockitoBean
    private ContentDetailService contentDetailService;

    private String bearerToken;

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"스케일"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/90")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(BASE + "/90").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"개정"}
                """)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/90")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/90/restore")).andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsCreatedDetailForPrincipal() throws Exception {
        authenticate(21L);
        when(contentDetailService.createContentDetail(eq(21L), eq(40L), eq(50L), any()))
                .thenReturn(detail(90L, "스케일", 1, "메모", 80));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"스케일","memo":"메모","targetBpm":80,"evaluationMemo":"평가","sheetUrl":"https://example.com/sheet","youtubeUrl":"https://youtu.be/scale","audioUrl":"https://example.com/scale.mp3"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString(BASE + "/90")))
                .andExpect(jsonPath("$.contentDetailId").value(90))
                .andExpect(jsonPath("$.name").value("스케일"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.memo").value("메모"))
                .andExpect(jsonPath("$.targetBpm").value(80))
                .andExpect(jsonPath("$.evaluationMemo").value("평가"))
                .andExpect(jsonPath("$.sheetUrl").value("https://example.com/sheet"))
                .andExpect(jsonPath("$.youtubeUrl").value("https://youtu.be/scale"))
                .andExpect(jsonPath("$.audioUrl").value("https://example.com/scale.mp3"))
                .andExpect(jsonPath("$.teacherId").doesNotExist())
                .andExpect(jsonPath("$.curriculumId").doesNotExist())
                .andExpect(jsonPath("$.categoryId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        ArgumentCaptor<ContentDetailChange> captor = ArgumentCaptor.forClass(ContentDetailChange.class);
        verify(contentDetailService).createContentDetail(eq(21L), eq(40L), eq(50L), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getName()).isEqualTo("스케일");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getTargetBpm()).isEqualTo(80);
    }

    @Test
    void createAllowsNullBpmAndRejectsInvalidFields() throws Exception {
        authenticate(21L);
        when(contentDetailService.createContentDetail(eq(21L), eq(40L), eq(50L), any()))
                .thenReturn(detail(90L, "스케일", 1, null, null));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"스케일","targetBpm":null}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<ContentDetailChange> captor = ArgumentCaptor.forClass(ContentDetailChange.class);
        verify(contentDetailService).createContentDetail(eq(21L), eq(40L), eq(50L), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getTargetBpm()).isNull();

        expectBadField("""
                {"memo":"메모"}
                """, "name");
        expectBadField("""
                {"name":" "}
                """, "name");
        expectBadField("{\"name\":\"" + "가".repeat(201) + "\"}", "name");
        expectBadField("""
                {"name":"스케일","targetBpm":59}
                """, "targetBpm");
        expectBadField("""
                {"name":"스케일","targetBpm":241}
                """, "targetBpm");
        expectBadField("{\"name\":\"스케일\",\"sheetUrl\":\"" + "a".repeat(2001) + "\"}", "sheetUrl");
    }

    @Test
    void listReturnsDetailsOrEmptyArray() throws Exception {
        authenticate(21L);
        when(contentDetailService.getContentDetails(21L, 40L, 50L))
                .thenReturn(List.of(detail(90L, "스케일", 1, "메모", 80)));

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contentDetailId").value(90))
                .andExpect(jsonPath("$[0].name").value("스케일"))
                .andExpect(jsonPath("$[0].displayOrder").value(1));

        when(contentDetailService.getContentDetails(21L, 40L, 50L)).thenReturn(List.of());
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void detailMapsNotFoundToErrorResponse() throws Exception {
        authenticate(21L);
        when(contentDetailService.getContentDetail(21L, 40L, 50L, 90L))
                .thenReturn(detail(90L, "스케일", 1, "메모", 80));

        mockMvc.perform(get(BASE + "/90").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("스케일"));

        when(contentDetailService.getContentDetail(21L, 40L, 50L, 91L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        mockMvc.perform(get(BASE + "/91").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(BASE + "/91"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void updatePreservesSpecifiedFlags() throws Exception {
        authenticate(21L);
        when(contentDetailService.updateContentDetail(eq(21L), eq(40L), eq(50L), eq(90L), any()))
                .thenReturn(detail(90L, "싱글 스트로크", 1, "다음 주까지 반복", 140));

        patchBody("""
                {"name":"싱글 스트로크"}
                """);
        patchBody("""
                {"targetBpm":120}
                """);
        patchBody("""
                {"targetBpm":null}
                """);
        patchBody("""
                {"memo":null}
                """);
        patchBody("""
                {"youtubeUrl":null}
                """);
        patchBody("""
                {"memo":"다음 주까지 반복","targetBpm":140,"youtubeUrl":null}
                """);

        ArgumentCaptor<ContentDetailChange> captor = ArgumentCaptor.forClass(ContentDetailChange.class);
        verify(contentDetailService, times(6))
                .updateContentDetail(eq(21L), eq(40L), eq(50L), eq(90L), captor.capture());
        List<ContentDetailChange> changes = captor.getAllValues();

        org.assertj.core.api.Assertions.assertThat(changes.get(0).isNameSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(changes.get(0).getName()).isEqualTo("싱글 스트로크");
        org.assertj.core.api.Assertions.assertThat(changes.get(0).isMemoSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(changes.get(0).isTargetBpmSpecified()).isFalse();

        org.assertj.core.api.Assertions.assertThat(changes.get(1).isTargetBpmSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(changes.get(1).getTargetBpm()).isEqualTo(120);
        org.assertj.core.api.Assertions.assertThat(changes.get(1).isNameSpecified()).isFalse();

        org.assertj.core.api.Assertions.assertThat(changes.get(2).isTargetBpmSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(changes.get(2).getTargetBpm()).isNull();
        org.assertj.core.api.Assertions.assertThat(changes.get(2).isMemoSpecified()).isFalse();

        org.assertj.core.api.Assertions.assertThat(changes.get(3).isMemoSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(changes.get(3).getMemo()).isNull();
        org.assertj.core.api.Assertions.assertThat(changes.get(3).isNameSpecified()).isFalse();

        org.assertj.core.api.Assertions.assertThat(changes.get(4).isYoutubeUrlSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(changes.get(4).getYoutubeUrl()).isNull();
        org.assertj.core.api.Assertions.assertThat(changes.get(4).isMemoSpecified()).isFalse();

        ContentDetailChange combined = changes.get(5);
        org.assertj.core.api.Assertions.assertThat(combined.isMemoSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(combined.getMemo()).isEqualTo("다음 주까지 반복");
        org.assertj.core.api.Assertions.assertThat(combined.isTargetBpmSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(combined.getTargetBpm()).isEqualTo(140);
        org.assertj.core.api.Assertions.assertThat(combined.isYoutubeUrlSpecified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(combined.getYoutubeUrl()).isNull();
        org.assertj.core.api.Assertions.assertThat(combined.isNameSpecified()).isFalse();
        org.assertj.core.api.Assertions.assertThat(combined.isSheetUrlSpecified()).isFalse();
    }

    @Test
    void updateRejectsInvalidNameBpmAndUrl() throws Exception {
        authenticate(21L);

        expectBadPatch("""
                {"name":null}
                """, "name");
        expectBadPatch("""
                {"name":" "}
                """, "name");
        expectBadPatch("{\"name\":\"" + "가".repeat(201) + "\"}", "name");
        expectBadPatch("""
                {"targetBpm":59}
                """, "targetBpm");
        expectBadPatch("""
                {"targetBpm":241}
                """, "targetBpm");
        expectBadPatch("{\"youtubeUrl\":\"" + "a".repeat(2001) + "\"}", "youtubeUrl");

        when(contentDetailService.updateContentDetail(eq(21L), eq(40L), eq(50L), eq(90L), any()))
                .thenReturn(detail(90L, "스케일", 1, null, null));
        mockMvc.perform(patch(BASE + "/90")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetBpm":null,"sheetUrl":null}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        authenticate(21L);

        mockMvc.perform(delete(BASE + "/90").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(contentDetailService).deleteContentDetail(21L, 40L, 50L, 90L);
    }

    @Test
    void restoreReturnsDetailAndMapsConflict() throws Exception {
        authenticate(21L);
        when(contentDetailService.restoreContentDetail(21L, 40L, 50L, 90L))
                .thenReturn(detail(90L, "스케일", 4, null, null));

        mockMvc.perform(post(BASE + "/90/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentDetailId").value(90))
                .andExpect(jsonPath("$.displayOrder").value(4))
                .andExpect(jsonPath("$.teacherId").doesNotExist());

        when(contentDetailService.restoreContentDetail(21L, 40L, 50L, 91L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_CONFLICT));
        mockMvc.perform(post(BASE + "/91/restore").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"))
                .andExpect(jsonPath("$.path").value(BASE + "/91/restore"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private void patchBody(String body) throws Exception {
        mockMvc.perform(patch(BASE + "/90")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentDetailId").value(90));
    }

    private void expectBadField(String body, String field) throws Exception {
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private void expectBadPatch(String body, String field) throws Exception {
        mockMvc.perform(patch(BASE + "/90")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    }

    private ContentDetail detail(Long contentDetailId, String name, int displayOrder, String memo, Integer targetBpm) {
        ContentDetail contentDetail = new ContentDetail();
        contentDetail.setContentDetailId(contentDetailId);
        contentDetail.setCategoryId(50L);
        contentDetail.setName(name);
        contentDetail.setDisplayOrder(displayOrder);
        contentDetail.setMemo(memo);
        contentDetail.setTargetBpm(targetBpm);
        contentDetail.setEvaluationMemo("평가");
        contentDetail.setSheetUrl("https://example.com/sheet");
        contentDetail.setYoutubeUrl("https://youtu.be/scale");
        contentDetail.setAudioUrl("https://example.com/scale.mp3");
        contentDetail.setStatus(RecordStatus.ACTIVE);
        return contentDetail;
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
