---
id: BDD-ORCH-BATCH-RECOMMEND
title: Pre-stage-0 Batch Recommendation (delivery-orchestrator)
status: ready
date: 2026-07-16
bdd_readiness: ready
task_ids: [orch-batch-recommend]
placement: ISOLATED
worktree_path: D:/working/DGE-orch-batch-recommend
branch: feat/orch-batch-recommend
---

# Delivery Batch Recommendation — BDD behavior spec

| Field | Value |
| --- | --- |
| **Slice** | `orch-batch-recommend` |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-16 |
| **Actor** | `delivery-orchestrator` / parent agent (**not** end user) |
| **Owning skill** | [`.cursor/skills/delivery-batch-recommend/SKILL.md`](../../.cursor/skills/delivery-batch-recommend/SKILL.md) |
| **Pipeline stage** | **Pre-stage-0** (before `worktree-router`) |
| **Product E2E / UIUX / backend code** | **`not-applicable`** |
| **Formal phase / CE-O01 / Task #81** | **Do not activate** |

---

## Classification

This slice defines **agent-governance behavior**: how the delivery orchestrator (and parent
session acting as orchestrator) decides whether related Task Master / plan tasks should be
delivered as **one leaf** (`merge`), kept separate (`solo`), or broken apart (`split`) so
fixed pipeline cost (worktree → gates → merge → doc-sync → commit) is amortized without
violating single-lane serial delivery.

| Surface | Applicability |
| --- | --- |
| Product management UI / Playwright E2E | **not-applicable** |
| Product UIUX review | **not-applicable** |
| Backend Java / Flyway / OpenAPI behavior | **not-applicable** |
| Agent / skill / constitution / handoff docs | **in scope** |
| Evidence amortization for this slice | **doc-sync + commit only** (no product deploy/E2E) |

Analogous governance precedent: [cursor-scaffold-hygiene.md](./cursor-scaffold-hygiene.md)
(agent-system hygiene). Unlike that slice (`bdd_readiness: not-applicable`), this slice
**does** require Given/When/Then because the orchestrator’s batch decision is a
testable contract for specialists and parent routing.

---

## 1. Actor / role

| Actor | Role |
| --- | --- |
| **delivery-orchestrator** | Primary decision owner for Batch Recommendation before stage 0 |
| **Parent agent** | Same contract when auto-mapping deliver intent; must emit the same placement record |
| **plan-orchestrator** | Consumer of the decision (`member_task_ids`, `proposed_slice_id`) — does not invent a second batch policy |
| **worktree-router** | Consumer — provisions **one** worktree for the chosen leaf only |
| **End user (bank OA)** | **Out of scope** — not an actor for this behavior |

---

## 2. Goal

1. Before stage 0, classify related pending tasks into **exactly one** of: `merge` | `solo` | `split`.
2. Amortize fixed pipeline cost across tasks that share an acceptance surface — **without**
   spawning multi-writer parallel leaves.
3. Preserve **single-lane serial** (at most one delivery leaf In Progress on this host).
4. Emit a durable **Batch Recommendation** record (handoff fields) so implementers and
   reviewers can audit the decision.
5. Prefer small, reversible batches (DORA-aligned): amortize **evidence**, do not blur BDD.

---

## 3. Trigger

| # | Trigger |
| --- | --- |
| T1 | User (or parent) starts **deliver** with multiple related `task_ids` / queue heads |
| T2 | Orchestrator discovers neighbor Task Master / plan tasks that share acceptance surface |
| T3 | Orchestrator re-evaluates after a red gate with `on_red_split_hint` present |
| T4 | User asks to “continue / 把剩下的做完” with several Not Started siblings in one epic |

**Non-triggers (must not batch):**

- Deploy-only / verify-done intents
- `force-parallel` multitask (separate opt-in path — not Batch Recommendation)
- Pure read-only Q&A

---

## 4. Preconditions

- Delivery intent is classified (`deliver` / serial queue head).
- Candidate tasks are known (Task Master ids and/or plan task ids).
- Stage 0 has **not** yet provisioned a worktree for the candidate set.
- Host single-lane rule is in force unless user said `force-parallel` / `强制并行`
  (parallel writers are **out of scope** for this skill — Batch Recommendation never means
  two writers).
