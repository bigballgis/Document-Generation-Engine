# CORE-FORTRESS F5 — Async Durability + Security Depth (Detailed Plan)

**Program ID:** `CORE-FORTRESS`  
**Phase ID:** `CORE-FORTRESS-F5-ASYNC-DURABILITY-SECURITY-DEPTH`  
**Phase status:** **Done** (2026-07-09)  
**Depends on:** CORE-FORTRESS F1–F4 (**Done**), P11 (**Done**), P10 (**Done**), LR-B2 ShedLock (**Done**), LR-B5 graceful shutdown (**Done**)  
**BDD:** `docs/behavior/core-fortress-f5-async-durability-security.md` — **ready** (`BDD-CORE-FORTRESS-F5-001`)

> **Single-active-phase invariant:** **F6** sole formal `In Progress` (2026-07-09). F1–F5 **Done**.

---

## 1. North star

**Async batch generation survives worker crashes and Kafka at-least-once delivery without duplicate side effects or stuck PROCESSING tasks**, while **audit and invocation persistence contain no variable plaintext** per ADR-0020. Runtime 429 denials and credential rotation events are **auditor-traceable**.

**Not in scope:** distributed rate limiting, frontend, full-platform security audit, Kafka prod topology change (ADR-0044 branch (b) remains accepted for v1).

---

## 2. Scope (in) / out (out)

| In scope (F5) | Out of scope (Done elsewhere or later) |
| --- | --- |
| F5-B1: Stale `PROCESSING` detection + reclaim scheduler + `isTerminalStatus` fix | F2 idempotency release cache (**Done**) |
| F5-B2: Kafka consumer idempotency + DLT integration test | LR-B4 Kafka prod compose branch (a) — **deferred** |
| F5-B3: Payload scrub on terminal; invocation `variablesHash`; safe error summaries | Full request encryption at rest |
| F5-B4: 429 runtime audit event; credential rotation generation fingerprint | Redis Bucket4j (**ADR-0044** scale-out) |
| P10 download regression test for batch-async artifacts | Frontend / E2E / UIUX |
| Flyway + config + `mvn verify` | F8 DLT depth metrics / alerting dashboards |

### Codebase gaps driving F5 (investigation 2026-07-09)

| Location | Gap |
| --- | --- |
| `AsyncBatchTaskRunner.isTerminalStatus()` L105–111 | Treats `PROCESSING` as terminal → **blocks crash recovery** on Kafka redelivery |
| `GenerationAsyncTaskEntity.request_payload_json` | Stores **full batch request including variables** indefinitely |
| `InvocationParameterSanitizer` L29–37, L76 | Persists **raw `variables`** to invocation records (ADR-0020 conflict) |
| `AsyncBatchTaskRunner.summarizeFailure()` L197–199 | May leak sensitive text via `Exception.getMessage()` into audit |
| `RuntimeRateLimitFilter` | 429 envelope only — **no runtime audit row** |
| `AsyncBatchTaskKafkaConsumerTest` | Happy path only — **no DLT test** |
| Stale reclaim | **No scheduler** — stuck PROCESSING until manual DB fix |

---

## 3. Exit criteria

1. **B1:** Stale threshold configurable; scheduler reclaims and re-dispatches; attempt cap → FAILED; unit + integration tests green.
2. **B2:** DLT integration test proves message lands on DLT after retry exhaustion; terminal-task consumer idempotency test green.
3. **B3:** Terminal async tasks scrubbed; invocation records use hashes; audit failure summaries safe — regression tests assert no plaintext vars.
4. **B4:** 429 writes `API_RATE_LIMIT_DENIED` audit; rotation audit includes `rotationGeneration` + `previousCredentialFingerprint`.
5. **Green gates:** `mvn -B -ntp -f backend/pom.xml verify`.
6. **Doc sync:** program roadmap, master-plan, ledger, behavior index — at F5 closeout (F5-T09).

---

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **F5-T01** | behavior-spec-author | **BDD behavior spec** + this plan | — | **Done** (2026-07-09; readiness `ready`) |
| **F5-T02** | backend-engineer | **B1 crash recovery** — Flyway `processing_attempt_count`; fix `isTerminalStatus`; `DocgenAsyncProperties` stale threshold; `AsyncBatchTaskStaleReclaimScheduler` (ShedLock); reclaim + re-dispatch; tests BDD-F5-B1-* | F5-T01 | **Done** (2026-07-09; V51) |
| **F5-T03** | backend-engineer | **B2 Kafka DLT + idempotency** — ensure exceptions propagate for retry; `AsyncBatchTaskKafkaDltIntegrationTest`; terminal skip test BDD-F5-B2-*; optional non-retryable classifier | F5-T01, F5-T02 | **Done** (2026-07-09) |
| **F5-T04** | backend-engineer | **B3 audit depth** — extract/share `VariableHashSupport`; scrub terminal `request_payload_json`; sanitizer hashes; safe `summarizeFailure`; extend `RuntimeGenerationAuditRecorderTest` / `InvocationParameterSanitizerTest` | F5-T01 | **Done** (2026-07-09) |
| **F5-T05** | backend-engineer | **B4 runtime security depth** — `API_RATE_LIMIT_DENIED` audit in filter or recorder; Flyway `rotation_generation` on `api_credential` + audit metadata; tests BDD-F5-B4-* | F5-T01 | **Done** (2026-07-09; V52) |
| **F5-T06** | backend-engineer | **P10 regression** — batch async success → download within window + credential mismatch 403 (`BDD-F5-REG-001`) | F5-T02 | **Done** (2026-07-09) |
| **F5-T07** | architecture-reviewer | **Boundary review** — runtime vs apimgmt vs audit; fail-closed; no variable leakage in logs/errors | F5-T02–T06 | **Done** (2026-07-09; no critical findings) |
| **F5-T08** | build-deploy-agent | **Gate evidence** — `mvn verify`; record test counts in ledger | F5-T02–T06 | **Done** (2026-07-09; F5 targeted **31/31**; full `mvn verify` Windows DOCX file-lock env caveat — not F5 regression) |
| **F5-T09** | post-task-doc-sync | **Closeout** — F5 Done; roadmap; master-plan; docs/README behavior index | F5-T07–T08 | **Done** (2026-07-09) |

