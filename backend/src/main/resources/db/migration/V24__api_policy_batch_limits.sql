-- P17-T02: Separate sync/async batch limit columns; keep max_batch_size for back-compat.

ALTER TABLE api_policy
    ADD COLUMN batch_sync_max_items INT NOT NULL DEFAULT 100,
    ADD COLUMN batch_async_max_items INT NOT NULL DEFAULT 10000;

UPDATE api_policy SET batch_sync_max_items = max_batch_size;
