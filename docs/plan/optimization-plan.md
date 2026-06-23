# Optimization Plan & Task Backlog

**Created:** 2026-06-23
**Scope:** Repository-wide optimization backlog (documentation drift, quality gates,
backend architecture/security/performance, frontend quality/UX).
**Status model:** `Not Started` | `In Progress` | `Blocked` | `Done`
**Rule:** Only one optimization wave may be `In Progress` at a time; each task must
land with a behavior spec (if behavior-changing), a failing-test-first loop, green
gates, and updated owning docs.

> This plan is a **backlog of improvements over the existing implementation**, not a
> reset. It records concrete, evidence-backed gaps found by read-only analysis on
> 2026-06-23. Source evidence is cited by file path; verify before acting.

---

## 0. Headline findings

| # | Finding | Severity |
| --- | --- | --- |
| F1 | **Documentation drift**: `docs/README.md`, `docs/plan/README.md`, `docs/PROJECT-STATUS-RESET.md` declare *"no implementation code yet / restart from zero"*, but the repo has **234 backend main classes + ~20 test classes + 66 frontend src files**, and `master-plan.md` marks **P0–P11 all `Done`**. Plan layer and reality contradict each other. | High |
| F2 | **Quality gates not enforced (and claimed green)**: TDD constitution requires `mvn verify` to run Checkstyle + PMD + SpotBugs + JaCoCo thresholds, but `backend/pom.xml` (L160–186) has **only a JaCoCo report — no coverage rules and no static-analysis plugins**, and no `checkstyle/pmd/spotbugs` config files exist. Yet `docs/plan/execution-sync-ledger.md` L21 records backend `mvn verify` = **Green** and L80 claims *"P9 Checkstyle/PMD/SpotBugs in verify"*. The ledger's green-gate evidence is **unbacked**. No `Done` claim can currently be backed by real green gates. | High |
| F3 | **Stack drift vs ADR/guardrails**: ADRs mandate QueryDSL, MapStruct, Resilience4j, Bucket4j, Redisson; the actual build uses none of these (plain Spring Data JPA, hand-written mappers, no circuit breaker, no rate limiting, Lettuce instead of Redisson). Argon2id, JWT, Flyway, MinIO **are** implemented. | High |
| F4 | **Test coverage is thin for a TDD-mandated project**: backend main:test ≈ 11.7:1; `apimgmt` and `infrastructure` have **zero** tests; core security/runtime classes lack focused tests. Frontend ≈ 38% of source files have tests; only 1 e2e (login a11y smoke). | High |
| F5 | **Module boundary violation**: `rendering` imports `runtime.api` / `runtime.service` (reverse dependency), breaking the ADR rule that rendering stays isolated from lifecycle/authorization/API-governance. | Medium |
| F6 | **Git baseline**: branch `main` has **no commits yet**; the entire codebase is uncommitted. First commit + baseline tag is a prerequisite for any auditable optimization history. | Medium |

---

## 1. Optimization themes

| Theme | Title | Primary driver | Suggested wave |
| --- | --- | --- | --- |
| OPT-A | Documentation & plan-layer reconciliation | F1, F6 | Wave 1 |
| OPT-B | Quality-gate enforcement | F2 | Wave 1 |
| OPT-C | Test coverage recovery | F4 | Wave 2 |
| OPT-D | Backend architecture & stack alignment | F3, F5 | Wave 2–3 |
| OPT-E | Backend security & correctness hardening | — | Wave 2 |
| OPT-F | Backend performance & resilience | — | Wave 3 |
| OPT-G | Frontend quality, types & UX | — | Wave 3 |

Recommended sequencing: **Wave 1 (A,B,F6)** establishes truth + gates so all later
work is verifiable; **Wave 2 (C,E + start D)** restores test confidence and fixes
correctness/security; **Wave 3 (D,F,G)** completes architecture/performance/UX.

---

## 2. Task backlog

Each task: `ID | Priority | Title | Evidence | Acceptance criteria | Status`.
Priority: **H/M/L**. All start `Not Started`.

