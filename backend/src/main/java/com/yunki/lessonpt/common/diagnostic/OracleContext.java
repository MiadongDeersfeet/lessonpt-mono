package com.yunki.lessonpt.common.diagnostic;

/**
 * 지금 붙어 있는 Oracle의 DB, PDB, Schema 이름이다.
 *
 * 개발 Schema와 이후 테스트 Schema를 구분하려면
 * 연결 성공만으로는 부족해서 SYS_CONTEXT 값을 담는다.
 * 비밀번호나 접속 주소는 넣지 않는다.
 */
public class OracleContext {

    private String dbName;
    private String containerName;
    private String currentSchema;

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getCurrentSchema() {
        return currentSchema;
    }

    public void setCurrentSchema(String currentSchema) {
        this.currentSchema = currentSchema;
    }
}
