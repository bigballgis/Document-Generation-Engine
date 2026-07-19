---
id: ADR-0064
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-20
deciders: architecture, template-governance, authorization, api, frontend, doc-keeper
owners:
  - template-governance
  - authorization
adrNumber: "0064"
topic: template-lifecycle
related:
  - docs/behavior/ibl-e3-legal-approval-matrix.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/domain/domain-model.md
  - docs/domain/lifecycle-review.md
  - docs/security/permission-matrix.md
  - docs/product/PRD.md
  - docs/api/contract-outline.md
  - docs/adr/template-lifecycle/0021-template-testing-approval-release-governance.md
  - docs/behavior/ce-g01-self-approval-block.md
  - docs/behavior/ce-k08-clause-legal-metadata.md
  - docs/behavior/approver-decision-journey.md
---

# ADR 0064: Legal → Compliance Multi-Stage Approval Matrix

## Status

**Accepted** (2026-07-20) — IBL-E3 / Task Master **#130** / F26 / **PD-8**.

| Gate | Note |
| --- | --- |
| PD-8 user confirmation | **Yes** — proceed with configurable legal→compliance matrix + forced legal-reviewer（**2026-07-19**） |
| BDD lock | [ibl-e3-legal-approval-matrix.md](../../behavior/ibl-e3-legal-approval-matrix.md) **ready** — **BDD-IBL-E3-001…018** lock **E3-C\*** defaults; **no remaining product fork** for this leaf |
| File status | **Accepted** (2026-07-20) — doc-keeper stage 3; Decision = E3-C\* |

`sourceOfTruth: true` while Accepted.

This ADR does **not** claim IBL-E3 implementation Done, flip checklist **#3b** / **#5a**, remove SPECIMEN (PD-6), activate IBL-E4…E7 / #119, force CE-K08 legal metadata required, or claim Wave E / IBL program Done / go-live.

**Amends** [ADR-0021](./0021-template-testing-approval-release-governance.md): one-level approval remains the **default** (`SINGLE_TRACK`); `LEGAL_THEN_COMPLIANCE` is an optional, PD-8-confirmed package-level matrix. Does **not** amend master-review or content-module independent approval tracks to multi-stage. Does **not** amend ADR-0062 / ADR-0063 / ADR-0061.

## Context

International bank letters need **segregated, ordered** legal then compliance approval with a **forced legal-reviewer** role, auditable stage decisions, and no skip/reorder (**F26** / **PD-8**).

Today:

- Template approval is **one-level** (`TEMPLATE_APPROVER` once → pending release) per ADR-0021 and domain §4.
- Management role catalog has **7** roles; no `LEGAL_REVIEWER` / `decideLegalApprovals`.
- CE-K08 optional legal metadata (`jurisdiction` / `legalReviewRef` / effective dating) remains optional and is **orthogonal** — this leaf closes the **approval-track** gap, not the metadata-optional finding.
- Master review and content-module version approval stay single-track (out of this leaf).

**PD-8** confirms the product boundary: configurable legal→compliance matrix with forced legal-reviewer. Wave E siblings (entity brands, effectiveFrom bulk, nesting, RTL, SPECIMEN removal, licensed fonts, Word) remain **out of IBL-E3**. Management UI for mode + staged decisions is **in scope** (`frontend_ui_in_scope=true`).

## Decision

1. **Scope = template lifecycle approval track only**  
   Master review and content-module independent approval remain single-level. No LEGAL→COMPLIANCE matrix for those tracks in this leaf.

2. **Package-level matrix mode**  
   Template packages declare `approvalMatrixMode` ∈ {`SINGLE_TRACK`, `LEGAL_THEN_COMPLIANCE`} (governable metadata at package level, same tier as locale). Mode applies to that package’s development line / submit-for-approval path.

3. **Defaults**  
   Migrated existing packages → `SINGLE_TRACK`. New packages default to `SINGLE_TRACK`. Authors/admins must explicitly set `LEGAL_THEN_COMPLIANCE` to enforce multi-stage.

