# Knip scan report — slim-knip-scan

**Date:** 2026-07-12  
**Tool:** [Knip](https://knip.dev) **6.26.0** (frontend dead files / exports / deps)  
**Worktree:** `D:/working/DGE-slim-knip-scan` · `feat/slim-knip-scan`  
**Command:** `pnpm -C frontend knip`  
**BDD readiness:** [`not-applicable`](../../behavior/slim-knip-scan.md) — tooling + evidence; no product actor journey  
**Wave-1:** **Done** — deleted 2 orphan unused files (zero importers); evidence refreshed post-delete

## What was integrated

| Item | Location |
| --- | --- |
| Dependency | `frontend/package.json` → `knip` ^6.26.0 |
| Config | `frontend/knip.json` (Vite / Vitest / Playwright / Vue) |
| Scripts | `pnpm -C frontend knip` · `pnpm -C frontend knip:prod` |
| Runner | `scripts/knip-scan.ps1` |
| Hygiene fix | declared missing `@eslint/js` (was imported by `eslint.config.js` but unlisted) |
| Wave-1 deletes | `TemplateRuleConfigurator.vue`, `auditEventColumnFilters.ts` |

Artifacts in this folder:

- `knip-report.txt` — full human report (post–Wave-1)
- `knip-summary.txt` — rolled-up counts
- `knip-report.json` — machine-readable (may include shell noise; prefer `.txt`)

## Scan results (post–Wave-1)

| Category | Count | Notes |
| --- | ---: | --- |
| Unused files | **0** | Wave-1 orphans removed |
| Unused dependencies | **0** | — |
| Unused devDependencies | **0** | Vite plugins correctly traced via `vite.shared.ts` |
| Unlisted dependencies | **0** | Fixed by adding `@eslint/js` |
| Unused exports | **93** | Review before delete (some may be intentional public surface) |
| Unused exported types | **66** | Mostly type-only; low runtime impact |
| Duplicate exports | **1** | `e2e/helpers/uiux-evidence.ts` viewport constants |

### Wave-1 unused files (deleted)

| File | LOC | Status |
| --- | ---: | --- |
| `src/components/templates/TemplateRuleConfigurator.vue` | 235 | **Deleted** — no importers |
| `src/views/audit/auditEventColumnFilters.ts` | 41 | **Deleted** — no importers |
| **Subtotal** | **276** | Removed this slice |

### Unused exports — concentration (top files)

| Hits | File |
| ---: | --- |
| 33 | `src/constants/roleJourneyDefinitions.ts` |
| 7 | `src/routing/routeKeys.ts` |
| 4 | `src/composables/useSessionRenewal.ts` |
| 4 | `src/utils/structuredContentDraftStorage.ts` |
| 24 types | `src/types/template.ts` |

## Calibrated slim estimate (from this scan)

| Bucket | Estimated removable | Confidence |
| --- | --- | --- |
| Unused files | **0** (Wave-1 harvested ~0.3k LOC) | High |
| Unused exports (careful) | ~0.5–2k LOC | Medium (verify tests/dynamic imports) |
| Unused exported types | ~0–0.5k (noise / DX only) | Low impact |
| **Frontend remaining harvest** | **~0.5–2.5k LOC** | — |

This is **frontend-only**. Java still relies on existing **SpotBugs / PMD / Checkstyle / ArchUnit** in `mvn verify` (no separate Knip equivalent installed this slice).

## How to re-run

```powershell
.\scripts\knip-scan.ps1
# or
pnpm -C frontend knip
pnpm -C frontend knip:prod   # production entries only (stricter)
```

Exit code `1` means findings exist (normal until cleaned). Treat as informational until a CI gate is explicitly enabled.

## Recommended next slice

1. Triage `roleJourneyDefinitions.ts` unused exports (largest cluster).
2. Optionally enable `knip` as non-blocking CI / pre-commit later.
3. Do **not** mass-delete the remaining 93 unused exports without review.
