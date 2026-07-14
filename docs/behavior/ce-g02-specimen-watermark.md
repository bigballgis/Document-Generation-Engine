# BDD 行为规格：CE-G02 — SPECIMEN 水印（preview / test-generate）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-G02` |
| **编写日期** | 2026-07-15 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §6 Wave CE-G · CE-G02 |
| **Slice** | `ce-g02-specimen-watermark` |
| **Worktree** | `D:/working/DGE-ce-g02-specimen-watermark` · `feat/ce-g02-specimen-watermark` |
| **Task Master** | **#73** |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED |
| **上游** | CE-K07 骨架 **Done**（#54）；CE-G01 **Done**（#72）；CE-U04 soft-dep（#67，水印可见性） |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；金标约定 [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md)；内联预览软依赖 [ce-u04-inline-pdf-preview.md](./ce-u04-inline-pdf-preview.md) |

**完成声明约束：** 本切片关闭内控缺口「测试件无水印」；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-O02；**不**实现 CE-G06 受控再生（再生件水印由 G06 消费本片能力）。

---

## 1. 概述

管理端预览 / 测试生成路径产出的 DOCX 与 PDF 是**测试件**，必须带有不可忽略的 **SPECIMEN** 水印，避免被误当作正式对外函件。正式 **runtime** 生成路径（sync / async / batch）**零行为改动**：正式产物不得含水印，既有金标 ACTIVE 包与 runtime 装配语义保持稳定。

| 行为域 | 摘要 |
| --- | --- |
| **DOCX 水印** | preview/test-generate 装配后的 DOCX：在**页眉与页脚**写入字面量 `SPECIMEN`（OOXML `header*.xml` / `footer*.xml` 可观察） |
| **PDF 水印** | 同一预览路径：LibreOffice 转换后，以 PDFBox **对角**叠加 `SPECIMEN`（复用 `PdfPageNumberStamper` 的后处理模式：load → append content stream → save） |
| **路径隔离** | 水印**仅**挂在预览装配链（`PreviewGenerationAssemblySupport` 及等价入口）；**不得**无条件写入共享 `DocumentArtifactPipeline` / `DocumentGenerationAssemblySupport` |
| **金标护栏** | 充实 `golden-corpus/06-specimen-watermark`：PLACEHOLDER → **ACTIVE**；断言预览路径有水印、正式路径无水印；既有 ACTIVE 包回归仍绿 |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| 预览装配无水印 | `PreviewGenerationAssemblySupport.assembleAndStore`：`docxAssembler.assembleStructured` → 直接 `objectStorage.put` DOCX → `documentArtifactPipeline.finalizeArtifact(..., "PDF", ...)` → 存 PDF |
| 预览入口统一 | `PreviewGenerationService.testGenerate` / async `testGenerate(..., previewId, ...)` / `runTestGenerateForBatch` 均走同一 `assembly.assembleAndStore` |
| Runtime 独立装配 | `DocumentGenerationAssemblySupport.generate`：钉扎母版 + assemble + `finalizeArtifact` → `generated/...`；与 preview 存储键前缀分离（`previews/` vs `generated/`） |
| PDFBox 后处理先例 | `PdfPageNumberStamper` + `PdfConversionPostProcessor.finishPdf`（页码加盖；失败时 stamper 返回原字节 — **水印不得照搬 fail-open**，见 G02-C8） |
| 金标占位 | `06-specimen-watermark` manifest `maturity=PLACEHOLDER`；`expected/*` `deferred: true`；K07 BDD-CE-K07-015  defer 至本片 |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **TEMPLATE_AUTHOR / TEMPLATE_TESTER** | 管理端编排/测试人员 | test-generate / batch-test / 下载或内联预览看到测试件水印 |
| **Runtime API 调用方** | 凭证调用 sync/async/batch | 正式产物**无** SPECIMEN；字节语义与本片前一致 |
| **系统（Rendering）** | Preview 装配 + PDFBox 对角 stamper | 仅预览路径加水印；失败 fail-closed |
| **平台 / CI** | `mvn verify` + golden-corpus harness | `specimen-watermark` ACTIVE；runtime/formal 无水印护栏 |
| **（间接）法务 / 内控** | 受益方 | 测试件不可被误作正式函 |

---

## 3. Goal

