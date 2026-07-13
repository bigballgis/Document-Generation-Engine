# BDD 行为规格：LR-C7 — In-app notification center (bell + unread)

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-11  
**BDD ID**: `BDD-LRP-C7-NOTIFY-001`  
**来源任务**: [LRP Wave LR-C § LR-C7 — In-app notification center](../plan/detail/LRP-C-usability-deepening.md)  
**程序边界**: [launch-readiness-program.md §0.1](../plan/launch-readiness-program.md) — **in-app only；无 email/IM/webhook**  
**依赖**: P14 collaboration work items + timeout escalations（**Done**）；Dashboard task hub partitions（COR-T11 / `useWorkflowTasks` / `useDashboardTabs`）  
**伴生**: `ManagementShell.vue` header actions；ADR-0021（协作通知仅站内 work items）  
**Task Master / slice**: plan `LR-C7` / slice `lrp-c7-notification-center`  
**Worktree**: `d:\working\DGE-lrp-c7-notification-center` · `feat/lrp-c7-notification-center` · base `2eaab40`

---

## 1. 概述

已登录且具备协作待办可见能力的管理会话，在 **ManagementShell** 顶栏看到 **铃铛（bell）+ 未读角标**；定时轮询未读数；打开下拉列出当前用户可见的 **开放协作待办**（角色队列）与 **超时升级（ESCALATION）**；点击条目 **深链到 Dashboard 任务中心对应分区**；支持单条 / 全部标已读。已读状态 **服务端按用户持久化**（多设备一致）。

数据来源是 P14 既有 `CollaborationWorkItem`（含 `ESCALATION` 队列），**不**新建独立 Notification 领域聚合；仅增加 **投影 API** + **per-user read marker**。

| 行为域 | 摘要 |
| --- | --- |
| **D1 壳铃铛** | 有协作可见权 → 显示铃铛；无权限 → 不渲染（fail-closed） |
| **D2 未读角标** | `unreadCount > 0` 显示数字（封顶展示见 C7-C12）；`0` 隐藏角标，铃铛仍在 |
| **D3 下拉列表** | 打开时拉取通知列表；展示类型/队列、摘要、模板名、相对时间；未读视觉区分 |
| **D4 深链** | 点击条目 → `/dashboard?queue={QUEUE}#tasks-section`；关闭下拉；该项标已读 |
| **D5 标已读** | 单条点击；下拉「Mark all as read」；**仅打开下拉不**批量已读 |
| **D6 轮询** | 默认 **30s** 拉 unread-count；可配置；`document.hidden` 时暂停；可见时立即补拉 |
| **D7 授权** | 与 P14 `CollaborationWorkItemAccessService` 同源可见队列 + 组范围；fail-closed |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| Email / IM / webhook / 推送通道 | **禁止**（§0.1 / ADR-0021） |
| 独立 Notification 领域实体与生命周期 | **禁止** — 投影 open work items + read marker |
| SSE / WebSocket 实时推送 | **禁止** — 仅轮询 |
| Master review / master rework 任务进铃铛 | **v1 Out of scope** — 仅 P14 collaboration（含 ESCALATION） |
| 改变协作 work item 创建/解决语义或 escalation 调度 | Out of scope — 仅消费 OPEN 项 |
| 改变 permission matrix 能力键（除非实现发现缺口） | Out of scope — 复用 `viewCollaborationWorkItems` / escalation 可见性 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **队列操作者** | `TEMPLATE_TESTER` / `TEMPLATE_APPROVER` / `TEMPLATE_AUTHOR`（及具备对应队列可见性的组合） | 看到本角色队列 OPEN 待办 |
| **管理员** | `GROUP_ADMIN` / `GLOBAL_ADMIN` | 额外可见 `ESCALATION`；管理员协作可见性按既有 P14 |
| **无协作权用户** | 不具备 `viewCollaborationWorkItems` | 无铃铛、无通知 API 数据 |
| **系统（UI）** | `ManagementShell` + notification dropdown | 铃铛、角标、轮询、深链、标已读 |
| **系统（API）** | collaboration 投影端点 + read marker | unread-count / list / mark-read |

