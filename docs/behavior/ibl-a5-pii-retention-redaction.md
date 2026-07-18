# BDD 行为规格：IBL-A5 — PII-category retention redaction on invocation parameters

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-A5` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | IBL Wave A · **IBL-A5** / F5（`ibl-a5-pii-retention-redaction`） |
| **Slice** | `ibl-a5-pii-retention-redaction` |
| **Branch** | `feat/ibl-a5-pii-retention-redaction` |
| **Worktree** | `D:/working/DGE-ibl-a5-pii-retention-redaction` |
| **Base** | `81e29d99`（handoff） |
| **Placement** | ISOLATED |
| **Task Master** | **#111** IBL-A5 — Batch Recommendation **solo**；`member_task_ids: ["111"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-a5-pii-retention-redaction`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F5 / IBL-A5；留存例外 [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md)（本叶**收窄**）；PII 枚举 [ce-g03-testdata-pii.md](./ce-g03-testdata-pii.md)；再生 [ce-g06-audit-reproducible.md](./ce-g06-audit-reproducible.md)；权限 [permission-matrix.md](../security/permission-matrix.md) §11 |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（本叶为 runtime 留存写路径 + regenerate重放 BE；E2E/UIUX **N/A**） |

**完成声明约束：** 本叶关闭 F5——`parameters_storage` 按 `VariablePiiCategory` 对禁止明文留存的字段脱敏/排除；非脱敏字段仍可按 ADR-0057 留存并支持 regenerate；测试/审计证明禁止类不清文落库。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave A / 程序 Done；**禁止**把 A6（regenerate locale）并入本叶；**禁止**为本叶发明列级 encryption-at-rest（ADR-0057 deferred 不变）。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["111"]
  proposed_slice_id: ibl-a5-pii-retention-redaction
  shared_acceptance_surface: >
    Invocation parameters_storage PII-category redaction + regenerate non-redacted path
  vetoes_applied:
    - different-acceptance-vs-A6-locale
    - umbrella-106-registry-only
  evidence_amortization: mvn verify (+ queued docker when acceptance surface requires)
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| 写入 `api_invocation_record.parameters_storage`（及等价 batch-item 参数 JSON）时按版本 `VariableSchema.piiCategory` 脱敏/排除 | IBL-A6 regenerate `locale` 忠实重放 |
| 禁止类（见 A5-C2）**不得**以调用方提交的明文值持久化 | 列级 / 应用层 `parameters_storage` encryption-at-rest（仍 deferred） |
| `NONE` / 非 PII 字段仍可按 ADR-0057 留存（密码仍剥离） | 放宽 HIST C6：管理端/审计/日志仍禁 variables 明文 |
| Regenerate 使用留存后的变量集；**非脱敏字段**可重放；脱敏字段缺失不导致「仅因 PII 脱敏」而拒绝再生 | 改变 CE-G06 权限矩阵 / SPECIMEN 水印政策 |
| 单测/集成测试证明禁止类明文不在 `parameters_storage` | 测试数据集 CE-G03 闸门改版 |
| 实现期同步 ADR-0057 / matrix §11 / domain 留存表述（收窄例外） | Runtime generate 入参 PII 扫描拒绝；翻转 #3b/#5a；go-live |
| Gates：`mvn -B -ntp -f backend/pom.xml verify` | FE Playwright / OA 旅程 |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| ADR-0057 / CE-G06 授权在调用记录 TTL 内留存**已消毒** variables（剥离加密密码）供 reconciliation + regenerate | [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md)；`InvocationParameterSanitizer` |
| Sanitizer **整图写入** `request.variables()`，**不**读取 `VariablePiiCategory` | `InvocationParameterSanitizer.sanitizeSingleRequest` / `sanitizeBatchItem` |
| Schema 已具备 `piiCategory`（CE-G03 七值）；`≠ NONE` = PII 标记字段 | `VariablePiiCategory`；[ce-g03-testdata-pii.md](./ce-g03-testdata-pii.md) G03-C2/C4 |
| F5：留存路径未应用 PII 分类脱敏 → PII 明文 at rest（High） | IBL plan F5 / IBL-A5 |
| Regenerate 从 `parameters_storage` 提取 `variables` 再 assemble SPECIMEN | `InvocationRegenerationAssemblySupport.extractVariables` |
| 管理端仍不得返回 variables（HIST C6） | [management-invocation-history.md](./management-invocation-history.md)；ADR-0057 §Access |
| Encryption-at-rest 仍 deferred | ADR-0057；ADR-0045 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **A5-S1 Retention write redaction** | 成功/失败调用记录写入 `parameters_storage` 前，对禁止明文留存的变量 key **排除明文**（见 A5-C3）；密码剥离保持 |
| **A5-S2 Retainable clear values** | `piiCategory=NONE`（或缺省等价）且属于版本 schema 的字段，可保留明文（ADR-0057 收窄后的例外） |
| **A5-S3 Regenerate replay** | 再生读取留存后的 map；非脱敏字段忠实重放；脱敏字段视为缺失（占位空/省略），**不**因「PII 曾被脱敏」单独 fail；仍产出 SPECIMEN（CE-G06） |
| **A5-S4 Proof surfaces** | 自动化测试断言 `parameters_storage` JSON **不含**禁止类明文；管理审计/再生响应仍无 variables |
| **A5-S5 Docs alignment** | 实现同变更集收窄 ADR-0057 / permission-matrix §11 / domain 对应句（doc-keeper 可同叶） |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **Runtime API 调用方** | 有效 API credential；sync/batch generate | 提交可含 PII 的 variables；**生成时**仍用明文装配；**落库留存**后禁止类不再明文可读 |
| **调用方 reconciliation** | 调用方 invocation detail（ADR-0040） | 可见留存后的 parameters（含脱敏后形态）；不得再读回禁止类明文 |
| **授权管理员** | `GLOBAL_ADMIN` / 同组 `GROUP_ADMIN` / 模板可见 `AUDIT_ADMIN` | CE-G06 regenerate：用留存变量重放非脱敏字段；响应/审计仍无 variables |
| **系统（retention write）** | `InvocationParameterSanitizer`（或紧邻扩展）+ schema 分类 | 写路径 redact；`variablesHash` 仍反映**原始请求**变量指纹 |
| **系统（regenerate）** | `InvocationRegenerationService` / `InvocationRegenerationAssemblySupport` | 从已脱敏 storage 重放；不向管理面回传变量 |

---

## 3. Goal

1. 凡写入 `parameters_storage`（single / batch-item / 单 item async 顶层 `variables`）的变量映射，对 **禁止明文留存** 的 key **不得**持久化调用方提交的明文值。  
2. **允许留存**的字段（`piiCategory=NONE`）继续支持 ADR-0057 用途：调用方 reconciliation + 受控 regenerate。  
3. Regenerate **不因 PII 脱敏本身失败**；非脱敏字段行为与今日一致；脱敏字段在 SPECIMEN 中为空/省略（可观测）。  
4. 测试证明禁止类明文不出现在持久化 JSON；管理审计与 regenerate HTTP 响应仍无 variables / 密码。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **A5-C1** | **收窄 ADR-0057 Store 条款：** 留存例外**仅**覆盖「非禁止类」模板变量明文 + 非密码加密摘要。禁止类必须 redact/exclude。密码剥离、TTL、管理端不暴露、encryption-at-rest deferred **不变**。 | F5；IBL-A5 验收；ADR-0057 + CE-G03 |
| **A5-C2** | **禁止明文留存（forbidden clear storage）集合：** 目标发布版本 `VariableSchema` 上 `piiCategory ≠ NONE` 的全部枚举值：`PERSONAL_NAME`、`GOVERNMENT_ID`、`FINANCIAL_ACCOUNT`、`CONTACT`、`ADDRESS`、`OTHER_SENSITIVE`。即 CE-G03「PII 标记字段」= 留存禁止类。**不**按类别再拆「可留 / 不可留」子表。 | G03-C2/C4；验收「per VariablePiiCategory」 |
| **A5-C3** | **脱敏形态（exclude + evidence）：** 禁止类 key：**不得**以调用方明文值写入 `variables` 对象。实现须二选一或组合且测试钉死：(a) **省略**该 key；和/或 (b) 写入稳定哨兵（如 JSON `null` 或不含明文的固定 token **`"[REDACTED]"`**，全库统一）。**推荐**同时写入旁路元数据（同级 JSON）：`redactedVariableKeys: string[]`（被脱敏的 key 列表）及可选 `redactedPiiCategories: { [key]: VariablePiiCategory }`——**仅 keys/categories，禁止值**。再生装配忽略哨兵/仅使用仍保留的明文值。 | 可测「无明文」+ regenerate 可组装 |
| **A5-C4** | **`NONE` / 缺省：** schema 缺省或 `NONE` → **允许**明文留存（ADR-0057 收窄后的例外）。 | G03-C3 |
| **A5-C5** | **未知 key（payload 有、schema 无）：** 按 permission-matrix「未知或未分类字段默认按敏感处理」→ **禁止明文留存**（与 A5-C3 同形态脱敏）。（Generate 校验可能已因 `UNKNOWN_FIELD` 失败；若失败路径仍写 parameters，同样不得留明文。） | matrix §11；fail-closed |
| **A5-C6** | **分类时机与版本：** 以该 invocation 解析到的 **template version**（`releaseBundleSnapshotId` / 请求解析版本）上的 `VariableSchema` 为准；写留存时分类。Schema 在事后变更**不**回溯改写历史行。 | 钉扎版本一致性；CE-K01/G06 |
| **A5-C7** | **`variablesHash` / `itemsHash`：** 继续对**调用方原始** variables（脱敏前）计算，以便完整性/对账；hash **不是**明文存储。 | 既有 `VariableHashSupport`；ADR-0020 指纹例外 |
| **A5-C8** | **装配 vs 留存：** Runtime/preview **生成装配**仍使用请求明文（本叶不改变 generate 填充）。仅 **持久化** `parameters_storage` 应用脱敏。成功制品可含 PII（既有正式件语义）；留存列不得再持有禁止类明文。 | F5 范围 = retention path |
| **A5-C9** | **Regenerate：** 从脱敏后 storage 提取 variables；**非脱敏字段**完整重放；脱敏 key 缺失/哨兵 → 按「未提供」进入 assemble（占位空），**不得**从别处取回明文。再生 **成功路径不因「存在已脱敏 PII key」而拒绝**（不新增 `PII_REDACTED` 类阻断码）。既有 CE-G06 失败（过期、BATCH_ROOT、指纹缺失、权限等）不变。SPECIMEN 水印不变。 | 验收「regenerate still works for non-redacted fields」；A6 locale OUT |
| **A5-C10** | **全量 PII 请求：** 若全部业务变量均为禁止类且脱敏后无可重放明文业务字段，regenerate 仍可走技术成功路径（SPECIMEN 可能大量空白）——**本叶不强制**因此失败；银行审计接受「结构/非 PII 可复现、PII 不可从留存重建」。 | 与 A5-C9 一致；哈希不可重建（ADR-0057 Alternatives） |
| **A5-C11** | **调用方 detail：** 返回的 parameters 为脱敏后 JSON（可含 `redactedVariableKeys`）；调用方**看不到**禁止类明文。 | ADR-0040 + 收窄后 0057 |
| **A5-C12** | **管理端 / 审计 / 日志：** HIST C6 / ADR-0057 展示禁令**不放宽**；`INVOCATION_REGENERATED` 摘要仍无 variables。可选：审计可记 `redactedVariableKeys` 计数或 key 列表（**无值**）——非强制。 | G06-C9；G03-C20 模式 |
| **A5-C13** | **Batch：** `sanitizeBatchItem` 与 single 同规则；多 item 根摘要中的 item 摘要若今日不含 variables 明文则保持；单 item async 顶层 `variables` 同样脱敏。 | `InvocationParameterSanitizer` |
| **A5-C14** | **Compute 字段：** 通常不由调用方提交；若 payload 含 compute key，按其 schema `piiCategory` 规则处理（误标 PII 则脱敏）。 | G03-C4 |
| **A5-C15** | **加密密码：** 继续 strip-on-write；与 PII 脱敏正交。 | ADR-0057 §3 |
| **A5-C16** | **存量行：** 本叶**不强制**迁移历史 `parameters_storage`；仅新写入路径生效。可选后续清理任务 OUT。 | 范围 |
| **A5-C17** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；`frontend_ui_in_scope=false` → E2E/UIUX N/A；architecture review（ADR 收窄）；行为变更留存路径 → Stage 5/10 queued deploy 证据按管线。 | delivery constitution |
| **A5-C18** | **完成边界：** Done ≠ Wave A 完备；≠ go-live；#3b/#5a 保持 CONDITIONAL；≠ 启用 encryption-at-rest。 | 队列政策 |

### 4.2 明确非确认 / 非目标（不得当作本叶需求）

| 项 | 说明 |
| --- | --- |
| 按 PII 类别差异化「可留存子集」（如仅 GOVERNMENT_ID 禁、NAME 可留） | 未确认；本叶统一 `≠ NONE` |
| 从 hash 重建 PII 或侧车 vault | 已拒绝（ADR-0057） |
| 管理 UI 查看留存 variables | 禁止 |
| IBL-A6 locale | OUT |
| §Pending SPECIMEN「true re-issue」 | OUT |
| 测试集 CE-G03 行为变更 | OUT |

### 4.3 Open questions

**无阻塞项。** ADR-0057 的 KMS/encryption-at-rest 仍为 pending 但**非**本叶 Done 门（与 CE-G06 相同）。类别级差异化留存若未来需要，另开 ADR/任务，不阻塞 IBL-A5。

```text
open_questions: []
```

---

## 5. Trigger / Preconditions

### Trigger

- Runtime sync/batch generate（或等价路径）**写入或更新** `api_invocation_record.parameters_storage`（成功或既有失败记录写参路径）。  
- 授权管理员 `POST …/invocations/{invocationId}/regenerate` 读取已留存参数重放。

### Preconditions

- 模板版本存在 `VariableSchema`（含可选 `piiCategory`；缺省 `NONE`）。  
- ADR-0057 / CE-G06 留存与再生能力已在主干（本叶叠加脱敏）。  
- 调用方/管理员已通过既有授权（本叶不放宽）。

---

## 6. Primary journey

1. 作者在发布版本上为 `customerName` 设 `piiCategory=PERSONAL_NAME`，为 `productCode` 设 `NONE`。  
2. 调用方 sync generate，variables 含真实姓名 + productCode（及其它合法字段）。  
3. 引擎用明文装配正式件（本叶不改）。  
4. 写 invocation 时 sanitizer：**剥离密码** + **脱敏 `customerName`** + **保留 `productCode` 明文** + 写 `variablesHash`（原始）+ 可选 `redactedVariableKeys`。  
5. DB/`parameters_storage` 中**不出现**姓名明文。  
6. 调用方 detail 所见 variables 无姓名明文，有 productCode。  
7. 管理员 regenerate → SPECIMEN 含 productCode 重放值；姓名占位空；响应无 variables；审计无 variables。

---

## 7. System responses

| 路径 | 响应 |
| --- | --- |
| Retention write | `parameters_storage` 符合 A5-C2…C5；禁止类无明文 |
| Caller detail | 脱敏后 parameters；无禁止类明文 |
| Regenerate success | 既有 CE-G06 成功 envelope；非脱敏字段重放；脱敏字段空；SPECIMEN |
| Regenerate authz fail | 既有 fail-closed（非本叶新码） |
| Management list/detail | 仍无 parameters/variables（HIST C6） |

---

## 8. Acceptance scenarios（Given / When / Then）

### BDD-IBL-A5-001 — PII 标记字段明文不落库（PERSONAL_NAME）

**Given** 可调用版本 schema：`customerName` → `PERSONAL_NAME`；`productCode` → `NONE`  
**And** 有效 runtime 凭证  
**When** sync generate 提交 `customerName="Alice Example"`、`productCode="PRD-1"`（其余合法）且请求成功写入 invocation  
**Then** 持久化 `parameters_storage`（或测试可读等价）中 **不包含** 子串 `Alice Example`  
**And** `variables.productCode`（或等价路径）仍为 `PRD-1`  
**And** 若实现写入 `redactedVariableKeys`，则含 `customerName`

### BDD-IBL-A5-002 — 各禁止类枚举均禁止明文（抽样覆盖）

**Given** 版本上分别存在（可同一用例多字段或参数化）`GOVERNMENT_ID` / `FINANCIAL_ACCOUNT` / `CONTACT` / `ADDRESS` / `OTHER_SENSITIVE` 标记字段  
**When** generate 为各字段提交可区分明文探针值并写入 invocation  
**Then** `parameters_storage` **均不包含**各探针明文  
**And** 至少一个 `NONE` 对照字段明文仍保留（若用例包含）

### BDD-IBL-A5-003 — `NONE` 字段明文可留存（ADR-0057 收窄例外）

**Given** 字段 `letterRef` → `NONE`  
**When** generate 提交 `letterRef="LR-9"` 并留存  
**Then** `parameters_storage` 含明文 `LR-9`  
**And** 加密密码字段仍不出现（回归 ADR-0057 / ADR-0040）

### BDD-IBL-A5-004 — 未知 key 按敏感脱敏

**Given** schema **无** `mysteryField`  
**When** 某路径仍将含 `mysteryField="secret-probe"` 的 parameters 写入 storage（若 generate 已因校验失败：则对失败记录写参路径断言；若实现从不写失败参，则单测直接调用 sanitizer）  
**Then** 落库 JSON **不含** `secret-probe`

### BDD-IBL-A5-005 — `variablesHash` 基于原始请求（非脱敏后）

**Given** BDD-IBL-A5-001 的请求  
**When** 记录已写入  
**Then** `variablesHash` 等于对**脱敏前** variables 的既有哈希算法结果  
**And** 哈希值本身不是明文姓名

### BDD-IBL-A5-006 — Regenerate 重放非脱敏字段

**Given** BDD-IBL-A5-001 已成功留下 invocation（含指纹等 CE-G06 前置）  
**And** 管理员具备 regenerate 权限  
**When** `POST …/invocations/{id}/regenerate`（DOCX 或 PDF）  
**Then** 再生**成功**（非因 PII 脱敏而 4xx/5xx）  
**And** SPECIMEN 制品可观测包含 `productCode` 的重放值（或装配输入断言）  
**And** 制品**不**包含原始 `Alice Example` 明文（因留存未提供）  
**And** 响应 body **无** `variables` / 密码字段  
**And** `INVOCATION_REGENERATED` 审计摘要**无**变量明文

### BDD-IBL-A5-007 — Batch item 留存同样脱敏

**Given** batch item 变量含 PII 标记字段明文探针与 `NONE` 对照  
**When** batch-generate 写入 item 级（或单 item 顶层）parameters  
**Then** 对应 storage **不含** PII 探针明文  
**And** `NONE` 对照值仍保留

### BDD-IBL-A5-008 — 管理端 detail 仍不返回 parameters

**Given** 任意含脱敏留存的 invocation  
**When** 管理端 invocation detail/list/CSV（既有 HIST 路径）  
**Then** **不**返回 `parameters` / variables 明文（回归 HIST C6；本叶不放宽）

### BDD-IBL-A5-009 — 调用方 detail 可见脱敏后 parameters、无禁止类明文

**Given** BDD-IBL-A5-001 的 invocation  
**When** 拥有该调用的 API 凭证查询 caller-facing invocation detail  
**Then** 返回 parameters 中无 `Alice Example`  
**And** 可见非脱敏字段（如 `productCode`）及/或 `redactedVariableKeys`（若实现）

### BDD-IBL-A5-010 — 密码剥离与 PII 脱敏正交

**Given** generate 请求启用输出加密并提供 `openPassword` / `ownerPassword`  
**When** 写入 `parameters_storage`  
**Then** storage **无**密码明文（既有）  
**And** 禁止类 PII 探针亦无明文（本叶）

### BDD-IBL-A5-011 — 文档收窄可追溯（实现同变更集）

**Given** 本叶代码合并准备  
**When** 审查 ADR-0057 / permission-matrix §11 / domain 留存句  
**Then** 文本明确：留存例外**排除** `piiCategory ≠ NONE` 明文；指向本行为文件  
**And** **不**将 encryption-at-rest 标为已交付

---

## 9. Boundary / exception

| 场景 | 行为 |
| --- | --- |
| 仅 PII、无 `NONE` 业务字段 | 留存 variables 可为空/仅元数据；regenerate 仍可不因脱敏失败（A5-C10） |
| Schema 全 `NONE` | 行为 ≈ 今日 ADR-0057（仅密码剥离） |
| 历史行含 PII 明文 | 不强制回填清理（A5-C16） |
| 再生过期 / BATCH_ROOT / 无指纹 | 既有 CE-G06 错误，本叶不改 |
| 授权失败 | 既有 401/403 |
| 日志意外打印 storage | 禁止；测试/评审守护（不新增日志字段含明文） |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| DB / JSON | `parameters_storage` 断言无探针明文；有 `NONE` 对照值 |
| API | Caller detail 脱敏；management 无 parameters；regenerate 无 variables |
| Audit | `INVOCATION_REGENERATED` 无变量值 |
| Tests | JUnit：sanitizer/redaction + regenerate 非脱敏重放 + 密码回归；覆盖 A5-C2 枚举抽样 |
| Docs | ADR-0057 / matrix §11 / domain 收窄句 + 本文件 |
| Gates | `mvn verify` GREEN；deploy 按管线 |

---

## 11. Traceability

| 项 | 引用 |
| --- | --- |
| Plan | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) · F5 · **IBL-A5** |
| Task Master | **#111** |
| ADR | [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md)（收窄）；[ADR-0020](../adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md)；[ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md) |
| Related BDD | [ce-g03-testdata-pii.md](./ce-g03-testdata-pii.md)；[ce-g06-audit-reproducible.md](./ce-g06-audit-reproducible.md)；[management-invocation-history.md](./management-invocation-history.md) |
| Code anchors | `InvocationParameterSanitizer`；`ApiInvocationRecordEntity.parametersStorage`；`InvocationRegenerationAssemblySupport`；`VariablePiiCategory`；`VariableSchemaEntity` |
| Permissions | [permission-matrix.md](../security/permission-matrix.md) §11 — 无新角色位 |

---

## 12. Out of scope（本叶）

- IBL-A6 locale 忠实重放  
- SPECIMEN「true re-issue」/ 去水印（§Pending）  
- `parameters_storage` encryption-at-rest / KMS  
- 历史行迁移擦除  
- CE-G03 测试集闸门 / FE PII UI  
- Runtime 因 PII 标签拒绝 generate  
- 翻转 #3b / #5a；go-live；Wave A Done  

---

## 13. Ready-for-implementation handoff

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-a5-pii-retention-redaction.md
task_ids: ["111"]
plan_id: IBL-A5
frontend_ui_in_scope: false
acceptance_scenario_ids:
  - BDD-IBL-A5-001
  - BDD-IBL-A5-002
  - BDD-IBL-A5-003
  - BDD-IBL-A5-004
  - BDD-IBL-A5-005
  - BDD-IBL-A5-006
  - BDD-IBL-A5-007
  - BDD-IBL-A5-008
  - BDD-IBL-A5-009
  - BDD-IBL-A5-010
  - BDD-IBL-A5-011
next_stage: backend-engineer (stage 4)
adr_note: >
  ADR-0057 Amendment 2026-07-18 applied (docs-first): clear values only for
  piiCategory=NONE (plus non-password encryption summary); all piiCategory≠NONE
  and unknown keys must redact/exclude. Encryption-at-rest remains deferred.
  Downstream sync: permission-matrix §11, domain-model, data-storage-view,
  contract-outline, api/README, ADR-0040 Amendment. #3b/#5a not flipped.
```

**TDD Red 优先场景：** BDD-IBL-A5-001（PII 不落库）、003（NONE 可留）、006（regenerate 非脱敏重放）；随后 002（枚举抽样）、007（batch）、010（密码正交）。
