# LR-E1 — SSE-through-proxy incremental E2E (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `lrp-e1-sse-proxy-e2e` |
| **Plan** | [launch-readiness-program.md §7 LR-E1](../plan/launch-readiness-program.md#lr-e1--sse-through-proxy-incremental-e2e) |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-12 |
| **Formal phase** | None (Wave LR-E — release readiness gate) |
| **Task Master** | **#42** (`in-progress`) · plan id **LR-E1** |

---

## Why BDD is not-applicable

LR-E1 delivers **browser-level reliability evidence** for already-shipped **LR-B3** SSE transport hardening — not a product behavior change:

- No new user-facing journey, management UI surface, API contract, permission rule, or audit semantics.
- No change to preview / batch-test **SSE event names, payloads, or progress semantics** — those remain as delivered by P12 ([template-testing-overhaul.md](./template-testing-overhaul.md)) and exercised for success-path download by [CD-E2E-T08](./preview-success-artifact-download-journey.md).
- Outcomes are **Playwright proof through the nginx proxy on Docker 4173** (incremental arrival + heartbeat survival) plus an evidence manifest — not new acceptance thresholds or product requirements.
- Preferred deliverables are **`frontend/e2e/LRP-E1-sse-incremental-progress.spec.ts`** + **`frontend/e2e/evidence/LRP-E1-sse-manifest.md`** — inventing new SSE UX, polling substitutes, or weakening LR-B3 headers is out of scope.

Plan authority: [launch-readiness-program.md](../plan/launch-readiness-program.md) §7 LR-E1 — **BDD: not-applicable — test-only evidence for LR-B3 (reliability, no behavior contract change).**

Upstream transport ownership: [LRP-B §LR-B3](../plan/detail/LRP-B-runtime-scaleout-session.md#lr-b3--sse-production-readiness) — **Done** (heartbeat, anti-buffering headers, nginx SSE location, Docker curl smoke); **browser-level incremental proof stays LR-E1**.

---

## What is in scope (evidence / reliability only)

| Deliverable | Intent |
| --- | --- |
| **Playwright Docker E2E** | Drive preview (and one long batch-test run) through UI on `http://localhost:4173`; assert SSE progress events with **arrival timestamps** |
| **Incremental proof** | ≥2 distinct arrival times before completion (not one terminal burst) — proves nginx does not buffer the stream into a single flush |
| **Heartbeat survival** | Stream open ≥60 s through proxy without premature termination; heartbeat keeps the connection alive |
| **Evidence manifest** | `frontend/e2e/evidence/LRP-E1-sse-manifest.md` recording timestamps / pass evidence |

**Environment / policy constraints (from program §7):**

- Deploy via queued Docker acceptance stack (UI **4173**); LR-B3 must already be merged.
- Do **not** use API polling to fake stream assertions.
- Do **not** weaken LR-B3 headers / nginx SSE config to make the test pass.
- Do **not** execute `CD-E2E-*` tasks from this slice (cross-link only).
- Do **not** invent new product SSE contracts or change event payloads.
- Leave **LR-E2** to its own checklist slice — this note only feeds E2 as a go/no-go input when E1 is green.
- Formal phase remains **None**; do **not** touch `DGE-audit-governance`.

**Upstream product journeys (reused, not re-specified):**

- P12 preview / batch SSE progress semantics — [template-testing-overhaul.md](./template-testing-overhaul.md) (SCEN-F1-01 / SCEN-F2-01).
- CD-E2E-T08 success + download path — [preview-success-artifact-download-journey.md](./preview-success-artifact-download-journey.md) (terminal success UX; not incremental timing).
- LR-B3 transport — heartbeat `: keep-alive`, `X-Accel-Buffering: no`, `Cache-Control: no-cache`, nginx `proxy_buffering off` for progress-stream locations.

---

## Acceptance scenarios (program §7 LR-E1 G/W/T)

These are **test evidence criteria** for Playwright reliability proof through the nginx proxy — **not** product BDD Given/When/Then for TDD Red of new user-facing behavior. No new product actor journeys are invented here.

### Scenario A — Incremental preview progress through proxy

- **Given** the Docker stack on **4173** with **LR-B3** merged
- **When** a preview with multi-step progress runs through the UI
- **Then** the spec proves **≥2 incremental arrival timestamps** before completion (events are not delivered as a single terminal burst)

### Scenario B — Long-lived stream + heartbeat through proxy

- **Given** a batch test run exceeding **60 s**
- **When** the stream is open through the nginx proxy
- **Then** there is no premature termination; heartbeat keeps the connection alive

---

## Explicit non-goals

- No product requirement inventing new SSE event types, progress UX, or API fields.
- No substituting EventSource with polling / WebSocket for the assertion path.
- No relaxing LR-B3 anti-buffering / heartbeat / nginx SSE location to green the gate.
- No re-running or owning **CD-E2E-T08** (or other `CD-E2E-*`) from this slice.
- No activating or expanding **LR-E2** beyond a cross-link that E1 evidence feeds the launch checklist.
- No marking LR-E1 **Done** in this readiness record alone — Done requires Playwright green + manifest + doc sync + commit review (program §7).

---

## Traceability

| Artifact | Role |
| --- | --- |
| [Program §7 LR-E1](../plan/launch-readiness-program.md#lr-e1--sse-through-proxy-incremental-e2e) | Authoritative task row + G/W/T |
| [LRP-B §LR-B3](../plan/detail/LRP-B-runtime-scaleout-session.md#lr-b3--sse-production-readiness) | Shipped transport hardening under test |
| [P12 template testing overhaul](./template-testing-overhaul.md) | Existing product SSE progress semantics |
| [CD-E2E-T08 preview success journey](./preview-success-artifact-download-journey.md) | Adjacent browser success path (terminal UX; not incremental timing) |
| CD-PIT-12 | Original finding: SSE buffered through frontend nginx |
| [LR-D6 Scenario B](./lrp-d6-load-smoke.md) | Prior concurrent SSE integrity smoke (API harness; not browser-through-proxy incremental) |
| Program §7 LR-E2 | Downstream go/no-go checklist consumer of E1 green evidence |

```
bdd_readiness: not-applicable
task_ids: [LR-E1 / lrp-e1-sse-proxy-e2e]  # Task Master id: allocate via plan-orchestrator
```
