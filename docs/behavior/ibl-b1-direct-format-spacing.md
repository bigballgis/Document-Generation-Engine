# BDD 行为规格：IBL-B1 — Direct-format paragraph spacing / indents（关闭白名单↔writer 缺口）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-B1` |
| **编写日期** | 2026-07-19 |
| **程序 / 队列** | IBL Wave B · **IBL-B1** / F9（`ibl-b1-direct-format-spacing`） |
| **Slice** | `ibl-b1-direct-format-spacing` |
| **Branch** | `feat/ibl-b1-direct-format-spacing` |
| **Worktree** | `D:/working/DGE-ibl-b1-direct-format-spacing` |
| **Base** | `d14a1507`（`origin/main`） |
| **Placement** | ISOLATED |
| **Task Master** | **#113** IBL-B1 — Batch Recommendation **solo**；`member_task_ids: ["113"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-b1-direct-format-spacing`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F9 / IBL-B1；白名单契约 [ADR-0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) + [domain-model.md](../domain/domain-model.md) §2.6.3；样式权威 [ce-k02-master-style-authority.md](./ce-k02-master-style-authority.md)（run 字体仍可覆盖） |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（本叶为结构化 DOCX writer / POI 保真；E2E/UIUX **N/A**） |

**完成声明约束：** 本叶关闭 F9——白名单内段落间距/缩进在 DOCX 中**真实生效**（POI 可观测），**禁止**静默忽略。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave B / 程序 Done；**禁止**把 IBL-B2（PDF 容量）/ B3（veraPDF）/ B7（Word Path E）并入本叶。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["113"]
  proposed_slice_id: ibl-b1-direct-format-spacing
  shared_acceptance_surface: >
    Whitelisted paragraph spacing/indent applied in DOCX (POI assertions);
    no silent ignore of DirectFormatRules keys
  vetoes_applied:
    - b2-pdf-capacity
    - b3-verapdf
    - b7-word-path-e
    - umbrella-106-registry-only
  evidence_amortization: mvn verify (+ queued docker when acceptance surface requires)
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| 白名单段落属性 `lineSpacing` / `spacingBefore` / `spacingAfter` / `firstLineIndent` / `leftIndent` / `rightIndent` 写入 DOCX 段落属性（POI） | IBL-B2 PDF 转换容量 / DEF-LRP-D6-001 |
| 结构化 writer **读取并应用**节点 `directFormat`（含既有 run 属性接线若缺口） | IBL-B3 veraPDF |
| POI 断言：间距/缩进可从输出 DOCX 读回 | IBL-B7 Word Path E / #3b GO |
| 保持 run 白名单 `fontFamily` / `fontSize` / `textColor` 仍可应用（不回归） | 收窄白名单为主路径（见 §4.2 备选，**未选用**） |
| 非法/非数值 fail-closed（绑定校验 blocker 或等价） | 任意 CSS / 全局页边距 / 分栏（仍 `DIRECT_FORMAT_GLOBAL_LAYOUT`） |
| Gates：`mvn -B -ntp -f backend/pom.xml verify` | Playwright / OA 旅程；翻转 #3b/#5a；go-live |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| `DirectFormatRules.WHITELIST` 含间距/缩进六键 + 字体三键 | `DirectFormatRules.java` |
| 绑定/发布校验接受上述键；白名单外 → `DIRECT_FORMAT_OUT_OF_WHITELIST`；全局版式 → `DIRECT_FORMAT_GLOBAL_LAYOUT` | `MasterStyleCatalogValidationSupport.validateDirectFormat` |
| Writer `applyDirectFormatIfPresent` **仅**处理 `fontFamily` / `fontSize` / `textColor` | `StructuredContentDocxStyleSupport` |
| 调用方多数路径**未**把节点 `directFormat` 传入 `writeRunText(..., directFormat)`；段落路径也**未**把间距/缩进落到 `XWPFParagraph` | `StructuredContentDocxInlineSupport` / `StructuredContentDocxBlockDispatchSupport` |
| Apache POI 5.5（项目 pin）对段落间距/缩进有公开 API | `XWPFParagraph#setSpacingBefore/After`、`setSpacingBetween`、`setIndentationLeft/Right/FirstLine`（twips / line rule） |
| 作者信任假控件：可保存白名单间距，生成件无对应 OOXML | F9；IBL-B1 验收 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **B1-S1 Wire** | 结构化 DOCX 装配从节点 JSON 读取 `directFormat` 并传入样式支持 |
| **B1-S2 Paragraph apply** | 段落键应用到当前写出的 `XWPFParagraph`（POI） |
| **B1-S3 Run apply** | 字体三键继续应用到对应 `XWPFRun`（含接线补齐） |
| **B1-S4 Honest contract** | 白名单键不得静默丢弃；POI 断言证明生效 |
| **B1-S5 Fail-closed values** | 非法类型/负值等 → 保真 blocker（发布 fail-closed），不半应用半忽略 |
| **B1-S6 Escape hatch（备选，未选用）** | 若某键经证明无法用 POI 可靠落盘，**必须**从白名单移除并保持/新增 fail-closed 发布消息——仍禁止静默忽略 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板作者** | 结构化内容树编辑；可写白名单 `directFormat` | 期望间距/缩进在预览与正式件中可见 |
| **发布闸门** | `PublishGate` / 绑定保真校验 | 白名单外/全局版式/非法值 → blocker |
| **Runtime / Preview 调用方** | 触发生成装配 | 获得含段落属性的 DOCX |
| **系统（writer）** | `StructuredContentDocx*` / `StructuredContentDocxStyleSupport` | 诚实应用白名单键 |

