# Project Status Reset

**Effective date:** 2026-06-23

## Decision

The project **restarted from zero** on 2026-06-23. All prior wave, epic, milestone,
and task-sheet completion claims were **historical and void** until re-earned with
real, durable, verifiable behavior and green quality gates.

## Re-earned progress (2026-06-23)

Phases **P0–P11** have been re-earned **Done** with implementation in `backend/`
and `frontend/`, green quality gates, and updated plan detail docs. **P13** (identity &
group administration) was subsequently delivered and marked **Done** (2026-06-23) with the
same gate bar (backend `mvn verify` 114 tests green / JaCoCo met; frontend lint/type-check/test/build green).

| Evidence | Location |
| --- | --- |
| Phase status | [docs/plan/master-plan.md](./plan/master-plan.md) |
| Task-level detail | [docs/plan/detail/](./plan/detail/) |
| Epic/milestone mirror | [docs/plan/execution-sync-ledger.md](./plan/execution-sync-ledger.md) |
| Gate logs | Backend `mvn verify`; frontend lint/type-check/test/build (2026-06-23) |

**Active formal phase (repo-wide):** **None** (2026-07-09+). **CORE-FORTRESS Done** (F1–F8); **CODE-QUALITY Done**. See [master-plan.md](./plan/master-plan.md).

