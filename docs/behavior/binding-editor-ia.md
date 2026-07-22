---
id: DOC-BEHAVIOR-BINDING-EDITOR-IA
type: Behavior Spec
status: Confirmed
readiness: ready
program: post-SYS-NORM parked UX (§4a)
slice: binding-editor-ia
taskMaster: "155+156"
related:
  - docs/plan/system-normalization-program-2026-07.md
  - docs/plan/detail/binding-editor-ia.md
  - docs/behavior/core-fortress-f7-authoring-ux.md
  - docs/behavior/ce-u16-authoring-path-compress.md
  - docs/behavior/ce-u17-editor-shortcuts.md
  - docs/behavior/ce-u21-draft-anchor-concurrency.md
  - docs/architecture/management-ui-constitution.md
  - docs/product/catalog-navigation-ux.md
  - docs/plan/detail/P20-i18n-ui-upgradeability.md
  - .cursor/rules/workspace-tab-shell-constitution.mdc
---

# Binding editor IA + auto `referenceKey`

> **TM:** Task Master **#155** (Binding editor re-layout) + **#156** (Auto `referenceKey`
> generation) · slice `binding-editor-ia` · Batch Recommendation **`merge`**  
> **User confirmation (2026-07-22):** Binding editor feels messy — remedi to bank OA
> hierarchy (sticky action rail, compact editor toolbar, sticky final-chain preview,
> collapsed visibility advanced, reduce nested cards/borders, fluid width,
> English-first i18n, WorkspaceTabShell rules). Add Clause Reference dialog MUST
> auto-generate `referenceKey` from selected clause `moduleCode` → `UPPER_SNAKE`, with
> `_2` / `_3` conflict suffixes and optional Advanced custom override; edit path keeps
> key locked.  
> **Out of scope / vetoes:** checklist **#3b** / **#5a**; CE umbrella **#53** Done claim;
> CE-O02; **N18** Legal-hold actor EntityLink; formal P-phase; inventing referenceKey
> rename UX (unless a one-line trivial keep of existing disabled field); backend API
> contract changes.

```
bdd_readiness: ready
frontend_ui_in_scope: true
backend_api_contract_change: false
open_questions: []
owning_doc: docs/behavior/binding-editor-ia.md
task_ids: ["155", "156"]
queue_slice_id: binding-editor-ia
member_task_ids: ["155", "156"]
batch_decision: merge
shared_acceptance_surface: Template Dev binding / clause-reference authoring UX
scenario_ids:
  - BDD-BEI-001 … BDD-BEI-020
scenario_count: 20
on_red_split_hint: If auto-refKey breaks independently, peel #156 to solo leaf; keep #155 layout
```

---

## 1. Actor / role

| Actor | Role / capability | Concern |
| --- | --- | --- |
| **Document author** | `DOCUMENT_AUTHOR` (Wave 5) with template authoring capability (`authorTemplates`) | Edit anchor bindings; register clause references; save; refresh preview |
| **Restricted tester-author** | `TEMPLATE_TESTER` / decideTests with draft edit where already allowed | Same surfaces when editable; no new privileges |
| **Read-only / unauthorized** | Lacks authoring capability | Fail-closed: no write CTAs / dialogs that mutate bindings or references |
| **System** | Vue authoring UI + existing binding / content-module-reference APIs | Layout chrome; client-side `referenceKey` suggestion; i18n; sticky rails |

Role codes follow Wave 5 six-role catalog. Historical `TEMPLATE_AUTHOR` maps per ADR-0070.

---

## 2. User goal

1. When configuring a master-anchor binding, the page reads as a clear OA workspace:
   **sticky action rail** (Back / anchor identity / Save) separate from content; left
   authoring column; right **sticky final-chain preview**; compact structured-editor
   toolbar; minimal nested borders/cards; fluid width.
2. When adding a clause reference, the author does **not** invent a key by hand for the
   happy path — selecting a clause module auto-fills a conflict-safe `UPPER_SNAKE`
   `referenceKey`, with optional Advanced override.
3. Editing an existing reference does **not** casually rename the key (locked by default).
4. All new/changed user-visible strings are i18n keys with **English primary** copy
   (`en.ts`); zh-CN may lag but MUST NOT ship hard-coded Chinese-only UI chrome.

---

