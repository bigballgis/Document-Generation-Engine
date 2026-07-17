# BDD 行为规格：ADR-0042 / ADR-0043 关闭与 checklist #3b 诚实路径（PRR-C01）

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-18  
**BDD ID 前缀**: `BDD-PRR-C01`  
**来源**: Task Master **#103** PRR-C01 · Wave C · launch checklist **#3b** · [ADR-0042](../adr/rendering-authoring/0042-pagination-delta-budget.md) · [ADR-0043](../adr/rendering-authoring/0043-ooxml-output-validation-gate.md) · [pagination-delta-corpus.md](../plan/pagination-delta-corpus.md) · [NFR §生产渲染](../requirements/non-functional-requirements.md) · LR-A5/A6/A7 residuals  
**程序 / 清单**: [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) · [launch-readiness-gate.md](../plan/launch-readiness-gate.md)  
**Task / slice**: `prod-adr-0042-0043-closeout` · `task_ids: ["103"]`  
**Worktree**: `D:/working/DGE-prod-adr-0042-0043-closeout` · `feat/prod-adr-0042-0043-closeout`  
**授权依据**: Parent Stage 1 handoff（2026-07-18）— Accept/implement ADR-0042 分页 delta 强制；Accept/implement ADR-0043 OOXML gate；Word↔LibreOffice 证据 **或** 诚实 durable exemption；**禁止**在本 Stage 将 #3b 标为 **GO**；`frontend_ui_in_scope=false` unless UI required（本叶默认 **false**）  
**完成声明约束**: 本叶关闭渲染信任残差的**可验证路径**（强制 + ADR Accepted + 证据或 exemption）。**禁止**宣称 production go-live；**禁止**虚构 Word 页数；**禁止**在证据/exemption 未落盘前由本 BDD 翻转 checklist **#3b → GO**（#3b 翻转仅允许 post-evidence `post-task-doc-sync`）。整体清单在其它 NO-GO 未关前仍可保持 **NO-GO**。

---

## 1. 概述

Checklist **#3b**（LR-A residuals）要求：**ADR-0042 / ADR-0043 Accepted** + **Word pagination delta**（或诚实残差）。现状：

| 组件 | 现状（implementation 输入，非 Done 声明） |
| --- | --- |
| ADR-0042 | **Proposed**；`paginationDeltaBudgetPages` 已绑定默认 `1`；**无**运行时 delta 强制 |
| ADR-0043 | **Proposed**；Decision **slice A**（OPC + XML well-formedness fail-closed）已由 LR-A6 交付（`OoxmlOutputValidator` / `OOXML_VALIDATION_FAILED`）；slice B（ECMA-376 XSD + LO24 headless）**deferred** |
| Word↔LO 语料 | Docker PDF 已测（LR-A7）；Word / delta = **n/a**（`ms-word-unavailable-on-host`） |
| Checklist #3b | **NO-GO** |

本叶行为分三域：

