# CD-HARD-T06 — Audit list/export E2E re-evidence (wave closeout pointer)

**文件状态:** `ready`（指针；不新增产品行为）  
**Task:** CD-HARD-T06 / Task Master **#140** / slice `cdp-cd3-hard-t06`  
**Wave:** CD-3 hardening closeout  
**编写日期:** 2026-07-20

---

## Purpose

本文件是 **CD-3 / CD-HARD-T06** 的波次收口指针，声明：本切片对 **CD-BDD-T08** 既有规格做 **re-evidence / residual closeout**，**不**扩展 Activity log 查询/导出产品范围。

**Canonical behavior (as-is):** [audit-admin-query-journey.md](./audit-admin-query-journey.md)

| Item | Value |
| --- | --- |
| BDD readiness | **`ready`** (reuse existing; no new scenarios) |
| Scenario IDs | `BDD-CDP-AUDIT-001`, `BDD-CDP-AUDIT-002` |
| Historical E2E | CD-E2E-T11 **Done** (merge `6e3f825`) — not greenfield |
| `frontend_ui_in_scope` | **true** (Playwright E2E + UIUX mandatory) |

---

## Scope lock

- **In scope:** Re-prove filter Apply + Export download on Docker `:4173` for `AUDIT_ADMIN`; UIUX evidence as needed for T06/CD-3 exit.
- **Out of scope:** New audit governance UI; GROUP_ADMIN scope matrix; checklist **#3b/#5a GO** flips; inventing scenarios beyond BDD-CDP-AUDIT-001/002.

---

## Next

`plan-orchestrator` → `e2e-test-engineer` (+ `e2e-uiux-reviewer`) — reuse/extend `CDP-E2E-T11-audit-query.spec.ts` (or CD-HARD-T06 harness) against the canonical scenarios above.
