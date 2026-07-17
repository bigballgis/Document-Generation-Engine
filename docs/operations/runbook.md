# Production Runbook (v1)

Operational procedures aligned with [ADR-0030](../adr/operations/0030-operational-platform-baseline.md).
For Kubernetes rollout specifics see [deploy/README.md](../../deploy/README.md) and
[deploy/blue-green-runbook.md](../../deploy/blue-green-runbook.md).

**Launch readiness (LR-E2):** evidence-linked go/no-go rows live in
[launch-readiness-checklist.md](./launch-readiness-checklist.md) — **not** a production go-live claim.
Related: [backup-restore-runbook.md](./backup-restore-runbook.md) (LR-D2 drill).
**JWT_SECRET:** [BDD-OPS-JWT-SECRET-001](../behavior/ops-jwt-secret-no-default.md) — explicit
provision for acceptance/prod; checklist [#9](./launch-readiness-checklist.md) is **GO**
(merge `587cd9a`) — overall checklist remains **NO-GO**; **not** a go-live claim.
**KAFKA_IMAGE:** [BDD-OPS-KAFKA-REGISTRY-001](../behavior/ops-kafka-company-registry.md) — explicit
company-approved Kafka image ref for any compose path that defines `docgen-kafka`; checklist
[#10](./launch-readiness-checklist.md) is **CONDITIONAL** (path remediated; operator must supply
coords; no invented registry hostname; **not** GO without company pull evidence) — overall
checklist remains **NO-GO**; **not** a go-live claim.
**AD Group resolver:** [BDD-OPS-AD-GROUP-STUB-001](../behavior/ops-ad-group-stub-close.md) /
[ADR-0054](../adr/authorization-security/0054-ad-group-resolver-production-boundary.md) —
acceptance/production must **not** silently use `docgen.ad-group-resolver.type=config`;
`AdGroupResolverGuard` fails closed on prod-shaped paths. Local docker acceptance may set
`DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB=true` (**LAB ONLY** — not production AD). Checklist
[#5a](./launch-readiness-checklist.md) remains **NO-GO** until implement + evidence (do **not**
flip at docs-first) — overall checklist remains **NO-GO**; **not** a go-live claim.

## Release gate

Run the automated release gate before tagging a release candidate:

```powershell
./scripts/release-gate.ps1
```

Linux agents:

```bash
./scripts/p0-gate.sh
./scripts/docker-deploy-gate.sh
```

Evidence is written to `artifacts/release-gate/<timestamp>/`.

## Local production profile

**JWT_SECRET (acceptance / prod compose):** Export a ≥32-byte secret that is **not** a known insecure default before bringing up the prod profile. Compose uses `${JWT_SECRET:?…}` (no `:-` fallback); missing or known-insecure values fail closed. See [Required environment variables](#required-environment-variables-production) and [BDD-OPS-JWT-SECRET-001](../behavior/ops-jwt-secret-no-default.md). Checklist [#9](./launch-readiness-checklist.md) is **GO** (merge `587cd9a`) — clearing #9 alone is **not** go-live; overall checklist remains **NO-GO**.

**KAFKA_IMAGE (any compose with `docgen-kafka`):** Export a full image reference before `docker compose config` / `up`. Compose uses `${KAFKA_IMAGE:?KAFKA_IMAGE must be set}` (**no** `:-bitnamilegacy…` silent default). Production / acceptance **must** use an operator-supplied **company-approved** registry coordinate shaped like `<company-registry>/<kafka-image>:<tag>` — the company registry hostname is **UNKNOWN** in-repo; **do not** invent a private registry hostname as a production fact. Local/dev may copy the Hub example from [`.env.example`](../../.env.example) (`bitnamilegacy/kafka:3.7`) — **LOCAL/DEV ONLY**, never a claimed production coordinate. Behavior SoT: [BDD-OPS-KAFKA-REGISTRY-001](../behavior/ops-kafka-company-registry.md). Checklist [#10](./launch-readiness-checklist.md) is **CONDITIONAL** — clearing the Hub-hardcode path alone is **not** go-live; overall checklist remains **NO-GO**.

**True prod contract (claimed production vs LAB):** [BDD-PRR-B01-TPC](../behavior/prod-true-prod-contract.md) —
`docker-compose.prod.yml` defaults are claimed-production shape (`SPRING_PROFILES_ACTIVE=prod` only,
`APP_ENVIRONMENT=prod`, `ASYNC_TRANSPORT=kafka`, AD stub LAB default **false**, demo classpath default
**false**). `ProductionAsyncTransportGuard` enforces kafka whenever the `prod` profile is active
(aligned with JWT / AD guards — `prod,dev` no longer bypasses). **Local docker acceptance / E2E**
must explicitly stack `docker-compose.lab.yml` (**LAB ONLY** — AD stub + demo classpath); that overlay
is **not** a claimed production entry. Helm claimed production: `values-prod.yaml`
(`springProfilesActive=prod`, `appEnvironment=prod`, `asyncTransport=kafka`). Checklist **#5a** stays
**CONDITIONAL** (no invented LDAP coords) — overall checklist remains **NO-GO**; **not** a go-live claim.

**AD Group resolver (acceptance / prod):** Do **not** silently run with `docgen.ad-group-resolver.type=config` (YAML `account-groups` stub) as production directory resolution. `AdGroupResolverGuard` refuses config stubs whenever the `prod` profile is active (even if `APP_ENVIRONMENT=dev`) **or** the process is outside soft `dev`/`local`/`test` — same honesty as JWT-C3. Unimplemented non-config types (`ldap` / `directory` / …) also fail closed (no invented LDAP client in-repo). **Local docker acceptance LAB ONLY:** stack `docker-compose.lab.yml` (or set `DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB=true`) so E2E can keep using the config map; this is **not** enterprise AD. Claimed production (`docker-compose.prod.yml` alone) defaults the override to **false** and fail-closes without a real directory adapter — company LDAP coords remain **UNKNOWN**; **do not** invent hostnames. Local/dev/test (no `prod` profile) may keep `type=config` + demo `account-groups`. Behavior SoT: [BDD-OPS-AD-GROUP-STUB-001](../behavior/ops-ad-group-stub-close.md) / [BDD-PRR-B01-TPC](../behavior/prod-true-prod-contract.md). Checklist [#5a](./launch-readiness-checklist.md) stays **CONDITIONAL** until real directory evidence — overall checklist remains **NO-GO**.

```powershell
# Example — operator-generated secret (never commit; never reuse local-dev / prod-change-me placeholders)
$env:JWT_SECRET = '<explicit-non-default-≥32-bytes>'
# Production / acceptance — set company-approved Kafka image (placeholder form only; do not invent hostname)
$env:KAFKA_IMAGE = '<company-registry>/<kafka-image>:<tag>'
# Local/dev only — may use the documented Hub example from .env.example (never claim as production)
# $env:KAFKA_IMAGE = 'bitnamilegacy/kafka:3.7'
# AD Group: do not silently use type=config on claimed production.
# Local docker acceptance (LAB ONLY): stack docker-compose.lab.yml (or set stub=true).
# Claimed production: omit LAB overlay / leave stub false; supply a real directory adapter when available
# (coords UNKNOWN — do not invent hostnames).
# $env:DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB = 'true'   # LAB ONLY
# $env:DOCGEN_AD_GROUP_RESOLVER_TYPE = '<directory-spi-type>'  # placeholder; not config
```

Build backend JAR first, then start the **claimed production** compose profile:

```powershell
mvn -B -ntp -f backend/pom.xml package -DskipTests
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d --build
```

Local acceptance / E2E (**LAB ONLY** — not claimed production):

```powershell
docker compose -f docker-compose.yml -f docker-compose.prod.yml -f docker-compose.lab.yml --profile prod up -d --build
```

Queued single-host acceptance deploy (preferred on this machine; includes LAB overlay via `docker-deploy.ps1`):

```powershell
$env:JWT_SECRET = '<explicit-non-default-≥32-bytes>'
$env:KAFKA_IMAGE = '<company-registry>/<kafka-image>:<tag>'
.\scripts\docker-deploy-queue.ps1
```

- Backend liveness: `http://localhost:8080/healthz`
- Backend readiness: `http://localhost:8080/readyz` (Postgres `SELECT 1` only — see **Readiness scope** below)
- Frontend liveness/readiness: `http://localhost:4173/healthz` and `http://localhost:4173/readyz`
- Prometheus metrics (prod profile): `http://localhost:8080/actuator/prometheus`

Compose prod profile enables backend and frontend health checks with `service_healthy` gating (SOR-O05).

## Readiness scope (SOR-O06)

| Probe | Endpoint | Traffic gate | Diagnostic checks |
| --- | --- | --- | --- |
| Liveness | `/healthz` | N/A — process up only | None |
| Readiness | `/readyz` | **PostgreSQL `SELECT 1` only** (503 when down) | `checks.postgres`, `checks.redis`, `checks.minio`, `checks.kafka` |

Redis, MinIO, and Kafka appear in `/readyz` JSON for **diagnostics only** — they do **not** remove the pod from Service endpoints when Postgres is healthy (F8-C2 / SOR-O06 preserved). Optional contributors use `@Profile("!test")` with short timeouts (≤ 2s).

Example response (Postgres up, Redis down):

```json
{
  "status": "UP",
  "checks": {
    "postgres": { "status": "UP" },
    "redis": { "status": "DOWN" },
    "minio": { "status": "UP" },
    "kafka": { "status": "SKIPPED", "detail": "async transport is not kafka" }
  }
}
```

When `ASYNC_TRANSPORT` is not `kafka`, `checks.kafka.status` is `SKIPPED`.

See [deploy/k8s-health-probes.md](../../deploy/k8s-health-probes.md) for probe wiring.

## Observability

- **Structured logs:** `prod` Spring profile emits JSON logs via Logstash encoder (`logback-spring.xml`).
- **Trace propagation:** `X-Trace-Id` request header is echoed on responses and bound to MDC `traceId` for log correlation.
- **Metrics:** Actuator exposes `health`, `info`, `metrics`, and `prometheus` in prod profile. Management security permits unauthenticated scrape of `/actuator/prometheus` for in-cluster collectors (SOR-O01).
- **Alert rules as code (LR-D3):** Versioned Prometheus rules live under [`deploy/observability/`](../../deploy/observability/README.md). Each rule must carry a `runbook` annotation pointing at a section below. **Thresholds are draft / proposed only** — see [Draft alert thresholds](#draft-alert-thresholds-lrd3--not-confirmed-slos); do **not** treat firings as SLA breaches until NFR confirmation ([NFR §待确认 LR-D5](../requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation)).
- **Kubernetes:** When `observability.serviceMonitor.enabled=true`, Helm renders a Prometheus Operator `ServiceMonitor` scraping `/actuator/prometheus`. Optional `PrometheusRule` alerts on pod restarts and elevated 5xx rates (see `deploy/helm/docgen/templates/`). No vendor APM is required or assumed.

### Verify Prometheus scrape (local prod compose)

```bash
curl -sf http://localhost:8080/actuator/prometheus | head
```

Expect `# HELP` lines for JVM and HTTP metrics. After LR-D3 instrumentation, also expect non-zero samples for generation / PDF pool / SSE / 429 / DLT series when those paths have been exercised.

<a id="draft-alert-thresholds-lrd3--not-confirmed-slos"></a>

### Draft alert thresholds (LR-D3 — NOT confirmed SLOs)

> **Governance:** Every number in this table is **draft / proposed**. Sources are LR-D6 measured inputs and LR-D5 pending NFR proposals. They are **not** confirmed requirements, contractual SLAs, or launch-gate enforcement bars. Keep `draft: "true"` on Prometheus rules until explicit user confirmation of NFR values.

| Alert family | Draft expr / threshold (proposed) | Evidence / proposal source | Status |
| --- | --- | --- | --- |
| Backend down | Probe `/healthz` (or equivalent up-metric) fails for **≥ 2m** | Ops availability planning (NFR availability **pre-measurement**); no vendor APM | **draft** |
| Generation / HTTP p95 breach | **Do not** enforce F8-era ≤3 s / ≤10 s as live SLOs — D6 FOL concurrent smoke **does not support** those bars. **Interim observed envelope:** sync success-sample p95 ≈ **15939 ms** / p99 ≈ **16065 ms** ([latest-summary.json](../plan/evidence/lrp-d6-load-smoke/latest-summary.json)). Prefer either (a) draft rule at ~**16 s** interim envelope **or** (b) keep aspirational ≤3 s / ≤10 s only with `draft: true` + NFR note that values are **stale vs D6**. | LR-D6 Scenario A; [NFR §待确认 LR-D5](../requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation); [DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) | **draft** |
| PDF pool rejections | `docgen` PDF conversion **pool rejection** counter / gauge **> 0** for **≥ 2m** | D6 Scenario A recorded **poolRejections=0** (measured-input only — not a capacity SLA) | **draft** |
| Async DLT depth | Kafka DLT depth gauge **> 0** for **≥ 5m** (when `ASYNC_TRANSPORT=kafka`) | No D6 measurement — **pre-measurement** ops signal for LR-B4 DLT | **draft** |
| HTTP 429 surge | Rate of HTTP **429** responses (or Micrometer 429 counter) **> 1/s** for **≥ 5m** (planning placeholder) | No D6 429 rate measured — **pre-measurement**; tune after scrape baseline | **draft** |
| SSE emitters (capacity / leak) | Active SSE emitters gauge **> 100** for **≥ 5m** (draft capacity signal) | LR-B3 SSE lifecycle; no confirmed concurrency SLO — **pre-measurement** | **draft** |

**Implementer note:** Wire `annotations.runbook` to `docs/operations/runbook.md#<id>` using the stable ids on the alert sections below.

<a id="alert-backend-down"></a>

### Alert: Backend down

**When this fires:** Backend process is unreachable or liveness fails continuously past the draft window.

**Triage:**

1. Confirm local/cluster probe: `curl -sf http://<backend>/healthz` (expect 200).
2. Check recent deploy / image tag / blue-green `activeColor`.
3. Inspect compose/pod logs for crash loops; verify Postgres is up (readiness gate is Postgres-only — see [Readiness scope](#readiness-scope-sor-o06)).
4. Capture `X-Trace-Id` from any client errors if still partially serving.

**Mitigate:** Restart unhealthy replicas; roll back color/image if post-deploy; do not destroy DB/MinIO volumes.

**Escalate when:** Healthz stays down after restart + dependency check, or multi-replica failure.

<a id="alert-p95-latency-breach-draft"></a>

### Alert: p95 latency breach (draft)

**When this fires:** Generation or HTTP p95 exceeds the **draft** rule threshold. **Not** an SLA breach until NFR confirmation.

**Triage:**

1. Confirm whether the rule uses the **interim ~16 s D6 envelope** or a stale aspirational ≤3 s / ≤10 s draft — see [Draft alert thresholds](#draft-alert-thresholds-lrd3--not-confirmed-slos).
2. Split DOCX vs PDF if metrics allow; concurrent PDF path may map to `api.error.generation.serviceUnavailable` / `TEMPLATE_VALIDATION_FAILED` ([DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md)).
3. Check LibreOffice / PDF pool saturation, JVM heap/CPU, HikariCP utilization, and recent load.
4. Correlate with [LR-D6 evidence](../plan/evidence/lrp-d6-load-smoke/) before changing product code to “green” an alert.

**Mitigate:** Reduce concurrent PDF load; verify pool size (`PDF_CONVERSION_POOL_SIZE`); scale replicas if CPU-bound; avoid tuning product thresholds solely to silence the alert.

**Escalate when:** Sustained breach after load drop, or DEF-LRP-D6-001-class failures spike with user impact.

<a id="alert-pdf-pool-rejections"></a>

### Alert: PDF conversion pool rejections

**When this fires:** Bounded PDF conversion pool is rejecting work (queue capacity / fail-fast per SOR-P03).

**Triage:**

1. Scrape pool gauges (active / queue / rejections) on `/actuator/prometheus`.
2. Compare to D6 baseline: Scenario A **poolRejections=0** under n=20 mixed sync — non-zero rejections are a **new** capacity signal, not explained by that smoke alone.
3. Check `PDF_CONVERSION_POOL_SIZE`, `PDF_CONVERSION_QUEUE_CAPACITY`, and LibreOffice process health.

**Mitigate:** Lower ingress concurrency; increase pool only with measured evidence; fail-fast is intentional when queue capacity is 0.

**Escalate when:** Rejections persist with healthy LibreOffice and rising user-facing `serviceUnavailable`.

<a id="alert-dlt-depth"></a>

### Alert: Async DLT depth

**When this fires:** Kafka dead-letter topic (or app DLT depth gauge) is non-empty while async transport is Kafka.

**Triage:**

1. Confirm `ASYNC_TRANSPORT=kafka` (otherwise DLT alert may be N/A / SKIPPED).
2. Inspect DLT consumer / poison messages; preserve payloads for audit — do not invent broker credentials here (use cluster Secret / `.env` patterns).
3. Check producer/consumer lag, schema/payload scrub errors, and recent batch jobs.

**Mitigate:** Replay or quarantine DLT messages per async durability runbook practices (F5 / LR-B4); fix root cause before bulk replay.

**Escalate when:** DLT depth grows continuously or blocks business batch completion.

<a id="alert-429-surge"></a>

### Alert: HTTP 429 surge

**When this fires:** Rate-limit / throttle responses surge above the **draft** rate placeholder.

**Triage:**

1. Confirm whether clients are retrying aggressively or a credential/policy is undersized.
2. Check API policy / Bucket4j (or equivalent) limits and management audit for related security/rate events.
3. No D6 measured 429 baseline — treat first firings as calibration, not capacity proof.

**Mitigate:** Back off clients; adjust policy only with product/ops confirmation; do not bake credentials into dashboards or alert annotations.

**Escalate when:** Legitimate traffic is blocked at scale or 429 coincides with auth failures.

<a id="alert-sse-emitters"></a>

### Alert: High SSE emitter count

**When this fires:** Active SSE emitters (`docgen_sse_emitters_active`) stay above the **draft** capacity threshold (default **> 100** for **≥ 5m**). May indicate a connection leak (LR-B3) or a genuine concurrency spike. **Not** a confirmed concurrency SLO until NFR confirmation.

**Triage:**

1. Scrape `docgen_sse_emitters_active` on `/actuator/prometheus` and note trend (step up vs sawtooth).
2. Correlate with preview / batch SSE clients still open after jobs complete; check browser tabs and API clients that never close the stream.
3. Review recent deploy for SSE lifecycle regressions (register/complete/timeout cleanup).
4. Confirm whether load is expected (many concurrent previews) vs stuck emitters after clients disconnect.

**Mitigate:** Restart only if emitters stay elevated with no live clients; prefer fixing client close / server timeout paths over raising the draft threshold solely to silence the alert.

**Escalate when:** Count grows continuously after clients disconnect, or preview/batch UX degrades with rising emitter gauge.

## Required environment variables (production)

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Management JWT signing (**min 32 bytes**). **Must be explicitly provisioned** (env / `.env` / Secret Manager / cluster Secret) for acceptance/prod compose, queued Docker deploy, and prod-shaped scripts (e.g. `container-hardening-smoke.ps1`) — compose uses `${JWT_SECRET:?…}` (**no** `:-` default) and scripts must **not** silently fall back. Known insecure values (`local-dev-only-change-me-please-32bytes-min`, `prod-change-me-32-bytes-minimum-secret`) are **refused fail-closed** on acceptance/prod paths (logs must not print the secret). Local `dev`/`local`/`test` may use documented test secrets (see [`.env.example`](../../.env.example)). Behavior SoT: [BDD-OPS-JWT-SECRET-001](../behavior/ops-jwt-secret-no-default.md). Checklist [#9](./launch-readiness-checklist.md) is **GO** (merge `587cd9a`) — **overall checklist remains NO-GO**; **not** a go-live claim. |
| `KAFKA_IMAGE` | Full Kafka container image reference for `docgen-kafka` (registry/repo/tag). **Required** for any compose file set that defines `docgen-kafka` (including prod overlay that `depends_on` it). Compose uses `${KAFKA_IMAGE:?KAFKA_IMAGE must be set}` (**no** `:-bitnamilegacy…` silent default; **no** hardcoded Hub image as the sole prod path). **Production / acceptance:** operator must supply a **company-approved** coordinate (`<company-registry>/<kafka-image>:<tag>`). Company registry hostname is **UNKNOWN** in this repository — **do not** invent a private registry hostname and treat it as production fact. **Local/dev only:** [`.env.example`](../../.env.example) may document `bitnamilegacy/kafka:3.7` as a non-production example — never claim it as the production coordinate. Behavior SoT: [BDD-OPS-KAFKA-REGISTRY-001](../behavior/ops-kafka-company-registry.md). Checklist [#10](./launch-readiness-checklist.md) is **CONDITIONAL** (path remediated; no company pull evidence → not GO) — **overall checklist remains NO-GO**; **not** a go-live claim. |
| `docgen.ad-group-resolver.type` / `DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB` | AD Group resolution source. **`config` = local/dev/test only** (`ConfigAdGroupResolver` + `account-groups` YAML). **Claimed production** (`docker-compose.prod.yml`): stub LAB default **false**; `AdGroupResolverGuard` refuses `type=config` on `prod` profile unless **LAB ONLY** overlay/`true`. **Local acceptance:** stack `docker-compose.lab.yml`. Unimplemented non-config types also fail closed. Company LDAP/AD coordinates **UNKNOWN** — **do not** invent hostnames. Behavior SoT: [BDD-OPS-AD-GROUP-STUB-001](../behavior/ops-ad-group-stub-close.md) / [BDD-PRR-B01-TPC](../behavior/prod-true-prod-contract.md) / [ADR-0054](../adr/authorization-security/0054-ad-group-resolver-production-boundary.md). Checklist [#5a](./launch-readiness-checklist.md) remains **CONDITIONAL** until implement + evidence — **overall checklist remains NO-GO**; **not** a go-live claim. |
| `POSTGRES_*` | Database connection |
| `MINIO_*` | Object storage |
| `APP_ENVIRONMENT` | Runtime environment label |
| `ASYNC_TRANSPORT` | Must be `kafka` in prod profile |

## Backup and restore (ADR-0030)

**Canonical procedure:** [backup-restore-runbook.md](./backup-restore-runbook.md) (LR-D2).

**Cadence (ADR-0030):** weekly full backup + daily incremental (operator-managed Postgres/MinIO tooling on this stack — logical `pg_dump`, WAL/snapshot guidance for prod-shaped Postgres, MinIO bucket mirror). Redis is rebuildable (no backup). Secrets stay in `.env` / cluster Secret patterns — never invent production values here.

**Status:** Scratch drill **executed 2026-07-12** — see [backup-restore-runbook.md § Drill evidence](./backup-restore-runbook.md#drill-evidence-2026-07-12--executed). Observed local RPO ≈ 0.93 min / RTO ≈ 4.75 min (**scratch scope only**; not a production compliance claim — no WAL/PITR).

## Disaster Recovery

Aligned with [ADR-0030](../adr/operations/0030-operational-platform-baseline.md): **RPO ≤ 15 min**, **RTO ≤ 30 min** (**targets**; local scratch drill 2026-07-12 met targets in-scope — see backup-restore evidence; production still needs WAL/PITR path). Failover is **manual**; use [blue-green runbook](../../deploy/blue-green-runbook.md) for app-level color revert. Full steps, confirmation gate, and evidence: [backup-restore-runbook.md](./backup-restore-runbook.md).

### Preconditions

- Isolated namespace/cluster or compose project for drill (never against production traffic). Prefer `-p docgen-scratch`.
- Latest Postgres dump (+ WAL/PITR material when configured) and MinIO backup bucket / volume copy available.
- Flyway is **forward-only** — no down migrations; schema fix = compensating migration; app rollback = blue-green color revert.
- **Explicit confirmation gate** before any destructive restore (`RESTORE-CONFIRM …`) — see backup-restore runbook.

### DR drill checklist

1. **Backup verify** — confirm latest Postgres and MinIO backups within the intended RPO window.
2. **Confirmation gate** — record `RESTORE-CONFIRM <project> <date>` before destructive steps.
3. **Restore Postgres** — restore dump (and WAL/PITR if applicable) to isolated instance; record restore start time.
4. **Restore MinIO** — mirror backup bucket → `STORAGE_BUCKET` (default `docgen-artifacts`); verify bucket head / sample object.
5. **Start stack** — `docker compose -p docgen-scratch -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d` (or Helm to isolated namespace).
6. **Flyway** — application startup runs migrate; expect no-op if backup is post-migration.
7. **Liveness / readiness** — `curl -sf http://localhost:8080/healthz` → 200; `curl -sf http://localhost:8080/readyz` → 200, `checks.postgres.status=UP`.
8. **Smoke** — one generated document round-trip (prior artifact from backup **or** regenerated equivalent).
9. **Record RPO/RTO** — note observed recovery time vs ADR-0030 **targets** (do not claim compliance if unmet).
10. **Archive evidence** — `artifacts/dr-drill/<YYYY-MM-DD>/` with:
   - `restore-log.txt` — commands and timestamps
   - `readyz.json` — post-restore readiness snapshot
   - `smoke-notes.md` — generation / document retrieval outcome
   - `rpo-rto.json` — `{ "rpoObservedMinutes": N, "rtoObservedMinutes": N, "targets": { "rpoMinutes": 15, "rtoMinutes": 30 }, "meetsTargets": null }`

First annual drill **execution** is tracked under **LR-D2** (Task Master #39). **2026-07-12 scratch drill complete** — evidence in [backup-restore-runbook.md § Drill evidence](./backup-restore-runbook.md#drill-evidence-2026-07-12--executed) and `artifacts/dr-drill/2026-07-12/` (gitignored locally; summary in runbook).

### Schema rollback policy

- **No Flyway down migrations.**
- Breaking schema: expand-contract / **compensating migration**; revert traffic via blue-green `activeColor` to previous image if the schema remains compatible — details in [backup-restore-runbook.md § Flyway](./backup-restore-runbook.md#flyway-forward-only-rollback-playbook) and [blue-green-runbook.md](../../deploy/blue-green-runbook.md).

## Incident response

1. **Detect:** Prometheus alert or user report; capture `X-Trace-Id` from UI error. For LR-D3 draft rules, open the linked `runbook` annotation ([alert sections](#observability)).
2. **Triage:** Check `/readyz`, dependency health (Postgres, Redis, MinIO, Kafka), recent deploy.
3. **Mitigate:** Scale replicas, rollback blue-green color, or disable feature flag seam documented in ADR.
4. **Communicate:** Email notification channel per ADR-0030 (operator runbook outside repo).
5. **Post-incident:** Update ledger evidence; file corrective ADR if architecture change required.

## Flyway migration rollout (blue-green)

1. Deploy **inactive** color with new image (migrations run on startup).
2. Run smoke on preview Service (`blueGreen.previewService.enabled`).
3. Manual approval per `blueGreen.requireManualApproval`.
4. Switch `activeColor` in values; verify `/readyz` on new color before draining old pods.
5. **Expand-contract:** backward-compatible migrations only; breaking schema changes require two-phase release notes.

## Rollback

1. Stop prod profile containers: `docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod down`
2. Deploy previous image tag (Kubernetes: revert `activeColor` or Helm revision).
3. Flyway migrations are forward-only; rollback requires a new migration if schema changed.

## Secret rotation (ADR-0030)

Target: automatic rotation every 30 days with zero-downtime refresh. Operator rotates `JWT_SECRET`, DB, and MinIO credentials in Secret Manager; rolling restart backend pods after Secret update. Verify login and runtime generation after rotation.
