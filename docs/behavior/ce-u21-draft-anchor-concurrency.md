# BDD 行为规格：CE-U21 — 草稿按锚点分 key + 保存并发冲突提示

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CE-U21-DAC`  
**编写日期:** 2026-07-17  
**程序:** [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U21  
**Slice:** `ce-u21-draft-anchor-concurrency`  
**Task Master:** **#95**  
**Formal phase:** **None**  
**Placement:** ISOLATED `D:/working/DGE-ce-u21-draft-anchor-concurrency` · `feat/ce-u21-draft-anchor-concurrency`  
**batch_recommendation:** **solo** `#95` `ce-u21-draft-anchor-concurrency`  
**伴生:** [LR-C2 local draft recovery](./lrp-c2-structured-editor-local-draft-recovery.md)（键空间与恢复横幅基线）；[CE-U20](./ce-u20-clause-create-structured.md)（已声明 U21 为后续叶）  
**完成声明约束:** 关闭「同 template+devVersion 下跨锚点草稿串写」与「绑定保存 last-write-wins 无冲突提示」缺口；**不**宣称 go-live；**不**激活 CD-3 / P3；**不**触碰 #50；**不**做 CE-U17 / CE-U19

---

## 1. 概述

模板作者在 **Authoring Bindings** 结构化编辑器中按锚点编辑时，本地草稿（LR-C2）当前键为：

`docgen.structuredDraft.v1:{userId}:{templateId}:{devVersionId}`

`anchorId` 仅写入载荷并在恢复时做软过滤（`shouldOfferDraftRecovery`）。同一 `templateId`+`devVersionId` 下切换锚点会 **覆盖同一 localStorage 键**，导致跨锚点草稿串扰。

服务端绑定保存（`PUT .../bindings/{anchorId}`）对已有行是 **last-write-wins**：`AnchorBindingEntity.updatedAt` 存在但未暴露于 `AnchorBindingView`，请求体亦无并发令牌，并发编辑时后写静默覆盖且无明确 UX。

| 缺口（现状证据） | 目标 |
| --- | --- |
| `buildStructuredDraftStorageKey` 三元组，无 `anchorId` | 有 `anchorId` 时键含锚点；同模板多锚点草稿隔离 |
| 载荷 `anchorId` 仅软过滤，无法阻止覆盖写 | 写/清/恢复均以 **per-anchor key** 为权威作用域 |
| `AnchorBindingView` 无 `updatedAt`；upsert 无期望版本 | 暴露 `updatedAt`；更新要求 `expectedUpdatedAt`；不匹配 → **409** + 清晰提示 |
| 冲突时仅 generic API error（若有） | English-first 专用 messageKey + 作者可理解动作（Reload / keep editing） |

| 行为域 | 摘要 |
| --- | --- |
| **DAC-01 Per-anchor localDraft key** | 键 = user + template + devVersion + **anchorId**（绑定编辑面） |
| **DAC-02 Legacy key compat** | 旧三元组键可读/可迁移；不串到其他锚点 |
| **DAC-03 Optimistic save lock** | 绑定更新用 `updatedAt` 作并发令牌；冲突 409 + UX |
| **DAC-04 Fail-closed** | 无编辑权 / readonly：不写草稿、不展示恢复；保存仍走既有 403 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-U17 编辑器快捷键 / CE-U19 依赖只读视图 | P3 parked — **禁止**本片实现 |
| 跨标签页实时协同 / 操作者 presence | Out of scope |
| 变量 schema / composition rules / 测试集 / 条款模块（CM）乐观锁 | Out of scope — 仅 **template anchor binding** upsert |
| 发布 semver「版本号冲突」UI（`publishVersionConflict`） | Out of scope — 既有 lifecycle 面，勿混用文案 |
| 服务端草稿存储 / 跨设备同步 | Out of scope（沿用 LR-C2） |
| 改变 explicit-save 语义或 dirty-guard 契约 | Out of scope — 仅叠加冲突提示与键隔离 |
| 宣称 go-live / 激活 CD-3 / 正式 P3 / #50 | **禁止** |

---

## 2. Actor / Role

| Actor | 能力 / 角色 | 说明 |
| --- | --- | --- |
| **模板作者** | `TEMPLATE_AUTHOR`（及具备 `authorTemplates` 的等价角色） | 在 DRAFT/TESTING Authoring Bindings 编辑结构化内容；写 per-anchor 本地草稿；Save 绑定 |
| **测试员（受限作者）** | 可编辑 dev 线 | 同编辑面；草稿键含其用户身份，与作者隔离（LR-C2） |
| **并发编辑者** | 同组另一会话 | 可对同一锚点先后保存；后者若持陈旧 `updatedAt` → 冲突提示，**不**静默覆盖 |
| **无授权 / readonly** | — | 不写草稿、无恢复横幅；保存 fail-closed（403 / 不可达） |
| **系统** | `localStorage` + binding upsert API | 本地草稿介质；乐观锁校验与 409 信封 |

