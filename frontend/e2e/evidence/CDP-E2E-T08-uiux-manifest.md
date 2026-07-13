# CDP-E2E-T08 UIUX Evidence Manifest

**Task:** CD-E2E-T08 / BDD-CDP-PREV-001–003 — Testing Tab preview success + artifact download  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-10  
**Viewport (primary):** **1920×1080** (desktop-first; CD-2 preview slice)  
**Stack:** Docker acceptance `http://127.0.0.1:5173` (FRONTEND_PORT) + `http://127.0.0.1:8080` (compose `dge-lrp-c9-load-error-panel`)  
**Placement:** ISOLATED `D:/working/DGE-cdp-e2e-t08-preview` / `feat/cdp-e2e-t08-preview`  
**Spec:** `frontend/e2e/CDP-E2E-T08-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T08-preview-artifact-download.spec.ts` (Stage 6)  
**Verdict:** **PASS** (3/3 screenshots; REDBC preview success + Download DOCX/PDF; closes P12 T13 success-frame gap; no Critical UIUX blockers)

## Gap closed

Closes P12 Template Testing Overhaul **T13** UIUX Note:

> Preview success dialog (download DOCX/PDF) not captured — FOL preview terminated in error state; frame 05 documents failure+retry UX instead.

This manifest includes a **preview success** frame showing **Download DOCX** + **Download PDF** (+ expires copy).

## Capture method

Evidence captured via Playwright Chromium against Docker UI while walking Template Author → Testing Tab → Run preview → SSE success. Fixture: `prepareCdpMvpGoldenDraft` (Demo Retail Letterhead + `CDP-MVP-DATASET-01`). Brand: REDBC via `switchBrand`. Screenshots under `frontend/e2e/evidence/CDP-E2E-T08/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Template Author (`10000003`) |
| Brands | REDBC (required); GREENBC optional / non-blocking per BDD-CDP-PREV-003 |
| Fixture | CDP MVP golden DRAFT with successful render dataset |
| Surfaces in scope | Testing tab data sets + Preview progress success dialog |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `E2E_BASE_URL=http://127.0.0.1:5173 pnpm -C frontend exec playwright test e2e/CDP-E2E-T08-preview-artifact-download.spec.ts e2e/CDP-E2E-T08-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **3/3 passed** |
| Stage 7 re-run: `E2E_BASE_URL=http://127.0.0.1:5173 … CDP-E2E-T08-uiux-evidence.spec.ts --workers=1` | **1/1 passed** (~8.4s; 2026-07-10) |
| Functional `CDP-E2E-T08-preview-artifact-download.spec.ts` | **2/2 passed** (PREV-001 + PREV-002) |
| UIUX `CDP-E2E-T08-uiux-evidence.spec.ts` | **1/1 passed** (PREV-003 capture) |

## Screenshot inventory (3)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T08/screenshots/01-testing-tab-data-sets-redbc-1920x1080.png` | Dev workspace — Template testing / Test data sets | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T08/screenshots/02-preview-progress-success-redbc-1920x1080.png` | **Preview success** — Download DOCX/PDF + expires copy | REDBC | 1920×1080 |
| 3 | `CDP-E2E-T08/screenshots/03-preview-success-workspace-redbc-1920x1080.png` | Full workspace with success dialog open | REDBC | 1920×1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01, 03 — Red Bank logo + brand switcher + Templates nav + contained workspace |
| Preview success dialog (Download DOCX/PDF + TTL) | **PASS** | 02 — **closes T13 gap**; green check + brand primary download CTAs + “Expires in …” |
| Workspace tab shell + single action rail | **PASS** | 01/03 — Full test / Submit for testing on testing tab rail; preview opens dialog (not inline form) |
| English-first i18n | **PASS** | All captured surfaces EN (`Generating preview`, `Download DOCX/PDF`, `Expires in`) |
| No text overflow / overlap at 1920 | **PASS** | No clipping/overlap on dialog or table frames |
| Tokens / no brand wash | **PASS** | Brand red on primary CTAs / active nav; white OA baseline; success progress uses semantic green |
| Entity display (no UUID primary) | **PASS** | Dataset name `CDP-MVP-DATASET-01`; template title human-readable |
| Dual-brand REDBC / GREENBC | **PASS (REDBC)** | REDBC required and captured; GREENBC optional per BDD-CDP-PREV-003 / T12 owns dual-brand golden |
| A11y spot check (preview success) | **PASS** | Dialog heading/role; Download links; Close dismiss; success check visible |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### Product defects fixed in this slice (blocking success path — Stage 6)

1. **Async preview ID mismatch** — `AsyncPreviewOrchestrator` allocated `previewId` A for SSE/download URLs while `PreviewGenerationService.testGenerate` persisted record under new UUID B → late SSE replay miss + artifact GET 500. Fixed by passing orchestrator ID into generation.
2. **Absolute SSE `streamUrl`** — controller built `http://host:8080/...` which breaks browser same-origin fetch via frontend proxy. Fixed to relative `/api/management/v1/templates/.../progress-stream`.

### 🟡 Suggestion (should improve)

1. **Success dialog title still “Generating preview”** — Frame 02/03 keep the in-progress title after completion. Prefer a success title (e.g. “Preview ready”) when `phase === 'success'`. _Rule: state completeness / hierarchy. Non-blocking._ Component: `PreviewProgressDialog.vue` / `templates.previewProgress.title`.

2. **Download CTAs as underlined links on primary buttons** — Spec correctly asserts `role=link` for download; visual underline on brand-primary buttons is slightly noisy. Prefer button-styled anchors without default link underline. _Rule: interaction polish. Non-blocking._ Frames 02–03.

### 🟢 Nice to have

1. GREENBC capture of success dialog (optional; deferred to CD-E2E-T12 dual-brand golden / not blocking T08).
2. Active workspace tab underline still uses Element Plus default blue under REDBC — optional brand-token alignment (same pattern as T05/T06).
3. Focus-visible ring evidence on Download DOCX / Close.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (3) |
| Preview success frame with Download DOCX/PDF | **PASS** (closes P12 T13 gap) |
| Dual-brand REDBC (GREENBC optional) | **PASS** — REDBC captured; GREENBC N/A for T08 |
| Critical UIUX / a11y blockers on CDP T08 surfaces | **None** |
| **Overall stage 7 (T08)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T08-preview-artifact-download.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T08-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/template-testing-api.ts`, `cdp-mvp-golden-api.ts`, `uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*`)
- BDD: `docs/behavior/preview-success-artifact-download-journey.md`
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
