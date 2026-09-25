package com.yunki.lessonpt.relationship.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lessonpt.student-session")
public class StudentSessionProperties {

    public static final String COOKIE_NAME = "LESSONPT_STUDENT_SESSION";
    public static final String COOKIE_PATH = "/api/v1/student";

    private boolean secure = true;
    private String sameSite = "";

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }
}
