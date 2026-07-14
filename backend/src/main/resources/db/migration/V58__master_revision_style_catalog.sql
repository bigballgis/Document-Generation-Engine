-- CE-K02: durable per-revision master style catalog (styles.xml + theme fonts + docDefaults).

ALTER TABLE master_revision_line
    ADD COLUMN style_catalog_json TEXT NULL;
