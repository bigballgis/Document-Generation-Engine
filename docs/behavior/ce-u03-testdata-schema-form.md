# BDD 行为规格：CE-U03 — 测试数据集 schema 驱动表单

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-14  
**BDD ID**: `BDD-CE-U03-TESTDATA-SCHEMA-001`  
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 Wave CE-U · **CE-U03**  
**Slice**: `ce-u03-testdata-schema-form`  
**Task**: CE-U03 测试数据集 schema 驱动  
**bdd_readiness**: **`ready`**  
**Worktree**: `D:/working/DGE-ce-u03-testdata-schema-form` · `feat/ce-u03-testdata-schema-form`  
**Formal phase**: **None**（非正式 P-phase；不 invent sole-active）  
**授权依据**: Parent / delivery-orchestrator Stage 1 — 本会话明确切片目标（动态表单 + 骨架 + 折叠 JSON + 后端 schema 字段级校验 + compute 跳过）

---

## 1. 概述

今日模板测试数据集编辑器是 **8 行 textarea 手写 JSON**：创建默认值硬编码 `{"customerName":"Sample"}`，保存仅 `JSON.parse`，后端 `TestDataSetService` **不**对照模板 `VariableSchema` 校验。作者无法按类型/必填即时纠错，错误要到预览/生成才暴露。

本切片将创建/编辑测试数据集的变量录入改为 **按当前模板 `VariableSchema` 驱动的动态表单**，并保留高级 JSON 折叠编辑；后端在 create/update 时按 schema **fail-closed** 校验并返回 `fieldErrors`。

| 行为域 | 摘要 |
| --- | --- |
| **TD-F1 动态表单** | 按 `VariableSchema` 渲染字段控件；类型与必填 **即时校验**（blur/change + Save 前） |
| **TD-F2 生成骨架** | 「从 schema 生成骨架」一键填充 typed skeleton（用 schema `defaultValue` / 类型占位；**禁止**硬编码 `customerName`） |
| **TD-F3 折叠 JSON** | 大 payload / 高级用户可用 **可折叠** JSON 编辑器；与表单双向同步；非法 JSON 阻断保存 |
| **TD-B1 后端校验** | `TestDataSetService` create/update 对照模板变量 schema；失败 → `VALIDATION` + `fieldErrors[]`（点路径） |
| **TD-K03 弱耦合** | 存在 compute 变量时，表单与保存校验 **跳过** 这些字段（不要求作者填；不因缺失 fail） |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| CE-K03 表达式求值 / SPELL_AMOUNT / 管理端表达式预览 | **Out of scope** — 本片仅「跳过 compute 字段」弱耦合 |
| 改变测试数据集锁定 / 派生 / 删除 / 预览触发语义 | **禁止** — 既有 locked/derive/preview 行为保持 |
| 跨模板共享测试数据集库 | **Out of scope**（requirements 未确认） |
| 改变 permission-matrix 角色能力 | **禁止** — 仍仅维护角色可写；测试/审批只读 |
| 站内 PDF 预览（CE-U04）、嵌套块编辑（CE-U01） | **Out of scope** |
| 宣称 production go-live / 激活 CD-3 | **禁止** |

---

## 2. Source-of-truth 与裁定

| 来源 | 陈述 | 本切片裁定 |
| --- | --- | --- |
| **CE-U03 plan** | 动态表单 + 骨架按钮 + 大 payload 折叠 + 后端 schema 字段级错误；compute 跳过 | **确认 SoT** |
| **[requirements-plan.md](../requirements/requirements-plan.md)** / **[PRD.md](../product/PRD.md)** | 测试数据集驱动 schema 校验；维护权限边界；证据链锁定不可变 | **确认** — 本片加强「变量 Schema 校验」在 **保存时** 的可观测性；不改锁定规则 |
| **[permission-matrix.md](../security/permission-matrix.md)** | 维护测试数据集：全局/分组管理员、母版设计、模板编排；测试/审批只读 | **确认** — 无权限变更 |
| **[domain-model.md](../domain/domain-model.md) §2.8** | 变量类型含文本/数字/金额/日期/枚举/布尔/列表/对象 + 计算表达式 | **确认** — 表单覆盖可录入类型；`COMPUTED` / compute 跳过 |
| **API 错误模型** | `fieldErrors[].reason` 枚举含 `REQUIRED`、`INVALID_TYPE`、`INVALID_FORMAT`、`ENUM_NOT_ALLOWED`、`UNKNOWN_FIELD` 等 | **确认** — 后端校验错误映射到既有 `FieldError` |
| **现状实现** | textarea + `customerName` 硬编码；`TestDataSetService` 无 schema 校验 | **待替换行为** — 不得保留为验收基线 |

