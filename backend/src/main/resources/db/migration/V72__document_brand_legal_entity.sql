-- IBL-E4 / ADR-0065: group-scoped DocumentBrand + LegalEntity catalogs (≠ UI BrandPreset).

ALTER TABLE business_group
    ADD COLUMN default_legal_entity_code VARCHAR(64);

ALTER TABLE template
    ADD COLUMN allowed_document_brand_codes_json TEXT;

CREATE TABLE document_brand (
    id                       UUID         NOT NULL,
    group_code               VARCHAR(64)  NOT NULL,
    document_brand_code      VARCHAR(64)  NOT NULL,
    display_name             VARCHAR(256) NOT NULL,
    status                   VARCHAR(16)  NOT NULL,
    logo_object_ref          VARCHAR(256) NOT NULL,
    default_seal_object_ref  VARCHAR(256),
    letterhead_legal_name    VARCHAR(256),
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    deleted_at               TIMESTAMPTZ,
    CONSTRAINT pk_document_brand PRIMARY KEY (id),
    CONSTRAINT chk_document_brand_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_document_brand_group_code
    ON document_brand (group_code, document_brand_code)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_document_brand_group_status
    ON document_brand (group_code, status)
    WHERE deleted_at IS NULL;

CREATE TABLE legal_entity (
    id                   UUID         NOT NULL,
    group_code           VARCHAR(64)  NOT NULL,
    legal_entity_code    VARCHAR(64)  NOT NULL,
    display_name         VARCHAR(256) NOT NULL,
    status               VARCHAR(16)  NOT NULL,
    document_brand_code  VARCHAR(64)  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT pk_legal_entity PRIMARY KEY (id),
    CONSTRAINT chk_legal_entity_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_legal_entity_group_code
    ON legal_entity (group_code, legal_entity_code)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_legal_entity_group_status
    ON legal_entity (group_code, status)
    WHERE deleted_at IS NULL;

-- Seed PLATFORM_DEFAULT document brand for every existing business group (not UI REDBC/GREENBC).
INSERT INTO document_brand (
    id,
    group_code,
    document_brand_code,
    display_name,
    status,
    logo_object_ref,
    default_seal_object_ref,
    letterhead_legal_name,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    gen_random_uuid(),
    bg.group_code,
    'PLATFORM_DEFAULT',
    'Platform default document brand',
    'ACTIVE',
    'platform/document-brands/PLATFORM_DEFAULT/logo',
    NULL,
    NULL,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    NULL
FROM business_group bg
WHERE bg.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM document_brand db
      WHERE db.group_code = bg.group_code
        AND db.document_brand_code = 'PLATFORM_DEFAULT'
        AND db.deleted_at IS NULL
  );
