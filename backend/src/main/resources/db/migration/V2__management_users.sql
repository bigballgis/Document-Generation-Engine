-- P1: management users, roles, group scope, and seed accounts for local authentication.

CREATE TABLE management_user (
    id UUID PRIMARY KEY,
    username VARCHAR(8) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    email VARCHAR(256) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    auth_source VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_management_user_username UNIQUE (username)
);

CREATE TABLE management_user_role (
    user_id UUID NOT NULL REFERENCES management_user (id),
    role VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE management_user_group_scope (
    user_id UUID NOT NULL REFERENCES management_user (id),
    group_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, group_code)
);

INSERT INTO management_user (id, username, display_name, email, password_hash, auth_source)
VALUES
    ('11111111-1111-1111-1111-111111111101', '10000001', 'Global Admin', 'global.admin@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL'),
    ('11111111-1111-1111-1111-111111111102', '10000002', 'Group Admin', 'group.admin@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL'),
    ('11111111-1111-1111-1111-111111111103', '10000003', 'Template Author', 'author@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$asNOSucJDAd64zDnC/x9Lg$XkCeMZfHtYtrfa05C8CV1NiDv53nC1SluF6eZXOUwsw', 'LOCAL');

INSERT INTO management_user_role (user_id, role) VALUES
    ('11111111-1111-1111-1111-111111111101', 'GLOBAL_ADMIN'),
    ('11111111-1111-1111-1111-111111111102', 'GROUP_ADMIN'),
    ('11111111-1111-1111-1111-111111111103', 'TEMPLATE_AUTHOR');

INSERT INTO management_user_group_scope (user_id, group_code) VALUES
    ('11111111-1111-1111-1111-111111111102', 'RETAIL'),
    ('11111111-1111-1111-1111-111111111102', 'CORP'),
    ('11111111-1111-1111-1111-111111111103', 'RETAIL');
