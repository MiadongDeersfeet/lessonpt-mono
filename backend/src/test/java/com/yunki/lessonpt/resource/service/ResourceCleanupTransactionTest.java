package com.yunki.lessonpt.resource.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;

@ExtendWith(MockitoExtension.class)
class ResourceCleanupTransactionTest {

    @Mock
    private ContentResourceMapper contentResourceMapper;
    @Mock
    private ObjectStorageGateway objectStorageGateway;
    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;

    private ResourceCleanup cleanup;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        cleanup = new ResourceCleanup(contentResourceMapper, objectStorageGateway);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void rollbackDoesNotDeleteObjects() {
        stubKeys();
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            cleanup.discardContentDetail(7L);
            cleanup.discardCategory(5L);
            cleanup.discardCurriculum(6L);
            throw new IllegalStateException("forced");
        })).hasMessage("forced");
        verify(objectStorageGateway, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitDeletesObjectsOnlyAfterCommit() {
        stubKeys();
        transaction.executeWithoutResult(status -> {
            cleanup.discardContentDetail(7L);
            cleanup.discardCategory(5L);
            cleanup.discardCurriculum(6L);
            verify(objectStorageGateway, never()).delete(org.mockito.ArgumentMatchers.any());
        });

        verify(objectStorageGateway).delete("detail-key");
        verify(objectStorageGateway).delete("category-key");
        verify(objectStorageGateway).delete("curriculum-key");
    }

    @Test
    void failedCommitDoesNotDeleteObjects() throws Exception {
        stubKeys();
        org.mockito.Mockito.doThrow(new SQLException("commit failed")).when(connection).commit();
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            cleanup.discardContentDetail(7L);
            cleanup.discardCategory(5L);
            cleanup.discardCurriculum(6L);
        })).isInstanceOf(RuntimeException.class);
        verify(objectStorageGateway, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private void stubKeys() {
        when(contentResourceMapper.selectActiveObjectKeysByContentDetailId(7L)).thenReturn(List.of("detail-key"));
        when(contentResourceMapper.selectActiveObjectKeysByCategoryId(5L)).thenReturn(List.of("category-key"));
        when(contentResourceMapper.selectActiveObjectKeysByCurriculumId(6L)).thenReturn(List.of("curriculum-key"));
    }
}
