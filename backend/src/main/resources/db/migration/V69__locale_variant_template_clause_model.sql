-- IBL-E1 / ADR-0061: body locale + optional locale variant family on template and content_module.

ALTER TABLE template
    ADD COLUMN locale VARCHAR(64),
    ADD COLUMN locale_variant_family_id UUID;

ALTER TABLE content_module
    ADD COLUMN locale VARCHAR(64),
    ADD COLUMN locale_variant_family_id UUID;

-- Migration default matches ComputeDslLimits.DEFAULT_LOCALE (documented as migration label, not historical business declaration).
UPDATE template SET locale = 'zh-CN' WHERE locale IS NULL;
UPDATE content_module SET locale = 'zh-CN' WHERE locale IS NULL;

ALTER TABLE template
    ALTER COLUMN locale SET NOT NULL;

ALTER TABLE content_module
    ALTER COLUMN locale SET NOT NULL;

CREATE UNIQUE INDEX uq_template_group_family_locale
    ON template (group_code, locale_variant_family_id, locale)
    WHERE deleted_at IS NULL AND locale_variant_family_id IS NOT NULL;

CREATE UNIQUE INDEX uq_content_module_group_family_locale
    ON content_module (group_code, locale_variant_family_id, locale)
    WHERE deleted_at IS NULL AND locale_variant_family_id IS NOT NULL;

CREATE INDEX idx_template_locale ON template (locale);
CREATE INDEX idx_content_module_locale ON content_module (locale);
CREATE INDEX idx_template_locale_variant_family ON template (locale_variant_family_id);
CREATE INDEX idx_content_module_locale_variant_family ON content_module (locale_variant_family_id);
