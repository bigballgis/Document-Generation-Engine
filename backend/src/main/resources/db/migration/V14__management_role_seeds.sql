-- Wave A: extended management roles and seed accounts for local authentication.

INSERT INTO management_user (id, username, display_name, email, password_hash, auth_source)
VALUES
    ('11111111-1111-1111-1111-111111111104', '10000004', 'Audit Admin', 'audit.admin@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL'),
    ('11111111-1111-1111-1111-111111111105', '10000005', 'Master Designer', 'master.designer@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL'),
    ('11111111-1111-1111-1111-111111111106', '10000006', 'Template Tester', 'template.tester@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL'),
    ('11111111-1111-1111-1111-111111111107', '10000007', 'Template Approver', 'template.approver@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL');

INSERT INTO management_user_role (user_id, role) VALUES
    ('11111111-1111-1111-1111-111111111104', 'AUDIT_ADMIN'),
    ('11111111-1111-1111-1111-111111111105', 'MASTER_DESIGNER'),
    ('11111111-1111-1111-1111-111111111106', 'TEMPLATE_TESTER'),
    ('11111111-1111-1111-1111-111111111107', 'TEMPLATE_APPROVER');

INSERT INTO management_user_group_scope (user_id, group_code) VALUES
    ('11111111-1111-1111-1111-111111111105', 'RETAIL'),
    ('11111111-1111-1111-1111-111111111106', 'RETAIL'),
    ('11111111-1111-1111-1111-111111111107', 'RETAIL');
