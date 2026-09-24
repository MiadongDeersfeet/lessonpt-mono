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
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.dto.CategoryUpdateRequest;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final CurriculumMapper curriculumMapper;
    private final ContentDetailMapper contentDetailMapper;

    @Transactional
    public Category createCategory(Long teacherId, Long curriculumId, String name) {
        requireName(name);
        lockOwnedCurriculum(teacherId, curriculumId);
        Integer maxOrder = categoryMapper.selectMaxDisplayOrderByCurriculumId(curriculumId);
        int nextOrder = maxOrder == null ? 1 : maxOrder + 1;

        Category category = new Category();
        category.setCurriculumId(curriculumId);
        category.setName(name);
        category.setDisplayOrder(nextOrder);
        category.setStatus(RecordStatus.ACTIVE);
        expectOne(categoryMapper.insertCategory(category));
        return requireActive(curriculumId, category.getCategoryId());
    }

    @Transactional(readOnly = true)
    public List<Category> getCategories(Long teacherId, Long curriculumId) {
        requireOwnedCurriculum(teacherId, curriculumId);
        return categoryMapper.selectActiveCategoriesByCurriculumId(curriculumId);
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long teacherId, Long curriculumId, Long categoryId) {
        requireOwnedCurriculum(teacherId, curriculumId);
        return requireActive(curriculumId, categoryId);
    }

    @Transactional
    public Category updateCategory(Long teacherId, Long curriculumId, Long categoryId, CategoryUpdateRequest request) {
        if (request.isNameSpecified()) {
            requireName(request.getName());
        }
        requireOwnedCurriculum(teacherId, curriculumId);
        Category category = requireActive(curriculumId, categoryId);
        Category locked = lockCategory(categoryId);
        if (!curriculumId.equals(locked.getCurriculumId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (request.isNameSpecified()) {
            category.setName(request.getName());
        }
        expectOne(categoryMapper.updateCategory(category));
        return requireActive(curriculumId, categoryId);
    }

    /**
     * 카테고리와 그 active 내용을 비활성화하고, 같은 커리큘럼의 뒤 카테고리 순서를 당긴다.
     * 내용의 표시 순서는 압축하지 않는다. 삭제한 내용은 카테고리 복구 때 되살리지 않는다.
     */
    @Transactional
    public void deleteCategory(Long teacherId, Long curriculumId, Long categoryId) {
        lockOwnedCurriculum(teacherId, curriculumId);
        Category category = requireActive(curriculumId, categoryId);
        contentDetailMapper.softDeleteActiveContentDetailsByCategoryId(categoryId);
        expectOne(categoryMapper.softDeleteCategory(categoryId, curriculumId));
        categoryMapper.shiftActiveDisplayOrdersDown(curriculumId, category.getDisplayOrder());
    }

    @Transactional
    public Category restoreCategory(Long teacherId, Long curriculumId, Long categoryId) {
        lockOwnedCurriculum(teacherId, curriculumId);
        Category category = categoryMapper.selectCategoryById(categoryId);
        if (category == null || !curriculumId.equals(category.getCurriculumId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (category.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        Integer maxOrder = categoryMapper.selectMaxDisplayOrderByCurriculumId(curriculumId);
        category.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        category.setStatus(RecordStatus.ACTIVE);
        category.setDeletedAt(null);
        expectOne(categoryMapper.restoreCategory(category));
        return requireActive(curriculumId, categoryId);
    }

    private Curriculum requireOwnedCurriculum(Long teacherId, Long curriculumId) {
        Curriculum curriculum = curriculumMapper.selectActiveCurriculumByIdAndTeacherId(curriculumId, teacherId);
        if (curriculum == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return curriculum;
    }

    private Curriculum lockOwnedCurriculum(Long teacherId, Long curriculumId) {
        requireOwnedCurriculum(teacherId, curriculumId);
        Curriculum locked;
        try {
            locked = curriculumMapper.lockCurriculumById(curriculumId);
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }
            throw exception;
        }
        if (locked == null
                || !teacherId.equals(locked.getTeacherId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return locked;
    }

    private Category requireActive(Long curriculumId, Long categoryId) {
        Category category = categoryMapper.selectActiveCategoryByIdAndCurriculumId(categoryId, curriculumId);
        if (category == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return category;
    }

    private Category lockCategory(Long categoryId) {
        try {
            Category locked = categoryMapper.lockCategoryById(categoryId);
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
