# CE-K02 母版样式权威 — BDD

| Field | Value |
| --- | --- |
| **Slice** | `ce-k02-master-style-authority` |
| **Plan task** | **CE-K02**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) § CE-K02） |
| **Task Master** | **#58** |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-15 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k02-master-style-authority` · `feat/ce-k02-master-style-authority` |
| **Scope of this slice** | 母版上传解析 `styles.xml`（+ theme 字体）→ 持久化 per-master（per-revision）style catalog；结构化→DOCX 按 `styleRef` / 母版样式 / `docDefaults` 继承链落地字体字号；移除渲染支持类中的 Calibri/10pt（及同路径硬编码字色）硬编码；金标 `dual-font-master` 充实为 ACTIVE。**不**重写 demo builder；**不** go-live；**不**激活 CD-3 |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；ADR-0019 [0019-structured-authoring-and-rendering-boundary.md](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md)；PRD §6.4.3；requirements 结构化写入不得退化为单一 Calibri 10pt；上游 K01 [ce-k01-release-bundle-pinning.md](./ce-k01-release-bundle-pinning.md)；K07 [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md) |

---

## 1. 概述

本切片让**母版 DOCX 的 styles.xml（含 theme 字体与 docDefaults）**成为结构化渲染的**唯一字体/字号/间距权威**。设计师在母版中设定的宋体/仿宋等中文字体必须出现在生成产物中；渲染路径不得再把 Calibri/10pt（及同路径硬编码字色）写进 run / styles 覆盖母版。

**现状证据（R2 / 计划卡）：**

- `MasterStyleCatalogService.loadForMaster(masterId)` **忽略** `masterId`，恒返回 classpath `authoring/default-master-style-catalog-v1.json`（仅 styleKey / applicableNodeTypes / renderPurpose，**无**真实 typography）。
- `StructuredContentDocxStyleSupport.applyDefaultRunStyle` 硬编码 `Calibri` / `10` / `#000000`。
- `DocxWordCompatibilitySupport`：`DEFAULT_FONT="Calibri"`、`DOC_DEFAULT_FONT_HALF_POINTS=22`，并对已有 styles 包 `setDefaultFonts(buildCalibriFonts())`。
- `DocxMasterStyleRegistry.ensureCatalogStyles` 注册缺失样式时字体写死 `"Calibri"`，字号按 styleKey 启发式。
- `DocxPlainAnchorParagraphSupport` 明文锚点路径同样硬编码 Calibri/10pt/黑色。
- `DocxAssembler` 构造时 `DocxMasterStyleCatalogSupport.loadDefault`，装配不消费 per-master catalog。
- 金标包 `01-dual-font-master` 仍为 **PLACEHOLDER**（K07 骨架），充实责任在本片。

**改动面（计划卡）：** `master`（上传/revision 时解析并持久化 catalog）、`authoring`（`loadForMaster` 返回该 master/revision 的 catalog）、`rendering`（style 继承链；去掉硬编码；`MASTER_STYLE_FALLBACK` warning）、金标 `dual-font-master` → ACTIVE。Demo builder（`DemoRetailLetterheadDocxBuilder` / `DemoMasterDocxStyleSupport` 等）**本片禁止改写**。

---

## 2. Actor / Role

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **Master Designer / Master 管理员** | 上传/替换含中文字体 styles 的母版 DOCX | 上传后 catalog 反映母版真实样式 |
| **Template Author** | 结构化绑定写 `styleRef` | 引用母版样式 ID；校验用 per-master catalog |
| **Runtime 生成路径（sync / async / batch）** | 结构化装配写入 DOCX | 产物 run/styles 无 Calibri 硬编码覆盖 |
| **Fidelity / 发布闸门** | 保真警告与 blocker | `MASTER_STYLE_FALLBACK` 仅在母版无 docDefaults 时出现 |
| **平台 / 渲染工程师** | 金标语料 | `dual-font-master` ACTIVE + verify 绿灯 |
| **（间接）法务 / 银行信函读者** | 中文信函字体正确 | 可观察：DOCX 内字体非 Calibri |

---

## 3. Goal

