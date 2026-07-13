# CORE-FORTRESS F8 — Observability, SLO, DR, Evidence Bundle (Detailed Plan)

**Program ID:** `CORE-FORTRESS`  
**Phase ID:** `CORE-FORTRESS-F8-OBSERVABILITY-SLO-DR`  
**Phase status:** **Done** (2026-07-09)  
**Depends on:** CORE-FORTRESS F1–F7 (**all Done** — F8 is final phase; F6∥F7 must complete before F8-T02+)  
**BDD:** `docs/behavior/core-fortress-f8-observability-slo-dr.md` — **ready** (`BDD-CORE-FORTRESS-F8-001`)

> **Single-active-phase invariant:** **F8** sole formal `In Progress` (2026-07-09). F1–F7 **Done**.

> **Program position:** F8 closes CORE-FORTRESS — rendering → runtime → production → async → frontend → **ops evidence**. Delivers an **achievable slice** of LR-D2/LR-D3 without blocking on full observability stack deployment.

---

## 1. North star

**Operators can measure generation SLOs, diagnose partial dependency outages via deep readiness, follow a DR playbook aligned with ADR-0030, and ship releases with a reproducible evidence bundle** — all without changing SOR-O06 Postgres-only traffic gating.

---

## 2. Scope (in) / out (out)

| In scope (F8) | Out of scope (LR-D / later) |
| --- | --- |
| F8-B1: `docgen.generation.*` + `docgen.pdf.conversion.outcome` Micrometer series | Full SSE/DLT/429 dashboard panels (LR-D3 remainder) |
| F8-B1: Update `deploy/observability/prometheus-alerts.yaml` (draft thresholds) | Prometheus/Grafana/Alertmanager stack deploy |
| F8-B2: Structured `/readyz` JSON with component checks | Readiness gating on Redis/MinIO/Kafka (SOR-O06 preserved) |
| F8-B2: `deploy/k8s-health-probes.md` + runbook readiness table sync | Multi-region HA readiness policy change |
| F8-B3: DR section expansion in `docs/operations/runbook.md` | Standalone `backup-restore-runbook.md` (LR-D2) |
| F8-B3: DR drill evidence **directory convention** + checklist | Executing first annual drill (LR-D2) |
| F8-B4: Evidence bundle script/checklist + `release-gate.ps1` integration | Replacing `p0-gate.ps1` / CI workflow rewrite |
| Backend `mvn verify` + scrape smoke (optional Docker) | Frontend E2E / UIUX |

### Reuse — do NOT re-implement

| Asset | Evidence |
| --- | --- |
| `PdfConversionPoolMetrics` | `docgen.pdf.conversion.pool.*` gauges (F4/SOR-P03) |
| `HealthController` + `ReadinessProbe` | P15-T07c Postgres gate |
| `deploy/observability/README.md` | Draft metric/alert catalog |
| `scripts/release-gate.ps1` | v1 gate + `artifacts/release-gate/` |
| `docs/operations/runbook.md` | Backup checklist stub § Backup and restore |
| ADR-0030 | RPO/RTO, dual health endpoints |

---

## 3. Exit criteria

1. **B1:** Generation + conversion SLO metrics registered; BDD-F8-B1-001…005 green; Prometheus sample shows series.
2. **B2:** `/readyz` returns structured checks; Postgres-down → 503; Postgres-up + Redis-down → 200; BDD-F8-B2-001…005 green.
3. **B3:** Runbook DR chapter complete with ADR-0030 + blue-green + Flyway links; BDD-F8-B3-001…003 satisfied by doc review.
4. **B4:** Evidence bundle produces `artifacts/core-fortress-evidence/<timestamp>/` with summary + checklist; BDD-F8-B4-001…005 green.
5. **Gates:** `mvn -B -ntp -f backend/pom.xml verify` — **GREEN**.
6. **Doc sync:** program roadmap F8 Done; ledger; behavior index — F8-T11.

