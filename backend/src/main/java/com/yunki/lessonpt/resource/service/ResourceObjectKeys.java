package com.yunki.lessonpt.resource.service;

import java.util.UUID;

import com.yunki.lessonpt.resource.domain.ResourceType;

public final class ResourceObjectKeys {

    private ResourceObjectKeys() {
    }

    public static String create(Long teacherId, Long contentDetailId, ResourceType type, String extension) {
        String folder = type == ResourceType.SHEET ? "sheet" : "audio";
        return "teachers/" + teacherId
                + "/contents/" + contentDetailId
                + "/" + folder
                + "/" + UUID.randomUUID()
                + "." + extension;
    }
}
