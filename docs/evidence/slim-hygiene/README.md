# Slim hygiene — Wave 1b evidence (Knip unused exports)

Branch: `feat/slim-hygiene`  
Scope: frontend only; behavior-preserving unexport / dead-helper deletion.

## Knip counts

| Metric | Before (Wave 1b) | After (Wave 1b) |
| --- | ---: | ---: |
| Unused files | 0 | 0 |
| Unused exports | 93 | 0 |
| Unused exported types | 66 | 22 |
| Duplicate exports | 1 | 0 |

Reports:

- `knip-before-wave1b.txt` — baseline captured at start of Wave 1b
- `knip-after-wave1b.txt` — after safe unused-export / type cleanup

## What changed

- Removed barrel re-exports from `roleJourneyDefinitions.ts` (consumers already import utils directly); unexported internal helpers/types; deleted unused `roleJourneyEmptyGuidanceKey`.
- Unexported route-key internals / deleted unused `ROUTE_NAV_LABEL_KEY`, `API_POLICY_DETAIL_PATH_PREFIX`, `routeKeyForPath`.
- Unexported session/draft constants used only inside their modules.
- Deleted dead helpers with zero importers (`listRecentInvocations`, `upsertGlobalRiskPromptConfig`, deprecated `canManageTemplateLifecycle`, unused table-filter helpers, `hasWorkflowBannerAction`, dead `buildOpenRemediationTemplateIds`).
- Fixed e2e duplicate VIEWPORT exports by using distinct object literals.
- Unexported clearly local-only exported types; skipped OpenAPI / contract-adjacent types.

## Residual unused exported types (22) — intentionally kept

All remaining items are OpenAPI-adjacent aliases, generated OpenAPI re-exports, or caller-contract DTOs:

- `src/types/openapi.ts` (`components`, `operations`, `paths`, `OpenApiSchemaName`)
- `src/types/contract.ts` (caller contract interfaces)
- Schema-backed / generated-mirror types in `template.ts`, `audit.ts`, `contentModule.ts`
- `UpsertGlobalRiskPromptConfigPayload` (API payload type; endpoint helper removed but DTO retained)

Do **not** delete these without an OpenAPI / contract review.

## Gates (Wave 1b)

| Gate | Result |
| --- | --- |
| `pnpm -C frontend lint` | GREEN |
| `pnpm -C frontend type-check` | GREEN |
| `pnpm -C frontend test` | GREEN (190 files / 1154 tests) |
| `pnpm -C frontend build` | GREEN |
| `pnpm -C frontend knip` | unused exports=0; unused exported types=22; duplicate exports=0 |
