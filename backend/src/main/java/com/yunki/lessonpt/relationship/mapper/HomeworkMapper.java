package com.yunki.lessonpt.relationship.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.domain.Homework;

@Mapper
public interface HomeworkMapper {

    Homework selectHomeworkById(Long homeworkId);

    Homework selectActiveHomeworkById(Long homeworkId);

    Homework selectActiveHomeworkByIdAndMonitoringId(
            @Param("homeworkId") Long homeworkId,
            @Param("monitoringId") Long monitoringId);

    List<Homework> selectActiveHomeworksByMonitoringId(Long monitoringId);

    int insertHomework(Homework homework);

    int updateHomework(Homework homework);

    int softDeleteHomework(
            @Param("homeworkId") Long homeworkId,
            @Param("monitoringId") Long monitoringId);

    /**
     * 같은 모니터링의 active 과제를 한 번에 비활성화한다.
     * 영향 행 0은 정상이다. 모니터링 복구 때 과제는 자동 복구하지 않는다.
     */
    int softDeleteActiveHomeworksByMonitoringId(Long monitoringId);

    int restoreHomework(Homework homework);

    Homework lockHomeworkById(Long homeworkId);
}
