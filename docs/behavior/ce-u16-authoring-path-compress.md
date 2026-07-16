# BDD 行为规格：CE-U16 — 创作路径压缩（Authoring path compression）

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U16-APC`  
**编写日期:** 2026-07-17  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U16  
**Slice:** `ce-u16-authoring-path-compress`  
**Task Master:** **#92**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u16-authoring-path-compress` · `feat/ce-u16-authoring-path-compress`  
**完成声明约束:** 关闭「design 默认落点偏离绑定主路径」与「新建后缺少精简创作引导」缺口；复用 U14/U15 `workspaceTab` / `designTab` / `testingTab` 约定；**不**改 CE-U15 生命周期 Stepper；**不**宣称 go-live；**不**激活 CD-3 / P3

---

## 1. 概述

模板创作主工作区（dev workspace → **Design**）今日默认子 Tab 为 **Variables**（`DEFAULT_TEMPLATE_AUTHORING_SUB_TAB = 'variables'`）。新建模板成功后仅跳转 Package Hub（`handleCreated` → `templateDetailPath`），作者需再手工进入 dev 并点到 Bindings。CE 计划卡 CE-U16 要求：**design 默认落点 bindings**；**新建后微向导**（母版 → 绑定 → 变量 → 预览步骤条）。

| 缺口（现状证据） | 目标 |
| --- | --- |
| 无 `designTab` 时落点 `variables` | 无显式 `designTab` 时默认 **Bindings** |
| 新建成功 → Hub，无创作引导 | 新建成功 → **dev workspace** + **Authoring path** 微向导步骤条 |
| 作者需猜「先母版确认还是先绑变量」 | 固定顺序：**Master → Bindings → Variables → Preview** |
| 与 U15 生命周期 Stepper 易混淆 | 微向导是**创作路径**导向，**不是**产品状态机 Stepper |

| 行为域 | 摘要 |
| --- | --- |
| **APC-01 Design 默认 Bindings** | `workspaceTab=design` 且无有效 `designTab`（及无有效 legacy `authoringTab`）时落地 **bindings** |
| **APC-02 新建后微向导** | `TemplateCreateDialog` 成功创建后进入带微向导的 dev workspace |
| **APC-03 步骤条导航** | 四步可点选；映射到既有 workspace / 子 Tab；不触发生命周期转移 |
| **APC-04 与 U14/U15 共存** | 显式深链 query 优先；生命周期 Stepper / Go fix 行为不变 |
| **APC-05 Fail-closed / 边界** | 无写权限不暴露误导写入口；可 Skip/Dismiss；不绕过授权 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-U15 生命周期 Stepper / publish-gate Go fix | 已交付；本片**不改**其语义与 DOM 契约（可并存） |
| 改变模板创建 API / 必填字段 / 母版选择规则 | Out of scope — 复用既有 create |
| 新增 design 子 Tab `master` 为永久第五子 Tab | Out of scope — Master 步为向导导向面，不永久改 `TEMPLATE_AUTHORING_SUB_TABS` 集合（除非实现选择把 Master 仅挂在向导态） |
| Content modules 纳入向导四步 | Out of scope — `contentModules` 仍可通过 Design 子 Tab 到达 |
| Clone published → new draft 强制同款向导 | Out of scope — 仅 **Create template** 对话框成功路径强制微向导；clone 仅享受默认 bindings 落点 |
| Import template 强制微向导 | Out of scope — import 保持既有 Hub 落地（仍受默认 bindings 约束当进入 design） |
| CE-U17 快捷键 / CE-U18–U21 / P3 | Out of scope |
| 宣称 go-live / 激活 CD-3 / 激活正式 P-phase | **禁止** |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **模板编排人员** | `authorTemplates` | 创建模板；跟随微向导完成 Master→Bindings→Variables→Preview；日常打开 design 默认见 Bindings |
| **只读 / 无编排权限** | 无 `authorTemplates` | 不得因向导或默认落点绕过写授权；不可见 Create 或不可完成创建 |
| **测试 / 审批 / 发布角色** | `decideTests` / `decideApprovals` / `publishTemplates` | 可打开 dev workspace；默认 bindings 与向导**不**改变其 testing/approval 深链；U14/U15 路径优先 |
| **系统** | create API + template detail + workspace query + i18n | 解析默认子 Tab；创建后导航；渲染微向导步骤条 |

---

## 3. Goal

