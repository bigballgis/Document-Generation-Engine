# CE-U21 Functional Evidence Manifest — Draft anchor concurrency

**Task:** CE-U21 / Task Master **#95** — per-anchor localDraft keys + binding save 409 conflict UX  
**Slice:** `ce-u21-draft-anchor-concurrency` (`feat/ce-u21-draft-anchor-concurrency`)  
**Worktree:** `D:/working/DGE-ce-u21-draft-anchor-concurrency`  
**BDD:** [docs/behavior/ce-u21-draft-anchor-concurrency.md](../../../docs/behavior/ce-u21-draft-anchor-concurrency.md) (`ready`; **BDD-CE-U21-DAC-001…012**)  
**Date:** 2026-07-17  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (stage-5 DEPLOY_OK)  
**Verdict:** **PASS** (4/4)

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U21-draft-anchor-concurrency.spec.ts` — DAC-001/002 | **passed** |
| `CE-U21-draft-anchor-concurrency.spec.ts` — DAC-005 | **passed** |
| `CE-U21-draft-anchor-concurrency.spec.ts` — DAC-007 | **passed** |
| `CE-U21-draft-anchor-concurrency.spec.ts` — DAC-008/012 | **passed** |

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm exec playwright test `
  e2e/CE-U21-draft-anchor-concurrency.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 4 passed (27.9s)
```

**HTML report:** `frontend/playwright-report/docker/`  
**Plan evidence mirror:** `docs/plan/evidence/ce-u21-stage6-e2e/`  
**Screenshots:** `frontend/e2e/evidence/CE-U21-draft-anchor-concurrency/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| DAC-001 Per-anchor key | After mutate on `FOL_HEADER`, localStorage key `docgen.structuredDraft.v1:{user}:{tpl}:{dev}:FOL_HEADER`; legacy triple key absent |
| DAC-002 Cross-anchor isolation | Draft on A retained after edit B; remount A Restore applies SA (not SB); B mount shows no A recovery banner |
| DAC-005 Clear-on-save | Save A clears `…:A` only; `…:B` draft retained |
| DAC-007 Conflict UX | Concurrent API upsert → UI Save → HTTP 409 `BINDING_VERSION_CONFLICT`; MessageBox **Binding updated elsewhere** with **Reload** / **Keep editing**; Keep editing retains draft; not `publishVersionConflict` |
| DAC-008 Reload → Save | Reload loads concurrent server JSON; subsequent Save succeeds with fresh token |
| DAC-012 Journey | Isolation + conflict + recovery path covered in this docker run (UIUX dual-brand → stage 7) |

### Selectors / helpers used

- `[data-testid=controlled-structured-content-editor]` / `[data-testid=structured-draft-recovery-banner*]`
- `.el-message-box` filter `/binding updated elsewhere/i` + buttons Reload / Keep editing
- `openDevBindingEditor` / `mutateBindingStructure` / dirty-guard Discard (retains localStorage)
- Fixtures: `prepareDualAnchorFolDraftTemplate` (CORP FOL, `FOL_HEADER` + `FOL_FACILITY_SUMMARY`); `prepareDraftTemplateWithCleanBinding` (RETAIL HEADER conflict)

### Notes

- Demo Retail Letterhead has a single `HEADER` anchor — dual-anchor isolation uses FOL master fixtures.
- Conflict Reload calls `clearStructuredLocalDraftOnSave` (draft cleared; further draft writes may be suppressed until remount). DAC-008 asserts Reload content + successful Save, not a post-Reload draft key.

## Artifacts added / updated

- `frontend/e2e/CE-U21-draft-anchor-concurrency.spec.ts` (new)
- `frontend/e2e/helpers/structured-authoring-api.ts` (`expectedUpdatedAt`, `getBindingUpdatedAtViaApi`, `prepareDualAnchorFolDraftTemplate`)
- `frontend/e2e/evidence/CE-U21-manifest.md` (this file)
- `docs/plan/evidence/ce-u21-stage6-e2e/`

## Notes for e2e-uiux-reviewer (stage 7)

1. Dual-brand @1920: conflict MessageBox (Reload / Keep editing) + recovery banner on bindings editor.
2. English-first: **Binding updated elsewhere** / **Reload** / **Keep editing** — must not reuse publish version-conflict copy.
3. Confirm MessageBox warning type + OA density; recovery banner testids unchanged from LR-C2.
4. No merge / MAIN doc-sync from stage 6.
