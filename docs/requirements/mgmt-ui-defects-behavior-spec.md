# BDD Behavior Specification: Management UI Defect Fixes

**Document status:** `ready`  
**Version:** 1.2.0 (P1 depth governance)  
**Authored:** 2026-07-10  
**Slice ID:** `mgmt-ui-p1` (extends `mgmt-ui-defects`)  
**Task IDs:** `P1-1` … `P1-4`  
**BDD ID prefix:** `BDD-MGMT-UI`

---

## Confirmed defects (Round 2 — retained)

| ID | Symptom | Expected (Round 2) |
| --- | --- | --- |
| D1 | Shell main area right-side empty gray; list pages not filling viewport | `ManagementShell` uses `shell-page-root` flex chain (breadcrumb excluded); `AppPageLayout` defaults `contentSurface=panel` for full-width white workspace; fluid list pages keep `layout-variant="fluid"` |
| D2 | Published release missing real basics/testing/approval content | API returns `TemplateDetailView`; Basics reuses `TemplateDetailOverviewTab`; Testing shows `BatchTestHistoryPanel`; Approval shows status summary + `TemplateLifecycleAuditTimeline` |
| D3 | `/api/policies` confused IA / empty when alerts fail | Alerts-first home only; `LoadErrorPanel` + retry on failure; empty alerts → `EmptyStatePanel` + **Browse templates** CTA (no duplicate published-packages catalog) |
| D4 | Login shows "Username is required" when field filled | Custom username validator + trim before submit |

---

## Confirmed P1 depth governance (v1.2.0)

| ID | Area | Expected |
| --- | --- | --- |
| P1-1 | Login password trim | Mirror username: trim password **leading/trailing** whitespace before validate/submit; whitespace-only password fails required (same message as empty); edge spaces do not cause false "password required" when a non-blank secret remains after trim |
| P1-2 | GroupManagementPanel load error | List/load failures use **only** `LoadErrorPanel` + retry (same pattern as `UserManagementListSection` / API surfaces); no dual `el-alert` error path while loading or after failure |
| P1-3 | Release Approval — publish-gate read-only | Approval tab shows a **read-only** publish-gate checklist panel (`PublishGateReadOnlyPanel` or equivalent reuse of checklist display) fed by a real API; **no invented/fake checklist items** |
| P1-4 | API policy home | **Confirmed non-goal for this slice** — R2 empty/error UX (`LoadErrorPanel` + retry; empty → `EmptyStatePanel` + Browse templates) is sufficient; no further UX work unless a new gap is found after R2 verification |

---

## Actors / goals

| Actor | Goal |
| --- | --- |
| Unauthenticated user | Sign in without false validation from whitespace |
| GLOBAL_ADMIN / identity admin | Manage groups; recover from list load failures with retry |
| Template author / reviewer | Inspect published release Approval with durable publish-gate evidence when available |
| API policy operator | Continue using R2 alerts-first home (no P1 redesign) |

---

## Acceptance scenarios (Round 2 summary — retained)

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

## Acceptance scenarios — P1 depth governance (v1.2.0)

### P1-1 — Login password trim (`BDD-MGMT-UI-P1-1`)

**Actor:** Unauthenticated user  
**Trigger:** Submit Sign in  
**Preconditions:** Login page rendered; English-first i18n messages

| # | Given | When | Then |
| --- | --- | --- | --- |
| P1-1-A | Password field contains a valid secret with leading and/or trailing spaces (e.g. ` secret `); username is a valid 8-digit id | User clicks Sign in | Client trims password edges before auth request; login proceeds with trimmed secret (or backend auth error if credentials wrong) — **no** false `login.validation.passwordRequired` |
| P1-1-B | Password is whitespace-only (e.g. `   `) | User clicks Sign in / blurs password | Validation fails with password-required (same outcome as empty); no auth call with a blank secret after trim |
| P1-1-C | Password is empty | User clicks Sign in | Existing required validation still fails with `login.validation.passwordRequired` |

