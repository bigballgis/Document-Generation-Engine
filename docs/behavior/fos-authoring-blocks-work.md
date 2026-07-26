# Behavior — FOS-W3 Authoring blocks work

| Field | Value |
| --- | --- |
| **Slice** | `fos-authoring-blocks-work` |
| **Task Master** | **#173** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **true** |
| **delivery_lane** | **full** |
| **Source** | [FOS-W3-authoring-blocks-work.md](../plan/detail/FOS-W3-authoring-blocks-work.md) |

## Goal

Authors can insert and edit structured content blocks honestly: content-module and list
blocks work at root, style/inline tools respect focus, nesting depth copy matches the
guard, table refs use search select, and save blocks on failed structure validation.

## Acceptance scenarios

### BDD-FOS-W3-001 — Top-level content module insert
**Given** an empty structured document  
**When** the toolbar inserts `contentModuleRef`  
**Then** the document gains a root node of type `contentModuleRef`

### BDD-FOS-W3-002 — List block is editable
**Given** a list block is inserted  
**When** the block card renders  
**Then** an editable paragraph child input is present (not a grey `list` meta card)

### BDD-FOS-W3-003 — Style apply scopes to focused block
**Given** two paragraphs with focus on the second  
**When** Apply style runs  
**Then** only the focused paragraph receives `styleRef`

### BDD-FOS-W3-004 — Nesting depth guard agrees with copy
**Given** `STRUCTURED_CONTENT_MAX_NEST_DEPTH = 3`  
**When** nesting eligibility is evaluated  
**Then** containers may nest to depth 3 and the UI limit message uses `{ max: 3 }`

### BDD-FOS-W3-005 — Inline insert targets focused block
**Given** three paragraphs with focus on the middle  
**When** toolbar inserts a variable  
**Then** the middle paragraph receives the inline node

### BDD-FOS-W3-006 — Table component ref uses search select
**Given** a `tableComponentRef` block  
**When** the card renders  
**Then** an `AppSearchSelect` (filterable select) is used instead of a bare free-text-only field when options exist; otherwise searchable allow-create select

### BDD-FOS-W3-007 — Save validates structure
**Given** structured content with unresolved `${var}` / validation issues  
**When** binding save runs  
**Then** `validateStructure` is invoked and save aborts
