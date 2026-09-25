package com.yunki.lessonpt.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lessonpt.storage")
public class StorageProperties {

    private String provider = "oci";
    private final Oci oci = new Oci();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Oci getOci() {
        return oci;
    }

    public static class Oci {

        private String configFile = "C:/Portfolio/Secret/config/config.txt";
        private String profile = "DEFAULT";
        private String bucketName = "lessonpt-resources";

        public String getConfigFile() {
            return configFile;
        }

        public void setConfigFile(String configFile) {
            this.configFile = configFile;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }
}
