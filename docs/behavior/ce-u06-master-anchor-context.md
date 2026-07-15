# BDD 行为规格：CE-U06 — 母版锚点可视上下文

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U06-MAC`  
**编写日期:** 2026-07-15  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U06  
**Slice:** `ce-u06-master-anchor-context`  
**Task Master:** **#88**  
**Formal phase:** **None**  
**完成声明约束:** 关闭「修订工作区锚点无位置上下文 + displayLabel 不可编辑」缺口；**不**做完整 DOCX WYSIWYG 渲染；**不**触碰 CE-K06 / CE-C04；**不**宣称 go-live

---

## 1. 概述

母版修订工作区 `design` Tab 今日仅展示扁平 `anchorId` + `displayLabel` 只读表；抽取时 `displayLabel` 默认等于 `anchorId`，设计师无法在平台内改成可读标签，也无法按文档顺序理解锚点在 DOCX 中的相对位置。完整在线 Word 渲染不在本片范围。

| 缺口（现状证据） | 目标 |
| --- | --- |
| 锚点表无文档顺序 / 位置上下文；无选中高亮 | **DOCX 概览**以「锚点位置高亮列表」呈现（按文档顺序；选中行高亮） |
| `displayLabel` 只读；抽取默认 = `anchorId` | 具备 `manageMasters` 且可写时，**可编辑并持久化** `displayLabel` |
| 无完整 DOCX 预览 | **明确不做**完整渲染 / 像素级阅读器（计划卡括号约束） |

| 行为域 | 摘要 |
| --- | --- |
| **MAC-01 位置高亮列表** | 修订工作区展示按文档顺序排列的锚点概览列表；选中/聚焦行可视高亮；展示稳定 `anchorId` 与位置序号（文档顺序） |
| **MAC-02 displayLabel 可编辑** | 可写会话下内联或等价控件编辑 `displayLabel`；保存后刷新仍保留；英文-first i18n |
| **MAC-03 只读 / fail-closed** | 历史修订线只读；无 `manageMasters` 或不可写状态不暴露写控件；`anchorId` 本片不可改 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 完整 DOCX / Word WYSIWYG 在线渲染、分页画布、像素对比 | **Out of scope** — 计划卡「不做完整渲染」 |
| CE-K06（若存在的知识/其他母版片） | **禁止触碰** |
| CE-C04 凭证 `expires_at` | **禁止触碰** |
| 改 `anchorId` / 锚点集合增删（须经 DOCX 替换抽取） | Out of scope — 稳定键仍由上传/替换产生 |
| 把纯 `displayLabel` 变更算作锚点集合 rename / 强制重测 | 与 CE-K05 一致：displayLabel-only **不**计入集合 delta |
| 宣称 go-live / 激活正式 P-phase / CD-3 | **禁止** |

---

## 2. Actor / Role

| Actor | 能力 | 说明 |
| --- | --- | --- |
| **MASTER_DESIGNER / 母版管理者** | `manageMasters` | 在可写 current 修订线编辑 `displayLabel`；查看位置高亮列表 |
| **母版读者 / 审批人** | 母版读权限（可无 `manageMasters`） | 可查看概览列表与标签；**不可**编辑 |
| **系统** | 修订线 detail + 锚点快照 +（本片）标签写路径 | 按文档顺序提供概览；fail-closed 授权；历史线只读 |

---

## 3. Goal

1. 设计师在 **母版修订工作区** 打开当前（或任意）修订线时，通过 **按文档位置排序并支持高亮的锚点列表** 理解 DOCX 锚点上下文——无需完整渲染器。  
2. 在可写条件下，设计师可将 `displayLabel` 改成可读英文（或既有 i18n 基线语言）标签并持久化，供后续模板绑定与审核阅读。  
3. 历史修订线与无写权限会话保持只读，不破坏修订线锚点快照不可变语义（历史）与 fail-closed 授权。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U06-C1** | 「DOCX 概览」= 修订工作区 **锚点位置高亮列表**；**禁止**本片交付完整 DOCX WYSIWYG / 画布渲染。 | 计划卡括号：「锚点位置高亮列表即可，不做完整渲染」 |
| **U06-C2** | **位置** = 母版 DOCX 抽取得到的 **文档顺序**（既有 `documentSequence` / 等价序）。列表必须按该顺序展示；UI 至少暴露可读的位置序号（1-based 或与 API 一致的序，实现固定一种并测）。本片**不要求**段落原文摘录或书签附近正文预览。 | 计划卡「位置」+ 现状持久化顺序模型；避免发明正文摘录产品 |
| **U06-C3** | **高亮** = 用户选中/聚焦某一锚点行时，该行（及可选的列表内关联指示）呈现明显选中态；无独立文档画布可滚。 | 计划卡「高亮列表」最小解读 |
| **U06-C4** | 表面落点：母版 **revision detail workspace** 的 **design**（或等价锚点目录）区域；复用既有 `WorkspaceTabShell` 修订工作区模式（Bank OA）。 | 计划卡「修订工作区」；现状 `MasterRevisionDetailWorkspace` |
| **U06-C5** | `displayLabel` **可编辑并持久化**；`anchorId` **只读**（本片不提供改 key）。 | 计划卡 + domain 稳定 `anchorId` |
| **U06-C6** | **写条件（可编辑）：** 会话持有 `manageMasters` **且** 目标修订线为 **current** **且** 包/线状态 **不是** `PENDING_REVIEW`。与 Hub `canWriteJourney` / replace 可写门控对齐（审批中不可改标签）。 | permission-matrix「维护锚点」+ Hub 可写模式；计划卡未写状态机时沿用既有母版写门控 |
| **U06-C7** | **历史修订线**（`current=false`）：锚点概览只读；不暴露编辑控件。符合 domain「历史线只读 / 锚点目录快照」。 | domain-model §修订线 |
| **U06-C8** | 持久化必须同时保持 **current 修订线快照** 与 **live `master_document` 锚点** 的 `displayLabel` 一致（current 同步约定）。纯标签变更 **不** 改变 `anchorId`、**不** 改变文档顺序、**不** 触发 CE-K05 集合 delta / `retestRequired`。 | domain current 同步；CE-K05 displayLabel-only 忽略 |
| **U06-C9** | `displayLabel` 保存前 trim；**不得为空**（空则校验失败，英文-first `messageKey`）；最大长度实现固定合理上限并测（建议与既有字符串列/校验一致）。OpenAPI `displayLabel` 已为必填字段语义。 | OpenAPI `MasterAnchorSummaryView` required；i18n-english-first |
| **U06-C10** | 无 `manageMasters`：列表可读（若有读权限），写控件不可见（fail-closed）；后端写路径仍授权校验。 | permission-matrix |
| **U06-C11** | 用户可见文案 English-first（en 基线 + zh-CN 翻译）；Bank OA 视觉/交互（无新营销风）。 | i18n + frontend-oa-design |
| **U06-C12** | **禁止：** CE-K06、CE-C04、完整渲染、go-live。 | 计划卡 / handoff |

### Pending questions

无阻塞项。下列实现细节由实现固定并测，**不**改变上述行为：

- 写 API 形态（例如 `PATCH …/revision-lines/{id}/anchors/{anchorId}` 或批量 PATCH）；须出现在 OpenAPI 并经契约测。  
- 列表是否在现有 `AppDataTable` 上增强，或替换为专用概览列表组件。  
- 位置序号 0-based API vs 1-based UI 展示映射。

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录；可访问目标母版修订线（组隔离 fail-closed）。  
- 目标修订线已有锚点快照（上传/替换抽取完成）；允许空列表时展示 empty 态。  
- 既有 revision detail 读 API 可用；本片可扩展锚点视图字段（如 `documentSequence`）与写 API。

**Triggers**

- 打开母版修订工作区 `design`（或等价锚点）Tab。  
- 选中列表中某一锚点行。  
- 在可写条件下编辑并保存某一行的 `displayLabel`。

---

## 6. Primary journey

1. 母版设计师打开 **current** 修订线工作区 → `design` Tab。  
2. 系统展示 **按文档顺序** 的锚点位置高亮列表（`anchorId`、位置序号、`displayLabel`）；**无**完整 DOCX 画布。  
3. 设计师点击某行 → 该行高亮，便于对照位置。  
4. 设计师将 `displayLabel` 从默认 `anchorId` 改为可读标签 → 保存。  
5. 刷新或重新进入该修订线 → 新标签仍在；`anchorId` 与顺序不变。  
6. 审批人/只读用户打开同一工作区 → 可见列表与标签，**无**编辑控件。

---

## 7. System responses（success）

| 表面 | 成功响应 |
| --- | --- |
| **位置高亮列表** | 按文档顺序渲染；含位置序号 + `anchorId` + `displayLabel`；选中行高亮 |
| **Empty** | 无锚点时 empty 文案（既有 `masters.revision.noAnchors` 或等价 English-first 键） |
| **displayLabel 保存** | 持久化成功；列表立即反映新值；再加载一致 |
| **只读** | 历史线 / 无写能力：无编辑控件 |
| **授权失败** | 写 API → `403` 统一信封；UI 不依赖「隐藏即安全」 |

---

## 8. Acceptance scenarios

### BDD-CE-U06-MAC-001 — 文档顺序位置列表（非完整渲染）

```gherkin
Given 用户可打开某母版修订线 detail（含至少 2 个锚点，文档顺序已知）
When 用户进入修订工作区 design（锚点）表面
Then 系统展示锚点位置列表，顺序与文档抽取顺序一致
And 每行至少展示位置序号、稳定 anchorId、displayLabel
And 页面不出现完整 DOCX/Word WYSIWYG 渲染画布
```

### BDD-CE-U06-MAC-002 — 选中行高亮

```gherkin
Given 修订工作区锚点位置列表已展示
When 用户选中其中一行
Then 该行呈现明显选中/高亮态
And 其他行不保持同级选中态（单选高亮）
```

### BDD-CE-U06-MAC-003 — 可写会话编辑 displayLabel

```gherkin
Given 会话持有 manageMasters
And 打开的是 current 修订线
And 包/线状态不是 PENDING_REVIEW
And 列表中存在 anchorId=A 的行
When 用户将 A 的 displayLabel 改为非空可读值 L 并保存
Then 系统持久化成功
And 列表立即显示 displayLabel=L
And anchorId 仍为 A
And 文档顺序不变
```

### BDD-CE-U06-MAC-004 — 刷新后标签仍保留

```gherkin
Given BDD-CE-U06-MAC-003 已成功将 A 的 displayLabel 存为 L
When 用户刷新或重新加载该修订线 detail
Then A 的 displayLabel 仍为 L
And live 母版锚点目录（若 API 暴露）与 current 快照一致为 L
```

### BDD-CE-U06-MAC-005 — 无 manageMasters fail-closed

```gherkin
Given 会话不具备 manageMasters 但可读取该修订线
When 用户打开修订工作区锚点列表
Then 可见位置列表与 displayLabel（只读）
And 不渲染 displayLabel 编辑控件
When 客户端若仍调用写 API
Then 后端返回 403 ACCESS_DENIED（或等价统一信封）
```

### BDD-CE-U06-MAC-006 — 历史修订线只读

```gherkin
Given 用户打开 current=false 的历史修订线
And 会话即使持有 manageMasters
When 查看锚点位置列表
Then 列表按该线快照只读展示
And 不暴露 displayLabel 编辑控件
```

### BDD-CE-U06-MAC-007 — PENDING_REVIEW 不可改标签

```gherkin
Given 会话持有 manageMasters
And current 修订线/包状态为 PENDING_REVIEW
When 用户查看锚点列表
Then 不暴露 displayLabel 编辑控件（与审批中不可写旅程对齐）
```

### BDD-CE-U06-MAC-008 — 空标签校验

```gherkin
Given 可写条件满足（同 MAC-003）
When 用户尝试将 displayLabel 保存为空白或仅空白字符
Then 保存被拒绝
And 展示英文-first 校验/错误文案（messageKey 可翻译）
And 原 displayLabel 保持不变
```

### BDD-CE-U06-MAC-009 — displayLabel-only 不影响锚点集合 delta

```gherkin
Given current 锚点集合与上一基线相比仅 displayLabel 不同（anchorId 集合与顺序相同）
When 计算母版锚点集合 delta / retestRequired（CE-K05 语义）
Then 不因纯 displayLabel 变更而产生 added/removed/renamed
And retestRequired 不因纯标签变更变为 true
```

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 空锚点列表 | empty 态；无崩溃；无假渲染画布 |
| 超长 displayLabel | 校验失败或截断策略实现固定并测；优先校验失败 + messageKey |
| 并发编辑 | 后写覆盖或乐观冲突——实现固定一种；失败时可读错误 |
| 组隔离 | 跨组修订线 → 403/404 既有语义；本片不放宽 |
| 网络/API 失败 | 保留编辑态或回滚到上次成功值；错误 English-first |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 组件 / store / API 单测 | 顺序、高亮、保存、只读门控、空标签 |
| 后端契约 / 授权测 | 写路径权限；持久化后 GET 一致；历史线拒绝写 |
| E2E（用户可见） | 修订工作区列表顺序 + 编辑 displayLabel + 刷新保留 |
| UIUX | Bank OA；双品牌 @1920 证据（交付阶段） |
| 非证据 | 完整 DOCX 画布截图（本片不应出现） |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-U06 plan §4 简卡 | 目标行为（位置高亮列表 + displayLabel 可编辑） |
| Task Master **#88** | 执行任务 |
| domain-model 修订线锚点快照 / 历史只读 | U06-C7 / C8 |
| permission-matrix「维护锚点」 | U06-C6 / C10 |
| CE-K05 displayLabel-only 忽略 | U06-C8 / MAC-009 |
| CE-U09 / CE-U10 | 上游 Done；复用修订工作区 / Hub 可写门控与 BDD 文档形态；本片不改其行为 |
| OpenAPI `displayLabel` required | U06-C9 |

---

## 12. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-u06-master-anchor-context.md
task_ids: ["#88", "CE-U06"]
scenario_ids:
  - BDD-CE-U06-MAC-001
  - BDD-CE-U06-MAC-002
  - BDD-CE-U06-MAC-003
  - BDD-CE-U06-MAC-004
  - BDD-CE-U06-MAC-005
  - BDD-CE-U06-MAC-006
  - BDD-CE-U06-MAC-007
  - BDD-CE-U06-MAC-008
  - BDD-CE-U06-MAC-009
next: plan-orchestrator
```
