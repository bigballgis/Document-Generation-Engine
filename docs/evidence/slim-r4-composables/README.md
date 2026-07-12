# Slim R4-composables — composable facade peel evidence

Branch: `feat/slim-r4-composables`  
Base: `main` @ `5350d39`  
Date: 2026-07-13

## Scope completed

Behavior-preserving split of three large composable facades. Public export names remain stable via re-exports from the original module paths (`useCommandPalette`, `useControlledStructuredContentEditor`, `useTemplateAuthoringBindingsPanel`). Callers (`CommandPalette.vue`, `ControlledStructuredContentEditor.vue`, `TemplateAuthoringBindingsPanel.vue`, `useCommandPalette.test.ts`) keep the same import paths. No intentional UX/behavior changes. Did **not** edit Clause/VersionLines panel SFCs (owned by panels worktree). BDD not applicable.

### 1. useCommandPalette

| File | LOC (physical) |
|------|----------------|
| **Before** `useCommandPalette.ts` | **549** |
| After `useCommandPalette.ts` (facade) | 264 |
| After `commandPaletteTypes.ts` | 47 |
| After `commandPaletteHelpers.ts` | 73 |
| After `useCommandPaletteCatalog.ts` | 253 |

### 2. useControlledStructuredContentEditor

| File | LOC (physical) |
|------|----------------|
| **Before** `useControlledStructuredContentEditor.ts` | **551** |
| After `useControlledStructuredContentEditor.ts` (facade) | 305 |
| After `controlledStructuredContentEditorTypes.ts` | 49 |
| After `useStructuredContentDocumentModel.ts` | 282 |
| After `useStructuredContentPasteFlow.ts` | 101 |

### 3. useTemplateAuthoringBindingsPanel

| File | LOC (physical) |
|------|----------------|
| **Before** `useTemplateAuthoringBindingsPanel.ts` | **462** |
| After `useTemplateAuthoringBindingsPanel.ts` (facade) | 155 |
| After `templateAuthoringBindingsTypes.ts` | 43 |
| After `useTemplateAuthoringBindingsEdit.ts` | 335 |

All three facades are under the ~350–400 LOC bar.

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `useDashboardJourney.ts` | ~387 | Next composable peel candidate |
| `useWorkflowTasks.ts` | ~345 | Near bar; optional further extract |
| `useAuditConsole.ts` | ~323 | Already peeled in R-frontend; optional deepen |
| `TemplateClauseAuthoringPanel.vue` | ~484 | Owned by `feat/slim-r4-panels` worktree |
| `TemplateVersionLinesPanel.vue` | ~479 | Owned by `feat/slim-r4-panels` worktree |
| `TemplateLifecycleDecisionDialog.vue` | ~432 | Dialog surface; not this wave |
| `TemplateDetailDevWorkspace.vue` | ~416 | Already a child of TemplateDetail; further peel optional |

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no user-visible selector or flow changes (facade re-exports preserve public API; behavior moved into collaborators only).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Thin facade + focused collaborators (catalog / document model / paste flow / bindings edit)
- Public types/helpers re-exported from original facade modules for caller stability
- BDD not applicable for this hygiene wave
