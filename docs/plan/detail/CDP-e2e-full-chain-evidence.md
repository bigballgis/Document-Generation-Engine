# CDP Wave CD-E2E — Full-Chain Browser Evidence

**Program:** [competitiveness-deepening-program.md](../competitiveness-deepening-program.md)  
**Wave:** CD-2 — **In Progress** (2026-07-11; **partial** — T01/T01b + T02/T03/T04 + T05 + T06 + T07 + T08 **Done**; T09–T12 + T13 remain Not Started; no sole-active CDP E2E slice)
**Owner default:** `e2e-test-engineer` + `e2e-uiux-reviewer`
**Prerequisites:** Docker stack (`.\scripts\docker-deploy-queue.ps1`); read `frontend/e2e/helpers/auth.ts`, `frontend/e2e/helpers/cdp-mvp-golden-api.ts`

> **Completion note (2026-07-11):** Slice `cdp-e2e-t07-api-policy` merged to `main` (`1eb230b`; worktree **REMOVED**). **CD-E2E-T07 → Done**. API policy edit → impact preview → save + hard-block finding UI closed (BDD-CDP-APIPOL-001/002). Playwright docker `--workers=1`: functional **2 passed** (`CDP-E2E-T07-api-policy-edit-save.spec.ts`); UIUX Verdict **PASS** (`CDP-E2E-T07-uiux-manifest.md` + **9** screenshots); architecture-reviewer **PASS_WITH_SUGGESTIONS** / **APPROVE** (no Critical); frontend lint/type-check/test/build **PASS** (955 tests); Stage 5/10 healthz **200** + `:4173` **200** (final deploy from MAIN `@ 1eb230b`). Do **not** mark wave CD-2 **Done**. Formal phase remains **None**. **Next recommend:** **T09** (preview comparison) or **T10** / **T13** (package materialize). **Task Master #19 → done**.

> **Activation note (2026-07-10):** Slice `cdp-e2e-t07-api-policy` — **CD-E2E-T07 → In Progress** (API policy edit → impact preview → save; browser evidence). Placement: ISOLATED `D:/working/DGE-cdp-e2e-t07-api-policy` · `feat/cdp-e2e-t07-api-policy` · base `71ee0b8` (T08 merge tip `c62b1a1` on main). BDD **ready**: `docs/behavior/api-policy-edit-save-journey.md` (BDD-CDP-APIPOL-001/002). Superseded by completion note above. **Task Master #19 → in-progress** (now done).

> **Completion note (2026-07-10):** Slice `cdp-e2e-t08-preview` merged to `main` (`c62b1a1`; feature `ee93c58`; worktree **REMOVED**). **CD-E2E-T08 → Done**. Preview success + artifact download browser journey closed (BDD-CDP-PREV-001/002/003). Product fixes: `previewId` alignment + relative SSE `streamUrl`; helper import fix. Playwright docker `:4173` `--workers=1`: **3 passed** (T08 functional + UIUX); UIUX Verdict **PASS** (3 screenshots @1920 REDBC; P12 T13 success-frame gap closed); architecture-reviewer **PASS_WITH_SUGGESTIONS** (no Critical; merge allowed); Stage 5/10 **DEPLOY_OK** healthz **200** + `:4173` **200** (compose `dge-lrp-c9-load-error-panel`; queue idle); `AsyncPreviewOrchestratorTest` **5/5**. Do **not** mark wave CD-2 **Done**. Formal phase remains **None**. Superseded scheduling: T07 completed (see completion note above). **Task Master #18 → done**.

> **Activation note (2026-07-10):** Slice `cdp-e2e-t08-preview` — **CD-E2E-T08 → In Progress** (Preview success + artifact download UI; closes T13 preview-success manifest gap). Placement: ISOLATED `D:/working/DGE-cdp-e2e-t08-preview` · `feat/cdp-e2e-t08-preview` · base `e0d8bef` (T06 docs tip; upstream merge `3aed175`). BDD **ready**: `docs/behavior/preview-success-artifact-download-journey.md` (BDD-CDP-PREV). Superseded by completion note above. **Task Master #18 → in-progress** (now done).

