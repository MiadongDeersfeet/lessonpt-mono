package com.yunki.lessonpt.resource.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yunki.lessonpt.resource.domain.ContentResource;
import com.yunki.lessonpt.resource.domain.ResourceType;

public record ResourcePair(ContentResourceResponse sheet, ContentResourceResponse audio) {

    public static Map<Long, ResourcePair> byContentDetail(List<ContentResource> resources) {
        Map<Long, ContentResourceResponse> sheets = new HashMap<>();
        Map<Long, ContentResourceResponse> audios = new HashMap<>();
        if (resources != null) {
            for (ContentResource resource : resources) {
                ContentResourceResponse response = ContentResourceResponse.from(resource);
                if (resource.getResourceType() == ResourceType.SHEET) {
                    sheets.putIfAbsent(resource.getContentDetailId(), response);
                } else if (resource.getResourceType() == ResourceType.AUDIO) {
                    audios.putIfAbsent(resource.getContentDetailId(), response);
                }
            }
        }
        Map<Long, ResourcePair> pairs = new HashMap<>();
        for (Long contentDetailId : sheets.keySet()) {
            pairs.put(contentDetailId, new ResourcePair(sheets.get(contentDetailId), audios.get(contentDetailId)));
        }
        for (Long contentDetailId : audios.keySet()) {
            pairs.putIfAbsent(contentDetailId, new ResourcePair(sheets.get(contentDetailId), audios.get(contentDetailId)));
        }
        return pairs;
    }

    public static ResourcePair of(Map<Long, ResourcePair> pairs, Long contentDetailId) {
        if (pairs == null || contentDetailId == null) {
            return new ResourcePair(null, null);
        }
        return pairs.getOrDefault(contentDetailId, new ResourcePair(null, null));
    }
}
