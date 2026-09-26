-- Full deployment rollback. Not for partial deployment recovery.
-- 이번 migration에서 만든 object만 제거한다.
-- TB_CONTENT_DETAIL의 SHEET_URL, AUDIO_URL, YOUTUBE_URL과 기존 행은 바꾸지 않는다.
-- TB_CONTENT_RESOURCE에 행이 있으면 그 메타데이터도 함께 사라진다. OCI object는 지우지 않는다.

DROP INDEX UX_RES_ACTIVE_TYPE;
DROP TABLE TB_CONTENT_RESOURCE;
DROP SEQUENCE SEQ_CONTENT_RESOURCE;

DROP TABLE TB_STORAGE_ADMISSION;

COMMIT;
