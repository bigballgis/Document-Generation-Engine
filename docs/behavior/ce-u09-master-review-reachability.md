# BDD 行为规格：CE-U09 — 母版审核可达性

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U09-MRR`  
**编写日期:** 2026-07-15  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U09  
**Slice:** `ce-u09-master-review-reachability`  
**Task Master:** **#84**  
**Formal phase:** **None**  
**完成声明约束:** 关闭 R4「母版审核入口藏深层 + Hub 死代码」；复用既有母版 review API / 状态机；**不**改变 CE-U08 条款审核语义；**不**宣称 go-live

---

## 1. 概述

母版提交/通过/驳回能力已在 **revision 详情** `approval` Tab 可用，但 Package Hub 与旅程 CTA 不可达：

| 缺口（现状证据） | 目标 |
| --- | --- |
| Hub `MasterPackageHubActions` 仅有 Download / Replace / Edit metadata；无 Submit / Approve / Reject | Hub 对 **current revision** 按状态与能力直接暴露提交/通过/驳回 |
| Hub 已接线 `submitReviewOpen` + `MasterPackageHubDialogs`，但无任何控件置 `true`（死代码） | 有真实入口；清掉无用死接线或改为活跃路径（不得残留无引用死状态） |
| Hub / revision 旅程块 `:show-primary-cta="false"` | 在可写且步骤需要时启用 Primary CTA（提交审核等） |
| Dashboard `master-review` 待办 path 仅为 `/masters/{id}`，落 Hub 仍需再钻 revision approval | 深链带 `?workspaceTab=approval`，打开即可决策 |

| 行为域 | 摘要 |
| --- | --- |
| **MRR-01 Hub 审核动作** | current revision：DRAFT/REJECTED → Submit；PENDING_REVIEW → Approve/Reject（能力门控） |
| **MRR-02 Dashboard 深链** | master-review（及必要时 rework）待办 → revision approval Tab |
| **MRR-03 旅程 CTA** | Hub（及对齐的旅程块）Primary CTA 可用，触发提交/替换等既有动作 |
| **MRR-04 死代码清理** | 无入口的 `submitReviewOpen` 残留消除；对话框仅由真实 CTA 打开 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 变更母版 review 状态机 / 同人阻断 / 权限矩阵角色集合 | Out of scope — 复用既有 `submitReview` / `decideReview` |
| CE-U08 条款 Dashboard 待办 | 已交付；本片不改 |
| CE-K05 impact 面板真化 | Out of scope |
| 非 current revision 上允许提交/审批 | Out of scope — 仅 current |

---

## 2. Actor / Role

| Actor | 能力 | 说明 |
| --- | --- | --- |
| **MASTER_DESIGNER / 母版管理者** | `manageMasters` | Hub 提交审核；旅程 CTA；返工 |
| **母版审批人** | `reviewMasters` | Hub / 深链上通过或驳回 `PENDING_REVIEW` |
| **系统** | Hub actions + Dashboard `useWorkflowTasks` + revision workspace tabs | 暴露入口；fail-closed 隐藏无能力按钮 |

---

## 3. Goal

1. 用户在 **Master Package Hub** 即可对 **current revision** 完成提交审核或审批决策，无需先钻入 revision 详情 Tab 才发现按钮。  
2. Dashboard「Masters to review」待办一键进入 **approval** 工作区（`workspaceTab=approval`）。  
3. 母版设计师旅程 Primary CTA 在可写步骤可用（尤其 Submit review）。  
4. 删除或激活死代码，使 `submitReviewOpen` 不再处于「接线但不可达」状态。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U09-C1** | Hub 动作栏按 **master / current revision 状态** 与能力显示：`manageMasters` + (DRAFT\|REJECTED) → Submit for review；`reviewMasters` + PENDING_REVIEW → Approve + Reject。 | 计划卡 |
| **U09-C2** | Hub 提交/审批调用与 revision 详情**同一** store API（`submitReview` / `decideReview`），成功后刷新 Hub 详情与 revision lines。 | 现状代码 |
| **U09-C3** | Dashboard `kind=master-review` 待办 `path` 必须落到 **current revision detail**（或等价可决策面）且 query 含 **`workspaceTab=approval`**。推荐形态：`/masters/{masterId}/revisions/{currentRevisionLineId}?workspaceTab=approval`（若 current 缺失则退回 Hub 并仍尽量带可发现提示——实现固定一种并测）。 | 计划卡；用户 scope |
| **U09-C4** | `kind=master-rework` 深链应进入可返工面（Hub 或 current revision design）；若带 workspaceTab，不得错误停在只读 approval 且无替换入口。 | 可达性完整 |
| **U09-C5** | Hub 上 `MasterDesignerJourneyBlock`：**`showPrimaryCta` 在 `canWriteJourney` 时为 true**（移除强制 `false`）；CTA 映射既有 `submitReview` / `upload` 等 emit。 | 计划卡 |
| **U09-C6** | Revision 详情旅程块可保持 workspace 内按钮为主；本片不强制改 revision 页 `showPrimaryCta=false`，但 Hub 必须可达。 | 计划卡聚焦 Hub |
| **U09-C7** | 清理死代码：Hub 若保留 `submitReviewOpen`，必须有按钮/CTA 置 true；否则删除 dialog 接线与 dead ref。 | 计划卡 |
| **U09-C8** | 无 `manageMasters` / `reviewMasters` 时对应按钮不可见（fail-closed）；不因 UI 暴露而绕过后端授权。 | 权限矩阵 |
| **U09-C9** | **禁止：** 改 review 状态机；做条款审核；宣称 go-live。 | 计划卡 |

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录；可访问 masters 路由。  
- 目标母版存在 current revision line。  
- 既有 submit/decide review API 可用。

**Triggers**

- 打开 Master Package Hub。  
- 点击 Dashboard master-review / master-rework 待办。  
- 点击旅程 Primary CTA（Submit review 等）。

---

## 6. Primary journey

1. 设计师在 Hub 完成上传/占位后，点 Hub **Submit for review**（或旅程 CTA）→ 填写 change summary → 母版进入 `PENDING_REVIEW`。  
2. 审批人在 Dashboard Tasks 看到 Masters to review → 打开待办 → 落在 current revision **approval** Tab。  
3. 审批人在 Hub 或 approval Tab **Approve / Reject**。  
4. 若驳回：设计师从 rework 待办回到可替换/再提交面。

---

## 7. System responses（success）

| 表面 | 成功响应 |
| --- | --- |
| **Hub actions** | 条件满足时可见 Submit / Approve / Reject；点击打开既有对话框 |
| **Hub journey CTA** | `canWriteJourney` 时 Primary CTA 可见且可触发 submit/upload |
| **Dashboard deep link** | master-review 待办 URL 含 `workspaceTab=approval`；落地后 approval Tab 激活 |
| **授权** | 无能力者不渲染对应按钮；API 仍 fail-closed |

---

## 8. Acceptance scenarios

### BDD-CE-U09-MRR-001 — Hub 提交审核（current）

```gherkin
Given 会话持有 manageMasters
And 母版 current 状态为 DRAFT 或 REJECTED（可提交）
When 用户打开 Master Package Hub
Then 可见 Submit for review（或等价英文-first 文案）动作
When 用户提交合法 changeSummary
Then 调用既有 submitReview 成功
And 母版进入 PENDING_REVIEW
And Hub 不再展示 Submit（改由审批动作或只读等待态）
```

### BDD-CE-U09-MRR-002 — Hub 通过 / 驳回

```gherkin
Given 会话持有 reviewMasters
And 母版状态为 PENDING_REVIEW
When 用户打开 Hub
Then 可见 Approve 与 Reject
When 用户 Approve（或 Reject 并填原因）
Then 调用既有 decideReview 成功
And Hub 状态与 banner 刷新为结果态
```

### BDD-CE-U09-MRR-003 — 无能力 fail-closed

```gherkin
Given 会话无 manageMasters
When 打开可提交态母版 Hub
Then 不展示 Submit for review
Given 会话无 reviewMasters
When 打开 PENDING_REVIEW 母版 Hub
Then 不展示 Approve / Reject
```

### BDD-CE-U09-MRR-004 — Dashboard 深链 workspaceTab=approval

```gherkin
Given 会话持有 reviewMasters
And 存在 PENDING_REVIEW 母版（current revision 已知）
When 审批人打开 Dashboard Tasks 中 kind=master-review 待办
Then 导航 URL 包含 workspaceTab=approval
And 落地页为该母版 current revision 详情（或等价审批工作区）
And approval Tab 处于激活状态，可执行 Approve / Reject
```

### BDD-CE-U09-MRR-005 — 旅程 Primary CTA（Hub）

```gherkin
Given 设计师旅程在 Hub 可见且 canWriteJourney=true
And 当前步骤为 submitReview（或 upload/rework 等需 CTA 的步骤）
When 渲染 MasterDesignerJourneyBlock
Then Primary CTA 可见（showPrimaryCta 未强制关闭）
When 用户点击 Submit-review CTA
Then 打开提交审核对话框（或触发与 Hub Submit 同一路径）
```

### BDD-CE-U09-MRR-006 — 死代码不可残留

```gherkin
Given 本片交付完成
When 审查 Hub 中 submitReviewOpen / MasterSubmitReviewDialog 接线
Then 存在至少一处用户可达控件将对话框打开
And 不存在「仅定义 model / handler、无任何入口」的死接线
```

### BDD-CE-U09-MRR-007 — 非 current / PENDING 边界

```gherkin
Given 用户查看的是非 current revision 详情
When 渲染审核动作
Then 不提供对该历史 revision 的 Submit/Approve/Reject（保持既有 isCurrentRevision 门控）
Given 母版已 PENDING_REVIEW
When manageMasters 用户看 Hub
Then 不提供 Replace file 与 Submit（与既有 canReplaceFile / 提交门控一致）
```

---

## 9. Boundary / exception

- API 失败：ElMessage / LoadError 既有模式；对话框可保持打开或关闭（实现与现网 revision 页一致）。  
- currentRevisionLineId 未知：深链退化策略见 U09-C3；不得 404 死页无说明。  
- 英文-first i18n：新增文案走 locale。

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 组件单测 | Hub actions 可见性；journey CTA；workflow task path |
| E2E | Hub 提交；Dashboard 深链落地 approval；双品牌 UIUX 抽检 |
| 代码审查 | 无死 `submitReviewOpen` |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-U09 plan §4 | 目标行为 |
| Task Master **#84** | 执行任务 |
| permission-matrix §13.1.2 / master review | 能力与行为入口 |
| ADR-0018 master review | 状态语义 |
| CE-U08 | 条款闭环对照；本片仅母版可达性 |

---

## 12. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-u09-master-review-reachability.md
task_ids: ["#84", "CE-U09"]
scenario_ids:
  - BDD-CE-U09-MRR-001
  - BDD-CE-U09-MRR-002
  - BDD-CE-U09-MRR-003
  - BDD-CE-U09-MRR-004
  - BDD-CE-U09-MRR-005
  - BDD-CE-U09-MRR-006
  - BDD-CE-U09-MRR-007
```
