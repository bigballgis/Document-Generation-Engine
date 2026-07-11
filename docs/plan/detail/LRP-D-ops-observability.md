# LRP Wave LR-D — Ops Observability & Data Lifecycle 「运维可观测与数据生命周期」

**Program:** [launch-readiness-program.md](../launch-readiness-program.md)  
**Wave status:** **In Progress** (2026-07-12 — partial: **LR-D1 Done**; **LR-D7 Done**; **LR-D6 Done**; **LR-D5 Done**; **LR-D2 In Progress** sole-active; **D3/D4 Not Started**)  
**Owner default:** `backend-engineer` + `deploy-engineer` (+ `doc-keeper` for runbook/NFR)  
**Prerequisites:** **D1 depends on LR-B2** (scheduler mutex — **Done**); D6 validates LR-A1/LR-B3 (**both Done**); D5 fed by D6 evidence (**Done** — proposals pending confirmation, not confirmed SLOs); **D2 sole-active**; leave **D3/D4 Not Started** — do **not** activate D3/D4 / LR-E / CD-3

> **Activation note (2026-07-12, LR-D2):** **LR-D2 → In Progress** (slice `lrp-d2-backup-restore`; formal phase remains **None**). Sole-active LRP slice — backup/restore runbook + timed drill vs ADR-0030 RPO≤15min/RTO≤30min (pg, MinIO, Flyway forward-only). BDD **not-applicable** (ops docs + drill; behavior-spec confirming in parallel). Placement: ISOLATED `D:/working/DGE-lrp-d2-backup-restore` · `feat/lrp-d2-backup-restore` · base `362a556` (`362a5560272d16d4986e6aa4e358feebf6d43ba0`). Gate evidence: []. **Task Master #39 → in-progress**. Wave remains **In Progress** (**D1+D5+D6+D7 Done**; **D2 In Progress** sole-active; **D3/D4 Not Started**). Do **not** activate D3/D4/LR-E/CD-3. Do **not** touch `DGE-audit-governance`. Next: **doc-keeper** + **deploy-engineer** (runbook + drill — do not implement in plan-orchestrator).

> **Completion note (2026-07-12, LR-D5):** **LR-D5 → Done** (slice `lrp-d5-nfr-proposals`; formal phase remains **None**). Docs-only NFR quantification proposals authored in [non-functional-requirements.md §待确认 LR-D5](../../requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation); every value «proposed — awaiting confirmation»; fed by LR-D6 evidence + [DEF-LRP-D6-001](../evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md); ≤3s superseded as unsupported by smoke; **not** confirmed SLOs. BDD **not-applicable**. **Merge:** `5b13476` (`5b13476b7aa2056fbbbe2ca4acc8d1dbe4659d0c`); worktree removed (stage 11). **Gates:** docs-only; architecture-reviewer **PASS_WITH_NOTES**. **Task Master #38 → done**. **No sole-active LRP slice**. Wave remains **In Progress** (**D1+D5+D6+D7 Done**; **D2–D4 Not Started**). Do **not** activate D2–D4/LR-E/CD-3. Do **not** touch `DGE-audit-governance`. Recommend next: **LR-D2** or **LR-D3** (do not activate until parent directs).

> **Activation note (2026-07-12, LR-D5):** **LR-D5 → In Progress** (now **Done** — see completion note above). Slice `lrp-d5-nfr-proposals`; formal phase remains **None**.

