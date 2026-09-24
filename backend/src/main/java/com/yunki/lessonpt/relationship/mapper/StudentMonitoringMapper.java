package com.yunki.lessonpt.relationship.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.domain.StudentMonitoring;

@Mapper
public interface StudentMonitoringMapper {

    StudentMonitoring selectStudentMonitoringById(Long monitoringId);

    StudentMonitoring selectActiveStudentMonitoringById(Long monitoringId);

    StudentMonitoring selectByStudentCurriculumIdAndContentDetailId(
            @Param("studentCurriculumId") Long studentCurriculumId,
            @Param("contentDetailId") Long contentDetailId);

    StudentMonitoring selectActiveByStudentCurriculumIdAndContentDetailId(
            @Param("studentCurriculumId") Long studentCurriculumId,
            @Param("contentDetailId") Long contentDetailId);

    List<StudentMonitoring> selectActiveStudentMonitoringsByStudentCurriculumId(Long studentCurriculumId);

    int insertStudentMonitoring(StudentMonitoring studentMonitoring);

    int updateStudentMonitoring(StudentMonitoring studentMonitoring);

    /**
     * 모니터링 행만 비활성화한다.
     * TODO: Homework가 구현되면 삭제 시 활성 과제도 같은 트랜잭션에서 soft delete한다. 복구 때 과제는 자동 복구하지 않는다.
     */
    int softDeleteStudentMonitoring(
            @Param("monitoringId") Long monitoringId,
            @Param("studentCurriculumId") Long studentCurriculumId);

    int restoreStudentMonitoring(StudentMonitoring studentMonitoring);

    /**
     * 모니터링 행 잠금이다.
     * 생성과 복구의 순서 보호는 StudentCurriculumMapper.lockStudentCurriculumById로 수강 행을 잠근다.
     */
    StudentMonitoring lockStudentMonitoringById(Long monitoringId);

    Integer selectMaxDisplayOrderByStudentCurriculumId(Long studentCurriculumId);

    int shiftActiveDisplayOrdersDown(
            @Param("studentCurriculumId") Long studentCurriculumId,
            @Param("displayOrder") Integer displayOrder);
}
