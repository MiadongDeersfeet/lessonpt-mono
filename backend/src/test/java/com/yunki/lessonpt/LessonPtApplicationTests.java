package com.yunki.lessonpt;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 접속 정보 없이 Spring 이 뜨는지, health로 Oracle 미연결을 구분하는지만 본다.
 * local 프로파일을 켜지 않아서 개발 Schema에는 붙지 않는다.
 */
@SpringBootTest(properties = "spring.profiles.active=context")
@AutoConfigureMockMvc
class LessonPtApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthShowsOracleDownWhenDatasourceIsMissing() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("DOWN")))
                .andExpect(content().string(containsString("oracle")))
                .andExpect(content().string(containsString("Oracle 접속 정보가 없다")));
    }

    @Test
    void smokeMapperXmlMatchesInterface() throws Exception {
        try (var input = getClass().getResourceAsStream("/mapper/common/SmokeMapper.xml")) {
            assertThat(input).isNotNull();
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(xml)
                    .contains("namespace=\"com.yunki.lessonpt.common.health.SmokeMapper\"")
                    .contains("SELECT 1 FROM DUAL");
        }
    }
}
