-- P3 extension: computed variable expressions on template variable schema.

ALTER TABLE variable_schema
    ADD COLUMN compute_expression TEXT NULL;
