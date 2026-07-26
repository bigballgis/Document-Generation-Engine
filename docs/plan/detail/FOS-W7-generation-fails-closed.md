# FOS-W7 — Generation fails closed

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W7 · **Status:** **Done** (gates green; merge + MAIN closeout pending)
**Slice id:** `fos-generation-fails-closed` · worktree `../DGE-fos-generation-fails-closed` · branch `feat/fos-generation-fails-closed`
**Task Master:** **#177** · **delivery_lane:** **full**
**Origin:** B2, B7, B8, B9, B10, B15
**Evidence:** [fos-generation-fails-closed/gates.md](../evidence/fos-generation-fails-closed/gates.md)
**Behavior:** [fos-generation-fails-closed.md](../../behavior/fos-generation-fails-closed.md)

---

## Before code

```powershell
git worktree add "..\DGE-fos-generation-fails-closed" -b feat/fos-generation-fails-closed origin/main
```

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W7-1 | **P0** | Missing pinned clause fails generation (no silent drop) |
| W7-2 | **P1** | Publish-gate API-policy item must be able to fail |
| W7-3 | **P1** | Publish uses the same in-flight version the gate validated |
| W7-4 | **P1** | Binding validate during gate must not pretend to persist |
| W7-5 | **P1** | Change-diff rules JSON parse fails closed |
| W7-6 | **P1** | Nesting validation uses APPROVED/ACTIVE comparable latest |

---

## W7-1 — Pinned clause missing → silent omission

**Severity:** P0
**File:** `TemplateContentModuleReferenceService#resolvePinnedContentStructures`

```java
contentModuleVersionRepository.findById(reference.getContentModuleVersionId())
    .ifPresent(version -> pinnedStructures.put(...));
```

### Implement

`orElseThrow` into the existing pinned-unavailable / rendering error path used elsewhere
in `DocumentGenerationAssemblySupport` (grep `pinned` / `contentModule` unavailable).
Red test: reference points at missing version id → generate fails closed with stable code;
output must not succeed without the clause.

---

## W7-2 — API-policy publish gate cannot fail

**Severity:** P1
**Files:** `TemplateLifecycleApprovalFlowSupport#publish` (materialize then `assertReady`),
`PublishGateCheckItemSupport#apiPolicyItem`

### Implement

1. Assert publish gate **before** materializing a skeleton, **or**
2. Make `apiPolicyItem` require a meaningful condition the skeleton does not auto-satisfy
   (e.g. default route target present / AD group configured — read ADR-0002/0040 and
   existing policy fields; pick the smallest real signal already on `ApiPolicyEntity`).

Red test: publish with no callable default route → gate blocked.

---

## W7-3 — Publish selects a different version than the gate

**Severity:** P1
**Files:** `PublishGateService#evaluate` (`requireInFlightDevVersion`) vs
`TemplateLifecycleEligibilitySupport#requireReleaseCandidateVersion` /
publish path `findByTemplateIdOrderByDevVersionNumberDesc…`

### Implement

Publish must reuse `requireInFlightDevVersion` (the row the gate validated). Add the
reproduction from audit B8: soft-deleted highest `dev_version_number` must not be stamped.

---

## W7-4 — Read-only gate transaction discards binding status writes

**Severity:** P1
**Files:** `TemplateBindingConfigurationService#validateBindings` (writes statuses),
`PublishGateService#evaluate` (`@Transactional(readOnly = true)`)

### Implement

Split pure validation from persistence. Gate calls compute-only; authoring validate
endpoint persists. Red test: stale `validation_status` is not required to change during
gate evaluation; gate still sees computed validity.

---

## W7-5 — Unparseable composition rules → empty diff

**Severity:** P1
**File:** `ChangeDiffDimensionHelperSupport#readRules`

### Implement

Fail closed like `PasteCleaningEvidenceSupport` — explicit invalid marker that blocks /
surfaces in the gate. Never return `List.of()` on parse error.

---

## W7-6 — Nesting walks wrong version graph

**Severity:** P1
**File:** `ContentModuleNestingService#neighbors`

Filter to APPROVED/ACTIVE non-deleted; select by comparable version key from FOS W6-4
(coordinate: if W6 not merged yet, include a minimal ordering fix here or declare
dependency on #176).

---

## Exit

`mvn verify` green; generation fail-closed test; publish-gate tests; deploy evidence;
arch review. TM **#177** → done.