授权：本规格 **不** 扩大可见面；服务端仍按会话角色队列 ∩ 组范围过滤。客户端隐藏铃铛仅为 UX；权威在服务端。

---

## 3. Goal

1. 操作者在任意管理页一眼看到是否有待处理协作通知（未读角标）。  
2. 打开下拉可浏览条目类型与摘要，点击跳到 Dashboard 正确队列分区。  
3. 标已读后角标下降；多设备 / 刷新后已读状态保持。  
4. 无 email/IM；无 SSE；无独立通知域；i18n English-first；Bank OA；双品牌无逻辑分叉。

---

## 4. 已确认决策（2026-07-11，behavior-spec-author 裁决）

| ID | 决策 |
| --- | --- |
| **C7-C1** | **作用面**：挂载于 `ManagementShell` header-actions（品牌/语言切换附近、用户菜单左侧）；未登录 / 无 shell → 无铃铛。 |
| **C7-C2** | **可见性门控**：仅当会话具备协作待办查看能力（与 `canViewCollaborationWorkItems` / 后端 `requireViewer` 同源）时渲染铃铛；否则 **不渲染**（fail-closed）。 |
| **C7-C3** | **条目来源**：仅 **status = OPEN** 的 collaboration work items，且队列 ∈ 会话 `visibleQueues`，组 ∈ 会话可访问组。含 `TEST` / `APPROVAL` / `REMEDIATION` / `PENDING_RELEASE` / `ESCALATION`（后两者/升级按既有角色规则）。**不含** master-review / master-rework。 |
| **C7-C4** | **未读定义**：对会话用户 U，某 OPEN work item W **未读** 当且仅当：**不存在** U 对 W 的有效已读标记（见 C7-C5）。已 RESOLVED 的项不计入未读、不出现在通知列表。 |
| **C7-C5** | **已读持久化**：服务端 **per-user read marker**（建议表：`user_id` + `work_item_id` + `read_at` UTC；唯一约束 `(user_id, work_item_id)`）。**禁止**仅 localStorage（多设备不一致）。标记 **不** 改变 work item 状态，**不** 等价于 resolve。 |
| **C7-C6** | **投影 API（新端点，非新领域）** — 统一信封；建议路径前缀 `/api/management/v1/collaboration-notifications`：<br>1. `GET …/unread-count` → `{ unreadCount: number }`（仅计未读 OPEN 可见项）<br>2. `GET …`（list）→ 可见 OPEN 项投影列表（含 `read: boolean`），默认按 `createdAt` **降序**，**size 上限 20**（常量可配置，测试锁定）<br>3. `POST …/{workItemId}/read` → 幂等标已读；不可见 / 不存在 → **404 或 403 fail-closed**（实现锁定一种，禁止泄露他组存在性时优先 **404**）<br>4. `POST …/read-all` → 将当前用户所有可见 OPEN 未读项标已读<br>OpenAPI 须在同切片由 doc-keeper/实现同步。 |
| **C7-C7** | **轮询间隔**：默认 **30 秒**（`docgen.ui.notification-poll-interval-ms` 或等价前端配置常量；后端可不强制）。测试可注入更短间隔。 |
| **C7-C8** | **可见性暂停**：`document.visibilityState === 'hidden'`（或 Page Visibility API 等价）时 **停止** 轮询定时器；变为 `visible` 时 **立即** 拉一次 unread-count，再恢复间隔。 |
| **C7-C9** | **拉取策略**：壳挂载后立即拉 unread-count；之后按 C7-C7/C8 轮询。**打开下拉**时拉 list（及可选刷新 unread-count）。下拉打开期间可用同一间隔刷新 list；关闭后仅需 unread-count。 |
| **C7-C10** | **标已读触发**：<br>• **单击条目** → 先（或并行）`POST …/read`，再导航深链，关闭下拉<br>• **「Mark all as read」** 控件（下拉头/底）→ `POST …/read-all`，刷新 list + unread-count<br>• **仅打开下拉 / 仅悬停** → **不**标已读 |
| **C7-C11** | **深链 URL**：`/dashboard?queue={QUEUE}#tasks-section`，其中 `{QUEUE}` 为条目的 `CollaborationWorkItemQueue`（如 `TEST`、`ESCALATION`）。与 `useDashboardTabs.handleTabChange` 的 `query.queue` 及既有 `#tasks-section` 锚点对齐。落地后 Dashboard 激活对应队列分区（`data-partition-id="queue-{QUEUE}"`）。 |
| **C7-C12** | **角标展示**：`unreadCount === 0` → 无角标；`1–99` → 显示数字；`≥100` → 显示 `99+`（或 i18n 等价）。`aria-label` 含未读数（i18n）。 |
| **C7-C13** | **空态 / 零未读**：铃铛仍可见（有协作权时）。下拉：无 OPEN 可见项 → empty i18n（如「No notifications」）；有已读 OPEN 项但仍 `unreadCount=0` → 列表展示已读项（弱化样式）+ 无角标。无「Mark all」当 `unreadCount=0`（禁用或隐藏，测试锁定一种；推荐 **隐藏**）。 |
| **C7-C14** | **列表行内容**：主文案 = `summaryText`（或队列/触发 i18n 回退）；副文案 = 模板名 + 队列类型标签；相对年龄可用既有 `ageSeconds` / locale formatter。`data-testid`：`notification-bell`、`notification-badge`、`notification-dropdown`、`notification-item`、`notification-mark-all`。 |
| **C7-C15** | **错误态**：unread-count / list 失败 → 不静默冒充 0（角标可保留上次成功值或显示错误指示，测试锁定；推荐 **保留上次成功 unread + 下拉内 inline 错误/重试**）。Mark-read 失败 → 不导航假装成功；toast/inline i18n。 |
| **C7-C16** | **通道禁令**：禁止 email/IM/webhook/SSE/WebSocket；禁止为通知引入第三方推送 SDK。 |
| **C7-C17** | **E2E + UIUX 强制**：`frontend/e2e/LRP-C7-notification-center.spec.ts`；manifest `frontend/e2e/evidence/LRP-C7-uiux-manifest.md`（双品牌）。 |
| **C7-C18** | **后端门禁**：本切片 **需要** `mvn -B -ntp -f backend/pom.xml verify`（新投影 API + Flyway read marker）。 |
| **C7-C19** | **权限矩阵**：复用既有 collaboration work-item 查看能力；**不**新增独立「notification」权限键（除非实现期架构评审要求显式键 — 默认不新增）。 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | Shell 挂载 / 会话就绪 | 首次 unread-count；启动轮询 |
| T2 | 轮询定时器到期（页可见） | 刷新 unread-count |
| T3 | 页从 hidden → visible | 立即补拉 unread-count |
| T4 | 用户点击铃铛 | 打开/关闭下拉；打开时拉 list |
| T5 | 用户点击条目 | 标已读 + 深链 + 关闭 |
| T6 | 用户点击 Mark all | read-all + 刷新 |
| T7 | 用户 Esc / 点击外侧 | 关闭下拉（不标已读） |

