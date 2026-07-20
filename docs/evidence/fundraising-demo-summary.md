# Fundraising Demo — Evidence Summary

**Document status:** `ready`  
**Version:** 1.1.0  
**Authored:** 2026-07-08  
**Phase:** P23-DEMO-TYPOGRAPHY-LAYOUT-EXCELLENCE (**P23-T16**); taskmaster **#8** (**Done**)  
**BDD (historical):** `BDD-DEMO-TYP-011`…`013`, `BDD-DEMO-TYP-015`, `BDD-DEMO-TYP-020`  
**Post-remediation refresh:** Wave A **Done** — TM **#141** / [bank-letter-demo-refresh.md](../behavior/bank-letter-demo-refresh.md) (`BDD-DEMO-REFRESH-001`…`014`); MAIN merge `aa88170f` / feature `5ae9575a`; plan [detail](../plan/detail/bank-letter-demo-refresh.md); evidence [plan/evidence/bank-letter-demo-refresh/](../plan/evidence/bank-letter-demo-refresh/README.md) (**13/13**). Wave B expand (**#142**) remains **pending** (not activated).

---

## Purpose

Human-readable index mapping each published demo template to its **evidence artifacts**: deploy package, generate script output, E2E coverage, POI/JUnit tests, and manifest paths. Use with [demo-typography-review-checklist.md](./demo-typography-review-checklist.md) for fundraising sign-off.

**Coverage unchanged:** still the **13** runtime `externalId`s (eight packages + `DEMO-FULL-FLOW-LETTER`). Wave A uplifts **content quality** of those same templates; it does **not** add new letter families.

**Prerequisite commands** (host compile; Docker runtime — prefer queued deploy):

```powershell
.\scripts\docker-deploy-queue.ps1
# Optional ops-safe cleanup (see deploy/demo-shared/README.md) — not a DB DROP
.\deploy\demo-fol\cleanup-fol-test-data-sets.ps1 -WhatIf
.\deploy\import-all-demos.ps1 -BackendUrl http://localhost:8080
.\deploy\publish-all-demos.ps1 -BackendUrl http://localhost:8080
.\deploy\generate-all-demos.ps1 -BackendUrl http://localhost:8080
.\deploy\capture-fundraising-evidence-bundle.ps1 -BackendUrl http://localhost:8080 -ContinueOnGenerateFailure
```

---

## Evidence bundle layout

```text
.tmp/
  credentials/<externalId>.json          # API credentials (P23-T12)
  generated_<externalId>.docx            # Runtime generate output (P23-T14)
  evidence/
    all-demos-publish-summary.json       # Publish orchestration summary (P23-T12)
    generated-docx-manifest.json         # SHA-256 + size + forbidden scan (P23-T14)
    audit-records/<externalId>.json      # SUCCESS generation audit (P23-T14)
    generated-docx/<externalId>.docx       # Optional copies for self-contained bundle
docs/evidence/
  demo-typography-review-checklist.md    # Human review checklist (P23-T16)
  fundraising-demo-summary.md            # This file
```

---

## Template → evidence matrix (13 published templates)

