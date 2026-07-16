# CE-U18 Stage 6 E2E Evidence — Batch test history drill-down

**Task:** #93 · slice `ce-u18-batch-test-history`  
**Worktree:** `D:/working/DGE-ce-u18-batch-test-history` · `feat/ce-u18-batch-test-history`  
**Date:** 2026-07-17  
**Config:** `frontend/playwright.docker.config.ts` → `http://127.0.0.1:4173`  
**Verdict:** **PASS** (3/3)

## Command

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/CE-U18-batch-test-history.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 3 passed (24.1s)
```

## Coverage map

| BDD | Spec case | Result |
| --- | --- | --- |
| BTH-001 | Expand sampleResults | passed (combined with 002) |
| BTH-002 | Open data set → dataSets + select | passed (combined with 001) |
| BTH-003 | Unmatched Open data set toast | passed |
| BTH-004 | Only async `batch-tests/run` | passed (combined with 005/006) |
| BTH-005 | Completed does not call sync batch | passed (combined with 004/006) |
| BTH-006 | No sync batch UI entry | passed (combined with 004/005) |

## Artifacts

- Spec: `frontend/e2e/CE-U18-batch-test-history.spec.ts`
- Functional manifest: `frontend/e2e/evidence/CE-U18-manifest.md`
- HTML report: `frontend/playwright-report/docker/`

## Next

Stage **7** — `e2e-uiux-reviewer` (dual-brand @1920 history expand + Sample results).
