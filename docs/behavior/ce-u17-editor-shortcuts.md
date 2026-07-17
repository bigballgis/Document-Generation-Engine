# BDD 行为规格：CE-U17 — 编辑器快捷键（Ctrl+S / Ctrl+P + 命令面板作者动作）

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U17-EKS`  
**编写日期:** 2026-07-17  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U17  
**Slice:** `ce-u17-editor-shortcuts`  
**Task Master:** **#96**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u17-editor-shortcuts` · `feat/ce-u17-editor-shortcuts`  
**batch_recommendation:** **solo** `#96` `ce-u17-editor-shortcuts`  
**依赖规格:** [LR-C6 global command palette](./lrp-c6-command-palette.md)（Ctrl/Cmd+K 壳级面板；本片扩展作者动作条目）；[LR-C3 editor undo/redo](./lrp-c3-editor-undo-redo.md)（Z/Y 不冲突）；[CE-U21 draft key + concurrency](./ce-u21-draft-anchor-concurrency.md)（Save 路径含乐观锁 UX，本片不改语义）  
**完成声明约束:** 关闭「绑定编辑面无 Ctrl+S / Ctrl+P、命令面板无作者动作」缺口；**不**宣称 go-live；**不**激活 CD-3；**不**做 CE-U19 / CE-E03 / CE-G05 / CE-O02；**不**触碰 #50 Vitest 专项

---

## 1. 概述

模板作者在 **Authoring Bindings** 侧栏编辑器（`TemplateAuthoringBindingEditor` + `AuthoringPreviewPane`）中今日只能靠鼠标点击 **Save** 与 **Refresh now**。CE 计划卡 CE-U17 要求：

1. **Ctrl+S**（macOS **Cmd+S**）触发与工具栏 Save **同一**绑定保存路径；  
2. **Ctrl+P**（macOS **Cmd+P**）触发与预览面板 **Refresh now** **同一**预览刷新路径；  
3. **命令面板**（既有 LR-C6 Ctrl/Cmd+K）在作者处于该编辑面时 **注册可发现的作者动作**（至少 Save binding / Refresh preview），键盘可选中执行。

| 缺口（现状证据） | 目标 |
| --- | --- |
| 绑定编辑工具栏仅按钮 Save；无文档级 Ctrl/Cmd+S | 快捷键 → `handleSaveBinding`（或等价 emit `save`） |
| 预览面板仅按钮 Refresh now；浏览器 Ctrl/Cmd+P 会打印 | 快捷键 → `handlePreviewRefresh` / `preview-refresh`；`preventDefault` |
| LR-C6 palette 仅 `route` / 三目录实体；无作者动作 | 在编辑面上下文注册 Actions 分组；Enter 执行后关闭面板 |
| 焦点陷阱 / 只读角色未定义与快捷键关系 | 明确抑制与 fail-closed 边界 |

| 行为域 | 摘要 |
| --- | --- |
| **EKS-01 Save shortcut** | Ctrl/Cmd+S → 保存当前锚点绑定（与 Save 按钮同路径） |
| **EKS-02 Preview refresh shortcut** | Ctrl/Cmd+P → 刷新预览（与 Refresh now 同路径） |
| **EKS-03 Palette author actions** | Ctrl/Cmd+K 面板列出并执行作者动作（至少两项） |
| **EKS-04 Focus / modal suppress** | 命令面板或其它 modal 焦点陷阱打开时不误触保存/刷新 |
| **EKS-05 Fail-closed** | 无编辑权 / 非编辑面：不暴露突变动作；不绕过授权 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-U19 依赖只读视图 | Out of scope（#97） |
| CE-E03 / CE-G05 / CE-O02 / CD-3 / go-live | Out of scope / parked |
| #50 Vitest 专项治理 | Out of scope |
| 改变绑定 upsert / CE-U21 乐观锁 / LR-C2 草稿契约 | **禁止** — 快捷键仅触发既有 handler |
| 改变 LR-C6 实体搜索 / `visibleRoutes` / C5 API | Out of scope — 仅叠加 Actions |
| 全局（非模板 authoring 编辑面）Ctrl+S/P 改写浏览器行为 | Out of scope — 仅在本片作用面激活 |
| 变量树 Save / 测试集对话框 Save / 母版 displayLabel Save | Out of scope — 本片仅 **anchor binding** Save |
| 新增后端端点或 OpenAPI 变更 | **禁止**（纯前端编排） |
| 自定义快捷键用户设置页 | Out of scope |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **模板作者** | `TEMPLATE_AUTHOR`（及具备 `authorTemplates` 的等价角色） | 在 DRAFT/TESTING Authoring Bindings 编辑面使用 Ctrl/Cmd+S、Ctrl/Cmd+P；在 palette 执行作者动作 |
| **测试员（可预览）** | 可打开同一编辑面且 Refresh now 本就可点 | 可使用 **Refresh preview** 快捷键/动作；**Save binding** 仅当其具备绑定写权限且 Save 按钮可达时可用 |
| **只读 / 无编排写权限** | 无 `authorTemplates`（或编辑器 readonly） | **不**启用 Save 快捷键与 Save palette 动作；不绕过 403 |
| **任意管理会话** | 已登录 ManagementShell | 仍可 Ctrl/Cmd+K 打开全局 palette（LR-C6）；作者动作仅在编辑面上下文出现 |
| **系统（UI）** | 绑定编辑器 + 预览面板 + CommandPalette | 监听快捷键、`preventDefault`、注册/过滤动作、焦点陷阱协调 |

