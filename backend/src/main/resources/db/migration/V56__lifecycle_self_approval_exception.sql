-- CE-G01: self-approval block audit persistence.
-- template_lifecycle_record / master_review_record gain a durable self-approval
-- exception marker + reason; content_module_version gains submitted_by so the
-- most recent submitter can be compared against the decision actor (CMP-2/Q1).

ALTER TABLE template_lifecycle_record
    ADD COLUMN IF NOT EXISTS self_approval_exception BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS exception_reason VARCHAR(2048);

ALTER TABLE master_review_record
    ADD COLUMN IF NOT EXISTS self_approval_exception BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS exception_reason VARCHAR(2048);

ALTER TABLE content_module_version
    ADD COLUMN IF NOT EXISTS submitted_by VARCHAR(8);
