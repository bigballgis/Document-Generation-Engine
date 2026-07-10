# Optimization Plan & Task Backlog

**Created:** 2026-06-23
**Scope:** Repository-wide optimization backlog (documentation drift, quality gates,
backend architecture/security/performance, frontend quality/UX).

> **Unified execution map:** Prioritized waves and cross-cutting frontend workflow items
> live in [comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)
> (COR-0…6). Use this file for OPT-* task detail; use the roadmap for sequencing.
**Status model:** `Not Started` | `In Progress` | `Blocked` | `Done`
**Rule:** Only one optimization wave may be `In Progress` at a time; each task must
land with a behavior spec (if behavior-changing), a failing-test-first loop, green
gates, and updated owning docs.

> This plan is a **backlog of improvements over the existing implementation**, not a
> reset. It records concrete, evidence-backed gaps found by read-only analysis on
> 2026-06-23. Source evidence is cited by file path; verify before acting.

> **2026-07-03:** Statuses reconciled against COR-*/P12 evidence; open residuals re-scoped into [LRP](./launch-readiness-program.md).

---

## 0. Headline findings

> **Audit date:** 2026-06-23. **Reconciliation (2026-06-29):** findings F1–F5 largely closed
> via OPT-A/B/C and COR waves; F6 partially closed (commits exist; OPT-B5 baseline tag pending).
> Use [execution-sync-ledger.md](./execution-sync-ledger.md) for authoritative gate evidence.

| # | Finding | Severity | Resolution (2026-06-29) |
| --- | --- | --- | --- |
| F1 | **Documentation drift** (plan vs code) | High | **Closed** — plan layer rewritten (OPT-A1–A3, COR-D01–D09); periodic sync still required |
| F2 | **Quality gates not enforced** | High | **Closed** — OPT-B1–B4: Checkstyle/PMD/SpotBugs/JaCoCo + frontend coverage/a11y in verify |
| F3 | **Stack drift vs ADR** | High | **Mostly closed** — QueryDSL, MapStruct, Bucket4j, Resilience4j landed; Redisson deferred (ADR-0039) |
| F4 | **Thin test coverage** | High | **Mostly closed** — COR-C/E + P14/P18 E2E; backend **524**, frontend **250** Vitest, 13 Playwright specs |
| F5 | **Rendering→runtime reverse dependency** | Medium | **Closed** — OPT-D1 (2026-06-24) |
| F6 | **Git baseline** | Medium | **Closed** — main carries full commit history; baseline tag (B5) optional/dropped |

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
| B5 | M | First commit + baseline tag | branch `main` has no commits (F6) | Initial commit of current tree + tag (e.g. `baseline-2026-06-23`) so optimization diffs are auditable | **Dropped** (2026-07-03 — main has established commit history; baseline tag no longer meaningful) |

**Gate evidence (2026-06-23, re-verified):** backend `mvn -B -ntp -f backend/pom.xml verify` → BUILD SUCCESS, 71 tests, 0 Checkstyle / PMD / SpotBugs violations, JaCoCo check passed. Frontend `pnpm lint` 0 problems; `pnpm test` (with coverage) passes ratchet floors. Ratchet debts: `EI_EXPOSE_REP`×~166 deferred to immutability pass ([spotbugs-exclusion-ratchet.md](./spotbugs-exclusion-ratchet.md)); `REC_CATCH_EXCEPTION` cleared in SOR-A05 slice 0 (2026-07-04).

### OPT-C Test coverage recovery

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| C1 | H | Test `apimgmt` module (currently 0 tests) | `ApiManagementServiceAuthorizationTest`, `ApiPolicyImpactPreviewServiceTest` | **Done** (Wave 2): authorization gate + policy impact preview blocking/default-route paths covered |
| C2 | H | Test authorization core | `GroupAccessServiceTest`, `ManagementAuthServiceTest`, `JwtAuthenticationFilterTest` | **Done** (Wave 2): RBAC/group isolation, login fail-closed/success, JWT filter context install/clear paths covered |
| C3 | H | Test runtime security/generation | `RuntimeGenerationServiceGenerateTest`, `DocumentDownloadServiceTest`, auth/idempotency/access tests | **Done** (Wave 2): sync replay/create, policy/format guards, download access/expiry covered |
| C4 | M | Test rendering PDF paths | `LibreOfficePdfConversionService`, `DockerExecPdfConversionService`, `DocumentArtifactPipeline` | Conversion success/timeout/cleanup covered | **Done** (2026-06-24; COR-E04) |
| C5 | M | Test audit query/recorder | `AuditQueryService`, `ManagementAuditRecorder` | Group-scoped filtering incl. GLOBAL_ADMIN path covered | **Done** (2026-06-24; COR-E03/E04) |
| C6 | M | Frontend: dashboard/tabs/router tests | Template detail decomposed; dashboard + tab router tests | Vitest for tasks, load error, tab query sync | **Done** (2026-06-23; COR-E05) |
| C7 | M | Expand e2e beyond a11y smoke | Playwright docker config + role journeys + P14/P18 slices | Role-journey + domain E2E green on 4173 | **Done** (2026-06-29; COR-E01/E02 + P14/P18 specs) |

