---
name: frontend-oa-design
description: Locked bank OA visual + interaction design system for the management UI. Use whenever building or changing any user-facing Vue surface — shell, navigation, tables, forms, dialogs, states, theming — to keep an advanced, professional, beautiful enterprise OA look across REDBC and GREENBC brands.
---

# Frontend Bank OA Design System (locked)

The management UI must look like a mature, premium bank OA product — not a workbench stub.
This skill is the locked execution standard. Pair with `docs/architecture/management-ui-constitution.md`.

## Foundations (single source of truth)

- Tokens: `frontend/src/theme/tokens.ts` (brand presets) + CSS variables in
  `frontend/src/styles/global.scss`. Components reference variables/tokens — never raw hex/px.
- Brand registry: `frontend/src/config/brands.ts` (`BRAND_REGISTRY`, codes `REDBC`/`GREENBC`,
  default `REDBC`). Theme applied by `applyBrandTheme(preset)` → sets `html[data-brand]` +
  CSS vars (`--brand-primary`, `--color-primary`, `--brand-header-bg`, `--nav-surface-bg`, …).
  Element Plus primary controls are wired to `--brand-primary` in `global.scss`.
- Brand: REDBC `#DB0011` (hover `#AF0010`), GREENBC `#00847F` (hover `#006A66`),
  header/surface `#FFFFFF`, text `#1A1A1A`, muted `#5C6670`, border `#E4E7EB`.
- Logo: `components/branding/BrandLogo.vue` (imports `assets/brands/redbc-logo.svg` /
  `greenbc-logo.svg`); brand switcher lives in `ManagementShell.vue` header
  (`AppSearchSelect.brand-switcher`); persistence via `useAppStore` → localStorage `docgen.app.brand`.
- Typography: system UI stack already set; one type scale, consistent weights/line-heights.
- Brand color = primary actions + emphasis only. Never a full background wash.

## Shared component vocabulary (use these before building new ones)

`frontend/src/components/common/`: `AppPageLayout`, `AppDataTable`, `AppTablePagination`,
`AppSearchSelect`, `CatalogFilterToolbar`, `EntityLinkCell`, `TableColumnHeader`, `SectionPanelHeader`,
`WorkspaceTabShell`, `LoadErrorPanel`, `EmptyStatePanel`, `ScopedGroupSelect`,
`LifecycleCommentDialog`, `ContextHelpTrigger`. Layout: `components/layout/ManagementShell.vue`,
`AppBreadcrumb`, `PageHeader`. Status badges per domain (`MasterStatusBadge`,
`ContentModuleStatusBadge`). Building a table/pagination/empty-state from raw Element Plus
when a shared wrapper exists is a review blocker.

## Layout

- OA shell: fixed top brand bar (logo slot + brand switch + session) + left navigation +
  spacious desktop-first content region.
- Desktop-first; consistent spacing scale (e.g. 4/8/12/16/24).
- **`AppPageLayout` variants** (`frontend/src/components/layout/AppPageLayout.vue`):
  - **`fluid` (default)** — all management pages (SYS-NORM Wave 1); no max-width; full shell content width.
  - **`contained` (opt-in)** — explicit exception only; `1440px` centered max width.
- Logo and brand assets come from the shared slot and switch with theme; no page-local branding.
- Entity display + filter rules: `.cursor/skills/frontend-entity-display/SKILL.md`.

## Components

| Element | Required quality |
| --- | --- |
| Data tables | clear headers, aligned columns, sensible density, sort where useful, sticky header for long lists, pagination, empty/loading/error states; **entity columns use `EntityLinkCell`** (human-readable label + optional subtitle + link when read access); never raw UUID as primary cell text |
| Filters | `CatalogFilterToolbar` for catalog pages; enum → select, entity → async `AppSearchSelect`, scoped group → `ScopedGroupSelect`; free-text only when API supports substring search |
| Forms | aligned labels, grouped sections, inline validation states, clear primary vs secondary actions |
| Dialogs/drawers | purposeful, dismissable, focus-trapped, no layout shift |
| Buttons | primary/secondary/tertiary hierarchy; hover/active/focus/disabled/loading states |
| Status | badges/tags with consistent semantic color mapping |
| Navigation | role-aware items, active state, collapsed-safe |

## State completeness (mandatory)

- Every interactive element: hover, active, focus (visible), disabled, loading.
- Every async surface: empty, loading, error (recoverable), success.
- Permission-aware forbidden state uses the unified no-access view; no data/existence leak;
  preserve `traceId`/`auditId`.

## Quality bar (blockers if violated)

- No text overflow, clipping, overlap, or misaligned controls at target desktop widths.
- Accessible: visible focus order, sufficient contrast, labels/roles; a11y smoke green.
- Both REDBC and GREENBC presets render correctly.
- English-first; all strings via i18n keys (`.cursor/skills/i18n-english-first/SKILL.md`).

## Definition of done (frontend visual)

- Tokens used (no magic values), states complete, both brands verified, a11y smoke green,
  E2E functional + UIUX evidence captured. Verified by `e2e-uiux-reviewer`.

## Workspace tab shell pattern

Detail/workspace pages split **orientation** from **work**:

| Layer | Role | Actions |
| --- | --- | --- |
| Journey / timeline | Read-only progress + guidance | None |
| Top-level workspace tabs | Phase work area (design / testing / approval) | Single right-aligned action rail on tab row only |
| Nested sub-tabs | Content navigation within a phase | None |
| Dialogs | Comments, summaries, decisions | Confirm/cancel in modal |

Use `WorkspaceTabShell.vue` + context-specific tab panes. Never duplicate action bars
below the tab row or inside journey blocks. See `docs/architecture/management-ui-constitution.md`.

## Related

- `.cursor/agents/frontend-engineer.md`, `.cursor/agents/e2e-uiux-reviewer.md`
- `docs/architecture/management-ui-constitution.md`
- `docs/architecture/ux-entity-display-constitution.md`
- `.cursor/skills/frontend-entity-display/SKILL.md`
