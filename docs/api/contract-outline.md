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
- 可调用版本列表路径为 `GET /api/{environment}/v1/templates/{templateId}/versions`；该接口返回当前授权视角下可调用的发布版本列表，不作为后台版本管理列表。
- API v1 请求头字段确认为 `X-Api-Credential-Id`、`X-Api-Credential-Secret`、`X-Access-Account`；可选追踪请求头为 `X-Trace-Id`。
- `X-Trace-Id` 传入时平台沿用该值作为响应和审计中的 `traceId`；未传入时由平台生成 `traceId`。
- `releaseVersion` 路径参数采用语义化版本号，例如 `1.0.0`、`1.1.0`、`2.0.0`。
- default 路径不得隐式指向最新版本，必须由全局管理员或分组管理员在 API 管理中显式配置目标发布版本。
- API 调用权限是模板级别。
- API 采用 API 凭证 + AD Group 双重认证授权。
- AD Group 解析规则适用于所有需要 AD Group 授权的 API 操作，包括生成、批量生成、异步任务查询、异步任务取消、下载取文件、API 契约查看和可调用版本列表。
- AD Group 成功解析结果按 `accessAccount` + `environment` 缓存 5 分钟；不缓存解析失败结果。
- AD Group 解析失败时，如果存在未过期缓存，则使用未过期缓存继续授权；如果不存在未过期缓存，则返回 `503 AD_GROUP_RESOLUTION_FAILED`，`retryable=true`。
- AD Group 授权不得使用过期缓存兜底；过期缓存不能作为授权依据。
- API 管理中的 AD Group 授权配置变更立即生效，并清理相关授权缓存；目录中的 AD Group 成员变更在目录同步完成且平台缓存过期后生效。
- API 凭证对象是调用方级身份，可授权到多个模板 API；模板调用仍必须同时满足 API 凭证授权、AD Group 授权和模板级授权。
- API 凭证创建和轮换时，secret 明文只展示一次；平台只保存不可逆摘要或指纹，不允许管理员后续重新查看 secret 明文。
- API 凭证必须设置有效期；默认有效期为 180 天，最长 365 天，管理员可设置更短有效期。
- API 凭证状态集合确认为 `ACTIVE`、`EXPIRING_SOON`、`EXPIRED`、`REVOKED`。
- API 凭证轮换时，新 secret 立即可用，旧 secret 保留 7 天宽限期；宽限期结束后旧 secret 失效。
- API 凭证吊销立即生效，阻断该凭证的所有后续 API 操作，包括新生成、异步任务查询、异步任务取消和下载取文件。
- API 管理由全局管理员和分组管理员承担，不设置独立 API 管理员角色。
- API 管理配置当前按模板级绑定；一个模板对应一组 API 管理配置，适用于该模板下所有未停用的发布版本。
- 发布版本继续锁定模板内容、变量、规则和发布版本契约；API 管理配置作为调用侧策略独立维护，不要求重新发布模板。
- 可调用版本列表从模板下发布版本生成；模板停用或废弃时所有发布版本不可调用，单个发布版本停用时仅该版本不可调用；模板或发布版本恢复后，恢复对象重新进入可调用候选范围，但仍受模板状态、发布版本状态和模板级 API 管理配置约束。
- 模板发布后需要生成接口地址、请求参数 Schema、响应 Schema、字段校验规则、示例请求/响应、错误码说明、契约版本对比和可调用版本列表。
- API 支持 DOCX/PDF 输出、同步文件流、同步下载地址、异步任务返回任务 ID、批量生成。
- 同步文件流响应中，文件内容放在响应体，核心元数据通过响应头承载。
- 同步文件流响应头核心元数据包括 `auditId`、`traceId`、`requestId`、`idempotencyStatus`、`documentId`、`templateId`、`routeType`、`resolvedReleaseVersion`、`output.format`、`output.mode`。
- 同步下载地址和异步结果下载地址采用短有效期策略；下载时需要二次授权，校验 API 凭证、AD Group 和模板级授权；有效期内允许多次下载。
- 异步任务查询需要返回任务状态、响应元数据、成功结果或统一错误明细；异步批量任务需要返回批次汇总和单笔成功/失败明细。
- 异步任务 v1 支持受控取消；仅未完成且未过期的任务可取消，取消成功后状态为 `CANCELLED`，且不返回已生成结果、下载地址或异步批量单笔成功结果。
- 异步任务查询不返回进度百分比；异步批量任务通过 `batch.summary` 返回进度摘要。
- 异步任务状态集合确认为 `ACCEPTED`、`PROCESSING`、`SUCCEEDED`、`FAILED`、`PARTIAL_SUCCEEDED`、`EXPIRED`、`CANCELLED`；`PARTIAL_SUCCEEDED` 仅用于异步批量任务。
- 异步任务和生成结果默认保留 7 天。
- 批量请求支持批次级统一输出和加密配置，也允许单笔记录单独覆盖输出格式、输出模式和加密参数。
- 批量请求中每笔记录的单独覆盖都必须受模板级 API 管理配置约束，不能绕过输出方式、批量上限或动态加密能力限制。
- 批量请求中每笔记录必须传入 `items[].itemId`，且同一批次内必须唯一；重复 `items[].itemId` 返回 `400 ITEM_ID_DUPLICATED`，不创建批次或异步任务。
- 同步批量中任一记录因参数校验或 API 管理策略失败时，整批失败且不生成任何文件；响应需要返回每笔失败明细，并按非重试幂等结果记录。
- 异步批量部分成功后的失败项重试必须使用新批次和新的 `idempotencyKey`，通过 `originalBatchId` 或等效字段关联原批次，原批次结果不被扩展或改写。
- API 支持 DOCX/PDF 动态加密参数，是否允许加密以及可用加密能力由 API 管理配置控制。
- `encryption.enabled=true` 时，`openPassword` 必填，`ownerPassword` 可选；`permissions` 采用统一抽象权限枚举，按 DOCX/PDF 输出格式映射到对应加密能力，且传入 `permissions` 时必须同时传入 `ownerPassword`。
- `encryption.enabled=false` 或未传 `enabled` 时，如果仍传入 `openPassword`、`ownerPassword` 或 `permissions`，返回 `400 ENCRYPTION_PARAMETER_INVALID`，不得静默忽略。
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
- `context` 采用安全白名单，v1 仅允许 `sourceSystem`、`channel`、`businessRequestId`、`upstreamTraceId`、`scenario`、`locale`；字段值均为字符串；未知 `context` 字段返回 `400 REQUEST_BODY_INVALID`。
- API 管理配置展示字段 v1 基线确认为 `apiPolicy.policyVersion`、`apiPolicy.updatedAt`、`apiPolicy.updatedBy`、`apiPolicy.allowedOutputFormats`、`apiPolicy.allowedOutputModes`、`apiPolicy.batchLimits.syncMaxItems`、`apiPolicy.batchLimits.asyncMaxItems`、`apiPolicy.encryptionCapabilities`、`apiPolicy.adGroupAuthorizationSummary`、`apiPolicy.credentialSummary`。
- 异步任务受理响应返回 `task.queryPath`，值为任务查询相对路径，不是免认证或签名地址；后续查询仍需 API 凭证、AD Group 和模板级授权。
- v1 采用统一授权判定基线；文档生成、批量生成、异步任务查询、异步任务取消、下载取文件、API 契约查看、可调用版本列表和 API 管理均在业务处理或敏感响应返回前完成对应授权判定。
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
| 查看 API 契约信息 | 让管理员、模板编排人员和被授权 API 调用方查看模板 API 契约、请求/响应结构、错误码和示例。 | 路径、查看权限、default 路径展示字段、契约响应范围已确认。 |
| 查看可调用版本列表 | 返回授权模板下当前可调用的发布版本列表。 | 路径、可调用版本规则和列表用途已确认。 |
| 查看内容模块治理契约 | 让管理员查看内容模块审批和生命周期管理接口的请求/响应结构、错误码和示例。 | 内容模块治理路径已随 OpenAPI v1 维护；查看范围沿用管理员契约可见性。 |
| 单笔生成 | 基于模板、发布版本和请求参数生成一份 DOCX/PDF。 | 路径命名、请求字段命名、响应 envelope 和 Schema 载体已确认；正式 OpenAPI v1 Schema 和示例已输出，后续随契约变更维护。 |
| 批量生成 | 基于同一模板和发布版本提交多笔生成请求。 | 独立 `batch-generate` 路径、默认上限、失败策略、`itemId` 必填唯一、重复 `itemId` 处理、同步失败明细、异步失败项重试策略、字段命名和全量明细返回已确认。 |
| 查询异步任务 | 查询异步生成任务状态、结果和错误明细。 | 查询路径、结果结构、状态命名和进度摘要已确认；不返回进度百分比。 |
| 取消异步任务 | 取消未完成且未过期的异步任务。 | 受控取消路径、授权方式、终态和不可取消错误码已确认。 |
| 获取下载地址文件 | 使用同步或异步返回的下载地址获取生成文件。 | 下载路径、15 分钟固定有效期、二次授权、多次下载、不可配置为一次性、过期不重新签发和结果保留已确认。 |

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
| API 契约查看 | 调用方查看当前授权模板的契约摘要、路径、策略、错误码和示例索引。 | 返回契约摘要，不内嵌完整 OpenAPI YAML。 | `/api/{environment}/v1/templates/{templateId}/contract` |
| 可调用版本列表 | 调用方查看当前授权视角下可调用的发布版本列表。 | 返回可调用发布版本，不作为后台版本管理列表。 | `/api/{environment}/v1/templates/{templateId}/versions` |

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

