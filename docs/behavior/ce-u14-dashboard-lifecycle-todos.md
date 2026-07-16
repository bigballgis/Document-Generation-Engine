# BDD 行为规格：CE-U14 — Dashboard 模板生命周期待办

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U14-DLT`  
**编写日期:** 2026-07-16  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U14  
**Slice:** `ce-u14-dashboard-lifecycle-todos`  
**Task Master:** **#90**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u14-dashboard-lifecycle-todos` · `feat/ce-u14-dashboard-lifecycle-todos`  
**完成声明约束:** 关闭「测试裁定 / 审批 / 待发布」三类模板生命周期队列在 Dashboard **Tasks Tab** 的可发现性与一键可达决策面；复用既有协作工作项与生命周期 API；**不**实现 CE-U15 Stepper / checklist 深链；**不**宣称 go-live；**不**激活 CD-3

---

## 1. 概述

P21 已交付协作工作项（`TEST` / `APPROVAL` / `PENDING_RELEASE` / `REMEDIATION` / `ESCALATION`）并投影进 Dashboard Tasks（`useWorkflowTasks` + `buildTaskPartitions`）。CE 计划卡 CE-U14 要求这三类**决策队列**在 Tasks Tab 形成与 CE-U08（条款审核待办）同级的闭环体验。现状证据显示：**分区与数据源大体已在**，但深链仍经 `templateLifecyclePanelPath` → Hub `?tab=lifecycle` → **一律** `openDevEditor('approval')`，测试员打开待办后需再点「Template testing」才能裁定（见 `frontend/e2e/helpers/lifecycle-ui.ts` 注释）。本片对齐 U09「待办深链即决策面」模式，补齐队列感知深链，并固化 Tasks Tab 验收。

| 缺口（现状证据） | 目标 |
| --- | --- |
| 计划卡要求测试裁定/审批/待发布进 Tasks Tab | 持对应 capability 的会话在 Tasks Tab 看到三队列分区与 OPEN 待办 |
| 协作待办 path 一律 `?tab=lifecycle` → 总是落地 approval workspace | **按队列**深链到 testing / approval(submit) / approval(publishReadiness) |
| E2E 需「hub 或再切 testing」双路径容错 | 主路径单次导航即可执行 Pass/Fail、Approve/Reject、Publish 入口可见动作 |

| 行为域 | 摘要 |
| --- | --- |
| **DLT-01 Tasks · 测试裁定** | `decideTests` → `queue=TEST` / `kind=template-test` 分区；深链 testing 决策面 |
| **DLT-02 Tasks · 审批** | `decideApprovals` → `queue=APPROVAL` / `kind=template-approval`；深链 approval 决策面 |
| **DLT-03 Tasks · 待发布** | `publishTemplates` → `queue=PENDING_RELEASE` / `kind=template-publish`；深链 publish readiness / go-live 面 |
| **DLT-04 能力 fail-closed** | 无 capability 不展示对应分区/条目；深链不得绕过 API/路由授权 |
| **DLT-05 关闭环** | 判定/发布成功后工作项 `RESOLVED`，Tasks 该项消失（复用既有 resolve） |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-U15 顶栏 Stepper + publish-gate「前往修复」 | Out of scope — 后续片 |
| 新建并行「仅靠 lifecycleStatus 扫描」待办源 | Out of scope — **权威源**仍为 OPEN collaboration work items（P21 已闭合发射/resolve） |
| 改变协作队列可见性矩阵 / 超时升级语义 / REMEDIATION·ESCALATION | Out of scope — 回归保持；本片不改权限矩阵角色集合 |
| 改变测试/审批/发布状态机或表单字段（Pass/Fail/Approve 等） | Out of scope — 复用既有 lifecycle decision APIs |
| CE-U08 CM 待办 / CE-U09 母版待办语义 | 已交付；本片不改 |
| 宣称 go-live / 激活正式 P-phase / CD-3 | **禁止** |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **测试人员** | `decideTests`（`TEMPLATE_TESTER` / `GROUP_ADMIN` / `GLOBAL_ADMIN`） | Tasks 见测试裁定待办；打开后可 Pass/Fail |
| **审批人员** | `decideApprovals`（`TEMPLATE_APPROVER` / `GROUP_ADMIN` / `GLOBAL_ADMIN`） | Tasks 见审批待办；打开后可 Approve/Reject |
| **发布负责人（组长）** | `publishTemplates`（仅 `GROUP_ADMIN` / `GLOBAL_ADMIN`，Batch B） | Tasks 见待发布待办；打开后可达发布摘要/二次确认 |
| **系统** | collaboration work-items API + `useWorkflowTasks` + Dashboard Tasks + template dev workspace tabs | 投影待办；队列感知深链；fail-closed 隐藏无能力分区 |

