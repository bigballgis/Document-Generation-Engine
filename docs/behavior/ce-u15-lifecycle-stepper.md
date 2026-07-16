# BDD 行为规格：CE-U15 — 生命周期 Stepper + checklist 深链

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U15-LSS`  
**编写日期:** 2026-07-17  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U15  
**Slice:** `ce-u15-lifecycle-stepper`  
**Task Master:** **#91**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u15-lifecycle-stepper` · `feat/ce-u15-lifecycle-stepper`  
**完成声明约束:** 关闭「模板开发工作区缺少状态机 Stepper」与「发布 gate pending 项无法一键前往修复面」缺口；复用既有 lifecycleStatus / publish-gate API 与 U14 深链约定；**不**实现 CE-U16 创作路径压缩；**不**宣称 go-live；**不**激活 CD-3 / P3

---

## 1. 概述

U14 已让 Dashboard Tasks 三类生命周期待办一键落到 testing / approval / publishReadiness。作者与发布负责人进入模板 **dev workspace** 后，仍主要依赖 StatusBadge + 角色旅程卡判断进度；发布 readiness 清单（`TemplateDetailApprovalPublishPane` / publish summary）对 `ready=false` 项仅展示 Pending 标签，**无「前往修复」深链**。CE 计划卡 CE-U15 要求：顶栏状态机 Stepper + 发布 gate 每条 pending 项可深链到修复面。

| 缺口（现状证据） | 目标 |
| --- | --- |
| 无 lifecycle Stepper DOM（U14 E2E 显式断言 0） | 顶栏展示状态机 Stepper，反映当前 `lifecycleStatus`（+ `approvalSubState`） |
| publish gate 列表只有 Ready/Pending/Informational 标签 | 每条 **pending**（`ready=false`）且可映射的检查项提供 **Go fix** 深链 |
| 作者需手动猜 Tab 修阻断项 | 单次点击落到对应 workspaceTab / 子 Tab |

| 行为域 | 摘要 |
| --- | --- |
| **LSS-01 顶栏 Stepper** | 产品状态线性 Stepper；高亮当前步；已过/未到可视化；只读导向（无 CTA 动作栏） |
| **LSS-02 Publish-gate Go fix** | pending 项映射到既有 workspace 深链；一次导航到修复面 |
| **LSS-03 与 U14 / Journey 对齐** | 复用 `workspaceTab` / `designTab` / `testingTab` / `approvalTab` 约定；不破坏 U14 Tasks 深链 |
| **LSS-04 Fail-closed / 边界** | 只读/无权限不暴露误导写入口；不可映射项无死链；gate 加载失败保持既有错误态 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-U16 authoring path（design 默认 bindings / 新建微向导） | Out of scope — #92 |
| 改变 lifecycle 状态机转移规则 / 发布语义 / gate check 算法 | Out of scope — 只做导航与可视化 |
| 新增后端 PublishGate 字段或新 checkCode | Out of scope — FE 映射既有 `PublishGateCheckCode` |
| Dashboard Tasks / collaboration 队列语义（U14） | 已交付；本片不改 |
| Release 详情只读 live gate 的「修复」写路径 | Out of scope — 主表面为 **dev** publish readiness；release 只读面板可不加 Go fix |
| 宣称 go-live / 激活 P3 / CD-3 | **禁止** |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **模板编排人员** | `authorTemplates` | 见 Stepper；对 pending gate 项点击 Go fix 进入设计/测试修复面 |
| **测试 / 审批人员** | `decideTests` / `decideApprovals` | 见 Stepper 定位阶段；可读 gate；Go fix 仅导航到可见面，不绕过写授权 |
| **发布负责人（组长）** | `publishTemplates` | 在 publish readiness 看 checklist + Go fix；修复本身仍依赖既有写能力 |
| **只读 / 无模板权限** | 无对应 capability | 不得因深链绕过 route/API 授权（403 / forbidden 基线） |
| **系统** | lifecycle detail + publish-gate API + workspace query | 渲染 Stepper；按 checkCode 解析修复深链 |

---

## 3. Goal

