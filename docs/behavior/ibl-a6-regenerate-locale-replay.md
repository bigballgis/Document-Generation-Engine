# BDD 行为规格：IBL-A6 — Regenerate locale replay fix（SPECIMEN unchanged）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-A6` |
| **编写日期** | 2026-07-19 |
| **程序 / 队列** | IBL Wave A · **IBL-A6** / F6 locale only（`ibl-a6-regenerate-locale-replay`） |
| **Slice** | `ibl-a6-regenerate-locale-replay` |
| **Branch** | `feat/ibl-a6-regenerate-locale-replay` |
| **Worktree** | `D:/working/DGE-ibl-a6-regenerate-locale-replay` |
| **Base** | `79ef82b3`（handoff） |
| **Placement** | ISOLATED |
| **Task Master** | **#112** IBL-A6 — Batch Recommendation **solo**；`member_task_ids: ["112"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-a6-regenerate-locale-replay`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F6 / IBL-A6 / **PD-6**；再生基线 [ce-g06-audit-reproducible.md](./ce-g06-audit-reproducible.md)；水印 [ce-g02-specimen-watermark.md](./ce-g02-specimen-watermark.md)；金额/拼写 locale 契约 [ibl-a2-format-amount-currency.md](./ibl-a2-format-amount-currency.md) / [ibl-a3-amount-in-words.md](./ibl-a3-amount-in-words.md)；留存 [ibl-a5-pii-retention-redaction.md](./ibl-a5-pii-retention-redaction.md) / [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（本叶为 regenerate 装配 locale 重放 BE；E2E/UIUX **N/A**；FE 再生 CTA 仍 OOS per G06-C17） |

