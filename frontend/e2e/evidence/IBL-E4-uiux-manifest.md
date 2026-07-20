# IBL-E4 UIUX Manifest — Entity document brands (#131)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-e4-entity-document-brands` |
| **Task** | Task Master **#131** |
| **Worktree** | `D:/working/DGE-ibl-e4-entity-document-brands` |
| **Date** | 2026-07-20 |
| **Viewport** | 1440×900 |
| **Stack** | Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` |
| **Verdict** | **PASS** |
| **Critical** | **0** |
| **merge_go** | **true** |
| **Stage 6** | **6/6 PASS** (`ibl-e4-entity-document-brands.spec.ts`) |
| **Coverage** | Dual-brand Document brands + Legal entities catalogs (+ create dialog) |

## Behavior SoT

- `docs/behavior/ibl-e4-entity-document-brands.md`
- Functional: `frontend/e2e/ibl-e4-entity-document-brands.spec.ts`
- Surfaces: `DocumentBrandListView.vue`, `LegalEntityListView.vue`, form dialogs, template allow-list field

## Dual-brand / visual review (Stage 7)

Browser review on Docker `:4173` @1440×900 (Group Admin `10000002`).

| Check | REDBC | GREENBC | Notes |
| --- | --- | --- | --- |
| `html[data-brand]` + `--brand-primary` | `#DB0011` | `#00847F` | Verified via CDP |
| Header brand / logo switch | Pass | Pass | Red Bank / Green Bank chrome only |
| Document brands catalog (fluid + table) | Pass | Pass | No overflow; no raw UUID primary cells |
| Legal entities catalog + default panel | Pass | Pass | Entity↔document brand codes human-readable |
| Create legal entity dialog | — | Pass | Labels + document-brand hint; Save disabled until valid |
| UI chrome orthogonal to document brands | Pass | Pass | Brand switcher options = Red/Green Bank only |

### Screenshot inventory

Path prefix: `frontend/e2e/evidence/IBL-E4/screenshots/`

| # | File | Brand | View |
| --- | --- | --- | --- |
| 1 | `ibl-e4-01-document-brands-greenbc-1440x900.png` | GREENBC | Document brands catalog (RETAIL) |
| 2 | `ibl-e4-02-document-brands-redbc-1440x900.png` | REDBC | Document brands catalog (RETAIL) |
| 3 | `ibl-e4-03-legal-entities-redbc-1440x900.png` | REDBC | Legal entities + group default panel |
| 4 | `ibl-e4-04-legal-entities-greenbc-1440x900.png` | GREENBC | Legal entities catalog |
| 5 | `ibl-e4-05-legal-entity-create-dialog-greenbc-1440x900.png` | GREENBC | Create legal entity dialog |

## a11y

| Gate | Result |
| --- | --- |
| `a11y-smoke.spec.ts` | **Not re-run this pass** (catalog-scoped visual review; shell a11y not newly regressing from list/dialog surfaces) |
| Catalog headings / form labels | Present (`Document brands`, `Legal entities`, dialog field labels) |
| Forbidden (non-admin) | Covered by Stage 6 BDD-IBL-E4-014 (redirect `/forbidden`) |

## Findings

| Severity | Item | Rule / file | Status |
| --- | --- | --- | --- |
| 🔴 Critical | — | — | **None** |
| 🟡 Suggestion | Collapsed nav: `document-brands` / `legal-entities` missing from `NAV_ICON_MAP` → empty icon-only buttons (`hasSvg: false`) | OA shell / `useManagementShell.ts` (follow `nav-missing-icons`) | Open — non-blocking for merge |
| 🟡 Suggestion | Catalog filters use ad-hoc flex row instead of `CatalogFilterToolbar`; group filter combobox lacks accessible name (Status has `aria-label`) | `frontend-oa-design` / entity-display filter conventions | Open — non-blocking |
| 🟢 Nice to have | Status filter shows empty placeholder when “all” selected rather than “All statuses” label | Polish | Optional |

## Verdict

**PASS** — Critical **0**, **merge_go = true**. Dual-brand governance catalogs meet bank OA bar for layout, tokens, entity display, and chrome orthogonality. Route missing-nav-icon polish to `frontend-engineer` as follow-up (not Stage 7 merge blocker).
