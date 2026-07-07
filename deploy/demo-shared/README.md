# Demo shared bank style manifest (P23-T03)

Canonical Word style catalog and layout baseline for all `deploy/demo-*` master DOCX assets.

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

- [demo-typography-layout-behavior-spec.md](../../docs/requirements/demo-typography-layout-behavior-spec.md)
- [P23 detail plan](../../docs/plan/detail/P23-demo-typography-layout-excellence.md) — task **P23-T03**

## Publish orchestration (P23-T12)

After v3 bank-grade rewrites, import then publish all runtime-callable demo templates:

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

Contract tests: `DemoPublishOrchestrationContractTest` (BDD-DEMO-TYP-011).