**完成声明约束：** 本叶关闭 F6 的 **locale 忠实重放**——受控 regenerate 对 compute 求值使用原 invocation 的 `context.locale`（不得硬编码 `null` 从而在「原请求有 locale」时静默落入引擎默认）。**SPECIMEN 水印政策不变**（**PD-6** 未确认前禁止去水印 /「true re-issue」）。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave A / 程序 Done；**禁止**把 IBL-B+ 或 PD-6 并入本叶。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["112"]
  proposed_slice_id: ibl-a6-regenerate-locale-replay
  shared_acceptance_surface: >
    Regenerate assembly replays original invocation locale into VariableComputeService;
    SPECIMEN watermark unchanged (PD-6 out)
  vetoes_applied:
    - pd6-true-reissue-blocked
    - umbrella-106-registry-only
    - different-acceptance-vs-IBL-B
  evidence_amortization: mvn verify (+ queued docker when acceptance surface requires)
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| Regenerate 装配调用 `VariableComputeService.applyCompute(..., localeTag)` 时使用原 invocation 留存 locale | **PD-6** true non-specimen re-issue / 去掉 SPECIMEN |
| 从 `parameters_storage.contextSummary.locale` 读取（见 A6-C1） | 改变 CE-G06 权限矩阵 / 审计字段 / FE 再生 CTA |
| 写路径：可再生行（含 **BATCH_ITEM**）须能留存 `contextSummary.locale`（见 A6-C3） | 回填/猜测历史行缺失的 locale |
| 回归：locale 敏感 `FORMAT_AMOUNT` / `FORMAT_DATE` / 二元 `SPELL_AMOUNT` 在 regenerate 下与原 locale 一致 | IBL-E1 locale-variant 模板；F8 timezone/as-of 语义大改 |
| Gates：`mvn -B -ntp -f backend/pom.xml verify` | Playwright / OA 旅程；翻转 #3b/#5a；go-live |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| Runtime generate 将 `request.context().locale()` 传入 `applyCompute` | `RuntimeGenerationService` / `DocumentGenerationAssemblySupport.generate(..., localeTag)` |
| 成功调用留存 `parameters_storage.contextSummary.locale`（非空白时） | `InvocationParameterSanitizer.putContextSummary` / `contextSummary` |
| **Regenerate 装配硬编码 `null` locale** → `VariableComputeEngine.resolveLocale(null)` → `ComputeDslLimits.DEFAULT_LOCALE`（`zh-CN`） | `InvocationRegenerationAssemblySupport.assembleSpecimen`：`applyCompute(versionId, variables, null)` |
| 原请求若为 `en-US` 等非默认 locale，再生件金额/日期/二元英文大写等与正式件不一致 | F6；IBL-A2/A3 locale 契约 |
| DOCX/PDF 再生仍强制 SPECIMEN（CE-G02 复用）——本叶保持 | `DocxSpecimenWatermarkStamper` / `PdfSpecimenWatermarkStamper`；PD-6 §Pending |
| **BATCH_ITEM** `sanitizeBatchItem` **未**写入 `contextSummary` → 即使修读路径，item 行仍无 locale 可重放 | `InvocationParameterSanitizer.sanitizeBatchItem` |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **A6-S1 Locale extract** | 再生前从该 invocation 的 `parameters_storage` 提取留存 locale 标签（BCP-47 字符串） |
| **A6-S2 Compute replay** | 将提取的标签传入 `applyCompute`；**不得**在「已留存非空 locale」时传 `null` |
| **A6-S3 Retention write (BATCH_ITEM)** | 可再生的 BATCH_ITEM 行写入时携带父 batch 请求的 `contextSummary`（含 locale），与 SINGLE/ASYNC 对齐 |
| **A6-S4 Absent locale fidelity** | 原请求无/空白 locale → 留存无 `locale` 键 → 再生传 `null` → 引擎默认（与原 generate 一致） |
| **A6-S5 SPECIMEN unchanged** | 成功再生件仍强制 SPECIMEN；水印失败 fail-closed；正式 runtime 路径仍无水印 |
| **A6-S6 Locale-sensitive regression** | 自动化测试证明 regenerate 下金额/日期（及适用的 SPELL）跟随原 locale，而非静默 `zh-CN` |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **Runtime API 调用方** | 有效凭证；generate / batch；`context.locale` | 正式件按请求 locale 求值；本叶不改 generate 路径语义 |
| **授权管理员** | `GLOBAL_ADMIN` / 同组 `GROUP_ADMIN` / 模板可见 `AUDIT_ADMIN` | CE-G06 regenerate；获得与原 locale 一致的 **SPECIMEN** 样件 |
| **系统（retention）** | `InvocationParameterSanitizer` | 可再生行留存 `contextSummary.locale` |
| **系统（regenerate）** | `InvocationRegenerationService` / `InvocationRegenerationAssemblySupport` | 提取 locale → compute → 钉扎装配 → SPECIMEN |

---

## 3. Goal

