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
     * 카테고리 한 행을 비활성화한다.
     * 하위 내용 삭제는 Service가 같은 트랜잭션에서 처리한다.
     */
    int softDeleteCategory(
            @Param("categoryId") Long categoryId,
            @Param("curriculumId") Long curriculumId);

    /**
     * 같은 커리큘럼의 active 카테고리를 한 번에 비활성화한다.
     * 영향 행 0은 정상이다. 표시 순서는 압축하지 않는다.
     */
    int softDeleteActiveCategoriesByCurriculumId(Long curriculumId);

    int restoreCategory(Category category);

    Category lockCategoryById(Long categoryId);

    Integer selectMaxDisplayOrderByCurriculumId(Long curriculumId);

    int shiftActiveDisplayOrdersDown(
            @Param("curriculumId") Long curriculumId,
            @Param("displayOrder") Integer displayOrder);
}
