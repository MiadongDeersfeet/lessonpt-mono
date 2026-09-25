-- 실행 전 확인. 이 스크립트는 데이터를 바꾸지 않는다.
-- 01_deploy.sql의 신규 이름이 아래에 나오면 배포하지 않는다.
-- Oracle 19c identifier 한도는 COMPATIBLE 12.2 이상에서 128바이트다.
-- 이 스키마의 기존 object 이름은 30자 이하이므로 신규 이름도 30자 이하로 둔다.

SELECT object_name, object_type
FROM user_objects
WHERE object_name IN (
    'SEQ_CONTENT_RESOURCE',
    'TB_CONTENT_RESOURCE',
    'TB_STORAGE_ADMISSION',
    'PK_CONTENT_RESOURCE',
    'FK_RES_CONTENT_DETAIL',
    'CK_RES_STATUS',
    'CK_RES_TYPE',
    'CK_RES_FILE_SIZE',
    'CK_RES_DEL',
    'UQ_RES_OBJECT_KEY',
    'IDX_RES_CONTENT_DETAIL_ID',
    'UX_RES_ACTIVE_TYPE',
    'PK_STORAGE_ADMISSION',
    'CK_STORAGE_ADMISSION_ID'
)
ORDER BY object_type, object_name;

SELECT column_name, data_type, data_precision, data_scale, data_length, char_used, nullable
FROM user_tab_columns
WHERE table_name = 'TB_CONTENT_DETAIL'
ORDER BY column_id;

SELECT COUNT(*) AS content_detail_rows,
       SUM(CASE WHEN status = 'Y' AND deleted_at IS NULL THEN 1 ELSE 0 END) AS active_content_details,
       SUM(CASE WHEN sheet_url IS NOT NULL THEN 1 ELSE 0 END) AS sheet_url_set,
       SUM(CASE WHEN audio_url IS NOT NULL THEN 1 ELSE 0 END) AS audio_url_set,
       SUM(CASE WHEN youtube_url IS NOT NULL THEN 1 ELSE 0 END) AS youtube_url_set
FROM tb_content_detail;

SELECT value AS nls_length_semantics
FROM nls_database_parameters
WHERE parameter = 'NLS_LENGTH_SEMANTICS';
