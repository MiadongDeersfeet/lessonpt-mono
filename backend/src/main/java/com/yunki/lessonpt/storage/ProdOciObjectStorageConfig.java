package com.yunki.lessonpt.storage;

import java.io.IOException;
import java.nio.file.Path;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;
import com.yunki.lessonpt.resource.storage.OciObjectStorageGateway;
import com.yunki.lessonpt.resource.storage.UnavailableObjectStorageGateway;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * prod는 Instance Principal을 쓰지 않는다. local과 같은 OCI config 파일 인증만 연다.
 * 파일이 없으면 기존처럼 저장소를 닫아 두고, 기동은 유지한다.
 */
@Configuration
@Profile("prod")
@EnableConfigurationProperties(StorageProperties.class)
public class ProdOciObjectStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdOciObjectStorageConfig.class);

    private ObjectStorage client;

    @Bean
    ObjectStorageGateway prodObjectStorageGateway(StorageProperties properties) throws IOException {
        StorageProperties.Oci oci = properties.getOci();
        if (!"oci".equalsIgnoreCase(properties.getProvider()) || !OciConfigFiles.isReadable(oci.getConfigFile())) {
            log.warn("Object storage is not configured.");
            return new UnavailableObjectStorageGateway();
        }
        ConfigFileAuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(
                Path.of(oci.getConfigFile()).toString(),
                oci.getProfile());
        client = ObjectStorageClient.builder().build(provider);
        return new OciObjectStorageGateway(client, properties);
    }

    @PreDestroy
    void closeClient() {
        if (client == null) {
            return;
        }
        try {
            client.close();
        } catch (Exception exception) {
            log.warn("Object storage client close failed");
        }
    }
}
