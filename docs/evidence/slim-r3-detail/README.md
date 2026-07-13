# Slim R3-detail — detail/list UI peel evidence

Branch: `feat/slim-r3-detail`  
Base: `main` @ `9131486`  
Date: 2026-07-12

## Scope completed

Behavior-preserving peel of detail/list orchestrators. Reused `WorkspaceTabShell` and F6-style composable controllers. Did **not** touch PackageHub views (owned by another agent). No intentional UX copy/flow changes. BDD not applicable.

### 1. ContentModuleDetailView (highest priority)

| File | LOC (physical) |
|------|----------------|
| **Before** `ContentModuleDetailView.vue` | **477** |
| After `ContentModuleDetailView.vue` (orchestrator) | 110 |
| After `useContentModuleDetailController.ts` | 360 |
| After `detail/ContentModuleDetailWorkspace.vue` | 135 |
| After `detail/ContentModuleDetailDialogs.vue` | 40 |

### 2. TemplateListView

| File | LOC (physical) |
|------|----------------|
| **Before** `TemplateListView.vue` | **416** |
| After `TemplateListView.vue` (orchestrator) | 107 |
| After `useTemplateListCatalog.ts` | 278 |
| After `list/TemplateListCatalogPanel.vue` | 130 |
| After `list/TemplateListWorkflowFilters.vue` | 44 |

### 3. MasterRevisionDetailView (bonus)

| File | LOC (physical) |
|------|----------------|
| **Before** `MasterRevisionDetailView.vue` | **458** |
| After `MasterRevisionDetailView.vue` (orchestrator) | 145 |
| After `useMasterRevisionDetailController.ts` | 258 |
| After `detail/MasterRevisionDetailWorkspace.vue` | 182 |

All three orchestrators are under the ~400 LOC bar.

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `TemplateDetailDevWorkspace.vue` | ~416 | Already a child of TemplateDetail; further peel optional |
| `TemplatePackageHubView.vue` / `MasterPackageHubView.vue` | ~500+ | Owned by hubs agent |

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no user-visible selector or flow changes (`WorkspaceTabShell` actions / catalog filters / dialogs preserved via prop-event passthrough).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match R2 / R-frontend pattern: thin orchestrator + focused child SFCs + composable kernel
- `defineModel` used for tab / dialog / catalog filter bindings (avoid `vue/no-mutating-props`)
- BDD not applicable for this hygiene wave
