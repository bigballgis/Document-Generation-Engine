-- FOS-W6-4: comparable numeric semver components so 1.10 > 1.9 (not lexicographic).
ALTER TABLE content_module_version
    ADD COLUMN version_major INT NOT NULL DEFAULT 0,
    ADD COLUMN version_minor INT NOT NULL DEFAULT 0,
    ADD COLUMN version_patch INT NOT NULL DEFAULT 0;

UPDATE content_module_version
SET
    version_major = COALESCE(
        NULLIF(
            split_part(regexp_replace(semantic_version, '[^0-9.].*$', ''), '.', 1),
            ''
        )::INT,
        0
    ),
    version_minor = COALESCE(
        NULLIF(
            split_part(regexp_replace(semantic_version, '[^0-9.].*$', ''), '.', 2),
            ''
        )::INT,
        0
    ),
    version_patch = COALESCE(
        NULLIF(
            split_part(regexp_replace(semantic_version, '[^0-9.].*$', ''), '.', 3),
            ''
        )::INT,
        0
    );

CREATE INDEX idx_content_module_version_semver_numeric
    ON content_module_version (module_id, version_major DESC, version_minor DESC, version_patch DESC);
