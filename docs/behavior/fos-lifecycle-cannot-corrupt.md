# Behavior — FOS-W6 Lifecycle cannot corrupt

| Field | Value |
| --- | --- |
| **Slice** | `fos-lifecycle-cannot-corrupt` |
| **Task Master** | **#176** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **partial** (content-module version id on stop/review) |
| **delivery_lane** | **full** |
| **Source** | [FOS-W6-lifecycle-cannot-corrupt.md](../plan/detail/FOS-W6-lifecycle-cannot-corrupt.md) |

## Goal

Lifecycle restore, master revision delete, content-module stop/review, version ordering,
supersede, bulk updates, and master review snapshots cannot corrupt package state.
`PENDING_RELEASE → DRAFT` remains **forbidden** (D10).

## Acceptance scenarios

### BDD-FOS-W6-001 — Restore skips individually deactivated releases
**Given** template STOPPED with release A deactivated earlier and B stopped by template STOP  
**When** restore runs  
**Then** only B becomes PUBLISHED; A stays STOPPED

### BDD-FOS-W6-002 — Current master revision cannot be deleted
**Given** a master whose current revision line is X  
**When** delete is requested for X  
**Then** the API fails closed with a stable error key; storage delete runs only after commit

### BDD-FOS-W6-003 — Module review targets explicit version
**Given** two SUBMITTED module versions  
**When** approve names version B  
**Then** only B becomes APPROVED

### BDD-FOS-W6-004 — Numeric semver order
**Given** versions `1.9` and `1.10`  
**When** ordered descending  
**Then** `1.10` precedes `1.9`

### BDD-FOS-W6-005 — Supersede keeps release_version
**Given** a published release is superseded by same-version republish  
**When** supersede completes  
**Then** `release_version` is retained (not nulled)

### BDD-FOS-W6-006 — Bulk lifecycle skips soft-deleted
**Given** a soft-deleted STOPPED version  
**When** bulk lifecycle update runs  
**Then** the soft-deleted row is unchanged

### BDD-FOS-W6-007 — Master review snapshot honesty
**Given** a review decision or new upload  
**When** statusSnapshot is written  
**Then** DRAFT is snapshotted on upload and decisions use the enum (including REJECTED)

### Non-goal
**W6-8** PENDING_RELEASE→DRAFT — **not implemented**
