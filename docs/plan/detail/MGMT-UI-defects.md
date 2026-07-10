# MGMT-UI-DEFECTS — Management UI Defect Fixes + Bounded Frontend Audit

**Program / slice family:** `mgmt-ui-defects` (ad-hoc; **not** a formal P-phase)  
**Formal plan phase:** **None** — single-active-phase discipline OK (ad-hoc slice does not occupy a P* slot)  
**Active delivery slice:** `mgmt-ui-p1` — **Round 3 / P1 depth governance** → **Done** (2026-07-10)  
**Prior slice:** `mgmt-ui-defects` Round 2 / P0 → **Done** (2026-07-10; snapshotted on feat as `3a616e8`)  
**Placement (Round 3 closeout):** MAIN — merge `180bffb` (`feat/mgmt-ui-p1` → `main`); feature commit `e70dfd0`; worktree `DGE-mgmt-ui-p1` **REMOVED**  
**BDD:** [mgmt-ui-defects-behavior-spec.md](../../requirements/mgmt-ui-defects-behavior-spec.md) — Round 2 v1.1.0 **ready**; Round 3 / P1 → **v1.2.0 ready** (P1-1..P1-3 Done; P1-4 Deferred / non-goal; OQ-P1-3-1 → **Option B**)

---

## Purpose

Fix confirmed Docker acceptance-stack management UI defects, apply bounded frontend audit on touched surfaces, then deepen governance UX (P1) after Round 2 P0.

### Round 2 / P0 (Done)

| ID | Area | Owner | Status |
| --- | --- | --- | --- |
| D1 | Shell `shell-page-root` flex chain + `AppPageLayout` `contentSurface=panel` white workspace | frontend | **Done** (R2) |
| D2 | Release detail `TemplateDetail` API + real basics/testing/approval (batch history + audit timeline) | frontend | **Done** (R2) |
| D3 | API policy home alerts-first IA (removed duplicate published-packages catalog) + backend alerts fix | backend + frontend | **Done** (R2) |
| D4 | Login username validation false positive | frontend | **Done** |
| A1 | Bounded audit on touched surfaces | frontend | **Done** |

### Round 3 / P1 depth governance (Done)

| ID | Area | Owner | Status |
| --- | --- | --- | --- |
| P1-1 | Login password trim (mirror username trim; no false required errors from edge whitespace) | frontend | **Done** |
| P1-2 | GroupManagementPanel — unified `LoadErrorPanel` + retry on list/load failure | frontend | **Done** |
| P1-3 | Release Approval — read-only publish-gate checklist (`PublishGateReadOnlyPanel`; real API only) | frontend + backend | **Done** — Option B: `GET .../releases/{rv}/publish-gate` |
| P1-4 | API policy home empty/error UX polish — only if gaps remain after R2; else confirmed non-goal | frontend | **Deferred** — confirmed non-goal (R2 D3/alerts-first already closed gaps) |
| P1-5 | Residual E2E journeys for D1–D4 (+ P1 scenarios) on Docker `:4173` | e2e | **Done** — `mgmt-ui-p1-residual.spec.ts` **12/12**; UIUX **PASS** @1920 |

---

## Exit criteria

### Round 2 / P0 (met)

| # | Criterion | Evidence |
| --- | --- | --- |
| 1 | BDD-MGMT-UI D1–D4 acceptance on Docker `:4173` | Manual browser verification 2026-07-10 |
| 2 | Frontend gates green | `pnpm -C frontend lint` / `type-check` / `test` / `build` — **GREEN** (2026-07-10) |
| 3 | Backend gates for touched apimgmt | `ApiAccessAlertQueryServiceTest` + `ApiAccessControllerTest` — **9/9** (2026-07-10) |
| 4 | Docker deploy | `docker-deploy-queue.ps1` — **GREEN** (2026-07-10) |

### Round 3 / P1 (met)

| # | Criterion | Evidence |
| --- | --- | --- |
| 1 | BDD-MGMT-UI P1-1..P1-3 readiness **ready** (v1.2.0); P1-4 out of scope | P1-4 **Deferred** (confirmed non-goal); OQ-P1-3-1 → **Option B** |
| 2 | P1-1..P1-3 durable behavior + regression tests | **Done** — feature commit `e70dfd0`; merge `180bffb` |
| 3 | Frontend + backend gates green | **GREEN** — `pnpm -C frontend lint` / `type-check` / `test` / `build`; `mvn -B -ntp -f backend/pom.xml verify` (worktree) |
| 4 | E2E residual D1–D4 + P1 journeys (P1-5) | **Done** — `mgmt-ui-p1-residual.spec.ts` **12/12** PASS |
| 5 | Queued Docker deploy + UIUX + architecture | `docker-deploy-queue.ps1` **GREEN** (:8080/:4173); UIUX **PASS** @1920 (Critical cleared after Option B); architecture-reviewer **PASS** |

