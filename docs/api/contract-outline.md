# 动态 API v1 契约说明

This document is the formal companion to dynamic API v1 OpenAPI. The formal API schema baseline is OpenAPI 3.1 YAML; this companion captures confirmed constraints, decision context, cross-document links, and a centralized open-issue register.

Field names, capability breakdown, error-code names, and response structure are maintained in parallel with formal OpenAPI v1; only items explicitly marked as open issues are non-final.

## 相关文档

- [文档索引](../README.md)
- [原始需求记录](../requirements/requirements-plan.md)
- [产品需求说明](../product/PRD.md)
- [领域模型](../domain/domain-model.md)
- [权限矩阵](../security/permission-matrix.md)
- [输出加密 ADR](../adr/authorization-security/0001-output-encryption.md)
- [PDF/A-2b 归档输出 ADR](../adr/rendering-authoring/0058-pdfa-2b-archival-output.md)（CE-O01）
- [API 管理配置范围 ADR](../adr/api-management/0002-api-management-template-scope.md)
- [API 路由与批量覆盖 ADR](../adr/api/0003-api-routing-and-batch-overrides.md)
- [API 幂等策略 ADR](../adr/api/0004-api-idempotency-strategy.md)
- [API 响应交付与下载安全 ADR](../adr/api/0005-api-response-delivery-and-download-security.md)
- [API 错误模型 ADR](../adr/api/0006-api-error-model.md)
- [API 管理配置变更治理 ADR](../adr/api-management/0007-api-management-change-governance.md)
- [API 异步任务生命周期 ADR](../adr/async-processing/0008-api-async-task-lifecycle.md)
- [API 凭证生命周期 ADR](../adr/api-management/0009-api-credential-lifecycle.md)
- [AD Group 授权解析与缓存 ADR](../adr/authorization-security/0010-ad-group-authorization-resolution.md)
- [API Schema 与响应 Envelope ADR](../adr/api/0011-api-schema-and-response-envelope.md)
- [API 枚举与标识命名 ADR](../adr/api/0012-api-enum-and-identifier-naming.md)
- [API 契约可见性、审计摘要与 context ADR](../adr/api/0013-api-contract-visibility-audit-and-context.md)
- [API OpenAPI v1 契约范围 ADR](../adr/api/0014-api-openapi-v1-contract-scope.md)
- [API 管理配置界面与审计格式 ADR](../adr/api-management/0016-api-management-ui-and-audit-format.md)
- [统一授权与敏感数据处理 ADR](../adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md)
- [正式 OpenAPI v1](openapi-v1.yaml)
- [API 示例](examples/README.md)
- [综合演示包扩展行为规格](../requirements/demo-expansion-behavior-spec.md)（P22 — 渲染保真；**无调用方 API 变更**）

## 已确认 API 能力

- API 根据模板动态生成。
- API 分环境，并通过环境变量读取当前环境。
- API 路径统一采用 `/api/{environment}/v1` 前缀；平台运行时仍通过环境变量读取当前部署环境，并校验路径中的 `{environment}` 与当前部署环境一致。
- API 路由需要支持显式发布版本路径；调用方通过路径选择目标模板和发布版本。
- API 需要支持 default 路径；default 路径由 API 管理配置显式路由到某个未停用发布版本。
- 显式发布版本单笔生成路径为 `/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/generate`。
- default 单笔生成路径为 `/api/{environment}/v1/templates/{templateId}/default/generate`。
- 显式发布版本批量生成路径为 `/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/batch-generate`。
- default 批量生成路径为 `/api/{environment}/v1/templates/{templateId}/default/batch-generate`。
- 异步任务查询路径为 `/api/{environment}/v1/templates/{templateId}/tasks/{taskId}`。
- 异步任务取消路径为 `POST /api/{environment}/v1/templates/{templateId}/tasks/{taskId}/cancel`。
- 下载地址取文件路径为 `/api/{environment}/v1/documents/{documentId}/download`；下载时仍需要通过 `documentId` 关联模板并执行模板级二次授权。
- API 契约查看路径为 `GET /api/{environment}/v1/templates/{templateId}/contract`。
- 管理面同装配契约路径为 `GET /api/management/v1/templates/{templateId}/api/contract`（query `environment`）；与 runtime `/contract` 共用装配，均含 `callableVersions[].variables`（IBL-A4）。
- 可调用版本列表路径为 `GET /api/{environment}/v1/templates/{templateId}/versions`；该接口返回当前授权视角下可调用的发布版本列表，不作为后台版本管理列表；字段级 Schema 以 `/contract` 为权威（列表可不附带完整 `variables[]`）。
- **调用记录**（2026-07-03）：`GET /api/{environment}/v1/templates/{templateId}/invocations`（`view=logical|flat`，可选 `requestId`）；`GET …/invocations/{invocationId}`。
- API v1 请求头字段确认为 `X-Api-Credential-Id`、`X-Api-Credential-Secret`、`X-Access-Account`；可选追踪请求头为 `X-Trace-Id`。
- `X-Trace-Id` 传入时平台沿用该值作为响应和审计中的 `traceId`；未传入时由平台生成 `traceId`。
- `releaseVersion` 路径参数采用语义化版本号，例如 `1.0.0`、`1.1.0`、`2.0.0`。
- default 路径不得隐式指向最新版本，必须由全局管理员或分组管理员在 API 管理中显式配置目标发布版本。
- API 调用权限是模板级别。
- API 采用 API 凭证 + AD Group 双重认证授权。
- AD Group 解析规则适用于所有需要 AD Group 授权的 API 操作，包括生成、批量生成、异步任务查询、异步任务取消、下载取文件、API 契约查看、可调用版本列表和 **调用记录查询**。
- AD Group 成功解析结果按 `accessAccount` + `environment` 缓存 5 分钟；不缓存解析失败结果。
- AD Group 解析失败时，如果存在未过期缓存，则使用未过期缓存继续授权；如果不存在未过期缓存，则返回 `503 AD_GROUP_RESOLUTION_FAILED`，`retryable=true`。
- AD Group 授权不得使用过期缓存兜底；过期缓存不能作为授权依据。
- API 管理中的 AD Group 授权配置变更立即生效，并清理相关授权缓存；目录中的 AD Group 成员变更在目录同步完成且平台缓存过期后生效。
- API 凭证对象是调用方级身份，可授权到多个模板 API；模板调用仍必须同时满足 API 凭证授权、AD Group 授权和模板级授权。
- API 凭证创建和轮换时，secret 明文只展示一次；平台只保存不可逆摘要或指纹，不允许管理员后续重新查看 secret 明文。
- API 凭证必须设置有效期；默认有效期为 180 天，最长 365 天，管理员可设置更短有效期；到期时间以持久化 `expiresAt` 为准（CE-C04）。
- API 凭证状态集合确认为 `ACTIVE`、`EXPIRING_SOON`、`EXPIRED`、`REVOKED`。
- API 凭证轮换时，新 secret 立即可用，旧 secret 保留 7 天宽限期；宽限期结束后旧 secret 失效；轮换不重置到期时间。
- API 凭证吊销立即生效，阻断该凭证的所有后续 API 操作，包括新生成、异步任务查询、异步任务取消和下载取文件。
- API 管理由全局管理员和分组管理员承担，不设置独立 API 管理员角色。
- API 管理配置当前按模板级绑定；一个模板对应一组 API 管理配置，适用于该模板下所有未停用的发布版本。
- 发布版本继续锁定模板内容、变量、规则和发布版本契约；API 管理配置作为调用侧策略独立维护，不要求重新发布模板。
- 可调用版本列表从模板下发布版本生成；模板停用或废弃时所有发布版本不可调用，单个发布版本停用时仅该版本不可调用；模板或发布版本恢复后，恢复对象重新进入可调用候选范围，但仍受模板状态、发布版本状态和模板级 API 管理配置约束。
- 可调用版本列表项（`CallableVersion`）可包含可选展示字段 `deprecated`（boolean）与 `sunsetAt`（date-time）；对当前可调用的发布版本通常为 `deprecated=false`（或省略）且省略 `sunsetAt`。这些字段仅用于契约/发现展示，不得借此把已停用或已永久废弃版本纳入可调用集（ADR-0003 / ADR-0017 展示边界；CE-C04）。
- **IBL-A4：** `/contract`（runtime + management）在每个 `callableVersions[]` 元素上返回 `variables[]` 逐字段 Schema 投影（至少 `variableKey` / `variableType` / `required` / `computed` / `piiCategory`；`ENUM` 时非空 `enumValues` string[]；可选 `description`）。按 `variableKey` 字典序升序。不返回内部 `id`、`defaultValue` 明文或 `computeExpression` 原文。顶层 `result.schemas: string[]` **保留**为信封 OpenAPI 类型名索引（至少含 `GenerateRequest` / `BatchGenerateRequest` / `OutputOptions` / `EncryptionOptions`），不得清空或伪装为字段 Schema。行为 SoT：[ibl-a4-contract-field-schemas.md](../behavior/ibl-a4-contract-field-schemas.md)（BDD-IBL-A4-001…011）。**不**翻转 checklist #3b/#5a；**不**宣称 go-live。
- 模板发布后需要生成接口地址、请求参数 Schema、响应 Schema、字段校验规则、示例请求/响应、错误码说明、契约版本对比和可调用版本列表。
- API 支持 DOCX/PDF 输出、同步文件流、同步下载地址、异步任务返回任务 ID、批量生成。
- 同步文件流响应中，文件内容放在响应体，核心元数据通过响应头承载。
- 同步文件流响应头核心元数据包括 `auditId`、`traceId`、`requestId`、`idempotencyStatus`、`documentId`、`templateId`、`routeType`、`resolvedReleaseVersion`、`output.format`、`output.mode`。
- 同步下载地址和异步结果下载地址采用短有效期策略；下载时需要二次授权，校验 API 凭证、AD Group 和模板级授权；有效期内允许多次下载。
- 异步任务查询需要返回任务状态、响应元数据、成功结果或统一错误明细；异步批量任务需要返回批次汇总和单笔成功/失败明细。
- 异步任务 v1 支持受控取消；仅未完成且未过期的任务可取消，取消成功后状态为 `CANCELLED`，且不返回已生成结果、下载地址或异步批量单笔成功结果。
- 异步任务查询不返回进度百分比；异步批量任务通过 `batch.summary` 返回进度摘要。
- 异步任务状态集合确认为 `ACCEPTED`、`PROCESSING`、`SUCCEEDED`、`FAILED`、`PARTIAL_SUCCEEDED`、`EXPIRED`、`CANCELLED`；`PARTIAL_SUCCEEDED` 仅用于异步批量任务。
- 异步任务和生成结果默认保留 7 天（**幂等/异步任务默认窗口**；包级 document/invocation retention 见 BDD-API-PACKAGE-ACCESS-INVOCATION-001）。
- 批量请求支持批次级统一输出和加密配置，也允许单笔记录单独覆盖输出格式、输出模式和加密参数。
- 批量请求中每笔记录的单独覆盖都必须受模板级 API 管理配置约束，不能绕过输出方式、批量上限或动态加密能力限制。
- 批量请求中每笔记录必须传入 `items[].itemId`，且同一批次内必须唯一；重复 `items[].itemId` 返回 `400 ITEM_ID_DUPLICATED`，不创建批次或异步任务。
- 同步批量中任一记录因参数校验或 API 管理策略失败时，整批失败且不生成任何文件；响应需要返回每笔失败明细，并按非重试幂等结果记录。
- 异步批量部分成功后的失败项重试必须使用新批次和新的 `idempotencyKey`，通过 `originalBatchId` 或等效字段关联原批次，原批次结果不被扩展或改写；`originalBatchId` 出现时须通过同凭证 `BATCH_ROOT` 校验，否则 `404 ORIGINAL_BATCH_NOT_FOUND`（CE-C05）。
- API 支持 DOCX/PDF 动态加密参数，是否允许加密以及可用加密能力由 API 管理配置控制。
- `encryption.enabled=true` 时，`openPassword` 必填，`ownerPassword` 可选；`permissions` 采用统一抽象权限枚举，**v1 仅对 PDF 映射并生效**（CE-C06）；DOCX + 非空 `permissions` 结构合法时成功并警告 `DOCX_PERMISSIONS_NOT_APPLIED`（`messageKey=generation.warning.fidelity.docxPermissionsNotApplied`）；传入 `permissions` 时必须同时传入 `ownerPassword`。
- `encryption.enabled=false` 或未传 `enabled` 时，如果仍传入 `openPassword`、`ownerPassword` 或 `permissions`，返回 `400 ENCRYPTION_PARAMETER_INVALID`，不得静默忽略。
- **CE-O01：** 发布锁定 `pdfArchivalProfile=PDF_A_2B` 与 PDF 请求 `encryption.enabled=true` 互斥 → `400 PDF_ARCHIVAL_ENCRYPTION_MUTEX`（`api.error.generation.pdfArchivalEncryptionMutex`）。行为 SoT：[ce-o01-pdfa-output.md](../behavior/ce-o01-pdfa-output.md)；ADR：[0058-pdfa-2b-archival-output.md](../adr/rendering-authoring/0058-pdfa-2b-archival-output.md)。
- `openPassword` 和 `ownerPassword` 的密码强度基线为最少 12 字符、最长 128 字符；如果两者同时传入，二者必须不同。
- 加密参数合法但实际加密处理失败时，返回 `500 ENCRYPTION_FAILED`，`retryable=true`；错误响应、日志和审计不得返回密码、内部加密细节或敏感配置值。
- 动态 API v1 请求字段命名基线采用 `output.format`、`output.mode`、`variables`、`encryption`、`requestId`、`idempotencyKey`、`items[].itemId` 和 `context`。
- 模板标识和发布版本号只通过路径表达，生成请求体不得重复传入 `templateId` 或 `releaseVersion`；重复传入按请求体字段错误处理。
- 正式 API 契约 Schema 采用 OpenAPI 3.1 YAML 维护；Markdown 文档负责解释、索引、决策背景和示例说明。
- v1 请求采用严格字段校验，契约 Schema 之外的未知字段返回 `400 REQUEST_BODY_INVALID`。
- JSON 响应采用统一 envelope：`metadata` 承载审计、追踪、请求、幂等、模板、路由和输出摘要；`result` 承载成功或已受理结果；`error` 承载整请求或整批失败。同步文件流响应体只承载文件内容，核心元数据通过响应头表达。
- DOCX/PDF 保真警告采用成功结果字段表达；单笔 JSON 成功响应通过 `result.fidelityWarnings[]` 返回，批量成功项通过 `result.batch.items[].fidelityWarnings[]` 返回，异步任务查询在生成完成后按相同结果层级返回。同步文件流响应体只承载文件内容，通过响应头返回保真警告数量和警告码摘要，完整警告明细进入审计摘要。
- 批量 JSON 响应必须按请求顺序返回全量单笔明细，每个输入对应一条明细，回显 `itemId`，并包含单笔状态、最终输出配置、加密策略摘要以及成功结果或错误信息。
- v1 API 枚举值统一采用英文 `UPPER_SNAKE_CASE`。
- 输出格式枚举确认为 `DOCX`、`PDF`；输出模式枚举确认为 `SYNC_STREAM`、`SYNC_DOWNLOAD_URL`、`ASYNC_TASK`；路由类型枚举确认为 `EXPLICIT_VERSION`、`DEFAULT_ROUTE`。
- 异步任务状态枚举确认为 `ACCEPTED`、`PROCESSING`、`SUCCEEDED`、`FAILED`、`PARTIAL_SUCCEEDED`、`EXPIRED`、`CANCELLED`；批量单笔状态枚举确认为 `SUCCEEDED`、`FAILED`、`SKIPPED`。
- `permissions` 的 v1 抽象权限枚举确认为 `ALLOW_PRINT`、`ALLOW_COPY`、`ALLOW_EDIT`、`ALLOW_ANNOTATE`、`ALLOW_FORM_FILL`。
- `templateId` 采用可读稳定模板键：`TPL-` 前缀 + 英文、数字和连字符；发布后不可修改，不得包含客户、个人、账号、金额或其他敏感业务信息。
- `taskId`、`batchId`、`documentId` 采用资源前缀 + 不透明随机 token：`TASK-`、`BATCH-`、`DOC-`；token 不得承载日期、序号、模板、客户或业务变量含义。
- `context` 采用安全白名单，v1 仅允许 `sourceSystem`、`channel`、`businessRequestId`、`upstreamTraceId`、`scenario`、`locale`、`jurisdiction`、`product`、`legalEntityCode`；字段值均为字符串；未知 `context` 字段返回 `400 REQUEST_BODY_INVALID`。`jurisdiction` / `product` / `channel` 可作为组合纳入控制输入（IBL-E2 / ADR-0063）；`legalEntityCode` 为可选不透明字段（ADR-0013；历史 IBL-E4）——**ADR-0071 / Wave 6 后不驱动** DocumentBrand 目录解析、不选包、不改 UI chrome；`sourceSystem` / `businessRequestId` / `upstreamTraceId` / `scenario` 不进入 inclusion 匹配。
- API 管理配置展示字段 v1 基线确认为 `apiPolicy.policyVersion`、`apiPolicy.updatedAt`、`apiPolicy.updatedBy`、`apiPolicy.allowedOutputFormats`、`apiPolicy.allowedOutputModes`、`apiPolicy.batchLimits.syncMaxItems`、`apiPolicy.batchLimits.asyncMaxItems`、`apiPolicy.encryptionCapabilities`、`apiPolicy.adGroupAuthorizationSummary`、`apiPolicy.credentialSummary`。
- 异步任务受理响应返回 `task.queryPath`，值为任务查询相对路径，不是免认证或签名地址；后续查询仍需 API 凭证、AD Group 和模板级授权。
- v1 采用统一授权判定基线；文档生成、批量生成、异步任务查询、异步任务取消、下载取文件、API 契约查看、可调用版本列表、**调用记录查询**和 API 管理均在业务处理或敏感响应返回前完成对应授权判定。
- 授权拒绝或授权依赖失败只返回已确认的安全错误码和通用安全消息，不泄露未授权资源是否存在、未授权组详情、完整成员列表、API secret、加密密码或内部配置细节。
- v1 建立敏感数据分级处理基线；日志、审计、管理界面、API 契约展示、契约示例、错误响应、导出文件和支持排查材料必须执行脱敏或摘要化规则。
- v1 不提供发布版本级 API 管理配置覆盖机制，模板级 API 管理配置仍是唯一基线。
- API 调用需要审计记录。

## 契约设计原则确认

以下为已确认原则。

- 调用方通过路径表达目标模板和发布版本；default 路径只作为 API 管理显式配置的兼容路由，不等同于自动最新版本。
- API 契约需要区分模板内容契约和 API 管理策略：发布版本锁定模板内容、变量、规则和契约，API 管理配置控制调用侧策略。
- 请求结构需要让调用方清楚区分业务变量、输出要求、批量控制和加密参数。
- 响应结构需要让调用方清楚判断同步成功、异步受理、批量部分成功、业务校验失败和系统生成失败。
- 错误模型需要稳定、可审计、可定位，并能被上游系统转化为业务可理解提示；v1 面向信贷客户经理的正式生成失败或警告展示由上游业务系统承接。
- API 不应在响应、日志或审计中返回或记录 API 传入的 DOCX/PDF 加密密码。

## 统一授权与敏感数据处理确认

确认基线：v1 采用统一授权判定基线。API 相关入口包括文档生成 API、批量生成、异步任务查询、异步任务取消、下载取文件、API 契约查看、可调用版本列表和 API 管理。

授权判定在执行受保护操作或返回敏感响应前完成。授权依赖不可用且没有已确认可用缓存时按 fail-closed 处理。

API 入口按入口类型组合校验 API 凭证、访问账号、AD Group、模板级授权、对象归属、环境、资源状态和 API 管理配置。任务查询、任务取消和下载取文件必须通过 `taskId` 或 `documentId` 解析到关联模板，并执行模板级二次授权。

API 契约查看和可调用版本列表必须按当前授权视角返回结果，不得展示未授权模板、未授权调用方、完整 AD Group 成员或未授权组详情。

授权拒绝或授权依赖失败只返回已确认的安全错误码和通用安全消息，不泄露未授权资源是否存在、未授权组详情、完整成员列表、API secret、加密密码或内部配置细节。

授权判定和授权拒绝需要记录安全审计摘要，包含主体摘要、入口、环境、对象范围摘要、判定结果、拒绝原因码或依赖失败原因；不得记录敏感明文。

敏感数据分级处理基线：

- 禁止明文持久化/展示。
- 允许摘要或指纹。
- 授权响应例外。

禁止明文持久化或展示的内容包括 API 凭证 secret、DOCX/PDF 加密密码、模板变量原值、模板测试数据敏感值、完整请求体、完整下载地址、完整 AD Group 成员、未授权组详情、历史密文、敏感配置明文和未授权生成文档内容；保真警告不得包含模板变量原值、粘贴原文、客户数据、完整请求体或生成文档敏感内容。

**CE-G03 澄清（2026-07-15）：** 「模板测试数据敏感值」禁明文面 = 日志、审计摘要、契约示例、导出、发布证据与未授权展示。授权维护者经 `SYNTHETIC` / `EXPLICIT_SENSITIVE` 闸门后写入 Template Test Data Set 存储的变量值是测试资产本体（见 [ce-g03-testdata-pii.md](../behavior/ce-g03-testdata-pii.md) G03-C14/C22；[data-storage-view.md](../architecture/data-storage-view.md)）。不修订 ADR-0020 正文。运行时调用方 generate API **不**新增入参 PII 扫描。

**CE-G06 / ADR-0057（2026-07-16；IBL-A5 Amendment 2026-07-18）：** `api_invocation_record.parameters_storage` 允许在调用记录留存窗口内持久化已消毒且按 PII 分类收窄后的模板变量：明文仅 `piiCategory = NONE`；`≠ NONE` 与未知 key 禁止明文；加密密码仍禁。供调用方 reconciliation 与受控再生内部重放（再生重放非脱敏字段）。管理端/审计/日志/导出仍禁明文；列级 encryption-at-rest 暂缓。权威：[ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md)；行为：[ibl-a5-pii-retention-redaction.md](../behavior/ibl-a5-pii-retention-redaction.md)。

允许以摘要或指纹表达的内容包括 API 凭证标识或指纹摘要、`idempotencyKey` 摘要、请求语义 hash、`variablesHash`、`itemsHash`、加密策略摘要、AD Group 授权摘要、下载地址脱敏值、`contextSummary`、`fidelityWarnings` 非敏感摘要、`policyVersion`、`changedAreas` 和配置差异摘要。

授权响应例外仅限已确认安全场景：API 凭证创建或轮换时 secret 明文只展示一次；授权 API 响应可返回可用 `download.url`；同步文件流和下载取文件可在授权通过后返回生成文档内容；`task.queryPath` 只是相对查询路径，不授予额外访问能力。

脱敏规则适用于日志、审计、管理界面、API 契约展示、契约示例、错误响应、导出文件和支持排查材料。未知或未分类字段默认按敏感处理，只能在明确确认安全后降级为摘要或可展示字段。

## 命名原则确认

以下命名原则为 v1 确认基线。

- 字段名建议使用 `lowerCamelCase`。
- 枚举值和错误码建议使用 `UPPER_SNAKE_CASE`。
- 业务对象字段建议使用清晰名词，例如 `templateId`、`releaseVersion`、`documentId`。
- 可排查字段统一放入 `metadata`，例如 `auditId`、`traceId`、`routeType`。
- 调用方业务请求标识和幂等标识建议分开：`requestId` 用于调用方业务追踪，`idempotencyKey` 用于重复提交识别。
- 时间字段建议使用 `At` 后缀表达时间点，例如 `expiresAt`。
- 布尔字段建议使用清晰状态语义，例如 `retryable`、`oneTime`。
- API 生成的资源标识使用资源前缀 + 不透明 token，避免在 ID 中暴露时间、序号、模板、客户或业务变量含义。

确认基线：v1 正式 API 采用以上命名风格；企业内部已有 API 命名规范如果与该基线冲突，需要作为兼容策略单独确认。

## 契约能力确认

| 契约能力 | 主要用途 | 当前状态 |
| --- | --- | --- |
| 查看 API 契约信息 | 让管理员、模板编排人员和被授权 API 调用方查看模板 API 契约、请求/响应结构、错误码和示例。 | 路径、查看权限、default 路径展示字段、契约响应范围已确认；IBL-A4 起含 `callableVersions[].variables` 逐字段 Schema。 |
| 查看可调用版本列表 | 返回授权模板下当前可调用的发布版本列表。 | 路径、可调用版本规则和列表用途已确认；字段级契约以 `/contract` 为权威。 |
| 查看内容模块治理契约 | 让管理员查看内容模块审批和生命周期管理接口的请求/响应结构、错误码和示例。 | 内容模块治理路径已随 OpenAPI v1 维护；查看范围沿用管理员契约可见性。 |
| 单笔生成 | 基于模板、发布版本和请求参数生成一份 DOCX/PDF。 | 路径命名、请求字段命名、响应 envelope 和 Schema 载体已确认；正式 OpenAPI v1 Schema 和示例已输出，后续随契约变更维护。 |
| 批量生成 | 基于同一模板和发布版本提交多笔生成请求。 | 独立 `batch-generate` 路径、默认上限、失败策略、`itemId` 必填唯一、重复 `itemId` 处理、同步失败明细、异步失败项重试策略、字段命名和全量明细返回已确认。 |
| 查询异步任务 | 查询异步生成任务状态、结果和错误明细。 | 查询路径、结果结构、状态命名和进度摘要已确认；不返回进度百分比。 |
| 取消异步任务 | 取消未完成且未过期的异步任务。 | 受控取消路径、授权方式、终态和不可取消错误码已确认。 |
| 获取下载地址文件 | 使用同步或异步返回的下载地址获取生成文件。 | 下载路径、15 分钟固定有效期、二次授权、多次下载、不可配置为一次性、过期不重新签发和结果保留已确认。 |
| 查询调用记录 | 调用方查询本凭证在模板下的历史调用与参数备份。 | 列表 `view=logical\|flat`、可选 `requestId` 过滤、详情含 sanitized parameters；幂等 replay 不新建记录；仅本 credential 可见。 |

## 路由与路径语义

API 路由需要同时支持显式发布版本路径和 default 路径。

API 路径统一采用 `/api/{environment}/v1` 前缀。`{environment}` 用于契约和调用路径表达环境；平台运行时仍通过环境变量读取当前部署环境，并校验路径中的 `{environment}` 与当前部署环境一致。

| 路由语义 | 用途 | 已确认规则 | 已确认路径 |
| --- | --- | --- | --- |
| 显式发布版本单笔生成 | 调用方明确选择模板和发布版本并生成单份文档。 | 模板标识和发布版本号跟随路径表达；`releaseVersion` 采用语义化版本号。 | `/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/generate` |
| default 单笔生成 | 调用方只选择模板，由 API 管理配置解析到默认目标发布版本并生成单份文档。 | default 路径必须显式配置到某个未停用发布版本，不得隐式指向最新版本。 | `/api/{environment}/v1/templates/{templateId}/default/generate` |
| 显式发布版本批量生成 | 调用方明确选择模板和发布版本并提交批量生成。 | 批量生成使用独立路径，避免与单笔生成仅靠请求体区分。 | `/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/batch-generate` |
| default 批量生成 | 调用方只选择模板，由 API 管理配置解析默认目标发布版本并提交批量生成。 | 批量 default 调用沿用 default 显式配置和审计规则。 | `/api/{environment}/v1/templates/{templateId}/default/batch-generate` |
| 异步任务查询 | 调用方查询异步任务状态、结果和错误明细。 | 查询路径挂在模板下，便于执行模板级授权。 | `/api/{environment}/v1/templates/{templateId}/tasks/{taskId}` |
| 异步任务取消 | 调用方取消未完成且未过期的异步任务。 | 取消路径挂在任务下，执行与任务查询相同的模板级授权；取消成功终态为 `CANCELLED`。 | `/api/{environment}/v1/templates/{templateId}/tasks/{taskId}/cancel` |
| 下载地址取文件 | 调用方使用返回的下载地址获取生成文件。 | 下载路径以文档为资源；下载时通过 `documentId` 关联模板并执行模板级二次授权。 | `/api/{environment}/v1/documents/{documentId}/download` |
| API 契约查看 | 调用方查看当前授权模板的契约摘要、路径、策略、错误码、信封 `schemas` 索引与逐字段 `variables`。 | 返回契约摘要，不内嵌完整 OpenAPI YAML；每可调用版本含 `variables[]`（IBL-A4）。 | `/api/{environment}/v1/templates/{templateId}/contract` |
| 管理面 API 契约查看 | 管理会话查看同装配契约（可含既有 policy/defaultRoute 明细）。 | 与 runtime 同装配；`variables[]` 对 admin 与 runtime 均可见（非凭证、非变量原值）。 | `/api/management/v1/templates/{templateId}/api/contract` |
| 可调用版本列表 | 调用方查看当前授权视角下可调用的发布版本列表。 | 返回可调用发布版本，不作为后台版本管理列表；可不附带完整 `variables[]`。 | `/api/{environment}/v1/templates/{templateId}/versions` |
| 调用记录列表 | 调用方分页查询本凭证在模板下的调用历史。 | `view=logical`（默认）或 `view=flat`；可选 `requestId`；不含其他 credential 记录。 | `/api/{environment}/v1/templates/{templateId}/invocations` |
| 调用记录详情 | 调用方查看单条调用详情与 sanitized 参数。 | `invocationId` 前缀 `INV-`；encryption 密码不返回；跨 credential 403。 | `/api/{environment}/v1/templates/{templateId}/invocations/{invocationId}` |

