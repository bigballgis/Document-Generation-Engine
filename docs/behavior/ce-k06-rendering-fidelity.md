# CE-K06 渲染保真补全 — BDD（Wave 1a = K06a）

| Field | Value |
| --- | --- |
| **Slice** | `ce-k06-rendering-fidelity` |
| **Plan task** | **CE-K06**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §3 CE-K06） |
| **Task Master** | **#62** |
| **bdd_readiness** | **`ready`**（本片确认范围 = **K06a**；K06b/K06c 为 residual 验收桩，不阻塞本片 TDD） |
| **Recorded** | 2026-07-15 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k06-rendering-fidelity` · `feat/ce-k06-rendering-fidelity` |
| **Scope of this slice** | **K06a only** — `repeatHeaderAcrossPages` → DOCX writer 落 `<w:tblHeader/>`；金标 `02-cross-page-table` PLACEHOLDER → **ACTIVE**；fail-first 回归。**不**实现 K06b（`qrBarcodeRef` writer）/ K06c（`attachmentListRef` writer + PDF stamp profile 收敛）；**不** go-live；**不**激活 CD-3；**不**改 CE-C04 / CE-U06 |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；产品 [PRD.md](../product/PRD.md) §6.5 表格跨页；需求 [requirements-plan.md](../requirements/requirements-plan.md) 表格组件；领域 [domain-model.md](../domain/domain-model.md) §2.6.4；金标约定 [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md)；writer-unsupported 正交 [lrp-a4-fail-closed-unsupported-nodes.md](./lrp-a4-fail-closed-unsupported-nodes.md) |

---

## 1. 概述

CE-K06 关闭渲染保真三类缺口。本 Wave 1a 切片 **只交付 K06a**（跨页表头），其余子片记录为 residual。

| 子片 | 本片状态 | 痛点摘要 |
| --- | --- | --- |
| **K06a** | **In scope / ready** | `TableComponentService` / 绑定已接受 `repeatHeaderAcrossPages`，但 `StructuredContentDocxTableSupport` 写表头行时 **不** 写 OOXML `<w:tblHeader/>`，Word 跨页时表头不重复 |
| **K06b** | **Deferred residual** | `qrBarcodeRef` 在节点矩阵与 LR-A4 writer-unsupported set；无 ZXing 图片嵌入 writer |
| **K06c** | **Deferred residual** | `attachmentListRef` 无编号列表段落 writer；PDF 页码 stamp 与全局 `pdf-page-number-stamping-enabled` / `renderProfile.pdfPageNumberStampingEnabled` 收敛未完结 |

**现状证据（K06a）：**

- `TableComponentRenderModel.repeatHeaderAcrossPages` 已从 JSON 解析并有单测覆盖。
- `StructuredContentDocxTableSupport.populateTable` 写 header 行时仅套 `TableHeader` 段落样式，**不**设置行级 `w:tblHeader`。
- 金标包 `02-cross-page-table` 成熟度仍为 **PLACEHOLDER**（K07 骨架；`docx-assertions.json` 为 `deferred: true`）。

---

## 2. Actor / Role

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **系统（Rendering pipeline / DocxAssembler）** | 结构化表格 → DOCX 发射 | `repeatHeaderAcrossPages=true` 时表头行带 `w:tblHeader` |
| **Runtime / Preview 生成路径** | 消费已发布绑定与变量 | 产物 DOCX 跨页表头可被 Word 重复；XML 可断言 |
| **平台 / CI** | 金标 harness + `mvn verify` | `cross-page-table` ACTIVE；XPath 失败即红 |
| **（间接）函件读者 / 法务** | 多页表格可读性 | 续页仍见列标题；不丢关键列语义 |

本片 **无** 新管理 UI 旅程、**无** 新公开 API 契约变更（作者侧 `repeatHeaderAcrossPages` 字段已存在）。

---

## 3. Goal（K06a）

1. 当表格组件定义（或等价绑定）`repeatHeaderAcrossPages == true` 时，DOCX writer 对 **每一个** 标记为表头的行写入 OOXML **`<w:tblHeader/>`**（位于该行 `w:trPr` 下）。
2. 当 `repeatHeaderAcrossPages == false` 或缺失（默认 false）时，**不得** 在表头行写入 `w:tblHeader`（保持非跨页重复语义）。
3. 金标 `backend/src/test/resources/golden-corpus/02-cross-page-table/` 由 PLACEHOLDER → **ACTIVE**：输入含足以跨页的循环行数据；`expected/docx-assertions.json` 用稳定 XPath/关键路径断言至少一个 `w:tblHeader` 存在；禁像素比对。
4. 回归：单元/装配测试 **fail-first** 再绿；`mvn verify` 执行金标 DOCX 半段（PDF 半段对齐 K07：无 soffice 可 skip）。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **K06-C1** | **本片交付范围 = K06a only。** K06b / K06c 不进入本片实现；验收桩见 §12；不阻塞 `bdd_readiness=ready`。 | 编排 handoff；CE-K06 可独立子片 |
| **K06-C2** | **触发字段：** 表格组件 JSON 布尔 `repeatHeaderAcrossPages`（已由 `TableComponentService` / `TableComponentRenderModel` 校验与建模）。等价绑定若经 `tableComponent` / `tableComponentRef` 内联同一字段，行为相同。 | 计划卡；domain §2.6.4 |
| **K06-C3** | **发射目标：** 对 `headerRows` 中写出的表头行（当前实现至少第一表头行；若多表头行均作为 header 写出，则 **每一** 表头行均带 `w:tblHeader`）写入 `<w:tblHeader/>`。循环行与 footer 行 **不得** 带 `w:tblHeader`。 | OOXML `CT_TrPr` / Word「标题行重复」；计划卡 |
| **K06-C4** | **实现落点：** `StructuredContentDocxTableSupport`（或同源 table writer）在 `repeatHeaderAcrossPages==true` 时设置行属性；优先经 POI 稳定 API（如 `XWPFTableRow` / `CTTrPr`）写出，保证解压后 `word/document.xml` XPath 可匹配 `//w:tblHeader`（或行级等价路径）。 | 代码现状 |
| **K06-C5** | **`false` / 缺省：** 不写 `w:tblHeader`；不得因「看起来像表头样式」而隐式加 tblHeader。 | fail-closed 语义清晰 |
| **K06-C6** | **金标：** 复用 K07 包 `cross-page-table`（目录 `02-cross-page-table`）；充实 input（母版 + 含 `repeatHeaderAcrossPages: true` 的 table 绑定 + 足够行数的变量，使表格在典型 A4 下可跨 ≥2 页）；`maturity: ACTIVE`；DOCX 断言至少：存在 `w:tblHeader`；可选断言表头单元格文本。**禁止** 像素/截图 golden。PDF 文本断言可选（有 soffice 时）；无 soffice → skip PDF 半段（K07-C9）。 | CE-K06；CE-K07 |
| **K06-C7** | **可观察证据：** (1) 单元/装配测试对写出的 DOCX 字节解压后 XPath 命中 `w:tblHeader`；(2) 金标 ACTIVE 包在 `mvn verify` 绿灯；(3) 负例：`repeatHeaderAcrossPages=false` 时 XPath **不** 命中。 | TDD |
| **K06-C8** | **与 LR-A4 正交：** 本片 **不** 从 `WriterUnsupportedStructuredNodeTypes` 移除 `qrBarcodeRef` / `attachmentListRef`；发布硬阻断与显式渲染失败保持至 K06b/K06c 落地。 | LR-A4；K06-C1 |
| **K06-C9** | **非目标：** 嵌套表；浮动/绝对定位表；合并单元格跨页修复；新 UI 开关；调用方覆盖 `renderProfile`；K06b ZXing；K06c 附件列表 writer；全局 vs profile PDF stamp 重构；CE-C04 / CE-U06；CD-3；go-live。 | handoff |

