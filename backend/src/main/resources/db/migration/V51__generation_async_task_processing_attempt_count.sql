ALTER TABLE generation_async_task
    ADD COLUMN processing_attempt_count INTEGER NOT NULL DEFAULT 0;