### OPT-D Backend architecture & stack alignment

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| D1 | H | Fix rendering→runtime reverse dependency | `EncryptionOptionsView` moved to `sharedkernel.api`; rendering throws `EncryptionFailedException` | **Done** (Wave 2): rendering module no longer imports `runtime.api` or `runtime.service` |
| D2 | H | Unify generation path | `DocumentGenerationEngine` vs `RuntimeGenerationService` duplicate assembly | **Done** (Wave 2–3): sync path delegates to `DocumentGenerationEngine`; `RuntimeGenerationServiceGenerateTest` updated; gates green (184 tests) |
| D3 | M | Introduce MapStruct mappers | hand-written `toSummary`/`toDetail`/`toPolicyView` (e.g. `TemplateService` L341–404) | Mappers via MapStruct (ADR); services slimmed; behavior unchanged | **Done** (2026-06-25; COR-P08 apimgmt `ApiPolicyViewMapper`; opportunistic expansion ongoing) |
| D4 | M | Introduce QueryDSL for complex queries | `ManagementAuditEventRepository` JPQL, in-memory filtering | Audit/list queries type-safe + pageable via QueryDSL | **Done** (2026-06-25; COR-P07; `ManagementAuditEventRepositoryQuerydslTest` 5/5; verify 319 tests) |
| D5 | M | Split god services | `TemplateService` ~250 L (was ~651), `BatchGenerationService` 403 L, `ApiManagementService` 326 L | Responsibilities separated (validation/mapping/authz extracted); each class focused | **In Progress** — slice 1 Done: `TemplateViewMapper` (651→547 L); slice 2 Done: `TemplateStructuredAuthoringService` (547→495 L); slice 3 **Done** (2026-07-01): `TemplateBindingConfigurationService` (variable schema, bindings, rules, validation; 495→250 L); `TemplateBindingConfigurationServiceTest`; BDD `not-applicable`; backend **588** Surefire |
| D6 | L | Evaluate declarative authorization | no `@PreAuthorize`; authz all manual in services | Decision recorded; if adopted, consistent enforcement reduces missed-endpoint risk | **Done** (2026-06-24; ADR-0001 service-layer pattern + `ManagementAuthorizationContractTest`; route coverage expanded **SOR-A04**) |

