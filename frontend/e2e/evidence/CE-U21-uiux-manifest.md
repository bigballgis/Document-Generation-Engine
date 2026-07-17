# CE-U21 UIUX Evidence Manifest — Draft anchor concurrency

**Task:** CE-U21 / Task Master **#95** — per-anchor localDraft keys + binding save 409 conflict UX  
**Slice:** `ce-u21-draft-anchor-concurrency` (`feat/ce-u21-draft-anchor-concurrency`)  
**Worktree:** `D:/working/DGE-ce-u21-draft-anchor-concurrency`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-17  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (Stage 5 DEPLOY_OK; Stage 6 4/4)  
**Verdict:** **PASS** (Critical = 0; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `CE-U21-draft-anchor-concurrency.spec.ts` | **4/4 passed** (see `CE-U21-manifest.md`) |
| Stage 7 evidence: `CE-U21-draft-anchor-concurrency-uiux-evidence.spec.ts` | **2/2 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (recovery banner; conflict MessageBox; style-picker excluded) | **0 critical** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U21-draft-anchor-concurrency-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 11 passed (50.7s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-recovery-banner-editor-redbc-1920x1080.png` | REDBC | Binding editor + per-anchor recovery banner (`FOL_HEADER`) |
| 1b | `01b-recovery-banner-crop-redbc-1920x1080.png` | REDBC | Banner crop — Restore red / Discard secondary |
| 1c | `01c-binding-editor-context-redbc-1920x1080.png` | REDBC | Controlled structured editor context |
| 1d | `01d-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-recovery-banner-editor-greenbc-1920x1080.png` | GREENBC | Same recovery surface dual-brand |
| 2b | `02b-recovery-banner-crop-greenbc-1920x1080.png` | GREENBC | Banner crop — Restore teal |
| 2c | `02c-binding-editor-context-greenbc-1920x1080.png` | GREENBC | Editor context dual-brand |
| 2d | `02d-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `03-conflict-dialog-redbc-1920x1080.png` | REDBC | Binding editor + 409 MessageBox (Reload / Keep editing) |
| 3b | `03b-conflict-dialog-crop-redbc-1920x1080.png` | REDBC | MessageBox crop — Reload red |
| 3c | `03c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 4 | `04-conflict-dialog-greenbc-1920x1080.png` | GREENBC | Conflict MessageBox dual-brand |
| 4b | `04b-conflict-dialog-crop-greenbc-1920x1080.png` | GREENBC | MessageBox crop — Reload teal |
| 4c | `04c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |

Path prefix: `frontend/e2e/evidence/CE-U21/screenshots/` (**14** files on disk)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–02 recovery; 03–04 conflict |
| Logo / brand header switch | ✅ | 01d Red Bank; 02d / 04c Green Bank |
| Per-anchor recovery banner (LR-C2 surface) | ✅ | 01 / 02 — title, draft/server timestamps, Restore / Discard |
| Brand primary on Restore | ✅ | 01b red; 02b teal (`rgb(0,132,127)` asserted) |
| Binding 409 conflict MessageBox | ✅ | 03 / 04 — **Binding updated elsewhere** + **Reload** / **Keep editing** |
| Not publish version-conflict copy | ✅ | Spec asserts `publish version conflict` count = 0; English binding-specific body |
| Brand primary on Reload | ✅ | 03b red; 04b teal (`rgb(0,132,127)` asserted) |
| Binding editor OA shell context | ✅ | 01 / 03 — WorkspaceTabShell Bindings + side-by-side preview |
| No horizontal page overflow @1920 | ✅ | Spec `assertNoViewportOverflow` on recovery + conflict |
| a11y smoke (critical axe) | ✅ | 9/9 + scoped banner/MessageBox critical = 0 |
| English-first copy | ✅ | Unsaved local draft found / Binding updated elsewhere / Reload / Keep editing |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Shared toolbar **style picker** (`[data-testid=style-picker]`) still lacks accessible name on the EP combobox — same pre-existing gap as CE-U20; Stage 7 scoped axe excludes it when analyzing the editor chrome. FE should add `:aria-label` on `StructuredContentEditorToolbar.vue` | OA a11y / shared toolbar — not introduced by CE-U21 |
| 🟢 Nice to have | EP `ElMessageBox.confirm` sits as a compact overlay (not a large centered dialog). Readable and brand-correct; optional polish: slightly wider dialog / stronger focus trap affordance for concurrency decisions | `bindingVersionConflict.ts` → `presentBindingVersionConflict` |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Recovery path uses dirty-guard **Discard** (retains per-anchor localStorage) then remount — matches Stage 6 DAC-002; banner scrolled into view before capture.
2. Conflict body uses `api.error.template.bindingVersionConflict` (“This binding was updated elsewhere…”) — distinct from publish version-conflict strings.
3. Helpers: `CE_U21_VIEWPORT` 1920×1080 + `captureCeU21Screenshot` / `captureCeU21LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
4. Spec: `frontend/e2e/CE-U21-draft-anchor-concurrency-uiux-evidence.spec.ts`.
5. No merge / no new deploy / no product Done claim (stage 7 handoff only).

## Next

**Stage 8 — `architecture-reviewer`**
