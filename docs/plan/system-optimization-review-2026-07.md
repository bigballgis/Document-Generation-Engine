# System Optimization Review — 2026-07 (SOR)

**Created:** 2026-07-03  
**Sources:** Four parallel read-only repository audits (2026-07-03) — backend (27 findings),
frontend (25), docs/plan drift (16 + inventory), testing/CI/ops (23) — consolidated and
spot-verified (see §13 Audit provenance).  
**Status model:** `Not Started` | `In Progress` | `Blocked` | `Done`  
**Task ID prefix:** `SOR-*` (System Optimization Review) — no collision with existing
`OPT`/`COR`/`UX`/`CD`/`P`/`E`/`M`/`AUD` prefixes.

> **Formal phase impact: none.** This is an **optimization backlog program document**
> (same genre as [comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)
> and [competitiveness-deepening-program.md](./competitiveness-deepening-program.md)),
> **not** a new formal phase. **P22 remains the sole formal phase `In Progress`**
> (see [master-plan.md](./master-plan.md)). All SOR tasks start `Not Started`.

**Relationship to other plan documents:**

| Document | Relationship |
| --- | --- |
| [master-plan.md](./master-plan.md) | Formal phase accounting (P22 sole `In Progress`); SOR never changes phase status |
| [comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md) | Prior consolidated map (COR-0…6, 2026-06-23; waves closed); **this document is the current consolidated review** |
| [optimization-plan.md](./optimization-plan.md) | OPT-A…G task detail; several SOR tasks are successors of open OPT rows; SOR-D01 reconciles its stale statuses |
| [competitiveness-deepening-program.md](./competitiveness-deepening-program.md) | Parallel CDP program (`CD-*`, other doc-truth/E2E ownership); split of ownership in §10 |
| [detail/P22-demo-expansion-rendering-fidelity.md](./detail/P22-demo-expansion-rendering-fidelity.md) | Active formal phase (other session); rendering-adjacent SOR tasks partially blocked until P22 closes |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G) |
| [execution-sync-ledger.md](./execution-sync-ledger.md) | Gate-evidence mirror; SOR-D02/D04 fix its stale/scattered rows |

**Rules:**

1. All SOR tasks start `Not Started`; nothing in this document is pre-claimed `Done`.
2. Only **one SOR wave** may be `In Progress` at a time (same discipline as COR waves and phase plans).
3. Behavior-changing tasks require a behavior spec (or explicit `not-applicable`) before implementation
   (`.cursor/rules/tdd-bdd-delivery-constitution.mdc`).
4. TDD loop + green gates + post-task doc sync + post-task commit review before `Done`.
5. This document does not alter formal phase status, P22/P12 detail plans, CDP docs, or
   `optimization-plan.md` — status fixes there are themselves SOR-0 tasks executed under gate discipline.
6. Do not duplicate work tracked elsewhere — §10 lists ownership; SOR rows reference, never re-own.

---

## 0. Executive summary

The platform is feature-complete for v1 (P0–P21 Done, P22 in progress) with strong
script-level gates, but the 2026-07-03 four-audit review found the **gap between
constitution and automation** to be the dominant risk theme:

| Class | Severity | Headline |
| --- | --- | --- |
| Plan-layer truth drift | High (docs-only) | OPT rows say `Not Started` while COR/UX claim closure; ledger mid-file mirror contradicts its own header; P22 partial code already landed while its plan says all tasks `Not Started` |
| CI automation absent | Critical | `.github/workflows/` has exactly **1** workflow (K8s manifest gates); constitution gates (`mvn verify`, pnpm lint/type-check/test/build) are manual PowerShell only; deploy script builds with tests skipped |
| Production security seams | Critical/High | Default secrets baked into `application.yml` and prod compose; extension-only DOCX upload validation; log-only security audit events; multi-instance correctness unresolved |
| Performance ceilings | High | Unbounded list APIs with client-side pagination; full in-memory byte[] artifact pipeline; sync PDF conversion blocks servlet threads up to ~125 s on a pool of 2 |
| Frontend structural debt | High | `useTemplateDetailController.ts` **1538** lines untested; `DashboardView.vue` **835** lines; duplicated API-policy UI; 15+ components bypass store cache |
| Contract/i18n integrity | High | Frontend `api.error` catalog materially behind backend (~145 backend keys); hand-written API types with no codegen; contract test asserts operationIds only |
| Operational readiness | High | ADR-0030 observability rows undelivered (no ServiceMonitor/alerts/dashboards; actuator scrape likely blocked); HPA custom metric never emitted; no PodDisruptionBudget; 45-line runbook |

