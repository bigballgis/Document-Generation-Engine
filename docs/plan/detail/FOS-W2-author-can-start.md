# FOS-W2 — Author can start & navigate

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W2
**Status:** **Not Started**
**Slice id:** `fos-author-can-start` · worktree `../DGE-fos-author-can-start` · branch `feat/fos-author-can-start`
**Task Master:** **#172**
**Audit baseline:** `f29211c5` — locate by snippet
**delivery_lane:** **full**
**Supersedes:** CRCH §7 W4-1/W4-2 navigation items that overlap A2/A21 (do not also execute CRCH W4)

**Origin audit ids:** A1, A2, A16, A17, A18, A21, A25, A26

---

## Before you write any code

```powershell
git fetch origin
git worktree add "..\DGE-fos-author-can-start" -b feat/fos-author-can-start origin/main
```

Leaf 1 (`fos-screens-tell-truth`) should be Done first unless the orchestrator explicitly
promotes this leaf for a blocked-create-path hotfix.

### Task order

| Order | Task | Sev | Primary file |
| --- | --- | --- | --- |
| 1 | [W2-1](#w2-1) Create-template loads approved letterheads | **P0** | `useTemplateCreateDialog.ts` |
| 2 | [W2-2](#w2-2) Post-create guide must not trap lifecycle/nav | **P0** | `TemplateDetailDevWorkspace.vue` / `templateDevWorkspaceTabs.ts` |
| 3 | [W2-3](#w2-3) Align guide order with tab order | **P1** | `templateAuthoringPathGuide.ts` |
| 4 | [W2-4](#w2-4) Version-lines → API settings `panel=routes` | **P1** | `VersionLinesTable.vue` |
| 5 | [W2-5](#w2-5) API package settings breadcrumb | **P1** | `breadcrumbTrail.ts` |
| 6 | [W2-6](#w2-6) `anchorId` deep link feedback | **P1** | `useTemplateAuthoringBindingsPanel.ts` |
| 7 | [W2-7](#w2-7) Guide Skip vs Dismiss + completed ticks | **P2** | `AuthoringPathGuide.vue` |
| 8 | [W2-8](#w2-8) Dirty guard on in-page workspace tab switches | **P2** | `useDirtyGuard` + workspace tab router |

---

<a id="w2-1"></a>
## W2-1 — New template dialog shows empty letterhead list after Dashboard

**Severity:** P0 — author cannot start
**Files:** `frontend/src/components/templates/useTemplateCreateDialog.ts`
(and the dialog Vue if empty-state UI lives there)
**Store:** `frontend/src/stores/createMastersCatalogActions.ts` (`fetchAllMasters`)

### Current behaviour

```ts
const approvedMasters = computed(() =>
  mastersStore.masters.filter((m) => m.status === 'APPROVED' /* … */),
)
```

The composable never fetches. Dashboard
`fetchDashboardWorkflowMasters` overwrites `masters` with pending/draft/rejected only
(or `[]`). Visiting Templates → New without first opening the Letterheads list yields an
empty select.

### Step 1 — Red test

In the create-dialog test, stub the store with `masters: []` and a spy on
`fetchAllMasters`. Open/mount the dialog. Expect `fetchAllMasters` called with a filter
that requests `APPROVED` (match existing `fetchAllMasters` signature — read it first).
Expect the select options to populate from the fetch result, not from stale dashboard rows.

### Step 2 — Implement

1. On dialog open / composable setup, call
   `mastersStore.fetchAllMasters({ status: 'APPROVED' })` (or the equivalent options object
   already used by `useTemplateImportDialog` / master list — **copy that call shape**).
2. Show a translated empty-state hint when the fetch returns zero approved masters
   (e.g. "No approved letterheads yet — ask a reviewer to approve a letterhead first").
3. Keep group-isolation behaviour of the existing fetch (do not bypass access rules).

### Do NOT

- Do not persist masters across sessions with a new plugin.
- Do not change dashboard workflow master fetch semantics for the dashboard widgets.

---

<a id="w2-2"></a>
## W2-2 — Post-create landing traps the author (lifecycle stepper no-ops)

**Severity:** P0
**Files:**
- `frontend/src/views/templates/detail/TemplateDetailDevWorkspace.vue`
- `frontend/src/views/templates/templateDevWorkspaceTabs.ts` (`buildDevWorkspaceQuery`,
  `stripAuthoringPathGuideQuery` if present)
- post-create path builder (`buildPostCreateAuthoringPath` — grep)

### Current behaviour

Post-create lands with `authoringGuide=1&authoringGuideStep=master`. The workspace tab
shell is hidden while the master guide step is active
(`v-if="!showAuthoringPathMasterPanel"`). Lifecycle stepper navigation calls
`buildDevWorkspaceQuery`, which strips tab keys but **keeps**
`authoringGuideStep=master`, so clicks do nothing.

### Chosen resolution

When the user clicks the lifecycle stepper (or any primary workspace navigation),
**strip** `authoringGuide` + `authoringGuideStep` (reuse `stripAuthoringPathGuideQuery`
if it exists; otherwise add a shared helper next to `buildDevWorkspaceQuery`).

Keep the guide usable via its own Next/Skip; do not remove the guide feature.

### Step 1 — Red test

Simulate active query `{ authoringGuide: '1', authoringGuideStep: 'master' }` and invoke
the stepper navigate helper toward Testing/Design. Expect resulting query to **omit**
guide keys and to include the target workspace tab.

### Do NOT

- Do not delete `AuthoringPathGuide`.
- Do not change default landing away from the guide on first create without also fixing
  escape hatches (this task *is* the escape hatch).

---

<a id="w2-3"></a>
## W2-3 — Guide order contradicts tab order

**Severity:** P1
**Files:** `frontend/src/utils/templateAuthoringPathGuide.ts` (`AUTHORING_PATH_GUIDE_STEPS`),
possibly guide copy in locales

### Current behaviour

Guide: `master → bindings → variables → preview`.
Sub-tabs display: `variables → contentModules → bindings` (and Design defaults to
`bindings` per CE-U16).

### Chosen resolution

Align guide sequence to: `master → variables → bindings → preview` (content modules stay
reachable from Design tabs; do not add a fifth guide step unless copy already requires it).

Update any E2E that asserts the old guide order (grep `authoringGuideStep`).

### Do NOT

- Do not change `DEFAULT_TEMPLATE_AUTHORING_SUB_TAB` in this task unless tests prove it
  must move — prefer aligning the **guide** to the existing default landing.

---

<a id="w2-4"></a>
## W2-4 — Version line opens API settings with unknown panel `route`

**Severity:** P1
**File:** `frontend/src/components/templates/VersionLinesTable.vue` (`openApiPerspective`)
**Shell:** `frontend/src/views/api/ApiPackageSettingsShellView.vue` (`KNOWN_PANELS` includes
`'routes'`, not `'route'`)

### Implement

Change `panel: 'route'` → `panel: 'routes'`. Add/adjust a unit test on the path builder.

---

<a id="w2-5"></a>
## W2-5 — API package settings breadcrumb is only "Home"

**Severity:** P1
**File:** `frontend/src/navigation/breadcrumbTrail.ts`

### Implement

Add a pattern for `/api/packages/:templateId/settings` returning:
Home → Templates → {template hub or package label} → API settings.
Follow existing `DETAIL_PREFIXES` / pattern style in the same file. Add a unit test.

---

<a id="w2-6"></a>
## W2-6 — `?anchorId=` deep link fails silently

**Severity:** P1
**File:** `frontend/src/composables/useTemplateAuthoringBindingsPanel.ts`
(`openAnchorFromQueryIfPresent`)

### Current behaviour

```ts
const row = …find(c => c.anchorId === anchorId)
if (!row) { return }  // silent
// on success: delete nextQuery.anchorId
```

### Implement

1. If not found after bindings load: show `ElMessage.warning` with a translated key
   explaining the placeholder was not found.
2. Prefer keeping `anchorId` in the URL until successfully opened **or** document why
   removal is required; if removal stays, re-apply when the bindings sub-tab remounts
   (watch query).
3. Unit test both found and not-found paths.

---

<a id="w2-7"></a>
## W2-7 — Guide Skip and Dismiss are identical; ticks lie

**Severity:** P2
**File:** `frontend/src/components/templates/AuthoringPathGuide.vue`

### Implement

Keep **one** dismiss control (or make Skip = advance without complete, Dismiss = exit —
pick one coherent pair and match i18n). Derive `completed` from real signals already
available in the parent (variables count / bindings count / preview present) **or** remove
the fake completed styling. Prefer minimal change: one dismiss + stop marking future steps
completed by index alone.

---

<a id="w2-8"></a>
## W2-8 — Switching Design→Testing does not dirty-guard

**Severity:** P2
**Files:** workspace tab switch sites that `router.replace({ query: buildDevWorkspaceQuery(...) })`,
`useDirtyGuard.ts`, bindings edit actions

### Implement

Before changing `workspaceTab` via in-page navigation, call the existing
`dirtyGuardRequestLeave` (same as Back). Add a Vitest covering dirty=true → tab switch
cancelled when user rejects.

### Do NOT

- Do not reimplement a second confirm dialog component.

---

## W2 exit criteria

| # | Criterion |
| --- | --- |
| 1 | From Dashboard (no prior Letterheads visit), New Template shows approved letterheads or a clear empty hint |
| 2 | Post-create: lifecycle stepper or workspace nav escapes the guide |
| 3 | FE gates green; E2E covers create-from-dashboard + escape guide |
| 4 | UIUX screenshots: create dialog + post-create workspace, both brands |

## Closeout

Same as W1 (deploy → reviews → merge → MAIN doc-sync → commit-review). Mark TM **#172** Done.
Do not auto-activate Leaf 3.
