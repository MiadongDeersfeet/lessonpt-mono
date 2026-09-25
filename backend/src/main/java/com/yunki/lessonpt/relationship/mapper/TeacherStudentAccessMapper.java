package com.yunki.lessonpt.relationship.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;

@Mapper
public interface TeacherStudentAccessMapper {

    TeacherStudentAccess selectByTeacherStudentId(Long teacherStudentId);

    TeacherStudentAccess selectActiveByTeacherStudentId(Long teacherStudentId);

    /**
     * 활성 접근권한만 찾는다. 키 조회 성공은 학생 인증이 아니다.
     */
    TeacherStudentAccess selectByPublicAccessKey(String publicAccessKey);

    int insertTeacherStudentAccess(TeacherStudentAccess teacherStudentAccess);

    /**
     * 폐기된 행만 새 공개 키로 다시 활성화한다. ID는 바꾸지 않는다.
     */
    int reactivateTeacherStudentAccess(TeacherStudentAccess teacherStudentAccess);

    /**
     * 관계의 활성 접근권한만 폐기한다. 영향 행 0은 정상이다.
     */
    int softDeleteActiveByTeacherStudentId(Long teacherStudentId);

    TeacherStudentAccess lockTeacherStudentAccessById(Long teacherStudentAccessId);

    int updateLastVerifiedAt(
            @Param("teacherStudentAccessId") Long teacherStudentAccessId,
            @Param("lastVerifiedAt") LocalDateTime lastVerifiedAt);
}
