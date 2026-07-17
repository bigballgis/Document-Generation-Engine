-- ADR-0042 / Task #103: author-declared Word page count + measured PDF pages on preview.

ALTER TABLE template_version
    ADD COLUMN author_word_page_count INTEGER;

ALTER TABLE preview_record
    ADD COLUMN pdf_page_count INTEGER;

COMMENT ON COLUMN template_version.author_word_page_count IS
    'Microsoft Word authoring page count declared by template author; never backfilled from LO/PDF.';

COMMENT ON COLUMN preview_record.pdf_page_count IS
    'Measured PDF page count after successful preview conversion (ADR-0042 publish-gate input).';
