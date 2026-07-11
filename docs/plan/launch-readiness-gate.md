# Launch Readiness Gate — LR-E2

**Status:** Draft (LR-E2) — populated as each prerequisite wave closes
**Last updated:** 2026-07-11
**Program:** [LRP](../../plan/launch-readiness-program.md) §7

## Purpose

A single go/no-go checklist for production launch. Every prerequisite wave (LR-A, LR-B,
LR-C, LR-D, LR-E1) must be **Done** with green gates and evidence before this checklist can
be signed off. No item is skipped; a single 🔴 blocks launch.

## Prerequisite wave status

| Wave | Status | Evidence |
| --- | --- | --- |
| LR-A (rendering trust) | **Done** (2026-07-10) — A1–A7 Done; **ADR-0041 Accepted**; 0042/0043 remain **Proposed**; Word-vs-LO / 0042 Accepted + 0043 slice B **deferred out of wave exit** (0041 Accepted residual closed) | LR-A5 merge `cc9e5f6` (docs-only + architecture-reviewer PASS_WITH_NOTES); LR-A6 merge `122d6d1`; LR-A7 evidence [lrp-a7-pagination](../evidence/lrp-a7-pagination/); CD-HARD-T01/T03/T04 Done |
| LR-B (multi-instance + session) | Done (2026-07-04) | ADR-0044, ShedLock V46, SSE proxy config, graceful shutdown, LR-B6 session renewal |
| LR-C (usability deepening) | **Done** (2026-07-11) — C1–C13 all Done; merge tip `bf9cbeb`; **no sole-active** | **LR-C8 Done** 2026-07-11 (`lrp-c8-role-onboarding-tour`; merge `bf9cbeb`; E2E 4/4; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #34); LR-C7 Done 2026-07-11 (`lrp-c7-notification-center`; merge `879108c`; E2E 5/5; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #33); LR-C6 Done 2026-07-11 (`lrp-c6-command-palette`; merge `c0c84aa`; E2E 8/8; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #32); LR-C5 Done 2026-07-11 (`lrp-c5-catalog-pagination`; merge `5543a33`; E2E 6/6; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; OPT-F4 residual closed; Task Master #31); LR-C3 Done 2026-07-11 (`lrp-c3-editor-undo-redo`; merge `0cf553b`; E2E 7/7; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #30); LR-C2 Done 2026-07-11 (`lrp-c2-local-draft-recovery`; merge `12a6a7e`; E2E 4/4; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #29); LR-C13 merge `45addd6` (OPT-G4/G5 closed; coverage 80/55/80/80; architecture PASS_WITH_NOTES; Task Master #28); LR-C12 merge `0357a16` (E2E 10/10; UIUX PASS_WITH_NOTES; COR-F21 residual closed; Task Master #27); LR-C11 merge `44fcf40` (parity **159/159**; architecture PASS_WITH_NOTES); LR-C10 merge `bdaf95d` (E2E 4/4; UIUX PASS_WITH_NOTES); LR-C9 merge `0013615`; F7 for C1/C4 |
| LR-D (ops + data lifecycle) | **In Progress** (2026-07-11 — partial; **sole-active = LR-D7**; **LR-D1 Done**) | **LR-D7 In Progress** (`lrp-d7-durable-security-audit`; Task Master #36; BDD ready); **LR-D1 Done** (`20b2a76`; Task Master #35; ADR-0048 Accepted). D2–D6 Not Started — do not activate. |
| LR-E1 (SSE-through-proxy E2E) | Scaffold in place | `LRP-E1-sse-incremental-progress.spec.ts` (full journey pending seeded template) |

## Go/no-go checklist

### Rendering fidelity

- [x] LR-A1: per-invocation LibreOffice profile isolation — parallel conversion regression green.
- [x] LR-A2: CJK + metric-compatible fonts baked into the backend image; `RenderingFontBaselineTest` / smoke green.
- [x] LR-A3: upload size limits (50MB) + deep validation enforced at Spring + nginx + service; regression green (**Done** 2026-07-10; merge `e62c210`; E2E 5/5).
- [x] LR-A4: unsupported structured node types fail closed; no silent content loss (**Done** 2026-07-10; merge `a523a09`; full writers deferred).
- [x] LR-A5: ADR-0041/0042/0043 drafted and indexed (**Done** 2026-07-10 — **ADR-0041 Accepted** via architecture-reviewer PASS_WITH_NOTES; 0042/0043 remain **Proposed**; Word residual deferred post-wave).
- [x] LR-A6: OOXML output validation gate green on the corpus (**Done** 2026-07-10; merge `122d6d1`; `OoxmlOutputValidator` + runtime `OOXML_VALIDATION_FAILED`; CD-HARD-T03 executed-by-LR-A6; ADR-0043 remains Proposed; ECMA-376 XSD / LO24 deferred).
- [x] LR-A7: Docker PDF pagination corpus measured (≥5 letters; max 9 / median 8) — **Done** 2026-07-10 with documented exception (Word pages/delta **n/a** — `ms-word-unavailable-on-host`; ADR-0042 remains Proposed until Word-equipped host confirms ±1 budget — deferred out of Wave LR-A exit).

### Multi-instance + session

- [ ] LR-B1: deployment topology decision recorded (ADR-0044).
- [ ] LR-B2: scheduler distributed mutex (ShedLock) green; schedulers locked.
- [ ] LR-B3: SSE production readiness (heartbeat, headers, nginx SSE location) — verified by LR-E1.
- [ ] LR-B5: graceful shutdown drains in-flight requests; restart smoke green.
- [ ] LR-B6: sliding session renewal + revocation fail-closed; Playwright Part A/B green.

### Usability

- [x] LR-C1: dirty-form guard wired into structured editor + metadata dialogs; E2E journey green. **(Done 2026-07-09 — CORE-FORTRESS F7)**
- [x] LR-C5: catalog server-side pagination/filter; E2E green. **(Done 2026-07-11 — merge `5543a33`; E2E 6/6; UIUX PASS_WITH_NOTES; Task Master #31; OPT-F4 residual closed)**
- [x] LR-C8: role onboarding tour (`el-tour`); first-login / skip / help replay; E2E green. **(Done 2026-07-11 — merge `bf9cbeb`; E2E 4/4; UIUX PASS_WITH_NOTES; Task Master #34)**
- [x] LR-C11: `api.error` N/N parity (live backend count; **159/159**); `apiErrorCatalog.test.ts` green (**Done** 2026-07-11; merge `44fcf40`).
- [x] LR-C12: keyboard a11y (Enter/Space row activation, skip-link); a11y smoke green. **(Done 2026-07-11 — merge `0357a16`; E2E 10/10; UIUX PASS_WITH_NOTES; Task Master #27)**
- [x] Wave LR-C exit: C1–C13 all Done (2026-07-11; merge tip `bf9cbeb`)

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
