# CE-K07 金标语料回归体系 — 骨架先行（BDD）

| Field | Value |
| --- | --- |
| **Slice** | `ce-k07-golden-corpus-skeleton` |
| **Plan task** | **CE-K07**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) § CE-K07） |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-14 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k07-golden-corpus-skeleton` · `feat/ce-k07-golden-corpus-skeleton` |
| **Scope of this slice** | 语料目录骨架 + 包约定 + `mvn verify` 接入 + **≥2** 个可绿最小样本；**不**实现 K01–K06 / G02 业务能力 |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；加密契约交叉引用 [permission-matrix.md](../security/permission-matrix.md) / [requirements-plan.md](../requirements/requirements-plan.md) |

---

## 1. 概述

本切片建立 **后端金标语料回归护栏（golden corpus）** 的可扩展骨架，使后续 CE-K01–K06、CE-G02、CE-O01 等保真/内控变更在交付时 **必须** 向语料库追加或充实对应样本，并由 `mvn verify` 自动执行。

| 交付物 | 本片目标 |
| --- | --- |
| **语料根目录** | `backend/src/test/resources/golden-corpus/` |
| **基准包数量** | **≥8** 个命名包骨架（覆盖计划所列 8 个主题） |
| **包内容** | 每个包含 **input**（母版 DOCX + 模板/绑定 JSON + 变量 JSON）与 **expected**（DOCX XML 关键路径断言 + PDF 文本抽取断言） |
| **断言风格** | **不做** 像素 / 截图 / 视觉 golden 比对 |
| **门禁** | 语料套件接入 `mvn -B -ntp -f backend/pom.xml verify` |
| **成熟度** | 本片 = **骨架 + 最小可绿样本**；K01–K06（及 G02 等）后续片 **充实** PLACEHOLDER 包，而非另起目录体系 |

本规格描述的是 **测试/工程护栏行为**（对平台工程师与 CI 可观察），不是新的管理 UI 或公开 API 产品旅程。仍使用完整 Given/When/Then，供 TDD Red 驱动 harness 与资源布局。

---

## 2. Actor / Role

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **平台 / 渲染工程师** | 维护语料包与断言 harness | 包约定、成熟度、verify 绿灯 |
| **后续切片交付方（K01–K06 / G02 / O01）** | 在交付保真变更时充实语料 | 对应主题包从 PLACEHOLDER → ACTIVE |
| **CI / `mvn verify`** | 自动发现并执行 ACTIVE 包 | 失败即阻断合并 |
| **（间接）法务 / 模板测试人员** | 受益于回归护栏 | 不直接操作本目录 |

---

## 3. Goal

1. 固化可发现的 **≥8** 包金标目录骨架与统一包布局。
2. 提供可复用的 **golden-corpus harness**，在 `mvn verify` 中执行 ACTIVE 包的 DOCX 关键路径 + PDF 文本断言。
3. 至少 **2** 个 ACTIVE 最小样本在本片交付时 **稳定绿灯**（证明接入真实，非空架子）。
4. PLACEHOLDER 包 **结构完整且被枚举**，但不因「业务能力尚未实现」而失败 verify。
5. **禁止** 像素级比对；加密样本遵守既有加密契约（密码仅测试夹具、不落库/不进日志）。

---

## 4. 已确认决策（2026-07-14）

| ID | 决策 |
| --- | --- |
| **K07-C1** | 语料根路径固定为 `backend/src/test/resources/golden-corpus/`（相对 backend 模块）。 |
| **K07-C2** | 本片必须落地 **恰好覆盖** 下列 8 个主题包（目录名稳定；可加数字前缀排序）：`dual-font-master`、`cross-page-table`、`nested-clauses`、`compute-variables`、`chinese-uppercase-amount`、`specimen-watermark`、`encrypted-pdf`、`long-clause-limits`。 |
| **K07-C3** | 每个包目录至少包含：`manifest.json`（或等价 YAML）、`input/`（`master.docx` + `template.json` + `variables.json`）、`expected/`（`docx-assertions.json` + `pdf-assertions.json`）。允许 `README.md` 说明主题与充实责任切片。 |
| **K07-C4** | 包成熟度枚举：`ACTIVE`（verify 必须执行断言）\| `PLACEHOLDER`（结构 + 枚举；**跳过** 业务断言，但缺失必需骨架文件仍 FAIL）。 |
| **K07-C5** | 本片 ACTIVE 最小样本（≥2）：**`nested-clauses`**、**`encrypted-pdf`**。其余 6 包本片为 PLACEHOLDER（由 K02/K03/K06a/G02 等后续充实为 ACTIVE）。`long-clause-limits` 若实现方可在本片升为 ACTIVE，但不强制第三 ACTIVE。 |
| **K07-C6** | DOCX 断言 = 对解压/`document.xml`（及必要时 styles/header）的 **关键路径或稳定 XPath** 存在性/文本/属性检查；PDF 断言 = PDFBox（或现有 PDF 测试工具）**文本抽取包含/不包含** 子串；**禁止** 位图/截图/SSIM/像素 golden。 |
| **K07-C7** | `encrypted-pdf` ACTIVE 样本使用 **测试夹具密码**（符合现有 12–128 字符基线）；断言在解密后抽取文本；密码不得出现在生产配置或日志期望中。加密参数模型遵循已确认契约（`enabled` / `openPassword` / `ownerPassword` / `permissions`）。 |
| **K07-C8** | Harness 以 JUnit 测试类形式挂入 backend 测试生命周期，由 **`mvn verify`（Surefire/既有 test 阶段）** 执行；无需独立 Maven profile 才能跑（可选 profile 仅作加速，不得作为唯一入口）。 |
| **K07-C9** | 若本机/CI **无可用 LibreOffice/`soffice`**：ACTIVE 包的 **PDF 半段** 允许 `Assumptions.assumeTrue` 跳过（与 `RenderingFontSmokeTest` / F4 模式一致）；**DOCX 半段必须执行**。PLACEHOLDER 包不触发转换。 |
| **K07-C10** | 本片 **不** 实现：母版 style catalog 权威（K02）、compute/SPELL_AMOUNT（K03）、跨页 `tblHeader`（K06a）、SPECIMEN 水印（G02）、release 钉扎（K01）。这些包仅占位。 |
| **K07-C11** | 后续切片充实语料时 **必须复用** 本目录与包 ID，禁止平行再建第二套 golden 根路径。 |
| **K07-C12** | 失败语义 fail-closed：ACTIVE 断言失败 → 测试失败 → verify 红；骨架文件缺失（含 PLACEHOLDER）→ 测试失败；不得静默跳过缺失包。 |

---

## 5. 前置条件

- CORE-FORTRESS F1 统一 writer 可用（嵌套条款最小样本可走现有结构化→DOCX 路径）。
- PDF 加密服务已存在（`PdfEncryptionService` 等）可供 `encrypted-pdf` 最小样本使用。
- Backend 质量门禁仍为 `mvn -B -ntp -f backend/pom.xml verify`。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- 工程师合并 CE 内核/保真相关变更前执行 `mvn verify`。
- 本切片实现完成后，CI / 本地 verify 首次发现并执行 golden-corpus 套件。

---

## 7. Primary journey（骨架 + 最小样本）

1. 在 `golden-corpus/` 下创建 8 个主题包目录与 `manifest`（成熟度 ACTIVE/PLACEHOLDER）。
2. 为每个包放入 input / expected 骨架文件（PLACEHOLDER 可用最小合法夹具 + 空断言列表或显式 `"deferred": true`）。
3. 实现 harness：扫描根目录 → 校验骨架 → 对 ACTIVE 包渲染/转换 → 执行 DOCX/PDF 断言。
4. 将 harness 纳入 backend 测试，使 `mvn verify` 执行到该套件。
5. 确认 `nested-clauses` 与 `encrypted-pdf` ACTIVE 样本绿灯（PDF 半段在无 soffice 时可 skip）。

---

## 8. System responses

| 情况 | 系统（harness / verify）响应 |
| --- | --- |
| 根目录包数 &lt; 8 或缺失任一主题 ID | **FAIL**（结构门禁） |
| PLACEHOLDER 包骨架完整 | **PASS**（枚举成功；不跑业务断言） |
| PLACEHOLDER 缺 `input/` 或 `expected/` 必需文件 | **FAIL** |
| ACTIVE 包 DOCX 关键路径断言失败 | **FAIL** |
| ACTIVE 包 PDF 文本断言失败（soffice 可用时） | **FAIL** |
| ACTIVE 包需 PDF 但 soffice 不可用 | **SKIP** PDF 半段；DOCX 仍必须 PASS |
| 发现像素/图片 golden 比对配置 | **FAIL** 或实现阶段拒绝该断言类型（本片不得引入） |

---

## 9. 验收场景（Given / When / Then）

### A. 目录与包骨架

#### BDD-CE-K07-001 — 语料根目录存在

**Given** 切片交付完成  
**When** 检查 backend 测试资源树  
**Then** 存在目录 `backend/src/test/resources/golden-corpus/`

#### BDD-CE-K07-002 — 至少八个主题包

**Given** `golden-corpus/` 根目录  
**When** harness 或结构测试枚举包  
**Then** 包数量 **≥ 8**  
**And** 下列主题 ID 均存在：`dual-font-master`、`cross-page-table`、`nested-clauses`、`compute-variables`、`chinese-uppercase-amount`、`specimen-watermark`、`encrypted-pdf`、`long-clause-limits`

#### BDD-CE-K07-003 — 统一包布局

**Given** 任一主题包目录  
**When** 检查包内文件  
**Then** 存在 `manifest`（含稳定 `id`、`theme`、`maturity`）  
**And** 存在 `input/master.docx`、`input/template.json`、`input/variables.json`  
**And** 存在 `expected/docx-assertions.json`、`expected/pdf-assertions.json`

#### BDD-CE-K07-004 — PLACEHOLDER 骨架完整性仍强制

**Given** 某包 `maturity=PLACEHOLDER`  
**When** 删除其 `input/template.json`（或任一必需骨架文件）后运行结构校验  
**Then** 测试 **FAIL**（不得因 PLACEHOLDER 而忽略缺失文件）

---

### B. Harness 与 mvn verify 接入

#### BDD-CE-K07-005 — verify 发现并执行套件

**Given** backend 测试类路径上存在 golden-corpus harness  
**When** 执行 `mvn -B -ntp -f backend/pom.xml verify`  
**Then** 测试报告中出现 golden-corpus 相关用例（结构枚举 + ACTIVE 断言）  
**And** 在 ACTIVE 样本满足预期时该套件为绿（或 PDF 半段因无 soffice 而 skip）

#### BDD-CE-K07-006 — ACTIVE 与 PLACEHOLDER 分流

**Given** 语料中同时存在 ACTIVE 与 PLACEHOLDER 包  
**When** harness 运行  
**Then** 对每个 ACTIVE 包执行 DOCX 断言（及可用时的 PDF 断言）  
**And** 对每个 PLACEHOLDER 包仅做骨架校验，**不**因未实现的 K02/K03/K06/G02 行为失败

#### BDD-CE-K07-007 — 禁止像素比对

**Given** 某包 `expected` 配置试图声明截图/像素/图片 hash 比对  
**When** harness 加载断言配置  
**Then** 配置被拒绝或测试 FAIL  
**And** 文档与实现约定仅允许 DOCX 关键路径与 PDF 文本抽取类断言

---

### C. 最小可绿样本（本片 ACTIVE）

#### BDD-CE-K07-008 — nested-clauses ACTIVE 最小样本

**Given** 包 `nested-clauses` 且 `maturity=ACTIVE`，input 含可被现有 writer 渲染的嵌套条款/条件结构夹具  
**When** harness 生成 DOCX（及可用时 PDF）  
**Then** DOCX 关键路径断言通过（例如嵌套段落/条款文本节点存在）  
**And** 若 soffice 可用，PDF 文本抽取包含期望子串；否则 PDF 半段 skip

#### BDD-CE-K07-009 — encrypted-pdf ACTIVE 最小样本

**Given** 包 `encrypted-pdf` 且 `maturity=ACTIVE`，input 声明符合契约的加密参数与测试夹具密码  
**When** harness 生成并加密 PDF（DOCX 路径按包配置）  
**Then** 未提供密码时无法按明文方式抽取业务正文（或等价「已加密」可观察证据）  
**And** 使用夹具 `openPassword` 解密后 PDF 文本断言通过  
**And** 密码不出现在失败消息的明文期望之外的日志断言中

#### BDD-CE-K07-010 — ACTIVE DOCX 回归失败会红

**Given** `nested-clauses` 为 ACTIVE  
**When** 故意改坏 `expected/docx-assertions.json` 中的关键路径期望后运行 harness  
**Then** 对应测试 **FAIL**（证明护栏非空操作）

---

### D. PLACEHOLDER 主题包（结构占位）

#### BDD-CE-K07-011 — dual-font-master 占位（待 K02）

**Given** 包 `dual-font-master` 为 PLACEHOLDER  
**When** harness 枚举  
**Then** 包被发现且骨架完整  
**And** 本片 **不** 要求断言「无 Calibri / 双字体落地」（属 K02 充实范围）

#### BDD-CE-K07-012 — cross-page-table 占位（待 K06a）

**Given** 包 `cross-page-table` 为 PLACEHOLDER  
**When** harness 枚举  
**Then** 骨架完整  
**And** 本片 **不** 要求断言 `<w:tblHeader/>` 或跨页表头行为  

> **Enrichment ownership (2026-07-15):** CE-K06a BDD **ready** — [ce-k06-rendering-fidelity.md](./ce-k06-rendering-fidelity.md)（`BDD-CE-K06a-004`）负责将本包升为 ACTIVE 并断言 `w:tblHeader`。  
> **Additional package (CE-K06b, 2026-07-15):** Wave 2 追加金标包 `09-qr-barcode`（同 `golden-corpus/` 根；非平行第二套体系）— 见 [ce-k06-rendering-fidelity.md](./ce-k06-rendering-fidelity.md) `BDD-CE-K06b-008`。  
> **Additional package (CE-K06c, 2026-07-15):** Wave 3 追加金标包 `10-attachment-list`（同根；`maturity: ACTIVE`；`string[]` → 编号列表）— 见 [ce-k06-rendering-fidelity.md](./ce-k06-rendering-fidelity.md) `BDD-CE-K06c-008`。

#### BDD-CE-K07-013 — compute-variables 占位（待 K03）

**Given** 包 `compute-variables` 为 PLACEHOLDER  
**When** harness 枚举  
**Then** 骨架完整  
**And** 本片 **不** 要求 `computeExpression` 求值结果断言

#### BDD-CE-K07-014 — chinese-uppercase-amount 占位（待 K03）

**Given** 包 `chinese-uppercase-amount` 为 PLACEHOLDER  
**When** harness 枚举  
**Then** 骨架完整  
**And** 本片 **不** 要求中文大写金额（SPELL_AMOUNT）文本断言

#### BDD-CE-K07-015 — specimen-watermark 占位（待 CE-G02）

**Given** 包 `specimen-watermark` 为 PLACEHOLDER  
**When** harness 枚举  
**Then** 骨架完整  
**And** 本片 **不** 要求 SPECIMEN 水印出现在预览/test-generate 产物中的断言

#### BDD-CE-K07-016 — long-clause-limits 占位或可选 ACTIVE

**Given** 包 `long-clause-limits` 存在且骨架完整  
**When** harness 枚举  
**Then** 包被发现  
**And** 若本片保持 PLACEHOLDER：不因极限长度业务阈值失败  
**And** 若实现方将其升为 ACTIVE：则 DOCX/PDF 断言必须描述「极限长条款」可观察结果（截断策略或完整保留——**以本片实现时写入 expected 的具体断言为准**，不得与未确认产品阈值冲突）

---

### E. 边界与后续契约

#### BDD-CE-K07-017 — 主题包缺失 fail-closed

**Given** 故意移除 `specimen-watermark` 包目录  
**When** 运行结构门禁  
**Then** 测试 **FAIL** 并指出缺失主题

#### BDD-CE-K07-018 — 后续切片充实路径

**Given** 未来 CE-K02（或 K03/K06a/G02）交付对应能力  
**When** 该切片完成  
**Then** 对应 PLACEHOLDER 包升级为 ACTIVE，并写入真实 expected 断言  
**And** 仍使用 `backend/src/test/resources/golden-corpus/` 同一根路径与主题 ID（K07-C11）

#### BDD-CE-K07-019 — soffice 不可用时的 PDF 半段

**Given** ACTIVE 包需要 PDF 文本断言且环境无 `soffice`  
**When** harness 运行  
**Then** PDF 断言以 assumption **skip**（明确原因）  
**And** 同一包的 DOCX 断言仍执行且决定该包 DOCX 结果

---

## 10. Boundary / exception（汇总）

| 边界 | 行为 |
| --- | --- |
| 包数不足 / 主题 ID 缺失 | FAIL |
| PLACEHOLDER 缺文件 | FAIL |
| ACTIVE 断言失败 | FAIL |
| 无 soffice | PDF skip；DOCX 仍跑 |
| 像素比对 | 禁止 |
| K01–K06/G02 能力 | 本片不做；仅占位 |
| 加密密码 | 仅测试夹具；遵循既有加密参数契约 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 目录树 | `backend/src/test/resources/golden-corpus/<package>/…` |
| 测试类 | golden-corpus harness（JUnit），出现在 surefire 报告 |
| 门禁 | `mvn -B -ntp -f backend/pom.xml verify` 绿（或仅 PDF skip） |
| ACTIVE 样本 | `nested-clauses`、`encrypted-pdf` 断言通过证据 |
| 非目标 | 无前端 E2E；无 Docker 部署门禁硬依赖（PDF skip 允许本地无 LO） |

---

## 12. Traceability

| 来源 | 关系 |
| --- | --- |
| [CE-K07 计划条目](../plan/core-excellence-program-2026-07.md) | 本片直接交付「骨架先行」；完整金标样本随 K01–K06 充实 |
| CE-K02 / K03 / K06a / G02 / O01 | 依赖或消费本骨架；G02 明确依赖 K07 骨架 |
| 加密：permission-matrix / requirements-plan / PRD | `encrypted-pdf` 样本不得违反已确认加密参数规则 |
| CORE-FORTRESS F1 / F4 | writer 路径与 soffice skip 模式对齐 |

---

## 13. Explicit non-goals（本片）

- 不实现双字体权威样式、compute/SPELL_AMOUNT、跨页表头、SPECIMEN 水印、release 钉扎。
- 不引入像素/视觉回归。
- 不要求前端 E2E / UIUX。
- 不宣称生产 go-live；不激活 CD-3。
- 不发明正式 plan phase。

---

## 14. Open questions（非阻塞）

下列问题 **不阻塞** `bdd_readiness: ready`；实现阶段由工程师按默认建议执行，若需改默认再回写本文件。

| # | 问题 | 默认建议（本片可采用） |
| --- | --- | --- |
| Q1 | `long-clause-limits` 本片 ACTIVE 还是 PLACEHOLDER？ | **PLACEHOLDER**（K07-C5）；若夹具可在现有 writer 下稳定断言再升 ACTIVE |
| Q2 | PDF 转换是否强制 Docker 内 soffice？ | **否**；无 soffice 则 PDF skip（K07-C9） |
| Q3 | manifest 用 JSON 还是 YAML？ | **JSON**（与 `template.json` / assertions 一致） |
| Q4 | 包目录是否强制数字前缀？ | **建议** `01-dual-font-master` 等形式；主题 ID 以 manifest.`id` 为准 |

---

## 15. BDD readiness

```
bdd_readiness: ready
acceptance_scenario_count: 19
open_questions: [Q1, Q2, Q3, Q4]  # non-blocking defaults above
owning_doc: docs/behavior/ce-k07-golden-corpus-skeleton.md
task_ids: [CE-K07, ce-k07-golden-corpus-skeleton]
next: plan-orchestrator → backend-engineer (TDD Red on harness + resources)
```

**Handoff：** 规格已就绪；可进入计划拆解与实现。实现方必须以失败测试先行覆盖 BDD-CE-K07-001…010（及结构类 011–019 中的门禁场景），再写最小代码使骨架 + 两个 ACTIVE 样本绿灯。
