package com.yunki.lessonpt.resource.service;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;

public final class YoutubeUrls {

    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Set<String> HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be");

    private YoutubeUrls() {
    }

    public static void requireValid(String url) {
        if (url == null) {
            return;
        }
        if (url.isBlank() || !hasVideoId(url)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT, "영상 URL이 올바르지 않습니다.");
        }
    }

    public static boolean hasVideoId(String url) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!HOSTS.contains(host)) {
            return false;
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        if ("youtu.be".equals(host)) {
            String id = path.startsWith("/") ? path.substring(1) : path;
            int slash = id.indexOf('/');
            if (slash >= 0) {
                id = id.substring(0, slash);
            }
            return VIDEO_ID.matcher(id).matches();
        }
        if (path.equals("/watch")) {
            return queryVideoId(uri.getRawQuery());
        }
        if (path.startsWith("/embed/") || path.startsWith("/shorts/")) {
            String id = path.substring(path.indexOf('/', 1) + 1);
            int slash = id.indexOf('/');
            if (slash >= 0) {
                id = id.substring(0, slash);
            }
            return VIDEO_ID.matcher(id).matches();
        }
        return false;
    }

    private static boolean queryVideoId(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            if ("v".equals(pair.substring(0, eq))) {
                return VIDEO_ID.matcher(pair.substring(eq + 1)).matches();
            }
        }
        return false;
    }
}
