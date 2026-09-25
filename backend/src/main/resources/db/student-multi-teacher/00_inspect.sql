-- 실행 전 현재 제약을 확인한다. 결과를 보고 01_deploy.sql의 신규 이름과 겹치지 않는지 본다.
-- 이 스크립트는 데이터를 바꾸지 않는다.

SELECT index_name, uniqueness, column_name
FROM user_ind_columns
WHERE table_name IN ('TB_STUDENT_ACCESS_SESSION', 'TB_STUDENT_EMAIL_VERIFICATION')
ORDER BY table_name, index_name, column_position;

SELECT constraint_name, constraint_type
FROM user_constraints
WHERE table_name IN ('TB_STUDENT_ACCESS_SESSION', 'TB_STUDENT_EMAIL_VERIFICATION', 'TB_STUDENT')
ORDER BY table_name, constraint_name;

SELECT column_name, nullable, data_type
FROM user_tab_columns
WHERE table_name = 'TB_STUDENT_ACCESS_SESSION'
ORDER BY column_id;
