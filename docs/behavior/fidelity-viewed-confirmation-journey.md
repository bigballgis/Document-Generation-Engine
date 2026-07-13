# BDD：保真摘要「已查看」确认（Pass / Approve / Publish）

**文件状态:** `ready`  
**BDD ID 前缀:** `BDD-CDP-FID`  
**CDP:** CD-BDD-T02/T03/T04 交叉 → **CD-E2E-T10**  
**编写日期:** 2026-07-11（CD-E2E-T10 readiness；补全 fail-closed 场景）  
**程序:** [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md)  
**计划任务:** [CDP-e2e-full-chain-evidence.md](../plan/detail/CDP-e2e-full-chain-evidence.md) § CD-E2E-T10

---

## 1. 概述

在浏览器内证明：测试通过、审批通过、发布（go-live）三条正向生命周期动作 **在未确认保真警告摘要「已查看」时不得完成**；确认后（checkbox 或等价控件）方可提交。

产品权威来自 domain / PRD / requirements 已确认基线：

> Template Test Record、Approval Summary 和 Release Summary **必须记录**保真警告摘要已查看确认；该确认不要求逐条解决警告，警告数量不自动阻断发布。

本文件是 CD-2 **浏览器证据** 规格，聚焦 **fail-closed 门禁**，不重开 P19 保真校验全矩阵，也不要求逐条 warning「已解决」。

**与既有旅程关系：**

| 既有文件 | 覆盖 | 本文件补充 |
| --- | --- | --- |
| [tester-decision-journey.md](./tester-decision-journey.md) | Pass/Fail happy path（含「确认保真摘要」） | **未勾选则不可 Pass** |
| [approver-decision-journey.md](./approver-decision-journey.md) | Approve/Reject happy path | **未确认保真则不可 Approve** |
| [team-lead-publish-journey.md](./team-lead-publish-journey.md) | Go-live + 二次确认 | **未确认保真则不可 Publish** |

---

## 2. Actor / Role

| Actor | 角色 / 账户 | 权限 |
| --- | --- | --- |
| **TEMPLATE_TESTER** | 种子测试员（`10000005`） | Pass test 对话框；勾选保真「已查看」 |
| **TEMPLATE_APPROVER** | 种子审批员（`10000006`） | Approve 对话框；确认保真摘要（专用勾选或等价） |
| **GROUP_ADMIN**（或具备发布权限角色） | 种子组管（`10000002`） | Publish / go-live 摘要；保真确认门禁 |

> E2E 可拆为 1–3 个 spec；每个场景须真实登录对应角色，不得仅 API 断言。

---

## 3. Goal（用户目标）

**作为** 测试员 / 审批员 / 发布负责人  
**我希望** 在提交 Pass / Approve / Publish 前必须显式确认已审阅保真警告摘要  
**以便** 生命周期推进有可审计的「已查看」证据，且未确认时系统 fail-closed。

---

## 4. Trigger（触发条件）

- Docker 验收栈健康（UI `:4173`，backend `:8080/healthz`）
- 用户打开模板详情，进入对应生命周期动作：
  - **Pass test** — Testing / 测试判定对话框
  - **Approve** — Approval 判定对话框
  - **Publish** — 发布摘要 / go-live 对话框

---

## 5. Preconditions（前置条件）

1. 用户已真实登录管理 UI（非角色模拟）。
2. 模板处于可执行该动作的状态（TEST 可 Pass；APPROVAL 可 Approve；PENDING_RELEASE 可 Publish）。
3. 判定/发布对话框可打开，且展示保真摘要确认控件（或发布门禁中含保真已查看项）。
4. Setup 可用 API helper 将模板推到目标阶段；**测试本体内**须经 UI 打开对话框并尝试提交（不得仅 API 断言）。
5. 其他必填项可按场景填好（如 Approve 理由、覆盖率/预览勾选），以便隔离「仅缺保真确认」这一变量。

---

## 6. Primary Journey（主路径 — fail-closed 证明）

| # | Actor | UI 动作 | 系统响应 |
| --- | --- | --- | --- |
| 1 | TESTER | 打开 Pass test 对话框；**不**勾选保真「已查看」；尝试 Submit | 提交不可用或被拒；模板仍在 TEST；无 PASSED 决策 |
| 2 | TESTER | 勾选保真「已查看」（及同表单其他必填确认）→ Submit | Pass 成功（可与 T02 证据互补；本切片以阻断为主） |
| 3 | APPROVER | 打开 Approve 对话框；**不**确认保真摘要；尝试 Submit | 提交不可用或被拒；模板仍在 APPROVAL |
| 4 | APPROVER | 确认保真摘要（专用勾选或等价）→ Submit | Approve 成功（可与 T04 互补） |
| 5 | GROUP_ADMIN | 打开发布摘要；保真已查看未满足时尝试 Confirm | Confirm 不可用或被拒；无新 release version |
| 6 | GROUP_ADMIN | 保真已查看满足后 Confirm | Publish 成功（可与 T05 互补） |

