# Competitiveness Deepening Program (CDP)

**Program ID:** `CDP`  
**Created:** 2026-07-04  
**Status:** **In Progress** (Wave **CD-2** — **partial**; T01/T01b + **T02/T03/T04 Done** (merge `6821f45`); **CD-E2E-T05 In Progress** (`cdp-e2e-t05-publish`); T06–T12 + T13 Not Started; CD-0 **Done**)  
**North star:** Close the gap between **「功能齐全」** and **「银行敢用、业务愿用、集成方信得过」** by making rendering fidelity, verifiable publish, role-complete journeys, and documentation truth **provably solid** before production launch.

**Authoritative entry for lower-tier implementers:** Read this file first, then the wave detail doc for your task ID prefix.

| Detail doc | Scope |
| --- | --- |
| [detail/CDP-doc-truth-reconciliation.md](./detail/CDP-doc-truth-reconciliation.md) | Wave CD-DOC — documentation drift fixes |
| [detail/CDP-e2e-full-chain-evidence.md](./detail/CDP-e2e-full-chain-evidence.md) | Wave CD-E2E — browser golden paths + UIUX evidence |
| [detail/CDP-industry-pitfall-registry.md](./detail/CDP-industry-pitfall-registry.md) | Wave CD-PIT — industry pitfalls → ADR/NFR/test mitigations |
| [detail/P22-demo-expansion-rendering-fidelity.md](./detail/P22-demo-expansion-rendering-fidelity.md) | **Done** (2026-07-04) — rendering engine + demos (do not reopen from CDP) |
| [launch-readiness-program.md](./launch-readiness-program.md) | **Sibling program (LRP)** — **Wave LR-A Done** (2026-07-10; A1–A7; merge `cc9e5f6`; **ADR-0041 Accepted**; 0042/0043 Proposed; Word/XSD/LO24 deferred); do not execute `LR-*` from CDP session |
| [comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md) | Historical COR/OPT waves (mostly Done) |

---

## Session routing (read first)

| Work stream | Where it runs | This CDP session owns |
| --- | --- | --- |
| **P22** (P22-T01…T15, rendering + demos) | **Done** (2026-07-04) — track via [P22 detail](./detail/P22-demo-expansion-rendering-fidelity.md) | **Nothing.** Do not reopen P22 from CDP. |
| **LRP** (`LR-*`, Wave LR-A) | **Wave LR-A Done** (2026-07-10 — A1–A7; merge `cc9e5f6`; **ADR-0041 Accepted**; 0042/0043 Proposed) | **Nothing.** Do not execute `LR-*` / virus scan / audit-governance from CDP session. |
| **CDP** (doc truth, BDD, E2E, pitfall specs, CD-HARD) | **Sibling program** — Wave **CD-2** partial (T01–T04 Done; **T05 In Progress**; T06–T12 Not Started) | CD-0 **Done** → CD-2 **In Progress** (T01/T01b + **T02/T03/T04 Done**; **T05 In Progress**; T06–T12 Not Started) → CD-3 later. |

**Formal phase note:** `master-plan.md` has formal phase **None** (2026-07-09+). CDP Wave **CD-2** is the active **program wave** (not a formal phase). **CDP implementers follow task IDs prefixed `CD-*` only.**


## 0. Executive summary

### 0.1 What we are NOT doing

- **Not** adding new product modules (content modules, API packages, identity, etc.) — breadth is sufficient.
- **Not** rewriting the stack (Java/Spring/Vue/LibreOffice remain per ADR guardrails).
- **Not** claiming Done on thin-slice behavior without upgrading exit criteria.

### 0.2 What we ARE doing (four deepening axes)

| Axis | Problem today | CDP outcome |
| --- | --- | --- |
| **A. Rendering truth (docs only here)** | P18 authoring Done but DOCX write path still plain-text downgrade; P18/P4 marked Done overstates fidelity | CDP documents honest split + pitfall registry; **P22 session** implements write path |
| **B. Journey operability** | 51 Playwright specs but **no single browser MVP chain**; lifecycle mid-stages driven by API helpers | Golden-path E2E + per-role **task-completion** specs (not only IA/timeline visibility) |
| **C. Documentation truth** | P22 vs None active phase; P12-API Done vs paused; COR-6 «all closed» vs open rendering gap | Single execution truth; seams index complete; lower-tier models cannot mis-route |
| **D. Production pitfall immunity** | Fonts, LibreOffice layout drift, OOXML strictness, async PDF, numbering — known industry traps under-specified | Pitfall registry → ADRs, NFR baselines, Docker image checks, regression tests |

