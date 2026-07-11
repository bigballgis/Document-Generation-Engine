# BDD 行为规格：LR-C6 — Global command palette (Ctrl+K / Cmd+K)

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-11  
**BDD ID**: `BDD-LRP-C6-PALETTE-001`  
**来源任务**: [LRP Wave LR-C § LR-C6 — Global search / command palette (Ctrl+K)](../plan/detail/LRP-C-usability-deepening.md)  
**程序发现**: [launch-readiness-program.md](../plan/launch-readiness-program.md) Wave LR-C / §1 finding 10  
**依赖规格**: [LR-C5 catalog server-side pagination/filter](./lrp-c5-catalog-pagination.md)（`search` + `PageView` + 授权 scoped list）  
**伴生**: LR-C12 keyboard/a11y 模式（焦点陷阱 / Esc）；ManagementShell + `visibleRoutes`  
**Task Master / slice**: plan `LR-C6` / slice `lrp-c6-command-palette` — **Task Master #32** (`in-progress`)  
**Worktree**: `D:/working/DGE-lrp-c6-command-palette` · `feat/lrp-c6-command-palette`

---

## 1. 概述

管理端已登录会话可在 **任意 ManagementShell 页面** 用 **Ctrl+K（Windows/Linux）或 Cmd+K（macOS）** 打开全局命令面板，搜索并跳转到：

1. **会话授权范围内** 的 Templates / Masters / Content modules（经 **LR-C5 既有 list `search`**）；  
2. **`visibleRoutes` 允许的导航路由**（客户端门控，fail-closed）。

面板支持键盘 ↑↓ / Enter / Esc；结果不得泄露未授权实体或不可见路由；**不**新增统一搜索后端端点；**不**引入重型客户端搜索依赖。

| 行为域 | 摘要 |
| --- | --- |
| **D1 打开/关闭** | Ctrl+K / Cmd+K 打开；Esc / 遮罩点击关闭；关闭后焦点回到触发前焦点 |
| **D2 实体搜索** | 非空查询 → 对已授权目录并发调用 C5 list（`search` + `page=0` + 小 `size`） |
| **D3 路由条目** | 仅列出 `session.visibleRoutes` ∩ 已知管理导航键；可按标签/路径片段过滤 |
| **D4 键盘导航** | ↑↓ 高亮；Enter 导航并关闭；焦点困在面板内 |
| **D5 授权 fail-closed** | 不可见路由不出现；无目录路由权限不发起该目录 API；服务端仍 scoped |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 新的统一 `/search` 或跨实体聚合后端 API | **禁止** — 复用 C5 三 list 的 `search`（见 **C6-C3**） |
| Fuse.js / Algolia / 其他重型搜索 SDK | **禁止**（任务单 Do NOT） |
| Recent-items / 本地「最近访问」记忆 | **v1 Out of scope**（见 **C6-C8**）— 验收未要求；避免陈旧缓存越权风险 |
| Audit / Identity / API credentials / collaboration 任务实体搜索 | Out of scope — 仅三目录 + 导航路由 |
| 改变权限矩阵、`visibleRoutes` 计算或 C5 search 语义 | Out of scope — 仅消费 |
| LR-C7 通知中心 / LR-C8 | Out of scope |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **管理操作者** | 任意已登录管理会话（具备 ≥1 条 `visibleRoutes`） | 在任意管理页唤起 palette 并导航 |
| **受限角色** | 例如无 `route.content-module-management` 或仅授权组 A | 用于 fail-closed 验收 |
| **系统（UI）** | `ManagementShell` + palette 组件 | 快捷键、结果合并、键盘导航、路由跳转 |
| **系统（API）** | 既有 C5 list endpoints | 按会话组/角色返回授权范围内的 `PageView` |

授权：本规格 **不** 改变 permission matrix。实体可见性完全依赖 C5 服务端 scoped list；路由可见性依赖会话 `visibleRoutes`（与导航同源）。

---

## 3. Goal

