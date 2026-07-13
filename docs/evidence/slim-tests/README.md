# Slim Tests — Wave 4 evidence (Test DRY)

Branch: `feat/slim-tests`  
Date: 2026-07-12  
Scope: behavior-preserving CQ-05 fixture / assertion helper extraction; no production code changes.

## Approach

1. Extract shared Spring MockMvc arrange helpers into `TemplateManagementWebTestSupport` (sessions, sample DOCX, master/template/lifecycle bootstrap).
2. Collapse duplicated arrange triples via `createDraftTemplate` / `createConfiguredTemplate` / `createPublishedTemplate`.
3. Normalize `TemplateVersionLineControllerTest` (remove blank-line inflation + delete duplicated private helpers).
4. Extract Dashboard Vitest fixtures into `dashboardViewTestSupport.ts` (role session presets, mount helpers, template/master/work-item fixtures, journey assertions).

## LOC before / after (hotspots)

| File | Before | After | Delta |
| --- | ---: | ---: | ---: |
| `TemplatePlatformSliceTest.java` | 1434 | 1095 | -339 |
| `TemplateVersionLineControllerTest.java` | 981 | 206 | -775 |
| `TemplateManagementWebTestSupport.java` *(new)* | 0 | 329 | +329 |
| `DashboardView.test.ts` | 1146 | 556 | -590 |
| `dashboardViewTestSupport.ts` *(new)* | 0 | 342 | +342 |
| **Cluster total (3 hotspots + helpers)** | **3561** | **2528** | **-1033** |

`AuditQueryServiceTest.java` (~746) left as residual this wave (fixtures already local; lower duplication ROI vs the three above).

## What changed

### Backend template web tests

- New abstract support: retail sample DOCX, standard role sessions, upload/approve master, create/configure template, lifecycle publish path, required test-data-set helper.
- Both `TemplatePlatformSliceTest` and `TemplateVersionLineControllerTest` extend the support and delete duplicated private helpers.
- Platform-only helpers (runtime generate bodies, encryption/batch credential setup) stay in the slice test.

### Frontend dashboard tests

- New `dashboardViewTestSupport.ts` with `stubSession` presets, `stubTemplates` / `stubMasters` / `setWorkItems`, fixture builders, and `expectJourney`.
- `DashboardView.test.ts` keeps all 27 scenarios; assertions unchanged in intent.

## Gates

| Gate | Result |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | GREEN (1347 tests, 7 skipped) |
| Targeted template web tests | GREEN — 41 tests (`TemplatePlatformSliceTest` 32 + `TemplateVersionLineControllerTest` 9) |
| `pnpm -C frontend lint` | GREEN |
| `pnpm -C frontend type-check` | GREEN |
| `pnpm -C frontend test` | GREEN — 191 files / 1159 tests |
| `pnpm -C frontend build` | GREEN |

Full Maven/Vitest console dumps are intentionally **not** retained in git (noise); gate outcomes recorded here only.

## Residuals

- `AuditQueryServiceTest.java` (~746) — not DRY'd this wave.
- Platform slice still holds a large private helper tail for runtime/encryption/batch payloads (~250 LOC); further split into HappyPath/Error files optional.
- Dashboard XHR AggregateError stderr noise during Vitest remains pre-existing (tests pass).
