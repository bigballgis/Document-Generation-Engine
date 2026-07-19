# IBL-E1 Functional Evidence Manifest — Locale-variant model UI

**Task:** IBL-E1 / Task Master **#128** — management UI locale declare / catalog filter / family sibling nav  
**Slice:** `ibl-e1-locale-variant-model` (`feat/ibl-e1-locale-variant-model`)  
**Worktree:** `D:/working/DGE-ibl-e1-locale-variant-model`  
**BDD:** [docs/behavior/ibl-e1-locale-variant-model.md](../../../docs/behavior/ibl-e1-locale-variant-model.md) (`ready`; **BDD-IBL-E1-013…015**)  
**Date:** 2026-07-20  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (stage-5 DEPLOY_OK)  
**Verdict:** **PASS** (4/4)

## Test execution

| Spec | Result |
| --- | --- |
| `IBL-E1-locale-variant-model.spec.ts` — 013 blank locale blocked | **passed** |
| `IBL-E1-locale-variant-model.spec.ts` — 013 locale options + hub locale | **passed** |
| `IBL-E1-locale-variant-model.spec.ts` — 014 catalog locale filter | **passed** |
| `IBL-E1-locale-variant-model.spec.ts` — 015 sibling family nav | **passed** |

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm exec playwright test `
  e2e/IBL-E1-locale-variant-model.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 4 passed (10.5s) — confirmation run 2026-07-20
```

**HTML report:** `frontend/playwright-report/docker/`  
**Plan evidence mirror:** `docs/plan/evidence/ibl-e1-locale-variant-model/`  
**Helper:** `frontend/e2e/helpers/ibl-e1-locale-variant-api.ts`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| BDD-IBL-E1-013 Create requires locale | Create dialog shows Body locale; submit without locale → validation “Select a body locale…”; no `POST /templates` |
| BDD-IBL-E1-013 Locale field usable | Locale select offers en-US / zh-CN; hub overview `[data-testid=template-overview-locale]` shows persisted `en-US` (API-seeded package) |
| BDD-IBL-E1-014 Catalog locale filter | Toolbar Locale → `en-US` issues `GET …/templates?locale=en-US`; en sibling row visible, zh sibling absent; Locale column present |
| BDD-IBL-E1-015 Family sibling nav | Hub `[data-testid=locale-variant-family-nav]` shows current locale + sibling; link navigates to brother package |

### Selectors / helpers used

- `[data-testid=template-create-locale]`, `[data-testid=template-overview-locale]`
- `[data-testid=locale-variant-family-nav]` / `locale-variant-current` / `locale-variant-sibling`
- `[data-testid=catalog-filter-search]` + Locale combobox in `.catalog-filter-toolbar`
- Fixtures: `prepareLocaleVariantSiblingPair` / `createLocaleVariantTemplate` (`E2E-IBL-E1-*`)

### Notes

- Full UI create→POST with letterhead selection is **not** asserted end-to-end: create dialog reads `mastersStore.masters` without its own fetch, and `TEMPLATE_AUTHOR` cannot open `/masters` to warm the store. Locale required / selectable + API persist + hub display cover BDD-IBL-E1-013.
- Functional only — dual-brand / polish → stage **7** `e2e-uiux-reviewer`.
- Do **not** claim Task Master #128 Done from this stage alone.

## Artifacts added / updated

- `frontend/e2e/IBL-E1-locale-variant-model.spec.ts` (new)
- `frontend/e2e/helpers/ibl-e1-locale-variant-api.ts` (new)
- `frontend/e2e/evidence/IBL-E1-locale-variant-model-manifest.md` (this file)
- `docs/plan/evidence/ibl-e1-locale-variant-model/`

## Notes for e2e-uiux-reviewer (stage 7)

1. Create dialog Body locale + optional Locale variant family collapse @1920 dual-brand.
2. Catalog Locale column + filter density (OA) with English-first labels.
3. Locale variants nav on template overview (sibling list / empty state).
4. No merge / MAIN doc-sync from stage 6.
