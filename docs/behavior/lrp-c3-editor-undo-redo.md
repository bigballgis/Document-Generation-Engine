# BDD 行为规格：LR-C3 — Editor undo/redo（structure-level）

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-11  
**BDD ID**: `BDD-LRP-C3-UNDO-001`  
**来源任务**: [LRP Wave LR-C § LR-C3 — Editor undo/redo](../plan/detail/LRP-C-usability-deepening.md)  
**程序发现**: [launch-readiness-program.md](../plan/launch-readiness-program.md) Wave LR-C / finding 10（editor lacks undo/redo）  
**伴生**: [LR-C2 local draft recovery](./lrp-c2-structured-editor-local-draft-recovery.md)（§10 存储分离）；[CORE-FORTRESS F7 / LR-C1 dirty guard](./core-fortress-f7-authoring-ux.md)（F7-B1）  
**Task Master**: plan `LR-C3` / slice `lrp-c3-editor-undo-redo` — **Task Master #30** (`in-progress`)  
**Worktree**: `D:/working/DGE-lrp-c3-editor-undo-redo` · `feat/lrp-c3-editor-undo-redo`

---

## 1. 概述

模板作者在 **dev-editor 结构化内容编辑器**（`ControlledStructuredContentEditor`）中进行 **结构级** 变更时，系统必须提供 **有界、会话内、内存中** 的 undo/redo，使作者能撤销/重做节点增删改/移动/粘贴应用等操作，而 **不** 提供第三方输入框内的字符级撤销，也 **不** 将 undo 栈持久化到 LR-C2 本地草稿。

| 行为域 | 摘要 |
| --- | --- |
| **D1 结构历史** | 每次 **已提交** 的结构变更推入快照历史；undo/redo 恢复整份结构 JSON |
| **D2 有界栈** | 历史深度上限 **50**；超出时淘汰最旧条目 |
| **D3 键盘 + 工具栏** | Ctrl/Cmd+Z、Ctrl+Y / Cmd+Shift+Z（及 Cmd+Y）；工具栏按钮 + disabled + i18n tooltip |
| **D4 与草稿/dirty 协调** | undo 栈 **永不** 写入 C2 draft blob；dirty 仅由「当前结构 vs pristine baseline」决定 |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 字符级 / 输入框原生 undo（逐字撤销） | Out of scope — 文本字段变更 **合并** 为一次结构提交（见 C3-C4） |
| 将 undo 栈写入 `localStorage` / 服务端 / C2 draft | **禁止** — 会话内存 only |
| 内容模块编辑器 / 非 `ControlledStructuredContentEditor` 面 | **v1 Out of scope** — composable 可复用，但不纳入本切片验收 |
| 改变 LR-C1 dirty-guard 三动作契约或 C2 草稿键/载荷契约 | Out of scope — 仅定义互操作（§9–§10） |
| 跨标签页共享 undo 历史 | Out of scope |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **模板作者** | `TEMPLATE_AUTHOR`（及具备 `authorTemplates` 的等价角色） | 在 DRAFT/TESTING dev-editor 编辑 structured content |
| **测试员（受限作者）** | `decideTests` 且可编辑 dev 线 | 同编辑面；历史为该浏览器会话内存，不跨用户 |
| **系统（历史栈）** | 会话内 in-memory undo/redo | 不跨刷新、不跨标签页、不入草稿 |
| **系统（草稿）** | LR-C2 `localStorage` | 仅结构快照；与历史栈隔离 |
| **系统（dirty guard）** | LR-C1 `useDirtyGuard` | 导航拦截；与 undo 互补 |

授权：`readonly` / 无编辑权限 → **不记录历史、不启用 undo/redo 快捷键与工具栏**（fail-closed）。本规格不改变权限矩阵。

---

## 3. Goal

1. 作者可对结构级操作执行 undo/redo，且结果与历史中对应快照的结构 JSON **逐字节等价**（同一序列化）。  
2. 历史深度 **≤ 50**；内存不因连续编辑无限增长。  
3. 键盘与工具栏行为一致；空栈时按钮 disabled；i18n English-first。  
4. 与 LR-C2：**草稿永不包含 undo 栈**；Restore/Discard/Save 按 §9 重置或清空历史。  
5. 与 LR-C1：undo/redo 后 dirty 标志正确反映相对服务端 baseline 的 diff。