授权：本规格 **不** 改变 permission-matrix；Save/Refresh 权限与现有按钮一致。

---

## 3. Goal

1. 作者在绑定编辑面可用键盘 **保存绑定** 与 **刷新预览**，无需鼠标点工具栏/预览按钮。  
2. 快捷键与对应按钮调用 **同一业务路径**（含 CE-U21 冲突 UX、预览 loading 防重入、成功/失败 toast）。  
3. 作者可通过 **命令面板** 发现并执行上述动作（English-first 标签 + 快捷键提示）。  
4. 在命令面板或其它 modal 焦点陷阱打开时，**不**误触发 Save/Refresh；浏览器默认 Save/Print 在作用面内被拦截。  
5. Formal phase 保持 **None**；不宣称 go-live；不激活 CD-3。

---

## 4. 已确认决策 vs 推导假设

### 4.1 已确认（产品 / 计划 / 既有交付）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U17-C1** | **Ctrl+S** 保存绑定。 | CE 计划 §4 CE-U17；Task Master #96 |
| **U17-C2** | **Ctrl+P** 刷新预览。 | 同上 |
| **U17-C3** | **命令面板注册作者动作**。 | 同上 |
| **U17-C4** | P3·S；依赖 #95 Done；solo leaf `ce-u17-editor-shortcuts`。 | Task Master / batch_recommendation |
| **U17-C5** | Formal phase **None**；不宣称 go-live；不激活 CD-3；U19/E03/G05 out of scope。 | CE 计划 / handoff |
| **U17-C6** | 用户面 → E2E + UIUX；银行 OA；English-first i18n。 | delivery constitutions |
| **U17-C7** | macOS 使用 **Cmd**（Meta）等价于 Windows/Linux **Ctrl**（与 LR-C3/C6 一致）。 | 既有快捷键规格 |

