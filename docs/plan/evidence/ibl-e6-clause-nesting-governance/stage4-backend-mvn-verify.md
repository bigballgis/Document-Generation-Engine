# IBL-E6 / #133 — Stage 4 backend evidence

| Field | Value |
| --- | --- |
| **Slice** | `ibl-e6-clause-nesting-governance` |
| **Worktree** | `D:/working/DGE-ibl-e6-clause-nesting-governance` |
| **Branch** | `feat/ibl-e6-clause-nesting-governance` |
| **Date** | 2026-07-20 |
| **Gate** | `mvn -B -ntp -f backend/pom.xml verify` |
| **Surefire heap** | `-Dsurefire.argLine=-Xmx1536m -XX:+ExitOnOutOfMemoryError` (default `-Xmx512m` hit OOM mid-suite on this host) |
| **Result** | **BUILD SUCCESS** |

## Counts

| Metric | Result |
| --- | --- |
| Tests | **2300** run, **0** failures, **0** errors, **15** skipped |
| JaCoCo | All coverage checks met (LINE ≥ 0.70 / BRANCH ≥ 0.45) |
| Checkstyle | 0 violations |
| PMD | pass |
| SpotBugs | pass (BugInstance size 0) |

## Architecture quick-fix (pre-Stage 10)

| Finding | Remediation |
| --- | --- |
| #1 Same target / multi `referenceKey` → UNIQUE explode as 500 | **Fixed:** write path collapses resolved Parent→Target to a single edge (first `referenceKey` wins). BDD: 图去重为单一 Parent→Target 边. |
| #2 Malformed `contentStructureJson` silent empty extract | **Fixed (write path):** `ContentModuleNestingStructureSupport.extractReferenceKeys` fail-closed → **422** `CONTENT_MODULE_NESTING_STRUCTURE_INVALID`. Publish closure treats invalid pin structure as unpinned-blocking. |

### Known residual (accepted — not in this quick fix)

- **No full historical backfill migration** for nesting edges from pre-E6 versions. Post-write rebuild / future migration remains the path for legacy rows written before edge sync. Finding #2 historical data residual is accepted for Stage 10+.

## E6-focused tests (sample)

- `ContentModuleNestingServiceTest` (11; incl. same-target dedupe + malformed structure)
- `ContentModuleWhereUsedNestedTest` / `ContentModuleWhereUsedServiceTest`
- `ContentModuleLifecycleImpactNestedTest`
- `IblE6PublishGateNestingTest`
- `StructuredContentDocxWriterTest#failsClosedWhenContentModuleNestingCycleDetected`

## Notes

- Demo/e2e DOCX binaries may appear dirty from unrelated fixture generators — **out of E6 scope**; do not include in feature commit.
