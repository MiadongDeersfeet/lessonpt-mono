package com.yunki.lessonpt.curriculum.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PATCH에서 name을 빼면 setter가 호출되지 않아 기존 이름을 유지한다.
 * null이나 빈 문자열은 setter가 호출되므로 검증 오류다.
 */
@ValidCategoryUpdate
public class CategoryUpdateRequest {

    private String name;
    private boolean nameSpecified;

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
}
