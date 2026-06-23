# P16 — Template & Version Lifecycle Governance Completeness (Detailed Plan)

**Phase status:** Not Started | **Depends on:** P5
**Confirmed:** 2026-06-23 (deep-review gap G1 — promoted from UXC3 backlog)

> Single-active-phase invariant: P13 completed **Done** (2026-06-23); there is currently no
> single active phase. P16 stays `Not Started` and must not be activated until it is
> selected as the next active phase.

## Source-of-truth & traceability

- PRD §9 (停用、废弃与删除规则), §8 (模板版本) — confirmed.
- permission-matrix §5 (恢复模板 / 废弃模板 / 停用版本 / 恢复版本 rows) — confirmed.
- requirements-plan 已确认：停用规则 / 模板状态 / 模板版本.
- Backlog origin: `ux-upgradeability-optimization-plan.md` UXC3 (`(needs backend work)`).

## Gap evidence

- `template/domain/LifecycleAction.java` only models `SUBMIT_FOR_TEST`,
  `RECORD_TEST_DECISION`, `SUBMIT_FOR_APPROVAL`, `RECORD_APPROVAL_DECISION`, `PUBLISH`.
- `TemplateLifecycleStatus` already has `STOPPED` / `DEPRECATED` but **no transition
  actions, no version-level deactivate/restore, no recovery/deprecate impact preview**.
- No backend endpoints or UI for Stop / Deprecate / Restore or version deactivate/restore.

## Behavior goal

Make the post-publish lifecycle fully operable per confirmed rules: stop (reversible),
restore, deprecate (permanent), plus release-version-level deactivate/restore — each with
impact preview, mandatory reason, secondary confirmation, role/group-scoped authorization,
and audit. Logical-delete only (no hard delete), consistent with matrix §13.

### Actors / roles

| Action | GLOBAL | GROUP (scoped) | others |
| --- | --- | --- | --- |
| Stop template / version | ✓ | ✓ | ✗ |
| Restore template / version | ✓ | ✓ | ✗ |
| Deprecate template | ✓ | ✓ | ✗ |
| Delete template (logical) | ✓ | ✗ | ✗ |

### Acceptance scenarios

- **Given** a PUBLISHED template, **When** GROUP admin stops it, **Then** all release
  versions become non-callable, impact preview lists affected callers/AD groups + default
  route impact, reason + secondary confirm captured, audit recorded; runtime calls return
  the confirmed not-callable error.
- **Given** a STOPPED template, **When** an admin restores it, **Then** non-stopped/
  non-deprecated versions re-enter the callable candidate set per API policy; impact preview
  shows callable-version delta; audit recorded.
- **Given** a STOPPED template with no callable versions, **When** an admin deprecates it,
  **Then** it becomes permanently non-callable (no restore), reason + impact preview + audit
  recorded.
- **Given** a single release version, **When** it is deactivated, **Then** only that version
  is non-callable; other versions remain callable per policy; restore re-enables only it.
- **Given** a non-admin role, **When** any of the above is attempted, **Then** fail-closed
  `403` with a generic safe message; no existence leakage.

## Tasks

| ID | Task | Status |
| --- | --- | --- |
| P16-T01 | Extend `LifecycleAction` + transitions: STOP / RESTORE / DEPRECATE (template) | Not Started |
| P16-T02 | Release-version deactivate / restore (version-scoped state + repository) | Not Started |
| P16-T03 | Recovery / deprecate / version impact-preview service (affected callers, AD-group scope, default-route impact, callable-version delta, recent-call summary) | Not Started |
| P16-T04 | Management endpoints with reason + secondary-confirm contract + group-scoped authorization (fail-closed) | Not Started |
| P16-T05 | Audit events (stop/restore/deprecate/version-deactivate/version-restore) with reason + scope summary | Not Started |
| P16-T06 | Runtime callable-version resolution honors stopped/deprecated/version states (regression tests) | Not Started |
| P16-T07 | UI: lifecycle actions on `TemplateDetailView` (impact preview modal, confirm, audit link); version list deactivate/restore | Not Started |
| P16-T08 | Logical-delete template (GLOBAL only) + no hard-delete enforcement | Not Started |

## Exit criteria (phase)

- Stop/Restore/Deprecate (template) + version deactivate/restore implemented with real
  persistence and impact preview; logical-delete only.
- Runtime resolution honors all states; regression tests prove non-callable behavior.
- Role/group-scoped fail-closed authorization; audit on every mutating action.
- Green gates: `mvn -B -ntp -f backend/pom.xml verify` + frontend lint/type-check/test/build.
- permission-matrix §5 + PRD §9 cross-links verified; execution-sync-ledger evidence recorded.

---

## Task-level behavior specs & test-first plan (2026-06-23)

