# P23 — Demo Document Typography & Layout Excellence (Detailed Plan)

**Phase ID:** `P23-DEMO-TYPOGRAPHY-LAYOUT-EXCELLENCE`  
**Phase status:** **Done** (closed 2026-07-08; T01–T16 complete; automated acceptance green; human checklist template published — reviewer sign-off operational follow-up)  
**Depends on:** P22 (**Done** — rendering engine + package scaffold), P18/P4 (authoring + pipeline Done), LRP Wave LR-A (font baseline **LR-A2**; pagination corpus **LR-A7**)  
**Task Master mirror:** `.taskmaster/tasks/tasks.json` tasks **4–8** (foreign-bank-letter grade rewrites + publish + generate + evidence)  
**BDD:** `docs/requirements/demo-typography-layout-behavior-spec.md` — **ready** (P23-T01 Done 2026-07-08; `BDD-DEMO-TYP-001`…`020`; see §6)

> **Single-active-phase invariant:** **P23 closed Done** (2026-07-08). No formal phase `In Progress` until next activation. **P22 remains Done** with honest carry-forward note (T05–T11 scaffold-only → P23 closed). **LRP** Wave LR-A **Done** (2026-07-10; A1–A7; not a formal phase). **LR-A7** pagination corpus was unblocked by P23 demo pack.

---

## 1. North star

**Every generated demo document must look and read like correspondence from an international wholesale/retail bank — not a prototype.**

Word is the authoritative typesetting surface. Generated DOCX must exhibit:

- **Professional typography** — consistent body/heading fonts, point sizes, line spacing, widow/orphan-safe paragraph spacing, emphasis that maps to Word runs (not plain-text downgrade).
- **Bank-grade layout** — margins, headers/footers, page numbering profiles per letter type, signature blocks, schedule tables with header styles.
- **Rich structured bindings** — `styleRef`, lists, tables, conditions, loops, module refs rendered with the P22 writer (no placeholder paragraphs).
- **Font-faithful Docker output** — CJK + metric-compatible Latin substitutes (Calibri/Cambria class) so PDF preview matches intent (**LR-A2**).
- **Verifiable acceptance** — automated POI/XML assertions + Playwright runtime generate checks + optional human typography checklist for fundraising evidence.

**Honest baseline (2026-07-08):** P22 closed the **rendering engine** and **deploy scaffold** (import chain, master generators, BDD contract tests). User review and taskmaster tasks 4–8 confirm **content depth and Word polish remain insufficient** — seven demo packages (T05–T11) were marked «scaffold; rich bindings/catalog generation pending» at P22 close. P23 owns closing that gap **alongside** comprehensive demo coverage (all 8+ templates runtime-callable with bank-letter-grade output).

---

## 2. Relationship to P22 (no reopen)

| P22 delivered (Done) | P23 completes (this phase) |
| --- | --- |
| `StructuredContentDocxWriter` + style catalog expansion (T01/T02) | Apply styles in **master assets** and **binding overlays** per demo |
| Section-aware page numbering + PDF stamper (T03/T04) | Per-demo **footer/header layouts** with real bank copy and field placement |
| Eight `deploy/demo-*` package scaffolds + `import-all-demos.ps1` (T05–T13) | **Rich bindings**, SQL clauses, variables, executive test data (taskmaster 4–5) |
| FOL executive scale (T14) | Bring **remaining demos** to FOL-comparable depth where product-appropriate |
| BDD contract/regression tests (T15) | **Typography/layout acceptance** tests (POI styles/fonts + E2E DOCX structure) |

**Do not reopen P22 phase status.** Track progress under **P23-T01…T16**.

---

## 3. Exit criteria