1. 用户打开模板 **dev workspace**（`/templates/{id}/dev/{devVersionId}`）时，在顶栏一眼看到当前生命周期阶段相对产品状态机的位置。  
2. 在 **Publish readiness**（及同源 publish summary checklist，若展示同一清单）中，每条 **pending** 检查项若存在可映射修复面，则显示 **Go fix**（English-first；zh-CN 等价「前往修复」），点击后一次导航到达该面。  
3. Stepper **不**承载提交测试/审批/发布等 CTA（遵守 Workspace Tab Shell：旅程/进度只读；动作仍在 tab action rail）。  
4. 不宣称 go-live；formal phase 保持 None。

---

## 4. 已确认决策 vs 推导假设

### 4.1 已确认（产品 / 计划 / 既有交付）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U15-C1** | 模板产品状态：`DRAFT` → `TESTING` → `APPROVAL`+`PENDING_SUBMIT`（测试通过）→ `APPROVAL`+`PENDING_DECISION`（待审批）→ `PENDING_RELEASE` → `PUBLISHED`；另有 `STOPPED` / `DEPRECATED`。 | [domain-model.md](../domain/domain-model.md) §4.1 |
| **U15-C2** | 发布前必须执行 publish-gate checklist；有阻断项不得发布。 | domain-model；既有 `PublishGateService` |
| **U15-C3** | Dev workspace 深链约定：`workspaceTab` ∈ design/testing/approval，及 `designTab` / `testingTab` / `approvalTab`。 | U14；`templateJourneyWorkspaceLink` / `templateDevWorkspaceTabs` |
| **U15-C4** | Journey / timeline = 只读导向，无 CTA；动作在 top-level tab action rail。 | [management-ui-constitution.md](../architecture/management-ui-constitution.md) Workspace Tab Shell |
| **U15-C5** | Formal phase **None**；不宣称 go-live；不激活 CD-3；deps U14 #90 **Done**。 | CE 计划 / Task Master #91 |
| **U15-C6** | `PublishGateCheckCode` 集合既有（含 `ANCHOR_INTEGRITY` … `FIDELITY_WARNINGS_VIEWED`）；`blocker=false` 在 UI 标 Informational。 | 后端 enum；`mapPublishGateChecklistItems` |

