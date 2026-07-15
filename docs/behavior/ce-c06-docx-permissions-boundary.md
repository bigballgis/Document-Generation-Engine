# BDD 行为规格：CE-C06 — DOCX permissions 边界声明

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-15  
**BDD ID 前缀**: `BDD-CE-C06`  
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) Wave CE-C · CE-C06  
**Slice**: `ce-c06-docx-permissions-boundary`  
**Worktree**: `D:/working/DGE-ce-c06-docx-permissions-boundary` · `feat/ce-c06-docx-permissions-boundary`  
**Task Master**: **#71**  
**Formal phase**: **None**  
**完成声明约束**: 本切片关闭契约与运行时校验对「`permissions` 仅 PDF 生效」的诚实边界；DOCX + 非空 `permissions` **警告、不硬失败**（结构校验仍适用）；**不**实现 Apache POI DOCX 写保护；**不**宣称 go-live；**不**改管理 UI（无既有权限配置面变更时 E2E/UIUX **not-applicable**）。

---

## 1. 概述

| 行为域 | 摘要 |
| --- | --- |
| **契约诚实** | OpenAPI / contract-outline / requirements / PRD / domain / permission-matrix 明确：`encryption.permissions` **仅对 `output.format=PDF` 映射并生效**；DOCX（及任何非 PDF）不映射权限位 |
| **运行时校验** | 请求 `output.format=DOCX`（或非 PDF）且 `encryption.enabled=true` 且 `permissions` **非空**时：在既有加密参数结构校验通过后，**成功继续生成**，并发出成功路径警告（见 W-1） |
| **PDF 路径** | PDF + 合法 `permissions`：继续按既有 `PdfEncryptionService` 映射权限；**不**因本片新增警告 |
| **DOCX 加密本体** | DOCX 仍支持 `openPassword` 动态加密（ADR-0001）；本片只收紧 **permissions 映射边界**，不取消 DOCX 口令加密 |
| **明确非目标** | Apache POI DOCX write-protect / 文档保护（长期另立片）；前端管理 UI；硬失败拒绝「DOCX+permissions」 |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| SoT 仍写「按 DOCX/PDF 输出格式映射」 | requirements / PRD / domain / contract-outline / ADR-0001 Decision |
| `DocxEncryptionService` 仅用 `openPassword`，忽略 `permissions` | `backend/.../rendering/DocxEncryptionService.java` |
| `PdfEncryptionService` 映射 `ALLOW_*` → PDFBox `AccessPermission` | `backend/.../rendering/PdfEncryptionService.java` |
| `EncryptionParameterValidator` 对非空 `permissions` 要求 `ownerPassword` + 枚举校验，**不**按 format 区分 | `EncryptionParameterValidator.java` |
| 成功路径警告信道：`fidelityWarnings[]` / SYNC_STREAM 头（CE-C03） | OpenAPI `FidelityWarning` / CE-C03 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **API 调用方** | Runtime caller | 持 API 凭证 + AD Group；提交 generate / batch-generate，可选 `encryption.permissions` |
| **系统** | Runtime API + 加密校验 + 渲染管线 | 结构校验 →（DOCX+permissions 时）发警告 → PDF 映射权限 / DOCX 仅口令加密 |
| **契约消费者** | OpenAPI / 集成方 | 依赖诚实描述：permissions PDF-only；警告码可机器校验 |
| **审计读者** | Audit / admin | 可读加密策略摘要与警告摘要；不得见密码明文 |

---

## 3. Goal

1. 契约与需求文档明确：**`permissions` 仅 PDF 生效**；DOCX 口令加密仍可用，但 **不**声明 DOCX 权限位映射。  
2. DOCX（非 PDF）+ 非空 `permissions` + 其余加密参数合法 → **HTTP 成功路径** + 警告码 **`DOCX_PERMISSIONS_NOT_APPLIED`**（不 400）。  
3. PDF + 合法 `permissions` → 权限映射照旧；无本片新增警告。  
4. 既有硬失败规则不变：`enabled=false` 仍带 permissions、缺 `ownerPassword`、非法枚举、密码强度等 → 仍 `400 ENCRYPTION_PARAMETER_INVALID`。  
5. **不**实现 POI DOCX write-protect。

