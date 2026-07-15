# CE-K06 渲染保真补全 — BDD（K06a/K06b shipped · Wave 3 = K06c）

| Field | Value |
| --- | --- |
| **Slice (Wave 3)** | `ce-k06c-attachment-pdf-stamp` |
| **Plan task** | **CE-K06**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §3 CE-K06） |
| **Task Master** | **#62**（sub-slice **K06c** — closes residual after K06a+K06b） |
| **bdd_readiness** | **`ready`** for **K06c** — USER/PARENT 已确认 K06c-Q1…Q3（§18.10）；stamp `BDD-CE-K06c-004…007` 保持不变。K06a/K06b 历史规格仍 **`ready`/shipped** |
| **Recorded** | 2026-07-15（K06a）；2026-07-15 Wave 2 K06b；**2026-07-15 Wave 3 K06c**；**2026-07-15 K06c locks confirmed → ready** |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k06c-attachment-pdf-stamp` · `feat/ce-k06c-attachment-pdf-stamp` · base `a689ca87` |
| **Scope of this slice** | **K06c only** — (1) `attachmentListRef` DOCX writer：结构化附件清单 → **编号列表段落**；退出 LR-A4 writer-unsupported set；(2) PDF 页码 stamp **按发布锁定 `renderProfile`（包级）** 控制，**不得**用全局 `pdf-page-number-stamping-enabled` 绕过已锁定 profile。**不**重做 K06a（tblHeader）或 K06b（ZXing `qrBarcodeRef`）；**不**改 CE-C04 / CE-U06；**不** go-live；**不**激活 CD-3 |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；产品 [PRD.md](../product/PRD.md)；需求 [requirements-plan.md](../requirements/requirements-plan.md)；领域 [domain-model.md](../domain/domain-model.md) §2.6.5 / §2.6.8 / §2.6.11；金标 [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md)；writer-unsupported 正交 [lrp-a4-fail-closed-unsupported-nodes.md](./lrp-a4-fail-closed-unsupported-nodes.md) |

---

## 1. 概述

CE-K06 关闭渲染保真三类缺口，可独立子片交付。

| 子片 | 状态 | 痛点摘要 |
| --- | --- | --- |
| **K06a** | **Shipped**（merge `485a7f3e`） | `repeatHeaderAcrossPages` → DOCX `<w:tblHeader/>`；金标 `02-cross-page-table` ACTIVE — 见 §2–§11 / `BDD-CE-K06a-*`（**本 Wave 不重做**） |
| **K06b** | **Shipped on MAIN tip `a689ca87`** | `qrBarcodeRef` ZXing PNG writer；金标 `09-qr-barcode` — 见 §15–§17 / `BDD-CE-K06b-*`（**本 Wave 不重做**） |
| **K06c** | **In scope / BDD `ready`（locks confirmed）** | `attachmentListRef` → `string[]` 编号列表段落 writer；金标 `10-attachment-list`；PDF stamp 按锁定 profile — 见 **§18–§20** |

**现状证据（K06c，基于 MAIN `a689ca87` + 本 worktree）：**

- `WriterUnsupportedStructuredNodeTypes` = `{ attachmentListRef }` 仅（K06b 后）；发布硬阻断 + 渲染 `api.error.rendering.unsupportedNodeType`。
- `ReferenceNodeService` 已校验非空 `referenceKey` 并解析 `AttachmentListReferenceModel`；**无** DOCX 发射分支。
- `PdfConversionPostProcessor.isStampingEnabled(RenderProfile)`：profile=`true` → 启用；profile=`false` 时仍 **OR** 回落到 `docgen.rendering.pdf-page-number-stamping-enabled`（全局默认 `false`）— 与「按包 profile、非全局布尔」目标冲突。
- 默认 profile 资产 `authoring/default-render-profile-v1.json` 已含 `pdfPageNumberStampingEnabled: true`；领域模型 §2.6.8/§2.6.11 已声明 profile 字段与调用方不可覆盖。

---

## 2–11. K06a（shipped — do not redo）

> **Historical SoT for K06a.** Acceptance `BDD-CE-K06a-001…006` remains authoritative for tblHeader behavior. Wave 2 implementers **must not** reopen K06a writer/golden work unless a regression is found.

### Actor / Role（K06a）

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **系统（Rendering pipeline / DocxAssembler）** | 结构化表格 → DOCX 发射 | `repeatHeaderAcrossPages=true` 时表头行带 `w:tblHeader` |
| **Runtime / Preview 生成路径** | 消费已发布绑定与变量 | 产物 DOCX 跨页表头可被 Word 重复；XML 可断言 |
| **平台 / CI** | 金标 harness + `mvn verify` | `cross-page-table` ACTIVE；XPath 失败即红 |

### Goal（K06a）

1. `repeatHeaderAcrossPages == true` → 表头行写 `<w:tblHeader/>`。
2. `false` / 缺省 → 不写 `w:tblHeader`。
3. 金标 `02-cross-page-table` ACTIVE（DOCX XML 断言；禁像素）。

### 已确认决策（K06a，2026-07-15）

| ID | 决策 |
| --- | --- |
| **K06-C1…C9** | 见原 Wave 1a 锁定（范围 = K06a only；发射落点 `StructuredContentDocxTableSupport`；与当时 LR-A4 正交保留 `qrBarcodeRef`/`attachmentListRef` unsupported）。**K06-C1 / C8 / C9 中「本片不实现 K06b」已被本 Wave 2  supersede：K06b 现为本片范围。** |

### Acceptance scenarios — K06a

#### BDD-CE-K06a-001 — 跨页表头写入 `w:tblHeader`

**Given** 结构化表格定义含非空 `headerRows`、合法 `columnSchema`，且 `"repeatHeaderAcrossPages": true`  
**And** 绑定变量使循环行足够填充表格  
**When** 渲染管线将 `tableComponent` / `tableComponentRef` 写入 DOCX  
**Then** 解压产物 `word/document.xml` 中至少存在一处 `w:tblHeader`  
**And** 该元素位于表头行的 `w:trPr`（或 POI 写出的等价结构）下

#### BDD-CE-K06a-002 — 关闭跨页重复时不写 `tblHeader`

**Given** 同结构表格但 `"repeatHeaderAcrossPages": false`（或字段缺省）  
**When** 渲染写出 DOCX  
**Then** 该表的表头行 **不** 含 `w:tblHeader`  
**And** 文档仍成功生成（非错误路径）

#### BDD-CE-K06a-003 — 仅表头行带 `tblHeader`

**Given** `repeatHeaderAcrossPages: true`，且存在 loop 数据行与（可选）footer 行  
**When** 渲染写出 DOCX  
**Then** `w:tblHeader` 仅出现在 header 行对应 `w:tr`  
**And** loop / footer 行的 `w:trPr` **不含** `w:tblHeader`

#### BDD-CE-K06a-004 — 金标 `cross-page-table` → ACTIVE

**Given** K06a 交付完成  
**When** 检查 `golden-corpus/02-cross-page-table/manifest.json`  
**Then** `maturity` 为 `ACTIVE`  
**And** `expected/docx-assertions.json` 非 `deferred`，且断言 `w:tblHeader` 存在  
**And** harness 在 `mvn verify` 中执行该包 DOCX 半段并通过

#### BDD-CE-K06a-005 — Fail-first 回归

**Given** 实现前（或故意破坏 tblHeader 写出）  
**When** 运行针对 `repeatHeaderAcrossPages=true` 的 writer / 装配测试或金标断言  
**Then** 测试失败（Red）  
**And** 最小实现写出 `w:tblHeader` 后同测试通过（Green）

#### BDD-CE-K06a-006 — 多表头行（若写出）

**Given** 定义含多个 `headerRows` 且 writer 将每一行作为表头写出，且 `repeatHeaderAcrossPages: true`  
**When** 渲染写出 DOCX  
**Then** 每一个写出的表头行均带 `w:tblHeader`  
**Note** 若 v1 writer 仍只写第一表头行，则本场景退化为对第一行的断言。

---

## 12. Residual pointer — superseded by §18（K06c）

> **Historical:** Wave 2 将 K06c 记为 deferred stub（原 §12.2）。**Wave 3** 已提升为完整规格草稿 → **§18–§20**（`BDD-CE-K06c-001…`）。本片 **不**重做 K06a/K06b。

---

## 13. Traceability（CE-K06 family）

| Source | Link |
| --- | --- |
| Plan card | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) § CE-K06 |
| Task Master | `#62` CE-K06 · sub-slice **K06c**（本 Wave）；K06a/K06b historical |
| PRD | [PRD.md](../product/PRD.md) — 附件清单引用；跨页表头；QR writer；K06c residual → Wave 3 |
| Requirements | [requirements-plan.md](../requirements/requirements-plan.md) |
| Domain | [domain-model.md](../domain/domain-model.md) §2.6.4（K06a）、§2.6.5（K06b/K06c）、§2.6.8 / §2.6.11（PDF stamp profile） |
| Golden corpus | [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md) — K06b `09-qr-barcode`；K06c `10-attachment-list` ACTIVE（同 harness 根） |
| Writer-unsupported | [lrp-a4-fail-closed-unsupported-nodes.md](./lrp-a4-fail-closed-unsupported-nodes.md) — K06b 后 `{ attachmentListRef }`；K06c 后空集 |
| K06a merge | `485a7f3e` |
| K06b base | MAIN tip `a689ca87`（handoff） |