### 4.2 本片确认的实现决策（计划卡薄 → 仓库事实推导）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **U15-D1** | **主表面：** 模板 **dev workspace** 顶栏（WorkspaceTabShell **之上**或与 page header 同一顶区）展示 Lifecycle Stepper；Hub 未打开 dev 时可不强制 Stepper。 | 计划「顶栏」；dev 为编排主工作区 |
| **U15-D2** | **Stepper 步骤（线性，English-first 标签）：** Draft → Testing → Ready for approval → Pending approval → Pending release → Published。映射：`DRAFT`；`TESTING`；`APPROVAL`+`PENDING_SUBMIT`（或 null 按既有 resolve）；`APPROVAL`+`PENDING_DECISION`；`PENDING_RELEASE`；`PUBLISHED`。 | U15-C1 |
| **U15-D3** | **Stepper 交互：** **只读可视化**（当前 / 已完成 / 未到达）。**不**在 Stepper 上放 Submit/Approve/Publish。允许点击步骤 **仅切换**到对应导向 workspaceTab（Draft/Testing→design 或 testing；Ready/Pending approval→approval/submitApproval；Pending release→approval/publishReadiness；Published→只读提示或保持），**不得**触发状态机转移。 | U15-C4 |
| **U15-D4** | **`STOPPED` / `DEPRECATED`：** 不插入线性步进；Stepper 上方或旁路展示既有 StatusBadge / 文案，避免假装仍在发布通道中。 | 终端态 |
| **U15-D5** | **Go fix 主表面：** `TemplateDetailApprovalPublishPane`（approvalTab=`publishReadiness`）checklist 行。若 `TemplatePublishSummaryDialog` 复用同一 `publishGateItems`，同步提供 Go fix（实现可选但行为一致更佳）。**不要求** `PublishGateReadOnlyPanel`（已发布 release live 评估）提供写修复链。 | 计划「发布 gate」；避免误导已发布线 |
| **U15-D6** | **何时显示 Go fix：** `ready === false` 且 checkCode 在映射表中有修复目标。`ready === true` 不显示。Informational（非 blocker）若 pending 且可映射，**仍显示** Go fix（计划「每条 pending」）。 | 计划卡字面 |
| **U15-D7** | **默认 checkCode → 修复深链（同会话内 router push / query 更新即可）：** | 检查语义 + 既有子 Tab |
| | `ANCHOR_INTEGRITY` / `BLOCKER_STATUS` / `PASTE_CLEANING_BLOCKERS` / `UNSUPPORTED_STRUCTURED_NODES` → `workspaceTab=design&designTab=bindings` | |
| | `VARIABLE_SCHEMA` → `design` + `variables` | |
| | `RULE_BOUNDS` → `design` + `bindings`（规则在绑定/组合面可达；无独立 rules Tab） | |
| | `CONTENT_MODULE_REFERENCES` / `CONTENT_MODULE_EFFECTIVE_EXPIRED` → `design` + `contentModules` | |
| | `TEST_RESULTS` / `PREVIEW_PRESENT` → `testing` + `previewRuns` | |
| | `COVERAGE_THRESHOLDS` → `testing` + `coverage` | |
| | `CHANGE_DIFF` → `testing` + `changeDiff` | |
| | `FIDELITY_WARNINGS_VIEWED` → `testing` + `previewRuns`（保真查看既有路径） | |
| | `APPROVAL_SUMMARY` → `approval` + `submitApproval`（查看/补齐审批摘要语境） | |
| | `API_POLICY` → 优先包/策略可达面：若模板详情已有 API policy 入口则深链该处；否则 `approval` + `publishReadiness` 并滚动至该行（**禁止**虚构新路由） | |
| **U15-D8** | 未知 / 未来新增 checkCode：无 Go fix 链接，保留标签；不得 404 死链。映射表集中维护（纯 FE helper），Vitest 覆盖。 | 演进安全 |
| **U15-D9** | **i18n：** Stepper 步名、Go fix 控件 English-first（`en` + `zh-CN`）。业务用语对齐「Pre-release checks / Confirm go-live」，避免对用户暴露内部词 gate（见 business-terminology-guide）。 | i18n + terminology |
| **U15-D10** | **不改** OpenAPI / publish-gate 响应契约；不改权限矩阵角色集合。 | 最小改动 |
| **U15-D11** | DOM 可测：Stepper 使用稳定 `data-testid`（如 `lifecycle-stepper`）；Go fix 使用 `publish-gate-go-fix-{checkCode}` 或等价。 | E2E |

### 4.3 非确认假设（不得升格为需求）

| 项 | 状态 |
| --- | --- |
| Stepper 是否用 Element Plus `el-steps` 或自研 OA 条 | 实现自选；须双品牌 @1920 可验收 |
| Hub `?tab=lifecycle` 是否镜像 Stepper | 非必须；dev 顶栏为验收主证据 |
| Go fix 是否新开页 vs 同页改 query | 同 SPA 导航即可 |

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录且可打开目标模板 dev workspace（组范围授权通过）。  
- 模板具备 `lifecycleStatus`（及 APPROVAL 时的 `approvalSubState`）。  
- Publish readiness 场景下 publish-gate API 可返回 checklist（或既有加载错误态）。

**Triggers**

- 打开 / 刷新 `/templates/{id}/dev/{devVersionId}`。  
- 进入 `workspaceTab=approval&approvalTab=publishReadiness`（或打开含同一清单的 publish summary）。  
- 用户点击 pending 项的 **Go fix**。  
- （可选）用户点击 Stepper 某步做导向导航。

---

## 6. Primary journey

1. 作者打开 DRAFT 模板 dev workspace → 顶栏 Stepper 高亮 **Draft**，后续步为未到达。  
2. 提交测试后状态 `TESTING` → Stepper 高亮 **Testing**。  
3. 测试通过 → **Ready for approval**；提交审批 → **Pending approval**；审批通过 → **Pending release**。  
4. 发布负责人打开 publish readiness → 见 pre-release checklist；若 `ANCHOR_INTEGRITY` pending → 点击 **Go fix** → 落地 design/bindings。  
5. 修复并重新评估 gate 后该项 ready；全部就绪后走既有 Confirm go-live（本片不改发布 API）。  
6. 发布成功 → Stepper 高亮 **Published**。