---

## 7. System Responses（成功与 fail-closed）

对齐 P19 结构化判定表单 + domain「Test / Approval / Release Summary 必须记录保真已查看」：

- **Pass test：** 正向提交要求 `fidelityViewedConfirmed === true`（UI checkbox + 后端校验）。未确认 → 不发出成功决策 / 按钮 disabled / 校验错误（`decisionFidelityConfirmationRequired` 或等价）。
- **Approve：** Approval Summary 必须记录保真警告摘要已查看确认。UI 须提供 **保真专用确认**（推荐与 Pass 相同的 `fidelityViewedConfirmed` checkbox）或 **明确覆盖保真摘要的等价控件**；未确认 → fail-closed。
- **Publish：** Release Summary 必须记录保真警告摘要已查看确认。可通过发布摘要中的显式勾选、或发布门禁 checklist 项（依赖上游已记录的确认）实现；未满足 → Confirm disabled / 拒绝发布。
- 确认 **不要求** 逐条解决警告；警告数量本身不自动阻断（阻断项另论）。
- 不展示模板变量原值、客户数据、完整请求体或完整生成正文。

---

## 8. Acceptance Scenarios（Given / When / Then）

### BDD-CDP-FID-001 — Pass test 未确认保真则不可通过

- **Given** Docker 栈就绪，TEMPLATE_TESTER 已登录，模板处于可 Pass 的 TEST 状态，Pass test 对话框可打开  
- **When** 用户打开 Pass test 对话框，填写/勾选除保真「已查看」外的其他必填项（若有），**保持保真确认未勾选**，并尝试提交  
- **Then** 提交不成功（主按钮 disabled，或校验错误阻止提交）  
- **And** 模板生命周期状态不变（仍可被 Pass；未进入可提交审批态）  
- **And** 可观测证据：对话框内保真确认控件可见（如 `confirmFidelityViewed` 文案 / checkbox）；无成功 toast；可选 UIUX 截图写入 `frontend/e2e/evidence/CDP-E2E-T10-uiux-manifest.md`

> 追溯：P19-T07 `testPass_requiresFidelityViewedConfirmation`；domain Test Record 保真已查看。

---

### BDD-CDP-FID-002 — Approve 未确认保真则不可批准

- **Given** Docker 栈就绪，TEMPLATE_APPROVER 已登录，模板处于可 Approve 的 APPROVAL 状态，Approve 对话框可打开  
- **When** 用户打开 Approve 对话框，填写理由等其他必填项，**不确认保真警告摘要已查看**，并尝试提交  
- **Then** 提交不成功（主按钮 disabled，或校验错误阻止提交）  
- **And** 模板仍停留在 APPROVAL（未进入待发布）  
- **And** 可观测证据：对话框内存在保真摘要确认控件（专用 checkbox，或文案/范围明确包含 fidelity warning summary 的等价确认）；UIUX 截图可登记

> 追溯：domain Approval Summary 必须记录保真已查看；CD-BDD-T03；plan T10 acceptance。  
> **产品落点：** 若当前 Approve 仅有「关键证据」勾选且文案/字段未覆盖保真摘要、且未持久化保真已查看，则本场景驱动 **产品补齐**（非仅 E2E 文案断言）。

---

### BDD-CDP-FID-003 — Publish 未确认保真则不可发布

- **Given** Docker 栈就绪，具备发布权限的用户已登录，模板处于 PENDING_RELEASE（或等价可发布态），发布摘要对话框可打开  
- **When** 用户打开发布摘要 / go-live 对话框，在保真警告摘要「已查看」确认未满足时（未勾选，或门禁项未 ready）尝试 Confirm  
- **Then** 发布不成功（Confirm disabled，或请求被拒）  
- **And** 无新的可调用 release version  
- **And** 可观测证据：发布摘要或门禁清单中可见保真相关确认/就绪态；UIUX 截图可登记

> 追溯：domain Release Summary 必须记录保真已查看；PRD 发布门禁「保真警告按已确认规则完成摘要查看确认」；CD-BDD-T04 / T05。  
> **产品落点：** 若发布路径仅依赖二次确认、未校验/展示保真已查看，则本场景驱动 **产品补齐或门禁接线**。

---

### BDD-CDP-FID-004 — 确认保真后正向动作可完成（冒烟）

- **Given** 同 FID-001/002/003 各自前置，且用户已勾选/满足保真「已查看」（及其他该动作必填项）  
- **When** 用户提交 Pass / Approve / Publish（可分三次会话或一条多角色链路）  
- **Then** 对应动作成功；状态按生命周期前进  
- **And** 决策或发布摘要侧可追溯保真已查看确认（UI 成功反馈即可；审计字段细节属 T11）