1. 作者进入模板 **Design** 且 URL **未**指定有效 `designTab`（或等价 legacy）时，**第一眼落在 Bindings**（版式占位符绑定），而不是 Variables。  
2. 作者从模板目录 **Create template** 成功后，**一次导航**进入该模板 **dev workspace**，看到 **Authoring path** 微向导步骤条（Master → Bindings → Variables → Preview），并可按步到达对应工作面。  
3. 微向导**不是** U15 生命周期 Stepper：不表示 DRAFT→PUBLISHED；不承载 Submit/Approve/Publish CTA。  
4. 显式深链（U14 Tasks、U15 Go fix、旅程链接）继续精确落地；不得被默认 bindings 或向导覆盖。  
5. Formal phase 保持 **None**；不宣称 go-live；不激活 CD-3。

---

## 4. 已确认决策 vs 推导假设

### 4.1 已确认（产品 / 计划 / 既有交付）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U16-C1** | design **默认落点 bindings**。 | CE 计划 §4 CE-U16 简卡；Task Master #92 |
| **U16-C2** | **新建后**微向导步骤条顺序：**母版 → 绑定 → 变量 → 预览**。 | 同上 |
| **U16-C3** | 复用 `workspaceTab` / `designTab` / `testingTab` / `approvalTab` 约定（U14/U15）。 | handoff；`templateDevWorkspaceTabs` |
| **U16-C4** | **不是** U15 生命周期 Stepper；两套导向可同时存在、职责分离。 | handoff；U15 BDD 非目标声明 |
| **U16-C5** | Formal phase **None**；不宣称 go-live；不激活 CD-3；P3 parked。 | CE 计划 / Task Master |
| **U16-C6** | Journey / 进度类导向只读；写动作仍在 top-level tab **action rail**（Workspace Tab Shell）。 | [management-ui-constitution.md](../architecture/management-ui-constitution.md) |
| **U16-C7** | UI 用语 English-first（en 基线 + zh-CN）；业务词对齐 terminology（Bindings / Variables / Preview；母版用 **Master**）。 | i18n-english-first；business-terminology-guide |

### 4.2 本片确认的实现决策（计划卡薄 → 仓库事实推导）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **U16-D1** | **默认子 Tab：** 将无 query 时的 design 默认从 `variables` 改为 **`bindings`**（`DEFAULT_TEMPLATE_AUTHORING_SUB_TAB` 或等价单一默认源）。`resolveDesignSubTabFromQuery` / `resolveTemplateAuthoringSubTab` 对**无效/缺失**值回落 bindings。 | U16-C1；现状 `templateAuthoringSubTabs.ts` |
| **U16-D2** | **显式 query 优先：** 合法 `designTab`（或 legacy `authoringTab` 非 `testPreview`）始终覆盖默认。U15 Go fix → `designTab=variables` 等深链**不得**被改成 bindings。 | U16-C3 |
| **U16-D3** | **主表面：** 模板 **dev workspace**（`/templates/{id}/dev/{devVersionId}`）。Hub 不是创作微向导主表面。 | 创作发生在 dev；现状 create 落 Hub 为缺口 |
| **U16-D4** | **新建成功导航：** `TemplateCreateDialog` → create API 成功后，导航到 **dev workspace**（需 `devVersionId`，来自 create/detail 响应），并**激活**微向导（建议 query 标记如 `authoringGuide=1`，实现固定一种并测）。**不再**以「仅 Hub」作为创建成功主路径。 | U16-C2；压缩路径 |
| **U16-D5** | **微向导名称（English-first）：** **Authoring path**（步骤条）。稳定 `data-testid`：`authoring-path-guide`（或等价）；**禁止**复用 `lifecycle-stepper` testid。 | 与 U15 区分 |
| **U16-D6** | **四步标签与落点：** | U16-C2 + 现有 Tab 模型 |
| | 1. **Master** — 只读导向：展示已绑定母版标识（名称/链接）+ 锚点清单摘要（数量与/或有序列表）。**不**要求打开母版修订工作区做编辑；可提供「Open master」只读链接。实现可将本步做成向导专用面板（不必永久加入 Design 子 Tab 枚举）。 | 创建已选 master；CE-U06 锚点上下文可复用只读数据 |
| | 2. **Bindings** — `workspaceTab=design&designTab=bindings` | |
| | 3. **Variables** — `workspaceTab=design&designTab=variables` | |
| | 4. **Preview** — `workspaceTab=testing&testingTab=previewRuns`（或与既有预览主入口等价且单测锁定的 testing 子 Tab）。**不**把已迁出的 design `testPreview` 子 Tab 复活为默认。 | U15/F6 已将 preview 归 testing |
| **U16-D7** | **步骤条交互：** 可点击跳到对应步并导航；高亮当前步。属**导向**控件：无 Submit for test / Approve / Publish。可选 **Next** / **Skip guide** / **Dismiss**（English-first）。Skip/Dismiss 后隐藏步骤条，用户仍可手动使用 Design/Testing Tabs。 | U16-C6 |
| **U16-D8** | **何时显示微向导：** 仅 **Create template** 成功进入的引导会话（U16-D4）。日常打开已有模板 **不**自动弹出（除非显式带引导 query）。Dismiss 后同模板本次引导不再自动出现（session 或 per-template dismiss；实现固定并测）。 | 「新建后」字面 |
| **U16-D9** | **`contentModules`：** 仍为 Design 合法子 Tab；**不**出现在四步条中；默认落点变更不得破坏 `designTab=contentModules` 深链。 | 最小范围 |
| **U16-D10** | **Clone / Import：** 不强制微向导。进入 design 无 `designTab` 时仍默认 bindings（U16-D1）。 | 非目标表 |
| **U16-D11** | **与 U15 Stepper 并存：** 顶栏可同时有 Lifecycle Stepper（U15）与 Authoring path 条（U16）；视觉层级不互相覆盖 action rail；窄屏可堆叠但须 @1920 可验收。 | U16-C4 |
| **U16-D12** | **i18n：** 步骤名、Skip/Dismiss/Next、Master 面板说明 English-first（`en` + `zh-CN`）。避免对用户暴露内部词 `designTab` / `authoringGuide`。 | U16-C7 |
| **U16-D13** | **不改** OpenAPI create 契约（除非 create 响应缺少 `devVersionId` 导致无法直达 — 若缺失允许 create 后 `GET` detail 再导航；验收只要求到达 dev+向导）。不改权限矩阵角色集合。 | 最小改动 |
| **U16-D14** | Vitest 覆盖默认解析与 query 优先；E2E 覆盖创建→向导→至少 Bindings 与 Preview 一步。 | TDD/E2E 门禁 |

