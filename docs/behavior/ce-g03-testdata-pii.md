# BDD 行为规格：CE-G03 — 测试数据 PII 治理

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-G03` |
| **编写日期** | 2026-07-15 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §6 Wave CE-G · CE-G03 |
| **Slice** | `ce-g03-testdata-pii` |
| **Worktree** | Merged to `main` (`50c1a524`); historical `D:/working/DGE-ce-g03-testdata-pii` · `feat/ce-g03-testdata-pii` |
| **Task Master** | **#74** → **done** |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | Merged (was ISOLATED) |
| **上游** | CE-U03 (#55) **Done**；CE-C05 (#70) **Done**（泳道顺序） |
| **Owning docs** | 本文件（行为 SoT）；[data-storage-view.md](../architecture/data-storage-view.md)（关闭挂起问题）；[domain-model.md](../domain/domain-model.md) §2.8 / 测试数据集规则；[requirements-plan.md](../requirements/requirements-plan.md) |

**完成声明约束：** 本切片关闭内控缺口「测试数据 PII 明文」与 `data-storage-view.md` 挂起问题；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-G04 legal hold / CE-G06 审计复现；**不**修订 ADR-0020 正文（以本规格 + domain/requirements 澄清适用范围）。

---

## 1. 概述

模板变量 Schema 缺少 PII 分类，测试数据集保存路径也无治理闸门：作者可把疑似真实客户标识写入 `variables` 持久化，而审计/摘要虽禁明文，**数据集本体**仍可能静默存放敏感值。CE-U03 已提供 schema 驱动表单与字段级校验，但**不**覆盖 PII。

本切片引入：

| 行为域 | 摘要 |
| --- | --- |
| **G03-S1 Schema 标签** | Variable Schema 增加可选 `piiCategory`（`UPPER_SNAKE_CASE` 枚举）；`NONE`/缺省 = 非 PII 治理字段 |
| **G03-S2 保存闸门** | create/update 测试数据集时：若 payload 触及任一 **PII 标记字段**（`piiCategory ≠ NONE`），必须走 **合成/脱敏声明** 或 **显式确认 + 审计**；否则 fail-closed |
| **G03-S3 审计安全** | 确认路径写耐久管理审计摘要；**禁止**审计落变量明文；仅键名、类别、`variablesHash`、处置方式 |
| **G03-S4 存储裁定** | 关闭 data-storage-view 挂起问题：允许合成/脱敏声明后的测试值入库；真实敏感测试值仅在显式确认后入库；**禁止**「仅脱敏、无声明」的静默路径 |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| Schema 无 PII 字段 | `VariableSchemaEntity` / `UpsertVariableSchemaRequest` / `VariableSchemaView` 无 `piiCategory` |
| 保存无 PII 闸门 | `UpsertTestDataSetRequest` 仅 name/description/variables/…；`TestDataSetService` 仅 U03 schema 校验 |
| 挂起问题 | [data-storage-view.md](../architecture/data-storage-view.md) Pending：「Whether any template test data may be stored after masking or must always be synthetic」 |
| U03 非目标 | [ce-u03-testdata-schema-form.md](./ce-u03-testdata-schema-form.md) U03-C15：本片不新增审计；不改变「审计不落变量明文」 |

---

## 2. Source-of-truth 与裁定

| 来源 | 陈述 | 本切片裁定 |
| --- | --- | --- |
| **CE-G03 plan** | `piiCategory`；保存时强制合成值或显式确认+审计；关闭 data-storage-view 挂起 | **确认 SoT** |
| **requirements-plan / domain** | 测试集默认脱敏或合成；确需敏感值时按敏感数据保护；审计/摘要/`variablesHash` 不落明文 | **确认** — 双路径落地该规则 |
| **ADR-0020 / Sensitive Data Classification** | 禁止明文持久化/展示「模板测试数据敏感值」于日志、审计、契约、导出等 | **澄清**：禁止面 = 审计/日志/摘要/契约/导出/未授权展示；**授权维护者**在 Test Data Set 存储中的值是测试资产本体（见 G03-C14）；确认路径不豁免审计禁明文 |
| **permission-matrix** | 维护测试数据集：全局/分组管理员、母版设计、模板编排；测试/审批只读 | **确认** — 无新角色；两路径均需既有维护权 |
| **CE-U03** | schema 表单 + 后端 fieldErrors；compute 跳过 | **确认** — 本片叠加 PII 闸门；不回退 U03 |

**Confirmed requirement（本切片）：** `piiCategory` + 测试集保存双路径（合成/脱敏声明 **或** 显式确认+审计）+ 关闭挂起问题。  
**Pending / out of scope：** 自动合成值生成器、运行时 API 入参 PII 扫描、字段级加密 at-rest、legal hold（G04）、按 invocation 再生（G06）。

---

## 3. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **模板编排 / 母版设计** | `TEMPLATE_AUTHOR` / `MASTER_DESIGNER` / `canAuthorTemplates` | 为变量设置 `piiCategory`；创建/编辑未锁定测试集并完成 PII 处置 |
| **分组 / 全局管理员** | `GROUP_ADMIN` / `GLOBAL_ADMIN` | 同维护能力（组范围 fail-closed） |
| **测试 / 审批人员** | Tester / Approver | **只读**；不可维护 schema PII 标签或测试集 |
| **系统（API）** | `TemplateBindingConfigurationService` + `TestDataSetService` | 持久化 `piiCategory`；保存前 PII 闸门；写审计摘要 |
| **系统（UI）** | 变量 Schema 编辑 + `TemplateTestDataSet` 编辑对话框（U03 表单上叠加） | 展示 PII 标记；合成声明控件；敏感确认对话框 |
| **审计读者** | 既有 `readAudit` + 组范围 | 可读 PII 确认审计摘要（无变量明文） |

授权：跨组 / 无维护权 → 既有 fail-closed。本规格**不**改 permission-matrix。

---

## 4. Goal

1. 作者可为每个模板变量声明可选 `piiCategory`；缺省/`NONE` 表示非 PII 治理字段。  
2. 保存（create/update）测试数据集时，若涉及任一 PII 标记字段的值，系统 **fail-closed** 要求：`SYNTHETIC`（合成/脱敏声明）**或** `EXPLICIT_SENSITIVE`（显式确认 + 原因 + 二次确认）之一。  
3. `EXPLICIT_SENSITIVE` 成功路径写入耐久管理审计；审计**永不**含变量明文。  
4. 无 PII 标记字段的数据集保存行为与 CE-U03 一致（仅 schema 校验）。  
5. 关闭 data-storage-view 挂起问题，答案可追溯到本规格 G03-C14。  
6. 不宣称 go-live。

---

## 5. 已确认决策（confirmed）

### 5.1 `piiCategory` on Variable Schema

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G03-C1** | Variable Schema 增加可选字段 `piiCategory`（API/DB/UI）。类型：`UPPER_SNAKE_CASE` 枚举。 | CE-G03 plan |
| **G03-C2** | **v1 枚举：** `NONE`、`PERSONAL_NAME`、`GOVERNMENT_ID`、`FINANCIAL_ACCOUNT`、`CONTACT`、`ADDRESS`、`OTHER_SENSITIVE`。未知值 → 校验失败（fail-closed）。 | CE-G03 + Sensitive Data Classification 落地；见非阻塞 Q1 |
| **G03-C3** | **缺省：** 未提供或 `null` 持久化为 `NONE`（或列默认 `NONE`）。`NONE` = **不**触发测试集 PII 闸门。 | 向后兼容存量 schema |
| **G03-C4** | **PII 标记字段** = `piiCategory ≠ NONE`。Compute 字段（`COMPUTED` / 非空 `computeExpression`）仍跳过录入与 U03 校验；若误标 PII，闸门仍按其是否出现在 payload 判定（见 G03-C9）。 | U03-C8 + 本片 |
| **G03-C5** | Schema upsert（变量创建/更新）可读写 `piiCategory`；导出/导入 bundle 携带该字段（若当前 bundle 含 variables）。本片**不**强制迁移存量 demo 数据打标（可后续补丁）；未打标 = `NONE`。 | 兼容 |
| **G03-C6** | UI：变量 Schema 编辑提供 `piiCategory` 选择（English-first 标签 + zh-CN）；PII 标记字段在测试集表单上有可见标识（badge/hint）。 | OA + i18n |

### 5.2 测试集保存闸门

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G03-C7** | **触发条件：** create/update 时，当前模板 VariableSchema 存在至少一个 PII 标记字段，**且**请求 `variables` 对该字段提供了非空值（null/缺省/空串对 optional 视为未提供，**不**触发；required 空值仍先走 U03 `REQUIRED`）。 | plan「标记字段保存」 |
| **G03-C8** | **未触发**（无 PII 标记，或所有 PII 字段均未提供值）：不要求 `piiHandling`；行为 = U03 only。 | 最小打扰 |
| **G03-C9** | **触发后必须**在请求中提供 `piiHandling`：`SYNTHETIC` **或** `EXPLICIT_SENSITIVE`。缺失/非法 → **422** `VALIDATION` + 稳定 `messageKey`（如 `api.error.template.testDataSetPiiHandlingRequired`）+ 可选 `fieldErrors` 指向 `piiHandling`。 | plan 双路径 |
| **G03-C10** | **`SYNTHETIC`：** 作者声明 payload 中所有已提供的 PII 标记字段值均为**合成或已脱敏**测试值（非真实客户可识别信息）。**不**要求 `piiConfirmReason` / `secondaryConfirmed`。系统**不**做自动真伪检测（见 G03-C16）。成功保存可写可选信息级审计（实现可选）；**不强制**。 | requirements「默认脱敏或合成」 |
| **G03-C11** | **`EXPLICIT_SENSITIVE`：** 作者声明确需保存敏感测试值。强制：`piiConfirmReason` 非空白（≤2048）+ `secondaryConfirmed=true`。任一缺失 → 422 + 既有/新增 messageKey（如 `api.error.template.piiConfirmReasonRequired` / `piiSecondaryConfirmRequired`）。 | plan「显式确认」；对齐 G01 例外形态 |
| **G03-C12** | **权限：** 两路径均只需既有测试集维护权；**不**限 `GROUP_ADMIN`。 | permission-matrix；作者日常路径 |
| **G03-C13** | **校验顺序：** 授权 → 锁定不可变（既有）→ U03 schema 校验 → **PII 闸门** → 持久化（→ 审计若 EXPLICIT）。PII 失败不写数据集变更。 | fail-closed |
| **G03-C14** | **关闭挂起问题（data-storage-view）：** Template Test Data Set **可以**持久化变量值，当且仅当：(a) 字段未标 PII，或 (b) PII 字段走 `SYNTHETIC` 声明（合成/脱敏测试值），或 (c) PII 字段走 `EXPLICIT_SENSITIVE` 确认+审计。**禁止**无声明的「掩码后静默入库」作为第三路径。脱敏/合成归入 (b)；真实敏感归入 (c)。 | 挂起问题关闭 |
| **G03-C15** | **存储位置：** 测试值仍存于既有 Test Data Set `variables`（JSON）。本片**不**引入字段级加密或独立 vault。at-rest 依赖平台既有 DB/磁盘加密基线（ADR-0030）。 | 范围 |
| **G03-C16** | **合成真实性：** v1 **仅声明制**；不扫描、不比对合成词库、不拒绝「看起来像真名」的 `SYNTHETIC` 值。滥用由审计抽样与流程治理，非本片自动执法。 | 可实现边界 |
| **G03-C17** | **derive / delete / lock / list / get / preview：** 不新增 PII 确认闸门。derive 产生的新草稿若随后 update 且仍含 PII 值，仍走 G03-C7。Locked 集不可 update（既有）。 | 最小范围 |
| **G03-C18** | **UI：** 当 schema 含 PII 标记且作者为 PII 字段填了值：Save 前展示处置选择（默认推荐 `SYNTHETIC`）；选 `EXPLICIT_SENSITIVE` 时弹确认框（reason + secondary confirm）再发请求。客户端可预检；**后端仍为权威**。 | U03 叠加 |

### 5.3 审计与敏感展示

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G03-C19** | `EXPLICIT_SENSITIVE` 成功保存必须写 `management_audit_event`（或等价 ManagementAuditRecorder）耐久行。建议 `eventType`：`TEMPLATE_TEST_DATA_PII_EXPLICIT_CONFIRM`（实现可微调，须稳定可查）。 | plan「+审计」 |
| **G03-C20** | 审计摘要字段（安全）：`eventAt`、actor 摘要、`templateId`/`testDataSetId`、`datasetVersion`、`variablesHash`、PII 字段 **keys** 列表、各 key 的 `piiCategory`、`piiHandling=EXPLICIT_SENSITIVE`、`piiConfirmReason`（原因允许进审计；**不是**变量值）、`traceId`/`auditId`。**禁止**变量明文、完整 `variables` JSON、完整请求体。 | ADR-0020；requirements |
| **G03-C21** | `SYNTHETIC` 成功：**不强制**审计行；若写，同样禁明文。 | 降噪 |
| **G03-C22** | 管理端测试集编辑/查看：对**已授权维护/只读测试视角**继续展示数据集变量值（测试资产用途）。契约页、调用示例、审计导出、日志、发布证据摘要**不得**回显这些明文（既有规则保持）。本片不扩大契约示例暴露面。 | Sensitive Data Classification 澄清 |

### 5.4 范围锁定 / 非目标

| ID | 决策 |
| --- | --- |
| **G03-C23** | **明确非目标：** runtime 生成 API 入参 PII 扫描；自动合成数据生成器；字段级 KMS 加密；CE-G04 legal hold；CE-G06 受控再生；修改 ADR-0020/0021 正文；go-live / CD-3；改变锁定/派生/删除语义；改变 permission-matrix |
| **G03-C24** | Formal phase 保持 **None**；完成切片后由 post-task-doc-sync 更新 CE 计划行与 Task Master #74 |

---

## 6. Trigger

| # | 触发 |
| --- | --- |
| T1 | 维护者 upsert 变量 Schema 并设置/更改 `piiCategory` |
| T2 | 维护者 create/update 测试数据集，且 payload 触及 PII 标记字段非空值 |
| T3 | 维护者在 UI 选择 `SYNTHETIC` 或 `EXPLICIT_SENSITIVE` 后 Save |
| T4 | API 直接 create/update（绕过 UI）且触发 G03-C7 |

---

## 7. Preconditions

| # | 前置 |
| --- | --- |
| PC1 | 用户已登录且对模板具备维护测试数据集 / 变量 Schema 权限 |
| PC2 | 模板已加载 VariableSchema（含 `piiCategory`）；Testing 面板可用（U03） |
| PC3 | 目标数据集未锁定（update 路径） |
| PC4 | CE-U03 行为已在主干可用（schema 校验 + 动态表单） |
| PC5 | E2E：Docker `4173`/`8080` |

---

## 8. Primary journey（成功路径）

1. 编排人员在变量 Schema 将 `customerName` 标为 `PERSONAL_NAME`，`accountNo` 标为 `FINANCIAL_ACCOUNT`。  
2. 打开创建测试数据集（U03 表单）；PII 字段有标识。  
3. 填入合成样例值；选择 **Synthetic / desensitized** 声明 → Save。  
4. 后端：U03 校验通过 → PII 闸门接受 `SYNTHETIC` → 持久化；列表刷新。  
5. （例外路径）作者确需接近真实样例：选择 Explicit sensitive → 填写 reason + 二次确认 → Save → 数据集保存 + 审计行（无明文）。  

---

## 9. System responses

### 9.1 Success

| 响应 | 可观测证据 |
| --- | --- |
| Schema 含 `piiCategory` | API `VariableSchemaView.piiCategory`；DB 列；UI 选择器 |
| `SYNTHETIC` 保存 | HTTP 2xx；数据集 `variables` 已存；无强制审计或仅安全摘要 |
| `EXPLICIT_SENSITIVE` 保存 | HTTP 2xx；`management_audit_event` 含 keys/categories/reason/`variablesHash`；**无**变量值 |
| 无 PII 触发 | 与 U03 相同；可不传 `piiHandling` |

### 9.2 Fail-closed / 错误

| 条件 | 响应 |
| --- | --- |
| 触发 PII 且无 `piiHandling` | 422 `VALIDATION` + `testDataSetPiiHandlingRequired`；不持久化 |
| `EXPLICIT_SENSITIVE` 缺 reason / 未二次确认 | 422 + 对应 messageKey；不持久化 |
| 非法 `piiCategory` / `piiHandling` | 422 VALIDATION |
| U03 schema 失败 | 既有 fieldErrors；可在 PII 闸门之前失败 |
| 无维护权 / 锁定集 | 既有 403 / immutable |
| 审计写入失败（EXPLICIT） | **fail-closed**：不提交数据集变更（或事务回滚）；不得出现「已存敏感值但无审计」 |

---

## 10. Acceptance scenarios（Given / When / Then）

### Schema

#### BDD-CE-G03-001 — Upsert variable with piiCategory

**Given** 维护者可编辑模板变量 Schema  
**When** upsert 变量 `customerName` 且 `piiCategory=PERSONAL_NAME`  
**Then** 持久化并在 `VariableSchemaView` 返回 `PERSONAL_NAME`

#### BDD-CE-G03-002 — Default NONE

**Given** upsert 变量时省略 `piiCategory`  
**When** 保存成功  
**Then** 该变量 `piiCategory` 为 `NONE`（或不触发 PII 闸门的等价缺省）

#### BDD-CE-G03-003 — Reject unknown piiCategory

**Given** upsert 请求 `piiCategory=SSN`（非枚举）  
**When** 提交  
**Then** 422 VALIDATION；不持久化非法值

### Save gate — happy paths

#### BDD-CE-G03-004 — Non-PII save unchanged

**Given** schema 全部为 `NONE`  
**When** create 测试集（合法 variables，无 `piiHandling`）  
**Then** 保存成功（U03 规则）

#### BDD-CE-G03-005 — PII field empty optional does not require handling

**Given** 可选字段 `customerName` 为 `PERSONAL_NAME`，payload 省略该键  
**When** create 其它非 PII 字段合法且无 `piiHandling`  
**Then** 保存成功

#### BDD-CE-G03-006 — SYNTHETIC save allowed

**Given** `customerName`=`PERSONAL_NAME` 且 payload 提供非空值  
**And** `piiHandling=SYNTHETIC`  
**When** create/update  
**Then** HTTP 2xx；值已持久化

#### BDD-CE-G03-007 — EXPLICIT_SENSITIVE save with audit

**Given** PII 字段非空  
**And** `piiHandling=EXPLICIT_SENSITIVE`、`piiConfirmReason` 非空、`secondaryConfirmed=true`  
**When** create/update  
**Then** HTTP 2xx；数据集已存  
**And** 存在审计事件含 template/testDataSet ids、PII keys、categories、reason、`variablesHash`  
**And** 审计 payload **不含**变量明文

### Save gate — fail-closed

#### BDD-CE-G03-008 — Missing piiHandling blocked

**Given** PII 字段非空且请求无 `piiHandling`  
**When** create/update  
**Then** 422 + `api.error.template.testDataSetPiiHandlingRequired`（或文档化等价 key）  
**And** 数据集未变更

#### BDD-CE-G03-009 — EXPLICIT without reason blocked

**Given** `piiHandling=EXPLICIT_SENSITIVE` 且 `piiConfirmReason` 空白  
**When** create/update  
**Then** 422；不持久化；无成功审计行

#### BDD-CE-G03-010 — EXPLICIT without secondary confirm blocked

**Given** `piiHandling=EXPLICIT_SENSITIVE` 且 `secondaryConfirmed≠true`  
**When** create/update  
**Then** 422；不持久化

#### BDD-CE-G03-011 — API bypass still enforced

**Given** 直接调用 management API（无 UI）且触发 G03-C7  
**When** 无合法 `piiHandling`  
**Then** 与 BDD-CE-G03-008 相同失败（后端权威）

### UI / UX

#### BDD-CE-G03-012 — Form shows PII marker

**Given** schema 含 `PERSONAL_NAME` 字段  
**When** 打开创建/编辑测试集对话框  
**Then** 该字段有可见 PII 标识（i18n）

#### BDD-CE-G03-013 — UI requires handling before save

**Given** 作者为 PII 字段填入值  
**When** 未选择处置并点击 Save  
**Then** 客户端阻断或发出后映射 422；不得静默成功

#### BDD-CE-G03-014 — Explicit confirm dialog

**Given** 作者选择 Explicit sensitive  
**When** 确认保存  
**Then** 必须填写 reason 并勾选二次确认后才发请求

### Boundaries

#### BDD-CE-G03-015 — Locked set still immutable

**Given** 已锁定测试集  
**When** update（即使带合法 PII handling）  
**Then** 既有不可变错误；PII 闸门不改变该优先级

#### BDD-CE-G03-016 — Audit never echoes values

**Given** 任意成功或失败路径  
**When** 检查日志与审计记录  
**Then** 无模板变量测试值明文

#### BDD-CE-G03-017 — Storage view hanging question closed

**Given** [data-storage-view.md](../architecture/data-storage-view.md)  
**When** 阅读 Pending Questions  
**Then** 「masking vs always synthetic」项已关闭并指向本规格 G03-C14（或 Confirmed Data Rules 等价陈述）

---

## 11. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 仅 `NONE` 字段 | 无 PII 闸门 |
| PII + required 空 | U03 `REQUIRED` 优先 |
| Compute + 误标 PII | 不要求录入；若 payload 显式带非空 compute key，实现可剥离（U03）或不触发闸门（无业务值）；不得因 compute 单独要求 EXPLICIT |
| 并发最后写获胜 | 既有；本片不加乐观锁 |
| 导入旧 bundle 无 `piiCategory` | 视为 `NONE` |
| 审计写失败 | EXPLICIT 路径整笔失败（G03 §9.2） |

---

## 12. Observable evidence

| 层 | 证据 |
| --- | --- |
| DB / API | `variable_schema.pii_category`；upsert/view 字段；`UpsertTestDataSetRequest` 含 `piiHandling` / reason / secondaryConfirmed |
| 审计 | `TEMPLATE_TEST_DATA_PII_EXPLICIT_CONFIRM`（或文档化名）行；无明文 |
| UI | PII badge；处置选择；显式确认对话框 |
| 测试 | JUnit：001–011、015–016；Vitest：012–014；Playwright E2E + UIUX（用户可见面） |
| Docs | data-storage-view 挂起关闭；domain/requirements 同步 |

---

## 13. Traceability

| 项 | 路径 / ID |
| --- | --- |
| Program | CE-G03 · [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) |
| Task Master | **#74** |
| Slice | `ce-g03-testdata-pii` |
| BDD | 本文件 `BDD-CE-G03-001…017` |
| Requirements | 测试数据集默认合成/脱敏；敏感值需保护 |
| Domain | §2.8 Template Variable；§2.17 Sensitive Data Classification；测试数据集规则 |
| Architecture | [data-storage-view.md](../architecture/data-storage-view.md) |
| Security | ADR-0020（适用范围澄清，不改 ADR 正文）；permission-matrix 维护边界 |
| Upstream | [ce-u03-testdata-schema-form.md](./ce-u03-testdata-schema-form.md) |
| Touchpoints（参考） | `VariableSchemaEntity`、`TestDataSetService`、`UpsertTestDataSetRequest`、U03 编辑对话框、ManagementAuditRecorder |

---

## 14. Open questions（非阻塞 / residual）

| # | 问题 | 本片默认 | 阻塞？ |
| --- | --- | --- | --- |
| Q1 | `piiCategory` 枚举是否需更细（如拆分 EMAIL/PHONE）？ | v1 用 G03-C2 七值；CONTACT 覆盖电邮/电话 | 否 |
| Q2 | `SYNTHETIC` 是否强制写审计？ | **否**（G03-C21） | 否 |
| Q3 | `variablesHash` 算法是否新建？ | **复用**平台既有 hash 工具（若无则 SHA-256 canonical JSON） | 否 |
| Q4 | 导出包是否必须含 `piiCategory`？ | **是**（若 variables 已在 bundle） | 否 |

**无阻塞 pending questions。**

---

## 15. BDD readiness

```
bdd_readiness: ready
acceptance_scenario_count: 17
open_questions: [Q1, Q2, Q3, Q4]  # non-blocking defaults above
owning_doc: docs/behavior/ce-g03-testdata-pii.md
task_ids: [CE-G03, #74, ce-g03-testdata-pii]
next: plan-orchestrator → doc-keeper（轻量已由本片同步 SoT）→ backend-engineer + frontend-engineer（full-stack，feature worktree）
```

**禁止本片宣称 go-live。** Formal phase 保持 **None**。
