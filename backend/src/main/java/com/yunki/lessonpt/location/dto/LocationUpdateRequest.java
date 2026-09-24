package com.yunki.lessonpt.location.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PATCH에서 필드를 빼면 setter가 호출되지 않는다.
 * null을 보내면 setter가 호출되므로, 주소 삭제와 미변경을 구분할 수 있다.
 */
@ValidLocationUpdate
public class LocationUpdateRequest {

    private String name;
    private boolean nameSpecified;
    private String address;
    private boolean addressSpecified;

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
    public boolean isAddressSpecified() {
        return addressSpecified;
    }

    public String getAddress() {
        return address;
    }

    @JsonProperty("address")
    public void setAddress(String address) {
        this.addressSpecified = true;
        this.address = address;
    }
}
