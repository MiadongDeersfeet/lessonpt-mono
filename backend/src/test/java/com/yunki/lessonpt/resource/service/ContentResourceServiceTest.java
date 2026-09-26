package com.yunki.lessonpt.resource.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.mock.web.MockMultipartFile;

import com.yunki.lessonpt.auth.security.StudentPrincipal;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.resource.domain.ContentResource;
import com.yunki.lessonpt.resource.domain.ResourceType;
import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;
import com.yunki.lessonpt.resource.storage.StoredObjectContent;

@ExtendWith(MockitoExtension.class)
class ContentResourceServiceTest {

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
    void uploadsPdfAndInsertsWhenNoneExists() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        when(contentResourceMapper.selectActiveFileSizeSum()).thenReturn(0L);
        when(contentResourceMapper.insertContentResource(any())).thenReturn(1);
        ContentResource stored = stored(9L, ResourceType.SHEET, 8L);
        when(contentResourceMapper.selectActiveByContentDetailIdAndType(70L, ResourceType.SHEET))
                .thenReturn(null, stored);

        var result = service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("notes.pdf", "%PDF-1.4"));

        assertThat(result.resource().resourceId()).isEqualTo(9L);
        assertThat(result.resource().originalFileName()).isEqualTo("notes.pdf");
        assertThat(result.storageWarning()).isFalse();
        verify(contentResourceMapper, never()).softDeleteActiveById(any(), any());
        verify(objectStorageGateway).put(any(), eq("application/pdf"), eq(8L), any());
    }

    @Test
    void rejectsPdfWithoutSignature() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        assertThatThrownBy(() -> service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("a.pdf", "hello")))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_INVALID_FILE);
        verify(objectStorageGateway, never()).put(any(), any(), any(Long.class), any());
    }

    @Test
    void rejectsPdfOver20Mb() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        byte[] body = new byte[(int) ResourceFileInspector.PDF_LIMIT + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, body, 0, 5);
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", body);
        assertThatThrownBy(() -> service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, file))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_FILE_TOO_LARGE);
    }

    @Test
    void uploadsMp3AndM4a() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        when(contentResourceMapper.selectActiveFileSizeSum()).thenReturn(0L);
        when(contentResourceMapper.insertContentResource(any())).thenReturn(1);
        when(contentResourceMapper.selectActiveByContentDetailIdAndType(70L, ResourceType.AUDIO))
                .thenReturn(null, stored(1L, ResourceType.AUDIO, 4L));
        service.save(3L, 40L, 50L, 70L, ResourceType.AUDIO, audio("a.mp3", "audio/mpeg", new byte[] {'I', 'D', '3', 0}));
        byte[] m4a = new byte[] {0, 0, 0, 24, 'f', 't', 'y', 'p'};
        when(contentResourceMapper.selectActiveByContentDetailIdAndType(70L, ResourceType.AUDIO))
                .thenReturn(null, stored(2L, ResourceType.AUDIO, 8L));
        service.save(3L, 40L, 50L, 70L, ResourceType.AUDIO, audio("a.m4a", "audio/mp4", m4a));
        verify(objectStorageGateway).put(any(), eq("audio/mpeg"), eq(4L), any());
        verify(objectStorageGateway).put(any(), eq("audio/mp4"), eq(8L), any());
    }

    @Test
    void rejectsAudioOver50Mb() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        org.springframework.web.multipart.MultipartFile file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(ResourceFileInspector.AUDIO_LIMIT + 1);
        assertThatThrownBy(() -> service.save(3L, 40L, 50L, 70L, ResourceType.AUDIO, file))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_FILE_TOO_LARGE);
    }

    @Test
    void rejectsInvalidMp3() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        assertThatThrownBy(() -> service.save(
                3L, 40L, 50L, 70L, ResourceType.AUDIO, audio("a.mp3", "audio/mpeg", new byte[] {1, 2, 3, 4})))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_INVALID_FILE);
    }

    @Test
    void replaceSoftDeletesBeforeInsertAndDeletesOldObjectAfterCommit() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        ContentResource previous = stored(4L, ResourceType.SHEET, 10L);
        previous.setObjectKey("old-key");
        when(contentResourceMapper.selectActiveFileSizeSum()).thenReturn(10L);
        when(contentResourceMapper.selectActiveByContentDetailIdAndType(70L, ResourceType.SHEET))
                .thenReturn(previous, stored(5L, ResourceType.SHEET, 8L));
        when(contentResourceMapper.softDeleteActiveById(4L, 70L)).thenReturn(1);
        when(contentResourceMapper.insertContentResource(any())).thenReturn(1);

        service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("notes.pdf", "%PDF-1.4"));

        InOrder order = inOrder(contentResourceMapper);
        order.verify(contentResourceMapper).softDeleteActiveById(4L, 70L);
        order.verify(contentResourceMapper).insertContentResource(any());
        verify(resourceCleanup).deleteAfterCommit(List.of("old-key"));
    }

    @Test
    void blocksWhenExpectedUsageExceedsSevenGb() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        when(contentResourceMapper.selectActiveByContentDetailIdAndType(70L, ResourceType.SHEET)).thenReturn(null);
        when(contentResourceMapper.selectActiveFileSizeSum()).thenReturn(ContentResourceService.ADMISSION_BYTES);
        assertThatThrownBy(() -> service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("a.pdf", "%PDF-1.4")))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_STORAGE_LIMIT_EXCEEDED);
        verify(objectStorageGateway, never()).put(any(), any(), any(Long.class), any());
    }

    @Test
    void allowsSmallerReplacementAboveSevenGb() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        ContentResource previous = stored(4L, ResourceType.SHEET, 100L);
        previous.setObjectKey("old-key");
        when(contentResourceMapper.selectActiveFileSizeSum()).thenReturn(ContentResourceService.ADMISSION_BYTES + 100);
        when(contentResourceMapper.selectActiveByContentDetailIdAndType(70L, ResourceType.SHEET))
                .thenReturn(previous, stored(5L, ResourceType.SHEET, 8L));
        when(contentResourceMapper.softDeleteActiveById(4L, 70L)).thenReturn(1);
        when(contentResourceMapper.insertContentResource(any())).thenReturn(1);
        var result = service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("a.pdf", "%PDF-1.4"));
        assertThat(result.storageWarning()).isTrue();
    }

    @Test
    void deletesNewObjectWhenInsertFails() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        when(contentResourceMapper.selectActiveFileSizeSum()).thenReturn(0L);
        when(contentResourceMapper.selectActiveByContentDetailIdAndType(70L, ResourceType.SHEET)).thenReturn(null);
        when(contentResourceMapper.insertContentResource(any())).thenReturn(0);
        assertThatThrownBy(() -> service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("a.pdf", "%PDF-1.4")))
                .isInstanceOf(BusinessException.class);
        verify(resourceCleanup).deleteNewObject(any());
    }

    @Test
    void putFailureDoesNotInsert() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenReturn(1L);
        when(contentResourceMapper.selectActiveFileSizeSum()).thenReturn(0L);
        when(contentResourceMapper.selectActiveByContentDetailIdAndType(70L, ResourceType.SHEET)).thenReturn(null);
        doThrow(new IllegalStateException("oci")).when(objectStorageGateway).put(any(), any(), any(Long.class), any());
        assertThatThrownBy(() -> service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("a.pdf", "%PDF-1.4")))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_INTERNAL_ERROR);
        verify(contentResourceMapper, never()).insertContentResource(any());
    }

    @Test
    void storageLockTimeoutIsResourceConflict() {
        stubOwned();
        when(contentResourceMapper.lockStorageAdmission()).thenThrow(new CannotAcquireLockException("busy"));
        assertThatThrownBy(() -> service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("a.pdf", "%PDF-1.4")))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_STORAGE_CONFLICT);
    }

    @Test
    void otherTeacherIsNotFound() {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 3L)).thenReturn(null);
        assertThatThrownBy(() -> service.save(3L, 40L, 50L, 70L, ResourceType.SHEET, pdf("a.pdf", "%PDF-1.4")))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void deleteSoftDeletesThenSchedulesObjectDelete() {
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumId(40L);
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 3L)).thenReturn(curriculum);
        Category category = new Category();
        category.setCategoryId(50L);
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(50L, 40L)).thenReturn(category);
        ContentDetail detail = new ContentDetail();
        detail.setContentDetailId(70L);
        detail.setCategoryId(50L);
        detail.setStatus(RecordStatus.ACTIVE);
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(70L, 50L)).thenReturn(detail);
        ContentResource resource = stored(9L, ResourceType.SHEET, 8L);
        resource.setObjectKey("gone");
        when(contentResourceMapper.selectActiveForTeacher(3L, 40L, 50L, 70L, 9L)).thenReturn(resource);
        when(contentResourceMapper.softDeleteActiveById(9L, 70L)).thenReturn(1);
        service.delete(3L, 40L, 50L, 70L, 9L);
        verify(resourceCleanup).deleteAfterCommit(List.of("gone"));
    }

    @Test
    void studentWithoutScopeIsRejected() {
        assertThatThrownBy(() -> service.openForStudent(new StudentPrincipal(null, 1L, 2L), 9L, null, "inline"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.STUDENT_SCOPE_REQUIRED);
    }

    @Test
    void studentOutsideScopeIsNotFound() {
        when(contentResourceMapper.selectActiveForStudent(2L, 8L, 9L)).thenReturn(null);
        assertThatThrownBy(() -> service.openForStudent(new StudentPrincipal(8L, 1L, 2L), 9L, null, "inline"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void studentReadUsesFullObjectWhenRangeIsAbsent() {
        ContentResource resource = stored(9L, ResourceType.AUDIO, 20L);
        resource.setObjectKey("audio-key");
        resource.setContentType("audio/mpeg");
        resource.setOriginalFileName("a.mp3");
        when(contentResourceMapper.selectActiveForStudent(2L, 8L, 9L)).thenReturn(resource);
        when(objectStorageGateway.open("audio-key", null))
                .thenReturn(new StoredObjectContent(new ByteArrayInputStream(new byte[20]), 20));
        var opened = service.openForStudent(new StudentPrincipal(8L, 1L, 2L), 9L, null, "inline");
        assertThat(opened.partial()).isFalse();
        assertThat(opened.length()).isEqualTo(20L);
    }

    @Test
    void studentReadRequestsPartialRange() {
        ContentResource resource = stored(9L, ResourceType.AUDIO, 20L);
        resource.setObjectKey("audio-key");
        resource.setFileSize(20L);
        when(contentResourceMapper.selectActiveForStudent(2L, 8L, 9L)).thenReturn(resource);
        when(objectStorageGateway.open("audio-key", "bytes=0-9"))
                .thenReturn(new StoredObjectContent(new ByteArrayInputStream(new byte[10]), 10));
        var opened = service.openForStudent(new StudentPrincipal(8L, 1L, 2L), 9L, "bytes=0-9", "inline");
        assertThat(opened.partial()).isTrue();
        assertThat(opened.length()).isEqualTo(10L);
    }

    @Test
    void unsatisfiableRangeIs416() {
        ContentResource resource = stored(9L, ResourceType.AUDIO, 100L);
        when(contentResourceMapper.selectActiveForStudent(2L, 8L, 9L)).thenReturn(resource);
        assertThatThrownBy(() -> service.openForStudent(new StudentPrincipal(8L, 1L, 2L), 9L, "bytes=100-200", "inline"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE);
        verify(objectStorageGateway, never()).open(any(), any());
    }

    private void stubOwned() {
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumId(40L);
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 3L)).thenReturn(curriculum);
        Category category = new Category();
        category.setCategoryId(50L);
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(50L, 40L)).thenReturn(category);
        ContentDetail detail = new ContentDetail();
        detail.setContentDetailId(70L);
        detail.setCategoryId(50L);
        detail.setStatus(RecordStatus.ACTIVE);
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(70L, 50L)).thenReturn(detail);
        when(contentDetailMapper.lockContentDetailById(70L)).thenReturn(detail);
    }

    private ContentResource stored(Long id, ResourceType type, long size) {
        ContentResource resource = new ContentResource();
        resource.setResourceId(id);
        resource.setContentDetailId(70L);
        resource.setResourceType(type);
        resource.setOriginalFileName(type == ResourceType.SHEET ? "notes.pdf" : "a.mp3");
        resource.setContentType(type == ResourceType.SHEET ? "application/pdf" : "audio/mpeg");
        resource.setFileSize(size);
        resource.setObjectKey("key-" + id);
        resource.setStatus(RecordStatus.ACTIVE);
        return resource;
    }

    private MockMultipartFile pdf(String name, String body) {
        return new MockMultipartFile("file", name, "application/pdf", body.getBytes(StandardCharsets.US_ASCII));
    }

    private MockMultipartFile audio(String name, String type, byte[] body) {
        return new MockMultipartFile("file", name, type, body);
    }
}
