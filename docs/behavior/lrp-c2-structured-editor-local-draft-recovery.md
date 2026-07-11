# BDD 行为规格：LR-C2 — Structured editor local draft recovery

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-11  
**BDD ID**: `BDD-LRP-C2-DRAFT-001`  
**来源任务**: [LRP Wave LR-C § LR-C2 — Structured editor local draft recovery](../plan/detail/LRP-C-usability-deepening.md)  
**程序发现**: [launch-readiness-program.md](../plan/launch-readiness-program.md) Wave LR-C / finding（JWT/会话中断工作丢失）  
**治理坑位**: [CD-PIT-13](../plan/detail/CDP-industry-pitfall-registry.md) companion（LR-B6 保会话；本切片保本地草稿）  
**伴生**: [CORE-FORTRESS F7 / LR-C1 dirty guard](./core-fortress-f7-authoring-ux.md)（F7-B1）；LR-C3 undo/redo（未开工，见 §10 互操作）  
**Task Master**: plan `LR-C2` / Task Master **#29**（slice `lrp-c2-local-draft-recovery`）

---

## 1. 概述

模板作者在 **dev-editor 结构化内容编辑器**（`ControlledStructuredContentEditor`）中编辑时，系统必须把未成功保存到服务端的结构变更 **防抖写入浏览器 `localStorage`**，以便标签页刷新、会话中断或意外离开后，重新打开同一编辑上下文时能 **恢复草稿**。

| 行为域 | 摘要 |
| --- | --- |
| **D1 本地草稿写入** | 结构变更后防抖写入 `localStorage`；键 = 用户 + templateId + devVersionId |
| **D2 恢复横幅** | 挂载时若存在与服务端加载内容不同的本地草稿 → 展示「Restore draft / Discard」+ 时间戳 |
| **D3 清除语义** | **仅**在服务端 **成功 save** 后清除该键草稿；Discard 横幅也清除；**不**在服务端存草稿 |
| **D4 配额守卫** | `QuotaExceededError` 时按草稿时间戳丢弃最旧条目直至写入成功或无可丢弃 |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 改变 explicit-save 语义（服务端 save 仍是唯一权威持久化） | Out of scope — 禁止隐式自动提交服务端 |
| 服务端草稿 / 跨设备同步 | Out of scope |
| LR-C3 结构级 undo/redo | Out of scope — 见 §10，存储设计不得阻塞 C3 |
| 内容模块编辑器 / 非模板面的草稿（无 templateId+devVersionId） | **v1 Out of scope** — 可复用 composable，但不纳入本切片验收 |
| 绑定表单字段、规则表达式的独立草稿 | Out of scope — 仅 **structured content JSON** |
| 改变 LR-C1 dirty guard 对话框文案/三动作契约 | Out of scope — 仅定义与草稿的互操作（§9） |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **模板作者** | `TEMPLATE_AUTHOR`（及具备 `authorTemplates` 的等价角色） | 在 DRAFT/TESTING dev-editor 编辑 structured content |
| **测试员（受限作者）** | `decideTests` 且可编辑 dev 线 | 同编辑面；草稿键含其用户身份，与作者隔离 |
| **系统（浏览器存储）** | `localStorage` | 客户端唯一草稿介质；失败时降级（见 §8） |
| **系统（路由 / dirty guard）** | LR-C1 `useDirtyGuard` | 导航/关闭拦截与草稿互补，不互相替代 |

授权：无编辑权限 / `readonly` → **不写草稿、不展示恢复横幅**（fail-closed，与既有 authoring 门控一致）。本规格不改变权限矩阵。

---

## 3. Goal

1. 未成功 save 的结构编辑在刷新/会话中断后 **可恢复**，且 Restore 后结构与草稿写入时 **逐字节等价**（同一序列化 JSON）。  
2. 成功 save 后草稿 **消失**；再次挂载 **无横幅**，服务端内容为权威。  
3. Discard 明确放弃本地草稿，保留当前服务端加载内容。  
4. 与 LR-C1 dirty guard **互补**：guard 防「导航丢内存」；本切片防「刷新/崩溃丢本地」。  
5. Bank OA + i18n：横幅与按钮全部走 i18n（en 基座 + zh-CN）；REDBC/GREENBC 无品牌特例逻辑。

---

## 4. 已确认决策（2026-07-11，自任务单裁决）

