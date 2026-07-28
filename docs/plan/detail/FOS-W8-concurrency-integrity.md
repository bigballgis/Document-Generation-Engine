# FOS-W8 — Concurrency & integrity

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Wave:** W8 · **Status:** **Done** (`b6307d02` / `954933cc`)
**Evidence:** [fos-concurrency-integrity/gates.md](../evidence/fos-concurrency-integrity/gates.md)
**Behavior:** [fos-concurrency-integrity.md](../../behavior/fos-concurrency-integrity.md)
**Slice id:** `fos-concurrency-integrity` · worktree `../DGE-fos-concurrency-integrity` · branch `feat/fos-concurrency-integrity`
**Task Master:** **#178** · **delivery_lane:** **full**
**Origin:** B11, B12, B17, B18, B20

---

## Before code

```powershell
git worktree add "..\DGE-fos-concurrency-integrity" -b feat/fos-concurrency-integrity origin/main
```

### Tasks

| Id | Sev | Task |
| --- | --- | --- |
| W8-1 | **P1** | Add `@Version` (or equivalent) to template authoring aggregates |
| W8-2 | **P1** | Unique `(template_id, release_version)` + publish locking |
| W8-3 | **P1** | Map `DataIntegrityViolationException` to conflict envelope |
| W8-4 | **P1** | Actionable `invalidState` / `publishGateBlocked` messages |
| W8-5 | **P2** | Deduplicate `assertDraft` helpers |

---

## W8-1 — No JPA `@Version` on authoring aggregates

**Evidence:** `rg '@Version' backend/src/main/java` → none. Only bindings use
`assertExpectedUpdatedAt` for existing rows.

### Implement

Add `@Version` to `TemplateVersionEntity` and child entities mutated by authoring
(variables schema, composition rules, inclusion rules, content-module refs — inventory
via the services that save them). Surface conflicts as the existing binding-conflict style
error (409 + stable code). FE already has CE-U21 `expectedUpdatedAt` patterns — wire
analogous handling where save APIs gain version tokens.

Start with `TemplateVersionEntity` + the highest-traffic children; do not boil the ocean
in one PR if Batch Recommendation splits — note residual in leaf report.

---

## W8-2 — Concurrent publish can duplicate release versions

**Evidence:** no unique constraint on `(template_id, release_version)`; publish unlocked;
repository comments tolerate duplicates.

### Implement

Flyway: unique index on `(template_id, release_version) WHERE deleted_at IS NULL AND release_version IS NOT NULL` (adjust to actual column nullability). Take a row lock /
distributed lock already used elsewhere on the template at publish start (grep Redisson /
ShedLock usage — do not invent Redisson if deferred by ADR-0039; prefer DB unique +
`SELECT … FOR UPDATE` on the template row).

Red test: second concurrent publish fails closed with conflict, not silent duplicate.

---

## W8-3 — Constraint collisions become generic 500

**File:** `GlobalExceptionHandler` / shared advice — no `DataIntegrityViolationException`
handler.

### Implement

Map to 409 conflict envelope with stable code per ADR-0006. Cover unique anchor binding
and nesting edge collisions.

---

## W8-4 — Operator errors lack which gate / which state

**Files:** template services throwing bare `"api.error.template.invalidState"` /
`"api.error.template.publishGateBlocked"`

### Implement

Pass offending status / first blocking gate code into message arguments (resolver already
supports arguments — see `api.audit.lifecycle.publishedRelease`). Update FE catalogues if
they need argument placeholders.

---

## W8-5 — Triplicated `assertDraft`

Delegate `CompositionInclusionRuleService` / `TemplateContentModuleReferenceService`
copies to `TemplateAccessGuardSupport`.

---

## Exit

`mvn verify` + FE if API shapes change; deploy; arch review (concurrency). TM **#178** → done.