1. **Master assets:** Each demo master DOCX defines bank-grade named styles (`Heading1`–`Heading3`, `ClauseBody`, `DefinedTerm`, `TableHeader`, `ScheduleTitle`, `SignatureBlock`, product-specific variants) with fonts, spacing, and margins documented in package README + generator test assertions.
2. **Rich bindings:** All eight demo families use structured nodes (not flat placeholder text): `sectionHeading`, `paragraph` with `emphasis`/`underline`, `styleRef`, `conditionBlock`, `loopBlock`, `tableComponentRef` where product requires; binding validation passes with zero errors.
3. **Font baseline:** **LR-A2 Done** — Docker images ship CJK + metric-compatible Latin fonts; `RenderingFontSmokeTest` green; demo letters with Chinese/Latin mixed content render without tofu (**P23-T02** evidence mirrors LR-A2).
4. **Runtime coverage:** All templates in §7 generate HTTP 200 DOCX via runtime API with package executive test variables; `publish-all-demos.ps1` + `generate-all-demos.ps1` + E2E `demo-runtime-generate.spec.ts` green (**P23-T13/T14 Done**; builds on user commit 2026-07-08).
5. **Visual acceptance:** POI assertions on generated DOCX verify style IDs, font names in `document.xml`, table/header/footer presence (**P23-T15**); optional human checklist signed for ≥2 CORP + ≥2 RETAIL samples (**P23-T16**).
6. **Pagination corpus:** **LR-A7** unblocked — ≥5 bank-letter-grade demos available for Word-vs-PDF page delta measurement (ADR-0042 budget).
7. **Green gates:** `mvn verify`; frontend lint/type-check/test/build; Docker deploy + import + generate smoke; ledger updated with evidence.