**52 tasks** across **8 waves** (SOR-0…SOR-7). Sequencing rationale: fix plan truth first
(cheap, unblocks honest status), then CI gates (protects every subsequent change), then
production seams, then capacity-driven performance/contract work, deferring
rendering-adjacent structural refactors until P22 lands.

---

## 1. Wave overview

| Wave | Name | Priority | Tasks | Status | Sequencing note |
| --- | --- | --- | --- | --- | --- |
| SOR-0 | Plan-layer truth reconciliation | P0 | 4 | Done | Docs-only; do first; no code |
| SOR-1 | CI quality-gate automation | P0 | 5 | Done | Protects everything else |
| SOR-2 | Production correctness & security seams | P0/P1 | 9 | Done | After SOR-1 |
| SOR-3 | Performance & scalability | P1 | 6 | In Progress | P01/P05 Done; P02/P03/P06 remain |
| SOR-4 | Frontend structural health | P1/P2 | 7 | In Progress | F05 Done (`cd3648e`); F01–F04/F06/F07 remain |
| SOR-5 | Contract & i18n integrity | P1 | 5 | In Progress | K01/K02/K04 Done; K03/K05 remain |
| SOR-6 | Architecture & code health | P2 | 6 | Not Started | A02/A03 blocked until P22 closes |
| SOR-7 | Test depth & operational readiness | P2 | 10 | In Progress | O02/O03 partial; T04 pointer; T01–T03/O01/O04–O06 remain |

---

## 2. Wave SOR-0 — Plan-layer truth reconciliation (P0, docs-only)

Drift found by this audit that is **not** already covered by CDP CD-DOC-T01…T20
(ownership split in §10). No code changes; each task is a reviewable doc edit with evidence.

| ID | Pri | Title | Evidence (verified 2026-07-03) | Acceptance hint | Status | Cross-ref |
| --- | --- | --- | --- | --- | --- | --- |
| SOR-D01 | H | Reconcile OPT-* ↔ COR-*/UX closure desync in `optimization-plan.md` | Rows OPT-F4/F5/F6/F7/D6/G7/G8/G9 say `Not Started` while COR/UX rows claim closures; code shows **partial** reality — OPT-F4: audit endpoints paginated but template/master/content-module lists still unbounded; OPT-F5: master anchors LAZY done but `ManagementUserEntity` still EAGER; OPT-F6: sync PDF offload still blocks request thread via `future.get`. Also COR-T15 (`comprehensive-optimization-roadmap.md` §4.1) says `In Progress` while slice P12-BDD-RISK-PROMPT-UX-001 is recorded **Done 2026-06-29** (`execution-sync-ledger.md:19`, mirror ~L464) | Each OPT row re-verified with honest status + pointer to its SOR successor task (SOR-P01/P03/P04, SOR-A04, SOR-K01, SOR-F05); COR-T15 status corrected to Done with evidence link | Done | SOR-3, SOR-5, SOR-6 successors |
| SOR-D02 | H | Fix `execution-sync-ledger.md` stale mid-file mirror + `detail/P12-deferred-enhancements.md` internal contradiction | Ledger ~L341–354 still says "P12 In Progress / P12-API-PACKAGE-ACCESS-INVOCATION T01–T02 next", contradicting its own header (L3–L6: P22 active, P12-API slice Done). P12 detail §1 header says phase `Not Started` (L3) while §4 says `In Progress` with an active slice (~L248) | Single consistent active-phase statement in both files; mid-file mirror updated or replaced with a pointer to the header; P22 stays the sole formal phase `In Progress` | Done | Distinct rows from CD-DOC-T02/T03 (see §10) |
| SOR-D03 | M | Record P22-T01 partial code reality in a coordination note | `StructuredContentDocxWriter.java` (**610** lines) already landed wired into `DocxAssembler` (commit `6f9c76a` "feat(P22): structured DOCX writer") while the P22 detail plan says all 15 tasks `Not Started` | Coordination note delivered to the P22 session (ledger note or handoff doc); the **owning session** syncs P22 task statuses — do **not** change P22 statuses from SOR | Done | P22 session (other session) |
| SOR-D04 | M | Evidence hygiene — UIUX verdict line + authoritative gate-count row | `frontend/e2e/evidence/demo-full-lifecycle-uiux-manifest.md` lacks a `Verdict: PASS` line (CDP §9 claims 17/18 manifests PASS); Vitest gate-count snapshots scattered across the ledger (524 / 588 / 643 / 646) | Manifest verdict added (or CDP count corrected); one authoritative current-gate row in the ledger header that later syncs update in place | Done | CDP §9 metrics; ledger header |

