# Plan Layer Index

Execution truth for this project lives here.

**Current baseline:** P0–P11 re-earned **Done** (2026-06-23); **P13** **Done** (2026-06-23);
**P17** per-domain API policy governance **Done** (2026-06-25; Wave 3); **P19**
verifiability/publish-gate **Done** (2026-06-25); **P20** i18n **Done** (2026-06-25).
**P14** confirmed large domains **Done** (2026-06-27). **P15** Kubernetes deployment **Done**
(2026-06-27; T01–T10). **P18** structured authoring **Done** (2026-06-28; T01–T10).
**Delivery focus note (2026-07-20):** **#131** IBL-E4 → **In Progress** (**sole-active**; Batch **solo** `ibl-e4-entity-document-brands`; ISOLATED `D:/working/DGE-ibl-e4-entity-document-brands` · `feat/ibl-e4-entity-document-brands`; base MAIN tip `3bd2cd87`; BDD **required**; owners backend-engineer + frontend-engineer; Wave **IBL-E stays In Progress**; **#132–#134** pending **not** activated; #119 remains Blocked; formal phase **None**; do **not** flip **#3b/#5a GO**; do **not** claim Wave E / IBL program Done / go-live). Prior **#130** IBL-E3 → **Done** (MAIN merge `233342d3` / feature tip `e81a6bac`; docs tip `3bd2cd87`; worktree **REMOVED**; Batch **solo** closed; **F26 closed**; ADR-0064 Accepted + legal→compliance approval matrix + `LEGAL_REVIEWER`; BDD **ready** [ibl-e3-legal-approval-matrix.md](../behavior/ibl-e3-legal-approval-matrix.md); Gates: `mvn verify` **GREEN 2241**; FE gates **GREEN**; E2E **4/4**; UIUX **PASS**; architecture **PASS_WITH_NOTES**; Stage 5+10 **DEPLOY_OK**; evidence `docs/plan/evidence/ibl-e3-*`). Prior **#129** IBL-E2 → **Done** (MAIN merge `81a1ca29` / `6a96e9ab`; F25 closed). Prior **#128** IBL-E1 → **Done** (MAIN merge `4c712b03` / feature tip `9decf19c`; worktree **REMOVED**; Batch **solo** closed; **F24 closed**; ADR-0062 Accepted + impl; BDD **ready** [ibl-e1-locale-variant-model.md](../behavior/ibl-e1-locale-variant-model.md); Gates: `mvn verify` **GREEN 2202**; FE lint/type-check/test **1571** / build **GREEN**; E2E **4/4**; UIUX **PASS** Critical=0; arch Critical=0 `merge_go`; Stage 5+10 **DEPLOY_OK**; evidence `docs/plan/evidence/ibl-e1-*`). Prior Out-of-band FE bug leaf `nav-missing-icons` → **Done** (MAIN merge `137a115f` / feature tip `47784667`; worktree **REMOVED**; Batch **solo** closed; BDD **ready**; FE gates + E2E **2/2** + UIUX **PASS** + arch **PASS_WITH_NOTES** `merge_go=true` + Stage 5/10 **DEPLOY_OK**; **session writer cleared**; program/TM **sole-active cleared** (#128 Done; #129 not activated)). Prior **#127** IBL-D5 → **Done** (MAIN merge `6f672271` / feature tip `2e56787e`; worktree removed; F23 legalhold half closed — create/enforce/block depth: 61 legalhold tests / +44 vs baseline 17; Playwright 9/162 residual **OUT**; BDD **not-applicable**; Batch **solo** closed; Gates: `mvn verify` **GREEN 2188**; arch **PASS_WITH_NOTES** `merge_go=true`; Stage 10 SkipBuild health **DEPLOY_OK**; FE/E2E **N/A**; **Wave IBL-D → Done** D1–D5 all Done; Wave **IBL-C** remains **Done** C1+C2+C3; Wave **IBL-B** stays **In Progress** B1–B6 Done / B7 Blocked residual — skip B7; do **not** claim Wave B / IBL program Done; Wave **IBL-A** remains **Done**; IBL/CE **sole-active cleared**; **#119** IBL-B7 still **Blocked** (do **not** invent Word evidence); formal phase **None**; do **not** flip **#3b/#5a GO**; do **not** invent Word/pixel; do **not** claim go-live. Prior **#126** IBL-D4 → **Done** (MAIN merge `94cc8eeb` / feature tip `94526674`; worktree removed; F22 closed with D3 load suite — LO pool chaos/failover (`-Plo-pool-chaos`; 6 tests / 1 skip no soffice); BDD **not-applicable**; Batch **solo** closed; Gates: `mvn verify` **GREEN 2144**; arch **PASS_WITH_NOTES** `merge_go=true`; Stage 10 SkipBuild health **DEPLOY_OK**; FE/E2E **N/A**). Prior **#125** IBL-D3 → **Done** (MAIN merge `6b9b6487` / feature tip `0242084e`; worktree removed; F22 load half — k6 suite + NFR confirmation path closed; SLOs stay proposed; honest dry-run residual (k6 binary unavailable); LO chaos → **IBL-D4**; BDD **not-applicable**; Batch **solo** closed; Gates: arch **PASS_WITH_NOTES** `merge_go=true`; Stage 10 SkipBuild health **DEPLOY_OK**; FE/E2E **N/A**). Prior **#124** IBL-D2 → **Done** (MAIN merge `21be3a99` / feature tip `4fd0c5da`; worktree removed; F21 closed — opt-in `-Plibreoffice-ci` fail-not-skip when `soffice` absent; default optional LO skip retained; BDD **not-applicable**; Batch **solo** closed; Gates: default `mvn verify` **GREEN 2138**/14 skipped; `-Plibreoffice-ci` fail-closed without soffice (**expected FAIL**); arch **PASS_WITH_NOTES** (after excluding demo docx); Stage 10 ForceRebuild **DEPLOY_OK**; FE/E2E **N/A**; residual: company CI wiring `-Plibreoffice-ci` may remain; soffice-positive GREEN not proven on this host). Prior **#123** IBL-D1 → **Done** (MAIN merge `1a686938` / feature tip `f399489c`; worktree removed; F20 closed — opt-in TC+Flyway lane; default H2 retained; BDD **not-applicable**; Batch **solo** closed; Gates: default `mvn verify` **GREEN 2133**; `-Ptestcontainers,dev-fast test` **GREEN** (Flyway **68** on PG16); arch **PASS_WITH_NOTES** `merge_go=true`; Stage 10 ForceRebuild **DEPLOY_OK**; FE/E2E **N/A**).. Prior **#122** IBL-C3 → **Done** (MAIN merge `bdfc285d` / feature tip `dbfff086`; worktree removed; F19 closed — en/zh + multi-currency themes honest **SYNTHETIC**; LO PDF upgrade residual → **IBL-D2** / **F21**; do **not** claim all goldens LIBREOFFICE-produced; BDD **not-applicable**; Batch **solo** closed; Gates: `mvn verify` **GREEN 2133**/0/11; arch **PASS_WITH_NOTES** `merge_go=true`; Stage 10 ForceRebuild **DEPLOY_OK**; FE/E2E **N/A**). Prior **#121** IBL-C2 → **Done** (MAIN merge `1d357e4d` / includes feature tip `767b4ceb` with `abf564dc` code + stage10 evidence; worktree removed; F18 closed — side-by-side rendered PDF compare UI; BDD **ready** [ibl-c2-rendered-compare-ui.md](../behavior/ibl-c2-rendered-compare-ui.md); Batch **solo** closed; Gates: `pnpm lint/type-check/test/build` **GREEN 1556**; E2E functional **4/4** PASS; UIUX Stage 7 **PASS** Critical=0; arch **PASS_WITH_NOTES** `merge_go=true`; Stage 5 **DEPLOY_OK** + Stage 10 SkipBuild **DEPLOY_OK**). Prior **#120** IBL-C1 → **Done** (MAIN merge `55909dd2` / includes `d2492fc4` code + `5d89b606` evidence; prior tip `c77418aa`; worktree removed; F17 closed for layout-metric — PDFBox `PAGE_COUNT`/`TEXT_POSITION`; `PIXEL_*` still rejected; LO half **SKIPPED** honestly; BDD **not-applicable**; Batch **solo** closed; Gates: `mvn verify` **GREEN 2125**; arch **PASS_WITH_NOTES**; ForceRebuild **DEPLOY_OK**; LO **SKIPPED**; FE/E2E **N/A**). Prior **#118** IBL-B6 → **Done** (MAIN merge `8722f4f1` / includes `8e8c62e6`; also brought previously local `e2f0a505` B5 docs; prior remote `1666312b`; worktree removed; F16 closed — ADR-0060 + ops freeze; docs-only; arch **PASS_WITH_NOTES** `merge_go=true`; deploy/FE/E2E **N/A**). Wave **IBL-B** → **In Progress** (B1–B6 **Done**; B7 **Blocked**). Prior **#117** IBL-B5 → **Done** (MAIN merge `1666312b` / includes `d7459405` code + `fbb40429` evidence; prior main `fc16d508`; worktree removed; F14 declarative geometric validation **Done**; writer absolute positioning / visual placement **NOT** claimed — arch Suggestion #1 residual). Prior **#116** IBL-B4 → **Done** (MAIN merge `610eb0fa` / includes `d6b389d1` code + `d2e8a1c9` evidence; also brought previously unpushed B3 docs from `3de54cbc`; prior remote `3710811a`; worktree removed; F13 theme 08 **ACTIVE** + paginate/full retention). Prior **#115** IBL-B3 → **Done** (MAIN merge `3710811a` / `c81054b0`+`e0102ddb`; F12 veraPDF; ADR-0059 Accepted). Wave **IBL-A** remains **Done** (A1–A6; prior **#112** tip `5cacff2a`). **#114** IBL-B2 → **Done** (`29d022b6` / `3dd1aa60`+`36a9821c`). **#113** IBL-B1 → **Done** (`a33da272` / `44237c99`). **#106** registry-only **pending**. Formal phase **None**. Do **not** flip **#3b/#5a GO**. Do **not** claim go-live / IBL program Done. Prior **#112** IBL-A6 → **Done**. Prior **#111** IBL-A5 → **Done** (MAIN merge `e5217a80` / feature tip `99e8c1a7`; worktree removed). Prior **#110** IBL-A4 → **Done** (MAIN merge `ddf6601c` / feature tip `6edb41cd`; worktree removed). Prior **#109** IBL-A3 → **Done** (MAIN merge `779b1979` / feature tip `f09326ca`; worktree removed; international `SPELL_AMOUNT` en/USD). Prior **#108** IBL-A2 → **Done** (MAIN merge `e3000479` / feature tip `89584242`; worktree removed; ISO-currency `FORMAT_AMOUNT`). Prior **#107** IBL-A1 → **Done** (`f0a2b6fe` / `4bda5f2d`). Prior **#137** PRR-P2 audit hygiene → **Done** (MAIN merge `baaf16cc` / feature tip `09cf85ce`; worktree removed). Slice `prod-audit-p2-hygiene`. listAll removed; `components/template`→`templates`; knip unused exports **31→0**. BDD **not-applicable** ([prod-audit-p2-hygiene.md](../behavior/prod-audit-p2-hygiene.md)). Prior **#136** PRR-D01c → **Done** (`a872c15b` / `8c52ee67`); Wave D residuals **D01A+#135+#136** closed; prior **#135** → **Done** (`6e776232` / `1ada6b41`); prior **#104** D01A → **Done** (`f1f79d14`); prior **#103** PRR-C01 → **Done** (`3513ab92` / `6408c210` / tip `7a1a1bb1`; Path **X**; ADR-0042/0043 **Accepted**; **#3b** → **CONDITIONAL** ≠ GO — do **not** regress); prior **#105** → **Done** (`50448016`); **#102** → **Done** (`40e264e7`); **#101** → **Done** (`5b705f56`); **PRR Wave A** → **Done** (`4197770f`); **push residual:** MAIN may still be ahead of `origin/main` — do **not** claim origin synced; do **not** flip **#3b GO** / **#5a GO** (#5a stays **CONDITIONAL**); do **not** mark Wave A / IBL program Done. **CORE-EXCELLENCE (CE)** — **#77** CE-G05 → **Done** (merge `c3f6a288` / feature `744b628a`; worktree removed; BDD **ready** [ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md); FE+E2E); **P3 queue empty** (#96/#97/#80/#77 all Done); umbrella **#53** remains **in-progress** (program registry only — **do not** claim program Done); **#80** CE-E03 → **Done** (merge `f1f02554` / feature `86e4ff10`; worktree removed; BDD **ready** [ce-e03-full-library-export.md](../behavior/ce-e03-full-library-export.md); API-first FE/E2E N/A); **#97** CE-U19 → **Done** (merge `e4679421` / feature `90a9e5cd`; worktree removed); **#96** CE-U17 → **Done** (merge `4fc2dbdb` / feature `d3293db1`; worktree removed) — do **not** claim program Done; **P2 CE continuum complete** — **#95** CE-U21 → **Done** (merge `8d8f6f6d` / feature `292fbc35`; worktree removed); **#94** CE-U20 → **Done** (merge `b9327a11` / feature `b4b0b420`; worktree removed); **#93** CE-U18 → **Done** (merge `05e9f8e1` / feature `e05407f2`; worktree removed); **#92** CE-U16 → **Done** (merge `5d683c40` / feature `1a3d0f20`; worktree removed); **#91** CE-U15 → **Done** (merge `b2968052`; closeout `ed8a15e6`; worktree removed); **#90** CE-U14 → **Done** (merge `05e845e4` / feature `09e0d251`); **#50** Vitest 3.2.7 → **Done** (`6c8fff7d`; GHSA-5xrq-8626-4rwp **CLOSED**); **#75** CE-G04 → **Done** (merge tip `42745ea5` / feature `b47ea896`; worktree removed); **#81** CE-O01 → **Done** (merge `e081bcfa`; worktree removed); **#79** CE-E02 → **Done** (merge `5bd3611e`) — do **not** reopen; **#78** CE-E01 → **Done** (merge `6ae57974`) — do **not** reopen; **#76** CE-G06 → **Done** (merge `d8636232`) — do **not** reopen; **#71** CE-C06 → **Done** (merge `35f6f47d`) — do **not** reopen; **#89** CE-U13 → **Done** (merge `ccdfacda`); **#74** CE-G03 → **Done** (merge `50c1a524`); **#62** CE-K06 → **Done** (K06a `485a7f3e` + K06b `a689ca87` + K06c tip `76297d08`); **#69** CE-C04 / **#70** CE-C05 / **#88** CE-U06 → **Done** (merges `c7be8305` / `405f7cea` / `7734366e`); Wave 0 #61/#86/#87 **Done**; umbrella **#53** remains **in-progress** (program registry only); [core-excellence-program-2026-07.md](core-excellence-program-2026-07.md). Prior waves closed (#60/#84/#85; #59/#68/#83). **Batch 4 remains Done**. Batch 1–3 Done. Formal phase remains **None**; **not** go-live; do **not** activate CD-3; do **not** invent a formal P-phase. Overall checklist remains **CONDITIONAL** (**#3b** Path X; #5a/#10 residuals — **not GO**). **Scaffold hygiene (2026-07-14):** slice `cursor-scaffold-hygiene` — Cursor-only agent/docs; ADR-0055; see ledger. **Prior:** **BOOT-4-1-UPGRADE → Done** (Task Master **#51**; merge `993c287`). **CDP Wave CD-2 Done** (no CDP wave In Progress). **LRP A–E Done**.

