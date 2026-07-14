# BDD 行为规格：CE-C03 — fidelityWarnings 契约对齐

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-15  
**BDD ID 前缀**: `BDD-CE-C03`  
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) Wave CE-C · CE-C03  
**Slice**: `ce-c03-fidelity-warnings-contract`  
**Worktree**: `D:/working/DGE-ce-c03-fidelity-warnings-contract` · `feat/ce-c03-fidelity-warnings-contract`  
**Task Master**: **#68**  
**Formal phase**: **None**  
**完成声明约束**: 本切片关闭运行时 `fidelityWarnings` **形态**与 OpenAPI / requirements 漂移；**不**宣称 go-live；**不**实现 CE-C04/C05/C06；**不**实现 `SYNC_DOWNLOAD_URL` 重签下载；**不**改管理端 preview `FidelityWarningView` 形态。

---

## 1. 概述

运行时动态 API 对保真警告的**承载形态**必须与 OpenAPI v1 / contract-outline / requirements / PRD **一致**：

| 行为域 | 摘要 |
| --- | --- |
| **JSON 批量项 / 任务查询** | 成功项返回完整 `FidelityWarning[]` 对象（含 `warningCode`、`messageKey`、`message`、`locationSummary`、`detectedSummary`、`recommendation`、`sensitiveDataExcluded`） |
| **同步文件流** | 响应体只承载文件字节；通过响应头 `fidelityWarningCount` + `fidelityWarningCodes` 返回摘要；完整明细进入审计摘要；契约（OpenAPI + Markdown）**明确注明**该分流 |
| **契约诚信** | 禁止再以 `string[]`（仅警告码）冒充 `fidelityWarnings`；调用方可见码必须落在 OpenAPI `FidelityWarningCode` 枚举内（诚实契约） |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| OpenAPI `BatchResultItem.fidelityWarnings` / `TaskResponse.result.fidelityWarnings` / `DownloadUrlResponse.result.fidelityWarnings` 为 `FidelityWarning[]` | `docs/api/openapi-v1.yaml` |
| OpenAPI 同步 generate `200` 声明头 `fidelityWarningCount` / `fidelityWarningCodes` | 同上 headers |
| 示例 JSON 为完整对象 | `docs/api/examples/batch-response.json` |
| SoT：JSON 全量对象；同步流头摘要 + 审计明细 | `docs/api/contract-outline.md`「保真警告响应确认」；`docs/requirements/requirements-plan.md`；`docs/product/PRD.md` |
| 实现：`BatchResultItemView.fidelityWarnings` 为 `List<String>`（仅码） | `backend/.../runtime/api/BatchResultItemView.java` |
| 实现：同步流已写头摘要（码列表） | `RuntimeTemplateSyncSupport` |
| 实现：引擎侧多为 `fidelityWarningCodes: List<String>` | `DocumentGenerationEngine` / `SyncGenerateResult` / `BatchExecutionService` |
| 管理端 preview 警告形态不同（`code`/`location`/`artifact`/`viewed`） | `FidelityWarningView` — **本片不改** |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **API 调用方** | Runtime caller | 持 API 凭证 + AD Group；消费批量/任务 JSON 或同步文件流 |
| **系统** | Runtime API + 生成引擎 + 审计 | 按输出模式分流警告承载；脱敏；统一 envelope / 响应头 |
| **审计 / 管理端读者** | Audit / admin | 同步流场景可读审计中的保真警告非敏感摘要；不得见敏感明文 |
| **契约消费者** | 集成方 / OpenAPI 客户端 | 依赖 OpenAPI schema 生成客户端；形态必须可机器校验 |

---

## 3. Goal

