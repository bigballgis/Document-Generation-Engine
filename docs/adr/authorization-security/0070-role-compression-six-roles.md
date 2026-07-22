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
  - docs/behavior/sys-norm-n18-role-l1.md
---

# ADR 0070: Role Compression to Six Management Roles

## Status

**Accepted** (2026-07-21) — user-confirmed System Normalization Program decision lock.

| Gate | Note |
| --- | --- |
| User confirmation | **Yes** — 2026-07-21 (LOCKED — do not reopen) |
| Behavior SoT (decision) | [system-normalization-program.md](../../behavior/system-normalization-program.md) §2.6 / §6 |
| Runtime BDD SoT | [sys-norm-roles.md](../../behavior/sys-norm-roles.md) **ready** / delivered — **BDD-SYS-NORM-ROLE-001…018** |
| Implementation wave | **Wave 5** `sys-norm-roles` → **Done** (TM **#149**; MAIN merge `febb95b3`; worktree **REMOVED**) |
| Matrix rewrite | **Landed** (doc-keeper stage 3) — [permission-matrix.md](../../security/permission-matrix.md) six-role SoT + production catalog/migration/FE |
| Checklist | Does **not** flip **#3b** / **#5a** |

`sourceOfTruth: true` while Accepted.

**Implementation note (2026-07-21):** Wave 5 delivery **Done** (`febb95b3`) — matrix rewrite,
Flyway remap, JWT/capabilities, and FE role surfaces landed. **Accepted status and Decision
merge/SoD text below are unchanged** (do not reopen merges).

**P-Q1 L1 labels — Confirmed (2026-07-22):** `DOCUMENT_AUTHOR` L1 display strings locked as
EN **Document author** / ZH **文档作者** (role ID unchanged). Behavior SoT
[sys-norm-n18-role-l1.md](../../behavior/sys-norm-n18-role-l1.md)
**BDD-N18-L1-008…010**; delivery leaf `sys-norm-n18-role-l1` TM **#157**+**#158** → **Done**
(`a4f59c4d` / `b54281b1` — governance Confirmed here; runtime i18n/EntityLink delivered on that leaf).

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
   - `DOCUMENT_AUTHOR` — **merge** of `MASTER_DESIGNER` ∪ `TEMPLATE_AUTHOR` (stable role ID preferred; **L1 EN/ZH display labels Confirmed** — EN **Document author** / ZH **文档作者**; P-Q1 closed via BDD-N18-L1-008…010)  
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

- Wave 5 owned matrix rewrite, Flyway/user-role migration, OpenAPI/FE role enums, and E2E —
  **delivered** 2026-07-21 (`febb95b3`). Decision text above unchanged.
- Onboarding / journey docs that listed eight roles were updated in Wave 5; L1
  `DOCUMENT_AUTHOR` display labels Confirmed 2026-07-22 (residual leaf
  `sys-norm-n18-role-l1`).
- ADR-0064 legal track remains; `LEGAL_REVIEWER` is not compressed away.
- `DOCUMENT_AUTHOR` L1 labels are **Confirmed** (EN **Document author** / ZH **文档作者**);
  P-Q1 is closed. Role ID / merge decisions above are unchanged.

## Related Documents

- Wave 5 runtime BDD: [sys-norm-roles.md](../../behavior/sys-norm-roles.md) (`ready`)
- P-Q1 / N18 L1 lock BDD: [sys-norm-n18-role-l1.md](../../behavior/sys-norm-n18-role-l1.md) (`ready`)
- Behavior charter: [system-normalization-program.md](../../behavior/system-normalization-program.md)
- Program: [system-normalization-program-2026-07.md](../../plan/system-normalization-program-2026-07.md)
- Permissions: [permission-matrix.md](../../security/permission-matrix.md) (Wave 5 rewrite + runtime Done)
- Legal approval: [0064-legal-compliance-approval-matrix.md](../template-lifecycle/0064-legal-compliance-approval-matrix.md)
- Terminology: [business-terminology-guide.md](../../product/business-terminology-guide.md)
