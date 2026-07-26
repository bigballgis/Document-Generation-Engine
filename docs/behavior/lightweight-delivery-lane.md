---
id: BDD-AI-SCALE-LIGHT-LANE
title: Lightweight delivery lane (skip E2E+Docker when acceptance surface unchanged)
status: ready
date: 2026-07-26
bdd_readiness: ready
task_ids: [166]
placement: ISOLATED
worktree_path: D:/working/DGE-ai-scale-remediation-g1
branch: feat/ai-scale-remediation-g1
slice: ai-scale-remediation-g1
user_confirmation: 2026-07-26 「按你的建议整改吧」
amends:
  - docs/behavior/delivery-batch-recommend.md  # Future selective-tests note → light lane for eligible leaves
---

# Lightweight Delivery Lane — BDD behavior spec

| Field | Value |
| --- | --- |
| **Slice** | `ai-scale-remediation-g1` (governance; this file = light-lane contract) |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-26 |
| **Actor** | `delivery-orchestrator` / parent agent / `build-deploy-agent` / E2E specialists |
| **Owning skills (to align)** | [delivery-pipeline](../../.cursor/skills/delivery-pipeline/SKILL.md), [delivery-batch-recommend](../../.cursor/skills/delivery-batch-recommend/SKILL.md) |
| **Product E2E / UIUX / runtime API** | **`not-applicable`** for this governance leaf; light lane **defines when product E2E/Docker are N/A for future leaves** |
| **Formal phase** | **None** |

---

## Classification

Agent-governance behavior: when a delivery leaf may use a **`light`** evidence path that
**skips stages 5–7 (E2E stack prep / functional / UIUX) and stage 10 (queued Docker
deploy evidence)** because BDD already proves **no user-facing / runtime acceptance
surface change**.

This lane **must not** weaken the full pipeline for product behavior-changing work.

| Surface | Applicability |
| --- | --- |
| Product management UI / Playwright E2E | **Full leaf required** when UI acceptance surface changes |
| Runtime / OpenAPI / Flyway acceptance | **Full leaf** (BE gates + deploy evidence as required) when runtime surface changes |
| Docs-only / agent-governance / single-module unit-fix with N/A acceptance | **Light lane eligible** |
| Worktree isolation | **Still mandatory** for multi-file delivery unless user `main-only` / `no-worktree` |

---

## 1. Actor / role

| Actor | Role |
| --- | --- |
| **delivery-orchestrator / parent** | Classifies `delivery_lane: full \| light` from BDD readiness + acceptance surface |
| **behavior-spec-author** | Must state whether UI/runtime acceptance surfaces change (`frontend_ui_in_scope`, runtime flags) |
| **build-deploy-agent** | Skips queued Docker when lane=`light` and eligibility holds; never invents a second stack |
| **e2e-test-engineer / e2e-uiux-reviewer** | **N/A** when lane=`light` and eligibility holds; **mandatory** when lane=`full` and UI in scope |
| **worktree-router / integration-merger** | Unchanged — isolation still applies for delivery writes |
| **End user** | **Out of scope** as actor |

---

## 2. Goal

1. Amortize host cost for leaves that cannot change user-facing or runtime acceptance
   surfaces (docs/governance/scaffold, or single-module unit-test refactors with BDD
   `not-applicable` / proven N/A surfaces).
2. Keep **full** pipeline (−1…14 with E2E+Docker when required) for any product
   behavior-changing work.
3. Preserve **mandatory worktree** for multi-file delivery; light lane is an **evidence**
   shortcut, not a MAIN-sandbox shortcut.
4. Emit an explicit, auditable `delivery_lane` decision in the handoff.

---

## 3. Trigger

| # | Trigger |
| --- | --- |
| T1 | Orchestrator finishes stage −1 / stage 1 and can classify acceptance surface |
| T2 | BDD marks `frontend_ui_in_scope=false` **and** no runtime/API acceptance change |
| T3 | Leaf is docs-only / agent-governance / constitution / skill scaffold |
| T4 | Red-gate re-plan: if a “light” leaf gains product surface mid-flight → **upgrade to full** |

**Non-triggers:** convenience (“E2E flaky so skip”); Batch Recommendation `merge` alone;
`force-parallel`; desire to avoid worktree.

---

## 4. Preconditions

- BDD readiness is `ready` or `not-applicable` with an explicit surface statement.
- Eligibility checklist (§6.1) is satisfied **or** lane is forced `full`.
- Single-lane serial host rule still applies.
- User has not required full gates in-session for that leaf.

---

## 5. Primary journey

1. Author BDD (stage 1) with explicit `frontend_ui_in_scope` and runtime/API surface flags.
2. Orchestrator sets `delivery_lane: light | full` with rationale citing BDD.
3. Stage 0 still provisions isolated worktree for delivery writes (unless `main-only`).
4. If `light`: skip stages **5, 6, 7, 10**; still run applicable unit/verify gates for
   touched code, architecture/doc review as needed, stage 11 merge, 12–13 on MAIN.
