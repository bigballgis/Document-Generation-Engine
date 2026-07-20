# BDD 行为规格：PD-6 — True non-SPECIMEN re-issue（受控生产重发）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-PD6` |
| **编写日期** | 2026-07-20 |
| **程序 / 队列** | IBL §8 **PD-6**（dedicated re-issue leaf；**OUT of** Wave E / IBL-E1） |
| **Slice** | `pd6-true-non-specimen-reissue` |
| **Branch** | `feat/pd6-true-non-specimen-reissue` |
| **Worktree** | `D:/working/DGE-pd6-true-non-specimen-reissue` |
| **Base** | `origin/main`（handoff） |
| **Placement** | ISOLATED |
| **Task Master** | **#138** → **`in-progress`**（sole-active；OUT of IBL-E；deps `#76` CE-G06 / `#112` IBL-A6） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: pd6-true-non-specimen-reissue`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) §8 **PD-6** / F6；再生基线 [ce-g06-audit-reproducible.md](./ce-g06-audit-reproducible.md)；水印 [ce-g02-specimen-watermark.md](./ce-g02-specimen-watermark.md)；locale 重放 [ibl-a6-regenerate-locale-replay.md](./ibl-a6-regenerate-locale-replay.md)；权限 [permission-matrix.md](../security/permission-matrix.md) §7 / CE-G06 条 |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（API-first；沿用 G06-C17 — 无管理端再生 CTA / E2E/UIUX） |

**完成声明约束：** 本叶交付 **受控、显式 opt-in** 的生产重发路径：授权管理员在 regenerate API 上选择生产重发模式时，产物 **不**施加 SPECIMEN。默认 regenerate（无 opt-in）以及 preview / test-generate **必须继续**强制 SPECIMEN。**禁止**据此翻转 checklist **#3b** / **#5a**；**禁止**宣称 go-live / IBL program Done；**禁止**为 **#119** 发明 Word host 证据；**禁止**把 PD-7 / CE-O02 / CD-3 并入本叶。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: []   # plan-orchestrator registers after BDD
  proposed_slice_id: pd6-true-non-specimen-reissue
  shared_acceptance_surface: >
    Management regenerate / invocation re-issue watermark policy —
    default SPECIMEN retained; explicit production re-issue omits SPECIMEN
  vetoes_applied: [B2-PD7, B3-CE-O02, B4-CD3, RTL, "#119"]
  evidence_amortization: verify+deploy
