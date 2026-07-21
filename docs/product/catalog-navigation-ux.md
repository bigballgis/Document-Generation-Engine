# Catalog navigation UX — packages vs versions

**Status:** Confirmed (2026-06-25)  
**Phase A implementation:** Done (2026-06-25) — hub + revision detail routes, revision-lines API, E2E 4/4.  
**Phase B implementation:** Done (2026-07-01) — BDD-MASTER-REVISION-NAV-001 Phase B; delivered as **P2-T06**.  
**Scope:** Management UI navigation, master/template list and detail pages.  
**Traceability:** BDD-MASTER-REVISION-NAV-001 (master revision two-page split); BDD-TEMPLATE-PACKAGE-NAV-001 (template package hub — **Done, P3-T06 2026-07-01**).

**Package list pagination (LR-C5, 2026-07-11):** Templates / Masters / Content-modules **package lists** use server-side `PageView` pagination + filter/search (default `size=20`, max 100; default sort group-first `groupCode ASC, updatedAt DESC`). Contract: [openapi-v1.yaml](../api/openapi-v1.yaml) `listTemplates` / `listMasters` / `listContentModules`; behavior [lrp-c5-catalog-pagination.md](../behavior/lrp-c5-catalog-pagination.md). Does not change package-hub version/revision-line pagination already specified below. Does **not** define LR-C6 command-palette API.

### SYS-NORM Confirmed intent (2026-07-21) — Waves 0–7 Done; Wave 8 In Progress

> Locked by [system-normalization-program.md](../behavior/system-normalization-program.md) §2.1–2.3 / §2.8–2.9.
> Historical hub-tab narrative below is **superseded for Hub IA** by Wave 2; External services
> dashboard / full settings panels delivered by Wave 3.
> Program: [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md).
> Wave 1 leaf TM **#145** `sys-norm-shell-fluid-nav` → **Done** (`7a62be44`).
> Wave 2 leaf TM **#146** `sys-norm-hub-ia` → **Done** (`5d77db80` / `992f6822`); BDD
> [sys-norm-hub-ia.md](../behavior/sys-norm-hub-ia.md) (**BDD-SYS-NORM-W2-001…018**).
> Wave 3 leaf TM **#147** `sys-norm-external-ops` → **Done** (`18a9e3b2` / `f21dda5e`); BDD
> [sys-norm-external-ops.md](../behavior/sys-norm-external-ops.md) (**BDD-SYS-NORM-W3-001…018**).
> Wave 8 leaf TM **#152** `sys-norm-demo-seed-terms` → **In Progress** (BDD
> [sys-norm-demo-seed-terms.md](../behavior/sys-norm-demo-seed-terms.md) **W8-001…018**).

