# BDD behavior spec: PQH-F7 — Redis / coordinated runtime rate-limit

| Field | Value |
| --- | --- |
| **Document status** | `ready` |
| **BDD ID prefix** | `BDD-PQH-F7` |
| **Authored** | 2026-07-23 |
| **Program / queue** | Post-queue hardening · **PQH-F7** (IBL **F7** / **Q1** follow-on; ADR-0044 prerequisite **#3**) |
| **Slice** | `pqh-f7-redis-rate-limit` |
| **Branch** | `feat/pqh-f7-redis-rate-limit` |
| **Worktree** | `D:/working/DGE-pqh-f7-redis-rate-limit` |
| **Base** | `850b51c9` (handoff) |
| **Placement** | ISOLATED |
| **Task Master** | **#163** PQH-F7 (unparked by parent deliver; Batch Recommendation **solo**; `member_task_ids: ["163"]`) |
| **Formal phase** | **None** (do not invent a sole-active formal P-phase) |
| **Batch recommendation** | **solo** (`proposed_slice_id: pqh-f7-redis-rate-limit`; vetoes below) |
| **Owning docs** | **This file (leaf behavior SoT)**; finding SoT [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) **F7 / Q1**; program [post-queue-hardening-program-2026-07.md](../plan/post-queue-hardening-program-2026-07.md); topology [ADR-0044 deployment topology](../adr/operations/0044-deployment-topology-v1.md); scale-out baseline [ADR-0044 multi-instance correctness](../adr/operations/0044-multi-instance-correctness-baseline.md); locks deferral [ADR-0039](../adr/technology-stack/0039-redisson-lock-evaluation.md); API hardening [ADR-0031](../adr/api/0031-api-platform-hardening-baseline.md); honesty sibling [prod-ops-security-hardening.md](./prod-ops-security-hardening.md) **D01B-C9** |
| **Frontend UI** | **`frontend_ui_in_scope=false`** (runtime filter / BE only; E2E/UIUX **N/A**) |
| **API contract** | **429 `RATE_LIMIT_EXCEEDED` envelope unchanged**; **new** fail-closed **503** surface when `distributed=true` and Redis coordination unavailable (see **F7-C7**) → `backend_api_contract_change=true` for that path only |

**Completion claim constraints:** This leaf delivers and verifies the **shared runtime rate-limit** scale-out prerequisite (ADR-0044 row **#3** / IBL **F7**). **Do not** claim multi-instance correctness complete. **Do not** claim SSE sticky/relay Done, Redisson locks Done (ADR-0039), or Kafka multi-instance Done. **Do not** claim IBL program Done, CE Done, or go-live. **Do not** flip checklist **#3b** / **#5a** GO. **Do not** activate **CE-O02** / **#119**. **Do not** mark umbrella **#53** Done.

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["163"]
  proposed_slice_id: pqh-f7-redis-rate-limit
  shared_acceptance_surface: >
    RuntimeRateLimitService/Filter + Redis coordinated limiter
    (Bucket4j-redis or project-accepted Redis-backed Bucket4j pattern)
  vetoes_applied:
    - checklist-#3b/#5a-GO
    - CE-O02
    - mark-#53-CE-Done
    - activate-#119-Word-host
    - do-not-claim-IBL-CE-go-live-Done
  evidence_amortization: mvn verify + Stage 10 queued deploy; FE E2E N/A
  on_red_split_hint: N/A solo
```

| IN (this leaf) | OUT (later / explicitly forbidden) |
| --- | --- |
| Redis-coordinated runtime API rate-limit when `docgen.runtime.rate-limit.distributed=true` | Management UI / login rate-limit |
| Process-local Bucket4j remains default authority when `distributed=false` | Claiming multi-instance complete (SSE / Redisson locks / Kafka) |
| Preserve missing-credential-header **pass-through** (LR-B7) | IP-based bucket fallback |
| Shared quota across ≥2 backend instances for same `credentialId:accessAccount` | Gateway-only enforcement as sole delivery (gateway may complement later; not this leaf’s Done) |
| Fail-closed when Redis unavailable **while** distributed=true | Silent fail-open that continues unlimited traffic under distributed mode |
| Docs honesty: close “distributed aspirational / not delivered” residual for **rate-limit row only** after green verify + deploy evidence | Flip #3b/#5a; claim IBL/CE/go-live; activate #119 / CE-O02; mark #53 Done |
| Wire / implement `RuntimeRateLimitProperties.distributed` + conditional Redis-backed service | Adopt Redisson **locks** (ADR-0039 remains deferred for idempotency/async ownership) |

---

## 1. Overview

### 1.1 Problem (current evidence — implementation input)

| Finding | Evidence |
| --- | --- |
| Runtime limiter is **process-local** `ConcurrentHashMap` of Bucket4j buckets | `RuntimeRateLimitService` |
| Filter scopes **only** runtime `/api/{env}/v1/*`; skips `/api/management/` | `RuntimeRateLimitFilter.shouldNotFilter` |
| Missing credential headers → **pass-through** (auth fails closed downstream 401) | `RuntimeRateLimitFilter` LR-B7 / ADR-0031 comment |
| Bucket key = `credentialExternalId:accessAccount` | Filter + `tryConsumeKey` |
| 429 contract: `RATE_LIMIT_EXCEEDED`, category `RUNTIME`, `retryable=true`, `Retry-After`, messageKey `api.error.runtime.rateLimitExceeded`; audit + `docgen.http.rate_limit.denied` | Filter + `RateLimitDeniedMetrics` |
| Prod `distributed` flag exists but defaults **false**; process-local remains authoritative (ADR-0044 honesty residual from PRR-D01B) | `application-prod.yml`; ADR-0044 multi-instance baseline |
| ADR-0044 target names `RedisRuntimeRateLimitService` when distributed enabled | ADR-0044 Decision §1 |
| ADR-0031 baseline: Redis centralized counters; deferred for v1 single-replica; required on scale-out | ADR-0031 + ADR-0044 topology deferral § |
| IBL **F7** / **Q1** parked as PQH **#163** | IBL program + PQH program |
| Redisson locks still deferred (orthogonal) | ADR-0039 |

### 1.2 Behavior domains

| Domain | Summary |
| --- | --- |
| **F7-S1 Process-local parity** | `distributed=false` (default): today’s in-process Bucket4j behavior unchanged |
| **F7-S2 Coordinated multi-instance quota** | `distributed=true`: shared Redis-backed bucket; N instances share one quota per key |
| **F7-S3 Identity + pass-through** | Key = `credentialId:accessAccount` only; missing headers pass through |
| **F7-S4 Scope** | Runtime API filter only; management paths excluded |
| **F7-S5 Redis unavailable (distributed)** | Fail-closed **503** (not unlimited fail-open; not fake 429 quota) |
| **F7-S6 429 contract stability** | Quota exceeded path unchanged vs today’s filter |
| **F7-S7 Honesty / ops switch** | Default remains `false`; enabling is the deliberate scale-out switch after Redis healthy |
| **F7-S8 Observability** | Keep 429 denied counter; add Redis-backend-unavailable metric when distributed path cannot coordinate |

---

## 2. Actor / Role

| Actor | Role / capability | Notes |
| --- | --- | --- |
| **Runtime API caller** | Supplies credential headers; calls `/api/{env}/v1/*` | Subject to per-identity quota |
| **Unauthenticated / headerless caller** | Omits credential headers on runtime path | Limiter **pass-through**; auth layer 401 fail-closed |
| **Platform operator / SRE** | Sets `RUNTIME_RATE_LIMIT_*` env; operates Redis | Opt-in `RUNTIME_RATE_LIMIT_DISTRIBUTED=true` only when Redis healthy and multi-replica intended |
| **Backend instance(s)** | One or more JVM processes behind the traffic path | With distributed=true must share quota |
| **(Non-actor) Management UI user** | `/api/management/*` | Out of scope — filter does not apply |

---

## 3. Goal

1. Deliver a **verified** Redis-coordinated runtime rate-limit implementation selectable via `docgen.runtime.rate-limit.distributed` / `RUNTIME_RATE_LIMIT_DISTRIBUTED`.
2. Preserve **single-instance parity** when the flag is false (v1 honesty / default).
3. When the flag is true, enforce a **single shared quota** per `credentialId:accessAccount` across instances.
4. Preserve deliberate **pass-through** without credential headers (LR-B7).
5. On Redis coordination failure while distributed=true: **fail-closed** (no unlimited traffic).
6. Keep **429** envelope / `Retry-After` / audit / denied metric semantics for true quota exhaustion.
7. Close ADR-0044 honesty residual for the **rate-limit prerequisite row only** after gates + queued deploy evidence — without claiming full multi-instance correctness.
8. `frontend_ui_in_scope=false`; formal phase **None**; hard vetoes respected.

---

## 4. Confirmed decisions (this leaf — locked)

Parent authorized unpark+deliver; product questions resolved with repo-grounded defaults below. **Confirmed for this leaf.**

| ID | Decision | Basis / rationale |
| --- | --- | --- |
| **F7-C1** | **Bucket key identity:** `credentialExternalId` (header credential id) **`:`** `accessAccount` only. **No** client-IP fallback, **no** anonymous IP bucket, **no** tenant-only key without access account. | Matches today’s filter; ADR-0031 dual-dimension intent realized as credential+account on runtime path |
| **F7-C2** | **Missing headers → pass-through:** If credential id or access account header is null/blank, the rate-limit filter **must not** consume a bucket and **must** continue the chain (auth fails closed with **401** downstream). | LR-B7 recorded decision; ledger seam «Runtime rate limit»; deliberate — not a defect |
| **F7-C3** | **Scope = runtime API filter only:** Applies to paths matching `/api/{env}/v1/*` when rate-limit `enabled=true`. **Does not** apply to `/api/management/**`. **Does not** add management-login rate limiting in this leaf. | `shouldNotFilter` today; handoff scope |
| **F7-C4** | **`enabled` vs `distributed`:** `docgen.runtime.rate-limit.enabled` (env `RUNTIME_RATE_LIMIT_ENABLED`) continues to master-switch the filter. `distributed` only selects **storage/coordination backend** when enabled. If `enabled=false`, filter skips entirely (both backends). | Existing `enabled()` gate |
| **F7-C5** | **Property path (canonical):** `docgen.runtime.rate-limit.distributed` ↔ env `RUNTIME_RATE_LIMIT_DISTRIBUTED`. ADR wording `docgen.rate-limit.distributed` (if present) is a **doc alias to align**, not a second runtime key. | `application-prod.yml` + `RuntimeRateLimitProperties` prefix |
| **F7-C6** | **Default after this leaf:** `distributed` remains **`false`** in prod/default config. Enabling is an **ops scale-out switch**, not auto-on when Redis is healthy. Single-replica v1 honesty preserved. | ADR-0044 honesty residual; PRR-D01B; handoff preference |
| **F7-C7** | **Redis unavailable while `distributed=true`:** **Fail-closed.** Do **not** fall back to process-local unlimited-or-weaker quota that would silently break shared semantics. Response: HTTP **503**, unified envelope with **new** code `RATE_LIMIT_BACKEND_UNAVAILABLE`, category `RUNTIME`, `retryable=true`, English-first messageKey `api.error.runtime.rateLimitBackendUnavailable`, optional `Retry-After` (≥1s, implementation may use a small fixed/backoff hint). **Must not** use `RATE_LIMIT_EXCEEDED` / **429** for backend outage (that would lie about caller quota). | Bank-grade: prefer availability control over silent fail-open; distinguish ops outage from caller throttle |
| **F7-C8** | **Quota exceeded (both backends):** HTTP **429**, `error.code=RATE_LIMIT_EXCEEDED`, `category=RUNTIME`, `retryable=true`, `messageKey=api.error.runtime.rateLimitExceeded`, `Retry-After` from probe refill nanos (min 1s), audit `API_RATE_LIMIT_DENIED`, metric `docgen.http.rate_limit.denied` — **unchanged contract**. | Existing filter + tests |
| **F7-C9** | **Shared quota semantics (`distributed=true`):** For the same key, total successful consumes across all instances within the configured window/burst **must not** exceed the configured `burstCapacity` / refill (`requestsPerMinute`) beyond Bucket4j’s normal token semantics. Two instances must not each grant a full independent process-local quota. | Closes F7 / ADR-0044 #3 |
| **F7-C10** | **Process-local mode (`distributed=false`):** Keep in-process Bucket4j map behavior (parity with today’s `RuntimeRateLimitService`). No Redis required for rate-limit correctness in this mode. | v1 single-replica / default |
| **F7-C11** | **Implementation pattern:** Prefer project-accepted **Bucket4j Redis** (or equivalent Redis-backed Bucket4j proxy named toward ADR’s `RedisRuntimeRateLimitService`) selected via `@ConditionalOnProperty` (or equivalent). **May** use existing Lettuce/Redis stack. **Does not** require adopting Redisson **distributed locks** (ADR-0039 remains deferred for idempotency/async ownership). | ADR-0044 Decision §1; ADR-0039 scope boundary; tech-stack guardrails |
| **F7-C12** | **Bandwidth parameters:** Reuse `requestsPerMinute` + `burstCapacity` from `RuntimeRateLimitProperties` for both backends (same knobs). | Existing properties |
| **F7-C13** | **Observability:** Retain `docgen.http.rate_limit.denied` on 429. Add a distinct counter (suggested name `docgen.http.rate_limit.backend_unavailable`) incremented on F7-C7 503 path. Alert family may extend `alert-429-surge` docs later; this leaf at minimum exposes the counter for ops. | LR-D3 sibling pattern |
| **F7-C14** | **Startup / honesty:** When `distributed=true`, Redis coordination must be **wired and verified** (not dead config). Docs/runbook/ADR honesty residual for rate-limit row may state “delivered and verified when flag true”; **still** must not claim SSE/locks/Kafka multi-instance complete. `ProductionMultiInstanceGuard` messaging updated accordingly for rate-limit only. | Closes PRR-D01B rate-limit aspirational residual |
| **F7-C15** | **`frontend_ui_in_scope=false`.** No Vue / Playwright / UIUX. | Handoff |
| **F7-C16** | **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; architecture review; Stage 10 queued `docker-deploy-queue` evidence for acceptance surface. E2E/UIUX **N/A**. | Delivery constitution; FE out of scope |
| **F7-C17** | **Completion boundary / vetoes:** F7 Done ≠ multi-instance complete ≠ IBL/CE/go-live Done; do not flip #3b/#5a; do not activate #119 / CE-O02; do not mark #53 Done. | Hard vetoes |

### 4.1 Explicitly non-goals (confirmed OUT)

- IP / anonymous throttling
- Management login / management API rate-limit
- Gateway-as-only enforcement (optional future complement)
- Redisson lock rollout (ADR-0039)
- Redis SSE pub/sub registry / sticky-session elimination
- Kafka multi-instance ownership changes
- Auto-enabling `distributed=true` on Redis health alone

---

## 5. Trigger / preconditions / journey

### 5.1 Trigger

A client issues an HTTP request to a runtime path `/api/{env}/v1/...` while runtime rate-limit is **enabled**.

### 5.2 Preconditions

| Mode | Preconditions |
| --- | --- |
| Process-local | `enabled=true`, `distributed=false` (default); no Redis required for limiter |
| Distributed | `enabled=true`, `distributed=true`; Redis reachable for Bucket4j coordination; same RPM/burst config as process-local |
| Disabled | `enabled=false` → filter does not run |

### 5.3 Primary journey (distributed=true, happy path)

1. Request hits `RuntimeRateLimitFilter` on `/api/{env}/v1/...`.
2. Credential headers present → build key `credentialId:accessAccount`.
3. Filter asks the **Redis-backed** rate-limit service to try-consume one token for that key.
4. If consumed → continue filter chain (auth / generation as today).
5. If not consumed → **429** + envelope + `Retry-After` + audit + denied metric (F7-C8).

### 5.4 System responses (success / throttle / outage)

| Condition | HTTP | Code | Notes |
| --- | --- | --- | --- |
| Token available | continue chain | — | Unchanged downstream |
| Quota exhausted | **429** | `RATE_LIMIT_EXCEEDED` | Unchanged envelope |
| Headers missing | pass-through | — | Auth 401 likely |
| Redis down / coordination fail (`distributed=true`) | **503** | `RATE_LIMIT_BACKEND_UNAVAILABLE` | Fail-closed; retryable |
| `enabled=false` | pass (no filter) | — | Unchanged |
| Management path | not filtered | — | Unchanged |

---

## 6. Acceptance scenarios (Given / When / Then)

### BDD-PQH-F7-001 — Process-local parity (distributed=false)

**Given** rate-limit `enabled=true` and `distributed=false`  
**And** a single backend instance with RPM/burst configured  
**When** a caller with valid credential headers exceeds the burst/refill quota on `/api/{env}/v1/...`  
**Then** the response is HTTP **429** with `RATE_LIMIT_EXCEEDED`, `Retry-After`, audit, and denied metric  
**And** behavior matches today’s process-local Bucket4j semantics (no Redis required)

### BDD-PQH-F7-002 — Shared quota across two instances (distributed=true)

**Given** `enabled=true` and `distributed=true` with Redis healthy  
**And** two backend instances A and B sharing the same Redis coordination backend and the same RPM/burst config  
**And** bucket key `cred-1:acct-1` with burst capacity **N**  
**When** instance A and instance B together attempt more than **N** successful consumes for that key without refill  
**Then** the combined successes across A+B do **not** exceed **N** (beyond normal token-bucket refill timing)  
**And** subsequent attempts receive **429** `RATE_LIMIT_EXCEEDED` (not independent per-process full quotas)

### BDD-PQH-F7-003 — Missing credential headers pass-through

**Given** rate-limit enabled (either backend)  
**When** a runtime `/api/{env}/v1/...` request omits credential id and/or access account headers  
**Then** the rate-limit filter does **not** reject with 429/503 for rate-limit reasons  
**And** the request continues to the auth layer (fail-closed **401** when credentials invalid/absent)  
**And** no IP-based bucket is created

### BDD-PQH-F7-004 — Management paths excluded

**Given** rate-limit enabled  
**When** a request targets `/api/management/...`  
**Then** `RuntimeRateLimitFilter` does not apply (no runtime rate-limit consume)

### BDD-PQH-F7-005 — Disabled master switch

**Given** `docgen.runtime.rate-limit.enabled=false`  
**When** any runtime request arrives  
**Then** the rate-limit filter does not consume tokens (pass through regardless of `distributed`)

### BDD-PQH-F7-006 — Redis unavailable fail-closed (distributed=true)

**Given** `enabled=true` and `distributed=true`  
**And** Redis is unreachable / Bucket4j proxy cannot coordinate  
**When** a runtime request with valid credential headers arrives  
**Then** the system responds HTTP **503** with `error.code=RATE_LIMIT_BACKEND_UNAVAILABLE`, `category=RUNTIME`, `retryable=true`, messageKey `api.error.runtime.rateLimitBackendUnavailable`  
**And** does **not** silently grant unlimited traffic via process-local fallback  
**And** does **not** return **429** `RATE_LIMIT_EXCEEDED` for this outage  
**And** the backend-unavailable metric increments

### BDD-PQH-F7-007 — 429 contract + Retry-After stable

**Given** either backend mode with tokens exhausted for a key  
**When** the filter denies the request for quota  
**Then** HTTP **429**  
**And** envelope `error.code=RATE_LIMIT_EXCEEDED`, `category=RUNTIME`, `retryable=true`, `messageKey=api.error.runtime.rateLimitExceeded`  
**And** `Retry-After` is present and ≥ 1 (seconds)  
**And** audit event `API_RATE_LIMIT_DENIED` is recorded  
**And** `docgen.http.rate_limit.denied` increments

### BDD-PQH-F7-008 — Distributed default remains false

**Given** claimed-prod / default configuration without operator override  
**When** the application starts  
**Then** `docgen.runtime.rate-limit.distributed` resolves to **`false`** unless `RUNTIME_RATE_LIMIT_DISTRIBUTED` is explicitly set true  
**And** process-local Bucket4j remains the default authority

### BDD-PQH-F7-009 — Under-quota allow (distributed=true)

**Given** `distributed=true`, Redis healthy, tokens available for the key  
**When** a runtime request with credential headers arrives  
**Then** the filter allows the request through (no 429/503 from rate-limit)

### BDD-PQH-F7-010 — Single-instance distributed mode still correct

**Given** `distributed=true`, Redis healthy, **one** backend instance  
**When** the caller exceeds burst/refill  
**Then** **429** `RATE_LIMIT_EXCEEDED` as in F7-007  
**And** Redis-backed storage is used (not merely process-local map)

### BDD-PQH-F7-011 — Honesty residual closed for rate-limit row only

**Given** this leaf’s implementation is verified (gates + queued deploy evidence)  
**When** operators enable `RUNTIME_RATE_LIMIT_DISTRIBUTED=true` with healthy Redis  
**Then** docs/ADR honesty may state the **shared runtime rate-limit** prerequisite is **delivered and verified**  
**And** docs **must not** claim SSE multi-pod complete, Redisson locks complete, Kafka multi-instance complete, or overall multi-instance correctness complete

### BDD-PQH-F7-012 — No FE surface

**Given** this leaf’s scope  
**When** delivery completes  
**Then** no management UI string/route/Playwright obligation is required for Done  
**And** `frontend_ui_in_scope=false`

---

## 7. Boundary / exception behavior

| Case | Expected |
| --- | --- |
| Blank-trimmed headers after trim empty | Treat as missing → pass-through (F7-C2) |
| Redis timeout / connection refused / proxy error (`distributed=true`) | 503 `RATE_LIMIT_BACKEND_UNAVAILABLE` (F7-C7) |
| Redis recovers | Subsequent requests resume shared consume; no sticky fail-open cache required beyond normal client retry |
| Config `distributed=true` but Redis bean missing / miswired | Fail-closed at runtime for consume attempts (prefer startup fail-fast if detectable — implementation may choose startup fail-fast **or** per-request 503; must not silently run process-local under the distributed flag) |
| Concurrent consumes same key | Token-bucket atomicity via Redis coordination |
| Category / i18n | English-first messageKey; no stack traces / Redis exception class names in `error.message` |

---

## 8. Observable evidence

| Evidence | How proven |
| --- | --- |
| Unit/IT: process-local parity | Extend `RuntimeRateLimitServiceTest` / filter tests |
| Unit/IT: Redis down → 503 code | New tests for distributed backend failure mapping |
| Unit/IT or Testcontainers: shared quota across two logical clients/proxies | Prove F7-002 without requiring full dual-JVM in unit layer if proxy mock/Testcontainers Redis suffices; dual-instance smoke optional in deploy evidence |
| Metric | `docgen.http.rate_limit.denied` on 429; backend-unavailable counter on 503 |
| Audit | `API_RATE_LIMIT_DENIED` on 429 only (not on 503 outage unless implementation records a distinct ops audit — **not required** for 503 in this leaf) |
| Deploy | Stage 10 queued docker-deploy evidence; healthz OK |
| Docs | ADR-0044 honesty residual updated for rate-limit row; OpenAPI/contract-outline note for new 503 code |

---

## 9. Traceability

| Item | Link |
| --- | --- |
| Task Master | **#163** PQH-F7 |
| PQH program | [post-queue-hardening-program-2026-07.md](../plan/post-queue-hardening-program-2026-07.md) F7 |
| IBL | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) **F7** / **Q1** |
| ADR-0044 topology | prerequisite row **#3** shared limiter |
| ADR-0044 multi-instance baseline | Decision §1 Redis rate-limit target + honesty residual |
| ADR-0039 | Redisson **locks** remain deferred (orthogonal) |
| ADR-0031 | Redis centralized counters baseline; LR-B7 pass-through alignment |
| Code | `RuntimeRateLimitFilter`, `RuntimeRateLimitService`, `RuntimeRateLimitProperties`, `RateLimitDeniedMetrics` |
| Prior honesty | [prod-ops-security-hardening.md](./prod-ops-security-hardening.md) D01B-C9 |

---

## 10. BDD readiness

| Field | Value |
| --- | --- |
| **bdd_readiness** | **`ready`** |
| **open_questions_remaining** | **[]** (all six product questions locked in §4) |
| **frontend_ui_in_scope** | `false` |
| **backend_api_contract_change** | `true` (new 503 `RATE_LIMIT_BACKEND_UNAVAILABLE`; **429 envelope unchanged**) |
| **scenario_ids** | `BDD-PQH-F7-001` … `BDD-PQH-F7-012` |
| **Next stage** | `plan-orchestrator` → backend TDD / implementers (no production code in this stage) |

### Confirmed decisions summary (handoff return shape)

```text
confirmed_decisions:
  bucket_key: "credentialId:accessAccount only; no IP fallback; missing headers pass-through"
  redis_down: "fail-closed HTTP 503 RATE_LIMIT_BACKEND_UNAVAILABLE retryable=true; no process-local silent fallback; not 429"
  distributed_default: "false (opt-in via RUNTIME_RATE_LIMIT_DISTRIBUTED); enable is scale-out switch"
  scope: "runtime /api/{env}/v1/* filter only; not management UI/login"
  frontend_ui_in_scope: false
```
