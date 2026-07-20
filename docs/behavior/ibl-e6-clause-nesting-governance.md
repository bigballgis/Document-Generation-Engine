# BDD 行为规格：IBL-E6 — Clause nesting governance（F28）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-E6` |
| **编写日期** | 2026-07-20 |
| **程序 / 队列** | IBL Wave E · **IBL-E6** / F28（clause nesting / module graph + where-used depth） |
| **Slice** | `ibl-e6-clause-nesting-governance` |
| **Branch** | `feat/ibl-e6-clause-nesting-governance` |
| **Worktree** | `D:/working/DGE-ibl-e6-clause-nesting-governance` |
| **Placement** | ISOLATED |
| **Base** | `65613c4b`（origin/main；IBL-E5 / #132 Done） |
| **Task Master** | **#133** IBL-E6 — Batch Recommendation **solo**；`member_task_ids: ["133"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-e6-clause-nesting-governance`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；[ADR-0067 Accepted](../adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md)（Decision = E6-C*；OpenAPI synced；Accepted ≠ impl Done）；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F28 / IBL-E6 验收；CE-G05 where-used [ce-g05-annual-review-fts.md](./ce-g05-annual-review-fts.md)（G05-C13…C15 — 本叶**扩展**深度）；domain §2.6.10 / §2.9.2 `contentModuleRef` 嵌套图；F1 [core-fortress-f1-rendering-correctness.md](./core-fortress-f1-rendering-correctness.md) 空 pin fail-closed；嵌套边界同构 [ComputeDslLimits](../../backend/src/main/java/com/bank/docgen/sharedkernel/document/compute/ComputeDslLimits.java) `MAX_NESTING_DEPTH=8`；permission-matrix §5.1 条款目录浏览 / where-used |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（owner = **backend-engineer**；API / 治理 / 门禁优先；既有 where-used 面板可消费扩展字段但**非** Done 条件；E2E/UIUX **N/A**） |

**完成声明约束：** 本叶关闭 F28——条款嵌套成为**可治理模块图**；嵌套深度受控；where-used 报告深度引用；环路 fail-closed。**不**交付 RTL（IBL-E7 / #134）。**SPECIMEN 水印不得在本叶移除**（PD-6 **OUT**）。**PD-7** 授权字体 **OUT**。**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 go-live / Wave E / IBL 程序 Done；**禁止**激活 IBL-E7 / #119；**禁止**发明 Word / SPECIMEN / PD 新边界。E1–E5 语义**正交不改写**。

```
bdd_readiness: ready
frontend_ui_in_scope: false
open_questions: []
owning_doc: docs/behavior/ibl-e6-clause-nesting-governance.md
task_ids: ["133"]
adr_status: Accepted — docs/adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md (Decision = E6-C*; OpenAPI/domain/indexes synced; Accepted ≠ E6 impl Done)
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["133"]
  proposed_slice_id: ibl-e6-clause-nesting-governance
  shared_acceptance_surface: >
    Nesting depth governed; where-used reports deep references;
    cycles fail-closed (module graph + governance)
  vetoes_applied:
    - IBL-E7/#134
    - IBL-B7/#119
    - umbrella-106
    - PD-6-specimen-removal
    - PD-7-licensed-fonts
    - checklist-3b-5a-go
    - frontend-nesting-ui-required
    - wave-e-done-claim
  evidence_amortization: mvn verify + queued docker (FE gates only if incidental OpenAPI client types; no Playwright mandate)
```