### 4.2 本片确认的实现决策（计划卡薄 → 仓库事实推导）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **U17-D1** | **作用面（快捷键激活）：** 模板 Authoring **绑定编辑态**已挂载时（`TemplateAuthoringBindingEditor` 可见，建议根 `data-testid` 锁定，如 `binding-editor` 或现有稳定选择器）。列表态 / 其它 Design 子 Tab / 非模板页 **不**激活 Ctrl/Cmd+S 与 Ctrl/Cmd+P 的作者语义。 | 计划「编辑器」= 绑定编辑器；按钮仅在该面存在 |
| **U17-D2** | **Ctrl/Cmd+S：** 调用与工具栏 Save **同一** handler（`emit('save')` → `handleSaveBinding` → `saveBindingDraft`）。`preventDefault` + `stopPropagation`（或等价）避免浏览器「保存网页」。保存中（`submitting`）再按 → **忽略**（与按钮 loading 一致）。 | U17-C1；现有 Save 按钮 |
| **U17-D3** | **Ctrl/Cmd+P：** 调用与 `data-testid="authoring-preview-refresh"` **同一** handler（`preview-refresh` → `handlePreviewRefresh`）。`preventDefault` 避免浏览器打印。`previewRefreshing` 为 true 时忽略（与按钮 disabled/loading 一致）。 | U17-C2；`AuthoringPreviewPane` |
| **U17-D4** | **不改业务语义：** 成功 toast、失败 toast、CE-U21 `bindingVersionConflict` Reload/Keep editing、预览 `testGenerate` 参数（含 `selectedTestDataSetId`）均与按钮路径一致。 | 最小改动；U21 已交付 |
| **U17-D5** | **命令面板作者动作（最少集合）：** | U17-C3；复用 LR-C6 壳 |
| | 1. **Save binding** — 仅当 U17-D1 编辑态且会话可保存绑定时列出；执行 = U17-D2 | |
| | 2. **Refresh preview** — 仅当 U17-D1 编辑态且 Refresh now 可达时列出；执行 = U17-D3 | |
| | 分组 id 建议 `actions`；`PaletteItemKind` 扩展 `'action'`（或等价）；**不**走 C5 list API。 | |
| **U17-D6** | **Palette 展示：** 主标题 English-first i18n；副文案或后缀展示快捷键提示（如 `Ctrl+S` / `⌘S`，按平台）。`data-testid` 建议：`command-palette-action-save-binding`、`command-palette-action-refresh-preview`（或 `command-palette-option` + 稳定 id）。Enter/点击执行后 **关闭 palette**（对齐 C6-C9）。 | LR-C6 交互 |
| **U17-D7** | **空查询与过滤：** 作者动作出现在空查询列表（与可见路由并列的 Actions 组）及非空查询的客户端 contains 过滤（标题/快捷键文案，不区分大小写）。无编辑面上下文 → **不**渲染 Actions 组。 | 扩展 C6-C4/C6-C11 |
| **U17-D8** | **焦点陷阱抑制（强制）：** 当 **命令面板打开**（`command-palette` dialog）或其它 **modal/dialog 焦点陷阱**（如 Element Plus MessageBox、通用 modal，`aria-modal="true"`）为顶层时：**不**执行 U17-D2/D3。用户在 palette 搜索框键入时 Ctrl/Cmd+S/P **不得**保存/刷新。LR-C2 草稿恢复横幅 **不是** modal — 不抑制快捷键。 | stage_done_definition；C6 焦点陷阱 |
| **U17-D9** | **与既有快捷键共存：** Ctrl/Cmd+K 仍打开/聚焦 palette（C6）；Ctrl/Cmd+Z/Y 仍为结构 undo/redo（C3）；本片 **不**占用 K/Z/Y。 | C6-C19；C3-C13 |
| **U17-D10** | **结构化编辑器内焦点：** 焦点在 `controlled-structured-content-editor` 内时，Ctrl/Cmd+S/P **仍生效**（作者主路径）。实现可用 document 级捕获或编辑面根监听，但须满足 U17-D8。 | 编辑器内保存是核心价值 |
| **U17-D11** | **Fail-closed：** readonly / 无写权限 → 不注册 Save 动作；不拦截为「假成功」；若仍监听按键则仅 `preventDefault` 浏览器默认且 **不**调 API。Refresh 与现有按钮可见性一致。 | 权限 fail-closed |
| **U17-D12** | **i18n：** 动作标题、快捷键 aria/tooltip English-first（`en` + `zh-CN`）。禁止对用户暴露内部 handler 名。 | i18n-english-first |
| **U17-D13** | **无新后端；** FE Vitest + E2E + UIUX 强制。建议规格文件：`frontend/e2e/CE-U17-editor-shortcuts.spec.ts`；manifest：`frontend/e2e/evidence/CE-U17-uiux-manifest.md`。 | Task #96 testStrategy |

### 4.3 非确认假设（不得升格为需求）

| 项 | 状态 |
| --- | --- |
| Actions 是否用独立 composable 注册表 vs 在 `useCommandPalette` 内联 | 实现自选；须满足 D5–D8 |
| pristine（非 dirty）时 Ctrl+S 是否仍调用 Save | **允许**与按钮一致（按钮未因 dirty disabled）— 不强制 no-op |
| 是否在 Save 按钮 tooltip 上展示 Ctrl/Cmd+S | 推荐但不强制；验收以快捷键与 palette 为准 |
| Testing 主 Tab 预览页的 Ctrl+P | Out of scope（本片作用面 = 绑定编辑侧栏预览） |

