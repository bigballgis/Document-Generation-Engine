# P12 — API Package Access & Invocation Records (Detailed Plan)

**Slice ID:** `P12-API-PACKAGE-ACCESS-INVOCATION`  
**Slice status:** **Paused** (2026-07-03 — **P22-DEMO-EXPANSION** user priority) | **Depends on:** P6, P7, P17, P21, template package hub (P3-T06)  
**Active phase note:** **P22** is the sole formal phase `In Progress`. Backend **T01–T06 Done** (2026-07-03); **frontend T07–T09** + E2E **T11–T12** resume after P22.

> **BDD:** [api-package-access-and-invocation-records.md](../../behavior/api-package-access-and-invocation-records.md) (`BDD-API-PACKAGE-ACCESS-INVOCATION-001`, status **ready**)

## Behavior goal

Reframe API management as **package-first configuration** (not a separate catalog workflow):

1. **Auto-materialize** `api_policy` on package lifecycle transitions; first publish sets default route to released version and exposes **default + explicit** generate paths.
2. **Convention over configuration** UI on template package hub — L1: AD Group, default route, retention, routes summary, credentials; advanced policy domains collapsed.
3. **Invocation records** — caller-queryable history with optional document retention; separate from compliance audit summaries.

## Confirmed decisions (traceability)

| ID | Summary |
| --- | --- |
| C1–C5 | Package API, no silent default switch, config-not-catalog, convention UI |
| C6 | Caller full parameters on own records; admin/audit summary only |
| C7 | Retention configurable; default save on / 90d record / 30d doc; presets; max 7y / 1y |
| C8 | Record may outlive document artifact |
| C9 | Single table; logical vs flat invocation list |
| C10 | Skeleton `api_policy` at `PENDING_RELEASE`; publish sets `defaultRoute` only |
| C11 | Flat view excludes `BATCH_ROOT` |
| C12 | Strip encryption passwords from stored parameters |
| C13 | Idempotency replay does not create duplicate invocation |
| C14 | `changedAreas` adds `INVOCATION_RETENTION` |
| C15 | Package hub L2 read-only invocation summary for admins |

## Exit criteria

- First publish creates policy + dual API paths without pre-existing manual policy row.
- Second publish does not change default without explicit governed change.
- Package hub replaces standalone API policy catalog as primary IA.
- Caller GET invocations (logical/flat) + detail with parameters (sanitized).
- Retention presets enforced; artifact TTL respects package config when save enabled.
- Green gates + E2E functional + UIUX evidence for hub access tab.
- Requirements, PRD, domain model, permission matrix, contract outline, **OpenAPI v1 invocation paths**, **ADR-0040** synced.

## Task breakdown

| ID | Owner | Task | Status |
| --- | --- | --- | --- |
| **P12-API-PKG-T01** | backend-engineer | Flyway: extend `api_policy` (`save_generated_documents`, `invocation_record_retention_days`, `document_retention_days`); platform defaults constant; `ApiPolicyPlatformDefaults` | **Done** (2026-07-03) |
| **P12-API-PKG-T02** | backend-engineer | Lifecycle: `ensureApiPolicySkeleton` on `PENDING_RELEASE`; `ensureApiPolicyOnPublish(releaseVersion)` before publish gate; remove pre-publish «policy must exist» blocker; import must not silent-clear default | **Done** (2026-07-03) |
| **P12-API-PKG-T03** | backend-engineer | Flyway: `api_invocation_record` table; `InvocationRecordService` write on generate/batch/async; link idempotency/task; sanitize encryption fields (C12); replay rule (C13) | **Done** (2026-07-03) |
| **P12-API-PKG-T04** | backend-engineer | Retention scheduler: cleanup records + artifacts per package TTL; four-layer clock (15m download / 7d idempotency / doc / record) | **Done** (2026-07-03) |
| **P12-API-PKG-T05** | backend-engineer | Runtime API: `GET …/invocations` (`view=logical\|flat`, `requestId` filter); `GET …/invocations/{invocationId}`; extend download TTL when save enabled | **Done** (2026-07-03) |
| **P12-API-PKG-T06** | backend-engineer | Management: retention domain save + `INVOCATION_RETENTION` audit; backfill migration for published packages missing policy | **Done** (2026-07-03) |
| **P12-API-PKG-T07** | frontend-engineer | Package hub **External access** tab: L1 (routes, AD Group, default, retention presets, credentials); advanced collapsed; remove empty «not configured» state | **Done** (2026-07-03, `22c7ab8`) |
| **P12-API-PKG-T08** | frontend-engineer | Deprecate/downgrade `ApiPolicyHomeView` catalog → dashboard cross-package alerts; redirect deep links to package hub tab | **Done** (2026-07-03, `22c7ab8`) |
| **P12-API-PKG-T09** | frontend-engineer | Admin L2 read-only recent invocations panel on hub (summary, no variable plaintext) | **Done** (2026-07-03) |
| **P12-API-PKG-T10** | doc-keeper | OpenAPI v1 paths/schemas for invocations; `contract-outline.md` invocation section; ADR-0040 | **Done** (doc-only 2026-07-03) |
| **P12-API-PKG-T11** | e2e-test-engineer | Playwright: S1–S8 BDD scenarios + caller invocation query journey | **In Progress** (2026-07-03 — hub L1 + overview spec landed) |
| **P12-API-PKG-T12** | e2e-uiux-reviewer | UIUX evidence: hub access tab L1, retention controls, logical vs flat API docs screenshots | Not Started |

## Implementation order

1. T01 → T02 (materialize + gate)  
2. T03 → T04 → T05 (invocation pipeline)  
3. T06 (management API + backfill)  
4. T07 → T08 → T09 (UI)  
5. T10 (OpenAPI + ADR) — **Done** (doc-only 2026-07-03)
6. T11 → T12 (E2E/UIUX)

## Non-goals (this slice)

- Per-release API policy overrides (v1 remains package-scoped).
- Cross-template global invocation list (v2).
- Changing 15-minute download URL TTL.
- Replacing compliance audit console with invocation records.

## Related docs

- [Behavior spec](../../behavior/api-package-access-and-invocation-records.md)
- [Catalog navigation UX](../../product/catalog-navigation-ux.md) § API access on package hub
- [ADR-0040 Package-first API access & invocation retention](../../adr/api-management/0040-api-package-access-and-invocation-retention.md)
- [P17 API policy domain governance](./P17-api-policy-domain-governance.md) (default-route governance reused)
- [P12 deferred enhancements](./P12-deferred-enhancements.md) (parent phase)
