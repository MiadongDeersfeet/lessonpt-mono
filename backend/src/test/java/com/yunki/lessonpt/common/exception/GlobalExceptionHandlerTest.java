package com.yunki.lessonpt.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공통 오류 JSON과 traceId를 MockMvc로 확인한다.
 * Oracle 접속 정보는 쓰지 않는다.
 */
@SpringBootTest(properties = "spring.profiles.active=context")
@AutoConfigureMockMvc
@Import(ErrorHandlingTestController.class)
class GlobalExceptionHandlerTest {

    private static final String SECRET_IN_EXCEPTION = "DB password is abc...";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationErrorUsesCommonInvalidInput() throws Exception {
        mockMvc.perform(post("/api/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("입력값을 확인해주세요."))
                .andExpect(jsonPath("$.path").value("/api/test/errors/validation"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')].message")
                        .value(hasItem("이메일 형식이 올바르지 않습니다.")))
                .andExpect(header().exists(TraceIdFilter.HEADER));
    }

    @Test
    void validationCollectsEveryFieldError() throws Exception {
        mockMvc.perform(post("/api/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("입력값을 확인해주세요."))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')].message")
                        .value(hasItem("이메일 형식이 올바르지 않습니다.")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')].message")
                        .value(hasItem("이름은 필수입니다.")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void constraintViolationCollectsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/test/errors/constraints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"createStudent.email\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해주세요."))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')].message")
                        .value(hasItem("이름은 필수입니다.")));
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 본문을 해석할 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/test/errors/validation"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void businessNotFoundIs404() throws Exception {
        mockMvc.perform(get("/api/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("요청한 대상을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/test/errors/not-found"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", empty()))
                .andExpect(header().exists(TraceIdFilter.HEADER));
    }

    @Test
    void businessConflictIs409() throws Exception {
        mockMvc.perform(get("/api/test/errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMON_CONFLICT"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("같은 순서가 이미 있습니다."))
                .andExpect(jsonPath("$.path").value("/api/test/errors/conflict"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void unsupportedMethodIs405() throws Exception {
        mockMvc.perform(post("/api/test/errors/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("COMMON_METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value("허용되지 않은 요청 방식입니다."))
                .andExpect(jsonPath("$.path").value("/api/test/errors/not-found"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void unexpectedExceptionHidesInternalMessage() throws Exception {
        mockMvc.perform(get("/api/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.path").value("/api/test/errors/unexpected"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", empty()))
                .andExpect(header().exists(TraceIdFilter.HEADER))
                .andExpect(content().string(not(containsString(SECRET_IN_EXCEPTION))));
    }

    @Test
    void traceIdHeaderMatchesBody() throws Exception {
        String headerName = TraceIdFilter.HEADER;
        mockMvc.perform(get("/api/test/errors/not-found"))
                .andExpect(header().exists(headerName))
                .andExpect(result -> {
                    String header = result.getResponse().getHeader(headerName);
                    String body = result.getResponse().getContentAsString();
                    org.assertj.core.api.Assertions.assertThat(body).contains(header);
                });
    }
}
