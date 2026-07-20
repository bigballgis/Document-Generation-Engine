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

### PII-category retention redaction (IBL-A5)

Retention write path (same `parameters_storage` column) **must** redact/exclude clear values when version `VariableSchema.piiCategory ≠ NONE` or the key is unknown; `NONE` may remain clear (passwords still stripped). Caller-facing invocation detail returns the post-redaction parameters; regenerate replays non-redacted fields and does not fail solely due to PII redaction. No new OpenAPI error codes for this leaf. Encryption-at-rest remains deferred. Do **not** flip checklist **#3b** / **#5a**.

Authority: [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md) Amendment 2026-07-18. Behavior SoT: [ibl-a5-pii-retention-redaction.md](../behavior/ibl-a5-pii-retention-redaction.md). Contract notes: [contract-outline.md](contract-outline.md) «参数存储与详情» / CE-G06 regenerate «参数留存».

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

### Template annual review + clause full-text search (CE-G05)

Management contracts for template `nextReviewDue`, annual-review due tasks, content-module `searchMode=FULL_TEXT`, and where-used (documented in [contract-outline.md](contract-outline.md) «模板年检与条款正文全文检索（CE-G05）」 and [openapi-v1.yaml](openapi-v1.yaml)). **No new capability bit.** Annual review requires `authorTemplates`; FTS/where-used reuse §5.1 catalog browse. Caller-facing runtime generate paths unchanged. Behavior SoT: [ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md).

| Operation | Method / path |
| --- | --- |
| Due tasks | `GET /api/management/v1/author-workflow/annual-review-due-tasks` |
| Complete review | `POST /api/management/v1/templates/{templateId}/annual-review/complete` |
| Clause FTS | `GET /api/management/v1/content-modules?search=&searchMode=FULL_TEXT` |
| Where-used | `GET /api/management/v1/content-modules/{moduleId}/where-used` |

`TemplateSummaryView` / `TemplateDetailView` expose optional nullable `nextReviewDue` (`format: date`). Audit on successful complete: `TEMPLATE_ANNUAL_REVIEW_COMPLETED` (no variables / credentials / clause body).

### Template batch-test history sampleResults (CE-U18)

Management batch-test history is a **management-API** contract (documented in [contract-outline.md](contract-outline.md) «批量测试历史 sampleResults（CE-U18）」 and [openapi-v1.yaml](openapi-v1.yaml)).

| Operation | Method / path |
| --- | --- |
| List recent runs (with `sampleResults`) | `GET /api/management/v1/templates/{templateId}/batch-tests?limit=` (default 5) |

`BatchTestRunSummaryView.sampleResults` is derived from persisted `sampleResultsJson`. Canonical async item shape: `dataSetExternalId`, `success`, optional `errorDetail` / `docxKey` / `pdfKey`. Frontend must normalize legacy sync-shaped historical items. Implementer may instead expose the same field on `GET .../batch-tests/{runId}` — choose one primary surface. Management UI retires the sync `POST .../previews/batch-test` journey (async `POST .../batch-tests/run` only). Caller-facing runtime generate paths are unchanged.

Behavior SoT: [ce-u18-batch-test-history.md](../behavior/ce-u18-batch-test-history.md).

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

### Runtime fail-closed variable validation (IBL-A1)

Caller-facing runtime generate / batch-generate and management preview assemble apply fail-closed `VariableSchema` validation (required / type / enum; unknown keys rejected) **before** compute/assemble. Formal schema: [openapi-v1.yaml](openapi-v1.yaml) `ErrorCode` includes `VARIABLE_VALIDATION_FAILED`. Companion: [contract-outline.md](contract-outline.md) error catalog + IBL-A1 note.

| Condition | HTTP | `error.code` | `error.category` | `error.messageKey` | `retryable` |
| --- | --- | --- | --- | --- | --- |
| Missing required / wrong type / wrong enum / unknown field (one or many) | 422 | `VARIABLE_VALIDATION_FAILED` | `VALIDATION` | `api.error.validation.variableValidationFailed` | `false` |