- If any candidate has unclear acceptance → **BDD-first** (stage 1) before batching
  (see DBR-08).

---

## 5. Primary journey

1. Orchestrator collects candidate `task_ids` and shared acceptance surface hints.
2. Apply **vetoes** (must-solo) → if any veto fires for a pair, those tasks cannot `merge`.
3. Apply **must-merge** affinity (same tiny docs leaf, identical surface) when no veto.
4. Apply **may-merge** heuristics within hard caps.
5. Emit decision `merge` | `solo` | `split` with rationale, `member_task_ids`,
   `proposed_slice_id`, `shared_acceptance_surface`, `vetoes_applied`,
   `evidence_amortization`, `on_red_split_hint`.
6. Proceed to stage 0 **once** for the chosen leaf (or queue splits serially).

---

## 6. Confirmed decisions (v1)

### 6.1 Decision enum

| Decision | Meaning |
| --- | --- |
| **`merge`** | Multiple member tasks → **one** delivery leaf / one worktree / one pipeline pass |
| **`solo`** | Exactly one member task → one leaf (default when no safe affinity) |
| **`split`** | Candidate set too large / multi-domain / veto-crossing → break into ordered solo (or smaller merge) leaves; still **serial**, never parallel writers |

### 6.2 Affinity classes

| Class | Rule |
| --- | --- |
| **must-merge** | Same governance/docs leaf with identical acceptance surface and no veto (e.g. one skill + its constitution one-liner + index link that only make sense together) |
| **may-merge** | Related product/docs tasks sharing one acceptance surface, within hard caps, no veto |
| **must-solo / veto** | Any veto below → refuse merge for that pair/set |

### 6.3 Hard caps (v1)

| Cap | Rule |
| --- | --- |
| Members | **≤ 3** `member_task_ids` per merged leaf |
| Module domains | **≤ 2** distinct module domains (e.g. `backend`+`frontend` OK; `backend`+`frontend`+`rendering` → prefer `split`) |
| File blast radius | Estimated touch set **> 25 files** → **prefer `split`** (may still merge only with explicit user override in-session) |

### 6.4 Vetoes (must-solo) — MinimumCD-style, project-adapted

| Veto id | Condition |
| --- | --- |
| `unrelated-parked-leaf` | Candidate belongs to a **parked** / unrelated worktree or CE leaf (e.g. parked CE-O01) |
| `sole-active-collision` | Attempt to merge work **into** an already sole-active In Progress leaf |
| `unclear-behavior` | Any member lacks confirmed BDD / acceptance → BDD first, not batch |
| `cross-permission-or-security` | Permission-matrix / authz fail-closed surface mixed with unrelated UX churn |
| `conflicting-acceptance-surface` | Members do not share deploy/E2E/doc evidence surface |
| `user-force-solo` | User same-session: keep tasks separate |
| `mega-pr-trap` | Merge would create a review-hostile mega-diff without shared surface (see §7) |

### 6.5 Never merge into sole-active leaf

If leaf **A** is already In Progress (worktree + pipeline running), Batch Recommendation
**must not** add tasks into A. Queue candidates for the **next** serial leaf after stage 11
merge + MAIN doc-sync of A.

### 6.6 Unclear behavior → BDD first

If behavior/acceptance is unclear for any candidate: invoke `behavior-spec-author`
(stage 1 path) / block with open questions. **Do not** use batching to paper over missing
BDD. Batch Recommendation runs on **ready** (or explicitly `not-applicable`) members only.

### 6.7 `on_red_split_hint`

When decision is `merge`, the record **should** include `on_red_split_hint`: how to split
members if gates go red (which task keeps the worktree, which re-queues solo). On red,
orchestrator **may** re-decide `split` without user permission-polling when the hint applies.

### 6.8 Evidence amortization (not fuzzy BDD)

**Confirmed:** “Small batch” (DORA) here means amortizing **fixed pipeline evidence**
(worktree, verify, queued deploy, E2E once per shared surface) — **not** bundling
unrelated Given/When/Then into one fuzzy behavior spec.

