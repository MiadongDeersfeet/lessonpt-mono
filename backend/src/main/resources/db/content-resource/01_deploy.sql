-- Run this file once, from the first statement through COMMIT.
-- Do not drop SHEET_URL, AUDIO_URL, or YOUTUBE_URL in this script.
--
-- Later replace flow: soft-delete the active resource, then INSERT the new one.
-- Inserting the new active row first conflicts with UX_RES_ACTIVE_TYPE.
--
-- TB_STORAGE_ADMISSION is a lock row, not a usage table.
-- It stores no byte total. A later upload locks ADMISSION_ID = 1,
-- then compares the active FILE_SIZE sum with the 7GB admission limit.
-- That sum is not OCI bucket usage. Orphans can sit outside the sum.
-- Warning 6GB. Admission limit 7GB. OCI hard quota 8GB. OCI quota is the final stop.

CREATE SEQUENCE SEQ_CONTENT_RESOURCE START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE TB_CONTENT_RESOURCE (
    RESOURCE_ID          NUMBER(19)     NOT NULL,
    CONTENT_DETAIL_ID    NUMBER(19)     NOT NULL,
    RESOURCE_TYPE        VARCHAR2(10)   NOT NULL,
    ORIGINAL_FILE_NAME   VARCHAR2(255)  NOT NULL,
    OBJECT_KEY           VARCHAR2(1024) NOT NULL,
    CONTENT_TYPE         VARCHAR2(100)  NOT NULL,
    FILE_SIZE            NUMBER(12)     NOT NULL,
    STATUS               CHAR(1)        NOT NULL,
    CREATED_AT           TIMESTAMP(6)   NOT NULL,
    UPDATED_AT           TIMESTAMP(6)   NULL,
    DELETED_AT           TIMESTAMP(6)   NULL,
    CONSTRAINT PK_CONTENT_RESOURCE PRIMARY KEY (RESOURCE_ID),
    CONSTRAINT FK_RES_CONTENT_DETAIL FOREIGN KEY (CONTENT_DETAIL_ID)
        REFERENCES TB_CONTENT_DETAIL (CONTENT_DETAIL_ID),
    CONSTRAINT CK_RES_STATUS CHECK (STATUS IN ('Y', 'N')),
    CONSTRAINT CK_RES_TYPE CHECK (RESOURCE_TYPE IN ('SHEET', 'AUDIO')),
    CONSTRAINT CK_RES_FILE_SIZE CHECK (FILE_SIZE > 0),
    CONSTRAINT CK_RES_DEL CHECK (
        (STATUS = 'Y' AND DELETED_AT IS NULL) OR
        (STATUS = 'N' AND DELETED_AT IS NOT NULL)
    ),
    CONSTRAINT UQ_RES_OBJECT_KEY UNIQUE (OBJECT_KEY)
);

CREATE INDEX IDX_RES_CONTENT_DETAIL_ID ON TB_CONTENT_RESOURCE (CONTENT_DETAIL_ID);

-- Inactive rows make both CASE results NULL, so they are not indexed.
-- One active SHEET and one active AUDIO per content detail.
CREATE UNIQUE INDEX UX_RES_ACTIVE_TYPE ON TB_CONTENT_RESOURCE (
    (CASE WHEN STATUS = 'Y' AND DELETED_AT IS NULL THEN CONTENT_DETAIL_ID END),
    (CASE WHEN STATUS = 'Y' AND DELETED_AT IS NULL THEN RESOURCE_TYPE END)
);

-- One row so concurrent uploads cannot both pass the 7GB check.
-- No usage column.
CREATE TABLE TB_STORAGE_ADMISSION (
    ADMISSION_ID    NUMBER(19)    NOT NULL,
    CONSTRAINT PK_STORAGE_ADMISSION PRIMARY KEY (ADMISSION_ID),
    CONSTRAINT CK_STORAGE_ADMISSION_ID CHECK (ADMISSION_ID = 1)
);

INSERT INTO TB_STORAGE_ADMISSION (ADMISSION_ID) VALUES (1);

COMMIT;