| # | externalId | Package / segment | Master asset | Generate output | Variables fixture | E2E case | POI / master test | Manifest entry |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `CORP-FOL-OFFER` | `deploy/demo-fol/` · CORP | `deploy/demo-fol/assets/wholesale-fol-master.docx` | `.tmp/generated_CORP-FOL-OFFER.docx` | `deploy/demo-fol/config/fol-demo-test-variables.json` | `demo-runtime-generate.spec.ts` | `FolMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (fol) | `generated-docx-manifest.json` |
| 2 | `DEMO-FULL-FLOW-LETTER` | full-flow seed · RETAIL | E2E fixture (no deploy package) | `.tmp/generated_DEMO-FULL-FLOW-LETTER.docx` | `frontend/e2e/fixtures/demo/full-flow-demo-test-variables.json` | `demo-runtime-generate.spec.ts` | `FullFlowMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (full-flow) | `generated-docx-manifest.json` |
| 3 | `DEMO-RETAIL-ACCOUNT-OPEN` | `deploy/demo-retail-account/` · RETAIL | `deploy/demo-retail-account/assets/retail-account-open-master.docx` | `.tmp/generated_DEMO-RETAIL-ACCOUNT-OPEN.docx` | `deploy/demo-retail-account/config/retail-account-demo-test-variables.json` (dataset `retail-open-executive`) | `demo-runtime-generate.spec.ts` | `RetailAccountMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (retail-open) | `generated-docx-manifest.json` |
| 4 | `DEMO-RETAIL-ACCOUNT-BALANCE` | `deploy/demo-retail-account/` · RETAIL | `deploy/demo-retail-account/assets/retail-account-balance-master.docx` | `.tmp/generated_DEMO-RETAIL-ACCOUNT-BALANCE.docx` | `deploy/demo-retail-account/config/retail-account-demo-test-variables.json` (dataset `retail-balance-executive`) | `demo-runtime-generate.spec.ts` | `RetailAccountMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (retail-balance) | `generated-docx-manifest.json` |
| 5 | `DEMO-MORTGAGE-APPROVAL` | `deploy/demo-mortgage/` · RETAIL | `deploy/demo-mortgage/assets/mortgage-approval-master.docx` | `.tmp/generated_DEMO-MORTGAGE-APPROVAL.docx` | `deploy/demo-mortgage/config/mortgage-demo-test-variables.json` | `demo-runtime-generate.spec.ts` | `MortgageMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (mortgage) | `generated-docx-manifest.json` |
| 6 | `DEMO-CREDIT-LIMIT-CONFIRM` | `deploy/demo-credit-limit/` · CORP | `deploy/demo-credit-limit/assets/credit-limit-master.docx` | `.tmp/generated_DEMO-CREDIT-LIMIT-CONFIRM.docx` | `deploy/demo-credit-limit/config/credit-limit-demo-test-variables.json` | `demo-runtime-generate.spec.ts` | `CreditLimitMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (credit-limit) | `generated-docx-manifest.json` |
| 7 | `DEMO-TRADE-LC-NOTICE` | `deploy/demo-trade-lc/` · TRADE | `deploy/demo-trade-lc/assets/trade-lc-notice-master.docx` | `.tmp/generated_DEMO-TRADE-LC-NOTICE.docx` | `deploy/demo-trade-lc/config/trade-lc-demo-test-variables.json` (dataset `trade-lc-executive`) | `demo-runtime-generate.spec.ts` | `TradeLcMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (trade-lc) | `generated-docx-manifest.json` |
| 8 | `DEMO-TRADE-GUARANTEE-NOTICE` | `deploy/demo-trade-lc/` · TRADE | `deploy/demo-trade-lc/assets/trade-guarantee-notice-master.docx` | `.tmp/generated_DEMO-TRADE-GUARANTEE-NOTICE.docx` | `deploy/demo-trade-lc/config/trade-lc-demo-test-variables.json` (dataset `trade-guarantee-executive`) | `demo-runtime-generate.spec.ts` | `TradeLcMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (trade-guarantee) | `generated-docx-manifest.json` |
| 9 | `DEMO-RATE-CHANGE-NOTICE` | `deploy/demo-collection/` · RETAIL | `deploy/demo-collection/assets/rate-change-notice-master.docx` | `.tmp/generated_DEMO-RATE-CHANGE-NOTICE.docx` | `deploy/demo-collection/config/collection-demo-test-variables.json` (dataset `rate-change-executive`) | `demo-runtime-generate.spec.ts` | `CollectionMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (collection-rate) | `generated-docx-manifest.json` |
| 10 | `DEMO-OVERDUE-COLLECTION` | `deploy/demo-collection/` · RETAIL | `deploy/demo-collection/assets/overdue-collection-master.docx` | `.tmp/generated_DEMO-OVERDUE-COLLECTION.docx` | `deploy/demo-collection/config/collection-demo-test-variables.json` (dataset `overdue-collection-executive`) | `demo-runtime-generate.spec.ts` | `CollectionMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (collection-overdue) | `generated-docx-manifest.json` |
| 11 | `DEMO-ANNUAL-REVIEW` | `deploy/demo-annual-review/` · CORP | `deploy/demo-annual-review/assets/annual-review-master.docx` | `.tmp/generated_DEMO-ANNUAL-REVIEW.docx` | `deploy/demo-annual-review/config/annual-review-demo-test-variables.json` (dataset `annual-review-executive`) | `demo-runtime-generate.spec.ts` | `AnnualReviewMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (annual-review) | `generated-docx-manifest.json` |
| 12 | `DEMO-FACILITY-RENEWAL` | `deploy/demo-annual-review/` · CORP | `deploy/demo-annual-review/assets/facility-renewal-master.docx` | `.tmp/generated_DEMO-FACILITY-RENEWAL.docx` | `deploy/demo-annual-review/config/annual-review-demo-test-variables.json` (dataset `facility-renewal-executive`) | `demo-runtime-generate.spec.ts` | `AnnualReviewMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (facility-renewal) | `generated-docx-manifest.json` |
| 13 | `DEMO-WEALTH-STATEMENT` | `deploy/demo-wealth/` · WEALTH | `deploy/demo-wealth/assets/wealth-statement-master.docx` | `.tmp/generated_DEMO-WEALTH-STATEMENT.docx` | `deploy/demo-wealth/config/wealth-demo-test-variables.json` | `demo-runtime-generate.spec.ts` | `WealthMasterDocxAssetGeneratorTest`; `DemoTypographyLayoutRegressionTest` (wealth) | `generated-docx-manifest.json` |

**Registry source of truth:** `Get-DemoPublishExternalIds` in `deploy/demo-import-shared.ps1` (**13** IDs). Runtime manifest: `deploy/demo-shared/demo-runtime-generate-manifest.json`.

---

## Automated acceptance cross-reference

| Layer | Artifact | Scope | BDD |
| --- | --- | --- | --- |
| **Publish** | `deploy/publish-all-demos.ps1` | Lifecycle + API policy + credentials | TYP-011 |
| **Generate script** | `deploy/generate-all-demos.ps1` | 13 DOCX + manifest + audit JSON | TYP-011, 013 |
| **E2E** | `frontend/e2e/demo-runtime-generate.spec.ts` | 13 runtime cases; HTTP 200 + size floor | TYP-011, 012 |
| **E2E helpers** | `frontend/e2e/helpers/demo-runtime-api.ts`, `frontend/src/utils/demoRuntimeRegistry.ts` | Publish registry + min bytes | TYP-012 |
| **Vitest** | `frontend/tests/demo-runtime-api.test.ts` | Registry + artifact assertions | TYP-012 |
| **POI suite** | `DemoTypographyLayoutAssertions` + `DemoTypographyLayoutRegressionTest` | 13 master cases × 25 assertions | TYP-001–004, 013, 014, 016, 018 |
| **POI unit** | `DemoTypographyLayoutAssertionsTest` | Assertion helper coverage | TYP-015 foundation |
| **Font baseline** | `RenderingFontSmokeTest`; Docker Carlito/Caladea/Noto CJK | Mixed-script PDF smoke | TYP-009, 010 |
| **Style manifest** | `deploy/demo-shared/demo-bank-style-manifest.json` | Shared bank style keys | TYP-016 |
| **Human review** | [demo-typography-review-checklist.md](./demo-typography-review-checklist.md) | ≥2 CORP + ≥2 RETAIL samples | TYP-015, 020 |

---

## Gate commands (evidence regeneration)

| Gate | Command |
| --- | --- |
| Backend full | `mvn -B -ntp -f backend/pom.xml verify` |
| Frontend full | `pnpm -C frontend lint && pnpm -C frontend type-check && pnpm -C frontend test && pnpm -C frontend build` |
| E2E demos | `pnpm -C frontend test:e2e:docker:demos` |
| Import | `.\deploy\import-all-demos.ps1 -BackendUrl http://localhost:8080` |
| Publish | `.\deploy\publish-all-demos.ps1 -BackendUrl http://localhost:8080` |
| Generate + manifest | `.\deploy\generate-all-demos.ps1 -BackendUrl http://localhost:8080` |

