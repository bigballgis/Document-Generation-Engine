# Slim R4-panels — clause / version-lines / decision-dialog peel evidence

Branch: `feat/slim-r4-panels`  
Base: `5350d39`  
Date: 2026-07-13

## Scope completed

Behavior-preserving peel of template panel/dialog orchestrators. Pattern: thin SFC orchestrator + focused child SFCs + composable kernel. No intentional UX copy/flow changes. BDD not applicable.

### 1. TemplateClauseAuthoringPanel

| File | LOC (physical) |
|------|----------------|
| **Before** `TemplateClauseAuthoringPanel.vue` | **446** |
| After `TemplateClauseAuthoringPanel.vue` (orchestrator) | 101 |
| After `useClauseAuthoringPanel.ts` | 288 |
| After `ClauseAuthoringReferencesTable.vue` | 90 |
| After `ClauseAuthoringDialogs.vue` | 125 |

### 2. TemplateVersionLinesPanel

| File | LOC (physical) |
|------|----------------|
| **Before** `TemplateVersionLinesPanel.vue` | **438** |
| After `TemplateVersionLinesPanel.vue` (orchestrator) | 117 |
| After `useVersionLinesPanel.ts` | 293 |
| After `VersionLinesTable.vue` | 156 |

### 3. TemplateLifecycleDecisionDialog (WIP completed)

| File | LOC (physical) |
|------|----------------|
| **Before** `TemplateLifecycleDecisionDialog.vue` | **410** |
| After `TemplateLifecycleDecisionDialog.vue` (orchestrator) | 200 |
| After `useLifecycleDecisionDialog.ts` | 265 |

All three orchestrators are under the ~400 LOC bar.

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `TemplateDetailDevWorkspace.vue` | ~385 | Already under bar; optional further peel |
| `NotificationBell.vue` | ~355 | Already under bar; optional further peel |

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no user-visible selector or flow changes (dialog/table actions preserved via prop-event / `defineModel` passthrough).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match R3 thin-orchestrator + child SFC + composable kernel
- `defineModel` used for dialog open state / form bag bindings (avoid `vue/no-mutating-props`)
- BDD not applicable for this hygiene wave
