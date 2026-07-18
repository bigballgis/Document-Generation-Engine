# BDD 行为规格：PRR-D01c — Dashboard summary API（首屏停用 fetchAll）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-PRR-D01C` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | NON-CE PRR Wave D **split** residual leaf（`prod-dashboard-summary-api`） |
| **Slice** | `prod-dashboard-summary-api` |
| **Branch** | `feat/prod-dashboard-summary-api` |
| **Worktree** | `D:/working/DGE-prod-dashboard-summary-api` |
| **Placement** | ISOLATED |
| **Task Master** | **#136** PRR-D01c — Batch Recommendation **split**；本叶 `member_task_ids: ["136"]` |
| **Prior leaf** | **#135** PRR-D01b（`prod-ops-security-hardening`）— actuator/nginx/IRC/ADR-0044 **Done**（dashboard FE 已 veto） |
| **Formal phase** | **None**（生产体量 Dashboard 有界加载叶；不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **split**（`member_task_ids: ["136"]`；`proposed_slice_id: prod-dashboard-summary-api`；`vetoes_applied: ["IBL"]`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [ce-u14-dashboard-lifecycle-todos.md](./ce-u14-dashboard-lifecycle-todos.md)；[lrp-c5-catalog-pagination.md](./lrp-c5-catalog-pagination.md)；[prod-ops-security-hardening.md](./prod-ops-security-hardening.md)；契约 [contract-outline.md](../api/contract-outline.md) / OpenAPI 管理面 |
| **Frontend UI** | **`frontend_ui_in_scope=true`**（E2E + UIUX **强制**） |

**完成声明约束：** 本叶关闭 Dashboard **首屏 / Overview 统计卡**对 masters/templates 的**无界 fetch-all** 依赖，改为**有界、授权组范围内**的服务端汇总计数（或等价耐久聚合）。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b**（保持 **CONDITIONAL**）；**禁止**将 **#5a** 标为 **GO**；**禁止**宣称 full Wave D bag Done；**禁止**激活 IBL 为本叶并行叶。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: split
  member_task_ids: ["136"]
  proposed_slice_id: prod-dashboard-summary-api
  vetoes_applied: ["IBL"]
  rationale: >
    Wave D residual after D01B; dashboard FE+BE summary is the sole remaining
    first-paint unbounded catalog load; keep IBL deferred (serial queue).
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| Dashboard 首屏停用 `fetchAllMasters` / `fetchAllTemplates`（及等价 `listAll*` 全页合并）作为 Overview 统计卡数据主路径 | 宣称 go-live / 翻转 **#3b GO** / **#5a GO** |
| 耐久服务端 summary / 聚合计数（授权组范围）驱动统计卡 | IBL 激活 / 与本叶并行交付 |
| 保持 CE-U14 协作生命周期待办与其它已交付 workflow todos 正确性 | 改变协作队列可见性矩阵 / 超时升级语义 |
| FE 消费 summary；E2E + UIUX | 目录主列表再造（LR-C5 已 Done；本叶不重做 catalog UX） |
| OpenAPI / contract-outline 同步管理面 summary（若新增端点） | knip / 全局 listAll 清扫（非本叶主角） |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| Dashboard `loadDashboardData` 在可访问母版/模板路由时调用 `mastersStore.fetchAllMasters()` 与 `templatesStore.fetchAllTemplates()` | `frontend/src/composables/useDashboardDataLoader.ts` |
| `fetchAll*` 经 `listAll*` **逐页合并全目录**进 store，生产体量下首屏无界 | `createMastersCatalogActions.ts` / `createTemplatesCatalogActions.ts`（注释：dashboard consumers） |
| Overview 统计卡除 `catalogMasters` / `catalogTemplates`（已用 `*ListTotalElements`）外，还对 **内存中的全量** `masters` / `templates` 做状态分桶计数 | `useDashboardStats.ts`：`PENDING_REVIEW`、`DRAFT\|REJECTED`、workflow lifecycle、`PUBLISHED`、`STOPPED` |
| 目录主路径已是 LR-C5 `PageView` 分页；Dashboard 仍走 fetch-all 旁路 | [lrp-c5-catalog-pagination.md](./lrp-c5-catalog-pagination.md) **Done** |
| CE-U14 模板生命周期待办权威源 = OPEN collaboration work items（**不**依赖 fetch-all 目录） | [ce-u14-dashboard-lifecycle-todos.md](./ce-u14-dashboard-lifecycle-todos.md)；`useWorkflowTasks` + `collaborationStore` |
| 母版审核 / 返工待办与部分 Overview 旅程解析仍扫描 `mastersStore.masters` / `templatesStore.templates` | `useWorkflowTasks.ts`；`useDashboardJourneyRoleResolutions.ts` |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **D01C-S1 首屏有界** | Dashboard 首次加载 / Overview 统计卡路径**不得**触发 masters/templates 无界全量拉取 |
| **D01C-S2 服务端汇总** | 统计卡计数由**耐久 BE summary / 聚合**提供（见 §4 确认决策）；授权组范围与 catalog 一致 |
| **D01C-S3 待办不回归** | CE-U14 及既有 workflow todos（含母版审核/返工、作者条款/年审、CM 审核）在授权范围内保持完整可发现；不得因「只取第一页目录」而静默截断 |
| **D01C-S4 FE + E2E** | Vue Dashboard 消费 summary；Playwright 功能 + UIUX 强制 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **管理会话用户** | 可访问 Dashboard；按路由可见性看 masters/templates/api-policy 统计卡 | Overview 首屏见正确计数，无需等待全目录下载 |
| **测试 / 审批 / 发布负责人** | `decideTests` / `decideApprovals` / `publishTemplates` 等（CE-U14） | Tasks Tab 生命周期待办与深链行为不回归 |
| **母版审核人 / 设计师** | `reviewMasters` / `manageMasters` | 母版 PENDING_REVIEW / 返工待办仍完整可见（有界专用路径，非 fetch-all） |
| **系统（API）** | Management summary（本叶）+ 既有 collaboration / author-workflow / CM workflow APIs | 聚合计数 fail-closed；无会话 → 401；无组授权 → 零计数/空 |
| **系统（UI）** | `useDashboardDataLoader` + `useDashboardStats` + Dashboard Overview | 首屏调用 summary；错误时统计区可降级隐藏（对齐今日 `showStatsSection`） |