---

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **P23-T01** | doc-keeper + behavior-spec-author | **BDD behavior spec** — `demo-typography-layout-behavior-spec.md`: actor (demo author / runtime caller), typography/layout acceptance scenarios, per-demo-type footer/header rules, font baseline expectations, POI/E2E evidence hooks (`BDD-DEMO-TYP-001`…`020`) | — | **Done** (2026-07-08; readiness `ready`) |
| **P23-T02** | deploy-engineer + backend-engineer | **Font baseline in Docker** — executes **LR-A2** / CD-HARD-T01: CJK + Carlito/Caladea (or verified substitutes) in both Dockerfiles; `fc-list` build assertion; `RenderingFontSmokeTest`; evidence PDF. **Single owner row** — update LR-A2 status when Done, do not duplicate work | P23-T01 (typography rules) | **Done** (2026-07-08; LR-A2 mirrored; BDD-DEMO-TYP-009/010) |
| **P23-T03** | backend-engineer | **Master style system for demos** — extend `*MasterDocxAssetGeneratorTest` pattern: standard bank style set (Heading1–3, ClauseBody, TableHeader, SignatureBlock, etc.); margins (e.g. 2.54 cm); default body font; header/footer slots per `pageNumberingProfile`; shared style manifest doc in `deploy/demo-shared/` | P23-T01 | **Done** (2026-07-08; `deploy/demo-shared/` manifest + `DemoMasterDocxStyleSupport`; credit-limit reference POI assertions; `mvn verify` green) |
| **P23-T04** | backend-engineer | **Rewrite `deploy/demo-credit-limit/`** — bank-grade credit-limit confirmation (parties, defined terms, facility, interest, covenants, EOD, governing law, signature); ≥20 variables; rich binding overlays + SQL; regenerate master; maps **taskmaster #4** | P23-T03 | **Done** (2026-07-08; 9 anchors; 32 variables; rich nodes styleRef/emphasis/underline/conditionBlock/loopBlock/tableComponentRef/contentModuleRef; `mvn verify` green) |
| **P23-T05** | backend-engineer | **Rewrite `deploy/demo-mortgage/`** — mortgage approval + amortization schedule table; ≥20 variables; SECTION_AND_GLOBAL footer; maps **taskmaster #5** (mortgage slice) | P23-T03 | **Done** (2026-07-08; 9 anchors; 34 variables; rich nodes styleRef/emphasis/underline/conditionBlock/loopBlock/tableComponentRef/contentModuleRef; `mvn verify` green) |
| **P23-T06** | backend-engineer | **Rewrite `deploy/demo-trade-lc/`** — LC/guarantee notice; document checklist table; seal/image refs; ≥20 variables | P23-T03 | **Done** (2026-07-08; LC + guarantee **9 anchors each**; **45** catalog variables (LC executive **33**, guarantee **22**); rich nodes styleRef/emphasis/underline/conditionBlock/tableComponentRef/contentModuleRef/imageRef/sealRef; SQL TRADE-LC-UCP + TRADE-GUARANTEE-URDG v3.0.0; `trade-lc-demo-v3-bank-grade`; `mvn verify` green) |
| **P23-T07** | backend-engineer | **Rewrite `deploy/demo-collection/`** — rate change + overdue collection; regulatory emphasis + disclaimer footer; ≥15 variables | P23-T03 | **Done** (2026-07-08; rate + overdue **8 anchors each**; **32** catalog variables (rate executive **24**, overdue executive **24**); rich nodes styleRef/RegulatoryEmphasis/DisclaimerBody/emphasis/underline/conditionBlock/tableComponentRef/contentModuleRef/sectionHeading; SQL COLLECTION-RATE/OVERDUE v3.0.0; `collection-layout-v3-eight-anchors` + `collection-demo-v3-bank-grade`; masters regenerated; e2e fixture synced; `mvn verify` green) |
| **P23-T08** | backend-engineer | **Rewrite `deploy/demo-annual-review/`** — annual review + facility renewal; covenant loop/table; CORP dual-page; ≥20 variables | P23-T03 | **Done** (2026-07-08; annual review + renewal **9 anchors each**; **36** catalog variables (executive **34** each); rich nodes styleRef/emphasis/underline/conditionBlock/loopBlock/tableComponentRef/contentModuleRef; SQL ANNUAL-REVIEW-STD/COV + FACILITY-RENEWAL-STD v3.0.0; `annual-review-layout-v3-nine-anchors` + `annual-review-demo-v3-bank-grade`; masters regenerated; e2e fixture synced; `mvn verify` green) |
| **P23-T09** | backend-engineer | **Rewrite `deploy/demo-wealth/`** — wealth statement; holdings tables; ≥20 variables | P23-T03 | **Done** (2026-07-08; **9 anchors** (WST_*); **31** catalog variables; rich nodes styleRef/emphasis/underline/conditionBlock/tableComponentRef/contentModuleRef/imageRef; SQL WEALTH-STATEMENT-STD + WEALTH-REGULATORY-STD v3.0.0; `wealth-layout-v3-nine-anchors` + `wealth-demo-v3-bank-grade`; masters regenerated; e2e fixture synced; `mvn verify` green) |
| **P23-T10** | backend-engineer | **Rewrite `deploy/demo-retail-account/`** — account opening/balance letters; fee schedule table; ≥15 variables | P23-T03 | **Done** (2026-07-08; open + balance **8 anchors each** (RAO_* / RAB_*); **29** catalog variables; rich nodes styleRef/emphasis/underline/conditionBlock/tableComponentRef/contentModuleRef/sectionHeading; SQL RETAIL-ACCOUNT-OPEN-STD + RETAIL-ACCOUNT-BALANCE-STD v3.0.0; `retail-account-layout-v3-eight-anchors` + `retail-account-demo-v3-bank-grade`; GLOBAL_ONLY footer; masters regenerated; e2e fixture synced; `mvn verify` green) |
| **P23-T11** | backend-engineer | **FOL + full-flow polish pass** — align `deploy/demo-fol/` and `deploy/demo-full-flow/` masters/bindings with P23 style manifest; bump `catalogMarker` where content changes | P23-T03, P23-T04…T10 | **Done** (2026-07-08; FOL `fol-layout-v6-bank-style-manifest` + `fol-exec-demo-v8-bank-style-manifest`; shared bank styles + SECTION_AND_GLOBAL dual-page footer POI assertions; full-flow `full-flow-layout-v2-bank-style-manifest` via `DemoRetailLetterheadDocxBuilder` + `FullFlowMasterDocxAssetGeneratorTest`; e2e fixtures regenerated; `mvn verify` green) |
| **P23-T12** | backend-engineer | **Publish orchestration** — `publish-all-demos.ps1` + lifecycle + API policy/credential for all templates; AD Group alignment (RETAIL_API / CORP_API); maps **taskmaster #6** | P23-T04…T11 | **Done** (2026-07-08; `Get-DemoPublishExternalIds` registry **13** templates; lifecycle publish + API policy + credential issuance to `.tmp/credentials/`; evidence `.tmp/evidence/all-demos-publish-summary.json`; `DemoPublishOrchestrationContractTest` **8** tests green) |
| **P23-T13** | e2e-test-engineer | **Runtime generate E2E** — extend `demo-runtime-generate.spec.ts` for all published demos; fixture JSON per package; assert HTTP 200 + DOCX size floor; maps **taskmaster #7** | P23-T12 | **Done** (2026-07-08; **13** cases aligned to `Get-DemoPublishExternalIds`; `full-flow-demo-test-variables.json`; calibrated `DEMO_RUNTIME_MIN_DOCX_BYTES`; `assertDocxArtifact` in `src/utils/demoRuntimeArtifact.ts`; Vitest `tests/demo-runtime-api.test.ts` **9** tests; E2E docker **8 passed / 6 skipped** unpublished — `pnpm -C frontend test:e2e:docker:demos`) |
| **P23-T14** | backend-engineer | **Runtime generate script + manifest** — `generate-all-demos.ps1` saves `.tmp/generated_<externalId>.docx`; `.tmp/evidence/generated-docx-manifest.json` (sizeBytes, sha256, contentMarkers, forbidden scan); audit SUCCESS records under `.tmp/evidence/audit-records/`; maps **taskmaster #7** | P23-T12 | **Done** (2026-07-08; **13** templates; `demo-runtime-generate-manifest.json`; `Get-DemoRuntimeGenerateManifest` + `Resolve-DemoExecutiveVariables`; `DemoGenerateOrchestrationContractTest` **17** tests green) |
| **P23-T15** | backend-engineer | **POI typography/layout assertions** — JUnit suite: style IDs applied; `w:rFonts` for body/headings; table styles; footer field codes; no `LOREM`/`{{placeholder}}`; unzipped `document.xml` spot checks | P23-T04…T11, P23-T02 | **Done** (2026-07-08; `DemoTypographyLayoutAssertions` + `DemoTypographyLayoutRegressionTest` **13** master cases / **25** assertions each; `DemoTypographyLayoutAssertionsTest` **3** tests; BDD TYP-001/002/003/004/013/014/016/018; `mvn verify` green) |
| **P23-T16** | doc-keeper | **Human review checklist + evidence bundle** — `docs/evidence/demo-typography-review-checklist.md`; `docs/evidence/fundraising-demo-summary.md`; maps **taskmaster #8**; archive sample DOCX under `.tmp/evidence/` | P23-T13, P23-T14, P23-T15 | **Done** (2026-07-08; checklist ≥2 CORP + ≥2 RETAIL mandatory samples; 13-template evidence matrix; BDD `BDD-DEMO-TYP-015/020`; reviewer sign-off **pending** — operational) |