**Confirmed requirement（本切片）:** schema 驱动表单 + 骨架 + 折叠 JSON + 后端字段级校验 + compute 跳过。  
**Pending / out of scope:** K03 求值引擎；深层嵌套 OBJECT/LIST 的可视化拖拽编排。

---

## 3. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **模板编排人员** | `TEMPLATE_AUTHOR` / `MASTER_DESIGNER` / 具备 `canAuthorTemplates` | 在模板 Testing 面板创建/编辑未锁定测试数据集 |
| **分组 / 全局管理员** | `GROUP_ADMIN` / `GLOBAL_ADMIN` | 同维护能力（组范围 fail-closed） |
| **测试 / 审批人员** | Tester / Approver | **只读**查看；不得维护（既有矩阵） |
| **系统（UI）** | `TemplateTestDataSetPanel` / Edit dialog（schema form） | 读模板 `variables`；渲染表单 / 骨架 / 折叠 JSON |
| **系统（API）** | `TestDataSetService` + `TestDataSetController` | create/update 时加载模板 VariableSchema 并校验 |

授权：跨组 / 无维护权 → 既有 fail-closed（`TemplateAccessDeniedException` / 403）。本规格不改 permission-matrix。

---

## 4. Goal

1. 作者打开创建/编辑对话框时，看到 **按当前模板 VariableSchema 生成的动态表单**，而非空白/硬编码 JSON textarea 作为主录入面。  
2. 类型错误与必填缺失在 **客户端即时** 提示；Save 前客户端阻断明显非法输入。  
3. 一键 **从 schema 生成骨架**，消除 `customerName` 硬编码。  
4. 变量多 / payload 大时，可通过 **可折叠 JSON 编辑器** 批量编辑；与表单同步。  
5. 后端 create/update **必须**按 schema 校验；失败返回可映射到字段的 `fieldErrors`（纵深防御，即使用 API 绕过 UI）。  
6. Compute 变量（见决策 **U03-C8**）不出现在必填表单中，也不因缺失而校验失败。

---

## 5. 已确认决策（confirmed）