---

## 3. Goal

1. 关闭 F9：白名单段落间距/缩进在 DOCX 输出中**实际生效**（POI 可读回）。  
2. 作者可见契约与 writer 能力一致——**无静默忽略**。  
3. 保持 ADR-0019 / domain §2.6.3 白名单集合（本叶主路径 = **应用**，非收窄）。  
4. 字体三键不回归；CE-K02 母版继承规则不因本叶破坏（显式 `directFormat` 仍可覆盖 run 字体）。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave B Done。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **B1-C1** | **主路径 = Apply（应用）。** 白名单六段落键必须经 Apache POI 写入段落属性；验收以 POI 读回（及/或 OOXML XPath）为准。计划中的「收窄白名单」仅为备选（§4.2），**本叶不采用**为默认交付。 | 用户偏好；POI 5.5 支持；IBL-B1 验收「applied in DOCX」优先 |
| **B1-C2** | **完整白名单键集合（本叶须诚实）：** run：`fontFamily`、`fontSize`、`textColor`；paragraph：`lineSpacing`、`spacingBefore`、`spacingAfter`、`firstLineIndent`、`leftIndent`、`rightIndent`。集合与 `DirectFormatRules` / ADR-0019 / domain §2.6.3 **对齐**，本叶不删键。 | SoT 一致 |
| **B1-C3** | **段落键 → `XWPFParagraph`：** 在写出该块段落时应用。权威附着点优先为 `type=paragraph`（及同等块段落如 `sectionHeading` 若携带 `directFormat`）节点自身的 `directFormat`。若仅子 inline 节点携带段落键，则应用到**当前封闭** `XWPFParagraph`（同段多子冲突：**后写覆盖**；作者应把间距放在段落节点）。 | F9 症状面；POI API 面 |
| **B1-C4** | **Run 键 → `XWPFRun`：** 附着于正在写出的 text/textRun（或段落级 `directFormat` 作为该段 runs 的默认字体覆盖，子节点显式值优先）。须补齐今日「校验有、writer 未读 `directFormat`」的接线缺口。 | `applyDirectFormatIfPresent` 已有字体实现；接线缺失属本叶 |
| **B1-C5** | **数值单位（作者 JSON → POI）：** (a) `spacingBefore` / `spacingAfter` / `firstLineIndent` / `leftIndent` / `rightIndent`：JSON **非负 number**，单位 **point（pt）**；writer 转为 twips（×20）调用 POI `setSpacing*` / `setIndentation*`。(b) `lineSpacing`：JSON **正 number**，表示 **行距倍数**（1.0=单倍，1.5=1.5 倍），经 `setSpacingBetween(value, LineSpacingRule.AUTO)`（或等价）。(c) `fontSize`：保持既有 **正 int pt** → `setFontSize`。 | POI 文档；银行信函常用 pt/倍数；无既有冲突 SoT |
| **B1-C6** | **缺省键：** 未出现或 JSON null 的键 → **不写**对应段落/run 属性（继承 `styleRef` / 母版）。不得写入 0 覆盖母版，除非作者显式提供合法 0（缩进/间距允许 0；`lineSpacing` / `fontSize` 须 > 0）。 | 继承诚实 |
| **B1-C7** | **非法值 fail-closed：** 非数值、NaN、负数（间距/缩进）、`lineSpacing`/`fontSize` ≤ 0、非对象 `directFormat` → 绑定/保真校验 **blocker**（稳定码：优先复用/扩展既有 fidelity 族，如新增 `DIRECT_FORMAT_INVALID_VALUE` **或**在实现中文档化所选码；**禁止**校验通过却在 writer 静默丢弃）。发布闸门对 blocker fail-closed（既有 PublishGate）。 | 「cannot render reliably」= ADR-0019 blocker；禁半忽略 |
| **B1-C8** | **白名单外 / 全局版式：** 行为不变——`DIRECT_FORMAT_OUT_OF_WHITELIST` / `DIRECT_FORMAT_GLOBAL_LAYOUT`。 | P18-T03 既有 |
| **B1-C9** | **适用路径：** 凡经结构化 DOCX writer 的 **preview 装配、runtime sync/batch generate、regenerate SPECIMEN 装配** 必须同规则应用（同源 writer）。明文非结构化锚点路径 **不**新增段落 `directFormat` 语义（本叶不扩展）。 | 关闭作者信任缺口于结构化面 |
| **B1-C10** | **与 CE-K02：** 无 `directFormat` 字体时仍省略硬编码 Calibri；有白名单字体 `directFormat` 时显式设 run（K02-014 保持）。段落间距不强制改 styles.xml catalog。 | ce-k02 |
| **B1-C11** | **可观测证据（强制）：** 自动化测试：结构化段落带已知 `spacingBefore`/`spacingAfter`/`lineSpacing`/`leftIndent`（及至少一缩进变体）、生成 DOCX 后用 POI 读回（或 XPath `w:spacing` / `w:ind`）断言与 B1-C5 换算一致（允许实现文档化的整数舍入）。至少一用例覆盖「仅字体」不破坏段落缺省；至少一用例覆盖「仅间距」无字体键。 | IBL-B1 验收 |
| **B1-C12** | **FE：** `frontend_ui_in_scope=false`。作者 UI 控件若已暴露这些键则受益于诚实渲染；本叶不改 Vue。 | handoff |
| **B1-C13** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；行为变更渲染验收面 → Stage 5/10 queued Docker 按管线；architecture review。E2E/UIUX N/A。 | delivery constitution |
| **B1-C14** | **完成边界：** B1 Done ≠ Wave B 完备；≠ go-live；#3b/#5a 保持 CONDITIONAL；≠ B2/B3/B7。 | 队列政策 |
| **B1-C15** | **备选路径纪律：** 实现中若发现某段落键在 POI 5.5 **无法**可靠落盘，**不得**保留该键于白名单并静默忽略；须：(1) 从 `DirectFormatRules` 移除；(2) 发布校验对该键 fail-closed（`DIRECT_FORMAT_OUT_OF_WHITELIST` 或明确「unsupported」blocker）；(3) 同步 ADR-0019 / domain §2.6.3 / 本文件 Amendment。**默认预期仍是六键全部可应用**——备选仅应急，且须在实现笔记中说明证据。 | 计划 OR 条款；禁静默 |