> Anchored to current code. Activation still requires P13 Done + plan-orchestrator
> selection of P16 as active (single-active-phase invariant). Each task: write the listed
> failing test(s) first → smallest implementation to green → refactor → doc sync.

### Current-state anchors (verified)

- `template/domain/LifecycleAction.java` — 5 values, no stop/restore/deprecate/version ops.
- `TemplateLifecycleStatus` — has `STOPPED`/`DEPRECATED` (no transitions wired).
- `TemplateEntity` — already has `deletedAt` (logical delete ready) + single
  `lifecycleStatus` + single `releaseVersion`.
- `TemplateVersionEntity` — already has per-version `lifecycleStatus` + `releaseVersion`.
- `TemplateLifecycleService` — `transition()` + `recordLifecycle()` helpers reusable; new
  flows follow the same pattern.
- `TemplateController` — lifecycle endpoints under `POST /api/management/v1/templates/{id}/lifecycle/*`.
- `GroupAccessService` — capability methods; **add** stop/restore/deprecate/version caps here.
- **Runtime callability gap:** `RuntimeGenerationService:125`, `BatchGenerationService:338`,
  `ContractAssemblyService:73` only check `template.lifecycleStatus == PUBLISHED`. They do
  **not** consult `TemplateVersionEntity.lifecycleStatus`, so version-level stop is not yet
  honored — fixed in T06.

### Role sets (permission-matrix §5, authoritative)

| Action | Allowed roles |
| --- | --- |
| Stop template | GLOBAL, GROUP (scoped), MASTER_DESIGNER, TEMPLATE_AUTHOR |
| Restore template | GLOBAL, GROUP (scoped) |
| Deprecate template | GLOBAL, GROUP (scoped) |
| Deactivate / restore release version | GLOBAL, GROUP (scoped) |
| Delete template (logical) | GLOBAL only |

New `GroupAccessService` capabilities: `canStopTemplates`, `canRestoreOrDeprecateTemplates`,
`canManageReleaseVersionState`, `canDeleteTemplate`.

### P16-T01 — Stop / Restore / Deprecate (template)

- **Behavior:** Stop a PUBLISHED template (→ STOPPED, reversible); restore STOPPED → PUBLISHED;
  deprecate STOPPED (with no callable versions) → DEPRECATED (terminal). Each requires reason
  + secondary-confirm flag; transitions reuse `transition()`/`recordLifecycle()`.
- **Valid transitions:** `PUBLISHED → STOPPED`; `STOPPED → PUBLISHED`; `STOPPED → DEPRECATED`.
  All others throw `TemplateValidationException("api.error.template.invalidState")`.
- **Failing tests first** (`TemplateLifecycleStopServiceTest`):
  1. `stop_fromPublished_setsStopped_andRecordsAudit`
  2. `stop_fromDraft_throwsInvalidState`
  3. `restore_fromStopped_setsPublished`
  4. `deprecate_fromStopped_withNoCallableVersions_setsDeprecated`
  5. `deprecate_fromStopped_withCallableVersion_throwsInvalidState`
  6. `deprecate_fromPublished_throwsInvalidState`
  7. `stop_byTesterRole_throwsAccessDenied` (fail-closed)
- **Impl:** add `STOP`/`RESTORE`/`DEPRECATE` to `LifecycleAction`; add `stop/restore/deprecate`
  methods + capability guards.

### P16-T02 — Release-version deactivate / restore

- **Behavior:** Deactivate a single PUBLISHED version (`TemplateVersionEntity` → STOPPED);
  restore STOPPED version → PUBLISHED. Template-level status unchanged; other versions keep
  their own state. (v1 keeps the existing single dev-version-1 shape; add a version-scoped
  state setter + lifecycle record keyed to the version.)
- **Failing tests first** (`ReleaseVersionStateServiceTest`):
  1. `deactivateVersion_setsVersionStopped_templateStaysPublished`
  2. `restoreVersion_setsVersionPublished`
  3. `deactivateVersion_alreadyStopped_throwsInvalidState`
  4. `deactivateVersion_byUnauthorizedGroup_throwsAccessDenied`
- **Impl:** version-state methods on the service; add version id/release-version to the
  lifecycle record; `canManageReleaseVersionState` guard.

### P16-T03 — Impact-preview service (recovery / deprecate / version)

- **Behavior:** Read-only preview before any stop/restore/deprecate/version op: affected
  callers / AD-group scope summary, default-route impact, callable-version delta, recent-call
  summary. No mutation. Returns a structured `LifecycleImpactPreviewView` (non-sensitive only).