1. 批量成功项与异步任务查询完成结果中的 `fidelityWarnings` 为**完整** `FidelityWarning` 对象数组，字段与 OpenAPI 一致。  
2. `SYNC_STREAM` 成功响应**继续**仅用头摘要表达警告，**不**把完整 `FidelityWarning[]` 塞进文件流响应体。  
3. OpenAPI / `contract-outline` **明确注明**「JSON 全量对象 vs 同步流头摘要」分流，与实现一致。  
4. 无警告时返回空数组 `[]`（JSON 成功路径），不返回字符串码列表或错误形态。  
5. 失败项通过 `error` 表达；不以残缺 `fidelityWarnings` 字符串码替代错误模型。  
6. 遵守既有脱敏：警告字段不得含模板变量原值、客户数据、粘贴原文、完整请求体、加密密码、完整生成内容；`sensitiveDataExcluded` 恒为 `true`。

---

## 4. 已确认决策（confirmed）

### 4.1 产品 / 契约基线（既有 SoT，本片落地）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **FW-1** | JSON 成功路径：`result.fidelityWarnings[]`（单笔 JSON）与 `result.batch.items[].fidelityWarnings[]`（批量）为完整对象数组 | requirements-plan / PRD / contract-outline / OpenAPI |
| **FW-2** | 异步任务查询在生成完成后，按**相同结果层级**返回保真警告（批量在 `items[]`；单笔 JSON 结果层级在 `result.fidelityWarnings[]`） | contract-outline |
| **FW-3** | 每个 `FidelityWarning` **必填**：`warningCode`、`messageKey`、`message`、`locationSummary`、`detectedSummary`、`recommendation`、`sensitiveDataExcluded`（`const: true`） | OpenAPI `FidelityWarning` |
| **FW-4** | 同步文件流：体 = 文件内容；头 `fidelityWarningCount`（integer ≥0）+ `fidelityWarningCodes`（逗号分隔警告码字符串）；完整明细进入**审计摘要** | OpenAPI headers；contract-outline |
| **FW-5** | v1 调用方可见 `warningCode` 以 OpenAPI `FidelityWarningCode` 枚举为准；ADR-0019 / contract-outline 基线五码：`OPTIONAL_CONTENT_EMPTY`、`LOW_RISK_PAGINATION_DIFFERENCE`、`LOW_RISK_TABLE_PAGE_BREAK`、`CONTROLLED_STYLE_FALLBACK`、`IMAGE_SCALING_ADJUSTED` | OpenAPI / ADR-0019 |
| **FW-6** | 保真警告是成功路径信息；阻断语义仍走发布门禁或错误模型，不以警告降级阻断项 | contract-outline |

### 4.2 本片范围锁定（confirmed for this slice）

| ID | 决策 |
| --- | --- |
| **S-1** | **必须对齐**：同步/异步**批量**响应中 `BatchResultItem.fidelityWarnings`；**任务查询**完成态中与 OpenAPI `TaskResponse` 一致的 `fidelityWarnings` 承载（含 `result.batch.items[]` 及（若该路径返回单笔结果）`result.fidelityWarnings`） |
| **S-2** | **必须保留**：`SYNC_STREAM` 头摘要行为；本片补齐/校对 OpenAPI 与 Markdown 对分流的显式说明（description / contract-outline 交叉引用），消除「以为 JSON 里也只有码」的歧义 |
| **S-3** | 头 `fidelityWarningCodes` 中的码集合必须与同次生成若走 JSON 路径时对象数组的 `warningCode` 集合**一致**（顺序：稳定、可文档化；推荐与生成顺序一致；重复码允许按实际发出条数出现） |
| **S-4** | `fidelityWarningCount` = 该次生成警告条数 = JSON 路径数组长度；无警告时为 `0`，`fidelityWarningCodes` 为空字符串 |
| **S-5** | **诚实枚举**：若运行时成功路径会向调用方发出 OpenAPI 枚举外的码（例如已在引擎路径出现的附加码），**同一变更集**必须扩展 OpenAPI `FidelityWarningCode` + 示例/说明；禁止静默返回未声明码 |
| **S-6** | 失败批量项：`status` ∈ {`FAILED`,`SKIPPED`} 时以 `error` 为准；`fidelityWarnings` 可省略或为 `[]`，**不得**返回仅含字符串码的旧形态 |
| **S-7** | 幂等重放：同步流重放须重放文件 + 同等核心响应头（含保真头）；JSON 批量/任务查询重放须重放同等完整 `FidelityWarning[]` 形态 |
| **S-8** | 明确非目标：管理端 preview `FidelityWarningView` / mark-viewed；CE-C04 `expires_at`；CE-C05 `originalBatchId` 血缘；CE-C06 DOCX permissions；`SYNC_DOWNLOAD_URL` **运行时交付**（契约 schema 已声明 `result.fidelityWarnings` 者保持形态一致声明即可，本片不实现重签下载）；管理 UI / E2E 用户旅程（本片为 runtime 契约；无管理 UI 变更则 E2E/UIUX **not-applicable**）；扩展 webhook / 定时发布 |