1. **上传时解析权威：** 母版 DOCX 上传（含新建 revision）时解析 `word/styles.xml`（及 theme 字体源），持久化 **per-master-revision** style catalog：段落样式、字符样式、theme 字体、docDefaults（默认字体/字号等）。
2. **`loadForMaster` 真实化：** `MasterStyleCatalogService.loadForMaster(masterId)`（及装配路径等价入口）返回该 master **当前 revision**（或调用方指定的 revision）的持久化 catalog，**不再**忽略 `masterId` 返回固定 classpath 目录。
3. **渲染继承链固定：** 结构化内容写入 DOCX 时：
   - 有 `styleRef` 且 catalog 命中 → 应用母版样式 ID（段落 `w:pStyle` / 字符样式按 OOXML 语义），typography 来自 catalog/母版包，**不**在 run 上硬编码字体。
   - 无 `styleRef`（或未指定字体相关属性）→ 继承母版 **docDefaults**（及段落 Normal 等母版默认），**不**写入硬编码 Calibri/10pt。
   - 仅当母版 **完全无 docDefaults** 时，才允许 **系统兜底**（见 K02-C7）并产生 fidelity warning **`MASTER_STYLE_FALLBACK`**。
4. **零硬编码（渲染支持类）：** `StructuredContentDocxStyleSupport`、`DocxWordCompatibilitySupport`、`DocxMasterStyleRegistry`、`DocxPlainAnchorParagraphSupport` 及同路径结构化 inline 写入 **不得**在母版已提供 docDefaults/样式时写入 Calibri/10pt/硬编码字色覆盖。Demo 资产生成器硬编码 **本片不改**。
5. **金标：** 充实 `backend/src/test/resources/golden-corpus/01-dual-font-master` 为 **ACTIVE**：母版含宋体/仿宋（或等价双字体）→ 生成产物断言无 Calibri（CJK/指定段落路径）。
6. **与 K01 协同：** 已发布 release 使用钉扎 master revision 的 catalog（catalog 随 revision 持久化；钉扎 revision 即钉扎样式权威）。本片**不**重开 K01 钉扎字段语义。
7. **禁止：** 重写 demo builder；go-live；激活 CD-3；本片不做 K03/K04/K05 行为。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **K02-C1** | 样式权威 = 母版 revision 的 OOXML：`word/styles.xml` 的 style 定义 + `docDefaults`；theme 字体（`word/theme/theme*.xml` 的 major/minor latin/ea/cs，若存在）并入 catalog 的 theme 段。Classpath `default-master-style-catalog-v1.json` **不再**作为运行时 typography 来源；仅可作为「平台已知 styleKey 的 applicableNodeTypes / renderPurpose」元数据合并源（见 K02-C5）。 | 计划卡 CE-K02；ADR-0019；CE 目标能力「样式单一权威」 |
| **K02-C2** | **解析触发：** 每次母版 DOCX 上传创建/替换 `master_revision_line` 时同步解析并**持久化**该 revision 的 catalog。持久化必须跨进程重启可读（DB 或等价 durable store，挂在 revision 上）。具体列名/表结构由实现选定，BDD 只要求可观察：上传后 `loadForMaster` / 管理端 style catalog API 返回该母版解析结果。 | 计划卡「母版上传时解析…持久化」 |
| **K02-C3** | **存量 hydrate：** 若某 revision 已有 DOCX 对象但尚无持久化 catalog（K02 前上传），则首次 `loadForMaster` 或首次需要 catalog 的装配路径必须从对象存储 **惰性解析并持久化**（幂等），不得静默永远回退 classpath 默认目录。 | 计划卡目标 + 存量母版可用性（不强制全员重传） |
| **K02-C4** | Catalog 至少包含：`catalogVersion`（稳定版本戳或内容指纹，可观察）、`docDefaults`（ascii/hAnsi/eastAsia/cs 字体名、默认字号 half-points，若 styles.xml 有则非空）、`themeFonts`（若 theme 存在）、`styles[]`：每项含 `styleKey`（OOXML styleId）、`styleType` ∈ {`PARAGRAPH`,`CHARACTER`}（及平台已支持的其他 OOXML 类型若解析到则保留或忽略按实现，但段落/字符必须支持）、typography（字体族分槽、字号、粗体/斜体等 styles.xml 已声明的 run 属性）。 | 计划卡；PRD §6.4.3 扩展 typography |
| **K02-C5** | **applicableNodeTypes / renderPurpose：** styleKey 与平台默认 catalog 交集的条目，保留平台默认的 applicableNodeTypes / renderPurpose；母版独有 styleKey：按 `styleType` 赋予基线适用性——`PARAGRAPH` → `paragraph` / `sectionHeading` / `list`；`CHARACTER` → `textRun` / `emphasis` / `underline`。缺失/不适用 styleRef 仍为 **发布 blocker**（既有 `MISSING_STYLE_REFERENCE` / `INAPPLICABLE_STYLE`）。 | PRD §6.4.3 + P18 既有校验 + 计划「styleRef → 母版样式 ID」 |
| **K02-C6** | **渲染继承链（强制顺序）：** (1) 节点 `styleRef` 命中 catalog → 使用该母版样式；(2) 否则母版样式体系中的默认段落样式（若有 Normal 等）；(3) 否则母版 `docDefaults`；(4) **仅当** (3) 亦不存在（母版完全无 docDefaults）→ 系统兜底 + **`MASTER_STYLE_FALLBACK`**。禁止跳过 (1)–(3) 直接写 Calibri。 | 计划卡兜底链原文 |
| **K02-C7** | **系统兜底取值：** 仅走 K02-C6 第 (4) 步时，允许使用平台基线 `Calibri` / `10pt` / `#000000`（与今日硬编码常量一致），且**必须**发出 `FidelityWarningCode.MASTER_STYLE_FALLBACK`（新增枚举值；英文 messageKey，i18n English-first）。母版有 docDefaults 时**禁止**发出该 warning，也**禁止**写入该基线覆盖。 | 计划卡；既有 `CONTROLLED_STYLE_FALLBACK` **保留原语义、不合并** |
| **K02-C8** | **装配到母版包时：** `assemble` / `assembleStructured` 打开母版 XWPFDocument 后，母版包内已有 styles **优先保留**；`DocxWordCompatibilitySupport` **不得**用 Calibri 覆盖已有 `docDefaults` / default fonts；仅在 styles 部件缺失或空包时补最小 Word 兼容结构，且若补的是系统基线则生成路径须满足 K02-C6/C7。结构化写入：设置 `styleRef` 对应 styleId 后，**省略**对 run 的 `setFontFamily`/`setFontSize`/`setColor`，除非来自白名单 `directFormat` 或字符样式显式要求。 | 现状 `DocxAssembler` + 计划「不再写入硬编码字体」 |
| **K02-C9** | **`DocxMasterStyleRegistry`：** 向空/缺样式的包注册 catalog 条目时，字体/字号必须来自 **per-master catalog typography**，不得写死 Calibri 或 styleKey 启发式字号（旧 `resolveHeadingSize` / `"Calibri"` 路径删除或仅用于无 catalog 的 fail 路径并触发 K02-C7）。 | 计划卡；现状 `DocxMasterStyleRegistry` |
| **K02-C10** | **明文锚点路径**（`DocxPlainAnchorParagraphSupport`）同样遵守 K02-C6/C8：不得硬编码 Calibri/10pt/黑色覆盖母版默认。 | 计划痛点「≥6 处」；同一切片一致性 |
| **K02-C11** | **`DocxAssembler` / writer** 必须注入或解析 **当前装配所用 master revision** 的 catalog，不得在组件字段上钉死 `loadDefault`。Preview（dev 母版）用 current revision catalog；已发布生成用 K01 钉扎 revision 的 catalog。 | 计划依赖 K01；K01 BDD 钉扎语义 |
| **K02-C12** | **解析失败 fail-closed：** 上传时 `styles.xml` 缺失或无法解析 → 拒绝上传（错误码 `api.error.master.styleCatalogParseFailed`），与锚点提取失败同级；**不**写入半残 revision。惰性 hydrate 解析失败 → fail-closed，禁止回退 classpath typography 假装成功。 | fail-closed 默认；对齐 `MasterDocxUploadSupport` 锚点失败模式 |
| **K02-C13** | **金标：** `01-dual-font-master` 由 PLACEHOLDER → **ACTIVE**；input 母版 styles 含至少两种非 Calibri 字体（计划示例：宋体 + 仿宋）；expected DOCX 断言：目标段落/run 的 `w:rFonts`（或继承链解析结果）**不包含** `Calibri`；PDF 半段按 K07 规则（无 soffice 可 skip）。Harness 复用 K07，不新建第二套 golden 根。 | 计划卡测试栏；K07-C2/C5/C11 |
| **K02-C14** | **本片禁止：** 重写 demo builder / demo 资产硬编码清理；改锚点提取逻辑；改 K01 钉扎字段；实现 K03 计算引擎；像素级视觉比对；go-live / CD-3。 | 计划卡「禁止」栏 |
| **K02-C15** | 管理端既有「获取 master style catalog」API（若已有）必须返回 per-master 解析目录（含 typography 可观察字段或至少 styleKey 集合与母版一致）；若 API 仅暴露 styleKey 列表，则后端单测/金标仍须断言 typography 持久化与生成产物字体。不强制本片新做前端 UI。 | 现状 `TemplateStructuredAuthoringService.getMasterStyleCatalog`；本片以后端行为为主 |

