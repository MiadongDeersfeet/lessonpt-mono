package com.yunki.lessonpt.relationship.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.domain.StudentCurriculum;

@Mapper
public interface StudentCurriculumMapper {

    StudentCurriculum selectStudentCurriculumById(Long studentCurriculumId);

    StudentCurriculum selectActiveStudentCurriculumById(Long studentCurriculumId);

    StudentCurriculum selectByTeacherStudentLocationIdAndCurriculumId(
            @Param("teacherStudentLocationId") Long teacherStudentLocationId,
            @Param("curriculumId") Long curriculumId);

    StudentCurriculum selectActiveByTeacherStudentLocationIdAndCurriculumId(
            @Param("teacherStudentLocationId") Long teacherStudentLocationId,
            @Param("curriculumId") Long curriculumId);

    List<StudentCurriculum> selectActiveStudentCurriculumsByTeacherStudentLocationId(
            Long teacherStudentLocationId);

    int insertStudentCurriculum(StudentCurriculum studentCurriculum);

    int updateStudentCurriculum(StudentCurriculum studentCurriculum);

    /**
     * 수강 배정만 비활성화한다.
     * 학습 이력은 지우지 않는다.
     */
    int softDeleteStudentCurriculum(
            @Param("studentCurriculumId") Long studentCurriculumId,
            @Param("teacherStudentLocationId") Long teacherStudentLocationId);

    /**
     * 장소 연결에 속한 활성 수강만 비활성화한다.
     * 모니터링과 과제는 학습 이력으로 남긴다. 영향 행 0은 정상이다.
     */
    int softDeleteActiveStudentCurriculumsByTeacherStudentLocationId(Long teacherStudentLocationId);

    /**
     * 비활성 배정을 다시 활성화하고 재수강으로 표시한다.
     */
    int restoreStudentCurriculum(Long studentCurriculumId);

    /**
     * 배정 행 잠금이다.
     * 아직 행이 없는 최초 배정의 중복 방지는 이 잠금으로 하지 않는다.
     */
    StudentCurriculum lockStudentCurriculumById(Long studentCurriculumId);
}
