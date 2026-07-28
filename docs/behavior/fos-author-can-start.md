# Behavior — FOS-W2 Author can start & navigate

| Field | Value |
| --- | --- |
| **Slice** | `fos-author-can-start` |
| **Task Master** | **#172** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **true** |
| **delivery_lane** | **full** |
| **Source** | [FOS-W2-author-can-start.md](../plan/detail/FOS-W2-author-can-start.md) |

## Goal

Authors can create a template from approved letterheads after Dashboard, escape the
post-create authoring guide via lifecycle/workspace navigation, and follow coherent
deep-links (API routes panel, breadcrumbs, layout-placeholder deep link, dirty-guard).

## Acceptance scenarios

### BDD-FOS-W2-001 — Create dialog fetches approved letterheads
**Given** the masters store list is empty (Dashboard workflow overwrite)  
**When** the New Template dialog opens  
**Then** `fetchAllMasters` is called with `status: APPROVED` (or equivalent) and options populate from the fetch

### BDD-FOS-W2-002 — Lifecycle stepper escapes authoring guide
**Given** query has `authoringGuide=1&authoringGuideStep=master`  
**When** the lifecycle stepper navigates to Testing  
**Then** the resulting query omits guide keys and includes the target workspace tab

### BDD-FOS-W2-003 — Guide order is master → variables → bindings → preview
**Given** the authoring path guide steps  
**When** steps are listed  
**Then** order is `master`, `variables`, `bindings`, `preview`

### BDD-FOS-W2-004 — Version lines open API settings panel `routes`
**Given** a published version line with releaseVersion  
**When** the author opens API settings from the row  
**Then** the path uses `panel=routes`

### BDD-FOS-W2-005 — API package settings breadcrumb trail
**Given** path `/api/packages/:templateId/settings`  
**When** breadcrumbs build  
**Then** trail is Templates → package hub → API settings (not Home-only)

### BDD-FOS-W2-006 — Missing `anchorId` deep link warns
**Given** bindings loaded and `?anchorId=` does not match any row  
**When** deep-link open runs  
**Then** a warning message is shown

### BDD-FOS-W2-007 — Guide has one dismiss control
**Given** the authoring path guide is visible  
**When** actions render  
**Then** Skip is not offered as a duplicate of Dismiss (single dismiss)

### BDD-FOS-W2-008 — Dirty guard blocks workspace tab switch
**Given** bindings form is dirty  
**When** the author switches Design → Testing and rejects leave  
**Then** the workspace tab does not change
