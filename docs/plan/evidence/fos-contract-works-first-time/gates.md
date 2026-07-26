# Gates — fos-contract-works-first-time (#179 / FOS-W9)

**Date:** 2026-07-26  
**Branch:** `feat/fos-contract-works-first-time`  
**Scope:** W9-1…W9-6

## Backend

| Gate | Result |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **PASS** (2435 tests, 11 skipped) |

## Frontend

| Gate | Result |
| --- | --- |
| `pnpm -C frontend lint` | **PASS** |
| `pnpm -C frontend type-check` | **PASS** |
| `pnpm -C frontend test` | **PASS** |
| `pnpm -C frontend build` | **PASS** |

## Deploy / E2E

| Gate | Result |
| --- | --- |
| Docker deploy queue / E2E | **BLOCKED** — daemon up, **0 images**; no greenwash |

## Implemented

- W9-1 Copyable curl uses `X-Api-Credential-*` / `X-Access-Account` (no Bearer; no Idempotency-Key header)
- W9-2 Example mode sync-safe (`SYNC_STREAM` only); else async batch curl
- W9-3 Error catalogue retryable flags + rate-limit/timeout codes; `REQUEST_BODY_INVALID` → VALIDATION
- W9-4 FE per-version `variables` schema table
- W9-5 Empty `examples` tokens; hint when no test-data payload
- W9-6 `CredentialSummary.credentialId` wire name + FE `expiresAt`