---

## 3. Goal

1. 测试员 / 审批人 / 发布负责人打开 Dashboard → **Tasks**，无需先翻模板目录或依赖 Overview 旅程卡，即可发现本角色队列内的模板生命周期待办。  
2. 点击待办 **一次导航** 落到可执行裁定/审批/发布的工作区（对齐旅程深链 `templateJourneyWorkspaceLink` 语义）。  
3. 无对应 capability 的会话看不到对应队列分区（fail-closed）。  
4. 成功判定/发布后，该项从 Tasks 消失（既有 RESOLVED 环）。

---

## 4. 已确认决策 vs 推导假设

### 4.1 已确认（产品 / 权限 / 既有交付）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U14-C1** | 模板生命周期协作待办按组+角色队列分配；`SUBMIT_FOR_TEST` / `SUBMIT_FOR_APPROVAL` / `APPROVAL_PENDING_RELEASE` 等触发生成 OPEN 工作项。 | [PRD.md](../product/PRD.md) 协作待办；[permission-matrix.md](../security/permission-matrix.md) §13.1.2；P21-T02/T07 **Done** |
| **U14-C2** | Tasks Tab 分区可见性：`decideTests`→TEST；`decideApprovals`→APPROVAL；`publishTemplates`→PENDING_RELEASE（另 REMEDIATION/ESCALATION 已有，本片不改）。 | `getVisibleCollaborationQueues`；permission-matrix §13.2 |
| **U14-C3** | Dashboard 行为型入口是 Tasks 的按队列过滤视图（`?queue=` + `#tasks-section`），不是独立 workbench。 | permission-matrix §13.1.2；P21-T01 |
| **U14-C4** | 待办权威数据源 = OPEN collaboration work items（经 `collaborationStore` → `collaborationWorkItemToTask`）。 | P21；`useWorkflowTasks` |
| **U14-C5** | Formal phase 保持 **None**；不宣称 go-live；不激活 CD-3。 | CE 计划 / Task Master #90 |