---

## 3. Goal

1. 打开 Dashboard（Overview）时，**首屏统计卡**加载路径对 masters/templates **有界**——网络与内存均不得随目录总行数线性膨胀到「全量合并」。  
2. 统计卡数字在授权组范围内与今日语义一致（分桶定义见 §4），可观测来自 summary/聚合响应，而非客户端全量 filter。  
3. CE-U14 协作生命周期待办及其它已交付 workflow todos **不回归**。  
4. `frontend_ui_in_scope=true`：E2E 功能 + UIUX 通过；银行 OA + English-first i18n。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **D01C-C1** | **采用耐久服务端 Dashboard summary / 聚合计数（首选）**，供 Overview 统计卡使用。路径形态实现锁定其一并写进 OpenAPI：优先新建紧凑管理端点（推荐 `GET /api/management/v1/dashboard/summary`），或等价的 masters/templates **count-aggregate** 端点；**禁止**仅用「`page=0&size=1` + `totalElements`」作为本叶完整解，因其**无法**覆盖现有分桶卡（见 D01C-C2）且易被误读为 FE-only 残缺方案。 | `useDashboardStats` 对内存全量分桶；父叶意图「prefer small durable BE summary」；LR-C5 已提供分页但非聚合 |
| **D01C-C2** | **统计卡语义保持**（授权组 ∩ 路由可见性过滤后）。BE summary **至少**提供与下列 key 对应的诚实计数（命名可映射，语义锁定）：`masterPendingReview`（`status=PENDING_REVIEW`）、`masterVersionsInProgress`（`DRAFT` ∪ `REJECTED`）、`templateVersionsInWorkflow`（`DRAFT` ∪ `TESTING` ∪ `APPROVAL` ∪ `PENDING_RELEASE`）、`publishedVersions`（`PUBLISHED`）、`stoppedVersions`（`STOPPED`）、`catalogMasters`（母版总数）、`catalogTemplates`（模板总数）。`pendingActions` 继续来自既有 workflow tasks 投影（非本 summary 必填字段）。`externalServicesAlerts` 继续来自既有 alerts API。 | `useDashboardStats.ts` 现网定义 |
| **D01C-C3** | **首屏禁止 fetch-all：** `useDashboardDataLoader`（及等价 Dashboard 首载路径）在加载 Overview 统计卡时**不得**调用 `fetchAllMasters` / `fetchAllTemplates` / `listAllMasters` / `listAllTemplates`。单测 / E2E 网络断言可观测。 | 本叶核心目标 |
| **D01C-C4** | **授权范围：** summary 计数仅含会话可访问组内实体；与 catalog list 的 group-access 一致；无权限路由对应卡不展示（既有 `visibleRoutes` 过滤保留）；未认证 → 401；跨组 fail-closed（不泄露他组计数）。 | LR-C5 / permission-matrix catalog 语义 |
| **D01C-C5** | **CE-U14 不回归：** 模板 `TEST` / `APPROVAL` / `PENDING_RELEASE`（及既有 REMEDIATION/ESCALATION）Tasks 投影与队列感知深链行为保持 [ce-u14-dashboard-lifecycle-todos.md](./ce-u14-dashboard-lifecycle-todos.md)；权威源仍为 OPEN collaboration work items。 | CE-U14 **Done** |
| **D01C-C6** | **母版 / 其它 todos 完整性：** 依赖实体行扫描的待办（如 `reviewMasters`→`PENDING_REVIEW`、`manageMasters`→返工候选）**不得**因去掉 fetch-all 而只显示「当前页」子集。允许实现为：(a) 既有/新增**有界 workflow inbox API**；(b) **状态过滤的 PageView 拉全候选**（page size≤100，循环至耗尽，候选集预期远小于全目录）；(c) summary 附带有界 candidate ids——实现任选，验收要求授权范围内**完整**。作者条款过期 / 年审、CM workflow 继续走既有专用 API。 | `useWorkflowTasks.ts` |
| **D01C-C7** | **Overview 旅程卡：** 不得为解析旅程而重新引入全目录 fetch-all。允许协作 work items + 有界状态过滤页 + summary 辅助；旅程步骤可见性允许在「无实体行缓存」时退化为协作/任务驱动（不得伪造步骤）。Master designer 旅程若需母版行，使用 D01C-C6 有界路径。 | `useDashboardJourneyRoleResolutions.ts` |
| **D01C-C8** | **错误降级：** summary 失败时，对齐今日 masters/templates 加载失败 → `showStatsSection=false`（或等价隐藏统计区）；不得用错误的部分页计数冒充全量。Workflow/collaboration 加载失败保持既有分区错误态。 | `useDashboardDataLoader` `mastersLoadError` / `templatesLoadError` |
| **D01C-C9** | **契约：** 若新增/扩展管理 API，同步 `docs/api/openapi-v1.yaml`（或管理面子契约）与 `contract-outline.md`；统一 envelope；枚举 `UPPER_SNAKE_CASE`。 | tech-stack / document-as-code |
| **D01C-C10** | **门禁：** BE `mvn verify`；FE `lint` / `type-check` / `test` / `build`；Stage 5/10 queued Docker；E2E 功能 + UIUX；architecture review。 | delivery constitution |
| **D01C-C11** | **完成边界：** 本叶 Done ≠ Wave D bag 产品完备；≠ go-live；#3b/#5a 保持 CONDITIONAL。 | 队列政策 |
| **D01C-C12** | **FE-only 有界页方案（否决为本叶主路径）：** 仅调用 `fetchMasters(0,1)` / `fetchTemplates(0,1)` 取 `totalElements` **不足**——会丢失分桶卡正确性或迫使 N 次状态过滤往返且仍难覆盖旅程/母版待办。允许在实现中**辅助**使用有界 PageView（D01C-C6），但 **Overview 分桶计数的权威源 = BE summary（D01C-C1）**。 | 仓库事实裁决 |

