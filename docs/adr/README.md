# ADR Index

Architecture Decision Records capture durable product, architecture, governance, and implementation decisions.

## ADR Metadata

New ADRs should include YAML frontmatter before the title so humans, AI agents, and scripts can classify decisions without relying only on filenames.

Recommended fields for new ADRs:

| Field | Purpose |
| --- | --- |
| `id` | Stable ADR identifier in the form `ADR-0000` |
| `type` | Document type, normally `ADR` |
| `status` | `Draft`, `Proposed`, `Accepted`, `Deprecated`, or `Superseded` |
| `sourceOfTruth` | Whether the ADR currently owns an accepted decision |
| `owners` | Owning area for review and future updates |
| `adrNumber` | Four-digit ADR number matching the filename |
| `topic` | Classification metadata from the topic taxonomy |
| `related` | Documents that should be checked together with the ADR |

Existing ADRs have been backfilled with metadata frontmatter. Numbered ADRs are physically organized under `docs/adr/<topic>/` by [ADR 0027](./documentation-governance/0027-adr-topic-directory-organization.md). The ADR root keeps this index and the template.

## ADR Topic Taxonomy

Topic metadata is used for classification and ADR placement. It does not replace related-document links or source-of-truth ownership.

| Topic | Use For |
| --- | --- |
| `api` | API schema, response model, error model, route behavior, idempotency, and contract visibility |
| `api-management` | API credential, policy, template-level API configuration, and API management UI behavior |
| `architecture` | System, module, runtime, storage, messaging, and security architecture views |
| `async-processing` | Kafka, asynchronous task lifecycle, retry, replay, batch, and DLQ behavior |
| `authorization-security` | AD Group, authorization, permission isolation, sensitive data, audit, and encryption decisions |
| `documentation-governance` | Document as software, documentation architecture, validation, indexes, and AI workflow |
| `operations` | Deployment, environment, observability, retention, and operational recovery decisions |
| `rendering-authoring` | Structured authoring, rendering boundaries, DOCX/PDF fidelity, and document output rules |
| `technology-stack` | Framework, language, infrastructure, database, cache, object storage, and platform choices |
| `template-lifecycle` | Master/template lifecycle, release gates, testing, approval, import, recovery, and deprecation |

## When to Create an ADR

Create an ADR when a decision affects future design or implementation, including:

- Template lifecycle.
- Versioning model.
- Dynamic API contract.
- API authorization.
- AD Group integration.
- Environment migration.
- DOCX/PDF encryption.
- Rendering strategy.
- Permission and group isolation.

## ADR List

