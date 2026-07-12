# OPS-PASTE-BINDING-SEAM UIUX Evidence Manifest

**Task / slice:** `ops-paste-binding-seam` (BDD-OPS-PASTE-BINDING-001 / checklist #5b paste↔binding)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-12  
**Viewport:** Playwright Desktop Chrome default **1280×720** (full-page off; viewport captures). Dialog fixed `560px` — no overflow risk at 1440/1920.  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on paste-blocked dialog, residue tag/alert surfaces, or English-first copy)

## Test execution

| Command / method | Result |
| --- | --- |
| Stage 6: `pnpm -C frontend exec playwright test e2e/ops-paste-binding-seam.spec.ts --config playwright.docker.config.ts --workers=1` | **6/6 passed** (upstream) |
| Stage 7 review method | Visual inspection of on-disk Stage 6 evidence PNGs + static review of `PasteCleaningSummaryDialog.vue`, `TemplateAuthoringBindingsPanel.vue` residue alert/tag, en/zh-CN i18n keys |

No separate `ops-paste-binding-seam-uiux-evidence.spec.ts` — functional spec already captures golden frames under `evidence/ops-paste-binding-seam/`.

### Surface coverage (BDD-OPS-PASTE-BINDING-001)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| S1a object paste BLOCKED; Accept disabled | `PasteCleaningSummaryDialog` on binding editor | `S1a-object-paste-blocked` |
| S1b absolute positioning paste BLOCKED; Accept disabled | same dialog | `S1b-absolute-paste-blocked` |
| S2 clean paste Accept → residue cleared / VALID | bindings list after save | `S2-clean-paste-accepted` |
| S3/S4 unresolved residue → incompatible + paste-blocked tag | bindings list (`binding-paste-residue-tag`) | `S3-S4-paste-residue-binding-ui` |
| S5 clear residue recovers VALID / no tag | bindings list after clear+save | `S5-paste-residue-cleared` |
| S5b clean rewrite clears residue | bindings list after Accept+save | `S5b-clean-rewrite-cleared` |

## Screenshot inventory (6)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `ops-paste-binding-seam/S1a-object-paste-blocked.png` | Paste cleaning summary — blocked alert + counts + table; Accept disabled | REDBC | en |
| 2 | `ops-paste-binding-seam/S1b-absolute-paste-blocked.png` | Same blocked dialog for absolute positioning paste | REDBC | en |
| 3 | `ops-paste-binding-seam/S2-clean-paste-accepted.png` | Bindings list — HEADER **VALID** after clean Accept+save; no paste-blocked tag | REDBC | en |
| 4 | `ops-paste-binding-seam/S3-S4-paste-residue-binding-ui.png` | Bindings list — incompatible validation + paste residue tag asserted | REDBC | en |
| 5 | `ops-paste-binding-seam/S5-paste-residue-cleared.png` | Bindings list — VALID + Configured; residue tag gone | REDBC | en |
| 6 | `ops-paste-binding-seam/S5b-clean-rewrite-cleared.png` | Bindings list — VALID after clean rewrite path | REDBC | en |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Dialog density / intro + counts + table / Cancel · Undo · primary Accept | ✅ | Frames 1–2; `width="560px"`; `PasteCleaningSummaryDialog.vue` |
| Blocked fail-closed: error alert + Accept `:disabled="blocked"` | ✅ | Frames 1–2; functional asserts Accept disabled; unit test `PasteCleaningSummaryDialog.test.ts` |
| Tokens / CSS vars (muted intro, EP error alert, brand primary on Accept/Save) | ✅ | Scoped SCSS uses `--text-muted`; primary via EP `type="primary"` |
| Residue list tag (`binding-paste-residue-tag`, danger, English “Paste blocked”) | ✅ | Functional S3 asserts + Frame 4 capture after Back; i18n `pasteResidue.blockedTag` |
| Residue editor alert + clear action (error, non-closable, show-icon) | ✅ (functional) / ⚠️ Partial visual | Asserted in S3/S5 before Back; **no dedicated PNG of editor alert** in inventory |
| No text overflow / clipping / overlap in dialog @ 1280×720 | ✅ | Frames 1–2; dialog well within shell; EP dialog teleports above side-by-side preview (no elevated z-index on preview) |
| English-first i18n keys (+ zh-CN parity present) | ✅ | `pasteSummary.*`, `pasteResidue.*`, `paste.summary.blocked`; zh-CN mirrors in `zh-CN.ts` |
| Dual-brand REDBC + GREENBC golden shots | ⚠️ Partial | REDBC only; primary wired via brand CSS vars — GREENBC not screenshot-verified this slice |
| Explicit 1440×900 / `uiux-evidence.ts` helpers | ⚠️ Partial | Default 1280×720; dialog-fixed width mitigates layout risk |
| a11y of blocked Accept + residual warnings | ✅ with notes | Disabled primary when blocked; error `el-alert` + danger tag; no dedicated `a11y-smoke` re-run cited for this slice |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Validation column truncates incompatible label** (`TemplateAuthoringBindingsPanel.vue` — `el-table-column` `validationStatus` **width="140"**) — Frame `S3-S4-paste-residue-binding-ui` shows **“Incompatible c…”** instead of full `Incompatible content type`. Residue severity still carried by the danger **Paste blocked** tag (functional assert), but the validation cell alone is hard to read at 1280. Prefer wider column, `show-overflow-tooltip`, or a compact status tag.  
   Rule: frontend-oa-design §Quality bar (no harmful text clipping at target desktop widths).