1. 受控 regenerate 对变量 compute 求值使用**原 invocation 的 locale**（来自留存 `contextSummary.locale`），关闭「硬编码 `null` → 引擎默认」导致的失真。  
2. 原请求未提供 locale 时，再生与原 generate 一样走引擎默认——**忠实**，不是 bug。  
3. **SPECIMEN 水印保持**（PD-6 未确认）；响应/审计仍无 variables。  
4. 回归覆盖 locale 敏感金额/日期（及适用 SPELL）在 regenerate 路径的可观测正确性。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave A Done（本叶仅关闭 A6）。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **A6-C1** | **Locale 来源：** regenerate 装配使用的 `localeTag` = 该 invocation `parameters_storage` JSON 中 `contextSummary.locale`（字符串；trim 后非空才视为存在）。**不**从管理端请求 body 另传 locale 覆盖；**不**从模板/引擎配置另取「偏好 locale」覆盖留存值。 | F6；sanitizer 已留存；G06 内部重放 parameters |
| **A6-C2** | **传入 compute：** `variableComputeService.applyCompute(versionId, variables, localeTag)`，其中 `localeTag` 为 A6-C1 结果或 `null`（键缺失/空白）。**禁止**继续无条件传字面量 `null` 而忽略已留存 locale。 | `InvocationRegenerationAssemblySupport` 缺陷点 |
| **A6-C3** | **BATCH_ITEM 写路径补齐：** `sanitizeBatchItem`（及任何写入可再生 BATCH_ITEM `parameters_storage` 的等价路径）必须 `putContextSummary(..., request.context())`，使 item 行具备与 SINGLE 同等的 `contextSummary.locale`（当原 batch 请求提供了非空白 locale）。SINGLE / 单 item ASYNC 顶层已有 `putContextSummary` 则保持。 | 今日 item 行无 context → 否则 A6 对 BATCH_ITEM 无效 |
| **A6-C4** | **缺失 locale 忠实重放：** 无 `contextSummary` / 无 `locale` / 空白 → `localeTag=null` → `VariableComputeEngine` 默认 `zh-CN`（`ComputeDslLimits.DEFAULT_LOCALE`）。此行为与原 generate「未传 locale」一致，**验收为正确**。 | A3-C10；引擎既有 resolve |
| **A6-C5** | **非法/未知 locale 标签：** 再生路径**不**新增校验码；行为与 runtime generate 相同（引擎 `resolveLocale` 回退默认）。本叶不单独发明 regenerate 专用 locale 错误。 | 缩小范围；对齐 runtime |
| **A6-C6** | **历史行：** 本叶上线前已写入、且无 `contextSummary.locale` 的行（含旧 BATCH_ITEM）→ 再生按 A6-C4（`null`）。**不**回填、**不**从其它系统推断 locale。 | G06-C6 诚实性同构 |
| **A6-C7** | **SPECIMEN / PD-6：** 再生成功路径**继续**强制 DOCX+PDF SPECIMEN（G06-C13 / G02）；水印失败 fail-closed。**禁止**本叶移除或弱化水印。True non-specimen re-issue **仅**在 PD-6 确认后另开任务。 | IBL plan §Pending PD-6；验收「watermark unchanged」 |
| **A6-C8** | **PII / 变量重放：** 继续 IBL-A5：`toReplayVariables`；脱敏字段不因本叶恢复。Locale 与 PII 正交。 | A5-C9 |
| **A6-C9** | **权限 / 过期 / drift / 指纹：** 既有 CE-G06 不变（403/410/409/422 等）。 | G06-C8…C19 |
| **A6-C10** | **响应 / 审计：** 仍禁 variables；`INVOCATION_REGENERATED` 可不新增 locale 字段（可选：摘要可记 `locale` 标签字符串——**非强制**；若记则仅标签无变量值）。 | G06-C9/C15 |
| **A6-C11** | **可观测回归（强制）：** 至少一组自动化测试：原 generate 使用显式非默认 locale（推荐 `en-US`），模板含 locale 敏感 compute（至少 **`FORMAT_AMOUNT`** 二元 ISO 与 **`FORMAT_DATE`** 之一必测；二元 **`SPELL_AMOUNT(en,USD)`** 推荐同测或等价断言装配输入/求值结果），regenerate 后求值/制品文本与「同 locale 直接 generate」一致方向，且**明显不同于**「强制 `null`→zh-CN」的错误结果。SPECIMEN 仍可观测。 | IBL-A6 验收「locale-sensitive amount/date regression」 |
| **A6-C12** | **对照用例：** 原请求**无** locale 的 regenerate → 与引擎默认一致（证明 A6-C4，避免「永远注入 en-US」回归）。 | 边界 |
| **A6-C13** | **正式 runtime 路径：** 本叶**不**改变无水印 formal generate；仅 regenerate 装配读 locale。 | G06-S3 |
| **A6-C14** | **FE：** `frontend_ui_in_scope=false`。 | handoff |
| **A6-C15** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；行为变更验收面 → Stage 5/10 queued Docker 按管线；architecture review。 | delivery constitution |
| **A6-C16** | **完成边界：** A6 Done ≠ Wave A 完备需等程序关闭规则；≠ go-live；#3b/#5a 保持 CONDITIONAL。Wave A 退出仍要求 A1–A6 全 Done（本叶可关闭 A6 行，**不**由本 BDD 单独宣称 Wave A Done——由 plan/doc-sync 在 A6 代码 Done 后处理）。 | 队列政策 |

