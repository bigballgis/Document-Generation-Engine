# Master Plan

**Baseline:** Project restart from zero — 2026-06-23  
**Active phase:** P12 (Deferred enhancements — no single active slice)

## Product goal

Build an enterprise low-code document generation platform for bank correspondence:
template lifecycle, dynamic generation API, API authorization, DOCX/PDF output with
optional dynamic encryption. v1 does not provide a customer-facing generation portal;
upstream systems invoke the dynamic API.

## Delivery strategy

1. **Thin vertical slice first** — one end-to-end path before breadth.
2. **Test-first** — behavior spec → failing test → implementation → green gates.
3. **Document-as-code** — update docs with or before behavior changes.
4. **English-first i18n** — all user-facing strings via message keys.

## Phase roadmap

| Phase | Name | Depends on | Exit criteria (summary) | Status |
| --- | --- | --- | --- | --- |
| P0 | Foundation & guardrails | — | Runnable skeleton, docker-compose, CI gates, OpenAPI contract test harness | Done |
| P1 | Login & session | P0 | Real local auth, seeded users, role/group session, login-first shell entry | Done |
| P2 | Master management | P1 | Upload DOCX master, anchor catalog, lightweight review, group isolation | Done |
| P3 | Template authoring | P2 | Create template from approved master, variables, structured content, rules | Done |
| P4 | Rendering & preview | P3 | DOCX/PDF render, fidelity warnings/blockers, preview records | Done |
| P5 | Lifecycle governance | P4 | Test → approve → publish with audit; publish gate checklist | Done |
| P6 | API management | P5 | Credentials, AD Group policy, output/batch/encryption/default route | Done |
| P7 | Runtime dynamic API | P5, P6 | OpenAPI v1 operations callable with auth, idempotency, sync/async/batch | Done |
| P8 | Audit & contract visibility | P5, P6, P7 | Role-scoped audit console; caller contract view | Done |
| P9 | Production readiness | P0–P8 | Security scans, observability, deployment evidence, release gates | Done |
| P10 | Runtime document download | P9 | Secure download with secondary auth and 15-minute expiry | Done |
| P11 | Batch & async generation | P10 | Sync batch, async task query/cancel | Done |

## Thin vertical slice (MVP chain)

Target before expanding breadth:

```text
Login → upload & approve master → create template → configure variables/content
  → test generate → submit test/approval → publish
  → configure minimal API policy → sync generate DOCX via runtime API
```

Phases involved: **P0 → P1 → P2 → P3 → P4 → P5 → P6 → P7 (minimal sync path)**.

## Deferred / post-MVP enhancements

| Area | Notes |
| --- | --- |
| P6-T04 | Management caller contract page | Done |
| P5-D03 | Publish gate checklist UI | Done |
| P3-T04/T05 deep UI | Rule configurator + binding validation UI (test data CRUD deferred) | Done (thin slice) |
| P4-T05 | Preview comparison panel + comparisonSummary | Done (thin slice) |
| P7-T06 | DOCX + PDF encryption execution (LibreOffice prod / PDFBox test conversion) | Done (thin slice) |
| Idempotency cache seam | Redis + DB dual-write | Done |
| E06-T05 | Playwright login a11y smoke | Done |
| Kafka async worker | Kafka transport + DLT + EmbeddedKafka test | Done (thin slice) |
| Test data set CRUD | P3-T05 full backend + UI | Done |
| Batch PDF/encryption | DocumentGenerationEngine batch path | Done |
| LibreOffice docker-exec | `conversion-mode=docker-exec` | Done |

## Epic cross-reference

| Epic | Maps to phases | Task sheet |
| --- | --- | --- |
| E01 Master & template authoring | P2, P3, P4 | [e01-task-sheet.md](../architecture/e01-task-sheet.md) |
| E02 Lifecycle workflow | P5 | [e02-task-sheet.md](../architecture/e02-task-sheet.md) |
| E03 API management | P6 | [e03-task-sheet.md](../architecture/e03-task-sheet.md) |
| E04 Audit console | P8 | [e04-task-sheet.md](../architecture/e04-task-sheet.md) |
| E05 Enterprise integration | P0, P7, P9 | [e05-task-sheet.md](../architecture/e05-task-sheet.md) |
| E06 Management UI finish | P1, P5, P6, P8 | [e06-task-sheet.md](../architecture/e06-task-sheet.md) |
| E07 Production readiness | P9 | [e07-task-sheet.md](../architecture/e07-task-sheet.md) |
| E11 Role-journey UI | P1, P6 | [e11-role-journey-ui-continuation-plan.md](../architecture/e11-role-journey-ui-continuation-plan.md) |
| E12 Frontend role-operation UI | P1, E11 | [e12-phase1-task-sheet.md](../architecture/e12-phase1-task-sheet.md) |

## Milestone cross-reference (technical waves)

| Milestone | Scope | Task sheet |
| --- | --- | --- |
| M1 | Wave 0–1: foundation + contract discovery | [m1-task-sheet.md](../architecture/m1-task-sheet.md) |
| M2 | Wave 2: sync generation | [m2-task-sheet.md](../architecture/m2-task-sheet.md) |
| M3 | Wave 3: batch & async | [m3-task-sheet.md](../architecture/m3-task-sheet.md) |
| M4 | Wave 4: download security | [m4-task-sheet.md](../architecture/m4-task-sheet.md) |
| M5 | Wave 5: API management backend | [m5-task-sheet.md](../architecture/m5-task-sheet.md) |
| M6 | Wave 6: lifecycle governance backend | [m6-task-sheet.md](../architecture/m6-task-sheet.md) |
| M7 | Wave 7: runtime integration | [m7-task-sheet.md](../architecture/m7-task-sheet.md) |
| M8–M11 | Security & dependency gates | m8–m11 task sheets |
| M12–M14 | Runtime adapter & transport closure | m12–m14 task sheets |

## Active phase rule

- Only one phase row may be `In Progress`.
- Activate the next phase only when the prior phase exit criteria are met and marked `Done`.