```

| IN（本叶） | OUT（明确禁止） |
| --- | --- |
| 扩展 CE-G06 regenerate：显式 `productionReissue` opt-in + 强制 `reason` | 默认 regenerate 去水印；preview / test-generate 去水印 |
| 授权 fail-closed：仅 `GLOBAL_ADMIN` / 同组 `GROUP_ADMIN` 可生产重发 | 放宽 `TEMPLATE_*` / 调用方 / 无 reason 去水印 |
| 生产重发成功：DOCX/PDF **不含** `SPECIMEN`；响应 `specimen=false` | 改变正式 runtime generate（已无水印）语义 |
| 默认 regenerate / 审计样件路径：仍强制 SPECIMEN（G06 + G02） | FE 再生按钮 / Playwright E2E/UIUX |
| 审计记录 `productionReissue` + `reason` + `specimen` | 翻转 #3b/#5a；#119 Word 证据；PD-7 字体；go-live |
| Gates：`mvn verify` + 管线 queued Docker | 新 capability bit（除非实现时矩阵行文案同步） |

---

## 1. 概述

### 1.1 问题（产品意图 — Confirmed 2026-07-19）

| 事实 | 来源 |
| --- | --- |
| PD-6「True non-specimen re-issue」为**已确认产品意图** | IBL plan §8；用户确认 2026-07-19 |
| 实现 **OUT of IBL-E1 / Wave E**；直至本 dedicated leaf 落地前，regenerate **必须**保持 SPECIMEN | A6-C7；IBL plan PD-6 行 |
| CE-G06 受控再生始终 `specimen=true`；装配强制 `DocxSpecimenWatermarkStamper` / `PdfSpecimenWatermarkStamper` | `InvocationRegenerationService` / `InvocationRegenerationAssemblySupport.assembleSpecimen` |
| Preview / test-generate 必须保持 SPECIMEN（CE-G02） | [ce-g02-specimen-watermark.md](./ce-g02-specimen-watermark.md) |
| 正式 runtime 路径本就无 SPECIMEN — **不在本叶改动范围** | G02-C5 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **PD6-S1 Default regenerate** | 无 opt-in（或 `productionReissue=false`）→ **完全保持** CE-G06：强制 SPECIMEN；权限 = G06-C8（含 `AUDIT_ADMIN`） |
| **PD6-S2 Production re-issue** | 显式 `productionReissue=true` + 非空 `reason` + 收窄角色 → 装配**跳过** SPECIMEN stamper；产物可作受控生产重发件（非审计样件） |
| **PD6-S3 Preview/test isolation** | Preview / test-generate / batch-test **零改动**：仍强制 SPECIMEN |
| **PD6-S4 Audit honesty** | 成功/失败终态审计可区分样件再生 vs 生产重发；含 reason；仍禁 variables |
| **PD6-S5 Locale / PII / drift** | 继续消费 IBL-A6 locale 重放、IBL-A5 脱敏重放、G06 指纹/过期/drift 闸门 — 本叶不放宽 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **GLOBAL_ADMIN** | 全局管理员 | 默认可 SPECIMEN regenerate；**可**显式生产重发（需 reason） |
| **GROUP_ADMIN** | 分组管理员（模板组范围内） | 同上（组范围） |
| **AUDIT_ADMIN** | 审计管理员 | **仅**默认 SPECIMEN regenerate（合规样件）；**禁止** `productionReissue=true`（403） |
| **TEMPLATE_AUTHOR / TESTER / APPROVER / MASTER_DESIGNER / Runtime 调用方** | — | **禁止**任何 regenerate（既有 G06 403） |
| **系统（regenerate）** | `InvocationRegenerationService` + assembly | 按 mode 决定是否 stamp；写元数据/审计 |
| **（间接）法务 / 内控** | 受益方 | 默认路径不可误作正式函；生产重发可审计追责 |

---

## 3. Goal

1. **默认**管理端 regenerate 成功件 **仍含** SPECIMEN（DOCX 眉脚 + PDF 对角可观测），与 CE-G06 / IBL-A6 回归一致。  
2. 授权的 `GLOBAL_ADMIN` / `GROUP_ADMIN` 可通过 **显式 opt-in** 触发生产重发：成功件 **不含** `SPECIMEN`，响应 `specimen=false`。  
3. Opt-in **fail-closed**：缺 flag、缺/空白 reason、或角色不足 → **不得**产出无水印成功件。  
4. Preview / test-generate **不得**因本叶失去 SPECIMEN。  
5. 正式 runtime generate **零行为改动目标**。  
6. Formal phase **None**；不翻转 #3b/#5a；不宣称 go-live。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实 + fail-closed 产品裁决 — 无需再阻塞提问）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **PD6-C1** | **同一 API 入口：** 仍使用 `POST /api/management/v1/templates/{templateId}/api/invocations/{invocationId}/regenerate`。不新建平行「formal reissue」端点（缩小表面积）。 | CE-G06 既有面；API-first |
| **PD6-C2** | **请求体扩展（OpenAPI 同步义务）：**  
`{ "outputFormat"?: "DOCX"\|"PDF", "productionReissue"?: boolean, "reason"?: string }`  
缺省 / `productionReissue` 缺省或 `false` → **审计样件模式**（SPECIMEN）。 | fail-closed explicit opt-in（handoff） |
| **PD6-C3** | **生产重发前置：** `productionReissue === true` **且** `reason` trim 后非空（建议 max **500** 字符；超长 → 400 validation）。缺 reason → **400** `error.code=PRODUCTION_REISSUE_REASON_REQUIRED`（或等价 VALIDATION），`messageKey=api.error.audit.productionReissueReasonRequired`，`retryable=false`。 | 审计追责；对齐 G01 例外 reason 模式 |
| **PD6-C4** | **角色收窄（生产重发）：** 仅 `GLOBAL_ADMIN` 与同组可见范围内的 `GROUP_ADMIN`。`AUDIT_ADMIN` 调用 `productionReissue=true` → **403** fail-closed（可继续做默认 SPECIMEN regenerate）。无新 capability bit。 | 生产件 ≠ 审计样件；比 G06-C8 更严 |
| **PD6-C5** | **默认模式角色：** 无 opt-in 时权限 **不变** = G06-C8（`GLOBAL_ADMIN` / `GROUP_ADMIN` / `AUDIT_ADMIN`）。 | 不破坏既有审计再生 |
| **PD6-C6** | **装配：** 生产重发路径复用钉扎母版 + locale 重放 + PII 重放规则，但 **不调用** `DocxSpecimenWatermarkStamper` / `PdfSpecimenWatermarkStamper`。审计样件路径继续 `assembleSpecimen`（或等价强制 stamp）。 | PD-6 意图 |
| **PD6-C7** | **可观测：** 生产重发成功 → DOCX OOXML 与 PDF 文本抽取 **均不含** 字面量 `SPECIMEN`；响应 `specimen=false`；持久化 regeneration 行 `specimen=false`。 | 对照 G06-C9/C13 |
| **PD6-C8** | **审计：** `INVOCATION_REGENERATED`（或同事件类型扩展字段）必须可区分模式：至少记录 `productionReissue`（bool）、`specimen`（bool）、`reason`（生产重发时非空；样件模式可空）、既有 source/regeneration/outcome/actor。**仍禁止** variables / 密码。失败终态同样写审计（含尝试的 productionReissue + reason 若有）。 | G06-C15 扩展 |
| **PD6-C9** | **存储前缀：** 继续 `regenerations/{regenerationId}/…`（与 G06-C13 一致）；靠元数据 `specimen` 区分，不强制新桶。 | 最小改动 |
| **PD6-C10** | **加密：** 生产重发与样件再生相同 — 原加密件 **不**重加密（G06-C10）；`encryptionReapplied=false`。 | 密码不落库 |
| **PD6-C11** | **指纹 / 过期 / kind / drift / 钉扎不可用：** 与 CE-G06 相同闸门（410/409/422/403/404 惯例）。生产重发 **不**放宽。 | G06-C11…C19 |
| **PD6-C12** | **不新建 runtime SUCCESS invocation**；不消耗调用方幂等键（G06-C14）。 | 审计边界 |
| **PD6-C13** | **Preview / test-generate：** **零改动**；继续强制 SPECIMEN（G02）。禁止把 `productionReissue` 接到预览 API。 | handoff 硬约束 |
| **PD6-C14** | **Runtime formal generate：** 本叶不改；继续无 SPECIMEN。 | G02-C5 |
| **PD6-C15** | **Idempotency：** 与 G06-C18 — 每次调用可新 `regenerationId`。 | 最小集 |
| **PD6-C16** | **FE：** `frontend_ui_in_scope=false`。无再生 CTA、无 reason 表单、无 E2E/UIUX 义务。OpenAPI 字段若触发 FE 类型生成，仅机械同步。 | G06-C17；handoff |
| **PD6-C17** | **完成边界：** 本叶 Done ≠ go-live；**不得**翻转 #3b/#5a；**不得**关闭 #119；**不得**宣称 IBL program Done。 | IBL plan 护栏 |
| **PD6-C18** | **错误码稳定性：**  
- 生产重发无权限 → 既有 403 `api.error.authorization.forbidden`（或管理端等价）  
- reason 缺失 → `PRODUCTION_REISSUE_REASON_REQUIRED`  
- 样件路径水印失败 → 既有 `SPECIMEN_WATERMARK_FAILED`  
生产重发路径 **不应** 因「未 stamp」触发 `SPECIMEN_WATERMARK_FAILED`。 | fail-closed + 契约诚实 |
| **PD6-C19** | **权限矩阵 / OpenAPI / contract-outline / i18n messageKeys：** 本叶实现时由 doc-keeper 或实现者在同变更集同步（矩阵新增「按 invocation 生产重发（无 SPECIMEN）」行；说明 opt-in + reason + 角色收窄）。行为 SoT = 本文件。**docs-first（2026-07-20）：** permission-matrix §7/§ CE-G06+PD-6、OpenAPI `ManagementInvocationRegenerateRequest`、contract-outline、api README、requirements/PRD、CE-G06 下游指针已同步；`messages_en.properties` 键 `api.error.audit.productionReissueReasonRequired` 由 backend-engineer 落地。 | document-as-code |
| **PD6-C20** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；行为变更验收面 → Stage 5/10 queued Docker；architecture review。FE gates 仅机械类型同步时 as applicable。 | delivery constitution |

### 4.2 已确认（上游交付，本叶只消费）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **PD6-U1** | 受控 regenerate API + 指纹 + SPECIMEN 样件路径 | CE-G06 |
| **PD6-U2** | DOCX 眉脚 + PDF 对角 `SPECIMEN` stamper | CE-G02 |
| **PD6-U3** | Locale 从 `contextSummary.locale` 重放 | IBL-A6 |
| **PD6-U4** | PII 脱敏后重放 | IBL-A5 / ADR-0057 |
| **PD6-U5** | PD-6 产品意图 Confirmed 2026-07-19 | IBL plan §8 |

### 4.3 非确认假设（不得升格为需求）

| ID | 陈述 | 状态 |
| --- | --- | --- |
| **PD6-N1** | 默认 regenerate 去水印 | **明确拒绝** |
| **PD6-N2** | Preview / test 去水印 | **明确拒绝** |
| **PD6-N3** | `AUDIT_ADMIN` 可生产重发 | **明确拒绝**（本叶 PD6-C4） |
| **PD6-N4** | 四眼双人核准 / 工单系统联动 | **非确认** — 本叶不做；reason 文本审计即可 |
| **PD6-N5** | FE 再生 UI / E2E | **非确认** — OOS |
| **PD6-N6** | 翻转 #3b / #5a / go-live / #119 | **明确拒绝** |
| **PD6-N7** | 调用方 runtime 自助「正式重打」 | **非确认** — OOS（仍仅管理端 regenerate） |
| **PD6-N8** | 新独立 capability bit / 新角色 | **非确认** — 本叶用角色收窄 + opt-in |

### 4.4 非阻塞备注（实现选型，不挡 BDD ready）

| # | 备注 | 默认 |
| --- | --- | --- |
| R1 | 装配方法是拆 `assembleProductionReissue` 还是 `assembleSpecimen(..., applyWatermark)` | 实现选型；须可测且默认 stamp |
| R2 | 审计是扩展 `INVOCATION_REGENERATED` 字段还是并行事件类型 | **优先扩展同事件** + 字段；避免双事件分叉 |
| R3 | `reason` 是否写入 regeneration 实体表列 | **建议是**（可查询）；至少审计必有 |

---

## 5. Trigger / Preconditions

**Trigger：** 授权管理员调用  
`POST /api/management/v1/templates/{templateId}/api/invocations/{invocationId}/regenerate`  
body 含或不含 `productionReissue` / `reason` / `outputFormat`。

**Preconditions（任一成功 regenerate）：**

- 模板可见；invocation 归属该模板；kind ∈ {`SINGLE`,`BATCH_ITEM`,`ASYNC_TASK`}；未过期；指纹非空；钉扎 hash 匹配。  
- 样件模式：角色满足 G06-C8。  
- 生产重发模式：角色满足 PD6-C4；`productionReissue=true`；`reason` 非空。

---

## 6. Primary journey

### 6.1 默认审计样件（回归 — 不得退化）

1. `AUDIT_ADMIN`（或 GLOBAL/GROUP）对合格 invocation 调用 regenerate（无 `productionReissue`）。  
2. 系统：权限 → 闸门 → locale/PII 重放 → 钉扎装配 → **SPECIMEN** → 存储 → 审计。  
3. 下载件含 `SPECIMEN`；响应 `specimen=true`。

### 6.2 生产重发（本叶新增）

1. `GLOBAL_ADMIN`（或同组 `GROUP_ADMIN`）调用 regenerate，body：  
   `{ "productionReissue": true, "reason": "Customer reprint after courier loss — ticket T-123", "outputFormat": "PDF" }`。  
2. 系统：收窄角色校验 → reason 校验 → 既有闸门 → locale/PII 重放 → 钉扎装配 → **跳过** SPECIMEN → 存储 → 审计（含 reason、`specimen=false`）。  
3. 下载件 **不含** `SPECIMEN`；响应 `specimen=false`。

---

## 7. System responses

| 路径 | 响应 |
| --- | --- |
| 样件 regenerate 成功 | 200；`specimen=true`；制品含 SPECIMEN；审计可观察样件模式 |
| 生产重发成功 | 200；`specimen=false`；制品不含 SPECIMEN；审计含 reason + productionReissue |
| `productionReissue=true` 无/空白 reason | 400 `PRODUCTION_REISSUE_REASON_REQUIRED`；无成功制品 |
| `AUDIT_ADMIN` + `productionReissue=true` | 403；无成功制品 |
| 无 regenerate 权限 | 403（既有） |
| 过期 / 无指纹 / drift / kind | 既有 G06 错误 |
| 样件路径水印失败 | `SPECIMEN_WATERMARK_FAILED`；无无水印成功件 |
| 管理端/审计 | 无 variables 明文 |

---

## 8. Acceptance scenarios（Given / When / Then）

### A. 默认 SPECIMEN 护栏（不得因本叶退化）

#### BDD-PD6-001 — 默认 regenerate 仍强制 SPECIMEN

**Given** 合格 SINGLE invocation 与钉扎母版 hash 匹配  
**And** GLOBAL_ADMIN 调用 regenerate **不**传 `productionReissue`（或传 `false`）  
**When** 下载再生 DOCX 与/或 PDF  
**Then** DOCX 眉脚与/或 PDF 文本含字面量 `SPECIMEN`  
**And** 响应 `specimen=true`

#### BDD-PD6-002 — AUDIT_ADMIN 默认可再生样件

**Given** AUDIT_ADMIN 对模板可见  
**When** 调用 regenerate（无 productionReissue）  
**Then** 在其它闸门通过时允许  
**And** 制品含 SPECIMEN

#### BDD-PD6-003 — Preview / test-generate 仍强制 SPECIMEN

**Given** 本叶变更已合并语义  
**When** 授权用户对模板 `POST …/previews/test-generate` 成功  
**Then** 预览 DOCX/PDF 仍含 `SPECIMEN`  
**And** 本叶未将 `productionReissue` 接到预览 API

#### BDD-PD6-004 — Runtime formal 仍无 SPECIMEN（回归）

**Given** 同一模板正式 runtime sync 生成  
**When** 检查正式产物  
**Then** 不含 SPECIMEN（与本叶前一致；本叶未向 formal 注入水印）

---

### B. 生产重发成功路径

#### BDD-PD6-005 — GLOBAL_ADMIN 显式生产重发：无 SPECIMEN

**Given** 合格 invocation；GLOBAL_ADMIN  
**When** regenerate body `{ "productionReissue": true, "reason": "Reprint for customer — case 42", "outputFormat": "PDF" }`  
**Then** HTTP 200；`specimen=false`  
**And** PDF 文本抽取 **不含** `SPECIMEN`  
**And** 响应不含 variables

#### BDD-PD6-006 — GROUP_ADMIN 组内生产重发 DOCX 无眉脚 SPECIMEN

**Given** invocation 属组 A；组 A 的 GROUP_ADMIN  
**When** regenerate `{ "productionReissue": true, "reason": "Branch reprint", "outputFormat": "DOCX" }`  
**Then** DOCX header/footer **均不含** 字面量 `SPECIMEN`  
**And** `specimen=false`

#### BDD-PD6-007 — 生产重发仍重放 locale（A6 不回归）

**Given** invocation 留存 `contextSummary.locale=en-US`；模板含 locale 敏感 FORMAT_AMOUNT  
**When** 生产重发成功  
**Then** compute/制品 locale 方向与 A6 一致（非静默 zh-CN）  
**And** 制品仍无 SPECIMEN

#### BDD-PD6-008 — 生产重发审计含 reason 与 specimen=false

**Given** BDD-PD6-005 成功  
**When** 查询管理审计 `INVOCATION_REGENERATED`  
**Then** 摘要含 `productionReissue=true`（或等价）、`specimen=false`、非空 `reason`、actor、outcome=SUCCESS  
**And** **无** variables 明文

---

### C. Fail-closed 授权与校验

#### BDD-PD6-009 — 生产重发缺 reason → 400

**Given** GLOBAL_ADMIN  
**When** regenerate `{ "productionReissue": true }` 或 `reason: "   "`  
**Then** 400 `PRODUCTION_REISSUE_REASON_REQUIRED`  
**And** 无 regeneration 成功制品

#### BDD-PD6-010 — AUDIT_ADMIN 禁止生产重发

**Given** AUDIT_ADMIN  
**When** regenerate `{ "productionReissue": true, "reason": "need clean copy" }`  
**Then** 403 fail-closed  
**And** 无无水印成功件  
**And** 审计 outcome=FAILURE（若写终态审计）

#### BDD-PD6-011 — TEMPLATE_AUTHOR 仍禁止任何 regenerate

**Given** TEMPLATE_AUTHOR  
**When** regenerate（样件或生产重发）  
**Then** 403  
**And** 无制品

#### BDD-PD6-012 — 跨组 GROUP_ADMIN 禁止生产重发

**Given** 组 B 的 GROUP_ADMIN；invocation 属组 A  
**When** 生产重发  
**Then** 403/404 fail-closed（不泄露存在性细节，对齐 G06）

---

### D. 既有闸门与边界

#### BDD-PD6-013 — 过期 / drift / 无指纹 对生产重发同样 fail-closed

**Given** 过期或 hash mismatch 或指纹缺失的 invocation  
**When** 生产重发  
**Then** 既有 410 / 409 / 冲突类错误保持  
**And** 不因 productionReissue 放宽

#### BDD-PD6-014 — 样件路径水印失败仍 fail-closed

**Given** 默认 regenerate；stamper 抛错（测试替身）  
**When** regenerate  
**Then** `SPECIMEN_WATERMARK_FAILED`（或既有映射）  
**And** 无「无水印成功件」

#### BDD-PD6-015 — 生产重发不写 runtime SUCCESS invocation

**Given** 生产重发成功  
**When** 按原 credential 查 runtime invocations  
**Then** 无新增冒充 SUCCESS 行  
**And** 原 invocation outcome 不被改写

#### BDD-PD6-016 — 原加密件生产重发仍不加密

**Given** 原请求 encryption.enabled=true（密码已 strip）  
**When** 生产重发成功  
**Then** 再生件未加密；`encryptionReapplied=false`  
**And** 无 SPECIMEN

---

### E. 非目标护栏

#### BDD-PD6-017 — 不翻转 #3b/#5a；不发明 #119 Word 证据

**Given** 本叶变更集与计划文档  
**When** 审查完成声明  
**Then** checklist #3b/#5a **不**变为 GO  
**And** **无**为 #119 新增的 Word-host 测量基线/证据宣称  
**And** **无** go-live / IBL program Done 宣称

#### BDD-PD6-018 — FE 再生 UI out of scope

**Given** 本叶范围  
**When** 验收 Done  
**Then** 无管理 UI 再生/生产重发 CTA  
**And** 无 Playwright E2E/UIUX 强制项  
**And** `frontend_ui_in_scope=false`

---

## 9. Boundary / exception 汇总

| 场景 | 期望 |
| --- | --- |
| 默认 regenerate | SPECIMEN 强制；G06 角色 |
| 生产重发 | 无 SPECIMEN；GLOBAL/GROUP + reason |
| AUDIT_ADMIN + opt-in | 403 |
| 缺 reason | 400 |
| Preview/test | SPECIMEN 不变 |
| Runtime formal | 无水印；本叶不改 |
| 水印失败（样件） | fail-closed |
| variables 暴露 | 禁止 |
| #3b/#5a/#119/go-live | 禁止翻转/发明 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | regenerate 200 + `specimen` 布尔；错误码/messageKey |
| Artifact | 样件含 SPECIMEN；生产重发不含（OOXML/PDF 文本） |
| DB / 元数据 | regeneration 行 `specimen`；建议存 `reason` |
| Audit | `INVOCATION_REGENERATED` 含 mode/reason/specimen |
| Preview 回归 | test-generate 仍含 SPECIMEN |
| Gates | `mvn verify` GREEN；queued deploy 按管线 |
| Docs | 本文件；permission-matrix / OpenAPI 同步义务 |
| 非证据 | 像素比对；FE E2E；Word #119；LRP GO |

---

## 11. Traceability

| 项 | 引用 |
| --- | --- |
| Plan | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) · F6 · **PD-6** Confirmed 2026-07-19 |
| Related BDD | [ce-g06-audit-reproducible.md](./ce-g06-audit-reproducible.md)；[ce-g02-specimen-watermark.md](./ce-g02-specimen-watermark.md)；[ibl-a6-regenerate-locale-replay.md](./ibl-a6-regenerate-locale-replay.md) |
| Permissions | [permission-matrix.md](../security/permission-matrix.md) §7 regenerate 行 + CE-G06 条 → 本叶扩展生产重发行 |
| Code anchors | `ApiManagementInvocationController` regenerate；`ManagementInvocationRegenerateRequest` / `View`；`InvocationRegenerationService`；`InvocationRegenerationAssemblySupport.assembleSpecimen`；`DocxSpecimenWatermarkStamper` / `PdfSpecimenWatermarkStamper`；`GroupAccessService.canRegenerateInvocation` |
| Retention | [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md) |

---

## 12. Out of scope（本叶）

- Preview / test-generate 去水印。  
- 默认 regenerate 去水印。  
- FE 再生 / 生产重发 UI、E2E/UIUX。  
- 调用方 runtime 自助正式重打。  
- 四眼核准工作流、工单系统集成。  
- PD-7 字体、CE-O02、CD-3、#119 Word 证据。  
- 翻转 #3b/#5a；go-live；宣称 IBL program Done。

---

## 13. FE management UI recommendation

| 项 | 本叶 | 理由 |
| --- | --- | --- |
| **Regenerate / production re-issue CTA** | **No** | G06 API-first；高敏动作宜 API + 审计先行 |
| **Reason 输入 UI** | **No** | 同左；客户端可用 API 工具传 reason |
| **E2E / UIUX** | **not-applicable** | `frontend_ui_in_scope=false` |

---

## 14. Ready-for-implementation handoff

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/pd6-true-non-specimen-reissue.md
task_ids: []   # plan-orchestrator to register
plan_id: PD-6
slice_id: pd6-true-non-specimen-reissue
frontend_ui_in_scope: false
acceptance_scenario_ids:
  - BDD-PD6-001
  - BDD-PD6-002
  - BDD-PD6-003
  - BDD-PD6-004
  - BDD-PD6-005
  - BDD-PD6-006
  - BDD-PD6-007
  - BDD-PD6-008
  - BDD-PD6-009
  - BDD-PD6-010
  - BDD-PD6-011
  - BDD-PD6-012
  - BDD-PD6-013
  - BDD-PD6-014
  - BDD-PD6-015
  - BDD-PD6-016
  - BDD-PD6-017
  - BDD-PD6-018
next_stage: plan-orchestrator (stage 2) → doc-keeper (matrix/OpenAPI as needed) → backend-engineer (stage 4)
authorization_model: >
  Explicit body.productionReissue=true + non-blank reason;
  roles GLOBAL_ADMIN | GROUP_ADMIN (group-scoped) only for non-SPECIMEN;
  AUDIT_ADMIN specimen-only; default regenerate unchanged SPECIMEN (G06-C8).
stage_done_definition: >
  Durable API behavior per BDD-PD6-001…018; default regenerate + preview still SPECIMEN;
  production re-issue omits SPECIMEN with audit reason; mvn verify GREEN;
  permission-matrix + OpenAPI/messageKeys synced; queued deploy evidence;
  architecture review; no FE E2E; do NOT flip #3b/#5a; do NOT invent #119 Word evidence;
  post-task doc-sync + commit-review on MAIN after merge; Task Master registered/closed by plan/doc-sync.
```

**Handoff to `plan-orchestrator`：** 按本文件注册 Task Master 叶、分解 TDD（请求体字段 → 角色闸门 → assembly 分支 → 审计字段 → 001…018 Red 测试 → 契约同步）。实现仅在 worktree `D:/working/DGE-pd6-true-non-specimen-reissue`。
