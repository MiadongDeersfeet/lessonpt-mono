package com.yunki.lessonpt.teacher.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.yunki.lessonpt.teacher.domain.Teacher;

@Mapper
public interface TeacherMapper {

    Teacher selectTeacherById(Long teacherId);

    Teacher selectActiveTeacherById(Long teacherId);

    Teacher selectTeacherByEmail(String email);

    Teacher selectActiveTeacherByEmail(String email);

    int insertTeacher(Teacher teacher);

    int updateTeacher(Teacher teacher);

    int softDeleteTeacher(Long teacherId);

    int restoreTeacher(Long teacherId);

    Teacher lockTeacherById(Long teacherId);
}
