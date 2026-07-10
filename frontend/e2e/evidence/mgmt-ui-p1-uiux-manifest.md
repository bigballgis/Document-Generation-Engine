# mgmt-ui-p1 / P1-5 UIUX Evidence Manifest

**Task:** MGMT-UI Round 3 P1 - visual/UX evidence for D1 layout fill + P1 surfaces (dashboard, users, api/policies, published release Approval)
**Reviewer:** e2e-uiux-reviewer (stage 7 re-check after P1-3 Option B)
**Date:** 2026-07-10
**Viewport (primary):** **1920x1080** (desktop-first; slice-required)
**Viewport (note):** 375x812 mobile smoke only
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080` (fresh deploy with release-scoped publish-gate API)
**Placement:** ISOLATED `D:/working/DGE-mgmt-ui-p1` / `feat/mgmt-ui-p1`
**Verdict:** **PASS** - prior Critical on Approval "template not found" cleared

## Capture method

Read-only on app code. Screenshots re-captured via one-off Playwright Chromium session against Docker `:4173` (GLOBAL_ADMIN `10000001`) after Option B fix. Metrics in `capture-report.json`.

| Item | Value |
| --- | --- |
| Login | UI login as GLOBAL_ADMIN |
| Brands | REDBC + GREENBC via `.brand-switcher` |
| Published release under test | `DEMO-FULL-FLOW-LETTER` / templateId `959fb33a-1333-4839-8395-f3019b9da6e3` / release `1.0.0` |
| Publish-gate | `GET .../templates/{id}/releases/1.0.0/publish-gate` → **200**, **11** checklist items |

## Screenshot inventory (12)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `screenshots/01-dashboard-redbc-1920x1080.png` | `/dashboard` My tasks | REDBC | 1920x1080 |
| 2 | `screenshots/01-users-redbc-1920x1080.png` | `/entitlement/users` (table populated) | REDBC | 1920x1080 |
| 3 | `screenshots/01-api-policies-redbc-1920x1080.png` | `/api/policies` Attention items | REDBC | 1920x1080 |
| 4 | `screenshots/02-dashboard-greenbc-1920x1080.png` | `/dashboard` | GREENBC | 1920x1080 |
| 5 | `screenshots/03-users-greenbc-1920x1080.png` | `/entitlement/users` | GREENBC | 1920x1080 |
| 6 | `screenshots/04-api-policies-greenbc-1920x1080.png` | `/api/policies` | GREENBC | 1920x1080 |
| 7 | `screenshots/05-release-detail-basics-redbc-1920x1080.png` | Release 1.0.0 Basics | REDBC | 1920x1080 |
| 8 | `screenshots/06-release-detail-approval-redbc-1920x1080.png` | Release Approval + live checks checklist | REDBC | 1920x1080 |
| 9 | `screenshots/07-release-detail-approval-greenbc-1920x1080.png` | Release Approval + live checks checklist | GREENBC | 1920x1080 |
| 10 | `screenshots/08-brand-header-greenbc-1920x1080.png` | Shell brand lockup | GREENBC | crop |
| 11 | `screenshots/09-brand-header-redbc-1920x1080.png` | Shell brand lockup | REDBC | crop |
| 12 | `screenshots/10-dashboard-mobile-375x812-note.png` | Dashboard narrow note only | REDBC | 375x812 |

## D1 layout-fill metrics (1920x1080)

Measured: `.app-page-layout` width / `.shell-page-root` width; require panel surface + ratio > 0.85.

| Surface | panel | shellW | layoutW | ratio | overflowX |
| --- | --- | --- | --- | --- | --- |
| `/dashboard` REDBC | PASS | 1660 | 1660 | **1.00** | false |
| `/entitlement/users` REDBC | PASS | 1660 | 1660 | **1.00** | false |
| `/api/policies` REDBC | PASS | 1660 | 1660 | **1.00** | false |
| `/dashboard` GREENBC | PASS | 1660 | 1660 | **1.00** | false |
| Release Basics REDBC | PASS | 1660 | 1660 | **1.00** | false |
| Release Approval REDBC | PASS | 1660 | 1660 | **1.00** | false |

No large right-side gray gutter inside the shell content column. Release detail uses contained workspace rhythm (expected for detail pages); catalog pages fill fluidly.

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | PASS | 01-07 |
| Dual-brand REDBC / GREENBC | PASS | 01 vs 02; 08 vs 09; Approval 06 vs 07 |
| Logo / brand lockup switch | PASS | 08 GREENBC teal; 09 REDBC red |
| D1 layout fill (panel, no gray gutter) | PASS | metrics + 01-03 |
| Users table density / no UUID primary cells | PASS | 01-users - employee id + display name |
| API policies alerts-first IA | PASS | 01-api-policies - Attention items; empty No Data; Browse templates CTA |
| Release read-only banner + Basics summary | PASS | 05 |
| Approval status + audit trail (no Publish/Submit) | PASS | 06 - Live + workflow audit; no publish/submit actions |
| P1-3 live pre-release checks (happy path) | **PASS** | 06/07 - release-scoped gate **200**; **11** Informational checklist rows; Option B copy; **no** "template not found" / LoadErrorPanel |
| English-first copy | PASS | All captured surfaces EN |
| No text overflow / overlap at 1920 | PASS | Visual review of 01-07 |
| Mobile (quick note) | NOTE | 10 - sidebar + content cramped; desktop-first OK; not a merge blocker for this slice |

## Findings

### Critical (must fix before merge / stage PASS)

_None._ Prior Critical **cleared**:

1. ~~**Approval live checks contradictory error**~~ — Re-check on published Live `DEMO-FULL-FLOW-LETTER` / `1.0.0`: Approval shows **Current pre-release checks evaluation** with Option B labeling (*Live evaluation of this published release version at view time — not a historical publish-time snapshot.*) and a real **11-item** checklist (Informational tags). No `LoadErrorPanel`, no "The template was not found." Gate HTTP **200**. Evidence: `06`/`07` + `capture-report.json` (`criticalCleared: true`, `gateStatus: 200`, `gateItemCount: 11`).

### Suggestion (should improve)

1. **API policies empty Attention items** - Full-width empty table at 1920 leaves a large sparse region; consider EmptyStatePanel + Browse templates CTA prominence (R2/P1-4 deferred polish). Rule: empty-state completeness. Evidence: `01-api-policies-*-1920x1080.png`.

2. **Basics Master ID raw UUID** - Summary shows Master ID as UUID string (linked). Prefer human-readable master name as primary label per entity-display constitution; UUID as subtitle only. Not introduced by Option B; not a P1-3 blocker. Evidence: `05`.

### Nice to have

1. Keyboard focus-visible ring screenshot on Approval workspace tabs / Clone action.
2. Full `a11y-smoke.spec.ts` green on this stack: **3 passed / 4 failed** in this session — failures are seed/env (content-modules list, tester dashboard, lifecycle submit-gate fixture, FOL catalog bindings), **not** the P1 Approval/dashboard/users/api surfaces under review. Visual contrast on captured P1 surfaces looks adequate.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | PASS (re-captured) |
| D1 layout fill on required routes | PASS |
| Dual-brand | PASS |
| Approval live checks usable evidence | **PASS** (critical cleared) |
| **Overall stage 7** | **PASS** |

## Paths

- Manifest: `frontend/e2e/evidence/mgmt-ui-p1-uiux-manifest.md` (this file)
- Screenshots: `frontend/e2e/evidence/mgmt-ui-p1/screenshots/`
- Capture metrics: `frontend/e2e/evidence/mgmt-ui-p1/capture-report.json`
- BDD: `docs/requirements/mgmt-ui-defects-behavior-spec.md` v1.2.0

## Related

- Functional residual: `frontend/e2e/mgmt-ui-p1-residual.spec.ts` (P1-3-A release-scoped gate)
- Plan: `docs/plan/detail/MGMT-UI-defects.md` (P1-5)