---

## Human review status (BDD-DEMO-TYP-020)

| Requirement | Status |
| --- | --- |
| Checklist template published | **Done** (2026-07-08) |
| ≥2 CORP samples reviewed | **Pending** — `CORP-FOL-OFFER`, `DEMO-CREDIT-LIMIT-CONFIRM` |
| ≥2 RETAIL samples reviewed | **Pending** — `DEMO-MORTGAGE-APPROVAL`, `DEMO-RETAIL-ACCOUNT-OPEN` |
| Reviewer sign-off archived | **Pending** — operational follow-up |

---

## Traceability

| Document | Relationship |
| --- | --- |
| [bank-letter-demo-refresh.md](../behavior/bank-letter-demo-refresh.md) | Wave A content refresh BDD — **Done** (`aa88170f`) |
| [bank-letter-demo-refresh plan](../plan/detail/bank-letter-demo-refresh.md) | Wave A exit criteria + vetoes (Wave A Done) |
| [plan/evidence/bank-letter-demo-refresh/](../plan/evidence/bank-letter-demo-refresh/README.md) | Wave A generate artifacts **13/13** |
| [demo-typography-layout-behavior-spec.md](../requirements/demo-typography-layout-behavior-spec.md) | Observable evidence §12 (P23 — **Done**; do not reopen) |
| [P23 detail plan](../plan/detail/P23-demo-typography-layout-excellence.md) | P23-T14/T15/T16 |
| [deploy/demo-shared/README.md](../../deploy/demo-shared/README.md) | Publish + generate orchestration + Wave A cleanup path |
| `.taskmaster/tasks/tasks.json` task **#8** | Historical fundraising evidence bundle (**Done**) |
| `.taskmaster/tasks/tasks.json` task **#141** | Wave A refresh (**done**) |

---

## Change log

| Version | Date | Description |
| --- | --- | --- |
| 1.1.1 | 2026-07-20 | Wave A **Done** (`aa88170f`); evidence 13/13; Wave B #142 still pending |
| 1.1.0 | 2026-07-20 | Wave A In Progress cross-links; ops-safe cleanup note; 13-ID coverage unchanged; **not** claiming refresh Done |
| 1.0.0 | 2026-07-08 | Initial evidence index — 13 templates; P23-T16 |
