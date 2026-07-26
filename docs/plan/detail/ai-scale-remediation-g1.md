# AI-SCALE Leaf 1 — governance module-map + light lane + soft budgets

**Program / slice:** `ai-scale-remediation-g1` (ad-hoc **NON-CE** AI operability remediation Leaf 1; **not** a formal P-phase; **not** CE-O02)  
**Program registry:** [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)  
**Formal plan phase:** **None** — single-active-phase discipline OK (does not occupy a P* slot)  
**Task Master:** **#166** (`AI-SCALE-G1` / `ai-scale-remediation-g1`) → **In Progress** (sole-active)  
**Queued peels (do not activate):** **#167** Leaf 2 TemplateImport* · **#168** Leaf 3 i18n locale split · **#169** Leaf 4 mega-test fixture split — all **pending**  
**Active delivery slice:** `ai-scale-remediation-g1`  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-ai-scale-remediation-g1` · branch `feat/ai-scale-remediation-g1` · base `origin/main` @ `df9a5b7d`  
**BDD:** [module-map-agent-retrieval.md](../../behavior/module-map-agent-retrieval.md) · [lightweight-delivery-lane.md](../../behavior/lightweight-delivery-lane.md) · [ai-scale-docs-conventions.md](../../behavior/ai-scale-docs-conventions.md) — all **ready**  
**Batch recommendation:** **solo** (`member_task_ids: ["166"]`; `proposed_slice_id: ai-scale-remediation-g1`;
`delivery_lane: light`;
vetoes_applied: `unrelated-code-peel`, `file-cap`, `risk-domain-split`) — **open** (Leaf 1)

**Prior (Done, do not reopen):** NON-CE **#165** `fix-abandoned-dev-blocks-clone` → **Done** (`c1bb6c77`)

---

## Purpose

Improve AI-agent operability at repository scale without inventing a formal P-phase:
module-map retrieval SoT, eligibility-gated lightweight delivery lane, soft size budgets
aligned with the quality-gate baseline, and progressive disclosure / archive of closed
programs for parent agents.

**Leaf 1 scope is docs/governance/scaffold only** — TemplateImport / i18n / mega-fixture
code peels stay queued as Leaves 2–4.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **#166** / `ai-scale-remediation-g1` |
| delivery_lane | **light** (product E2E/Docker **N/A**) |
| Umbrella #53 / #106 | Registry-only — **not** Done (veto held) |
| #119 | **Blocked**/pending — **not** activated |
| Queued Leaves 2–4 | **#167–#169** **pending** — do **not** fold into Leaf 1 |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; activate **#119**; invent P24+; implement TemplateImport / i18n / fixture peels in this leaf |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| AI-SCALE-G1-T01 | Plan/TM sole-active activation + program/detail/ledger/index cross-links; document queued Leaves 2–4 | **Done** |
| AI-SCALE-G1-T02 | Module map SoT (`docs/architecture/module-map.md`) + agent retrieval wiring (MM-01…08) | **Done** (docs scaffold; stage 3) |
| AI-SCALE-G1-T03 | Lightweight delivery lane wiring (`delivery_lane` light\|full) without weakening full product leaves (LDL-01…12) | **Done** (docs/skills/rules wiring; stage 3) |
| AI-SCALE-G1-T04 | Soft size budgets + progressive disclosure / archive conventions (ADC-01…08) | **Done** (docs/skills/rules + ARCHIVE; stage 3) |
| AI-SCALE-G1-T05 | Gates appropriate to light lane + merge + MAIN doc-sync | **Not Started** |

---

## Scope

| IN | OUT |
| --- | --- |
| Module map SoT + retrieval contract for agents | TemplateImport* code peel (**#167**) |
| Light delivery lane docs/skills/constitutions wiring | i18n `en.ts` / `zh-CN.ts` split (**#168**) |
| Soft budgets aligned with quality-gate-threshold-baseline | Mega-test fixture split (**#169**) |
| Progressive disclosure / archive closed programs | Product FE/BE behavior changes requiring full E2E/Docker |
| Worktree-isolated delivery (still mandatory) | Checklist **#3b** / **#5a** GO flips; CE-O02; #53/#106 Done; #119 activation |

---

## Queued peel leaves (serial, documented only)

| Leaf | TM | Suggested slice id | Status | Focus |
| --- | --- | --- | --- | --- |
| **2** | **#167** | `ai-scale-template-import-peel` | **pending** | TemplateImport* peel |
| **3** | **#168** | `ai-scale-i18n-locale-split` | **pending** | i18n locale split |
| **4** | **#169** | `ai-scale-mega-test-fixture-split` | **pending** | Mega-test fixture split |

Activate only after prior AI-SCALE leaf Done + sole-active cleared. Do **not** fold into #166.

---

## Traceability

- Program: [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)
- Behavior: [module-map-agent-retrieval.md](../../behavior/module-map-agent-retrieval.md) · [lightweight-delivery-lane.md](../../behavior/lightweight-delivery-lane.md) · [ai-scale-docs-conventions.md](../../behavior/ai-scale-docs-conventions.md)
- Soft budgets baseline: [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md)
- Batch recommend: [delivery-batch-recommend.md](../../behavior/delivery-batch-recommend.md)
