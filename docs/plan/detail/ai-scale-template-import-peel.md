# AI-SCALE Leaf 2 — TemplateImport* structural peel

**Program / slice:** `ai-scale-peel-template-import` (ad-hoc **NON-CE** AI-SCALE Leaf 2; **not** a formal P-phase)  
**Program registry:** [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)  
**Formal plan phase:** **None**  
**Task Master:** **#167** (`AI-SCALE-L2` / TemplateImport* peel) — **In Progress** sole-active  
**BDD:** [ai-scale-template-import-peel.md](../../behavior/ai-scale-template-import-peel.md) — **`ready`** (2026-07-27; **BDD-AI-SCALE-TIP** TIP-01…08)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-ai-scale-peel-template-import` · branch `feat/ai-scale-peel-template-import` · base `main@f29211c5`  
**Batch recommendation:** **solo** (`member_task_ids: ["167"]`; `proposed_slice_id: ai-scale-peel-template-import`;
`delivery_lane: full`;
vetoes_applied: `unrelated-frontend-i18n`, `mega-test-split`)

**Prior (Done, do not reopen):** Leaf 1 **#166** `ai-scale-remediation-g1` → **Done** (`c4f1b0d4`)

---

## Purpose

Peel oversized TemplateImport hotspots (`TemplateImportService` ~553 LOC, `TemplateImportDependencyPrecheck` ~508 LOC) toward soft size budgets from G1 / quality-gate baseline — **structural extract only**, with **no intentional** change to import dry-run/apply or dependency precheck semantics.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** (sole-active; plan activated 2026-07-27 — not Done) |
| Formal phase | **None** |
| `bdd_readiness` | **`ready`** |
| `delivery_lane` | **full** (BE `mvn verify` required; **not** light) |
| `frontend_ui_in_scope` | **false** |
| `openapi_contract_change` | **false** (intent) |
| Stages 5–7 / 10 | **N/A-eligible** if FE + OpenAPI/runtime contract delta evidence is empty |
| Queued after this leaf | **#168** i18n · **#169** mega-test — **pending**; do **not** fold in |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; activate **#119**; invent P24+ |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| AI-SCALE-L2-T01 | Sole-active activation + TM/plan/ledger wiring for #167 | **Done** (2026-07-27 Stage 2) |
| AI-SCALE-L2-T02 | TDD Red: characterization / regression for dry-run, apply, dependency precheck | **Not Started** |
| AI-SCALE-L2-T03 | Peel `TemplateImportService` + `TemplateImportDependencyPrecheck` under soft budgets | **Not Started** |
| AI-SCALE-L2-T04 | module-map update if public entry points move; boundary check | **Not Started** |
| AI-SCALE-L2-T05 | `mvn verify` + arch review + FE/OpenAPI N/A evidence + merge + MAIN doc-sync | **Not Started** |

---

## Scope

| IN | OUT |
| --- | --- |
| Structural peel of TemplateImport* hotspots | Intentional import product-semantics change |
| Soft budgets: Service ≤400; Support ≤200; file ≤500 soft | i18n locale split (**#168**) |
| Preserve CE-E01 / Wave 7 / P14 observable outcomes | Mega-test fixture split (**#169**) |
| module-map if entry points move | FE UI / Playwright unless FE touched |
| BE full verify | Light-lane reclassification |

---

## Traceability

- Behavior SoT: [ai-scale-template-import-peel.md](../../behavior/ai-scale-template-import-peel.md)
- Soft budgets: [ai-scale-docs-conventions.md](../../behavior/ai-scale-docs-conventions.md) · [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md)
- Product import SoT (unchanged): [ce-e01-export-bundle-v2.md](../../behavior/ce-e01-export-bundle-v2.md) · [sys-norm-promotion-pack.md](../../behavior/sys-norm-promotion-pack.md)
- Program: [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)
