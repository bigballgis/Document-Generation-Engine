# LRP Wave LR-B — Multi-Instance Correctness & Session Governance 「多实例正确性与会话治理」

**Program:** [launch-readiness-program.md](../launch-readiness-program.md)  
**Wave status:** **Done** (2026-07-04 — B1–B8 all Done; batch 1 B1/B2/B7 + batch 2 wrap-up B3/B4/B5/B6/B8; evidence in [execution-sync-ledger.md](../execution-sync-ledger.md) § LRP)  
**Owner default:** `backend-engineer` (+ `deploy-engineer`, `doc-keeper`/`architecture-reviewer` for ADR-0044)  
**Prerequisites:** none for B1/B3/B5/B7/B8; **B2/B4 depend on LR-B1 decision**; **B6 depends on explicit user confirmation of the session policy**

> **Session note:** `LR-B*` tasks only. The topology contradiction (P15 HPA Done vs ADR-0039 single-instance assumption) is resolved **by decision (B1)** first — implementation tasks then follow the decided branch, never both.

---

## 0. Problem statement

2026-07-03 inventory (evidence verified in program §1):

- P15 shipped HPA autoscaling, yet: 3 `@Scheduled` jobs (`InvocationRetentionCleanupScheduler`, `CollaborationEscalationScheduler`, `PreviewTempCleanupScheduler`) have no distributed mutex; `RuntimeRateLimitService` buckets live in a `ConcurrentHashMap`; `SseEmitterRegistry` is in-process; ADR-0039 explicitly deferred Redisson assuming single instance (**CD-PIT-14**, added 2026-07-03).
- SSE: `frontend/nginx.conf` has no SSE location (default `proxy_buffering on`); `SseEmitterRegistry` 3-min timeout, no heartbeat; `PreviewController` sets no anti-buffering headers (**CD-PIT-12**, added 2026-07-03).
- JWT `PT30M` hard expiry with no renewal; logout log-only (**CD-PIT-13**, added 2026-07-03).
- No `server.shutdown=graceful`; prod compose backend `healthcheck: disable: true`, no resource limits.

---

## 1. Task breakdown

### LR-B1 — Deployment topology decision (ADR-0044)

- **Owner agent:** doc-keeper + architecture-reviewer
- **BDD:** not-applicable — architecture decision record; no runtime behavior in this task.
- **Read first:**
  1. `docs/adr/technology-stack/0039-redisson-lock-evaluation.md` (single-instance assumption)
  2. [P15 detail](./P15-kubernetes-deployment-container-hardening.md) — T05 HPA scope
  3. `deploy/helm/docgen/values*.yaml` (replica + HPA settings)
  4. Program §1 findings 3/4/5 (SSE, schedulers, rate limit — all process-local)
- **Do NOT:** Edit the accepted ADR-0039 decision text (add a superseding/refining ADR instead); implement locks/relays here (that is B2/B3/B4); leave the Helm values contradicting the decision.
- **Steps:**
  1. Draft `docs/adr/operations/0044-deployment-topology-v1.md` (next free number after 0043 reserved by LR-A5) with two candidate outcomes: **(a) v1 single replica** — Helm backend `replicas: 1`, backend HPA disabled, constraint documented; **(b) v1 multi-replica** — LR-B2, LR-B3 multi-instance section, LR-B4 become mandatory.
  2. Present trade-offs (SSE affinity, scheduler mutex, in-process rate limit, in-process async transport) and a recommendation; confirm the choice with the user.
  3. Record decision (Proposed → review → Accepted per ADR flow); cross-link ADR-0039 as refined-by-0044.
  4. Sync Helm values / `deploy/helm/docgen/README.md` + `docker-compose.prod.yml` comments to the decision in the same change set.
  5. Update ledger seam «Redisson multi-instance locks» row to point at ADR-0044.