| 行为域 | 摘要 |
| --- | --- |
| **C01-A ADR-0042 enforcement** | 确认 ±1 页预算；当作者声明的 Word 页数可得时，对 PDF 页数做 delta；越预算 → fidelity warning；越 `2×` 预算 → publish blocker；无 Word 页数 → **跳过**（不得用 Docker PDF 冒充 Word） |
| **C01-B ADR-0043 Accept (slice A)** | 将 ADR-0043 **Accepted** 锚定在已交付的 slice A；slice B 诚实 residual；运行时 gate 保持 fail-closed 且默认真开 |
| **C01-C Evidence or exemption** | Path **E**（Word 主机实测）或 Path **X**（durable exemption）；二者均不得虚构数字；#3b **GO** 仅 Path E；Path X → #3b 最多 **CONDITIONAL**（由 doc-sync 翻转，本 BDD 不翻转） |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 虚构 Word 页数 / 把 Docker PDF 当 Word 基线 | **禁止** |
| 本 Stage / 无证据时翻转 #3b → **GO** | **禁止** |
| ECMA-376 全量 XSD / LO24 headless 作为本叶 Done 线 | **Out of scope residual**（ADR-0043 slice B） |
| 像素级 Word↔PDF 一致承诺 | **禁止**（ADR-0042 / NFR 边界） |
| 管理端 Vue 录入 Word 页数 UI / Playwright / UIUX | **`frontend_ui_in_scope=false`**（作者页数经管理 API / 测试夹具；UI 为后续 residual） |
| checklist #5a / #9 / #10 / paste #5b / Kafka / LDAP | **Out of scope** |
| 宣称 production go-live / 激活 CD-3 | **禁止** |
| 触碰 `DGE-audit-governance` | **禁止** |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **模板作者 / 发布人** | Author / publisher | 声明权威 Word 页数（authoring baseline）；审阅分页 fidelity 警告；超 `2×` 预算时不得发布直至布局调整或更正声明 |
| **API / 引擎** | System | PDF 转换后计算 delta；发出 warning / 驱动 publish blocker；装配后 OOXML gate fail-closed |
| **测量主机操作者** | Measurer | Path E：在 Word-equipped host 打开语料 DOCX，记录 Word 页数并回填证据 |
| **发布评审人** | Launch reviewer | 按 §4 / §11 诚实规则对待 #3b（GO / CONDITIONAL / NO-GO）；不因本叶 Alone 签 go-live |
| **doc-keeper / post-task-doc-sync** | Docs | ADR status、语料表、exemption、#3b verdict — **仅在证据存在后**更新 |

---

## 3. Goal

1. **确认** ADR-0042 预算：`paginationDeltaBudgetPages = 1`（±1 页）为 v1 Accepted 预算。  
2. **实现** metadata-gated 运行时强制：有 `authorWordPageCount` 时比较 PDF 页数；无则跳过且不伪造。  
3. **实现** 越预算 warning + 越 `2×` 预算 publish blocker（与 ADR-0042 Decision 3–4 一致）。  
4. **Accept ADR-0043** 于 Decision **slice A**（已实现 gate + 回归）；slice B 明确 residual。  
5. 交付 Path **E** 证据 **或** Path **X** durable exemption（诚实残差）。  
6. **不**在本 BDD/本 Stage 将 #3b 标为 GO；整体 **not** go-live。  
7. `frontend_ui_in_scope=false`。

---

## 4. 已确认决策（confirmed）

### 4.1 ADR-0042 — 预算与强制（C01-A）

| ID | 决策 |
| --- | --- |
| **PAG-C1** | **预算 Accepted：** `docgen.rendering.pagination-delta-budget-pages` / `PAGINATION_DELTA_BUDGET_PAGES` 默认 **`1`**。Wave C / #103 将此提案升为 **Confirmed**（取代 NFR「pending user confirmation」措辞，由后续 doc-sync 落盘）。 |
| **PAG-C2** | **权威页数：** 作者声明字段名为 **`authorWordPageCount`**（`Integer`，可选），存于**模板版本**持久化元数据（`template_version` 列或等价 JSON；实现选型由 TDD 最小改动决定，契约须可测）。语义 = Microsoft Word 作者视图页数，**不是** LibreOffice / Docker PDF 页数。 |
| **PAG-C3** | **比较时机：** 成功完成 DOCX→PDF 转换后（preview 与 runtime PDF 成功路径），取 `pdfPages = PDF 页数`，若 `authorWordPageCount` 非 null 且 > 0，则 `delta = |pdfPages - authorWordPageCount|`。 |
| **PAG-C4** | **阈值（budget = B，默认 1）：**<br>• `delta ≤ B` → 不因分页预算发出 warning/blocker<br>• `B < delta ≤ 2×B` → fidelity **warning**（不阻断生成成功）<br>• `delta > 2×B` → fidelity **blocker** 语义：生成仍可产出工件供审阅，但 **PublishGate** 必须 **ready=false**（模板不得发布）直至 delta 回到阈值内或作者更正 `authorWordPageCount` / 调整布局后重测 |
| **PAG-C5** | **警告码：** 预算内越界 warning 使用既有 `LOW_RISK_PAGINATION_DIFFERENCE`（ADR-0019 基线码）；不得发明未入 OpenAPI 的调用方可见码而不更新契约。 |
| **PAG-C6** | **阻断码：** publish gate 新增（或等价映射）`PublishGateCheckCode.PAGINATION_DELTA_BUDGET`（名称以实现为准，须稳定、可测）。`delta > 2×B` 且最新成功 PDF preview（或约定的权威 PDF 测量点）仍越界 → 该项 **blocker=true**。 |
| **PAG-C7** | **无 Word 页数 → 跳过：** `authorWordPageCount` 缺失/null/≤0 时，**不得**计算 Word↔PDF delta，**不得**发出 PAG-C4/C5/C6 信号，**不得**用 Docker PDF 页数回填 `authorWordPageCount`。可保留既有无关分页的其它 fidelity 行为。 |
| **PAG-C8** | **可配置：** `B` 仍来自 `DocgenRenderingProperties.getPaginationDeltaBudgetPages()`；部署可调，但 v1 Accepted 文档默认仍为 **1**。 |
| **PAG-C9** | **写入面：** 本叶提供**管理 API**（或既有版本更新契约扩展）设置/读取 `authorWordPageCount`；OpenAPI / contract-outline 同步。**无** Vue UI（`frontend_ui_in_scope=false`）。测试用夹具直接设字段。 |
| **PAG-C10** | **ADR-0042 → Accepted：** 在强制落地 +（Path E 证据 **或** Path X exemption 落盘）后，由 doc-sync 将 ADR-0042 标为 **Accepted**，并诚实记录 Word 语料状态（已填 / n/a+exemption）。本 BDD **不**直接改 ADR frontmatter。 |

