package com.yunki.lessonpt.resource.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import com.oracle.bmc.model.Range;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.yunki.lessonpt.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OciObjectStorageGatewayRangeTest {

    @Mock
    private ObjectStorage objectStorage;

    private OciObjectStorageGateway gateway;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.getOci().setBucketName("lessonpt-resources");
        gateway = new OciObjectStorageGateway(objectStorage, properties);
        when(objectStorage.getNamespace(any())).thenReturn(
                GetNamespaceResponse.builder().__httpStatusCode__(200).value("ns").build());
    }

    @Test
    void openEndedRangeRequestsInclusiveLastByte() throws Exception {
        Range range = capture("bytes=0-73077");
        assertThat(range.getStartByte()).isEqualTo(0L);
        assertThat(range.getEndByte()).isEqualTo(73077L);
        assertThat(range.toString()).isEqualTo("bytes=0-73077");
    }

    @Test
    void fixedRangeRequestsInclusiveBounds() throws Exception {
        Range range = capture("bytes=10-20");
        assertThat(range.getStartByte()).isEqualTo(10L);
        assertThat(range.getEndByte()).isEqualTo(20L);
        assertThat(range.toString()).isEqualTo("bytes=10-20");
    }

    @Test
    void suffixRangeRequestsResolvedTail() throws Exception {
        Range range = capture("bytes=89-99");
        assertThat(range.getStartByte()).isEqualTo(89L);
        assertThat(range.getEndByte()).isEqualTo(99L);
        assertThat(range.toString()).isEqualTo("bytes=89-99");
    }

    @Test
    void absentRangeOmitsSdkRange() throws Exception {
        assertThat(capture(null)).isNull();
    }

    private Range capture(String rangeHeader) throws Exception {
        when(objectStorage.getObject(any())).thenReturn(GetObjectResponse.builder()
                .__httpStatusCode__(200)
                .contentLength(1L)
                .inputStream(new ByteArrayInputStream(new byte[] {1}))
                .build());
        try (StoredObjectContent content = gateway.open("object-key", rangeHeader)) {
            assertThat(content.contentLength()).isEqualTo(1L);
        }
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(objectStorage).getObject(captor.capture());
        return captor.getValue().getRange();
    }
}
