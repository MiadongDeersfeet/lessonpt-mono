package com.yunki.lessonpt.relationship.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.query.StudentLearningCurriculumRow;
import com.yunki.lessonpt.relationship.query.StudentLearningHomeworkRow;
import com.yunki.lessonpt.relationship.query.StudentLearningLocationRow;
import com.yunki.lessonpt.relationship.query.StudentLearningMonitoringRow;
import com.yunki.lessonpt.relationship.query.StudentLearningStudentRow;

@Mapper
public interface StudentLearningQueryMapper {

    StudentLearningStudentRow selectActiveStudent(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId);

    List<StudentLearningLocationRow> selectActiveLocations(
            @Param("teacherId") Long teacherId,
            @Param("teacherStudentId") Long teacherStudentId);

    List<StudentLearningCurriculumRow> selectActiveCurriculums(
            @Param("teacherId") Long teacherId,
            @Param("teacherStudentLocationIds") List<Long> teacherStudentLocationIds);

    List<StudentLearningMonitoringRow> selectActiveMonitorings(
            @Param("studentCurriculumIds") List<Long> studentCurriculumIds);

    List<StudentLearningHomeworkRow> selectActiveHomeworks(
            @Param("monitoringIds") List<Long> monitoringIds);
}