List callable release versions
/api/{environment}/v1/templates/{templateId}/versions
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

| 路由语义 | 用途 | 已确认规则 | 已确认路径 |
| --- | --- | --- | --- |
| 内容模块审批流转 | 对条款或内容模块版本执行提交、审批通过或审批不通过。 | 使用独立版本审批生命周期；审批前置条件和角色边界遵循权限矩阵与领域模型。 | `/api/{environment}/v1/admin/content-modules/{moduleId}/review/transition` |
| 内容模块生命周期操作 | 对条款或内容模块执行停用、恢复或废弃治理操作。 | 停用、恢复和废弃由管理员执行；执行前必须进行影响分析、二次确认并记录审计。 | `/api/{environment}/v1/admin/content-modules/{moduleId}/lifecycle/operation/apply` |

内容模块治理契约的正式字段与响应结构以 [OpenAPI v1](openapi-v1.yaml) 为准；本文档仅提供索引和语义解释。

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

加密参数确认：`encryption.enabled=true` 时，`openPassword` 必填，`ownerPassword` 可选。`permissions` 采用统一抽象权限枚举，按 DOCX/PDF 输出格式映射到对应加密能力；传入 `permissions` 时必须同时传入 `ownerPassword`。`encryption.enabled=false` 或未传 `enabled` 时，如果仍传入 `openPassword`、`ownerPassword` 或 `permissions`，返回 `400 ENCRYPTION_PARAMETER_INVALID`，不得静默忽略。

