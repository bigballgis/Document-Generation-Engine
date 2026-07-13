# Slim R8-frontend — near-line views/panels peel evidence

Branch: `feat/slim-r8-frontend`  
Base: `main` @ `9df16f5`  
Date: 2026-07-13  
HEAD (pre-commit working tree; commit SHA after push): see git log after commit

## Scope completed

Behavior-preserving peel of Vue/TS near-line hotspots (≥260 LOC) toward soft target **&lt;260**. Pattern: thin orchestrator SFC or facade composable + same-dir sibling collaborators. Public props/events/exports and call-site paths preserved. English-first i18n (reused existing keys). BDD not applicable for pure peel.

`dashboardViewTestSupport.ts` skipped (optional; non-trivial / not required).

### MUST Vue

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `TemplateVariableTreePanel.vue` | **315** | **219** | `useTemplateVariableTreePanel.ts` (**151**) |
| `TemplateDetailApprovalTab.vue` | **319** | **205** | `useTemplateDetailApprovalTab.ts` (**77**) + `TemplateDetailApprovalPublishPane.vue` (**147**) |

### SHOULD Vue

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `LoginView.vue` | **306** | **211** | `useLoginView.ts` (**131**) |
| `ManagementShell.vue` | **311** | **181** | `useManagementShell.ts` (**183**) |
| `UserManagementPanel.vue` | **303** | **88** | `useUserManagementPanel.ts` (**180**) + `createUserManagementPanelActions.ts` (**140**) |
| `TemplateReleaseVersionHistoryPanel.vue` | **297** | **225** | `useTemplateReleaseVersionHistoryPanel.ts` (**139**) |
| `TemplateImportDialog.vue` | **303** | **188** | `useTemplateImportDialog.ts` (**161**) |
| `TemplateReleaseDetailView.vue` | **299** | **178** | `useTemplateReleaseDetailView.ts` (**168**) |
| `ContentModuleListView.vue` | **291** | **140** | `useContentModuleListView.ts` (**204**) |

### SHOULD TS

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `useControlledStructuredContentEditor.ts` | **305** | **236** | `useControlledStructuredContentCatalogOptions.ts` (**73**) + `createStructuredContentDraftHandlers.ts` (**111**) |
| `useLifecycleDecisionDialog.ts` | **283** | **219** | `lifecycleDecisionDialogTypes.ts` (**104**) (types + payload helpers; re-exported from facade) |
| `useTemplateAuthoringBindingsEditActions.ts` | **289** | **206** | `useTemplateAuthoringBindingsPasteResidue.ts` (**57**) + `createTemplateAuthoringBindingsSaveFlow.ts` (**153**) |

LOC via `(Get-Content file).Count`.

## Extracted files

- `frontend/src/components/templates/useTemplateVariableTreePanel.ts`
- `frontend/src/views/templates/detail/useTemplateDetailApprovalTab.ts`
- `frontend/src/views/templates/detail/TemplateDetailApprovalPublishPane.vue`
- `frontend/src/views/useLoginView.ts`
- `frontend/src/components/layout/useManagementShell.ts`
- `frontend/src/views/identity/useUserManagementPanel.ts`
- `frontend/src/views/identity/createUserManagementPanelActions.ts`
- `frontend/src/components/templates/useTemplateReleaseVersionHistoryPanel.ts`
- `frontend/src/components/templates/useTemplateImportDialog.ts`
- `frontend/src/views/templates/useTemplateReleaseDetailView.ts`
- `frontend/src/views/contentModules/useContentModuleListView.ts`
- `frontend/src/composables/useControlledStructuredContentCatalogOptions.ts`
- `frontend/src/composables/createStructuredContentDraftHandlers.ts`
- `frontend/src/components/templates/lifecycleDecisionDialogTypes.ts`
- `frontend/src/composables/useTemplateAuthoringBindingsPasteResidue.ts`
- `frontend/src/composables/createTemplateAuthoringBindingsSaveFlow.ts`

## Residuals (still ≥260 after this wave)

See operator report `_slim_r8_report.txt` for full scan. MUST/SHOULD peel targets above are all &lt;260.

`frontend/src/auto-imports.d.ts` may regenerate on build (line-ending noise); left unstaged.

## Gates

`	ext
pnpm -C frontend lint        # GREEN (exit 0)
pnpm -C frontend type-check  # GREEN (exit 0; fixed saveRules CompositionRuleInput typing in createTemplateAuthoringBindingsSaveFlow.ts)
pnpm -C frontend test        # GREEN — Test Files 191 passed; Tests 1159 passed; Duration ~173s
pnpm -C frontend build       # GREEN (vue-tsc --noEmit && vite build; built in ~53s)
`

Fix during gates: `createTemplateAuthoringBindingsSaveFlow` `saveRules` callback typed as `CompositionRuleInput[]` (was incorrectly `CompositionRule[]` via panel props).

## E2E

Skipped full e2e: no intentional user-visible selector or flow changes (prop/event contracts and facade exports preserved; script logic moved into sibling composables/child SFCs).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match R5–R7 thin-orchestrator + sibling composable/subcomponent peels
- Public types re-exported from `useLifecycleDecisionDialog.ts` facade
- Did not re-peel lifecycle kernels
- BDD not applicable for this hygiene wave