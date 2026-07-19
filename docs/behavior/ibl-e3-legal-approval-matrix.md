# BDD 行为规格：IBL-E3 — Multi-stage legal→compliance approval matrix + forced legal-reviewer（F26 / PD-8）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-E3` |
| **编写日期** | 2026-07-20 |
| **程序 / 队列** | IBL Wave E · **IBL-E3** / F26（`ibl-e3-legal-approval-matrix`） |
| **Slice** | `ibl-e3-legal-approval-matrix` |
| **Branch** | `feat/ibl-e3-legal-approval-matrix` |
| **Worktree** | `D:/working/DGE-ibl-e3-legal-approval-matrix` |
| **Placement** | ISOLATED |
| **Base** | `e66a3bbf`（#129 IBL-E2 Done on main；sole-active cleared at closeout） |
| **Task Master** | **#130** IBL-E3 — Batch Recommendation **solo**；`member_task_ids: ["130"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-e3-legal-approval-matrix`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；[ADR-0064](../adr/template-lifecycle/0064-legal-compliance-approval-matrix.md) **Accepted**（2026-07-20；Decision = E3-C*；Accepted ≠ impl Done）；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F26 / IBL-E3 / **PD-8**；基线 [ADR-0021](../adr/template-lifecycle/0021-template-testing-approval-release-governance.md)（一级审批 → 可配置多级；Amendment 2026-07-20）；[lifecycle-review.md](../domain/lifecycle-review.md)；[domain-model.md](../domain/domain-model.md) §4；[permission-matrix.md](../security/permission-matrix.md)；CE-G01 [ce-g01-self-approval-block.md](./ce-g01-self-approval-block.md)；CE-K08 [ce-k08-clause-legal-metadata.md](./ce-k08-clause-legal-metadata.md)（法务元数据**仍可选**）；既有审批旅程 [approver-decision-journey.md](./approver-decision-journey.md) |
| **Frontend UI** | **`frontend_ui_in_scope=true`**（owners = doc-keeper → backend-engineer **+ frontend-engineer**；审批矩阵配置 + 分阶段决策面 + 角色队列/旅程为用户面；E2E/UIUX **required**） |

**完成声明约束：** 本叶关闭 F26「法务元数据可选且仅一级审批轨、无法务→合规多级矩阵、无强制 legal-reviewer」中的 **审批治理缺口**——模板可配置并强制执行 **LEGAL → COMPLIANCE** 两级有序审批，且 LEGAL 阶段**强制**由 `LEGAL_REVIEWER`（或具备例外干预权的管理员路径）判定。CE-K08 法务元数据字段**保持可选**（本叶不强制填写）。**SPECIMEN 水印不得在本叶移除**（PD-6 意图 ≠ E3 实现）。**禁止**激活 IBL-E4…E7 / #119；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 go-live / Wave E / IBL 程序 Done。母版审核、条款独立审批轨、outbound delivery（PD-1）**不**被本叶改写为多级矩阵。

```
bdd_readiness: ready
frontend_ui_in_scope: true
open_questions: []
owning_doc: docs/behavior/ibl-e3-legal-approval-matrix.md
task_ids: ["130"]
suggested_adr: 0064 — docs/adr/template-lifecycle/0064-legal-compliance-approval-matrix.md (Accepted 2026-07-20; Decision = E3-C*; Accepted ≠ impl Done)
scenario_count: 18
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["130"]
  proposed_slice_id: ibl-e3-legal-approval-matrix
  shared_acceptance_surface: >
    Multi-stage legal→compliance approval matrix configured and enforced;
    LEGAL_REVIEWER role + permission-matrix rows; management UI for mode + stage decisions
  vetoes_applied:
    - IBL-E4-entity-brands
    - IBL-E5-effectiveFrom-bulk
    - IBL-E6-nesting-governance
    - IBL-E7-RTL-spike
    - PD-6-specimen-removal
    - PD-7-licensed-fonts
    - IBL-B7-Word-host
    - umbrella-106-registry-only
    - checklist-3b-5a-go
    - force-ce-k08-legal-metadata-required
    - multi-stage-master-or-content-module-lifecycle
  evidence_amortization: >
    mvn verify + FE lint/type-check/test/build + Playwright functional+UIUX
    + queued docker deploy (stages 5/10 as pipeline requires)
```

