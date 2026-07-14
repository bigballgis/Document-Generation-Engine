# BDD 行为规格：CE-G01 同人审批阻断（self-approval block）

**文件状态**: `ready`
**版本**: 1.0.0
**编写日期**: 2026-07-14
**BDD ID 前缀**: `BDD-CE-G01`
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §6 Wave CE-G · CE-G01；§10 D1 拍板记录
**Slice**: `ce-g01-self-approval-block`
**Worktree**: `D:/working/DGE-ce-g01-self-approval-block` · `feat/ce-g01-self-approval-block`
**Task Master**: #72
**授权依据**: 用户 2026-07-14 确认 ADR 决策 **D1** —— 不做四眼双人复核；**仅做同人审批阻断**（CE-G01）；四眼待内控要求出现再修订 ADR-0021
**完成声明约束**: 本切片关闭内控硬控制缺口 CE-G01；**不**实现四眼发布复核；**不**修订 ADR-0021；**不**实现 CE-G02 水印 / CE-G03 PII / CE-G04 legal hold / CE-G06 审计复现；**不**宣称 go-live

---

## 1. 概述

模板 / 母版 / 条款三处生命周决策服务统一引入**同人审批阻断**：当决策执行人（`decisionActor`）与最近一次提交人（`lastSubmitActor`）为**同一自然人**时，决策被 fail-closed 拒绝；仅 `GROUP_ADMIN`（含 `GLOBAL_ADMIN` 兜底见 §4.4）可通过**显式例外干预路径**绕过，但必须强制提供 `exceptionReason` + 二次确认，且例外事实与原因进入审计永久留存。

| 行为域 | 摘要 |
| --- | --- |
| **模板审批阻断** | `TemplateLifecycleApprovalFlowSupport.recordApprovalDecision` 在 `APPROVED` / `REJECTED` 决策前校验 `decisionActor != lastSubmitActor`，否则 `api.error.lifecycle.selfApprovalForbidden` |
| **母版审批阻断** | `MasterDocumentReviewSupport.decideReview` 在 `APPROVED` / `REJECTED` 决策前同一校验 |
| **条款审批阻断** | `ContentModuleReviewService.transition` 在 `APPROVE_REVIEW` / `REJECT_REVIEW` 前同一校验 |
| **GROUP_ADMIN 例外** | 复用既有 `exceptionIntervention` 机制（模板已存在）；母版/条款 DTO 新增对等字段；强制 `exceptionReason` + `secondaryConfirmed=true`；审计保留例外标记与原因 |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| 模板决策无同人校验 | `TemplateLifecycleApprovalFlowSupport.recordApprovalDecision` 直走 `decisionFormService.validateApprovalDecision` → `transitions.transition`，无 `lastSubmitActor` 比对 |
| 母版决策无同人校验 | `MasterDocumentReviewSupport.decideReview` 仅 `canReviewMasters` 角色检查，无 submitter 比对；`MasterReviewRecordEntity` 已存 `SUBMITTED` 行的 `actorUsername` 可作为 lastSubmitActor 来源 |
| 条款决策无同人校验 | `ContentModuleReviewService.transition` 的 `APPROVE_REVIEW` / `REJECT_REVIEW` 仅角色 + 状态校验，无 submitter 比对；`version.setUpdatedBy()` 在 submit 时写入但被后续 transition 覆盖，需新增 `submittedBy` 持久化或回查审计 |
| 既有例外干预机制（模板） | `DecisionFormService.validateExceptionIntervention`：`exceptionIntervention=true` + `GROUP_ADMIN` 角色 + 非空 `exceptionReason` + `secondaryConfirmed=true`；messageKey `api.error.template.exceptionInterventionNotAllowed` / `exceptionReasonRequired` / `exceptionSecondaryConfirmRequired` |
| 四眼复核已被 ADR 拒绝 | ADR-0021；用户 2026-07-14 D1 确认维持 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **TEMPLATE_APPROVER** | 模板审批人 | 对 `PENDING_REVIEW`/`APPROVAL` 状态模板执行 `APPROVED`/`REJECTED` 决策 |
| **MASTER_DESIGNER / 复核角色** | 母版审批人 | 持 `reviewMasters` 能力（`GLOBAL_ADMIN` / `GROUP_ADMIN`）对 `PENDING_REVIEW` 母版执行 `APPROVED`/`REJECTED` |
| **TEMPLATE_APPROVER / GROUP_ADMIN / GLOBAL_ADMIN** | 条款审批人 | 持 `canDecideContentModuleReviews` 能力对 `SUBMITTED` 条款版本执行 `APPROVE_REVIEW`/`REJECT_REVIEW` |
| **GROUP_ADMIN**（例外干预） | 分组管理员 | 当决策人 == 提交人时，唯一可发起例外干预的角色；强制 `exceptionReason` + 二次确认 |
| **GLOBAL_ADMIN** | 全局管理员 | 跨组兜底；与 `GROUP_ADMIN` 等价享有例外干预权（见 §4.4） |
| **系统** | 三处 decision service + 审计 | 同人比对；fail-closed 拒绝；例外路径校验；审计留痕 |
| **审计读者** | AUDIT_ADMIN / GLOBAL_ADMIN / GROUP_ADMIN（带 groupScope） | 可读生命周期审计行，含 `selfApprovalException=true` + `exceptionReason` |