授权：本规格不改变 permission-matrix；沿用既有 template authoring 门控。

---

## 3. Goal

1. 同一用户在同一 `templateId`+`devVersionId` 下，锚点 A 与锚点 B 的本地草稿 **互不覆盖、互不误恢复**。  
2. 绑定 **更新** 保存时，客户端携带加载时的 `updatedAt` 作为 `expectedUpdatedAt`；服务端不匹配则 **拒绝写入** 并返回可映射的冲突错误。  
3. 冲突时 UI 展示 **清晰、可行动** 的 English-first 提示（刷新服务端内容或保留本地继续编辑），且 **不**清除该锚点本地草稿（Save 未成功）。  
4. 匹配令牌的成功保存路径与 LR-C2 clear-on-save 一致：清除 **该锚点** 草稿键；编辑器 pristine。  
5. Formal phase 保持 **None**；不宣称 go-live；不激活 CD-3。

---

## 4. 已确认决策 vs 待决

### 4.1 已确认（产品 / 计划）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U21-C1** | localDraft key **加** `anchorId`（按锚点分 key）。 | CE 计划 §4 CE-U21；Task Master #95 |
| **U21-C2** | 服务端保存 **乐观锁** 版本冲突时给出清晰 UX 提示。 | 同上 |
| **U21-C3** | P2·M；U20 Done 后的 queue head；P3 parked；leave #50。 | CE 计划 / handoff |
| **U21-C4** | Formal phase **None**；不宣称 go-live；不激活 CD-3。 | CE 计划 |
| **U21-C5** | 用户面变更 → E2E + UIUX；银行 OA + English-first。 | delivery constitutions |
| **U21-C6** | Batch recommendation：**solo** `#95`。 | Stage −1 |

### 4.2 本片确认的实现决策（计划卡薄 → 仓库事实推导）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **U21-D1** | **作用面：** 模板 Authoring Bindings 内 `ControlledStructuredContentEditor`（`TemplateAuthoringBindingEditor` 已传 `anchorId` + `devVersionId`）。无 `anchorId` 的挂载点（如条款 CM 创建/版本对话框）**保持** LR-C2 三元组键，不强制伪造锚点。 | 计划「草稿按锚点」；编辑器 props 已有可选 `anchorId` |
| **U21-D2** | **新键格式（有 anchorId）：** `docgen.structuredDraft.v1:{userId}:{templateId}:{devVersionId}:{anchorId}`。前缀与 schemaVersion=1 载荷字段不变；载荷仍可带 `anchorId`（冗余一致性校验，非键替代）。 | 扩展 C2-C3；最小破坏 |
| **U21-D3** | **读写清路径：** `buildStructuredDraftStorageKey` / `clearExactStructuredDraftOnSave` / composable `resolveKey` 在 `anchorId` 存在时使用四元组；Save 成功仅清 **当前锚点** 键。 | 修复跨锚点覆盖 |
| **U21-D4** | **Legacy 三元组键兼容（一次性）：** 挂载锚点 A 时优先读四元组键；若无，再读旧三元组键。仅当旧载荷 `anchorId == A` **或** `anchorId` 为空/缺省时，才对该锚点 `shouldOfferDraftRecovery`。Restore / 后续 debounce 写 / Discard / 成功 Save 针对 A 时：写入/清除四元组键，并 **删除** 该旧三元组键（避免残留串扰）。若旧载荷 `anchorId` 指向其他锚点 B，则 **不**对 A 展示恢复、**不**删除旧键。 | 不发明静默丢稿；对齐既有 payload 软过滤 |
| **U21-D5** | **并发令牌：** 复用实体已有 `AnchorBindingEntity.updatedAt`（ISO-8601），**不**新增独立 `@Version` 列（除非实现中证明 Instant 比较不足——默认不足时再开 pending）。 | 实体已有字段；计划「版本号」= 乐观锁令牌 |
| **U21-D6** | **契约：** `AnchorBindingView`（及 FE `AnchorBinding` / OpenAPI 管理视图对齐）增加 **required** `updatedAt`。`UpsertAnchorBindingRequest` 增加可选 `expectedUpdatedAt`：对 **已存在** 绑定的更新 **必须** 提供且与库中 `updatedAt` 相等（UTC Instant 语义相等）；缺失 → **422**（validation / `TEMPLATE_VALIDATION_FAILED` 族，messageKey 可区分）；不匹配 → **409** `CONFLICT`。 | View 今日无 `updatedAt`；upsert 无令牌 |
| **U21-D7** | **首次创建绑定**（该 `templateVersionId`+`anchorId` 尚无行）：允许省略 `expectedUpdatedAt`；成功响应返回新 `updatedAt`。 | 现状 create 分支 |
| **U21-D8** | **错误信封（冲突）：** HTTP **409**；`category=CONFLICT`；稳定 `errorCode`（建议 `BINDING_VERSION_CONFLICT`）；`messageKey=api.error.template.bindingVersionConflict`；`retryable=true`。经 `TemplateExceptionAdvice` / 统一 envelope 暴露。 | 对齐既有 409 模式（governance / immutable 等） |
| **U21-D9** | **UX：** 保存失败为冲突时，展示 i18n 提示（优先映射 `messageKey`；FE 兜底 key 如 `templates.authoring.bindingVersionConflict`）。文案须说明内容已被他人（或其他会话）更新，并提供明确动作：**Reload**（重新拉取该绑定，更新 baseline/`updatedAt`，可丢弃未保存服务端侧差异——作者确认后）与 **Keep editing**（关闭提示，保留本地 dirty + 本地草稿）。**禁止**在冲突路径调用 `markPristine` / 清除草稿。 | U21-C2「清晰提示」 |
| **U21-D10** | **成功路径：** `expectedUpdatedAt` 匹配 → 写入 → 响应含新 `updatedAt` → FE 更新本地令牌 → `markPristine` / clear per-anchor draft（LR-C2）。 | 既有 save 成功流 |
| **U21-D11** | **传 `serverUpdatedAt`：** 绑定编辑器将当前绑定的 `updatedAt` 传给编辑器 props（今日未传），供恢复横幅与草稿载荷；Save 时用同一值作 `expectedUpdatedAt`。 | props 已支持 `serverUpdatedAt` |
| **U21-D12** | i18n English-first（en + zh-CN）；API catalog + UI 文案同步。`frontend_ui_in_scope=true` → Stage 5/6/7 + 10；BE 契约 → `mvn verify`。 | constitutions |

