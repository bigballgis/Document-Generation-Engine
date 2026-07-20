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

4. **Management layout is fluid system-wide (SYS-NORM Wave 1).**
   - All management pages using `AppPageLayout` default to **fluid** (full shell content width).
   - **`contained`** (1440px max) is an **opt-in exception** only when a slice explicitly requires a centered narrow form/wizard rhythm — do not use it as the management default.

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
| **Management default (system-wide)** | `fluid` (default) | None (100% of shell content) | Catalogs, hubs, detail/workspace, dashboard, audit, identity |
| Opt-in narrow form/wizard only | `contained` | `1440px` | Explicit exception when a slice requires centered narrow rhythm |

Implementation note: `AppPageLayout` accepts `layoutVariant` (`contained` | `fluid`).
**Default is `fluid`** (SYS-NORM Wave 1 / Confirmed 2026-07-21). When `fluid`, omit
max-width; when `contained`, apply `1440px` centered content. Prefer omitting the prop
(inherit fluid) over repeating `layout-variant="fluid"` on every call site.

Management pages must not waste horizontal space: tables and workspaces expand to
available width; sticky header and horizontal scroll only when column count genuinely
exceeds viewport.

## Review Checklist

### frontend-engineer (before handoff)

- [ ] No user-facing column renders raw UUID or internal id as primary text.
- [ ] Entity primary columns use `EntityLinkCell` (or documented equivalent) with correct route.
- [ ] Links omitted or disabled when session lacks read access; no data leak.
- [ ] Filters follow the matrix; entity filters use async `AppSearchSelect`.
- [ ] Management pages use fluid layout (default); `contained` only when explicitly justified.
- [ ] Subtitle pattern applied where secondary identifiers help operators.
- [ ] i18n keys for labels, placeholders, empty states; English base in `en.ts`.
- [ ] Unit tests cover link vs non-link branches and filter wiring where non-trivial.

### e2e-uiux-reviewer (evidence pass)

- [ ] Screenshot catalog tables at 1440×900 — no UUID columns visible.
- [ ] Click entity link navigates to expected detail; forbidden user sees no link/leak.
- [ ] Filter row shows select for enums, search select for entities — not mismatched controls.
- [ ] Fluid management pages use horizontal space; no excessive empty margin on wide viewports.
- [ ] Entity cells have no overflow/overlap at target desktop widths.
- [ ] Both REDBC and GREENBC verified on changed views.

## Traceability

| Source | Relationship |
| --- | --- |
| [management-ui-constitution.md](./management-ui-constitution.md) | Shell, login-first sequence, OA product bar; this doc extends it with entity column and catalog UX rules. |
| [usability-review.md](../product/usability-review.md) | Confirmed baselines: English-first, desktop-first OA layout, role-aware navigation, fail-closed forbidden routes — entity display must align with friendly, task-oriented governance surfaces. |
| `.cursor/skills/frontend-entity-display/SKILL.md` | Step-by-step implementer workflow and anti-patterns. |
| `.cursor/skills/frontend-oa-design/SKILL.md` | Visual tokens, shared component vocabulary, layout variants. |
| `.cursor/rules/frontend-entity-display-constitution.mdc` | Cursor enforcement for `frontend/**/*.{vue,ts}`. |

## Implementation status

Rollout phases are **not** formal `master-plan.md` phases; they track constitution adoption
across management UI surfaces. Evidence mirror:
[execution-sync-ledger.md](../plan/execution-sync-ledger.md) (UX entity display slice).

| Phase | Scope | Status | Evidence (2026-07-08) |
| --- | --- | --- | --- |
| **0** | Governance — this constitution, `.cursor/skills/frontend-entity-display/SKILL.md`, `.cursor/rules/frontend-entity-display-constitution.mdc` | **Done** | Landed with P13 slice `115e2d7` |
| **1** | Shared primitives — `EntityLinkCell`, `AppPageLayout` `layoutVariant` (`fluid` \| `contained`), `AuditConsoleView` entity columns + fluid layout | **Done** | `EntityLinkCell.vue`, `AppPageLayout.vue`, `AuditConsoleView.vue`; Vitest on link cell + layout variant |
| **2** | Display-name enrichment — backend `*DisplayName` on list/summary APIs (`updatedByDisplayName`, `submitterDisplayName`, `createdByDisplayName`); `ManagementUserDisplayService`; OpenAPI v1 + codegen; frontend catalog lists (template, master, content module), dashboard submitter, `TemplateContentModuleReferencesPanel`, `MasterRevisionLinesPanel`, `TemplateVersionLinesPanel`; `userDisplay.ts` + `useEntityLinkTargets` | **Done** | Backend: `mvn -B -ntp -f backend/pom.xml -Pdev-fast test` on display-name test classes ✓. Frontend: `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**844**/844) |
| **3** | Catalog filters + fluid — `ApiPolicyHomeView` (entity link column, lifecycle/group filters, fluid); `GroupManagementView` / `GroupManagementPanel` (search + fluid) | **Done** | Same frontend gates as Phase 2; `ApiPolicyHomeView.vue`, `GroupManagementView.vue` Vitest |
| **4** | Remaining surfaces — collaboration work-item queues, batch-test history panels, identity user-management columns, audit actor enrichment on all event types, E2E UIUX evidence pass (REDBC/GREENBC screenshots per review checklist) | **Done** | `BatchTestHistoryPanel` `createdByDisplayName`; `TemplateInvocationsPanel` technical-id copy buttons; `TemplateDetailOverviewTab` master name link; collaboration/dashboard display names + audit actor enrichment; **tail:** release/version/revision detail `updatedByDisplayName`, `TemplateReleaseVersionHistoryPanel`, `DashboardView` fluid; frontend `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**847**/847); E2E UIUX manifest **PASS** (`UX-ENTITY-DISPLAY-uiux-evidence.spec.ts` **1/1**, manifest `frontend/e2e/evidence/UX-ENTITY-DISPLAY-uiux-manifest.md`) |

**Not in scope for this rollout:** formal phase status changes; LR-C wave tasks (see
[LRP-C detail](../plan/detail/LRP-C-usability-deepening.md)).

## Pending Questions

- Whether audit and API invocation log tables expose a stable business label for all entity
  types or require type-specific subtitle rules (Phase 4 — audit console has
  `EntityLinkCell` for resource columns; actor display enrichment landed).
- Standard copy for cross-group entity references when the viewer's scope excludes the owning group.