### 4.3 与 CE-C01+C02 边界

| ID | 决策 |
| --- | --- |
| **B-1** | CE-C01+C02 已落地 `context` + unknown-field；本片**不**回退严格校验 |
| **B-2** | CE-C01+C02 明确排除了 CE-C03；本片专收 `fidelityWarnings` 形态 |

---

## 5. Trigger

| # | 触发 |
| --- | --- |
| T1 | 调用方发起批量生成（sync 或 async），至少一笔成功且引擎产生 ≥1 条保真警告 |
| T2 | 调用方查询已完成（`SUCCEEDED` / `PARTIAL_SUCCEEDED`）的异步任务 |
| T3 | 调用方以 `output.mode=SYNC_STREAM` 单笔生成成功，且存在保真警告 |
| T4 | 同上路径但无保真警告 |
| T5 | 批量中部分项失败（async 部分成功）或整批失败（sync） |

---

## 6. Preconditions

- 调用方凭证有效、AD Group 授权通过、模板/版本可路由。  
- 目标模板策略允许所用 `output.format` / `output.mode`（`SYNC_DOWNLOAD_URL` 仍按 ADR-0038 / CE-C01 范围 defer 运行时交付）。  
- OpenAPI v1 `FidelityWarning` / headers 与 contract-outline「保真警告响应确认」为行为权威。  
- 工作树：`feat/ce-c03-fidelity-warnings-contract`。

---

## 7. Primary journey（成功）

### 7.1 批量 JSON（sync 或 async 完成后查询）

1. 调用方提交批量生成；一笔或多笔成功项触发保真警告。  
2. 系统返回（或任务查询返回）`result.batch.items[]`。  
3. 每个 **SUCCEEDED** 项的 `fidelityWarnings` 为对象数组；每项含 FW-3 全部必填字段。  
4. 调用方可用 `warningCode` / `messageKey` 做稳定集成，无需解析字符串码列表。

### 7.2 同步文件流

1. 调用方以 `SYNC_STREAM` 生成成功。  
2. 响应体为 DOCX/PDF 字节；**无** JSON envelope。  
3. 响应头含 `fidelityWarningCount` 与 `fidelityWarningCodes`（及既有核心元数据头）。  
4. 完整警告明细仅出现在审计摘要（非敏感），不出现在文件流正文。

---

## 8. System responses

### 8.1 成功 — 批量 / 任务查询 JSON

| 条件 | HTTP | 形态 |
| --- | --- | --- |
| 批量全部成功 | 200 | `BatchResponse`；成功项 `fidelityWarnings: FidelityWarning[]` |
| 异步部分成功 | 200 | `TaskResponse`；`items[].status` 区分；成功项同上 |
| 无警告成功项 | 200 | `fidelityWarnings: []` |

### 8.2 成功 — 同步文件流

| 条件 | HTTP | 形态 |
| --- | --- | --- |
| 有警告 | 200 | 二进制体 + 头 `fidelityWarningCount>0` + 非空 `fidelityWarningCodes` |
| 无警告 | 200 | 二进制体 + `fidelityWarningCount=0` + `fidelityWarningCodes=""` |

### 8.3 失败 / 边界

| 条件 | 期望 |
| --- | --- |
| 同步批量整批失败 | 既有错误信封；不冒充成功项 `fidelityWarnings` 字符串码 |
| 异步失败项 | `items[].error`；见 S-6 |
| 未授权 / 路由失败 | 既有 fail-closed 安全错误；本片不改授权 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-CE-C03-001 — 批量成功项返回完整 FidelityWarning 对象

