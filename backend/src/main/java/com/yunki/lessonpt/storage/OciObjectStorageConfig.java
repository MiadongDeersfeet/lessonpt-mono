package com.yunki.lessonpt.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;
import com.yunki.lessonpt.resource.storage.UnavailableObjectStorageGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * local에서만 OCI config 파일로 Object Storage 클라이언트를 연다.
 * region과 key_file은 config 파일이 가지고, 값은 여기에 적지 않는다.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class OciObjectStorageConfig {

    @Bean(destroyMethod = "close")
    @Profile("local")
    @ConditionalOnProperty(prefix = "lessonpt.storage", name = "provider", havingValue = "oci")
    ObjectStorage objectStorage(StorageProperties properties) throws IOException {
        StorageProperties.Oci oci = properties.getOci();
        Path configFile = Path.of(oci.getConfigFile());
        if (!Files.isRegularFile(configFile)) {
            throw new IllegalStateException("OCI config file was not found.");
        }
        ConfigFileAuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(
                configFile.toString(),
                oci.getProfile());
        return ObjectStorageClient.builder().build(provider);
    }

    @Bean
    @Profile("!local")
    ObjectStorageGateway objectStorageGateway() {
        return new UnavailableObjectStorageGateway();
    }
}
