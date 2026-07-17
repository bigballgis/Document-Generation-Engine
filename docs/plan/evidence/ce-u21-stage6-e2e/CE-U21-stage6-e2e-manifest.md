# CE-U21 Stage 6 — E2E functional evidence (mirror)

Canonical FE manifest: [frontend/e2e/evidence/CE-U21-manifest.md](../../../../frontend/e2e/evidence/CE-U21-manifest.md)

**Task:** #95 / CE-U21  
**Slice:** `ce-u21-draft-anchor-concurrency`  
**Date:** 2026-07-17  
**Config:** `frontend/playwright.docker.config.ts` @ `:4173` / `:8080`  
**Verdict:** **PASS** (4/4, 27.9s)

## Spec

`frontend/e2e/CE-U21-draft-anchor-concurrency.spec.ts`

| Test | BDD |
| --- | --- |
| DAC-001/002 per-anchor isolation | BDD-CE-U21-DAC-001, 002 |
| DAC-005 clear-on-save | BDD-CE-U21-DAC-005 |
| DAC-007 conflict Keep editing | BDD-CE-U21-DAC-007 |
| DAC-008/012 Reload then Save | BDD-CE-U21-DAC-008, 012 |

## Command

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm exec playwright test `
  e2e/CE-U21-draft-anchor-concurrency.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

**Blockers:** none  
**Next:** Stage **7** — `e2e-uiux-reviewer`
