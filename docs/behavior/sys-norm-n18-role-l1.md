---
id: DOC-BEHAVIOR-SYS-NORM-N18-ROLE-L1
type: Behavior Spec
status: Confirmed
readiness: ready
program: post-SYS-NORM residual (N18 + ADR-0070 P-Q1)
slice: sys-norm-n18-role-l1
taskMaster: "157+158"
related:
  - docs/plan/system-normalization-program-2026-07.md
  - docs/behavior/system-normalization-program.md
  - docs/behavior/sys-norm-shell-fluid-nav.md
  - docs/behavior/sys-norm-roles.md
  - docs/behavior/sys-norm-demo-seed-terms.md
  - docs/behavior/ce-g04-legal-hold.md
  - docs/adr/authorization-security/0070-role-compression-six-roles.md
  - docs/architecture/ux-entity-display-constitution.md
  - docs/product/business-terminology-guide.md
  - docs/security/permission-matrix.md
---

# N18 Legal-hold actor EntityLink + DOCUMENT_AUTHOR L1 labels

> **Slice:** `sys-norm-n18-role-l1` · Batch Recommendation **`merge`**  
> **Member tasks (register at stage 2):** TM **#157** (N18 Legal-hold actor EntityLink) +
> TM **#158** (`DOCUMENT_AUTHOR` L1 EN/ZH lock).  
> **Placement:** ISOLATED · worktree `D:/working/DGE-sys-norm-n18-role-l1` ·
> branch `feat/sys-norm-n18-role-l1` · base `c4af526d`.  
> **Trace:** program plan §4a “Also deferred” N18; Wave 1/8 explicit deferral; charter
> [system-normalization-program.md](./system-normalization-program.md) N18 + **P-Q1**;
> [ADR-0070](../adr/authorization-security/0070-role-compression-six-roles.md) L1 labels.  
> **Locks / vetoes:** do **not** flip checklist **#3b** / **#5a**; do **not** mark **#53**
> Done; do **not** activate CE-O02 / **#119**; do **not** expand to N19–N20 / Word-host /
> umbrella CE. Formal phase **None**.

```
bdd_readiness: ready
frontend_ui_in_scope: true
backend_api_contract_change: optional-additive
open_questions: []
owning_doc: docs/behavior/sys-norm-n18-role-l1.md
task_ids: ["157", "158"]
queue_slice_id: sys-norm-n18-role-l1
member_task_ids: ["157", "158"]
batch_decision: merge
shared_acceptance_surface: LegalHoldListView EntityLink + DOCUMENT_AUTHOR L1 i18n/docs
scenario_ids:
  - BDD-N18-L1-001 … BDD-N18-L1-012
scenario_count: 12
vetoes_applied:
  - checklist-#3b/#5a
  - CE-O02
  - "#53"
  - "#119-Word-host"
  - "#106-umbrella"
on_red_split_hint: If N18 fails, peel L1 docs-only to solo
```

---

## 1. Actor / role

| Actor | Role / capability | Concern |
| --- | --- | --- |
| Legal-hold operator | User who can open Legal holds catalog (existing CE-G04 / Security nav entitlement) | Reads Created by column; navigates to user when permitted |
| Identity administrator | `canAccessRoute(route.identity-administration)` (Users & Groups) | Receives deep-link into users catalog |
| Role catalog consumer | Any management UI showing `DOCUMENT_AUTHOR` L1 label (pickers, journey, matrix-facing copy) | Sees finalized EN/ZH role display name |
| Unauthorized viewer | Lacks identity-administration route | Sees actor **label** but **no** navigable link (fail-closed) |

Role **ID** `DOCUMENT_AUTHOR` remains locked (ADR-0070 / Wave 5). This leaf finalizes **L1
display strings only**.

---

## 2. User goal

1. On Legal holds list, the **Created by** actor is not a raw plain-text username dump —
   it uses the shared `EntityLinkCell` pattern (same family as Users → Authorized groups
   EntityLink and other EntityLink columns): friendly **display name** when available,
   navigable to Users catalog when the viewer may administer identity.
2. Everywhere L1 role labels surface `DOCUMENT_AUTHOR`, operators see final copy:
   EN **Document author** / ZH **文档作者** (no “interim” suffix on L1).

---

## 3. Trigger

| Surface | Trigger |
| --- | --- |
| Legal holds list | Operator opens Security → Legal holds (`LegalHoldListView`) with ≥1 hold row |
| Role L1 copy | Operator opens any surface that renders `roles.DOCUMENT_AUTHOR` (or equivalent L1 role label keys) in EN or zh-CN |

---

## 4. Preconditions

- SYS-NORM Waves **0–8 Done**; §4a parked UX queue **empty** (Reminder / Asset library /
  Binding editor + Auto `referenceKey` Done).
