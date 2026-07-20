---
id: ADR-0066
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-20
deciders: architecture, template-governance, content-module-governance, api, doc-keeper
owners:
  - template-governance
  - api
adrNumber: "0066"
topic: template-lifecycle
related:
  - docs/behavior/ibl-e5-effectivefrom-bulk-repin.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/behavior/ce-k08-clause-legal-metadata.md
  - docs/behavior/ce-u07-clause-outdated-bump.md
  - docs/domain/domain-model.md
  - docs/api/contract-outline.md
  - docs/adr/template-lifecycle/0065-legal-entity-document-brand-variants.md
---

# ADR 0066: Future `effectiveFrom` Publish Gate + Bulk Re-Pin Tooling

## Status

**Accepted** (2026-07-20) — IBL-E5 / Task Master **#132** / F27 (`effectiveFrom` + bulk half).

| Gate | Note |
| --- | --- |
| Wave E / F27 residual | **Yes** — E4 closed document-brand half; this ADR covers remaining F27 half |
| BDD lock | [ibl-e5-effectivefrom-bulk-repin.md](../../behavior/ibl-e5-effectivefrom-bulk-repin.md) **ready** — **BDD-IBL-E5-001…017** lock **E5-C\***; **no remaining product fork** for this leaf |
| File status | **Accepted** (2026-07-20) — doc-keeper stage 3; Decision = E5-C\* |

`sourceOfTruth: true` while Accepted.

This ADR does **not** claim IBL-E5 implementation Done, flip checklist **#3b** / **#5a**, remove SPECIMEN (PD-6), embed licensed fonts (PD-7), activate IBL-E6/E7 / #119, rewrite E4 DocumentBrand/LegalEntity, introduce a deferred/`SCHEDULED` publish lifecycle, or claim Wave E / IBL program Done / go-live.

**Amends** CE-K08 publish stance: future `effectiveFrom` **does** hard-block template publish via a **new** check code (orthogonal to `CONTENT_MODULE_EFFECTIVE_EXPIRED`). Does **not** amend ADR-0062 / 0063 / 0064 / 0065 field semantics.

## Context

International bank letters need:

1. Content-module versions with a future `effectiveFrom` must **not** ride along into a newly published template release before that Instant.
2. After clause upgrades, operators need **group-scoped bulk re-pin** across many draft templates — beyond CE-U07’s single-template bump UI — with **dry-run** and **audit**.

Today (pre-E5):

- CE-K08 stores optional `effectiveFrom` / `effectiveTo` and hard-blocks only **expired** `effectiveTo` at publish (`CONTENT_MODULE_EFFECTIVE_EXPIRED`). Future `effectiveFrom` explicitly **does not** block (K08-C6 / LM-011).
- CE-U07 provides per-template out-of-date badge + bump / bump-all on one draft; no cross-template mass migration API.
- F27 document brands are closed by IBL-E4 / ADR-0065; F27 residual maps to **IBL-E5**.

## Decision

1. **Hard-block, not deferred publish lifecycle**  
   Authors may continue to **schedule** intent by writing a future `effectiveFrom` on CM **draft** versions (CE-K08 write path unchanged). Template publish is **hard-blocked** while any pinned CM version has `effectiveFrom != null && utcNow.isBefore(effectiveFrom)`. **No** new template lifecycle state such as `SCHEDULED` / deferred activation of published releases.

2. **Not-started predicate (UTC Instant)**  
   - FAIL when `effectiveFrom != null && utcNow.isBefore(effectiveFrom)`.  
   - `effectiveFrom == null` → this check does not fail.  
   - `utcNow` equal to `effectiveFrom` → PASS (same edge policy as CE-K08 equal-`effectiveTo` not expired).

3. **New publish-gate check code**  
   `PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_NOT_STARTED` (stable messageKey in OpenAPI). Must **not** overload `CONTENT_MODULE_EFFECTIVE_EXPIRED`. Publish-gate GET and publish execution share one evaluator; FAIL summary lists offending `referenceKey` + module/version + `effectiveFrom`.

4. **Orthogonality**  
   Keep `CONTENT_MODULE_EFFECTIVE_EXPIRED` unchanged. Other hard gates (including `CONTENT_MODULE_REFERENCES`) unchanged. Runtime generation for **already published** locked versions does **not** gain new failures solely from clock vs `effectiveFrom`/`effectiveTo` (same “new publish only” policy as CE-K08 LM-012).

