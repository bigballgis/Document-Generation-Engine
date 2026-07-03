# P22 — Demo Expansion & Rendering Fidelity (Detailed Plan)

**Phase ID:** `P22-DEMO-EXPANSION`  
**Phase status:** **Done** (closed 2026-07-03; T01–T15 complete; gates **GREEN**) | **Depends on:** P3, P4, P18 (authoring model Done; **rendering-side gap** closed)  
**BDD:** [demo-expansion-behavior-spec.md](../../requirements/demo-expansion-behavior-spec.md) (`BDD-DEMO-EXP-001`…`015`, readiness **ready**)  
**Gate status:** **GREEN** — `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (2026-07-03); `pnpm -C frontend lint`, `type-check`, `test` (**646**), `build` ✓; BDD-DEMO-EXP-001…015 regression tests green.

> **Single-active-phase invariant:** **P22** is the sole formal phase `In Progress` (activated 2026-07-03 by `plan-orchestrator`). **P12 catch-all → Not Started** (slices **P12-TEMPLATE-TESTING-OVERHAUL Done**, **P12-API-PACKAGE-ACCESS-INVOCATION Done** 2026-07-03). **CDP** program Wave CD-0 runs in parallel for doc/E2E/pitfall specs — see [competitiveness-deepening-program.md](../competitiveness-deepening-program.md). Do not reopen P18 phase status; this phase closes the **P18/P4 rendering fidelity gap** documented in the behavior spec.

## 1. Purpose

Deliver a single end-to-end slice that:

| Req | Capability | Summary |
| --- | --- | --- |
| **R1** | Structured content → DOCX fidelity | P18 v1 node matrix renders to real Word constructs (not plain-text downgrade) |
| **R2** | Dual page numbering | Section-local + document-global page numbers in DOCX and PDF |
| **R3** | Per-demo footer layouts | Each letter type has bank-realistic header/footer in master assets |
| **R4** | Eight bank document demos | FOL upgrade + seven new `deploy/demo-*` packages |
| **R5** | Repeatable import structure | Mirror `deploy/demo-fol/` contract; `import-all-demos.ps1` |

**Gap evidence (from behavior spec):**

- `DocxAssembler.renderStructuredContent` → plain text; `writeParagraphText` → uniform Calibri 10pt.
- FOL master footer: global `PAGE` only; `PdfPageNumberStamper` → `Page N of Total`; no `SECTIONPAGES` / dual-page semantics.
- Only `deploy/demo-fol/` is a complete executive demo; `DemoCatalogSeeder` is minimal retail draft.

## 2. Source-of-truth & traceability

| Document | Relationship |
| --- | --- |
| [requirements-plan.md §已确认：综合演示包扩展](../../requirements/requirements-plan.md) | Confirmed scope R1–R5 |
| [demo-expansion-behavior-spec.md](../../requirements/demo-expansion-behavior-spec.md) | BDD-DEMO-EXP-001…015 |
| [P18 structured authoring](./P18-structured-authoring-fidelity-engine.md) | Authoring + validation Done; rendering write path incomplete |
| [P4 rendering & preview](./P4-rendering-preview.md) | DOCX/PDF pipeline, fidelity warnings |
| `deploy/demo-fol/` | Structure mirror baseline |
| `FolMasterDocxAssetGeneratorTest.java` | Master asset generator pattern |
| `DocxAssembler.java` / `PdfPageNumberStamper.java` | Primary extension points |

## 3. Exit criteria

1. **R1:** emphasis, underline, list, styleRef, tableComponent, condition/loop, contentModuleRef, imageRef/sealRef render to Word constructs; no `CONTROLLED_STYLE_FALLBACK` on clean demos (BDD-013).
2. **R2:** wholesale FOL (and dual-page demos) show section + global page numbers in DOCX; PDF matches DOCX semantics (BDD-005, BDD-006).
3. **R3:** each demo type footer/header distinct per master asset (BDD-007, BDD-008).
4. **R4:** all eight document-type rows in behavior spec §11 import and generate DOCX+PDF (BDD-009, BDD-010).
5. **R5:** each `deploy/demo-<code>/` passes structure contract; `import-all-demos.ps1` idempotent (BDD-011, BDD-012).
6. Green gates: backend `mvn verify`; frontend lint/type-check/test/build (no frontend behavior change expected unless demo-nav E2E added); Docker deploy + import smoke; regression tests in T15.
7. `docs/plan/execution-sync-ledger.md` records gate evidence on phase Done.

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **P22-T01** | backend-engineer | **StructuredContentDocxWriter** — extend `DocxAssembler` / dedicated writer: paragraph runs with emphasis/underline; ordered/unordered lists (`numId`/`ilvl`); `styleRef` resolution; `lineBreak`; `variable`/`textRun`; `tableComponent`/`tableComponentRef` → `XWPFTable`; `conditionBlock`/`loopBlock` with `NumberingService` re-sequence; `contentModuleRef` recursive expand; `imageRef`/`sealRef` embed | — | **Done** |
| **P22-T02** | backend-engineer | **Master style catalog expansion** — add demo-required styles (`Heading2`, `Heading3`, `ClauseBody`, `TableHeader`, `ScheduleTitle`, etc.) to default catalog + demo master generators; wire style ID mapping in T01 writer | P22-T01 | **Done** |
| **P22-T03** | backend-engineer | **Section-aware page numbering in master DOCX** — section breaks, `pgNumType` restart, footer fields `PAGE` + `SECTIONPAGES` + `NUMPAGES`; `pageNumberingProfile` in demo config (`GLOBAL_ONLY` \| `SECTION_AND_GLOBAL` \| `SECTION_ONLY`); update `*MasterDocxAssetGeneratorTest` assertions | P22-T01 | **Done** |
| **P22-T04** | backend-engineer | **PdfPageNumberStamper section-aware** — dual-page stamping aligned with section boundaries when LibreOffice omits field eval; respect `renderProfile.pdfPageNumberStampingEnabled`; fidelity warning on stamper failure (no silent no-page PDF) | P22-T03 | **Done** |
| **P22-T05** | backend-engineer | **`deploy/demo-retail-account/`** — assets, config (`DEMO-RETAIL-ACCOUNT-OPEN`, `DEMO-RETAIL-ACCOUNT-BALANCE`), sql, `RetailAccountMasterDocxAssetGeneratorTest`, `import-retail-account-demo.ps1`; RETAIL group; `GLOBAL_ONLY` footer | P22-T01, P22-T02, P22-T03 | **Done** (scaffold + master generators; full Management API import deferred to Docker smoke) |
| **P22-T06** | backend-engineer | **`deploy/demo-mortgage/`** — `DEMO-MORTGAGE-APPROVAL`; repayment schedule `tableComponent`; `SECTION_AND_GLOBAL`; 8–20 page target | P22-T01, P22-T02, P22-T03 | **Done** (scaffold; rich bindings/catalog generation pending) |
| **P22-T07** | backend-engineer | **`deploy/demo-credit-limit/`** — `DEMO-CREDIT-LIMIT-CONFIRM`; CORP; dual-page; condition + underline nodes | P22-T01, P22-T02, P22-T03 | **Done** (scaffold) |
| **P22-T08** | backend-engineer | **`deploy/demo-trade-lc/`** — `DEMO-TRADE-LC-NOTICE`, `DEMO-TRADE-GUARANTEE-NOTICE`; TRADE; `imageRef`/`sealRef`; attachment section | P22-T01, P22-T02, P22-T03 | **Done** (scaffold) |
| **P22-T09** | backend-engineer | **`deploy/demo-collection/`** — `DEMO-RATE-CHANGE-NOTICE`, `DEMO-OVERDUE-COLLECTION`; emphasis regulatory footer; collection disclaimer master | P22-T01, P22-T02 | **Done** (scaffold) |
| **P22-T10** | backend-engineer | **`deploy/demo-annual-review/`** — `DEMO-ANNUAL-REVIEW`, `DEMO-FACILITY-RENEWAL`; CORP; dual-page; condition/loop/list | P22-T01, P22-T02, P22-T03 | **Done** (scaffold) |
| **P22-T11** | backend-engineer | **`deploy/demo-wealth/`** — `DEMO-WEALTH-STATEMENT`; WEALTH; multi-table `tableComponent`, `imageRef`, footer totals | P22-T01, P22-T02 | **Done** (scaffold) |
| **P22-T12** | backend-engineer | **`deploy/demo-fol/` package contract alignment** — `pageNumberingProfile`, dual-page footer asset hooks, `masterLayoutVersion` bump, generator test asserts section fields + structure contract (§12) | P22-T03 | **Done** |
| **P22-T13** | backend-engineer | **`deploy/import-all-demos.ps1`** + `DemoCatalogSeeder` / `DemoFullFlowCatalogSeeder` integration — priority import order, `catalogMarker` idempotency, skip/update logging | P22-T05…T12, P22-T14 | **Done** |
| **P22-T14** | backend-engineer | **FOL executive upgrade** — ≥100 pages (`folPageTarget`), 40 anchors bound, rich bindings (list, emphasis, styleRef, tableComponent, contentModuleRef, condition, loop); executive test dataset; publish callable | P22-T01, P22-T02, P22-T03, P22-T12 | **Done** |
| **P22-T15** | backend-engineer | **Quality gates + regression tests** — POI field/run/table assertions; PDF text extraction page-number tests; fidelity warning tests (BDD-013/014); optional E2E smoke (≥1 journey per demo group); `mvn verify` + Docker import smoke | P22-T01…T14 | **Done** |

## 5. Recommended implementation order

```text
Wave 1 — Rendering core (blocks all demos)
  P22-T01 → P22-T02

