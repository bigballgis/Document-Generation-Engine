# CRCH-W1 — Preview consolidation

**Program:** [CRCH](../core-render-compute-hardening-program.md)
**Wave:** W1
**Status:** **Done**
**Delivered together with:** [CRCH-W0](CRCH-W0-rendering-correctness.md) — one merged slice, one worktree
**Slice id:** `render-p0-preview-dedupe` (same worktree as W0 — do **not** create a second one)
**Audit baseline commit:** `df9a5b7d` — locate code by the quoted snippet, not by line number

**Origin:** The user identified this himself — "模板编排页面的预览功能有两个版面，明显重复".
The audit confirmed it and found it is a component nested inside a component that already
contains it, causing the same PDF to be fetched from storage twice.

---

## Before you write any code

- Work in the **same** worktree created for W0: `../DGE-render-p0-preview-dedupe`.
- Finish W0 first. W0 is backend-only, W1 is frontend-only, so they do not conflict, but the
  gate evidence is produced once for the merged slice.
- Every user-facing string goes in **both** `frontend/src/locales/en.ts` and
  `frontend/src/locales/zh-CN.ts`. English is the base language.
- The bank OA visual system is locked — see `.cursor/skills/frontend-oa-design/SKILL.md`. Do not
  introduce new colours, spacing values, or component patterns; reuse existing tokens.

### Task order

