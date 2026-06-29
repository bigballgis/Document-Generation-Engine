# Confirmed decisions — Behavior-typed IA & business-friendly terminology (2026-06-29)

**Status:** Accepted (user-confirmed, two rounds, 2026-06-29)
**Extends (does not supersede):** [Batch B — workflow defaults](./2026-06-23-batch-b-workflow-defaults.md) (COR-T11)
**Implementation:** Not Started — delivered under [P21](../../plan/detail/P21-role-journey-frontend-redesign.md).

These decisions record the confirmed direction for the role-journey frontend redesign. They are
durable for plan/docs sync; reopen only via explicit user confirmation.

| ID | Decision | Rationale |
| --- | --- | --- |
| P21-D01 | **Hybrid IA, single task hub stays authoritative** — keep the single `/dashboard` task hub as the one authoritative work entry (COR-T11 unchanged); add **behavior-typed navigation entries** ("waiting on my test/approval/rework/go-live", "masters to review") implemented as **filtered views of the task hub**, not standalone workbench pages | Honors ADR Batch B / COR-T11 (no duplicate dead UI) while restoring role-behavior entry points |
| P21-D02 | **Per-role guided journey** — a reusable `RoleJourneyTimeline` stepper shows each role's current position, available actions, and waiting items | Non-IT users need a guided "what do I do next" view, not a flat resource catalog |
| P21-D03 | **Task hub deepening** — queue partitioning (TEST/APPROVAL/REMEDIATION/PENDING_RELEASE/ESCALATION/master review), restore dropped fields (`triggerType/summaryText/ageSeconds`), SLA aging + overdue badges, inline open actions; no in-list pass/reject (controlled decisions stay on detail) | Make to-dos actionable without weakening controlled governance decisions |
| P21-D04 | **Primary persona = foreign-bank front/middle-office non-IT staff** — drive all L1 copy by business language | Reduce learning cost; the UI should read like a bank business OA, not a dev/ops console |
| P21-D05 | **Business-friendly terminology, keys stable** — change user-visible message **values** only; never change stable i18n keys, API paths, enum codes, or audit field names. Three-layer copy model (L1 business / L2 business + help / L3 technical on contract/audit). SSOT = [business-terminology-guide.md](../../product/business-terminology-guide.md) | English-first i18n constitution preserved; safe, reversible relabeling |
| P21-D06 | **Backend collaboration completeness is a prerequisite** — behavior-typed IA requires all 6 collaboration triggers to emit work items and decisions to write `RESOLVED` (today only `SUBMIT_FOR_TEST` is emitted; `RESOLVED` is never written) before the corresponding behavior queue is exposed | Avoid empty-shell behavior entries decoupled from real workflow state |
| P21-D07 | **Publish/orchestration separation preserved** — orchestrators reaching `PENDING_RELEASE` see "awaiting team-lead go-live"; publish primary action remains GROUP/GLOBAL only (reaffirms COR-T07) | Bank governance: fail-closed admin go-live |
| P21-D08 | **Delivery by 4 role clusters in workflow order** — ① MASTER_DESIGNER + TEMPLATE_AUTHOR + TEMPLATE_TESTER → ② TEMPLATE_APPROVER + GROUP_ADMIN → ③ GLOBAL_ADMIN → ④ AUDIT_ADMIN | Upstream-first matches the natural workflow timeline |

## Non-goals / explicitly preserved

- Standalone Tester/Approver/Escalation workbench pages remain **removed** (COR-T11). Behavior
  entries are filtered task-hub views, not a reintroduction of workbench routes.
- No change to authorization truth: route visibility and capabilities stay backend-issued and
  fail-closed.

## Related

- [P21 detailed plan](../../plan/detail/P21-role-journey-frontend-redesign.md)
- [Business terminology guide](../../product/business-terminology-guide.md)
- [Catalog navigation UX](../../product/catalog-navigation-ux.md)
- [Permission matrix §13.3](../../security/permission-matrix.md)
- [Batch B workflow defaults](./2026-06-23-batch-b-workflow-defaults.md)
