# FOS-W4 — Gates & actions explain themselves

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W4
**Status:** **Done**
**Slice id:** `fos-gates-explain-themselves` · worktree `../DGE-fos-gates-explain-themselves` · branch `feat/fos-gates-explain-themselves`
**Task Master:** **#174**
**Audit baseline:** `f29211c5`
**delivery_lane:** **full**

**Origin audit ids:** A3, A8, A13, A14, A15, A19, A20, A27, D10, D12, D13, D14

---

## Before you write any code

```powershell
git fetch origin
git worktree add "..\DGE-fos-gates-explain-themselves" -b feat/fos-gates-explain-themselves origin/main
```

### Task order

| Order | Task | Sev |
| --- | --- | --- |
| 1 | [W4-1](#w4-1) Variable delete impact preview | **P0** |
| 2 | [W4-2](#w4-2) Submit-for-test eligibility on deep link | **P0** |
| 3 | [W4-3](#w4-3) Tooltips on Submit-for-approval + Publish | **P1** |
| 4 | [W4-4](#w4-4) Binding validation shows per-anchor issues | **P1** |
| 5 | [W4-5](#w4-5) Coverage panel actionable uncovered list | **P1** |
| 6 | [W4-6](#w4-6) Fidelity "Edit binding" uses real anchor id | **P1** |
| 7 | [W4-7](#w4-7) Version-lines in-flight from whole collection | **P1** |
| 8 | [W4-8](#w4-8) Form dialogs: no click-outside discard | **P1** |
| 9 | [W4-9](#w4-9) Master review submit loading guard | **P1** |
| 10 | [W4-10](#w4-10) Coverage load-error distinct from empty | **P1** |
| 11 | [W4-11](#w4-11) Informational publish-gate tags show ready/pending | **P2** |

---

<a id="w4-1"></a>
## W4-1 — Delete variable has no impact analysis

**Severity:** P0
**File:** `frontend/src/components/templates/useTemplateVariableTreePanel.ts`
(`handleDeleteVariable`)

### Current behaviour

Rename runs `analyzeVariableRenameImpact(...)`. Delete only `confirmAction({ titleKey: … })`
with no reference counts.

### Implement

Reuse the rename impact analyzer (or a shared "references" helper if extractable without
drive-by). Confirmation must list counts for bindings / rules / test sets / compute refs.
Block delete when impact > 0 unless the user confirms a stronger second step that already
exists in rename cascade — **prefer the same cascade UX as rename** if safe; otherwise
show counts and require typed confirm via existing `useConfirmAction` reason pattern.

Red test: variable referenced by a binding → delete confirm shows non-zero binding count.

---

<a id="w4-2"></a>
## W4-2 — Submit-for-test disabled with empty tooltip on deep link

**Severity:** P0
**Files:** `frontend/src/views/templates/detail/useTemplateDetailDevWorkspace.ts`,
`frontend/src/composables/useSubmitTestEligibility.ts`

### Current behaviour

```ts
watch(activeWorkspaceTab, tab => {
  if (tab === 'testing') void refreshEligibility()
}, { immediate: false })
```

Landing on `?workspaceTab=testing` never fetches; `eligibility === null` → disabled with
empty tooltip; `loadError` is returned but not rendered.

### Implement

Set `{ immediate: true }` (or refresh on mount). Render `loadError` inline near the action
rail. Red test: mount with testing tab active → `refreshEligibility` called once.

---

<a id="w4-3"></a>
## W4-3 — Approval/Publish buttons have no disabled tooltip

**Severity:** P1
**File:** `frontend/src/views/templates/detail/TemplateDetailDevWorkspaceActions.vue`

### Implement

Wrap Submit-for-approval and Publish in the same `el-tooltip` pattern as Submit-for-test,
content from first blocking gate item / load error. Mirror D10.

---

<a id="w4-4"></a>
## W4-4 — Binding validation is only a count summary

**Severity:** P1
**File:** `frontend/src/components/templates/TemplateAuthoringBindingsList.vue`

### Implement

Render per-anchor issues already present on `BindingValidationResult` as a list; each row
opens/focuses that binding if a focus helper exists. Add tooltip on Validate when
`configuredBindingCount === 0`.

---

<a id="w4-5"></a>
## W4-5 — Coverage threshold shows raw GLOBAL/GROUP; uncovered list inert

**Severity:** P1
**File:** `frontend/src/components/templates/TemplateCoveragePanel.vue`

### Implement

Translate `scopeType`; expand uncovered lists by default; make each entry a link to
bindings/variables deep links (patterns already used elsewhere — grep `anchorId=`).

---

<a id="w4-6"></a>
## W4-6 — Fidelity Edit-binding link passes storage key as anchorId

**Severity:** P1
**Files:** `frontend/src/components/authoring/FidelityWarningList.vue` (`bindingEditLink`),
`frontend/src/components/templates/useTemplatePreviewPanel.ts` (`artifactHint`)

### Implement

Build edit links only from a real anchor id (`warning.location` or dedicated field — read
warning payload). Never pass `artifactStorageKey` as `anchorId`. Friendly artifact label
in the Artifact column.

---

<a id="w4-7"></a>
## W4-7 — "New version" hides based on current page only

**Severity:** P1
**File:** `frontend/src/components/templates/useVersionLinesPanel.ts`
(`hasInFlightLine`, `showCreateFromLatestRelease`)

### Implement

Derive in-flight from a whole-collection signal (API flag or fetch all in-flight — prefer
an existing endpoint field; do not invent pagination hacks). Replace hide with disabled +
tooltip "A draft version already exists".

---

<a id="w4-8"></a>
## W4-8 — Form dialogs dismiss on overlay click

**Severity:** P1
**Files:** form-bearing dialogs lacking `:close-on-click-modal="false"`, especially
`ContentModuleVersionDialog.vue`, `ContentModuleCreateDialog.vue`,
`TemplateCreateDialog.vue`, `LegalHoldCreateDialog.vue`, `UserFormDialog.vue`

### Implement

Set `:close-on-click-modal="false"` on these (match `DirtyGuardConfirmDialog`). Where a
dirty guard already exists, route through it. Add a focused test on the clause version
dialog.

---

<a id="w4-9"></a>
## W4-9 — Master review Approve/Reject double-submit

**Severity:** P1
**Files:** `frontend/src/components/masters/MasterReviewDialog.vue`,
`frontend/src/views/masters/createMasterRevisionDetailActions.ts`

### Implement

Pass `submitting` / store flag into the dialog; bind `:loading` + `:disabled` on the
decision button.

---

<a id="w4-10"></a>
## W4-10 — Coverage fetch failure looks like empty coverage

**Severity:** P1
**File:** `TemplateCoveragePanel.vue`

### Implement

Add a `LoadErrorPanel` branch with retry (same pattern as list consoles). Keep toast if
desired, but the panel must not look empty.

---

<a id="w4-11"></a>
## W4-11 — Informational publish-gate tags hide ready/pending

**Severity:** P2
**File:** `frontend/src/views/templates/detail/TemplateDetailApprovalPublishPane.vue`

### Implement

Show ready/pending tag alongside Informational.

---

## W4 exit criteria

Disabled actions explain why; delete variable shows impact; deep-linked Testing eligibility
loads; dialogs do not discard on overlay; FE gates + E2E for delete-impact + eligibility
tooltip; UIUX action rail screenshots.

## Closeout

Standard. TM **#174** → done.
