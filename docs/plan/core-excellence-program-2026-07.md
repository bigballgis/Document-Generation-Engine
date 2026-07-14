# Core Excellence Program（CE）— 模板/母版/条款/渲染核心能力统一优化计划

**Created:** 2026-07-13
**Sources:** 四轮只读深度审查（全部有文件级证据，未改动任何代码）：

| 轮次 | 侧重点 | 产出前缀 |
| --- | --- | --- |
| R1 | 全系统功能性缺口（管理面） | FO-*（已并入本计划） |
| R2 | 银行/法律信函内核（编排、变量、对比、样式、DOCX/PDF 保真） | CORE-*（已并入本计划） |
| R3 | 契约诚信、银行内控合规、环境晋级、归档输出 | GAP-*（已并入本计划） |
| R4 | 核心链路易用性（作者/审核人/集成方日常操作） | UX-*（已并入本计划） |

**Status model:** `Not Started` | `In Progress` | `Blocked` | `Done`
**Relationship to other plans:** 本计划是四轮审查的**唯一合并执行地图**；与
[system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md)（SOR，非功能为主）、
[optimization-plan.md](./optimization-plan.md)（技术债）互补，不重复其条目。
执行真相记录在 `.taskmaster/tasks/tasks.json`（新工作）+ 本文件状态列。

**执行纪律（对任何执行模型强制）：**

1. 行为变更先写/确认 BDD 行为规格（或显式 `not-applicable`），落到 `docs/behavior/`。
2. TDD：先写失败测试，再实现最小变更。
3. 门禁全绿才算 Done：后端 `mvn -B -ntp -f backend/pom.xml verify`；前端 `pnpm -C frontend lint && type-check && test && build`。
4. 用户可见的前端变更必须有 Playwright E2E + UIUX 证据（Docker 4173）。
5. 每个切片走独立 worktree（`../DGE-<slice-id>`），合并后在 MAIN 做 doc-sync + commit review。
6. 一次只做一个切片；禁止在一个切片里"顺手"做相邻任务。

---

## 0. 核心痛点执行摘要（直击点）

四轮审查交叉验证后，以下 8 条是**平台达到银行/法律信函级别的真正阻断项**，
所有 P0 任务均直接服务于它们：

| # | 核心痛点 | 证据锚点 | 对应任务 |
| --- | --- | --- | --- |
| 1 | **发布不可复现**：运行时直接用 `master.getStorageKey()` 取当前母版，模板发布后母版换文件即改变已发布产出；无发布包快照 | `DocumentGenerationAssemblySupport` | CE-K01 |
| 2 | **样式无权威**：Calibri/10pt/黑色硬编码在 ≥6 处渲染支持类；`MasterStyleCatalogService.loadForMaster` 忽略 masterId 返回固定目录 | `StructuredContentDocxStyleSupport` 等 | CE-K02 |
| 3 | **变量计算是空壳**：`computeExpression` 可存可读**从不求值**；无聚合、无金额/日期格式化、无中文大写金额 | `TemplateViewMapperTest`、渲染裸 `String.valueOf` | CE-K03 |
| 4 | **变更对比是假 diff**：content 维度只比 `masterCatalogVersion` 字符串，anchor 只比 `bindingHash`；Preview 对比只比绑定状态；法务无法阅读"改了哪句话" | `ChangeDiffDimensionSupport`、`PreviewComparisonService` | CE-K04 |
| 5 | **作者体验是专家路径**：嵌套块不能编辑、无拖拽、测试数据手写 JSON、预览只能下载、fidelity viewed 不落库 | R4 全部高优先项 | CE-U01/03/04/05 |
| 6 | **契约与实现漂移**：`context` 字段整个不存在、unknown field 静默忽略、fidelityWarnings 形态不符——OpenAPI 承诺了代码没有的东西 | `GenerateRequestBody` 仅 5 字段 | CE-C01/02/03 |
| 7 | **内控硬控制缺失**：可自提自批（三处审批均无同人校验）、测试件无水印、发出错函后无法按版本圈定受影响调用 | `TemplateLifecycleApprovalFlowSupport` 等 | CE-G01/02 + CE-U11 |
| 8 | **母版影响分析是 stub**：固定返回空列表 + `retestRequired=false`，前端照常渲染"无引用模板"，给出**错误的安全感** | `MasterDocumentService.impactAnalysis` | CE-K05 |

---

## 1. 目标能力模型（银行/法律信函级）

Done 时平台应满足：

1. **可复现**：任一已发布 release 在任意时刻重放生成，产出与首发 bitwise 等价（发布包钉扎母版 revision + 条款版本 + 样式 + render profile）。
2. **样式单一权威**：母版 styles.xml 是唯一字体/字号/间距来源，渲染代码零硬编码字体。
3. **可计算**：声明式变量 DSL（求值、聚合、FORMAT_AMOUNT/DATE、SPELL_AMOUNT 中文大写、locale 感知），白名单函数，禁任意代码。
4. **可审阅**：候选 vs 基线、release A vs B 的**语义级**（条款正文句子级）diff，法务可读。
5. **可操作**：作者不写 JSON、不下载文件也能完成"编排→填充→预览→修复"闭环。
6. **契约诚信**：OpenAPI 声明的每个字段/校验/错误码在实现中真实存在，反之亦然。
7. **内控合规**：同人不能审批自己、测试件带 SPECIMEN 水印、错函可按版本+时间圈定召回范围。
8. **可迁移**：模板可携带全部依赖跨环境晋级，缺依赖 dry-run 预检报告而非导入后半残。

