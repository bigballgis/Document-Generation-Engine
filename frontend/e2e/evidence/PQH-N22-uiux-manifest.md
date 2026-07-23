# PQH-N22 UIUX Evidence Manifest — Catalog Edit/More row actions

**Task:** PQH N22 / Task Master **#162** — `TableEditMoreActions` Edit/More on Asset Library, Legal Holds, API Invocations; Users/Groups regression  
**Slice:** `pqh-n22-catalog-row-actions` (`feat/pqh-n22-catalog-row-actions`)  
**Worktree:** `D:/working/DGE-pqh-n22-catalog-row-actions`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-23  
**Viewport:** 1440×900 (standard)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (Stage 5 `dc9229ca`; no redeploy)  
**Tip at Stage 7 close:** `c0cc57a0` (+ validation refresh)
**Verdict:** **PASS** (Critical = 0; dual-brand evidence complete)

## Surfaces checked

| # | Surface | Pattern | Brands |
| --- | --- | --- | --- |
| 1 | Asset Library (`/library/assets`) | More-only + Disable under More | REDBC + GREENBC |
| 2 | Legal Holds (`/governance/legal-holds`) | More-only + Release under More | REDBC + GREENBC |
| 3 | API Invocations (`/api/invocations`) | Open summary (edit slot) + API settings under More | REDBC + GREENBC |
| 4 | Users / Groups (`/entitlement/users`, `/entitlement/groups`) | Edit + More regression | REDBC + GREENBC |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `PQH-N22-catalog-row-actions.spec.ts` | **7/7 passed** (`f1c47287`) |
| Stage 7 full UIUX suite (re-run 2026-07-23): a11y + PQH-N22 + CE-E02 + CE-G04 + SYS-NORM-W3 | **21/21 passed** (~3.8m) |
| Stage 7 evidence: `PQH-N22-uiux-evidence.spec.ts` | **4/4 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/PQH-N22-uiux-evidence.spec.ts `
  e2e/CE-E02-asset-library-uiux-evidence.spec.ts `
  e2e/CE-G04-legal-hold-uiux-evidence.spec.ts `
  e2e/SYS-NORM-W3-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 21 passed (3.8m)
```

## Screenshot inventory

