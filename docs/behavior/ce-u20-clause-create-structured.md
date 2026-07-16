# BDD 行为规格：CE-U20 — 条款创建结构化化 + 列表状态列

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U20-CCS`  
**编写日期:** 2026-07-17  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U20  
**Slice:** `ce-u20-clause-create-structured`  
**Task Master:** **#94**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u20-clause-create-structured` · `feat/ce-u20-clause-create-structured`  
**batch_recommendation:** **solo** `#94` `ce-u20-clause-create-structured`  
**完成声明约束:** 关闭「创建对话框手写 JSON」与「目录无状态列/筛选」缺口；复用 CE-U01 / 版本对话框结构化编辑器；**不**宣称 go-live；**不**激活 CD-3 / P3；**不**触碰 #50

---

## 1. 概述

内容模块（条款）**版本**创建/编辑已使用 `ControlledStructuredContentEditor`（含 CE-U01 嵌套子树），但 **新建模块** 对话框仍要求作者手写 `contentStructureJson` textarea（默认 `{"blocks": []}` 旧形态）。条款目录列表仅有 group / code / name / updatedAt，**无** review/lifecycle 状态列与筛选；`ContentModuleSummaryView` 亦未暴露状态字段（LR-C5 已声明 content-modules 状态 filter 为可选，本片落地）。

| 缺口（现状证据） | 目标 |
| --- | --- |
| `ContentModuleCreateDialog`：`el-input type="textarea"` 绑定 `contentStructureJson` | 弃用结构 JSON textarea；嵌入与版本对话框同款结构化编辑器 |
| 默认 `{\n  "blocks": []\n}` | 默认 `DEFAULT_STRUCTURED_CONTENT_JSON`（`schemaVersion` + `nodes`），与版本对话框一致 |
| `ContentModuleListView` 无 status 列；toolbar 仅 `groupCode` filter | 增加状态列（复用 `ContentModuleStatusBadge`）+ 状态筛选 |
| `ContentModuleSummaryView` 无 `reviewState` / `lifecycleState`；list query 无 status | 摘要 enrich + 服务端 `status` filter（LR-C5 server paging 下禁止纯客户端切片） |

| 行为域 | 摘要 |
| --- | --- |
| **CCS-01 结构化创建** | 创建对话框用结构化编辑器编辑初始内容；提交仍走既有 `POST .../content-modules` |
| **CCS-02 目录状态列** | 列表展示 head 版本状态徽章（与详情版本表一致语义） |
| **CCS-03 状态筛选** | 工具栏状态 filter → 服务端精确过滤；与 search/group/sort AND；改 filter 重置 page 0 |
| **CCS-04 Fail-closed** | 无 `authorContentModules` 不可创建；目录浏览/结构查看沿用既有授权 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-U21 草稿 per-anchor / 并发乐观锁提示 | Out of scope（#95） |
| CE-U17 编辑器快捷键 / CE-U19 依赖只读 | P3 parked |
| 创建对话框采集 CE-K08 legal metadata | Out of scope — create 契约无这些字段；版本对话框后续可对齐 |
| 改变 review/lifecycle 状态机或审核 API | **禁止** — 仅投影与筛选 |
| 创建路径保留「Advanced / raw JSON」逃生口 | **禁止** — 本片明确弃 textarea；与版本对话框一致 |
| 宣称 go-live / 激活 CD-3 / 正式 P3 / #50 | **禁止** |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **条款作者** | `authorContentModules` | 打开创建对话框；用结构化编辑器编写初始内容；提交创建 |
| **目录浏览者** | `canBrowseContentModuleCatalog`（既有） | 看列表状态列；使用状态筛选；无创建权则无 Create CTA |
| **可配置共享组者** | `configureContentModuleSharedGroups` | 创建时「Share to groups」行为不变（CE-U10） |
| **无授权会话** | — | 不可创建；不可见他组模块；403/404 既有错误面 |
| **系统** | CM create/list API + `ControlledStructuredContentEditor` | 持久化 structure JSON；摘要 enrich head 状态；服务端 filter |

---

## 3. Goal

1. 作者创建条款时，在对话框内用 **结构化块编辑器**（非 JSON textarea）编辑初始内容，并可使用既有嵌套/校验/undo-redo 能力（编辑器自身已交付）。  
2. 提交后仍创建模块 + 首个 DRAFT 版本，`contentStructureJson` 为规范化 structured content JSON。  
3. 条款目录每一行展示与详情版本表一致的 **状态徽章**（head 版本）。  
4. 作者/浏览者可按状态筛选目录，筛选为 **服务端** 精确匹配，并与既有 search / groupCode / sort 共存（LR-C5）。  
5. Formal phase 保持 **None**；不宣称 go-live；不激活 CD-3。

