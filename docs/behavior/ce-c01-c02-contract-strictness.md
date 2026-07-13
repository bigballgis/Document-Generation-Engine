# BDD 行为规格：CE-C01+C02 运行时契约严格化（context + unknown-field）

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-14  
**BDD ID 前缀**: `BDD-CE-C01` / `BDD-CE-C02`  
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) Wave CE-C · CE-C01 / CE-C02  
**Slice**: `ce-c01-c02-contract-strictness`（计划 §9：C01+C02 同一 PR 链）  
**Worktree**: `D:/working/DGE-ce-c01-c02-contract-strictness` · `feat/ce-c01-c02-contract-strictness`  
**授权依据**: 用户 2026-07-14 确认 ADR 决策 D2 / D3 / D4；以及已确认的 requirements / PRD / OpenAPI / permission-matrix 中的 `context` 白名单与严格字段校验基线  
**完成声明约束**: 本切片关闭契约诚信缺口 CE-C01+C02；**不**宣称 go-live；**不**实现 CE-C03/C04/C05/C06；**不**实现 `SYNC_DOWNLOAD_URL` 重签下载。

---

## 1. 概述

运行时动态生成 API（单笔 / 批量 / 同步 / 异步）的请求体必须与 OpenAPI v1 **真实对齐**：

| 行为域 | 摘要 |
| --- | --- |
| **CE-C01 `context` 白名单落地** | `GenerateRequestBody` / `BatchGenerateRequestBody` 增加可选 `context`（6 字段 record）；未知子字段 → `400 REQUEST_BODY_INVALID`；`InvocationParameterSanitizer` 写入 `contextSummary` |
| **CE-C02 unknown-field 严格校验** | **仅** runtime 请求 DTO 使用 `fail-on-unknown-properties`；契约外字段 → 统一 `400 REQUEST_BODY_INVALID` + `fieldErrors[].reason=UNKNOWN_FIELD`；**不**改管理端 DTO |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| OpenAPI 已声明 `context` / `additionalProperties: false` | `docs/api/openapi-v1.yaml` `GenerateRequest` / `BatchGenerateRequest` / `Context` |
| Java DTO 无 `context`（仅 5 字段） | `GenerateRequestBody`：`output` / `variables` / `encryption` / `requestId` / `idempotencyKey` |
| 批量 DTO 无 `context`（亦无 `originalBatchId`） | `BatchGenerateRequestBody` |
| 未知字段静默忽略 | 默认 Jackson 行为；与契约「严格字段校验」漂移 |
| 审计 sanitizer 未写 `contextSummary` | `InvocationParameterSanitizer` 仅 variablesHash / output / encryption |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **API 调用方** | Runtime caller | 持 API 凭证 + AD Group，调用动态生成 API；可传可选 `context` |
| **系统** | Runtime API + sanitizer + 错误信封 | 严格反序列化；白名单校验；摘要审计；统一错误信封 |
| **审计 / 管理端读者** | Audit / admin | 仅见 `contextSummary` 等摘要；不得见敏感明文（既有矩阵） |
| **调用方记录读者** | Caller invocation query | 在既有「自身调用记录」授权下可读完整 `context`（见既有包级调用记录规格；本片不改授权） |

---

## 3. Goal

1. 调用方可在单笔/批量生成请求中传入可选 `context`（6 个白名单字符串字段），系统接受并参与参数摘要。  
2. `context` 未知子字段、或 runtime 请求体契约外字段，一律 **fail-closed**：HTTP **400**，`error.code=REQUEST_BODY_INVALID`。  
3. OpenAPI / Markdown 契约与 runtime 实现一致：声明的字段可被绑定；声明为禁止的未知字段不可静默忽略。  
4. 管理端请求 DTO 的反序列化严格性 **本片不变**。  
5. 遵守已拍板 ADR：D2 immediate-only；D3 无 webhook；D4 `SYNC_DOWNLOAD_URL` 继续 defer（契约/文档对齐「不重签」，本片不实现下载 URL）。

---

## 4. 已确认决策（confirmed）