---

## 4. 已确认决策（2026-07-11，任务单 + 推荐裁决）

| ID | 决策 |
| --- | --- |
| **C3-C1** | **历史模型 = 结构快照（snapshot）**：每次已提交变更后，将 **变更前** 的序列化结构推入 undo 栈（或等价：保留 past snapshots + 当前指针）。**不**采用 inverse-ops 链（实现简单、与现有 `serializeStructuredContent` / C2 快照一致）。Redo 栈保存被 undo 掉的快照。 |
| **C3-C2** | **上限 = 50**：undo 可回退的已提交步数 **≤ 50**。第 51 次新提交时淘汰最旧 undo 条目；redo 栈在新分支上清空（见 C3-C6）。 |
| **C3-C3** | **结构变更（计入历史）**：下列任一导致 `documentModel` 提交且序列化结果相对上一提交快照 **有 diff** 时，记为一步：节点 **add / remove / move / reorder**；**paste-clean 确认应用**；**style apply**；**inline 子节点增删改**；**节点字段编辑**（含 text/variable/condition/loop/ref 等字段，经合并规则 C3-C4）。纯 UI 状态（选中样式 key、打开对话框未应用、catalog 加载）**不计**。 |
| **C3-C4** | **文本/字段合并（anti character-undo）**：对 **同一节点同一字段** 的连续输入，在失焦、切换到其他节点/字段、或发生另一类结构操作（增删移/粘贴应用/样式应用）之前，合并为 **至多一步** 历史。实现可用 blur-commit 和/或短防抖；验收以「Ctrl+Z 一次撤销整段字段编辑，而非逐字」为准。 |
| **C3-C5** | **作用面 v1**：`ControlledStructuredContentEditor`（模板 dev-editor Authoring/Design 挂载点）。 |
| **C3-C6** | **分支截断**：在已 undo（redo 栈非空）后发生 **新的** 结构提交 → **清空 redo 栈**；仅保留新分支。 |
| **C3-C7** | **草稿分离**：C2 draft payload **禁止** 任何 undo/redo 字段；历史 **仅** 内存。刷新后历史为空（草稿恢复走 C2，见 C3-C9）。 |
| **C3-C8** | **成功服务端 save → 清空历史**：`markPristine` / save 成功回调后 undo+redo 栈均为空；当前结构成为新基线。Save 失败 → **保留** 历史。 |
| **C3-C9** | **Restore draft（C2 横幅）→ 重置历史**：以恢复后结构为唯一当前态；undo/redo 清空（不得 undo「回到 Restore 前」或「回到草稿写入前的服务端态」——用户可用 Discard 草稿路径另选）。 |
| **C3-C10** | **Discard draft（C2 横幅）→ 重置历史**：保持服务端加载结构；undo/redo 清空。 |
| **C3-C11** | **dirty-guard Discard（导航放弃）**：离开即销毁会话内存历史（无额外持久化）。再次进入 = 空历史 + 服务端（或 C2 可恢复草稿）加载。 |
| **C3-C12** | **与 dirty 标志**：每次 undo/redo 应用快照后，按既有规则重算 `dirty = (currentSerialized ≠ pristineBaseline)`。Undo 回到与 baseline 相同结构 → dirty=false；否则 dirty=true。历史深度本身不单独驱动 dirty。 |
| **C3-C13** | **键盘**：Windows/Linux：`Ctrl+Z` = undo，`Ctrl+Y` = redo（可选同时支持 `Ctrl+Shift+Z` = redo）。macOS：`Cmd+Z` = undo，`Cmd+Shift+Z` = redo，**且** `Cmd+Y` = redo。在编辑器根（`data-testid="controlled-structured-content-editor"`）聚焦或事件目标在其内时拦截并 `preventDefault`（避免与浏览器默认文档 undo 冲突）；`readonly` 不拦截。 |
| **C3-C14** | **工具栏**：Undo / Redo 按钮；无可撤销/重做时 `disabled`；`aria-label` + tooltip 走 i18n；建议 `data-testid`：`structured-editor-undo` / `structured-editor-redo`。 |
| **C3-C15** | **无 diff 不入栈**：若某操作后序列化与栈顶/当前提交快照相同，**不**推入新历史步。 |
| **C3-C16** | **E2E + UIUX 强制**；规格 `frontend/e2e/LRP-C3-undo-redo.spec.ts`；manifest `frontend/e2e/evidence/LRP-C3-uiux-manifest.md`。 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 已提交结构变更（C3-C3/C3-C4） | 推入 undo；按 C3-C6 可能清空 redo |
| T2 | 用户 Ctrl/Cmd+Z 或点击 Undo | 应用上一快照；启用 redo |
| T3 | 用户 Ctrl+Y / Cmd+Shift+Z / Cmd+Y 或点击 Redo | 应用下一快照 |
| T4 | 成功服务端 save / Restore draft / Discard draft | 按 C3-C8…C3-C10 清空或重置历史 |
| T5 | 编辑器挂载 / 卸载 | 挂载时历史为空；卸载丢弃内存历史 |