---

## 6. Preconditions

- 用户已登录；ManagementShell 已挂载。  
- P14 collaboration work items API 与 escalation 数据可用；种子/夹具可创建 OPEN 队列项与 ESCALATION。  
- Docker 验收：`http://localhost:4173` + `http://localhost:8080`（Playwright）。  
- Dashboard `#tasks-section` 与 `?queue=` 分区行为可用（COR-T11）。

---

## 7. Primary journey（成功路径）

1. 具备 `TEMPLATE_TESTER`（或可见 TEST 队列）的操作者登录；存在一条 OPEN `TEST` 队列待办（其组在会话范围内）。  
2. Shell 拉 unread-count → 角标 ≥1。  
3. 操作者点击铃铛；下拉列出该条目及类型。  
4. 操作者点击该条目 → 该项标已读；导航至 `/dashboard?queue=TEST#tasks-section`；任务中心显示 `queue-TEST` 分区；角标递减（若无其它未读则消失）。  
5. 刷新或其它设备同用户 → 该项保持已读；铃铛行为一致。

---

## 8. 系统响应

### 成功

- 铃铛可见；角标与 unread-count 一致；下拉条目与投影 list 一致。  
- 深链后 URL 含正确 `queue` 与 `#tasks-section`；分区可见。  
- 已读标记写入服务端；后续 unread-count 反映变更。  
- 文案全部 i18n。

