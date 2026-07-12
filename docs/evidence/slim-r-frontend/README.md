# Slim R-frontend — residual Wave evidence

Branch: `feat/slim-r-frontend`  
Base: `main` @ `09fca45`  
Date: 2026-07-12

## Scope completed

Behavior-preserving decomposition of remaining oversized Vue SFCs from Wave 3 residuals. No intentional UX copy/flow changes. Existing `data-testid` selectors preserved. BDD not applicable.

### 1. ControlledStructuredContentEditor (highest priority)

| File | LOC (physical) |
|------|----------------|
| **Before** `ControlledStructuredContentEditor.vue` | **864** |
| After `ControlledStructuredContentEditor.vue` (orchestrator) | 177 |
| After `useControlledStructuredContentEditor.ts` | 551 |
| After `StructuredContentEditorToolbar.vue` | 154 |
| After `StructuredContentBlockCard.vue` | 191 |

Orchestrator is under the ~400–500 LOC bar; toolbar / block card / composable hold the extracted surface.

### 2. AuditConsoleView (second hotspot)

| File | LOC (physical) |
|------|----------------|
| **Before** `AuditConsoleView.vue` | **588** |
| After `AuditConsoleView.vue` (orchestrator) | 155 |
| After `useAuditConsole.ts` | 323 |
| After `AuditConsoleFilters.vue` | 125 |
| After `AuditManagementEventsTable.vue` | 99 |
| After `AuditLifecycleEventsTable.vue` | 122 |

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `TemplateDetailView.vue` | ~577 | Already F6-composablized (`useTemplateDetailController` + detail tabs). Remaining bulk is prop-drill / journey / dialog wiring — peel UI shells next if needed; do **not** re-split lifecycle kernel. |

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no user-visible selector or flow changes (`data-testid` values for structured editor toolbar / paste area / undo-redo, audit console filters/tables unchanged in behavior).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match Wave 3 pattern: thin orchestrator + focused child SFCs + composable kernel
- Audit filters use `defineModel` field bindings (avoid `vue/no-mutating-props`)
- BDD not applicable for this hygiene wave