| Order | Task | Severity | Primary file |
| --- | --- | --- | --- |
| 1 | [W1-1](#w1-1) Remove the duplicated PDF viewer | **P0 UX** | `AuthoringPreviewPane.vue` |
| 2 | [W1-2](#w1-2) Prove the PDF is fetched once | **P1** | `AuthoringPreviewPane.test.ts` |
| 3 | [W1-3](#w1-3) One refresh action with one meaning | **P0 UX** | `TemplatePreviewPanel.vue` |
| 4 | [W1-4](#w1-4) Split batch history from preview history | **P1** | `TemplateDetailTestingTab.vue` |
| 5 | [W1-5](#w1-5) Delete the ghost `testPreview` sub-tab | **P2** | `templateAuthoringSubTabs.ts` |
| 6 | [W1-6](#w1-6) Legacy authoring duplication — **investigate only** | **P2** | report only |

---

<a id="w1-1"></a>
## W1-1 — The same PDF viewer is rendered twice, nested

**Severity:** P0 UX
**Files:** `frontend/src/components/templates/AuthoringPreviewPane.vue`
**Test file:** `frontend/src/components/templates/AuthoringPreviewPane.test.ts` (exists)

### Current behaviour

`AuthoringPreviewPane` renders its own inline PDF section and then embeds
`TemplatePreviewPanel`, which renders a second inline PDF for the same preview:

```vue
<section
  v-if="hasPreview && canShowInlinePdf"
  class="authoring-preview-pane__inline-pdf"
  data-testid="authoring-inline-pdf-section"
>
  <h4 class="authoring-preview-pane__inline-pdf-title">
    {{ t('templates.authoring.inlinePdfTitle') }}
  </h4>
  <InlinePdfPreviewViewer ... />
</section>

<TemplatePreviewPanel
  v-if="hasPreview"
  compact
  :template-id="templateId"
  :bindings="bindings"
  :preview="preview"
/>
```

and inside `TemplatePreviewPanel`:

```vue
<section v-if="canShowInlinePdf" class="preview-inline-pdf" data-testid="preview-inline-pdf-section">
  <h3>{{ t('templates.preview.inlinePdf.title') }}</h3>
  <InlinePdfPreviewViewer :blob="pdfBlob" ... />
</section>
```

Both components independently call the `useInlinePdfPreview` composable with the same
`templateId` and `preview`, and that composable calls
`panelDataStore.downloadPreviewArtifact(templateId, previewId, 'pdf')`. The store action is a
pure pass-through with no caching, so the same PDF is streamed from object storage twice on every
preview.

### Chosen resolution — delete, do not add a flag

Remove the PDF viewer from `AuthoringPreviewPane` and let the nested `TemplatePreviewPanel`
remain the single owner of PDF rendering.

Why this direction rather than hiding the panel's viewer behind a prop:

- It removes code instead of adding a conditional.
- `TemplatePreviewPanel` is also used standalone in the Testing tab. If the panel's viewer were
  the one suppressed, that surface would lose its PDF entirely.
- One `useInlinePdfPreview` instance remains, so the double fetch disappears as a consequence
  rather than needing a cache.

`AuthoringPreviewPane` keeps everything that is genuinely its own: the title, the stale badge, the
boundary notice, the regenerate button, and the empty state.

### Step 1 — Write the failing test

In `AuthoringPreviewPane.test.ts`, with a preview in `SUCCEEDED` state and a PDF artifact key
present, assert there is exactly one inline PDF viewer in the rendered tree:

```ts
expect(wrapper.findAll('[data-testid="authoring-inline-pdf-section"]')).toHaveLength(0)
expect(wrapper.findAllComponents(InlinePdfPreviewViewer)).toHaveLength(1)
```

Follow the existing tests in that file for mounting, the Pinia store stub, and the i18n setup.

**Expected red symptom:** two `InlinePdfPreviewViewer` instances are found, and
`authoring-inline-pdf-section` is present.

### Step 2 — Implement

In `AuthoringPreviewPane.vue`:

1. Delete the `<section class="authoring-preview-pane__inline-pdf">` block including its heading.
2. Delete the `useInlinePdfPreview(...)` call and the destructured
   `inlinePdfLoading`, `inlinePdfError`, `pdfBlob`, `canShowInlinePdf`.
3. Delete the now-unused imports of `InlinePdfPreviewViewer` and `useInlinePdfPreview`.
4. Delete the `&__inline-pdf` and `&__inline-pdf-title` SCSS rules.
5. Remove the now-orphaned i18n key `templates.authoring.inlinePdfTitle` from **both** locale files
   — but first grep the whole `frontend/src` for that key and only remove it if this was the sole
   usage.

Keep the `hasPreview` computed — it still gates `TemplatePreviewPanel` and the `el-empty`
fallback.

### Do NOT

- Do not delete `InlinePdfPreviewViewer.vue` or `useInlinePdfPreview.ts`. Both remain in use by
  `TemplatePreviewPanel`.
- Do not change `TemplatePreviewPanel`'s PDF section — it becomes the single viewer.
- Do not change the regenerate button, the stale badge, or the boundary notice.

---

<a id="w1-2"></a>
## W1-2 — Prove the PDF is fetched exactly once

**Severity:** P1 — regression guard for W1-1
**File:** `frontend/src/components/templates/AuthoringPreviewPane.test.ts`

### Why this is a test and not a cache

Before W1-1 the double fetch was caused by two component instances, not by a missing cache. Once
the duplicate viewer is gone the fetch happens once. Adding a blob cache would introduce memory
and lifetime concerns for no additional benefit.

**Caching `downloadPreviewArtifact` is an explicit non-goal of this task.** Do not add one.

### Implement

Add a test that spies on the store action and asserts a single call after mounting the pane with
a successful preview:

```ts
expect(downloadPreviewArtifactSpy).toHaveBeenCalledTimes(1)
```

Name the test so its intent survives future refactors, e.g.
`fetches the preview PDF exactly once (regression: CRCH-W1-1 duplicated viewer)`.

If you write this test **before** W1-1, it fails with `2` — which is a useful way to demonstrate
the defect. Either order is acceptable as long as you observe it red.

---

<a id="w1-3"></a>
## W1-3 — Two buttons labelled "refresh" that do different things

**Severity:** P0 UX
**Files:** `frontend/src/components/templates/TemplatePreviewPanel.vue`,
`frontend/src/views/templates/detail/TemplateDetailTestingTab.vue`,
`frontend/src/components/templates/AuthoringPreviewPane.vue`
**Test files:** `TemplatePreviewPanel.test.ts`, `AuthoringPreviewPane.test.ts`

### Current behaviour

Two refresh affordances sit in the same sticky pane, visually identical, semantically different:

| Button | Handler | What it actually does |
| --- | --- | --- |
| `AuthoringPreviewPane` → `templates.authoring.previewRefreshNow` | `handlePreviewRefresh` in `createTemplateAuthoringBindingsEditHandlers.ts` → `templatesStore.testGenerate(...)` | **Regenerates** the preview with current bindings |
| `TemplatePreviewPanel` → `templates.preview.refresh` | `refreshPreview` in `useTemplatePreviewPanel.ts` → `panelDataStore.fetchPreview(...)` | **Re-fetches metadata** for the existing preview id |

After editing a binding, clicking the inner one produces no visible change. A user reads that as a
broken product.

### The dead `compact` prop

`TemplatePreviewPanel` declares:

```ts
const props = withDefaults(
  defineProps<{
    templateId: string
    bindings: AnchorBinding[]
    preview: PreviewRecord | null
    compact?: boolean
  }>(),
  {
    compact: false,
  },
)
```

`compact` is passed by both call sites and **never referenced anywhere in the template**. A
declared-and-ignored prop is worse than no prop: it makes call sites believe they configured
something.

### Chosen resolution

Rename `compact` to `embedded` and give it exactly one meaning:
**"I am rendered inside a host preview surface that owns regeneration, so I must not offer my own
refresh action."**

| Call site | Prop | Result |
| --- | --- | --- |
| `AuthoringPreviewPane` | `embedded` | Panel hides its refresh button; the pane's regenerate button is the only action |
| `TemplateDetailTestingTab` | *(none)* | Panel keeps its refresh button — it is standalone there |

Also relabel the panel's own button so it cannot be mistaken for a re-render. Add a new i18n key
(both locales) such as `templates.preview.reloadDetails` with English text along the lines of
"Reload details", and use it instead of `templates.preview.refresh`. Remove the old key only if
no other component uses it — grep first.

### Step 1 — Write the failing tests

In `TemplatePreviewPanel.test.ts`:

```ts
it('hides its own refresh action when embedded in a host preview surface', () => {
  // mount with embedded: true
  expect(wrapper.find('[data-testid="preview-reload-details"]').exists()).toBe(false)
})

it('offers reload-details when standalone', () => {
  // mount without embedded
  expect(wrapper.find('[data-testid="preview-reload-details"]').exists()).toBe(true)
})
```

Add the `data-testid="preview-reload-details"` attribute to the button as part of the
implementation — the existing button has no test id.

**Expected red symptom:** the first test fails because the button renders regardless.

### Step 2 — Implement

1. Rename the prop `compact` → `embedded` in `TemplatePreviewPanel.vue` (keep the
   `withDefaults(..., { embedded: false })` default).
2. Wrap the refresh button in `v-if="!embedded"` and give it the new test id and new label key.
3. Update `AuthoringPreviewPane.vue` to pass `embedded` instead of `compact`.
4. Update `TemplateDetailTestingTab.vue` to pass **neither** — remove the `compact` attribute from
   that usage entirely.
5. Grep `frontend/src` and `frontend/e2e` for `compact` on `TemplatePreviewPanel` and for any test
   asserting the old label, and update them.

### Do NOT

- Do not touch `compact` on other components — `TemplateCoveragePanel` and
  `TemplateChangeDiffPanel` have their own `compact` props that are unrelated and presumably live.
  Verify before assuming, but do not change them in this task.
- Do not remove `refreshPreview` from `useTemplatePreviewPanel.ts`. The standalone Testing usage
  still calls it.
- Do not change what the regenerate button does.

---

<a id="w1-4"></a>
## W1-4 — "Preview runs" is three panels stacked

**Severity:** P1
**Files:** `frontend/src/views/templates/detail/TemplateDetailTestingTab.vue`,
`frontend/src/views/templates/templateTestingSubTabs.ts`
**Test file:** the existing testing-tab spec (find it under `frontend/src/views/templates/`)

### Current behaviour

```vue
<el-tab-pane :label="t(templateTestingSubTabLabelKey('previewRuns'))" name="previewRuns">
  <BatchTestHistoryPanel ... />
  <TemplatePreviewRunHistoryPanel ... />
  <TemplatePreviewPanel v-if="lastPreview" compact class="preview-detail-panel" ... />
</el-tab-pane>
```

Batch-test history and preview-run history are different domains competing for attention inside
one sub-tab, followed by a detail panel for whichever preview is selected.

### Chosen resolution

Give batch tests their own sub-tab.

1. In `templateTestingSubTabs.ts`, add `'batchRuns'` to `TEMPLATE_TESTING_SUB_TABS` and add its
   label key `templates.devWorkspace.testing.subTabs.batchRuns` to the label map.
2. Add that i18n key to **both** locale files. Suggested English: "Batch tests".
3. Move `BatchTestHistoryPanel` into the new `batchRuns` pane.
4. Leave `previewRuns` containing `TemplatePreviewRunHistoryPanel` plus the single
   `TemplatePreviewPanel` detail panel (standalone — no `embedded`, per W1-3).
5. `BatchTestHistoryPanel` emits `open-data-set` and `open-preview`, handled by
   `handleOpenDataSet` and `handleOpenPreview`, both of which switch sub-tabs. They already set
   the target sub-tab explicitly, so they keep working across the new tab boundary — **verify this
   with a test**, do not assume.

### Deep links

`resolveTemplateTestingSubTab` falls back to the default for unrecognised values, so existing
`?testingTab=previewRuns` links keep working and no migration is required. Add a test asserting
`?testingTab=batchRuns` resolves to the new tab.

### Step 1 — Write the failing tests

- `resolveTemplateTestingSubTab('batchRuns')` returns `'batchRuns'`.
- The testing tab renders `BatchTestHistoryPanel` in the `batchRuns` pane and **not** in
  `previewRuns`.
- Clicking through `open-preview` from batch history still lands on `previewRuns` with the preview
  selected.

### Do NOT

- Do not change `BatchTestHistoryPanel` or `TemplatePreviewRunHistoryPanel` internals.
- Do not change the default sub-tab (`dataSets`).
- Do not remove the auto-switch behaviour in the `selectedPreviewId` watcher.

---

<a id="w1-5"></a>
## W1-5 — Delete the ghost `testPreview` authoring sub-tab

**Severity:** P2
**File:** `frontend/src/views/templates/templateAuthoringSubTabs.ts`

### Current behaviour

```ts
export const TEMPLATE_AUTHORING_SUB_TABS = [
  'variables',
  'contentModules',
  'bindings',
  'testPreview',
] as const
```

`testPreview` is filtered out by `templateDevWorkspaceTabs.ts` and is rendered by no component in
either the Design tab or the legacy Authoring tab. The type system and the label map still
advertise a sub-tab that does not exist, which is one of the reasons "where does preview live" has
three different answers.

### Before you remove it — mandatory check

`templateDevWorkspaceTabs.ts` contains logic (around the filter) that maps a legacy
`authoringTab=testPreview` deep link onto the Testing workspace tab. **Read that code first.**

- If such a remap exists, it must keep working after `testPreview` is removed from the union type.
  Handle the string literal explicitly at the query-parsing boundary rather than relying on it
  being a member of `TemplateAuthoringSubTab`.
- Write a test that `?authoringTab=testPreview` still lands the user on the Testing workspace tab
  (or wherever the current behaviour sends them — preserve it exactly).

### Implement

1. Remove `'testPreview'` from `TEMPLATE_AUTHORING_SUB_TABS`.
2. Remove its entry from `TEMPLATE_AUTHORING_SUB_TAB_LABEL_KEYS`.
3. Remove the i18n key `templates.authoring.subTabs.testPreview` from both locale files, after
   grepping to confirm no remaining usage.
4. Fix any resulting type errors — `pnpm -C frontend type-check` will find them.
5. Remove the now-dead filtering in `templateDevWorkspaceTabs.ts` **only** if it exists solely to
   exclude `testPreview`. If it filters for other reasons too, leave it.

### Do NOT

- Do not change `DEFAULT_TEMPLATE_AUTHORING_SUB_TAB` (`bindings`).
- Do not break the legacy deep-link behaviour. Preserving it is the whole risk of this task.

---

<a id="w1-6"></a>
## W1-6 — Legacy authoring duplication — INVESTIGATE AND REPORT ONLY

**Severity:** P2
**Deliverable:** a written finding. **Do not refactor in this slice.**

### Context

Two near-duplicate authoring surfaces exist:

- `frontend/src/views/templates/detail/TemplateDetailDesignTab.vue` (dev-editor workspace) — wires
  the side-by-side preview
- `frontend/src/views/templates/detail/TemplateDetailAuthoringTab.vue` (legacy workspace) — same
  three panels, no preview pane

### What to determine

1. Is the legacy workspace still reachable in the running application? Identify the exact route
   and condition (`isDevEditor` is the discriminator — find what sets it).
2. If unreachable, what would deleting `TemplateDetailLegacyWorkspace` and
   `TemplateDetailAuthoringTab` touch? List the files.
3. If reachable, which users hit it and why does it lack the preview pane — deliberate or
   accidental?

### Why this is not being fixed here

Consolidating two authoring shells is a behaviour change with its own BDD spec, E2E coverage, and
regression surface. Folding it into a slice whose purpose is de-duplicating a preview pane would
make the change set unreviewable.

Write the finding into the report. If it turns out to be genuinely dead code, propose it as the
next queued leaf.

---

## W1 exit criteria

| # | Criterion |
| --- | --- |
| 1 | W1-1 through W1-5 each have a test observed **red first**, then green |
| 2 | `pnpm -C frontend lint`, `type-check`, `test`, `build` all green |
| 3 | Playwright E2E covers: open a binding in the dev editor with a successful preview, and assert **exactly one** PDF viewer is present |
| 4 | UIUX review produces a screenshot of the binding editor showing a single preview pane, at desktop width, for both REDBC and GREENBC brands |
| 5 | Every new or changed string exists in **both** `en.ts` and `zh-CN.ts` |
| 6 | No orphaned i18n keys left behind (grep each key you removed) |
| 7 | W1-6 delivered as a written finding, with no refactor performed |

---

## Merged-slice closeout (W0 + W1)

After both waves are green, in this order:

1. `.\scripts\docker-deploy-queue.ps1` — deploy evidence on the single acceptance stack
2. Architecture review, then optional code-quality review
3. `integration-merger` — merge `feat/render-p0-preview-dedupe` into `main`, then
   `git worktree remove` and `git worktree prune`
4. `post-task-doc-sync` **on MAIN** — update
   [the program document](../core-render-compute-hardening-program.md) (W0/W1 → Done),
   [the plan index](../README.md), and `execution-sync-ledger.md` with gate evidence
5. `post-task-commit-review` **on MAIN** — commit and push

OD-1 / OD-2 are **resolved** (inline seals; stamping default OFF) — see CRCH program D4/D5 and
W0-7. Do not mark the CRCH program Done — W2, W3, W5 remain Not Started (W4 superseded by FOS).