4. **Writable window**  
   Mode may change only in `DRAFT` or `APPROVAL`+`PENDING_SUBMIT`. Once in `PENDING_LEGAL_DECISION` / `PENDING_COMPLIANCE_DECISION` / `PENDING_DECISION` / `PENDING_RELEASE` / published lines → **422** `APPROVAL_MATRIX_MODE_LOCKED` (stable code).

5. **Ordered two stages (multi-stage only)**  
   Fixed order: `LEGAL` then `COMPLIANCE`. v1 does **not** support a third stage, parallel countersign, or COMPLIANCE→LEGAL.

6. **Sub-states (multi-stage)**  
   After `SUBMIT_FOR_APPROVAL` → `APPROVAL` + `PENDING_LEGAL_DECISION`; LEGAL approve → `PENDING_COMPLIANCE_DECISION`; COMPLIANCE approve → `PENDING_RELEASE`. Reject at either stage → `DRAFT` (history retained; re-candidate requires full re-test + full two-stage re-approval, aligning ADR-0021).

7. **`SINGLE_TRACK` compatibility**  
   Submit still yields `PENDING_DECISION`; `TEMPLATE_APPROVER` (+ admin path) decides once → `PENDING_RELEASE`. `LEGAL_REVIEWER`-only users **must not** perform that single-track decision (**403**).

8. **Forced legal-reviewer**  
   In multi-stage mode, normal LEGAL decider = **`LEGAL_REVIEWER`**. Pure `TEMPLATE_APPROVER` (no `LEGAL_REVIEWER`, non-admin) calling LEGAL decide → **403** `APPROVAL_STAGE_ROLE_FORBIDDEN`. No “skip LEGAL” API/UI.

9. **COMPLIANCE role**  
   Normal COMPLIANCE decider = `TEMPLATE_APPROVER` (role id unchanged). Pure `LEGAL_REVIEWER` (no `TEMPLATE_APPROVER`, non-admin) calling COMPLIANCE → **403**.

10. **Role catalog 7→8 + capability**  
    Add `LEGAL_REVIEWER`. New capability `decideLegalApprovals` = {`GLOBAL_ADMIN`, `GROUP_ADMIN`, `LEGAL_REVIEWER`}. `decideApprovals` remains {`GLOBAL_ADMIN`, `GROUP_ADMIN`, `TEMPLATE_APPROVER`} (COMPLIANCE / single-track).

11. **Role assignment**  
    `GROUP_ADMIN` operational assignable set includes `LEGAL_REVIEWER`; still cannot assign `GLOBAL_ADMIN` / `AUDIT_ADMIN` / `GROUP_ADMIN`. Seeds/fixtures must cover `LEGAL_REVIEWER`.

12. **Admin path**  
    `GLOBAL_ADMIN` / `GROUP_ADMIN` may perform **normal** decisions on both stages within group scope without also holding `LEGAL_REVIEWER` / `TEMPLATE_APPROVER`. CE-G01 self-approval block and exception-intervention rules still apply.

13. **Self-approval (CE-G01)**  
    Each stage APPROVE/REJECT enforces CE-G01 (decision actor ≠ most recent `SUBMIT_FOR_APPROVAL` submitter; admin exception path unchanged). LEGAL approve does **not** reset the submitter — COMPLIANCE still compares against that submitter.

14. **Structured comments + fidelity**  
    Both stages reuse ADR-0021 controlled approval forms. Fidelity-viewed confirmation remains mandatory before Approve at **each** stage.

15. **Collaboration work items**  
    Multi-stage submit → LEGAL role-queue item; LEGAL approve → resolve LEGAL + create APPROVAL/COMPLIANCE item; COMPLIANCE approve → resolve + pending-release item (existing). Reject → remediation to submitter/orchestrator. Timeout escalation remains visibility-only (ADR-0021).

16. **Dashboard / routing**  
    New behavior entry “Waiting on my legal review” (LEGAL queue) visible to `decideLegalApprovals`. Existing “Waiting on my approval” shows COMPLIANCE / single-track `PENDING_DECISION` only. Deep links land on the matching stage decision surface.

17. **Management UI (required)**  
    (1) Configure `approvalMatrixMode` on create/detail; (2) stepper/status shows Legal / Compliance stage; (3) stage CTAs gated by role + sub-state; (4) wrong role fail-closed. Bank OA + English-first i18n.

