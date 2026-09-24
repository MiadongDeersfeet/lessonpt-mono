package com.yunki.lessonpt.auth.security;

/**
 * 인증이 끝난 뒤 Controller가 현재 강사를 알 때 쓰는 값이다.
 * 비밀번호 해시는 인증 결과에 남기지 않는다.
 */
public record TeacherPrincipal(Long teacherId, String email) {
}