密码强度确认：`openPassword` 和 `ownerPassword` 最少 12 字符、最长 128 字符；如果两者同时传入，二者必须不同。不满足时返回 `400 ENCRYPTION_PARAMETER_INVALID`。

加密权限枚举确认：`permissions` 使用允许类抽象枚举，v1 取值为 `ALLOW_PRINT`、`ALLOW_COPY`、`ALLOW_EDIT`、`ALLOW_ANNOTATE`、`ALLOW_FORM_FILL`。open/view 能力由 `openPassword` 控制，不放入 `permissions` 枚举。

加密失败确认：加密参数合法但实际加密处理失败时，返回 `500 ENCRYPTION_FAILED`，`retryable=true`；错误响应、日志和审计不得返回密码、内部加密细节或敏感配置值。同步/异步和批量场景沿用已确认的失败承载规则。

请求体字段确认：`templateId` 和 `releaseVersion` 不允许在请求体中重复表达；如果请求体重复传入这些路径字段，按 `400 REQUEST_BODY_INVALID` 处理。

## context 字段白名单确认

确认基线：`context` 只用于调用方非敏感追踪和分组排查信息，不用于传递模板变量、客户信息或生成控制参数。

| 字段名 | 语义 | 约束 |
| --- | --- | --- |
| `sourceSystem` | 调用来源系统。 | 字符串；不得放入 API secret 或内部敏感配置。 |
| `channel` | 调用渠道。 | 字符串；用于渠道统计或排查。 |
| `businessRequestId` | 调用方业务请求关联标识。 | 字符串；不得直接使用客户姓名、账号、证件号、金额或合同全文。 |
| `upstreamTraceId` | 上游链路追踪标识。 | 字符串；用于跨系统排查。 |
| `scenario` | 调用场景。 | 字符串；用于非敏感场景分类。 |
| `locale` | 调用方期望语种或地区提示。 | 字符串；不替代模板变量或输出规则。 |

