# Slim R7-frontend — remaining mid-tier views and panels peel evidence

Branch: `feat/slim-r7-frontend`  
Base: `main` @ `ccf6859`  
Date: 2026-07-13

## Scope completed

Behavior-preserving peel of remaining Vue mid-tier hotspots (≥300 LOC) plus SHOULD mid-tier Vue/TS collaborators still above ~280 after R6. Pattern: thin orchestrator/shell SFC or facade composable + focused sibling collaborators. Public props/events/exports and call-site paths preserved. English-first i18n (reused existing keys). BDD not applicable for pure peel.

### MUST Vue (≥300 → under soft target)

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `TemplateDetailView.vue` | **378** | **255** | `TemplateDetailHeaderActions.vue` + reactive controller shell |
| `MasterListView.vue` | **344** | **164** | `useMasterListView.ts` |
| `CommandPalette.vue` | **327** | **187** | `CommandPaletteResults.vue` + `useCommandPaletteDialogFocus.ts` |
| `TemplateContentModuleReferencesPanel.vue` | **314** | **204** | `useTemplateContentModuleReferencesPanel.ts` |
| `TemplateCallerContractPanel.vue` | **313** | **246** | `useTemplateCallerContractPanel.ts` |
| `GroupManagementPanel.vue` | **313** | **208** | `useGroupManagementPanel.ts` |
| `TemplateInvocationsPanel.vue` | **300** | **240** | `useTemplateInvocationsPanel.ts` |

All MUST Vue shells are under **260** (prefer) except none above 255 for this set.

### SHOULD Vue / TS (when gates allowed)

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `RoleJourneyTimeline.vue` | **292** | **249** | `useRoleJourneyTimeline.ts` |
| `TaskHubPartitionSection.vue` | **280** | **247** | `useTaskHubPartitionSection.ts` |
| `PreviewProgressDialog.vue` | **283** | **145** | `usePreviewProgressStream.ts` |
| `useAuditConsole.ts` | **293** | **267** | `createAuditConsoleSorts.ts` + `useAuditConsoleExport.ts` |
| `useVersionLinesPanel.ts` | **293** | **185** | `useVersionLinesActions.ts` |
| `useClauseAuthoringPanel.ts` | **288** | **261** | `useClauseAuthoringEditors.ts` |
| `useDashboardJourneyResolutions.ts` | **281** | **172** | `useDashboardJourneyRoleResolutions.ts` |

Skipped `dashboardViewTestSupport.ts` (non-trivial / not required). `VariableTreePanel` / `DetailApprovalTab` not present as residual hotspots under those paths.

## Extracted files

- `frontend/src/views/templates/detail/TemplateDetailHeaderActions.vue`
- `frontend/src/views/masters/useMasterListView.ts`
- `frontend/src/components/layout/CommandPaletteResults.vue`
- `frontend/src/composables/useCommandPaletteDialogFocus.ts`
- `frontend/src/components/templates/useTemplateContentModuleReferencesPanel.ts`
- `frontend/src/components/templates/useTemplateCallerContractPanel.ts`
- `frontend/src/views/identity/useGroupManagementPanel.ts`
- `frontend/src/components/templates/useTemplateInvocationsPanel.ts`
- `frontend/src/components/journey/useRoleJourneyTimeline.ts`
- `frontend/src/components/dashboard/useTaskHubPartitionSection.ts`
- `frontend/src/components/template/usePreviewProgressStream.ts`
- `frontend/src/composables/createAuditConsoleSorts.ts`
- `frontend/src/composables/useAuditConsoleExport.ts`
- `frontend/src/components/templates/useVersionLinesActions.ts`
- `frontend/src/components/templates/useClauseAuthoringEditors.ts`
- `frontend/src/composables/useDashboardJourneyRoleResolutions.ts`

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| Vue mid-tier 260–279 | various | Soft target prefer &lt;260; optional deepen later |
| `useAuditConsole.ts` | **267** | Under 280; further deepen optional |
| `useClauseAuthoringPanel.ts` | **261** | Under 280 |
| `RoleJourneyTimeline.vue` | **249** | Styles still co-located; optional style-child peel |

Did **not** expand into unrelated surfaces once MUST/SHOULD targets were under budget and gates were green.

`frontend/src/auto-imports.d.ts` may regenerate on build (line-ending noise); left unstaged when not required for compile.

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no intentional user-visible selector or flow changes (prop/event contracts and facade exports preserved; script/stream logic moved into sibling composables).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match R5/R6 thin-orchestrator + sibling composable/subcomponent peels
- Public types re-exported from original facade modules where needed (`DashboardJourneyVisibility`)
- BDD not applicable for this hygiene wave