```gherkin
Given 调用方已授权且可对模板发起批量生成
And 至少一笔成功项在生成过程中产生一条或多条保真警告
When 调用方收到同步批量成功响应（或异步任务查询至完成态）
Then result.batch.items 中每个 SUCCEEDED 项的 fidelityWarnings 为对象数组
And 每个对象包含 warningCode、messageKey、message、locationSummary、detectedSummary、recommendation、sensitiveDataExcluded
And sensitiveDataExcluded 恒为 true
And fidelityWarnings 的元素类型不是 string
```

### BDD-CE-C03-002 — 无警告时为空数组

```gherkin
Given 成功批量项未产生任何保真警告
When 调用方读取该成功项
Then fidelityWarnings 为 []（空数组）
And 不返回 null 以表示「无警告」的替代字符串码列表
```

### BDD-CE-C03-003 — 任务查询与批量项形态一致

```gherkin
Given 异步批量任务已到达 SUCCEEDED 或 PARTIAL_SUCCEEDED
When 调用方 GET 任务查询路径
Then 成功项上的 fidelityWarnings 形态与 BDD-CE-C03-001 相同
And 若该任务结果按 OpenAPI TaskResponse 在 result 层暴露单笔 fidelityWarnings，则同样为完整对象数组而非 string[]
```

### BDD-CE-C03-004 — 同步流仅头摘要

```gherkin
Given 调用方以 output.mode=SYNC_STREAM 生成成功且存在保真警告
When 调用方读取 HTTP 响应
Then 响应体为文件字节流（非 JSON envelope）
And 响应头含 fidelityWarningCount（等于警告条数）
And 响应头含 fidelityWarningCodes（逗号分隔，码集合与同次生成 JSON 路径对象 warningCode 集合一致）
And 响应体中不包含 FidelityWarning 对象数组
```

### BDD-CE-C03-005 — 同步流无警告头归零

```gherkin
Given 调用方以 SYNC_STREAM 生成成功且无保真警告
When 调用方读取响应头
Then fidelityWarningCount 为 0
And fidelityWarningCodes 为空字符串
```

### BDD-CE-C03-006 — 契约文档明示分流

```gherkin
Given 本片变更集已合并文档更新
When 集成方阅读 OpenAPI 中 SYNC_STREAM 成功响应与 FidelityWarning schema，以及 contract-outline「保真警告响应确认」
Then 可明确得知：JSON 路径返回完整 FidelityWarning[]；同步文件流仅返回头摘要，完整明细在审计摘要
And OpenAPI 头组件 FidelityWarningCount / FidelityWarningCodes 与 BatchResultItem.fidelityWarnings 的语义对照可被定位
```

### BDD-CE-C03-007 — 失败项不回退字符串码形态

```gherkin
Given 异步批量 PARTIAL_SUCCEEDED，至少一笔 FAILED
When 调用方读取失败项
Then 失败原因在 items[].error（既有 ErrorDetail）
And 该失败项若出现 fidelityWarnings 字段，则仅为省略或 []，绝不为 string[] 警告码列表
```

### BDD-CE-C03-008 — 脱敏与敏感字段禁止

```gherkin
Given 任意成功路径返回的 FidelityWarning 对象或同步流对应审计摘要
When 检查 message、locationSummary、detectedSummary、recommendation（及审计等价字段）
Then 不包含模板变量原值、客户 PII、粘贴原文、加密密码、API secret、完整请求体或完整生成文档内容
And sensitiveDataExcluded 为 true
```

### BDD-CE-C03-009 — 调用方可见 warningCode 诚实枚举

```gherkin
Given 运行时成功路径向调用方返回的任一 warningCode
When 对照 OpenAPI components.schemas.FidelityWarningCode
Then 该码属于枚举声明值
And 若实现新增调用方可见码，则同变更集已扩展 OpenAPI 枚举与示例
```

### BDD-CE-C03-010 — 幂等重放保持形态