| ADR | Status | Topic |
| --- | --- | --- |
| [0001-output-encryption.md](./authorization-security/0001-output-encryption.md) | Accepted | DOCX/PDF dynamic output encryption |
| [0002-api-management-template-scope.md](./api-management/0002-api-management-template-scope.md) | Accepted | Template-level API management configuration scope |
| [0003-api-routing-and-batch-overrides.md](./api/0003-api-routing-and-batch-overrides.md) | Accepted (display-boundary amend 2026-07-15) | API route versioning, default route, and batch item overrides; CE-C04 callable-version optional display metadata |
| [0004-api-idempotency-strategy.md](./api/0004-api-idempotency-strategy.md) | Accepted | API idempotency strategy |
| [0005-api-response-delivery-and-download-security.md](./api/0005-api-response-delivery-and-download-security.md) | Accepted | API response delivery and download security |
| [0006-api-error-model.md](./api/0006-api-error-model.md) | Accepted (amended 2026-07-16 — includes `RENDERING`) | API error model |
| [0007-api-management-change-governance.md](./api-management/0007-api-management-change-governance.md) | Accepted | API management configuration change governance |
| [0008-api-async-task-lifecycle.md](./async-processing/0008-api-async-task-lifecycle.md) | Accepted | API async task lifecycle |
| [0009-api-credential-lifecycle.md](./api-management/0009-api-credential-lifecycle.md) | Accepted | API credential lifecycle |
| [0010-ad-group-authorization-resolution.md](./authorization-security/0010-ad-group-authorization-resolution.md) | Accepted | AD Group authorization resolution |
| [0011-api-schema-and-response-envelope.md](./api/0011-api-schema-and-response-envelope.md) | Accepted | API schema format, request field naming, and response envelope |
| [0012-api-enum-and-identifier-naming.md](./api/0012-api-enum-and-identifier-naming.md) | Accepted | API enum values and identifier naming |
| [0013-api-contract-visibility-audit-and-context.md](./api/0013-api-contract-visibility-audit-and-context.md) | Accepted (amended 2026-07-20 ADR-0063 + ADR-0065) | API contract visibility, audit summary, and context fields; Amendments — optional `jurisdiction`/`product` (E2); optional `legalEntityCode` (E4 document brand resolve) |
| [0014-api-openapi-v1-contract-scope.md](./api/0014-api-openapi-v1-contract-scope.md) | Accepted | OpenAPI v1 contract scope, discovery paths, auth headers, and trace ID handling |
| [0015-template-release-verifiability.md](./template-lifecycle/0015-template-release-verifiability.md) | Accepted | Template verifiability and release gate |
| [0016-api-management-ui-and-audit-format.md](./api-management/0016-api-management-ui-and-audit-format.md) | Accepted | API management UI structure, policy versioning, preview blocking, and audit format |
| [0017-template-lifecycle-recovery-deprecation-import.md](./template-lifecycle/0017-template-lifecycle-recovery-deprecation-import.md) | Accepted (display-boundary amend 2026-07-15) | Template lifecycle recovery, deprecation, and import conflict rules; CE-C04 display-only `deprecated`/`sunsetAt` on callable versions |
| [0018-master-review-state-and-impact-analysis.md](./template-lifecycle/0018-master-review-state-and-impact-analysis.md) | Accepted | Master review state and impact analysis |
| [0019-structured-authoring-and-rendering-boundary.md](./rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) | Accepted | Structured authoring, controlled rich text, and DOCX rendering boundary |
| [0020-unified-authorization-and-sensitive-data-handling.md](./authorization-security/0020-unified-authorization-and-sensitive-data-handling.md) | Accepted (amended 2026-07-16 by ADR-0057; further narrowed 2026-07-18 IBL-A5) | Unified authorization and sensitive data handling |
| [0021-template-testing-approval-release-governance.md](./template-lifecycle/0021-template-testing-approval-release-governance.md) | Accepted (amended 2026-07-20 ADR-0064) | Template testing, approval, and release governance; Amendment — `SINGLE_TRACK` default; optional `LEGAL_THEN_COMPLIANCE` per ADR-0064 / PD-8 |
| [0022-basic-technology-stack-baseline.md](./technology-stack/0022-basic-technology-stack-baseline.md) | Accepted | Basic technology stack baseline |
| [0023-document-as-software-operating-model.md](./documentation-governance/0023-document-as-software-operating-model.md) | Accepted | Document as software operating model |
| [0024-documentation-knowledge-architecture.md](./documentation-governance/0024-documentation-knowledge-architecture.md) | Accepted | Documentation knowledge architecture and validation |
| [0025-architecture-documentation-layer.md](./architecture/0025-architecture-documentation-layer.md) | Accepted | Architecture documentation layer |
| [0026-adr-metadata-taxonomy-and-migration-plan.md](./documentation-governance/0026-adr-metadata-taxonomy-and-migration-plan.md) | Accepted | ADR metadata, topic taxonomy, and migration plan |
| [0027-adr-topic-directory-organization.md](./documentation-governance/0027-adr-topic-directory-organization.md) | Accepted | ADR topic directory organization |
| [0028-backend-platform-stack-baseline.md](./technology-stack/0028-backend-platform-stack-baseline.md) | Accepted (amended 2026-07-13) | Backend platform stack baseline — Java **25** + Spring Boot **4.x** (target pin **4.1.0**); Task Master **#51** / [boot-4-1-upgrade](../behavior/boot-4-1-upgrade.md) |
| [0029-frontend-application-stack-baseline.md](./technology-stack/0029-frontend-application-stack-baseline.md) | Accepted (amended 2026-07-17) | Frontend application stack baseline — Vitest **3.x** (security floor **≥3.2.6**) + Vue Test Utils + Playwright; Task Master **#50** / [fe-vitest-3-upgrade](../behavior/fe-vitest-3-upgrade.md) |
| [0030-operational-platform-baseline.md](./operations/0030-operational-platform-baseline.md) | Accepted | Operational platform baseline |
| [0031-api-platform-hardening-baseline.md](./api/0031-api-platform-hardening-baseline.md) | Accepted | API platform hardening baseline |
| [0032-identity-and-security-operations-baseline.md](./authorization-security/0032-identity-and-security-operations-baseline.md) | Accepted | Identity and security operations baseline |
| [0033-async-messaging-and-task-retry-baseline.md](./async-processing/0033-async-messaging-and-task-retry-baseline.md) | Accepted | Async messaging and task retry baseline |
| [0034-data-and-storage-operations-baseline.md](./technology-stack/0034-data-and-storage-operations-baseline.md) | Accepted | Data and storage operations baseline |
| [0035-implementation-realization-and-quality-gate-baseline.md](./technology-stack/0035-implementation-realization-and-quality-gate-baseline.md) | Accepted | Implementation realization and quality gate baseline |
| [0036-local-account-store-authorization-authority.md](./authorization-security/0036-local-account-store-authorization-authority.md) | Accepted | Local account store as authorization authority, SSO authentication-only |
| [0037-backend-dependency-realization-sequencing.md](./technology-stack/0037-backend-dependency-realization-sequencing.md) | Accepted | Backend dependency realization sequencing (amends ADR 0028: defer MapStruct/QueryDSL; reaffirm + schedule Resilience4j/Bucket4j/Redisson) |
| [0038-sync-download-url-runtime-deferred.md](./api/0038-sync-download-url-runtime-deferred.md) | Accepted | Defer `SYNC_DOWNLOAD_URL` runtime delivery until secure download URL contract is ready (COR-B01) |
| [0039-redisson-lock-evaluation.md](./technology-stack/0039-redisson-lock-evaluation.md) | Accepted | Redisson distributed lock evaluation — accepted single-instance risk; mandatory before multi-instance (COR-P05) |
| [0040-api-package-access-and-invocation-retention.md](./api-management/0040-api-package-access-and-invocation-retention.md) | Accepted (amended 2026-07-18 IBL-A5 align) | Package-first API access surface, auto-materialize policy, invocation records, four-layer retention; Amendment aligns `parameters_storage` cleartext with ADR-0057 PII redaction |
| [0041-rendering-font-baseline.md](./rendering-authoring/0041-rendering-font-baseline.md) | Accepted | Rendering font baseline for DOCX→PDF conversion images — Debian jammy CJK + Carlito/Caladea (LR-A5; architecture-reviewer PASS_WITH_NOTES 2026-07-10) |
| [0042-pagination-delta-budget.md](./rendering-authoring/0042-pagination-delta-budget.md) | Accepted (Path X residual) | Pagination delta ±1 metadata-gated enforcement; Word/delta **n/a** Path X (PRR-C01 #103 / merge `3513ab92`; checklist **#3b** **CONDITIONAL** ≠ GO) |
| [0043-ooxml-output-validation-gate.md](./rendering-authoring/0043-ooxml-output-validation-gate.md) | Accepted (slice A) | OOXML OPC+XML well-formedness fail-closed; slice B XSD/LO24 residual honest (PRR-C01 #103 / BDD OOX-C4; merge `3513ab92`) |
| [0044-deployment-topology-v1.md](./operations/0044-deployment-topology-v1.md) | Accepted | v1 deployment topology — **single serving backend replica**; HPA off until scale-out prerequisites; sticky SSE + process-local rate-limit residuals honest (PRR-D01b #135); refines ADR-0039 (LR-B1, 2026-07-04) |
| [0044-multi-instance-correctness-baseline.md](./operations/0044-multi-instance-correctness-baseline.md) | Accepted (honesty residual) | SOR-S07 companion — Decision §1 prod-default distributed rate-limit is **aspirational / not current authority** (`RUNTIME_RATE_LIMIT_DISTRIBUTED:false`); sticky SSE residual; **≠ multi-instance complete** (PRR-D01b #135) |
| [0046-frontend-openapi-typescript-codegen.md](./technology-stack/0046-frontend-openapi-typescript-codegen.md) | Accepted | Frontend OpenAPI TypeScript codegen (`openapi-typescript`) for management DTO types (SOR-K03) |
| [0047-distributed-tracing-otlp-baseline.md](./operations/0047-distributed-tracing-otlp-baseline.md) | Accepted | Distributed tracing OTLP export and trace ID correlation baseline (SOR-A06) |
| [0048-audit-data-retention-policy.md](./operations/0048-audit-data-retention-policy.md) | Accepted | Audit Tier-1 retention — management 90d / runtime 365d hard delete; Tier-2 archival deferred (LR-D1, 2026-07-11) |
| [0049-distributed-trace-propagation.md](./operations/0049-distributed-trace-propagation.md) | Accepted | Cross-boundary `traceId` propagation — MDC + async `TaskDecorator` + Kafka `X-Trace-Id` headers; no Zipkin/Tempo in D4 (LR-D4, 2026-07-12) |
| [0053-task-master-ai-adoption.md](./documentation-governance/0053-task-master-ai-adoption.md) | Accepted | task-master-ai as task source for new/active work; `docs/plan/` archive + live programs (2026-07-05) |
| [0054-ad-group-resolver-production-boundary.md](./authorization-security/0054-ad-group-resolver-production-boundary.md) | Accepted | AD Group resolver production boundary — `ConfigAdGroupResolver` / `type=config` = local/dev/test only; acceptance/production requires directory adapter SPI **or** startup fail-closed; company LDAP/AD coords UNKNOWN; does not supersede ADR-0010 cache/`503` (ops-ad-group-stub-close / Task Master #46, 2026-07-12) |
| [0055-cursor-sole-parent-agent.md](./documentation-governance/0055-cursor-sole-parent-agent.md) | Accepted | Cursor sole parent agent; MCP canonical `.cursor/mcp.json`; Claude dual-stack removed; amends ADR-0053 tooling (2026-07-14) |
| [0056-whitelist-variable-compute-dsl-bounds.md](./rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md) | Accepted | Whitelist compute DSL bounds; forbid Groovy/JS/SpEL; FILTER form; locale default zh-CN; SPELL_AMOUNT CNY-only (CE-K03) |
| [0057-invocation-parameters-retention-for-regenerate.md](./authorization-security/0057-invocation-parameters-retention-for-regenerate.md) | Accepted (amended 2026-07-18 IBL-A5) | Retention-scoped `parameters_storage` exception for caller reconciliation + CE-G06 regenerate; amends ADR-0020; aligns ADR-0040; **Amendment:** cleartext only for `piiCategory=NONE`; `≠ NONE` / unknown keys must redact/exclude; encryption-at-rest deferred |
| [0058-pdfa-2b-archival-output.md](./rendering-authoring/0058-pdfa-2b-archival-output.md) | Accepted | PDF/A-2b archival via publish-locked `pdfArchivalProfile` (`NONE`\|`PDF_A_2B`); LO filter; mutex with encryption (CE-O01 / D6, 2026-07-16) |
| [0059-verapdf-pdfa-verify-gate.md](./rendering-authoring/0059-verapdf-pdfa-verify-gate.md) | Accepted | veraPDF Greenfield (`validation-model-jakarta`) PDF/A-2b machine gate in `mvn verify` (IBL-B3 / F12, 2026-07-19) — does not amend ADR-0058 product decision |
| [0060-legal-reproducibility-freeze.md](./rendering-authoring/0060-legal-reproducibility-freeze.md) | Accepted | Legal reproducibility freeze — LO version record, ADR-0041 font set reaffirm, SHA-256 content-hash baseline procedure (IBL-B6 / F16, 2026-07-19); no Word/pixel baselines; does not flip #3b/#5a |
| [0062-locale-variant-template-clause-model.md](./template-lifecycle/0062-locale-variant-template-clause-model.md) | Accepted | Locale-variant template/clause model (IBL-E1 / #128 / F24 / PD-4, 2026-07-19; **renumbered from draft ADR-0061** same day — collision with documentation-governance ADR-0061 audience-manuals). BDD [ibl-e1-locale-variant-model.md](../behavior/ibl-e1-locale-variant-model.md) **ready** (BDD-IBL-E1-001…018 / E1-C*); does not flip #3b/#5a or PD-6 |
| [0063-jurisdiction-product-channel-composition-rules.md](./template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md) | Accepted | Jurisdiction/product/channel Composition Inclusion Rules (IBL-E2 / #129 / F25 / PD-5, 2026-07-20). BDD [ibl-e2-jurisdiction-rule-engine.md](../behavior/ibl-e2-jurisdiction-rule-engine.md) **ready** (BDD-IBL-E2-001…016 / E2-C*); amends ADR-0013 context whitelist; API-first (`frontend_ui_in_scope=false`); impl **Done** (`81a1ca29` / `6a96e9ab`); does not flip #3b/#5a |
| [0064-legal-compliance-approval-matrix.md](./template-lifecycle/0064-legal-compliance-approval-matrix.md) | Accepted | Legal→compliance multi-stage approval matrix + forced `LEGAL_REVIEWER` (IBL-E3 / #130 / F26 / PD-8, 2026-07-20). BDD [ibl-e3-legal-approval-matrix.md](../behavior/ibl-e3-legal-approval-matrix.md) **ready** (BDD-IBL-E3-001…018 / E3-C*); amends ADR-0021 (`SINGLE_TRACK` default); `frontend_ui_in_scope=true`; impl **Done** (`233342d3` / `e81a6bac`); does not flip #3b/#5a |
| [0065-legal-entity-document-brand-variants.md](./template-lifecycle/0065-legal-entity-document-brand-variants.md) | Accepted | Per-legal-entity document brand variants (IBL-E4 / #131 / F27 document-brand half / PD-9, 2026-07-20). BDD [ibl-e4-entity-document-brands.md](../behavior/ibl-e4-entity-document-brands.md) **ready** (BDD-IBL-E4-001…017 / E4-C*); amends ADR-0013 context whitelist (`legalEntityCode`); DocumentBrand ≠ UI BrandPreset; `frontend_ui_in_scope=true`; impl **Done** (`4d810395` / `212c6be9`); does not flip #3b/#5a |
| [0066-effectivefrom-publish-and-bulk-repin.md](./template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md) | Accepted | Future `effectiveFrom` publish hard-block + bulk re-pin dry-run/audit (IBL-E5 / #132 / F27 residual, 2026-07-20). BDD [ibl-e5-effectivefrom-bulk-repin.md](../behavior/ibl-e5-effectivefrom-bulk-repin.md) **ready** (BDD-IBL-E5-001…017 / E5-C*); amends CE-K08 K08-C6/LM-011; OpenAPI `CONTENT_MODULE_EFFECTIVE_NOT_STARTED` + `POST …/content-module-references/bulk-repin`; `frontend_ui_in_scope=false`; impl **Done** (`688f9e58` / `20ead1ce`); does not flip #3b/#5a |
| [0067-clause-nesting-module-graph-governance.md](./template-lifecycle/0067-clause-nesting-module-graph-governance.md) | Accepted | Clause nesting module-graph governance (IBL-E6 / #133 / F28, 2026-07-20). BDD [ibl-e6-clause-nesting-governance.md](../behavior/ibl-e6-clause-nesting-governance.md) **ready** (BDD-IBL-E6-001…018 / E6-C1…C21); CM↔CM graph; max depth **8**; cycle fail-closed; deep where-used; transitive pins; OpenAPI codes locked; `frontend_ui_in_scope=false`; impl **Done** (`dcc42c81` / `0e542c03`); does not flip #3b/#5a |
| [0068-rtl-bidi-out-of-scope-until-market.md](./rendering-authoring/0068-rtl-bidi-out-of-scope-until-market.md) | Accepted | RTL / bidirectional scripts **descope** until market confirmation (IBL-E7 / #134 / F15, 2026-07-20). Evidence [ibl-e7-rtl-bidi-spike/SPIKE-REPORT.md](../plan/evidence/ibl-e7-rtl-bidi-spike/SPIKE-REPORT.md) **DESCOPE**; F15 closed by descope; BDD not-applicable; Accepted ≠ #134 Done; does not flip #3b/#5a; PD-6/7 OUT; no Word invent; no product RTL impl |
| [0001-management-api-service-layer-authorization.md](./authorization/0001-management-api-service-layer-authorization.md) | Accepted | Management API service-layer authorization — `ManagementRoute` UI-only; `GroupAccessService` for API (COR-P06) |

### Proposed (not yet Accepted)

| ADR | Status | Topic |
| --- | --- | --- |
| *(none)* | — | — |

### LR-A5 triad (0041/0042/0043 Accepted — 0042 Path X residual; 0043 slice B residual)

| ADR | On-disk status | Note |
| --- | --- | --- |
| [0041-rendering-font-baseline.md](./rendering-authoring/0041-rendering-font-baseline.md) | **Accepted** | architecture-reviewer **PASS_WITH_NOTES** 2026-07-10 (slice `lrp-a5-adr-closeout`); also listed in Accepted table above. |
| [0042-pagination-delta-budget.md](./rendering-authoring/0042-pagination-delta-budget.md) | **Accepted** | PRR-C01 / Task Master **#103** (merge `3513ab92`). Metadata-gated enforcement landed; Path **X** Word n/a residual ([word-baseline-exemption.md](../evidence/prod-adr-0042-0043-closeout/word-baseline-exemption.md)). Checklist **#3b** → **CONDITIONAL** only (Path X ≠ GO). Do not invent Word numbers. |
| [0043-ooxml-output-validation-gate.md](./rendering-authoring/0043-ooxml-output-validation-gate.md) | **Accepted** | PRR-C01 / **#103** — Accepted for Decision **slice A** only (OPC+XML well-formedness fail-closed); slice B (ECMA-376 XSD + LO24) **residual honest** (BDD OOX-C4). Do not read Accepted as LO24-safe / full XSD. |

Use [0000-template.md](./0000-template.md) when creating new ADRs. Place new numbered ADRs in the directory matching their `topic` frontmatter.