1. 凡经管理端 **preview / test-generate**（含 async preview、batch-test 子预览）成功产出的 DOCX：**页眉与页脚**均可观察到字面量 `SPECIMEN`。
2. 同一路径成功产出的 PDF：文本抽取可观察到 `SPECIMEN`（对角叠加后仍可抽取；不做像素比对）。
3. 正式 **runtime** 路径（`DocumentGenerationAssemblySupport` / sync·async·batch）**不**施加 SPECIMEN；产物文本/OOXML **不得**因本片出现 `SPECIMEN`。
4. 共享 finalize 管线不得变成「无条件水印」；水印逻辑作用域限定预览装配（或显式 `specimen=true` 且仅预览调用）。
5. 金标包 `specimen-watermark` 升为 **ACTIVE**，纳入 `mvn verify`；既有 ACTIVE 包（至少 `nested-clauses`、`encrypted-pdf`）持续绿灯。
6. CE-U04 软依赖可兑现：内联 PDF 预览消费的字节流已含水印（本片不改 pdf.js UI；断言以产物为准）。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G02-C1** | 水印字面量为英文 **`SPECIMEN`**（精确大小写）；不做「样本」中文变体；English-first。 | 计划 §6 CE-G02；i18n English-first |
| **G02-C2** | DOCX：在**页眉与页脚均**写入 `SPECIMEN`（至少一个 header part **且**至少一个 footer part 含该子串）。不得仅改 `document.xml` 正文冒充水印。 | 计划「DOCX 页眉页脚」 |
| **G02-C3** | PDF：LibreOffice 转换**之后**，用 PDFBox 对角叠加 `SPECIMEN`（模式对齐 `PdfPageNumberStamper`：`Loader.loadPDF` → 每页 `PDPageContentStream.AppendMode.APPEND` → `document.save`）。允许与页眉页脚文本水印并存。 | 计划「PDF 对角水印」 |
| **G02-C4** | 水印**仅**作用于预览装配链产出并写入 `previews/{previewId}/output.docx|pdf` 的字节。覆盖：`POST .../previews/test-generate`、async preview orchestrator、`POST .../previews/batch-test` 触发的子 `runTestGenerateForBatch`。 | 代码路径 + 计划「预览/test-generate」 |
| **G02-C5** | 正式 runtime 路径**零改动目标**：不调用 specimen stamper；不在 `DocumentGenerationAssemblySupport` 插入水印步骤。既有 ACTIVE 金标包（formal assemble）不得因本片新增 `SPECIMEN` 断言失败。 | 计划「正式 runtime 路径零改动」 |
| **G02-C6** | 「bitwise 不变」操作性定义：本片不得修改会改变**所有**正式产物字节的共享无条件后处理。护栏 =（a）runtime/formal 产物 **TEXT/XML 不含** `SPECIMEN`；（b）既有 ACTIVE 金标包 verify 仍绿；（c）实现审查确认 watermark 调用点仅在 preview 装配。不要求跨环境对 LibreOffice 输出做绝对字节恒等。 | 计划「金标护栏」+ K07 禁像素约定 |
| **G02-C7** | 金标：`06-specimen-watermark` → `ACTIVE`；expected 含 preview 路径「含 SPECIMEN」断言，以及 formal/runtime 同输入「不含 SPECIMEN」断言。Harness 可扩展双路径，或由本片配套测试驱动 preview 路径 + formal 路径对照；目录 ID 仍为 `specimen-watermark`（K07-C11）。 | K07-015 / package README |
| **G02-C8** | 预览路径水印失败（DOCX 写入或 PDF 对角 stamp 抛错/无法完成）→ **fail-closed**：preview 记 `FAILED`，不落「无水印成功件」；**禁止**静默返回未水印成功产物。 | 内控硬控制；对比页码 stamper 的 fail-open 不得复用 |
| **G02-C9** | 可观测性以 **OOXML 子串 / PDF 文本抽取** 为准；**禁止**像素/截图/SSIM 作为 Done 证据（对齐 K07-C6）。对角角度、字号、透明度为实现细节，只要文本可抽取且肉眼可辨认为对角覆盖即可。 | K07 + 计划 |
| **G02-C10** | 预览路径 PDF 当前使用 `NO_ENCRYPTION`；本片**不**扩展预览加密。加密 PDF 主题仍由 `encrypted-pdf` / runtime 拥有。 | 现状 `PreviewGenerationAssemblySupport` |
| **G02-C11** | 本片**不**改管理 UI 文案/按钮（除产物字节导致 CE-U04 内联可见水印）。OpenAPI 契约字段**不**因本片强制新增（水印为渲染副作用，非请求开关）。 | 范围 |
| **G02-C12** | **明确非目标：** go-live、CD-3、CE-O02、CE-G06 受控再生 API、CE-G03 PII、四眼复核、修改 ADR-0021、对 runtime 加可选水印开关。 | 计划 out of scope |