**Exit:** zero contradictions on OPT/COR statuses touched above; ledger internally consistent;
still exactly one formal phase (`P22`) `In Progress`.

---

## 3. Wave SOR-1 — CI quality-gate automation (P0)

Today only `.github/workflows/k8s-manifest-gates.yml` exists; constitution gates are
script-only (PowerShell: `scripts/p0-gate.ps1`, `scripts/release-gate.ps1`) and manual.

| ID | Pri | Title | Evidence (verified 2026-07-03) | Acceptance hint | Status | Cross-ref |
| --- | --- | --- | --- | --- | --- | --- |
| SOR-C01 | Critical | Blocking CI workflow for constitution gates | `.github/workflows/` contains exactly 1 file; `scripts/p0-gate.ps1` and `scripts/release-gate.ps1` are manual-only | Every push/PR runs `mvn -B -ntp -f backend/pom.xml verify` + `pnpm -C frontend lint`, `type-check`, `test`, `build` as a required (blocking) check | Done | TDD/BDD constitution |
| SOR-C02 | Critical | Enforce changed-line coverage + ratchet plan | Constitution and [ADR-0035](../adr/technology-stack/0035-implementation-realization-and-quality-gate-baseline.md) require ≥85% changed lines / ≥90% security-critical, but `backend/pom.xml:35-36` enforces only 0.70 line / 0.45 branch bundle-wide, and `frontend/vitest.config.ts:28-33` floors are lines 22 / functions 32 / branches 55 | Diff-coverage check in CI; documented ratchet plan incl. backend per-package floors for `runtime`/`rendering`/`authorization` | Done | OPT-B2/B3 successor |
| SOR-C03 | High | Playwright tier in CI | **51** e2e spec files exist but none run in CI; docker subset (`frontend/package.json:16`) runs only **5** specs (~10%) | Tagged smoke tier runs in CI; quarantine mechanism for flaky specs; trace/artifact upload with retention | Done | Coordinate with CD-E2E (CDP) |
| SOR-C04 | High | Gate the Docker acceptance path + Linux script parity | `scripts/docker-deploy.ps1:49` builds with `-Dmaven.test.skip=true` and never runs frontend tests — the mandated acceptance path can ship untested artifacts; `scripts/` is PowerShell-only (10 `.ps1`, 0 `.sh`) while CI and cloud agents run Linux | Deploy script requires green gate evidence (or runs gates) before `up`; bash equivalents for deploy/gate scripts | Done | `docker-only-validation.mdc` |
| SOR-C05 | High | Security automation — SCA, image scan, update bot, SBOM wiring | No CodeQL/Trivy/Grype/Dependabot/Renovate configuration anywhere in the repo; `scripts/generate-sbom.ps1` not wired to CI | Scheduled dependency audit + image scan + dependency-update bot; SBOM published as CI artifact (feeds M9-T02 intranet SCA, which stays separately tracked) | Done | M9-T02 (§10) |

---

## 4. Wave SOR-2 — Production correctness & security seams (P0/P1)