### 4.1 产品 / 契约基线（既有 SoT，本片落地）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **CTX-1** | `context` 可选；仅用于非敏感追踪/排查；**不是**模板变量或生成控制参数 | PRD / requirements-plan / contract-outline |
| **CTX-2** | v1 白名单恰好 6 字段，值均为 **string**：`sourceSystem`、`channel`、`businessRequestId`、`upstreamTraceId`、`scenario`、`locale` | OpenAPI `Context`；permission-matrix |
| **CTX-3** | `context` 未知子字段 → `400 REQUEST_BODY_INVALID`（`UNKNOWN_FIELD`） | 同上 |
| **CTX-4** | `context` 不得承载客户姓名、证件号、账号、金额、密码、模板变量原值、完整请求体、API secret、完整下载地址、完整 AD Group 成员等敏感内容（调用方责任 + 平台摘要侧不扩大暴露面） | permission-matrix / ADR-0020 |
| **UF-1** | v1 runtime 请求采用严格字段校验；契约 Schema 之外未知字段 → `400 REQUEST_BODY_INVALID`，字段级 `reason=UNKNOWN_FIELD` | contract-outline / OpenAPI `FieldError` |
| **UF-2** | 请求体重复传入路径字段 `templateId` / `releaseVersion` → 按不允许/未知字段处理 → `400 REQUEST_BODY_INVALID` | contract-outline |
| **UF-3** | **本片仅**收紧 **runtime** 生成相关请求 DTO；**禁止**改动管理端 DTO 的 unknown-field 策略 | CE 计划 CE-C02 |
| **ERR-1** | 错误信封：`metadata` + `error`；`error` 必含 `code`、`category`、`message`、`messageKey`、`retryable`；未知字段场景附加 `fieldErrors[]`（`field` / `reason` / `message`） | OpenAPI `ErrorEnvelope` / `ErrorDetail` / `FieldError` |
| **ERR-2** | `REQUEST_BODY_INVALID`：`category=VALIDATION`，`messageKey=api.error.validation.requestBodyInvalid`，`retryable=false`，HTTP **400** | contract-outline 错误表 |
| **AUD-1** | 审计/调用参数摘要写入 `contextSummary`；不得写入模板变量原值、加密密码、完整请求体、secret 等 | permission-matrix / ADR-0013 |

### 4.2 本会话用户确认（2026-07-14）

| ID | 决策 |
| --- | --- |
| **D2** | 维持 ADR-0007 **immediate-only**（本片无定时生效语义） |
| **D3** | 维持 ADR-0008 **poll-only**；不做 webhook |
| **D4** | 维持 ADR-0038：`SYNC_DOWNLOAD_URL` + 下载重签 **继续 deferred**；本片可做契约/文档对齐并写清「不重签」，**不实现**该 mode 的运行时交付 |

### 4.3 本片范围锁定（confirmed for this slice）

| ID | 决策 |
| --- | --- |
| **S-1** | 覆盖端点：单笔 sync/async generate、批量 sync/async generate（路径参数模板 + 默认路由 / 显式版本，凡绑定 `GenerateRequestBody` / `BatchGenerateRequestBody` 者） |
| **S-2** | `context` 在单笔与批量请求体均为可选；字段缺失或 `context: null` 等价于「无 context」 |
| **S-3** | `context.locale` 本片 **仅接受并摘要**；**不**改变渲染/格式化行为（消费方为后续 CE-K03） |
| **S-4** | `variables` 仍为 schema 驱动的开放对象（`additionalProperties: true`）；**不对** `variables` 内未知键做 CE-C02 的 `UNKNOWN_FIELD`（变量未知键由既有模板契约/规则校验） |
| **S-5** | nested runtime 对象（`output`、`encryption`、`context`、batch `items[]` 元素及其嵌套 `output`/`encryption`）同样禁止契约外属性 |
| **S-6** | OpenAPI 已声明、本片必须可绑定的批量字段：`context`；对 OpenAPI 已声明的 `originalBatchId`：**本片在 DTO 上接受为可选 string，不做 CE-C05 血缘校验**（不查原批次、不写血缘审计扩展），避免严格校验与契约声明冲突；完整语义留给 CE-C05 |
| **S-7** | `InvocationParameterSanitizer`（及等价审计参数 JSON）在存在 `context` 时写入 `contextSummary`：仅含白名单键的**非空**字符串值摘要对象（或等价安全摘要）；缺省 context 时不伪造假字段 |
| **S-8** | 明确非目标：CE-C03 fidelityWarnings 形态、CE-C04 expires_at、CE-C05 血缘校验、CE-C06 DOCX permissions、管理 UI、webhook、`SYNC_DOWNLOAD_URL` 实现、定时发布 |

