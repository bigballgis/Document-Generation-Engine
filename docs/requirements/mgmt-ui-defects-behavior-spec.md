# BDD Behavior Specification: Management UI Defect Fixes

**Document status:** `ready`  
**Version:** 1.1.0 (Round 2 deep fix)  
**Authored:** 2026-07-10  
**Slice ID:** `mgmt-ui-defects`  
**BDD ID prefix:** `BDD-MGMT-UI`

---

## Confirmed defects

| ID | Symptom | Expected (Round 2) |
| --- | --- | --- |
| D1 | Shell main area right-side empty gray; list pages not filling viewport | `ManagementShell` uses `shell-page-root` flex chain (breadcrumb excluded); `AppPageLayout` defaults `contentSurface=panel` for full-width white workspace; fluid list pages keep `layout-variant="fluid"` |
| D2 | Published release missing real basics/testing/approval content | API returns `TemplateDetailView`; Basics reuses `TemplateDetailOverviewTab`; Testing shows `BatchTestHistoryPanel`; Approval shows status summary + `TemplateLifecycleAuditTimeline` |
| D3 | `/api/policies` confused IA / empty when alerts fail | Alerts-first home only; `LoadErrorPanel` + retry on failure; empty alerts → `EmptyStatePanel` + **Browse templates** CTA (no duplicate published-packages catalog) |
| D4 | Login shows "Username is required" when field filled | Custom username validator + trim before submit |

---

## Acceptance scenarios (summary)

### D1 — Layout fill

- **Given** GLOBAL_ADMIN on Docker `:4173`
- **When** visiting `/dashboard`, `/entitlement/users`, `/api/policies`, or a published release detail
- **Then** main content area fills shell width with white panel surface (no large right-side gray margin)

### D2 — Release read-only governance

- **Given** a published template release with API snapshot `readOnly: true`
- **When** opening release detail URL
- **Then** user can view Basics (name, group, master link), Testing (batch test history), Approval (status + workflow audit trail) read-only

### D3 — API policy home

- **Given** alerts endpoint healthy
- **When** opening `/api/policies`
- **Then** alerts table renders; empty alerts offer browse-templates CTA; alerts failure shows error panel (not silent empty table)

### D4 — Login validation

- **Given** login form with username `10000001` and valid password
- **When** clicking Sign in
- **Then** session establishes without false "Username is required"

---

## Traceability

- Plan: [MGMT-UI-defects.md](../plan/detail/MGMT-UI-defects.md)
- Layout: `ManagementShell.vue`, `AppPageLayout.vue`
- Release: `TemplateReleaseDetailView.vue`, `TemplateLifecycleAuditTimeline.vue`
- Policies: `ApiPolicyHomeView.vue`
