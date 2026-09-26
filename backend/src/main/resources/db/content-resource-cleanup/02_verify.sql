-- Metadata checks only. Do not select SHEET_URL or AUDIO_URL from TB_CONTENT_DETAIL.
-- Expected: sheet_url_column = 0, audio_url_column = 0, youtube_url_column = 1.

SELECT COUNT(*) AS content_detail_table
FROM user_tables
WHERE table_name = 'TB_CONTENT_DETAIL';

SELECT COUNT(*) AS content_resource_table
FROM user_tables
WHERE table_name = 'TB_CONTENT_RESOURCE';

SELECT COUNT(*) AS storage_admission_table
FROM user_tables
WHERE table_name = 'TB_STORAGE_ADMISSION';

SELECT column_name
FROM user_tab_columns
WHERE table_name = 'TB_CONTENT_DETAIL'
  AND column_name IN ('SHEET_URL', 'AUDIO_URL', 'YOUTUBE_URL')
ORDER BY column_name;

SELECT COUNT(*) AS sheet_url_column
FROM user_tab_columns
WHERE table_name = 'TB_CONTENT_DETAIL'
  AND column_name = 'SHEET_URL';

SELECT COUNT(*) AS audio_url_column
FROM user_tab_columns
WHERE table_name = 'TB_CONTENT_DETAIL'
  AND column_name = 'AUDIO_URL';

SELECT COUNT(*) AS youtube_url_column
FROM user_tab_columns
WHERE table_name = 'TB_CONTENT_DETAIL'
  AND column_name = 'YOUTUBE_URL';

SELECT COUNT(*) AS content_detail_rows
FROM tb_content_detail;

SELECT COUNT(*) AS content_resource_rows
FROM tb_content_resource;

SELECT constraint_name, constraint_type
FROM user_constraints
WHERE table_name = 'TB_CONTENT_RESOURCE'
  AND constraint_name IN (
      'PK_CONTENT_RESOURCE',
      'FK_RES_CONTENT_DETAIL',
      'CK_RES_STATUS',
      'CK_RES_TYPE',
      'CK_RES_FILE_SIZE',
      'CK_RES_DEL',
      'UQ_RES_OBJECT_KEY'
  )
ORDER BY constraint_name;

SELECT index_name, uniqueness
FROM user_indexes
WHERE index_name IN ('UQ_RES_OBJECT_KEY', 'IDX_RES_CONTENT_DETAIL_ID', 'UX_RES_ACTIVE_TYPE')
ORDER BY index_name;

SELECT COUNT(*) AS storage_admission_rows
FROM tb_storage_admission;
