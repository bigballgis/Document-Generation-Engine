---
id: BDD-AI-SCALE-DOCS-CONV
title: AI-scale docs conventions — soft size budgets + progressive disclosure
status: ready
date: 2026-07-26
bdd_readiness: ready
task_ids: [166]
placement: ISOLATED
worktree_path: D:/working/DGE-ai-scale-remediation-g1
branch: feat/ai-scale-remediation-g1
slice: ai-scale-remediation-g1
user_confirmation: 2026-07-26 「按你的建议整改吧」
kind: docs-convention  # agent/doc-consumer observable; not product runtime
---

# AI-Scale Docs Conventions — Soft Size Budgets + Progressive Disclosure

| Field | Value |
| --- | --- |
| **Slice** | `ai-scale-remediation-g1` |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-26 |
| **Actor** | Parent agent / plan-orchestrator / doc-keeper / code-quality-reviewer |
| **Kind** | **Docs-convention with agent-observable acceptance** (not product UI/API) |
| **Size SoT** | [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) § Complexity and Size |
| **Review skill alignment** | [code-quality-review SKILL](../../.cursor/skills/code-quality-review/SKILL.md) soft budgets (warn/critical) |
| **Product E2E / Docker** | **`not-applicable`** |

---

## Classification

These are **governance conventions** for agents and documentation consumers. They do **not**
change bank OA product journeys. They **do** have Given/When/Then acceptance because parent
agents and reviewers must behave consistently when reading plans and peeling oversized files.

| Surface | Applicability |
| --- | --- |
| Product UI / runtime API | **not-applicable** |
| Agent plan reading / file peel guidance | **in scope** |
| CI hard-fail thresholds | Remain owned by quality-gate baseline (do not invent a second hard SoT) |

---

## 1. Soft size budgets

### 1.1 Goal

Keep agent attention and review load bounded by **soft** size budgets aligned with the
accepted quality-gate baseline, without claiming new CI hard blocks beyond that baseline.

### 1.2 Confirmed alignment

| Artifact | Soft target (align baseline) | Soft critical / hard-block reference |
| --- | --- | --- |
| Function length | ≤ 80 lines (baseline default target) | > 120 unless decomposition plan approved (baseline hard) |
| File length | ≤ 500 lines (baseline default target) | > 800 unless split plan approved (baseline hard) |
| Reviewer skill table | Warn/critical bands in code-quality-review skill remain **review signals** | Must not contradict baseline hard thresholds |

**Confirmed:**

1. Soft budgets are **guidance for agents and code-quality review**, not a license to
   ignore baseline hard thresholds.
2. New G1 docs must not invent a **stricter hard CI gate** than
   [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md)
   without an ADR / user confirmation.
3. When a file exceeds soft targets, agents **prefer peel / split plans** (queued as
   separate leaves) over silent growth — peels themselves are **out of this G1 leaf**.
4. Generated artifacts (e.g. OpenAPI client) stay **regen-only**; size flags apply to
   **manual** edits only (same as code-quality skill).

### 1.3 Actor / trigger

- **Trigger:** implementer or reviewer touches or creates a large source file; or plan
  work estimates a mega-file peel.
- **Actor:** implementers + `code-quality-reviewer` + parent when batching peels.

---

## 2. Progressive disclosure / archive closed programs

### 2.1 Goal

Parent agents start from **active** sources (`docs/README.md`, sole-active / queue head,
live program docs) and treat **closed** programs as archived history — linked, not
first-read dumps.

### 2.2 Confirmed decisions

1. **First read** for delivery: `docs/README.md` delivery focus + active plan/Task Master
   sole-active → then the owning behavior/plan for the queue head.
2. **Closed programs** (status Done / retired) remain reachable via indexes but should be
   labeled Done/archived in the index row; agents must **not** load entire closed-program
   histories as the default context for unrelated new work.
3. **Progressive disclosure:** summarize closed program pointers in one index line; open
   detail docs only when the task explicitly concerns that program’s residual or audit.
4. **Ledger:** `execution-sync-ledger.md` stays evidence SoT; agents read the **current**
   header / sole-active notes first, not the entire historical mid-file corpus, unless
   auditing a named past slice.
5. G1 may add archive/progressive-disclosure **guidance** for parent agents (skills /
   README notes) in later stages of this leaf; this BDD locks the expected agent behavior.

