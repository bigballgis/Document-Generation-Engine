# Slim R3-hubs — package hub peel evidence

Branch: `feat/slim-r3-hubs`  
Base: `main` @ `9131486`  
Date: 2026-07-12

## Scope completed

Behavior-preserving split of package hub views. Reused existing catalog/table patterns (`useDataTableFilters` / `useCatalogPagination` via `useTemplatePolicyCredentials`) and R2 thin-orchestrator + child SFC shape. No intentional UX copy/flow changes. BDD not applicable.

### 1. TemplatePackageHubView

| File | LOC (physical) |
|------|----------------|
| **Before** `TemplatePackageHubView.vue` | **501** |
| After `TemplatePackageHubView.vue` (orchestrator) | 145 |
| After `useTemplatePackageHub.ts` | 358 |
| After `hub/TemplatePackageHubActions.vue` | 40 |
| After `hub/TemplatePackageHubWorkspace.vue` | 106 |

Also extended `useTemplatePolicyCredentials` with optional `revealSecret` so the hub keeps tab-inline secret reveal while sharing credential filter/pagination/actions.

### 2. MasterPackageHubView

| File | LOC (physical) |
|------|----------------|
| **Before** `MasterPackageHubView.vue` | **508** |
| After `MasterPackageHubView.vue` (orchestrator) | 112 |
| After `useMasterPackageHub.ts` | 193 |
| After `hub/MasterPackageHubActions.vue` | 33 |
| After `hub/MasterPackageHubBody.vue` | 49 |
| After `hub/MasterPackageHubDialogs.vue` | 45 |

Both orchestrators are under the ~350–400 LOC bar.

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `ContentModuleDetailView.vue` | ~477 | Detail orchestration |
| `TemplateDetailDevWorkspace.vue` | ~416 | Already a child of TemplateDetail; further peel optional |
| `TemplateListView.vue` | ~416 | List + filters surface |

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no user-visible selector or flow changes (hub header/actions, version/revision panels, API-access credential reveal path preserved via prop-event / `revealSecret` passthrough).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Shared credential catalog filters/tables via `useTemplatePolicyCredentials`
- `defineModel` used for dialog open flags and workspace tab/filter bindings (avoid `vue/no-mutating-props`)
- BDD not applicable for this hygiene wave
