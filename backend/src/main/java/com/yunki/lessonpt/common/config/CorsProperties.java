package com.yunki.lessonpt.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lessonpt.cors")
public class CorsProperties {

    private String allowedOrigins = "";

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> originList() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
