package com.yunki.lessonpt.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OciConfigFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsMissingAndBlankPaths() {
        assertThat(OciConfigFiles.isReadable(null)).isFalse();
        assertThat(OciConfigFiles.isReadable(" ")).isFalse();
        assertThat(OciConfigFiles.isReadable(tempDir.resolve("missing").toString())).isFalse();
    }

    @Test
    void acceptsAReadableFile() throws Exception {
        Path config = tempDir.resolve("config");
        Files.writeString(config, "[DEFAULT]\n");
        assertThat(OciConfigFiles.isReadable(config.toString())).isTrue();
    }
}