**Task count:** **9** (F5-T01 … F5-T09)

---

## 5. Recommended wave order

```text
Wave 0 — BDD + plan (Done)
  F5-T01

Wave 1 — Core durability (TDD; sequential B1 before B2)
  F5-T02 (B1 stale reclaim + isTerminalStatus fix)
  F5-T03 (B2 Kafka DLT + consumer idempotency)

Wave 2 — Security depth (T04/T05 parallel OK after T01)
  F5-T04 (B3 audit scrub + hashes)
  F5-T05 (B4 429 audit + rotation depth)

Wave 3 — Regression + review + closeout
  F5-T06 (P10 download regression)
  F5-T07 → F5-T08 → F5-T09
```

---

## 6. Implementation notes (for backend-engineer)

### F5-T02 — B1

- **Repository:** add `findByStatusInAndUpdatedAtBefore(...)` for stale query.
- **Reclaim transaction:** compare-and-set on `updated_at` or `@Version` to avoid double dispatch.
- **Audit:** new event type or result summary token `STALE_TASK_RECLAIMED` on reclaim (stable string, i18n not required for audit row).
- **Config:** wire `docgen.async.stale-processing-threshold-seconds` (default 900), `docgen.async.stale-reclaim-interval-ms` (default 300000).

### F5-T03 — B2

- Extend `AsyncBatchTaskKafkaConsumerTest` or new `@EmbeddedKafka` test with **real** runner stub throwing on first N invocations.
- Verify DLT topic message count via `KafkaTestUtils` or `@EmbeddedKafka` listener.
- Document retry/backoff in behavior spec §6.3 (already specified).

### F5-T04 — B3

- Scrub hook: call from `markSucceeded` / `markFailed` / `markPartialSucceeded` / `markExpired` paths in runner or entity helper.
- **In-flight exception (F5-C1):** do not scrub until terminal.
- Align `InvocationRecordService` call sites to use updated sanitizer output.

### F5-T05 — B4

- Rate limit audit: inject `RuntimeGenerationAuditRecorder` or thin `RuntimeSecurityAuditRecorder` — **avoid** filter → template DB lookup on hot path if expensive; fingerprint-only row acceptable per spec.
- Credential: increment `rotationGeneration` on `rotateSecret`; store previous external id fingerprint in audit JSON metadata.

---

## 7. Test matrix (TDD Red targets)

| BDD ID | Test class (proposed) |
| --- | --- |
| BDD-F5-B1-001..003 | `AsyncBatchTaskRunnerTest`, `AsyncBatchTaskStaleReclaimSchedulerTest` |
| BDD-F5-B2-001..002 | `AsyncBatchTaskKafkaDltIntegrationTest` |
| BDD-F5-B3-001..003 | `AsyncBatchTaskRunnerTest`, `InvocationParameterSanitizerTest`, `RuntimeGenerationAuditRecorderTest` |
| BDD-F5-B4-001..002 | `RuntimeRateLimitFilterTest`, `ApiManagementServiceTest` |
| BDD-F5-REG-001 | `DocumentDownloadServiceTest` or `TemplatePlatformSliceTest` |

---

## 8. Configuration surface (new)

```yaml
docgen:
  async:
    stale-processing-threshold-seconds: 900
    stale-reclaim-interval-ms: 300000
    # existing: transport, kafka.*
```

---

## 9. LRP / program cross-reference

| LRP / seam | F5 coverage |
| --- | --- |
| LR-B5 graceful shutdown | Complements — in-flight tasks may become stale after kill |
| LR-B2 ShedLock | Reclaim scheduler uses same mutex pattern |
| LR-D3 DLT depth metrics | F5-T03 supplies **test evidence**; alerting in F8 |
| Ledger «Async batch transport» | Unchanged — in-process v1 accepted |
| Ledger «Runtime rate limit» | F5-T05 adds audit depth only |

---

## 10. References

| Doc | Purpose |
| --- | --- |
| [Behavior spec F5](../../behavior/core-fortress-f5-async-durability-security.md) | BDD source |
| [Program roadmap](./CORE-FORTRESS-program-roadmap.md) | F1–F8 status |
| [P11 batch-async](./P11-batch-async.md) | Async API baseline |
| [ADR-0020](../../adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md) | Sensitive data |
| [ADR-0044](../../adr/operations/0044-deployment-topology-v1.md) | v1 topology |
