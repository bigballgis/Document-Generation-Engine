# BDD 行为规格：CORE-FORTRESS Phase F7 — Authoring UX

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-09  
**BDD ID**: `BDD-CORE-FORTRESS-F7-001`  
**来源**: CORE-FORTRESS 纲领 F7 + LRP Wave LR-C1/LR-C4 + ADR-0051（Proposed）+ 用户确认（2026-07-09）

---

## 1. 概述

F7 交付模板结构化编辑的 **两项核心可用性能力**：

| 工作流 | 要点 |
| --- | --- |
| **F7-B1 Dirty guard（未保存变更警告）** | 可复用 `useDirtyGuard` composable；路由离开 / 对话框关闭 / 浏览器关闭时提示 |
| **F7-B2 Side-by-side preview（并排预览）** | 结构化编辑器左侧 + final-chain 预览 artifact 右侧；stale 徽章 + 显式刷新 CTA |

**与 LRP / ADR 的关系**

| 来源 | F7 覆盖 |
| --- | --- |
| **LR-C1** Dirty-form guard | **F7-B1 完整实现**（F7 为 CORE-FORTRESS 交付入口） |
| **LR-C4** Side-by-side preview | **F7-B2 完整实现** |
| **LR-C2** Draft recovery | **Out of scope** — 后续 LRP 或 F7+ slice |
| **LR-C3** Undo/redo | **Out of scope** |
| **ADR-0051** Debounced auto-refresh | **Out of v1** — F7 v1 采用 stale badge + 显式刷新（见 F7-C4） |
| **P19-T05** Preview-vs-final comparison | **Done** — F7 复用现有 preview API/records，不新建渲染路径 |
| **CD-PIT-08** | 预览 pane 必须展示 **非权威** 边界 copy |

---

## 2. Actor / Role

| Actor | 说明 | 权限 / 场景 |
| --- | --- | --- |
| **模板作者 (TEMPLATE_AUTHOR)** | 在 dev-editor 编辑 structured content、bindings | `authorTemplates`；DRAFT/TESTING |
| **测试员（受限作者）** | dev-editor 下 DRAFT/TESTING 编辑 | `decideTests` 且无 full author |
| **系统（路由 / 浏览器）** | `onBeforeRouteLeave` + `beforeunload` | fail-closed：有 dirty 则拦截 |
| **预览渲染路径** | 既有 test-generate / preview refresh API | final-chain DOCX→PDF；rate limit 1 in-flight per template（ADR-0051） |

---

## 3. Goal

1. **B1**：作者在 structured editor（及首批 wired 面）有未保存编辑时，导航离开必须经确认；pristine 时零摩擦。
2. **B2**：作者在 authoring 工作区可同时看到编辑区与最新 preview artifact；结构变更后显示 stale 状态直至显式刷新。
3. **边界清晰**：预览 pane 文案声明 edit-preview 非发布权威证据（CD-PIT-08）。
4. **Bank OA + i18n**：REDBC/GREENBC 双品牌、响应式堆叠窄屏；全部用户可见字符串走 i18n key（en 优先）。

---

## 4. 已确认决策（2026-07-09）

| ID | 决策 |
| --- | --- |
| **F7-C1** | F7 v1 **首批 wiring**：`ControlledStructuredContentEditor.vue`、模板 metadata 编辑对话框；identity/API policy 表单 **可选 wave 2**（F7-T03b） |
| **F7-C2** | Dirty 定义：**结构化内容 JSON / 绑定 / 规则** 相对上次成功 save 或初始 load 有 diff；metadata 对话框独立 dirty |
| **F7-C3** | Guard 对话框三动作：**Stay**（取消导航）、**Discard**（放弃并继续）、**Save**（若上下文有 save handler 且可用 — structured editor **有** explicit save） |
| **F7-C4** | Side-by-side v1：**无** keystroke/debounce 自动刷新；结构变更 → **stale badge** + **Refresh now** CTA；ADR-0051 debounce 留待 ADR 接受后修订 |
| **F7-C5** | 预览 artifact 复用 **final-chain**（与 test-generate / `TemplatePreviewPanel` 相同 API）；**禁止** HTML 近似预览 |
| **F7-C6** | 布局：≥ `md`  breakpoint 左右分栏；`< md` 预览折叠/堆叠在编辑区下方（OA skill responsive） |
| **F7-C7** | 每模板 **最多 1** in-flight preview 生成；重复点击 Refresh 禁用按钮直至完成 |
| **F7-C8** | F7 依赖 F4 **Done**（LO pool/profile）与 F5 **Done** 后启动；与 F6 **可并行** |
| **F7-C9** | E2E + UIUX manifest **强制**（用户可见行为变更） |

