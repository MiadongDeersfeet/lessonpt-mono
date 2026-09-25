package com.yunki.lessonpt.student.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.student.query.StudentPortalContentRow;
import com.yunki.lessonpt.student.query.StudentPortalEnrollment;
import com.yunki.lessonpt.student.query.StudentPortalHomeworkRow;
import com.yunki.lessonpt.student.query.StudentPortalIdentity;
import com.yunki.lessonpt.student.query.StudentPortalMonitoringRow;

@Mapper
public interface StudentPortalMapper {

    StudentPortalIdentity selectActiveIdentity(Long teacherStudentAccessId);

    List<StudentPortalEnrollment> selectActiveEnrollments(Long teacherStudentAccessId);

    List<StudentPortalContentRow> selectActiveContents(@Param("curriculumIds") List<Long> curriculumIds);

    List<StudentPortalMonitoringRow> selectActiveMonitoring(
            @Param("studentCurriculumIds") List<Long> studentCurriculumIds);

    List<StudentPortalHomeworkRow> selectActiveHomework(
            @Param("studentCurriculumIds") List<Long> studentCurriculumIds);
}
