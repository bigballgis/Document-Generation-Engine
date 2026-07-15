# BDD 行为规格：CE-C05 — `originalBatchId` 重试血缘

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-15  
**BDD ID 前缀**: `BDD-CE-C05`  
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) Wave CE-C · CE-C05  
**Slice**: `ce-c05-original-batch-id`  
**Worktree**: `D:/working/DGE-ce-c05-original-batch-id` · `feat/ce-c05-original-batch-id`  
**Task Master**: **#70**  
**Formal phase**: **None**  
**完成声明约束**: 本切片落地批量请求可选 `originalBatchId` 的**存在性 + 同凭证**校验、成功响应回显、调用/审计持久化关联与契约文档对齐；**不**宣称 go-live；**不**实现 CE-C06；**不**触碰 CE-K06b / CE-U06 / CE-U13；**不**强制校验「重试 items ⊆ 原批次失败项」（调用方责任，本片非目标）。

---

## 1. 概述

| 行为域 | 摘要 |
| --- | --- |
| **请求字段** | 批量生成请求体可选 `originalBatchId`（OpenAPI 模式 `^BATCH-[A-Za-z0-9]+$`）；CE-C01/C02 已可绑定该字段，本片补齐**语义校验与血缘持久化** |
| **校验** | 字段出现时：必须能在调用方 **同一 API 凭证** 下解析到既有 `BATCH_ROOT`（`batchExternalId` / `batchId` 匹配）；否则 fail-closed，**不**创建新批次/异步任务 |
| **响应** | 校验通过并受理/完成后，`result.batch.originalBatchId` 回显请求值；新 `batchId` 与原批次不同 |
| **不可变原批次** | 重试**不得**扩展或改写原批次结果；原批次 invocation / 明细保持不变 |
| **审计关联** | 新批次的 API 调用审计与 invocation 持久化必须记录 `originalBatchId`（或等效关联字段），便于失败项重试溯源 |
| **契约** | OpenAPI / contract-outline / 错误码清单与实现对齐；示例可展示带血缘的批量响应 |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| OpenAPI `BatchGenerateRequest.originalBatchId` 与 `BatchResult.originalBatchId` 已声明（pattern） | `docs/api/openapi-v1.yaml` |
| DTO 可选绑定，**无**存在性/同凭证校验 | `BatchGenerateRequestBody`；CE-C01 BDD-CE-C02-008 |
| `BatchResultView` **无** `originalBatchId` 字段 | `BatchResultView.java` |
| `InvocationParameterSanitizer.sanitizeBatchRequest` **不**写入 `originalBatchId` | sanitizer |
| Invocation 有 `batchExternalId` + `credentialId`，可按批次+凭证查找 | `ApiInvocationRecordEntity` / repository |
| SoT：异步失败项重试用新批次+新 `idempotencyKey`+`originalBatchId`；原批次不改写 | ADR-0004；requirements / PRD / domain / contract-outline / permission-matrix |
| 错误码清单尚无 `ORIGINAL_BATCH_NOT_FOUND` | OpenAPI `ErrorCode`；contract-outline 错误表 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **API 调用方** | Runtime caller | 持 API 凭证 + AD Group；异步批量部分成功后，用新批次重试失败项并传入 `originalBatchId` |
| **系统** | Runtime batch + invocation + audit | 校验血缘、创建新批次、回显字段、写审计；fail-closed 且不泄露跨凭证批次是否存在 |
| **审计 / 管理端读者** | Audit / admin | 可从调用记录/审计摘要看到重试关联的 `originalBatchId`；不得见敏感明文 |
| **契约消费者** | 集成方 / OpenAPI 客户端 | 依赖 schema、错误码与字段语义 |

---

## 3. Goal

1. 批量请求可选 `originalBatchId` 在出现时执行**可测试**的血缘校验：原批次存在且属**同一 API 凭证**。  
2. 校验失败 → 统一错误信封，**不**创建批次或异步任务，**不**改写任何既有批次。  
3. 校验成功 → 新批次正常生成路径；响应 `result.batch.originalBatchId` 回显；新 `batchId` ≠ `originalBatchId`。  
4. 新批次 invocation / 审计持久化包含 `originalBatchId` 关联。  
5. 省略 `originalBatchId` 时行为与今日「普通新批次」一致（无血缘校验、无回显字段义务）。  
6. 契约文档（OpenAPI、contract-outline、错误码）与上述行为对齐。  
7. 不向错误消息、日志或审计泄露他凭证批次是否存在、变量明文、加密密码等敏感信息。