| Shared surface examples | Evidence that may be amortized |
| --- | --- |
| Same UI journey | One E2E + UIUX pass |
| Same API contract slice | One `mvn verify` + contract tests |
| Agent/skill/constitution docs | Doc-sync + commit only |

### 6.9 Out of scope for v1 (Future — do not implement)

| Future idea | v1 stance |
| --- | --- |
| Selective / partial test execution based on batch membership | **NOT in v1** — always run the normal gates required by the leaf’s surfaces |
| Multi-writer parallel as a “batch” | **Forbidden** — use `force-parallel` skill only when user opts in; still ≠ Batch Recommendation |
| Auto-merging into parked CE-O01 / Task #81 | **Forbidden** this slice |

---

## 7. Research synthesis (project-adapted)

These principles are **confirmed requirements** for the skill and orchestrator contract
(adapted to this repo’s single Docker host + single-lane serial model):

| Theme | Project rule |
| --- | --- |
| **PR trap** | Do not merge tasks merely to “save” pipeline stages if the result is an unreviewable mega-PR / mega-diff. Prefer `solo`/`split` when blast radius or domains exceed caps. |
| **Merge-queue amortize evidence** | When members share one acceptance surface, one leaf may run gates/deploy/E2E **once** for the batch — analogous to merge-queue CI amortization, not parallel CI fans. |
| **MinimumCD vetoes** | Fail-closed veto list (§6.4); when unsure, **solo**. |
| **DORA small batch** | Keep batches small and releasable; “small” = clear evidence boundary, not fuzzy multi-BDD soup. |
| **Future selective tests** | Documented as **Future / NOT in v1** — do not invent partial-gate shortcuts in the skill. |

---

## 8. System responses

### Success

- Emit Batch Recommendation block in handoff (see §10).
- Route **one** leaf through stage 0 → … → 13.
- State routing line:
  `[routing] intent=deliver → batch=<merge|solo|split> → slice=<proposed_slice_id>`.

### Fail-closed / refuse

- Refuse `merge` on any veto.
- Refuse batching when BDD unclear (`blocked` until confirmed).
- Refuse interpreting Batch Recommendation as multi-writer parallel.
- Refuse activating CE-O01 / Task #81 as a side effect of this governance slice.

---

## 9. Boundary and exception behavior

| Case | Expected behavior |
| --- | --- |
| Exactly one candidate | Decision **`solo`**; still emit the record |
| Three related UI tasks, one surface, ≤25 files | **`may-merge`** → `merge` allowed |
| Four related tasks | **`split`** (cap ≤3) into serial leaves |
| Backend + frontend + rendering | Prefer **`split`** (domain cap) |
| Active leaf In Progress + new siblings | Queue; **never** merge into active leaf |
| User says `force-parallel` | **Out of scope** for this skill — hand off to parallel skill; do not call that a batch merge |
| Red gates mid-merge leaf | Apply `on_red_split_hint`; re-queue remaining members solo |
| Parked unrelated CE (e.g. CE-O01) | Veto `unrelated-parked-leaf` |

---

## 10. Observable evidence

| Evidence | Form |
| --- | --- |
| Placement / handoff record | Fields in Task handoff payload (below) |
| Skill existence | `.cursor/skills/delivery-batch-recommend/SKILL.md` (implementer next) |
| Constitution / orchestrator pointer | Delivery pipeline / orchestrator docs reference pre-stage-0 batch step |
| This BDD | `docs/behavior/delivery-batch-recommend.md` |
| Product runtime | **None required** |

### Required handoff fields (Batch Recommendation)

```
batch_recommendation:
  decision: merge | solo | split
  rationale: <string>
  member_task_ids: [...]
  proposed_slice_id: <string>
  shared_acceptance_surface: <string>
  vetoes_applied: [...]
  evidence_amortization: <string>
  on_red_split_hint: <string>   # required when decision=merge; N/A when solo
```

### This slice’s own recommendation (meta, confirmed)

