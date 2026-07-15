-- CE-G03: optional PII classification on template variable schema.

ALTER TABLE variable_schema
    ADD COLUMN pii_category VARCHAR(32) NOT NULL DEFAULT 'NONE';

ALTER TABLE variable_schema
    ADD CONSTRAINT chk_variable_schema_pii_category
        CHECK (pii_category IN (
            'NONE',
            'PERSONAL_NAME',
            'GOVERNMENT_ID',
            'FINANCIAL_ACCOUNT',
            'CONTACT',
            'ADDRESS',
            'OTHER_SENSITIVE'
        ));
