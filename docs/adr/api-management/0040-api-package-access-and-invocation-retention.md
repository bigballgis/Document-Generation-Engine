---
id: ADR-0040
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api-management
	- api
adrNumber: "0040"
topic: api-management
related:
	- docs/behavior/api-package-access-and-invocation-records.md
	- docs/behavior/sys-norm-hub-ia.md
	- docs/behavior/sys-norm-external-ops.md
	- docs/adr/api-management/0002-api-management-template-scope.md
	- docs/adr/api-management/0016-api-management-ui-and-audit-format.md
	- docs/adr/api/0004-api-idempotency-strategy.md
	- docs/adr/api/0005-api-response-delivery-and-download-security.md
	- docs/api/contract-outline.md
	- docs/domain/domain-model.md
---

# ADR 0040: Package-First API Access and Invocation Retention

## Status

Accepted (2026-07-03)

## Context

API management was originally modeled as a standalone template catalog with manual policy creation. Product review confirmed that API configuration is **package-scoped editing**, not a separate onboarding workflow: a published template package should expose callable paths by default, and administrators configure access on the **template package hub** (convention-over-configuration L1 + advanced collapsed domains).

Callers also need durable **invocation records** (parameters + optional artifacts) for reconciliation and backup, distinct from compliance audit summaries. Retention must be configurable per package without breaking the existing **7-day idempotency window** (ADR 0004) or **15-minute download URL TTL** (ADR 0005).

## Decision

### Package-first configuration surface

- The **primary** API management surface is the template package hub **External access** tab (`对外接入`), not a standalone API policy catalog list.
- The standalone API policy home remains a **cross-package monitoring / alert** entry only; deep links redirect to the package hub tab.
- Configuration domains (AD Group, output policy, batch limits, encryption capability, default route, **invocation retention**) remain template-level and immediate-only per ADR 0007; UI structure in ADR 0016 applies within the hub tab (L1 essentials + advanced collapsed areas).

