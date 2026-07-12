-- ops-paste-binding-seam: non-sensitive paste-cleaning residue on anchor bindings (ADR-0019).
-- Stores counts + messageKeys only; never source HTML / pasted plaintext.

ALTER TABLE anchor_binding
    ADD COLUMN IF NOT EXISTS paste_cleaning_evidence_json TEXT;