| IN（本叶） | OUT（明确禁止 / 后续叶） |
| --- | --- |
| CM↔CM 嵌套图治理（由 `contentModuleRef` 边派生/维护） | **IBL-E7** RTL / bidirectional spike |
| 嵌套深度硬上限 + 写路径 / 发布门禁 fail-closed | **#119** Word host |
| 环路检测 fail-closed（自环、互环、间接环） | **PD-6** 去 SPECIMEN；**PD-7** 字体 |
| where-used **深度**引用（扩展 CE-G05；含嵌套路径可观测字段） | 管理 UI 嵌套图编辑器作为 Done 条件 |
| 传递钉扎完整性（发布硬门禁；对齐 F1 空 pin） | 翻转 **#3b/#5a**；go-live；宣称 Wave E Done |
| 权限复用条款目录浏览 + `authorContentModules`（无新 capability） | 改写 E1 locale / E2 inclusion / E3 矩阵 / E4 brand / E5 effectiveFrom·bulk |
| Gates：`mvn verify` + queued deploy evidence；**无**强制 FE E2E | 发明新 PD 边界 |

---

## 1. 概述

### 1.1 问题（F28 — 现状证据）

| 发现 | 证据 |
| --- | --- |
| 条款套条款可出现在 `content_structure_json` 的 `contentModuleRef` 节点，但**无**受治理的 CM↔CM 模块图 | domain §2.6.10「`contentModuleRef` 递归展开」；`ContentModuleService.validateContentStructureJson` 仅非空校验 |
| where-used 只扫模板直连钉扎，**遗漏**「模板钉 A、A 结构嵌套 B」时对 B 的深度命中 | `ContentModuleWhereUsedService` ← `template_content_module_reference` only；CE-G05 G05-C13「不要求扫描全部 binding JSON」 |
| 渲染展开按 `referenceKey` 查模板 pin map；嵌套子条款若未钉扎 → F1 空结构失败，但**写路径不预先治理**深度/环 | `StructuredContentDocxExpandSupport.expandContentModule`；F1-A2 |
| 程序验收已确认：Nesting depth governed；where-used reports deep references；cycles fail-closed | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) §7 IBL-E6 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **E6-S1 Module graph** | 从 CM 版本 `content_structure_json` 中的 `contentModuleRef` 维护有向嵌套边；图为治理/where-used/门禁权威 |
| **E6-S2 Depth bound** | 嵌套深度硬上限；超限 fail-closed（写路径 + 发布门禁） |
| **E6-S3 Cycle fail-closed** | 自环 / 互环 / 间接环一律拒绝（写路径 + 发布门禁 + 渲染不得死循环） |
| **E6-S4 Deep where-used** | 扩展 `GET …/where-used`：报告直连 + 经嵌套传递引用到目标模块的模板（及可观测嵌套摘要） |
| **E6-S5 Transitive pins** | 模板发布时，直接钉扎 CM 的嵌套闭包所需 `referenceKey` 必须齐全且可解析 |
| **E6-S6 API-first** | 无新 capability；无强制管理 UI；`frontend_ui_in_scope=false` |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **条款作者** | `authorContentModules` 组范围 | 写入含 `contentModuleRef` 的 CM 结构；承受深度/环校验 |
| **可浏览条款者** | 矩阵 §5.1 目录浏览（含 `TEMPLATE_APPROVER`；**不含** `TEMPLATE_TESTER`） | 调用深度 where-used |
| **模板编排人员** | `authorTemplates` | 钉扎引用；publish-gate 见嵌套相关硬项 |
| **测试员** | `TEMPLATE_TESTER` | where-used **403**（CE-G05 不变） |
| **系统** | 嵌套图服务 + where-used + publish-gate + 渲染展开 | Fail-closed；无正文泄漏 |

---

## 3. Goal