---

## 4. 已确认决策 vs 仓库事实推导

### 4.1 已确认（产品 / 计划）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U20-C1** | 创建对话框弃 JSON textarea，改结构化编辑器。 | CE 计划 §4 CE-U20；Task Master #94 |
| **U20-C2** | 目录加状态列 + 筛选。 | 同上 |
| **U20-C3** | P2·S；frontend-focused；依赖 U18 Done。 | CE 计划 / Task Master |
| **U20-C4** | Formal phase **None**；不宣称 go-live；不激活 CD-3；P3 parked；leave #50。 | CE 计划 |
| **U20-C5** | 用户面变更 → E2E + UIUX；银行 OA + English-first。 | delivery constitutions |
| **U20-C6** | Batch recommendation：**solo** `#94`。 | Stage −1 |

### 4.2 本片确认的实现决策（计划卡薄 → 仓库事实推导）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **U20-D1** | **复用** `ControlledStructuredContentEditor`（与 `ContentModuleVersionDialog` 同组件）；不新建第二套编辑器。CE-U01 嵌套能力随组件带入，本片不改嵌套语义。 | 版本对话框已落地；CE-U01 Done |
| **U20-D2** | 创建表单默认 `contentStructureJson = DEFAULT_STRUCTURED_CONTENT_JSON`；打开/reset 均如此。提交前由编辑器产出的 JSON 字符串写入既有 `CreateContentModuleRequest.contentStructureJson`；**不再**要求用户手写或通过 textarea 校验 `JSON.parse`（编辑器保证可序列化）。 | 现状 create 默认 legacy `blocks`；版本对话框已用 DEFAULT |
| **U20-D3** | 创建对话框 **不** 展示结构 JSON textarea / Advanced JSON。`description` / `changeDescription` 等普通多行文本框保留。对话框宽度对齐版本对话框（约 **900px**），避免编辑器挤在 640px。 | U20-C1；版本对话框 `width="900px"` |
| **U20-D4** | 创建成功路径不变：store `createModule` → toast → 导航详情。CE-U10 shared groups 行为不变。 | 现状 `handleCreated` / SGC tests |
| **U20-D5** | **Head 版本：** 模块摘要状态取该模块全部版本中 `updatedAt` 最大者；并列时取 `semanticVersion` 字典序更大者。投影 `reviewState`（必填）+ `lifecycleState`（可空）。 | Summary 当前无状态；详情按版本展示；需单一目录状态 |
| **U20-D6** | **状态列 UI：** 复用 `ContentModuleStatusBadge`（lifecycle DEPRECATED/STOPPED 优先，否则 reviewState）。列标题 English-first：`Status`。 | 详情 versions 表已用同一徽章 |
| **U20-D7** | **契约：** `ContentModuleSummaryView` 增加 `reviewState`（required）与 `lifecycleState`（optional）；OpenAPI + Java record + FE 类型同步。模块无版本（异常）→ fail-closed：不出现在可筛选成功页或返回可观测错误——实现须保证正常 create 路径至少有一版本；测试锁定「至少一版本」的摘要映射。 | 无字段则无法做服务端 filter / 列绑定 |
| **U20-D8** | **筛选：** `GET .../content-modules` 新增可选 query `status`，枚举与徽章展示值对齐：`DRAFT` \| `SUBMITTED` \| `APPROVED` \| `STOPPED` \| `DEPRECATED`（`UPPER_SNAKE_CASE`）。匹配规则与徽章一致：若 head `lifecycleState` 为 `DEPRECATED`/`STOPPED` 则仅这些值命中；否则按 `reviewState` 命中（`APPROVED` 且 lifecycle 为 `ACTIVE` 或 null/缺省）。非法/未知 `status` → **空页**（不 400）。与 `search`/`groupCode`/`sort`/K08 legal filters **AND**。改 status → page 重置 0。 | LR-C5 C5-C6 对 CM 状态 filter 的开放条款；模板目录同类 UX |
| **U20-D9** | FE `listContentModules` / store `fetchModules` / `useContentModuleListView` 传递 `status`；`CatalogFilterToolbar` 增加 select filter（选项 i18n：`contentModules.reviewState.*` + `contentModules.lifecycle.*`）。**禁止**仅在当前页做客户端 status filter。 | LR-C5 server paging |
| **U20-D10** | 授权：创建 CTA / 提交沿用 `authorContentModules`；列表沿用 catalog browse；结构字段读权限不变（create 请求体由作者提交，不改变 read 省略规则）。 | permission-matrix §5 / §5.1 |
| **U20-D11** | i18n English-first（en + zh-CN）；控件如 **Create content module**、**Status**、结构化编辑器既有文案复用。 | i18n-english-first |
| **U20-D12** | `frontend_ui_in_scope=true` → Stage 5/6/7 + 10 按管线；BE 契约变更 → `mvn verify` 必跑。 | delivery pipeline |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 点击条款目录 **Create** | 打开 `ContentModuleCreateDialog` |
| T2 | 在创建对话框编辑块并 **Submit** | `POST /content-modules` |
| T3 | 进入条款目录路由 / 翻页 / 改 search·group·sort·**status** | 服务端列表请求 |
| T4 | 清除 status filter chip | 重新请求无 `status` 的列表 |