- Wave 1 EntityLink primitives (`EntityLinkCell`, `useEntityLinkTargets`) and Users
  Authorized groups EntityLink (**BDD-SYS-NORM-W1-014**) already shipped.
- CE-G04 Legal hold list API returns `createdByUsername` (required).
- Wave 5 six-role catalog live; FE interim already uses EN “Document author” /
  ZH “文档作者” — this leaf **locks** those strings as Confirmed (removes Pending / interim).
- Host sole-active cleared before this leaf; Docker queue used only at later stages.

---

## 5. Primary journey

### 5.1 N18 — Legal hold Created by EntityLink

1. Operator opens Legal holds list.
2. For each row with a non-empty `createdByUsername`, the Created by cell renders
   `EntityLinkCell` (not bare `{{ row.createdByUsername }}` text).
3. **Label:** `formatUserDisplayLabel(createdByUsername, createdByDisplayName)` —
   prefer trimmed display name; else username; else em dash `—`.
4. **Link (permitted):** when `sessionStore.canAccessRoute(ROUTE_KEYS.identityAdministration)`
   is true and username is non-empty, `to` points at users catalog
   `/entitlement/users` with search prefilling the username (parallel to
   `groupCatalogLink` → `/entitlement/groups?q=…`).
5. **Link (denied):** `to` omitted → plain-text label (fail-closed; no 403 navigation).
6. Clicking a permitted link navigates to Users management with the actor discoverable via
   the prefilled search.

### 5.2 P-Q1 — DOCUMENT_AUTHOR L1 labels

1. Operator views role pickers / journey / L1 role labels in EN locale → sees
   **Document author**.
2. Same surfaces in zh-CN → sees **文档作者**.
3. No L1 user-facing string appends “(interim)” / “（interim）” for this role.
4. Role code / API enum remains `DOCUMENT_AUTHOR` (no rename).

---

## 6. System responses (success)

| Path | Observable response |
| --- | --- |
| N18 permitted | Created by cell = `EntityLinkCell` link; label = display name when present else username; navigates to `/entitlement/users?q=<username>` |
| N18 denied identity route | Same label, plain text (no router-link) |
| N18 missing actor | Em dash; not a link |
| L1 EN | `roles.DOCUMENT_AUTHOR` (and L1 mirrors) = `Document author` |
| L1 zh-CN | same keys = `文档作者` |

---

## 7. Boundary / exception / fail-closed

| Case | Behavior |
| --- | --- |
| No `identity-administration` route | No user link; still show label |
| Blank / whitespace username | Em dash; no link |
| Optional `createdByDisplayName` absent | Label falls back to `createdByUsername` (still EntityLink when permitted) |
| `releasedByUsername` column | **Out of scope** this leaf (Created by / N18 only) |
| Cross-user privacy | Linking only when identity admin route already allowed; do not invent new permission bits |
| Checklist #3b / #5a / #53 / CE-O02 / #119 | Unchanged; leaf must not flip or activate |
| N19–N20 EntityLink residuals | Out of scope |
| Governance PRD/matrix/terminology “Confirmed” prose | Behavior locked here; **doc-keeper stage 3** promotes product/security/ADR narrative |

### Optional additive API (N18 display-name parity)

When implementers enrich the list payload, Legal hold view MAY add optional nullable
`createdByDisplayName` (OpenAPI additive; reuse `ManagementUserDisplayService` pattern from
ux-entity-display Phase 2). **Required field set unchanged** (`createdByUsername` remains
required). No new authz codes. If enrichment is deferred within the leaf, FE still ships
EntityLink with username label — display-name enrichment is preferred for constitution
parity but **must not** block EntityLink gating.

---

## 8. Acceptance scenarios (Given / When / Then)

### BDD-N18-L1-001 — Created by uses EntityLinkCell

**Given** a Legal holds list row with `createdByUsername` = `10000001`  
**When** the Created by column renders  
**Then** the cell uses `EntityLinkCell` (not bare interpolated username text)

### BDD-N18-L1-002 — Display name preferred as label

**Given** the row has `createdByUsername` = `10000001` and `createdByDisplayName` = `Alice Author`  
**When** the Created by cell renders  
**Then** the visible label is `Alice Author`

### BDD-N18-L1-003 — Username fallback when display name missing

**Given** the row has `createdByUsername` = `10000001` and no usable display name  
**When** the Created by cell renders  
**Then** the visible label is `10000001`

### BDD-N18-L1-004 — Link when identity administration permitted

**Given** the viewer can access `route.identity-administration`  
**And** a row has non-empty `createdByUsername`  
**When** the Created by cell renders  
**Then** `EntityLinkCell` receives a `to` targeting `/entitlement/users` with `q` prefilling that username

### BDD-N18-L1-005 — Plain text when identity administration denied