## 3. Trigger

| Surface | Trigger |
| --- | --- |
| Binding editor | Author opens Design → Bindings → Configure/Edit an anchor binding (`TemplateAuthoringBindingEditor`) |
| Clause references | Author opens Design → Content modules / Clause authoring → **Add clause reference** dialog |
| Preview | Author clicks Refresh on the binding-editor preview pane (secondary to Save) |
| Nested Design tabs | Author switches Variables / Bindings / Content modules — **no** new CTAs appear on nested tab rows |

---

## 4. Preconditions

- SYS-NORM Waves **0–8 Done**; §4a Reminder timing **#153** Done; Asset library **#154** Done.
- F7 side-by-side preview + dirty guard baseline exist
  ([core-fortress-f7-authoring-ux.md](./core-fortress-f7-authoring-ux.md)).
- CE-U16 default Design → Bindings; CE-U17 Ctrl/Cmd+S / Ctrl/Cmd+P shortcuts;
  CE-U21 concurrency hint — **behavior preserved** (this leaf remedi IA, does not
  regress those contracts).
- Existing APIs for binding upsert and content-module reference upsert remain the
  persistence boundary — **no** new backend endpoints for key generation.
- Template is in an editable lifecycle for the actor (DRAFT / TESTING per existing rules).
- Clause modules available to the author expose durable `moduleCode`.

---

## 5. Confirmed decisions

| ID | Decision |
| --- | --- |
| **BEI-C1** | Slice merges **#155** + **#156** into one leaf `binding-editor-ia` (shared FE authoring surface + shared E2E). |
| **BEI-C2** | Binding editor has a **sticky top action rail**: **Back** (secondary) · **anchor title** (anchorId + optional displayLabel) · **Save** (primary). Rail is visually/spatially separate from the content columns. |
| **BEI-C3** | Left column order: **Content type** → **Visibility** (collapsed advanced by default) → **structured editor** with a **compact** single-plane toolbar (not multi-boxed nested border stacks). |
| **BEI-C4** | Right column: **final-chain** preview pane is **sticky** (md+ side-by-side); **Refresh** is visually **secondary** to Save (default/plain button — not competing primary). F7 stale badge + boundary copy remain. |
| **BEI-C5** | Reduce nested `el-card` / double borders / padded boxes that create “border hell”; prefer flat OA sections and fluid width (AppPageLayout / authoring fluid conventions). |
| **BEI-C6** | WorkspaceTabShell constitution: journey/timeline = no CTA; top-level workspace tabs keep their single action rail; **Design nested sub-tabs** remain content navigation only — **no** Back/Save/Refresh CTAs on nested tab rows. Binding-editor CTAs live on the binding-editor rail / preview pane only. |
| **BEI-C7** | Auto `referenceKey` runs on **module select** in **Add** dialog (create path): derive from selected module’s `moduleCode` via deterministic **UPPER_SNAKE** normalize (BEI-C8). |
| **BEI-C8** | Normalize algorithm (client): (1) map each run of non `[A-Za-z0-9]` to `_`; (2) collapse consecutive `_`; (3) trim leading/trailing `_`; (4) uppercase ASCII letters. Empty result → leave key blank and require Advanced override before save. |
| **BEI-C9** | Conflict scope = existing `referenceKey` values already registered on **this template** (current references list). If candidate taken → append `_2`, then `_3`, … smallest unused `_<n>` for `n≥2`. Base key without suffix preferred when free. |
| **BEI-C10** | **Advanced** (collapsed): optional custom `referenceKey` override. Opening Advanced and editing the key marks **user-overridden**. While overridden, changing module MUST **not** clobber the custom key. Clearing override (or collapsing + reset control if provided) restores auto-suggest on next module change / re-select. |
| **BEI-C11** | **Edit existing reference:** `referenceKey` field **locked/disabled** (current behavior kept). Explicit rename UX is **out of scope** for this leaf. |
| **BEI-C12** | Create dialog: before module select, key may be empty; after auto-fill, Save still validates non-empty key + module + approved version (existing validation). |
| **BEI-C13** | English-first i18n: every new chrome string uses i18n keys; primary locale English; placeholders like `LOAN_DISCLOSURE` remain illustrative EN. |
| **BEI-C14** | Formal phase remains **None**; do **not** flip checklist **#3b/#5a**; do **not** mark **#53** Done; do **not** activate CE-O02; **N18** stays deferred. |
| **BEI-C15** | Coverage expectation: Vitest for normalize + conflict suffix; Playwright E2E for Add-dialog auto-key + binding-editor layout hierarchy/smoke; UIUX dual-brand @1920. |