---

## 5. Trigger

| # | 触发 |
| --- | --- |
| T1 | 调用方对已授权模板发起单笔/批量生成（sync 或 async），请求体含合法可选 `context` |
| T2 | 请求体 `context` 含白名单外子字段 |
| T3 | 请求体顶层或 nested runtime 对象含契约外字段（含 `templateId`/`releaseVersion` 体重复） |
| T4 | 管理端 API 请求体含「契约外」字段（回归：行为不因本片变严） |

---

## 6. Preconditions

- 调用方凭证有效、AD Group 授权通过、模板/版本可路由（本片不改授权/路由）。  
- 目标模板策略允许所用 `output.format` / `output.mode`（除 D4 已 defer 的 `SYNC_DOWNLOAD_URL` 仍按 ADR-0038 拒绝）。  
- OpenAPI v1 与 `docs/api/contract-outline.md` 中 `Context` / 严格校验条款为行为权威。  
- 工作树：`feat/ce-c01-c02-contract-strictness`。

---

## 7. Primary journey（成功）

1. 调用方构造生成请求：必填 `output`、`variables`（或批量 `items`）、`requestId`、`idempotencyKey`；可选 `encryption`、`context`（0–6 白名单键）。  
2. Runtime 反序列化：**拒绝**未知属性；绑定 `context` record。  
3. 既有校验链（策略、变量、加密等）通过。  
4. 生成成功（或异步受理成功）。  
5. 参数 sanitizer / 审计摘要含 `contextSummary`（当 context 有非空白名单值时）。  
6. （可选）调用方查询自身 invocation 详情时，完整 `context` 按既有调用记录规格回读（本片保证写入/可回读链路不丢字段）。

---

## 8. System responses

### 8.1 成功

- HTTP 成功码与既有 generate/batch/async 契约一致（本片不改成功信封形状）。  
- 请求中的合法 `context` 被保留用于摘要与（调用方）记录回读。  
- `contextSummary` 出现在审计/参数摘要中且不含敏感扩大面。

### 8.2 失败（本片新增/收紧）

| 条件 | HTTP | `error.code` | `error.category` | `messageKey` | `retryable` | `fieldErrors` |
| --- | --- | --- | --- | --- | --- | --- |
| `context` 未知子字段 | 400 | `REQUEST_BODY_INVALID` | `VALIDATION` | `api.error.validation.requestBodyInvalid` | `false` | `field` 如 `context.<name>`，`reason=UNKNOWN_FIELD` |
| runtime 请求契约外字段 | 400 | 同上 | 同上 | 同上 | `false` | `field` 为点路径，`reason=UNKNOWN_FIELD` |
| `context` 某键非 string（如 number/object） | 400 | 同上 | 同上 | 同上 | `false` | `reason=INVALID_TYPE`（或等价类型失败映射到同一信封） |

失败响应必须：

- 使用统一 `ErrorEnvelope`（`metadata` + `error`）。  
- `error.message` 为英文业务可读句，**不**回显 API secret / 变量原值 / 密码。  
- 至少一条 `fieldErrors` 指向违规字段路径（多字段可多条）。  
- **不得**部分生成 / 不得静默丢弃未知字段后继续成功。

### 8.3 管理端（非目标但须回归）

- 管理端 DTO 在本片后 **不得** 仅因「加了全局 fail-on-unknown」而突然对既有客户端变严。  
- 实现须使用 **runtime 专用** ObjectMapper / 反序列化配置，而非改全局默认。

---

## 9. Acceptance scenarios（Given / When / Then）

### CE-C01 — `context` 白名单

#### BDD-CE-C01-001 — 单笔生成接受完整白名单 context

