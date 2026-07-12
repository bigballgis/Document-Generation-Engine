# Slim R2-frontend — TemplateDetailView peel evidence

Branch: `feat/slim-r2-frontend`  
Base: `main` @ `451c6c2`  
Date: 2026-07-12

## Scope completed

Behavior-preserving UI-shell peel of `TemplateDetailView.vue`. Reused existing F6 composables (`useTemplateDetailController`, lifecycle gates/actions/decisions, tabs, journey context, policy credentials). Did **not** re-split the lifecycle kernel. No intentional UX copy/flow changes. BDD not applicable.

### TemplateDetailView

| File | LOC (physical) |
|------|----------------|
| **Before** `TemplateDetailView.vue` | **577** |
| After `TemplateDetailView.vue` (orchestrator) | 388 |
| After `detail/TemplateDetailJourneyStack.vue` | 99 |
| After `detail/TemplateDetailLifecycleActions.vue` | 124 |
| After `detail/TemplateDetailDialogs.vue` | 146 |
| After `detail/TemplateDetailLegacyWorkspace.vue` | 213 |

Orchestrator is under the ~350–400 LOC bar; journey / lifecycle actions / dialogs / legacy tab shell hold the extracted surface.

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `TemplatePackageHubView.vue` | ~501 | Package hub orchestration; next peel candidate if residual wave continues |
| `MasterPackageHubView.vue` | ~508 | Mirror hub pattern |
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

Skipped full e2e: no user-visible selector or flow changes (`data-testid` / journey / dialog wiring preserved via prop-event passthrough).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match Wave R-frontend pattern: thin orchestrator + focused child SFCs; keep F6 composable kernel intact
- `defineModel` used for dialog open flags and legacy tab/filter bindings (avoid `vue/no-mutating-props`)
- BDD not applicable for this hygiene wave
