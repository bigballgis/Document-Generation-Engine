# API Documentation

This folder is reserved for API v1 contract documentation and maintenance notes.

## Contents

- [contract-outline.md](contract-outline.md): dynamic API v1 contract guide, confirmed constraints, and an explicit open-issue register.
- [openapi-v1.yaml](openapi-v1.yaml): formal OpenAPI 3.1 contract for dynamic API v1.
- [examples/](examples/): example requests and responses for the v1 contract.
- Dynamic template API contract.
- API management and content-module governance contract surface.
- Request and response schemas.
- OpenAPI 3.1 YAML as the formal API contract schema format for v1.
- Error code definitions.
- API examples.
- Batch generation behavior.
- Output mode behavior.
- API authorization behavior.
- API idempotency behavior.
- API response delivery and download security behavior.
- API error model behavior.
- DOCX/PDF dynamic encryption request behavior.

## Contract Status

API behavior is defined by formal API contract artifacts and source-of-truth docs:

- [OpenAPI v1](openapi-v1.yaml)
- ADRs under [docs/adr/](../adr/README.md)

- [Requirements Plan](../requirements/requirements-plan.md)
- [PRD](../product/PRD.md)
- [Domain Model](../domain/domain-model.md)
- [Permission Matrix](../security/permission-matrix.md)
- [P22 Demo expansion behavior spec](../requirements/demo-expansion-behavior-spec.md) — rendering fidelity; **no caller-facing OpenAPI change**

OpenAPI v1 is the formal API schema baseline. The contract outline is the formal companion explainer with rationale, confirmed constraints, cross-links, and explicit open notes; only entries explicitly marked as open notes are non-final.

Content-module governance routes currently documented in this folder include:

- `POST /api/{environment}/v1/admin/content-modules/{moduleId}/review/transition`
- `POST /api/{environment}/v1/admin/content-modules/{moduleId}/lifecycle/operation/apply`

Master revision-line read routes (management UI — P2-T05 Phase A / P2-T06 Phase B):

- `GET /api/management/v1/masters/{masterId}/revision-lines` — paginated list (Phase A may return current line only; Phase B full history)
- `GET /api/management/v1/masters/{masterId}/revision-lines/{revisionLineId}` — revision line detail (any line belonging to master in Phase B)
- `GET /api/management/v1/masters/{masterId}/revision-lines/{revisionLineId}/download` — historical or current DOCX bytes

Traceability: BDD-MASTER-REVISION-NAV-001 — [catalog-navigation-ux.md](../product/catalog-navigation-ux.md).