---

## 5. 前置条件

- CE-K01 Done：已发布生成读钉扎 master revision（本片 catalog 挂在 revision 上即可被钉扎间接锁定）。
- CE-K07 骨架 Done：`golden-corpus/` 与 harness 已接入 `mvn verify`；`01-dual-font-master` 为 PLACEHOLDER 可充实。
- P18 styleRef 校验与 `FidelityWarningCode` 基础设施存在。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- Master Designer 上传或替换母版 DOCX（创建新 `master_revision_line`）。
- `loadForMaster(masterId)` / 管理端拉取 style catalog / 结构化保真校验。
- Runtime sync/async/batch 或 preview 结构化装配生成 DOCX。
- `mvn verify` 执行 golden-corpus（含 ACTIVE `dual-font-master`）。

---

## 7. Primary journey（上传 → 生成无 Calibri）

1. Master Designer 上传含 `styles.xml` 声明宋体/仿宋（及 docDefaults）的母版 DOCX。
2. 后端在同一事务路径解析 styles（+ theme）→ 持久化到该 revision 的 style catalog。
3. Template Author 绑定段落 `styleRef` 指向母版样式 ID（如 `ClauseBody`）；保真校验使用该 master 的 catalog。
4. Runtime 对该模板生成 DOCX（装配打开母版包 + 结构化写入锚点）。
5. 产物中目标 run/段落 **不**出现硬编码 Calibri；字体来自母版样式或 docDefaults（宋体/仿宋等）。
6. 金标 `dual-font-master` ACTIVE 包在 `mvn verify` 中断言上述行为。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| 上传且 styles 可解析 | revision 持久化 catalog；上传成功 |
| 上传但 styles.xml 缺失/损坏 | **fail-closed** `api.error.master.styleCatalogParseFailed`；不创建半残 revision |
| `loadForMaster` 有持久化 catalog | 返回该 catalog（含 typography） |
| `loadForMaster` 无 catalog 但有 DOCX | 惰性解析并持久化后返回；失败则 fail-closed |
| 结构化写入有 styleRef 且命中 | 应用母版 styleId；不硬编码字体 |
| 无 styleRef，母版有 docDefaults | 继承 docDefaults；**无** `MASTER_STYLE_FALLBACK`；无 Calibri 硬编码 |
| 母版完全无 docDefaults | 系统兜底 Calibri/10pt/#000000 + **`MASTER_STYLE_FALLBACK`** warning |
| styleRef 不在 catalog | 既有 **blocker** `MISSING_STYLE_REFERENCE`（发布闸门） |
| styleRef 不适用节点类型 | 既有 **blocker** `INAPPLICABLE_STYLE` |
| 金标 dual-font-master | ACTIVE；断言失败 → verify 红 |

