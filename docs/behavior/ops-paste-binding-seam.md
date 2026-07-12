# BDD 行为规格：Paste cleaning ↔ binding / publish fail-closed seam

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-12  
**BDD ID**: `BDD-OPS-PASTE-BINDING-001`  
**来源**: LR-E2 checklist item **#5b** · Ledger seam「Paste cleaning ↔ binding validation」· **ADR-0019**（Accepted — paste must be cleaned; script / embedded object / iframe / absolute positioning **block**; summary as editing **or** release-check evidence）· CD-HARD-T05 adjacency  
**程序 / 清单**: [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) · [launch-readiness-program.md](../plan/launch-readiness-program.md) · [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md)（CD-HARD-T05）  
**Task / slice**: `ops-paste-binding-seam`  
**Worktree**: merged to MAIN (`f1f00da`); feature worktree removed  
**授权依据**: 本会话用户 / parent 明确方向（2026-07-12）— 关闭缝隙使无效粘贴**不能**静默通过绑定/发布；偏好 **durable fail-closed / 用户可见门禁**；**禁止**以「ADR 接受 edit-time-only」逃逸；「自动继续」  
**完成声明约束**: 本切片**仅**处理 checklist **#5b** 与 CD-HARD-T05「wire」路径；**禁止**据此宣称 production go-live。整体清单在 **#3b** 等未关闭前仍为 **NO-GO**。清除 #5b alone ≠ go-live。

---

## 1. 概述

Word/HTML 粘贴清洗（P18-T07/T10）今日**仅**在编辑时运行：`PasteCleaningService` + `POST .../paste-clean` + 摘要对话框。清洗结果**不**进入 `computeBindingStatus` / `PublishGateService`；实现分类**松于** ADR-0019（`<object>` → `REMOVED`、`position:absolute` → `WARNING`，仍可 Accept 进绑定）。发布路径因此可能对「ADR 已要求阻断的粘贴构造」**静默放行**。

本切片关闭该缝：分类对齐 ADR-0019；Accept 后在绑定上持久化**非敏感**粘贴清洗证据（residue）；`computeBindingStatus` / 发布门禁对未解除的粘贴阻断 **fail-closed**；发布 checklist / 绑定状态**用户可见**；干净重写清除 residue。

| 行为域 | 摘要 |
| --- | --- |
| **PB-C1 Classify** | 粘贴源中 script / javascript: / iframe / **embedded object** / **absolute positioning**（及 ADR 已列且可检测的同类构造）→ **`BLOCKED`**；整次粘贴 `blocked=true`，无 cleaned JSON，Accept 禁用 |
| **PB-C2 Residue** | 用户 **Accept** 成功路径后，绑定持久化非敏感 paste-cleaning residue（摘要计数 + 稳定 messageKey / detectionSummary；**禁止**源 HTML / 粘贴明文） |
| **PB-C3 Binding gate** | `computeBindingStatus`（及 `validateBindings` / 保存绑定）在绑定存在**未解除粘贴阻断**时 → **非 VALID**（汇入 `INCOMPATIBLE_CONTENT_TYPE` 或等价专用状态），不得被当作可发布绑定 |
| **PB-C4 Publish gate** | `PublishGateService` fail-closed：未解除粘贴阻断 → checklist 项 **FAIL**；用户可见稳定 messageKey；不得进入 `PUBLISHED` |
| **PB-C5 Clear** | 干净重写（无 BLOCKED 的成功 Accept，或显式清除 residue 的等价写路径）→ 清除该绑定上的粘贴阻断 residue |
| **PB-C6 Checklist** | 交付后 #5b → **GO**（wire 路径，非 ADR edit-time-only 逃逸）；overall 仍 **NO-GO**（#3b 等） |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| ADR-0042 / ADR-0043 / Word host 残差 | **Out of scope**（checklist **#3b**） |
| 真实企业 AD / LDAP 目录 | **Out of scope**（#5a residual） |
| 完整激活 CD-3 / 宣称 go-live | **禁止** |
| 发明公司 LDAP / Kafka 坐标 | **禁止** |
| 以「ADR 接受 paste = edit-time-only」关闭 #5b | **禁止** — 用户锁定 durable fail-closed |
| 与 **LR-A4**（writer-unsupported / `qrBarcodeRef` / `attachmentListRef`）混为一谈 | **禁止** — 不同缝；本切片只处理 paste cleaning ↔ binding |
| 实现完整 Word→结构化矩阵（复杂表格、外链图、宏解析深度等） | **Out of scope** — 仅关闭分类 + residue + 门禁；既有 paragraph 转换可保留 |
| 改写 ADR-0019 已接受决策正文为「edit-time-only」 | **禁止** |
| 触碰 `DGE-audit-governance` | **禁止** |