### 4.2 ADR-0043 — OOXML gate Accept（C01-B）

| ID | 决策 |
| --- | --- |
| **OOX-C1** | **Accepted 范围 = Decision slice A：** OPC open + Word XML parts well-formedness；失败 → `OOXML_VALIDATION_FAILED` / `RENDERING` / `api.error.rendering.ooxmlValidationFailed` / HTTP **422** / `retryable=false`；不落库、不预览。 |
| **OOX-C2** | **实现已存在则强化证据：** 确认 `OoxmlOutputValidator` 挂在装配路径；`docgen.rendering.ooxml-validation-enabled` 默认 **true**；回归覆盖 well-formed accept + corrupt reject（含 CD-PIT-03 类）。缺口则本叶 TDD 补齐。 |
| **OOX-C3** | **Slice B residual（诚实）：** 全量 ECMA-376 XSD、LibreOffice 24+ headless open **不**因本叶 Accepted 而宣称完成；记入 ADR Consequences / checklist residual。 |
| **OOX-C4** | **修订历史「不得仅凭 well-formedness Accept」：** Wave C 明确裁定 — **可以**在 slice A 已交付且 residual 诚实的前提下将 ADR-0043 标为 **Accepted**（关闭 #3b 对「0043 Accepted」的阻塞），**不得**把 Accepted 解读为「LO24-safe / 全 XSD」。doc-sync 更新 ADR 正文与 `docs/adr/README.md` 索引注记。 |
| **OOX-C5** | **禁用开关：** 属性可关（性能逃生），但验收/声称生产文档**不得**推荐默认关闭；测试可覆盖 enabled/disabled 行为。 |

### 4.3 证据 Path E vs Exemption Path X vs Blocked（C01-C）

