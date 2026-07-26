# Behavior — FOS-W5 Time & locale honesty

| Field | Value |
| --- | --- |
| **Slice** | `fos-time-locale-honesty` |
| **Task Master** | **#175** |
| **bdd_readiness** | **ready** |
| **frontend_ui_in_scope** | **true** |
| **delivery_lane** | **full** |
| **Source** | [FOS-W5-time-locale-honesty.md](../plan/detail/FOS-W5-time-locale-honesty.md) |

## Goal

Datetime pickers send true UTC; displayed timestamps show an explicit UTC zone;
Element Plus follows the app locale; collaboration/audit server summaries can
resolve in zh-CN; timeout config shows formatted updatedAt.

## Acceptance scenarios

### BDD-FOS-W5-001 — Local wall-clock → UTC ISO
**Given** a datetime picker value representing local wall-clock  
**When** the value is converted for API send  
**Then** the payload is a correct UTC ISO string (not local digits + literal `Z`)

### BDD-FOS-W5-002 — Timestamps show UTC
**Given** an ISO instant  
**When** `formatDateTime` runs  
**Then** the string includes an explicit UTC/GMT zone marker

### BDD-FOS-W5-003 — Element Plus follows app locale
**Given** app locale is `zh-CN`  
**When** the shell mounts  
**Then** `ElConfigProvider` uses Element Plus zh-cn locale

### BDD-FOS-W5-004 — Collaboration summaries localisable
**Given** Accept-Language / locale context is zh-CN  
**When** MessageResolver resolves collaboration work-item summaries  
**Then** Chinese catalogue strings are used

### BDD-FOS-W5-005 — Timeout config uses formatDateTime
**Given** timeout config has `updatedAt`  
**When** the panel renders  
**Then** the timestamp is formatted via `formatDateTime`
