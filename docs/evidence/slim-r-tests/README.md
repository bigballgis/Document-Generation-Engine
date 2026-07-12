# Slim R-tests — residual Wave evidence (AuditQueryServiceTest DRY)

Branch: `feat/slim-r-tests`  
Base: `main` @ `09fca45`  
Date: 2026-07-12  
Scope: behavior-preserving CQ-05 fixture / assertion helper extraction for residual audit query tests; no production code changes.

## Approach

1. Extract shared Mockito arrange helpers into `AuditQueryServiceTestSupport` (same package, Wave 4 style).
2. Collapse duplicated role sessions, entity fixtures, audit-allow stubs, and null-heavy `queryManagementEvents` / `queryLifecycleEvents` call sites.
3. Keep all 17 critical audit query scenarios and assertions (access scope, security/retention visibility, enrichment, requestId routing, generation-by-external-id).

## LOC before / after (hotspot)

| File | Before | After | Delta |
| --- | ---: | ---: | ---: |
| `AuditQueryServiceTest.java` | 746 | 318 | **-428** |
| `AuditQueryServiceTestSupport.java` *(new)* | 0 | 440 | +440 |
| **Cluster total (hotspot + helper)** | **746** | **758** | **+12** |

Hotspot file is the residual called out by Wave 4; helper extraction trades a near-flat cluster LOC for readable arrange/query reuse. Coverage of critical paths is unchanged (17/17 green).

## What changed

- New abstract support: `globalAdmin` / `groupAdmin` / `auditAdmin` / `templateAuthor` sessions; management / retention / security / runtime / lifecycle entity fixtures; `allowAuditRead` / page stubs; default-page query/export wrappers.
- `AuditQueryServiceTest` extends the support and deletes duplicated private helpers / verbose null argument lists.
- Duplicate `ManagementUserDisplayService` import removed as part of the rewrite.

## Gates

| Gate | Result |
| --- | --- |
| Targeted `AuditQueryServiceTest` | GREEN — 17 tests |
| `mvn -B -ntp -f backend/pom.xml verify` | GREEN (1347 tests, 7 skipped) |

Full Maven console dumps are intentionally **not** retained in git (noise); gate outcomes recorded here only.

## Residuals

- Cluster LOC is roughly flat (+12) because fixtures moved 1:1 into the support; further gains would require production API ergonomics (query filter DTO) rather than test-only wrappers.
- Platform slice private helper tail (~250 LOC) and Dashboard XHR AggregateError stderr noise remain pre-existing Wave 4 residuals.
