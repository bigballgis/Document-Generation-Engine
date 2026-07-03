-- V41: Add temporary file TTL management fields to preview_record
ALTER TABLE preview_record
    ADD COLUMN expires_at          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN temp_storage_key    TEXT,
    ADD COLUMN temp_artifact_cleaned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN error_details       TEXT;