---

## 5. 前置条件

- CORE-FORTRESS F1–F4 **Done**（final-chain 预览路径可靠）。
- F5 **Done** 或至少 preview/test-generate API 稳定。
- Docker 栈可访问：`http://localhost:4173`（E2E）。
- `ControlledStructuredContentEditor.vue` explicit-save 语义不变（LR-C2 draft 未启用）。

---

## 6. 主旅程

### 6.1 Dirty guard — 路由离开

1. 作者打开 structured editor，修改节点内容（未 save）。
2. 作者点击侧栏另一路由或 dev-workspace 另一 tab。
3. 系统弹出确认对话框（i18n title/body + Stay / Discard / Save）。
4. Stay → 留当前页，编辑保留；Discard → 导航继续，未保存丢失；Save → 触发 save → 成功则导航继续。

### 6.2 Side-by-side preview

1. 作者在 dev-editor authoring 子 tab；左侧 structured editor，右侧 preview pane。
2. 作者完成一次试生成或 Refresh，右侧展示 PDF/DOCX 预览（iframe/下载链接 per 现有 panel）。
3. 作者修改结构 → 右侧显示 **Stale** 徽章 + Refresh CTA；边界 copy 可见。
4. 作者点击 Refresh → 调用既有 preview 生成/刷新 → 徽章清除，artifact 更新。

---

## 7. 验收场景（Given / When / Then）

### F7-B1 — Dirty guard

#### BDD-F7-B1-001 — 未保存时路由拦截

**Given** 作者在 structured editor 有 dirty 状态  
**When** 点击管理侧栏另一菜单项（触发 `vue-router` 导航）  
**Then** 显示 `common.dirtyGuard.title`（或等价 key）确认对话框  
**And** 导航 **未** 立即发生

#### BDD-F7-B1-002 — Stay 保留编辑

**Given** BDD-F7-B1-001 对话框已打开  
**When** 用户选择 Stay  
**Then** 对话框关闭；当前路由不变；editor dirty 仍为 true

#### BDD-F7-B1-003 — Discard 放弃并导航

**Given** dirty editor  
**When** 用户选择 Discard  
**Then** 导航到目标路由完成；editor 状态丢弃

#### BDD-F7-B1-004 — Save 后导航（structured editor）

**Given** dirty editor 且 save API 可用  
**When** 用户选择 Save 且 save 成功  
**Then** dirty 清除；导航继续

#### BDD-F7-B1-005 — Pristine 无摩擦

**Given** editor 无变更（或 save 后）  
**When** 用户导航离开  
**Then** **无** 确认对话框

#### BDD-F7-B1-006 — 浏览器 tab 关闭

**Given** dirty editor  
**When** 用户关闭浏览器 tab / 刷新页面  
**Then** 浏览器原生 `beforeunload` 提示触发（best-effort；文案由浏览器控制）

#### BDD-F7-B1-007 — Metadata 对话框 dirty

**Given** 模板 metadata 编辑对话框打开且有未保存字段  
**When** 用户点击 Cancel 或对话框关闭  
**Then** 确认对话框出现；Stay 保持对话框打开

---

### F7-B2 — Side-by-side preview

#### BDD-F7-B2-001 — 分栏布局（宽屏）

**Given** 视口 ≥ md breakpoint 且用户在 authoring structured 子 tab  
**When** 页面渲染  
**Then** 左侧 structured editor 与右侧 preview pane **同时可见**  
**And** 符合 WorkspaceTabShell（子 tab 无 action rail 按钮）

