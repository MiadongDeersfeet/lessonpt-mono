-- One check per statement. Compare the TB_CONTENT_DETAIL counts with 00_inspect.sql.

SELECT COUNT(*) AS content_resource_table
FROM user_tables
WHERE table_name = 'TB_CONTENT_RESOURCE';

SELECT column_name, data_type, data_precision, data_scale, data_length, nullable
FROM user_tab_columns
WHERE table_name = 'TB_CONTENT_RESOURCE'
ORDER BY column_id;

SELECT constraint_name, constraint_type, delete_rule, search_condition_vc
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

SELECT index_name, uniqueness, index_type
FROM user_indexes
WHERE index_name IN ('UQ_RES_OBJECT_KEY', 'IDX_RES_CONTENT_DETAIL_ID', 'UX_RES_ACTIVE_TYPE')
ORDER BY index_name;

SELECT index_name, column_position, column_expression
FROM user_ind_expressions
WHERE index_name = 'UX_RES_ACTIVE_TYPE'
ORDER BY column_position;

SELECT sequence_name, increment_by, cache_size
FROM user_sequences
WHERE sequence_name = 'SEQ_CONTENT_RESOURCE';

SELECT COUNT(*) AS storage_admission_rows
FROM tb_storage_admission;

SELECT column_name, data_type, data_length, nullable
FROM user_tab_columns
WHERE table_name = 'TB_CONTENT_DETAIL'
  AND column_name IN ('SHEET_URL', 'AUDIO_URL', 'YOUTUBE_URL')
ORDER BY column_name;

SELECT COUNT(*) AS content_detail_rows
FROM tb_content_detail;

SELECT COUNT(*) AS active_content_details
FROM tb_content_detail
WHERE status = 'Y'
  AND deleted_at IS NULL;

SELECT COUNT(*) AS sheet_url_set
FROM tb_content_detail
WHERE sheet_url IS NOT NULL;

SELECT COUNT(*) AS audio_url_set
FROM tb_content_detail
WHERE audio_url IS NOT NULL;

SELECT COUNT(*) AS youtube_url_set
FROM tb_content_detail
WHERE youtube_url IS NOT NULL;
