# BDD 行为规格：IBL-E2 — Jurisdiction / product / channel composition rule engine（F25 / PD-5）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-E2` |
| **编写日期** | 2026-07-20 |
| **程序 / 队列** | IBL Wave E · **IBL-E2** / F25（`ibl-e2-jurisdiction-rule-engine`） |
| **Slice** | `ibl-e2-jurisdiction-rule-engine` |
| **Branch** | `feat/ibl-e2-jurisdiction-rule-engine` |
| **Worktree** | `D:/working/DGE-ibl-e2-jurisdiction-rule-engine` |
| **Placement** | ISOLATED |
| **Base** | `0a2ee56a`（IBL-E1 Done on main） |
| **Task Master** | **#129** IBL-E2 — Batch Recommendation **solo**；`member_task_ids: ["129"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-e2-jurisdiction-rule-engine`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；[ADR-0063 Accepted](../adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md)（2026-07-20；Decision = E2-C*）；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F25 / IBL-E2 / **PD-5**；CE-K08 [ce-k08-clause-legal-metadata.md](./ce-k08-clause-legal-metadata.md)；E1 正交 [ibl-e1-locale-variant-model.md](./ibl-e1-locale-variant-model.md) / [ADR-0062](../adr/template-lifecycle/0062-locale-variant-template-clause-model.md)；context 基线 [ADR-0013](../adr/api/0013-api-contract-visibility-audit-and-context.md)（Amendment 2026-07-20）；domain §2.9 / §2.9.2；API [contract-outline.md](../api/contract-outline.md)；permission-matrix 既有 `authorTemplates` |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（owners = doc-keeper → **backend-engineer**；API-first 引擎 + 管理 API；编排 UI 配置面为 residual，非本叶 Done 条件；E2E/UIUX **not required**） |

**完成声明约束：** 本叶关闭 F25「无辖区/产品/渠道驱动的组合引擎」——模板版本具备可治理的 **composition inclusion rules**，runtime 按 `context.jurisdiction` / `product` / `channel` **确定性**纳入或排除钉扎内容模块引用，且结果可审计。**SPECIMEN 水印不得在本叶移除**（PD-6 意图 ≠ E2 实现）。**禁止**激活 IBL-E3…E7 / #119；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 go-live / Wave E / IBL 程序 Done。CE-K08 jurisdiction 元数据语义**不**被本叶改写；E1 locale 模型**正交**。

```
bdd_readiness: ready
frontend_ui_in_scope: false
open_questions: []
owning_doc: docs/behavior/ibl-e2-jurisdiction-rule-engine.md
task_ids: ["129"]
adr_status: Accepted — docs/adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md (2026-07-20; Decision = E2-C*; Accepted ≠ E2 impl Done)
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["129"]
  proposed_slice_id: ibl-e2-jurisdiction-rule-engine
  shared_acceptance_surface: >
    Jurisdiction/product/channel composition inclusion engine
    (ADR + deterministic auditable runtime inclusion);
    CE-K08 metadata remains orthogonal legal catalog/expiry
  vetoes_applied:
    - IBL-E3-multistage-approval
    - IBL-E4-entity-brands
    - IBL-E5-effectiveFrom-bulk
    - IBL-E6-nesting-governance
    - IBL-E7-RTL-spike
    - PD-6-specimen-removal
    - PD-7-licensed-fonts
    - IBL-B7-Word-host
    - umbrella-106-registry-only
    - checklist-3b-5a-go
    - frontend-authoring-ui-required
  evidence_amortization: mvn verify + queued docker (FE gates only if incidental API client types; no Playwright mandate)
```

