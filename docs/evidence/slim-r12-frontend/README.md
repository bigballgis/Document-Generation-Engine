# Slim R12-frontend — near-190 panels / api / stores peel evidence

Branch: `feat/slim-r12-frontend`  
Base: `main` @ `cccf4f9`  
Date: 2026-07-13

## Scope completed

Behavior-preserving peel of remaining near-190 Vue panels / composables toward soft target **&lt;180**, and Pinia stores / api modules toward **&lt;200**. Pattern: thin orchestrator + sibling SCSS / factories / child sections; public store IDs and `@/api/apiPolicy` import path preserved via barrel re-exports. English-first (no new i18n keys). BDD not applicable for pure peel.

LOC via `(Get-Content file).Count`.

### MUST

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `api/apiPolicy.ts` | **293** | **25** | `apiPolicyAccess.ts` + `apiPolicyDomains.ts` + `apiPolicyInvocations.ts` (barrel) |
| `stores/contentModules.ts` | **231** | **122** | `createContentModulesMutationActions.ts` |
| `stores/identity.ts` | **232** | **68** | `createIdentityUserActions.ts` + `createIdentityGroupActions.ts` |
| `stores/audit.ts` | **221** | **148** | `createAuditQueryActions.ts` |
| `stores/createMastersCatalogActions.ts` | **212** | **161** | `createMastersCatalogMutationActions.ts` |
| `useAuditConsole.ts` | **233** | **143** | `createAuditConsoleFilterActions.ts` |
| `useBatchTestProgressStream.ts` | **227** | **160** | `batchTestProgressStreamHandlers.ts` |
| `ApiPolicyImpactPreviewPanel.vue` | **239** | **144** | `.scss` |
| `useTemplateJourneyContext.ts` | **223** | **67** | `createTemplateJourneyVisibility.ts` |
| `ManagementShellNav.vue` | **228** | **88** | `.scss` |
| `useLifecycleDecisionDialog.ts` | **219** | **146** | `createLifecycleDecisionDialogDerived.ts` |
| `TemplatePreviewPanel.vue` | **226** | **120** | `useTemplatePreviewPanel.ts` + `.scss` |
| `TemplateTestDataSetPanel.vue` | **211** | **121** | `TemplateTestDataSetEditDialog.vue` + `.scss` |
| `TemplateDetailLegacyWorkspace.vue` | **213** | **157** | `templateDetailLegacyWorkspaceProps.ts` + `.scss` |
| `useContentModuleDetailController.ts` | **220** | **168** | `createContentModuleDetailDerived.ts` |
| `TemplateVariableTreePanel.vue` | **219** | **154** | `.scss` |
| `TemplateLifecycleDecisionDialog.vue` | **209** | **111** | `LifecycleDecision*Fields.vue` |

### SHOULD (were ≥190)

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `ManagementShellHeader.vue` | **218** | **115** | `.scss` |
| `usePreviewProgressStream.ts` | **217** | **152** | `previewProgressStreamHandlers.ts` |
| `assembleTemplateDetailControllerApi.ts` | **200** | **125** | `assembleTemplateDetailControllerSlices.ts` |
| `TemplateDetailLoadedBody.vue` | **202** | **83** | `TemplateDetailLoadedDevSection.vue` + `LegacySection.vue` |
| `useContentModuleDetailActions.ts` | **208** | **101** | `createContentModuleDetailActionHandlers.ts` |
| `useTemplateAuthoringBindingsEditActions.ts` | **206** | **166** | `createTemplateAuthoringBindingsEditHandlers.ts` |
| `UserManagementListSection.vue` | **211** | **179** | `.scss` |
| `TemplatePreviewRunHistoryPanel.vue` | **217** | **128** | `useTemplatePreviewRunHistoryPanel.ts` + `.scss` |
| `GroupManagementPanel.vue` | **208** | **174** | `.scss` |
| `BatchTestProgressDialog.vue` | **219** | **135** | `.scss` |
| `LoginView.vue` | **211** | **127** | `.scss` |
| `useTemplateListCatalog.ts` | **214** | **162** | `templateListCatalogQuery.ts` |
| `TemplateContentModuleReferencesPanel.vue` | **204** | **171** | `.scss` |
| `TemplateReleaseVersionHistoryPanel.vue` | **199** | **98** | `TemplateReleaseVersionHistoryTable.vue` |

## Residuals (production Vue/TS ≥190 after this wave; skip generated/i18n/tests)