Envelope reuses existing `error.fieldErrors[]` (`field` / `reason` / `message`); `reason` ∈ `REQUIRED` \| `INVALID_TYPE` \| `INVALID_FORMAT` \| `ENUM_NOT_ALLOWED` \| `UNKNOWN_FIELD`. Legacy single-field codes `VARIABLE_REQUIRED` / `VARIABLE_TYPE_INVALID` / `VARIABLE_FORMAT_INVALID` / `VARIABLE_RULE_FAILED` remain in the enum for catalog compatibility; **runtime/preview schema validation uses the aggregate code**. Publish does **not** validate generate body. CE-U03 test-data-set top-level code migration is out of scope. Implement in `ApiErrorCodes` + `messages_en.properties` (backend). Behavior SoT: [ibl-a1-variable-validation.md](../behavior/ibl-a1-variable-validation.md)（BDD-IBL-A1-001…008）. Formal phase **None**; **not** go-live.

### ISO-currency `FORMAT_AMOUNT` (IBL-A2)

Whitelist compute DSL `FORMAT_AMOUNT` accepts an optional second argument: ISO 4217 alphabetic currency code. Unary `FORMAT_AMOUNT(value)` keeps locale-default currency (CE-K03 compatible). Binary `FORMAT_AMOUNT(value, currencyCode)` sets currency identity from the ISO code while number/grouping localization still follows `context.locale` (evaluate API optional `locale`). Missing, blank, or invalid currency → existing **`VARIABLE_COMPUTE_FAILED`** (422; no silent locale-default fallback). Second argument is **not** a locale tag (e.g. do not pass `en-US`).

Formal schema notes: [openapi-v1.yaml](openapi-v1.yaml) `validateComputeExpression` / `evaluateComputeExpression` (+ request `expression` / `locale` descriptions). Companion: [contract-outline.md](contract-outline.md) Schema 规则 IBL-A2 bullet + error catalog note. Behavior SoT: [ibl-a2-format-amount-currency.md](../behavior/ibl-a2-format-amount-currency.md)（BDD-IBL-A2-001…010）. Formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**.

### International amount-in-words `SPELL_AMOUNT` (IBL-A3)

Whitelist compute DSL `SPELL_AMOUNT` accepts an optional second argument: ISO 4217 alphabetic currency code. Unary `SPELL_AMOUNT(value)` is **always** CNY Chinese uppercase (locale-independent; CE-K03 / golden compatible). Binary `SPELL_AMOUNT(value, currencyCode)` sets currency identity from the ISO code while spelling **language** follows the primary language of `context.locale` (evaluate API optional `locale`). This leaf requires at least `(en, USD)` and `(zh, CNY)`. Unsupported (language, currency) pair, missing/blank/invalid currency, or arity ∉ {1,2} → existing **`VARIABLE_COMPUTE_FAILED`** (422; no silent wrong-language fallback). Second argument is **not** a locale tag. Default locale `zh-CN` means binary USD needs an explicit `en` / `en-US` locale.

Formal schema notes: [openapi-v1.yaml](openapi-v1.yaml) `validateComputeExpression` / `evaluateComputeExpression` (+ request `expression` / `locale` descriptions). Companion: [contract-outline.md](contract-outline.md) Schema 规则 IBL-A3 bullet + error catalog note. ADR: [ADR-0056](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md) Amendment (SPELL_AMOUNT ISO + locale language). Behavior SoT: [ibl-a3-amount-in-words.md](../behavior/ibl-a3-amount-in-words.md)（BDD-IBL-A3-001…012）. Formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**.

### `/contract` per-field variable schemas + consumer breaking gate (IBL-A4)

Runtime `GET /api/{environment}/v1/templates/{templateId}/contract` and management `GET /api/management/v1/templates/{templateId}/api/contract` (same assembly) expose **per-field** VariableSchema under each `callableVersions[]` item as `variables[]` (`ContractVariableSchemaView`: `variableKey`, `variableType`, `required`, `computed`, `piiCategory`, `enumValues` when ENUM, optional `description`). Sorted by `variableKey` ascending. Never returns internal `id`, `defaultValue` plaintext, or `computeExpression` plaintext.

Top-level `result.schemas: string[]` remains an **envelope type-name index** (at least `GenerateRequest` / `BatchGenerateRequest` / `OutputOptions` / `EncryptionOptions`) — **not** field schemas. Do **not** clear or replace it.

**Consumer contract breaking-change gate (CI):** repository consumer contract tests (compat classifier + golden/fixture) run under `mvn -B -ntp -f backend/pom.xml verify`. **BREAKING → FAIL:** key remove/rename, `variableType` change, `required` false→true, ENUM shrink, enterable→computed. **NON_BREAKING → PASS:** additive optional field, ENUM widen, description-only, `required` true→false, `piiCategory` change. This is **not** a publish-API hard block.

