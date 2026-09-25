package com.yunki.lessonpt.relationship.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.relationship.domain.StudentAccessSession;

@Mapper
public interface StudentAccessSessionMapper {

    StudentAccessSession selectByTokenHash(String sessionTokenHash);

    StudentAccessSession selectActiveByAccessId(Long teacherStudentAccessId);

    int insertStudentAccessSession(StudentAccessSession session);

    int revokeActiveByAccessId(
            @Param("teacherStudentAccessId") Long teacherStudentAccessId,
            @Param("revokedAt") LocalDateTime revokedAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    int revokeActiveByTeacherStudentId(
            @Param("teacherStudentId") Long teacherStudentId,
            @Param("revokedAt") LocalDateTime revokedAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    int revokeActiveByStudentId(
            @Param("studentId") Long studentId,
            @Param("revokedAt") LocalDateTime revokedAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    int updateSlidingWindow(StudentAccessSession session);

    int updateSelectedAccess(StudentAccessSession session);

    int revokeBySessionId(
            @Param("studentAccessSessionId") Long studentAccessSessionId,
            @Param("revokedAt") LocalDateTime revokedAt,
            @Param("updatedAt") LocalDateTime updatedAt);
}