### OPT-E Backend security & correctness hardening

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| E1 | H | Idempotency conflict → 409 not 500 | `IdempotencyService` now DB-authoritative + handles `DataIntegrityViolationException` | **Done** (Wave 2): same key + different request hash → `IdempotencyConflictException` → 409 `IDEMPOTENCY_KEY_CONFLICT` (contract-aligned) + messageKey; concurrent-insert race re-reads winner; 5 regression tests in `IdempotencyServiceConflictTest`; full gates green (114 tests) |
| E2 | H | Runtime auth error envelope compliance | `ApiCredentialAuthenticationFilter` now writes full `ErrorEnvelope` | **Done** (Wave 2): metadata + message + category + retryable; ACCESS_DENIED → 403 AUTHORIZATION; slice test updated |
| E3 | H | Audit fail-open JSON parsing | filter `readGroups` + runtime/batch/contract `readStringList` | **Done** (Wave 2): malformed policy/output JSON now fail-closed (deny/validation error) |
| E4 | M | Close `listCallableVersions` access check gap | `listCallableVersionsResult` + `TemplateCallabilitySupport` | **Done** (Wave 2): controller passes session; `RuntimeGenerationServiceAccessTest` proves cross-credential listing is denied |
| E5 | M | Bean-validation messages via messageKey | `GlobalExceptionHandler` maps constraint codes → message keys | **Done** (Wave 2): field errors use `field`/`reason`/resolved `message`; `GlobalExceptionHandlerTest` |
| E6 | M | Remove hardcoded user-facing strings | `ContractAssemblyService`, `TemplateLifecycleService` publish audit | **Done** (Wave 2, scoped): contract AD-group summaries + publish lifecycle reason via `messages_en.properties` |
| E7 | M | Handle uncovered exceptions in `GlobalExceptionHandler` | `ObjectStorageException`, `DocxAssemblyException`, `IllegalStateException` | **Done** (Wave 2): mapped to envelope with stable message keys |
| E8 | M | Fix download Content-Type | `RuntimeDocumentController` L35 hardcoded DOCX MIME | **Done** (Wave 3): Content-Type derived from storage key via `ArtifactContentTypes`; PDF/DOCX covered in `DocumentDownloadServiceTest` |
| E9 | L | Idempotency hash failure should not fall back to raw payload | `IdempotencyService` L88–90 returns payload on digest error | Digest failure is a hard error, not weakened key | **Done** (2026-07-04; LR-B7 / SOR — `IdempotencyDigestException` hard 500 + `IDEMPOTENCY_DIGEST_FAILED` envelope + `IdempotencyServiceDigestTest`; raw-payload fallback removed) |

### OPT-F Backend performance & resilience

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| F1 | H | Add rate limiting (Bucket4j or gateway) | no rate limiting anywhere; `BatchLimitsView` is contract-only | **Done** (Wave 3): Bucket4j per `credentialId:accessAccount`; 429 + `Retry-After` + `RATE_LIMIT_EXCEEDED`; `RuntimeRateLimitFilterTest` |
| F2 | H | Add resilience around external calls | no Resilience4j; LibreOffice/MinIO/Kafka unguarded | **Done** (Wave 3): Resilience4j circuit-breaker + retry on MinIO put/get/delete and PDF conversion; `serviceUnavailable` mapping; `ResilienceFailureMapperTest` |
| F3 | H | Stream large artifacts instead of `readAllBytes` | `DocumentDownloadService` L60–62, `RuntimeGenerationService` L138–139 | **Done** (Wave 3): download + sync replay stream from object storage; lazy-load tests in `DocumentDownloadServiceTest` / `RuntimeGenerationServiceGenerateTest` |
| F4 | M | Paginate list/audit queries | `TemplateRepository.findBy...`, `ManagementAuditEventRepository.search` return `List` | Pageable endpoints; default page size; tests | **Partial** (2026-07-03; audit paginated; template list server-paged via **SOR-P01**; masters/content-modules remain → **LR-C5**) |
| F5 | M | Fix EAGER anchors fetch | `MasterDocumentEntity` L62 `@OneToMany(EAGER)` | LAZY + explicit fetch-join where needed; no N+1 on list | **Done** (2026-06-25; COR-P04 + **SOR-P04** `ManagementUserEntity`) |
| F6 | M | Offload synchronous LibreOffice from request thread | `RuntimeGenerationService`→`finalizeArtifact` runs PDF conversion inline | Conversion async/bounded pool with timeout; request not blocked unduly | **Partial** (pool offload + **SOR-P03** fail-fast queue; LibreOffice hardening residual → **LR-A1**) |
| F7 | L | Clean LibreOffice temp dirs | `LibreOfficePdfConversionService` L27–52 no `finally` cleanup | Temp dirs deleted in `finally`; no leak | **Done** (2026-06-24; COR-P03) |
| F8 | L | Evaluate Redisson distributed lock | only Lettuce KV idempotency; no lock | Decision recorded; lock added for idempotency-begin/async-task if multi-instance | **Done (evaluation)** (2026-06-24; ADR-0039 — single-instance accepted; multi-instance deferred → LR-B1/LR-B2) |