**System responses:** Trim is product-safe edge-only (do not alter internal spaces of intentional passwords). Username trim behavior from D4 remains unchanged.  
**Observable evidence:** Unit/component test on `LoginView`; network payload password has no leading/trailing whitespace; UI error keys as above.  
**Boundary:** Do not strip internal whitespace; do not change password hashing/auth backend rules beyond client trim.

---

### P1-2 — GroupManagementPanel unified LoadErrorPanel (`BDD-MGMT-UI-P1-2`)

**Actor:** User with identity Groups route access  
**Trigger:** Open Groups / reload groups list after failure  
**Preconditions:** `identityStore.fetchGroups` can fail with messageKey + retryable flag

| # | Given | When | Then |
| --- | --- | --- | --- |
| P1-2-A | Groups list fetch fails | Groups panel finishes loading attempt | User sees `LoadErrorPanel` with store messageKey (fallback `identity.error.loadGroups`) and retry control when retryable — **not** a standalone `el-alert` error chrome as the primary failure surface |
| P1-2-B | Prior load failed; `LoadErrorPanel` visible | User clicks Retry | `fetchGroups` is invoked again; on success panel shows table or empty state; error panel clears |
| P1-2-C | Groups load succeeds with zero rows | Panel renders | Empty state path remains (no false error panel) |

**System responses:** Align with `UserManagementListSection` — single primary load-error surface = `LoadErrorPanel` + `@retry`. Remove/replace the dual path where an `el-alert` can show while `loadingGroups` is true.  
**Observable evidence:** Component test asserting `LoadErrorPanel` on failure and retry emit/call; no primary `el-alert` error banner for list load failure.  
**Boundary:** Create/edit/toggle mutation errors may still use toast/`ElMessage`; this scenario covers **list/load** failures only. Fail-closed: unauthorized users keep read-only hint + existing authz.

---

### P1-3 — Release Approval publish-gate read-only checklist (`BDD-MGMT-UI-P1-3`)

**Actor:** User who can open published release detail  
**Trigger:** Open Approval tab on `TemplateReleaseDetailView`  
**Preconditions:** Published release detail loaded (`TemplateDetail` / release snapshot); R2 Approval status + audit timeline remain

| # | Given | When | Then |
| --- | --- | --- | --- |
| P1-3-A | A **release-scoped or equivalent durable** publish-gate read API is available and returns checklist items for this release (or implementer-chosen supported binding — see API dependency) | User opens Approval tab | Read-only panel shows checklist title/description, each item label + ready/pending (or fail) state; **no publish/submit action buttons**; data matches API response (no hardcoded fake checks) |
| P1-3-B | Publish-gate read API fails or is unavailable for this release | User opens Approval tab | Panel shows `LoadErrorPanel` (or explicit unavailable empty state with retry if retryable) — Approval status summary + audit timeline still visible; **no fabricated checklist** |
| P1-3-C | Publish-gate API returns empty items / not-applicable for published release | User opens Approval tab | Honest empty/unavailable copy — not a fake “all green” checklist |

**Desired UX (confirmed):** Enrich Approval beyond status + audit with a **read-only** publish-gate checklist presentation, reusable from existing publish-gate display patterns (`TemplateDetailApprovalTab` / lifecycle checklist), named `PublishGateReadOnlyPanel` or equivalent.

**API dependency (confirmed constraint — not invented):**

| Option | Reality in inventory | Allowed for implementers |
| --- | --- | --- |
| A — Reuse live `GET /templates/{templateId}/publish-gate` | Exists today; evaluates **current template** (optional `phase`), **not** a published `releaseVersion` snapshot | Only if product accepts “live evaluation at view time” labeled honestly as current gate state — **not** as historical release snapshot |
| B — Minimal release-scoped backend | **No** releaseVersion publish-gate snapshot API in inventory | May add minimal endpoint (e.g. gate snapshot on release detail / dedicated read) if Option A is incorrect for published release governance |
| C — Omit checklist data | — | Prefer BDD P1-3-B/C honest error/empty over fake rows |

