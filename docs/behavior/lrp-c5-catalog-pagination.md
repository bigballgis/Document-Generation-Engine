# BDD 行为规格：LR-C5 — Catalog server-side pagination/filter

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-11  
**BDD ID**: `BDD-LRP-C5-CATALOG-001`  
**来源任务**: [LRP Wave LR-C § LR-C5 — Catalog server-side pagination/filter](../plan/detail/LRP-C-usability-deepening.md)  
**程序发现**: [launch-readiness-program.md](../plan/launch-readiness-program.md) Wave LR-C / catalog full-fetch latency  
**映射**: OPT-F4 residual（masters/content-modules + 端到端 filter）；COR-F09 group-first 语义保留（本规格裁决）；SOR-P01 模板列表分页基线扩展  
**Task Master / slice**: plan `LR-C5` / slice `lrp-c5-catalog-pagination`  
**伴生（非本切片）**: LR-C6 command palette 可复用本切片的 `search` / filter 查询参数（仅溯源；**不**实现 C6）

---

## 1. 概述

管理端三大目录列表（**Templates** / **Masters** / **Content modules**）必须改为 **服务端分页 + 服务端筛选/搜索**，返回标准 `PageView` 元数据（`content` / `page` / `size` / `totalElements` / `totalPages`）。前端不得再依赖「全量拉取 + `useCatalogPagination` 客户端 `slice`」作为目录主路径；在 ≥500 行种子数据下，首屏列表请求 **p95 < 1 s**（证据记入 ledger）。

| 行为域 | 摘要 |
| --- | --- |
| **D1 服务端分页** | `page`（0-based）+ `size`；默认 size **20**；上限 **100**；`totalElements`/`totalPages` 诚实 |
| **D2 服务端筛选/搜索** | 目录工具栏 search + 字段 filter +（模板）workflow chip 全部驱动 **网络分页请求**，禁止回退全量客户端切片主路径 |
| **D3 Group-first** | 默认排序 **组优先**（`groupCode ASC` + 次级键）；组为组织主键；分页单位为 **行**（非「按组数分页」） |
| **D4 授权范围** | 仅返回会话可访问组内实体；空授权 → 空页；跨组 fail-closed |
| **D5 性能证据** | ≥500 行跨组种子；记录 p95 列表延迟 < 1 s |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| LR-C6 全局搜索 / Ctrl+K 命令面板 | Out of scope — 仅允许在溯源中注明可复用 `search` 参数 |
| 按「组个数」分页（历史 `useGroupedCatalogPagination` Design A） | **Superseded** — 见决策 **C5-C3**；不以组为 page 单位 |
| Audit / Identity / 包内 version-lines / revision-lines 列表改造 | Out of scope（已有或另切片） |
| 改变目录列、空态/错态组件契约（LR-C9）或上传 UX（LR-C10） | Out of scope |
| 改变权限矩阵角色能力 | Out of scope — 仅消费既有 catalog browse 授权 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **目录浏览者** | 具备对应 catalog 浏览能力的管理会话角色（如 `TEMPLATE_AUTHOR`、`MASTER_DESIGNER`、`GROUP_ADMIN`、`GLOBAL_ADMIN` 等，以 permission matrix § catalog 为准） | 打开 Templates / Masters / Content modules 列表 |
| **工作流操作者** | `decideTests` / `decideApprovals` / `publishTemplates` | 使用模板列表 workflow chips（测试/审批/发布队列） |
| **系统（API）** | Management list endpoints | 按会话组范围返回 `PageView`；拒绝越权 |
| **系统（UI）** | `TemplateListView` / `MasterListView` / `ContentModuleListView` + stores | 发起分页请求、展示元数据与分页控件 |

授权：无浏览权限 → 路由 fail-closed（既有）；API 对不可访问组返回空结果或 `403`（与既有 group-access 一致）。本规格不改变权限矩阵。

---

## 3. Goal