---

## 7. System responses（success）

| 表面 | 成功响应 |
| --- | --- |
| **UI — Lifecycle Stepper** | 可见；当前步与 U15-D2 映射一致；已完成步可区分样式 |
| **UI — Go fix** | pending + 可映射项显示链接/按钮；点击后 URL/`workspaceTab`（及子 Tab）符合 U15-D7 |
| **API** | 继续使用既有 template detail + publish-gate；无新契约字段要求 |
| **i18n** | Stepper / Go fix English-first；zh-CN 同步 |
| **授权** | 深链不授予额外写权限；无权限写操作仍 fail-closed |

---

## 8. Acceptance scenarios

### BDD-CE-U15-LSS-001 — Dev workspace 顶栏 Stepper 可见且反映 DRAFT

```gherkin
Given 会话可打开模板 TPL-DRAFT 的 dev workspace
And TPL-DRAFT.lifecycleStatus = DRAFT
When 用户打开 /templates/{id}/dev/{devVersionId}
Then 顶栏出现 Lifecycle Stepper（data-testid=lifecycle-stepper 或等价）
And 当前步为 Draft
And Testing / Ready for approval / Pending approval / Pending release / Published 以未完成或后续态展示
And Stepper 上不出现 Submit for test / Approve / Publish 等 workflow CTA
```

### BDD-CE-U15-LSS-002 — Stepper 随状态推进

```gherkin
Given 模板 lifecycleStatus = TESTING
When 打开其 dev workspace
Then Stepper 当前步为 Testing
And Draft 显示为已完成（或等价已走过样式）
Given 模板 lifecycleStatus = APPROVAL 且 approvalSubState = PENDING_DECISION
When 打开 dev workspace
Then Stepper 当前步为 Pending approval
Given 模板 lifecycleStatus = PENDING_RELEASE
When 打开 dev workspace
Then Stepper 当前步为 Pending release
Given 模板 lifecycleStatus = PUBLISHED
When 打开对应可访问的详情/dev 或发布后回跳面（实现固定一处并测）
Then Stepper 当前步为 Published
```

### BDD-CE-U15-LSS-003 — STOPPED / DEPRECATED 不伪装线性进度

```gherkin
Given 模板 lifecycleStatus = STOPPED（或 DEPRECATED）
When 打开其详情或 workspace
Then 用户能明确看到 Stopped/Deprecated 状态（既有 StatusBadge 可）
And 不得呈现「当前仍在 Pending release 通道中」的误导性当前步
```

### BDD-CE-U15-LSS-004 — Pending gate 项显示 Go fix 并深链

```gherkin
Given 会话在模板 publish readiness 面
And publish-gate 返回 ANCHOR_INTEGRITY ready=false（blocker 或否均可）
When 用户查看 checklist
Then 该行显示 Go fix 控件（English-first）
When 用户激活 Go fix
Then 导航到同一模板 workspace 且 workspaceTab=design、designTab=bindings（或 U15-D7 等价）
And 用户可在该面开始修复，无需再手动寻找 bindings
```

### BDD-CE-U15-LSS-005 — Ready 项与不可映射项

```gherkin
Given publish-gate 中 VARIABLE_SCHEMA ready=true
Then 该行不显示 Go fix
Given 返回未知 checkCode X_UNKNOWN 且 ready=false
Then 该行显示 Pending（或等价）标签
And 不渲染指向无效路由的 Go fix
```

### BDD-CE-U15-LSS-006 — 映射抽样（测试 / 保真 / 条款）

```gherkin
Given COVERAGE_THRESHOLDS ready=false
When 激活其 Go fix
Then 落地 testing + coverage
Given FIDELITY_WARNINGS_VIEWED ready=false
When 激活其 Go fix
Then 落地 testing + previewRuns
Given CONTENT_MODULE_EFFECTIVE_EXPIRED ready=false
When 激活其 Go fix
Then 落地 design + contentModules
```

### BDD-CE-U15-LSS-007 — Stepper 可选导向点击不改状态

