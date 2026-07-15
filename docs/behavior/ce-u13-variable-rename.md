# BDD 行为规格：CE-U13 — 变量重命名联动 + 表达式补全

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U13-VRC`  
**编写日期:** 2026-07-15  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U13  
**Slice:** `ce-u13-variable-rename`（Task Master alt: `ce-u13-variable-rename-autocomplete`）  
**Task Master:** **#89**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u13-variable-rename` · `feat/ce-u13-variable-rename`  
**完成声明约束:** 关闭「变量 key 不可改 / 改名后引用断裂」与「conditionExpression 无变量补全」缺口；**优先前端**；后端仅在原子性/fail-closed 需要时新增 API；**不**触碰 CE-K06c；**不**宣称 go-live

---

## 1. 概述

今日模板变量编辑对话框在编辑态将 `variableKey` **禁用**（`TemplateVariableTreePanel`：`:disabled="Boolean(editingVariableKey)"`）。作者无法在平台内安全重命名变量；若通过 API 删除旧 key 再 upsert 新 key，**bindings structured content、composition rules、测试集 variables JSON**（以及其它变量的 `computeExpression`）仍保留旧引用，导致校验失败或运行时/预览断裂。`conditionBlock.conditionExpression` 与锚点 visibility 表达式均为裸 `el-input`，无 schema 变量补全。

| 缺口（现状证据） | 目标 |
| --- | --- |
| Edit 对话框 `variableKey` 只读 | 可写会话下支持 **重命名**（改 `variableKey`） |
| Rename 不传播到引用面 | Rename **级联**更新 bindings / 规则 / 未锁定测试集 JSON（+ schema 内 compute 引用） |
| conditionExpression 无补全 | 编辑 `conditionExpression`（含 visibility）时提供 **变量补全** |

| 行为域 | 摘要 |
| --- | --- |
| **VRC-01 变量重命名** | 编辑对话框可改 `variableKey`；校验新 key；确认影响后执行 |
| **VRC-02 引用级联** | 持久化更新：bindings JSON、rules JSON、未锁定 test-set variables 键、其它变量 `computeExpression` 中的旧引用 |
| **VRC-03 表达式补全** | conditionBlock / 锚点 visibility `conditionExpression` 输入提供当前 schema 变量建议并插入合法引用形态 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-K06c / 渲染保真残余 | **禁止触碰** |
| 已发布 release 线 / 历史不可写版本上的 rename | Out of scope — 仅 in-flight 可写创作版本 |
| 锁定测试集内容改写 | **禁止** — 证据不可变；跳过并汇总告知 |
| 通用 IDE 级语言服务器 / 任意脚本补全 | Out of scope — 仅变量 key 建议 |
| 改变 permission-matrix / 变量类型语义 / K03 DSL 白名单 | **禁止** |
| 宣称 go-live / 激活正式 P-phase / CD-3 | **禁止** |

---

## 2. Actor / Role

| Actor | 能力 | 说明 |
| --- | --- | --- |
| **模板编排人员 / 母版设计** | `authorTemplates`（及既有配置锚点内容能力） | 在可写 in-flight 版本重命名变量；使用表达式补全 |
| **分组 / 全局管理员** | 同维护能力（组范围 fail-closed） | 同上 |
| **测试 / 审批 / 只读** | 无写权限 | 可见变量与表达式；**不可** rename；不暴露写控件 |
| **系统** | 变量面板 + 绑定编辑器 +（可选）级联写路径 | 校验、确认、级联持久化、补全建议；fail-closed 授权 |

---

## 3. Goal