### 失败 / 边界

| 条件 | 响应 |
| --- | --- |
| 无协作查看权 | 无铃铛；通知 API 403/denied（fail-closed） |
| 他组 / 不可见队列 OPEN 项 | 不计入 unread、不出现在 list |
| 页签 hidden | 不发起轮询请求 |
| list/unread 网络失败 | 下拉错误态；不把失败当成「无通知」成功空态（与 C7-C15） |
| 对不可见 workItemId mark-read | 404（推荐）或 403；无副作用 |
| RESOLVED 项 | 不出现在通知投影 |
| 未登录 | 无 shell → 无铃铛 |

---

## 9. 验收场景（Given / When / Then）

### BDD-LRP-C7-001 — 开放角色队列待办显示未读角标（任务单验收 #1）

**Given** 操作者具备可见 `TEST`（或任一角色）队列的协作权限  
**And** 存在一条 OPEN collaboration to-do，队列属于该用户可见队列且组在范围内，且用户对其无已读标记  
**When** ManagementShell 挂载并完成首次 unread-count 轮询  
**Then** 铃铛可见（`data-testid="notification-bell"`）  
**And** 未读角标显示 ≥1（`notification-badge`）  
**When** 用户打开下拉  
**Then** 列表包含该条目，并展示其类型/队列信息

### BDD-LRP-C7-002 — 点击条目深链到任务中心分区并标已读（任务单验收 #2）

**Given** 下拉中存在未读条目，队列为 `Q`（如 `TEST`）  
**When** 用户点击该条目  
**Then** 发出对该 `workItemId` 的 mark-read（成功）  
**And** 导航至 `/dashboard?queue=Q#tasks-section`  
**And** Dashboard 任务区分区 `queue-Q` 可见/激活  
**And** 下拉关闭  
**And** unread-count / 角标按剩余未读更新

### BDD-LRP-C7-003 — 仅打开下拉不标已读

**Given** 存在 ≥1 未读  
**When** 用户打开下拉后 Esc（或点击外侧）关闭，未点击任何条目、未点 Mark all  
**Then** unread-count 不变  
**And** 服务端无新增该用户对这些项的已读标记

### BDD-LRP-C7-004 — Mark all as read

**Given** 用户有 ≥2 条未读可见 OPEN 项  
**When** 用户打开下拉并点击「Mark all as read」  
**Then** 调用 read-all 成功  
**And** unread-count 变为 0；角标隐藏  
**And** 列表项均呈已读态（若仍 OPEN）或按 list 刷新结果展示

### BDD-LRP-C7-005 — 轮询默认 30s 且 hidden 暂停

**Given** Shell 已挂载且页可见  
**When** 观察 unread-count 请求间隔  
**Then** 默认间隔为 **30s**（允许测试注入覆盖）  
**When** 将 document 置为 hidden  
**Then** 在 hidden 期间 **不再** 发出轮询请求  
**When** 恢复 visible  
**Then** **立即** 发出一次 unread-count，之后恢复间隔

### BDD-LRP-C7-006 — 超时升级（ESCALATION）对管理员可见

**Given** 会话为 `GROUP_ADMIN` 或 `GLOBAL_ADMIN`（具备 escalation 可见性）  
**And** 存在 OPEN `ESCALATION` work item（组范围内）且未读  
**When** 拉取 unread-count / list  
**Then** 计入未读并出现在下拉  
**When** 点击该条目  
**Then** 深链 `/dashboard?queue=ESCALATION#tasks-section`

### BDD-LRP-C7-007 — 无 escalation 权的角色看不到 ESCALATION 项

**Given** 会话为 `TEMPLATE_TESTER`（无 admin escalation 可见性）  
**And** 系统中存在 OPEN `ESCALATION` 项  
**When** 拉取通知 list / unread-count  
**Then** 该 ESCALATION 项不出现、不计入未读  
**And** DOM 不泄露其 `workItemId` / 模板标识

