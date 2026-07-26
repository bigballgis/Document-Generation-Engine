# Gates — fos-lifecycle-cannot-corrupt (#176 / FOS-W6)

**Date:** 2026-07-26  
**Branch:** `feat/fos-lifecycle-cannot-corrupt`  
**Scope:** W6-1…W6-7 only (W6-8 non-goal — no PENDING_RELEASE→DRAFT)

## Backend

| Gate | Result |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **PASS** (after rebase onto `ce47186a`) |

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

- W6-1 restore skips individually deactivated STOPPED versions
- W6-2 refuse delete of current master revision; storage delete afterCommit
- W6-3 explicit `versionId` / `semanticVersion` on module lifecycle + review
- W6-4 Flyway V78 numeric semver columns + query order (`1.10` > `1.9`)
- W6-5 supersede keeps `release_version`
- W6-6 bulk lifecycle updates `AND deletedAt IS NULL`
- W6-7 master upload snapshot DRAFT; review uses enum incl. REJECTED + line snapshot sync
