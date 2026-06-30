# P2 — Master Document Management (Detailed Plan)

**Phase status:** Done | **Depends on:** P1

## Behavior goal

Master designers upload DOCX masters, maintain anchor catalog, submit for review;
admins approve/reject. Only approved masters can be referenced by new templates.
Group isolation enforced.

## Key tasks

| ID | Task | Status |
| --- | --- | --- |
| P2-D01 | Master entity, DOCX storage (MinIO), anchor extraction model | Done |
| P2-D02 | Master review states: DRAFT, PENDING_REVIEW, APPROVED, REJECTED | Done |
| P2-T01 | Upload/create master + anchor integrity check on submit | Done |
| P2-T02 | Master review workflow + audit | Done |
| P2-T03 | Master list/detail UI (English i18n, group-scoped) | Done |
| P2-T04 | Impact analysis summary on master change (referenced templates list) | Done |
| P2-T05 | Master revision navigation UX — hub (`/masters/:masterId`) vs revision detail (`/masters/:masterId/revisions/:revisionLineId`); Phase A honest single-line pagination; Phase B full history pending | Done |
| P2-T06 | Phase B full master revision history — each file replace persisted as revision row; full paginated history on hub; revision detail deep-links to any historical line; download historical DOCX | Done |

> Single-active-slice invariant: **No formal phase `In Progress`** — P2 phase remains **Done**;
> **P2-T06** slice closed (2026-07-01). Traceability: `docs/product/catalog-navigation-ux.md`
> § Master revision history — phased delivery **Phase B Done**.

**Exit:** Approved master with stable `anchorId` catalog usable by template authoring.

**Management UI (2026-06-24):** Navigation uses **Masters** / **Templates** package lists; detail shows revision/version lines. See `docs/product/catalog-navigation-ux.md`.

**Revision navigation UX (2026-06-25):** BDD-MASTER-REVISION-NAV-001 splits master package detail into hub + revision detail routes; Phase A API may return only the current revision line. Spec: `docs/product/catalog-navigation-ux.md` § Master package hub / Master revision line detail.

**Phase A implementation (2026-06-25):** Backend — `MasterRevisionLineController` (`GET /api/management/v1/masters/{masterId}/revision-lines`, revision-line detail), `MasterRevisionLineService`, Flyway `V22__master_current_revision_line_id.sql`, `MasterRevisionLineControllerTest`. Frontend — `MasterPackageHubView`, `MasterRevisionDetailView`, paginated `MasterRevisionLinesPanel`; routes in `frontend/src/router/index.ts`; i18n keys; unit tests `MasterPackageHubView.test.ts`, `MasterRevisionDetailView.test.ts`, `MasterRevisionLinesPanel.test.ts`. E2E — `frontend/e2e/master-revision-two-page.spec.ts` **4/4** green; UIUX review pass (minor findings). Gates — `mvn -B -ntp -f backend/pom.xml verify`, `pnpm -C frontend lint` / `type-check` / `test` / `build`.

**Evidence (P2-D01–T04):** `MasterDocumentService`, `MasterListView.vue`, Flyway V3/V4, `mvn verify` green.

### P2-T06 — Phase B full master revision history

**Origin:** `docs/product/catalog-navigation-ux.md` § Master revision history — phased delivery
**Phase B**; BDD-MASTER-REVISION-NAV-001 Phase A closed under P2-T05 (2026-06-25).

**Phase A baseline (Done — do not regress):** `MasterRevisionLineService` returns only the
current line from `master_document.current_revision_line_id` (Flyway V22); `replaceFile`
overwrites in place — no historical rows; hub UI honest about single-line pagination.

**Depends on:** P2-T05 (hub + revision detail routes, `MasterRevisionLineController`,
`MasterPackageHubView`, `MasterRevisionDetailView`, E2E `master-revision-two-page.spec.ts` 4/4).

**Scope (confirmed 2026-07-01):**

1. **Backend (TDD):** Persist each master file replace as a new revision row (immutable history);
   advance `current_revision_line_id` on replace; paginated `GET .../revision-lines` returns full
   history ordered by recency; revision-line detail and download endpoints serve any historical
   line by `revisionLineId`; MinIO/object keys retain per-revision artifacts.
2. **Frontend (TDD + E2E):** Hub revision-lines table shows full paginated history (not
   single-line honesty mode); row click deep-links to `/masters/:masterId/revisions/:revisionLineId`
   for any historical line; revision detail supports download of historical DOCX; i18n en first.
3. **BDD:** Extend BDD-MASTER-REVISION-NAV-001 Phase B — actor (master designer), trigger
   (replace file twice), preconditions (approved or draft master per policy), acceptance
   Given/When/Then for multi-row hub pagination, historical deep-link, historical download;
   boundary — first upload (single row), empty page edge, unauthorized group scope.
4. **Gates:** Backend `mvn verify`; frontend lint/type-check/test/build; Playwright functional
   + UIUX evidence for multi-revision hub/history/download journeys (Docker 4173).

**Exit criteria (slice):**

- Each `replaceFile` creates a persisted revision row; prior rows remain queryable (not overwritten).
- Hub lists full paginated revision history; UI no longer implies single-line-only when multiple rows exist.
- Revision detail route resolves any historical `revisionLineId`; historical DOCX download works.
- Phase A routes and hub/revision two-page IA unchanged; E2E Phase A scenarios remain green or updated with Phase B assertions.
- Green gates + E2E/UIUX evidence recorded in ledger on close.
- P2 phase status unchanged (**Done**); `catalog-navigation-ux.md` Phase B row → **Done** on slice close.

**Status:** **Done** (2026-07-01).

**Implementation evidence (2026-07-01):**

- **Backend:** Flyway `V32__master_revision_line.sql` — `master_revision_line` + `master_revision_line_anchor` tables; Phase A backfill as revision line 1; `MasterRevisionLineEntity`, `MasterRevisionLineAnchorEntity`, `MasterRevisionLineRepository`; `MasterRevisionLineService` — append row on create/replace, paginated list, detail/get/download any line; `MasterDocumentService` — `revisionSequence` monotonic advance, live `status` for current line / `status_snapshot` for historical; architecture review M1/M2 remediated.
- **Frontend:** `MasterRevisionLinesPanel` full paginated multi-row hub; `MasterRevisionDetailView` historical read-only detail + download; `masterRevisionLineLabel.ts` — `revisionSequence` i18n label (en + zh-CN); unit tests `MasterRevisionLinesPanel.test.ts`, `MasterRevisionDetailView.test.ts`, `masterRevisionLineLabel.test.ts`, `MasterPackageHubView.test.ts`.
- **E2E:** `frontend/e2e/master-revision-two-page.spec.ts` **7/7** (Phase B multi-row, historical deep-link, download); `frontend/e2e/P2-T06-uiux-evidence.spec.ts` — **6** screenshots @ 1440×900 (REDBC/GREENBC + zh-CN); evidence `frontend/e2e/evidence/P2-T06/screenshots/`.
- **Gates:** backend `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (**564** Surefire); frontend `pnpm -C frontend lint` / `type-check` / `test` (**528** Vitest) / `build` green.