---

## 2. Source-of-truth 冲突与裁定

| 来源 | 陈述 | 本切片裁定 |
| --- | --- | --- |
| **[ADR-0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md)**（Accepted） | Scripts, macros, **embedded objects**, iframes, external resources, **absolute positioning**, floating layout, … **block** the paste from entering the template. Summaries/blockers are traceable as **editing or release-check** evidence (no sensitive plaintext). | **确认 SoT** — 本切片行为以 ADR-0019 为准 |
| **[domain-model.md](../domain/domain-model.md) §2.6.7**（描述现状） | `<object>` → REMOVED；`position:absolute` → WARNING；仅 script/iframe 为 BLOCKED；**未**接线 `computeBindingStatus` | **描述性偏差 / 实现松弛** — **不得**当作已接受「可发布」行为；实现与文档须在交付中对齐 ADR-0019（doc-keeper / post-task-doc-sync 更新 domain-model） |
| Ledger / checklist #5b / CD-HARD-T05 | 「Wired to publish gate **OR** ADR documents edit-time-only」 | 用户锁定：**仅 wire 路径**；**不**走 edit-time-only ADR 逃逸 |
| **LR-A4** BDD | unsupported / writer-missing 节点 fail-closed | **正交** — 不替代本缝；不共享验收场景 |