---

## 14. BDD readiness — K06a (historical)

```
bdd_readiness: ready
k06_scope_slice: a
status: shipped (merge 485a7f3e)
owning_doc: docs/behavior/ce-k06-rendering-fidelity.md
task_ids: [#62, CE-K06, K06a]
```

---

## 15. Wave 2 — K06b `qrBarcodeRef` writer

### 15.1 Actor / Role（K06b）

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **系统（Rendering pipeline / DocxAssembler）** | 结构化引用节点 → DOCX 发射 | ZXing 生成 PNG；嵌入 drawing/blip；配置生效 |
| **Runtime / Preview 生成路径** | 变量 payload + 已发布绑定 | 成功嵌入可扫描码；失败显式错误 |
| **发布门禁 / 绑定校验** | LR-A4 writer-unsupported 集合 | `qrBarcodeRef` 退出 unsupported；`attachmentListRef` 仍阻断 |
| **平台 / CI** | 金标 + `mvn verify` | `09-qr-barcode` ACTIVE；DOCX 断言 + 可选 decode 回归 |
| **（间接）函件读者** | 扫码可达性 | 码可读；尺寸/纠错符合模板配置 |

本片 **无** 新管理 UI 旅程、**无** 新公开 REST 契约（节点字段为既有 structured JSON `additionalProperties` 扩展；错误走既有渲染失败信封）。

### 15.2 Goal（K06b）