1. 任意管理页一键打开命令面板，输入模板 **code/name 片段** 即可定位并 Enter 进入其包枢纽/详情。  
2. 无模块（或组）访问权时，搜索不得出现该实体/目录结果（fail-closed）。  
3. 可用键盘完成打开 → 搜索 → 选择 → 关闭全流程；a11y 达标（dialog + 焦点）。  
4. 网络仅使用 C5 `search` 参数；无新后端契约；无重型搜索依赖。  
5. i18n English-first；Bank OA；双品牌无逻辑分叉。

---

## 4. 已确认决策（2026-07-11，behavior-spec-author 裁决）

| ID | 决策 |
| --- | --- |
| **C6-C1** | **作用面**：挂载于 `ManagementShell`（已登录管理壳）；未登录 / 无 shell → 无 palette。 |
| **C6-C2** | **打开快捷键**：Windows/Linux `Ctrl+K`；macOS `Meta+K`（Cmd+K）。在 shell 文档级（或等价）监听；`preventDefault` 避免浏览器默认行为。面板已打开时再次按下 → **保持打开并将焦点移回搜索框**（不关闭）。 |
| **C6-C3** | **后端**：**不**新增端点。实体结果来自既有：`GET /api/management/v1/templates`、`/masters`、`/content-modules`，参数对齐 C5：**`search=<query>`**、`page=0`、`size` 见 C6-C6；可选省略其它 filter（默认 sort 即可）。 |
| **C6-C4** | **空查询**：trim 后为空 → **仅展示可导航路由条目**（`visibleRoutes`）；**不**发三目录 list 请求。 |
| **C6-C5** | **目录门控（客户端）**：仅当对应 route key ∈ `visibleRoutes` 时才请求该目录：`route.template-management` → templates；`route.master-management` → masters；`route.content-module-management` → content-modules。缺省路由 → 不请求、不渲染该分组。 |
| **C6-C6** | **每目录结果上限**：`size=8`（实现可配置常量；须测试锁定）。UI 按分组展示；总列表为「路由匹配 ∪ 各目录 content」。 |
| **C6-C7** | **防抖 + 取消**：输入防抖 **250 ms**；新查询 / 关闭 / 卸载时 **AbortSignal** 取消进行中请求；过期响应不得覆盖更新查询。 |
| **C6-C8** | **Recent-items**：**v1 不做**。任务单标注 optional；为降低会话角色变更后的陈旧泄露面与范围，本切片明确 defer。若后续切片加入，必须按 ID 再校验授权后再展示。 |
| **C6-C9** | **导航目标**：Template → `templatePackageHubPath(id)`（`/templates/{id}`）；Master → `masterDetailPath(id)`；Content module → `contentModuleDetailPath(id)`；Route → `pathForRouteKey(routeKey)`。Enter 或点击后 **关闭 palette** 并 `router.push`。 |
| **C6-C10** | **结果展示**：主标题 = 实体 `name` 或路由 i18n 标签；副文案 = 模板 `externalId`（及可选 `groupCode`）/ 模块 `moduleCode` / master `groupCode` / 路由 path。分组标题 i18n（Routes / Templates / Masters / Content modules）。 |
| **C6-C11** | **路由过滤**：空查询列出全部可见导航键（去重 legacy→dashboard 映射后的当前键，与侧栏一致）；非空查询对 **已解析 i18n 标签** 与 **path** 做不区分大小写 contains（纯客户端，无网络）。 |
| **C6-C12** | **键盘**：`ArrowDown` / `ArrowUp` 在扁平结果列表中循环或钳制高亮（实现任选，须测试锁定；推荐 **钳制不循环**）；`Enter` 激活高亮项；无结果时 Enter 无导航；`Escape` 关闭。 |
| **C6-C13** | **焦点 / a11y**：打开时焦点落入搜索框；`role="dialog"` + `aria-modal="true"` + 可访问名称（i18n）；焦点陷阱于面板；关闭后焦点回到打开前的 activeElement（若仍在文档中）。建议 `data-testid="command-palette"` / `command-palette-input` / `command-palette-option`。 |
| **C6-C14** | **关闭方式**：Esc；点击遮罩；成功导航后。 |
| **C6-C15** | **错误**：任一目录请求失败 → 该分组显示 inline 错误/重试或统一 palette 级错误文案（i18n）；**不得**用「无结果」冒充失败；其它成功分组仍可展示。 |
| **C6-C16** | **无匹配**：查询无路由命中且各目录空页 → 显示 empty/no-match 文案（i18n），**不是** LoadErrorPanel。 |
| **C6-C17** | **依赖禁令**：禁止新增 Fuse/Algolia 等；路由过滤用简单字符串匹配即可。 |
| **C6-C18** | **E2E + UIUX 强制**：`frontend/e2e/LRP-C6-command-palette.spec.ts`；manifest `frontend/e2e/evidence/LRP-C6-uiux-manifest.md`。 |
| **C6-C19** | **与编辑器快捷键**：palette 为 shell 全局；若焦点在需保留 Ctrl/Cmd+K 的第三方控件（当前无），本切片不特殊豁免——管理端统一占用 Ctrl/Cmd+K。结构编辑器 undo 使用 Z/Y，不冲突。 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 用户按下 Ctrl+K / Cmd+K | 打开 palette（或聚焦搜索框） |
| T2 | 用户在搜索框输入（防抖后） | 刷新路由过滤 + 条件触发 C5 list |
| T3 | 用户 ↑↓ / Enter / 点击结果 | 高亮或导航 |
| T4 | 用户 Esc / 点击遮罩 / 导航成功 | 关闭 palette |
| T5 | 会话 `visibleRoutes` 变化（罕见） | 下次打开/下次查询按新门控 |