1. 打开任一目标目录时，**仅加载当前页**行数据，并展示诚实的总数/总页数。  
2. 搜索、筛选、翻页、改变 page size 均触发 **带 `page`/`size`/filter 的服务端请求**（网络可观测）。  
3. **COR-F09**：组仍是主组织维度——默认 **group-first 排序**；`groupCode` 筛选时在组内分页；不破坏组列/筛选 UX。  
4. 在 ≥500 行种子下，列表请求 **p95 < 1 s**，证据可审计。  
5. Bank OA + i18n：分页与筛选文案走既有 i18n；REDBC/GREENBC 无品牌特例逻辑。

---

## 4. 已确认决策（2026-07-11，behavior-spec-author 裁决）

| ID | 决策 |
| --- | --- |
| **C5-C1** | **作用面**：管理目录列表三端点 — `GET /api/management/v1/templates`、`GET /api/management/v1/masters`、`GET /api/management/v1/content-modules`（及前端对应 stores/views）。 |
| **C5-C2** | **分页契约**：统一 `PageView`；`page` 默认 **0**；`size` 默认 **20**；`size` 合法范围 **1…100**（越界/缺失 → 默认 20；`page < 0` → 0）；超出末页 → `content: []` 且 `totalElements` 不变（与既有 PageView 约定一致）。 |
| **C5-C3** | **COR-F09 group-first（银行 OA 默认）**：**行分页** + **默认排序 `groupCode ASC`，次级 `updatedAt DESC`**（稳定簇集同组行）。**不**按「每页 N 个组」分页（历史 Design A / `useGroupedCatalogPagination` 已移除，本切片明确 supersede）。同组行在默认排序下连续出现；**允许**一个组的行跨页（flat `AppDataTable` 可接受）。用户显式选择其他 sort（如 `updatedAtDesc`、`nameAsc`）时，可打破组簇集——组组织仍可通过 `groupCode` 列与 filter 获得。 |
| **C5-C4** | **`groupCode` filter**：精确匹配（trim 后）；与会话授权求交；无权限组 → 空页（不泄露他组）。Content-modules 既有可选 `groupCode` 升级为与 page/size 共存。 |
| **C5-C5** | **`search`（可选）**：不区分大小写的 **contains**；Templates：`name` ∪ `externalId` ∪ `groupCode`；Masters：`name` ∪ `groupCode`；Content-modules：`name` ∪ `moduleCode` ∪ `groupCode`。空/空白 → 忽略。 |
| **C5-C6** | **状态 filter**：Templates：`lifecycleStatus`（精确；对应 UI `status`）；Masters：`status`（母版审核状态精确）；Content-modules：v1 **不**强制状态 filter（与当前 UI 一致；若实现期 UI 已有则对齐，否则不加）。 |
| **C5-C7** | **模板 workflow chips → 服务端**：`awaitingTest` → `lifecycleStatus=TESTING`；`awaitingPublish` → `lifecycleStatus=PENDING_RELEASE`；`awaitingApproval` → `lifecycleStatus=APPROVAL` **且** `approvalSubState=PENDING_DECISION`（新增可选 query `approvalSubState`，仅 templates）。Chip 与工具栏 filter **同时生效**（AND）；切换 chip/filter/search → **重置到 page 0**。 |
| **C5-C8** | **`sort` query（可选）**：白名单枚举字符串，至少支持：`groupCodeAsc`（**默认**）、`updatedAtDesc`、`updatedAtAsc`、`nameAsc`；Templates 另支持 `externalIdAsc`；Content-modules 另支持 `moduleCodeAsc`；Masters 另支持显式 `groupAsc`（与 `groupCodeAsc` 同义可接受其一）。非法 sort → 回退默认 `groupCodeAsc`（不 400，避免打断 UX；实现可记 debug）。 |
| **C5-C9** | **前端迁移**：三列表主路径 **始终** server paging；废除「有 filter 时回退客户端 slice」的 `serverPagingActive` 混合模式；`useCatalogPagination` 可保留给非目录表，但 **不得**再作为三大目录的数据源切片。UI 默认 page size 与 API 对齐为 **20**（可提供 10/20/50 选项；不得超过 100）。 |
| **C5-C10** | **OpenAPI / contract**：三 list 操作同步为 pageable + filter 参数 + `PageView` 响应；masters/content-modules 从「裸数组」升级为 `PageView`（**破坏性契约**，管理端一体升级，无外部 runtime 调用方依赖）。 |
| **C5-C11** | **性能门禁**：提供 ≥500 行跨多组种子（script/seeder flag）；在 Docker 验收栈记录 templates（及至少一侧 masters 或 modules）列表首屏或典型 page 请求 **p95 < 1 s**；证据路径写入 execution ledger / E2E evidence。 |
| **C5-C12** | **E2E + UIUX 强制**；规格文件 `frontend/e2e/LRP-C5-catalog-pagination.spec.ts`；manifest `frontend/e2e/evidence/LRP-C5-uiux-manifest.md`。 |
| **C5-C13** | **QueryDSL**：复杂多条件筛选可按 ADR-0037 机会性使用；简单单字段可用 Spring Data 方法/Specification — 实现选择，不阻塞行为。 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 进入 Templates / Masters / Content modules 列表路由 | 首次 `page=0` 加载 |
| T2 | 用户翻页 / 改 page size | 带当前 filter 的新 page 请求 |
| T3 | 用户输入 search、应用/清除 filter chip、切换 workflow chip | 重置 page=0 并请求 |
| T4 | 用户更改 sort | 重置 page=0 并请求 |
| T5 | LoadErrorPanel Retry / 显式刷新 | 重放当前查询 |