**Delivery focus note (2026-07-24):** **#164** slim demo catalog keep bank-letters (`demo-catalog-keep-bank-letters`) → **Done** (MAIN tip/merge `0e6d0bad`; feature `6e8cc8b3`; worktree **REMOVED**; Batch **solo** closed; BDD **ready**/shipped docs/behavior/demo-catalog-keep-bank-letters.md **BDD-DEMO-KEEP-001…014**; Gates `mvn verify` SUCCESS / FE GREEN / DEPLOY_OK / **focused** Stage 6 demos E2E **14/14** (not full suite) / Arch+CQ **PASS_WITH_NOTES**; evidence docs/plan/evidence/demo-catalog-keep-bank-letters/; **sole-active cleared**; formal phase **None**; vetoes held #3b/#5a / CE-O02 / #53 / #106 / #119; follow-ups: orphan-SQL BDD-004/005 not fully automated, cleanup-pagination, residual-e2e purged IDs). Prior **Delivery focus note (2026-07-23):** **#162** PQH N22 Catalog row actions (`pqh-n22-catalog-row-actions`) → **Done** (MAIN tip/merge `ef1b505d`; Stage 10 tip `c5121164`; FE product tip `c094d513`; worktree **REMOVED**; Batch **solo** closed; BDD **ready**/shipped docs/behavior/pqh-n22-catalog-row-actions.md **BDD-PQH-N22-001…014**; detail docs/plan/detail/pqh-n22-catalog-row-actions.md; program docs/plan/post-queue-hardening-program-2026-07.md Leaf 3 **Done** / Leaf 1–3 **Done** / program **In Progress**; Gates FE/E2E **7/7**/UIUX **PASS**/Arch **PASS_WITH_NOTES**/Stage 5+10 **DEPLOY_OK**; **sole-active cleared**; **push blocked** (GitHub 443; `main` ahead of `origin/main`); F7 **#163** parked (not auto-activated); formal phase **None**). Prior **#161** PQH N19–N20 EntityLink (`pqh-n19-n20-entitylink`) → **Done** (MAIN tip `20c67ac9`; feature `1e023a35`; worktree **REMOVED**; Batch **solo** closed; BDD **ready**/shipped docs/behavior/pqh-n19-n20-entitylink.md **BDD-PQH-N19N20-001…014**; detail docs/plan/detail/pqh-n19-n20-entitylink.md; program docs/plan/post-queue-hardening-program-2026-07.md Leaf 2 **Done** / program **In Progress**; Gates FE **1721**/E2E **7/7**/UIUX **PASS_WITH_NOTES**/Arch **PASS_WITH_NOTES**/Stage 5+10 **DEPLOY_OK**; **sole-active cleared**; formal phase **None**). Prior **Delivery focus note (2026-07-23):** **#159**+**#160** PQH-CHARTER + PQH-F8 (`pqh-f8-format-date-tz`) → **Done** (MAIN tip `ab382c02`; feature `ee0893fe`; worktree **REMOVED**; Batch **merge** closed; BDD **ready**/shipped docs/behavior/pqh-f8-format-date-tz.md **BDD-PQH-F8-001…012**; detail docs/plan/detail/pqh-f8-format-date-tz.md; program docs/plan/post-queue-hardening-program-2026-07.md Leaf 1 **Done** / program **In Progress**; Gates `mvn verify` **GREEN 2414**/Arch **PASS_WITH_NOTES**/Stage 10 **DEPLOY_OK**; **sole-active cleared**; next queue **#161** pending; formal phase **None**). Prior **Delivery focus note (2026-07-22):** **#155**+**#156** Binding editor IA + auto `referenceKey` (`binding-editor-ia`) → **Done** (MAIN merge `9f2378ad`; feature `9e318d9c`; worktree **REMOVED**; Batch **merge** closed; BDD **ready**/shipped docs/behavior/binding-editor-ia.md **BDD-BEI-001…020**; detail docs/plan/detail/binding-editor-ia.md; program docs/plan/system-normalization-program-2026-07.md §4a Binding editor + Auto `referenceKey` → **Done**; Gates FE **1697**/E2E **9/9**/UIUX **PASS_WITH_NOTES** Critical=0/Arch **PASS_WITH_NOTES** Critical=0/CQ Critical FIXED/Stage 5+10 **DEPLOY_OK**; **sole-active cleared**; §4a parked UX queue **empty**; formal phase **None**; **N18 deferred**). Prior **#154** Asset library group isolation → **Done** (`c12a0687` / `5b48117f`). Prior **#153** Reminder timing settings IA → **Done** (`d213834f` / `807d8213`). Prior **#152** SYS-NORM Wave 8 → **Done** (`8aca145b` / `7df6c563`; Waves **0–8 Done** — **program Done**; detail docs/plan/detail/sys-norm-demo-seed-terms.md). Prior **#151** Wave 7 → **Done** (`11356c63` / `f795b04a`). Prior **#150** Wave 6 → **Done** (`64b0a650`). Prior **#149** Wave 5 → **Done** (`febb95b3`). Prior **#148** Wave 4 → **Done** (`dac9dcd9` / `5c71acc0`). Prior **#147** SYS-NORM Wave 3 → **Done** (`18a9e3b2` / `f21dda5e`). Prior **#146** SYS-NORM Wave 2 → **Done** (`5d77db80` / `992f6822`). Prior **#145** SYS-NORM Wave 1 → **Done** (`7a62be44` / `f1594f2a` / `ce2cb9f0`; N18 deferred). Prior **#144** Published template test artifacts → **Done** (`ac36ecbc` / `6bc74ff1`; remapped from mistaken **#143**). Prior **#143** SYS-NORM Wave 0 → **Done** (`f8e898ad` / `28d4abe1` / `ade18bdb`; program SoT + ADR-0070/0071 Accepted; docs-only gates N/A). Prior **#142** Demo expand Wave B → **Done** (`288ce98f`; Wave B complete; registry 20/20; generate 20/20; `mvn verify` GREEN 2340; docker DEPLOY_OK; arch merge_go; FE E2E N/A; vitest baseline RED unrelated; do not reopen P22/P23; CE-O02 Deferred; no RTL; do not claim go-live). Prior **#141** Demo refresh Wave A → **Done** (`aa88170f` / `5ae9575a`; Wave A complete; import/publish/generate 13/13). Prior **#140** CD-HARD-T06 → **Done** (`b933965c`; Wave CD-3 Done). Prior **#139** PD-7 → **Done** (`b966874a`; OUT of IBL-E; pursue docs ADR-0069 + procurement; docs-only gates; **Accepted ≠ embedding Done**; still cannot ship without licensed assets; #119 Blocked; do not claim embedding / IBL program Done). Prior **#138** PD-6 → **Done** (`73ab03cb`; OUT of IBL-E; F6 opt-in production re-issue; `mvn verify` GREEN 2309; arch PASS_WITH_SUGGESTIONS merge_go; Stage 10 DEPLOY_OK). Prior **#134** IBL-E7 → **Done** (`37239d68` / `68abc7c3`; F15 closed by descope; ADR-0068 Accepted; Wave IBL-E → Done E1–E7).

