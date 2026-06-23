# P17 — Per-Domain API Policy Governance (Detailed Plan)

**Phase status:** In Progress (impact-preview seam Done 2026-06-23) | **Depends on:** P6
**Confirmed:** 2026-06-23 (deep-review gap G2 — promoted from UXF3 backlog)

> Single-active-phase invariant: P13 completed **Done** (2026-06-23); there is currently no
> single active phase. P17 stays `Not Started` until selected as the next active phase.

## Source-of-truth & traceability

- PRD §11–12 (动态 API / API 授权 — 配置域导航、影响预览、`policyVersion`、回滚) — confirmed.
- permission-matrix §7 (API 管理权限 — 配置域、影响预览硬阻断/警告、policyVersion、回滚) — confirmed.
- PRD §11 default 路径目标版本治理（影响预览、立即生效、回滚、审计）— confirmed.
- audit: PRD §14 / matrix §10 — `API_POLICY_UPDATED` + `changedAreas` baseline.
- Backlog origin: `ux-upgradeability-optimization-plan.md` UXF3 / finding X3.

## Gap evidence

- `apimgmt/web/ApiManagementController` + `UpsertApiPolicyRequest` expose a **single
  monolithic `PUT` upsert**; no per-domain save, no impact preview, no `policyVersion`
  lineage, no rollback, no `changedAreas` audit.
- `ApiPolicyEntity` lacks `policyVersion` / version history; default-route change has no
  dedicated governance flow or impact preview.

## Behavior goal

Replace the monolithic policy upsert with confirmed per-domain governance: each config
domain (AD-group authorization, output policy, batch limits, encryption capability,
default-route target) is edited and saved independently, requires a re-runnable impact
preview that distinguishes hard-blocks (prevent save) from warnings (confirm-to-proceed),
produces a new `policyVersion` on success, supports rollback as a new controlled change,
and emits `API_POLICY_UPDATED` audit with `changedAreas` + diff summary. Immediate effect
only; no scheduled/pending changes; no proactive caller notification.

### Config domains (`changedAreas`)

`AD_GROUP_AUTHORIZATION` · `OUTPUT_POLICY` · `BATCH_LIMIT` · `ENCRYPTION_CAPABILITY` ·
`DEFAULT_ROUTE_TARGET`.

### Acceptance scenarios

- **Given** a template policy, **When** an admin edits the output-policy domain and runs
  impact preview, **Then** preview shows current-vs-candidate diff, affected non-stopped
  versions, caller/AD-group scope summary, recent-call summary, rejected-mode/batch/
  encryption call summary, and expected error codes; save bumps `policyVersion` and audits
  `changedAreas=[OUTPUT_POLICY]`.
- **Given** a candidate change violating a confirmed policy, **When** preview runs, **Then**
  a hard-block (reason/impact/fix structure) prevents save.
- **Given** a candidate change with risk, **When** preview runs, **Then** a warning
  (reason/impact/fix) allows admin confirm-to-proceed, takes effect immediately, audits.
- **Given** an AD-group authorization change, **When** saved, **Then** related authz caches
  are cleared immediately (not waiting 5-min TTL); audit recorded.
- **Given** a default-route change, **When** previewed, **Then** preview includes current vs
  candidate target, authorized-caller scope, recent-call volume, contract diff, idempotency
  impact; immediate effect; no proactive notification; idempotency conflict on stale key.
- **Given** any domain, **When** an admin rolls back, **Then** rollback selects a historical
  config as candidate, re-runs impact preview, takes effect immediately, audits rollback
  source version.
- **Given** GROUP admin out of scope, **When** editing, **Then** fail-closed `403`.

## Tasks

| ID | Task | Status |
| --- | --- | --- |
| P17-T01 | `policyVersion` lineage: entity + history table (Flyway), version on each domain save | Not Started |
| P17-T02 | Per-domain save endpoints (5 domains) replacing monolithic PUT (back-compat seam noted) | Not Started |
| P17-T03 | Impact-preview engine: hard-block vs warning, fixed reason/impact/fix structure, expected error codes | Done (minimal seam: `ApiPolicyImpactPreviewService` + UI confirm; full hard-block/warning matrix → follow-up) |
| P17-T04 | Default-route target governance (preview, immediate effect, idempotency-conflict rule, audit) | Not Started |
| P17-T05 | Rollback as a new controlled change (historical candidate + preview + audit source version) | Not Started |
| P17-T06 | AD-group authorization save → immediate authz cache invalidation | Not Started |
| P17-T07 | Audit `API_POLICY_UPDATED` with `changedAreas`, prev/next `policyVersion`, diff/preview/block/warning summary, rollback flag | Not Started |
| P17-T08 | Contract view surfaces `policyVersion` + per-role policy summary (no secrets/members) | Not Started |
| P17-T09 | UI: config-domain navigation + detail (current summary, candidate editor, field hints, preview, hard-block/warning, save confirm, policyVersion, audit entry) | Done (full-field form + policyVersion + impact preview before save; domain nav → P17-T02) |

