# CE-U16 UIUX Evidence Manifest — Authoring path compression

**Task:** CE-U16 / Task Master **#92** — Design default Bindings + create Authoring path micro-wizard  
**Slice:** `ce-u16-authoring-path-compress` (`feat/ce-u16-authoring-path-compress`)  
**Worktree:** `D:/working/DGE-ce-u16-authoring-path-compress`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-17  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (Stage 5 DEPLOY_OK)  
**Verdict:** **PASS** (Critical = 0; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `CE-U16-authoring-path-compress.spec.ts` | **5/5 passed** (see `CE-U16-manifest.md`) |
| Stage 7 evidence: `CE-U16-authoring-path-compress-uiux-evidence.spec.ts` | **3/3 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (Master / default Bindings / Bindings step / Preview / after Skip) | **0 critical** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U16-authoring-path-compress-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 12 passed (54.9s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-authoring-path-master-redbc-1920x1080.png` | REDBC | Post-create Master step — Authoring path + lifecycle-stepper coexist |
| 1b | `01b-authoring-path-guide-crop-redbc-1920x1080.png` | REDBC | `[data-testid=authoring-path-guide]` crop |
| 1c | `01c-lifecycle-stepper-coexist-crop-redbc-1920x1080.png` | REDBC | `[data-testid=lifecycle-stepper]` crop (U15 coexistence) |
| 1d | `01d-master-panel-crop-redbc-1920x1080.png` | REDBC | Master confirm panel (Demo Retail Letterhead / HEADER) |
| 1e | `01e-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-authoring-path-master-greenbc-1920x1080.png` | GREENBC | Master step dual-brand |
| 2b | `02b-authoring-path-guide-crop-greenbc-1920x1080.png` | GREENBC | Authoring path guide crop (teal current pill) |
| 2c | `02c-lifecycle-stepper-coexist-crop-greenbc-1920x1080.png` | GREENBC | Lifecycle stepper crop |
| 2d | `02d-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `03-default-bindings-redbc-1920x1080.png` | REDBC | Daily Design default Bindings (no guide) |
| 3b | `03b-dev-workspace-bindings-crop-redbc-1920x1080.png` | REDBC | `#dev-workspace` Bindings crop |
| 3c | `03c-bindings-panel-crop-redbc-1920x1080.png` | REDBC | `.bindings-panel` crop |
| 4 | `04-default-bindings-greenbc-1920x1080.png` | GREENBC | Default Bindings dual-brand |
| 4b | `04b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 5 | `05-authoring-path-bindings-redbc-1920x1080.png` | REDBC | Authoring path Bindings step + guide |
| 5b | `05b-authoring-path-bindings-guide-crop-redbc-1920x1080.png` | REDBC | Guide crop (Bindings `aria-current`) |
| 6 | `06-authoring-path-bindings-greenbc-1920x1080.png` | GREENBC | Authoring path Bindings dual-brand |
| 6b | `06b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 7 | `07-authoring-path-preview-redbc-1920x1080.png` | REDBC | Preview → Testing / Preview runs |
| 7b | `07b-authoring-path-preview-guide-crop-redbc-1920x1080.png` | REDBC | Guide crop (Preview current) |
| 7c | `07c-dev-workspace-preview-crop-redbc-1920x1080.png` | REDBC | `#dev-workspace` Preview runs crop |
| 8 | `08-authoring-path-preview-greenbc-1920x1080.png` | GREENBC | Preview dual-brand |
| 8b | `08b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 9 | `09-skip-guide-workspace-redbc-1920x1080.png` | REDBC | After Skip — guide gone; Design/Bindings + stepper remain |
| 9b | `09b-dev-workspace-after-skip-crop-redbc-1920x1080.png` | REDBC | Workspace after skip crop |
| 10 | `10-skip-guide-workspace-greenbc-1920x1080.png` | GREENBC | After Skip dual-brand |
| 10b | `10b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |

Path prefix: `frontend/e2e/evidence/CE-U16/screenshots/` (27 files on disk)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–02, 03–04, 05–06, 07–08, 09–10 |
| Logo / brand header switch | ✅ | 01e Red Bank; 02d / 04b / 06b / 08b / 10b Green Bank |
| APC-003 Authoring path Master + U15 lifecycle-stepper coexistence | ✅ | 01 / 01b / 01c / 02 — distinct testids; both visible |
| Guide has no Submit/Approve/Publish CTAs | ✅ | Spec `assertAuthoringPathOrientationOnly`; Preview CTAs stay on Testing action rail (07) |
| APC-001 Design default Bindings (no guide) | ✅ | 03 / 03c / 04 — Bindings selected; guide count 0 |
| Authoring path Bindings step | ✅ | 05 / 06 — `authoringGuideStep=bindings` + Bindings panel |
| APC-004 Preview → testing / previewRuns | ✅ | 07 / 08 — Template testing + Preview runs selected |
| APC-005 Skip / Dismiss guide | ✅ | 09 / 10 — guide absent; Design/Bindings + stepper remain |
| No horizontal overflow @1920 | ✅ | Spec `assertNoViewportOverflow` on all surfaces |
| a11y smoke (critical axe) | ✅ | 9/9 + inline critical axe on changed surfaces |
| English-first copy | ✅ | Authoring path / Master…Preview / Skip guide / Dismiss / Confirm linked master |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | None | — |

### Notes (non-blocking)

1. **Authoring path** and **Workflow progress** are visually stacked (lifecycle above guide) with distinct English titles — no confusion with U15 product-state Stepper.
2. Guide primary **Next** uses brand primary (REDBC red / GREENBC teal); Skip/Dismiss remain secondary outline — hierarchy clear at 1920.
3. On Preview step, lifecycle CTAs (**Full test** / **Submit for testing**) remain on `.workspace-tab-shell__actions` only; guide shows Skip/Dismiss only (no Next on last step).
4. Helpers: `CE_U16_VIEWPORT` 1920×1080 + `captureCeU16Screenshot` / `captureCeU16LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
5. Spec: `frontend/e2e/CE-U16-authoring-path-compress-uiux-evidence.spec.ts`.
6. No merge / no new deploy performed (stage 7 handoff only). No product Done claim.

## Next

**Stage 8 — `architecture-reviewer`**
