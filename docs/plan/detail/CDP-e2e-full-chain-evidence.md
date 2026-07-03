# CDP Wave CD-E2E — Full-Chain Browser Evidence

**Program:** [competitiveness-deepening-program.md](../competitiveness-deepening-program.md)  
**Wave:** CD-2 (starts after CD-BDD specs `ready`; **no dependency on P22**)  
**Owner default:** `e2e-test-engineer` + `e2e-uiux-reviewer`  
**Prerequisites:** Docker stack (`.\scripts\docker-deploy.ps1`); read `frontend/e2e/helpers/auth.ts`, `frontend/e2e/helpers/demo-full-flow.ts`

> **Session note:** CD-E2E runs in the **CDP session**. Lifecycle golden path uses existing demo seeds; full structured rendering fidelity is validated in the **P22 session**, not a blocker for CD-E2E-T01.

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
  2. Use dedicated seed template code `CDP-MVP-GOLDEN` (add to demo seeder in separate backend task if missing — coordinate via CD-E2E-T01b).
  3. Browser flow: GLOBAL_ADMIN login → (or role handoff) master approve → author configure minimal binding → test tab SSE preview success → Pass test form → Submit for approval → Approver Approve → GROUP_ADMIN publish confirm → API policy save one domain → runtime generate via UI-exposed path OR contract page trigger documented in spec.
  4. Assert queue items appear/disappear on Dashboard after each decision.
  5. Pair UIUX manifest `frontend/e2e/evidence/CDP-E2E-T01-uiux-manifest.md` (≥8 screenshots, REDBC + GREENBC).
- **Acceptance:**
  - **G** Docker stack ready **W** spec runs **T** ≥1 test green end-to-end without API stage advancement for test/approve/publish.
  - **G** author submits failing gate **T** submit blocked with visible checklist (reuse AUD-B10 patterns).
- **Gates:** Playwright green; `e2e-uiux-reviewer` PASS.
- **Status:** Not Started

### CD-E2E-T01b — Golden path seed fixture (backend)

- **Owner:** `backend-engineer`
- **Depends on:** CD-BDD-T01 draft
- **Steps:** Add `CDP-MVP-GOLDEN` template seed: DRAFT, minimal binding, one test dataset, callable after publish script.
- **Acceptance:** Idempotent Flyway/SQL or DemoCatalogSeeder marker; documented in spec.
- **Status:** Not Started

### CD-E2E-T02 — Tester structured pass journey

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/tester-decision-journey.md`, `P21-T05-tester-journey.spec.ts`
- **Artifacts:** `CDP-E2E-T02-tester-pass-decision.spec.ts` + UIUX manifest
- **Acceptance:** UI clicks Pass → structured form fields → confirm → work item leaves TEST queue.
- **Status:** Not Started

### CD-E2E-T03 — Tester structured fail journey

- **Owner:** `e2e-test-engineer`
- **Acceptance:** Fail path requires reason category + returns template to fix state; collaboration todo visible to author.
- **Status:** Not Started

### CD-E2E-T04 — Approver approve journey

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/approver-decision-journey.md`
- **Acceptance:** Approve with evidence confirmation + risk summary viewed; extends P12-AUD-B10 (submit side only today).
- **Status:** Not Started

### CD-E2E-T05 — Team lead publish / go-live

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/team-lead-publish-journey.md`
- **Acceptance:** PENDING_RELEASE → publish summary dialog → confirm → release version callable (UI indicator).
- **Status:** Not Started

### CD-E2E-T06 — Master designer upload-to-approve

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/master-designer-lifecycle.md`, `master-replace-docx.spec.ts`
- **Acceptance:** Upload (or fixture) → anchor check UI → submit review → approver approves in browser.
- **Status:** Not Started

### CD-E2E-T07 — API policy edit → impact preview → save

- **Owner:** `e2e-test-engineer`
- **Read first:** `docs/behavior/api-policy-edit-save-journey.md`, `demo-full-lifecycle.spec.ts` stages 6–9
- **Acceptance:** One config domain (e.g. output format) edited, impact preview shown, save confirmed, audit entry link visible.
- **Status:** Not Started

### CD-E2E-T08 — Preview success + artifact download UI

- **Owner:** `e2e-test-engineer`
- **Fixes:** T13 manifest gap «preview success frame not captured»
- **Acceptance:** Test tab shows preview complete; download link works; screenshot in manifest.
- **Status:** Not Started

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
| P12-API BDD S1–S3, S7 | Package materialize on publish, default route stability | CD-E2E-T13 (Not Started — add after T01) |

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

- [ ] CD-E2E-T01…T11 Done (T12 recommended)
- [ ] All paired manifests **PASS**
- [ ] `execution-sync-ledger.md` records Playwright counts + Docker deploy date
- [ ] `usability-review.md` references golden path spec as confirmed evidence