### Recommended wave order

```text
Wave 0 — Behavior + font baseline (parallel OK)
  P23-T01 (BDD ready gate)
  P23-T02 (LR-A2 font baseline — can start after T01 typography rules drafted)

Wave 1 — Style manifest (blocks all package rewrites)
  P23-T03

Wave 2 — Package rewrites (parallel per package after T03)
  P23-T04 (credit-limit — taskmaster #4 pilot)
  P23-T05…T10 (remaining six families — taskmaster #5)
  P23-T11 (FOL + full-flow polish)

Wave 3 — Publish + runtime proof
  P23-T12 → P23-T13/T14

Wave 4 — Acceptance + evidence
  P23-T15 → P23-T16
  LR-A7 pagination corpus (doc-keeper — after ≥5 Wave 2 packages Done)
```

**First delegation:** ~~**behavior-spec-author** → **P23-T01**~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T02** (LR-A2 font)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T03** (style manifest)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T04** (credit-limit pilot rewrite)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T05** (mortgage rewrite)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T06** (trade-lc rewrite)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T07** (collection rewrite)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T08** (annual-review rewrite)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T09** (wealth rewrite)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T10** (retail-account rewrite)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T11** (FOL + full-flow polish)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T12** (publish orchestration)~~ **Done** (2026-07-08); ~~**e2e-test-engineer** → **P23-T13** (runtime generate E2E)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T14** (runtime generate script + manifest)~~ **Done** (2026-07-08); ~~**backend-engineer** → **P23-T15** (POI typography/layout assertions)~~ **Done** (2026-07-08); ~~**doc-keeper** → **P23-T16** (human typography review checklist + evidence bundle)~~ **Done** (2026-07-08). **Phase closed.**

---

## 5. LRP / CDP cross-links