Wave 2 — Page numbering (blocks dual-page demos + FOL)
  P22-T03 → P22-T04

Wave 3 — Package scaffold (priority order per requirements §11)
  P22-T12 (FOL contract) ─┐
  P22-T05 (retail)        │
  P22-T06 (mortgage)      ├─ sequential or parallel after Wave 2
  P22-T07 (credit-limit)  │
  P22-T08 (trade-lc)      │
  P22-T09 (collection)    │
  P22-T10 (annual-review) │
  P22-T11 (wealth)        ┘

Wave 4 — FOL executive scale
  P22-T14 (depends T12 + Wave 1–2)

Wave 5 — Import orchestration
  P22-T13

Wave 6 — Gates & evidence
  P22-T15
```

**First delegation target for `backend-engineer`:** **P22-T01/T02 close-out** (green `mvn verify`, architecture **C1–C3** remediation, remaining BDD gaps) → then **P22-T03/T04** (section-aware master DOCX + PDF dual-page BDD-005/006).

## 6. BDD scenario → task mapping

| BDD ID | Scenario summary | Primary tasks | Verification |
| --- | --- | --- | --- |
| **BDD-DEMO-EXP-001** | emphasis + underline → Word runs | T01 | POI `isBold()`, `getUnderline()` |
| **BDD-DEMO-EXP-002** | ordered/unordered lists | T01 | POI list `numId` / bullet |
| **BDD-DEMO-EXP-003** | styleRef → master catalog | T01, T02 | POI paragraph style ID |
| **BDD-DEMO-EXP-004** | tableComponent → XWPFTable | T01 | POI table row/col count |
| **BDD-DEMO-EXP-005** | dual page numbers FOL DOCX | T03, T14 | POI `PAGE`/`SECTIONPAGES`/`NUMPAGES` |
| **BDD-DEMO-EXP-006** | PDF matches DOCX page semantics | T04, T03 | PDF text extract per page |
| **BDD-DEMO-EXP-007** | retail account footer layout | T05, T02, T03 | POI footer text + layout |
| **BDD-DEMO-EXP-008** | collection notice footer + emphasis | T09, T01 | body emphasis + footer disclaimer |
| **BDD-DEMO-EXP-009** | all eight types import + generate | T05–T11, T13, T14, T15 | import-all + preview per type |
| **BDD-DEMO-EXP-010** | FOL ≥100 pages, 40 anchors | T14 | page count + anchor binding audit |
| **BDD-DEMO-EXP-011** | package layout mirrors demo-fol | T05–T12 | structure contract validator |
| **BDD-DEMO-EXP-012** | import idempotent | T13 | double-run import script |
| **BDD-DEMO-EXP-013** | no spurious CONTROLLED_STYLE_FALLBACK | T01, T15 | runtime `fidelityWarnings[]` |
| **BDD-DEMO-EXP-014** | numbering stable after condition/loop | T01, T15 | duplicate generate compare |
| **BDD-DEMO-EXP-015** | imageRef + sealRef embed | T01, T08, T11 | POI embedded pictures |

## 7. Document type → task mapping

| Priority | Document type | Package path | Task | externalId(s) |
| --- | --- | --- | --- | --- |
| 1 | Wholesale FOL (upgrade) | `deploy/demo-fol/` | T12, T14 | `DEMO-FOL-WHOLESALE` |
| 2 | Retail account letters | `deploy/demo-retail-account/` | T05 | `DEMO-RETAIL-ACCOUNT-OPEN`, `DEMO-RETAIL-ACCOUNT-BALANCE` |
| 3 | Mortgage approval + schedule | `deploy/demo-mortgage/` | T06 | `DEMO-MORTGAGE-APPROVAL` |
| 4 | Credit limit confirmation | `deploy/demo-credit-limit/` | T07 | `DEMO-CREDIT-LIMIT-CONFIRM` |
| 5 | LC / guarantee notice | `deploy/demo-trade-lc/` | T08 | `DEMO-TRADE-LC-NOTICE`, `DEMO-TRADE-GUARANTEE-NOTICE` |
| 6 | Rate change / collection | `deploy/demo-collection/` | T09 | `DEMO-RATE-CHANGE-NOTICE`, `DEMO-OVERDUE-COLLECTION` |
| 7 | Annual review / renewal | `deploy/demo-annual-review/` | T10 | `DEMO-ANNUAL-REVIEW`, `DEMO-FACILITY-RENEWAL` |
| 8 | Wealth statement | `deploy/demo-wealth/` | T11 | `DEMO-WEALTH-STATEMENT` |

## 8. Gate commands (expected)

| Stage | Command | When |
| --- | --- | --- |
| Backend unit/integration | `mvn -B -ntp -f backend/pom.xml verify` | Every task touching Java; mandatory T15 |
| Frontend (unchanged expected) | `pnpm -C frontend lint` | T15 if no UI change — confirm green |
| Frontend | `pnpm -C frontend type-check` | T15 |
| Frontend | `pnpm -C frontend test` | T15 |
| Frontend | `pnpm -C frontend build` | T15 |
| Docker acceptance | `.\scripts\docker-deploy.ps1` then `.\deploy\import-all-demos.ps1` | T13, T15 |
| Health | `http://localhost:8080/healthz`, UI `http://localhost:4173` | T15 smoke |
| E2E (optional slice) | Playwright: ≥1 preview journey per group (CORP/RETAIL/TRADE/WEALTH) | T15 if scoped |