---

## 3. Goal

1. 模板 / 母版 / 条款三处决策服务在执行 `APPROVED`/`REJECTED`（条款为 `APPROVE_REVIEW`/`REJECT_REVIEW`）前，**统一**校验决策执行人 username 与最近一次提交人 username 不相等。
2. 同人时 **fail-closed**：HTTP **403**，`error.code=SELF_APPROVAL_FORBIDDEN`，`messageKey=api.error.lifecycle.selfApprovalForbidden`，`retryable=false`；不改变任何生命周期状态、不写决策审计行（仅写失败审计/trace）。
3. `GROUP_ADMIN`（及 `GLOBAL_ADMIN`）可通过例外干预路径绕过：必须 `exceptionIntervention=true` + 非空 `exceptionReason` + `secondaryConfirmed=true`；否则按同人阻断或字段缺失失败。
4. 例外干预的成功决策必须在生命周期审计行中**永久保留** `selfApprovalException=true` 与 `exceptionReason`（既不因后续 transition 丢失，也不在摘要视图抹除）。
5. 三模块各**至少一条回归测试**断言同人被阻断 + 一条断言 GROUP_ADMIN 例外可放行且审计留痕。
6. 不引入四眼双人复核（D1）；不修订 ADR-0021。

---

## 4. 已确认决策（confirmed）

### 4.1 比对规则

| ID | 决策 | 来源 |
| --- | --- | --- |
| **CMP-1** | 比对对象为 `ManagementSessionClaims.username()`（决策执行人）与最近一次提交记录的 `actorUsername`；按字符串**精确相等**（case-sensitive、trim 后比较）判定同人 | 既有 `ManagementSessionClaims` / `TemplateLifecycleRecordEntity` / `MasterReviewRecordEntity` |
| **CMP-2** | "最近一次提交"定义：模板 = 最新一条 `LifecycleAction=SUBMIT_FOR_APPROVAL` 的 `TemplateLifecycleRecordEntity`；母版 = 最新一条 `MasterReviewAction=SUBMITTED` 的 `MasterReviewRecordEntity`；条款 = 该版本在 `SUBMIT_FOR_REVIEW` 转入 `SUBMITTED` 状态时的提交人（实现可在 `ContentModuleVersionEntity` 新增 `submittedBy` 字段或回查 `ManagementAuditRecorder` 的 `SUBMIT_FOR_REVIEW` 行，见 Q1） | 既有审计/状态机 |
| **CMP-3** | 若历史无提交记录（数据迁移缺 submitter），视为 `lastSubmitActor=null` → **不触发**同人阻断（避免误伤存量；该路径仅允许在迁移空仓场景出现，正常流程必然有 submitter） | 防回归 |
| **CMP-4** | 比对发生在**授权检查通过之后、状态机迁移之前**；失败时不修改实体、不写决策成功审计行 | fail-closed |

### 4.2 失败错误模型

| ID | 决策 |
| --- | --- |
| **ERR-1** | HTTP **403**；`error.code=SELF_APPROVAL_FORBIDDEN`；`error.category=AUTHORIZATION`；`error.messageKey=api.error.lifecycle.selfApprovalForbidden`；`error.retryable=false` |
| **ERR-2** | 错误信封遵循既有统一 `ErrorEnvelope`（`metadata` + `error`）；`error.message` 为英文业务可读句，**不**回显 `exceptionReason`、不回显 submitter 全名以外敏感字段 |
| **ERR-3** | `SELF_APPROVAL_FORBIDDEN` 与既有 `AUTHORIZATION` 类错误一致不写决策成功审计行；可选写"决策被拒"失败审计（实现可选，不强制） |

### 4.3 GROUP_ADMIN 例外干预

| ID | 决策 |
| --- | --- |
| **EX-1** | 例外触发条件：`exceptionIntervention=true` **且** `session.roles()` 含 `GROUP_ADMIN` 或 `GLOBAL_ADMIN` |
| **EX-2** | 例外字段强制：`exceptionReason` 非空白（≤2048 字符）；`secondaryConfirmed=true`；任一缺失 → 既有 messageKey `api.error.template.exceptionReasonRequired` / `api.error.template.exceptionSecondaryConfirmRequired`（模板）或等价 `api.error.lifecycle.exceptionReasonRequired` / `api.error.lifecycle.exceptionSecondaryConfirmRequired`（母版/条款，见 Q2） |
| **EX-3** | 非 `GROUP_ADMIN`/`GLOBAL_ADMIN` 角色传 `exceptionIntervention=true` → 既有 `api.error.template.exceptionInterventionNotAllowed`（模板）或等价 `api.error.lifecycle.exceptionInterventionNotAllowed`（母版/条款） |
| **EX-4** | 例外放行后的生命周期审计行必须持久化 `selfApprovalException=true` + `exceptionReason`；管理端生命周期审计读取 API 在 `GROUP_ADMIN` 带 `groupScope` + `templateId`（既有矩阵 §348）下可见该字段 |
| **EX-5** | 例外**不**绕过其它既有校验（fidelity 确认、rationale、reject 结构化字段、发布 gate 等）；仅绕过同人比对这一条 |
| **EX-6** | 例外不可用于"无提交记录"路径（CMP-3）—— 该路径本就不阻断，无需例外 |