```
batch_recommendation:
  decision: solo
  rationale: Single governance/docs leaf; unrelated to parked CE-O01; no neighbor product tasks to merge.
  member_task_ids: [orch-batch-recommend]
  proposed_slice_id: orch-batch-recommend
  shared_acceptance_surface: agent/skill/constitution docs
  vetoes_applied: [unrelated-parked-CE-O01]
  evidence_amortization: doc-sync+commit only (no product E2E/deploy)
  on_red_split_hint: N/A
```

---

## 11. Acceptance scenarios (Given / When / Then)

### DBR-01 — Must-merge affinity yields `merge`

**Given** two Not Started governance tasks that only make sense together  
(e.g. new skill file + mandatory index/constitution one-liner for the same surface)  
**And** no veto applies  
**And** member count ≤ 3, domains ≤ 2, estimated files ≤ 25  
**When** the orchestrator runs Batch Recommendation before stage 0  
**Then** `decision` is `merge`  
**And** `member_task_ids` lists both tasks  
**And** exactly one `proposed_slice_id` / one worktree is planned  
**And** `evidence_amortization` names the shared docs surface.

### DBR-02 — May-merge within caps

**Given** two or three related product tasks sharing one acceptance surface  
**And** hard caps are satisfied  
**And** no veto applies  
**When** Batch Recommendation runs  
**Then** the orchestrator **may** choose `merge` with explicit rationale  
**Or** choose `solo` with rationale (may-merge is optional, never mandatory)  
**And** either way the decision record is emitted.

### DBR-03 — Must-solo / veto refuses merge

**Given** two candidates where at least one veto applies  
(e.g. `unrelated-parked-leaf`, `conflicting-acceptance-surface`, `mega-pr-trap`)  
**When** Batch Recommendation runs  
**Then** `decision` is `solo` or `split` (not `merge` for the vetoed pairing)  
**And** `vetoes_applied` lists the veto id(s).

### DBR-04 — Hard cap: member count > 3 → `split`

**Given** four or more related candidates with shared surface  
**When** Batch Recommendation runs  
**Then** `decision` is `split`  
**And** no single leaf lists more than **3** `member_task_ids`.

### DBR-05 — Hard cap: module domains > 2 → prefer `split`

**Given** candidates spanning more than **two** module domains  
(e.g. backend + frontend + rendering)  
**When** Batch Recommendation runs  
**Then** `decision` is `split` (or user in-session override is recorded)  
**And** rationale cites the domain cap.

### DBR-06 — Hard cap: > 25 files prefer `split`

**Given** a candidate merge whose estimated touch set exceeds **25** files  
**When** Batch Recommendation runs  
**Then** the default decision is `split` (or `solo` members)  
**And** `merge` occurs only if the user explicitly overrides in the same session  
**And** the override is recorded in `rationale`.

### DBR-07 — Never merge into sole-active leaf

**Given** leaf A is sole-active In Progress (worktree + pipeline)  
**And** related tasks B, C are queued  
**When** Batch Recommendation evaluates B/C  
**Then** they are **not** merged into A  
**And** they are queued for a **subsequent** serial leaf after A’s stage 11 + MAIN doc-sync  
**And** veto `sole-active-collision` is applied if a merge-into-A was proposed.

### DBR-08 — Unclear behavior → BDD first, not batch

**Given** at least one candidate has unclear acceptance / missing BDD  
**When** the orchestrator considers batching  
**Then** it does **not** emit `merge` for that set  
**And** it routes to `behavior-spec-author` (or returns `bdd_readiness: blocked`)  
**And** Batch Recommendation resumes only after members are `ready` or `not-applicable`.

### DBR-09 — Decision enum completeness

**Given** any Batch Recommendation invocation with known candidates  
**When** the decision is produced  
**Then** `decision` is exactly one of `merge` | `solo` | `split`  
**And** all required handoff fields in §10 are present  
**And** for `merge`, `on_red_split_hint` is non-empty.

### DBR-10 — `on_red_split_hint` applied after red gates

**Given** a `merge` leaf with `on_red_split_hint` describing how to peel members  
**And** a quality gate fails mid-pipeline  
**When** the orchestrator re-evaluates  
**Then** it may re-decide `split` per the hint without asking permission menus  
**And** remaining members are re-queued as serial solo (or smaller) leaves  
**And** single-lane serial is preserved (no second writer).