| Intent | Confirmed decision | Implementation status | Wave |
| --- | --- | --- | --- |
| **Layout** | All management pages are **fluid** (system-wide; supersedes catalog=fluid / detail=contained for management `AppPageLayout`) | **Done** (2026-07-21; `#145` / `7a62be44`) | 1 `sys-norm-shell-fluid-nav` |
| **Security nav trim + D1 nav hide** | Security = Audit + Legal holds; Document brands + Legal entities **not required surfaces** ([ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)); Letterhead (master) owns logo/seal; shell REDBC/GREENBC UI-only | **Done** (nav hide; 2026-07-21) | 1 (nav hide) / 6 (hard retire) |
| **Nav icon contract + Edit/More + EntityLink N1–N3** | Every remaining nav item has an icon; Users/Groups Edit+More shared primitive; task-hub + catalog `groupCode` EntityLink | **Done** (2026-07-21); **N18** Legal-hold actor EntityLink **deferred** | 1 |
| **Hub primary** | Version lines only (fluid) | **Done** (2026-07-21; `#146` / `5d77db80`) | 2 `sys-norm-hub-ia` |
| **Properties** | Hub header control → **right drawer** (content formerly Overview tab) | **Done** (2026-07-21) | 2 |
| **Remove hub tabs** | Overview, Dependencies, External access | **Done** (2026-07-21) | 2 |
| **Dependencies** | Live on **per-version** surfaces (release / dev detail), not package hub tab | **Done** (2026-07-21) | 2 |
| **API model A** | Package-level API settings SoT under External services; hub **API settings** jump to `/api/packages/:templateId/settings` shell; per-version perspective + deep-link; **forbidden** per-version ApiPolicy entities; legacy `?tab=apiAccess` / `#apiAccess` / `/api/policies/:templateId` → settings shell | **Done** IA shell + redirects (Wave 2) + full settings home (Wave 3; `#147` / `18a9e3b2`) | 2 (+ 3 settings) |
| **External services** | Invocation records = **separate page** (dashboard-like); package API settings = single edit surface | **Done** (2026-07-21; `#147` / `18a9e3b2` / `f21dda5e`; [sys-norm-external-ops.md](../behavior/sys-norm-external-ops.md)) | 3 `sys-norm-external-ops` |
| **D1 brands/entities runtime** | Full product-surface + runtime retirement per ADR-0071 — hard-retire routes/APIs/catalogs; Letterhead (master) SoT for logo/seal; Legal holds kept; no brand/entity sidecar for promotion/export ([sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) **D1-001…020**) | **Done** (TM **#150** `64b0a650`; BDD **ready/Done**) | 6 `sys-norm-d1-brands` |
| **L1 Letterhead / 母版 + honest empties** | L1 EN **Letterhead** / ZH **母版**; purge L1 Master mix; Asset Library / Legal hold / revision design / journey honest empties; optional demo seed ops; N23 classpath ≠ library | **In Progress** (docs locked; FE/BE impl pending) — **N18 deferred**; parked UX OOS | 8 `sys-norm-demo-seed-terms` |

### Wave 8 empty-state product notes (N13 / N15 / N21)

| Surface | Confirmed UX | BDD |
| --- | --- | --- |
| Legal holds catalog (zero rows) | Honest empty + Create CTA iff manage capability | W8-005 / W8-006 (N13) |
| Letterhead revision **design** tab (empty summary) | Honest empty (reason + next step), not silent card body | W8-012 (N15) |
| Role journey timeline (no current step / empty work set) | Visible `*.empty.guidance` (or equivalent); forbid silent blank chrome | W8-013 / W8-014 (N21) |

## Design principle

The management shell exposes **Letterhead templates** (route/domain: masters) and **Templates**
as top-level catalog entries—not “master versions” or “template versions” as L1 menu labels.
API/L3 may still say `masters` / `masterId`.

Navigation follows a package-first mental model (masters add a third level — revision line detail):

```text
Masters / Templates (menu)
  └── Package list (one row per logical master or template package, keyed by name)
        └── Package hub (master or template package detail)
              └── Version / revision lines (status, last updated, last actor, …)
                    ├── Master: revision line detail (anchors, review history)
                    └── Template: dev editor (in-flight) OR release read-only (published)
```

| Layer | User sees | Backend reality (v1) |
| --- | --- | --- |
| Package list | Master or template **name** (plus group, workflow summary) | One `master_document` or `template` row per package |
| Package hub — version lines | Paginated table of version/revision lines with status and audit fields | **Template (BDD-TEMPLATE-PACKAGE-NAV-001):** paginated `template_version` rows — current in-flight dev line(s) plus all published release lines. **Master (Phase B Done):** full immutable revision-line history per replace — paginated `master_revision_line` rows ordered by recency (see § phased delivery) |
| Revision / version line detail | Master: overview, anchor catalog, review history. Template: dev editor or release read-only | **Master:** keyed by `revisionLineId`. **Template:** dev route keyed by `devVersionId`; release route keyed by `releaseVersion` (read-only + clone) |

## Why this is correct

1. **Matches domain language:** PRD and domain model describe **master documents** and **templates** as managed objects; release versions are an inner concern of templates, not the primary menu identity.
2. **Matches user tasks:** Operators look for “the retail letter template” or “the retail letterhead master”, not an abstract “version catalog” index.
3. **Separates concerns:** The list answers “what packages exist?”; the hub answers “what revision lines exist and what is the package impact?”; revision detail answers “what anchors and review history belong to this line?”
4. **Honest for masters (Phase B):** Masters do not have template-style release versions. The hub shows a paginated revision-line table with full immutable history after each file replace (Phase B Done, P2-T06).

## UI mapping

### Navigation

- Group: **Document content** (`nav.groups.content`)
- Items: **Masters** (`/masters`), **Templates** (`/templates`)

### Master package list

Columns emphasize the **package**: name, workflow status, anchor count, last updated, last updated by.

### Master package hub — `/masters/:masterId`

Package-level surface (BDD-MASTER-REVISION-NAV-001). Row click on a revision line navigates to revision detail.

1. **Package header** — name, group, workflow status
2. **Package actions** — download, replace file, submit/review (when permitted)
3. **Revision lines** — paginated table (full history after replace — status, source file summary, anchor count, revision sequence label, updated at/by)
4. **Impact analysis** — referenced templates summary (unchanged from P2-T04)

Does **not** host the full anchor catalog or review history; those live on the revision detail route.

### Master revision line detail — `/masters/:masterId/revisions/:revisionLineId`

Revision-scoped surface for one DOCX revision line.

1. **Revision overview** — status, source file, updated at/by, link back to package hub
2. **Anchor catalog** — filterable table for this revision line
3. **Review history** — audit trail for this revision line

### Master revision history — phased delivery

| Phase | Scope | API / UI honesty | Status |
| --- | --- | --- | --- |
| **A** | Two-page split (hub + revision detail); paginated revision-lines API | May return **only the current revision line**; UI must not imply multiple historical rows exist when the API does not provide them | **Done (2026-06-25)** — `MasterPackageHubView`, `MasterRevisionDetailView`, `MasterRevisionLineController`, Flyway V22, E2E 4/4 |
| **B** | True multi-revision history — each file replace persisted as a revision row | Full paginated history on hub; revision detail deep-links to any historical line; historical DOCX download | **Done (2026-07-01)** — Flyway V32, `MasterRevisionLineService`, hub/detail UI, E2E **7/7** + UIUX **6** screenshots; **P2-T06 Done** |

Phase B introduces a persisted **master revision line** entity (one row per uploaded/replaced DOCX). Phase A routes and two-page IA are unchanged; only data honesty and historical read paths expand.

#### BDD-MASTER-REVISION-NAV-001 — Phase B (full revision history)

**BDD ID:** `BDD-MASTER-REVISION-NAV-001` (Phase B extension)  
**Task traceability:** P2-T06 (`docs/plan/detail/P2-master-management.md` § P2-T06)  
**Status:** Done (2026-07-01) — delivered P2-T06; gates green  
**Depends on:** Phase A Done (P2-T05) — hub + revision detail routes, `MasterRevisionLineController`, Flyway V22 `current_revision_line_id`

| Field | Specification |
| --- | --- |
| **Actors / roles** | **`MASTER_DESIGNER`** — create master, replace file, list/view/download revision lines within authorized group(s). **`GLOBAL_ADMIN`** — same operations across all groups. Both require `route.master-management` capability. Group scope enforced via `GroupAccessService` (fail-closed). |
| **User goal** | After one or more file replacements, operators see **full immutable revision history** on the package hub, open any historical revision line in detail, and download the DOCX artifact that belonged to that line — without losing Phase A two-page navigation. |
| **Trigger** | (1) Initial master create (first revision row). (2) **Replace file** on package hub (`POST …/masters/{masterId}/file`). (3) Hub revision-lines table row click or direct navigation to `/masters/:masterId/revisions/:revisionLineId`. (4) Download action on revision detail (current or historical). (5) Paginated list fetch on hub load or page change. |
| **Preconditions** | Master exists and is not logically deleted. Actor session is authenticated with management JWT. Actor's authorized groups include the master's `groupCode` (or actor is global). Master is not `PENDING_REVIEW` when replacing file (existing policy). Phase A routes and panels already deployed. |
| **Primary journey** | 1. Actor creates master with initial DOCX → system persists **revision line 1** and sets `master_document.current_revision_line_id`. 2. Actor replaces file on hub → system persists **revision line 2** (new row + new MinIO object key), marks line 2 `current: true`, line 1 `current: false`; prior artifact and anchor snapshot remain on line 1. 3. Hub loads paginated revision-lines table → shows ≥2 rows, current row first (recency order). 4. Actor clicks a **historical** row → navigates to `/masters/:masterId/revisions/:revisionLineId` with that line's overview, anchor catalog snapshot, and review history scoped to that line. 5. Actor downloads DOCX on historical detail → receives the superseded file bytes, not the current file. |
| **System responses (success)** | `GET …/revision-lines` returns `PageView` with `content[]` of `MasterRevisionLineSummaryView` (`id`, `lineLabel`, `status`, `originalFilename`, `anchorCount`, `updatedAt`, `updatedBy`, `current`). `GET …/revision-lines/{revisionLineId}` returns `MasterRevisionLineDetailView` for **any** line belonging to the master. `GET …/revision-lines/{revisionLineId}/download` streams the stored DOCX for that line. Hub UI shows pagination controls when `totalPages > 1`. Historical rows are visually distinct from current (`current: false`; i18n label, not hardcoded English). |
| **Boundary / exception** | **First upload only:** list returns exactly **1** row (`totalElements: 1`) — no fake history. **Replace while `PENDING_REVIEW`:** `403`/`422` per existing master state policy (`api.error.master.invalidState`). **Unknown `revisionLineId`:** `404` (`MASTER_NOT_FOUND` or equivalent — no cross-master leakage). **Cross-group access:** list, get, and download for a master in group B by a user authorized only for group A → **`403 ACCESS_DENIED`** (fail-closed; no empty success). **Empty page:** requesting `page` beyond `totalPages - 1` returns empty `content[]` with correct `totalElements` (no error). **Phase A non-regression:** current-line deep-link, breadcrumbs, and hub package actions remain functional. |
| **Observable evidence** | API: `$.result.content.length() ≥ 2` after two uploads; historical `id ≠ current_revision_line_id`; `$.result.content[?(@.current==true)].length() == 1`; download response `Content-Disposition` filename matches historical `originalFilename`; cross-group calls return `$.error.code == "ACCESS_DENIED"`. UI: hub table row count matches API page; URL contains historical `revisionLineId`; downloaded file hash/size differs from current when files differ. Audit: `metadata.auditId` / `traceId` on envelope responses. E2E: Playwright journeys on Docker `:4173`. |
| **Source docs** | This file § phased delivery; `docs/plan/detail/P2-master-management.md` § P2-T06; `docs/security/permission-matrix.md` §4 (母版), §13 (group isolation); domain: versioned master DOCX + logical delete (permission matrix baseline). |

##### Acceptance scenarios (Given / When / Then)

**S1 — Multi-row history after replace (required)**  
- **Given** a `MASTER_DESIGNER` authorized for group `RETAIL` and a master in `RETAIL` created with `file-a.docx`,  
- **When** the actor replaces the file with `file-b.docx` and calls `GET /api/management/v1/masters/{masterId}/revision-lines?page=0&size=20`,  
- **Then** the response contains **≥ 2** rows; exactly one row has `current: true` with `originalFilename` matching `file-b.docx`; the superseded row has `current: false` with `originalFilename` matching `file-a.docx`; rows are ordered by recency (current first).

**S2 — Historical revision detail deep-link (required)**  
- **Given** a master with at least two revision lines and the superseded line id `historicalLineId`,  
- **When** the actor opens `/masters/{masterId}/revisions/{historicalLineId}` (hub row click or direct URL),  
- **Then** the revision detail page loads with overview for `historicalLineId` (`current: false`); anchor catalog reflects the **historical snapshot**; review history is scoped to that line; breadcrumb links back to package hub; workflow actions that apply only to the **current** line remain disabled or hidden on the historical view.

**S3 — Download historical DOCX (required)**  
- **Given** a superseded revision line whose stored artifact differs from the current line,  
- **When** the actor calls `GET …/revision-lines/{historicalLineId}/download` (API) or clicks download on historical revision detail (UI),  
- **Then** the response is `200` with DOCX content-type and attachment filename equal to the historical line's `originalFilename`; file bytes match the artifact stored for `historicalLineId`, not the current line's storage key.

**S4 — Group isolation fail-closed (required)**  
- **Given** a master in group `RETAIL` and a `MASTER_DESIGNER` (or `GROUP_ADMIN`) authorized **only** for group `CORP`,  
- **When** the actor calls list, get, or download for that master's revision lines,  
- **Then** each call returns **`403`** with unified error envelope `error.code: ACCESS_DENIED`; no revision line payload is returned.

**S5 — Pagination with many revision rows (required)**  
- **Given** a master with **N > size** revision lines (e.g. `N = 25`, `size = 20`) and an authorized actor,  
- **When** the actor requests `page=0&size=20` then `page=1&size=20`,  
- **Then** page 0 returns 20 rows with `totalElements: N`, `totalPages: ceil(N/size)`; page 1 returns the remaining rows; no duplicate ids across pages; hub pagination controls reflect `totalPages` and fetch the correct page on change.

**S6 — First upload single row (boundary)**  
- **Given** a newly created master with only the initial upload,  
- **When** the actor lists revision lines,  
- **Then** `totalElements: 1`, the sole row has `current: true`, and the UI does not imply additional historical rows exist.

**S7 — Global admin cross-group read (actor coverage)**  
- **Given** a `GLOBAL_ADMIN` and a master in any group with multiple revision lines,  
- **When** the admin lists, opens detail for a historical line, and downloads its DOCX,  
- **Then** all three operations succeed with the same semantics as S1–S3 (global scope bypasses group restriction only for authorized global role, not for other roles).

**Scenario count:** 7 (5 required + 2 boundary/actor coverage).

### Template package list

Columns: name, external ID, in-flight workflow status, current release version, release version count, last updated, last updated by.

### Template package hub — `/templates/:templateId`

Package-level surface (BDD-TEMPLATE-PACKAGE-NAV-001). **Default primary surface:** paginated **version lines** table (in-flight dev line + published release lines). Row click navigates by line kind; published lines expose a **Clone** action.

1. **Package header** — name, external ID, group, package-level workflow status
2. **Package actions** — export/import, metadata edit (when permitted)
3. **Version lines** — paginated table: dev version number, release version (when published), lifecycle status, approval sub-state (when `APPROVAL`), updated at/by, **default-route indicator** (published only), **explicit generate path summary** (published only)
4. **Secondary tabs (hub-retained — historical shipped baseline)** — overview, **External access** (primary API configuration surface per BDD-API-PACKAGE-ACCESS-INVOCATION-001), workflow/journey panels as needed

**SYS-NORM Wave 2 target IA (Confirmed; BDD ready — [sys-norm-hub-ia.md](../behavior/sys-norm-hub-ia.md)):**

- Hub primary remains **Version lines** only; **remove** hub tabs Overview, Dependencies, External access.
- Former Overview content → hub header **Properties** → **right drawer**.
- **Dependencies** move to per-version release + development surfaces.
- **API model A:** hub header **API settings** → package settings route shell
  `/api/packages/:templateId/settings` (optional `?panel=` / `?releaseVersion=`); per-version API
  perspective + deep-link; **no** per-version ApiPolicy entities. Full invocation dashboard +
  complete settings = Wave 3 **Done** (`#147` / `18a9e3b2`).
- Legacy `?tab=apiAccess`, `#apiAccess`, and `/api/policies/:templateId` → redirect to settings shell.
- Master hub **N14** parity: Properties drawer + revision-lines primary; no Template External access on Master.

**External access tab (historical target IA, 2026-07-03; C10 / api-ops-discoverability 2026-07-14 — superseded as hub tab by Wave 2; content migrates to package settings route):**

- L1: package `externalId`; default + explicit route summary; AD Group; default route selector (governed change); retention presets (save documents, record/doc days); credentials; read-only recent invocation summary for admins.
- Advanced (collapsed): output formats/modes, batch limits, encryption overrides.
- **No** «API not configured» empty state once skeleton policy exists from `PENDING_RELEASE`.
- **Pre-publish setup (C10):** with `canManageApiPolicy`, settings are editable for **`PENDING_RELEASE` ∪ `PUBLISHED`** (skeleton AD Group / access pre-config before go-live). Other lifecycle statuses stay hub-authoring only — not an API settings editor.
- Standalone sidebar «API management» template catalog **deprecated**. Cross-package home `/api/policies` remains **package-first monitoring only** (ADR-0040): optional readiness **summary cards** + **alerts** table + settings/hub deep links — **not** a second paginated template catalog (see [api-ops-discoverability.md](../behavior/api-ops-discoverability.md), SCEN-ALERT-04).

Does **not** host full authoring (variables, bindings, structured content editor); those live on the dev version route.

### Template dev version editor — `/templates/:templateId/dev/:devVersionId`

Authoring surface for one **in-flight** dev line (`DRAFT`, `TESTING`, `APPROVAL`, `PENDING_RELEASE`).

1. **Dev overview** — status, dev version number, link back to package hub
2. **Authoring** — variables, anchor bindings, composition rules, structured content (existing P3/P18 authoring scope)
3. **Workflow actions** — submit test, lifecycle decisions, publish gate (capability-gated; unchanged lifecycle semantics)

### Template release version detail — `/templates/:templateId/releases/:releaseVersion`

Read-only surface for one **published** (or published-then-`STOPPED`) release line.

1. **Release overview** — release version, dev version number at publish time, lifecycle status, updated at/by, link back to package hub
2. **Read-only binding / variable / rule summary** — published snapshot; no mutation controls
3. **Clone action** — creates a new dev line in `DRAFT` from this release snapshot (see BDD below)

#### BDD-TEMPLATE-PACKAGE-NAV-001 — template package hub navigation

**BDD ID:** `BDD-TEMPLATE-PACKAGE-NAV-001`  
**Status:** Confirmed (2026-07-01) — **Not yet implemented** (replaces monolithic `TemplateDetailView` tab-default `overview`)  
**Parity reference:** BDD-MASTER-REVISION-NAV-001 (hub + detail route split)  
**Replaces / supersedes:** P21-T06a default tab `overview` on `/templates/:templateId`; `TemplateReleaseVersionHistoryPanel` as published-only list without row navigation or clone

| Field | Specification |
| --- | --- |
| **Actors / roles** | **`TEMPLATE_AUTHOR`** — list version lines, open in-flight dev editor, clone published release to new dev line, author within authorized group(s). **`MASTER_DESIGNER`** — same authoring/clone scope as template author (permission matrix §5). **`GLOBAL_ADMIN`** / **`GROUP_ADMIN`** — read hub and release detail across authorized groups; metadata/export where matrix permits; clone when author-capable. **`TEMPLATE_TESTER`** / **`TEMPLATE_APPROVER`** — read hub, open in-flight dev editor in read-only or decision context per existing lifecycle panels (no clone unless also author-capable). All require `route.template-management` capability. Group scope enforced via `GroupAccessService` (fail-closed). |
| **User goal** | Operators land on a template **package hub** that answers “what dev and release lines exist?”, open the in-flight line for authoring, inspect any published line read-only, and **clone** a published line into a new draft dev line — mirroring master hub + revision detail navigation without losing P21 workflow/journey affordances. |
| **Trigger** | (1) Package list row click or direct URL `/templates/:templateId`. (2) Version-lines table row click (in-flight → dev editor; published → release detail). (3) **Clone** on published row or release detail. (4) Breadcrumb / back link from dev or release detail to hub. (5) Paginated list fetch on hub load or page change. |
| **Preconditions** | Template exists and is not logically deleted. Actor session is authenticated with management JWT. Actor's authorized groups include the template's `groupCode` (or actor is global). Monolithic `TemplateDetailView` tabs remain reachable only during migration shim if explicitly flagged — target end state is route split (see Pending). |
| **Primary journey** | 1. Actor opens `/templates/{templateId}` → hub loads paginated version-lines (`GET …/version-lines`) showing the current in-flight dev line (if any) and all published release lines. 2. Actor clicks an **in-flight** row (`DRAFT` / `TESTING` / `APPROVAL` / `PENDING_RELEASE`) → navigates to `/templates/{templateId}/dev/{devVersionId}` with full authoring. 3. Actor clicks a **published** row → navigates to `/templates/{templateId}/releases/{releaseVersion}` read-only. 4. Actor clicks **Clone** on a published line → `POST …/release-versions/{releaseVersion}/clone` creates dev line `N+1` in `DRAFT`, copies published snapshot (bindings, variables, rules, structured content, render profile reference), sets template active dev pointer, returns `201` with new `devVersionId`; UI navigates to dev editor. 5. Actor uses breadcrumb to return to hub; table reflects new dev row. |
| **System responses (success)** | `GET …/templates/{templateId}/version-lines` returns `PageView` with `content[]` of `TemplateVersionLineSummaryView`: `devVersionId`, `devVersionNumber`, `releaseVersion` (null when unpublished), `lifecycleStatus`, `approvalSubState` (when `APPROVAL`), `lineKind` (`IN_FLIGHT` \| `PUBLISHED`), `updatedAt`, `updatedBy`, `defaultRouteTarget` (published only), `cloneable` (true for published lines when actor may author). `GET …/templates/{templateId}/dev/{devVersionId}` returns dev-scoped detail for authoring (extends current `TemplateDetailView` fields scoped to that dev version). `GET …/templates/{templateId}/releases/{releaseVersion}` returns read-only release snapshot. `POST …/templates/{templateId}/release-versions/{releaseVersion}/clone` returns `201` + `TemplateDevVersionCreatedView` (`devVersionId`, `devVersionNumber`, `lifecycleStatus: DRAFT`). Hub UI default view is version-lines table (not overview tab). Published rows: no inline edit; clone button visible when `cloneable`. |
| **Boundary / exception** | **No in-flight line:** hub lists published rows only; `totalElements` equals published count; no fake dev row. **In-flight exists:** exactly one `IN_FLIGHT` row for the template's active dev version (package `devVersionId`); status matches template live lifecycle. **Clone while in-flight dev exists:** `409 TEMPLATE_DEV_LINE_IN_FLIGHT` (`api.error.template.devLineInFlight`) — operator must finish or abandon current dev work before cloning (fail-closed; no silent overwrite). **Mutations on published dev version:** any `PUT`/`PATCH`/`DELETE` authoring or lifecycle transition targeting a published `devVersionId` or published `releaseVersion` content → **`403 TEMPLATE_VERSION_IMMUTABLE`** (`api.error.template.versionImmutable`). **Unknown `devVersionId` / `releaseVersion`:** `404` (`TEMPLATE_NOT_FOUND` or `TEMPLATE_VERSION_NOT_FOUND` — no cross-template leakage). **Cross-group access:** list, get, clone for template in group B by user authorized only for group A → **`403 ACCESS_DENIED`**. **Empty page:** page beyond `totalPages - 1` returns empty `content[]` with correct `totalElements`. **STOPPED release line:** appears in hub and release detail as read-only; clone still permitted when no in-flight dev (creates new DRAFT). **Deprecated template package:** hub read-only; clone and authoring mutations blocked per existing deprecate policy. |
| **Observable evidence** | API: after clone, `devVersionNumber` increments; new row `lifecycleStatus == DRAFT` and `releaseVersion == null`; published source row unchanged; `GET version-lines` includes both published source and new in-flight row. Mutation on published dev returns `$.error.code == "TEMPLATE_VERSION_IMMUTABLE"`. Cross-group list/get/clone returns `$.error.code == "ACCESS_DENIED"`. UI: hub URL `/templates/{templateId}` shows version-lines table first; in-flight click → URL contains `/dev/{devVersionId}`; published click → URL contains `/releases/{releaseVersion}`; clone success → redirect to new dev URL. Audit: clone records `metadata.auditId` / lifecycle audit with source `releaseVersion` and new `devVersionId`. E2E: Playwright journeys on Docker `:4173`. |
| **Source docs** | This file § Template package hub; `docs/domain/domain-model.md` §2.7, §2.10–2.11; `docs/security/permission-matrix.md` §5 (模板), §13 (group isolation); `docs/plan/detail/P3-template-authoring.md`; P21 journey/tab components (hub-retained secondary panels). |

##### Acceptance scenarios (Given / When / Then)

**S1 — Hub lists in-flight dev and published release lines (required)**  
- **Given** a `TEMPLATE_AUTHOR` authorized for group `RETAIL`, a template in `RETAIL` with one published release `1.0.0` (dev version 1) and a current in-flight dev version 2 in `DRAFT`,  
- **When** the actor opens `/templates/{templateId}` and the hub loads `GET /api/management/v1/templates/{templateId}/version-lines?page=0&size=20`,  
- **Then** the response contains **≥ 2** rows: one row with `lineKind: IN_FLIGHT`, `devVersionNumber: 2`, `releaseVersion: null`, `lifecycleStatus: DRAFT`; one row with `lineKind: PUBLISHED`, `devVersionNumber: 1`, `releaseVersion: "1.0.0"`, `lifecycleStatus: PUBLISHED`; rows ordered with in-flight first, then published by recency; hub UI shows the version-lines table as the default primary surface (not overview).

**S2 — Click in-flight dev line opens authoring editor (required)**  
- **Given** a template whose active dev version is `devVersionId` in `TESTING`,  
- **When** the actor clicks the in-flight row on the hub (or navigates directly to `/templates/{templateId}/dev/{devVersionId}`),  
- **Then** the dev editor route loads with authoring panels (variables, bindings, rules); workflow banner reflects `TESTING`; breadcrumb links back to package hub; URL contains `/dev/{devVersionId}`.

**S3 — Click published release line opens read-only detail (required)**  
- **Given** a template with published release `1.0.0` backed by dev version 1,  
- **When** the actor clicks the published row on the hub (or navigates to `/templates/{templateId}/releases/1.0.0`),  
- **Then** the release detail page loads read-only overview (release version, status, updated at/by); binding/variable summaries are display-only (no save/delete controls); breadcrumb links back to package hub; URL contains `/releases/1.0.0`.

**S4 — Clone published release creates new DRAFT dev line (required — new API)**  
- **Given** a template in group `RETAIL` with published release `1.0.0`, package lifecycle `PUBLISHED` (no in-flight dev), and a `TEMPLATE_AUTHOR` authorized for `RETAIL`,  
- **When** the actor invokes `POST /api/management/v1/templates/{templateId}/release-versions/1.0.0/clone` (UI clone action on hub or release detail),  
- **Then** the response is `201` with a new `devVersionId`, `devVersionNumber` equal to prior max + 1, `lifecycleStatus: DRAFT`, `releaseVersion: null`; subsequent `GET …/version-lines` includes the new in-flight row plus unchanged published `1.0.0` row; UI navigates to `/templates/{templateId}/dev/{newDevVersionId}`; audit records clone provenance from `1.0.0`.

**S5 — Published version content is immutable (required)**  
- **Given** a published release `1.0.0` with dev version id `publishedDevId`,  
- **When** an authorized actor attempts to mutate authoring state (e.g. `PUT …/variables/{key}` or `PUT …/bindings/{anchorId}` scoped to `publishedDevId`, or dev editor save targeting the published dev version),  
- **Then** the API returns **`403`** with unified error envelope `error.code: TEMPLATE_VERSION_IMMUTABLE` and `error.messageKey: api.error.template.versionImmutable`; no content change is persisted.

**S6 — Group isolation fail-closed (required)**  
- **Given** a template in group `RETAIL` and a `TEMPLATE_AUTHOR` authorized **only** for group `CORP`,  
- **When** the actor calls version-lines list, dev get, release get, or clone for that template,  
- **Then** each call returns **`403`** with `error.code: ACCESS_DENIED`; no version line payload is returned.

**S7 — Clone blocked while in-flight dev exists (boundary)**  
- **Given** a template with an active in-flight dev version in `DRAFT` and a published release `1.0.0`,  
- **When** the actor attempts `POST …/release-versions/1.0.0/clone`,  
- **Then** the API returns **`409`** with `error.code: TEMPLATE_DEV_LINE_IN_FLIGHT`; no new dev version is created; published row unchanged.

**S8 — Global admin cross-group read and clone (actor coverage)**  
- **Given** a `GLOBAL_ADMIN` and a template in any group with published release `2.0.0` and no in-flight dev,  
- **When** the admin lists version lines, opens release detail, and clones `2.0.0`,  
- **Then** all three operations succeed with the same semantics as S1, S3, and S4 (global scope bypasses group restriction only for authorized global role).

**Scenario count:** 8 (6 required + 2 boundary/actor coverage).

## Terminology (en)

| Avoid in nav / list titles | Prefer |
| --- | --- |
| Master versions | Masters |
| Template versions | Templates |
| Version catalog | Masters / Templates (under Document content) |
| Version status (on package list) | Workflow status |

## Hybrid IA — resource-typed + behavior-typed navigation (P21)

**Status:** Confirmed design (2026-06-29) | **Implementation:** **Done** (2026-06-30) — delivered under
[P21](../plan/detail/P21-role-journey-frontend-redesign.md) (T01/T01a behavior nav + task hub; X02 governance close). Decision:
[behavior-typed IA + business terminology](../adr/decisions/2026-06-29-behavior-typed-ia-business-terminology.md)
(extends Batch B / COR-T11; single task hub stays authoritative).

The package-first catalog above remains the **resource-typed** navigation. P21 adds a
**behavior-typed** group so non-IT users can find work by "what is waiting on me", while the
single `/dashboard` task hub remains the one authoritative work entry (behavior entries are
**filtered views of the task hub**, not standalone workbench pages).

```text
Left navigation IA
├── Resource-typed (business functions)
│     Users & permissions · Letterhead templates (Masters) · Templates ·
│     External services (API management) · Activity log (Audit)
└── Behavior-typed (my to-dos) — filtered task-hub views
      Waiting on my testing · Waiting on my approval · Waiting on my fixes ·
      Waiting to confirm go-live · Masters to review · Overdue to follow up
```

- Behavior-typed entries are capability/queue-driven and role-aware (visibility per
  [permission matrix §13.1.2](../security/permission-matrix.md)); no-permission entries are hidden.
- Labels follow the [business terminology guide](./business-terminology-guide.md) (L1 business
  language; no `policy`/`credential`/`lifecycle`/`gate` as primary labels). i18n keys stay stable;
  only message values change.
- Each role also gets a guided `RoleJourneyTimeline` (current step, available actions, waiting
  items) reachable from the task hub.

### Navigation terminology (business-friendly, P21)

| Avoid on L1 (current) | Prefer (en) | zh-CN |
| --- | --- | --- |
| API policy / API access | API management / External services | API 管理 / 对外服务 |
| Access & identity | Users & permissions | 用户与权限 |
| Audit log / console | Activity log | 操作记录 |
| Lifecycle (tab) | Workflow status / Approval progress | 流转进度 |

Full mapping: [business-terminology-guide.md](./business-terminology-guide.md).

## Related docs

- `docs/domain/domain-model.md` §2.5 (master), §2.10–2.11 (template versions)
- `docs/product/business-terminology-guide.md` (business-friendly terminology SSOT)
- `docs/plan/detail/P2-master-management.md`
- `docs/plan/detail/P3-template-authoring.md` (BDD-TEMPLATE-PACKAGE-NAV-001 implementation target)
- `docs/plan/detail/P16-lifecycle-version-governance.md`
- `docs/plan/detail/P21-role-journey-frontend-redesign.md`
- `docs/adr/decisions/2026-06-29-behavior-typed-ia-business-terminology.md`

## Pending (out of scope for Phase A / Phase B spec)

- Master anchor catalog versioning aligned with OpenAPI `anchor-catalogs` admin contract
- URL rename (`/masters` / `/templates` retained for stability)

### BDD-TEMPLATE-PACKAGE-NAV-001 — pending questions (not confirmed)

- **Hub secondary IA during migration:** Whether overview / workflow / external-access remain as hub tabs, hub secondary panels only, or gain dedicated routes — implementation may ship a temporary redirect from legacy `?tab=` query params to new routes; default landing remains version-lines table once hub ships.
- **Concurrent in-flight dev lines:** v1 confirmed model is **one active in-flight dev line** per template (package `devVersionId`); historical dev versions that were published remain as immutable published rows only — no second parallel in-flight line without completing or abandoning current dev (clone blocked per S7).
- **Abandon / discard in-flight dev:** No new “delete dev line” API in this slice; returning to published-only state may require a future lifecycle action (out of scope here). **Superseded 2026-07-03** — implemented; see BDD-TEMPLATE-VERSION-LINE-ACTIONS below.

## BDD-TEMPLATE-VERSION-LINE-ACTIONS (confirmed 2026-07-03)

**Status:** Implemented (backend + hub UI)

| Field | Specification |
| --- | --- |
| **Actors** | `TEMPLATE_AUTHOR` (abandon, clone); `GROUP_ADMIN` / `GLOBAL_ADMIN` (release deactivate/restore) |
| **Hub IA** | Version-lines table primary; no journey blocks or workflow banner |
| **Abandon (Option A)** | Logical delete in-flight dev; published releases unchanged |
| **Default route guard** | Cannot deactivate release that is API default route target (`409`) |
- **OpenAPI publication:** `version-lines`, dev get, release get, and `clone` management endpoints added to `openapi-v1.yaml` (P3-T06, 2026-07-01).