---

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **F8-T01** | behavior-spec-author | **BDD behavior spec** — `core-fortress-f8-observability-slo-dr.md` + this plan | — | **Done** (2026-07-09; readiness `ready`) |
| **F8-T02** | backend-engineer | **B1 metrics instrumentation** — `GenerationMetrics` / extend conversion path: `docgen.generation.duration`, `docgen.pdf.conversion.duration`, `docgen.pdf.conversion.outcome`; wire in `DocumentGenerationEngine` + PDF service | F8-T01, F1–F7 Done | **Done** (2026-07-09) |
| **F8-T03** | backend-engineer | **B1 metric tests** — `GenerationMetricsTest`, `PdfConversionMetricsTest`; optional `@SpringBootTest` scrape smoke (`MetricsScrapeSmokeTest`) | F8-T02 | **Done** (2026-07-09) |
| **F8-T04** | backend-engineer | **B2 readiness depth** — `ReadinessProbe` → `ReadinessReport` with `checks` map; optional Redis/MinIO/Kafka probes (`@Profile("!test")`); preserve 503 only on Postgres failure | F8-T01, F1–F7 Done | **Done** (2026-07-09) |
| **F8-T05** | backend-engineer | **B2 readiness tests + probe docs** — `ReadinessProbeTest` / MVC test BDD-F8-B2-*; update `deploy/k8s-health-probes.md`, `docs/operations/runbook.md` § Readiness scope | F8-T04 | **Done** (2026-07-09) |
| **F8-T06** | backend-engineer + deploy-engineer | **B1 alerting as code** — sync `deploy/observability/prometheus-alerts.yaml` + README with implemented metric names; `draft: true` annotations; promtool lint or documented manual validation | F8-T02 | **Done** (2026-07-09) |
| **F8-T07** | doc-keeper + deploy-engineer | **B3 DR runbook section** — expand `docs/operations/runbook.md` § Disaster Recovery: backup cadence, restore steps, smoke, RPO/RTO template, `artifacts/dr-drill/` convention; index from `deploy/README.md` | F8-T01 | **Done** (2026-07-09) |
| **F8-T08** | build-deploy-agent | **B4 evidence bundle** — `scripts/core-fortress-evidence-bundle.ps1` + checklist (`docs/operations/core-fortress-release-checklist.md`); integrate with `release-gate.ps1` (`-EvidenceBundle` or post-step); git SHA + health/metrics snapshots | F8-T02, F8-T04 | **Done** (2026-07-09) |
| **F8-T09** | architecture-reviewer | **Boundary review** — SOR-O06 preserved; no secrets in evidence bundle; metrics cardinality bounded; fail-closed readiness JSON schema stable | F8-T02–T08 | **Done** (2026-07-09; PASS) |
| **F8-T10** | build-deploy-agent | **Gate evidence** — full `mvn verify`; optional Docker scrape smoke; record counts in ledger | F8-T03–T08 | **Done** (2026-07-09) |
| **F8-T11** | post-task-doc-sync | **Closeout** — F8 Done; roadmap; master-plan; `docs/README.md` behavior index; CORE-FORTRESS program **Done** | F8-T09–T10 | **Done** (2026-07-09) |

**Task count:** **11** (F8-T01 … F8-T11)

---

## 5. Recommended wave order

```text
Wave 0 — BDD + plan (Done)
  F8-T01

Wave 1 — Core instrumentation (after F1–F7 Done)
  F8-T02 (generation + conversion metrics)
  F8-T04 (readiness depth) — parallel OK

Wave 2 — Tests + ops artifacts
  F8-T03 (metric unit tests)
  F8-T05 (readiness tests + probe docs)
  F8-T06 (alert rules sync)
  F8-T07 (DR runbook) — parallel with T03–T06

Wave 3 — Evidence + review + closeout
  F8-T08 (evidence bundle — needs T02+T04)
  F8-T09 (architecture review)
  F8-T10 (full gate)
  F8-T11 (doc sync — marks CORE-FORTRESS program Done)
```

**Dependency gate:** Do **not** start F8-T02 until roadmap shows **F6 Done AND F7 Done** (F5 must also be Done — async metrics tags optional but program order requires it).

---

## 6. File map (target)

