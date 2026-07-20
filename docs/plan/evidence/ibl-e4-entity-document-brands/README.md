# IBL-E4 Stage 6 — E2E functional evidence

| Field | Value |
| --- | --- |
| **Task** | #131 / IBL-E4 |
| **Slice** | `ibl-e4-entity-document-brands` |
| **Worktree** | `D:/working/DGE-ibl-e4-entity-document-brands` |
| **Date** | 2026-07-20 |
| **Config** | `frontend/playwright.docker.config.ts` @ `:4173` / `:8080` |
| **Verdict** | **PASS** (6/6, ~1.9m) |
| **Stack** | Stage 5 `DEPLOY_OK` (single host compose) |

## Spec

`frontend/e2e/ibl-e4-entity-document-brands.spec.ts`  
Helper: `frontend/e2e/helpers/ibl-e4-document-brand-api.ts`

| Test | BDD |
| --- | --- |
| Admin creates DocumentBrand; absent from UI chrome switcher | BDD-IBL-E4-001 (+ E4-C1 chrome orthogonality) |
| LegalEntity create via ACTIVE document brand picker | BDD-IBL-E4-002 / 013 |
| Admin rebinds LegalEntity → document brand | BDD-IBL-E4-003 |
| REDBC/GREENBC chrome switch leaves entity→document brand bind unchanged | BDD-IBL-E4-012 |
| Template author configures document brand allow-list | BDD-IBL-E4-011 (UI) |
| TEMPLATE_AUTHOR forbidden on catalog routes; admin retains create | BDD-IBL-E4-014 |

## Command

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test e2e/ibl-e4-entity-document-brands.spec.ts `
  --config playwright.docker.config.ts --workers=1 --trace on
```

## Result

```
6 passed (1.9m)
```

## Artifacts

| Kind | Path |
| --- | --- |
| Summary | [latest-summary.json](./latest-summary.json) |
| HTML report | [report/index.html](./report/index.html) |
| Traces | `traces/*-trace.zip` |
| Digests | [digests.txt](./digests.txt) |
| Also | `frontend/playwright-report/docker/` |

## Gaps for Stage 7 (`e2e-uiux-reviewer`)

- Dual-brand REDBC/GREENBC visual evidence @1440×900 on document-brand / legal-entity catalogs
- Brand picker + allow-list select polish / overflow
- Bank OA chrome on default legal-entity panel

## Next

Stage **7** — `e2e-uiux-reviewer`
