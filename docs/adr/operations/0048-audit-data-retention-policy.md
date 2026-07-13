---
id: ADR-0048
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-05
acceptedDate: 2026-07-11
deciders: architecture, compliance, backend-engineer
owners:
  - operations
  - audit
adrNumber: "0048"
topic: operations
related:
  - docs/adr/operations/0030-operational-platform-baseline.md
  - docs/adr/api-management/0040-api-package-access-and-invocation-retention.md
  - docs/behavior/lrp-d1-audit-retention.md
  - docs/security/permission-matrix.md
  - docs/plan/detail/LRP-D-ops-observability.md
---

# ADR 0048: Audit Data Retention & Archival Policy

## Status

Accepted (2026-07-11) — confirmed-for-D1 baselines from
[lrp-d1-audit-retention.md](../../behavior/lrp-d1-audit-retention.md)
(user authorization 2026-07-11「按你建议继续」; BDD-LRP-D1-001…010).

> **Plan note:** Wave LR-D / LR-D1 plan-row status is owned by `plan-orchestrator`
> on MAIN. This worktree’s `LRP-D-ops-observability.md` copy may lag activation;
> do not treat a stale «Not Started» plan row as overriding this Accepted decision.

## Context

The platform maintains two audit tables that grow unbounded (CD-PIT-15, 2026-07-04):

1. **management_audit_event** — lifecycle events (template create/update/publish, user management, policy changes). Field: `event_at` (Instant).
2. **runtime_generation_audit_event** — API invocation/generation audit records. Field: `event_at` (Instant).

Prior state:

- No retention policy → tables grow indefinitely.
- No cleanup mechanism → storage costs increase, query performance degrades.
- No archival strategy → multi-year compliance retention was stated in docs without an online implementation.

The invocation-record table already has a cleanup pattern (ADR-0040 + `InvocationRetentionCleanupScheduler` + ShedLock), but the audit tables did not.

### Relationship to earlier documents (conflict resolved explicitly)

| Source | Prior wording | Resolution under this ADR |
| --- | --- | --- |
| [permission-matrix.md](../../security/permission-matrix.md) §10 | 「默认保留 5 年」 | **Confirmed Tier-1** = 90/365 hard delete (below). **「5 年」** = **Tier-2 deferred** compliance intent — not Tier-1 PostgreSQL hot retention. |
| [ADR-0030](./0030-operational-platform-baseline.md) | «Database retention 180 days + object storage retention 3 years» | Generic platform baseline row. **For these two audit tables’ Tier-1 online windows, this ADR (0048) is authoritative.** ADR-0030’s Accepted decision body is **not** rewritten here; Tier-2 object-storage archival remains deferred and may later reconcile 3y/5y/7y numbers. |
| ADR-0040 | Invocation record default 90d hard delete | **Pattern source** for disposition + ShedLock; audit tables are a **separate** scheduler and lock names. |

## Decision

Adopt a **two-tier retention policy**. **LR-D1 delivers Tier-1 only.**

### Tier 1: Active Retention (PostgreSQL) — Confirmed for D1

| Table | Retention (confirmed-for-D1) | Disposition | Rationale |
|-------|------------------------------|-------------|-----------|
| `management_audit_event` | **90 days** | **Hard delete** | Mirrors ADR-0040 invocation-record default; operational troubleshooting |
| `runtime_generation_audit_event` | **365 days** | **Hard delete** | API usage trends, support, disputes; bounded (no unlimited retention) |

- **Clock field:** `event_at` (UTC). Cutoff: `nowUtc - retentionDays`. Delete predicate: `event_at < cutoff`. Boundary (`event_at == cutoff`) **retained**.
- **No per-row `retention_days` column** — windows are platform config (unlike V43 package-level invocation retention). Existing `event_at` indexes on V9/V17 are sufficient.
- **Configuration (defaults after D1):**
  - `docgen.audit.management-retention-days` = **90**
  - `docgen.audit.runtime-retention-days` = **365**
  - `docgen.audit.retention-enabled` = **true**
  - `docgen.audit.retention-cron` — daily **03:00** (configurable)
- **Scheduler:** `AuditRetentionCleanupScheduler` (name as implemented); `@Scheduled` + LR-B2 `@SchedulerLock` (prefer dual lock names `audit-retention-cleanup-management` / `audit-retention-cleanup-runtime`, mirroring `InvocationRetentionCleanupScheduler`). Lock not acquired → skip tick (no delete, no evidence).
- **Purge evidence:** each successful purge with `deletedCount > 0` inserts `AUDIT_RETENTION_PURGE` into `management_audit_event` (system actor; table name, retentionDays, cutoff, deletedCount; no sensitive plaintext). Visible to `AUDIT_ADMIN` / `GLOBAL_ADMIN` only; `GROUP_ADMIN` must not see platform-level purge rows (`group_code` null).
- **Self-protection:** purge-evidence rows must not be deleted in the same tick that creates them. Evidence itself remains subject to the management 90-day window on later ticks.
- **Isolation:** do not modify `InvocationRetentionCleanupScheduler` or `api_invocation_record` retention semantics.

### Tier 2: Archive (Object Storage) — Deferred (not D1)

Future enhancement: export records older than Tier-1 retention to object storage (e.g. MinIO Parquet, month-partitioned) for compliance retrieval. Multi-year targets historically stated as 「默认保留 5 年」, ADR-0030’s «3 years object storage», or Basel/SOX 7-year narratives are **pending** for Tier-2 design — **not** Tier-1 online defaults.

## Consequences

- **Positive:** storage cost control; query performance preserved; meets confirmed operational windows; purge actions are themselves auditable.
- **Negative:** Tier-1 records older than the window are permanently deleted until Tier-2 exists; no recovery path for purged hot rows.
- **Neutral:** ops may tune windows via config after monitoring; evidence rows age out under the management window by design.

## Alternatives Considered

- **No retention (status quo):** rejected — unbounded growth, unsustainable.
- **Soft delete (`deleted_at`):** rejected — tables still grow; does not solve storage.
- **Implement 5-year hot retention in PostgreSQL:** rejected for D1 — contradicts confirmed-for-D1 operational baselines; multi-year retention is Tier-2.
- **Partitioning by month + DROP PARTITION:** deferred — revisit when table size exceeds ~10M rows.

## Compliance Mapping

| Requirement | Source | Implementation |
|-------------|--------|----------------|
| Operational bounded growth | CD-PIT-15 / LR-D1 | Tier-1 90/365 hard delete |
| Multi-year / 5-year intent | Prior matrix / PRD wording | **Tier-2 deferred** — not Tier-1 |
| 7-year retention narratives | Basel III, SOX (aspirational) | Tier-2 archive (future) |
| Right to erasure | GDPR | Hard delete (Tier-1) + archive deletion (Tier-2, future) |
| Audit trail integrity | ISO 27001 | Immutable records (no UPDATE; INSERT + DELETE only) |

## Related Documents

- BDD: [lrp-d1-audit-retention.md](../../behavior/lrp-d1-audit-retention.md) (BDD-LRP-D1-001…010)
- Pattern: [0040-api-package-access-and-invocation-retention.md](../api-management/0040-api-package-access-and-invocation-retention.md)
- Permissions: [permission-matrix.md](../../security/permission-matrix.md) §10
- Plan: [LRP-D-ops-observability.md](../../plan/detail/LRP-D-ops-observability.md) § LR-D1
- Platform baseline (generic): [0030-operational-platform-baseline.md](./0030-operational-platform-baseline.md)