| IN（本叶） | OUT（明确禁止 / 后续叶） |
| --- | --- |
| ADR（建议 **0064**）Accepted；Decision = E3-C* | **PD-6** 去 SPECIMEN / true re-issue |
| 模板包级 `approvalMatrixMode`：`SINGLE_TRACK` \| `LEGAL_THEN_COMPLIANCE` | **PD-7** 授权字体；**#119** Word |
| 有序两级：`LEGAL` → `COMPLIANCE`；不可跳级、不可反序 | **IBL-E4** 法人品牌；**IBL-E5** bulk；**IBL-E6** 嵌套；**IBL-E7** RTL |
| 新角色 `LEGAL_REVIEWER` + capability `decideLegalApprovals`；permission-matrix / 角色分配 / 路由可见性 | 强制 CE-K08 `jurisdiction`/`legalReviewRef` 必填 |
| `approvalSubState` 扩展：`PENDING_LEGAL_DECISION` / `PENDING_COMPLIANCE_DECISION`（多级模式） | 母版审核多级化；条款版本多级化 |
| 协作待办：LEGAL 队列 + COMPLIANCE（既有 APPROVAL）队列 | 代理审批 / 自动通过 / 邮件外发（ADR-0021 仍禁） |
| 管理 UI：模式配置 + 分阶段决策面 + Dashboard 队列分轨 | 翻转 **#3b/#5a**；go-live；宣称 Wave E Done |
| Gates：BE verify + FE 四门 + E2E/UIUX + queued deploy | 四眼发布复核（CE-G01 D1 仍仅同人阻断） |

---

## 1. 概述

### 1.1 问题（F26）

| 发现 | 证据 |
| --- | --- |
| 模板审批为**一级**（`TEMPLATE_APPROVER` 单次通过 → 待发布） | ADR-0021；lifecycle-review「审批为一级审批」；domain §4 |
| 无法务→合规**有序多级**矩阵；无强制 legal-reviewer 角色 | 程序 F26；permission-matrix 角色目录 = 7 角色 |
| CE-K08 法务元数据仍可选（本叶**不**关闭「可选」——关闭的是审批轨缺口） | [ce-k08-clause-legal-metadata.md](./ce-k08-clause-legal-metadata.md) |
| 国际银行信函需 LEGAL 与 COMPLIANCE 分责、可审计、不可跳级 | PD-8 Confirmed 2026-07-19 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **E3-S1 Matrix mode** | 模板包声明 `approvalMatrixMode`；草稿可配置；多级模式下提交后进入 LEGAL 阶段 |
| **E3-S2 Legal stage** | 仅 `LEGAL_REVIEWER`（+ 管理员正常/例外路径见 E3-C*）可判定 LEGAL；通过 → COMPLIANCE；拒绝 → DRAFT |
| **E3-S3 Compliance stage** | 仅 `TEMPLATE_APPROVER`（合规轨，+ 管理员）可判定 COMPLIANCE；通过 → `PENDING_RELEASE`；拒绝 → DRAFT |
| **E3-S4 Role matrix** | 新角色 + capability + 分配边界 + Dashboard/旅程可见性写入 permission docs |
| **E3-S5 UI** | 模式配置、阶段指示、分角色决策对话框、队列分轨（Bank OA + English-first） |
| **E3-S6 Orthogonal** | 测试轨不变；发布二次确认不变；CE-G01 同人阻断每阶段生效；CE-K08 可选元数据不变 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板编排人员** | `TEMPLATE_AUTHOR` / `MASTER_DESIGNER` + `authorTemplates` | 配置 `approvalMatrixMode`；提交审批；不可判定 LEGAL/COMPLIANCE（除非兼角色） |
| **法务审阅人** | **`LEGAL_REVIEWER`** + `decideLegalApprovals` | **强制** LEGAL 阶段正常判定人（多级模式） |
| **合规/业务审批人** | `TEMPLATE_APPROVER` + `decideApprovals` | COMPLIANCE 阶段正常判定人（多级）；`SINGLE_TRACK` 下行为与 ADR-0021 一致 |
| **分组/全局管理员** | `GROUP_ADMIN` / `GLOBAL_ADMIN` | 可配置模式；可分配 `LEGAL_REVIEWER`；可走既有例外干预；具备两阶段判定能力（见 E3-C12） |
| **测试人员** | `TEMPLATE_TESTER` | 测试轨不变；不获 LEGAL/COMPLIANCE 判定权 |
| **系统** | 生命周期状态机 / 待办 / 审计 / UI 门禁 | Fail-closed 错阶段/错角色；同人阻断；结构化意见表单 |

