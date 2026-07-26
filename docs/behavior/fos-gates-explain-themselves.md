# Behavior — FOS-W4 Gates & actions explain themselves

| Field | Value |
| --- | --- |
| **Slice** | `fos-gates-explain-themselves` |
| **Task Master** | **#174** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **true** |
| **delivery_lane** | **full** |
| **Source** | [FOS-W4-gates-explain-themselves.md](../plan/detail/FOS-W4-gates-explain-themselves.md) |

## Goal

Disabled lifecycle actions explain why; variable delete shows reference impact;
deep-linked Testing eligibility loads; form dialogs do not discard on overlay click;
coverage and binding validation surfaces are actionable.

## Acceptance scenarios

### BDD-FOS-W4-001 — Delete variable shows impact counts
**Given** a variable referenced by a binding  
**When** the author confirms delete  
**Then** the confirm dialog includes a non-zero binding count (and rules / test sets / compute refs)

### BDD-FOS-W4-002 — Deep-linked Testing refreshes eligibility
**Given** the workspace opens with `workspaceTab=testing`  
**When** the testing action rail mounts  
**Then** submit-for-test eligibility is fetched once and load errors are shown near the rail

### BDD-FOS-W4-003 — Approval / Publish disabled tooltips
**Given** submit-for-approval or publish is blocked  
**When** the author hovers the disabled button  
**Then** a tooltip shows the first blocking gate reason or load error

### BDD-FOS-W4-004 — Binding validation lists per-anchor issues
**Given** validation returns invalid bindings  
**When** the bindings list shows the summary  
**Then** each invalid anchor is listed and can focus that binding

### BDD-FOS-W4-005 — Coverage uncovered lists are actionable
**Given** uncovered anchors or variables  
**When** the coverage panel renders  
**Then** scope type is translated and uncovered entries link to design deep links

### BDD-FOS-W4-006 — Fidelity edit-binding never uses storage key
**Given** a fidelity warning with only an artifact storage key hint  
**When** the Edit binding link is built  
**Then** `anchorId` is never the storage key; Artifact column shows a friendly label

### BDD-FOS-W4-007 — New version respects whole-collection in-flight
**Given** an in-flight draft exists (even on another page of version lines)  
**When** Create from latest release is shown  
**Then** the action is disabled with tooltip “A draft version already exists”

### BDD-FOS-W4-008 — Form dialogs ignore overlay click
**Given** a form-bearing create/edit dialog is open  
**When** the overlay is clicked  
**Then** the dialog stays open (`close-on-click-modal=false`)

### BDD-FOS-W4-009 — Master review decision guards double-submit
**Given** Approve/Reject is in flight  
**When** the review dialog is open  
**Then** the decision button is loading and disabled

### BDD-FOS-W4-010 — Coverage load error ≠ empty
**Given** coverage fetch fails  
**When** the panel renders  
**Then** a LoadErrorPanel with retry is shown (not an empty success state)

### BDD-FOS-W4-011 — Informational publish-gate tags keep ready/pending
**Given** a publish-gate item is informational  
**When** the checklist renders  
**Then** Informational appears alongside Ready or Pending