| ID | 决策 |
| --- | --- |
| **U03-C1** | **作用面**：管理端模板详情 Testing / Dev workspace 内 `TemplateTestDataSetPanel` 的创建与编辑对话框；后端 `POST/PUT`（或既有 upsert 路径）`TestDataSetService.create` / `update`。 |
| **U03-C2** | **Schema 来源**：当前模板已加载的 `VariableSchema[]`（与详情页 variables 同源）。对话框打开时以最新已加载 schema 为准；若 schema 为空 → 显示空态说明 + 仅允许 `{}` 或空对象保存（无 required 失败）。 |
| **U03-C3** | **主录入面 = 动态表单**：每个**可录入**变量一行（或嵌套组）；控件按 `variableType`：`TEXT`→text；`NUMBER`/`AMOUNT`→number（AMOUNT 允许小数）；`DATE`→date；`BOOLEAN`→switch/checkbox；`ENUM`→select（`enumValues`）；`LIST`/`OBJECT`→见 **U03-C4**。标签优先 `description`，否则 `variableKey`（既有 display-name 约定）。`required=true` 标必填。 |
| **U03-C4** | **LIST / OBJECT**：一层内联：OBJECT 展开子键（若 schema 描述了子结构则用子字段；否则该键用折叠 JSON 子编辑）；LIST 提供「添加/删除项」+ 项内标量或 JSON。**深度 > 1 或不规则结构** → 该字段回退到字段级折叠 JSON，不阻塞本片。 |
| **U03-C5** | **即时校验**：客户端在 change/blur 与 Save 时校验：required 非空；类型可解析；ENUM 在允许集。文案 English-first i18n + zh-CN。非法时字段旁错误，Save 不发请求。 |
| **U03-C6** | **「从 schema 生成骨架」按钮**：可见于创建与编辑对话框。点击后用 schema 生成变量对象：对可录入字段，优先 schema `defaultValue`（按类型解析）；否则类型占位（`TEXT`→`""`，`NUMBER`/`AMOUNT`→`0`，`BOOLEAN`→`false`，`DATE`→`""` 或 ISO 空提示占位、`ENUM`→首个 enum 或 `""`，`LIST`→`[]`，`OBJECT`→`{}`）。**跳过 compute 字段**。生成后写入表单模型并同步折叠 JSON。**覆盖确认**：若当前 payload 非空且与骨架不同，二次确认后再覆盖。 |
| **U03-C7** | **折叠 JSON 编辑器**：对话框提供「Advanced / Raw JSON」可折叠区。默认：**变量数 ≥ 12 或序列化 JSON ≥ 2 KiB 时折叠区默认展开**；否则默认折叠（表单为主）。表单改动 → JSON 文本同步；JSON 合法解析后 → 表单同步。JSON 语法非法 → 字段级提示 + Save 阻断（客户端）；后端仍校验对象形态。**禁止**以 8 行无折叠 textarea 作为唯一录入面。 |
| **U03-C8** | **Compute 跳过（K03 弱耦合）**：满足任一则视为 compute 字段，**不渲染输入控件、不参与 required、骨架不生成、后端校验忽略缺失/类型**：`variableType == COMPUTED` **或** `computeExpression` 非空白。K03 落地前后行为一致。若 payload **显式携带** compute key：本片允许忽略或剥离后保存（实现二选一，须文档化）；**不得**因「多了 compute key」单独失败为硬阻断（优先 `UNKNOWN_FIELD` 仅针对非 schema 键，见 U03-C10）。 |
| **U03-C9** | **后端校验时机**：`TestDataSetService.create` 与 `update` 在持久化前，加载该模板 VariableSchema，对 `request.variables()` 校验。`derive` / `delete` / `lockForEvidence` / `list` / `get` **不**新增本片校验。Locked 更新仍走既有 `TestDataSetImmutableException`。 |
| **U03-C10** | **校验规则（后端，fail-closed）** | 对每个**可录入** schema 变量：`required` 且值缺失/null/空串 → `REQUIRED`；值存在但类型不匹配 → `INVALID_TYPE`（DATE 非法格式 → `INVALID_FORMAT`）；ENUM 不在集合 → `ENUM_NOT_ALLOWED`。payload 中 **不在 schema 且非 compute** 的 key → `UNKNOWN_FIELD`。`field` 使用点路径（如 `borrower.legalName`、`items[0].amount`）。HTTP：**422**（或项目既有 validation 状态）+ `error.category=VALIDATION` + 稳定 `messageKey`（如 `api.error.template.testDataSetSchemaInvalid`）+ `fieldErrors[]`。 |
| **U03-C11** | **前端映射后端错误**：Save 若返回 `fieldErrors`，对话框按 `field` 路径高亮对应表单控件（或 JSON 区摘要列表）；不得仅 toast 泛化「save failed」。 |
| **U03-C12** | **去除硬编码默认**：删除 `{"customerName":"Sample"}` 作为创建默认；创建打开时：空对象或自动骨架（实现可选：**推荐**打开创建时若 schema 非空则自动应用骨架且不弹覆盖确认；编辑打开则加载已存 variables）。 |
| **U03-C13** | **i18n / OA**：新文案 English-first；bank OA 表单密度与既有 dialog 一致；无品牌特例。 |
| **U03-C14** | **测试门禁**：后端 JUnit 覆盖 schema 通过/必填失败/类型失败/ENUM/UNKNOWN/compute 跳过；前端 Vitest 覆盖表单渲染、骨架、即时校验、JSON 同步；用户可见面 → Playwright E2E + UIUX（Docker 4173）。 |
| **U03-C15** | **审计 / 敏感**：不改变「审计不落变量明文」；本片不新增审计事件要求。 |