---

## 3. Goal

1. 关闭 F26 审批缺口：提供可配置的 **legal→compliance** 多级矩阵并在运行时**强制执行**阶段顺序与角色边界。  
2. 引入并强制使用 **`LEGAL_REVIEWER`** 完成 LEGAL 阶段（多级模式不可跳过、不可由纯 `TEMPLATE_APPROVER` 代批 LEGAL）。  
3. 角色矩阵落入 permission-matrix / 路由 / 分配规则；管理 UI 可配置与分阶段决策。  
4. 保留 `SINGLE_TRACK` 兼容既有一级审批；CE-K08 元数据仍可选；CE-G01 同人阻断仍生效。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave E Done；不激活 E4–E7 / #119。  

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（SoT + PD-8 范围确认 + 保守银行级默认 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **E3-C1** | **范围 = 模板生命周期审批轨。** 母版审核、内容模块独立审批**保持单级**；本叶不为其引入 LEGAL→COMPLIANCE。 | F26「lifecycle/approval model」；lifecycle-review 分轨 |
| **E3-C2** | **矩阵模式（包级）：** `approvalMatrixMode` ∈ {`SINGLE_TRACK`, `LEGAL_THEN_COMPLIANCE`}。挂在**模板包**（与 locale 同级可治理元数据），对该包开发线/提交审批生效。 | 「configured and enforced」；向后兼容 |
| **E3-C3** | **默认值：** 存量迁移 → `SINGLE_TRACK`。**新建**模板默认 `SINGLE_TRACK`（安全兼容）；作者/管理员显式改为 `LEGAL_THEN_COMPLIANCE` 后强制多级。 | 防大爆炸；PD-8 交付可配置矩阵 |
| **E3-C4** | **可写窗口：** 仅 `DRAFT` 或 `APPROVAL`+`PENDING_SUBMIT` 可改 mode。已进入 `PENDING_LEGAL_DECISION` / `PENDING_COMPLIANCE_DECISION` / `PENDING_DECISION` / `PENDING_RELEASE` / 已发布线 → **422**（稳定码，如 `APPROVAL_MATRIX_MODE_LOCKED`）。 | 防中途改轨 |
| **E3-C5** | **两级有序（仅多级模式）：** 阶段枚举固定为 `LEGAL` 然后 `COMPLIANCE`。v1 **不**支持第三级、并行会签、或 COMPLIANCE→LEGAL。 | PD-8「legal→compliance」 |
| **E3-C6** | **子状态（多级）：** 提交审批后 → `APPROVAL` + `PENDING_LEGAL_DECISION`；LEGAL 通过 → `PENDING_COMPLIANCE_DECISION`；COMPLIANCE 通过 → `PENDING_RELEASE`。**拒绝任一阶段** → `DRAFT`（历史记录保留；再发须重测+重审全链，对齐 ADR-0021）。 | 扩展 domain §4.1；可测 |
| **E3-C7** | **`SINGLE_TRACK` 子状态不变：** 提交后仍 `PENDING_DECISION`；`TEMPLATE_APPROVER`（+管理员）一次判定 → `PENDING_RELEASE`。`LEGAL_REVIEWER` **不得**在单级模式执行该判定（403）。 | ADR-0021 兼容 |
| **E3-C8** | **强制 legal-reviewer：** 多级模式下，LEGAL 阶段正常判定角色 = **`LEGAL_REVIEWER`**。纯 `TEMPLATE_APPROVER`（无 `LEGAL_REVIEWER`、非管理员路径）调用 LEGAL 判定 → **403**（如 `APPROVAL_STAGE_ROLE_FORBIDDEN`）。不得提供「跳过 LEGAL」API/UI。 | PD-8「forced legal-reviewer」 |
| **E3-C9** | **COMPLIANCE 角色：** 多级模式下 COMPLIANCE 正常判定 = `TEMPLATE_APPROVER`（叙事：合规/业务审批；角色标识不变以降低迁移成本）。纯 `LEGAL_REVIEWER`（无 `TEMPLATE_APPROVER`、非管理员）调用 COMPLIANCE 判定 → **403**。 | 最小角色膨胀；矩阵可文档化 |
| **E3-C10** | **新角色与能力：** 角色目录 7→**8**：新增 `LEGAL_REVIEWER`。新 capability `decideLegalApprovals` = {`GLOBAL_ADMIN`,`GROUP_ADMIN`,`LEGAL_REVIEWER`}。`decideApprovals` 仍 = {`GLOBAL`,`GROUP`,`TEMPLATE_APPROVER`}（COMPLIANCE / 单级）。 | permission-matrix SoT |
| **E3-C11** | **角色分配：** `GROUP_ADMIN` 可分配运营类角色集合扩展含 `LEGAL_REVIEWER`；仍不得分配 `GLOBAL_ADMIN`/`AUDIT_ADMIN`/`GROUP_ADMIN`。种子用户/测试夹具须覆盖 `LEGAL_REVIEWER`。 | 防提权惯例 |
| **E3-C12** | **管理员路径：** `GLOBAL_ADMIN`/`GROUP_ADMIN` 可对两阶段做**正常**判定（组范围），**不**要求兼 `LEGAL_REVIEWER`/`TEMPLATE_APPROVER`。同人阻断与例外干预（CE-G01）仍适用。 | 与今日管理员可批一致 |
| **E3-C13** | **同人阻断：** 每一阶段的 APPROVE/REJECT 均执行 CE-G01（决策人 ≠ 最近一次 `SUBMIT_FOR_APPROVAL` 提交人；管理员例外路径不变）。LEGAL 通过不重置「提交人」——COMPLIANCE 阶段比对的仍是该次提交审批的 submitter。 | CE-G01；银行内控 |
| **E3-C14** | **结构化意见：** 两阶段均复用 ADR-0021 受控审批表单（通过：理由摘要+证据确认；拒绝：原因分类+影响+整改）。保真摘要已查看门禁（CDP fidelity）在**每一阶段 Approve**前仍强制。 | 既有审批 UX |
| **E3-C15** | **协作待办：** 提交多级 → 创建 **LEGAL** 角色队列待办；LEGAL 通过 → resolve LEGAL 待办并创建 **APPROVAL/COMPLIANCE** 待办；COMPLIANCE 通过 → resolve 并创建 pending-release 待办（既有）。拒绝 → remediation 待办给提交/编排侧。超时升级规则沿用 ADR-0021（不自动判定）。 | 站内待办 only |
| **E3-C16** | **Dashboard / 路由：** 新增行为入口「待我法务审阅」（LEGAL 队列）对 `decideLegalApprovals` 可见；既有「待我审批」仅展示 COMPLIANCE/`PENDING_DECISION`（单级）项。深链落入对应 `approvalTab`/决策面。 | P21 旅程模式 |
| **E3-C17** | **管理 UI（必交付）：** (1) 模板创建/详情可配置 `approvalMatrixMode`；(2) Stepper/状态区展示当前阶段（Legal / Compliance）；(3) LEGAL vs COMPLIANCE 决策控件按角色+子状态显隐；(4) 错角色不展示可点 Approve 或点击后 fail-closed。Bank OA + English-first i18n。 | owners 含 FE；`frontend_ui_in_scope=true` |
| **E3-C18** | **管理 API：** detail/summary 回显 `approvalMatrixMode` + `approvalSubState`（含新子状态）+ 可选 `approvalStage`（`LEGAL`\|`COMPLIANCE`\|null）。判定 endpoint 须带阶段或由服务端按子状态推导；错阶段 → **409/422**（稳定码，如 `APPROVAL_STAGE_MISMATCH`）。OpenAPI 同步。 | API+UI 同契约 |
| **E3-C19** | **审计：** 每阶段决策写生命周期审计（含 `approvalStage`、结果、非敏感理由摘要、例外标记）。禁止 variables/客户明文。 | 可审计验收 |
| **E3-C20** | **CE-K08 正交：** 本叶**不**将 `jurisdiction`/`legalReviewRef`/`effective*` 改为必填；不新增「无 legalReviewRef 不得进 LEGAL」门禁。 | F26「legal metadata optional」诚实保留 |
| **E3-C21** | **代理/自动/并行：** 仍禁止代理审批、自动通过/拒绝、邮件外发（ADR-0021）。超时仅升级可见性。 | ADR-0021 |
| **E3-C22** | **SPECIMEN / PD-6 / PD-7 / Word：** 不改变再生水印；不嵌入授权字体；不发明 #119 Word 证据。 | 程序 §8 / 队列 veto |
| **E3-C23** | **ADR：** doc-keeper 以本文件 E3-C* 为 Decision 接受 **ADR-0064**（建议路径 `docs/adr/template-lifecycle/0064-legal-compliance-approval-matrix.md`）。ADR-0021 **Amendment**：一级审批仍为 `SINGLE_TRACK` 默认；`LEGAL_THEN_COMPLIANCE` 为经 PD-8 确认的可选矩阵。Accepted ≠ E3 impl Done。 | 程序 IBL-E3 验收 |
| **E3-C24** | **门禁：** BE `mvn verify`；FE lint/type-check/test/build；用户面 Playwright functional + UIUX；queued Docker deploy evidence。 | delivery constitution |
| **E3-C25** | **完成边界：** E3 Done ≠ Wave E Done；≠ go-live；#3b/#5a 保持 CONDITIONAL；E4–E7 / #119 不激活。 | 队列政策 |