---

## 5. Preconditions / Trigger

**Preconditions**

- 用户已登录管理端；Docker 验收 `http://localhost:4173`（E2E）。  
- 目标模板 dev 线可打开 Authoring Bindings 编辑某一锚点（侧栏预览面板可见）。  
- LR-C6 命令面板已存在；本片仅扩展动作。  
- CE-U21 Save 冲突路径已存在（成功/409 UX 不改）。

**Triggers**

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 用户按下 Ctrl/Cmd+S | 保存绑定（U17-D2） |
| T2 | 用户按下 Ctrl/Cmd+P | 刷新预览（U17-D3） |
| T3 | 用户打开命令面板并选择作者动作 | 执行对应 handler 并关闭面板 |
| T4 | 打开/关闭 palette 或其它 modal | 启用/抑制 T1/T2（U17-D8） |

---

## 6. Primary journey（成功路径）

1. 作者打开模板 dev workspace → Design → Bindings → 进入某锚点编辑器。  
2. 修改结构化内容使 dirty。  
3. 按 **Ctrl+S**（或 Cmd+S）→ 绑定保存成功 toast；dirty 清除（及 U21 草稿清除语义）。  
4. 按 **Ctrl+P**（或 Cmd+P）→ 预览刷新；`authoring-preview-refresh` loading 结束；成功 toast；无浏览器打印对话框。  
5. 按 **Ctrl+K** → 命令面板打开 → 可见 **Save binding** / **Refresh preview** → 高亮 Refresh → Enter → 再次刷新并关闭面板。

---

## 7. Acceptance scenarios（Given / When / Then）

### BDD-CE-U17-EKS-001 — Ctrl+S 保存绑定（与 Save 同路径）

```gherkin
Given 作者处于 Authoring Bindings 锚点编辑态且可保存
And 绑定内容相对服务端有可保存变更（或与 Save 按钮同等可提交态）
When 作者按下 Ctrl+S（Windows/Linux）或 Cmd+S（macOS）
Then 系统调用与工具栏 Save 同一保存路径
And 浏览器默认「保存网页」不出现（preventDefault）
And 成功时展示既有 saveBindingSuccess（或等价）反馈
```

### BDD-CE-U17-EKS-002 — Ctrl+P 刷新预览（与 Refresh now 同路径）

```gherkin
Given 作者处于绑定编辑态且预览面板 Refresh now 可达
When 作者按下 Ctrl+P 或 Cmd+P
Then 系统调用与 authoring-preview-refresh 同一刷新路径
And 浏览器打印对话框不出现（preventDefault）
And 刷新进行中再次按键不并发重复提交（与按钮 loading 一致）
```

### BDD-CE-U17-EKS-003 — 命令面板列出并执行 Save binding

```gherkin
Given 作者处于绑定编辑态且可保存
When 作者打开命令面板（Ctrl/Cmd+K）
Then Actions 组可见 Save binding（English-first），并提示 Ctrl/Cmd+S
When 作者高亮该动作并 Enter（或点击）
Then 执行与 Ctrl+S 同一保存路径
And 命令面板关闭
```

### BDD-CE-U17-EKS-004 — 命令面板列出并执行 Refresh preview

```gherkin
Given 作者处于绑定编辑态且 Refresh now 可达
When 作者打开命令面板并选择 Refresh preview
Then 执行与 Ctrl+P 同一刷新路径
And 命令面板关闭
```

### BDD-CE-U17-EKS-005 — 非编辑面不激活作者快捷键语义

```gherkin
Given 用户在管理端但不在绑定锚点编辑态（如 Bindings 列表、Variables、Dashboard）
When 用户按下 Ctrl/Cmd+S 或 Ctrl/Cmd+P
Then 不调用 handleSaveBinding / handlePreviewRefresh
And 命令面板空查询不展示本片作者 Actions（Save binding / Refresh preview）
```

### BDD-CE-U17-EKS-006 — 命令面板打开时抑制 Ctrl+S / Ctrl+P

```gherkin
Given 绑定编辑态下命令面板已打开且焦点在搜索框
When 用户按下 Ctrl/Cmd+S 或 Ctrl/Cmd+P
Then 不触发绑定保存或预览刷新
And 面板保持打开（除非实现将某键定义为关闭——本片不要求）
```

