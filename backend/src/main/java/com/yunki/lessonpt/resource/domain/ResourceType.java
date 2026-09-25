package com.yunki.lessonpt.resource.domain;

public enum ResourceType {
    SHEET,
    AUDIO;

    public static ResourceType fromPath(String pathValue) {
        if (pathValue == null) {
            return null;
        }
        return switch (pathValue) {
            case "sheet" -> SHEET;
            case "audio" -> AUDIO;
            default -> null;
        };
    }
}