1. 合法 `qrBarcodeRef` 在顶层与嵌套路径均可写出 DOCX **嵌入图片**（PNG part + 文档中的 drawing），**禁止** 静默省略。
2. 用 **ZXing**（`com.google.zxing`）从运行时变量 payload 生成码图（默认 QR）。
3. **尺寸**（`sizePx`）与 **纠错级别**（`errorCorrection`，QR）可配置；非法配置 → 显式失败。
4. 从 `WriterUnsupportedStructuredNodeTypes` **仅移除** `qrBarcodeRef`；发布门禁与绑定校验对合法 QR 节点 **放行**（不再 `UNSUPPORTED_NODE` / LR-A4 checklist 阻断）。
5. 金标包 `golden-corpus/09-qr-barcode/` → **ACTIVE**（DOCX 关键路径/关系断言；禁像素比对）。
6. Fail-first 单测/装配测试驱动 writer。

### 15.3 已确认决策（K06b，2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **K06b-C1** | **本片交付范围 = K06b only。** 不重做 K06a；不实现 K06c；不改 CE-C04 / CE-U06；不 go-live；不激活 CD-3。 | 编排 handoff |
| **K06b-C2** | **库：** **ZXing**（`com.google.zxing:core` + 写出 PNG 所需的 `javase` 或等价 Matrix→PNG 路径）。引入前在公司批准仓库核验版本可用性；不得静默换成未确认库。 | CE-K06 计划卡 |
| **K06b-C3** | **Payload：** 从渲染/预览 **`variables` map** 按节点 `referenceKey`（trim 后）取值；值必须为非空字符串（或可稳定 `String.valueOf` 的标量）。**不**依赖 CE-E02 资产库 / MinIO 对象（E02 键名约定不阻塞本片）。缺失、空白、或无法编码为文本 → fail-closed。 | stub + E02 不阻塞 |
| **K06b-C4** | **节点配置字段（structured JSON，可选）：** (1) `sizePx` — 正整数，**默认 128**，合法范围 **[32, 512]**（含端点）；同时作为生成位图边长（QR 方形）与 Word 显示尺寸（`Units.pixelToEMU(sizePx)`）。(2) `errorCorrection` — `L` \| `M` \| `Q` \| `H`（大小写不敏感），**默认 `M`**；仅 `format=QR_CODE` 使用。(3) `format` — `QR_CODE`（默认）\| `CODE_128`。未知/越界 → 显式配置错误。 | stub「尺寸/纠错可配」+ PRD 二维码/条码 |
| **K06b-C5** | **发射：** Writer（`StructuredContentDocxInlineSupport` / block dispatch 同源路径）对 `qrBarcodeRef` 生成 PNG 字节并 `XWPFRun.addPicture`（`PICTURE_TYPE_PNG`）；顶层块节点与段落 inline 子节点均须覆盖。 | 代码现状（seal/image 路径） |
| **K06b-C6** | **LR-A4 收缩：** 实现后权威 set = `{ attachmentListRef }` 仅。`WriterUnsupportedStructuredNodeTypes`、绑定 `UNSUPPORTED_NODE`、发布门禁专用项、一致性单测必须同源更新。既有「unsupported」渲染测试对 `qrBarcodeRef` **改为成功路径或删除并替换为 encode 回归**。 | LR-A4-C4 |
| **K06b-C7** | **Fail-closed messageKey（English-first）：** payload 缺失/空白 → `api.error.rendering.qrBarcodePayloadMissing`；非法 size/EC/format → `api.error.rendering.qrBarcodeConfigInvalid`；ZXing 编码失败 → `api.error.rendering.qrBarcodeEncodeFailed`。均 **非** `unsupportedNodeType`。禁止静默 skip。 | LR-A4 精神；本片细化 |
| **K06b-C8** | **嵌套：** `conditionBlock` / `loopBlock` children、pinned `contentModuleRef` 展开、inline paragraph children — 与顶层相同发射/失败语义（延续 LR-A4 嵌套可达性，改为成功或显式业务错误）。 | LR-A4 A4 |
| **K06b-C9** | **金标：** 新增 `backend/src/test/resources/golden-corpus/09-qr-barcode/`（复用 K07 根目录与 harness 约定；`maturity: ACTIVE`）。input：母版 + 含 `qrBarcodeRef` 的绑定 + variables 提供 payload；`expected/docx-assertions.json` 断言存在嵌入图片（如 `a:blip` / `w:drawing` / relationship image 目标之一，稳定即可）。**禁止** 像素/截图 golden。PDF 半段可选；无 soffice → skip（K07-C9）。单元测试可额外 ZXing **decode** 嵌入 PNG 校验 payload 往返（推荐，非像素比对）。 | CE-K07「K01–K06 加样本」；handoff |
| **K06b-C10** | **缩放：** `applyScaling` **不**对 QR/条码产生 `IMAGE_SCALING_ADJUSTED` warning（保持现网）；writer **不得**对码图做「适配容器」缩放逻辑。本片不强制把 `applyScaling=true` 升为 seal 级 blocker（现网对 QR 忽略该字段）。 | PRD；ReferenceNodeServiceTest |
| **K06b-C11** | **非目标：** K06c；CE-E02 资产目录 UI；QR 管理面配置实体；缓存/异步预生成；像素级视觉回归；新权限码；调用方覆盖 renderProfile；CE-C04 / CE-U06。 | handoff |

### 15.4 Preconditions

- K06a 已在 main（`485a7f3e`）；本 worktree 基于可含 K06a 的 integration base。
- LR-A4 fail-closed 基础设施存在（单一权威 unsupported set）。
- CORE-FORTRESS F1 结构化 writer / `DocxAssembler` 路径可用。
- CE-K07 golden harness 可发现新包目录。
- 隔离 worktree 交付；不在 MAIN 实现。