> **Completion note (2026-07-10):** Slice `cdp-e2e-t06-master` merged to `main` (`3aed175`; feature `3aed175`; worktree **REMOVED**). **CD-E2E-T06 → Done**. Master designer upload → anchor → submit → GROUP_ADMIN approve browser journey closed (BDD-CDP-MASTER-001). Product fix: `MASTER_DESIGNER` `manageMasters` authz aligned to matrix §4 (`GroupAccessService` + `roles.ts` + permission-matrix §13.2). Playwright docker `--workers=1`: **2 passed** (functional 1 + UIUX evidence 1); UIUX Verdict **PASS** (6 screenshots @1920; REDBC+GREENBC); architecture-reviewer **PASS_WITH_SUGGESTIONS** (no Critical; merge allowed); Stage 5/10 deploy healthz/4173 **200** (compose `dge-lrp-c9-load-error-panel`; queue idle); `pnpm lint` + `type-check` PASS; backend GroupAccess/ManagementCapabilities **22**; `roles.test` **20**. Do **not** mark wave CD-2 **Done**. Formal phase remains **None**. Superseded scheduling: T08 completed (see completion note above). **Task Master #17 → done**.

> **Activation note (2026-07-10):** Slice `cdp-e2e-t06-master` — **CD-E2E-T06 → In Progress** (Master designer upload → anchor check → submit review → approver approve). Placement: ISOLATED `D:/working/DGE-cdp-e2e-t06-master` · `feat/cdp-e2e-t06-master` · base `f67cf61` (includes T05 docs tip; merge `895f16e` on main). BDD **ready**: `docs/behavior/master-designer-lifecycle.md` (BDD-CDP-MASTER-001). Superseded by completion note above. **Task Master #17 → in-progress** (now done).

> **Completion note (2026-07-10):** Slice `cdp-e2e-t05-publish` merged to `main` (`895f16e`; feature `a5e5491`; worktree **REMOVED**). **CD-E2E-T05 → Done**. Team lead publish / go-live browser journey closed (BDD-CDP-PUB-001/002). Playwright docker `--workers=1`: **3 passed** (functional 2 + UIUX evidence 1); UIUX Verdict **PASS** (4 screenshots @1920; REDBC+GREENBC queue + go-live dialog + External access); architecture-reviewer **PASS_WITH_SUGGESTIONS** (no Critical; DRY helper / PUB-002 checkbox note); Stage 5/10 deploy healthz/4173 **200** (compose `dge-lrp-c9-load-error-panel`; queue idle). No T08 fallback. Do **not** mark wave CD-2 **Done**. Formal phase remains **None**. Superseded scheduling: T06 completed (see completion note above).

> **Activation note (2026-07-10):** Slice `cdp-e2e-t05-publish` — **CD-E2E-T05 → In Progress** (Team lead publish / go-live). Placement: ISOLATED `D:/working/DGE-cdp-e2e-t05-publish` · `feat/cdp-e2e-t05-publish` · base `aa41b89` / upstream merges `6821f45`. Superseded by completion note above.

> **Completion note (2026-07-10):** Slice `cdp-e2e-cd2-t02` merged to `main` (`6821f45`; feature `13e8e98`; worktree removed). **CD-E2E-T02** + **T03** + **T04 → Done**. Playwright docker `--workers=1`: functional **3 passed** + UIUX evidence **3 passed**; manifests Verdict **PASS** (T02:3 / T03:1 / T04:3 shots); architecture **PASS_WITH_SUGGESTIONS** (no Critical); deploy healthz/4173 OK (rebuild skipped, E2E-only). Do **not** mark wave CD-2 **Done**. Formal phase remains **None**. Superseded scheduling: T05 activated (see activation note above).

> **Activation note (2026-07-10):** Slice `cdp-e2e-cd2-t02` — bounded batch **CD-E2E-T02** (Tester pass) → **In Progress**; **T03** (Tester fail) + **T04** (Approver approve) = this-slice scope. Superseded by completion note above.

> **Completion note (2026-07-10):** Slice `cdp-e2e-golden` merged to `main` (`1930842`; feature `6d27c57` / `5e1c3cd`; worktree removed). **CD-E2E-T01** + **T01b Done**. Full T02–T12 matrix + T13 deferred that slice. Do **not** mark wave CD-2 **Done**. Formal phase remains **None**.

---

## 0. Problem statement

Current E2E inventory (2026-07-04 audit):

- **51** Playwright specs; **17/18** UIUX manifests PASS.
- **No** single browser spec completes MVP vertical slice.
- `demo-full-lifecycle.spec.ts` stages 2–5 use **API helpers** (`ensureDemoFullFlowAtStage`) — invalid for «user can operate the product» Done claims.
- P21 journey specs assert **timeline/IA visibility**, not **task completion** (Pass test, Approve, Publish clicks).

**CD-2 goal:** Every competitiveness pillar has **browser task-completion evidence**, not only API or visibility evidence.

---

## 1. Environment contract (all CD-E2E tasks)