**Given** 调用方已授权某已发布模板，且构造合法单笔 generate 请求  
**And** `context` 含全部 6 键：`sourceSystem`、`channel`、`businessRequestId`、`upstreamTraceId`、`scenario`、`locale`（均为非空字符串）  
**When** 调用单笔 sync（或 async）生成 API  
**Then** 请求被接受（不因 `context` 失败）  
**And** 参数摘要 / 审计侧出现 `contextSummary`，覆盖上述非空键  
**And** 不因本片改变文档字节语义（locale 不触发格式化变更）

#### BDD-CE-C01-002 — 单笔生成接受部分 / 空缺 context

**Given** 合法单笔请求且 `context` 仅含 `channel` 与 `locale`，或省略 `context`  
**When** 调用生成 API  
**Then** 请求被接受  
**And** 省略 `context` 时摘要中不出现伪造的空 context 对象键集；仅含提供的非空白名单键（若有）

#### BDD-CE-C01-003 — 批量生成接受 context

**Given** 合法批量请求体含可选 `context`（白名单子集）  
**When** 调用批量 sync 或 async 生成 API  
**Then** 请求被接受  
**And** 批量参数摘要含对应 `contextSummary`

#### BDD-CE-C01-004 — context 未知子字段被拒绝

**Given** 合法生成请求骨架  
**And** `context` 含白名单外键（例如 `customerName` 或 `debugFlag`）  
**When** 调用单笔或批量生成 API  
**Then** HTTP **400**  
**And** `error.code=REQUEST_BODY_INVALID`，`category=VALIDATION`，`messageKey=api.error.validation.requestBodyInvalid`，`retryable=false`  
**And** `error.fieldErrors` 至少一条：`reason=UNKNOWN_FIELD`，`field` 指向该 context 子路径  
**And** 不产生成功生成结果 / 不创建成功受理任务（fail-closed）

#### BDD-CE-C01-005 — context 字段类型错误

**Given** `context.locale`（或其它白名单键）值为非字符串（例如 `123` 或 `{}`）  
**When** 调用生成 API  
**Then** HTTP **400**，`error.code=REQUEST_BODY_INVALID`  
**And** `fieldErrors` 指示该字段，`reason=INVALID_TYPE`（或平台统一的类型失败 reason，但仍为同一 error.code）

#### BDD-CE-C01-006 — contextSummary 不含敏感扩大面

**Given** 请求含合法 `context` 与含 PII 的 `variables`  
**When** 生成流程写入审计 / 参数摘要  
**Then** `contextSummary` 仅反映 context 白名单摘要  
**And** 摘要 **不** 含 `variables` 原值、encryption 密码、API secret

### CE-C02 — unknown-field 严格校验

#### BDD-CE-C02-001 — 顶层未知字段被拒绝

**Given** 合法单笔请求另含契约外顶层字段（例如 `"foo":"bar"`）  
**When** 调用生成 API  
**Then** HTTP **400**，`REQUEST_BODY_INVALID`，`fieldErrors[].reason=UNKNOWN_FIELD`  
**And** 不得静默忽略后成功

#### BDD-CE-C02-002 — 请求体重复路径字段被拒绝

**Given** 单笔或批量请求体含 `templateId` 和/或 `releaseVersion`  
**When** 调用生成 API  
**Then** HTTP **400**，`REQUEST_BODY_INVALID`，对应字段 `UNKNOWN_FIELD`（或不允许字段的等价字段错误）

#### BDD-CE-C02-003 — nested `output` / `encryption` 未知属性被拒绝

**Given** `output` 或 `encryption` 对象含契约外属性  
**When** 调用生成 API  
**Then** HTTP **400**，`REQUEST_BODY_INVALID`，`UNKNOWN_FIELD` 指向嵌套路径

#### BDD-CE-C02-004 — 批量 `items[]` 元素未知属性被拒绝

**Given** 某 `items[i]` 含契约外属性（非 `itemId`/`variables`/`output`/`encryption`）  
**When** 调用批量生成 API  
**Then** HTTP **400**，`REQUEST_BODY_INVALID`，`UNKNOWN_FIELD` 指向该 item 路径

#### BDD-CE-C02-005 — `variables` 开放键不受 CE-C02 误伤

**Given** 模板变量 schema 允许（或既有规则接受）的变量键集合，请求 `variables` 仅含业务变量键、无顶层未知字段  
**When** 调用生成 API  
**Then** **不** 因 CE-C02 对 `variables` 内键报 `UNKNOWN_FIELD`  
**And** 变量合法性仍由既有模板契约/规则路径判定