- **Failing tests first** (`LifecycleImpactPreviewServiceTest`):
  1. `preview_forStop_listsCallableVersionsAffected`
  2. `preview_forDeprecate_flagsDefaultRouteImpact`
  3. `preview_forVersionDeactivate_showsCallableVersionDelta`
  4. `preview_excludesSensitiveData` (no variable values / full members)
- **Impl:** new `LifecycleImpactPreviewService` reading template/version + API policy +
  recent-call summary; reuse existing default-route resolution from `apimgmt`.

### P16-T04 — Management endpoints (reason + secondary confirm, group-scoped)

- **Behavior:** New `POST .../lifecycle/stop|restore|deprecate`, `POST
  .../versions/{releaseVersion}/deactivate|restore`, and `POST .../lifecycle/impact-preview`.
  Request bodies carry `reason` (required for mutations) + `confirmed` flag; preview is a
  GET-like POST with no mutation. Unauthorized → fail-closed `403` generic message.
- **Failing tests first** (`TemplateLifecycleControllerTest` slice):
  1. `stop_missingReason_returns400`
  2. `stop_notConfirmed_returns409OrValidation` (confirm contract)
  3. `stop_authorized_returns200_withUpdatedStatus`
  4. `stop_unauthorizedRole_returns403_genericMessage`
  5. `impactPreview_returnsPreview_withoutMutation`
- **Impl:** controller methods delegating to T01–T03 services; envelope per existing pattern.

### P16-T05 — Audit events

- **Behavior:** Emit lifecycle audit for stop/restore/deprecate/version-deactivate/
  version-restore with action, from/to status, reason, actor, group-scope summary,
  release-version (for version ops); no sensitive plaintext.
- **Failing tests first** (extend `ManagementAuditRecorder`/lifecycle audit test):
  1. `stop_recordsLifecycleAuditWithReasonAndScope`
  2. `versionDeactivate_recordsVersionScopedAudit`
  3. `audit_excludesSensitiveValues`
- **Impl:** extend `recordLifecycle` (or audit recorder) to carry reason + scope summary.

### P16-T06 — Runtime resolution honors stopped/deprecated/version states (regression)

- **Behavior:** Runtime generate/batch/contract resolution must treat a template as
  non-callable when STOPPED/DEPRECATED, and a resolved release version as non-callable when
  that **version** is STOPPED — even if the template is PUBLISHED.
- **Failing tests first** (regression, `TemplatePlatformSliceTest` or new
  `RuntimeCallabilityTest`):
  1. `generate_onStoppedTemplate_returnsNotCallableError`
  2. `generate_onDeprecatedTemplate_returnsNotCallableError`
  3. `generate_onStoppedVersion_whileTemplatePublished_returnsNotCallableError` ← **currently fails**
  4. `contract_excludesStoppedVersionsFromCallableList`
- **Impl:** update the three call sites to also check resolved `TemplateVersionEntity`
  lifecycle state; keep the confirmed runtime error code/category/envelope.

### P16-T07 — UI (TemplateDetailView)

- **Behavior:** Lifecycle action buttons (Stop/Restore/Deprecate) gated by capabilities;
  version list deactivate/restore; each opens an impact-preview modal → reason input →
  `useConfirmAction` confirm → call endpoint → refresh; audit link surfaced. Hidden (not
  disabled) when capability absent (matrix §13).
- **Failing tests first** (Vitest): capability-gated rendering + confirm-flow calls the
  right endpoint; preview shown before confirm.

### P16-T08 — Logical-delete template (GLOBAL only)

- **Behavior:** `DELETE /api/management/v1/templates/{id}` sets `deletedAt` (logical),
  GLOBAL only; deleted templates excluded from lists/runtime/authz; no hard delete.
- **Failing tests first** (`TemplateDeleteServiceTest`):
  1. `delete_byGlobalAdmin_setsDeletedAt`
  2. `delete_byGroupAdmin_returns403_userDeleteNotAllowed` (→ reuse template delete denial)
  3. `deletedTemplate_excludedFromList_andRuntime`
- **Impl:** `canDeleteTemplate` (GLOBAL only); soft-delete + repository filters on `deletedAt`.

### Migration & sequencing

- Flyway: `template_lifecycle_record` **already has** `release_version` + `comment_summary`
  (verified). Reason can reuse `comment_summary`; add only an optional
  `actor_group_scope_summary` column if a dedicated scope field is wanted. New
  `LifecycleAction` enum values are stored as `EnumType.STRING` (no schema change needed for
  the action column, length 64 is sufficient). No template-status column change required —
  `STOPPED`/`DEPRECATED` already exist.
- Suggested intra-phase order: T01 → T02 → T06 (close the runtime gap early) → T03 → T04 →
  T05 → T08 → T07.
- Coverage: lifecycle/authorization are core/security → JaCoCo changed-line ≥90%.
