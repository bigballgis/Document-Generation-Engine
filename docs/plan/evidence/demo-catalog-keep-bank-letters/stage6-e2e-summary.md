# Stage 6 — E2E functional (TM #164 / demo-catalog-keep-bank-letters)

| Field | Value |
| --- | --- |
| **Date** | 2026-07-24 |
| **Agent** | e2e-test-engineer |
| **Stack** | Docker acceptance `:8080` + `:4173` (DEPLOY_OK; cleanup already applied) |
| **Config** | `frontend/playwright.docker.config.ts` |
| **Worktree** | `D:/working/DGE-demo-catalog-keep-bank-letters` |
| **Branch** | `feat/demo-catalog-keep-bank-letters` |
| **Result** | **PASS** (14/14) |

## Specs run

| Spec | Cases | Result |
| --- | --- | --- |
| `frontend/e2e/demo-runtime-generate.spec.ts` | registry length 8 + runtime generate × 8 keep IDs | PASS |
| `frontend/e2e/demo-catalog-keep-bank-letters.spec.ts` | API keep present / purge absent + UI FOL template/master | PASS |
| `frontend/e2e/catalog.spec.ts` | catalog smoke retargeted to keep-set FOL (was purged retail) | PASS |

Command:

```powershell
pnpm -C frontend exec playwright test `
  e2e/demo-runtime-generate.spec.ts `
  e2e/demo-catalog-keep-bank-letters.spec.ts `
  e2e/catalog.spec.ts `
  --config playwright.docker.config.ts
```

Also available via: `pnpm -C frontend test:e2e:docker:demos` (runtime + keep-set membership; catalog included in focused Stage 6 run above).

## Keep-set confirmed

`keep_set_confirmed: true`

- All 8 keep externalIds present and `PUBLISHED` via management `GET /templates`.
- Purge sample absent: `DEMO-FULL-FLOW-LETTER`, `DEMO-RETAIL-LETTER`, `DEMO-RETAIL-ACCOUNT-OPEN`, `DEMO-MORTGAGE-APPROVAL`, `DEMO-INSURANCE-ENDORSEMENT`, `DEMO-KYC-CDD-NOTICE`, `DEMO-ACCOUNT-CLOSURE`, `DEMO-WEALTH-STATEMENT`.
- Runtime generate DOCX succeeded for each keep ID (BDD-DEMO-TYP-011/012 + BDD-DEMO-KEEP-010 surface).
- UI catalog lists `CORP-FOL-OFFER` and `Meridian Wholesale FOL Master`.

## BDD mapping

| Scenario | Coverage |
| --- | --- |
| BDD-DEMO-KEEP-001 | API + UI keep membership / PUBLISHED |
| BDD-DEMO-KEEP-002 | Purge sample absent from API catalog |
| BDD-DEMO-KEEP-010 (runtime surface) | `demo-runtime-generate` × 8 |

Ops/registry/disk scenarios (003–009, 011–014) remain Stage 4–5 / contract evidence — not re-asserted as Playwright journeys here.

## Fixture honesty notes

- `catalog.spec.ts` no longer asserts purged `DEMO-RETAIL-LETTER` / `Demo Retail Letterhead`; uses keep-set FOL markers.
- Broader suite helpers still reference `DEMO_TEMPLATE_EXTERNAL_ID` / `DEMO_FULL_FLOW_*` for non-keep journeys (e.g. `role-journeys`, `demo-full-lifecycle`, SYS-NORM-*). Those were **not** in this focused Stage 6 run; full `test:e2e:docker` may still hit purged-ID fixtures until a later retarget leaf.
- `frontend_ui_in_scope=false` — no product UI redesign; UI checks are catalog membership smoke only.

## Artifacts

- Playwright HTML report: `frontend/playwright-report/docker/`
- This note: `docs/plan/evidence/demo-catalog-keep-bank-letters/stage6-e2e-summary.md`

## UIUX

`uiux_recommended: skip` — `frontend_ui_in_scope=false`; no new user-facing chrome; functional catalog smoke only.

## Next

Architecture/CQ **PASS_WITH_NOTES** → merge `0e6d0bad` → Stage 12 doc-sync → Stage 13 commit-review. Residual: broader suite helpers may still reference purged IDs (not in this focused 14/14 run).
