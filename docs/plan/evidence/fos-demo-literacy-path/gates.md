# Gates — fos-demo-literacy-path (#184 / FOS-W14)

**Date:** 2026-07-26  
**Branch:** `feat/fos-demo-literacy-path`

## Backend

| Gate | Result |
| --- | --- |
| BE verify | **N/A** (deploy SQL + FE e2e/docs; no Java behavior beyond demo SQL seed) |

## Frontend

| Gate | Result |
| --- | --- |
| `pnpm -C frontend lint` | **PASS** (after change) |
| `pnpm -C frontend type-check` | **PASS** |
| `pnpm -C frontend test` | **PASS** |
| `pnpm -C frontend build` | **PASS** |

## E2E / Deploy

| Gate | Result |
| --- | --- |
| `test:e2e:docker:demos` | **BLOCKED** — no acceptance stack/images on this host; suite now fail-closed when KEEP-8 missing |
| Docker deploy | **BLOCKED** (0 app images / overlay issues) |

## Implemented

- W14-1 KEEP-8 generate fail-closed
- W14-2 `deploy/*-all-demos.sh` pwsh wrappers
- W14-3 learner walkthrough linked
- W14-4 commitment first content module `blocks`→`nodes`
