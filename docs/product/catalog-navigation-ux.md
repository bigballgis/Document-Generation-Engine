# Catalog navigation UX — packages vs versions

**Status:** Confirmed (2026-06-25)  
**Phase A implementation:** Done (2026-06-25) — hub + revision detail routes, revision-lines API, E2E 4/4.  
**Phase B implementation:** Done (2026-07-01) — BDD-MASTER-REVISION-NAV-001 Phase B; delivered as **P2-T06**.  
**Scope:** Management UI navigation, master/template list and detail pages.  
**Traceability:** BDD-MASTER-REVISION-NAV-001 (master revision two-page split).

## Design principle

The management shell exposes **Masters** and **Templates** as top-level catalog entries—not “master versions” or “template versions” as menu labels.

Navigation follows a package-first mental model (masters add a third level — revision line detail):

```text
Masters / Templates (menu)
  └── Package list (one row per logical master or template package, keyed by name)
        └── Package hub (master or template package detail)
              └── Version / revision lines (status, last updated, last actor, …)
                    └── Revision line detail (master only — anchors, review history)
```

| Layer | User sees | Backend reality (v1) |
| --- | --- | --- |
| Package list | Master or template **name** (plus group, workflow summary) | One `master_document` or `template` row per package |
| Package hub — version lines | Paginated table of version/revision lines with status and audit fields | **Template:** `template_version` rows with `release_version`. **Master (Phase B Done):** full immutable revision-line history per replace — paginated `master_revision_line` rows ordered by recency (see § phased delivery) |
| Revision line detail (master) | Overview, anchor catalog, review history for one revision line | **Master:** keyed by `revisionLineId`; hub retains package-level actions and impact analysis |

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

### Template package detail

Default tab: **Versions** (release version lines). Other tabs: overview & workflow, authoring, API access.

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
- `docs/plan/detail/P16-lifecycle-version-governance.md`
- `docs/plan/detail/P21-role-journey-frontend-redesign.md`
- `docs/adr/decisions/2026-06-29-behavior-typed-ia-business-terminology.md`

## Pending (out of scope for Phase A / Phase B spec)

- Master anchor catalog versioning aligned with OpenAPI `anchor-catalogs` admin contract
- URL rename (`/masters` / `/templates` retained for stability)
