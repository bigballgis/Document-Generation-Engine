# CE-U16 Functional Evidence Manifest — Authoring path compression

**Task:** CE-U16 / Task Master **#92** — Design default Bindings + Create Authoring path micro-wizard  
**Slice:** `ce-u16-authoring-path-compress` (`feat/ce-u16-authoring-path-compress`)  
**Worktree:** `D:/working/DGE-ce-u16-authoring-path-compress`  
**BDD:** [docs/behavior/ce-u16-authoring-path-compress.md](../../../docs/behavior/ce-u16-authoring-path-compress.md) (`ready`)  
**Date:** 2026-07-17  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (stage-5 DEPLOY_OK)  
**Verdict:** **PASS**

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U16-authoring-path-compress.spec.ts` — BDD-CE-U16-APC-001 | **passed** |
| `CE-U16-authoring-path-compress.spec.ts` — BDD-CE-U16-APC-002 | **passed** |
| `CE-U16-authoring-path-compress.spec.ts` — BDD-CE-U16-APC-003/004/006 | **passed** |
| `CE-U16-authoring-path-compress.spec.ts` — BDD-CE-U16-APC-005 | **passed** |
| `CE-U16-authoring-path-compress.spec.ts` — BDD-CE-U16-APC-007 | **passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U16-authoring-path-compress.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 5 passed (18.7s)
```

**HTML report:** `frontend/playwright-report/docker/`  
**Plan evidence mirror:** `docs/plan/evidence/ce-u16-stage6-e2e/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| APC-001 Design default Bindings | Author opens `/dev/{id}?workspaceTab=design` (no designTab) → Bindings tab `aria-selected=true`; `.bindings-panel` visible; Variables not selected |
| APC-002 Explicit designTab wins | `designTab=variables` → Variables; `designTab=contentModules` → Clause references; bindings default does not override |
| APC-003/004/006 Authoring path | Post-create URL contract (`authoringGuide=1&authoringGuideStep=master`) → `authoring-path-guide` + Master panel (master identity + anchors); steps Bindings / Variables / Preview → `designTab` / `testingTab=previewRuns`; guide has no Submit/Approve/Publish CTAs; `lifecycle-stepper` coexists |
| APC-005 Skip guide | Skip → guide gone; Template design / Template testing tabs usable; Design defaults Bindings |
| APC-007 Daily open | No `authoringGuide` → guide absent; default Bindings; stepper still present |

### Fixture notes

- Draft fixtures: `prepareDraftTemplateWithCleanBinding` (`E2E-` prefix; global teardown cleans)
- Wizard entry: deep-link matching `buildPostCreateAuthoringPath` / `handleCreated` query contract (see Known gap)

## Known gap (FE follow-up — not stage-6 blocker for wizard surface)

**TEMPLATE_AUTHOR Create dialog** cannot populate Approved letterhead options unless `mastersStore` was already filled: create dialog does not call `fetchAllMasters` on open (Import does), and dashboard skips masters fetch when the actor lacks `route.master-management`. E2E therefore validates the **post-create Authoring path URL + wizard** that `handleCreated` emits, rather than clicking through Create under TEMPLATE_AUTHOR. Recommend: mirror Import — `fetchAllMasters` inside `useTemplateCreateDialog` when dialog opens.

## Artifacts added / updated

- `frontend/e2e/CE-U16-authoring-path-compress.spec.ts` (new)
- `frontend/e2e/evidence/CE-U16-manifest.md` (this file)
- `docs/plan/evidence/ce-u16-stage6-e2e/CE-U16-stage6-e2e-manifest.md`

## Notes for e2e-uiux-reviewer (stage 7)

1. Dual-brand @1920: Authoring path guide (Master step) + lifecycle-stepper coexistence; Design/Bindings default; Preview step on Testing.
2. Confirm guide does not collide with Workspace Tab Shell action rail after Skip.
3. English-first: Authoring path / Master / Bindings / Variables / Preview / Skip guide / Dismiss.
4. No merge / MAIN doc-sync from stage 6.
