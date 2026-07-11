# BDD 行为规格：LR-C8 — Role onboarding tour

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-11  
**BDD ID**: `BDD-LRP-C8-TOUR-001`  
**来源任务**: [LRP Wave LR-C § LR-C8 — Role onboarding tour](../plan/detail/LRP-C-usability-deepening.md)  
**程序发现**: [launch-readiness-program.md](../plan/launch-readiness-program.md) Wave LR-C / RoleJourneyTimeline (P21)  
**依赖**: P21 `RoleJourneyTimeline` + `frontend/src/constants/roleJourneyDefinitions.ts`（**Done** — 复用，禁止分叉）  
**伴生**: `ManagementShell.vue` header-actions；Dashboard `#journey-section`；Element Plus `el-tour`（`element-plus@^2.9.1`）  
**Task Master / slice**: plan `LR-C8` / slice `lrp-c8-role-onboarding-tour` — **Task Master #34** (`in-progress`)  
**Worktree**: `D:/working/DGE-lrp-c8-role-onboarding-tour` · `feat/lrp-c8-role-onboarding-tour`

---

## 1. 概述

已登录管理会话在 **首次进入 ManagementShell**（该浏览器、该用户尚无 dismiss 标记）时，系统按 **角色旅程** 自动打开 **可跳过** 的引导 tour（Element Plus `el-tour`）。Tour 步骤 **复用** P21 `roleJourneyDefinitions` / `RoleJourneyTimeline` 的 per-role step 列表与既有 i18n `label`/`guidance` 键，**不**分叉步骤定义、**不**引入第三方 tour 库、**不**强制无法关闭。

用户可 **Skip**（关闭并持久记住不再自动弹出）、走完最后一步（同样持久）、或稍后通过 **Help 菜单 → Replay role tour** **无视 dismiss 标记**从第 1 步重播。

| 行为域 | 摘要 |
| --- | --- |
| **D1 角色步骤** | 从 `roleJourneyDefinitions` 解析当前会话主旅程角色的 step 数组；tour 步序 = 该数组顺序 |
| **D2 首次触发** | Shell 挂载 + 会话就绪 + 无 dismiss 标记 + 可解析 tour 角色 → 自动打开，定位 step 1 |
| **D3 跳过 / 记住** | Skip（及完成 tour）写入 per-user localStorage dismiss；之后 **不**自动弹出 |
| **D4 重播** | Help 菜单「Replay role tour」始终可从 step 1 打开（忽略 dismiss） |
| **D5 i18n / OA** | 步骤文案复用 `journey.roles.*`；tour 壳层控件新键 `onboardingTour.*`（en 基座 + zh-CN）；Bank OA；双品牌无逻辑分叉 |
| **D6 a11y** | 可键盘操作；不永久困住焦点；可跳过；焦点环符合 LR-C12 基线 |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 第三方 tour / intro.js / shepherd 等依赖 | **禁止** — 仅 Element Plus `el-tour` |
| 强制不可关闭 / 无 Skip | **禁止** |
| 硬编码步骤文案或分叉一份新的 step 列表 | **禁止** — 复用 `roleJourneyDefinitions` + 既有 `journey.roles.*.steps.*.label/.guidance` |
| 服务端 dismiss / 跨设备同步 | **禁止** — 仅浏览器 `localStorage`（与 C2 本地偏好同类） |
| 新后端 API / 权限矩阵变更 | **禁止** — FE-only |
| 改变 `RoleJourneyTimeline` 业务语义或 Dashboard 旅程索引算法 | Out of scope — 仅消费步骤定义与角色优先级 |
| 产品教学以外的 feature spotlight / changelog | Out of scope |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **模板作者（主验收）** | `TEMPLATE_AUTHOR` | 首次登录自动打开 author 旅程 step 1；Skip 持久 |
| **其它旅程角色** | `MASTER_DESIGNER` / `TEMPLATE_TESTER` / `TEMPLATE_APPROVER` / `GROUP_ADMIN` / `GLOBAL_ADMIN` / `AUDIT_ADMIN` | 各角色使用对应 `*JourneySteps` 定义 |
| **无旅程角色用户** | 会话无法解析出主 tour 角色 | **不**自动打开；Help 重播项禁用或 no-op + i18n 提示 |
| **系统（UI）** | `ManagementShell` + tour 宿主 + Help 菜单 | 触发、Skip、Replay、锚点 |
| **系统（存储）** | `localStorage` | per-user dismiss 标记 |

授权：本规格 **不** 扩大可见面；tour 仅引导已授权壳内 UI，不绕过 `visibleRoutes` / capabilities。

