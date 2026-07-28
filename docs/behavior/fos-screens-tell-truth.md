# Behavior — FOS-W1 Screens tell the truth

| Field | Value |
| --- | --- |
| **Slice** | `fos-screens-tell-truth` |
| **Task Master** | **#171** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **true** |
| **delivery_lane** | **full** |
| **Source** | [FOS-W1-screens-tell-truth.md](../plan/detail/FOS-W1-screens-tell-truth.md) |

## Goal

Bank authors and operators see human, English-first (with zh-CN) labels for fidelity warnings,
publish-gate checks, audit event types, master statuses, invocation status/kind, variable types,
policy domains, and `api.error.template.invalidRulesJson` — never raw machine keys in primary UI.

## Acceptance scenarios

### BDD-FOS-W1-001 — Fidelity warning human label
**Given** a preview warning with code `SEAL_OUTSIDE_AUTHORIZED_AREA` and messageKey `generation.warning.fidelity.sealOutsideAuthorizedArea`  
**When** the fidelity warning list renders  
**Then** the primary label is a human sentence and does **not** start with / contain visible `generation.warning.fidelity.`

### BDD-FOS-W1-002 — Publish-gate nesting cycle label + Go-fix
**Given** a publish-gate item with messageKey `api.publishGate.contentModuleNestingCycle.blocked`  
**When** the checklist label and Go-fix resolve  
**Then** the label is a human sentence without `=` key/value pairs, and Go-fix targets `designTab=contentModules`

### BDD-FOS-W1-003 — Audit event catalogue
**Given** the management audit event-type filter  
**When** options are built  
**Then** catalogue size ≥ backend ManagementAuditEventTypes event codes and includes labeled `USER_DELETED`, `LEGAL_HOLD_CREATED`, `API_POLICY_UPDATED`

### BDD-FOS-W1-004 — No phantom ARCHIVED master status
**Given** master status filter options  
**When** options are listed  
**Then** values are exactly `DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED` (no `ARCHIVED`)

### BDD-FOS-W1-005 — Invocation status/kind labels
**Given** invocation list/filter surfaces  
**When** status/kind are shown  
**Then** translated labels (and status tags) are used instead of raw enums

### BDD-FOS-W1-006 — invalidRulesJson message exists
**Given** backend/FE error catalogues  
**When** `api.error.template.invalidRulesJson` is looked up  
**Then** English (and FE zh) messages exist

## Non-goals
- No new product features; no `en.ts` mega-split (#168); no absolute seals; no stamp-default-on; do not flip #3b/#5a; do not mark #53/#106 Done.
