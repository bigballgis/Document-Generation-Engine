# Launch Readiness & Deep-Optimization Program (LRP) 「上线就绪与深度优化计划」

**Program ID:** `LRP`  
**Created:** 2026-07-03  
**Status:** **In Progress** (Wave LR-B activated 2026-07-04)  
**North star:** Lift the system from **「功能齐全」** to **「生产可靠 + 业务好用」** — absorb the industry's known production pitfalls (rendering/conversion, proxy streaming, multi-instance, session), and maximize functionality + usability **inside the confirmed v1 boundary**, before launch.

**Authoritative entry for lower-tier implementers:** Read this file first, then the wave detail doc for your task ID prefix. LRP task IDs are prefixed **`LR-*`** only.

| Detail doc | Scope |
| --- | --- |
| [detail/LRP-A-rendering-trust-hardening.md](./detail/LRP-A-rendering-trust-hardening.md) | Wave LR-A — rendering trust chain & file safety 「渲染信任链与文件安全」 |
| [detail/LRP-B-runtime-scaleout-session.md](./detail/LRP-B-runtime-scaleout-session.md) | Wave LR-B — multi-instance correctness & session governance 「多实例正确性与会话治理」 |
| [detail/LRP-C-usability-deepening.md](./detail/LRP-C-usability-deepening.md) | Wave LR-C — business usability deepening 「业务易用性深化」 |
| [detail/LRP-D-ops-observability.md](./detail/LRP-D-ops-observability.md) | Wave LR-D — ops observability & data lifecycle 「运维可观测与数据生命周期」 |
| §7 of this file | Wave LR-E — release readiness gate 「发布就绪门禁」 (no separate detail doc) |
| [competitiveness-deepening-program.md](./competitiveness-deepening-program.md) | **Sibling program CDP** — doc truth, browser E2E, pitfall registry (`CD-*`; do not execute here) |
| [detail/P22-demo-expansion-rendering-fidelity.md](./detail/P22-demo-expansion-rendering-fidelity.md) | **External session** — formal phase P22 rendering + demos (do not execute here) |

---

## Session routing (read first)

| Work stream | Where it runs | This LRP program owns |
| --- | --- | --- |
| **P22** (P22-T01…T15, rendering + demos) | **Another session** — track via [P22 detail](./detail/P22-demo-expansion-rendering-fidelity.md) | **Nothing.** Do not start `P22-*` tasks from LRP docs. |
| **CDP** (CD-DOC/CD-BDD/CD-E2E/CD-HARD, `CD-*`) | **Another session** — track via [CDP program](./competitiveness-deepening-program.md) | **Nothing.** LRP references CD-2 (browser E2E) and CD-3 (CD-HARD) as prerequisite/sibling waves; it does **not** duplicate or execute their tasks. |
| **LRP** (`LR-*` — production hardening + usability deepening) | **This program** | LR-A…LR-E per §2 wave map. |

**Formal phase note:** `master-plan.md` keeps **P22** as the sole formal phase `In Progress`. LRP is a **cross-cutting program** at the same level as CDP — **not** a new formal phase and **not** a replacement for P22 accounting. LRP implementers follow task IDs prefixed **`LR-*`** only.

**Shared-wave contract with CDP:** where an LRP task executes a CD-HARD task (LR-A2→CD-HARD-T01, LR-A6→CD-HARD-T03, LR-A7→CD-HARD-T04), status is recorded **once** in the CDP detail doc and mirrored by ledger reference — never forked.

---

## 0. Executive summary

### 0.1 What we are NOT doing 「明确不做」

- **No SSO/OIDC** — local account store + JWT remains the v1 authentication boundary (ADR-0036); LR-B6 improves session continuity within it.
- **No customer-facing generation portal** — v1 confirmed: upstream systems invoke the runtime API (PRD scope).
- **No email/IM outbound notifications** — v1 confirmed in-app only; LR-C7 is a bell + unread count on existing collaboration data.
- **No free-form Word editor** — controlled structured authoring stays the contract (ADR-0019, P18 node matrix).
- **No stack replacement** — Java 21/Spring Boot/Vue 3/Element Plus/LibreOffice/PostgreSQL per accepted ADRs; new libraries (ShedLock, bucket4j-redis, etc.) enter only via dependency policy verification + ADR.