| File | Purpose |
| --- | --- |
| `backend/.../runtime/GenerationMetrics.java` (or equivalent) | Generation timer binder |
| `backend/.../rendering/PdfConversionOutcomeMetrics.java` | Success/failure counter |
| `backend/.../sharedkernel/health/ReadinessReport.java` | Structured checks DTO |
| `backend/.../sharedkernel/health/ReadinessProbe.java` | Extended probes |
| `backend/src/test/java/.../GenerationMetricsTest.java` | BDD-F8-B1-005 |
| `backend/src/test/java/.../ReadinessProbeTest.java` | BDD-F8-B2-* |
| `deploy/observability/prometheus-alerts.yaml` | Draft SLO alerts |
| `docs/operations/runbook.md` | DR section expansion |
| `docs/operations/core-fortress-release-checklist.md` | Release checklist |
| `scripts/core-fortress-evidence-bundle.ps1` | Evidence collector |
| `scripts/release-gate.ps1` | Integration hook |

---

## 7. Gate commands

| Context | Command |
| --- | --- |
| TDD inner loop | `mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=GenerationMetricsTest,ReadinessProbeTest` |
| Full backend gate | `mvn -B -ntp -f backend/pom.xml verify` |
| Metrics scrape smoke (Docker up) | `curl -sf http://localhost:8080/actuator/prometheus \| findstr docgen` |
| Evidence bundle | `.\scripts\core-fortress-evidence-bundle.ps1` or `.\scripts\release-gate.ps1 -EvidenceBundle` |
| Alert rule lint (if promtool installed) | `promtool check rules deploy/observability/prometheus-alerts.yaml` |

---

## 8. SLO target reference (draft — LR-D5 proposals pending confirmation)

| SLI | Metric | Draft threshold | Alert severity |
| --- | --- | --- | --- |
| Sync generation latency p95 | `docgen.generation.duration` | ≤ 3s (stale pre-measurement NFR proposal — **not supported** by LR-D6 FOL concurrent smoke) | warning |
| End-to-end generation p95 (incl. PDF) | `docgen.generation.duration` | ≤ 10s (observability README draft — **not supported** by LR-D6 measured p95≈15939ms) | warning |
| PDF conversion failure rate | `rate(docgen.pdf.conversion.outcome{result="failure"}[5m])` | > 0.17/s (~10/min) | critical |
| PDF conversion latency p95 | `docgen.pdf.conversion.duration` | ≤ 30s | warning |

> **Not SLA until LR-D5 confirmed.** Rules carry `draft: true` in annotations.
> **LR-D5 (2026-07-12):** Authoritative **pending** proposals (measured vs pre-measurement, launch-gate flags) live in
> [non-functional-requirements.md §待确认 LR-D5](../../requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation).
> Thresholds in this table remain **draft** pending user confirmation of those proposals; do not remove `draft: true` or promote ≤3s/≤10s from smoke alone.

---

## 9. LRP / program cross-links

| Program row | F8 relationship |
| --- | --- |
| LR-D2 Backup/restore runbook | F8-B3 delivers runbook **section**; LR-D2 adds standalone doc + drill execution |
| LR-D3 Metrics as code | F8-B1 implements **core generation/conversion** series; LR-D3 adds SSE/DLT/429 |
| LR-D5 NFR quantification | Threshold source; confirmation unblocks removing `draft` from alerts |
| LR-E2 Launch checklist | F8-B4 evidence bundle feeds checklist item «release gate evidence» |
| CORE-FORTRESS F1–F7 | **Hard dependency** — F8 closes program |

---

## 10. Risk register

| Risk | Mitigation |
| --- | --- |
| Metric cardinality explosion from templateId tags | **Forbidden** — use outcome/format/mode only |
| Readiness probe latency on MinIO/Kafka | Short timeouts (≤ 2s); async parallel probes |
| Evidence bundle leaks secrets | Snapshot URLs only; no JWT/credentials in artifacts |
| F6/F7 slip blocks F8 | F8-T01 Done early; execution gated on roadmap status |
| Windows CI `mvn verify` flake | Record env caveat in ledger (F4 precedent) |

---

## 11. Doc sync checklist (F8-T11)

- [x] `CORE-FORTRESS-program-roadmap.md` — F8 → **Done**; program banner **CORE-FORTRESS Done**
- [x] `docs/plan/master-plan.md` — CORE-FORTRESS status mirror
- [x] `docs/plan/execution-sync-ledger.md` — gate evidence + metric series list
- [x] `docs/README.md` — behavior spec index entry
- [x] `deploy/README.md` — link evidence bundle + observability updates
- [x] LR-D2/D3 rows — note F8 partial delivery (avoid duplicate Done claims)
