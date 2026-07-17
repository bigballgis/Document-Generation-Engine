# Launch Readiness Gate — LR-E2

**Status:** **Done** (LR-E2 2026-07-12 — slice `lrp-e2-launch-checklist`; merge `ae39fbb`; Task Master #43; **authoritative checklist:** [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) — overall snapshot **NO-GO**; Wave LR-E docs exit gate closed — **not** production go-live; this gate file retains prerequisite wave summary + historical checkboxes)
**Last updated:** 2026-07-12
**Program:** [LRP §7](./launch-readiness-program.md#7-wave-lr-e--release-readiness-gate-发布就绪门禁) · [LR-E2](./launch-readiness-program.md#lr-e2--launch-readiness-checklist-上线-gonogo-清单)

## Purpose

Prerequisite-wave summary supporting the ops **[launch-readiness-checklist.md](../operations/launch-readiness-checklist.md)** (LR-E2 SoT for go/no-go / conditional + verdict template). Every prerequisite wave (LR-A, LR-B, LR-C, LR-D, LR-E1) must be **Done** with green gates and evidence before a real launch decision. Incomplete evidence → **NO-GO** for that item. **Not** a production go-live authorization.

## Prerequisite wave status

| Wave | Status | Evidence |
| --- | --- | --- |
| LR-A (rendering trust) | **Done** (2026-07-10) — A1–A7 Done; **ADR-0041/0042/0043 Accepted** (0042/0043 via PRR-C01 #103 `3513ab92`; 0042 Path X residual; 0043 slice B residual); checklist **#3b CONDITIONAL** (≠ GO) | LR-A5 merge `cc9e5f6`; LR-A6 merge `122d6d1`; LR-A7 evidence [lrp-a7-pagination](../evidence/lrp-a7-pagination/); PRR-C01 Path X [word-baseline-exemption](../evidence/prod-adr-0042-0043-closeout/word-baseline-exemption.md); CD-HARD-T01/T03/T04 Done |
| LR-B (multi-instance + session) | Done (2026-07-04) | ADR-0044, ShedLock V46, SSE proxy config, graceful shutdown, LR-B6 session renewal |
| LR-C (usability deepening) | **Done** (2026-07-11) — C1–C13 all Done; merge tip `bf9cbeb`; **no sole-active** | **LR-C8 Done** 2026-07-11 (`lrp-c8-role-onboarding-tour`; merge `bf9cbeb`; E2E 4/4; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #34); LR-C7 Done 2026-07-11 (`lrp-c7-notification-center`; merge `879108c`; E2E 5/5; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #33); LR-C6 Done 2026-07-11 (`lrp-c6-command-palette`; merge `c0c84aa`; E2E 8/8; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #32); LR-C5 Done 2026-07-11 (`lrp-c5-catalog-pagination`; merge `5543a33`; E2E 6/6; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; OPT-F4 residual closed; Task Master #31); LR-C3 Done 2026-07-11 (`lrp-c3-editor-undo-redo`; merge `0cf553b`; E2E 7/7; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #30); LR-C2 Done 2026-07-11 (`lrp-c2-local-draft-recovery`; merge `12a6a7e`; E2E 4/4; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; Task Master #29); LR-C13 merge `45addd6` (OPT-G4/G5 closed; coverage 80/55/80/80; architecture PASS_WITH_NOTES; Task Master #28); LR-C12 merge `0357a16` (E2E 10/10; UIUX PASS_WITH_NOTES; COR-F21 residual closed; Task Master #27); LR-C11 merge `44fcf40` (parity **159/159**; architecture PASS_WITH_NOTES); LR-C10 merge `bdaf95d` (E2E 4/4; UIUX PASS_WITH_NOTES); LR-C9 merge `0013615`; F7 for C1/C4 |
| LR-D (ops + data lifecycle) | **Done** (2026-07-12 — D1–D7 all Done; merge tip 218dcf1) | **LR-D4 Done** (lrp-d4-trace-propagation; merge 218dcf1; feature tip 670a683; Task Master #41; BDD not-applicable; ADR-0049 Accepted; MdcTaskDecorator + Kafka X-Trace-Id; Scenario A/B; mvn verify GREEN 1321; architecture PASS_WITH_NOTES; DEPLOY_OK 2026-07-12T04:18:13+08:00 healthz 200; X-Trace-Id echo OK). **LR-D3 Done** (ba5ea2e; #40). **LR-D2 Done** (3d78bc5; #39; scratch drill). **LR-D5 Done** (5b13476; #38; NFR proposals pending — **not** confirmed SLOs). **LR-D6 Done** (56383eb; #37). **LR-D7 Done** (c94a356; #36; seam closed). **LR-D1 Done** (20b2a76; #35; ADR-0048). |
| LR-E1 (SSE-through-proxy E2E) | **Done** (2026-07-12 — merge `575d0aa`; Task Master #42) | Slice `lrp-e1-sse-proxy-e2e`; BDD not-applicable; Playwright 2/2; Scenario A maxGapMs≈1864; Scenario B idle 65075 ms + ~20 s keep-alive; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; evidence [LRP-E1-sse-manifest.md](../../frontend/e2e/evidence/LRP-E1-sse-manifest.md) |
| LR-E2 (launch readiness checklist) | **Done** (2026-07-12 — merge `ae39fbb`; Task Master #43) | Slice `lrp-e2-launch-checklist`; docs-only; [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md); overall **NO-GO**; architecture PASS_WITH_NOTES; Wave LR-E **Done** (docs gate — **not** production go-live); **no sole-active** |

## Go/no-go checklist

### Rendering fidelity

- [x] LR-A1: per-invocation LibreOffice profile isolation — parallel conversion regression green.
- [x] LR-A2: CJK + metric-compatible fonts baked into the backend image; `RenderingFontBaselineTest` / smoke green.
- [x] LR-A3: upload size limits (50MB) + deep validation enforced at Spring + nginx + service; regression green (**Done** 2026-07-10; merge `e62c210`; E2E 5/5).
- [x] LR-A4: unsupported structured node types fail closed; no silent content loss (**Done** 2026-07-10; merge `a523a09`; full writers deferred).
- [x] LR-A5: ADR-0041/0042/0043 drafted and indexed (**Done** 2026-07-10 — **ADR-0041 Accepted** via architecture-reviewer PASS_WITH_NOTES; **0042/0043 Accepted** 2026-07-18 via PRR-C01 #103 with Path X / slice B residuals; checklist **#3b CONDITIONAL**).
- [x] LR-A6: OOXML output validation gate green on the corpus (**Done** 2026-07-10; merge `122d6d1`; `OoxmlOutputValidator` + runtime `OOXML_VALIDATION_FAILED`; CD-HARD-T03 executed-by-LR-A6; ADR-0043 **Accepted** slice A 2026-07-18 PRR-C01; ECMA-376 XSD / LO24 deferred residual).
- [x] LR-A7: Docker PDF pagination corpus measured (≥5 letters; max 9 / median 8) — **Done** 2026-07-10 with documented exception (Word pages/delta **n/a** — `ms-word-unavailable-on-host`); ADR-0042 **Accepted** 2026-07-18 via PRR-C01 Path X + metadata-gated enforcement (Path E still required for checklist **#3b GO**).

### Multi-instance + session

- [x] LR-B1: deployment topology decision recorded (ADR-0044) — **Done** 2026-07-04.
- [x] LR-B2: scheduler distributed mutex (ShedLock) green; schedulers locked — **Done** 2026-07-04.
- [x] LR-B3: SSE production readiness (heartbeat, headers, nginx SSE location) — verified by LR-E1 (**Done** 2026-07-12 — merge `575d0aa`; Playwright 2/2; [manifest](../../frontend/e2e/evidence/LRP-E1-sse-manifest.md)).
- [x] LR-B5: graceful shutdown drains in-flight requests; restart smoke green — **Done** 2026-07-04.
- [x] LR-B6: sliding session renewal + revocation fail-closed; Playwright Part A/B green — **Done** 2026-07-04.
- [x] LR-B8: prod compose `/healthz` healthcheck + mem/cpu limits — **Done** 2026-07-04 (see [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) row #7).

> Prefer [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) for overall **CONDITIONAL** residuals (ADR-0042/0043 Accepted + Path X → **#3b CONDITIONAL** ≠ GO; **#9 JWT_SECRET GO** merge `587cd9a`; **#10 Kafka → CONDITIONAL**; **#5a** AD → **CONDITIONAL**; overall **CONDITIONAL** — **not** go-live).

### Usability

- [x] LR-C1: dirty-form guard wired into structured editor + metadata dialogs; E2E journey green. **(Done 2026-07-09 — CORE-FORTRESS F7)**
- [x] LR-C5: catalog server-side pagination/filter; E2E green. **(Done 2026-07-11 — merge `5543a33`; E2E 6/6; UIUX PASS_WITH_NOTES; Task Master #31; OPT-F4 residual closed)**
- [x] LR-C8: role onboarding tour (`el-tour`); first-login / skip / help replay; E2E green. **(Done 2026-07-11 — merge `bf9cbeb`; E2E 4/4; UIUX PASS_WITH_NOTES; Task Master #34)**
- [x] LR-C11: `api.error` N/N parity (live backend count; **159/159**); `apiErrorCatalog.test.ts` green (**Done** 2026-07-11; merge `44fcf40`).
- [x] LR-C12: keyboard a11y (Enter/Space row activation, skip-link); a11y smoke green. **(Done 2026-07-11 — merge `0357a16`; E2E 10/10; UIUX PASS_WITH_NOTES; Task Master #27)**
- [x] Wave LR-C exit: C1–C13 all Done (2026-07-11; merge tip `bf9cbeb`)

### Ops + data lifecycle

- [x] LR-D1: audit retention scheduler; cleanup tested at the window edge.
- [x] LR-D2: backup/restore runbook; **timed drill completed** (scratch 2026-07-12 — RPO≈0.933 min / RTO≈4.751 min vs ADR-0030 targets; merge `3d78bc5`; [dated evidence](../operations/backup-restore-runbook.md#drill-evidence-2026-07-12--executed) — **not** a production compliance claim; Wave LR-D now **Done**).
- [x] LR-D3: metrics + alerting rules deployed; dashboards importable — merge `ba5ea2e`; scrape DEPLOY_OK 2026-07-12T03:43:07+08:00; artifacts `deploy/observability/prometheus-alerts.yaml` + `grafana/docgen-ops-overview.json`; promtool unavailable — manual lint 7/7.
- [x] LR-D4: trace propagation decision + minimal path proven — merge `218dcf1`; feature tip `670a683`; ADR-0049 Accepted; `MdcTaskDecorator` + Kafka `X-Trace-Id`; Scenario A/B; mvn verify GREEN 1321; DEPLOY_OK 2026-07-12T04:18:13+08:00; X-Trace-Id echo OK; Task Master #41.
- [x] LR-D5: NFR quantification proposals authored as pending — merge `5b13476`; Task Master #38; **not** confirmed SLOs.
- [x] LR-D6: load smoke baseline (≥20 concurrent sync + SSE preview) measured on Docker — merge `56383eb`; A: n=20 success=12 errorRate=0.4 triaged DEF-LRP-D6-001; B: 5/5 SSE dropped=0; evidence [lrp-d6-load-smoke](./evidence/lrp-d6-load-smoke/).
- [x] LR-D7: durable security audit events (login/403/download → DB) — merge `c94a356`; seam closed.
- [x] Wave LR-D exit: D1–D7 all Done (2026-07-12; merge tip `218dcf1`; **no sole-active**).

### Release gates

- [x] LR-E1: SSE-through-proxy incremental E2E green on Docker 4173 — merge `575d0aa`; Playwright 2/2; [manifest](../../frontend/e2e/evidence/LRP-E1-sse-manifest.md); closes CD-PIT-12 browser proof.
- [x] LR-E2: Launch readiness checklist authored + indexed + reviewed — merge `ae39fbb`; [checklist](../operations/launch-readiness-checklist.md); overall **NO-GO**; Wave LR-E docs exit gate **Done** — **not** production go-live.
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

Readiness review may proceed only when every role signs off with no 🔴 open. Completing this table does **not** authorize production go-live (see Purpose; ops SoT [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md)).
