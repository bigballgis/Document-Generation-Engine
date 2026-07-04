# SpotBugs Exclusion Ratchet Plan (SOR-A05)

**Status:** Active — slice 0 complete (REC_CATCH_EXCEPTION removed).  
**Filter file:** `backend/config/spotbugs/exclude.xml`  
**Automated guard:** `SpotBugsExclusionRatchetTest` (Surefire)

## Baseline (2026-07-04, SOR-A05 slice 0)

| Metric | Value | Notes |
| --- | --- | --- |
| `<Match>` blocks | **3** | `BASELINE_MATCH_COUNT=3` in guard test |
| Deferred `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` | ~166 | Immutability / defensive-copy pass |
| Deferred `REC_CATCH_EXCEPTION` | **0** | Removed from exclude.xml; 11 call sites narrowed |
| Framework token exclusions | 1 block | `CT_CONSTRUCTOR_THROW,SE_BAD_FIELD` (Spring auth tokens) |
| Test-class exclusion | 1 block | `~.*Test$` (tests out of gate scope) |

**Banned patterns (must not reappear without a new ratchet slice + guard update):** `REC_CATCH_EXCEPTION`

## Cadence

| Slice | Target | Scope | Exit criteria |
| --- | --- | --- | --- |
| **0 (Done)** | `REC_CATCH_EXCEPTION` | 11 findings across storage, master, rendering, runtime, template | 0 SpotBugs REC_CATCH; pattern removed from exclude.xml |
| **1 (next)** | `EI_EXPOSE_REP` — security-critical | `ManagementSessionClaims`, runtime credential views, JWT/session DTOs | −10…−15 findings; update baseline count in plan + test |
| **2** | `EI_EXPOSE_REP` — persistence entities | JPA entities with collection getters | Module-by-module; JaCoCo unchanged |
| **3** | `EI_EXPOSE_REP` — API views / records | MapStruct-generated + hand-written views | Reduce deferred count below 100 |
| **4+** | Remaining `EI_EXPOSE_REP` | Quarterly −20% of deferred count | Remove blanket `<Match>` when &lt; 10 remain |

## Slice 0 evidence (REC_CATCH fixes)

Narrow catches with structured logging (fail-closed domain exceptions preserved):

- `MinioObjectStorage.exists` — MinIO declared exceptions
- `MasterDocumentService` — `IOException` on upload / anchor extraction
- `DocxEncryptionService`, `PdfEncryptionService`, `TestPdfConversionService` — `IOException` / `GeneralSecurityException`
- `PreviewArtifactDownloadService`, `PreviewGenerationService`, `DocumentGenerationEngine` — I/O or runtime paths
- `SubmitTestEligibilityService`, `CoverageComputationService` — `JsonProcessingException`

## Guard rules

1. `<Match>` count must equal `BASELINE_MATCH_COUNT` (currently **3**).
2. Banned patterns must not appear in exclude.xml.
3. Increasing `<Match>` count or re-adding a banned pattern requires updating this plan **and** `SpotBugsExclusionRatchetTest` in the same change set.

## Gate

```bash
mvn -B -ntp -f backend/pom.xml verify
```

SpotBugs must report **0** bugs with the tightened filter.

## Related

- OPT-B / B1: `docs/plan/optimization-plan.md`
- SOR-A05: `docs/plan/system-optimization-review-2026-07.md` §8
- Coverage ratchet (parallel): `docs/plan/coverage-ratchet-plan.md`
