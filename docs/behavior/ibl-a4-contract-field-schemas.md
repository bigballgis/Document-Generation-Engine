# BDD 行为规格：IBL-A4 — `/contract` per-field variable schemas + breaking-change gate

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-A4` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | IBL Wave A · **IBL-A4** / F4（`ibl-a4-contract-field-schemas`） |
| **Slice** | `ibl-a4-contract-field-schemas` |
| **Branch** | `feat/ibl-a4-contract-field-schemas` |
| **Worktree** | `D:/working/DGE-ibl-a4-contract-field-schemas` |
| **Base** | `2f7a1ba7`（handoff） |
| **Placement** | ISOLATED |
| **Task Master** | **#110** IBL-A4 — Batch Recommendation **solo**；`member_task_ids: ["110"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-a4-contract-field-schemas`；vetoes: `different-acceptance-vs-A5-A6`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F4 / IBL-A4；契约 [openapi-v1.yaml](../api/openapi-v1.yaml) / [contract-outline.md](../api/contract-outline.md)；变量实体 `VariableSchemaEntity`；装配 `ContractAssemblyService` |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（本叶为 runtime/management `/contract` 契约形状 + 消费者兼容闸门；E2E/UIUX **N/A**。允许 OpenAPI 生成类型 / `CallerContract` TS 随契约同步，非 OA 旅程） |

**完成声明约束：** 本叶关闭 F4——`/contract` 对调用方诚实暴露**逐字段**变量 Schema，并以**消费者契约测试**在 breaking rename/shape 变更时失败；OpenAPI 同步。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave A / 程序 Done；**禁止**把 A5（PII retention）/ A6（regenerate locale）并入本叶。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["110"]
  proposed_slice_id: ibl-a4-contract-field-schemas
  shared_acceptance_surface: >
    /contract per-field variable schemas + breaking-change gate
  vetoes_applied:
    - different-acceptance-vs-A5-A6
  evidence_amortization: mvn verify + docker
  on_red_split_hint: N/A
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| Runtime `GET /api/{env}/v1/templates/{templateId}/contract` 返回 **per-field** 变量 Schema | IBL-A5 留存参数按 `VariablePiiCategory` 脱敏 |
| Management `GET /api/management/v1/templates/{id}/api/contract` **同装配**含 per-field | IBL-A6 regenerate locale 重放 |
| 字段至少含 name/type/required；enum / pii **as applicable** | 改变 generate 校验语义（已由 IBL-A1 覆盖） |
| 信封级 `schemas: string[]`（GenerateRequest 等）**保留**作 OpenAPI 类型名索引 | 把 breaking gate 做成 publish 硬阻断 API（本叶为 **consumer/CI 测试闸**） |
| Consumer contract tests：breaking rename / remove / type / required↑ / enum 收缩 → **FAIL** | FE Playwright / OA 视觉旅程 |
| 非破坏性 additive（新增 optional 字段、enum 增值、description-only）→ **PASS** | 翻转 #3b/#5a；go-live；Wave A Done |
| OpenAPI `ContractResponse` / `CallableVersion` + contract-outline 同步 | 发明正式 P-phase |
| Gates：`mvn -B -ntp -f backend/pom.xml verify`；queued deploy 证据 | |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| `/contract` 的 `result.schemas` **硬编码**信封类型名，不读 `VariableSchemaEntity` | `ContractAssemblyService.assemble` → `List.of("GenerateRequest", "BatchGenerateRequest", "OutputOptions", "EncryptionOptions")` |
| `CallableVersionView` 仅 `releaseVersion` / URL / deprecated 元数据，**无** variables | `CallableVersionView` |
| OpenAPI `ContractResponse.result.schemas` 为 `string[]` | [openapi-v1.yaml](../api/openapi-v1.yaml) `ContractResponse` |
| 发布版本已锁定逐字段 Schema（key/type/required/enum/pii/compute） | `VariableSchemaEntity`；contract-outline「发布版本锁定的模板变量 Schema」 |
| 无消费者契约兼容测试；占位符/字段 rename 不会在 CI 失败 | 仓库无 consumer contract / breaking-change gate 针对 `/contract` variables |
| 管理面与 runtime 共用装配，但都缺字段级契约 | `TemplatePlatformSliceTest` 仅断言 paths/policy/versions，不断言 variables |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **A4-S1 Contract per-field** | `/contract`（runtime + management）对每个可调用发布版本暴露 `variables[]` 逐字段 Schema |
| **A4-S2 Envelope schemas 兼容** | 顶层 `schemas: string[]` 信封类型名索引保留（非破坏） |
| **A4-S3 Breaking gate** | 消费者契约测试对 breaking rename/shape 失败；additive 非破坏通过 |
| **A4-S4 OpenAPI sync** | OpenAPI + contract-outline 描述新字段与兼容规则 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **Runtime API 调用方** | 有效 API credential + AD group 授权；`GET …/contract` | 发现该模板各可调用版本需提交的 variables 字段形状 |
| **模板编排 / API 管理员** | 管理会话；`GET …/api/contract?environment=` | 同一字段级契约（管理面可含既有 policy 明细；variables **非**敏感原值） |
| **平台工程 / CI** | 运行 `mvn verify` 内消费者契约测试 | rename/破坏性 schema 变更时闸门变红 |
| **系统** | `ContractAssemblyService` (+ ViewSupport) 装配 `ContractResultView` | 从版本 `VariableSchema` 投影字段视图；不嵌入完整 OpenAPI YAML |

---

## 3. Goal

1. 授权调用方通过 `/contract` 获得**真实**、**按发布版本锁定**的逐字段变量 Schema（至少 `variableKey` / `variableType` / `required`；`enumValues` / `piiCategory` 在适用时出现）。  
2. 信封级 `schemas` 字符串列表继续存在，不因本叶删除而破坏既有消费者只读该列表的用法。  
3. 消费者契约测试在 **breaking** 占位符/字段变更时失败；**non-breaking additive** 变更通过。  
4. OpenAPI 与 contract-outline 与实现同步。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **A4-C1** | **验收面（强制）：** Runtime `GET /api/{environment}/v1/templates/{templateId}/contract`；Management `GET /api/management/v1/templates/{templateId}/api/contract`（既有 query `environment`）。两者共用 `ContractAssemblyService` 装配，均含 per-field variables。 | F4；既有双路径测试 |
| **A4-C2** | **挂载点：** 在每个 `callableVersions[]` 元素上增加 `variables`（数组）。Schema 来自该 `releaseVersion` 对应 `template_version` 的 `VariableSchemaEntity` 列表。调用方以 `defaultRoute.currentTargetReleaseVersion` 匹配同名 `callableVersions[].releaseVersion` 取得 default 路径字段契约。 | 契约「按发布版本锁定」；`CallableVersionView` 今日无字段 |
| **A4-C3** | **信封 `schemas: string[]` 保留：** 继续返回至少 `GenerateRequest` / `BatchGenerateRequest` / `OutputOptions` / `EncryptionOptions`（允许实现扩展索引，但不得清空）。本叶**不**把信封类型名伪装为字段 Schema。 | 兼容既有 `CallerContract.schemas: string[]` |
| **A4-C4** | **字段投影形状（调用方可见友好对象）：** 每项至少：`variableKey`（string）、`variableType`（`TemplateVariableType` / `VariableType` 枚举）、`required`（boolean）。**As applicable：** `ENUM` → 非空 `enumValues`（稳定序列：优先 JSON **string 数组**；若实现沿用实体序列化字符串，OpenAPI 必须写清且测试钉死）；`piiCategory` **始终输出**（含 `NONE`），以便消费者知晓分类而不必猜。可选：`description`（可 null/省略）。 | IBL-A4 验收文案；CE-G03 pii；OpenAPI 对齐 |
| **A4-C5** | **Compute 字段：** `variableType=COMPUTED` **或** 非空 `computeExpression` 的字段**仍列入** `variables[]`，并带 `computed=true`（boolean，必填于投影）。调用方不应提交这些 key（与 IBL-A1 校验跳过一致）；契约诚实暴露其存在。非 compute → `computed=false`。 | IBL-A1 A1-C5；避免「幽灵字段」 |
| **A4-C6** | **禁止出现在 `/contract` variables 投影中：** 内部 UUID `id`；`defaultValue` 明文；`computeExpression` 原文（仅用 `computed` 标志）。避免把作者默认值/表达式当调用方契约泄漏。 | 安全摘要惯例；验收未要求 default |
| **A4-C7** | **排序：** `variables[]` 按 `variableKey` **字典序升序**（稳定，便于 diff / 快照）。 | 消费者契约可测 |
| **A4-C8** | **空 / 未发布：** 模板非 `PUBLISHED` 或无可调用发布版本 → `callableVersions=[]`（与今日一致），无 variables 可挂。已发布但该版本 0 条 schema → `variables=[]`（合法空数组）。 | `buildCallableVersions` 今日过滤 |
| **A4-C9** | **Audience：** Runtime 继续剥离 management-only policy/defaultRoute 明细（既有行为不回归）。`variables[]` **对 runtime 与 admin 均可见**（字段名/类型非凭证、非变量原值）。 | `ContractViewAudience`；现有 slice 测试 |
| **A4-C10** | **授权 fail-closed 不回归：** 无凭证/无组授权 → 既有 401/403；不得因本叶放宽访问或在 403 响应体泄漏他模板 variables。 | runtime authz |
| **A4-C11** | **Breaking-change gate = 消费者契约测试（CI / `mvn verify`），非 publish API 新硬阻断。** 提供可复用的兼容性分类器（测试或小型纯函数）+ 至少一则固定基线（fixture / golden）与突变用例。Publish gate **不**因本叶新增「禁止 rename」检查项。 | 验收「consumer contract tests fail」；对齐 A1 publish scoped 风格 |
| **A4-C12** | **Breaking（必须 FAIL）：** (a) 删除已有 `variableKey`；(b) rename（旧 key 消失且出现新 key — 按 key 集合删除检测即可，不强制启发式「同义 rename」）；(c) `variableType` 变更；(d) `required`: `false` → `true`；(e) `ENUM` 允许集**收缩**（移除既有枚举值）；(f) `computed`: `false` → `true`（调用方原可填变为不可填）。 | F4「breaking placeholder/schema renames」 |
| **A4-C13** | **Non-breaking（必须 PASS）：** (a) 新增 `required=false` 且 `computed=false` 的字段；(b) `ENUM` **仅增值**；(c) 仅 `description` / 文案变化；(d) `required`: `true` → `false`；(e) `piiCategory` 变化（本叶 gate **不**因 pii 标签变更失败 — 留存脱敏属 A5；契约仍**输出** change 后的值）。 | handoff「non-breaking additive if applicable」 |
| **A4-C14** | **基线范围：** 测试比较的是「版本级 `variables[]` 语义指纹」（key/type/required/enum set/computed），**不是**整份 `ContractResponse`（paths/policyVersion 等噪声）。允许用装配结果抽 `variables` 再 diff。 | 缩小闸门噪声 |
| **A4-C15** | **OpenAPI / 文档（强制）：** `CallableVersion.variables` 数组 + 组件 schema（建议名 `ContractVariableSchemaView`）；更新 `getTemplateApiContract` description；`contract-outline.md` API 契约查看段注明 per-field；`docs/api` 交叉引用本行为文件。实现同期同步，不得只改代码不改契约。 | IBL-A4「OpenAPI synced」 |
| **A4-C16** | **列表端点：** `GET …/versions`（`listCallableVersions`）**本叶可不**附带完整 `variables[]`（避免列表变重）；字段级契约以 `/contract` 为权威。若实现为共享 DTO 而附带，不得破坏列表既有必填字段。 | 缩小范围；`/contract` 是 F4 验收面 |
| **A4-C17** | **errorCodes 目录：** 本叶**不强制**把 `VARIABLE_VALIDATION_FAILED` 补进 `standardErrorCodes()`（A1 已文档化错误码）；允许后续卫生任务。 | 缩小范围 |
| **A4-C18** | **FE：** `frontend_ui_in_scope=false`。允许 `CallerContract` / openapi-ts 类型随字段扩展以免 type-check 破；不要求 Playwright。管理契约面板若只展示信封名，可不改 UI 文案本叶。 | 交付范围 |
| **A4-C19** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；行为变更验收面 → Stage 5/10 queued Docker deploy 证据；architecture review。 | delivery constitution |
| **A4-C20** | **完成边界：** Done ≠ Wave A 完备；≠ go-live；#3b/#5a 保持 CONDITIONAL。 | 队列政策 |

### 4.2 已确认（上游交付，本叶只消费）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **A4-U1** | Runtime generate 已对 VariableSchema fail-closed（required/type/enum） | IBL-A1 / #107 |
| **A4-U2** | `VariableSchemaEntity` 含 key/type/required/enum/pii/compute | 领域实体 |
| **A4-U3** | `/contract` 不嵌入完整 OpenAPI YAML；返回摘要 | contract-outline；OpenAPI description |
| **A4-U4** | Formal phase None；非 go-live | IBL 队列政策 |

### 4.3 非确认假设（不得升格为需求）

| ID | 陈述 | 状态 |
| --- | --- | --- |
| **A4-N1** | Publish API 硬阻断字段 rename | **非确认** — 本叶仅为 consumer/CI 测试闸（A4-C11） |
| **A4-N2** | 顶层新增与 `callableVersions[].variables` 重复的 `variableSchemas` 平行字段 | **非确认** — 版本挂载已足够；勿双写除非实现证明必要 |
| **A4-N3** | Pact/第三方 broker 强制 | **非确认** — 允许仓库内 golden + 分类器 |
| **A4-N4** | 契约响应内嵌完整 JSON Schema / OpenAPI components | **非确认** — 仍为摘要对象列表 |
| **A4-N5** | 本叶实施 retention PII 脱敏 | **非确认** — A5 |
| **A4-N6** | 删除或重命名信封级 `schemas` 字符串数组 | **非确认** — **明确保留**（A4-C3） |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 授权调用方 `GET …/contract`（runtime） | 主验收面 |
| T2 | 管理会话 `GET …/api/contract` | 同装配 per-field |
| T3 | `mvn verify` 消费者契约 / 兼容分类器测试 | Breaking gate |
| T4 | OpenAPI / contract-outline 文档同步（实现同变更集） | 契约对齐 |

---

## 6. Preconditions

| # | 前置条件 |
| --- | --- |
| PC1 | 模板已发布且至少一可调用 `releaseVersion`（成功路径含 variables） |
| PC2 | 该版本已配置 ≥1 条 `VariableSchema`（含 required TEXT 与可选 ENUM/PII 更佳） |
| PC3 | Runtime：有效 credential + 组授权；Management：有效管理会话 + 组范围 |
| PC4 | 调用方已通过既有认证/授权（本叶不放宽） |

---

## 7. Primary journey（成功）

1. 作者在发布版本上定义变量（如 `customerName` TEXT required、`letterType` ENUM、`principalAmount` AMOUNT、compute 字段、`idNumber` 带 piiCategory）。  
2. 调用方 `GET /api/dev/v1/templates/{externalId}/contract`。  
3. 响应 `callableVersions[]` 含对应 `releaseVersion`，其 `variables[]` 列出上述字段（含 type/required/enum/pii/computed），按 key 排序。  
4. 顶层 `schemas` 仍含信封类型名。  
5. CI 消费者契约测试：相对 golden 基线无 breaking diff → 通过；人为 rename → 失败。

---

## 8. System responses

### 8.1 Success

| 形态 | 响应 |
| --- | --- |
| Runtime `/contract` 200 | envelope `result.callableVersions[i].variables[]` 为字段对象列表；`schemas` 仍为 string[] |
| Management `/api/contract` 200 | 同上 variables；管理明细字段规则不变 |
| 非破坏 additive 相对基线 | 兼容分类器 → **compatible** / 测试 PASS |
| 空 schema 已发布版本 | `variables: []` |

### 8.2 Fail-closed / 闸门

| 条件 | 行为 |
| --- | --- |
| 认证/授权失败 | 既有 401/403；不返回他模板 variables |
| Breaking rename / remove / type / required↑ / enum 收缩 / enterable→computed | 消费者契约测试 **FAIL**（红灯） |
| 模板不可调用 | 既有空 `callableVersions`；不发明伪字段 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-A4-001 — Runtime `/contract` 返回 per-field 形状（F4 主验收）

**Given** 已发布模板 `TPL-*`，可调用版本 `1.0.0` 的 VariableSchema 至少含：  
- `customerName`：`TEXT`，`required=true`，`computed=false`，`piiCategory=NONE`（或等价）  
- `letterType`：`ENUM`，`required=true`，允许集含 `OFFER`  
**And** 调用方凭证与组授权有效  
**When** `GET /api/{env}/v1/templates/{externalId}/contract`  
**Then** HTTP **200**  
**And** `result.callableVersions` 中存在 `releaseVersion=1.0.0`  
**And** 该元素 `variables` 为数组，含 `variableKey=customerName` 项：`variableType=TEXT`、`required=true`、`computed=false`  
**And** 含 `variableKey=letterType` 项：`variableType=ENUM`、`required=true`、`enumValues` 含 `OFFER`  
**And** 每项含 `piiCategory`  
**And** `result.schemas` **仍为** string 数组且含 `GenerateRequest`（及既有信封名）

### BDD-IBL-A4-002 — Management `/api/contract` 同装配 per-field

**Given** 同 BDD-IBL-A4-001 模板与版本  
**And** 管理会话授权有效  
**When** `GET /api/management/v1/templates/{templateId}/api/contract?environment={env}`  
**Then** HTTP **200**  
**And** 对应 `callableVersions[].variables` 与 runtime 同版本字段语义一致（key/type/required/enum/computed/pii）  
**And** 管理面既有 policy/defaultRoute 明细可见性规则不回归

### BDD-IBL-A4-003 — Compute 字段标记 `computed=true` 且无表达式明文

**Given** 版本 schema 含 compute 字段（`COMPUTED` 或非空 `computeExpression`），key 如 `amountInWords`  
**When** runtime `/contract`  
**Then** `variables` 含该 key  
**And** `computed=true`  
**And** 响应 JSON **不**包含 `computeExpression` 字段明文  
**And** **不**包含 `defaultValue` 明文

### BDD-IBL-A4-004 — `piiCategory` as applicable（非 NONE 可见）

**Given** 版本字段 `idNumber` 的 `piiCategory=DIRECT_IDENTIFIER`（或仓库既有非 `NONE` 枚举值）  
**When** `/contract`  
**Then** 该字段投影 `piiCategory` 等于所配置非 `NONE` 值  
**And** 不返回变量原值（契约本身无 values）

### BDD-IBL-A4-005 — `variables[]` 按 `variableKey` 字典序稳定排序

**Given** 版本含无序插入的多个 variable keys（如 `zebra`, `alpha`, `middle`）  
**When** `/contract`  
**Then** 该版本 `variables[].variableKey` 序列为字典序升序（`alpha`, `middle`, `zebra`）

### BDD-IBL-A4-006 — Breaking rename → 消费者契约测试 FAIL

**Given** 固定 golden 基线版本 variables 含 `customerName`  
**And** 候选（突变）schema 将 `customerName` **重命名**为 `clientName`（旧 key 消失）  
**When** 运行 breaking-change / consumer contract 兼容分类器（`mvn verify` 内测试）  
**Then** 判定为 **BREAKING**  
**And** 该测试 **失败**（红灯）— 证明 rename 被闸住

### BDD-IBL-A4-007 — Breaking：删除字段 / 类型变更 / required 收紧 / enum 收缩

**Given** 同一 golden 基线  
**When** 分别对候选应用：(a) 删除一 key；(b) 变更 `variableType`；(c) `required` false→true；(d) ENUM 移除一允许值；(e) `computed` false→true  
**Then** 每一类突变均判定 **BREAKING** 且对应测试失败

### BDD-IBL-A4-008 — Non-breaking additive：新增 optional 字段 → PASS

**Given** golden 基线  
**And** 候选仅**新增** `optionalNote`：`TEXT`，`required=false`，`computed=false`（其余字段不变）  
**When** 兼容分类器运行  
**Then** 判定为 **NON_BREAKING** / compatible  
**And** 该测试 **通过**

### BDD-IBL-A4-009 — Non-breaking：enum 仅增值 / description-only → PASS

**Given** golden 基线含 ENUM `letterType` 允许 `OFFER`  
**When** 候选 (a) 仅增加枚举值 `REMINDER`；或 (b) 仅改 `description`  
**Then** 判定 **NON_BREAKING**  
**And** 测试通过

### BDD-IBL-A4-010 — OpenAPI / contract-outline 对齐

**Given** 本叶实现完成  
**When** 检查 OpenAPI `ContractResponse` / `CallableVersion`（或等价）与 [contract-outline.md](../api/contract-outline.md) API 契约查看段  
**Then** 文档描述 `callableVersions[].variables` per-field 形状（含 required 字段与 enum/pii/computed 规则）  
**And** 注明信封 `schemas: string[]` 仍为类型名索引  
**And** 交叉引用本行为文件（BDD-IBL-A4-001…011）  
**And** 文档与运行时 JSON 字段名一致（无已删/未实现字段）

### BDD-IBL-A4-011 — 授权失败不泄漏契约字段

**Given** 无有效 runtime 凭证（或无组授权）  
**When** `GET …/contract`  
**Then** **401** 或 **403**（既有映射）  
**And** 响应体**不**包含目标模板的 `variables` 字段列表

---

## 10. Boundary / exception

| 场景 | 行为 |
| --- | --- |
| 多版本可调用 | 每个 `callableVersions[]` 条目各自投影自己版本的 `variables`（互不串版） |
| default 路径目标版本 | 不强制顶层重复字段；用 `defaultRoute.currentTargetReleaseVersion` 关联 |
| `listCallableVersions` | 可不含 `variables`（A4-C16） |
| piiCategory 变更 | 契约输出新值；兼容闸 **不**因此失败（A4-C13） |
| 未知/非法环境 | 既有 400/404 等；本叶不新造 |
| 极长 description | 允许截断策略若已有平台惯例；不得因 description 导致 breaking |
| FE 旧客户端忽略未知字段 | JSON 新增 `variables` / `computed` 对忽略未知字段的客户端应可兼容；破坏性的是**删除**信封 `schemas`（禁止） |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | 200 `/contract` JSON：`callableVersions[].variables[]` 字段形状 |
| 否定 | 401/403 无 variables 泄漏；响应无 `computeExpression` / `defaultValue` |
| 单测 | JUnit：装配 per-field；compat BREAKING/NON_BREAKING 矩阵；slice/MVC 可选 |
| 契约 | OpenAPI + contract-outline 已更新 |
| 门禁 | `mvn verify` GREEN（含消费者契约测试红/绿断言）；queued Docker deploy 证据 |
| Trace | 既有 `metadata.traceId` 保留 |

---

## 12. Traceability

| 项 | 引用 |
| --- | --- |
| Plan | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) · F4 · **IBL-A4** |
| Task Master | **#110** |
| Requirements / PRD | 契约查看路径 `GET …/contract`；变量 Schema 属发布版本契约一部分 |
| Domain | `VariableSchemaEntity`；`VariableType`；`VariablePiiCategory` |
| API | [openapi-v1.yaml](../api/openapi-v1.yaml) `getTemplateApiContract` / `ContractResponse`；[contract-outline.md](../api/contract-outline.md) |
| Code (现状) | `ContractAssemblyService`；`ContractResultView`；`CallableVersionView` |
| Upstream | IBL-A1 校验语义；CE-G03 pii 标签 |
| Permissions | 无新 capability bit |
| Checklist | **不**翻转 #3b / #5a |

---

## 13. Out of scope（本叶）

- IBL-A5 PII retention redaction；IBL-A6 regenerate locale  
- Publish 硬阻断 rename  
- Pact broker / 外部契约市场  
- FE E2E/UIUX；管理契约面板大改版  
- 翻转 #3b/#5a；go-live；Wave A Done  
- 改变 IBL-A1 校验码或 generate 行为  

---

## 14. Ready-for-implementation handoff

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-a4-contract-field-schemas.md
task_ids: ["110"]
plan_id: IBL-A4
frontend_ui_in_scope: false
acceptance_scenario_ids:
  - BDD-IBL-A4-001
  - BDD-IBL-A4-002
  - BDD-IBL-A4-003
  - BDD-IBL-A4-004
  - BDD-IBL-A4-005
  - BDD-IBL-A4-006
  - BDD-IBL-A4-007
  - BDD-IBL-A4-008
  - BDD-IBL-A4-009
  - BDD-IBL-A4-010
  - BDD-IBL-A4-011
next_stage: backend-engineer (stage 4; stage 3 docs-first OpenAPI/outline/README synced)
contract_field_decision: >
  Keep result.schemas:string[] envelope type-name index;
  add callableVersions[].variables[] per-field projection
  (variableKey, variableType, required, computed, piiCategory,
   enumValues as applicable; no id/defaultValue/computeExpression);
  consumer contract tests FAIL on breaking renames/shape;
  PASS on additive optional / enum widen / description-only;
  OpenAPI + contract-outline synced; not publish hard-gate; not go-live.
```

**TDD Red 优先场景：** BDD-IBL-A4-001（runtime per-field）、006（rename FAIL）、008（additive PASS）、010（OpenAPI 对齐）。