---

## 6. Primary journeys

### 6.1 Binding editor re-layout (#155)

1. Author opens an editable template → Design → Bindings → Configure/Edit an unbound or
   existing anchor.
2. Binding editor mounts with **sticky action rail** above content: Back · anchor title · Save.
3. Below the rail, side-by-side (md+): left authoring / right preview.
4. Left: chooses **Content type**; optionally expands **Visibility** advanced to enable
   expression; edits structured blocks via **compact toolbar** + block list.
5. Right: sticky final-chain preview; stale badge when structure dirty; Refresh secondary.
6. Author Saves (rail primary or Ctrl/Cmd+S per CE-U17) → existing save/concurrency semantics.
7. Author Back → returns to bindings list; dirty guard (F7) still applies.

### 6.2 Auto `referenceKey` on Add Clause Reference (#156)

1. Author opens Clause authoring / Content modules references → **Add clause reference**.
2. Selects a content module (clause) from the module picker.
3. System auto-fills `referenceKey` from `moduleCode` (BEI-C8) with conflict suffixes (BEI-C9).
4. Author picks an APPROVED+ACTIVE semantic version → Save reference → success toast;
   reference appears in table for use in `contentModuleRef` binding nodes.
5. Optional: expand Advanced → type custom key → save uses override; further module
   changes do not overwrite until override cleared.
6. Edit existing row → dialog opens with key **disabled**; module/version may update per
   existing rules; key unchanged.

---

## 7. System responses

### 7.1 Success

| Action | Response |
| --- | --- |
| Open binding editor | Sticky rail + two-column OA layout; fluid width; compact toolbar |
| Save binding | Existing success messaging / pristine mark; CE-U21 conflict dialog unchanged on 409 |
| Refresh preview | Existing final-chain refresh; Refresh control secondary; in-flight disable (F7) |
| Select module (Add dialog) | `referenceKey` auto-filled; Advanced collapsed |
| Save new reference | Existing upsert API success; dialog closes; list refresh |
| Edit reference | Key locked; other editable fields behave as today |

### 7.2 Boundary / exception / fail-closed

| Case | Response |
| --- | --- |
| No authoring capability | No mutate CTAs / dialogs (existing authz) |
| moduleCode normalizes to empty | Key stays blank; Save blocked by required validation until Advanced override |
| All of base, `_2`, … temporarily collide | Keep incrementing `_n` until free (client); server uniqueness errors still surface via existing error envelope |
| User-overridden key then module change | Key **unchanged** |
| Edit path key field | Disabled — no rename |
| Nested Design sub-tabs | No Back/Save/Refresh buttons injected onto nested tab row |
| Paste residue / publish gates | Unchanged (ops-paste-binding-seam / publish gate) |
| Narrow viewport (`< md`) | Preview stacks under editor; may collapse; action rail remains usable |

---

## 8. Acceptance scenarios (Given / When / Then)

### A — Binding editor layout (#155)

#### BDD-BEI-001 — Sticky action rail hierarchy

**Given** an entitled author opens the binding editor for an anchor  
**When** the editor is visible at md+ width  
**Then** a sticky top action rail shows **Back**, the **anchor title** (anchorId; displayLabel when present), and **Save**  
**And** the rail is separate from the left/right content columns (not buried inside nested cards)

#### BDD-BEI-002 — Save is the primary rail CTA

**Given** BDD-BEI-001  
**When** the author inspects rail button emphasis  
**Then** **Save** is the primary CTA on the rail  
**And** **Back** is non-primary  
**And** Save remains reachable without scrolling the structured content (sticky rail)

#### BDD-BEI-003 — Visibility advanced collapsed by default

**Given** the author opens the binding editor on a binding without an existing visibility expression focus requirement  
**When** the left column renders  
**Then** Content type is visible  
**And** Visibility controls are presented as **collapsed advanced** (expression UI not fully expanded by default)  
**And** expanding advanced reveals enable-checkbox + expression input (existing ConditionExpressionInput semantics)

