---
id: BDD-ORCH-SPECIALIST-FALLBACK
title: Specialist runtime fallback (Task enum / API)
status: ready
date: 2026-07-16
bdd_readiness: ready
task_ids: [orch-specialist-fallback]
placement: ISOLATED
worktree_path: D:/working/DGE-orch-specialist-fallback
branch: feat/orch-specialist-fallback
---

# Specialist Runtime Fallback — BDD behavior spec

| Field | Value |
| --- | --- |
| **Slice** | `orch-specialist-fallback` |
| **bdd_readiness** | **`ready`** |
| **Actor** | Parent agent / `delivery-orchestrator` |
| **Owning skill** | [`.cursor/skills/specialist-runtime-fallback/SKILL.md`](../../.cursor/skills/specialist-runtime-fallback/SKILL.md) |
| **Product E2E / UIUX / backend** | **`not-applicable`** |
| **CE-O01 / Task #81** | **Do not activate** |

---

## 1. Goal

When Cursor’s `Task` tool cannot run a **project** specialist named in
`.cursor/agents/*.md` (enum missing or API unavailable), the session must degrade in a
**documented, auditable** way — still obeying that specialist’s contract — instead of
silent improvisation or false “specialist succeeded” claims.

---

## 2. Confirmed decisions (v1)

### 2.1 Modes

| `runtime_routing.mode` | Meaning |
| --- | --- |
| `NATIVE_SPECIALIST` | `Task(subagent_type=<requested>)` succeeded |
| `FALLBACK_GENERAL_PURPOSE` | Named type unavailable; `generalPurpose` runs with contract injection |
| `INLINE_CHECKLIST` | Parent follows agent-file checklist (only when that agent documents inline fallback) |
| `BLOCKED` | Cannot proceed safely; stop and report |

### 2.2 Reasons

`ENUM_MISSING` | `API_UNAVAILABLE` | `TASK_REJECTED` | `NONE`

### 2.3 Ladder

Native → one retry on transport failure → GP fallback with contract injection →
documented inline checklist → `BLOCKED`.

### 2.4 Lazy GP still forbidden

If the Task enum **includes** the requested project specialist, using `generalPurpose`
instead is **forbidden**.

---

## 3. Acceptance scenarios

### SRF-01 — Native when enum present

**Given** Task enum includes `delivery-orchestrator`  
**When** parent starts deliver  
**Then** it invokes `Task(subagent_type=delivery-orchestrator)`  
**And** does not use `generalPurpose` for that stage  
**And** may record `mode: NATIVE_SPECIALIST`.

### SRF-02 — ENUM_MISSING → GP with contract

**Given** Task enum lacks `delivery-orchestrator` (and other project names)  
**When** parent must run deliver orchestration  
**Then** it emits `runtime_routing` with `mode: FALLBACK_GENERAL_PURPOSE`, `reason: ENUM_MISSING`  
**And** spawns `generalPurpose` whose prompt requires reading
`.cursor/agents/delivery-orchestrator.md` + pipeline/batch skills  
**And** still runs stage −1 Batch Recommendation before stage 0.

### SRF-03 — API_UNAVAILABLE → retry then GP

**Given** named `Task` fails with transport/`ENOTFOUND`/`unavailable`  
**When** parent handles the failure  
**Then** it retries the **same** `subagent_type` once  
**And** if still failing, uses `FALLBACK_GENERAL_PURPOSE` with `reason: API_UNAVAILABLE`  
**And** `retry_attempted: true`.

### SRF-04 — No false specialist claim

**Given** work completed via `FALLBACK_GENERAL_PURPOSE`  
**When** the session reports status  
**Then** it must **not** claim `Task(<named>)` succeeded  
**And** the user-visible note states fallback was used.

### SRF-05 — Lazy GP forbidden

**Given** Task enum includes `backend-engineer`  
**When** a backend slice is routed  
**Then** parent must not choose `generalPurpose` merely for convenience  
**And** must use `backend-engineer` (or orchestrator path that delegates to it).

### SRF-06 — Inline checklist only when documented

**Given** `worktree-router` is unavailable in the enum  
**And** `.cursor/agents/worktree-router.md` documents inline fallback  
**When** stage 0 must run  
**Then** parent may use `INLINE_CHECKLIST` and emit the same placement record  
**And** must not skip worktree creation.

### SRF-07 — Gates not skipped under fallback

**Given** any fallback mode for a delivery leaf  
**When** the leaf reaches Done criteria  
**Then** Batch Recommendation, worktree, required gates, merge, doc-sync, and commit-review
still apply per surface  
**And** fallback is not used as an excuse to skip them.

### SRF-08 — BLOCKED when unsafe

**Given** GP Task also fails and no documented inline checklist applies  
**When** continuing would require inventing gates or writing without isolation  
**Then** `mode: BLOCKED`  
**And** parent stops with a clear blocker (no fake Done).

### SRF-09 — Built-ins unchanged

**Given** a deep read-only audit request  
**When** `explore` is in the enum  
**Then** parent uses `explore` (not project-specialist fallback).

### SRF-10 — This slice stays solo; CE-O01 untouched

**Given** slice `orch-specialist-fallback` and parked CE-O01  
**When** Batch Recommendation runs  
**Then** `decision` is `solo`  
**And** CE-O01 / #81 are not activated.

---

## 4. Handoff fields

```
runtime_routing:
  mode: NATIVE_SPECIALIST | FALLBACK_GENERAL_PURPOSE | INLINE_CHECKLIST | BLOCKED
  requested_subagent: ...
  actual_subagent: ...
  reason: ENUM_MISSING | API_UNAVAILABLE | TASK_REJECTED | NONE
  contract_sources: [...]
  retry_attempted: true | false
  user_visible_note: ...
```

### This slice self-check

```
batch_recommendation:
  decision: solo
  member_task_ids: [orch-specialist-fallback]
  proposed_slice_id: orch-specialist-fallback
  vetoes_applied: [unrelated-parked-CE-O01]
runtime_routing:
  mode: INLINE_CHECKLIST
  requested_subagent: delivery-orchestrator
  actual_subagent: parent-inline
  reason: API_UNAVAILABLE
  retry_attempted: true
  user_visible_note: Specialist Task unavailable; parent landed governance docs in worktree per fallback skill.
```

---

## 5. Traceability

| Artifact | Role |
| --- | --- |
| This doc | Behavior SoT |
| `.cursor/skills/specialist-runtime-fallback/SKILL.md` | Runtime ladder |
| `.cursor/rules/subagent-routing-mandate.mdc` | Parent hard rule |
| `.cursor/agents/delivery-orchestrator.md` | Orchestrator consumer |
| `.cursor/skills/delivery-pipeline/SKILL.md` | Handoff payload |

```
bdd_readiness: ready
task_ids: [orch-specialist-fallback]
open_questions: []
product_e2e_uiux_backend: not-applicable
```