```powershell
# From repo root — implementer MUST run before claiming Done
.\scripts\docker-deploy.ps1
# UI: http://localhost:4173  Backend: http://localhost:8080/healthz
pnpm -C frontend exec playwright test --config=frontend/playwright.docker.config.ts <spec>
```

**Seed users:** `frontend/e2e/helpers/auth.ts` (`10000001`–`10000008`).

**Do NOT:** Use API helpers to skip UI steps in golden-path specs (helpers OK for setup/teardown only).

---

## 2. Task breakdown

### CD-E2E-T01 — MVP golden path (browser-only lifecycle)

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/mvp-golden-path-browser.md` (CD-BDD-T01), `demo-full-lifecycle.spec.ts`, `P12-AUD-B10-submit-approval-gate.spec.ts`
- **Do NOT:** Modify backend unless test reveals contract bug (escalate).
- **Steps:**
  1. Create `frontend/e2e/CDP-E2E-T01-mvp-golden-path.spec.ts`.
  2. Use dedicated seed template via CD-E2E-T01b fixture helper (`prepareCdpMvpGoldenDraft` — idempotent DRAFT clone; not Flyway).
  3. Browser flow: GLOBAL_ADMIN login → (or role handoff) master approve → author configure minimal binding → test tab SSE preview success → Pass test form → Submit for approval → Approver Approve → GROUP_ADMIN publish confirm → API policy save one domain → runtime generate via UI-exposed path OR contract page trigger documented in spec.
  4. Assert queue items appear/disappear on Dashboard after each decision.
  5. Pair UIUX manifest `frontend/e2e/evidence/CDP-E2E-T01-uiux-manifest.md` (≥8 screenshots, REDBC + GREENBC).
- **Acceptance:**
  - **G** Docker stack ready **W** spec runs **T** ≥1 test green end-to-end without API stage advancement for test/approve/publish.
  - **G** author submits failing gate **T** submit blocked with visible checklist (reuse AUD-B10 patterns).
- **Gates:** Playwright green; `e2e-uiux-reviewer` PASS.
- **Status:** **Done** (2026-07-10 — slice `cdp-e2e-golden`; merge `1930842`)
- **Evidence:** `CDP-E2E-T01-mvp-golden-path.spec.ts` Docker **1 passed / 0 failed / 0 skipped** (:4173); UIUX manifest **Verdict PASS** (15 screenshots @1920, REDBC+GREENBC); architecture-reviewer **PASS** (suggestions only; Design panel read-only for decision roles deferred); production fixes `routeCapabilities.ts`, `useTemplateDetailVisibility.ts`; deploy `docker-deploy-queue.ps1` COMPOSE_PROJECT_NAME=`documentgenerationengine` healthz/4173 OK after routeCapabilities rebuild.

### CD-E2E-T01b — Golden path seed fixture

- **Owner:** `e2e-test-engineer` (API fixture helper; not backend Flyway)
- **Depends on:** CD-BDD-T01 draft
- **Steps:** Provide idempotent DRAFT clone for golden-path specs (setup/teardown only — not lifecycle advancement inside test body).
- **Acceptance:** Idempotent fixture yields DRAFT; documented in spec; callable from T01 / UIUX evidence specs.
- **Delivered as:** `frontend/e2e/helpers/cdp-mvp-golden-api.ts` → `prepareCdpMvpGoldenDraft` (idempotent DRAFT clone) — **not** a Flyway/DemoCatalogSeeder seeder.
- **Status:** **Done** (2026-07-10 — verified with T01 Docker green)

### CD-E2E-T02 — Tester structured pass journey

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/tester-decision-journey.md`, `P21-T05-tester-journey.spec.ts`
- **Artifacts:** `CDP-E2E-T02-tester-pass-decision.spec.ts` + `CDP-E2E-T02-*-uiux-evidence` + UIUX manifest
- **Acceptance:** UI clicks Pass → structured form fields → confirm → work item leaves TEST queue.
- **Status:** **Done** (2026-07-10 — slice `cdp-e2e-cd2-t02`; merge `6821f45`)
- **Evidence:** Playwright docker functional PASS; UIUX manifest Verdict **PASS** (3 screenshots); architecture PASS_WITH_SUGGESTIONS (no Critical)

### CD-E2E-T03 — Tester structured fail journey