### 4.3 待决（不阻塞 readiness；实现期锁定）

| ID | 问题 | 默认倾向（非确认需求） |
| --- | --- | --- |
| **U21-P1** | `updatedAt` Instant 比较是否允许时钟精度截断（DB 微秒 vs JSON 毫秒） | 实现用规范化比较（截断到毫秒）或返回服务端原文字符串原样回传 |
| **U21-P2** | Reload 是否用整页 refetch bindings 还是单锚点 GET | 优先复用现有 list/detail 刷新路径，无新端点除非必要 |

以上 **pending 不阻止** TDD；验收以 D5–D11 为准。

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 绑定编辑器结构变更（非 readonly） | 防抖写入 **四元组** 草稿键 |
| T2 | 切换编辑锚点 / 挂载编辑器 | 按当前锚点评估恢复；不读错锚点草稿 |
| T3 | 用户 Restore / Discard 草稿横幅 | 作用于当前锚点键（+ legacy 规则） |
| T4 | 用户 Save 绑定且 `expectedUpdatedAt` 匹配 | 成功持久化；清当前锚点草稿 |
| T5 | 用户 Save 绑定且令牌陈旧 | 409 + 冲突 UX；保留本地草稿 |
| T6 | Reload（冲突后） | 刷新服务端绑定与 `updatedAt` |

---

## 6. Preconditions

- 用户已登录；对目标模板 dev 线具备编辑权限；绑定面板非 `readonly`。  
- 路由/上下文可解析 `templateId`、`devVersionId`、当前 `anchorId`。  
- 目标锚点绑定已存在（更新冲突场景）或尚未创建（首次保存场景）。  
- 浏览器支持 `localStorage`（否则草稿降级同 LR-C2 §8）。  
- Docker 验收：`http://localhost:4173`（Playwright）。

---

## 7. Primary journey（成功路径）

1. 作者打开模板 Authoring → 编辑锚点 **A** 的结构化内容。  
2. 未 Save → 防抖写入键 `…:devVersionId:A`。  
3. 切换到锚点 **B** 并编辑 → 写入 `…:devVersionId:B`；**A** 的草稿仍在。  
4. 回到 **A** → 可按 LR-C2 规则恢复 **A** 的草稿（非 B）。  
5. Save **A** 且 `expectedUpdatedAt` 与服务端一致 → 200 + 新 `updatedAt`；清除 **A** 键草稿；pristine。  
6. （并发）另一会话已保存 **A** 后，本会话仍持旧令牌 Save → **409**；提示冲突；本地草稿保留；Reload 后带新令牌再 Save 成功。