### 0.3 Relationship to P22 (external session)

**P22 is not part of CDP execution.** Rendering fidelity code (P22-T01…T15) runs in a **separate session** per [P22 detail](./detail/P22-demo-expansion-rendering-fidelity.md).

**CDP interacts with P22 only as:**

| CDP delivers | P22 consumes |
| --- | --- |
| Pitfall registry + proposed ADRs (CD-PIT) | P22-T01/T03/T15 acceptance references |
| Doc truth: P18/P4 «authoring Done, write path open» | Avoids false Done claims |
| E2E BDD for lifecycle journeys (CD-BDD) | Independent of rendering write path |

**CDP does not gate on P22.** Wave CD-0 and CD-2 (lifecycle/browser E2E) proceed without waiting for P22-T01.

```text
[Other session]  P22-T01…T15  ── rendering + eight demos
       ▲
       │ reads pitfall registry + honest P18/P4 docs (CD-0)
       │
[This session]   CD-0 → CD-2 → CD-3
                 doc truth, BDD, browser E2E, post-P22 hardening specs
```

**Repo phase accounting:** `master-plan.md` formal phase is **None** (2026-07-09+). **Wave LR-A Done** (2026-07-10; A1–A7; merge `cc9e5f6`; **ADR-0041 Accepted**; 0042/0043 Proposed; Word/XSD/LO24 deferred). CDP Wave CD-2 remains **In Progress** as a sibling program (partial — T01/T01b + **T02/T03/T04 Done**, merge `6821f45`; **CD-E2E-T05 In Progress**; T06–T12 Not Started). LRP program remains **In Progress** (**LR-C9 Done** 2026-07-10 — merge `0013615`; Wave LR-C partial; Wave LR-A Done — Word/XSD residuals deferred separately). CDP is a **parallel program** — not a phase replacement.

---

## 1. Core competitiveness → CDP mapping

From [authoring-rendering-first-principles-review.md](../product/authoring-rendering-first-principles-review.md) and [usability-review.md](../product/usability-review.md):

| Competitiveness pillar | Current evidence | CDP primary waves | Done means |
| --- | --- | --- | --- |
| **Controlled authoring → faithful DOCX** | P18 UI + validation; gap documented | **P22 session** (T01…T04); CD-PIT specs here | P22 detail §3 exit criteria |
| **Verifiable publish gate** | P19 Done; submit-for-approval gate (P12-AUD-B10) | CD-E2E-T03…T05 | Browser: test pass → submit approval → approve → publish with structured forms |
| **Embeddable runtime API** | P12-API Done; runtime E2E API-only | CD-E2E-T06, CD-PIT-09 | Caller contract page + invocation history journey; idempotency/rate-limit seam documented |
| **Role journey task time** | P21 IA Done; mostly visibility E2E | CD-E2E-T01…T12, CD-UX-T01 | Per-role golden screenshot + task-completion Playwright; optional timing budget in NFR |
| **Governance without friction** | API policy UI thin; impact preview partial E2E | CD-E2E-T07, CD-BDD-T01 | Edit → impact preview → save for ≥1 config domain in browser |

---

## 2. Program waves (sequencing)

Only **one CDP wave** may be `In Progress` at a time (same discipline as phase plans).

| Wave | Name | Type | Depends on | Exit gate | Session |
| --- | --- | --- | --- | --- | --- |
| **CD-0** | Doc truth + pitfall specs + E2E BDD drafts | **Non-code** | — | CD-DOC batch Done; pitfall ADR drafts ready; CD-BDD specs `ready` | **This** |
| **CD-2** | Full-chain E2E + UIUX evidence | Test + frontend | CD-0 BDD `ready` | CD-E2E-T01…T12 Done; manifests PASS | **This** |
| **CD-3** | Production pitfall hardening | Code + infra | P22 Done (external) | CD-HARD-T01…T06; NFR §production rendering | **This** (after P22) |
| *(P22)* | *(Rendering + demos)* | *(Code)* | *(P22 plan)* | *(P22 phase Done)* | ***Other session*** |

**Removed from CDP:** former «CD-1 = P22 code» — that work is **only** in the P22 session, not delegated via CDP task IDs.

