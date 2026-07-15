---
id: ADR-0057
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - security
  - api-management
  - architecture
adrNumber: "0057"
topic: authorization-security
related:
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
  - docs/adr/api-management/0040-api-package-access-and-invocation-retention.md
  - docs/adr/authorization-security/0045-artifact-encryption-at-rest-evaluation.md
  - docs/security/permission-matrix.md
  - docs/behavior/ce-g06-audit-reproducible.md
  - docs/behavior/management-invocation-history.md
  - docs/architecture/data-storage-view.md
  - docs/domain/domain-model.md
---

# ADR 0057: Retention-Scoped Invocation Parameters for Audit Regenerate

## Status

Accepted (2026-07-16)

**Amends (does not supersede):** [ADR-0020](./0020-unified-authorization-and-sensitive-data-handling.md) sensitive-data baseline — narrow at-rest exception only.

**Aligns:** [ADR-0040](../api-management/0040-api-package-access-and-invocation-retention.md) invocation-record parameter retention with ADR-0020.

## Context

ADR-0020 forbids plaintext **persistence or display** of template variable raw values. ADR-0040 already requires each runtime generate to write an `api_invocation_record` with **sanitized** stored parameters (encryption passwords stripped) so callers can reconcile within the package invocation-retention window.

CE-G06 (Task Master **#76**) confirms audit-reproducible regenerate-by-invocation: authorized administrators replay the same stored parameters plus the pinned release-bundle master to produce a **SPECIMEN** artifact. That replay **requires** reading `parameters_storage` (or equivalent) at rest.

Architecture review of CE-G06 recorded a **Critical** conflict: persisting variables for regenerate without an authorized exception violates ADR-0020 / permission-matrix §11 as written. Document-as-code must resolve this explicitly — not by silently preferring regenerate over the sensitive-data ban, and not by dropping regenerate.

Display / management non-exposure (HIST C6) remains in force and is **not** relaxed by this ADR.

## Decision

### Retention-scoped at-rest exception (confirmed)

ADR-0020’s ban on plaintext **persistence** of template variable raw values is amended with the following **narrow exception**:

1. **Store:** `api_invocation_record.parameters_storage` (JSON or equivalent column) **may** retain sanitized template variable values and non-password encryption metadata needed to reassemble a generation request.
2. **Purpose (only):**
   - Caller-facing invocation reconciliation (ADR-0040); and
   - Internal CE-G06 regenerate-by-invocation replay (`POST …/invocations/{invocationId}/regenerate`).
3. **Sanitization (mandatory):** Must **never** persist DOCX/PDF encryption password plaintext (`openPassword`, `ownerPassword`) — unchanged from ADR-0040. Passwords remain strip-on-write; regenerate must not re-apply output encryption when passwords are absent (CE-G06 G06-C10).
4. **Retention / TTL (mandatory):** Parameter bytes share the **invocation record** retention clock (ADR-0040: package `invocationRecordRetentionDays`, default 90 / max 2555). When the row is purged or hard-deleted by the retention job, `parameters_storage` is destroyed with it. **No** separate longer retention for variables. Expired records are not regenerable (`410 INVOCATION_RECORD_EXPIRED`).
5. **Access (mandatory):**
   - **Allowed readers of variable plaintext:** (a) the owning API credential’s caller-facing invocation detail APIs; (b) **server-internal** regenerate assembly only.
   - **Forbidden readers:** management list/detail/CSV, management audit events, application logs, OpenAPI examples, exports, support dumps, and any unauthorized UI — **HIST C6 / ADR-0020 display ban unchanged**. Regenerate HTTP responses and `INVOCATION_REGENERATED` audit summaries must not include variables or passwords.
6. **Authorization for regenerate:** Unchanged CE-G06 matrix — `GLOBAL_ADMIN`, same-group `GROUP_ADMIN`, template-visible `AUDIT_ADMIN` only; fail-closed otherwise.

### Encryption-at-rest (deferred — not blocking CE-G06)

**Confirmed for this ADR:** Application-layer or DB column encryption of `parameters_storage` is **deferred**, same compensating posture as MinIO SSE deferral in [ADR-0045](./0045-artifact-encryption-at-rest-evaluation.md): access control + retention purge + management non-exposure are the v1 controls.

**Pending (not a CE-G06 Done gate):** When bank KMS / column-encryption ownership is assigned (M9 / follow-on security slice), re-evaluate encrypting `parameters_storage` at rest and amend this ADR or ADR-0045 accordingly. Until then, **backend MUST NOT** invent an ad-hoc crypto scheme for this column.

### Backend remediation obligations (for implementers)

| Control | Required for CE-G06 / arch Critical close? | Spec |
| --- | --- | --- |
| Documented exception + matrix §11 alignment | **Yes** (this ADR) | Docs only |
| Strip encryption passwords on write | **Yes** | ADR-0040; verify still enforced |
| Purge `parameters_storage` with invocation retention job | **Yes** | Same TTL as invocation row; no orphan parameter blobs |
| Management APIs never return variables | **Yes** | HIST C6; regenerate response whitelist |
| Column / app encryption-at-rest for `parameters_storage` | **No** (deferred) | Pending KMS; see above |

## Consequences

- Architecture Critical “plaintext variables vs ADR-0020” is closed by an **authorized, retention-scoped** exception rather than by removing regenerate or by silent drift.
- ADR-0040 invocation parameter storage and CE-G06 regenerate share one lawful basis and one TTL.
- Management and audit surfaces stay free of variable plaintext.
- Future encryption hardening is tracked as pending, not as a hidden assumption of Done.

## Alternatives Considered

- **Forbid parameter persistence; regenerate from hashes only:** rejected — CE-G06 confirmed BDD requires faithful variable replay; hashes cannot reconstruct values.
- **Store parameters only in memory / short Redis TTL:** rejected — audit regenerate must work across the invocation retention window (up to years per package config).
- **Allow management UI to view variables for “audit convenience”:** rejected — HIST C6 / ADR-0020 display ban; regenerate produces SPECIMEN artifact without exposing inputs.
- **Require column encryption before accepting this exception:** deferred — no approved KMS path yet (ADR-0045); would block CE-G06 without improving authorized access boundaries.
- **Supersede ADR-0020 entirely:** rejected — unified sensitive-data baseline remains; only a scoped at-rest carve-out is needed.

## Related Documents

- [ADR-0020 Unified Authorization and Sensitive Data Handling](./0020-unified-authorization-and-sensitive-data-handling.md)
- [ADR-0040 Package-First API Access and Invocation Retention](../api-management/0040-api-package-access-and-invocation-retention.md)
- [ADR-0045 Artifact Encryption at Rest Evaluation](./0045-artifact-encryption-at-rest-evaluation.md)
- [Permission Matrix](../../security/permission-matrix.md) §11
- [CE-G06 behavior](../../behavior/ce-g06-audit-reproducible.md)
- [Management invocation history](../../behavior/management-invocation-history.md) (HIST C6)
- [Data storage view](../../architecture/data-storage-view.md)
