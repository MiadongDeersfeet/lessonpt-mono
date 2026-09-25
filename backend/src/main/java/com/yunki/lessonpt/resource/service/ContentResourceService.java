package com.yunki.lessonpt.resource.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import com.yunki.lessonpt.resource.dto.ContentResourceResponse;
import com.yunki.lessonpt.resource.mapper.ContentResourceMapper;
import com.yunki.lessonpt.resource.service.ResourceFileInspector.InspectedFile;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;
import com.yunki.lessonpt.resource.storage.StoredObjectContent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentResourceService {

    public static final long WARNING_BYTES = 6L * 1024 * 1024 * 1024;
    public static final long ADMISSION_BYTES = 7L * 1024 * 1024 * 1024;

    private final ContentResourceMapper contentResourceMapper;
    private final ContentDetailMapper contentDetailMapper;
    private final CategoryMapper categoryMapper;
    private final CurriculumMapper curriculumMapper;
    private final ObjectStorageGateway objectStorageGateway;
    private final ResourceCleanup resourceCleanup;

    @Transactional
    public UploadResult save(
            Long teacherId,
            Long curriculumId,
            Long categoryId,
            Long contentDetailId,
            ResourceType resourceType,
            MultipartFile file) {
        if (resourceType == null) {
            throw new BusinessException(ErrorCode.RESOURCE_INVALID_TYPE);
        }
        requireOwned(teacherId, curriculumId, categoryId, contentDetailId);
        lockContentDetail(contentDetailId, categoryId);
        lockAdmission();
        InspectedFile inspected = ResourceFileInspector.inspect(file, resourceType);
        ContentResource previous = contentResourceMapper.selectActiveByContentDetailIdAndType(
                contentDetailId, resourceType);
        long previousSize = previous == null || previous.getFileSize() == null ? 0L : previous.getFileSize();
        long used = contentResourceMapper.selectActiveFileSizeSum();
        long expected = used - previousSize + inspected.size();
        if (inspected.size() > previousSize && expected > ADMISSION_BYTES) {
            throw new BusinessException(ErrorCode.RESOURCE_STORAGE_LIMIT_EXCEEDED);
        }
        String objectKey = ResourceObjectKeys.create(teacherId, contentDetailId, resourceType, inspected.objectExtension());
        try (InputStream body = inspected.openStream()) {
            objectStorageGateway.put(objectKey, inspected.contentType(), inspected.size(), body);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_INVALID_FILE);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }
        try {
            if (previous != null) {
                expectOne(contentResourceMapper.softDeleteActiveById(previous.getResourceId(), contentDetailId));
            }
            ContentResource created = new ContentResource();
            created.setContentDetailId(contentDetailId);
            created.setResourceType(resourceType);
            created.setOriginalFileName(inspected.fileName());
            created.setObjectKey(objectKey);
            created.setContentType(inspected.contentType());
            created.setFileSize(inspected.size());
            created.setStatus(RecordStatus.ACTIVE);
            expectOne(contentResourceMapper.insertContentResource(created));
            if (previous != null) {
                resourceCleanup.deleteAfterCommit(List.of(previous.getObjectKey()));
            }
            ContentResource stored = contentResourceMapper.selectActiveByContentDetailIdAndType(
                    contentDetailId, resourceType);
            return new UploadResult(ContentResourceResponse.from(stored), expected >= WARNING_BYTES);
        } catch (RuntimeException exception) {
            resourceCleanup.deleteNewObject(objectKey);
            throw exception;
        }
    }

    @Transactional
    public void delete(
            Long teacherId,
            Long curriculumId,
            Long categoryId,
            Long contentDetailId,
            Long resourceId) {
        requireOwned(teacherId, curriculumId, categoryId, contentDetailId);
        ContentResource resource = contentResourceMapper.selectActiveForTeacher(
                teacherId, curriculumId, categoryId, contentDetailId, resourceId);
        if (resource == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        expectOne(contentResourceMapper.softDeleteActiveById(resourceId, contentDetailId));
        resourceCleanup.deleteAfterCommit(List.of(resource.getObjectKey()));
    }

    @Transactional(readOnly = true)
    public OpenedResource openForTeacher(
            Long teacherId,
            Long curriculumId,
            Long categoryId,
            Long contentDetailId,
            Long resourceId,
            String rangeHeader,
            String disposition) {
        ContentResource resource = contentResourceMapper.selectActiveForTeacher(
                teacherId, curriculumId, categoryId, contentDetailId, resourceId);
        if (resource == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return open(resource, rangeHeader, disposition);
    }

    @Transactional(readOnly = true)
    public OpenedResource openForStudent(StudentPrincipal principal, Long resourceId, String rangeHeader, String disposition) {
        if (principal == null || principal.studentId() == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        if (principal.teacherStudentAccessId() == null) {
            throw new BusinessException(ErrorCode.STUDENT_SCOPE_REQUIRED);
        }
        ContentResource resource = contentResourceMapper.selectActiveForStudent(
                principal.studentId(), principal.teacherStudentAccessId(), resourceId);
        if (resource == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return open(resource, rangeHeader, disposition);
    }

    @Transactional(readOnly = true)
    public List<ContentResource> activeForContentDetails(List<Long> contentDetailIds) {
        if (contentDetailIds == null || contentDetailIds.isEmpty()) {
            return List.of();
        }
        return contentResourceMapper.selectActiveByContentDetailIds(contentDetailIds);
    }

    private OpenedResource open(ContentResource resource, String rangeHeader, String disposition) {
        String mode = disposition == null || disposition.isBlank() ? "inline" : disposition;
        if (!mode.equals("inline") && !mode.equals("attachment")) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        ByteRanges.Resolved range = ByteRanges.resolve(rangeHeader, resource.getFileSize());
        StoredObjectContent content = objectStorageGateway.open(resource.getObjectKey(), range.httpRange());
        return new OpenedResource(
                content,
                resource,
                mode,
                range.partial(),
                range.start(),
                range.end(),
                range.length());
    }

    private void requireOwned(Long teacherId, Long curriculumId, Long categoryId, Long contentDetailId) {
        Curriculum curriculum = curriculumMapper.selectActiveCurriculumByIdAndTeacherId(curriculumId, teacherId);
        if (curriculum == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        Category category = categoryMapper.selectActiveCategoryByIdAndCurriculumId(categoryId, curriculumId);
        if (category == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        ContentDetail contentDetail = contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(
                contentDetailId, categoryId);
        if (contentDetail == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    private void lockContentDetail(Long contentDetailId, Long categoryId) {
        ContentDetail locked;
        try {
            locked = contentDetailMapper.lockContentDetailById(contentDetailId);
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.RESOURCE_STORAGE_CONFLICT);
            }
            throw exception;
        }
        if (locked == null
                || !categoryId.equals(locked.getCategoryId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    private void lockAdmission() {
        try {
            Long id = contentResourceMapper.lockStorageAdmission();
            if (id == null) {
                throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.RESOURCE_STORAGE_CONFLICT);
            }
            throw exception;
        }
    }

    private void expectOne(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    private boolean isLockTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CannotAcquireLockException) {
                return true;
            }
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == 30006) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("ORA-30006")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public record UploadResult(ContentResourceResponse resource, boolean storageWarning) {
    }

    public record OpenedResource(
            StoredObjectContent content,
            ContentResource resource,
            String disposition,
            boolean partial,
            long start,
            long end,
            long length) {
    }
}