## Exit criteria (phase)

- Five config domains saved independently with re-runnable impact preview (hard-block vs
  warning) and `policyVersion` lineage; rollback supported; immediate effect only.
- Default-route governance + idempotency-conflict rule enforced and tested.
- `API_POLICY_UPDATED` audit carries `changedAreas` + diff/preview summary; no sensitive
  plaintext.
- Group-scoped fail-closed authorization; AD-group save invalidates caches.
- Green gates: `mvn verify` + frontend lint/type-check/test/build.
- permission-matrix §7 + PRD §11–12 cross-links verified; ledger evidence recorded.

---

## Task-level behavior specs & test-first plan (2026-06-23)

> Anchored to current code. Activation requires P13 Done + plan-orchestrator selection
> (single-active-phase). Each task: failing test first → smallest implementation → refactor
> → doc sync.

### Current-state anchors (verified)

- `apimgmt/web/ApiManagementController` — single `PUT /api/management/v1/templates/{id}/api/policy`
  (`upsertPolicy`) replaces all domains at once.
- `ApiManagementService.upsertPolicy` (L98–154) — diffs `changedAreas`, bumps `policyVersion`
  once, audits `recordPolicyUpdated`. **No impact preview, no rollback, no per-domain save,
  no AD-group cache invalidation, no default-route specialized flow.**
- `ApiPolicyEntity` — has `policyVersion` (int, `update()` does `+= 1`) but **no version
  history table** (cannot roll back).
- Capability guard: `GroupAccessService.canManageApiPolicy` (GLOBAL/GROUP) — reuse; add
  group-scope object check via `templateService.requireReadableTemplate` (already used).
- Runtime/contract reads policy via `ContractAssemblyService` + `apiPolicyRepository`.

### Confirmed model deltas to fix during P17 (PRD §11–12 / matrix §7)

1. **`changedAreas` value bug:** code emits `ENCRYPTION_POLICY`; confirmed baseline is
   **`ENCRYPTION_CAPABILITY`**. Align (matrix §10, PRD §14).
2. **Batch limits:** entity has a single `maxBatchSize` + `batchEnabled`; confirmed model is
   **`batchLimits.syncMaxItems` + `batchLimits.asyncMaxItems`** (defaults 100 / 10,000,
   policy may lower). Model both.
3. **Encryption:** entity uses two booleans (`docxEncryptionEnabled`/`pdfEncryptionEnabled`);
   confirmed `apiPolicy.encryptionCapabilities` is a capability summary. Keep back-compat but
   surface as the confirmed capability shape.

> These are confirmed-contract corrections, recorded here so they are not silent drift.
> Implement behind the per-domain save so the runtime contract is not broken mid-phase.

### P17-T01 — policyVersion lineage (history table)

- **Behavior:** Every successful domain save snapshots the resulting config as a new
  immutable `api_policy_version` row (templateId, version, changedAreas, full config snapshot,
  updatedBy, updatedAt). Current `ApiPolicyEntity` remains the "head"; history enables rollback.
- **Failing tests first** (`ApiPolicyVersionLineageTest`):
  1. `save_createsHistoryRow_withIncrementedVersion`
  2. `multipleSaves_produceOrderedHistory`
  3. `historySnapshot_isImmutable_afterNextSave`
- **Impl:** Flyway `api_policy_version` table + `ApiPolicyVersionEntity`/repository; write a
  snapshot inside the save transaction.

### P17-T02 — Per-domain save endpoints (replace monolithic PUT)

- **Behavior:** Five endpoints, each saving one domain and bumping version once:
  `PUT .../api/policy/ad-groups`, `/output`, `/batch-limits`, `/encryption`, `/default-route`.
  Keep the old `PUT /policy` as a deprecated back-compat seam (documented) until UI migrates.
- **Failing tests first** (`ApiPolicyDomainSaveServiceTest`):
  1. `saveOutputDomain_changesOnlyOutput_bumpsVersion_auditsOutputPolicy`
  2. `saveBatchLimits_persistsSyncAndAsyncMax`
  3. `saveAdGroups_changesOnlyAdGroups_auditsAdGroupAuthorization`
  4. `saveEncryption_auditsEncryptionCapability` (not `ENCRYPTION_POLICY`)
  5. `saveDomain_onUnpublishedTemplate_throwsTemplateNotPublished`
  6. `saveDomain_byUnauthorizedGroup_returns403`
- **Impl:** per-domain service methods + controller routes; reuse `requireApiAdmin`.

### P17-T03 — Impact-preview engine (hard-block vs warning)

- **Behavior:** `POST .../api/policy/{domain}/impact-preview` (no mutation) returns current
  vs candidate diff, affected non-stopped versions, caller/AD-group scope summary, recent-call
  summary, rejected-mode/batch/encryption call summary, expected error codes, and a list of
  hard-blocks (prevent save) + warnings (confirm-to-proceed). Fixed structure: reason / impact
  / fix. Save endpoints require a matching preview token or `confirmed=true` when warnings exist.
