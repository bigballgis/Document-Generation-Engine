# International Bank Letter Readiness Program (IBL) 「国际跨国大型银行金融信函就绪」

**Program ID:** `IBL`  
**Created:** 2026-07-17  
**Status:** **Wave IBL-A In Progress** (A1–A3 **Done**; A4–A6 **Not Started** — do **not** mark Wave A or program Done). Program registered **2026-07-18**; A1 closed **2026-07-18** (MAIN merge `f0a2b6fe`); A2 closed **2026-07-18** (MAIN merge `e3000479`); A3 closed **2026-07-18** (MAIN merge `779b1979`). Checklist **#3b** is **CONDITIONAL** (PRR-C01 Path X; merge `3513ab92`) — do **not** flip **#3b GO** or claim go-live from IBL.  
**Formal phase / program:** **None** (cross-cutting optimization backlog; same genre as LRP / SOR — **not** a new P-phase).  
**North star:** Close the verified gaps between today’s Document Generation Platform and **international multinational bank financial-letter readiness** — fail-closed filling, ISO-correct amounts, high-fidelity Word/PDF trust, layout regression evidence, realistic CI/integration, and (only after ADR/user confirmation) multinational content models.  
**Task ID prefix:** `IBL-*` (International Bank Letter readiness) — verified free of collision with `LR-*` / `CD-*` / `SOR-*` / `CQ-*` / `CE-*` / `OPT-*` / `COR-*` / `PRR-*`.  
**Task Master:** Umbrella **#106** (registry only, **`pending`**) + leaves **#107–#134** (28 tasks = A6+B7+C3+D5+E7). **#107** IBL-A1 → **`done`**; **#108** IBL-A2 → **`done`**; **#109** IBL-A3 → **`done`**; **#110–#134** remain **`pending`** (B7 + Wave E descriptions start with **BLOCKED**). See § Task Master ID map.  
**Audit provenance:** Four-track (+ multinational template track) **read-only** deep audit, evidence spot-verified **2026-07-17**. Findings F1–F28 below are **confirmed symptoms** (code/docs evidence). Proposed remediations are **not** confirmed product requirements until activation + (where noted) ADR/user decision.

**Queue policy (critical):** Host serial delivery queue: PRR **#105 Done** (`50448016`); **#103 Done** (`3513ab92` / `6408c210`); **#104** PRR-D01a (**D01A**) → **Done** (`f1f79d14`); **#135** PRR-D01b → **Done** (`6e776232` / `1ada6b41`); **#136** PRR-D01c → **Done** (`a872c15b` / `8c52ee67`; Wave D residuals **D01A+#135+#136** closed); **#137** PRR-P2 audit hygiene → **Done** (`baaf16cc` / `09cf85ce`); **#107** IBL-A1 → **Done** (`f0a2b6fe` / `4bda5f2d`; worktree removed); **#108** IBL-A2 → **Done** (`e3000479` / `89584242`; worktree removed); **#109** IBL-A3 → **Done** (`779b1979` / `f09326ca`; worktree removed). **Sole-active cleared** (no delivery leaf). Umbrella **#106** is **registry only** — **not** a sole-active delivery leaf. **Next queue (not activated):** IBL-A4 **#110**. A4–A6 / Waves B–E stay **Not Started** / **Blocked** until next serial activation.

**Authoritative entry for lower-tier implementers:** Read this file first. Execute only `IBL-*` task IDs after wave activation. Do **not** invent a formal phase. Do **not** touch [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) from IBL work unless a task explicitly owns a checklist cross-link.