### 4.2 已确认（上游交付，本叶只消费）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **D01C-U1** | Catalog list 已为 `PageView` + 服务端 filter（含 `status` / `lifecycleStatus`） | LR-C5 |
| **D01C-U2** | 模板生命周期 Tasks 权威源 = collaboration work items | CE-U14 / P21 |
| **D01C-U3** | Formal phase None；非 go-live | PRR / CE 队列 |

### 4.3 非确认假设（不得升格为需求）

| 项 | 状态 |
| --- | --- |
| Summary 响应是否包含 content-modules 计数 | **不强制**（今日 Overview 卡无 CM 目录总数卡） |
| Summary 是否返回图表/时间序列 | Out of scope |
| 是否废弃 `fetchAll*` 符号（其它 picker/import 消费者） | **本叶非硬门槛**——仅强制 Dashboard 首屏路径停用；其它调用方可另叶清理 |
| 实时推送 / SSE 刷新统计卡 | Out of scope（保持请求-响应） |

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录管理会话，可访问 Dashboard。  
- 目标授权组内存在可计母版/模板（可为 0；零计数合法）。  
- LR-C5 catalog list API 可用；本叶 summary API（交付后）可用。  
- Collaboration / author-workflow / CM workflow APIs 可用（待办回归）。

**Triggers**

