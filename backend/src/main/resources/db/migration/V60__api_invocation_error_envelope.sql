-- CE-U11: persist unified error envelope on failed invocation records for troubleshooting.

ALTER TABLE api_invocation_record
    ADD COLUMN error_code VARCHAR(128),
    ADD COLUMN error_category VARCHAR(64),
    ADD COLUMN error_message_key VARCHAR(256),
    ADD COLUMN error_retryable BOOLEAN,
    ADD COLUMN error_message VARCHAR(1024);