#### BDD-BEI-004 — Compact structured editor toolbar

**Given** the binding editor left column shows `ControlledStructuredContentEditor`  
**When** the author views the block/history/inline/style/paste controls  
**Then** those controls appear as a **compact toolbar** (single cohesive toolbar plane)  
**And** the UI avoids stacked nested bordered cards around the toolbar that recreate “border hell”

#### BDD-BEI-005 — Sticky final-chain preview

**Given** md+ side-by-side layout  
**When** the author scrolls the left editor content  
**Then** the right final-chain preview pane remains sticky/visible in the viewport working area  
**And** preview continues to use the existing final-chain artifact path (no HTML approximate preview)

#### BDD-BEI-006 — Refresh secondary to Save

**Given** the binding editor with preview pane  
**When** comparing Save (rail) vs Refresh (preview)  
**Then** Refresh is a **secondary** control (not competing `type="primary"` emphasis with Save)  
**And** F7 stale badge + Refresh-now behavior still apply when structure is stale

#### BDD-BEI-007 — Reduced nesting + fluid width

**Given** the binding editor surface  
**When** rendered in the management shell  
**Then** the layout uses fluid width conventions for the authoring workspace  
**And** unnecessary nested cards/borders around content-type / editor / preview are reduced vs the pre-remedi “messy” stacking

#### BDD-BEI-008 — WorkspaceTabShell: no CTA on nested Design tabs

**Given** the author is on Design with nested sub-tabs (Variables / Bindings / Content modules)  
**When** inspecting the nested sub-tab row  
**Then** there are **no** Back / Save / Refresh (or other workflow CTAs) on that nested tab row  
**And** binding Save/Back live only on the binding-editor action rail; preview Refresh only on the preview pane

#### BDD-BEI-009 — English-first i18n for layout chrome

**Given** locale `en`  
**When** the binding editor remedi chrome renders (rail labels reuse `common.back` / `common.save` or dedicated keys; new section labels if any)  
**Then** all user-visible new/changed strings resolve via i18n keys  
**And** English copy is present and primary (no hard-coded non-i18n chrome)

#### BDD-BEI-010 — Narrow viewport stacks without losing rail

**Given** viewport width `< md`  
**When** the binding editor opens  
**Then** preview stacks under the editor (existing collapse/expand affordance allowed)  
**And** the sticky action rail (Back / title / Save) remains usable

#### BDD-BEI-011 — Preserve F7 dirty guard + CE-U17/U21 contracts

**Given** dirty structured content in the binding editor  
**When** the author attempts to navigate away  
**Then** F7 dirty guard still intercepts (Stay / Discard / Save)  
**And** Ctrl/Cmd+S still targets binding save; Ctrl/Cmd+P still refresh preview (CE-U17)  
**And** 409 binding version conflict still offers Reload / Keep editing (CE-U21)

### B — Auto `referenceKey` (#156)

#### BDD-BEI-012 — Auto-generate on module select

**Given** the Add clause reference dialog is open (create path; not editing)  
**And** the template has no reference using the normalized key for module `loan-disclosure`  
**When** the author selects that module (`moduleCode` e.g. `loan-disclosure` or `LOAN-DISCLOSURE`)  
**Then** `referenceKey` auto-fills to `LOAN_DISCLOSURE` (BEI-C8)  
**And** Advanced custom override remains collapsed

#### BDD-BEI-013 — Conflict suffix `_2`

**Given** the template already has `referenceKey` `LOAN_DISCLOSURE`  
**When** the author selects a module whose normalized base is `LOAN_DISCLOSURE`  
**Then** the suggested key is `LOAN_DISCLOSURE_2`

#### BDD-BEI-014 — Conflict suffix `_3`

**Given** the template already has `LOAN_DISCLOSURE` and `LOAN_DISCLOSURE_2`  
**When** the author selects a module whose normalized base is `LOAN_DISCLOSURE`  
**Then** the suggested key is `LOAN_DISCLOSURE_3`

#### BDD-BEI-015 — Advanced custom override

**Given** Add dialog with an auto-filled key  
**When** the author expands Advanced and enters a custom key `MY_CUSTOM_REF`  
**And** saves with a valid approved version  
**Then** the persisted reference uses `MY_CUSTOM_REF`  
**And** the key is marked user-overridden for the open dialog session