| Sibling / related document | Relationship |
| --- | --- |
| [master-plan.md](./master-plan.md) | Formal phase accounting remains **None**; IBL never changes phase status |
| [launch-readiness-program.md](./launch-readiness-program.md) | Sibling LRP (`LR-*`) — waves A–E **Done**; IBL inherits residuals (e.g. ADR-0042 Accepted + Path X / Path E for #3b GO, DEF-LRP-D6-001, Bucket4j/ADR-0039) without re-owning closed LR rows |
| [competitiveness-deepening-program.md](./competitiveness-deepening-program.md) | Sibling CDP (`CD-*`) — CD-2 Done; CD-3 Not Started; do not execute `CD-*` from IBL |
| [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md) | Historical SOR inventory; IBL is the **2026-07-17** bank-letter readiness consolidation |
| [core-excellence-program-2026-07.md](./core-excellence-program-2026-07.md) | Active delivery program CE (`CE-*` / Task Master); coordinate — do not fold IBL into an In Progress CE/PRR leaf |
| [execution-sync-ledger.md](./execution-sync-ledger.md) | **#109** IBL-A3 → **Done** (`779b1979` / `f09326ca`); **#108** IBL-A2 → **Done** (`e3000479` / `89584242`); **#107** IBL-A1 → **Done** (`f0a2b6fe` / `4bda5f2d`); Wave IBL-A In Progress (A1–A3 Done; A4–A6 Not Started); sole-active cleared; next **#110** not activated |
| [non-functional-requirements.md](../requirements/non-functional-requirements.md) | NFR SLOs remain **proposed — awaiting confirmation**; IBL-D3 feeds confirmation, never invents confirmed SLOs |
| [ADR-0042](../adr/rendering-authoring/0042-pagination-delta-budget.md) (pagination — **Accepted** + Path X residual) | IBL-B7 owns Path E Word measurement path toward checklist **#3b GO**; Word host is an external dependency |

**Rules:**

1. All IBL tasks start **`Not Started`** (or **`Blocked`** where a pending decision / external host blocks). Nothing in this document is pre-claimed `Done`.
2. Only **one IBL wave** may be `In Progress` at a time (same discipline as LRP/SOR). Repo delivery remains **single-lane serial**.
3. Behavior-changing tasks require BDD **`required`** (ready spec in `docs/behavior/`) before implementation; refactor/infra/docs tasks use **`not-applicable`**.
4. TDD + green gates + post-task doc sync + post-task commit review before any task `Done`.
5. **Confirmed facts vs pending questions:** §1 findings are evidence-grounded symptoms. §Pending decisions「待确认」items **must not** be scheduled as ordinary activatable work until user/PRD/ADR confirmation. Never promote a recommendation into a confirmed requirement.
6. This document does **not** alter formal phase status or the launch-readiness checklist overall verdict. Task Master **is** registered (#106–#134, all `pending`); registration alone does **not** create a formal phase, activate a wave, or flip checklist **#3b GO**.
7. Do not duplicate ownership of closed LRP/CDP/CE rows — reference and extend; never re-open Done waves without user direction.

---

## 0. Executive summary 「执行摘要」

### 0.1 What we are NOT doing 「明确不做」(until pending decisions confirm otherwise)

- **No outbound delivery orchestration** (print / email / ECM / registered mail) — v1 PRD: upstream systems invoke the runtime API.
- **No pixel/visual PDF regression** until a new ADR revises the golden-corpus stance (current corpus + ADR forbid `PIXEL_*`).
- **No Word-host measurement invention** — ADR-0042 acceptance needs a licensed MS Word host; do not fabricate baselines.
- **No locale-variant template model or jurisdiction rule engine** until ADR + user confirmation (F24/F25).
- **No non-specimen re-issue** until legal/compliance decides watermark policy (F6 specimen path stays until then).
- **No licensed font procurement** (true Calibri etc.) from this program alone — licensing is external.
- **No stack replacement** — Java 25 / Spring Boot 4 / Vue 3 / LibreOffice / PostgreSQL per accepted ADRs; new tools (veraPDF, Testcontainers, k6, …) enter only via dependency-policy verification + ADR where required.
- **No production go-live claim** and **no checklist #3b flip** from IBL authorship or Task Master registration alone.

### 0.2 What we ARE proposing (five waves)

| Wave | Axis | Problem today (audit) | Proposed outcome |
| --- | --- | --- | --- |
| **IBL-A** | Filling correctness & consumer contract 「填充正确性与消费者契约」 | Required variables silent-blank; currency/locale wrong; contract API shallow; PII retention unreacted; regenerate loses locale | Fail-closed variable validation; ISO amount + intl amount-in-words; per-field contract + break gate; PII redaction; locale-faithful regenerate replay |
| **IBL-B** | Rendering fidelity trust 「渲染保真信任」 | Direct-format lie; PDF capacity DEF-LRP-D6-001; PDF/A metadata-only; long-clause placeholder; seal boolean-only; reproducibility unfrozen; ADR-0042 unproven | Paragraph spacing applied; PDF capacity plan; veraPDF gate; overflow policy; seal geometry; freeze doc; Word baseline + budget (Blocked on host) |
| **IBL-C** | Comparison & regression evidence 「对比与回归证据」 | Golden = XML/text only; no layout metrics; no side-by-side render compare; weak cross-locale matrix | PDFBox layout-metric regression; FE side-by-side compare; en/zh/multi-currency LO goldens |
| **IBL-D** | Test & integration realism 「测试与集成真实性」 | H2/Flyway-off default verify; LO tests skip; no k6; thin legalhold/E2E subset | Testcontainers+Flyway lane; mandatory LO CI lane; k6 + SLO confirmation path; LO chaos; legalhold depth |
| **IBL-E** | Multinational content model 「跨国内容模型」 | One body/locale; no jurisdiction engine; single approval track; UI-only brands; weak effectiveFrom/bulk; nesting governance gap; no RTL | **Blocked** on §Pending decisions — ADR-first then implement |

**Task count:** **28** scheduled tasks (A1–A6 + B1–B7 + C1–C3 + D1–D5 + E1–E7) + deferred residuals F7/F8 (not scheduled — see §1 / §Open questions). Task Master leaves **#107–#134** map 1:1 (see § Task Master ID map).

---

## Task Master ID map 「Task Master 映射」

Registered **2026-07-18** under `.taskmaster/tasks/tasks.json`. Program status = **Wave IBL-A In Progress** (A1–A3 **Done**; A4–A6 **Not Started**). TM status column below mirrors Task Master; plan-layer wave tables use `Not Started` / `In Progress` / `Blocked` / `Done` vocabulary.

| IBL-* | TM id | Status |
| --- | --- | --- |
| umbrella (program registry) | **106** | pending (registry only — **not** a delivery leaf) |
| IBL-A1 | **107** | **done** |
| IBL-A2 | **108** | **done** |
| IBL-A3 | **109** | **done** |
| IBL-A4 | **110** | pending |
| IBL-A5 | **111** | pending |
| IBL-A6 | **112** | pending |
| IBL-B1 | **113** | pending |
| IBL-B2 | **114** | pending |
| IBL-B3 | **115** | pending |
| IBL-B4 | **116** | pending |
| IBL-B5 | **117** | pending |
| IBL-B6 | **118** | pending |
| IBL-B7 | **119** | pending (**BLOCKED** in description — Word host / ADR-0042) |
| IBL-C1 | **120** | pending |
| IBL-C2 | **121** | pending |
| IBL-C3 | **122** | pending |
| IBL-D1 | **123** | pending |
| IBL-D2 | **124** | pending |
| IBL-D3 | **125** | pending |
| IBL-D4 | **126** | pending |
| IBL-D5 | **127** | pending |
| IBL-E1 | **128** | pending (**BLOCKED** in description — §Pending decisions) |
| IBL-E2 | **129** | pending (**BLOCKED** in description — §Pending decisions) |
| IBL-E3 | **130** | pending (**BLOCKED** in description — §Pending decisions) |
| IBL-E4 | **131** | pending (**BLOCKED** in description — §Pending decisions) |
| IBL-E5 | **132** | pending (**BLOCKED** in description — §Pending decisions) |
| IBL-E6 | **133** | pending (**BLOCKED** in description — §Pending decisions) |
| IBL-E7 | **134** | pending (**BLOCKED** in description — §Pending decisions) |

**Queue note:** Wave D residuals **D01A+#135+#136** closed (**#136** Done `a872c15b` / `8c52ee67`; **#135** `6e776232` / `1ada6b41`; **#104** `f1f79d14`; **#103/#105** Done). Audit P2 **#137** → **Done** (`baaf16cc` / `09cf85ce`). **#107** IBL-A1 → **Done** (`f0a2b6fe` / `4bda5f2d`). **#108** IBL-A2 → **Done** (`e3000479` / `89584242`). **#109** IBL-A3 → **Done** (`779b1979` / `f09326ca`). **Sole-active cleared**. **Next queue (not activated):** IBL-A4 **#110**. Do **not** treat #106 as sole-active. A4–A6 / later waves stay pending.

---

## 1. Evidence-grounded findings 「证据发现」

Verified **2026-07-17** (read-only). Implementers must re-verify paths before coding.

| # | Finding 「症状」 | Evidence | Risk | IBL task |
| --- | --- | --- | --- | --- |
| **F1** | Runtime generate does **not** enforce `VariableSchemaEntity` required/type — missing vars render as empty strings silently | `DocumentGenerationAssemblySupport` calls compute only; `VariableSchemaEntity` has `required_flag`/types/enum/`piiCategory` but no runtime validation call | **Critical** — silent wrong letters | **IBL-A1** |
| **F2** | `FORMAT_AMOUNT` uses `NumberFormat.getCurrencyInstance(locale)` — locale default currency, not ISO code+amount | `ComputeExpressionEvaluator.evalFormatAmount` | **Critical** — EUR under `en-US` prints `$` | **IBL-A2** |
| **F3** | Amount-in-words only Chinese CNY (`SpellAmountCn`); engine default locale `zh-CN` | `SpellAmountCn`; `ComputeDslLimits.DEFAULT_LOCALE` | **Critical** — no “USD One Thousand Only” | **IBL-A3** |
| **F4** | `/contract` API publishes hardcoded schema names, not per-field variable schemas; no consumer contract tests; placeholder renames lack API compatibility gate | `ContractAssemblyService` | **High** — consumer break risk | **IBL-A4** |
| **F5** | Retained invocation parameters store full variables (ADR-0057); `VariablePiiCategory` not applied as retention redaction | Invocation retention path; `VariablePiiCategory` | **High** — PII at rest | **IBL-A5** |
| **F6** | Regenerate is SPECIMEN-watermarked and replays with `locale=null` — not faithful re-issue; original locale lost | `InvocationRegenerationService` | **Medium** — locale loss (**specimen policy** is §Pending) | **IBL-A6** (locale); watermark → §Pending |
| **F7** | Bucket4j rate limit in-process only — multiplies under scale-out | `RuntimeRateLimitService`; ADR-0039 residual | **Medium** | **Deferred residual** (not scheduled; see §Open Q) |
| **F8** | No timezone/as-of date semantics; `FORMAT_DATE` forces UTC via `java.util.Date` | `FORMAT_DATE` / date formatting path | **Medium** | **Deferred residual** (not scheduled; see §Open Q) |
| **F9** | Direct-format whitelist permits lineSpacing/spacingBefore/After/indents, but writer only applies fontFamily/fontSize/textColor | `DirectFormatRules` vs `StructuredContentDocxStyleSupport.applyDirectFormatIfPresent` | **Critical** — authors trust false controls | **IBL-B1** |
| **F10** | Word↔LibreOffice pagination trust still unproven (Path X); ADR-0042 **Accepted** with Word n/a residual + metadata-gated enforcement; checklist **#3b CONDITIONAL** (≠ GO) | ADR-0042; Path X exemption; Path E / Word host; launch checklist #3b | **Critical** (Path E residual) | **IBL-B7** (**Blocked** on Word host for Path E / #3b GO) |
| **F11** | PDF conversion capacity — pool default 2, queue 0 fail-fast; LR-D6: 8/10 concurrent PDF sync failures | `DocgenRenderingProperties`; DEF-LRP-D6-001; `docs/plan/evidence/lrp-d6-load-smoke/` | **Critical** | **IBL-B2** |
| **F12** | PDF/A-2b verification is `pdfaid` XMP metadata only — no veraPDF in verify | `PdfAidXmpAssertor` | **High** | **IBL-B3** |
| **F13** | Golden theme `08-long-clause-limits` PLACEHOLDER — no overflow/truncation/page-break policy | Golden theme 08 | **High** | **IBL-B4** |
| **F14** | Seal placement `withinAuthorizedArea` is a boolean JSON flag — no geometric validation | `ReferenceNodeService.validateSealRef` | **High** | **IBL-B5** |
| **F15** | No RTL/bidirectional script support in rendering package | rendering package inventory | **Medium** | **IBL-E7** (spike; Wave E blocked) |
| **F16** | No deterministic legal-reproducibility freeze (LO version + font set + content-hash PDF baselines) | ops/rendering docs gap | **Medium** | **IBL-B6** |
| **F17** | Golden corpus = DOCX XML/XPath + PDF text-extract only; `PIXEL_*` rejected; zero baseline PDF binaries; no layout regression | `GoldenCorpusAssertionLoader`; golden-corpus README | **High** | **IBL-C1** (layout-metric, **not** pixel) |
| **F18** | No side-by-side visual compare of two **rendered** outputs in UI | `PreviewComparisonService` (warnings); `ChangeDiffService` / `SemanticContentDiffEngine` (semantic) | **Medium** | **IBL-C2** |
| **F19** | Cross-locale/multi-script matrix incomplete; Chinese-amount theme only; several golden PDF halves SYNTHETIC | golden corpus themes | **Medium** | **IBL-C3** |
| **F20** | Default `mvn verify` uses H2 (PG mode), Flyway off, Redis/Kafka excluded; zero Testcontainers | `backend/src/test/resources/application-test.yml` | **High** | **IBL-D1** |
| **F21** | LibreOffice-dependent tests skip silently when `soffice` missing — green without conversion | font smoke / parallel conversion IT / LIBREOFFICE golden halves | **High** | **IBL-D2** |
| **F22** | No industry load tool (k6/Gatling); NFR SLOs still proposed; no LO pool chaos suite | `docs/requirements/non-functional-requirements.md` | **Medium** | **IBL-D3**, **IBL-D4** |
| **F23** | `legalhold` has only 2 test classes; Docker E2E smoke subset only 9 of 162 Playwright specs | legalhold tests; Playwright docker subset | **Medium** | **IBL-D5** |
| **F24** | No locale/language template or clause variants — one body per package | `TemplateEntity` / `CreateTemplateRequest` lack locale | **Critical** — **needs ADR + user** | **IBL-E1** (**Blocked**) |
| **F25** | No jurisdiction/product/channel-driven composition engine — jurisdiction optional CM metadata + filter + expiry (CE-K08); inclusion via author expressions | CM/jurisdiction paths | **Critical** — **needs user** | **IBL-E2** (**Blocked**) |
| **F26** | Legal metadata optional; single approval track; no legal→compliance multi-stage matrix; no forced legal-reviewer role | lifecycle/approval model | **High** — **needs user** | **IBL-E3** (**Blocked**) |
| **F27** | No per-legal-entity document brand variants (REDBC/GREENBC = UI theming); no bulk re-pin/mass-migration beyond single import/export; `effectiveFrom` future-dating not enforced at publish | brand/publish/import paths | **High** | **IBL-E4**, **IBL-E5** (**Blocked**) |
| **F28** | Clause-in-clause nesting not a governed module graph; where-used may miss deep nesting | content-module graph | **Medium** | **IBL-E6** (**Blocked**) |

---

## 2. Wave map 「波次与依赖」

| Wave | Name | Priority | Type | Depends on | Parallelism / notes |
| --- | --- | --- | --- | --- | --- |
| **IBL-A** | Filling correctness & consumer contract 「填充正确性与消费者契约」 | **P0 — activate first** | Code + BDD | — | A1–A6 independent of Word host; A6 does **not** remove SPECIMEN without §Pending |
| **IBL-B** | Rendering fidelity trust 「渲染保真信任」 | **P0/P1** | Code + ADR + infra | B2 may coordinate LRP-D6 evidence; B7 **Blocked** on Word host | B1/B2/B3/B4/B5/B6 schedulable after A or in series after A exit; only one wave In Progress |
| **IBL-C** | Comparison & regression evidence 「对比与回归证据」 | **P1** | Test + FE | C1 benefits from B3/B4; C3 benefits from A2/A3 | After IBL-A (recommended) and core IBL-B |
| **IBL-D** | Test & integration realism 「测试与集成真实性」 | **P1** | CI + test infra | D2/D4 align with B2; D3 needs NFR confirmation path | Can follow or interleave after A; still one wave at a time |
| **IBL-E** | Multinational content model 「跨国内容模型」 | **P2 — Blocked** | Domain + ADR | **All tasks Blocked** on §Pending decisions | Do **not** activate until user/ADR confirmations |

**Rules (wave discipline):**

1. Within IBL, only **one wave** may be `In Progress` at a time.
2. **Recommended activation order (single-lane serial):** **IBL-A → IBL-B (B1–B6; B7 when Word host available) → IBL-C → IBL-D → IBL-E (only after pending decisions)**.
3. Formal phase remains **None**. Task Master registration (#106–#134) is complete; **Wave IBL-A** remains **In Progress** (A1–A3 **Done**; A4–A6 **Not Started**) — do not invent a P-phase here.
4. Tasks marked **BDD: required** may not start implementation until `behavior-spec-author` publishes a `ready` spec.

**Current wave:** **IBL-A** (`In Progress` — A1–A3 **Done**; A4–A6 **Not Started**; **sole-active cleared**).

```text
Sole-active: (cleared)
Next queue (not activated): #110 IBL-A4
Recommended after #110 Done:
  IBL-A5…A6 (#111–#112) → IBL-B core (#113–#118; B7 gated) → IBL-C → IBL-D → IBL-E (pending decisions)
```

---

## 3. Wave IBL-A — Filling correctness & consumer contract 「填充正确性与消费者契约」

**Wave status:** **In Progress** (A1–A3 **Done**; A4–A6 **Not Started** — do **not** mark Wave A Done until A1–A6 complete)  
**Goal:** Runtime filling is fail-closed and internationally correct; consumers get truthful contracts; retained variables respect PII categories; regenerate replays the original locale (specimen watermark policy unchanged until §Pending).

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| IBL-A1 | backend-engineer | Runtime required/type fail-closed variable enforcement | — | **required** (`ready`) | F1 | **Done** |
| IBL-A2 | backend-engineer | ISO-currency `FORMAT_AMOUNT` (currency code + amount + locale) | — | **required** (`ready`) | F2 | **Done** |
| IBL-A3 | backend-engineer | International amount-in-words framework + `en` locale (extensible beyond CNY) | A2 recommended | **required** (`ready`) | F3 | **Done** |
| IBL-A4 | backend-engineer | `/contract` per-field variable schemas + breaking-change gate + consumer contract tests | — | **required** | F4 | **Not Started** |
| IBL-A5 | backend-engineer | PII-category-driven retention redaction on stored invocation variables | — | **required** | F5; ADR-0057 | **Not Started** |
| IBL-A6 | backend-engineer | Regenerate locale replay fix (`locale` preserved; no silent `null`) | — | **required** | F6 (locale only) | **Not Started** |

### Acceptance criteria (concise)

| ID | Acceptance |
| --- | --- |
| **IBL-A1** | Generate request missing a required variable (or wrong type/enum) → **4xx** with stable code e.g. `VARIABLE_VALIDATION_FAILED` and field list; **no** silent blank in DOCX/PDF; regression tests cover required/type/enum; publish/preview paths aligned or explicitly scoped in BDD. Gates: `mvn -B -ntp -f backend/pom.xml verify`. **BDD `ready`:** [ibl-a1-variable-validation.md](../behavior/ibl-a1-variable-validation.md)（**BDD-IBL-A1-001…008**；preview **aligned**；publish **scoped**；**not** go-live；do **not** flip #3b/#5a）。 |
| **IBL-A2** | `FORMAT_AMOUNT` with ISO currency (e.g. EUR + `en-US`) renders EUR symbol/format — **not** locale-default `$`; BDD + unit tests for multi-currency pairs; OpenAPI/docs note currency argument contract. Gates: backend verify. **BDD `ready`:** [ibl-a2-format-amount-currency.md](../behavior/ibl-a2-format-amount-currency.md)（**BDD-IBL-A2-001…010**；一元 locale-default **兼容**；二元 ISO fail-closed；**not** go-live；do **not** flip #3b/#5a）。 |
| **IBL-A3** | Amount-in-words supports at least **en** + USD (and framework for more); CNY/`zh` path remains correct; default locale behavior documented (no silent wrong language). Gates: backend verify. **BDD `ready`:** [ibl-a3-amount-in-words.md](../behavior/ibl-a3-amount-in-words.md)（**BDD-IBL-A3-001…012**；一元 CNY 中文 **locale-independent 兼容**；二元 ISO + locale language；`(en,USD)`+`(zh,CNY)`；未支持 pair fail-closed；**not** go-live；do **not** flip #3b/#5a）。 |
| **IBL-A4** | `/contract` returns per-field variable schemas (name/type/required/enum/pii as applicable); consumer contract tests fail on breaking placeholder/schema renames; OpenAPI synced. Gates: backend verify. |
| **IBL-A5** | Retained invocation parameters redact/exclude fields per `VariablePiiCategory`; regenerate still works for non-redacted fields per BDD; audit/tests prove PII not stored in clear where category forbids. Gates: backend verify. |
| **IBL-A6** | Regenerate uses the **original invocation locale** (not `null` → engine default); SPECIMEN watermark **unchanged** unless §Pending “true re-issue” is confirmed; regression test for locale-sensitive amount/date. Gates: backend verify. |

**Wave A exit:** A1–A6 Done with green backend verify + BDD ready specs + ledger rows. Does **not** imply go-live.

---

## 4. Wave IBL-B — Rendering fidelity trust 「渲染保真信任」

**Wave status:** **Not Started** (B7 starts **Blocked**)  
**Goal:** Authors’ direct-format controls are honest; PDF conversion capacity is planned and measured; PDF/A is machine-validated; long clauses and seals have governed behavior; reproducibility is documented; Word baseline path exists when host available.

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| IBL-B1 | rendering-engineer | Apply direct-format paragraph spacing/indents (close whitelist↔writer gap) | — | **required** | F9 | **Not Started** |
| IBL-B2 | rendering-engineer + backend-engineer + build-deploy-agent | PDF conversion capacity plan — queue + async offload; close **DEF-LRP-D6-001** | — | **required** (runtime capacity behavior) | F11; DEF-LRP-D6-001; `docs/plan/evidence/lrp-d6-load-smoke/` | **Not Started** |
| IBL-B3 | rendering-engineer + build-deploy-agent | veraPDF (or approved equivalent) PDF/A validation in verify gates | — | not-applicable | F12 | **Not Started** |
| IBL-B4 | rendering-engineer + doc-keeper | Long-clause overflow/truncation/page-break policy + activate golden theme `08` | — | **required** | F13 | **Not Started** |
| IBL-B5 | rendering-engineer | Seal geometry validation (authorized area as real geometry, not boolean-only) | — | **required** | F14 | **Not Started** |
| IBL-B6 | doc-keeper (+ rendering-engineer) | Deterministic legal-reproducibility freeze doc/ADR (LO version + font set + content-hash baselines) | — | not-applicable | F16 | **Not Started** |
| IBL-B7 | rendering-engineer + doc-keeper | Path E Word baseline measurement (promote checklist #3b CONDITIONAL → GO); ADR-0042 already Accepted (Path X) + enforcement landed under PRR-C01 | **Blocked:** licensed MS Word host for Path E | not-applicable (measurement) | F10; checklist #3b GO path | **Blocked** |

### Acceptance criteria (concise)

| ID | Acceptance |
| --- | --- |
| **IBL-B1** | Whitelisted paragraph spacing/indent properties that authors can set are **applied** in DOCX output (POI assertions); or whitelist is narrowed to match writer with fail-closed publish messaging — **no** silent ignore. Gates: backend verify. |
| **IBL-B2** | Documented capacity plan (pool/queue/async offload); sync path no longer exhibits LR-D6-class **8/10** failure under agreed smoke; metrics for queue/reject; DEF-LRP-D6-001 triage closed or superseded with evidence. Gates: backend verify + queued Docker deploy smoke as required by delivery pipeline. |
| **IBL-B3** | Verify (or dedicated CI profile) runs veraPDF (or company-approved PDF/A validator) on PDF/A artifacts — not XMP-only; dependency policy + docs. Gates: backend verify / CI lane green. |
| **IBL-B4** | Written overflow policy (truncate vs paginate vs fail-closed) confirmed in BDD; theme `08-long-clause-limits` activated with LO golden assertions. Gates: backend verify. |
| **IBL-B5** | Seal refs validated against geometric authorized area; out-of-area → fail-closed error; tests with in/out fixtures. Gates: backend verify. |
| **IBL-B6** | ADR or ops freeze doc records LO version, font set, and content-hash baseline procedure; indexed from docs. Gates: docs review (architecture-reviewer as needed). |
| **IBL-B7** | On Word-capable host: Word vs LO page deltas measured; ADR-0042 Accepted or explicit deferral with evidence; budget enforced in gate when Accepted. **Do not** invent Word numbers. Status remains **Blocked** until host exists. |

**Wave B exit:** B1–B6 Done; B7 Done **or** remains Blocked with explicit host dependency recorded. Checklist #3b only changes when evidence warrants (owned by checklist process — not auto-flipped here).

---

## 5. Wave IBL-C — Comparison & regression evidence 「对比与回归证据」

**Wave status:** **Not Started**  
**Goal:** Layout regressions are caught with **non-pixel** PDF metrics; authors can compare two rendered outputs; cross-locale goldens use real LibreOffice where claimed.

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| IBL-C1 | rendering-engineer + backend-engineer | Layout-metric PDF regression (page count / text-position via PDFBox — **no pixel** unless ADR revised) | B3/B4 recommended | not-applicable | F17 | **Not Started** |
| IBL-C2 | frontend-engineer | Side-by-side **rendered** output compare UI | — | **required** (frontend E2E **mandatory**) | F18 | **Not Started** |
| IBL-C3 | rendering-engineer + doc-keeper | Cross-locale golden matrix + SYNTHETIC→LIBREOFFICE upgrades | A2/A3 recommended | not-applicable | F19 | **Not Started** |

### Acceptance criteria (concise)

| ID | Acceptance |
| --- | --- |
| **IBL-C1** | Golden/CI assertions cover page count and key text-position (PDFBox); still **reject** `PIXEL_*` unless §Pending pixel ADR Accepted; baselines checked in or generated under freeze (B6). Gates: backend verify. |
| **IBL-C2** | UI shows two rendered artifacts side-by-side (not only semantic/warning diff); BDD ready; Playwright functional + UIUX on Docker 4173; i18n English-first. Gates: `pnpm -C frontend lint && type-check && test && build` + E2E. |
| **IBL-C3** | Corpus includes en/zh and multi-currency themes; PDF halves marked LIBREOFFICE are produced by LO (not SYNTHETIC) or labels corrected honestly. Gates: backend verify + LO lane when D2 exists. |

---

## 6. Wave IBL-D — Test & integration realism 「测试与集成真实性」

**Wave status:** **Not Started**  
**Goal:** CI cannot go green while skipping real Postgres/Flyway or real LibreOffice conversion; load and chaos evidence exist; legalhold depth matches criticality.

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| IBL-D1 | backend-engineer + build-deploy-agent | Testcontainers PostgreSQL + Flyway lane in verify (or required CI profile) | — | not-applicable | F20 | **Not Started** |
| IBL-D2 | build-deploy-agent + rendering-engineer | CI LibreOffice **mandatory** lane (fail-not-skip when profile selected) | — | not-applicable | F21 | **Not Started** |
| IBL-D3 | build-deploy-agent + doc-keeper | k6 (or approved) load suite + NFR SLO **confirmation path** (values stay pending until user confirms) | B2 recommended | not-applicable | F22; NFR §待确认 | **Not Started** |
| IBL-D4 | rendering-engineer + backend-engineer | LibreOffice pool chaos / failover tests | B2 | not-applicable | F22 | **Not Started** |
| IBL-D5 | backend-engineer | `legalhold` test depth increase (critical paths) | — | not-applicable (tests) / **required** if behavior gaps found | F23 | **Not Started** |

### Acceptance criteria (concise)

| ID | Acceptance |
| --- | --- |
| **IBL-D1** | A verify/CI lane runs against Testcontainers PostgreSQL with Flyway on; migration/SQL defects fail the lane; docs explain H2 vs TC split. Gates: documented `mvn` profile/CI green. |
| **IBL-D2** | LO-dependent tests **fail** (not skip) on the mandatory LO CI lane when `soffice` absent; optional local skip remains documented. Gates: CI evidence. |
| **IBL-D3** | k6 (or company-approved) suite checked in; results feed NFR §待确认 — **no** confirmed SLO invented; user confirmation recorded separately. Gates: scripted run evidence. |
| **IBL-D4** | Chaos/failover tests for LO pool saturation/timeout/reject paths; ties to B2 metrics. Gates: backend verify / IT profile. |
| **IBL-D5** | Meaningful legalhold coverage beyond 2 thin classes (create/enforce/block paths); regressions for critical hold behavior. Gates: backend verify. |

**Note:** Expanding Docker Playwright subset beyond 9/162 is **out of IBL-D5 scope** unless batched later — coordinate with CDP/CE E2E ownership; do not silently re-own CD-E2E.

---

## 7. Wave IBL-E — Multinational content model 「跨国内容模型」

**Wave status:** **Blocked** (entire wave — see §Pending decisions「待确认」)  
**Goal:** After explicit user/PRD/ADR confirmation, support locale variants, jurisdiction-driven composition, multi-stage legal approval, entity brand variants, effective dating / bulk re-pin, clause nesting governance, and RTL exploration.

| ID | Owner | Title | Depends | BDD | Maps | Status |
| --- | --- | --- | --- | --- | --- | --- |
| IBL-E1 | doc-keeper → backend-engineer (+ frontend-engineer) | Locale-variant template/clause model — ADR + implementation | **Pending:** F24 ADR + user | **required** | F24 | **Blocked** |
| IBL-E2 | doc-keeper → backend-engineer | Jurisdiction/product/channel rule engine — ADR + implementation | **Pending:** F25 user/ADR | **required** | F25 | **Blocked** |
| IBL-E3 | doc-keeper → backend-engineer (+ frontend-engineer) | Multi-stage legal→compliance approval matrix + legal-reviewer role | **Pending:** F26 user | **required** | F26 | **Blocked** |
| IBL-E4 | backend-engineer + frontend-engineer | Per-legal-entity document brand variants (beyond UI theming) | **Pending:** brand/PRD | **required** | F27 | **Blocked** |
| IBL-E5 | backend-engineer | `effectiveFrom` publish enforcement + bulk re-pin / mass-migration tooling | E1/E4 may interact | **required** | F27 | **Blocked** |
| IBL-E6 | backend-engineer | Clause nesting governance (module graph + where-used depth) | — | **required** | F28 | **Blocked** |
| IBL-E7 | rendering-engineer + doc-keeper | RTL / bidirectional script exploration spike | **Pending:** market need | not-applicable (spike) | F15 | **Blocked** |

### Acceptance criteria (placeholders — finalize only after pending decisions)

| ID | Acceptance (draft — not confirmed scope) |
| --- | --- |
| **IBL-E1** | Accepted ADR for locale-variant model; data model + API + UI per ADR; BDD ready; no single-body assumption left undocumented. |
| **IBL-E2** | Accepted ADR for composition rules; runtime inclusion deterministic and auditable; BDD ready. |
| **IBL-E3** | Multi-stage approval configured and enforced; role matrix in permission docs; BDD ready. |
| **IBL-E4** | Document brand variants selectable per legal entity (not only REDBC/GREENBC UI chrome). |
| **IBL-E5** | Future `effectiveFrom` blocked or scheduled correctly at publish; bulk re-pin tool with dry-run + audit. |
| **IBL-E6** | Nesting depth governed; where-used reports deep references; cycles fail-closed. |
| **IBL-E7** | Spike report: feasibility, LO/POI gaps, recommend ADR or descoped. |

---

## 8. Pending decisions 「待确认」

**Do not schedule or activate the following as ordinary IBL tasks until explicit user / PRD / ADR confirmation.** These conflict with or expand accepted v1 boundaries or prior ADR stance.

| # | Topic | Why blocked | Related findings / tasks |
| --- | --- | --- | --- |
| **PD-1** | Outbound delivery channels (print / email / ECM / registered mail) | v1 PRD: upstream systems invoke API; delivery orchestration is scope expansion | — (out of program until confirmed) |
| **PD-2** | Pixel / visual PDF regression | Golden-corpus README + ADR stance forbid pixel compare; needs **new ADR** to revise | F17; gates IBL-C1 pixel mode |
| **PD-3** | Word-host measurement for ADR-0042 | Requires machine with **licensed MS Word** — operational dependency outside repo | F10; **IBL-B7** |
| **PD-4** | Locale-variant template model | Significant domain-model / ADR change | F24; **IBL-E1** |
| **PD-5** | Jurisdiction / product / channel rule engine | Significant domain-model / ADR change | F25; **IBL-E2** |
| **PD-6** | True non-specimen re-issue | Legal/compliance decision on watermark policy | F6; beyond **IBL-A6** locale fix |
| **PD-7** | Licensed font embedding (true Calibri etc.) | Font licensing procurement | pairs with LRP font baseline / ADR-0041 |
| **PD-8** | Multi-stage legal approval matrix & forced legal-reviewer | Product/permission boundary | F26; **IBL-E3** |
| **PD-9** | Per-legal-entity document brands (vs UI theming only) | Product boundary | F27; **IBL-E4** |

Until confirmed, Wave **IBL-E** remains **Blocked**, and IBL-A6 must **not** remove SPECIMEN watermarking.

---

## 9. Open questions 「开放问题」(non-blocking residuals)

Kept separate from confirmed findings and from §Pending product-boundary decisions:

| # | Question | Notes |
| --- | --- | --- |
| **Q1** | Schedule **F7** distributed/ coördinated rate-limit (Bucket4j / Redis) as a future IBL-A/D task or leave under ADR-0039 / CE-PRR? | Medium; multi-instance residual |
| **Q2** | Schedule **F8** timezone / as-of date semantics for `FORMAT_DATE` into IBL-A follow-on? | Medium; interacts with A2/A3 locale work |
| **Q3** | Expand Docker Playwright smoke subset (9→N of 162) under IBL or CDP/CE? | Coordinate ownership; avoid double-own |
| **Q4** | Company-approved artifacts for veraPDF, Testcontainers, k6 — intranet availability? | Dependency policy before IBL-B3/D1/D3 |
| **Q5** | When activating IBL-A after PRR, Batch Recommendation `merge`/`solo` bundling for #107–#112? | Delivery-orchestrator stage −1; TM IDs already registered |

---

## 10. Gates & Done definition 「门禁与完成定义」

### 10.1 Gates (per task class)

| Surface | Gate |
| --- | --- |
| Backend / rendering | `mvn -B -ntp -f backend/pom.xml verify` |
| Frontend (when FE touched) | `pnpm -C frontend lint` · `type-check` · `test` · `build` |
| User-facing FE | Playwright functional + UIUX on Docker **4173** (queued deploy) |
| Docs/ADR-only | Link check + architecture-reviewer as needed |
| Deploy evidence | `pwsh ./scripts/docker-deploy-queue.ps1` when acceptance surface requires it |

### 10.2 Done (IBL task)

An IBL task is `Done` only when:

1. BDD ready **or** explicit `not-applicable`.
2. Failing test first (TDD); regression for fixes.
3. Gates green for touched surfaces.
4. Finding row / wave status / ledger evidence updated in the **same** change set.
5. `post-task-doc-sync` → `post-task-commit-review` completed.
6. No promotion of §Pending items into confirmed scope without user/ADR.

### 10.3 Program activation (human gate)

Task Master registration is **complete** (#106–#134; MAIN merge `9fc2bc97`). **Wave IBL-A** remains **In Progress** — **IBL-A1 / #107** → **Done** (merge `f0a2b6fe`; Batch Recommendation **solo** closed); **IBL-A2 / #108** → **Done** (merge `e3000479` / tip `89584242`; Batch Recommendation **solo** closed); **IBL-A3 / #109** → **Done** (merge `779b1979` / tip `f09326ca`; Batch Recommendation **solo** closed). Formal phase remains **None**. Closing A3 does **not** flip checklist **#3b** / **#5a** or claim go-live.

---

## 11. Lower-tier delegation protocol 「低级模型委托协议」

```markdown
### <IBL-TASK-ID> — <title>
- **Owner agent:** backend-engineer | rendering-engineer | frontend-engineer | doc-keeper | build-deploy-agent | …
- **Read first:** this program §wave; evidence paths in §1; related ADR/BDD
- **Do NOT:** activate §Pending items; flip checklist #3b; invent Word baselines; claim go-live; touch Task Master without delivery-orchestrator
- **Steps:** numbered, ≤8
- **Acceptance:** copy from wave table + BDD scenarios
- **Gates:** exact commands
- **Done when:** behavior + gates + doc sync + ledger row
```

**Forbidden:**

- Implementing **BDD: required** before ready spec.
- New dependencies without company-repo verification + ADR when required.
- Pixel compare without PD-2 ADR.
- Removing SPECIMEN watermark without PD-6.
- Activating IBL-E without PD-4/PD-5/PD-8/PD-9 as applicable.
- Changing formal phase status or claiming program Done from a single task.

---

## 12. Traceability 「追溯」

| Source | IBL relationship |
| --- | --- |
| 2026-07-17 four-track (+ template) read-only audit | §1 F1–F28 |
| [launch-readiness-program.md](./launch-readiness-program.md) LR-D6 / DEF-LRP-D6-001 | **IBL-B2** |
| ADR-0042 Accepted (Path X) / checklist #3b CONDITIONAL → Path E for GO | **IBL-B7** (Blocked on Word host) |
| ADR-0039 / Bucket4j in-process | F7 deferred (Q1) |
| ADR-0057 invocation retention | **IBL-A5** |
| Golden corpus anti-pixel stance | **IBL-C1** + PD-2 |
| [non-functional-requirements.md](../requirements/non-functional-requirements.md) §待确认 | **IBL-D3** feeds confirmation only |
| CE-K08 jurisdiction expiry gate | Context for F25 / **IBL-E2** — do not re-own CE rows |

---

## 13. Changelog

| Date | Change |
| --- | --- |
| 2026-07-18 | **IBL-A3 / #109 → Done** — MAIN merge `779b1979` / feature tip `f09326ca`; worktree removed. International `SPELL_AMOUNT` (en/USD + zh/CNY; unary CNY locale-independent; unsupported pair → `VARIABLE_COMPUTE_FAILED`). Gates: `mvn verify` **2038**/0/0/8 GREEN; arch **PASS_WITH_NOTES** Critical=0; Stage 10 **DEPLOY_OK** `healthz` 200. Residuals: MAIN may be ahead of `origin/main` (443). Wave **IBL-A** stays **In Progress** (A1–A3 Done; A4–A6 Not Started). Sole-active cleared. **#106** registry-only **pending**. Next queue (not activated): **#110** IBL-A4. Formal phase **None**. Do **not** flip **#3b/#5a GO**. Do **not** claim go-live. Do **not** mark Wave A / IBL program Done. |
| 2026-07-18 | **Activated IBL-A3 / #109** — Task Master **#109** → **in-progress** (sole-active); Wave **IBL-A** stays **In Progress** (A1+A2 Done; A3 In Progress; A4–A6 Not Started). Slice `ibl-a3-amount-in-words`; ISOLATED `D:/working/DGE-ibl-a3-amount-in-words` · `feat/ibl-a3-amount-in-words`. BDD **ready** [ibl-a3-amount-in-words.md](../behavior/ibl-a3-amount-in-words.md) (**BDD-IBL-A3-001…012**). Batch **solo** (`member_task_ids: ["109"]`). **#107/#108** remain **done**. **#106** stays registry-only **pending**. Formal phase **None**. Do **not** flip **#3b/#5a GO**. Do **not** claim go-live. Do **not** mark Wave A / IBL program Done. |
| 2026-07-18 | **IBL-A2 / #108 → Done** — MAIN merge `e3000479` / feature tip `89584242`; worktree removed. ISO-currency `FORMAT_AMOUNT` (EUR+en-US ≠ `$`; unary locale-default compatible; illegal currency → `VARIABLE_COMPUTE_FAILED`). Gates: `mvn verify` **2025**/0/0/8 GREEN; arch **PASS_WITH_NOTES** Critical=0; Stage 10 **DEPLOY_OK** `healthz` 200. Residuals: ADR-0056 formal Amendment deferred; MAIN may be ahead of `origin/main`. Wave **IBL-A** stays **In Progress** (A1+A2 Done; A3–A6 Not Started). Sole-active cleared. **#106** registry-only **pending**. Next queue (not activated): **#109** IBL-A3. Formal phase **None**. Do **not** flip **#3b/#5a GO**. Do **not** claim go-live. Do **not** mark Wave A / IBL program Done. |
| 2026-07-18 | **Activated IBL-A2 / #108** — Task Master **#108** → **in-progress** (sole-active); Wave **IBL-A** stays **In Progress** (A1 Done; A2 In Progress; A3–A6 Not Started). Slice `ibl-a2-format-amount-currency`; ISOLATED `D:/working/DGE-ibl-a2-format-amount-currency` · `feat/ibl-a2-format-amount-currency`. BDD **ready** [ibl-a2-format-amount-currency.md](../behavior/ibl-a2-format-amount-currency.md) (**BDD-IBL-A2-001…010**). Batch **solo** (`member_task_ids: ["108"]`; veto **different-acceptance-vs-A3**). **#107** remains **done**. **#106** stays registry-only **pending**. Formal phase **None**. Do **not** flip **#3b/#5a GO**. Do **not** claim go-live. Do **not** mark Wave A / IBL program Done. |

| 2026-07-18 | **IBL-A1 / #107 → Done** — MAIN merge `f0a2b6fe` / feature tip `4bda5f2d`; worktree removed; `origin/main` pushed. Runtime/preview fail-closed `VARIABLE_VALIDATION_FAILED` + `fieldErrors`. Gates: `mvn verify` **2014**/0/0/8 GREEN; arch **PASS_WITH_NOTES** Critical=0; Stage 10 **DEPLOY_OK** `2026-07-18T19:35:46+08:00`. Residuals: regenerate unhooked (**R1**); IRC no `fieldErrors` (**R3**). Wave **IBL-A** stays **In Progress** (A1 Done; A2–A6 Not Started). Sole-active cleared. **#106** registry-only **pending**. Next queue (not activated): **#108** IBL-A2. Formal phase **None**. Do **not** flip **#3b/#5a GO**. Do **not** claim go-live. Do **not** mark Wave A / IBL program Done. |
| 2026-07-18 | **Activated Wave IBL-A / IBL-A1** — Task Master **#107** → **in-progress** (sole-active); Wave **IBL-A** → **In Progress** (A1 only). Slice `ibl-a1-variable-validation`; ISOLATED `D:/working/DGE-ibl-a1-variable-validation` · `feat/ibl-a1-variable-validation`. BDD **ready** [ibl-a1-variable-validation.md](../behavior/ibl-a1-variable-validation.md) (**BDD-IBL-A1-001…008**). Batch **solo** (`member_task_ids: ["107"]`; vetoes A2/A3 + umbrella **#106**). **#106** stays registry-only **pending**. Formal phase **None**. Do **not** flip **#3b/#5a GO**. Do **not** claim go-live. |
| 2026-07-18 | **Registered in Task Master** umbrella **#106** + leaves **#107–#134** (**28** tasks). Status → **Registered / Not Started** (waves **not** activated). MAIN merge `9fc2bc97`; worktree removed. PRR truth: **#105 Done**; remaining ahead of IBL **#103 → #104** (pending). Task-count correction **27 → 28**. No checklist **#3b** flip; no go-live; no Wave A `In Progress`. |
| 2026-07-18 | PRR-C01 **#103 Done** (merge `3513ab92`; doc-sync `6408c210`); ADR-0042/0043 Accepted; checklist **#3b → CONDITIONAL** (Path X ≠ GO). Remaining ahead of IBL = **#104** only. F10/B7 residual = Path E Word host. No go-live; no IBL wave activation. |
| 2026-07-18 | PRR **#104** D01A activated sole-active (Wave D **SPLIT** `prod-ops-resilience-pdf-pool`). IBL stays **pending** / not activated. Do **not** flip **#3b GO**. |
| 2026-07-18 | PRR **#104** D01A → **Done** (tip `f1f79d14`; stage 12 doc-sync). Wave D bag OUT unfinished — next queue Wave D residuals + IRC mapper residual. Sole-active cleared. IBL still deferred. Do **not** flip **#3b GO**. |
| 2026-07-18 | PRR **#135** D01B activated sole-active (`prod-ops-security-hardening`; stage 2 plan-orchestrator). IBL stays **pending** / not activated. Do **not** flip **#3b GO**. |
| 2026-07-18 | PRR **#135** D01B → **Done** (merge `6e776232` / tip `1ada6b41`; stage 12 doc-sync). Wave D residual SPLIT closed — actuator/nginx/IRC/ADR-0044; dashboard residual unfinished. Sole-active cleared. IBL still deferred. Do **not** flip **#3b GO**. |
| 2026-07-18 | PRR **#136** D01C activated sole-active (`prod-dashboard-summary-api`; stage 2 plan-orchestrator). IBL stays **pending** / not activated. Do **not** flip **#3b GO**. |
| 2026-07-18 | PRR **#136** D01C → **Done** (merge `a872c15b` / tip `8c52ee67`; stage 12 doc-sync). Wave D residuals **D01A+#135+#136** closed. Sole-active cleared. IBL still **pending** / not activated. Do **not** flip **#3b GO**. |
| 2026-07-18 | PRR **#137** P2 audit hygiene activated sole-active (`prod-audit-p2-hygiene`; stage 2 plan-orchestrator; IBL **vetoed**). IBL stays **pending** / not activated. Do **not** flip **#3b GO**. |
| 2026-07-18 | PRR **#137** P2 audit hygiene → **Done** (merge `baaf16cc` / tip `09cf85ce`; stage 12 doc-sync). Sole-active cleared. IBL still **pending** / not activated. Do **not** flip **#3b GO**. |
| 2026-07-17 | Program document created from 2026-07-17 deep audit. Waves IBL-A…E defined (scheduled task set later counted as **28**). All activatable tasks **Not Started**; B7 + Wave E **Blocked**. Formal phase unchanged (**None**). Task Master then untouched. Proposal awaiting user confirmation to write TM. |

---

**Next action:** Sole-active **cleared**. **Next queue (not activated):** **#110** IBL-A4. Do **not** invent a formal P-phase. Do **not** flip checklist **#3b GO** / **#5a GO**. Do **not** mark Wave A or IBL program Done.