1. 关闭 F28：条款嵌套成为**可治理模块图**（非仅渲染时偶然展开）。  
2. 嵌套深度受硬上限治理；超限不可保存 / 不可发布。  
3. 环路 fail-closed（写、发布、渲染）。  
4. where-used 报告**深度**引用，修复「只见直连钉扎」缺口；授权边界与 CE-G05 一致。  
5. 传递钉扎在发布时完整，避免嵌套子条款静默缺失。  
6. API-first（`frontend_ui_in_scope=false`）。  
7. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave E Done；E7 / Word / SPECIMEN / PD-6/7 **OUT**。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（E6-C* — 保守银行级默认 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **E6-C1** | **嵌套机制 = 结构化 `contentModuleRef`：** CM 版本 `content_structure_json` 内的 `contentModuleRef` 节点构成 CM↔CM 嵌套边。模板侧 `template_content_module_reference` 仍为模板→CM **钉扎**（正交）。本叶不发明第二种嵌套语法。 | domain §2.6.10；既有节点矩阵 |
| **E6-C2** | **治理图权威：** 在 CM 版本结构**写入成功路径**同步维护有向嵌套边（实现可为显式边表或等价可查询投影；ADR 定存储）。图用于深度/环校验、深度 where-used、发布传递钉扎检查。**禁止**仅依赖运行时偶然展开而无写路径治理。 | F28「not a governed module graph」 |
| **E6-C3** | **深度定义：** 自被保存的 CM 版本根出发，沿 `contentModuleRef` 边的**最长简单路径边数** = nesting depth。无嵌套边 → depth `0`。 | 图论最小定义；可测 |
| **E6-C4** | **深度上限 = 8：** `nestingDepth > 8` → fail-closed。数值同构平台已确认的嵌套硬界 `ComputeDslLimits.MAX_NESTING_DEPTH = 8` / `MAX_DEPENDENCY_DEPTH = 8`（CE-K03 / ADR-0056）。稳定错误码建议 `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED`（OpenAPI 固定一种）。 | 程序「Nesting depth governed」+ 平台先例；非新 PD |
| **E6-C5** | **环路 fail-closed：** 自环、互环、间接环（含写入后与既有图形成的环）→ **拒绝写入**（422）且发布门禁 FAIL。稳定码建议 `CONTENT_MODULE_NESTING_CYCLE`。渲染路径遇环 → 结构化失败（不得 StackOverflow / 静默截断）。 | 程序「cycles fail-closed」；平台 fail-closed |
| **E6-C6** | **写路径触发点：** CM 版本 create / update `contentStructureJson`（及任何等价写结构 API）在持久化前跑深度+环+目标可解析校验。非法 → **零**结构写入。 | 银行写时治理；对齐 `validateContentStructureJson` 扩展点 |
| **E6-C7** | **嵌套目标可解析：** 每个 `contentModuleRef` 必须能解析到授权可见的目标内容模块（`referenceKey` 与模板钉扎键同构：规范化大写；解析规则 ADR/OpenAPI 固定一种——优先既有 `referenceKey`→模块/版本解析惯例）。不可解析 / 跨组不可见 → 422（建议 `CONTENT_MODULE_NESTING_TARGET_UNRESOLVED`）。 | F1 解析失败同构；GroupAccess fail-closed |
| **E6-C8** | **深度 where-used：** 扩展既有 `GET /api/management/v1/content-modules/{moduleId}/where-used`。除 CE-G05 直连模板外，**必须**包含经嵌套闭包间接使用该模块的模板。每行可观测：`referenceKind` = `DIRECT` \| `NESTED`；`NESTED` 时提供 `nestingDepth` 与非敏感路径摘要（如 `moduleCode` 链，**不含**条款正文）。分页/排序保持目录惯例；响应仍**不含**条款全文。 | 程序「where-used reports deep references」；G05-C13/C15 |
| **E6-C9** | **where-used 授权不变：** 与 §5.1 / G05-C14 相同；`TEMPLATE_TESTER` → **403**；不得返回调用方不可见模板行；跨组 fail-closed。无新 capability bit。 | permission-matrix；CE-G05 |
| **E6-C10** | **生命周期 impact 深度：** `ContentModuleLifecycleImpactService`（及等价 impact preview）对引用模板计数须与深度 where-used **同闭包**（至少：受影响模板集合含 NESTED）。 | 同一 F28 缺口；避免 impact/where-used 分叉 |
| **E6-C11** | **传递钉扎（发布硬门禁）：** 评估 publish 时，对每个直接钉扎的 CM 版本走嵌套闭包：闭包内每个 `contentModuleRef.referenceKey` 必须存在于该模板版本的 pins 且 pinned 结构非空可解析。缺失 → 硬项 FAIL：`PublishGateCheckCode.CONTENT_MODULE_NESTING_UNPINNED`（**禁止**复用 `CONTENT_MODULE_REFERENCES`；ADR-0067 / OpenAPI 已锁定）。对齐 F1 空 pin 不得静默。 | domain 递归展开 + F1-A2；「governed」发布面 |
| **E6-C12** | **发布亦拦环/超深：** 即使存量脏数据绕过旧写路径，publish-gate 对钉扎闭包仍须 FAIL `CONTENT_MODULE_NESTING_CYCLE` / `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED`（或汇总进上述硬项明细）。 | defense-in-depth；fail-closed |
| **E6-C13** | **运行期：** 已发布锁定版本不因本叶新规则被「回溯改写」pins；但生成/预览若仍遇环或缺失嵌套结构 → **显式失败**（不得死循环/静默省略）。新草稿/新发布走 E6-C6/C11/C12。 | 钉扎不可变 + F1 fail-closed |
| **E6-C14** | **权限：** 无新角色 / capability。结构写 = `authorContentModules`；where-used = 目录浏览边界；publish-gate = 既有模板编排边界。 | E5/G05 模式 |
| **E6-C15** | **Frontend：** **`frontend_ui_in_scope=false`**。既有 CE-G05 where-used UI 可展示新字段属 bonus，**非** Done 条件；不强制嵌套图可视化。 | owner = backend-engineer；mission |
| **E6-C16** | **审计：** 嵌套校验失败不必强制写业务审计（422 即可）；结构成功保存若变更嵌套边，建议管理审计摘要含边变更计数（可选最小：依赖既有 CM 版本更新审计）。禁止正文/variables。 | 审计卫生；不过度发明 |
| **E6-C17** | **E1–E5 正交：** 不改 locale、inclusion rules、审批矩阵、DocumentBrand、`effectiveFrom`/bulk-repin 语义。 | Wave E 兄弟叶 Done |
| **E6-C18** | **SPECIMEN / PD-6 / PD-7 / Word：** 不改变。 | 程序 §8；vetoes |
| **E6-C19** | **ADR：** [ADR-0067](../adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md) **Accepted**（2026-07-20；Decision = 本节 E6-C*；OpenAPI/domain/indexes synced）。Accepted ≠ E6 impl Done。 | 程序「ADR + BDD per leaf」 |
| **E6-C20** | **门禁：** BE `mvn verify`；queued Docker deploy evidence。无强制 Playwright。若 OpenAPI 生成前端类型，FE type-check/test 按需绿，仍非 UI 交付。 | delivery constitution + API-first |
| **E6-C21** | **完成边界：** E6 Done ≠ Wave E Done；≠ go-live；#3b/#5a 保持 CONDITIONAL；E7 / #119 不激活；**不**翻转 #3b/#5a。 | 队列政策 |