### 0.2 What we ARE doing (five deepening axes)

| Axis | Problem today | LRP outcome |
| --- | --- | --- |
| **A. Rendering trust chain & file safety** 「渲染信任链与文件安全」 | LibreOffice CLI shares one profile under concurrency; no CJK fonts in image; upload validated by extension only; two structured node types silently unrenderable | Per-invocation profile isolation, font baseline + smoke, deep upload validation + size limits, fail-closed node closure, ADR-0041/42/43 |
| **B. Multi-instance correctness & session governance** 「多实例正确性与会话治理」 | HPA delivered (P15) but schedulers/rate-limit/SSE are process-local; ADR-0039 assumes single instance; JWT 30-min hard expiry loses authoring work; SSE breaks behind buffering proxies | Topology decision ADR-0044, scheduler mutex, SSE production readiness, graceful shutdown, session renewal + revocation |
| **C. Business usability deepening** 「业务易用性深化」 | Dirty forms lost on navigation; no draft recovery/undo; catalog fetches everything client-side; `api.error` catalog 94/145; 6+ lists dead-end on error | Dirty guard, local drafts, undo/redo, side-by-side preview, server-side pagination, search palette, notification bell, i18n closure, a11y |
| **D. Ops observability & data lifecycle** 「运维可观测与数据生命周期」 | Audit tables grow unbounded; no backup/restore runbook; no alert rules or dashboards as code; security audit log-only | Retention + archival, backup/restore drill, metrics/alerts as code, trace propagation, durable security audit |
| **E. Release readiness gate** 「发布就绪门禁」 | No single go/no-go checklist; SSE-through-proxy never proven incrementally in Docker | SSE incremental E2E + launch readiness checklist |

---

## 1. Evidence-grounded findings 「关键发现」

From the 2026-07-03 full-repo inventory + industry research. Verify evidence paths before acting; CD-PIT-11…15 are pitfall registry rows **added 2026-07-03** (see [CDP-industry-pitfall-registry.md](./detail/CDP-industry-pitfall-registry.md)).

