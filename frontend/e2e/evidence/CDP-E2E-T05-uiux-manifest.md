# CDP-E2E-T05 UIUX Evidence Manifest

**Task:** CD-E2E-T05 / BDD-CDP-PUB-001 — Team lead publish / go-live (PENDING_RELEASE → Go-live summary → Confirm)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-10  
**Viewport (primary):** **1920×1080** (desktop-first; CD-2 publish slice)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080`  
**Placement:** ISOLATED `d:\working\DGE-cdp-e2e-t05-publish` / `feat/cdp-e2e-t05-publish`  
**Spec:** `frontend/e2e/CDP-E2E-T05-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T05-team-lead-publish.spec.ts` (Stage 6)  
**Verdict:** **PASS** (4/4 screenshots; REDBC + GREENBC PENDING_RELEASE queue; no Critical UIUX blockers)

## Capture method

Evidence captured via Playwright Chromium against Docker `:4173` while walking the team-lead go-live path (fresh PENDING_RELEASE fixture from `prepareTemplatePendingRelease`). Brands switched in-app via `.brand-switcher` (`switchBrand`). Screenshots under `frontend/e2e/evidence/CDP-E2E-T05/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Group Admin (team lead / `10000002`) |
| Brands | REDBC + GREENBC (queue); REDBC (go-live dialog + External access) |
| Fixture | Fresh `E2E CDP T05 UX *` PENDING_RELEASE → PUBLISHED after confirm |
| Surfaces in scope | Dashboard `?queue=PENDING_RELEASE` dual-brand + Go-live summary dialog + External access callable |

## Test execution

| Command | Result |
| --- | --- |
| `pnpm -C frontend exec playwright test e2e/CDP-E2E-T05-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** |
| Functional baseline `CDP-E2E-T05-team-lead-publish.spec.ts` | **2/2 passed** (PUB-002 + PUB-001) |
| Combined T05 docker `--workers=1` | **3/3 passed** (2026-07-10) |

## Screenshot inventory (4)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T05/screenshots/01-dashboard-pending-release-queue-redbc-1920x1080.png` | Group Admin `/dashboard?queue=PENDING_RELEASE` — Waiting to confirm go-live row + Open | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T05/screenshots/02-dashboard-pending-release-queue-greenbc-1920x1080.png` | Same PENDING_RELEASE queue after brand switch | GREENBC | 1920×1080 |
| 3 | `CDP-E2E-T05/screenshots/03-go-live-summary-dialog-redbc-1920x1080.png` | Go-live summary — release version + checklist + coverage/validation + Cancel / Confirm go-live | REDBC | 1920×1080 |
| 4 | `CDP-E2E-T05/screenshots/04-external-access-callable-redbc-1920x1080.png` | Hub External access after Live — route summary with callable generate path | REDBC | 1920×1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01–04 |
| Dual-brand REDBC / GREENBC | **PASS** | 01 vs 02 — logo, switcher, active tab/nav accents follow brand |
| Logo / brand lockup switch | **PASS** | 01 Red Bank; 02 Green Bank |
| Workspace tab shell + single action rail | **PASS** | 03 — Confirm go-live opens summary dialog (not inline form) |
| Dashboard PENDING_RELEASE queue (team-lead Open deep-link) | **PASS** | 01–02 — human-readable item name; Open control; no raw UUID primary cell |
| Go-live summary dialog | **PASS** | 03 — release version + pre-release checks + coverage/validation summaries + Cancel / Confirm |
| External access callable after publish | **PASS** | 04 — route summary generate path + release badge |
| English-first i18n | **PASS** | All captured surfaces EN |
| No text overflow / overlap at 1920 | **PASS** | No clipping/overlap on captured frames |
| Tokens / no brand wash | **PASS** | Brand color on primary accents / active nav; white OA baseline |
| Entity display (no UUID primary) | **PASS** | Dashboard Item uses display name |
| A11y spot check (publish surfaces) | **PASS** | Dialog heading/role; Cancel/Confirm; dismiss control |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Dashboard Stage / Submitter ellipsis** — At 1920×1080, Stage and Submitter may truncate with ellipsis on the PENDING_RELEASE queue (frames 01–02), same pattern as T02/T04 queues. Consider tooltip/`title` on truncated cells. _Rule: text overflow / entity readability. Non-blocking._

### 🟢 Nice to have

1. Capture Go-live summary dialog under GREENBC for dual-brand dialog primary CTA polish.
2. Focus-visible ring evidence on Confirm go-live.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (4) |
| Dual-brand REDBC + GREENBC (PENDING_RELEASE queue) | **PASS** |
| Team-lead publish surfaces (queue → go-live dialog → External access) | **PASS** |
| Critical UIUX / a11y blockers on CDP T05 surfaces | **None** |
| **Overall stage 7 (T05)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T05-team-lead-publish.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T05-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/lifecycle-ui.ts`, `submit-approval-gate-api.ts` (`prepareTemplatePendingRelease`), `uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*`)
- BDD: `docs/behavior/team-lead-publish-journey.md`
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