| ID | 决策 |
| --- | --- |
| **EVD-C1** | **Path E — Word↔LO 证据（#3b GO 前置）：** 在 Word-equipped host，对语料 ≥5 封必测信（见 [pagination-delta-corpus.md](../plan/pagination-delta-corpus.md)）用 Microsoft Word 记录 `wordPages`；与已归档 Docker PDF 页数算 `delta`；回填 NFR 表 + corpus + `docs/evidence/lrp-a7-pagination/`（或本叶续证目录）；**禁止**编造。完成后 + ADR Accepted + 强制绿灯 → doc-sync 可将 **#3b → GO**。 |
| **EVD-C2** | **Path X — Durable exemption（#3b 非 GO）：** 当会话/宿主仍无 Word 时，允许落盘 **durable exemption**（建议路径：`docs/evidence/prod-adr-0042-0043-closeout/word-baseline-exemption.md` 或等价，须从 checklist / ADR / corpus 可链达），**必须**同时满足：<br>1. 明确 `method=ms-word-unavailable-on-host`（或运维签字的等价原因）<br>2. Word / delta 列保持 **n/a**（不填假数）<br>3. Docker PDF 基线仍为漂移哨兵（LR-A7 证据有效）<br>4. 运行时强制已按 PAG-C7 metadata-gated 落地<br>5. ADR-0042 / 0043 按 §4.1–4.2 Accepted（含 residual 文案）<br>6. 残差所有者与复测触发条件（Word host 可用时走 Path E）<br>7. 声明：**本 exemption ≠ #3b GO；≠ Word↔LO 已证明** |
| **EVD-C3** | **Checklist #3b 诚实规则：**<br>• **NO-GO** — ADR 仍 Proposed，或强制未落地，或既无 Path E 亦无合格 Path X<br>• **CONDITIONAL** — Path X + ADR Accepted + 强制落地 + residual 诚实（Word 仍 n/a）— **仅** post-evidence doc-sync 可写<br>• **GO** — Path E 语料 Word/delta 已填 + ADR Accepted + 强制落地<br>本 Stage 1 BDD **不**改 checklist 表内 verdict。 |
| **EVD-C4** | **Blocked（本叶不得宣称 remediated）：** 虚构 Word 数字；无强制却标 ADR-0042 Accepted；把 slice A 说成 LO24/XSD 完成；无 Path E/X 却标 #3b CONDITIONAL/GO；宣称 go-live。 |
| **EVD-C5** | **会话内无 Word → 默认走 Path X 规格：** 实现与文档按 Path X 关闭强制/ADR；#3b 保持 **NO-GO** 直至 doc-sync 按 EVD-C3 评估（预期 **CONDITIONAL**）。若后续出现 Word host，另开测量叶子走 Path E 再冲 GO。 |

### 4.4 范围与诚实（横切）

| ID | 决策 |
| --- | --- |
| **XR-C1** | **`frontend_ui_in_scope=false`：** 无 Vue/i18n/Playwright/UIUX；E2E stages N/A。 |
| **XR-C2** | **不翻转 #3b→GO（本叶任何阶段，含 BDD 落盘时）。** |
| **XR-C3** | **Formal phase 保持 None；** 不激活 CD-3；不触碰 audit-governance worktree。 |
| **XR-C4** | **与 IBL-B7：** Path X 关闭「强制未落地 / ADR Proposed」类阻塞；Word 实测仍为 IBL/checklist 残差，直至 Path E。 |

### 4.5 上游现状（implementation 输入）

| 发现 | 证据 |
| --- | --- |
| 预算属性存在、未强制 | `DocgenRenderingProperties.paginationDeltaBudgetPages`；无 `authorWordPageCount` 字段/比较器 |
| OOXML slice A 已实现 | `OoxmlOutputValidator` + `DocxAssembler`；`OoxmlOutputValidationGateTest`；契约 `OOXML_VALIDATION_FAILED` |
| Word 基线不可用 | ADR-0042 Residual；NFR footnote；corpus `ms-word-unavailable-on-host` |
| PublishGate 无分页预算项 | `PublishGateCheckCode` 无 `PAGINATION_DELTA_*` |
| 警告码已有 | `FidelityWarningCode.LOW_RISK_PAGINATION_DIFFERENCE` |
| #3b | **NO-GO** |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | PDF 转换成功且版本含 `authorWordPageCount` | 计算 delta；可能 warning / gate blocker |
| T2 | PDF 转换成功但无 Word 页数 | 跳过分页预算强制（PAG-C7） |
| T3 | 装配后 OOXML 校验失败 | 422 fail-closed（OOX-C1） |
| T4 | 发布评估 / Publish | `PAGINATION_DELTA_BUDGET` blocker 时 ready=false |
| T5 | Path E 测量完成或 Path X exemption 落盘 | doc-sync 更新 ADR / #3b（非本 BDD 文件职责） |

---

## 6. Preconditions