---

## 9. 验收场景（Given / When / Then）

### A. 上传解析与持久化

#### BDD-CE-K02-001 — 上传解析 styles.xml 并持久化 per-revision catalog

**Given** 一个合法母版 DOCX，其 `word/styles.xml` 含段落样式 `ClauseBody`（eastAsia 或 ascii 字体为「宋体」）及非空 `docDefaults`  
**When** Master Designer 上传该 DOCX 创建 master revision  
**Then** 该 `master_revision_line` 关联的 style catalog 已持久化  
**And** catalog 中存在 `styleKey=ClauseBody` 且 typography 反映「宋体」（或 styles.xml 中的等价字体名）  
**And** catalog 的 `docDefaults` 非空

#### BDD-CE-K02-002 — loadForMaster 不再忽略 masterId

**Given** master A 的 catalog 含「宋体」样式，master B 的 catalog 含「仿宋」样式（两份不同母版）  
**When** 分别调用 `loadForMaster(A)` 与 `loadForMaster(B)`  
**Then** 返回的 catalog **不相同**  
**And** 均**不是**仅含 classpath 默认、无 typography 的固定目录冒充

#### BDD-CE-K02-003 — 替换母版新 revision 得到新 catalog

**Given** master 当前 revision R1 catalog 字体为「宋体」  
**When** 上传替换文件创建 R2，styles 中对应样式改为「仿宋」  
**Then** R2 catalog 反映「仿宋」  
**And** R1 catalog 仍为「宋体」（历史 revision 不随 current 被覆盖）

