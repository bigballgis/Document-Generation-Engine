# Demo shared bank style manifest (P23-T03)

Canonical Word style catalog and layout baseline for all `deploy/demo-*` master DOCX assets.

## Post-remediation content refresh (Wave A — In Progress)

**Status:** **In Progress** (Task Master **#141** / slice `bank-letter-demo-refresh`) — **do not** treat as Done until import → publish → generate evidence is archived.

| Item | Value |
| --- | --- |
| Intent | Clean shallow/padding/test-flavored demo content; uplift **existing** eight `deploy/demo-*` packages + `DEMO-FULL-FLOW-LETTER` to 100% realistic bank-letter quality (mock parties/amounts only) |
| Behavior SoT | [bank-letter-demo-refresh.md](../../docs/behavior/bank-letter-demo-refresh.md) (`BDD-DEMO-REFRESH-001`…`014`) |
| Plan | [bank-letter-demo-refresh.md](../../docs/plan/detail/bank-letter-demo-refresh.md) |
| Evidence stub | [docs/plan/evidence/bank-letter-demo-refresh/](../../docs/plan/evidence/bank-letter-demo-refresh/README.md) |
| Wave B | **OUT** this leaf — queued as TM **#142** / `bank-letter-demo-expand` (new letter families) |
| Hard vetoes | Do **not** flip checklist **#3b/#5a GO**; do **not** reopen RTL / CE-O02; do **not** invent Word-host evidence |

Template coverage table below remains the **publish/runtime registry** (13 `externalId`s). Wave A rewrites content **inside** those packages; it does **not** add new product families.

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

## Related

- [bank-letter-demo-refresh.md](../../docs/behavior/bank-letter-demo-refresh.md) — Wave A content refresh BDD (**In Progress**; not Done)
- [demo-typography-layout-behavior-spec.md](../../docs/requirements/demo-typography-layout-behavior-spec.md) — P23 typography (**Done**; do not reopen)
- [demo-expansion-behavior-spec.md](../../docs/requirements/demo-expansion-behavior-spec.md) — P22 engine + scaffolds (**Done**; do not reopen)
- [P23 detail plan](../../docs/plan/detail/P23-demo-typography-layout-excellence.md) — task **P23-T03**
- [Wave A plan](../../docs/plan/detail/bank-letter-demo-refresh.md)

## Ops-safe cleanup → reimport (Wave A preferred path)

Prefer **idempotent re-import overwrite** over destructive database surgery. **Forbidden:** reckless whole-DB `DROP`, second Compose project, or parallel Docker stacks.

```powershell
# From repo root — acceptance stack healthy on :8080 / :4173
# Deploy only via: .\scripts\docker-deploy-queue.ps1

# 1) Optional FOL test-data cleanup (supports -WhatIf)
.\deploy\demo-fol\cleanup-fol-test-data-sets.ps1 -WhatIf
.\deploy\demo-fol\cleanup-fol-test-data-sets.ps1

# 2) Optional — only when intentionally stripping non-FOL catalog leftovers
#    (narrow; do NOT use as default full refresh)
# .\deploy\demo-fol\cleanup-catalog-except-fol.ps1 -WhatIf

# 3) Import-all overwrite (preferred primary refresh mechanism)
#    Per-package import uses demo-import-shared DRAFT reset when needed
#    (local demo only — not a DROP). Bump catalogMarker / masterLayoutVersion
#    in package configs when content materially changes.
.\deploy\import-all-demos.ps1

# 4) Publish + generate evidence
.\deploy\publish-all-demos.ps1
.\deploy\generate-all-demos.ps1
```

| Tool | Role | Notes |
| --- | --- | --- |
| `deploy/demo-fol/cleanup-fol-test-data-sets.ps1` | Remove duplicate FOL executive test data sets; keep one canonical row | Prefer with `-WhatIf` first |
| `deploy/demo-fol/cleanup-catalog-except-fol.ps1` | Narrow catalog strip (non-FOL leftovers) | **Not** the default full Wave A path |
| `deploy/demo-import-shared.ps1` DRAFT reset | Local-demo lifecycle reset so bindings/variables can refresh | Invoked by package import; not a DB DROP |
| `deploy/import-all-demos.ps1` | **Primary** overwrite of all eight packages | Full-flow via existing seeder path |
| `deploy/publish-all-demos.ps1` / `generate-all-demos.ps1` | Lifecycle + DOCX evidence | Same 13-ID registry as coverage table |

BDD: `BDD-DEMO-REFRESH-001`…`004`.

## Publish orchestration (P23-T12)

After bank-grade package content is ready (P23 historical rewrite **or** Wave A refresh), import then publish all runtime-callable demo templates:

```powershell
# From repo root — backend must be healthy on :8080
.\deploy\import-all-demos.ps1
.\deploy\publish-all-demos.ps1
```

| Step | Script | Templates |
| --- | --- | --- |
| Import | `deploy/import-all-demos.ps1` | 8 packages → 12 external IDs from `*-template-config.json` |
| Full-flow seed | `DemoFullFlowCatalogSeeder` (when `docgen.demo-catalog.seed-enabled=true`) | `DEMO-FULL-FLOW-LETTER` |
| Publish | `deploy/publish-all-demos.ps1` | All **13** external IDs via `Get-DemoPublishExternalIds` |

### Template coverage (publish registry)

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

Runtime callers `svc-caller` and `e2e-runtime-caller` are granted **both** `RETAIL_API` and `CORP_API` in `application.yml`.

### Outputs

- `.tmp/credentials/<externalId>.json` — API credential bundles for runtime generate (P23-T14)
- `.tmp/evidence/all-demos-publish-summary.json` — publish evidence table

### Runtime generate (P23-T14)

After publish, generate executive DOCX artifacts for all **13** templates:

```powershell
.\deploy\generate-all-demos.ps1
```

| Step | Script | Outputs |
| --- | --- | --- |
| Generate | `deploy/generate-all-demos.ps1` | `.tmp/generated_<externalId>.docx` |
| Manifest | (same script) | `.tmp/evidence/generated-docx-manifest.json` |
| Audit | (same script) | `.tmp/evidence/audit-records/<externalId>.json` |

Manifest source: `deploy/demo-shared/demo-runtime-generate-manifest.json` (mirrors `DEMO_RUNTIME_CASES` / `demoRuntimeRegistry.ts`).

Contract tests: `DemoPublishOrchestrationContractTest` (BDD-DEMO-TYP-011), `DemoGenerateOrchestrationContractTest` (BDD-DEMO-TYP-012/013).