- 工作树：`D:/working/DGE-prod-adr-0042-0043-closeout` · `feat/prod-adr-0042-0043-closeout`。  
- Formal phase：**None**；单车道交付叶 #103。  
- Docker 验收栈经 `.\scripts\docker-deploy-queue.ps1`（需要部署证据时）。  
- 不得假设本机已装 Microsoft Word；无 Word 时走 Path X。  
- LR-A7 Docker PDF 证据保持可读。

---

## 7. Primary journey

### 7.1 强制路径（有 Word 页数）

1. Author（或测试）经管理 API 为模板版本设置 `authorWordPageCount = W`。  
2. 系统生成 PDF（preview 或 runtime）；`pdfPages = P`。  
3. 计算 `delta = |P - W|`，按 PAG-C4 发出 warning 和/或标记 publish blocker。  
4. `delta ≤ B`：无本预算信号；可发布（其它门禁另计）。  
5. `B < delta ≤ 2×B`：成功响应含 `LOW_RISK_PAGINATION_DIFFERENCE`；PublishGate 该项不因本预算阻断。  
6. `delta > 2×B`：PublishGate `PAGINATION_DELTA_BUDGET` blocker；发布拒绝直至修复。

### 7.2 跳过路径（无 Word 页数 — 当前宿主常态）

1. 版本无 `authorWordPageCount`。  
2. PDF 生成成功。  
3. **不**发出 PAG-C4 分页预算 warning/blocker。  
4. OOXML gate 仍按 OOX-C1 运行。

### 7.3 OOXML fail-closed

1. 装配产出畸形 OOXML（或测试夹具）。  
2. Gate 拒绝 → 统一错误信封 `OOXML_VALIDATION_FAILED`；不持久化、不预览。

### 7.4 Closeout 文档路径

1. 实现强制 + 回归绿灯。  
2. Path E **或** Path X。  
3. doc-sync：ADR-0042/0043 → Accepted；#3b 按 EVD-C3（**不**在无 Path E 时 GO）。

---

## 8. System responses

| 场景 | 响应 |
| --- | --- |
| delta 在预算内 | 无本预算 warning/blocker |
| 越预算 ≤2×B | `LOW_RISK_PAGINATION_DIFFERENCE`；生成成功 |
| 越预算 >2×B | PublishGate blocker；生成可供审阅 |
| 无 authorWordPageCount | 跳过本预算逻辑 |
| OOXML 失败 | 422 / `OOXML_VALIDATION_FAILED` / `retryable=false` |
| Path X | 证据文档 + residual；#3b 非 GO |
| Path E | 语料 Word/delta 回填；#3b 可 GO（doc-sync） |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-PRR-C01-001 — 预算内无分页预算信号

**Given** 模板版本 `authorWordPageCount = 6` 且 `paginationDeltaBudgetPages = 1`  
**And** 转换后 PDF 页数为 6 或 7（`delta ≤ 1`）  
**When** 成功生成 PDF  
**Then** 响应 fidelity 中**不**因分页预算新增 `LOW_RISK_PAGINATION_DIFFERENCE`（本叶职责范围内）  
**And** PublishGate `PAGINATION_DELTA_BUDGET` **不是** blocker

### BDD-PRR-C01-002 — 越预算 warning（≤2×B）

**Given** `authorWordPageCount = 6`，`B = 1`，PDF 页数 = 8（`delta = 2`）  
**When** 成功生成 PDF  
**Then** fidelity 含 `LOW_RISK_PAGINATION_DIFFERENCE`  
**And** PublishGate 分页预算项 **不是** blocker  
**And** 生成成功（非 4xx）

### BDD-PRR-C01-003 — 越 2×B → publish blocker

**Given** `authorWordPageCount = 6`，`B = 1`，权威 PDF 测量 `delta = 3`  
**When** 评估 PublishGate（PUBLISH）  
**Then** `PAGINATION_DELTA_BUDGET`（或等价）为 **blocker**  
**And** `ready = false`  
**And** 发布 API 拒绝（既有 publish-not-ready 语义）

### BDD-PRR-C01-004 — 无 Word 页数跳过（禁止伪造）