### 4.4 角色范围

| ID | 决策 |
| --- | --- |
| **ROLE-1** | 例外干预权授予 `GROUP_ADMIN` 与 `GLOBAL_ADMIN`；`TEMPLATE_APPROVER` / `MASTER_DESIGNER` / `TEMPLATE_TESTER` 一律无例外权 |
| **ROLE-2** | `GROUP_ADMIN` 仅可在其被授权的 `groupCode` 范围内发起例外；越组 → 既有 `403`（沿用 `GroupAccessService` 既有组范围校验，本片不新增） |
| **ROLE-3** | 同人阻断对**所有角色**生效，含 `GLOBAL_ADMIN` 自提自批；`GLOBAL_ADMIN` 唯一可通过例外路径放行 |

### 4.5 本片范围锁定

| ID | 决策 |
| --- | --- |
| **S-1** | 覆盖决策点：模板 `recordApprovalDecision`（APPROVED/REJECTED）；母版 `decideReview`（APPROVED/REJECTED）；条款 `transition`（APPROVE_REVIEW/REJECT_REVIEW） |
| **S-2** | **不**覆盖：模板 `publish`（发布不是审批，发布人 == 审批人的情形由后续四眼 ADR 决定，本片不动）；模板测试裁定 `recordTestDecision`（测试裁定非审批，不在 CE-G01 范围） |
| **S-3** | **不**实现四眼双人复核（D1）；不修订 ADR-0021；不在审计/视图引入"second approver"字段 |
| **S-4** | 母版/条款 DTO 新增 `exceptionIntervention` / `exceptionReason` / `secondaryConfirmed` 三字段（与模板 `LifecycleDecisionRequest` 对齐，OpenAPI 同步更新） |
| **S-5** | 审计持久化：模板复用既有 `TemplateLifecycleRecordEntity`（新增 `selfApprovalException` boolean + `exceptionReason` varchar(2048) 列，Flyway 迁移）；母版 `MasterReviewRecordEntity` 同样新增两列；条款审计由 `ManagementAuditRecorder.recordContentModuleReviewTransition` 携带例外标记（审计 JSON 扩展字段，无需 schema 迁移若审计为 JSON 列；若为关系列则 Flyway 迁移） |
| **S-6** | 明确非目标：CE-G02 水印、CE-G03 PII 治理、CE-G04 legal hold、CE-G06 审计复现、CE-K01 发布包钉扎、四眼复核、AD Group 真实 LDAP 适配 |

---

## 5. Trigger

| # | 触发 |
| --- | --- |
| T1 | `TEMPLATE_APPROVER` 对 `APPROVAL` 状态模板发起 `recordApprovalDecision`，且 `session.username()` == 该模板最新 `SUBMIT_FOR_APPROVAL` 行的 `actorUsername` |
| T2 | 母版复核角色对 `PENDING_REVIEW` 母版发起 `decideReview`，且 `session.username()` == 最新 `SUBMITTED` 行的 `actorUsername` |
| T3 | 条款审批角色对 `SUBMITTED` 条款版本发起 `transition(APPROVE_REVIEW/REJECT_REVIEW)`，且 `session.username()` == 该版本 submitter |
| T4 | T1/T2/T3 同人场景下，决策人为 `GROUP_ADMIN`/`GLOBAL_ADMIN` 且请求带 `exceptionIntervention=true` + `exceptionReason` + `secondaryConfirmed=true` |
| T5 | T1/T2/T3 同人场景下，决策人非 `GROUP_ADMIN`/`GLOBAL_ADMIN` 但请求带 `exceptionIntervention=true` |
| T6 | T4 但 `exceptionReason` 缺失或 `secondaryConfirmed != true` |
| T7 | 非同人场景（正常路径）—— 回归：行为不变 |

---

## 6. Preconditions

- 决策人已通过既有认证授权（管理会话有效、角色/能力检查通过）。
- 目标对象处于可决策状态（模板 `APPROVAL` / 母版 `PENDING_REVIEW` / 条款版本 `SUBMITTED`）。
- 历史存在至少一条提交记录（CMP-3 例外：无提交记录则不阻断）。
- 工作树：`feat/ce-g01-self-approval-block`。
- Flyway 迁移已为 `template_lifecycle_record` / `master_review_record` 添加 `self_approval_exception` (boolean default false) + `exception_reason` (varchar(2048) nullable) 列；条款审计 JSON schema 允许 `selfApprovalException` + `exceptionReason` 键。