### 4.2 明确非本叶确认（禁止当作已定产品事实）

| 项 | 状态 |
| --- | --- |
| RTL / 双向文稿 | **IBL-E7** |
| Word 测量宿主 | **#119** — **OUT** |
| SPECIMEN 移除 / 授权字体 | PD-6/7 — **OUT** |
| 管理 UI 嵌套图编辑器 / 可视化作为 Done | **拒绝**（E6-C15） |
| 深度上限改为可配置运营参数（本叶） | **拒绝**（锁定 8；配置化属后续） |
| 跨组静默嵌套 / 绕过 GroupAccess | **拒绝** |
| 用扫描全部 template binding JSON 替代模块图 | **拒绝**（延续 G05-C13：图/边投影权威） |

### 4.3 ADR / 用户确认

| 问题 | 结论 |
| --- | --- |
| 是否还需用户再确认「要不要做嵌套治理」？ | **否** — F28 / IBL-E6 程序验收（PD-gate Wave E 已确认边界；本叶为 nesting governance） |
| 深度上限是否需产品二选一？ | **否（BDD ready）** — 锁定 **8**，引用平台已确认 `ComputeDslLimits` 同构硬界 |
| where-used 是否仅模板、是否含父 CM？ | **模板行必含深度命中**（E6-C8）；父 CM 可作为路径摘要字段出现，**不**另开必选 UI |
| ADR 何时 Accepted？ | **已 Accepted**（doc-keeper stage 3，2026-07-20；Decision = E6-C*；OpenAPI 稳定码已锁定） |