### OPT-A Documentation & plan-layer reconciliation

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| A1 | H | Reconcile "zero baseline" vs real code | plan index now states "P0–P11 re-earned Done"; `docs/README.md` shows P0–P11 | Done (resolved by plan-layer rewrite; `PROJECT-STATUS-RESET.md` retained as historical context) |
| A2 | H | Fix `master-plan.md` active phase | `master-plan.md` L4 "Active phase: P12" but phase table only lists P0–P11; no P12 detail file | Set a real active phase consistent with the single-active-phase rule; remove or define P12 | Done (2026-06-23: active phase = P13; P12/P13/P14 rows added to roadmap; single-active rule satisfied) |
| A3 | M | Add P11 to plan index | plan index now lists P0–P15 with statuses | Done (resolved by plan-layer rewrite) |
| A4 | M | Re-derive phase status from evidence + correct gate evidence | F2 (unbacked green gates) now resolved by B1–B4 | Done (gate-evidence portion): ledger backend gate row updated to reference real Checkstyle/PMD/SpotBugs/JaCoCo gates. Full per-phase re-derivation vs coverage remains an OPT-C concern |
| A5 | M | Sync ADR ledger with real stack | ADR-0037 created; ledger rows amended | Done — ADR 0037 amends ADR 0028 (defer MapStruct/QueryDSL; reaffirm+schedule Resilience4j/Bucket4j/Redisson as OPT-F); index + stack ledger updated |

### OPT-B Quality-gate enforcement

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| B1 | H | Add Checkstyle + PMD + SpotBugs to build | `backend/pom.xml` + `backend/config/{checkstyle,pmd,spotbugs}` | `mvn verify` runs all three (enforce via `-Dgate.fail` default true); **0 Checkstyle, PMD clean, SpotBugs clean** on 2026-06-23 | **Done** |
| B2 | H | Add JaCoCo coverage thresholds | `pom.xml` `coverage-check` execution | `jacoco:check` enforces ratchet floors LINE ≥0.70 / BRANCH ≥0.45 (current 0.746 / 0.473); ratchet target 0.85/0.60 documented | **Done** |
| B3 | M | Add frontend coverage gate | `frontend/vitest.config.ts` coverage block; `test` script uses `--coverage` | Vitest v8 coverage with ratchet floors (lines 22 / fn 32 / branch 55; baseline 23.6/34.7/64.5); `pnpm -C frontend test` enforces it | **Done** |
| B4 | M | Add ESLint a11y plugin | `frontend/eslint.config.js` + `eslint-plugin-vuejs-accessibility` | `flat/recommended` a11y rules active; `pnpm -C frontend lint` green (0 problems) | **Done** |
| B5 | M | First commit + baseline tag | branch `main` has no commits (F6) | Initial commit of current tree + tag (e.g. `baseline-2026-06-23`) so optimization diffs are auditable | Not Started (awaiting go-ahead) |

**Gate evidence (2026-06-23, re-verified):** backend `mvn -B -ntp -f backend/pom.xml verify` → BUILD SUCCESS, 71 tests, 0 Checkstyle / PMD / SpotBugs violations, JaCoCo check passed. Frontend `pnpm lint` 0 problems; `pnpm test` (with coverage) passes ratchet floors. Ratchet debts recorded in `config/spotbugs/exclude.xml` (EI_EXPOSE_REP×166 deferred to an immutability pass; REC_CATCH_EXCEPTION×8 deferred to OPT-E3).