### 4.2 本片确认的实现决策（计划卡薄 → 由仓库事实推导，银行 OA 一致）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **U14-D1** | Tasks 待办 `path` **必须队列感知**，不得对所有队列固定落地 approval。 | 现状 bug（hub `redirectLifecycleDeepLink`）；对齐 CE-U09 |
| **U14-D2** | 推荐深链（有 `devVersionId` 时）： | 对齐 `templateJourneyWorkspaceLink` + CDP tester/approver/publish 旅程 |
| | · `TEST` → `/templates/{id}/dev/{devVersionId}?workspaceTab=testing`（子 Tab 以能立即看到 Pass/Fail 动作栏为准；默认可用 `testingTab=previewRuns` 或与旅程 `recordResult` 一致） | |
| | · `APPROVAL` → `...?workspaceTab=approval&approvalTab=submitApproval` | |
| | · `PENDING_RELEASE` → `...?workspaceTab=approval&approvalTab=publishReadiness` | |
| **U14-D3** | 无 `devVersionId` 时：可先落 Hub，但 **redirect 必须按原队列**打开对应 workspace（修复「lifecycle 一律 approval」），不得静默丢队列语义。 | 可达性完整 |
| **U14-D4** | 未过滤 Tasks（Overview/Tasks 主视图）对当前会话可见队列展示分区；空队列可隐藏空分区或展示空态——**实现二选一，须一致且测**；有待办时分区标题与条目必须可见。 | 对齐 U08 分区模式 |
| **U14-D5** | 待办投影须覆盖操作者可见范围内全部 OPEN 项（不得静默只显示第一页）。 | 对齐 U08 CMRL 分页约束；协作 list API 既有分页则须拉全或提供充足 page size |
| **U14-D6** | 不新增后端队列类型；不改 OpenAPI 工作项契约，除非深链所需字段（如 `devVersionId`）已在 summary 中缺失——若缺失，允许 FE 用 template detail/catalog 补全后再导航（实现细节，验收只要求落地正确）。 | 最小改动 |

### 4.3 非确认假设（不得升格为需求）

| 项 | 状态 |
| --- | --- |
| 是否在 Tasks 行内展示 age / submitter display name 的额外列 | 已有则保持；本片**不强制**新列 |
| Overview 旅程卡文案/步骤是否同步改写 | Out of scope（除非深链 helper 共用导致必要回归） |

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录管理会话，可访问 Dashboard。  
- 目标组内存在至少一条 OPEN collaboration work item，队列为 `TEST` 或 `APPROVAL` 或 `PENDING_RELEASE`。  
- 对应模板仍处于与队列一致的生命周期态（`TESTING` / `APPROVAL`+`PENDING_DECISION` / `PENDING_RELEASE`）。  
- 既有测试裁定 / 审批决策 / 发布 API 可用。

**Triggers**

- 作者提交测试 / 提交审批 / 审批通过进入待发布（工作项发射，既有）。  
- 任一角色打开 Dashboard → Tasks（或行为型入口 `?queue=…#tasks-section`）。  
- 用户点击 Tasks 行「Open」/ 待办链接。

---

## 6. Primary journey

1. 作者提交测试 → 组内测试队列出现 OPEN `TEST` 工作项。  
2. 测试员打开 Dashboard Tasks → 见 template-test 待办 → Open → 落地 **testing** workspace → Pass 或 Fail。  
3. 若 Pass 且作者提交审批 → 审批人 Tasks 见 template-approval → Open → **approval / submitApproval** → Approve 或 Reject。  
4. 若 Approve → 模板 `PENDING_RELEASE`，组长 Tasks 见 template-publish → Open → **publishReadiness** → 发布摘要 + 二次确认（既有 go-live 行为）。  
5. 每步成功后对应 OPEN 工作项 resolve，Tasks 该项消失。

---

## 7. System responses（success）

| 表面 | 成功响应 |
| --- | --- |
| **API — collaboration work items** | 既有 list（按 queue 可选过滤）返回 OPEN 项；判定/发布后 status=`RESOLVED`（本片不改契约） |
| **UI — Dashboard Tasks** | 可见队列以分区展示；条目含可识别 `entityName`（模板名）、kind/queue、可点击 path |
| **UI — 深链** | 按 U14-D2/D3 打开对应 workspace Tab；Pass/Fail 或 Approve/Reject 或 Publish 入口在首屏可达（无需再猜 Tab） |
| **投影** | `WorkflowTaskKind`：`template-test` / `template-approval` / `template-publish`；`source=collaboration` |
| **i18n** | 分区/条目标题 English-first；沿用既有 `dashboard.tasks.template*` / `collaboration.workItem.queue.*` keys，缺译补齐 |

---

## 8. Acceptance scenarios

### BDD-CE-U14-DLT-001 — Tasks 测试裁定待办 + 深链

