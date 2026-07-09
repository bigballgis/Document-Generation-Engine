ALTER TABLE template_version
    ADD COLUMN IF NOT EXISTS fidelity_warning_codes_json TEXT;