| IN（本叶） | OUT（明确禁止 / 后续叶） |
| --- | --- |
| ADR（建议 0063）**Accepted**；Decision = E2-C* | **PD-6** 去 SPECIMEN / true re-issue |
| Runtime `context` 白名单增加可选 `jurisdiction`、`product`（`channel` 已存在） | **PD-7** 授权字体；**#119** Word |
| 模板版本级 **Composition Inclusion Rule**（结构化匹配，非变量表达式） | **IBL-E3** 多级法务审批 / legal-reviewer |
| Runtime：按规则确定性 INCLUDE/EXCLUDE 钉扎 CM `referenceKey`；审计 inclusion 摘要 | **IBL-E4** 法人品牌；**IBL-E5** bulk re-pin；**IBL-E6** 嵌套图；**IBL-E7** RTL |
| 管理 API：草稿可写 inclusion rules；发布锁定；引用校验门禁 | 管理 UI 规则编辑器（residual；**非** Done 条件） |
| 可选 CE-K08 一致性：双方非空 jurisdiction 不匹配 → fail-closed | 公司辖区/产品枚举目录、LDAP 映射、outbound delivery（PD-1） |
| Gates：`mvn verify` + queued deploy evidence；**无**强制 FE E2E | 翻转 **#3b/#5a**；go-live；宣称 Wave E Done |

---

## 1. 概述

### 1.1 问题（F25）

| 发现 | 证据 |
| --- | --- |
| 无辖区/产品/渠道驱动的组合引擎 | 程序 F25；PD-5 Confirmed 2026-07-19 |
| CE-K08 仅提供 CM **版本**可选 `jurisdiction` + 目录筛选 + `effectiveTo` 发布过期门禁 | [ce-k08-clause-legal-metadata.md](./ce-k08-clause-legal-metadata.md) |
| 现有 composition rules = 锚点可见性 `conditionExpression`（对 **variables** 求值），不消费 context 维度 | `CompositionRule` / `ConditionExpressionEvaluator` |
| Runtime `context` 白名单无 `jurisdiction` / `product`；`channel` 文档定位为排查统计，未作为组合轴 | ADR-0013；contract-outline context 表 |
| 国际银行信函需同一模板版本按辖区/产品/渠道**确定性**纳入不同条款，且可审计 | PD-5；IBL-E2 验收草案 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **E2-S1 Context dims** | Runtime `context` 增加可选 `jurisdiction`、`product`；与既有 `channel` 共同构成组合三轴 |
| **E2-S2 Inclusion rules** | 模板版本声明结构化 inclusion rules，绑定钉扎 CM `referenceKey` + 轴匹配条件 |
| **E2-S3 Evaluate** | Generate/preview/test 路径用同一确定性求值器 INCLUDE/EXCLUDE |
| **E2-S4 Audit** | Invocation / 管理审计可观测 inclusion 结果（referenceKey + 决策 + matched ruleId） |
| **E2-S5 Govern** | 草稿可写；发布锁定；未解析 referenceKey 硬门禁；权限复用 `authorTemplates` |
| **E2-S6 Orthogonal** | 不改 CE-K08 字段语义；不改 E1 locale；不自动选模板包 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板编排人员 / 母版设计人员** | `authorTemplates` 组范围 | 在草稿版本维护 inclusion rules（管理 API） |
| **分组管理员 / 全局管理员** | 组或全局治理 | 同左；发布门禁可见失败项 |
| **审批 / 测试人员** | 既有生命周期角色 | 测试/预览走同一求值器；不获额外写规则权 |
| **Runtime API 调用方** | 有效凭证；路径钉扎模板+版本；可选 context 三轴 | 正文来自钉扎版本 + 求值后的 CM 集合 |
| **系统** | Inclusion 求值 / 发布门禁 / 审计摘要 | Fail-closed 校验；确定性 omit 或 required 硬失败 |

---

## 3. Goal