---

## 5. Preconditions

- CE-K07 金标骨架存在；`02-cross-page-table` PLACEHOLDER 可被本片充实。
- CE-K02（样式来源）已 Done — 表头可用既有 `TableHeader` 样式；本片不改 style catalog 权威。
- `TableComponentService` 已接受并建模 `repeatHeaderAcrossPages`。
- CORE-FORTRESS F1 结构化 writer 路径可用（`tableComponent` / `tableComponentRef` → `populateTable`）。
- 隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- Runtime / preview / test-generate 装配含 `tableComponent`（或 `tableComponentRef`）且定义 `repeatHeaderAcrossPages: true` 的结构化内容。
- CI / 工程师执行 `mvn -B -ntp -f backend/pom.xml verify`（含 golden-corpus harness）。

---

## 7. Primary journey（K06a）

1. 作者（或演示/金标夹具）在表格组件定义中设置 `repeatHeaderAcrossPages: true`，并提供 `headerRows` + 足够多的 `loopRow` 数据。
2. 发布门禁通过既有表格校验（无 NESTED_TABLE 等 blocker）。
3. 渲染管线打开母版、写入锚点结构化内容，调用 table support 填充 `XWPFTable`。
4. Writer 对表头行写入 `w:tblHeader`。
5. 产物 DOCX 可被解压；`document.xml` XPath 断言通过；Word 打开时续页重复表头（人工/集成可选；自动化以 XML 断言为准）。
6. 金标 `cross-page-table` ACTIVE 在 verify 中执行并通过。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| `repeatHeaderAcrossPages=true` + 合法表头行 | 成功写出 DOCX；表头行含 `<w:tblHeader/>` |
| `repeatHeaderAcrossPages=false` 或缺失 | 成功写出 DOCX；表头行 **无** `w:tblHeader` |
| 表格其它既有 blocker（嵌套表等） | 保持既有发布/校验阻断；本片不放宽 |
| 金标 ACTIVE DOCX 断言失败 | `mvn verify` **FAIL** |
| 金标 PDF 半段且无 soffice | **SKIP** PDF；DOCX 仍必须 PASS |