`context` 未列出的字段按未知字段处理，返回 `400 REQUEST_BODY_INVALID`。`context` 不得包含客户姓名、证件号、账号、金额、密码、模板变量原值、完整请求体、API secret、完整下载地址或完整 AD Group 成员等敏感内容。审计中使用 `contextSummary` 记录必要摘要。

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
- v1 请求采用严格字段校验，契约 Schema 之外的未知字段返回 `400 REQUEST_BODY_INVALID`，字段级原因使用 `UNKNOWN_FIELD`。
- 模板标识和发布版本号只通过路径表达，请求体重复传入 `templateId` 或 `releaseVersion` 也按未知或不允许字段处理。
- v1 兼容变更应优先采用向后兼容的新增可选字段、枚举扩展或说明增强；破坏性字段重命名、必填字段新增或语义变更需要新的 API 版本或单独兼容策略确认。

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

### 幂等响应与审计字段状态

| 语义字段 | 候选字段名 | 建议语义 | 当前状态 |
| --- | --- | --- | --- |
| 幂等标识 | `idempotencyKey` | 回显本次请求使用的幂等标识。 | 已确认：生成类成功和错误响应统一回显；同步文件流通过响应头回显。 |
| 幂等处理状态 | `idempotencyStatus` | 表达新请求、重复命中、冲突等状态。 | 枚举值已确认为 `IDEMPOTENCY_NEW`、`IDEMPOTENCY_REPLAYED`、`IDEMPOTENCY_CONFLICTED`。 |
| 原始请求时间 | `originalRequestAt` | 重复命中或幂等冲突安全摘要中表达原请求受理时间。 | 已确认：重复命中成功场景固定返回。 |
| 原始任务标识 | `task.taskId` | 重复命中异步请求时返回原任务。 | 已确认：重复命中异步请求返回完整 `task` 对象。 |
| 原始批次标识 | `batchId` | 重复命中批量请求时返回原批次。 | 已确认。 |
| 失败项重试原批次标识 | `originalBatchId` | 异步批量失败项以新批次重试时关联原批次。 | 字段名和语义已确认。 |

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

`fidelityWarnings[]` 中每个对象必须包含：

- `warningCode`：稳定警告码，v1 取值为 `OPTIONAL_CONTENT_EMPTY`、`LOW_RISK_PAGINATION_DIFFERENCE`、`LOW_RISK_TABLE_PAGE_BREAK`、`CONTROLLED_STYLE_FALLBACK` 和 `IMAGE_SCALING_ADJUSTED`。
- `messageKey`：用于本地化和前端展示的稳定文案键。
- `message`：默认可读提示。
- `locationSummary`：影响位置摘要，例如锚点、章节、组件或页码范围摘要，不返回敏感正文。
- `detectedSummary`：检测结果摘要，不返回变量原值、客户数据、粘贴原文或完整生成内容。
- `recommendation`：处理建议。
- `sensitiveDataExcluded`：固定为 `true`，表达该警告明细已经过敏感数据排除处理。