Formal schema: [openapi-v1.yaml](openapi-v1.yaml) `getTemplateApiContract` / `CallableVersion.variables` / `ContractVariableSchemaView`. Companion: [contract-outline.md](contract-outline.md) IBL-A4 bullets + «消费者契约 breaking-change 闸门（IBL-A4）». Example: [examples/contract-response.json](examples/contract-response.json). Behavior SoT: [ibl-a4-contract-field-schemas.md](../behavior/ibl-a4-contract-field-schemas.md)（BDD-IBL-A4-001…011）. `frontend_ui_in_scope=false`. Formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**.

### Jurisdiction / product / channel composition inclusion (IBL-E2)

Runtime `context` whitelist adds optional `jurisdiction` / `product`; existing `channel` also matches Composition Inclusion Rules ([ADR-0063](../adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md)). Management draft API: `GET|PUT /api/management/v1/templates/{templateId}/composition-inclusion-rules` (orthogonal to visibility `/rules`).

| Condition | HTTP | `error.code` (stable) |
| --- | --- | --- |
| Unknown `context` field | 400 | `REQUEST_BODY_INVALID` |
| Illegal inclusion rule set on PUT | 422 | `COMPOSITION_INCLUSION_RULE_INVALID` |
| Required inclusion unsatisfied at generate | 422 | `COMPOSITION_INCLUSION_UNSATISFIED` |
| INCLUDE + CE-K08 jurisdiction both set and unequal | 422 | `CONTENT_MODULE_JURISDICTION_MISMATCH` |
| Publish dangling inclusion `referenceKey` | publish gate | `COMPOSITION_INCLUSION_REFERENCE_INVALID` |

Formal schema: [openapi-v1.yaml](openapi-v1.yaml) `Context`, `CompositionInclusionRuleView`, `PublishGateCheckCode`, `ErrorCode`. Companion: [contract-outline.md](contract-outline.md) IBL-E2 bullet + context whitelist table. Behavior SoT: [ibl-e2-jurisdiction-rule-engine.md](../behavior/ibl-e2-jurisdiction-rule-engine.md)（BDD-IBL-E2-001…016）. `frontend_ui_in_scope=false`. Accepted ADR ≠ impl Done; formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**.

### Per-legal-entity document brand variants (IBL-E4)

Runtime `context` whitelist adds optional `legalEntityCode` ([ADR-0065](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md) / ADR-0013 Amendment). Management catalog APIs: DocumentBrand + LegalEntity + group `defaultLegalEntityCode`. Template package optional `allowedDocumentBrandCodes`. Resolve applies to **document artifact** brand slots only — orthogonal to UI `REDBC`/`GREENBC` chrome.

| Condition | HTTP | `error.code` (stable) |
| --- | --- | --- |
| Unknown `context` field | 400 | `REQUEST_BODY_INVALID` |
| Unknown legal entity | 422 | `LEGAL_ENTITY_UNKNOWN` |
| Inactive legal entity | 422 | `LEGAL_ENTITY_INACTIVE` |
| Bound document brand inactive/missing | 422 | `DOCUMENT_BRAND_INACTIVE` |
| Resolved brand ∉ template allow-list | 422 | `DOCUMENT_BRAND_NOT_ALLOWED` |

Formal schema: [openapi-v1.yaml](openapi-v1.yaml) `Context`, `DocumentBrandView`, `LegalEntityView`, `ErrorCode`, template `allowedDocumentBrandCodes`, metadata `resolvedLegalEntityCode` / `resolvedDocumentBrandCode`. Companion: [contract-outline.md](contract-outline.md) IBL-E4 bullet + context whitelist table. Behavior SoT: [ibl-e4-entity-document-brands.md](../behavior/ibl-e4-entity-document-brands.md)（BDD-IBL-E4-001…017）. `frontend_ui_in_scope=true`. Accepted ADR ≠ impl Done; formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**.

### Future `effectiveFrom` publish gate + bulk re-pin (IBL-E5)

Publish hard gate for not-yet-effective CM pins + group-scoped bulk re-pin tooling ([ADR-0066](../adr/template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md)). Amends CE-K08 “future effectiveFrom does not block”. Management API-first (`frontend_ui_in_scope=false`).

