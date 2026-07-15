# CE-U06 UIUX Evidence Manifest — master anchor position overview

**Task:** CE-U06 / Task Master **#88** — MasterAnchorPositionOverview (documentSequence + selection + editable displayLabel)  
**Slice:** `ce-u06-master-anchor-context` (`feat/ce-u06-master-anchor-context`)  
**Worktree:** `D:/working/DGE-ce-u06-master-anchor-context`  
**Tip:** `fdf92cb65105f9d2b8214415c2ee44314ff6bdbd`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-15  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (no redeploy)  
**Verdict:** **FAIL** — one Critical dialog-init defect (must fix before merge)

## Test execution

| Command | Result |
| --- | --- |
| `e2e/a11y-smoke.spec.ts` | **9/9 passed** |
| `e2e/CE-U06-master-anchor-context-uiux-evidence.spec.ts` | **2/2 passed** (~17.7s) — capture + overflow asserts green; Critical found on visual review |

```powershell
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U06-master-anchor-context-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-anchor-overview-writable-redbc-1440x900.png` | REDBC | Writable DRAFT revision — design tab + ordered anchor table + Edit label |
| 1b | `01b-anchor-overview-crop-redbc-1440x900.png` | REDBC | Overview crop — Position / Anchor ID / Display label / Actions; selected row |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-edit-display-label-dialog-redbc-1440x900.png` | REDBC | **First open** Edit display label dialog (empty field + required error) |
| 2b | `02b-edit-dialog-crop-redbc-1440x900.png` | REDBC | Dialog crop — Anchor ID `HEADER` readonly; Display label blank + error |
| 3 | `03-anchor-overview-writable-greenbc-1440x900.png` | GREENBC | Dual-brand writable overview |
| 3b | `03b-anchor-overview-crop-greenbc-1440x900.png` | GREENBC | Overview crop — teal primary Edit label |
| 3c | `03c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 4 | `04-edit-display-label-dialog-greenbc-1440x900.png` | GREENBC | **Second open** dialog (label prefilled — contrast with #2) |
| 4b | `04b-edit-dialog-crop-greenbc-1440x900.png` | GREENBC | Dialog crop — Display label `HEADER` + enabled Save (teal) |
| 5 | `05-anchor-overview-pending-review-readonly-redbc-1440x900.png` | REDBC | PENDING_REVIEW — no Edit label column |
| 5b | `05b-pending-review-overview-crop-redbc-1440x900.png` | REDBC | Readonly overview crop |
| 6 | `06-anchor-overview-historical-readonly-redbc-1440x900.png` | REDBC | Historical line — hint + no edit actions |
| 6b | `06b-historical-overview-crop-redbc-1440x900.png` | REDBC | Historical overview crop |

Path prefix: `frontend/e2e/evidence/CE-U06/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1440 | ✅ | 01–04 + crops |
| Logo / brand header switch | ✅ | 01c Red Bank; 03c Green Bank |
| Ordered position table (1-based) + English-first copy | ✅ | 01 / 01b / 03 / 03b — Position / Anchor ID / Display label |
| Selection highlight on row click | ✅ | 01b / 03b selected row background |
| Writable Edit label control | ✅ presence | 01 / 03 Actions column |
| Edit dialog seeds current displayLabel | ❌ | 02 / 02b empty + required on **first** open; 04b OK on reopen |
| PENDING_REVIEW / historical fail-closed (no edit) | ✅ | 05 / 05b / 06 / 06b |
| No horizontal overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke (critical axe) | ✅ | 9/9 |
| Shared table vocabulary (`AppDataTable` / `TableColumnHeader`) | ✅ | Component review + crops |
| Tokens / no brand wash | ✅ | White OA baseline; brand on accents only |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🔴 Critical | **Edit display label dialog does not seed `initialDisplayLabel` on first open.** Parent mounts dialog with `v-if="editingAnchor"` and sets `editLabelOpen=true` in the same tick; `MasterAnchorDisplayLabelDialog` only copies the label in a `watch(modelValue)` **without `immediate`**, so the first open shows a blank field + English “Display label is required.” and disabled Save. Re-open after Cancel (false→true on already-mounted dialog) works — see REDBC 02b vs GREENBC 04b. Blocks BDD-CE-U06-MAC-003 happy path. | `MasterAnchorDisplayLabelDialog.vue`; `MasterRevisionDetailView.vue` / `useMasterRevisionDetailController.ts`; OA form state completeness |
| 🟡 Suggestion | Empty **Revision overview** card (no change summary on fresh DRAFT) sits above the anchors table and pushes the primary CE-U06 surface down at 1440×900. Not a blocker; consider collapsing empty overview or elevating the anchors card. | `MasterRevisionDetailWorkspace.vue`; density / hierarchy |

## Notes

1. Helpers: `CE_U06_VIEWPORT` 1440×900 + `captureCeU06Screenshot` / `captureCeU06LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-U06-master-anchor-context-uiux-evidence.spec.ts`.
3. Surfaces: `MasterAnchorPositionOverview.vue`, `MasterAnchorDisplayLabelDialog.vue`, `MasterRevisionDetailWorkspace.vue`.
4. **Fix routing:** `frontend-engineer` — seed label on mount (`immediate` watch and/or `onMounted` / initialize `form.displayLabel` from props); add regression asserting first-open input value equals current `displayLabel`.
5. No merge / no doc-sync / no Wave 2 / no redeploy (stage 7 handoff only).