1. 关闭 F25：提供 **jurisdiction / product / channel** 驱动的条款纳入引擎（非仅作者自由表达式）。  
2. Runtime 对同一发布版本 + 同一 context 三轴 → **相同** inclusion 集合（确定性）。  
3. Inclusion 决策 **可审计**（无条款正文、无 variables 明文）。  
4. 与 CE-K08 / E1 **正交**；不发明公司辖区/产品主数据目录。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave E Done。  
6. **API-first**（`frontend_ui_in_scope=false`）。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（SoT + PD-5 范围确认 + 保守银行级默认 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **E2-C1** | **三轴语义：** 组合维度 = `jurisdiction`、`product`、`channel`（均为非敏感短字符串）。**不是** E1 `locale`；**不是** UI i18n；**不是** outbound delivery channel（PD-1）。 | PD-5；F25；context 既有 `channel` |
| **E2-C2** | **Context 白名单扩展：** 在 ADR-0013 白名单上**新增可选** `jurisdiction`、`product`（字符串）。未知字段仍 `400 REQUEST_BODY_INVALID`。`channel` 既有字段**额外**承担组合匹配职责（不改字段名）。审计 `contextSummary` 同步收录非空白三轴。 | PD-5 必然消费点；对齐 locale 已用于生成控制先例 |
| **E2-C3** | **Context 叙事修正（本叶范围）：** 对上述三轴，允许作为**组合控制**输入（仍禁止 PII/金额/变量）。`sourceSystem` / `businessRequestId` / `upstreamTraceId` / `scenario` **不**进入 inclusion 匹配。ADR-0013 / contract-outline 由 doc-keeper 在 Accept ADR 时同步指针。 | 银行可审计；最小白名单扩张 |
| **E2-C4** | **类型与归一：** 三轴值 trim；空串 → 视为缺失（null）；建议 max **128**（对齐 CE-K08 `jurisdiction`）。匹配 = 大小写不敏感 **exact**（trim 后）。**不**做模糊/前缀/正则。 | K08-C2/C7 同构；fail-closed |
| **E2-C5** | **规则种类：** 新增 **Composition Inclusion Rule**（结构化），与既有锚点可见性 composition rule（`conditionExpression` → `targetAnchorId`）**并存且正交**。Inclusion **不**经 `ConditionExpressionEvaluator` / variables。 | F25「engine」；防把辖区塞进变量 |
| **E2-C6** | **挂载点：** Inclusion rules 挂在 **模板版本**（与现有 rules / CM references 同版本线）；发布时锁定；已发布版本不可改。 | 既有 authoring/publish 模型 |
| **E2-C7** | **规则字段（最小契约）：** `ruleId`（版本内唯一非空字符串）、`referenceKey`（必须指向同版本已声明的 CM reference）、`match.jurisdiction?` / `match.product?` / `match.channel?`（至少一轴非空）、可选 `priority`（整数，缺省 0）、可选 `requiredInclusion`（布尔，缺省 **false**）。 | 可测最小面；禁止发明 CM 侧 product schema |
| **E2-C8** | **匹配：** 单规则内已声明轴 **AND**；未声明轴 = 通配。请求侧对应轴缺失/空白 → 该规则 **不匹配**（不 400）。 | 确定性；调用方可逐步接入 |
| **E2-C9** | **纳入算法（确定性）：** 对每个钉扎 `referenceKey`：(1) 若无任何 inclusion rule 指向它 → **INCLUDE**（向后兼容无规则模板）；(2) 否则按 (`priority` 升序, `ruleId` 字典序升序) 求值，**任一**匹配 → **INCLUDE**（OR）；(3) 皆不匹配且该 key 存在任一条 `requiredInclusion=true` 的规则 → **422** 稳定码（如 `COMPOSITION_INCLUSION_UNSATISFIED`），**不**生成；(4) 皆不匹配且无 required → **EXCLUDE**（跳过该 CM 的 `contentModuleRef` 展开，**不** 500）。同一求值器用于 runtime generate、preview、test generation。 | 确定性 + 银行可强制必选条款 |
| **E2-C10** | **与 CE-K08 正交一致性（可选硬校验）：** 当某 CM **被 INCLUDE** 且钉扎版本 `jurisdiction != null` 且请求 `context.jurisdiction` 非空白且二者大小写不敏感不等 → **422**（如 `CONTENT_MODULE_JURISDICTION_MISMATCH`）。任一侧空白 → **跳过**该校验。不改 K08 过期门禁 / 目录筛选。 | 法务元数据可挂钩 runtime，无强制填元数据 |
| **E2-C11** | **无 product/channel CM 元数据：** 本叶**不**在 `content_module_version` 增加 product/channel 字段；产品/渠道仅规则 `match.*` vs `context.*`。 | 禁止超范围 schema |
| **E2-C12** | **不自动选模板包：** 路径仍钉扎具体模板+版本（同 E1-C6）。引擎只决定版本内 CM 纳入。 | PRD 路径钉扎 |
| **E2-C13** | **管理 API：** 草稿版本 GET/PUT inclusion rules（可与现有 `/rules` 扩展或并列 endpoint；OpenAPI 固定一种）；校验：未知 `referenceKey` → 422；空 match → 422；重复 `ruleId` → 422/409（实现固定一种稳定码）。Detail/export 视图回显。 | 程序验收 API；API-first |
| **E2-C14** | **发布门禁：** 硬项——每条 inclusion rule 的 `referenceKey` 必须存在于版本 CM references（新 `PublishGateCheckCode`，如 `COMPOSITION_INCLUSION_REFERENCE_INVALID`）。**不**要求每个 CM reference 都有 inclusion rule。 | 防悬空规则 |
| **E2-C15** | **审计：** runtime 成功路径在 invocation/audit 摘要写入非敏感 `compositionInclusionSummary`：每条求值过的 `referenceKey` → `INCLUDE`\|`EXCLUDE` + `matchedRuleId`（默认纳入时用字面 `NONE_DEFAULT` 或 null，实现固定一种并写入 OpenAPI）。禁止条款正文 / variables。管理侧 rules 变更走既有模板更新审计载荷（宜含 ruleId 列表）。 | 「auditable」验收 |
| **E2-C16** | **权限：** 无新角色；写规则 = 写模板编排 rules 同一边界；越权 403/404 惯例不变。 | permission-matrix |
| **E2-C17** | **导入/导出：** 导出包携带 inclusion rules；导入保留并重跑 E2-C13 校验。 | CE-E01 方向；防规则丢失 |
| **E2-C18** | **Frontend：** 本叶 **不**交付管理 UI 规则编辑器（`frontend_ui_in_scope=false`）。残留：后续叶可接 `TemplateRuleConfigurator`。作者可通过 API / 测试夹具配置。 | owners 无 FE；诚实范围 |
| **E2-C19** | **SPECIMEN / PD-6：** 不改变再生/水印政策。 | 程序 §8 |
| **E2-C20** | **ADR：** [ADR-0063](../adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md) **Accepted**（2026-07-20；Decision = 本节 E2-C*）。PD-5 确认范围；BDD-IBL-E2-001…016 锁定默认；无剩余产品二选一。Accepted ≠ E2 impl Done。 | 程序 IBL-E2 验收 |
| **E2-C21** | **门禁：** BE `mvn verify`；queued Docker deploy evidence（验收表面为 API/runtime）。无强制 Playwright。若改动 OpenAPI 生成前端类型，FE type-check/test 按需绿，仍非 UI 交付。 | delivery constitution + API-first |
| **E2-C22** | **完成边界：** E2 Done ≠ Wave E Done；≠ go-live；#3b/#5a 保持 CONDITIONAL；E3–E7 / #119 不激活。 | 队列政策 |