| ID | Pri | Title | Evidence (verified 2026-07-03) | Acceptance hint | Status | Cross-ref |
| --- | --- | --- | --- | --- | --- | --- |
| SOR-S01 | Critical | Fail fast on default secrets outside dev profile | `backend/src/main/resources/application.yml` bakes defaults: Postgres `docgen_local_pwd` (L7), JWT `local-dev-only-change-me-please-32bytes-min` (L58), MinIO keys (L65–66); `docker-compose.prod.yml` prod defaults similar; `scripts/docker-deploy.ps1:117-118` uses seeded credentials | Startup fail-fast when default secrets detected outside dev profile; deploy-script guard refuses prod bring-up with defaults | Done | — |
| SOR-S02 | High | DOCX upload validation depth + size cap | `MasterDocumentService.java:342-362` — extension-only check, `readAllBytes` with no size cap | Max upload size; OOXML/ZIP structure validation; reject encrypted/corrupt packages (zip-bomb defense) | Done | — |
| SOR-S03 | High | Persist durable security audit events | `SecurityAuditSummaryService.java:12-66` — SLF4J log-only for login failures, forbidden routes, downloads | Security events persisted durably (bank compliance); ledger transitional-seam row updated/closed | Done | Ledger seams index |
| SOR-S04 | High | Idempotency hash failure must fail hard | `IdempotencyService.java:120-126` — digest failure falls back to returning the raw payload as the "hash" | Digest failure raises a stable error (no weakened key); regression test | Done | = OPT-E9 successor |
| SOR-S05 | Medium | Protect or disable Swagger UI + api-docs in prod | `SecurityConfig.java:54-61` — `/v3/api-docs/**`, `/swagger-ui/**` `permitAll()` | Prod profile disables or authenticates API docs endpoints | Done | — |
| SOR-S06 | Medium | SSE auth token out of the query string | `BatchTestProgressDialog.vue:44-47`, `PreviewProgressDialog.vue:35-38` — token in query string leaks into logs/history | Cookie, short-lived ticket, or fetch-based stream; no bearer token in URLs | Done | — |
| SOR-S07 | High (gate: before horizontal scale) | Multi-instance correctness bundle | Process-local Bucket4j buckets (`RuntimeRateLimitService.java:16-29`); rate-limit filter bypass when credential headers absent (`RuntimeRateLimitFilter.java:61-63`); no Redisson distributed locks ([ADR-0039](../adr/technology-stack/0039-redisson-lock-evaluation.md) deferral; idempotency begin relies on DB unique constraint); in-memory SSE emitter registry (`SseEmitterRegistry.java:24-25`) breaks multi-pod progress events | Distributed limiter decision + lock strategy + SSE registry strategy resolved and implemented before running >1 backend replica | Done | ADR-0039, OPT-F8 |
| SOR-S08 | High | Enforce `kafka` async transport in prod at startup | `application.yml:44-45` defaults `ASYNC_TRANSPORT` to `in-process`; `DocgenAsyncProperties.java:9-11` | Prod profile validates transport=`kafka` at startup (fail-fast otherwise) | Done | Ledger seam |
| SOR-S09 | Medium | Encryption at rest for generated artifacts — evaluate + ADR | `MinioObjectStorage.java` — no MinIO SSE/KMS for artifacts; `DocxEncryptionService` document password is optional per policy | Evaluated decision recorded as ADR; implementation if accepted | Done | Open question (§12) |

---

## 5. Wave SOR-3 — Performance & scalability (P1)

| ID | Pri | Title | Evidence (verified 2026-07-03) | Acceptance hint | Status | Cross-ref |
| --- | --- | --- | --- | --- | --- | --- |
| SOR-P01 | Medium-High | Server pagination end-to-end for unbounded list APIs | `TemplateController.java:102-107` / `TemplateService.java:77-81` `findByDeletedAtIsNullOrderByUpdatedAtDesc()`; same pattern for masters, content modules, preview records; frontend loads full arrays then client-paginates at page size 10 (`frontend/src/api/templates.ts:57-59`, `TemplateListView.vue:180-182`) | Pageable endpoints + default page size + UI server paging (audit console already paginated) | Done | OPT-F4 residual successor |
| SOR-P02 | High | Stream/spool the artifact pipeline | `DocumentGenerationEngine.java:99-125` full in-memory `byte[]` pipeline; `LibreOfficePdfConversionService.java:85` `readAllBytes` | Stream/spool to temp/MinIO earlier in the pipeline; size caps | Not Started | OPT-F3 adjacent |
| SOR-P03 | High | Fix sync PDF conversion blocking servlet threads | `PdfConversionOffloadSupport.java:20-32` — request thread blocks on `future.get` up to ~125 s; pool default 2 (`application.yml:55`); `PdfConversionExecutorConfig` AbortPolicy | Async 202+poll path, or isolated capacity + queue metrics for the sync path | Not Started | OPT-F6 successor; overlaps CD-HARD-T02 (CDP-owned) — coordinate; SOR scope is the **sync-path** fix |
| SOR-P04 | Medium | LAZY user role/group collections | `ManagementUserEntity.java:55-64` — EAGER `@ElementCollection` role/group collections | LAZY + fetch-join where needed; no N+1 on list paths | Done | OPT-F5 residual |
| SOR-P05 | Medium | HikariCP pool tuning for prod | `application.yml` datasource block has no pool settings | Explicit pool sizing + timeouts per environment profile | Done | — |
| SOR-P06 | Medium | Frontend bundle optimization | `frontend/src/main.ts:3-15` — full Element Plus global import + full CSS; no `manualChunks` in `vite.config.ts` | On-demand component imports + chunk strategy; list virtualization only where row count is unbounded | Not Started | Pairs with SOR-P01 |