### DBR-11 — PR trap (refuse mega-merge)

**Given** candidates that would produce a review-hostile mega-diff without a true shared surface  
**When** Batch Recommendation runs  
**Then** veto `mega-pr-trap` applies  
**And** `decision` is `solo` or `split`  
**And** rationale cites reviewability / PR trap.

### DBR-12 — Merge-queue-style evidence amortization

**Given** a valid `merge` with `shared_acceptance_surface` = one UI journey (or one API surface)  
**When** the leaf runs stages 5–7 / verify / deploy as required by that surface  
**Then** amortized evidence is collected **once** for the batch  
**And** doc-sync records member tasks Done against that shared evidence  
**And** the orchestrator does **not** spawn parallel deploy stacks.

### DBR-13 — DORA small batch ≠ fuzzy BDD bundling

**Given** two tasks with **distinct** behavior specs / acceptance journeys  
**When** Batch Recommendation is tempted to merge them only to “save BDD authoring”  
**Then** merge is refused unless they truly share one acceptance surface  
**And** each distinct behavior remains a separate spec (no fuzzy combined BDD)  
**And** rationale distinguishes evidence amortization from BDD bundling.

### DBR-14 — Future selective tests not in v1

**Given** a merged leaf that touches only a subset of modules  
**When** gates are selected  
**Then** v1 still runs the **normal** gate set required by the leaf’s surfaces  
**And** the skill/docs mark “selective tests” as **Future / NOT in v1**  
**And** no implementer invents partial-skip as Batch Recommendation behavior.

### DBR-15 — Single-lane serial preserved (not multi-writer)

**Given** any Batch Recommendation decision (`merge` | `solo` | `split`)  
**When** delivery proceeds  
**Then** at most **one** writer pipeline is In Progress  
**And** `merge` means multiple task ids in **one** leaf — **not** two worktrees writing in parallel  
**And** `split` means ordered serial leaves, not fan-out.

### DBR-16 — This governance slice stays solo; CE-O01 untouched

**Given** slice `orch-batch-recommend` and a parked unrelated CE-O01 / Task #81  
**When** Batch Recommendation runs for this governance work  
**Then** `decision` is `solo`  
**And** `vetoes_applied` includes `unrelated-parked-CE-O01` (or equivalent)  
**And** CE-O01 / #81 are **not** activated or merged.

---

## 12. Stage Done definition (this authoring slice)

| Check | Required |
| --- | --- |
| This file persisted at `docs/behavior/delivery-batch-recommend.md` | Yes |
| Scenario IDs DBR-01 … DBR-16 authored | Yes |
| `bdd_readiness: ready` | Yes |
| Product E2E / UIUX / backend implementation | **not-applicable** |
| Skill file | Next implementer (traceability target exists as path) |
| Activate CE-O01 / #81 | **No** |

---

## 13. Traceability

| Artifact | Role |
| --- | --- |
| **This doc** | Confirmed orchestrator Batch Recommendation behavior (SoT for TDD of skill/docs) |
| `.cursor/skills/delivery-batch-recommend/SKILL.md` | Implementer skill (create next; must match DBR-*) |
| `.cursor/skills/delivery-pipeline/SKILL.md` | Stage numbers; Batch Recommendation is **pre-stage-0** |
| `.cursor/agents/delivery-orchestrator.md` | Consumer of the decision |
| `.cursor/rules/delivery-orchestration-constitution.mdc` | Single-lane serial constitution |
| `.cursor/rules/worktree-and-deploy-queue-constitution.mdc` | One worktree per active leaf; queued Docker |
| `.cursor/skills/cursor-native-parallel/SKILL.md` | Opt-in parallel — **not** Batch Recommendation |
| Task / slice id | `orch-batch-recommend` |
| CE-O01 / Task #81 | **Explicitly out of scope — do not activate** |

```
bdd_readiness: ready
task_ids: [orch-batch-recommend]
owning_doc: docs/behavior/delivery-batch-recommend.md
open_questions: []
product_e2e_uiux_backend: not-applicable
```