---

## 9. Acceptance scenarios — K06a（Given / When / Then）

### BDD-CE-K06a-001 — 跨页表头写入 `w:tblHeader`

**Given** 结构化表格定义含非空 `headerRows`、合法 `columnSchema`，且 `"repeatHeaderAcrossPages": true`  
**And** 绑定变量使循环行足够填充表格  
**When** 渲染管线将 `tableComponent` / `tableComponentRef` 写入 DOCX  
**Then** 解压产物 `word/document.xml` 中至少存在一处 `w:tblHeader`（稳定 XPath 或关键字路径断言）  
**And** 该元素位于表头行的 `w:trPr`（或 POI 写出的等价结构）下

### BDD-CE-K06a-002 — 关闭跨页重复时不写 `tblHeader`

**Given** 同结构表格但 `"repeatHeaderAcrossPages": false`（或字段缺省）  
**When** 渲染写出 DOCX  
**Then** 该表的表头行 **不** 含 `w:tblHeader`  
**And** 文档仍成功生成（非错误路径）

### BDD-CE-K06a-003 — 仅表头行带 `tblHeader`

**Given** `repeatHeaderAcrossPages: true`，且存在 loop 数据行与（可选）footer 行  
**When** 渲染写出 DOCX  
**Then** `w:tblHeader` 仅出现在 header 行对应 `w:tr`  
**And** loop / footer 行的 `w:trPr` **不含** `w:tblHeader`

### BDD-CE-K06a-004 — 金标 `cross-page-table` → ACTIVE

**Given** 本片交付完成  
**When** 检查 `golden-corpus/02-cross-page-table/manifest.json`  
**Then** `maturity` 为 `ACTIVE`  
**And** `input/` 含可渲染母版 + 带 `repeatHeaderAcrossPages: true` 的模板/绑定 + 变量（行数足以体现跨页意图）  
**And** `expected/docx-assertions.json` 非 `deferred`，且断言 `w:tblHeader` 存在  
**And** harness 在 `mvn verify` 中执行该包 DOCX 半段并通过

### BDD-CE-K06a-005 — Fail-first 回归

**Given** 实现前（或故意破坏 tblHeader 写出）  
**When** 运行针对 `repeatHeaderAcrossPages=true` 的 writer / 装配测试或金标断言  
**Then** 测试失败（Red）  
**And** 最小实现写出 `w:tblHeader` 后同测试通过（Green）

### BDD-CE-K06a-006 — 多表头行（若写出）

**Given** 定义含多个 `headerRows` 且 writer 将每一行作为表头写出，且 `repeatHeaderAcrossPages: true`  
**When** 渲染写出 DOCX  
**Then** 每一个写出的表头行均带 `w:tblHeader`  
**Note** 若 v1 writer 仍只写第一表头行，则本场景退化为对第一行的断言；不得静默丢弃后续表头行而不记录（行为与现网一致即可，本片不强制扩展多表头写出能力）。

---

## 10. Boundary / exception（K06a）

| 边界 | 行为 |
| --- | --- |
| 空 `headerRows` 但 `repeatHeaderAcrossPages=true` | 无表头行可标 → 不强制人造空行；既有校验若已 blocker 则保持；否则不写 `w:tblHeader` |
| `columnKeys` 为空 | 保持现网 early-return；不新增成功路径 |
| 未知/非法表格 | 既有 `INVALID_TABLE_COMPONENT` / 发布阻断不变 |
| 像素断言 | **禁止** |
| Authorization | 无新权限码；沿用既有生成/预览授权 fail-closed |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| DOCX XML | XPath / 关键字：`w:tblHeader` 存在性与行归属 |
| 金标 | `02-cross-page-table` ACTIVE + verify |
| 单测 | `StructuredContentDocxWriterTest` / table support / assembler 回归 |
| 审计 | 无新审计事件要求（渲染保真内部行为） |

