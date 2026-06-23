-- Wave B: persist composition rules on the dev template version.

ALTER TABLE template_version
    ADD COLUMN rules_json TEXT NULL;
