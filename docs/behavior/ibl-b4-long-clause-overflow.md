# BDD 行为规格：IBL-B4 — Long-clause overflow / page-break policy + golden theme 08

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-B4` |
| **编写日期** | 2026-07-19 |
| **程序 / 队列** | IBL Wave B · **IBL-B4** / F13（`ibl-b4-long-clause-overflow`） |
| **Slice** | `ibl-b4-long-clause-overflow` |
| **Branch** | `feat/ibl-b4-long-clause-overflow` |
| **Worktree** | `D:/working/DGE-ibl-b4-long-clause-overflow` |
| **Base** | `3de54cbc`（local `main`，含 B3 Done 文档） |
| **Placement** | ISOLATED |
| **Task Master** | **#116** IBL-B4 — Batch Recommendation **solo**；`member_task_ids: ["116"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-b4-long-clause-overflow`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F13 / IBL-B4；金标骨架 [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md)（`08-long-clause-limits` / K07-016）；包路径 `backend/src/test/resources/golden-corpus/08-long-clause-limits/` |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（无 Vue / Playwright / UIUX 义务） |

**完成声明约束：** 本叶关闭 F13——书面确认长条款溢出策略，并将金标主题 `08-long-clause-limits` 从 `PLACEHOLDER` 升为 **ACTIVE**，带 LibreOffice（LO）PDF 文本断言。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave B / 程序 Done；**禁止**把 IBL-B5（seal geometry）/ B7（Word Path E）并入本叶；**禁止**引入 `PIXEL_*` 视觉断言（PD-2 / K07-C6）。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["116"]
  proposed_slice_id: ibl-b4-long-clause-overflow
  shared_acceptance_surface: >
    Written overflow policy in BDD (paginate/full-retention confirmed);
    theme 08-long-clause-limits ACTIVE with LO golden assertions
  vetoes_applied:
    - b5-seal-geometry
    - b7-word-path-e
    - umbrella-106-registry-only
    - pixel-visual-assertions
  evidence_amortization: mvn verify (LO PDF half may Assumptions.skip when soffice unavailable — K07-C9)
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| 书面溢出策略（三选一裁决，见 §4） | IBL-B5 seal geometry / F14 |
| 金标 `08-long-clause-limits` → **ACTIVE** | IBL-B7 Word Path E / #3b GO |
| DOCX keypath + LO PDF 文本断言（起止标记完整保留） | `PIXEL_*` / 截图金标 |
| 结构化 writer 路径对极限长条款 **完整写出**（无静默截断） | 发明 Word host baselines |
| Gates：`mvn -B -ntp -f backend/pom.xml verify` | Playwright / OA 旅程；翻转 #3b/#5a；go-live |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| 金标包 `08-long-clause-limits` 成熟度 `PLACEHOLDER`；业务断言 `deferred: true`、空列表 | `manifest.json`；`expected/docx-assertions.json`；`expected/pdf-assertions.json` |
| README / manifest 明确推迟「截断 vs 完整保留」至后续切片 | 包内 README；`owningFutureSlice` 未钉死 |
| CE-K07 允许本包保持 PLACEHOLDER，或升 ACTIVE 时**必须**写入与已确认产品策略一致的断言 | [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md) BDD-CE-K07-016 / Q1 |
| IBL-B4 / F13 要求书面溢出策略 + theme 08 ACTIVE + LO golden | IBL program 验收表 |
| 今日结构化 writer **无**「长条款静默截断」产品语义；流程型 DOCX 天然跨页 | `StructuredContentDocxWriter` 等；无 truncate-clause API |
| PDF 断言类型仅 `TEXT_CONTAINS` / `TEXT_NOT_CONTAINS`（禁像素） | `GoldenCorpusAssertionLoader` |
| LO 不可用时 ACTIVE 包 PDF 半段可 `Assumptions.assumeTrue` 跳过；DOCX 半段必须执行 | K07-C9；既有 harness |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **B4-S1 Policy** | 确认产品溢出策略 = **Paginate / full retention**（见 §4）；明确否决 Truncate |
| **B4-S2 Writer honesty** | 合法结构化长条款正文完整写入 DOCX；不得静默截断或省略尾部 |
| **B4-S3 Page-break** | 超页长内容经 Word/LO **自然流式分页**；本叶不要求作者强制 `w:br type=page`，也不以截断冒充分页 |
| **B4-S4 Golden ACTIVE** | `08-long-clause-limits` 升 ACTIVE；`pdfSource=LIBREOFFICE`；非 deferred 断言 |
| **B4-S5 Fail-closed ceiling** | 仅对**已有/文档化硬上限**或不可渲染结构 fail-closed；硬上限**不是**合法长条款的默认响应 |
| **B4-S6 Honesty bounds** | 不宣称 go-live；不翻转 #3b/#5a；OUT B5/B7 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板作者** | 结构化条款/段落可含较长正文（变量展开或静态 textRun） | 期望银行信函条款不被静默截断 |
| **Runtime / Preview 调用方** | 触发生成 → DOCX（及可选 PDF） | 获得完整条款文本 |
| **系统（writer + LO）** | 结构化 DOCX 装配；LibreOffice 转 PDF | 完整保留；自然分页 |
| **CI / `mvn verify`** | 金标 harness | ACTIVE 包断言失败 → verify 红 |
| **（非本片）管理 UI 用户** | — | `frontend_ui_in_scope=false` |

---

## 3. Goal

1. 关闭 F13：书面确认长条款溢出策略，且与实现/金标一致。  
2. 产品行为 = **完整保留 + 自然分页（Paginate）**；**禁止**截断（Truncate）作为合法条款路径。  
3. 激活金标 `08-long-clause-limits`（ACTIVE + LO 文本断言证明起止完整）。  
4. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave B Done；OUT B5/B7。

---

## 4. 已确认决策 vs 非确认

### 4.1 溢出策略三选一（本叶确认 — 产品行为）

| 选项 | 本叶裁决 | 含义 |
| --- | --- | --- |
| **Truncate（截断）** | **否决（REJECTED）** | 不得在合法生成路径上静默截断条款正文、以省略号/裁切冒充完整件，或丢弃尾部仍返回成功制品 |
| **Paginate / full retention（分页 + 完整保留）** | **确认（CONFIRMED）— 主路径** | 合法长条款正文**完整**写入 DOCX；超出单页时由 Word 流式布局 / LibreOffice 分页自然断页；输出可观测地保留条款**起始与结尾**标记 |
| **Fail-closed（硬拒）** | **次级护栏（SECONDARY only）** | **仅**当请求/载荷触及既有平台硬上限、或结构不可渲染时失败并返回稳定错误；**不是**「条款偏长但仍合法」的默认行为 |

**一句话产品行为：** 国际银行信函长条款 → **完整保留并自然分页**；**绝不截断**；仅病理超限 / 不可渲染 → **fail-closed**。

### 4.2 本叶确认决策明细

| ID | 决策 | 依据 |
| --- | --- | --- |
| **B4-C1** | **主路径 = Paginate / full retention。** 结构化装配路径对合法长条款写出**全部**正文；跨页靠自然分页，不靠截断。 | 银行信函法律完整性；IBL north star「fail-closed filling / high-fidelity」；今日 writer 无 truncate 语义；K07-016「完整保留」选项 |
| **B4-C2** | **Truncate 禁止。** 成功制品不得缺少夹具约定的条款尾部标记；不得以静默裁切通过金标。 | 禁静默内容丢失（与 A1/B1 诚实契约同族） |
| **B4-C3** | **Fail-closed 仅次级。** 超出现有 HTTP/载荷/作者侧硬上限或不可渲染 → 既有/稳定错误码失败；本叶**不**为「合法多页条款」新发明软截断阈值。若实现中发现 DoS/OOM 必须新增硬上限，须：**fail-closed（非 truncate）** + 稳定错误码 + 本文件 Amendment 钉死阈值。 | 平台既有 fail-closed 模式；避免未确认阈值 |
| **B4-C4** | **页面断开语义。** 默认**不**要求作者插入显式分页符；超页内容依赖段落流自然断页。本叶不新增「强制 keep-with-next 永不跨页」策略（OUT：版式精细控制属后续/其他片）。 | Word/LO 流式模型；F13「page-break policy」= 允许跨页完整保留 |
| **B4-C5** | **金标包激活。** `08-long-clause-limits`：`maturity=ACTIVE`；`renderMode=STRUCTURED_ASSEMBLE`；`pdfSource=LIBREOFFICE`；`deferred=false`；充实可渲染夹具（非 placeholder 单句）。 | IBL-B4 验收；K07 ACTIVE 规则 |
| **B4-C6** | **夹具形态。** 结构化 BODY 含**单一极限长条款**（或等价长 `paragraph`/`textRun` 链），含稳定子串：起始标记 `LONG_CLAUSE_START` 与结尾标记 `LONG_CLAUSE_END`，中间为足够长度的可重复正文，使在默认信函版式下 LO 转换后**通常 ≥2 页**（页数由补充测试证明；见 B4-C8）。 | 可观测完整保留；证明分页而非截断 |
| **B4-C7** | **金标断言（强制）。** DOCX：`XML_CONTAINS` 同时命中 `LONG_CLAUSE_START` 与 `LONG_CLAUSE_END`（`word/document.xml`）。PDF（soffice 可用时）：`TEXT_CONTAINS` 同时命中两标记。可选：`TEXT_NOT_CONTAINS` 禁止明显截断伪影（如实现曾使用的 `…[TRUNCATED]`——若夹具未使用该串则可不写）。**禁止** `PIXEL_*`。 | harness 允许类型；关闭 F13 |
| **B4-C8** | **补充页数证据（TDD，非金标 JSON 类型）。** 因 harness PDF 类型无 `PAGE_COUNT`，实现须另加（或同包 runner 旁路）PDFBox `getNumberOfPages() >= 2` 断言：**仅当** soffice 可用且 PDF 半段执行时；无 soffice 时与 K07-C9 一致 skip PDF，**不得**因此跳过 DOCX 半段。 | 证明自然分页发生；不扩展禁像素 |
| **B4-C9** | **适用路径。** preview 装配、runtime sync/batch generate、regenerate 结构化装配同源 writer——长条款完整保留规则一致。 | 关闭作者/runtime 信任缺口 |
| **B4-C10** | **FE：** `frontend_ui_in_scope=false`。 | handoff |
| **B4-C11** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；E2E/UIUX N/A。架构审查按管线。Deploy：本叶验收面为 verify 金标；非用户旅程——Stage 5/10 按编排器对渲染变更的既有策略（可 N/A 若编排器判定无验收栈义务）。 | delivery constitution |
| **B4-C12** | **完成边界：** B4 Done ≠ Wave B 完备；≠ go-live；#3b/#5a 保持 CONDITIONAL；≠ B5/B7；≠ IBL-C1 布局度量扩展（C1 可消费本叶 ACTIVE 包，但不在本叶实现 PDFBox 版式矩阵）。 | 队列政策 |
| **B4-C13** | **所有权标注。** manifest / README 将 owning slice 更新为 **IBL-B4 / #116**（取代「future CE-K」模糊指针）。 | 可追溯 |
| **B4-C14** | **与 CE-K07 关系。** 本叶兑现 K07-016「升 ACTIVE 时写入完整保留断言」；不重做骨架枚举门禁。 | ce-k07 |

### 4.3 文档化备选（未选用）

| 备选 | 说明 |
| --- | --- |
| **Truncate-at-N** | 超 N 字符截断并成功返回。**否决**——银行信函不可静默丢条款。 |
| **Fail-closed-as-primary** | 任何超单页或超固定短阈值即拒绝生成。**否决为默认**——合法多页条款是银行信函常态；硬拒仅作病理护栏（B4-C3）。 |

### 4.4 Open questions

**无阻塞项。** 三选一策略已由本 BDD 确认（主路径 Paginate / full retention）。具体夹具重复次数/字数由实现在满足 B4-C6/C8（起止标记 + 通常 ≥2 页）下选定并钉进包内 README；不阻塞 readiness。

```text
open_questions: []
```

---

## 5. Trigger / Preconditions

### Trigger

- 结构化内容含极限长条款正文（金标夹具或等价生产内容）。  
- Preview / runtime / regenerate 走结构化 DOCX writer；PDF 路径经 LibreOffice（金标 `pdfSource=LIBREOFFICE`）。  
- `mvn verify` 执行 golden-corpus ACTIVE runner。

### Preconditions

- 模板绑定为结构化装配（`STRUCTURED_ASSEMBLE`）。  
- 作者/调用方已通过既有授权（本叶不放宽）。  
- LO/`soffice`：PDF 半段按 K07-C9；DOCX 半段始终执行。

---

## 6. Primary journey

1. 金标（或作者）提供含 `LONG_CLAUSE_START` … 长正文 … `LONG_CLAUSE_END` 的结构化 BODY。  
2. Writer 装配 DOCX：**完整**写入起止标记与中间正文。  
3. DOCX 断言：`document.xml` 同时含起止标记。  
4. LibreOffice 将 DOCX 转为 PDF（环境可用时）。  
5. PDF 文本提取同时含起止标记；补充断言页数 ≥ 2。  
6. 验证通过 → theme 08 作为 ACTIVE 回归锁，防止未来引入静默截断。

---

## 7. System responses

### 7.1 Success

| 形态 | 响应 |
| --- | --- |
| 结构化 generate / preview（合法长条款） | 成功 DOCX；正文完整（起止标记均在） |
| LO PDF（soffice 可用） | 成功 PDF；提取文本含起止标记；通常多页 |
| 金标 ACTIVE | verify 绿（PDF 半段无 soffice 时可 skip，DOCX 仍绿） |

### 7.2 Fail-closed（次级）

| 条件 | 行为 |
| --- | --- |
| 超过既有平台硬上限 / 载荷拒绝 | 既有 4xx/稳定错误；**不**返回截断成功件 |
| 不可渲染结构（既有矩阵 blocker） | 发布/生成 fail-closed（既有码） |
| 金标 ACTIVE 断言失败（缺尾部标记等） | 测试失败 → verify 红 |
| 未授权 | 既有 401/403；本叶不放宽 |

### 7.3 明确禁止的「成功」形态

- 返回成功但缺少 `LONG_CLAUSE_END`（或等价尾部）。  
- 以截断标记 / 省略号替代未写出的正文仍标成功。  
- PLACEHOLDER 空断言冒充本叶 Done。

---

## 8. Acceptance scenarios（Given / When / Then）

### BDD-IBL-B4-001 — 策略确认：Paginate / full retention（F13 书面裁决）

**Given** IBL-B4 / F13 要求在 truncate vs paginate vs fail-closed 中确认产品行为  
**When** 读取本 BDD §4.1  
**Then** 主路径确认为 **Paginate / full retention**  
**And** **Truncate** 被明确否决  
**And** **Fail-closed** 仅为病理/硬上限次级护栏

### BDD-IBL-B4-002 — DOCX 完整保留起止标记

**Given** 金标包 `08-long-clause-limits` 夹具含 `LONG_CLAUSE_START` 与 `LONG_CLAUSE_END` 及中间长正文  
**When** 经结构化 writer 装配 DOCX  
**Then** `word/document.xml` `XML_CONTAINS` 两者均成立  
**And** **不**因长度而省略 `LONG_CLAUSE_END`

### BDD-IBL-B4-003 — LO PDF 完整保留起止标记

**Given** 包 `maturity=ACTIVE` 且 `pdfSource=LIBREOFFICE`  
**And** 环境 **有**可用 `soffice`  
**When** harness 转换并断言 PDF  
**Then** `TEXT_CONTAINS` `LONG_CLAUSE_START`  
**And** `TEXT_CONTAINS` `LONG_CLAUSE_END`

### BDD-IBL-B4-004 — 自然分页（补充页数）

**Given** BDD-IBL-B4-003 前置（soffice 可用）  
**When** 用 PDFBox 打开金标 PDF  
**Then** `getNumberOfPages() >= 2`  
**And** 这证明超页内容经分页保留，而非单页截断

### BDD-IBL-B4-005 — 无 soffice 时 DOCX 仍强制

**Given** 环境无可用 LibreOffice  
**When** 运行 golden-corpus ACTIVE 套件  
**Then** PDF 半段按 K07-C9 **skip**（不假绿、不硬红）  
**And** DOCX 半段（BDD-IBL-B4-002）**仍必须执行并通过**

### BDD-IBL-B4-006 — 主题成熟度 ACTIVE

**Given** 包目录 `08-long-clause-limits`  
**When** 扫描 `manifest.json`  
**Then** `maturity` 为 `ACTIVE`（非 `PLACEHOLDER`）  
**And** `expected/*.json` 中 `deferred` 为 `false`  
**And** `assertions` 非空且覆盖起止标记

### BDD-IBL-B4-007 — 禁止截断成功路径（负向）

**Given** 实现若错误地截断长条款并丢掉 `LONG_CLAUSE_END`  
**When** 运行金标 / 等价单元断言  
**Then** 断言 **失败**（verify 红）  
**And** 不得将截断件标为成功交付

### BDD-IBL-B4-008 — 病理超限 fail-closed（次级，不截断）

**Given** 请求触及既有平台硬上限（或本叶 Amendment 钉死的 DoS 上限）  
**When** 生成被拒绝  
**Then** 返回稳定错误（fail-closed）  
**And** **不**返回「已截断正文」的成功制品

### BDD-IBL-B4-009 — 禁像素断言

**Given** 金标 expected 配置  
**When** harness 加载断言  
**Then** **不**包含 `PIXEL_*` / screenshot 类类型  
**And** 仅 DOCX XML keypath/XPath 与 PDF 文本类型

### BDD-IBL-B4-010 — Runtime 与 Preview 同源完整保留

**Given** 同一已发布结构化长条款内容  
**When** 分别走 preview 装配与 runtime generate  
**Then** 两路径 DOCX 均同时包含起止标记（同源 writer）

### BDD-IBL-B4-011 — 完成边界：非 go-live / 非 #3b/#5a / 非 B5–B7

**Given** 本叶测试与文档更新完成  
**When** 声称切片状态  
**Then** 可关闭 **IBL-B4 / F13** 行为缺口  
**And** **不**宣称 IBL 程序 Done 或 Wave B 全部 Done  
**And** **不**翻转 checklist **#3b** / **#5a**  
**And** **不**交付 B5/B7 验收面

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 短条款（单页内） | 完整保留；页数可为 1；金标夹具仍用长文以锁回归 |
| 显式分页符节点 | 本叶不新增/不要求；若矩阵已支持则行为不变 |
| 表格/嵌套条件内长文 | 同源完整保留（不截断）；金标最小夹具可用纯段落 |
| SYNTHETIC PDF | 本包必须 `LIBREOFFICE`——**禁止**用 SYNTHETIC 冒充 LO 验收 |
| 加密 PDF | OUT（07-encrypted-pdf）；本包 `requireEncrypted=false` |
| FE 未改 | 允许 |

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 本文件 §4.1 | 书面策略裁决（Paginate confirmed） |
| 金标 manifest | `maturity=ACTIVE`；`pdfSource=LIBREOFFICE` |
| DOCX assertions | `LONG_CLAUSE_START` / `LONG_CLAUSE_END` |
| PDF assertions | 同上 TEXT_CONTAINS（soffice 时） |
| PDFBox page count | ≥ 2（补充 TDD；soffice 时） |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` GREEN |
| Checklist #3b/#5a | **不**翻转 |

---

## 11. Traceability

| 源 | 关系 |
| --- | --- |
| IBL program F13 / IBL-B4 | 本叶关闭 |
| Task Master **#116** | 交付叶 |
| CE-K07 / BDD-CE-K07-016 | PLACEHOLDER → ACTIVE（完整保留断言） |
| Golden package `08-long-clause-limits` | 实现落点 |
| K07-C6 / PD-2 | 禁像素 |
| K07-C9 | 无 soffice PDF skip |
| Checklist #3b/#5a | **不**由本叶翻转 |
| IBL-B5 / B7 | **OUT** |

---

## 12. BDD readiness

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-b4-long-clause-overflow.md
task_ids: ["116", "IBL-B4"]
frontend_ui_in_scope: false
confirmed_overflow_policy: paginate_full_retention
rejected_overflow_policy: truncate
secondary_overflow_policy: fail_closed_hard_limits_only
golden_theme: 08-long-clause-limits → ACTIVE + LIBREOFFICE
next: plan-orchestrator → rendering-engineer (+ doc-keeper for package docs)
```

**Handoff：** Spec `ready`。实现须先红：金标 ACTIVE + DOCX/PDF 起止标记断言（及 soffice 时页数 ≥ 2），再充实 `08-long-clause-limits` 夹具与 expected；**禁止**截断实现；**禁止**翻转 #3b/#5a。
