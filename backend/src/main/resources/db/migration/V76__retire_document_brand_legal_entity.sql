-- ADR-0071 / SYS-NORM Wave 6 (BDD-SYS-NORM-D1-011):
-- Durable hard delete of DocumentBrand / LegalEntity product persistence.
-- Letterhead/logo/seal governance remains via Letterhead (master).

DROP TABLE IF EXISTS legal_entity;
DROP TABLE IF EXISTS document_brand;

UPDATE business_group
SET default_legal_entity_code = NULL
WHERE default_legal_entity_code IS NOT NULL;

ALTER TABLE business_group
    DROP COLUMN IF EXISTS default_legal_entity_code;

UPDATE template
SET allowed_document_brand_codes_json = NULL
WHERE allowed_document_brand_codes_json IS NOT NULL;