```gherkin
Given 模板 lifecycleStatus = PENDING_RELEASE
When 用户点击 Stepper 上的 Testing 步（若实现为可点击导向）
Then workspace 导向到 testing 相关 Tab
And 模板 lifecycleStatus 仍为 PENDING_RELEASE（无状态转移 API 调用）
```

### BDD-CE-U15-LSS-008 — 授权 fail-closed

```gherkin
Given 会话无 authorTemplates（只读或测试员）
When 用户通过 Go fix 进入 design/bindings
Then 不得出现可保存的写控件（或保存 API 仍 403）
And 不得因 query 篡改绕过既有路由守卫
```

### BDD-CE-U15-LSS-009 — Gate 加载失败保持既有模式

```gherkin
Given publish-gate 请求失败
When 用户停留在 publish readiness
Then 展示既有 LoadErrorPanel / Retry
And 不伪造 checklist 成功态
And Stepper 仍可根据 template detail 的 lifecycleStatus 渲染
```

### BDD-CE-U15-LSS-010 — 回归：U14 与非目标

```gherkin
Given U14 Dashboard Tasks 深链行为已交付
When 回归打开 TEST / APPROVAL / PENDING_RELEASE 待办
Then 仍落到 testing / submitApproval / publishReadiness
And 本片不实现 CE-U16 默认 bindings 落点或新建微向导
And 不宣称 go-live / 不激活 CD-3
```

---

## 9. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| `approvalSubState` 缺失但 status=`APPROVAL` | 与既有 `resolveApprovalSubState` / 详情字段一致；不得崩溃；步进落到可辩护的 Ready vs Pending approval 默认并测 |
| 无 `devVersionId`（仅 Hub） | 不强制 Stepper；进入 dev 后出现 |
| Go fix 目标 Tab 对当前角色隐藏 | 落到最近可访问父级 Tab 或只读空态；不得空白死循环 |
| Informational pending | 仍可 Go fix（U15-D6）；不因此允许 publish（gate ready 仍由后端/既有逻辑决定） |
| 双品牌 / 窄屏 | @1920 主验收；Stepper 可换行但不得遮挡 action rail |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | `lifecycle-stepper`；pending 行 Go fix；URL 含正确 workspace/子 Tab |
| Vitest | Stepper 状态映射；checkCode→query helper；无死链默认 |
| E2E + UIUX | 至少 LSS-001 + LSS-004 +（002 或 006）；UIUX 双品牌 @1920；Critical=0 |
| 回归 | U14 Tasks 深链；CDP publish 旅程；既有 publish-gate 阻断语义 |
| 非目标 | 无 U16 微向导；无 go-live 宣称 |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U15 | 目标行为（P2·M 简卡） |
| Task Master **#91** | 执行任务；slice `ce-u15-lifecycle-stepper` |
| [domain-model.md](../domain/domain-model.md) §4.1 | 产品状态机 |
| [management-ui-constitution.md](../architecture/management-ui-constitution.md) | Stepper 只读 / action rail 分离 |
| [ce-u14-dashboard-lifecycle-todos.md](./ce-u14-dashboard-lifecycle-todos.md) | 深链约定依赖（#90 Done） |
| `templateJourneyWorkspaceLink` / `templateDevWorkspaceTabs` | query 对齐 |
| `PublishGateCheckCode` / `TemplateDetailApprovalPublishPane` | checklist 表面 |
| CE-U16 (#92) | 后续片；本片不实现 |

---

## 12. 现状 → 实现提示（非额外需求）

| 发现 | 路径提示（供 plan / 实现） |
| --- | --- |
| U14 E2E 断言无 `.el-steps` | 本片引入后更新/替换该负向断言 |
| `TemplateDetailApprovalPublishPane` 行内仅 tag | 行尾加 Go fix `RouterLink`/`button` |
| 无集中 checkCode→tab map | 新增纯函数 helper + 单测（对齐 U15-D7） |
| Stepper 位置 | `TemplateDetailDevWorkspace` / LoadedDevSection header 插槽 |
| 术语 | UI 用 Pre-release checks；testid 可用 publish-gate |

---

**bdd_readiness: ready**  
**open_questions: []**  
**frontend_ui_in_scope: true**  
**owning_doc:** `docs/behavior/ce-u15-lifecycle-stepper.md`  
**task_ids:** ["91"]
