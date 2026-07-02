# E06 Role-Journey Release Evidence

> **Sync status (2026-07-02):** Re-aligned — functional/UIUX evidence **re-earned via P21 Done**; this doc tracks **release packaging** gaps only. Mirror: [execution-sync-ledger.md](../plan/execution-sync-ledger.md).

## Purpose

E06 implementation (management UI shell, role-aware nav, lifecycle/API surfaces) is **Done** per [e06-task-sheet.md](./e06-task-sheet.md).

**P21** (role-journey frontend redesign) superseded the original empty evidence slots with Playwright + UIUX manifests. This document now records:

1. **Re-earned evidence** (mirror P21 — do not duplicate test implementation).
2. **Optional release packaging** artifacts for formal bank OA release bundles (if required by compliance).

## Evidence matrix

| Evidence | Description | Status | Primary source |
| --- | --- | --- | --- |
| E06-EV-01 | Login → role landing → first critical task (author/tester/approver) | **Re-earned (P21)** | P21-T03–T11 Playwright specs + UIUX manifests |
| E06-EV-02 | Forbidden route UX + audit trace | **Re-earned (P21 + E2E)** | Role gating tests; `a11y-smoke.spec.ts` |
| E06-EV-03 | Dual-brand theme + logo switch | **Re-earned (P21 UIUX)** | REDBC/GREENBC evidence in P21/P12/P14 manifests |
| E06-EV-04 | Frontend quality gate log | **Re-earned (ledger)** | `pnpm lint/type-check/test/build`; Vitest counts in ledger |
| E06-EV-05 | Formal release evidence bundle (zip/index for compliance archive) | Not Started | Optional; only if release governance requires a static bundle |

## Activation rule

Do **not** reopen P21 phase status. New E06 work is limited to **E06-EV-05** (packaging) when explicitly prioritized — not a substitute for ongoing BDD slices in `docs/plan/`.