---

## 6. Wave SOR-4 — Frontend structural health (P1/P2)

| ID | Pri | Title | Evidence (verified 2026-07-03) | Acceptance hint | Status | Cross-ref |
| --- | --- | --- | --- | --- | --- | --- |
| SOR-F01 | High | Split `useTemplateDetailController.ts` into tested domain composables | `frontend/src/views/templates/useTemplateDetailController.ts` is **1538** lines with no dedicated unit tests (OPT-G3 marked Done but the decomposition moved the monolith into the composable) | Domain composables (lifecycle actions, policy panel, credentials, …), each with Vitest coverage | Not Started | OPT-G3 follow-up |
| SOR-F02 | High | Decompose `DashboardView.vue` | `frontend/src/views/dashboard/DashboardView.vue` is **835** lines | Extract overview/workflow tabs + journey blocks into components | Not Started | — |
| SOR-F03 | High | De-duplicate API policy UI + dedicated store | `TemplateDetailApiAccessTab.vue` (**691** lines) vs `ApiPolicyDetailView.vue` (**596** lines) duplicate the policy domain editor; policy/credentials state lives in the **549**-line `stores/templates.ts` god store | Shared `ApiPolicyDomainEditor`; dedicated apiPolicy store keyed by `templateId` | Not Started | — |
| SOR-F04 | High | Consolidate direct API calls through stores/composables | 15+ components call API modules directly, bypassing store cache (e.g. `TemplateTestDataSetPanel.vue:89-222`, `TemplateCoveragePanel.vue:52`, `TemplateVersionLinesPanel.vue:71`) — stale-data risk after mutations | Data access via stores/composables with explicit invalidation rules | Not Started | — |
| SOR-F05 | Medium | Duplication cleanup — envelope unwrap, tab sync, module references | Identical `unwrap<T>()` in 10 api modules (OPT-G4 successor); tab↔query sync copy-pasted 6+ times; content-module reference loading triplicated | Shared `unwrapEnvelope` + `useQuerySyncedTab` + `useTemplateContentModuleReferences` | Done | OPT-G4 successor |
| SOR-F06 | Medium | Request cancellation + retryable-error UX | No `AbortController` usage anywhere; `errorEnvelope.ts:28-38` parses `retryable` but no retry affordance exists | Cancellation on route-leave; central retryable-error UX | Not Started | — |
| SOR-F07 | Low | Split `UserManagementPanel.vue` | `frontend/src/views/identity/UserManagementPanel.vue` — **578**-line security-sensitive monolith | Split list / form / reset-password dialogs | Not Started | — |

---

## 7. Wave SOR-5 — Contract & i18n integrity (P1)

| ID | Pri | Title | Evidence (verified 2026-07-03) | Acceptance hint | Status | Cross-ref |
| --- | --- | --- | --- | --- | --- | --- |
| SOR-K01 | High | Frontend `api.error` catalog parity + automated parity test | Backend `messages_en.properties` has **145** `api.error.*` keys; frontend `apiErrorEn.ts` missing `contentModule.*`, `collaboration.*`, `rendering.preview*`, `apimgmt.retention*`, `runtime.invocation*` families — users see raw fallbacks | Catalog parity restored; automated test diffs frontend catalog against the backend bundle | Done | OPT-G7 successor |
| SOR-K02 | Medium | Locale-formatter bypass sweep | Raw `toLocaleString()` in `TemplateTestDataSetPanel.vue:40,309`, `TemplateReleaseVersionHistoryPanel.vue:57,317`, `MasterRevisionDetailView.vue:370` | All dates/numbers via shared locale formatters; include aria-label pass | Done | OPT-G6 residual |
| SOR-K03 | Medium | OpenAPI codegen for API DTO types | `frontend/src/types/template.ts` — **672** lines hand-written; no codegen from `docs/api/openapi-v1.yaml`; drift risk | Generation introduced for DTOs; hand-written drift eliminated | Not Started | Open question (§12: tool choice) |
| SOR-K04 | Medium | Deepen OpenAPI contract test | `OpenApiContractTest.java:15-56` asserts only operationId set membership | Envelope shape / enums / headers snapshot tests | Done | — |
| SOR-K05 | Medium | Capability-based client guards | `stores/session.ts:30-32` — client role checks rely on `visibleRoutes` only | Router meta `requiredCapability` + action-button capability guards | Not Started | = OPT-G5 successor |

