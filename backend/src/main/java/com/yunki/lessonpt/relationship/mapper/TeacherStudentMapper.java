package com.yunki.lessonpt.relationship.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.dto.ActiveTeacherStudent;

@Mapper
public interface TeacherStudentMapper {

    TeacherStudent selectTeacherStudentById(Long teacherStudentId);

    TeacherStudent selectByTeacherIdAndStudentId(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId);

    TeacherStudent selectActiveByTeacherIdAndStudentId(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId);

    ActiveTeacherStudent selectActiveStudentForTeacher(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId);

    List<ActiveTeacherStudent> selectActiveStudentsByTeacherId(Long teacherId);

    int insertTeacherStudent(TeacherStudent teacherStudent);

    int softDeleteTeacherStudent(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId);

    int restoreTeacherStudent(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId);

    TeacherStudent lockTeacherStudentById(Long teacherStudentId);
}
