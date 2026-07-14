# CE-U02 — Block sort / copy / validate scroll E2E Manifest

**Slice:** `ce-u02-block-sort-copy-scroll` (CE-U02 / Task Master **#65**)  
**Stage:** 6 — e2e-test-engineer (functional)  
**Date:** 2026-07-14  
**BDD:** `ready` ([docs/behavior/ce-u02-block-sort-copy-scroll.md](../../../docs/behavior/ce-u02-block-sort-copy-scroll.md))  
**Spec:** `frontend/e2e/ce-u02-block-sort-copy-scroll.spec.ts`  
**Verdict:** **PASS** (2/2)

## Environment

| Item | Value |
| --- | --- |
| UI | `http://127.0.0.1:5173` (docker frontend; `.env` FRONTEND_PORT) |
| API / healthz | `http://127.0.0.1:8080` **UP** |
| Deploy | **DEPLOY_OK_WITH_NOTES** (backend compose healthcheck wget missing in Temurin image; `/healthz` UP) |
| Role | Global Admin (`10000001`) |

## Command

```powershell
$env:E2E_BASE_URL='http://127.0.0.1:5173'; $env:E2E_TARGET='docker'
pnpm -C frontend exec playwright test e2e/ce-u02-block-sort-copy-scroll.spec.ts `
  --config playwright.docker.config.ts
```

**Result:** **2 passed** — 2026-07-14

## Scenario mapping (BDD-CE-U02-BLOCK-SORT-COPY-SCROLL-001)

| Test | BDD | Result |
| --- | --- | --- |
| BS-02 copy block duplicates paragraph text | BS-02 | **PASS** |
| BS-03 validation issue scrolls block into view | BS-03 | **PASS** |

## Artifacts

| File | Description |
| --- | --- |
| `CE-U02-block-sort-copy-scroll/BS-02-copy-block.png` | Copy block evidence |
| `CE-U02-block-sort-copy-scroll/BS-03-validate-scroll.png` | Validate scroll evidence |