| # | Finding 「症状」 | Evidence | Risk | LRP task |
| --- | --- | --- | --- | --- |
| 1 | LibreOffice CLI concurrency without profile isolation | `backend/src/main/java/com/bank/docgen/rendering/LibreOfficePdfConversionService.java` L63–71 `ProcessBuilder` has no `-env:UserInstallation`; conversion pool `PdfConversionExecutorConfig` (default size **2**, `DocgenRenderingProperties.conversionPoolSize`) makes concurrent invocations share one profile | Industry-frequent intermittent conversion failures / hangs under parallel load (CD-PIT-11, added 2026-07-03) | **LR-A1** |
| 2 | Container lacks CJK + metric-compatible fonts | `backend/Dockerfile.packaged` L6–9 installs only `libreoffice ttf-dejavu fontconfig`; `backend/Dockerfile` same gap | Chinese letters render tofu boxes in PDF; Calibri/Cambria substituted → line/page drift (CD-PIT-01) | **LR-A2** |
| 3 | SSE likely buffered through frontend nginx proxy | `frontend/nginx.conf` has no SSE location (nginx default `proxy_buffering on`); `SseEmitterRegistry.java` L19 3-min timeout, no heartbeat; `PreviewController` sends no `X-Accel-Buffering: no` / `Cache-Control: no-cache` | On Docker 4173 progress stream may arrive as one burst or drop mid-flight (CD-PIT-12, added 2026-07-03) | **LR-B3** / **LR-E1** |
| 4 | Multi-instance contradiction | P15 delivered HPA autoscaling, but 3 `@Scheduled` jobs (`InvocationRetentionCleanupScheduler` / `CollaborationEscalationScheduler` / `PreviewTempCleanupScheduler`) have no distributed mutex; Bucket4j is in-process (`RuntimeRateLimitService` `ConcurrentHashMap`); SSE registry in-process; ADR-0039 assumes single instance | Duplicate cleanup/escalation, per-instance rate limits, lost SSE on scale-out — topology decision never closed (CD-PIT-14, added 2026-07-03) | **LR-B1**/**B2** |
| 5 | JWT 30-min hard expiry, no renewal/revocation | `backend/src/main/resources/application.yml` `jwt.access-token-ttl: PT30M`; `JwtTokenService` has no refresh; logout is log-only (`ManagementAuthService` L69–71) | Authors lose in-flight work mid-session; logout does not invalidate tokens (CD-PIT-13, added 2026-07-03) | **LR-B6** + **LR-C1**/**C2** |
| 6 | Upload validated by extension only; no size limits | `MasterDocumentService` L342–349 checks `.docx` suffix only; no `spring.servlet.multipart.max-file-size/max-request-size` in `application.yml`; `frontend/nginx.conf` has no `client_max_body_size` | Malformed/oversized/masquerading files reach POI/LibreOffice; DoS surface | **LR-A3** |
| 7 | No graceful shutdown; prod healthcheck disabled | No `server.shutdown=graceful` in `application.yml`; `docker-compose.prod.yml` L44–45 backend `healthcheck: disable: true`; no mem/cpu limits | Restarts kill in-flight generations; orchestrators cannot see backend death | **LR-B5**/**B8** |
| 8 | Management/runtime audit tables grow unbounded | `V9__management_audit.sql` / `V17__runtime_generation_audit.sql` have no retention/archival (contrast: invocation records have `InvocationRetentionCleanupScheduler` + `V43`/`V44` pattern per ADR-0040) | Unbounded growth degrades queries and backups (CD-PIT-15, added 2026-07-03) | **LR-D1** |
| 9 | Security audit events log-only | `SecurityAuditSummaryService` — login success/failure, logout, 403 route, download all `LOGGER.*` only | Permission matrix §13.3 durable-audit promise unmet; forensic gap (ledger seam «Security forbidden-route audit») | **LR-D7** |
| 10 | Frontend usability debt cluster | No `beforeunload`/`onBeforeRouteLeave` anywhere in `frontend/src`; catalog full fetch + client paging (`stores/templates.ts` `fetchTemplates`, `useCatalogPagination.ts` `slice`); editor lacks undo/redo; preview manual refresh (`TemplatePreviewPanel.vue` L50–63); 6+ lists dead-end `el-alert` without retry (`TemplateListView.vue` L249–256, `MasterListView.vue` L187–193, `ContentModuleListView.vue` L158–169, `ApiPolicyHomeView.vue` L56–65, `UserManagementPanel.vue` L344–348, `GroupManagementPanel.vue` L172–176); `api.error` frontend catalog **94/145** keys (`frontend/src/i18n/catalogs/apiErrorEn.ts` vs `backend/src/main/resources/i18n/messages_en.properties`); 3 raw `toLocaleString` (`MasterRevisionDetailView.vue` L370, `TemplateReleaseVersionHistoryPanel.vue` L57/L317, `TemplateTestDataSetPanel.vue` L40/L309); `useActivatableTableRow` click-only; 10 api modules duplicate private `unwrap`; vite has no `manualChunks`; Vitest coverage floor lines **22%** | Work loss, slow catalogs at scale, en-fallback in zh-CN, keyboard users blocked, bundle/coverage debt | **LR-C1…C13** |
| 11 | Rendering node matrix not closed | `StructuredContentNodeType` declares `qrBarcodeRef`/`attachmentListRef` but `StructuredContentDocxWriter` has no branch (validation UNSUPPORTED_NODE); `StructuredContentDocxWriter` L225–226 `contentModuleRef` without pinned structure **silently returns** with no fidelity warning | Silent content loss in published letters (CD-PIT-07 adjacent) | **LR-A4** (depends P22-T01/T02 Done) |
| 12 | Ops runbook gaps | No backup/restore runbook (Flyway forward-only); no alert rules / dashboards as code; no distributed tracing propagation | Unrehearsed recovery; blind production; RPO/RTO of ADR-0030 unproven | **LR-D2**/**D3**/**D4** |

---

## 2. Wave map 「波次与依赖」

| Wave | Name | Type | Depends on | Parallelism notes |
| --- | --- | --- | --- | --- |
| **LR-A** | Rendering trust chain & file safety | Code + infra | — (A4/A6 depend **P22-T01/T02 Done**; A7 depends P22 demo packages T05+) | A1/A2/A3/A5 schedulable immediately, independent of P22 |
| **LR-B** | Multi-instance correctness & session governance | Code + infra + ADR | — (B2/B4 depend **LR-B1** decision; B6 depends user session-policy confirmation) | B1/B3/B5/B7/B8 schedulable immediately |
| **LR-C** | Business usability deepening | Frontend + backend | — (C6 depends LR-C5 endpoint; C7 depends P14 **Done**) | Schedulable immediately; heaviest E2E/UIUX load |
| **LR-D** | Ops observability & data lifecycle | Ops + code + docs | D1 depends **LR-B2** (mutex); D6 validates LR-A1/LR-B3 | D2/D3/D4/D5/D7 schedulable immediately |
| **LR-E** | Release readiness gate | Test + docs | E1 depends **LR-B3**; E2 depends LR-A/LR-B key tasks + **P22 Done** + **CD-2 Done** | Last wave before go/no-go |

**Rules:**

1. Within LRP, only **one wave** may be `In Progress` at a time (same discipline as CDP).
2. **Recommended first activation:** **LR-B** (core **B1/B3/B5/B8**) then **LR-A** (core **A1/A2/A3**) — production-defect classes first; both cores are independent of P22.
3. Formal phase accounting is untouched: **P22 remains the sole formal phase In Progress**; LRP wave status lives in this file + [execution-sync-ledger.md](./execution-sync-ledger.md) § LRP.
4. Tasks marked **BDD: required** may not start implementation until `behavior-spec-author` publishes a `ready` spec in `docs/behavior/`.

**Current wave:** **LR-B** (`In Progress` 2026-07-04; activated on user confirmation). Program status: **In Progress**.

---

## 3. Wave LR-A — Rendering trust chain & file safety 「渲染信任链与文件安全」

**Goal:** Every DOCX→PDF conversion is concurrency-safe and font-faithful; every upload is deeply validated; no structured node can silently vanish. Detail: [LRP-A-rendering-trust-hardening.md](./detail/LRP-A-rendering-trust-hardening.md).

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| LR-A1 | backend-engineer | LibreOffice per-invocation profile isolation + CLI hardening | — | not-applicable | CD-PIT-11; COR-P02/OPT-F6 (pool exists) | Not Started |
| LR-A2 | deploy-engineer + backend-engineer | Font baseline (CJK + metric-compatible) in images + smoke test | — | not-applicable | CD-PIT-01; CD-HARD-T01; ADR-0041 (LR-A5) | Not Started |
| LR-A3 | backend-engineer (+deploy-engineer) | Upload deep validation + size limits | — | **required** | §1 finding 6 | Not Started |
| LR-A4 | backend-engineer | Unsupported-node fail-closed closure | **P22-T01/T02 Done** | **required** | CD-PIT-07; P18/P22 | Not Started |
| LR-A5 | doc-keeper | Draft ADR-0041/0042/0043 (font baseline / pagination delta / OOXML validation) | — | not-applicable | CD-PIT registry §4 | Not Started |
| LR-A6 | backend-engineer | OOXML output validation gate (executes CD-HARD-T03) | P22-T01 Done | not-applicable | CD-PIT-03 | Not Started |
| LR-A7 | doc-keeper | Pagination delta budget + corpus (executes CD-HARD-T04) | P22 demo packages (T05+) | not-applicable | CD-PIT-02; ADR-0042 | Not Started |

---

## 4. Wave LR-B — Multi-instance correctness & session governance 「多实例正确性与会话治理」

**Goal:** Deployment topology is a recorded decision, not an accident; schedulers/SSE/rate-limit behave correctly under that topology; restarts drain gracefully; sessions renew instead of destroying work. Detail: [LRP-B-runtime-scaleout-session.md](./detail/LRP-B-runtime-scaleout-session.md).

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| LR-B1 | doc-keeper + architecture-reviewer | Deployment topology decision ADR-0044 (single vs multi replica) | — | not-applicable | CD-PIT-14; ADR-0039; P15 HPA | **Done** (2026-07-04 — ADR-0044 Accepted; Helm values synced; helm lint green; architecture review PASS-with-suggestions) |
| LR-B2 | backend-engineer | Scheduler distributed mutex (ShedLock JDBC or DB lock) | LR-B1 | not-applicable | CD-PIT-14; new dependency → policy check + ADR | **Done** (2026-07-04 — ShedLock 6.10.0 + V46 + 3 schedulers locked; SchedulerLockAnnotationTest + JdbcTemplateLockProviderIntegrationTest; verify 727 green; intranet SCA checkpoint open via M9) |
| LR-B3 | backend-engineer + frontend-engineer | SSE production readiness (heartbeat, headers, nginx SSE location) | — | not-applicable (verified by LR-E1) | CD-PIT-12 | **In Progress** (2026-07-04 — backend heartbeat/anti-buffering headers/config-driven timeout + @PreDestroy + tests done; `frontend/nginx.conf` SSE location + Docker curl smoke pending; E2E owned by LR-E1) |
| LR-B4 | deploy-engineer + backend-engineer | Async transport production topology (Kafka or accepted in-process) | LR-B1 | not-applicable | Ledger seam «Async batch transport» | **In Progress** (2026-07-04 — branch (b) recorded in ADR-0044 + seam re-annotated; dev compose kafka healthcheck + prod-profile evidence pending) |
| LR-B5 | backend-engineer | Graceful shutdown & drain (server, pools, SSE) | — | not-applicable | §1 finding 7 | **In Progress** (2026-07-04 — graceful shutdown config + executor drain + SSE registry shutdown + GracefulShutdownConfigTest done; Docker restart smoke pending) |
| LR-B6 | backend-engineer + frontend-engineer | Session renewal + revocation (no SSO/OIDC) | User session-policy confirmation | **required** | CD-PIT-13; COR-F03 | Blocked (awaiting user session-policy confirmation) |
| LR-B7 | backend-engineer | Idempotency digest hard-fail + rate-limit filter fail-closed alignment | — | not-applicable | OPT-E9; COR-B10 residual; ADR-0031 | **Done** (2026-07-04 — digest hard-fail + filter decision recorded; 3 test classes; verify green) |
| LR-B8 | deploy-engineer | Prod health & resource limits (compose healthcheck, mem/cpu, JVM) | — | not-applicable | §1 finding 7 | Not Started |

---

## 5. Wave LR-C — Business usability deepening 「业务易用性深化」

**Goal:** No lost work, no dead ends, no untranslated errors; catalogs stay fast at production scale; every user-facing change lands with functional E2E + UIUX evidence on Docker 4173. Detail: [LRP-C-usability-deepening.md](./detail/LRP-C-usability-deepening.md).

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| LR-C1 | frontend-engineer | Dirty-form guard framework `useDirtyGuard` | — | **required** | §1 finding 10 | Not Started |
| LR-C2 | frontend-engineer | Structured editor local draft recovery | — | **required** | CD-PIT-13 companion | Not Started |
| LR-C3 | frontend-engineer | Editor undo/redo (bounded history + Ctrl+Z/Y) | — | **required** | §1 finding 10 | Not Started |
| LR-C4 | frontend-engineer | Side-by-side authoring preview (final-chain artifact) | — | **required** | CD-PIT-08 boundary kept | Not Started |
| LR-C5 | backend-engineer + frontend-engineer | Catalog server-side pagination/filter (templates/masters/modules) | — | **required** | OPT-F4 residual; COR-F09 semantics kept | Not Started |
| LR-C6 | frontend-engineer (+backend-engineer) | Global search / command palette (Ctrl+K) | LR-C5 (server search) | **required** | §1 finding 10 | Not Started |
| LR-C7 | backend-engineer + frontend-engineer | In-app notification center (bell + unread; in-app only) | P14 (Done) | **required** | v1 boundary §0.1 | Not Started |
| LR-C8 | frontend-engineer | Role onboarding tour (`el-tour`, no new dependency) | — | **required** | RoleJourneyTimeline (P21) | Not Started |
| LR-C9 | frontend-engineer | Unified list states rollout (`LoadErrorPanel` + retry + role-aware empty CTA) | — | not-applicable (confirmed pattern rollout — COR-F05/F14 + roadmap §9) | COR-F05/F14 | Not Started |
| LR-C10 | frontend-engineer | Upload UX polish (progress, drag hint, inline size/type errors) | LR-A3 (limits copy) | not-applicable (pattern rollout, no new behavior contract) | LR-A3 | Not Started |
| LR-C11 | frontend-engineer | i18n closure — `api.error` 145/145 + parity test + raw dates | — | not-applicable (catalog completion per i18n constitution) | OPT-G7 + OPT-G6 residual | Not Started |
| LR-C12 | frontend-engineer | Keyboard a11y & table activation (Enter/Space, skip-link, focus audit) | — | not-applicable (a11y baseline hardening) | COR-F21 residual | Not Started |
| LR-C13 | frontend-engineer | Frontend engineering debt (shared `unwrap`, route guards, `manualChunks`, coverage ratchet) | — | not-applicable (refactor + build config, behavior unchanged) | OPT-G4/G5 | Not Started |

---

## 6. Wave LR-D — Ops observability & data lifecycle 「运维可观测与数据生命周期」

**Goal:** Data has a lifecycle, backups have a rehearsed runbook, production has alarms and dashboards as code, security events survive log rotation. Detail: [LRP-D-ops-observability.md](./detail/LRP-D-ops-observability.md).

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| LR-D1 | backend-engineer | Audit data retention & archival (management + runtime) | LR-B2 (mutex) | **required** + ADR | CD-PIT-15; ADR-0040 pattern | Not Started |
| LR-D2 | deploy-engineer + doc-keeper | Backup/restore runbook + drill (pg, MinIO, Flyway forward-only) | — | not-applicable | ADR-0030 RPO≤15min/RTO≤30min | Not Started |
| LR-D3 | backend-engineer + deploy-engineer | Metrics & alerting as code (Micrometer + Prometheus rules + Grafana JSON) | — | not-applicable | §1 finding 12 | Not Started |
| LR-D4 | backend-engineer | Trace propagation decision + minimal impl (traceId → Kafka/async/MDC) | — | not-applicable | §1 finding 12 | Not Started |
| LR-D5 | doc-keeper | NFR quantification proposals (p95, SSE, capacity — as **pending**, never confirmed) | — | not-applicable | usability-review L87–91; CD-UX-T01 | Not Started |
| LR-D6 | backend-engineer + e2e-test-engineer | Load smoke baseline (≥20 concurrent sync + SSE preview on Docker) | LR-A1, LR-B3 recommended first | not-applicable | validates LR-A1/B3 | Not Started |
| LR-D7 | backend-engineer | Durable security audit events (login/403/download → DB) | — | **required** | COR-P06 residual; ledger seam close | Not Started |

---

## 7. Wave LR-E — Release readiness gate 「发布就绪门禁」

Small wave; tasks live here (no separate detail doc). Browser journeys CD-E2E-T01…T12 belong to **CDP CD-2** — referenced as a go/no-go input, never duplicated here.

### LR-E1 — SSE-through-proxy incremental E2E

- **Owner agent:** e2e-test-engineer
- **BDD:** not-applicable — test-only evidence for LR-B3 (reliability, no behavior contract change).
- **Read first:** [LRP-B detail](./detail/LRP-B-runtime-scaleout-session.md) § LR-B3; `frontend/e2e/helpers/auth.ts`; existing preview progress specs under `frontend/e2e/`; `frontend/playwright.docker.config.ts`.
- **Do NOT:** Use API polling to fake stream assertions; weaken LR-B3 headers to make the test pass; execute `CD-E2E-*` tasks from here.
- **Steps:**
  1. Deploy Docker stack: `.\scripts\docker-deploy.ps1` (UI `http://localhost:4173`).
  2. Create `frontend/e2e/LRP-E1-sse-incremental-progress.spec.ts`.
  3. Drive a preview (and one batch test run) through the UI; capture SSE progress events with arrival timestamps.
  4. Assert events arrive **incrementally** (≥2 distinct arrival times separated by a threshold, not one terminal burst) and stream survives ≥60 s idle via heartbeat.
  5. Record evidence into `frontend/e2e/evidence/LRP-E1-sse-manifest.md`.
- **Acceptance (G/W/T):**
  - **G** Docker stack on 4173 with LR-B3 merged **W** a preview with multi-step progress runs **T** the spec proves ≥2 incremental arrival timestamps before completion.
  - **G** a batch test run exceeding 60 s **W** the stream is open through the nginx proxy **T** no premature termination; heartbeat keeps the connection alive.
- **Gates:** `pnpm -C frontend exec playwright test LRP-E1-sse-incremental-progress.spec.ts --config playwright.docker.config.ts` green.
- **Artifacts:** `frontend/e2e/LRP-E1-sse-incremental-progress.spec.ts`; `frontend/e2e/evidence/LRP-E1-sse-manifest.md`.
- **Done when:** Spec green on Docker 4173 + manifest recorded + ledger row updated (post-task-doc-sync → post-task-commit-review).
- **Depends:** LR-B3.
- **Status:** Not Started

### LR-E2 — Launch readiness checklist 「上线 go/no-go 清单」

- **Owner agent:** doc-keeper
- **BDD:** not-applicable — documentation gate, no runtime behavior.
- **Read first:** This file §8; [execution-sync-ledger.md](./execution-sync-ledger.md) seams table + § LRP; [deploy/README.md](../../deploy/README.md); [docs/operations/runbook.md](../operations/runbook.md); CDP §8/§9.
- **Do NOT:** Mark any checklist item green without linked evidence; reopen Done phases; duplicate CD-E2E task rows.
- **Steps:**
  1. Create `docs/operations/launch-readiness-checklist.md` with go/no-go items: **P22 Done**; **CD-2 Done** (browser golden paths); LR-A critical tasks (A1/A2/A3) Done; LR-B critical tasks (B1/B3/B5/B8) Done; remaining ledger seams closed **or** ADR-accepted for v1; backup/restore drill evidence (LR-D2) exists; prod compose healthchecks green (LR-B8); LR-E1 green.
  2. Each item links to its evidence (ledger row, manifest, ADR).
  3. Index the checklist from `docs/README.md` (operations section) and this program §7.
  4. Record a go/no-go verdict template (date, verdict, sign-off, open risks).
- **Acceptance (G/W/T):**
  - **G** all referenced waves have status rows **W** the checklist is generated **T** every item resolves to a live link and none claims Done without evidence.
  - **G** any critical item is not Done **W** verdict is computed **T** the template forces **no-go** with the blocking item listed.
- **Gates:** Doc-only — link check (all relative links resolve) + review by architecture-reviewer.
- **Artifacts:** `docs/operations/launch-readiness-checklist.md`; index rows in `docs/README.md`.
- **Done when:** Checklist merged + indexed + ledger row updated.
- **Depends:** LR-A (A1–A3), LR-B (B1/B3/B5/B8), LR-D2, LR-E1, P22 (external), CD-2 (external).
- **Status:** Not Started

---

## 8. Success metrics 「成功指标」

| Metric | Baseline (2026-07-03) | LRP target |
| --- | --- | --- |
| Concurrent PDF conversion failure rate (pool ≥2, parallel ≥4) | Shared-profile races possible (CD-PIT-11) | **0** failures in LR-A1 regression + LR-D6 load smoke |
| Chinese-letter PDF font smoke | No CJK fonts in image — tofu risk | `RenderingFontSmokeTest` **green** in CI (LR-A2) |
| SSE incremental arrival through 4173 proxy | Unproven; buffering likely | LR-E1 E2E **green** (≥2 incremental timestamps + heartbeat survival) |
| Session expiry during authoring | Hard 30-min loss; logout ineffective | Renewal + revocation live (LR-B6) + draft recovery (LR-C2) |
| Catalog list latency at ≥500 rows | Full fetch + client slice | Server-side pagination, **p95 < 1 s** documented evidence (LR-C5) |
| `api.error` zh-CN/en catalog coverage | **94/145** keys | **145/145** + backend parity test (LR-C11) |
| Audit table lifecycle | Unbounded growth | Retention configured + cleanup verified (LR-D1) |
| Backup/restore rehearsal | Never rehearsed | Drill evidence recorded (LR-D2) |
| Alert rules & dashboards | None as code | `deploy/observability/` committed + linked runbook (LR-D3) |
| Go/no-go discipline | Implicit | `launch-readiness-checklist.md` merged with evidence links (LR-E2) |

---

## 9. Lower-tier model delegation protocol 「低级模型委托协议」

Every implementer task across LRP MUST include these fields (detail docs follow this template):

```markdown
### <TASK-ID> — <title>
- **Owner agent:** backend-engineer | frontend-engineer | e2e-test-engineer | doc-keeper | ...
- **Read first:** <ordered file list>
- **Do NOT:** <explicit forbidden scope creep>
- **Steps:** numbered, ≤8
- **Acceptance (G/W/T):** minimum 2 scenarios
- **Gates:** exact commands
- **Artifacts:** paths to create/modify
- **Done when:** behavior + gates + doc sync + ledger row
```

**Forbidden for lower-tier models (LRP program):**

- Picking up **`P22-*`** or **`CD-*`** tasks from LRP docs (they belong to other sessions/programs).
- Implementing a task marked **BDD: required** before `behavior-spec-author` publishes a `ready` spec in `docs/behavior/`.
- Introducing a new dependency (ShedLock, bucket4j-redis, k6, tracing bridge, font packages, …) before dependency-policy verification against company-approved repositories + ADR record.
- Skipping `post-task-doc-sync` → `post-task-commit-review` after any task completion.
- Marking P18/P4/P22 rendering claims «Done» — rendering fidelity status is owned by the P22 session.
- Changing ADR accepted decisions without user confirmation.

**Escalate to parent/human when:**

- BDD spec ambiguous after 1 clarification pass.
- A required dependency is unavailable in company-approved repositories (do not substitute the stack).
- E2E blocked >2h on Docker/seed — document skip reason in the evidence manifest.

---

## 10. Traceability 「追溯」

| Source | LRP relationship |
| --- | --- |
| [usability-review.md](../product/usability-review.md) §待确认 L87–91 | L89 task-time quantification → LR-D5 proposals; L91 no-access feedback copy → LR-C9 empty/error-state rollout; L90 landing fusion stays **CD-UX-T04** (CDP) |
| [non-functional-requirements.md](../requirements/non-functional-requirements.md) quantification gaps | LR-D5 writes p95/SSE/capacity/availability/session caps as **pending proposals**, never confirmed |
| [execution-sync-ledger.md](./execution-sync-ledger.md) seams | «Async batch transport» → LR-B4; «Security forbidden-route audit» → LR-D7; «Redisson multi-instance locks» (ADR-0039) → LR-B1/B2; «Paste cleaning ↔ binding validation» + «Structured content DOCX write» adjacent → LR-A4 (closure stays with P22/CD-HARD-T05) |
| [optimization-plan.md](./optimization-plan.md) residuals | OPT-E9 → LR-B7; OPT-F4 residual → LR-C5; OPT-G4/G5 → LR-C13; OPT-G6 residual + G7 → LR-C11; OPT-D5/D6 **not absorbed** — remain OPT backlog |
| [CDP-industry-pitfall-registry.md](./detail/CDP-industry-pitfall-registry.md) | CD-PIT-01→LR-A2; 02→LR-A7; 03→LR-A6; 07→LR-A4; 08→LR-C4 copy boundary; **11→LR-A1; 12→LR-B3/LR-E1; 13→LR-B6; 14→LR-B1/B2; 15→LR-D1** (11–15 added 2026-07-03) |
| [competitiveness-deepening-program.md](./competitiveness-deepening-program.md) | CD-2 browser E2E + CD-3 CD-HARD are prerequisite/sibling waves; LR-A2/A6/A7 execute CD-HARD-T01/T03/T04 without forking status |
| [master-plan.md](./master-plan.md) P22 row | Formal phase accounting unchanged; LRP is a program, not a phase |

---

**Next action (Wave LR-B In Progress; batch 1 landed 2026-07-04 — B1/B2/B7 Done):** close the LR-B3 residual (`frontend/nginx.conf` SSE location + Docker curl smoke), LR-B4 residual (dev compose kafka healthcheck + prod-profile evidence), LR-B5 residual (Docker restart smoke); start **LR-B8**; **LR-B6** stays Blocked until the user confirms the session policy. **Do not** start `P22-*` or `CD-*` from here.