> **Completion note (2026-07-12, LR-D6):** **LR-D6 → Done** (slice `lrp-d6-load-smoke`; formal phase remains **None**). Flag-gated JUnit load-smoke harness; Scenario A n=20 success=12 errorRate=0.4 triaged **DEF-LRP-D6-001** (PDF concurrent → `serviceUnavailable` mapped as `TEMPLATE_VALIDATION_FAILED`; p95≈15939ms p99≈16065ms; poolRejections=0); Scenario B 5/5 SSE completed dropped=0. BDD **not-applicable**. **Merge:** `56383eb` (`56383ebd6f4dedc5413339aabe88ac16ea857d74`); worktree removed (stage 11). **Gates:** `mvn -B -ntp -f backend/pom.xml verify` **GREEN** (1303 tests, pre-merge worktree); architecture **PASS_WITH_NOTES**; **DEPLOY_OK** 2026-07-12T01:26:46+08:00. **Evidence:** [latest-summary.json](../evidence/lrp-d6-load-smoke/latest-summary.json) + [TRIAGE-pdf-422.md](../evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md). **Task Master #37 → done**. Superseded sole-active by **LR-D5** (see activation note above). Wave remains **In Progress** (**D1+D7+D6 Done**; **D2–D5 were Not Started at D6 close**). Do **not** activate D2–D4/LR-E/CD-3. Do **not** touch `DGE-audit-governance`.

> **Activation note (2026-07-12, LR-D6):** **LR-D6 → In Progress** (now **Done** — see completion note above). Slice `lrp-d6-load-smoke`; formal phase remains **None**.

> **Completion note (2026-07-11, LR-D7):** **LR-D7 → Done** (slice `lrp-d7-durable-security-audit`; formal phase remains **None**). Durable `SECURITY_*` audit events (login/403/download → `management_audit_event`); route-denied API + frontend; retention via ADR-0048/D1; closes ledger seam «Security forbidden-route audit». BDD **`ready`** (`docs/behavior/lrp-d7-durable-security-audit.md`; BDD-LRP-D7-001…010). **Merge:** `c94a356` (`c94a356070dff7a9ab35ffbc0ba53b49f63270d0`); worktree removed (stage 11). **Gates:** `mvn -B -ntp -f backend/pom.xml verify` **GREEN** (1294 tests); `pnpm` frontend lint/type-check/test/build **GREEN** (1144 tests); architecture **PASS_WITH_NOTES** (Critical **0**; merge_go=true); **DEPLOY_OK** 2026-07-11T23:50:03+08:00 healthz 200; E2E/UIUX skipped per BDD D7-C14. **Task Master #36 → done**. **No sole-active LRP slice**. Wave remains **In Progress** (**D1+D7 Done**; **D2–D6 Not Started**). Do **not** activate D2–D6/LR-E/CD-3. Do **not** touch `DGE-audit-governance`.

> **Activation note (2026-07-11, LR-D7):** **LR-D7 → In Progress** (now **Done** — see completion note above). Slice `lrp-d7-durable-security-audit`; formal phase remains **None**.

> **Completion note (2026-07-11):** **LR-D1 → Done** (slice `lrp-d1-audit-retention`; formal phase remains **None**). Audit retention cleanup for management + runtime audit tables (mirror ADR-0040; closes **CD-PIT-15**; under LR-B2 ShedLock). BDD **`ready`** (`docs/behavior/lrp-d1-audit-retention.md`). **ADR-0048 Accepted**. Flyway **V54** applied. **Merge:** `20b2a76` (`feat(audit): LR-D1 audit retention cleanup with V54 and ADR-0048`); worktree removed. **Gates:** `mvn verify` **GREEN**; architecture **PASS_WITH_NOTES** (merge_go=true); **DEPLOY_OK** 2026-07-11T22:26:31+08:00 healthz 200. **Task Master #35 → done**. Wave remains **In Progress**. Do **not** activate D2–D6/LR-E/CD-3. Do **not** touch `DGE-audit-governance`.

> **Activation note (2026-07-11):** **LR-D1 → In Progress** (now **Done** — see completion note above). Slice `lrp-d1-audit-retention`; formal phase remains **None**.

> **Session note:** `LR-D*` tasks only. Retention deletes data — D1 is **BDD: required** and carries its own ADR. NFR numbers land as **pending proposals**, never silently confirmed (document-as-code constitution).

---

## 0. Problem statement

2026-07-03 inventory (evidence verified in program §1):