| Condition | Surface | Stable code |
| --- | --- | --- |
| Pinned CM `effectiveFrom` in the future | publish gate | `CONTENT_MODULE_EFFECTIVE_NOT_STARTED` |
| Pinned CM `effectiveTo` expired | publish gate | `CONTENT_MODULE_EFFECTIVE_EXPIRED` (unchanged) |
| Missing `dryRun` / invalid target XOR | 400/422 | validation / `BULK_REPIN_TARGET_INVALID` (per-item on apply preview) |
| Unauthorized caller | 403/404 | existing `authorTemplates` convention |

Formal schema: [openapi-v1.yaml](openapi-v1.yaml) `PublishGateCheckCode`, `BulkRepinContentModuleReferencesRequest`, `BulkRepinContentModuleReferencesResultView`, `ErrorCode`. Path: `POST /api/management/v1/content-module-references/bulk-repin`. Companion: [contract-outline.md](contract-outline.md) IBL-E5 bullet. Behavior SoT: [ibl-e5-effectivefrom-bulk-repin.md](../behavior/ibl-e5-effectivefrom-bulk-repin.md)（BDD-IBL-E5-001…017）. Accepted ADR ≠ E5 impl Done; formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**.

### Clause nesting module-graph governance (IBL-E6)

Governed CM↔CM nesting from `contentModuleRef`, depth/cycle fail-closed, deep where-used, and transitive pin publish gates ([ADR-0067](../adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md)). Extends CE-G05 where-used. Management API-first (`frontend_ui_in_scope=false`). **No new capability bit.**

| Condition | Surface | Stable code |
| --- | --- | --- |
| Self / mutual / indirect nest cycle on CM structure write | 422 | `CONTENT_MODULE_NESTING_CYCLE` |
| Nesting depth > 8 on CM structure write | 422 | `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED` |
| Nest `referenceKey` unresolved / invisible | 422 | `CONTENT_MODULE_NESTING_TARGET_UNRESOLVED` |
| Malformed `contentStructureJson` on nesting write | 422 | `CONTENT_MODULE_NESTING_STRUCTURE_INVALID` |
| Cycle in pinned nest closure | publish gate | `CONTENT_MODULE_NESTING_CYCLE` |
| Depth > 8 in pinned nest closure | publish gate | `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED` |
| Nested `referenceKey` missing as template pin | publish gate | `CONTENT_MODULE_NESTING_UNPINNED` |
| Render expand encounters cycle | structured failure | `CONTENT_MODULE_NESTING_CYCLE` |

Where-used row extensions: `referenceKind` (`DIRECT`\|`NESTED`), `nestingDepth`, optional `nestingPathSummary`. Formal schema: [openapi-v1.yaml](openapi-v1.yaml) `ContentModuleWhereUsedTemplateView`, `ContentModuleWhereUsedReferenceKind`, `PublishGateCheckCode`, `ErrorCode`. Companion: [contract-outline.md](contract-outline.md) IBL-E6 bullet. Behavior SoT: [ibl-e6-clause-nesting-governance.md](../behavior/ibl-e6-clause-nesting-governance.md)（BDD-IBL-E6-001…018）. Accepted ADR ≠ E6 impl Done; formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**.

### Full-library export (CE-E03)

Management full-library export in [openapi-v1.yaml](openapi-v1.yaml) (`exportLibraryTemplates`) / [contract-outline.md](contract-outline.md) «模板导出/导入契约» CE-E03:

- `POST /api/management/v1/library/export` — optional JSON body (`groupId` / `templateIds` / `includeSkipped`); success `200` `application/zip` (`template-library-export-v1-zip`: root manifest + nested E01 v2 per-template ZIPs + deduped `masters/` / `clauses/`)

Schemas: `LibraryExportRequest`, `LibraryExportManifestView`. **No** library-import path. **No** `Idempotency-Key` requirement. FE/E2E out of scope (API-first).

| Condition | HTTP | `error.messageKey` |
| --- | --- | --- |
| No exportable INCLUDED templates | 422 | `api.error.library.exportEmpty` |
| `templateIds` or eligible candidates > 500 | 422 | `api.error.library.exportLimitExceeded` |
| Caller lacks matrix §5 export-template permission | 403 | `api.error.template.accessDenied` (same boundary as single-template export) |

Permission reuse: [permission-matrix.md](../security/permission-matrix.md) §5「导出模板」— no new permission code. Behavior SoT: [ce-e03-full-library-export.md](../behavior/ce-e03-full-library-export.md).