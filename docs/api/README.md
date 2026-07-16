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

### Audit-reproducible regenerate (CE-G06)

Management regenerate-by-invocation is a **management-API** contract (documented in [contract-outline.md](contract-outline.md) «审计可复现受控再生（CE-G06）」 and [openapi-v1.yaml](openapi-v1.yaml) `regenerateTemplateManagementInvocation`). Caller-facing generate paths stay watermark-free. FE regenerate CTA is out of scope. Sanitized `parameters_storage` retention for replay is authorized by [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md); management APIs still must not expose variables.

Stable English-first fail-closed keys (implement in `messages_en.properties`):

| Condition | HTTP | `error.messageKey` |
| --- | --- | --- |
| Missing fingerprint / pre-G06 row | 409 | `api.error.audit.releaseBundleSnapshotUnavailable` |
| Bundle hash drift | 409 | `api.error.audit.releaseBundleHashMismatch` |
| Pinned master unavailable | (K01) | `api.error.rendering.pinnedMasterUnavailable` |
| Unsupported invocation kind | 422 | `api.error.audit.invocationKindNotRegenerable` |
| Expired invocation record | 410 | `api.error.audit.invocationRecordExpired` |
| SPECIMEN watermark failure | 500 | `api.error.audit.specimenWatermarkFailed` |

Behavior SoT: [ce-g06-audit-reproducible.md](../behavior/ce-g06-audit-reproducible.md).

### Legal hold administration (CE-G04)

Management legal-hold routes are a **management-API** contract (documented in [contract-outline.md](contract-outline.md) «Legal hold 管理契约（CE-G04）」 and [openapi-v1.yaml](openapi-v1.yaml)). Caller-facing runtime generate paths are unchanged. ACTIVE holds overlay retention exemption on ADR-0040 / ADR-0048 hard-delete schedulers — **do not** edit those ADR decision bodies. Authorization is **GLOBAL_ADMIN only** (no new capability bit). Behavior SoT: [ce-g04-legal-hold.md](../behavior/ce-g04-legal-hold.md).

| Operation | Method / path |
| --- | --- |
| List | `GET /api/management/v1/legal-holds` |
| Get | `GET /api/management/v1/legal-holds/{id}` |
| Create | `POST /api/management/v1/legal-holds` → `201` |
| Release | `POST /api/management/v1/legal-holds/{id}/release` → `200` |

No physical DELETE. Stable fail-closed keys:

| Condition | HTTP | `error.code` | `error.messageKey` |
| --- | --- | --- | --- |
| Non-GLOBAL_ADMIN | 403 | `ACCESS_DENIED` | `api.error.authorization.accessDenied` |
| Hold not found | 404 | `LEGAL_HOLD_NOT_FOUND` | `api.error.notFound.legalHoldNotFound` |
| Already RELEASED | 409 | `LEGAL_HOLD_ALREADY_RELEASED` | `api.error.conflict.legalHoldAlreadyReleased` |
| Mixed / invalid scope payload | 422 | `REQUEST_BODY_INVALID` / validation | `api.error.validation.*` |
| Template missing (TEMPLATE_WINDOW) | 404 | `TEMPLATE_NOT_FOUND`（既有） | 既有模板 not-found 键 |

### Platform asset library catalog (CE-E02)

Management asset catalog routes in [openapi-v1.yaml](openapi-v1.yaml) / [contract-outline.md](contract-outline.md) «资产库管理契约（CE-E02）」:

- `GET /api/management/v1/library/assets` — paginated metadata (`PageView`; default `status=ACTIVE`)
- `POST /api/management/v1/library/assets` — multipart upload (`file` + `assetKey` + `assetClass`)
- `POST /api/management/v1/library/assets/{assetKey}/disable` — disable + remove resolvable MinIO keys

`assetKey` ≡ MinIO-resolvable object key (`^[A-Za-z][A-Za-z0-9._-]{0,127}$`). **Does not** change `StructuredContentImageResolver` protocol. Virus scan pending (OOS). Disable-already-DISABLED is **confirmed idempotent HTTP 200** (catalog remains `DISABLED`; resolvable objects re-checked for deletion). Upload `Idempotency-Key` is **reserved / not enforced** in CE-E02.

| Condition | HTTP | `error.messageKey` |
| --- | --- | --- |
| Invalid `assetKey` | 422 | `api.error.assetLibrary.assetKeyInvalid` |
| ACTIVE key conflict | 409 | `api.error.assetLibrary.assetKeyConflict` |
| Unsupported content type | 422 | `api.error.assetLibrary.contentTypeUnsupported` |
| Magic / Content-Type mismatch | 422 | `api.error.assetLibrary.contentTypeMismatch` |
| Application payload > 5 MiB | 422 | `api.error.assetLibrary.payloadTooLarge` |
| Not found (authorized admin) | 404 | `api.error.assetLibrary.assetNotFound` |

Behavior SoT: [ce-e02-asset-library.md](../behavior/ce-e02-asset-library.md). Permissions: [permission-matrix.md](../security/permission-matrix.md) §13.2.