---

## 4. 已确认决策（confirmed）

### 4.1 产品 / 契约基线（既有 SoT，本片落地）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **OB-1** | 异步批量部分成功后的失败项重试必须使用**新批次**和**新的** `idempotencyKey`，并通过 `originalBatchId` 关联原批次；原批次结果不被扩展或改写 | ADR-0004；requirements / PRD / domain / contract-outline |
| **OB-2** | 字段名确认为 `originalBatchId`；模式 `^BATCH-[A-Za-z0-9]+$`（与 `batchId` 一致） | OpenAPI |
| **OB-3** | 批量审计须记录失败项重试关联的 `originalBatchId`（或等效关联字段） | requirements；permission-matrix；ADR-0004 |
| **OB-4** | 授权/资源探测不得泄露未授权资源是否存在；安全错误消息保持通用英文 | requirements / permission-matrix |
| **OB-5** | 批量未知字段严格拒绝（CE-C02）；`originalBatchId` 为已声明可选字段 | CE-C01/C02 Done |

### 4.2 本片范围锁定（confirmed for this slice）

| ID | 决策 |
| --- | --- |
| **S-1** | **何时校验**：请求体提供非 null 的 `originalBatchId` 时必须校验；省略或 JSON `null` → 不校验、不写血缘、成功响应可不含 `originalBatchId`（或省略该可选属性） |
| **S-2** | **格式**：不符合 OpenAPI pattern → `400 REQUEST_BODY_INVALID`，`fieldErrors` 指向 `originalBatchId`，`reason` ∈ {`PATTERN_MISMATCH`,`INVALID_FORMAT`}；空字符串按格式非法处理（不得当「省略」） |
| **S-3** | **存在性 + 同凭证**：在调用方当前 `credentialId` 下查找 `invocation_kind = BATCH_ROOT` 且 `batch_external_id = originalBatchId` 的记录。找不到（含他凭证批次、已清理/过期不可见、非 ROOT）→ **`404 ORIGINAL_BATCH_NOT_FOUND`**（不区分「不存在」与「他凭证」，防探测） |
| **S-4** | **错误码**：`ORIGINAL_BATCH_NOT_FOUND`；`category=BATCH`；`messageKey=api.error.batch.originalBatchNotFound`；`retryable=false`；英文 message：`Original batch was not found.`；HTTP **404**。OpenAPI `ErrorCode` 与 contract-outline 错误表同步新增 |
| **S-5** | **模板 / 环境**：本片**不**额外要求原批次模板或路径环境与当前请求一致；血缘以「同凭证 + batchId」为充分条件（调用方通常在同模板上重试，但非本片硬门槛） |
| **S-6** | **成功响应**：校验通过后，同步批量完成响应与异步批量受理后的任务查询批次结果中，`result.batch.originalBatchId` **必须**等于请求值；`result.batch.batchId` 为**新**批次 id |
| **S-7** | **原批次不可变**：重试路径不得更新原 `BATCH_ROOT` / `BATCH_ITEM` 的结果、状态或明细；不得把新 item 挂到原 `batchId` |
| **S-8** | **审计 / invocation**：新批次 `BATCH_ROOT`（及适用的审计事件）必须持久化 `originalBatchId`。实现可选：专用列 **或** `parametersStorage` / 审计摘要中的显式键 `originalBatchId`；须可被后续排查读取。`InvocationParameterSanitizer.sanitizeBatchRequest`（或等价路径）在字段存在且校验通过后写入该键 |
| **S-9** | **同步与异步**：字段位于共享 `BatchGenerateRequest`；同步/异步批量在字段出现时均执行 S-2/S-3。产品主旅程是异步失败项重试；同步带血缘亦允许（关联用途） |
| **S-10** | **幂等**：血缘校验失败发生在创建批次之前；不得留下「成功新批次」幂等记录。校验失败为不可自动重执行类失败（与既有校验失败幂等基线一致）。成功路径仍要求调用方使用**新的** `idempotencyKey`（与原批次不同） |
| **S-11** | **明确非目标**：强制「items ⊆ 原批次失败 itemId」；强制「部分成功」才允许传 `originalBatchId`；管理 UI / Playwright E2E；CE-C06；CE-K06b/U06/U13；go-live / CD-3；改写 ADR-0004 决策正文（仅契约/需求落地，ADR 已含产品规则） |

### 4.3 与相邻切片边界

