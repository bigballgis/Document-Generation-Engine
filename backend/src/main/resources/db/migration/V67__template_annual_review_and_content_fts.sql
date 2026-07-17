-- CE-G05: template annual-review due date + content-module version tsvector FTS.

ALTER TABLE template
    ADD COLUMN next_review_due DATE;

COMMENT ON COLUMN template.next_review_due IS
    'CE-G05 UTC calendar date for next annual review; null until first PUBLISHED seed or complete';

ALTER TABLE content_module_version
    ADD COLUMN content_search_vector tsvector;

COMMENT ON COLUMN content_module_version.content_search_vector IS
    'CE-G05 searchable text vector (config simple) derived from content_structure_json';

CREATE INDEX idx_cmv_content_search_vector
    ON content_module_version
    USING GIN (content_search_vector);

-- Bootstrap: index raw JSON text; subsequent writes use extracted human-readable text.
UPDATE content_module_version
SET content_search_vector = to_tsvector('simple', coalesce(content_structure_json, ''))
WHERE content_search_vector IS NULL;