### 4.2 明确非本叶确认（禁止当作已定产品事实）

| 项 | 状态 |
| --- | --- |
| 法人文档品牌变体 | **IBL-E4** |
| `effectiveFrom` 硬阻断 / bulk re-pin | **IBL-E5** |
| 条款嵌套图治理 | **IBL-E6** |
| RTL / SPECIMEN 移除 / 授权字体 / Word | E7 / PD-6/7 / #119 — **OUT** |
| 强制填写 CE-K08 法务元数据 | **拒绝**（E3-C20） |
| 三级及以上审批 / 会签 / 外部工作流引擎 | **拒绝本叶** |
| 将 `LEGAL_REVIEWER` 并入 `TEMPLATE_APPROVER` 而不新增角色 | **拒绝**（与「forced legal-reviewer」冲突） |
| 母版/条款多级审批 | **拒绝本叶**（E3-C1） |

### 4.3 ADR / 用户确认

| 问题 | 结论 |
| --- | --- |
| 是否还需用户再确认「要不要做多级法务审批 + legal-reviewer」？ | **否** — PD-8 已确认 2026-07-19 |
| 是否还需用户再确认 E3-C1…C25 默认？ | **否（BDD ready）** — 由 ADR-0021 一级基线 + PD-8 矩阵边界 + 银行分责/fail-closed/可审计要求隐含；记入本 BDD；ADR-0064 **Accepted** |
| ADR 文件状态何时 Accepted？ | **Accepted（2026-07-20）** — [ADR-0064](../adr/template-lifecycle/0064-legal-compliance-approval-matrix.md)；Decision = E3-C\*；Accepted ≠ E3 impl Done |