1. 作者可将 in-flight 模板变量的 `variableKey` 从 `oldKey` 改为 `newKey`，并在一次确认的操作中把引用面一并更新，避免手改 JSON。  
2. 级联后：bindings structured content、composition rules、**未锁定**测试集 variables、以及其它变量 `computeExpression` 中指向 `oldKey` 的引用均变为 `newKey`；旧 schema 行不再存在。  
3. 作者在编辑 **conditionExpression**（conditionBlock 与锚点 visibility）时，可从当前 VariableSchema 获得变量补全并插入合法引用。  
4. 无写权限 / 不可写版本保持只读；跨组 fail-closed。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U13-C1** | **主表面（rename）：** 模板创作 Variables 面板 `TemplateVariableTreePanel` 编辑对话框：编辑态 **允许** 修改 `variableKey`（取消今日 disabled）。 | 计划卡；现状代码 |
| **U13-C2** | **触发：** 保存时若 `newKey ≠ oldKey`（trim 后）→ 进入 **rename 路径**（非普通 upsert）。同 key 仅更新元数据时不触发级联。 | 计划卡「rename」 |
| **U13-C3** | **新 key 校验（客户端 + 服务端纵深）：** trim；非空；符合既有 `variableKey` 形态约定；**不得**与同版本其它已存在 key 冲突（冲突 → 拒绝，English-first `messageKey`）。 | 计划卡；OpenAPI schema |
| **U13-C4** | **影响预览 + 确认：** Rename 前展示可观察影响摘要（至少：受影响 binding 锚点数、rule 条数、将更新的未锁定测试集数、跳过的锁定测试集数、其它 compute 引用数）。用户确认后才执行；取消则不改任何持久化状态。 | UX fail-safe；Bank OA |
| **U13-C5** | **级联目标（必须持久化）：** (1) **bindings** structured content JSON 内：`${oldKey}`、精确字段 `variableKey`/`loopVariable` 等于 `oldKey` 的节点属性；(2) **composition rules** 的 `conditionExpression`；(3) **未锁定** test-set `variables` 对象键 `oldKey`→`newKey`（值不变）；(4) **其它变量** 的 `computeExpression` 中 `${oldKey}`（及与规则一致的整词引用）。 | 计划卡「bindings/规则/测试集 JSON」+ K03 引用完整性 |
| **U13-C6** | **替换语义：** 仅 **整 token / 精确字段**；禁止子串误伤（例：`customer` 不得改写 `customerName`）。`${oldKey}` → `${newKey}`；字段等值替换；表达式内裸标识整词边界替换（visibility / 规则方言若存在）。 | 计划卡；F3 `${var}` 约定 |
| **U13-C7** | **锁定测试集：** 不修改 locked 集；摘要中报告跳过数量。若仅锁定集仍含 `oldKey`，rename **仍可完成**（证据冻结）；UI 用非阻断 warning 说明证据集仍为旧键。 | CE-U03 不可变证据 |
| **U13-C8** | **实现偏好：** **优先前端**编排既有 upsert/delete/bindings/rules/test-set API 完成级联；若多步无法保证原子性或易半成功，允许新增 **后端 rename API**（单事务）——须 OpenAPI + 契约测。半成功时须可观察回滚或明确错误且不静默丢引用。 | handoff「Prefer frontend; backend only if API needed」 |
| **U13-C9** | **写门控：** 仅 in-flight 可写版本 + `authorTemplates`（既有模板写门控）。历史/已发布线不暴露 rename。后端写路径仍授权校验（fail-closed）。 | permission-matrix |
| **U13-C10** | **补全面：** (a) structured `conditionBlock.conditionExpression`；(b) 锚点 **visibility** `conditionExpression` 输入。建议列表 = 当前模板 `VariableSchema[].variableKey`（可含正在编辑的 newKey）。 | 计划卡「conditionExpression 变量补全」 |
| **U13-C11** | **补全交互（最小）：** 用户在上述输入聚焦时，通过明确触发（键入 `${` **或** 控件旁「Insert variable」/等价动作——实现固定一种并测）打开建议列表；选择后插入 **`${variableKey}`**（structured condition 强制该形态）。Visibility 输入：若现有方言接受裸标识，插入形态与现有求值器一致，但建议列表仍展示 schema keys。过滤：按已键前缀过滤。 | 计划卡；F3-C2 |
| **U13-C12** | **i18n / OA：** 所有用户可见文案 English-first（en + zh-CN）；Bank OA 视觉；无新营销风。 | i18n + frontend-oa-design |
| **U13-C13** | **禁止：** CE-K06c、改权限矩阵、go-live、改锁定测试集内容。 | handoff / 计划卡 |

### Pending questions

无阻塞项。下列实现细节由实现固定并测，**不**改变上述行为：