---

## 5. 前置条件

- CE-K07 金标骨架与 harness 已在 `mvn verify` 中运行；`06-specimen-watermark` 目录存在且为 PLACEHOLDER。
- CE-G01 已合并（本片无硬代码依赖，仅批次顺序）。
- Preview 链路可用：`PreviewGenerationService` + `PreviewGenerationAssemblySupport` + artifact download。
- Runtime 链路可用：`DocumentGenerationAssemblySupport` + `DocumentArtifactPipeline`。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- 用户（或 E2E）调用管理端 `POST /api/management/v1/templates/{templateId}/previews/test-generate`（或 async / batch-test）。
- 用户下载 `.../previews/{previewId}/artifacts/docx|pdf`，或 CE-U04 内联加载 PDF 字节。
- Runtime 调用方触发已发布模板的 sync/async/batch 生成。
- CI 执行 `mvn -B -ntp -f backend/pom.xml verify`（含 golden-corpus）。

---

## 7. Primary journey（预览测试件加水印）

1. 授权通过的 TEMPLATE_AUTHOR / TEMPLATE_TESTER 对某模板发起 test-generate（或 batch-test / async preview）。
2. 系统走预览装配：结构化 assemble → **施加 DOCX 页眉+页脚 SPECIMEN** → 存 `previews/{id}/output.docx`。
3. 系统将（已水印）DOCX 转为 PDF → **PDFBox 对角 SPECIMEN** → 存 `previews/{id}/output.pdf`。
4. Preview 记录 `SUCCEEDED`；下载 / 内联预览得到的字节含 `SPECIMEN`。
5. 同一模板若经 runtime 正式生成：产物**不含** `SPECIMEN`；装配不经过预览水印步骤。

---

## 8. System responses

### 成功（预览）

- HTTP 200 + `PreviewRecordView`（既有信封）；`status=SUCCEEDED`。
- DOCX artifact：header 与 footer 含 `SPECIMEN`。
- PDF artifact：抽取文本含 `SPECIMEN`。
- 存储键仍为 `previews/{previewId}/output.docx|pdf`（本片不改键约定）。

### 成功（runtime）

- 既有成功语义不变；DOCX/PDF **不含** `SPECIMEN`。

### 失败（预览水印）

- 水印步骤失败 → preview `FAILED`；客户端收到既有 `api.error.rendering.generationFailed`（或本片若引入专用 messageKey，须同步 i18n catalog — 默认复用 generationFailed，避免无必要新码）。
- **不得**返回 `SUCCEEDED` 且产物无水印。

### 授权

- 预览仍走既有 `TemplatePreviewAuthorizationPort.requireReadableSnapshot`；本片**不**放宽或收紧授权矩阵。

---

## 9. Acceptance scenarios

### A. DOCX 预览水印

#### BDD-CE-G02-DOCX-001 — test-generate DOCX 页眉页脚含 SPECIMEN

```gherkin
Given 授权用户对可预览模板发起 POST .../previews/test-generate 且装配成功
When 下载 GET .../previews/{previewId}/artifacts/docx
Then DOCX 的 header part(s) 文本含 "SPECIMEN"
And footer part(s) 文本含 "SPECIMEN"
And word/document.xml 正文可不含 SPECIMEN（允许但不作为唯一证据）
```

#### BDD-CE-G02-DOCX-002 — batch-test 子预览 DOCX 同样含水印

```gherkin
Given 授权用户发起 POST .../previews/batch-test 且至少一条子预览 SUCCEEDED
When 下载该子预览的 DOCX artifact
Then header 与 footer 均含 "SPECIMEN"
```

