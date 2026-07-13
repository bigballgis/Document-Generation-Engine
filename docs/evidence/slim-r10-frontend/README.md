# Slim R10-frontend — stores / API / detail body peel evidence

Branch: `feat/slim-r10-frontend`  
Base: `main` @ `0c58a0c`  
Date: 2026-07-13

## Scope completed

Behavior-preserving peel of frontend stores, template API, detail body, types, and auth role helpers toward soft target **&lt;300** (MUST: `TemplateDetailViewBody.vue` **&lt;200**). Pattern: thin orchestrator / facade + sibling domain modules; public import paths (`@/api/templates`, `@/types/template`, `@/auth/roles`, Pinia store IDs) preserved via barrel / factory re-exports. English-first (no new i18n keys). BDD not applicable for pure peel.

Skipped: `types/generated/openapi-v1.ts`.

### MUST

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `TemplateDetailViewBody.vue` | **239** | **57** | `TemplateDetailLoadedBody.vue` (**202**) |
| `stores/templates.ts` | **446** | **278** | `templatesStoreHelpers.ts` + `createTemplatesLifecycleActions.ts` + `createTemplatesAuthoringActions.ts` |
| `stores/templatePanelData.ts` | **399** | **94** | `templatePanelDataTypes.ts` + `createTemplatePanelDataActions.ts` (+ test/preview + version/export factories) |
| `stores/masters.ts` | **371** | **291** | `mastersStoreHelpers.ts` + `createMastersRevisionActions.ts` |
| `api/templates.ts` | **568** | **8** (barrel) | `templatesList.ts` / `templatesDetail.ts` (+ `templatesDetailPanels.ts`) / `templatesLifecycle.ts` / `templatesBindings.ts` (+ `templatesNormalize.ts`) |

### SHOULD

| File | Before | After | Collaborator(s) |
|------|--------|-------|-----------------|
| `types/template.ts` | **627** | **10** (barrel) | `templateCore.ts` / `templatePaste.ts` / `templateLifecycle.ts` / `templatePreview.ts` / `templateApiAccess.ts` / `templateExport.ts` |
| `auth/roles.ts` | **290** | **55** (barrel) | `roleCapabilityCore.ts` / `roleCapabilitiesMasters.ts` / `roleCapabilitiesTemplates.ts` / `roleCapabilitiesContentCollab.ts` |

LOC via `(Get-Content file).Count`.

## Extracted files

- `frontend/src/views/templates/detail/TemplateDetailLoadedBody.vue`
- `frontend/src/api/templatesList.ts`
- `frontend/src/api/templatesDetail.ts`
- `frontend/src/api/templatesDetailPanels.ts`
- `frontend/src/api/templatesLifecycle.ts`
- `frontend/src/api/templatesBindings.ts`
- `frontend/src/api/templatesNormalize.ts`
- `frontend/src/stores/templatesStoreHelpers.ts`
- `frontend/src/stores/createTemplatesLifecycleActions.ts`
- `frontend/src/stores/createTemplatesAuthoringActions.ts`
- `frontend/src/stores/templatePanelDataTypes.ts`
- `frontend/src/stores/createTemplatePanelDataActions.ts`
- `frontend/src/stores/createTemplatePanelTestPreviewActions.ts`
- `frontend/src/stores/createTemplatePanelVersionExportActions.ts`
- `frontend/src/stores/mastersStoreHelpers.ts`
- `frontend/src/stores/createMastersRevisionActions.ts`
- `frontend/src/types/templateCore.ts`
- `frontend/src/types/templatePaste.ts`
- `frontend/src/types/templateLifecycle.ts`
- `frontend/src/types/templatePreview.ts`
- `frontend/src/types/templateApiAccess.ts`
- `frontend/src/types/templateExport.ts`
- `frontend/src/auth/roleCapabilityCore.ts`
- `frontend/src/auth/roleCapabilitiesMasters.ts`
- `frontend/src/auth/roleCapabilitiesTemplates.ts`
- `frontend/src/auth/roleCapabilitiesContentCollab.ts`

## Residuals (stores / api / types ≥300 after this wave)

| LOC | Path |
|-----|------|
| 304 | `frontend/src/stores/apiPolicy.ts` |

Target MUST/SHOULD kernels for this wave are all &lt;300 (or facade barrels).  
`frontend/src/auto-imports.d.ts` may regenerate on build (line-ending noise); left unstaged.

## Gates

```text
pnpm -C frontend lint        # GREEN (exit 0)
pnpm -C frontend type-check  # GREEN (exit 0)
pnpm -C frontend test        # GREEN — Test Files 191 passed; Tests 1159 passed
pnpm -C frontend build       # GREEN (vue-tsc --noEmit && vite build; built in ~24s)
```

## E2E

Skipped full e2e: no intentional user-visible selector or flow changes (prop/event contracts and facade exports preserved; logic moved into sibling modules).

## Approach notes

- English-first i18n (reused existing keys)
- Match R5–R9 thin-orchestrator / facade + sibling peels
- `@/api/templates`, `@/types/template`, `@/auth/roles` remain stable import surfaces
- `envelope.test.ts` updated to assert unwrap usage on template domain modules (barrel no longer owns HTTP)
- BDD not applicable for this hygiene wave