---

## 5. Preconditions

- 操作者具备对应写/读权限（`authorContentModules` 或条款目录浏览 / `authorTemplates`）。  
- #133 为本交付叶（orchestrator 激活本切片）。  
- CE-G05 where-used 端点已存在；本叶扩展语义。  
- CM 版本可写 `contentStructureJson`；模板可钉扎 CM 引用。  
- 结构化节点类型已含 `contentModuleRef`。

---

## 6. Trigger

- 条款作者保存含 `contentModuleRef` 的 CM 版本结构。  
- 授权用户 `GET …/content-modules/{moduleId}/where-used`。  
- 编排人员评估/执行模板 publish（publish-gate）。  
- 预览/运行时渲染展开嵌套 `contentModuleRef`。

---

## 7. Primary journey

1. 条款作者创建/编辑模块 **Child**（无嵌套）并批准。  
2. 作者在模块 **Parent** 草稿结构中加入 `contentModuleRef` → Child（depth=1，无环）→ 保存成功；嵌套边入图。  
3. 模板草稿仅钉扎 Parent，并补齐 Child 的 pin（传递钉扎）→ publish-gate 嵌套项 PASS（其它硬项亦满足时可发布）。  
4. 查询 Child 的 where-used → 返回该模板，且 `referenceKind=NESTED`（或等价可观测字段）。  
5. 反例：Parent 结构再引用回 Child 形成环 → 保存 **422** `CONTENT_MODULE_NESTING_CYCLE`；where-used/发布不得带病放行。

---

## 8. System responses（成功路径）

- 合法无环且 depth≤8 的结构保存成功；嵌套边与版本一致。  
- where-used 同时返回 DIRECT 与 NESTED 模板行（授权范围内）；无正文。  
- publish-gate 在传递钉扎完整且无环/未超深时，本叶硬项 PASS。  
- 渲染按钉扎结构递归展开，结果确定性。

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-E6-001 — Acyclic depth-1 nest saves

**Given** 模块 Child 可解析；作者对 Parent 草稿写入单一 `contentModuleRef` → Child（无环）  
**When** 保存 Parent 版本 `contentStructureJson`  
**Then** **200/成功**；嵌套边 Parent→Child 可查询；nestingDepth = 1

### BDD-IBL-E6-002 — Self-cycle rejected on write

**Given** 模块 M 草稿结构含 `contentModuleRef` 指向 M 自身  
**When** 保存  
**Then** **422** `CONTENT_MODULE_NESTING_CYCLE`（或 OpenAPI 固定等价码）；结构**未**更新

### BDD-IBL-E6-003 — Mutual cycle rejected on write

**Given** A 已嵌套 B；作者编辑 B 使其嵌套 A  
**When** 保存 B  
**Then** **422** `CONTENT_MODULE_NESTING_CYCLE`；B 结构未更新；A 边保持

### BDD-IBL-E6-004 — Depth > 8 rejected on write

**Given** 一条无环链将使被保存根的 nestingDepth = 9  
**When** 保存该根版本结构  
**Then** **422** `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED`；结构未更新

### BDD-IBL-E6-005 — Depth = 8 accepted

**Given** 无环链 nestingDepth = 8  
**When** 保存根结构  
**Then** 成功；图深度记录为 8

### BDD-IBL-E6-006 — where-used DIRECT unchanged

**Given** 模板 T 直连钉扎模块 M（无嵌套）  
**When** `GET …/content-modules/{M}/where-used`（授权会话）  
**Then** 含 T 且 `referenceKind=DIRECT`（或缺省等价直连语义）