```gherkin
Given 先前 SYNC_STREAM 或批量 JSON 成功结果已落幂等存储
When 调用方以相同 idempotencyKey 与相同请求语义重放
Then SYNC_STREAM 重放仍返回文件流 + 同等保真响应头摘要
And JSON 批量/任务查询重放仍返回完整 FidelityWarning[]（非 string[]）
```

---

## 10. Boundary / exception behavior

| 边界 | 行为 |
| --- | --- |
| 警告条数很大 | 仍返回全量数组（v1 无截断）；头 `fidelityWarningCodes` 为全部码的逗号连接；审计仍为非敏感摘要 |
| 重复同一 `warningCode` 多条 | JSON 保留多条对象；头码列表按条数重复或文档化去重策略——**本片默认按条数保留，与 count 一致** |
| `SYNC_DOWNLOAD_URL` | 运行时交付仍 defer；若契约示例/schema 展示 `result.fidelityWarnings`，形态必须是完整对象（文档一致），本片不实现重签 |
| 管理端 preview | 继续使用 `FidelityWarningView`；**禁止**为对齐 runtime 而破坏 CE-U05 viewed 字段 |
| 授权失败 | fail-closed；不泄露未授权资源是否存在 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| **API JSON** | `BatchResponse` / `TaskResponse` 中 `fidelityWarnings` 元素为对象且含 OpenAPI 必填字段 |
| **API Headers** | `SYNC_STREAM`：`fidelityWarningCount`、`fidelityWarningCodes` |
| **OpenAPI** | `FidelityWarning` schema；generate 操作 `200` headers；必要时更新 `FidelityWarningCode` 枚举与 description |
| **Docs** | `contract-outline.md` 分流说明可被 BDD-CE-C03-006 定位 |
| **Audit** | 同步流场景审计含非敏感保真警告摘要（既有 permission-matrix 允许的摘要级） |
| **Gates** | `mvn -B -ntp -f backend/pom.xml verify` 覆盖契约形态回归测试（TDD Red→Green） |
| **非证据** | 管理 UI 截图（本片无 FE 行为变更时 N/A） |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §5 CE-C03 | 计划卡 / P1·S |
| Task Master **#68** | 执行任务 |
| [openapi-v1.yaml](../api/openapi-v1.yaml) `FidelityWarning` / headers / `BatchResultItem` / `TaskResponse` | 正式 schema |
| [contract-outline.md](../api/contract-outline.md)「保真警告响应确认」 | Markdown 契约解释 |
| [requirements-plan.md](../requirements/requirements-plan.md) 保真警告 API 条款 | 需求 SoT |
| [PRD.md](../product/PRD.md) 同条款 | 产品 SoT |
| [ADR-0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) | 警告码与展示边界 |
| [permission-matrix.md](../security/permission-matrix.md) | 审计摘要允许的 fidelity 非敏感摘要 |
| [ce-c01-c02-contract-strictness.md](./ce-c01-c02-contract-strictness.md) | 前序契约片；本片专收 FW 形态 |

---

## 13. TDD Red 提示（给 implementer，非本片执行）

优先失败测试方向（runtime，非管理端）：

1. 批量成功项 JSON：`fidelityWarnings[0]` 为对象且含 `warningCode`（断言 **不是** `String`）。  
2. 任务查询完成态：同上。  
3. `SYNC_STREAM`：断言响应头 count/codes；断言 body 非 JSON warning 数组。  
4. OpenAPI 枚举与返回码一致性（契约测试或 slice 测试）。

---

## 14. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-c03-fidelity-warnings-contract.md
task_ids: [#68]
```

**Handoff**: `plan-orchestrator` → 分解 TDD / OpenAPI 文档校对任务 → `backend-engineer`（runtime 形态对齐；FE E2E **not-applicable** unless 意外触及管理 UI）。

---

## 15. Out of scope（复述）

- Go-live / CD-3 / 正式 P-phase 激活  
- CE-C04 / C05 / C06  
- `SYNC_DOWNLOAD_URL` 重签实现  
- Preview mark-viewed / 管理 UI `FidelityWarningList`  
- 改变发布门禁「警告数量不自动阻断」基线  
- Webhook / 定时发布  