**Current wave (this session):** **CD-2** — **In Progress** (2026-07-10; **partial progress**). **CD-0 Done**. **CD-E2E-T01** + **T01b** → **Done** (merge `1930842`). **CD-E2E-T02** + **T03** + **T04** → **Done** (slice `cdp-e2e-cd2-t02`; merge `6821f45`). **CD-E2E-T05 → In Progress** (slice `cdp-e2e-t05-publish`). T06–T12 + T13 remain `Not Started`. Do **not** mark full CD-2 Done until remaining matrix green.

---

## 3. Wave CD-0 — Non-code deliverables (FIRST PRIORITY)

Lower-tier models in **this session**: complete tasks prefixed **`CD-*`** only. Do not pick up **`P22-*`** tasks from this program doc.

### 3.1 CD-DOC — Documentation truth (20 tasks)

**Owner:** `doc-keeper` or `post-task-doc-sync` checklist inline.  
**Detail:** [CDP-doc-truth-reconciliation.md](./detail/CDP-doc-truth-reconciliation.md)  
**Exit:** Zero contradictions on active phase, P12-API status, P18 vs P22 rendering split; seams index complete.

### 3.2 CD-PIT — Industry pitfall registry (spec-only in CD-0)

**Owner:** `doc-keeper` + `architecture-reviewer` review.  
**Detail:** [CDP-industry-pitfall-registry.md](./detail/CDP-industry-pitfall-registry.md)  
**Exit:** Each pitfall has: symptom, detection test, mitigation owner, ADR or NFR anchor.

**Headline pitfalls (from industry + codebase audit):**

| ID | Pitfall | Why it kills bank trust |
| --- | --- | --- |
| CD-PIT-01 | **Missing fonts in conversion container** | LibreOffice substitutes → line breaks/page count drift vs author Word |
| CD-PIT-02 | **LibreOffice ≠ Word layout engine** | PDF pagination legally significant for filings; must document acceptable delta |
| CD-PIT-03 | **OOXML strictness / escaping** | Generated DOCX «corrupt» in LO 24+ until XML properly escaped |
| CD-PIT-04 | **Word numbering (`numId`/`ilvl`) fragility** | Lists restart wrong across sections — top structured-content bug class |
| CD-PIT-05 | **Dual page fields (`PAGE`/`SECTIONPAGES`/`NUMPAGES`)** | FOL-style letters require section + global semantics |
| CD-PIT-06 | **Sync LibreOffice on request thread** | Timeouts under load; already OPT-F6 seam |
| CD-PIT-07 | **Rich-text boundary creep** | Editors paste layout into prose fields → fidelity warnings or silent loss |
| CD-PIT-08 | **Preview ≠ final artifact authority** | Users trust edit preview; must enforce final-path evidence in UI copy + E2E |
| CD-PIT-09 | **Template fill programmatic fragility** | Table auto-fit, margin drift when generating at scale |
| CD-PIT-10 | **Two-UI problem** | Separate review PDF vs in-app review → SMEs disengage (Paligo/CCMS lesson) |

### 3.3 CD-BDD — Behavior specs for missing journeys

**Owner:** `behavior-spec-author`.  
**Output location:** `docs/behavior/` (new files; index in `docs/README.md`).

| ID | BDD file to create | Covers |
| --- | --- | --- |
| CD-BDD-T01 | `mvp-golden-path-browser.md` | Login → master approve → template → test → approve → publish → API → runtime |
| CD-BDD-T02 | `tester-decision-journey.md` | Structured pass/fail form + evidence confirmation |
| CD-BDD-T03 | `approver-decision-journey.md` | Approve/reject + risk summary + fidelity viewed |
| CD-BDD-T04 | `team-lead-publish-journey.md` | Go-live summary + secondary confirm |
| CD-BDD-T05 | `master-designer-lifecycle.md` | Upload → anchor check → submit review → approve |
| CD-BDD-T06 | `api-policy-edit-save-journey.md` | One config domain edit → impact preview → save |
| CD-BDD-T07 | `preview-comparison-journey.md` | Side-by-side preview vs final DOCX/PDF |
| CD-BDD-T08 | `audit-admin-query-journey.md` | Filter + export smoke |

Each spec MUST include: Actor, Goal, Trigger, Preconditions, Primary Journey, Acceptance Scenarios (G/W/T), Boundary/exception, Observable evidence, Traceability to PRD §.

