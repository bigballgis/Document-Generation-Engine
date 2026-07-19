# nav-missing-icons UIUX Evidence Manifest — Shell nav icons

**Task / Slice:** `nav-missing-icons` → **Done** (MAIN merge `137a115f` / feature tip `47784667`; worktree **REMOVED**)  
**Branch (historical):** `feat/nav-missing-icons`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-19  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (Stage 5 + Stage 10 DEPLOY_OK)  
**Verdict:** **PASS** (Critical = 0; dual-brand expanded + collapsed verified)

## Behavior SoT

- `docs/behavior/nav-missing-icons.md` (BDD-NAV-ICON-001…004)
- Icon map: `frontend/src/components/layout/useManagementShell.ts` — `asset-library` → `FolderOpened`, `legal-holds` → `Lock`
- Shell chrome: `ManagementShellNav.vue` / `ManagementShellNav.scss`

## Coordination with Stage 6

| Artifact | Status |
| --- | --- |
| Functional: `nav-missing-icons.spec.ts` | **2/2 passed** (reconfirmed this stage, ~5.3s) |
| UIUX evidence harness: `nav-missing-icons-uiux-evidence.spec.ts` | **On MAIN** (merged with slice tip `47784667`); Stage 7 visual review also used Docker browser + CDP geometry |

## Test / evidence execution

| Command / method | Result |
| --- | --- |
| Stage 6 reconfirm: `nav-missing-icons.spec.ts` + `playwright.docker.config.ts` | **2/2 passed** |
| Browser visual review @1440×900 on `:4173` | **Done** — REDBC + GREENBC, expanded + collapsed |
| CDP geometry asserts (left edge, icon column, height, SVG paths) | **Pass** — see metrics below |
| `a11y-smoke.spec.ts` | **Not re-run this pass** (nav-scoped review; shell a11y not newly regressing from icon map-only change) |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test e2e/nav-missing-icons.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 2 passed (5.3s) — 2026-07-19
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `nav-missing-icons-01-expanded-redbc-1440x900.png` | REDBC | Shell + expanded nav — Asset library folder + Legal holds lock |
| 2 | `nav-missing-icons-03-collapsed-redbc-1440x900.png` | REDBC | Icon-only collapsed rail (64px) |
| 3 | `nav-missing-icons-01-expanded-greenbc-1440x900.png` | GREENBC | Dual-brand expanded — Green Bank logo + teal active |
| 4 | `nav-missing-icons-03-collapsed-greenbc-1440x900.png` | GREENBC | Icon-only collapsed dual-brand |

Path prefix: `frontend/e2e/evidence/nav-missing-icons/screenshots/` (**4** files on disk)  
Manifest: `frontend/e2e/evidence/nav-missing-icons-uiux-manifest.md`

Temp capture source (Cursor browser): `%LOCALAPPDATA%\Temp\cursor\screenshots\nav-missing-icons-*.png`

## Geometry metrics (expanded REDBC @1440)

| Item | row x | row h | icon x | icon size | svg paths |
| --- | --- | --- | --- | --- | --- |
| Templates (sibling) | 11.25 | 39.09 | 26.25 | 15×15 | 1 |
| Asset library | 11.25 | 39.09 | 26.25 | 15×15 | 1 |
| Activity log (sibling) | 11.25 | 39.09 | 26.25 | 15×15 | 1 |
| Legal holds | 11.25 | 39.09 | 26.25 | 15×15 | 2 |

- Left-edge drift: **0**  
- Icon-column drift: **0**  
- Height density drift: **0**  
- Horizontal overflow: scrollWidth === clientWidth (**1425**)  
- Collapsed: aside width **64px**; Asset library / Legal holds `nav-item--icon-only` with icons 14×14, pathCount ≥ 1; aria-label retained  

GREENBC: `data-brand=GREENBC`, `--brand-primary=#00847F`, greenbc logo SVG applied; same icon geometry.

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1440 | ✅ | frames 1–4 |
| Logo / brand header switch | ✅ | Red Bank → Green Bank logo + primary |
| Expanded nav icon alignment vs siblings | ✅ | CDP table + frames 1, 3 |
| Collapsed icon-only path | ✅ | frames 2, 4 + CDP |
| No missing glyphs / empty icon slots | ✅ | SVG pathCount ≥ 1; visual |
| No overlap / clipping @1440 | ✅ | visual + overflow check |
| Density regression vs bank OA shell | ✅ | shared `.nav-item` / `.nav-item__icon` tokens |
| English-first nav labels | ✅ | Asset library / Legal holds |
| a11y smoke (full suite) | ⚠️ | Not re-run; icon-map-only change; collapsed buttons keep aria-label |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | No 🔴 Critical | — |
| — | No 🟡 Suggestion specific to this slice | — |
| 🟢 Nice to have | Wire harness helpers into shared `uiux-evidence.ts` patterns (optional) so Stage 7 matches CE-E02/IBL-C2 automation style — harness file already on MAIN | evidence harness |
| 🟢 Nice to have | Collapsed rail can scroll shorter viewports — some icon rows may sit below the fold (pre-existing shell); not introduced by this icon mapping | `ManagementShellNav.scss` overflow |

## Verdict / merge gate (UIUX dimension only)

| Gate | Value |
| --- | --- |
| **Verdict** | **PASS** |
| **Critical** | **0** |
| **merge_go (UIUX)** | **true** |

**Merge recommendation (UIUX):** Approve for Stage 11 merge from UIUX perspective. Do **not** claim product Done / flip #3b/#5a / activate IBL-B7 from this review.

## Notes

1. Stage 7 was read-only on app Vue/TS; product icon-map fix landed in frontend-engineer stage and merged to MAIN (`137a115f`).  
2. Evidence files under `frontend/e2e/evidence/nav-missing-icons/` (screenshots + this manifest) are on MAIN after Stage 11.  
3. `nav-missing-icons-uiux-evidence.spec.ts` is present on MAIN; Stage 7 also collected geometry + screenshots against the live Docker stack.
