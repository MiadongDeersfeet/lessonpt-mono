package com.yunki.lessonpt.student.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.yunki.lessonpt.student.domain.Student;

@Mapper
public interface StudentMapper {

    Student selectStudentById(Long studentId);

    Student selectActiveStudentById(Long studentId);

    Student selectStudentByEmail(String email);

    Student selectActiveStudentByEmail(String email);

    int insertStudent(Student student);

    int updateStudent(Student student);

    int softDeleteStudent(Long studentId);

    int restoreStudent(Long studentId);
}