### 4.2 已确认（上游交付，本叶只消费）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **A6-U1** | 受控 regenerate API + SPECIMEN + 钉扎指纹 | CE-G06 |
| **A6-U2** | `contextSummary.locale` 留存形态（SINGLE/batch root） | `InvocationParameterSanitizer` |
| **A6-U3** | `FORMAT_AMOUNT` ISO + locale 数字格式；`SPELL_AMOUNT` 二元语言来自 locale | IBL-A2 / IBL-A3 |
| **A6-U4** | 引擎默认 locale `zh-CN` | CE-K03 / `ComputeDslLimits` |
| **A6-U5** | PD-6 未确认 → 不得去 SPECIMEN | IBL plan §Pending |

### 4.3 非确认假设（不得升格为需求）

| ID | 陈述 | 状态 |
| --- | --- | --- |
| **A6-N1** | 再生件可作为正式对外函件（无 SPECIMEN） | **非确认** — **明确拒绝**直至 PD-6 |
| **A6-N2** | 管理端 regenerate body 允许覆盖 locale | **非确认** — 本叶拒绝（A6-C1） |
| **A6-N3** | 回填历史 BATCH_ITEM / 无 locale 行 | **非确认** — A6-C6 |
| **A6-N4** | F8 时区 / as-of date 语义修复 | **非确认** — deferred residual；本叶仅 locale 标签重放 |
| **A6-N5** | FE 再生按钮 / E2E | **非确认** — OOS |
| **A6-N6** | 翻转 #3b / #5a / go-live | **非确认** — **明确拒绝** |

---

## 5. Trigger / Preconditions

**Trigger：** 授权管理员对合格 invocation 调用  
`POST /api/management/v1/templates/{templateId}/api/invocations/{invocationId}/regenerate`  
（可选 `outputFormat`；既有 G06-C7）。

**Preconditions（成功再生）：**

- 调用方角色满足 G06-C8；模板范围可见。  
- Invocation 为 `SINGLE` / `BATCH_ITEM` / `ASYNC_TASK`；未过期；指纹非空；钉扎母版 hash 匹配。  
- `parameters_storage` 可提取可重放 variables（A5 规则）。  
- 本叶：若需证明非默认 locale，原 generate 已写入 `contextSummary.locale`（新写 BATCH_ITEM 含 A6-C3）。

---

## 6. Primary journey

1. Runtime 调用方以 `context.locale = "en-US"`（示例）成功 generate；记录留存 `contextSummary.locale`。  
2. 授权管理员调用 regenerate（DOCX 或 PDF）。  
3. 系统：权限/过期/指纹/drift 校验 → 提取 variables + **locale** → `applyCompute(..., "en-US")` → 钉扎装配 → **SPECIMEN** → 存储 → `INVOCATION_REGENERATED`。  
4. 管理员下载样件：可见 SPECIMEN；locale 敏感字段与原 `en-US` generate 一致方向，而非 `zh-CN` 默认失真。

---

## 7. System responses

| 路径 | 响应 |
| --- | --- |
| 再生成功（有留存 locale） | 200 + 既有 regeneration 摘要；制品 SPECIMEN；compute 使用原 locale |
| 再生成功（无留存 locale） | 同上；compute 使用引擎默认（与原 generate 一致） |
| 水印失败 | 既有 fail-closed（无无水印成功件） |
| 权限/过期/drift/指纹 | 既有 CE-G06 错误 |
| 管理端/审计 | 无 variables 明文 |

---

## 8. Acceptance scenarios（Given / When / Then）

### BDD-IBL-A6-001 — 留存非默认 locale 时 regenerate 传入该标签（装配契约）

**Given** 合格 SINGLE（或 ASYNC_TASK）invocation，其 `parameters_storage.contextSummary.locale = "en-US"`  
**And** 管理员具备 regenerate 权限  
**When** 调用 regenerate  
**Then** 再生装配对 `VariableComputeService.applyCompute` 的 `localeTag` 参数为 `"en-US"`（或与留存值字节相等的字符串）  
**And** **不是**无条件 `null`

### BDD-IBL-A6-002 — locale 敏感 FORMAT_AMOUNT 再生不落入 zh-CN 默认失真