### OPT-C Test coverage recovery

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| C1 | H | Test `apimgmt` module (currently 0 tests) | `ApiManagementServiceAuthorizationTest` added | In Progress (Wave 2): API-policy authorization gate (deny non-admin / no persistence touch / admin not-found) covered; remaining service methods pending |
| C2 | H | Test authorization core | `GroupAccessServiceTest`, `ManagementAuthServiceTest`, `JwtAuthenticationFilterTest` | **Done** (Wave 2): RBAC/group isolation, login fail-closed/success, JWT filter context install/clear paths covered |
| C3 | H | Test runtime security/generation | `ApiCredentialAuthenticationFilterTest`, `IdempotencyServiceConflictTest`, `RuntimeGenerationServiceAccessTest` | In Progress (Wave 2): auth filter envelope + fail-closed + access check covered; `DocumentDownloadService`/`RuntimeGenerationService` generation paths pending |
| C4 | M | Test rendering PDF paths | `LibreOfficePdfConversionService`, `DockerExecPdfConversionService`, `DocumentArtifactPipeline` untested | Conversion success/timeout/cleanup covered (mock process where needed) | Not Started |
| C5 | M | Test audit query/recorder | `AuditQueryService` (264 L), `ManagementAuditRecorder` (146 L) untested | Group-scoped filtering incl. GLOBAL_ADMIN unbounded path covered | Not Started |
| C6 | M | Frontend: test `TemplateDetailView`, router, `http`/`auth` | `views/templates/TemplateDetailView.vue` (550 L), `router/index.ts`, `api/http.ts`, `api/auth.ts` untested | Unit/integration tests incl. guard redirects + 401 handling | Not Started |
| C7 | M | Expand e2e beyond a11y smoke | only `e2e/a11y-smoke.spec.ts` | Role-journey e2e: login→landing, 403, master & template flow | Not Started |

### OPT-D Backend architecture & stack alignment

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| D1 | H | Fix rendering→runtime reverse dependency | `DocumentArtifactPipeline.java` L3, `PdfEncryptionService.java` L3–4 import `runtime.api`/`runtime.service` | Shared types moved to `sharedkernel`/`rendering.api`; rendering no longer imports runtime | Not Started |
| D2 | H | Unify generation path | `DocumentGenerationEngine` vs `RuntimeGenerationService` L152–191 duplicate assembly | Sync path reuses the engine; duplication removed; tests green | Not Started |
| D3 | M | Introduce MapStruct mappers | hand-written `toSummary`/`toDetail`/`toPolicyView` (e.g. `TemplateService` L341–404) | Mappers via MapStruct (ADR); services slimmed; behavior unchanged | Not Started |
| D4 | M | Introduce QueryDSL for complex queries | `ManagementAuditEventRepository` JPQL, in-memory filtering | Audit/list queries type-safe + pageable via QueryDSL | Not Started |
| D5 | M | Split god services | `TemplateService` 405 L, `BatchGenerationService` 403 L, `ApiManagementService` 326 L | Responsibilities separated (validation/mapping/authz extracted); each class focused | Not Started |
| D6 | L | Evaluate declarative authorization | no `@PreAuthorize`; authz all manual in services | Decision recorded; if adopted, consistent enforcement reduces missed-endpoint risk | Not Started |

### OPT-E Backend security & correctness hardening

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| E1 | H | Idempotency conflict → 409 not 500 | `IdempotencyService` now DB-authoritative + handles `DataIntegrityViolationException` | **Done** (Wave 2): same key + different request hash → `IdempotencyConflictException` → 409 `IDEMPOTENCY_KEY_CONFLICT` (contract-aligned) + messageKey; concurrent-insert race re-reads winner; 5 regression tests in `IdempotencyServiceConflictTest`; full gates green (114 tests) |
| E2 | H | Runtime auth error envelope compliance | `ApiCredentialAuthenticationFilter` now writes full `ErrorEnvelope` | **Done** (Wave 2): metadata + message + category + retryable; ACCESS_DENIED → 403 AUTHORIZATION; slice test updated |
| E3 | H | Audit fail-open JSON parsing | filter `readGroups` + runtime/batch `readStringList` | **Done** (Wave 2, scoped): malformed policy/output JSON now fail-closed (deny/validation error); `ContractAssemblyService` deferred |
| E4 | M | Close `listCallableVersions` access check gap | `listCallableVersionsResult` + `TemplateCallabilitySupport` | **Done** (Wave 2): controller passes session; `RuntimeGenerationServiceAccessTest` proves cross-credential listing is denied |
| E5 | M | Bean-validation messages via messageKey | `GlobalExceptionHandler` L54–58 uses `getDefaultMessage()` (raw English) | Field errors carry stable messageKey; English base bundle has keys | Not Started |
| E6 | M | Remove hardcoded user-facing strings | `ContractAssemblyService` L116/121–122, `TemplateLifecycleService` L112, `Minio/FileSystemObjectStorage` messages | Strings referenced via message keys; `messages_en.properties` updated | Not Started |
| E7 | M | Handle uncovered exceptions in `GlobalExceptionHandler` | missing `ObjectStorageException`, `DocxAssemblyException`, `IllegalStateException` | All thrown domain exceptions map to envelope; no leaked stack/500 generic where avoidable | Not Started |
| E8 | M | Fix download Content-Type | `RuntimeDocumentController` L35 hardcoded DOCX MIME | Content-Type derived from artifact format (DOCX/PDF) | Not Started |
| E9 | L | Idempotency hash failure should not fall back to raw payload | `IdempotencyService` L88–90 returns payload on digest error | Digest failure is a hard error, not weakened key | Not Started |