---

## 8. 验收场景（Given / When / Then）

### BDD-CE-U21-DAC-001 — 草稿键包含 anchorId

```gherkin
Given 作者在 template T / devVersion V 编辑锚点 A（非 readonly）
And 会话用户为 U
When 结构发生未保存变更且防抖写入完成
Then localStorage 存在键 docgen.structuredDraft.v1:U:T:V:A
And 不存在（或不再作为权威写目标）仅三元组 U:T:V 的新写入覆盖 A/B 共用槽
```

### BDD-CE-U21-DAC-002 — 跨锚点无串写 / 无误恢复

```gherkin
Given 作者已在锚点 A 写入本地草稿内容 SA
When 作者切换到锚点 B 并写入不同草稿内容 SB
Then 键 …:A 的 structureJson 仍为 SA（未被 SB 覆盖）
And 在锚点 B 挂载时恢复横幅（若出现）仅基于 …:B 或合法 legacy 规则
And 在锚点 A 再次挂载时不得把 SB 当作可恢复草稿
```

### BDD-CE-U21-DAC-003 — Legacy 三元组键兼容迁移

```gherkin
Given localStorage 仅有旧键 docgen.structuredDraft.v1:U:T:V
And 其载荷 structureJson 与服务端不同，且 payload.anchorId 为 A 或为空
When 作者打开锚点 A 的结构化编辑器
Then 按 LR-C2 规则可展示恢复横幅
When 作者 Restore 或继续编辑导致新写入，或 Discard，或成功 Save
Then 使用四元组键 …:A 作为后续权威键
And 旧三元组键被删除（在 U21-D4 声明的认领条件下）
```

### BDD-CE-U21-DAC-004 — Legacy 键属于其他锚点时不误用

```gherkin
Given 旧三元组键载荷 payload.anchorId = B
When 作者打开锚点 A
Then 不因该旧键对 A 展示恢复横幅
And 不删除该旧键
```

### BDD-CE-U21-DAC-005 — 成功保存清除当前锚点草稿

```gherkin
Given 锚点 A 存在本地草稿且编辑器 dirty
And 客户端持有与服务端一致的 updatedAt 作为 expectedUpdatedAt
When 作者 Save 绑定且服务端返回成功
Then 四元组键 …:A 被清除
And 编辑器 pristine
And 锚点 B 的本地草稿（若有）不受影响
```

### BDD-CE-U21-DAC-006 — 更新保存乐观锁成功路径

```gherkin
Given 锚点 A 已有服务端绑定，View.updatedAt = T0
When 作者 PUT 绑定且 body.expectedUpdatedAt = T0，内容合法
Then 响应 2xx，result.updatedAt = T1 且 T1 ≠ T0（或严格晚于 T0）
And 持久化内容为请求中的 structuredContentJson
```

### BDD-CE-U21-DAC-007 — 陈旧 expectedUpdatedAt → 409 + 清晰提示

```gherkin
Given 会话持有 expectedUpdatedAt = T0
And 服务端该绑定 updatedAt 已变为 T1（其他会话已保存）
When 作者 Save（PUT）仍携带 expectedUpdatedAt = T0
Then 响应 HTTP 409，category=CONFLICT，messageKey=api.error.template.bindingVersionConflict
And 服务端 structuredContentJson 仍为其他会话已保存内容（本请求未写入）
And UI 展示 English-first 冲突提示（非空白 generic）
And 提供 Reload 与 Keep editing（或等价可发现动作）
And 本地草稿键 …:A 仍存在（Save 未成功）
```

### BDD-CE-U21-DAC-008 — 冲突后 Reload 再保存成功

```gherkin
Given BDD-CE-U21-DAC-007 已发生
When 作者选择 Reload
Then 编辑器加载服务端当前结构与 updatedAt = T1（或最新）
And 作者可再次编辑后以新 expectedUpdatedAt 成功 Save
```

### BDD-CE-U21-DAC-009 — 首次创建绑定无需 expectedUpdatedAt

```gherkin
Given 该 templateVersion 下锚点 A 尚无绑定行
When 作者 PUT 创建绑定且省略 expectedUpdatedAt
Then 创建成功并返回 updatedAt
And 不因缺少 expectedUpdatedAt 而 409
```

