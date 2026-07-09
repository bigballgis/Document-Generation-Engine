# BDD 行为规格：CORE-FORTRESS Phase F6 — 前端内核重构

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-09  
**BDD ID**: `BDD-CORE-FORTRESS-F6-001`  
**来源**: CORE-FORTRESS 纲领 F6 + SOR-F01 部分完成基线（2026-07-04）+ 用户确认（2026-07-09）

---

## 1. 概述

F6 将模板详情 **前端内核** 从「巨型 composable + 视图」进一步拆分为 **按领域划分的可测 composable 簇**，使 bindings / lifecycle / preview / rules / navigation 等关注点各自独立，**不改变任何用户可见行为**。

| 工作流 | 要点 |
| --- | --- |
| **F6-A1 预览与测试 composable** | 从 `useTemplateDetailController` 提取 test-generate、batch-test、preview 选择与 loading 状态 |
| **F6-A2 生命周期 composable 细分** | 将 `useTemplateLifecycleActions`（~658 行）拆为 gate 加载、决策动作、对话框状态等子 composable |
| **F6-A3 导航与旅程 composable 细分** | 将 `useTemplateDetailNavigation`（~542 行）拆为 tab/route 同步、旅程展示、load shell |
| **F6-A4 可见性与能力门面** | 控制器仅保留 orchestration + re-export；单文件 ≤ **350** 行（composable）/ ≤ **450** 行（view） |
| **F6-A5 行为等价回归** | 现有 Vitest + E2E 全绿；无 API/路由/i18n key 变更 |

**与已完成工作的关系**

| 已有资产 | 状态 | F6 关系 |
| --- | --- | --- |
| SOR-F01 slice 1–3 | **Done** (2026-07-04) | 已提取 `useTemplateLifecycleActions`、`useTemplatePolicyCredentials`、`useTemplateDetailNavigation`；controller **1538 → 408** 行 |
| `useTemplateDetailController.ts` | **408 行** | F6-A1 继续瘦身；作为门面保留 |
| `TemplateDetailView.vue` | **~550 行** | F6 不强制拆分 view（除非 orchestration 可下沉）；优先 composable 拆分 |
| P12 UIUX deep refactor | **Done** | WorkspaceTabShell、journey 只读 — **行为不变** |
| LR-C / F7 authoring UX | **Not Started** | **Out of scope** — F7 负责 dirty guard 与 side-by-side preview |

---

## 2. Actor / Role

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **模板作者 (TEMPLATE_AUTHOR)** | 在 dev-editor / legacy 详情页编辑、预览、提交测试 | 旅程 CTA、authoring tab、test generate |
| **测试员 / 审批员 / 团队负责人** | 生命周期决策、证据查看 | lifecycle gates、journey 上下文 |
| **API 管理员** | 策略与凭证面板 | policy composable（已独立，F6 仅回归） |
| **前端工程师** | 维护 composable 边界 | 单测覆盖、文件行数、Bank OA 样式锁 |
| **系统（Vitest / Playwright）** | 行为等价证明 | 无 UI 回归、无路由契约漂移 |

---

## 3. Goal

1. **A1–A3**：每个领域 composable 职责单一、可独立单测；消除 >500 行的 template-detail composable。
2. **A4**：`useTemplateDetailController` 成为薄门面（目标 **≤ 250** 行），仅 wiring + 类型 re-export。
3. **A5**：用户旅程、tab 同步、生命周期动作、预览生成、绑定/规则面板数据流与 refactor 前 **逐字段等价**。
4. **零行为变更**：不新增 dirty guard、side-by-side preview、API 字段或权限规则（属 F7 / 后端 phase）。

---

## 4. 已确认决策（2026-07-09）