### 15.5 Trigger

- Runtime / preview / test-generate 装配含 `qrBarcodeRef` 的结构化内容，且 variables 提供对应 payload。
- 发布门禁评估含 `qrBarcodeRef`（且无 `attachmentListRef`）的草稿版本。
- CI / 工程师执行 `mvn -B -ntp -f backend/pom.xml verify`（含 golden-corpus）。

### 15.6 Primary journey（K06b）

1. 作者在锚点结构化 JSON 插入 `"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"`，可选 `sizePx` / `errorCorrection` / `format`。
2. 绑定校验：`referenceKey` 非空；**不再**因 writer-unsupported 阻断 `qrBarcodeRef`（`attachmentListRef` 仍阻断）。
3. 发布门禁对仅含合法 QR 引用的版本放行（就 writer-unsupported 项而言）。
4. 生成/预览时 variables 含 `"PAYMENT-QR":"https://pay.example/abc"`（或测试夹具字符串）。
5. Writer 调 ZXing 生成 PNG → 嵌入 DOCX。
6. 产物可解压；drawing/image part 可断言；可选 decode 得原 payload。
7. 金标 `09-qr-barcode` ACTIVE 在 verify 中通过。

### 15.7 System responses

| 情况 | 系统响应 |
| --- | --- |
| 合法 QR + 非空 payload + 合法/缺省配置 | 成功 DOCX；含嵌入 PNG；非 `unsupportedNodeType` |
| `sizePx` / `errorCorrection` / `format` 缺省 | 使用默认 128 / `M` / `QR_CODE` |
| 非法配置（越界 size、未知 EC、未知 format） | 显式失败 `qrBarcodeConfigInvalid`；不写残缺图 |
| variables 缺 key 或空白 payload | 显式失败 `qrBarcodePayloadMissing` |
| ZXing 无法编码（极端 payload / CODE_128 非法字符等） | 显式失败 `qrBarcodeEncodeFailed` |
| 仍含 `attachmentListRef` | 保持 LR-A4：发布阻断 + 渲染 `unsupportedNodeType` |
| 金标 ACTIVE DOCX 断言失败 | `mvn verify` **FAIL** |
| 金标 PDF 半段且无 soffice | **SKIP** PDF；DOCX 仍必须 PASS |

### 15.8 Boundary / exception（K06b）

| 边界 | 行为 |
| --- | --- |
| `referenceKey` 空白 | 既有 `MISSING_REFERENCE_KEY` 绑定 blocker（发布前）；若绕过直达 writer → 与 payload missing 同类显式失败 |
| 超长 payload | ZXing 能编码则成功；不能则 `qrBarcodeEncodeFailed`（不截断静默） |
| `format=CODE_128` + 提供 `errorCorrection` | **忽略** EC（不报错），仅用 format+payload+size |
| 非字符串变量值（number/boolean） | 允许稳定字符串化后编码；`null` → payload missing |
| Authorization | 无新权限码；沿用既有生成/预览授权 fail-closed |
| 像素断言 | **禁止** |

### 15.9 Observable evidence

| 证据 | 说明 |
| --- | --- |
| DOCX package | `word/media/*` PNG + `document.xml` drawing/`a:blip` |
| 单测 decode（推荐） | ZXing 读回嵌入 PNG，payload 匹配 |
| 金标 | `09-qr-barcode` ACTIVE + verify |
| Unsupported set | 一致性测试：仅 `attachmentListRef` |
| 发布门禁 | 含 `qrBarcodeRef` 的版本不再因该类型硬阻断 |
| 错误信封 | 稳定 `messageKey` + `retryable=false`（业务失败） |

---

## 16. Acceptance scenarios — K06b（Given / When / Then）

### BDD-CE-K06b-001 — 合法 `qrBarcodeRef` 嵌入 QR 图片并退出 unsupported

**Given** 锚点结构化 JSON 含 `"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"`（合法非空 key）  
**And** 生成变量含 `"PAYMENT-QR":"https://pay.example/k06b"`  
**And** 本片实现已完成  
**When** 渲染管线写出 DOCX  
**Then** 产物含至少一处嵌入图片（`w:drawing` / `a:blip` 或等价 media relationship）  
**And** `WriterUnsupportedStructuredNodeTypes` **不再**包含 `qrBarcodeRef`  
**And** 渲染 **不** 抛 `api.error.rendering.unsupportedNodeType`（因该节点）

### BDD-CE-K06b-002 — 默认尺寸与纠错级别

**Given** `qrBarcodeRef` **未**指定 `sizePx` / `errorCorrection` / `format`  
**And** variables 提供非空 payload  
**When** 渲染写出 DOCX  
**Then** 成功嵌入图片  
**And** 生成使用默认 **`sizePx=128`**、**`errorCorrection=M`**、**`format=QR_CODE`**（可由单测对生成器入参或 decode/尺寸断言验证）

### BDD-CE-K06b-003 — 可配置 `sizePx` 与 `errorCorrection`

**Given** 节点为 `"sizePx":256,"errorCorrection":"H","format":"QR_CODE"`（或省略 format）  
**And** 非空 payload  
**When** 渲染写出 DOCX  
**Then** 嵌入图显示尺寸对应 256px（EMU）且生成器使用 EC level **H**  
**And**（推荐）decode 得原 payload

### BDD-CE-K06b-004 — 非法配置显式失败

