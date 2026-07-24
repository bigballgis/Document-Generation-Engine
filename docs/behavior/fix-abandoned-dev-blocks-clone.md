# BDD behavior spec: Abandoned STOPPED/DEPRECATED must not block Clone

| Field | Value |
| --- | --- |
| **Document status** | `ready` |
| **BDD ID prefix** | `BDD-FIX-ABANDON-CLONE` |
| **Authored** | 2026-07-24 |
| **Slice** | `fix-abandoned-dev-blocks-clone` |
| **Branch** | `feat/fix-abandoned-dev-blocks-clone` |
| **Worktree** | `D:/working/DGE-fix-abandoned-dev-blocks-clone` |
| **Placement** | ISOLATED |
| **Batch recommendation** | **solo** (`proposed_slice_id: fix-abandoned-dev-blocks-clone`; `member_task_ids: [fix-abandoned-dev-blocks-clone]`) |
| **Shared acceptance surface** | Template version-lines + Clone from published release when no real DRAFT/in-flight |
| **Formal phase** | **None** |
| **Owning docs (leaf SoT)** | **This file** |
| **Traceability** | [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) **BDD-TEMPLATE-PACKAGE-NAV-001** (S4 clone / S7 in-flight block); [requirements-plan.md](../requirements/requirements-plan.md) §模板包导航 UX; [domain-model.md](../domain/domain-model.md) §2.10–2.11 (dev / release lines; same-release republish clears `release_version` → `STOPPED`) |
| **Frontend UI** | **`frontend_ui_in_scope=true`** (version-lines honesty + Clone affordance; hub / release detail) |

**Completion claim constraints:** Closes the proven bug that blank/`null` `releaseVersion` alone marks a version as in-flight, so abandoned `STOPPED`/`DEPRECATED` rows fake an in-flight line and block Clone on a published sibling. **Do not** invent a third `lineKind`. **Do not** change publish / abandon lifecycle transitions beyond in-flight classification and version-lines projection honesty. **Do not** flip checklist **#3b** / **#5a**. **Do not** claim go-live / CE / IBL Done.

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  proposed_slice_id: fix-abandoned-dev-blocks-clone
  member_task_ids: [fix-abandoned-dev-blocks-clone]
  shared_acceptance_surface: >
    Template version-lines + Clone from published release when no real DRAFT/in-flight