| ID | 决策 |
| --- | --- |
| **F6-C1** | F6 为 **纯结构重构**；BDD 场景仅证明 **行为等价**，不引入新 UX |
| **F6-C2** | 在 SOR-F01 三分 composable 基础上继续拆分；**不**回滚已提取模块 |
| **F6-C3** | 单 composable 软上限 **350 行**（不含 test）；超过则继续按子域拆分 |
| **F6-C4** | `useTemplatePolicyCredentials` 已满足大小 — **仅回归测试**，不重拆 |
| **F6-C5** | bindings / rules 逻辑若仍散落在 `TemplateDetailAuthoringTab` 或 panel 组件，F6 **不**强行上提到 composable，除非 duplicate 逻辑可 DRY 且无行为变化 |
| **F6-C6** | 公共 export surface（`useTemplateDetailController` return 对象 keys）**保持不变**；允许内部 import 路径变更 |
| **F6-C7** | Bank OA 样式锁：重构 **不得** 改动 layout/token/class；仅文件移动与逻辑拆分 |
| **F6-C8** | F6 **不含** E2E 新 journey（复用现有 `TemplateDetailView` / dev-workspace specs）；UIUX reviewer 可选（无视觉变更） |
| **F6-C9** | 依赖 F5 **Done** 后启动；与 F7 **可并行**（F7 改 authoring UX，F6 改内核结构 — 需协调 merge 冲突） |

---

## 5. 前置条件

- CORE-FORTRESS F1–F4 **Done**；F5 **Done** 或至少不阻塞前端编译。
- SOR-F01 composable 提取 **Done**（三 composable + 46 Vitest tests 基线）。
- 前端 gates 绿：`pnpm -C frontend lint && type-check && test && build`。
- `WorkspaceTabShell` / `templateDevWorkspaceTabs` 契约稳定。

---

## 6. 主旅程（行为等价 — 无变更）

### 6.1 作者 — 试生成预览

1. 作者打开模板 dev-editor → authoring 子 tab。
2. 选择测试数据集 → 点击试生成。
3. 系统调用既有 store/API → 跳转 testing 子 tab → 展示 preview 记录。
4. **F6 后**：步骤 1–4 的 UI 状态、消息 key、tab query 与 refactor 前一致。

### 6.2 测试员 — 提交测试决策

1. 测试员在 lifecycle/testing 面板查看 submit gate。
2. 提交通过/拒绝决策 → 对话框 → API → 刷新模板状态。
3. **F6 后**：gate 项、disabled 状态、对话框模式与 refactor 前一致。

---

## 7. 验收场景（Given / When / Then）

### F6-A1 — 预览与测试提取

#### BDD-F6-A1-001 — 试生成成功后 tab 与 preview 状态

**Given** 作者权限且模板处于 DRAFT/TESTING  
**When** 调用 `handleTestGenerate(testDataSetId)`  
**Then** `generatingPreview` 在请求期间为 true  
**And** 成功后 `lastPreview` / `selectedPreviewId` 更新  
**And** dev-workspace 激活 tab 为 `testing`  
**And** 成功 toast 使用 `templates.testGenerate.success`

#### BDD-F6-A1-002 — 批量试生成

**Given** 模板至少有一个 test data set  
**When** 调用 `handleBatchTestGenerate()`  
**Then** `batchTesting` 生命周期正确  
**And** `coverageRefreshToken` 递增  
**And** 成功/空数据集/失败消息 key 与 refactor 前一致

#### BDD-F6-A1-003 — 预览选择加载

**Given** 已有 previewId  
**When** 调用 `handlePreviewSelected(previewId)`  
**Then** `lastPreview` 经 API 刷新  
**And** `previewId === null` 时清空 `lastPreview`

---

### F6-A2 — 生命周期 composable 细分

#### BDD-F6-A2-001 — Publish gate 加载与展示

**Given** 模板可发布且用户有 publish 能力  
**When** 打开 publish 摘要 / 加载 publish gate  
**Then** `publishGateItems`、`publishGateReady`、`publishVersion` 与 API 响应一致  
**And** loading / error 状态与 refactor 前相同

#### BDD-F6-A2-002 — 生命周期决策提交流

**Given** 决策对话框已打开（approve/reject 等模式）  
**When** 调用 `submitLifecycleDecision`  
**Then** 正确 API 被调用；成功后 `loadTemplate` 刷新  
**And** 对话框关闭语义不变

#### BDD-F6-A2-003 — Governance 动作

**Given** STOPPED/PUBLISHED 等状态下的 governance 可见性  
**When** 触发 stop/restore/deprecate/delete  
**Then** 确认流、comment 对话框、成功/失败消息与 refactor 前一致

---

### F6-A3 — 导航与旅程

