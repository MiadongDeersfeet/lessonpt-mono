package com.yunki.lessonpt.curriculum.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.curriculum.domain.ContentDetail;

@Mapper
public interface ContentDetailMapper {

    ContentDetail selectContentDetailById(Long contentDetailId);

    ContentDetail selectActiveContentDetailById(Long contentDetailId);

    ContentDetail selectActiveContentDetailByIdAndCategoryId(
            @Param("contentDetailId") Long contentDetailId,
            @Param("categoryId") Long categoryId);

    List<ContentDetail> selectActiveContentDetailsByCategoryId(Long categoryId);

    int insertContentDetail(ContentDetail contentDetail);

    int updateContentDetail(ContentDetail contentDetail);

    /**
     * 내용 행만 비활성화한다.
     * 학습 이력은 유지한다. Monitoring 행은 물리 삭제하지 않는다.
     */
    int softDeleteContentDetail(
            @Param("contentDetailId") Long contentDetailId,
            @Param("categoryId") Long categoryId);

    /**
     * 같은 카테고리의 active 내용을 한 번에 비활성화한다.
     * 영향 행 0은 정상이다. 학습 이력은 지우지 않고, 표시 순서는 압축하지 않는다.
     */
    int softDeleteActiveContentDetailsByCategoryId(Long categoryId);

    int restoreContentDetail(ContentDetail contentDetail);

    /**
     * 내용 행 잠금이다.
     * 생성, 삭제, 복구의 표시 순서 보호는 CategoryMapper.lockCategoryById로 카테고리 행을 잠근다.
     */
    ContentDetail lockContentDetailById(Long contentDetailId);

    Integer selectMaxDisplayOrderByCategoryId(Long categoryId);

    int shiftActiveDisplayOrdersDown(
            @Param("categoryId") Long categoryId,
            @Param("displayOrder") Integer displayOrder);
}
