---
id: DOC-BEHAVIOR-SYS-NORM-HUB-IA
type: Behavior Spec
status: Confirmed
readiness: ready
program: SYS-NORM
wave: 2
slice: sys-norm-hub-ia
taskMaster: 146
related:
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/product/catalog-navigation-ux.md
  - docs/behavior/api-package-access-and-invocation-records.md
  - docs/adr/api-management/0040-api-policy-package-first.md
---

# SYS-NORM Wave 2 — Template (+ Master parity) Package Hub IA

> **TM:** Task Master **#146** · slice `sys-norm-hub-ia` → **Done** (MAIN merge `5d77db80` / feature
> `992f6822`; worktree **REMOVED**).  
> **Locks:** Program charter [system-normalization-program.md](./system-normalization-program.md) §2.2,
> §2.9; plan [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md).  
> **Do not reopen** API model A vs B, hub tab removals, or Wave 3/4 scope.

```
bdd_readiness: ready
frontend_ui_in_scope: true
open_questions: []
owning_doc: docs/behavior/sys-norm-hub-ia.md
task_ids: ["146"]
queue_slice_id: sys-norm-hub-ia
scenario_ids:
  - BDD-SYS-NORM-W2-001 … BDD-SYS-NORM-W2-018
scenario_count: 18
```

---

## 1. Actor / role

| Actor | Role |
| --- | --- |
| Template author / document author (group-scoped) | Open Template Package Hub; inspect version lines; open Properties; follow API settings / per-version API deep-links when entitled |
| Group / global admin with `canManageApiPolicy` | Same + edit package API settings on the package settings route (shell may be read/edit stub until Wave 3 fills panels) |
| Master / letterhead designer (group-scoped) | Open Master Package Hub with IA parity (Properties drawer; revision lines primary) |
| Template tester / approver / other entitled readers | Read hub + per-version surfaces; no invent of per-version ApiPolicy |

Group isolation remains fail-closed (`GroupAccessService` / existing matrix).

## 2. User goal

Operators use a **Version-lines-primary** Template Package Hub (fluid) with package metadata in a
**Properties right drawer**, Dependencies on **per-version** surfaces, and **API model A**
(package-level API settings under External services — not hub tabs, not per-version ApiPolicy).
Legacy hub External access / `#apiAccess` dual surfaces redirect. Dev editor shows **honest empty**
or **wrong-surface redirect**. Master hub follows the same IA pattern where applicable (N14).

## 3. Trigger

- Open `/templates/:templateId` (hub) or `/masters/:masterId` (master hub).
- Header **Properties** / **API settings**.
- Legacy deep-links: `?tab=overview|dependencies|apiAccess`, `#apiAccess`, `/api/policies/:templateId`.
- Open release `/templates/:templateId/releases/:releaseVersion` or dev
  `/templates/:templateId/dev/:devVersionId` (Dependencies; locale/status de-dupe; blank whiteboard).
- Per-version row API perspective control / deep-link.

## 4. Preconditions

- Wave 1 fluid layout already delivered (regression: hubs remain fluid).
- Template / master exists; actor authenticated with management JWT and group (or global) scope.
- Package-level `api_policy` SoT unchanged (ADR-0040 / BDD-API-PACKAGE-ACCESS-INVOCATION-001) —
  Wave 2 moves **IA only**, does not invent per-version ApiPolicy entities.
- Wave 3 full External services **invocation dashboard** is **out of scope** (shell + redirect OK).
- Wave 4 test-artifacts **not folded**.

## 5. Primary journey (Template hub)

1. Actor opens Template Package Hub → lands on **Version lines** table only as primary surface
   (fluid).
2. Actor opens **Properties** in hub header → **right drawer** shows former Overview content;
   closes drawer → returns focus to version lines.
3. Actor uses hub header **API settings** → navigates to **package API settings route shell**
   under External services (interim path documented below).
4. Actor opens a published or in-flight version → **Dependencies** available on that per-version
   surface (not a hub tab).
5. On a published version-line row, actor sees **API perspective** summary + deep-link into package
   settings (version context via query, e.g. `?panel=` / `?releaseVersion=` — not a new entity).