**Confirmed requirement（本切片）:** paste blockers per ADR-0019 + residue on binding + binding/publish fail-closed + user-visible gate.  
**Pending / out of scope:** company Word host residual (#3b); inventing extra paste detectors beyond ADR minimum unless already trivial.

---

## 3. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **模板编排人员** | `TEMPLATE_AUTHOR` / `MASTER_DESIGNER` / 具备模板写权限 | 在受控编辑器中粘贴 Word/HTML；查看清洗摘要；Accept / Cancel / Undo；保存绑定 |
| **审批 / 发布操作者** | `TEMPLATE_APPROVER` / 具备 publish 权限 | 打开发布 checklist；存在粘贴阻断时不得发布 |
| **系统（粘贴清洗）** | `PasteCleaningService` + management `paste-clean` API | 分类、摘要、blocked 标志；不落敏感明文 |
| **系统（绑定 / 发布）** | `TemplateBindingConfigurationService.computeBindingStatus` + `PublishGateService` | 读取 residue；未解除阻断 → fail-closed |
| **发布评审人** | Launch reviewer | 交付证据齐全后将 checklist **#5b** 标 **GO**；不据此签 overall go-live |

授权：跨组 / 无权限 → 既有 fail-closed；本规格不改 permission-matrix。

---

## 4. Goal

1. 粘贴分类与 ADR-0019 对齐：至少 **embedded object** 与 **absolute positioning** 与 script/iframe 同为 **BLOCKED**（不得再以 REMOVED/WARNING 放行 Accept）。  
2. Accept 后绑定持久化**非敏感** paste-cleaning residue，可作为编辑或发布检查证据。  
3. 存在未解除粘贴阻断时，`computeBindingStatus` / 保存校验 **非 VALID**。  
4. 发布门禁 **fail-closed**，checklist **用户可见**。  
5. 干净重写清除 residue，恢复可发布路径（其它门禁仍适用）。  
6. checklist **#5b** → **GO**（wire）；**禁止** go-live；overall 在 #3b 未关时仍 **NO-GO**。

---

## 5. 已确认决策（confirmed）

| ID | 决策 |
| --- | --- |
| **PB-C1** | **ADR-0019 为粘贴阻断 SoT**：检测到下列构造时，摘要项必须为 **`BLOCKED`**，且整次结果 `blocked=true`（无 `cleanedStructuredContentJson`；UI Accept **禁用**）：`<script>` / `javascript:`、`<iframe>`、**`<object>`（embedded object）**、**`position:absolute`（absolute positioning）**。既有 script/iframe 阻断保持。 |
| **PB-C2** | **禁止降级**：不得将 object 标为 `REMOVED`、将 absolute 标为 `WARNING` 后仍允许 Accept 进入模板绑定（相对今日实现的**行为变更**）。 |
| **PB-C3** | **可选增强（非阻塞本切片）**：若实现已能可靠检测 ADR 所列其它构造（macros、floating layout、complex columns 等），同样 **BLOCKED**；未实现的检测器**不** invent 假阳性，也**不**用「未检测」证明可发布——本切片最低门槛是 PB-C1。 |
| **PB-C4** | **Residue 持久化（Accept 成功）**：当用户 Accept 且 `blocked=false` 时，随绑定保存**非敏感** residue，至少包含：`transformedCount` / `removedCount` / `warningCount` / `blockedCount`、条目级 `category` + `messageKey` + 非敏感 `detectionSummary`（或等价结构化字段）。**禁止**持久化源 HTML、粘贴正文、客户敏感字段。 |
| **PB-C5** | **未解除阻断定义**：绑定上存在 paste residue 且 `blockedCount > 0`，或显式 `unresolvedPasteBlockers=true`（或等价标志），或 API/存储被篡改导致 residue 含 `BLOCKED` 项 → 视为**未解除粘贴阻断**。 |
| **PB-C6** | **绑定门禁**：`computeBindingStatus` 在未解除粘贴阻断时不得返回 `VALID`；映射为 `INCOMPATIBLE_CONTENT_TYPE` **或** 文档化的等价专用 `BindingValidationStatus`（若新增须 en 基 i18n + 发布门禁汇入）。保存 / `validateBindings` 与之一致。 |
| **PB-C7** | **发布门禁**：`PublishGateService` 在任一绑定存在未解除粘贴阻断时，相关 checklist 项 **FAIL**（优先专用 `PublishGateCheckCode` 如 `PASTE_CLEANING_BLOCKERS`，**或** 汇入既有 `ANCHOR_INTEGRITY` / `BLOCKER_STATUS` 且详情可展示稳定 messageKey）。**禁止**仅日志、UI 无原因。 |
| **PB-C8** | **用户可见**：编排人员在绑定状态 / 结构化编辑面，以及发布 checklist 上能看到可翻译原因（English-first `messageKey` + zh-CN）；不得静默成功发布。 |
| **PB-C9** | **清除 residue**：对同一锚点绑定，一次**无阻断**的粘贴 Accept（或等价「以干净内容树重写并明确清除 paste residue」的保存路径）必须清除既有未解除阻断 residue。Cancel / Undo 回粘贴前状态时，不得写入新的阻断 residue。 |
| **PB-C10** | **纵深防御**：即使 UI 禁用 Accept，管理 API / 直接写绑定若携带未解除阻断 residue 或试图绕过清洗写入，后端仍须在校验/发布路径 fail-closed。 |
| **PB-C11** | **不采用 edit-time-only ADR 逃逸**：CD-HARD-T05 / #5b 关闭方式 = **wire**；不得新增 ADR 声称「paste 仅编辑时、可不进发布门禁」为 v1 接受范围。 |
| **PB-C12** | **Checklist #5b**：实现 + 门禁证据后 → **GO**；ledger seam「Paste cleaning ↔ binding validation」标为 **wired / closed**；**overall** 在 #3b 等未关时仍 **NO-GO**；**禁止** go-live。 |
| **PB-C13** | **与 LR-A4 分离**：本切片不改 writer-unsupported 集合；场景与 messageKey 不得复用为「粘贴清洗」的唯一证据。 |

### 5.1 上游现状（implementation 输入，非已验收行为）

| 发现 | 证据 |
| --- | --- |
| object → REMOVED | `PasteCleaningService.analyzeSource`：`OBJECT_PATTERN` → `REMOVED` |
| absolute → WARNING | 同文件：`ABSOLUTE_POSITION_PATTERN` → `WARNING` |
| script / iframe → BLOCKED | 同文件；`blocked` 聚合任一 BLOCKED |
| Accept 仅禁 blocked | `PasteCleaningSummaryDialog` `:disabled="blocked"`；WARNING/REMOVED 仍可 Accept |
| 无 binding residue | 绑定实体 / `computeBindingStatus` **不**读 paste summary |
| computeBindingStatus 范围 | 节点矩阵 / 样式 / 表 / 引用 / 编号 blockers → `INCOMPATIBLE_CONTENT_TYPE`；**无** paste |
| domain-model 松弛描述 | §2.6.7 记录 REMOVED/WARNING 为「v1」——与 ADR-0019 冲突 |
| 清单 #5b | **GO** — Wired to publish gate (merge `f1f00da`; wire path — not edit-time-only ADR) |
| CD-HARD-T05 | **Done** (2026-07-12) — Task Master **#47** / this slice (**wire** path; **no** edit-time-only escape) |
| Ledger seam | **closed** — residue → `computeBindingStatus` + PublishGate fail-closed |

---

## 6. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 用户粘贴含 object 和/或 absolute positioning 的 HTML | 清洗 → BLOCKED；Accept 禁用（S1） |
| T2 | 用户粘贴仅含可转换段落（无 ADR 阻断构造） | 清洗成功；Accept 写入内容 + 非敏感 residue（S2） |
| T3 | 保存绑定 / `validateBindings` / `computeBindingStatus` 且存在未解除粘贴阻断 | 绑定非 VALID（S3） |
| T4 | 发布评估 / publish 且存在未解除粘贴阻断 | 门禁 FAIL；不可 PUBLISHED（S4） |
| T5 | 干净重写同一绑定（无阻断 Accept 或等价清除） | residue 清除；粘贴维门禁通过（S5） |
| T6 | 发布评审人更新 checklist #5b | 按 PB-C12 → **GO**（交付后 doc-sync）（S6） |

---

## 7. Preconditions

- 工作树：`feat/ops-paste-binding-seam` / `D:/working/DGE-ops-paste-binding-seam`。  
- Formal phase：**None**；不触碰 `DGE-audit-governance`；不激活 CD-3。  
- ADR-0019 Accepted 仍有效；本切片**实现**其对 paste 的要求，不改 ADR Decision 正文。  
- P18-T07/T10 编辑时清洗 UI/API 已存在；本切片扩展分类、持久化与门禁。  
- LR-A4 行为保持；不混验收。

---

## 8. Primary journey（成功 — 本切片范围内）

1. Author 粘贴干净 HTML → summary 无 BLOCKED → Accept → 绑定保存受控节点 + 非敏感 residue（blockedCount=0）。  
2. `computeBindingStatus` 在粘贴维返回 VALID（其它校验另计）。  
3. Publish checklist 粘贴相关项 PASS（其它项另计）。  
4. Author 粘贴含 `<object>` 或 `position:absolute` → BLOCKED → 无法 Accept；模板内容不因该次粘贴进入绑定。  
5. 若绑定仍带未解除阻断 residue（历史/API）→ 绑定状态与发布 checklist **显式 FAIL**。  
6. Author 以干净内容重写并清除 residue → 粘贴维门禁恢复。  
7. doc-sync：#5b → **GO**；overall 仍 **NO-GO**（#3b）；**禁止** go-live。

---

## 9. System responses

| 路径 | 系统响应 |
| --- | --- |
| paste-clean 含 object / absolute / script / iframe | `blocked=true`；摘要含 `BLOCKED`；无 cleaned JSON；Accept 禁用 |
| paste-clean 干净可转换 | `blocked=false`；cleaned JSON；Accept 可用 |
| Accept 成功 | 应用 cleaned 树；持久化非敏感 residue；无源 HTML |
| 绑定存在未解除粘贴阻断 | `computeBindingStatus` ≠ `VALID`；API/UI 可见 |
| 发布存在未解除粘贴阻断 | PublishGate 项 FAIL；不可发布 |
| 干净重写 | 清除阻断 residue；粘贴维通过 |
| 清单 | #5b GO（交付后）；不宣称 go-live |

---

## 10. Acceptance scenarios（Given / When / Then）

### S1 — object / absolute positioning：粘贴期 BLOCKED（对齐 ADR-0019）

**BDD-OPS-PASTE-BINDING-001 / S1**

```
Given 模板编排人员在受控结构化编辑器中对某锚点绑定发起粘贴
  And 源 HTML 含 <object> 和/或 CSS/内联 style 中的 position:absolute
    （可分别或同时覆盖；回归须覆盖两者至少各一例）
When 调用 paste-clean（PasteCleaningService / POST .../paste-clean）
Then 结果 blocked=true
  And summary 中对应项 category=BLOCKED（不得再为 REMOVED 或 WARNING）
  And cleanedStructuredContentJson 为空/null
  And UI Accept 控件禁用
  And 该次粘贴不得将内容写入绑定内容树
  And summary / 日志不得包含源 HTML 敏感明文
```

### S2 — 干净粘贴：Accept + 非敏感 residue

**BDD-OPS-PASTE-BINDING-001 / S2**

```
Given 源 HTML 仅含可转换为受控 paragraph/textRun 的内容
  And 不含 PB-C1 所列阻断构造
When paste-clean 返回 blocked=false 且用户 Accept，并保存绑定
Then 绑定内容树为清洗后的受控结构化 JSON
  And 绑定持久化非敏感 paste-cleaning residue（含计数与 messageKey；blockedCount=0）
  And residue 不含源 HTML 或粘贴正文明文
```

### S3 — 未解除粘贴阻断 → computeBindingStatus fail-closed

**BDD-OPS-PASTE-BINDING-001 / S3**

```
Given 某锚点绑定带有未解除粘贴阻断 residue
  （blockedCount>0 或等价 unresolvedPasteBlockers；含 API 直写纵深场景）
When 执行 computeBindingStatus / validateBindings / 保存绑定校验
Then 该绑定不得为 VALID
  And 状态为 INCOMPATIBLE_CONTENT_TYPE 或文档化等价专用状态
  And 调用方/UI 可获得稳定可翻译 messageKey（粘贴阻断语义）
```

### S4 — 发布门禁用户可见 fail-closed

**BDD-OPS-PASTE-BINDING-001 / S4**

```
Given 模板版本至少一处绑定存在未解除粘贴阻断（S3）
When 发布操作者获取 PublishGate checklist 或尝试 publish
Then 相关 checklist 项为 FAIL（专用 PASTE_CLEANING_BLOCKERS 或汇入 ANCHOR_INTEGRITY/BLOCKER_STATUS）
  And 失败原因用户可见（稳定 messageKey；English-first + zh-CN）
  And 模板不得进入 PUBLISHED
```

### S5 — 干净重写清除 residue

**BDD-OPS-PASTE-BINDING-001 / S5**

```
Given 某锚点绑定曾存在未解除粘贴阻断 residue
When 用户以无阻断的粘贴 Accept 覆盖该绑定内容，或经等价保存路径显式写入干净内容并清除 paste residue
Then 该绑定上的未解除粘贴阻断 residue 被清除
  And 在无其它 blockers 时 computeBindingStatus 可为 VALID
  And 粘贴相关 PublishGate 项不再因该 residue 失败
```

### S6 — Checklist #5b → GO（wire；overall 仍 NO-GO）

**BDD-OPS-PASTE-BINDING-001 / S6**

```
Given 本切片已交付：PB-C1 分类对齐、residue、binding/publish fail-closed、用户可见门禁
  And 有可审计门禁/测试证据
When 发布评审人 / post-task-doc-sync 更新 launch-readiness-checklist.md #5b
Then #5b verdict 为 GO（wired to publish gate — 非 edit-time-only ADR）
  And ledger seam「Paste cleaning ↔ binding validation」记为 wired/closed
  And CD-HARD-T05 记为 Done（wire 路径）
  And 不得宣称 overall production go-live
  And overall checklist 在 #3b 等未关时仍为 NO-GO
```

---

## 11. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 仅 WARNING/REMOVED、无 BLOCKED（历史实现路径） | **不满足** PB-C1/C2 — object/absolute **必须** BLOCKED |
| Accept 被禁用但 API 仍 POST 旧 cleaned JSON | 后端保存/校验仍按内容与 residue 规则；不得因 UI 假定而信任客户端 |
| Residue 含 BLOCKED 但内容树「看起来干净」 | 仍 fail-closed（PB-C5/C10）直至清除 residue |
| Cancel / Undo | 恢复 pre-paste；不写入新阻断 residue |
| 敏感明文 | summary / residue / audit **禁止**源 HTML、客户数据、完整请求体 |
| LR-A4 writer-unsupported | 独立 blockers；本切片不替代、不弱化 |
| 复杂表格 / 外链图等「须走受控组件流」 | 保持 ADR-0019 既有「不静默导入」精神；本切片不扩展完整导入器 |
| edit-time-only ADR 提案 | **拒绝**作为 #5b 关闭方式（PB-C11） |
| 无 UI 文案仅后端抛错 | **不满足** PB-C7/C8 — checklist/绑定状态须可见 |

---

## 12. Observable evidence

| 证据 | 用途 |
| --- | --- |
| `PasteCleaningServiceTest`（或扩展）：object → BLOCKED；absolute → BLOCKED；script/iframe 回归 | S1 TDD |
| 绑定持久化字段 / API 契约：非敏感 paste residue；无源 HTML | S2 |
| `computeBindingStatus` / binding 服务测：未解除阻断 → 非 VALID | S3 |
| `PublishGateService` 测 + checklist messageKey | S4 |
| 清除 residue 后 VALID / gate PASS（粘贴维） | S5 |
| checklist #5b GO + ledger seam closed（doc-sync 后） | S6 |
| 可选：管理 UI / E2E — Accept 禁用 + publish checklist 可见失败 | S1/S4 用户可见 |

---

## 13. Traceability

| 文档 | 关系 |
| --- | --- |
| [ADR-0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) | **Confirmed SoT** — paste block list + summary as editing/release-check evidence |
| [domain-model.md](../domain/domain-model.md) §2.6.7 | **冲突（松弛现状）** — 交付时对齐为 BLOCKED + binding/publish 接线 |
| [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) **#5b** | 本 BDD 验收规格；交付后 → GO |
| [execution-sync-ledger.md](../plan/execution-sync-ledger.md) seams「Paste cleaning ↔ binding validation」 | 退出标准：wire（本切片） |
| [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md) **CD-HARD-T05** | **Done** (wire under #47; merge `f1f00da`) |
| [lrp-a4-fail-closed-unsupported-nodes.md](./lrp-a4-fail-closed-unsupported-nodes.md) | **正交缝** — 勿混 |
| P18-T07 / P18-T10 | 既有 edit-time 清洗与 UI；本切片扩展 |
| [ops-ad-group-stub-close.md](./ops-ad-group-stub-close.md) 等 | 同族 ops checklist 诚实关闭模式（本项预期 **GO** 而非 CONDITIONAL） |

---

## 14. TDD Red 提示（交给 backend-engineer / frontend-engineer）

1. **PasteCleaningService**：含 `<object>` 的源 → `blocked=true` 且 category `BLOCKED`（打破今日 REMOVED）。  
2. 含 `position:absolute` → 同 S1（打破今日 WARNING）。  
3. script / iframe 回归仍 BLOCKED。  
4. **Binding**：保存带未解除 paste residue → `computeBindingStatus` ≠ VALID。  
5. **PublishGate**：未解除 residue → checklist FAIL；无 residue / 已清除 → 粘贴维不拦。  
6. Residue 序列化断言：**无**源 HTML 字段。  
7. Frontend（若本切片含 UI）：Accept disabled on blocked；checklist/绑定状态展示 messageKey（英基 + zh-CN）。

---

## 15. Handoff（plan-orchestrator / doc-keeper / engineers）

| 下游 | 动作 |
| --- | --- |
| **plan-orchestrator** | 分配/激活 Task Master 任务；sole-active = `ops-paste-binding-seam`；分解：分类对齐 ADR → residue 模型 → `computeBindingStatus` → PublishGate → UI 可见 → 测试；**不**激活 CD-3 / #3b / LDAP |
| **doc-keeper** | **Done (docs-first 2026-07-12):** domain-model §2.6.7 + OpenAPI/contract-outline `pasteCleaningEvidence` + permission note + CD-HARD-T05 / #5b pending notes aligned to ADR-0019; **禁止**起草「edit-time-only accepted」ADR；checklist/ledger **GO** flip 仍由 post-task-doc-sync 在门禁绿后执行 |
| **backend-engineer** | TDD：S1–S5；`mvn verify` GREEN |
| **frontend-engineer** | 若绑定/发布表面需展示新 messageKey / residue：英基 i18n + OA；E2E 按管线 |
| **post-task-doc-sync** | #5b GO；CD-HARD-T05 Done；ledger seam closed；overall NO-GO 诚实 |

---

## Change log

| Date | Change |
| --- | --- |
| 2026-07-12 | Initial BDD authored (`ready`) for slice `ops-paste-binding-seam` (BDD-OPS-PASTE-BINDING-001 S1–S6); SoT conflict domain-model vs ADR-0019 resolved toward ADR-0019; wire path locked (no edit-time-only escape). |
| 2026-07-12 | Plan activation: Task Master **#47** in-progress; sole-active = this slice; CD-HARD-T05 adjacency In Progress (wire); checklist #5b remains NO-GO until implement + doc-sync. |
| 2026-07-12 | Slice **Done** (merge `f1f00da`); checklist #5b → **GO**; CD-HARD-T05 → **Done**; ledger seam closed; Task Master **#47** → done; sole-active cleared; overall checklist still **NO-GO** (#3b). |
