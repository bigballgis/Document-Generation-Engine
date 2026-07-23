# PQH-N22 UIUX Evidence Manifest — Catalog Edit/More row actions

**Task:** PQH N22 / Task Master **#162** — Catalog `TableEditMoreActions` (Edit/More)  
**Slice:** `pqh-n22-catalog-row-actions` (`feat/pqh-n22-catalog-row-actions`)  
**Worktree:** `D:/working/DGE-pqh-n22-catalog-row-actions`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-23  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Base tip (handoff):** `f69c3f0c`  
**Verdict:** **PASS** (Critical = 0)

## Surfaces checked

| # | Surface | Brands | Pattern |
| --- | --- | --- | --- |
| 1 | Asset Library — More-only Disable | REDBC + GREENBC | More → teleported menu |
| 2 | Legal Holds — More-only Release | REDBC + GREENBC | More → teleported menu |
| 3 | API Invocations — Open summary + settings under More | REDBC + GREENBC | Edit slot + More |
| 4 | Users / Groups — Edit/More regression | REDBC + GREENBC | Edit + More |

## Spec fixes (Stage 6 CRITICAL follow-up)

UIUX evidence specs previously asserted Disable/Release on the row without opening More
(menu items teleport to `body`). Fixed to open More and scope `.el-dropdown-menu:visible`:

| Spec | Fix |
| --- | --- |
| `CE-E02-asset-library-uiux-evidence.spec.ts` | More → `asset-library-disable` |
| `CE-G04-legal-hold-uiux-evidence.spec.ts` | More → `legal-hold-release` |
| `SYS-NORM-W3-uiux-evidence.spec.ts` | Open summary via `table-edit-more-actions`; settings via visible More menu |
| `PQH-N22-uiux-evidence.spec.ts` | **New** dedicated dual-brand N22 evidence |

Shared contract: `data-testid="table-edit-more-actions"`.

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (handoff): `PQH-N22-catalog-row-actions.spec.ts` | **6/6 passed** (`f69c3f0c`) |
| Stage 7: `PQH-N22-uiux-evidence.spec.ts` + CE-E02 + CE-G04 + SYS-NORM-W3 UIUX | **12/12 passed** (~2.9m) |
| `a11y-smoke.spec.ts` (docker) | **9/9 passed** (~44s) |

```powershell
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/PQH-N22-uiux-evidence.spec.ts `
  e2e/CE-E02-asset-library-uiux-evidence.spec.ts `
  e2e/CE-G04-legal-hold-uiux-evidence.spec.ts `
  e2e/SYS-NORM-W3-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

## Screenshot inventory (PQH-N22)

Path: `frontend/e2e/evidence/PQH-N22/screenshots/`

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-asset-library-more-menu-redbc-1440x900.png` | REDBC | Asset Library More open → Disable |
| 1b | `01b-asset-more-only-redbc-crop.png` | REDBC | More-only Actions crop |
| 1c | `01c-asset-disable-menu-redbc-crop.png` | REDBC | Disable menu item |
| 1g | `01-*-greenbc-*` / `01b/01c-*-greenbc-*` | GREENBC | Dual-brand Asset Library |
| 2 | `02-legal-hold-more-menu-redbc-1440x900.png` | REDBC | Legal Holds More → Release |
| 2b/2c | `02b/02c-*-redbc-*` | REDBC | More-only + Release menu crops |
| 2g | `02-*-greenbc-*` | GREENBC | Dual-brand Legal Holds |
| 3 | `03-invocations-more-settings-redbc-1440x900.png` | REDBC | Invocations More → API settings |
| 3b | `03b-invocations-edit-more-redbc-crop.png` | REDBC | Open summary + More hierarchy |
| 3c | `03c-invocations-settings-menu-redbc-crop.png` | REDBC | API settings menu |
| 3d/3e | `03d/03e-*-greenbc-*` | GREENBC | Dual-brand Invocations More |
| 4 | `04-users-edit-more-redbc-1440x900.png` | REDBC | Users Edit/More + menu |
| 4b/4c | `04b/04c-*-redbc-*` | REDBC | Users crop + More menu |
| 4d/4e | `04d/04e-*-redbc-*` | REDBC | Groups Edit/More |
| 4g | `04-*-greenbc-*` | GREENBC | Users/Groups dual-brand |

Related refreshed captures (regression UIUX specs):

- `frontend/e2e/evidence/CE-E02-asset-library/screenshots/` (+ `01d` / `02b` More crops)
- `frontend/e2e/evidence/CE-G04-legal-hold/screenshots/` (+ `01e` / `03b` More crops)
- `frontend/e2e/evidence/SYS-NORM-W3/screenshots/` (+ `03c` / `03d` settings-via-More)

## Checklist (frontend-oa-design DoD)

| Check | Result | Evidence |
| --- | --- | --- |
| Token / OA shell | ✅ | White header, left nav, fluid catalogs |
| Both brands + logo | ✅ | REDBC `#DB0011` / GREENBC `#00847F` frames |
| No overflow @1440 | ✅ | Asserted in specs; screenshots clean |
| Edit/More hierarchy | ✅ | More-only catalogs; Invocations primary Open summary; Users Edit+More |
| Teleported menu usable | ✅ | Disable / Release / API settings under `.el-dropdown-menu:visible` |
| a11y smoke | ✅ | 9/9 |
| English-first | ✅ | Labels via i18n |
| No raw UUID primary | ✅ | Asset key / Hold ID / package names |

## Findings

### Critical
_None._

### Suggestion (High/Med)
1. **🟡 Med — EP `ElMessageBox` confirm not centered** (platform-known; CE-E02 / CE-G04). Buttons usable. Not introduced by N22.
2. **🟡 Med — Legal Holds Actions cell can show adjacent column bleed** when More menu is open (timestamp fragment under Release popper). Does not block More-first interaction.
3. **🟡 Low — Users More menu item gray weight** — readable; secondary hierarchy could align with OA tertiary tokens (same note as SYS-NORM-W1).

### Nice to have
- Capture Disable/Release confirm MessageBox dual-brand inside dedicated N22 evidence (covered by CE-E02 / CE-G04 refreshed specs).
- Optional polish if EP tooltip fires on Invocations Actions cell.

## Counts

| Severity | Count |
| --- | --- |
| Critical | **0** |
| High | **0** |
| Med (Suggestion) | **2** |
| Nice to have | **2** |

## Stage 8 notes

- No architecture boundary change; UI-only Actions column pattern reuse of `TableEditMoreActions`.
- Confirm no new shared-component API beyond existing `showEdit` / More slot usage.
- Platform MessageBox centering remains a cross-cutting CSS item (not N22-blocking).

## References

- `docs/behavior/pqh-n22-catalog-row-actions.md`
- `docs/architecture/management-ui-constitution.md`
- `.cursor/skills/frontend-oa-design/SKILL.md`
- `frontend/src/components/common/TableEditMoreActions.vue`
