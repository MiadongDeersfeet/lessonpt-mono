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
            builder.range(toSdkRange(rangeHeader));
        }
        GetObjectResponse response = objectStorage.getObject(builder.build());
        long length = response.getContentLength() == null ? -1L : response.getContentLength();
        return new StoredObjectContent(response.getInputStream(), length);
    }

    /**
     * ByteRanges already resolved the browser header to {@code bytes=start-end}.
     * OCI SDK 3.66 {@link Range#parse(String)} requires {@code bytes=start-end/length}
     * and rejects that request header. {@link Range#Range(Long, Long)} sets the
     * inclusive bounds the GetObject client sends.
     */
    private static Range toSdkRange(String httpRange) {
        String spec = httpRange.trim();
        if (spec.regionMatches(true, 0, "bytes=", 0, "bytes=".length())) {
            spec = spec.substring("bytes=".length()).trim();
        }
        int dash = spec.indexOf('-');
        if (dash <= 0 || dash == spec.length() - 1 || spec.indexOf('-', dash + 1) >= 0) {
            throw new IllegalArgumentException("Resolved object range must be bytes=<start>-<end>");
        }
        long start = Long.parseLong(spec.substring(0, dash));
        long end = Long.parseLong(spec.substring(dash + 1));
        return new Range(start, end);
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