---

## 7. Primary journey（成功 — 非同人正常路径）

1. 决策人（与提交人不同）发起审批决策请求。
2. 既有授权/角色/状态/表单校验全部通过。
3. 同人比对：`decisionActor != lastSubmitActor` → 通过。
4. 状态机迁移完成；审计行写入（`selfApprovalException=false`，`exceptionReason=null`）。
5. 返回成功视图（既有信封形态不变）。

## 7b. 例外干预成功路径（同人 + GROUP_ADMIN）

1. `GROUP_ADMIN`（同人于提交人）发起决策请求，带 `exceptionIntervention=true` + 非空 `exceptionReason` + `secondaryConfirmed=true`。
2. 既有授权/角色/状态校验通过。
3. 例外字段校验通过（`exceptionReason` 非空、`secondaryConfirmed=true`、角色为 `GROUP_ADMIN`/`GLOBAL_ADMIN`）。
4. 同人比对被例外路径**绕过**（不抛 `SELF_APPROVAL_FORBIDDEN`）。
5. 既有决策表单校验（rationale / fidelity / reject 结构化字段等）继续执行。
6. 状态机迁移完成；审计行写入 `selfApprovalException=true` + `exceptionReason` 文本。
7. 返回成功视图；管理端生命周期审计读取 API 在授权范围内可见例外标记与原因。

---

## 8. System responses

### 8.1 成功

- 非同人路径：HTTP 200，既有响应信封，审计行 `selfApprovalException=false`。
- 例外路径：HTTP 200，既有响应信封，审计行 `selfApprovalException=true` + `exceptionReason` 持久化。

### 8.2 失败

| 条件 | HTTP | `error.code` | `error.category` | `messageKey` | `retryable` | 说明 |
| --- | --- | --- | --- | --- | --- | --- |
| 同人决策且无例外干预 | 403 | `SELF_APPROVAL_FORBIDDEN` | `AUTHORIZATION` | `api.error.lifecycle.selfApprovalForbidden` | `false` | fail-closed；状态不变；不写决策成功审计 |
| 非例外角色传 `exceptionIntervention=true` | 403 | `EXCEPTION_INTERVENTION_NOT_ALLOWED` | `AUTHORIZATION` | `api.error.lifecycle.exceptionInterventionNotAllowed`（或复用 `api.error.template.exceptionInterventionNotAllowed`，见 Q2） | `false` | 沿用既有语义 |
| 例外 `exceptionReason` 缺失/空白 | 422 | `EXCEPTION_REASON_REQUIRED` | `VALIDATION` | `api.error.lifecycle.exceptionReasonRequired`（或复用模板既有 key） | `false` | 沿用既有语义 |
| 例外 `secondaryConfirmed != true` | 422 | `EXCEPTION_SECONDARY_CONFIRM_REQUIRED` | `VALIDATION` | `api.error.lifecycle.exceptionSecondaryConfirmRequired`（或复用模板既有 key） | `false` | 沿用既有语义 |

失败响应必须：

- 使用统一 `ErrorEnvelope`（`metadata` + `error`）。
- `error.message` 为英文业务可读句，不回显 `exceptionReason` 原文外的敏感内容、不回显其它用户 PII。
- fail-closed：不部分迁移状态、不写决策成功审计行。

---

## 9. Acceptance scenarios（Given / When / Then）

### 模板模块（Template）

#### BDD-CE-G01-T-001 — 模板同人审批被阻断（回归）

**Given** 模板 `T1` 处于 `APPROVAL` 状态，最新 `SUBMIT_FOR_APPROVAL` 行的 `actorUsername="alice"`
**And** 当前会话用户为 `alice`，持 `TEMPLATE_APPROVER` 角色，已通过既有授权/状态校验
**When** `alice` 对 `T1` 发起 `recordApprovalDecision(APPROVED)`，请求**未**带 `exceptionIntervention=true`
**Then** HTTP **403**
**And** `error.code=SELF_APPROVAL_FORBIDDEN`，`category=AUTHORIZATION`，`messageKey=api.error.lifecycle.selfApprovalForbidden`，`retryable=false`
**And** 模板 `T1` 状态仍为 `APPROVAL`（未迁移至 `PENDING_RELEASE`）
**And** 不写入 `RECORD_APPROVAL_DECISION` 成功审计行
**And** 不写入 `selfApprovalException=true` 行

#### BDD-CE-G01-T-002 — 模板同人 + GROUP_ADMIN 例外放行且审计留痕

