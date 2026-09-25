package com.yunki.lessonpt.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * config 파일이 있는 로컬에서만 Private Bucket에 잠시 썼다가 지운다.
 * 일반 단위 테스트는 이 조건이 거짓이면 OCI에 접속하지 않는다.
 */
class OciObjectStorageSmokeTest {

    private static final String CONTENT = "LessonPT Object Storage smoke test";
    private static final String BUCKET = System.getenv().getOrDefault("LESSONPT_OCI_BUCKET_NAME", "lessonpt-resources");

    @Test
    @EnabledIf("configFileExists")
    void putGetDeleteRoundTrip() throws Exception {
        String configFile = System.getenv().getOrDefault("LESSONPT_OCI_CONFIG_FILE", "C:/Portfolio/Secret/config/config.txt");
        String profile = System.getenv().getOrDefault("LESSONPT_OCI_PROFILE", "DEFAULT");
        String objectName = "lessonpt-smoke/" + UUID.randomUUID() + ".txt";
        byte[] body = CONTENT.getBytes(StandardCharsets.UTF_8);

        try (ObjectStorage client = ObjectStorageClient.builder()
                .build(new ConfigFileAuthenticationDetailsProvider(configFile, profile))) {
            String namespace = namespace(client);
            assertThat(namespace).isNotBlank();
            assertThat(BUCKET).isEqualTo("lessonpt-resources");
            try {
                put(client, namespace, objectName, body);
                assertThat(get(client, namespace, objectName)).isEqualTo(CONTENT);
            } finally {
                delete(client, namespace, objectName);
            }
            assertThatThrownBy(() -> get(client, namespace, objectName))
                    .isInstanceOf(BmcException.class)
                    .extracting(thrown -> ((BmcException) thrown).getStatusCode())
                    .isEqualTo(404);
        }
    }

    static boolean configFileExists() {
        String configFile = System.getenv().getOrDefault("LESSONPT_OCI_CONFIG_FILE", "C:/Portfolio/Secret/config/config.txt");
        return Files.isRegularFile(Path.of(configFile));
    }

    private static String namespace(ObjectStorage client) {
        try {
            return client.getNamespace(GetNamespaceRequest.builder().build()).getValue();
        } catch (BmcException exception) {
            throw diagnosis("GetNamespace", exception);
        }
    }

    private static void put(ObjectStorage client, String namespace, String objectName, byte[] body) {
        try {
            client.putObject(PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(BUCKET)
                    .objectName(objectName)
                    .contentLength((long) body.length)
                    .contentType("text/plain")
                    .putObjectBody(new ByteArrayInputStream(body))
                    .build());
        } catch (BmcException exception) {
            throw diagnosis("PutObject", exception);
        }
    }

    private static String get(ObjectStorage client, String namespace, String objectName) {
        try {
            GetObjectResponse response = client.getObject(GetObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(BUCKET)
                    .objectName(objectName)
                    .build());
            try (var input = response.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (BmcException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("GetObject body could not be read.", exception);
        }
    }

    private static void delete(ObjectStorage client, String namespace, String objectName) {
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(BUCKET)
                    .objectName(objectName)
                    .build());
        } catch (BmcException exception) {
            throw diagnosis("DeleteObject", exception);
        }
    }

    private static AssertionError diagnosis(String operation, BmcException exception) {
        return new AssertionError(
                operation
                        + " failed status="
                        + exception.getStatusCode()
                        + " serviceCode="
                        + exception.getServiceCode()
                        + " opcRequestId="
                        + exception.getOpcRequestId(),
                exception);
    }
}
