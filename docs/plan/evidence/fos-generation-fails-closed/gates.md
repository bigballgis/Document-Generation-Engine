# Gates — fos-generation-fails-closed (#177 / FOS-W7)

**Date:** 2026-07-26  
**Branch:** `feat/fos-generation-fails-closed`  
**Scope:** W7-1…W7-6

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

- W7-1 missing pinned clause → `api.error.validation.contentModuleStructureMissing`
- W7-2 API-policy gate requires AD groups or default route; assertReady before ensureApiPolicyOnPublish
- W7-3 publish skips soft-deleted highest unreleased dev line
- W7-4 `evaluateBindings` (gate, no persist) vs `validateBindings` (authoring persist)
- W7-5 unparseable composition rules → `api.error.template.invalidRulesJson`
- W7-6 nesting neighbors use APPROVED/ACTIVE + semantic version order
