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

### Audit-reproducible regenerate (CE-G06) + production re-issue (PD-6)

Management regenerate-by-invocation is a **management-API** contract (documented in [contract-outline.md](contract-outline.md) «审计可复现受控再生（CE-G06）+ 生产重发（PD-6）」 and [openapi-v1.yaml](openapi-v1.yaml) `regenerateTemplateManagementInvocation`). Caller-facing generate paths stay watermark-free. FE regenerate CTA is out of scope. Sanitized `parameters_storage` retention for replay is authorized by [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md); management APIs still must not expose variables.

**Default (CE-G06):** omit `productionReissue` or set `false` → forced SPECIMEN; roles `GLOBAL_ADMIN` / in-scope `GROUP_ADMIN` / in-scope `AUDIT_ADMIN`.

**Production re-issue (PD-6):** `productionReissue=true` + non-blank `reason` → skip SPECIMEN; `specimen=false`; roles narrowed to `GLOBAL_ADMIN` / in-scope `GROUP_ADMIN` only (`AUDIT_ADMIN` → 403). Preview / test-generate must not accept `productionReissue`. No new ADR (behavior extension of CE-G06). Do **not** flip checklist **#3b** / **#5a**.

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
| SPECIMEN watermark failure (specimen path only) | 500 | `api.error.audit.specimenWatermarkFailed` |
| Production re-issue missing/blank reason | 400 | `api.error.audit.productionReissueReasonRequired` |
| Production re-issue forbidden role (incl. `AUDIT_ADMIN`) | 403 | `api.error.authorization.forbidden` (or management equivalent) |

Behavior SoT: [ce-g06-audit-reproducible.md](../behavior/ce-g06-audit-reproducible.md); [pd6-true-non-specimen-reissue.md](../behavior/pd6-true-non-specimen-reissue.md).

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

### Template batch-test history sampleResults (CE-U18 / PTA)

Management batch-test history is a **management-API** contract (documented in [contract-outline.md](contract-outline.md) «批量测试历史 sampleResults（CE-U18 / PTA）」 and [openapi-v1.yaml](openapi-v1.yaml)).

| Operation | Method / path |
| --- | --- |
| List recent runs (with `sampleResults`) | `GET /api/management/v1/templates/{templateId}/batch-tests?limit=` (default 5) |

`BatchTestRunSummaryView.sampleResults` is derived from persisted `sampleResultsJson`. Canonical async item shape: `dataSetExternalId`, `success`, optional `errorDetail`; when a sample produced a preview — non-null `previewId`, plus `docxKey` / `pdfKey` when those artifacts were stored (PTA / BDD-PTA-004). Failed samples may omit preview/artifact keys. Frontend must normalize legacy sync-shaped items and pre-PTA rows that dropped `previewId`. Implementer may instead expose the same field on `GET .../batch-tests/{runId}` — choose one primary surface. Management UI retires the sync `POST .../previews/batch-test` journey (async `POST .../batch-tests/run` only). Caller-facing runtime generate paths are unchanged. **No permission-matrix widen.**

Behavior SoT: [ce-u18-batch-test-history.md](../behavior/ce-u18-batch-test-history.md); PTA persist: [published-template-test-artifacts.md](../behavior/published-template-test-artifacts.md).

### Preview artifact download (existing; PTA documented)

Existing management download paths (not new endpoints). Documented so published-release Testing can reuse the same contracts without inventing a second download surface.

| Operation | Method / path |
| --- | --- |
| Download SUCCEEDED preview DOCX | `GET /api/management/v1/templates/{templateId}/previews/{previewId}/artifacts/docx` |
| Download SUCCEEDED preview PDF | `GET /api/management/v1/templates/{templateId}/previews/{previewId}/artifacts/pdf` |

Authorization: `requireReadableSnapshot` (fail-closed). **Not** lifecycle-gated on `PUBLISHED` / `STOPPED` / `DEPRECATED` — prior test artifacts remain downloadable when still available (BDD-PTA-008). Binary attachment responses (not JSON envelope). OpenAPI operationIds: `downloadTemplatePreviewDocx` / `downloadTemplatePreviewPdf`.

