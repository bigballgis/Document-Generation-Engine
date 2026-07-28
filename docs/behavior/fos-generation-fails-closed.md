# Behavior — FOS-W7 Generation fails closed

| Field | Value |
| --- | --- |
| **Slice** | `fos-generation-fails-closed` |
| **Task Master** | **#177** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **partial** (publish-gate API-policy blocked message) |
| **delivery_lane** | **full** |
| **Source** | [FOS-W7-generation-fails-closed.md](../plan/detail/FOS-W7-generation-fails-closed.md) |

## Goal

Generation, publish-gate evaluation, publish version selection, binding validation,
change-diff rules parsing, and content-module nesting resolution fail closed — no silent
omission, no auto-satisfied gate, no soft-deleted stamp, no pretended persistence.

## Acceptance scenarios

### BDD-FOS-W7-001 — Missing pinned clause fails generation
**Given** a template version references a content-module version id that no longer exists  
**When** pinned structures are resolved for generation  
**Then** the system fails closed with `api.error.validation.contentModuleStructureMissing`  
**And** no structure map is returned without that clause

### BDD-FOS-W7-002 — API-policy publish gate can block
**Given** an API policy skeleton with neither AD groups nor a default route  
**When** the publish gate is evaluated  
**Then** the API_POLICY check is a blocker with `api.publishGate.apiPolicy.blocked`  
**And** publish asserts the gate before materializing/auto-satisfying the skeleton

### BDD-FOS-W7-003 — Publish skips soft-deleted highest dev line
**Given** the highest `dev_version_number` row is soft-deleted and an older unreleased line exists  
**When** publish selects the release candidate  
**Then** the soft-deleted row is not stamped; the non-deleted unreleased line is published

### BDD-FOS-W7-004 — Gate binding validate does not persist
**Given** a binding whose stored `validation_status` is stale relative to computed validity  
**When** publish-gate evaluation runs (`evaluateBindings`)  
**Then** computed validity is used for the gate  
**And** no binding status write/`save` occurs  
**When** the authoring validate endpoint runs  
**Then** status writes are persisted

### BDD-FOS-W7-005 — Unparseable composition rules fail closed
**Given** a version whose `rulesJson` is not valid JSON  
**When** change-diff reads composition rules  
**Then** the system throws `api.error.template.invalidRulesJson`  
**And** does not return an empty rule list that hides the defect

### BDD-FOS-W7-006 — Nesting uses APPROVED/ACTIVE comparable latest
**Given** nested content modules with multiple version lines  
**When** nesting neighbors are resolved  
**Then** only APPROVED + ACTIVE versions ordered by numeric semantic version are considered