**Given** 模板 `T1` 处于 `APPROVAL`，最新提交人为 `alice`
**And** 当前会话用户为 `alice`，持 `GROUP_ADMIN` 角色（且 `groupScope` 覆盖 `T1` 所属组）
**When** `alice` 发起 `recordApprovalDecision(APPROVED)`，带 `exceptionIntervention=true`、`exceptionReason="Solo approval due to approver pool outage 2026-07-14"`、`secondaryConfirmed=true`，且满足既有 rationale/fidelity 确认
**Then** HTTP **200**，模板迁移至 `PENDING_RELEASE`
**And** 写入 `RECORD_APPROVAL_DECISION` 审计行，`selfApprovalException=true`，`exceptionReason` 列持久化原文本
**And** 后续任一 transition 不抹除该审计行的 `selfApprovalException` / `exceptionReason` 值

#### BDD-CE-G01-T-003 — 模板同人 + 非 GROUP_ADMIN 例外请求被拒

**Given** 模板 `T1` 处于 `APPROVAL`，最新提交人为 `alice`
**And** 当前会话用户为 `alice`，仅持 `TEMPLATE_APPROVER` 角色
**When** `alice` 发起决策请求带 `exceptionIntervention=true`
**Then** HTTP **403**，`error.code=EXCEPTION_INTERVENTION_NOT_ALLOWED`，`messageKey=api.error.template.exceptionInterventionNotAllowed`
**And** 状态不变；不写决策成功审计行

#### BDD-CE-G01-T-004 — 模板同人 + GROUP_ADMIN 例外字段缺失被拒

**Given** 同 T-002 但 `exceptionReason` 为空白 或 `secondaryConfirmed != true`
**When** 发起例外决策
**Then** HTTP **422**，`error.code=EXCEPTION_REASON_REQUIRED`（或 `EXCEPTION_SECONDARY_CONFIRM_REQUIRED`），对应 messageKey
**And** 状态不变

#### BDD-CE-G01-T-005 — 模板非同人正常路径无回归

**Given** 模板 `T1` 处于 `APPROVAL`，最新提交人为 `alice`，当前会话用户为 `bob`（不同人），持 `TEMPLATE_APPROVER`
**When** `bob` 发起 `recordApprovalDecision(APPROVED)`，不带例外字段
**Then** HTTP **200**，模板迁移至 `PENDING_RELEASE`
**And** 审计行 `selfApprovalException=false`，`exceptionReason=null`
**And** 既有所有下游行为（API policy skeleton、collaboration work item）不因本片改变

### 母版模块（Master）

#### BDD-CE-G01-M-001 — 母版同人审批被阻断（回归）

**Given** 母版 `M1` 处于 `PENDING_REVIEW`，最新 `MasterReviewRecordEntity` 行 `action=SUBMITTED`，`actorUsername="alice"`
**And** 当前会话用户为 `alice`，持 `reviewMasters` 能力
**When** `alice` 发起 `decideReview(APPROVED)`，未带 `exceptionIntervention=true`
**Then** HTTP **403**，`SELF_APPROVAL_FORBIDDEN`，`api.error.lifecycle.selfApprovalForbidden`
**And** 母版状态仍为 `PENDING_REVIEW`
**And** 不写入 `APPROVED` 行

#### BDD-CE-G01-M-002 — 母版同人 + GROUP_ADMIN 例外放行且审计留痕

**Given** 母版 `M1` 处于 `PENDING_REVIEW`，最新提交人为 `alice`
**And** 当前会话用户为 `alice`，持 `GROUP_ADMIN` 角色
**When** `alice` 发起 `decideReview(APPROVED)`，带 `exceptionIntervention=true`、`exceptionReason="..."`、`secondaryConfirmed=true`
**Then** HTTP **200**，母版迁移至 `APPROVED`
**And** 写入 `MasterReviewRecordEntity` 行 `action=APPROVED`，`selfApprovalException=true`，`exceptionReason` 持久化

#### BDD-CE-G01-M-003 — 母版非同人正常路径无回归

**Given** 母版 `M1` 处于 `PENDING_REVIEW`，最新提交人为 `alice`，当前会话为 `bob`
**When** `bob` 发起 `decideReview(APPROVED)`，不带例外字段
**Then** HTTP **200**，母版迁移至 `APPROVED`，审计行 `selfApprovalException=false`

### 条款模块（Content Module）

#### BDD-CE-G01-C-001 — 条款同人审批被阻断（回归）

**Given** 条款版本 `V1` 处于 `SUBMITTED`，提交人（`submittedBy` 或回查审计行）为 `alice`
**And** 当前会话用户为 `alice`，持 `canDecideContentModuleReviews` 能力 + `TEMPLATE_APPROVER` 角色对
**When** `alice` 发起 `transition(APPROVE_REVIEW)`，未带 `exceptionIntervention=true`
**Then** HTTP **403**，`SELF_APPROVAL_FORBIDDEN`，`api.error.lifecycle.selfApprovalForbidden`
**And** 版本状态仍为 `SUBMITTED`，未迁移至 `APPROVED`
**And** 不写入 `APPROVE_REVIEW` 成功审计行

#### BDD-CE-G01-C-002 — 条款同人 + GROUP_ADMIN 例外放行且审计留痕

