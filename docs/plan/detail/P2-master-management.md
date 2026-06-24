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

**Exit:** Approved master with stable `anchorId` catalog usable by template authoring.

**Management UI (2026-06-24):** Navigation uses **Masters** / **Templates** package lists; detail shows revision/version lines. See `docs/product/catalog-navigation-ux.md`.

**Evidence:** `MasterDocumentService`, `MasterListView.vue`, `MasterDetailView.vue`,
`MasterListView.test.ts`, Flyway V3/V4, `mvn verify` green.