### 4.3 非确认假设（不得升格为需求）

| 项 | 状态 |
| --- | --- |
| 微向导用 `el-steps` 还是 OA 自定义条 | 实现自选；双品牌 @1920 可验收即可 |
| Dismiss 存 `sessionStorage` vs `localStorage` vs 仅清 query | 实现自选；须满足 U16-D8 |
| Master 步是否内嵌 CE-U06 同款位置高亮列表组件 | 可选复用；验收只需母版标识 + 锚点摘要可读 |
| 向导完成后是否 toast「Authoring path complete」 | 非必须 |

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录且持有 `authorTemplates`（创建路径）。  
- 存在至少一个同组 **APPROVED** 母版可供创建选择。  
- 目标模板具备可打开的 `devVersionId`（创建后）。

**Triggers**

- 打开 / 刷新 dev workspace 且 `workspaceTab=design`（或默认 design）且无有效 `designTab`。  
- `TemplateCreateDialog` 提交成功。  
- 用户点击 Authoring path 某一步 / Next / Skip / Dismiss。

---

## 6. Primary journey

1. 作者在 Templates 目录打开 **Create template**，选择 group + approved master，填写 externalId/name，提交成功。  
2. 系统关闭对话框，导航至 `/templates/{id}/dev/{devVersionId}`，显示 **Authoring path** 步骤条，当前步 **Master**。  
3. 作者确认母版与锚点摘要后进入 **Bindings**（或点步骤条 / Next）→ Design/Bindings，开始配置占位符绑定。  
4. 进入 **Variables** → 声明/调整变量 schema。  
5. 进入 **Preview** → Testing / preview runs，可发起预览（既有预览动作；本片不改生成引擎）。  
6. 作者 Skip/Dismiss 或走完引导后，步骤条隐藏；之后再开该模板 design 时默认仍落 **Bindings**（无显式 query 时）。

---

## 7. System responses（success）

