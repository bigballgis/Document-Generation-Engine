# Binding editor IA + auto `referenceKey`

**Program / slice:** `binding-editor-ia` (post-SYS-NORM parked UX §4a; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#155** (Binding editor re-layout) + **#156** (Auto `referenceKey` generation) → **In Progress** (merge leaf)  
**Active delivery slice:** `binding-editor-ia` (**sole-active**)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-binding-editor-ia` · branch `feat/binding-editor-ia` · base_sha `c8967e3c898db4aaf70e5daf6557c50a05a485dd`  
**BDD:** [binding-editor-ia.md](../../behavior/binding-editor-ia.md) — **ready** (`BDD-BEI-001…020`); `frontend_ui_in_scope=true`; `backend_api_contract_change=false`  
**Upstream:** SYS-NORM Waves **0–8 Done** (program **Done**); §4a Reminder timing **#153** → **Done**; Asset library **#154** → **Done**; user explicit «完成剩余任务» → activate Binding editor + Auto `referenceKey`  
**Batch recommendation:** **merge** (`member_task_ids: ["155", "156"]`; `proposed_slice_id: binding-editor-ia`;
`shared_acceptance_surface: Template Dev binding/clause-reference authoring UX`;
`evidence_amortization: one FE verify + E2E + UIUX + queued deploy`;
vetoes_applied: checklist-#3b/#5a, CE-O02, #53, N18-unless-trivial;
`on_red_split_hint: If auto-refKey breaks independently, peel #156 to solo leaf; keep #155 layout`) — **active**

---

## Purpose

Deliver bank-OA binding / clause-reference authoring UX under one shared FE leaf:

1. **#155 Binding editor re-layout** — sticky action rail (Back · anchor title · Save); left Content type → collapsed Visibility advanced → compact structured-editor toolbar; right sticky final-chain preview with Refresh secondary; reduce nested cards/borders; fluid width; English-first i18n; WorkspaceTabShell: no CTAs on nested Design sub-tabs.
2. **#156 Auto `referenceKey` generation** — Add clause reference dialog auto-fills `referenceKey` from selected clause `moduleCode` → `UPPER_SNAKE` with `_2`/`_3` conflict suffixes; Advanced custom override; edit path keeps key locked; **client-side only** (existing upsert APIs).

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **`binding-editor-ia`** (TM **#155** + **#156** merge) — no other CE delivery leaf In Progress |
| Program | SYS-NORM Waves **0–8 Done** — program **Done**; §4a Binding editor + Auto `referenceKey` → **In Progress** under this leaf |
| Gate evidence | pending (stage 4–10) |
| Do **not** | Flip **#3b/#5a**; mark **#53** Done; activate CE-O02; invent referenceKey rename UX; change backend API contract |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| BEI-T01 | Register TM **#155** + **#156** + plan/ledger sole-active mirror | **Done** (plan-orchestrator) |
| BEI-T02 | Doc-keeper: catalog-nav / management-ui constitution / i18n index sync (no API contract) | **Done** (stage 3) |
| BEI-T03 | Frontend **#155**: sticky action rail + compact toolbar + sticky preview + Visibility collapsed + fluid width | **Not Started** |
| BEI-T04 | Frontend **#156**: Add-dialog auto `referenceKey` (UPPER_SNAKE + conflict suffix + Advanced override + edit lock) | **Not Started** |
| BEI-T05 | FE unit/component tests (TDD) — layout chrome + normalize/suffix helpers | **Not Started** |
| BEI-T06 | E2E + UIUX (BDD-BEI-001…020) | **Not Started** |
| BEI-T07 | Queued docker deploy evidence + merge + MAIN doc-sync | **Not Started** |

### Task Master members

| TM | Title | Status |
| --- | --- | --- |
| **#155** | Binding editor re-layout | **In Progress** (leaf lead) |
| **#156** | Auto `referenceKey` generation | **In Progress** (merged member) |

---

## Exit criteria (from BDD-BEI-001…020)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Sticky action rail (Back / title / Save) | BEI-001 | **Not Started** |
| 2 | Save is primary rail CTA | BEI-002 | **Not Started** |
| 3 | Visibility advanced collapsed by default | BEI-003 | **Not Started** |
| 4 | Compact structured editor toolbar | BEI-004 | **Not Started** |
| 5 | Sticky final-chain preview | BEI-005 | **Not Started** |
| 6 | Refresh secondary to Save | BEI-006 | **Not Started** |
| 7 | Reduced nesting + fluid width | BEI-007 | **Not Started** |
| 8 | No CTA on nested Design tabs | BEI-008 | **Not Started** |
| 9 | English-first i18n for layout chrome | BEI-009 | **Not Started** |
| 10 | Narrow viewport keeps rail | BEI-010 | **Not Started** |
| 11 | Preserve F7 + CE-U17/U21 | BEI-011 | **Not Started** |
| 12 | Auto-generate on module select | BEI-012 | **Not Started** |
| 13 | Conflict suffix `_2` | BEI-013 | **Not Started** |
| 14 | Conflict suffix `_3` | BEI-014 | **Not Started** |
| 15 | Advanced custom override | BEI-015 | **Not Started** |
| 16 | Override not clobbered on module change | BEI-016 | **Not Started** |
| 17 | Edit existing locks key | BEI-017 | **Not Started** |
| 18 | Empty normalize requires Advanced override | BEI-018 | **Not Started** |
| 19 | English-first i18n for dialog chrome | BEI-019 | **Not Started** |
| 20 | E2E / UIUX dual-brand acceptance | BEI-020 | **Not Started** |

---

## Out of scope this leaf

- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Activating CE-O02 / RTL  
- **N18** Legal-hold actor EntityLink  
- Backend API contract changes / new key-generation endpoints  
- Explicit referenceKey **rename** UX on edit (keep locked field)  
- Formal P-phase activation  

## Related docs

| Doc | Role |
| --- | --- |
| [binding-editor-ia.md](../../behavior/binding-editor-ia.md) | BDD SoT (**ready**) |
| [core-fortress-f7-authoring-ux.md](../../behavior/core-fortress-f7-authoring-ux.md) | F7 preview + dirty guard baseline |
| [ce-u16-authoring-path-compress.md](../../behavior/ce-u16-authoring-path-compress.md) | Design → Bindings default |
| [ce-u17-editor-shortcuts.md](../../behavior/ce-u17-editor-shortcuts.md) | Ctrl/Cmd+S / Ctrl/Cmd+P |
| [ce-u21-draft-anchor-concurrency.md](../../behavior/ce-u21-draft-anchor-concurrency.md) | 409 conflict dialog |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | §4a parked queue — this leaf **In Progress** |
| [execution-sync-ledger.md](../execution-sync-ledger.md) | Activation / evidence mirror |
| [workspace-tab-shell-constitution.mdc](../../../.cursor/rules/workspace-tab-shell-constitution.mdc) | Nested-tab CTA rules |
| [management-ui-constitution.md](../../architecture/management-ui-constitution.md) | Binding editor page chrome + WorkspaceTabShell |
| [catalog-navigation-ux.md](../../product/catalog-navigation-ux.md) | Dev authoring pointer (no new catalog routes) |
| [P20-i18n-ui-upgradeability.md](./P20-i18n-ui-upgradeability.md) | English-first i18n constitution (chrome keys) |