6. Legacy `?tab=apiAccess` / `#apiAccess` / old hub External access entry → **redirect** to package
   settings shell (N6); hub no longer hosts Overview / Dependencies / External access tabs.
7. Dev editor wrong surface or empty whiteboard → **redirect** or **honest empty** (no silent hide).

### 5.1 System responses (success)

| Surface | Response |
| --- | --- |
| Hub default | Version lines primary; no Overview / Dependencies / External access hub tabs |
| Properties | Right drawer with former Overview fields; dismissible; does not replace version lines |
| API settings (hub header) | Navigates to package settings route shell |
| Per-version Dependencies | Present on release + development detail workspaces |
| Per-version API perspective | Read-only summary + deep-link; package policy remains SoT |
| Legacy apiAccess | Redirect to package settings shell; dual hub tab removed |
| Dev blank / wrong surface | Explicit empty copy or redirect to correct surface |
| Master hub (N14) | Same Properties + primary revision-lines pattern; no template External access tab invented |

### 5.2 Boundary / exception / fail-closed

- **Forbidden:** per-version ApiPolicy entities / breaking package-level policy SoT (model B).
- **Forbidden:** claiming Wave 3 invocation dashboard Done from this leaf.
- Cross-group hub / settings / version detail → `403 ACCESS_DENIED` (existing).
- Unauthorized API settings → control hidden or fail-closed navigate (existing capability gates).
- Unknown template/master id → existing `404` behavior.
- Wave 2 settings **shell** may show interim empty / “settings home under construction” panels
  when Wave 3 content is not ready — **must not** be a dead hub tab link.

### 5.3 Interim package API settings route (Wave 2 shell)

| Item | Locked for Wave 2 |
| --- | --- |
| Canonical shell path | `/api/packages/:templateId/settings` |
| Optional query | `?panel=<id>` and/or `?releaseVersion=<ver>` for deep-link perspective (Wave 3 may refine panels) |
| Hub header **API settings** | Router push to canonical shell |
| Legacy hub `?tab=apiAccess` / `#apiAccess` | Redirect → canonical shell (same `templateId`) |
| Legacy `/api/policies/:templateId` | Redirect → canonical shell (replace prior redirect-to-hub-tab) |
| Full invocation dashboard / ops cards | **Wave 3** — stub shell must not pretend dashboard completeness |

## 6. Acceptance scenarios

### BDD-SYS-NORM-W2-001 — Hub primary is Version lines only

**Given** an authorized operator and a template package with at least one version line  
**When** the operator opens `/templates/:templateId`  
**Then** the primary workspace is the paginated **Version lines** table  
**And** the hub does **not** render Overview, Dependencies, or External access as hub secondary tabs  
**And** layout remains **fluid** (Wave 1 regression).

### BDD-SYS-NORM-W2-002 — Hub tabs Overview / Dependencies / External access removed

**Given** the Template Package Hub after Wave 2  
**When** the operator inspects hub tab / workspace chrome  
**Then** there is no selectable hub tab named Overview, Dependencies, or External access (or i18n
equivalents)  
**And** `?tab=overview`, `?tab=dependencies`, and `?tab=apiAccess` are not valid hub secondary
surfaces (redirect or ignore-to-version-lines per W2-012 / W2-007).

### BDD-SYS-NORM-W2-003 — Properties opens right drawer with former Overview content

**Given** the Template Package Hub  
**When** the operator activates the header **Properties** control  
**Then** a **right drawer** opens  
**And** it presents the content formerly shown on the hub Overview tab (package metadata fields)  
**And** the Version lines table remains the underlying primary surface.

### BDD-SYS-NORM-W2-004 — Properties drawer closes

**Given** the Properties drawer is open on the Template Package Hub  
**When** the operator dismisses the drawer (close control, Esc, or equivalent OA pattern)  
**Then** the drawer closes  
**And** focus returns to the hub primary surface without navigating away from `/templates/:templateId`.

### BDD-SYS-NORM-W2-005 — Dependencies on published release detail