```

| IN (this leaf) | OUT |
| --- | --- |
| Correct `isInFlight` so terminal abandoned rows with blank/`null` release are **not** in-flight | Changing abandon / publish / restore state-machine transitions |
| Clone on published release succeeds when only abandoned STOPPED/DEPRECATED (blank release) siblings exist | New Clone API / new error codes |
| version-lines show abandoned STOPPED as **STOPPED**, not package-status overlay PUBLISHED/Live | New `TemplateVersionLineKind` enum value (keep `IN_FLIGHT` \| `PUBLISHED`) |
| Real in-flight (`DRAFT` / `TESTING` / `APPROVAL` / `PENDING_RELEASE` + blank release) still blocks Clone with `409 TEMPLATE_DEV_LINE_IN_FLIGHT` | Relaxing group isolation / author capability on Clone |

---

## 1. Overview

### 1.1 Proven bug (current evidence)

| Finding | Evidence |
| --- | --- |
| `TemplateCurrentVersionResolver.isInFlight` treats blank/`null` `releaseVersion` as in-flight **regardless of** `lifecycle_status` | `TemplateCurrentVersionResolver.isInFlight` |
| Abandoned DEV (`STOPPED` or `DEPRECATED`) with blank/`null` release incorrectly counts as in-flight | Same + `hasInFlightDevVersion` / clone guard |
| That fake in-flight blocks Clone on a published sibling (`cloneable=false`, clone → `409 TEMPLATE_DEV_LINE_IN_FLIGHT`) | Example: `DEMO-COVENANT-WAIVER` — Dev#1 `STOPPED`+`null` release; Dev#2 `PUBLISHED` `1.0.0` not cloneable |
| version-lines can **overlay package** lifecycle onto a misclassified “in-flight” row | `TemplateVersionLineViewSupport.resolveLifecycleStatus`: when `inFlight`, returns `template.getLifecycleStatus()` (often `PUBLISHED`) instead of the row’s own `STOPPED` |

### 1.2 Product intent

1. After publish, the next version is created via **Clone** from a published release → new `DRAFT` dev line.  
2. An abandoned `STOPPED` (or `DEPRECATED`) line with blank/`null` `releaseVersion` is **not** in-flight and **must not** block Clone.  
3. version-lines must show abandoned `STOPPED` **honestly** as `STOPPED` — not a fake `PUBLISHED` / Live overlay from the package head.

### 1.3 Confirmed in-flight definition (this leaf)

A non-deleted template version is **in-flight** if and only if:

- `releaseVersion` is `null` or blank **and**
- `lifecycleStatus` is one of: `DRAFT`, `TESTING`, `APPROVAL`, `PENDING_RELEASE`.

A non-deleted version is **not** in-flight when:

- `lifecycleStatus` is `STOPPED` or `DEPRECATED` (including blank/`null` release — abandon, same-release republish predecessor, or equivalent), **or**
- `lifecycleStatus` is `PUBLISHED` with a non-blank semver `releaseVersion`.

Blank/`null` `releaseVersion` alone is **insufficient** to classify as in-flight.

---

## 2. Actor / Role

| Actor | Role / capability | Notes |
| --- | --- | --- |
| **Template author** | `authorTemplates` + `route.template-management` within authorized group | Opens package hub version-lines; Clones a published release when no real in-flight exists |
| **Master designer / admin with author** | Same author/clone scope per permission-matrix §5 | Same Clone semantics |
| **Tester / approver (read)** | Lifecycle read; **no** Clone unless also author-capable | Must still see honest STOPPED status on abandoned rows |
| **Cross-group unauthorized** | No target-group authz | Fail-closed `403 ACCESS_DENIED` on version-lines / clone (unchanged) |
| **System** | `TemplateCurrentVersionResolver` + version-lines / clone supports | Classifies in-flight; projects `lifecycleStatus` / `lineKind` / `cloneable`; enforces Clone gate |

---

## 3. Goal

1. Operators can **Clone** a published release into a new `DRAFT` after the previous in-flight work was abandoned (`STOPPED` + blank release), without a false `TEMPLATE_DEV_LINE_IN_FLIGHT`.  
2. version-lines list abandoned terminal rows with their **own** `lifecycleStatus` (`STOPPED` / `DEPRECATED`), never overlaying package `PUBLISHED` as if the abandoned row were Live.  
3. Real active authoring lines (`DRAFT` / `TESTING` / `APPROVAL` / `PENDING_RELEASE`) with blank release remain the sole in-flight gate and still block Clone.  
4. Published semver rows remain not in-flight; `cloneable=true` for author-capable actors when no real in-flight exists.

---

## 4. Trigger

1. Hub loads `GET …/templates/{templateId}/version-lines`.  
2. Actor invokes Clone: `POST …/templates/{templateId}/release-versions/{releaseVersion}/clone` (hub or release detail).  
3. Internal callers of `isInFlight` / `hasInFlightDevVersion` / `findInFlightDevVersion` (clone guard, version-lines projection).

---

## 5. Preconditions

- Template exists and is not logically deleted.  
- Actor authenticated with management JWT; group scope includes the template (or global).  
- For Clone success path: a published release line exists (non-blank semver + `PUBLISHED`), and **no** real in-flight line exists.  
- Abandoned predecessor may exist: `STOPPED` or `DEPRECATED` with `releaseVersion` null/blank (e.g. abandon after publish of a sibling, or same-release republish clearing prior release — domain §2.11).

---

## 6. Primary journey

1. Author opens `/templates/{templateId}` → version-lines load.  
2. Hub shows published release (e.g. `1.0.0`, `PUBLISHED`) and any abandoned predecessor (`STOPPED`, blank release) **without** treating the abandoned row as in-flight.  
3. Published row has `cloneable: true` (author-capable; no real in-flight).  
4. Author Clones the published release → `201` + new `DRAFT` with blank `releaseVersion` and incremented `devVersionNumber`.  
5. Subsequent version-lines show the new real `IN_FLIGHT` `DRAFT` plus unchanged published and abandoned rows.

---

## 7. System responses (success)

| Surface | Success response |
| --- | --- |
| **In-flight query** | `hasInFlightDevVersion` / `findInFlightDevVersion` ignore `STOPPED`/`DEPRECATED` (blank or not) and ignore `PUBLISHED`+semver |
| **version-lines** | Abandoned row: `lifecycleStatus: STOPPED` (or `DEPRECATED`); **not** package-overlay `PUBLISHED`; not presented as active Live in-flight |
| **cloneable** | Published release row `cloneable: true` when actor may author and no real in-flight exists |
| **Clone** | `201` + `TemplateDevVersionCreatedView` (`lifecycleStatus: DRAFT`, `releaseVersion: null`); audit provenance from source release |

---

## 8. Acceptance scenarios (Given / When / Then)

### BDD-FIX-ABANDON-CLONE-001 — STOPPED + null/blank release is not in-flight; does not block Clone

- **Given** a template with Dev#1 `lifecycleStatus: STOPPED`, `releaseVersion: null` (or blank), and Dev#2 `lifecycleStatus: PUBLISHED`, `releaseVersion: "1.0.0"` (package may be `PUBLISHED`), and an authorized `TEMPLATE_AUTHOR`,  
- **When** the system evaluates in-flight for the template and the actor loads version-lines then `POST …/release-versions/1.0.0/clone`,  
- **Then** Dev#1 is **not** in-flight (`isInFlight=false`); `hasInFlightDevVersion` is false before clone; published `1.0.0` has `cloneable: true`; Clone returns **`201`** with a new `DRAFT` (not `409 TEMPLATE_DEV_LINE_IN_FLIGHT`).

### BDD-FIX-ABANDON-CLONE-002 — DEPRECATED + blank release is not in-flight

- **Given** a non-deleted template version with `lifecycleStatus: DEPRECATED` and blank/`null` `releaseVersion`,  
- **When** `isInFlight` / `hasInFlightDevVersion` run for that template (no other real in-flight rows),  
- **Then** that version is **not** in-flight; it does not alone set `hasInFlightDevVersion=true` and does not alone force published siblings to `cloneable: false`.

### BDD-FIX-ABANDON-CLONE-003 — DRAFT + blank release is in-flight and blocks Clone

- **Given** a template with an active version `lifecycleStatus: DRAFT`, `releaseVersion: null` (or blank), and a published release `1.0.0`,  
- **When** an authorized author calls `POST …/release-versions/1.0.0/clone`,  
- **Then** the DRAFT row is in-flight (`isInFlight=true`); Clone returns **`409`** with `error.code: TEMPLATE_DEV_LINE_IN_FLIGHT`; no new dev version is created; published row unchanged; version-lines show `cloneable: false` on the published row while that DRAFT exists.

### BDD-FIX-ABANDON-CLONE-004 — PUBLISHED + semver is not in-flight; cloneable when no real in-flight

- **Given** a template whose only non-deleted lines are published releases with non-blank semver (e.g. `1.0.0`, `PUBLISHED`) and optionally abandoned `STOPPED`/`DEPRECATED` blank-release rows — **no** `DRAFT`/`TESTING`/`APPROVAL`/`PENDING_RELEASE` blank-release row,  
- **When** an authorized author loads version-lines and Clones `1.0.0`,  
- **Then** each `PUBLISHED`+semver row is not in-flight; `cloneable: true` on published rows (author-capable); Clone succeeds with **`201`** and a new `DRAFT`.

### BDD-FIX-ABANDON-CLONE-005 — version-lines show abandoned STOPPED honestly (not fake PUBLISHED/Live)

- **Given** a template package lifecycle `PUBLISHED` with Dev#1 abandoned `STOPPED` + blank/`null` `releaseVersion` and Dev#2 published `1.0.0`,  
- **When** `GET …/templates/{templateId}/version-lines` returns Dev#1,  
- **Then** Dev#1 `lifecycleStatus` is **`STOPPED`** (the version’s own status) — **not** overlaid as `PUBLISHED` from the package head; the UI status badge / Live treatment must not present that abandoned row as the active published Live line; Dev#1 must not be classified as the blocking in-flight line.

---

## 9. Boundary and exception behavior

| Case | Expected |
| --- | --- |
| **Real in-flight statuses** | `TESTING` / `APPROVAL` / `PENDING_RELEASE` + blank/`null` release → still in-flight; Clone blocked (`409 TEMPLATE_DEV_LINE_IN_FLIGHT`) — same as DRAFT |
| **STOPPED published release** | `STOPPED` + non-blank semver → not in-flight; may appear on hub/release detail; Clone from **another** live published release still allowed when no real in-flight (existing catalog-nav: STOPPED release cloneable when no in-flight) |
| **Whitespace-only releaseVersion** | Treated as blank for in-flight release check; classification still requires active authoring lifecycle — blank+`STOPPED`/`DEPRECATED` → not in-flight; blank+`DRAFT` → in-flight |
| **Multiple abandoned blank-release terminal rows** | None count as in-flight; Clone still allowed |
| **Abandoned + real DRAFT coexist** | DRAFT wins as in-flight; Clone blocked |
| **Deprecated template package policy** | Unchanged: package-level deprecate may still block authoring/clone per existing deprecate policy — orthogonal to per-version in-flight classification |
| **Group isolation** | Cross-group version-lines / clone → `403 ACCESS_DENIED` (fail-closed; unchanged) |
| **Unknown release** | Clone unknown `releaseVersion` → `404` (unchanged) |
| **Published immutability** | Mutations on published content still `403 TEMPLATE_VERSION_IMMUTABLE` (unchanged) |
| **Deleted versions** | Logically deleted versions excluded from in-flight and version-lines (unchanged) |

---

## 10. Observable evidence

| Layer | Evidence |
| --- | --- |
| **Unit / BE** | `TemplateCurrentVersionResolver.isInFlight`: STOPPED/DEPRECATED+blank → false; DRAFT+blank → true; PUBLISHED+semver → false; regression for DEMO-COVENANT-WAIVER-shaped fixture |
| **API** | `GET …/version-lines`: abandoned row `lifecycleStatus=STOPPED`; published `cloneable=true` when no real in-flight; `POST …/clone` → `201` (or `409` only for real in-flight) |
| **UI** | Hub version-lines: abandoned row status **STOPPED** (not Live/PUBLISHED fake); Clone control enabled on published sibling when `cloneable` |
| **Audit** | Successful clone still records provenance from source `releaseVersion` + new `devVersionId` |
| **Gates** | Backend `mvn verify`; frontend lint/type-check/test/build; E2E on Docker acceptance stack for hub Clone journey when FE in scope |

---

## 11. Confirmed decisions (this leaf)

| ID | Decision | Basis |
| --- | --- | --- |
| **FAC-C1** | In-flight = blank/`null` release **and** active authoring status (`DRAFT`\|`TESTING`\|`APPROVAL`\|`PENDING_RELEASE`) | Product intent + catalog-nav in-flight set; proven bug |
| **FAC-C2** | `STOPPED` / `DEPRECATED` never in-flight (blank or semver) | Abandon + same-release republish (domain §2.11) |
| **FAC-C3** | version-lines `lifecycleStatus` for non-in-flight rows = **version** status (no package overlay masquerading abandoned as PUBLISHED) | `resolveLifecycleStatus` honesty |
| **FAC-C4** | Clone gate uses the corrected `hasInFlightDevVersion` only | Existing `409 TEMPLATE_DEV_LINE_IN_FLIGHT` |
| **FAC-C5** | Keep `lineKind` enum `IN_FLIGHT` \| `PUBLISHED`; honesty for abandoned rows is primarily **lifecycleStatus** + not counting as in-flight | No API enum expansion in this leaf |

### Pending questions

None — behavior confirmed by product intent + proven bug + existing BDD-TEMPLATE-PACKAGE-NAV-001.

---

## 12. Traceability

| Source | Link |
| --- | --- |
| Parent navigation / Clone BDD | [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) BDD-TEMPLATE-PACKAGE-NAV-001 S4, S7 |
| Requirements | [requirements-plan.md](../requirements/requirements-plan.md) §已确认：模板包导航 UX |
| Domain | [domain-model.md](../domain/domain-model.md) §2.10 Development Version, §2.11 Release Version |
| Implementation hotspot (post-BDD) | `TemplateCurrentVersionResolver.isInFlight`; `TemplateVersionLineViewSupport`; clone mutation support |
| Slice id | `fix-abandoned-dev-blocks-clone` |

---

## 13. BDD readiness

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/fix-abandoned-dev-blocks-clone.md
task_ids: [fix-abandoned-dev-blocks-clone]
acceptance_scenarios:
  - BDD-FIX-ABANDON-CLONE-001
  - BDD-FIX-ABANDON-CLONE-002
  - BDD-FIX-ABANDON-CLONE-003
  - BDD-FIX-ABANDON-CLONE-004
  - BDD-FIX-ABANDON-CLONE-005
```

**Handoff:** `plan-orchestrator` → TDD Red tests for `isInFlight` / version-lines / clone gate → implementers in this worktree only.
