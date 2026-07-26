# AI-Scale Remediation Program — 2026-07

| Field | Value |
| --- | --- |
| **Program ID** | `ai-scale-remediation-2026-07` (short: **AI-SCALE**) |
| **Created** | 2026-07-26 |
| **Status** | **In Progress** (Leaf 1–3 **Done**; Leaf 4 **In Progress**) |
| **Formal phase** | **None** (NON-CE agent/docs operability program — not a P-phase) |
| **Sole-active leaf** | TM **#169** · slice `ai-scale-peel-mega-tests` (**In Progress**) |
| **Leaf 1 (Done)** | TM **#166** · `ai-scale-remediation-g1` → **Done** (`c4f1b0d4`; worktree **REMOVED**) |
| **Leaf 2 (Done)** | TM **#167** · `ai-scale-peel-template-import` → **Done** (`d02aa414` / feat `1f55a420`; worktree **REMOVED**) |
| **Leaf 3 (Done)** | TM **#168** · slice `ai-scale-peel-i18n` · i18n `en.ts` / `zh-CN.ts` domain split → **Done** (`ce47186a` / `0a5e928e`; BDD **ready**/shipped) |
| **Leaf 4 (In Progress)** | TM **#169** · slice `ai-scale-peel-mega-tests` · mega-test fixture split → **In Progress** |
| **Batch (Leaf 1)** | **solo** · `member_task_ids: ["166"]` · `proposed_slice_id: ai-scale-remediation-g1` · vetoes: `unrelated-code-peel`, `file-cap`, `risk-domain-split` — **closed** |
| **Batch (Leaf 2)** | **solo** · `member_task_ids: ["167"]` · `proposed_slice_id: ai-scale-peel-template-import` · `delivery_lane: full` · vetoes: `unrelated-frontend-i18n`, `mega-test-split` — **closed** |
| **Batch (Leaf 3)** | **solo** · `member_task_ids: ["168"]` · `proposed_slice_id: ai-scale-peel-i18n` · `delivery_lane: light` · vetoes: `mega-test-split`, `backend-domain` — **closed** |
| **Batch (Leaf 4)** | **solo** · `member_task_ids: ["169"]` · `proposed_slice_id: ai-scale-peel-mega-tests` · `delivery_lane: light` · vetoes: `unrelated-product-peel`, `checklist-#3b/#5a-GO`, `CE-O02`, `mark-#53-CE-Done` — **open** |
| **delivery_lane (Leaf 1)** | **light** (docs/governance/scaffold; product E2E/Docker **N/A**) |
| **delivery_lane (Leaf 2)** | **full** (BE verify required; stages **5–7**/10 **N/A** — zero FE + zero OpenAPI/runtime contract change) |
| **delivery_lane (Leaf 3)** | **light** (structure-only i18n modularization; identical keys/values; FE gates required; E2E/Docker **N/A**; E1–E5) |
| **delivery_lane (Leaf 4)** | **light** (test-infra peel; E1–E5; `mvn verify` required; stages **5–7**/10 **N/A**) |
| **Behavior SoT (G1)** | [module-map-agent-retrieval.md](../behavior/module-map-agent-retrieval.md) · [lightweight-delivery-lane.md](../behavior/lightweight-delivery-lane.md) · [ai-scale-docs-conventions.md](../behavior/ai-scale-docs-conventions.md) — all **ready**/shipped |
| **Behavior SoT (Leaf 2)** | [ai-scale-template-import-peel.md](../behavior/ai-scale-template-import-peel.md) — **ready**/shipped (**BDD-AI-SCALE-TIP** TIP-01…08) |
| **Behavior SoT (Leaf 3)** | [ai-scale-i18n-locale-split.md](../behavior/ai-scale-i18n-locale-split.md) — **ready**/shipped (**BDD-AI-SCALE-I18N** I18N-01…09) |
| **Behavior SoT (Leaf 4)** | [ai-scale-mega-test-fixture-split.md](../behavior/ai-scale-mega-test-fixture-split.md) — **ready** (**BDD-AI-SCALE-MTF** MTF-01…10) |
| **Detail plan (Leaf 1)** | [detail/ai-scale-remediation-g1.md](./detail/ai-scale-remediation-g1.md) |
| **Detail plan (Leaf 2)** | [detail/ai-scale-template-import-peel.md](./detail/ai-scale-template-import-peel.md) |
| **Detail plan (Leaf 3)** | [detail/ai-scale-i18n-locale-split.md](./detail/ai-scale-i18n-locale-split.md) |
| **Detail plan (Leaf 4)** | [detail/ai-scale-mega-test-fixture-split.md](./detail/ai-scale-mega-test-fixture-split.md) — **authored** (Stage 3; leaf remains **In Progress**) |
| **Worktree (Leaf 4)** | `D:/working/DGE-ai-scale-peel-mega-tests` · `feat/ai-scale-peel-mega-tests` · base `b1bea35d` |
| **Next queue head** | *(none beyond sole-active #169)* — do **not** activate FOS **#177** or other leaves |
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
(`delivery_lane: full`). **Leaf 3 (Done):** i18n locale domain split
(`delivery_lane: light`; BDD **ready**/shipped) — public facades stay `@/i18n/locales/en` /
`zh-CN`; agents edit domain modules under `frontend/src/i18n/locales/domains/*`
(see [module-map.md](../architecture/module-map.md) + i18n-english-first skill).
**Leaf 4 (In Progress):** mega-test fixture / oversized test-helper peel under soft
budgets (`slice_id: ai-scale-peel-mega-tests`) — test-infra only; no product behavior
change claimed.

---

## 2. Serial queue

| Leaf | Slice id | TM | Status | Focus |
| --- | --- | --- | --- | --- |
| **Leaf 1** | `ai-scale-remediation-g1` | **#166** | **Done** (`c4f1b0d4`) | Module map SoT + light lane + soft budgets + progressive disclosure |
| **Leaf 2** | `ai-scale-peel-template-import` | **#167** | **Done** (`d02aa414` / `1f55a420`) | TemplateImport* peel under soft budgets |
| **Leaf 3** | `ai-scale-peel-i18n` | **#168** | **Done** (`ce47186a` / `0a5e928e`) | i18n facade + `locales/domains/*` split (`delivery_lane: light`) |
| **Leaf 4** | `ai-scale-peel-mega-tests` | **#169** | **In Progress** | Mega-test fixture split |

**Rule:** At most **one** AI-SCALE delivery leaf sole-active at a time. Host sole-active =
TM **#169** / `ai-scale-peel-mega-tests` only. Do **not** fold other peels into this leaf.

---

## 3. Vetoes (hard)

| Veto | Rule |
| --- | --- |
| unrelated-code-peel | Do not merge TemplateImport / i18n / fixture peels into G1 governance leaf |
| file-cap | Peel oversized surfaces in dedicated later leaves, not as drive-by in G1 |
| risk-domain-split | Keep docs/governance light lane separate from product FE/BE behavior peels |
| mega-test-split | Do not fold **#169** into Leaf 3 (**#168**) |
| backend-domain | Do not fold backend peels into Leaf 3 i18n leaf |
| unrelated-product-peel | Do not fold product FE/BE/runtime work into Leaf 4 test-infra peel |
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
| **AI-SCALE-L3** | **#168** | **done** | i18n locale split — BDD **ready**/shipped ([ai-scale-i18n-locale-split.md](../behavior/ai-scale-i18n-locale-split.md)); merge `ce47186a` |
| **AI-SCALE-L4** | **#169** | **in-progress** | Mega-test fixture split — BDD **ready** ([ai-scale-mega-test-fixture-split.md](../behavior/ai-scale-mega-test-fixture-split.md) **MTF-01…10**); detail [detail/ai-scale-mega-test-fixture-split.md](./detail/ai-scale-mega-test-fixture-split.md); sole-active `ai-scale-peel-mega-tests`; `delivery_lane: light` |

**Sole-active statement:** Host delivery sole-active is TM **#169** / slice
`ai-scale-peel-mega-tests` (worktree `D:/working/DGE-ai-scale-peel-mega-tests` ·
`feat/ai-scale-peel-mega-tests`; base `b1bea35d`). Leaves **#166–#168** remain **done**.
Umbrella **#53** stays **in-progress** registry-only (not a delivery leaf; host
sole-active note → **#169**). **#106** stays **pending** registry-only. **#119** stays
Blocked/pending. FOS **#177** remains queued — do **not** activate.

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
| CRCH | Sibling NON-CE; W0+W1 Done — do **not** steal sole-active from an active AI-SCALE leaf |
| FOS | Sibling NON-CE; **#171–#176** Done; next **#177** queued — do **not** activate while **#169** sole-active |

---

## 7. Activation log

| Date | Event |
| --- | --- |
| 2026-07-26 | Program registered; Leaf 1 TM **#166** → **in-progress** sole-active; Leaves **#167–#169** queued **pending** |
| 2026-07-26 | Leaf 1 TM **#166** → **Done** (`c4f1b0d4`; worktree **REMOVED**); **sole-active cleared**; next queue head **#167** pending (not activated) |
| 2026-07-27 | Leaf 2 BDD authored **ready** — [ai-scale-template-import-peel.md](../behavior/ai-scale-template-import-peel.md) (**TIP-01…08**); detail [detail/ai-scale-template-import-peel.md](./detail/ai-scale-template-import-peel.md); worktree `DGE-ai-scale-peel-template-import`; `delivery_lane: full`; stages 5–7/10 N/A-eligible |
| 2026-07-27 | Leaf 2 TM **#167** → **in-progress** sole-active (plan-orchestrator Stage 2); Batch **solo** `ai-scale-peel-template-import`; **#168–#169** remain **pending**; formal phase **None**; umbrella **#53** host sole-active note → **#167** |
| 2026-07-27 | Leaf 2 TM **#167** → **Done** (`d02aa414` / feat `1f55a420`; worktree **REMOVED**); Batch **solo** closed; `mvn verify` exit 0; Arch **PASS**; CQ **PASS**; stages 5–7/10 **N/A**; **sole-active cleared**; next queue head **#168** pending (not activated); program stays **In Progress** |
| 2026-07-27 | Leaf 3 BDD authored **ready** — [ai-scale-i18n-locale-split.md](../behavior/ai-scale-i18n-locale-split.md) (**I18N-01…09**); worktree `DGE-ai-scale-peel-i18n` · `feat/ai-scale-peel-i18n`; `delivery_lane: light`; stages 5–7/10 N/A |
| 2026-07-27 | Leaf 3 TM **#168** → **in-progress** sole-active (plan-orchestrator Stage 2); Batch **solo** `ai-scale-peel-i18n`; **#169** remains **pending**; formal phase **None**; umbrella **#53** host sole-active note → **#168** |
| 2026-07-27 | Leaf 3 Stage 3 docs-first — module-map + i18n-english-first path guidance (facade stable; domains under `locales/domains/*`); indexes already linked; T02 Done; leaf remains **In Progress** (not Done) |
| 2026-07-27 | Leaf 3 TM **#168** → **Done** (`ce47186a`; worktree **REMOVED**); Batch **solo** closed; FE lint/type-check/test/build **PASS**; `localeDomainSplit.test.ts` **5/5**; Arch **PASS**; CQ **PASS**; stages 5–7/10 **N/A**; **sole-active cleared**; next queue head **#169** pending (not activated); program stays **In Progress** |
| 2026-07-27 | Leaf 4 BDD authored **ready** — [ai-scale-mega-test-fixture-split.md](../behavior/ai-scale-mega-test-fixture-split.md) (**MTF-01…10**); worktree `DGE-ai-scale-peel-mega-tests` · `feat/ai-scale-peel-mega-tests`; `delivery_lane: light`; stages 5–7/10 N/A; evidence `mvn verify`; TM **#169** sole-active **In Progress** |
| 2026-07-27 | Leaf 4 TM **#169** → **in-progress** sole-active (plan-orchestrator Stage 2); Batch **solo** `ai-scale-peel-mega-tests`; placement **ISOLATED**; worktree `D:/working/DGE-ai-scale-peel-mega-tests` · `feat/ai-scale-peel-mega-tests`; base `b1bea35d`; **#166–#168** remain **done**; formal phase **None**; umbrella **#53** host sole-active note → **#169**; BDD now **ready** (see prior log row); detail plan may still author in Stage 2/3 |
| 2026-07-27 | Leaf 4 Stage 3 docs-first — detail [detail/ai-scale-mega-test-fixture-split.md](./detail/ai-scale-mega-test-fixture-split.md) authored (hotspot baseline, peel approach, exit criteria, `mvn verify` gate, E2E/Docker **N/A**); program + `docs/README.md` indexes linked; leaf remains **In Progress** (not Done); ready for `backend-engineer` |
