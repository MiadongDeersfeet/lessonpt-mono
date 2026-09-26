package com.yunki.lessonpt.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.resource.domain.ContentResource;
import com.yunki.lessonpt.resource.domain.ResourceType;

@Mapper
public interface ContentResourceMapper {

    ContentResource selectActiveByContentDetailIdAndType(
            @Param("contentDetailId") Long contentDetailId,
            @Param("resourceType") ResourceType resourceType);

    ContentResource selectActiveForTeacher(
            @Param("teacherId") Long teacherId,
            @Param("curriculumId") Long curriculumId,
            @Param("categoryId") Long categoryId,
            @Param("contentDetailId") Long contentDetailId,
            @Param("resourceId") Long resourceId);

    ContentResource selectActiveForStudent(
            @Param("studentId") Long studentId,
            @Param("teacherStudentAccessId") Long teacherStudentAccessId,
            @Param("resourceId") Long resourceId);

    List<ContentResource> selectActiveByContentDetailIds(@Param("contentDetailIds") List<Long> contentDetailIds);

    List<String> selectActiveObjectKeysByContentDetailId(Long contentDetailId);

    List<String> selectActiveObjectKeysByCategoryId(Long categoryId);

    List<String> selectActiveObjectKeysByCurriculumId(Long curriculumId);

    Long selectActiveFileSizeSum();

    Long lockStorageAdmission();

    int insertContentResource(ContentResource resource);

    int softDeleteActiveById(
            @Param("resourceId") Long resourceId,
            @Param("contentDetailId") Long contentDetailId);

    int softDeleteActiveByContentDetailId(Long contentDetailId);

    int softDeleteActiveByCategoryId(Long categoryId);

    int softDeleteActiveByCurriculumId(Long curriculumId);
}