**Given** 模板版本 **未**设置 `authorWordPageCount`  
**And** Docker/LO PDF 页数已知（例如 6）  
**When** 成功生成 PDF  
**Then** **不**发出本预算 warning/blocker  
**And** 系统**未**将 PDF 页数写入 `authorWordPageCount`

### BDD-PRR-C01-005 — 预算属性可配置

**Given** `docgen.rendering.pagination-delta-budget-pages = 2`  
**And** `authorWordPageCount = 6`，PDF 页数 = 8（`delta = 2`）  
**When** 成功生成 PDF  
**Then** `delta ≤ B` → **无**本预算 warning  
（与默认 B=1 时 BDD-002 区分，证明读取配置）

### BDD-PRR-C01-006 — OOXML well-formed accept

**Given** 装配产出结构良好的语料/夹具 DOCX  
**And** `ooxml-validation-enabled = true`  
**When** 校验运行  
**Then** 通过；文档可进入后续预览/持久化路径（按既有装配语义）

### BDD-PRR-C01-007 — OOXML corrupt reject fail-closed

**Given** 含畸形 XML / 未转义 `&` 类缺陷的 DOCX 夹具（CD-PIT-03）  
**When** 装配后校验  
**Then** 失败关闭；错误码 `OOXML_VALIDATION_FAILED`；category `RENDERING`；`retryable=false`；不落库/不预览

### BDD-PRR-C01-008 — authorWordPageCount API 可读写

**Given** 授权管理会话可更新模板版本  
**When** 设置 `authorWordPageCount` 为正整数并再读取  
**Then** 持久化值一致  
**When** 清除或设为 null（若契约允许）  
**Then** 后续生成走 BDD-004 跳过语义

### BDD-PRR-C01-009 — Path X exemption 形态（文档验收）

**Given** 宿主无 Microsoft Word（或会话选择 Path X）  
**When** 本叶交付 closeout 文档  
**Then** 存在 durable exemption，满足 EVD-C2 全部条款  
**And** 语料 Word/delta 仍为 **n/a**  
**And** checklist #3b **未被**本变更标为 **GO**

### BDD-PRR-C01-010 — Path E 证据形态（若 Word 可用）

**Given** Word-equipped host 可打开语料 DOCX  
**When** 完成 ≥5 封必测信 Word 页数记录并与 Docker PDF 比对  
**Then** NFR / corpus / evidence 中 Word 与 delta **非** n/a（真实测量）  
**And** **无**编造页数  
**And** 仅在此路径下 doc-sync **可以**将 #3b → **GO**

### BDD-PRR-C01-011 — ADR Accepted 诚实边界

**Given** 强制与 OOX slice A 证据绿灯，且 Path E 或 Path X 已满足  
**When** doc-sync 更新 ADR  
**Then** ADR-0042 / ADR-0043 可为 **Accepted**  
**And** ADR-0043 正文标明 slice B residual  
**And** ADR-0042 正文标明语料 Word 状态（已测或 n/a+exemption）  
**And** **不**声称像素级 Word 一致或 LO24/XSD 已完成

### BDD-PRR-C01-012 — #3b 与 go-live 诚实

**Given** 本叶任意中间或完成状态  
**When** 审查 launch-readiness-checklist  
**Then** 本叶交付物**不**把 #3b 写成 **GO**，除非 Path E 证据已由 doc-sync 确认  
**And** **不**宣称 production go-live

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| `authorWordPageCount` ≤ 0 | 视为未声明 → 跳过（PAG-C7） |
| PDF 转换失败 | 既有转换错误模型；不发本预算信号 |
| 仅 DOCX 输出（无 PDF） | 本预算比较不适用（无 `pdfPages`）；不强制 |
| OOXML 校验关闭 | 可测；非验收推荐默认 |
| 与其它 PublishGate blockers 并存 | 任一 blocker → ready=false |
| 并发/多版本 | 比较针对**当前评估版本**的声明页数与其权威 PDF 测量 |

---

## 11. Observable evidence