#### BDD-CE-G02-DOCX-003 — async preview DOCX 同样含水印

```gherkin
Given async preview orchestrator 分配 previewId 并成功完成 testGenerate
When 下载该 previewId 的 DOCX
Then header 与 footer 均含 "SPECIMEN"
```

---

### B. PDF 预览水印

#### BDD-CE-G02-PDF-001 — test-generate PDF 对角水印可抽取

```gherkin
Given 授权用户 test-generate 成功且 PDF 已存储
When 下载 GET .../previews/{previewId}/artifacts/pdf 并用 PDFBox（或 harness）抽取文本
Then 抽取文本包含 "SPECIMEN"
And 断言方式为文本包含（禁止像素比对）
```

#### BDD-CE-G02-PDF-002 — 多页 PDF 每页可见水印语义

```gherkin
Given 预览 PDF 总页数 ≥ 2
When 对每一页抽取或检查叠加内容
Then 每一页均可观察到 SPECIMEN（文本抽取或等价 PDFBox 内容断言）
```

#### BDD-CE-G02-PDF-003 — CE-U04 内联消费同一含水印字节（软依赖兑现）

```gherkin
Given CE-U04 内联预览加载的是 preview PDF download 字节流
When 用户查看第 1 页
Then 渲染结果与下载 PDF 一致地呈现 SPECIMEN（与 BDD-CE-U04-IPP-004 对齐）
```

> 本片以**后端产物断言**为 Done 主证据；浏览器视觉 E2E 为推荐补强，不阻塞后端金标绿灯。

---

### C. Runtime / formal 零水印

#### BDD-CE-G02-RT-001 — runtime sync 产物不含 SPECIMEN

```gherkin
Given 已发布模板可经 runtime sync 生成 DOCX 与/或 PDF
When 调用正式生成 API 并取得产物
Then DOCX OOXML 与 PDF 抽取文本均不含 "SPECIMEN"
```

#### BDD-CE-G02-RT-002 — runtime async / batch 同样无水印

```gherkin
Given 已发布模板支持 async 或 batch 生成
When 任务成功完成并取得产物
Then 产物不含 "SPECIMEN"
```

#### BDD-CE-G02-RT-003 — 共享管线无无条件水印

```gherkin
Given 代码审查 / 架构门禁
When 检索 specimen / SPECIMEN watermark 调用点
Then 调用仅出现在预览装配路径（或仅被预览调用的显式 helper）
And DocumentGenerationAssemblySupport 不调用该 helper
```

---

### D. 金标护栏

#### BDD-CE-G02-GOLD-001 — specimen-watermark 升为 ACTIVE

```gherkin
Given 包 backend/src/test/resources/golden-corpus/06-specimen-watermark
When 本片交付完成
Then manifest.maturity = ACTIVE
And expected/docx-assertions.json 与 pdf-assertions.json 不再 deferred 空断言
And mvn verify 执行该包业务断言
```

#### BDD-CE-G02-GOLD-002 — 金标：预览路径含 SPECIMEN；formal 路径不含

```gherkin
Given specimen-watermark 包对同一 input 分别走预览水印路径与 formal assemble 路径
When harness / 配套测试运行
Then 预览路径 DOCX/PDF 断言含 "SPECIMEN"
And formal 路径 DOCX/PDF 断言不含 "SPECIMEN"
```

#### BDD-CE-G02-GOLD-003 — 既有 ACTIVE 包无回归

```gherkin
Given nested-clauses 与 encrypted-pdf 为 ACTIVE
When mvn verify
Then 两包既有断言仍通过（本片未向 formal 产物注入 SPECIMEN）
```

---

### E. 边界与失败

#### BDD-CE-G02-X-001 — 水印失败 fail-closed

```gherkin
Given 预览装配已成功 assemble DOCX
When DOCX 或 PDF specimen stamp 步骤失败
Then preview 状态为 FAILED
And 不得持久化 SUCCEEDED 的无水印 output.docx/output.pdf 作为成功件
```

#### BDD-CE-G02-X-002 — 预览授权失败不产生水印副作用

```gherkin
Given 用户对模板无预览读权限
When 调用 test-generate
Then 既有授权失败语义保持
And 不创建成功预览产物
```

#### BDD-CE-G02-X-003 — 装配失败仍走既有 FAILED（水印未执行）

