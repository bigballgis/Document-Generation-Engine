# Gates — fos-default-verify-honesty (#183 / FOS-W13)

**Date:** 2026-07-26  
**Branch:** `feat/fos-default-verify-honesty`  
**Scope:** W13-1…W13-8

## Honesty rule

Do not claim Testcontainers GREEN without a runnable Docker engine. This host pulls images but fails container start (`overlay` mount invalid argument).

## Backend

| Gate | Result |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **PASS** |
| `mvn -Ptestcontainers,dev-fast test` (local) | **BLOCKED** — images pulled; container start fails (containerd overlay mount) |
| Constitution Gates `backend-testcontainers` job | **pending after push** — cite run id; expected GREEN on GitHub-hosted runners |

## Frontend

| Gate | Result |
| --- | --- |
| `pnpm -C frontend lint/type-check/test/build` | **PASS** (1775 tests; hygiene) |

## Deploy / E2E lab

| Gate | Result |
| --- | --- |
| Docker deploy / E2E | **BLOCKED** — no app images / stack; no greenwash |

## Implemented

- W13-1 Flyway migrate + core table presence; CI job `backend-testcontainers`
- W13-2 `harnessSelfTest` on SYNTHETIC packages + README honesty
- W13-3 `productPdf: pending-CRCH-W5` on QR/attachment/cross-page
- W13-4 `condition-inside-loop` + `empty-loop-collection`; empty list emits nothing
- W13-5/6/7 MinIO + Redis idempotency/rate-limit + Postgres FTS TC tests (authored; local run BLOCKED)
- W13-8 JaCoCo PACKAGE floors authoring≥0.40 / apimgmt≥0.50 / template≥0.55 + ratchet plan
