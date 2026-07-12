# Slim R5-frontend — DevWorkspace / NotificationBell / DashboardJourney peel evidence

Branch: `feat/slim-r5-frontend`  
Base: `main` @ `6c1ecd1`  
Date: 2026-07-13

## Scope completed

Behavior-preserving peel of three remaining soft-budget FE hotspots. Pattern: thin SFC/composable facade + focused child SFC or collaborator composable. No intentional UX copy/flow changes. Public component/composable APIs kept stable. BDD not applicable for pure peel.

### 1. TemplateDetailDevWorkspace

| File | LOC (physical) |
|------|----------------|
| **Before** `TemplateDetailDevWorkspace.vue` | **416** |
| After `TemplateDetailDevWorkspace.vue` (orchestrator) | 236 |
| After `TemplateDetailDevWorkspaceActions.vue` | 146 |
| After `useTemplateDetailDevWorkspace.ts` | 164 |

Target: &lt;300 — **met** (236).

### 2. NotificationBell

| File | LOC (physical) |
|------|----------------|
| **Before** `NotificationBell.vue` | **400** |
| After `NotificationBell.vue` (shell) | 201 |
| After `NotificationDropdownPanel.vue` | 245 |

Target: &lt;280 — **met** (201).

### 3. useDashboardJourney

| File | LOC (physical) |
|------|----------------|
| **Before** `useDashboardJourney.ts` | **387** |
| After `useDashboardJourney.ts` (facade) | 142 |
| After `useDashboardJourneyResolutions.ts` | 303 |

Target: &lt;280 — **met** (142). Public return shape of `useDashboardJourney()` unchanged.

## Extracted files

- `frontend/src/views/templates/detail/TemplateDetailDevWorkspaceActions.vue`
- `frontend/src/views/templates/detail/useTemplateDetailDevWorkspace.ts`
- `frontend/src/components/layout/NotificationDropdownPanel.vue`
- `frontend/src/composables/useDashboardJourneyResolutions.ts`

## Residuals (not split this wave)

| File | Approx LOC | Notes |
|------|------------|--------|
| `TemplateTestDataSetPanel.vue` | ~372 | Mid-tier; optional next peel |
| `useTemplateLifecycleDecisions.ts` | ~340 | Mid-tier; optional next peel |
| `useDashboardJourneyResolutions.ts` | 303 | New collaborator; under soft Vue bar; optional deepen later |

Did **not** expand into optional mid-tier peels once the three primary targets were clearly under budget and gates were green.

## Gates

```text
pnpm -C frontend lint        # GREEN
pnpm -C frontend type-check  # GREEN
pnpm -C frontend test        # GREEN (191 files / 1159 tests)
pnpm -C frontend build       # GREEN
```

## E2E

Skipped full e2e: no user-visible selector or flow changes (`data-testid` preserved on NotificationBell; DevWorkspace actions/dialogs wired via prop-event passthrough; `useDashboardJourney` return contract unchanged).

## Approach notes

- English-first i18n (reused existing keys; no new Chinese-only strings)
- Match R3/R4 thin-orchestrator + child SFC + composable kernel
- BDD not applicable for this hygiene wave
