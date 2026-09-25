package com.yunki.lessonpt.resource.storage;

import java.io.InputStream;

public interface ObjectStorageGateway {

    void put(String objectKey, String contentType, long contentLength, InputStream body);

    void delete(String objectKey);

    StoredObjectContent open(String objectKey, String rangeHeader);
}