| 表面 | 成功响应 |
| --- | --- |
| **UI — Design 默认** | 无有效 `designTab` → Bindings 面板可见；URL 可规范化写入 `designTab=bindings`（若实现会 sync query） |
| **UI — 微向导** | `authoring-path-guide` 可见；四步顺序固定；当前步高亮 |
| **UI — 步骤导航** | 点击 Bindings/Variables/Preview 更新 workspace/子 Tab 与内容面 |
| **导航 — 创建后** | 到达 dev workspace 而非停在仅 Hub |
| **i18n** | English-first 步骤与控件文案 |
| **授权** | 向导与默认落点不授予额外写权限；无权限写操作仍 fail-closed |
| **回归** | U15 `lifecycle-stepper` 与 Go fix；U14 Tasks 深链保持 |

---

## 8. Acceptance scenarios

### BDD-CE-U16-APC-001 — Design 无 designTab 时默认 Bindings

```gherkin
Given 会话可打开模板 TPL-DRAFT 的 dev workspace
When 用户导航到 /templates/{id}/dev/{devVersionId} 且未带有效 designTab（可仅 workspaceTab=design 或省略子 Tab）
Then Design 工作区激活的子面为 Bindings
And Bindings 面板内容可见（data-testid 或稳定角色名可测）
And 不得默认落在 Variables（除非 URL 显式 designTab=variables）
```

### BDD-CE-U16-APC-002 — 显式 designTab 优先于默认

```gherkin
Given 同一模板 dev workspace
When 用户打开 ?workspaceTab=design&designTab=variables
Then 激活面为 Variables
When 用户打开 ?workspaceTab=design&designTab=contentModules
Then 激活面为 Content modules（或等价条款引用面）
And 默认 bindings 策略不得覆盖上述显式 query
```

### BDD-CE-U16-APC-003 — 新建成功进入微向导（Master 起步）

```gherkin
Given 会话持有 authorTemplates
And 存在可用 APPROVED 母版
When 用户通过 Create template 成功创建模板
Then 导航到该模板 /dev/{devVersionId}（不得以「仅停留在 Package Hub」为成功主路径）
And Authoring path 微向导可见（data-testid=authoring-path-guide 或等价）
And 当前步为 Master
And 可见已绑定母版标识（名称或稳定链接）
And 可见锚点摘要（至少锚点数量或有序列表之一）
And 该控件不是 lifecycle-stepper（testid 与语义均不同）
```

### BDD-CE-U16-APC-004 — 微向导步：Bindings / Variables / Preview

```gherkin
Given 新建后微向导已显示
When 用户激活步骤 Bindings（或 Next 至 Bindings）
Then URL/状态为 workspaceTab=design 且 designTab=bindings
And Bindings 面板可见
When 用户激活步骤 Variables
Then designTab=variables 且 Variables 面板可见
When 用户激活步骤 Preview
Then workspaceTab=testing 且 testingTab 为 previewRuns（或本片锁定的等价预览子 Tab）
And 预览工作面可达（既有 Generate/Refresh 入口可见性按角色与状态）
```

### BDD-CE-U16-APC-005 — Skip / Dismiss 隐藏向导且不挡工作

```gherkin
Given 微向导可见
When 用户选择 Skip guide 或 Dismiss（English-first 控件）
Then authoring-path-guide 不再显示
And 用户仍可使用 Design / Testing / Approval 顶栏 Tab
And 无 designTab 时 Design 默认仍为 Bindings
```

### BDD-CE-U16-APC-006 — 微向导无生命周期 CTA

```gherkin
Given Authoring path 微向导可见
Then 步骤条上不出现 Submit for test / Approve / Reject / Confirm go-live / Publish
And 点击步骤不得改变 lifecycleStatus
```

### BDD-CE-U16-APC-007 — 日常打开不强制向导

```gherkin
Given 模板已存在且非「本会话刚 Create 成功」引导
When 用户从 Hub 打开 Open editor / design（无 authoringGuide 类引导标记）
Then 不自动显示 Authoring path 微向导
And Design 无 designTab 时仍默认 Bindings
```

### BDD-CE-U16-APC-008 — 回归：U15 Go fix 与 U14 Tasks 深链

```gherkin
Given U15 publish-gate Go fix 与 U14 Tasks 深链已交付
When 用户激活 VARIABLE_SCHEMA 的 Go fix
Then 仍落到 designTab=variables（不被默认 bindings 覆盖）
When 用户打开 Dashboard Tasks 的 TEST / APPROVAL / PENDING_RELEASE 待办
Then 仍分别落到 testing / submitApproval / publishReadiness
And lifecycle-stepper 行为保持
```