default 路径目标版本变更属于 API 管理配置变更，需要审计，并应提供影响预览。default 路径目标版本变更只支持立即生效，不支持未来定时生效或待生效变更；变更不主动通知调用方或管理员。

已确认路径汇总：

```text
Explicit version single generation
/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/generate

Default single generation
/api/{environment}/v1/templates/{templateId}/default/generate

Explicit version batch generation
/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/batch-generate

Default batch generation
/api/{environment}/v1/templates/{templateId}/default/batch-generate

Async task query
/api/{environment}/v1/templates/{templateId}/tasks/{taskId}

Async task cancellation
/api/{environment}/v1/templates/{templateId}/tasks/{taskId}/cancel

Download generated document
/api/{environment}/v1/documents/{documentId}/download

View API contract summary
/api/{environment}/v1/templates/{templateId}/contract

View API contract summary (management assembly)
/api/management/v1/templates/{templateId}/api/contract

List callable release versions
/api/{environment}/v1/templates/{templateId}/versions

List invocation records (caller-scoped)
/api/{environment}/v1/templates/{templateId}/invocations

Get invocation record detail
/api/{environment}/v1/templates/{templateId}/invocations/{invocationId}
```

default 路径调用时，审计记录需要能体现请求使用了 default 路径，以及 default 路径解析后的目标发布版本。

### default 路径契约展示确认

确认基线：API 契约展示 default 路径时，需要让被授权查看 API 契约的用户同时看到稳定 default 路径和当前实际目标发布版本，避免调用方误以为 default 自动指向最新版本。

| 展示字段 | 说明 | 当前状态 |
| --- | --- | --- |
| `defaultRoute.url` | default 单笔或批量生成路径。 | 已确认。 |
| `defaultRoute.currentTargetReleaseVersion` | 当前 default 路径指向的目标发布版本。 | 已确认。 |
| `defaultRoute.currentTargetStatus` | 当前目标发布版本状态，目标必须是未停用发布版本。 | 已确认。 |
| `defaultRoute.updatedAt` | default 目标最近更新时间。 | 已确认。 |
| `defaultRoute.updatedBy` | default 目标最近操作人。 | 已确认。 |
| `defaultRoute.explicitVersionUrl` | 当前目标发布版本对应的显式版本路径。 | 已确认。 |

不展示待生效目标版本，因为 default 目标版本变更不支持未来定时生效或待生效状态。

### default 目标变更治理确认

确认基线：default 路径目标版本变更只支持立即生效，不支持未来定时生效、待生效变更或取消待生效变更。变更不主动通知调用方或管理员，仅记录审计；调用方通过 API 契约查看当前 default 目标版本。

影响预览至少包含：

- 当前目标发布版本与候选目标发布版本，包括版本号、状态和是否可调用。
- 授权调用方范围摘要，包括受影响 API 凭证、AD Group 或调用方范围摘要。
- 近期 default 调用量摘要，例如近期调用量、失败量和主要调用方摘要。
- 契约差异摘要，例如变量、规则、输出能力差异摘要；不得展示敏感业务变量值或加密密码。
- 幂等影响提示，说明旧 `idempotencyKey` 命中旧 default 解析记录时会返回幂等冲突。

default 路径目标版本回滚按一次新的受控变更处理：管理员重新选择历史目标发布版本作为候选目标，执行影响预览，确认后立即生效，并记录审计。回滚不主动通知调用方或管理员。

## 条款或内容模块治理契约

已确认内容模块治理接口与模板生命周期管理接口共享同一后台管理员授权边界，但使用独立资源路径和独立请求/响应对象。

