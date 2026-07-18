# BDD 行为规格：IBL-A1 — Runtime fail-closed variable validation

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-A1` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | IBL Wave A · **IBL-A1** / F1（`ibl-a1-variable-validation`） |
| **Slice** | `ibl-a1-variable-validation` |
| **Branch** | `feat/ibl-a1-variable-validation` |
| **Worktree** | `D:/working/DGE-ibl-a1-variable-validation` |
| **Placement** | ISOLATED |
| **Task Master** | **#107** IBL-A1 — Batch Recommendation **solo**；`member_task_ids: ["107"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-a1-variable-validation`；vetoes: unrelated-acceptance-surface-vs-A2-A3, umbrella-106-registry-only） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F1 / IBL-A1；校验语义复用 [ce-u03-testdata-schema-form.md](./ce-u03-testdata-schema-form.md) U03-C8/C10；契约 [contract-outline.md](../api/contract-outline.md) / OpenAPI |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（本叶为 runtime/BE 契约；E2E/UIUX **N/A**） |

**完成声明约束：** 本叶关闭 runtime generate（及对齐的 preview 装配）对 `VariableSchemaEntity` required/type/enum 的静默空填。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b**（保持 **CONDITIONAL**）；**禁止**将 **#5a** 标为 **GO**；**禁止**宣称 IBL Wave A / 程序 Done；**禁止**把 A2/A3/A4/A5/A6 并入本叶。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["107"]
  proposed_slice_id: ibl-a1-variable-validation
  shared_acceptance_surface: >
    Runtime generate variable validation fail-closed VARIABLE_VALIDATION_FAILED
  vetoes_applied:
    - unrelated-acceptance-surface-vs-A2-A3
    - umbrella-106-registry-only
  evidence_amortization: mvn verify + queued docker deploy
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| Runtime sync generate：`POST …/default/generate`、`POST …/versions/{releaseVersion}/generate` | IBL-A2 `FORMAT_AMOUNT` ISO 币种 |
| Runtime batch generate：每 item 变量在装配前同样 fail-closed | IBL-A3 amount-in-words / `en` |
| 管理面 **preview 装配**与 runtime **对齐**同一 schema 规则（见 A1-C3） | IBL-A4 `/contract` per-field + break gate |
| 稳定 `error.code=VARIABLE_VALIDATION_FAILED` + `fieldErrors[]` | IBL-A5 PII retention redaction |
| 无 DOCX/PDF 静默 blank（校验失败则不产出成功制品） | IBL-A6 regenerate locale / SPECIMEN 政策 |
| 复用 CE-U03 字段 reason 语义（REQUIRED / INVALID_TYPE / …） | 改变 publish gate checklist 项语义为「校验请求体」 |
| OpenAPI / `ApiErrorCodes` / contract-outline 登记新码 | 作者态 `TemplateVariableValidation*`（ROLE_DENIED 等）改版 |
| Gates：`mvn -B -ntp -f backend/pom.xml verify`；queued deploy 证据 | FE E2E/UIUX；翻转 #3b/#5a；go-live |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| Runtime DOCX/PDF 装配前**仅**跑 compute，**不**校验 `required_flag` / type / enum | `DocumentGenerationAssemblySupport.generate` → `variableComputeService.applyCompute` only |
| `VariableSchemaEntity` 已具备 `required` / `variableType` / `enumValues` / `computeExpression` / `piiCategory` | `VariableSchemaEntity`；F1 in IBL plan |
| 管理测试集保存路径**已有** fail-closed schema 校验（CE-U03） | `TestDataSetVariablesSchemaValidator` + `TestDataSetSchemaValidationException` → 422 + `fieldErrors`（今日顶层码仍为 `REQUEST_BODY_INVALID`） |
| 管理 preview 装配同样只 `applyCompute`，ad-hoc / 未校验变量可静默空填 | `PreviewGenerationAssemblySupport.assembleAndStore` |
| Publish gate 只检查「变量 schema 是否存在」+「是否有成功 preview」，**不**校验 runtime 请求体 | `PublishGateCheckItemSupport.variableSchemaItem` / `previewPresentItem` |
| 契约表已列 `VARIABLE_REQUIRED` / `VARIABLE_TYPE_INVALID` 等单码，但 runtime **未实现**；本叶引入**聚合码** `VARIABLE_VALIDATION_FAILED` + 字段列表（多字段一次返回） | [contract-outline.md](../api/contract-outline.md) VALIDATION 表；OpenAPI `ErrorDetail.code` enum；IBL-A1 验收文案 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **A1-S1 Runtime generate** | 同步 generate 在装配前按版本 `VariableSchema` 校验；失败 → **422** `VARIABLE_VALIDATION_FAILED` + `fieldErrors`；**不**写成功制品、**不**静默 blank |
| **A1-S2 Batch generate** | 每 batch item 同样校验；失败 item **不**产出该 item 的成功 DOCX/PDF（映射为 item FAILED + 同码/字段列表语义） |
| **A1-S3 Preview 对齐** | 管理 preview 在装配前应用**同一套** required/type/enum 规则，避免「成功 preview + 静默空填」喂给 publish gate |
| **A1-S4 Publish 作用域** | Publish **不**新增「对 generate 请求体校验」；继续依赖 schema 存在 + 成功 preview 证据。因 A1-S3，错误变量无法再以静默 blank 的「成功 preview」过闸 |
| **A1-S5 契约同步** | OpenAPI / `ApiErrorCodes` / contract-outline 增加并文档化 `VARIABLE_VALIDATION_FAILED` |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **Runtime API 调用方** | 有效 API credential + AD group 授权；调用 sync/batch generate | 缺必填 / 错类型 / 错枚举时立即收到稳定 4xx，而非空白函件 |
| **模板作者 / 测试人员** | 管理会话；创建 preview（含 ad-hoc variables 或测试集解析变量） | Preview 与 runtime 同规则，避免误以为「预览成功 = 生产可填」 |
| **发布负责人** | Publish gate | 不直接消费本叶新校验码；间接受益于 preview 不再静默空填 |
| **系统（runtime）** | `RuntimeGenerationService` → `DocumentGenerationEngine` → `DocumentGenerationAssemblySupport` | 校验 → compute → assemble；校验失败 fail-closed |
| **系统（preview）** | `PreviewGenerationService` → `PreviewGenerationAssemblySupport` | 与 runtime 共享校验语义（实现可抽共享 validator） |

---

## 3. Goal

1. 已发布（及可调用）版本上的 generate，对 schema 声明的可录入变量执行 **required / type / enum** fail-closed 校验。  
2. 校验失败返回统一 envelope：`error.code=VARIABLE_VALIDATION_FAILED`、`category=VALIDATION`、`retryable=false`、非空 `fieldErrors[]`；**HTTP 422**。  
3. 失败路径**不得**产生成功 DOCX/PDF（含 sync stream 与 batch item 成功制品）；不得用空字符串静默替换必填缺失。  
4. 管理 preview 装配路径**对齐**同一规则（A1-C3）。  
5. Publish 路径**明确 scoped**：不校验 generate body；靠对齐后的 preview + 既有 checklist。  
6. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **A1-C1** | **校验时机：** 在 `applyCompute` / DOCX assemble **之前**，对目标 `template_version` 的 `VariableSchema` + 请求 `variables` 执行 fail-closed 校验。推荐挂点：runtime 与 preview 装配入口共享校验（可从 `TestDataSetVariablesSchemaValidator` 抽取共享组件）。 | F1；`DocumentGenerationAssemblySupport` / `PreviewGenerationAssemblySupport` 今日只 compute |
| **A1-C2** | **Runtime 验收面（强制）：** `POST /api/{env}/v1/templates/{externalId}/default/generate` 与 `…/versions/{releaseVersion}/generate`；以及 `…/default/batch-generate` / `…/versions/{releaseVersion}/batch-generate` 的**每个 item**。`variables == null` 仍按既有 `VARIABLES_REQUIRED` / request 结构校验（本叶不替换）。 | `RuntimeTemplateController`；IBL-A1 验收 |
| **A1-C3** | **Preview：对齐（aligned），非「仅 runtime」。** 管理 `POST …/previews` 装配路径在 assemble 前应用与 A1-C5 **相同** 的 required/type/enum（及 UNKNOWN_FIELD）规则。错误响应：管理面亦使用 `VARIABLE_VALIDATION_FAILED` + `fieldErrors`（422 / VALIDATION），以便作者与消费者看到一致语义。**不**要求本叶改 FE 文案或 E2E。 | Preview 与 runtime 共享静默 blank 风险；publish 依赖成功 preview |
| **A1-C4** | **Publish：显式 scoped-out of request-body validation。** Publish gate **不**新增对 runtime generate payload 的校验；保持 `variableSchema` 存在性 + `previewPresent`（及既有项）。本叶通过 A1-C3 防止「静默空填成功 preview」污染闸门。 | `PublishGateCheckItemSupport`；验收「aligned **or** scoped」 |
| **A1-C5** | **字段规则（与 CE-U03 对齐）：** 跳过 compute 字段（`COMPUTED` 或非空 `computeExpression`）。可录入字段：`required` 且缺失/null/空白串 → `REQUIRED`；类型不匹配 → `INVALID_TYPE`（DATE 非法 → `INVALID_FORMAT`）；ENUM 不在允许集 → `ENUM_NOT_ALLOWED`；payload 中未知 key → `UNKNOWN_FIELD`。可选字段缺失 → 通过。显式携带 compute key：剥离后校验（与 U03 strip 一致），不因 compute key 单独阻断。 | `TestDataSetVariablesSchemaValidator`；[ce-u03](./ce-u03-testdata-schema-form.md) U03-C8/C10 |
| **A1-C6** | **顶层错误码：** 一次请求可有多字段失败 → 单一稳定码 **`VARIABLE_VALIDATION_FAILED`**（非逐字段换顶层码）。`category=VALIDATION`；`retryable=false`；`messageKey=api.error.validation.variableValidationFailed`；`error.message` 英文稳定通用文案；细节在 `fieldErrors[].field|reason|message`。HTTP **422**。 | IBL-A1 / TM #107「VARIABLE_VALIDATION_FAILED + field list」；合同 422 变量校验档 |
| **A1-C7** | **与契约表既有 `VARIABLE_REQUIRED` / `VARIABLE_TYPE_INVALID` / `VARIABLE_FORMAT_INVALID` / `VARIABLE_RULE_FAILED` 的关系：** 本叶**采用聚合码** `VARIABLE_VALIDATION_FAILED` 作为 runtime/preview schema 校验的权威顶层码；字段级 `reason` 表达细分。实现期同步 OpenAPI enum + contract-outline：新增 `VARIABLE_VALIDATION_FAILED`；既有单字段码保留在枚举中（兼容文档），**本叶 runtime 路径不改用它们作为多字段聚合响应的顶层码**。 | contract-outline 已列单码但未落地；验收强制聚合码 |
| **A1-C8** | **`default_value`：本叶不自动套用。** Schema 列 `default_value` 今日未参与 runtime 装配；缺失 required **仍失败**，即使列上有 default。默认值填充若需要，另开任务/ADR。 | `VariableSchemaEntity.defaultValue` 无 runtime 消费者 |
| **A1-C9** | **成功路径：** 校验通过后行为与今日一致（compute → assemble → DOCX/PDF）；必填均提供且类型/枚举合法时，占位符被真实值替换（回归：产物中不得以空串冒充该必填字段的业务值——以装配输入/测试断言可观测）。 | 无 silent blank |
| **A1-C10** | **Batch：** 校验失败的 item **不得** `SUCCEEDED` 且不得写入该 item 成功制品；错误进入 item 级失败投影（既有 batch 失败模型），错误码/字段列表语义与 sync 一致。`continueOnItemFailure` 时其它合法 item 仍可成功。 | `BatchExecutionService` |
| **A1-C11** | **与 `VARIABLE_COMPUTE_FAILED` 顺序：** schema 校验先于 compute；compute 失败仍映射既有 `VARIABLE_COMPUTE_FAILED`，不伪装为本叶码。 | CE-K03 |
| **A1-C12** | **授权 fail-closed 不回归：** 无凭证/无组授权仍走既有 401/403；本叶不放宽访问。 | runtime authz |
| **A1-C13** | **测试数据保存路径：** CE-U03 保持；允许共享 validator 实现，但**不要求**本叶把测试集顶层码从今日 `REQUEST_BODY_INVALID` 改成 `VARIABLE_VALIDATION_FAILED`（可留后续一致性清理）。 | 缩小范围 |
| **A1-C14** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；行为变更验收面 → Stage 5/10 queued Docker deploy 证据；`frontend_ui_in_scope=false` → E2E/UIUX N/A；architecture review。 | delivery constitution |
| **A1-C15** | **完成边界：** Done ≠ IBL Wave A 完备；≠ go-live；#3b/#5a 保持 CONDITIONAL。 | 队列政策 |

### 4.2 已确认（上游交付，本叶只消费）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **A1-U1** | Variable schema 持久化与类型枚举已存在 | `VariableSchemaEntity` / `VariableType` |
| **A1-U2** | CE-U03 字段 reason 与校验算法已落地于测试集 | `TestDataSetVariablesSchemaValidator` |
| **A1-U3** | Compute 引擎在装配前 fail-closed | CE-K03 / `VariableComputeService` |
| **A1-U4** | Formal phase None；非 go-live | IBL / PRR 队列政策 |

### 4.3 非确认假设（不得升格为需求）

| ID | 陈述 | 状态 |
| --- | --- | --- |
| **A1-N1** | 运行时自动应用 `default_value` | **非确认** — 本叶明确不做（A1-C8） |
| **A1-N2** | 改变 CE-U03 测试集 API 的顶层 `error.code` | **非确认** — 可选后续 |
| **A1-N3** | Publish checklist 新增「sample payload schema valid」项 | **非确认** — 本叶 scoped-out（A1-C4） |
| **A1-N4** | FE 错误面板 / Playwright 旅程 | **非确认** — `frontend_ui_in_scope=false` |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | Runtime sync generate（default 或显式版本） | 主验收面 |
| T2 | Runtime batch generate（任一 item） | 每 item 校验 |
| T3 | 管理 preview 创建/装配（含 inline variables） | 对齐面 |
| T4 | （非触发）Publish transition | 无新 body 校验；间接依赖 T3 |

---

## 6. Preconditions

| # | 前置条件 |
| --- | --- |
| PC1 | 模板版本存在且对调用方 callable（sync/batch）；preview 则会话可预览该模板 |
| PC2 | 该版本已配置 `VariableSchema`（可含 required / ENUM / 类型字段）；允许零 schema（则无可录入 required 失败——仅 UNKNOWN_FIELD 若乱传 key） |
| PC3 | API policy / 输出格式等既有请求结构校验已通过（或将先失败于本叶之前） |
| PC4 | 调用方已通过认证与模板 AD 组授权 |

---

## 7. Primary journey（成功）

1. 调用方提交 generate（或 preview），`variables` 含全部 required 且类型/枚举合法。  
2. 系统加载版本 `VariableSchema`，校验通过。  
3. 系统 `applyCompute`（若有）。  
4. 系统装配 DOCX（及请求的 PDF），返回成功制品 / preview 记录。  
5. 观测：必填业务值出现在产物中（非空串冒充）。

---

## 8. System responses

### 8.1 Success

| 表面 | 响应 |
| --- | --- |
| Sync generate | 既有成功流（DOCX/PDF bytes 或既定 sync 写响应）；无 `error` |
| Batch item | `SUCCEEDED` + documentId（当该 item 合法） |
| Preview | 既有成功 preview 记录 + 制品；无 `VARIABLE_VALIDATION_FAILED` |

### 8.2 Fail-closed（本叶）

| 条件 | HTTP | code | category | fieldErrors | 制品 |
| --- | --- | --- | --- | --- | --- |
| 缺 required / 错类型 / 错枚举 / 未知字段（一条或多条） | **422** | **`VARIABLE_VALIDATION_FAILED`** | `VALIDATION` | 非空列表；`reason` ∈ `REQUIRED` \| `INVALID_TYPE` \| `INVALID_FORMAT` \| `ENUM_NOT_ALLOWED` \| `UNKNOWN_FIELD` | **无**成功 DOCX/PDF（sync）；batch 该 item 非 SUCCEEDED |

`messageKey`: `api.error.validation.variableValidationFailed`  
`retryable`: `false`

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-A1-001 — Sync generate 缺必填 → VARIABLE_VALIDATION_FAILED

**Given** 可调用模板版本的 VariableSchema 含 required 字段 `customerName`（TEXT）  
**And** 调用方凭证与组授权有效  
**When** `POST …/default/generate`（或显式版本）body `variables` **省略** `customerName`（其余合法）  
**Then** 响应 **422**  
**And** `error.code=VARIABLE_VALIDATION_FAILED`  
**And** `error.category=VALIDATION`  
**And** `error.retryable=false`  
**And** `error.fieldErrors` 含 `field=customerName` 且 `reason=REQUIRED`  
**And** 无成功文档制品写入（无成功 documentId / 无成功 storage 制品供下载）

### BDD-IBL-A1-002 — Sync generate 错误类型 → fieldErrors INVALID_TYPE

**Given** schema 字段 `principalAmount` 类型为 `AMOUNT`（或 `NUMBER`）且 required  
**When** sync generate 提交 `"principalAmount": "not-a-number"`（或非数字 JSON 类型）  
**Then** **422** `VARIABLE_VALIDATION_FAILED`  
**And** `fieldErrors` 含该 field 且 `reason=INVALID_TYPE`（DATE 场景则 `INVALID_FORMAT`）  
**And** 不产出成功 DOCX/PDF

### BDD-IBL-A1-003 — Sync generate 错误枚举 → ENUM_NOT_ALLOWED

**Given** schema 字段 `letterType` 为 `ENUM`，允许集含 `OFFER`（及其它），且 required  
**When** sync generate 提交 `"letterType": "NOT_IN_ENUM"`  
**Then** **422** `VARIABLE_VALIDATION_FAILED`  
**And** `fieldErrors` 含 `field=letterType` 且 `reason=ENUM_NOT_ALLOWED`  
**And** 不产出成功 DOCX/PDF

### BDD-IBL-A1-004 — Sync generate 成功路径（无静默 blank）

**Given** 同上版本；全部 required 提供且类型/枚举合法  
**When** sync generate `output.format=DOCX`（及回归用例 `PDF` 至少一则）  
**Then** 请求成功（非 4xx validation）  
**And** 产物中对应必填占位被提供值填充（测试可断言装配输入或产物文本/XML 含该值）  
**And** **不**因缺失必填而生成「成功但空白」函件

### BDD-IBL-A1-005 — Preview 对齐：缺必填不得成功 preview

**Given** 同版本 schema 含 required `customerName`  
**When** 管理面创建 preview，resolved/inline `variables` 省略 `customerName`  
**Then** preview 装配 **失败** 并返回 **422** `VARIABLE_VALIDATION_FAILED` + `fieldErrors`（含 `REQUIRED`）  
**And** **不**创建「成功」preview 证据记录用于 publish gate 计数  
**And** （对照）合法 variables 的 preview 仍可成功（不回归既有成功路径）

### BDD-IBL-A1-006 — Publish scope：不新增 generate-body 校验项

**Given** 模板满足既有 publish checklist（含 variableSchema 存在、至少一次**合法变量**成功 preview 等）  
**When** 授权用户执行 publish  
**Then** 行为仍按既有 PublishGate 规则（本叶**不**要求在 publish API 上校验一笔 runtime `variables` body）  
**And** 文档/实现注释或测试锁定：错误变量无法再通过「静默空填成功 preview」满足 `previewPresent`（由 BDD-IBL-A1-005 保证）

### BDD-IBL-A1-007 — Batch item 错变量 fail-closed

**Given** batch 含两个 item：item A 变量合法；item B 缺 required  
**When** `batch-generate`（`continueOnItemFailure=true` 或等价允许部分成功的模式）  
**Then** item A 可 `SUCCEEDED`  
**And** item B 为 `FAILED`，错误语义含 `VARIABLE_VALIDATION_FAILED` 与 field list（投影字段按既有 batch item error 模型）  
**And** item B **无**成功制品

### BDD-IBL-A1-008 — Compute 字段不要求调用方提供

**Given** schema 含 compute 字段（`COMPUTED` 或非空 `computeExpression`）与另一 required 录入字段  
**When** sync generate 仅提供录入 required，省略 compute key  
**Then** **不**因 compute 缺失而 `REQUIRED`  
**And** 在其它字段合法时请求可成功（compute 由引擎计算；compute 失败仍走 `VARIABLE_COMPUTE_FAILED`）

---

## 10. Boundary / exception

| 场景 | 行为 |
| --- | --- |
| 多字段同时非法 | 单次响应聚合全部 `fieldErrors`；仍一个顶层 `VARIABLE_VALIDATION_FAILED` |
| 未知字段 | `UNKNOWN_FIELD`；计入失败 |
| 可选字段省略 | 允许 |
| 空 schema | 无 required 可失败；未知 key 仍 `UNKNOWN_FIELD` |
| 认证/授权失败 | 既有 401/403；不泄露 schema |
| Idempotent replay of prior **success** | 既有幂等；本叶不改变成功 replay |
| 校验失败是否写 failed invocation | 保持既有 `recordFailedSingleInvocation` 行为；不得写成功制品 |
| LIST/OBJECT 类型 | 与 CE-U03 一致：`LIST` 需为 JSON array，`OBJECT` 需为 JSON object；否则 `INVALID_TYPE` |
| `default_value` 有值但请求省略 required | **仍** `REQUIRED`（A1-C8） |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | 422 envelope：`code` / `category` / `messageKey` / `fieldErrors` |
| 制品否定 | 失败用例无成功 document storage / preview SUCCESS 证据 |
| 单测 | JUnit：required / type / enum / success / compute skip / preview 对齐 / batch item（按实现挂点） |
| 契约 | OpenAPI `ErrorDetail.code` 含 `VARIABLE_VALIDATION_FAILED`；contract-outline 行更新 |
| 门禁 | `mvn verify` GREEN；queued Docker deploy 证据（验收面） |
| Trace | 既有 `metadata.traceId` / `auditId` 保留 |

---

## 12. Traceability

| 项 | 引用 |
| --- | --- |
| Plan | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) · F1 · **IBL-A1** |
| Task Master | **#107** |
| Related BDD | [ce-u03-testdata-schema-form.md](./ce-u03-testdata-schema-form.md)；[ce-k03-variable-compute-engine.md](./ce-k03-variable-compute-engine.md) |
| Code anchors | `DocumentGenerationAssemblySupport`；`PreviewGenerationAssemblySupport`；`TestDataSetVariablesSchemaValidator`；`VariableSchemaEntity`；`RuntimeTemplateController`；`ApiErrorCodes` |
| API | [openapi-v1.yaml](../api/openapi-v1.yaml)；[contract-outline.md](../api/contract-outline.md) |
| Permissions | 无新 capability bit；沿用 runtime credential + AD group / management preview 既有授权 |

---

## 13. Out of scope（本叶）

- IBL-A2 / A3 / A4 / A5 / A6  
- 自动 `default_value` 填充  
- 改变 publish checklist 结构或 #3b/#5a  
- FE i18n 面板 / Playwright  
- 作者态 template variable-validate 管理 API（`VARIABLE_VALIDATION_ROLE_DENIED` 等）  
- 将 CE-U03 测试集顶层码强制迁移为 `VARIABLE_VALIDATION_FAILED`

---

## 14. Ready-for-implementation handoff

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-a1-variable-validation.md
task_ids: ["107"]
plan_id: IBL-A1
frontend_ui_in_scope: false
acceptance_scenario_ids:
  - BDD-IBL-A1-001
  - BDD-IBL-A1-002
  - BDD-IBL-A1-003
  - BDD-IBL-A1-004
  - BDD-IBL-A1-005
  - BDD-IBL-A1-006
  - BDD-IBL-A1-007
  - BDD-IBL-A1-008
next_stage: plan-orchestrator (stage 2)
publish_preview_scope_decision: >
  Preview ALIGNED (same required/type/enum fail-closed + VARIABLE_VALIDATION_FAILED);
  Publish SCOPED OUT of generate-body validation (checklist unchanged; protected via aligned preview).
```

**TDD Red 优先场景：** BDD-IBL-A1-001（缺必填）、002（错类型）、003（错枚举）、004（成功无 blank）；随后 005（preview）、007（batch）。