### BDD-IBL-E6-007 — where-used reports deep NESTED template

**Given** 模板 T 钉扎 Parent；Parent 结构嵌套 Child；T 亦钉扎 Child（传递完整）  
**When** `GET …/where-used` for Child  
**Then** 响应含 T 且 `referenceKind=NESTED`；含 `nestingDepth` ≥ 1；**不含**条款全文

### BDD-IBL-E6-008 — where-used deep without requiring binding JSON scan

**Given** 嵌套边已由图维护；模板 binding JSON **未**另存 Child id  
**When** where-used for Child  
**Then** 仍能通过模块图 + 模板对 Parent 的钉扎命中 T（权威=图/边，非全量 binding 扫描）

### BDD-IBL-E6-009 — where-used auth fail-closed

**Given** 模板 T 在组 B；会话仅可访问组 A  
**When** where-used  
**Then** 不返回 T；无权模块 → 403/404 惯例；`TEMPLATE_TESTER` → **403**

### BDD-IBL-E6-010 — lifecycle impact includes nested templates

**Given** 同 BDD-IBL-E6-007 布置  
**When** 对 Child 调用 lifecycle impact preview  
**Then** 受影响模板集合含 T（与深度 where-used 闭包一致）

### BDD-IBL-E6-011 — publish fails on nesting cycle in pinned closure

**Given** 模板草稿钉扎含环的 CM 版本闭包（存量或异常数据）  
**When** 评估 publish-gate / publish  
**Then** 硬项 FAIL（`CONTENT_MODULE_NESTING_CYCLE` 或明细等价）；发布拒绝

### BDD-IBL-E6-012 — publish fails on depth exceed in pinned closure

**Given** 钉扎闭包 nestingDepth > 8  
**When** publish-gate / publish  
**Then** 硬项 FAIL（`CONTENT_MODULE_NESTING_DEPTH_EXCEEDED` 或明细等价）

### BDD-IBL-E6-013 — publish fails when nested pin missing

**Given** 模板仅钉扎 Parent；Parent 嵌套 Child；Child **未**钉扎  
**When** publish-gate / publish  
**Then** 硬项 FAIL（`CONTENT_MODULE_NESTING_UNPINNED`）；对齐不得空结构发布

### BDD-IBL-E6-014 — publish passes with complete transitive pins

**Given** Parent→Child depth=1 无环；模板钉扎 Parent 与 Child 且结构非空  
**When** publish-gate（其它硬项 PASS）  
**Then** 本叶嵌套相关硬项 PASS

### BDD-IBL-E6-015 — unresolved nest target rejected on write

**Given** `contentModuleRef.referenceKey` 无法解析到可见模块  
**When** 保存 CM 结构  
**Then** **422** `CONTENT_MODULE_NESTING_TARGET_UNRESOLVED`（或 OpenAPI 固定等价码）

### BDD-IBL-E6-016 — render cycle fail-closed

**Given** 预览/生成路径遇到嵌套环（异常夹具）  
**When** 渲染展开  
**Then** 结构化错误（稳定码）；**不**死循环；**不**静默省略嵌套子树

### BDD-IBL-E6-017 — no new capability / API-first

**Given** 本叶交付完成  
**When** 检查 permission-matrix / OpenAPI  
**Then** 无新 capability bit；where-used/结构写复用既有边界；**无**强制新管理 UI 路由作为 Done 条件

### BDD-IBL-E6-018 — OUT boundaries unchanged

**Given** 本叶范围  
**When** 对照程序 vetoes  
**Then** 不移除 SPECIMEN；不触及 RTL/Word/#119；不翻转 #3b/#5a；不宣称 Wave E / go-live Done；不改写 E1–E5 行为

---

## 10. Boundary / exception

