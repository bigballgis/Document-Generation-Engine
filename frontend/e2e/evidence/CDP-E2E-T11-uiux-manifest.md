# CDP-E2E-T11 UIUX Evidence Manifest

**Task:** CD-E2E-T11 / BDD-CDP-AUDIT-001…002 — Audit admin Activity log filter + export smoke  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport (primary):** **1920×1080** (desktop-first; CD-2 audit query slice)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080` (healthz/4173 **200**)  
**Placement:** ISOLATED `D:/working/DGE-cdp-e2e-t11-audit` / `feat/cdp-e2e-t11-audit`  
**Spec:** `frontend/e2e/CDP-E2E-T11-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T11-audit-query.spec.ts` (Stage 6 — 2/2)  
**CD-3 / CD-HARD-T06:** Stage 6 functional re-evidence **2026-07-20** — **2/2 passed** (docker workers=1); manifest `frontend/e2e/evidence/CD-HARD-T06-functional-manifest.md` (pointer `docs/behavior/cd-hard-t06-audit-export-reevidence.md`). UIUX screenshots below remain T11 baseline unless Stage 7 re-captures.  
**Verdict:** **PASS** (5/5 screenshots; REDBC @1920 Activity log filters + table/empty + view-only + export confirm; bank OA OK; no Critical UIUX blockers)

## Capture method

AUDIT_ADMIN → `/audit` → Management activity tab → view-only banner + filters + table → apply Event type “Template go-live” → empty/filtered list → Export confirm dialog (Cancel without download; functional E2E covers download). Brand: REDBC via `switchBrand`. Screenshots under `frontend/e2e/evidence/CDP-E2E-T11/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Audit Admin (`E2E_AUDIT_ADMIN`) |
| Brands | REDBC (required); GREENBC optional / deferred to T12 |
| Surfaces in scope | Activity log shell, filters card, Management table/empty, Export confirm |
| View-only | `audit.viewOnly.banner` tag + empty-state copy; no My to-dos group |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `… CDP-E2E-T11-audit-query.spec.ts --config playwright.docker.config.ts --workers=1` | **2/2 passed** (upstream) |
| Stage 7: `pnpm -C frontend exec playwright test e2e/CDP-E2E-T11-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (~5.9s) |
| Stage 7 visual review | Screenshots 01–05 inspected @1920 REDBC; stack **200** on `:4173` / `:8080` |

## Screenshot inventory (5)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T11/screenshots/01-activity-log-shell-view-only-filters-redbc-1920x1080.png` | Activity log shell — view-only banner, filters, Management table | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T11/screenshots/02-activity-log-filters-card-redbc-1920x1080.png` | Filters card detail — Event type / Request ID / From / To / Apply | REDBC | 1920×1080 |
| 3 | `CDP-E2E-T11/screenshots/03-activity-log-filtered-table-or-empty-redbc-1920x1080.png` | After Event type = Template go-live — empty state + view-only copy | REDBC | 1920×1080 |
| 4 | `CDP-E2E-T11/screenshots/04-activity-log-export-confirm-redbc-1920x1080.png` | Export confirm over Activity log workspace | REDBC | 1920×1080 |
| 5 | `CDP-E2E-T11/screenshots/05-activity-log-export-confirm-detail-redbc-1920x1080.png` | Export confirm detail — Cancel / Download export | REDBC | 1920×1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01/03 — Red Bank logo + brand switcher + SECURITY & ACTIVITY / Activity log active |
| View-only banner (audit admin) | **PASS** | 01/03 — “View only — no actions” tag beside Export; empty-state reinforces view-only |
| Filters (Event type select + Apply) | **PASS** | 02 — Event type `AppSearchSelect`; Request ID text; datetime From/To; Apply primary / Reset text |
| Table / empty after filter | **PASS** | 01 unfiltered rows; 03 empty after Template go-live with EN empty copy |
| Export affordance + confirm | **PASS** | 01 Export primary; 04–05 “Export activity records” + Cancel / Download export |
| Fluid catalog layout | **PASS** | `AppPageLayout` fluid — table uses full content width at 1920 |
| English-first i18n | **PASS** | Activity log, filters, tabs, empty, export dialog EN |
| No text overflow / overlap at 1920 | **PASS** | No clipping on filters, table headers, empty illustration, or dialog actions |
| Density / spacing rhythm | **PASS** | Moderate OA density; white baseline; clear header / filter / table hierarchy |
| Tokens / no brand wash | **PASS** | Brand red on Export / Apply / active nav; white OA baseline; info tag for view-only |
| Entity display (no UUID primary) | **PASS (with note)** | `EntityLinkCell` on Template column; frame 01 shows truncated id-like labels when display name unresolved — see 🟡 |
| Dual-brand REDBC / GREENBC | **PASS (REDBC)** | REDBC required and captured; GREENBC optional per T12 dual-brand golden |
| A11y spot check (filters + dialog) | **PASS** | H1 Activity log; labeled filters; Export + Apply; dialog title + Cancel / Download export; contrast OK on white baseline |

## Stable selectors (functional + UIUX)

| Selector | Purpose |
| --- | --- |
| heading `/^activity log$/i` | Page title |
| text `/view only — no actions/i` | Audit admin view-only banner |
| tab `/management activity/i` | Management activity tab |
| `.filters-card` | Filter toolbar card |
| form-item Event type + `.el-select` | Event type filter (`AppSearchSelect`) |
| `button` name `/^apply filters$/i` | Apply filters |
| `button` name `/^export$/i` | Export primary |
| `.el-message-box` + `/export activity records/i` | Export confirm |
| `button` name `/^download export$/i` | Confirm download |
| `.app-data-table` / `.el-empty` | Table or empty state |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Template column truncated id-like labels (frame 01)** — Unfiltered Management rows show Template cells as truncated alphanumeric (`ad0e7ecc…`) rather than human-readable package names when resolution falls back. Prefer always resolving display name (or externalId) before truncating UUID. _Rule: entity display / no raw UUID primary. Non-blocking for T11 smoke (EntityLinkCell present; dedicated UX-ENTITY-DISPLAY evidence exists)._ Component: `AuditConsoleView.vue` + `resolveAuditTemplateDisplay` / `EntityLinkCell`.

2. **Export confirm position (frames 04–05)** — Element Plus message-box appears toward the upper-left over the shell rather than a clear viewport-centered modal. Actions remain usable; optional: ensure message-box uses standard centered overlay so Export confirm does not visually collide with the brand bar / nav. _Non-blocking._

### 🟢 Nice to have

1. GREENBC capture of Activity log + Export confirm (optional; deferred to CD-E2E-T12).
2. Active Management tab underline uses Element Plus default blue while brand primary is red — optional token alignment for tab ink.
3. Event type placeholder truncation in narrow filter cells (“Filter by event t…”) — widen Event type column slightly at fluid layouts.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (5) |
| Activity log filters + view-only (AUDIT-001) | **PASS** (frames 01–03) |
| Export confirm affordance (AUDIT-002 UI) | **PASS** (frames 04–05) |
| Dual-brand REDBC (GREENBC optional) | **PASS** — REDBC captured; GREENBC N/A for T11 |
| Critical UIUX / a11y blockers on CDP T11 surfaces | **None** |
| **Overall stage 7 (T11)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T11-audit-query.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T11-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*`, task id `CDP-E2E-T11`)
- View: `frontend/src/views/audit/AuditConsoleView.vue`
- BDD: `docs/behavior/audit-admin-query-journey.md`
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
