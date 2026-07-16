---
id: BDD-ORCH-SPECIALIST-FALLBACK
title: Specialist runtime — retry then allowed GP downgrade
status: ready
date: 2026-07-16
bdd_readiness: ready
task_ids: [orch-specialist-retry-then-gp]
placement: MAIN
opt_out: main-only
supersedes: orch-specialist-retry-only (retry→BLOCKED; opt-in only GP)
---

# Specialist Runtime — Retry then GP Downgrade — BDD behavior spec

| Field | Value |
| --- | --- |
| **Slice** | `orch-specialist-retry-then-gp` (governance; MAIN / `main-only`) |
| **bdd_readiness** | **`ready`** |
| **Actor** | Parent agent / `delivery-orchestrator` |
| **Owning skill** | [`.cursor/skills/specialist-runtime-fallback/SKILL.md`](../../.cursor/skills/specialist-runtime-fallback/SKILL.md) |
| **Product E2E / UIUX / backend** | **`not-applicable`** |
| **User policy** | **Retry ≤3, then may auto-downgrade to GP** (confirmed 2026-07-16) |

---

## 1. Goal

When `Task` cannot run a project specialist, **retry the named specialist** (API flake)
up to **3** total attempts. After the retry budget is exhausted — or `ENUM_MISSING` is
confirmed — the parent/orchestrator **may** use `FALLBACK_GENERAL_PURPOSE` /
`INLINE_CHECKLIST` with **full contract injection**. Prefer native specialists whenever
the enum includes them. Do **not** stop forever at `BLOCKED` solely because GP needs a
fresh human `允许降级` each session.

User may still **forbid** downgrade in-session with `禁止降级` / `no-gp-fallback`
(then stay `BLOCKED` + recovery hints).

---

## 2. Confirmed decisions

### 2.1 Modes

| `runtime_routing.mode` | Meaning |
| --- | --- |
| `NATIVE_SPECIALIST` | Named Task succeeded |
| `RETRYING` | Mid retry loop (same `subagent_type`) |
| `BLOCKED` | Downgrade forbidden (`禁止降级` / `no-gp-fallback`), or unrecoverable stop |
| `FALLBACK_GENERAL_PURPOSE` | After retry budget / ENUM_MISSING confirm — GP under injected contract |
| `INLINE_CHECKLIST` | Same gate as GP for agents that document inline checklists |

### 2.2 Retry budget

- **API_UNAVAILABLE** / transport / timeout: up to **3** attempts total (1 initial + 2 retries)
  on the **same** `subagent_type`, then downgrade allowed.
- **ENUM_MISSING**: do not burn three identical doomed calls; **one** confirmation that the
  name is absent from the Task enum is enough to enter the post-retry downgrade path
  (`retry_count` may be `1`; `reason: ENUM_MISSING`).
- Record `retry_count` / `retry_attempted` in `runtime_routing`.

### 2.3 Default after failure

**`FALLBACK_GENERAL_PURPOSE` (or `INLINE_CHECKLIST`)** with contract injection — **not**
indefinite `BLOCKED` — unless the user said `禁止降级` / `no-gp-fallback`.

### 2.4 Honesty

Never claim the named specialist ran when GP/inline was used. Emit `runtime_routing`
with `actual_subagent: generalPurpose` (or `inline`) and accurate `reason`.

### 2.5 Lazy GP still forbidden

If the named specialist **is** in the Task enum, must use it — no convenience GP.

---

## 3. Acceptance scenarios

### SRF-01 — Native when enum present

**Given** Task enum includes `delivery-orchestrator`  
**When** parent starts deliver  
**Then** it uses `Task(subagent_type=delivery-orchestrator)`  
**And** does not use `generalPurpose`.

### SRF-02 — ENUM_MISSING → GP after confirm (auto downgrade)

**Given** Task enum lacks `delivery-orchestrator`  
**And** user has **not** said `禁止降级` / `no-gp-fallback`  
**When** parent would orchestrate deliver  
**Then** after confirming `ENUM_MISSING`, it may use `FALLBACK_GENERAL_PURPOSE`  
**And** injects `.cursor/agents/delivery-orchestrator.md` (+ skills / pipeline)  
**And** emits `runtime_routing` with `reason: ENUM_MISSING`  
**And** must not claim native specialist ran.

### SRF-03 — API_UNAVAILABLE → retry up to 3, then GP

**Given** named Task fails with transport/`ENOTFOUND`/`unavailable`  
**And** no `禁止降级` / `no-gp-fallback`  
**When** parent handles the failure  
**Then** it retries the **same** `subagent_type` until success or **3** attempts  
**And** if all fail → `mode: FALLBACK_GENERAL_PURPOSE`, `reason: API_UNAVAILABLE`, `retry_count: 3`  
**And** contract injection is mandatory.

### SRF-04 — Explicit forbid keeps BLOCKED

**Given** retries exhausted or ENUM_MISSING  
**And** user said `禁止降级` or `no-gp-fallback` in this session  
**When** parent continues  
**Then** `mode: BLOCKED`  
**And** it does **not** spawn `generalPurpose`  
**And** it surfaces recovery hints (new chat / reload / workspace root).

### SRF-05 — Early opt-in still allowed

**Given** user said `允许降级` / `allow-gp-fallback` before retry budget is spent  
**When** enum is missing or Task fails  
**Then** parent may downgrade immediately (budget not required)  
**And** `user_opt_in_gp: true`.

### SRF-06 — Lazy GP still forbidden

**Given** named specialist is in the enum  
**When** routing  
**Then** `generalPurpose` must not be used for convenience.

### SRF-07 — No fake Done

**Given** GP/inline mode  
**When** reporting  
**Then** delivery Done still requires real gates + doc-sync + commit-review  
**And** `runtime_routing` is honest.

### SRF-08 — Inline checklist after downgrade gate

**Given** downgrade is allowed (retry exhausted / ENUM_MISSING / early opt-in)  
**And** `worktree-router.md` documents inline checklist  
**When** stage 0 must run and enum lacks the agent  
**Then** `INLINE_CHECKLIST` is allowed  
**And** placement record is still emitted.

### SRF-09 — Built-ins unchanged

**Given** deep read-only audit  
**When** `explore` is available  
**Then** use `explore` (not this ladder).

### SRF-10 — Honesty block always present

**Given** any non-native mode  
**When** reporting  
**Then** `runtime_routing` is present and accurate.

---

## 4. Limits (confirmed facts)

- Retry **can** recover API flakes.
- Retry **cannot** create Task enum entries when Cursor did not load `.cursor/agents`.
- Auto GP after budget **unblocks** delivery under contract; it does **not** waive BDD/TDD/E2E/deploy/doc-sync.
- Platform injection of custom agents remains outside this repo’s control (worktree root
  orphan / missing `.cursor/agents` can still empty the enum until reload).

```
bdd_readiness: ready
task_ids: [orch-specialist-retry-then-gp]
open_questions: []
product_e2e_uiux_backend: not-applicable
```