**Given** `sizePx` 为 `16` 或 `999`，**或** `errorCorrection` 为 `"X"`，**或** `format` 为 `"AZTEC"`（未支持）  
**When** 渲染  
**Then** 失败且 `messageKey=api.error.rendering.qrBarcodeConfigInvalid`  
**And** **不** 静默省略该节点；**不** 产出假装成功的无码文档

### BDD-CE-K06b-005 — Payload 缺失 fail-closed

**Given** 合法 `qrBarcodeRef` 但 variables **无**对应 key 或值为空白  
**When** 渲染  
**Then** 失败且 `messageKey=api.error.rendering.qrBarcodePayloadMissing`  
**And** 禁止静默省略

### BDD-CE-K06b-006 — 发布门禁放行 `qrBarcodeRef`（仍阻断 attachmentList）

**Given** 草稿版本绑定仅含合法 `qrBarcodeRef`（无 `attachmentListRef`、无其它既有 blocker）  
**When** 评估发布门禁 / 绑定校验中的 writer-unsupported 规则  
**Then** **不** 因 `qrBarcodeRef` 硬阻断  
**And** Given 另含 `attachmentListRef`，When 评估，Then **仍** 硬阻断 `attachmentListRef`

### BDD-CE-K06b-007 — 嵌套路径发射

**Given** `qrBarcodeRef` 位于 `conditionBlock`/`loopBlock` children、或 pinned `contentModuleRef` 结构、或 paragraph inline children，且条件/循环使节点可达，variables 有 payload  
**When** 渲染  
**Then** DOCX 含嵌入码图（与顶层同等成功语义）  
**And** 不得再走「嵌套静默 omit」或 `unsupportedNodeType`

### BDD-CE-K06b-008 — 金标 `09-qr-barcode` ACTIVE + fail-first

**Given** 本片交付完成  
**When** 检查 `golden-corpus/09-qr-barcode/manifest.json` 并运行 `mvn verify`  
**Then** `maturity` 为 `ACTIVE`  
**And** `expected/docx-assertions.json` 非 `deferred`，断言嵌入图片存在  
**And** 实现前故意缺少 writer 时对应测试/金标为 Red；最小 ZXing 嵌入实现后为 Green  
**And** **无** 像素/截图比对

### BDD-CE-K06b-009 — `CODE_128` 条码（同片可选但确认）

**Given** `"format":"CODE_128"`、合法 payload（ZXing CODE_128 可编码字符集）、合法 `sizePx`  
**When** 渲染  
**Then** DOCX 含嵌入条码 PNG  
**And** `errorCorrection` 若存在则被忽略且不报错  
**Note** 金标最小样本以 **QR_CODE** 为主；CODE_128 至少由单元测试覆盖。

---

## 17. BDD readiness — K06b（historical）

```
bdd_readiness: ready
k06_scope_slice: b
status: shipped on MAIN tip a689ca87 (do not redo in Wave 3)
residual_closed_by: K06c Wave 3 (§18–§20)
owning_doc: docs/behavior/ce-k06-rendering-fidelity.md
task_ids: [#62, CE-K06, K06b]
```

---

## 18. Wave 3 — K06c `attachmentListRef` writer + PDF stamp via render profile

### 18.1 Actor / Role（K06c）

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **系统（Rendering pipeline / DocxAssembler）** | 结构化引用节点 → DOCX 发射 | `attachmentListRef` → 编号列表段落；禁止静默省略 |
| **系统（PDF post-process）** | LibreOffice 转换后 `PdfConversionPostProcessor` / `PdfPageNumberStamper` | stamp 启用与否跟随 **发布锁定 renderProfile** |
| **Runtime / Preview 生成路径** | 已发布绑定 + variables + 锁定 profile | 附件清单可见；PDF stamp 与包配置一致 |
| **发布门禁 / 绑定校验** | LR-A4 writer-unsupported 集合 | `attachmentListRef` 退出 unsupported；集合可空 |
| **平台 / CI** | 金标 + `mvn verify` | 金标 `10-attachment-list` ACTIVE；stamp 优先级单测 |
| **（间接）函件读者** | 附件清单可读性；页码语义 | 编号列表顺序正确；stamp 不违背包配置 |

本片 **无** 新管理 UI 旅程、**无** 新公开 REST 契约（节点仍为既有 `attachmentListRef` + `referenceKey`；错误走既有渲染失败信封；profile 字段已存在）。

### 18.2 Goal（K06c）

1. 合法 `attachmentListRef` 在顶层与嵌套路径均可写出 DOCX **编号列表段落**（有序编号），映射运行时结构化附件清单；**禁止** 静默省略。
2. 从 `WriterUnsupportedStructuredNodeTypes` **移除** `attachmentListRef`；发布门禁与绑定校验对合法附件清单节点 **放行**。
3. PDF 页码 stamp：**发布锁定** `RenderProfile.pdfPageNumberStampingEnabled` 为该包的 **唯一权威开关**；全局 `docgen.rendering.pdf-page-number-stamping-enabled` **不得** 覆盖已锁定 profile 的 true/false。
4. 调用方 `CallerRenderOverride` 仍不可覆盖 stamp（延续 P18-T08）。
5. Fail-first 单测/装配测试驱动 writer + stamp 优先级；金标包 `golden-corpus/10-attachment-list` → **ACTIVE**。
6. **不**重做 K06a/K06b；**不**交付附件元数据管理 UI / CE-E02 资产目录。