### 2.3 Actor / trigger

- **Trigger:** parent starts deliver / explore / plan for a new leaf.
- **Actor:** parent agent, plan-orchestrator, doc-keeper.

---

## 3. Boundary / exceptions

| Case | Expected behavior |
| --- | --- |
| Audit of a named historical slice | May open that slice’s detail + ledger note; still avoid unrelated closed programs |
| Soft budget exceeded in hotspot listed by skill | Flag in review; queue peel — do not block G1 docs leaf |
| Conflict between skill soft table and baseline hard | **Baseline hard wins**; update skill if drift is found |
| Product behavior change | Full delivery lane — see [lightweight-delivery-lane.md](./lightweight-delivery-lane.md) |

---

## 4. Observable evidence

| Evidence | Form |
| --- | --- |
| Budget SoT citation | Links to quality-gate-threshold-baseline § Complexity and Size |
| Agent first-read behavior | Routing/handoff cites active README/plan, not a closed-program dump |
| Archive labeling | Index rows for Done programs show Done/archived status |
| Product runtime | **None** |

---

## 5. Acceptance scenarios (Given / When / Then)

### ADC-01 — Soft budgets cite baseline

**Given** an agent or reviewer evaluates file/function size  
**When** applying soft budgets under AI-scale remediation  
**Then** targets align with quality-gate-threshold-baseline (≤80 / ≤500 soft; >120 / >800 hard references)  
**And** no conflicting harder CI policy is invented in G1 docs alone.

### ADC-02 — Over-soft-target prefers peel queue

**Given** a manually maintained source file exceeds the soft file target (≥500 lines)  
**And** growth is in-scope for remediation planning  
**When** agents plan follow-up work  
**Then** they recommend a **separate peel leaf** (not silent continued growth)  
**And** do not fold unrelated peels into the G1 docs leaf.

### ADC-03 — Review skill bands are signals

**Given** code-quality-review soft warn/critical bands differ in presentation from baseline  
**When** a review runs  
**Then** warn/critical bands may guide comments  
**And** baseline hard thresholds remain the blocking SoT for size exceptions.

### ADC-04 — Progressive disclosure on deliver

**Given** a parent agent starts a new unrelated delivery leaf  
**And** multiple historical programs are Done  
**When** gathering context  
**Then** it starts from `docs/README.md` + active/queue-head plan  
**And** does **not** dump full closed-program plan bodies into the working context by default.

### ADC-05 — Closed program remains reachable

**Given** a program is Done  
**When** a reader uses `docs/README.md` / plan indexes  
**Then** the program remains linked  
**And** the row/status communicates Done/archived  
**And** detail docs are available on demand.

### ADC-06 — Ledger read is header-first

**Given** an agent needs current execution truth  
**When** opening `execution-sync-ledger.md`  
**Then** it reads current sole-active / latest completion notes first  
**And** only deep-dives historical mid-file notes when the task names that slice.

### ADC-07 — Generated files excluded from manual size blame

**Given** `openapi-v1.ts` (or equivalent generated client) is large  
**When** size review runs  
**Then** size flags apply only if **manual** edits appear  
**And** regen-only growth is not treated as a peel defect by itself.

### ADC-08 — Docs-convention leaf evidence N/A for product E2E

**Given** only these conventions are in scope for G1  
**When** gate planning runs  
**Then** product E2E/Docker are N/A  
**And** acceptance is satisfied by persisted docs + index reachability.

---

## 6. Out of scope

- Performing TemplateImport / i18n / mega-test peels in this leaf
- Rewriting all closed program docs into an archive folder in G1 (guidance first;
  physical moves only if a later task explicitly scopes them)
- Changing JaCoCo / CI numeric floors

---

## 7. Traceability

| Source | Link |
| --- | --- |
| User confirmation | 2026-07-26 「按你的建议整改吧」 |
| Size baseline | [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) |
| Review skill | [code-quality-review SKILL](../../.cursor/skills/code-quality-review/SKILL.md) |
| Sibling G1 behaviors | [module-map-agent-retrieval.md](./module-map-agent-retrieval.md), [lightweight-delivery-lane.md](./lightweight-delivery-lane.md) |

```
bdd_readiness: ready
task_ids: [166]
open_questions: []
product_e2e_uiux_backend: not-applicable
frontend_ui_in_scope: false
kind: docs-convention
```
