---
id: ADR-0048
title: Audit Data Retention & Archival Policy
status: Proposed
date: 2026-07-05
deciders: architecture, compliance, backend-engineer
related:
  - docs/adr/operations/0030-operational-platform-baseline.md
  - docs/adr/api-management/0040-api-package-access-and-invocation-retention.md
  - docs/plan/detail/LRP-D-ops-observability.md
---

# ADR-0048 — Audit Data Retention & Archival Policy

## Context

The platform maintains two audit tables that grow unbounded (CD-PIT-15, 2026-07-04):

1. **management_audit_event** — lifecycle events (template create/update/publish, user management, policy changes). Field: `event_at` (Instant).
2. **runtime_generation_audit_event** — API invocation records (generation requests, document downloads). Field: `event_at` (Instant).

Current state:

- No retention policy → tables grow indefinitely.
- No cleanup mechanism → storage costs increase, query performance degrades.
- No archival strategy → historical data inaccessible after migration to new schema versions.

The invocation-record table already has a cleanup pattern (ADR-0040 + `InvocationRetentionCleanupScheduler` + ShedLock), but the audit tables do not.

## Decision

Adopt a **two-tier retention policy** with automated cleanup. The baselines below are **pending
user confirmation** per the document-as-code constitution — until confirmed, the scheduler is
off by default (`docgen.audit.retention-enabled=false`).

### Tier 1: Active Retention (PostgreSQL)

| Table | Retention (proposed) | Rationale |
|-------|----------------------|-----------|
| management_audit_event | 90 days | Operational troubleshooting, recent lifecycle tracking |
| runtime_generation_audit_event | 365 days | API usage trends, customer support, billing disputes |

Cleanup runs daily at 03:00 (configurable via `docgen.audit.retention-cron`), deleting records
older than the retention window. Guarded by the LR-B2 ShedLock mutex so only one replica runs it.

### Tier 2: Archive (Object Storage) — NOT in v1

Future enhancement: export records older than Tier 1 retention to MinIO as Parquet, partitioned
by month, queryable via Athena/Redshift Spectrum for compliance audits. Retain indefinitely.

## Consequences

- **Positive:** storage cost control; query performance preserved; meets operational needs.
- **Negative:** records older than the window are permanently deleted (no recovery until Tier 2).
- **Neutral:** 90/365 are proposed baselines; tune after monitoring actual usage.

## Implementation Notes

- **Flyway V47**: adds `retention_days` column to both audit tables (default 90/365).
- **`AuditRetentionCleanupScheduler`**: daily `@Scheduled` + `@SchedulerLock("auditRetentionCleanup")`.
- **Configuration:** `docgen.audit.management-retention-days` / `docgen.audit.runtime-retention-days` /
  `docgen.audit.retention-enabled` / `docgen.audit.retention-cron`.
- **Monitoring:** log deleted counts; future Micrometer counter `audit.retention.deleted`.

## Alternatives Considered

- **No retention (status quo):** rejected — unbounded growth, unsustainable beyond ~2 years.
- **Soft delete (`deleted_at`):** rejected — tables still grow, doesn't solve storage.
- **Partitioning by month + DROP PARTITION:** deferred — revisit when table size exceeds 10M rows.

## Compliance Mapping

| Requirement | Source | Implementation |
|-------------|--------|----------------|
| 7-year retention | Basel III, SOX | Tier 2 archive (future) |
| Right to erasure | GDPR | Hard delete (Tier 1) + archive deletion (Tier 2) |
| Audit trail integrity | ISO 27001 | Immutable records (no UPDATE, only INSERT/DELETE) |
