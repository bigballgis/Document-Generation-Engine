# AI-SCALE Leaf 3 — Frontend i18n locale domain split

**Program / slice:** `ai-scale-peel-i18n` (ad-hoc **NON-CE** AI-SCALE Leaf 3; **not** a formal P-phase)  
**Program registry:** [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)  
**Formal plan phase:** **None**  
**Task Master:** **#168** (`AI-SCALE-L3` / i18n locale split) → **Done** (`ce47186a`; sole-active **cleared**)  
**BDD:** [ai-scale-i18n-locale-split.md](../../behavior/ai-scale-i18n-locale-split.md) — **`ready`/shipped** (2026-07-27; **BDD-AI-SCALE-I18N** I18N-01…09)  
**Placement:** **MAIN** (post stage 11) · worktree **REMOVED** · merge `ce47186a0b949495e4a74dda8711bb910e877dcc`  
**Batch recommendation:** **solo** (`member_task_ids: ["168"]`; `proposed_slice_id: ai-scale-peel-i18n`;
`delivery_lane: light`;
vetoes_applied: `mega-test-split`, `backend-domain`) — **closed**

**Prior (Done, do not reopen):** Leaf 1 **#166** → **Done** (`c4f1b0d4`); Leaf 2 **#167** → **Done** (`d02aa414` / `1f55a420`)

---

## Purpose

Split oversized locale catalogs `frontend/src/i18n/locales/en.ts` and `zh-CN.ts` into domain modules under soft size budgets (public facades stay `@/i18n/locales/en` / `zh-CN`; domains under `locales/domains/*`) — **structural extract only**, with **identical** message keys and copy values (English-first preserved). No management-UI journey / visual / IA change.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** (2026-07-27; merge `ce47186a`) |
| Formal phase | **None** |
| Host sole-active | **cleared** (next queue head **#169** **pending** — do **not** activate) |
| `bdd_readiness` | **`ready`/shipped** |
| `delivery_lane` | **light** (E1–E5; FE gates still required) |
| `frontend_ui_in_scope` | **false** |
| `openapi_contract_change` | **false** |
| Stages 5–7 / 10 | **N/A** |
| Queued after this leaf | **#169** mega-test — **pending**; do **not** activate / fold |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; activate **#119**; invent P24+ |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| AI-SCALE-L3-T01 | Sole-active activation + TM/plan/ledger wiring for #168 | **Done** (2026-07-27 Stage 2) |
| AI-SCALE-L3-T02 | Doc-keeper: indexes / module-map / i18n convention cross-links as needed | **Done** (2026-07-27 Stage 3) |
| AI-SCALE-L3-T03 | TDD Red: locale key completeness / en↔zh-CN parity characterization | **Done** (2026-07-27; `localeDomainSplit.test.ts` 5/5) |
| AI-SCALE-L3-T04 | Peel `en.ts` / `zh-CN.ts` into domain modules under soft budgets (identical keys/values) | **Done** (2026-07-27) |
| AI-SCALE-L3-T05 | FE lint / type-check / test / build + light-lane N/A evidence + merge + MAIN doc-sync | **Done** (2026-07-27; merge `ce47186a`; stages 5–7/10 **N/A**) |

---

## Scope (IN / OUT)

| IN | OUT |
| --- | --- |
| Structural domain split of mega locale catalogs | Copy meaning / key-path changes |
| Soft budgets for locale modules | Mega-test fixture split (**#169**) |
| FE unit gates + key parity tests | Backend / OpenAPI / runtime semantics |
| English-first + zh-CN parity preserved | Playwright E2E / Docker deploy (N/A light) |

---

## Gate evidence

| Gate | Result |
| --- | --- |
| `pnpm` lint / type-check / test / build | **PASS** (pre-merge worktree) |
| `localeDomainSplit.test.ts` | **5/5 PASS** after rebase |
| architecture-reviewer | **PASS** |
| code-quality-reviewer | **PASS** |
| Stages 5–7 / 10 | **N/A** (`delivery_lane: light`; `frontend_ui_in_scope=false`) |

---

## Traceability

- Behavior SoT: [ai-scale-i18n-locale-split.md](../../behavior/ai-scale-i18n-locale-split.md)
- Program: [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)
- Agent paths: [module-map.md](../../architecture/module-map.md) (`frontend/src/i18n` row)
- Soft budgets: [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md)
- i18n skill: [.cursor/skills/i18n-english-first/SKILL.md](../../../.cursor/skills/i18n-english-first/SKILL.md)
