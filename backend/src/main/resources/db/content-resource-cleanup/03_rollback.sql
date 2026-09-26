-- Adds the legacy columns back as empty nullable columns.
-- Values that existed before DROP COLUMN cannot be recovered.
-- This rollback assumes 00_inspect.sql showed SHEET_URL and AUDIO_URL non-null counts of 0.
-- YOUTUBE_URL is not changed.

ALTER TABLE TB_CONTENT_DETAIL ADD (
    SHEET_URL VARCHAR2(2000),
    AUDIO_URL VARCHAR2(2000)
);

COMMIT;