Behavior SoT: [published-template-test-artifacts.md](../behavior/published-template-test-artifacts.md); journey pattern: [preview-success-artifact-download-journey.md](../behavior/preview-success-artifact-download-journey.md).

### Group-scoped asset library catalog (CE-E02 + ALGI)

Management asset catalog routes in [openapi-v1.yaml](openapi-v1.yaml) / [contract-outline.md](contract-outline.md) «资产库管理契约（CE-E02 + ALGI）」. Platform-shared catalog is **withdrawn**.

- `GET /api/management/v1/library/assets` — paginated metadata (`PageView`; default `status=ACTIVE`; optional exact `groupCode`; items include `groupCode`; non-GLOBAL ∩ authorized groups)
- `POST /api/management/v1/library/assets` — multipart upload (`file` + `assetKey` + `assetClass` + **required `groupCode`**)
- `POST /api/management/v1/library/assets/{assetKey}/disable?groupCode={groupCode}` — disable identity `(groupCode, assetKey)` + remove namespaced resolvable MinIO keys

Natural uniqueness is **`(groupCode, assetKey)`**. Logical binding `assetKey` grammar unchanged (`^[A-Za-z][A-Za-z0-9._-]{0,127}$`); physical object key is **`{groupCode}/{assetKey}`**. Template `imageRef`/`sealRef` stay bare `assetKey`; resolve succeeds only for ACTIVE catalog membership in the template's group. Virus scan pending (OOS). Disable-already-DISABLED is **confirmed idempotent HTTP 200**. Upload `Idempotency-Key` is **reserved / not enforced**. Binding editor / Auto `referenceKey` out of scope.

| Condition | HTTP | `error.messageKey` |
| --- | --- | --- |
| Missing/blank `groupCode` on upload | 422 | `api.error.assetLibrary.groupCodeRequired` |
| Invalid `assetKey` | 422 | `api.error.assetLibrary.assetKeyInvalid` |
| ACTIVE `(groupCode, assetKey)` conflict | 409 | `api.error.assetLibrary.assetKeyConflict` |
| Unsupported content type | 422 | `api.error.assetLibrary.contentTypeUnsupported` |
| Magic / Content-Type mismatch | 422 | `api.error.assetLibrary.contentTypeMismatch` |
| Application payload > 5 MiB | 422 | `api.error.assetLibrary.payloadTooLarge` |
| Not found (authorized admin) | 404 | `api.error.assetLibrary.assetNotFound` |
| Unauthorized group (upload/disable) | 403 | (access denied; no existence leak) |

Behavior SoT: [asset-library-group-isolation.md](../behavior/asset-library-group-isolation.md) (`BDD-ALGI-001…018`); CE-E02 §15: [ce-e02-asset-library.md](../behavior/ce-e02-asset-library.md). Permissions: [permission-matrix.md](../security/permission-matrix.md) §13.2.

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

### `FORMAT_DATE` timezone / as-of (PQH-F8 / IBL F8 residual)

Whitelist compute DSL `FORMAT_DATE` accepts an optional second argument: **IANA ZoneId** string. Unary `FORMAT_DATE(value)` resolves a calendar day then formats with `DateTimeFormatter.ofLocalizedDate(MEDIUM)` under `context.locale` (evaluate optional `locale`; default `zh-CN`). For instant-like inputs (`Instant`, `java.util.Date`, ISO datetime with offset/`Z`), unary conversion uses documented **UTC**. Binary `FORMAT_DATE(value, zoneId)` converts those inputs via the explicit zone. Date-only `yyyy-MM-dd` / `LocalDate` (preferred for letter as-of) are zone-independent — a binary zone may be present but is unused. Locale never selects the conversion zone. Do **not** add `context.timeZone` (ADR-0013 unchanged). ISO datetime strings must not succeed via prefix truncate. Blank/invalid ZoneId, bad arity, or unparseable datetime → existing **`VARIABLE_COMPUTE_FAILED`** (422). Second argument is **not** a locale tag.

