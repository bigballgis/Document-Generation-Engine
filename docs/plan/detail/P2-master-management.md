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

**Exit:** Approved master with stable `anchorId` catalog usable by template authoring.

**Management UI (2026-06-24):** Navigation uses **Masters** / **Templates** package lists; detail shows revision/version lines. See `docs/product/catalog-navigation-ux.md`.

**Revision navigation UX (2026-06-25):** BDD-MASTER-REVISION-NAV-001 splits master package detail into hub + revision detail routes; Phase A API may return only the current revision line. Spec: `docs/product/catalog-navigation-ux.md` § Master package hub / Master revision line detail.

**Phase A implementation (2026-06-25):** Backend — `MasterRevisionLineController` (`GET /api/management/v1/masters/{masterId}/revision-lines`, revision-line detail), `MasterRevisionLineService`, Flyway `V22__master_current_revision_line_id.sql`, `MasterRevisionLineControllerTest`. Frontend — `MasterPackageHubView`, `MasterRevisionDetailView`, paginated `MasterRevisionLinesPanel`; routes in `frontend/src/router/index.ts`; i18n keys; unit tests `MasterPackageHubView.test.ts`, `MasterRevisionDetailView.test.ts`, `MasterRevisionLinesPanel.test.ts`. E2E — `frontend/e2e/master-revision-two-page.spec.ts` **4/4** green; UIUX review pass (minor findings). Gates — `mvn -B -ntp -f backend/pom.xml verify`, `pnpm -C frontend lint` / `type-check` / `test` / `build`.

**Evidence (P2-D01–T04):** `MasterDocumentService`, `MasterListView.vue`, Flyway V3/V4, `mvn verify` green.