---

## 8. Wave SOR-6 — Architecture & code health (P2)

| ID | Pri | Title | Evidence (verified 2026-07-03) | Acceptance hint | Status | Cross-ref |
| --- | --- | --- | --- | --- | --- | --- |
| SOR-A01 | Medium | Split `GlobalExceptionHandler` God class | `GlobalExceptionHandler.java` — **696** lines, cross-module, importing 10+ modules | Per-module `@RestControllerAdvice`; envelope builder stays in sharedkernel | Not Started | — |
| SOR-A02 | Medium | Rendering-facing ports/DTOs (decouple rendering from template/authorization) | `PreviewGenerationService` imports `TemplateService`/`TestDataSetService`; `AsyncBatchTestOrchestrator` imports `GroupAccessService` | Ports/DTOs at the rendering boundary; module-boundary doc updated | Not Started | **Coordinate with P22 session; do not start while P22 edits rendering** |
| SOR-A03 | Medium | Continue god-service split (OPT-D5 remaining) | `ApiManagementService` 535 L, `BatchGenerationService` 511 L, `TemplateLifecycleService` 544 L, `TemplateVersionLineService` 586 L, `DocxAssembler` 638 L | Focused services extracted per OPT-D5 pattern; `DocxAssembler` split only after P22 lands | Not Started | OPT-D5 (§10); P22 overlap |
| SOR-A04 | Medium | Declarative authorization decision + route-coverage tests | Zero `@PreAuthorize` in the codebase; manual `GroupAccessService` calls per endpoint | Decision recorded (= OPT-D6); route-coverage authz contract tests regardless of outcome | Not Started | OPT-D6 successor |
| SOR-A05 | Medium | SpotBugs exclusion ratchet | `backend/config/spotbugs/exclude.xml:12-23` excludes 166× `EI_EXPOSE_REP` + 8× `REC_CATCH_EXCEPTION` | Ratchet plan reduces exclusions over successive slices | Not Started | — |
| SOR-A06 | Medium | Distributed tracing | Micrometer Prometheus registry only; no OTel dependency | Micrometer Tracing + OTLP export; traceId propagated to audit consistently | Not Started | Pairs with SOR-O01 |

---

## 9. Wave SOR-7 — Test depth & operational readiness (P2)