| LOC | Path |
|-----|------|
| 342 | `frontend/src/views/dashboard/dashboardViewTestSupport.ts` |
| 248 | `frontend/src/types/templatePreview.ts` |
| 238 | `frontend/src/utils/variableSchemaTree.ts` |
| 219 | `frontend/src/api/masters.ts` |
| 218 | `frontend/src/utils/masterDesignerJourney.ts` |
| 215 | `frontend/src/components/templates/TemplateChangeDiffPanel.vue` |
| 211 | `frontend/src/views/dashboard/DashboardView.vue` |
| 211 | `frontend/src/router/index.ts` |
| 209 | `frontend/src/components/templates/InvocationSummaryDrawer.vue` |
| 207 | `frontend/src/stores/createTemplatesCatalogActions.ts` |
| 205 | `frontend/src/views/templates/detail/TemplateDetailApprovalTab.vue` |
| 204 | `frontend/src/views/contentModules/useContentModuleListView.ts` |
| 204 | `frontend/src/views/masters/useMasterRevisionDetailController.ts` |
| 204 | `frontend/src/components/templates/useTemplateContentModuleReferencesPanel.ts` |
| 203 | `frontend/src/views/templates/detail/TemplateDetailApiAccessTab.vue` |
| 203 | `frontend/src/components/templates/createTemplateTestDataSetPanelActions.ts` |
| 203 | `frontend/src/components/masters/MasterReplaceFileDialog.vue` |
| 202 | `frontend/src/composables/useCommandPalette.ts` |
| 201 | `frontend/src/components/layout/NotificationBell.vue` |
| 201 | `frontend/src/components/authoring/FidelityWarningList.vue` |
| 200 | `frontend/src/api/templatesDetailPanels.ts` |
| 198 | `frontend/src/components/templates/TemplateCallerContractPanel.vue` |
| 198 | `frontend/src/views/templates/useTemplateJourneyHandlers.ts` |
| 198 | `frontend/src/views/masters/useMasterListView.ts` |
| 197 | `frontend/src/utils/structuredContentNodes.ts` |
| 197 | `frontend/src/composables/useStructuredContentLocalDraft.ts` |
| 197 | `frontend/src/components/templates/TemplateSubmitForApprovalSummaryDialog.vue` |
| 196 | `frontend/src/components/templates/TemplatePublishSummaryDialog.vue` |
| 194 | `frontend/src/views/templates/detail/TemplateDetailLifecyclePublishPane.vue` |
| 193 | `frontend/src/views/identity/useGroupManagementPanel.ts` |
| 193 | `frontend/src/views/masters/useMasterPackageHub.ts` |
| 191 | `frontend/src/components/authoring/StructuredContentBlockCard.vue` |
| 191 | `frontend/src/components/masters/MasterRevisionLinesPanel.vue` |
| 191 | `frontend/src/composables/createApiPolicyDomainSaveHandlers.ts` |
| 190 | `frontend/src/composables/useStructuredContentDocumentModel.ts` |
| 190 | `frontend/src/components/api/ApiPolicyDomainEditor.vue` |
| 190 | `frontend/src/components/templates/TemplateCoveragePanel.vue` |
| 190 | `frontend/src/views/templates/useTemplatePackageHub.ts` |

### api/stores ≥200 residuals

| LOC | Path |
|-----|------|
| 219 | `frontend/src/api/masters.ts` |
| 207 | `frontend/src/stores/createTemplatesCatalogActions.ts` |
| 200 | `frontend/src/api/templatesDetailPanels.ts` |

Wave MUST/SHOULD kernels and target stores/api are all under thresholds.  
`frontend/src/auto-imports.d.ts` may regenerate on build (line-ending noise); left unstaged.

## Gates

```text
pnpm -C frontend lint        # GREEN (exit 0)
pnpm -C frontend type-check  # GREEN (exit 0)
pnpm -C frontend test        # GREEN — Test Files 191 passed; Tests 1159 passed
pnpm -C frontend build       # GREEN (vue-tsc --noEmit && vite build; built in ~21s)
```

## E2E

Skipped full e2e: no intentional user-visible selector or flow changes (prop/event contracts and store public APIs preserved; logic/styles moved into sibling modules).

## Approach notes

- English-first i18n (reused existing keys)
- Match R10–R11 thin-orchestrator / factory + sibling peels; scoped styles via `<style src="./X.scss">`
- Pinia store IDs (`contentModules`, `identity`, `audit`, `masters`) and `@/api/apiPolicy` public surface preserved
- BDD not applicable for this hygiene wave