---

## 6. Preconditions

- 用户已登录；对目标模板 dev 线具备编辑权限；面板非 `readonly`。  
- LR-C2 草稿机制已存在（存储分离约束生效）。  
- LR-C1 dirty guard 已在同一编辑面生效。  
- Docker 验收：`http://localhost:4173`（Playwright）。

---

## 7. Primary journey（成功路径）

1. 作者打开模板 dev-editor 结构化编辑器；历史为空；Undo/Redo disabled。  
2. 作者执行结构编辑 A → B → C（三步已提交变更）。  
3. 作者按 Ctrl+Z 两次 → 结构回到 **仅含 A 之后** 的状态；Redo 按钮 enabled。  
4. 作者按 Redo 一次 → 结构回到 A+B 之后；可再 Redo 到 A+B+C。  
5. 若在 undo 后执行新编辑 D → redo 栈清空；无法再回到被截断的 B/C 分支。  
6. 作者 Save 成功 → 历史清空；Undo/Redo disabled；dirty=false。

---

## 8. 验收场景（Given / When / Then）

### BDD-LRP-C3-001 — 三次结构编辑后 Undo×2 回到第一步后状态

**Given** 作者在 structured editor 连续完成 **三次** 已提交结构编辑（记为 E1、E2、E3），且每次均改变序列化结构  
**When** 用户按 **Ctrl+Z**（或 Cmd+Z）**两次**  
**Then** 编辑器结构等于 **仅 E1 完成后** 的状态（与 E2/E3 应用前一致）  
**And** 工具栏 **Redo** 为 enabled  
**And** 工具栏 **Undo** 在仍可再撤时保持 enabled（若仅一步可撤则再撤一次后 disabled）

### BDD-LRP-C3-002 — 历史达上限后淘汰最旧

**Given** undo 历史已达 cap **50**（已有 50 步可撤）  
**When** 再发生一次已提交结构编辑  
**Then** 最旧的历史条目被淘汰  
**And** 可 undo 深度保持 **≤ 50**（无法撤到被淘汰的最早状态）

### BDD-LRP-C3-003 — Undo 后 Redo 恢复

**Given** 作者已完成至少两步结构编辑并 Undo 一次  
**When** 用户执行 Redo（Ctrl+Y 或 Cmd+Shift+Z 或 Cmd+Y 或工具栏 Redo）  
**Then** 结构恢复为 Undo 前的状态  
**And** 与该步快照序列化一致

### BDD-LRP-C3-004 — 新编辑截断 redo 分支

**Given** 作者已 Undo 至少一次（redo 栈非空）  
**When** 用户执行一次 **新的** 结构编辑（非 redo）  
**Then** redo 栈被清空  
**And** Redo 按钮 disabled  
**And** 无法再回到被截断分支上的状态

### BDD-LRP-C3-005 — 空栈工具栏 disabled