---

## 3. Goal

1. 首次登录（无 dismiss）的 `TEMPLATE_AUTHOR` 在 shell 挂载后看到 **author 旅程第 1 步** tour。  
2. Skip（或完成）后刷新/再登录 **不再**自动弹出；Help → Replay 仍可从 step 1 重播。  
3. 步骤内容与 Dashboard / 详情侧 `RoleJourneyTimeline` **同源**（定义 + i18n），无分叉。  
4. 仅 `el-tour`；English-first i18n；Bank OA；REDBC/GREENBC 无逻辑分叉。

---

## 4. 已确认决策（2026-07-11，behavior-spec-author 裁决）

| ID | 决策 |
| --- | --- |
| **C8-C1** | **技术选型**：仅 Element Plus **`el-tour`**（仓库已有 `element-plus@^2.9.1` ≥2.4）。**禁止**新增 tour/intro 第三方依赖。 |
| **C8-C2** | **作用面**：挂载于 `ManagementShell`（已登录管理壳）；未登录 / 无 shell → 无 tour。 |
| **C8-C3** | **步骤源（禁止分叉）**：tour 的 step 列表 = 与 `RoleJourneyTimeline` 相同的 `RoleJourneyStep[]`，来自 `frontend/src/constants/roleJourneyDefinitions.ts`（`templateAuthorJourneySteps` 等）。**禁止**复制一份平行数组。 |
| **C8-C4** | **步骤文案**：每步 `title` ← `t(step.labelKey)`；`description` ← `t(stepGuidanceKeyFromLabel(step.labelKey))`（即既有 `.guidance` 键）。Tour 壳层（Skip / Next / Finish / Don’t show again 文案 / Help / Replay / 空态）使用新命名空间 **`onboardingTour.*`**（en 基座先加，zh-CN 镜像）。 |
| **C8-C5** | **主 tour 角色解析（与 Dashboard 对齐 + AUDIT 兜底）**：复用 `useDashboardJourney` 优先级：① Cluster-one `resolvePrimaryClusterOneRole`（`MASTER_DESIGNER` > `TEMPLATE_AUTHOR` > `TEMPLATE_TESTER`）；② `TEMPLATE_APPROVER`（`showApproverJourney` 条件）；③ `GLOBAL_ADMIN`；④ `GROUP_ADMIN`（team-lead 条件）；⑤ 若仍无且会话含 `AUDIT_ADMIN`（或等价 audit 旅程可见能力）→ `AUDIT_ADMIN` + `auditAdminJourneySteps`。无匹配 → 无自动 tour。实现可抽取共享 `resolvePrimaryTourRole`，但 **不得**改变 Dashboard 既有显示逻辑 unless 纯抽取。 |
| **C8-C6** | **Dismiss 存储键**：`docgen.onboardingTour.dismissed.v1:{username}`，值为稳定标记（建议 `'1'`）。`username` = `ManagementSession.username`（稳定主体；禁止仅用 displayName）。跨浏览器/清站点数据后视为「首次」。 |
| **C8-C7** | **首次自动触发**：当 `ManagementShell` 挂载且会话就绪、可解析 tour 角色、且 **不存在** dismiss 标记时，打开 tour 并定位 **step index 0**（产品语义「step 1」）。触发时机：壳挂载后下一 tick / `requestAnimationFrame`（实现锁定；须等锚点可查询）。 |
| **C8-C8** | **Skip / Don’t show again / 完成**：<br>• **Skip** → 关闭 tour **并写入** dismiss（任务单验收「skip closes and persists」）。<br>• **Don’t show again**：与 Skip **同等持久化语义**（可为 Skip 的可见标签/辅助说明，或并列控件；不得出现「Skip 不持久、只有勾选才持久」的强制路径）。<br>• **走完最后一步（Finish）** → 关闭 **并写入** dismiss。<br>• **Esc / 点遮罩关闭**：视为 Skip → **同样写入** dismiss（避免反复强制弹出）。 |
| **C8-C9** | **强制禁止**：任何路径不得阻断核心导航或使 tour 无法关闭；无 dismiss 前用户必须总能 Skip/Esc。 |
| **C8-C10** | **Help 菜单 Replay**：在 shell `header-actions` 增加 **Help** 菜单（当前用户菜单仅 logout — 本切片引入 Help，不塞进 logout 项混淆）。至少一项：`onboardingTour.help.replay`（英：「Replay role tour」）。点击 → 打开 tour @ step 1，**忽略** dismiss 标记；**不**清除 dismiss（重播结束后若用户再 Skip/Finish/Esc，dismiss 仍保持或重新写入 — 反正自动触发仍被抑制）。无可解析角色时：菜单项 **disabled** 或点击后 i18n toast/inline，**不**打开空 tour。 |
| **C8-C11** | **锚点**：实现为 timeline / shell 稳定选择器。优先：当 `[data-journey-timeline]` 存在时，第 *i* 步 target = 第 *i* 个 `[data-journey-step]`（与 step 数组下标对齐）。若当前路由无 timeline（非 Dashboard），则：自动 tour **先导航到** `/dashboard`（或会话 `defaultRoute` 若已是 dashboard 等价）再开 tour；Replay 同理若缺锚点则导航到 dashboard 再开。可选补充 `data-tour-anchor="{role}:{stepId}"`（不得改变步骤语义）。 |
| **C8-C12** | **路由与默认落地**：E2E / 典型登录后 `TEMPLATE_AUTHOR` 落在可显示 journey 的 Dashboard（与现夹具一致）。实现不得依赖未文档化的隐藏路由。 |
| **C8-C13** | **多标签页**：dismiss 以 `localStorage` 为准；写入后同浏览器其它标签在下次 shell 挂载/检查时不再自动弹出（不做 BroadcastChannel 硬性要求）。 |
| **C8-C14** | **a11y**：tour 步骤控件可键盘到达；焦点可见；关闭后焦点回到合理壳控件（Help 触发器或主内容）；不与 LR-C6 palette / LR-C7 bell 永久抢焦点冲突（tour 打开时优先 tour；关闭后恢复）。建议 `data-testid`：`onboarding-tour`、`onboarding-tour-skip`、`onboarding-tour-next`、`onboarding-tour-finish`、`help-menu`、`help-menu-replay-tour`。 |
| **C8-C15** | **品牌 / 主题**：REDBC/GREENBC 仅 token 差异；tour 逻辑与文案键无品牌分叉。 |
| **C8-C16** | **E2E + UIUX 强制**：`frontend/e2e/LRP-C8-onboarding-tour.spec.ts`；manifest `frontend/e2e/evidence/LRP-C8-uiux-manifest.md`（双品牌）。 |
| **C8-C17** | **后端门禁**：本切片 **不要求** `mvn verify`（FE-only）。前端：`pnpm -C frontend lint && type-check && test && build`。 |
| **C8-C18** | **Vitest**：覆盖角色步骤构建、dismiss 读写、首次触发门控、Replay 忽略 dismiss、无角色时不打开。 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | Shell 挂载 + 会话就绪 + 无 dismiss + 有 tour 角色 | 自动打开 @ step 1 |
| T2 | 用户 Skip / Don’t show again / Esc / 遮罩关闭 | 关闭 + 写 dismiss |
| T3 | 用户 Next / 最后一步 Finish | 前进或关闭 +（Finish 时）写 dismiss |
| T4 | Help → Replay role tour | 打开 @ step 1（忽略 dismiss） |
| T5 | 有 dismiss 的后续登录 / 刷新 | **不**自动打开 |

