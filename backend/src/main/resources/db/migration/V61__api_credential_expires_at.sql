-- CE-C04: Persist API credential expiry (ADR-0009).
-- Backfill existing rows as created_at + 180 days, then enforce NOT NULL.

ALTER TABLE api_credential
    ADD COLUMN expires_at TIMESTAMPTZ;

UPDATE api_credential
SET expires_at = created_at + INTERVAL '180 days'
WHERE expires_at IS NULL;

ALTER TABLE api_credential
    ALTER COLUMN expires_at SET NOT NULL;
