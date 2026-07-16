# CE-U20 Functional Evidence Manifest — Clause create structured + catalog Status

**Task:** CE-U20 / Task Master **#94** — structured create editor (no JSON textarea) + catalog Status column/filter  
**Slice:** `ce-u20-clause-create-structured` (`feat/ce-u20-clause-create-structured`)  
**Worktree:** `D:/working/DGE-ce-u20-clause-create-structured`  
**BDD:** [docs/behavior/ce-u20-clause-create-structured.md](../../../docs/behavior/ce-u20-clause-create-structured.md) (`ready`; **BDD-CE-U20-CCS-001…010**)  
**Date:** 2026-07-17  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (stage-5 DEPLOY_OK)  
**Verdict:** **PASS** (7/7)

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U20-clause-create-structured.spec.ts` — CCS-001/002 | **passed** |
| `CE-U20-clause-create-structured.spec.ts` — CCS-003/010 | **passed** |
| `CE-U20-clause-create-structured.spec.ts` — CCS-004 | **passed** |
| `CE-U20-clause-create-structured.spec.ts` — CCS-005/006 | **passed** |
| `CE-U20-clause-create-structured.spec.ts` — CCS-007 | **passed** |
| `CE-U20-clause-create-structured.spec.ts` — CCS-008 | **passed** |
| `CE-U20-clause-create-structured.spec.ts` — CCS-009 | **passed** |

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm exec playwright test `
  e2e/CE-U20-clause-create-structured.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 7 passed (22.6s)
```

**HTML report:** `frontend/playwright-report/docker/`  
**Plan evidence mirror:** `docs/plan/evidence/ce-u20-stage6-e2e/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| CCS-001 No JSON textarea | Create dialog → `[data-testid=controlled-structured-content-editor]` visible; no “Content structure (JSON)” |
| CCS-002 Default structured content | Editor shows empty `[data-testid=paragraph-input]` (DEFAULT_STRUCTURED_CONTENT_JSON / nodes) |
| CCS-003 Structured create | Fill paragraph → Create → POST `contentStructureJson` contains `schemaVersion` + `nodes` + text; navigate detail |
| CCS-004 Shared groups | GROUP_ADMIN Share to groups → payload `sharedGroupCodes` + structured JSON |
| CCS-005 Status column | Catalog columnheader **Status**; Draft / Approved badges on fixture rows |
| CCS-006 Filter DRAFT | CatalogFilterToolbar Status → `status=DRAFT` query; page 0; approved row excluded |
| CCS-007 STOPPED | STOP_USE fixture → Status=Stopped; filter STOPPED shows / DRAFT hides |
| CCS-008 Illegal status | `GET ?status=NOT_A_REAL_STATUS` → empty page `totalElements=0` |
| CCS-009 Fail-closed create | TEMPLATE_APPROVER → no **New content module**; editor unreachable |
| CCS-010 Journey | CCS-003/010: create → list Status Draft → filter DRAFT still visible (UIUX dual-brand deferred to stage 7) |

### Selectors used

- `[data-testid=controlled-structured-content-editor]` / `[data-testid=paragraph-input]`
- Table columnheader **Status** + `ContentModuleStatusBadge` labels (Draft / Approved / Stopped)
- CatalogFilterToolbar Status combobox (`aria-label` / placeholder **Status**)

### Fixture notes

- Draft / approved / stopped modules via `content-modules-api` (`E2E-` moduleCode prefix)
- STOPPED via approve + `lifecycle/operation/apply` `STOP_USE` (`createStoppedContentModule`)
- Create UI selects owner group `RETAIL` explicitly (ScopedGroupSelect default can race)

## Artifacts added / updated

- `frontend/e2e/CE-U20-clause-create-structured.spec.ts` (new)
- `frontend/e2e/helpers/content-modules-api.ts` (`listContentModulesViaApi`, `createStoppedContentModule`)
- `frontend/e2e/evidence/CE-U20-manifest.md` (this file)
- `docs/plan/evidence/ce-u20-stage6-e2e/`

## Notes for e2e-uiux-reviewer (stage 7)

1. Dual-brand @1920: Create dialog structured editor (900px) + catalog Status column + Status filter.
2. English-first: New content module / Create module / Status / Draft / Approved / Stopped.
3. Confirm Status badges align with detail versions table; filter toolbar chips readable.
4. No merge / MAIN doc-sync from stage 6.