> 说明：完整 happy path 已由 CD-E2E-T02/T04/T05 覆盖；本场景为 T10 阻断证明后的最小正向冒烟，允许与既有 spec 复用 helper，不强制重复全旅程截图矩阵。

---

## 9. Boundary / Exception（本切片范围）

| 情况 | 期望 | 归属 |
| --- | --- | --- |
| Fail test / Reject | 不要求保真「已查看」勾选 | Out of scope（T02/T04 负向） |
| 逐条 warning「已解决」 | 不要求 | Out of scope（domain：确认 ≠ 解决） |
| 警告数量阈值自动阻断 | 不要求 | Out of scope |
| T09 对比面板 / warningCode 筛选 | 已 Done | 可复用预览材料，不扩展对比断言 |
| T08 预览下载 | 已 Done | 不重复验收下载 |
| 无权限用户 | fail-closed：不可见动作 | 沿用权限矩阵 |
| 敏感明文 | 摘要仅非敏感 | 回归：截图不得依赖变量原值 |
| GROUP_ADMIN 例外干预 | 仍须保真确认 + 例外原因/二次确认 | 可断言保真仍必填；例外全矩阵不强制 |

### CD-PIT-08（相关边界）

| 要点 | 本切片要求 |
| --- | --- |
| **Preview ≠ published legal authority** | 保真确认针对测试/审批/发布材料中的保真**警告摘要**，不把 edit-preview 当作法律终态 |
| **Mitigation 落点** | Pass/Approve/Publish 强制「已查看」确认；与 T08 final-path 下载互补 |

---

## 10. Observable Evidence

| 类型 | 证据 |
| --- | --- |
| UI | Pass/Approve/Publish 对话框内保真确认控件或门禁项 |
| Browser | Playwright：未确认时无法提交；确认后可提交（FID-004 冒烟） |
| Screenshot | CDP-E2E-T10 UIUX manifest（建议 REDBC @1920；至少 1 张阻断态 + 可选确认态） |
| Traceability | Plan T10 acceptance；ledger 由实现后 doc-sync 更新 |

**预期交付物（实现阶段，非本 BDD 写入）：**

- `frontend/e2e/CDP-E2E-T10-fidelity-viewed.spec.ts`（功能；可拆分角色）
- `frontend/e2e/CDP-E2E-T10-uiux-evidence.spec.ts` + `frontend/e2e/evidence/CDP-E2E-T10-uiux-manifest.md`
- 复用 T02/T04/T05 / CDP golden helpers 预置生命周期阶段

---

## 11. Product vs E2E expectation

| 路径 | 期望 | 说明 |
| --- | --- | --- |
| **Pass test** | **默认 E2E + UIUX** | 产品已有 `fidelityViewedConfirmed` checkbox + `DecisionFormService.validateTestDecision`；本切片补浏览器证据 |
| **Approve** | **E2E；若缺保真专用确认则产品补齐** | domain 要求 Approval Summary 记录保真已查看；若 UI/API 仅有 `keyEvidenceConfirmed` 且未覆盖保真字段，escalate 为同切片缺陷修复 |
| **Publish** | **E2E；若缺保真已查看门禁则产品补齐** | domain 要求 Release Summary 记录保真已查看；若摘要/门禁未强制，escalate 为同切片缺陷修复 |
| **种子/数据** | Setup helper 允许 | 属实现细节，不阻塞 BDD |

---

## 12. Out of scope（显式）

- CD-E2E-T09 对比面板重测、T11 audit 导出、T12 zh-CN dual-brand、T13 package materialize
- 逐条 fidelity warning 已解决工作流
- 重开 P19 `FidelityValidationService` 全部分支单测矩阵
- 新建独立保真阅读器（沿用现有摘要条 + 列表）

---

## 13. Traceability

| 来源 | 关系 |
| --- | --- |
| CD-E2E-T10 plan acceptance | Cannot pass / approve / publish without acknowledging fidelity summary |
| `docs/domain/domain-model.md` §2.9.x | Test / Approval / Release Summary 必须记录保真已查看 |
| `docs/product/PRD.md` / requirements | 发布门禁：保真警告摘要查看确认 |
| P19-T07 | `testPass_requiresFidelityViewedConfirmation` |
| CD-BDD-T02 / T03 / T04 | 既有 happy path；本文件补 fail-closed |
| CD-PIT-08 | 相关检测波次（与 T08 互补） |

---

## 14. BDD 就绪声明

**bdd_readiness: ready**

- 场景 ID：`BDD-CDP-FID-001` … `BDD-CDP-FID-004` 可直接驱动 Playwright Red
- 已持久化本文件；`docs/README.md` behavior 索引已登记（ready）
- 无阻塞性 open questions（Approve/Publish 产品落点若缺口 → 实现阶段 escalate，行为本身已由 domain 确认）