> **Navigation surface superseded** by [Amendment — 2026-07-21](#amendment--2026-07-21-navigation-surface-sys-norm-wave-2) (package settings route). Package-first / non-catalog decision **unchanged**.

### Auto-materialize `api_policy`

- When a template package enters **`PENDING_RELEASE`**, the platform creates a **skeleton** `api_policy` row (platform defaults; empty AD Group; no default route target yet).
- On **first publish**, the platform sets `defaultRouteReleaseVersion` to the released version and exposes both **default** and **explicit** generate paths without requiring a pre-existing manual policy row.
- On **subsequent publish**, the platform **does not silently change** the default route; new releases add explicit-version paths only. Default route changes follow governed `DEFAULT_ROUTE_TARGET` change flow (impact preview + audit).
- Template import must **not** silently clear an existing `defaultRouteReleaseVersion`.

### Invocation records

- Each runtime generate / batch / async acceptance writes an **`api_invocation_record`** row scoped to the calling **API credential**.
- **`IDEMPOTENCY_REPLAYED`** does **not** create a duplicate invocation row; callers resolve the original record via `idempotencyKey` or `requestId`.
- Stored parameters **must not** include encryption password plaintext (`openPassword`, `ownerPassword`); only sanitized encryption metadata is persisted.
- **Sensitive-data basis (ADR-0057, 2026-07-16):** sanitized template variable persistence in `parameters_storage` is an **authorized retention-scoped exception** to ADR-0020’s general plaintext-persistence ban, for caller reconciliation and CE-G06 regenerate replay. Management APIs must still never expose variables. Column encryption-at-rest is deferred.
- Caller-facing query:
  - `GET /api/{environment}/v1/templates/{templateId}/invocations` with `view=logical|flat` (default `logical`) and optional `requestId` filter.
  - `GET …/invocations/{invocationId}` for detail including full sanitized parameters.
- **`logical`** view: `SINGLE`, `BATCH_ROOT`, `ASYNC_TASK`.
- **`flat`** view: `SINGLE`, `BATCH_ITEM` only (excludes `BATCH_ROOT`).
- Package hub L2 shows **read-only invocation summaries** for administrators (no variable plaintext); compliance detail remains in the audit console.

### Four-layer retention clock

| Layer | Scope | Default | Configurable |
| --- | --- | --- | --- |
| Download URL | Per issued URL | 15 minutes | No (ADR 0005) |
| Idempotency record | Per idempotency key | 7 days | No (ADR 0004) |
| Document artifact | MinIO/DB when `saveGeneratedDocuments=true` | 30 days | Yes — `documentRetentionDays` (max 365) |
| Invocation record | DB row | 90 days | Yes — `invocationRecordRetentionDays` (max 2555) |

- **`saveGeneratedDocuments`** defaults to **true**. When **false**, no artifact is stored; invocation rows may still exist for parameter audit; download returns **410** when no artifact.
- **Record retention may exceed document retention** (C8): after document expiry, callers may still query parameters; download returns **410**.
- Package retention changes affect **new invocations only**; recorded under `changedAreas` value **`INVOCATION_RETENTION`** (extends ADR 0016 baseline set).

### Relationship to existing ADRs

- ADR **0004** idempotency **7-day** retention is unchanged.
- ADR **0005** **15-minute** download URL TTL is unchanged; when `saveGeneratedDocuments=true` and artifact TTL exceeds idempotency TTL, re-download after idempotency expiry uses invocation/document retention rules (not a new generation).
- ADR **0016** `changedAreas` baseline gains **`INVOCATION_RETENTION`**; primary navigation amends to package hub tab as the default entry.

## Amendment — 2026-07-18 (align ADR-0057 PII retention redaction; IBL-A5)

**Prior decision (Sensitive-data basis bullet, 2026-07-16):** sanitized template variable persistence in `parameters_storage` is an authorized retention-scoped exception for caller reconciliation and CE-G06 regenerate.

**Amended alignment:** That exception is **narrowed by** [ADR-0057](../authorization-security/0057-invocation-parameters-retention-for-regenerate.md) Amendment 2026-07-18 — cleartext variable values may be retained **only** for `piiCategory = NONE` (schema default); `piiCategory ≠ NONE` and unknown keys must redact/exclude. Password strip, invocation TTL, management non-exposure, and encryption-at-rest deferral are unchanged. Behavior: [ibl-a5-pii-retention-redaction.md](../../behavior/ibl-a5-pii-retention-redaction.md). Does **not** flip checklist **#3b** / **#5a**.

## Amendment — 2026-07-21 (navigation surface; SYS-NORM Wave 2)

**Unchanged package-first decision:** API configuration remains **package-scoped** (not a standalone policy catalog); `/api/policies` remains cross-package monitoring only; auto-materialize / retention / invocation semantics above are unchanged.

**Amended navigation surface (IA only):** The primary management edit entry is **no longer** the template package hub **External access** tab. SYS-NORM Wave 2 removes that hub tab and routes operators to the package API settings shell:

| Surface | Role after Wave 2 |
| --- | --- |
| Template package hub | Version lines primary; hub header **API settings** jump |
| Canonical edit shell | `/api/packages/:templateId/settings` (optional `?panel=` / `?releaseVersion=`) |
| Legacy hub `?tab=apiAccess` / `#apiAccess` | Redirect → settings shell |
| Legacy `/api/policies/:templateId` | Redirect → settings shell (replaces prior redirect-to-hub-tab) |
| Full settings panels + invocation dashboard | Wave 3 (`sys-norm-external-ops`) — see [Amendment — 2026-07-21 (Wave 3)](#amendment--2026-07-21-ia-completion-sys-norm-wave-3) |

Behavior: [sys-norm-hub-ia.md](../../behavior/sys-norm-hub-ia.md); redirect table sync in [api-package-access-and-invocation-records.md](../../behavior/api-package-access-and-invocation-records.md) §15. Does **not** flip checklist **#3b** / **#5a**. Does **not** reopen per-version ApiPolicy entities.

## Amendment — 2026-07-21 (IA completion; SYS-NORM Wave 3)

**Unchanged package-first decision:** API configuration remains **package-scoped** (not a standalone policy catalog); `/api/policies` remains cross-package monitoring / dashboard only; auto-materialize / retention / invocation semantics in the Decision body above are **unchanged**.

**Amended IA completeness (navigation + surfaces only):** SYS-NORM Wave 3 completes the Wave 2 settings shell and adds the separate invocations surface:

| Surface | Role after Wave 3 |
| --- | --- |
| Package API settings | **Complete** edit home at `/api/packages/:templateId/settings` (panels + deep-link; no interim “shell only” banner) |
| Invocation records | **Separate** management page (dashboard-like list/filters/detail summary-only) |
| External services nav | Overview (ops dashboard) + Invocations membership |
| `/api/policies` | Cross-package readiness/alerts monitoring entry (not a second catalog) |

Behavior: [sys-norm-external-ops.md](../../behavior/sys-norm-external-ops.md) (**BDD-SYS-NORM-W3-001…018**); TM **#147** Done (`18a9e3b2` / `f21dda5e`). Does **not** flip checklist **#3b** / **#5a**. Does **not** reopen per-version ApiPolicy entities. Does **not** invent NFR SLOs.

## Consequences

- Publish gate logic must materialize policy **before** «policy must exist» checks (see BDD R1).
- Runtime and cleanup jobs must implement separate schedulers for idempotency (7d), artifacts (package doc TTL), and invocation rows (package record TTL).
- OpenAPI v1 and management APIs gain invocation list/detail paths and retention fields on `api_policy`.
- Standalone API catalog UX is demoted; management edit journeys anchor on the package API settings route (`/api/packages/:templateId/settings`) after SYS-NORM Wave 2–3 (historical hub External access tab retired; Wave 3 completes settings + separate invocations page).

## Alternatives Considered

- **Per-release API policy overrides** — rejected for v1; package-level policy remains the sole baseline (ADR 0002).
- **Separate tables for batch root vs items** — rejected; single table with `invocationKind` and view filters (C9).
- **Extend global 7-day artifact TTL for all saves** — rejected; package-configurable retention with explicit supersede rules preserves idempotency semantics while meeting backup needs.

## Related Documents

- BDD: [api-package-access-and-invocation-records.md](../../behavior/api-package-access-and-invocation-records.md)
- Plan: [P12-api-package-access-invocation-records.md](../../plan/detail/P12-api-package-access-invocation-records.md)
- [ADR-0057 Retention-Scoped Invocation Parameters for Audit Regenerate](../authorization-security/0057-invocation-parameters-retention-for-regenerate.md)
