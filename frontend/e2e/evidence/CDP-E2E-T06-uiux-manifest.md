# CDP-E2E-T06 UIUX Evidence Manifest

**Task:** CD-E2E-T06 / BDD-CDP-MASTER-001 — Master designer upload → anchor check → submit review → group admin Approve  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-10  
**Viewport (primary):** **1920×1080** (desktop-first; CD-2 master lifecycle slice)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080` (compose `dge-lrp-c9-load-error-panel`)  
**Placement:** ISOLATED `D:/working/DGE-cdp-e2e-t06-master` / `feat/cdp-e2e-t06-master`  
**Spec:** `frontend/e2e/CDP-E2E-T06-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T06-master-lifecycle.spec.ts` (Stage 6)  
**Verdict:** **PASS** (6/6 screenshots; REDBC + GREENBC draft anchors + pending approve rail; no Critical UIUX blockers on T06 surfaces)

## Capture method

Evidence captured via Playwright Chromium against Docker `:4173` while walking the master designer replace → submit → group-admin approve path (Demo Retail Letterhead + replacement DOCX fixture). Brands switched in-app via `.brand-switcher` (`switchBrand`). Screenshots under `frontend/e2e/evidence/CDP-E2E-T06/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Master Designer (`10000005`) → Group Admin (`10000002`) |
| Brands | REDBC + GREENBC (draft anchors + pending approve rail); approved frame switched back to REDBC |
| Fixture | Demo Retail Letterhead replace → DRAFT → PENDING_REVIEW → APPROVED |
| Surfaces in scope | Revision design (anchor catalog) dual-brand + Submit dialog + Letterhead review Approve rail dual-brand + Approved |

## Test execution

| Command | Result |
| --- | --- |
| `pnpm -C frontend exec playwright test e2e/CDP-E2E-T06-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (2026-07-10 ~22:03; 6 screenshots re-captured) |
| Functional baseline `CDP-E2E-T06-master-lifecycle.spec.ts` | **1/1 passed** (Stage 6 handoff) |
| `e2e/a11y-smoke.spec.ts` (same docker run) | **3/7 passed** — failures on unrelated surfaces (content modules H1, tester My to-dos, FOL seed / lifecycle panel); **not T06 master surfaces** |

## Screenshot inventory (6)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T06/screenshots/01-revision-draft-anchor-catalog-redbc-1920x1080.png` | Revision DRAFT — Anchor catalog after replace | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T06/screenshots/02-revision-draft-anchor-catalog-greenbc-1920x1080.png` | Same draft anchors after brand switch | GREENBC | 1920×1080 |
| 3 | `CDP-E2E-T06/screenshots/03-submit-review-dialog-redbc-1920x1080.png` | Submit letterhead for review dialog + change summary | REDBC | 1920×1080 |
| 4 | `CDP-E2E-T06/screenshots/04-pending-review-approve-rail-redbc-1920x1080.png` | Group Admin Letterhead review — Approve / Reject rail | REDBC | 1920×1080 |
| 5 | `CDP-E2E-T06/screenshots/05-pending-review-approve-rail-greenbc-1920x1080.png` | Same pending approve rail after brand switch | GREENBC | 1920×1080 |
| 6 | `CDP-E2E-T06/screenshots/06-revision-approved-redbc-1920x1080.png` | Revision APPROVED after group-admin decision | REDBC (`switchBrand` before approve) | 1920×1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01–06 |
| Dual-brand REDBC / GREENBC | **PASS** | 01 vs 02; 04 vs 05 — logo + nav accent switch |
| Logo / brand lockup switch | **PASS** | 01 Red Bank; 02 / 05 Green Bank |
| Workspace tab shell + single action rail | **PASS** | 03 — Submit on Letterhead review rail → dialog; 04–05 — Approve/Reject on tab rail only |
| Anchor catalog readability | **PASS** | 01–02 — HEADER row; human-readable labels; no raw UUID primary |
| Submit dialog | **PASS** | 03 — Change summary + Cancel/Submit hierarchy; EN copy |
| Pending approve rail (Group Admin) | **PASS** | 04–05 — Approve (success) / Reject (danger); review history with change summary |
| Approved state | **PASS** | 06 — Approved + Current badges; review history Approved + Submitted |
| English-first i18n | **PASS** | All captured surfaces EN |
| No text overflow / overlap at 1920 | **PASS** | No clipping/overlap on captured frames |
| Tokens / no brand wash | **PASS** | Brand on primary accents / active nav; white OA baseline |
| Entity display (no UUID primary) | **PASS** | Master/revision titles human-readable; actor shown as employee id in history (not UUID) |
| A11y spot check (T06 master surfaces) | **PASS** | Dialog heading/roles; tab rail actions; journey alert read-only guidance |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Revision overview empty panel (frames 01–02)** — Left “Revision overview” reads as blank white space beside Anchor catalog with no empty-state copy. Prefer `EmptyStatePanel` or hide until content exists. _Rule: state completeness. Non-blocking._ Surfaces: master revision design tab.

2. **Review history actor as employee id** — “By 10000005” / “By 10000002” without display name. Prefer role/display name (+ id as subtitle) for scanability. _Rule: entity display readability. Non-blocking._ Frames 04–06.

_Resolved before commit:_ Frame 06 brand/filename mismatch — `switchBrand(page, 'REDBC')` added before approve/capture so filename matches brand.

### 🟢 Nice to have

1. Capture approved state under both brands (or at least correctly labeled REDBC) for dual-brand badge polish.
2. Active workspace tab underline still uses Element Plus default blue under both brands — optional brand-token alignment (same pattern as prior CDP slices).
3. Focus-visible ring evidence on Submit / Approve primary CTAs.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (6) |
| Dual-brand REDBC + GREENBC (draft anchors + approve rail) | **PASS** |
| Master lifecycle surfaces (draft anchors → submit → approve → approved) | **PASS** |
| Critical UIUX / a11y blockers on CDP T06 surfaces | **None** |
| **Overall stage 7 (T06)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T06-master-lifecycle.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T06-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/masters-api.ts`, `ui.ts` (`reLoginAs`), `uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*`)
- BDD: `docs/behavior/master-designer-lifecycle.md`
- Auth fix (matrix §4): `GroupAccessService.canManageMasters` includes `MASTER_DESIGNER`
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
