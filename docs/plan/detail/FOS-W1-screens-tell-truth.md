# FOS-W1 — Screens tell the truth

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W1
**Status:** **Not Started**
**Slice id:** `fos-screens-tell-truth` · worktree `../DGE-fos-screens-tell-truth` · branch `feat/fos-screens-tell-truth`
**Task Master:** **#171** (pending — activate only when this leaf becomes sole-active)
**Audit baseline commit:** `f29211c5` — locate code by quoted snippet, not line number
**delivery_lane:** **full** (user-facing string + filter/label behaviour)

**Origin audit ids:** D1, D4, D5, D6, D7, D16, A4, A22, A23, A24

---

## Before you write any code

1. Read FOS program §0 (worktree, TDD, gates, no-new-features).
2. Confirm host sole-active is free. Prefer finishing CRCH W0+W1 first if it is still Not Started and the orchestrator scheduled it ahead of FOS.
3. Create worktree:

```powershell
git fetch origin
git worktree add "..\DGE-fos-screens-tell-truth" -b feat/fos-screens-tell-truth origin/main
```

4. **Do not** split `en.ts` / `zh-CN.ts` into multiple files — that is AI-SCALE **#168**. Only **add/fix keys**.

### Task order

| Order | Task | Sev | Primary surface |
| --- | --- | --- | --- |
| 1 | [W1-1](#w1-1) Fidelity warning labels (18 missing) | **P0** | `FidelityWarningList.vue` + locales |
| 2 | [W1-2](#w1-2) Publish-gate checklist labels + Go-fix | **P0** | `en.ts` `api.publishGate` / `checkCodes` + `publishGateGoFixLink.ts` |
| 3 | [W1-3](#w1-3) Audit event type catalogue (6→45) | **P0** | `useAuditEventTypeOptions.ts` |
| 4 | [W1-4](#w1-4) Remove phantom `ARCHIVED` master status | **P0** | `useTableFilterOptions.ts` |
| 5 | [W1-5](#w1-5) Invocation status/kind labels + colour | **P0** | `ApiInvocationsView.vue` / `TemplateInvocationsPanel.vue` |
| 6 | [W1-6](#w1-6) Variable type / container type labels | **P1** | `TemplateVariableTreePanel.vue` |
| 7 | [W1-7](#w1-7) Terminology + help copy (anchor / clause) | **P1** | `en.ts` + `zh-CN.ts` only |
| 8 | [W1-8](#w1-8) `INVOCATION_RETENTION` domain label | **P1** | `apiPolicy.detail.domains.*` |
| 9 | [W1-9](#w1-9) Backend `api.error.template.invalidRulesJson` | **P0** | `messages_en.properties` + `apiErrorEn.ts` / `apiErrorZhCN.ts` |

---

<a id="w1-1"></a>
## W1-1 — Fidelity warnings render as raw `generation.warning.fidelity.*` keys

**Severity:** P0
**Files:** `frontend/src/components/authoring/FidelityWarningList.vue`,
`frontend/src/i18n/locales/en.ts`, `frontend/src/i18n/locales/zh-CN.ts`
**Test:** extend existing `FidelityWarningList` / preview panel Vitest (find colocated `*.test.ts`)

### Current behaviour

`humanWarningLabel()` falls back to `return warning.messageKey` when
`templates.preview.fidelityMessages.<CODE>` is missing. Backend emits ~22
`generation.warning.fidelity.*` codes; frontend covers only a handful
(`IMAGE_SCALING_ADJUSTED`, `MISSING_STYLE_REFERENCE`, `UNRESOLVED_VARIABLE`,
`UNSUPPORTED_NODE`).

### Step 1 — Red test

Mount the list with a warning whose `code` / `messageKey` maps to
`SEAL_OUTSIDE_AUTHORIZED_AREA` (or the actual enum name in
`RuntimeFidelityWarningMapper` / fidelity warning codes). Assert the rendered
text is **not** a string starting with `generation.warning.fidelity.`.

### Step 2 — Implement

1. Grep backend for `"generation.warning.fidelity.` and list every distinct code.
2. Add `templates.preview.fidelityMessages.<CODE>` for **every** code in **both** locales.
   English must be actionable for a non-IT author (what is wrong + what to do).
3. Keep the technical `code` column inside the existing "technical details" disclosure.

### Do NOT

- Do not add a `message` field to the backend DTO in this leaf (optional later).
- Do not remove the technical disclosure.

---

<a id="w1-2"></a>
## W1-2 — Publish-gate rows show machine `summary` with no Go-fix

**Severity:** P0
**Files:**
- `frontend/src/i18n/locales/en.ts` + `zh-CN.ts` (`api.publishGate.*`, `templates.publishGate.checkCodes.*`)
- `frontend/src/utils/publishGateGoFixLink.ts` (`PUBLISH_GATE_GO_FIX_QUERY`)
- `frontend/src/utils/templateLifecycleDecisionForm.ts` (`mapPublishGateChecklistItems`) — only if it drops `summary`

**Test:** unit test for `resolvePublishGateItemLabel` / Go-fix resolver (create if missing).

### Current behaviour

Backend emits keys such as:

- `api.publishGate.contentModuleNestingCycle.blocked`
- `api.publishGate.contentModuleLocaleMismatch.blocked`
- `api.publishGate.contentModuleEffectiveNotStarted.blocked`
- `api.publishGate.compositionInclusionReferenceInvalid.blocked`
- `api.publishGate.paginationDeltaBudget.blocked`

and check codes for nesting/locale/inclusion. Frontend keys are missing; label falls
through to raw `summary` like
`authorWordPageCount=3,pdfPages=5,delta=2,budget=1,outcome=BLOCKER`.
`PUBLISH_GATE_GO_FIX_QUERY` has no mapping for those codes.

### Step 1 — Red test

Given a checklist item with `messageKey` =
`api.publishGate.contentModuleNestingCycle.blocked`, the display label must be a
human sentence (not containing `=` key/value pairs), and Go-fix must resolve to a
dev-workspace query that includes `designTab=contentModules` (or the current
content-modules sub-tab key — read `templateAuthoringSubTabs.ts` first).

### Step 2 — Implement

1. Grep `PublishGateCheckCode` / `PublishGateCheckItem*Support` for every
   `api.publishGate.*` and `checkCodes` value.
2. Add missing `ready`/`blocked` keys + `checkCodes.*` in **both** locales.
3. Map nesting/locale/inclusion codes to Go-fix → content modules; pagination →
   the existing preview/testing surface used by sibling codes.
4. If `mapPublishGateChecklistItems` drops `summary` for non-`API_POLICY` items,
   keep `summary` as **secondary detail** under the human label (do not show it alone).

### Do NOT

- Do not change publish-gate evaluation logic on the backend in this leaf.
- Do not invent new gate checks.

---

<a id="w1-3"></a>
## W1-3 — Audit console only lists 6 of ~45 event types

**Severity:** P0
**Files:** `frontend/src/composables/useAuditEventTypeOptions.ts`,
`frontend/src/utils/auditEventLabels.ts`, both locale files under `audit.eventTypes.*`
**Source of truth:** `backend/.../audit/service/ManagementAuditEventTypes.java`

### Step 1 — Red test

Assert `AUDIT_EVENT_TYPE_CODES` length equals the backend catalogue size (read the
Java constants list in the test via a checked-in mirror array, or assert critical
codes like `USER_DELETED`, `LEGAL_HOLD_CREATED`, `API_POLICY_UPDATED` are present
and labelled).

### Step 2 — Implement

1. Expand `AUDIT_EVENT_TYPE_CODES` to the full backend set.
2. Add `audit.eventTypes.<CODE>` for every code in en + zh-CN.
3. Ensure `auditEventLabels` never returns the raw code when a key exists (`te` guard).

### Do NOT

- Do not change backend event emission.
- Do not invent event types not in `ManagementAuditEventTypes`.

---

<a id="w1-4"></a>
## W1-4 — Phantom `ARCHIVED` master status filter

**Severity:** P0
**File:** `frontend/src/composables/useTableFilterOptions.ts`
**Evidence:** `MASTER_STATUSES` includes `'ARCHIVED'`; `MasterDocumentStatus.java` has
no `ARCHIVED`; `masters.status.ARCHIVED` locale keys are absent → raw `ARCHIVED` label.

### Implement

Remove `'ARCHIVED'` from `MASTER_STATUSES`. Add/adjust a unit test that the master
status options equal the backend enum names only.

### Do NOT

- Do not add an ARCHIVED lifecycle to the backend.

---

<a id="w1-5"></a>
## W1-5 — Invocation status/kind shown as raw enums

**Severity:** P0
**Files:**
- `frontend/src/views/api/ApiInvocationsView.vue`
- `frontend/src/components/templates/TemplateInvocationsPanel.vue`
- `frontend/src/composables/useCrossPackageInvocations.ts`
- `frontend/src/composables/useTemplateInvocationsPanel.ts` (filter option builders)
- locales: add `apiPolicy.invocationsPage.status.*` and `.kind.*` (or reuse an existing
  namespace if one already exists — grep first)

### Implement

1. List distinct status/kind values from backend enums / OpenAPI.
2. Add labels in both locales.
3. Render status as coloured `el-tag` (success / warning / danger) using existing OA tag
   patterns from other status columns.
4. Filter dropdowns use translated labels, raw values as `value`.

### Do NOT

- Do not change invocation persistence or search API.

---

<a id="w1-6"></a>
## W1-6 — Variable type labels are SCREAMING_CASE

**Severity:** P1
**File:** `frontend/src/components/templates/TemplateVariableTreePanel.vue`

### Implement

Mirror the existing `piiCategories` pattern: add
`templates.authoring.variableTypes.<TYPE>` and container-type keys; use `t(...)` for
option labels and tags.

---

<a id="w1-7"></a>
## W1-7 — Terminology drift + developer help copy

**Severity:** P1
**Files:** `en.ts` + `zh-CN.ts` only

### Implement

1. Pick **one** persona term for master placeholders on template surfaces:
   **"layout placeholder"** (already used in templates copy). Align masters list/detail
   strings that say only "Anchors" / "Anchor ID" toward the same persona wording
   **without** renaming API fields or route keys.
2. Rewrite the two help strings that mention `contentModuleRef` / `referenceKey` in
   developer voice (approx. en.ts lines calling out those tokens — grep
   `contentModuleRef`) into business language ("registered clause", "clause key") while
   keeping the same instructions.

### Do NOT

- Do not rename JSON property names, OpenAPI fields, or test ids.

---

<a id="w1-8"></a>
## W1-8 — Policy impact shows raw `INVOCATION_RETENTION`

**Severity:** P1
**Files:** locales `apiPolicy.detail.domains.INVOCATION_RETENTION` (+ hint if siblings have hints)

### Implement

Add the missing domain label (and hint key if `ApiPolicyImpactPreviewPanel` looks up
`hints.*`). Grep `API_POLICY_DOMAINS` / `changedAreas` to ensure no other domain is missing.

---

<a id="w1-9"></a>
## W1-9 — `api.error.template.invalidRulesJson` missing everywhere

**Severity:** P0
**Files:**
- `backend/src/main/resources/i18n/messages_en.properties`
- `frontend/src/i18n/catalogs/apiErrorEn.ts`
- `frontend/src/i18n/catalogs/apiErrorZhCN.ts` (or whatever the zh catalogue file is named — grep)

**Evidence:** thrown from `TemplateViewMapper` / `TemplateBindingMutationSupport` /
`CompositionInclusionRuleService` with that exact key; absent from properties + FE catalogues.

### Implement

Add English + Chinese messages consistent with sibling `api.error.template.*` tone.
Add a unit/catalogue parity test if one already exists for api.error keys (LR-C11 style);
extend it rather than inventing a new harness.

---

## W1 exit criteria

| # | Criterion |
| --- | --- |
| 1 | W1-1…W1-9 each observed red then green (or W1-4/W1-8 as focused assertions) |
| 2 | `pnpm -C frontend lint type-check test build` green |
| 3 | Backend catalogue/message change: `mvn -B -ntp -f backend/pom.xml verify` green |
| 4 | Playwright: open a preview with a fidelity warning fixture **or** publish-gate panel with a nesting blocker fixture — assert **no** visible `generation.warning.fidelity.` / `danglingReferenceKeys=` / `authorWordPageCount=` in the primary label |
| 5 | UIUX evidence: screenshot publish-gate + fidelity list, REDBC + GREENBC, desktop |
| 6 | Both locales updated for every new key; grep confirms no orphaned removals |

## Closeout order

1. Queued docker deploy evidence
2. Architecture review → optional code-quality
3. `integration-merger` → remove worktree
4. `post-task-doc-sync` on MAIN (program W1 → Done; ledger; TM #171 → done)
5. `post-task-commit-review` on MAIN

Do **not** mark FOS program Done. Do **not** activate Leaf 2 automatically.