### BDD-CE-U16-APC-009 — Fail-closed：无创建权限

```gherkin
Given 会话无 authorTemplates
When 用户查看 Templates 目录
Then 不提供可用的 Create template 主路径（或提交被拒绝）
And 不得因深链伪造创建成功态
```

### BDD-CE-U16-APC-010 — 非目标与完成约束

```gherkin
Given 本片交付完成
Then 未宣称 go-live
And 未激活 CD-3 / 正式 P3
And 未将 Content modules 强制插入四步向导
And Clone/Import 未被迫显示微向导（默认 bindings 仍适用）
```

---

## 9. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| create 成功但短暂缺少 `devVersionId` | 允许 refetch detail 再进 dev；失败则可见错误 + 可退回 Hub；不得静默丢模板 |
| 母版锚点列表加载失败 | Master 步仍显示母版标识；锚点区错误/空态可 Retry；不阻断进入 Bindings |
| 无效 `designTab` 值 | 回落 **bindings**（新默认） |
| legacy `authoringTab=testPreview` | 仍映射到 **testing** workspace（既有 `resolveTemplateDevWorkspaceTabFromQuery`）；不把它当 design 默认 |
| 只读会话打开 design | 可见 Bindings 只读/既有空态；不出现误导性 Save |
| 双品牌 / 窄屏 | @1920 主验收；步骤条不得永久遮挡 tab action rail |
| 与 U15 Stepper 同时存在 | 两者可同屏；职责与 testid 分离 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | Design 默认 Bindings；`authoring-path-guide`；创建后 URL 含 `/dev/` |
| Vitest | 默认子 Tab=`bindings`；显式 query 优先；向导步→query 映射 helper |
| E2E + UIUX | 至少 APC-001 + APC-003 + APC-004（或 005）；UIUX 双品牌 @1920；Critical=0 |
| 回归 | U14 Tasks；U15 Stepper + Go fix；create 校验；contentModules 深链 |
| 非目标 | 无 go-live 宣称；向导 ≠ lifecycle-stepper |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U16 | 目标行为（P2·M 简卡） |
| Task Master **#92** | 执行任务；slice `ce-u16-authoring-path-compress` |
| [ce-u15-lifecycle-stepper.md](./ce-u15-lifecycle-stepper.md) | 区分生命周期 Stepper；深链约定依赖（#91 Done） |
| [ce-u14-dashboard-lifecycle-todos.md](./ce-u14-dashboard-lifecycle-todos.md) | 深链不得被默认覆盖（#90 Done） |
| [management-ui-constitution.md](../architecture/management-ui-constitution.md) | Workspace Tab Shell；导向 vs action rail |
| `templateAuthoringSubTabs` / `templateDevWorkspaceTabs` | 默认与 query 解析 |
| `TemplateCreateDialog` / `useTemplateListCatalog.handleCreated` | 创建后导航缺口 |
| [ce-u06-master-anchor-context.md](./ce-u06-master-anchor-context.md) | Master 步可复用锚点只读摘要思路 |
| CE-U17+ / CD-3 / P3 | 后续或 parked；本片不实现 |

---

## 12. 现状 → 实现提示（非额外需求）

| 发现 | 路径提示（供 plan / 实现） |
| --- | --- |
| `DEFAULT_TEMPLATE_AUTHORING_SUB_TAB = 'variables'` | 改为 `'bindings'`；更新 `templateAuthoringSubTabs.test.ts` / `resolveDesignSubTabFromQuery` 回落断言 |
| `handleCreated` → Hub only | 改为 fetch/`devVersionId` → `templateDevVersionPath(..., { workspaceTab:'design', designTab:'bindings', authoringGuide:'1' })` |
| Design 子 Tab 无 master | Master 步用向导面板组件，避免强行改 `TEMPLATE_AUTHORING_SUB_TABS` |
| Preview 已在 testing | 向导第 4 步切 `workspaceTab=testing` |
| U15 E2E 使用 `lifecycle-stepper` | 新 testid 分离；更新任何「默认 variables」旧断言 |
| 术语 | UI：**Authoring path** / **Master** / **Bindings** / **Variables** / **Preview**；**Skip guide** |

---

**bdd_readiness: ready**  
**open_questions: []**  
**frontend_ui_in_scope: true**  
**owning_doc:** `docs/behavior/ce-u16-authoring-path-compress.md`  
**task_ids:** ["92"]