### OPT-G Frontend quality, types & UX

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| G1 | H | Add axios response interceptor (401/403 + envelope) | `api/http.ts` request-only interceptor; `en.ts` L17–18 `sessionExpired` unused | **Done** (Wave 3): 401 clears session + `sessionExpired` login redirect; 403 → forbidden with server `traceId`; `http.test.ts` |
| G2 | H | Align `ApiEnvelope.error` with OpenAPI | `types/session.ts` L25–29 lacks `category`, `retryable` | **Done** (Wave 3): contract-aligned `ApiErrorDetail`; centralized `resolveApiError*` helpers; stores use structured parsing |
| G3 | H | Split `TemplateDetailView.vue` (550 L) | single file holds lifecycle+authoring+preview+policy+contract | Decomposed into subviews/composables; tested | **Done** (2026-07-03; P12-UIUX-DEEP-REFACTOR D1–D3: `useTemplateDetailController` composable + view; `WorkspaceTabShell`; **606** Vitest) |
| G4 | M | Extract shared `unwrap`/`resolveApiError`/list patterns | duplicated in 5 api modules + 4 stores + 3 list views | Shared composables/util; duplication removed | **Partial** (2026-07-03; `unwrapEnvelope` shared — **SOR-F05**; residual → **LR-C13**) |
| G5 | M | Route client-side role checks + tests | `stores/session.ts` L35–47 only checks master/template/audit; api-policy/home rely on backend `visibleRoutes` | Symmetric client checks + router integration tests | Not Started (**SOR-K05** / **LR-C13**) |
| G6 | M | i18n-ize placeholders/aria-labels/brand + locale-aware dates | `LoginView` L76, `ManagementShell` L87, `theme/tokens.ts` L17–24, 7× `toLocaleString()` | All user-facing strings via keys; dates use i18n locale | **Partial** (2026-07-03; locale-formatter bypass **SOR-K02** + P12 brand rename; `logoSlotLabel` = proper-noun exempt per LR-C11; **locale dates + catalog residual closed by LR-C11 Done** 2026-07-11 merge `44fcf40`; **aria-label sweep remains**) |
| G7 | M | Align frontend catalog with backend messageKeys | `en.ts` `api.error` only 2 keys (L14–20) | Catalog covers backend `api.error.*` at **live N/N** (N = backend count; **159/159** Done; historical plan figure 145 retired) | **Done** (2026-07-03; **SOR-K01** parity test; residual key expansion **closed by LR-C11** 2026-07-11 merge `44fcf40` — parity Vitest **159/159**) |
| G8 | M | Implement or de-scope template creation UI | `api/templates.ts` L36 `createTemplate` unused; no create button | Create flow built, or `createTemplate` removed and docs updated | **Done** (2026-06-23; COR-F12 `TemplateCreateDialog`) |
| G9 | L | Replace role-home placeholders / hide internal route keys | `RoleHomeView.vue` L44/L60–63 placeholder + raw `routeKey` | Real role dashboards or explicit deferred-scope note; no debug leakage | **Done** (2026-06-24; RoleHomeView removed — legacy home routes redirect to /dashboard) |
| G-DX1 | H | Fix Surefire reuseForks + parameterise forkCount | `pom.xml` surefire config | `reuseForks=true`; `forkCount` property; no test regression | **Done** |
| G-DX2 | H | Add `dev-fast` Maven profile | `pom.xml` profile `dev-fast` | `mvn -Pdev-fast test` skips Checkstyle/PMD/SpotBugs/JaCoCo | **Done** |
| G-DX3 | M | Parameterise SpotBugs effort | `pom.xml` `spotbugs.effort` property | effort=Max in full verify; effort=Default in dev-fast | **Done** |
| G-DX4 | M | Add `.mvn/jvm.config` | `backend/.mvn/jvm.config` | Maven daemon JVM flags fixed; `-Xmx2g`, `TieredCompilation` | **Done** |

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
| Wave 1 | OPT-A + OPT-B (incl. B5) | **Done** — B1–B4 green; B5 baseline tag optional |
| Wave 2 | OPT-C + OPT-E + OPT-D (start) | **Done** — OPT-C/E complete; D1–D4 Done; D5–D6 open |
| Wave 3 | OPT-D (finish) + OPT-F + OPT-G | **Superseded** (2026-07-03) — F1/F2/F3, G1/G2/G3, E8, F5/F7/F8 Done; D5 slice 3 Done (2026-07-01); residuals (F4/F6 partial, G4/G5/G6/G7 residual, E9) re-scoped into **[LRP](./launch-readiness-program.md)** (LR-A1, LR-B7, LR-C5, LR-C11, LR-C13); D5 remainder + D6 stay OPT backlog (not absorbed) |

Post-Wave-3 optimization intake is tracked in [launch-readiness-program.md](./launch-readiness-program.md) (LR-*).