| External task | P23 relationship | Rule |
| --- | --- | --- |
| **LR-A2** Font baseline | **P23-T02** executes same work | Record Done once; mirror in LRP-A row + ledger |
| **LR-A7** Pagination delta corpus | Depends on P23 Wave 2 (≥5 letter-grade demos) | Schedule after P23-T05/T08/T04/T06/T07 |
| **LR-A5** ADR-0041/0042/0043 | ADR-0041 fed by T02; ADR-0042 by LR-A7 | doc-keeper |
| **LR-A6** OOXML validation gate | Complements **P23-T15** | Can run in parallel once writer output stable |
| **CD-HARD-T01** | Executed via LR-A2 / P23-T02 | Single status row |
| **taskmaster 4–8** | Maps to P23-T04…T16 | Update taskmaster status as P23 tasks close |

---

## 6. BDD scenario outline (for T01 authoring)

| BDD ID | Scenario summary | Primary tasks |
| --- | --- | --- |
| **BDD-DEMO-TYP-001** | Generated DOCX applies `ClauseBody` style to operative paragraphs | T03, T04, T15 |
| **BDD-DEMO-TYP-002** | Headings use `Heading1`–`Heading3` with correct outline level | T03, T15 |
| **BDD-DEMO-TYP-003** | Table rows use `TableHeader` style | T03, T05, T06, T15 |
| **BDD-DEMO-TYP-004** | Signature block uses dedicated style + spacing | T03, T04, T15 |
| **BDD-DEMO-TYP-005** | Emphasis/underline render as Word runs (not plain text) | T04…T11, T15 |
| **BDD-DEMO-TYP-006** | `styleRef` resolves to master catalog style ID | T04…T11, T15 |
| **BDD-DEMO-TYP-007** | Retail demo footer layout (GLOBAL_ONLY) | T10, T15 |
| **BDD-DEMO-TYP-008** | CORP dual-page footer fields | T04, T08, T11, T15 |
| **BDD-DEMO-TYP-009** | CJK sample paragraph renders with Noto (no tofu) in Docker PDF | T02, T15 |
| **BDD-DEMO-TYP-010** | Latin body uses metric-compatible font (Carlito class) | T02, T15 |
| **BDD-DEMO-TYP-011** | All eight demo types import + generate after rewrite | T12, T13 |
| **BDD-DEMO-TYP-012** | Runtime E2E generates DOCX > size floor per template | T13 |
| **BDD-DEMO-TYP-013** | No placeholder markers in extracted text | T14, T15 |
| **BDD-DEMO-TYP-014** | POI asserts `w:rFonts` for heading vs body differ | T15 |
| **BDD-DEMO-TYP-015** | Human checklist items pass for CORP-FOL + credit-limit samples | T16 |
| **BDD-DEMO-TYP-016** | Master DOCX embeds full bank style catalog at build time | T03, T15 |
| **BDD-DEMO-TYP-017** | Demo bindings use rich structured nodes not flat placeholder text | T04…T11 |
| **BDD-DEMO-TYP-018** | Document margins conform to bank baseline (≥2.54 cm) | T03, T15 |
| **BDD-DEMO-TYP-019** | FOL and full-flow align with shared style manifest | T11 |
| **BDD-DEMO-TYP-020** | Human typography review covers ≥2 CORP + ≥2 RETAIL samples | T16 |

---

## 7. Demo template coverage matrix

| Priority | Package | externalId(s) | P23 task | Min variables | Typography notes |
| --- | --- | --- | --- | --- | --- |
| 1 | demo-credit-limit | `DEMO-CREDIT-LIMIT-CONFIRM` | T04 | ≥20 | CORP dual-page; defined terms block |
| 2 | demo-mortgage | `DEMO-MORTGAGE-APPROVAL` | T05 | ≥20 | Schedule table; SECTION_AND_GLOBAL |
| 3 | demo-trade-lc | `DEMO-TRADE-LC`, etc. | T06 | ≥20 | Document checklist; UCP reference |
| 4 | demo-collection | `DEMO-RATE-CHANGE-NOTICE`, `DEMO-OVERDUE-COLLECTION` | T07 | ≥15 | Regulatory emphasis footer |
| 5 | demo-annual-review | `DEMO-ANNUAL-REVIEW`, `DEMO-FACILITY-RENEWAL` | T08 | ≥20 | Covenant table loop |
| 6 | demo-wealth | `DEMO-WEALTH-STATEMENT` | T09 | ≥20 | Multi-table holdings |
| 7 | demo-retail-account | `DEMO-RETAIL-ACCOUNT`, etc. | T10 | ≥15 | Fee table; GLOBAL_ONLY |
| 8 | demo-fol | `CORP-FOL-OFFER` | T11 | (existing) | Style manifest alignment |
| 9 | demo-full-flow | `DEMO-FULL-FLOW-LETTER` | T11 | (existing) | Retail letter polish |