| ID | 决策 |
| --- | --- |
| **B-1** | CE-C01/C02：BDD-CE-C02-008「不触发 CE-C05」由本片**取代**；字段仍可解析，但本片起必须执行血缘校验 |
| **B-2** | CE-C03 fidelityWarnings 形态不变；带血缘的新批次成功项仍返回完整 `FidelityWarning[]` |
| **B-3** | CE-C04 凭证到期与本片正交；校验使用已认证凭证身份 |
| **B-4** | CE-U11 排障 UI 不强制本片改 FE；若管理端调用记录已展示 parameters 摘要，实现后应能看到 `originalBatchId`（只读，非本片 UI 交付） |

---

## 5. Trigger

| # | 触发 |
| --- | --- |
| T1 | 调用方对同步或异步 `batch-generate` 提交可选 `originalBatchId` |
| T2 | 异步批量部分成功后，调用方用新 `idempotencyKey` + 失败项子集 + `originalBatchId` 提交重试批次 |
| T3 | 调用方省略 `originalBatchId` 提交普通批量 |
| T4 | 调用方传入格式非法或不存在/他凭证的 `originalBatchId` |

---

## 6. Preconditions

| # | 前置 |
| --- | --- |
| P1 | 调用方凭证有效（ACTIVE / EXPIRING_SOON），AD Group + 模板授权通过 |
| P2 | 模板/版本/输出模式允许批量；items 合法且 `itemId` 同批唯一 |
| P3 | 重试场景：原批次已存在且为调用方同凭证下的 `BATCH_ROOT`；调用方使用**新的** `idempotencyKey` |
| P4 | CE-C01/C02 严格请求体行为已在线（未知字段 400） |

---

## 7. Primary journey（成功路径）

1. 调用方完成异步批量，部分 item 失败，记下 `batchId = BATCH-ORIG…`。  
2. 调用方组装新批量请求：仅失败项、`idempotencyKey` 新值、`originalBatchId = BATCH-ORIG…`。  
3. 系统校验 pattern → 按同凭证查找 `BATCH_ROOT` → 通过。  
4. 系统创建**新** `batchId`，按同步或异步既有路径执行；**不**修改原批次。  
5. 响应（或任务查询批次结果）含 `batchId`（新）与 `originalBatchId`（原）。  
6. 新批次 invocation / 审计持久化含 `originalBatchId`。

---

## 8. System responses

### 成功

- HTTP 与既有批量模式一致（同步完成 200；异步受理 202 等）。  
- `result.batch.batchId` = 新 id；`result.batch.originalBatchId` = 请求值（当请求提供时）。  
- `metadata` 含既有 `auditId` / `traceId` / 幂等字段。  
- 原批次查询结果与重试前 bitwise/语义一致（状态与明细不因重试改变）。

### 失败（本片新增/收紧）

| 条件 | HTTP | code | category | retryable |
| --- | --- | --- | --- | --- |
| pattern / 空串非法 | 400 | `REQUEST_BODY_INVALID` | `VALIDATION` | false |
| 同凭证下无匹配 `BATCH_ROOT`（含他凭证） | 404 | `ORIGINAL_BATCH_NOT_FOUND` | `BATCH` | false |

既有批量/授权/幂等错误保持不变。

---

## 9. Acceptance scenarios（Given / When / Then）

#### BDD-CE-C05-001 — 省略字段：普通新批次

**Given** 合法批量请求且**不**含 `originalBatchId`  
**When** 调用同步或异步 batch-generate  
**Then** 不执行原批次查找  
**And** 创建新批次并按既有成功路径返回  
**And** 成功响应可不包含 `result.batch.originalBatchId`  
**And** 新批次审计/parameters **不**伪造 `originalBatchId`

#### BDD-CE-C05-002 — 异步失败项重试：校验通过 + 回显 + 新 batchId

**Given** 同凭证下已存在异步批量 `BATCH_ROOT`，`batchId = BATCH-ORIG01`，且为部分成功  
**And** 调用方使用新的 `idempotencyKey` 与待重试 items，请求含 `originalBatchId: "BATCH-ORIG01"`  
**When** 提交异步 batch-generate  
**Then** 系统受理新批次（HTTP 202 或既有异步受理语义）  
**And** 新 `batchId` ≠ `BATCH-ORIG01`  
**And** 任务查询完成后的 `result.batch.originalBatchId` = `BATCH-ORIG01`  
**And** 原批次结果未被扩展或改写

#### BDD-CE-C05-003 — 同步批量带合法 originalBatchId