```gherkin
Given 会话持有 decideTests
And 组内存在 OPEN collaboration work item，queue=TEST，模板名为 TPL-TEST
When 测试员打开 Dashboard Tasks（未过滤或 ?queue=TEST）
Then 出现 kind=template-test 待办，entityName 可识别为 TPL-TEST
And 待办 path 指向该模板的 testing 决策面（workspaceTab=testing；有 devVersion 时含 /dev/{devVersionId}）
When 测试员打开该待办
Then 进入模板开发工作区且 testing Tab 激活
And 可立即看到 Confirm test pass / Record test failure（或等价主动作），无需先手动从 approval 切到 testing
```

### BDD-CE-U14-DLT-002 — Tasks 审批待办 + 深链

```gherkin
Given 会话持有 decideApprovals
And 组内存在 OPEN work item，queue=APPROVAL，模板名为 TPL-APPR
When 审批人打开 Dashboard Tasks
Then 出现 kind=template-approval 待办，可识别 TPL-APPR
And path 指向 approval 决策面（workspaceTab=approval；推荐 approvalTab=submitApproval）
When 审批人打开该待办
Then 可执行 Approve / Reject（既有受控表单），无需额外寻找隐藏入口
```

### BDD-CE-U14-DLT-003 — Tasks 待发布待办 + 深链

```gherkin
Given 会话持有 publishTemplates
And 组内存在 OPEN work item，queue=PENDING_RELEASE，模板名为 TPL-PUB
When 发布负责人打开 Dashboard Tasks
Then 出现 kind=template-publish 待办，可识别 TPL-PUB
And path 指向 publish readiness / go-live 面（workspaceTab=approval 且 approvalTab=publishReadiness，或等价可发布面）
When 用户打开该待办
Then 可见发布摘要入口与二次确认发布能力（既有 BDD-CDP-PUB 行为），编排人员无 publishTemplates 时仍不得出现主发布按钮
```

### BDD-CE-U14-DLT-004 — 能力 fail-closed

```gherkin
Given 会话无 decideTests
When 用户打开 Dashboard Tasks
Then 不出现 queue=TEST / kind=template-test 分区或条目
Given 会话无 decideApprovals
Then 不出现 APPROVAL / template-approval 待办
Given 会话无 publishTemplates
Then 不出现 PENDING_RELEASE / template-publish 待办
And 无权限用户不得通过篡改 path 绕过既有 route/API 授权（403 / forbidden 基线不变）
```

### BDD-CE-U14-DLT-005 — 判定后待办消失

```gherkin
Given 测试员对 TPL-TEST 的 OPEN TEST 工作项执行 Pass test（或 Fail test）成功
When 刷新或重新进入 Dashboard Tasks
Then 该 workItemId 对应的 template-test 待办不再出现
And 工作项状态为 RESOLVED（API 可验证）
```

### BDD-CE-U14-DLT-006 — 行为型导航过滤落地

```gherkin
Given 会话持有 decideTests 且可见「待我测试」行为型入口
When 用户激活该入口（/dashboard?queue=TEST#tasks-section）
Then Tasks 区域滚动可见且仅（或首要）展示 TEST 队列分区
And 其中 OPEN 测试待办与未过滤视图中的 TEST 项一致（同源）
```

### BDD-CE-U14-DLT-007 — 可见范围内全量投影

```gherkin
Given 操作者可见范围内存在多于一页的 OPEN TEST（或 APPROVAL / PENDING_RELEASE）工作项
When 打开 Dashboard Tasks（对应该队列）
Then 待办列表覆盖全部可见 OPEN 项，不得静默只显示第一页
```

### BDD-CE-U14-DLT-008 — 回归：非目标队列与邻片

```gherkin
Given 会话具备 REMEDIATION 或 ESCALATION 可见性（若有）
When 打开 Dashboard Tasks
Then REMEDIATION / ESCALATION 既有分区行为不回退
And content-module-review / master-review 待办语义保持 CE-U08 / CE-U09 既有验收
And 本片不引入 CE-U15 Stepper DOM
```