#### BDD-CE-K02-004 — styles.xml 无法解析 → 上传 fail-closed

**Given** 一个 ZIP/OPC 看似 docx 但 `word/styles.xml` 缺失或损坏的上传包  
**When** 调用母版上传  
**Then** 返回 `api.error.master.styleCatalogParseFailed`  
**And** 不持久化新的 master revision 行（或事务回滚，无半残 catalog）

#### BDD-CE-K02-005 — 存量 revision 惰性 hydrate

**Given** K02 前已存在的 revision R_old 有 storage DOCX、无持久化 catalog  
**When** 首次 `loadForMaster`（或首次装配需要 catalog）  
**Then** 系统从对象存储解析 styles 并持久化  
**And** 返回的 catalog 与直接解析该 DOCX 一致  
**And** 第二次调用不重复破坏性覆盖（幂等）

---

### B. Theme 与 docDefaults

#### BDD-CE-K02-006 — theme 字体进入 catalog

**Given** 母版含 `word/theme/theme1.xml` 且 minor East Asian 字体为「宋体」  
**When** 上传完成  
**Then** 持久化 catalog 的 `themeFonts`（或等价结构）包含该 East Asian 字体名  
**And** 后续无显式 rFonts 的继承路径可解析到该 theme 槽位（实现可在装配时解析，BDD 要求 catalog 可观察且生成不回退 Calibri）

#### BDD-CE-K02-007 — 有 docDefaults 时不发出 MASTER_STYLE_FALLBACK

**Given** 母版 styles.xml 含 `docDefaults`（例如默认 eastAsia=宋体、字号非空）  
**And** 结构化绑定某段落**无** `styleRef`  
**When** 生成 DOCX  
**Then** 产物不因系统兜底写入 Calibri  
**And** fidelity warnings **不包含** `MASTER_STYLE_FALLBACK`

---

### C. 渲染继承链与去硬编码

#### BDD-CE-K02-008 — styleRef 落地母版样式且无 Calibri 硬编码

**Given** 母版 catalog 含 `ClauseBody`（字体仿宋）  
**And** 锚点结构化 JSON 段落 `styleRef=ClauseBody`，子节点为普通 textRun（无 directFormat 字体）  
**When** `assembleStructured`（或等价最终生成路径）生成 DOCX  
**Then** 段落使用 Word styleId 关联 `ClauseBody`（或母版映射后的同一 styleId）  
**And** 该段落可见 run 的显式 `w:rFonts` **不**被写成 `Calibri`；若 run 省略 rFonts，则继承链解析结果为仿宋而非 Calibri

#### BDD-CE-K02-009 — 无 styleRef 时继承母版 docDefaults

