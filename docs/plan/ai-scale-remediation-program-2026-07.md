# AI-Scale Remediation Program — 2026-07

| Field | Value |
| --- | --- |
| **Program ID** | `ai-scale-remediation-2026-07` (short: **AI-SCALE**) |
| **Created** | 2026-07-26 |
| **Status** | **In Progress** (Leaf 1 sole-active; Leaves 2–4 queued pending) |
| **Formal phase** | **None** (NON-CE agent/docs operability program — not a P-phase) |
| **Sole-active leaf** | **#166** · `ai-scale-remediation-g1` · **In Progress** |
| **Leaf 1 (active)** | TM **#166** · `ai-scale-remediation-g1` → **In Progress** (ISOLATED `D:/working/DGE-ai-scale-remediation-g1` · `feat/ai-scale-remediation-g1`) |
| **Leaf 2 (queued)** | TM **#167** · TemplateImport* peel → **pending** (do **not** activate) |
| **Leaf 3 (queued)** | TM **#168** · i18n `en.ts` / `zh-CN.ts` split → **pending** (do **not** activate) |
| **Leaf 4 (queued)** | TM **#169** · mega-test fixture split → **pending** (do **not** activate) |
| **Batch (Leaf 1)** | **solo** · `member_task_ids: ["166"]` · `proposed_slice_id: ai-scale-remediation-g1` · vetoes: `unrelated-code-peel`, `file-cap`, `risk-domain-split` |
| **delivery_lane (Leaf 1)** | **light** (docs/governance/scaffold; product E2E/Docker **N/A**) |
| **Behavior SoT (G1)** | [module-map-agent-retrieval.md](../behavior/module-map-agent-retrieval.md) · [lightweight-delivery-lane.md](../behavior/lightweight-delivery-lane.md) · [ai-scale-docs-conventions.md](../behavior/ai-scale-docs-conventions.md) — all **ready** |
| **Detail plan (Leaf 1)** | [detail/ai-scale-remediation-g1.md](./detail/ai-scale-remediation-g1.md) |
| **Next queue head** | After Leaf 1 Done → TM **#167** (not auto-activated) |
| **Upstream** | User confirmation 2026-07-26「按你的建议整改吧»; formal phase remains **None**; do **not** reopen PQH / SYS-NORM waves |

---

## 1. North star

Improve **AI-agent operability** at repository scale without inventing a formal P-phase:
module-map retrieval SoT, eligibility-gated lightweight delivery lane, soft size budgets
aligned with the quality-gate baseline, and progressive disclosure / archive of closed
programs so parent agents stay within attention budgets.

**Leaf 1 (active):** governance + docs scaffold only — **no** TemplateImport / i18n /
mega-fixture peels.

**Leaves 2–4 (queued, documented only):** serial code peels deferred by Batch
Recommendation vetoes (`unrelated-code-peel`, `file-cap`, `risk-domain-split`).

---

## 2. Serial queue

| Leaf | Slice id | TM | Status | Focus |
| --- | --- | --- | --- | --- |
| **Leaf 1** | `ai-scale-remediation-g1` | **#166** | **In Progress** | Module map SoT + light lane + soft budgets + progressive disclosure |
| **Leaf 2** | TBD (`ai-scale-template-import-peel`) | **#167** | **pending** | TemplateImport* peel |
| **Leaf 3** | TBD (`ai-scale-i18n-locale-split`) | **#168** | **pending** | i18n `en.ts` / `zh-CN.ts` split |
| **Leaf 4** | TBD (`ai-scale-mega-test-fixture-split`) | **#169** | **pending** | Mega-test fixture split |

**Rule:** At most **one** AI-SCALE delivery leaf sole-active at a time. Do **not** fold
queued peels into Leaf 1.

---

## 3. Vetoes (hard)

| Veto | Rule |
| --- | --- |
| unrelated-code-peel | Do not merge TemplateImport / i18n / fixture peels into G1 governance leaf |
| file-cap | Peel oversized surfaces in dedicated later leaves, not as drive-by in G1 |
| risk-domain-split | Keep docs/governance light lane separate from product FE/BE behavior peels |
| checklist-#3b/#5a-GO | Never flip **#3b** / **#5a** to GO from this program |
| CE-O02 | Never activate CE-O02 |
| mark-#53-CE-Done | Never mark umbrella **#53** Done |
| mark-#106-IBL-Done | Never mark umbrella **#106** Done |
| activate-#119-Word-host | Never activate **#119** without licensed Word host |
| invent-P-phase | Formal phase stays **None** — do not invent P24+ for AI-SCALE |

---

## 4. Task Master map

| Alias | TM | Status | Notes |
| --- | --- | --- | --- |
| **AI-SCALE-G1** | **#166** | **in-progress** | Sole-active host delivery leaf |
| **AI-SCALE-L2** | **#167** | **pending** | TemplateImport* peel — queued only |
| **AI-SCALE-L3** | **#168** | **pending** | i18n locale split — queued only |
| **AI-SCALE-L4** | **#169** | **pending** | Mega-test fixture split — queued only |

**Sole-active statement:** Host delivery sole-active = TM **#166** /
`ai-scale-remediation-g1`. Umbrella **#53** stays **in-progress** registry-only (not a
delivery leaf). **#106** stays **pending** registry-only. **#119** stays Blocked/pending.

---

## 5. Formal phase invariant

**Formal phase remains None.** AI-SCALE is a NON-CE program registry (same pattern as
PQH / SYS-NORM residuals), not a sole-active P-phase. Do not invent `In Progress` rows
on P0–P23 for this work.

---

## 6. Relation to other programs

| Program | Relation |
| --- | --- |
| PQH / SYS-NORM | Closed — do **not** reopen |
| CE (#53) | Registry-only umbrella — do **not** treat as delivery leaf or mark Done |
| IBL (#106 / #119) | Outside AI-SCALE; #119 stays Blocked |
| ORCH light-lane / batch-recommend | Leaf 1 wires eligibility; does not weaken full product leaves |

---

## 7. Activation log

| Date | Event |
| --- | --- |
| 2026-07-26 | Program registered; Leaf 1 TM **#166** → **in-progress** sole-active; Leaves **#167–#169** queued **pending** |