```gherkin
Given assembleStructured 抛错
When test-generate
Then preview FAILED（既有）
And 不要求执行水印步骤
```

#### BDD-CE-G02-X-004 — 母版已有页眉页脚时仍追加 SPECIMEN

```gherkin
Given 母版 DOCX 已含业务页眉/页脚内容
When 预览装配加水印
Then 页眉与页脚在保留业务内容的前提下仍可观察到 "SPECIMEN"
And 不得删除母版原有页眉页脚语义（允许追加/叠加）
```

---

### F. 非目标护栏

#### BDD-CE-G02-OUT-001 — 不实现 CE-G06 / CE-O02 / go-live

```gherkin
Given 本片变更集
When 审查范围
Then 无 CE-G06 受控再生 API、无 CE-O02、无 go-live/CD-3 宣称、无 runtime 水印开关产品化
```

---

## 10. Boundary / exception 汇总

| 场景 | 期望 |
| --- | --- |
| 预览成功 | DOCX 眉脚 + PDF 对角均有 `SPECIMEN` |
| Runtime 成功 | 无 `SPECIMEN` |
| 水印 stamp 失败 | Preview `FAILED`；无成功无水印件 |
| 无 soffice（CI） | 金标 PDF 半段可按 K07-C9 `assumeTrue` 跳过；DOCX 水印半段必须执行 |
| 像素比对 | **禁止** |
| 预览加密 PDF | 本片不引入 |
| 授权失败 | 既有 fail-closed；本片不改矩阵 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| Backend 单测 / 集成 | Preview DOCX header/footer 与 PDF 文本含 `SPECIMEN`；runtime 对照不含 |
| Golden corpus | `06-specimen-watermark` ACTIVE；`mvn verify` |
| 既有 ACTIVE 回归 | `nested-clauses` / `encrypted-pdf` 仍绿 |
| 架构审查 | watermark 调用点仅预览路径 |
| CE-U04（推荐） | 内联 PDF 可见水印，兑现 IPP-004 |
| 门禁 | `mvn -B -ntp -f backend/pom.xml verify` **必须**；前端 gates 仅当本片改 FE 时适用（默认 backend-only） |

---

## 12. Traceability

| 来源 | 链接 |
| --- | --- |
| 计划 CE-G02 | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §6 |
| R3 缺口「测试件无水印」 | 同文档 §1 / §11 |
| Task Master | `#73` CE-G02 |
| 金标骨架 | [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md) BDD-CE-K07-015 |
| 内联预览软依赖 | [ce-u04-inline-pdf-preview.md](./ce-u04-inline-pdf-preview.md) BDD-CE-U04-IPP-004 |
| PDFBox 后处理先例 | `PdfPageNumberStamper` / `PdfConversionPostProcessor` |
| 预览装配 | `PreviewGenerationAssemblySupport` |
| Runtime 装配 | `DocumentGenerationAssemblySupport` |
| 预览 API | `PreviewController` `POST .../test-generate`、`batch-test`、artifacts |

---

## 13. Implementation notes（非需求，供 TDD）

- 建议新增专用 `SpecimenWatermarkStamper`（PDF）与 DOCX header/footer writer，**不要**把 specimen 逻辑塞进 `PdfPageNumberStamper`（职责分离：页码 ≠ 水印）。
- 预览路径顺序建议：assemble → DOCX specimen → store DOCX → convert PDF → PDF specimen → store PDF。
- 金标 harness 若仍只跑 formal assemble：本片须扩展 runner 或增加平行测试类覆盖 preview 水印路径；不得仅靠 formal 路径「意外」出现 SPECIMEN。

---

## 14. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-g02-specimen-watermark.md
task_ids: [CE-G02, slice:ce-g02-specimen-watermark, taskmaster:#73]
scenario_ids:
  - BDD-CE-G02-DOCX-001 … 003
  - BDD-CE-G02-PDF-001 … 003
  - BDD-CE-G02-RT-001 … 003
  - BDD-CE-G02-GOLD-001 … 003
  - BDD-CE-G02-X-001 … 004
  - BDD-CE-G02-OUT-001
```

**Handoff to plan-orchestrator / rendering-engineer：** 以本文件场景写 Red 测试；先充实金标与预览装配；禁止改 runtime 装配语义。