**Given** 条款版本 `V1` 处于 `SUBMITTED`，提交人为 `alice`
**And** 当前会话用户为 `alice`，持 `GROUP_ADMIN` 角色
**When** `alice` 发起 `transition(APPROVE_REVIEW)`，带 `exceptionIntervention=true`、`exceptionReason="..."`、`secondaryConfirmed=true`
**Then** HTTP **200**，版本迁移至 `APPROVED` + `lifecycleState=ACTIVE`
**And** `ManagementAuditRecorder.recordContentModuleReviewTransition` 写入的审计 JSON 含 `selfApprovalException=true` + `exceptionReason` 文本

#### BDD-CE-G01-C-003 — 条款非同人正常路径无回归

**Given** 条款版本 `V1` 处于 `SUBMITTED`，提交人为 `alice`，当前会话为 `bob`
**When** `bob` 发起 `transition(APPROVE_REVIEW)`，不带例外字段
**Then** HTTP **200**，版本迁移至 `APPROVED`，审计 JSON `selfApprovalException=false`/缺省

### 跨模块与边界

#### BDD-CE-G01-X-001 — lastSubmitActor 缺失时不阻断（防误伤存量）

**Given** 历史迁移导致某模板无 `SUBMIT_FOR_APPROVAL` 行（或母版/条款无 `SUBMITTED`/submitter 记录）
**When** 任一持权用户发起决策
**Then** **不**因 `SELF_APPROVAL_FORBIDDEN` 失败（CMP-3）；正常走既有路径

#### BDD-CE-G01-X-002 — GLOBAL_ADMIN 自提自批走例外路径

**Given** `GLOBAL_ADMIN` 用户 `root` 同时是模板 `T1` 的提交人
**When** `root` 发起决策带 `exceptionIntervention=true` + `exceptionReason` + `secondaryConfirmed=true`
**Then** HTTP **200**，例外放行，审计行 `selfApprovalException=true`

#### BDD-CE-G01-X-003 — username 大小写精确匹配

**Given** 提交人 username 为 `Alice`（精确大小写），决策人 session username 为 `alice`
**When** 决策
**Then** 视为**不同人**（case-sensitive 比较），不阻断（CMP-1）
**And** 反之 `Alice` vs `Alice` 视为同人阻断

#### BDD-CE-G01-X-004 — 例外不绕过其它既有校验

**Given** 模板同人场景 + `GROUP_ADMIN` 例外字段齐全，但缺失 `commentSummary`（rationale）或 `fidelityViewedConfirmed`
**When** 发起 `APPROVED` 决策
**Then** 仍按既有 messageKey 失败（`api.error.template.decisionRationaleRequired` / `decisionFidelityConfirmationRequired`）
**And** 状态不变（EX-5）

#### BDD-CE-G01-X-005 — GROUP_ADMIN 跨组例外被既有组范围校验拒绝

**Given** `GROUP_ADMIN` `g1-admin` 对不属于其授权 `groupCode` 的模板发起例外决策
**When** 调用决策端点
**Then** 既有 `GroupAccessService` 组范围校验先行返回 `403`（ROLE-2），不进入同人比对

#### BDD-CE-G01-X-006 — REJECT 同人同样阻断

**Given** 模板/母版/条款处于待决策状态，提交人 == 决策人，请求 `decision=REJECTED`（或条款 `REJECT_REVIEW`），不带例外
**When** 决策
**Then** HTTP **403** `SELF_APPROVAL_FORBIDDEN`（阻断覆盖 APPROVE 与 REJECT 双向，S-1）

#### BDD-CE-G01-X-007 — 审计持久化跨 transition 不丢失

**Given** 模板通过例外路径审批通过，审计行 `selfApprovalException=true`
**When** 后续对该模板执行 `publish` 或 `abandon` 等 transition
**Then** 原 `RECORD_APPROVAL_DECISION` 审计行的 `selfApprovalException` / `exceptionReason` 列**不被**覆写或清空（EX-4）

#### BDD-CE-G01-X-008 — 管理端审计读取 API 暴露例外字段

**Given** 已有例外审计行存在
**When** `AUDIT_ADMIN` / `GLOBAL_ADMIN` / `GROUP_ADMIN`（带 `groupScope`+`templateId`）调用生命周期审计读取 API
**Then** 响应包含 `selfApprovalException` 与 `exceptionReason` 字段
**And** `GROUP_ADMIN` 缺 `groupScope`/`templateId` 时仍按既有矩阵返回 `422`（本片不放宽）

### 契约 / 文档对齐

#### BDD-CE-G01-DOC-001 — OpenAPI 同步新增例外字段