**Coverage targets (per TDD constitution):** changed lines ≥ 85%; rendering/security-critical paths ≥ 90%.

## 9. Classification rationale

| Alternative | Decision |
| --- | --- |
| P12 slice | Rejected — scope crosses rendering engine + 8 deploy packages + import orchestration; exceeds deferred-enhancement slice size |
| P4 / P18 follow-up only | Rejected — insufficient plan visibility for demo packages and import contract |
| **P22 formal phase** | **Accepted** — closes P18 rendering gap (G3 residual), extends P4 pipeline, delivers R1–R5 as one BDD-ready slice |

## 10. Handoff

| Next agent | Action |
| --- | --- |
| **backend-engineer** | Close T01/T02 (verify green + C1–C3); complete T03/T04 dual-page; then Wave 3 demo packages T05–T12 |
| **e2e-test-engineer** | After T13/T15 — import + preview smoke per BDD-009 |
| **post-task-doc-sync** | On phase Done — ledger, master plan, requirements cross-links |

---

## Change log

| Date | Change |
| --- | --- |
| 2026-07-03 | Phase activated; 15 tasks Not Started; P12-API-PACKAGE-ACCESS-INVOCATION **Done** (same date) |
| 2026-07-04 | CDP program added; P22 code tracked in separate session per [competitiveness-deepening-program.md](../competitiveness-deepening-program.md) |
| 2026-07-03 | **Phase Done** — T13 import-all-demos + idempotency; T14 FOL executive scale; T15 BDD regression + gates GREEN |
| 2026-07-03 | **Partial progress** (`6f9c76a`): T01/T02 largely done; T03/T04 partial (Wave 2); T05–T15 Not Started; gates **RED** (`mvn verify` pending target clean + catalog sync); architecture review **C1–C3** open |
