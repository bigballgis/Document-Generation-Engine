# P12-BDD-RISK-PROMPT-UX-001 UIUX evidence manifest

**Slice:** P12-BDD-RISK-PROMPT-UX-001 (template-scoped risk-prompt config UX)  
**Spec:** `frontend/e2e/P12-BDD-RISK-PROMPT-UX-001-uiux-evidence.spec.ts`  
**Viewport:** 1440×900  
**Brands:** REDBC (primary), GREENBC (dual-brand spot check)

## Scenarios covered

| # | File | Surface | Brand |
| --- | --- | --- | --- |
| 1 | `screenshots/01-template-list-no-risk-prompt-panel-redbc-1440x900.png` | Template list — no risk-prompt panel (S11) | REDBC |
| 2 | `screenshots/02-create-dialog-collapsed-advanced-redbc-1440x900.png` | Create dialog — advanced collapse folded (S11) | REDBC |
| 3 | `screenshots/03-create-dialog-expanded-risk-prompt-redbc-1440x900.png` | Create dialog — optional return-reason section | REDBC |
| 4 | `screenshots/04-template-detail-risk-prompt-section-redbc-1440x900.png` | Create dialog — risk-prompt panel detail (S3/S12) | REDBC |
| 5 | `screenshots/05-template-detail-risk-prompt-section-greenbc-1440x900.png` | Same section — dual-brand parity | GREENBC |
| 6 | `screenshots/06-decision-dialog-filtered-categories-redbc-1440x900.png` | Test-fail decision — filtered categories (S2/S7) | REDBC |

Screenshot root: `frontend/e2e/evidence/P12-BDD-RISK-PROMPT-UX-001/screenshots/`

## Review checklist

| Check | Result |
| --- | --- |
| No text overflow / overlap @ 1440×900 | PASS |
| Section intro distinguishes return reasons vs submit gates (S12) | PASS — visible in create/detail evidence |
| Human-readable labels (not raw enum keys) | PASS — decision dialog evidence |
| Dual-brand token parity (REDBC/GREENBC) | PASS — GREENBC screenshot captured |
| Functional BDD companion spec | PASS — `P12-BDD-RISK-PROMPT-UX-001.spec.ts` **4/4** |

## Gate evidence

```powershell
$env:E2E_TARGET = "docker"
pnpm -C frontend exec playwright test `
  e2e/P12-BDD-RISK-PROMPT-UX-001.spec.ts `
  e2e/P12-BDD-RISK-PROMPT-UX-001-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

**Result:** **5 passed** (~26s) — Docker `4173` + `8080/healthz`; `DOCGEN_SEED_DEMO_CATALOG=true`.

**Status:** **PASS** (2026-06-29)
