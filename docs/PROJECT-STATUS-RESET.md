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

**Delivery focus note (2026-07-17):** **CORE-EXCELLENCE (CE)** — **P2 CE continuum** — **#94 CE-U20** → **Done** (merge `b9327a11` / feature `b4b0b420`; worktree removed); **#93 CE-U18** → **Done** (merge `05e9f8e1` / feature `e05407f2`; worktree removed); **no sole-active delivery leaf**; **§9.2 next serial:** **#95 CE-U21** → **pending**; **#92 CE-U16** → **Done** (merge `5d683c40` / feature `1a3d0f20`; worktree removed); **#91 CE-U15** → **Done** (merge `b2968052`; closeout `ed8a15e6`; worktree removed); **#90 CE-U14** → **Done** (merge `05e845e4` / feature `09e0d251`); **#75 CE-G04** → **Done** (merge tip `42745ea5` / feature `b47ea896`); **#81 CE-O01** → **Done** (merge `e081bcfa`; worktree removed); **#79 CE-E02** → **Done** (merge `5bd3611e`) — do **not** reopen; **#78 CE-E01** → **Done** (merge `6ae57974`); **#76 CE-G06** → **Done** (merge `d8636232`); **#71 CE-C06** → **Done** (merge `35f6f47d`) — do **not** reopen; **#89 CE-U13** → **Done** (merge `ccdfacda`); **#74 CE-G03** → **Done** (`50c1a524`); **#62 CE-K06** → **Done** (K06a `485a7f3e` + K06b `a689ca87` + K06c tip `76297d08`); **#69/#70/#88** → **Done** (`c7be8305` / `405f7cea` / `7734366e`); Wave 0 **#61/#86/#87** → **Done**; umbrella **#53** **in-progress** (program registry only). Prior waves **#60/#84/#85** and **#59/#68/#83** **Done**. **Batch 4 remains Done** — **#73 CE-G02** (merge `2ea74018`) + **#58 CE-K02** (merge `2f6792eb`). Formal phase remains **None**; **no sole-active formal phase**. Remaining blocking **NO-GO**: **#3b** only; CONDITIONAL **#10** / **#5a**; overall **NO-GO**. Do **not** activate CD-3. Leave **#50** alone. **Prior:** **BOOT-4-1-UPGRADE → Done** (merge `993c287`; tip `e9bf43c`; Task Master #51; Java 25 + Boot 4.1.0; ADR-0028 amended; mvn verify GREEN 1357/0/0/7; DEPLOY_OK_WITH_NOTES healthz 200). **Prior:** **DEPS-SECURITY-REFRESH → Done** (merge `08c7d56`; Task Master #49; Boot 3.3.13 + baseline-safe Maven/pnpm; Vitest Critical exception → #50 expires 2026-10-13; M9-T02 still In Progress). **Prior:** **SLIM-KNIP-SCAN Done** (merge `ea7db64`; Task Master #48). **Prior:** **OPS-PASTE-BINDING-SEAM → Done** (merge `f1f00da`; Task Master #47; checklist #5b → **GO**; CD-HARD-T05 Done; seam closed). **Prior:** **OPS-AD-GROUP-STUB-CLOSE Done** (merge `4e51a1b`; #5a **CONDITIONAL**). **Prior:** **OPS-KAFKA-COMPANY-REGISTRY → Done** (merge `e54d03c`; Task Master #45; checklist #10 **CONDITIONAL** — not GO without company registry evidence). **Prior:** **OPS-JWT-SECRET Done** (merge `587cd9a`; #9 **GO**). **Wave LR-D Done** (D1–D7; merge tip `218dcf1`) — [LRP-D detail](./plan/detail/LRP-D-ops-observability.md). **Wave LR-A Done** (A1–A7; merge `cc9e5f6`); **LR-A5 Done** (**ADR-0041 Accepted**; 0042/0043 Proposed; Word/XSD/LO24 deferred) — [launch-readiness-program.md](./plan/launch-readiness-program.md). **P12 catch-all Not Started** (slices Done). **P21 Done** (2026-06-30). User sequence **P14 → P15 → P18** complete.

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
