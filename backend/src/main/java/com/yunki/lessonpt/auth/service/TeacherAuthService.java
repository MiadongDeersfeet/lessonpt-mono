package com.yunki.lessonpt.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.auth.domain.TeacherAuthSession;
import com.yunki.lessonpt.auth.dto.AuthTokenResponse;
import com.yunki.lessonpt.auth.jwt.IssuedToken;
import com.yunki.lessonpt.auth.jwt.JwtProvider;
import com.yunki.lessonpt.auth.jwt.JwtVerificationException;
import com.yunki.lessonpt.auth.jwt.TokenHasher;
import com.yunki.lessonpt.auth.jwt.VerifiedToken;
import com.yunki.lessonpt.auth.mapper.TeacherAuthSessionMapper;
import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.dto.TeacherLoginRequest;
import com.yunki.lessonpt.teacher.dto.TeacherSignupRequest;
import com.yunki.lessonpt.teacher.dto.TeacherSignupResponse;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;
import com.yunki.lessonpt.teacher.service.TeacherService;

/**
 * 로그인 성공은 토큰 발급과 세션 저장이 함께 끝나야 한다.
 * 실패 원인은 계정 없음, 비밀번호 불일치, 비활성을 같은 문구로 돌려 구분하지 않는다.
 */
@Service
public class TeacherAuthService {

    private final TeacherService teacherService;
    private final TeacherMapper teacherMapper;
    private final TeacherAuthSessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenHasher tokenHasher;

    public TeacherAuthService(
            TeacherService teacherService,
            TeacherMapper teacherMapper,
            TeacherAuthSessionMapper sessionMapper,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            TokenHasher tokenHasher) {
        this.teacherService = teacherService;
        this.teacherMapper = teacherMapper;
        this.sessionMapper = sessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public TeacherSignupResponse signup(TeacherSignupRequest request) {
        Teacher teacher = teacherService.createTeacher(
                request.email(), request.password(), request.name(), request.phone());
        return new TeacherSignupResponse(
                teacher.getTeacherId(),
                teacher.getEmail(),
                teacher.getName(),
                teacher.getPhone(),
                teacher.getRole());
    }

    @Transactional
    public AuthTokenResponse login(TeacherLoginRequest request) {
        Teacher teacher = teacherMapper.selectActiveTeacherByEmail(request.email());
        if (teacher == null || !passwordEncoder.matches(request.password(), teacher.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        teacherMapper.lockTeacherById(teacher.getTeacherId());
        return issueSession(teacher);
    }

    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        VerifiedToken verified = parseRefresh(refreshToken);
        TeacherAuthSession session = sessionMapper.selectSessionByRefreshTokenHash(tokenHasher.hash(refreshToken));
        if (session == null || !verified.teacherId().equals(session.getTeacherId())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        Teacher teacher = teacherMapper.selectActiveTeacherById(session.getTeacherId());
        if (teacher == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        IssuedToken access = jwtProvider.createAccessToken(teacher.getTeacherId());
        sessionMapper.updateAccessToken(session.getAuthSessionId(), access.jti(), access.expiresAt());
        return new AuthTokenResponse(access.value(), refreshToken, jwtProvider.accessTokenTtl().toSeconds());
    }

    @Transactional
    public void logout(String accessJti) {
        if (accessJti == null || accessJti.isBlank()) {
            return;
        }
        TeacherAuthSession session = sessionMapper.selectActiveSessionByAccessJti(accessJti);
        if (session != null) {
            sessionMapper.deleteAuthSession(session.getAuthSessionId());
        }
    }

    private AuthTokenResponse issueSession(Teacher teacher) {
        IssuedToken access = jwtProvider.createAccessToken(teacher.getTeacherId());
        IssuedToken refresh = jwtProvider.createRefreshToken(teacher.getTeacherId());
        TeacherAuthSession session = new TeacherAuthSession();
        session.setTeacherId(teacher.getTeacherId());
        session.setAccessJti(access.jti());
        session.setRefreshTokenHash(tokenHasher.hash(refresh.value()));
        session.setAccessExpiresAt(access.expiresAt());
        session.setRefreshExpiresAt(refresh.expiresAt());
        sessionMapper.insertAuthSession(session);
        return new AuthTokenResponse(access.value(), refresh.value(), jwtProvider.accessTokenTtl().toSeconds());
    }

    private VerifiedToken parseRefresh(String refreshToken) {
        try {
            return jwtProvider.parseRefreshToken(refreshToken);
        } catch (JwtVerificationException | IllegalStateException exception) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
    }
}