- 用户导航至 `/dashboard`（或默认落地 Dashboard）并进入 Overview。  
- （回归）用户打开 Tasks Tab / 行为型 `?queue=` 入口。

---

## 6. Primary journey

1. 用户打开 Dashboard → Overview。  
2. 前端发起 **有界** summary（及既有 collaboration/alerts/workflow 专用请求）；**不**发起 masters/templates 全量合并。  
3. 系统按授权组返回聚合计数。  
4. Overview 展示统计卡（路由可见性过滤后），数字与授权范围内实体状态一致。  
5. 用户切换至 Tasks：CE-U14 与其它可见队列待办仍完整可发现；深链行为不回归。

---

## 7. System responses

### 成功

- HTTP 200 + 统一 envelope；summary 字段含 D01C-C2 计数（整数 ≥ 0）。  
- UI：统计卡展示；loading 结束后无全量 catalog 传输。  
- 网络：无 `listAll*` / 无「page 循环直至耗尽全目录」的 Dashboard 首屏路径。

### Fail-closed / 降级

- 未认证 → 401。  
- 无 Dashboard/管理会话能力 → 既有路由守卫。  
- Summary 失败 → 隐藏统计区（或等价错误态），不展示误导性部分计数。  
- 无组授权 → 相关计数为 0；不泄露他组。

---

## 8. Acceptance scenarios（Given / When / Then）

### BDD-PRR-D01C-001 — 首屏不触发 fetch-all

```gherkin
Given 管理用户可访问 Dashboard 且授权组内母版与模板总数均显著大于单页上限（例如各 ≥ 120）
When 用户打开 Dashboard Overview 首屏
Then 浏览器网络中不得出现对 masters/templates 的无界全量合并请求（fetchAll / listAll 或等价逐页耗尽全目录）
And Overview 统计卡仍能显示 catalog 总数与状态分桶计数
```

### BDD-PRR-D01C-002 — Summary 分桶与目录真相一致

```gherkin
Given 授权组内存在已知分布的母版状态（含 PENDING_REVIEW、DRAFT、REJECTED）与模板生命周期（含 DRAFT/TESTING/APPROVAL/PENDING_RELEASE/PUBLISHED/STOPPED）
When 客户端请求 Dashboard summary（或等价聚合 API）
Then 响应中各分桶计数分别等于该授权范围内对应状态的实体数量
And catalogMasters / catalogTemplates 分别等于母版/模板总数
```

### BDD-PRR-D01C-003 — 路由可见性过滤保留

```gherkin
Given 会话可见路由包含模板管理但不包含母版管理
When 用户打开 Dashboard Overview
Then 不展示母版相关统计卡（masterPendingReview / masterVersionsInProgress / catalogMasters）
And 仍展示模板相关统计卡（在模板路由可见时）
```

### BDD-PRR-D01C-004 — 跨组 fail-closed

```gherkin
Given 会话仅授权组 A，组 B 存在大量母版/模板
When 请求 Dashboard summary
Then 计数不得包含组 B 实体
And 响应不泄露组 B 的名称或 id
```

### BDD-PRR-D01C-005 — Summary 失败降级

```gherkin
Given Dashboard summary API 对当前会话返回 5xx 或网络失败
When 用户打开 Dashboard Overview
Then 统计卡区域按既有模式隐藏或显示错误态（不得展示错误的部分页计数冒充全量）
And 页面其余可访问区域（如 Tasks 入口）不因统计失败而整体白屏崩溃
```

### BDD-PRR-D01C-006 — CE-U14 生命周期待办不回归

```gherkin
Given 组内存在 OPEN 的 TEST / APPROVAL / PENDING_RELEASE collaboration work items
And 会话具备对应 decideTests / decideApprovals / publishTemplates 能力
When 用户打开 Dashboard → Tasks（或 ?queue= 行为入口）
Then 对应当前会话可见队列的待办分区与条目仍可见
And 待办深链仍按队列落到 testing / approval / publishReadiness 决策面（CE-U14）
And 本场景不得依赖 masters/templates fetch-all
```

### BDD-PRR-D01C-007 — 母版审核待办完整性（有界）