**Do not:** Seed UI with mock check codes; do not claim “passed at publish time” without API evidence.

**Observable evidence:** Approval tab renders read-only checklist when API succeeds; Vitest + later E2E; network call to chosen real endpoint.  
**Boundary:** Read-only only on release detail; do not enable Publish / Submit-for-approval from this surface. Fail-closed on authz errors via existing envelope + `LoadErrorPanel`.

---

### P1-4 — API policy home (confirmed deferral) (`BDD-MGMT-UI-P1-4`)

**Actor:** API policy operator  
**Confirmed decision:** After Round 2, `/api/policies` already provides alerts-first IA, `LoadErrorPanel` + retry on alerts failure, and empty alerts → `EmptyStatePanel` + Browse templates CTA.

| # | Given | When | Then |
| --- | --- | --- | --- |
| P1-4-A | R2 D3 behavior is verified on Docker | Operator opens `/api/policies` | No additional P1 UX change is required for this slice (**confirmed non-goal**) |
| P1-4-B | A **new** empty/error gap is discovered after R2 verification | (Out of slice) | Track as follow-up defect — not in P1-4 delivery scope |

---

## Residual E2E verification note (D1–D4)

Round 2 D1–D4 remain **in scope for Docker E2E / acceptance verification** alongside P1 work. E2E later should still cover:

| Residual journey | Smoke check |
| --- | --- |
| D1 | Shell fill on dashboard, users, policies, release detail |
| D2 | Published release Basics / Testing / Approval real content |
| D3 | API policy alerts table / empty CTA / load error retry |
| D4 | Login with filled employee id does not false-require username |

P1-1…P1-3 add journeys on top; P1-4 does not expand E2E beyond D3 residual.

---

## Boundary / exception summary

- Authorization remains fail-closed on management APIs.
- English-first i18n for any new user-visible strings; zh-CN lazy catalog updated in same change set at implementation.
- P1 does not reopen R2 IA redesign of API policy home.
- P1-3 never invents publish-gate rows.

---

## Open questions

| # | Question | Impact | Default if unanswered |
| --- | --- | --- | --- |
| OQ-P1-3-1 | For published release Approval, must publish-gate checklist be a **historical snapshot at publish time**, or is **live** `GET .../publish-gate` at view time acceptable if labeled “current evaluation”? | Chooses Option A vs B for P1-3 | **Blocked for implementation choice only** — UX still ready: show real API data or honest error/empty. Prefer Option A (reuse live gate) **only if** labeling is explicit; otherwise implementers add minimal release-scoped read (Option B). Product owner should confirm before backend work. |

All other P1 items are unambiguous. **BDD readiness is `ready`** with OQ-P1-3-1 tracked as an implementation-choice open question (does not block scenario authorship or frontend tests that mock the chosen client API).

---

## Traceability

- Plan: [MGMT-UI-defects.md](../plan/detail/MGMT-UI-defects.md) (R2 Done; P1 depth extends slice)
- Behavior slice: `mgmt-ui-p1` / tasks `P1-1`…`P1-4`
- Layout: `ManagementShell.vue`, `AppPageLayout.vue`
- Login: `LoginView.vue` (D4 username trim; P1-1 password trim)
- Identity: `GroupManagementPanel.vue` (P1-2), `UserManagementListSection.vue` (pattern reference)
- Release: `TemplateReleaseDetailView.vue`, `TemplateLifecycleAuditTimeline.vue`
- Publish gate (inventory): `templatesApi.fetchPublishGate` → `GET /templates/{templateId}/publish-gate`; lifecycle UI in `TemplateDetailApprovalTab.vue` / `useTemplateLifecycleGates.ts`
- Policies: `ApiPolicyHomeView.vue` (R2; P1-4 non-goal)

---

## BDD readiness

```
bdd_readiness: ready
version: 1.2.0
task_ids: [mgmt-ui-p1, P1-1, P1-2, P1-3, P1-4]
owning_doc: docs/requirements/mgmt-ui-defects-behavior-spec.md
open_questions: [OQ-P1-3-1]
```
