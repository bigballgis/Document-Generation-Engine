# BDD：Management shell sidebar nav icons — Asset Library & Legal Hold

| Field | Value |
| --- | --- |
| **Slice** | `nav-missing-icons` |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-19 |
| **Formal phase** | **None** (do **not** activate IBL-B7 / Wave E; do **not** flip checklist **#3b** / **#5a**) |
| **Task / slice id** | `nav-missing-icons` |
| **Placement** | **ISOLATED** — `D:/working/DGE-nav-missing-icons` · `feat/nav-missing-icons` |
| **`frontend_ui_in_scope`** | **`true`** (shell chrome only) |
| **Parent surfaces** | CE-E02 Asset Library (`/library/assets`); CE-G04 Legal Hold (`/governance/legal-holds`); ManagementShell nav |

---

## 1. Summary

When an authenticated management-UI user can see sidebar entries **Asset Library** (`asset-library`) and/or **Legal Hold** (`legal-holds`), each visible item must render an Element Plus icon via the shell’s existing `getNavIcon(itemId)` → `NAV_ICON_MAP` pattern — the same chrome contract already used by sibling items (`dashboard`, `templates`, `audit`, etc.).

**Bug (confirmed for this leaf):** those two nav item ids are present in `navGroupsCatalog` / visible nav but missing from `NAV_ICON_MAP`, so `ManagementShellNav` skips `<el-icon>` (`v-if="getNavIcon(item.id)"`) and the rows appear icon-less.

**Explicit non-goals**

| Non-goal | Handling |
| --- | --- |
| New nav items, routes, labels, or group structure | Out of scope |
| Permission / `visibleRoutes` / route-key changes | Out of scope — consume existing visibility |
| Changing Asset Library or Legal Hold page behavior | Out of scope (CE-E02 / CE-G04 remain SoT) |
| New icon library or custom SVG assets | Out of scope — Element Plus icons only |
| IBL-B7 / Wave E / checklist #3b / #5a / go-live | **Forbidden** for this leaf |

---

## 2. Actor / goal / trigger

| Field | Value |
| --- | --- |
| **Actor / role** | Authenticated management UI user whose session `visibleRoutes` includes the Asset Library and/or Legal Hold administration route(s) |
| **Goal** | Identify and scan sidebar destinations with the same visual affordance as other OA shell nav rows (label + icon) |
| **Trigger** | Management shell sidebar renders (login → shell mount, or navigation that keeps the shell mounted) |
| **Preconditions** | Valid session; at least one of `asset-library` / `legal-holds` is in the built visible nav groups; bank OA shell chrome is active |

---

## 3. Primary journey

1. User authenticates and lands in the management shell (or already has shell mounted).
2. Sidebar builds visible nav groups from session + `navGroupsCatalog`.
3. For each visible item, shell resolves `getNavIcon(item.id)`.
4. When a mapping exists, `ManagementShellNav` renders `<el-icon>` with the mapped Element Plus component beside the label.
5. User sees **Asset Library** and **Legal Hold** (when visible) with icons consistent with siblings.

---

## 4. System responses (success)

- `getNavIcon('asset-library')` and `getNavIcon('legal-holds')` each return a defined Element Plus icon component.
- Visible rows for those ids render an `<el-icon>` (expanded and collapsed chrome paths that already gate on `getNavIcon`).
- Sibling mapped items continue to resolve as before (no regression of existing `NAV_ICON_MAP` keys).

---

## 5. Acceptance scenarios (Given / When / Then)

### BDD-NAV-ICON-001 — Asset Library icon present when visible

**Given** an authenticated management session for which the sidebar includes nav item id `asset-library` (Asset Library / 资产库)  
**When** the management shell sidebar renders  
**Then** that item shows an Element Plus icon resolved via `getNavIcon('asset-library')` (same pattern as siblings with mapped ids)  
**And** the icon is visible in the nav row chrome (not omitted by a missing `NAV_ICON_MAP` key)

### BDD-NAV-ICON-002 — Legal Hold icon present when visible

**Given** an authenticated management session for which the sidebar includes nav item id `legal-holds` (Legal Hold / 法律冻结)  
**When** the management shell sidebar renders  
**Then** that item shows an Element Plus icon resolved via `getNavIcon('legal-holds')` (same pattern as siblings with mapped ids)  
**And** the icon is visible in the nav row chrome (not omitted by a missing `NAV_ICON_MAP` key)

### BDD-NAV-ICON-003 — Hidden items remain absent (fail-closed visibility unchanged)

**Given** an authenticated management session whose `visibleRoutes` do **not** include Asset Library and/or Legal Hold administration  
**When** the management shell sidebar renders  
**Then** the corresponding nav item(s) are **not** shown  
**And** this leaf does **not** change visibility rules — only icon mapping for items that are already visible

### BDD-NAV-ICON-004 — Sibling icons unchanged

**Given** a session that shows multiple sidebar items that already have `NAV_ICON_MAP` entries (e.g. `templates`, `audit`)  
**When** the management shell sidebar renders after the fix  
**Then** those sibling items still show their icons via `getNavIcon`  
**And** no existing mapped id loses its icon solely due to this leaf

---

## 6. Boundary / exception

| Case | Expected |
| --- | --- |
| Item not in visible nav | No icon requirement (item not rendered) |
| Unknown future nav id without map entry | Remains icon-less until an explicit map entry is added (existing chrome contract; this leaf only closes the two confirmed gaps) |
| Collapsed sidebar | Same `getNavIcon` contract; if chrome shows an icon for mapped siblings when collapsed, these two ids must match that pattern |

---

## 7. Observable evidence

| Evidence | What proves it |
| --- | --- |
| Unit / component | `getNavIcon('asset-library')` and `getNavIcon('legal-holds')` are defined; nav row renders `<el-icon>` when mapped |
| E2E (shell) | Visible Asset Library / Legal Hold nav rows show icon chrome consistent with siblings |
| Manual / UIUX | Sidebar scan: no icon-less gap for those two items when visible |

---

## 8. Traceability

| Artifact | Role |
| --- | --- |
| This doc | Owning behavior SoT for slice `nav-missing-icons` |
| [ce-e02-asset-library.md](./ce-e02-asset-library.md) | Asset Library product surface / route (icon chrome only here) |
| [ce-g04-legal-hold.md](./ce-g04-legal-hold.md) | Legal Hold product surface / route (icon chrome only here) |
| `frontend/src/navigation/navGroupsCatalog.ts` | Nav item ids `asset-library`, `legal-holds` |
| `frontend/src/components/layout/useManagementShell.ts` | `NAV_ICON_MAP` / `getNavIcon` (implementation surface — **not** edited in BDD stage) |
| `frontend/src/components/layout/ManagementShellNav.vue` | Renders icon when `getNavIcon` returns a component |
| P12 shell polish / bank OA sidebar-with-icons | Existing locked chrome pattern this leaf restores for two gaps |

```
bdd_readiness: ready
owning_doc: docs/behavior/nav-missing-icons.md
task_ids: [nav-missing-icons]
open_questions: []
```