### 4.2 明确非本叶确认（禁止当作已定产品事实）

| 项 | 状态 |
| --- | --- |
| 多级法务审批矩阵 | **IBL-E3** |
| 法人文档品牌变体 | **IBL-E4** |
| `effectiveFrom` 硬阻断 / bulk re-pin | **IBL-E5** |
| 条款嵌套图治理 | **IBL-E6** |
| RTL / SPECIMEN 移除 / 授权字体 / Word | E7 / PD-6/7 / #119 — **OUT** |
| 公司辖区/产品主数据枚举服务 | **拒绝本叶发明**（自由短字符串） |
| 按辖区自动改路径选模板包 | **拒绝**（E2-C12） |
| 用 variables 表达式替代三轴引擎 | **拒绝作为本叶主路径**（既有 visibility rules 可继续用于变量条件） |

### 4.3 ADR / 用户确认

| 问题 | 结论 |
| --- | --- |
| 是否还需用户再确认「要不要做辖区/产品/渠道引擎」？ | **否** — PD-5 已确认 2026-07-19 |
| 是否还需用户再确认 E2-C1…C22 默认？ | **否（BDD ready）** — 由既有 CM 钉扎 + context 白名单 + 银行确定性/可审计要求隐含；记入本 BDD；doc-keeper Accept ADR |
| ADR 文件状态何时 Accepted？ | **已 Accepted**（2026-07-20）— [ADR-0063](../adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md)；BDD `ready` 锁定 Decision |

