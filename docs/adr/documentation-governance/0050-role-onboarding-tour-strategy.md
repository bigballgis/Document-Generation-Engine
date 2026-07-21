---
id: ADR-0050
title: Role Onboarding Tour Strategy
status: Proposed
date: 2026-07-05
deciders: architecture, frontend-engineer
related:
  - docs/plan/detail/LRP-C-usability-deepening.md
  - docs/product/catalog-navigation-ux.md
  - docs/adr/authorization-security/0070-role-compression-six-roles.md
  - docs/behavior/sys-norm-roles.md
  - docs/security/permission-matrix.md
---

# ADR-0050 — Role Onboarding Tour Strategy

## Context

New bank users (template authors, testers, approvers, group admins) land on a feature-rich
management UI with no guided first-run experience. The role-journey timeline (P21) shows the
journey statically, but does not walk a first-time user through the actual clicks.

Industry baseline: enterprise SaaS apps ship a first-run tour that highlights 3–6 key
affordances, dismissable and re-triggerable from the help menu.

## Decision

Use **Element Plus `el-tour`** (bundled in `element-plus@2.9+`, no new dependency) to deliver
a role-aware first-run onboarding tour:

1. **One tour per role** — align to the **six-role** assignable catalog
   ([ADR-0070](../authorization-security/0070-role-compression-six-roles.md)):
   `GLOBAL_ADMIN`, `GROUP_ADMIN` (absorbs former approver tour), `DOCUMENT_AUTHOR`
   (merges former designer/author tours; interim L1 labels OK), `TEMPLATE_TESTER`,
   `LEGAL_REVIEWER`, `AUDIT_ADMIN`. Each tour highlights the 3–6 affordances that role
   uses most.
2. **Dismissable + re-triggerable** — a "Take the tour" entry in the help menu restarts the
   tour for the current role.
3. **Persistence** — completion is stored in `localStorage` (`docgen.onboarding.<role>`);
   the tour auto-runs only once per role per browser unless the user resets it.
4. **No backend dependency** — the tour is pure frontend; it does not call the API or change
   permissions. It only highlights existing UI.
5. **i18n** — tour step text uses message keys (`onboarding.<role>.stepN.title` /
   `.body`) so it ships in English first with zh-CN additive.

## Consequences

- **Positive:** first-run friction drops; users discover the role-specific affordances
  without reading docs; no new dependency (el-tour is already in the bundle).
- **Negative:** tour steps must be maintained when the UI changes — a step pointing at a
  removed selector breaks silently. Mitigation: tour steps target stable data attributes
  (`data-tour="template-create-btn"`) not CSS classes.
- **Neutral:** the tour does not teach the full lifecycle (that is the role-journey
  timeline's job); it only points at the entry points.
- **Wave 5:** retired role codes must not appear as assignable picker options; journey
  resolution follows [sys-norm-roles.md](../../behavior/sys-norm-roles.md) ROLE-014.

## Alternatives considered

- **Driver.js / Shepherd.js** — rejected: adds a new dependency; el-tour already ships with
  Element Plus and matches the design system.
- **Video onboarding** — rejected: production cost; cannot be navigated at the user's pace.
- **No tour** — rejected: first-run friction is a known usability gap (LRP §1 finding 10).
