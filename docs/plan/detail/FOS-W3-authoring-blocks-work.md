# FOS-W3 — Authoring blocks work

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W3
**Status:** **Done**
**Slice id:** `fos-authoring-blocks-work` · worktree `../DGE-fos-authoring-blocks-work` · branch `feat/fos-authoring-blocks-work`
**Task Master:** **#173**
**Audit baseline:** `f29211c5`
**delivery_lane:** **full**
**Supersedes:** parts of CRCH §7 W4 that touch structured-editor usability (not the 708-line test-data dialog — that remains CRCH W4-3 / may be absorbed later as a dedicated sub-task under W3-extra if still open)

**Origin audit ids:** A5, A6, A7, A9, A10, A11, A12

---

## Before you write any code

```powershell
git fetch origin
git worktree add "..\DGE-fos-authoring-blocks-work" -b feat/fos-authoring-blocks-work origin/main
```

Read `.cursor/skills/frontend-oa-design/SKILL.md` before changing editor chrome.

### Task order

| Order | Task | Sev | Primary file |
| --- | --- | --- | --- |
| 1 | [W3-1](#w3-1) Top-level Content module insert works | **P0** | `structuredContentNodes.ts` `insertBlockNode` |
| 2 | [W3-2](#w3-2) List block has an editor (or leave toolbar) | **P0** | `StructuredContentBlockCard.vue` |
| 3 | [W3-3](#w3-3) Apply style scopes correctly | **P0** | `applyStyleToParagraphs` / `applySelectedStyle` |
| 4 | [W3-4](#w3-4) Nesting depth guard matches message | **P1** | `structuredContentNodePath.ts` |
| 5 | [W3-5](#w3-5) Inline insert targets focused block | **P1** | `createStructuredContentDocumentMutations.ts` |
| 6 | [W3-6](#w3-6) Table component ref uses search select | **P1** | `StructuredContentBlockCard.vue` |
| 7 | [W3-7](#w3-7) Validate structure on save | **P1** | binding editor save flow |

---

<a id="w3-1"></a>
## W3-1 — Top-level "Content module" toolbar button no-ops

**Severity:** P0
**File:** `frontend/src/utils/structuredContentNodes.ts` (`insertBlockNode`)

### Current behaviour

Toolbar uses `STRUCTURED_BLOCK_NODE_TYPES` (includes `'contentModuleRef'`), but:

```ts
const blockTypes = ['sectionHeading','paragraph','list','conditionBlock','loopBlock','tableComponentRef']
if (!blockTypes.includes(nodeType)) return document
```

Nested insert via `insertNestedBlock` → `createNodeTemplate` works. Top-level does not.

### Implement

Add `'contentModuleRef'` to `blockTypes` (prefer deriving from
`STRUCTURED_BLOCK_NODE_TYPES` so they cannot drift again). Red test: insert at root
increases node count and last/new node type is `contentModuleRef`.

---

<a id="w3-2"></a>
## W3-2 — List block renders as grey "list" meta card

**Severity:** P0
**File:** `frontend/src/components/authoring/StructuredContentBlockCard.vue`

### Current behaviour

`list` is insertable and `createNodeTemplate('list')` builds children, but the card has
no `v-else-if="node.type === 'list'"` branch and `isNestedContainerType` excludes it →
falls through to `<p class="node-meta">{{ node.type }}</p>`.

### Chosen resolution (minimal, no new feature)

Render `list` children with the **existing paragraph editor** used for nested children
of other containers (reuse the same child recursion path as `conditionBlock` /
`loopBlock` for editing children; keep list-specific chrome minimal). If that proves to
require new list-item UX beyond reuse, **instead** remove `'list'` from the insertable
toolbar types and document the removal in the PR — do not ship a dead control.

Prefer reuse-first. Red test: after inserting list, an editable paragraph child is present.

---

<a id="w3-3"></a>
## W3-3 — Apply style rewrites every top-level paragraph/heading

**Severity:** P0
**Files:** `frontend/src/utils/structuredContentNodes.ts` (`applyStyleToParagraphs`),
`frontend/src/composables/createStructuredContentDocumentMutations.ts` (`applySelectedStyle`)

### Current behaviour

```ts
nodes.map(node =>
  node.type === 'paragraph' || node.type === 'sectionHeading'
    ? { ...node, styleRef: styleKey }
    : node)
```

Ignores `applicableNodeTypes` and selection/focus.

### Chosen resolution

1. Apply only to the **focused** block path if the editor tracks focus/selection path
   (preferred — grep for selected path / `focusedPath`).
2. If no focus tracking exists yet, **require confirm** before bulk apply, and filter by
   `applicableNodeTypes` from the style catalogue entry.

Red test: with two paragraphs and focus on the second, only the second gets `styleRef`.

### Do NOT

- Do not invent a full rich selection model beyond existing focus signals.

---

<a id="w3-4"></a>
## W3-4 — Nesting depth message promises 3 layers but allows 2

**Severity:** P1
**File:** `frontend/src/utils/structuredContentNodePath.ts`
(`canAddNestedBlockChildren`, `STRUCTURED_CONTENT_MAX_NEST_DEPTH`)

### Current behaviour

`return parentPath.length + 1 < STRUCTURED_CONTENT_MAX_NEST_DEPTH` with max `3` → container
at path length 2 already fails. Message uses `{ max: 3 }`.

### Chosen resolution

Make guard and copy agree. **Preferred:** allow true depth 3 containers
(`parentPath.length < MAX`) if backend/publish gates already allow depth 3 — verify
against backend nesting limits / CE-U01 tests before loosening. If backend max is 2
container layers, change the i18n message to state 2.

Do not silently raise backend limits.

---

<a id="w3-5"></a>
## W3-5 — Inline "Variable" inserts into the last top-level block

**Severity:** P1
**File:** `frontend/src/composables/createStructuredContentDocumentMutations.ts`
(`insertInline`)

### Current behaviour

Always targets `nodes[nodes.length - 1]`, ignoring focus; nested blocks never targeted.

### Implement

Route toolbar inline inserts through the same path the card already emits via
`add-inline` / focused path. Red test: focused middle paragraph receives the inline node.

---

<a id="w3-6"></a>
## W3-6 — Table component ref is free text

**Severity:** P1
**File:** `frontend/src/components/authoring/StructuredContentBlockCard.vue`
(`tableComponentRef` branch)

### Implement

Reuse the `AppSearchSelect` pattern from the sibling `contentModuleRef` branch, fed by
the existing table-component catalogue options already loaded for authoring (grep
`tableComponent` options in template stores). Keep advanced free-text only if the
catalogue pattern already supports custom values — do not invent a new API.

---

<a id="w3-7"></a>
## W3-7 — Structure validation only runs on manual button

**Severity:** P1
**Files:**
- `frontend/src/components/authoring/ControlledStructuredContentEditor.vue` (`validateStructure`)
- `frontend/src/components/templates/TemplateAuthoringBindingEditor.vue` (`defineExpose`)
- `createTemplateAuthoringBindingsSaveFlow` (grep)

### Implement

Expose `validateStructure` through the binding editor and call it on save. On failure:
block save (or warn with confirm — prefer **block** for unresolved `${var}` / bad
expressions already detected by the client validator). Red test: save flow invokes
validate and aborts when invalid.

### Do NOT

- Do not remove the manual Validate button.

---

## Optional follow-up (same wave only if time — else queue note)

**W3-X — Split `TemplateTestDataSetEditDialog.vue` (CRCH W4-3)** — 708 lines, JSON fallback
for LIST/OBJECT. Only tackle if Leaf 3 is still green and Batch Recommendation allows
merge; otherwise leave as a written residual pointing at CRCH W4-3.

**W3-Y — `${...}` autocomplete beyond `ConditionExpressionInput` (CRCH W4-4)** — same rule.

---

## W3 exit criteria

| # | Criterion |
| --- | --- |
| 1 | Top-level content module insert works; list is editable or removed from toolbar |
| 2 | Style apply is scoped; inline insert respects focus |
| 3 | Save runs structure validation |
| 4 | FE gates + E2E nested editor smoke + UIUX binding editor screenshot |

## Closeout

Standard FOS closeout. TM **#173** → done. Do not auto-activate Leaf 4.


## Closeout evidence

- Gates: [evidence/fos-authoring-blocks-work/gates.md](../evidence/fos-authoring-blocks-work/gates.md)
- Behavior: [docs/behavior/fos-authoring-blocks-work.md](../../behavior/fos-authoring-blocks-work.md)
- E2E/UIUX/deploy: **BLOCKED** (daemon up; stack not built this session).
