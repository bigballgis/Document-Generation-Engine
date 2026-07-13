# Slim R9-frontend — near-250 panels/composables peel evidence

Branch: `feat/slim-r9-frontend`  
Base: `main` @ `cf02c24`  
Date: 2026-07-13

## Scope completed

Behavior-preserving peel of Vue/TS near-250 hotspots toward soft target **&lt;240**. Pattern: thin orchestrator SFC or facade composable + same-dir sibling collaborators. Public props/events/exports and call-site paths preserved. English-first i18n (reused existing keys). BDD not applicable for pure peel.

Kernels left as residuals: `types/`, `api/`, large Pinia stores, `auth/roles.ts` (structural; not panel/composable peels).

### MUST

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `TemplateRiskPromptConfigPanel.vue` | **291** | **178** | `useTemplateRiskPromptConfigPanel.ts` (**175**) |
| `useStructuredContentDocumentModel.ts` | **282** | **190** | `createStructuredContentDocumentMutations.ts` (**117**) |
| `useTemplateTestDataSetPanel.ts` | **278** | **102** | `createTemplateTestDataSetPanelActions.ts` (**203**) |
| `useTemplateListCatalog.ts` | **278** | **214** | `createTemplateListCatalogControls.ts` (**98**) |

### Also peeled (≥250 after recount)

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `TemplateDetailLifecycleTab.vue` | **280** | **186** | `TemplateDetailLifecyclePublishPane.vue` |
| `templateAuthorJourney.ts` | **276** | **115** | `templateAuthorJourneyTypes.ts` + `resolveTemplateAuthorDashboardJourney.ts` |
| `useAuditConsole.ts` | **267** | **233** | `createAuditConsoleDisplayHelpers.ts` |
| `useMasterListView.ts` | **266** | **198** | `createMasterListCatalogControls.ts` |
| `useCommandPalette.ts` | **264** | **202** | `createCommandPaletteDerivedState.ts` |
| `useApiPolicyDomainEditorActions.ts` | **261** | **119** | `createApiPolicyDomainSaveHandlers.ts` |
| `useClauseAuthoringPanel.ts` | **261** | **150** | `createClauseAuthoringReferenceActions.ts` |
| `navStructure.ts` | **259** | **84** | `navCatalog.ts` + `navGroupsCatalog.ts` |
| `useTemplateDetailController.ts` | **259** | **146** | `assembleTemplateDetailControllerApi.ts` |
| `useMasterRevisionDetailController.ts` | **258** | **204** | `createMasterRevisionDetailActions.ts` |
| `TemplateDetailView.vue` | **255** | **21** | `TemplateDetailViewBody.vue` (**248**) |
| `useCommandPaletteCatalog.ts` | **253** | **138** | `createCommandPaletteCatalogSearchTasks.ts` |
| `TemplatePublishSummaryDialog.vue` | **250** | **196** | `useTemplatePublishSummaryDialog.ts` |
| `stores/contentModules.ts` | **250** | **231** | `contentModuleStoreHelpers.ts` |

LOC via `(Get-Content file).Count`.

## Extracted files

- `frontend/src/components/templates/useTemplateRiskPromptConfigPanel.ts`
- `frontend/src/composables/createStructuredContentDocumentMutations.ts`
- `frontend/src/components/templates/createTemplateTestDataSetPanelActions.ts`
- `frontend/src/views/templates/createTemplateListCatalogControls.ts`
- `frontend/src/views/templates/detail/TemplateDetailLifecyclePublishPane.vue`
- `frontend/src/utils/templateAuthorJourneyTypes.ts`
- `frontend/src/utils/resolveTemplateAuthorDashboardJourney.ts`
- `frontend/src/composables/createAuditConsoleDisplayHelpers.ts`
- `frontend/src/views/masters/createMasterListCatalogControls.ts`
- `frontend/src/composables/createCommandPaletteDerivedState.ts`
- `frontend/src/composables/createCommandPaletteCatalogSearchTasks.ts`
- `frontend/src/composables/createApiPolicyDomainSaveHandlers.ts`
- `frontend/src/components/templates/createClauseAuthoringReferenceActions.ts`
- `frontend/src/navigation/navCatalog.ts`
- `frontend/src/navigation/navGroupsCatalog.ts`
- `frontend/src/views/templates/assembleTemplateDetailControllerApi.ts`
- `frontend/src/views/masters/createMasterRevisionDetailActions.ts`
- `frontend/src/views/templates/detail/TemplateDetailViewBody.vue`
- `frontend/src/components/templates/useTemplatePublishSummaryDialog.ts`
- `frontend/src/stores/contentModuleStoreHelpers.ts`

## Residuals (still ≥250 after this wave)

| LOC | Path |
|-----|------|
| 721 | `frontend/src/types/template.ts` |
| 626 | `frontend/src/api/templates.ts` |
| 482 | `frontend/src/stores/templates.ts` |
| 435 | `frontend/src/stores/templatePanelData.ts` |
| 393 | `frontend/src/stores/masters.ts` |
| 323 | `frontend/src/auth/roles.ts` |
| 304 | `frontend/src/stores/apiPolicy.ts` |
| 293 | `frontend/src/api/apiPolicy.ts` |

`frontend/src/auto-imports.d.ts` may regenerate on build (line-ending noise); left unstaged.  
`dashboardViewTestSupport.ts` excluded (test helper).

## Gates

```text
pnpm -C frontend lint        # GREEN (exit 0)
pnpm -C frontend type-check  # GREEN (exit 0)
pnpm -C frontend test        # GREEN — Test Files 191 passed; Tests 1159 passed; Duration ~178s
pnpm -C frontend build       # GREEN (vue-tsc --noEmit && vite build; built in ~19s)
```

## E2E

Skipped full e2e: no intentional user-visible selector or flow changes (prop/event contracts and facade exports preserved; script logic moved into sibling composables/child SFCs).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match R5–R8 thin-orchestrator + sibling composable/subcomponent peels
- Public types re-exported from `templateAuthorJourney.ts` / `navStructure.ts` facades
- Did not peel types/api/store/auth kernels
- BDD not applicable for this hygiene wave