#### BDD-F7-B2-002 — 窄屏堆叠

**Given** 视口 < md breakpoint  
**When** 页面渲染  
**Then** editor 全宽；preview 可折叠或在下方堆叠（per OA responsive 规则）  
**And** 无横向溢出/重叠

#### BDD-F7-B2-003 — Stale 徽章

**Given** 右侧已展示 preview artifact（timestamp T0）  
**When** 作者在左侧完成一次 structure mutation（未 refresh）  
**Then** preview pane 显示 stale 徽章（i18n `templates.authoring.previewStale`）  
**And** Refresh CTA 可点击

#### BDD-F7-B2-004 — 显式刷新清除 stale

**Given** stale 状态  
**When** 作者点击 Refresh now 且生成成功  
**Then** 右侧 artifact 更新；stale 徽章消失  
**And** 成功/失败 toast 使用既有 preview 消息 key

#### BDD-F7-B2-005 — CD-PIT-08 边界 copy

**Given** preview pane 可见  
**When** 作者查看 preview 区域  
**Then** 非权威边界说明文案可见（i18n key，en + zh-CN）  
**And** 文案 **不** 声称 preview 等同最终发布 artifact 的法律效力

#### BDD-F7-B2-006 — 无 in-flight 重复提交

**Given** 一次 preview 生成进行中  
**When** 作者再次点击 Refresh  
**Then** 按钮 disabled 或忽略重复点击  
**And** 仅一个 in-flight 请求

#### BDD-F7-B2-007 — 无 preview 时空状态

**Given** 从未生成 preview  
**When** authoring tab 打开  
**Then** 右侧显示 empty state + 引导试生成/刷新的 CTA（i18n）  
**And** 不报错

---

## 8. 边界与异常

| 场景 | 期望 |
| --- | --- |
| Save 失败（dirty guard Save 路径） | 停留当前页；显示 API error envelope；dirty 保持 |
| Preview 生成失败 | stale 保持；error toast；不 blank 左侧 editor |
| 无 author 权限 | 既有 fail-closed — 不显示 authoring / preview pane |
| LO 池饱和（F4） | 刷新失败显示 retryable error；不 hang UI |
| F6 并行 refactor | F7 在 F6 facade 稳定后 wiring；composable import 路径可适配 |

---

## 9. 可观测证据

| 证据 | 说明 |
| --- | --- |
| Vitest | `useDirtyGuard.test.ts`；side-by-side 组件测试 |
| Playwright | `frontend/e2e/CORE-FORTRESS-F7-dirty-guard.spec.ts`；`CORE-FORTRESS-F7-side-by-side-preview.spec.ts` |
| UIUX manifest | `frontend/e2e/evidence/F7-uiux-manifest.md` — REDBC + GREENBC |
| i18n | 新 keys 在 `en.ts` + `zh-CN.ts` |
| Docker E2E | `playwright.docker.config.ts` against `:4173` |

---

## 10. 追溯

| 文档 | 用途 |
| --- | --- |
| [F7 详细计划](../plan/detail/CORE-FORTRESS-f7-authoring-ux.md) | 任务分解 |
| [LRP-C usability deepening](../plan/detail/LRP-C-usability-deepening.md) | LR-C1/C4 来源 |
| [ADR-0051 side-by-side preview](../../adr/rendering-authoring/0051-side-by-side-authoring-preview.md) | 架构意图（v1 子集） |
| [CD-PIT-08](../plan/detail/CDP-industry-pitfall-registry.md) | 预览边界 |
| [Workspace tab shell](../../.cursor/rules/workspace-tab-shell-constitution.mdc) | 子 tab 无按钮 |

---

## 11. BDD readiness

**`ready`** — 规格完整；F7-C4 已裁决 v1 无 debounce 自动刷新。待 F5 Done 后 hand off `frontend-engineer` + `e2e-test-engineer` + `e2e-uiux-reviewer`。
