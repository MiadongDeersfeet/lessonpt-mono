package com.yunki.lessonpt.curriculum.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.resource.service.ResourceCleanup;
import com.yunki.lessonpt.resource.service.YoutubeUrls;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContentDetailService {

    private static final int MIN_BPM = 60;
    private static final int MAX_BPM = 240;

    private final ContentDetailMapper contentDetailMapper;
    private final CategoryMapper categoryMapper;
    private final CurriculumMapper curriculumMapper;
    private final ResourceCleanup resourceCleanup;

    @Transactional
    public ContentDetail createContentDetail(
            Long teacherId, Long curriculumId, Long categoryId, ContentDetailChange change) {
        requireName(change.getName());
        requireBpm(change.getTargetBpm());
        YoutubeUrls.requireValid(change.getYoutubeUrl());
        requireOwnedCurriculum(teacherId, curriculumId);
        requireActiveCategory(curriculumId, categoryId);
        lockOwnedCategory(curriculumId, categoryId);

        Integer maxOrder = contentDetailMapper.selectMaxDisplayOrderByCategoryId(categoryId);
        ContentDetail contentDetail = new ContentDetail();
        contentDetail.setCategoryId(categoryId);
        contentDetail.setName(change.getName());
        contentDetail.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        contentDetail.setMemo(change.getMemo());
        contentDetail.setTargetBpm(change.getTargetBpm());
        contentDetail.setEvaluationMemo(change.getEvaluationMemo());
        contentDetail.setYoutubeUrl(change.getYoutubeUrl());
        contentDetail.setStatus(RecordStatus.ACTIVE);
        expectOne(contentDetailMapper.insertContentDetail(contentDetail));
        return requireActive(categoryId, contentDetail.getContentDetailId());
    }

    @Transactional(readOnly = true)
    public List<ContentDetail> getContentDetails(Long teacherId, Long curriculumId, Long categoryId) {
        requireOwnedCurriculum(teacherId, curriculumId);
        requireActiveCategory(curriculumId, categoryId);
        return contentDetailMapper.selectActiveContentDetailsByCategoryId(categoryId);
    }

    @Transactional(readOnly = true)
    public ContentDetail getContentDetail(
            Long teacherId, Long curriculumId, Long categoryId, Long contentDetailId) {
        requireOwnedCurriculum(teacherId, curriculumId);
        requireActiveCategory(curriculumId, categoryId);
        return requireActive(categoryId, contentDetailId);
    }

    @Transactional
    public ContentDetail updateContentDetail(
            Long teacherId, Long curriculumId, Long categoryId, Long contentDetailId, ContentDetailChange change) {
        if (change.isNameSpecified()) {
            requireName(change.getName());
        }
        if (change.isTargetBpmSpecified()) {
            requireBpm(change.getTargetBpm());
        }
        if (change.isYoutubeUrlSpecified()) {
            YoutubeUrls.requireValid(change.getYoutubeUrl());
        }
        requireOwnedCurriculum(teacherId, curriculumId);
        requireActiveCategory(curriculumId, categoryId);
        ContentDetail contentDetail = requireActive(categoryId, contentDetailId);
        ContentDetail locked = lockContentDetail(contentDetailId);
        if (!categoryId.equals(locked.getCategoryId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        applyChange(contentDetail, change);
        expectOne(contentDetailMapper.updateContentDetail(contentDetail));
        return requireActive(categoryId, contentDetailId);
    }

    /**
     * 내용 행만 비활성화하고, 같은 카테고리의 뒤 순서를 당긴다.
     * 학습 이력은 지우지 않는다.
     */
    @Transactional
    public void deleteContentDetail(Long teacherId, Long curriculumId, Long categoryId, Long contentDetailId) {
        requireOwnedCurriculum(teacherId, curriculumId);
        requireActiveCategory(curriculumId, categoryId);
        lockOwnedCategory(curriculumId, categoryId);
        ContentDetail contentDetail = requireActive(categoryId, contentDetailId);
        resourceCleanup.discardContentDetail(contentDetailId);
        expectOne(contentDetailMapper.softDeleteContentDetail(contentDetailId, categoryId));
        contentDetailMapper.shiftActiveDisplayOrdersDown(categoryId, contentDetail.getDisplayOrder());
    }

    @Transactional
    public ContentDetail restoreContentDetail(
            Long teacherId, Long curriculumId, Long categoryId, Long contentDetailId) {
        requireOwnedCurriculum(teacherId, curriculumId);
        requireActiveCategory(curriculumId, categoryId);
        lockOwnedCategory(curriculumId, categoryId);
        ContentDetail contentDetail = contentDetailMapper.selectContentDetailById(contentDetailId);
        if (contentDetail == null || !categoryId.equals(contentDetail.getCategoryId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (contentDetail.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        Integer maxOrder = contentDetailMapper.selectMaxDisplayOrderByCategoryId(categoryId);
        contentDetail.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        contentDetail.setStatus(RecordStatus.ACTIVE);
        contentDetail.setDeletedAt(null);
        expectOne(contentDetailMapper.restoreContentDetail(contentDetail));
        return requireActive(categoryId, contentDetailId);
    }

    private void applyChange(ContentDetail contentDetail, ContentDetailChange change) {
        if (change.isNameSpecified()) {
            contentDetail.setName(change.getName());
        }
        if (change.isMemoSpecified()) {
            contentDetail.setMemo(change.getMemo());
        }
        if (change.isTargetBpmSpecified()) {
            contentDetail.setTargetBpm(change.getTargetBpm());
        }
        if (change.isEvaluationMemoSpecified()) {
            contentDetail.setEvaluationMemo(change.getEvaluationMemo());
        }
        if (change.isYoutubeUrlSpecified()) {
            contentDetail.setYoutubeUrl(change.getYoutubeUrl());
        }
    }

    private Curriculum requireOwnedCurriculum(Long teacherId, Long curriculumId) {
        Curriculum curriculum = curriculumMapper.selectActiveCurriculumByIdAndTeacherId(curriculumId, teacherId);
        if (curriculum == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return curriculum;
    }

    private Category requireActiveCategory(Long curriculumId, Long categoryId) {
        Category category = categoryMapper.selectActiveCategoryByIdAndCurriculumId(categoryId, curriculumId);
        if (category == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return category;
    }

    private Category lockOwnedCategory(Long curriculumId, Long categoryId) {
        Category locked;
        try {
            locked = categoryMapper.lockCategoryById(categoryId);
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }
            throw exception;
        }
        if (locked == null
                || !curriculumId.equals(locked.getCurriculumId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return locked;
    }

    private ContentDetail requireActive(Long categoryId, Long contentDetailId) {
        ContentDetail contentDetail = contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(
                contentDetailId, categoryId);
        if (contentDetail == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return contentDetail;
    }

    private ContentDetail lockContentDetail(Long contentDetailId) {
        try {
            ContentDetail locked = contentDetailMapper.lockContentDetailById(contentDetailId);
            if (locked == null) {
                throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
            }
            return locked;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }
            throw exception;
        }
    }

    private void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }

    private void requireBpm(Integer targetBpm) {
        if (targetBpm == null) {
            return;
        }
        if (targetBpm < MIN_BPM || targetBpm > MAX_BPM) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
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
}