---

## 4. 已确认决策（confirmed）

### 4.1 产品 / 契约基线（本片确认）

| ID | 决策 |
| --- | --- |
| **P-1** | v1 运行时：`encryption.permissions` **仅**在 `output.format=PDF` 时应用到输出文件的访问权限位 |
| **P-2** | `output.format=DOCX`（及契约仅有的非 PDF 枚举成员）时，非空 `permissions` **不**改变产物权限/写保护状态；DOCX 加密仍仅由 `openPassword`（及既有 DOCX 加密实现）决定 |
| **P-3** | DOCX + 非空 `permissions`：**警告，不硬失败**（产品意图优先于「不支持组合 → 400」的旧笼统表述， insofar as format≠PDF） |
| **P-4** | 警告走 CE-C03 成功路径信道：JSON → `fidelityWarnings[]`；`SYNC_STREAM` → `fidelityWarningCount` / `fidelityWarningCodes` 头；完整对象字段与 CE-C03 一致 |
| **P-5** | 警告码（诚实枚举）：`DOCX_PERMISSIONS_NOT_APPLIED`；`messageKey` = `generation.warning.fidelity.docxPermissionsNotApplied` |
| **P-6** | 结构校验仍优先：传入非空 `permissions` 时仍须 `ownerPassword`；非法枚举 / 密码规则 / `enabled` 不一致仍 `400 ENCRYPTION_PARAMETER_INVALID` — **先**结构失败，**不**用警告替代硬失败 |
| **P-7** | `encryptionSummary.permissions` 仍回显**请求侧**权限摘要（含 DOCX 场景）；调用方须结合警告理解「已请求但未应用到 DOCX」 |
| **P-8** | 批量：仅对 `output.format` 解析为非 PDF 且该项最终加密含非空 `permissions` 的成功项发警告；PDF 项不受影响 |
| **P-9** | 空数组 / 省略 `permissions`：不发本片警告 |

### 4.2 本片范围锁定

| ID | 决策 |
| --- | --- |
| **S-1** | 更新 OpenAPI：`EncryptionOptions` / `permissions` 描述；`FidelityWarningCode` 增加 `DOCX_PERMISSIONS_NOT_APPLIED` |
| **S-2** | 更新 contract-outline、requirements-plan、PRD、domain-model、permission-matrix、governance 中「按 DOCX/PDF 映射」表述为 PDF-only + DOCX 警告 |
| **S-3** | ADR-0001：**不重写 Decision 历史正文**；增加 **Amendment（CE-C06）** 澄清 v1 映射仅 PDF |
| **S-4** | 后端：校验/管线在 DOCX+permissions 成功路径发出警告；PDF 路径回归不变 |
| **S-5** | 明确非目标：POI DOCX write-protect；管理 UI / Playwright E2E（无 UI 变更）；CE-C07+；改写 ADR Decision 原句为「已映射 DOCX」；go-live / CD-3 |

### 4.3 与前序 CE-C 边界

| ID | 决策 |
| --- | --- |
| **B-1** | 复用 CE-C03 `FidelityWarning` 形态；本片只**扩展**诚实枚举一码 |
| **B-2** | 不回退 CE-C01/C02 严格请求体 |
| **B-3** | 与 CE-C04/C05 正交 |

---

## 5. Trigger

| # | 触发 |
| --- | --- |
| T1 | 单笔/批量生成，`output.format=DOCX`，`encryption.enabled=true`，非空 `permissions`（及合法 `ownerPassword`/`openPassword`） |
| T2 | 同上但 `output.format=PDF` |
| T3 | DOCX 加密启用但**省略**或空 `permissions` |
| T4 | DOCX + permissions 但缺 `ownerPassword` / 非法枚举 / `enabled=false` 仍带字段 |
| T5 | `SYNC_STREAM` DOCX + permissions 成功 |

