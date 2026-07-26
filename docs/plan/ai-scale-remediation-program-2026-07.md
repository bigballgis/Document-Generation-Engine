# AI-Scale Remediation Program — 2026-07

| Field | Value |
| --- | --- |
| **Program ID** | `ai-scale-remediation-2026-07` (short: **AI-SCALE**) |
| **Created** | 2026-07-26 |
| **Status** | **In Progress** (Leaf 1–2 **Done**; Leaves 3–4 queued **pending**) |
| **Formal phase** | **None** (NON-CE agent/docs operability program — not a P-phase) |
| **Sole-active leaf** | **cleared** (no delivery leaf In Progress) |
| **Leaf 1 (Done)** | TM **#166** · `ai-scale-remediation-g1` → **Done** (`c4f1b0d4`; worktree **REMOVED**) |
| **Leaf 2 (Done)** | TM **#167** · `ai-scale-peel-template-import` → **Done** (`d02aa414` / feat `1f55a420`; worktree **REMOVED**) |
| **Leaf 3 (queued)** | TM **#168** · i18n `en.ts` / `zh-CN.ts` split → **pending** (do **not** activate) |
| **Leaf 4 (queued)** | TM **#169** · mega-test fixture split → **pending** (do **not** activate) |
| **Batch (Leaf 1)** | **solo** · `member_task_ids: ["166"]` · `proposed_slice_id: ai-scale-remediation-g1` · vetoes: `unrelated-code-peel`, `file-cap`, `risk-domain-split` — **closed** |
| **Batch (Leaf 2)** | **solo** · `member_task_ids: ["167"]` · `proposed_slice_id: ai-scale-peel-template-import` · `delivery_lane: full` · vetoes: `unrelated-frontend-i18n`, `mega-test-split` — **closed** |
| **delivery_lane (Leaf 1)** | **light** (docs/governance/scaffold; product E2E/Docker **N/A**) |
| **delivery_lane (Leaf 2)** | **full** (BE verify required; stages **5–7**/10 **N/A** — zero FE + zero OpenAPI/runtime contract change) |
| **Behavior SoT (G1)** | [module-map-agent-retrieval.md](../behavior/module-map-agent-retrieval.md) · [lightweight-delivery-lane.md](../behavior/lightweight-delivery-lane.md) · [ai-scale-docs-conventions.md](../behavior/ai-scale-docs-conventions.md) — all **ready**/shipped |
| **Behavior SoT (Leaf 2)** | [ai-scale-template-import-peel.md](../behavior/ai-scale-template-import-peel.md) — **ready**/shipped (**BDD-AI-SCALE-TIP** TIP-01…08) |
| **Detail plan (Leaf 1)** | [detail/ai-scale-remediation-g1.md](./detail/ai-scale-remediation-g1.md) |
| **Detail plan (Leaf 2)** | [detail/ai-scale-template-import-peel.md](./detail/ai-scale-template-import-peel.md) |
| **Next queue head** | TM **#168** (queued **pending** — do **not** activate / fold) |
| **Upstream** | User confirmation 2026-07-26「按你的建议整改吧»; formal phase remains **None**; do **not** reopen PQH / SYS-NORM waves |

---

## 1. North star

Improve **AI-agent operability** at repository scale without inventing a formal P-phase:
module-map retrieval SoT, eligibility-gated lightweight delivery lane, soft size budgets
aligned with the quality-gate baseline, and progressive disclosure / archive of closed
programs so parent agents stay within attention budgets.

**Leaf 1 (Done):** governance + docs scaffold only — **no** TemplateImport / i18n /
mega-fixture peels.

**Leaf 2 (Done):** TemplateImport* structural peel under soft budgets
(`delivery_lane: full`). **Leaves 3–4** remain queued **pending** (do **not** fold / auto-activate).

---

## 2. Serial queue

| Leaf | Slice id | TM | Status | Focus |
| --- | --- | --- | --- | --- |
| **Leaf 1** | `ai-scale-remediation-g1` | **#166** | **Done** (`c4f1b0d4`) | Module map SoT + light lane + soft budgets + progressive disclosure |
| **Leaf 2** | `ai-scale-peel-template-import` | **#167** | **Done** (`d02aa414` / `1f55a420`) | TemplateImport* peel under soft budgets |
| **Leaf 3** | TBD (`ai-scale-i18n-locale-split`) | **#168** | **pending** | i18n `en.ts` / `zh-CN.ts` split |
| **Leaf 4** | TBD (`ai-scale-mega-test-fixture-split`) | **#169** | **pending** | Mega-test fixture split |

**Rule:** At most **one** AI-SCALE delivery leaf sole-active at a time. Do **not** auto-activate
Leaf 3/4. Host sole-active is **cleared** after Leaf 2 close.

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
| **AI-SCALE-G1** | **#166** | **done** | Leaf 1 closed; merge `c4f1b0d4` |
| **AI-SCALE-L2** | **#167** | **done** | Leaf 2 closed; merge `d02aa414` / feat `1f55a420` |
| **AI-SCALE-L3** | **#168** | **pending** | i18n locale split — next queue head (not activated) |
| **AI-SCALE-L4** | **#169** | **pending** | Mega-test fixture split — queued only |

**Sole-active statement:** Host delivery sole-active **cleared**. Next queue head **#168**
**pending** — do **not** activate. Umbrella **#53** stays **in-progress** registry-only
(not a delivery leaf). **#106** stays **pending** registry-only. **#119** stays
Blocked/pending.

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
| 2026-07-26 | Leaf 1 TM **#166** → **Done** (`c4f1b0d4`; worktree **REMOVED**); **sole-active cleared**; next queue head **#167** pending (not activated) |
| 2026-07-27 | Leaf 2 BDD authored **ready** — [ai-scale-template-import-peel.md](../behavior/ai-scale-template-import-peel.md) (**TIP-01…08**); detail [detail/ai-scale-template-import-peel.md](./detail/ai-scale-template-import-peel.md); worktree `DGE-ai-scale-peel-template-import`; `delivery_lane: full`; stages 5–7/10 N/A-eligible |
| 2026-07-27 | Leaf 2 TM **#167** → **in-progress** sole-active (plan-orchestrator Stage 2); Batch **solo** `ai-scale-peel-template-import`; **#168–#169** remain **pending**; formal phase **None**; umbrella **#53** host sole-active note → **#167** |
| 2026-07-27 | Leaf 2 TM **#167** → **Done** (`d02aa414` / feat `1f55a420`; worktree **REMOVED**); Batch **solo** closed; `mvn verify` exit 0; Arch **PASS**; CQ **PASS**; stages 5–7/10 **N/A**; **sole-active cleared**; next queue head **#168** pending (not activated); program stays **In Progress** |