### 18.3 已确认决策（K06c，2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **K06c-C1** | **本片交付范围 = K06c only。** 不重做 K06a（tblHeader）或 K06b（ZXing QR）；不改 CE-C04 / CE-U06；不 go-live；不激活 CD-3。Parallel U06 可能合并 — 合入前 rebase。 | 编排 handoff + CE-K06 卡 |
| **K06c-C2** | **节点：** `"type":"attachmentListRef"` + 非空 `referenceKey`（既有校验）。发射落在 structured DOCX writer 同源路径（顶层块 + 嵌套 `conditionBlock`/`loopBlock`/pinned module/inline 可达路径）。 | LR-A4 + P18-T05 + 卡 |
| **K06c-C3** | **Payload 来源：** 从渲染/预览 **`variables` map** 按节点 `referenceKey`（trim 后）取值 — 与 `qrBarcodeRef` / `loopBlock` 变量解析模式一致。**不**依赖 CE-E02 资产库 UI。 | K06b 模式 + referenceKey 既有模型 |
| **K06c-C4** | **DOCX 输出：** 将清单项按顺序写成 **Word 有序编号列表段落**（复用/对齐既有 `DocxListNumberingSupport` ordered 路径或等价 `numPr`）；每一清单项对应至少一个编号段落；**禁止** 像素断言。 | CE-K06 卡「编号列表段落」+ 既有 list writer |
| **K06c-C5** | **LR-A4 收缩：** 实现后权威 writer-unsupported set = **空集**（`{}`）。`WriterUnsupportedStructuredNodeTypes`、绑定 `UNSUPPORTED_NODE`、发布门禁专用项、一致性单测同源更新。既有 attachmentList「unsupported」渲染测试改为成功路径或显式业务失败回归。 | LR-A4-C4 + 卡 |
| **K06c-C6** | **Fail-closed messageKey（English-first，对齐 qrBarcode 模式）：** 缺失 key / 值为 `null` → `api.error.rendering.attachmentListPayloadMissing`；值为非数组或数组元素非 string（非法项）→ `api.error.rendering.attachmentListPayloadInvalid`。均 **非** `unsupportedNodeType`。**禁止** 静默 skip。 | LR-A4 精神；USER/PARENT 2026-07-15 |
| **K06c-C7** | **PDF stamp 优先级（锁定）：** 当本次生成解析到非 null 的 `RenderProfile`（含发布锁定快照与 preview 解析路径）时，`effectiveStampingEnabled = renderProfile.pdfPageNumberStampingEnabled` **仅此字段**。全局 `docgen.rendering.pdf-page-number-stamping-enabled` / `DocgenRenderingProperties.pdfPageNumberStampingEnabled` **不得** OR/AND 翻转该结果（profile=`false` 时全局=`true` 也不得 stamp；profile=`true` 时全局=`false` 仍须 stamp）。 | CE-K06 卡「按包配置而非全局布尔」+ stub「全局不得绕过已锁定 profile」+ 领域 §2.6.8/§2.6.11 |
| **K06c-C8** | **Profile 缺省：** `RenderProfile.fromJsonNode` 对缺失字段已 `asBoolean(false)`；默认资产 `rp-v1` 显式 `true`。本片 **不** 改双页码/节边界 stamp 算法（P22 已交付），只收敛 **启用开关** 权威。 | 代码 + P22 |
| **K06c-C9** | **无 profile 回落：** 仅当调用路径传入 `renderProfile == null`（无锁定快照的内部/测试旁路）时，才允许使用全局应用属性作为启用开关。生产预览/运行时生成必须走锁定或可解析 profile。 | 卡「非全局布尔」+ 运维旁路最小保留 |
| **K06c-C10** | **Caller override：** `CallerRenderOverride` / 请求体 **不可** 设置或覆盖 `pdfPageNumberStampingEnabled`（既有 P18 规则）。 | P18-T08 |
| **K06c-C11** | **Stamp 失败语义：** 当 profile 要求 stamp（`true`）且 stamper 失败 → 既有 fidelity warning 路径；**不得**在要求页码时静默返回无页码 PDF（P22 已确认，本片保持）。 | 领域 §2.6.11 |
| **K06c-C12** | **非目标：** 附件清单编排 UI；对象存储附件实体；改 QR/tblHeader；新权限码；像素视觉回归；ADR 级换库；go-live / CD-3。 | handoff |
| **K06c-C13** | **Payload 形态（K06c-Q1 = A）：** `variables[referenceKey]` **必须** 为 JSON/运行时 **`string[]`**；每一数组元素 = 对应编号列表段落的显示文本（按数组顺序写出）。**不**接受 `object[]` 或其它形态。 | USER/PARENT 2026-07-15 |
| **K06c-C14** | **空数组（K06c-Q2）：** `[]` → **成功**，写出 **零** 个编号列表段落（合法空清单）。`null` / 缺失 key / 非数组 → fail-closed（见 C6）。 | USER/PARENT 2026-07-15 |
| **K06c-C15** | **金标（K06c-Q3）：** 新建 `backend/src/test/resources/golden-corpus/10-attachment-list/`（同 K07 根与 harness；模式对齐 K06b `09-qr-barcode`）；`maturity: ACTIVE`；DOCX 断言编号列表存在性/顺序（禁像素）。 | USER/PARENT 2026-07-15 |

### 18.4 Preconditions