---

## 6. Preconditions

- 用户已登录；创建场景具备 `authorContentModules`；目录场景具备 catalog browse。  
- 会话对目标 `groupCode` 有授权（创建与筛选均 fail-closed）。  
- Docker 验收栈可用（E2E）：`http://localhost:4173` + `http://localhost:8080`。  
- Formal phase **None**；不依赖 go-live / CD-3。

---

## 7. Primary journey

1. 作者打开 Content modules 目录 → 点击 Create。  
2. 对话框展示元数据字段 + **结构化编辑器**（无结构 JSON textarea）。  
3. 作者添加/编辑 paragraph（或嵌套块）→ Submit。  
4. 系统创建模块与首版本；导航到详情；详情 content/versions 可见刚写入的 structure。  
5. 返回目录：新行 **Status** 显示 `DRAFT`（新建默认）。  
6. 作者将 status filter 设为 `DRAFT` → 列表仅含 head 状态为 DRAFT 的模块；设为 `APPROVED` → 不含该新模块。

---

## 8. System responses

| 路径 | 响应 |
| --- | --- |
| 创建成功 | 201/200 既有信封；详情含版本；UI toast + 路由详情 |
| 创建校验失败（code/name 等） | 表单校验；不调用 API |
| 创建 API 失败 | 对话框内错误 alert（messageKey）；对话框保持打开 |
| 列表加载 | `PageView`；每行含可渲染 status 徽章字段 |
| status filter | 请求带 `status=`；结果仅匹配 head 展示状态 |
| 无权限创建 | 无 Create 按钮；直调 API → 403 既有 |
| 无权限浏览 | 既有 deny / 空错误面；不泄露他组 |

---

## 9. Acceptance scenarios (Given / When / Then)

### BDD-CE-U20-CCS-001 — 创建对话框无结构 JSON textarea

```gherkin
Given 会话具备 authorContentModules
When 作者打开 Content module 创建对话框
Then 不存在用于编辑 contentStructureJson 的 textarea
And 存在 ControlledStructuredContentEditor（或等价 data-testid 结构化编辑器根）
```

### BDD-CE-U20-CCS-002 — 默认 structured content 非 legacy blocks

```gherkin
Given 作者新打开创建对话框（或关闭后再次打开）
When 检查初始 contentStructureJson / 编辑器文档
Then 其为 DEFAULT_STRUCTURED_CONTENT_JSON 形态（含 schemaVersion 与 nodes）
And 不是仅 {"blocks": []} 的 legacy 默认
```

### BDD-CE-U20-CCS-003 — 结构化编辑后成功创建

```gherkin
Given 创建对话框已打开且必填元数据有效
And 作者在编辑器中写入可见段落文本
When 作者点击 Submit
Then createModule / POST 的 contentStructureJson 包含该段落文本（规范化 nodes JSON）
And 创建成功后导航到新模块详情
```

### BDD-CE-U20-CCS-004 — 共享组行为不被本片破坏

```gherkin
Given GROUP_ADMIN 打开创建对话框
When 选择 Share to groups 并提交合法结构
Then payload 仍包含所选 sharedGroupCodes（CE-U10）
```

### BDD-CE-U20-CCS-005 — 目录状态列可见

