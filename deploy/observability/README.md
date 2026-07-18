# Metrics & Alerting as Code (LR-D3)

**Status:** Draft — proposed baseline for LR-D3; **not** confirmed SLOs  
**Last updated:** 2026-07-12  
**Slice:** `lrp-d3-metrics-alerting` · Task Master **#40** · BDD **not-applicable**  
**Behavior readiness:** [docs/behavior/lrp-d3-metrics-alerting.md](../../docs/behavior/lrp-d3-metrics-alerting.md)  
**ADR reference:** [ADR-0030 operational platform baseline](../../docs/adr/operations/0030-operational-platform-baseline.md)

## Purpose

Version-control Micrometer scrape expectations, Prometheus alert rules, and Grafana
dashboard JSON so observability is reviewable and reproducible. The application stack
exposes `/actuator/prometheus`; a separate Prometheus/Grafana/Alertmanager deployment
scrapes and evaluates rules. **PRR-D01b:** claimed-prod scrape uses **HTTP Basic**
(env/secrets) — see [runbook § Observability](../../docs/operations/runbook.md#observability)
and comments in `prometheus-scrape.yaml`. **No vendor APM** is adopted or documented as required.

## Folder layout (implementer contract)

| Path | Role | Owner at LR-D3 |
| --- | --- | --- |
| `README.md` (this file) | Index + **draft** threshold table + runbook annotation map | doc-keeper (docs-first) |
| `prometheus-alerts.yaml` | Prometheus rule groups (`draft: "true"` until NFR confirmation) | backend / deploy engineer |
| `prometheus-scrape.yaml` | **Reference-only** example scrape job (`job_name: docgen-backend`) — comments document **HTTP Basic** scrape (PRR-D01b); placeholder `basic_auth` commented; not mounted by compose; **no** committed passwords | backend / deploy engineer |
| `grafana/docgen-ops-overview.json` | Importable dashboard (generation, PDF pool, SSE, rate-limit, DLT) | backend / deploy engineer |
| `grafana/README.md` | Dashboard import notes | backend / deploy engineer |

Backend may add or rename YAML/JSON here; keep this README and
[docs/operations/runbook.md](../../docs/operations/runbook.md) anchors in sync.

## Draft alert thresholds (NOT confirmed SLOs)

> Every threshold is **draft / proposed**. Prefer measured inputs from LR-D6 and pending
> proposals from LR-D5. Do **not** promote into confirmed NFR/SLA language. Do **not**
> invent credentials, Alertmanager secrets, or vendor SaaS endpoints.

| Alert (family) | Draft threshold (proposed) | Source | Runbook annotation target |
| --- | --- | --- | --- |
| Backend down | `/healthz` (or up-metric) failing **≥ 2m** | NFR availability **pre-measurement** | [`#alert-backend-down`](../../docs/operations/runbook.md#alert-backend-down) |
| p95 latency breach | Prefer interim envelope **~16 s** (D6 success-sample p95≈**15939 ms** / p99≈**16065 ms**) **or** keep aspirational ≤3 s / ≤10 s only with explicit `draft: true` + stale-vs-D6 note. **Do not** claim ≤3 s / ≤10 s as supported by FOL concurrent smoke. | [latest-summary.json](../../docs/plan/evidence/lrp-d6-load-smoke/latest-summary.json); [NFR §待确认 LR-D5](../../docs/requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation); [DEF-LRP-D6-001](../../docs/plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) | [`#alert-p95-latency-breach-draft`](../../docs/operations/runbook.md#alert-p95-latency-breach-draft) |
| PDF pool rejections | Rejection signal **> 0** for **≥ 2m** | D6 Scenario A **poolRejections=0** (measured-input) | [`#alert-pdf-pool-rejections`](../../docs/operations/runbook.md#alert-pdf-pool-rejections) |
| Async DLT depth | DLT depth **> 0** for **≥ 5m** when Kafka active | **pre-measurement** (no D6 series) | [`#alert-dlt-depth`](../../docs/operations/runbook.md#alert-dlt-depth) |
| HTTP 429 surge | 429 rate **> 1/s** for **≥ 5m** (placeholder) | **pre-measurement** (no D6 429 rate) | [`#alert-429-surge`](../../docs/operations/runbook.md#alert-429-surge) |
| SSE emitters (supplemental) | Active emitters **> 100** for **≥ 5m** (draft capacity / leak) | LR-B3 SSE lifecycle; **pre-measurement** | [`#alert-sse-emitters`](../../docs/operations/runbook.md#alert-sse-emitters) |

**Related F8 partial:** Existing generation/PDF Micrometer series + early `prometheus-alerts.yaml`
drafts remain; LR-D3 still owns SSE / DLT / 429 series + Grafana panels + runbook-linked rules.
F8 rules that still cite ≤3 s / ≤10 s without a D6 caveat are **stale** relative to FOL concurrent
smoke — revise with this table (see NFR confirmation gate).

## Required `runbook` annotation

Every firing rule in `prometheus-alerts.yaml` (and Helm `PrometheusRule` mirrors, if any)
must include:

```yaml
annotations:
  runbook: "docs/operations/runbook.md#<anchor>"
  draft: "true"
  nfr_reference: "LR-D5 pending proposal — not SLA until confirmed"
```

Replace `<anchor>` with one of: `alert-backend-down`, `alert-p95-latency-breach-draft`,
`alert-pdf-pool-rejections`, `alert-dlt-depth`, `alert-429-surge`, `alert-sse-emitters`
(plus supplemental PDF-failure may share `#alert-pdf-pool-rejections`).

## Metrics (Micrometer → Prometheus) — series names

Backend exposes `/actuator/prometheus`. LR-D3 acceptance expects non-zero samples after
one PDF generation and one 429 (plus DLT/SSE when those paths are active):

| Micrometer name | Prometheus series | Intent | Alert family |
| --- | --- | --- | --- |
| `docgen.generation.duration` | `docgen_generation_duration_seconds_*` | Sync generation latency (`mode`, `format=pdf\|docx`, `outcome`) | p95 breach (draft ~16s) |
| `docgen.pdf.conversion.duration` / `.outcome` | `docgen_pdf_conversion_*` | Conversion latency + outcomes | PDF failure (supplemental) |
| `docgen.pdf.conversion.pool.*` | `docgen_pdf_conversion_pool_*` | Active / queue / remaining + **`rejections`** | pool rejections |
| `docgen.sse.emitters.active` | `docgen_sse_emitters_active` | Active SSE connections (preview+batch) | capacity / leak |
| `docgen.http.rate_limit.denied` | `docgen_http_rate_limit_denied_total` | Runtime 429 count | 429 surge |
| `docgen.async.dlt.depth` | `docgen_async_dlt_depth` | DLT depth when `ASYNC_TRANSPORT=kafka` | DLT depth |
| `up` | `up{job="docgen-backend"}` | Scrape target availability | backend down |

## Validation

1. Scrape smoke: `curl -sf http://localhost:8080/actuator/prometheus` after PDF gen + 429.
2. Rule lint: `promtool check rules deploy/observability/prometheus-alerts.yaml` when available;
   otherwise document manual YAML parse + runbook-link checklist in slice evidence.
3. Confirm every rule has a `runbook` annotation hitting a live runbook section.

### Manual rule lint (when `promtool` unavailable)

Checked 2026-07-12 on slice `lrp-d3-metrics-alerting`:

- `promtool` **not installed** on the delivery host → documented equivalent below.
- YAML parses as Prometheus rule groups (`groups` → `rules` → `alert` / `expr` / `for` / `labels` / `annotations`).
- **7/7** alerts carry `annotations.runbook` → `docs/operations/runbook.md#…` (five primary anchors + `#alert-sse-emitters`; supplemental PDF-failure shares `#alert-pdf-pool-rejections`).
- All rules carry `draft: "true"` (labels and/or annotations).
- p95 rule uses interim **16s** D6-aligned envelope (not stale ≤3s/≤10s).

Docker `/actuator/prometheus` scrape after PDF gen + 429 remains for **stage 10** deploy evidence when the acceptance stack is up.

## Open questions (pending — not requirements)

- Alertmanager routing (email / chat / pager) — operator-owned; not claimed in-repo.
- Prometheus/Grafana retention — recommendation only (e.g. 15d hot / 90d cold) until ops confirms.
- Whether interim ~16 s p95 envelope or post-`DEF-LRP-D6-001` remediation becomes the launch bar
  — **user confirmation** via LR-D5 NFR section.

## Cross-links

- [Production runbook — Observability + alerts](../../docs/operations/runbook.md#observability)
- [Draft thresholds in runbook](../../docs/operations/runbook.md#draft-alert-thresholds-lrd3--not-confirmed-slos)
- [LRP-D §LR-D3](../../docs/plan/detail/LRP-D-ops-observability.md#lr-d3--metrics--alerting-as-code)
- [deploy/README.md index](../README.md)
- [LR-D6 evidence](../../docs/plan/evidence/lrp-d6-load-smoke/)
