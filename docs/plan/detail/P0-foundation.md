# P0 — Foundation & Guardrails (Detailed Plan)

**Phase status:** Done (2026-06-23)  
**Master plan:** [master-plan.md](../master-plan.md)

## Behavior goal

Developers can clone the repo, start local dependencies, run backend and frontend
quality gates, and validate the OpenAPI v1 contract — with no product features yet.

## Design tasks

| ID | Task | Owner module | Status |
| --- | --- | --- | --- |
| P0-D01 | Repository layout: `backend/`, `frontend/`, `docker-compose.yml`, `.env.example` | Platform | Done |
| P0-D02 | Backend Maven module skeleton (`com.bank.docgen.*` module-first packages) | Shared kernel | Done |
| P0-D03 | Unified API error envelope + metadata mapping from OpenAPI | API adapters | Done |
| P0-D04 | Flyway baseline migration (empty schema + audit/time/UUID conventions) | Infrastructure | Done |
| P0-D05 | Frontend Vite + Vue3 + Element Plus + Pinia + Router + i18n (`en` base) | Frontend shell | Done |
| P0-D06 | Dual-brand theme tokens (REDBC / GREENBC) + brand logo slot | Frontend shell | Done |
| P0-D07 | OpenAPI contract validation test harness | API adapters | Done |
| P0-D08 | CI script stubs: backend verify, frontend lint/type-check/test/build | Platform | Done |

## Implementation tasks

| ID | Task | Acceptance | Status |
| --- | --- | --- | --- |
| P0-T01 | docker-compose for PostgreSQL, Redis, Kafka, MinIO, LibreOffice sidecar | Services healthy; documented in README | Done |
| P0-T02 | Spring Boot app boots with `/healthz` and `/readyz` | Health endpoints return 200 when deps up | Done |
| P0-T03 | Frontend dev server boots with empty login route shell | `pnpm build` passes | Done |
| P0-T04 | Contract test fails on schema drift vs `docs/api/openapi-v1.yaml` | Test exists and runs in CI | Done |
| P0-T05 | Argon2id password hasher bean + JWT config skeleton (no login yet) | Unit test for hash/verify round-trip | Done |

## Exit checklist

- [x] Backend `mvn verify` green on skeleton
- [x] Frontend lint, type-check, test, build green
- [x] docker-compose documented (Docker install noted if missing locally)
- [x] Plan status updated to Done in master-plan.md
