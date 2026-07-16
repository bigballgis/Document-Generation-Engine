# CE-U15 UIUX Evidence Manifest — Lifecycle Stepper + Go fix

**Task:** CE-U15 / Task Master **#91** — Dev workspace lifecycle Stepper + publish readiness Go-fix deep links  
**Slice:** `ce-u15-lifecycle-stepper` (`feat/ce-u15-lifecycle-stepper`)  
**Worktree:** `D:/working/DGE-ce-u15-lifecycle-stepper`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-17  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (Critical = 0; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `CE-U15-lifecycle-stepper.spec.ts` | **5/5 passed** (see `CE-U15-manifest.md`) |
| Stage 7 evidence: `CE-U15-lifecycle-stepper-uiux-evidence.spec.ts` | **3/3 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (DRAFT stepper / publish readiness Go fix / post-Go-fix bindings / dashboard Tasks) | **0 critical** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U15-lifecycle-stepper-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 12 passed (1.4m)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-draft-stepper-redbc-1920x1080.png` | REDBC | DRAFT `#dev-workspace` — Workflow progress Stepper (Draft current) |
| 1b | `01b-lifecycle-stepper-crop-redbc-1920x1080.png` | REDBC | `[data-testid=lifecycle-stepper]` crop |
| 1c | `01c-dev-workspace-crop-redbc-1920x1080.png` | REDBC | `#dev-workspace` crop |
| 1d | `01d-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-draft-stepper-greenbc-1920x1080.png` | GREENBC | DRAFT stepper dual-brand |
| 2b | `02b-lifecycle-stepper-crop-greenbc-1920x1080.png` | GREENBC | Stepper crop (teal current pill) |
| 2c | `02c-dev-workspace-crop-greenbc-1920x1080.png` | GREENBC | `#dev-workspace` crop |
| 2d | `02d-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `03-publish-readiness-gofix-redbc-1920x1080.png` | REDBC | PENDING_RELEASE publish readiness + English **Go fix** |
| 3b | `03b-stepper-pending-release-crop-redbc-1920x1080.png` | REDBC | Stepper — Pending release current |
| 3c | `03c-dev-workspace-publish-crop-redbc-1920x1080.png` | REDBC | `#dev-workspace` publish readiness crop |
| 3d | `03d-go-fix-anchor-crop-redbc.png` | REDBC | `publish-gate-go-fix-ANCHOR_INTEGRITY` crop |
| 3e | `03e-workspace-actions-crop-redbc-1920x1080.png` | REDBC | `.workspace-tab-shell__actions` (Confirm go-live) |
| 3f | `03f-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 4 | `04-publish-readiness-gofix-greenbc-1920x1080.png` | GREENBC | Publish readiness + Go fix dual-brand |
| 4b | `04b-stepper-pending-release-crop-greenbc-1920x1080.png` | GREENBC | Stepper pending-release crop |
| 4c | `04c-go-fix-anchor-crop-greenbc.png` | GREENBC | Go fix control crop |
| 4d | `04d-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 5 | `05-post-gofix-bindings-redbc-1920x1080.png` | REDBC | Post Go-fix → Template design / Bindings |
| 5b | `05b-dev-workspace-bindings-crop-redbc-1920x1080.png` | REDBC | Bindings surface crop |
| 6 | `06-post-gofix-bindings-greenbc-1920x1080.png` | GREENBC | Post Go-fix bindings dual-brand |
| 6b | `06b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 7 | `07-dashboard-tasks-no-stepper-redbc-1920x1080.png` | REDBC | Dashboard Tasks `?queue=TEST` — no stepper |
| 7b | `07b-tasks-section-crop-redbc-1920x1080.png` | REDBC | `#tasks-section` crop |
| 8 | `08-dashboard-tasks-no-stepper-greenbc-1920x1080.png` | GREENBC | Dashboard Tasks dual-brand — no stepper |
| 8b | `08b-tasks-section-crop-greenbc-1920x1080.png` | GREENBC | Tasks crop |
| 8c | `08c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |

Path prefix: `frontend/e2e/evidence/CE-U15/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–02, 03–04, 05–06, 07–08 |
| Logo / brand header switch | ✅ | 01d Red Bank; 02d / 04d / 06b / 08c Green Bank |
| LSS-001 DRAFT Workflow progress Stepper | ✅ | 01 / 01b / 02 / 02b — Draft `aria-current`; English title |
| Stepper above WorkspaceTabShell; no CTAs on stepper | ✅ | Spec `assertStepperOrientationOnly`; Confirm go-live stays on action rail (03e) |
| LSS-004 publish readiness + Go fix | ✅ | 03 / 03d / 04 / 04c — English **Go fix** |
| Post Go-fix → design/bindings | ✅ | 05 / 06 — Template design + Bindings selected |
| LSS-010 dashboard Tasks stepper-free | ✅ | 07 / 08 — zero `lifecycle-stepper` / `[data-ce-u15-stepper]` |
| No horizontal overflow @1920 | ✅ | Spec `assertNoViewportOverflow` on all surfaces |
| a11y smoke (critical axe) | ✅ | 9/9 + inline critical axe on changed surfaces |
| English-first copy | ✅ | Workflow progress / Draft…Published / Go fix / Confirm go-live |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | None | — |

### Notes (non-blocking)

1. **Go fix** uses link-blue text on the publish-gate checklist row (not brand-primary fill) — readable, English-first; consistent with secondary deep-link affordance next to Informational tags.
2. Stepper completed markers use brand primary (REDBC red / GREENBC teal); current step uses brand-tinted pill — dual-brand differentiation is clear at 1920.
3. Confirm go-live remains on `.workspace-tab-shell__actions` only; stepper is orientation-only (U15-D3 / workspace tab shell constitution).
4. Helpers: `CE_U15_VIEWPORT` 1920×1080 + `captureCeU15Screenshot` / `captureCeU15LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
5. Spec: `frontend/e2e/CE-U15-lifecycle-stepper-uiux-evidence.spec.ts`.
6. No merge / no new deploy performed (stage 7 handoff only). No product Done claim.

## Next

**Stage 8 — `architecture-reviewer`**