5. If `full`: existing pipeline — E2E when UI in scope; queued Docker when acceptance
   surfaces require deploy evidence.
6. Handoff records `delivery_lane` + `evidence_amortization`.

---

## 6. Confirmed decisions

### 6.1 Light-lane eligibility (all must hold)

A leaf **may** use `delivery_lane: light` only when **all** are true:

| # | Criterion |
| --- | --- |
| E1 | BDD proves **no** management-UI user journey / visual acceptance change (`frontend_ui_in_scope=false` or UI explicitly out of scope) |
| E2 | BDD proves **no** runtime / OpenAPI / Flyway / generation acceptance surface change **or** the leaf is pure docs/agent-governance with zero product code |
| E3 | Stage 1 readiness is `ready` or `not-applicable` (not `blocked`) |
| E4 | Orchestrator records `delivery_lane: light` + rationale quoting E1–E3 |
| E5 | Worktree rule still obeyed for multi-file delivery (light ≠ main-only) |

If any criterion fails → **`delivery_lane: full`**.

### 6.2 What light may skip (confirmed)

| Stage | Light lane |
| --- | --- |
| −1 Batch Recommendation | **Required** |
| 0 Worktree | **Required** for delivery (unless user `main-only` / `no-worktree`) |
| 1 BDD | **Required** (or explicit `not-applicable`) |
| 2–4 Plan / docs / implement | As applicable |
| **5 E2E stack prep** | **Skip** |
| **6 E2E functional** | **Skip** |
| **7 E2E UIUX** | **Skip** |
| 8 Architecture review | As applicable (docs/governance still reviewable) |
| 9 Code quality | Optional as today |
| **10 Deploy evidence (Docker queue)** | **Skip** |
| 11 Integration merger | **Required** when ISOLATED |
| 12–13 Doc-sync + commit-review | **Required** before Done |
| 14 Verifier | Optional |

### 6.3 What light must **not** skip (confirmed)

- BDD / TDD for behavior-changing **code** peels (code peels are out of G1; when queued
  later they use full or light per **their** BDD — never inherit light from a docs leaf).
- Backend `mvn verify` / frontend lint·type-check·test·build when those stacks are touched.
- Worktree isolation for multi-file delivery.
- Fail-closed authorization / security review expectations when security surfaces are touched
  (security-touching leaves are **full** by default — not light-eligible under E2).

### 6.4 Full leaf remains default for product behavior (confirmed)

Any change to user-visible management UI, Playwright-relevant journeys, runtime generation,
OpenAPI contracts, Flyway-visible schema, or permission-matrix behavior → **`full`**.
Doubt → **`full`**.

### 6.5 Worktree vs main-only (confirmed)

| Placement | When |
| --- | --- |
| **ISOLATED worktree** | Default for any multi-file / behavior-doc delivery leaf — **including light** |
| **MAIN / `main-only`** | Read-only Q&A; single mechanical one-line edit; or explicit user opt-out in-session |
| Light lane | **Does not** authorize coding on MAIN |

### 6.6 Relationship to Batch Recommendation “Future selective tests”

[delivery-batch-recommend.md](./delivery-batch-recommend.md) previously marked
“selective tests / risk-tier skip E2E” as **Future / not v1**. **This spec confirms** a
narrow, eligibility-gated light lane for leaves whose BDD proves acceptance surfaces are
N/A. It does **not** authorize risk-tier E2E skip for product UI leaves.

### 6.7 Handoff field (confirmed)

```
delivery_lane: full | light
delivery_lane_rationale: <cite BDD surface flags + E1–E5>
```

Orchestrators should also keep `evidence_amortization` consistent (e.g.
`docs lint/consistency; E2E/Docker N/A`).

---

## 7. System responses

### Success (light)

- Handoff shows `delivery_lane: light` with E1–E5 rationale.
- Stages 5–7 and 10 are recorded as **N/A** (not silently omitted without rationale).
- Stages 11–13 still complete on the normal path.
- Done claim does not invent Playwright/Docker green for skipped stages.

### Success (full)

- Unchanged full pipeline when eligibility fails or product surfaces change.

### Fail-closed / refuse

- Refuse light lane when UI or runtime acceptance surface changes.
- Refuse using light lane to avoid worktree.
- Refuse claiming Done with skipped BE/FE unit gates when code was touched.
- Refuse a second Docker compose project “just for light leaves”.

---

## 8. Boundary and exception behavior

| Case | Expected behavior |
| --- | --- |
| Docs-only constitution/skill update | `light` eligible |
| Single-module pure unit-test refactor, BDD `not-applicable`, zero API/UI | `light` eligible |
| FE copy/i18n user-visible string change | `full` (UI surface) |
| OpenAPI field add | `full` |
| Mid-leaf scope creep adds UI | Upgrade to `full`; run skipped stages before Done |
| User says “skip E2E” without BDD N/A proof | Refuse; keep `full` |
| `bdd_readiness: blocked` | No lane shortcut; block at stage 1 |

---