---

## 6. Preconditions

- 用户已登录；ManagementShell 已挂载。  
- LR-C5 list `search` 行为可用（服务端含授权 scoped）。  
- Docker 验收：`http://localhost:4173` + `http://localhost:8080`（Playwright）。  
- 种子数据中存在可搜索的授权模板（含可区分的 `externalId` / name 片段）；受限角色夹具可用于 fail-closed。

---

## 7. Primary journey（成功路径）

1. 操作者在任意管理页（如 Dashboard）按下 **Ctrl+K**（或 Cmd+K）。  
2. Palette 打开；焦点在搜索框；可见路由列表（空查询）。  
3. 操作者输入授权模板的 code/name 片段；防抖后发出带 `search=` 的 templates（及有权限的其它目录）list 请求。  
4. 面板列出匹配模板（及路由）；操作者 ↓ 选中模板，**Enter**。  
5. 路由跳转到 `/templates/{id}`；palette 关闭；焦点回到页面主内容上下文。

---

## 8. 系统响应

### 成功

- Palette 可见；结果分组正确；Enter 后 URL 为目标路径。  
- 网络：仅对门控通过的目录发出 `page=0&size=8&search=…`（空查询无 list）。  
- UI 文案全部 i18n。

### 失败 / 边界

| 条件 | 响应 |
| --- | --- |
| 角色无 content-modules 路由 | 不请求 `/content-modules`；结果中无该分组/实体 |
| 他组实体名/code | C5 返回空或不含该行；面板不展示 |
| 查询无匹配 | Empty/no-match i18n |
| 目录 API 5xx/网络失败 | 该分组错误态；不静默当无结果 |
| 未登录 | 无 shell → 无快捷键行为（既有） |
| 面板打开时页面仍可部分交互 | 遮罩拦截；焦点陷阱 |

---

## 9. 验收场景（Given / When / Then）

### BDD-LRP-C6-001 — 任意管理页 Ctrl+K 打开

**Given** 操作者已登录且 ManagementShell 可见（任意管理路由，如 `/dashboard`）  
**When** 按下 Ctrl+K（Windows/Linux 夹具）或 Cmd+K（macOS 夹具）  
**Then** 命令面板打开（`data-testid="command-palette"` 可见）  
**And** 焦点在搜索输入框  
**And** 默认展示当前会话 `visibleRoutes` 对应的导航条目（空查询，无三目录 list 请求）

### BDD-LRP-C6-002 — 模板片段搜索 + Enter 导航（任务单验收 #1）

