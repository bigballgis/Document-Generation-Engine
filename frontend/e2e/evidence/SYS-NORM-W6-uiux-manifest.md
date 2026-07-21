# SYS-NORM-W6-UIUX Evidence Manifest — D1 brands / legal entities retirement

**Task:** SYS-NORM Wave 6 / Task Master **#150** — DocumentBrand / LegalEntity hard retirement (ADR-0071)  
**Slice:** `sys-norm-d1-brands` (`feat/sys-norm-d1-brands`)  
**Worktree:** `D:/working/DGE-sys-norm-d1-brands`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-21  
**Viewport:** 1440×900 (standard) + spot-check 1024×768  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS_WITH_NOTES** (Critical = 0; dual-brand visual review complete; durable Playwright evidence package still recommended)

## Surfaces checked

| # | Surface | Route / state | Brands |
| --- | --- | --- | --- |
| 1 | SurfaceRetiredView — document brands bookmark | `/governance/document-brands` | REDBC + GREENBC |
| 2 | SurfaceRetiredView — legal entities bookmark | `/governance/legal-entities` | REDBC + GREENBC |
| 3 | Security nav trim (expanded) | Shell nav — Security & activity | REDBC + GREENBC |
| 4 | Legal holds still usable | `/governance/legal-holds` | REDBC |
| 5 | Responsive spot-check (entities retired) | `/governance/legal-entities` @1024×768 | GREENBC |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `SYS-NORM-W6-d1-brands.spec.ts` | **16/16 passed** (handoff) |
| Stage 7 Playwright `SYS-NORM-W6-uiux-evidence.spec.ts` | **Not present** — visual review via browser against docker @4173 |
| `a11y-smoke.spec.ts` (Stage 7 re-run) | **Not re-run this stage** — retired surface a11y checked manually (skip link, labels, contrast) |

> Note: Repo lacks `frontend/e2e/SYS-NORM-W6-uiux-evidence.spec.ts` and `captureSysNormW6*` helpers. Stage 7 used live browser capture at docker stack. Recommend adding a durable evidence spec before claiming Done (non-blocking for UIUX merge bar when Critical = 0).

## Screenshot inventory

Path prefix: `frontend/e2e/evidence/SYS-NORM-W6/screenshots/`

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `w6-brands-retired-redbc-1440x900-final.png` | REDBC | Document brands retired + expanded nav |
| 2 | `w6-brands-retired-greenbc-1440x900-final.png` | GREENBC | Document brands retired + expanded nav |
| 3 | `w6-entities-retired-redbc-1440x900.png` | REDBC | Legal entities retired + Letterhead + Legal holds CTAs |
| 4 | `w6-entities-retired-greenbc-1024x768.png` | GREENBC | Legal entities retired @1024×768 |
| 5 | `w6-nav-security-trim-redbc-expanded-1440x900.png` | REDBC | Security & activity = Activity log + Legal holds only |
| 6 | `w6-brands-retired-redbc-1440x900.png` | REDBC | Brands retired (earlier capture) |

Legal holds usability also verified live @1440 (Create legal hold empty + header affordances; fluid layout) — screenshot not separately numbered.

Manifest (this file): `frontend/e2e/evidence/SYS-NORM-W6-uiux-manifest.md`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Honest retired surface (not Forbidden / not catalog) | ✅ | Title + message; no brand/entity tables |
| English-first copy via i18n | ✅ | `retiredSurface.*` keys; page title English |
| Letterhead CTA | ✅ | `Open Letterhead templates` → `/masters` |
| Legal holds CTA on entities retired | ✅ | `Open Legal holds` → `/governance/legal-holds` |
| Legal holds product surface usable | ✅ | h1 Legal holds; Create legal hold; fluid layout |
| Security nav trim (no Document brands / Legal entities) | ✅ | Expanded nav: Activity log + Legal holds only |
| Dual-brand REDBC + GREENBC | ✅ | `--brand-primary` #DB0011 / #00847F; logo switch |
| No text overlap / clipping @1440 | ✅ | Visual + layout metrics |
| No horizontal overflow @1440 / 1024 | ✅ | scrollWidth ≤ clientWidth |
| a11y basics (skip link, labeled controls) | ✅ | Skip to main content; CTAs named |
| a11y-smoke green (automated) | ⚠️ | Not re-run in Stage 7 |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | `SurfaceRetiredView` uses `min-height: 100vh` inside `ManagementShell`, so content bottom exceeds viewport (~header height) and produces a needless vertical scrollbar (`scrollHeight` ≈ 993 @900 / ≈861 @768). Prefer `min-height: 100%` or `calc(100vh - shell chrome)`. | OA layout / `SurfaceRetiredView.vue` |
| 🟡 Suggestion | Retired title is an `el-result` `DIV`, not a content `h1`/`role=heading` (`titleIsHeading: false`). Document title is fine; main landmark lacks a dedicated heading for screen readers. | a11y heading hierarchy / `SurfaceRetiredView.vue` |
| 🟢 Nice to have | Breadcrumb shows only “Home” on retired bookmarks (no logicalRoute) — acceptable for gone surfaces; optional “Retired” crumb. | Shell breadcrumb |
| 🟢 Nice to have | Add `SYS-NORM-W6-uiux-evidence.spec.ts` + `captureSysNormW6*` for durable dual-brand PNG inventory under `evidence/SYS-NORM-W6/screenshots/`. | Evidence machinery |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Stage 6 functional journeys already cover bookmark honesty, nav trim, Letterhead/Legal holds reachability, and brand orthogonality (16/16).
2. Shell brand theme remains UI chrome only — copy on retired brands surface states this explicitly (English).
3. Legal holds remains under Security & activity and renders empty-state create affordances — not folded into brand retirement.

## Stage 7 gate

**PASS_WITH_NOTES** — Critical = 0; ready for Stage 8 architecture-reviewer. Address 🟡 layout/heading polish optionally; persist Playwright UIUX evidence package when convenient.
