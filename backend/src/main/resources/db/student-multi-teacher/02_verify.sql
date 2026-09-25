-- 배포 후 확인. 모두 기대값이면 마이그레이션이 끝난 것이다.

SELECT COUNT(*) AS missing_student_id
FROM TB_STUDENT_ACCESS_SESSION
WHERE STUDENT_ID IS NULL;

SELECT column_name, nullable
FROM user_tab_columns
WHERE table_name = 'TB_STUDENT_ACCESS_SESSION'
  AND column_name IN ('STUDENT_ID', 'TEACHER_STUDENT_ACCESS_ID')
ORDER BY column_name;

SELECT constraint_name, constraint_type
FROM user_constraints
WHERE constraint_name IN ('FK_SAS_STUDENT', 'PK_STUDENT_LOGIN_VERIFICATION', 'FK_SLV_STUDENT', 'CK_SLV_STATUS');

SELECT index_name
FROM user_indexes
WHERE index_name IN ('IDX_SAS_STUDENT_ID', 'IDX_SLV_STUDENT_ID');

SELECT COUNT(*) AS login_verification_table
FROM user_tables
WHERE table_name = 'TB_STUDENT_LOGIN_VERIFICATION';
