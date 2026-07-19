-- IBL-E3 / ADR-0064: package-level approval matrix mode + LEGAL_REVIEWER seed.

ALTER TABLE template
    ADD COLUMN approval_matrix_mode VARCHAR(32);

UPDATE template
SET approval_matrix_mode = 'SINGLE_TRACK'
WHERE approval_matrix_mode IS NULL;

ALTER TABLE template
    ALTER COLUMN approval_matrix_mode SET NOT NULL;

-- Note: …108 / 10000008 is CORP-only TEMPLATE_AUTHOR (V34). Legal reviewer uses …109 / 10000009.
INSERT INTO management_user (id, username, display_name, email, password_hash, auth_source)
VALUES
    ('11111111-1111-1111-1111-111111111109', '10000009', 'Legal Reviewer', 'legal.reviewer@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL');

INSERT INTO management_user_role (user_id, role) VALUES
    ('11111111-1111-1111-1111-111111111109', 'LEGAL_REVIEWER');

INSERT INTO management_user_group_scope (user_id, group_code) VALUES
    ('11111111-1111-1111-1111-111111111109', 'RETAIL');