- Management/runtime audit tables (`V9__management_audit.sql`, `V17__runtime_generation_audit.sql`) grow unbounded — contrast with the invocation-record pattern (`InvocationRetentionCleanupScheduler` + `V43`/`V44`, ADR-0040) (**CD-PIT-15**, added 2026-07-03).
- Security events (login success/failure, 403 route, download) were **log-only** (`SecurityAuditSummaryService`) — ledger seam «Security forbidden-route audit» **closed** by **LR-D7** (2026-07-11; merge `c94a356`; durable `SECURITY_*` on `management_audit_event`).
- No backup/restore runbook despite Flyway forward-only + ADR-0030 RPO ≤15 min / RTO ≤30 min commitments; no alert rules or dashboards as code; no trace propagation into async/Kafka paths.

---

## 1. Task breakdown

### LR-D1 — Audit data retention & archival

- **Owner agent:** backend-engineer
- **BDD:** **required** — retention deletes/archives user-visible audit data; behavior spec + user confirmation of retention baselines. Pair with an **ADR** (retention periods; mirror ADR-0040's shape for invocation records).
- **Depends on:** LR-B2 (cleanup job must run under the distributed mutex).
- **Read first:**
  1. `backend/src/main/java/com/bank/docgen/runtime/scheduler/InvocationRetentionCleanupScheduler.java` + `V43__api_policy_invocation_retention.sql` / `V44__api_invocation_record.sql` (the pattern to mirror)
  2. `V9__management_audit.sql`, `V17__runtime_generation_audit.sql` (targets)
  3. `docs/adr/api-management/0040-api-package-access-and-invocation-retention.md`
  4. `docs/security/permission-matrix.md` audit sections (who may see/purge what)
- **Do NOT:** Hard-delete without the spec-confirmed baseline; ship retention defaults nobody confirmed (propose, confirm, then configure); bypass the LR-B2 mutex.
- **Steps:**
  1. `behavior-spec-author` publishes the retention spec (per-table retention windows, archival vs delete, who can see purge evidence); user confirms baselines.
  2. ADR draft (next free number after 0044): retention baseline for management + runtime audit tables; Proposed → review.
  3. Flyway migration for retention config (mirroring the V43 pattern) + scheduled cleanup service/scheduler guarded by the LR-B2 lock.
  4. Purge/archival action itself writes an audit trail row (what was purged, window, count).
  5. Tests: rows older than the window purged, newer retained; purge audit row written; scheduler skips without lock.
- **Acceptance (G/W/T):**
  - **G** audit rows older than the confirmed window **W** the cleanup job runs **T** they are purged/archived per the ADR and a purge-evidence row records count + window.
  - **G** rows inside the window **W** the same run executes **T** they remain untouched (boundary test at exactly the window edge).
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`
- **Artifacts:** behavior spec; ADR; migration + scheduler/service + tests.
- **Done when:** Confirmed baseline implemented + scenarios green + ADR recorded + doc sync + commit review.
- **Maps:** CD-PIT-15; ADR-0040 pattern; LR-B2.
- **Status:** **Done** (2026-07-11 — slice `lrp-d1-audit-retention`; merge `20b2a76`; Task Master #35; BDD ready; ADR-0048 Accepted; V54; mvn verify GREEN; architecture PASS_WITH_NOTES; DEPLOY_OK 2026-07-11T22:26:31+08:00)

### LR-D2 — Backup/restore runbook + drill

- **Owner agent:** deploy-engineer + doc-keeper
- **BDD:** not-applicable — operational documentation + rehearsal evidence. Readiness note: [lrp-d2-backup-restore.md](../../behavior/lrp-d2-backup-restore.md) (`bdd_readiness: not-applicable`, 2026-07-12).
- **Read first:**
  1. `docs/adr/operations/0030-operational-platform-baseline.md` (RPO ≤15 min / RTO ≤30 min commitments)
  2. `docs/operations/runbook.md`; `deploy/README.md`; `deploy/blue-green-runbook.md`
  3. `docker-compose.yml` volumes (`docgen-postgres-data`, MinIO data)
- **Do NOT:** Claim RPO/RTO compliance without a timed drill; script destructive restore steps without an explicit confirmation gate; invent cloud services outside the stack.
- **Steps:**
  1. Write `docs/operations/backup-restore-runbook.md`: pg dump/restore (and WAL/scheduled-snapshot guidance for prod), MinIO bucket backup strategy, Redis (cache — document as rebuildable), secrets handling.
  2. Document the **Flyway forward-only rollback playbook**: roll forward with a compensating migration; blue-green color revert for app-level rollback (cross-link `deploy/blue-green-runbook.md`).
  3. Define the drill procedure: restore into a scratch stack, verify healthz + one generated document round-trip.
  4. **Execute the drill once** on the local Docker stack; record timings vs RPO/RTO targets in an evidence section (date, duration, verifier).
  5. Index from `docs/README.md` operations section + `docs/operations/runbook.md`.
- **Acceptance (G/W/T):**
  - **G** the runbook **W** an operator follows it on a scratch stack **T** the restored stack serves `/healthz` 200 and a previously generated document (or regenerated equivalent) is retrievable.
  - **G** the drill evidence **W** LR-E2 builds the launch checklist **T** the backup item resolves to a dated drill record with measured durations vs ADR-0030 targets.
- **Gates:** Doc + drill evidence; no code gates.
- **Artifacts:** `docs/operations/backup-restore-runbook.md`; drill evidence section; index updates.
- **Done when:** Runbook merged + drill executed + evidence recorded + doc sync + commit review.
- **Maps:** ADR-0030; LR-E2 checklist input.
- **Status:** **In Progress** (2026-07-12 — slice `lrp-d2-backup-restore`; Task Master #39; sole-active; BDD not-applicable; base `362a556`; D3/D4 remain Not Started)

### LR-D3 — Metrics & alerting as code

- **Owner agent:** backend-engineer + deploy-engineer
- **BDD:** not-applicable — observability instrumentation; no user-facing behavior.
- **Read first:**
  1. Existing Micrometer/actuator setup (`application.yml` management section; P9 observability work)
  2. `deploy/helm/docgen/` monitoring hooks (P15-T05b custom metric `docgen_http_requests_per_second`; NetworkPolicy `monitoring.enabled`)
  3. LR-B3/LR-B4 outputs (SSE + DLT metrics to expose)
- **Do NOT:** Add a vendor APM dependency; alert on unmeasured thresholds (use LR-D6 baselines or mark rules as draft); bake credentials into dashboards.
- **Steps:**
  1. Add Micrometer custom metrics: generation latency (sync, with/without PDF), PDF conversion pool queue depth + rejections, active SSE connections, 429 count, async DLT depth (when Kafka active).
  2. Create `deploy/observability/` — Prometheus alert rules YAML (backend down, p95 breach vs draft threshold, pool rejections > 0, DLT depth > 0, 429 surge) + Grafana dashboard JSON (generation, conversion pool, SSE, rate-limit panels).
  3. Each alert rule carries a `runbook` annotation linking `docs/operations/runbook.md` (add matching sections).
  4. Tests: metrics registered + incremented (unit-level); scrape smoke on Docker (`/actuator/prometheus` shows the new series).
  5. Index `deploy/observability/` from `deploy/README.md`.
- **Acceptance (G/W/T):**
  - **G** the Docker stack **W** one PDF generation and one 429 occur **T** `/actuator/prometheus` exposes the new counters/timers with non-zero samples.
  - **G** the alert rules file **W** validated with promtool (or documented equivalent) **T** rules parse; every rule links a runbook section.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; scrape smoke evidence; rule lint (promtool if available — else documented manual validation).
- **Artifacts:** metric instrumentation + tests; `deploy/observability/alert-rules.yml` + `deploy/observability/grafana-docgen.json`; runbook sections; index updates.
- **Done when:** Series visible + rules/dashboards committed + doc sync + commit review.
- **Maps:** Program §1 finding 12; LR-D6 (thresholds), LR-B3/B4 (series sources).
- **Status:** Not Started

### LR-D4 — Trace propagation decision + minimal impl

- **Owner agent:** backend-engineer
- **BDD:** not-applicable — internal observability plumbing.
- **Read first:**
  1. Current trace handling (`X-Trace-Id` in `frontend/nginx.conf`; envelope `traceId`; MDC usage in logging config)
  2. Async paths: `asyncTaskExecutor` (`AsyncConfig`), Kafka producer/consumer for batch tasks
  3. `.cursor/rules/tech-stack-guardrails.mdc` dependency policy (Micrometer Tracing bridge is a **new dependency**)
- **Do NOT:** Adopt a full tracing backend (Zipkin/Tempo) in this task — decision first, minimal propagation second; break the existing envelope `traceId` contract.
- **Steps:**
  1. Write a short ADR (next free number): adopt Micrometer Tracing bridge now vs defer; scope v1 to **traceId propagation** (no span export) — verify dependency availability per policy if adopted.
  2. Implement the minimal path per decision: traceId flows request → MDC → `asyncTaskExecutor` tasks (decorator) → Kafka headers → consumer MDC.
  3. Tests: async task log carries the originating traceId; Kafka round-trip preserves it.
- **Acceptance (G/W/T):**
  - **G** a sync request with trace id T **W** it spawns an async batch task **T** worker logs for that task carry T (MDC assertion).
  - **G** Kafka transport active **W** a message round-trips **T** the consumer-side MDC traceId equals the producer's.
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`
- **Artifacts:** ADR; executor decorator + Kafka header propagation + tests.
- **Done when:** Decision recorded + propagation proven + doc sync + commit review.
- **Maps:** Program §1 finding 12.
- **Status:** Not Started

### LR-D5 — NFR quantification proposals

- **Owner agent:** doc-keeper
- **BDD:** not-applicable — documentation of **pending** proposals.
- **Read first:**
  1. `docs/requirements/non-functional-requirements.md` (current gaps)
  2. [usability-review.md](../../product/usability-review.md) §待确认 L87–91 + CD-UX-T01 task-time budget table
  3. LR-D6 results — **available** (2026-07-12 Done): [latest-summary.json](../evidence/lrp-d6-load-smoke/latest-summary.json); named defect [DEF-LRP-D6-001](../evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) feeds concurrent-PDF / resilience-mapping NFR inputs (proposed only)
- **Do NOT:** Write any number as a **confirmed** requirement — every value lands in the pending/待确认 section with rationale and measurement method; contradict CD-UX-T01 draft budgets (cross-reference them); **do not activate this task from D6 completion alone**.
- **Steps:**
  1. Draft proposal rows: p95 sync generation (with/without PDF), SSE first-event latency, concurrent generation capacity, availability target, max concurrent sessions/SSE connections.
  2. For each: proposed value, measurement method (LR-D6 harness / Playwright timing / metrics), environment assumptions, source (industry norm vs measured). Use D6 measured p95≈15939ms / p99≈16065ms / errorRate=0.4 (PDF) + DEF-LRP-D6-001 as **proposed** inputs only.
  3. Add to the NFR pending-questions section with owner + date; cross-link CD-UX-T01 budgets and LR-D6 evidence.
  4. Flag which proposals gate launch (feeds LR-E2) vs post-launch tuning.
- **Acceptance (G/W/T):**
  - **G** the NFR doc **W** proposals are merged **T** every value sits in the pending section marked «proposed — awaiting confirmation», never in confirmed sections.
  - **G** LR-D6 evidence exists **W** proposals reference it **T** each measured value links the measurement record (no orphan numbers).
- **Gates:** Doc-only; link check.
- **Artifacts:** NFR pending-section additions; cross-links.
- **Done when:** Proposals merged + flagged + doc sync + commit review.
- **Maps:** usability-review L87–91 (L89 quantification → NFR §待确认 pointer); CD-UX-T01 task-time budgets (UX only); **fed by LR-D6 Done** (DEF-LRP-D6-001 — pending consumption only).
- **Discovery:** [non-functional-requirements.md §待确认 LR-D5](../../requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation) (authored 2026-07-12; every row «proposed — awaiting confirmation»; D6 evidence linked; launch-gate vs post-launch flagged).
- **Status:** **Done** (2026-07-12 — merge `5b13476`; Task Master #38; BDD not-applicable; docs-only; NFR §待确认 proposals authored — **pending user confirmation, NOT confirmed SLOs**; architecture PASS_WITH_NOTES)

### LR-D6 — Load smoke baseline

- **Owner agent:** backend-engineer + e2e-test-engineer
- **BDD:** not-applicable — measurement harness + evidence; no behavior change. Readiness note: [lrp-d6-load-smoke.md](../../behavior/lrp-d6-load-smoke.md) (`bdd_readiness: not-applicable`, 2026-07-12).
- **Depends on:** best run after LR-A1 (profile isolation) + LR-B3 (SSE hardening) so results reflect the hardened system; validates both.
- **Read first:**
  1. Runtime generation API contract (`docs/api/openapi-v1.yaml` sync generate); demo seed credentials/templates
  2. LR-A1/LR-B3 task rows (what this smoke validates)
  3. `.cursor/rules/tech-stack-guardrails.mdc` — load-tool choice (k6 = new dependency → policy check; a JUnit/`ExecutorService` harness needs no new dependency)
- **Do NOT:** Run against shared/production environments; tune thresholds to pass (record reality); introduce k6 without dependency-policy verification (JUnit harness is the default).
- **Steps:**
  1. Choose the tool per policy (default: JUnit-based concurrent harness in `backend/src/test/` behind a system-property flag so it never runs in normal `verify`).
  2. Scenario A: **≥20 concurrent sync generations** (mixed DOCX/PDF) against the Docker stack; record p95/p99, error rate, pool rejections.
  3. Scenario B: preview + SSE under concurrency (≥5 parallel previews with progress streams) — assert zero dropped streams.
  4. Record results (date, stack version, hardware note) in the ledger + feed LR-D5 proposals; archive raw output under `frontend/e2e/evidence/` or ledger-linked location.
  5. If failures surface LR-A1/B3 regressions, file them against those tasks (do not patch here).
- **Acceptance (G/W/T):**
  - **G** the Docker stack post-LR-A1 **W** 20 concurrent sync generations run **T** error rate 0 (or every failure triaged to a named defect), p95 recorded.
  - **G** ≥5 parallel SSE preview streams **W** the batch completes **T** all streams received their terminal event (no silent drops).
- **Gates:** Harness run evidence (flagged execution); `mvn -B -ntp -f backend/pom.xml verify` unaffected.
- **Artifacts:** harness code (flag-gated) or k6 script + policy note; results evidence; ledger row.
- **Done when:** Both scenarios measured + evidence recorded + doc sync + commit review.
- **Maps:** validates LR-A1/LR-B3; feeds LR-D5/LR-D3 thresholds (DEF-LRP-D6-001 → D5 pending).
- **Status:** **Done** (2026-07-12 — merge `56383eb`; Task Master #37; BDD not-applicable; Scenario A n=20 success=12 errorRate=0.4 triaged DEF-LRP-D6-001 p95≈15939ms p99≈16065ms poolRejections=0; Scenario B 5/5 SSE dropped=0; mvn verify GREEN 1303; architecture PASS_WITH_NOTES; DEPLOY_OK 2026-07-12T01:26:46+08:00; evidence [lrp-d6-load-smoke](../evidence/lrp-d6-load-smoke/))

### LR-D7 — Durable security audit events

- **Owner agent:** backend-engineer
- **BDD:** **required** — **`ready`** ([lrp-d7-durable-security-audit.md](../../behavior/lrp-d7-durable-security-audit.md); BDD-LRP-D7-001…010) — audit records become queryable data with access rules (permission matrix §13.3).
- **Read first:**
  1. `docs/behavior/lrp-d7-durable-security-audit.md` (**authoritative**)
  2. `backend/src/main/java/com/bank/docgen/authorization/management/service/SecurityAuditSummaryService.java` + `SecurityManagementAuditRecorder` (partial SOR wiring; route-deny path unwired)
  3. `docs/security/permission-matrix.md` §13.3 (durable security audit expectation)
  4. Ledger seam «Security forbidden-route audit» ([execution-sync-ledger.md](../execution-sync-ledger.md)); management audit event model (`V9`)
  5. LR-D1 / ADR-0048 (events join `management_audit_event` 90-day retention — **no dedicated table**)
- **Do NOT:** Log passwords/tokens/PII beyond the matrix-approved fields; break existing log lines (keep them; add persistence); bypass group scoping in the query path; create a separate security-audit table; touch `DGE-audit-governance`.
- **Steps:**
  1. ~~Wait for BDD spec `ready`~~ — **done** (2026-07-11).
  2. Persist login success/failure, 403 route denials, and download grants/denials as durable `SECURITY_*` rows on **existing** `management_audit_event` (extend `SecurityManagementAuditRecorder`; add `SECURITY_DOCUMENT_DOWNLOAD_DENIED`; wire forbidden-route report API + AccessDeniedHandler + download deny path).
  3. Keep `SecurityAuditSummaryService` log output; persistence **fail-safe** (must not block login — D7-C9).
  4. Expose via the existing audit console query path with role/group scoping per matrix §10 / §13.3.
  5. Confirm LR-D1 retention covers new event types (same table); tests: each event type + scoped query + fail-safe; optional audit console smoke.
  6. Close the ledger seam «Security forbidden-route audit» row in the same change set.
- **Acceptance (G/W/T):**
  - **G** a failed login **W** it occurs **T** a durable audit row exists with username, outcome, traceId — queryable by an authorized auditor, invisible to unauthorized roles. (BDD-LRP-D7-001)
  - **G** a 403 route denial **W** it fires **T** a durable row records user + routeKey; the seam row in the ledger is marked closed with this evidence. (BDD-LRP-D7-003)
- **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; audit console smoke on Docker 4173 if UI columns change (+ §LR-C gate block in that case). Frontend E2E **not** mandatory when no new UI columns (BDD §12).
- **Artifacts:** behavior spec (**ready**); persistence + query scoping + tests; seam row closure; OpenAPI if new report endpoint.
- **Done when:** Scenarios green + seam closed + doc sync + commit review.
- **Maps:** COR-P06 residual; permission matrix §13.3; ledger seam «Security forbidden-route audit».
- **Status:** **Done** (2026-07-11 — merge `c94a356`; Task Master #36; BDD **`ready`**; mvn verify GREEN 1294; frontend gates GREEN 1144; architecture PASS_WITH_NOTES; DEPLOY_OK 2026-07-11T23:50:03+08:00; seam «Security forbidden-route audit» **closed**)

---

## 2. Exit gate (Wave LR-D)

- [x] Retention live for management/runtime audit tables (LR-D1, under LR-B2 mutex) with ADR-0048 Accepted (merge `20b2a76`; V54)
- [ ] Backup/restore drill executed with dated evidence vs ADR-0030 targets
- [ ] `deploy/observability/` alert rules + dashboards committed; new metric series scrapeable
- [ ] TraceId propagation decision recorded + minimal path proven
- [x] NFR proposals merged as pending (LR-D5 — merge `5b13476`; Task Master #38; fed by D6 evidence + DEF-LRP-D6-001; **not** confirmed SLOs)
- [x] Load smoke baselines recorded (LR-D6 — merge `56383eb`; evidence + DEF-LRP-D6-001 triage)
- [x] Security audit seam closed (LR-D7 — merge `c94a356`; BDD + tests; ledger seam closed)