---

## 6. Preconditions

- 用户已登录且对目标目录具备浏览权限。  
- 后端与前端契约按本规格同步（OpenAPI）。  
- Docker 验收：`http://localhost:4173` + `http://localhost:8080`（Playwright / 性能证据）。  
- 性能场景：种子 ≥500 行 templates（跨 ≥2 个 `groupCode`）；masters/modules 至少一侧可演示分页（或同等规模）。

---

## 7. Primary journey（成功路径）

1. 操作者打开 Templates 目录。  
2. UI 请求 `GET /templates?page=0&size=20`（默认 sort=group-first）；渲染 `content`；分页控件显示 `totalElements` / `totalPages`。  
3. 操作者输入 search 或选择 `groupCode` / status → UI 以 **page=0** 重请求；网络可见 query 参数；表格仅显示匹配页。  
4. 操作者翻到第 2 页 → 仅请求 `page=1`；无全量下载。  
5. 默认排序下，同 `groupCode` 行簇集；跨组顺序按 `groupCode` 字典序。  
6. Masters / Content-modules 同等：打开 → 筛选 → 翻页均为服务端分页。

---

## 8. 系统响应

### 成功

- `200` + envelope `result: PageView<Summary>`。  
- `content.length ≤ size`；`totalPages = ceil(totalElements / size)`（size>0）。  
- UI：表格行 = 当前 `content`；`AppTablePagination` 绑定服务端 total；loading / LoadErrorPanel 既有模式。

### 失败 / 边界

| 条件 | 响应 |
| --- | --- |
| 未认证 | `401`；UI 既有会话处理 |
| 无组权限（空 accessible groups） | `200` 空页 `totalElements=0`（与 templates 现状一致）或既有 fail-closed；**不得**返回他组数据 |
| 无效枚举 filter（未知 lifecycleStatus/status） | `400`/`422` 统一错误信封 **或** 视为无匹配空页 — 实现任选其一，但须测试锁定；推荐 **空页**（宽松 UX） |
| size > 100 | 钳制为 100 或回退 20（推荐钳制到 100）；须测试锁定 |
| 列表加载失败 | LoadErrorPanel + retryable 标志（LR-C9 模式）；不静默空表冒充「无数据」 |

---

## 9. 验收场景（Given / When / Then）

### BDD-LRP-C5-001 — 首屏服务端分页元数据（≥500）

**Given** 系统中存在 **≥500** 个 templates，分布在 **≥2** 个 `groupCode`  
**And** 操作者具备 templates catalog 浏览权限  
**When** 打开 Templates 目录（默认无 filter）  
**Then** 网络请求包含 `page=0` 与 `size`（默认 20）  
**And** 响应 `PageView` 含诚实 `totalElements ≥ 500`、`totalPages = ceil(totalElements/size)`  
**And** 表格渲染行数 ≤ `size`  
**And** 该列表请求延迟满足 **p95 < 1 s**（证据入库）