### BDD-CE-U21-DAC-010 — 更新缺少 expectedUpdatedAt → 校验失败

```gherkin
Given 锚点 A 已有绑定行
When 作者 PUT 更新且省略 expectedUpdatedAt
Then 响应 422（或项目统一校验失败形态），不写入新内容
And 不伪装为成功 last-write-wins
```

### BDD-CE-U21-DAC-011 — Fail-closed：无编辑权 / readonly

```gherkin
Given 会话不可编辑该模板绑定，或编辑器 readonly
When 挂载结构化编辑器或尝试 Save
Then 不写入 localDraft；不展示恢复横幅
And Save 不可达或服务端 403（既有 accessDenied），无乐观锁旁路
```

### BDD-CE-U21-DAC-012 — E2E：分锚点草稿 + 冲突提示旅程

```gherkin
Given Docker 验收栈健康且作者已登录
When 作者在两锚点分别产生本地草稿并验证隔离
And 模拟陈旧令牌保存触发冲突提示
Then 功能旅程 PASS
And UIUX 证据双品牌 Critical=0（银行 OA）
```

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| `localStorage` 不可用 | 同 LR-C2：不写不读；Save/冲突逻辑仍生效 |
| 损坏草稿 JSON | 删除坏键；无横幅 |
| 无 `anchorId` 的编辑器挂载 | 保持三元组键（非本片回归面） |
| 多标签同四元组键 | v1 仍后写覆盖；不做跨 tab 锁（LR-C2 已知限制） |
| 发布版本号冲突 UI | 不复用；文案/testid 分离 |
| CM / 变量 / rules 保存 | 不引入本片乐观锁 |
| 冲突时 dirty-guard | 仍可拦截导航；Discard 导航不清除 localStorage（LR-C2） |

---

## 10. Observable evidence

| 证据 | 命令 / 产物 |
| --- | --- |
| 单元 | `structuredContentDraftStorage` / `useStructuredContentLocalDraft` 四元组键 + legacy；binding upsert 冲突映射 |
| 后端 | `TemplateBindingMutationSupport` / advice：匹配写入、陈旧 409、缺令牌 422；`mvn verify` |
| 前端门禁 | `pnpm -C frontend lint` / `type-check` / `test` / `build` |
| E2E | `frontend/e2e/ce-u21-*.spec.ts`（命名实现期锁定） |
| UIUX | `frontend/e2e/evidence/CE-U21-*-manifest.md` + dual-brand screenshots |
| Deploy | `.\scripts\docker-deploy-queue.ps1` Stage 5/10 |

---

## 11. English-first UI / API copy keys

| Key | 用途（en 基座语义） |
| --- | --- |
| `api.error.template.bindingVersionConflict` | API 冲突 messageKey — 绑定已被更新，请重新加载后再保存 |
| `templates.authoring.bindingVersionConflict`（或实现期等价 FE 兜底） | Toast/对话框标题或正文兜底 |
| `templates.authoring.bindingVersionConflictReload` | **Reload** 动作 |
| `templates.authoring.bindingVersionConflictKeepEditing` | **Keep editing** 动作 |
| 既有 `templates.structuredEditor.draftRecovery.*` | 恢复横幅 — 复用，不改契约语义 |

（最终 key 路径以实现与 i18n catalog 对齐为准；验收要求 en + zh-CN 均有条目。）

---

## 12. Traceability

| 来源 | 本规格 |
| --- | --- |
| CE 计划 §4 CE-U21 | DAC-01…04；U21-C1/C2 |
| Task Master **#95** | 全片 |
| LR-C2 / `structuredContentDraftStorage.ts` | 键前缀、恢复横幅、clear-on-save、配额 |
| `AnchorBindingEntity.updatedAt` | U21-D5 并发令牌 |
| `PUT /templates/{id}/bindings/{anchorId}` | U21-D6…D10 |
| permission-matrix template authoring | U21-D11 / DAC-011 |
| CE-U20 non-goal 声明 | 本叶接手 |

---

## 13. BDD readiness

```
bdd_readiness: ready
open_questions: []
pending_non_blocking: ["U21-P1", "U21-P2"]
owning_doc: docs/behavior/ce-u21-draft-anchor-concurrency.md
task_ids: ["95", "CE-U21"]
frontend_ui_in_scope: true
batch_recommendation: solo
member_task_ids: ["95"]
proposed_slice_id: ce-u21-draft-anchor-concurrency
```

**Handoff:** `plan-orchestrator` → 实现任务分解（FE draft key + conflict UX；BE binding `updatedAt` / `expectedUpdatedAt` + 409；OpenAPI；E2E/UIUX）。
