-- P2: business groups for master/template isolation.

CREATE TABLE business_group (
    id UUID PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_business_group_code UNIQUE (group_code)
);

INSERT INTO business_group (id, group_code, display_name) VALUES
    ('22222222-2222-2222-2222-222222222201', 'RETAIL', 'Retail Banking'),
    ('22222222-2222-2222-2222-222222222202', 'CORP', 'Corporate Banking');
