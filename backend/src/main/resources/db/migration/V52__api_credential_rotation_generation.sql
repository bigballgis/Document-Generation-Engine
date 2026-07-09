ALTER TABLE api_credential
    ADD COLUMN rotation_generation INTEGER NOT NULL DEFAULT 0;
