# Gates — fos-concurrency-integrity (#178 / FOS-W8)

**Date:** 2026-07-26  
**Branch:** `feat/fos-concurrency-integrity`  
**Scope:** W8-1…W8-5

## Backend

| Gate | Result |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **PASS** |

## Frontend

| Gate | Result |
| --- | --- |
| `pnpm -C frontend lint` | **PASS** |
| `pnpm -C frontend type-check` | **PASS** |
| `pnpm -C frontend test` | **PASS** (1770) |
| `pnpm -C frontend build` | **PASS** |

## Deploy / E2E

| Gate | Result |
| --- | --- |
| Docker deploy queue / E2E | **BLOCKED** — daemon up, **0 images**; no greenwash |

## Implemented

- W8-1 `@Version` / `row_version` on `TemplateVersionEntity` + optimistic-lock → 409
- W8-2 unique active PUBLISHED release index + publish `FOR UPDATE` lock
- W8-3 `DataIntegrityViolationException` → 409 `DATA_INTEGRITY_CONFLICT`
- W8-4 actionable `invalidState` / `publishGateBlocked` message args
- W8-5 shared `assertDraft` via `TemplateService` / `TemplateAccessGuardSupport`