**Readiness gate:** Status `ready` in spec header before `e2e-test-engineer` implements.

### 3.4 CD-UX — Usability metrics & journey maps (doc-only)

**Owner:** `doc-keeper` updating [usability-review.md](../product/usability-review.md).

| ID | Task | Acceptance |
| --- | --- | --- |
| CD-UX-T01 | Define **role task-time budgets** (draft targets, not contractual SLAs) | Table: role × task × target minutes × measurement method (Playwright timing optional) |
| CD-UX-T02 | Resolve **§待确认** items L87–91 | Either confirmed into PRD/NFR or explicitly deferred with reason |
| CD-UX-T03 | Add **forbidden UX anti-patterns** list | e.g. API helper-only E2E for user-facing Done claims; timeline-only journey tests |
| CD-UX-T04 | **MASTER_DESIGNER ↔ TEMPLATE_AUTHOR** landing fusion decision | Record in PRD or ADR; update P21 doc footnote |

### 3.5 CD-P22-HANDOFF — Pitfall IDs for P22 session (optional doc-only)

**Owner:** `plan-orchestrator` in **P22 session**, or `doc-keeper` here as handoff artifact.  
**Action:** When P22 session starts, amend [P22 detail](./detail/P22-demo-expansion-rendering-fidelity.md) T01/T03/T04/T15 to reference CD-PIT IDs from [pitfall registry](./detail/CDP-industry-pitfall-registry.md).  
**Not a CDP exit blocker:** CD-0 can complete without editing P22 task rows.

---

## 4. P22 rendering (external session — not CDP)

**P22-T01…T15 are out of scope for this program and this session.**

Track progress only via:

- [detail/P22-demo-expansion-rendering-fidelity.md](./detail/P22-demo-expansion-rendering-fidelity.md)
- `docs/plan/execution-sync-ledger.md` § P22
- [demo-expansion-behavior-spec.md](../requirements/demo-expansion-behavior-spec.md)

CDP **feeds** P22 via CD-PIT registry and honest P18/P4 doc split (CD-DOC). CDP **does not** schedule or gate P22 implementation.

---

## 5. Wave CD-2 — E2E full-chain evidence

**Detail:** [CDP-e2e-full-chain-evidence.md](./detail/CDP-e2e-full-chain-evidence.md)

**Principle:** Replace «API pushes state + UI peeks» with «browser completes user intent» for all **Done**-claim journeys.

**Priority order (user value):**

1. CD-E2E-T01 — MVP golden path (single spec, ≤25 min Docker)
2. CD-E2E-T02…T05 — Tester / Approver / Team-lead / Master-designer decision chains
3. CD-E2E-T06 — Runtime observable from management UI
4. CD-E2E-T07 — API policy edit-save-impact
5. CD-E2E-T08…T12 — Preview download, comparison, audit, zh-CN dual-brand

Each task pairs: `*.spec.ts` + `evidence/*-uiux-manifest.md` + `e2e-uiux-reviewer` PASS.

---

## 6. Wave CD-3 — Production hardening (post-P22)

| ID | Task | Owner | Maps | Status |
| --- | --- | --- | --- | --- |
| CD-HARD-T01 | Font bundle in Docker image + CI smoke | deploy-engineer | CD-PIT-01 | **Done** (executed-by-LR-A2 / P23-T02) |
| CD-HARD-T02 | LibreOffice conversion pool (async) | backend-engineer | CD-PIT-06, OPT-F6 | Done (OPT-F6 / COR-P02 lineage) |
| CD-HARD-T03 | OOXML output validation test (LO 24 open) | backend-engineer | CD-PIT-03 | **Done** (2026-07-10 — executed-by-LR-A6; merge `122d6d1`; `OoxmlOutputValidator` fail-closed; LO24 / ECMA-376 XSD deferred; ADR-0043 remains Proposed) |
| CD-HARD-T04 | Pagination delta budget doc + sample corpus | doc-keeper | CD-PIT-02 | **Done** (2026-07-10 — executed-by-LR-A7; Docker PDF corpus ≥5 + FOL; Word/delta n/a — `ms-word-unavailable-on-host`; ADR-0042 remains Proposed) |
| CD-HARD-T05 | Paste cleaning wired to binding validation OR ADR «edit-time only» | backend-engineer | P18-T07 seam | Not Started |
| CD-HARD-T06 | List audit/export E2E | e2e-test-engineer | CD-BDD-T08 | Not Started |