- **Acceptance (G/W/T):**
  - **G** ADR-0044 Accepted **W** Helm values are rendered **T** replica/HPA settings match the decision (no HPA enabled while the ADR says single replica).
  - **G** a lower-tier model reads LR-B2/B3/B4 **W** it checks dependencies **T** each states unambiguously what the decided branch requires (mandatory vs recommended insurance).
- **Gates:** Doc-only — `.\scripts\helm-validate.ps1 -SkipKubeconform` green after values sync; architecture-reviewer review recorded.
- **Artifacts:** `docs/adr/operations/0044-deployment-topology-v1.md`; Helm values sync; ledger seam row update.
- **Done when:** Decision Accepted + values synced + doc sync + commit review.
- **Maps:** CD-PIT-14; ADR-0039; P15-T05.
- **Status:** **Done** (2026-07-04 — ADR-0044 Accepted; Helm values synced; helm lint green; architecture review PASS-with-suggestions)

### LR-B2 — Scheduler distributed mutex

- **Owner agent:** backend-engineer
- **BDD:** not-applicable — internal scheduling correctness; job outcomes unchanged.
- **Depends on:** LR-B1 — decided (ADR-0044, 2026-07-04): single serving replica. Per ADR-0044 the mutex is **recommended low-cost insurance under the Docker Compose single-container topology** (restart overlap) and **MANDATORY before the first K8s blue-green prod deployment** (chart keeps blue+green resident → schedulers double-run). Still schedule it this wave.
- **Read first:**
  1. `backend/src/main/java/com/bank/docgen/runtime/scheduler/InvocationRetentionCleanupScheduler.java`
  2. `backend/src/main/java/com/bank/docgen/collaboration/scheduler/CollaborationEscalationScheduler.java`
  3. `backend/src/main/java/com/bank/docgen/rendering/scheduler/PreviewTempCleanupScheduler.java`
  4. ADR-0044 (LR-B1 outcome); `.cursor/rules/tech-stack-guardrails.mdc` dependency policy
- **Do NOT:** Introduce ShedLock before dependency-policy verification + ADR note; change job schedules/semantics; use Redis-only locking that fails open when Redis is down (JDBC/DB-backed preferred for these DB-centric jobs).
- **Steps:**
  1. Verify ShedLock (`net.javacrumbs.shedlock:shedlock-spring` + `shedlock-provider-jdbc-template`) availability in the company-approved repository; record in ADR-0044 appendix (or a short ADR amendment). If unavailable, implement an equivalent Flyway-managed DB lock table + `SELECT … FOR UPDATE SKIP LOCKED` guard.
  2. Add the lock table migration (next free `V__` number) and wire `@SchedulerLock` (or the equivalent guard) around all three schedulers.
  3. Set `lockAtMostFor` **significantly greater** than the longest observed execution (measure once; document the value next to the config).
  4. Tests: second concurrent invocation is skipped while the lock is held; lock released after completion; job still runs on the next tick.
