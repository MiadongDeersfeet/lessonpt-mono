package com.yunki.lessonpt.common.diagnostic;

import java.util.List;

/**
 * 현재 Oracle Schema v3에서 LessonPT가 쓰는 테이블 이름이다.
 *
 * USER_TABLES 전체를 세지 않고 이 목록만 확인한다.
 * 다른 테이블이 더 있어도 이 15개가 있으면 현재 구조로 본다.
 */
public final class SchemaV3Tables {

    public static final List<String> NAMES = List.of(
            "TB_TEACHER",
            "TB_TEACHER_AUTH_SESSION",
            "TB_LOCATION",
            "TB_STUDENT",
            "TB_TEACHER_STUDENT",
            "TB_TEACHER_STUDENT_ACCESS",
            "TB_STUDENT_EMAIL_VERIFICATION",
            "TB_STUDENT_ACCESS_SESSION",
            "TB_TEACHER_STUDENT_LOCATION",
            "TB_CURRICULUM",
            "TB_CATEGORY",
            "TB_CONTENT_DETAIL",
            "TB_STUDENT_CURRICULUM",
            "TB_STUDENT_MONITORING",
            "TB_HOMEWORK");

    public static final int EXPECTED_COUNT = NAMES.size();

    private SchemaV3Tables() {
    }
}