---

## 5. Preconditions

- 操作者具备对应组范围模板编排写权限（维护规则时）。  
- PD-5 已 Confirmed；#129 为本交付叶（orchestrator 已激活本切片）。  
- 模板版本已有（或可声明）content-module references（`referenceKey` + 钉扎语义版本）。  
- Runtime 调用方持有可调用已发布版本的凭证与路径。  
- CE-K08 / E1 行为保持可用且不被本叶破坏。

---

## 6. Trigger

- 作者经管理 API 在草稿版本 PUT inclusion rules。  
- 发布候选进入发布门禁（含 inclusion reference 校验）。  
- Runtime generate / preview / test generation 携带可选 `context.jurisdiction` / `product` / `channel`。

---

## 7. Primary journey

1. 作者在模板草稿声明 CM references：`ref-hk-law`、`ref-uk-law`、`ref-common`。  
2. 作者 PUT inclusion rules：`R1` → `ref-hk-law` match `{jurisdiction:"Hong Kong"}`；`R2` → `ref-uk-law` match `{jurisdiction:"England and Wales"}`；`ref-common` **无**规则（默认始终纳入）。  
3. 测试/审批/发布锁定规则与钉扎版本。  
4. 上游调用已发布版本，`context.jurisdiction="Hong Kong"`（可选附带 product/channel）→ 文档含 `ref-hk-law` + `ref-common`，不含 `ref-uk-law`。  
5. 审计/invocation 摘要可见 `ref-hk-law=INCLUDE/R1`、`ref-uk-law=EXCLUDE/…`、`ref-common=INCLUDE/NONE_DEFAULT`。  
6. 若作者将 `R1.requiredInclusion=true` 且请求省略 jurisdiction → **422** `COMPOSITION_INCLUSION_UNSATISFIED`。

---

## 8. System responses（成功路径）

- PUT rules 持久化并回显；非法规则 422/409。  
- 发布门禁在 reference 全合法时通过（与其它硬项 AND）。  
- Runtime：按 E2-C9 展开/跳过 CM；成功响应与既有钉扎生成一致（除纳入集合）。  
- 审计摘要含 composition inclusion 与扩展后的 contextSummary 三轴（非空白时）。

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-E2-001 — Context accepts jurisdiction and product

**Given** 可调用已发布模板版本  
**When** sync generate 提交 `context.jurisdiction="Hong Kong"` 与 `context.product="TRADE-LC"`（及既有合法字段）  
**Then** `400` **不**因未知字段；请求被接受进入后续处理（其余门禁满足时成功或按既有错误）

### BDD-IBL-E2-002 — Unknown context field still rejected

**Given** 可调用已发布模板版本  
**When** generate 提交 `context.unknownDim="x"`  
**Then** `400` `REQUEST_BODY_INVALID`；不生成

### BDD-IBL-E2-003 — Default include when no inclusion rules

**Given** 已发布版本钉扎 `referenceKey=ref-a` 且 **零** inclusion rules  
**When** generate（无论是否传三轴）  
**Then** `ref-a` **INCLUDE**（向后兼容）；产物/装配可观测包含该 CM

### BDD-IBL-E2-004 — Include when rule matches jurisdiction

**Given** 规则 `R1`：`referenceKey=ref-hk`，`match.jurisdiction="Hong Kong"`  
**And** 钉扎 `ref-hk`  
**When** generate 且 `context.jurisdiction="hong kong"`（大小写不同）  
**Then** `ref-hk` **INCLUDE**；`matchedRuleId=R1`

### BDD-IBL-E2-005 — Exclude when rules exist but none match

