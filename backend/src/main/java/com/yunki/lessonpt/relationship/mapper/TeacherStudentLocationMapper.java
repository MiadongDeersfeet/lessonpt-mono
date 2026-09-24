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

    TeacherStudentLocationView selectActiveViewById(Long teacherStudentLocationId);

    List<TeacherStudentLocationView> selectActiveViewsByTeacherStudentId(Long teacherStudentId);

    int insertTeacherStudentLocation(TeacherStudentLocation teacherStudentLocation);

    int softDeleteTeacherStudentLocation(Long teacherStudentLocationId);

    int restoreTeacherStudentLocation(Long teacherStudentLocationId);

    TeacherStudentLocation lockTeacherStudentLocationById(Long teacherStudentLocationId);
}