**Given** 模板含二元 `FORMAT_AMOUNT`（ISO 币种，如 EUR）且原 generate 使用 `context.locale=en-US` 成功并留存  
**When** 管理员 regenerate（DOCX 或可抽取文本的 PDF）  
**Then** 再生件中该金额格式化结果与「同 variables + `en-US` 的 generate/compute」一致方向（数字/分组/符号本地化跟随 `en-US`）  
**And** 结果**可区分于**强制 `localeTag=null`（引擎 `zh-CN`）下的错误格式（单测可用求值断言或制品文本断言钉死）

### BDD-IBL-A6-003 — locale 敏感 FORMAT_DATE 再生跟随原 locale

**Given** 模板含 `FORMAT_DATE`（或等价 locale 敏感日期格式化 compute），原 generate `context.locale=en-US` 并留存  
**When** regenerate  
**Then** 日期文本本地化与 `en-US` 路径一致方向，而非静默 `zh-CN` 默认模式

### BDD-IBL-A6-004 — 二元 SPELL_AMOUNT(en,USD) 再生保持英文（推荐；若模板含该表达式则强制）

**Given** 模板含 `SPELL_AMOUNT(amount, 'USD')`，原 generate `context.locale=en-US` 成功并留存  
**When** regenerate  
**Then** 再生求值/制品含英文 USD 大写契约方向（对齐 IBL-A3，如稳定金标串）  
**And** **不**因 regenerate 丢 locale 而变为中文路径或 `VARIABLE_COMPUTE_FAILED` 的错误失败（在原 generate 已成功的前提下）

### BDD-IBL-A6-005 — 原请求无 locale → 再生忠实默认

**Given** 合格 invocation，`parameters_storage` **无**非空 `contextSummary.locale`  
**When** regenerate  
**Then** `applyCompute` 的 `localeTag` 为 `null`（或等价「未提供」）  
**And** 求值行为与引擎默认 `zh-CN` 一致（与原无-locale generate 一致）

### BDD-IBL-A6-006 — SPECIMEN 水印仍强制（PD-6 未确认）

**Given** BDD-IBL-A6-001 成功路径  
**When** 检查再生 DOCX 与/或 PDF  
**Then** DOCX 眉脚与/或 PDF 文本含字面量 `SPECIMEN`（对齐 G06-C13 / G02）  
**And** 本叶**不**提供无水印成功再生件

### BDD-IBL-A6-007 — BATCH_ITEM 留存并重放 locale

**Given** batch-generate 请求 `context.locale=en-US`，产生可再生 `BATCH_ITEM` 行（本叶写路径已含 `contextSummary`）  
**When** 管理员对该 item invocation regenerate  
**Then** item 的 `parameters_storage` 含 `contextSummary.locale=en-US`  
**And** 再生 `applyCompute` 使用 `"en-US"`（同 A6-001）

### BDD-IBL-A6-008 — 历史无 locale 的 BATCH_ITEM 不臆造

**Given** 上线前写入的 BATCH_ITEM，`parameters_storage` 无 `contextSummary.locale`  
**When** regenerate  
**Then** 行为同 BDD-IBL-A6-005（`null` → 引擎默认）  
**And** **不**从 batch 根或其它行推断补写

### BDD-IBL-A6-009 — 管理响应与审计仍无 variables

**Given** 任意本叶成功/失败 regenerate 终态  
**When** 检查 HTTP 响应与 `INVOCATION_REGENERATED` 审计摘要  
**Then** **无** variables / 密码明文（回归 G06-C9 / A5）

### BDD-IBL-A6-010 — 正式 runtime 路径仍无 SPECIMEN（回归）

**Given** 同一模板正式 runtime generate（非 regenerate）  
**When** 检查正式产物  
**Then** **无** SPECIMEN 水印要求（本叶未把水印带入 formal 路径）

### BDD-IBL-A6-011 — 既有 CE-G06 失败路径不变

**Given** 过期 / 无指纹 / hash mismatch / 无权限 invocation  
**When** regenerate  
**Then** 既有 410 / 冲突 / 403 等行为保持  
**And** 本叶不因「修 locale」放宽这些闸门

---

## 9. Boundary / exception