**Active formal phase / program:** **None** (2026-07-09). **CODE-QUALITY program Done** (CQ-01A…CQ-08; ArchUnit **11/11**); see [program entry](code-quality-program.md) and [task sheet](detail/CODE-QUALITY-code-hygiene.md). **CORE-FORTRESS program Done** (F1–F8; 2026-07-09). **CORE-EXCELLENCE** is the active delivery program (not a formal P-phase). **LRP waves A–E → Done**. **CDP Wave CD-2 → Done** (T01–T13; **no CDP wave In Progress**). Historical LR-C/D/E and CD-E2E slice evidence: [execution-sync-ledger.md](./execution-sync-ledger.md) · [LRP program](launch-readiness-program.md) · [CDP program](competitiveness-deepening-program.md).

**Ad-hoc slice (2026-07-16):** **ORCH-AGENT-ENUM-DOCS → Done** — align AGENTS /
MODEL-STRATEGY / inline-agent wording with retry-first runtime policy. Formal phase
**None**. **#81** CE-O01 is **Done** (`e081bcfa`).

**Ad-hoc slice (2026-07-16):** **ORCH-SPECIALIST-RETRY-THEN-GP → Done** — retry ≤3
then GP under contract (`orch-specialist-retry-then-gp`;
[specialist-runtime-fallback.md](../behavior/specialist-runtime-fallback.md);
[orch-specialist-fallback-governance.md](./orch-specialist-fallback-governance.md)).
Forbid: `禁止降级` / `no-gp-fallback`. Early opt-in: `允许降级` / `allow-gp-fallback`.
Formal phase **None**. Placement **MAIN** (`main-only`). Committed with CE-G04 closeout.

