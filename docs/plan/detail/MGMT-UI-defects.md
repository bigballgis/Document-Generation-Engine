# MGMT-UI-DEFECTS — Management UI Defect Fixes + Bounded Frontend Audit

**Slice ID:** `mgmt-ui-defects`  
**Slice status:** **Done** (2026-07-10, Round 2 deep fix)  
**Formal plan phase:** **None** (ad-hoc acceptance defect slice; not a P-phase)  
**Placement:** MAIN (`main`)  
**BDD:** [mgmt-ui-defects-behavior-spec.md](../../requirements/mgmt-ui-defects-behavior-spec.md) v1.0.0 — **ready**

---

## Purpose

Fix confirmed Docker acceptance-stack management UI defects and apply a bounded high-impact frontend audit on touched surfaces.

| ID | Area | Owner | Status |
| --- | --- | --- | --- |
| D1 | Shell `shell-page-root` flex chain + `AppPageLayout` `contentSurface=panel` white workspace | frontend | **Done** (R2) |
| D2 | Release detail `TemplateDetail` API + real basics/testing/approval (batch history + audit timeline) | frontend | **Done** (R2) |
| D3 | API policy home alerts-first IA (removed duplicate published-packages catalog) + backend alerts fix | backend + frontend | **Done** (R2) |
| D4 | Login username validation false positive | frontend | **Done** |
| A1 | Bounded audit on touched surfaces | frontend | **Done** |

---

## Exit criteria

| # | Criterion | Evidence |
| --- | --- | --- |
| 1 | BDD-MGMT-UI D1–D4 acceptance on Docker `:4173` | Manual browser verification 2026-07-10 |
| 2 | Frontend gates green | `pnpm -C frontend lint` / `type-check` / `test` / `build` — **GREEN** (2026-07-10) |
| 3 | Backend gates for touched apimgmt | `ApiAccessAlertQueryServiceTest` + `ApiAccessControllerTest` — **9/9** (2026-07-10) |
| 4 | Docker deploy | `docker-deploy-queue.ps1` — **GREEN** (2026-07-10) |

---

## Task breakdown

| ID | Task | Status |
| --- | --- | --- |
| 9.1 | Backend: fix `GET /api/management/v1/api-policies/alerts` HTTP 500 | **Done** |
| 9.2 | Backend: SESSION_EXPIRED on fresh login | **N/A** — wrong path `/auth/login`; correct `/api/management/v1/auth/login` works |
| 9.3 | Frontend: shell-page-root + AppPageLayout panel surface (D1 R2) | **Done** |
| 9.4 | Frontend: release TemplateDetail + batch history + audit timeline (D2 R2) | **Done** |
| 9.5 | Frontend: ApiPolicyHome alerts-first IA (D3 R2) + login validation + A1 | **Done** |

---

## Key files

| Module | Files |
| --- | --- |
| Layout | `frontend/src/components/layout/ManagementShell.vue`, `AppPageLayout.vue` |
| Release | `TemplateReleaseDetailView.vue`, `TemplateLifecycleAuditTimeline.vue`, `TemplateDetailOverviewTab.vue`, `api/templates.ts` |
| API policy | `frontend/src/views/api/ApiPolicyHomeView.vue` |
| Login | `frontend/src/views/LoginView.vue` |
| Users | `frontend/src/views/identity/UserManagementView.vue` |
| Backend | `backend/.../apimgmt/service/ApiAccessAlertQueryService.java`, `web/ApiAccessController.java` |

---

## Activation log

| Date | Event |
| --- | --- |
| 2026-07-10 | Slice started — parent browser evidence for dashboard, release, policies, users |
| 2026-07-10 | Implementation complete on MAIN; Docker redeployed |
| 2026-07-10 | Round 2 deep fix — layout architecture, release real data, alerts-first policies |
| 2026-07-10 | R2 gates green — targeted vitest + `pnpm build` |
