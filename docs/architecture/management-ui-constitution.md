---
id: DOC-ARCH-MANAGEMENT-UI-CONSTITUTION
type: Architecture View
status: Accepted
sourceOfTruth: true
sourceOfTruthScope: "Management UI execution constitution, login-first overhaul sequence, and frontend productization priorities only; excludes product, domain, permission, API contract, and durable ADR decisions."
owners:
  - architecture
  - implementation
  - orchestration
dependsOn:
  - docs/architecture/orchestration-high-level-plan.md
  - docs/architecture/implementation-task-plan.md
  - docs/product/PRD.md
  - docs/domain/domain-model.md
  - docs/security/permission-matrix.md
related:
  - docs/architecture/security-view.md
  - docs/architecture/e06-task-sheet.md
  - docs/architecture/e11-role-journey-ui-continuation-plan.md
  - docs/architecture/e12-frontend-role-journey-development-plan.md
---

# Management UI Constitution

## Purpose

This document is the execution constitution for the management UI subtrack inside the broader end-to-end production capability chain.

It defines the login-first rebuild sequence, the non-negotiable product rules for the frontend shell, and the order in which the management experience must be improved.

It does not replace product requirements, domain rules, permission rules, or ADR decisions.

## Constitutional Rules

1. Real login is mandatory.
   - Role selection simulation is not a valid final login model.
   - The first management UI entry point must resolve through authenticated management-session state.

2. Local management authentication is the current baseline.
   - Eight-digit employee-ID usernames are required for test accounts.
   - Passwords must be stored and handled as hashes only.
   - Future company SSO remains a seam, not a claimed delivery.

3. The management UI is a product shell, not a workbench stub.
   - The shell must present a coherent OA-style operating surface.
   - English-first copy, white baseline, desktop-first layout, and stable navigation are mandatory.

4. Branding must stay centralized.
   - REDBC red and GREENBC green presets are both required.
   - Theme tokens, brand assets, and logo selection must come from shared primitives, not page-local overrides.

5. Authenticated identity must drive the post-login experience.
   - Roles, group scope, and landing pages must come from the authenticated session.
   - The frontend must not invent its own identity model after login.

6. Progressive replacement beats broad rewrite by accident.
   - Rebuild from login outward in thin vertical slices.
   - Keep each slice shippable, testable, and reviewable.

7. Behavior changes require documented confirmation first.
   - If a change affects login, navigation, permissions, or session behavior, update the owning source documents before implementation.

## Delivery Sequence

| Phase | Goal | Key Deliverables | Exit Criteria |
| --- | --- | --- | --- |
| Phase 1 | Login foundation | Real login form, backend auth session, logout, session persistence, expired-session return-to-login behavior, test management users | Users authenticate with real local accounts and the app no longer depends on role-only simulation |
| Phase 2 | Shell reconstruction | OA shell layout, top brand bar, left navigation, role-aware landing pages, theme switching, logo switching | The management UI feels like a coherent product shell rather than a temporary workbench |
| Phase 3 | Governance surface closure | Highest-value API governance, lifecycle governance, and audit console entry points | The main management tasks can be completed from the shell without falling back to temporary views |
| Phase 4 | Session-derived backend integration | Session-based identity propagation, authorization checks, audit summaries, management API data binding | Frontend and backend share the same authenticated identity model and fail closed on missing context |
| Phase 5 | Hardening | Accessibility, responsive behavior within desktop-first scope, regression tests, static scans, observability evidence | The rebuilt UI is stable enough for continued feature delivery |
| Phase 6 | Broader platform completion | Remaining management surfaces, deeper workflow links, migration of any legacy entry points | The management experience is consistent enough to support the broader product backlog |

## Initial Build Rules

- Start with login and session state before any broader navigation rewrite.
- Do not resurrect role-only simulated login once the real login path exists.
- Keep SSO out of scope until the project explicitly confirms its delivery phase.
- Prefer small, vertical, testable slices over large front-end rewrites that cannot be validated.
- Keep all user-facing copy and navigation stable during any migration period.

## Pending Questions

- Which role landing pages should be rebuilt first after login stabilizes.
- Whether the current shell should retain any legacy entry points during migration or cut over all at once.
- How far the first pass of governance-surface closure should extend before moving to hardening.

## Relationship to Other Plans

- [Orchestration High-Level Plan](./orchestration-high-level-plan.md) owns execution ordering and current priority.
- [Implementation Task Plan](./implementation-task-plan.md) owns wave and task decomposition.
- [PRD](../product/PRD.md), [Domain Model](../domain/domain-model.md), and [Permission Matrix](../security/permission-matrix.md) own the confirmed behavior and authority rules.
- This document governs the frontend shell and post-login management experience only; the project-wide chain now also includes template lifecycle, API management, audit, notifications, and deployment hardening.