---

## 5. Preconditions

- 操作者具备对应组范围模板访问与目标动作权限。  
- PD-8 已 Confirmed；#130 为本交付叶（orchestrator 已激活本切片）。  
- 既有测试→提交审批→（一级）审批→待发布→发布链路可用。  
- CE-G01 同人阻断与保真已查看门禁可用。  
- CE-K08 / E1 / E2 行为保持可用且不被本叶破坏。

---

## 6. Trigger

- 作者/管理员设置或变更 `approvalMatrixMode`。  
- 测试通过后提交审批（`SUBMIT_FOR_APPROVAL`）。  
- `LEGAL_REVIEWER`（或管理员）对 `PENDING_LEGAL_DECISION` 做通过/拒绝。  
- `TEMPLATE_APPROVER`（或管理员）对 `PENDING_COMPLIANCE_DECISION`（或单级 `PENDING_DECISION`）做通过/拒绝。  
- 用户打开 Dashboard LEGAL/APPROVAL 队列或模板审批工作区。

---

## 7. Primary journey

1. 作者将模板包 `approvalMatrixMode` 设为 `LEGAL_THEN_COMPLIANCE` 并完成编排与测试通过。  
2. 作者提交审批 → 状态 `APPROVAL` + `PENDING_LEGAL_DECISION`；LEGAL 队列出现待办；COMPLIANCE 队列**无**可决策项。  
3. `LEGAL_REVIEWER` 打开模板 → 确认证据与保真摘要 → Approve（结构化理由）→ 进入 `PENDING_COMPLIANCE_DECISION`；LEGAL 待办关闭；COMPLIANCE 待办创建。  
4. `TEMPLATE_APPROVER` Approve → `PENDING_RELEASE`；此后发布路径与 ADR-0021 相同（摘要+二次确认+门禁）。  
5. 若任一层 Reject → 回 `DRAFT` + remediation；再次发布候选须重新测试与完整两级审批。  
6. 对照：另一模板保持 `SINGLE_TRACK` → 提交后仅 `PENDING_DECISION`，由 `TEMPLATE_APPROVER` 一次通过。