- **Owner:** `e2e-test-engineer`
- **Acceptance:** Fail path requires reason category + returns template to fix state; collaboration todo visible to author.
- **Status:** **Done** (2026-07-10 — slice `cdp-e2e-cd2-t02`; merge `6821f45`)
- **Artifacts:** `CDP-E2E-T03-tester-fail-decision.spec.ts` + UIUX evidence + manifest
- **Read first:** `docs/behavior/tester-decision-journey.md`
- **Evidence:** Playwright docker functional PASS; UIUX manifest Verdict **PASS** (1 screenshot)

### CD-E2E-T04 — Approver approve journey

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/approver-decision-journey.md`
- **Acceptance:** Approve with evidence confirmation + risk summary viewed; extends P12-AUD-B10 (submit side only today).
- **Status:** **Done** (2026-07-10 — slice `cdp-e2e-cd2-t02`; merge `6821f45`)
- **Artifacts:** `CDP-E2E-T04-approver-approve-decision.spec.ts` + UIUX evidence + manifest
- **Evidence:** Playwright docker functional PASS; UIUX manifest Verdict **PASS** (3 screenshots)

### CD-E2E-T05 — Team lead publish / go-live

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/team-lead-publish-journey.md` (BDD-CDP-PUB-001/002 **ready**)
- **Acceptance:** PENDING_RELEASE → publish summary dialog → confirm → release version callable (UI indicator).
- **Status:** **Done** (2026-07-10 — slice `cdp-e2e-t05-publish`; merge `895f16e`; feature `a5e5491`; worktree removed)
- **Artifacts:** `CDP-E2E-T05-team-lead-publish.spec.ts` + `CDP-E2E-T05-uiux-evidence.spec.ts` + `frontend/e2e/evidence/CDP-E2E-T05-uiux-manifest.md`
- **Evidence:** Playwright docker `--workers=1` **3 passed** (functional 2 + UIUX 1); UIUX Verdict **PASS** (4 screenshots @1920); architecture **PASS_WITH_SUGGESTIONS** (no Critical); deploy healthz/4173 **200**; no T08 fallback

### CD-E2E-T06 — Master designer upload-to-approve

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/master-designer-lifecycle.md` (BDD-CDP-MASTER-001 **ready**), `master-replace-docx.spec.ts`
- **Acceptance:** Upload (or fixture) → anchor check UI → submit review → approver approves in browser.
- **Status:** **Done** (2026-07-10 — slice `cdp-e2e-t06-master`; merge `3aed175`; feature `3aed175`; worktree removed)
- **Artifacts:** `CDP-E2E-T06-master-lifecycle.spec.ts` + `CDP-E2E-T06-uiux-evidence.spec.ts` + `frontend/e2e/evidence/CDP-E2E-T06-uiux-manifest.md` + `frontend/e2e/evidence/CDP-E2E-T06/screenshots/` (6)
- **Evidence:** Playwright docker `--workers=1` **2 passed** (functional 1 + UIUX 1); UIUX Verdict **PASS** (6 screenshots @1920 REDBC+GREENBC); architecture **PASS_WITH_SUGGESTIONS** (no Critical); Stage 5/10 deploy healthz/4173 **200**; product fix `MASTER_DESIGNER` `manageMasters` (matrix §4 / §13.2)

### CD-E2E-T07 — API policy edit → impact preview → save

- **Owner:** `e2e-test-engineer` (+ `frontend-engineer` if product gaps)
- **Read first:** `docs/behavior/api-policy-edit-save-journey.md` (BDD-CDP-APIPOL **ready**), `demo-full-lifecycle.spec.ts` stages 6–9, existing `CDP-E2E-T07-api-policy-edit-save.spec.ts`
- **Acceptance:** One config domain (e.g. output format) edited, impact preview shown, save confirmed, audit entry link visible.
- **Status:** **Done** (2026-07-11 — slice `cdp-e2e-t07-api-policy`; merge `1eb230b`; worktree removed)
- **Artifacts:** `CDP-E2E-T07-api-policy-edit-save.spec.ts` + `CDP-E2E-T07-uiux-evidence.spec.ts` + `frontend/e2e/evidence/CDP-E2E-T07-uiux-manifest.md` + `frontend/e2e/evidence/CDP-E2E-T07/screenshots/` (9)
- **Evidence:** Playwright docker `--workers=1` functional **2 passed**; UIUX Verdict **PASS** (9 screenshots); architecture **PASS_WITH_SUGGESTIONS** / **APPROVE** (no Critical); frontend lint/type-check/test/build **PASS** (955 tests); Stage 5/10 healthz/4173 **200** (MAIN `@ 1eb230b`)

### CD-E2E-T08 — Preview success + artifact download UI

- **Owner:** `e2e-test-engineer`
- **Fixes:** T13 manifest gap «preview success frame not captured» (closed); T13 package materialize remains Not Started
- **Read first:** `docs/behavior/preview-success-artifact-download-journey.md` (BDD-CDP-PREV **ready**)
- **Acceptance:** Test tab shows preview complete; download link works; screenshot in manifest.
- **Status:** **Done** (2026-07-10 — slice `cdp-e2e-t08-preview`; merge `c62b1a1`; feature `ee93c58`; worktree removed)
- **Artifacts:** `CDP-E2E-T08-*.spec.ts` + `frontend/e2e/evidence/CDP-E2E-T08-uiux-manifest.md` + screenshots (3 @1920 REDBC)
- **Evidence:** Playwright docker `:4173` `--workers=1` **3/3**; UIUX Verdict **PASS**; architecture **PASS_WITH_SUGGESTIONS** (no Critical); Stage 5/10 deploy healthz/4173 **200**; `AsyncPreviewOrchestratorTest` **5/5**; product fixes `previewId` + relative SSE `streamUrl`

### CD-E2E-T09 — Preview vs final comparison panel

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/preview-comparison-journey.md`, PRD/usability-review comparison requirements
- **Acceptance:** Side-by-side view opens; filter by warningCode; screenshot evidence.
- **Status:** Not Started

