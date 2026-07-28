# W1-6 — Legacy authoring duplication (investigate only)

**Slice:** `render-p0-preview-dedupe` / CRCH-W1-6  
**Date:** 2026-07-26  
**Scope:** report only — no refactor in this leaf.

## Surfaces

| Surface | File | Preview pane |
| --- | --- | --- |
| Dev-editor Design tab | `TemplateDetailDesignTab.vue` | Yes — side-by-side `AuthoringPreviewPane` |
| Legacy Authoring tab | `TemplateDetailAuthoringTab.vue` | No preview pane |

## Reachability

`isDevEditor` (and related workspace routing via `templateDevWorkspaceTabs`) selects the
dev-editor workspace. Legacy authoring remains reachable when the non-dev-editor template
detail path is used (non–dev-editor management route / legacy shell). Exact discriminator
is the template detail workspace mode flag that chooses `TemplateDetailLegacyWorkspace` vs
the design/testing/approval workspace.

## If deleted

Touch list would include at least:

- `TemplateDetailAuthoringTab.vue`
- `TemplateDetailLegacyWorkspace` (and its route wiring)
- any remaining `tab=authoring` deep-link branches beyond the preserved `testPreview` remap
- i18n keys under `templates.authoring.*` that are legacy-only

## Recommendation

Queue a dedicated leaf (BDD + E2E) to retire the legacy authoring shell **after** confirming
no production traffic still lands on the non-dev-editor path. Do **not** fold that into
preview de-duplication.