**Given** 操作者具备 templates catalog 访问权  
**And** 存在授权模板，其 `externalId` 或 `name` 含可区分片段 `T`  
**And** 操作者处于任意管理页  
**When** 打开 palette，输入 `T`，等待防抖与结果  
**Then** 面板结果中出现该模板  
**When** 高亮该模板并按 Enter（或点击）  
**Then** 导航至 `templatePackageHubPath(templateId)`（`/templates/{id}`）  
**And** palette 关闭

### BDD-LRP-C6-003 — 无模块访问权 fail-closed（任务单验收 #2）

**Given** 会话 **没有** `route.content-module-management`（或等价：对该 content-module 无组授权）  
**And** 系统中存在名称可搜的 content-module `M`（对其他角色可见）  
**When** 操作者打开 palette 并搜索 `M` 的名称片段  
**Then** 结果中 **不出现** 该 content-module  
**And** **不**发出未授权目录的 list 请求（无路由门控时）**或** 发出后服务端空页且 UI 不展示行（组授权场景）  
**And** 不得通过 DOM/可访问名泄露 `M` 的标识

### BDD-LRP-C6-004 — 键盘 ↑↓ 与 Enter

**Given** palette 已打开且结果 ≥2 条  
**When** 按 ArrowDown / ArrowUp  
**Then** 高亮项按 C6-C12 移动（可观测 `aria-selected` 或等价 testid）  
**When** 在高亮项上按 Enter  
**Then** 导航到该条目目标并关闭 palette

### BDD-LRP-C6-005 — Esc 关闭并恢复焦点

**Given** 打开 palette 前焦点在可识别控件（如主内容区链接/按钮）  
**When** 打开 palette 后按 Escape  
**Then** palette 关闭  
**And** 焦点回到打开前的元素（若仍挂载）

### BDD-LRP-C6-006 — 空查询与无匹配

**Given** palette 已打开  
**When** 搜索框为空（或仅空白）  
**Then** 仅路由条目（按 visibleRoutes）；无 templates/masters/content-modules list 请求  
**When** 输入保证无命中的查询串  
**Then** 显示 no-match / empty 文案（i18n）  
**And** 不是错误面板冒充

### BDD-LRP-C6-007 — 可导航路由跳转

**Given** 会话 `visibleRoutes` 含 `route.template-management`  
**When** 打开 palette（空查询或输入匹配「Templates」标签/路径的片段）并选择 Templates 路由条目后 Enter  
**Then** 导航至 `/templates`  
**And** palette 关闭  
**And** 不在 `visibleRoutes` 中的路由键 **永不** 出现在列表中

### BDD-LRP-C6-008 — 网络契约复用 C5 search

**Given** 操作者有 templates 路由权限且输入非空查询 `Q`  
**When** 防抖完成  
**Then** 至少发出 `GET …/templates?page=0&size=8&search=Q`（参数名与 C5 一致；size 锁定为 8）  
**And** **不**存在新的聚合 search 端点调用  
**And** 无 Fuse/Algolia 等第三方搜索脚本依赖（构建/包审计可抽查）

### BDD-LRP-C6-009 — 多目录门控

**Given** 会话仅有 `route.template-management` + dashboard，**无** masters / content-modules 路由  
**When** 输入非空查询  
**Then** 仅请求 templates list  
**And** 不请求 masters / content-modules

### BDD-LRP-C6-010 — 跨组实体不泄露

**Given** 会话仅授权组 A；组 B 存在名称含片段 `X` 的模板  
**When** 搜索 `X`  
**Then** 结果不含组 B 模板（依赖 C5 授权 scoped；E2E 用夹具断言）

### BDD-LRP-C6-011 — a11y dialog

**Given** palette 打开  
**When** 检查可访问树  
**Then** 存在 `role="dialog"`（或等价模式对话框）且 `aria-modal="true"`  
**And** 有可访问名称（i18n）  
**And** Tab 焦点不逃逸到遮罩后页面控件（焦点陷阱）

### BDD-LRP-C6-012 — i18n / OA

**Given** palette 打开（含空态、无匹配、分组标题、占位符）  
**When** 检查用户可见文案  
**Then** 全部走 i18n（en 基座 + zh-CN 键存在）；无硬编码中文/英文散落  
**And** REDBC/GREENBC 无逻辑分叉