#### BDD-CE-C02-006 — 管理端 DTO 严格性不变

**Given** 任一既有管理端写接口在本片前可接受的请求形态（相对其当前反序列化行为）  
**When** 本片仅启用 runtime 专用 fail-on-unknown  
**Then** 管理端行为不因本片变为「全局拒绝未知字段」  
**And** 验证方式：管理端 ObjectMapper / `@JsonIgnoreProperties` 策略与 runtime 隔离（测试或配置断言）

#### BDD-CE-C02-007 — 错误信封字段完整

**Given** 任一本片触发的未知字段失败  
**When** 读取错误响应  
**Then** body 为 `ErrorEnvelope`：含 `metadata`（至少既有 audit/trace 基线）与 `error`  
**And** `error` 含 `code`、`category`、`message`、`messageKey`、`retryable`  
**And** `fieldErrors` 非空，每条含 `field`、`reason`、`message`

### 契约对齐与 ADR 边界

#### BDD-CE-C01-C02-DOC-001 — OpenAPI / outline 与实现一致

**Given** 本片实现完成  
**When** 对照 `docs/api/openapi-v1.yaml` 的 `GenerateRequest` / `BatchGenerateRequest` / `Context` 与 runtime DTO  
**Then** OpenAPI 声明的可选 `context` 在实现中可绑定  
**And** `additionalProperties: false` 与运行时拒绝未知字段行为一致  
**And** Markdown 契约说明与错误码表不与实现冲突

#### BDD-CE-C01-C02-DOC-002 — D4：SYNC_DOWNLOAD_URL 仍 deferred / 不重签

**Given** 用户确认 D4（2026-07-14）  
**When** 本片交付  
**Then** **不**实现 `SYNC_DOWNLOAD_URL` 成功路径，**不**实现下载 URL 重签  
**And** 契约/文档（若本片触及）明确：该 mode 按 ADR-0038 deferred；调用方应使用 `SYNC_STREAM` 或 `ASYNC_TASK`  
**And** 若请求 `output.mode=SYNC_DOWNLOAD_URL`，仍按既有 ADR-0038 拒绝语义（非本片新增成功行为）

#### BDD-CE-C01-C02-DOC-003 — D2/D3 边界无回归扩大

**Given** D2 immediate-only、D3 无 webhook  
**When** 本片交付  
**Then** 不引入定时生效配置字段，不引入 webhook 回调字段或投递行为

#### BDD-CE-C02-008 — 批量 `originalBatchId` 可解析且不触发 CE-C05

**Given** 批量请求含可选 `originalBatchId`（符合 OpenAPI 模式的字符串）且其余字段合法  
**When** 调用批量生成 API  
**Then** **不**因「未知字段」拒绝该键（DTO 已声明）  
**And** **不**执行「原批次存在且同凭证」的 CE-C05 校验（可忽略血缘或仅原样保留至后续切片）  
**And** 文档/计划仍将完整血缘行为归 CE-C05

---

## 10. Boundary / exception 摘要

| 边界 | 行为 |
| --- | --- |
| `context` 省略 / null | 成功路径；无 contextSummary 伪造 |
| `context: {}` | 成功；视为无非空摘要键 |
| 白名单键为空字符串 | 实现可视为「未提供该键」（不写入 summary）或按普通字符串保留；**不得**当未知字段拒绝（见开放问题 Q1 偏好） |
| 未知字段 + 其它校验错误 | 反序列化/未知字段失败优先 fail-closed；不得先生成再报错 |
| 幂等重放 | 本片不改变幂等键语义；未知字段请求不得进入成功幂等记录 |
| 授权失败 | 仍走既有 401/403 等；本片不放宽 |
| `SYNC_DOWNLOAD_URL` | 仍 deferred（D4） |
| 管理端 | 严格性不变（UF-3） |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 后端单测 / MockMvc / WebTestClient | C01/C02 场景：接受 context；拒绝未知字段；错误信封断言 |
| `InvocationParameterSanitizer` 单测 | `contextSummary` 出现/缺省 |
| 契约测试 | OpenAPI 组件与 DTO 字段对齐（既有 `OpenApiContractTest` 或等价扩展） |
| `mvn -B -ntp -f backend/pom.xml verify` | 门禁全绿 |
| 文档 | 本文件；如需微调 `contract-outline` / ADR-0038 说明「不重签」的交叉链接（由后续 doc-keeper 或实现片同提交） |