单笔 JSON 成功响应在 `result.fidelityWarnings[]` 返回保真警告；批量成功项在 `result.batch.items[].fidelityWarnings[]` 返回保真警告；异步任务查询在生成完成后按单笔或批量结果层级返回保真警告。同步文件流响应使用 `fidelityWarningCount` 和 `fidelityWarningCodes` 响应头返回摘要，完整明细进入审计摘要。

同步文件流响应头确认字段：`auditId`、`traceId`、`requestId`、`idempotencyKey`、`idempotencyStatus`、`documentId`、`templateId`、`routeType`、`resolvedReleaseVersion`、`output.format`、`output.mode`、`fidelityWarningCount` 和 `fidelityWarningCodes`。

后台 API 契约页提供调用方视图，展示授权模板的契约版本对比、错误码说明、调用示例、可调用版本列表、API 策略摘要、调用方自身 API 凭证非敏感状态、保真警告码目录、字段含义、JSON 示例、同步文件流响应头说明，以及授权范围内的非敏感调用结果警告摘要和 `traceId` 或 `auditId` 定位标识。契约版本对比由页面基于已授权可见的现有契约信息、可调用版本、请求 Schema、响应 Schema、错误码、API 策略、路由/default 目标和示例计算展示；v1 不在 `ContractResponse` 中新增专门的契约版本对比字段。v1 不建设独立开发者门户；该视图不展示完整审计明细、完整请求体、模板变量原值、客户数据、完整生成内容或 API 凭证 secret，也不提供 API 凭证自助管理。

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

结果清理确认：生成结果 7 天到期清理前不主动通知调用方或管理员，仅记录清理审计。

## 异步任务查询与取消接口确认