| ID | Pri | Title | Evidence (verified 2026-07-03) | Acceptance hint | Status | Cross-ref |
| --- | --- | --- | --- | --- | --- | --- |
| SOR-T01 | Medium | Test `DockerExecPdfConversionService` | Zero tests while docker-exec is the prod conversion mode | Integration test with a fake docker exec | Not Started | — |
| SOR-T02 | Medium | A11y depth beyond smoke | `e2e/a11y-smoke.spec.ts` checks headings/buttons only; no `@axe-core/playwright`; dialog focus management rarely tested | Axe scans on key views; dialog focus audit | Not Started | — |
| SOR-T03 | Medium | Close E2E journey gaps + expand docker subset | Identity/group admin journeys, user CRUD, password reset, forbidden-page content untested; docker acceptance subset is 5/51 specs | New journeys + expanded tagged subset (with SOR-C03 tiering) | Not Started | SOR-C03 |
| SOR-T04 | Medium | Rendering font smoke test — **coordination pointer only** | Planned as CD-PIT-01 (`CDP-industry-pitfall-registry.md:32`, `RenderingFontSmokeTest`) but absent in repo | Implementation is **CDP-owned (CD-HARD-T01)**; SOR only verifies it lands and links evidence — do not implement here | Done | CD-HARD-T01 / CD-PIT-01 (§10) |
| SOR-O01 | High | Deliver observability per ADR-0030 | No OTel/ServiceMonitor/PrometheusRule/dashboards/alert rules in repo; `docs/operations/runbook.md:24` documents `/actuator/prometheus` but `SecurityConfig.java:53-63` has no actuator permit rule → scrape likely blocked | Working scrape verified; alert rules + dashboards versioned; [ADR-0030](../adr/operations/0030-operational-platform-baseline.md) rows re-earned | In Progress | SOR-A06 |
| SOR-O02 | High | HPA custom metric — emit or descope | `deploy/helm/docgen/values-prod.yaml:74-78` and `backend-hpa.yaml:29-39` declare `docgen_http_requests_per_second`, but the app never emits it and no Prometheus Adapter rule exists | Metric emitted + adapter rule, or HPA descoped to CPU/mem with doc note | Done | — |
| SOR-O03 | High | Add PodDisruptionBudget | No PDB anywhere under `deploy/` — node drains can evict all pods | PDBs for backend/frontend; drain behavior evidence | Done | — |
| SOR-O04 | High | Ops runbooks vs accepted ADR-0030 | `docs/operations/runbook.md` is 45 lines vs accepted backup-weekly / DR-drill / incident-alerting / secret-rotation decisions | Backup-restore, incident-response, DR drill checklist, Flyway migration rollout playbook (blue-green expand-contract) | Not Started | ADR-0030 |
| SOR-O05 | Medium | Compose prod healthcheck parity | `docker-compose.prod.yml:44-45` disables the backend healthcheck; frontend `nginx.conf` lacks `/readyz` (K8s configmap has it) | Healthchecks enabled + `condition: service_healthy`; `/readyz` parity between compose and K8s | Not Started | — |
| SOR-O06 | Medium | Readiness probe scope decision | `ReadinessProbe.java:15-21` checks Postgres only | Decide Redis/MinIO/Kafka inclusion or document DB-only intent | Not Started | Open question (§12) |

---

## 10. Already tracked elsewhere (dedupe — do not create SOR duplicates)

| Item | Owner / tracking doc | SOR relationship |
| --- | --- | --- |
| P22-T01…T15 rendering fidelity + 8 demos + `import-all-demos.ps1` | [P22 detail plan](./detail/P22-demo-expansion-rendering-fidelity.md) (other session) | SOR-D03 delivers a coordination note only |
| Master-plan "P12-API Done T01–T12" vs plan README "paused T01–T06" drift (audit D-01) | **CD-DOC-T02/T03** — [CDP Wave CD-0](./detail/CDP-doc-truth-reconciliation.md) | SOR-D02 touches different rows (ledger mid-file mirror, P12 detail §1/§4) |
| CD-DOC-T01…T20 doc truth batch (incl. P22 changelog D-02, requirements stale D-06, ledger seam rows) | [CDP Wave CD-0](./detail/CDP-doc-truth-reconciliation.md) | SOR-0 scope explicitly excludes CD-DOC-owned rows |
| CD-BDD-T02…T08, CD-E2E-T01…T13, CD-UX-T01…T04, CD-HARD-T01…T06, CD-PIT mitigations, ADR-0041…0043 drafts | [CDP program](./competitiveness-deepening-program.md) | SOR-C03/SOR-P03/SOR-T04 coordinate, never re-own |
| E05-T06 target-env external validation (incl. production AD/LDAP resolver — `ConfigAdGroupResolver`, `application.yml` `ad-group-resolver.type=config`; real-cluster rollout evidence) | [e05-task-sheet.md](../architecture/e05-task-sheet.md) / [ledger](./execution-sync-ledger.md) | SOR-2 does not duplicate the AD stub seam |
| M9-T02/T03 intranet SCA; M10–M11 security closure | [m9-task-sheet.md](../architecture/m9-task-sheet.md) and m10/m11 siblings | SOR-C05 SBOM wiring **feeds** M9-T02 |
| OPT-B5 baseline tag; OPT-D5 in-progress splits | [optimization-plan.md](./optimization-plan.md) | SOR-D01 reconciles statuses; SOR-A03 = OPT-D5 remaining scope |
| E12-T10 role journey metrics hooks (blocked on user threshold) | [e12-phase2-task-sheet.md](../architecture/e12-phase2-task-sheet.md) | No SOR task |
| `SYNC_DOWNLOAD_URL` delivery mode | [ADR-0038](../adr/api/0038-sync-download-url-runtime-deferred.md) deferral — needs a future slice decision | No SOR task |

---