---

## 6. Preconditions

| # | 前置 |
| --- | --- |
| P1 | 调用方凭证有效；AD Group + 模板授权通过 |
| P2 | API policy 允许对应格式的动态加密（DOCX → `docxEncryptionEnabled`；PDF → `pdfEncryptionEnabled`） |
| P3 | 输出格式/模式被策略允许 |
| P4 | CE-C03 警告承载形态已在线 |

---

## 7. Primary journey（成功 — DOCX + permissions 警告）

1. 调用方提交生成：`format=DOCX`，`encryption.enabled=true`，合法 `openPassword` + `ownerPassword`，`permissions: ["ALLOW_PRINT", …]`。  
2. 系统通过结构校验与策略校验（不因 format=DOCX 拒绝 permissions）。  
3. 系统生成并（若 enabled）对 DOCX 做口令加密；**不**应用 permissions 写保护/权限位。  
4. 系统在成功结果中附加 `DOCX_PERMISSIONS_NOT_APPLIED` 警告（JSON 或流头）。  
5. `encryptionSummary` 反映 enabled、DOCX、密码是否提供、以及请求的 permissions 摘要。

---

## 8. System responses

### 成功（本片）

| 场景 | HTTP / 结果 | 警告 | 产物权限行为 |
| --- | --- | --- | --- |
| DOCX + 非空 permissions（结构合法） | 既有成功语义 | **必须**含 `DOCX_PERMISSIONS_NOT_APPLIED` | 无 permissions 映射；仅 openPassword 加密（若 enabled） |
| PDF + 非空 permissions（结构合法） | 既有成功语义 | **无**本片码 | PDFBox 权限映射照旧 |
| DOCX 无 permissions | 既有成功语义 | **无**本片码 | 口令加密照旧 |

### 失败（既有，本片不放宽）

| 条件 | HTTP | code |
| --- | --- | --- |
| enabled=false/省略仍带 permissions 等 | 400 | `ENCRYPTION_PARAMETER_INVALID` |
| 非空 permissions 缺 ownerPassword | 400 | `ENCRYPTION_PARAMETER_INVALID` |
| 非法 permission 枚举 | 400 | `ENCRYPTION_PARAMETER_INVALID` |
| 密码长度/相同等 | 400 | `ENCRYPTION_PARAMETER_INVALID` |
| 策略不允许加密 | 400 | `ENCRYPTION_PARAMETER_INVALID`（既有 messageKey） |
| 加密处理失败 | 500 | `ENCRYPTION_FAILED` retryable=true |

---

## 9. Acceptance scenarios（Given / When / Then）

#### BDD-CE-C06-001 — 契约声明：permissions 仅 PDF

**Given** 本片交付的 OpenAPI 与 contract-outline  
**When** 阅读 `EncryptionOptions.permissions` / 加密参数确认节  
**Then** 明确写明 permissions **仅对 PDF 输出生效**  
**And** 写明 DOCX + 非空 permissions → 成功路径警告 `DOCX_PERMISSIONS_NOT_APPLIED`，**不** 400（结构合法时）  
**And** 写明 Apache POI DOCX write-protect **不在**本片范围

#### BDD-CE-C06-002 — DOCX + permissions → 成功 + 警告

**Given** 合法单笔请求：`output.format=DOCX`，`encryption.enabled=true`，合法 open/owner 密码，非空 `permissions`  
**And** 策略允许 DOCX 加密  
**When** 调用同步或异步生成（JSON 结果路径）  
**Then** 生成成功（既有成功 HTTP/状态）  
**And** `fidelityWarnings`（或批量项同等字段）含一条 `warningCode=DOCX_PERMISSIONS_NOT_APPLIED`  
**And** `messageKey=generation.warning.fidelity.docxPermissionsNotApplied`  
**And** `sensitiveDataExcluded=true`；警告字段不含密码明文  
**And** 产物为 DOCX 口令加密（若 enabled），**无** permissions 写保护语义