**Given** 母版 docDefaults 默认字体为宋体、字号为母版声明值  
**And** 结构化段落无 `styleRef`、无字体类 directFormat  
**When** 生成 DOCX  
**Then** 写入路径**不**调用硬编码 `setFontFamily("Calibri")` / `setFontSize(10)`  
**And** 产物继承母版 docDefaults（断言无 Calibri 覆盖）

#### BDD-CE-K02-010 — 无 docDefaults 时系统兜底 + MASTER_STYLE_FALLBACK

**Given** 母版 styles 可解析但**完全没有** `docDefaults`  
**And** 节点无可用 styleRef typography  
**When** 生成 DOCX  
**Then** 允许使用系统基线 Calibri/10pt/#000000  
**And** fidelity warnings **包含** `MASTER_STYLE_FALLBACK`  
**And** 英文 messageKey 已注册（i18n English-first）

#### BDD-CE-K02-011 — DocxWordCompatibilitySupport 不覆盖母版默认字体

**Given** 装配打开的母版包已含非 Calibri 的 docDefaults / default fonts  
**When** 调用 `ensureWordCompatiblePackage`（装配末尾既有调用）  
**Then** 母版 docDefaults / default fonts **不被**改写成 Calibri  
**And** 仍满足 Word 可打开的最小兼容性（不引入空白页回归）

#### BDD-CE-K02-012 — DocxMasterStyleRegistry 注册字体来自 catalog

**Given** per-master catalog 中 `Heading1` typography 字体为宋体、字号为 catalog 值  
**When** 需要向 styles 部件注册缺失的 `Heading1`  
**Then** 注册的 `w:rFonts` / `w:sz` 来自 catalog  
**And** **不**使用硬编码 `"Calibri"` 或旧启发式 half-points 表冒充母版

#### BDD-CE-K02-013 — 明文锚点路径无 Calibri 硬编码

**Given** 母版有 docDefaults 宋体  
**When** 明文锚点替换路径写入段落 run（非结构化 writer）  
**Then** 不硬编码 Calibri/10pt/黑色覆盖母版默认

#### BDD-CE-K02-014 — 白名单 directFormat 仍可显式设字体

**Given** 节点带白名单内 `directFormat` 字体/字号（P18 既有）  
**When** 生成 DOCX  
**Then** run 可携带该显式字体（受白名单约束）  
**And** 不因此重新引入全局 Calibri 默认覆盖其他节点

---

### D. 校验与发布闸门（既有语义 + per-master）

#### BDD-CE-K02-015 — styleRef 不在该母版 catalog → blocker

**Given** master catalog 无 `UnknownStyle`  
**And** 结构化内容引用 `styleRef=UnknownStyle`  
**When** 保真/绑定校验 `MasterStyleCatalogService.validate`  
**Then** blocker `MISSING_STYLE_REFERENCE`（既有码）  
**And** 发布闸门 fail-closed（既有 PublishGate 行为）

#### BDD-CE-K02-016 — 母版独有样式可作为合法 styleRef

**Given** 母版 styles.xml 含平台默认 JSON 中不存在的段落样式 `BankLetterBody`  
**And** 上传后 catalog 含 `BankLetterBody`（PARAGRAPH）  
**When** 绑定 `styleRef=BankLetterBody` 于 paragraph 节点并校验  
**Then** **不**报 `MISSING_STYLE_REFERENCE`  
**And** 生成时应用该样式且无 Calibri 硬编码覆盖

---

### E. K01 钉扎协同

#### BDD-CE-K02-017 — 已发布生成使用钉扎 revision 的 catalog

**Given** release 钉扎 master revision R1（宋体 catalog），此后 current 变为 R2（仿宋 catalog）  
**When** 对该 PUBLISHED release 生成  
**Then** 装配使用 R1 的 DOCX **与** R1 的 style catalog  
**And** 产物字体权威为 R1（宋体），不跟随 R2

---

### F. 金标语料

#### BDD-CE-K02-018 — dual-font-master 升为 ACTIVE 且断言无 Calibri

**Given** `golden-corpus/01-dual-font-master` 已按 K07 包约定充实（母版双字体：宋体+仿宋或等价）  
**When** `mvn -B -ntp -f backend/pom.xml verify` 执行 golden harness  
**Then** 该包 `maturity=ACTIVE` 被执行  
**And** DOCX 断言：目标 CJK/双字体段落路径 **不包含** Calibri  
**And** PDF 半段按 K07（无 soffice 可 assume skip）；DOCX 半段必须执行

