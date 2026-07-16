# BDD 行为规格：CE-O01 — PDF/A-2b 归档输出选项

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-O01` |
| **编写日期** | 2026-07-16 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §8 Wave CE-O · CE-O01 |
| **Slice** | `ce-o01-pdfa-output` |
| **Worktree** | `D:/working/DGE-ce-o01-pdfa-output` · `feat/ce-o01-pdfa-output` |
| **Task Master** | **#81**（pending → 本片交付） |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED |
| **上游** | CE-K07 (#54) **Done**（金标语料骨架可扩展）；E02 (#79) **Done**（拓扑收敛）；PDF 动态加密（P7 / [ADR-0001](../adr/authorization-security/0001-output-encryption.md)）已存在 |
| **Owning docs** | 本文件（行为 SoT）；计划 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §8/§10 D6；需求 [requirements-plan.md](../requirements/requirements-plan.md)；产品 [PRD.md](../product/PRD.md)；领域 [domain-model.md](../domain/domain-model.md) §2.6.8；ADR [0058-pdfa-2b-archival-output.md](../adr/rendering-authoring/0058-pdfa-2b-archival-output.md)；契约 [contract-outline.md](../api/contract-outline.md) + OpenAPI（实现期） |
| **Frontend UI** | **Out of scope（API / render-only）** — 无管理端 render-profile / PDF/A 开关旅程；无 Playwright E2E/UIUX 义务 |

**完成声明约束：** 本切片关闭「PDF/A 缺失」缺口的最小闭环（发布锁定 `pdfArchivalProfile` + LibreOffice PDF/A-2b 过滤器 + 与加密互斥 + ADR-0058 + 金标/轻量归档校验）；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-O02（D5 skipped）；**不**交付管理端 profile 编辑 UI。

---

## 1. 概述

银行信函正式 PDF 在部分合规/归档场景需要 **PDF/A** 自包含归档格式。平台今日 LibreOffice 转换路径仅输出常规 PDF（`--convert-to pdf`），无归档配置面；与动态加密（密码保护）在标准上不兼容。CE 决策 **D6（2026-07-14）** 选定 **PDF/A-2b**（非 A-1b）。

| 行为域 | 摘要 |
| --- | --- |
| **O01-S1 Render profile 字段** | 发布锁定 `RenderProfile.pdfArchivalProfile`：`NONE` \| `PDF_A_2B`；缺省 `NONE`；调用方不可覆盖 |
| **O01-S2 LibreOffice 过滤器** | `PDF_A_2B` 且 `output.format=PDF` 时使用 LO PDF/A-2b 导出过滤器；`NONE` 保持既有常规 PDF 路径 |
| **O01-S3 加密互斥** | `pdfArchivalProfile=PDF_A_2B` 与请求 `encryption.enabled=true` **互斥**；fail-closed 拒绝，不生成半成品 |
| **O01-S4 ADR + 金标** | ADR-0058 记录 A-2b 选型；金标语料 ACTIVE 包 + veraPDF **或**轻量归档元数据断言接入 `mvn verify` |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| `RenderProfile` 无 `pdfArchivalProfile` | `sharedkernel.document.RenderProfile` |
| LO 仅 `--convert-to pdf` | `LibreOfficePdfConversionService` |
| PDF 加密后处理存在 | `PdfEncryptionService` + `DocumentArtifactPipeline` |
| 金标骨架含 `07-encrypted-pdf` | CE-K07；本片新增/激活 PDF/A 主题包 |
| D6 = PDF/A-2b | CE 计划 §10 拍板 2026-07-14 |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **Runtime API 调用方** | 持有效凭证的系统/集成方 | 按已发布模板生成 PDF；需要归档级 PDF 时消费已锁定 `PDF_A_2B` 的 release；不得用加密参数绕过互斥 |
| **平台工程师 / 渲染运维** | 配置默认/版本 render profile 资产 | 将 `pdfArchivalProfile` 写入平台默认或版本锁定 JSON（本片无管理 UI） |
| **模板发布路径** | `RenderProfileService.lockForPublish` | 发布时快照含 `pdfArchivalProfile`；运行时只读锁定值 |
| **系统** | 渲染管线 + LO + 校验 | 过滤器切换；互斥校验；金标回归 |
| **（非本片）管理 UI 用户** | 编排/策略页 | 本片不提供 PDF/A 开关；既有加密开关仍在 API policy |

无新权限码：生成权限仍由既有 runtime / API policy 控制；越权 fail-closed 不变。

---

## 3. Goal

1. 发布锁定的 `RenderProfile` 可声明 `pdfArchivalProfile=PDF_A_2B`，使正式 **PDF** 产出走 LibreOffice **PDF/A-2b** 导出过滤器。
2. 缺省 / 缺字段行为为 `NONE`：既有常规 PDF 路径与金标 bitwise/语义不因本片默认翻转而破坏。
3. 当锁定 profile 为 `PDF_A_2B` 且请求启用动态加密时，**在转换/加密前**拒绝并返回稳定错误码，不产出加密或非归档半成品。
4. 调用方 **不得** 通过 `CallerRenderOverride` 或请求体覆盖 `pdfArchivalProfile`。
5. ADR-0058 记录 D6（选 A-2b、不选 A-1b）及互斥后果。
6. 金标语料至少 1 个 ACTIVE PDF/A 主题包，用 veraPDF（若可嵌入 verify）**或**轻量归档校验（PDF/A 标识 / `pdfaid` XMP 等）证明归档路径。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **O01-C1** | **归档级别固定 PDF/A-2b。** 枚举值仅 `NONE` \| `PDF_A_2B`（`UPPER_SNAKE_CASE`）。不实现 A-1b / A-2a / A-3* 本片。 | D6 2026-07-14 |
| **O01-C2** | 字段名：`RenderProfile.pdfArchivalProfile`。JSON 缺省 / null / 缺字段 → **`NONE`**。 | 计划卡 + 向后兼容 |
| **O01-C3** | **发布锁定、调用方不可覆盖。** 与 `pdfPageNumberStampingEnabled` 同族：`resolveEffectiveProfile` 忽略 caller override；runtime / preview 解析到的非 null profile 以锁定快照为准。 | P18-T08 + CE-K06c 模式 |
| **O01-C4** | **仅 `output.format=PDF` 应用 LO PDF/A-2b 过滤器。** `DOCX` 输出忽略归档过滤器（字段仍可出现在 profile 快照中，不影响 DOCX 字节）。 | 归档语义仅 PDF |
| **O01-C5** | **LibreOffice：** `PDF_A_2B` 时 `--convert-to`（或等价 FilterData）使用 **writer PDF/A-2b** 导出过滤器；`NONE` 保持既有 `pdf` 路径。具体 FilterData 键以实现期 ADR/代码注释为准，行为验收以产出通过归档校验为准。 | 计划卡 |
| **O01-C6** | **加密互斥（硬门禁）：** 当有效 `pdfArchivalProfile=PDF_A_2B` **且** 请求 `encryption.enabled=true`（PDF 路径）→ **拒绝**，HTTP **400**，错误码 `PDF_ARCHIVAL_ENCRYPTION_MUTEX`，`messageKey=api.error.generation.pdfArchivalEncryptionMutex`，`retryable=false`。不调用加密后处理；不返回文件流。 | handoff mutex |
| **O01-C7** | 互斥优先于「policy 允许加密」：即使 `pdfEncryptionEnabled=true`，只要本请求 `encryption.enabled=true` 且 profile 为 `PDF_A_2B`，仍 400。`encryption.enabled=false` / 省略且无非法子字段 → 允许 PDF/A 生成。 | ADR-0001 + O01 |
| **O01-C8** | **DOCX + encryption** 与 `pdfArchivalProfile` **无互斥**（归档过滤器不作用于 DOCX）。 | O01-C4 |
| **O01-C9** | **后处理与 PDF/A：** Runtime / 正式生成路径在 `PDF_A_2B` 下 **禁止** PDFBox 加密后处理（由 O01-C6 保证）。页码 stamp：优先依赖转换前 DOCX 准备；若 `finishPdf` 会改写 PDF 字节，实现须保证不破坏归档符合性，否则跳过 stamp 并记 fidelity warning，**不得**静默交付已声明 PDF/A 却明显非归档的文件。 | 技术边界 |
| **O01-C10** | **Preview / test-generate：** 仍可读锁定 profile 走 LO 过滤器；SPECIMEN 水印（CE-G02）可在预览件后加盖——**预览件不作为 PDF/A 合规 Done 证据**。正式 runtime 产物 + 金标为 SoT。 | G02 + O01 |
| **O01-C11** | **配置面本片：** 通过默认 render profile 资产（如 `authoring/default-render-profile-v1.json`）与/或已锁定 `render_profile_json` 声明字段。**无**管理端 UI、**无** runtime 请求字段设置归档级别。 | FE out of scope |
| **O01-C12** | **金标：** 新增或激活至少 1 个主题包（建议 `11-pdfa-2b` 或等价）；`pdfArchivalProfile=PDF_A_2B`；断言 = veraPDF CLI/库（若 verify 环境可重复获得）**或**轻量检查（存在 PDF/A 标识：`pdfaid:part`/`pdfaid:conformance` 或等价 catalog/XMP 证据指向 part=2 + conformance=B）。禁止像素比对。接入 `mvn verify`。 | K07 + handoff |
| **O01-C13** | **ADR-0058 Accepted** 记录：选 A-2b 而非 A-1b（LO 支持更好）；互斥加密；配置在 render profile。 | D6 |
| **O01-C14** | **明确非目标：** CE-O02 addressBlock/多文档包；管理端 profile 编辑器；A-1b；调用方覆盖归档级别；预览件 veraPDF 门禁；go-live；CD-3；正式 P-phase。 | handoff OOS |
| **O01-C15** | 未知枚举值（非 `NONE`/`PDF_A_2B`）→ 发布/解析 fail-closed：`422` 或配置校验错误（稳定 `messageKey=api.error.rendering.renderProfileInvalid` 族），不得默认为 `NONE` 静默吞掉显式非法值。 | fail-closed |
| **O01-C16** | 审计/元数据：生成成功时可在非敏感摘要中记录 `pdfArchivalProfile`（枚举）；**不得**因本片记录加密密码。 | 可观测 |
| **O01-C17** | **FE / E2E / UIUX：本片 out of scope。** Done 主证据 = 后端契约/单测 + 金标 + `mvn verify` +（若验收面变更）queued deploy；无 Playwright 义务。 | E2E 路由 |

---

## 5. 前置条件

- CE-K07 金标 harness 已合并并可扩展 ACTIVE 包。
- LibreOffice headless 转换路径可用（cli / Docker exec）。
- PDF 动态加密与 `EncryptionParameterValidator` 已存在。
- 发布锁定 `RenderProfile` / `lockForPublish` 已存在。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- Runtime（或 preview）请求生成 `output.format=PDF`，且解析到的 `RenderProfile.pdfArchivalProfile` 为 `NONE` 或 `PDF_A_2B`。
- 同上且请求携带 `encryption.enabled=true`（互斥路径）。
- `mvn verify` 执行金标 ACTIVE 包（含 PDF/A 主题）。

---

## 7. Primary journey

### 7.1 常规 PDF（默认）

1. Release 锁定 profile `pdfArchivalProfile=NONE`（或缺省）。
2. 调用方请求 PDF，不启用加密。
3. 系统走既有 LO `pdf` 转换 → 可选 stamp → 返回常规 PDF。

### 7.2 PDF/A-2b 归档输出

1. 平台/发布快照将 `pdfArchivalProfile=PDF_A_2B` 写入锁定 profile。
2. 调用方请求 PDF，`encryption.enabled` 不为 true。
3. 系统使用 LO PDF/A-2b 过滤器转换。
4. 产出通过金标归档校验；响应成功；摘要可含 `pdfArchivalProfile=PDF_A_2B`。

### 7.3 互斥拒绝

1. 锁定 profile 为 `PDF_A_2B`。
2. 调用方请求 PDF 且 `encryption.enabled=true`（合法密码等）。
3. 系统在转换前返回 400 `PDF_ARCHIVAL_ENCRYPTION_MUTEX`；无产物；无密码泄露。

---

## 8. System responses

| 路径 | HTTP / 结果 | 要点 |
| --- | --- | --- |
| `NONE` + PDF | 200 / 既有成功 | 常规 PDF；行为与本片前兼容 |
| `PDF_A_2B` + PDF + 无加密 | 200 / 成功 | LO PDF/A-2b；归档校验通过 |
| `PDF_A_2B` + PDF + 加密 | **400** | `PDF_ARCHIVAL_ENCRYPTION_MUTEX`；`retryable=false` |
| `PDF_A_2B` + DOCX | 200 / 成功 | 无 PDF/A 过滤器；DOCX 加密规则不变 |
| 非法枚举写入 profile | 422 / 配置无效 | 不得静默当 `NONE` |
| Caller 试图覆盖 profile | 忽略覆盖 | 锁定值生效 |

---

## 9. Acceptance scenarios (Given / When / Then)

### BDD-CE-O01-001 — 缺省为 NONE（向后兼容）

**Given** 锁定或默认 `RenderProfile` 未含 `pdfArchivalProfile` 字段  
**When** 系统解析 `RenderProfile`  
**Then** 有效值为 `NONE`  
**And** PDF 转换使用既有常规 `pdf` 路径（非 PDF/A 过滤器）

### BDD-CE-O01-002 — 显式 NONE 常规 PDF

**Given** `pdfArchivalProfile=NONE`  
**And** 合法 runtime 请求 `output.format=PDF`，未启用加密  
**When** 生成完成  
**Then** 返回成功 PDF  
**And** 金标/断言不要求 PDF/A 标识

### BDD-CE-O01-003 — PDF_A_2B 使用 LO PDF/A-2b 过滤器

**Given** 发布锁定 `pdfArchivalProfile=PDF_A_2B`  
**And** 请求 `output.format=PDF`，`encryption.enabled` 不为 true  
**When** 系统执行 PDF 转换  
**Then** LibreOffice 调用使用 PDF/A-2b 导出过滤器（非裸 `pdf`）  
**And** 产出通过 O01-C12 归档校验

### BDD-CE-O01-004 — DOCX 忽略归档过滤器

**Given** `pdfArchivalProfile=PDF_A_2B`  
**And** 请求 `output.format=DOCX`  
**When** 生成完成  
**Then** 不调用 PDF/A 过滤器  
**And** DOCX 产物成功（加密规则按既有 ADR-0001 / CE-C06）

### BDD-CE-O01-005 — 加密互斥硬拒绝

**Given** `pdfArchivalProfile=PDF_A_2B`  
**And** 请求 `output.format=PDF` 且 `encryption.enabled=true`（其余加密参数结构合法，且 API policy 允许 PDF 加密）  
**When** 提交生成  
**Then** HTTP **400**  
**And** `error.code=PDF_ARCHIVAL_ENCRYPTION_MUTEX`  
**And** `messageKey=api.error.generation.pdfArchivalEncryptionMutex`  
**And** `retryable=false`  
**And** 无文档产物返回  
**And** 日志/响应不含密码明文

### BDD-CE-O01-006 — 加密关闭时允许 PDF/A

**Given** `pdfArchivalProfile=PDF_A_2B`  
**And** `encryption.enabled=false` 或省略且无非法加密子字段  
**When** 请求 PDF 生成  
**Then** 走 BDD-CE-O01-003 成功路径

### BDD-CE-O01-007 — 调用方不可覆盖

**Given** 锁定 profile `pdfArchivalProfile=PDF_A_2B`  
**And** 调用方提供任意 render override / 未文档化请求字段试图改为 `NONE`  
**When** 解析有效 profile  
**Then** 仍为 `PDF_A_2B`  
**And** 转换仍使用 PDF/A-2b 过滤器

### BDD-CE-O01-008 — 非法枚举 fail-closed

**Given** profile JSON 含 `pdfArchivalProfile` 为未知值（如 `PDF_A_1B`）  
**When** 发布锁定或运行时解析该 profile  
**Then** 失败（配置/校验错误，`renderProfileInvalid` 族）  
**And** **不得**静默降级为 `NONE`

### BDD-CE-O01-009 — 发布快照持久化字段

**Given** 默认或草稿 profile 含 `pdfArchivalProfile=PDF_A_2B`  
**When** 模板发布 `lockForPublish`  
**Then** `template_version.render_profile_json` 含 `"pdfArchivalProfile":"PDF_A_2B"`  
**And** 运行时装配读取该快照

### BDD-CE-O01-010 — 金标 ACTIVE 包归档断言

**Given** 金标主题包配置 `pdfArchivalProfile=PDF_A_2B` 且状态 ACTIVE  
**When** `mvn verify` 执行金标 harness  
**Then** 包通过 veraPDF **或**轻量 PDF/A-2b 标识断言  
**And** 不使用像素比对

### BDD-CE-O01-011 — 与 encrypted-pdf 金标正交

**Given** 既有 ACTIVE `07-encrypted-pdf`（或等价）要求加密 PDF  
**When** 该包执行  
**Then** 其 profile **不得**同时为 `PDF_A_2B`（或请求侧不启用归档）  
**And** 本片不破坏既有加密金标绿灯

### BDD-CE-O01-012 — Preview 非合规门禁

**Given** preview/test-generate 使用 `PDF_A_2B` 且可能加 SPECIMEN 水印  
**When** 预览完成  
**Then** 预览成功不因「预览件未过 veraPDF」而失败  
**And** 正式 runtime + 金标仍为合规 SoT

### BDD-CE-O01-013 — ADR-0058 存在且 Accepted

**Given** 本片文档集  
**When** 审查 ADRs  
**Then** [ADR-0058](../adr/rendering-authoring/0058-pdfa-2b-archival-output.md) 状态 **Accepted**  
**And** 正文记录选 A-2b、拒绝 A-1b 本片、与加密互斥

### BDD-CE-O01-014 — FE / E2E 非目标

**Given** 本片 Done 定义  
**When** 评估 E2E/UIUX 门禁  
**Then** **不**要求 Playwright 功能/UIUX 套件  
**And** 无新管理端 PDF/A 配置旅程

### BDD-CE-O01-015 — 批量单笔互斥

**Given** 批量请求中某笔 `output.format=PDF`、`encryption.enabled=true`，且该笔解析到的 release profile 为 `PDF_A_2B`  
**When** 处理该笔  
**Then** 该笔失败，错误码同 BDD-CE-O01-005  
**And** 其他无冲突笔按既有批量语义继续（全批失败策略不变于本片）

### BDD-CE-O01-016 — 英文优先 messageKey

**Given** 互斥错误响应  
**When** 读取 `messageKey`  
**Then** 为 `api.error.generation.pdfArchivalEncryptionMutex`  
**And** `messages_en.properties` 有对应英文文案（实现期）

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| PDF/A + 加密 | 400 mutex；绝不先转换再加密 |
| PDF/A + DOCX 加密 | 允许（无 PDF 过滤器） |
| PDF/A + 页码 stamp | 不得破坏归档；否则 skip + warning |
| PDF/A + SPECIMEN 预览 | 允许；预览非合规门禁 |
| 未知枚举 | fail-closed |
| Caller override | 忽略 |
| CE-O02 | 本片不做 |
| 权限 | 无新码；既有 fail-closed |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API error envelope | `PDF_ARCHIVAL_ENCRYPTION_MUTEX` + messageKey |
| PDF 字节 | 金标归档断言（veraPDF 或 XMP/`pdfaid`） |
| `render_profile_json` | 含枚举字段 |
| LO 命令/过滤器 | 单测或集成断言过滤器选择 |
| ADR-0058 | Accepted |
| `mvn verify` | 含新金标 ACTIVE |
| **非证据** | Playwright、管理 UI 截图 |

---

## 12. Traceability

| 工件 | 关系 |
| --- | --- |
| Task Master **#81** | 本片交付 |
| CE 计划 §8 CE-O01 / §10 D6 | 产品决策 |
| CE-K07 #54 | 金标 harness |
| ADR-0001 | 加密基线；本片 mutex 互补 |
| ADR-0058 | A-2b 选型 |
| domain-model §2.6.8 | profile 维度扩展 |
| requirements-plan / PRD | 确认条目 |
| CE-O02 | **Skipped**（D5） |

---

## 13. Open questions

**None** — D6、枚举、LO 过滤器、加密互斥、ADR、金标校验策略、FE out of scope 均已确认。实现期可在「veraPDF 嵌入 vs 轻量 XMP」间择一满足 O01-C12，不阻塞 BDD `ready`。

---

## 14. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-o01-pdfa-output.md
task_ids: ["81"]
frontend_ui_in_scope: false
e2e_routing: API/render-only — skip e2e-test-engineer / e2e-uiux-reviewer for this slice
```