**Given** a published release line for a template  
**When** the operator opens `/templates/:templateId/releases/:releaseVersion`  
**Then** a **Dependencies** surface/section is available on that per-version page  
**And** Dependencies is not reached via a package-hub tab.

### BDD-SYS-NORM-W2-006 — Dependencies on development (in-flight) detail

**Given** an in-flight dev version for a template  
**When** the operator opens `/templates/:templateId/dev/:devVersionId`  
**Then** a **Dependencies** surface/section is available on that per-version page  
**And** Dependencies is not reached via a package-hub tab.

### BDD-SYS-NORM-W2-007 — Hub API settings jumps to package settings route shell

**Given** an authorized operator on the Template Package Hub  
**When** the operator activates hub header **API settings**  
**Then** the app navigates to `/api/packages/:templateId/settings` (package API settings route shell)  
**And** the destination is under External services / API management IA (not a hub tab)  
**And** if Wave 3 panels are not ready, the shell still renders an honest interim page (not a dead
link and not a silent no-op).

### BDD-SYS-NORM-W2-008 — Per-version API perspective + deep-link (model A)

**Given** a published version line that participates in package API routing  
**When** the operator views that row (or its release detail API summary) on the hub  
**Then** the UI shows a **version API perspective** (e.g. generate path / default-route indicator /
callable or warning summary as already available on version-line data)  
**And** a control deep-links into `/api/packages/:templateId/settings` with version context query
(`releaseVersion` and/or `panel`)  
**And** the system does **not** create or require a per-version ApiPolicy entity.

### BDD-SYS-NORM-W2-009 — Forbidden per-version ApiPolicy

**Given** Wave 2 Hub IA implementation  
**When** delivery is reviewed against API model A  
**Then** there is no new per-version ApiPolicy domain entity, route, or CRUD surface  
**And** package-level `api_policy` remains the configuration SoT.

### BDD-SYS-NORM-W2-010 — Legacy hub External access / `?tab=apiAccess` redirect (N6)

**Given** a bookmark or in-app link to `/templates/:templateId?tab=apiAccess` (or hub External access)  
**When** the operator navigates to it after Wave 2  
**Then** the app redirects to `/api/packages/:templateId/settings`  
**And** the hub External access tab is gone.

### BDD-SYS-NORM-W2-011 — Legacy `#apiAccess` / `/api/policies/:templateId` redirect (N6)

**Given** a legacy `#apiAccess` hash deep-link on the template hub **or** `/api/policies/:templateId`  
**When** the operator navigates to it after Wave 2  
**Then** the app redirects to `/api/packages/:templateId/settings` (preserving template id; hash/domain
panel may map to `?panel=` when known)  
**And** dual-surface editing on the hub tab is eliminated.

### BDD-SYS-NORM-W2-012 — Legacy Overview / Dependencies hub query cleanup

**Given** legacy links `?tab=overview` or `?tab=dependencies` on `/templates/:templateId`  
**When** the operator navigates to them after Wave 2  
**Then** Overview query opens **Properties** drawer (or equivalent one-shot reveal) without restoring
an Overview tab  
**And** Dependencies query navigates to the appropriate per-version Dependencies surface when a
version context exists, or lands on Version lines with honest guidance when it does not  
**And** neither restores removed hub tabs.

### BDD-SYS-NORM-W2-013 — Dev editor wrong-surface redirect + honest empty

**Given** an operator opens a template **dev** editor route that is the wrong surface for the current
line state **or** the whiteboard/authoring canvas has no content to show  
**When** the page resolves  
**Then** the system either **redirects** to the correct surface (hub / release / valid dev id) **or**
shows an **honest empty** state with explicit copy  
**And** it does **not** silently hide the empty/wrong condition behind a blank whiteboard with no
explanation.

### BDD-SYS-NORM-W2-014 — Locale metadata de-dupe on Properties / overview content (N4)

**Given** a template with locale / locale-variant family metadata  
**When** the operator views Properties drawer content (former Overview)  
**Then** locale and locale-variant information is presented **once** in a coherent block  
**And** redundant duplicate locale/variant rows or panels that restated the same facts are removed.

