# BDD 行为规格：CE-G05 — 模板年检 + 条款正文全文检索（where-used）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-G05` |
| **编写日期** | 2026-07-17 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §6 Wave CE-G · CE-G05 |
| **Slice** | `ce-g05-annual-review-fts` |
| **Worktree** | `D:/working/DGE-ce-g05-annual-review-fts` · `feat/ce-g05-annual-review-fts` |
| **Task Master** | **#77** · **In Progress** (sole-active) |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED |
| **Soft dep** | CE-G04 (#75) **Done**（软依赖已满足；本片不依赖 hold 语义） |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；契约/领域/权限已由 `doc-keeper` 同步（OpenAPI / contract-outline / domain-model / permission-matrix / requirements-plan）— handoff **backend-engineer** |

**完成声明约束：** 关闭内控缺口「年检/正文检索缺失」的最小闭环（模板 `nextReviewDue` + Dashboard 到期待办 + 条款 `content_structure_json` PostgreSQL tsvector 全文检索与 where-used）；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-O02；**leave #50 alone**；**不**做全库导入/eDiscovery/诉讼包。

---

## 1. 概述

| 缺口（现状证据） | 目标 |
| --- | --- |
| `template` 无年检到期字段；Dashboard 无年检待办 | 持久化 `nextReviewDue`；到期模板投影到 Dashboard Tasks；可完成年检并滚动下一到期日 |
| `GET /content-modules?search=` 仅 `name` ∪ `moduleCode` ∪ `groupCode` ILIKE，**不**扫 `content_structure_json` | PostgreSQL **tsvector** 对条款正文/结构可检索文本做全文检索 |
| 无「按正文命中 → 谁在用」闭环（模板侧有 references；模块侧 impact 偏生命周期） | FTS 命中后提供 **where-used**（引用该模块的模板清单，只读） |

| 行为域 | 摘要 |
| --- | --- |
| **G05-S1 年检字段** | 模板级 `nextReviewDue`（UTC 日期）；发布时默认播种；可查询/完成年检 |
| **G05-S2 到期待办** | 授权会话在 Dashboard Tasks 看到到期/已过期年检分区；深链模板治理面 |
| **G05-S3 条款 FTS** | 对 `content_module_version.content_structure_json` 维护 tsvector；目录/专用检索 query 可命中正文 |
| **G05-S4 where-used** | 对命中模块返回授权范围内引用模板列表（只读；复用 reference 关系） |
| **G05-S5 授权 fail-closed** | 无新 capability bit；复用既有模板/条款目录权限；跨组不泄露 |

### 1.1 FE vs API 表面（计划 + 治理模式锁定）

| 表面 | In scope? | 依据 |
| --- | --- | --- |
| **API — 年检** | **Yes（权威）** | 计划卡 `nextReviewDue` + 到期待办数据源；对齐 CE-U07 author-task API 投影模式 |
| **FE — Dashboard 年检待办** | **Yes** | 计划「到期待办」= 用户可发现队列；对齐 CE-U07 / CE-U08 / CE-U14 Tasks Tab（**非** G06/E01/E03 API-first） |
| **FE — 模板详情展示/完成年检** | **Yes（最小）** | 待办深链落地后须能查看 `nextReviewDue` 并完成年检（Overview/治理区控件即可；不新建独立「年检工作台」） |
| **API — 条款 FTS + where-used** | **Yes（权威）** | 计划 tsvector + where-used；目录现有 `search` 不含正文 |
| **FE — 条款目录正文检索 + where-used** | **Yes（最小）** | 银行 OA 发现面：Content Modules 目录可切换/使用正文检索；模块详情只读 where-used 区（对齐 U19 Dependencies 只读聚合） |
| **E2E + UIUX** | **Yes** | `frontend_ui_in_scope=true` → 管线 stages 5–7 义务 |
| **新独立治理路由（如 `/governance/annual-review`）** | **No** | 复用 Dashboard Tasks + 既有模板/条款页；对比 G04 才新开 GLOBAL_ADMIN 专页 |
| **新 capability / 新角色** | **No** | 对齐 G03/G06「无新 capability bit」 |

**对比邻近治理片：**

| 片 | 用户面策略 | 对本片含义 |
| --- | --- | --- |
| CE-G04 Legal hold | FE 专页（仅 GLOBAL_ADMIN） | 年检**不**需要专页；待办即可 |
| CE-G06 受控再生 | API-first；FE 再生按钮 OOS | 年检/FTS **不是**纯审计 API——计划明确「待办」 |
| CE-E01/E03 导出 | API-first；FE OOS | 不套用到 G05 |
| CE-U07/U08/U14 | Dashboard Tasks + 深链 | **年检待办主模式** |
| CE-U19 Dependencies | 模板 Hub 只读聚合 | **where-used 只读区**同构 |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 关注点 |
| --- | --- | --- |
| **模板作者 / 编排** | `authorTemplates`（`TEMPLATE_AUTHOR` / `MASTER_DESIGNER` / `GROUP_ADMIN` / `GLOBAL_ADMIN`） | 见组内到期年检待办；完成年检；查看 `nextReviewDue` |
| **组/全局管理员** | `GROUP_ADMIN` / `GLOBAL_ADMIN`（含 `publishTemplates`） | 同上；可对组内/全局可见模板完成年检 |
| **条款作者 / 可浏览条款者** | `authorContentModules` 或矩阵 §5.1 目录浏览角色（`GLOBAL_ADMIN` / `GROUP_ADMIN` / `MASTER_DESIGNER` / `TEMPLATE_AUTHOR` / `TEMPLATE_APPROVER`） | 正文 FTS；查看 where-used |
| **测试员** | `TEMPLATE_TESTER` | **无**条款目录浏览 → FTS/where-used **403/不可见**；**无**年检完成权（除非另具 `authorTemplates`，测试员默认无） |
| **系统** | Flyway tsvector + 年检字段；Tasks 投影；审计 | 索引维护；fail-closed；UTC |

---

## 3. Goal

1. 已发布（或已播种 `nextReviewDue` 的）模板在到期日到达后，授权用户在 Dashboard → **Tasks** 可发现年检待办，并一键到达可完成年检的模板面。  
2. 完成年检后滚动下一 `nextReviewDue`，该项从到期待办消失；写管理审计。  
3. 授权用户可按条款**正文/结构可检索文本**全文检索内容模块，并查看命中模块在授权范围内的**引用模板 where-used**。  
4. 无权会话看不到待办分区/检索结果细节（fail-closed）。  
5. 不宣称 go-live；不激活 CD-3 / O02；不动 #50。

---

## 4. 已确认决策 vs 非确认假设

### 4.1 已确认（计划 / 仓库事实 / 治理惯例）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G05-C1** | 切片关闭「年检/正文检索缺失」（R3 → CE-G05）。 | CE 计划 §6 / §12 |
| **G05-C2** | 年检载体字段名 **`nextReviewDue`**（API camelCase；DB `next_review_due` DATE/UTC 日历日）。挂在 **`template` 行**（非单 release 行）——年检是模板治理周期，不是单版本生命周期态。 | 计划字段名；领域惯例 |
| **G05-C3** | **到期待办**权威投影：专用 list API（建议 `GET /api/management/v1/author-workflow/annual-review-due-tasks` 或等价）+ `useWorkflowTasks` / `buildTaskPartitions` 新分区；**不**新建 collaboration `queue_type`（避免污染 P21 协作状态机）。对齐 CE-U07 outdated-clause tasks。 | U07 模式；计划「todos」 |
| **G05-C4** | **到期判定：** `nextReviewDue != null` **且** `nextReviewDue <= todayUtc`（UTC 日期，半开语义：到期日当天即入队）。 | 可测 |
| **G05-C5** | **默认播种：** 模板**首次**进入 `PUBLISHED` 且 `nextReviewDue` 为空 → 设为 `publishInstant` 的 UTC 日期 **+ 365 天**。后续再发布**不**自动改写已有 `nextReviewDue`（避免冲掉人工年检计划）。 | 最小治理默认 |
| **G05-C6** | **完成年检 API：** `POST /api/management/v1/templates/{templateId}/annual-review/complete`（名以实现为准）。Body 可选 `{ "nextReviewDue": "YYYY-MM-DD" }`；缺省 = 完成日 UTC 日期 **+ 365 天**。成功返回更新后的模板摘要（含新 `nextReviewDue`）。 | 计划闭环 |
| **G05-C7** | **授权（年检）：** 完成/列表待办要求对模板具备组范围访问 **且** `authorTemplates`（与 U07 作者工作流同级）。其他角色 → 待办不可见；完成 API **403**。无新 capability。 | matrix §13.2；G03 无新 bit |
| **G05-C8** | **审计：** 成功完成写 `TEMPLATE_ANNUAL_REVIEW_COMPLETED`（摘要：templateId/externalId、previousNextReviewDue、newNextReviewDue、actorUsername）。**禁止** variables / 凭证 / 条款全文。 | G04/G06 审计卫生 |
| **G05-C9** | **存量模板：** 迁移后 `nextReviewDue` 可为 NULL；**不**强制回填。仅当首次发布播种或管理员/作者显式完成/设置后进入年检队列。可选实现：`PATCH` 仅管理员设置日期（若做，须同权控）；**最小片允许**仅 complete 路径设置/滚动。 | 诚实迁移 |
| **G05-C10** | **FTS 对象：** `content_module_version.content_structure_json`。Flyway 增加 `tsvector` 列（或 GENERATED）+ GIN 索引；写入/更新版本时同步维护。配置使用 PostgreSQL **`simple`**（中英混合信函，避免 english stemmer 误伤）。 | 计划 tsvector；仓库列已存在 |
| **G05-C11** | **可检索文本：** 从 JSON 抽取人类可读文本节点（段落/列表文本、常见 string 叶子）；**不**要求完美 AST 覆盖所有节点类型，但须覆盖 `paragraph` / `text` / list item 文本；JSON 键名与纯结构标点可剥离。抽取失败 → 该版本向量为空（不 500）。 | 最小可用 |
| **G05-C12** | **检索 API：** 扩展 `GET /api/management/v1/content-modules`：可选 `searchMode=NAME`（默认，保持 LR-C5 ILIKE）\| `FULL_TEXT`（tsvector `@@` plainto_tsquery/websearch）。`FULL_TEXT` 时 `search` 对**目录过滤版本**的正文向量匹配（目录过滤版本规则对齐 CE-K08：优先最新 `APPROVED`+`ACTIVE`，否则最新版本）——与 CE-U20 `status` / legal filters **AND**。空 search 忽略。 | 计划 + C5 兼容 |
| **G05-C13** | **where-used API：** `GET /api/management/v1/content-modules/{moduleId}/where-used`（分页可选；默认合理 page size）。返回授权范围内引用该模块的模板摘要（id、externalId、name、groupCode、lifecycleStatus、pinned semanticVersion 若有）。复用 `template_content_module_reference`（及发布锁定关系）；**不**要求扫描全部 binding JSON。 | 计划 where-used |
| **G05-C14** | **FTS/where-used 授权：** 与条款目录 list/get 相同（矩阵 §5.1）；`TEMPLATE_TESTER` 无浏览权 → **403**。跨组模块：仅共享/授权可见。where-used **不得**返回调用方不可见模板行。 | fail-closed |
| **G05-C15** | **`contentStructureJson` 响应策略不变：** FTS 命中列表仍按既有结构字段可见性（§5.1）；where-used 响应**不含**条款全文。 | contract-outline |
| **G05-C16** | **`frontend_ui_in_scope=true`；E2E+UIUX 必修。** | §1.1 |
| **G05-C17** | **明确非目标：** CD-3；CE-O02；go-live；#50；全库导入；eDiscovery；改条款审批状态机；改 CE-K08 法务门禁；改 G04 hold；协作队列新 `queue_type`；独立年检治理路由。 | handoff OOS |

### 4.2 本片确认的实现决策（计划卡薄 → 银行 OA 一致推导）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **G05-D1** | Dashboard 分区 kind 建议：`template-annual-review`（实现名可微调，须稳定 E2E `data-*`）。标题 English-first：`Annual review due`。 | U07/U08 分区模式 |
| **G05-D2** | 待办深链：`/templates/{id}` Overview（或 Hub overview）且可见年检控件；有 `devVersionId` 时仍以**治理信息可见**为验收，不强制进 editor。 | 最小可达 |
| **G05-D3** | FE 条款目录：search 旁提供正文检索模式（toggle 或明确 hint）；结果行可进详情；详情 **Where used** 只读表/列表（EntityLink 到模板）。 | U19 只读；OA |
| **G05-D4** | FTS 排序：默认按 `ts_rank` DESC，其次 `updatedAt` DESC（实现锁定并测）。 | 可用性 |
| **G05-D5** | 完成年检二次确认（ElMessageBox 或既有 confirm composable）；English-first i18n。 | OA destructive-ish |

### 4.3 非确认假设（不得升格为需求）

| 项 | 状态 |
| --- | --- |
| 年检是否阻断发布/runtime 生成 | **不阻断**（本片仅待办+字段；非门禁） |
| 是否邮件/IM 通知到期 | Out of scope（LR-C7 不扩展） |
| 是否对母版/条款实体做并行年检字段 | Out of scope（仅模板） |
| 中文分词插件（zhparser 等） | Out of scope（`simple` 即可） |
| 高亮 snippet 返回 | 可选增强；**不**作为 Done 门槛 |

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录管理会话。  
- 年检：目标组存在 `nextReviewDue <= todayUtc` 的模板，或可完成一次发布以播种。  
- FTS：存在含可检索正文的内容模块版本；且有模板引用该模块（where-used 非空场景）。  
- CE-G04 已合并（软依赖；行为正交）。

**Triggers**

- 模板首次 PUBLISHED（播种 `nextReviewDue`）。  
- 日历到达/越过 `nextReviewDue`。  
- 用户打开 Dashboard → Tasks；点击年检待办；完成年检。  
- 用户在 Content Modules 使用 `FULL_TEXT` 检索；打开模块 where-used。

---

## 6. Primary journey

### 6.1 模板年检

1. 作者发布模板 → 系统播种 `nextReviewDue = publishDateUtc + 365d`。  
2. 到期日到达后，同组 `authorTemplates` 用户打开 Dashboard → Tasks，看到 **Annual review due** 分区与该模板待办。  
3. 用户打开待办 → 落地模板 Overview，见到期日与 **Complete annual review**。  
4. 用户确认完成 → API 滚动下一到期日 → 审计事件 → 待办消失。

### 6.2 条款正文 FTS + where-used

1. 条款作者在 Content Modules 选择正文检索，输入特有短语。  
2. 系统返回正文命中的模块（组范围）。  
3. 用户打开模块 → Where used 列出引用模板（可点击进入模板）。  
4. 无权限用户无法检索或看不到不可见模板。

---

## 7. System responses

| 路径 | 响应 |
| --- | --- |
| 完成年检成功 | 200 envelope；`nextReviewDue` 已更新；审计已写；待办列表不再含该模板（除非新日期仍 ≤ today——仅当调用方把 next 设到过去/今天） |
| 完成年检无权限 | 403 |
| 模板不存在/不可见 | 404（或不泄露的 403；与既有模板 API 惯例一致） |
| FTS 默认 NAME 模式 | 行为与 LR-C5 完全一致（回归） |
| FTS FULL_TEXT 命中 | PageView；仅授权模块 |
| FTS 无命中 | 200 空页 |
| where-used 无引用 | 200 空列表 |
| where-used 无模块权 | 403/404 惯例 |

---

## 8. Acceptance scenarios（Given / When / Then）

### BDD-CE-G05-001 — 首次发布播种 nextReviewDue

```
Given 一模板首次从非 PUBLISHED 进入 PUBLISHED 且 nextReviewDue 为空
When 发布成功
Then 模板 nextReviewDue = 发布日 UTC 日期 + 365 天
And TemplateSummary/Detail 可读取该字段
```

### BDD-CE-G05-002 — 再发布不覆盖已有 nextReviewDue

```
Given 模板已 PUBLISHED 且 nextReviewDue = D
When 再次走发布产生新 release
Then nextReviewDue 仍为 D
```

### BDD-CE-G05-003 — 到期日当天进入待办

```
Given 授权作者会话，模板 nextReviewDue = todayUtc
When GET 年检待办 API / 打开 Dashboard Tasks
Then 该模板出现在 Annual review due 分区
```

### BDD-CE-G05-004 — 未来日期不入队

```
Given 模板 nextReviewDue = tomorrowUtc
When 拉取年检待办
Then 列表不含该模板
```

### BDD-CE-G05-005 — null 不入队

```
Given 存量模板 nextReviewDue IS NULL
When 拉取年检待办
Then 列表不含该模板
```

### BDD-CE-G05-006 — 完成年检默认 +365

```
Given 到期模板与授权作者
When POST …/annual-review/complete 且 body 无 nextReviewDue
Then nextReviewDue = 完成日 UTC 日期 + 365 天
And 写 TEMPLATE_ANNUAL_REVIEW_COMPLETED 审计（无敏感正文）
And 待办不再包含该模板
```

### BDD-CE-G05-007 — 完成年检显式下一日期

```
Given 到期模板与授权作者
When POST complete 且 nextReviewDue = 合法未来日 F
Then 模板 nextReviewDue = F
```

### BDD-CE-G05-008 — 完成年检校验

```
Given 授权作者
When POST complete 且 nextReviewDue 非法（格式错误或非日期）
Then 422 VALIDATION + 稳定 messageKey
And 不写成功审计
```

### BDD-CE-G05-009 — 年检授权 fail-closed

```
Given 仅 TEMPLATE_TESTER（无 authorTemplates）会话
When GET 年检待办或 POST complete
Then 待办不可见 / API 403
```

### BDD-CE-G05-010 — Dashboard 深链可达

```
Given Tasks 中有年检待办
When 用户点击 Open
Then 到达模板面且可见 nextReviewDue 与完成年检动作（授权会话）
```

### BDD-CE-G05-011 — NAME 检索回归

```
Given 模块 name 含 "Alpha" 但正文不含
When GET content-modules?search=Alpha&searchMode=NAME（或默认）
Then 仍按 LR-C5 ILIKE 命中 name/moduleCode/groupCode
And 不依赖 tsvector
```

### BDD-CE-G05-012 — FULL_TEXT 命中正文

```
Given 模块目录过滤版本 content_structure_json 含独特短语 "force majeure carve-out-xyz"
And name/moduleCode 均不含该短语
When GET content-modules?search=force majeure carve-out-xyz&searchMode=FULL_TEXT
Then 该模块出现在 PageView 结果中
```

### BDD-CE-G05-013 — FULL_TEXT 组范围

```
Given 短语仅存在于调用方不可见组的模块正文
When 同 FULL_TEXT 查询
Then 结果不含该模块（空页或其它可见命中，但不泄露不可见组）
```

### BDD-CE-G05-014 — where-used 列表

```
Given 模块 M 被模板 T1（可见）引用，被模板 T2（不可见组）引用
When GET …/content-modules/{M}/where-used
Then 结果含 T1
And 不含 T2
```

### BDD-CE-G05-015 — where-used 空

```
Given 模块无任何引用
When GET where-used
Then 200 空列表
```

### BDD-CE-G05-016 — FTS 索引随版本更新

```
Given 模块新版本更新正文加入短语 P（或从正文删除 P）
When 保存版本成功后执行 FULL_TEXT 搜索 P
Then 命中结果反映最新目录过滤版本正文（含则中，不含则不中）
```

### BDD-CE-G05-017 — FE 条款正文检索 + where-used（E2E）

```
Given Docker 验收栈与授权条款作者
When 在 Content Modules 使用正文检索命中短语并打开 where-used
Then UI 展示命中模块与引用模板链接（English-first）
And UIUX Critical=0（双品牌抽检按项目惯例）
```

### BDD-CE-G05-018 — FE 年检待办闭环（E2E）

```
Given 到期模板与授权作者
When Dashboard Tasks 打开年检待办并完成年检
Then 待办消失且模板 nextReviewDue 已滚动
```

### BDD-CE-G05-019 — 非目标护栏

```
Given 本切片 Done 声明
Then 未激活 CD-3 / CE-O02 / go-live
And 未修改 #50 Vitest 专项范围
And 未新增 collaboration queue_type
```

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 逻辑删除模板 | 不出现在待办；complete → 404 |
| STOPPED/DEPRECATED 模板仍到期 | **仍入队**（治理提醒；不因停用而静默消失） |
| complete 把 nextReviewDue 设为 ≤ today | 允许但待办可立即再次出现（诚实） |
| 超长 search 字符串 | 服务端上限（建议 ≤ 200）；超限 422 |
| tsvector 维护失败 | 写版本 fail-closed 或记 warning + 空向量——实现锁定：**写版本成功则向量最终一致**（同事务） |
| 并发 complete | 最后写赢；均写审计 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | Template 含 `nextReviewDue`；complete + due-tasks；`searchMode=FULL_TEXT`；where-used |
| DB | Flyway：`template.next_review_due`；`content_module_version` tsvector + GIN |
| UI | Dashboard 年检分区；模板完成控件；CM 正文检索 + where-used |
| Audit | `TEMPLATE_ANNUAL_REVIEW_COMPLETED` |
| Gates | `mvn verify`；FE lint/type-check/test/build；E2E + UIUX；queued deploy |
| 非证据 | 不宣称 go-live / CD-3 / program Done |

---

## 11. Traceability

| 工件 | 关系 |
| --- | --- |
| Task Master **#77** | 本叶子 |
| CE 计划 §6 CE-G05 | 计划卡 |
| Soft dep **#75** CE-G04 | Done；正交 |
| CE-U07 / U08 / U14 | 待办投影模式 |
| CE-U19 | where-used 只读聚合模式 |
| LR-C5 / CE-U20 / CE-K08 | 目录检索/筛选 AND 兼容 |
| permission-matrix §5.1 / §13.2 | 无新 bit；复用浏览/authorTemplates |
| OOS | CD-3、CE-O02、go-live、#50、E03（已 Done 勿复开） |

---

## 12. BDD readiness

```yaml
bdd_readiness: ready
frontend_ui_in_scope: true
e2e_uiux_needed: true
open_questions: []
owning_doc: docs/behavior/ce-g05-annual-review-fts.md
task_ids: ["77"]
ce_id: CE-G05
slice_id: ce-g05-annual-review-fts
out_of_scope:
  - CD-3
  - CE-O02
  - go-live
  - "#50"
  - CE-E03
  - new-collaboration-queue-type
  - standalone-annual-review-route
handoff: backend-engineer
doc_keeper_completed: 2026-07-17
doc_keeper_artifacts:
  - docs/security/permission-matrix.md  # §5 年检行；§5.1 FTS/where-used；§13.2 CE-G05；无新 capability
  - docs/domain/domain-model.md         # §2.7 nextReviewDue；§2.9.2 tsvector/where-used；审计事件
  - docs/api/contract-outline.md        # «模板年检与条款正文全文检索（CE-G05）» + content-modules 字段
  - docs/api/openapi-v1.yaml            # nextReviewDue；complete；due-tasks；searchMode；where-used
  - docs/api/README.md                  # CE-G05 索引节
  - docs/requirements/requirements-plan.md  # 条款 FTS + 模板年检 + 审计确认条
```

**Acceptance IDs for TDD Red：** `BDD-CE-G05-001` … `BDD-CE-G05-019`。
