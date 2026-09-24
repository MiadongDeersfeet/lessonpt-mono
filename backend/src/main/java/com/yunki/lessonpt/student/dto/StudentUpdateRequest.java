package com.yunki.lessonpt.student.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PATCH에서 필드를 빼면 setter가 호출되지 않는다.
 * null을 보내면 setter가 호출되므로, 이메일·전화번호 삭제와 미변경을 구분한다.
 */
@ValidStudentUpdate
public class StudentUpdateRequest {

    private String email;
    private boolean emailSpecified;
    private String name;
    private boolean nameSpecified;
    private String phone;
    private boolean phoneSpecified;

    @JsonIgnore
    public boolean isEmailSpecified() {
        return emailSpecified;
    }

    public String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.emailSpecified = true;
        this.email = email;
    }

    @JsonIgnore
    public boolean isNameSpecified() {
        return nameSpecified;
    }

    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.nameSpecified = true;
        this.name = name;
    }

    @JsonIgnore
    public boolean isPhoneSpecified() {
        return phoneSpecified;
    }

    public String getPhone() {
        return phone;
    }

    @JsonProperty("phone")
    public void setPhone(String phone) {
        this.phoneSpecified = true;
        this.phone = phone;
    }
}