### BDD-LRP-C6-013 — 目录请求失败不静默

**Given** templates list 对 search 返回 5xx（或网络中断，测试可 mock）  
**When** 查询触发该请求  
**Then** 展示错误态（i18n），**不是**「无匹配」空态冒充成功  
**And** 若其它分组成功，仍可显示其它分组结果

### BDD-LRP-C6-014 — 遮罩点击关闭

**Given** palette 已打开  
**When** 用户点击遮罩（dialog 外）  
**Then** palette 关闭

### BDD-LRP-C6-015 — Masters / Content-modules 对称导航（有权限时）

**Given** 会话具备对应 catalog 路由且存在可搜授权实体  
**When** 搜索并 Enter 选中 master 或 content-module  
**Then** 分别导航至 `masterDetailPath(id)` 或 `contentModuleDetailPath(id)`  
**And** palette 关闭

---

## 10. 与 LR-C5 / shell 的关系

| 来源 | 本切片处理 |
| --- | --- |
| **LR-C5 `search` + PageView** | 唯一实体数据源；语义含 C5-C5 contains 字段集与 D4 授权 |
| **`visibleRoutes` / ManagementShell nav** | 路由条目与目录门控的唯一客户端授权源 |
| **LR-C12 a11y** | 对齐 dialog/焦点陷阱模式；不重复改造其它表面 |
| **Recent-items（任务单 optional）** | C6-C8 明确 v1 不做 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | Playwright：打开/搜索/Enter/Esc/焦点；`data-testid` |
| 网络 | list 请求含 `search`/`page`/`size`；无新聚合 API；门控下无多余目录请求 |
| 授权 | 受限角色夹具：无模块结果 / 无他组实体 |
| a11y | dialog / aria-modal / 焦点陷阱（E2E 或 axe 抽查） |
| 门禁 | `pnpm -C frontend lint && type-check && test && build`；E2E `LRP-C6-command-palette.spec.ts`；UIUX manifest；若仅前端则 backend verify 非强制（无新 API） |
| 包 | 无新增重型搜索依赖 |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [LRP-C detail § LR-C6](../plan/detail/LRP-C-usability-deepening.md) | 任务单 / G/W/T / Do NOT / E2E 路径 |
| [launch-readiness-program.md](../plan/launch-readiness-program.md) | Wave LR-C / finding 10 |
| [lrp-c5-catalog-pagination.md](./lrp-c5-catalog-pagination.md) | `search` 语义与授权 scoped list |
| `frontend/src/components/layout/ManagementShell.vue` | 挂载点 |
| `frontend/src/routing/routeKeys.ts` | `visibleRoutes` 路径 / detail path helpers |
| permission matrix | catalog browse / route 可见性（不改） |
| i18n English-first skill | 文案约定 |

---

## 13. E2E / 实现提示

| 项 | 值 |
| --- | --- |
| Playwright | `frontend/e2e/LRP-C6-command-palette.spec.ts` |
| UIUX manifest | `frontend/e2e/evidence/LRP-C6-uiux-manifest.md` |
| 建议 Vitest | palette 组件：打开快捷键、防抖请求、门控、键盘、fail-closed fixtures |
| Owner | frontend-engineer（**无** backend-engineer，除非实现期发现 C5 缺口——当前规格认定 **不需要**） |

---

## 14. Open questions

**无。** 任务单验收两点 + 键盘/空态/路由/授权均可由 LRP-C detail、C5 既有行为与本规格 C6-C* 裁决覆盖。Recent-items 已明确 defer（C6-C8），不阻塞 `ready`。

---

## 15. BDD readiness

| 字段 | 值 |
| --- | --- |
| **bdd_readiness** | **ready** |
| **owning_doc** | `docs/behavior/lrp-c6-command-palette.md` |
| **task_ids** | `LR-C6`, `lrp-c6-command-palette` |
| **backend_new_endpoint** | **no** — 复用 C5 三 list 的 `search`（C6-C3） |
| **next** | `plan-orchestrator` → frontend-engineer（TDD + E2E） |