- K06a + K06b 已在 integration base（handoff base `a689ca87`）；本 worktree 基于该 tip。
- LR-A4 基础设施存在；当前 unsupported set = `{ attachmentListRef }`。
- `RenderProfile.pdfPageNumberStampingEnabled` 与 `PdfPageNumberStamper` / `PdfConversionPostProcessor` 已存在。
- CORE-FORTRESS F1 structured writer / `DocxAssembler` 路径可用；ordered list numbering 支持可用。
- 隔离 worktree 交付；不在 MAIN 实现。

### 18.5 Trigger

- Runtime / preview / test-generate 装配含 `attachmentListRef` 的结构化内容，且 variables 提供对应清单数据。
- 发布门禁评估含合法 `attachmentListRef`（无其它既有 blocker）的草稿版本。
- PDF 转换路径在已锁定 `renderProfile` 下启用/禁用 stamp。
- CI / 工程师执行 `mvn -B -ntp -f backend/pom.xml verify`。

### 18.6 Primary journey（K06c）

1. 作者在锚点结构化 JSON 插入 `"type":"attachmentListRef","referenceKey":"ATTACHMENTS"`。
2. 绑定校验：`referenceKey` 非空；**不再**因 writer-unsupported 阻断。
3. 发布门禁对仅含合法附件清单引用的版本放行（就 writer-unsupported 项而言）。
4. 生成/预览时 variables 含 `ATTACHMENTS` = `string[]`（如 `["Annex A","Annex B"]`）。
5. Writer 按序写出编号列表段落（每项一段）。
6. 若输出 PDF：`resolveOptions` / `isStampingEnabled(profile)` 仅服从锁定 profile 布尔。
7. 金标 `10-attachment-list` + 单测在 verify 中覆盖 writer + stamp 优先级。

### 18.7 System responses

| 情况 | 系统响应 |
| --- | --- |
| 合法 `attachmentListRef` + `string[]`（≥1 项） | 成功 DOCX；含对应数量编号列表段落；非 `unsupportedNodeType` |
| 合法 `attachmentListRef` + `[]` | 成功 DOCX；**零** 编号列表段落（合法空清单） |
| 缺失 key / `null` | 显式失败 `api.error.rendering.attachmentListPayloadMissing`；禁止静默省略 |
| 非数组 / 数组元素非 string | 显式失败 `api.error.rendering.attachmentListPayloadInvalid`；禁止静默省略 |
| 发布仅含合法 attachmentList | 不再因该类型硬阻断 |
| `renderProfile.pdfPageNumberStampingEnabled=true`（全局任意） | PDF stamp **启用** |
| `renderProfile.pdfPageNumberStampingEnabled=false`（全局任意） | PDF stamp **禁用**；全局 true 不得绕过 |
| `renderProfile == null` | 回落全局应用属性（仅旁路） |
| Caller 试图覆盖 stamp | 忽略（既有） |
| profile=`true` 且 stamp 失败 | fidelity warning；不静默无页码（既有） |

### 18.8 Boundary / exception（K06c）

| 边界 | 行为 |
| --- | --- |
| `referenceKey` 空白 | 既有 `MISSING_REFERENCE_KEY` 绑定 blocker；若绕过直达 writer → `attachmentListPayloadMissing` |
| 嵌套路径 | 与顶层相同发射/失败语义（延续 LR-A4 嵌套可达性） |
| 空清单数组 `[]` | **成功**，零编号段落（K06c-C14） |
| 非 `string[]`（含 `object[]`） | `attachmentListPayloadInvalid` |
| Authorization | 无新权限码；沿用既有生成/预览授权 fail-closed |
| 像素断言 | **禁止** |
| 全局默认 false | 不得阻止 profile=`true` 的包 stamp；亦不得在 profile=`false` 时强制 stamp |

### 18.9 Observable evidence

| 证据 | 说明 |
| --- | --- |
| DOCX package | 编号列表段落（`w:numPr` / ordered numId 或稳定等价）+ 清单项文本顺序与 `string[]` 一致 |
| Unsupported set | 一致性测试：空集（不再含 `attachmentListRef`） |
| 发布门禁 | 含 `attachmentListRef` 的版本不再因该类型硬阻断 |
| Stamp 单测 | profile true/false × 全局 true/false 四象限；仅 profile 决定 |
| 错误信封 | `attachmentListPayloadMissing` / `attachmentListPayloadInvalid` + `retryable=false` |
| 金标 | `golden-corpus/10-attachment-list` ACTIVE + XML（禁像素） |

### 18.10 Confirmed decisions — former open questions（USER/PARENT 2026-07-15）

> 原 blocking Q 已关闭。下列为 **confirmed** 产品锁，非候选。

| ID | Decision | Lock |
| --- | --- | --- |
| **K06c-Q1** | `variables[referenceKey]` 形态 = **`string[]`**；每项 = 编号列表段落显示文本 | **A**（confirmed）→ **K06c-C13** |
| **K06c-Q2** | 空数组 `[]` → **成功、零段落**；`null` / 缺失 key / 非数组 → fail-closed（C6 messageKeys） | confirmed → **K06c-C14** |
| **K06c-Q3** | 金标包 ID = **`10-attachment-list`** ACTIVE（同 K06b `09-qr-barcode` 模式） | confirmed → **K06c-C15** |

**Stamp scenarios unchanged:** `BDD-CE-K06c-004…007` 保持既有锁定，本修订不改写。

---

## 19. Acceptance scenarios — K06c（Given / When / Then）

### BDD-CE-K06c-001 — 合法 `string[]` 写出编号列表并退出 unsupported

