# k6 load suite + NFR confirmation path

| Field | Value |
| --- | --- |
| **Slice** | IBL-D3 / F22 (`ibl-d3-k6-nfr-path`) |
| **Task Master** | **#125** |
| **Recorded** | 2026-07-19 |
| **Tool** | **k6** (Grafana Labs) — checked in under [`perf/k6/`](../../perf/k6/) |
| **Runner** | [`scripts/k6-smoke.ps1`](../../scripts/k6-smoke.ps1) |
| **NFR sink** | [non-functional-requirements.md](../requirements/non-functional-requirements.md) § LR-D5 / 待确认 |

## Why this leaf

**F22** recorded: no industry load tool in-repo; NFR SLOs remain **proposed — awaiting confirmation** with no durable confirmation path. Closing the tooling half means:

1. A **checked-in** k6 suite + scripted runner against the single acceptance stack (`:8080`).
2. A documented path that **feeds measured results into** LR-D5 as **measured-input / proposed**.
3. **No** inventing or promoting **confirmed** SLOs from a green smoke.

User confirmation of any NFR number remains a **separate** governance act (explicit user/PRD confirmation).

## What is checked in

| Artifact | Role |
| --- | --- |
| [`perf/k6/smoke-healthz.js`](../../perf/k6/smoke-healthz.js) | Safe smoke: `GET /healthz` only (2 VU × 15s) |
| [`perf/k6/README.md`](../../perf/k6/README.md) | Install + run against `:8080` |
| [`scripts/k6-smoke.ps1`](../../scripts/k6-smoke.ps1) | Host k6 or Docker fallback; evidence writer; `-DryRun` honesty path |
| [`docs/plan/evidence/ibl-d3-k6-nfr-path/`](../plan/evidence/ibl-d3-k6-nfr-path/) | Scripted run / dry-run evidence |

## How to run (acceptance stack)

```powershell
# Stack must be healthy on 8080 (queued deploy)
.\scripts\docker-deploy-queue.ps1 -Status
# curl / Invoke-WebRequest http://localhost:8080/healthz

# Smoke (preferred wrapper)
.\scripts\k6-smoke.ps1

# Dry-run when k6 binary / image unavailable
.\scripts\k6-smoke.ps1 -DryRun
```

Default `mvn verify` is **unchanged** — this leaf does not gate backend verify on k6.

## Confirmation path (LR-D5)

```text
acceptance stack :8080
        │
        ▼
  scripts/k6-smoke.ps1  →  perf/k6/smoke-healthz.js
        │
        ▼
  docs/plan/evidence/ibl-d3-k6-nfr-path/  (latest-summary.*)
        │
        ▼
  NFR §待确认 / LR-D5  ← measured-input / proposed notes only
        │
        ▼
  explicit user confirmation  ← ONLY step that may flip proposed → confirmed
```

| Allowed | Forbidden |
| --- | --- |
| Record p95 / error rate / check pass as **measured-input** | Promote those numbers to **confirmed** SLOs in «已确认» |
| Soft k6 `thresholds` for smoke usability | Treat thresholds as product SLA / alert enforcement |
| Point ops/Prometheus drafts at LR-D5 pending | Flip checklist **#3b/#5a GO** or claim go-live |

## Relation to prior harnesses

| Source | Role vs IBL-D3 |
| --- | --- |
| LR-D6 / IBL-B2 JUnit `loadsmoke` | Historical **generation/capacity** measured-input — still not confirmed SLOs |
| IBL-D3 k6 smoke | Industry-tool **confirmation-path infrastructure** + safe `/healthz` smoke |

Do **not** re-open or invent concurrent-PDF SLOs from either path.

## Honesty residuals (out of this leaf)

- Host may lack `k6` on `PATH`; Docker Hub pull may fail — use `-DryRun` and install guidance; do not invent metrics.
- Heavier authenticated / generation load scripts may be added later under `perf/k6/` without flipping NFR confirmation.
- **IBL-D4** LO pool chaos / **IBL-D5** legalhold depth — separate leaves.
- Soft thresholds in `smoke-healthz.js` are **not** F8 alert thresholds.

## Traceability

- Behavior readiness: [ibl-d3-k6-nfr-path.md](../behavior/ibl-d3-k6-nfr-path.md) (`bdd_readiness: not-applicable`)
- Program: [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § IBL-D3 / F22
- Sibling lanes: [test-database-strategy.md](./test-database-strategy.md) (D1) · [libreoffice-ci-lane.md](./libreoffice-ci-lane.md) (D2)
