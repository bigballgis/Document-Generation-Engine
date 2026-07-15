# BDD: Management invocation history

**Status:** `ready`  
**BDD ID:** `BDD-MGMT-INVOCATION-HISTORY-001`  
**Slice:** P13-EXTERNAL-SERVICES-EXCELLENCE Phase 2 (C01)  
**Traceability:** [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md), BDD C6/C15, permission-matrix §7

## Actor / role

**GROUP_ADMIN** or **GLOBAL_ADMIN** with `canManageApiPolicy` on the template's group scope.

## Goal

Administrators review **paginated invocation history** on the package hub External access tab with filters and a **summary drawer** — without caller variable plaintext.

## Preconditions

- Template is published (or policy skeleton exists) and actor can read the template package.
- `api_policy` exists for the template (auto-materialized per ADR-0040).

## Locked constraint (C6)

Management APIs and UI **must not** expose `parametersStorage`, `variables`, or encryption password fields. Caller-facing runtime APIs remain unchanged.

**CE-G06 note:** Controlled regenerate may **internally** read `parametersStorage` to rebuild a SPECIMEN artifact. Responses and audit summaries still must not expose variables or passwords. See [ce-g06-audit-reproducible.md](./ce-g06-audit-reproducible.md).

---

## Acceptance scenarios

### SCEN-HIST-01 — Paginated list (required)

- **Given** a published template with ≥ 25 invocation records in retention window
- **When** the administrator opens External access tab and scrolls to Invocations
- **Then** page 1 loads with default size (20) and shows `totalElements`; pagination advances to page 2 without variable plaintext

### SCEN-HIST-02 — Filter by status and kind (required)

- **Given** invocations with mixed outcome and invocationKind values
- **When** the administrator filters status=FAILED and invocationKind=SINGLE
- **Then** only matching summary rows appear; API query params mirror UI filters

### SCEN-HIST-03 — Summary drawer, no parameters (required)

- **Given** a successful invocation with stored caller parameters
- **When** the administrator opens invocation summary for that row
- **Then** the drawer shows invocationId, requestId, route summary, masked access account, timing, outcome; **And** no variables or parameters fields appear in response or UI

### SCEN-HIST-04 — Audit deep link (required)

- **Given** an invocation with known requestId
- **When** the administrator clicks **View in activity log**
- **Then** navigation opens the audit console with requestId filter pre-applied (when audit API supports it)

### SCEN-HIST-05 — Empty state (boundary)

- **Given** a template with zero invocations
- **When** the panel loads
- **Then** empty state uses `templates.policy.invocations.emptyTitle` / `emptyDescription`

---

## L1 copy keys (en base, zh-CN additive)

| Surface | en | Key |
| --- | --- | --- |
| Panel title | Invocation history | `templates.policy.invocations.title` |
| Filter status | Status | `templates.policy.invocations.filters.status` |
| Filter kind | Kind | `templates.policy.invocations.filters.kind` |
| Filter request ID | Request ID | `templates.policy.invocations.filters.requestId` |
| Drawer title | Invocation summary | `templates.policy.invocations.drawer.title` |
| Audit link | View in activity log | `templates.policy.invocations.drawer.auditLink` |