---

## 2. 波次总览

| 波次 | 主题 | 任务数 | 优先级 |
| --- | --- | --- | --- |
| **CE-K** | 信函内核正确性（钉扎/样式/变量/对比/影响/保真/金标/条款治理） | 8 | P0–P2 |
| **CE-U** | 核心链路易用性（编辑器/测试数据/预览/审核闭环/排障/导航） | 21 | P0–P3 |
| **CE-C** | 运行时契约诚信 | 6 | P0–P2 |
| **CE-G** | 银行内控合规 | 6 | P0–P3 |
| **CE-E** | 环境晋级与资产治理 | 3 | P2–P3 |
| **CE-O** | 归档输出合规 | 2 | P2–P3 |

三条可并行泳道（文件面互不冲突）：
**泳道 1（后端内核）** CE-K*；**泳道 2（前端体验）** CE-U*；**泳道 3（契约+合规）** CE-C* + CE-G*。
同一泳道内串行执行。

---

## 3. Wave CE-K — 信函内核正确性

### CE-K01 发布包不可变快照（钉扎）— P0 · XL · `Done`

**痛点：** 已发布模板的产出会随母版文件替换、条款改动而漂移；审计无法复现历史产出。
**现状证据：** `DocumentGenerationAssemblySupport` 运行时经
`objectStoragePort.get(master.getStorageKey())` 取**当前**母版；`TemplateVersionEntity`
不持有 master revision 指针；条款引用虽 pin `semanticVersion` 但样式/render profile 未全钉。
**目标行为（BDD 必须覆盖）：**
- Given 模板发布为 release 1.0.0，When 母版随后替换文件产生新 revision，Then 调用 1.0.0 生成的产物仍使用发布时的母版 revision（字体/页眉/锚点与首发一致）。
- Given release 已发布，Then `template_version` 行持久化 `master_revision_id` + `master_file_hash` + render profile 快照；运行时装配只读取钉扎引用。
- 停用后的 master revision 不可被物理删除（有已发布 release 引用时 fail-closed）。
**改动面：** `template`（发布流程写入钉扎字段 + Flyway 迁移）、`runtime`/`rendering` 装配读取钉扎 revision、`master` 删除保护。
**测试：** 发布→替换母版→生成 回归测试（断言 storage key 为钉扎 revision）；迁移向后兼容测试（存量 release 回填当前 revision 并在 ledger 标记 `PINNED_RETROACTIVELY`）。
**禁止：** 不改锚点提取逻辑；不动预览路径语义（预览允许跟随 dev 母版）。
**依赖：** 无（最先做，K04/G06/E01 都依赖它）。

### CE-K02 母版样式权威 — P0 · L · `Not Started`

**痛点：** 渲染硬编码 Calibri/10pt，母版设计师改字体不生效，中文信函字体错误。
**现状证据：** `DocxWordCompatibilitySupport.DEFAULT_FONT="Calibri"`、
`StructuredContentDocxStyleSupport.applyDefaultRunStyle` 硬编码、
`MasterStyleCatalogService.loadForMaster(masterId)` 忽略参数返回 `defaultCatalog`。
**目标行为：**
- 母版上传时解析 styles.xml → 持久化 per-master style catalog（段落/字符样式、theme 字体、默认字号）。
- 结构化内容写入 DOCX 时按 `styleRef` → 母版样式 ID 落地；未指定时继承母版 `docDefaults`，**不再**写入硬编码字体。
- 兜底链固定为：节点 styleRef > 母版样式 > 母版 docDefaults；仅当母版完全无 docDefaults 时才允许系统兜底并产生 fidelity warning `MASTER_STYLE_FALLBACK`。
**测试：** 上传含宋体/仿宋 styles.xml 的母版 → 生成产物 run 属性断言无 Calibri；金标语料（K07）加双字体母版样本。
**禁止：** 不在本片重写 demo builder（demo 硬编码单独小片清理）。
**依赖：** 建议先于/并行 K01（互不阻塞，但 K01 钉扎需包含 style catalog 版本）。

### CE-K03 变量计算与格式化引擎 — P0 · XL · `Not Started`