**Ad-hoc slice (2026-07-16):** **ORCH-SPECIALIST-RETRY-ONLY → superseded** by
retry-then-GP policy above.

**Ad-hoc slice (2026-07-16):** **ORCH-SPECIALIST-FALLBACK → superseded** (historical).

**Ad-hoc slice (2026-07-16):** **ORCH-BATCH-RECOMMEND → Done** — pre-0 Batch Recommendation
governance (`orch-batch-recommend`; skill + constitution + BDD
[delivery-batch-recommend.md](../behavior/delivery-batch-recommend.md);
note [orch-batch-recommend-governance.md](./orch-batch-recommend-governance.md)). Formal phase
remains **None**. **#81** CE-O01 → **Done** (`e081bcfa`; not activated by this governance slice). Do **not** invent a P-phase.

**Ad-hoc slice (2026-07-10):** **[MGMT-UI-DEFECTS](detail/MGMT-UI-defects.md)** — Round 2 / P0 **Done**; **Round 3 / P1 depth governance Done** (`mgmt-ui-p1`; merge `180bffb`; worktree removed).

**Formal phase P23:** **[P23 Demo typography & layout excellence](detail/P23-demo-typography-layout-excellence.md)** — **Done** (2026-07-08; T01–T16; bank-grade Word output for all demo packages).
**Formal phase P22:** **Done** (2026-07-04) — rendering engine + demo scaffolds; [P22 detail](detail/P22-demo-expansion-rendering-fidelity.md).
**P21 Done** (2026-06-30; T01–T11 + X01–X06 + X02; backend **553**,
frontend **511** Vitest; **AUD-B10 resolved** via P12-AUD-B10; **AUD-M02 resolved** via P12-AUD-M02). Latest gates: backend `mvn verify` BUILD SUCCESS + frontend **643** Vitest (2026-07-03, P12-TEMPLATE-TESTING-OVERHAUL). See
[execution-sync-ledger.md](./execution-sync-ledger.md).

