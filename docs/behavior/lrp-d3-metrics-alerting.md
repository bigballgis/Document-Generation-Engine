# LR-D3 — Metrics & alerting as code (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `lrp-d3-metrics-alerting` |
| **Plan** | [LRP-D §LR-D3](../plan/detail/LRP-D-ops-observability.md#lr-d3--metrics--alerting-as-code) |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-12 |
| **Formal phase** | None (Wave LR-D In Progress) |
| **Task Master** | **#40** (in-progress) · plan id **LR-D3** |

---

## Why BDD is not-applicable

LR-D3 delivers **observability instrumentation + alert/dashboard as code**, not a product behavior change:

- No new user-facing journey, management UI surface, API contract, permission rule, or audit semantics.
- No change to generation, authoring, or runtime **response contracts** — Micrometer series and Prometheus scrape expose existing operational signals.
- Alert rule thresholds consume **LR-D6 measured baselines** and **LR-D5 NFR proposals** as **draft only** — never confirmed SLOs or product acceptance bars.
- Preferred deliverables are **metric instrumentation + tests**, `deploy/observability/` YAML/JSON, runbook annotations, and scrape/rule-lint evidence — not product BDD.

Plan authority: [LRP-D-ops-observability.md](../plan/detail/LRP-D-ops-observability.md) §LR-D3 — **BDD: not-applicable — observability instrumentation; no user-facing behavior.**

Program row: [launch-readiness-program.md](../plan/launch-readiness-program.md) § wave map — LR-D3 BDD column **`not-applicable`**.

---

## What is in scope (ops / observability only)

| Deliverable | Intent |
| --- | --- |
| **Custom Micrometer metrics** | Generation latency (sync, with/without PDF); PDF conversion pool queue depth + rejections; active SSE connections; 429 count; async DLT depth (when Kafka active) |
| **Alert rules as code** | `deploy/observability/` Prometheus alert rules YAML — backend down, p95 breach vs **draft** threshold, pool rejections > 0, DLT depth > 0, 429 surge; each rule `runbook` annotation → `docs/operations/runbook.md` section |
| **Dashboard as code** | Grafana dashboard JSON — generation, conversion pool, SSE, rate-limit panels |
| **Validation** | Unit-level metric register/increment tests; Docker scrape smoke (`/actuator/prometheus` non-zero samples); promtool (or documented equivalent) rule lint |

**Environment / policy constraints (from plan):**

- Do **not** add a vendor APM dependency.
- Do **not** alert on unmeasured thresholds — use LR-D6 baselines or mark rules as **draft**.
- Do **not** bake credentials into dashboards.
- Leave **LR-D4 Not Started**; do **not** activate LR-E / CD-3; do **not** touch `DGE-audit-governance`.

**Upstream inputs (draft thresholds only — not confirmed SLOs):**

- **LR-D6** Done — Scenario A p95≈15939ms / p99≈16065ms / errorRate=0.4 (DEF-LRP-D6-001 triaged); Scenario B 5/5 SSE dropped=0; evidence under `docs/plan/evidence/lrp-d6-load-smoke/`.
- **LR-D5** Done — NFR §待确认 proposals «proposed — awaiting confirmation»; feed draft alert annotations only.

---

## Acceptance scenarios (plan §LR-D3 G/W/T)

These are **ops / instrumentation acceptance** criteria for metrics scrape + alert-rules validation — **not** product BDD Given/When/Then for TDD Red of new user-facing behavior. No product actor journeys are invented here.

### Scenario A — Prometheus scrape shows new series

- **Given** the Docker stack
- **When** one PDF generation and one 429 occur
- **Then** `/actuator/prometheus` exposes the new counters/timers with non-zero samples

### Scenario B — Alert rules validate and link runbooks

- **Given** the alert rules file under `deploy/observability/`
- **When** validated with promtool (or documented equivalent)
- **Then** rules parse and every rule links a runbook section

---

## Explicit non-goals

- No product requirement inventing metrics/alerts as a **management-UI** or **runtime API** feature.
- No promoting draft alert thresholds into **confirmed** SLOs / NFRs (that stays LR-D5 pending + user confirmation).
- No vendor APM; no credentials in dashboard JSON.
- No activating **LR-D4** / LR-E / CD-3 from this readiness record.
- No marking LR-D3 **Done** in this readiness record alone — Done requires series visible + rules/dashboards committed + scrape/rule-lint evidence + doc sync + commit review (plan §LR-D3).

---

## Traceability

| Artifact | Role |
| --- | --- |
| [LRP-D §LR-D3](../plan/detail/LRP-D-ops-observability.md) | Authoritative task row + G/W/T |
| Micrometer/actuator (`application.yml` management); P9 observability | Existing instrumentation baseline |
| `deploy/helm/docgen/` monitoring hooks (P15-T05b) | Helm metric / NetworkPolicy hooks |
| LR-B3 / LR-B4 | SSE + DLT series sources |
| [LR-D6 load smoke](./lrp-d6-load-smoke.md) + evidence | Measured baselines for **draft** thresholds |
| [NFR §待确认 LR-D5](../requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation) | Proposed values only — not confirmed SLOs |
| [Runbook alert sections](../operations/runbook.md#observability) | Ops response + draft threshold table; anchors for Prometheus `runbook` annotations |
| [deploy/observability/README.md](../../deploy/observability/README.md) | As-code folder index + draft threshold summary for implementers |
| Program §1 finding 12 | Ops runbook / alert-as-code gap |

```
bdd_readiness: not-applicable
task_ids: [LR-D3 / lrp-d3-metrics-alerting, Task Master #40]
```
