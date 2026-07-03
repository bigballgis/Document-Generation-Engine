-- P12-API-PKG-T06: Backfill skeleton api_policy for published templates missing policy row.
-- default_route_release_version points at current template release; platform defaults for output/batch/retention.

INSERT INTO api_policy (
    id,
    template_id,
    policy_version,
    allowed_ad_groups,
    default_route_release_version,
    output_formats,
    output_modes,
    batch_enabled,
    max_batch_size,
    batch_sync_max_items,
    batch_async_max_items,
    docx_encryption_enabled,
    pdf_encryption_enabled,
    save_generated_documents,
    invocation_record_retention_days,
    document_retention_days,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    t.id,
    1,
    '[]',
    t.release_version,
    '["DOCX","PDF"]',
    '["SYNC_STREAM","SYNC_DOWNLOAD_URL","ASYNC_TASK"]',
    true,
    100,
    100,
    10000,
    false,
    false,
    true,
    90,
    30,
    'SYSTEM',
    'SYSTEM',
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC'
FROM template t
WHERE t.lifecycle_status = 'PUBLISHED'
  AND t.release_version IS NOT NULL
  AND t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM api_policy p WHERE p.template_id = t.id
  );
