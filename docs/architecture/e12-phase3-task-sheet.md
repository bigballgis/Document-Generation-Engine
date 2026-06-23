---
id: DOC-ARCH-E12-PHASE3-TASK-SHEET.MD
type: Architecture View
status: Accepted
sourceOfTruth: false
owners:
  - architecture
  - implementation
dependsOn:
  - docs/plan/master-plan.md
  - docs/plan/ux-upgradeability-optimization-plan.md
  - docs/PROJECT-STATUS-RESET.md
---

# E12 Phase 3 Task Sheet (UX Wave A/B)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Maps to UX-A/UX-B/UX-D tasks in [ux-upgradeability-optimization-plan.md](../plan/ux-upgradeability-optimization-plan.md).

## Purpose

Session capability gating, operator workbenches, and half-built interaction closure
delivered as UX optimization Wave A + Wave B frontend slice.

## Baseline

- **Scope:** Epic E12 extension — UX Wave A/B
- **Priority:** P1
- **Exit criteria:** Roles see only authorized controls; tester/approver workbenches
  reachable; credential lifecycle and template create flows complete in UI.

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E12-T11 | P0 | Frontend auth | Session `ManagementCapabilities` + `useCapabilities` | Frontend consumes backend capability flags; role helpers fall back when absent | Done |
| E12-T12 | P0 | Frontend routing | `visibleRoutes`-only access (`canAccessRoute`, `ManagementShell`) | Nav and guards derive from session routes, not hardcoded role lists | Done |
| E12-T13 | P0 | Frontend roles | Remove `API_ADMIN`; capability-based helpers in `roles.ts` | API policy gated by admin capabilities; no orphaned API_ADMIN path | Done |
| E12-T14 | P0 | Frontend template | `TemplateCreateDialog` + list create entry | Author can create template from approved master; lands on detail | Done |
| E12-T15 | P0 | Frontend template | Granular capability gating on `TemplateDetailView` | Lifecycle actions hidden when backend would deny; no 403 from visible controls | Done |
| E12-T16 | P0 | Frontend API mgmt | Credential rotate/revoke UI + `apiPolicy` client | Rotate/revoke with confirm; one-time secret reveal; revoked state shown | Done |
| E12-T17 | P0 | Frontend shell | 401 session-expiry interceptor in `http.ts` | 401 clears session and redirects to login with `sessionExpired` notice | Done |
| E12-T18 | P0 | Frontend UX | `useConfirmAction` composable | Reusable confirm applied to destructive/irreversible actions | Done |
| E12-T19 | P0 | Frontend workbench | `TesterWorkbenchView` + `route.tester-workbench` | Test queue, pass/fail gated by `canDecideTests` | Done |
| E12-T20 | P0 | Frontend workbench | `ApproverWorkbenchView` + `route.approver-workbench` | Approval queue, approve/reject gated by `canDecideApprovals` | Done |
| E12-T21 | P1 | Frontend home | `RoleHomeView` actionable summary | Governance homes show summaries instead of placeholder cards | In Progress |
| E12-T22 | P1 | Frontend i18n | Wave A/B message keys in `en.ts` | New user-facing strings via keys; remaining hardcoded strings tracked in UXE2 | In Progress |

## Gate commands

- Frontend: `pnpm -C frontend lint` / `type-check` / `test` / `build` — green (40 tests, 2026-06-23)

## Evidence

| Evidence slot | Status |
| --- | --- |
| Unit / component tests | Done |
| Frontend quality gates | Done (2026-06-23) |
| Plan status sync | Done |