| ID | 决策 |
| --- | --- |
| **C2-C1** | **权威持久化**：仅 explicit 服务端 save；本地草稿是 **best-effort 工作副本**，不得暗示「已保存」。 |
| **C2-C2** | **作用面 v1**：模板 dev-editor 内 `ControlledStructuredContentEditor`（Authoring / Design 等挂载点，具备 `templateId` + 路由 `devVersionId`）。 |
| **C2-C3** | **存储键**：`docgen.structuredDraft.v1:{userId}:{templateId}:{devVersionId}`（`userId` = 当前管理会话主体稳定 id；禁止仅用 display name）。 |
| **C2-C4** | **载荷**：至少 `{ schemaVersion: 1, structureJson: string, draftUpdatedAt: ISO-8601, serverUpdatedAt?: ISO-8601 \| null }`；**禁止**把 undo 历史塞进同一 blob（留给 LR-C3）。 |
| **C2-C5** | **写入时机**：`structure-change` / 文档模型变更后 **防抖写入**（建议默认 **400ms**，实现可调；测试可注入 0）。`readonly` 不写。 |
| **C2-C6** | **横幅条件**：编辑器可编辑挂载完成且已加载服务端结构后，若该键存在草稿且 `structureJson` **≠** 当前加载的服务端结构 → 显示恢复横幅（含草稿时间戳；若有 `serverUpdatedAt` 或加载时服务端修订时间则一并展示）。内容相等 → **不**显示。 |
| **C2-C7** | **Restore**：用草稿 `structureJson` 替换编辑器模型；标记 **dirty**（相对服务端 baseline）；横幅关闭；**保留** localStorage 草稿直至成功 save 或 Discard。 |
| **C2-C8** | **Discard（横幅）**：删除该键草稿；编辑器保持服务端加载内容；横幅关闭；若内容与 baseline 一致则 **pristine**。 |
| **C2-C9** | **Clear-on-save**：任意将该结构成功持久化到服务端的路径（含 dirty-guard Save → 成功）在 `markPristine` / 成功回调中 **删除**该键草稿。Save 失败 → **保留**草稿。 |
| **C2-C10** | **配额**：`setItem` 抛配额错误 → 按各草稿 `draftUpdatedAt` **升序**删除其他 `docgen.structuredDraft.v1:*` 条目并重试；若仍失败 → 静默跳过本次写入（不打断编辑；可选非阻塞 toast，i18n key，非强制）。 |
| **C2-C11** | **与 dirty-guard Discard（导航放弃内存）**：路由 Discard **不**清除 localStorage 草稿（刷新后仍可恢复）。仅横幅 Discard 与成功 save 清除。 |
| **C2-C12** | **E2E + UIUX 强制**（任务单 UIUX: yes）；规格文件 `frontend/e2e/LRP-C2-draft-recovery.spec.ts`；manifest `frontend/e2e/evidence/LRP-C2-uiux-manifest.md`。 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 结构节点增删改/粘贴应用等导致 `structure-change` | 防抖写草稿 |
| T2 | 编辑器挂载（进入 dev-editor authoring 结构化编辑） | 检测草稿 → 可能显示横幅 |
| T3 | 用户点击 Restore / Discard | 应用或清除草稿 |
| T4 | 服务端 save 成功 | 清除草稿 |
| T5 | 标签页 reload / 进程恢复后再次打开同一 template+devVersion | 同 T2 |

---

## 6. Preconditions

- 用户已登录；对目标模板 dev 线具备编辑权限；面板非 `readonly`。  
- 路由上下文可解析 `templateId` 与 `devVersionId`。  
- 浏览器支持 `localStorage`（不支持则降级：不写、不横幅，编辑仍可用 — §8）。  
- LR-C1 dirty guard 已在同一编辑面生效（F7 Done）。  
- Docker 验收：`http://localhost:4173`（Playwright）。

---

## 7. Primary journey（成功路径）

1. 作者打开模板 dev-editor 结构化编辑器，服务端结构加载为 baseline。  
2. 作者修改结构（未点 Save）→ 防抖后 `localStorage` 写入该键草稿（含 `draftUpdatedAt`）。  
3. 标签页刷新（或会话中断后重新登录并打开同一 template+devVersion）。  
4. 编辑器挂载：检测到草稿且内容 ≠ 服务端 → 显示恢复横幅（Restore / Discard + 时间戳）。  
5a. **Restore** → 编辑器呈现草稿结构；dirty=true；可继续编辑；之后 Save 成功 → 草稿清除；再次挂载无横幅。  
5b. **Discard** → 草稿删除；编辑器保持服务端结构；无横幅。