```gherkin
Given 目录至少有一个 head 为 DRAFT 的模块与一个 head 展示为 APPROVED 的模块
When 作者打开 Content modules 列表
Then 表格存在 Status 列
And 各行渲染 ContentModuleStatusBadge，文案分别对应该行 head 状态
```

### BDD-CE-U20-CCS-006 — 状态筛选 DRAFT

```gherkin
Given 目录同时存在 DRAFT 与 APPROVED（按徽章语义）模块
When 作者将 Status filter 设为 DRAFT
Then 列表请求携带 status=DRAFT
And 结果仅包含 head 展示状态为 DRAFT 的模块
And 当前页重置为第一页（page 0）
```

### BDD-CE-U20-CCS-007 — STOPPED / DEPRECATED 筛选

```gherkin
Given 存在 head lifecycleState=STOPPED 的模块
When 作者筛选 status=STOPPED
Then 该模块出现在结果中
And 筛选 status=DRAFT 时该模块不出现（徽章优先 lifecycle）
```

### BDD-CE-U20-CCS-008 — 非法 status → 空页

```gherkin
Given 调用方传入未知 status 值
When GET content-modules?status=NOT_A_REAL_STATUS
Then 响应为成功空页（content=[]，totalElements=0 或等价空结果）
And 不返回 500
```

### BDD-CE-U20-CCS-009 — 无创建权 fail-closed

```gherkin
Given 会话不可 authorContentModules
When 打开条款目录
Then 不展示 Create 入口
And 结构化创建对话框不可达
```

### BDD-CE-U20-CCS-010 — E2E 创建 + 列表状态（用户旅程）

```gherkin
Given Docker 验收栈健康且作者已登录
When 作者用结构化编辑器创建模块并返回目录
Then 新行 Status 为 DRAFT
And 筛选 DRAFT 仍可见该模块
And UIUX 证据双品牌 @1920 Critical=0（银行 OA）
```

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 编辑器为空段落默认文档 | 允许提交（与版本对话框一致）；后端既有 structure 校验若拒绝则展示 messageKey |
| 嵌套深度 / 校验 | 沿用 ControlledStructuredContentEditor + CE-U01；本片不放宽/收紧 |
| status 与 groupCode/search 同时生效 | AND；无匹配 → 空态面板 |
| K08 legal query 参数 | 保持可用；与 status AND（本片不改 legal 语义） |
| Summary 缺状态字段的旧客户端 | 本片同步 FE；无外部 runtime 依赖 CM summary |
| 多版本模块 | 仅展示/筛选 **head**（U20-D5）；详情仍可看全部版本状态 |

---

## 11. Observable evidence

| 证据 | 命令 / 产物 |
| --- | --- |
| 单元 / 组件 | `ContentModuleCreateDialog` Vitest（无 textarea；submit JSON 形态）；list view / store status query；BE summary enrich + status filter 测试 |
| 后端门禁 | `mvn -B -ntp -f backend/pom.xml verify` |
| 前端门禁 | `pnpm -C frontend lint` / `type-check` / `test` / `build` |
| E2E | `frontend/e2e/ce-u20-*.spec.ts`（命名实现期锁定） |
| UIUX | `frontend/e2e/evidence/CE-U20-*-manifest.md` + dual-brand screenshots |
| Deploy | `.\scripts\docker-deploy-queue.ps1` Stage 5/10 证据目录 |

---

## 12. Traceability

| 来源 | 本规格 |
| --- | --- |
| CE 计划 §4 CE-U20 | CCS-01…CCS-04；U20-C1/C2 |
| Task Master **#94** | 全片 |
| CE-U01 nested editor | U20-D1 复用 |
| ContentModuleVersionDialog | U20-D1/D2/D3 对齐 |
| LR-C5 catalog pagination | U20-D8/D9 服务端 filter |
| CE-U10 shared groups | CCS-004 回归 |
| CE-U08 status badge / review | U20-D6 徽章语义 |
| permission-matrix content modules | U20-D10 |
| ADR-0019 structured content | 默认 nodes JSON |

---

## 13. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-u20-clause-create-structured.md
task_ids: ["94", "CE-U20"]
frontend_ui_in_scope: true
batch_recommendation: solo
member_task_ids: ["94"]
proposed_slice_id: ce-u20-clause-create-structured
```

**Handoff:** `plan-orchestrator` → 实现任务分解（FE create dialog + list；BE summary/status filter；OpenAPI；E2E/UIUX）。
