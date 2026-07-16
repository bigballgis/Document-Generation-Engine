---
id: BDD-ORCH-SPECIALIST-FALLBACK
title: Specialist runtime — retry first (no auto GP downgrade)
status: ready
date: 2026-07-16
bdd_readiness: ready
task_ids: [orch-specialist-retry-only]
placement: ISOLATED
worktree_path: D:/working/DGE-orch-specialist-retry
branch: feat/orch-specialist-retry-only
supersedes: orch-specialist-fallback auto-GP policy (2026-07-16)
---

# Specialist Runtime — Retry First — BDD behavior spec

| Field | Value |
| --- | --- |
| **Slice** | `orch-specialist-retry-only` |
| **bdd_readiness** | **`ready`** |
| **Actor** | Parent agent / `delivery-orchestrator` |
| **Owning skill** | [`.cursor/skills/specialist-runtime-fallback/SKILL.md`](../../.cursor/skills/specialist-runtime-fallback/SKILL.md) |
| **Product E2E / UIUX / backend** | **`not-applicable`** |
| **User policy** | **Retry, do not auto-downgrade** (confirmed 2026-07-16) |

---

## 1. Goal

When `Task` cannot run a project specialist, **retry the named specialist** (API flake)
or **BLOCK with recovery hints** (enum missing / retries exhausted). Do **not**
automatically switch to `generalPurpose` unless the user explicitly opts in
(`allow-gp-fallback` / `允许降级`).

---

## 2. Confirmed decisions

### 2.1 Modes

| `runtime_routing.mode` | Meaning |
| --- | --- |
| `NATIVE_SPECIALIST` | Named Task succeeded |
| `RETRYING` | Mid retry loop (same `subagent_type`) |
| `BLOCKED` | Retries exhausted or ENUM_MISSING; stop; no auto GP |
| `FALLBACK_GENERAL_PURPOSE` | **Only** after user `allow-gp-fallback` / `允许降级` |
| `INLINE_CHECKLIST` | **Only** after same user opt-in (or agent-doc + opt-in) |

### 2.2 Retry budget

Up to **3** attempts total (1 initial + 2 retries) for `API_UNAVAILABLE` / transport errors.
`ENUM_MISSING` does not burn useless retries of the same missing name — go to `BLOCKED`
with recovery hints.

### 2.3 Default after failure

**`BLOCKED`**, not GP.

---

## 3. Acceptance scenarios

### SRF-01 — Native when enum present

**Given** Task enum includes `delivery-orchestrator`  
**When** parent starts deliver  
**Then** it uses `Task(subagent_type=delivery-orchestrator)`  
**And** does not use `generalPurpose`.

### SRF-02 — ENUM_MISSING → BLOCKED (no auto GP)

**Given** Task enum lacks `delivery-orchestrator`  
**And** user has **not** said `允许降级` / `allow-gp-fallback`  
**When** parent would orchestrate deliver  
**Then** `mode: BLOCKED`, `reason: ENUM_MISSING`  
**And** it does **not** spawn `generalPurpose`  
**And** it surfaces recovery hints (new chat / reload / workspace root / opt-in).

### SRF-03 — API_UNAVAILABLE → retry up to 3, then BLOCKED

**Given** named Task fails with transport/`ENOTFOUND`/`unavailable`  
**And** no user GP opt-in  
**When** parent handles the failure  
**Then** it retries the **same** `subagent_type` until success or **3** attempts  
**And** if all fail → `mode: BLOCKED`, `reason: API_UNAVAILABLE`, `retry_count: 3`  
**And** it does **not** auto-switch to `generalPurpose`.

### SRF-04 — User opt-in enables GP

**Given** retries exhausted or ENUM_MISSING  
**And** user said `允许降级` or `allow-gp-fallback` in this session  
**When** parent continues  
**Then** it may use `FALLBACK_GENERAL_PURPOSE` with contract injection  
**And** `user_opt_in_gp: true`  
**And** must not claim named specialist ran.

### SRF-05 — Lazy GP still forbidden

**Given** named specialist is in the enum  
**When** routing  
**Then** `generalPurpose` must not be used for convenience.

### SRF-06 — No fake Done while BLOCKED

**Given** `mode: BLOCKED`  
**When** reporting to the user  
**Then** delivery is not claimed Done  
**And** no skip of gates “to finish somehow”.

### SRF-07 — Opt-in inline checklist

**Given** user opted in to downgrade  
**And** `worktree-router.md` documents inline checklist  
**When** stage 0 must run and enum lacks the agent  
**Then** `INLINE_CHECKLIST` is allowed  
**And** placement record is still emitted.

### SRF-08 — Built-ins unchanged

**Given** deep read-only audit  
**When** `explore` is available  
**Then** use `explore` (not this ladder).

### SRF-09 — Honesty

**Given** any non-native mode  
**When** reporting  
**Then** `runtime_routing` is present and accurate.

### SRF-10 — CE-O01 untouched

**Given** this governance slice  
**When** Batch Recommendation runs  
**Then** `solo`; do not activate #81 CE-O01.

---

## 4. Limits (confirmed facts)

- Retry **can** recover API flakes.
- Retry **cannot** create Task enum entries when Cursor did not load `.cursor/agents`.
- Platform injection of custom agents is outside this repo’s control.

```
bdd_readiness: ready
task_ids: [orch-specialist-retry-only]
open_questions: []
product_e2e_uiux_backend: not-applicable
```
