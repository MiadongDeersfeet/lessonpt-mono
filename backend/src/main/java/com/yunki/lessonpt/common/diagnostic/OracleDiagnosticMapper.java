package com.yunki.lessonpt.common.diagnostic;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 현재 애플리케이션이 어느 Oracle Schema에 연결되어 있는지 확인한다.
 *
 * 개발 환경과 테스트 환경의 Schema를 분리할 예정이기 때문에
 * 단순히 DB 연결 성공 여부만 확인하는 것으로는 부족하다.
 *
 * DB, PDB, CURRENT_SCHEMA와 Schema v3 테이블 존재 여부만 읽고
 * 데이터는 변경하지 않는다.
 */
@Mapper
public interface OracleDiagnosticMapper {

    OracleContext selectContext();

    /**
     * USER_TABLES에서 넘긴 테이블 이름만 센다.
     * 로그인한 계정이 소유한 테이블 기준이라, 개발 Schema 계정으로 접속해야 한다.
     */
    int countExistingTables(@Param("tableNames") List<String> tableNames);
}