### 4.2 文档化备选（未选用为主路径）

| 备选 | 说明 |
| --- | --- |
| **Narrow whitelist** | 将白名单收窄为 writer 已支持的字体三键，并对间距/缩进键在发布时 fail-closed。满足「无静默忽略」，但削弱 ADR-0019 已确认的作者能力。**本叶否决为默认**（B1-C1）；仅在 B1-C15 应急条件下按键启用。 |

### 4.3 Open questions

**无阻塞项。** 单位约定（B1-C5）与附着点（B1-C3）由本 BDD 确认，供 TDD Red 使用；实现可选稳定 fidelity 码名（B1-C7）属实现细节，不阻塞 readiness。

```text
open_questions: []
```

---

## 5. Trigger / Preconditions

### Trigger

- 结构化内容树节点含白名单 `directFormat`（段落与/或 run 键）。  
- Preview / runtime generate / regenerate 走结构化 DOCX writer。  
- 绑定校验 / 发布闸门评估 `directFormat`。

### Preconditions

- 模板绑定使用结构化节点矩阵；母版 catalog / `styleRef` 规则按 CE-K02。  
- Apache POI ooxml 5.5.x 在 classpath（项目 pin）。  
- 作者/调用方已通过既有授权（本叶不放宽）。

---

## 6. Primary journey