---

## 10. 边界与例外

| 边界 | 行为 |
| --- | --- |
| Demo builder 内 Calibri | **本片不改**；不作为本片 Done 阻塞，但渲染支持类硬编码必须清除 |
| 仅 theme、无 styles docDefaults | 继承链用 theme + 样式定义；若仍无 docDefaults 且无 style typography → K02-C6 (4) + warning |
| 字符样式 styleRef | 按 OOXML 字符样式应用到 run；不适用节点类型 → `INAPPLICABLE_STYLE` |
| Preview vs Published | Preview 用 current revision catalog；Published 用钉扎 revision catalog |
| `CONTROLLED_STYLE_FALLBACK` | 保留既有语义；与 `MASTER_STYLE_FALLBACK` 并存、不互相替代 |
| 东西方字体槽位 | ascii/hAnsi/eastAsia/cs 按母版/theme 分槽写入或继承；禁止四槽全部写死 Calibri（除非 K02-C7） |
| 本片无前端 E2E 强制 | 无新管理 UI 旅程；若仅改 API 投影字段，以后端门禁+金标为准 |

---

## 11. 可观察证据

| 证据 | 说明 |
| --- | --- |
| DB / durable catalog | 上传后 revision 可读取持久化 catalog（测试直接读仓储或 API） |
| `loadForMaster` 单测 | 两 master 不同 catalog；忽略 masterId 的旧行为被回归锁定 |
| 生成 DOCX OOXML | 断言 `document.xml` / styles 路径无 Calibri 覆盖；styleId 存在 |
| Fidelity warnings | 有/无 `MASTER_STYLE_FALLBACK` 场景对照 |
| Golden corpus | `01-dual-font-master` ACTIVE + verify |
| 门禁 | `mvn -B -ntp -f backend/pom.xml verify` GREEN（本片以后端为主；前端门禁若无 FE 变更可 N/A） |

---

## 12. 追溯

| 类型 | 引用 |
| --- | --- |
| Plan | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) § CE-K02、§1 目标#2、追溯矩阵 R2 |
| Task Master | **#58** CE-K02 |
| ADR | [ADR-0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) 母版样式目录与发布锁定 |
| PRD | §6.4.3 结构化片段与母版样式映射 |
| Requirements | 结构化写入不得退化为单一 Calibri 10pt；`styleRef` 须解析母版样式目录 |
| Upstream | K01 钉扎 revision；K07 `dual-font-master` PLACEHOLDER |
| Code anchors | `MasterStyleCatalogService`、`StructuredContentDocxStyleSupport`、`DocxWordCompatibilitySupport`、`DocxMasterStyleRegistry`、`DocxPlainAnchorParagraphSupport`、`DocxAssembler` |

---

## 13. Out of scope（本片明确不做）

- Demo builder / `deploy/demo-*` 资产生成器重写与 Calibri 清理（计划禁止；可另开小片）。
- K03 变量计算、K04 语义 diff、K05 impact analysis。
- 生产 go-live、CD-3、正式 P-phase 激活。
- 像素/截图视觉 golden。
- 新前端样式目录编辑 UI（除非实现发现 API 契约缺口需最小投影字段——仍以后端为准）。

---

## 14. Open questions

**无阻塞开放问题。** 下列项已在 §4 收口为确认决策，实现阶段不得再扩大范围：

- 系统兜底具体取值 → **K02-C7**（仅无 docDefaults 时 Calibri/10pt/#000000 + `MASTER_STYLE_FALLBACK`）。
- applicableNodeTypes 对母版独有样式 → **K02-C5**。
- 存量无 catalog → **K02-C3** 惰性 hydrate。
- 持久化物理模型（列 vs 表）→ 实现可选，BDD 只要求 durable + 可观察。

---

## 15. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-k02-master-style-authority.md
task_ids: [#58, CE-K02]
scenario_ids:
  - BDD-CE-K02-001 … BDD-CE-K02-018
next: plan-orchestrator（分解实现任务；TDD Red 自本文件场景）
```