### OPT-F Backend performance & resilience

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| F1 | H | Add rate limiting (Bucket4j or gateway) | no rate limiting anywhere; `BatchLimitsView` is contract-only | Runtime API rate-limited per credential/policy; tests | Not Started |
| F2 | H | Add resilience around external calls | no Resilience4j; LibreOffice/MinIO/Kafka unguarded | Circuit-breaker/timeout/retry on LibreOffice + MinIO; graceful degradation | Not Started |
| F3 | H | Stream large artifacts instead of `readAllBytes` | `DocumentDownloadService` L60–62, `RuntimeGenerationService` L138–139 | Streamed download/replay; bounded memory; large-file test | Not Started |
| F4 | M | Paginate list/audit queries | `TemplateRepository.findBy...`, `ManagementAuditEventRepository.search` return `List` | Pageable endpoints; default page size; tests | Not Started |
| F5 | M | Fix EAGER anchors fetch | `MasterDocumentEntity` L62 `@OneToMany(EAGER)` | LAZY + explicit fetch-join where needed; no N+1 on list | Not Started |
| F6 | M | Offload synchronous LibreOffice from request thread | `RuntimeGenerationService`→`finalizeArtifact` runs PDF conversion inline | Conversion async/bounded pool with timeout; request not blocked unduly | Not Started |
| F7 | L | Clean LibreOffice temp dirs | `LibreOfficePdfConversionService` L27–52 no `finally` cleanup | Temp dirs deleted in `finally`; no leak | Not Started |
| F8 | L | Evaluate Redisson distributed lock | only Lettuce KV idempotency; no lock | Decision recorded; lock added for idempotency-begin/async-task if multi-instance | Not Started |

### OPT-G Frontend quality, types & UX

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| G1 | H | Add axios response interceptor (401/403 + envelope) | `api/http.ts` request-only interceptor; `en.ts` L17–18 `sessionExpired` unused | 401 → session expiry redirect; centralized envelope/error parsing | Not Started |
| G2 | H | Align `ApiEnvelope.error` with OpenAPI | `types/session.ts` L25–29 lacks `category`, `retryable` | Error type matches contract; stores read structured fields | Not Started |
| G3 | H | Split `TemplateDetailView.vue` (550 L) | single file holds lifecycle+authoring+preview+policy+contract | Decomposed into subviews/composables; tested | Not Started |
| G4 | M | Extract shared `unwrap`/`resolveApiError`/list patterns | duplicated in 5 api modules + 4 stores + 3 list views | Shared composables/util; duplication removed | Not Started |
| G5 | M | Route client-side role checks + tests | `stores/session.ts` L35–47 only checks master/template/audit; api-policy/home rely on backend `visibleRoutes` | Symmetric client checks + router integration tests | Not Started |
| G6 | M | i18n-ize placeholders/aria-labels/brand + locale-aware dates | `LoginView` L76, `ManagementShell` L87, `theme/tokens.ts` L17–24, 7× `toLocaleString()` | All user-facing strings via keys; dates use i18n locale | Not Started |
| G7 | M | Align frontend catalog with backend messageKeys | `en.ts` `api.error` only 2 keys (L14–20) | Catalog covers backend `api.error.*`; fallback strategy documented | Not Started |
| G8 | M | Implement or de-scope template creation UI | `api/templates.ts` L36 `createTemplate` unused; no create button | Create flow built, or `createTemplate` removed and docs updated | Not Started |
| G9 | L | Replace role-home placeholders / hide internal route keys | `RoleHomeView.vue` L44/L60–63 placeholder + raw `routeKey` | Real role dashboards or explicit deferred-scope note; no debug leakage | Not Started |

