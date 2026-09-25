package com.yunki.lessonpt.resource.storage;

import java.io.InputStream;

import com.oracle.bmc.model.Range;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.yunki.lessonpt.storage.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "lessonpt.storage", name = "provider", havingValue = "oci")
public class OciObjectStorageGateway implements ObjectStorageGateway {

    private final ObjectStorage objectStorage;
    private final String bucketName;
    private volatile String namespace;

    public OciObjectStorageGateway(ObjectStorage objectStorage, StorageProperties properties) {
        this.objectStorage = objectStorage;
        this.bucketName = properties.getOci().getBucketName();
    }

    @Override
    public void put(String objectKey, String contentType, long contentLength, InputStream body) {
        objectStorage.putObject(PutObjectRequest.builder()
                .namespaceName(namespace())
                .bucketName(bucketName)
                .objectName(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .putObjectBody(body)
                .build());
    }

    @Override
    public void delete(String objectKey) {
        objectStorage.deleteObject(DeleteObjectRequest.builder()
                .namespaceName(namespace())
                .bucketName(bucketName)
                .objectName(objectKey)
                .build());
    }

    @Override
    public StoredObjectContent open(String objectKey, String rangeHeader) {
        GetObjectRequest.Builder builder = GetObjectRequest.builder()
                .namespaceName(namespace())
                .bucketName(bucketName)
                .objectName(objectKey);
        if (rangeHeader != null && !rangeHeader.isBlank()) {
            builder.range(Range.parse(rangeHeader));
        }
        GetObjectResponse response = objectStorage.getObject(builder.build());
        long length = response.getContentLength() == null ? -1L : response.getContentLength();
        return new StoredObjectContent(response.getInputStream(), length);
    }

    private String namespace() {
        String current = namespace;
        if (current == null) {
            current = objectStorage.getNamespace(GetNamespaceRequest.builder().build()).getValue();
            namespace = current;
        }
        return current;
    }
}
