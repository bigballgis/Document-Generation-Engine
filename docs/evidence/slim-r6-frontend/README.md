# Slim R6-frontend — lifecycle composables and mid-tier panel peel evidence

Branch: `feat/slim-r6-frontend`  
Base: `main` @ `dac7907`  
Date: 2026-07-13

## Scope completed

Behavior-preserving peel of MUST lifecycle/hub composables plus mid-tier panels still above ~300 LOC after R5. Pattern: thin facade/orchestrator + focused collaborator modules. Public export names and call sites keep the original module paths. No intentional UX copy/flow changes. BDD not applicable for pure peel.

### MUST composables (target &lt;300)

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `useTemplateLifecycleDecisions.ts` | **365** | **233** | `useTemplateLifecycleGovernance.ts` (165) |
| `useTemplateLifecycleGates.ts` | **365** | **243** | `useTemplateLifecycleGateData.ts` (178) |
| `useTemplatePackageHub.ts` | **358** | **240** | `useTemplatePackageHubRouting.ts` (158) |
| `useTemplateJourneyContext.ts` | **360** | **224** | `useTemplateJourneyHandlers.ts` (199) |
| `useWorkflowTasks.ts` | **345** | **131** | `workflowTaskPartitions.ts` (239) |
| `useTemplateAuthoringBindingsEdit.ts` | **335** | **119** | `useTemplateAuthoringBindingsEditActions.ts` (290) |
| `useContentModuleDetailController.ts` | **360** | **221** | `useContentModuleDetailActions.ts` (209) |

All MUST facades are under **300** physical LOC.

### Mid-tier panels (moved toward &lt;300)

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `BatchTestProgressDialog.vue` | **392** | **220** | `useBatchTestProgressStream.ts` (228) |
| `TemplateReleaseVersionHistoryPanel.vue` | **388** | **298** | `useTemplateReleaseVersionActions.ts` (126) |
| `TemplateTestDataSetPanel.vue` | **398** | **212** | `useTemplateTestDataSetPanel.ts` (279) |

All three peeled panels are under **300** (history panel at 298).

## Extracted files

- `frontend/src/views/templates/useTemplateLifecycleGovernance.ts`
- `frontend/src/views/templates/useTemplateLifecycleGateData.ts`
- `frontend/src/views/templates/useTemplatePackageHubRouting.ts`
- `frontend/src/views/templates/useTemplateJourneyHandlers.ts`
- `frontend/src/composables/workflowTaskPartitions.ts`
- `frontend/src/composables/useTemplateAuthoringBindingsEditActions.ts`
- `frontend/src/views/contentModules/useContentModuleDetailActions.ts`
- `frontend/src/components/template/useBatchTestProgressStream.ts`
- `frontend/src/components/templates/useTemplateReleaseVersionActions.ts`
- `frontend/src/components/templates/useTemplateTestDataSetPanel.ts`

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `TemplateDetailView.vue` | ~389 | Shell/orchestrator; optional deepen |
| `MasterListView.vue` | ~372 | Catalog surface; optional next peel |
| `CommandPalette.vue` | ~365 | SFC shell over already-peeled composable |
| `GroupManagementPanel.vue` | ~348 | Identity mid-tier |
| `TemplateContentModuleReferencesPanel.vue` | ~347 | Mid-tier panel |
| `TemplateCallerContractPanel.vue` | ~346 | Mid-tier panel |
| `TemplateInvocationsPanel.vue` | ~331 | Mid-tier panel |
| `useAuditConsole.ts` | ~324 | Already peeled earlier; optional deepen |
| `useVersionLinesPanel.ts` / `useClauseAuthoringPanel.ts` | ~325 / ~316 | Collaborators from prior waves |
| `PreviewProgressDialog.vue` | ~319 | Sibling of BatchTest peel; optional |

Did **not** expand into unrelated mid-tier panels once MUST composables and the three targeted panels were under budget and gates were green.

`frontend/src/auto-imports.d.ts` may regenerate on build (line-ending noise); left unstaged when not required for compile.

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no user-visible selector or flow changes (facade exports preserve public API; panel script logic moved into composables with prop/event wiring unchanged).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match R4/R5 thin-orchestrator + collaborator composable
- Public types/helpers re-exported from original facade modules where needed for caller stability
- BDD not applicable for this hygiene wave