- 是否新增 `POST/PATCH …/variables/{oldKey}/rename` 原子 API，或纯 FE 多步编排。  
- 补全 UI 组件形态（dropdown / popover / Element autocomplete）。  
- 影响摘要是否同时列出样例锚点/ruleId（可选增强）。

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录；对目标模板组内有读权限；rename 需写权限。  
- 目标为 **in-flight 可写** 模板版本（既有 authoring 门控）。  
- 模板已有 ≥1 个变量；引用面可为空（空级联仍允许 rename）。  

**Triggers**

- 在变量编辑对话框将 `variableKey` 改为新值并 Save → 确认级联。  
- 在 conditionBlock 或 visibility 表达式输入中触发变量补全。

---

## 6. Primary journey

1. 模板作者打开 in-flight 版本 Variables 面板 → Edit 某变量 `customerName`。  
2. 将 key 改为 `borrowerLegalName` → Save。  
3. 系统展示影响摘要（如：2 bindings、1 rule、3 unlocked datasets、1 locked skipped、1 compute ref）→ 确认。  
4. 系统持久化：schema 仅保留新 key；bindings/rules/unlocked datasets/compute 引用已更新。  
5. 作者打开某 binding 的 conditionBlock 表达式 → 触发补全 → 选择 `borrowerLegalName` → 插入 `${borrowerLegalName}`。  
6. 刷新后变量树与引用面一致；无旧 key 残留于可写引用面。

---

## 7. System responses（success）

| 表面 | 成功响应 |
| --- | --- |
| **Rename 确认** | 影响摘要可见；确认后成功 toast（English-first） |
| **Schema** | 仅 `newKey`；`oldKey` 不存在 |
| **Bindings / rules** | 表达式与字段引用为 `newKey`；再加载一致 |
| **Unlocked test sets** | variables 键已改；值保留 |
| **Locked test sets** | 未改；摘要/warning 说明跳过 |
| **Autocomplete** | 建议列表来自 schema；插入合法引用 |
| **只读 / 无权限** | 无 rename 控件；写 API → 403 统一信封 |

---

## 8. Acceptance scenarios

### BDD-CE-U13-VRC-001 — 编辑态可改 variableKey

```gherkin
Given 会话具备模板写权限且打开 in-flight 版本 Variables 面板
And 存在变量 variableKey=oldKey
When 用户打开该变量的 Edit 对话框
Then variableKey 输入可编辑（非 disabled）
```

### BDD-CE-U13-VRC-002 — Rename 级联 bindings / rules / unlocked test sets

```gherkin
Given in-flight 版本存在变量 oldKey
And 至少一处 binding structured content 含 ${oldKey} 或 variableKey/loopVariable=oldKey
And 至少一条 composition rule 的 conditionExpression 引用 oldKey
And 至少一个未锁定测试集 variables 含键 oldKey
When 用户将变量重命名为 newKey 并确认级联
Then schema 仅含 newKey 不含 oldKey
And 上述 binding 引用均为 newKey
And 上述 rule 引用均为 newKey
And 该未锁定测试集键为 newKey 且值不变
```

### BDD-CE-U13-VRC-003 — computeExpression 引用同步

```gherkin
Given 另一变量 C 的 computeExpression 含 ${oldKey}
When 用户将 oldKey 重命名为 newKey 并确认
Then C 的 computeExpression 含 ${newKey} 且不再含 ${oldKey}
```

### BDD-CE-U13-VRC-004 — 锁定测试集跳过

```gherkin
Given 存在锁定测试集 L 的 variables 含键 oldKey
And 存在未锁定测试集 U 含键 oldKey
When 用户将 oldKey 重命名为 newKey 并确认
Then U 的键更新为 newKey
And L 的内容不变（仍为 oldKey）
And 用户可见跳过锁定集的摘要或 warning（English-first）
```

### BDD-CE-U13-VRC-005 — 冲突 key 拒绝

```gherkin
Given 已存在变量 otherKey
When 用户试图将 oldKey 重命名为 otherKey
Then 保存/级联被拒绝
And 展示英文-first 冲突校验文案
And schema 与引用面均不变化
```

### BDD-CE-U13-VRC-006 — 空/非法 newKey 拒绝

```gherkin
Given 可写会话编辑变量 oldKey
When 用户将 variableKey 保存为空白或仅空白字符（或违反既有 key 形态）
Then 保存被拒绝
And 原 key 与引用面不变
```

