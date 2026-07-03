# BDD 行为规格：综合演示包扩展与渲染保真（Demo Expansion & Rendering Fidelity）

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-03  
**BDD ID 前缀**: `BDD-DEMO-EXP`  
**交付切片**: 单次交付（ONE delivery slice）— 渲染保真 + 双页码 + 八类银行信函演示包

---

## 目录

1. [概述](#1-概述)
2. [Actor / Role](#2-actor--role)
3. [Goal（用户目标）](#3-goal用户目标)
4. [Trigger（触发条件）](#4-trigger触发条件)
5. [Preconditions（前置条件）](#5-preconditions前置条件)
6. [Primary Journey（主路径）](#6-primary-journey主路径)
7. [System Responses（系统响应）](#7-system-responses系统响应)
8. [Acceptance Scenarios（Given/When/Then）](#8-acceptance-scenarios-givenwhenthen)
9. [边界与异常行为](#9-边界与异常行为)
10. [可观测证据](#10-可观测证据)
11. [文档类型矩阵（八类演示）](#11-文档类型矩阵八类演示)
12. [演示包结构契约](#12-演示包结构契约)
13. [BDD 就绪声明](#13-bdd-就绪声明)
14. [可追溯性](#14-可追溯性)

---

## 1. 概述

本规格描述**单次交付切片**，在现有 `deploy/demo-fol/` 与 P18 结构化创作模型基础上，实现：

| # | 能力域 | 说明 |
|---|--------|------|
| R1 | **结构化内容 → DOCX 渲染保真** | 将 P18 v1 节点矩阵（emphasis、underline、list、styleRef、tableComponent、编号等）从纯文本降级路径升级为真实 Word 元素输出 |
| R2 | **双页码语义** | 同一文档同时支持**章节级**（section page X of Y）与**文档全局**（document page X of Y）页码，DOCX 与 PDF 一致 |
| R3 | **按演示可配置页脚版式** | 各信函类型采用贴近真实银行文档的页眉/页脚布局（非单一 FOL 模板） |
| R4 | **八类银行信函演示包** | 覆盖批发 FOL 升级 + 七类新增零售/贸易/催收/财富等信函；版式与文案极度贴近真实文档，仅数据为 mock |
| R5 | **可重复导入的演示包结构** | 各演示包镜像 `demo-fol` 目录契约（assets、config、sql、import 脚本） |

**当前技术缺口（约束，非待确认假设）**：

- `DocxAssembler.renderStructuredContent` 将结构化树渲染为纯文本；`writeParagraphText` 统一 Calibri 10pt，不应用 emphasis/list/underline/styleRef/编号。
- 页码：FOL 母版页脚仅含全局 `PAGE` 字段；PDF 经 `PdfPageNumberStamper` 加盖 `Page N of Total`；无 section restart、无 `SECTIONPAGES`、无双页码并存。
- 演示：仅 `deploy/demo-fol/` 为完整 executive demo；`DemoCatalogSeeder` 为最小零售草稿种子。

---

## 2. Actor / Role

| Actor | 角色描述 | 权限 / 范围 |
|-------|---------|-------------|
| **平台工程师（Platform Engineer）** | 维护演示包、导入脚本、构建期母版资产生成器 | 仓库 `deploy/demo-*`、CI 构建测试 |
| **母版设计人员（Master Designer）** | 上传/审核含多节版式与双页码页脚的 DOCX 母版 | `MASTER_DESIGNER`；组范围 CORP / RETAIL / TRADE / WEALTH |
| **模板编排人员（Template Author）** | 配置锚点结构化内容、变量、规则、测试数据集 | `canAuthorTemplates`；对应演示组 |
| **模板测试人员（Tester）** | 对演示模板执行预览/全量测试，验证 DOCX/PDF 保真 | `canDecideTemplateTests` |
| **API 调用方（Runtime Caller）** | 对已发布演示模板调用 runtime generate | 模板级 API 凭证 + AD Group |
| **系统（Rendering Pipeline）** | `DocxAssembler`、PDF 转换、`PdfPageNumberStamper`、`RenderProfile` | 内部；发布锁定配置，调用方不可覆盖 |

> 演示包导入与母版资产生成以**平台工程师**自动化脚本为主；人工角色用于验收与 E2E 旅程。

---

## 3. Goal（用户目标）

1. **R1**：模板作者在结构化编辑器中使用的节点（强调、下划线、列表、样式引用、表格组件、条件/循环块等）在最终 DOCX 中可见且语义正确，而非退化为纯文本。
2. **R2**：多节长文档（尤其批发 FOL）页脚同时显示章节页码与全文页码，符合真实银团/批发信贷信函惯例。
3. **R3**：八类信函各有独立、逼真的页眉/页脚/版式（地址行、免责声明、监管标识、表格页脚等），而非共用单一 FOL 页脚。
4. **R4**：销售/验收/培训可在 Docker 部署后一键导入全部演示，生成 DOCX/PDF 样例，视觉与真实银行信函不可区分（数据为虚构）。
5. **R5**：演示包可版本化、可重复导入（幂等 marker）、与现有 FOL import 脚本模式一致。

---

## 4. Trigger（触发条件）

| 触发 | 说明 |
|------|------|
| **T1 — 演示导入** | 运维执行 `deploy/import-all-demos.ps1`（或各 `deploy/demo-*/import-*-demo.ps1`） |
| **T2 — 构建期母版生成** | `mvn test` 中 `*MasterDocxAssetGeneratorTest` 写入 `deploy/demo-*/assets/*.docx` |
| **T3 — 管理面预览/测试** | 模板作者在测试 Tab 对演示模板运行预览或全量测试 |
| **T4 — Runtime 生成** | 已发布演示模板的 `generate` API 调用（sync 文件流或 download URL） |

---

## 5. Preconditions（前置条件）

1. 平台已通过 `mvn verify` 与前端 gates；Docker 栈健康（`backend /healthz`、`frontend 4173`）。
2. P18 节点矩阵、母版样式目录、`renderProfile` 发布锁定已存在（P18 Done）。
3. LibreOffice headless 可用于 PDF 转换（或测试 profile 下 PDFBox 占位 + stamper 路径可测）。
4. 演示用 AD 组 / 管理会话种子存在：`CORP`、`RETAIL`、`TRADE`、`WEALTH`（或等价组代码，与演示包 manifest 一致）。
5. 导入脚本所需 PostgreSQL 容器与 management API 可达。
6. FOL 升级前已执行 `deploy/demo-fol/generate-fol-catalog.ps1`（保持现有 hybrid anchor 契约）。

---

## 6. Primary Journey（主路径）

### 6.1 平台工程师 — 全量演示导入

1. 从仓库根目录执行 `.\deploy\import-all-demos.ps1`（或按类型单独 import）。
2. 脚本按优先级顺序：SQL 种子（content modules）→ 生成/读取 catalog JSON → Management API 上传母版 → 创建/更新模板 → 绑定锚点结构化内容 → 写入测试数据集。
3. 各包写入 `catalogMarker`；重复导入时检测 marker 跳过或增量更新（与 FOL `fol-exec-demo-v*` 模式一致）。
4. 批发 FOL 模板发布至可调用状态（`DemoFullFlowCatalogSeeder` 或 import 脚本末段）；其余类型至少达到**已发布 + 测试数据集齐全**。

### 6.2 模板作者 — 保真验证

1. 打开演示模板（如 `DEMO-FOL-WHOLESALE`）开发版本 → 测试 Tab。
2. 选择 executive 测试数据集 → 运行预览（DOCX + PDF）。
3. 检查：结构化样式在 DOCX 中保留；多节文档页脚双页码正确；PDF 与 DOCX 页码语义一致。
4. 全量测试通过；覆盖率与门禁满足 P12 阈值（FOL 演示沿用现有 executive 数据集规模）。

### 6.3 Runtime 调用方 — 生成样例

1. 使用已授权 API 凭证调用 `POST .../templates/{id}/versions/{release}/generate`。
2. 接收 DOCX/PDF；页码与页脚版式与预览一致；`result.fidelityWarnings[]` 无未预期的阻断级问题。

---

## 7. System Responses（系统响应）

### 7.1 结构化内容 DOCX 写入（R1）

- 渲染管线将结构化内容树写入锚点占位符时，**按节点类型生成对应 Word 构造**，而非 `renderStructuredContent` → 纯文本 → 单一样式 run。
- 支持的 v1 节点最低集（本切片必须覆盖）：

| 节点类型 | DOCX 输出要求 |
|---------|--------------|
| `paragraph` | 独立段落；支持子 run 组合 |
| `sectionHeading` | 标题段落；应用 `styleRef` 或母版目录中的 Heading 样式 |
| `list` | Word 编号/项目符号列表（`numId`/`ilvl` 或等价 POI 构造）；有序与无序 |
| `emphasis` | `bold` / `italic` / `boldItalic` 映射到 run 属性 |
| `underline` | run 下划线属性 |
| `styleRef` | 解析母版样式目录，应用到段落或 run |
| `lineBreak` | 软换行 |
| `variable` / `textRun` | 变量替换与字面文本 |
| `tableComponent` / `tableComponentRef` | 真实 `XWPFTable`：表头、循环行、footer 行、repeat header 元数据 |
| `conditionBlock` / `loopBlock` | 条件/循环语义保持；循环后编号确定性重排（与 `NumberingService` 一致） |
| `contentModuleRef` | 展开引用的模块结构并递归写入 |
| `imageRef` / `sealRef` | 嵌入图片 run（演示资产来自 `assets/`）；签章在授权区域内 |

- 发布锁定的 `renderProfile` 继续约束样式/编号/表格分页；调用方不得覆盖。

### 7.2 双页码（R2）

**语义定义（已确认）**：

| 页码类型 | 含义 | Word 字段（实现参考） | 示例标签 |
|---------|------|----------------------|---------|
| **章节级（Section）** | 当前 Word **节**内页序 | `PAGE` + `SECTIONPAGES`（该节启用页码重启时） | `Section Page 2 of 15` |
| **文档全局（Document）** | 整份文档物理页序 | 连续编号的 `PAGE` + `NUMPAGES`（或等价全局计数） | `Page 47 of 120` |

- 同一页脚可**同时展示**两种页码（版式因演示而异：同行、分两行、左章节右全局等）。
- 母版通过 **分节符（section break）** 划分章节；需要章节重启的节在 `sectPr` 中配置 `pgNumType` restart。
- **DOCX**：打开生成的文档时，字段更新后章节页码与全局页码均正确（验收时以 Word 或 POI 字段解析为准）。
- **PDF**：转换后页码与 DOCX 语义一致。若 LibreOffice 省略字段求值，则 `PdfPageNumberStamper`（或后继组件）必须能按节边界加盖**双页码**（非仅全局 `Page N of Total`）；`renderProfile.pdfPageNumberStampingEnabled` 控制是否启用 PDF 加盖。

### 7.3 按演示页脚版式（R3）

- 每种信函类型的母版 DOCX 自带独立 `configureDefaultHeader` / `configureDefaultFooter`（或等价静态资产），定义：
  - 银行品牌、地址、监管声明
  - 保密/免责声明
  - 页码区布局（单页码、双页码、表格续页脚等）
- 页脚版式在**母版资产**中定义，不由 runtime 变量拼接（符合 ADR：母版负责页眉页脚）。
- 演示包 `config/*-template-config.json` 记录 `masterLayoutVersion` 用于导入幂等。

### 7.4 八类演示包（R4）

见 [§11 文档类型矩阵](#11-文档类型矩阵八类演示)。每类至少一个**已发布**模板、≥1 个 executive 级测试数据集、可生成 DOCX+PDF。

### 7.5 演示包结构（R5）

见 [§12 演示包结构契约](#12-演示包结构契约)。

---

## 8. Acceptance Scenarios（Given/When/Then）

### 8.1 结构化样式 — emphasis 与 underline（R1）

```gherkin
Scenario: BDD-DEMO-EXP-001 emphasis and underline render to Word runs
  Given a published demo template whose anchor binding contains
        a paragraph with emphasis(bold) and underline children
  When  the platform generates DOCX via the final render pipeline
  Then  the anchor paragraph contains at least two runs
  And   bold and underline properties match the structured node types
  And   the output is not a single undifferentiated Calibri 10pt run for that paragraph
```

### 8.2 结构化样式 — list（R1）

```gherkin
Scenario: BDD-DEMO-EXP-002 ordered and unordered lists render as Word lists
  Given a demo binding with an ordered list (3 items) and an unordered list (2 items)
  When  DOCX is generated
  Then  the ordered list uses numeric list formatting with correct item count
  And   the unordered list uses bullet formatting
  And   list items are not flattened into plain text lines without list properties
```

### 8.3 结构化样式 — styleRef（R1）

```gherkin
Scenario: BDD-DEMO-EXP-003 styleRef resolves to master style catalog entry
  Given an approved master with style "ClauseBody" in its style catalog
    And a binding paragraph with styleRef "ClauseBody"
  When  DOCX is generated
  Then  the paragraph uses the Word style linked to "ClauseBody"
  And   fidelity validation reports no blocker for missing style
```

### 8.4 结构化样式 — tableComponent（R1）

```gherkin
Scenario: BDD-DEMO-EXP-004 table component renders as XWPFTable
  Given a mortgage demo binding with a repayment schedule tableComponent
        (header row, loop rows, footer totals row)
  When  DOCX is generated with sample variables
  Then  the anchor contains a Word table with column count matching columnSchema
  And   data rows count matches the loop variable list size
  And   footer row cells contain formatted total values
  And   the table is not rendered as pipe-delimited plain text
```

### 8.5 双页码 — 多节 FOL DOCX（R2）

```gherkin
Scenario: BDD-DEMO-EXP-005 dual page numbers in wholesale FOL DOCX
  Given the upgraded wholesale FOL master with >= 3 sections and dual-page footer
    And section 2 restarts section page numbering
  When  an executive FOL test dataset generates DOCX with enough content to span multiple pages per section
  Then  a page in section 2 displays section-local "Page X of Y" where X >= 1 and Y = section page count
  And   the same page displays document-global "Page A of B" where A > section-1 page count
  And   B equals the total physical page count of the document
```

### 8.6 双页码 — PDF 与 DOCX 一致（R2）

```gherkin
Scenario: BDD-DEMO-EXP-006 PDF page numbers match DOCX semantics
  Given renderProfile has pdf conversion enabled
    And pdfPageNumberStampingEnabled is true for the demo template
  When  the same dataset generates DOCX and PDF
  Then  for each physical page index N, the PDF footer contains
        the same document-global page index and total as DOCX field evaluation
  And   where the DOCX footer exposes section page numbers, the PDF shows matching section X of Y on pages within that section
```

### 8.7 按演示页脚 — 零售账户函（R3）

```gherkin
Scenario: BDD-DEMO-EXP-007 retail account letter footer layout
  Given the retail account opening demo master asset
  When  DOCX is generated
  Then  the footer contains the retail branch address line and customer service line
  And   the footer does not contain wholesale FOL-specific disclaimer text
  And   page numbering layout matches the retail demo config (single or dual per manifest)
```

### 8.8 按演示页脚 — 催收通知（R3）

```gherkin
Scenario: BDD-DEMO-EXP-008 collection notice footer with emphasis block
  Given the overdue collection notice demo with structured emphasis on amount and due date
  When  DOCX is generated
  Then  the body shows bold emphasis on amount and due date nodes
  And   the footer contains regulatory collection disclaimer per master asset
```

### 8.9 文档类型 — 八类均可导入并生成（R4）

```gherkin
Scenario: BDD-DEMO-EXP-009 all eight document type demos import and generate
  Given a fresh Docker deployment with demo import enabled
  When  import-all-demos completes successfully
  Then  each document type row in the matrix (§11) has a template with the listed externalId
  And   each template has at least one test dataset
  And   preview generation succeeds for DOCX and PDF for each type
```

### 8.10 批发 FOL 升级 — 规模与锚点（R4）

```gherkin
Scenario: BDD-DEMO-EXP-010 upgraded wholesale FOL retains executive scale
  Given the upgraded FOL demo after import
  When  executive test dataset generates DOCX
  Then  the document has >= 100 pages (or configured folPageTarget from manifest)
  And   all 40 clause/schedule anchors from fol-master-anchor-ids.json are bound
  And   structured content uses list, emphasis, styleRef, tableComponent, and contentModuleRef nodes in multiple anchors
```

### 8.11 演示包结构 — 镜像 demo-fol（R5）

```gherkin
Scenario: BDD-DEMO-EXP-011 demo package layout mirrors demo-fol
  Given any new demo package under deploy/demo-<code>/
  When  the package is validated against the structure contract
  Then  it contains assets/, config/, sql/, import-<code>-demo.ps1
  And   config includes catalog manifest, variables, template-config with catalogMarker
  And   a build-time *MasterDocxAssetGeneratorTest writes assets/*.docx
```

### 8.12 导入幂等（R5）

```gherkin
Scenario: BDD-DEMO-EXP-012 demo import is idempotent
  Given import-all-demos has completed once with catalogMarker M
  When  import-all-demos runs again without -RegenerateCatalog
  Then  no duplicate templates with the same externalId are created
  And   the script logs skip or update-in-place per marker
  And   masters are re-uploaded only when masterLayoutVersion changes
```

### 8.13 保真警告 — 干净演示无硬编码 fallback（R1）

```gherkin
Scenario: BDD-DEMO-EXP-013 demo generation emits real fidelity warnings only
  Given a demo template with valid node matrix and approved styles
  When  runtime generate succeeds
  Then  result.fidelityWarnings does not contain CONTROLLED_STYLE_FALLBACK
  And   any warning includes warningCode, messageKey, and non-sensitive location
```

### 8.14 条件块与循环块 — 编号确定性（R1）

```gherkin
Scenario: BDD-DEMO-EXP-014 numbering stable after condition and loop render
  Given a demo binding with numbered clauses where a loopBlock omits empty items
  When  DOCX is generated twice with the same variables
  Then  visible clause numbers are identical across both generations
  And   no duplicate or skipped numbers appear in the output
```

### 8.15 图片与签章引用（R1）

```gherkin
Scenario: BDD-DEMO-EXP-015 image and seal references embed in demo documents
  Given the LC/guarantee or wealth demo binding includes imageRef (logo) and sealRef
  When  DOCX is generated
  Then  the document contains embedded pictures at the anchor locations
  And   seal image remains within the master authorized seal area without clip/overlap blockers
```

---

## 9. 边界与异常行为

| 场景 | 期望行为 |
|------|---------|
| 母版样式目录缺失 `styleRef` 目标 | 发布阻断（`INCOMPATIBLE_CONTENT_TYPE` / 节点矩阵 blocker）；演示包导入前应在构建测试中拦截 |
| 不支持的节点类型进入绑定 | 发布阻断；渲染不得静默丢弃为空白 |
| 单节短信函（零售开户确认） | 仅配置全局页码或章节=全文；**不强制**双页码，由该演示 `config` 的 `pageNumberingProfile` 声明 |
| LibreOffice 未评估 Word 字段 | PDF 路径启用 stamper 双页码；stamper 失败时记录 fidelity warning，不得 silently 返回无页码 PDF（若 `renderProfile` 要求页码） |
| `pdfPageNumberStampingEnabled=false` | PDF 页码依赖 LO 字段转换；验收标准降级为「无崩溃 + 警告说明」 |
| 导入脚本 API 401/403 | 失败并非零退出；不部分写入未标记 catalog |
| Runtime 未授权模板 | `403 AUTHORIZATION`；fail-closed |
| 锚点缺失 | 生成失败；错误码与现有 assembly 行为一致 |
| 演示组与登录用户组不匹配 | 管理面导入使用服务账户；UI 验收用户须属对应组 |

---

## 10. 可观测证据

| 证据类型 | 内容 |
|---------|------|
| **构建产物** | `deploy/demo-*/assets/*.docx`；`*MasterDocxAssetGeneratorTest` 绿 |
| **导入日志** | `import-all-demos.ps1` 逐步 SUCCESS + catalogMarker |
| **生成产物** | 每类演示各 1 组 DOCX+PDF 参考样例（可存 `deploy/demo-*/samples/` 或测试 resources，不含真实客户数据） |
| **页码验证** | 自动化测试：POI 解析 `PAGE`/`SECTIONPAGES`/`NUMPAGES` 字段 + PDF 文本提取断言 |
| **样式验证** | POI `XWPFRun.isBold()`、`getUnderline()`、表格行数、段落样式 ID |
| **API** | Preview/runtime `result.fidelityWarnings[]`、`documentId`、审计 `traceId` |
| **门禁** | `mvn verify`；`pnpm -C frontend lint/type-check/test/build`；E2E 抽样 ≥1 旅程 per 演示组 |
| **计划同步** | `docs/plan/execution-sync-ledger.md` 记录本切片证据 |

---

## 11. 文档类型矩阵（八类演示）

优先级按用户确认顺序；`externalId` 为导入契约（可在实现中微调，但须保持一一对应）。

| 优先级 | 文档类型 | 包路径 | 模板 externalId | 组 | 页码配置 | 必须覆盖的结构化节点（抽样） | 规模目标 |
|--------|---------|--------|-----------------|-----|---------|---------------------------|---------|
| 1 | 批发 FOL（升级） | `deploy/demo-fol/` | `DEMO-FOL-WHOLESALE` | CORP | 双页码 | moduleRef, list, emphasis, styleRef, table, condition, loop | ≥100 页，40 锚点 |
| 2 | 零售账户函（开户/余额确认） | `deploy/demo-retail-account/` | `DEMO-RETAIL-ACCOUNT-OPEN`, `DEMO-RETAIL-ACCOUNT-BALANCE` | RETAIL | 全局为主 | paragraph, emphasis, variable, styleRef | 2–4 页 |
| 3 | 按揭/住房贷款批核 + 还款表 | `deploy/demo-mortgage/` | `DEMO-MORTGAGE-APPROVAL` | RETAIL | 双页码（正文+schedule） | tableComponent, list, emphasis, sectionHeading | 8–20 页 |
| 4 | 授信/额度确认 | `deploy/demo-credit-limit/` | `DEMO-CREDIT-LIMIT-CONFIRM` | CORP | 双页码 | tableComponent, condition, styleRef, underline | 6–15 页 |
| 5 | 信用证/保函通知 | `deploy/demo-trade-lc/` | `DEMO-TRADE-LC-NOTICE`, `DEMO-TRADE-GUARANTEE-NOTICE` | TRADE | 全局 + 附件节 | imageRef, sealRef, list, moduleRef | 5–12 页 |
| 6 | 利率变更 / 逾期催收 | `deploy/demo-collection/` | `DEMO-RATE-CHANGE-NOTICE`, `DEMO-OVERDUE-COLLECTION` | RETAIL | 全局 | emphasis, underline, paragraph, variable | 2–6 页 |
| 7 | 年审 / 续期函 | `deploy/demo-annual-review/` | `DEMO-ANNUAL-REVIEW`, `DEMO-FACILITY-RENEWAL` | CORP | 双页码 | condition, loop, list, styleRef | 10–30 页 |
| 8 | 财富/私人银行投资结单 | `deploy/demo-wealth/` | `DEMO-WEALTH-STATEMENT` | WEALTH | 全局（多表续页） | tableComponent, emphasis, imageRef, footer totals | 12–40 页 |

**视觉逼真度（已确认）**：每类须使用真实银行信函版式（抬头、分行地址、条款编号、表格线框、页脚声明）；仅客户名、账号、金额、日期等为 mock 变量。

---

## 12. 演示包结构契约

每个 `deploy/demo-<code>/` 目录**必须**包含：

```
deploy/demo-<code>/
  assets/                    # 母版 DOCX、图片、签章 PNG
  config/
    <code>-catalog-manifest.json
    <code>-template-config.json
    <code>-variables.json
    <code>-binding-overlays.json   # 可选：富绑定覆盖
    <code>-master-anchor-ids.json
  sql/
    001-<code>-content-modules.sql # 条款/模块种子
  import-<code>-demo.ps1
  generate-<code>-catalog.ps1      # 可选：从模板生成 JSON
  <code>-catalog-shared.ps1        # 可选：共享函数
```

仓库级：

```
deploy/import-all-demos.ps1        # 按优先级调用各 import 脚本
```

构建期：

```
backend/src/test/java/.../<Code>MasterDocxAssetGeneratorTest.java
  → writes deploy/demo-<code>/assets/*-master.docx
  → asserts anchor extraction + footer field presence
```

`config/*-template-config.json` 最低字段：

- `catalogMarker`（幂等）
- `masterLayoutVersion`
- `pageNumberingProfile`: `GLOBAL_ONLY` | `SECTION_AND_GLOBAL` | `SECTION_ONLY`
- `templateExternalId`, `groupCode`, `masterName`

---

## 13. BDD 就绪声明

| 项 | 值 |
|----|-----|
| **BDD readiness** | `ready` |
| **阻塞问题** | 无（用户已确认范围） |
| **Handoff** | `plan-orchestrator` decomposition complete → **P22 Done** (2026-07-03) |
| **计划落点** | **P22-DEMO-EXPANSION** — [P22 detail plan](../plan/detail/P22-demo-expansion-rendering-fidelity.md) **Done** (2026-07-03) |

---

## 14. 可追溯性

| 来源文档 | 关系 |
|---------|------|
| [requirements-plan.md](./requirements-plan.md) | 模板创作与渲染边界、结构化节点矩阵、保真分级 — §已确认：模板创作与渲染边界 |
| [PRD.md §6.5.1、§6.7](../product/PRD.md) | 锚点内容、母版页眉页脚职责、renderProfile、八类演示包 |
| [P18 structured authoring](../plan/detail/P18-structured-authoring-fidelity-engine.md) | v1 节点矩阵、样式目录、编号 — 本切片补齐**渲染侧**缺口 |
| [P4 rendering](../plan/detail/P4-rendering-preview.md) | DOCX/PDF 管线、保真警告 |
| [P3 template authoring](../plan/detail/P3-template-authoring.md) | FOL catalog E2E 先例 |
| `deploy/demo-fol/` | 结构镜像与 FOL 升级基线 |
| `FolMasterDocxAssetGeneratorTest.java` | 母版生成模式参考 |
| [Domain model §2.6.10–2.6.11、§2.18](../domain/domain-model.md) | StructuredContentDocxWriter、页码语义、演示包契约 |
| `DocxAssembler.java` / `PdfPageNumberStamper.java` | 当前实现待扩展点 |

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-07-03 | 初始行为规格 — 用户确认综合演示扩展单次交付切片 |