```gherkin
Given 会话具备 reviewMasters
And 授权组内 PENDING_REVIEW 母版数量大于单页 size（例如 > 20）
When 用户打开 Dashboard → Tasks
Then 所有授权范围内 PENDING_REVIEW 母版待办均出现在任务列表（不得只显示第一页）
And 加载路径保持有界（专用 inbox 或状态过滤分页拉全候选，而非全目录 fetch-all）
```

### BDD-PRR-D01C-008 — 未认证拒绝

```gherkin
Given 无有效管理会话
When 客户端请求 Dashboard summary API
Then 响应为 401（或与既有管理 API 未认证语义一致）
And 不返回任何组内计数正文
```

### BDD-PRR-D01C-009 — 零数据合法

```gherkin
Given 会话已认证但授权组内无母版且无模板
When 用户打开 Dashboard Overview
Then 相关统计卡计数为 0（或卡按路由隐藏）
And 不出现 fetch-all
And 不出现错误态（除非其它独立 API 失败）
```

### BDD-PRR-D01C-010 — E2E 首屏有界 + 卡可见

```gherkin
Given Docker 验收栈可用且种子数据使目录总量 > 单页上限
When Playwright 以授权用户打开 Dashboard Overview
Then 统计卡区域可见且至少 catalog 与一分桶计数与种子预期一致
And 测试证据记录首屏未发出 fetch-all / listAll 全量合并
```

### BDD-PRR-D01C-011 — UIUX 银行 OA

```gherkin
Given BDD-PRR-D01C-010 功能场景通过
When 进行 E2E UIUX 评审（双品牌截图 / manifest）
Then Critical = 0
And 统计区符合银行 OA 布局与 English-first i18n（无硬编码中文主文案）
```

### BDD-PRR-D01C-012 — 完成声明边界

```gherkin
Given 本叶代码与门禁通过
When 更新计划 / Task Master / ledger
Then #136 可标 Done
And 不得将 checklist #3b 或 #5a 标为 GO
And 不得宣称 go-live 或 full Wave D bag Done
And Formal phase 保持 None
```

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 目录极大（数千行） | Summary 为聚合查询 / COUNT；响应体保持小；首屏时延不随行数线性膨胀到全量传输 |
| 仅 masters 路由可见 | 只请求/展示母版相关计数；模板卡隐藏 |
| Collaboration 失败 | 既有 collaboration 错误态；不强制隐藏 summary 成功卡（除非实现选择统一 loading——须测且一致） |
| `fetchAll*` 仍被 import picker 使用 | 允许残留；本叶验收不要求全局删除符号 |
| 并发打开 Dashboard + 目录页 | 目录页继续 LR-C5 分页；互不要求共享全量 cache |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | Summary 200 + 字段计数；401 未认证；组范围断言（集成测试） |
| FE unit | `useDashboardDataLoader` / `useDashboardStats`：mock summary；断言不调用 `fetchAll*` |
| 网络 / E2E | Playwright 请求日志：无 listAll / 无全目录 page 耗尽；有 summary（或文档化的等价聚合） |
| UI | Overview 统计卡数字；Tasks CE-U14 回归 |
| UIUX | `frontend/e2e/evidence/` manifest + 截图 |
| Gates | `mvn verify`；FE lint/type-check/test/build；queued `docker-deploy-queue` |
| Docs | 本文件；OpenAPI/contract；ledger 证据行（post-task-doc-sync） |

---

## 11. Traceability

| 项 | 引用 |
| --- | --- |
| Task Master | **#136** PRR-D01c |
| Slice | `prod-dashboard-summary-api` |
| Batch | **split** `member_task_ids: ["136"]` |
| Upstream | #135 D01B Done；LR-C5 Done；CE-U14 Done |
| Plan mirrors | `docs/plan/master-plan.md`；`execution-sync-ledger.md`；`PROJECT-STATUS-RESET.md`（doc-sync 阶段） |
| 非目标 | go-live；#3b/#5a GO；IBL 激活 |

---

## 12. BDD readiness

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/prod-dashboard-summary-api.md
task_ids: ["136"]
approach_confirmed: durable_BE_dashboard_summary
rejected_as_sole_path: FE_only_page0_size1_totalElements
frontend_ui_in_scope: true
e2e_uiux_required: true
formal_phase: None
go_live: false
checklist_3b_5a: remain CONDITIONAL (do not flip GO)
```

**Handoff → `plan-orchestrator`：** 按本规格拆 TDD 任务（BE summary + FE loader/stats 改造 + 待办有界路径 + E2E/UIUX）；实现期锁定具体 OpenAPI path/DTO 名称时以 D01C-C1/C2 语义为准。
