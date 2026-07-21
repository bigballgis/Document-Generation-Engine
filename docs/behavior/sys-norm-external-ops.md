---
id: DOC-BEHAVIOR-SYS-NORM-EXTERNAL-OPS
type: Behavior Spec
status: Confirmed
readiness: ready
program: SYS-NORM
wave: 3
slice: sys-norm-external-ops
taskMaster: 147
related:
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/behavior/sys-norm-hub-ia.md
  - docs/behavior/api-ops-discoverability.md
  - docs/behavior/api-package-access-and-invocation-records.md
  - docs/adr/api-management/0040-api-package-access-and-invocation-retention.md
  - docs/product/catalog-navigation-ux.md
---

# SYS-NORM Wave 3 — External services ops (dashboard + invocations + package settings)

> **TM:** Task Master **#147** · slice `sys-norm-external-ops` → **Done** (MAIN merge
> `18a9e3b2` / feature `f21dda5e`; sole-active **cleared**).  
> **Locks:** Program charter [system-normalization-program.md](./system-normalization-program.md)
> §2.3; plan [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md);
> Wave 2 shell [sys-norm-hub-ia.md](./sys-norm-hub-ia.md) **do not reopen**.  
> **Policy SoT:** [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md)
> package-first (settings route is the edit surface; `/api/policies` remains monitoring/dashboard).

```
bdd_readiness: ready
frontend_ui_in_scope: true
open_questions: []
owning_doc: docs/behavior/sys-norm-external-ops.md
task_ids: ["147"]
queue_slice_id: sys-norm-external-ops
scenario_ids:
  - BDD-SYS-NORM-W3-001 … BDD-SYS-NORM-W3-018
scenario_count: 18
```

---

## 1. Actor / role

| Actor | Role |
| --- | --- |
| Group / global admin with `canManageApiPolicy` | Open External services **dashboard**; browse **invocation records**; open/edit **package API settings**; follow hub / alert deep-links |
| Entitled template reader (no `canManageApiPolicy`) | Must **not** see External services edit/ops nav items that require the capability; fail-closed if deep-linked |
| Hub operator (author / tester with package access) | Jump from Template hub **API settings** / version API perspective into package settings (Wave 2); Wave 3 completes the destination |

Group isolation remains fail-closed (`GroupAccessService` / existing matrix). Management invocation
surfaces show **summaries only** (no `variables` plaintext) — ADR-0040 / C6 / C15.

## 2. User goal

Operators manage External services as a coherent group:

1. **Dashboard** — cross-package ops overview: readiness/alerts (existing semantics) **plus**
   performance, failure-rate, and artifacts **summaries** (honest counts / rates derived from
   in-scope data — **no invented SLOs or NFR thresholds**).
2. **Invocation records** — a **separate page** (not only an embedded package panel) for
   cross-package list + filters + detail (management summary).
3. **Package API settings** — the **single edit surface** at
   `/api/packages/:templateId/settings` (Wave 2 shell complete: remove interim-only framing;
   host former hub External access L1/advanced panels).
4. **Nav** — External services group lists dashboard + invocations; package settings reachable
   by deep-link / hub jump (not a third catalog).

## 3. Trigger

- Open External services nav → dashboard (`/api/policies` or successor path kept as overview).
- Open External services nav → **Invocation records**.
- Hub header **API settings** / per-version API deep-link / alert row → package settings.
- Legacy redirects already shipped in Wave 2 (`?tab=apiAccess`, `#apiAccess`,
  `/api/policies/:templateId`) — Wave 3 **keeps** them landing on the completed settings surface.
- Filters / row open on invocations page → detail (drawer or route — OA pattern).

## 4. Preconditions

- Waves **0–2 Done** on integration line (Wave 2 merge `5d77db80` / feature `992f6822`).
- Canonical settings shell path exists: `/api/packages/:templateId/settings` (optional
  `?panel=` / `?releaseVersion=`).
- Package-level `api_policy` SoT unchanged (ADR-0040); **forbidden** per-version ApiPolicy.
- Existing per-template management invocation APIs
  (`GET /templates/{templateId}/api/invocations` [+ detail/export]) and Overview readiness /
  alerts APIs are available for composition.
- Actor authenticated with management JWT; `canManageApiPolicy` for ops/edit surfaces.
- Wave 4 test-artifacts / Wave 5 roles / Wave 6 D1 runtime **not folded**.

## 5. Primary journey

1. Operator opens **External services → Overview/Dashboard** → sees readiness summary cards +
   alerts (AOD / SCEN-ALERT semantics retained) **and** performance / failure / artifacts
   summary blocks for authorized scope.
