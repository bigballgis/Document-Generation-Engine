-- P18-T08: publish-locked render profile on template versions and preview records.

ALTER TABLE template_version
    ADD COLUMN render_profile_version VARCHAR(32),
    ADD COLUMN render_profile_json TEXT;

ALTER TABLE preview_record
    ADD COLUMN render_profile_version VARCHAR(32);
