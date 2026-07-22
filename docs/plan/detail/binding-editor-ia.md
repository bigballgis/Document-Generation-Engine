# Binding editor IA + auto `referenceKey`

**Program / slice:** `binding-editor-ia` (post-SYS-NORM parked UX §4a; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#155** (Binding editor re-layout) + **#156** (Auto `referenceKey` generation) → **Done** (merge leaf)  
**Active delivery slice:** `binding-editor-ia` (**closed**; sole-active **cleared**)  
**Placement:** **ISOLATED** · worktree **REMOVED** · branch `feat/binding-editor-ia` (merged) · MAIN merge `9f2378ad` · feature tip `9e318d9c`  
**BDD:** [binding-editor-ia.md](../../behavior/binding-editor-ia.md) — **ready**/shipped (`BDD-BEI-001…020`); `frontend_ui_in_scope=true`; `backend_api_contract_change=false`  
**Upstream:** SYS-NORM Waves **0–8 Done** (program **Done**); §4a Reminder timing **#153** → **Done**; Asset library **#154** → **Done**; user explicit «完成剩余任务» → Binding editor + Auto `referenceKey` delivered  
**Batch recommendation:** **merge** (`member_task_ids: ["155", "156"]`; `proposed_slice_id: binding-editor-ia`;
`shared_acceptance_surface: Template Dev binding/clause-reference authoring UX`;
`evidence_amortization: one FE verify + E2E + UIUX + queued deploy`;
vetoes_applied: checklist-#3b/#5a, CE-O02, #53, N18-unless-trivial;
`on_red_split_hint: If auto-refKey breaks independently, peel #156 to solo leaf; keep #155 layout`) — **closed**

---

## Purpose

Deliver bank-OA binding / clause-reference authoring UX under one shared FE leaf:

1. **#155 Binding editor re-layout** — sticky action rail (Back · anchor title · Save); left Content type → collapsed Visibility advanced → compact structured-editor toolbar; right sticky final-chain preview with Refresh secondary; reduce nested cards/borders; fluid width; English-first i18n; WorkspaceTabShell: no CTAs on nested Design sub-tabs.
2. **#156 Auto `referenceKey` generation** — Add clause reference dialog auto-fills `referenceKey` from selected clause `moduleCode` → `UPPER_SNAKE` with `_2`/`_3` conflict suffixes; Advanced custom override; edit path keeps key locked; **client-side only** (existing upsert APIs).

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | **cleared** (TM **#155** + **#156** merge closed) — no delivery leaf In Progress |
| Program | SYS-NORM Waves **0–8 Done** — program **Done**; §4a Binding editor + Auto `referenceKey` → **Done** |
| Gate evidence | FE lint/type-check/test (**1697**)/build **GREEN**; BE `mvn verify` **N/A** (no Java); E2E `binding-editor-ia.spec.ts` **9/9**; UIUX **PASS_WITH_NOTES** Critical=0 Major=0 Minor=2; Architecture **PASS_WITH_NOTES** Critical=0 `merge_go=yes`; CQ Critical toolbar selector drift **FIXED**; Stage 5 **DEPLOY_OK** `2026-07-22T12:34:41+08:00`; Stage 10 SkipBuild **DEPLOY_OK** `2026-07-22T13:03:32+08:00` |
| Do **not** | Flip **#3b/#5a**; mark **#53** Done; activate CE-O02; invent referenceKey rename UX; change backend API contract; claim N18 Done |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| BEI-T01 | Register TM **#155** + **#156** + plan/ledger sole-active mirror | **Done** (plan-orchestrator) |
| BEI-T02 | Doc-keeper: catalog-nav / management-ui constitution / i18n index sync (no API contract) | **Done** (stage 3) |
| BEI-T03 | Frontend **#155**: sticky action rail + compact toolbar + sticky preview + Visibility collapsed + fluid width | **Done** |
| BEI-T04 | Frontend **#156**: Add-dialog auto `referenceKey` (UPPER_SNAKE + conflict suffix + Advanced override + edit lock) | **Done** |
| BEI-T05 | FE unit/component tests (TDD) — layout chrome + normalize/suffix helpers | **Done** |
| BEI-T06 | E2E + UIUX (BDD-BEI-001…020) | **Done** (E2E **9/9**; UIUX **PASS_WITH_NOTES** Critical=0) |
| BEI-T07 | Queued docker deploy evidence + merge + MAIN doc-sync | **Done** (Stage 5+10 **DEPLOY_OK**; MAIN merge `9f2378ad`) |

### Task Master members

| TM | Title | Status |
| --- | --- | --- |
| **#155** | Binding editor re-layout | **Done** (leaf lead) |
| **#156** | Auto `referenceKey` generation | **Done** (merged member) |

---

## Exit criteria (from BDD-BEI-001…020)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Sticky action rail (Back / title / Save) | BEI-001 | **Done** |
| 2 | Save is primary rail CTA | BEI-002 | **Done** |
| 3 | Visibility advanced collapsed by default | BEI-003 | **Done** |
| 4 | Compact structured editor toolbar | BEI-004 | **Done** |
| 5 | Sticky final-chain preview | BEI-005 | **Done** |
| 6 | Refresh secondary to Save | BEI-006 | **Done** |
| 7 | Reduced nesting + fluid width | BEI-007 | **Done** |
| 8 | No CTA on nested Design tabs | BEI-008 | **Done** |
| 9 | English-first i18n for layout chrome | BEI-009 | **Done** |
| 10 | Narrow viewport keeps rail | BEI-010 | **Done** |
| 11 | Preserve F7 + CE-U17/U21 | BEI-011 | **Done** |
| 12 | Auto-generate on module select | BEI-012 | **Done** |
| 13 | Conflict suffix `_2` | BEI-013 | **Done** |
| 14 | Conflict suffix `_3` | BEI-014 | **Done** |
| 15 | Advanced custom override | BEI-015 | **Done** |
| 16 | Override not clobbered on module change | BEI-016 | **Done** |
| 17 | Edit existing locks key | BEI-017 | **Done** |
| 18 | Empty normalize requires Advanced override | BEI-018 | **Done** |
| 19 | English-first i18n for dialog chrome | BEI-019 | **Done** |
| 20 | E2E / UIUX dual-brand acceptance | BEI-020 | **Done** |

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
| [binding-editor-ia.md](../../behavior/binding-editor-ia.md) | BDD SoT (**ready**/shipped) |
| [core-fortress-f7-authoring-ux.md](../../behavior/core-fortress-f7-authoring-ux.md) | F7 preview + dirty guard baseline |
| [ce-u16-authoring-path-compress.md](../../behavior/ce-u16-authoring-path-compress.md) | Design → Bindings default |
| [ce-u17-editor-shortcuts.md](../../behavior/ce-u17-editor-shortcuts.md) | Ctrl/Cmd+S / Ctrl/Cmd+P |
| [ce-u21-draft-anchor-concurrency.md](../../behavior/ce-u21-draft-anchor-concurrency.md) | 409 conflict dialog |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | §4a parked queue — this leaf **Done** |
| [execution-sync-ledger.md](../execution-sync-ledger.md) | Completion / evidence mirror |
| [workspace-tab-shell-constitution.mdc](../../../.cursor/rules/workspace-tab-shell-constitution.mdc) | Nested-tab CTA rules |
| [management-ui-constitution.md](../../architecture/management-ui-constitution.md) | Binding editor page chrome + WorkspaceTabShell |
| [catalog-navigation-ux.md](../../product/catalog-navigation-ux.md) | Dev authoring pointer (no new catalog routes) |
| [P20-i18n-ui-upgradeability.md](./P20-i18n-ui-upgradeability.md) | English-first i18n constitution (chrome keys) |
