# CE-K06 渲染保真补全 — BDD（K06a shipped · Wave 2 = K06b）

| Field | Value |
| --- | --- |
| **Slice (Wave 2)** | `ce-k06b-qr-barcode` |
| **Plan task** | **CE-K06**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §3 CE-K06） |
| **Task Master** | **#62**（sub-slice **K06b**） |
| **bdd_readiness** | **`ready`** for **K06b**（本片确认范围 = **K06b only**；K06a 已合并 `485a7f3e`；K06c 仍为 residual） |
| **Recorded** | 2026-07-15（K06a）；**2026-07-15 Wave 2 K06b** |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k06b-qr-barcode` · `feat/ce-k06b-qr-barcode` |
| **Scope of this slice** | **K06b only** — `qrBarcodeRef` DOCX writer：ZXing 生成 PNG 嵌入；`sizePx` / `errorCorrection`（及 `format`）可配；退出 LR-A4 writer-unsupported set（仅 `qrBarcodeRef`）；金标 `09-qr-barcode` ACTIVE。**不**重做 K06a；**不**实现 K06c（`attachmentListRef` + PDF stamp profile）；**不**改 CE-C04 / CE-U06；**不** go-live；**不**激活 CD-3 |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；产品 [PRD.md](../product/PRD.md)；需求 [requirements-plan.md](../requirements/requirements-plan.md)；领域 [domain-model.md](../domain/domain-model.md) §2.6.5；金标 [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md)；writer-unsupported 正交 [lrp-a4-fail-closed-unsupported-nodes.md](./lrp-a4-fail-closed-unsupported-nodes.md) |

---

## 1. 概述

CE-K06 关闭渲染保真三类缺口，可独立子片交付。

| 子片 | 状态 | 痛点摘要 |
| --- | --- | --- |
| **K06a** | **Shipped**（merge `485a7f3e`） | `repeatHeaderAcrossPages` → DOCX `<w:tblHeader/>`；金标 `02-cross-page-table` ACTIVE — 见 §2–§11 / `BDD-CE-K06a-*`（**本 Wave 不重做**） |
| **K06b** | **In scope / ready**（本 Wave 2） | `qrBarcodeRef` 在节点矩阵与 LR-A4 writer-unsupported set；无 ZXing 图片嵌入 writer |
| **K06c** | **Deferred residual** | `attachmentListRef` 无编号列表段落 writer；PDF 页码 stamp 与全局 / `renderProfile` 收敛未完结 — 见 §12.2 |

**现状证据（K06b）：**

- `StructuredContentNodeType.QR_BARCODE_REF` / schema 已声明；`ReferenceNodeService` 校验非空 `referenceKey`。
- `WriterUnsupportedStructuredNodeTypes` = `{ qrBarcodeRef, attachmentListRef }`；发布硬阻断 + 渲染 `api.error.rendering.unsupportedNodeType`（LR-A4）。
- `StructuredContentDocxWriter` / inline / nested 路径 **无** QR 发射分支；`pom.xml` **无** ZXing 依赖。
- 金标目录尚无 QR 主题包（K07 八包骨架；K06a 已充实 `02-cross-page-table`）。

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

## 12. Residual — K06c only（K06b stubs superseded）

> **K06b：** 原 §12.1 stub 已提升为完整规格 → **§15–§22**（`BDD-CE-K06b-001…008`）。  
> **K06c：** 仍 deferred；**不**作为 K06b Done 门槛；保持 `attachmentListRef` 在 LR-A4 writer-unsupported set，直至 K06c 落地。

### 12.2 K06c — `attachmentListRef` + PDF stamp via render profile（deferred residual）

| Field | Stub |
| --- | --- |
| **Actor** | Rendering pipeline + PDF post-process |
| **Goal** | `attachmentListRef` → 结构化附件清单的编号列表段落；PDF 页码 stamp **按发布锁定 `renderProfile`（包级）** 控制，而非仅依赖全局应用布尔 |
| **Then (stub)** | 附件列表可见编号段落；`pdfPageNumberStampingEnabled`（或等价 profile 字段）驱动 stamp；全局默认不得绕过已锁定 profile |
| **Out of K06b** | 完整 stamp 策略重构、附件元数据 UI、从 unsupported set 移除 `attachmentListRef` |

**Stub scenarios:**

- **BDD-CE-K06c-001 (deferred):** Given 合法 `attachmentListRef` + 附件清单数据，When 渲染，Then DOCX 含编号列表段落；节点退出 writer-unsupported set。
- **BDD-CE-K06c-002 (deferred):** Given 包级 `renderProfile.pdfPageNumberStampingEnabled=true|false`，When PDF 转换，Then stamp 启用与否跟随 **profile**（调用方不可覆盖）；与全局 `pdf-page-number-stamping-enabled` 的优先级在实现片锁定并写 ADR/规格。

---

## 13. Traceability（CE-K06 family）

| Source | Link |
| --- | --- |
| Plan card | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) § CE-K06 |
| Task Master | `#62` CE-K06 · sub-slice K06b |
| PRD | [PRD.md](../product/PRD.md) — 二维码/条码引用；K06a 跨页表头；K06b writer |
| Requirements | [requirements-plan.md](../requirements/requirements-plan.md) |
| Domain | [domain-model.md](../domain/domain-model.md) §2.6.4（K06a）、§2.6.5（K06b） |
| Golden corpus | [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md) — K06b 追加 `09-qr-barcode` |
| Writer-unsupported | [lrp-a4-fail-closed-unsupported-nodes.md](./lrp-a4-fail-closed-unsupported-nodes.md) — K06b 后 set 收缩为 `{ attachmentListRef }` |
| K06a merge | `485a7f3e` |

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

## 17. BDD readiness — K06b（this slice）

```
bdd_readiness: ready
k06_scope_this_slice: b
residual: K06c (attachmentListRef writer + PDF stamp via render profile)
open_questions: []
owning_doc: docs/behavior/ce-k06-rendering-fidelity.md
task_ids: [#62, CE-K06, K06b]
next: plan-orchestrator → rendering-engineer (TDD Red: unsupported removal + ZXing embed + golden 09-qr-barcode)
```

**Next:** `plan-orchestrator` → TDD tasks for **K06b only** → prefer **`rendering-engineer`**.