---

## 8. System responses（成功路径）

- Mode 持久化并在 detail/UI 回显。  
- 多级提交后子状态与待办分轨正确。  
- 合法角色+阶段的 Approve/Reject 迁移状态并写审计。  
- 单级模式行为与升级前一致（回归）。  
- UI 仅向授权角色展示对应阶段主 CTA。

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-E3-001 — Default and migrate SINGLE_TRACK

**Given** 存量模板（迁移前无 mode 字段）或新建模板未显式设置  
**When** 读取 `approvalMatrixMode`  
**Then** 值为 `SINGLE_TRACK`；提交审批后子状态为 `PENDING_DECISION`（非 LEGAL/COMPLIANCE 子状态）

### BDD-IBL-E3-002 — Configure LEGAL_THEN_COMPLIANCE on draft

**Given** 授权编排人员；模板处于 `DRAFT`  
**When** 更新 `approvalMatrixMode=LEGAL_THEN_COMPLIANCE`  
**Then** `200`；detail/UI 回显该 mode

### BDD-IBL-E3-003 — Mode locked after legal stage entered

**Given** 多级模板已 `PENDING_LEGAL_DECISION`  
**When** 尝试改为 `SINGLE_TRACK`（或改回）  
**Then** `422` `APPROVAL_MATRIX_MODE_LOCKED`（或等价）；mode 不变

### BDD-IBL-E3-004 — Submit enters PENDING_LEGAL_DECISION

**Given** 多级模板处于测试通过（`APPROVAL`+`PENDING_SUBMIT`）；提交门禁通过  
**When** `SUBMIT_FOR_APPROVAL`  
**Then** `approvalSubState=PENDING_LEGAL_DECISION`；创建 LEGAL 队列待办；**不**直接 `PENDING_RELEASE`；**不**出现可被合规角色立即关闭的「已待合规决策」误状态

### BDD-IBL-E3-005 — Legal approve advances to compliance

**Given** `PENDING_LEGAL_DECISION`；用户为组内 `LEGAL_REVIEWER`（非提交人）；保真已确认  
**When** LEGAL 阶段 Approve（结构化表单合法）  
**Then** `approvalSubState=PENDING_COMPLIANCE_DECISION`；LEGAL 待办 resolved；COMPLIANCE/APPROVAL 待办 OPEN；审计含 `approvalStage=LEGAL` + APPROVED

### BDD-IBL-E3-006 — Compliance approve reaches pending release

**Given** `PENDING_COMPLIANCE_DECISION`；用户为组内 `TEMPLATE_APPROVER`（非提交人）；保真已确认  
**When** COMPLIANCE 阶段 Approve  
**Then** 进入 `PENDING_RELEASE`；COMPLIANCE 待办 resolved；发布待办按既有规则创建；审计含 `approvalStage=COMPLIANCE`

### BDD-IBL-E3-007 — Legal reject returns to draft

**Given** `PENDING_LEGAL_DECISION`  
**When** LEGAL 阶段 Reject（原因分类等必填满足）  
**Then** 模板 `DRAFT`；LEGAL 待办关闭；创建 remediation；**不**进入 COMPLIANCE；原审批记录可查

### BDD-IBL-E3-008 — Compliance reject returns to draft

**Given** `PENDING_COMPLIANCE_DECISION`  
**When** COMPLIANCE Reject  
**Then** 模板 `DRAFT`；须重新测试+重新两级审批方可再待发布