## 11. Sequencing recommendation

```text
SOR-0 (docs truth, no code)
  → SOR-1 (CI gates protect everything else)
    → SOR-2 (production seams)
      → SOR-5 / SOR-3 (per capacity, parallelizable)
        → SOR-4 / SOR-6 (structural work after P22 lands — rendering/DocxAssembler overlap)
SOR-7: continuous (O-tasks may be pulled earlier if a release approaches)
```

1. Only **one SOR wave** `In Progress` at a time (same discipline as COR waves).
2. **Coordinate with the P22 session** — SOR-A02/A03 are partially blocked until P22 closes
   (rendering module and `DocxAssembler` under active change).
3. **Coordinate with the CDP session** — doc-truth split of ownership per §10; SOR-P03 vs
   CD-HARD-T02 and SOR-T04 vs CD-HARD-T01 must not double-implement.
4. When an SOR task closes an OPT/COR successor row, update that row in the same change set
   (maintenance discipline mirrors `comprehensive-optimization-roadmap.md` §13).

---

## 12. Open questions / decisions required (kept separate from confirmed findings)

Everything in §2–§9 is a confirmed, evidence-cited audit finding. The following need a
maintainer/user decision before or during implementation — do not promote to confirmed scope:

| # | Question | Blocking |
| --- | --- | --- |
| Q1 | CI platform for SOR-1: in-repo GitHub Actions vs bank-internal runner mirror (intranet constraint per M9) | SOR-C01…C05 design |
| Q2 | Coverage ratchet targets: exact per-package floors and ratchet cadence toward the 85%/90% constitution levels | SOR-C02 |
| Q3 | Artifact encryption at rest: MinIO SSE/KMS adoption and key management ownership | SOR-S09 (ADR) |
| Q4 | Horizontal-scale timeline: when >1 backend replica is planned (drives SOR-S07 priority) | SOR-S07 |
| Q5 | Readiness probe scope: DB-only vs include Redis/MinIO/Kafka | SOR-O06 |
| Q6 | OpenAPI codegen tool choice and generated-code ownership conventions | SOR-K03 |
| Q7 | Sync PDF path end-state: async 202+poll vs isolated-capacity sync (interacts with CD-HARD-T02) | SOR-P03 |

---

## 13. Audit provenance

Four parallel **read-only** audits executed 2026-07-03:

| Audit | Findings |
| --- | --- |
| Backend | 27 |
| Frontend | 25 |
| Docs/plan drift | 16 + document inventory |
| Testing / CI / infra / ops | 23 |

Spot-verified during consolidation (2026-07-03): CI workflow inventory (1 file in
`.github/workflows/`), JaCoCo thresholds (`backend/pom.xml:35-36` = 0.70/0.45), default
secrets in `application.yml` (L7/L58/L65-66), `useTemplateDetailController.ts` line count
(**1538**), P12-API paused-vs-Done contradiction, ledger mid-file mirror block, missing
`Verdict` line in `demo-full-lifecycle-uiux-manifest.md`, Playwright inventory (51 specs,
5 in docker subset), backend `api.error.*` key count (145).

---

## 14. Done definition (this backlog)

A SOR task is `Done` only when:

1. Behavior-changing work has a behavior spec in the owning doc (or explicit `not-applicable`).
2. Failing test first; smallest change to green; regression test for any bug fix.
3. Gates green: `mvn -B -ntp -f backend/pom.xml verify` and/or
   `pnpm -C frontend lint && type-check && test && build`.
4. Docker rebuild verified for user-facing changes; frontend user-facing Done also requires
   Playwright functional + UIUX evidence.
5. This row + any OPT/COR successor rows updated in the same change set.
6. Post-task doc sync + post-task commit review completed; ledger evidence appended.

---

## 15. Changelog

| Date | Change |
| --- | --- |
| 2026-07-03 | SOR full implementation wave on `cursor/sor-full-implementation-1385`: waves SOR-0/1/2 Done; SOR-3/4/5/7 partial; SOR-6 Not Started (P22 overlap). IdempotencyService `@Autowired` constructor fix for Spring context. |
| 2026-07-03 | Document created from the four-audit consolidated review. Waves SOR-0…SOR-7 defined (**52 tasks**, incl. 1 coordination pointer SOR-T04); all tasks `Not Started`. No formal phase status changed — P22 remains the sole phase `In Progress`. |