**Given** 本片实现完成
**When** 对照 `docs/api/openapi-v1.yaml` 中母版决策 / 条款决策请求 schema
**Then** 母版 `DecideMasterReviewRequest` 与条款 `ContentModuleReviewTransitionRequest` 含可选 `exceptionIntervention` / `exceptionReason` / `secondaryConfirmed` 字段
**And** 模板 `LifecycleDecisionRequest` 既有字段不变（已存在）
**And** 错误码表新增 `SELF_APPROVAL_FORBIDDEN` / `EXCEPTION_INTERVENTION_NOT_ALLOWED` / `EXCEPTION_REASON_REQUIRED` / `EXCEPTION_SECONDARY_CONFIRM_REQUIRED`（若选择新建 lifecycle 命名空间 key，见 Q2）

#### BDD-CE-G01-DOC-002 — ADR-0021 不被修订

**Given** 用户 D1 拍板（2026-07-14）
**When** 本片交付
**Then** ADR-0021 状态/决策文本不变
**And** 不引入"second approver"/"four-eyes"字段或行为

#### BDD-CE-G01-DOC-003 — permission-matrix 增补例外干预角色

**Given** 本片交付
**When** 阅读 `docs/security/permission-matrix.md`
**Then** 矩阵注明：例外干预权仅 `GROUP_ADMIN` / `GLOBAL_ADMIN`；`TEMPLATE_APPROVER` / `MASTER_DESIGNER` / `TEMPLATE_TESTER` 无例外权
**And** 同人阻断对全部角色生效

---

## 10. Boundary / exception 摘要

| 边界 | 行为 |
| --- | --- |
| 无提交记录（CMP-3） | 不阻断；走既有路径 |
| `exceptionIntervention` 缺省 / `false` | 同人则阻断；非同人正常放行 |
| `exceptionReason` 仅空白字符 | 视为缺失 → 422 |
| 同人比对与既有授权检查的顺序 | 授权/角色/状态/表单校验先行 → 同人比对 → 状态迁移；失败点不写决策成功审计 |
| `REJECTED` / `REJECT_REVIEW` 同人 | 同样阻断（X-006） |
| 例外审计行被后续 transition 影响 | 不被覆写/清空（X-007） |
| `username` 大小写差异 | case-sensitive 精确比较（X-003） |
| `GLOBAL_ADMIN` 自提自批 | 走例外路径放行（X-002） |
| 跨组 `GROUP_ADMIN` 例外 | 既有组范围校验先行拒绝（X-005） |
| 审计读取权限 | 沿用既有 `AUDIT_ADMIN`/`GLOBAL_ADMIN`/`GROUP_ADMIN(+groupScope+templateId)` 矩阵（X-008） |
| 模板 `publish` / 测试裁定 | **不在**本片范围（S-2） |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 后端单测（三模块各一组） | T-001/002/003/004/005、M-001/002/003、C-001/002/003：同人阻断 + 例外放行 + 例外字段缺失 + 非同人回归 |
| 跨模块边界测试 | X-001…X-008：lastSubmitActor 缺失、GLOBAL_ADMIN、大小写、例外不绕过其它校验、跨组拒绝、REJECT 阻断、审计持久化、审计读取暴露 |
| Flyway 迁移测试 | `template_lifecycle_record` / `master_review_record` 新列存在；向后兼容（既有行 `selfApprovalException=false`/`null`） |
| 审计 JSON schema 测试 | 条款审计 JSON 含 `selfApprovalException` + `exceptionReason` 键且读取 API 暴露 |
| `mvn -B -ntp -f backend/pom.xml verify` | 门禁全绿（Checkstyle/PMD/SpotBugs/JaCoCo） |
| OpenAPI 契约测试 | 母版/条款决策 DTO 含例外字段；错误码表含新码 |
| 文档 | 本文件；`docs/api/openapi-v1.yaml`；`docs/api/contract-outline.md` 错误表；`docs/security/permission-matrix.md` 例外角色条目（由 doc-keeper 同提交） |

本片为**后端生命周期控制行为**，无管理 UI 用户可见变更 → **不要求** Playwright E2E/UIUX。若实现顺带触及前端决策对话框文案（提示同人阻断错误），则按前端门禁最小集补 Vitest（错误信封 messageKey 渲染），**不**强制 E2E。

---

## 12. Traceability

| 工件 | 路径 / ID |
| --- | --- |
| 计划 | `docs/plan/core-excellence-program-2026-07.md` §6 CE-G01；§10 D1 拍板 |
| 行为规格 | `docs/behavior/ce-g01-self-approval-block.md`（本文件） |
| 既有例外机制 | `backend/src/main/java/com/bank/docgen/template/service/DecisionFormService.java`（`validateExceptionIntervention`） |
| 三处决策点 | `TemplateLifecycleApprovalFlowSupport.recordApprovalDecision` / `MasterDocumentReviewSupport.decideReview` / `ContentModuleReviewService.transition` |
| 提交人来源 | `TemplateLifecycleRecordEntity`（SUBMIT_FOR_APPROVAL）/ `MasterReviewRecordEntity`（SUBMITTED）/ `ContentModuleVersionEntity.submittedBy` 或审计回查（Q1） |
| 权限矩阵 | `docs/security/permission-matrix.md`（`GROUP_ADMIN`/`GLOBAL_ADMIN`/`reviewMasters`/`canDecideContentModuleReviews`） |
| ADR | ADR-0021（四眼复核 — 维持拒绝，本片不修订） |
| OpenAPI | `docs/api/openapi-v1.yaml`（`LifecycleDecisionRequest` / `DecideMasterReviewRequest` / `ContentModuleReviewTransitionRequest` / 错误码表） |
| Slice / 分支 | `ce-g01-self-approval-block` · `feat/ce-g01-self-approval-block` · Task Master **#72** |
| 后续 | CE-G02 水印、CE-G03 PII、CE-G04 legal hold、CE-G06 审计复现；四眼复核待内控要求出现再修订 ADR-0021 |

