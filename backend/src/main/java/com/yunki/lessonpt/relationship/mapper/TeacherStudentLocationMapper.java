package com.yunki.lessonpt.relationship.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.dto.TeacherStudentLocationView;

@Mapper
public interface TeacherStudentLocationMapper {

    TeacherStudentLocation selectTeacherStudentLocationById(Long teacherStudentLocationId);

    TeacherStudentLocation selectActiveTeacherStudentLocationById(Long teacherStudentLocationId);

    TeacherStudentLocation selectByTeacherStudentIdAndLocationId(
            @Param("teacherStudentId") Long teacherStudentId,
            @Param("locationId") Long locationId);

    TeacherStudentLocation selectActiveByTeacherStudentIdAndLocationId(
            @Param("teacherStudentId") Long teacherStudentId,
            @Param("locationId") Long locationId);

    List<TeacherStudentLocation> selectActiveByTeacherStudentId(Long teacherStudentId);

    List<TeacherStudentLocation> selectActiveByLocationId(Long locationId);

    TeacherStudentLocationView selectActiveViewById(Long teacherStudentLocationId);

    List<TeacherStudentLocationView> selectActiveViewsByTeacherStudentId(Long teacherStudentId);

    int insertTeacherStudentLocation(TeacherStudentLocation teacherStudentLocation);

    int softDeleteTeacherStudentLocation(Long teacherStudentLocationId);

    /**
     * 강사-학생 관계의 활성 장소 연결만 비활성화한다. 영향 행 0은 정상이다.
     */
    int softDeleteActiveByTeacherStudentId(Long teacherStudentId);

    /**
     * 한 장소의 활성 학생 연결만 비활성화한다. 영향 행 0은 정상이다.
     */
    int softDeleteActiveByLocationId(Long locationId);

    int restoreTeacherStudentLocation(Long teacherStudentLocationId);

    TeacherStudentLocation lockTeacherStudentLocationById(Long teacherStudentLocationId);
}