### BDD-LRP-C5-002 — 翻页为服务端请求

**Given** Templates（或 Masters / Content-modules）`totalElements` 大于一页  
**When** 操作者切换到下一页  
**Then** 发出新的带 `page=N` 的列表请求（N≥1）  
**And** 页面内容与上一页无重复 id  
**And** **不**出现全量无 page 参数的目录 list 请求作为主路径

### BDD-LRP-C5-003 — 筛选 + 翻页均服务端

**Given** 目录存在可区分的 `groupCode`（或 status）数据  
**When** 操作者应用 filter（或 search）并翻页  
**Then** 请求同时携带 filter/search 与 `page`/`size`  
**And** 结果均满足筛选条件  
**And** `totalElements` 反映筛选后总数（非全库总数）

### BDD-LRP-C5-004 — Group-first 默认排序

**Given** 多组数据且无用户自定义 sort（或 sort=`groupCodeAsc`）  
**When** 加载任意页  
**Then** `content` 按 `groupCode` 升序簇集（同组相邻）  
**And** 同组内按 `updatedAt` 降序（默认次级）  
**And** 不要求「整组不跨页」，但要求组序稳定

### BDD-LRP-C5-005 — 组内筛选分页

**Given** 某 `groupCode` 下实体数大于一页  
**When** 操作者设置 `groupCode` filter 并翻页  
**Then** 所有页行均属于该组  
**And** `totalElements` 等于该组（授权可见）匹配总数

### BDD-LRP-C5-006 — 模板 workflow chip 服务端化

**Given** 存在 TESTING / APPROVAL+PENDING_DECISION / PENDING_RELEASE 模板  
**When** 操作者启用对应 workflow chip  
**Then** 列表请求携带映射后的 `lifecycleStatus`（及 approval 时的 `approvalSubState`）  
**And** 结果与 chip 语义一致  
**And** 仍为分页请求（非全量客户端过滤）

### BDD-LRP-C5-007 — Masters 服务端 PageView

**Given** masters 数量大于默认 page size  
**When** 打开 Masters 目录并翻页/筛选  
**Then** `GET /masters` 返回 `PageView`（非裸数组）  
**And** 行为同 BDD-LRP-C5-002/003（参数名按 C5-C4…C5-C6）

### BDD-LRP-C5-008 — Content-modules 服务端 PageView

**Given** content-modules 数量大于默认 page size（或配合 `groupCode`）  
**When** 打开 Content modules 目录并翻页/搜索  
**Then** `GET /content-modules` 返回 `PageView`  
**And** 可选 `groupCode` + `search` + `page`/`size` 生效

### BDD-LRP-C5-009 — 授权 fail-closed

**Given** 会话仅授权组 A  
**When** 请求列表（无 filter 或 filter=组 B）  
**Then** 不出现组 B 实体  
**And** filter=组 B 时为空页（或 403，与既有 group-access 一致且测试锁定）

### BDD-LRP-C5-010 — 筛选变更重置页码

**Given** 操作者在第 2 页（或更高）  
**When** 更改 search / filter / workflow chip / sort  
**Then** 下一次请求 `page=0`（UI 当前页回到 1）  
**And** 展示第一页结果

### BDD-LRP-C5-011 — Page size 边界

**Given** 合法会话  
**When** 请求 `size` 省略 / `size=0` / `size>100` / `page=-1`  
**Then** 服务端按 C5-C2 规范化后返回成功页（不 500）  
**And** 实际 `result.size` 在 1…100

### BDD-LRP-C5-012 — 空结果 vs 加载错误

**Given** 筛选条件无匹配  
**When** 列表返回成功空页  
**Then** 显示空态（EmptyStatePanel），**不是** LoadErrorPanel  
**Given** 服务端 5xx/网络失败  
**When** 加载失败  
**Then** 显示 LoadErrorPanel + Retry（既有）

### BDD-LRP-C5-013 — OpenAPI / 契约同步

**Given** 本切片实现完成  
**When** 检查 `docs/api/openapi-v1.yaml`（及必要的 contract-outline 条目）  
**Then** 三 list 操作文档化 page/size/filter/search/sort 与 `PageView` 响应  
**And** 与运行行为一致

