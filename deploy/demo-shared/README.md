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
