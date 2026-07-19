-- IBL-E2 / ADR-0063: Composition Inclusion Rules on template_version (JSON payload).

ALTER TABLE template_version
    ADD COLUMN composition_inclusion_rules_json TEXT;
