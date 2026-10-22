package com.yunki.lessonpt.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void splitsCommaSeparatedOrigins() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(" https://app.example ,https://preview.example ");
        assertThat(properties.originList()).containsExactly("https://app.example", "https://preview.example");
    }

    @Test
    void keepsOriginsEmptyByDefault() {
        assertThat(new CorsProperties().originList()).isEmpty();
    }
}
