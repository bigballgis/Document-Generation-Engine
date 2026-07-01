-- CORP-scoped template author for cross-group isolation E2E (BDD-TEMPLATE-PACKAGE-NAV-001 S6).

INSERT INTO management_user (id, username, display_name, email, password_hash, auth_source)
VALUES
    ('11111111-1111-1111-1111-111111111108', '10000008', 'Corporate Template Author', 'corp.author@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL');

INSERT INTO management_user_role (user_id, role) VALUES
    ('11111111-1111-1111-1111-111111111108', 'TEMPLATE_AUTHOR');

INSERT INTO management_user_group_scope (user_id, group_code) VALUES
    ('11111111-1111-1111-1111-111111111108', 'CORP');
