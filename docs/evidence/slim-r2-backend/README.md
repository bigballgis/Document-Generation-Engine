# Slim R2-backend — PublishGateService shrink evidence

Branch: `feat/slim-r2-backend` (base `main` @ `451c6c2`)
Date: 2026-07-12

## Approach

- Dead private methods: **none** (rg call-site proof — every private helper had ≥2 refs in-file or via method-ref)
- Behavior-preserving package-private collaborators in the same package
- Public constructor / method signatures unchanged (helpers constructed internally)
- Publish-gate semantics unchanged (check codes, phase filter, blocker aggregation, assertReady message keys)

## LOC before / after (touched god-class)

| File | Before | After | Delta |
|------|-------:|------:|------:|
| `PublishGateService.java` | 429 | 181 | −248 |

## Extracted collaborators (new)

| Collaborator | Role | LOC |
|--------------|------|----:|
| `PublishGateCheckItemSupport` | Individual gate check item builders (anchor / schema / rules / tests / preview / diff / approval / coverage / API policy / content-module / structured nodes / paste / blocker status) | 252 |
| `PublishGateChecklistSupport` | Checklist assembly + `SUBMIT_FOR_APPROVAL` phase filter + ready/blocker aggregation | 66 |

## Residuals

- Facade still owns evaluate / evaluateForRelease / assertReady orchestration + rule-validation request mapping (~rule helpers remain on the service)
- Further extract of rule-validation helpers would be marginal (~35 LOC) and was skipped for seam clarity
- No new tests required (existing `PublishGateServiceTest` + lifecycle publish-gate tests cover public API)

## Verify

- Targeted: `PublishGateServiceTest` + `TemplateLifecyclePublishGateTest` + `ExceptionInterventionTest` + `ModuleBoundaryArchTest` — GREEN (40 tests)
- Full: `mvn -B -ntp -f backend/pom.xml verify` — **BUILD SUCCESS** (Tests run: 1347, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