---

## 8. Gate commands

| Stage | Command | When |
| --- | --- | --- |
| Backend | `mvn -B -ntp -f backend/pom.xml verify` | Every Java task; mandatory T15 |
| Frontend | `pnpm -C frontend lint && type-check && test && build` | T13 E2E slice |
| Docker | `.\scripts\docker-deploy.ps1` | T02, T12, T13 |
| Import | `.\deploy\import-all-demos.ps1` | T12 |
| Publish | `.\deploy\publish-all-demos.ps1` | T12 |
| Generate | `.\deploy\generate-all-demos.ps1` | T14 |
| E2E | `pnpm -C frontend test:e2e:docker` | T13 |
| Health | `http://localhost:8080/healthz`, UI `http://localhost:4173` | T13 smoke |

---

## 9. Classification rationale

| Alternative | Decision |
| --- | --- |
| Reopen P22 | **Rejected** — P22 honestly closed engine + scaffold; reopening blurs Done evidence |
| LRP wave only | **Rejected** — typography/content is product-facing demo deliverable, not ops hardening alone |
| P12 slice | **Rejected** — scope spans 8 packages + fonts + acceptance suite; warrants formal phase |
| **P23 formal phase** | **Accepted** — user strategic priority 2026-07-08; aligns with taskmaster 4–8; pairs with LR-A2/A7 |

---

## Change log