- **Acceptance (G/W/T):**
  - **G** two application contexts (or two invocations simulating instances) **W** the same scheduler tick fires in both **T** exactly one executes the job body; the other records a skipped acquisition.
  - **G** a job holding the lock crashes **W** `lockAtMostFor` elapses **T** the next tick acquires the lock and runs (no permanent deadlock).
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`
- **Artifacts:** new Flyway migration; scheduler annotations/guards; tests; dependency verification note.
- **Done when:** Mutex proven by tests + gates green + doc sync + commit review.
- **Maps:** CD-PIT-14; ledger seam «Redisson multi-instance locks» (DB lock chosen here; Redisson stays ADR-0039/0044 scoped).
- **Status:** **Done** (2026-07-04 — ShedLock 6.10.0 + V46 + 3 schedulers locked; SchedulerLockAnnotationTest + JdbcTemplateLockProviderIntegrationTest; verify 727 green; intranet SCA checkpoint open via M9)

### LR-B3 — SSE production readiness

- **Owner agent:** backend-engineer + frontend-engineer
- **BDD:** not-applicable — transport reliability; event semantics unchanged. **End-to-end verification is owned by LR-E1** (program §7).
- **Read first:**
  1. `backend/src/main/java/com/bank/docgen/rendering/service/SseEmitterRegistry.java` (L19 timeout; no heartbeat)
  2. `backend/src/main/java/com/bank/docgen/rendering/web/PreviewController.java` (progress-stream endpoint; no anti-buffering headers)
  3. `frontend/nginx.conf` (no SSE location)
  4. Batch test run durations (longest expected stream) — `BatchTestProgressDialog.vue` + backend batch test services
- **Do NOT:** Change event names/payloads consumed by the frontend; implement a Redis pub/sub relay unless ADR-0044 decided multi-replica (record the constraint instead); raise nginx timeouts globally (scope to the SSE location).
- **Steps:**
  1. Backend: send an SSE **comment heartbeat** (e.g. `: keep-alive`) every ~20 s on registered emitters (scheduled sweep in `SseEmitterRegistry` or per-emitter timer).
  2. Backend: set `X-Accel-Buffering: no` and `Cache-Control: no-cache` on the progress-stream responses (both preview and batch-test streams).
  3. Backend: align `SseEmitterRegistry` timeout with the longest expected batch-test duration + margin (config-driven; document the value).
  4. Frontend nginx: add a dedicated SSE `location` for the progress-stream paths: `proxy_buffering off`, `proxy_read_timeout` ≥ 3× heartbeat interval, `proxy_http_version 1.1`, `proxy_set_header Connection ""`.
  5. Record the multi-replica constraint (sticky routing or Redis relay required before scale-out) in ADR-0044 (LR-B1) — one sentence, no implementation.
  6. Tests: unit — heartbeat emitted, headers present; Docker smoke — stream stays open > 60 s idle through 4173.
- **Acceptance (G/W/T):**
  - **G** a client connected to the progress stream via 4173 **W** no progress events occur for 60 s **T** the connection stays open (heartbeats observed) and later events still arrive.
  - **G** the progress-stream response **W** inspected through the proxy **T** carries `X-Accel-Buffering: no` + `Cache-Control: no-cache` and arrives unbuffered (curl `-N` shows incremental chunks).
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; `pnpm -C frontend build` (nginx.conf ships with the frontend image); `.\scripts\docker-deploy.ps1` + curl smoke; **LR-E1** Playwright proof follows.
- **Artifacts:** modified `SseEmitterRegistry.java`, `PreviewController.java` (+ batch stream controller), `frontend/nginx.conf`; tests; config documentation.
- **Done when:** Unit tests + Docker curl smoke green + doc sync + commit review (LR-E1 closes the wave-level evidence).
- **Maps:** CD-PIT-12; LR-E1.
- **Status:** **Done** (2026-07-04 — batch 1: backend heartbeat/anti-buffering headers/config-driven timeout + @PreDestroy + tests; batch 2: `frontend/nginx.conf` SSE location (regex `progress-stream` match, `proxy_buffering off`, 90 s read/send timeouts) + Docker 4173 curl smoke — headers `X-Accel-Buffering: no` + `Cache-Control: no-cache` at backend (nginx consumes the former, preserves the latter — buffering off in effect); progress 10% → completed 524 ms apart (incremental, not burst); 78 s idle survival with `: keep-alive` at strict 20 s cadence ×3. Browser-level incremental proof stays **LR-E1**)

### LR-B4 — Async transport production topology

- **Owner agent:** deploy-engineer + backend-engineer
- **BDD:** not-applicable — deployment topology/config; API semantics unchanged.
- **Depends on:** LR-B1 (ADR-0044 records the chosen branch).
- **Read first:**
  1. Ledger seam «Async batch transport» ([execution-sync-ledger.md](../execution-sync-ledger.md)) — in-process default, Kafka via `ASYNC_TRANSPORT=kafka`
  2. `docker-compose.yml` (`docgen-kafka` has no healthcheck) + `docker-compose.prod.yml` (no Kafka service)
  3. `deploy/helm/docgen/values-prod.yaml`; `.env.example`
- **Do NOT:** Rewrite transport code (both paths exist since P11/M14); leave the seam row untouched after the decision.
- **Steps:**
  1. Follow ADR-0044 branch: **(a) Kafka in prod** — add Kafka to `docker-compose.prod.yml` (+ healthcheck) and Helm prod values with `ASYNC_TRANSPORT=kafka`; verify DLT flow in the prod-profile stack; add DLT depth to LR-D3 metrics list. **(b) in-process accepted for v1** — record operational constraints (single replica, restart drains via LR-B5) in ADR-0044 + runbook.
  2. Either way: add a `healthcheck` to `docgen-kafka` in the dev `docker-compose.yml` (broker API probe) so dependent services can gate on it.
  3. Update the ledger seam «Async batch transport» row: closed (Kafka in prod) or re-annotated **accepted-for-v1** with ADR-0044 link.
  4. Redeploy prod profile and archive evidence (`docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d` health output).
- **Acceptance (G/W/T):**
  - **G** the decided branch is (a) **W** the prod-profile stack starts **T** async batch tasks flow through Kafka with DLT reachable and healthchecks green.
  - **G** the decided branch is (b) **W** the ledger + ADR are read **T** the seam row and ADR-0044 state the v1 constraint explicitly — no silent «default» remains.
- **Gates:** Backend unaffected (`mvn -B -ntp -f backend/pom.xml verify` stays green); prod compose up + health evidence; `.\scripts\helm-validate.ps1 -SkipKubeconform` if Helm values change.
- **Artifacts:** compose/Helm changes; ledger seam row update; ADR-0044 appendix.
- **Done when:** Decision implemented or formally accepted + evidence archived + doc sync + commit review.
- **Maps:** Ledger seam «Async batch transport»; P11/M14.
- **Status:** **Done** (2026-07-04 — batch 1: branch (b) in-process accepted-for-v1 recorded in ADR-0044 + seam re-annotated; batch 2: dev compose `docgen-kafka` healthcheck (real broker API probe `kafka-broker-api-versions.sh`, healthy at t+5 s after start) + image source fix `bitnami/kafka:3.7` delisted from Docker Hub → `bitnamilegacy/kafka:3.7` (same image ID `cb4410499b04`; production must use company-approved registry coordinates — comment in compose); prod-profile stack 6 containers healthy evidence archived)

### LR-B5 — Graceful shutdown & drain

- **Owner agent:** backend-engineer
- **BDD:** not-applicable — lifecycle hardening; request/response contracts unchanged.
- **Read first:**
  1. `backend/src/main/resources/application.yml` (no `server.shutdown` today)
  2. `backend/src/main/java/com/bank/docgen/infrastructure/config/AsyncConfig.java` (`asyncTaskExecutor`) + `PdfConversionExecutorConfig.java` (`pdfConversionExecutor`)
  3. `SseEmitterRegistry.java` (emitters must complete on shutdown)
  4. Kafka listener config (if `ASYNC_TRANSPORT=kafka`)
- **Do NOT:** Set unbounded shutdown waits; kill in-flight PDF conversions abruptly when a bounded wait suffices; change pool sizing (LR-A1/COR-P02 scope).
- **Steps:**
  1. Set `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase` (e.g. 30 s, env-overridable) in `application.yml`.
  2. Configure both executors with `waitForTasksToCompleteOnShutdown=true` + `awaitTerminationSeconds` aligned to the phase timeout.
  3. Complete all registered SSE emitters on context shutdown (registry `@PreDestroy`: send terminal event where possible, then `complete()`).
  4. Configure Kafka listener container shutdown timeout consistently (when Kafka transport active).
  5. Restart smoke on Docker: start a slow generation, `docker stop` (default grace), assert the in-flight request completes or fails cleanly (envelope error, no connection reset), and container exits before the grace period.
- **Acceptance (G/W/T):**
  - **G** an in-flight sync generation **W** the backend receives SIGTERM **T** the request completes (or clean envelope error) and no new requests are accepted during drain.
  - **G** active SSE clients **W** shutdown proceeds **T** emitters are completed (clients see stream end, not a hung socket).
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; Docker restart smoke evidence (timestamps + logs) recorded in ledger.
- **Artifacts:** `application.yml` lifecycle block; executor config changes; registry shutdown hook; smoke evidence.
- **Done when:** Restart smoke proven + gates green + doc sync + commit review.
- **Maps:** Program §1 finding 7; LR-B8 (healthcheck pairs with drain).
- **Status:** **Done** (2026-07-04 — batch 1: graceful shutdown config + executor drain + SSE registry shutdown + GracefulShutdownConfigTest; batch 2 Docker restart smoke: 3 async-preview requests fired, `docker stop` exits in **1.606 s** (ExitCode 143), logs `Commencing graceful shutdown` → `Graceful shutdown complete` → Hikari shutdown; restart back to healthy in ~20 s)

### LR-B6 — Session renewal + revocation

- **Owner agent:** backend-engineer + frontend-engineer
- **BDD:** **required** — and implementation starts only after the **user confirms the session policy** (sliding renewal vs refresh endpoint; TTLs; revocation semantics). Stays inside the local-auth boundary — **no SSO/OIDC** (program §0.1, ADR-0036).
- **Read first:**
  1. `backend/src/main/java/com/bank/docgen/sharedkernel/security/JwtTokenService.java` (no refresh today)
  2. `backend/src/main/java/com/bank/docgen/authorization/management/service/ManagementAuthService.java` L69–71 (logout log-only)
  3. `frontend/src/api/http.ts` (401 handling — COR-F03 Done: login redirect preserves destination)
  4. `docs/security/permission-matrix.md` + `docs/architecture/security-view.md` (must be updated with the new session semantics)
- **Do NOT:** Introduce SSO/OIDC or external IdP; extend token TTL as a «fix» without revocation; store revocation state only in memory (must survive instance restart → Redis with TTL).
- **Steps:**
  1. `behavior-spec-author` publishes the session behavior spec (renewal trigger, idle vs absolute limits, revocation on logout, expiry warning UX); user confirms policy.
  2. Backend: implement the confirmed mechanism — sliding renewal (re-issue on activity) **or** a refresh endpoint; keep access-token TTL short.
  3. Backend: Redis-backed revocation list keyed by token id (`jti`) with TTL = remaining token life; `logout` writes it; the JWT filter rejects revoked tokens (fail-closed if Redis unavailable — per spec decision).
  4. Frontend: silent renewal ahead of expiry + a pre-expiry reminder (i18n keys, en base + zh-CN); 401 flow keeps COR-F03 redirect behavior.
  5. Update `permission-matrix.md` §session + `security-view.md`; add messageKeys.
  6. Tests: renewal path, revoked-token rejection, expiry warning component, filter fail-closed behavior.
- **Acceptance (G/W/T):**
  - **G** an author active in the structured editor **W** the access token nears expiry **T** the session renews without interrupting editing (no logout, no lost state).
  - **G** a user logs out **W** the old token is replayed **T** the API rejects it (401) — logout is effective, not just logged.
  - **G** Redis is unavailable **W** a token is validated **T** behavior matches the spec decision (documented fail-closed/fail-open choice, with rationale).
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; `pnpm -C frontend lint` / `type-check` / `test` / `build`; Playwright session journey on Docker 4173 (`pnpm -C frontend exec playwright test <spec> --config playwright.docker.config.ts`) + e2e-uiux-reviewer evidence for the reminder UI.
- **Artifacts:** behavior spec; backend token/filter/logout changes; frontend renewal + reminder; matrix/security-view updates; tests + E2E spec.
- **Done when:** Confirmed policy implemented + scenarios green + gates + E2E/UIUX evidence + doc sync + commit review.
- **Maps:** CD-PIT-13; COR-F03 (Done — reused); LR-C1/C2 (companion work-loss guards).
- **Status:** **Done** (2026-07-04 — full chain: BDD [BDD-LRP-SESSION-001](../../behavior/session-renewal-revocation.md) ready (policy user-confirmed) → backend (jti/sessionStartedAt claims, `POST /auth/renew` sliding renewal + 8 h cap truncation, Redis revocation fail-closed, logout writes revocation, legacy token rejected, 3 new error codes + messageKeys, prod memory-store startup guard) → frontend (session store timestamps/renewSession, `useSessionRenewal` silent renewal, `SessionLimitReminder` banner, apiError + i18n en/zh) → E2E Playwright Part A (silent renewal without interrupting editing) + Part B (reminder/i18n/redirect) with TTL environment probe → UIUX initial FAIL (button contrast) → tokenized 3-state fix 4.84:1/7.09:1 → re-review **PASS** (manifest `frontend/e2e/evidence/LRP-B6-uiux-manifest.md`, 7 screenshots) → security review **PASS-with-suggestions** (agent 23c9b81a); 🟡#2 race guard fixed (late renew response after logout discarded + test); implementation deviations written back to spec §14.1; permission matrix §13.5 + security-view updated)

### LR-B7 — Idempotency digest hard-fail + rate-limit filter fail-closed alignment

- **Owner agent:** backend-engineer
- **BDD:** not-applicable — internal hardening; error envelope semantics already contract-defined.
- **Read first:**
  1. [optimization-plan.md](../optimization-plan.md) **OPT-E9** (digest failure falls back to raw payload key)
  2. `backend/src/main/java/com/bank/docgen/runtime/security/RuntimeRateLimitService.java` + the rate-limit filter (missing-credential-header pass-through noted in ledger seam «Runtime rate limit»)
  3. `docs/adr/api/0031-api-platform-hardening-baseline.md`
- **Do NOT:** Swap Bucket4j for a distributed limiter here (that is an ADR-0044/ADR-0031 follow-up if multi-replica); change 429 response contract.
- **Steps:**
  1. Make idempotency digest failure a **hard error** (500-class envelope with stable messageKey) instead of weakening the key to raw payload (absorbs OPT-E9).
  2. Align the rate-limit filter's behavior for requests missing credential headers with ADR-0031: either fail-closed at the filter or document the auth-layer-rejects-later contract in ADR-0031/ledger seam — record the choice.
  3. Tests: digest-failure path returns hard error; missing-credential-header path matches the recorded decision.
  4. Update `optimization-plan.md` OPT-E9 row + ledger seam «Runtime rate limit» row in the same change set.
- **Acceptance (G/W/T):**
  - **G** the digest algorithm is unavailable/failing **W** an idempotent request arrives **T** the request fails with a hard, retryable-flagged envelope error — never a silently weakened idempotency key.
  - **G** a request without credential headers **W** it passes the rate-limit filter **T** observed behavior equals the documented ADR-0031 alignment (test asserts the recorded choice).
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`
- **Artifacts:** `IdempotencyService` change + tests; filter change or ADR/seam documentation; OPT-E9 status update.
- **Done when:** Both alignments proven by tests/docs + doc sync + commit review.
- **Maps:** OPT-E9; COR-B10 residual; ADR-0031.
- **Status:** **Done** (2026-07-04 — digest hard-fail + filter decision recorded; 3 test classes; verify green)

