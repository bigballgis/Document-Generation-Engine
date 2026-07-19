# IBL-E1 Stage 6 — E2E functional evidence

Canonical FE manifest: [frontend/e2e/evidence/IBL-E1-locale-variant-model-manifest.md](../../../../frontend/e2e/evidence/IBL-E1-locale-variant-model-manifest.md)

**Task:** #128 / IBL-E1  
**Slice:** `ibl-e1-locale-variant-model`  
**ADR:** [ADR-0062](../../../adr/template-lifecycle/0062-locale-variant-template-clause-model.md) (Accepted; formerly drafted as ADR-0061, renumbered 2026-07-19)  
**Date:** 2026-07-20  
**Config:** `frontend/playwright.docker.config.ts` @ `:4173` / `:8080`  
**Verdict:** **PASS** (4/4, ~10.5s confirmation run)

## Spec

`frontend/e2e/IBL-E1-locale-variant-model.spec.ts`

| Test | BDD |
| --- | --- |
| create form requires body locale; blocks blank submit | BDD-IBL-E1-013 |
| locale options selectable; hub shows persisted locale | BDD-IBL-E1-013 |
| catalog locale filter shows only matching packages | BDD-IBL-E1-014 |
| template hub sibling locale navigation | BDD-IBL-E1-015 |

## Command

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm exec playwright test `
  e2e/IBL-E1-locale-variant-model.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

**Run log:** [playwright-run.txt](./playwright-run.txt)  
**HTML report (workspace):** `frontend/playwright-report/docker/`  
**Stage 5 deploy evidence:** [../ibl-e1-stage5-deploy/](../ibl-e1-stage5-deploy/)

**Blockers:** none  
**Next:** Stage **7** — `e2e-uiux-reviewer`  
**Not claimed:** Task Master #128 Done / Wave E Done