**Given** 同凭证下已存在 `BATCH_ROOT` `BATCH-ORIG02`  
**And** 同步批量请求含合法 `originalBatchId: "BATCH-ORIG02"` 且其余字段合法  
**When** 调用同步 batch-generate  
**Then** 校验通过并执行同步批量  
**And** 响应 `result.batch.originalBatchId` = `BATCH-ORIG02`  
**And** `result.batch.batchId` 为新 id

#### BDD-CE-C05-004 — 审计 / invocation 持久化关联

**Given** BDD-CE-C05-002 或 003 成功路径  
**When** 检查新批次 `BATCH_ROOT` 的 invocation parameters / 审计摘要  
**Then** 可观察到 `originalBatchId` 等于请求值（专用列或 JSON 键）  
**And** 不含加密密码、变量明文或完整敏感请求体

#### BDD-CE-C05-005 — 原批次不存在 → 404 ORIGINAL_BATCH_NOT_FOUND

**Given** `originalBatchId` 符合 pattern 但系统中无任何同凭证 `BATCH_ROOT` 匹配  
**When** 提交 batch-generate  
**Then** HTTP 404，`error.code = ORIGINAL_BATCH_NOT_FOUND`，`category = BATCH`，`retryable = false`  
**And** `messageKey = api.error.batch.originalBatchNotFound`  
**And** **不**创建批次或异步任务  
**And** 错误 message 不暗示「是否属于其他调用方」

#### BDD-CE-C05-006 — 他凭证批次 → 同一 404（防探测）

**Given** 批次 `BATCH-OTHER1` 存在于**另一** API 凭证下  
**And** 当前调用方请求 `originalBatchId: "BATCH-OTHER1"`  
**When** 提交 batch-generate  
**Then** 响应与 BDD-CE-C05-005 **相同**（`ORIGINAL_BATCH_NOT_FOUND`）  
**And** 不返回 403 差异化信息以致可枚举他凭证资源

#### BDD-CE-C05-007 — 格式非法 → 400 REQUEST_BODY_INVALID

**Given** `originalBatchId` 为 `"not-a-batch"`、`""` 或其他不符 pattern 的值  
**When** 提交 batch-generate  
**Then** HTTP 400，`error.code = REQUEST_BODY_INVALID`  
**And** `fieldErrors` 含 `field = originalBatchId`，`reason` 为 `PATTERN_MISMATCH` 或 `INVALID_FORMAT`  
**And** **不**创建批次或异步任务

#### BDD-CE-C05-008 — 原批次不可变

**Given** 原批次 `BATCH-ORIG03` 已有固定 summary / items 快照  
**When** 带 `originalBatchId: "BATCH-ORIG03"` 的重试批次成功完成  
**Then** 再次查询原批次（invocation / 任务结果）时 summary 与 items 与重试前一致  
**And** 新成功/失败项只出现在新 `batchId` 下

#### BDD-CE-C05-009 — 校验失败不留成功幂等批次

**Given** 非法或不存在的 `originalBatchId` 导致 BDD-CE-C05-005/007  
**When** 检查幂等与 invocation 存储  
**Then** 不存在以该请求语义创建的成功新 `BATCH_ROOT`  
**And** 不改写任何既有批次

#### BDD-CE-C05-010 — 契约文档与错误码对齐

**Given** 本片交付集  
**When** 核对 OpenAPI 与 contract-outline  
**Then** `BatchGenerateRequest.originalBatchId` / `BatchResult.originalBatchId` 描述含：可选；出现时须存在且属同凭证；失败 `ORIGINAL_BATCH_NOT_FOUND`  
**And** `ErrorCode` 枚举含 `ORIGINAL_BATCH_NOT_FOUND`  
**And** 错误表含 HTTP 404 映射与 `api.error.batch.originalBatchNotFound`  
**And** 需求/PRD/domain/permission 中重试血缘条款与本规格一致（见 §12）

#### BDD-CE-C05-011 — 非 ROOT 标识不可用作原批次

**Given** 某 `batchExternalId` 仅存在于 `BATCH_ITEM`（或非 ROOT）记录，无同凭证 `BATCH_ROOT`  
**When** 以其作为 `originalBatchId` 提交  
**Then** `404 ORIGINAL_BATCH_NOT_FOUND`  
**And** 不创建新批次

#### BDD-CE-C05-012 — 明确非目标不回归扩大

