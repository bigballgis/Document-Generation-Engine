# Knip / folder hygiene — prod-audit-p2-hygiene (#137)

**Date:** 2026-07-18  
**Slice:** `prod-audit-p2-hygiene` · Task Master **#137**  
**BDD readiness:** [`not-applicable`](../../behavior/prod-audit-p2-hygiene.md)

## FE deliverables

| Item | Result |
| --- | --- |
| Folder merge | `frontend/src/components/template/*` → `components/templates/` (no name collisions) |
| Import rewrite | `@/components/template/...` → `@/components/templates/...` |
| Knip cleanup | High-confidence unused **exports** only (not mass type purge) |

## Knip high-confidence removals (this leaf)

### Deleted (zero real callers)

- `getLegalHold` (`src/api/legalHolds.ts`) + test mocks
- `canAccessLegalHoldAdministration` (`roleCapabilitiesLegalHold.ts` + `roles.ts` barrel)
- `nodeDepthAtPath`, `siblingCountAtPath` (`structuredContentNodePath.ts`)

### Un-exported (same-file / barrel-only; keep implementation)

- Barrel: `canAccessAssetLibraryManagement` from `roles.ts`
- Re-exports: `STRUCTURED_BLOCK_NODE_TYPES`, `BINDING_CONTENT_TYPES`, `filterPaletteRouteItems`
- Internals: `detectShortcutPlatform`, `useTemplatePreviewRunHistoryPanel`, `COLLABORATION_QUEUES`, `canSeeBehaviorRemediation`, contract-copy helpers, legacy draft helpers, `parentPathOf`, `VARIABLE_PII_CATEGORIES`, `ADVANCED_JSON_SIZE_THRESHOLD`, `VARIABLE_KEY_PATTERN`, `expressionReferencesVariable`, `WORKFLOW_CHIP_QUERY`, dashboard test-support helpers (`BASE_CAPABILITIES`, `journeyTimelineStub`, `mountDashboardWorkflowTab`, `journeyStub`)
- Types: `LibraryAssetClass` / `Status` / `ListStatusFilter` inlined as string unions (removed type-only const arrays)

### Explicitly left for later triage

- Unused **exported types** (~50)
- Auth capability functions still used by their own unit tests
- Knip configuration hints

## Knip counts

| Scan | Unused exports | Unused exported types |
| --- | ---: | ---: |
| Pre-cleanup (`knip-report.txt`) | **31** | **51** |
| Post-cleanup (`knip-after-cleanup.txt`) | **0** | **51** (deferred) |

**FE commit:** `09cf85ce` — `refactor(frontend): merge template components folder and trim knip exports (#137)`  
**BE commit:** `49b4d9e1` — `fix(template): remove unused listAll catalog helpers`  
**MAIN merge:** `baaf16cc` — Task Master **#137** **Done** (stage 12 doc-sync; sole-active cleared)
