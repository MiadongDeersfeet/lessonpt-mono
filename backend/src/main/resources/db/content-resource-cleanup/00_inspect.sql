-- Read-only inspect before dropping legacy URL columns.
-- Run this whole file. It does not change data.
-- Stop if sheet_url_set or audio_url_set is greater than 0.
-- YOUTUBE_URL stays. Do not drop it.

SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS current_schema
FROM dual;

SELECT COUNT(*) AS content_detail_rows
FROM tb_content_detail;

SELECT column_name, data_type, data_length, nullable
FROM user_tab_columns
WHERE table_name = 'TB_CONTENT_DETAIL'
  AND column_name IN ('SHEET_URL', 'AUDIO_URL', 'YOUTUBE_URL')
ORDER BY column_name;

-- Non-null counts are dynamic so a missing column does not raise ORA-00904.
-- NULL means that column is already absent.
DECLARE
    sheet_exists NUMBER;
    audio_exists NUMBER;
    result_cursor SYS_REFCURSOR;
BEGIN
    SELECT COUNT(*)
      INTO sheet_exists
      FROM user_tab_columns
     WHERE table_name = 'TB_CONTENT_DETAIL'
       AND column_name = 'SHEET_URL';

    SELECT COUNT(*)
      INTO audio_exists
      FROM user_tab_columns
     WHERE table_name = 'TB_CONTENT_DETAIL'
       AND column_name = 'AUDIO_URL';

    IF sheet_exists = 1 AND audio_exists = 1 THEN
        OPEN result_cursor FOR
            'SELECT '
            || '(SELECT COUNT(*) FROM tb_content_detail WHERE sheet_url IS NOT NULL) AS sheet_url_set, '
            || '(SELECT COUNT(*) FROM tb_content_detail WHERE audio_url IS NOT NULL) AS audio_url_set '
            || 'FROM dual';
    ELSIF sheet_exists = 1 THEN
        OPEN result_cursor FOR
            'SELECT '
            || '(SELECT COUNT(*) FROM tb_content_detail WHERE sheet_url IS NOT NULL) AS sheet_url_set, '
            || 'CAST(NULL AS NUMBER) AS audio_url_set '
            || 'FROM dual';
    ELSIF audio_exists = 1 THEN
        OPEN result_cursor FOR
            'SELECT '
            || 'CAST(NULL AS NUMBER) AS sheet_url_set, '
            || '(SELECT COUNT(*) FROM tb_content_detail WHERE audio_url IS NOT NULL) AS audio_url_set '
            || 'FROM dual';
    ELSE
        OPEN result_cursor FOR
            SELECT CAST(NULL AS NUMBER) AS sheet_url_set,
                   CAST(NULL AS NUMBER) AS audio_url_set
              FROM dual;
    END IF;

    DBMS_SQL.RETURN_RESULT(result_cursor);
END;
/