---

## 6. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 点击 Testing 面板「创建测试数据集」 | 打开对话框 + schema 表单 |
| T2 | 点击未锁定行的「编辑」 | 打开对话框 + 预填 variables |
| T3 | 点击「从 schema 生成骨架」 | 生成/覆盖变量模型 |
| T4 | 展开/编辑折叠 JSON | 双向同步 |
| T5 | 点击 Save | 客户端校验 → API create/update → 后端 schema 校验 |
| T6 | API 直接 create/update（绕过 UI） | 后端仍 schema 校验 |

---

## 7. Preconditions

| # | 前置条件 |
| --- | --- |
| PC1 | 用户已登录且对模板具备维护测试数据集权限（`canAuthorTemplates` + 组可读） |
| PC2 | 模板详情已加载（含 `variables` / VariableSchema）；Testing 面板可用 |
| PC3 | 目标数据集未锁定（编辑路径）；锁定集仅可派生（既有） |
| PC4 | Docker 验收栈：`http://localhost:4173` + `http://localhost:8080`（E2E） |

---

## 8. Primary journey（成功路径）

1. 编排人员打开模板详情 → Testing（或 Dev workspace 测试数据集区）。  
2. 点击创建 → 对话框展示 schema 动态表单（无 `customerName` 硬编码默认）。  
3. （可选）点击「从 schema 生成骨架」→ 字段填入占位/默认值；compute 字段不出现。  
4. 填写必填与类型正确的值；大 schema 时展开 Advanced JSON 微调后折叠回表单。  
5. Save → 客户端校验通过 → API 成功 → 列表刷新；新建行可选中。  
6. 编辑已有集 → 表单反映已存值 → 改一字段 Save → 后端通过并持久化。

---

## 9. System responses

### 9.1 Success

| 响应 | 可观测证据 |
| --- | --- |
| 对话框渲染 schema 字段 | DOM：每可录入 `variableKey` 有对应控件；无硬编码 Sample JSON 作为唯一面 |
| 骨架生成 | 表单/JSON 含 schema 键；无 compute 键要求 |
| Save 成功 | HTTP 2xx；列表出现/更新行；toast success（既有 i18n） |
| 后端接受合法 payload | 持久化 `variablesJson`；后续 preview 可选用该集 |

### 9.2 Fail-closed / 错误

| 条件 | 响应 |
| --- | --- |
| 客户端必填/类型失败 | 字段错误；不发请求 |
| 后端 schema 失败 | 422 + `VALIDATION` + `fieldErrors`；UI 映射到字段 |
| 无维护权限 | 既有 403 / UI 无创建按钮 |
| 锁定集编辑 | 既有不可变；按钮 disabled |
| JSON 语法非法 | 客户端阻断；提示 INVALID_FORMAT 语义文案 |

---

## 10. Acceptance scenarios（Given / When / Then）

### Frontend

#### S1 — 动态表单按 schema 渲染

**Given** 模板 VariableSchema 含 `customerName`（TEXT, required）、`amount`（AMOUNT, optional）、`status`（ENUM）  
**And** 编排人员打开「创建测试数据集」  
**When** 对话框渲染完成  
**Then** 主录入面为上述字段的动态表单控件（非仅 8 行硬编码 Sample textarea）  
**And** `customerName` 标为必填

#### S2 — 类型即时校验

**Given** 打开创建对话框且 schema 含 `amount`（AMOUNT）  
**When** 用户在 `amount` 输入不可解析为数字的文本并触发校验（blur 或 Save）  
**Then** 该字段显示类型错误（i18n）  
**And** Save 不发起 API 请求

#### S3 — 必填即时校验

**Given** schema 含 required `customerName`  
**When** 字段为空时点击 Save  
**Then** 显示必填错误  
**And** 不发起 API 请求

#### S4 — 从 schema 生成骨架