2. Operator opens **External services → Invocation records** → sees a **cross-package**
   paginated list with filters; opens a row → management **detail** (summary fields; no
   variable plaintext).
3. From dashboard alert / hub API settings / version deep-link, operator lands on
   `/api/packages/:templateId/settings` → edits L1 access (AD Group, default route, retention,
   credentials) + advanced (collapsed) on that **single** surface; saves via existing
   impact-preview / domain save journeys.
4. Operator without capability cannot reach edit/ops; GROUP_ADMIN never sees other groups’
   packages, alerts, summary counts, or invocations.

### 5.1 System responses (success)

| Surface | Response |
| --- | --- |
| Dashboard (`/api/policies` evolved) | Readiness cards + alerts **plus** performance / failure-rate / artifacts **summaries**; still **not** a paginated template catalog (SCEN-ALERT-04 / AOD-C5) |
| Invocation records page | Separate route under External services; cross-package list + filters + detail |
| Package settings | Complete edit home (no “interim / under construction” as the primary story); hosts former External access panels; Wave 2 redirects still resolve here |
| Nav | External services group includes dashboard item + invocations item; icons satisfy Wave 1 nav-icon contract |
| Deep-links | Hub / alerts / legacy → settings; settings may deep-link to invocations filtered by package when useful |

### 5.2 Boundary / exception / fail-closed

- **Forbidden:** second standalone API policy **catalog** or restoring hub External access tab.
- **Forbidden:** per-version ApiPolicy entities (model B).
- **Forbidden:** inventing numeric SLOs / latency budgets / error-budget NFRs in this wave.
- **Forbidden:** exposing invocation `variables` / full parameters on management UI (C6).
- **Forbidden:** claiming Wave 4 testing artifact downloads Done; claiming program Done; flipping
  go-live checklist **#3b** / **#5a**.
- No `canManageApiPolicy` → nav items hidden / route guard fail-closed; no edit.
- Cross-group → empty counts / empty lists / `403 ACCESS_DENIED` (existing).
- Load failures → honest error + retry (not silent blank).
- Empty scope / zero invocations → honest empty states (not fake sample metrics).

### 5.3 Implementation preference (non-product fork)

| Preference | Rule |
| --- | --- |
| Compose first | Prefer composing existing per-template invocation + readiness/alert APIs on the FE |
| BE aggregation | Add a management cross-package aggregation endpoint **only** if composition cannot
  deliver honest pagination/filters/group scope without incorrect UX |
| File-cap peel | If leaf goes red on file-cap: keep **invocations page + settings completion** preferred
  over dashboard polish; dashboard MVP (extend existing overview) still in-leaf when possible |
| Batch | `solo` — do **not** fold Wave 4 |

### 5.4 Locked routes / IA (Wave 3)

| Surface | Path / placement |
| --- | --- |
| External services dashboard | `/api/policies` (evolve `ApiPolicyHomeView`; L1 title may stay/refresh English-first
  “External services overview” / dashboard wording) |
| Invocation records | New management route under External services group (suggested
  `/api/invocations` — implementer may align with `ROUTE_KEYS` conventions) |
| Package API settings | `/api/packages/:templateId/settings` (+ `?panel=` / `?releaseVersion=`) |
| Package settings in nav | **Deep-link only** (not a third sidebar catalog of all packages) |

---

## 6. Acceptance scenarios

### BDD-SYS-NORM-W3-001 — External services dashboard shows ops summaries

**Given** an operator with `canManageApiPolicy` and at least one in-scope package  
**When** the operator opens the External services dashboard (`/api/policies`)  
**Then** the page shows the existing readiness summary cards (published-in-scope / attention /
pending-release needing setup) **and** summary blocks for **performance**, **failure rate**,
and **artifacts** derived from authorized-scope data  
**And** the page remains monitoring/dashboard (alerts + summaries + deep-links) — **not** a
second paginated template catalog  
**And** no invented SLO threshold labels (e.g. hard-coded p95 budgets) are required for Done.

### BDD-SYS-NORM-W3-002 — Dashboard retains alerts + deep-link to package settings

**Given** in-scope alerts exist (e.g. `MISSING_AD_GROUP`, credential alerts per AOD / SCEN-ALERT)  
**When** the operator opens the dashboard and activates an alert row  
**Then** the app navigates to `/api/packages/:templateId/settings` (not hub `?tab=apiAccess`)  
**And** GROUP_ADMIN sees only in-scope alerts.

### BDD-SYS-NORM-W3-003 — Dashboard honest empty / error