### BDD-IBL-E3-009 — Template approver cannot decide LEGAL

**Given** `PENDING_LEGAL_DECISION`；用户仅 `TEMPLATE_APPROVER`（无 `LEGAL_REVIEWER`、非管理员）  
**When** 调用 LEGAL Approve/Reject API 或 UI 主 CTA  
**Then** **403** `APPROVAL_STAGE_ROLE_FORBIDDEN`（或等价）；状态不变

### BDD-IBL-E3-010 — Legal reviewer cannot decide COMPLIANCE

**Given** `PENDING_COMPLIANCE_DECISION`；用户仅 `LEGAL_REVIEWER`  
**When** 调用 COMPLIANCE Approve/Reject  
**Then** **403**；状态不变

### BDD-IBL-E3-011 — Wrong stage mismatch fail-closed

**Given** `PENDING_LEGAL_DECISION`  
**When** 请求体声称 `approvalStage=COMPLIANCE`（或命中合规 endpoint）  
**Then** **409/422** `APPROVAL_STAGE_MISMATCH`（或等价）；状态不变

### BDD-IBL-E3-012 — Self-approval blocked on each stage

**Given** 多级模板；提交人 username = `alice`；`alice` 兼 `LEGAL_REVIEWER`  
**When** `alice` 尝试 LEGAL Approve（无例外干预字段）  
**Then** **403** `SELF_APPROVAL_FORBIDDEN`；COMPLIANCE 阶段对提交人同样阻断（CE-G01）

### BDD-IBL-E3-013 — SINGLE_TRACK regression

**Given** `approvalMatrixMode=SINGLE_TRACK`；测试通过后提交  
**When** `TEMPLATE_APPROVER` Approve（保真已确认）  
**Then** 进入 `PENDING_RELEASE`；行为与 ADR-0021 一级审批一致；`LEGAL_REVIEWER`-only 用户 **403**

### BDD-IBL-E3-014 — Permission matrix documents LEGAL_REVIEWER

**Given** 本叶文档落地后  
**When** 阅读 permission-matrix 角色清单、capability 表、§13 路由/行为入口、分组可分配角色列表  
**Then** 含 `LEGAL_REVIEWER`、`decideLegalApprovals`、LEGAL 队列入口，及 COMPLIANCE vs LEGAL 分责说明；角色目录为 **8**（非仍写死 7）

### BDD-IBL-E3-015 — Management UI mode + stage visibility

**Given** 授权作者打开多级模板详情/创建高级区  
**When** 选择 `LEGAL_THEN_COMPLIANCE` 并保存；模板进入 `PENDING_LEGAL_DECISION`  
**Then** UI 展示当前阶段为 Legal（English-first）；合规 Approve 主 CTA **不可用**；法务角色可见 Legal Approve/Reject

### BDD-IBL-E3-016 — Dashboard LEGAL queue deep link

**Given** `LEGAL_REVIEWER` 会话；存在 OPEN LEGAL 待办  
**When** 打开「待我法务审阅」/ LEGAL 队列入口  
**Then** 列表含该模板；深链进入 LEGAL 决策面（非误入仅合规面）；无 `decideLegalApprovals` 的角色不可见该入口

### BDD-IBL-E3-017 — No skip legal stage

**Given** 多级模板 `PENDING_LEGAL_DECISION`  
**When** 任何客户端尝试直接迁移到 `PENDING_RELEASE` 或「一键双批」  
**Then** 被拒绝（4xx）；状态仍为 `PENDING_LEGAL_DECISION` 直至合法 LEGAL 判定

### BDD-IBL-E3-018 — SPECIMEN / PD-6 / orthogonality / non-activation