**痛点：** `computeExpression` 是死字段；金额/日期靠调用方预格式化；无中文大写金额。
**目标行为：**
- 白名单 DSL：`COALESCE`、`SUM`、`COUNT`、`AVG`、`FILTER`、`FORMAT_AMOUNT`、`FORMAT_DATE`、**`SPELL_AMOUNT`**（人民币大写，覆盖 0 元/整元/角分/亿级/负数拒绝边界表）；变量引用 `${path.to.var}`；禁循环、禁任意方法调用、深度与长度上限。
- 求值时机：生成/预览装配前统一求值层；求值失败 → `VARIABLE_COMPUTE_FAILED`（含变量 key 与表达式摘要），fail-closed。
- `FORMAT_*` 接 locale（默认 zh-CN；locale 来源见 CE-C01 的 `context.locale`）。
- 管理端：变量面板对 compute 类型变量提供表达式即时校验（语法 + 引用变量存在性）+ 样例求值预览。
**改动面：** 新 `template`（或 sharedkernel）表达式引擎子包、`runtime`/`rendering` 装配接入、前端变量面板。
**测试：** 表达式引擎独立单测矩阵（每个函数 ≥5 边界）；端到端：schema 定义 compute 变量 → 生成 → 断言 DOCX 文本。
**禁止：** 不引入脚本引擎（Groovy/JS/SpEL 完整版）；ADR 记录 DSL 边界。
**依赖：** 无；与 K01/K02 并行安全（不同文件面）。

### CE-K04 语义级变更对比 + release A/B 对比 — P1 · L · `Not Started`

**痛点：** `diffContent` 只比 `masterCatalogVersion`，`diffAnchors` 只比 `bindingHash`；`PreviewComparisonService` 只比绑定状态码；无任意两 release 对比入口。法务看不到"哪句话变了"。
**目标行为：**
- 结构化内容树 diff：按锚点 → 块路径逐节点比较，产出人类可读条目（"第 3 段：'贷款利率 4.9%' → '贷款利率 5.2%'"），含条款版本升降与文字级差异。
- `ChangeDiffService` 抽出 `computeBetween(versionA, versionB)`；管理 API 增加 release A vs B 端点；前端 release 列表多选对比。
- 审批/发布摘要对话框展示语义 diff 而非维度码表。
**依赖：** K01（对比对象必须是钉扎快照才有意义）。
**测试：** diff 引擎单测（增/删/改/移动/嵌套）；E2E：改一句条款文字 → 审批页可见该句 diff。

### CE-K05 母版影响分析真实化 + revision diff — P1 · M · `Not Started`

**痛点：** `impactAnalysis` 固定返回 `List.of(), false`；前端 `MasterImpactPanel` 呈现"无引用模板"的假象；无 revision 间差异对比。
**目标行为：**
- 真实查询：按 masterId 反查引用模板（含各生命周期状态）+ 各 release 钉扎的 revision（依赖 K01 数据）→ `retestRequired` 按锚点集合变化计算。
- Revision diff：锚点新增/删除/重命名清单 + 文件 hash；替换文件前置影响确认对话框。
- 前端 impact 面板用模板名（非 UUID）+ 可点击链接。
**依赖：** K01（release→revision 关联）。

### CE-K06 渲染保真补全 — P1 · L · `Not Started`

**痛点：** `WriterUnsupportedStructuredNodeTypes` 明确不支持 `qrBarcodeRef`/`attachmentListRef`；表格 `repeatHeaderAcrossPages` 绑定配置存在但 writer 不写 `tblHeader`；`pdf-page-number-stamping-enabled` 默认 false。
**目标行为（三个子片，可独立交付）：**
- K06a：表格跨页表头 — writer 落 `<w:tblHeader/>`；金标含 2 页表格样本。
- K06b：`qrBarcodeRef` writer（ZXing 生成图片嵌入，尺寸/纠错级别可配）。
- K06c：`attachmentListRef` writer（结构化附件清单 → 编号列表段落）；PDF 页码 stamp 策略进 render profile（按包配置而非全局布尔）。
**依赖：** K02（样式来源）先行为佳；资产/图片依赖 CE-E02 的键名约定但不阻塞。

### CE-K07 金标语料回归体系 — P1 · M · `Done`

**目标：** `backend/src/test/resources/golden-corpus/` 固化 ≥8 个基准包（双字体母版、跨页表格、嵌套条款、compute 变量、中文大写金额、水印 SPECIMEN、加密 PDF、极限长条款），每个含输入（母版+模板 JSON+变量）与期望产出断言（DOCX XML 关键路径断言 + PDF 文本抽取断言，不做像素比对）；接入 `mvn verify`。K01–K06 每片交付时必须往语料库加对应样本。
**依赖：** 骨架先行，可与 K01 并行启动。
**状态（2026-07-14）：** **Done** — 骨架先行切片 `ce-k07-golden-corpus-skeleton`（Task Master **#54**）。**Merge:** `e8f996a0` (`e8f996a000fcf3845fa4c1dc66295e2b0c0f5282`); feature tip `91455ca3`。8 主题包骨架就位（`nested-clauses`/`encrypted-pdf` = ACTIVE，其余 6 = PLACEHOLDER）；harness `GoldenCorpusScanner`/`GoldenCorpusActiveRunner`/`GoldenCorpusStructureTest`/`GoldenCorpusHarnessTest` 接入 `mvn verify`。**Gates:** `mvn -B -ntp -f backend/pom.xml verify` **GREEN** (**1379** / 0 fail / 0 error / 7 skipped；golden-corpus 套件绿；Checkstyle/PMD/SpotBugs/JaCoCo 通过）。Architecture **PASS_WITH_NOTES** (Critical=0; review `7bc4b23f`)。BDD **ready** ([ce-k07-golden-corpus-skeleton.md](../behavior/ce-k07-golden-corpus-skeleton.md); **BDD-CE-K07-001…019**)。PLACEHOLDER 充实由后续 K01–K06 / CE-G02 拥有。正式 phase 仍为 **None**；**not** go-live；不激活 CD-3。

