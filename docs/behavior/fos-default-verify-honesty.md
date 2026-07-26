# Behavior: FOS-W13 Default Verify Honesty

**Status:** Confirmed for delivery  
**Traceability:** TM #183 · `fos-default-verify-honesty` · W13-1…W13-8

## Goal

Default/CI verification lanes tell the truth about Postgres/Flyway, SYNTHETIC PDF harness
limits, nested loop/condition corpus, MinIO/Redis/FTS on real Testcontainers, and package
coverage floors.

## Acceptance

- W13-1 Flyway migrate + core table presence on Postgres; CI `-Ptestcontainers` job
- W13-2 SYNTHETIC PDFs labeled `harnessSelfTest` (not product PDF proof)
- W13-3 QR/attachment/cross-page `productPdf: pending-CRCH-W5`
- W13-4 condition-inside-loop + empty-loop-collection corpus; empty list emits nothing
- W13-5 MinIO put/get/delete Testcontainers
- W13-6 Redis idempotency + rate-limit Testcontainers
- W13-7 Postgres FTS match smoke
- W13-8 JaCoCo PACKAGE floors for authoring/apimgmt/template

## Deploy honesty

Lab Docker may still lack app images; Testcontainers pulls postgres/redis/minio as needed.