### BDD-LRP-C5-014 — 回归：既有列表 E2E 不因契约升级红灯

**Given** 既有 Playwright 目录/列表相关规格  
**When** 跑 LR-C5 规格 + 受影响回归  
**Then** 全部绿色（或已按 PageView 更新断言）

### BDD-LRP-C5-015 — i18n / OA

**Given** 目录分页与筛选控件可见  
**When** 检查用户可见文案  
**Then** 全部 i18n（en 基座 + zh-CN）；无硬编码  
**And** 双品牌无逻辑分叉

---

## 10. 与 COR-F09 / OPT-F4 / 现状基线的关系

| 来源 | 本切片处理 |
| --- | --- |
| **COR-F09**（Done：group-first） | 语义保留为 **C5-C3**（行分页 + 默认 groupCode 排序）；不恢复按组计数分页 |
| **OPT-F4 / SOR-P01** | Templates 已有 page/size 基线但缺 filter 与 group-first 默认排序；masters/modules 仍全量 — 本切片闭合 residual |
| **前端混合模式**（`serverPagingActive`） | **删除**；筛选不再回退客户端全量切片 |
| **`useCatalogPagination`** | 目录主路径停用；其他面板可继续客户端分页 |
| **LR-C6** | 可复用 `search`；本切片不交付 palette |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 网络 | Playwright / DevTools：list 请求含 `page`/`size`；筛选后仍分页 |
| API | 后端 pageable + filter 单测；`PageView` 字段断言 |
| UI | 分页控件 total；空态/错态分离 |
| 性能 | ≥500 种子；p95 < 1 s 记录（ledger + `frontend/e2e/evidence/`） |
| 契约 | OpenAPI diff 与 BDD-LRP-C5-013 |
| 门禁 | `mvn verify`；`pnpm lint/type-check/test/build`；E2E `LRP-C5-catalog-pagination.spec.ts`；UIUX manifest |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [LRP-C detail § LR-C5](../plan/detail/LRP-C-usability-deepening.md) | 任务单 / G/W/T 来源 |
| [launch-readiness-program.md](../plan/launch-readiness-program.md) | Wave LR-C / p95 目标 |
| [comprehensive-optimization-roadmap.md](../plan/comprehensive-optimization-roadmap.md) COR-F09 | group-first 语义 |
| [optimization-plan.md](../plan/optimization-plan.md) OPT-F4 | 分页 residual |
| [openapi-v1.yaml](../api/openapi-v1.yaml) `PageView` | 分页信封 |
| [requirements-plan.md](../requirements/requirements-plan.md) | 已确认需求条目（本切片同步） |
| [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) | 目录 IA（列表分页注记） |
| permission matrix | catalog browse 授权（不改） |

---

## 13. 非阻塞说明 / 显式延期

| 项 | 状态 |
| --- | --- |
| 用户可选 page size 控件是否暴露 10/20/50 | **非阻塞** — 默认 20 即可；选项为 UX 增强 |
| Content-modules 状态 filter | **v1 不要求**（C5-C6） |
| sort 非法值 400 vs 回退默认 | **已选回退默认**（C5-C8） |
| 无效 status 枚举 422 vs 空页 | **推荐空页**；实现锁定其一即可 |
| LR-C6 palette | **延期**至 C6；不阻塞 C5 |

**无必须阻塞实现的未决问题。**

---

## 14. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/lrp-c5-catalog-pagination.md
task_ids: [LR-C5, lrp-c5-catalog-pagination]
scenario_ids:
  - BDD-LRP-C5-001
  - BDD-LRP-C5-002
  - BDD-LRP-C5-003
  - BDD-LRP-C5-004
  - BDD-LRP-C5-005
  - BDD-LRP-C5-006
  - BDD-LRP-C5-007
  - BDD-LRP-C5-008
  - BDD-LRP-C5-009
  - BDD-LRP-C5-010
  - BDD-LRP-C5-011
  - BDD-LRP-C5-012
  - BDD-LRP-C5-013
  - BDD-LRP-C5-014
  - BDD-LRP-C5-015
```

下一步：`plan-orchestrator` 分解实现任务 → backend-engineer + frontend-engineer（feature worktree 已就位）。
