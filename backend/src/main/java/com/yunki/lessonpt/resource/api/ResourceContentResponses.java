package com.yunki.lessonpt.resource.api;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.yunki.lessonpt.resource.service.ContentResourceService.OpenedResource;

public final class ResourceContentResponses {

    private ResourceContentResponses() {
    }

    public static ResponseEntity<StreamingResponseBody> write(OpenedResource opened) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, opened.resource().getContentType());
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentLength(opened.length());
        headers.setContentDisposition(ContentDisposition.builder(opened.disposition())
                .filename(opened.resource().getOriginalFileName(), StandardCharsets.UTF_8)
                .build());
        if (opened.partial()) {
            headers.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + opened.start() + "-" + opened.end() + "/" + opened.resource().getFileSize());
        }
        StreamingResponseBody body = output -> {
            try (InputStream input = opened.content().body()) {
                input.transferTo(output);
            }
        };
        HttpStatus status = opened.partial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(body, headers, status);
    }
}