---

## 9. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| collaboration fetch 失败 | Tasks 区展示既有错误态 + Retry；不得伪造空成功掩盖失败（Dashboard 既有 `collaborationFetchFailed` 模式） |
| 工作项 OPEN 但模板已不可达 / 无 devVersion | 深链失败时 fail-closed 提示或回退 Hub；不得 空白页死循环；不得误开错误队列 Tab |
| `TEMPLATE_AUTHOR` 看 PENDING_RELEASE | 无 `publishTemplates` → 无发布待办；旅程可显示「等待组长确认」（既有 COR-T07） |
| 同人审批 / 保真未查看阻断 | 既有 CE-G01 / fidelity viewed fail-closed — 本片不改 |
| 空队列 | 不展示误导性待办条；空态文案 English-first |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | Dashboard Tasks 三队列分区；待办 Open 后 URL 含正确 `workspaceTab`（及子 Tab） |
| Vitest | `collaborationWorkItemToTask` / path builder 按 queue 断言；`buildTaskPartitions` fail-closed；必要时 hub redirect 队列感知 |
| E2E + UIUX | 至少覆盖 DLT-001 +（002 或 003）+ DLT-004；UIUX @1920 双品牌截图清单 |
| 回归 | `collaboration-todos` / CDP tester·approver·publish 旅程保持 GREEN；CM/master 待办不回退 |
| 非目标 | 无 CE-U15 Stepper；无 go-live 宣称 |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U14 | 目标行为（P2·S 简卡） |
| Task Master **#90** | 执行任务；slice `ce-u14-dashboard-lifecycle-todos` |
| [PRD.md](../product/PRD.md) 协作待办 / 测试·审批·待发布 | 产品确认队列语义 |
| [permission-matrix.md](../security/permission-matrix.md) §13.1.2 / §13.2 | 行为入口与 capability |
| [ce-u08-content-module-review-loop.md](./ce-u08-content-module-review-loop.md) | Dashboard 待办分区模式参照 |
| [ce-u09-master-review-reachability.md](./ce-u09-master-review-reachability.md) | 待办深链即决策面参照 |
| [tester-decision-journey.md](./tester-decision-journey.md) / [approver-decision-journey.md](./approver-decision-journey.md) / [team-lead-publish-journey.md](./team-lead-publish-journey.md) | 决策动作语义（本片不改表单） |
| `useWorkflowTasks` + `workflowTaskPartitions` + `collaborationWorkItems` | 投影扩展点 |
| `templateJourneyWorkspaceLink` | 队列→workspace Tab 对齐参照 |
| CE-U15 (#91) | 硬依赖本片之后；本片不实现 Stepper |

---

## 12. 现状 → 实现提示（非额外需求）

| 发现 | 路径提示（供 plan / 实现） |
| --- | --- |
| `collaborationWorkItemToTask` 固定 `templateLifecyclePanelPath` | 改为按 `item.queue` 生成 U14-D2 path（可抽 `collaborationWorkItemPath(item)`） |
| Hub `redirectLifecycleDeepLink` 写死 `openDevEditor('approval')` | 接受可选 queue/focus query，或废弃该中间跳转，直接深链 `/dev/...?workspaceTab=…` |
| Journey CTA 已有正确 Tab 映射 | Tasks Open 应复用同一映射，避免双标准 |
| E2E `lifecycle-ui.ts` 双路径容错 | 本片 GREEN 后可收窄为单一 testing/approval 落地断言 |

---

**bdd_readiness: ready**  
**open_questions: []**  
**frontend_ui_in_scope: true**  
**owning_doc:** `docs/behavior/ce-u14-dashboard-lifecycle-todos.md`  
**task_ids:** ["90"]
