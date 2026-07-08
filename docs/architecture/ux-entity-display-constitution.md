---
id: DOC-ARCH-UX-ENTITY-DISPLAY-CONSTITUTION
type: Architecture View
status: Accepted
sourceOfTruth: true
sourceOfTruthScope: "Management UI entity display, navigable links, filter control selection, and table layout width policy only; excludes product, domain, permission, API contract, and durable ADR decisions."
owners:
  - architecture
  - implementation
related:
  - docs/architecture/management-ui-constitution.md
  - docs/product/usability-review.md
  - .cursor/skills/frontend-entity-display/SKILL.md
  - .cursor/skills/frontend-oa-design/SKILL.md
---

# UX Entity Display Constitution

## Purpose

This document governs how the management UI presents **entities** (templates, masters,
content modules, users, groups, API packages, audit records, and similar domain objects)
in tables, filters, and cross-reference columns.

The goal is consistent **human-readable labels**, **navigable links when the user has read
access**, **correct filter controls**, and **layout width that matches page intent** —
so catalog pages feel professional and task-oriented rather than exposing internal identifiers.

It complements [Management UI Constitution](./management-ui-constitution.md) (shell and
delivery sequence) and [Usability Review](../product/usability-review.md) (product
experience baselines). It does not replace permission rules or API contracts.

## Constitutional Rules

1. **No raw UUID in user-facing entity columns.**
   - Primary columns show a human-readable name or code (e.g. template name, master code,
     user display name, group name).
   - UUIDs may exist in row data, route params, and API payloads — never as the default
     visible cell value in a management table or summary field.

2. **Entity names must link when the user has read access.**
   - When the session can open the entity detail route, the primary label is a navigable
     link (via `EntityLinkCell` or equivalent shared primitive).
   - When the user lacks read access, show the label as plain text (or em dash if unknown);
     do not link, do not leak existence beyond what permission rules allow.

3. **Filter controls must match data shape.**
   - Use the [Filter control matrix](#filter-control-matrix) — enum → select, entity → async
     search select, free-text only when full-text search is the intended query model.

4. **Layout variant must match page type.**
   - Table-heavy catalog/list pages use **fluid** width (full content region).
   - Form, wizard, and detail/workspace pages use **contained** width (1440px max).

5. **Reuse shared primitives before inventing page-local patterns.**
   - Building a one-off link cell, filter bar, or fixed 1440px wrapper when a shared
     component exists is a review blocker.

## Entity Display Format Standards

### Primary label + optional subtitle

| Part | Content | Example |
| --- | --- | --- |
| Primary label | Human-readable entity name or business code | `Annual Review Letter` |
| Subtitle (optional) | Secondary identifier useful to operators — code, version, scoped group | `TMPL-2024-001 · v3` |

Rules:

- Primary label is always present when the entity is loaded; use i18n fallback keys for
  missing names (e.g. `common.entity.unnamed`), never a raw id.
- Subtitle is secondary, muted typography; never duplicate the primary label.
- Status badges and lifecycle tags sit in their own column — not inside the link cell.
- Truncate with ellipsis + accessible `title` tooltip when space is constrained; no overlap
  with adjacent columns.

### Navigable link behavior

- Link target = canonical detail route for that entity type (`routeKeys.ts`).
- Middle-click / open-in-new-tab must work (real `<router-link>` or equivalent).
- Row click activation (where used) must not conflict with link click — follow
  `useActivatableTableRow` conventions.
- Forbidden or unknown entities: no link, unified no-access handling per permission matrix.

## Filter Control Matrix

| Data kind | Control | Component / pattern |
| --- | --- | --- |
| Fixed enum (lifecycle status, enabled flag, output format) | Single-select | `CatalogFilterToolbar` filter `type: 'select'` with static options from composables (e.g. `useLifecycleStatusFilterOptions`) |
| Entity reference (template, master, user, group, content module) | Async searchable select | `AppSearchSelect` with remote search API; debounced query; clearable |
| Scoped group (when session is global admin) | Group picker | `ScopedGroupSelect` |
| Full-text name/code search across catalog | Search input | `CatalogFilterToolbar` top-level search query |
| Free-text column filter | Text input | Only when the backend supports substring/contains on that field — document the query semantics in the view |

Anti-patterns:

- Enum rendered as free-text input (users must guess exact spelling).
- Entity id pasted into a text field instead of entity picker.
- Client-side-only filtering of large entity lists without search when the API supports query.

## Layout Policy

| Page intent | `AppPageLayout` variant | Max width | Examples |
| --- | --- | --- | --- |
| Catalog / list / audit table-heavy | `fluid` | None (100% of shell content) | Template list, master list, audit console, API package hub tables |
| Form / wizard / detail / workspace | `contained` | `1440px` | Template detail tabs, master revision workspace, create/edit dialogs context, dashboard cards |

Implementation note: `AppPageLayout` accepts `layoutVariant` (`contained` | `fluid`). When
`fluid`, omit max-width constraint; when `contained`, apply `1440px` centered content.
Existing pages may pass `maxWidth` until migrated — new work must use `layoutVariant`.

Table-heavy pages must not waste horizontal space: tables expand to available width;
sticky header and horizontal scroll only when column count genuinely exceeds viewport.

## Review Checklist

### frontend-engineer (before handoff)

- [ ] No user-facing column renders raw UUID or internal id as primary text.
- [ ] Entity primary columns use `EntityLinkCell` (or documented equivalent) with correct route.
- [ ] Links omitted or disabled when session lacks read access; no data leak.
- [ ] Filters follow the matrix; entity filters use async `AppSearchSelect`.
- [ ] Catalog pages use `layoutVariant="fluid"`; detail/form pages use `contained`.
- [ ] Subtitle pattern applied where secondary identifiers help operators.
- [ ] i18n keys for labels, placeholders, empty states; English base in `en.ts`.
- [ ] Unit tests cover link vs non-link branches and filter wiring where non-trivial.

### e2e-uiux-reviewer (evidence pass)

- [ ] Screenshot catalog tables at 1440×900 — no UUID columns visible.
- [ ] Click entity link navigates to expected detail; forbidden user sees no link/leak.
- [ ] Filter row shows select for enums, search select for entities — not mismatched controls.
- [ ] Fluid catalog pages use horizontal space; no excessive empty margin on wide tables.
- [ ] Contained detail pages respect 1440px rhythm; no overflow/overlap in entity cells.
- [ ] Both REDBC and GREENBC verified on changed views.

## Traceability

| Source | Relationship |
| --- | --- |
| [management-ui-constitution.md](./management-ui-constitution.md) | Shell, login-first sequence, OA product bar; this doc extends it with entity column and catalog UX rules. |
| [usability-review.md](../product/usability-review.md) | Confirmed baselines: English-first, desktop-first OA layout, role-aware navigation, fail-closed forbidden routes — entity display must align with friendly, task-oriented governance surfaces. |
| `.cursor/skills/frontend-entity-display/SKILL.md` | Step-by-step implementer workflow and anti-patterns. |
| `.cursor/skills/frontend-oa-design/SKILL.md` | Visual tokens, shared component vocabulary, layout variants. |
| `.cursor/rules/frontend-entity-display-constitution.mdc` | Cursor enforcement for `frontend/**/*.{vue,ts}`. |

## Pending Questions

- Whether audit and API invocation log tables expose a stable business label for all entity
  types or require type-specific subtitle rules.
- Standard copy for cross-group entity references when the viewer's scope excludes the owning group.