---

## 12. Residual acceptance stubs（K06b / K06c — deferred）

> **Status:** 规格意图已记录，**不**作为本片 Done 门槛；后续切片充实为完整 BDD 并实现。保持 LR-A4 fail-closed 直至 writer 落地并从 unsupported set 移除。

### 12.1 K06b — `qrBarcodeRef` writer（deferred）

| Field | Stub |
| --- | --- |
| **Actor** | Rendering pipeline |
| **Goal** | 对合法 `qrBarcodeRef` 用 ZXing（或平台批准等价库）生成二维码/条码图片并嵌入 DOCX；尺寸与纠错级别可配置 |
| **Then (stub)** | 节点不再属于 writer-unsupported；发布门禁放行；DOCX 含嵌入图片；缺失 reference / 非法配置 fail-closed（显式错误，禁止静默省略） |
| **Out of this slice** | 依赖引入、图片缓存、CE-E02 资产键名深化 |

**Stub scenarios:**

- **BDD-CE-K06b-001 (deferred):** Given 绑定含合法 `qrBarcodeRef`，When 渲染，Then DOCX 含 QR/barcode 图片 part，且 `WriterUnsupportedStructuredNodeTypes` 不再包含 `qrBarcodeRef`。
- **BDD-CE-K06b-002 (deferred):** Given 可配置 size / error-correction，When 渲染，Then 嵌入图反映配置；非法配置 → 显式失败。

### 12.2 K06c — `attachmentListRef` + PDF stamp via render profile（deferred）

| Field | Stub |
| --- | --- |
| **Actor** | Rendering pipeline + PDF post-process |
| **Goal** | `attachmentListRef` → 结构化附件清单的编号列表段落；PDF 页码 stamp **按发布锁定 `renderProfile`（包级）** 控制，而非仅依赖全局应用布尔 |
| **Then (stub)** | 附件列表可见编号段落；`pdfPageNumberStampingEnabled`（或等价 profile 字段）驱动 stamp；全局默认不得绕过已锁定 profile |
| **Out of this slice** | 完整 stamp 策略重构、附件元数据 UI |

**Stub scenarios:**

- **BDD-CE-K06c-001 (deferred):** Given 合法 `attachmentListRef` + 附件清单数据，When 渲染，Then DOCX 含编号列表段落；节点退出 writer-unsupported set。
- **BDD-CE-K06c-002 (deferred):** Given 包级 `renderProfile.pdfPageNumberStampingEnabled=true|false`，When PDF 转换，Then stamp 启用与否跟随 **profile**（调用方不可覆盖）；与全局 `pdf-page-number-stamping-enabled` 的优先级在实现片锁定并写 ADR/规格。

---

## 13. Traceability

| Source | Link |
| --- | --- |
| Plan card | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) § CE-K06 |
| Task Master | `#62` CE-K06 |
| PRD | [PRD.md](../product/PRD.md) — 表格跨页表头完整；v1 表格组件「跨页重复表头」 |
| Requirements | [requirements-plan.md](../requirements/requirements-plan.md) — 同左 |
| Domain | [domain-model.md](../domain/domain-model.md) §2.6.4 |
| Golden corpus | [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md) BDD-CE-K07-012 → 本片充实 |
| Writer-unsupported | [lrp-a4-fail-closed-unsupported-nodes.md](./lrp-a4-fail-closed-unsupported-nodes.md) — K06b/c 前保持 |
| P18 table | [P18-structured-authoring-fidelity-engine.md](../plan/detail/P18-structured-authoring-fidelity-engine.md) P18-T04 `repeatHeader_acrossPages_preserved` |

---

## 14. BDD readiness

```
bdd_readiness: ready
k06_scope_this_slice: a
residual: K06b (qrBarcodeRef/ZXing), K06c (attachmentListRef + PDF stamp via render profile)
open_questions: []
owning_doc: docs/behavior/ce-k06-rendering-fidelity.md
task_ids: [#62, CE-K06, K06a]
```

**Next:** `plan-orchestrator` → TDD tasks for K06a only（failing tests → `w:tblHeader` writer → golden ACTIVE → `mvn verify`）。