**Given** CE-G06 regenerate、CE-K08 可选字段、E1 locale、E2 inclusion 已存在  
**When** E3 变更合并后执行 regenerate / 发布含可选空法务元数据的 CM 引用 / 查阅计划状态  
**Then** 成功样件仍含 SPECIMEN；CE-K08 字段仍可空；E1/E2 行为不被削弱；**不**激活 E4–E7；**不**翻转 #3b/#5a；**不**发明 #119 Word 证据

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 未认证 / 无组权限 | 401/403/404 惯例 |
| 错角色阶段判定 | 403 stage-role |
| 错阶段 payload | 409/422 mismatch |
| 同人无例外 | 403 SELF_APPROVAL_FORBIDDEN |
| 保真未查看即 Approve | 既有 fidelity fail-closed |
| 中途改 mode | 422 locked |
| 单级模式 LEGAL_REVIEWER 代批 | 403 |
| 超时升级 | 仅待办/指示；不改状态 |
| 代理审批 / 自动通过 | 不提供 |
| 条款/母版多级 | OUT |
| 强制 CE-K08 必填 | OUT |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | mode 字段；子状态；阶段判定 403/422；审计 `approvalStage` |
| DB | mode 列/枚举 + 迁移默认 `SINGLE_TRACK`；角色种子 |
| UI / E2E | 模式配置、阶段指示、分轨队列、分角色决策（Playwright + UIUX） |
| Docs | 本 BDD；ADR-0064（Accept）；permission-matrix 8 角色；ADR-0021 Amendment 指针 |
| Gates | `mvn verify`；FE 四门；E2E+UIUX；deploy queue |
| 负向 | 无 SPECIMEN 移除；无 #3b/#5a 翻转；无 E4–E7 / #119 |

---

## 12. Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#130** | IBL-E3 delivery leaf |
| IBL program **F26** / **PD-8** / §7 IBL-E3 acceptance | 范围 + Done 草案 |
| ADR-0064 **Accepted** | 架构 Decision（= E3-C*；≠ impl Done） |
| ADR-0021 | 一级基线；Amendment 承认可配置多级 |
| permission-matrix | 角色/capability/路由 SoT（doc-keeper 同步） |
| domain-model §4 / lifecycle-review | 状态与审批规则指针 |
| CE-G01 / CDP fidelity / approver journey | 同人阻断 + 保真 + UI 旅程扩展 |
| CE-K08 | 可选法务元数据正交 |
| IBL-E1 / E2 | 正交；不本叶激活后续叶 |

---

## 13. Implementation notes（非产品发明；供 TDD）

- 错误码 / 子状态枚举名以实现为准，须稳定并写入 OpenAPI/`messageKey`/i18n。  
- `approvalStage` 可显式入参或由 `approvalSubState` 唯一推导——对外契约固定一种，禁止歧义双源。  
- Dashboard 队列值建议新增 `LEGAL`（或等价），与既有 `APPROVAL` 并列；深链参数写入前端路由约定并测回归。  
- `ManagementRole` / seed SQL / JWT claims / `RouteVisibilityService` 同步第 8 角色。  
- FE 遵循 `.cursor/skills/frontend-oa-design` 与 `i18n-english-first`；扩展 Stepper（CE-U15）映射新子状态。  
- doc-keeper stage 3（本步）：ADR-0064 **Accepted**；permission-matrix / domain §4.1 / ADR-0021 Amendment / PRD / lifecycle-review / OpenAPI+contract stubs / docs 索引已同步；**不**宣称 impl Done。

---

## 14. Handoff

```text
bdd_readiness: ready
frontend_ui_in_scope: true
scenario_count: 18
scenario_ids:
  - BDD-IBL-E3-001
  - BDD-IBL-E3-002
  - BDD-IBL-E3-003
  - BDD-IBL-E3-004
  - BDD-IBL-E3-005
  - BDD-IBL-E3-006
  - BDD-IBL-E3-007
  - BDD-IBL-E3-008
  - BDD-IBL-E3-009
  - BDD-IBL-E3-010
  - BDD-IBL-E3-011
  - BDD-IBL-E3-012
  - BDD-IBL-E3-013
  - BDD-IBL-E3-014
  - BDD-IBL-E3-015
  - BDD-IBL-E3-016
  - BDD-IBL-E3-017
  - BDD-IBL-E3-018
open_questions: []
suggested_adr_number: "0064"
suggested_adr_path: docs/adr/template-lifecycle/0064-legal-compliance-approval-matrix.md
adr_status: Accepted (2026-07-20) — docs/adr/template-lifecycle/0064-legal-compliance-approval-matrix.md; Decision = E3-C*; Accepted ≠ impl Done
recommended_next_stage: backend-engineer + frontend-engineer (TDD; FE E2E mandatory; frontend_ui_in_scope=true)
```
