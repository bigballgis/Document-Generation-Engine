---
name: frontend-entity-display
description: Entity display, navigable links, filter control selection, and fluid vs contained layout for management UI tables and catalogs. Use when adding or changing list views, table columns referencing entities, catalog filters, or AppPageLayout width on frontend surfaces.
---

# Frontend Entity Display

Pair with `.cursor/skills/frontend-oa-design/SKILL.md` (visual tokens) and
`docs/architecture/ux-entity-display-constitution.md` (constitutional rules).

## When to use

- New or refactored **catalog / list / audit table** views.
- Table columns that reference another entity (template, master, user, group, module, API package).
- Catalog **filter toolbars** (status, entity picker, search).
- Choosing **fluid vs contained** layout on `AppPageLayout`.

## Required components

| Component | Location | Use |
| --- | --- | --- |
| `EntityLinkCell` | `frontend/src/components/common/EntityLinkCell.vue` | Primary entity column: label + optional subtitle + router link when allowed |
| `AppPageLayout` | `frontend/src/components/layout/AppPageLayout.vue` | **Default `fluid`** for all management pages (SYS-NORM Wave 1); `"contained"` only as explicit opt-in |
| `AppSearchSelect` | `frontend/src/components/common/AppSearchSelect.vue` | Async entity filter/search; extend with remote method + options slot |
| `CatalogFilterToolbar` | `frontend/src/components/common/CatalogFilterToolbar.vue` | Standard catalog filter row (search + filters + sort + chips) |
| `ScopedGroupSelect` | `frontend/src/components/common/ScopedGroupSelect.vue` | Group scope filter for global admins |

Supporting composables: `useCatalogTableControls`, `useTableFilterOptions`, `useActivatableTableRow`,
`useCapabilities` / `sessionStore.canAccessRoute()` for link eligibility.

## Step-by-step: new table with entity columns

```
1. Read behavior spec + permission matrix for read routes on referenced entities.
2. Use AppPageLayout (fluid by default). Do not set contained for management pages unless justified.
3. Define columns: primary entity fields via EntityLinkCell; status in separate column.
4. Wire CatalogFilterToolbar:
   - enums → type: 'select' + composable options
   - entity refs → AppSearchSelect with API search handler
   - name/code search → toolbar searchQuery
5. Map API rows: expose displayName, optional code/subtitle, entity id for link target only.
6. Gate links: pass linkTo only when canAccessRoute(detailRouteKey) (or domain capability).
7. Add unit tests: renders name not uuid; link vs plain text by permission mock.
8. Hand off to e2e-uiux-reviewer with fluid width + filter control evidence.
```

## Step-by-step: entity filter

```
1. Identify filter data kind (enum vs entity vs free-text) — see constitution matrix.
2. For entity: AppSearchSelect, debounced remote fetch, min query length if API requires.
3. Store filter value as stable id in filterValues; display label from selected option.
4. Clear chip / clear all resets id and label state.
5. Do not use el-input for entity id entry.
```

## Layout variants

| Variant | Prop | When |
| --- | --- | --- |
| `fluid` (default) | No max-width on content | **All management pages** (catalogs, hubs, detail, dashboard) |
| `contained` (opt-in) | `1440px` centered | Explicit exception only — narrow form/wizard rhythm |

```vue
<!-- Management default (fluid) -->
<AppPageLayout>...</AppPageLayout>

<!-- Explicit opt-in only -->
<AppPageLayout layout-variant="contained">...</AppPageLayout>
```

Confirmed (SYS-NORM Wave 1, 2026-07-21): management UI is fluid-everywhere; prior
catalog=fluid / detail=contained default is superseded.

## Anti-patterns (blockers)

- Raw `row.id` or UUID string in `<template #default>` table cells.
- `<el-button link>` duplicating EntityLinkCell per view with inconsistent truncate/a11y.
- Lifecycle status filter as free-text input.
- Template/master filter as paste-UUID text field.
- Management page forced to `contained` / 1440px leaving wide empty gutters on 1920px displays.
- Link to detail when `canAccessRoute` is false (permission leak or confusing 403).
- Hardcoded entity type labels — use i18n keys under domain namespaces.

## Definition of done

- [ ] Constitution rules satisfied (`docs/architecture/ux-entity-display-constitution.md`).
- [ ] Entity columns use `EntityLinkCell`; no raw UUID visible at 1440×900.
- [ ] Filters match matrix; entity filters use async `AppSearchSelect`.
- [ ] `AppPageLayout` fluid (default) for management pages; contained only if justified.
- [ ] i18n English keys for new labels/placeholders.
- [ ] Unit tests for display + link gating where non-trivial.
- [ ] `frontend-engineer` full gates green; UIUX evidence includes filter row + table width.

## Related

- `docs/architecture/ux-entity-display-constitution.md`
- `docs/architecture/management-ui-constitution.md`
- `.cursor/skills/frontend-oa-design/SKILL.md`
- `.cursor/agents/frontend-engineer.md`, `.cursor/agents/e2e-uiux-reviewer.md`