| 证据 | 用途 |
| --- | --- |
| 单测：delta 阈值 × warning × publish blocker × 跳过 | PAG 强制 |
| `OoxmlOutputValidationGateTest`（及增补） | OOX slice A |
| 管理 API 契约 + 测试 | `authorWordPageCount` |
| `mvn -B -ntp -f backend/pom.xml verify` | 质量门 |
| Path E：更新后的 corpus / NFR / evidence JSON | #3b GO |
| Path X：`docs/evidence/.../word-baseline-exemption.md`（或等价） | #3b CONDITIONAL 候选 |
| ADR-0042/0043 Accepted + adr README | closeout |
| checklist #3b verdict（仅 doc-sync） | GO / CONDITIONAL / NO-GO |
| 架构评审 Critical=0 | merge 门 |
| 队列部署（若 stage 要求） | 运行时可见性 |

---

## 12. Traceability

| 工件 | 关系 |
| --- | --- |
| Task Master **#103** | 本叶 |
| Checklist **#3b** | 关闭目标（诚实 GO/CONDITIONAL） |
| [ADR-0042](../adr/rendering-authoring/0042-pagination-delta-budget.md) | 预算 + 强制 |
| [ADR-0043](../adr/rendering-authoring/0043-ooxml-output-validation-gate.md) | OOXML gate |
| [ADR-0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) | `LOW_RISK_PAGINATION_DIFFERENCE` |
| LR-A5 / A6 / A7 | 历史交付与残差 |
| CD-PIT-02 / CD-PIT-03 | 分页漂移 / OOXML 严格性 |
| NFR §生产渲染 · pagination-delta-corpus | 语料与测量规程 |
| OpenAPI / contract-outline | `OOXML_VALIDATION_FAILED`；`authorWordPageCount`；FidelityWarningCode |
| IBL-B7 | Word host 阻塞的上游映射 |

---

## 13. TDD Red 提示（给 plan / implementer）

1. **Red：** 设 `authorWordPageCount` + 控制 PDF 页数（夹具/ stub page count）→ 断言 warning / PublishGate blocker 阈值。  
2. **Red：** 无 `authorWordPageCount` → 断言无本预算信号。  
3. **Green：** 最小比较器 + 属性 + gate 项 + API 字段。  
4. **OOXML：** 若既有测试已覆盖 006/007，本叶以回归锁定；缺口则补。  
5. **Docs：** ADR Accepted + Path X/E — **post-task-doc-sync**；**勿**在实现中途把 #3b 标 GO。

---

## 14. BDD readiness

```
bdd_readiness: ready
open_questions: []   # 非阻塞残差见下
owning_doc: docs/behavior/prod-adr-0042-0043-closeout.md
task_ids: ["103"]
slice_id: prod-adr-0042-0043-closeout
frontend_ui_in_scope: false
checklist_3b: do-not-flip-to-GO-in-this-stage
```

### 非阻塞残差（不阻碍 `ready`；阻碍 #3b GO）

| 残差 | 处理 |
| --- | --- |
| 本机无 Microsoft Word | 默认 Path X；#3b GO 延后至 Path E |
| ADR-0043 slice B（XSD / LO24） | Accepted 后仍标 residual |
| Word 页数录入 UI | 后续 UX residual；本叶 API/夹具足够 |

### 若升级为 `blocked` 的条件（当前未触发）

- 用户否定 ±1 预算或否定「0043 可在 slice A Accepted」裁定；或  
- 要求本叶必须完成 Path E / slice B 才能继续实现 — 则停写代码并等待确认。

---

## 15. Handoff to plan-orchestrator

- **Next:** `plan-orchestrator` 分解 #103 实现任务（backend：字段 + 比较器 + PublishGate + API；doc-keeper：ADR Accepted 文案、Path X/E、#3b 规则）。  
- **Implementers:** `backend-engineer` / `rendering-engineer`（分页比较靠近渲染管线时优先 rendering）。  
- **Gates:** `mvn verify`；FE/E2E N/A；架构评审；按需 queued deploy。  
- **End:** stage 11 merge → MAIN doc-sync（ADR/#3b 诚实）→ commit-review。  
- **禁止：** 无 Path E 时 #3b GO；虚构 Word 页数；宣称 go-live。
