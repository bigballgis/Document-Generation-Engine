# Slim R11-frontend — near-210 panels / composables / stores peel evidence

Branch: `feat/slim-r11-frontend`  
Base: `main` @ `7effe3b`  
Date: 2026-07-13

## Scope completed

Behavior-preserving peel of remaining near-210 Vue panels / composables toward soft target **&lt;200**, and Pinia stores toward **&lt;250**. Pattern: thin orchestrator + sibling SCSS / factories / child columns; public store IDs and import paths preserved. English-first (no new i18n keys). BDD not applicable for pure peel.

Skipped: test files; `dashboardViewTestSupport.ts` (non-trivial).

LOC via `(Get-Content file).Count`.

### MUST (Vue/TS were ≥220)

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `TemplateCallerContractPanel.vue` | **246** | **198** | `TemplateCallerContractPanel.scss` |
| `TaskHubPartitionSection.vue` | **247** | **158** | `TaskHubPartitionSection.scss` + `TaskHubCollaborationColumns.vue` |
| `RoleJourneyTimeline.vue` | **249** | **104** | `RoleJourneyTimeline.scss` |
| `TemplateDetailDevWorkspace.vue` | **236** | **180** | `templateDetailDevWorkspaceProps.ts` + `.scss` |
| `TemplateInvocationsPanel.vue` | **240** | **182** | `TemplateInvocationsPanel.scss` |
| `OnboardingTour.vue` | **248** | **150** | `OnboardingTour.scss` |
| `useTemplateLifecycleGates.ts` | **242** | **157** | `createTemplateLifecycleVisibility.ts` |
| `TemplateCreateDialog.vue` | **241** | **101** | `useTemplateCreateDialog.ts` + `.scss` |
| `useTemplatePackageHub.ts` | **239** | **190** | `createTemplatePackageHubActions.ts` |

### SHOULD (were ≥210)

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `NotificationDropdownPanel.vue` | **245** | **116** | `NotificationDropdownPanel.scss` |
| `useControlledStructuredContentEditor.ts` | **236** | **166** | `bindControlledStructuredContentEditorLifecycle.ts` |
| `workflowTaskPartitions.ts` | **238** | **141** | `workflowTaskPartitionTypes.ts` |
| `useDashboardDataLoader.ts` | **244** | **182** | `createDashboardCollaborationLoader.ts` |
| `useTemplateLifecycleDecisions.ts` | **232** | **145** | `createTemplateLifecycleDecisionSubmitters.ts` |
| `MasterUploadDialog.vue` | **236** | **131** | `useMasterUploadDialog.ts` + `.scss` |
| `TemplateAuthoringBindingEditor.vue` | **232** | **174** | `.scss` |
| `TemplateReleaseVersionHistoryPanel.vue` | **225** | **199** | `.scss` |
| `TemplateCoveragePanel.vue` | **233** | **190** | `.scss` |
| `AppDataTable.vue` | **236** | **187** | `AppDataTable.scss` |

### Stores SHOULD → &lt;250

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `stores/apiPolicy.ts` | **304** | **146** | `apiPolicyStoreTypes.ts` + `createApiPolicyMutationActions.ts` |
| `stores/masters.ts` | **291** | **135** | `createMastersCatalogActions.ts` |
| `stores/templates.ts` | **278** | **112** | `createTemplatesCatalogActions.ts` + `templateListFetchOptions.ts` |

## Residuals (production Vue/TS ≥210 after this wave; skip generated/i18n/tests)

| LOC | Path |
|-----|------|
| 293 | `frontend/src/api/apiPolicy.ts` |
| 248 | `frontend/src/types/templatePreview.ts` |
| 239 | `frontend/src/components/api/ApiPolicyImpactPreviewPanel.vue` |
| 238 | `frontend/src/utils/variableSchemaTree.ts` |
| 233 | `frontend/src/composables/useAuditConsole.ts` |
| 232 | `frontend/src/stores/identity.ts` |
| 231 | `frontend/src/stores/contentModules.ts` |
| 228 | `frontend/src/components/layout/ManagementShellNav.vue` |
| 227 | `frontend/src/components/template/useBatchTestProgressStream.ts` |
| 226 | `frontend/src/components/templates/TemplatePreviewPanel.vue` |
| 223 | `frontend/src/views/templates/useTemplateJourneyContext.ts` |
| 221 | `frontend/src/stores/audit.ts` |
| 220 | `frontend/src/views/contentModules/useContentModuleDetailController.ts` |
| 219 | `frontend/src/api/masters.ts` |
| 219 | `frontend/src/components/template/BatchTestProgressDialog.vue` |
| 219 | `frontend/src/components/templates/useLifecycleDecisionDialog.ts` |
| 219 | `frontend/src/components/templates/TemplateVariableTreePanel.vue` |
| 218 | `frontend/src/components/layout/ManagementShellHeader.vue` |
| 218 | `frontend/src/utils/masterDesignerJourney.ts` |
| 217 | `frontend/src/components/templates/TemplatePreviewRunHistoryPanel.vue` |
| 217 | `frontend/src/components/template/usePreviewProgressStream.ts` |
| 215 | `frontend/src/components/templates/TemplateChangeDiffPanel.vue` |
| 214 | `frontend/src/views/templates/useTemplateListCatalog.ts` |
| 213 | `frontend/src/views/templates/detail/TemplateDetailLegacyWorkspace.vue` |
| 212 | `frontend/src/stores/createMastersCatalogActions.ts` (extracted collaborator) |
| 211 | `frontend/src/components/templates/TemplateTestDataSetPanel.vue` |
| 211 | `frontend/src/views/dashboard/DashboardView.vue` |
| 211 | `frontend/src/views/identity/UserManagementListSection.vue` |
| 211 | `frontend/src/router/index.ts` |
| 211 | `frontend/src/views/LoginView.vue` |

Wave MUST/SHOULD kernels and target stores are all under thresholds.  
No production `stores/*.ts` remains ≥250 (store `*.test.ts` excluded).  
`frontend/src/auto-imports.d.ts` may regenerate on build (line-ending noise); left unstaged.

## Gates

```text
pnpm -C frontend lint        # GREEN (exit 0)
pnpm -C frontend type-check  # GREEN (exit 0)
pnpm -C frontend test        # GREEN — Test Files 191 passed; Tests 1159 passed
pnpm -C frontend build       # GREEN (vue-tsc --noEmit && vite build; built in ~19s)
```

## E2E

Skipped full e2e: no intentional user-visible selector or flow changes (prop/event contracts and store public APIs preserved; logic/styles moved into sibling modules).

## Approach notes

- English-first i18n (reused existing keys)
- Match R9–R10 thin-orchestrator / factory + sibling peels; scoped styles via `<style src="./X.scss">`
- Pinia store IDs (`apiPolicy`, `masters`, `templates`) and public action surfaces preserved
- BDD not applicable for this hygiene wave