1. 作者在段落节点设置 `directFormat`: `{ "spacingBefore": 12, "spacingAfter": 6, "lineSpacing": 1.5, "leftIndent": 24 }`（pt / 倍数，见 B1-C5）。  
2. 绑定校验通过（键在白名单、值合法）。  
3. 发布通过（无其它 blocker）。  
4. Runtime/preview 生成 DOCX。  
5. Writer 将间距/缩进写入该段 `w:pPr`（经 POI）。  
6. 打开/解析 DOCX：段前 12pt、段后 6pt、1.5 倍行距、左缩进 24pt 可观测。  
7. 同段另测 `fontFamily`/`fontSize`/`textColor` 仍落到 run。

---

## 7. System responses

### 7.1 Success

| 形态 | 响应 |
| --- | --- |
| Preview / generate / regenerate（结构化） | 成功制品 DOCX 含对应段落间距/缩进与 run 字体 |
| 绑定校验（合法白名单） | 无 `DIRECT_FORMAT_*` blocker |
| POI 读回 | 与 B1-C5 换算一致（舍入按实现钉死） |

### 7.2 Fail-closed

| 条件 | 行为 |
| --- | --- |
| 白名单外键 | `DIRECT_FORMAT_OUT_OF_WHITELIST` blocker → 发布失败 |
| 全局版式键 | `DIRECT_FORMAT_GLOBAL_LAYOUT` blocker → 发布失败 |
| 非法数值（B1-C7） | fidelity blocker → 发布失败；**不**生成「丢弃该键」的成功件 |
| 未授权 / 其它既有错误 | 既有 401/403/4xx；本叶不放宽 |

---

## 8. Acceptance scenarios（Given / When / Then）

### BDD-IBL-B1-001 — spacingBefore / spacingAfter 落入 DOCX（F9 主验收）

**Given** 结构化 `paragraph` 节点 `directFormat` 含 `spacingBefore=12`、`spacingAfter=6`（pt）  
**And** 其余校验通过  
**When** 经结构化 writer 生成 DOCX（preview 或 runtime 同源路径）  
**Then** 用 POI 打开首段（或目标段）  
**And** `getSpacingBefore()`（或等价 OOXML）对应 **240** twips（12×20）  
**And** `getSpacingAfter()` 对应 **120** twips（6×20）  
**And** **不**因未实现而等于缺省未设置状态（证明非静默忽略）

### BDD-IBL-B1-002 — lineSpacing 倍数生效

**Given** 段落 `directFormat.lineSpacing=1.5`  
**When** 生成 DOCX  
**Then** 段落行距为 1.5 倍（`getSpacingBetween()` ≈ 1.5 且 rule 为 AUTO，或 OOXML `w:spacing/@w:line` 与 AUTO 语义一致）

### BDD-IBL-B1-003 — leftIndent / firstLineIndent / rightIndent

**Given** 段落 `directFormat` 含 `leftIndent=24`、`firstLineIndent=12`、`rightIndent=6`（pt）  
**When** 生成 DOCX  
**Then** POI `getIndentationLeft/FirstLine/Right`（或等价）分别为 480 / 240 / 120 twips

### BDD-IBL-B1-004 — 字体三键不回归

**Given** 段落或 textRun `directFormat` 含 `fontFamily`、`fontSize`、`textColor`（合法值）  
**When** 生成 DOCX  
**Then** 对应 run 的字体/字号/颜色可观测等于所设值（去 `#` 后的 color）  
**And** 不强制改写无 directFormat 的其它段落为硬编码 Calibri（CE-K02）

### BDD-IBL-B1-005 — 仅间距键、无字体键

**Given** `directFormat` **仅**含 `spacingBefore`（合法）  
**When** 生成 DOCX  
**Then** 间距生效  
**And** 不因缺失字体键而失败或静默丢弃间距