**Given** the operator has `canManageApiPolicy` but zero in-scope packages **or** summary APIs fail  
**When** the dashboard loads  
**Then** empty scope shows an **honest empty** state (not fabricated metrics)  
**And** load failure shows **LoadError** (or equivalent) with retry  
**And** the UI does not silently show blank cards as if healthy.

### BDD-SYS-NORM-W3-004 — Invocation records is a separate page

**Given** an operator with `canManageApiPolicy`  
**When** the operator opens **Invocation records** from the External services nav group  
**Then** the app lands on a dedicated invocations page (distinct from the dashboard route and
from package settings)  
**And** the page is fluid (Wave 1 regression).

### BDD-SYS-NORM-W3-005 — Cross-package invocation list + filters

**Given** in-scope packages have management-visible invocation summaries  
**When** the operator views the invocations page  
**Then** the list can include invocations across authorized packages (not only a single
hard-coded template)  
**And** filters are available for at least: status/outcome, time range, and package identity
and/or `requestId` (reuse existing filter fields where present)  
**And** results respect group scope (no cross-group rows).

### BDD-SYS-NORM-W3-006 — Invocation detail (management summary)

**Given** a visible invocation row on the invocations page  
**When** the operator opens detail  
**Then** the UI shows management summary fields (identity, route/release, outcome, timing,
artifact availability indicators as available)  
**And** it does **not** display full template `variables` / parameter plaintext  
**And** compliance deep-dive remains Activity log / audit — not this page as a second audit console.

### BDD-SYS-NORM-W3-007 — Invocations honest empty / error

**Given** authorized scope has no invocations matching filters **or** the list API fails  
**When** the invocations page renders  
**Then** zero matches → honest empty  
**And** failure → error + retry (not silent blank table).

### BDD-SYS-NORM-W3-008 — Package settings is the single edit surface (complete)

**Given** a template in `PENDING_RELEASE` or `PUBLISHED` and `canManageApiPolicy`  
**When** the operator opens `/api/packages/:templateId/settings`  
**Then** the page is the **complete** package API settings home (L1: routes/AD Group/default
route/retention/credentials; advanced collapsed domains per ADR-0040 / package-access BDD)  
**And** interim-only “under construction” framing is removed or demoted so operators can edit
for real  
**And** the hub does **not** host a parallel External access editor tab.

### BDD-SYS-NORM-W3-009 — Settings editable for PENDING_RELEASE and PUBLISHED

**Given** skeleton/`api_policy` exists (C10) and `canManageApiPolicy`  
**When** the operator opens settings for `PENDING_RELEASE` and for `PUBLISHED`  
**Then** policy load/edit paths work for both (AOD P1 semantics mapped to settings shell)  
**And** non-target lifecycles remain non-editable for API policy (existing AOD-C1).

### BDD-SYS-NORM-W3-010 — Settings panel / releaseVersion deep-link

**Given** a deep-link `/api/packages/:templateId/settings?panel=<id>` and/or
`?releaseVersion=<ver>`  
**When** the operator navigates to it  
**Then** the settings page opens for that package  
**And** known panel/domain anchors or release-context chrome are honored when the panel id is
supported  
**And** unknown panel ids fail closed to the default settings home (honest, not crash).

### BDD-SYS-NORM-W3-011 — Hub / legacy redirects land on completed settings

**Given** Wave 2 redirects from hub `?tab=apiAccess` / `#apiAccess` / `/api/policies/:templateId`
and hub header **API settings**  
**When** the operator follows any of them after Wave 3  
**Then** they land on `/api/packages/:templateId/settings` with the completed edit surface  
**And** dual-surface hub editing remains eliminated (N6 closed for settings home).

### BDD-SYS-NORM-W3-012 — External services nav group membership

**Given** a session with `canManageApiPolicy`  
**When** the shell builds the External services (API) nav group  
**Then** the group includes a **dashboard/overview** item and an **invocation records** item  
**And** each item has a mapped nav icon (Wave 1 contract)  
**And** package settings is **not** listed as a third catalog of all packages.

### BDD-SYS-NORM-W3-013 — Nav / route fail-closed without capability

**Given** a session **without** `canManageApiPolicy`  
**When** the shell builds nav **or** the user deep-links dashboard / invocations / settings  
**Then** External services ops items are hidden from nav  
**And** direct URL access is rejected fail-closed (existing route-guard pattern)  
**And** no cross-package invocation or policy edit UI is reachable.

### BDD-SYS-NORM-W3-014 — GROUP_ADMIN group scope on all three surfaces