#### BDD-F6-A3-001 — Tab 与 route query 双向同步

**Given** 用户在 template detail（legacy 或 dev-editor）  
**When** 切换 detail tab 或 dev-workspace tab  
**Then** URL query（`tab` / `devTab` / `authoringTab`）与 `activeDetailTab` / `activeDevWorkspaceTab` 同步  
**And** 浏览器后退/前进恢复正确 tab

#### BDD-F6-A3-002 — Journey 上下文只读展示

**Given** 各角色 journey 可见性规则  
**When** 模板生命周期状态变化  
**Then** `showAuthorJourney` / `showTesterJourney` 等 computed 与 refactor 前一致  
**And** journey CTA handler 仍委托 lifecycle/navigation（Workspace Tab Shell：journey 无内嵌 CTA）

#### BDD-F6-A3-003 — 模板加载 shell

**Given** route `templateId` 变化  
**When** `loadTemplate` 执行  
**Then** skeleton / error / selected template 匹配逻辑不变  
**And** `loadFailed` 与 store error key 映射不变

---

### F6-A4 — 门面与文件大小

#### BDD-F6-A4-001 — Controller return shape 稳定

**Given** `TemplateDetailView` 消费 `useTemplateDetailController()`  
**When** F6 refactor 合并  
**Then** return 对象 **keys 集合与 refactor 前相同**（TypeScript 结构测试或 snapshot）  
**And** 无 consumer 需修改 props/emit（除 import 路径允许变更）

#### BDD-F6-A4-002 — Composable 行数预算

**Given** F6 完成  
**When** 统计 `frontend/src/views/templates/useTemplate*.ts`（不含 `.test.ts`）  
**Then** 无单文件超过 **350** 行  
**And** `useTemplateDetailController.ts` ≤ **250** 行

---

### F6-A5 — 回归 gates

#### BDD-F6-A5-001 — Vitest 全绿

**Given** F6 代码合并  
**When** `pnpm -C frontend test --run`  
**Then** 所有 template-detail composable 测试通过  
**And** 新增/迁移测试覆盖 extracted 模块（每个新 composable ≥ **8** cases）

#### BDD-F6-A5-002 — 构建与类型

**Given** F6 完成  
**When** `pnpm -C frontend lint && type-check && build`  
**Then** 0 errors；无新增 lint 抑制

---

## 8. 边界与异常（保持不变）

| 场景 | 期望（F6 不得改变） |
| --- | --- |
| 无权限用户 | lifecycle/authoring/policy 区块按 capability fail-closed 隐藏 |
| API 失败 | 仍使用 store `lastErrorMessageKey` → i18n；不泄露内部 stack |
| dev-editor 无 devVersionId | 既有 error/skeleton 行为保持 |
| 并发行程 | 无新增全局 mutable singleton；composable 仍 per-component 实例 |

---

## 9. 可观测证据

| 证据 | 说明 |
| --- | --- |
| Vitest 报告 | composable 单测 count ≥ SOR-F01 基线 + 新增模块覆盖 |
| `pnpm build` | 无 TS 断裂 |
| 可选 Playwright | 现有 template detail smoke 复跑 green |
| Git diff stat | 无 `.vue` 模板结构变更（除 import 路径） |
| 行数脚本 | composable 文件均 ≤ 350 行 |

---

## 10. 追溯

| 文档 | 用途 |
| --- | --- |
| [F6 详细计划](../plan/detail/CORE-FORTRESS-f6-frontend-kernel-refactor.md) | 任务分解 |
| [Program roadmap](../plan/detail/CORE-FORTRESS-program-roadmap.md) | F6 状态 |
| [SOR-F01 / system optimization review](../plan/system-optimization-review-2026-07.md) | 已完成的 slice 1–3 |
| [Workspace tab shell constitution](../../.cursor/rules/workspace-tab-shell-constitution.mdc) | 交互不变约束 |
| [Frontend OA design skill](../../.cursor/skills/frontend-oa-design/SKILL.md) | 视觉不变约束 |

---

## 11. BDD readiness

**`ready`** — 行为等价规格完整；F6 为结构重构，场景服务于 TDD 回归与 file-size 验收。待 F5 完成后 hand off `frontend-engineer`。
