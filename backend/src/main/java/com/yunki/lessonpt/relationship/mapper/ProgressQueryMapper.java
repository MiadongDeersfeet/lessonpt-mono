package com.yunki.lessonpt.relationship.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.query.StudentCurriculumProgressView;

@Mapper
public interface ProgressQueryMapper {

    StudentCurriculumProgressView selectProgressByStudentCurriculumId(Long studentCurriculumId);

    List<StudentCurriculumProgressView> selectProgressByStudentCurriculumIds(
            @Param("studentCurriculumIds") List<Long> studentCurriculumIds);

    /**
     * 현재 강사가 소유하고 수강, 장소 연결, 강사-학생, 커리큘럼이 모두 활성인 배정 ID다.
     */
    List<Long> selectOwnedActiveStudentCurriculumIds(
            @Param("teacherId") Long teacherId,
            @Param("studentCurriculumIds") List<Long> studentCurriculumIds);
}
