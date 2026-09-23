package com.yunki.lessonpt.common.health;

import org.apache.ibatis.annotations.Mapper;

/**
 * Oracle에 붙어 있는지만 확인하는 Mapper다.
 *
 * 도메인 테이블은 조회하지 않고 DUAL에서 1만 읽는다.
 * 강사, 학생, 커리큘럼 Mapper는 기능을 만들 때 따로 추가한다.
 */
@Mapper
public interface SmokeMapper {

    Integer selectOne();
}
