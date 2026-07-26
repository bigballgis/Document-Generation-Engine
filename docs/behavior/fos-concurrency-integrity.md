# Behavior — FOS-W8 Concurrency & integrity

| Field | Value |
| --- | --- |
| **Slice** | `fos-concurrency-integrity` |
| **Task Master** | **#178** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **partial** (error catalogue message placeholders) |
| **delivery_lane** | **full** |
| **Source** | [FOS-W8-concurrency-integrity.md](../plan/detail/FOS-W8-concurrency-integrity.md) |

## Goal

Concurrent authoring and publish cannot silently corrupt template versions; DB integrity
collisions and optimistic-lock failures surface as conflict envelopes; operator-facing
invalid-state / publish-gate errors name the offending status or gate check.

## Acceptance scenarios

### BDD-FOS-W8-001 — Optimistic lock on template version
**Given** a `template_version` row with `@Version` / `row_version`  
**When** two writers save conflicting mutations  
**Then** the loser receives HTTP 409 `TEMPLATE_OPTIMISTIC_LOCK_CONFLICT`

### BDD-FOS-W8-002 — Unique active release + publish lock
**Given** two concurrent publish attempts for the same `(template_id, release_version)`  
**When** both try to stamp PUBLISHED  
**Then** at most one succeeds; the other fails closed (lock and/or unique index)

### BDD-FOS-W8-003 — Data integrity → conflict envelope
**Given** a unique constraint collision (e.g. duplicate anchor binding)  
**When** the persistence exception reaches the API  
**Then** the response is 409 `DATA_INTEGRITY_CONFLICT`, not an opaque 500

### BDD-FOS-W8-004 — Actionable invalidState / publishGateBlocked
**Given** a lifecycle action in the wrong status, or a blocked publish gate  
**When** the API rejects the action  
**Then** the message includes the current status or first blocking gate check code

### BDD-FOS-W8-005 — Shared assertDraft
**Given** composition inclusion / content-module reference mutations  
**When** the template is not DRAFT  
**Then** they reject via the shared `TemplateAccessGuardSupport#assertDraft` path
