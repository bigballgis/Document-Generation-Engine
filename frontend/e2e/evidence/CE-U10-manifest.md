# CE-U10 Functional Evidence Manifest — sharedGroupCodes UI

**Task:** CE-U10 / Task Master **#85** — Share to groups create + settings + summary  
**Slice:** `ce-u10-shared-group-codes-ui` (`feat/ce-u10-shared-group-codes-ui`)  
**BDD:** [docs/behavior/ce-u10-shared-group-codes-ui.md](../../../docs/behavior/ce-u10-shared-group-codes-ui.md) (`ready`)  
**Date:** 2026-07-15  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (healthz **UP**)  
**Verdict:** **PASS**

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U10-shared-group-codes-ui.spec.ts` — SGC-001…007 | **6/6 passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U10-shared-group-codes-ui.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 6 passed (~16s wall after warm)
```

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| SGC-001 GROUP_ADMIN create writes `sharedGroupCodes` | Create dialog → Share CORP → detail summary + API detail |
| SGC-002 TEMPLATE_AUTHOR hides Share to groups | Create dialog: no multiselect |
| SGC-003 summary owner / shared / empty | API-seeded modules; `Owner:` / `Shared with:` / `Not shared outside owner group` |
| SGC-004/005 Settings confirm cancel then save | MessageBox cancel → no PUT; confirm → PUT + summary refresh |
| SGC-006 fail-closed Settings | Author sees summary; no Settings button |
| SGC-007 options exclude owner | Share dropdown has CORP, not RETAIL |

## Notes

1. Seeded groups are **RETAIL** (owner) + **CORP** (share target); BDD WEALTH/HQ mapped to available catalog.
2. Helpers: `createDraftContentModuleWithSharedGroups`, `updateContentModuleSharedGroupCodesViaApi` in `content-modules-api.ts`.
3. EP select race: wait for prior dropdown `:visible` count 0 before opening Share to groups.
