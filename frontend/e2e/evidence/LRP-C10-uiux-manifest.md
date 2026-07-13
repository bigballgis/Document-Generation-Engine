# LRP-C10 UIUX Evidence Manifest

**Task:** LR-C10 / TaskMaster #25 — Master DOCX upload UX (drag hint, progress, inline errors)  
**Slice:** `lrp-c10-upload-ux` (`feat/lrp-c10-upload-ux`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport:** Playwright `Desktop Chrome` default (~1280 CSS px; full-page captures 1280×837–1024). Dialogs are fixed `520px` — no overflow risk at 1440/1920.  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (Stage 5 DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on create/replace upload surfaces)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `pnpm -C frontend exec playwright test e2e/LRP-C10-upload-ux.spec.ts --config playwright.docker.config.ts --workers=1` | **4/4 passed** (upstream) + LRP-A3 **5/5** regression |
| Stage 7 review method | Visual inspection of on-disk Stage 6 evidence PNGs + static review of `MasterUploadDialog.vue` / `MasterReplaceFileDialog.vue` / i18n / `validateMasterDocxUpload.ts` |

No separate `LRP-C10-uiux-evidence.spec.ts` — functional spec already captures golden frames under `evidence/LRP-C10-upload-ux/`.

### Surface coverage (handoff LR-C10-A/B)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| LR-C10-A create — drag hint, 50 MB tip, progress during upload | `MasterUploadDialog` on Masters catalog | `C10-A-create-progress` |
| LR-C10-A replace — drag hint + progress during transfer | `MasterReplaceFileDialog` on package hub | `C10-A-replace-progress` |
| LR-C10-B client precheck — non-docx / oversized inline errors | Replace dialog `.upload-error` | `C10-B-client-precheck` |
| LR-C10-B server `docxTooLarge` — translated inline, no raw envelope | Replace dialog + retry-enabled primary | `C10-B-server-inline` |

## Screenshot inventory (4)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-C10-upload-ux/C10-A-create-progress.png` | Create dialog — drag zone + tip + file staged + progress bar + “Uploading letterhead…” | REDBC | en |
| 2 | `LRP-C10-upload-ux/C10-A-replace-progress.png` | Replace dialog — current file + drag zone + progress + primary loading | REDBC | en |
| 3 | `LRP-C10-upload-ux/C10-B-client-precheck.png` | Replace dialog — inline “Only .docx letterhead files are accepted.”; Replace disabled | REDBC | en |
| 4 | `LRP-C10-upload-ux/C10-B-server-inline.png` | Replace dialog — inline “The uploaded DOCX exceeds the maximum allowed size.”; Replace enabled for retry | REDBC | en |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Dialog density / label-top form / primary vs secondary footer | ✅ | Frames 1–4; `width="520px"`; Cancel + primary Upload/Replace |
| Drag affordance (`el-upload` drag + dashed zone + icon + i18n hint) | ✅ | Frames 1–4; `masters.upload.dragHint` |
| 50 MB / .docx limit tip (English-first) | ✅ | Frames 1–4; `masters.upload.fileHint` |
| Progress visibility (`master-upload-progress`, `aria-live="polite"`) | ✅ | Frames 1–2; striped `el-progress` + label |
| Inline error clarity (`role="alert"`, translated, no raw envelope/HTML) | ✅ | Frames 3–4; functional asserts vs `RAW_ENVELOPE` |
| Client precheck disables submit; server error keeps dialog + enables retry | ✅ | Frame 3 disabled; frame 4 enabled primary |
| Tokens / CSS vars (muted tip, danger error; primary via EP `type="primary"`) | ✅ | Scoped SCSS uses `--text-muted` / `--color-danger`; brand primary on actions |
| No text overflow / clipping / overlap in dialog @ captured desktop width | ✅ | Frames 1–4 (520px dialog well within 1280 shell) |
| English-first i18n keys | ✅ | `dragHint`, `fileHint`, `progressLabel`, `errorTooLarge`, `errorDocxOnly`, `api.error.master.docxTooLarge` |
| Dual-brand REDBC + GREENBC golden shots | ⚠️ Partial | REDBC only in evidence set; primary wired via brand CSS vars — GREENBC not screenshot-verified this slice |
| Explicit 1440×900 / 1920 viewport capture | ⚠️ Partial | Desktop Chrome default (~1280); dialog-fixed width mitigates layout risk |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Server error + green file-list checkmark conflict** (`MasterReplaceFileDialog.vue` / EP `el-upload` file list) — Frame `C10-B-server-inline` shows a green success check on `retail-letterhead-replacement.docx` while `.upload-error` reports size exceeded. Client accepted the file; server rejected it. Mild mixed signal. Prefer mapping file-list status to `error` when `serverErrorKey` is set, or hide the success icon on server failure.  
   Rule: frontend-oa-design §State completeness / quality bar (no confusing competing status).

2. **Dual-brand GREENBC evidence missing** — All four frames are REDBC. Upload chrome is brand-neutral; primary buttons use `type="primary"` → `--brand-primary`. Capture one create or replace frame after `switchBrand(page, 'GREENBC')` in a follow-up evidence pass for parity with LRP-C9 / CDP manifests.  
   Rule: frontend-oa-design §Definition of done (both brands verified).

### 🟢 Nice to have

1. **Gated progress shows 0%** — Stage 6 holds the network until progress is asserted; frames show `0%` + “Uploading letterhead…”. Prefer indeterminate when percent is still `0`/`null` at start so users do not read “stuck at zero” (code already supports indeterminate when `uploadProgress == null`).
2. Optional dedicated `LRP-C10-uiux-evidence.spec.ts` with `uiux-evidence.ts` helpers + explicit 1440×900 (or 1920) viewport for archive consistency.
3. zh-CN spot-check frames for drag hint / error strings (en sufficient for this slice).

## Files for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/LRP-C10-upload-ux.spec.ts` | Functional + screenshot capture (LR-C10-A/B) |
| `frontend/e2e/evidence/LRP-C10-upload-ux/*.png` | Four golden frames |
| `frontend/e2e/evidence/LRP-C10-uiux-manifest.md` | This manifest |
| `frontend/src/components/masters/MasterUploadDialog.vue` | Create upload surface |
| `frontend/src/components/masters/MasterReplaceFileDialog.vue` | Replace upload surface |

## Notes for architecture / doc-sync

- Stage 7 verdict: **PASS_WITH_NOTES** — ready for architecture-reviewer (stage 8).
- No ADR / permission-matrix / API contract change required for UIUX presentation.
- Doc-sync (later, on MAIN after merge) should record Stage 7 PASS_WITH_NOTES, evidence paths above, and Stage 6 4/4 + LRP-A3 5/5.
- Optional non-blocking polish (file-list error status; GREENBC frame) → route to `frontend-engineer` if product wants follow-up.

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/LRP-C10-upload-ux.spec.ts`
- Prior upload validation: `frontend/e2e/evidence/LRP-A3-upload-validation/manifest.md`
