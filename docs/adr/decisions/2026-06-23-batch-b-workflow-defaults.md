# Confirmed decisions — Batch B (2026-06-23)

User authorized **recommended defaults** for unattended remediation.

| ID | Decision | Rationale |
| --- | --- | --- |
| COR-T11 | **Dashboard consolidation** — role queues live in `DashboardView` task table; legacy workbench views removed; URL redirects to `/dashboard` retained | Avoid duplicate dead UI; grouped queue UX deferred to COR-T12 |
| COR-B01 | **De-scope runtime v1** — keep enum; reject `SYNC_DOWNLOAD_URL` at runtime until dedicated slice (ADR 0038) | Avoid half-implemented download URL contract |
| COR-T07 | **Publish permission** — code truth: `GLOBAL_ADMIN` + `GROUP_ADMIN` only; domain/PRD author-publish wording superseded by permission matrix | Fail-closed admin publish aligns with bank governance |
| COR-B10 | **Rate limit v1** — process-local Bucket4j documented as single-instance seam; shared limiter deferred to OPT-F8 / multi-instance ADR | Docker single-replica validation path |

These decisions are durable for plan/docs sync; reopen only via explicit user confirmation.