Path prefix: `frontend/e2e/evidence/PQH-N22/screenshots/` (**27** files on disk)

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-asset-library-more-menu-redbc-1440x900.png` | REDBC | Asset Library — More open → Disable |
| 1b | `01b-asset-more-only-redbc-crop.png` | REDBC | More-only actions crop (no Edit) |
| 1c | `01c-asset-disable-menu-redbc-crop.png` | REDBC | Disable menu item crop |
| 2 | `01-asset-library-more-menu-greenbc-1440x900.png` | GREENBC | Asset Library dual-brand |
| 2b | `01b-asset-more-only-greenbc-crop.png` | GREENBC | More-only crop |
| 2c | `01c-asset-disable-menu-greenbc-crop.png` | GREENBC | Disable menu crop |
| 3 | `02-legal-hold-more-menu-redbc-1440x900.png` | REDBC | Legal Holds — More open → Release |
| 3b | `02b-legal-more-only-redbc-crop.png` | REDBC | More-only actions crop |
| 3c | `02c-legal-release-menu-redbc-crop.png` | REDBC | Release menu item crop |
| 4 | `02-legal-hold-more-menu-greenbc-1440x900.png` | GREENBC | Legal Holds dual-brand |
| 4b | `02b-legal-more-only-greenbc-crop.png` | GREENBC | More-only crop |
| 4c | `02c-legal-release-menu-greenbc-crop.png` | GREENBC | Release menu crop |
| 5 | `03-invocations-more-settings-redbc-1440x900.png` | REDBC | Invocations — Open summary + More → API settings |
| 5b | `03b-invocations-edit-more-redbc-crop.png` | REDBC | Open summary + More crop |
| 5c | `03c-invocations-settings-menu-redbc-crop.png` | REDBC | API settings menu crop |
| 6 | `03d-invocations-more-settings-greenbc-1440x900.png` | GREENBC | Invocations dual-brand |
| 6b | `03e-invocations-settings-menu-greenbc-crop.png` | GREENBC | API settings menu crop |
| 7 | `04-users-edit-more-redbc-1440x900.png` | REDBC | Users — Edit + More → Disable / Reset / Delete |
| 7b | `04b-users-edit-more-redbc-crop.png` | REDBC | Edit / More crop |
| 7c | `04c-users-more-menu-redbc-crop.png` | REDBC | Users More menu crop |
| 8 | `04-users-edit-more-greenbc-1440x900.png` | GREENBC | Users dual-brand |
| 8b | `04b-users-edit-more-greenbc-crop.png` | GREENBC | Edit / More crop |
| 8c | `04c-users-more-menu-greenbc-crop.png` | GREENBC | Users More menu crop |
| 9 | `04d-groups-edit-more-redbc-1440x900.png` | REDBC | Groups — Edit + More → Disable |
| 9b | `04e-groups-edit-more-redbc-crop.png` | REDBC | Groups Edit / More crop |
| 10 | `04d-groups-edit-more-greenbc-1440x900.png` | GREENBC | Groups dual-brand |
| 10b | `04e-groups-edit-more-greenbc-crop.png` | GREENBC | Groups Edit / More crop |

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| TableEditMoreActions shared pattern (no ad-hoc button stacks) | ✅ | All four catalogs; `data-testid="table-edit-more-actions"` |
| More-only when no primary edit (Asset / Legal Hold) | ✅ | 01b / 02b — Edit absent; More → Disable / Release |
| Edit + More when primary edit exists (Users / Groups) | ✅ | 04 / 04d — Edit (brand) + More (muted) |
| Invocations: Open summary primary + settings under More | ✅ | 03 / 03b / 03c — not a second top-level Settings button |
| Dual-brand REDBC (#DB0011) + GREENBC (#00847F) | ✅ | Spec `--brand-primary` assert + logo/header switch |
| Logo / brand header switch | ✅ | Full-page 01–04 pairs — Red Bank / Green Bank |
| No horizontal page overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke (critical axe) | ✅ | 9/9 |
| English-first copy | ✅ | More / Disable / Release / Open summary / API settings / Edit |
| Released Legal Hold rows omit empty Actions chrome | ✅ | 02 — Released rows show blank Actions (no ghost Edit) |
| Fluid catalog width (no wasted contained gutter) | ✅ | Asset / Legal / Invocations / Users / Groups full shell width |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Asset Library and Legal Holds Actions columns sit tight against the preceding datetime / Created-by cells at 1440×900. When More opens, the popover correctly overlays the row, but the More-only trigger crop is very small — consider a slightly wider Actions min-width so the More label has clearer breathing room beside truncated timestamps. | OA density / `TableEditMoreActions` on Asset Library + Legal Holds — not a readability blocker |
| 🟡 Suggestion | Invocations row shows a native/EP tooltip bubble reading **"Open summary More"** (concatenated titles) while the More menu is open. Prefer a single purposeful tooltip or none when both controls are adjacent. | Interaction polish — `TableEditMoreActions` / Invocations row |
| 🟢 Nice to have | Users More menu items (Disable / Reset password / Delete) render in light gray; readable but tertiary hierarchy could align more clearly with OA muted-token weight (pre-existing; same as SYS-NORM-W1 note). | `TableEditMoreActions` + EP dropdown |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Dedicated N22 evidence under `frontend/e2e/evidence/PQH-N22/`; related catalog UIUX specs (CE-E02 / CE-G04 / SYS-NORM-W3) updated to open More before asserting teleported menu items (`3c594eb9`).
2. Helpers: `PQH_N22_VIEWPORT` 1440×900 + `capturePqhN22Screenshot` / `capturePqhN22LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
3. Spec: `frontend/e2e/PQH-N22-uiux-evidence.spec.ts`.
4. Product Vue unchanged in this Stage 7 pass (evidence + harness only).

## Stage 7 gate

**PASS** — ready for Stage 8 architecture-reviewer (no UIUX blockers for merge of #162).