### BDD-SYS-NORM-W2-015 — Release status / lifecycle de-dupe (N5)

**Given** a published (or stopped) release detail page  
**When** the operator views status / lifecycle chrome  
**Then** lifecycle status is shown in **one** primary status treatment  
**And** redundant second status/lifecycle badges or summary lines that duplicated the same state are
removed.

### BDD-SYS-NORM-W2-016 — Master hub IA parity (N14)

**Given** an authorized operator opens `/masters/:masterId`  
**When** the Master Package Hub renders  
**Then** the primary surface remains **revision lines** (fluid)  
**And** package-level metadata that would otherwise clutter the hub uses a header **Properties**
right drawer (same interaction pattern as Template hub)  
**And** the hub does **not** invent Template-style External access / API settings tabs on Master  
**And** Impact analysis (package-level) may remain as an existing Master affordance — not reclassified
as a removed Template hub tab.

### BDD-SYS-NORM-W2-017 — N7–N9 hub/header redundancy as fits

**Given** Template hub header and Properties content after Wave 2  
**When** the operator views `groupCode`, package identity links, and External ID  
**Then** `groupCode` in Properties uses EntityLink when identity admin is visible (plain text when not)  
**And** hub header identity fields are not redundantly restated as a second competing External ID /
group block in the same viewport  
**And** no duplicate “External ID” column/field pair remains in Properties + header for the same value
without a single clear primary.

### BDD-SYS-NORM-W2-018 — English-first i18n for new Hub IA strings

**Given** new user-facing copy introduced by Wave 2 (Properties, API settings, honest empty, redirects)  
**When** locales are checked  
**Then** English keys exist in `en` first; zh-CN mirrors as needed  
**And** no hardcoded user-facing strings appear in changed Vue surfaces.

---

## 7. Observable evidence

| Evidence | Proves |
| --- | --- |
| Hub DOM / Playwright | No Overview/Dependencies/External access hub tabs; Version lines visible |
| Properties control | Right drawer open/close; former overview fields present |
| Router | `/api/packages/:templateId/settings` reached from hub API settings + legacy redirects |
| Per-version pages | Dependencies section on release + dev |
| Version-line row | API perspective + deep-link query; no per-version ApiPolicy API |
| Dev editor | Redirect or honest empty test id / copy |
| Properties / release detail | Single locale block; single status treatment |
| Master hub | Properties drawer + revision lines primary |
| FE gates + E2E + UIUX + queued Docker | Delivery Done evidence (later stages) |

## 8. Traceability

| Source | Note |
| --- | --- |
| Program §2.2 Hub IA + API model A | Locked product facts |
| Program §2.9 Dev blank whiteboard | Locked |
| N4, N5, N6, N7–N9, N14 | Plan backlog → this wave |
| BDD-TEMPLATE-PACKAGE-NAV-001 | Version-lines primary baseline (amended by this wave for tabs/drawer) |
| BDD-API-PACKAGE-ACCESS-INVOCATION-001 | Package policy SoT; IA migrates off hub tab |
| catalog-navigation-ux.md | Product hub IA intent table |
| Task Master | **#146** |

## 9. Out of scope (explicit)

| Item | Owner |
| --- | --- |
| Full External services invocation dashboard / ops overview | Wave 3 `sys-norm-external-ops` |
| Completing every settings panel beyond shell + redirects | Wave 3 |
| Published/history Testing artifact downloads | Wave 4 |
| Role compression / matrix | Wave 5 |
| D1 runtime retirement | Wave 6 |
| N15 master revision empty design summary (unless touched incidentally) | Wave 2 peel / Wave 8 |
| N18 Legal-hold actor EntityLink | Still deferred (Wave 1 closeout) |

## 10. Handoff notes

- Next: `plan-orchestrator` → FE TDD Red against **BDD-SYS-NORM-W2-001…018**.
- Prefer peeling **N14 Master** only if file-cap forces split (`on_red_split_hint`).
- Update terminology guide “Package hub API tab / External access” row when implementation lands
  (Wave 2/3) — intent already Confirmed here; do not mark Pending as Confirmed beyond §2.
