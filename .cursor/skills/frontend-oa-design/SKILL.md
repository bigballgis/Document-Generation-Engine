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
- Brand: REDBC `#DB0011` (hover `#AF0010`), GREENBC `#00847F` (hover `#006A66`),
  header/surface `#FFFFFF`, text `#1A1A1A`, muted `#5C6670`, border `#E4E7EB`.
- Typography: system UI stack already set; one type scale, consistent weights/line-heights.
- Brand color = primary actions + emphasis only. Never a full background wash.

## Layout

- OA shell: fixed top brand bar (logo slot + brand switch + session) + left navigation +
  spacious desktop-first content region.
- Desktop-first; comfortable max content width; consistent spacing scale (e.g. 4/8/12/16/24).
- Logo and brand assets come from the shared slot and switch with theme; no page-local branding.

## Components

| Element | Required quality |
| --- | --- |
| Data tables | clear headers, aligned columns, sensible density, sort where useful, sticky header for long lists, pagination, empty/loading/error states |
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

## Related

- `.cursor/agents/frontend-engineer.md`, `.cursor/agents/e2e-uiux-reviewer.md`
- `docs/architecture/management-ui-constitution.md`
