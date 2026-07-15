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

### Management master DOCX upload validation (LR-A3)

Management write paths `POST /api/management/v1/masters` and `PUT /api/management/v1/masters/{id}/file` share deep DOCX validation + size limits. These are **management-API** keys (not entries in the runtime OpenAPI v1 baseline error catalog in [contract-outline.md](contract-outline.md)):

| Condition | HTTP (service path) | `error.messageKey` |
| --- | --- | --- |
| Missing / non-`.docx` / disallowed Content-Type | 422 | `api.error.master.docxRequired` |
| File bytes above service limit (default 50MB) | 422 | `api.error.master.docxTooLarge` |
| Bad ZIP magic or missing OPC required entries | 422 | `api.error.master.docxCorrupt` |

Do **not** introduce `api.error.master.invalidDocxContent`. Multipart / nginx oversize must still surface a readable, localizable error (JSON envelope and/or UI mapping). Virus scanning is **pending**, not confirmed. Full scenarios: [LR-A3 upload validation](../behavior/lrp-a3-master-docx-upload-validation.md).

### Template test-data PII governance (CE-G03)

Management test-data-set create/update and variable-schema `piiCategory` are **management-API** contracts (documented in [contract-outline.md](contract-outline.md) «测试数据集 PII 治理（CE-G03）」). Caller-facing generate paths are unchanged. Export/import bundle variables carry optional `piiCategory` in [openapi-v1.yaml](openapi-v1.yaml) (`VariablePiiCategory` / `TemplateExportVariableSchemaView`).

Stable English-first fail-closed keys (implement in `messages_en.properties` + frontend catalog):

| Condition | HTTP | `error.messageKey` |
| --- | --- | --- |
| PII values without / illegal `piiHandling` | 422 | `api.error.template.testDataSetPiiHandlingRequired` |
| `EXPLICIT_SENSITIVE` missing reason | 422 | `api.error.template.piiConfirmReasonRequired` |
| `EXPLICIT_SENSITIVE` without secondary confirm | 422 | `api.error.template.piiSecondaryConfirmRequired` |
| Illegal `piiCategory` | 422 | `api.error.template.piiCategoryInvalid` |

Behavior SoT: [ce-g03-testdata-pii.md](../behavior/ce-g03-testdata-pii.md). Storage ruling: [data-storage-view.md](../architecture/data-storage-view.md).