**Given** schema 含 `customerName`（defaultValue=`"Acme"`）、`flag`（BOOLEAN, 无默认）、无硬编码依赖  
**When** 用户点击「从 schema 生成骨架」（空表单或确认覆盖后）  
**Then** `customerName` 为 `"Acme"`（或解析后的默认）  
**And** `flag` 为 `false`（类型占位）  
**And** payload **不含** 硬编码键仅因历史默认而出现的无关 Sample 行为  
**And** 折叠 JSON（若可见）与表单一致

#### S5 — 创建默认不再硬编码 Sample

**Given** 编排人员点击创建  
**When** 对话框初次打开（未点骨架前，若实现选择空对象路径）  
**Then** 变量模型 **不得** 固定为 `{"customerName":"Sample"}`  
**And**（若实现选择自动骨架）则骨架仅来自当前 schema（U03-C12）

#### S6 — 大 payload 折叠 JSON 编辑器

**Given** schema 可录入变量数 ≥ 12（或等价大 payload）  
**When** 打开创建/编辑对话框  
**Then** 存在可折叠的 Advanced/Raw JSON 编辑器  
**And** 默认展开策略符合 U03-C7  
**When** 用户在合法 JSON 中修改某键并失焦/应用  
**Then** 对应表单控件反映新值

#### S7 — 非法 JSON 阻断保存

**Given** 折叠 JSON 区内容为非法 JSON  
**When** 用户点击 Save  
**Then** 显示格式错误  
**And** 不发起 API 请求

#### S8 — 表单跳过 compute 字段

**Given** schema 含 `principal`（AMOUNT, required）与 `principalCn`（`COMPUTED` 或非空 `computeExpression`）  
**When** 打开创建对话框  
**Then** 仅渲染 `principal`（及非 compute 字段）  
**And** **不**渲染 `principalCn` 输入控件  
**And** 骨架不包含对 `principalCn` 的必填要求

#### S9 — 后端 fieldErrors 映射到 UI

**Given** 客户端因故提交了缺 required 的 payload（或测试桩返回 fieldErrors）  
**When** API 返回 `fieldErrors`（如 `field=customerName`, `reason=REQUIRED`）  
**Then** 对话框在对应字段或错误摘要中展示该错误  
**And** 不只显示无字段信息的泛化失败 toast

#### S10 — 只读角色不可维护

**Given** 会话为测试人员（无测试数据集维护权）  
**When** 打开模板 Testing 面板  
**Then** 不提供创建/编辑写路径（与既有矩阵一致；回归）

### Backend

#### S11 — 合法 variables 保存成功

**Given** 模板 schema 含 required TEXT `customerName`  
**And** 维护会话 create 请求 `variables: { "customerName": "Acme" }`  
**When** `TestDataSetService.create` 执行  
**Then** 持久化成功并返回 `TestDataSetView`  
**And** 无 `fieldErrors`

#### S12 — required 缺失 → fieldErrors REQUIRED

**Given** 同上 schema  
**When** create/update 提交 `variables: {}`  
**Then** 拒绝持久化  
**And** `error.category=VALIDATION`  
**And** `fieldErrors` 含 `field=customerName`, `reason=REQUIRED`

#### S13 — 类型不匹配 → INVALID_TYPE

**Given** schema 含 `amount`（NUMBER 或 AMOUNT）  
**When** 提交 `variables: { "amount": "not-a-number" }`（或无法强制转换的值）  
**Then** 拒绝持久化  
**And** `fieldErrors` 含该字段 `reason=INVALID_TYPE`（或 DATE 场景 `INVALID_FORMAT`）

#### S14 — ENUM 非法 → ENUM_NOT_ALLOWED

**Given** schema 含 `status` ENUM 允许 `ACTIVE,CLOSED`  
**When** 提交 `variables: { "status": "NOPE" }`  
**Then** `fieldErrors` 含 `reason=ENUM_NOT_ALLOWED`

#### S15 — 未知字段 → UNKNOWN_FIELD

**Given** schema 仅定义 `customerName`  
**When** 提交 `variables: { "customerName": "Acme", "extraHack": 1 }`  
**Then** 拒绝持久化  
**And** `fieldErrors` 含 `field=extraHack`, `reason=UNKNOWN_FIELD`

#### S16 — compute 字段缺失不失败

