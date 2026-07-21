---
id: DOC-BEHAVIOR-SYS-NORM-SHELL-FLUID-NAV
type: Behavior Spec
status: Confirmed
readiness: ready
program: SYS-NORM
wave: 1
slice: sys-norm-shell-fluid-nav
taskMaster: 145
related:
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md
  - docs/architecture/ux-entity-display-constitution.md
---

# SYS-NORM Wave 1 — Shell fluid + nav + EntityLink / Actions

> **TM id note:** Wave 1 leaf is Task Master **#145**. Handoff/BDD briefly cited `#144`, but
> `#144` remains **published-template-test-artifacts** (Done). Do not overwrite `#144`.

## Actor / goal

Management operators use a consistent fluid management canvas, complete nav icons,
Security group without retired D1 product surfaces, aligned Edit/More actions, and
navigable EntityLink cells for task-hub entities/groups and catalog group codes.

## Acceptance scenarios

### BDD-SYS-NORM-W1-001 — Management AppPageLayout defaults to fluid

**Given** any management page using `AppPageLayout` without an explicit variant  
**When** the page renders  
**Then** the layout uses the `fluid` variant (full shell content width)  
**And** `contained` remains available only when explicitly requested.

### BDD-SYS-NORM-W1-002 — Detail and hub pages are fluid

**Given** template/master hubs and detail workspaces  
**When** an authorized user opens them  
**Then** they use fluid layout (system-wide fluid supersedes prior contained default).

### BDD-SYS-NORM-W1-003 — Security nav hides Document brands and Legal entities

**Given** ADR-0071 Accepted (product-surface retire)  
**When** the shell builds Security group items  
**Then** Security contains only Audit and Legal holds (when routes are visible)  
**And** Document brands and Legal entities do not appear in the nav catalog.

### BDD-SYS-NORM-W1-004 — Routes for brands/entities remain non-nav (Wave 6 hard retire later)

**Given** Wave 1 nav hide  
**When** a user bookmarks a legacy brands/entities path  
**Then** routing may still resolve (hard delete is Wave 6)  
**And** the item is absent from `navGroupsCatalog`.

> **Wave 6 closeout pointer:** Hard retire of legacy routes/API/runtime is specified in
> [sys-norm-d1-brands.md](./sys-norm-d1-brands.md) **BDD-SYS-NORM-D1-006** (closes this soft
> allowance after Wave 6 deploy).

### BDD-SYS-NORM-W1-005 — Every remaining nav item has an icon (N10)

**Given** the published `NAV_GROUPS` catalog  
**When** the shell resolves icons  
**Then** every remaining nav item id maps to a concrete icon component  
**And** a contract test fails if any item lacks an icon.

### BDD-SYS-NORM-W1-006 — Nav icons render for entitlement and content items

**Given** a session that sees users, groups, masters, templates, modules, assets, API, audit, legal holds  
**When** the sidebar renders  
**Then** each visible item shows its mapped icon (collapsed-safe).

### BDD-SYS-NORM-W1-007 — Users and Groups share Edit/More actions primitive (N3)

**Given** an admin viewing Users or Groups tables with write access  
**When** the actions column renders  
**Then** Edit and More use the shared `TableEditMoreActions` primitive  
**And** alignment/spacing matches across both tables (`data-testid="table-edit-more-actions"`).

### BDD-SYS-NORM-W1-008 — More menus keep domain commands

**Given** Users More (enable/disable, reset password, delete when permitted) and Groups More (enable/disable)  
**When** the operator opens More  
**Then** existing commands remain available; only presentation is shared.

### BDD-SYS-NORM-W1-009 — Task hub entity name is EntityLink (N1)

**Given** a task-hub row with `entityName` and a navigable `path`  
**When** the Item column renders  
**Then** the name uses `EntityLinkCell` linking to the task entity path.

### BDD-SYS-NORM-W1-010 — Task hub groupCode is EntityLink when permitted (N1)

**Given** a task-hub row with `groupCode` and identity administration visible  
**When** the Group column renders  
**Then** the code uses `EntityLinkCell` to the groups catalog  
**And** when identity route is not visible, the code is plain text.

### BDD-SYS-NORM-W1-011 — Template catalog groupCode is EntityLink (N2)

**Given** the templates catalog and identity administration visible  
**When** the Group column renders  
**Then** `groupCode` uses `EntityLinkCell` (plain text when not permitted).

### BDD-SYS-NORM-W1-012 — Master catalog groupCode is EntityLink (N2)

**Given** the masters catalog and identity administration visible  
**When** the Group column renders  
**Then** `groupCode` uses `EntityLinkCell` (plain text when not permitted).

### BDD-SYS-NORM-W1-013 — Content-module catalog groupCode is EntityLink (N2)

**Given** the content-modules catalog and identity administration visible  
**When** the Group column renders  
**Then** `groupCode` uses `EntityLinkCell` (plain text when not permitted).

### BDD-SYS-NORM-W1-014 — Users authorized groups are EntityLink when permitted

**Given** a user row with `authorizedGroupCodes` and identity administration visible  
**When** the Groups column renders  
**Then** each code uses `EntityLinkCell` to the groups catalog  
**And** when not permitted, codes remain plain text.

### BDD-SYS-NORM-W1-015 — Constitution documents fluid-everywhere for management

**Given** Wave 1 layout decision  
**When** `ux-entity-display-constitution` and `frontend-entity-display` skill are read  
**Then** they state management pages are fluid by default / system-wide  
**And** `contained` is opt-in exception only.

### BDD-SYS-NORM-W1-016 — English-first i18n for any new user-facing strings

**Given** new UI copy introduced by this wave  
**When** locales are checked  
**Then** English keys exist in `en.ts` first; zh-CN mirrors as needed  
**And** no hardcoded user-facing strings appear in changed Vue surfaces.

## Out of scope

Hub Properties / External ops pages / test artifact BE / role matrix rewrite /
D1 runtime API kill / promotion pack (later waves).

## Explicitly deferred (with evidence)

| ID | Item | Disposition |
| --- | --- | --- |
| **N18** | EntityLink — Legal hold actor | **Deferred** from Wave 1 — not implemented in `sys-norm-shell-fluid-nav`; remains queued for a later SYS-NORM wave (Wave 2+ / N* sweep) with dedicated BDD before code. Wave 1 Done **does not** claim N18. |
