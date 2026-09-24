package com.yunki.lessonpt.curriculum.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.curriculum.domain.Category;

@Mapper
public interface CategoryMapper {

    Category selectCategoryById(Long categoryId);

    Category selectActiveCategoryById(Long categoryId);

    Category selectActiveCategoryByIdAndCurriculumId(
            @Param("categoryId") Long categoryId,
            @Param("curriculumId") Long curriculumId);

    List<Category> selectActiveCategoriesByCurriculumId(Long curriculumId);

    int insertCategory(Category category);

    int updateCategory(Category category);

    /**
     * 카테고리 행만 비활성화한다.
     * TODO: ContentDetail이 구현되면 삭제 시 하위 행 soft delete는 Service에서 이어서 처리한다.
     */
    int softDeleteCategory(
            @Param("categoryId") Long categoryId,
            @Param("curriculumId") Long curriculumId);

    int restoreCategory(Category category);

    Category lockCategoryById(Long categoryId);

    Integer selectMaxDisplayOrderByCurriculumId(Long curriculumId);

    int shiftActiveDisplayOrdersDown(
            @Param("curriculumId") Long curriculumId,
            @Param("displayOrder") Integer displayOrder);
}
