package com.yunki.lessonpt.resource.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.resource.domain.ContentResource;
import com.yunki.lessonpt.resource.domain.ResourceType;
import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.resource.service.ContentResourceService;
import com.yunki.lessonpt.resource.service.ContentResourceService.OpenedResource;
import com.yunki.lessonpt.resource.service.ResourceCleanup;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;
import com.yunki.lessonpt.resource.storage.StoredObjectContent;

@ExtendWith(MockitoExtension.class)
class StudentAudioRangeResponseTest {

    private static final long SIZE = 100L;

    @Mock
    private ContentResourceMapper contentResourceMapper;
    @Mock
    private ContentDetailMapper contentDetailMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private CurriculumMapper curriculumMapper;
    @Mock
    private ObjectStorageGateway objectStorageGateway;
    @Mock
    private ResourceCleanup resourceCleanup;

    private ContentResourceService service;

    @BeforeEach
    void setUp() {
        service = new ContentResourceService(
                contentResourceMapper,
                contentDetailMapper,
                categoryMapper,
                curriculumMapper,
                objectStorageGateway,
                resourceCleanup);
    }

    @Test
    void absentRangeReturns200AndStreamsTheObject() throws Exception {
        stubAudio();
        byte[] object = objectBytes();
        when(objectStorageGateway.open("audio-key", null))
                .thenReturn(new StoredObjectContent(new ByteArrayInputStream(object), SIZE));

        ResponseEntity<StreamingResponseBody> response = write(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(SIZE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isNull();
        assertThat(response.getBody()).isInstanceOf(StreamingResponseBody.class);
        assertThat(bytes(response)).isEqualTo(object);
    }

    @Test
    void openEndedRangeReturns206ForTheWholeObject() throws Exception {
        stubAudio();
        byte[] object = objectBytes();
        when(objectStorageGateway.open("audio-key", "bytes=0-99"))
                .thenReturn(new StoredObjectContent(new ByteArrayInputStream(object), SIZE));

        ResponseEntity<StreamingResponseBody> response = write("bytes=0-");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(SIZE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 0-99/100");
        assertThat(bytes(response)).isEqualTo(object);
    }

    @Test
    void fixedRangeReturnsExactlyElevenBytes() throws Exception {
        stubAudio();
        byte[] slice = Arrays.copyOfRange(objectBytes(), 10, 21);
        when(objectStorageGateway.open("audio-key", "bytes=10-20"))
                .thenReturn(new StoredObjectContent(new ByteArrayInputStream(slice), slice.length));

        ResponseEntity<StreamingResponseBody> response = write("bytes=10-20");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(11L);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 10-20/100");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(bytes(response)).hasSize(11).isEqualTo(slice);
    }

    @Test
    void suffixRangeIsSatisfied() throws Exception {
        stubAudio();
        byte[] slice = Arrays.copyOfRange(objectBytes(), 89, 100);
        when(objectStorageGateway.open("audio-key", "bytes=89-99"))
                .thenReturn(new StoredObjectContent(new ByteArrayInputStream(slice), slice.length));

        ResponseEntity<StreamingResponseBody> response = write("bytes=-11");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(11L);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 89-99/100");
        assertThat(bytes(response)).isEqualTo(slice);
    }

    @Test
    void rangePastObjectSizeIs416() {
        stubAudio();
        assertThatThrownBy(() -> write("bytes=100-200"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE);
        assertThat(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE.status()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    private ResponseEntity<StreamingResponseBody> write(String range) {
        OpenedResource opened = service.openForStudent(new StudentPrincipal(8L, 1L, 2L), 9L, range, "inline");
        return ResourceContentResponses.write(opened);
    }

    private void stubAudio() {
        ContentResource resource = new ContentResource();
        resource.setResourceId(9L);
        resource.setContentDetailId(70L);
        resource.setResourceType(ResourceType.AUDIO);
        resource.setOriginalFileName("lesson.mp3");
        resource.setObjectKey("audio-key");
        resource.setContentType("audio/mpeg");
        resource.setFileSize(SIZE);
        resource.setStatus(RecordStatus.ACTIVE);
        when(contentResourceMapper.selectActiveForStudent(2L, 8L, 9L)).thenReturn(resource);
    }

    private static byte[] objectBytes() {
        byte[] object = new byte[(int) SIZE];
        for (int i = 0; i < object.length; i++) {
            object[i] = (byte) i;
        }
        return object;
    }

    private static byte[] bytes(ResponseEntity<StreamingResponseBody> response) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);
        return output.toByteArray();
    }
}
