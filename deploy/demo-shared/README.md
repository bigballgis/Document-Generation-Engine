# Demo shared bank style + acceptance catalog helpers

Canonical Word style catalog / layout baseline for keep-set `deploy/demo-*` master DOCX assets, plus shared import/publish/generate helpers.

## Acceptance catalog SoT (KEEP-8 — current)

**Status:** **Authoritative for screenshot / acceptance demos** (Task Master **#164** / slice `demo-catalog-keep-bank-letters` → **In Progress**; keep-set registries + package purge + Java seeder retirement landed in feature worktree — acceptance cleanup via `deploy/cleanup-demo-catalog-keep-list.ps1` scheduled for Stage 5/10).

User confirmation **2026-07-24** contracted the Live demo catalog to **8** bank-letter templates. This supersedes the Wave B **20**-ID registry as the **default acceptance catalog size**. Wave A/B remain **Done** historical quality work; non-KEEP packages are **removed from repo**.

| Item | Value |
| --- | --- |
| Intent | Keep only the eight Live bank-letter templates + transitive letterhead / clauses / assets; purge other DEMO noise; retire Java template seeders that dual-source the catalog |
| Behavior SoT | [demo-catalog-keep-bank-letters.md](../../docs/behavior/demo-catalog-keep-bank-letters.md) (`BDD-DEMO-KEEP-001`…`014`) |
| Ops runbook | [demo-catalog-keep-bank-letters.md](../../docs/operations/demo-catalog-keep-bank-letters.md) |
| Plan | [demo-catalog-keep-bank-letters.md](../../docs/plan/detail/demo-catalog-keep-bank-letters.md) |
| Evidence stub | [docs/plan/evidence/demo-catalog-keep-bank-letters/](../../docs/plan/evidence/demo-catalog-keep-bank-letters/README.md) |
| Authoritative load path | **Deploy package + PowerShell** `import-all-demos.ps1` → `publish-all-demos.ps1` (optional `generate-all-demos.ps1`) — **not** Java `ApplicationRunner` auto-seed |
| Hard vetoes | Do **not** flip checklist **#3b/#5a GO**; do **not** mark **#53** / **#106** Done; do **not** touch CE-O02; do **not** claim go-live / IBL / CE Done |

### KEEP-8 Live templates (confirmed)

| # | externalId | Deploy package |
| --- | --- | --- |
| 1 | `DEMO-COVENANT-WAIVER` | `deploy/demo-covenant-waiver` |
| 2 | `DEMO-FORMAL-DEMAND` | `deploy/demo-formal-demand` |
| 3 | `DEMO-COMMITMENT-LETTER` | `deploy/demo-commitment` |
| 4 | `DEMO-FACILITY-AMENDMENT` | `deploy/demo-facility-amendment` |
| 5 | `DEMO-ANNUAL-REVIEW` | `deploy/demo-annual-review` |
| 6 | `DEMO-FACILITY-RENEWAL` | `deploy/demo-annual-review` |
| 7 | `DEMO-CREDIT-LIMIT-CONFIRM` | `deploy/demo-credit-limit` |
| 8 | `CORP-FOL-OFFER` | `deploy/demo-fol` |

**Retain:** `deploy/demo-shared/` (this directory) for bank style + runtime generate helpers scoped to KEEP. `demo-runtime-generate-manifest.json` and `Get-DemoPublishExternalIds` list **only** these eight externalIds (`expectedCount=8`).

### PURGE (completed in repo; run cleanup script against acceptance DB)

| Category | Items |
| --- | --- |
| **Deploy packages removed** | `demo-retail-account`, `demo-mortgage`, `demo-trade-lc`, `demo-collection`, `demo-wealth`, `demo-kyc-cdd`, `demo-account-closure`, `demo-insurance-endorsement` |
| **Java-only template IDs retired** | `DEMO-FULL-FLOW-LETTER`, `DEMO-RETAIL-LETTER` (seeders deleted) |
| **Java classes removed** | `DemoFullFlowCatalogSeeder`, `DemoFullFlowPublishSupport`, `DemoCatalogSeeder`, `DemoCatalogSeedProperties` |
| **Java retained** | `DemoRetailLetterheadDocxBuilder` + `DemoDocxFactory` (CatalogLoadSeeder / E2E fixtures); `DemoAssetLibrarySeeder`, `CatalogLoadSeeder` (`LOAD-TPL-*`) — default `false` |
| **Cleanup script** | `deploy/cleanup-demo-catalog-keep-list.ps1` (FOL-only script redirects here) |

### Why Java template seeders existed / why retired

| | Confirmed |
| --- | --- |
| **Historical why** | Boot-time `ApplicationRunner` auto-seed + in-JVM DOCX builders gave acceptance/E2E a minimal catalog **without** PowerShell import |
| **Why retire now** | Dual catalog sources (deploy import vs Java seed) let purged IDs return on reboot; KEEP-8 SoT requires a **single** load path |

Full ops note: behavior §9 + [operations runbook §3](../../docs/operations/demo-catalog-keep-bank-letters.md#3-ops-note--historical-java-seeders-confirmed).

### Confirmed vs pending

| Kind | Content |
| --- | --- |
| **Confirmed** | KEEP-8 / PURGE inventories; PowerShell import as SoT load path; seeder retirement rationale; vetoes |
| **Pending (implementers)** | Delete purge packages from disk; shrink import/publish/generate registries; retire Java seeder classes; land `cleanup-demo-catalog-keep-list.ps1`; archive Docker/verify evidence |

Until registry shrink lands, scripts on disk may still reference Wave B packages — treat that as **implementation lag**, not a reversion of KEEP SoT.

---

## Historical context (Wave A / Wave B — Done)

### Post-remediation content refresh (Wave A — Done)

**Status:** **Done** (Task Master **#141** / slice `bank-letter-demo-refresh`) — MAIN merge `aa88170f`; evidence **13/13**.

| Item | Value |
| --- | --- |
| Intent | Clean shallow/padding/test-flavored demo content; uplift **then-existing** eight `deploy/demo-*` packages + `DEMO-FULL-FLOW-LETTER` to bank-letter quality |
| Behavior SoT | [bank-letter-demo-refresh.md](../../docs/behavior/bank-letter-demo-refresh.md) (`BDD-DEMO-REFRESH-001`…`014`) |
| Evidence | [docs/plan/evidence/bank-letter-demo-refresh/](../../docs/plan/evidence/bank-letter-demo-refresh/README.md) |

### Catalogue expand (Wave B — Done)

**Status:** **Done** (Task Master **#142** / slice `bank-letter-demo-expand`) — MAIN merge `288ce98f`; generate **20/20**. Sole-active **cleared** (successor **#164** KEEP leaf).

| Item | Value |
| --- | --- |
| Intent | Added seven bank-practice letter families; expanded publish/runtime registry **13 → 20** |
| Behavior SoT | [bank-letter-demo-expand.md](../../docs/behavior/bank-letter-demo-expand.md) (`BDD-DEMO-EXPAND-001`…`016`) |
| Evidence | [docs/plan/evidence/bank-letter-demo-expand/](../../docs/plan/evidence/bank-letter-demo-expand/README.md) |
| Relation to KEEP | Wave B quality bar retained for KEEP families; **non-KEEP** Wave A/B packages are purge targets under **#164** |

**Do not** treat the historical **20**-ID table below as the current acceptance default — see **KEEP-8** above.

---

## Style keys

| Style key | Purpose | Typical use |
| --- | --- | --- |
| `Heading1` | Document title / part heading | Cover title, major part breaks |
| `Heading2` | Clause group heading | Facility terms, covenant sections |
| `Heading3` | Sub-clause heading | Nested operative clauses |
| `ClauseBody` | Operative paragraph body | Default binding paragraph `styleRef` |
| `DefinedTerm` | Defined terms emphasis | CORP credit / facility letters |
| `TableHeader` | Schedule table header row | Amortization, covenant, checklist tables |
| `ScheduleTitle` | Schedule section title | Text between heading and table |
| `SignatureBlock` | Closing signature area | Authorized signatory block |

## Layout baseline

- **Page size:** A4 (11906 × 16838 twips)
- **Margins:** 2.54 cm (1440 twips) on all sides — see `demo-bank-style-manifest.json` → `layout.marginsTwips`
- **Header/footer distance:** 708 twips from edge (default Word-like)

## Fonts

| Role | Development (host) | Docker substitute (P23-T02) |
| --- | --- | --- |
| Body Latin | Calibri | Carlito |
| Heading Latin | Cambria | Caladea |
| CJK mixed content | — | Noto Sans CJK SC |

Bindings must reference style keys from this manifest via `styleRef` or `sectionHeading` nodes. Build-time `*MasterDocxAssetGeneratorTest` classes apply the catalog through `DemoMasterDocxStyleSupport`.

## Machine-readable manifest

`demo-bank-style-manifest.json` is the source of truth for generator tests. When styles or margins change, bump `manifestVersion` and regenerate affected `deploy/demo-*/assets/*.docx`.

## BDD traceability

- `BDD-DEMO-TYP-001`…`004` — named styles in master catalog
- `BDD-DEMO-TYP-016` — full catalog at build time
- `BDD-DEMO-TYP-018` — margin baseline ≥ 2.54 cm
- `BDD-DEMO-KEEP-001`…`014` — slim catalog keep-set / seeder retirement

## Related

- [demo-catalog-keep-bank-letters.md](../../docs/behavior/demo-catalog-keep-bank-letters.md) — **current** acceptance KEEP SoT (**#164** In Progress)
- [docs/operations/demo-catalog-keep-bank-letters.md](../../docs/operations/demo-catalog-keep-bank-letters.md) — ops runbook + Java seeder retirement note
- [bank-letter-demo-refresh.md](../../docs/behavior/bank-letter-demo-refresh.md) — Wave A content refresh BDD (**Done**)
- [bank-letter-demo-expand.md](../../docs/behavior/bank-letter-demo-expand.md) — Wave B catalogue expand BDD (**Done**)
- [demo-typography-layout-behavior-spec.md](../../docs/requirements/demo-typography-layout-behavior-spec.md) — P23 typography (**Done**; do not reopen)
- [demo-expansion-behavior-spec.md](../../docs/requirements/demo-expansion-behavior-spec.md) — P22 engine + scaffolds (**Done**; do not reopen)
- [P23 detail plan](../../docs/plan/detail/P23-demo-typography-layout-excellence.md) — task **P23-T03**
- [Wave A plan](../../docs/plan/detail/bank-letter-demo-refresh.md)

## Ops-safe cleanup → reimport (KEEP preferred path)

Prefer **keep-set cleanup** + **idempotent re-import overwrite** over destructive database surgery. **Forbidden:** reckless whole-DB `DROP`, second Compose project, or parallel Docker stacks.

```powershell
# From repo root — acceptance stack healthy on :8080 / :4173
# Deploy only via: .\scripts\docker-deploy-queue.ps1

# 1) Keep-set catalog cleanup (planned — implementers land script)
#    Do NOT use FOL-only cleanup-catalog-except-fol.ps1 for this leaf
#    (it would delete the other seven KEEP templates).
# .\deploy\cleanup-demo-catalog-keep-list.ps1 -WhatIf
# .\deploy\cleanup-demo-catalog-keep-list.ps1

# 2) Optional FOL test-data cleanup (supports -WhatIf) — FOL package only
.\deploy\demo-fol\cleanup-fol-test-data-sets.ps1 -WhatIf
.\deploy\demo-fol\cleanup-fol-test-data-sets.ps1

# 3) Import-all overwrite (KEEP packages only — after registry shrink)
.\deploy\import-all-demos.ps1

# 4) Publish + generate evidence (expectedCount=8 after shrink)
.\deploy\publish-all-demos.ps1
.\deploy\generate-all-demos.ps1
```

| Tool | Role | Notes |
| --- | --- | --- |
| `deploy/cleanup-demo-catalog-keep-list.ps1` | Keep-8 catalog cleanup | **Planned** — supersedes FOL-only cleanup for this leaf |
| `deploy/demo-fol/cleanup-catalog-except-fol.ps1` | Historical FOL-only strip | **Unsafe** for KEEP-8 (deletes other seven keep IDs) |
| `deploy/demo-fol/cleanup-fol-test-data-sets.ps1` | Remove duplicate FOL executive test data sets | Prefer with `-WhatIf` first |
| `deploy/demo-import-shared.ps1` DRAFT reset | Local-demo lifecycle reset so bindings/variables can refresh | Invoked by package import; not a DB DROP |
| `deploy/import-all-demos.ps1` | Primary overwrite of demo packages | Target: **KEEP packages only** (after implementer shrink) |
| `deploy/publish-all-demos.ps1` / `generate-all-demos.ps1` | Lifecycle + DOCX evidence | Target: **8** keep externalIds |

BDD: `BDD-DEMO-KEEP-001`…`014` (current); Wave A/B scenarios remain historical evidence.

## Publish orchestration (target after #164)

After KEEP packages are ready:

```powershell
# From repo root — backend must be healthy on :8080
.\deploy\import-all-demos.ps1
.\deploy\publish-all-demos.ps1
```

| Step | Script | Templates (target) |
| --- | --- | --- |
| Import | `deploy/import-all-demos.ps1` | Seven KEEP packages → eight template IDs |
| Full-flow seed | ~~`DemoFullFlowCatalogSeeder`~~ | **Retired** — do not enable for slim catalog |
| Publish | `deploy/publish-all-demos.ps1` | Exactly **8** keep externalIds via `Get-DemoPublishExternalIds` |

### Historical Wave A+B coverage (20 IDs — superseded for acceptance default)

Retained for audit of Wave A/B Done evidence only. **Current SoT = KEEP-8 table above.**

<details>
<summary>Historical 20-ID publish registry (Wave A 13 + Wave B 7)</summary>

**Wave A (13 — historical):**

| externalId | groupCode | API policy AD group |
| --- | --- | --- |
| `CORP-FOL-OFFER` | CORP | `CORP_API` |
| `DEMO-FULL-FLOW-LETTER` | RETAIL | `RETAIL_API` |
| `DEMO-RETAIL-ACCOUNT-OPEN` / `BALANCE` | RETAIL | `RETAIL_API` |
| `DEMO-MORTGAGE-APPROVAL` | RETAIL | `RETAIL_API` |
| `DEMO-CREDIT-LIMIT-CONFIRM` | CORP | `CORP_API` |
| `DEMO-TRADE-LC-NOTICE` / `GUARANTEE-NOTICE` | TRADE | `RETAIL_API` |
| `DEMO-RATE-CHANGE-NOTICE` / `OVERDUE-COLLECTION` | RETAIL | `RETAIL_API` |
| `DEMO-ANNUAL-REVIEW` / `FACILITY-RENEWAL` | CORP | `CORP_API` |
| `DEMO-WEALTH-STATEMENT` | WEALTH | `RETAIL_API` |

**Wave B (+7 — historical):**

| externalId | groupCode | API policy AD group |
| --- | --- | --- |
| `DEMO-FACILITY-AMENDMENT` | CORP | `CORP_API` |
| `DEMO-KYC-CDD-NOTICE` | RETAIL | `RETAIL_API` |
| `DEMO-ACCOUNT-CLOSURE` | RETAIL | `RETAIL_API` |
| `DEMO-COMMITMENT-LETTER` | CORP | `CORP_API` |
| `DEMO-FORMAL-DEMAND` | CORP | `CORP_API` |
| `DEMO-COVENANT-WAIVER` | CORP | `CORP_API` |
| `DEMO-INSURANCE-ENDORSEMENT` | RETAIL | `RETAIL_API` |

</details>

Runtime callers `svc-caller` and `e2e-runtime-caller` are granted **both** `RETAIL_API` and `CORP_API` in `application.yml`.

### Outputs

- `.tmp/credentials/<externalId>.json` — API credential bundles for runtime generate (P23-T14)
- `.tmp/evidence/all-demos-publish-summary.json` — publish evidence table (**target `expectedCount=8`**)

### Runtime generate

After publish, generate executive DOCX artifacts for KEEP templates only:

```powershell
.\deploy\generate-all-demos.ps1
```

| Step | Script | Outputs |
| --- | --- | --- |
| Generate | `deploy/generate-all-demos.ps1` | `.tmp/generated_<externalId>.docx` |
| Manifest | (same script) | `.tmp/evidence/generated-docx-manifest.json` |
| Audit | (same script) | `.tmp/evidence/audit-records/<externalId>.json` |

Manifest source: `deploy/demo-shared/demo-runtime-generate-manifest.json` (must mirror KEEP-8 after implementer shrink).

Contract tests: `DemoPublishOrchestrationContractTest` / `DemoGenerateOrchestrationContractTest` — assert keep-set of **8** under `mvn verify` after #164 implementation.