---

## 6. Preconditions

- 用户已登录；`ManagementShell` 已挂载。  
- P21 `roleJourneyDefinitions` 与 Dashboard journey 可见（作者夹具）。  
- Docker 验收：`http://localhost:4173`（+ 后端登录依赖 `:8080`）。  
- E2E 可清除/注入 `localStorage` dismiss 键以模拟首次 / 回访用户。

---

## 7. Primary journey（成功路径）

1. 清除 `TEMPLATE_AUTHOR` 用户的 dismiss 标记；以该用户登录。  
2. Shell 挂载；如需则进入 Dashboard，timeline 可见。  
3. Tour 自动打开，当前为 author 旅程 **step 1**（`create` — 「Create template」/对应 guidance）。  
4. 用户点 **Skip** → tour 关闭；localStorage 写入 dismiss。  
5. 刷新或再次进入壳 → **不**自动打开。  
6. 打开 Help → **Replay role tour** → tour 从 step 1 再开。

---

## 8. 系统响应

### 成功

- Tour 可见（`data-testid="onboarding-tour"`）；步文案来自 i18n journey 键。  
- Skip/Finish/Esc 后 dismiss 键存在；自动触发停止。  
- Replay 无视 dismiss，从 step 1 开始。

### 失败 / 边界

| 条件 | 响应 |
| --- | --- |
| 无可解析 tour 角色 | 不自动打开；Replay 禁用或 i18n 提示 |
| Timeline 锚点暂不可用 | 导航至 `/dashboard` 后再开（C8-C11）；仍失败则不阻塞壳，可降级仅壳级锚点或跳过自动打开并记 console（测试锁定：优先 dashboard 导航） |
| `localStorage` 不可用 / 配额失败 | 无法持久时：本会话可 Skip 关闭；下次仍可能自动弹出（best-effort）；**不得**因存储失败卡住壳 |
| 未登录 | 无 shell → 无 tour |
| Tour 打开时用户打开 command palette / 通知铃铛 | 不崩溃；关闭 tour 后其它壳控件可用 |

