# Gates — fos-policy-impact-troubleshoot (#181 / FOS-W11)

**Date:** 2026-07-26  
**Branch:** `feat/fos-policy-impact-troubleshoot`  
**Scope:** W11-1…W11-8

## Backend

| Gate | Result |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **PASS** |

## Frontend

| Gate | Result |
| --- | --- |
| `pnpm -C frontend lint` | **PASS** |
| `pnpm -C frontend type-check` | **PASS** |
| `pnpm -C frontend test` | **PASS** (1772) |
| `pnpm -C frontend build` | **PASS** |

## Deploy / E2E

| Gate | Result |
| --- | --- |
| Docker deploy queue / E2E | **BLOCKED** — daemon up, **0 images**; no greenwash |

## Implemented

- W11-1 Narrowing warnings (`OUTPUT_FORMATS_NARROWED` / modes / AD groups / batch limit)
- W11-2 Rollback preview + commit wired in policy domain editor UI
- W11-3 Failed sync mapper never returns null (INTERNAL_ERROR fallback); redundant nullcheck removed
- W11-4/5 Request-shape + blank `requestId`/`idempotencyKey` → `REQUEST_BODY_INVALID` + fieldErrors
- W11-6 Sync+batch share `OUTPUT_MODE_NOT_ALLOWED` via `RuntimeBatchValidationException`
- W11-7 ADR-0004: `ITEM_ID_DUPLICATED` documented as 422
- W11-8 Structured `currentDefaultRouteTarget` / `candidateDefaultRouteTarget`; findings expected code `TEMPLATE_VALIDATION_FAILED`
