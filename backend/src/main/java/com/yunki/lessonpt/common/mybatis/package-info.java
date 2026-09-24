/**
 * Schema v3를 Java로 읽을 때의 공통 타입 기준이다.
 *
 * NUMBER(19)는 Long, 화면 순서와 BPM처럼 더 작은 정수 NUMBER는 Integer다.
 * VARCHAR2와 CLOB는 String이다.
 * TIMESTAMP에는 시간대가 없으므로 LocalDateTime으로 두고, 응답용 시간대 변환은 나중에 정한다.
 * PK는 시퀀스와 INSERT 트리거가 만들므로 여기서 ID를 생성하지 않는다.
 *
 * Y/N이라도 의미가 다르면 변환을 나누어 둔다.
 * STATUS는 레코드 상태이고, IS_COMPLETED와 IS_REENROLLED만 참/거짓이다.
 */
package com.yunki.lessonpt.common.mybatis;