**Given** 本片范围 S-11  
**When** 交付完成  
**Then** **不**因「items 非原失败子集」而新增硬拒绝（可文档化调用方责任）  
**And** **不**改管理 UI / 不要求 Playwright E2E  
**And** **不**实现 CE-C06 / 不触碰 K06b·U06·U13  
**And** **不**宣称 go-live

---

## 10. Boundary / exception 摘要

| 边界 | 行为 |
| --- | --- |
| 省略 / JSON null | 无血缘；普通批次 |
| `""` / 坏 pattern | 400 `REQUEST_BODY_INVALID` |
| 不存在 / 他凭证 / 非 ROOT / 已清理不可见 | 404 `ORIGINAL_BATCH_NOT_FOUND`（统一） |
| 原批次仍 PROCESSING | 若已有 `BATCH_ROOT` 行且同凭证 → **允许**关联（本片不要求原批次终态）；调用方宜等部分成功后再重试（指导性，非硬门槛） |
| 与原批次相同 `idempotencyKey` | 既有幂等冲突/重放规则优先；血缘不取代幂等 |
| 授权失败（无模板权限等） | 既有 401/403；本片不放宽 |
| 敏感数据 | 审计仅摘要；无密码/变量明文 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 后端单测 / MockMvc / WebTestClient | 001–009、011：校验、回显、404/400、原批次不变、审计键 |
| `InvocationParameterSanitizer`（或等价）单测 | 写入/省略 `originalBatchId` |
| 契约测试 | OpenAPI `ErrorCode` + schema 描述；DTO/view 含响应字段 |
| `mvn -B -ntp -f backend/pom.xml verify` | 门禁全绿 |
| 文档 | 本文件；contract-outline；openapi；requirements / PRD / domain / permission；docs/README 索引 |

本片为 **runtime API / 契约**行为，无管理 UI 用户旅程 → Playwright E2E/UIUX **not-applicable**（除非实现意外改 FE）。

---

## 12. Traceability

| 工件 | 路径 / ID |
| --- | --- |
| 计划 | `docs/plan/core-excellence-program-2026-07.md` §5 CE-C05 |
| 行为规格 | `docs/behavior/ce-c05-original-batch-id.md`（本文件） |
| OpenAPI | `docs/api/openapi-v1.yaml` — `BatchGenerateRequest.originalBatchId`、`BatchResult.originalBatchId`、`ErrorCode` |
| 契约说明 | `docs/api/contract-outline.md` — 批量幂等 / 审计 / 错误表 |
| 需求 / PRD / 域 / 权限 | `requirements-plan.md`、`PRD.md`、`domain-model.md`、`permission-matrix.md` |
| ADR | ADR-0004（产品规则已 Accepted；本片不改决策正文） |
| 前序 | CE-C01/C02（字段绑定）；CE-C03（警告形态）；CE-C04（正交） |
| Task Master | **#70** |
| 后续 | CE-C06 DOCX permissions；CE-G03 |

---

## 13. 开放问题（不阻塞 `ready`；实现默认如下）

| ID | 问题 | 默认（可被用户推翻） | 阻塞？ |
| --- | --- | --- | --- |
| **Q1** | 他凭证批次用 403 还是与「不存在」统一 404？ | **统一 404 `ORIGINAL_BATCH_NOT_FOUND`**（防探测） | 否 |
| **Q2** | 是否强制同模板 / 同环境？ | **否**（同凭证 + batchId 足够） | 否 |
| **Q3** | 是否强制 items ⊆ 原失败项？ | **否**（本片非目标；契约可写调用方责任） | 否 |
| **Q4** | 持久化用专用列还是 parameters JSON？ | **二选一即可**；必须可审计读取 | 否 |
| **Q5** | 原批次仍 PROCESSING 是否拒绝？ | **不拒绝**（仅要求 ROOT 可见） | 否 |

若用户明确推翻 Q1–Q5 默认，再修订本规格后进入实现。

---

## 14. BDD readiness

```
bdd_readiness: ready
owning_doc: docs/behavior/ce-c05-original-batch-id.md
task_ids: ["#70", "CE-C05"]
open_questions: [Q1 opaque 404, Q2 same-template, Q3 item-subset, Q4 storage shape, Q5 in-flight original]
next: backend-engineer (TDD Red on BDD-CE-C05-001…012)
```

**Handoff note:** 实现前先写失败测试覆盖 005/006/007 与 002/004；再最小实现校验 + 响应字段 + sanitizer/审计写入 + OpenAPI ErrorCode。