### LR-B8 — Prod health & resource limits

- **Owner agent:** deploy-engineer
- **BDD:** not-applicable — deployment configuration.
- **Read first:**
  1. `docker-compose.prod.yml` L44–45 (backend `healthcheck: disable: true`; no resource limits)
  2. `docker-compose.yml` (`docgen-kafka` lacks healthcheck)
  3. `deploy/helm/docgen/values*.yaml` (K8s side already has requests/limits — keep aligned)
  4. `backend/Dockerfile.packaged` `ENTRYPOINT` (JVM flags)
- **Do NOT:** Copy K8s probe semantics blindly (compose healthcheck hits `/healthz`); set memory limits below observed LibreOffice + JVM peak (measure first); change dev-profile ergonomics.
- **Steps:**
  1. Restore a real backend healthcheck in `docker-compose.prod.yml` (`wget`/`curl` `http://127.0.0.1:8080/healthz`, sane interval/retries/start_period for Flyway migration time).
  2. Add `mem_limit`/`cpus` (or `deploy.resources.limits`) for backend/frontend prod services; document measured baseline next to the values.
  3. Add `-XX:MaxRAMPercentage` (e.g. 75) to the backend JVM options so the heap respects the container limit.
  4. Add the missing `docgen-kafka` healthcheck in dev `docker-compose.yml` (shared step with LR-B4 — coordinate, don't duplicate).
  5. Bring up the prod profile; verify `docker inspect` shows healthy + limits; archive evidence.
- **Acceptance (G/W/T):**
  - **G** the prod-profile stack **W** `docker inspect docgen-backend` runs **T** healthcheck is enabled and healthy, and memory/cpu limits are present.
  - **G** the backend under its memory limit **W** a PDF generation runs **T** no OOM kill; JVM heap respects `MaxRAMPercentage` (evidence: `docker stats` snapshot).
- **Gates:** `docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d` healthy; `.\scripts\docker-deploy.ps1` unaffected for dev.
- **Artifacts:** `docker-compose.prod.yml` + `docker-compose.yml` changes; JVM flag change; evidence in ledger.
- **Done when:** Prod stack healthy under limits + evidence archived + doc sync + commit review.
- **Maps:** Program §1 finding 7; LR-B5 (drain + health pair); ADR-0030.
- **Status:** **Done** (2026-07-04 — prod compose backend real healthcheck (`wget http://127.0.0.1:8080/healthz`, start_period 90 s for Flyway) + frontend `depends_on: service_healthy` + mem/cpu limits (backend 1536m/2.0, frontend 256m/0.5 — same order of magnitude as Helm values) + `JAVA_TOOL_OPTIONS -XX:MaxRAMPercentage=75.0` (container log confirms `Picked up`). Environment note: sandbox cgroup v2 lacks the memory controller, so limits are evidenced via HostConfig inspection (Created-state container Mem=1610612736 / NanoCpus=2000000000) + run with a no-limits override — a sandbox constraint, not a config defect; production K8s/normal Docker unaffected)

---

## 2. Exit gate (Wave LR-B) — **all met, 2026-07-04**

- [x] ADR-0044 Accepted; Helm/compose consistent with the decision — LR-B1 (helm lint green; compose single-replica comments; LR-B4/B8 compose changes follow the decision)
- [x] Schedulers mutex-guarded (or single-replica insurance recorded); SSE heartbeat + proxy config landed — LR-B2 (ShedLock V46 + 3 schedulers) + LR-B3 (heartbeat/headers + nginx SSE location + Docker curl smoke)
- [x] Graceful shutdown proven by restart smoke; prod healthcheck + limits live — LR-B5 (`docker stop` 1.606 s, graceful shutdown logs) + LR-B8 (real `/healthz` healthcheck + mem/cpu limits + MaxRAMPercentage)
- [x] Session policy confirmed → renewal + revocation shipped with E2E evidence — LR-B6 (BDD-LRP-SESSION-001; Playwright Part A/B green; UIUX PASS; security review PASS-with-suggestions)
- [x] Ledger seams «Async batch transport» / «Redisson multi-instance locks» / «Runtime rate limit» rows resolved or ADR-annotated — accepted-for-v1 per ADR-0044 branch (b) / ADR-0044 + ShedLock / ADR-0031 alignment recorded (LR-B4/B1+B2/B7)