### BDD-CE-U17-EKS-007 — 其它 modal 焦点陷阱抑制快捷键

```gherkin
Given 绑定编辑态下顶层 aria-modal dialog（非命令面板）已打开
When 用户按下 Ctrl/Cmd+S 或 Ctrl/Cmd+P
Then 不触发保存或预览刷新
```

### BDD-CE-U17-EKS-008 — Fail-closed：无写权限不暴露 Save

```gherkin
Given 会话对模板绑定为 readonly 或无 authorTemplates 写权限
When 用户处于只读绑定查看或无法到达 Save 的状态
Then Ctrl/Cmd+S 不调用保存 API
And 命令面板不列出 Save binding
And 不出现假成功 toast
```

### BDD-CE-U17-EKS-009 — 保存中 / 刷新中忽略重复快捷键

```gherkin
Given Save 正在 submitting 或预览正在 refreshing
When 用户再次按下对应快捷键
Then 不启动第二次并发请求（与按钮 disabled/loading 一致）
```

### BDD-CE-U17-EKS-010 — 与 undo / palette 打开快捷键不冲突

```gherkin
Given 焦点在结构化编辑器内
When 用户使用 Ctrl/Cmd+Z（undo）与 Ctrl/Cmd+K（palette）
Then 行为仍符合 LR-C3 / LR-C6
And Ctrl/Cmd+S 与 Ctrl/Cmd+P 仍可按本规格触发保存/刷新（面板未打开时）
```

### BDD-CE-U17-EKS-011 — Save 失败与 U21 冲突路径不变

```gherkin
Given 绑定保存将返回错误或 409 bindingVersionConflict
When 作者通过 Ctrl+S 或 palette Save binding 触发保存
Then 错误/冲突 UX 与点击 Save 按钮一致（含 Reload / Keep editing）
And 不静默成功
```

### BDD-CE-U17-EKS-012 — E2E + UIUX 旅程

```gherkin
Given Docker 验收栈健康且作者已登录
When 作者在绑定编辑面验证 Ctrl/Cmd+S、Ctrl/Cmd+P 与命令面板作者动作
Then 功能旅程 PASS
And UIUX 证据双品牌 Critical=0（银行 OA；快捷键不破坏壳布局）
```

---

## 8. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| 作用面外 Ctrl+S/P | 不调用作者 handler（浏览器默认可保留） |
| Palette / modal 打开 | 抑制作者 Ctrl+S/P（U17-D8） |
| Save / Refresh 进行中 | 忽略重复触发 |
| 无写权限 | 无 Save 动作；无保存 API |
| 预览 API 失败 | 与 Refresh now 相同 error toast；不崩溃 |
| 未登录 / 无 shell | 无 palette、无本片快捷键（既有） |
| 输入框内普通字符 | 不拦截；仅修饰键组合 S/P |

---

## 9. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | Save/Refresh 反馈与按钮路径一致；无打印/保存网页对话框 |
| DOM | `authoring-preview-refresh` loading；palette action testids |
| Network | Save → 既有 binding upsert；Refresh → 既有 test-generate；**无新 API** |
| Gates | `pnpm -C frontend lint && type-check && test && build`；E2E + UIUX；queued docker-deploy |
| 非证据 | 不宣称 go-live；不改 OpenAPI |

---

## 10. Traceability

| 项 | 链接 |
| --- | --- |
| Plan | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 **CE-U17** |
| Task Master | **#96** — CE-U17: Editor keyboard shortcuts |
| Slice | `ce-u17-editor-shortcuts` |
| 依赖交付 | LR-C6 (#32)；LR-C3 (#30)；CE-U21 (#95) |
| Out of scope | CE-U19 (#97)；CE-E03；CE-G05；CD-3；#50 |

---

## 11. BDD readiness

**`ready`** — 规格完整；U17-C1…C7 与 U17-D1…D13 已按计划卡 + 既有编辑器/命令面板模式裁决；验收场景覆盖 Ctrl+S、Ctrl+P、命令面板作者动作、焦点陷阱抑制、非编辑面、fail-closed、进行中忽略、与 C3/C6 共存、U21 冲突路径不变、E2E/UIUX。  
**open_questions:** 无（阻塞性问题）。

**handoff:** `plan-orchestrator` → `frontend-engineer`（TDD Red：快捷键 composable / palette actions → 绿 → E2E）。
