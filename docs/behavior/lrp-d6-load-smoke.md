# LR-D6 — Load smoke baseline (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `lrp-d6-load-smoke` |
| **Plan** | [LRP-D §LR-D6](../plan/detail/LRP-D-ops-observability.md#lr-d6--load-smoke-baseline) |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-12 |
| **Formal phase** | None (Wave LR-D In Progress) |

---

## Why BDD is not-applicable

LR-D6 delivers a **measurement harness + dated evidence**, not a product behavior change:

- No new user-facing journey, API contract, permission rule, or audit semantics.
- No change to sync-generation or SSE preview **response contracts** — the harness exercises existing runtime APIs against the Docker acceptance stack.
- Outcomes are **recorded baselines** (latency, error rate, stream completion), not new acceptance thresholds promoted into confirmed NFRs (those stay in LR-D5 **pending** proposals).
- Tooling default remains a **flag-gated JUnit / `ExecutorService` harness** (no new dependency). k6 or any APM vendor is **out of scope** unless separately verified under tech-stack guardrails — this note does **not** invent a tool or vendor choice.

Plan authority: [LRP-D-ops-observability.md](../plan/detail/LRP-D-ops-observability.md) §LR-D6 — **BDD: not-applicable — measurement harness + evidence; no behavior change.**

---

## What is measured (scope)

| Scenario | Load | Recorded signals |
| --- | --- | --- |
| **A — Concurrent sync generation** | ≥20 concurrent sync generations, mixed DOCX/PDF, against Docker stack post–LR-A1 | p95 / p99 latency, error rate, PDF conversion pool rejections |
| **B — Preview + SSE under concurrency** | ≥5 parallel previews with progress streams | Every stream receives its terminal event (no silent drops) |

**Environment constraints (from plan):**

- Run against the **local Docker acceptance stack only** — never shared/production.
- Prefer post–**LR-A1** (profile isolation) + **LR-B3** (SSE hardening) so results reflect the hardened system.
- Do **not** tune thresholds to pass; record observed reality.
- Failures that look like LR-A1/B3 regressions are **filed against those tasks**, not patched inside D6.
- Normal `mvn verify` must remain unaffected (harness behind a system-property flag).

**Feeds (downstream, not confirmed here):** LR-D5 NFR pending proposals; LR-D3 draft alert thresholds.

---

## Acceptance scenarios (plan §LR-D6 G/W/T)

These are **measurement acceptance** criteria for the harness run + evidence record — not product BDD Given/When/Then for TDD Red of new behavior.

### Scenario A — Concurrent sync generations

- **Given** the Docker stack (post–LR-A1 baseline preferred)
- **When** 20 concurrent sync generations run (mixed DOCX/PDF)
- **Then** error rate is 0 (or every failure is triaged to a named defect), and p95 latency is recorded (with date, stack version, hardware note)

### Scenario B — Parallel SSE preview streams

- **Given** ≥5 parallel SSE preview streams under concurrency
- **When** the batch completes
- **Then** all streams received their terminal event (no silent drops)

---

## Explicit non-goals

- No product requirement inventing SLO/SLA numbers as **confirmed**.
- No APM / tracing vendor selection.
- No k6 (or other new load-tool) adoption without dependency-policy verification.
- No marking LR-D6 **Done** in this readiness record alone — Done requires harness + both scenarios measured + evidence + doc sync + commit review (plan §LR-D6).

---

## How to run the harness

Flag-gated JUnit harness (no k6). Details:
[backend loadsmoke README](../../backend/src/test/java/com/bank/docgen/runtime/loadsmoke/README.md).

```powershell
# Prerequisites: Docker :8080 healthy + FOL published credential at
# .tmp/credentials/CORP-FOL-OFFER.json ; for Scenario B raise preview max-concurrent ≥5.
mvn -B -ntp -f backend/pom.xml -Pdev-fast test `
  -Dtest=LoadSmokeDockerHarnessTest `
  -Ddocgen.loadSmoke=true
```

Evidence directory: [docs/plan/evidence/lrp-d6-load-smoke/](../plan/evidence/lrp-d6-load-smoke/).

System property flag: **`docgen.loadSmoke=true`** (unset → never runs in normal `mvn verify`).

---

## Traceability

| Artifact | Role |
| --- | --- |
| [LRP-D §LR-D6](../plan/detail/LRP-D-ops-observability.md) | Authoritative task row + G/W/T |
| Runtime sync generate OpenAPI | Existing API under test |
| LR-A1 / LR-B3 | Hardened system this smoke validates |
| LR-D5 / LR-D3 | Consumers of recorded baselines (pending / draft only) |

```
bdd_readiness: not-applicable
task_ids: [lrp-d6-load-smoke]
```
