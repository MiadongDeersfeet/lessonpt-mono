package com.yunki.lessonpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LessonPT 백엔드의 시작점이다.
 *
 * 현재 프로젝트는 기능 중심 패키지 구조를 사용할 예정이므로
 * 이 클래스에는 별도의 비즈니스 로직을 두지 않는다.
 *
 * Spring Boot 실행과 Component Scan의 기준점 역할만 담당한다.
 */
@SpringBootApplication
public class LessonPtApplication {

    public static void main(String[] args) {
        SpringApplication.run(LessonPtApplication.class, args);
    }
}