### CE-K08 条款治理元数据 — P2 · M · `Not Started`

**目标：** `content_module_version` 增加 `jurisdiction`、`effectiveFrom/To`、`legalReviewRef`（法务评审单号）可选字段 + 目录筛选；模板发布 gate 增加"引用条款已过 effectiveTo"阻断项。
**依赖：** 无硬依赖；与 CE-U07 升版提醒同泳道排前后。

---

## 4. Wave CE-U — 核心链路易用性

R4 审查结论：平台是"专家路径"——功能在但闭环断。以下按作者 ROI 排序。

### P0 组

**CE-U01 结构化编辑器嵌套子树编辑 — P0 · L · `Done`**
condition/loop 块 `children` 数组已在 schema 中但 `StructuredContentBlockCard` 无递归 UI，嵌套内容只能靠导入 HTML 或改 JSON。目标：块卡片递归渲染子编辑器（限深 3 层），子块增删改与 undo/redo（LR-C3 历史栈）兼容。测试：Vitest 递归渲染 + E2E 在 condition 内新增段落并预览。禁止：本片不做拖拽（U02）。
**Slice:** `ce-u01-nested-editor` · branch `feat/ce-u01-nested-editor` · BDD [ce-u01-nested-editor.md](../behavior/ce-u01-nested-editor.md) (`ready`, NE-01…NE-05).
**Status (2026-07-14):** **Done** — recursive `StructuredContentBlockCard` + path-based mutations (`structuredContentNodePath.ts`, max depth 3); nested toolbar; undo/redo compatible; i18n en/zh-CN; Vitest **4/4** CE-U01 + path utils **5/5**; E2E **2/2** + UIUX **2/2** @1920 dual-brand; `mvn verify` GREEN; frontend gates GREEN. Formal phase **None**; **not** go-live.

**CE-U03 测试数据集 schema 驱动 — P0 · M · `Done`**
现状 8 行 textarea 手写 JSON、默认值硬编码 `{"customerName":"Sample"}`、保存仅 `JSON.parse`、后端不对照 schema 校验。目标：按 `VariableSchema` 生成动态表单（类型/必填即时校验）+ "从 schema 生成骨架"按钮 + 大 payload 折叠编辑；后端 `TestDataSetService` 保存时按 schema 校验并返回字段级错误。依赖：无（compute 变量出现后表单跳过 compute 字段——与 K03 弱耦合）。
**Slice:** `ce-u03-testdata-schema-form` · branch `feat/ce-u03-testdata-schema-form` · BDD [ce-u03-testdata-schema-form.md](../behavior/ce-u03-testdata-schema-form.md) (`ready`, 18 scenarios).
**Status (2026-07-14):** **Done** — backend `TestDataSetSchemaValidationException` (422 / VALIDATION / fieldErrors) + 6 JUnit; frontend schema-driven form + skeleton + collapsible JSON + i18n en/zh-CN + Vitest; E2E **9/9** + UIUX **PASS_WITH_NOTES** (24 screenshots @1920 dual-brand); architecture **PASS_WITH_NOTES** Critical=0. Gates GREEN on feature worktree. Formal phase **None**; **not** go-live.

### P1 组

**CE-U02 块排序/复制/校验定位 — P1 · M · `Done`**
拖拽排序（限同层）、块复制、绑定校验失败条目一键滚动到出错块（blockPath → scrollIntoView）。依赖 U01。
**Slice:** `ce-u02-block-sort-copy-scroll` · branch `feat/ce-u02-block-sort-copy-scroll` · BDD [ce-u02-block-sort-copy-scroll.md](../behavior/ce-u02-block-sort-copy-scroll.md) (`ready`, BS-01…BS-05).
**Status (2026-07-14):** **Done** — same-layer drag reorder + copy + client validation scroll (`structuredContentBindingValidation.ts`, `structuredContentDragState.ts`); Vitest CE-U02 + validation utils; E2E **2/2** + UIUX **2/2** @1920 dual-brand; frontend gates **GREEN** (1211 tests); merge `50b7d04d`; Task Master **#65**. Formal phase **None**; **not** go-live.

**CE-U04 站内 PDF 预览 — P1 · M · `Done`**
全库无 iframe/pdf.js，预览产物只能下载外部打开。目标：预览面板嵌 pdf.js 只读视图（带 SPECIMEN 水印后的产物，见 CE-G02）；split view（LR-C 已有 `AuthoringSideBySideLayout`）内直接翻页。E2E：预览刷新后无需下载即可看到第 1 页。

