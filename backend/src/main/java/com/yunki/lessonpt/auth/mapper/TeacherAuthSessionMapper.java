package com.yunki.lessonpt.auth.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;

@Mapper
public interface TeacherAuthSessionMapper {

    int insertAuthSession(TeacherAuthSession session);

    TeacherAuthSession selectActiveSessionByAccessJti(String accessJti);

    TeacherAuthSession selectSessionByRefreshTokenHash(String refreshTokenHash);

    int updateLastUsedAt(@Param("authSessionId") Long authSessionId);

    int updateAccessToken(
            @Param("authSessionId") Long authSessionId,
            @Param("accessJti") String accessJti,
            @Param("accessExpiresAt") LocalDateTime accessExpiresAt);

    int deleteAuthSession(Long authSessionId);

    int deleteAuthSessionsByTeacherId(Long teacherId);
}
