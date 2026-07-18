# BDD 行为规格：IBL-A2 — ISO-currency `FORMAT_AMOUNT`

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-A2` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | IBL Wave A · **IBL-A2** / F2（`ibl-a2-format-amount-currency`） |
| **Slice** | `ibl-a2-format-amount-currency` |
| **Branch** | `feat/ibl-a2-format-amount-currency` |
| **Worktree** | `D:/working/DGE-ibl-a2-format-amount-currency` |
| **Base** | `60c9efd3`（handoff） |
| **Placement** | ISOLATED |
| **Task Master** | **#108** IBL-A2 — Batch Recommendation **solo**；`member_task_ids: ["108"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-a2-format-amount-currency`；vetoes: `different-acceptance-vs-A3`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F2 / IBL-A2；上游 DSL [ce-k03-variable-compute-engine.md](./ce-k03-variable-compute-engine.md)（K03-C10/C11 一元形态保留）；边界 [ADR-0056](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md)（本叶实现同期修订 currency 参数）；契约 [openapi-v1.yaml](../api/openapi-v1.yaml) / [contract-outline.md](../api/contract-outline.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（本叶为 compute DSL / BE 求值契约；E2E/UIUX **N/A**。管理端客户端白名单校验可随 arity 同步修正，非 UI 旅程） |

**完成声明约束：** 本叶关闭 F2——`FORMAT_AMOUNT` 在显式 ISO 币种下按该币种渲染，而非 locale 默认币种（如 `en-US` + EUR 不得出 `$`）。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave A / 程序 Done；**禁止**把 A3（amount-in-words）/ A4–A6 并入本叶。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["108"]
  proposed_slice_id: ibl-a2-format-amount-currency
  shared_acceptance_surface: FORMAT_AMOUNT ISO currency rendering
  vetoes_applied:
    - different-acceptance-vs-A3
  evidence_amortization: mvn verify + docker
  on_red_split_hint: N/A
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| DSL：`FORMAT_AMOUNT(value, currencyCode)` 二元形态（ISO 4217 + amount + `context.locale`） | IBL-A3 `SPELL_AMOUNT` / amount-in-words `en` |
| 一元 `FORMAT_AMOUNT(value)` **兼容**保留：locale 默认币种（CE-K03 / 金标 `04-compute-variables`） | 改变 `SPELL_AMOUNT` CNY 语义 |
| EUR + `en-US` → EUR 币种符号/代码展示，**非** `$` | IBL-A4 `/contract`；A5 PII；A6 regenerate locale |
| 多币种单测矩阵（至少 EUR/USD/CNY × locale 交叉） | FE Playwright / OA 视觉旅程 |
| OpenAPI / contract-outline / ADR-0056 文档化 currency 参数契约 | 翻转 #3b/#5a；go-live |
| Gates：`mvn -B -ntp -f backend/pom.xml verify`；queued deploy 证据（行为验收面） | 发明正式 P-phase |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| `evalFormatAmount` 仅接受 **1** 参，调用 `NumberFormat.getCurrencyInstance(locale)` | `ComputeExpressionEvaluator.evalFormatAmount` + `requireOne` |
| Locale 默认币种：`en-US` → USD/`$`，即使业务金额为 EUR | F2；JDK `NumberFormat.getCurrencyInstance(Locale.US)` |
| 分数位硬编码 2 | `setMinimumFractionDigits(2)` / `setMaximumFractionDigits(2)` |
| 金标/单测使用一元形态 | golden `04-compute-variables`：`FORMAT_AMOUNT(${principal})`；`VariableComputeEngineTest.formatAmount*` |
| FE 客户端样例曾写 `FORMAT_AMOUNT(..., en-US)`（第二参像 locale）——**与本叶契约不符**；今日后端亦拒绝 ≠1 参 | `computeExpressionValidate.test.ts`（仅白名单存在性，不解析 arity） |
| CE-K03 明确「本片不引入多币种参数」 | [ce-k03](./ce-k03-variable-compute-engine.md) K03-C10；**由本叶超集扩展** |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **A2-S1 二元 ISO 币种** | `FORMAT_AMOUNT(amountExpr, currencyExpr)`：金额按 ISO 4217 币种格式化；**数字/分组/符号本地化**仍跟 `context.locale` |
| **A2-S2 一元兼容** | 省略 currency → 保持 CE-K03 locale 默认币种行为（含金标回归） |
| **A2-S3 Fail-closed** | 非法 arity、null/空白/非法 ISO 币种、null 金额 → `VARIABLE_COMPUTE_FAILED`（既有码） |
| **A2-S4 契约文档** | OpenAPI compute 表达式描述 + contract-outline / ADR-0056 记载 currency 参数 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板作者** | 管理会话；编写 `computeExpression`；可选样例求值 API | 需能声明 EUR/USD/CNY 等 ISO 币种，而非依赖 locale 默认币种 |
| **Runtime API 调用方** | 有效凭证；generate / batch；`context.locale` | 产物金额币种与模板表达式一致 |
| **系统（compute）** | `VariableComputeService` → `ComputeExpressionEvaluator` | 解析 arity；ISO → `Currency`；locale → 数字格式 |
| **系统（author APIs）** | `POST …/compute-expressions/validate|evaluate` | 二元表达式合法；evaluate 可预览多币种结果 |

---

## 3. Goal

1. 显式 ISO 币种的 `FORMAT_AMOUNT` 渲染该币种（符号或 ISO 代码展示，由 locale 的 currency 格式规则决定），**不得**静默改用 locale 默认币种。  
2. 典型验收：`FORMAT_AMOUNT(1234.56, 'EUR')` + `context.locale=en-US` → 结果表示 **EUR**（含 `€` 和/或 `EUR` 标识），**不得**以 `$` / USD 作为币种身份。  
3. 一元形态继续可用，行为与 CE-K03 一致（locale 默认币种；金标不破）。  
4. 多币种 × locale 单测矩阵覆盖至少 EUR / USD / CNY。  
5. OpenAPI / 文档注明 currency 参数契约；ADR-0056 同步修订决策文本（语义扩展，非任务完成状态）。  
6. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **A2-C1** | **函数形态：** `FORMAT_AMOUNT(value)` **或** `FORMAT_AMOUNT(value, currency)`。Arity ∉ {1,2} → 求值失败 `VARIABLE_COMPUTE_FAILED`。 | F2；解析器已支持多参列表；今日 `requireOne` 需放宽 |
| **A2-C2** | **第二参语义 = ISO 4217 字母币种码**（如 `'EUR'`、`'USD'`、`'CNY'`），**不是** locale 标签。Locale **仅**来自请求 `context.locale`（及 CE-K03 默认/回退 `zh-CN`）。 | IBL-A2「currency code + amount + locale」；避免与 CE-C01 locale 双通道冲突 |
| **A2-C3** | **币种求值：** `currency` 可为字符串字面量或求值后为字符串的表达式（含 `${path}`）。求值结果 trim 后按 **大写** 规范化再解析 ISO（`eur` → `EUR`）。 | DSL 一致性；作者常写变量绑定币种 |
| **A2-C4** | **格式化规则（二元）：** 使用 `context.locale` 的数字/货币本地化规则，**币种身份**来自 ISO `Currency`（实现上等价于 `NumberFormat.getCurrencyInstance(locale)` + `setCurrency(Currency.getInstance(code))`，或同效 API）。 | F2 根因修复 |
| **A2-C5** | **分数位（二元）：** 使用该 ISO 币种的默认小数位（`Currency.getDefaultFractionDigits()`），不再对所有币种硬编码 2（例：JPY → 0）。 | ISO 正确性；与一元硬编码 2 区分 |
| **A2-C6** | **一元兼容（missing currency）：** 仅 `FORMAT_AMOUNT(value)` 时保持 CE-K03：`getCurrencyInstance(locale)` + 最小/最大分数位 **2**（金标/单测稳定）。文档标注：国际函件应优先二元显式币种。 | golden `04-compute-variables`；K03-C10 保留路径 |
| **A2-C7** | **null / 空白 currency（二元）：** currency 求值为 `null`、或 trim 后空串 → `VARIABLE_COMPUTE_FAILED`（不回退 locale 默认币种）。 | fail-closed；避免静默错币种 |
| **A2-C8** | **非法 ISO 码：** `Currency.getInstance` 拒绝（或平台等价校验失败）→ `VARIABLE_COMPUTE_FAILED`。 | fail-closed |
| **A2-C9** | **null / 非数值 amount：** 与 CE-K03 一致 → `VARIABLE_COMPUTE_FAILED`。 | K03；既有 `formatAmountNullFails` |
| **A2-C10** | **CNY + zh-CN 回归：** 二元 `FORMAT_AMOUNT(amount, 'CNY')` + locale `zh-CN` 必须呈现 **CNY/人民币** 币种身份（不得呈现 USD/`$`）；一元 + `zh-CN` 继续为 locale 默认（CNY）路径，结果可区分于一元 + `en-US`。 | handoff「CNY/zh regression」 |
| **A2-C11** | **可观测断言策略（单测）：** 对「非 locale 默认币种」用例，断言结果 **含** 目标币种标识（`€` 或 `EUR`；`¥`/`￥` 或 `CNY`；`$` 或 `USD` 等——按 JDK/locale 实际输出允许符号 **或** ISO 码其一），并 **断言不出现** 错误币种的主导符号（例：EUR+en-US **不得**含 `$` 作为币种符号）。不强制锁死完整 ICU 字面量跨 JDK，但矩阵用例须稳定可重复。 | JDK 差异；F2 验收 |
| **A2-C12** | **错误码：** 本叶不新增顶层码；求值失败继续 `VARIABLE_COMPUTE_FAILED`（HTTP 422，`category`/`retryable` 与 CE-K03 一致）。 | CE-K03；缩小范围 |
| **A2-C13** | **文档面（强制）：** OpenAPI 中 compute 表达式相关 schema/operation 描述注明 `FORMAT_AMOUNT(value)` 与 `FORMAT_AMOUNT(value, currencyCode)`；`contract-outline`（或等价 API 文档段）交叉引用；实现同期 **修订 ADR-0056** Decision（locale + optional ISO currency），不把任务 Done 写进 ADR。 | IBL-A2 验收「OpenAPI/docs note」 |
| **A2-C14** | **与 CE-K03 关系：** K03-C10「不引入多币种」被本叶 **超集**：一元语义保留；二元为新确认需求。BDD-CE-K03-011/012 仍有效（一元）。 | 源序：本叶确认 > K03 历史范围 |
| **A2-C15** | **FE：** `frontend_ui_in_scope=false`。允许修正客户端样例/注释，使第二参示例为 `'EUR'` 而非伪 locale；不要求 Playwright。 | 交付范围 |
| **A2-C16** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；行为变更验收面 → Stage 5/10 queued Docker deploy 证据；architecture review。 | delivery constitution |
| **A2-C17** | **完成边界：** Done ≠ Wave A 完备；≠ go-live；#3b/#5a 保持 CONDITIONAL。 | 队列政策 |

### 4.2 已确认（上游交付，本叶只消费）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **A2-U1** | 白名单含 `FORMAT_AMOUNT`；locale 默认 `zh-CN` | CE-K03；ADR-0056 |
| **A2-U2** | 求值失败 → `VARIABLE_COMPUTE_FAILED` | CE-K03 |
| **A2-U3** | `context.locale` 白名单与回退 | CE-C01 / K03-C11 |
| **A2-U4** | Formal phase None；非 go-live | IBL 队列政策 |

### 4.3 非确认假设（不得升格为需求）

| ID | 陈述 | 状态 |
| --- | --- | --- |
| **A2-N1** | 强制所有既有一元表达式迁移为二元 | **非确认** — 一元兼容（A2-C6） |
| **A2-N2** | `SPELL_AMOUNT` 多币种 / en 大写 | **非确认** — IBL-A3 |
| **A2-N3** | 数字币种码（ISO 4217 numeric） | **非确认** — 本叶仅字母码 |
| **A2-N4** | 作者面板专用币种下拉 UI | **非确认** — FE OOS |
| **A2-N5** | 第二参传入 locale 标签（如 `en-US`）作为合法形态 | **非确认** — **明确拒绝**（A2-C2）；若传入非 ISO 币种串则按 A2-C8 失败 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | Runtime / preview / test-generate 装配前 compute 求值含 `FORMAT_AMOUNT` | 主验收面 |
| T2 | 管理 `POST …/compute-expressions/evaluate`（可选 `locale`） | 作者预览 |
| T3 | 单元测试 / `mvn verify`（含金标一元回归） | 门禁 |

---

## 6. Preconditions

| # | 前置条件 |
| --- | --- |
| PC1 | 模板变量 schema 含合法 `computeExpression`（或 evaluate API 直接提交表达式） |
| PC2 | 绑定值可解析为数值金额（二元成功路径） |
| PC3 | `context.locale` 缺失时引擎按 CE-K03 默认 `zh-CN` |
| PC4 | 调用方已通过既有认证/授权（本叶不放宽） |

---

## 7. Primary journey（成功）

1. 作者编写 `FORMAT_AMOUNT(${principal}, 'EUR')`（或 `${currency}`）。  
2. Runtime/preview 请求带 `context.locale=en-US`，`principal=1234.56`。  
3. 引擎求值：金额 + ISO EUR + en-US 本地化规则。  
4. 结果字符串以 EUR 为币种身份写入装配变量 / 样例 evaluate `result`。  
5. 一元金标路径仍可求值成功（locale 默认币种）。

---

## 8. System responses

### 8.1 Success

| 形态 | 响应 |
| --- | --- |
| 二元合法 | 返回货币格式化字符串；币种 = ISO；locale 影响符号位置/分组 |
| 一元合法 | 与 CE-K03 一致的 locale 默认币种字符串 |
| evaluate API | `success=true` + `result` 为上述字符串 |

### 8.2 Fail-closed

| 条件 | 行为 |
| --- | --- |
| Arity ∉ {1,2} | `VARIABLE_COMPUTE_FAILED`；无成功静默格式化 |
| 二元 currency null/空白/非法 ISO | 同上 |
| amount null / 非数值 | 同上（既有） |
| 认证/授权失败 | 既有 401/403；不改变 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-A2-001 — EUR + en-US 不得渲染 locale 默认 `$`

**Given** 表达式 `FORMAT_AMOUNT(${principal}, 'EUR')`，`principal=1234.56`  
**And** `context.locale=en-US`  
**When** compute 求值（引擎单测或 evaluate API）  
**Then** 结果字符串标识 **EUR** 币种（含 `€` 和/或 `EUR`）  
**And** 结果**不得**以 `$` 作为币种符号（F2 回归：禁止 locale-default USD）

### BDD-IBL-A2-002 — USD + zh-CN：币种为 USD 而非 CNY

**Given** 表达式 `FORMAT_AMOUNT(${principal}, 'USD')`，`principal=1234.56`  
**And** `context.locale=zh-CN`  
**When** 求值  
**Then** 结果标识 **USD** 币种（含 `$` 和/或 `USD`）  
**And** 结果**不得**以人民币默认币种身份冒充（不得仅因 locale=zh-CN 变成 CNY/`¥` 主导且无 USD 标识——断言策略按 A2-C11：必须可观测为 USD）

### BDD-IBL-A2-003 — CNY + zh-CN 二元回归

**Given** 表达式 `FORMAT_AMOUNT(${principal}, 'CNY')`，`principal=1234.56`  
**And** `context.locale=zh-CN`  
**When** 求值  
**Then** 结果标识 **CNY**/人民币币种（含 `¥`/`￥` 和/或 `CNY`）  
**And** 结果**不得**呈现为 USD/`$` 主导币种

### BDD-IBL-A2-004 — 一元兼容：缺失 currency 仍用 locale 默认币种

**Given** 表达式 `FORMAT_AMOUNT(${principal})`（无第二参），`principal=1234.5`  
**When** 分别以 `context.locale=zh-CN` 与 `en-US` 求值  
**Then** 两次结果均成功且**彼此可区分**（CE-K03 BDD-CE-K03-011/012 回归）  
**And** 金标包 `04-compute-variables` 一元表达式路径不因本叶而 fail

### BDD-IBL-A2-005 — Locale 交互：同一 ISO，不同 locale，币种身份不变

**Given** 表达式 `FORMAT_AMOUNT(${principal}, 'EUR')`，同一金额  
**When** 分别以 `en-US` 与 `de-DE`（或另一 CE-C01 允许且可解析的非 en-US locale）求值  
**Then** 两次结果均标识 **EUR**（均不得变成 `$`/USD）  
**And** 两次结果的分组/符号位置等本地化形式**允许不同**（证明 locale 仍影响格式、不影响币种身份）

### BDD-IBL-A2-006 — 币种来自变量绑定

**Given** 表达式 `FORMAT_AMOUNT(${principal}, ${ccy})`，`principal=100`，`ccy='GBP'`（或等价绑定）  
**And** `context.locale=en-US`  
**When** 求值  
**Then** 结果标识 **GBP** 币种  
**And** 结果不得标识为 USD/`$` 主导（除非断言策略证明同时含 GBP 身份——以 GBP 为准）

### BDD-IBL-A2-007 — 二元 currency 缺失/空白 → fail-closed

**Given** 表达式 `FORMAT_AMOUNT(${principal}, ${ccy})`，`principal=100`，`ccy` 为 `null` 或 `''`  
**When** 求值  
**Then** 抛出/映射 `VARIABLE_COMPUTE_FAILED`  
**And** **不**回退为 locale 默认币种成功串

### BDD-IBL-A2-008 — 非法 ISO 币种 → fail-closed

**Given** 表达式 `FORMAT_AMOUNT(${principal}, 'NOTACURRENCY')`（或其它非法字母码）  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`  
**And** 无成功格式化结果

### BDD-IBL-A2-009 — 非法 arity → fail-closed

**Given** 表达式 `FORMAT_AMOUNT(${principal}, 'EUR', 'USD')`（3 参）或 `FORMAT_AMOUNT()`（0 参）  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`

### BDD-IBL-A2-010 — 契约文档记载 currency 参数

**Given** 本叶实现完成集  
**When** 审查 OpenAPI（compute validate/evaluate 相关 description）与 contract-outline / ADR-0056  
**Then** 文档明确：`FORMAT_AMOUNT(value)` 与 `FORMAT_AMOUNT(value, currencyCode)`；`currencyCode` 为 ISO 4217；locale 仍为 `context.locale`  
**And** 示例使用 `'EUR'` 等币种码，**不**把第二参描述为 locale

---

## 10. Boundary / exception

| 场景 | 行为 |
| --- | --- |
| 小写 `'eur'` | 规范化为大写后成功（A2-C3） |
| JPY 等 0 小数位币种（二元） | 使用币种默认小数位（A2-C5）；单测至少一个非 2 位币种 |
| 金额 `0` / 负数 | 非 null 数值允许格式化（符号随 locale）；与 SPELL_AMOUNT 负数拒绝无关 |
| 嵌套：`FORMAT_AMOUNT(SUM(...), 'EUR')` | 合法（第一参为表达式） |
| 第二参误传 `'en-US'` | 非法 ISO → A2-C8 失败（**不是**切换 locale） |
| 授权失败 | 既有 fail-closed；本叶不放宽 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 单测矩阵 | 至少覆盖 BDD-IBL-A2-001…009（010 为文档审查） |
| API evaluate（可选） | `success` + `result` 字符串 |
| 金标 | `04-compute-variables` 一元路径 GREEN |
| 契约 | OpenAPI description + ADR-0056 修订 + contract-outline 交叉引用 |
| 门禁 | `mvn verify` GREEN；queued Docker deploy 证据 |
| Trace | 既有 `metadata.traceId` 保留 |

---

## 12. Traceability

| 项 | 引用 |
| --- | --- |
| Plan | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) · F2 · **IBL-A2** |
| Task Master | **#108** |
| Related BDD | [ce-k03-variable-compute-engine.md](./ce-k03-variable-compute-engine.md)（一元）；IBL-A3 后续 |
| Code anchors | `ComputeExpressionEvaluator.evalFormatAmount`；`VariableComputeEngineTest`；golden `04-compute-variables` |
| API | [openapi-v1.yaml](../api/openapi-v1.yaml)；[contract-outline.md](../api/contract-outline.md) |
| ADR | [0056-whitelist-variable-compute-dsl-bounds.md](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md)（实现期修订） |
| Permissions | 无新 capability |

---

## 13. Out of scope（本叶）

- IBL-A3 amount-in-words / `SPELL_AMOUNT` en  
- IBL-A4 / A5 / A6  
- 强制迁移全部一元表达式  
- FE E2E/UIUX；作者币种下拉  
- 翻转 #3b/#5a；go-live；Wave A Done  

---

## 14. Ready-for-implementation handoff

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-a2-format-amount-currency.md
task_ids: ["108"]
plan_id: IBL-A2
frontend_ui_in_scope: false
acceptance_scenario_ids:
  - BDD-IBL-A2-001
  - BDD-IBL-A2-002
  - BDD-IBL-A2-003
  - BDD-IBL-A2-004
  - BDD-IBL-A2-005
  - BDD-IBL-A2-006
  - BDD-IBL-A2-007
  - BDD-IBL-A2-008
  - BDD-IBL-A2-009
  - BDD-IBL-A2-010
next_stage: plan-orchestrator (stage 2)
currency_arity_decision: >
  FORMAT_AMOUNT(value) locale-default currency (compat);
  FORMAT_AMOUNT(value, iso4217) ISO currency + context.locale formatting;
  missing/blank/invalid currency on 2-arg → VARIABLE_COMPUTE_FAILED (no silent locale-default).
```

**TDD Red 优先场景：** BDD-IBL-A2-001（EUR+en-US ≠ `$`）、002（USD+zh-CN）、003（CNY+zh）、004（一元兼容）、007/008（fail-closed）。