---

## 8. 验收场景（Given / When / Then）

### BDD-LRP-C2-001 — 刷新后提供带时间戳的草稿恢复

**Given** 作者在 structured editor 有未保存结构变更，且本地草稿已写入  
**When** 标签页 reload（或等价：卸载后重新挂载同一 `templateId`+`devVersionId`）  
**Then** 显示恢复横幅（i18n），含草稿时间戳  
**And** 提供 Restore 与 Discard 两个动作  
**And** 选择 Restore 后编辑器结构与草稿写入时的 JSON **完全一致**

### BDD-LRP-C2-002 — 恢复后保存再挂载无横幅

**Given** 作者已 Restore 草稿且随后 **成功** 执行服务端 save  
**When** 编辑器再次挂载（或刷新）  
**Then** **不**显示恢复横幅  
**And** 该键 `localStorage` 草稿已清除  
**And** 编辑器内容与服务端权威结构一致（pristine）

### BDD-LRP-C2-003 — Discard 放弃本地草稿

**Given** 挂载时恢复横幅可见  
**When** 用户选择 Discard  
**Then** 横幅关闭；该键草稿删除  
**And** 编辑器保持服务端加载结构（未应用草稿）  
**And** 相对 baseline 为 pristine（除非另有未保存非草稿变更 — v1 挂载时无）

### BDD-LRP-C2-004 — 键隔离（用户 / 模板 / 版本）

**Given** 用户 A 在 template T1 / devVersion V1 有草稿  
**When** 用户 A 打开 T1/V2，或用户 B 打开 T1/V1，或打开其他模板  
**Then** **不**误用 A@T1@V1 的草稿（无错误横幅 / 不串内容）

### BDD-LRP-C2-005 — 成功 save 清除草稿（含 dirty-guard Save）

**Given** 存在本地草稿且 editor dirty  
**When** 用户经显式 Save **或** dirty-guard 对话框 Save，且服务端返回成功  
**Then** 该键草稿清除；dirty=false（既有 `markPristine`）  
**When** Save 失败  
**Then** 草稿 **保留**；dirty 保持

### BDD-LRP-C2-006 — 与 LR-C1 dirty guard 互补

**Given** 作者有未保存结构变更（草稿已写入）  
**When** 用户尝试路由离开  
**Then** LR-C1 确认对话框仍按 F7-B1 出现（Stay / Discard / Save）  
**And** 若用户选 dirty-guard **Discard** 并离开，**不**因该动作清除 localStorage 草稿  
**And** 用户再次打开同一编辑上下文时，仍可按 BDD-LRP-C2-001 看到恢复横幅（若服务端内容仍不同于草稿）

### BDD-LRP-C2-007 — 无差异不横幅

**Given** localStorage 中草稿 `structureJson` 与当前服务端加载结构相等（或键不存在）  
**When** 编辑器挂载  
**Then** **不**显示恢复横幅

### BDD-LRP-C2-008 — 配额淘汰最旧草稿

**Given** `localStorage` 写入因配额失败  
**When** 系统执行配额守卫  
**Then** 按 `draftUpdatedAt` 丢弃 **其他** `docgen.structuredDraft.v1:*` 中最旧条目并重试写入当前草稿  
**And** 当前编辑会话不因配额错误崩溃或丢失内存中的编辑模型

### BDD-LRP-C2-009 — readonly / 无权限

**Given** 编辑器 `readonly` 或用户无编辑权限  
**When** 结构展示或挂载  
**Then** 不写入草稿；不显示恢复横幅

---

## 9. 边界与异常

| 场景 | 期望 |
| --- | --- |
| `localStorage` 不可用 / 隐私模式拒绝 | 降级：不写不读；无横幅；编辑与 save 行为不变 |
| 草稿 JSON 损坏 / 无法 parse | 删除坏键；无横幅；加载服务端结构；可选一次非阻塞警告（i18n） |
| 服务端在草稿之后被他人更新 | 仍按 C2-C6 在内容不等时展示横幅；时间戳帮助用户决策；Restore 会覆盖为本地草稿（用户显式选择） |
| 粘贴清理对话框未确认 | 以最终应用到模型的结构为准触发写入；未应用的 pending 不写 |
| 多标签同键编辑 | v1 **不**做跨 tab 锁；后写覆盖；可接受（记录为已知限制） |
| beforeunload（LR-C1） | 刷新前仍可出现浏览器原生提示；刷新后走本切片恢复 — 两者并存 |

