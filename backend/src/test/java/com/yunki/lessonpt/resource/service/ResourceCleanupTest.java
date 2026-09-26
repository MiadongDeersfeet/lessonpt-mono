package com.yunki.lessonpt.resource.service;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;

@ExtendWith(MockitoExtension.class)
class ResourceCleanupTest {

    @Mock
    private ContentResourceMapper contentResourceMapper;
    @Mock
    private ObjectStorageGateway objectStorageGateway;

    @Test
    void categoryAndCurriculumCleanupSoftDeleteMatchingRows() {
        ResourceCleanup cleanup = new ResourceCleanup(contentResourceMapper, objectStorageGateway);
        org.mockito.Mockito.when(contentResourceMapper.selectActiveObjectKeysByCategoryId(5L)).thenReturn(List.of());
        org.mockito.Mockito.when(contentResourceMapper.selectActiveObjectKeysByCurriculumId(6L)).thenReturn(List.of());
        org.mockito.Mockito.when(contentResourceMapper.selectActiveObjectKeysByContentDetailId(7L)).thenReturn(List.of());
        cleanup.discardCategory(5L);
        cleanup.discardCurriculum(6L);
        cleanup.discardContentDetail(7L);
        verify(contentResourceMapper).softDeleteActiveByCategoryId(5L);
        verify(contentResourceMapper).softDeleteActiveByCurriculumId(6L);
        verify(contentResourceMapper).softDeleteActiveByContentDetailId(7L);
    }

    @Test
    void failedObjectDeleteIsSwallowed() {
        ResourceCleanup cleanup = new ResourceCleanup(contentResourceMapper, objectStorageGateway);
        org.mockito.Mockito.doThrow(new IllegalStateException("oci")).when(objectStorageGateway).delete("orphan");
        cleanup.deleteNewObject("orphan");
    }
}
