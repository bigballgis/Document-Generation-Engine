-- CE-E02: platform shared MinIO asset-library catalog (logical key ≡ storage key).

CREATE TABLE library_asset (
    asset_key            VARCHAR(128)  NOT NULL,
    asset_class          VARCHAR(16)   NOT NULL,
    status               VARCHAR(16)   NOT NULL,
    content_type         VARCHAR(64)   NOT NULL,
    size_bytes           BIGINT        NOT NULL,
    content_sha256       VARCHAR(64)   NOT NULL,
    original_file_name   VARCHAR(512)  NOT NULL,
    uploaded_by          VARCHAR(128)  NOT NULL,
    uploaded_at          TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT pk_library_asset PRIMARY KEY (asset_key),
    CONSTRAINT chk_library_asset_class CHECK (asset_class IN ('IMAGE', 'SEAL', 'OTHER')),
    CONSTRAINT chk_library_asset_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_library_asset_size CHECK (size_bytes >= 1)
);

CREATE INDEX idx_library_asset_status ON library_asset (status);
CREATE INDEX idx_library_asset_class ON library_asset (asset_class);
CREATE INDEX idx_library_asset_uploaded_at ON library_asset (uploaded_at DESC);