本片为 **API 契约行为**，无管理 UI 用户可见变更 → **不要求** Playwright E2E/UIUX（除非实现意外触及前端契约页文案；文案-only 则按前端门禁最小集）。

---

## 12. Traceability

| 工件 | 路径 / ID |
| --- | --- |
| 计划 | `docs/plan/core-excellence-program-2026-07.md` §5 CE-C01 / CE-C02；§9 同链；§10 D2/D3/D4 |
| 行为规格 | `docs/behavior/ce-c01-c02-contract-strictness.md`（本文件） |
| OpenAPI | `docs/api/openapi-v1.yaml` — `Context`、`GenerateRequest`、`BatchGenerateRequest`、`ErrorEnvelope`、`FieldError` |
| 契约说明 | `docs/api/contract-outline.md` — context 白名单；严格字段校验；错误模型 |
| 需求 / PRD / 域 / 权限 | `requirements-plan.md`、`PRD.md`、`domain-model.md`、`permission-matrix.md` 中 context / REQUEST_BODY_INVALID 条款 |
| ADR | ADR-0007 (D2)、ADR-0008 (D3)、ADR-0038 (D4)、ADR-0013 / ADR-0020（摘要与敏感数据） |
| Slice / 分支 | `ce-c01-c02-contract-strictness` · `feat/ce-c01-c02-contract-strictness` |
| 后续 | CE-C03 fidelityWarnings；CE-C05 `originalBatchId` 血缘；CE-K03 消费 `context.locale` |

---

## 13. 开放问题（不阻塞 `ready`；实现默认如下）

| ID | 问题 | 默认（可被用户推翻） | 阻塞？ |
| --- | --- | --- | --- |
| **Q1** | 白名单键值为 `""` 时：拒绝、还是视为未提供？ | **视为未提供**：不写入 `contextSummary` 该键；不 400 | 否 |
| **Q2** | context 字符串是否加平台 `maxLength`？ | OpenAPI 未声明则本片 **不加** 新上限；过长仅受容器/全局限制 | 否 |
| **Q3** | `originalBatchId` 在 CE-C05 前是否持久化到调用记录？ | **允许原样保留在请求绑定中**；不校验、不扩展血缘审计字段 | 否 |
| **Q4** | 未知字段时 `fieldErrors[].message` 英文固定句式？ | 使用既有/统一英文短句（含字段名），不泄露其它请求内容 | 否 |

若用户明确推翻 Q1–Q3 默认，再修订本规格后进入实现。

---

## 14. BDD readiness

```
bdd_readiness: ready
owning_doc: docs/behavior/ce-c01-c02-contract-strictness.md
task_ids: [CE-C01, CE-C02, slice:ce-c01-c02-contract-strictness]
acceptance_scenario_count: 17
open_questions: [Q1 empty-string context keys, Q2 maxLength, Q3 originalBatchId persistence depth, Q4 field error message wording]
next: plan-orchestrator → backend-engineer (TDD) in worktree
```

**Acceptance scenario count: 17**

| 分组 | 场景 ID | 计数 |
| --- | --- | --- |
| CE-C01 | BDD-CE-C01-001 … 006 | 6 |
| CE-C02 | BDD-CE-C02-001 … 008 | 8 |
| Doc / ADR | BDD-CE-C01-C02-DOC-001 … 003 | 3 |
| **合计** | | **17** |

**非阻塞开放问题: 4**（Q1–Q4，均有实现默认）

---

## 15. 明确禁止（实现片）

- 实现代码前不得跳过本规格中的失败场景测试（TDD Red → Green）。  
- 不得用全局 Jackson `FAIL_ON_UNKNOWN_PROPERTIES` 误伤管理端。  
- 不得在本片实现 webhook、定时发布、`SYNC_DOWNLOAD_URL` 重签、CE-C03/C04/C05/C06。  
- 不得将 `context` 用于覆盖路径上的 template/version 或输出策略。  
- 不得宣称 production go-live。