## Layer model

```text
docs/plan/master-plan.md              ← Overall plan (phase granularity)
    └── docs/plan/detail/P*.md        ← Detailed tasks & design per phase
docs/plan/execution-sync-ledger.md    ← Epic/milestone mirror + evidence
docs/architecture/orchestration-high-level-plan.md   ← Epic ordering (reference)
docs/architecture/implementation-task-plan.md        ← Technical waves (reference)
docs/architecture/m*-task-sheet.md                   ← Milestone task decomposition
docs/architecture/e*-task-sheet.md                   ← Epic task decomposition
```

## Rules

1. Exactly **one phase** may be `In Progress` at a time.
2. Status vocabulary: `Not Started` | `In Progress` | `Blocked` | `Done`.
3. Prior completion claims were void at reset; re-earned status is recorded in
   [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md) and this layer.
4. `Done` requires real behavior + green gates — never demo/in-memory/mock-only.
5. Behavior-changing work needs a behavior spec before task decomposition.
6. **Post-task doc sync is mandatory** before claiming Done (see
   `.cursor/agents/post-task-doc-sync.md`).

## Phase overview

| Phase | Name | Detailed plan | Status |
| --- | --- | --- | --- |
| P0 | Foundation & guardrails | [detail/P0-foundation.md](detail/P0-foundation.md) | Done |
| P1 | Login & session | [detail/P1-login-session.md](detail/P1-login-session.md) | Done |
| P2 | Master document management | [detail/P2-master-management.md](detail/P2-master-management.md) | Done |
| P3 | Template authoring | [detail/P3-template-authoring.md](detail/P3-template-authoring.md) | Done |
| P4 | Rendering & preview | [detail/P4-rendering-preview.md](detail/P4-rendering-preview.md) | Done |
| P5 | Lifecycle governance | [detail/P5-lifecycle-governance.md](detail/P5-lifecycle-governance.md) | Done |
| P6 | API management | [detail/P6-api-management.md](detail/P6-api-management.md) | Done |
| P7 | Runtime dynamic API | [detail/P7-runtime-api.md](detail/P7-runtime-api.md) | Done |
| P8 | Audit & contract visibility | [detail/P8-audit-contract.md](detail/P8-audit-contract.md) | Done |
| P9 | Production readiness | [detail/P9-production-readiness.md](detail/P9-production-readiness.md) | Done |
| P10 | Runtime document download | [detail/P10-runtime-download.md](detail/P10-runtime-download.md) | Done |
| P11 | Batch & async generation | [detail/P11-batch-async.md](detail/P11-batch-async.md) | Done |
| P12 | Deferred enhancements | [detail/P12-deferred-enhancements.md](detail/P12-deferred-enhancements.md) · [API package access (Done)](detail/P12-api-package-access-invocation-records.md) | **Not Started** — slices **P12-TEMPLATE-TESTING-OVERHAUL Done**, **P12-API-PACKAGE-ACCESS-INVOCATION Done** (2026-07-03) |
| P13 | Identity & group administration | [detail/P13-identity-group-administration.md](detail/P13-identity-group-administration.md) | Done |
| P14 | Confirmed large domains | [detail/P14-confirmed-large-domains.md](detail/P14-confirmed-large-domains.md) | Done (2026-06-27) |
| P15 | Kubernetes deployment & container hardening | [detail/P15-kubernetes-deployment-container-hardening.md](detail/P15-kubernetes-deployment-container-hardening.md) | Done (2026-06-27; T01–T10) |
| P16 | Template & version lifecycle governance completeness | [detail/P16-lifecycle-version-governance.md](detail/P16-lifecycle-version-governance.md) | Done (2026-06-23) |
| P17 | Per-domain API policy governance | [detail/P17-api-policy-domain-governance.md](detail/P17-api-policy-domain-governance.md) | Done (2026-06-25; Wave 3) |
| P18 | Structured authoring & rendering-fidelity engine | [detail/P18-structured-authoring-fidelity-engine.md](detail/P18-structured-authoring-fidelity-engine.md) | Done (2026-06-28; T01–T10) |
| P19 | Template verifiability, publish gate & decision forms | [detail/P19-verifiability-publish-gate.md](detail/P19-verifiability-publish-gate.md) | Done (2026-06-25; T01–T10) |
| P20 | i18n multi-locale readiness & UI upgradeability | [detail/P20-i18n-ui-upgradeability.md](detail/P20-i18n-ui-upgradeability.md) | Done (2026-06-25) |
| P21 | Role-journey frontend redesign & business-friendly terminology | [detail/P21-role-journey-frontend-redesign.md](detail/P21-role-journey-frontend-redesign.md) | Done (2026-06-30; T01–T11 + X01–X06 + X02; AUD-B10 resolved P12-AUD-B10 Done 2026-07-01; AUD-M02 resolved P12-AUD-M02 Done 2026-07-01) |
| P22 | Demo expansion & rendering fidelity | [detail/P22-demo-expansion-rendering-fidelity.md](detail/P22-demo-expansion-rendering-fidelity.md) | **Done** (2026-07-04; engine + scaffolds; typography → P23) |
| P23 | Demo document typography & layout excellence | [detail/P23-demo-typography-layout-excellence.md](detail/P23-demo-typography-layout-excellence.md) | **Done** (2026-07-08; T01–T16; POI + E2E + human checklist template) |