### CD-E2E-T10 — Fidelity «viewed» confirmation on pass/approve/publish

- **Owner:** `e2e-test-engineer`
- **Acceptance:** Cannot pass test / approve / publish without acknowledging fidelity summary (checkbox or equivalent).
- **Status:** Not Started

### CD-E2E-T11 — Audit admin query smoke

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/audit-admin-query-journey.md`, `P21-T11-audit-journey.spec.ts`
- **Acceptance:** Filter by date/event type; export button triggers download; UIUX manifest (first for audit role).
- **Status:** Not Started

### CD-E2E-T12 — zh-CN + dual-brand golden screenshots

- **Owner:** `e2e-uiux-reviewer` + `e2e-test-engineer`
- **Acceptance:** Extend T01 manifest with zh-CN locale switch on ≥3 key screens; REDBC/GREENBC both captured.
- **Status:** Not Started

---

## 3. Deferred from P12 (include in CD-2)

| Source | Scenario | CD-E2E task |
| --- | --- | --- |
| P12-API BDD S1–S3, S7 | Package materialize on publish, default route stability | CD-E2E-T13 (Not Started — preview-success frame gap closed by T08; package materialize still open) |

---

## 4. Anti-patterns (reject in code review)

| Anti-pattern | Why rejected |
| --- | --- |
| `ensureDemoFullFlowAtStage()` inside golden path test body | Proves API not UX |
| Assert only `RoleJourneyTimeline` visible | Proves IA not operability |
| Skip with `test.skip` without manifest entry | Hides launch risk |
| UIUX manifest without Verdict line | Blocks CD-2 Done |

---

## 5. CD-2 exit gate

- [x] CD-E2E-T01 Done (2026-07-10) — T07…T11 remain
- [x] CD-E2E-T02/T03/T04 Done (2026-07-10 — slice `cdp-e2e-cd2-t02`; merge `6821f45`) — **not** wave exit
- [x] CD-E2E-T05 Done (2026-07-10 — slice `cdp-e2e-t05-publish`; merge `895f16e`) — **not** wave exit
- [x] CD-E2E-T06 Done (2026-07-10 — slice `cdp-e2e-t06-master`; merge `3aed175`) — **not** wave exit
- [x] CD-E2E-T08 Done (2026-07-10 — slice `cdp-e2e-t08-preview`; merge `c62b1a1`) — **not** wave exit
- [x] CD-E2E-T07 Done (2026-07-11 — slice `cdp-e2e-t07-api-policy`; merge `1eb230b`) — **not** wave exit
- [ ] CD-E2E-T01…T11 Done (T12 recommended) — **wave not closed** (T09–T13 Not Started)
- [x] T01 paired manifest **PASS** (15 screenshots); T02/T03/T04 manifests **PASS** (3/1/3 shots); T05 manifest **PASS** (4 shots @1920); T06 manifest **PASS** (6 shots @1920); T07 manifest **PASS** (9 shots); T08 manifest **PASS** (3 shots @1920)
- [x] `execution-sync-ledger.md` records T01 + T02–T04 + T05 + T06 + T07 + T08 Playwright counts + Docker deploy (2026-07-11)
- [x] `usability-review.md` references golden path spec as confirmed evidence (T01)
- [ ] Full matrix + all manifests PASS before wave **Done**