### BDD-CE-U13-VRC-007 — 取消确认不改持久化

```gherkin
Given rename 影响摘要已展示
When 用户取消确认
Then 不发生 schema/bindings/rules/test-set 持久化变更
```

### BDD-CE-U13-VRC-008 — 整 token 替换不误伤子串

```gherkin
Given 表达式或字段同时存在 customer 与 customerName
When 将变量 customer 重命名为 party
Then customerName 引用保持不变
And 仅整 token customer / ${customer} 变为 party / ${party}
```

### BDD-CE-U13-VRC-009 — 无写权限 fail-closed

```gherkin
Given 会话可读取模板但无 authorTemplates 写能力
When 用户打开变量面板
Then 不暴露可完成 rename 的写控件（或 Save 不可达）
When 客户端仍调用 rename/写 API
Then 后端返回 403 ACCESS_DENIED（或等价统一信封）
```

### BDD-CE-U13-VRC-010 — conditionBlock 变量补全

```gherkin
Given 模板 VariableSchema 含 borrowerLegalName
And 用户在 structured conditionBlock 的 conditionExpression 输入中
When 用户触发变量补全（键入 ${ 或等价 Insert variable）
Then 建议列表包含 borrowerLegalName
When 用户选择该建议
Then 输入中插入 ${borrowerLegalName}
```

### BDD-CE-U13-VRC-011 — visibility 表达式变量补全

```gherkin
Given 锚点 visibility 条件已启用
And schema 含 showNotice
When 用户在 visibility expression 输入触发补全
Then 建议列表包含 showNotice
And 选择后按 U13-C11 插入与现有求值方言一致的引用
```

### BDD-CE-U13-VRC-012 — 无引用时仍可 rename

```gherkin
Given 变量 lonelyKey 未被 bindings/rules/test sets/compute 引用
When 用户将其重命名为 soloKey 并确认（摘要可均为 0）
Then schema 仅含 soloKey
And 操作成功完成
```

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 新 key = 旧 key（仅改描述等） | 普通 upsert；不弹级联确认 |
| 部分级联 API 失败 | 不静默成功；可读错误；尽量不留下「仅 schema 改了、引用仍旧」的半成功（原子 API 或补偿） |
| 跨组模板 | 403/404 既有语义 |
| 已发布 / 不可写版本 | 无 rename |
| 补全时 schema 为空 | 空建议；不崩溃 |
| 网络失败 | 保留对话框状态或回到上次成功快照；English-first 错误 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| Vitest / 单元 | 级联替换整 token；冲突/空 key；补全插入 |
| 可选后端契约测 | 若新增 rename API：授权、原子级联、冲突 |
| E2E | Variables rename → 确认 → binding/rule/test-set 可见更新；conditionExpression 补全 |
| UIUX | Bank OA；双品牌截图（交付阶段） |
| 非证据 | K06c 变更；锁定测试集被改写 |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-U13 plan §4 简卡 | 目标行为（rename 传播 + conditionExpression 补全） |
| Task Master **#89** | 执行任务 |
| CE-U06 (#88) | 上游依赖（泳道顺序）；本片不改母版锚点行为 |
| CE-U03 | 锁定测试集不可变 → U13-C7 |
| F3 / CE-K03 | `${var}` 与 compute 引用形态 → U13-C6 / C5 / C11 |
| permission-matrix `authorTemplates` | U13-C9 / VRC-009 |
| handoff | Prefer FE；不触碰 K06c；formal phase None；not go-live |

---

## 12. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-u13-variable-rename.md
task_ids: ["#89", "CE-U13"]
scenario_ids:
  - BDD-CE-U13-VRC-001
  - BDD-CE-U13-VRC-002
  - BDD-CE-U13-VRC-003
  - BDD-CE-U13-VRC-004
  - BDD-CE-U13-VRC-005
  - BDD-CE-U13-VRC-006
  - BDD-CE-U13-VRC-007
  - BDD-CE-U13-VRC-008
  - BDD-CE-U13-VRC-009
  - BDD-CE-U13-VRC-010
  - BDD-CE-U13-VRC-011
  - BDD-CE-U13-VRC-012
next: frontend-engineer
```
