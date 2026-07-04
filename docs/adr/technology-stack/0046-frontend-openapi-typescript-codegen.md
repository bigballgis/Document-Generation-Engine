---
id: ADR-0046
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - frontend
  - api
adrNumber: "0046"
topic: technology-stack
related:
  - docs/api/openapi-v1.yaml
  - docs/adr/api/0014-api-openapi-v1-contract-scope.md
  - docs/adr/technology-stack/0029-frontend-application-stack-baseline.md
  - docs/plan/system-optimization-review-2026-07.md
---

# ADR 0046: Frontend OpenAPI TypeScript Codegen for Management DTOs

## Status

Accepted

## Context

The management UI maintained hand-written TypeScript DTO modules under `frontend/src/types/`
(especially `template.ts`, ~672 lines) that drifted from the authoritative contract in
`docs/api/openapi-v1.yaml`. SOR-K03 required a durable codegen approach without replacing
the existing Axios API modules or changing runtime UI/API behavior.

Open question Q6 in [system-optimization-review-2026-07.md](../../plan/system-optimization-review-2026-07.md)
asked which codegen tool to adopt and how generated artifacts are owned in the repository.

## Decision

Adopt **`openapi-typescript`** (types-only generator) as the frontend OpenAPI codegen tool.

| Area | Decision |
| --- | --- |
| Tool | `openapi-typescript` devDependency |
| Input | `docs/api/openapi-v1.yaml` |
| Output | `frontend/src/types/generated/openapi-v1.ts` (committed; **do not hand-edit**) |
| Regeneration | `pnpm -C frontend codegen:openapi` before release/CI when the contract changes |
| Consumption | Domain modules under `frontend/src/types/*.ts` re-export or wrap generated schemas via `Schema<T>` aliases in `frontend/src/types/openapi.ts`; stable export names preserved for existing imports |
| API modules | Existing hand-written Axios modules in `frontend/src/api/*.ts` remain unchanged |
| Drift gate | Vitest parity test `frontend/src/types/generated/openapiCodegenParity.test.ts` fails if committed output is stale relative to the YAML |

Types without a matching OpenAPI schema (management auth session, identity admin, some
management-only list DTOs) remain hand-written with an explicit comment until the contract
grows.

## Consequences

- OpenAPI schema changes require running `codegen:openapi` and committing the regenerated file.
- Type modules anchor to the contract where schemas exist, reducing silent drift.
- Management-only shapes that differ from or extend OpenAPI use thin wrapper types (Omit/extend)
  rather than duplicating full hand-written interfaces.
- Future alternatives (e.g. full client generators) are out of scope unless reopened by ADR.

## Alternatives Considered

- **`openapi-generator` / Orval (client + types):** Rejected for SOR-K03 — would replace or
  duplicate Axios modules and increase behavior risk.
- **Hand-written types only:** Rejected — drift already observed; no automated gate.
- **Import OpenAPI YAML at build time without committing output:** Rejected — CI and offline
  builds must not depend on non-deterministic regen; committed artifact + parity test is simpler.

## References

- SOR-K03 — [system-optimization-review-2026-07.md](../../plan/system-optimization-review-2026-07.md) §7
- ADR 0014 — OpenAPI v1 contract scope
- ADR 0029 — Frontend application stack baseline