**Given** 编辑器刚挂载或历史已因 save/Restore/Discard 清空，且无未提交可撤步  
**When** 用户查看工具栏  
**Then** Undo 与 Redo 均为 **disabled**  
**And** 快捷键不改变结构

### BDD-LRP-C3-006 — macOS 快捷键等价

**Given** 在可编辑 structured editor 中有可撤/可重做历史（测试可模拟 metaKey）  
**When** 用户使用 **Cmd+Z** / **Cmd+Shift+Z**（及 **Cmd+Y** 作为 redo）  
**Then** 行为分别与 undo / redo 一致（与 BDD-LRP-C3-001/003 相同断言）

### BDD-LRP-C3-007 — i18n English-first

**Given** 工具栏 Undo/Redo 可见  
**When** 检查用户可见文案（tooltip / aria-label）  
**Then** 全部来自 i18n（`en.ts` 为基座，`zh-CN.ts` 成对）  
**And** 无硬编码中英文用户可见字符串

### BDD-LRP-C3-008 — 草稿 blob 不含 undo 栈

**Given** 作者有结构编辑且 C2 本地草稿已写入  
**When** 读取该键 `localStorage` 草稿 JSON  
**Then** payload **仅** 含 C2 约定字段（`schemaVersion` / `structureJson` / `draftUpdatedAt` / 可选 `serverUpdatedAt` / `anchorId`）  
**And** **不存在** undo/redo/history 相关字段或嵌套栈

### BDD-LRP-C3-009 — Save 成功清空历史

**Given** 作者有可撤历史且 editor dirty  
**When** 显式 Save 或 dirty-guard Save **成功**（`markPristine`）  
**Then** undo 与 redo 栈均为空；Undo/Redo disabled  
**When** Save 失败  
**Then** 历史 **保留**；结构不变

### BDD-LRP-C3-010 — Restore draft 重置历史

**Given** C2 恢复横幅可见且当前会话可能已有历史步  
**When** 用户选择 Restore  
**Then** 结构变为草稿内容；dirty=true（相对服务端 baseline）  
**And** undo/redo 栈清空（Reset，C3-C9）

### BDD-LRP-C3-011 — Discard draft 重置历史

**Given** C2 恢复横幅可见  
**When** 用户选择 Discard  
**Then** 结构保持服务端加载内容  
**And** undo/redo 栈清空（C3-C10）

### BDD-LRP-C3-012 — Undo/Redo 与 dirty 标志

**Given** pristine baseline 为 S0；作者编辑到 S1（dirty=true）  
**When** 用户 Undo 回到序列化等于 S0 的状态  
**Then** dirty=false（LR-C1 不再因该编辑面拦截，除非其他绑定/规则仍 dirty — 本切片仅断言 structured editor 发出的 dirty）  
**When** 用户 Redo 回到 S1  
**Then** dirty=true

### BDD-LRP-C3-013 — readonly 不启用

**Given** 编辑器 `readonly` 或用户无编辑权限  
**When** 用户尝试快捷键或查看工具栏  
**Then** 不记录历史；Undo/Redo 不可用（隐藏或 disabled）；快捷键不拦截为结构 undo

### BDD-LRP-C3-014 — 字段编辑合并为一步

**Given** 作者在同一段落文本字段连续输入多字符（未失焦、未切换操作类型）  
**When** 用户按一次 Undo  
**Then** 整段该字段编辑被撤销（回到该字段编辑开始前的结构），**不是**只少一个字符

---

## 9. 边界与异常

| 场景 | 期望 |
| --- | --- |
| 粘贴清理对话框未确认 | 不入历史；仅「应用到模型」后记一步 |
| 操作后结构无 diff | 不推入历史（C3-C15） |
| 刷新 / 关闭标签页 | 内存历史丢失；C2 草稿仍可恢复结构，但历史为空 |
| 多标签同编辑上下文 | v1 各标签独立内存历史；可接受 |
| dirty-guard Stay | 历史不变 |
| dirty-guard Discard 离开 | 历史随卸载销毁 |
| 焦点在输入框内按 Ctrl+Z | **结构级** undo（C3-C13 preventDefault）；依赖 C3-C4 合并，避免「像字符 undo」的体验 |
| 历史模块与 draft 写入 | 两者都可监听结构变更，但 **不得** 共享可变栈引用写入 storage |