5. **CE-K08 amendment**  
   K08-C6 / BDD-CE-K08-LM-011 “future effectiveFrom does not block” is **superseded** by this Decision. Field types, catalog filters, and `effectiveTo` expiry gate remain.

6. **Bulk re-pin management API (API-first)**  
   `POST /api/management/v1/content-module-references/bulk-repin` (path fixed in OpenAPI). Required `dryRun` boolean. Selects group-visible **draft** template versions pinning a `contentModuleId`, optionally filtered by `fromSemanticVersion` / `templateIds[]`, and retargets to `toSemanticVersion` **xor** `useLatestApproved=true`.

7. **Apply semantics**  
   Successful mutations reuse `upsertReference` (or equivalent internal service) validation. Published/locked pins → `SKIPPED_LOCKED`. Already at target → `SKIPPED_ALREADY_AT_TARGET`. No match → `SKIPPED_NO_MATCH`. Invalid target → per-item `FAILED` (`BULK_REPIN_TARGET_INVALID` or equivalent). Partial success per template version (E5-C13). `dryRun=true` → zero pin persistence.

8. **Audit**  
   Every call writes a management audit event (suggested action `CONTENT_MODULE_BULK_REPIN`) including actor, group, `dryRun`, selection, counts, and apply before/after pins. Dry-run is audited. No clause body / variables.

9. **Authorization**  
   Reuse `authorTemplates` group boundary; no new roles or capability bits. Unauthorized → `403`/`404` per existing convention (dry-run and apply alike).

10. **Frontend**  
    `frontend_ui_in_scope=false`. No management UI bulk console as a Done gate. CE-U07 single-template UI need not expand in this leaf.

11. **Out of scope**  
    IBL-E6/E7, PD-6/PD-7, #119 Word, checklist **#3b**/**#5a** GO, E4 brand rewrites, runtime as-of dynamic re-pin, cross-group silent writes, package-format migration as a substitute for the online tool.

Normative behavior scenarios: **BDD-IBL-E5-001…017** in [ibl-e5-effectivefrom-bulk-repin.md](../../behavior/ibl-e5-effectivefrom-bulk-repin.md) (**E5-C1…C23**).

## Alternatives Considered

| Option | Verdict |
| --- | --- |
| **A. Soft-warn only on future effectiveFrom** | Rejected — bank fail-closed; F27 requires enforcement. |
| **B. Publish now + SCHEDULED activation later** | Rejected for this leaf — new lifecycle complexity; E5-C1 locks hard-block. |
| **C. Overload CONTENT_MODULE_EFFECTIVE_EXPIRED for not-started** | Rejected — conflates expiry vs not-started observability. |
| **D. UI-only bump-all across catalog** | Rejected as sole delivery — owners are backend; need API + dry-run + audit. |
| **E. Hard-block + API bulk re-pin with dry-run/audit** (this Decision) | **Selected** — matches program acceptance and CE-K08 gate style. |

## Consequences

- F27 residual (`effectiveFrom` / bulk) closes at the **decision** layer once Accepted; implementation remains IBL-E5 / #132 delivery work.
- CE-K08 docs/tests that asserted “future effectiveFrom does not block” must be updated to expect `CONTENT_MODULE_EFFECTIVE_NOT_STARTED`.
- Operators gain a governable, auditable mass re-pin tool without a mandatory UI.
- No deferred publish state machine enters the product from this leaf.

## Related Documents

- Behavior: [ibl-e5-effectivefrom-bulk-repin.md](../../behavior/ibl-e5-effectivefrom-bulk-repin.md)
- Program: [intl-bank-letter-readiness-program.md](../../plan/intl-bank-letter-readiness-program.md) §7 (IBL-E5, F27)
- CE-K08: [ce-k08-clause-legal-metadata.md](../../behavior/ce-k08-clause-legal-metadata.md)
- CE-U07: [ce-u07-clause-outdated-bump.md](../../behavior/ce-u07-clause-outdated-bump.md)
- E4 (orthogonal): [0065-legal-entity-document-brand-variants.md](./0065-legal-entity-document-brand-variants.md)
- API: [contract-outline.md](../../api/contract-outline.md), [openapi-v1.yaml](../../api/openapi-v1.yaml)