**Given** `GROUP_ADMIN` authorized only for group `RETAIL`  
**When** dashboard summaries/alerts, invocations list, and package settings for a non-`RETAIL`
package are requested  
**Then** dashboard counts/alerts exclude other groups  
**And** invocations exclude other groups  
**And** settings for out-of-scope packages deny (`403` / existing access denial)  
**And** no cross-group leakage occurs.

### BDD-SYS-NORM-W3-015 — Published vs runtime callable honesty on settings

**Given** settings for a package with empty AD Group (`adGroupsConfigured=false`)  
**When** the operator views the settings surface  
**Then** a visible warning distinguishes lifecycle publishability from **runtime callable**
(AOD P4 mapped to settings — not only buried in machine summary strings).

### BDD-SYS-NORM-W3-016 — Dashboard / invocations link into settings without catalog duplication

**Given** the operator is on the dashboard or invocations page  
**When** they follow a package deep-link  
**Then** they open package settings (or hub when only identity browse is intended)  
**And** neither surface becomes a full paginated Templates catalog clone.

### BDD-SYS-NORM-W3-017 — English-first i18n for Wave 3 surfaces

**Given** new user-facing copy for dashboard ops summaries, invocations page, settings completion,
and nav labels  
**When** locales are checked  
**Then** English keys exist in `en` first; zh-CN mirrors as needed  
**And** no hardcoded user-facing strings appear in changed Vue surfaces.

### BDD-SYS-NORM-W3-018 — Out-of-scope waves not claimed

**Given** Wave 3 delivery evidence  
**When** Done is evaluated  
**Then** Wave 4 testing artifact downloads, Wave 5 role compression, Wave 6 D1 runtime retire,
and SYS-NORM program Done are **not** claimed  
**And** checklist **#3b** / **#5a** remain untouched by this leaf  
**And** CE umbrella **#53** remains registry-only.

---

## 7. Observable evidence

| Evidence | Proves |
| --- | --- |
| Dashboard DOM / Playwright | Readiness + perf/failure/artifacts summaries; alerts; no template catalog |
| Invocations route / nav | Separate page under External services; filters; detail summary-only |
| Settings route | Complete edit panels; interim framing gone; PENDING_RELEASE + PUBLISHED edit |
| Router regression | Hub / legacy redirects → settings |
| Nav catalog + icon contract | Dashboard + invocations items; icons mapped |
| Session / group tests | Capability + GROUP_ADMIN scope fail-closed |
| FE gates + E2E + UIUX + queued Docker | Delivery Done evidence (later stages) |

## 8. Traceability

| Source | Note |
| --- | --- |
| Program §2.3 External services | Locked: separate invocations page; dashboard-like overview; package settings single edit |
| Plan Wave 3 Done criteria | Invocation page + package API settings surface; hub redirects |
| Wave 2 OOS → this wave | Full dashboard / complete settings panels |
| ADR-0040 (+ 2026-07-21 nav amendment) | Package-first; settings shell; `/api/policies` monitoring |
| api-ops-discoverability | Overview/alerts semantics retained; surface evolves |
| api-package-access-and-invocation-records | L1 settings content; management summary (no variables); C10/C15 |
| api-access-cross-package-alerts | SCEN-ALERT-04 non-catalog |
| catalog-navigation-ux.md | External services IA |
| Task Master | **#147** (activation by plan-orchestrator) |

## 9. Out of scope (explicit)

| Item | Owner |
| --- | --- |
| Published/history Testing DOCX/PDF downloads | Wave 4 `sys-norm-test-artifacts` |
| Role compression / matrix rewrite | Wave 5 |
| D1 runtime/management hard retire | Wave 6 |
| Promotion pack / dry-run | Wave 7 |
| Demo seed / L1 Letterhead terminology sweep | Wave 8 |
| Inventing NFR SLOs / error budgets | Forbidden (program §6) |
| Caller-facing runtime invocation API redesign | Already shipped; not this IA wave |
| Restoring hub External access tab | Forbidden |
| Flipping go-live **#3b** / **#5a**; CE-#53 delivery | Forbidden |

## 10. Handoff notes

- Next: `plan-orchestrator` → register/activate TM **#147** if needed; FE(+optional BE) TDD Red
  against **BDD-SYS-NORM-W3-001…018**.
- Prefer compose existing per-template APIs; add BE aggregation only on real gap.
- `on_red_split_hint`: peel dashboard polish vs keep **invocations page + settings completion**.
- Do **not** fold Wave 4; do **not** mark Wave 3 / program Done from this BDD alone.