### BDD-IBL-B1-006 — 缺省键不覆盖母版

**Given** 段落有 `styleRef`，`directFormat` **省略**所有间距/缩进键  
**When** 生成 DOCX  
**Then** writer **不**强制写入 spacing/indent 覆盖（保持样式/母版继承；测试断言未设置或与无-directFormat 对照件一致）

### BDD-IBL-B1-007 — 非法间距值 → 发布 fail-closed

**Given** `directFormat.spacingBefore` 为负数、或非数值字符串  
**When** 绑定/保真校验（及发布闸门）  
**Then** 产生 fidelity **blocker**（B1-C7 码）  
**And** 发布 **失败**  
**And** **不**产生「校验绿但生成丢弃该键」的成功路径

### BDD-IBL-B1-008 — 白名单外键仍阻断（回归）

**Given** `directFormat` 含 `fontWeight`（或其它非白名单键）  
**When** 校验  
**Then** blocker `DIRECT_FORMAT_OUT_OF_WHITELIST`  
**And** 发布失败

### BDD-IBL-B1-009 — 全局版式键仍阻断（回归）

**Given** `directFormat` 含 `pageMarginTop`  
**When** 校验  
**Then** blocker `DIRECT_FORMAT_GLOBAL_LAYOUT`  
**And** 发布失败

### BDD-IBL-B1-010 — Runtime 与 Preview 同源应用

**Given** 同一已发布结构化内容含段落间距 `directFormat`  
**When** 分别走 preview 装配与 runtime generate  
**Then** 两路径 DOCX 目标段的间距/缩进 POI 读回一致（同源 writer）

### BDD-IBL-B1-011 — 完成边界：非 go-live / 非 #3b/#5a / 非 B2–B3–B7

**Given** 本叶测试与文档更新完成  
**When** 声称切片状态  
**Then** 可关闭 **IBL-B1 / F9** 行为缺口  
**And** **不**宣称 IBL 程序 Done 或 Wave B 全部 Done  
**And** **不**翻转 checklist **#3b** / **#5a**  
**And** **不**交付 B2/B3/B7 验收面

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| `directFormat` 缺失或 null | 忽略；纯 styleRef/母版 |
| `directFormat: {}` | 无属性写入 |
| 同段多子携带冲突段落键 | 后写覆盖（B1-C3） |
| 表格单元格内段落 | 若 writer 为单元格创建 `XWPFParagraph`，同样应用（同源 `StyleSupport`） |
| 应急无法落盘某键 | B1-C15：移出白名单 + fail-closed；禁止静默 |
| FE 未改控件 | 允许；后端诚实即可 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| POI 读回 | `getSpacingBefore/After`、`getSpacingBetween`、`getIndentation*` |
| OOXML（可选补充） | `w:pPr/w:spacing`、`w:ind` XPath |
| 单元/集成测试 | `StructuredContentDocx*Test` 或等价 Red→Green |
| 发布 blocker | 非法值 / 白名单外用例 |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` GREEN |
| Deploy | 管线要求时 `docker-deploy-queue` 证据 |

---

## 11. Traceability

| 源 | 关系 |
| --- | --- |
| IBL program F9 / IBL-B1 | 本叶关闭 |
| Task Master **#113** | 交付叶 |
| ADR-0019 有限直接格式白名单 | 保持集合；本叶落实 writer |
| domain-model §2.6.3 | 同白名单 |
| CE-K02 | 字体继承 / 显式 directFormat |
| P18-T03 | 历史校验落地；本叶补渲染诚实性 |
| Checklist #3b/#5a | **不**由本叶翻转 |
| IBL-B2 / B3 / B7 | **OUT** |

---

## 12. BDD readiness

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-b1-direct-format-spacing.md
task_ids: ["113", "IBL-B1"]
frontend_ui_in_scope: false
confirmed_path: apply_paragraph_spacing_via_poi
documented_alternative: narrow_whitelist_fail_closed (not selected; B1-C15 escape only)
next: plan-orchestrator → rendering-engineer (TDD Red on BDD-IBL-B1-001…)
```

**Handoff：** Spec `ready`。实现须先写失败 POI 断言（BDD-IBL-B1-001…003 至少），再接线 `directFormat` 并扩展 `StructuredContentDocxStyleSupport` 段落应用。
