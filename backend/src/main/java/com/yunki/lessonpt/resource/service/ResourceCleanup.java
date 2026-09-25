package com.yunki.lessonpt.resource.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResourceCleanup {

    private static final Logger log = LoggerFactory.getLogger(ResourceCleanup.class);

    private final ContentResourceMapper contentResourceMapper;
    private final ObjectStorageGateway objectStorageGateway;

    public void discardContentDetail(Long contentDetailId) {
        List<String> keys = contentResourceMapper.selectActiveObjectKeysByContentDetailId(contentDetailId);
        contentResourceMapper.softDeleteActiveByContentDetailId(contentDetailId);
        deleteAfterCommit(keys);
    }

    public void discardCategory(Long categoryId) {
        List<String> keys = contentResourceMapper.selectActiveObjectKeysByCategoryId(categoryId);
        contentResourceMapper.softDeleteActiveByCategoryId(categoryId);
        deleteAfterCommit(keys);
    }

    public void discardCurriculum(Long curriculumId) {
        List<String> keys = contentResourceMapper.selectActiveObjectKeysByCurriculumId(curriculumId);
        contentResourceMapper.softDeleteActiveByCurriculumId(curriculumId);
        deleteAfterCommit(keys);
    }

    public void deleteAfterCommit(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }
        List<String> keys = List.copyOf(objectKeys);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String objectKey : keys) {
                    deleteQuietly(objectKey);
                }
            }
        });
    }

    public void deleteNewObject(String objectKey) {
        deleteQuietly(objectKey);
    }

    private void deleteQuietly(String objectKey) {
        try {
            objectStorageGateway.delete(objectKey);
        } catch (RuntimeException exception) {
            log.error("OCI object delete failed. orphan objectKey={}", objectKey, exception);
        }
    }
}
