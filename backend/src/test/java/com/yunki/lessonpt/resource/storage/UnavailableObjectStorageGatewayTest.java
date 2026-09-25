package com.yunki.lessonpt.resource.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

class UnavailableObjectStorageGatewayTest {

    private final ObjectStorageGateway gateway = new UnavailableObjectStorageGateway();

    @Test
    void putDeleteAndOpenFailWhenStorageIsNotConfigured() {
        assertThatThrownBy(() -> gateway.put(
                "teachers/1/contents/2/sheet/a.pdf",
                "application/pdf",
                1,
                new ByteArrayInputStream(new byte[] {'%'})))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Object storage is not configured.");
        assertThatThrownBy(() -> gateway.delete("teachers/1/contents/2/sheet/a.pdf"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Object storage is not configured.");
        assertThatThrownBy(() -> gateway.open("teachers/1/contents/2/audio/a.mp3", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Object storage is not configured.");
    }
}