---

## 10. 与 LR-C3（undo/redo）互操作（前瞻约束）

| 约束 | 说明 |
| --- | --- |
| **存储分离** | 草稿 blob **仅**结构快照 + 元数据；undo 栈不得写入同一 key/同一对象字段 |
| **Restore 语义** | Restore 替换当前结构后，未来 C3 应将历史栈 **重置**为以恢复后结构为唯一基线（C3 BDD 承接；C2 实现不预建 undo API） |
| **写入源** | 草稿监听「结构已变更」的稳定出口（现有 `structure-change` / 序列化后的 model），避免与未来 undo 内部指针耦合 |
| **本切片禁止** | 实现任何 undo/redo UI 或历史栈 |

---

## 11. 系统响应（成功 / 失败）

| 路径 | UI / 存储响应 |
| --- | --- |
| 防抖写入成功 | 无打断性 UI（静默） |
| 挂载需恢复 | 横幅可见：`data-testid` 建议 `structured-draft-recovery-banner`；按钮 `…-restore` / `…-discard` |
| Restore | 模型更新；dirty；横幅关 |
| Discard | 键删除；横幅关 |
| Save 成功 | 键删除 |
| 配额耗尽且淘汰后仍失败 | 静默跳过写入；编辑继续 |

**建议 i18n 键（en 基座；实现时可微调命名但须 en+zh-CN 成对）**

- `templates.structuredEditor.draftRecovery.title`
- `templates.structuredEditor.draftRecovery.message`（含时间戳插值）
- `templates.structuredEditor.draftRecovery.draftTimestamp`
- `templates.structuredEditor.draftRecovery.serverTimestamp`（可选）
- `templates.structuredEditor.draftRecovery.restore`
- `templates.structuredEditor.draftRecovery.discard`

---

## 12. 可观测证据

| 证据 | 说明 |
| --- | --- |
| Vitest | 键作用域；防抖写入；clear-on-save；restore/discard；配额淘汰；readonly 跳过 |
| Playwright | `frontend/e2e/LRP-C2-draft-recovery.spec.ts`：编辑 → reload → restore 内容在；discard 路径；save 后无横幅 |
| UIUX manifest | `frontend/e2e/evidence/LRP-C2-uiux-manifest.md` — REDBC + GREENBC |
| i18n | 新 keys 在 `en.ts` + `zh-CN.ts`（无硬编码用户可见字符串） |
| Docker E2E | `playwright.docker.config.ts` against `:4173` |

---

## 13. 追溯

| 文档 | 用途 |
| --- | --- |
| [LRP-C usability deepening](../plan/detail/LRP-C-usability-deepening.md) § LR-C2 | 任务单 / G/W/T / Do NOT |
| [launch-readiness-program.md](../plan/launch-readiness-program.md) | Wave LR-C；CD-PIT-13 工作丢失缓解 |
| [CD-PIT-13](../plan/detail/CDP-industry-pitfall-registry.md) | 会话中断伴生缓解 |
| [F7 authoring UX / LR-C1](./core-fortress-f7-authoring-ux.md) | Dirty guard 契约与互补 |
| [session-renewal-revocation.md](./session-renewal-revocation.md) | LR-B6 — 保会话；本切片保草稿 |
| `ControlledStructuredContentEditor.vue` | `structure-change` / `dirty-change` / `markPristine` / explicit baseline |
| `useDirtyGuard.ts` | 导航拦截；Save 成功路径须清草稿 |

---

## 14. BDD readiness

**`ready`** — 规格完整；C2-C1…C2-C12 已自任务单裁决；无阻塞性 pending questions。

**移交**: `plan-orchestrator`（分配/激活 Task Master 与计划行，若尚未激活）→ `frontend-engineer` TDD（建议 composable + banner）→ `e2e-test-engineer` + `e2e-uiux-reviewer`。

**本 agent 不**将计划行标为 Done；不实现生产代码。