| 场景 | 行为 |
| --- | --- |
| depth 0（无 `contentModuleRef`） | 与今日行为兼容；where-used 仅 DIRECT |
| 重复 `referenceKey` 多边 / 多 key 解析到同一 Target | 图去重为单一 Parent→Target 边（保留首个 `referenceKey`）；depth 按最长路径；避免 UNIQUE(parent_version_id, target_module_id) 炸为 500 |
| 畸形 `contentStructureJson`（嵌套写路径解析失败） | **422** `CONTENT_MODULE_NESTING_STRUCTURE_INVALID`；零结构/边写入（fail-closed，不静默空图） |
| 跨组嵌套目标 | fail-closed 不可解析/拒绝（E6-C7） |
| 空 `pinnedStructure` 嵌套子项 | 发布 FAIL（E6-C11）；渲染 F1 码 |
| where-used 无命中 | 200 空页（G05 同） |
| 并发写结构 | 事务内校验；后写仍须通过环/深度（实现选乐观锁或版本号——ADR 可注） |
| 导入包含环/超深 | 导入校验 fail-closed **或**导入后不可发布（至少满足 E6-C12）；不得静默放行可发布脏图 |

---

## 11. Observable evidence

| 表面 | 证据 |
| --- | --- |
| API | CM 结构写 422 码；where-used 含 `DIRECT`/`NESTED`；publish-gate 明细含嵌套硬项 |
| DB / 图 | 嵌套边与版本一致（测试可查 repository 或等价查询） |
| 渲染 | 环夹具失败码；完整钉扎深度展开成功 |
| 审计 | 若实现边变更审计：无正文；可选 |
| Gates | `mvn -B -ntp -f backend/pom.xml verify`；queued `docker-deploy-queue.ps1` 证据 |
| FE/E2E | **N/A**（`frontend_ui_in_scope=false`） |

---

## 12. Traceability

| 源 | 关系 |
| --- | --- |
| [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F28 / IBL-E6 | 程序验收 + 本叶范围 |
| Task Master **#133** | 交付叶 |
| [ce-g05-annual-review-fts.md](./ce-g05-annual-review-fts.md) G05-C13…C15 | where-used 基线；本叶扩展深度 |
| domain §2.6.10 / §2.9.2 | `contentModuleRef`；where-used 直连局限 |
| [core-fortress-f1-rendering-correctness.md](./core-fortress-f1-rendering-correctness.md) F1-A2 | 空 pin fail-closed |
| `ComputeDslLimits.MAX_NESTING_DEPTH=8` | 深度上限同构依据 |
| permission-matrix §5.1 | 目录浏览 / where-used 授权 |
| ADR-0067（Accepted） | Decision = E6-C*；OpenAPI/domain synced；≠ impl Done |
| E1–E5 BDD/ADR | 正交；不改写 |

---

## 13. TDD Red 提示（给 backend-engineer）

优先红测（名称示意）：

1. `ContentModuleNestingValidation_selfCycle_rejected`  
2. `ContentModuleNestingValidation_depthNine_rejected`  
3. `ContentModuleNestingValidation_depthEight_accepted`  
4. `ContentModuleWhereUsedService_nestedTemplate_reported`  
5. `PublishGate_nestedPinMissing_fails`  
6. `PublishGate_nestingCycle_fails`  
7. `ContentModuleLifecycleImpact_includesNestedTemplate`  
8. `StructuredContentExpand_cycle_failsClosed`

---

## 14. Handoff

```text
task_ids: ["133"]
bdd_readiness: ready
placement: ISOLATED
worktree_path: D:/working/DGE-ibl-e6-clause-nesting-governance
branch: feat/ibl-e6-clause-nesting-governance
frontend_ui_in_scope: false
batch_recommendation:
  decision: solo
  member_task_ids: ["133"]
  proposed_slice_id: ibl-e6-clause-nesting-governance
  vetoes_applied: ["IBL-E7/#134", "IBL-B7/#119", "umbrella-106"]
runtime_routing:
  mode: NATIVE_SPECIALIST
  requested_subagent: behavior-spec-author
  actual_subagent: behavior-spec-author
  reason: NONE
next: backend-engineer (TDD Red → impl; ADR-0067 Accepted; OpenAPI/domain synced)
```