---

## 9. 验收场景（Given / When / Then）

### BDD-LRP-C8-001 — 首次 TEMPLATE_AUTHOR 登录自动打开 step 1（任务单验收 #1）

**Given** 用户角色为 `TEMPLATE_AUTHOR`  
**And** 不存在 dismiss 键 `docgen.onboardingTour.dismissed.v1:{username}`  
**When** 登录成功且 ManagementShell 挂载（落地 Dashboard / journey 可见）  
**Then** tour 打开（`data-testid="onboarding-tour"`）  
**And** 当前为 author 旅程 **step 1**（对应 `templateAuthorJourneySteps[0].id === 'create'`）  
**And** 可见文案来自 `journey.roles.TEMPLATE_AUTHOR.steps.create.label`（及 guidance）

### BDD-LRP-C8-002 — Skip 关闭并持久（任务单验收 #1 续）

**Given** BDD-LRP-C8-001 中 tour 已打开  
**When** 用户点击 Skip（`data-testid="onboarding-tour-skip"`）  
**Then** tour 关闭  
**And** localStorage 写入 dismiss 键  
**When** 刷新页面或重新进入 ManagementShell（同用户）  
**Then** tour **不**自动打开

### BDD-LRP-C8-003 — Help 菜单 Replay 无视 dismiss（任务单验收 #2）

**Given** 回访用户（dismiss 已存在）且 ManagementShell 可见  
**When** 打开 Help 菜单并选择 Replay role tour  
**Then** tour 打开并从 **step 1** 开始  
**And** 不要求清除 dismiss 键（自动触发仍保持抑制）

### BDD-LRP-C8-004 — 完成 tour 亦写入 dismiss

**Given** 首次用户 tour 已打开  
**When** 用户 Next 至最后一步并 Finish  
**Then** tour 关闭且 dismiss 已写入  
**And** 之后不自动打开

### BDD-LRP-C8-005 — Esc / 遮罩关闭等同 Skip 持久

**Given** 首次用户 tour 已打开  
**When** 用户按 Escape（或点击 tour 遮罩关闭，若组件支持）  
**Then** tour 关闭且 dismiss 已写入

### BDD-LRP-C8-006 — 步骤源与 RoleJourneyTimeline 同源（TEMPLATE_AUTHOR）

**Given** `TEMPLATE_AUTHOR` tour 打开  
**When** 检查 tour 步数与顺序  
**Then** 步数 = `templateAuthorJourneySteps.length`（6）  
**And** 步 id 顺序为 `create` → `design` → `trialGenerate` → `submitTest` → `submitApproval` → `awaitGoLive`  
**And** 实现 **未**维护平行硬编码 step 数组（代码审查 / 单测锁定从 definitions import）

### BDD-LRP-C8-007 — 其它角色使用对应旅程（抽样 MASTER_DESIGNER）

**Given** 用户主角色解析为 `MASTER_DESIGNER` 且无 dismiss  
**When** shell 挂载触发自动 tour  
**Then** 步序对齐 `masterDesignerJourneySteps`（`upload`…`rework`）  
**And** step 1 对应 `upload`

### BDD-LRP-C8-008 — 角色优先级（Cluster-one）

**Given** 会话角色同时含 `MASTER_DESIGNER` 与 `TEMPLATE_AUTHOR`  
**When** 解析 tour 角色  
**Then** 使用 `MASTER_DESIGNER` 旅程（与 `resolvePrimaryClusterOneRole` 一致）

### BDD-LRP-C8-009 — 无旅程角色不自动打开

**Given** 会话无法解析 C8-C5 任一 tour 角色  
**When** shell 挂载  
**Then** tour **不**自动打开  
**And** Help → Replay 不打开空 tour（禁用或 i18n 提示）

### BDD-LRP-C8-010 — 不强制：Skip 始终可用

**Given** tour 打开于任一步  
**When** 检查控件  
**Then** Skip（或等价关闭）始终可激活  
**And** 用户可在不完成全部步骤的情况下关闭 tour

### BDD-LRP-C8-011 — 无第三方 tour 依赖