**Given** the viewer cannot access `route.identity-administration`  
**And** a row has non-empty `createdByUsername`  
**When** the Created by cell renders  
**Then** the label still shows  
**And** the cell is not a navigable link

### BDD-N18-L1-006 — Empty actor is em dash

**Given** a row with blank `createdByUsername` (and no display name)  
**When** the Created by cell renders  
**Then** the label is `—`  
**And** it is not a link

### BDD-N18-L1-007 — Navigation lands on users catalog

**Given** a permitted Created by link for username `10000001`  
**When** the operator activates the link  
**Then** the app navigates to the Users management surface with search prefilled for that username

### BDD-N18-L1-008 — English L1 Document author locked

**Given** locale `en`  
**When** any L1 surface renders the `DOCUMENT_AUTHOR` role display label (`roles.DOCUMENT_AUTHOR` or equivalent L1 key)  
**Then** the text is exactly `Document author`  
**And** it does not include `interim`

### BDD-N18-L1-009 — Chinese L1 文档作者 locked

**Given** locale `zh-CN`  
**When** any L1 surface renders the `DOCUMENT_AUTHOR` role display label  
**Then** the text is exactly `文档作者`  
**And** it does not include `interim` / `（interim）`

### BDD-N18-L1-010 — Role ID unchanged

**Given** Wave 5 six-role catalog  
**When** this leaf ships  
**Then** the assignable role code remains `DOCUMENT_AUTHOR`  
**And** no new role ID is introduced for the label lock

### BDD-N18-L1-011 — Vetoes held

**Given** this leaf’s change set  
**When** plan / checklist / CE registry are inspected  
**Then** checklist **#3b** / **#5a** are unchanged  
**And** umbrella **#53** is not marked Done  
**And** CE-O02 / **#119** are not activated

### BDD-N18-L1-012 — English-first i18n for any new strings

**Given** any new user-facing copy introduced by this leaf  
**When** locales are checked  
**Then** English keys exist first in `en.ts`; zh-CN mirrors as needed  
**And** no hardcoded user-facing Chinese-only chrome is introduced in changed Vue surfaces

---

## 9. Observable evidence

| Evidence | Proof |
| --- | --- |
| UI | `LegalHoldListView` Created by = `EntityLinkCell`; link vs plain by identity route mock |
| Unit | Vitest: label preference, link gating, empty actor |
| i18n | `roles.DOCUMENT_AUTHOR` EN/ZH exact strings; no interim suffix on L1 |
| API (optional) | OpenAPI `LegalHoldView.createdByDisplayName` nullable additive if enrichment lands |
| E2E / UIUX | Legal holds list actor cell + role label spot-check (stages 6–7) |
| Docs | Behavior SoT = this file; governance Confirmed sync at **doc-keeper stage 3** |

---

## 10. Traceability

| Source | Link |
| --- | --- |
| Program plan §4a / N18 deferred | [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) |
| Charter N18 + P-Q1 | [system-normalization-program.md](./system-normalization-program.md) §2.9 / §2.10 / §3 |
| Wave 1 deferral | [sys-norm-shell-fluid-nav.md](./sys-norm-shell-fluid-nav.md) Explicitly deferred N18 |
| Wave 8 deferral | [sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md) W8-C8 / W8-015 |
| Wave 5 roles / interim L1 | [sys-norm-roles.md](./sys-norm-roles.md); ADR-0070 P-Q1 |
| Legal hold baseline | [ce-g04-legal-hold.md](./ce-g04-legal-hold.md) |
| Entity display constitution | [ux-entity-display-constitution.md](../architecture/ux-entity-display-constitution.md) |
| Terminology (stage 3 Confirmed) | [business-terminology-guide.md](../product/business-terminology-guide.md) |

---

## 11. Out of scope

- N19 / N20 EntityLink residuals; MasterImpact
- `releasedByUsername` EntityLink
- Checklist **#3b** / **#5a** GO flips; CE umbrella **#53** Done; CE-O02; **#119** Word-host
- Role capability / matrix rewrite (Wave 5 already Done)
- Claiming SYS-NORM program re-open or formal P-phase activation
- Full governance doc Confirmed sweep (delegate **doc-keeper** stage 3)

---

## 12. Stage Done definition (this BDD stage)

Stage 1 Done when:

1. This file is persisted under `docs/behavior/` with `bdd_readiness: ready`.
2. Acceptance scenarios **BDD-N18-L1-001…012** are complete and open_questions is empty.
3. P-Q1 L1 strings are **Confirmed in behavior** (EN Document author / ZH 文档作者).
4. N18 EntityLink + fail-closed link gating are Confirmed for TDD Red.
5. Handoff to **plan-orchestrator** (stage 2) to register TM **#157** / **#158** and detail plan;
   then **doc-keeper** (stage 3) for terminology / matrix / ADR narrative Confirmed sync.