18. **Management API**  
    Detail/summary echo `approvalMatrixMode` + `approvalSubState` (including new values) + optional `approvalStage` (`LEGAL` \| `COMPLIANCE` \| null). Decide endpoint carries stage explicitly **or** derives uniquely from sub-state (one public contract — no ambiguous dual source). Wrong stage → **409/422** `APPROVAL_STAGE_MISMATCH`. OpenAPI synced.

19. **Audit**  
    Each stage decision writes lifecycle audit including `approvalStage`, outcome, non-sensitive rationale summary, exception markers. No variables / customer plaintext.

20. **CE-K08 orthogonal**  
    Do **not** make `jurisdiction` / `legalReviewRef` / `effective*` required; do **not** add “no legalReviewRef ⇒ cannot enter LEGAL” gate.

21. **Still forbidden**  
    Proxy approval, auto approve/reject, email outbound (ADR-0021). Timeout escalates visibility only.

22. **SPECIMEN / PD-6 / PD-7 / Word**  
    No regenerate watermark change; no licensed fonts; no #119 Word evidence invented.

23. **Out of scope**  
    IBL-E4…E7, checklist **#3b**/**#5a** GO, Wave E / IBL program Done / go-live claims, merging `LEGAL_REVIEWER` into `TEMPLATE_APPROVER` without a new role, third+ stage / external workflow engine.

Normative behavior scenarios: **BDD-IBL-E3-001…018** in [ibl-e3-legal-approval-matrix.md](../../behavior/ibl-e3-legal-approval-matrix.md) (**E3-C1…C25**).

## Alternatives Considered

| Option | Verdict |
| --- | --- |
| **A. Keep one-level only; document legal via CE-K08 metadata** | Rejected — PD-8 requires ordered legal→compliance with forced legal-reviewer; metadata-optional remains honest. |
| **B. Reuse `TEMPLATE_APPROVER` for both stages without new role** | Rejected — conflicts with “forced legal-reviewer” segregation. |
| **C. Force all templates to `LEGAL_THEN_COMPLIANCE` on migrate** | Rejected — big-bang risk; default `SINGLE_TRACK` preserves ADR-0021 compatibility. |
| **D. Multi-stage for masters and content modules too** | Rejected for this leaf (E3-C1). |
| **E. Configurable LEGAL→COMPLIANCE matrix + `LEGAL_REVIEWER`** (this Decision) | **Accepted** — fail-closed stages, auditable, UI+API in scope. |

## Consequences

- F26 approval-governance gap is closed at the **decision** layer; implementation remains IBL-E3 / #130 delivery work (Accepted ≠ impl Done).
- Permission matrix role catalog becomes **8**; new capability and LEGAL dashboard queue are SoT for FE/BE.
- Packages without mode change keep ADR-0021 single-track behavior.
- Authors/admins can opt into ordered legal→compliance; skip/wrong-role/wrong-stage fail closed.
- ADR-0021 Amendment: one-level remains default; multi-stage is optional under PD-8.
- Management UI + E2E/UIUX are mandatory for this leaf (`frontend_ui_in_scope=true`).

## Related Documents

- Behavior: [ibl-e3-legal-approval-matrix.md](../../behavior/ibl-e3-legal-approval-matrix.md)
- Program: [intl-bank-letter-readiness-program.md](../../plan/intl-bank-letter-readiness-program.md) §7–§8 (IBL-E3, PD-8, F26)
- Baseline approval: [0021-template-testing-approval-release-governance.md](./0021-template-testing-approval-release-governance.md)
- Domain: [domain-model.md](../../domain/domain-model.md) §4.1; [lifecycle-review.md](../../domain/lifecycle-review.md)
- Permissions: [permission-matrix.md](../../security/permission-matrix.md)
- PRD: [PRD.md](../../product/PRD.md)
- API: [contract-outline.md](../../api/contract-outline.md), [openapi-v1.yaml](../../api/openapi-v1.yaml)
- CE-G01: [ce-g01-self-approval-block.md](../../behavior/ce-g01-self-approval-block.md)
- CE-K08 (orthogonal): [ce-k08-clause-legal-metadata.md](../../behavior/ce-k08-clause-legal-metadata.md)