**Given** 前端依赖清单  
**When** 审查本切片引入  
**Then** **无**新增 intro.js / shepherd / driver.js 等；仅使用 Element Plus `el-tour`

### BDD-LRP-C8-012 — i18n English-first

**Given** tour 打开（含壳层按钮与 Help 菜单项）  
**When** 检查用户可见文案  
**Then** 步骤文案走既有 `journey.roles.*`；壳层走 `onboardingTour.*`  
**And** en 基座键齐全；zh-CN 镜像存在  
**And** 无硬编码散落中英文

### BDD-LRP-C8-013 — a11y 边界

**Given** tour 打开  
**When** 键盘操作 Next / Skip / Esc  
**Then** 可完成前进与关闭  
**And** 关闭后焦点回到壳内可聚焦控件（Help 或主内容）  
**And** 焦点可见（符合既有 focus-ring token）

### BDD-LRP-C8-014 — Replay 不清除 dismiss（自动仍抑制）

**Given** dismiss 已存在；用户经由 Replay 打开 tour 后再次 Skip  
**When** 刷新 shell  
**Then** 仍 **不**自动打开（dismiss 保持）

### BDD-LRP-C8-015 — 锚点：timeline 步进高亮

**Given** Dashboard 上 `[data-journey-timeline]` 可见且 tour 打开  
**When** 用户在 tour 中 Next 到第 2 步  
**Then** 高亮/指向第 2 个 `[data-journey-step]`（或等价 `data-tour-anchor`）  
**And** 不破坏 timeline 既有只读展示语义

### BDD-LRP-C8-016 — FE-only / 无权限矩阵变更

**Given** 本切片变更集  
**When** 审查范围  
**Then** 无新后端 API / Flyway；不修改 permission-matrix 能力键；不改变 fail-closed 授权

### BDD-LRP-C8-017 — Bank OA / 双品牌无逻辑分叉

**Given** REDBC 与 GREENBC  
**When** 打开 tour / Help  
**Then** 行为一致；仅主题 token 差异；UIUX manifest 双侧证据

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | `onboarding-tour` 可见性；step 文案；Skip/Replay |
| Storage | `localStorage['docgen.onboardingTour.dismissed.v1:{username}']` |
| Vitest | 角色解析、dismiss、触发门控、definitions 同源 |
| E2E | `frontend/e2e/LRP-C8-onboarding-tour.spec.ts`（至少覆盖 001–003） |
| UIUX | `frontend/e2e/evidence/LRP-C8-uiux-manifest.md` |
| 包审计 | 无新 tour 第三方依赖 |

---

## 11. Traceability

| 来源 | 引用 |
| --- | --- |
| Plan task | `docs/plan/detail/LRP-C-usability-deepening.md` § LR-C8 |
| Program | `docs/plan/launch-readiness-program.md` LR-C8 row |
| P21 | `RoleJourneyTimeline.vue` + `roleJourneyDefinitions.ts` |
| i18n skill | `.cursor/skills/i18n-english-first/SKILL.md` |
| OA skill | `.cursor/skills/frontend-oa-design/SKILL.md` |
| Slice | `lrp-c8-role-onboarding-tour` |

---

## 12. Open questions

**无阻塞问题。** 下列项已由 LRP-C8 任务单 + P21 定义 + 本规格 C8-C* 裁决关闭：

| 曾可能疑问 | 裁决 |
| --- | --- |
| Skip vs Don’t show again 是否两套持久化？ | **C8-C8** — 同等持久；Skip 必须 persist |
| Help 菜单位置？ | **C8-C10** — shell header-actions 新建 Help 菜单 |
| 多角色选谁？ | **C8-C5** — 对齐 Dashboard + AUDIT 兜底 |
| 非 Dashboard 路由如何锚点？ | **C8-C11** — 导航至 `/dashboard` 再开 |
| AUDIT_ADMIN 是否纳入？ | **C8-C5** — 作为无更高优先级时的兜底旅程 |

---

## 13. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/lrp-c8-role-onboarding-tour.md
task_ids: [LR-C8]
```

**Handoff：** BDD `ready` — next `frontend-engineer`（TDD + `el-tour`；遵循 C8-C1…C18）→ E2E / UIUX。

**E2E 最小映射建议**

| E2E 用例 | 场景 |
| --- | --- |
| fresh author login → tour step 1 → skip persists | BDD-LRP-C8-001, 002 |
| returning user → help replay from step 1 | BDD-LRP-C8-003 |
|（可选）finish dismisses / esc dismisses | BDD-LRP-C8-004, 005 |