**P14-T01 范围说明：** OpenAPI 定义管理 CRUD（list/create/detail、版本 CRUD）及审批流转与治理操作路由（见下表）。
[P14-T01b](../plan/detail/P14-confirmed-large-domains.md) **Done** (2026-06-26)；持久化状态两轴映射见
[`domain-model.md` §2.9.2.1](../domain/domain-model.md#2921-产品状态--实现映射p14-t01)。
[P14-T01c](../plan/detail/P14-confirmed-large-domains.md) **Done** (2026-06-26) — 模板 content-module-references
GET/PUT、生命周期 impact preview、`PublishGateCheckCode.CONTENT_MODULE_REFERENCES` 发布阻断项。

**CE-K08（BDD `ready`，2026-07-15）：** `ContentModuleVersionView` / create-update 请求增加可选法务字段 `jurisdiction`、`effectiveFrom`、`effectiveTo`、`legalReviewRef`；`GET /content-modules` 增加对应筛选 query；发布门禁新增硬项 `PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED`（与 `CONTENT_MODULE_REFERENCES` 正交）。行为 SoT：[ce-k08-clause-legal-metadata.md](../behavior/ce-k08-clause-legal-metadata.md)。OpenAPI 字段以同片实现同步为准。**IBL-E5 修正：** 未来 `effectiveFrom` 由正交硬项 `CONTENT_MODULE_EFFECTIVE_NOT_STARTED` 阻断（见下）。

**IBL-E5 / F27 residual（BDD `ready`，2026-07-20；Task Master #132；[ADR-0066 Accepted](../adr/template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md)）：** 发布门禁新增硬项 `PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_NOT_STARTED`：任一钉扎 CM 版本 `effectiveFrom != null && utcNow.isBefore(effectiveFrom)` → FAIL（与 `CONTENT_MODULE_EFFECTIVE_EXPIRED` 正交；**禁止**复用 EXPIRED 码）。`effectiveFrom == null` 或 `utcNow == effectiveFrom` → 本项 PASS。已发布锁定版本运行期不因时钟新增本叶失败。管理面 `POST /api/management/v1/content-module-references/bulk-repin`（必填 `dryRun`；`toSemanticVersion` **xor** `useLatestApproved`；可选 `fromSemanticVersion` / `templateIds[]` / `groupCode`）：组内 `authorTemplates` 可见草稿钉扎批量改钉；dry-run 零持久化；apply 复用 upsertReference；`SKIPPED_LOCKED` / `SKIPPED_ALREADY_AT_TARGET` / `SKIPPED_NO_MATCH` / per-item `FAILED`（`BULK_REPIN_TARGET_INVALID`）；审计 `CONTENT_MODULE_BULK_REPIN`（含 dry-run）。**无**新角色 / capability；**无**延迟发布 / `SCHEDULED` 生命周期；管理 UI bulk 控制台非 Done 条件（`frontend_ui_in_scope=false`）。行为 SoT：[ibl-e5-effectivefrom-bulk-repin.md](../behavior/ibl-e5-effectivefrom-bulk-repin.md)（**BDD-IBL-E5-001…017** / E5-C*）。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。Accepted ADR ≠ E5 impl Done；**不**翻转 #3b/#5a。

**IBL-E6 / F28（BDD `ready`，2026-07-20；Task Master #133；[ADR-0067 Accepted](../adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md)）：** CM 版本 `content_structure_json` 内 `contentModuleRef` 构成 CM↔CM 嵌套边；写路径成功后维护可查询嵌套边投影。嵌套深度 = 自根最长简单路径边数；硬上限 **8**（同构 `ComputeDslLimits`）；`>8` → **422** `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED`；环路 → **422** `CONTENT_MODULE_NESTING_CYCLE`；目标不可解析/跨组不可见 → **422** `CONTENT_MODULE_NESTING_TARGET_UNRESOLVED`；畸形结构 JSON → **422** `CONTENT_MODULE_NESTING_STRUCTURE_INVALID`；多 `referenceKey` 解析到同一 Target → 图去重为单一 Parent→Target 边。扩展 `GET /content-modules/{moduleId}/where-used`：含直连 + 嵌套闭包模板；行字段 `referenceKind`（`DIRECT`\|`NESTED`）、`nestingDepth`、`nestingPathSummary`（非敏感 `moduleCode` 链；无条款全文）；权威 = 嵌套图 + 祖先钉扎（**不**扫 binding JSON）。生命周期 impact 与深度 where-used **同闭包**。发布门禁硬项：`CONTENT_MODULE_NESTING_CYCLE` / `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED` / `CONTENT_MODULE_NESTING_UNPINNED`（传递钉扎缺失；**禁止**复用 `CONTENT_MODULE_REFERENCES`）。渲染遇环 → 结构化失败（同 `CONTENT_MODULE_NESTING_CYCLE`）。**无**新角色 / capability；管理 UI 嵌套图可视化非 Done 条件（`frontend_ui_in_scope=false`）。行为 SoT：[ibl-e6-clause-nesting-governance.md](../behavior/ibl-e6-clause-nesting-governance.md)（**BDD-IBL-E6-001…018** / E6-C*）。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。Accepted ADR ≠ E6 impl Done；**不**翻转 #3b/#5a。

**IBL-E1 / PD-4（BDD `ready`，2026-07-19；Task Master #128；[ADR-0062 Accepted](../adr/template-lifecycle/0062-locale-variant-template-clause-model.md)）：** 模板包与内容模块包行增加必填 `locale`（BCP-47）与可选 `localeVariantFamilyId`；管理创建请求（`CreateTemplateRequest` / `CreateContentModuleRequest`）及 summary/detail 视图同步；`GET /templates` 与 `GET /content-modules` 增加可选精确筛选 `locale`（与既有 filters **AND**；非法值推荐空页）。同组同家族 `(localeVariantFamilyId, locale)` 冲突 → `409`（`LOCALE_VARIANT_CONFLICT`）。发布门禁新增硬项 `PublishGateCheckCode.CONTENT_MODULE_LOCALE_MISMATCH`，与 CE-K08 过期门禁正交。Runtime：路径仍钉扎模板版本；非空 `context.locale` 与模板 `locale` 语言不兼容 → `422`（`TEMPLATE_LOCALE_MISMATCH`）；省略 `context.locale` 不做该校验（compute 默认仍按 ADR-0056）。**不**提供按 locale 自动选包。行为 SoT：[ibl-e1-locale-variant-model.md](../behavior/ibl-e1-locale-variant-model.md)（**BDD-IBL-E1-001…018** / E1-C*）。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。

**IBL-E2 / PD-5（BDD `ready`，2026-07-20；Task Master #129；[ADR-0063 Accepted](../adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md)）：** Runtime `context` 白名单新增可选 `jurisdiction`、`product`；既有 `channel` 额外承担组合匹配（非 outbound delivery）。模板版本挂载 **Composition Inclusion Rules**（结构化；与锚点可见性 `/rules` 正交）。管理面 `GET|PUT /api/management/v1/templates/{templateId}/composition-inclusion-rules`（草稿可写；非法规则 → `422` `COMPOSITION_INCLUSION_RULE_INVALID`）。发布硬门禁 `PublishGateCheckCode.COMPOSITION_INCLUSION_REFERENCE_INVALID`。Runtime 确定性 INCLUDE/EXCLUDE；`requiredInclusion` 不满足 → `422` `COMPOSITION_INCLUSION_UNSATISFIED`；可选 CE-K08 双方非空 jurisdiction 不等 → `422` `CONTENT_MODULE_JURISDICTION_MISMATCH`。成功路径审计/invocation 摘要含非敏感 `compositionInclusionSummary`（`matchedRuleId` 默认纳入用字面 `NONE_DEFAULT`）。导出包携带 inclusion rules。路径仍钉扎模板版本；**不**按辖区自动选包；**不**在 CM 版本增加 product/channel 字段；管理 UI 规则编辑器非本叶 Done 条件（`frontend_ui_in_scope=false`）。行为 SoT：[ibl-e2-jurisdiction-rule-engine.md](../behavior/ibl-e2-jurisdiction-rule-engine.md)（**BDD-IBL-E2-001…016** / E2-C*）。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。Accepted ADR ≠ E2 impl Done；**不**翻转 #3b/#5a。

**IBL-E3 / PD-8（BDD `ready`，2026-07-20；Task Master #130；[ADR-0064 Accepted](../adr/template-lifecycle/0064-legal-compliance-approval-matrix.md)）：** 模板包级 `approvalMatrixMode` ∈ {`SINGLE_TRACK`,`LEGAL_THEN_COMPLIANCE`}（默认/迁移 = `SINGLE_TRACK`）。`CreateTemplateRequest` / `UpdateTemplateRequest` / `TemplateSummaryView` / `TemplateDetailView` 回显 mode；`approvalSubState` 扩展 `PENDING_LEGAL_DECISION` / `PENDING_COMPLIANCE_DECISION`；可选 `approvalStage` ∈ {`LEGAL`,`COMPLIANCE`}（或由子状态唯一推导）。Mode 在非法窗口写入 → **422** `APPROVAL_MATRIX_MODE_LOCKED`。`RECORD_APPROVAL_DECISION`（或等价阶段判定）错角色 → **403** `APPROVAL_STAGE_ROLE_FORBIDDEN`；错阶段 → **409/422** `APPROVAL_STAGE_MISMATCH`。协作队列新增 `LEGAL`（与 `APPROVAL` 分轨）。新角色 `LEGAL_REVIEWER` + capability `decideLegalApprovals`（见权限矩阵）。管理 UI 必交付（`frontend_ui_in_scope=true`）。行为 SoT：[ibl-e3-legal-approval-matrix.md](../behavior/ibl-e3-legal-approval-matrix.md)（**BDD-IBL-E3-001…018** / E3-C*）。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。Accepted ADR ≠ E3 impl Done；**不**翻转 #3b/#5a。

**IBL-E4 / PD-9（historical — BDD `ready`，2026-07-20；Task Master #131；[ADR-0065 Accepted](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md)；impl Done）：** 曾交付组范围 DocumentBrand / LegalEntity 目录、`context.legalEntityCode` 解析、模板 `allowedDocumentBrandCodes`、管理 UI。产品面由 **ADR-0071 / SYS-NORM Wave 6** 退役（见下）。历史证据：[ibl-e4-entity-document-brands.md](../behavior/ibl-e4-entity-document-brands.md)。

**SYS-NORM Wave 6 / D1（BDD `ready`，2026-07-21；TM #150；[ADR-0071 Accepted](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)）：** DocumentBrand / LegalEntity **管理端点退役**——`/api/management/v1/document-brands*`、`/legal-entities*`、`/groups/{groupCode}/default-legal-entity` 返回 **404 或 410** + 稳定码 `DOCUMENT_BRAND_SURFACE_RETIRED` / `LEGAL_ENTITY_SURFACE_RETIRED`（禁止 200 空目录冒充存活）。Runtime / preview / test-generation **不再** LegalEntity→DocumentBrand 目录解析；letterhead/logo/seal 来自 **Letterhead（master）**。`context.legalEntityCode` 仍白名单可选、**不驱动**目录、**不**产出退役目录 422 族。`allowedDocumentBrandCodes` generate **忽略**；写 fail-closed 或 strip（实现择一并对齐 OpenAPI）。晋升/导出依赖闭合 **不得要求** brand/entity sidecar（Wave 7 dry-run UI 另叶）。Legal holds **保持**。壳层 `REDBC`/`GREENBC` UI-only。行为 SoT：[sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md)（**BDD-SYS-NORM-D1-001…020**）。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。**不**翻转 #3b/#5a；**不**宣称 SYS-NORM program Done。

**PRR-C01 / Task #103（BDD `ready`，2026-07-18）：** 管理面 `GET|PUT /api/management/v1/templates/{templateId}/dev-version/author-word-page-count` 读写可选 `authorWordPageCount`（Microsoft Word 作者页数；**禁止**用 LO/PDF 回填）。PDF 成功路径按预算 `B=paginationDeltaBudgetPages`（默认 1）发出 `LOW_RISK_PAGINATION_DIFFERENCE`；`delta > 2×B` 时发布门禁 `PublishGateCheckCode.PAGINATION_DELTA_BUDGET` blocker。行为 SoT：[prod-adr-0042-0043-closeout.md](../behavior/prod-adr-0042-0043-closeout.md)。

**CE-U20（BDD `ready`，2026-07-17；Task Master #94）：** `ContentModuleSummaryView` 增加 head 版本投影字段 `reviewState`（必填）与 `lifecycleState`（可选）；`GET /api/management/v1/content-modules` 增加可选 query `status`（`DRAFT` \| `SUBMITTED` \| `APPROVED` \| `STOPPED` \| `DEPRECATED`），按 **head 版本** 徽章语义服务端精确过滤，与 `search` / `groupCode` / sort / CE-K08 legal filters **AND**；非法 `status` → 成功空页（不 400）。Head 选择：`updatedAt` 最大，并列取 `semanticVersion` 字典序更大者。**不**改变 create 请求体（仍为 `CreateContentModuleRequest.contentStructureJson`）、**不**改变审批/生命周期状态机。行为 SoT：[ce-u20-clause-create-structured.md](../behavior/ce-u20-clause-create-structured.md)。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。

**CE-G05（BDD `ready`，2026-07-17；Task Master #77）：** 条款目录 `GET /content-modules` 增加可选 `searchMode=NAME`（缺省，LR-C5 ILIKE）\| `FULL_TEXT`（catalog-filter 版本 `content_structure_json` 的 PostgreSQL tsvector；config `simple`）；与 `status` / `groupCode` / CE-K08 legal filters **AND**。新增只读 `GET /content-modules/{moduleId}/where-used`（授权范围内引用模板摘要；无条款全文）。模板年检见下方专节。行为 SoT：[ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md)。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。

**CE-U21（BDD `ready`，2026-07-17；Task Master #95；BE 契约）：** `TemplateExportAnchorBindingView` / 管理面绑定视图增加必填 `updatedAt`（ISO-8601 Instant，并发令牌）。`PUT /api/management/v1/templates/{templateId}/bindings/{anchorId}` 请求体 `UpsertAnchorBindingRequest` 增加可选 `expectedUpdatedAt`：对**已存在**绑定的更新必须提供且与库中 `updatedAt` 毫秒语义相等；匹配成功写入并返回新 `updatedAt`；不匹配 → **409** `BINDING_VERSION_CONFLICT` / category `CONFLICT` / `messageKey=api.error.template.bindingVersionConflict` / `retryable=true`；缺失 → **422** `TEMPLATE_VALIDATION_FAILED` / `api.error.template.bindingExpectedUpdatedAtRequired`。首次创建可省略 `expectedUpdatedAt`。模板 import replace 走服务端内部旁路（非作者 Save）。行为 SoT：[ce-u21-draft-anchor-concurrency.md](../behavior/ce-u21-draft-anchor-concurrency.md)。正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。

| 路由语义 | 用途 | 已确认规则 | 已确认路径 |
| --- | --- | --- | --- |
| 内容模块审批流转 | 对条款或内容模块版本执行提交、审批通过或审批不通过。 | 使用独立版本审批生命周期；审批前置条件和角色边界遵循权限矩阵与领域模型。 | `/api/{environment}/v1/admin/content-modules/{moduleId}/review/transition` |
| 内容模块生命周期操作 | 对条款或内容模块执行停用、恢复或废弃治理操作。 | 停用、恢复和废弃由管理员执行；执行前必须进行影响分析、二次确认并记录审计。 | `/api/{environment}/v1/admin/content-modules/{moduleId}/lifecycle/operation/apply` |

内容模块治理契约的正式字段与响应结构以 [OpenAPI v1](openapi-v1.yaml) 为准；本文档仅提供索引和语义解释。

### 内容模块治理响应字段语义确认

- `ContentModuleVersionView.contentStructureJson`（[`ContentModuleVersionView`](openapi-v1.yaml)）：可选字段；仅当调用方具备结构查看权限（[权限矩阵 §5.1](../security/permission-matrix.md#51-条款或内容模块权限矩阵) — `GLOBAL_ADMIN`、`GROUP_ADMIN`、`MASTER_DESIGNER`、`TEMPLATE_AUTHOR`、`TEMPLATE_APPROVER`）时在 list/detail 响应中返回；否则省略或为 `null`（fail-closed）。`TEMPLATE_TESTER` 无目录浏览权限（list/get 返回 `403`），不接收该字段。
- `ContentModuleSummaryView.reviewState` / `lifecycleState`（[`ContentModuleSummaryView`](openapi-v1.yaml)；CE-U20）：目录 list 行投影自模块 **head 版本**（见上 CE-U20 注与 domain §2.9.2）。`reviewState` 必填；`lifecycleState` 可空。与详情版本表徽章语义一致（lifecycle `DEPRECATED`/`STOPPED` 优先于 review）。模块无版本为异常 fail-closed（正常 create 路径至少一版本）。
- `GET /content-modules?status=`（CE-U20）：可选；枚举 `DRAFT` \| `SUBMITTED` \| `APPROVED` \| `STOPPED` \| `DEPRECATED`。匹配 head 展示状态；未知值 → 空页。与 CE-K08 legal query 的 **catalog filter version**（最新 `APPROVED`+`ACTIVE`，否则最新版本）选择规则正交，不得混用。
- `GET /content-modules?searchMode=`（CE-G05）：可选；`NAME`（缺省）= LR-C5 ILIKE（`name` ∪ `moduleCode` ∪ `groupCode`）；`FULL_TEXT` = catalog-filter 版本正文 tsvector 匹配（版本选择同 CE-K08：最新 `APPROVED`+`ACTIVE`，否则最新）。空 `search` 忽略。`search` 长度 > 200 → `422` VALIDATION。非法 `searchMode` → `422` VALIDATION。`FULL_TEXT` 有非空 search 时默认按 `ts_rank` DESC、其次 `updatedAt` DESC。列表响应仍遵守 §5.1 `contentStructureJson` 可见性；**不**因 FTS 放宽结构字段。
- `GET /content-modules/{moduleId}/where-used`（CE-G05 / IBL-E6）：分页可选；返回授权可见的引用模板摘要（`id`、`externalId`、`name`、`groupCode`、`lifecycleStatus`、可选 `pinnedSemanticVersion`、必填 `referenceKind`/`nestingDepth`、可选 `nestingPathSummary`）。直连复用 `template_content_module_reference`（及发布锁定关系）；嵌套命中走 CM↔CM 嵌套图 + 祖先钉扎；**不**扫 binding JSON；**不含**条款全文。无引用 → 200 空页；无模块浏览权 → 403/404 惯例；不可见组模板不得出现。
- CM 结构写嵌套校验（IBL-E6）：create/update `contentStructureJson` 持久化前校验环/深度/目标可解析/结构可解析；非法 → **422**（`CONTENT_MODULE_NESTING_CYCLE` / `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED` / `CONTENT_MODULE_NESTING_TARGET_UNRESOLVED` / `CONTENT_MODULE_NESTING_STRUCTURE_INVALID`）；零结构写入；同 Target 多 key 去重为单一边。
- 发布嵌套硬项（IBL-E6）：钉扎闭包环/超深/`CONTENT_MODULE_NESTING_UNPINNED`（传递钉扎缺失）→ publish-gate FAIL；与 `CONTENT_MODULE_REFERENCES` 正交。
- `ContentModuleLifecycleImpactSummary.templateStopRequired` / `releaseStopRequired`（[`ContentModuleLifecycleImpactSummary`](openapi-v1.yaml)）：当存在引用且 blocking 条件成立时（近 7 日 runtime 生成调用 > 0 或 default 路由受影响）分别提示管理员需停用引用模板或发布版本以立即阻断生成；与权限矩阵 §5.1 停用/废弃影响分析要求一致。示例见 [`content-module-lifecycle-operation-request.json`](examples/content-module-lifecycle-operation-request.json)。

### 内容模块治理校验与错误语义确认

确认基线：内容模块治理接口使用 `applied` + `errorCode` + `errorMessage` 返回治理结果；成功时 `applied=true` 并返回 `snapshot`，失败时 `applied=false` 并返回稳定错误码。

内容模块审批流转 `/review/transition`：

- `SUBMIT_FOR_REVIEW` 必须提供非空 `changeDescription`，否则返回 `422 MODULE_CHANGE_DESCRIPTION_REQUIRED`。
- `REJECT_REVIEW` 必须提供非空 `rejectionReason`，否则返回 `422 MODULE_REJECTION_REASON_REQUIRED`。
- 角色越权返回 `403 MODULE_REVIEW_ROLE_DENIED`。
- 状态前置条件不满足返回 `409 MODULE_REVIEW_STATE_TRANSITION_DENIED`。
- 请求体解析失败或必要字段缺失返回 `422 MODULE_REVIEW_REQUEST_INVALID`。

内容模块生命周期操作 `/lifecycle/operation/apply`：

- 仅 `GLOBAL_ADMIN`、`GROUP_ADMIN` 可执行，越权返回 `403 CONTENT_MODULE_ROLE_DENIED`。
- `STOP_USE`、`RECOVER`、`DEPRECATE` 必须同时满足 `impactSummaryViewed=true` 与 `secondConfirmation=true`，否则返回 `409 CONTENT_MODULE_IMPACT_CONFIRMATION_REQUIRED`。
- `STOP_USE` 与 `DEPRECATE` 还必须提供结构化 `impactSummary`，否则返回 `409 CONTENT_MODULE_IMPACT_CONFIRMATION_REQUIRED`。
- 生命周期状态前置条件不满足返回 `409 CONTENT_MODULE_STATE_TRANSITION_DENIED`。
- 请求体解析失败或必要字段缺失返回 `422 CONTENT_MODULE_REQUEST_INVALID`。

## 资产库管理契约（CE-E02 + ALGI）

**CE-E02（2026-07-16）+ ALGI（2026-07-22 / TM #154 / BDD `ready`）：** **组作用域**资产目录管理 API（平台共享目录 **已撤回**）。权威行为：[asset-library-group-isolation.md](../behavior/asset-library-group-isolation.md)；CE-E02 §15 修正：[ce-e02-asset-library.md](../behavior/ce-e02-asset-library.md)。正式字段与响应结构以 [OpenAPI v1](openapi-v1.yaml) 为准。

| 操作 | 方法 / 路径 | 说明 |
| --- | --- | --- |
| 列表 | `GET /api/management/v1/library/assets` | 分页 `page`/`size`；过滤 `groupCode`（可选精确）/ `assetClass` / `status`（默认 `ACTIVE`；`DISABLED`\|`ALL` 显式）/ `q`；条目含 `groupCode`；非 GLOBAL 结果 ∩ 授权组；未授权 `groupCode` → 空页；统一 envelope + `PageView` |
| 上传 | `POST /api/management/v1/library/assets` | multipart：`file` + `assetKey` + `assetClass` + **`groupCode`（必填）**；`201`；`result.groupCode`；同组 ACTIVE 冲突 → `409`；缺 `groupCode` → `422`；`Idempotency-Key` **预留 / 不强制不生效** |
| 停用 | `POST /api/management/v1/library/assets/{assetKey}/disable?groupCode={groupCode}` | 身份 `(groupCode, assetKey)`（**required query** `groupCode`）；`ACTIVE`→`DISABLED`；移除 namespaced 可解析 MinIO 键；已 `DISABLED` → **已确认**幂等 `200`；越权组 → `403` |

**键与类：** 逻辑绑定 `assetKey` 语法 `^[A-Za-z][A-Za-z0-9._-]{0,127}$`（E02-C2；模板 `imageRef`/`sealRef` 仍为裸键）；自然唯一 **`(groupCode, assetKey)`**；物理对象键 **`{groupCode}/{assetKey}`**（± 扩展名候选）。`assetClass`=`IMAGE`\|`SEAL`\|`OTHER`。MIME：`image/png`\|`image/jpeg`；应用层单文件上限 **5 MiB** → `422` `api.error.assetLibrary.payloadTooLarge`（nginx/Spring 边界超限仍可能为 413，须可读可翻译）。解析须模板组内 ACTIVE 目录命中（ALGI-C5）；既有 not-found `messageKey` 家族不变。权限见 [permission-matrix.md](../security/permission-matrix.md) §13.2 CE-E02 + ALGI。错误码（`error.code`）与 messageKey：`ASSET_LIBRARY_GROUP_CODE_REQUIRED` / `ASSET_LIBRARY_ASSET_KEY_INVALID` / `ASSET_LIBRARY_ASSET_KEY_CONFLICT` / `ASSET_LIBRARY_CONTENT_TYPE_UNSUPPORTED` / `ASSET_LIBRARY_CONTENT_TYPE_MISMATCH` / `ASSET_LIBRARY_PAYLOAD_TOO_LARGE` / `ASSET_LIBRARY_ASSET_NOT_FOUND`（messageKey 前缀 `api.error.assetLibrary.*`，含 `groupCodeRequired`）。

## Legal hold 管理契约（CE-G04）

**CE-G04（2026-07-16 确认 / BDD `ready`）：** 平台级 legal hold 管理 API + retention 删除前豁免叠加。权威行为：[ce-g04-legal-hold.md](../behavior/ce-g04-legal-hold.md)。正式字段与响应结构以 [OpenAPI v1](openapi-v1.yaml) 为准。权限：[permission-matrix.md](../security/permission-matrix.md) §13.1 / §13.2 CE-G04。领域：[domain-model.md](../domain/domain-model.md) §2.15.1。

**Confirmed — 路由：**

| 操作 | 方法 / 路径 | 说明 |
| --- | --- | --- |
| 列表 | `GET /api/management/v1/legal-holds` | 分页 `page`/`size`（默认 0/20）；可选 `status=ACTIVE|RELEASED`；缺省含 ACTIVE+RELEASED；统一 envelope + `PageView` |
| 详情 | `GET /api/management/v1/legal-holds/{id}` | 按 UUID；含 `invocationExternalIds`（INVOCATION_SET） |
| 创建 | `POST /api/management/v1/legal-holds` | `201`；`scopeType` 互斥字段见下 |
| 释放 | `POST /api/management/v1/legal-holds/{id}/release` | `ACTIVE`→`RELEASED`；**无**物理 DELETE |

**Confirmed — 请求/范围：**

| 项 | 规则 |
| --- | --- |
| 授权 | **仅** `GLOBAL_ADMIN`；其他已认证角色 **403** `ACCESS_DENIED` / `api.error.authorization.accessDenied`；未认证 401 |
| `TEMPLATE_WINDOW` | 必填 `effectiveFrom`；`templateId` **或** `templateExternalId`；可选 `effectiveTo`（`null`=开放结束）、`reason`（≤512）；**禁止**非空 `invocationExternalIds` |
| `INVOCATION_SET` | 非空 `invocationExternalIds`（1…500，去重 trim）；**禁止** template/window 字段 |
| 审计 | 成功 create → `LEGAL_HOLD_CREATED`；成功 release → `LEGAL_HOLD_RELEASED`；摘要无 variables / 凭证 / 完整参数体 |
| 豁免语义 | ACTIVE hold 叠加于 ADR-0040 / ADR-0048 硬删调度器；**不**改 ADR 正文；INVOCATION_SET **不**豁免 management 审计行（G04-C13） |
| Out of scope | eDiscovery 导出；GROUP_ADMIN 范围 hold；go-live / CD-3 |

**Fail-closed messageKeys（English-first；management legal-hold surface）：**

| Condition | HTTP | category | `error.code` | messageKey（稳定） |
| --- | --- | --- | --- | --- |
| 非 GLOBAL_ADMIN | 403 | `AUTHORIZATION` | `ACCESS_DENIED` | `api.error.authorization.accessDenied` |
| Hold 不存在 | 404 | `NOT_FOUND` | `LEGAL_HOLD_NOT_FOUND` | `api.error.notFound.legalHoldNotFound` |
| 模板不存在（TEMPLATE_WINDOW） | 404 | `NOT_FOUND` | `TEMPLATE_NOT_FOUND` | 既有模板 not-found 键 |
| 已 RELEASED 再释放 | 409 | `CONFLICT` | `LEGAL_HOLD_ALREADY_RELEASED` | `api.error.conflict.legalHoldAlreadyReleased` |
| 混合 scope / 校验失败 | 422 | `VALIDATION` | `REQUEST_BODY_INVALID` 等 | `api.error.validation.requestBodyInvalid` / `fieldRequired` / `fieldInvalid` / `fieldSizeInvalid` |

## 模板年检与条款正文全文检索（CE-G05）

**CE-G05（2026-07-17 确认 / BDD `ready`）：** 模板级 `nextReviewDue` + 年到期待办投影 + 条款正文 tsvector FTS + where-used。权威行为：[ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md)。正式字段与响应结构以 [OpenAPI v1](openapi-v1.yaml) 为准。权限：[permission-matrix.md](../security/permission-matrix.md) §5 / §5.1 / §13.2 CE-G05。领域：[domain-model.md](../domain/domain-model.md) §2.7 / §2.9.2。

**Confirmed — 年检路由与字段：**

| 操作 | 方法 / 路径 | 说明 |
| --- | --- | --- |
| 到期待办列表 | `GET /api/management/v1/author-workflow/annual-review-due-tasks` | 授权 `authorTemplates` 可见模板；`nextReviewDue != null` 且 `nextReviewDue <= todayUtc`（到期日当天入队）；**不**新建 collaboration `queue_type`；对齐 CE-U07 作者待办投影 |
| 完成年检 | `POST /api/management/v1/templates/{templateId}/annual-review/complete` | Body 可选 `{ "nextReviewDue": "YYYY-MM-DD" }`；缺省 = 完成日 UTC 日期 + 365 天；返回更新后的 `TemplateSummaryView`（含新 `nextReviewDue`） |
| Summary / Detail 读回 | 既有模板 Summary / Detail | 可选可空 `nextReviewDue`（`format: date`）；挂在 **template 行**，非单 release |

**Confirmed — 年检规则：**

| 项 | 规则 |
| --- | --- |
| 播种 | 模板**首次**进入 `PUBLISHED` 且 `nextReviewDue` 为空 → `publishInstant` UTC 日期 + 365 天；后续再发布**不**覆盖已有值 |
| 存量 | 迁移后可为 NULL；**不**强制回填；NULL 不入队 |
| 生命周期 | `STOPPED` / `DEPRECATED` 仍可入队（治理提醒）；逻辑删除不入队；complete → 404 |
| 授权 | 组范围访问 **且** `authorTemplates`；无新 capability；`TEMPLATE_TESTER`（默认无该权）→ 待办不可见 / complete **403** |
| 审计 | 成功 complete → `TEMPLATE_ANNUAL_REVIEW_COMPLETED`（templateId/externalId、previousNextReviewDue、newNextReviewDue、actorUsername；禁 variables / 凭证 / 条款全文） |
| 非目标 | 不阻断发布/runtime；无邮件/IM；无独立年检治理路由；不新建 collaboration `queue_type`；CD-3 / CE-O02 / go-live / #50 |

**Confirmed — FTS / where-used（条款侧，字段语义见内容模块节）：**

| 操作 | 方法 / 路径 | 说明 |
| --- | --- | --- |
| 正文检索 | `GET /api/management/v1/content-modules?search=&searchMode=FULL_TEXT` | `NAME` 缺省保持 LR-C5；`FULL_TEXT` 匹配 catalog-filter 版本正文 tsvector（config `simple`） |
| Where-used | `GET /api/management/v1/content-modules/{moduleId}/where-used` | 授权范围内引用模板；无条款全文 |

**Fail-closed messageKeys（English-first；management annual-review / FTS surface）：**

| Condition | HTTP | category | 说明 |
| --- | --- | --- | --- |
| 无 `authorTemplates`（年检 list/complete） | 403 | `AUTHORIZATION` | 与既有 author-workflow 惯例一致 |
| 模板不可见 / 已删（complete） | 404（或不泄露 403） | `NOT_FOUND` / `AUTHORIZATION` | 与既有模板 API 惯例一致 |
| `nextReviewDue` 非法 | 422 | `VALIDATION` | 稳定 messageKey；不写成功审计 |
| 无条款目录浏览权（FTS / where-used） | 403 | `AUTHORIZATION` | 同 §5.1 list/get；`TEMPLATE_TESTER` |
| `search` > 200 或非法 `searchMode` | 422 | `VALIDATION` | 稳定 messageKey |

## 模板导出/导入契约

已确认模板跨环境导出/导入接口与 [P14-T03](../plan/detail/P14-confirmed-large-domains.md) 行为一致，并经 **CE-E01** 扩展自包含 v2 + dry-run、**CE-E03** 全库导出 ZIP，以及 **SYS-NORM Wave 7** 晋级依赖闭合（promotion pack）+ 管理端 Import dry-run UI。正式字段与响应结构以 [OpenAPI v1](openapi-v1.yaml) 为准；本文档提供路由索引、bundle 语义、冲突策略与权限说明。权威行为：[ce-e01-export-bundle-v2.md](../behavior/ce-e01-export-bundle-v2.md)、[ce-e03-full-library-export.md](../behavior/ce-e03-full-library-export.md)、[sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md)。

**P14-T03 范围说明：** OpenAPI 定义管理路径导出（JSON + `format=zip`）与导入（`POST`）；bundle schema、`TemplateImportConflictPolicy` 枚举与权限边界对齐 [权限矩阵 §5](../security/permission-matrix.md#5-模板权限矩阵)。

**CE-E01 扩展（2026-07-16 确认）：**

| 项 | 已确认规则 |
| --- | --- |
| 导出 `bundleVersion` | 查询参数；缺省 `1` → `template-export-bundle-v1-json`；`2` → `template-export-bundle-v2-json` |
| v2 ZIP 条目 | 固定 `template-export-bundle.json` + `artifacts/master.docx`（钉扎母版 DOCX）；禁止可执行/未知必需外条目 |
| v2 JSON 字段（追加） | `masterPin`（revisionId/fileHash 等）、`clauseSnapshots[]`、`renderProfile`、`assetKeyManifest[]`；仍禁 secret/凭证/测试数据明文 |
| 导入 dry-run | 请求 `dryRun=true`（JSON 或 multipart）→ `200` + `dependencyReport`（`readyToCommit`/`blockingCount`/项级 `dependencyType`+`severity`）；**零**业务写入；审计 `TEMPLATE_IMPORT_DRY_RUN` |
| 依赖门禁 | 提交时 `blockingCount>0` → `422` `api.error.template.importDependenciesUnsatisfied` + 完整 report；禁止半导入 |
| 条款物化 | 目标缺模块但 snapshot 存在 → dry-run `WILL_MATERIALIZE`；提交事务内创建草稿内容模块版本 |
| 母版 | 仍要求目标已批准同组 `masterId`；用 `masterPin.masterFileHash` 与目标 revision 比对；本片不从 DOCX 自动建母版 |
| 导入载体 | 既有 JSON body；另支持 multipart（`file`=v2 ZIP + `masterId` + 可选 policy/dryRun） |
| UI | 管理端导出/导入/dry-run UI **本片 out of scope**（API-first；Wave 7 拥有 Import dry-run UI） |

**CE-E03 全库导出（2026-07-17 确认）：**

| 项 | 已确认规则 |
| --- | --- |
| 路径 | `POST /api/management/v1/library/export`（JSON body，非 multipart；body 可省略或 `{}`） |
| 请求字段 | 可选 `groupId`（UUID）；可选 `templateIds`（UUID 数组，最多 500）；可选 `includeSkipped`（boolean，缺省 `true`） |
| 成功响应 | `200` + `application/zip` 附件（**非** JSON envelope）；`Content-Disposition: attachment` |
| 格式常量 | 根 manifest `format` = `template-library-export-v1-zip`；内嵌 per-template 恒为 E01 v2（manifest `bundleVersion=2`） |
| ZIP 固定路径 | `library-export-manifest.json`；`templates/{templateId}.zip`（字节级等价 E01 `bundleVersion=2&format=zip`）；`masters/{masterFileHash}.docx`（去重）；`clauses/{moduleCode}__{semanticVersion}.json`（去重） |
| 候选解析 | 非空 `templateIds` → 仅这些 ID ∩ 授权 ∩ 存在；否则非空 `groupId` → 该组 ∩ 授权；否则授权范围内全部；再过滤导出合格生命周期（同 E01） |
| 护栏 | 合格候选最多 500；超出 → `422` `api.error.library.exportLimitExceeded`；不引入异步 job |
| 部分成功 | `includedCount ≥ 1` → HTTP 200；FAILED/SKIPPED 仅记 manifest；空 INCLUDED → `422` `api.error.library.exportEmpty` |
| 越权 ID | `templateIds` 中无权限/不存在的 ID **不**出现在 manifest `templates[]`；计入 `omittedUnauthorizedOrUnknownCount` |
| 资产 | 聚合 `assetKeyManifest`（键 + 用途）；**默认不**嵌入资产二进制（Wave 7 `dependencyClosure=PROMOTION` 见下） |
| 权限 | **完全复用**矩阵 §5「导出模板」；无新权限码、无新 capability；逐模板授权过滤 |
| 审计 | 成功（含部分成功）→ `LIBRARY_EXPORT`（`exportBatchId`、counts、scope、actor；无条款全文/DOCX/变量值） |
| Idempotency | **不要求** `Idempotency-Key`（每次新 `exportBatchId`） |
| Out of scope | 全库导入；管理端批量导出 UI / E2E；默认路径嵌入资产二进制；改 E01 单模板默认语义 |

正式 schema：[openapi-v1.yaml](openapi-v1.yaml) `LibraryExportRequest` / `LibraryExportManifestView`（operationId `exportLibraryTemplates`）。

**SYS-NORM Wave 7 — UAT→PROD promotion pack（2026-07-21 确认 / BDD `ready`）：**

| 项 | 已确认规则 |
| --- | --- |
| 参数名（OpenAPI 锁定） | `dependencyClosure=PROMOTION`（单模板导出 query；全库导出 body 同名字段）。缺省/省略 = 既有 E01/E03 行为不变 |
| 单模板 promotion ZIP | `bundleVersion=2` + `format=zip` + `dependencyClosure=PROMOTION`：E01 v2 基线 + `artifacts/assets/{assetKey}` 二进制 + 嵌套条款闭包 + 可选 `clauseNestingGraph`（edges + depth≤8） |
| 全库 promotion ZIP | 同上 profile：每个 `templates/{id}.zip` 为 promotion pack；根级可去重 `assets/{assetKey}` |
| 禁带 | DocumentBrand/LegalEntity sidecar；secrets / 凭证 / 测试数据变量明文（ADR-0071 Decision 5 / PP-C4/C5） |
| 导入 | 沿用 `POST …/templates/import`；自动识别嵌入资产/嵌套图；`dryRun=true` 零业务写入；提交落地模板 **DRAFT**；可选物化资产与 **DRAFT** 母版（**禁止**经 pack 跳过母版 APPROVED） |
| 报告类型（加法） | `dependencyType` 增 `CLAUSE_NESTING`、`ASSET_BINARY`（亦可复用 `ASSET_KEY` + `ASSET_WILL_MATERIALIZE` / `ASSET_BINARY_ABSENT`） |
| 权限 | **无新码**；复用矩阵 §5 导出/导入（含 dry-run） |
| UI | Templates **Import** 对话框：**Check dependencies**（`dryRun=true`）→ 报告 → **Import** 仅当 `readyToCommit=true`（P-Q4） |
| 兼容 | 非 promotion 的 v1/v2/E03 默认路径 fail-closed 语义不得回退 |
| 行为 SoT | [sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md) **BDD-SYS-NORM-PP-001…020**；上游 extend E01/E03 |

| 路由语义 | 用途 | 已确认规则 | 已确认路径 |
| --- | --- | --- | --- |
| 模板导出（JSON） | 导出已通过审批或已发布模板 bundle（元数据、变量、绑定、规则、内容模块引用、API 策略快照；v2 另含指纹/快照/清单）。 | 仅 `PENDING_RELEASE`、`PUBLISHED`、`STOPPED`、`DEPRECATED` 可导出；默认 `template-export-bundle-v1-json`；`bundleVersion=2` 为 v2；不得包含 secret、API 凭证或运行时凭证；导出动作记录审计。 | `GET /api/management/v1/templates/{templateId}/export` |
| 模板导出（ZIP） | 与 JSON 相同 bundle，封装为单文件 ZIP 附件；v2 另嵌母版 DOCX。 | v1：ZIP 内仅含 `template-export-bundle.json`；v2：另含 `artifacts/master.docx`；响应 `Content-Type: application/zip`；`Content-Disposition` 为 attachment。 | `GET /api/management/v1/templates/{templateId}/export?format=zip`（可选 `bundleVersion=2`） |
| 晋级包导出（Wave 7） | UAT→PROD promotion dependency closure：资产二进制 + 条款嵌套闭包。 | 须 `bundleVersion=2` + `format=zip` + `dependencyClosure=PROMOTION`；ZIP 另含 `artifacts/assets/{assetKey}`；无 brand/entity sidecar；无 secrets。行为：[sys-norm-promotion-pack.md](../behavior/sys-norm-promotion-pack.md)。 | `GET /api/management/v1/templates/{templateId}/export?bundleVersion=2&format=zip&dependencyClosure=PROMOTION` |
| 全库导出（ZIP）（CE-E03） | 一次导出授权范围内（或筛选后）全部导出合格模板：根 manifest + 嵌套 E01 v2 per-template ZIP + 去重母版/条款目录。 | 格式 `template-library-export-v1-zip`；**权限同矩阵 §5 导出模板**（无新码）；空集/超限 422；部分成功允许；默认**不含**资产二进制；`dependencyClosure=PROMOTION` 时嵌套包为晋级包且根可含去重 `assets/`；全库导入 out of scope。OpenAPI：`exportLibraryTemplates`。行为：[ce-e03-full-library-export.md](../behavior/ce-e03-full-library-export.md)。 | `POST /api/management/v1/library/export` |
| 模板导入 / dry-run | 将 bundle（含晋级包）导入目标环境并从草稿重新走流程；或仅预检依赖。 | 提交后模板状态为 `DRAFT`；须重新执行测试→审批→发布；`masterId` 须为同 `groupCode` 下已批准母版（绑定既有母版时）；晋级包可物化 DRAFT 母版但**不得**经 pack 置 APPROVED；`dryRun=true` 不落库；导入动作记录审计并返回 `importBatchId`（dry-run 无 batch 业务写入）。管理端 Import 对话框 dry-run UI = Wave 7。 | `POST /api/management/v1/templates/import` |
| 目录列表分页（LR-C5） | 管理端 Templates / Masters / Content-modules **包列表**服务端分页与筛选。 | 见下方「目录列表分页契约（LR-C5）」；完整行为 [lrp-c5-catalog-pagination.md](../behavior/lrp-c5-catalog-pagination.md)；正式字段以 [OpenAPI v1](openapi-v1.yaml) 为准。CE-G05：content-modules 可选 `searchMode=FULL_TEXT`（见上专节）。 | `GET /api/management/v1/templates` · `GET /api/management/v1/masters` · `GET /api/management/v1/content-modules` |
| Dashboard Overview 汇总（PRR-D01c） | 首屏统计卡授权组范围内分桶计数 + 目录总数；**禁止**以 fetch-all 为权威源。 | 见下方「Dashboard summary 契约（PRR-D01c）」；行为 [prod-dashboard-summary-api.md](../behavior/prod-dashboard-summary-api.md)；OpenAPI `getDashboardSummary`。 | `GET /api/management/v1/dashboard/summary` |
| 模板年到期待办（CE-G05） | Dashboard Tasks 年检分区数据源。 | 见「模板年检与条款正文全文检索（CE-G05）」；**不**新建 collaboration `queue_type`。 | `GET /api/management/v1/author-workflow/annual-review-due-tasks` |
| 完成模板年检（CE-G05） | 滚动 `nextReviewDue` 并写审计。 | 可选 body 下一到期日；缺省 +365 UTC 日；需 `authorTemplates`。 | `POST /api/management/v1/templates/{templateId}/annual-review/complete` |
| 条款 where-used（CE-G05 / IBL-E6） | 模块被哪些授权可见模板引用（只读；含嵌套深度命中）。 | 直连 reference + 嵌套图闭包；`referenceKind`/`nestingDepth`/`nestingPathSummary`；无条款全文。 | `GET /api/management/v1/content-modules/{moduleId}/where-used` |
| 模板版本线列表 | 分页列出模板包下进行中 dev 行与已发布 release 行，供包 hub 导航。 | 含 `release_version IS NULL` 的当前 dev 行与全部已发布行；跨组 `403 ACCESS_DENIED`；排序为 dev 行优先、再按 dev 版本降序。 | `GET /api/management/v1/templates/{templateId}/version-lines?page=&size=` |
| 模板版本线详情 | 只读查看指定版本线的变量、绑定与规则快照。 | 版本线须属于该模板；跨组 `403`。CE-U19：已发布且有钉扎时可含可选 `result.masterPin`（同 release 详情）。 | `GET /api/management/v1/templates/{templateId}/version-lines/{versionLineId}` |
| 模板 dev 版本详情 | 获取指定 dev 行的编排详情（在途编辑）。 | 已发布 dev 行只读；变更返回 `403 TEMPLATE_VERSION_IMMUTABLE`。 | `GET /api/management/v1/templates/{templateId}/dev/{devVersionId}` |
| 锚点绑定 upsert（CE-U21） | 在途 DRAFT/TESTING 创建或更新单锚点结构化绑定。 | 响应含必填 `updatedAt`；更新须带匹配的 `expectedUpdatedAt`；陈旧 → `409 BINDING_VERSION_CONFLICT`；缺令牌 → `422`。 | `PUT /api/management/v1/templates/{templateId}/bindings/{anchorId}` |
| 模板 release 详情 | 获取已发布 release 只读快照。 | 按语义版本 `releaseVersion` 定位；未知版本 `404`。CE-U19：`result.masterPin` 可选（见下方「release `masterPin`（CE-U19）」）。 | `GET /api/management/v1/templates/{templateId}/releases/{releaseVersion}` |
| release / version-line `masterPin`（CE-U19） | 管理读回 CE-K01 母版钉扎，供 Package Hub Dependencies 只读面。 | 加法可选字段；形状对齐 E01 `TemplateExportMasterPinView`；无新权限、无新聚合 API。 | 同上 release GET；必要时同 schema 的 `GET …/version-lines/{versionLineId}` |
| 克隆已发布 release | 从已发布 release 复制快照到新的 DRAFT dev 行（`max(dev_version_number)+1`）。 | 存在进行中 dev 行时 `409 TEMPLATE_DEV_LINE_IN_FLIGHT`；成功后模板包状态为 `DRAFT`；记录 lifecycle 审计。 | `POST /api/management/v1/templates/{templateId}/release-versions/{releaseVersion}/clone` |
| 粘贴清洗（P18-T10 / ops-paste-binding-seam） | 编辑期将 Word/HTML 清洗为受控结构化 JSON + 非敏感摘要。 | SoT **ADR-0019**：script / iframe / **object** / **absolute** → `BLOCKED`（整次 `blocked=true`，无 cleaned JSON）。Accept 后绑定持久化非敏感 `pasteCleaningEvidence`；未解除阻断在 validate / `computeBindingStatus` / PublishGate **fail-closed**。**不**新增权限面（复用配置锚点内容）。行为：[ops-paste-binding-seam.md](../behavior/ops-paste-binding-seam.md)；领域 §2.6.7。 | `POST /api/management/v1/templates/{templateId}/paste-clean`（管理面；OpenAPI 绑定/validate/export 已声明 `pasteCleaningEvidence`） |
| 变量 Schema PII 标签（CE-G03） | 变量 upsert/view 可选 `piiCategory`。 | 见下方「测试数据集 PII 治理（CE-G03）」；导出 bundle `variables[]` 携带该字段（OpenAPI `TemplateExportVariableSchemaView.piiCategory`）。 | 既有变量 Schema 管理路由（与配置模板变量同权） |
| 测试数据集 PII 闸门（CE-G03） | create/update 触及 PII 标记字段非空值时强制 `piiHandling`。 | fail-closed；`SYNTHETIC` 或 `EXPLICIT_SENSITIVE`+审计；无新权限。行为：[ce-g03-testdata-pii.md](../behavior/ce-g03-testdata-pii.md)。 | `POST/PUT /api/management/v1/templates/{templateId}/test-data-sets[/{testDataSetId}]` |
| 批量测试历史钻取（CE-U18） | 管理端历史摘要暴露可消费的逐样本结果，供 Testing 历史展开与跳转。 | 见下方「批量测试历史 sampleResults（CE-U18 / PTA）」；正式字段以 [OpenAPI v1](openapi-v1.yaml) `BatchTestRunSummaryView.sampleResults` 为准。 | `GET /api/management/v1/templates/{templateId}/batch-tests`（首选）；或同字段的 `GET .../batch-tests/{runId}` 详情（实现择一） |
| 预览产物下载（既有；PTA 文档化） | 对 SUCCEEDED preview 流式下载 DOCX / PDF，供 authoring Testing 与 **已发布 release Testing** 只读回顾。 | 见下方「预览产物下载（既有路径；PTA）」；**不**按 `PUBLISHED`/`STOPPED`/`DEPRECATED` 生命周期拦截；**不**新建第二条下载 API。 | `GET /api/management/v1/templates/{templateId}/previews/{previewId}/artifacts/docx` · `…/artifacts/pdf` |

### release `masterPin`（CE-U19）

管理面契约（**不**改变 caller-facing runtime generate；**不**新增 `/templates/{id}/dependencies` 聚合 API）。权威行为：[ce-u19-dependency-readonly-view.md](../behavior/ce-u19-dependency-readonly-view.md) **U19-D5**。正式 schema：[openapi-v1.yaml](openapi-v1.yaml) `TemplateVersionLineDetailView.masterPin` → 复用 `TemplateExportMasterPinView`。

| 项 | 已确认 |
| --- | --- |
| 暴露面 | **首选** `GET /api/management/v1/templates/{templateId}/releases/{releaseVersion}` 的 `result.masterPin`；**允许**同字段出现在 `GET …/version-lines/{versionLineId}`（同一 `TemplateVersionLineDetailView`） |
| 字段形状 | 与 CE-E01 导出 `masterPin` 对齐：`masterRevisionId`（UUID）、`masterFileHash`（SHA-256 小写 hex）、可选 `revisionSequence`、可选 `pinOrigin`（`PUBLISHED` \| `PINNED_RETROACTIVELY` \| `EXPORT_TIME`） |
| 有无 pin | 行上存在 CE-K01 `master_revision_id` / `master_file_hash` 时填充；未钉扎（如 in-flight）→ `null` 或省略 |
| 授权 | 沿用既有模板 release / version-line 读边界（与 Package Hub 相同）；**无**新权限位 |
| 非目标 | 新建依赖聚合微服务或端点；CE-E01 导出 UI；改写 `TemplateDetailView` 列表摘要（本片不要求） |

### 批量测试历史 sampleResults（CE-U18 / PTA）

管理面契约（**不**改变 caller-facing runtime generate / `batch-generate`）。权威行为：[ce-u18-batch-test-history.md](../behavior/ce-u18-batch-test-history.md)；PTA 持久化对齐：[published-template-test-artifacts.md](../behavior/published-template-test-artifacts.md)（BDD-PTA-004）。正式 schema：[openapi-v1.yaml](openapi-v1.yaml) `BatchTestHistorySampleResultView` / `BatchTestRunSummaryView` / `BatchTestRunSummaryListResponse`。

| 项 | 已确认 |
| --- | --- |
| 数据来源 | 持久化 `BatchTestRunEntity.sampleResultsJson`（P12）；API **不得**要求 FE 直读 DB |
| 响应字段名 | `sampleResults`（数组；`null` 或 `[]` 表示尚未可用 / 空） |
| 暴露面 | **首选**扩展 `GET .../batch-tests` 列表项；**允许**改为（或另加）`GET .../batch-tests/{runId}` 详情返回同名字段 — 实现择一并单测锁定 |
| 规范样本形状（异步） | `dataSetExternalId`、`success`、可选 `errorDetail`；当样本产生了 preview 时 **必须**含非空 `previewId`，并在产物已落库时含 `docxKey` / `pdfKey`（与 `PreviewRecordView` 存储键映射） |
| 失败样本 | `success=false` 时可无 `previewId` / `docxKey` / `pdfKey`（null-ok） |
| 历史兼容 | 旧同步形状（`testDataSetId` / `previewId` / `status` 等）及 **PTA 修复前**异步行（`previewId`/`docxKey`/`pdfKey` 为 null）可能仍出现；**FE normalize**；无 `previewId` 则不展示 Open preview / 无钻取 |
| 授权 | 沿用 `requireReadableSnapshot`；无读权限 → 既有 403/404，不泄露他组样本；**不**扩大 permission-matrix |
| 管理 UI 路径 | 全量测试用户旅程仅异步 `POST .../batch-tests/run` + SSE；退役同步 `POST .../previews/batch-test` 的用户旅程调用（后端 endpoint / seed 服务直调可暂留） |
| 非目标 | Runtime P11 generation 批处理；历史保留策略变更；go-live / CD-3 / #50；为下载 API 新增 PUBLISHED 生命周期闸 |

**Pending / open：** 无（PTA 合同字段与下载生命周期语义已由用户确认；实现落库见 PTA-T02，非本契约开放项）。

### 预览产物下载（既有路径；PTA）

管理面契约：文档化**既有** preview artifact 下载路径（`PreviewController` / `PreviewArtifactDownloadService`），供 authoring Testing 与 published release Testing 只读回顾共用。权威行为：[published-template-test-artifacts.md](../behavior/published-template-test-artifacts.md)（BDD-PTA-002 / BDD-PTA-008）；对照 [preview-success-artifact-download-journey.md](../behavior/preview-success-artifact-download-journey.md)。正式 OpenAPI：`downloadTemplatePreviewDocx` / `downloadTemplatePreviewPdf`。

| 项 | 已确认 |
| --- | --- |
| 路径 | `GET /api/management/v1/templates/{templateId}/previews/{previewId}/artifacts/docx` · `…/artifacts/pdf` |
| 响应 | 原始字节流 + `Content-Disposition: attachment`（**非** JSON envelope） |
| 前置 | preview `status=SUCCEEDED` 且产物仍可用（既有不可用 / 清理语义） |
| 授权 | `requireReadableSnapshot`（或现行等价）；403/404 fail-closed；**无**新 capability |
| 生命周期 | **不**因 `lifecycleStatus` ∈ {`PUBLISHED`,`STOPPED`,`DEPRECATED`} 拒绝；本片**禁止**新增 PUBLISHED 下载阻断 |
| 非目标 | 新建第二条「发布专用」下载 API；放宽跨组读；翻转 #3b/#5a |

**Pending / open：** 无。

### 测试数据集 PII 治理（CE-G03）

管理面契约（**不**在 caller-facing OpenAPI 运行时 generate 路径上扩展；导出变量 Schema 字段见 [openapi-v1.yaml](openapi-v1.yaml) `VariablePiiCategory` / `TemplateExportVariableSchemaView.piiCategory`）。权威行为：[ce-g03-testdata-pii.md](../behavior/ce-g03-testdata-pii.md)。

#### Variable Schema — `piiCategory`

| 项 | 已确认 |
| --- | --- |
| 字段 | 可选 `piiCategory`（API / DB / UI / export-import bundle） |
| 枚举（`UPPER_SNAKE_CASE`） | `NONE`、`PERSONAL_NAME`、`GOVERNMENT_ID`、`FINANCIAL_ACCOUNT`、`CONTACT`、`ADDRESS`、`OTHER_SENSITIVE` |
| 缺省 | 省略或 `null` → 持久化为 `NONE`；导入旧 bundle 无字段 → `NONE` |
| 非法值 | `422` `VALIDATION`；不持久化 |
| PII 标记字段 | `piiCategory ≠ NONE` |

#### Test Data Set create/update — `piiHandling`

触发：当前模板 Schema 存在 ≥1 个 PII 标记字段，且请求 `variables` 对该字段提供非空值（optional 缺省/null/空串不触发；required 空值先走 CE-U03 `REQUIRED`）。

| 请求字段 | 何时必需 | 语义 |
| --- | --- | --- |
| `piiHandling` | 触发后必需 | `SYNTHETIC` \| `EXPLICIT_SENSITIVE` |
| `piiConfirmReason` | 仅 `EXPLICIT_SENSITIVE` | 非空白，≤2048；可进审计（**不是**变量值） |
| `secondaryConfirmed` | 仅 `EXPLICIT_SENSITIVE` | 必须 `true` |

未触发时可不传 `piiHandling`（行为 = CE-U03 only）。校验顺序：授权 → 锁定不可变 → U03 schema → **PII 闸门** → 持久化（→ `EXPLICIT_SENSITIVE` 审计）。`derive` / `delete` / `lock` / list / get / preview **不**新增 PII 闸门。

#### Fail-closed messageKeys（English-first）

| Condition | HTTP | category | messageKey（稳定） | English default (implement in `messages_en.properties`) |
| --- | --- | --- | --- | --- |
| PII 触发且缺/非法 `piiHandling` | 422 | `VALIDATION` | `api.error.template.testDataSetPiiHandlingRequired` | PII-tagged test data values require `piiHandling` of `SYNTHETIC` or `EXPLICIT_SENSITIVE`. |
| `EXPLICIT_SENSITIVE` 缺 reason | 422 | `VALIDATION` | `api.error.template.piiConfirmReasonRequired` | An explicit sensitive test-data confirmation requires a non-blank reason. |
| `EXPLICIT_SENSITIVE` 未二次确认 | 422 | `VALIDATION` | `api.error.template.piiSecondaryConfirmRequired` | An explicit sensitive test-data confirmation requires secondary confirmation. |
| 非法 `piiCategory` | 422 | `VALIDATION` | `api.error.template.piiCategoryInvalid` | The variable `piiCategory` value is not supported. |

可选 `fieldErrors` 可指向 `piiHandling` / `piiConfirmReason` / `secondaryConfirmed` / 变量路径。`EXPLICIT_SENSITIVE` 成功须写耐久管理审计（建议 `eventType=TEMPLATE_TEST_DATA_PII_EXPLICIT_CONFIRM`）：含 keys、categories、reason、`variablesHash`；**禁止**变量明文。审计写失败 → 整笔回滚（不得出现「已存敏感值但无审计」）。

### Dashboard summary 契约（PRR-D01c）

已确认 Dashboard Overview 统计卡权威源为服务端聚合（BDD-PRR-D01C；OpenAPI `getDashboardSummary`）。**不是** catalog `PageView`；**不是** collaboration / workflow inbox。

| 项 | 已确认规则 |
| --- | --- |
| 路径 | `GET /api/management/v1/dashboard/summary` |
| 响应 | 统一 envelope；`result` = `DashboardSummaryView` |
| 字段 | `masterPendingReview` · `masterVersionsInProgress`（DRAFT∪REJECTED）· `templateVersionsInWorkflow`（DRAFT∪TESTING∪APPROVAL∪PENDING_RELEASE）· `publishedVersions` · `stoppedVersions` · `catalogMasters` · `catalogTemplates`（均为整数 ≥ 0） |
| 授权 | 与 catalog list 相同的会话组范围（**无新 capability；非 object-scope**）；无组 → 全 0；未认证 → 401；不泄露他组。矩阵：[permission-matrix.md](../security/permission-matrix.md) §13.1.3 |
| 非目标 | `pendingActions` / `externalServicesAlerts`（继续既有 tasks / alerts API）；图表时间序列；废弃全局 `fetchAll*` 符号 |

### 目录列表分页契约（LR-C5）

已确认管理端三大目录包列表统一为服务端 `PageView`（BDD-LRP-C5-CATALOG-001；OpenAPI `listTemplates` / `listMasters` / `listContentModules`）。**不**定义 LR-C6 命令面板专用 API（C6 可复用本切片的 `search` 参数）。

| 项 | 已确认规则 |
| --- | --- |
| 响应 | `result` = `PageView`：`content` / `page` / `size` / `totalElements` / `totalPages`（字段名非 Spring `number`） |
| 分页 | `page` 默认 **0**；`size` 默认 **20**，合法范围 **1…100**；越界/缺失规范化（不 500）；超出末页 → 空 `content`，`totalElements` 不变 |
| 默认排序（COR-F09） | **行分页** + `groupCode ASC`，次级 `updatedAt DESC`（`sort=groupCodeAsc`）；**不**按组个数分页 |
| 共用 query | `search`（可选，contains，空忽略）；`groupCode`（可选，精确，与会话授权求交） |
| Templates 专有 | `lifecycleStatus`；`approvalSubState`（审批 chip：`APPROVAL` + `PENDING_DECISION`；**IBL-E3** 另支持 `PENDING_LEGAL_DECISION` / `PENDING_COMPLIANCE_DECISION`）；`sort` 另含 `externalIdAsc`；**IBL-E1** 可选精确 `locale`（BCP-47；与其它 filters **AND**） |
| Masters 专有 | `status`（`MasterDocumentReviewStatus`）；`sort` 接受 `groupAsc` 作为 `groupCodeAsc` 同义 |
| Content-modules 专有 | 既有可选 `groupCode` 与 page/size/search/sort 共存；`sort` 另含 `moduleCodeAsc`；**CE-U20** 落地可选 `status`（head 版本徽章语义；见「条款或内容模块治理契约」CE-U20 注）。LR-C5 C5-C6 曾写「v1 不强制」——本表以 CE-U20 BDD `ready` 为最新确认；**IBL-E1** 可选精确 `locale`（与 status / CE-K08 / searchMode **AND**） |
| 破坏性契约 | Masters / Content-modules 自裸数组升级为 `PageView`（管理端一体升级） |
| 溯源 | OPT-F4 residual（masters/modules + 端到端 filter）；COR-F09 语义保留为默认 group-first 行排序 |

### 权限边界（对齐权限矩阵 §5）

| 角色 | 导出 | 导入 | 说明 |
| --- | --- | --- | --- |
| 全局管理员 | 是（全部模板） | 是（全部模板） | 不受组范围限制。**CE-E03** 全库导出共用本列「导出」权限（`POST …/library/export`）；无新权限码。 |
| 分组管理员 | 是（被授权组范围内） | 是（被授权组范围内） | 须满足 `groupCode` 组访问判定；全库导出仅含被授权组范围内合格模板。 |
| 模板编排人员 | 是（自己负责的模板） | 是（自己负责的模板） | 仅 `createdBy` 与当前会话用户一致时可导出/导入已有模板；新建导入须满足组访问；全库导出仅含自己负责的合格模板。 |
| 母版设计人员、测试人员、审批人员、API 调用方 | 否 | 否 | 不因角色本身获得导出/导入权限（含全库导出 → 403）。 |

导入到生产环境后从草稿阶段重新走完整流程；遇到已有相同内部模板 UUID 时，`KEEP_TEMPLATE_ID` 保留模板 ID 并复位为草稿开发版本，不重新生成模板 ID 或 API 地址（与权限矩阵 §5 导入说明一致）。

### Bundle schema（`TemplateExportBundleView`）

| 字段 | 必需 | 说明 |
| --- | --- | --- |
| `format` | 是 | v1：`template-export-bundle-v1-json`；v2：`template-export-bundle-v2-json`。 |
| `metadata` | 是 | 源环境快照：`templateId`（内部 UUID）、`externalId`、`groupCode`、`name`、`description`、`masterId`、`lifecycleStatus`、`releaseVersion`、`devVersionId`、`devVersionNumber`、`exportedAt`。 |
| `variables` | 是 | 变量 Schema 列表（`variableKey`、`variableType`、`required`、可选 `piiCategory` 等；CE-G03）。 |
| `bindings` | 是 | 锚点绑定列表。每项可含可选非敏感 `pasteCleaningEvidence`（粘贴清洗 residue；**禁止**源 HTML）。未解除粘贴阻断在 validate / publish 路径 fail-closed — 见 [ops-paste-binding-seam.md](../behavior/ops-paste-binding-seam.md) / domain-model §2.6.7。 |
| `rules` | 是 | 组合规则列表。 |
| `contentModuleReferences` | 是 | 内容模块引用列表；`locked=true` 的发布锁定引用在导入时不重新写入。 |
| `policySnapshot` | 否 | 模板级 API 管理策略快照（AD Group、输出/批量/加密/default 路由等）；不含 API 凭证 secret。 |
| `masterPin` | v2 是 | CE-E01：`masterRevisionId`（UUID）、`masterFileHash`（SHA-256 小写 hex）、可选 `revisionSequence` / `pinOrigin`（`PUBLISHED` \| `PINNED_RETROACTIVELY` \| `EXPORT_TIME`）。消费 CE-K01 钉扎字段，见 [ce-k01-release-bundle-pinning.md](../behavior/ce-k01-release-bundle-pinning.md)。 |
| `clauseSnapshots` | v2 是 | CE-E01：条款正文/结构快照数组（可空）。项含 `moduleCode`、`moduleVersionId`、`versionNumber`、`contentStructureJson`；可选 `locked` 与 CE-K08 法务元数据。 |
| `renderProfile` | v2 否 | CE-E01：`version` + `json`（JSON 文本快照）。皆空可省略；dry-run 记 `RENDER_PROFILE_ABSENT`（INFO）。 |
| `assetKeyManifest` | v2 是 | CE-E01：`referenceKey` + `usage`（`IMAGE` \| `OTHER`）；可空数组；默认无二进制。Wave 7 promotion：键仍在此清单；二进制在 ZIP `artifacts/assets/{assetKey}`。 |
| `clauseNestingGraph` | promotion 可选 | Wave 7：`edges[]`（`parentModuleCode` / `childModuleCode` / `depth`≤8）+ 可选 `maxDepth`；非嵌套可省略或空 edges。 |

JSON 导出成功响应 envelope：`metadata` + `result`，其中 `result.format` 与 `result.bundle` 承载上述结构。ZIP 导出不含 envelope；v1 文件内容为 bundle JSON；v2 另含 `artifacts/master.docx`（字节 SHA-256 须等于 `masterPin.masterFileHash`）；Wave 7 promotion 另可含 `artifacts/assets/{assetKey}` 二进制。

### 导入 dry-run 与依赖报告（CE-E01 + Wave 7）

| 项 | 已确认 |
| --- | --- |
| 请求 | `dryRun: true`（JSON）或 multipart 同名字段 |
| 成功 | `200`；`result.imported=false`；`result.dependencyReport` 含 `items[]`、`readyToCommit`、`blockingCount`、`warningCount`、`infoCount`、可选 `bundleFormat` |
| 项字段 | `dependencyType`（`MASTER_PIN` \| `CLAUSE` \| `ASSET_KEY` \| `RENDER_PROFILE` \| `BUNDLE_FORMAT` \| Wave 7 加法 `CLAUSE_NESTING` \| `ASSET_BINARY`）、`severity`（`OK` \| `MISSING` \| `MISMATCH` \| `WILL_MATERIALIZE` \| `INFO`）、`code`（UPPER_SNAKE；资产亦可 `ASSET_WILL_MATERIALIZE` / `ASSET_BINARY_ABSENT`）、`messageKey`、可选非敏感 `detail` |
| 提交拒绝 | `422`；`error.code=IMPORT_DEPENDENCIES_UNSATISFIED`；`error.messageKey=api.error.template.importDependenciesUnsatisfied`；`error.dependencyReport` 同形 |
| 载体 | JSON body（v1/v2 清单）或 **multipart**（规范面：`file`=ZIP + `masterId` + 可选 policy/`dryRun`）。v2 自包含提交须 ZIP（含 `artifacts/master.docx`）；纯 JSON v2 可 dry-run，**不可**作为自包含提交载体；晋级包另可含 `artifacts/assets/{assetKey}` |
| 晋级物化 | 嵌入资产二进制 → dry-run 非阻断 `WILL_MATERIALIZE`；缺键且缺二进制 → blocking；嵌套闭包 incomplete → blocking；提交可事务内物化 CE-E02 资产 + 可选 DRAFT 母版（**禁止**经 pack 置 APPROVED） |
| 审计 | dry-run → `TEMPLATE_IMPORT_DRY_RUN`（含 `readyToCommit`/blockingCount/bundleFormat/actor；无条款全文、无 DOCX 字节、无 secrets） |
| 权限 | 与导入相同（矩阵 §5）；无新权限码 |
| UI | Wave 7：Templates Import 对话框 **Check dependencies** + 仅 `readyToCommit=true` 可 **Import**（P-Q4） |

### 导入冲突策略（`TemplateImportConflictPolicy`）

| 枚举值 | 语义 |
| --- | --- |
| `REJECT_IMPORT` | 默认策略（请求省略 `importConflictPolicy` 时等同此值）。内部 `templateId` 或 `externalId` 与现有模板冲突时拒绝导入。 |
| `KEEP_TEMPLATE_ID` | 仅当 bundle `metadata.templateId`（内部 UUID）已存在时：保留该 UUID，将模板复位为 `DRAFT` 并应用 bundle 内容；不适用于 `externalId` 冲突（`externalId` 冲突始终拒绝）。 |

### 导入请求与成功响应

- JSON 请求体：`masterId`（目标环境母版 UUID）、`bundle`（完整 bundle）、可选 `importConflictPolicy`、可选 `dryRun`。
- Multipart 请求：`masterId`、`file`（ZIP）、可选 `importConflictPolicy`、可选 `dryRun`。
- Dry-run 成功（`200`）：`result.imported=false` + `result.dependencyReport`（无 `importBatchId` 业务写入）。
- 提交成功（`201`）：`result.importSummary` 含 `resolvedTemplateId`、`newDevelopmentVersion`、`importBatchId`，以及 CE-E01 可选扩展 `bundleFormat`、`materializedClauseCount`；`result.template` 为导入后 `DRAFT` 模板详情。

### 校验与错误语义确认

- 模板不在可导出生命周期状态时返回 `422`，`messageKey` 为 `api.error.template.exportNotEligible`。
- 不支持的导出 `format` 查询参数返回 `422`，`messageKey` 为 `api.error.template.exportFormatUnsupported`。
- 导出时钉扎母版 DOCX 对象不可用：`422`/`404` 等价 fail-closed；`error.code=PINNED_MASTER_UNAVAILABLE`，`messageKey=api.error.rendering.pinnedMasterUnavailable`（与 CE-K01/CE-G06 复用，不另造导出专用 key）。**CE-E03：** 全库导出中单模板母版缺失记 manifest `FAILED` / `PINNED_MASTER_UNAVAILABLE`（条目级同一 `messageKey` 语义）；其它模板继续；若仍有 INCLUDED → HTTP 200。
- bundle 格式非 `template-export-bundle-v1-json` 且非 `template-export-bundle-v2-json` 返回 `422`，`messageKey` 为 `api.error.template.importBundleUnsupportedFormat`。
- bundle 结构无效或必需字段缺失返回 `422`，`messageKey` 为 `api.error.template.importBundleInvalid`。
- bundle 含 secret/凭证标记返回 `422`，`messageKey` 为 `api.error.template.importBundleContainsSecrets`。
- 导入冲突（默认策略或 `externalId` 冲突）返回 `422`，`messageKey` 为 `api.error.template.importConflict`。
- 目标母版未批准或组不匹配分别返回 `422`，`messageKey` 为 `api.error.template.masterNotApproved` / `api.error.template.masterGroupMismatch`。
- **CE-E01：** 提交导入时存在 blocking 依赖返回 `422`，`error.code=IMPORT_DEPENDENCIES_UNSATISFIED`，`messageKey=api.error.template.importDependenciesUnsatisfied`（附 `error.dependencyReport`）。
- **CE-E03：** 全库导出合格候选为空（无 INCLUDED）返回 `422`，`messageKey` 为 `api.error.library.exportEmpty`（无 ZIP body）。
- **CE-E03：** `templateIds` 长度或解析后合格候选 > 500 返回 `422`，`messageKey` 为 `api.error.library.exportLimitExceeded`。
- 角色或对象范围越权返回 `403`，`messageKey` 为 `api.error.template.accessDenied`（含无导出权角色调用全库导出）。
- 模板或母版不存在返回 `404`（`api.error.template.notFound` 或母版 not-found 等价错误）。

## 请求语义确认

以下字段名为 v1 请求命名基线。

| 语义字段 | 是否必需 | 说明 | 当前状态 |
| --- | --- | --- | --- |
| 模板标识 | 是 | 标识要调用的模板。 | 只通过路径 `{templateId}` 表达，请求体不得重复传入；编码规则已确认。 |
| 发布版本号 | 显式版本路径必需 | 标识要调用的发布版本，采用语义化版本号。 | 只通过显式版本路径 `{releaseVersion}` 表达；default 路径由 API 管理配置解析目标版本；请求体不得重复传入。 |
| 输出格式 | 是 | DOCX 或 PDF。 | 字段名 `output.format` 和枚举值已确认。 |
| 输出模式 | 是 | 同步文件流、同步下载地址或异步任务。 | 字段名 `output.mode`、模式集合和枚举值已确认。 |
| 业务变量 | 是 | 模板变量和值，用于驱动文档生成。 | 字段名 `variables` 已确认；变量结构由发布版本锁定的变量 Schema 约束。 |
| 批量输入 | 批量时必需 | 多笔生成输入及每笔业务标识。 | 字段名 `items` 和 `items[].itemId` 已确认。 |
| 加密参数 | 可选 | `enabled`、`openPassword`、`ownerPassword`、`permissions`。 | 参数模型、启用语义、密码强度、权限语义和失败处理已确认。 |
| 调用方请求标识 | 是 | 便于幂等、排查和审计关联。 | 字段名 `requestId` 已确认，文档生成类 API 必填已确认。 |

## 请求字段命名确认

以下字段名为 v1 确认基线。

| 语义字段 | 候选字段名 | 建议语义 | 待确认点 |
| --- | --- | --- | --- |
| 模板标识 | `templateId` | 标识被调用模板。 | 仅作为路径参数；请求体不得重复传入；编码规则已确认。 |
| 发布版本号 | `releaseVersion` | 标识被调用发布版本。 | 仅作为显式版本路径参数；请求体不得重复传入；采用语义化版本号已确认。 |
| 输出格式 | `output.format` | 表达 DOCX 或 PDF。 | 字段名和枚举值已确认。 |
| 输出模式 | `output.mode` | 表达同步文件流、同步下载地址或异步任务。 | 字段名和枚举值已确认；API 管理配置可限制可选模式已确认。 |
| 业务变量 | `variables` | 模板变量和值集合。 | 字段名已确认；变量值类型、嵌套对象、数组、空值和默认值语义由发布版本变量 Schema 约束。 |
| 批量输入 | `items` | 批量请求中的每笔生成输入。 | 字段名已确认；最大条数、重复提交、失败项重试和 `itemId` 规则已确认。 |
| 单笔批量标识 | `itemId` | 批量请求中调用方提供的单笔业务标识。 | 必填且同批唯一已确认；响应明细必须回显；审计记录 `itemId` 或其摘要。 |
| 加密参数 | `encryption` | 包含 `enabled`、`openPassword`、`ownerPassword`、`permissions`。 | 外层字段名、启用语义、权限语义、权限枚举、密码强度和失败处理已确认。 |
| 调用方业务请求标识 | `requestId` | 用于排查、审计和上下游业务关联。 | 文档生成类 API 必填已确认。 |
| 幂等标识 | `idempotencyKey` | 用于重复提交识别。 | 文档生成类 API 必填、唯一性范围和过期后行为已确认。 |
| 调用上下文 | `context` | 可放置调用系统业务编号、渠道或追踪信息。 | 字段名、允许字段集合和审计摘要规则已确认。 |

## 单笔请求结构确认

以下结构表达已确认 v1 请求字段分组；正式 Schema 采用 OpenAPI 3.1 YAML 维护。

```text
Generate document request draft
- output
	- format
	- mode
- variables
- encryption
	- enabled
	- openPassword
	- ownerPassword
	- permissions
- requestId
- idempotencyKey
- context
```

确认基线：模板标识和发布版本号只跟随路径表达，请求体不得重复传入；输出格式、输出模式、业务变量、`requestId` 和 `idempotencyKey` 在单笔生成中必需；加密参数仅在调用方需要加密输出时传入，并且必须受 API 管理配置允许。

加密参数确认：`encryption.enabled=true` 时，`openPassword` 必填，`ownerPassword` 可选。`permissions` 采用统一抽象权限枚举；**v1 仅当 `output.format=PDF` 时映射并应用到输出访问权限**（CE-C06 / [ce-c06-docx-permissions-boundary.md](../behavior/ce-c06-docx-permissions-boundary.md)）。DOCX 仍支持 `openPassword` 动态加密，但 **不**将 `permissions` 映射为 DOCX 写保护/权限位；当 `format=DOCX`（非 PDF）且 `permissions` 非空、其余加密参数结构合法时，生成**成功**并在成功路径发出 `DOCX_PERMISSIONS_NOT_APPLIED`（`messageKey=generation.warning.fidelity.docxPermissionsNotApplied`；JSON `fidelityWarnings[]` 或 SYNC_STREAM 保真警告头），**不得**因此返回 `400`。Apache POI DOCX write-protect **不在** CE-C06 范围。传入 `permissions` 时必须同时传入 `ownerPassword`。`encryption.enabled=false` 或未传 `enabled` 时，如果仍传入 `openPassword`、`ownerPassword` 或 `permissions`，返回 `400 ENCRYPTION_PARAMETER_INVALID`，不得静默忽略。

密码强度确认：`openPassword` 和 `ownerPassword` 最少 12 字符、最长 128 字符；如果两者同时传入，二者必须不同。不满足时返回 `400 ENCRYPTION_PARAMETER_INVALID`。

加密权限枚举确认：`permissions` 使用允许类抽象枚举，v1 取值为 `ALLOW_PRINT`、`ALLOW_COPY`、`ALLOW_EDIT`、`ALLOW_ANNOTATE`、`ALLOW_FORM_FILL`。open/view 能力由 `openPassword` 控制，不放入 `permissions` 枚举。`encryptionSummary.permissions` 回显请求侧摘要；DOCX 场景须结合 `DOCX_PERMISSIONS_NOT_APPLIED` 理解「已请求但未应用到文件」。

加密失败确认：加密参数合法但实际加密处理失败时，返回 `500 ENCRYPTION_FAILED`，`retryable=true`；错误响应、日志和审计不得返回密码、内部加密细节或敏感配置值。同步/异步和批量场景沿用已确认的失败承载规则。

请求体字段确认：`templateId` 和 `releaseVersion` 不允许在请求体中重复表达；如果请求体重复传入这些路径字段，按 `400 REQUEST_BODY_INVALID` 处理。

## context 字段白名单确认

确认基线：`context` 用于调用方非敏感追踪/排查信息，以及（IBL-E2）可选的**组合纳入控制轴**；不用于传递模板变量、客户信息或任意生成控制参数。

| 字段名 | 语义 | 约束 |
| --- | --- | --- |
| `sourceSystem` | 调用来源系统。 | 字符串；不得放入 API secret 或内部敏感配置；**不**进入 inclusion 匹配。 |
| `channel` | 调用渠道。 | 字符串；用于渠道统计或排查；**另**作为 Composition Inclusion 匹配轴（ADR-0063）；非 outbound delivery channel（PD-1）。 |
| `businessRequestId` | 调用方业务请求关联标识。 | 字符串；不得直接使用客户姓名、账号、证件号、金额或合同全文；**不**进入 inclusion 匹配。 |
| `upstreamTraceId` | 上游链路追踪标识。 | 字符串；用于跨系统排查；**不**进入 inclusion 匹配。 |
| `scenario` | 调用场景。 | 字符串；用于非敏感场景分类；**不**进入 inclusion 匹配。 |
| `locale` | 调用方期望语种或地区提示。 | 字符串；用于 compute / 语言兼容（ADR-0056 / ADR-0062）；**不**作为 IBL-E2 组合三轴。 |
| `jurisdiction` | 调用方法域/辖区提示（可选）。 | 字符串；IBL-E2 组合轴；trim；空串视为缺失；建议 max 128；大小写不敏感 exact 匹配。 |
| `product` | 调用方产品线/产品码提示（可选）。 | 字符串；IBL-E2 组合轴；约束同 `jurisdiction`。 |
| `legalEntityCode` | 调用方可选不透明上下文字段。 | 字符串；ADR-0013 白名单（历史 IBL-E4 轴）。**ADR-0071 / Wave 6 后：** trim；空串视为缺失；建议 max 64；**不**驱动 DocumentBrand 目录解析；**不**产生退役目录 422；**不**改 UI chrome；**不**进入 inclusion；**不**选包。 |

`context` 未列出的字段按未知字段处理，返回 `400 REQUEST_BODY_INVALID`。`context` 不得包含客户姓名、证件号、账号、金额、密码、模板变量原值、完整请求体、API secret、完整下载地址或完整 AD Group 成员等敏感内容。审计中使用 `contextSummary` 记录必要摘要（非空白三轴与非空白 `legalEntityCode` 同步收录）。

## 批量请求结构确认

以下结构表达已确认 v1 批量请求字段分组；正式 Schema 采用 OpenAPI 3.1 YAML 维护。

```text
Batch generate document request draft
- output
	- format
	- mode
- encryption
	- enabled
	- openPassword
	- ownerPassword
	- permissions
- requestId
- idempotencyKey
- items
	- itemId
	- output
		- format
		- mode
	- encryption
		- enabled
		- openPassword
		- ownerPassword
		- permissions
	- variables
```

确认基线：批量请求默认以同一模板和同一发布版本提交；批量请求支持批次级统一输出和加密配置，也允许单笔记录覆盖输出格式、输出模式和加密参数；每笔输入通过 `items` 区分业务变量、单笔业务标识和单笔覆盖配置。

确认校验顺序：先解析路径确定模板和发布版本，再合并批次级配置与单笔覆盖配置，最后对每笔记录按模板级 API 管理配置校验输出方式、批量上限和动态加密能力。

## Schema 与兼容规则确认

确认基线：

- 正式 API 契约 Schema 采用 OpenAPI 3.1 YAML 维护；Markdown 文档负责解释、索引、决策背景和示例说明。
- OpenAPI 契约应覆盖请求、响应、错误、批量明细、异步任务、下载地址、API 管理配置展示和枚举定义。
- 发布版本锁定的模板变量 Schema 是 `variables` 的校验依据；模板变量 Schema 属于发布版本 API 契约的一部分。
- **IBL-A1：** runtime generate / batch item 与管理 preview 装配在 compute/assemble **之前**对可录入变量执行 fail-closed required/type/enum（及未知 key）校验；失败 → **422** `VARIABLE_VALIDATION_FAILED` + 非空 `fieldErrors[]`，不得静默 blank 或产出成功 DOCX/PDF。详见 [ibl-a1-variable-validation.md](../behavior/ibl-a1-variable-validation.md)。
- **IBL-A2：** 白名单 `FORMAT_AMOUNT` 支持一元 `FORMAT_AMOUNT(value)`（locale 默认币种，CE-K03 兼容）与可选二元 `FORMAT_AMOUNT(value, currencyCode)`（ISO 4217 字母码；数字/分组本地化仍跟 `context.locale`）。第二参**不是** locale 标签。二元形态下缺失/空白/非法币种 → **422** `VARIABLE_COMPUTE_FAILED`（既有码；不静默回退 locale 默认币种）。OpenAPI：`validateComputeExpression` / `evaluateComputeExpression` 及相关 schema description。行为 SoT：[ibl-a2-format-amount-currency.md](../behavior/ibl-a2-format-amount-currency.md)（BDD-IBL-A2-001…010）。**不**翻转 checklist #3b/#5a；**不**宣称 go-live。
- **IBL-A3：** 白名单 `SPELL_AMOUNT` 支持一元 `SPELL_AMOUNT(value)`（**始终** CNY 中文大写，与 `context.locale` 语言无关，CE-K03 / 金标兼容）与可选二元 `SPELL_AMOUNT(value, currencyCode)`（ISO 4217 字母码；拼写语言 = `context.locale` 的 primary language）。本叶至少支持 `(en, USD)` 与 `(zh, CNY)`。第二参**不是** locale 标签。未支持 (language, currency) pair、缺失/空白/非法币种、或 arity ∉ {1,2} → **422** `VARIABLE_COMPUTE_FAILED`（既有码；禁止静默错语言回退）。OpenAPI：同上 compute validate/evaluate schema description。行为 SoT：[ibl-a3-amount-in-words.md](../behavior/ibl-a3-amount-in-words.md)（BDD-IBL-A3-001…012）。**不**翻转 checklist #3b/#5a；**不**宣称 go-live。
- **IBL-A4：** `/contract` 暴露发布版本锁定的逐字段变量 Schema（`callableVersions[].variables` → OpenAPI `ContractVariableSchemaView`）。顶层 `schemas: string[]` 仍为信封类型名索引。字段级契约兼容闸门见下方「消费者契约 breaking-change 闸门（IBL-A4）」。行为 SoT：[ibl-a4-contract-field-schemas.md](../behavior/ibl-a4-contract-field-schemas.md)（BDD-IBL-A4-001…011）。**不**翻转 checklist #3b/#5a；**不**宣称 go-live。
- v1 请求采用严格字段校验，契约 Schema 之外的未知字段返回 `400 REQUEST_BODY_INVALID`，字段级原因使用 `UNKNOWN_FIELD`。
- 模板标识和发布版本号只通过路径表达，请求体重复传入 `templateId` 或 `releaseVersion` 也按未知或不允许字段处理。
- v1 兼容变更应优先采用向后兼容的新增可选字段、枚举扩展或说明增强；破坏性字段重命名、必填字段新增或语义变更需要新的 API 版本或单独兼容策略确认。

### 消费者契约 breaking-change 闸门（IBL-A4）

确认基线（Task Master **#110**；**非** publish API 硬阻断）：

| 项 | 确认规则 |
| --- | --- |
| 闸门形态 | 仓库内 **consumer contract tests**（兼容性分类器 + golden/fixture 基线），随 `mvn -B -ntp -f backend/pom.xml verify`（CI）执行。Publish 路径**不**因本叶新增「禁止 rename」硬检查项。 |
| 比较范围 | 版本级 `variables[]` 语义指纹（`variableKey` / `variableType` / `required` / enum 允许集 / `computed`），**不是**整份 `ContractResponse`（paths / policyVersion 等噪声可忽略）。 |
| **BREAKING → 测试 FAIL** | (a) 删除已有 `variableKey`；(b) rename（旧 key 消失）；(c) `variableType` 变更；(d) `required`: `false` → `true`；(e) `ENUM` 允许集收缩；(f) `computed`: `false` → `true`。 |
| **NON_BREAKING → 测试 PASS** | (a) 新增 `required=false` 且 `computed=false` 字段；(b) `ENUM` 仅增值；(c) 仅 `description` 变化；(d) `required`: `true` → `false`；(e) `piiCategory` 变化（本叶闸门**不**因此失败）。 |
| 非确认 | Pact/第三方 broker 强制；publish 硬阻断 rename；顶层重复 `variableSchemas` 平行字段。 |

OpenAPI：`CallableVersion.variables` / `ContractVariableSchemaView`；示例：[contract-response.json](examples/contract-response.json)。行为 SoT：[ibl-a4-contract-field-schemas.md](../behavior/ibl-a4-contract-field-schemas.md)（BDD-IBL-A4-006…009）。

## 标识编码规则确认

确认基线：

- `templateId` 采用可读稳定模板键，格式为 `TPL-` 前缀 + 英文、数字和连字符，例如 `TPL-LOAN-NOTICE`。
- `templateId` 发布后不可修改；如需更换业务命名，应创建新的模板或通过后续兼容策略确认迁移规则。
- `templateId` 不得包含客户、个人、账号、金额、合同号或其他敏感业务信息。
- `taskId`、`batchId`、`documentId` 由平台生成，分别使用 `TASK-`、`BATCH-`、`DOC-` 前缀。
- `taskId`、`batchId`、`documentId` 的前缀后必须是不透明随机 token，不得编码日期、序号、模板、客户、业务变量或环境信息。
- 日志、审计和 API 响应可以记录这些资源 ID；调用方不得从 ID 推断生成时间、调用规模或业务内容。

## 幂等策略确认

以下为已确认幂等基线。幂等策略的目标是避免调用方在网络超时、重试、异步任务受理结果丢失或批量重复提交时生成重复文档或重复任务。

### 标识分工

| 标识 | 建议用途 | 当前状态 |
| --- | --- | --- |
| `requestId` | 调用方业务追踪标识，用于排查、审计和上下游业务关联。 | 文档生成类 API 必填已确认。 |
| `idempotencyKey` | 幂等标识，用于识别同一调用方对同一生成请求的重复提交。 | 文档生成类 API 必填已确认。 |
| `itemId` | 批量请求内每笔记录的业务标识，用于响应明细、审计明细和失败项定位。 | 必填且同批唯一已确认。 |
| `taskId` | 平台生成的异步任务标识，用于任务查询和结果获取。 | 异步场景已需要返回，查询结果结构已确认。 |

确认原则：文档生成类 API 必须传入 `requestId` 和 `idempotencyKey`。`requestId` 不承担幂等判断；`idempotencyKey` 不替代业务追踪；批量中的 `itemId` 不替代批次级 `idempotencyKey`。

### 适用范围确认

| API 能力 | `idempotencyKey` 要求 | 原因 | 当前状态 |
| --- | --- | --- | --- |
| 单笔同步文件流 | 必填。 | 调用方超时后重试可能重复生成文件。 | 已确认：重复命中时允许重放原文件流结果与响应头核心元数据。 |
| 单笔同步下载地址 | 必填。 | 可返回同一文档或同一下载资源，避免重复生成。 | 已确认：重复命中时优先返回原下载地址；原地址过期且结果仍在保留期内可重新签发。 |
| 单笔异步任务 | 必填。 | 防止重复创建任务。 | 已确认：重复命中响应返回原任务完整状态对象，而不是仅返回 `taskId`。 |
| 批量同步生成 | 必填。 | 防止重复提交整批请求。 | 同步批量非重试失败记录幂等结果已确认。 |
| 批量异步生成 | 必填。 | 防止重复创建批量任务。 | 失败项重试使用新批次和新的 `idempotencyKey` 已确认。 |
| 查询异步任务 | 不适用。 | 查询由 `taskId` 定位，不产生新文档。 | 已确认：接口可选接受 `requestId` 作为附加追踪标识并写入审计。 |
| 获取下载地址文件 | 不适用。 | 下载行为由下载地址安全策略控制。 | 短有效期、二次授权和有效期内多次下载已确认。 |

### 幂等匹配范围确认与实现说明

幂等匹配确认至少包含以下语义：

- 调用方身份，例如 API 凭证或调用方应用。
- 环境。
- 模板标识。
- 路由类型。
- 解析后的发布版本。
- 输出格式和输出模式。
- 请求变量和批量输入摘要。
- 加密策略摘要，但不得包含加密密码明文。
- `idempotencyKey`。

已确认：幂等唯一性范围为调用方、环境、模板和解析后的发布版本。相同唯一性范围内，同一 `idempotencyKey` 对应不同请求语义时返回幂等冲突。

已确认：幂等记录保留 7 天；记录过期后，同一 `idempotencyKey` 可按新请求处理。

default 路径特殊规则：首次请求创建幂等记录时，应记录当时解析出的 `resolvedReleaseVersion`。虽然幂等唯一性范围包含解析后的发布版本，但 default 路径需要额外冲突保护：如果 default 路径目标版本后来变化，重复提交命中同一调用方、环境、模板下的旧幂等记录时应返回幂等冲突错误，不按新的 default 目标版本生成文档。

### 重复提交结果确认与剩余问题

| 场景 | 行为 | 当前状态 |
| --- | --- | --- |
| 相同 `idempotencyKey`、相同请求语义，原请求已成功 | 返回原成功结果、下载信息、任务 ID 或任务状态。 | 已确认：同步文件流允许重放；同步下载地址优先返回原地址，原地址过期且结果仍在保留期内可重新签发。 |
| 相同 `idempotencyKey`、相同请求语义，原请求处理中 | 返回原任务状态对象。 | 已确认：返回完整任务状态对象（含 `taskId` 和当前状态）。 |
| 相同 `idempotencyKey`、相同请求语义，原请求失败 | 仅系统类临时故障且 `retryable=true` 的场景允许自动重执行；其他失败返回原失败结果。 | 已确认。 |
| 相同 `idempotencyKey`、不同请求语义 | 拒绝请求并返回幂等冲突错误；响应可返回安全差异摘要，不返回旧/新请求原始值。 | 安全摘要字段已确认。 |
| 幂等记录已过期 | 按新请求处理；API 响应不提示历史复用信息，仅在审计中标记过期 key 复用。 | 审计标记字段已确认。 |

### 幂等冲突安全摘要确认

确认基线：幂等冲突响应允许返回安全差异摘要，帮助调用方区分请求语义不一致、default 路径目标变更等冲突原因。安全摘要不得返回旧请求或新请求的业务变量原值、加密密码、完整请求体或敏感配置明文。

| 字段 | 说明 | 当前状态 |
| --- | --- | --- |
| `idempotencyStatus` | 幂等处理状态。 | 枚举值已确认为 `IDEMPOTENCY_NEW`、`IDEMPOTENCY_REPLAYED`、`IDEMPOTENCY_CONFLICTED`。 |
| `error.idempotencyConflict.conflictType` | 幂等冲突类型，例如请求语义不一致或 default 路径目标变化。 | 已确认返回。 |
| `error.idempotencyConflict.conflictFields` | 仅列出发生差异的字段名或摘要字段名，例如 `output.format`、`variablesHash`、`itemsHash`。 | 已确认返回；不得返回字段原值。 |
| `error.idempotencyConflict.originalRequestAt` | 原幂等记录的请求受理时间。 | 已确认返回。 |
| `error.idempotencyConflict.originalResolvedReleaseVersion` | 原请求解析后的发布版本；default 路径目标变化时用于帮助调用方识别旧路由结果。 | 已确认返回。 |
| `error.idempotencyConflict.requestHash` | 原请求语义摘要。 | 可返回；不得用于替代审计记录。 |
| `error.idempotencyConflict.variablesHash` | 变量输入摘要。 | 可返回；不得返回变量明文。 |
| `error.idempotencyConflict.itemsHash` | 批量输入摘要。 | 可返回；不得返回单笔明文。 |

确认冲突类型基线：`REQUEST_SEMANTICS_MISMATCH` 表示相同唯一性范围内同一 `idempotencyKey` 对应不同请求语义；`DEFAULT_ROUTE_CHANGED` 表示 default 路径目标变更后重复提交命中旧幂等记录。

### 过期幂等 key 复用审计确认

确认基线：幂等记录过期后复用同一 `idempotencyKey` 按新请求处理，API 响应不提示历史复用信息，仅在审计中标记，便于后续排查与风控分析。

| 审计字段 | 说明 | 当前状态 |
| --- | --- | --- |
| `reusedExpiredIdempotencyKey` | 标记本次请求是否复用了已过期的同一 `idempotencyKey`。 | 已确认。 |
| `previousIdempotencyExpiredAt` | 上一条幂等记录的过期时间。 | 已确认。 |
| `previousRequestAt` | 上一条幂等记录的原请求时间。 | 已确认。 |
| `previousResolvedReleaseVersion` | 上一条幂等记录解析后的发布版本。 | 已确认。 |

审计不得记录旧请求业务变量明文、加密密码、完整请求体或历史生成文档标识作为过期 key 复用标记的一部分。

### 批量幂等确认

- 批次级 `idempotencyKey` 用于识别整批重复提交。
- `items[].itemId` 必填，且必须在同一批次内唯一，用于返回单笔明细、审计和失败项定位。
- 同一批次中重复的 `items[].itemId` 作为整批请求校验错误处理，返回 `400 ITEM_ID_DUPLICATED`，不创建批次或异步任务。
- 批量重复提交命中原批次时，应返回原 `batchId`、汇总结果、任务 ID 或当前任务状态。
- 同步批量中任一记录因参数校验或 API 管理策略失败时，整批失败且不生成任何文件；响应需要返回每笔失败明细，并按非重试幂等结果记录，重复提交同一 `idempotencyKey` 时重放该失败结果。
- 异步批量部分成功后的失败项重试使用新批次和新的 `idempotencyKey`；新批次只提交需要重试的失败项，并通过 `originalBatchId` 或等效关联字段关联原批次，原批次结果不被扩展或改写。
- **`originalBatchId` 运行时校验（CE-C05，2026-07-15）：** 可选。出现时必须在当前 API 凭证下存在匹配的原 `BATCH_ROOT`；否则 `404 ORIGINAL_BATCH_NOT_FOUND`（`category=BATCH`，`messageKey=api.error.batch.originalBatchNotFound`，`retryable=false`）。格式不符 pattern → `400 REQUEST_BODY_INVALID`（`fieldErrors` → `originalBatchId`）。成功时 `result.batch.originalBatchId` 回显；新批次审计/调用记录必须持久化关联。详见 [ce-c05-original-batch-id.md](../behavior/ce-c05-original-batch-id.md)。

### 幂等响应与审计字段状态

| 语义字段 | 候选字段名 | 建议语义 | 当前状态 |
| --- | --- | --- | --- |
| 幂等标识 | `idempotencyKey` | 回显本次请求使用的幂等标识。 | 已确认：生成类成功和错误响应统一回显；同步文件流通过响应头回显。 |
| 幂等处理状态 | `idempotencyStatus` | 表达新请求、重复命中、冲突等状态。 | 枚举值已确认为 `IDEMPOTENCY_NEW`、`IDEMPOTENCY_REPLAYED`、`IDEMPOTENCY_CONFLICTED`。 |
| 原始请求时间 | `originalRequestAt` | 重复命中或幂等冲突安全摘要中表达原请求受理时间。 | 已确认：重复命中成功场景固定返回。 |
| 原始任务标识 | `task.taskId` | 重复命中异步请求时返回原任务。 | 已确认：重复命中异步请求返回完整 `task` 对象。 |
| 原始批次标识 | `batchId` | 重复命中批量请求时返回原批次。 | 已确认。 |
| 失败项重试原批次标识 | `originalBatchId` | 异步批量失败项以新批次重试时关联原批次；出现时须同凭证下存在原 `BATCH_ROOT`，否则 `404 ORIGINAL_BATCH_NOT_FOUND`；成功响应回显并写入审计。 | **CE-C05 确认**：字段名、校验、回显、审计关联已锁定（[BDD](../behavior/ce-c05-original-batch-id.md)）。 |

审计建议记录 `requestId`、`idempotencyKey` 或其摘要、幂等处理状态、原始请求时间、是否重复命中、是否冲突、冲突原因、解析后的发布版本和请求参数摘要。批量调用还需要记录 `batchId`、`items[].itemId` 或其摘要、失败项重试关联的 `originalBatchId` 或等效关联字段。过期 `idempotencyKey` 复用时，审计记录需要包含 `reusedExpiredIdempotencyKey`、`previousIdempotencyExpiredAt`、`previousRequestAt`、`previousResolvedReleaseVersion`。幂等响应字段统一放入 `metadata`。审计摘要不得包含 API 传入的 DOCX/PDF 加密密码。

## 响应语义确认

| 场景 | 响应应表达 | 当前状态 |
| --- | --- | --- |
| 同步文件流成功 | 文件内容、响应头核心元数据、输出格式、生成文件标识、审计关联标识、保真警告数量和警告码摘要。 | 文件流体和响应头承载方式已确认；完整保真警告明细进入审计摘要。 |
| 同步下载地址成功 | 下载地址、有效期、生成文件标识、审计关联标识、保真警告明细。 | 下载地址安全策略、15 分钟固定有效期和 `result.fidelityWarnings[]` 已确认。 |
| 异步任务受理 | 任务 ID、初始任务状态、查询方式、审计关联标识。 | 返回任务 ID 和 HTTP 202 Accepted 已确认；生成完成后的任务查询结果返回保真警告明细。 |
| 同步批量成功 | 全部生成成功的文件信息或下载信息、单笔保真警告明细。 | 同步批量全部成功或全部失败已确认，正式 OpenAPI v1 以 `BatchResponse` 表达；成功项可返回 `items[].fidelityWarnings[]`。 |
| 同步批量失败 | 整批失败原因、失败项摘要、是否可重试。 | 整批失败、每笔失败明细、非重试幂等记录、响应 envelope 和字段命名已确认。 |
| 异步批量完成 | 每笔成功/失败明细、总数、成功数、失败数、成功项保真警告明细。 | 异步批量允许部分成功已确认；部分成功查询结果返回 HTTP 200 OK，通过 `result.batch.items[].status` 和 `result.batch.items[].error` 表达单笔失败；成功项可返回 `items[].fidelityWarnings[]`。 |
| 参数或权限失败 | 稳定错误码、业务可读消息、可排查关联标识。 | 错误类别和 HTTP 状态码映射已确认。 |

## 通用响应元数据确认

以下字段名为 v1 响应命名基线。

确认基线：除纯文件流本身外，API 响应需要提供可排查的通用响应元数据；同步文件流场景通过响应头表达核心元数据，文件响应体只承载文件内容。

| 语义字段 | 推荐字段名 | 语义说明 | 状态说明 |
| --- | --- | --- | --- |
| 审计关联标识 | `auditId` | 用于关联 API 调用审计记录。 | 对 API 调用方可见已确认。 |
| 追踪标识 | `traceId` | 用于跨系统排查。 | 平台必须返回；调用方传入 `X-Trace-Id` 时沿用该值，未传入时由平台生成。 |
| 调用方请求标识 | `requestId` | 回显调用方业务追踪标识。 | 文档生成类 API 必填已确认。 |
| 幂等标识 | `idempotencyKey` | 回显生成请求幂等标识。 | 生成类 API 必填已确认。 |
| 模板标识 | `templateId` | 回显被调用模板。 | 所有生成类 JSON 响应和文件流响应头均返回；编码规则已确认。 |
| 请求路由类型 | `routeType` | 表达显式发布版本路径或 default 路径。 | 字段名和枚举值已确认。 |
| 解析后发布版本 | `resolvedReleaseVersion` | default 路径解析后的实际发布版本；显式路径时与路径版本一致。 | 字段名和所有生成类响应均返回已确认。 |
| 幂等处理状态 | `idempotencyStatus` | 表达新请求、重复命中或幂等冲突。 | 放入 `metadata`，枚举值已确认。 |
| 原始请求时间 | `originalRequestAt` | 重复命中或幂等冲突安全摘要中表达原请求受理时间。 | 放入 `metadata`；采用 ISO 8601 带时区偏移格式已确认。 |
| 输出格式 | `output.format` | DOCX 或 PDF。 | 字段名和枚举值已确认。 |
| 输出模式 | `output.mode` | 同步文件流、同步下载地址或异步任务。 | 字段名和枚举值已确认。 |
| 生成文件标识 | `documentId` | 标识生成文档结果。 | 暴露给调用方并用于下载或追踪已确认。 |

## 保真警告响应确认

确认基线：DOCX/PDF 保真警告只表达不直接破坏文档语义或合规结果的低风险问题。阻断项仍通过发布门禁或错误模型处理，不以警告形式降级。

已发布模板运行期生成对外只返回生成成功警告或生成失败错误。API 契约和响应不得暴露内部渲染诊断明文、模板变量原值、客户数据、完整请求体或完整生成内容。

### 承载分流（JSON 全量对象 vs SYNC_STREAM 头摘要）— CE-C03

| 路径 | 保真警告承载 | 说明 |
| --- | --- | --- |
| JSON 批量成功项 | `result.batch.items[].fidelityWarnings[]` 为完整 `FidelityWarning` **对象**数组 | 禁止 `string[]` 仅警告码；无警告时为 `[]` |
| 异步任务查询完成态 | 与批量相同：成功项在 `result.batch.items[].fidelityWarnings[]`；若单笔结果层暴露 `result.fidelityWarnings[]`，同样为完整对象数组 | 形态与 OpenAPI `TaskResponse` / `BatchResultItem` 一致 |
| `SYNC_DOWNLOAD_URL`（契约声明） | `result.fidelityWarnings[]` 为完整对象数组 | 运行时重签下载仍 defer（ADR-0038）；schema 形态保持一致 |
| **`SYNC_STREAM` 同步文件流** | **响应体 = 文件字节 only**；响应头 `fidelityWarningCount` + `fidelityWarningCodes`（逗号分隔码） | **不**把完整 `FidelityWarning[]` 写入流响应体；完整非敏感明细进入**审计摘要** |

对照 OpenAPI：`components.schemas.FidelityWarning` / `BatchResultItem.fidelityWarnings`（JSON 全量）与 `components.headers.FidelityWarningCount` / `FidelityWarningCodes`（流头摘要）。头中的码集合与同次生成若走 JSON 路径时的 `warningCode` 集合一致；`fidelityWarningCount` = 警告条数。

`fidelityWarnings[]` 中每个对象必须包含：

- `warningCode`：稳定警告码。v1 调用方可见枚举以 OpenAPI `FidelityWarningCode` 为准（基线 ADR-0019 五码：`OPTIONAL_CONTENT_EMPTY`、`LOW_RISK_PAGINATION_DIFFERENCE`、`LOW_RISK_TABLE_PAGE_BREAK`、`CONTROLLED_STYLE_FALLBACK`、`IMAGE_SCALING_ADJUSTED`；另含运行时成功路径可发出的引擎码，如 `MASTER_STYLE_FALLBACK`、`PDF_PAGE_NUMBER_STAMP_FAILED`、`DOCX_PERMISSIONS_NOT_APPLIED`（CE-C06）等 — 见 OpenAPI 枚举，诚实契约）。
- `messageKey`：用于本地化和前端展示的稳定文案键。CE-C06：`DOCX_PERMISSIONS_NOT_APPLIED` → `generation.warning.fidelity.docxPermissionsNotApplied`。
- `message`：默认可读提示。
- `locationSummary`：影响位置摘要，例如锚点、章节、组件或页码范围摘要，不返回敏感正文。
- `detectedSummary`：检测结果摘要，不返回变量原值、客户数据、粘贴原文或完整生成内容。
- `recommendation`：处理建议。
- `sensitiveDataExcluded`：固定为 `true`，表达该警告明细已经过敏感数据排除处理。

失败批量项（`status` ∈ {`FAILED`,`SKIPPED`}）以 `error` 为准；`fidelityWarnings` 可省略或为 `[]`，不得回退为字符串码列表。

单笔 JSON 成功响应在 `result.fidelityWarnings[]` 返回保真警告；批量成功项在 `result.batch.items[].fidelityWarnings[]` 返回保真警告；异步任务查询在生成完成后按单笔或批量结果层级返回保真警告。同步文件流响应使用 `fidelityWarningCount` 和 `fidelityWarningCodes` 响应头返回摘要，完整明细进入审计摘要。

同步文件流响应头确认字段：`auditId`、`traceId`、`requestId`、`idempotencyKey`、`idempotencyStatus`、`documentId`、`templateId`、`routeType`、`resolvedReleaseVersion`、`output.format`、`output.mode`、`fidelityWarningCount` 和 `fidelityWarningCodes`。

后台 API 契约页提供调用方视图，展示授权模板的契约版本对比、错误码说明、调用示例、可调用版本列表、API 策略摘要、调用方自身 API 凭证非敏感状态、保真警告码目录、字段含义、JSON 示例、同步文件流响应头说明，以及授权范围内的非敏感调用结果警告摘要和 `traceId` 或 `auditId` 定位标识。契约版本对比由页面基于已授权可见的现有契约信息、可调用版本、请求 Schema、响应 Schema、错误码、API 策略、路由/default 目标和示例计算展示；v1 不在 `ContractResponse` 中新增专门的契约版本对比字段。v1 不建设独立开发者门户；该视图不展示完整审计明细、完整请求体、模板变量原值、客户数据、完整生成内容或 API 凭证 secret，也不提供 API 凭证自助管理。

## P22 演示扩展与渲染保真 — API 影响（2026-07-03）

**调用方（动态 API v1）：无契约变更。** P22 不新增或修改 OpenAPI 请求/响应字段；`generate`、`batch-generate`、异步任务查询与同步文件流响应头语义保持不变。演示模板运行期仍通过既有路径返回 `result.fidelityWarnings[]`（或响应头摘要）；干净演示绑定不得再依赖虚假 `CONTROLLED_STYLE_FALLBACK` stub（实现侧 P22-T01/T15）。

**发布锁定 `renderProfile`（内部，非 OpenAPI 暴露）：** 实现可将 `pdfPageNumberStampingEnabled` 纳入 `template_version.render_profile_json`（与 `renderProfileVersion` 一并发布锁定）。该字段控制 PDF 页码加盖路径，**调用方不可通过请求体覆盖**；与既有 `CallerRenderOverride` 忽略规则一致。默认 profile（`rp-v1`）在 P22 实现时按需扩展；不要求 bump `renderProfileVersion` 除非发布语义变化需新版本号。

**`pageNumberingProfile`：** 仅存在于 `deploy/demo-*/config/*-template-config.json` 演示导入配置，驱动母版资产生成与导入幂等；**不是** runtime API 字段，也不进入 `renderProfile` JSON schema 的调用方可见契约。

**管理面：** 无新增管理 API 路由。演示导入通过 PowerShell 脚本 + 既有 Management API（母版上传、模板创建等）完成；权限沿用现有角色与组范围（见 [permission-matrix.md](../security/permission-matrix.md)）。

## 响应字段命名确认

以下字段名为 v1 响应命名基线。

| 语义字段 | 推荐字段名 | 推荐理由 | 当前状态 |
| --- | --- | --- | --- |
| 审计关联标识 | `auditId` | 直接对应审计记录。 | 对 API 调用方可见已确认。 |
| 追踪标识 | `traceId` | 便于跨系统排查。 | 平台必须返回；调用方传入 `X-Trace-Id` 时沿用该值，未传入时由平台生成。 |
| 调用方业务请求标识 | `requestId` | 表达调用方业务侧请求标识。 | 文档生成类 API 必填已确认。 |
| 幂等标识 | `idempotencyKey` | 与业务请求标识分离，用于重复提交处理。 | 文档生成类 API 必填，保留 7 天；唯一性范围和过期后行为已确认。 |
| 模板标识 | `templateId` | 与请求路径语义保持一致。 | 编码规则已确认。 |
| 路由类型 | `routeType` | 表达显式版本路径或 default 路径。 | 枚举值已确认。 |
| 解析后发布版本 | `resolvedReleaseVersion` | 明确 default 路径解析结果。 | 显式版本路径也返回已确认。 |
| 幂等处理状态 | `idempotencyStatus` | 表达首次受理、重复命中或冲突。 | 放入 `metadata`，枚举值 `IDEMPOTENCY_NEW`、`IDEMPOTENCY_REPLAYED`、`IDEMPOTENCY_CONFLICTED` 已确认。 |
| 原始请求时间 | `originalRequestAt` | 重复命中时辅助调用方判断返回结果来源。 | 采用 ISO 8601 带时区偏移格式。 |
| 输出格式 | `output.format` | 与请求输出结构一致。 | 枚举值已确认。 |
| 输出模式 | `output.mode` | 与请求输出结构一致。 | 枚举值已确认。 |
| 生成文档标识 | `documentId` | 比 `fileId` 更贴近文档生成业务语义。 | 对调用方只暴露 `documentId` 已确认。 |
| 下载地址 | `download.url` | 与下载结果分组。 | API 响应返回可用地址；日志、审计、管理界面和契约示例脱敏展示。 |
| 下载过期时间 | `download.expiresAt` | 清楚表达时间点。 | 字段和 ISO 8601 带时区偏移格式已确认。 |
| 是否一次性下载 | `download.oneTime` | 表达下载地址使用策略。 | 字段已确认；当前 v1 固定为 `false`。 |
| 异步任务标识 | `task.taskId` | 与任务分组。 | ID 编码规则已确认。 |
| 异步任务状态 | `task.status` | 与任务状态枚举关联。 | 状态集合和字段承载方式已确认。 |
| 异步任务查询路径 | `task.queryPath` | 表达查询任务状态的相对路径入口。 | 已确认；不是免认证或签名地址，后续查询仍需 API 凭证、AD Group 和模板级授权。 |

## 单笔响应结构确认

以下结构表达已确认响应分组；正式 Schema 采用 OpenAPI 3.1 YAML 维护。

确认基线：单笔响应按输出模式分为同步文件流、同步下载地址和异步任务受理三类；三类响应都需要能关联审计记录、调用方请求标识、模板、路由类型和解析后的发布版本。

```text
Synchronous file stream response draft
- response headers
	- auditId
	- traceId
	- requestId
	- idempotencyStatus
	- documentId
	- templateId
	- routeType
	- resolvedReleaseVersion
	- output
		- format
		- mode
- file stream body
```

```text
Synchronous download URL response draft
- metadata
	- auditId
	- traceId
	- requestId
	- idempotencyKey
	- idempotencyStatus
	- originalRequestAt
	- templateId
	- routeType
	- resolvedReleaseVersion
	- output
		- format
		- mode
- result
	- download
		- url
		- expiresAt
		- requiresAuthorization
		- oneTime
	- documentId
```

```text
Asynchronous accepted response draft
- metadata
	- auditId
	- traceId
	- requestId
	- idempotencyKey
	- idempotencyStatus
	- originalRequestAt
	- templateId
	- routeType
	- resolvedReleaseVersion
- result
	- task
		- taskId
		- status
		- queryPath
```

确认基线：JSON 响应采用统一 `metadata`、`result`、`error` envelope；同步文件流响应头承载核心元数据；下载地址固定 15 分钟有效、需要二次授权、有效期内允许多次下载、不可配置为一次性、过期后不重新签发；异步任务查询返回状态、元数据、结果或错误明细。

确认基线：异步任务受理响应返回 `task.queryPath`，取值为 `/api/{environment}/v1/templates/{templateId}/tasks/{taskId}` 形式的相对路径；该字段仅方便调用方发现查询入口，不授予额外访问能力。

## 下载地址安全确认

确认基线：同步下载地址和异步结果下载地址固定有效期为 15 分钟，不允许通过 API 管理配置调整有效期；下载时必须二次授权，且有效期内允许多次下载。下载、任务和重复命中相关时间字段采用 ISO 8601 带时区偏移格式。

已确认下载路径为 `/api/{environment}/v1/documents/{documentId}/download`。下载路径以生成文档为资源，下载时仍必须通过 `documentId` 关联模板并执行模板级二次授权。

| 安全语义 | 确认规则 | 当前状态或待确认点 |
| --- | --- | --- |
| 有效期 | 下载地址固定有效期为 15 分钟，不允许通过 API 管理配置覆盖为更短或更长。 | 时间格式已确认。 |
| 二次授权 | 下载时校验 API 凭证、AD Group、模板级授权、下载地址有效期和结果有效性。 | AD Group 解析失败、缓存和权限变更生效策略已确认。 |
| 发布版本状态 | 下载时不重新校验发布版本是否仍可调用；已生成结果不会因为发布版本后续停用或 default 路径目标变更而在下载阶段失效。 | 无。 |
| 下载次数 | 15 分钟有效期内允许多次下载，不允许 API 管理配置覆盖为一次性下载。 | 无。 |
| 返回字段 | 下载地址响应需要返回 `download.expiresAt` 和 `download.oneTime`；当前 v1 策略下 `download.oneTime` 固定为 `false`。 | 时间格式已确认。 |
| 过期后处理 | 下载地址取文件接口在地址过期后不重新签发；相同 `idempotencyKey` 重复命中同步下载地址成功结果时优先返回原下载地址，若原地址过期且结果仍在保留期内可在重复命中响应中重新签发。 | 已确认。 |
| 结果保留 | 异步任务和生成结果默认保留 7 天；结果保留不等同于下载地址可重新签发。 | 清理前不主动通知调用方或管理员，仅记录清理审计。 |
| 敏感信息 | 下载地址和审计不得包含 API 传入的 DOCX/PDF 加密密码。 | API 响应返回可用下载地址；日志、审计、管理界面和契约示例必须脱敏展示下载地址。 |

时间格式确认：`download.expiresAt`、`task.acceptedAt`、`task.updatedAt`、`task.expiresAt`、`originalRequestAt` 等 API 时间字段采用 ISO 8601 带时区偏移格式，例如 `2026-06-03T16:30:00+08:00`；不得返回无时区本地时间或纯 Unix 时间戳。

下载地址展示确认：`download.url` 在 API 响应中返回对当前授权调用方可用的地址；日志、审计、管理界面和契约示例展示下载地址时必须脱敏，不展示完整可用地址。

结果清理确认：生成结果 7 天到期清理前不主动通知调用方或管理员，仅记录清理审计（**幂等/异步任务默认窗口**；包级 `saveGeneratedDocuments=true` 时 artifact 按 `documentRetentionDays` 独立清理，见 ADR-0040）。

## 调用记录查询接口确认

确认基线（2026-07-03，BDD-API-PACKAGE-ACCESS-INVOCATION-001，ADR-0040）：调用记录是 **产品化查询/备份** 能力，与合规审计摘要并存。调用方仅可查询 **本 API 凭证** 在授权模板下的记录；管理端包 Hub 只读摘要不含 variables 明文。

### 路径与查询参数

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/{environment}/v1/templates/{templateId}/invocations` | 分页列表 |
| GET | `/api/{environment}/v1/templates/{templateId}/invocations/{invocationId}` | 单条详情 |

| 查询参数 | 必填 | 说明 |
| --- | --- | --- |
| `view` | 否 | `logical`（默认）或 `flat` |
| `requestId` | 否 | 按调用方 `requestId` 过滤；不参与幂等定位 |
| `page` / `size` | 否 | 分页（具体默认值在 OpenAPI schema 中定义） |

### 视图语义

| `view` | 包含 `invocationKind` | 用途 |
| --- | --- | --- |
| `logical` | `SINGLE`、`BATCH_ROOT`、`ASYNC_TASK` | 批次聚合视图；ROOT 可内嵌或链接子 item 摘要 |
| `flat` | `SINGLE`、`BATCH_ITEM` | 平铺对账；**不含** `BATCH_ROOT` |

### 标识与关联字段

- `invocationId`：`INV-` 前缀 + 不透明 token。
- 关联：`requestId`、`idempotencyKey`、`batchId`、`taskId`（async）、`parentInvocationId`（batch item）、`items[].itemId`（batch item 回显）。
- `artifactSaved`：是否保存了生成文档；`false` 时 download 始终 410。
- `documentExpiresAt` / `recordExpiresAt`：artifact 与记录到期时间（ISO 8601 带时区）。

### 参数存储与详情

- 详情 `parameters` 返回调用时 sanitized JSON（variables、output、encryption 元数据等）。
- **IBL-A5 / ADR-0057 Amendment：** 持久化与调用方详情中的 `variables` **不得**含 `piiCategory ≠ NONE` 或未知 key 的调用方明文；`NONE` 字段可保留明文；可选 `redactedVariableKeys`（仅键名）。行为：[ibl-a5-pii-retention-redaction.md](../behavior/ibl-a5-pii-retention-redaction.md)。
- **禁止** 持久化或返回 `openPassword` / `ownerPassword`；`encryption.enabled` 与 permissions 摘要可返回。
- `IDEMPOTENCY_REPLAYED` **不**新建 invocation；列表/详情通过原记录的 `idempotencyKey` 或 `requestId` 解析。

### 授权与错误

- 授权：API 凭证 + AD Group + 模板级授权；记录范围 **credential-scoped**。
- 跨 credential 访问已知 `invocationId` → `403 ACCESS_DENIED`。
- 记录或 artifact 已过期 → 列表不可见或详情 404；artifact 过期但记录未过期 → 详情可查参数，download → `410`.

### 与四层时钟关系

| 层 | 默认 | 可配 |
| --- | --- | --- |
| 下载 URL | 15 分钟 | 否 |
| 幂等记录 | 7 天 | 否 |
| 文档 artifact | 30 天（save=true） | 包级 `documentRetentionDays`（max 365） |
| 调用记录 | 90 天 | 包级 `invocationRecordRetentionDays`（max 2555） |

包级留存变更仅影响 **新产生** 的记录 TTL；`changedAreas` 含 `INVOCATION_RETENTION`。

### 管理端包 Hub 只读摘要（2026-07-08，P13-EXTERNAL-SERVICES-EXCELLENCE）

管理端 **不含** 调用方 `variables` 明文；与 runtime 调用方 invocation API 分离。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/management/v1/templates/{templateId}/api/routes-summary` | 包 externalId、默认 generate 路径、explicit 路径列表 |
| GET | `/api/management/v1/templates/{templateId}/api/invocations` | 分页列表；筛选 `status`、`invocationKind`、`requestId`、`createdAfter`/`createdBefore`、`credentialId`；摘要可含 `releaseBundleSnapshotId`/`releaseBundleHash`（CE-G06，可空） |
| GET | `/api/management/v1/templates/{templateId}/api/invocations/{invocationId}` | 单条摘要详情（无 parameters；CE-G06 可含指纹字段） |
| POST | `/api/management/v1/templates/{templateId}/api/invocations/{invocationId}/regenerate` | CE-G06 受控再生 SPECIMEN（默认）；PD-6 显式生产重发可选；见下方专节 |
| GET | `/api/management/v1/api-access/alerts` | 跨包待关注项（缺 AD 组含 PENDING_RELEASE；凭证类仍仅 PUBLISHED） |
| GET | `/api/management/v1/api-access/summary` | Overview 轻量就绪计数（published / attention / pending-setup；非 catalog） |

### 审计可复现受控再生（CE-G06）+ 生产重发（PD-6）

管理面契约（**不**扩展 caller-facing runtime generate 路径语义；正式 runtime **不**施加 SPECIMEN）。权威行为：默认样件 [ce-g06-audit-reproducible.md](../behavior/ce-g06-audit-reproducible.md)；生产重发 opt-in [pd6-true-non-specimen-reissue.md](../behavior/pd6-true-non-specimen-reissue.md)。上游钉扎：[ce-k01-release-bundle-pinning.md](../behavior/ce-k01-release-bundle-pinning.md)；水印复用：[ce-g02-specimen-watermark.md](../behavior/ce-g02-specimen-watermark.md)。**无新 ADR**（水印策略为产品行为扩展，非栈决策）。

#### 指纹落库（runtime write path）

| 字段 | 语义 |
| --- | --- |
| `release_bundle_snapshot_id` / `releaseBundleSnapshotId` | 生成时解析到的 PUBLISHED `template_version.id` |
| `release_bundle_hash` / `releaseBundleHash` | 该行 `master_file_hash` 拷贝（64 字符小写 hex SHA-256）；落库时**不**重算对象字节 |

写入对象：`SINGLE` / `BATCH_ITEM` / `ASYNC_TASK`（有解析 release 时非空）。`BATCH_ROOT` **不要求**指纹。解析失败行两字段 **NULL**。**不**回填历史行。

#### Regenerate API

| 项 | 已确认 |
| --- | --- |
| 路径 | `POST /api/management/v1/templates/{templateId}/api/invocations/{invocationId}/regenerate`（**同一入口**；不新建平行 formal-reissue 端点） |
| Body | 可选 `{ "outputFormat": "DOCX" \| "PDF", "productionReissue"?: boolean, "reason"?: string }`；`outputFormat` 缺省 = 原 invocation `output_format`，若空则 `PDF` |
| 默认模式（审计样件） | `productionReissue` 缺省或 `false` → CE-G06：强制 SPECIMEN；`specimen=true` |
| 生产重发模式（PD-6） | `productionReissue=true` **且** `reason` trim 后非空（建议 max **500**；超长 → 400 validation）→ **跳过** SPECIMEN stamper；`specimen=false` |
| 授权（默认样件） | `GLOBAL_ADMIN`；同组 `GROUP_ADMIN`；模板可见范围内 `AUDIT_ADMIN`（`readAudit` + 组范围）。其他角色 403。跨组探测对齐既有 403/404 惯例 |
| 授权（生产重发） | **仅** `GLOBAL_ADMIN` / 同组 `GROUP_ADMIN`。`AUDIT_ADMIN` + `productionReissue=true` → **403**。无新 capability bit |
| 可再生 kind | 仅 `SINGLE` / `BATCH_ITEM` / `ASYNC_TASK` |
| Drift / 过期 / 指纹 | 与 CE-G06 相同闸门；生产重发**不**放宽 |
| 水印 | 默认：复用 CE-G02 DOCX 眉脚 + PDF 对角 `SPECIMEN`；失败 fail-closed，不落无水印成功件。生产重发：**不** stamp；**不得**因「未 stamp」触发 `SPECIMEN_WATERMARK_FAILED` |
| 加密 | 再生件一律不加密；`encryptionReapplied=false` |
| 审计 | 终态必写 `INVOCATION_REGENERATED`（成功/失败）；摘要须可区分模式：至少 `productionReissue`、`specimen`、`reason`（生产重发时非空；样件模式可空）+ 既有 source/regeneration/outcome/actor；**禁止** variables / 密码明文 |
| 参数留存 | 内部重放读 `parameters_storage`（IBL-A5 脱敏后形态：非脱敏字段可重放；脱敏字段按未提供）；授权依据 [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md) Amendment 2026-07-18；管理端响应仍禁 variables；**不**因 PII 脱敏本身拒绝再生 |
| 边界 | **不**新建调用方 runtime SUCCESS 记录；**不**消耗调用方幂等键；**不**要求 regenerate `idempotencyKey`；**不**把 `productionReissue` 接到 preview / test-generate |
| FE | 再生 / 生产重发 CTA / E2E/UIUX **out of scope**（`frontend_ui_in_scope=false`） |
| 过期 | `record_expires_at` 已过 → **410**（契约钉死；与 BDD Q2 默认一致） |
| Go-live 护栏 | **不**翻转 checklist **#3b** / **#5a**；**不**宣称 IBL program Done |

成功 `result` 至少含：`regenerationId`、`sourceInvocationId`、`releaseBundleSnapshotId`、`releaseBundleHash`、`outputFormat`、`specimen`（默认 `true`；生产重发 `false`）、`encryptionReapplied=false`、artifact 引用（`downloadUrl` 和/或 `artifactPath`）。

#### Fail-closed messageKeys（English-first；management regenerate surface）

| Condition | HTTP | category | `error.code` | messageKey（稳定） | English default |
| --- | --- | --- | --- | --- | --- |
| 指纹缺失 / 历史未记录 | 409 | `GENERATION` | `RELEASE_BUNDLE_SNAPSHOT_UNAVAILABLE` | `api.error.audit.releaseBundleSnapshotUnavailable` | Release-bundle snapshot is not available for this invocation. |
| Bundle hash drift | 409 | `GENERATION` | `RELEASE_BUNDLE_HASH_MISMATCH` | `api.error.audit.releaseBundleHashMismatch` | Release-bundle hash does not match the pinned master object. |
| 钉扎母版不可用 | 422/500 对齐 K01 运行时语义 | `RENDERING` | `PINNED_MASTER_UNAVAILABLE` | `api.error.rendering.pinnedMasterUnavailable` | Pinned master revision is unavailable. |
| `BATCH_ROOT` 等不可再生 kind | 422 | `VALIDATION` | `INVOCATION_KIND_NOT_REGENERABLE` | `api.error.audit.invocationKindNotRegenerable` | This invocation kind cannot be regenerated; use a SINGLE, BATCH_ITEM, or ASYNC_TASK record. |
| 记录过期 / 已清理 | 410 | `API_POLICY` | `INVOCATION_RECORD_EXPIRED` | `api.error.audit.invocationRecordExpired` | Invocation record has expired. |
| SPECIMEN 水印失败（**仅样件路径**） | 500 | `GENERATION` | `SPECIMEN_WATERMARK_FAILED` | `api.error.audit.specimenWatermarkFailed` | SPECIMEN watermark could not be applied. |
| 生产重发缺 / 空白 reason | 400 | `VALIDATION` | `PRODUCTION_REISSUE_REASON_REQUIRED` | `api.error.audit.productionReissueReasonRequired` | A non-blank reason is required for production re-issue. |
| 生产重发角色不足（含 `AUDIT_ADMIN`） | 403 | `AUTHORIZATION` | （既有 forbidden） | `api.error.authorization.forbidden`（或管理端等价） | （既有英文） |

说明：`api.error.audit.*` 为 **management** messageKey 命名空间（对齐 `api.error.template.*` / `api.error.master.*`）；envelope `error.category` 仍取固定 11 类之一，**不**新增 `AUDIT` 类别。钉扎母版键 **复用 K01**（G06-C12 契约钉死，不用并行 `api.error.audit.pinnedMasterUnavailable`）。

## 异步任务查询与取消接口确认

确认基线：异步任务查询需要返回任务状态、响应元数据、成功结果或统一错误明细；异步批量任务需要返回批次汇总和单笔成功/失败明细。异步任务和生成结果默认保留 7 天（**幂等/异步任务默认窗口**；包级 artifact/record retention 见 ADR-0040）。

确认基线：异步任务查询接口可选接受 `requestId` 作为附加追踪标识；该字段不参与 `taskId` 定位或幂等判断，仅用于审计关联与排查。

已确认异步任务查询路径为 `/api/{environment}/v1/templates/{templateId}/tasks/{taskId}`，任务查询挂在模板下以便执行模板级授权。

已确认异步任务取消路径为 `POST /api/{environment}/v1/templates/{templateId}/tasks/{taskId}/cancel`，任务取消执行与任务查询相同的 API 凭证、AD Group 和模板级授权校验。

受控取消规则：

- 仅 `ACCEPTED` 或 `PROCESSING` 状态且未过期的任务可取消。
- 已完成、已失败、已部分成功、已过期、已取消或其他不可取消状态再次取消时返回 `409 ASYNC_TASK_CANCELLATION_NOT_ALLOWED`。
- 取消成功后的最终状态为 `CANCELLED`。
- 取消后的任务不返回已生成结果、下载地址或异步批量单笔成功结果，即使取消前已有部分单笔生成完成。
- 取消操作必须记录审计。

进度表达规则：

- 异步任务查询不返回 `progressPercent` 或其他百分比字段。
- 单笔异步任务通过 `task.status`、`task.acceptedAt`、`task.updatedAt` 和 `task.expiresAt` 表达进展。
- 异步批量任务通过 `batch.summary` 返回总数、已处理数、成功数、失败数和跳过数等进度摘要。

```text
Async task query response structure
- metadata
	- auditId
	- traceId
	- requestId
	- templateId
	- routeType
	- resolvedReleaseVersion
- result
	- task
		- taskId
		- status
		- acceptedAt
		- updatedAt
		- expiresAt
	- download
		- url
		- expiresAt
		- requiresAuthorization
		- oneTime
	- documentId
	- output
		- format
		- mode
	- batch
		- batchId
		- summary
			- totalCount
			- processedCount
			- successCount
			- failureCount
			- skippedCount
		- items
- error
```

取消成功后的任务查询响应不包含 `result.download`、`result.documentId` 或异步批量单笔成功结果。

## 异步任务状态确认

确认基线：异步任务状态集合确认为 `ACCEPTED`、`PROCESSING`、`SUCCEEDED`、`FAILED`、`PARTIAL_SUCCEEDED`、`EXPIRED`、`CANCELLED`。不单独增加 `QUEUED`；排队或已创建未处理的任务统一表达为 `ACCEPTED`。

| 状态语义 | 确认状态 | 说明 | 适用范围 |
| --- | --- | --- | --- |
| 已受理 | `ACCEPTED` | 请求已通过基础校验并创建任务，包含排队或尚未开始处理。 | 单笔与批量任务。 |
| 处理中 | `PROCESSING` | 任务正在生成文档。 | 单笔与批量任务。 |
| 成功 | `SUCCEEDED` | 全部生成成功。 | 单笔与批量任务。 |
| 失败 | `FAILED` | 任务整体失败。 | 单笔与批量任务。 |
| 部分成功 | `PARTIAL_SUCCEEDED` | 异步批量中部分记录成功、部分失败。 | 仅异步批量任务。 |
| 已过期 | `EXPIRED` | 任务或结果超过保留期限。 | 单笔与批量任务。 |
| 已取消 | `CANCELLED` | 任务被受控取消，且不返回生成结果。 | 单笔与批量任务。 |

终态包括 `SUCCEEDED`、`FAILED`、`PARTIAL_SUCCEEDED`、`EXPIRED` 和 `CANCELLED`；终态任务不可再次取消。

## 枚举命名确认

以下枚举值为 v1 确认基线。

| 枚举语义 | 确认枚举值 | 说明 |
| --- | --- | --- |
| 输出格式 DOCX | `DOCX` | 生成 DOCX。 |
| 输出格式 PDF | `PDF` | 生成 PDF。 |
| 同步文件流 | `SYNC_STREAM` | 同步返回文件流。 |
| 同步下载地址 | `SYNC_DOWNLOAD_URL` | 同步返回下载地址。 |
| 异步任务 | `ASYNC_TASK` | 返回异步任务 ID。 |
| 显式版本路径 | `EXPLICIT_VERSION` | 调用显式发布版本路径。 |
| default 路径 | `DEFAULT_ROUTE` | 调用 default 路径并解析到目标发布版本。 |
| 幂等首次受理 | `IDEMPOTENCY_NEW` | 当前请求首次被受理。 |
| 幂等重复命中 | `IDEMPOTENCY_REPLAYED` | 当前请求命中已有幂等记录并返回原结果。 |
| 幂等冲突 | `IDEMPOTENCY_CONFLICTED` | 相同幂等标识对应不同请求语义，或 default 路径目标版本变化后命中旧幂等记录。 |
| 单笔成功 | `SUCCEEDED` | 单笔或任务成功。 |
| 单笔失败 | `FAILED` | 单笔或任务失败。 |
| 单笔跳过 | `SKIPPED` | 批量中因前置失败或策略未执行。 |
| 任务已受理 | `ACCEPTED` | 异步任务已创建。 |
| 任务处理中 | `PROCESSING` | 异步任务处理中。 |
| 任务部分成功 | `PARTIAL_SUCCEEDED` | 异步批量部分成功。 |
| 任务已过期 | `EXPIRED` | 任务或结果已过期。 |
| 任务已取消 | `CANCELLED` | 任务被取消。 |
| 允许打印 | `ALLOW_PRINT` | 允许输出文档打印。 |
| 允许复制 | `ALLOW_COPY` | 允许复制输出文档内容。 |
| 允许编辑 | `ALLOW_EDIT` | 允许编辑输出文档内容。 |
| 允许批注 | `ALLOW_ANNOTATE` | 允许批注或注释输出文档。 |
| 允许填写表单 | `ALLOW_FORM_FILL` | 允许填写输出文档中的表单域。 |

## 批量响应明细确认

批量响应需要表达批次级结果和每笔结果。以下字段名为 v1 确认基线。

确认基线：批量 JSON 响应必须同时表达批次级汇总和单笔明细；单笔明细按请求顺序返回，每个输入对应一条明细，回显调用方单笔标识，并体现单笔覆盖后的最终输出配置、加密策略摘要、成功结果或错误信息。

| 语义字段 | 候选字段名 | 建议语义 | 待确认点 |
| --- | --- | --- | --- |
| 批量请求标识 | `batchId` | 标识本次批量请求或异步批量任务。 | 由平台生成已确认。 |
| 总数 | `summary.totalCount` | 批量输入总笔数。 | 包含所有输入项已确认。 |
| 成功数 | `summary.successCount` | 成功生成数量。 | 同步批量失败时为 0 已确认。 |
| 失败数 | `summary.failureCount` | 失败数量。 | 包含失败项；是否单独统计跳过项按 `skippedCount` 表达。 |
| 单笔明细 | `items` | 每笔生成结果、错误、覆盖配置摘要。 | 按请求顺序全量返回已确认。 |
| 单笔标识 | `itemId` | 回显调用方提供的单笔业务标识。 | 必填且强制回显已确认。 |
| 单笔状态 | `status` | 每笔成功、失败、跳过等状态。 | 字段名和状态集合已确认。 |
| 单笔输出 | `output` | 单笔最终使用的输出格式和输出模式。 | 返回单笔覆盖后的最终值已确认。 |
| 单笔加密策略摘要 | `encryptionSummary` | 单笔最终使用的加密策略摘要。 | 不返回密码；仅返回摘要已确认。 |
| 单笔错误 | `error` | 每笔失败时的错误信息。 | 复用统一错误结构已确认。 |

## 批量响应字段命名确认

以下字段名为 v1 批量响应命名基线。

| 语义字段 | 推荐字段名 | 推荐理由 | 当前状态 |
| --- | --- | --- | --- |
| 批量请求标识 | `batchId` | 与批次级响应和审计关联。 | 由平台生成已确认。 |
| 批量汇总 | `summary` | 聚合总数、成功数、失败数。 | 作为独立对象已确认。 |
| 总数 | `summary.totalCount` | 表达输入总数。 | 包含所有输入项已确认。 |
| 成功数 | `summary.successCount` | 表达成功数量。 | 同步批量失败时为 0 已确认。 |
| 失败数 | `summary.failureCount` | 表达失败数量。 | 失败项数量已确认；跳过项单独使用 `summary.skippedCount` 表达。 |
| 单笔明细 | `items` | 与请求中的 `items` 对齐。 | 按请求顺序全量返回已确认。 |
| 单笔标识 | `items[].itemId` | 回显调用方单笔标识。 | 必填且同批唯一已确认。 |
| 单笔状态 | `items[].status` | 表达单笔成功、失败或跳过。 | 状态枚举已确认。 |
| 单笔输出 | `items[].output` | 表达单笔最终输出格式和模式。 | 已确认。 |
| 单笔加密策略摘要 | `items[].encryptionSummary` | 表达单笔最终加密策略摘要。 | 不返回密码；仅返回摘要已确认。 |
| 单笔文档标识 | `items[].documentId` | 标识单笔生成结果。 | 成功时返回，失败或跳过时不返回已确认。 |
| 单笔错误 | `items[].error` | 单笔失败时复用统一错误结构。 | 字段级错误嵌套到该错误对象下已确认。 |

确认基线：同步批量要求全部成功或全部失败；同步批量整体失败时需要返回每笔失败明细，且 `summary.successCount` 为 0。异步批量允许部分成功并返回成功/失败明细。单笔覆盖后的最终输出格式、输出模式和加密策略摘要必须在响应明细或审计中体现。异步批量部分成功后的失败项重试使用新批次和新的 `idempotencyKey`，通过 `originalBatchId` 或等效字段关联原批次。

## 错误模型确认基线

确认基线：API 错误模型采用细分 `error.code` + `error.category` 分组；`error.code` 使用稳定细分错误码，`error.category` 用于调用方按大类处理。所有 API 错误响应必须返回 `error.retryable`。`error.message` 使用英文业务可读消息，并返回 `error.messageKey` 供调用方进行多语言映射。

`error.messageKey` 命名规则采用 `api.error.<category>.<camelCaseCode>`，例如 `api.error.versionRouting.defaultRouteNotConfigured`。`error.message` 采用简洁、业务可读且不泄露敏感信息的英文消息，不暴露 API 凭证、密码、内部配置或未授权资源细节。

`error.message` 保持错误码级别的通用安全英文文案；同一 `error.code` 不因具体业务场景临时返回不同 `message`。不为错误响应新增 `resolutionHint`、`developerMessage` 等提示字段；更具体的业务说明通过字段级错误 `fieldErrors[].message`、安全差异摘要或 API 契约示例表达。

## 错误类别确认

| 错误类别语义 | 确认类别值 | 说明 |
| --- | --- | --- |
| 认证类 | `AUTHENTICATION` | API 凭证缺失、无效、过期、吊销，或访问账号缺失。 |
| 授权类 | `AUTHORIZATION` | AD Group 解析失败、AD Group 未授权或模板级授权失败。 |
| 路由与版本类 | `VERSION_ROUTING` | 环境不匹配、显式版本或 default 路径解析失败、版本不可用。 |
| API 管理策略类 | `API_POLICY` | 输出格式、输出模式、批量上限、加密能力、下载有效期或结果保留策略不允许。 |
| 幂等类 | `IDEMPOTENCY` | 幂等标识缺失、冲突或幂等存储暂不可用。 |
| 参数校验类 | `VALIDATION` | 请求体、请求标识、输出参数、变量或字段规则校验失败。 |
| 模板契约类 | `TEMPLATE_CONTRACT` | 发布版本契约、锚点或模板内容异常。 |
| 渲染装配类 | `RENDERING` | DOCX 装配、结构化内容渲染或装配后输出校验失败（含 OOXML 门禁）。 |
| 生成类 | `GENERATION` | 文档生成、PDF 转换、生成任务或生成结果资源异常。 |
| 加密类 | `ENCRYPTION` | 动态加密参数或加密处理失败。 |
| 批量类 | `BATCH` | 批量输入、单笔标识、部分失败或整批处理失败。 |

## v1 基线错误码清单

完整 v1 基线错误码如下。默认 `retryable` 是契约基线；需要不同重试语义时，应新增或细分错误码，而不是在同一错误码下临时改变默认含义。

| 错误类别 | 错误码 | messageKey | 默认 retryable | 英文消息 |
| --- | --- | --- | --- | --- |
| `AUTHENTICATION` | `API_CREDENTIAL_REQUIRED` | `api.error.authentication.apiCredentialRequired` | `false` | API credential is required. |
| `AUTHENTICATION` | `API_CREDENTIAL_INVALID` | `api.error.authentication.apiCredentialInvalid` | `false` | API credential is invalid. |
| `AUTHENTICATION` | `API_CREDENTIAL_EXPIRED` | `api.error.authentication.apiCredentialExpired` | `false` | API credential has expired. |
| `AUTHENTICATION` | `API_CREDENTIAL_REVOKED` | `api.error.authentication.apiCredentialRevoked` | `false` | API credential has been revoked. |
| `AUTHENTICATION` | `ACCESS_ACCOUNT_REQUIRED` | `api.error.authentication.accessAccountRequired` | `false` | Access account is required. |
| `AUTHORIZATION` | `AD_GROUP_RESOLUTION_FAILED` | `api.error.authorization.adGroupResolutionFailed` | `true` | Access account groups could not be resolved. |
| `AUTHORIZATION` | `AD_GROUP_NOT_AUTHORIZED` | `api.error.authorization.adGroupNotAuthorized` | `false` | Access account is not authorized for this API. |
| `AUTHORIZATION` | `TEMPLATE_ACCESS_DENIED` | `api.error.authorization.templateAccessDenied` | `false` | Access to this template is denied. |
| `AUTHORIZATION` | `SELF_APPROVAL_FORBIDDEN` | `api.error.lifecycle.selfApprovalForbidden` | `false` | Self-approval is not permitted; the decision actor must differ from the most recent submitter. |
| `AUTHORIZATION` | `EXCEPTION_INTERVENTION_NOT_ALLOWED` | `api.error.lifecycle.exceptionInterventionNotAllowed` | `false` | Exception intervention is only allowed for group or global administrators. |
| `VALIDATION` | `EXCEPTION_REASON_REQUIRED` | `api.error.lifecycle.exceptionReasonRequired` | `false` | An exception reason is required for intervention decisions. |
| `VALIDATION` | `EXCEPTION_SECONDARY_CONFIRM_REQUIRED` | `api.error.lifecycle.exceptionSecondaryConfirmRequired` | `false` | Secondary confirmation is required for exception intervention. |
| `VERSION_ROUTING` | `ENVIRONMENT_MISMATCH` | `api.error.versionRouting.environmentMismatch` | `false` | Requested environment does not match the deployment environment. |
| `VERSION_ROUTING` | `RELEASE_VERSION_REQUIRED` | `api.error.versionRouting.releaseVersionRequired` | `false` | Release version is required for this route. |
| `VERSION_ROUTING` | `RELEASE_VERSION_FORMAT_INVALID` | `api.error.versionRouting.releaseVersionFormatInvalid` | `false` | Release version must be a semantic version. |
| `VERSION_ROUTING` | `RELEASE_VERSION_NOT_FOUND` | `api.error.versionRouting.releaseVersionNotFound` | `false` | Release version was not found. |
| `VERSION_ROUTING` | `RELEASE_VERSION_DISABLED` | `api.error.versionRouting.releaseVersionDisabled` | `false` | Release version is disabled. |
| `VERSION_ROUTING` | `DEFAULT_ROUTE_NOT_CONFIGURED` | `api.error.versionRouting.defaultRouteNotConfigured` | `false` | Default route is not configured. |
| `VERSION_ROUTING` | `DEFAULT_ROUTE_TARGET_UNAVAILABLE` | `api.error.versionRouting.defaultRouteTargetUnavailable` | `false` | Default route target is unavailable. |
| `VERSION_ROUTING` | `TEMPLATE_DISABLED` | `api.error.versionRouting.templateDisabled` | `false` | Template is disabled. |
| `VERSION_ROUTING` | `TEMPLATE_DEPRECATED` | `api.error.versionRouting.templateDeprecated` | `false` | Template is deprecated. |
| `API_POLICY` | `OUTPUT_FORMAT_NOT_ALLOWED` | `api.error.apiPolicy.outputFormatNotAllowed` | `false` | Output format is not allowed for this API. |
| `API_POLICY` | `OUTPUT_MODE_NOT_ALLOWED` | `api.error.apiPolicy.outputModeNotAllowed` | `false` | Output mode is not allowed for this API. |
| `API_POLICY` | `BATCH_LIMIT_EXCEEDED` | `api.error.apiPolicy.batchLimitExceeded` | `false` | Request exceeds the configured batch limit. |
| `API_POLICY` | `ENCRYPTION_NOT_ALLOWED` | `api.error.apiPolicy.encryptionNotAllowed` | `false` | Dynamic encryption is not allowed for this API. |
| `API_POLICY` | `DOWNLOAD_URL_EXPIRED` | `api.error.apiPolicy.downloadUrlExpired` | `false` | Download URL has expired. |
| `API_POLICY` | `RESULT_RETENTION_EXPIRED` | `api.error.apiPolicy.resultRetentionExpired` | `false` | Generated result is no longer retained. |
| `IDEMPOTENCY` | `IDEMPOTENCY_KEY_REQUIRED` | `api.error.idempotency.idempotencyKeyRequired` | `false` | Idempotency key is required. |
| `IDEMPOTENCY` | `IDEMPOTENCY_KEY_CONFLICT` | `api.error.idempotency.idempotencyKeyConflict` | `false` | Idempotency key conflicts with a previous request. |
| `IDEMPOTENCY` | `IDEMPOTENCY_RETRY_NOT_ALLOWED` | `api.error.idempotency.idempotencyRetryNotAllowed` | `false` | Previous failed request is not retryable. |
| `IDEMPOTENCY` | `IDEMPOTENCY_STORE_UNAVAILABLE` | `api.error.idempotency.idempotencyStoreUnavailable` | `true` | Idempotency state is temporarily unavailable. |
| `VALIDATION` | `REQUEST_BODY_INVALID` | `api.error.validation.requestBodyInvalid` | `false` | Request body is invalid. |
| `VALIDATION` | `REQUEST_ID_REQUIRED` | `api.error.validation.requestIdRequired` | `false` | Request ID is required. |
| `VALIDATION` | `OUTPUT_FORMAT_REQUIRED` | `api.error.validation.outputFormatRequired` | `false` | Output format is required. |
| `VALIDATION` | `OUTPUT_MODE_REQUIRED` | `api.error.validation.outputModeRequired` | `false` | Output mode is required. |
| `VALIDATION` | `VARIABLES_REQUIRED` | `api.error.validation.variablesRequired` | `false` | Variables are required. |
| `VALIDATION` | `VARIABLE_REQUIRED` | `api.error.validation.variableRequired` | `false` | Required variable is missing. |
| `VALIDATION` | `VARIABLE_TYPE_INVALID` | `api.error.validation.variableTypeInvalid` | `false` | Variable type is invalid. |
| `VALIDATION` | `VARIABLE_FORMAT_INVALID` | `api.error.validation.variableFormatInvalid` | `false` | Variable format is invalid. |
| `VALIDATION` | `VARIABLE_RULE_FAILED` | `api.error.validation.variableRuleFailed` | `false` | Variable does not satisfy a validation rule. |
| `VALIDATION` | `VARIABLE_VALIDATION_FAILED` | `api.error.validation.variableValidationFailed` | `false` | One or more template variables failed VariableSchema validation (required / type / enum / unknown field). |
| `TEMPLATE_CONTRACT` | `TEMPLATE_CONTRACT_INVALID` | `api.error.templateContract.templateContractInvalid` | `false` | Template contract is invalid. |

**IBL-A1（2026-07-18；Task Master #107）：** runtime `generate` / `batch-generate`（逐 item）与管理 preview 装配路径对目标版本 `VariableSchema` 的 fail-closed 校验，权威顶层码为 **`VARIABLE_VALIDATION_FAILED`**（HTTP **422**，`category=VALIDATION`，`retryable=false`，非空 `fieldErrors[]`；`fieldErrors[].reason` ∈ `REQUIRED` \| `INVALID_TYPE` \| `INVALID_FORMAT` \| `ENUM_NOT_ALLOWED` \| `UNKNOWN_FIELD`）。既有 `VARIABLE_REQUIRED` / `VARIABLE_TYPE_INVALID` / `VARIABLE_FORMAT_INVALID` / `VARIABLE_RULE_FAILED` **保留在枚举中（兼容文档）**；本叶 runtime/preview **不以**它们作为多字段聚合响应的顶层码。`variables == null` 仍走既有 `VARIABLES_REQUIRED`。Publish **不**校验 generate body。CE-U03 测试集保存路径顶层码可不迁移。实现须同步 `ApiErrorCodes` + `messages_en.properties`。行为 SoT：[ibl-a1-variable-validation.md](../behavior/ibl-a1-variable-validation.md)（BDD-IBL-A1-001…008）。

**IBL-A2（2026-07-18；Task Master #108）：** `FORMAT_AMOUNT(value)` 保留 locale 默认币种；可选 `FORMAT_AMOUNT(value, currencyCode)` 第二参为 **ISO 4217** 字母码（例 `'EUR'`），**不是** locale。二元形态下 null/空白/非法币种（及非法 arity）→ 既有 **`VARIABLE_COMPUTE_FAILED`**（HTTP **422**，`category=GENERATION`，`messageKey=api.error.variable.computeFailed`，`retryable=false`；可观察变量 key + 表达式摘要）。本叶**不**新增顶层错误码。行为 SoT：[ibl-a2-format-amount-currency.md](../behavior/ibl-a2-format-amount-currency.md)（BDD-IBL-A2-001…010）。Formal phase **None**；**not** go-live；do **not** flip **#3b/#5a GO**。

**IBL-A3（2026-07-18；Task Master #109）：** `SPELL_AMOUNT(value)` **始终** CNY 中文大写（locale-independent）；可选 `SPELL_AMOUNT(value, currencyCode)` 第二参为 **ISO 4217** 字母码，拼写语言取自 `context.locale` primary language。本叶至少 `(en, USD)` + `(zh, CNY)`；未支持 pair / 非法币种 / 非法 arity → 既有 **`VARIABLE_COMPUTE_FAILED`**（同上 HTTP/category/messageKey；禁止静默错语言）。本叶**不**新增顶层错误码。行为 SoT：[ibl-a3-amount-in-words.md](../behavior/ibl-a3-amount-in-words.md)（BDD-IBL-A3-001…012）。Formal phase **None**；**not** go-live；do **not** flip **#3b/#5a GO**。
| `GENERATION` | `VARIABLE_COMPUTE_FAILED` | `api.error.variable.computeFailed` | `false` | Whitelist compute expression evaluation failed (incl. invalid FORMAT_AMOUNT currency; unsupported/illegal SPELL_AMOUNT pair). |
| `TEMPLATE_CONTRACT` | `TEMPLATE_ANCHOR_MISSING` | `api.error.templateContract.templateAnchorMissing` | `false` | Template anchor is missing. |
| `CONFLICT` | `BINDING_VERSION_CONFLICT` | `api.error.template.bindingVersionConflict` | `true` | This binding was updated elsewhere. Reload the binding, then save again. |
| `RENDERING` | `OOXML_VALIDATION_FAILED` | `api.error.rendering.ooxmlValidationFailed` | `false` | Generated document failed OOXML validation. |
| `RENDERING` | `PINNED_MASTER_UNAVAILABLE` | `api.error.rendering.pinnedMasterUnavailable` | `false` | Pinned master revision is unavailable. |
| `GENERATION` | `DOCX_GENERATION_FAILED` | `api.error.generation.docxGenerationFailed` | `true` | DOCX generation failed. |
| `GENERATION` | `PDF_CONVERSION_FAILED` | `api.error.generation.pdfConversionFailed` | `true` | PDF conversion failed. |
| `GENERATION` | `PDF_CONVERSION_CAPACITY_EXCEEDED` | `api.error.generation.pdfConversionCapacityExceeded` | `true` | PDF conversion pool saturated; retry later (HTTP 503). |
| `GENERATION` | `PDF_ARCHIVAL_ENCRYPTION_MUTEX` | `api.error.generation.pdfArchivalEncryptionMutex` | `false` | PDF/A archival profile cannot be combined with encryption (CE-O01). |
| `GENERATION` | `GENERATION_TIMEOUT` | `api.error.generation.generationTimeout` | `true` | Document generation timed out. |
| `GENERATION` | `GENERATION_SERVICE_UNAVAILABLE` | `api.error.generation.generationServiceUnavailable` | `true` | Document generation service is temporarily unavailable. |
| `GENERATION` | `ASYNC_TASK_NOT_FOUND` | `api.error.generation.asyncTaskNotFound` | `false` | Async task was not found. |
| `GENERATION` | `ASYNC_TASK_EXPIRED` | `api.error.generation.asyncTaskExpired` | `false` | Async task has expired. |
| `GENERATION` | `ASYNC_TASK_CANCELLATION_NOT_ALLOWED` | `api.error.generation.asyncTaskCancellationNotAllowed` | `false` | Async task cannot be cancelled. |
| `GENERATION` | `DOCUMENT_NOT_FOUND` | `api.error.generation.documentNotFound` | `false` | Generated document was not found. |
| `ENCRYPTION` | `ENCRYPTION_PARAMETER_INVALID` | `api.error.encryption.encryptionParameterInvalid` | `false` | Encryption parameter is invalid. |
| `ENCRYPTION` | `ENCRYPTION_FAILED` | `api.error.encryption.encryptionFailed` | `true` | Document encryption failed. |
| `BATCH` | `BATCH_ITEMS_REQUIRED` | `api.error.batch.batchItemsRequired` | `false` | Batch items are required. |
| `BATCH` | `BATCH_ITEM_COUNT_INVALID` | `api.error.batch.batchItemCountInvalid` | `false` | Batch item count is invalid. |
| `BATCH` | `ITEM_ID_REQUIRED` | `api.error.batch.itemIdRequired` | `false` | Batch item ID is required. |
| `BATCH` | `ITEM_ID_DUPLICATED` | `api.error.batch.itemIdDuplicated` | `false` | Batch item ID is duplicated. |
| `BATCH` | `ORIGINAL_BATCH_NOT_FOUND` | `api.error.batch.originalBatchNotFound` | `false` | Original batch was not found. |
| `BATCH` | `BATCH_PARTIAL_FAILED` | `api.error.batch.batchPartialFailed` | `false` | One or more batch items failed. |
| `BATCH` | `BATCH_PROCESSING_FAILED` | `api.error.batch.batchProcessingFailed` | `true` | Batch processing failed. |

## HTTP 状态码确认映射

确认基线：HTTP 状态码用于表达错误大类和调用结果大类，调用方判断精确失败原因仍以稳定 `error.code` 为主。错误响应正文必须继续返回 `error.code`、`error.category`、`error.message`、`error.messageKey` 和 `error.retryable`。

| HTTP 状态码 | 适用错误码或场景 | 确认语义 |
| --- | --- | --- |
| 200 OK | 异步批量任务查询结果为部分成功，单笔失败通过 `result.batch.items[].status` 与 `result.batch.items[].error` 表达。 | 请求成功完成，业务结果由批量明细表达；不使用顶层 `error` 表达部分失败。 |
| 202 Accepted | 异步单笔或批量生成请求已受理。 | 请求已通过基础校验并创建任务，结果通过任务查询获取。 |
| 400 Bad Request | `ENVIRONMENT_MISMATCH`、`RELEASE_VERSION_REQUIRED`、`RELEASE_VERSION_FORMAT_INVALID`、`OUTPUT_FORMAT_NOT_ALLOWED`、`OUTPUT_MODE_NOT_ALLOWED`、`BATCH_LIMIT_EXCEEDED`、`ENCRYPTION_NOT_ALLOWED`、`REQUEST_BODY_INVALID`、`REQUEST_ID_REQUIRED`、`OUTPUT_FORMAT_REQUIRED`、`OUTPUT_MODE_REQUIRED`、`VARIABLES_REQUIRED`、`ENCRYPTION_PARAMETER_INVALID`、`BATCH_ITEMS_REQUIRED`、`BATCH_ITEM_COUNT_INVALID`、`ITEM_ID_REQUIRED`、`ITEM_ID_DUPLICATED`。 | 请求结构、必填字段、格式类错误或 API 管理策略拒绝。 |
| 401 Unauthorized | `API_CREDENTIAL_REQUIRED`、`API_CREDENTIAL_INVALID`、`API_CREDENTIAL_EXPIRED`、`API_CREDENTIAL_REVOKED`、`ACCESS_ACCOUNT_REQUIRED`。 | API 凭证或访问账号认证失败。 |
| 403 Forbidden | `AD_GROUP_NOT_AUTHORIZED`、`TEMPLATE_ACCESS_DENIED`、`SELF_APPROVAL_FORBIDDEN`、`EXCEPTION_INTERVENTION_NOT_ALLOWED`。 | 调用方已被识别，但未获得模板 API 访问授权，或管理端同人审批 / 例外干预被拒绝；消息不得泄露未授权资源细节。 |
| 404 Not Found | `RELEASE_VERSION_NOT_FOUND`、`ASYNC_TASK_NOT_FOUND`、`DOCUMENT_NOT_FOUND`、`ORIGINAL_BATCH_NOT_FOUND`。 | 授权范围内请求的发布版本、任务、文档不存在，或批量重试血缘中的原批次在同凭证下不可见。 |
| 409 Conflict | `RELEASE_VERSION_DISABLED`、`DEFAULT_ROUTE_NOT_CONFIGURED`、`DEFAULT_ROUTE_TARGET_UNAVAILABLE`、`TEMPLATE_DISABLED`、`TEMPLATE_DEPRECATED`、`IDEMPOTENCY_KEY_CONFLICT`、`IDEMPOTENCY_RETRY_NOT_ALLOWED`、`ASYNC_TASK_CANCELLATION_NOT_ALLOWED`、`BINDING_VERSION_CONFLICT`（CE-U21 锚点绑定乐观锁）。 | 请求与当前版本、模板、default 配置、幂等状态、异步任务或绑定并发令牌冲突。 |
| 410 Gone | `DOWNLOAD_URL_EXPIRED`、`RESULT_RETENTION_EXPIRED`、`ASYNC_TASK_EXPIRED`。 | 资源曾可用，但下载地址、任务或结果已过期。 |
| 422 Unprocessable Entity | `VARIABLE_VALIDATION_FAILED`（IBL-A1 runtime/preview VariableSchema 聚合校验）、`VARIABLE_COMPUTE_FAILED`（CE-K03 / IBL-A2 / IBL-A3 compute 求值失败，含非法 `FORMAT_AMOUNT` 币种、未支持/非法 `SPELL_AMOUNT` pair）、`VARIABLE_REQUIRED`、`VARIABLE_TYPE_INVALID`、`VARIABLE_FORMAT_INVALID`、`VARIABLE_RULE_FAILED`、`OOXML_VALIDATION_FAILED`、`EXCEPTION_REASON_REQUIRED`、`EXCEPTION_SECONDARY_CONFIRM_REQUIRED`。 | 请求结构可解析，但模板变量、业务规则或 compute 表达式校验/求值未通过，装配后 DOCX 未通过 OOXML 输出校验（fail-closed，不落库/不预览），或 CE-G01 例外干预字段缺失。 |
| 500 Internal Server Error | `TEMPLATE_CONTRACT_INVALID`、`TEMPLATE_ANCHOR_MISSING`、`DOCX_GENERATION_FAILED`、`PDF_CONVERSION_FAILED`、`ENCRYPTION_FAILED`、`BATCH_PROCESSING_FAILED`。 | 平台处理、模板资产、生成、转换、加密或整批处理失败。 |
| 503 Service Unavailable | `AD_GROUP_RESOLUTION_FAILED`、`IDEMPOTENCY_STORE_UNAVAILABLE`、`GENERATION_SERVICE_UNAVAILABLE`、`PDF_CONVERSION_CAPACITY_EXCEEDED`。 | 权限依赖、幂等存储、生成服务临时不可用，或 PDF 转换池饱和。 |
| 504 Gateway Timeout | `GENERATION_TIMEOUT`。 | 文档生成超时。 |

## 统一错误响应确认

以下结构表达 v1 统一错误响应语义基线。

确认基线：API 错误响应采用统一语义结构，由错误信息和响应元数据组成；批量错误复用同一错误语义，并通过单笔错误明细表达每笔失败原因。

```text
Error response structure
- metadata
	- auditId
	- traceId
	- requestId
	- idempotencyKey
	- idempotencyStatus
	- templateId
	- routeType
	- resolvedReleaseVersion
- error
	- code
	- category
	- message
	- messageKey
	- retryable
	- fieldErrors
		- field
		- reason
		- message
	- items
		- itemId
		- status
		- output
		- encryptionSummary
		- error
			- code
			- category
			- message
			- messageKey
			- retryable
			- fieldErrors
```

错误响应确认原则：

- 错误响应必须包含稳定错误码和业务可读消息。
- 错误响应使用细分 `error.code`，并通过 `error.category` 分组。
- 错误响应的 `error.message` 使用英文业务可读消息，`error.messageKey` 用于调用方多语言映射。
- `error.messageKey` 命名规则采用 `api.error.<category>.<camelCaseCode>`。
- 英文错误消息必须简洁可读，且不得泄露 API 凭证、密码、内部配置或未授权资源细节。
- 所有错误响应必须包含 `error.retryable`。
- 错误响应应包含审计关联标识，便于调用方和平台管理员共同排查。
- 字段级校验错误应指向具体字段或变量路径，字段路径使用点路径和数组下标，例如 `variables.customerName`、`items[0].variables.amount`。
- 字段级错误原因 `fieldErrors[].reason` 采用已确认通用枚举集合。
- 批量部分失败以 `result.batch.items[].error` 承载单笔错误；顶层 `error` 仅用于整批失败或整个请求失败。
- 同步批量整批失败需要返回每笔失败明细时，单笔明细嵌入 `error.items`，不得在 envelope 顶层额外放置 `items`。
- 错误响应不得回显 API 传入的 DOCX/PDF 加密密码。
- 加密参数错误包括缺少必需密码、不支持的权限组合、`permissions` 缺少 `ownerPassword`、`enabled=false` 或未传 `enabled` 时仍传入加密子字段、密码长度不符合 12 到 128 字符基线、open/owner 密码相同；这些错误返回 `400 ENCRYPTION_PARAMETER_INVALID`。
- 加密参数合法但实际加密处理失败时，返回 `500 ENCRYPTION_FAILED`，`retryable=true`。
- 是否可重试由 `error.retryable` 明确表达。
- HTTP 状态码只表达错误大类；调用方判断精确失败原因仍应使用 `error.code`。

## 错误响应示例确认

以下示例用于确认错误语义、字段组合和 envelope 承载方式；不代表最终字段完整集合。

确认基线：错误响应示例采用重点场景覆盖，不要求每个错误码都提供独立示例。重点场景覆盖授权与 AD Group、版本与 default 路由、API 管理策略、异步与下载结果、生成与加密失败、批量单笔失败。示例中的 `error.message` 仍使用错误码级别的通用安全英文文案，不为同一 `error.code` 按场景变化。

### 认证失败示例

```text
HTTP/1.1 401 Unauthorized
{
	"error": {
		"code": "API_CREDENTIAL_INVALID",
		"category": "AUTHENTICATION",
		"message": "API credential is invalid.",
		"messageKey": "api.error.authentication.apiCredentialInvalid",
		"retryable": false
	},
	"metadata": {
		"auditId": "AUD-20250115-0001",
		"traceId": "TRC-8f12c0",
		"requestId": "REQ-20250115-0001"
	}
}
```

### 字段校验失败示例

```text
HTTP/1.1 422 Unprocessable Entity
{
	"error": {
		"code": "VARIABLE_RULE_FAILED",
		"category": "VALIDATION",
		"message": "Variable does not satisfy a validation rule.",
		"messageKey": "api.error.validation.variableRuleFailed",
		"retryable": false,
		"fieldErrors": [
			{
				"field": "variables.loanAmount",
				"reason": "OUT_OF_RANGE",
				"message": "Loan amount is outside the allowed range."
			}
		]
	},
	"metadata": {
		"auditId": "AUD-20250115-0002",
		"traceId": "TRC-1be0a2",
		"requestId": "REQ-20250115-0002",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "EXPLICIT_VERSION",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### VariableSchema 校验失败示例（IBL-A1）

确认基线：runtime generate / preview 装配路径对发布版本 `VariableSchema` 的 fail-closed 校验使用聚合顶层码 `VARIABLE_VALIDATION_FAILED`；多字段失败仍只有一个顶层 `error.code`，细节在 `fieldErrors[]`。

```text
HTTP/1.1 422 Unprocessable Entity
{
	"error": {
		"code": "VARIABLE_VALIDATION_FAILED",
		"category": "VALIDATION",
		"message": "One or more template variables failed validation.",
		"messageKey": "api.error.validation.variableValidationFailed",
		"retryable": false,
		"fieldErrors": [
			{
				"field": "variables.customerName",
				"reason": "REQUIRED",
				"message": "Required variable is missing."
			},
			{
				"field": "variables.letterType",
				"reason": "ENUM_NOT_ALLOWED",
				"message": "Variable value is not in the allowed enumeration."
			}
		]
	},
	"metadata": {
		"auditId": "AUD-20260718-0001",
		"traceId": "TRC-ibl-a1",
		"requestId": "REQ-20260718-0001",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "EXPLICIT_VERSION",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 幂等冲突示例

```text
HTTP/1.1 409 Conflict
{
	"error": {
		"code": "IDEMPOTENCY_KEY_CONFLICT",
		"category": "IDEMPOTENCY",
		"message": "Idempotency key conflicts with a previous request.",
		"messageKey": "api.error.idempotency.idempotencyKeyConflict",
		"retryable": false,
		"idempotencyConflict": {
			"conflictType": "REQUEST_SEMANTICS_MISMATCH",
			"conflictFields": [
				"output.format",
				"variablesHash"
			],
			"originalRequestAt": "2025-01-15T17:30:00+08:00",
			"originalResolvedReleaseVersion": "1.0.0",
			"requestHash": "sha256:request-summary",
			"variablesHash": "sha256:variables-summary"
		}
	},
	"metadata": {
		"auditId": "AUD-20250115-0003",
		"traceId": "TRC-5d9c11",
		"requestId": "REQ-20250115-0003",
		"idempotencyKey": "idem-20250115-0003",
		"idempotencyStatus": "IDEMPOTENCY_CONFLICTED",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "DEFAULT_ROUTE",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### AD Group 解析失败示例

```text
HTTP/1.1 503 Service Unavailable
{
	"error": {
		"code": "AD_GROUP_RESOLUTION_FAILED",
		"category": "AUTHORIZATION",
		"message": "Access account groups could not be resolved.",
		"messageKey": "api.error.authorization.adGroupResolutionFailed",
		"retryable": true
	},
	"metadata": {
		"auditId": "AUD-20250115-0006",
		"traceId": "TRC-46a8c2",
		"requestId": "REQ-20250115-0006",
		"templateId": "TPL-LOAN-NOTICE"
	}
}
```

### default 目标不可用示例

```text
HTTP/1.1 409 Conflict
{
	"error": {
		"code": "DEFAULT_ROUTE_TARGET_UNAVAILABLE",
		"category": "VERSION_ROUTING",
		"message": "Default route target is unavailable.",
		"messageKey": "api.error.versionRouting.defaultRouteTargetUnavailable",
		"retryable": false
	},
	"metadata": {
		"auditId": "AUD-20250115-0007",
		"traceId": "TRC-98f3a1",
		"requestId": "REQ-20250115-0007",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "DEFAULT_ROUTE"
	}
}
```

### 输出模式策略拒绝示例

```text
HTTP/1.1 400 Bad Request
{
	"error": {
		"code": "OUTPUT_MODE_NOT_ALLOWED",
		"category": "API_POLICY",
		"message": "Output mode is not allowed for this API.",
		"messageKey": "api.error.apiPolicy.outputModeNotAllowed",
		"retryable": false,
		"fieldErrors": [
			{
				"field": "output.mode",
				"reason": "ENUM_NOT_ALLOWED",
				"message": "Output mode is not enabled for this template API."
			}
		]
	},
	"metadata": {
		"auditId": "AUD-20250115-0008",
		"traceId": "TRC-c28510",
		"requestId": "REQ-20250115-0008",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "EXPLICIT_VERSION",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 异步任务过期示例

```text
HTTP/1.1 410 Gone
{
	"error": {
		"code": "ASYNC_TASK_EXPIRED",
		"category": "GENERATION",
		"message": "Async task has expired.",
		"messageKey": "api.error.generation.asyncTaskExpired",
		"retryable": false
	},
	"metadata": {
		"auditId": "AUD-20250115-0009",
		"traceId": "TRC-1f0e77",
		"requestId": "REQ-20250115-0009",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "DEFAULT_ROUTE",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 异步任务不可取消示例

```text
HTTP/1.1 409 Conflict
{
	"error": {
		"code": "ASYNC_TASK_CANCELLATION_NOT_ALLOWED",
		"category": "GENERATION",
		"message": "Async task cannot be cancelled.",
		"messageKey": "api.error.generation.asyncTaskCancellationNotAllowed",
		"retryable": false
	},
	"metadata": {
		"auditId": "AUD-20250115-0014",
		"traceId": "TRC-74df39",
		"requestId": "REQ-20250115-0014",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "DEFAULT_ROUTE",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 生成结果保留过期示例

```text
HTTP/1.1 410 Gone
{
	"error": {
		"code": "RESULT_RETENTION_EXPIRED",
		"category": "API_POLICY",
		"message": "Generated result is no longer retained.",
		"messageKey": "api.error.apiPolicy.resultRetentionExpired",
		"retryable": false
	},
	"metadata": {
		"auditId": "AUD-20250115-0013",
		"traceId": "TRC-7f2d0e",
		"requestId": "REQ-20250115-0013",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "DEFAULT_ROUTE",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 生成超时示例

```text
HTTP/1.1 504 Gateway Timeout
{
	"error": {
		"code": "GENERATION_TIMEOUT",
		"category": "GENERATION",
		"message": "Document generation timed out.",
		"messageKey": "api.error.generation.generationTimeout",
		"retryable": true
	},
	"metadata": {
		"auditId": "AUD-20250115-0010",
		"traceId": "TRC-0b6dc9",
		"requestId": "REQ-20250115-0010",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "EXPLICIT_VERSION",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 加密失败示例

```text
HTTP/1.1 500 Internal Server Error
{
	"error": {
		"code": "ENCRYPTION_FAILED",
		"category": "ENCRYPTION",
		"message": "Document encryption failed.",
		"messageKey": "api.error.encryption.encryptionFailed",
		"retryable": true
	},
	"metadata": {
		"auditId": "AUD-20250115-0011",
		"traceId": "TRC-a31d94",
		"requestId": "REQ-20250115-0011",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "EXPLICIT_VERSION",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 批量单笔标识重复示例

```text
HTTP/1.1 400 Bad Request
{
	"error": {
		"code": "ITEM_ID_DUPLICATED",
		"category": "BATCH",
		"message": "Batch item ID is duplicated.",
		"messageKey": "api.error.batch.itemIdDuplicated",
		"retryable": false,
		"fieldErrors": [
			{
				"field": "items[1].itemId",
				"reason": "DUPLICATED",
				"message": "Batch item ID is duplicated."
			}
		]
	},
	"metadata": {
		"auditId": "AUD-20250115-0012",
		"traceId": "TRC-f4912a",
		"requestId": "REQ-20250115-0012",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "EXPLICIT_VERSION",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 下载地址过期示例

```text
HTTP/1.1 410 Gone
{
	"error": {
		"code": "DOWNLOAD_URL_EXPIRED",
		"category": "API_POLICY",
		"message": "Download URL has expired.",
		"messageKey": "api.error.apiPolicy.downloadUrlExpired",
		"retryable": false
	},
	"metadata": {
		"auditId": "AUD-20250115-0004",
		"traceId": "TRC-7aa431",
		"requestId": "REQ-20250115-0004",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "EXPLICIT_VERSION",
		"resolvedReleaseVersion": "1.0.0"
	}
}
```

### 异步批量部分成功查询示例

以下示例适用于异步批量任务查询结果；同步批量仍要求全部成功或全部失败。

```text
HTTP/1.1 200 OK
{
	"metadata": {
		"auditId": "AUD-20250115-0005",
		"traceId": "TRC-29db77",
		"requestId": "REQ-20250115-0005",
		"templateId": "TPL-LOAN-NOTICE",
		"routeType": "DEFAULT_ROUTE",
		"resolvedReleaseVersion": "1.0.0",
		"output": {
			"format": "PDF",
			"mode": "ASYNC_TASK"
		}
	},
	"result": {
		"task": {
			"taskId": "TASK-7K3M9Q2R",
			"status": "PARTIAL_SUCCEEDED",
			"queryPath": "/api/prod/v1/templates/TPL-LOAN-NOTICE/tasks/TASK-7K3M9Q2R",
			"acceptedAt": "2026-06-04T10:30:00+08:00",
			"updatedAt": "2026-06-04T10:32:00+08:00",
			"expiresAt": "2026-06-11T10:30:00+08:00"
		},
		"batch": {
			"batchId": "BATCH-7K3M9Q2R",
			"summary": {
				"totalCount": 2,
				"processedCount": 2,
				"successCount": 1,
				"failureCount": 1,
				"skippedCount": 0
			},
			"items": [
				{
					"itemId": "ITEM-001",
					"status": "SUCCEEDED",
					"output": {
						"format": "PDF",
						"mode": "ASYNC_TASK"
					},
					"encryptionSummary": {
						"enabled": true,
						"outputFormat": "PDF",
						"openPasswordProvided": true,
						"ownerPasswordProvided": true,
						"permissions": ["ALLOW_PRINT"]
					},
					"documentId": "DOC-8F2N6P4Q",
					"fidelityWarnings": []
				},
				{
					"itemId": "ITEM-002",
					"status": "FAILED",
					"output": {
						"format": "PDF",
						"mode": "ASYNC_TASK"
					},
					"encryptionSummary": {
						"enabled": true,
						"outputFormat": "PDF",
						"openPasswordProvided": true,
						"ownerPasswordProvided": true,
						"permissions": ["ALLOW_PRINT"]
					},
					"error": {
						"code": "VARIABLE_VALIDATION_FAILED",
						"category": "VALIDATION",
						"message": "One or more template variables failed validation.",
						"messageKey": "api.error.validation.variableValidationFailed",
						"retryable": false,
						"fieldErrors": [
							{
								"field": "items[1].variables.customerName",
								"reason": "REQUIRED",
								"message": "Required variable is missing."
							}
						]
					}
				}
			]
		}
	}
}
```

## 错误字段命名确认基线

以下错误字段名、语义、HTTP 状态码映射和 envelope 承载方式已确认。

| 语义字段 | 推荐字段名 | 推荐理由 | 待确认点 |
| --- | --- | --- | --- |
| 错误对象 | `error` | 统一承载错误信息。 | 已确认。 |
| 错误码 | `error.code` | 稳定机器可读细分错误码。 | v1 基线清单已确认。 |
| 错误类别 | `error.category` | 便于上游按大类处理。 | 11 类固定集合已确认（含 `RENDERING`）。 |
| 业务可读消息 | `error.message` | 英文业务可读消息。 | 简洁可读且不泄露敏感信息已确认。 |
| 消息键 | `error.messageKey` | 供调用方进行多语言映射。 | `api.error.<category>.<camelCaseCode>` 已确认。 |
| 是否可重试 | `error.retryable` | 明确调用方能否重试。 | 已确认必须返回。 |
| 字段级错误 | `error.fieldErrors` | 表达请求字段或变量校验失败。 | 已确认。 |
| 字段路径 | `fieldErrors[].field` | 使用点路径和数组下标指向失败字段。 | 已确认。 |
| 字段错误原因 | `fieldErrors[].reason` | 表达字段失败原因。 | 通用枚举集合已确认。 |
| 批量单笔错误 | `result.batch.items[].error` 或 `error.items[].error` | 异步批量部分失败时在 `result.batch.items[]` 表达；整批失败需要每笔明细时在 `error.items[]` 表达。 | 已确认以单笔明细为主。 |
| 单笔标识 | `items[].itemId` | 关联调用方单笔输入。 | 批量明细回显已确认。 |

## 字段级 reason 确认枚举

确认基线：错误模型采用细分 `error.code` + `error.category` 分组。`error.reason` 不作为主错误细分机制；字段级错误继续使用 `fieldErrors[].reason` 表达字段失败原因。

| reason 枚举 | 说明 |
| --- | --- |
| `REQUIRED` | 必填字段或变量缺失。 |
| `INVALID_TYPE` | 字段或变量类型不符合契约。 |
| `INVALID_FORMAT` | 字段或变量格式不符合契约。 |
| `OUT_OF_RANGE` | 数值、日期或长度范围不符合契约。 |
| `TOO_LONG` | 字符串、数组或集合超过允许上限。 |
| `TOO_SHORT` | 字符串、数组或集合低于允许下限。 |
| `ENUM_NOT_ALLOWED` | 枚举值不在允许集合内。 |
| `PATTERN_MISMATCH` | 字段不满足格式模式。 |
| `RULE_FAILED` | 模板规则或业务规则校验失败。 |
| `DUPLICATED` | 字段值在当前请求范围内重复。 |
| `UNKNOWN_FIELD` | 请求包含契约未定义字段。 |

## 审计映射确认与开放议题

API 调用和 API 管理配置变更审计采用标准摘要对象。

标准摘要字段基线：

- `auditId`。
- `eventType`。
- `eventAt`。
- 操作者或系统主体摘要。
- API 凭证或指纹摘要。
- 访问账号。
- 环境。
- 模板、发布版本、解析后发布版本和路由类型。
- `requestId`、`idempotencyKey` 摘要和幂等状态。
- `taskId`、`batchId`、`itemId`（或其安全摘要）。
- `contextSummary`。
- 输出摘要、加密摘要、批量摘要、资源 ID、结果摘要、错误摘要和耗时。
- API 管理配置变更的配置差异摘要，包括变更字段、变更前摘要、变更后摘要和影响预览摘要。

标准审计摘要不得记录模板变量原值、加密密码、完整请求体、API 凭证 secret、完整下载地址、完整 AD Group 成员、未授权组详情、历史密文或敏感配置明文。

审计补充关联语义已确认并纳入标准摘要字段基线：

- 调用方请求标识。
- 任务 ID。
- 批量请求 ID。
- 单笔批量明细标识。
- API 管理配置版本或变更关联标识已确认为 `policyVersion`；API 管理配置变更统一使用 `API_POLICY_UPDATED` 审计事件，并通过 `changedAreas` 表达变更配置域。
- 错误码和错误类别。

审计摘要不得包含 API 传入的 DOCX/PDF 加密密码。

## API 管理配置契约展示与变更治理确认

### AD Group 解析、缓存与权限变更生效确认

确认基线：AD Group 是 API 双重授权的一部分。AD Group 解析规则适用于所有需要 AD Group 授权的 API 操作，包括生成、批量生成、异步任务查询、异步任务取消、下载取文件、API 契约查看和可调用版本列表。

解析与缓存规则：

- AD Group 成功解析结果按 `accessAccount` + `environment` 缓存 5 分钟。
- 平台不缓存 AD Group 解析失败结果。
- AD Group 解析失败时，如果存在未过期缓存，则使用未过期缓存继续授权。
- AD Group 解析失败且不存在未过期缓存时，返回 `503 AD_GROUP_RESOLUTION_FAILED`，`retryable=true`。
- AD Group 授权不得使用过期缓存兜底；过期缓存不能作为授权依据。
- API 管理中的 AD Group 授权配置变更立即生效，并清理相关授权缓存；不等待 5 分钟缓存自然过期。
- 目录中的 AD Group 成员变更在目录同步完成且平台缓存过期后生效。
- 平台需要在 API 契约或管理界面说明最多可能存在 5 分钟平台缓存延迟，不承诺消除外部目录同步延迟。
- AD Group 解析、缓存命中、缓存失效、解析失败和授权拒绝需要记录审计摘要。
- 审计、日志、契约展示和管理界面不得记录或展示完整 AD Group 成员清单或未授权组详情。

建议契约展示语义：

| 展示语义 | 说明 | 当前状态 |
| --- | --- | --- |
| AD Group 授权摘要 | 当前调用方是否满足模板级 AD Group 授权，或管理员视角的授权组摘要。 | 字段名 `apiPolicy.adGroupAuthorizationSummary` 已确认；不得展示完整成员或未授权组详情。 |
| AD Group 缓存 TTL | 成功解析结果的平台缓存时长。 | 5 分钟已确认；纳入 `apiPolicy.adGroupAuthorizationSummary` 表达。 |
| 权限变更生效说明 | API 管理配置变更立即生效并清理缓存；目录成员变更受目录同步和缓存过期影响。 | 已确认；纳入 `apiPolicy.adGroupAuthorizationSummary` 表达。 |
| AD Group 解析失败语义 | 无有效缓存时返回 `503 AD_GROUP_RESOLUTION_FAILED`。 | 已确认。 |

### API 凭证生命周期确认

确认基线：API 凭证对象代表调用系统或应用，是调用方级身份，可授权到多个模板 API。模板调用仍必须同时满足 API 凭证授权、AD Group 授权和模板级授权。

生命周期规则：

- API 凭证由全局管理员和分组管理员管理；全局管理员可管理全部 API 凭证，分组管理员只能管理被授权组范围内的 API 凭证。
- API 凭证创建和轮换时，secret 明文只展示一次；平台只保存不可逆摘要或指纹，不允许管理员后续重新查看 secret 明文。
- API 凭证必须设置有效期；默认有效期为 180 天，最长 365 天，管理员可设置更短有效期。
- API 凭证到期时间以持久化 `expires_at` / 契约字段 `credentialSummary.expiresAt` 为真相；不得仅用 `createdAt + 180` 冒充持久化到期（CE-C04）。
- API 凭证状态集合确认为 `ACTIVE`、`EXPIRING_SOON`、`EXPIRED`、`REVOKED`。
- `EXPIRING_SOON` 用于到期前提醒窗口（到期前 30 天且未过期），轮换状态由当前 secret 与旧 secret 宽限期表达，不新增凭证级 `ROTATING` 状态。
- 有效状态为 `ACTIVE` 或 `EXPIRING_SOON` 的凭证在 secret 匹配时可调用；`EXPIRED` → `401 API_CREDENTIAL_EXPIRED`；`REVOKED` → `401 API_CREDENTIAL_REVOKED`。
- API 凭证轮换时，新 secret 立即可用，旧 secret 保留 7 天宽限期；宽限期结束后旧 secret 失效；轮换不重置 `expires_at`。
- 旧 secret 在轮换宽限期结束后不再可用；使用已失效旧 secret 的请求按认证失败处理，不通过错误消息泄露轮换细节。
- API 凭证吊销立即生效，阻断该凭证的所有后续 API 操作，包括新生成、异步任务查询、异步任务取消和下载取文件。
- 已受理的后台生成任务可继续完成，但调用方不能再使用被吊销凭证获取结果。
- API 凭证过期后，使用该凭证的 API 请求返回 `401 API_CREDENTIAL_EXPIRED`。
- API 凭证吊销后，使用该凭证的 API 请求返回 `401 API_CREDENTIAL_REVOKED`。
- API 凭证到期前 30 天、7 天和 1 天提醒全局管理员和对应分组管理员。
- API 凭证到期前不主动提醒 API 调用方；API 调用方可通过 API 契约或管理界面查看自己凭证的非敏感状态和到期摘要。
- API 凭证生命周期审计需要覆盖创建、轮换、吊销、过期、到期提醒和凭证摘要查看；审计至少记录操作者、时间、操作原因、管理范围、状态变化、到期时间、凭证标识或指纹摘要和受影响授权范围，不记录 secret 明文。

建议契约展示语义：

| 展示语义 | 说明 | 当前状态 |
| --- | --- | --- |
| API 凭证标识 | 标识调用方级凭证，不展示 secret。 | 纳入 `apiPolicy.credentialSummary` 表达。 |
| API 凭证状态 | `ACTIVE`、`EXPIRING_SOON`、`EXPIRED`、`REVOKED`。 | 已确认。 |
| 到期时间 | 当前凭证到期时间（持久化 `expiresAt`，ISO 8601 带时区）。 | 默认 180 天、最长 365 天已确认；纳入 `apiPolicy.credentialSummary.expiresAt`；CE-C04 结束 `createdAt+180` 过渡推导。 |
| 授权模板摘要 | 当前凭证可调用的模板范围摘要。 | 纳入 `apiPolicy.credentialSummary` 表达。 |
| 轮换宽限期 | 当前是否存在旧 secret 宽限期及其结束时间。 | 7 天宽限期已确认；纳入 `apiPolicy.credentialSummary` 表达。 |
| 指纹摘要 | 用于管理员识别当前 secret 版本的非敏感摘要。 | 已确认，不展示 secret 明文。 |

确认基线：API 管理配置界面采用模板级 API 管理页，使用配置域导航 + 详情区；配置域导航固定包含 AD Group 授权、输出方式、批量上限、DOCX/PDF 动态加密能力和 default 路径目标发布版本，详情区展示当前配置摘要、候选配置编辑区、字段提示、影响预览、硬阻断和警告、保存确认动作、当前 `policyVersion`、最近更新时间、最近操作人和审计入口。配置按配置域独立保存；每个配置域操作动线为编辑候选配置、执行影响预览、处理硬阻断或确认警告、管理员确认立即生效；候选配置变更后必须重新执行影响预览，保存成功生成新的 `policyVersion` 和审计记录。API 管理配置引入 `policyVersion`；每次配置域变更成功生效后生成新的配置版本，用于契约展示、审计、影响预览和回滚关联。

确认基线：API 管理配置字段控件采用固定控件 + 内联提示。AD Group 授权使用可搜索 AD Group 选择器和授权范围摘要，不展示完整成员或未授权组详情；输出方式使用输出格式和输出模式勾选；批量上限使用同步/异步数值输入并展示上限含义；DOCX/PDF 动态加密使用启用开关和能力项选择，不保存加密密码；default 路径目标发布版本使用发布版本选择器，并展示版本状态、契约摘要和影响提示。

确认基线：API 管理配置中的 AD Group 授权、输出方式、批量上限、DOCX/PDF 动态加密能力变更，均需要在变更前提供影响预览；default 路径目标发布版本继续遵循已确认的专门治理规则。影响预览需要区分硬阻断和警告；违反已确认策略或会导致候选配置不可生效的硬阻断必须阻止保存，风险提示类警告允许管理员确认后继续。硬阻断和警告文案采用固定结构：原因、影响、处理建议；影响信息至少包含受影响发布版本或调用方范围摘要和预期错误码。硬阻断文案必须明确无法保存，警告文案必须明确确认继续后会立即生效并记录审计。API 凭证生命周期、轮换、吊销、过期、到期提醒和相关审计策略已确认，并独立遵循 API 凭证生命周期规则。

API 契约按角色展示 API 管理配置：

| 查看角色 | 可见内容 | 不可展示内容 |
| --- | --- | --- |
| 被授权 API 调用方 | 当前可用策略摘要，包括允许的输出方式、批量上限、是否允许 DOCX/PDF 动态加密、可用加密能力摘要、当前调用方是否满足模板级调用授权，以及自己凭证的非敏感状态和到期摘要。 | API 凭证明文、其他调用方凭证信息、完整 AD Group 成员、未授权组信息、敏感值、历史密文和完整审计明细。 |
| 管理员和模板编排人员 | API 管理配置详情、当前配置状态、`policyVersion`、最近更新时间、最近操作人、影响预览、硬阻断和警告摘要、凭证非敏感状态摘要和审计关联入口。 | API 凭证明文、加密密码、历史密文和其他敏感值。 |

配置变更治理：

- 变更只支持立即生效，不支持未来定时生效、待生效变更或取消待生效变更；AD Group 授权配置变更同时清理相关授权缓存。
- AD Group 授权、输出方式、批量上限、DOCX/PDF 动态加密能力和 default 路径目标发布版本按配置域独立保存；每次成功生效生成新的 `policyVersion`。
- 变更不主动通知调用方或管理员，仅记录审计；调用方通过 API 契约查看当前可用策略摘要。
- 影响预览至少包含当前配置与候选配置差异、受影响模板及未停用发布版本、授权调用方或 AD Group 范围摘要、近期调用量摘要、可能被拒绝的输出模式/批量/加密调用摘要和预期错误码提示。
- 影响预览需要区分硬阻断和警告；硬阻断阻止保存，警告允许管理员确认后继续。
- 回滚按一次新的受控变更处理：管理员选择历史配置作为候选配置，执行影响预览，确认后立即生效，并记录审计；回滚不主动通知调用方或管理员。

审计格式：API 管理配置变更统一使用 `eventType=API_POLICY_UPDATED`，并通过 `changedAreas` 表达变更配置域。`changedAreas` 取值基线为 `AD_GROUP_AUTHORIZATION`、`OUTPUT_POLICY`、`BATCH_LIMIT`、`ENCRYPTION_CAPABILITY`、`DEFAULT_ROUTE_TARGET`。审计需要记录 `policyVersion`、上一配置版本、变更配置域、配置差异摘要、影响预览摘要、硬阻断和警告摘要、确认结果、是否回滚以及回滚来源版本；不得记录敏感配置明文。

建议契约展示语义：

| 展示语义 | 说明 | 当前状态 |
| --- | --- | --- |
| API 管理配置版本 | 当前 API 管理配置版本，用于契约展示、审计、影响预览和回滚关联。 | 字段名 `apiPolicy.policyVersion` 已确认。 |
| API 管理配置更新时间 | 当前配置最近更新时间。 | 字段名 `apiPolicy.updatedAt` 已确认。 |
| API 管理配置最近操作人 | 当前配置最近操作人。 | 字段名 `apiPolicy.updatedBy` 已确认；不得展示敏感身份明细。 |
| 允许输出格式 | 当前模板 API 允许的输出格式集合。 | 字段名 `apiPolicy.allowedOutputFormats` 已确认。 |
| 允许输出模式 | 当前模板 API 允许的输出模式集合。 | 字段名 `apiPolicy.allowedOutputModes` 已确认。 |
| 批量上限 | 当前同步批量和异步批量上限。 | 字段名 `apiPolicy.batchLimits.syncMaxItems` 和 `apiPolicy.batchLimits.asyncMaxItems` 已确认。 |
| 动态加密能力摘要 | 当前是否允许 DOCX/PDF 动态加密及可用能力摘要。 | 字段名 `apiPolicy.encryptionCapabilities` 已确认。 |
| AD Group 授权摘要 | 当前调用方是否满足模板级调用授权，或管理员视角的授权组摘要，并说明 5 分钟平台缓存和权限变更生效规则。 | 字段名 `apiPolicy.adGroupAuthorizationSummary` 已确认。 |
| API 凭证摘要 | 当前调用方自己凭证的非敏感状态和到期摘要，或管理员视角的凭证非敏感状态摘要。 | 字段名 `apiPolicy.credentialSummary` 已确认；不得展示 secret 明文。 |
| 契约版本对比 | 调用方视图基于已授权可见的现有契约信息、可调用版本、请求 Schema、响应 Schema、错误码、API 策略、路由/default 目标和示例计算展示非敏感对比摘要。 | 页面计算展示已确认；v1 不新增 `ContractResponse` 专门字段。 |

## 契约文档骨架

| 契约部分 | 应覆盖内容 | 当前状态 |
| --- | --- | --- |
| 接口地址 | 环境、模板标识、显式发布版本路径、default 路径、单笔/批量生成、异步任务查询、异步任务取消和下载路径。 | 路由语义、路径命名、异步取消路径和 default 路径契约展示字段已确认。 |
| 认证与授权 | API 凭证、访问账号、AD Group、模板级授权、统一授权判定、失败场景。 | 授权模型、统一授权判定、fail-closed 策略、API 凭证生命周期、AD Group 解析失败、缓存、同步延迟、权限变更生效策略和审计已确认。 |
| 发布版本与可调用列表 | 显式发布版本号、未停用发布版本列表、模板停用/废弃和版本停用后的可调用判断；可选展示字段 `deprecated` / `sunsetAt`。 | 模板级 API 管理配置基线、契约查看接口和可调用版本列表响应格式已确认；可选字段为展示/发现元数据，不改变可调用候选集（ADR-0003 / ADR-0017 展示边界；CE-C04）。 |
| 请求 Schema | 模板变量、规则校验、输出格式、输出模式、批量输入、加密参数。 | 字段命名、路径/请求体边界、OpenAPI 3.1 YAML 载体和严格未知字段策略已确认；正式 OpenAPI v1 已输出，后续随契约变更维护。 |
| 幂等策略 | `requestId`、`idempotencyKey`、`itemId`、重复提交处理、default 路径幂等、幂等保留期限。 | 生成类 API 必填范围、唯一性范围、7 天保留、过期后按新请求处理、default 变更后冲突、失败后按 `retryable` 决定、幂等状态枚举、冲突安全摘要、过期 key 复用审计标记、批量 `itemId` 必填唯一、重复 `itemId` 处理、同步批量失败幂等记录和异步失败项重试策略已确认。 |
| 响应 Schema | 文件流、下载地址、异步任务 ID、批量成功/失败明细、通用响应元数据。 | JSON envelope、`metadata`/`result`/`error` 承载方式、字段命名和批量全量明细返回已确认；正式 OpenAPI v1 已输出，后续随契约变更维护。 |
| 错误码 | 认证失败、AD Group 解析失败、授权失败、版本不可用、参数校验失败、锚点缺失、渲染/OOXML 校验失败、生成失败、加密失败、批量部分失败、异步任务、异步取消和下载取文件失败。 | v1 基线错误码清单、11 类 `error.category`（含 `RENDERING`）、默认 `retryable`、英文 `message`、`messageKey` 命名规则、字段级 `reason` 枚举、HTTP 状态码映射、通用安全消息策略和重点场景错误响应示例已确认。 |
| 异步任务 | 任务状态、查询方式、进度摘要、过期策略、取消策略。 | 查询接口、受控取消、状态命名、无百分比进度和取消后不返回结果已确认。 |
| 下载地址 | 有效期、访问控制、一次性/多次下载、文件清理策略。 | 15 分钟固定有效期、二次授权、不重新校验发布版本可调用状态、有效期内多次下载、不可配置为一次性、过期后不重新签发、7 天结果保留、时间格式、下载地址脱敏展示和清理前不通知已确认。 |
| 批量生成 | 默认同步 100 条、默认异步 10,000 条、API 管理可配置更低上限、失败明细。 | 上限、失败策略、`itemId` 必填唯一、重复 `itemId` 处理、同步失败明细、异步失败项重试策略、字段命名和按请求顺序全量响应明细已确认；正式 OpenAPI v1 已输出，后续随契约变更维护。 |
| API 管理配置 | API 凭证、AD Group 授权、输出方式、批量上限、DOCX/PDF 动态加密能力、default 路径目标发布版本；由全局管理员和分组管理员承担；按模板级绑定并影响该模板下所有未停用发布版本。 | 功能边界、负责角色、模板级绑定基线、v1 不提供发布版本级覆盖、模板级 API 管理页、配置域导航 + 详情区、固定控件与内联提示、按配置域独立保存、`policyVersion`、API 凭证生命周期、AD Group 授权解析与缓存、default 路径配置职责、按角色契约展示字段、立即生效、影响预览硬阻断/警告、警告文案结构、审计事件和回滚策略已确认。 |
| 加密参数 | `enabled`、`openPassword`、`ownerPassword`、`permissions`。 | 参数模型、`enabled` 语义、`openPassword` 必填规则、`ownerPassword` 与 `permissions` 关系、密码强度、权限抽象映射、权限枚举和失败处理已确认；正式 OpenAPI v1 已输出，后续随契约变更维护。 |
| 审计摘要 | 请求参数摘要、生成文件标识、加密策略摘要、错误原因、配置差异摘要、授权判定摘要。 | 标准摘要对象、授权判定安全摘要、敏感数据分级和禁止明文字段已确认。 |

## 后续 API 契约维护议题

- 正式 OpenAPI 3.1 YAML 文件和示例请求/响应已输出，后续需要随契约变更持续维护。

## 开放议题集中清单

以下开放议题用于收敛仍未决的契约细节，避免分散在多个表格中。

### OQ-1 幂等未决点收敛（已收敛）

- 状态：已收敛
- 收敛日期：2026-06-16
- 决策依据： [ADR 0004：API 幂等策略](../adr/api/0004-api-idempotency-strategy.md)
- 关键锚点：
	- `Decision`：确认 `idempotencyKey` 适用范围、default 路径目标变更冲突保护、失败重放边界、批量 `items[].itemId` 唯一与失败重试约束。
	- `Consequences`：确认调用方重试可预期性与审计可追溯边界。
- 收敛结果：
	- 幂等唯一性与冲突判定规则以 ADR 0004 为准，不再作为未决项。
	- default 路径目标变更下的幂等冲突行为已固定，不存在待确认分支。
	- 批量失败重放与失败项重试边界已固定，后续仅按已确认规则演进。

### OQ-2 审计映射未决点收敛（已收敛）

- 状态：已收敛
- 收敛日期：2026-06-16
- 决策依据：
	- [ADR 0013：API 契约可见性、审计摘要与 context](../adr/api/0013-api-contract-visibility-audit-and-context.md)
	- [ADR 0016：API 管理配置界面与审计格式](../adr/api-management/0016-api-management-ui-and-audit-format.md)
- 关键锚点：
	- `Decision`（ADR 0013）：确认标准审计摘要字段基线、敏感字段禁入和 `context` 白名单。
	- `Decision`（ADR 0016）：确认 `eventType=API_POLICY_UPDATED`、`changedAreas` 及 `policyVersion` 关联基线。
- 收敛结果：
	- 契约可见性与审计摘要映射口径已统一，不再作为未决项。
	- API 管理配置变更审计事件与版本关联规则已固定并可回溯。
	- 与 `policyVersion` 的契约-审计-预览-回滚关联已纳入既有 ADR 规则。

### OQ-3 响应与批量字段命名残留开放项收敛（已收敛）

- 状态：已收敛
- 收敛日期：2026-06-20
- 决策依据： [ADR 0011：API Schema 与响应 Envelope](../adr/api/0011-api-schema-and-response-envelope.md)
- 关键锚点：
	- `Decision`：确认 v1 请求字段命名、统一 JSON envelope（`metadata`/`result`/`error`）和批量明细承载基线。
	- `Consequences`：确认响应处理与批量排查的一致性，不再保留字段命名残留开放表述。
- 收敛结果：
	- 响应与批量字段命名相关“开放项/待确认项”表述已完成收敛。
	- 契约文档中相关段落改为已确认口径，不新增行为或决策。

### OQ-4 错误字段命名残留开放项收敛（已收敛）

- 状态：已收敛
- 收敛日期：2026-06-20
- 决策依据： [ADR 0006：API 错误模型](../adr/api/0006-api-error-model.md)
- 关键锚点：
	- `Decision`：确认 `error.code`、`error.category`、`error.retryable`、`error.message`、`error.messageKey` 以及字段级错误承载规则。
	- `Consequences`：确认错误处理可分组、可重试、可定位，错误字段命名口径稳定。
- 收敛结果：
	- 错误字段命名相关“开放项/待确认项”表述已完成收敛。
	- 契约文档中相关段落改为已确认口径，不新增行为或决策。


## 维护建议

1. 持续同步 [正式 OpenAPI v1](openapi-v1.yaml)、[API 示例](examples/README.md) 与本说明文档，避免契约漂移。
2. OQ-1（幂等）、OQ-2（审计映射）、OQ-3（响应与批量字段命名）与 OQ-4（错误字段命名）已完成收敛并同步 ADR 与契约说明；后续新增 OQ 需在“开放议题集中清单”登记状态，并在收敛当日同步更新对应 ADR 与契约条目。
