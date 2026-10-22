package com.yunki.lessonpt.storage;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * OCI config 파일 존재만 확인한다. 경로 문자열을 로그에 남기지 않는다.
 */
public final class OciConfigFiles {

    private OciConfigFiles() {
    }

    public static boolean isReadable(String configFile) {
        if (configFile == null || configFile.isBlank()) {
            return false;
        }
        Path path = Path.of(configFile);
        return Files.isRegularFile(path) && Files.isReadable(path);
    }
}