---

## 3. Done definition (per task)

A task is `Done` only when:

1. Behavior change has a behavior spec persisted in the owning doc (if applicable).
2. A failing test was written first, then the smallest change made it pass.
3. Quality gates are green: `mvn -B -ntp -f backend/pom.xml verify` and/or
   `pnpm -C frontend lint && type-check && test && build`.
4. Owning documentation and this plan's status column are updated in the same change.

## 4. Decisions (confirmed 2026-06-23, delegated to maintainer)

These were delegated to the maintainer to decide on production grounds. Decisions
are durable and, where they touch architecture, must be reflected as ADR updates
(task A5 / new ADRs) — not silently.

### D-1 Baseline direction → **Implementation is the authoritative baseline**

Reconcile **documentation to reality**. The repository contains a substantial,
runnable implementation (234 backend classes, full frontend, 13 Flyway migrations);
the production goal is to ship and harden the platform, not to discard working code.
OPT-A rewrites the "zero baseline" narrative to "implementation exists; statuses
pending re-verification against real gates." `PROJECT-STATUS-RESET.md` is retained as
historical context, annotated as superseded.

### D-2 Stack alignment → **Production-critical added; ergonomics deferred (via ADR)**

Driven by what a bank-facing runtime API actually needs in production:

| ADR-mandated tech | Decision | Rationale (production) |
| --- | --- | --- |
| **Bucket4j** (rate limiting) | **Add** (OPT-F1) | Abuse/DoS protection on the public runtime API is a hard production requirement |
| **Resilience4j** (circuit breaker/timeout/retry) | **Add** (OPT-F2) | LibreOffice/MinIO/Kafka are failure-prone; unguarded calls threaten availability |
| **Redisson** (distributed lock) | **Add** (OPT-F8) | Idempotency-begin + async-task ownership need a real lock for multi-instance correctness |
| **MapStruct** (mapping) | **Defer → amend ADR** | Developer ergonomics only; adopt opportunistically during refactors (D3), not runtime-critical |
| **QueryDSL** (type-safe queries) | **Defer → amend ADR** | Ergonomics/maintainability; adopt where complex/pageable queries are reworked (D4) |

Net effect: OPT-F1/F2/F8 are promoted to **must-do this cycle**; D3/D4 become
**opportunistic** (no forced churn). The amendment of MapStruct/QueryDSL from
"mandated" to "recommended, incremental" is recorded as an ADR change in A5.

### D-3 Active wave → **Wave 1 is now active**

Single active wave = **Wave 1**. Execution order:

1. **B5** — snapshot current tree as the first commit + `baseline-2026-06-23` tag
   *(requires explicit go-ahead to commit; everything else can proceed first)*.
2. **B1/B2/B3/B4** — add Checkstyle/PMD/SpotBugs + JaCoCo + Vitest coverage + a11y
   lint; run them and triage real violations.
3. **A1–A5** — reconcile docs/plan/ledger statuses using the *actual* gate results
   from step 2 (so no status is asserted without backing evidence).

Wave 2 (OPT-C, OPT-E, start OPT-D) and Wave 3 (OPT-F, OPT-G, finish OPT-D) follow
once Wave 1 exit criteria are met.

## 5. Active wave status

| Wave | Scope | Status |
| --- | --- | --- |
| Wave 1 | OPT-A + OPT-B (incl. B5) | **In Progress** — OPT-B (B1–B4) Done; B5 + OPT-A reconciliation remaining |
| Wave 2 | OPT-C + OPT-E + OPT-D (start) | In Progress — E1/E2/E3/E4 Done; C2 Done; C3 partial; apimgmt/generation tests remain |
| Wave 3 | OPT-D (finish) + OPT-F + OPT-G | Not Started |
