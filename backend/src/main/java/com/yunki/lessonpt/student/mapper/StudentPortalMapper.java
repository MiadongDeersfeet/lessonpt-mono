package com.yunki.lessonpt.student.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.student.query.StudentPortalCategoryRow;
import com.yunki.lessonpt.student.query.StudentPortalEnrollment;
import com.yunki.lessonpt.student.query.StudentPortalHomeworkRow;
import com.yunki.lessonpt.student.query.StudentPortalIdentity;
import com.yunki.lessonpt.student.query.StudentPortalMonitoringRow;
import com.yunki.lessonpt.student.query.StudentPortalRelationshipRow;

@Mapper
public interface StudentPortalMapper {

    StudentPortalIdentity selectActiveIdentity(Long teacherStudentAccessId);

    String selectActiveStudentName(Long studentId);

    int countActiveAccesses(Long studentId);

    List<StudentPortalRelationshipRow> selectActiveRelationships(Long studentId);

    List<StudentPortalEnrollment> selectActiveEnrollments(Long teacherStudentAccessId);

    List<StudentPortalCategoryRow> selectActiveCategories(@Param("curriculumIds") List<Long> curriculumIds);

    List<StudentPortalMonitoringRow> selectActiveMonitoringContents(
            @Param("studentCurriculumIds") List<Long> studentCurriculumIds);

    List<StudentPortalHomeworkRow> selectActiveHomework(
            @Param("studentCurriculumIds") List<Long> studentCurriculumIds);
}