确认基线：异步任务查询需要返回任务状态、响应元数据、成功结果或统一错误明细；异步批量任务需要返回批次汇总和单笔成功/失败明细。异步任务和生成结果默认保留 7 天。

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
| `TEMPLATE_CONTRACT` | `TEMPLATE_CONTRACT_INVALID` | `api.error.templateContract.templateContractInvalid` | `false` | Template contract is invalid. |
| `TEMPLATE_CONTRACT` | `TEMPLATE_ANCHOR_MISSING` | `api.error.templateContract.templateAnchorMissing` | `false` | Template anchor is missing. |
| `GENERATION` | `DOCX_GENERATION_FAILED` | `api.error.generation.docxGenerationFailed` | `true` | DOCX generation failed. |
| `GENERATION` | `PDF_CONVERSION_FAILED` | `api.error.generation.pdfConversionFailed` | `true` | PDF conversion failed. |
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
| 403 Forbidden | `AD_GROUP_NOT_AUTHORIZED`、`TEMPLATE_ACCESS_DENIED`。 | 调用方已被识别，但未获得模板 API 访问授权；消息不得泄露未授权资源细节。 |
| 404 Not Found | `RELEASE_VERSION_NOT_FOUND`、`ASYNC_TASK_NOT_FOUND`、`DOCUMENT_NOT_FOUND`。 | 授权范围内请求的发布版本、任务或文档不存在。 |
| 409 Conflict | `RELEASE_VERSION_DISABLED`、`DEFAULT_ROUTE_NOT_CONFIGURED`、`DEFAULT_ROUTE_TARGET_UNAVAILABLE`、`TEMPLATE_DISABLED`、`TEMPLATE_DEPRECATED`、`IDEMPOTENCY_KEY_CONFLICT`、`IDEMPOTENCY_RETRY_NOT_ALLOWED`、`ASYNC_TASK_CANCELLATION_NOT_ALLOWED`。 | 请求与当前版本、模板、default 配置、幂等状态或异步任务当前状态冲突。 |
| 410 Gone | `DOWNLOAD_URL_EXPIRED`、`RESULT_RETENTION_EXPIRED`、`ASYNC_TASK_EXPIRED`。 | 资源曾可用，但下载地址、任务或结果已过期。 |
| 422 Unprocessable Entity | `VARIABLE_REQUIRED`、`VARIABLE_TYPE_INVALID`、`VARIABLE_FORMAT_INVALID`、`VARIABLE_RULE_FAILED`。 | 请求结构可解析，但模板变量或业务规则校验未通过。 |
| 500 Internal Server Error | `TEMPLATE_CONTRACT_INVALID`、`TEMPLATE_ANCHOR_MISSING`、`DOCX_GENERATION_FAILED`、`PDF_CONVERSION_FAILED`、`ENCRYPTION_FAILED`、`BATCH_PROCESSING_FAILED`。 | 平台处理、模板资产、生成、转换、加密或整批处理失败。 |
| 503 Service Unavailable | `AD_GROUP_RESOLUTION_FAILED`、`IDEMPOTENCY_STORE_UNAVAILABLE`、`GENERATION_SERVICE_UNAVAILABLE`。 | 权限依赖、幂等存储或生成服务临时不可用。 |
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
						"code": "VARIABLE_REQUIRED",
						"category": "VALIDATION",
						"message": "Required variable is missing.",
						"messageKey": "api.error.validation.variableRequired",
						"retryable": false,
						"fieldErrors": [
							{
								"field": "items[1].variables.customerName",
								"reason": "REQUIRED",
								"message": "Customer name is required."
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
| 错误类别 | `error.category` | 便于上游按大类处理。 | 10 类固定集合已确认。 |
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
- API 凭证状态集合确认为 `ACTIVE`、`EXPIRING_SOON`、`EXPIRED`、`REVOKED`。
- `EXPIRING_SOON` 用于到期前提醒窗口，轮换状态由当前 secret 与旧 secret 宽限期表达，不新增凭证级 `ROTATING` 状态。
- API 凭证轮换时，新 secret 立即可用，旧 secret 保留 7 天宽限期；宽限期结束后旧 secret 失效。
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
| 到期时间 | 当前凭证到期时间。 | 默认 180 天、最长 365 天已确认；纳入 `apiPolicy.credentialSummary` 表达。 |
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
| 发布版本与可调用列表 | 显式发布版本号、未停用发布版本列表、模板停用/废弃和版本停用后的可调用判断。 | 模板级 API 管理配置基线、契约查看接口和可调用版本列表响应格式已确认。 |
| 请求 Schema | 模板变量、规则校验、输出格式、输出模式、批量输入、加密参数。 | 字段命名、路径/请求体边界、OpenAPI 3.1 YAML 载体和严格未知字段策略已确认；正式 OpenAPI v1 已输出，后续随契约变更维护。 |
| 幂等策略 | `requestId`、`idempotencyKey`、`itemId`、重复提交处理、default 路径幂等、幂等保留期限。 | 生成类 API 必填范围、唯一性范围、7 天保留、过期后按新请求处理、default 变更后冲突、失败后按 `retryable` 决定、幂等状态枚举、冲突安全摘要、过期 key 复用审计标记、批量 `itemId` 必填唯一、重复 `itemId` 处理、同步批量失败幂等记录和异步失败项重试策略已确认。 |
| 响应 Schema | 文件流、下载地址、异步任务 ID、批量成功/失败明细、通用响应元数据。 | JSON envelope、`metadata`/`result`/`error` 承载方式、字段命名和批量全量明细返回已确认；正式 OpenAPI v1 已输出，后续随契约变更维护。 |
| 错误码 | 认证失败、AD Group 解析失败、授权失败、版本不可用、参数校验失败、锚点缺失、生成失败、加密失败、批量部分失败、异步任务、异步取消和下载取文件失败。 | v1 基线错误码清单、10 类 `error.category`、默认 `retryable`、英文 `message`、`messageKey` 命名规则、字段级 `reason` 枚举、HTTP 状态码映射、通用安全消息策略和重点场景错误响应示例已确认。 |
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