**Slice:** `ce-u04-inline-pdf-preview` · branch `feat/ce-u04-inline-pdf-preview` · BDD [ce-u04-inline-pdf-preview.md](../behavior/ce-u04-inline-pdf-preview.md) (`ready`, IPP-01…IPP-04).
**Status (2026-07-14):** **Done** — `pdfjs-dist@4.10.38` + `InlinePdfPreviewViewer` / `useInlinePdfPreview` embedded in `AuthoringPreviewPane` + `TemplatePreviewPanel`; page toolbar + canvas render; download retained; nginx `.mjs` → `application/javascript` for pdf.js worker. **Gates:** frontend lint/type-check/test/build **GREEN** (1235 Vitest; branches **80.01%**); E2E **3/3** + UIUX **1/1** @1920; deploy **DEPLOY_OK_WITH_NOTES** (JWT env; `FRONTEND_PORT=5173`; backend healthcheck wget flake). CE-G02 watermark **soft-dep** (#73). Task Master **#67**. Formal phase **None**; **not** go-live.

**CE-U05 fidelity viewed 持久化 + 修复动线 — P1 · M**
`markViewed` 仅本地 state、父组件未监听、后端无 API——发布勾选"已查看 fidelity"与真实浏览完全脱钩。目标：viewed 落库（per warning per preview run）、publish gate 校验未 viewed 数、警告默认人话文案（技术码收进展开区）、警告行链接到对应绑定编辑位。

**Slice:** `ce-u05-fidelity-viewed-persist` · branch `feat/ce-u05-fidelity-viewed-persist` · BDD [ce-u05-fidelity-viewed-persist.md](../behavior/ce-u05-fidelity-viewed-persist.md) (`ready`, FVP-01…FVP-04).
**Status (2026-07-14):** **Done** — per-warning viewed persistence in `preview_record.fidelity_warnings_json`; `PUT …/fidelity-warnings/viewed`; publish gate `FIDELITY_WARNINGS_VIEWED`; human-readable warning UI + Edit binding deep link. **Gates:** `mvn verify` **GREEN**; frontend lint/type-check/test/build **GREEN** (1213 Vitest); E2E **4/4** + UIUX **PASS_WITH_NOTES** (4 screenshots @1920); deploy **DEPLOY_OK_WITH_NOTES** (JWT env + backend healthcheck wget flake; stack @8080/:5173). **Merge:** `12741d69`; Task Master **#66**. Formal phase **None**; **not** go-live.

**CE-U07 条款升版提醒 + 一键 bump — P1 · M**
引用表只显示 pinned 版本，条款升版后模板侧零感知。目标：模板详情条款面板对"有更新已批准版本"的引用显示 out-of-date 徽标 + 一键升 pin（走既有 upsertReference）+ 批量升级确认；Dashboard 作者待办加"引用条款有新版"项。

**Slice:** `ce-u07-clause-outdated-bump` · branch `feat/ce-u07-clause-outdated-bump` · BDD [ce-u07-clause-outdated-bump.md](../behavior/ce-u07-clause-outdated-bump.md) (`ready`, COB-001…004).
**Status (2026-07-15):** **Done** — out-of-date badge + one-click bump (`upsertReference`) + bulk upgrade confirm + Dashboard author todo deep link. **Gates:** `mvn verify` **GREEN** (1481 tests); frontend lint/type-check/test/build **GREEN**; E2E **3/3** (COB-001/002, COB-004, UIUX); UIUX **PASS_WITH_NOTES** ([CE-U07-uiux-manifest.md](../../frontend/e2e/evidence/CE-U07-uiux-manifest.md)); architecture **PASS_WITH_NOTES** Critical=0 (Majors: export-schema coupling, soft deny empty list — follow-ups OK). **Merge:** `fde9342a` (`fde9342a4c70fc141fdf7c054422472f38387a71`); Task Master **#82**. Formal phase **None**; **not** go-live.

**CE-U08 条款审核闭环 — P1 · M**
后端 review 完整但：Dashboard 无条款审核待办、驳回原因不回显、无审核时间线。目标：`useWorkflowTasks` 增加 content module 待审/返工任务；版本表展示 `rejectionReason`；对齐母版的 `el-timeline` 审核历史。

**CE-U09 母版审核可达性 — P1 · S**
审批按钮藏在 revision 详情 Tab；Hub 的 `submitReviewOpen` 接线了却无入口（死代码）；旅程 CTA 被 `showPrimaryCta:false` 关闭。目标：Hub 对 current revision 直接暴露提交/通过/驳回；Dashboard 待办深链 `?workspaceTab=approval`；清死代码。

**CE-U10 sharedGroupCodes 配置 UI — P1 · S**
后端/API 全通，前端零入口。目标：条款创建与设置对话框加多选"共享到组"，详情摘要展示共享范围。

**CE-U11 调用排障 + 召回检索 — P1 · M**（合并 R3 GAP-07）
Drawer 无 errorCode/messageKey；管理查询无 `resolvedReleaseVersion` 过滤（实体有字段、predicates 没接）。目标：`ManagementInvocationFilters` + repository + controller + 前端筛选器加 releaseVersion；detail view 与 Drawer 暴露统一 error envelope；列表可导出 CSV（召回圈定的最小闭环）。

**CE-U12 契约页可复制示例 — P1 · S**
examples 只是 token 字符串。目标：契约页生成完整 curl（含 Auth 头 + Idempotency-Key）+ 按选定测试数据集生成请求 payload JSON + 复制按钮。

### P2/P3 组（简卡）

| ID | 标题 | 级 | 要点 |
| --- | --- | --- | --- |
| CE-U06 | 母版锚点可视上下文 | P2·M | 修订工作区 DOCX 概览（锚点位置高亮列表即可，不做完整渲染）+ displayLabel 可编辑 |
| CE-U13 | 变量重命名联动 + 表达式补全 | P2·M | rename 传播到 bindings/规则/测试集 JSON；conditionExpression 变量补全 |
| CE-U14 | Dashboard 模板生命周期待办 | P2·S | 测试裁定/审批/待发布队列进 Tasks Tab |
| CE-U15 | 生命周期 Stepper + checklist 深链 | P2·M | 顶栏状态机 Stepper；发布 gate 每条 pending 项"前往修复"链接 |
| CE-U16 | 创作路径压缩 | P2·M | design 默认落点 bindings；新建后微向导（母版→绑定→变量→预览步骤条） |
| CE-U18 | 批量测试历史钻取 + 双路径统一 | P2·M | `sampleResultsJson` 前端消费（展开明细+跳转）；移除无进度 legacy 同步批量路径 |
| CE-U20 | 条款创建结构化化 + 列表状态列 | P2·S | 创建对话框弃 JSON textarea 改结构化编辑器；目录加状态列/筛选 |
| CE-U21 | 草稿按锚点分 key + 并发提示 | P2·M | localDraft key 加 anchorId；服务端保存乐观锁版本号冲突提示 |
| CE-U17 | 编辑器快捷键 | P3·S | Ctrl+S 保存绑定 / Ctrl+P 刷新预览；命令面板注册作者动作 |
| CE-U19 | 依赖关系只读视图 | P3·M | 模板详情"依赖"页：母版 revision、锚点、条款版本、release 线 |

---

## 5. Wave CE-C — 运行时契约诚信

| ID | 标题 | 级/量 | 要点（证据见 R3 报告） | 依赖 |
| --- | --- | --- | --- | --- |
| CE-C01 | `context` 白名单落地 | P0·S · `Done` | DTO 加 `context`（6 字段 record）；未知子字段 400；`InvocationParameterSanitizer` 写 `contextSummary`。Slice `ce-c01-c02-contract-strictness` · `mvn verify` GREEN | 无 |
| CE-C02 | unknown-field 严格校验 | P0·S · `Done` | runtime DTO 专用 ObjectMapper `fail-on-unknown-properties` → 统一 `400 REQUEST_BODY_INVALID`；**不**动管理端 DTO。Slice `ce-c01-c02-contract-strictness` · `mvn verify` GREEN | C01 同链 |
| CE-C03 | fidelityWarnings 契约对齐 | P1·S | 批量项/任务查询返回完整 `FidelityWarning[]`；同步流保留头摘要并在契约注明 | 无 |
| CE-C04 | 凭证 `expires_at` 持久化 + 暴露 | P1·M | 结束 `ApiCredentialLifecycleSupport` 过渡态（R1 项）：Flyway 加列 + 发放/轮换写入；`RuntimeCredentialSummaryView` 加 `expiresAt`/`EXPIRING_SOON`；契约页 callable versions 加可选 `deprecated`/`sunsetAt`（需轻量修订 ADR-0003/0017 展示边界） | 无 |
| CE-C05 | `originalBatchId` 重试血缘 | P1·M | 请求字段 + 校验（原批次存在且属同凭证）+ 审计关联 + 契约文档 | 无 |
| CE-C06 | DOCX permissions 边界声明 | P2·S | 短期：契约明确"permissions 仅 PDF 生效"，校验对 DOCX+permissions 返回警告；长期 POI 写保护另立片 | 无 |

## 6. Wave CE-G — 银行内控合规

| ID | 标题 | 级/量 | 要点 | 依赖 |
| --- | --- | --- | --- | --- |
| CE-G01 | 同人审批阻断 | P0·S · `Done` | 模板/母版/条款三处 decision service 统一 `decisionActor != lastSubmitActor` → `api.error.lifecycle.selfApprovalForbidden`；GROUP_ADMIN/GLOBAL_ADMIN 例外干预（强制 reason+secondary+审计）；Flyway V56；BDD ready ([ce-g01-self-approval-block.md](../behavior/ce-g01-self-approval-block.md); 22 scenarios)。Slice `ce-g01-self-approval-block` · Task Master **#72** · merge `c187a230`. **Gates:** `mvn verify` GREEN (1470/0/0/7). | 无 |
| CE-G02 | SPECIMEN 水印 | P0·M | 预览/test-generate 路径 DOCX 页眉页脚 + PDF 对角水印（复用 `PdfPageNumberStamper` 的 PDFBox 后处理模式）；**正式 runtime 路径零改动**（金标护栏断言正式产物 bitwise 不变） | K07 骨架 |
| CE-G03 | 测试数据 PII 治理 | P1·M | 变量 schema `piiCategory` 标签；标记字段保存测试集时强制合成值或显式确认+审计；关闭 `data-storage-view.md` 挂起问题 | U03 先行 |
| CE-G04 | Legal hold 最小实现 | P2·M | hold 实体（模板+时间窗 / invocation 集合）；两个 retention 清理调度器删除前查豁免；GLOBAL_ADMIN 专属管理页 | 无 |
| CE-G06 | 审计可复现最小集 | P2·M | invocation 记录发布包快照 ID + bundle hash；管理端"按 invocation 受控再生"API（需权限 + 审计 + SPECIMEN 水印标记再生件） | K01 |
| CE-G05 | 模板年检 + 条款正文检索 | P3·M | `nextReviewDue` + 到期待办；`content_structure_json` 全文检索（PostgreSQL tsvector）where-used | 无 |

## 7. Wave CE-E — 环境晋级与资产

| ID | 标题 | 级/量 | 要点 | 依赖 |
| --- | --- | --- | --- | --- |
| CE-E01 | 自包含导出包 v2 + dry-run | P2·L | bundle 追加母版 revision 指纹与 DOCX（ZIP 内嵌）、条款正文快照、render profile、资产键清单；导入 dry-run 依赖预检报告 | K01 |
| CE-E02 | 资产库管理面 | P2·M | MinIO 资产目录 API（上传/列表/停用）+ 键名约定固化 + 管理页；印章类上传需审批角色；`StructuredContentImageResolver` 协议不变 | 无 |
| CE-E03 | 全库导出 | P3·M | 基于 per-template bundle + 母版/条款批量导出 + manifest | E01 |

## 8. Wave CE-O — 归档输出合规

| ID | 标题 | 级/量 | 要点 | 依赖 |
| --- | --- | --- | --- | --- |
| CE-O01 | PDF/A 输出选项 | P2·M | render profile 加 `pdfArchivalProfile`（NONE/PDF_A_2B）；LibreOffice 过滤器参数切换；与加密互斥校验；小 ADR 记录选型；veraPDF 或最小自校验进金标 | K07 |
| CE-O02 | addressBlock / 多文档包 | P3·— | **待产品拍板后再细化**（见 §10） | — |

---

## 9. 依赖图与推荐执行顺序

```text
泳道1(后端内核):  CE-K07骨架 → CE-K01 → CE-K02 → CE-K03 → CE-K04 → CE-K05 → CE-K06a/b/c → CE-K08
泳道2(前端体验):  CE-U03 → CE-U01 → CE-U02 → CE-U05 → CE-U04 → CE-U07 → CE-U08 → CE-U09/U10 → P2组
泳道3(契约合规):  CE-C01+C02 → CE-G01 → CE-G02(需K07骨架) → CE-C03 → CE-U11 → CE-C04 → CE-C05 → CE-G03
其后(收敛期):     CE-G06/CE-E01(需K01) → CE-E02 → CE-O01 → P3组
```

**推荐首批（3 泳道并行，各自独立 worktree，文件面零冲突）：**

1. `ce-k07-golden-corpus-skeleton`（金标骨架，为一切保真变更提供护栏）
2. `ce-u03-testdata-schema-form`（作者 ROI 最高的独立前端片）
3. `ce-c01-c02-contract-strictness`（context + unknown field，一条 PR 链）

首批合并后第二批：`ce-k01-release-bundle-pinning`（内核最大件）、`ce-u01-nested-editor`、`ce-g01-self-approval-block`。

---

## 10. 待用户拍板的 ADR 级决策（不拍板不排期）

| # | 决策 | 现行立场 | 推荐默认 |
| --- | --- | --- | --- |
| D1 | 发布四眼双人复核 | ADR-0021 明确拒绝 multi-person release | 仅做 CE-G01 同人阻断；四眼待内控要求出现再修订 ADR |
| D2 | 定时发布/生效日 | ADR-0007 immediate-only | 维持；监管有"预先公告生效日"要求时再立项 |
| D3 | 异步 webhook 回调 | ADR-0008 poll-only | 维持否决 |
| D4 | `SYNC_DOWNLOAD_URL` 完整实现 + 下载重签 | ADR-0038 deferred | 维持 defer；先做契约文档与实现对齐（"不重签"写明） |
| D5 | addressBlock 节点 + 多文档包 | 无需求确认 | 待产品确认是否覆盖窗口信封/组合包场景 |
| D6 | PDF/A 目标级别（A-1b vs A-2b） | 无 ADR | 推荐 A-2b（LibreOffice 支持好）；CE-O01 落 ADR |
| D7 | 停用版本对在途异步任务 | release-locked（跑完） | 维持，运维手册写清语义即可 |

**拍板记录（2026-07-14）：**

- **D1**：按推荐默认（不做四眼；只做 **CE-G01 同人审批阻断**；四眼待内控要求出现再修订 ADR）
- **D2**：按推荐默认（维持 immediate-only）
- **D3**：按推荐默认（维持否决，不做 webhook）
- **D4**：按推荐默认（继续 deferred；先做契约/文档与实现对齐并写清“不重签”）
- **D5**：按推荐默认（本期不做 addressBlock / 多文档包；后续如有窗口信封/组合包需求再立项）
- **D6**：按推荐默认（选 **PDF/A-2b**；在 CE-O01 单独落 ADR）
- **D7**：按推荐默认（release-locked：停用不取消在途异步任务；运维手册写清语义）

---

## 11. 交给执行模型的固定提示词模板

每个切片开工时，向执行模型提供以下模板（替换尖括号内容）：

```text
你在 Document Generation Engine 仓库工作。执行切片 <CE-ID>：<标题>。

强制流程：
1. 从 origin/main 创建 worktree ../DGE-<slice-id>，分支 feat/<slice-id>。
2. 先读 docs/plan/core-excellence-program-2026-07.md 中 <CE-ID> 的任务卡，
   以及卡内列出的证据文件。禁止扩大范围到卡外文件。
3. 行为变更：先在 docs/behavior/<slice-id>.md 写 BDD 规格（Given/When/Then，
   覆盖任务卡"目标行为"每一条），不确定处停下来问，不要假设。
4. TDD：每个行为先写失败测试再实现。
5. 门禁：后端 mvn -B -ntp -f backend/pom.xml verify 全绿；
   前端 pnpm -C frontend lint && type-check && test && build 全绿；
   前端用户可见变更补 Playwright E2E（Docker 4173）。
6. 完成后更新本计划文件中 <CE-ID> 状态行 + docs/plan/execution-sync-ledger.md
   证据行，同一提交内。
7. 遵守任务卡"禁止"栏。遇到卡内未预见的架构问题，停下来报告，不要自行决策。
```

---

## 12. 追溯矩阵（四轮审查 → CE 任务）

| 审查发现 | 来源轮 | CE 任务 |
| --- | --- | --- |
| 运行时用 live master storageKey，无发布快照 | R2 | CE-K01 |
| Calibri 硬编码 ×6、style catalog 忽略 masterId | R2 | CE-K02 |
| computeExpression 从不求值；无聚合/格式化 | R2 | CE-K03 |
| 中文大写金额缺失、locale 无行为 | R3 | CE-K03 |
| ChangeDiff/PreviewComparison 假 diff；无 A/B 对比 | R2+R3 | CE-K04 |
| 母版 impactAnalysis stub；无 revision diff | R1+R4 | CE-K05 |
| qrBarcode/attachmentList 不支持；tblHeader 不写 | R2 | CE-K06 |
| 无金标回归 | R2 | CE-K07 |
| 条款缺法务元数据 | R2 | CE-K08 |
| 嵌套块不可编辑、无拖拽、校验不定位 | R4 | CE-U01/02 |
| 测试数据手写 JSON、不校验 schema | R4 | CE-U03 |
| 预览只能下载 | R4 | CE-U04 |
| fidelity viewed 不落库、与发布确认脱钩 | R4 | CE-U05 |
| 条款升版模板端零感知 | R4 | CE-U07 |
| 条款审核无待办/驳回不回显 | R4 | CE-U08 |
| 母版审核入口藏深层 + Hub 死代码 | R4 | CE-U09 |
| sharedGroupCodes 无 UI | R1+R4 | CE-U10 |
| 排障无 error envelope；召回无版本过滤 | R3+R4 | CE-U11 |
| 契约页无 curl/payload 示例 | R4 | CE-U12 |
| context 字段不存在、unknown field 静默 | R3 | CE-C01/02 |
| fidelityWarnings 形态漂移 | R3 | CE-C03 |
| 凭证 expires_at 过渡态未收口 | R1+R3 | CE-C04 |
| originalBatchId 零实现 | R3 | CE-C05 |
| DOCX permissions 不生效 | R3 | CE-C06 |
| 自提自批无阻断 | R3 | CE-G01 |
| 测试件无水印 | R3 | CE-G02 |
| 测试数据 PII 明文 | R3 | CE-G03 |
| 无 legal hold | R3 | CE-G04 |
| 审计不可复现 | R3 | CE-G06 |
| 年检/正文检索缺失 | R3 | CE-G05 |
| 导出包不自包含 | R3 | CE-E01 |
| 资产库无管理面 | R1+R3 | CE-E02 |
| PDF/A 缺失 | R3 | CE-O01 |

---

## 13. 维护

完成任一 CE-* 任务时：更新本文件状态 → 更新 `execution-sync-ledger.md` 证据行 →
若与 SOR/OPT/LRP 条目重叠，在对方文件标注 `superseded by CE-*` → 走 post-task-doc-sync。

**Task Master registry (2026-07-14):** umbrella **#53**; leaves **#54–#97** (CE-O02 skipped per D5). **Batch 1 Done:** **#54** CE-K07 (merge `e8f996a0`); **#55** CE-U03 (merge `22bb391f`; tip `0565e1ae`); **#56** CE-C01+C02 (merge `da08f3fe`). **Batch 2 Done:** **#57** CE-K01 (merge `f2db3346`; tip `720a75bd`); **#72** CE-G01 (merge `c187a230`). Formal phase remains **None**. Do **not** implement C03–C06 in this sync.

**Last reviewed:** 2026-07-14（CE-G01 Done #72；Batch 2 complete #57+#72）