---

## 13. 开放问题（不阻塞 `ready`；实现默认如下）

| ID | 问题 | 默认（可被用户推翻） | 阻塞？ |
| --- | --- | --- | --- |
| **Q1** | 条款模块 `lastSubmitActor` 来源：在 `ContentModuleVersionEntity` 新增 `submittedBy` 列，还是回查 `ManagementAuditRecorder` 的 `SUBMIT_FOR_REVIEW` 行？ | **新增 `submittedBy` 列**（Flyway 迁移；submit 时写入，approve/reject 不覆写）；回查审计路径作为兜底 | 否 |
| **Q2** | 母版/条款例外字段缺失的 messageKey：复用模板既有 `api.error.template.exceptionInterventionNotAllowed` / `exceptionReasonRequired` / `exceptionSecondaryConfirmRequired`，还是新建 `api.error.lifecycle.*` 命名空间？ | **新建 `api.error.lifecycle.*` 命名空间**（与 `api.error.lifecycle.selfApprovalForbidden` 同族；模板侧可保留既有 key 或迁移至 lifecycle 命名空间，二选一在实现片定） | 否 |
| **Q3** | 同人失败是否写"决策被拒"失败审计行？ | **不强制**：实现可选写失败审计（含 actor + targetId + reason=SELF_APPROVAL_FORBIDDEN），但**不得**写决策成功审计行 | 否 |
| **Q4** | `exceptionReason` 是否纳入审计导出 CSV / 摘要视图？ | **纳入**：在既有生命周期审计读取授权范围内可见；不在面向调用方的运行时审计暴露 | 否 |
| **Q5** | 母版 `decideReview` 现仅有 `decision` + `commentSummary` 字段，是否同时新增 `exceptionIntervention`/`exceptionReason`/`secondaryConfirmed`？ | **是**（S-4）；OpenAPI 同步 | 否 |
| **Q6** | 条款 `REJECT_REVIEW` 同人是否阻断？ | **是**（X-006 覆盖 APPROVE 与 REJECT 双向） | 否 |

若用户明确推翻 Q1–Q6 默认，再修订本规格后进入实现。

---

## 14. BDD readiness

```
bdd_readiness: ready
owning_doc: docs/behavior/ce-g01-self-approval-block.md
task_ids: [CE-G01, slice:ce-g01-self-approval-block, taskmaster:#72]
acceptance_scenario_count: 22
open_questions: [Q1 submittedBy source, Q2 exception messageKey namespace, Q3 failure audit row, Q4 exceptionReason in export, Q5 master DTO fields, Q6 REJECT block]
next: plan-orchestrator → backend-engineer (TDD) in worktree D:/working/DGE-ce-g01-self-approval-block
```

**Acceptance scenario count: 22**

| 分组 | 场景 ID | 计数 |
| --- | --- | --- |
| 模板 | BDD-CE-G01-T-001 … 005 | 5 |
| 母版 | BDD-CE-G01-M-001 … 003 | 3 |
| 条款 | BDD-CE-G01-C-001 … 003 | 3 |
| 跨模块/边界 | BDD-CE-G01-X-001 … 008 | 8 |
| 契约/文档 | BDD-CE-G01-DOC-001 … 003 | 3 |
| **合计** | | **22** |

**非阻塞开放问题: 6**（Q1–Q6，均有实现默认）

---

## 15. 明确禁止（实现片）

- 实现代码前不得跳过本规格中的失败场景测试（TDD Red → Green）。
- 不得实现四眼双人复核、`secondApprover` 字段或"双人签名"语义（D1）。
- 不得修订 ADR-0021 状态或决策文本。
- 不得让 `TEMPLATE_APPROVER` / `MASTER_DESIGNER` / `TEMPLATE_TESTER` 持有例外干预权。
- 不得将同人比对放在授权/状态校验之前（顺序错误会导致无权用户也能触发比对副作用）。
- 不得在例外放行后抹除或覆写审计行的 `selfApprovalException` / `exceptionReason`。
- 不得在本片实现 CE-G02/G03/G04/G06 或 CE-K01。
- 不得将 `exceptionReason` 暴露给运行时调用方审计或非授权审计读者。
- 不得宣称 production go-live。
