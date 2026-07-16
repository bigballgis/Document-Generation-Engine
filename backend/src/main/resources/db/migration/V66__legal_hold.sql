-- CE-G04: legal hold entity + invocation set membership (retention exemption).

CREATE TABLE legal_hold (
    id                     UUID          NOT NULL,
    hold_external_id       VARCHAR(64)   NOT NULL,
    scope_type             VARCHAR(32)   NOT NULL,
    status                 VARCHAR(16)   NOT NULL,
    reason                 VARCHAR(512),
    template_id            UUID,
    template_external_id   VARCHAR(128),
    effective_from         TIMESTAMPTZ,
    effective_to           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL,
    created_by_username    VARCHAR(8)    NOT NULL,
    released_at            TIMESTAMPTZ,
    released_by_username   VARCHAR(8),
    CONSTRAINT pk_legal_hold PRIMARY KEY (id),
    CONSTRAINT uq_legal_hold_external_id UNIQUE (hold_external_id),
    CONSTRAINT chk_legal_hold_scope_type CHECK (scope_type IN ('TEMPLATE_WINDOW', 'INVOCATION_SET')),
    CONSTRAINT chk_legal_hold_status CHECK (status IN ('ACTIVE', 'RELEASED'))
);

CREATE INDEX idx_legal_hold_status ON legal_hold (status);
CREATE INDEX idx_legal_hold_template_id ON legal_hold (template_id);

CREATE TABLE legal_hold_invocation (
    hold_id                   UUID         NOT NULL,
    invocation_external_id    VARCHAR(64)  NOT NULL,
    CONSTRAINT pk_legal_hold_invocation PRIMARY KEY (hold_id, invocation_external_id),
    CONSTRAINT fk_legal_hold_invocation_hold
        FOREIGN KEY (hold_id) REFERENCES legal_hold (id)
);

CREATE INDEX idx_legal_hold_invocation_external_id
    ON legal_hold_invocation (invocation_external_id);
