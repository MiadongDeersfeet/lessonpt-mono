package com.yunki.lessonpt.resource.storage;

import java.io.InputStream;

public class UnavailableObjectStorageGateway implements ObjectStorageGateway {

    @Override
    public void put(String objectKey, String contentType, long contentLength, InputStream body) {
        throw new IllegalStateException("Object storage is not configured.");
    }

    @Override
    public void delete(String objectKey) {
        throw new IllegalStateException("Object storage is not configured.");
    }

    @Override
    public StoredObjectContent open(String objectKey, String rangeHeader) {
        throw new IllegalStateException("Object storage is not configured.");
    }
}