**Latest gate evidence (authoritative):** backend `mvn verify` BUILD SUCCESS (2026-07-03, P12-TEMPLATE-TESTING-OVERHAUL);
frontend **643** Vitest (2026-07-03, P12-TEMPLATE-TESTING-OVERHAUL); Playwright T13 **8+1** passed, **3** skipped (documented); UIUX manifest **PASS**; see [execution-sync-ledger.md](./plan/execution-sync-ledger.md).

| Phase | Status | Closed |
| --- | --- | --- |
| P0–P11 | Done | 2026-06-23 |
| P13 Identity & group administration | Done | 2026-06-23 |
| P14 Confirmed large domains (T01–T03) | Done | 2026-06-27 |
| P15 Kubernetes deployment (T01–T10) | Done | 2026-06-27 |
| P16 Lifecycle/version governance | Done | 2026-06-23 |
| P17 Per-domain API policy | Done | 2026-06-25 |
| P18 Structured authoring & fidelity | Done | 2026-06-28 |
| P19 Verifiability & publish gate | Done | 2026-06-25 |
| P20 i18n & UI upgradeability | Done | 2026-06-25 |
| P12 Deferred enhancements (P12-TEMPLATE-TESTING-OVERHAUL) | Not Started (slice Done) | 2026-07-03 (P12-TEMPLATE-TESTING-OVERHAUL T01–T13 closed) |
| P21 Role-journey frontend redesign | Done | 2026-06-30 |

**P14 summary:** content modules (T01), collaboration to-dos + timeout escalation (T02),
template export/import (T03); E2E/UIUX green; backend **481** at phase close.

**P15 summary:** container hardening, Helm chart, ConfigMap/Secret, Ingress/TLS, HPA,
NetworkPolicy, probes, blue-green, CI manifest gates — see [deploy/README.md](../deploy/README.md).

**P18 summary:** controlled node matrix, style catalog, paste cleaning, renderProfile,
fidelity engine + management UI; Playwright P18-T10 **5/5** + UIUX evidence (2026-06-29).

**Optimization backlog:** COR-0…COR-5 **Done**; COR-6 phase-aligned domains **Done**;
OPT Wave 3 **In Progress** (OPT-D5 service split, OPT-G frontend polish) — see
[optimization-plan.md](./plan/optimization-plan.md).

**Still open (not blocking MVP Done):** external deployment validation (E05-T06),
role-journey release evidence (E06), intranet SCA (M9-T02), M10–M11 security closure,
real K8s cluster deploy evidence (P15 render-only).

## What is preserved

| Category | Status |
| --- | --- |
| Confirmed requirements (`docs/requirements/`, `docs/product/PRD.md`) | Preserved |
| Domain model, permission matrix, API contract (OpenAPI v1) | Preserved |
| Accepted ADRs under `docs/adr/` | Preserved — ADR status records a **decision**, not task completion |
| Architecture views (module boundaries, runtime, storage, security) | Preserved as design baseline |

## What was reset (2026-06-23)

| Category | Baseline at reset |
| --- | --- |
| Epic / wave / milestone / task completion | Void until re-earned |
| Closure evidence, gate logs, in-repo done snapshots | Empty until re-captured |
| Active epic / active phase | None until explicitly activated |

## Execution truth

- **Overall plan:** `docs/plan/master-plan.md`
- **Detailed plans:** `docs/plan/detail/<phase>.md`
- **Sync ledger:** `docs/plan/execution-sync-ledger.md`
- **Epic ordering reference:** `docs/architecture/orchestration-high-level-plan.md`
- **Technical wave reference:** `docs/architecture/implementation-task-plan.md`

## Status vocabulary (only these)

- `Not Started` — no meaningful implementation work yet
- `In Progress` — active delivery focus (only one phase/epic at a time)
- `Blocked` — cannot proceed until dependency, decision, or environment is resolved
- `Done` — exit criteria met with real behavior + green gates + updated docs

## Re-earning Done

A task or phase may be marked `Done` only when:

1. Behavior is durable (not demo/in-memory/mock-only).
2. Required tests and quality gates pass.
3. Owning documentation and plan status are updated in the same change set.
4. Post-task doc sync completes (see `.cursor/rules/post-task-doc-sync-constitution.mdc`).