**Given** 锚点结构化 JSON 含 `"type":"attachmentListRef","referenceKey":"ATTACHMENTS"`（合法非空 key）  
**And** 生成变量含 `ATTACHMENTS` = `string[]` 至少两项非空白段落文本（例：`["Annex A — KYC pack","Annex B — Fee schedule"]`）  
**And** 本片 writer 已实现  
**When** 渲染管线写出 DOCX  
**Then** 产物含 **两项** 有序编号列表段落，段落文本顺序与数组一致  
**And** `WriterUnsupportedStructuredNodeTypes` **不再**包含 `attachmentListRef`  
**And** 渲染 **不** 抛 `api.error.rendering.unsupportedNodeType`（因该节点）

### BDD-CE-K06c-002 — 空数组成功；缺失/null/非数组 fail-closed

**Given** 合法 `attachmentListRef` 且 variables 中对应 key = `[]`  
**When** 渲染  
**Then** 生成 **成功**  
**And** DOCX **不含** 因该节点写出的编号列表段落（零段落）  
**And** **不** 抛 `unsupportedNodeType` / `attachmentListPayloadMissing` / `attachmentListPayloadInvalid`

**Given** 合法 `attachmentListRef` 但 variables **无**对应 key，或值为 `null`  
**When** 渲染  
**Then** 显式失败且 `messageKey=api.error.rendering.attachmentListPayloadMissing`  
**And** **禁止** 静默省略该节点

**Given** 合法 `attachmentListRef` 但 variables 对应值为 **非数组**（含标量 / object / `object[]`），或为数组但其元素 **非 string**  
**When** 渲染  
**Then** 显式失败且 `messageKey=api.error.rendering.attachmentListPayloadInvalid`  
**And** **禁止** 静默省略；**禁止** 产出假装成功的无清单文档

### BDD-CE-K06c-003 — 发布门禁放行 `attachmentListRef` + 嵌套路径

**Given** 草稿版本绑定仅含合法 `attachmentListRef`（无其它既有 blocker）  
**When** 评估发布门禁 / 绑定校验中的 writer-unsupported 规则  
**Then** **不** 因 `attachmentListRef` 硬阻断  
**And** Given 节点位于 `conditionBlock`/`loopBlock` children、或 pinned `contentModuleRef`、或可达 inline/block 路径且条件使节点可达，When 渲染（合法 `string[]` payload），Then DOCX 含编号列表（与顶层同等成功语义）

### BDD-CE-K06c-004 — Profile `true` 启用 stamp（全局任意）

**Given** 发布锁定（或解析得到的）`RenderProfile.pdfPageNumberStampingEnabled=true`  
**And** 全局 `docgen.rendering.pdf-page-number-stamping-enabled` 为 `true` **或** `false`  
**When** PDF 转换后处理解析 stamp 选项  
**Then** stamp **启用**（`isStampingEnabled(profile)==true` / `resolveOptions` 非 disabled）  
**And** 调用方 override **无效**

### BDD-CE-K06c-005 — Profile `false` 禁用 stamp（全局不得绕过）

**Given** `RenderProfile.pdfPageNumberStampingEnabled=false`  
**And** 全局 `docgen.rendering.pdf-page-number-stamping-enabled=true`  
**When** PDF 转换后处理解析 stamp 选项  
**Then** stamp **禁用**  
**And** **不得**因全局 true 而 stamp（关闭当前 OR 回落漏洞）

### BDD-CE-K06c-006 — Profile `false` + 全局 `false` 仍禁用

**Given** profile=`false` 且全局=`false`  
**When** 解析 stamp 选项  
**Then** stamp **禁用**

### BDD-CE-K06c-007 — `renderProfile == null` 回落全局

**Given** 调用路径传入 `renderProfile == null`  
**And** 全局 stamp 属性为 `true`（或 `false`）  
**When** `isStampingEnabled(null)` / 等价旁路  
**Then** 结果等于全局应用属性  
**And** 生产预览/运行时主路径 **不得** 在可解析锁定 profile 时走此旁路

### BDD-CE-K06c-008 — 金标 `10-attachment-list` ACTIVE + fail-first

**Given** 本片交付完成  
**When** 检查 `golden-corpus/10-attachment-list/manifest.json` 并运行 `mvn verify`  
**Then** `maturity` 为 `ACTIVE`  
**And** input 含 `attachmentListRef` + variables 提供 `string[]` payload  
**And** `expected/docx-assertions.json` 非 `deferred`，断言编号列表存在性/顺序（`w:numPr` 或稳定等价；禁像素）  
**And** harness 在 `mvn verify` 中执行该包 DOCX 半段并通过  
**And** 实现前缺 writer 时对应测试为 Red；最小编号列表实现后为 Green

---

## 20. BDD readiness — K06c（this slice）

```
bdd_readiness: ready
k06_scope_this_slice: c
open_questions: []
confirmed_locks: [K06c-C1…C15, K06c-Q1=A string[], K06c-Q2 empty[] success, K06c-Q3=10-attachment-list, messageKeys attachmentListPayloadMissing|Invalid]
acceptance_scenarios: [BDD-CE-K06c-001…008]
owning_doc: docs/behavior/ce-k06-rendering-fidelity.md
task_ids: [#62, CE-K06, K06c]
next: plan-orchestrator → prefer rendering-engineer (TDD Red: unsupported removal + string[] numbered-list writer + stamp priority quadrants + golden 10-attachment-list)
```

**Next:** 交 `plan-orchestrator` → prefer **`rendering-engineer`**（TDD Red：unsupported 移除 + `string[]` 编号列表 writer + stamp 优先级四象限 + 金标 `10-attachment-list`）。
