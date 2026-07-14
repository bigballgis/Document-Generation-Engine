# BDD 行为规格：CE-U08 — 条款（内容模块）审核闭环

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U08-CMRL`  
**编写日期:** 2026-07-15  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U08  
**Slice:** `ce-u08-content-module-review-loop`（Task Master 描述别名 `ce-u08-clause-review-loop`；worktree / 计划切片 id 以本文件为准）  
**Task Master:** **#83**  
**Formal phase:** **None**  
**完成声明约束:** 关闭 R4「条款审核无待办 / 驳回不回显 / 无审核时间线」前端闭环；**不**改变既有 `POST …/review/transition` 状态机与 CE-G01 同人阻断；**不**实现 CE-U09 母版审核可达性；**不**宣称 go-live

---

## 1. 概述

后端内容模块 review 流转已完整（`SUBMIT_FOR_REVIEW` / `APPROVE_REVIEW` / `REJECT_REVIEW`；`REJECT_REVIEW` 必填 `rejectionReason`；实体持久化 `rejectionReason`；审计 `CONTENT_MODULE_REVIEW_TRANSITION`）。但管理 UI 闭环断裂：

| 缺口（现状证据） | 目标 |
| --- | --- |
| `useWorkflowTasks` 仅有母版待审/返工与 CE-U07 条款升版待办，**无** CM pending/rework | Dashboard Tasks 增加 content-module 待审与返工任务分区 |
| `ContentModuleVersionView` **未**暴露 `rejectionReason`；版本表无该列 | 版本表展示驳回原因 |
| lifecycle Tab 仅 hint，**无**母版式 `el-timeline` | 对齐 `MasterRevisionDetailWorkspace` approval 时间线 |

| 行为域 | 摘要 |
| --- | --- |
| **CMRL-01 Dashboard 待审** | 持 `decideContentModuleReviews` 者看到 `SUBMITTED` 版本模块的待办，深链模块详情 lifecycle |
| **CMRL-02 Dashboard 返工** | 持 `authorContentModules` 者看到 `DRAFT` 且带非空 `rejectionReason` 的模块返工待办 |
| **CMRL-03 驳回回显** | 版本表列展示 `rejectionReason`（DRAFT 返工态可见） |
| **CMRL-04 审核时间线** | lifecycle Tab 以母版同构 `el-timeline` 展示 `reviewHistory` |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 变更 review 状态机 / 角色矩阵 / CE-G01 同人阻断 | Out of scope — 复用既有 `ContentModuleReviewService` |
| CE-U09 母版 Hub / `?workspaceTab=approval` 可达性 | Out of scope |
| CE-U07 条款升版 bump 待办 | 已交付；本片不改语义 |
| Activity log 全站审计浏览器替代时间线 | Out of scope — 模块详情内时间线即可 |
| 新审核专用角色 | Out of scope — 复用 `authorContentModules` / `decideContentModuleReviews` |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **条款作者** | `authorContentModules`（`TEMPLATE_AUTHOR` / `MASTER_DESIGNER` / `GROUP_ADMIN` / `GLOBAL_ADMIN`） | 提交审核、编辑草稿、查看驳回原因与返工待办 |
| **条款审批人** | `decideContentModuleReviews`（`TEMPLATE_APPROVER` / `GROUP_ADMIN` / `GLOBAL_ADMIN`） | 通过/驳回 `SUBMITTED` 版本；Dashboard 待审待办 |
| **系统** | CM detail/list API + `useWorkflowTasks` + Dashboard Tasks | 暴露驳回字段与审核历史；投影待办；fail-closed 隐藏无能力分区 |

---

## 3. Goal

1. 审批人打开 Dashboard Tasks 即可发现待审内容模块，并一键进入 lifecycle 决策面。  
2. 作者在驳回后从 Dashboard 与版本表看到**同一条**驳回原因，无需猜审计。  
3. 模块详情 lifecycle Tab 的审核历史视觉与交互对齐母版 revision approval `el-timeline`。  
4. 无对应能力的会话看不到 CM 审核/返工待办（fail-closed）。

---

## 4. Preconditions / Trigger

**Preconditions**

- 用户已登录管理会话，可访问 `route.content-module-management` 与/或 Dashboard。  
- 目标组内存在至少一条内容模块版本处于 `SUBMITTED`，或 `DRAFT` 且 `rejectionReason` 非空（驳回后）。  
- 既有 review transition API 可用（OpenAPI `transitionContentModuleReview`）。

**Triggers**

- 作者在 lifecycle 提交审核 / 审批人通过或驳回。  
- 任一角色打开 Dashboard → Tasks。  
- 用户打开内容模块详情 `versions` 或 `lifecycle` Tab。

---

## 5. Primary journey

1. 作者在模块详情 lifecycle 提交 `SUBMIT_FOR_REVIEW`（必填 changeDescription）→ 版本 `SUBMITTED`。  
2. 审批人在 Dashboard Tasks 看到 content-module 待审项 → 打开深链 → lifecycle 通过或驳回。  
3. 若驳回：版本回 `DRAFT`，`rejectionReason` 持久化 → 版本表回显 → 作者 Dashboard 出现返工待办。  
4. 作者按驳回原因修改草稿后再次提交；lifecycle 时间线保留提交/驳回/通过记录。

---

## 6. System responses（success）

| 表面 | 成功响应 |
| --- | --- |
| **API — VersionView** | `ContentModuleVersionView` 增加可选 `rejectionReason`；detail/list 映射实体字段；`APPROVE_REVIEW` 后为 null；`REJECT_REVIEW` 后为非空 trim 文本 |
| **API — Detail reviewHistory** | `ContentModuleDetailView` 增加 `reviewHistory[]`（母版同构字段：`action`、`actorUsername`、`createdAt`，可选 `changeSummary` / `commentSummary`）；按时间升序或与母版一致的展示顺序；覆盖本模块历次 SUBMIT / APPROVE / REJECT（含 semanticVersion 上下文时可放入 summary） |
| **UI — Versions 表** | 新增 Rejection reason 列；`DRAFT` 且非空时展示全文；无值时空/— |
| **UI — Lifecycle 时间线** | 有历史：`el-timeline` + `el-timeline-item`（timestamp、action 文案、change/reject 文本、actor）；无历史：`el-empty`（对齐母版 `masters.revision.noReviewHistory` 模式） |
| **Dashboard — useWorkflowTasks** | 新 `WorkflowTaskKind`：`content-module-review`、`content-module-rework`；分区进入 Tasks Tab；深链 `/content-modules/{moduleId}?workspaceTab=lifecycle` |
| **Dashboard 数据加载** | Tasks/Overview 在具备能力时加载足以投影 CM 待办的模块工作流数据（实现可选：enrich catalog、专用 workflow 端点、或既有 list+filter；**不得**要求审批人手工打开每个模块详情才能发现待办） |

---

## 7. Acceptance scenarios

### BDD-CE-U08-CMRL-001 — Dashboard 待审待办

```gherkin
Given 会话持有 decideContentModuleReviews
And 组内内容模块 MOD-A 存在 reviewState=SUBMITTED 的版本
When 审批人打开 Dashboard Tasks
Then 出现 kind=content-module-review 待办，entityName 为 MOD-A 名称
And 待办 path 指向 /content-modules/{moduleId}?workspaceTab=lifecycle
When 审批人打开该待办
Then 进入该模块详情且 lifecycle Tab 激活，并可执行 Approve / Reject
```

### BDD-CE-U08-CMRL-002 — Dashboard 返工待办

```gherkin
Given 会话持有 authorContentModules
And 模块 MOD-B 最新相关版本 reviewState=DRAFT 且 rejectionReason 非空
When 作者打开 Dashboard Tasks
Then 出现 kind=content-module-rework 待办，可识别模块名称
And 待办深链至该模块 lifecycle（或 versions，须能立即看到驳回原因）
When 作者打开该待办
Then 可看见驳回原因并继续编辑/再次提交
```

### BDD-CE-U08-CMRL-003 — 版本表回显 rejectionReason

```gherkin
Given 审批人对 SUBMITTED 版本执行 REJECT_REVIEW 且填写 rejectionReason="Wording not acceptable"
When 任意有结构查看权限的用户打开该模块 versions Tab
Then 该版本行 reviewState 为 DRAFT
And 版本表 Rejection reason 列展示 "Wording not acceptable"
And GET content-module detail 中对应 ContentModuleVersionView.rejectionReason 等于该文本
```

### BDD-CE-U08-CMRL-004 — lifecycle 审核时间线（对齐母版）

```gherkin
Given 模块至少发生过一次 SUBMIT_FOR_REVIEW 与一次 REJECT_REVIEW（或 APPROVE_REVIEW）
When 用户打开模块详情 lifecycle Tab
Then 展示 el-timeline，条目含时间戳、动作文案、actor
And 提交条目展示 changeDescription（或 changeSummary）
And 驳回条目展示 rejectionReason（或 commentSummary）
And 时间线视觉/信息密度对齐 MasterRevisionDetailWorkspace approval 历史（非 Activity log 页）
```

### BDD-CE-U08-CMRL-005 — 无审核历史空态

```gherkin
Given 模块从未发生 review transition（仅有未提交草稿）
When 用户打开 lifecycle Tab
Then 不渲染虚假时间线条目
And 展示空态说明（i18n，English-first）
```

### BDD-CE-U08-CMRL-006 — 能力 fail-closed

```gherkin
Given 会话无 decideContentModuleReviews
When 用户打开 Dashboard Tasks
Then 不出现 content-module-review 待办分区/条目
Given 会话无 authorContentModules
Then 不出现 content-module-rework 待办
And 无权限用户不得通过待办深链绕过既有 route/API 授权（403 / 禁止路由基线不变）
```

### BDD-CE-U08-CMRL-007 — 批准后清除驳回回显

```gherkin
Given 版本曾被驳回后再次提交并被 APPROVE_REVIEW
When 用户查看 versions 表该版本行
Then rejectionReason 列为空
And API 该版本 rejectionReason 为 null/省略
And reviewHistory 仍保留历史驳回与最终通过条目
```

---

## 8. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| `REJECT_REVIEW` 缺少 `rejectionReason` | 既有 **422** `MODULE_REJECTION_REASON_REQUIRED` — 本片不改 |
| 角色越权 transition | 既有 **403** `MODULE_REVIEW_ROLE_DENIED` — 本片不改 |
| 状态前置失败 | 既有 **409** `MODULE_REVIEW_STATE_TRANSITION_DENIED` — 本片不改 |
| CE-G01 同人审批 | 既有 **403** `SELF_APPROVAL_FORBIDDEN` — 本片不改；时间线可显示例外标记若记录已有 |
| 分页 catalog 未含全部 SUBMITTED | 待办投影必须覆盖审批人可见范围内全部待审模块（不得静默只显示第一页） |
| `TEMPLATE_TESTER` | 无 CM 目录权限 — 无 CM 待办、无详情时间线入口 |

---

## 9. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | `ContentModuleVersionView.rejectionReason`；`ContentModuleDetailView.reviewHistory`；OpenAPI + 契约示例同步 |
| UI | versions 列；lifecycle `el-timeline`；Dashboard Tasks 分区 |
| Vitest | `useWorkflowTasks` 投影 CM review/rework；版本列/时间线组件或 detail derived |
| E2E + UIUX | 至少覆盖 CMRL-001/002/003（或 003+004）+ UIUX @1920 双品牌截图清单 |
| 回归 | 既有 submit/approve/reject action handlers 与 CE-G01 测试保持 GREEN |

---

## 10. Traceability

| 来源 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U08 | 目标行为 / R4 缺口 |
| Task Master **#83** | 执行任务；TM 描述 slice 别名 `ce-u08-clause-review-loop` |
| [domain-model.md](../domain/domain-model.md) §2.9.2 | DRAFT ↔ SUBMITTED ↔ APPROVED；驳回回草稿 |
| [permission-matrix.md](../security/permission-matrix.md) §13.2 | `authorContentModules` / `decideContentModuleReviews` |
| [openapi-v1.yaml](../api/openapi-v1.yaml) `transitionContentModuleReview` | 既有流转契约 |
| Master `MasterRevisionDetailWorkspace` `#approval` | 时间线 UX 对齐参照 |
| CE-U07 / CE-G01 | 相邻切片；本片不重叠改动 |
| `useWorkflowTasks` + `workflowTaskPartitions` | 待办投影扩展点 |

---

## 11. 现状 → 实现提示（非验收假设）

| 发现 | 路径提示（供 plan / 实现，非额外需求） |
| --- | --- |
| `ContentModuleVersionView` Java/OpenAPI 无 `rejectionReason` | 映射实体已有字段 |
| `ContentModuleSummaryView` 无 review 态 | Dashboard 投影需 enrich 或专用 workflow 查询 |
| 无 `reviewHistory` 表（母版有 `MasterReviewRecordEntity`） | 可新增 durable review records，或从 `CONTENT_MODULE_REVIEW_TRANSITION` 审计投影；**验收只要求 detail 可渲染时间线** |
| lifecycle Tab 无时间线 DOM | 复用母版 timeline 结构 + i18n `contentModules.reviewHistory.*` |
| `SUBMIT_FOR_REVIEW` 当前不清理 `rejectionReason` | 批准已清理；建议提交时清理以免 SUBMITTED 行残留旧驳回——CMRL-003/007 以 DRAFT 回显与批准清空为准 |

---

**bdd_readiness: ready**