2. **Status column density with dual tags** (width **120** + Configured + Paste blocked wrap) — Works via `.binding-status-cell` flex-wrap, but cramped next to truncated validation. Consider `min-width` ≥160 when residue tag can appear.  
   Rule: frontend-oa-design §Components / table density.

3. **Dual-brand GREENBC evidence missing** — All six frames are REDBC. Paste chrome is brand-neutral; primary actions use `--brand-primary`. Capture one blocked-dialog and one residue-list frame after `switchBrand(page, 'GREENBC')` for parity with F7 / LRP manifests.  
   Rule: frontend-oa-design §Definition of done (both brands verified).

4. **Editor residue alert not in golden frames** — S3 opens the editor, asserts `binding-paste-residue-alert`, then **Back** and screenshots the list only. Add a capture while the error alert + “Clear paste residue on save” are visible for auditability.  
   Rule: e2e-uiux-reviewer evidence completeness for changed surfaces.

### 🟢 Nice to have

1. Optional dedicated `ops-paste-binding-seam-uiux-evidence.spec.ts` using `uiux-evidence.ts` (1440×900 + `switchBrand`).
2. Wire disabled Accept to blocked alert via `aria-describedby` (or visible helper text under the footer) so AT users get the block reason without leaving the control.
3. zh-CN spot-check frames for `pasteSummary.blockedTitle` / `pasteResidue.blockedTag` (en sufficient for this slice).
4. S1a/S1b frames are near-duplicates — one blocked-dialog golden + one absolute-variant crop would shrink evidence set without losing coverage.

## Files for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/ops-paste-binding-seam.spec.ts` | Functional + screenshot capture (S1–S5b) |
| `frontend/e2e/evidence/ops-paste-binding-seam/*.png` | Six golden frames |
| `frontend/e2e/evidence/OPS-PASTE-BINDING-SEAM-uiux-manifest.md` | This manifest |
| `frontend/src/components/authoring/PasteCleaningSummaryDialog.vue` | Blocked dialog UI |
| `frontend/src/components/templates/TemplateAuthoringBindingsPanel.vue` | Residue alert / tag / clear action |
| `frontend/src/i18n/locales/en.ts` (+ `zh-CN.ts`) | English-first copy + locale parity |

## Verdict rationale

Paste blocked dialog, disabled Accept, English-first copy, OA shell density, and residue tag/list recovery states are acceptable for merge. Notes are evidence gaps (GREENBC, editor-alert frame, 1440 viewport) and mild truncation/density polish — **not** merge blockers. Functional Stage 6 **6/6 PASS** remains authoritative for behavior; this Stage 7 pass does **not** flip launch checklist #5b / merge / commit.