## First delivery target (thin vertical slice) — achieved

Login → upload approved master → create template → test → approve → publish →
call runtime API → receive DOCX. Mapped to **P0–P7 (minimal sync path)** — Done.

## Next focus

**User sequence P14 → P15 → P18 — complete** (2026-06-28).

| Phase | Summary | Evidence |
| --- | --- | --- |
| P14 | Content modules, collaboration, export/import | E2E/UIUX green; see [P14 detail](detail/P14-confirmed-large-domains.md) |
| P15 | K8s Helm, probes, blue-green, CI gates | [deploy/README.md](../deploy/README.md); helm-validate green |
| P18 | Structured authoring + fidelity engine + UI | `mvn verify` **524** tests; Vitest **250**; Playwright P18-T10 **5/5** + UIUX **1/1** |

**In-flight work:**
- **CDP Wave CD-2 Done** (2026-07-11) — CD-E2E-T01–T13 Done, merges `1930842` / `6821f45` / `895f16e` / `3aed175` / `c62b1a1` / `1eb230b` / `55a6ab6` / `b16e52a` / `6e3f825` / `f12b193` / `b2b0899`; **CD-0 Done** — [program](./competitiveness-deepening-program.md); [detail](detail/CDP-e2e-full-chain-evidence.md)
- **LRP Wave LR-A Done** (2026-07-10) — A1–A7 Done; **ADR-0041 Accepted**; 0042/0043 remain Proposed; Word/XSD/LO24/0042-Accepted/0043-Accepted residuals deferred out of wave exit — [program](./launch-readiness-program.md); [detail](detail/LRP-A-rendering-trust-hardening.md); [corpus](./pagination-delta-corpus.md)
- **CODE-QUALITY Done** (2026-07-09) — code hygiene & structural consistency ([program](code-quality-program.md); [detail](detail/CODE-QUALITY-code-hygiene.md))
- **CORE-FORTRESS Done** (2026-07-09; F1–F8 — [program roadmap](detail/CORE-FORTRESS-program-roadmap.md))
- **P23 Done** (2026-07-08) — demo typography & layout excellence ([plan](detail/P23-demo-typography-layout-excellence.md); T01–T16; human reviewer sign-off operational follow-up)

