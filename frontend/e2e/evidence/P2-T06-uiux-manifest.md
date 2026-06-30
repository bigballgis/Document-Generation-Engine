# P2-T06 UIUX Evidence Manifest

**Task:** Phase B full master revision history — hub pagination + historical revision detail (P2-T06)  
**Reviewer:** e2e-test-engineer (UIUX evidence capture)  
**Date:** 2026-07-01  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (evidence captured; visual review delegated to e2e-uiux-reviewer)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/P2-T06-uiux-evidence.spec.ts --config playwright.docker.config.ts` | **1/1 passed** (~6.8s) |

**Setup:** `prepareDemoMasterWithReplaceHistory` seeds demo master with 2+ revision lines (current replacement + historical approved seed line).

## Screenshot inventory (6)

| # | File | View / state | Brand / locale |
| --- | --- | --- | --- |
| 1 | `screenshots/01-master-hub-revision-lines-redbc-1440x900.png` | Master package hub — paginated revision-lines table (2+ rows) | REDBC / en |
| 2 | `screenshots/02-master-hub-revision-lines-greenbc-1440x900.png` | Same hub after brand switch | GREENBC / en |
| 3 | `screenshots/03-historical-revision-detail-redbc-1440x900.png` | Historical revision detail — read-only hint, no submit-for-review CTA | REDBC / en |
| 4 | `screenshots/04-historical-revision-detail-greenbc-1440x900.png` | Same historical detail after brand switch | GREENBC / en |
| 5 | `screenshots/05-master-hub-revision-lines-zhcn-1440x900.png` | Hub with zh-CN locale (修订线 title + table) | REDBC / zh-CN |
| 6 | `screenshots/06-historical-revision-detail-zhcn-1440x900.png` | Historical detail zh-CN (历史 badge + read-only hint) | REDBC / zh-CN |

## Coverage checklist

| Requirement | Status | Evidence |
| --- | --- | --- |
| Hub with 2+ revision rows — REDBC + GREENBC | ✅ | 01, 02 |
| Historical revision detail read-only — REDBC + GREENBC | ✅ | 03, 04 |
| zh-CN locale — hub + historical detail (≥2 screenshots) | ✅ | 05, 06 |
| 1440×900 desktop viewport | ✅ | All filenames |

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/e2e/P2-T06-uiux-evidence.spec.ts` | UIUX screenshot capture spec |
| `frontend/e2e/helpers/uiux-evidence.ts` | P2-T06 screenshot helpers + `switchLocale` |
| `frontend/e2e/evidence/P2-T06-uiux-manifest.md` | This manifest |

## References

- Functional baseline: `frontend/e2e/master-revision-two-page.spec.ts`
- BDD: BDD-MASTER-REVISION-NAV-001 Phase B
- `docs/product/catalog-navigation-ux.md` § Master revision history — Phase B
