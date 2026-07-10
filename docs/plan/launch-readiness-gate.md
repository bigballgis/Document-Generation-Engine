# Launch Readiness Gate — LR-E2

**Status:** Draft (LR-E2) — populated as each prerequisite wave closes
**Last updated:** 2026-07-10
**Program:** [LRP](../../plan/launch-readiness-program.md) §7

## Purpose

A single go/no-go checklist for production launch. Every prerequisite wave (LR-A, LR-B,
LR-C, LR-D, LR-E1) must be **Done** with green gates and evidence before this checklist can
be signed off. No item is skipped; a single 🔴 blocks launch.

## Prerequisite wave status

| Wave | Status | Evidence |
| --- | --- | --- |
| LR-A (rendering trust) | **In Progress** — A1–A4 **Done**; **A7 Done** (Docker PDF corpus; Word delta residual); A5 Partial (0042/0043); **A6 In Progress** (`lrp-a6-ooxml-gate`) | LR-A4 merge `a523a09`; LR-A3 merge `e62c210`; ADR-0042/0043 Proposed (0041 deferred); CD-HARD-T04 Done executed-by-LR-A7; CD-HARD-T03 executes via LR-A6; evidence [lrp-a7-pagination](../evidence/lrp-a7-pagination/) |
| LR-B (multi-instance + session) | Done (2026-07-04) | ADR-0044, ShedLock V46, SSE proxy config, graceful shutdown, LR-B6 session renewal |
| LR-C (usability deepening) | Partial — C1/C4 **Done** (F7); C2–C3/C5–C13 Not Started | F7 evidence in ledger; not current delivery focus |
| LR-D (ops + data lifecycle) | Not Started (checklist rows may be aspirational) | Do not treat as active delivery focus |
| LR-E1 (SSE-through-proxy E2E) | Scaffold in place | `LRP-E1-sse-incremental-progress.spec.ts` (full journey pending seeded template) |

## Go/no-go checklist

### Rendering fidelity

- [x] LR-A1: per-invocation LibreOffice profile isolation — parallel conversion regression green.
- [x] LR-A2: CJK + metric-compatible fonts baked into the backend image; `RenderingFontBaselineTest` / smoke green.
- [x] LR-A3: upload size limits (50MB) + deep validation enforced at Spring + nginx + service; regression green (**Done** 2026-07-10; merge `e62c210`; E2E 5/5).
- [x] LR-A4: unsupported structured node types fail closed; no silent content loss (**Done** 2026-07-10; merge `a523a09`; full writers deferred).
- [ ] LR-A6: OOXML output validation gate green on the corpus.
- [x] LR-A7: Docker PDF pagination corpus measured (≥5 letters; max 9 / median 8) — **Done** 2026-07-10 with documented exception (Word pages/delta **n/a** — `ms-word-unavailable-on-host`; ADR-0042 remains Proposed until Word-equipped host confirms ±1 budget).

### Multi-instance + session

- [ ] LR-B1: deployment topology decision recorded (ADR-0044).
- [ ] LR-B2: scheduler distributed mutex (ShedLock) green; schedulers locked.
- [ ] LR-B3: SSE production readiness (heartbeat, headers, nginx SSE location) — verified by LR-E1.
- [ ] LR-B5: graceful shutdown drains in-flight requests; restart smoke green.
- [ ] LR-B6: sliding session renewal + revocation fail-closed; Playwright Part A/B green.

### Usability

- [ ] LR-C1: dirty-form guard wired into structured editor + metadata dialogs; E2E journey green.
- [ ] LR-C5: catalog server-side pagination/filter; E2E green.
- [ ] LR-C11: `api.error` 145/145 parity; `apiErrorCatalog.test.ts` green.
- [ ] LR-C12: keyboard a11y (Enter/Space row activation, skip-link); a11y smoke green.

### Ops + data lifecycle

- [ ] LR-D1: audit retention scheduler; cleanup tested at the window edge.
- [ ] LR-D2: backup/restore runbook; **timed drill completed** (RPO ≤ 15 min / RTO ≤ 30 min).
- [ ] LR-D3: metrics + alerting rules deployed; dashboards importable.
- [ ] LR-D6: load smoke baseline (≥20 concurrent sync + SSE preview) green on Docker.
- [ ] LR-D7: durable security audit events (login/403/download → DB).

### Release gates

- [ ] Backend `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (0 Checkstyle/PMD/SpotBugs violations).
- [ ] Frontend `pnpm -C frontend lint && type-check && test && build` green.
- [ ] Playwright Docker smoke tier green (`pnpm -C frontend test:e2e:docker`).
- [ ] Docker stack redeployed with LR-A2 font baseline; `/healthz` 200; UI 4173 reachable.
- [ ] No 🔴 Critical findings from `architecture-reviewer`.
- [ ] No secrets / credentials / `.env` in staged files.

## Sign-off

| Role | Decision | Date | Notes |
| --- | --- | --- | --- |
| Architecture | _pending_ | | |
| Backend engineering | _pending_ | | |
| Frontend engineering | _pending_ | | |
| Ops / SRE | _pending_ | | |
| Compliance | _pending_ | | (audit retention, security audit durability) |

A launch is authorized only when every role signs off with no 🔴 open.
