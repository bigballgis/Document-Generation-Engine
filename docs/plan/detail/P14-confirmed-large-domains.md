# P14 — Confirmed Large Domains (Detailed Plan)

**Phase status:** Spec Done (2026-06-23) | **Depends on:** P2–P8  
**Implementation status:** Not Started — behavior specs confirmed in this plan + `permission-matrix.md` §5/§5.1 + ADR-0019; zero backend/UI code until P14 activation.

## Behavior goal

Deliver three confirmed product domains that were deferred post-P11:

1. **Clause / content module lifecycle** (PRD §6.4.2, permission-matrix §5.1)
2. **Collaboration to-dos + timeout escalation** (PRD §协作, permission-matrix §5)
3. **Template export / import across environments** (PRD §环境迁移, permission-matrix §5)

Each domain is a vertical slice with backend persistence, management API, UI, audit,
and green gates before marking Done.

---

## P14-T01 Clause / content module lifecycle

### Actor / roles

| Action | GLOBAL | GROUP | MASTER_DESIGNER | TEMPLATE_AUTHOR | TESTER | APPROVER |
| --- | --- | --- | --- | --- | --- | --- |
| Create/edit draft, new version, submit approval | ✓ | scoped | ✓ | ✓ | ✗ | ✗ |
| Approve/reject module version | ✓ | scoped | ✗ | ✗ | ✗ | ✓ |
| Stop/restore/deprecate (logical) | ✓ | scoped | ✗ | ✗ | ✗ | ✗ |
| Reference in template (approved only) | ✓ | scoped | ✓ | ✓ | read-only in test | read-only |

### States

`DRAFT` → `PENDING_APPROVAL` → `APPROVED` → `STOPPED` / `DEPRECATED` (logical delete).

### Acceptance scenarios

- **Given** an approved clause module v1.0 in group RETAIL, **When** author references it
  in a template publish candidate, **Then** the publish locks module version 1.0.
- **Given** module v1.1 approved, **When** template still references v1.0, **Then** generation
  uses v1.0 until author upgrades reference and re-runs test→approve→publish.
- **Given** GROUP admin, **When** deprecating a module with referencing templates, **Then**
  impact analysis lists templates; confirm + audit; existing published templates keep locked version.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P14-T01a | Domain model + Flyway + repository | Not Started |
| P14-T01b | Management REST CRUD + lifecycle transitions | Not Started |
| P14-T01c | Template reference + impact analysis integration | Not Started |
| P14-T01d | Management UI (list, detail, lifecycle) | Not Started |

---

## P14-T02 Collaboration to-dos + timeout escalation

### Behavior (confirmed)

- In-app to-do queues by role (author → submit/fix; tester → test queue; approver → approval
  queue; group admin → scoped escalation).
- Timeout thresholds configurable by GLOBAL/GROUP admin; escalation is **notification only**
  (no auto-decision, no proxy approval, no state change).
- To-do payload: non-sensitive summary only (no variable values, customer data, full content).

### Acceptance scenarios

- **Given** template in TESTING, **When** tester logs in, **Then** workbench shows to-do item
  with template name, group, submitter, age.
- **Given** test to-do exceeds threshold, **When** escalation runs, **Then** group admin sees
  escalation item; template status unchanged; audit records escalation.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P14-T02a | To-do entity + query API by role queue | Not Started |
| P14-T02b | Timeout config API (global + group override) | Not Started |
| P14-T02c | Escalation scheduler (no state mutation) | Not Started |
| P14-T02d | UI: to-do panels on workbenches + admin config | Not Started |

---

## P14-T03 Template export / import

### Behavior (confirmed)

- Export: approved template bundle (metadata, variables, bindings, rules refs, policy snapshot).
- Import: lands in **DRAFT**; must re-run test→approve→publish in target environment.
- Permissions: GLOBAL all; GROUP scoped; TEMPLATE_AUTHOR own templates only.

### Acceptance scenarios

- **Given** published template in dev, **When** GROUP admin exports, **Then** JSON/ZIP bundle
  downloads without secrets or runtime credentials.
- **Given** bundle imported to staging, **When** import completes, **Then** template status is
  DRAFT; lifecycle actions available per role.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P14-T03a | Export service + management endpoint | Not Started |
| P14-T03b | Import service + validation + DRAFT landing | Not Started |
| P14-T03c | UI export/import on template detail + admin bulk | Not Started |

---

## Exit criteria (phase)

- All P14-T01…T03 tasks Done with real persistence (not in-memory).
- Role-scoped UI for each domain; audit on mutating actions.
- Green gates: `mvn verify` + frontend lint/type-check/test/build.
- Permission-matrix + PRD cross-links updated; execution-sync-ledger evidence recorded.