**P21 Done** — closure evidence in [detail/P21-role-journey-frontend-redesign.md](detail/P21-role-journey-frontend-redesign.md).

**Open backlog (non-active slices):**
- **SOR-0…7** — consolidated system optimization review (2026-07-03; 52 tasks, all Not Started) — see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md)
- **OPT-D / M9** — architecture/security debt (Redisson, QueryDSL expansion, intranet SCA) — see [optimization-plan.md](./optimization-plan.md)
- **OPT-G6 (partial)** — brand rename Done via P12; `logoSlotLabel` proper-noun exempt (LR-C11); locale dates + catalog residual **closed by LR-C11** (merge `44fcf40`); **aria-label sweep remains**
- **OPT-G7 Done** — `api.error` frontend/backend parity **159/159** + Vitest (LR-C11 Done 2026-07-11)
- ~~**Phase B** — multi-revision master history API~~ → **Done (P2-T06, 2026-07-01)** — see [P2 detail](detail/P2-master-management.md) § P2-T06 and [catalog-navigation-ux.md](../product/catalog-navigation-ux.md)

## Optimization backlogs

| Backlog | Lens |
| --- | --- |
| **[code-quality-program.md](./code-quality-program.md)** | **CODE-QUALITY program Done (2026-07-09):** behavior-preserving hygiene — module boundaries, god-class extraction, DRY demo/PDF/E2E infra (`CQ-*`) |
| **[system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md)** | **Consolidated optimization review (SOR-0…7, 2026-07-04 closeout)** — open hygiene items superseded by **CODE-QUALITY** where duplicated |
| **[core-excellence-program-2026-07.md](./core-excellence-program-2026-07.md)** | **Core Excellence（CE-K/U/C/G/E/O）** — Task Master **#53–#97**; **#77** CE-G05 **Done** (`c3f6a288` / `744b628a`); **P3 queue empty**; umbrella **#53** in-progress (not program Done); **host sole-active #131** IBL-E4 (`ibl-e4-entity-document-brands`) — NON-CE; do not treat #53 as delivery leaf; OOB session writer cleared) (OOB `nav-missing-icons` Done `137a115f` / `47784667`; prior NON-CE **#127** IBL-D5 Done `6f672271` / `2e56787e`; F23 legalhold half closed; Wave IBL-D → Done D1–D5; Playwright residual OUT; prior NON-CE **#126** IBL-D4 Done `94cc8eeb` / `94526674`; F22 closed; prior NON-CE **#125** IBL-D3 Done `6b9b6487` / `0242084e`; F22 load half; prior NON-CE **#124** IBL-D2 Done `21be3a99` / `4fd0c5da`; F21 closed; Wave IBL-C → **Done** C1+C2+C3; prior **#123** Done `1a686938` / `f399489c`; prior **#122** Done `bdfc285d` / `dbfff086`; prior NON-CE **#121** IBL-C2 Done `1d357e4d` / `767b4ceb`+`abf564dc`; prior NON-CE **#120** IBL-C1 Done `55909dd2` / `d2492fc4`+`5d89b606`; Wave IBL-B **In Progress** B1–B6 Done / B7 Blocked residual; prior NON-CE **#118** Done `8722f4f1` / `8e8c62e6`); prior NON-CE **#117** IBL-B5 Done `1666312b` / `d7459405`+`fbb40429`; prior NON-CE **#116** IBL-B4 Done `610eb0fa` / `d6b389d1`+`d2e8a1c9`; Wave IBL-A **Done**; prior **#115** Done `3710811a` / `c81054b0`+`e0102ddb`; prior **#114** Done `29d022b6` / `3dd1aa60`+`36a9821c`; prior **#113** Done `a33da272` / `44237c99`; prior NON-CE **#112** IBL-A6 **Done** `5cacff2a`; prior NON-CE **#111** IBL-A5 **Done** `e5217a80` / `99e8c1a7`; prior NON-CE **#110** IBL-A4 **Done** `ddf6601c` / `6edb41cd`; prior NON-CE **#109** IBL-A3 **Done** (`779b1979` / `f09326ca`); prior NON-CE **#108** IBL-A2 **Done** (`e3000479` / `89584242`); prior NON-CE **#107** IBL-A1 **Done** (`f0a2b6fe` / `4bda5f2d`); prior NON-CE **#137** PRR-P2 **Done** (`baaf16cc` / `09cf85ce`); **#136** D01C **Done** (`a872c15b` / `8c52ee67`); **#135** D01B **Done** (`6e776232` / `1ada6b41`); **#104** D01A **Done** (`f1f79d14`); Wave D residuals **D01A+#135+#136** closed; prior **#103** Done `6408c210` / `3513ab92`; **#105** Done `50448016`; **#3b** → **CONDITIONAL** (≠ GO); do **not** flip **#3b GO** / **#5a GO**; **Batch 1–4 Done**; Wave 0 #61/#86/#87 **Done**; formal phase **None** (optimization backlog, not a P-phase) |
| **[orch-batch-recommend-governance.md](./orch-batch-recommend-governance.md)** | **Agent governance (2026-07-16)** — pre-0 Batch Recommendation (`merge`\|`solo`\|`split`); not a P-phase; #81 CE-O01 later **Done** |
| **[comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)** | **Unified prioritized map (COR-0…6): docs sync, API contract, template workflow, frontend UX, performance, E2E, P14–P20 — start here (2026-06-23)** |
| [launch-readiness-program.md](./launch-readiness-program.md) | Launch-readiness deep optimization — production pitfalls + usability deepening (LR-A…E; sibling program; **Wave LR-C Done**; **Wave LR-D Done** — D1–D7; merge tip `218dcf1`; **Wave LR-E Done** — E1 merge `575d0aa` #42; E2 merge `ae39fbb` #43; docs gate; checklist **NO-GO**; **LRP planned waves → Done**) |
| [intl-bank-letter-readiness-program.md](./intl-bank-letter-readiness-program.md) | **IBL (Wave E In Progress — #128 IBL-E1 Done `4c712b03`/`9decf19c` F24 closed; #129 IBL-E2 Done `81a1ca29`/`6a96e9ab` F25 closed; #130 IBL-E3 Done `233342d3`/`e81a6bac`/`3bd2cd87` F26 closed; #131 IBL-E4 In Progress sole-active `ibl-e4-entity-document-brands`; Wave D Done D1–D5; Wave C Done; Wave B residual In Progress B7 Blocked; Wave A Done)** — TM **#106** registry-only pending + **#107–#118/#120–#130** **Done** + **#131** **in-progress** + **#132–#134** pending (not activated) + **#119** Blocked; F26/F25/F24/F23/F22/F21/F20 closed; formal phase **None**; do **not** flip **#3b/#5a GO**; do **not** invent confirmed NFR SLOs; do **not** claim Wave B / Wave E / IBL program Done |
| [optimization-plan.md](./optimization-plan.md) | Technical debt detail: quality gates, coverage, backend architecture/security/performance (OPT-A…G) |
| [spotbugs-exclusion-ratchet.md](./spotbugs-exclusion-ratchet.md) | SpotBugs exclude.xml ratchet (SOR-A05): baseline `<Match>` count, banned patterns, EI slice cadence |
| [coverage-ratchet-plan.md](./coverage-ratchet-plan.md) | JaCoCo / Vitest bundle floors (SOR-C02) |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G); verify Done claims against comprehensive roadmap |
