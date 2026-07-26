# FOS-W6 — Lifecycle cannot corrupt

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W6 · **Status:** **Not Started**
**Slice id:** `fos-lifecycle-cannot-corrupt` · worktree `../DGE-fos-lifecycle-cannot-corrupt` · branch `feat/fos-lifecycle-cannot-corrupt`
**Task Master:** **#176** · **delivery_lane:** **full** (backend lifecycle / Flyway may apply)
**Origin:** B1, B3, B4, B5, B6, B13, B16, B19, B23
**Open decision:** **OD-FOS-3** blocks any new `PENDING_RELEASE → DRAFT` transition — implement everything else first; leave B16 as a documented residual if OD unanswered

---

## Before code

```powershell
git worktree add "..\DGE-fos-lifecycle-cannot-corrupt" -b feat/fos-lifecycle-cannot-corrupt origin/main
```

TDD against Spring tests under `backend/src/test/java/com/bank/docgen/template` and
`contentmodule` / `master`. Prefer extending existing lifecycle test classes.

### Task order

| Id | Sev | Task |
| --- | --- | --- |
| W6-1 | **P0** | Restore must not blanket re-publish withdrawn STOPPED versions |
| W6-2 | **P0** | Refuse delete of current master revision; storage delete after commit |
| W6-3 | **P0** | Content-module stop-use / review target an explicit version |
| W6-4 | **P1** | Order module versions by numeric semver, not string |
| W6-5 | **P1** | Stop supersede from nulling `release_version` |
| W6-6 | **P2** | Bulk lifecycle updates ignore soft-deleted rows |
| W6-7 | **P2** | Master review decision enum + `statusSnapshot` honesty |
| W6-8 | **blocked** | PENDING_RELEASE non-destructive exit — **OD-FOS-3** |

---

<a id="w6-1"></a>
## W6-1 — `restore` re-publishes every STOPPED release

**Severity:** P0
**Files:**
- `backend/.../template/service/TemplateLifecycleService.java` (`restore`)
- `backend/.../template/service/TemplateLifecycleTransitionSupport.java`
  (`syncStoppedVersionsToPublished`)
- `TemplateVersionRepository.bulkUpdateLifecycleStatus`

**Evidence:** restore calls `syncStoppedVersionsToPublished` which
`bulkUpdateLifecycleStatus(templateId, STOPPED, PUBLISHED, …)` with no filter for
versions stopped by deliberate `deactivateVersion`.

### Implement

Restore only versions that were stopped by the owning template-level STOP (use lifecycle
records / reason linkage already written on STOP — read `TemplateLifecycleRecord` actions
first). Versions individually deactivated must stay STOPPED.

Red test: template STOPPED with release A deactivated earlier and release B stopped by
template STOP → restore republishes only B.

### Do NOT

- Do not change Clone/`isInFlight` rules from #165.

---

<a id="w6-2"></a>
## W6-2 — Current master revision can be deleted

**Severity:** P0
**File:** `backend/.../master/service/MasterRevisionLineService.java` (`deleteRevisionLine`)

### Implement

1. Refuse when `revisionLineId.equals(master.getCurrentRevisionLineId())` with a stable
   `api.error.*` key.
2. Move MinIO/object storage delete to after successful DB commit (TransactionSynchronization
   afterCommit, matching patterns elsewhere — grep `registerSynchronization` /
   `afterCommit`).

Red tests for both behaviours.

---

<a id="w6-3"></a>
## W6-3 — Clause stop-use / review resolves wrong version (string sort)

**Severity:** P0
**Files:**
- `ContentModuleLifecycleService#resolveTargetVersion` / `#applyOperation`
- `ContentModuleReviewService#resolveTargetVersion`
- request DTOs `ContentModuleLifecycleOperationApplyRequest`,
  `ContentModuleReviewTransitionRequest`

### Implement

Require explicit `versionId` or `semanticVersion` on stop-use / approve / reject requests
(OpenAPI + FE call sites). Reject when the target is missing. Until FE is updated, management
UI must pass the version shown on screen (find the version dialog actions).

Also fix ordering root cause in W6-4 so "latest" helpers stop using string sort.

Red tests: two SUBMITTED versions → approve names version B → only B becomes APPROVED.

---

<a id="w6-4"></a>
## W6-4 — `OrderBySemanticVersionDesc` is lexicographic

**Severity:** P1
**Files:** `ContentModuleVersionEntity.semanticVersion` + repository finders using
`OrderBySemanticVersionDesc`

### Implement

Persist comparable major/minor/patch (or order by parsed integers in queries). Ensure
`1.10` > `1.9`. Migrate with Flyway if new columns are required; backfill from existing
strings using the same parser as `parseVersionNumber` helpers.

Update nesting / lifecycle / review resolvers to use the comparable order.

---

<a id="w6-5"></a>
## W6-5 — Supersede nulls `release_version`

**Severity:** P1
**File:** `TemplateLifecycleApprovalFlowSupport#supersedePublishedVersionsWithSameRelease`

### Implement

Keep `release_version`; disambiguate with existing
`findFirst…OrderByDevVersionNumberDesc` (already used at runtime). Adjust any test that
asserted nulling. Ensure `restoreVersion` still finds historical rows.

---

<a id="w6-6"></a>
## W6-6 — Bulk lifecycle updates touch soft-deleted rows

**Severity:** P2
**File:** `TemplateVersionRepository#bulkUpdateLifecycleStatus` /
`#bulkUpdateAllLifecycleStatus`

Add `AND v.deletedAt IS NULL`.

---

<a id="w6-7"></a>
## W6-7 — Master `statusSnapshot` / REJECTED dead state

**Severity:** P2
**Files:** `MasterDocumentFileMutationSupport#replaceFile`,
`MasterDocumentReviewSupport#decideReview`

### Implement

Snapshot `DRAFT` on new upload; update current line `statusSnapshot` on review decision.
Compare decision against the parsed enum (not string `"APPROVED"`). Either use `REJECTED`
or remove it from the enum in a follow-up — prefer **use it** if product copy already
mentions rejection.

---

<a id="w6-8"></a>
## W6-8 — PENDING_RELEASE has no non-destructive exit — OD-FOS-3

**Status:** **Blocked** until user answers OD-FOS-3.

Document the proposed transition (reuse rejection lifecycle record shape) in the leaf
report; do **not** implement without confirmation.

---

## Exit

W6-1…W6-7 green (`mvn verify`); FE updates for explicit module version ids covered by
Vitest/E2E where UI changed; deploy evidence; architecture review (lifecycle/authz focus).

TM **#176** → done. Carry OD-FOS-3 residual explicitly.