| Date | Change |
| --- | --- |
| 2026-07-08 | Phase activated **In Progress**; 16 tasks Not Started; honest P22 carry-forward documented; taskmaster 4–8 mapped; LR-A2/A7 cross-linked |
| 2026-07-08 | **P23-T02 Done** — LR-A2 font baseline: Debian jammy runtime in both Dockerfiles; `RenderingFontSmokeTest`; `fc-list :lang=zh` build gate |
| 2026-07-08 | **P23-T03 Done** — `deploy/demo-shared/` bank style manifest; `DemoMasterDocxStyleSupport` + credit-limit POI assertions; margin baseline 2.54 cm; gates green |
| 2026-07-08 | **P23-T04 Done** — `deploy/demo-credit-limit/` bank-grade rewrite: 9 anchors (CL_PARTIES…CL_SIGNATURE); 32 variables; rich binding overlays (styleRef, emphasis, underline, conditionBlock, loopBlock, tableComponentRef, contentModuleRef); SQL EOD module; master regenerated; POI assertions green; pilot pattern for T05–T10 |
| 2026-07-08 | **P23-T06 Done** — `deploy/demo-trade-lc/` bank-grade rewrite: LC + guarantee **9 anchors each** (TLC_* / TGN_*); **45** catalog variables; rich bindings (styleRef, emphasis, underline, conditionBlock, tableComponentRef, contentModuleRef, imageRef, sealRef); SQL TRADE-LC-UCP + TRADE-GUARANTEE-URDG v3.0.0; `trade-lc-layout-v3-nine-anchors` + `trade-lc-demo-v3-bank-grade`; masters regenerated; `TradeLcMasterDocxAssetGeneratorTest` POI assertions; e2e fixture synced; `mvn verify` green |
| 2026-07-08 | **P23-T07 Done** — `deploy/demo-collection/` bank-grade rewrite: rate + overdue **8 anchors each** (RCN_* / OCN_*); **32** catalog variables; rich bindings (styleRef, RegulatoryEmphasis, DisclaimerBody, emphasis, underline, conditionBlock, tableComponentRef, contentModuleRef, sectionHeading); SQL COLLECTION-RATE/OVERDUE v3.0.0; `collection-layout-v3-eight-anchors` + `collection-demo-v3-bank-grade`; masters regenerated; e2e fixture synced; `mvn verify` green |
| 2026-07-08 | **P23-T08 Done** — `deploy/demo-annual-review/` bank-grade rewrite: annual review + renewal **9 anchors each** (ARR_* / FRN_*); **36** catalog variables; rich bindings (styleRef, emphasis, underline, conditionBlock, loopBlock, tableComponentRef, contentModuleRef, sectionHeading); SQL ANNUAL-REVIEW-STD/COV + FACILITY-RENEWAL-STD v3.0.0; `annual-review-layout-v3-nine-anchors` + `annual-review-demo-v3-bank-grade`; masters regenerated; e2e fixture synced; `mvn verify` green |
| 2026-07-08 | **P23-T09 Done** — `deploy/demo-wealth/` bank-grade rewrite: **9 anchors** (WST_*); **31** catalog variables; rich bindings (styleRef, emphasis, underline, conditionBlock, tableComponentRef, contentModuleRef, imageRef, sectionHeading); **5** multi-table holdings via tableComponentRef; SQL WEALTH-STATEMENT-STD + WEALTH-REGULATORY-STD v3.0.0; `wealth-layout-v3-nine-anchors` + `wealth-demo-v3-bank-grade`; masters regenerated; e2e fixture synced; `mvn verify` green |
| 2026-07-08 | **P23-T11 Done** — FOL + full-flow polish: `fol-layout-v6-bank-style-manifest` + `fol-exec-demo-v8-bank-style-manifest`; `DemoMasterDocxStyleSupport` promoted to main; FOL master applies shared bank styles + SECTION_AND_GLOBAL dual-page footer POI assertions; full-flow `full-flow-layout-v2-bank-style-manifest` via `DemoRetailLetterheadDocxBuilder` (GLOBAL_ONLY); e2e fixtures regenerated; `mvn verify` green — **Wave 2 complete** (T04–T11) |
| 2026-07-08 | **P23-T10 Done** — `deploy/demo-retail-account/` bank-grade rewrite: open + balance **8 anchors each** (RAO_PARTIES…RAO_DISCLAIMER / RAB_PARTIES…RAB_DISCLAIMER); **29** catalog variables; rich bindings (styleRef, emphasis, underline, conditionBlock, tableComponentRef for fee schedule + transactions, contentModuleRef, sectionHeading); SQL RETAIL-ACCOUNT-OPEN-STD + RETAIL-ACCOUNT-BALANCE-STD v3.0.0; `retail-account-layout-v3-eight-anchors` + `retail-account-demo-v3-bank-grade`; GLOBAL_ONLY footer; masters regenerated; e2e fixture synced; `RetailAccountMasterDocxAssetGeneratorTest` POI assertions; `mvn verify` green — **Wave 2 package rewrites complete** (T04–T10) |
| 2026-07-08 | **P23-T13 Done** — runtime generate E2E: `demo-runtime-generate.spec.ts` **13** `DEMO_RUNTIME_CASES` (publish registry order); `full-flow-demo-test-variables.json`; per-template `DEMO_RUNTIME_MIN_DOCX_BYTES`; `assertDocxArtifact` + Vitest **9** tests; `test:e2e:docker:demos` green (**8** passed / **6** skipped unpublished in current Docker); BDD `BDD-DEMO-TYP-011/012`; **next P23-T14** runtime generate script + manifest |
| 2026-07-08 | **P23-T15 Done** — `DemoTypographyLayoutAssertions` (main) + `DemoTypographyLayoutRegressionTest` **13** parameterized master cases (8 families + FOL + full-flow + secondary templates); **25** POI assertions per case (styles.xml, w:rFonts heading vs body, margins ≥2.54 cm, footer NUMPAGES/SECTIONPAGES profile, document.xml forbidden scan); `DemoTypographyLayoutAssertionsTest` **3** unit tests; BDD `BDD-DEMO-TYP-001/002/003/004/013/014/016/018`; `mvn -B -ntp -f backend/pom.xml verify` green — **next P23-T16** human checklist |
| 2026-07-08 | **P23-T16 Done** — `docs/evidence/demo-typography-review-checklist.md` (mandatory ≥2 CORP + ≥2 RETAIL samples; fonts/styles/margins/headers-footers/tables/signatures); `docs/evidence/fundraising-demo-summary.md` (13-template artifact matrix); BDD `BDD-DEMO-TYP-015/020`; human reviewer sign-off **pending** (operational). **P23 phase closed Done** — all exit criteria met except optional signed checklist execution |