### BDD-LRP-C7-008 — 跨组 fail-closed

**Given** 会话仅授权组 A  
**And** 组 B 存在 OPEN 可见队列类型待办  
**When** 拉取通知  
**Then** 组 B 项不出现、不计入未读

### BDD-LRP-C7-009 — 无协作权用户无铃铛

**Given** 会话不具备 collaboration work-item 查看能力  
**When** ManagementShell 渲染  
**Then** 不存在 `notification-bell`  
**And** 若直接调用通知 API → 被拒绝（403/denied envelope）

### BDD-LRP-C7-010 — 零未读与空列表 UX

**Given** 用户有协作权且 unread-count = 0，且无 OPEN 可见项  
**When** 打开下拉  
**Then** 显示 empty i18n 文案（非错误面板冒充）  
**And** 无角标  
**And** 「Mark all as read」隐藏（或禁用，按 C7-C13）

### BDD-LRP-C7-011 — 已读状态服务端持久化（多设备语义）

**Given** 用户 U 在设备/会话 A 将 work item W 标已读  
**When** 同一用户 U 在新会话（刷新或另一客户端）拉取 unread-count / list  
**Then** W 对 U 为已读（不计入未读；list 中 `read=true` 或不再以未读样式展示）

### BDD-LRP-C7-012 — RESOLVED 项退出通知投影

**Given** work item W 曾对用户未读，随后被 resolve（OPEN → RESOLVED）  
**When** 拉取 unread-count / list  
**Then** W 不出现在 list  
**And** 不计入 unread-count（无论是否曾有已读标记）

### BDD-LRP-C7-013 — 列表上限与排序

**Given** 用户可见 OPEN 项 > 20  
**When** 打开下拉拉取 list  
**Then** 返回至多 **20** 条，按 `createdAt` 降序  
**And** unread-count 仍可为全部未读总数（可 > 20）

### BDD-LRP-C7-014 — 网络失败不静默空成功

**Given** unread-count 或 list 请求失败（5xx / 网络中断，测试可 mock）  
**When** 用户打开下拉或轮询发生失败  
**Then** 展示错误/重试态（i18n）  
**And** **不得**用「No notifications」空成功态冒充

### BDD-LRP-C7-015 — 无 email/IM/SSE

**Given** 本切片实现与配置  
**When** 审查交付物与网络行为  
**Then** 无 outbound email/IM/webhook 集成  
**And** 无 SSE/WebSocket 用于通知中心  
**And** 仅 HTTP 轮询投影 API

### BDD-LRP-C7-016 — i18n / OA / 双品牌

**Given** 铃铛、角标 aria、下拉空态/错误/Mark all、队列类型标签  
**When** 检查用户可见文案  
**Then** 全部走 i18n（en 基座 + zh-CN 键存在）；无硬编码散落  
**And** REDBC/GREENBC 无逻辑分叉

### BDD-LRP-C7-017 — Mark-read 对不可见 ID fail-closed

**Given** 用户已知或猜测他组 / 不可见 `workItemId`  
**When** 调用 `POST …/{workItemId}/read`  
**Then** 返回 404（推荐）或 403  
**And** 不创建有效已读标记、不泄露资源详情

---

## 10. 与 P14 / Dashboard / Shell 的关系

