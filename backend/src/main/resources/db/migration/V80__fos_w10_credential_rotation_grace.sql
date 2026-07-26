-- FOS-W10 / ADR-0009: retain previous secret hash for 28-day rotation grace.
ALTER TABLE api_credential
    ADD COLUMN previous_secret_hash VARCHAR(256),
    ADD COLUMN rotation_grace_period_ends_at TIMESTAMP WITH TIME ZONE;