## 9. Observable evidence

| Evidence | Form |
| --- | --- |
| Lane decision | `delivery_lane` + rationale in Task handoff |
| Skipped stages | Explicit N/A notes in gate_evidence / ledger |
| Worktree placement | `placement: ISOLATED` (+ path) unless user opt-out |
| Product runtime | **None required for this governance leaf** |

---

## 10. Acceptance scenarios (Given / When / Then)

### LDL-01 — Docs-only leaf may be light

**Given** a leaf only changes agent/docs/governance files  
**And** BDD states `frontend_ui_in_scope=false` and no runtime acceptance change  
**When** the orchestrator classifies the lane  
**Then** `delivery_lane` is `light`  
**And** stages 5–7 and 10 are N/A  
**And** stages 11–13 still run when ISOLATED.

### LDL-02 — Product UI change forces full

**Given** BDD sets `frontend_ui_in_scope=true` (user-facing journey change)  
**When** the orchestrator classifies the lane  
**Then** `delivery_lane` is `full`  
**And** E2E functional + UIUX remain mandatory  
**And** queued Docker prep/evidence remains as required by the pipeline.

### LDL-03 — Runtime/API change forces full

**Given** the leaf changes OpenAPI, Flyway, or runtime generation acceptance  
**When** lane classification runs  
**Then** `delivery_lane` is `full`  
**And** light skip of deploy evidence is refused when stage 10 is required for that surface.

### LDL-04 — Light does not waive worktree

**Given** a multi-file docs/governance delivery  
**And** user did not say `main-only` / `no-worktree`  
**When** stage 0 runs under `delivery_lane: light`  
**Then** an isolated worktree is still provisioned  
**And** implementation does not proceed on MAIN.

### LDL-05 — main-only remains explicit opt-out

**Given** user says `main-only` for a single mechanical or explicitly opted delivery  
**When** placement is recorded  
**Then** MAIN placement is allowed per existing constitution  
**And** that opt-out is independent of `delivery_lane`  
**And** light lane is not cited as the reason for MAIN coding.

### LDL-06 — Doubt defaults to full

**Given** acceptance surface impact is unclear  
**When** lane classification runs  
**Then** `delivery_lane` is `full`  
**And** stage 1 remains `blocked` until surfaces are clarified if BDD is incomplete.

### LDL-07 — Mid-flight upgrade

**Given** a leaf started as `light`  
**And** new scope adds a management UI journey  
**When** the orchestrator re-plans  
**Then** lane upgrades to `full`  
**And** previously skipped E2E/Docker stages are executed before Done.

### LDL-08 — Honesty of evidence

**Given** `delivery_lane: light`  
**When** specialists report gate_evidence  
**Then** they record E2E/Docker as **N/A** with rationale  
**And** do not claim Playwright or deploy greens that were not run.

### LDL-09 — Batch Recommendation merge does not imply light

**Given** Batch Recommendation `decision: merge` for related product tasks  
**When** lane classification runs  
**Then** lane follows BDD surfaces of the merged leaf  
**And** merge alone never authorizes skipping E2E/Docker.

### LDL-10 — Security-sensitive touch not light by default

**Given** a leaf changes authorization fail-closed behavior or permission-matrix semantics  
**When** lane classification runs  
**Then** `delivery_lane` is `full`  
**And** light eligibility E2 fails.

### LDL-11 — Unit gates still required when code touched

**Given** a light-eligible single-module unit-test-only leaf that touches Java/TS  
**When** gates run  
**Then** applicable `mvn verify` and/or frontend lint/type-check/test/build still run  
**And** only E2E/Docker stages are skipped.

### LDL-12 — This G1 leaf itself is light

**Given** slice `ai-scale-remediation-g1` is docs/governance scaffold only  
**When** evidence is planned  
**Then** product Playwright E2E and Docker acceptance deploy are N/A  
**And** Code peel siblings remain separately queued (not folded into this light leaf).

---

## 11. Out of scope

- TemplateImport / i18n / mega-test peels (separate full/light classification later)
- Inventing risk-tier partial E2E for product UI leaves
- Parallel Docker stacks
- Waiving doc-sync / commit-review

---

## 12. Traceability

| Source | Link |
| --- | --- |
| User confirmation | 2026-07-26 「按你的建议整改吧」 |
| Pipeline stages | [delivery-pipeline SKILL](../../.cursor/skills/delivery-pipeline/SKILL.md) |
| Batch Recommendation | [delivery-batch-recommend.md](./delivery-batch-recommend.md) |
| Worktree constitution | `.cursor/rules/worktree-and-deploy-queue-constitution.mdc` |
| Sibling G1 behaviors | [module-map-agent-retrieval.md](./module-map-agent-retrieval.md), [ai-scale-docs-conventions.md](./ai-scale-docs-conventions.md) |

```
bdd_readiness: ready
task_ids: [166]
open_questions: []
product_e2e_uiux_backend: not-applicable
frontend_ui_in_scope: false
delivery_lane: light   # for this governance leaf
```