| 场景 | 行为 |
| --- | --- |
| 留存 locale 非法/未知标签 | 与 runtime 相同：引擎回退默认；不新增 regenerate 专用错误码 |
| 仅 PII 脱敏后无可重放业务字段 | 仍按 A5；与 locale 正交 |
| 水印失败 | fail-closed；无成功无水印件 |
| PD-6 未确认 | 禁止去水印 |
| FE 再生 CTA | OOS |
| #3b / #5a | 保持 CONDITIONAL；本叶不翻转 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| Unit / IT | `applyCompute` locale 参数断言；FORMAT_AMOUNT / FORMAT_DATE（+ 可选 SPELL）regenerate 对比 |
| Artifact | SPECIMEN 仍存在；locale 敏感文本正确方向 |
| DB JSON | 新 BATCH_ITEM 含 `contextSummary.locale`（当请求提供） |
| API / Audit | 无 variables；既有 regeneration 摘要 |
| Gates | `mvn verify` GREEN；deploy 按管线 |
| Docs | 本文件；IBL plan A6 指向本 BDD；**不**改 PD-6 为已确认 |

---

## 11. Traceability

| 项 | 引用 |
| --- | --- |
| Plan | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) · F6 · **IBL-A6** · **PD-6** |
| Task Master | **#112** |
| Related BDD | [ce-g06-audit-reproducible.md](./ce-g06-audit-reproducible.md)；[ce-g02-specimen-watermark.md](./ce-g02-specimen-watermark.md)；[ibl-a2-format-amount-currency.md](./ibl-a2-format-amount-currency.md)；[ibl-a3-amount-in-words.md](./ibl-a3-amount-in-words.md)；[ibl-a5-pii-retention-redaction.md](./ibl-a5-pii-retention-redaction.md) |
| Code anchors | `InvocationRegenerationAssemblySupport.assembleSpecimen`；`InvocationParameterSanitizer`（`putContextSummary` / `sanitizeBatchItem`）；`VariableComputeService.applyCompute`；`VariableComputeEngine.resolveLocale`；`ComputeDslLimits.DEFAULT_LOCALE` |
| Permissions | [permission-matrix.md](../security/permission-matrix.md) §11 — 无新角色位 |

---

## 12. Out of scope（本叶）

- PD-6 true non-specimen / 去 SPECIMEN  
- 管理端覆盖 locale；FE 再生 UI / E2E  
- 历史行 locale 回填  
- F8 timezone / as-of date  
- IBL-E1 locale-variant 模板模型  
- 翻转 #3b / #5a；go-live；单独宣称 Wave A / IBL 程序 Done  

---

## 13. Ready-for-implementation handoff

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-a6-regenerate-locale-replay.md
task_ids: ["112"]
plan_id: IBL-A6
frontend_ui_in_scope: false
acceptance_scenario_ids:
  - BDD-IBL-A6-001
  - BDD-IBL-A6-002
  - BDD-IBL-A6-003
  - BDD-IBL-A6-004
  - BDD-IBL-A6-005
  - BDD-IBL-A6-006
  - BDD-IBL-A6-007
  - BDD-IBL-A6-008
  - BDD-IBL-A6-009
  - BDD-IBL-A6-010
  - BDD-IBL-A6-011
next_stage: plan-orchestrator (stage 2) → backend-engineer (stage 4)
defect_anchor: >
  InvocationRegenerationAssemblySupport.assembleSpecimen calls
  variableComputeService.applyCompute(versionId, variables, null)
  while runtime passes request.context().locale(); locale is already
  retained at parameters_storage.contextSummary.locale for SINGLE/ASYNC;
  BATCH_ITEM sanitizeBatchItem must also putContextSummary.
pd6_note: SPECIMEN unchanged until PD-6 confirmed; do not flip #3b/#5a.
```

**Handoff to `plan-orchestrator`：** 按本文件分解 TDD 任务（读 locale → applyCompute；BATCH_ITEM 写路径；A6-001…011 Red 测试；SPECIMEN 回归；gates）。实现仅在 worktree `D:/working/DGE-ibl-a6-regenerate-locale-replay`。
