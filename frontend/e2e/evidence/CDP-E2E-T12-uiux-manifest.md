# CDP-E2E-T12 UIUX Evidence Manifest

**Task:** CD-E2E-T12 / TaskMaster #23 / BDD-CDP-I18N-001…002 — zh-CN locale + REDBC/GREENBC dual-brand golden surfaces  
**Capture author:** e2e-test-engineer (Stage 6)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport (primary):** **1920×1080**  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080` (healthz/4173 **200**)  
**Placement:** ISOLATED `D:/working/DGE-cdp-e2e-t12-i18n-brands` / `feat/cdp-e2e-t12-i18n-brands`  
**Functional spec:** `frontend/e2e/CDP-E2E-T12-i18n-brands.spec.ts`  
**UIUX capture spec:** `frontend/e2e/CDP-E2E-T12-uiux-evidence.spec.ts`  
**Verdict:** **PASS** (7/7 screenshots; ≥3 zh-CN key surfaces; REDBC + GREENBC @1920; no Critical UIUX blockers)

## Capture method

GLOBAL_ADMIN → shell `.locale-switcher` → **zh-CN** → key golden surfaces (Dashboard TEST queue, Templates catalog, External services hub) → `.brand-switcher` **REDBC** + **GREENBC**. Screenshots under `frontend/e2e/evidence/CDP-E2E-T12/screenshots/`. Extends T01 EN dual-brand baseline; does **not** overwrite T01 Verdict.

| Item | Value |
| --- | --- |
| Roles exercised | Global Admin (`E2E_ADMIN` / `10000001`) |
| Locale | zh-CN via `switchLocale` (UI) |
| Brands | REDBC + GREENBC via `switchBrand` (UI; localized option labels 红色银行 / 绿色银行) |
| Surfaces | Dashboard `?queue=TEST`, `/templates`, `/api/policies` |
| Cross-ref | [CDP-E2E-T01-uiux-manifest.md](./CDP-E2E-T01-uiux-manifest.md) (EN dual-brand) |

## Test execution

| Command | Result |
| --- | --- |
| `pnpm -C frontend exec playwright test e2e/CDP-E2E-T12-i18n-brands.spec.ts --config playwright.docker.config.ts --workers=1` | **2/2 passed** (Stage 6) |
| `pnpm -C frontend exec playwright test e2e/CDP-E2E-T12-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (Stage 6; 7 screenshots) |
| Stage 7 visual review | **PASS** — frames 01–07 inspected; capture requirements unchanged (no re-run) |

## Screenshot inventory (7)

| # | File | View / state | Locale | Brand | Viewport |
| --- | --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T12/screenshots/01-dashboard-test-queue-zhcn-redbc-1920x1080.png` | Dashboard TEST queue — 我的任务 / 待我测试 | zh-CN | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T12/screenshots/02-brand-header-zhcn-redbc-1920x1080.png` | Brand lockup — 红色银行 文档生成系统 | zh-CN | REDBC | crop |
| 3 | `CDP-E2E-T12/screenshots/03-templates-catalog-zhcn-redbc-1920x1080.png` | Templates catalog — 模板 | zh-CN | REDBC | 1920×1080 |
| 4 | `CDP-E2E-T12/screenshots/04-external-services-hub-zhcn-redbc-1920x1080.png` | External services hub — 对外服务概览 | zh-CN | REDBC | 1920×1080 |
| 5 | `CDP-E2E-T12/screenshots/05-dashboard-test-queue-zhcn-greenbc-1920x1080.png` | Dashboard TEST queue after brand switch | zh-CN | GREENBC | 1920×1080 |
| 6 | `CDP-E2E-T12/screenshots/06-brand-header-zhcn-greenbc-1920x1080.png` | Brand lockup — 绿色银行 文档生成系统 | zh-CN | GREENBC | crop |
| 7 | `CDP-E2E-T12/screenshots/07-templates-catalog-zhcn-greenbc-1920x1080.png` | Templates catalog after brand switch | zh-CN | GREENBC | 1920×1080 |

## BDD traceability

| Scenario | Requirement | Evidence |
| --- | --- | --- |
| **BDD-CDP-I18N-001** | ≥3 distinct zh-CN key surfaces readable | Frames **01** (Dashboard), **03** (Templates), **04** (External services) — chrome/nav/headings/filters in 简体中文; no layout break from longer CN strings |
| **BDD-CDP-I18N-002** | REDBC + GREENBC dual-brand @1920 | REDBC **01–04** (红 / `#DB0011` accents); GREENBC **05–07** (绿 / `#00847F` accents); logo crops **02** vs **06** |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01, 03–05, 07 |
| Dual-brand REDBC / GREENBC | **PASS** | 01 vs 05; 02 vs 06; 03 vs 07 — primary CTAs / active tabs follow brand |
| Logo / brand lockup switch | **PASS** | 02 红色银行 (red); 06 绿色银行 (teal) |
| zh-CN chrome readability | **PASS** | Nav, H1, tabs, filters, empty-state copy all CN; sharp at 1920 |
| Catalog fluid width / density | **PASS** | 03, 07 — table uses content width; no wasted gutters / no overlap |
| External services empty state | **PASS** | 04 — 暂无待关注项 + primary 浏览模板 |
| Entity display (no UUID primary) | **PASS** | Template names / task objects human-readable; External ID secondary column |
| No text overflow / overlap @1920 | **PASS** | Visual review of 01, 03–05, 07 |
| Tokens / no brand wash | **PASS** | Brand on logo, primary buttons, active accents; white OA baseline |
| English-first product strings (data) | **N/A (fixture)** | Domain fixture names remain EN; UI chrome is zh-CN (slice scope) |
| Locale + brand switchers coexist | **PASS** | Header: brand select + 简体中文 + Global Admin |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **GREENBC nav active tint (carry-forward from T01)** — On GREENBC dashboard (frame 05), sidebar active “我的任务” can read as muted grey rather than full brand teal, while header logo / tab underline correctly switch to green. Templates catalog (frame 07) shows stronger teal active nav. Same dual-brand theming note as T01 — **non-blocking for T12**. _Rule: dual-brand theming._ Evidence: `05-dashboard-test-queue-zhcn-greenbc-1920x1080.png`.

2. **Pagination chrome residual EN** — Dashboard pagination shows “Total 11” while surrounding UI is zh-CN. Low severity Element Plus / locale wiring polish; does not block I18N-001 surface readability. Evidence: frames 01, 05.

### 🟢 Nice to have

1. Session role chip remains “Global Admin” (E2E identity label) under zh-CN — acceptable for fixture; optional role-label i18n later.

## Stage 7 disposition

- Capture requirements **unchanged** — no UIUX evidence re-run required.
- Do **not** mark CD-E2E-T12 / CD-2 Done from this review alone (remaining pipeline stages apply).
- Do **not** merge from this stage.

## Helper fixes noted (Stage 6; accepted)

| Change | Why |
| --- | --- |
| `helpers/nav.ts` `managementNav` | Match `管理导航` after zh-CN switch |
| `helpers/uiux-evidence.ts` `switchBrand` | Match `红色银行` / `绿色银行` option labels under zh-CN |
| `CdpE2eCd2DecisionTaskId` | Add `CDP-E2E-T12` for evidence dirs |
