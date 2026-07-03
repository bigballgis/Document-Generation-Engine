# ADR-0045: Generated Artifact Encryption at Rest — Evaluation

**Status:** Accepted (defer implementation)  
**Date:** 2026-07-03  
**Context:** SOR-S09 — MinIO artifacts lack SSE/KMS; optional DOCX password exists per policy.

## Decision

**Defer MinIO SSE/KMS** until bank key-management ownership is assigned (SOR Q3). Continue optional DOCX document-password via `DocxEncryptionService` for policy-bound outputs.

## Rationale

- KMS integration requires intranet HSM/KMS vendor selection (M9 scope).
- Application-layer password encryption covers contractual output protection today.
- MinIO SSE-S3 can be enabled at bucket policy without app changes when keys are ready.

## Follow-up

- M9 key-management slice enables bucket SSE + ADR amendment.
- Re-evaluate when artifact retention crosses compliance threshold.
