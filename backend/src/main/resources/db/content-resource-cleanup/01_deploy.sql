-- Drop legacy URL columns only after 00_inspect.sql shows both non-null counts are 0.
-- Do not change YOUTUBE_URL.
-- DDL commits. Values stored in the dropped columns cannot be restored.

ALTER TABLE TB_CONTENT_DETAIL DROP COLUMN SHEET_URL;
ALTER TABLE TB_CONTENT_DETAIL DROP COLUMN AUDIO_URL;

COMMIT;