---

## Task breakdown

### Round 2 / P0 tasks (Done — retained)

| ID | Task | Status |
| --- | --- | --- |
| 9.1 | Backend: fix `GET /api/management/v1/api-policies/alerts` HTTP 500 | **Done** |
| 9.2 | Backend: SESSION_EXPIRED on fresh login | **N/A** — wrong path `/auth/login`; correct `/api/management/v1/auth/login` works |
| 9.3 | Frontend: shell-page-root + AppPageLayout panel surface (D1 R2) | **Done** |
| 9.4 | Frontend: release TemplateDetail + batch history + audit timeline (D2 R2) | **Done** |
| 9.5 | Frontend: ApiPolicyHome alerts-first IA (D3 R2) + login validation + A1 | **Done** |

### Round 3 / P1 tasks

| ID | Task | Status |
| --- | --- | --- |
| P1-1 | Frontend: trim password edges on login submit; align validators with username trim | **Done** |
| P1-2 | Frontend: GroupManagementPanel load failures → `LoadErrorPanel` + retry | **Done** |
| P1-3 | Frontend + backend: Approval tab publish-gate read-only checklist via release-scoped `GET .../releases/{rv}/publish-gate` (Option B) | **Done** |
| P1-4 | Frontend: API policy home empty/error polish **or** document confirmed deferral | **Deferred** — confirmed non-goal (R2 gaps closed) |
| P1-5 | E2E: residual D1–D4 + P1 acceptance journeys on queued Docker stack | **Done** |

---

## Key files

| Module | Files |
| --- | --- |
| Layout | `frontend/src/components/layout/ManagementShell.vue`, `AppPageLayout.vue` |
| Release | `TemplateReleaseDetailView.vue`, `TemplateLifecycleAuditTimeline.vue`, `TemplateDetailOverviewTab.vue`, `PublishGateReadOnlyPanel.vue`, `api/templates.ts` |
| API policy | `frontend/src/views/api/ApiPolicyHomeView.vue` |
| Login | `frontend/src/views/LoginView.vue` |
| Users / groups | `frontend/src/views/identity/UserManagementView.vue`, `GroupManagementPanel.vue` |
| Backend | `ApiAccessAlertQueryService` / `ApiAccessController` (R2); `PublishGateService` + `TemplateVersionLineController` release publish-gate (P1-3 Option B) |
| E2E / UIUX | `frontend/e2e/mgmt-ui-p1-residual.spec.ts`; `frontend/e2e/evidence/mgmt-ui-p1*` |

---

## Placement note (Round 3)

| Field | Value |
| --- | --- |
| slice-id | `mgmt-ui-p1` |
| worktree (delivery) | `D:/working/DGE-mgmt-ui-p1` — **REMOVED** after stage 11 |
| branch | `feat/mgmt-ui-p1` (merged) |
| feature commit | `e70dfd08caefec1602bc9a8192414369f0e215bd` |
| merge SHA | `180bffba8595374893ea6b4d6f7b249bc3841723` (`feat/mgmt-ui-p1` → `main`) |
| R2 continuity | Commit `3a616e8` — Round 2 P0 snapshot |
| Doc-sync / commit | Stage 12 on **MAIN**; user **no-commit** for end gate — plan/ledger/BDD sync only |
| Docker | QUEUE_ONLY — `.\scripts\docker-deploy-queue.ps1` **GREEN** |

---

## Activation log

| Date | Event |
| --- | --- |
| 2026-07-10 | Slice started — parent browser evidence for dashboard, release, policies, users |
| 2026-07-10 | Implementation complete on MAIN; Docker redeployed |
| 2026-07-10 | Round 2 deep fix — layout architecture, release real data, alerts-first policies |
| 2026-07-10 | R2 gates green — targeted vitest + `pnpm build` |
| 2026-07-10 | Round 2 / P0 **Done**; R2 snapshotted onto `feat/mgmt-ui-p1` as `3a616e8` |
| 2026-07-10 | **Round 3 / P1 depth governance activated → In Progress** (`mgmt-ui-p1`; ISOLATED `DGE-mgmt-ui-p1`); P1-1..P1-3 In Progress; P1-4/P1-5 Not Started; BDD v1.2.0 authoring in flight |
| 2026-07-10 | P1-1..P1-3 implementation **Done** (frontend gates **GREEN**); P1-4 **Deferred** / confirmed non-goal; P1-5 E2E **In Progress** |
| 2026-07-10 | P1-5 E2E **12/12** + UIUX **PASS** @1920 + architecture-reviewer **PASS**; Docker queue **GREEN**; merge `180bffb` to MAIN; worktree removed |
| 2026-07-10 | **Round 3 / P1 depth governance → Done** (ad-hoc; formal phase remains **None**); stage 12 doc-sync on MAIN; **no-commit** per user |