#### BDD-CE-C06-003 — PDF + permissions → 映射生效、无本片警告

**Given** 合法请求：`output.format=PDF`，`encryption.enabled=true`，合法 open/owner，非空 `permissions`（如含 `ALLOW_PRINT`）  
**And** 策略允许 PDF 加密  
**When** 生成成功  
**Then** PDF 权限按既有映射应用  
**And** `fidelityWarnings` **不含** `DOCX_PERMISSIONS_NOT_APPLIED`

#### BDD-CE-C06-004 — DOCX 无 permissions → 无本片警告

**Given** `format=DOCX`，`enabled=true`，仅 `openPassword`（可选无 owner），**无** permissions 或空数组  
**When** 生成成功  
**Then** 不出现 `DOCX_PERMISSIONS_NOT_APPLIED`

#### BDD-CE-C06-005 — SYNC_STREAM 头摘要含本片码

**Given** BDD-CE-C06-002 条件且 `output.mode=SYNC_STREAM`  
**When** 同步流成功  
**Then** 响应头 `fidelityWarningCodes` 含 `DOCX_PERMISSIONS_NOT_APPLIED`  
**And** `fidelityWarningCount` ≥ 1 且与码条数一致（CE-C03）

#### BDD-CE-C06-006 — 结构非法仍 400（不降级为警告）

**Given** `format=DOCX`，`enabled=true`，非空 `permissions`，但缺 `ownerPassword`  
**When** 提交生成  
**Then** HTTP 400，`error.code=ENCRYPTION_PARAMETER_INVALID`  
**And** **不**创建成功产物  
**And** **不以** `DOCX_PERMISSIONS_NOT_APPLIED` 替代该错误

#### BDD-CE-C06-007 — enabled=false 仍带 permissions → 400

**Given** `format=DOCX`，`encryption.enabled=false`（或省略 enabled）仍传入 `permissions`  
**When** 提交生成  
**Then** HTTP 400，`ENCRYPTION_PARAMETER_INVALID`  
**And** 不发成功路径警告

#### BDD-CE-C06-008 — 非法 permission 枚举 → 400

**Given** `permissions` 含 OpenAPI `Permission` 枚举外的值  
**When** 提交生成（任意 format）  
**Then** HTTP 400，`ENCRYPTION_PARAMETER_INVALID`  
**And** 不发 `DOCX_PERMISSIONS_NOT_APPLIED`

#### BDD-CE-C06-009 — 批量混合 format

**Given** 同步或异步批量：一项最终 `DOCX`+非空 permissions，一项 `PDF`+非空 permissions，均结构合法  
**When** 批量成功完成（或异步查询完成）  
**Then** DOCX 成功项含 `DOCX_PERMISSIONS_NOT_APPLIED`  
**And** PDF 成功项不含该码  
**And** 各项 `encryptionSummary` 回显各自请求 permissions 摘要

#### BDD-CE-C06-010 — encryptionSummary 回显与警告并存

**Given** BDD-CE-C06-002 成功  
**When** 检查成功结果中的 `encryptionSummary`  
**Then** `outputFormat=DOCX`，`enabled=true`，`ownerPasswordProvided=true`，`permissions` 为请求摘要  
**And** 同结果仍含 `DOCX_PERMISSIONS_NOT_APPLIED`（摘要 ≠ 已应用到文件）

#### BDD-CE-C06-011 — OpenAPI 诚实枚举

**Given** 本片交付集  
**When** 核对 `docs/api/openapi-v1.yaml`  
**Then** `FidelityWarningCode` 枚举含 `DOCX_PERMISSIONS_NOT_APPLIED`  
**And** `EncryptionOptions` / `permissions` description 含 PDF-only + DOCX 警告语义

