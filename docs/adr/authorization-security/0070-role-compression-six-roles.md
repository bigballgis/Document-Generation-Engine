---
id: ADR-0070
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-21
deciders: product, security, architecture, doc-keeper
owners:
  - security
  - architecture
adrNumber: "0070"
topic: authorization-security
related:
  - docs/behavior/sys-norm-roles.md
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/security/permission-matrix.md
  - docs/adr/template-lifecycle/0064-legal-compliance-approval-matrix.md
  - docs/product/business-terminology-guide.md
---

# ADR 0070: Role Compression to Six Management Roles

## Status

**Accepted** (2026-07-21) — user-confirmed System Normalization Program decision lock.

| Gate | Note |
| --- | --- |
| User confirmation | **Yes** — 2026-07-21 (LOCKED — do not reopen) |
| Behavior SoT (decision) | [system-normalization-program.md](../../behavior/system-normalization-program.md) §2.6 / §6 |
| Runtime BDD SoT | [sys-norm-roles.md](../../behavior/sys-norm-roles.md) **ready** — **BDD-SYS-NORM-ROLE-001…018** |
| Implementation wave | **Wave 5** `sys-norm-roles` — BDD ready; slice **Not Started** until activate |
| Matrix rewrite | Wave 5 (permission-matrix tables; doc-keeper after BDD `ready`); Wave 0 recorded Confirmed intent only |
| Checklist | Does **not** flip **#3b** / **#5a** |

`sourceOfTruth: true` while Accepted.

**Accepted ≠ Wave 5 Done.** Production role catalog, migration SQL, and FE labels remain Wave 5
delivery after matrix rewrite + this wave BDD (`ready`) — code follows plan-orchestrator /
doc-keeper / implementers.

## Context

The management assignable catalog currently exposes eight business roles (plus API caller
as a non-management identity). Operators and onboarding materials carry overlapping
author / designer / approver distinctions that raise understanding cost for an internal
bank OA surface.

The System Normalization Program confirmed a **six-role** compression target while
retaining separation of duties for testing, legal review, and audit.

## Decision

1. **Target assignable management roles (exactly six)**  
   - `GLOBAL_ADMIN` — keep  
   - `GROUP_ADMIN` — keep; **absorbs** `TEMPLATE_APPROVER`  
   - `DOCUMENT_AUTHOR` — **merge** of `MASTER_DESIGNER` ∪ `TEMPLATE_AUTHOR` (stable role ID preferred; **L1 EN/ZH display labels finalizable** — Pending P-Q1)  
   - `TEMPLATE_TESTER` — keep (do **not** merge into author)  
   - `LEGAL_REVIEWER` — keep (ADR-0064 legal track unchanged by this merge)  
   - `AUDIT_ADMIN` — keep  

2. **Retire from assignable catalog (after Wave 5 migration)**  
   `TEMPLATE_APPROVER`, `MASTER_DESIGNER`, `TEMPLATE_AUTHOR`.

3. **Migration semantics (locked)**  

   | From | To | Semantics |
   | --- | --- | --- |
   | `TEMPLATE_APPROVER` | `GROUP_ADMIN` | Approvers **become group admins** (privilege expansion **accepted**) |
   | `MASTER_DESIGNER` and/or `TEMPLATE_AUTHOR` | `DOCUMENT_AUTHOR` | Union of letterhead + template (+ clause authoring per matrix) capabilities; **no** test decide / approval decide / master review admin / publish as pure author |
   | Users with both designer + author | `DOCUMENT_AUTHOR` once | Idempotent |
   | Users with approver + group admin | `GROUP_ADMIN` once | Idempotent |

4. **SoD retained**  
   `TEMPLATE_TESTER` remains the normal `decideTests` role. Authors do **not** gain
   test-pass by this merge. Self-approval / exception intervention rules remain
   fail-closed and are re-expressed against `GROUP_ADMIN` as the normal compliance
   approver.

5. **Gate before production code**  
   Permission matrix rewrite + this ADR Accepted + Wave 5 BDD `ready`
   (**BDD-SYS-NORM-ROLE-*** ) **before** any production role-catalog / migration code.

6. **Amends intent of permission-matrix role catalog**  
   This ADR amends the **Confirmed intent** of the assignable role catalog toward six
   roles. It does **not** rewrite matrix capability tables in Wave 0; full table rewrite
   is Wave 5.

## Acceptance scenarios (decision lock)

Normative Given/When/Then for implementation: [sys-norm-roles.md](../../behavior/sys-norm-roles.md)
(**BDD-SYS-NORM-ROLE-001…018**, `ready`). Charter §6.3 keeps decision-lock sketches. ADR owns
the architectural decision only.

| ID | Summary |
| --- | --- |
| [BDD-SYS-NORM-ROLE-001](../../behavior/sys-norm-roles.md#bdd-sys-norm-role-001--approver--group-admin) | Approver → Group Admin |
| [BDD-SYS-NORM-ROLE-002](../../behavior/sys-norm-roles.md#bdd-sys-norm-role-002--designer--author--document_author) | Designer ∪ Author → `DOCUMENT_AUTHOR` |
| [BDD-SYS-NORM-ROLE-003](../../behavior/sys-norm-roles.md#bdd-sys-norm-role-003--tester-retained-sod) | Tester retained (SoD) |
| [BDD-SYS-NORM-ROLE-004](../../behavior/sys-norm-roles.md#bdd-sys-norm-role-004--legal-and-audit-untouched-by-merge) | Legal and Audit untouched by merge |
| [BDD-SYS-NORM-ROLE-005](../../behavior/sys-norm-roles.md#bdd-sys-norm-role-005--fail-closed-unknown--retired-legacy-role-on-assignment-api) | Fail-closed unknown/retired legacy role assignment |
| ROLE-006…018 | Idempotency, matrix gate, JWT/capabilities, FE pickers/journeys, seeds, OpenAPI, governance boundaries — see Wave 5 BDD |

## Alternatives Considered

| Option | Verdict |
| --- | --- |
| Keep 8-role catalog | Rejected — confirmed understanding-cost remedi |
| Merge tester into author | Rejected — SoD retained |
| Keep `TEMPLATE_APPROVER` separate | Rejected — privilege accept into `GROUP_ADMIN` |
| Rename-only without migration | Rejected — durable remap + audit required |

## Consequences

- Wave 5 owns matrix rewrite, Flyway/user-role migration, OpenAPI/FE role enums, and E2E.
- Onboarding / journey docs that list eight roles must be updated in Wave 5 (or with
  terminology Wave 8 for display labels only).
- ADR-0064 legal track remains; `LEGAL_REVIEWER` is not compressed away.
- `DOCUMENT_AUTHOR` L1 label finalization remains Pending until product locks EN/ZH strings.

## Related Documents

- Wave 5 runtime BDD: [sys-norm-roles.md](../../behavior/sys-norm-roles.md) (`ready`)
- Behavior charter: [system-normalization-program.md](../../behavior/system-normalization-program.md)
- Program: [system-normalization-program-2026-07.md](../../plan/system-normalization-program-2026-07.md)
- Permissions: [permission-matrix.md](../../security/permission-matrix.md) (rewrite = Wave 5 stage 3)
- Legal approval: [0064-legal-compliance-approval-matrix.md](../template-lifecycle/0064-legal-compliance-approval-matrix.md)
- Terminology: [business-terminology-guide.md](../../product/business-terminology-guide.md)