#### BDD-BEI-016 — Override not clobbered on module change

**Given** Add dialog with user-overridden custom key  
**When** the author changes the selected module  
**Then** the custom `referenceKey` value is **not** replaced by a new auto suggestion  
**Until** the author clears the override (explicit reset), after which module select/re-select resumes auto-suggest

#### BDD-BEI-017 — Edit existing locks key

**Given** an existing clause reference `LOAN_DISCLOSURE`  
**When** the author opens Edit reference  
**Then** the `referenceKey` input is disabled/locked  
**And** the author can still change module/version per existing editable rules without renaming the key in this leaf

#### BDD-BEI-018 — Empty normalize requires Advanced override

**Given** a module whose `moduleCode` normalizes to an empty string under BEI-C8  
**When** the author selects that module  
**Then** `referenceKey` stays empty  
**And** Save remains blocked by required-field validation until Advanced supplies a non-empty key

#### BDD-BEI-019 — Create validation still fail-closed

**Given** Add dialog with module selected but version missing (or key cleared)  
**When** the author clicks Save reference  
**Then** the dialog does not persist  
**And** the existing required-field warning (`templates.clauseAuthoring.validation.required` or successor i18n key) is shown

#### BDD-BEI-020 — Shared journey evidence (Vitest + E2E)

**Given** the merged leaf implementation  
**When** quality gates run  
**Then** Vitest covers BEI-C8 normalize + BEI-C9 conflict suffix helpers  
**And** Playwright E2E covers: (1) Add clause reference auto-key happy path, (2) binding-editor sticky rail + secondary Refresh smoke on the authoring surface  
**And** UIUX review targets dual-brand @1920 with Critical = 0 for the remedi chrome

---

## 9. Observable evidence

| Evidence | What proves it |
| --- | --- |
| UI | `data-testid="binding-editor"` rail sticky; preview `authoring-preview-pane`; Refresh not primary; visibility advanced collapsed; compact toolbar class/structure |
| UI dialog | Add clause reference auto-fills key on module change; Advanced override; edit disables key |
| Unit | Pure function tests for UPPER_SNAKE + suffix allocation |
| E2E | Journey on Docker `4173` for dialog + editor layout |
| API | Unchanged upsert envelopes — no new generate endpoint |
| i18n | New keys in `frontend/src/i18n/locales/en.ts` (zh-CN mirrored or fallback-safe) |
| Authz | Unauthorized roles cannot mutate (regression) |

---

## 10. Traceability

| Item | Link |
| --- | --- |
| Program §4a | [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) §4a Binding editor + Auto `referenceKey` |
| Task Master | **#155**, **#156** (register via plan-orchestrator) |
| Slice | `binding-editor-ia` |
| Batch | `merge` · `member_task_ids: ["155","156"]` · vetoes: checklist-#3b/#5a, CE-O02, #53, N18 |
| Related UX | F7 authoring UX; CE-U16/U17/U21; WorkspaceTabShell constitution |
| Components (likely) | `TemplateAuthoringBindingEditor.vue` (+ scss), `ControlledStructuredContentEditor.vue`, `StructuredContentEditorToolbar.vue`, `AuthoringPreviewPane.vue`, `AuthoringSideBySideLayout.vue`, `ClauseAuthoringDialogs.vue`, `createClauseAuthoringReferenceActions.ts` |
| Formal phase | **None** |

---

## 11. Out of scope

- Checklist **#3b** / **#5a** status flips  
- Marking CE umbrella **#53** Done  
- Activating **CE-O02**  
- **N18** Legal-hold actor EntityLink  
- Explicit `referenceKey` rename workflow (beyond keeping edit lock)  
- Backend generate API / Flyway / OpenAPI contract changes  
- Changing publish gates, paste-residue rules, or preview artifact pipeline  
- Claiming go-live  

---

## 12. Handoff notes (plan-orchestrator / FE)

- Prefer one FE verify + E2E + UIUX + queued deploy for the merged leaf.  
- If auto-refKey regresses independently after Red: peel **#156** to a solo leaf; keep
  **#155** layout in `binding-editor-ia`.  
- Do not implement until plan-orchestrator registers TM **#155/#156** and detail plan
  exists; this document is the BDD SoT for acceptance.
