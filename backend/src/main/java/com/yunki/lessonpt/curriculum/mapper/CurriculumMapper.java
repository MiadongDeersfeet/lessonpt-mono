package com.yunki.lessonpt.curriculum.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.curriculum.domain.Curriculum;

@Mapper
public interface CurriculumMapper {

    Curriculum selectCurriculumById(Long curriculumId);

    Curriculum selectActiveCurriculumById(Long curriculumId);

    Curriculum selectActiveCurriculumByIdAndTeacherId(
            @Param("curriculumId") Long curriculumId,
            @Param("teacherId") Long teacherId);

    List<Curriculum> selectActiveCurriculumsByTeacherId(Long teacherId);

    int insertCurriculum(Curriculum curriculum);

    int updateCurriculum(Curriculum curriculum);

    /**
     * 커리큘럼 행만 비활성화한다.
     * TODO: Category, ContentDetail이 생기면 하위 행 soft delete는 Service에서 이어서 처리한다.
     */
    int softDeleteCurriculum(
            @Param("curriculumId") Long curriculumId,
            @Param("teacherId") Long teacherId);

    int restoreCurriculum(Curriculum curriculum);

    Curriculum lockCurriculumById(Long curriculumId);

    Integer selectMaxDisplayOrderByTeacherId(Long teacherId);

    int shiftActiveDisplayOrdersDown(
            @Param("teacherId") Long teacherId,
            @Param("displayOrder") Integer displayOrder);
}