---

## 7. Lower-tier model delegation protocol

Every implementer task across CDP MUST include these fields (detail docs follow this template):

```markdown
### <TASK-ID> — <title>
- **Owner agent:** backend-engineer | frontend-engineer | e2e-test-engineer | doc-keeper | ...
- **Read first:** <ordered file list>
- **Do NOT:** <explicit forbidden scope creep>
- **Steps:** numbered, ≤8
- **Acceptance (G/W/T):** minimum 2 scenarios
- **Gates:** exact commands
- **Artifacts:** paths to create/modify
- **Done when:** behavior + gates + doc sync + ledger row
```

**Forbidden for lower-tier models (this session):**

- Picking up **`P22-*`** tasks (belongs to another session).
- Marking P18/P4 rendering «Done» without honest «write path open» doc language.
- Adding Playwright tests that only assert timeline visibility for decision/publish Done claims.
- Skipping `post-task-doc-sync` after behavior change.
- Changing ADR accepted decisions without user confirmation.

**Escalate to parent/human when:**

- BDD spec ambiguous after 1 clarification pass.
- Rendering fidelity requires new node type not in P18 matrix.
- E2E blocked >2h on Docker/seed — document skip reason in manifest.

---

## 8. CD-0 exit gate checklist

Mark CD-0 **Done** only when ALL true:

- [x] CD-DOC-T01…T20 merged (2026-07-04)
- [x] CD-PIT registry published; CD-PIT-01…10 each have mitigation row
- [x] CD-BDD-T01…T08 status `ready`
- [x] CD-UX-T01/T03 merged into usability-review; T04 pending
- [x] `docs/README.md` indexes CDP + behavior specs
- [x] `execution-sync-ledger.md` CDP section added
- [x] Proposed ADR drafts 0041–0043 (CD-PIT §4) — **optional**; deferred / non-blocking (0041 deferred; 0042/0043 Proposed under LRP-A5)

**CD-0 status:** **Done** (2026-07-10) — required checklist met; optional ADR drafts do not block wave close.

**Next wave (this session):** **CD-2 In Progress** — **T01/T01b Done** (merge `1930842`); **T02/T03/T04 Done** (2026-07-10; merge `6821f45`); **T05 In Progress** (`cdp-e2e-t05-publish`). T06+ Not Started. T07/T13 deferred.

---

## 9. Success metrics (launch readiness)

| Metric | Baseline (2026-07-04) | CDP target | P22 session (external) |
| --- | --- | --- | --- |
| Browser MVP golden path E2E | **None** (API hybrid) | **Met** — `CDP-E2E-T01` Docker **1/1** PASS (2026-07-10) | — |
| Structured node DOCX fidelity | Plain-text downgrade | Doc + pitfall specs Done | 8 demos, 0 false fallback |
| Playwright skipped scenarios (T13) | 3 skipped | 0 skipped OR documented fixtures (CD-2) | — |
| Doc status contradictions | ≥6 critical | 0 | — |
| UIUX manifests with formal PASS | 17/18 | 18/18 + golden path manifest | — |
| Transitional seams undocumented | ≥4 | 0 (ledger exit criteria) | — |

---

## 10. Traceability

| Source | CDP relationship |
| --- | --- |
| [PRD §6.5 rendering fidelity](../product/PRD.md) | CD-PIT specs; **implementation = P22 session** |
| [demo-expansion-behavior-spec.md](../requirements/demo-expansion-behavior-spec.md) | Read-only reference for pitfall mapping; **not executed here** |
| [usability-review.md](../product/usability-review.md) | CD-UX, CD-E2E |
| [optimization-plan.md](./optimization-plan.md) OPT-F6, OPT-G | CD-HARD, CD-2 |
| [master-plan.md](./master-plan.md) P22 row | Parallel formal phase; **not this session's task queue** |

---

**Next action (this session):** **CD-E2E-T05 → In Progress** (Team lead publish / go-live; slice `cdp-e2e-t05-publish`). Deliver PENDING_RELEASE → publish summary → confirm → release version UI (browser-only + UIUX). After T05: **T06** (master designer) or **T08** if publish fixtures blocked. Do **not** mark CD-2 Done. Out of scope: LR-C5/C10, audit-governance, LR-A. Formal phase remains **None**.