**Given** 仅规则 `R1` 指向 `ref-hk`（`requiredInclusion=false`）  
**When** generate 且 `context.jurisdiction="England and Wales"`（或不传 jurisdiction）  
**Then** `ref-hk` **EXCLUDE**（跳过展开）；HTTP 成功路径不因该排除而失败；审计记录 EXCLUDE

### BDD-IBL-E2-006 — Required inclusion fail-closed

**Given** 规则 `R1` 指向 `ref-hk` 且 `requiredInclusion=true`，`match.jurisdiction="Hong Kong"`  
**When** generate **省略** `context.jurisdiction`（或传不匹配值）  
**Then** `422` `COMPOSITION_INCLUSION_UNSATISFIED`（或等价稳定码）；不生成

### BDD-IBL-E2-007 — Multi-axis AND on single rule

**Given** 规则 `R2`：`match.jurisdiction="Hong Kong"` **且** `match.product="TRADE-LC"` **且** `match.channel="API"`  
**When** generate 仅匹配其中两轴、缺一轴或一轴不匹配  
**Then** 该规则不匹配；若无其它匹配规则则按 E2-C9 EXCLUDE 或 required 422

### BDD-IBL-E2-008 — OR across rules / deterministic priority

**Given** `R-low` priority=10 与 `R-high` priority=0 均可能匹配同一 `referenceKey`  
**When** generate 使两者皆匹配  
**Then** INCLUDE；`matchedRuleId` = 排序后**第一条**匹配规则（priority 升序，再 `ruleId` 升序）——可重复求值结果一致

### BDD-IBL-E2-009 — CE-K08 jurisdiction mismatch on include

**Given** INCLUDE 的钉扎 CM 版本 `jurisdiction="Hong Kong"`  
**When** generate 且 `context.jurisdiction="England and Wales"`  
**Then** `422` `CONTENT_MODULE_JURISDICTION_MISMATCH`；不生成

### BDD-IBL-E2-010 — CE-K08 blank skips mismatch check

**Given** INCLUDE 的 CM 版本 `jurisdiction` 为空 **或** 请求未传 `context.jurisdiction`  
**When** generate（其它条件满足）  
**Then** **不**因 E2-C10 拒绝（K08 过期门禁等其它规则仍适用）

### BDD-IBL-E2-011 — Management API persists inclusion rules on draft

**Given** 授权编排人员；草稿版本已有 `referenceKey=ref-hk`  
**When** PUT inclusion rules 含合法 `R1`  
**Then** 200/204；随后 GET/detail 回显相同规则集

### BDD-IBL-E2-012 — Reject rule with unknown referenceKey

**Given** 草稿版本无 `ref-missing`  
**When** PUT 含指向 `ref-missing` 的 inclusion rule  
**Then** `422`（稳定校验码）；不落库非法集

### BDD-IBL-E2-013 — Publish gate invalid inclusion reference

**Given** 版本曾保存合法规则后 CM reference 被移除导致悬空（或等价夹具）  
**When** 评估/执行 publish  
**Then** 门禁硬失败，含 `COMPOSITION_INCLUSION_REFERENCE_INVALID`（或等价）；不得发布

### BDD-IBL-E2-014 — Audit / invocation inclusion summary

**Given** BDD-IBL-E2-004 成功路径  
**When** 读取 invocation 或 runtime 审计摘要  
**Then** 可见非敏感 `compositionInclusionSummary`（含 referenceKey、INCLUDE/EXCLUDE、matchedRuleId）；**无**条款正文、**无** variables 明文；`contextSummary` 含非空白 `jurisdiction`（及 product/channel 若提供）

### BDD-IBL-E2-015 — Unauthorized rules write fail-closed

**Given** 用户无目标组模板写权限  
**When** 尝试 PUT inclusion rules  
**Then** `403` 或 `404`（既有惯例）；无写入

### BDD-IBL-E2-016 — SPECIMEN / PD-6 / orthogonality unchanged

