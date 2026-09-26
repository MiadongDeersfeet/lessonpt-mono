package com.yunki.lessonpt.resource.storage;

import java.io.InputStream;

public record StoredObjectContent(InputStream body, long contentLength) implements AutoCloseable {

    @Override
    public void close() throws Exception {
        body.close();
    }
}
