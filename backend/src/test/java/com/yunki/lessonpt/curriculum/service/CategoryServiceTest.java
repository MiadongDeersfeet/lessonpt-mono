package com.yunki.lessonpt.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.dto.CategoryUpdateRequest;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CurriculumMapper curriculumMapper;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryMapper, curriculumMapper);
    }

    @Test
    void createAssignsOrderOneWhenNoneExist() {
        stubOwnedCurriculum();
        when(categoryMapper.selectMaxDisplayOrderByCurriculumId(40L)).thenReturn(null);
        when(categoryMapper.insertCategory(any())).thenAnswer(invocation -> {
            invocation.<Category>getArgument(0).setCategoryId(70L);
            return 1;
        });
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(70L, 40L)).thenReturn(saved(70L, 40L, 1, "준비"));

        Category created = categoryService.createCategory(8L, 40L, "준비");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        InOrder order = inOrder(curriculumMapper, categoryMapper);
        order.verify(curriculumMapper).selectActiveCurriculumByIdAndTeacherId(40L, 8L);
        order.verify(curriculumMapper).lockCurriculumById(40L);
        order.verify(categoryMapper).selectMaxDisplayOrderByCurriculumId(40L);
        order.verify(categoryMapper).insertCategory(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
        assertThat(captor.getValue().getCurriculumId()).isEqualTo(40L);
        assertThat(created.getCategoryId()).isEqualTo(70L);
    }

    @Test
    void createAppendsAfterExistingMax() {
        stubOwnedCurriculum();
        when(categoryMapper.selectMaxDisplayOrderByCurriculumId(40L)).thenReturn(3);
        when(categoryMapper.insertCategory(any())).thenAnswer(invocation -> {
            invocation.<Category>getArgument(0).setCategoryId(71L);
            return 1;
        });
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(71L, 40L)).thenReturn(saved(71L, 40L, 4, "연주"));

        categoryService.createCategory(8L, 40L, "연주");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).insertCategory(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(4);
    }

    @Test
    void createRejectsUnownedCurriculumAndLockTimeout() {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 9L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> categoryService.createCategory(9L, 40L, "준비"));

        Curriculum inactive = ownedCurriculum();
        inactive.setStatus(RecordStatus.INACTIVE);
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> categoryService.createCategory(8L, 40L, "준비"));
        verify(curriculumMapper, never()).lockCurriculumById(any());

        stubOwnedCurriculum();
        when(curriculumMapper.lockCurriculumById(40L)).thenThrow(new CannotAcquireLockException("ORA-30006"));
        assertCode(ErrorCode.ORDER_CONFLICT, () -> categoryService.createCategory(8L, 40L, "준비"));
        verify(categoryMapper, never()).insertCategory(any());
    }

    @Test
    void getReturnsOwnCategoriesAndHidesOtherCurriculum() {
        stubReadableCurriculum();
        when(categoryMapper.selectActiveCategoriesByCurriculumId(40L)).thenReturn(List.of(saved(70L, 40L, 1, "준비")));
        assertThat(categoryService.getCategories(8L, 40L)).extracting(Category::getName).containsExactly("준비");

        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(70L, 40L)).thenReturn(saved(70L, 40L, 1, "준비"));
        assertThat(categoryService.getCategory(8L, 40L, 70L).getName()).isEqualTo("준비");

        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 9L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> categoryService.getCategory(9L, 40L, 70L));

        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(71L, 40L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> categoryService.getCategory(8L, 40L, 71L));
    }

    @Test
    void updateChangesNameInsideCurriculum() {
        stubReadableCurriculum();
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(70L, 40L)).thenReturn(saved(70L, 40L, 1, "준비"));
        when(categoryMapper.lockCategoryById(70L)).thenReturn(saved(70L, 40L, 1, "준비"));
        when(categoryMapper.updateCategory(any())).thenReturn(1);

        categoryService.updateCategory(8L, 40L, 70L, updateName("준비운동"));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).updateCategory(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("준비운동");
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void updateRejectsOtherCurriculumBlankNameAndBadRowCount() {
        stubReadableCurriculum();
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(71L, 40L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> categoryService.updateCategory(8L, 40L, 71L, updateName("다른분류")));

        assertCode(ErrorCode.COMMON_INVALID_INPUT, () -> categoryService.updateCategory(8L, 40L, 70L, updateName(" ")));
        verify(categoryMapper, never()).updateCategory(any());

        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(70L, 40L)).thenReturn(saved(70L, 40L, 1, "준비"));
        when(categoryMapper.lockCategoryById(70L)).thenReturn(saved(70L, 40L, 1, "준비"));
        when(categoryMapper.updateCategory(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> categoryService.updateCategory(8L, 40L, 70L, updateName("준비운동")));
    }

    @Test
    void deleteSoftDeletesAndCompressesOnlyThatCurriculum() {
        stubOwnedCurriculum();
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(70L, 40L)).thenReturn(saved(70L, 40L, 1, "준비"));
        when(categoryMapper.softDeleteCategory(70L, 40L)).thenReturn(1);

        categoryService.deleteCategory(8L, 40L, 70L);

        InOrder order = inOrder(curriculumMapper, categoryMapper);
        order.verify(curriculumMapper).lockCurriculumById(40L);
        order.verify(categoryMapper).selectActiveCategoryByIdAndCurriculumId(70L, 40L);
        order.verify(categoryMapper).softDeleteCategory(70L, 40L);
        order.verify(categoryMapper).shiftActiveDisplayOrdersDown(40L, 1);
        verify(categoryMapper, never()).shiftActiveDisplayOrdersDown(eq(41L), any());
    }

    @Test
    void deleteRejectsAnotherTeacherAndUnexpectedRowCount() {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 9L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> categoryService.deleteCategory(9L, 40L, 70L));
        verify(categoryMapper, never()).softDeleteCategory(any(), any());

        stubOwnedCurriculum();
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(70L, 40L)).thenReturn(saved(70L, 40L, 1, "준비"));
        when(categoryMapper.softDeleteCategory(70L, 40L)).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> categoryService.deleteCategory(8L, 40L, 70L));
        verify(categoryMapper, never()).shiftActiveDisplayOrdersDown(any(), any());
    }

    @Test
    void restoreAppendsInactiveCategory() {
        stubOwnedCurriculum();
        Category inactive = saved(70L, 40L, 1, "준비");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(categoryMapper.selectCategoryById(70L)).thenReturn(inactive);
        when(categoryMapper.selectMaxDisplayOrderByCurriculumId(40L)).thenReturn(2);
        when(categoryMapper.restoreCategory(any())).thenReturn(1);
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(70L, 40L)).thenReturn(saved(70L, 40L, 3, "준비"));

        Category restored = categoryService.restoreCategory(8L, 40L, 70L);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        InOrder order = inOrder(curriculumMapper, categoryMapper);
        order.verify(curriculumMapper).lockCurriculumById(40L);
        order.verify(categoryMapper).selectMaxDisplayOrderByCurriculumId(40L);
        order.verify(categoryMapper).restoreCategory(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
        assertThat(restored.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void restoreRejectsActiveOtherCurriculumInactiveParentAndBadRowCount() {
        stubOwnedCurriculum();
        when(categoryMapper.selectCategoryById(70L)).thenReturn(saved(70L, 40L, 1, "준비"));
        assertCode(ErrorCode.COMMON_CONFLICT, () -> categoryService.restoreCategory(8L, 40L, 70L));

        Category other = saved(71L, 41L, 1, "다른과정");
        other.setStatus(RecordStatus.INACTIVE);
        when(categoryMapper.selectCategoryById(71L)).thenReturn(other);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> categoryService.restoreCategory(8L, 40L, 71L));

        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> categoryService.restoreCategory(8L, 40L, 70L));

        stubOwnedCurriculum();
        Category inactive = saved(72L, 40L, 1, "준비");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(categoryMapper.selectCategoryById(72L)).thenReturn(inactive);
        when(categoryMapper.selectMaxDisplayOrderByCurriculumId(40L)).thenReturn(null);
        when(categoryMapper.restoreCategory(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> categoryService.restoreCategory(8L, 40L, 72L));
    }

    private CategoryUpdateRequest updateName(String name) {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName(name);
        return request;
    }

    private void stubOwnedCurriculum() {
        Curriculum curriculum = ownedCurriculum();
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(curriculum);
        when(curriculumMapper.lockCurriculumById(40L)).thenReturn(curriculum);
    }

    private void stubReadableCurriculum() {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(ownedCurriculum());
    }

    private Curriculum ownedCurriculum() {
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumId(40L);
        curriculum.setTeacherId(8L);
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }

    private Category saved(Long categoryId, Long curriculumId, int order, String name) {
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setCurriculumId(curriculumId);
        category.setName(name);
        category.setDisplayOrder(order);
        category.setStatus(RecordStatus.ACTIVE);
        return category;
    }

    private void assertCode(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(errorCode);
    }
}