Formal schema notes: [openapi-v1.yaml](openapi-v1.yaml) `validateComputeExpression` / `evaluateComputeExpression` (+ request `expression` / `locale` descriptions). Companion: [contract-outline.md](contract-outline.md) Schema 规则 PQH-F8 bullet + error catalog note. ADR: [ADR-0056](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md) Amendment 2026-07-23 (FORMAT_DATE optional IANA zoneId). Behavior SoT: [pqh-f8-format-date-tz.md](../behavior/pqh-f8-format-date-tz.md)（BDD-PQH-F8-001…012）. Program: [post-queue-hardening-program-2026-07.md](../plan/post-queue-hardening-program-2026-07.md). Formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**; do **not** claim IBL program Done; do **not** reopen IBL Wave A.

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

### Per-legal-entity document brand variants (IBL-E4) — historical; Wave 6 retire

**Historical IBL-E4** ([ADR-0065](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md) / #131 Done): optional `context.legalEntityCode`, DocumentBrand/LegalEntity catalogs, `allowedDocumentBrandCodes`, catalog 422 codes. Evidence: [ibl-e4-entity-document-brands.md](../behavior/ibl-e4-entity-document-brands.md).

**SYS-NORM Wave 6 / D1** ([ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md) Accepted; BDD [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) **ready/Done** **D1-001…020**; TM **#150** Done `64b0a650`): management brand/entity + group `defaultLegalEntityCode` APIs **retired** (404/410 + surface-retired codes). Runtime simplify — letterhead/logo/seal from **Letterhead (master)**; `legalEntityCode` opaque non-driving; allow-list ignored at generate. Promotion/export **must not** require brand/entity sidecar (Wave 7 owns dry-run UI). Legal holds kept. UI `REDBC`/`GREENBC` orthogonal.

| Condition | HTTP | `error.code` (stable) |
| --- | --- | --- |
| Unknown `context` field | 400 | `REQUEST_BODY_INVALID` |
| DocumentBrand management surface called | 404 or 410 | `DOCUMENT_BRAND_SURFACE_RETIRED` |
| LegalEntity / defaultLegalEntity management surface called | 404 or 410 | `LEGAL_ENTITY_SURFACE_RETIRED` |
| Catalog resolve 422 family (`LEGAL_ENTITY_UNKNOWN` / `INACTIVE` / `DOCUMENT_BRAND_*`) | — | **Not produced** from retired catalogs after Wave 6 |
| Non-empty `allowedDocumentBrandCodes` write (if not stripped) | 422 (or strip) | `DOCUMENT_BRAND_SURFACE_RETIRED` (or empty strip — implement one) |

Formal schema: [openapi-v1.yaml](openapi-v1.yaml) retired ops + `ErrorCode` surface-retired values + `Context.legalEntityCode`. Companion: [contract-outline.md](contract-outline.md) Wave 6 / D1 bullet. `frontend_ui_in_scope=true` for FE hard retire. Formal phase **None**; do **not** flip **#3b/#5a GO**; do **not** claim SYS-NORM program Done.

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

Where-used row extensions: `referenceKind` (`DIRECT`\|`NESTED`), `nestingDepth`, optional `nestingPathSummary`. Formal schema: [openapi-v1.yaml](openapi-v1.yaml) `ContentModuleWhereUsedTemplateView`, `ContentModuleWhereUsedReferenceKind`, `PublishGateCheckCode`, `ErrorCode`. Companion: [contract-outline.md](contract-outline.md) IBL-E6 bullet. Behavior SoT: [ibl-e6-clause-nesting-governance.md](../behavior/ibl-e6-clause-nesting-governance.md)（BDD-IBL-E6-001…018）. impl **Done** (`dcc42c81` / `0e542c03` / `32b62136`); formal phase **None**; **not** go-live; do **not** flip **#3b/#5a GO**.

### Full-library export (CE-E03)

Management full-library export in [openapi-v1.yaml](openapi-v1.yaml) (`exportLibraryTemplates`) / [contract-outline.md](contract-outline.md) «模板导出/导入契约» CE-E03:

- `POST /api/management/v1/library/export` — optional JSON body (`groupId` / `templateIds` / `includeSkipped`; optional Wave 7 `dependencyClosure=PROMOTION`); success `200` `application/zip` (`template-library-export-v1-zip`: root manifest + nested E01 v2 per-template ZIPs + deduped `masters/` / `clauses/`; promotion may add root `assets/`)

Schemas: `LibraryExportRequest`, `LibraryExportManifestView`. **No** library-import path. **No** `Idempotency-Key` requirement. FE/E2E out of scope for E03 itself (API-first).

| Condition | HTTP | `error.messageKey` |
| --- | --- | --- |
| No exportable INCLUDED templates | 422 | `api.error.library.exportEmpty` |
| `templateIds` or eligible candidates > 500 | 422 | `api.error.library.exportLimitExceeded` |
| Caller lacks matrix §5 export-template permission | 403 | `api.error.template.accessDenied` (same boundary as single-template export) |

Permission reuse: [permission-matrix.md](../security/permission-matrix.md) §5「导出模板」— no new permission code. Behavior SoT: [ce-e03-full-library-export.md](../behavior/ce-e03-full-library-export.md).

### UAT→PROD promotion pack (SYS-NORM Wave 7)

Additive opt-in profile on CE-E01 / CE-E03 export + existing import dry-run/commit. Companion: [contract-outline.md](contract-outline.md) «SYS-NORM Wave 7». Behavior SoT: [sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md) (**BDD-SYS-NORM-PP-001…020**).

- `GET /api/management/v1/templates/{templateId}/export?bundleVersion=2&format=zip&dependencyClosure=PROMOTION` — promotion ZIP (`artifacts/master.docx` + `artifacts/assets/{assetKey}` + nesting closure / optional `clauseNestingGraph`)
- `POST /api/management/v1/library/export` with `{ "dependencyClosure": "PROMOTION", … }` — nested promotion packs + optional deduped root `assets/`
- `POST /api/management/v1/templates/import` — unchanged path; `dryRun=true|false`; auto-detects embedded assets/graph; additive `dependencyType` `CLAUSE_NESTING` / `ASSET_BINARY`
- Management UI: Templates **Import** dialog dry-run (**Check dependencies** → gated **Import** when `readyToCommit=true`)

Permission reuse: matrix §5 export/import — **no new capability codes**. Reuses [ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md) Decision 5 (no brand/entity sidecar; two-phase master P2). Do **not** flip checklist **#3b/#5a**; do **not** mark **#53** Done; Wave 8 OOS.

### Runtime rate-limit errors (PQH-F7)

Runtime filter on `/api/{environment}/v1/*` only (`frontend_ui_in_scope=false`). Companion: [contract-outline.md](contract-outline.md) PQH-F7 block; [openapi-v1.yaml](openapi-v1.yaml) `ErrorCategory=RUNTIME`, `ErrorCode` `RATE_LIMIT_*`. Behavior SoT: [pqh-f7-redis-rate-limit.md](../behavior/pqh-f7-redis-rate-limit.md) (**BDD-PQH-F7-001…012**).

| Condition | HTTP | `error.code` | `error.category` | `error.messageKey` | `retryable` |
| --- | --- | --- | --- | --- | --- |
| Identity quota exhausted | **429** | `RATE_LIMIT_EXCEEDED` | `RUNTIME` | `api.error.runtime.rateLimitExceeded` | `true` |
| Redis coordination unavailable while `distributed=true` | **503** | `RATE_LIMIT_BACKEND_UNAVAILABLE` | `RUNTIME` | `api.error.runtime.rateLimitBackendUnavailable` | `true` |

- Bucket key: `credentialId:accessAccount`; missing credential headers → pass-through (not 429/503 from rate-limit).
- `RUNTIME_RATE_LIMIT_DISTRIBUTED` / `docgen.runtime.rate-limit.distributed` defaults **`false`**.
- English defaults: implement `api.error.runtime.rateLimitBackendUnavailable` in `messages_en.properties` (+ frontend `api.error` catalog parity when applicable). Existing `api.error.runtime.rateLimitExceeded` unchanged.
- Permission matrix: **N/A** (no new capability). Formal phase **None**; do **not** flip **#3b/#5a**; docs-first — do **not** claim implementation Done from this note alone.