| 来源 | 本切片处理 |
| --- | --- |
| **P14 OPEN work items + ESCALATION** | 唯一通知数据源；投影 + read marker |
| **`CollaborationWorkItemAccessService`** | 可见队列与访问门控权威 |
| **Dashboard `?queue=` + `#tasks-section`** | 深链目标；不新建任务中心路由 |
| **`ManagementShell` header-actions** | 铃铛挂载点（邻接 locale / user menu） |
| **Master review/rework** | 不进入铃铛（C7-C3） |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | Playwright：角标、下拉、深链、标已读、空态、权限隐藏铃铛 |
| 网络 | unread-count / list / read / read-all；轮询间隔与 hidden 暂停；无 SSE |
| API | 后端单测 + MockMvc：投影过滤、已读幂等、跨组 fail-closed |
| DB | Flyway read-marker 表；无独立 notification 业务表（除 marker） |
| 门禁 | `mvn verify`；`pnpm -C frontend lint && type-check && test && build`；E2E `LRP-C7-notification-center.spec.ts`；UIUX manifest |
| 通道 | 无 email/IM/webhook 代码路径 |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [LRP-C detail § LR-C7](../plan/detail/LRP-C-usability-deepening.md) | 任务单 / G/W/T / Do NOT / E2E 路径 |
| [launch-readiness-program.md §0.1](../plan/launch-readiness-program.md) | in-app only 边界 |
| [P14 detail](../plan/detail/P14-confirmed-large-domains.md) | work items + escalation 源 |
| ADR-0021 | 站内协作通知；拒绝 email/IM v1 |
| `frontend/src/api/collaboration.ts` | 既有 list API（通知投影另增端点） |
| `frontend/src/composables/useWorkflowTasks.ts` / `useDashboardTabs.ts` | 分区与 `?queue=` |
| `frontend/src/components/layout/ManagementShell.vue` | 挂载点 |
| permission matrix | collaboration view（不改键，C7-C19） |
| i18n English-first / frontend OA design | 文案与视觉 |

---

## 13. E2E / 实现提示

| 项 | 值 |
| --- | --- |
| Playwright | `frontend/e2e/LRP-C7-notification-center.spec.ts` |
| UIUX manifest | `frontend/e2e/evidence/LRP-C7-uiux-manifest.md` |
| 建议 Vitest | 轮询/暂停、角标封顶、门控隐藏、深链构造、mark-read 触发矩阵 |
| 建议 Backend | Service/Controller/Access + Flyway marker + 幂等 read |
| Owner | **backend-engineer** + **frontend-engineer** |

**实现顺序建议（非阻塞 ready）**：Flyway marker → 投影 API TDD → Shell 铃铛/轮询 → E2E。

---

## 14. Open questions

**无。** 下列曾模糊点均已用银行 OA 安全默认裁决并写入 §4：

| 原模糊点 | 裁决 |
| --- | --- |
| 未读定义 | C7-C4（OPEN + 无 per-user marker） |
| 已读持久化 | C7-C5（服务端 marker） |
| 轮询间隔 | C7-C7（**30s**） |
| 标已读触发 | C7-C10（条目点击 + Mark all；开下拉不标） |
| 条目类型 | C7-C3（协作 OPEN 队列 + ESCALATION；无 master） |
| 深链 | C7-C11（`/dashboard?queue={QUEUE}#tasks-section`） |
| 空/零未读 UX | C7-C13 |
| 授权 | C7-C2 / C7-C8 场景 + P14 access |

**显式 defer（不阻塞 ready）**

| 项 | 说明 |
| --- | --- |
| Master review 进铃铛 | 后续切片；本切片禁止 |
| 下拉内「仅未读」过滤开关 | v1 不做；列表混合已读/未读样式区分即可 |
| 点击条目是否同时打开模板详情 | v1 仅任务中心分区；模板行内跳转沿用 Dashboard 既有行为 |

---

## 15. BDD readiness

| 字段 | 值 |
| --- | --- |
| **bdd_readiness** | **ready** |
| **owning_doc** | `docs/behavior/lrp-c7-notification-center.md` |
| **task_ids** | `LR-C7`, `lrp-c7-notification-center` |
| **backend_new_endpoint** | **yes** — collaboration-notifications 投影 + read marker（非独立通知域） |
| **next** | `plan-orchestrator` → backend-engineer + frontend-engineer（TDD） |

---

## 16. doc-keeper follow-ups（实现切片内）

| # | 跟进 | 时机 |
| --- | --- | --- |
| DK-1 | 将 C7-C6 四端点写入 `docs/api/openapi-v1.yaml` + 再生 `openapi-v1.ts` | 实现 API 时 |
| DK-2 | 若架构评审要求显式 permission 行，更新 `docs/security/permission-matrix.md`（默认复用 collaboration view） | 仅当需要时 |
| DK-3 | 计划层激活/Done 时由 `plan-orchestrator` / `post-task-doc-sync` 更新 LRP-C 行与 ledger | 非本 BDD 阻塞 |