**Given** schema 含 required `principal` 与 compute 字段 `principalCn`  
**When** 提交仅 `{ "principal": 100 }`（无 `principalCn`）  
**Then** 校验通过并持久化  
**And** 不因缺失 `principalCn` 产生 `REQUIRED`

#### S17 — 无维护权限 fail-closed

**Given** 会话不可 `canAuthorTemplates`  
**When** 调用 create/update  
**Then** 既有访问拒绝（不写入）

#### S18 — 锁定集不可 update（既有回归）

**Given** 数据集已 `locked`  
**When** update  
**Then** 既有 `TestDataSetImmutableException` / 锁定错误  
**And** 本片 schema 校验不改变该优先级（先不可变或等价明确顺序，实现须可测）

---

## 11. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| Schema 为空 | 空态说明；允许保存 `{}`；骨架生成 `{}` |
| Schema 仅含 compute | 无输入控件；允许 `{}`；后端不因「无录入字段」失败 |
| 可选字段省略 | 允许；不写 key 或写 null 的处理：省略视为未提供（optional OK）；显式 null 对 required → REQUIRED，对 optional → 接受或视为省略（实现统一即可） |
| LIST 空数组 | 允许（除非未来另有 minItems——**本片无**） |
| 超大 JSON | 折叠编辑器可用；无本片新的服务端 size 上限（沿用既有请求限制若有） |
| 并发编辑 | 最后写获胜（既有）；本片不新增乐观锁 |

---

## 12. Observable evidence

| 层 | 证据 |
| --- | --- |
| UI | 对话框字段控件、骨架按钮、折叠 JSON、字段错误、成功 toast |
| 网络 | create/update 请求体为对象 `variables`；失败响应含 `fieldErrors` |
| 后端测试 | JUnit：S11–S18 |
| 前端测试 | Vitest：表单/骨架/校验/同步；E2E：S1/S4/S6 主路径 |
| 回归 | 无 `customerName: Sample` 硬编码字符串作为创建默认 |

---

## 13. Traceability

| 项 | 路径 / ID |
| --- | --- |
| Program task | CE-U03 · [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) |
| Slice | `ce-u03-testdata-schema-form` |
| BDD | `BDD-CE-U03-TESTDATA-SCHEMA-001` · 本文件 |
| Requirements | 测试数据集 + 变量 Schema 校验（requirements-plan / PRD） |
| Domain | §2.8 Template Variable |
| Permission | 维护模板测试数据集 |
| API error model | `FieldError` reason 枚举 |
| Weak dep | CE-K03（compute 求值 — 非本片） |
| Implementation touchpoints（参考，非验收） | `TemplateTestDataSetEditDialog.vue`、`createTemplateTestDataSetPanelActions.ts`、`TestDataSetService.java` |

---

## 14. Open questions（非阻塞 / residual）

以下 **不** 阻止 `bdd_readiness: ready`；实现期按决策默认执行，若产品需改再开补丁片：

| # | 问题 | 本片默认 |
| --- | --- | --- |
| Q1 | 创建打开时自动骨架 vs 空对象？ | **自动骨架**（schema 非空时），无覆盖确认 |
| Q2 | payload 显式含 compute key 时剥离还是保留原样？ | **剥离或不校验后原样保留**；推荐剥离以保持存储干净 |
| Q3 | OBJECT 无子 schema 时是否强制 JSON-only？ | **是**（字段级折叠 JSON） |
| Q4 | AMOUNT 存 number 还是 decimal string？ | **JSON number**（与 NUMBER 相同解析路径）；展示可格式化但不改存储类型 |

**无阻塞 pending questions。**

---

## 15. BDD readiness

```
bdd_readiness: ready
acceptance_scenario_count: 18
open_questions: [Q1, Q2, Q3, Q4]  # non-blocking defaults above
owning_doc: docs/behavior/ce-u03-testdata-schema-form.md
task_ids: [CE-U03, ce-u03-testdata-schema-form]
next: plan-orchestrator → backend-engineer + frontend-engineer（feature worktree）
```

**禁止本片宣称 go-live。** Formal phase 保持 **None**。