**Given** CE-G06 regenerate 路径与 E1 locale 资产  
**When** E2 变更合并后执行 regenerate / locale 兼容路径  
**Then** 成功样件仍含 SPECIMEN；E1 locale 字段与门禁行为不被本叶移除或削弱；不激活 E3–E7；不翻转 #3b/#5a

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 未知 context 字段 | `400 REQUEST_BODY_INVALID` |
| 轴值仅空白 | 视为缺失 |
| 规则 match 全空 | 422 保存失败 |
| 重复 ruleId | 422/409（实现固定） |
| 无规则 CM | 默认 INCLUDE |
| 有规则不匹配 + required=false | EXCLUDE |
| 有规则不匹配 + required=true | 422 |
| INCLUDE + K08 jurisdiction 双方非空且不等 | 422 |
| 已发布版本改规则 | 拒绝（既有锁定） |
| 权限不足 | fail-closed |
| 表达式 visibility rules | 不变；不替代三轴引擎 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | context 字段；rules PUT/GET；422/400 码；publish gate code |
| Runtime | 同请求重复生成 inclusion 集合一致；EXCLUDE 跳过可测 |
| Audit | `compositionInclusionSummary` + contextSummary 三轴 |
| Docs | 本 BDD；[ADR-0063 Accepted](../adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md)；domain/API/permission 指针（doc-keeper stage 3） |
| Gates | `mvn verify`；queued deploy；**无**强制 FE E2E |
| 负向 | 无 SPECIMEN 移除；无 #3b/#5a 翻转；无 E3–E7 激活 |

---

## 12. Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#129** | IBL-E2 delivery leaf |
| IBL program **F25** / **PD-5** / §7 IBL-E2 acceptance | 范围 + Done 草案 |
| [ADR-0063](../adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md) | 架构 Decision（Accepted = E2-C*；≠ impl Done） |
| ADR-0013 | context 白名单基线；本叶扩展三轴组合职责 |
| CE-K08 BDD | 正交法务元数据 + E2-C10 挂钩 |
| IBL-E1 / ADR-0062 | 正交 locale |
| domain-model §2.9 / §2.9.2 | 领域确认指针（doc-keeper 同步） |
| contract-outline / OpenAPI | API 契约 |
| permission-matrix | 无新角色 |

---

## 13. Implementation notes（非产品发明；供 TDD）

- 错误码与 `PublishGateCheckCode` 枚举名以实现为准，但须稳定、可测、写入 OpenAPI/`messageKey`。  
- Inclusion rules 存储可为独立 JSON 列/表或扩展现有 rules 载荷并带 `ruleKind=INCLUSION`；**契约对外字段**以 E2-C7 为准。  
- EXCLUDE 时 writer 对对应 `contentModuleRef` 跳过展开；不得留下半残占位错误文本。  
- Preview/test 必须调用同一求值入口，禁止「仅 runtime 生效」分叉。  
- OpenAPI 生成前端类型若触发 FE type-check，属附带，不构成 UI 范围扩张。  
- doc-keeper stage 3：**已** Accept ADR-0063 并同步 domain / PRD / contract-outline / OpenAPI stubs / docs indexes；Accepted ≠ E2 impl Done。

---

## 14. Handoff

```text
bdd_readiness: ready
frontend_ui_in_scope: false
scenario_count: 16
scenario_ids:
  - BDD-IBL-E2-001
  - BDD-IBL-E2-002
  - BDD-IBL-E2-003
  - BDD-IBL-E2-004
  - BDD-IBL-E2-005
  - BDD-IBL-E2-006
  - BDD-IBL-E2-007
  - BDD-IBL-E2-008
  - BDD-IBL-E2-009
  - BDD-IBL-E2-010
  - BDD-IBL-E2-011
  - BDD-IBL-E2-012
  - BDD-IBL-E2-013
  - BDD-IBL-E2-014
  - BDD-IBL-E2-015
  - BDD-IBL-E2-016
open_questions: []
adr_status: Accepted — docs/adr/template-lifecycle/0063-jurisdiction-product-channel-composition-rules.md
recommended_next_stage: plan-orchestrator (if needed) → backend-engineer (TDD impl; API-first; no FE UI mandate)
```
