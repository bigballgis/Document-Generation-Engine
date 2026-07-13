# Slim Frontend — Wave 3 evidence (oversized SFC / composable splits)

Branch: `feat/slim-frontend`  
Base: `main` @ `3263ec2`  
Date: 2026-07-12

## Scope completed

Behavior-preserving decomposition of oversized Vue SFCs. No intentional UX copy/flow changes. Existing `data-testid` selectors preserved.

### 1. TemplateAuthoringBindingsPanel (highest priority)

| File | LOC (physical) |
|------|----------------|
| **Before** `TemplateAuthoringBindingsPanel.vue` | **1249** |
| After `TemplateAuthoringBindingsPanel.vue` (orchestrator) | 170 |
| After `TemplateAuthoringBindingsList.vue` | 166 |
| After `TemplateAuthoringBindingEditor.vue` | 232 |
| After `useTemplateAuthoringBindingsPanel.ts` | 462 |
| After `mergeAnchorVisibilityRule.ts` | 36 |
| After `mergeAnchorVisibilityRule.test.ts` | 91 |

Orchestrator is under the ~400–500 LOC bar; list/editor/composable hold the extracted surface.

### 2. ManagementShell (second hotspot)

| File | LOC (physical) |
|------|----------------|
| **Before** `ManagementShell.vue` | **671** |
| After `ManagementShell.vue` (orchestrator) | 311 |
| After `ManagementShellHeader.vue` | 218 |
| After `ManagementShellNav.vue` | 228 |

Existing `ManagementShell.test.ts` retained (no selector changes).

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `ControlledStructuredContentEditor.vue` | ~848 / 785 non-blank | Next candidate: toolbar + block surface extracts |
| `AuditConsoleView.vue` | ~588 | View orchestration; can peel filter/table panes |
| `TemplateDetailView.vue` | ~577 | Reuse F6 composables only; do **not** re-split lifecycle kernel |

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no user-visible selector or flow changes (`data-testid` values for paste residue, help menu, notification bell, skip-link, etc. unchanged). Prefer existing docker e2e smoke only if selectors regress.

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Views/panels stay orchestration-only where split
- Pure visibility-rule merge extracted + unit-tested
- BDD not applicable for this hygiene wave