- **Failing tests first** (`ApiPolicyImpactPreviewServiceTest`):
  1. `preview_outputRemovesModeInUse_returnsWarning_withExpectedErrorCode`
  2. `preview_batchLimitBelowConfirmedFloor_returnsHardBlock`
  3. `preview_noChange_returnsEmptyBlocksAndWarnings`
  4. `preview_excludesSensitiveData` (no full AD members / secrets)
  5. `save_withUnconfirmedWarning_returns409OrValidation`
  6. `save_withHardBlock_isRejected`
- **Impl:** `ApiPolicyImpactPreviewService`; classify candidate vs current; map to expected
  runtime error codes.

### P17-T04 — Default-route target governance

- **Behavior:** `PUT .../api/policy/default-route` targets a non-stopped release version
  explicitly; preview shows current vs candidate target, authorized-caller scope, recent-call
  volume, contract diff, idempotency impact; immediate effect only; no proactive notification.
  Stale idempotency key after a route change returns the confirmed idempotency-conflict
  (`DEFAULT_ROUTE_CHANGED`).
- **Failing tests first** (`DefaultRouteGovernanceTest`):
  1. `setDefaultRoute_toStoppedVersion_isHardBlock`
  2. `setDefaultRoute_validTarget_bumpsVersion_auditsDefaultRouteTarget`
  3. `preview_includesContractDiffAndIdempotencyImpact`
  4. `staleIdempotencyKey_afterRouteChange_returnsDefaultRouteChangedConflict` (runtime regression)
- **Impl:** default-route domain save + preview; coordinate with runtime idempotency-conflict
  path (cross-check `IdempotencyService`).

### P17-T05 — Rollback as a new controlled change

- **Behavior:** `POST .../api/policy/rollback` with a target historical `policyVersion`:
  loads that snapshot as candidate, re-runs impact preview, on confirm applies it as a **new**
  version (no in-place rewrite), audits with rollback flag + source version.
- **Failing tests first** (`ApiPolicyRollbackServiceTest`):
  1. `rollback_toPreviousVersion_createsNewHigherVersion`
  2. `rollback_runsImpactPreview_andRespectsHardBlock`
  3. `rollback_audit_recordsRollbackFlag_andSourceVersion`
- **Impl:** rollback service reading history (T01) + reusing preview (T03) + save (T02).

### P17-T06 — AD-group save → immediate authz cache invalidation

- **Behavior:** Saving the AD-group authorization domain clears the related authz cache
  immediately (does not wait the 5-min TTL), per confirmed rule.
- **Failing tests first** (`AdGroupAuthorizationCacheTest`):
  1. `saveAdGroups_invalidatesAuthorizationCache_forTemplate`
  2. `otherDomainSave_doesNotInvalidateAdGroupCache`
- **Impl:** hook cache eviction (AD-group resolver/authz cache) into the AD-group save path.

### P17-T07 — Audit `API_POLICY_UPDATED` enrichment

- **Behavior:** Audit carries `changedAreas`, previous + next `policyVersion`, config diff
  summary, impact-preview summary, hard-block/warning summary, confirm result, rollback flag +
  source version; no sensitive plaintext.
- **Failing tests first** (extend `ManagementAuditRecorder` test):
  1. `policySave_recordsChangedAreasAndPrevNextVersion`
  2. `rollbackSave_recordsRollbackSourceVersion`
  3. `audit_excludesSecretsAndFullMembers`
- **Impl:** extend `recordPolicyUpdated` signature/payload (or add fields) — keep back-compat.

### P17-T08 — Contract view surfaces policyVersion + per-role summary

- **Behavior:** `ContractAssemblyService` / caller-contract view shows current `policyVersion`
  + per-role policy summary (callers see summary only; admins/authors see detail); never
  secrets / full AD members / history ciphertext.
- **Failing tests first** (extend contract slice): caller view excludes detail; admin view
  includes `apiPolicy.*` baseline fields; `policyVersion` present.

### P17-T09 — UI: config-domain navigation + detail

- **Behavior:** Replace monolithic policy form with config-domain nav (AD group / output /
  batch / encryption / default-route) + detail area: current summary, candidate editor, field
  hints, impact preview (hard-block vs warning), save confirm, `policyVersion`, audit entry.
  Field controls per matrix §7 baseline; encryption shows capability selection (no password).
- **Failing tests first** (Vitest): per-domain save calls the right endpoint; preview shown
  before save; warning requires confirm; hard-block disables save.

### Migration & sequencing

- Flyway: new `api_policy_version` history table (T01); add `batch_sync_max_items` /
  `batch_async_max_items` columns (T02 batch model) — keep `max_batch_size` until migrated;
  optionally an `encryption_capabilities` column for the capability summary.
- Suggested order: T01 → T02 → T03 → T04 → T05 → T06 → T07 → T08 → T09.
- API governance is core/security → JaCoCo changed-line ≥90%; keep runtime contract intact
  throughout (per-domain save lands before old PUT is removed).