---

## 10. 与 LR-C2 / LR-C1 互操作

| 约束 | 说明 |
| --- | --- |
| **存储分离** | 兑现 C2-C4 / C2 §10：draft blob 无 undo 栈（BDD-LRP-C3-008） |
| **Restore / Discard** | C3-C9 / C3-C10 重置历史 |
| **Clear-on-save** | C2 清草稿 + C3 清历史，同一成功 save 路径 |
| **dirty** | C3-C12；undo 到 baseline 可清除 structured dirty，与 F7-B1 一致 |
| **写入源** | 历史提交点应基于「结构已提交」的稳定出口（与 C2 `structure-change` / 序列化 model 对齐），避免把内部指针写入 draft |

---

## 11. 系统响应（成功 / 失败）

| 路径 | UI / 内存响应 |
| --- | --- |
| 结构提交 | 静默推入 undo；必要时清 redo；更新按钮 disabled |
| Undo / Redo | 模型替换为快照；重算 dirty；触发既有 `structure-change` / draft 防抖写（若仍 dirty） |
| Save 成功 | 历史清空 |
| 空栈快捷键 | 无结构变化；可选 no-op |

**建议 i18n 键（en 基座；实现可微调命名但须 en+zh-CN 成对）**

- `templates.structuredEditor.undo`
- `templates.structuredEditor.redo`
- `templates.structuredEditor.undoTooltip`（可含快捷键提示）
- `templates.structuredEditor.redoTooltip`

---

## 12. 可观测证据

| 证据 | 说明 |
| --- | --- |
| Vitest | push / undo / redo / cap 淘汰 / branch truncation / coalesce 字段编辑 / save 清栈 / restore·discard 重置 / dirty 重算 / draft payload 无 history 字段 |
| Playwright | `frontend/e2e/LRP-C3-undo-redo.spec.ts`：三步编辑 → undo×2 → 结构断言 + redo enabled；redo×1；可选 cap 单测为主、E2E 抽检工具栏 disabled |
| UIUX manifest | `frontend/e2e/evidence/LRP-C3-uiux-manifest.md` — REDBC + GREENBC；工具栏 disabled/enabled 态 |
| i18n | 新 keys 在 `en.ts` + `zh-CN.ts` |
| Docker E2E | `playwright.docker.config.ts` against `:4173` |

---

## 13. 追溯

| 文档 | 用途 |
| --- | --- |
| [LRP-C usability deepening](../plan/detail/LRP-C-usability-deepening.md) § LR-C3 | 任务单 / G/W/T / Do NOT |
| [launch-readiness-program.md](../plan/launch-readiness-program.md) | Wave LR-C；finding 10 |
| [LR-C2 draft recovery](./lrp-c2-structured-editor-local-draft-recovery.md) | 存储分离；Restore 重置历史前瞻 |
| [F7 authoring UX / LR-C1](./core-fortress-f7-authoring-ux.md) | Dirty guard 与 dirty 重算 |
| `ControlledStructuredContentEditor.vue` | 结构变更入口；`structure-change` / `dirty-change` / `markPristine` |
| `structuredContentDraftStorage.ts` | Draft payload 契约（无 history） |

---

## 14. BDD readiness

**`ready`** — 规格完整；C3-C1…C3-C16 已裁决（快照模型、cap 50、结构变更定义、字段合并、草稿分离、Save/Restore/Discard 清栈、dirty 互操作、快捷键与工具栏）；无阻塞性 pending questions。

**移交**: `plan-orchestrator`（激活 LR-C3 / Task Master）→ `frontend-engineer` TDD（建议 `useStructuredContentHistory` composable + 工具栏接线）→ `e2e-test-engineer` + `e2e-uiux-reviewer`。

**本 agent 不**将计划行标为 Done；不实现生产代码。