#### BDD-CE-C06-012 — 明确非目标不扩大

**Given** 本片范围 S-5  
**When** 交付完成  
**Then** **不**实现 Apache POI DOCX write-protect / 文档保护  
**And** **不**因「DOCX+permissions」新增硬失败（结构合法时）  
**And** **不**要求管理 UI / Playwright E2E（无 UI 变更）  
**And** **不**宣称 go-live

---

## 10. Boundary / exception 摘要

| 边界 | 行为 |
| --- | --- |
| 非空 permissions + DOCX | 警告 + 忽略权限映射 |
| 非空 permissions + PDF | 映射；无本片警告 |
| 空 / 省略 permissions | 无本片警告 |
| 结构非法（缺 owner、坏枚举、enabled 不一致等） | 400；无成功警告 |
| 策略禁止 DOCX/PDF 加密 | 既有 400 |
| 加密处理失败 | 500 `ENCRYPTION_FAILED` |
| 幂等重放 | 重放同等警告形态（CE-C03） |
| 敏感数据 | 警告与审计无密码 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 后端单测 | 002–010：DOCX 警告、PDF 无警告、400 不降级、批量混合、流头 |
| 契约测试 | OpenAPI 枚举 + EncryptionOptions 描述 |
| `mvn -B -ntp -f backend/pom.xml verify` | 门禁全绿 |
| 文档 | 本文件；openapi；contract-outline；requirements / PRD / domain / permission-matrix；ADR-0001 amendment；docs/README 索引 |

本片为 **runtime API / 契约**行为 → Playwright E2E/UIUX **not-applicable**（除非意外改 FE）。

---

## 12. Traceability

| 工件 | 路径 / ID |
| --- | --- |
| 计划 | `docs/plan/core-excellence-program-2026-07.md` §5 CE-C06 |
| 行为规格 | `docs/behavior/ce-c06-docx-permissions-boundary.md`（本文件） |
| OpenAPI | `docs/api/openapi-v1.yaml` — `EncryptionOptions`、`Permission`、`FidelityWarningCode` |
| 契约说明 | `docs/api/contract-outline.md` — 加密参数确认 |
| 需求 / PRD / 域 / 权限 | `requirements-plan.md`、`PRD.md`、`domain-model.md`、`permission-matrix.md` |
| ADR | ADR-0001 Amendment（CE-C06）；Decision 历史不重写 |
| 前序 | CE-C03（警告形态）；CE-C05 Done |
| Task Master | **#71** |
| 后续 | 长期 POI DOCX write-protect（另立片）；CE-G03 等 |

---

## 13. 开放问题（不阻塞 `ready`；实现默认如下）

| ID | 问题 | 默认（可被用户推翻） | 阻塞？ |
| --- | --- | --- | --- |
| **Q1** | 警告走 `fidelityWarnings` 还是独立 encryptionWarnings？ | **`fidelityWarnings` + 新码**（复用 CE-C03） | 否 |
| **Q2** | DOCX+permissions 时是否仍强制 ownerPassword？ | **是**（保持 ADR 结构规则；警告不替代结构校验） | 否 |
| **Q3** | `encryptionSummary.permissions` 在 DOCX 是否清空？ | **不清空**；回显请求摘要 + 警告说明未应用 | 否 |

若用户明确推翻 Q1–Q3 默认，再修订本规格后进入实现。

---

## 14. BDD readiness

```
bdd_readiness: ready
owning_doc: docs/behavior/ce-c06-docx-permissions-boundary.md
task_ids: ["#71", "CE-C06"]
open_questions: [Q1 fidelity channel, Q2 ownerPassword still required, Q3 summary echo]
next: plan-orchestrator → backend-engineer (TDD Red on BDD-CE-C06-001…012)
```

**Handoff note:** 实现先 Red：DOCX+permissions 成功断言警告码；PDF 回归无该码；缺 ownerPassword 仍 400；OpenAPI 枚举与描述；**禁止**引入 POI write-protect。
