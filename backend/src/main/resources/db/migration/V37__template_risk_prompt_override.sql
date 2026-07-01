CREATE TABLE template_risk_prompt_override (
    template_id             UUID PRIMARY KEY REFERENCES template(id),
    reason_categories_json  TEXT         NOT NULL,
    risk_prompt_copy_json   TEXT         NOT NULL,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL
);
