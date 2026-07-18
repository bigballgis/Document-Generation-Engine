# CE-K03 变量计算与格式化引擎 — BDD

| Field | Value |
| --- | --- |
| **Slice** | `ce-k03-variable-compute-engine` |
| **Plan task** | **CE-K03**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §3 CE-K03） |
| **Task Master** | **#59** |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-15 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k03-variable-compute-engine` · `feat/ce-k03-variable-compute-engine` |
| **Scope of this slice** | 白名单 DSL 求值引擎（COALESCE / SUM / COUNT / AVG / FILTER / FORMAT_AMOUNT / FORMAT_DATE / SPELL_AMOUNT）；生成与预览装配前统一求值；`VARIABLE_COMPUTE_FAILED` fail-closed；`FORMAT_*` 消费 `context.locale`（默认 zh-CN）；管理端变量面板即时校验 + 样例求值预览；DSL 边界 ADR；金标 `04-compute-variables` + `05-chinese-uppercase-amount` → ACTIVE。**禁止** Groovy / JS / SpEL 完整脚本引擎；**不** go-live；**不**激活 CD-3 |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；locale 上游 [ce-c01-c02-contract-strictness.md](./ce-c01-c02-contract-strictness.md)；测试集 compute 跳过 [ce-u03-testdata-schema-form.md](./ce-u03-testdata-schema-form.md)；金标约定 [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md)；本片交付须新增 **DSL bounds ADR**（见 K03-C20） |

---

## 1. 概述

本切片把模板变量上的死字段 `computeExpression` 变成**可求值、可失败、可审计**的声明式计算层。作者用白名单函数与 `${path}` 引用声明派生变量；运行时与预览在装配 DOCX **之前**统一求值；金额/日期格式化与人民币大写金额（`SPELL_AMOUNT`）由引擎产出，调用方无需预格式化。

**现状证据（R2 / 计划卡）：**

- `computeExpression` 可持久化、可在 View/API 回读，**生成/预览路径从不求值**。
- 无聚合、无 `FORMAT_AMOUNT` / `FORMAT_DATE`、无中文大写金额。
- `context.locale`（CE-C01）仅接受并摘要，**不**驱动格式化（本片开始消费）。
- 管理端变量面板对 `COMPUTED` 仅有表达式文本框，无语法/引用校验、无样例求值。
- 金标 `04-compute-variables`、`05-chinese-uppercase-amount` 仍为 **PLACEHOLDER**（K07 骨架）。

**改动面（计划卡）：** 新 `template`（或 `sharedkernel`）表达式引擎子包；`runtime` / `rendering` / preview 装配前接入；前端 `TemplateVariableTreePanel`（及等价变量面板）；金标两包 ACTIVE；**DSL 边界 ADR**。

---

## 2. Actor / Role

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **Template Author** | 在变量 schema 定义 `COMPUTED` + `computeExpression` | 即时校验、样例预览、保存合法表达式 |
| **TEMPLATE_TESTER / 预览路径** | test-generate / preview 装配 | 装配前求值；失败可见；成功则 DOCX 含计算值 |
| **Runtime API 调用方** | sync / async / batch 生成 | 入参不含 compute 键（U03）；产物含求值结果；失败 fail-closed |
| **系统（Compute Engine）** | 白名单 DSL 求值层 | 禁脚本；深度/长度上限；locale 感知 |
| **平台 / CI** | 单测矩阵 + 金标 | 每函数边界；两包 ACTIVE；`mvn verify` 绿 |
| **（间接）法务 / 函件读者** | 金额大写与格式正确 | 可观察：DOCX/PDF 文本 |

---

## 3. Goal

1. **白名单 DSL 求值：** 仅支持函数 `COALESCE`、`SUM`、`COUNT`、`AVG`、`FILTER`、`FORMAT_AMOUNT`、`FORMAT_DATE`、`SPELL_AMOUNT`；变量引用 `${path.to.var}`；禁止循环构造、禁止任意方法/属性调用、禁止 Groovy/JS/SpEL 脚本引擎。
2. **统一求值时机：** 正式 runtime 生成与管理端 preview / test-generate 在 **DOCX 装配之前**对所有 compute 变量求值；求值结果注入绑定上下文后再渲染。
3. **Fail-closed：** 任一 compute 变量求值失败 → 错误码 **`VARIABLE_COMPUTE_FAILED`**（含失败变量 key + 表达式摘要），**不**产出成功件 / 半残件。
4. **Locale：** `FORMAT_AMOUNT` / `FORMAT_DATE` 使用请求 `context.locale`；缺省或空白 → **`zh-CN`**（对齐 CE-C01 白名单键）。
5. **SPELL_AMOUNT：** 人民币中文大写；覆盖 0 元、整元、角分、亿级；**负数拒绝**。
6. **管理端：** 变量面板对 compute 类型提供表达式**即时校验**（语法 + 引用变量存在性）+ **样例求值预览**。
7. **金标：** `04-compute-variables` 与 `05-chinese-uppercase-amount` 由 PLACEHOLDER → **ACTIVE**，纳入 `mvn verify`。
8. **治理：** 本片交付同变更集新增 **Accepted ADR** 固化 DSL 边界与禁脚本引擎决策。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **K03-C1** | **引擎形态：** 自研（或平台内）**白名单 DSL 解析/求值器**；**禁止**引入 Groovy、JavaScript、完整 SpEL、或其他任意代码执行引擎。字面量、`${path}` 引用与白名单函数调用是仅有可执行构造。 | 计划卡「禁止」栏；用户确认 scope |
| **K03-C2** | **白名单函数（精确集合，大小写敏感，UPPER_SNAKE）：** `COALESCE`、`SUM`、`COUNT`、`AVG`、`FILTER`、`FORMAT_AMOUNT`、`FORMAT_DATE`、`SPELL_AMOUNT`。未知函数名 → 求值失败。 | 计划卡 |
| **K03-C3** | **变量引用：** `${path}`，path 为点分路径（如 `${loan.principal}`、`${items}`）。路径只解析**当前绑定上下文**中已存在的输入变量与**已求值**的其他 compute 变量；不得反射调用对象方法。 | 计划卡 |
| **K03-C4** | **compute 变量识别：** `variableType == COMPUTED` **或** `computeExpression` 非空白（与 U03-C8 一致）。此类变量**不得**要求调用方在 `variables` 中提供值；引擎写入求值结果。若调用方显式携带同名键：生成路径**忽略调用方值并以引擎结果为准**（或剥离后求值——实现二选一，须在 ADR/实现注释固定；不得因「多了 compute key」单独硬阻断）。 | U03-C8；本片求值权威 |
| **K03-C5** | **求值时机：** 在 runtime `DocumentGenerationAssemblySupport`（及 sync/async/batch 等价入口）与 preview `PreviewGenerationAssemblySupport` / test-generate **打开母版装配之前**完成全部 compute 求值。渲染层只消费已解析标量/字符串，不再二次解释表达式。 | 计划卡「生成/预览装配前」 |
| **K03-C6** | **求值顺序：** 按依赖拓扑对 compute 变量排序；若存在环（A→B→A）→ `VARIABLE_COMPUTE_FAILED`。无依赖边时按稳定的 schema 声明顺序。 | fail-closed；可测试 |
| **K03-C7** | **失败错误：** 错误码 **`VARIABLE_COMPUTE_FAILED`**；`category` 适合业务失败（非 AUTH）；`retryable=false`；`messageKey=api.error.variable.computeFailed`（English-first i18n）；错误详情/信封必须可观察 **失败变量 key** 与 **表达式摘要**（截断至 ≤128 字符，勿回显完整超长表达式与客户敏感原文以外的多余 payload）。HTTP：runtime API **4xx**（建议 422 或与既有业务失败一致，实现固定一种并写入 OpenAPI）；preview → 预览记 `FAILED`，不落成功产物。 | 计划卡 |
| **K03-C8** | **长度/深度上限（硬顶）：** 单条 `computeExpression` 长度 ≤ **2048** 字符；函数嵌套深度 ≤ **8**；`${path}` 段数 ≤ **16**；compute→compute 依赖链深度 ≤ **8**；`SUM`/`AVG`/`COUNT`/`FILTER` 输入集合元素数 ≤ **10_000**。超限 → `VARIABLE_COMPUTE_FAILED`（校验阶段亦可拒绝保存，见 K03-C16）。 | 计划卡「深度与长度上限」 |
| **K03-C9** | **禁止构造：** `for`/`while`/递归用户语法、任意 `.method()`、任意 `[index]` 赋值、脚本块、注释注入执行、字符串拼出函数名执行。仅允许字面量（number/string/boolean/null）、`${path}`、白名单函数调用与逗号分隔参数列表。 | 计划卡 |
| **K03-C10** | **函数语义（基线）：** | 计划卡 + TDD 矩阵 |
| | `COALESCE(a, b, …)` — 返回第一个非 null / 非缺失值；全缺失 → null（若目标类型不允许 null 则失败）。 | |
| | `SUM(collection)` — 数值集合求和；空集合 → `0`。非数值元素 → 失败。 | |
| | `COUNT(collection)` — 元素个数；空 → `0`。`COUNT` 亦可对 FILTER 结果计数。 | |
| | `AVG(collection)` — 算术平均；空集合 → 失败（不可定义）。非数值 → 失败。 | |
| | `FILTER(collection, fieldPath, op, literal)` — 从对象列表过滤；`op` ∈ {`EQ`,`NE`,`GT`,`GE`,`LT`,`LE`,`IS_NULL`,`IS_NOT_NULL`}；`fieldPath` 为相对字段路径（点分，无 `${}`）；`IS_NULL`/`IS_NOT_NULL` 忽略 literal。结果为列表，可供 SUM/COUNT/AVG。 | |
| | `FORMAT_AMOUNT(value)` — 按 locale 格式化为金额展示字符串（默认币种展示规则由 locale 决定；本片不引入多币种参数）。**超集：** 国际函件 ISO 币种二元形态见 [IBL-A2](./ibl-a2-format-amount-currency.md) `FORMAT_AMOUNT(value, currencyCode)`。 | |
| | `FORMAT_DATE(value)` — 按 locale 格式化为日期展示字符串；输入为 ISO 日期/日期时间或平台 DATE 类型。 | |
| | `SPELL_AMOUNT(value)` — 人民币中文大写字符串（见 K03-C12）。 | |
| **K03-C11** | **Locale：** `FORMAT_AMOUNT` / `FORMAT_DATE` 读取生成请求 `context.locale`（CE-C01 白名单）。缺失、null、空白 → 默认 **`zh-CN`**。非法 locale 标签（CE-C01 已拦类型；若通过但无法解析）→ 回退 `zh-CN` **或** fail-closed 为 `VARIABLE_COMPUTE_FAILED`——本片选定：**无法解析则回退 zh-CN**（与「默认 zh-CN」一致，避免仅为 locale 标签失败整单；审计摘要仍保留原始 locale 字符串）。`SPELL_AMOUNT` **固定人民币中文大写**，不随 locale 切换语言。 | 计划卡；CE-C01 S-3 |
| **K03-C12** | **SPELL_AMOUNT（CNY 大写）边界表（必须覆盖）：** | 计划卡 |
| | **0** → `零元整` | |
| | **整元**（无角分，如 `1.00` / `100`）→ `…元整` | |
| | **角分**（如 `1.23`）→ 含「角」「分」的规范大写（实现须与金标期望字面量一致） | |
| | **亿级**（如 `100000000` / `1.5e8` 等价十进制）→ 正确含「亿」 | |
| | **负数** → **拒绝**（`VARIABLE_COMPUTE_FAILED`），不输出「负」字前缀成功件 | |
| | 超过引擎支持的最大绝对值（实现固定，建议 ≤ 9999999999999.99）→ 失败 | |
| | 非数值 / null → 失败 | |
| **K03-C13** | **字面量与类型：** 数值字面量支持十进制；字符串用单引号或双引号（实现固定一种并在校验器一致）；布尔 `true`/`false`；`null`。金额输入允许 NUMBER/AMOUNT 绑定值。 | TDD 可测性 |
| **K03-C14** | **管理端即时校验：** 当变量类型为 `COMPUTED`（或编辑 compute 表达式时），面板对表达式做：**语法合法**（仅白名单构造）+ **引用变量存在性**（`${path}` 根/路径在当前模板变量树可解析）。非法时保存前可见错误（inline / form error）；**不得**静默保存明显非法表达式。授权：既有模板变量编辑权限；不新增独立权限码（除非 permission-matrix 已有更细码——本片不扩张权限模型）。 | 计划卡「管理端」 |
| **K03-C15** | **样例求值预览：** 面板提供「样例求值」动作（按钮或等价）：用作者提供的**样例 JSON**（或当前测试集精简样本）在**不落文档**的前提下调用后端预览求值 API（或同源服务），展示该表达式结果或失败原因。失败展示与 `VARIABLE_COMPUTE_FAILED` 信息对齐（变量 key + 摘要）。English-first i18n。 | 计划卡 |
| **K03-C16** | **保存期校验（作者路径）：** 创建/更新变量 schema 时，若含 `computeExpression`：后端同样执行语法 + 引用存在性 + 长度/深度上限校验；失败 → `TEMPLATE_VALIDATION_FAILED`（或专用子码），**不**写入非法表达式。运行时仍保留求值 fail-closed（防导入绕过、并发变更）。 | 计划 + fail-closed |
| **K03-C17** | **金标：** `04-compute-variables` → **ACTIVE**：至少覆盖 COALESCE/SUM/COUNT/AVG/FILTER/FORMAT_* 中的代表性求值结果写入 DOCX 文本断言。`05-chinese-uppercase-amount` → **ACTIVE**：断言 `SPELL_AMOUNT` 大写文本出现在 DOCX（及 PDF 文本抽取，无 soffice 可 skip，对齐 K07）。Harness 复用 K07，不新建第二套 golden 根。 | 计划卡；K07 |
| **K03-C18** | **单测矩阵：** 每个白名单函数 ≥ **5** 个边界用例（含成功与失败）；`SPELL_AMOUNT` 必须覆盖 K03-C12 全表。 | 计划卡「测试」 |
| **K03-C19** | **E2E：** 管理端：定义 compute 变量 → 即时校验可见 → 样例求值可见 →（可选）test-generate 产物含计算文本。Runtime：schema 定义 compute → generate → DOCX 文本含求值结果；失败路径断言 `VARIABLE_COMPUTE_FAILED`。 | 计划卡；交付管线 FE E2E |
| **K03-C20** | **ADR（强制）：** 本片 Done 前必须新增 Accepted ADR（建议路径 `docs/adr/rendering-authoring/` 或 `docs/adr/technology-stack/`），记录：白名单函数集、禁 Groovy/JS/SpEL、长度/深度上限、FILTER 四元形式、locale 默认 zh-CN、SPELL_AMOUNT CNY-only。ADR **决策文本**写边界，不写任务完成状态。 | 计划卡「ADR 记录 DSL 边界」 |
| **K03-C21** | **本片禁止 / 非目标：** 任意脚本引擎；用户自定义函数注册；多币种 SPELL；循环语法；像素金标；go-live；CD-3；CE-K04/K05/K06 行为；改写 demo builder；改变 U03「跳过 compute 录入」语义（仅消费其约定）。 | 计划卡 |

---

## 5. 前置条件

- CE-C01/C02 Done：`context.locale` 可接受并进入审计摘要；本片开始消费其值做 FORMAT_*。
- CE-U03 Done：测试集对 COMPUTED / 非空 `computeExpression` 跳过录入（弱耦合保持）。
- CE-K07 骨架 Done：`04-compute-variables`、`05-chinese-uppercase-amount` 目录存在且为 PLACEHOLDER，可充实为 ACTIVE。
- 变量 schema 已支持 `COMPUTED` 与 `computeExpression` 持久化（现状）。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- Template Author 在变量面板编辑/保存 `COMPUTED` 表达式，或点击样例求值。
- Runtime 调用方对含 compute 变量的已发布模板发起 sync/async/batch 生成。
- 管理端 preview / test-generate / batch-test 装配。
- `mvn verify` 执行 golden-corpus（含 ACTIVE compute / SPELL_AMOUNT 包）。

---

## 7. Primary journey（定义 → 求值 → 装配）

1. Template Author 定义输入变量（如 `principal` AMOUNT）与 compute 变量（如 `principalCn` = `SPELL_AMOUNT(${principal})`）。
2. 面板即时校验通过；样例求值预览显示大写金额。
3. Author 保存 schema；后端校验通过并持久化。
4. Runtime（或 preview）收到生成请求：`variables` 含 `principal`，无 `principalCn`；可选 `context.locale`。
5. 装配前求值层计算全部 compute 变量，注入上下文。
6. 渲染装配 DOCX；产物文本含格式化/大写结果。
7. 金标 ACTIVE 包在 `mvn verify` 中断言上述行为。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| 合法表达式 + 完整输入 | 求值成功；绑定含 compute 结果；装配继续 |
| 语法非法 / 未知函数 / 超限 | 保存期校验失败 **或** 运行时 `VARIABLE_COMPUTE_FAILED` |
| 引用缺失变量 / 环依赖 / 类型错误 | `VARIABLE_COMPUTE_FAILED`；无成功产物 |
| `SPELL_AMOUNT` 负数 | `VARIABLE_COMPUTE_FAILED` |
| `FORMAT_*` 无 locale | 使用 `zh-CN` |
| 面板非法表达式 | 即时错误；阻止或明确拒绝保存 |
| 样例求值失败 | 面板展示失败原因（对齐错误摘要） |
| 金标断言失败 | `mvn verify` 红 |

---

## 9. 验收场景（Given / When / Then）

### A. DSL 白名单与安全边界

#### BDD-CE-K03-001 — 未知函数名求值失败

**Given** compute 变量 `x` 的表达式为 `FOO(${a})`（`FOO` 不在白名单）  
**When** 运行时或预览求值  
**Then** 返回 `VARIABLE_COMPUTE_FAILED`  
**And** 错误可观察变量 key=`x` 与表达式摘要  
**And** 不产出成功 DOCX/PDF

#### BDD-CE-K03-002 — 禁止任意方法调用

**Given** 表达式含 `${a}.toString()` 或等价任意方法调用构造  
**When** 校验或求值  
**Then** 失败（保存校验失败或 `VARIABLE_COMPUTE_FAILED`）  
**And** 不执行任意代码

#### BDD-CE-K03-003 — 表达式超长拒绝

**Given** `computeExpression` 长度 > 2048  
**When** 保存变量 schema 或求值  
**Then** 拒绝（保存失败或 `VARIABLE_COMPUTE_FAILED`）

#### BDD-CE-K03-004 — 嵌套深度超限拒绝

**Given** 白名单函数嵌套深度 > 8  
**When** 校验或求值  
**Then** 拒绝（保存失败或 `VARIABLE_COMPUTE_FAILED`）

#### BDD-CE-K03-005 — 不引入脚本引擎

**Given** 仓库本片变更集  
**When** 架构/依赖审查  
**Then** 无 Groovy/JS 脚本引擎/完整 SpEL 求值依赖作为 compute 实现  
**And** DSL bounds ADR（K03-C20）已 Accepted 并写明该禁令

---

### B. 核心函数语义

#### BDD-CE-K03-006 — COALESCE 取首个非空

**Given** 表达式 `COALESCE(${a}, ${b}, 'N/A')`，绑定 `a` 缺失、`b`=`"ok"`  
**When** 求值  
**Then** 结果为 `"ok"`

#### BDD-CE-K03-007 — SUM / COUNT / AVG 数值集合

**Given** `${nums}` = `[1, 2, 3]`  
**When** 分别求值 `SUM(${nums})`、`COUNT(${nums})`、`AVG(${nums})`  
**Then** 结果分别为 `6`、`3`、`2`（AVG 允许等价十进制表示）

#### BDD-CE-K03-008 — AVG 空集合失败

**Given** `${nums}` = `[]`  
**When** 求值 `AVG(${nums})`  
**Then** `VARIABLE_COMPUTE_FAILED`

#### BDD-CE-K03-009 — FILTER + SUM 组合

**Given** `${items}` 为对象列表，含 `amount` 字段；表达式 `SUM(FILTER(${items}, amount, GT, 0))`  
**When** 求值  
**Then** 结果等于所有 `amount > 0` 的元素之和

#### BDD-CE-K03-010 — FILTER 非法 op 失败

**Given** `FILTER(${items}, amount, LIKE, 'x')`（`LIKE` 非白名单 op）  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`

---

### C. FORMAT_* 与 locale

#### BDD-CE-K03-011 — FORMAT_AMOUNT 默认 zh-CN

**Given** 请求无 `context.locale`（或空白）  
**And** 表达式 `FORMAT_AMOUNT(${principal})`，`principal=1234.5`  
**When** 求值  
**Then** 结果为按 **zh-CN** 规则的金额展示字符串（金标/单测固定期望字面量）

#### BDD-CE-K03-012 — FORMAT_AMOUNT 尊重 context.locale

**Given** `context.locale=en-US`  
**And** 同一 `FORMAT_AMOUNT(${principal})`  
**When** 求值  
**Then** 结果按 **en-US** 金额展示规则（与 zh-CN 结果可区分）

#### BDD-CE-K03-013 — FORMAT_DATE 默认 zh-CN

**Given** 无 locale；`FORMAT_DATE(${signDate})`，`signDate` 为合法日期  
**When** 求值  
**Then** 结果为 zh-CN 日期展示字符串（单测固定期望）

---

### D. SPELL_AMOUNT（人民币大写）

#### BDD-CE-K03-014 — SPELL_AMOUNT 零元

**Given** `SPELL_AMOUNT(${principal})`，`principal=0`（或 `0.00`）  
**When** 求值  
**Then** 结果为 `零元整`

#### BDD-CE-K03-015 — SPELL_AMOUNT 整元

**Given** `principal=100`（无角分）  
**When** 求值 `SPELL_AMOUNT(${principal})`  
**Then** 结果以 `元整` 结尾，且符合人民币大写规范（金标固定完整字面量）

#### BDD-CE-K03-016 — SPELL_AMOUNT 角分

**Given** `principal=1.23`  
**When** 求值 `SPELL_AMOUNT(${principal})`  
**Then** 结果含角、分对应大写（金标固定完整字面量）

#### BDD-CE-K03-017 — SPELL_AMOUNT 亿级

**Given** `principal=100000000`  
**When** 求值 `SPELL_AMOUNT(${principal})`  
**Then** 结果含「亿」且数值正确（金标固定完整字面量）

#### BDD-CE-K03-018 — SPELL_AMOUNT 负数拒绝

**Given** `principal=-1`  
**When** 求值 `SPELL_AMOUNT(${principal})`  
**Then** `VARIABLE_COMPUTE_FAILED`  
**And** 不产出含负金额大写的成功件

---

### E. 求值时机、依赖与 fail-closed

#### BDD-CE-K03-019 — 装配前求值并写入产物

**Given** 已发布模板：输入 `principal`，compute `principalCn=SPELL_AMOUNT(${principal})`，结构化/锚点绑定展示 `principalCn`  
**When** runtime sync（或等价）生成，`variables.principal=100`  
**Then** 产物 DOCX 文本含 `SPELL_AMOUNT` 对应大写  
**And** 求值发生在装配之前（可观察：无「裸表达式字符串」写入正文）

#### BDD-CE-K03-020 — Preview / test-generate 同样求值

**Given** 同上模板与测试数据  
**When** 管理端 test-generate / preview 装配  
**Then** 预览产物同样含求值后文本（非原始表达式）  
**And** （若 CE-G02 已合并）预览路径仍施加 SPECIMEN；本片不取消水印

#### BDD-CE-K03-021 — 求值失败 fail-closed

**Given** compute 表达式引用不存在的变量 `${missing}`  
**When** runtime 或 preview 生成  
**Then** `VARIABLE_COMPUTE_FAILED`（含变量 key + 表达式摘要）  
**And** 无成功 artifact；preview 状态非成功

#### BDD-CE-K03-022 — compute 变量环依赖失败

**Given** `a` 表达式引用 `${b}`，`b` 表达式引用 `${a}`  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`

#### BDD-CE-K03-023 — 调用方未传 compute 键仍成功

**Given** schema 含 COMPUTED `principalCn`  
**And** 请求 `variables` **仅**含输入变量、不含 `principalCn`  
**When** 生成  
**Then** 求值注入 `principalCn` 后装配成功（对齐 U03 跳过录入）

---

### F. 管理端变量面板

#### BDD-CE-K03-024 — 即时语法校验

**Given** Template Author 打开变量面板，类型为 `COMPUTED`  
**When** 输入非法表达式（未知函数或非法构造）  
**Then** 面板即时展示校验错误（保存前可见）  
**And** 文案 English-first（i18n key）

#### BDD-CE-K03-025 — 引用变量存在性校验

**Given** 模板仅有变量 `principal`  
**When** 输入 `SUM(${notExists})`  
**Then** 面板提示引用不存在  
**And** 后端保存同样拒绝

#### BDD-CE-K03-026 — 样例求值预览成功

**Given** 合法表达式 `FORMAT_AMOUNT(${principal})`  
**And** 样例 JSON `{"principal": 100}`  
**When** Author 触发样例求值  
**Then** 面板展示非空格式化结果  
**And** 不创建正式 invocation 成功文档（无正式 generated 产物义务）

#### BDD-CE-K03-027 — 样例求值预览失败

**Given** 表达式 `SPELL_AMOUNT(${principal})`，样例 `{"principal": -1}`  
**When** 触发样例求值  
**Then** 面板展示失败原因（对齐 compute 失败摘要）  
**And** 不假装成功

---

### G. 金标与门禁

#### BDD-CE-K03-028 — compute-variables 金标 ACTIVE

**Given** 包 `backend/src/test/resources/golden-corpus/04-compute-variables`  
**When** 本片交付完成  
**Then** manifest `maturity=ACTIVE`  
**And** expected 断言含至少一种白名单求值结果文本  
**And** `mvn verify` 执行该包且通过

#### BDD-CE-K03-029 — chinese-uppercase-amount 金标 ACTIVE

**Given** 包 `05-chinese-uppercase-amount`  
**When** 本片交付完成  
**Then** manifest `maturity=ACTIVE`  
**And** expected 断言含 `SPELL_AMOUNT` 大写文本  
**And** `mvn verify` 执行该包且通过

#### BDD-CE-K03-030 — 函数单测矩阵门槛

**Given** 表达式引擎单元测试套件  
**When** `mvn verify`  
**Then** 每个白名单函数至少 5 个边界用例绿灯  
**And** `SPELL_AMOUNT` 覆盖 BDD-CE-K03-014…018

---

## 10. 边界与异常（汇总）

| 边界 | 行为 |
| --- | --- |
| 未知函数 / 非法构造 / 超限 | 失败；不执行 |
| 缺失引用 / 环依赖 / 类型错误 | `VARIABLE_COMPUTE_FAILED` |
| FILTER 非法 op | 失败 |
| AVG 空集 | 失败 |
| SUM/COUNT 空集 | `0` |
| SPELL_AMOUNT 负/非数/超大 | 失败 |
| locale 缺失 | FORMAT_* → zh-CN |
| SPELL_AMOUNT 与 locale | 固定 CNY 中文大写，不随 locale 改语言 |
| 授权失败 | 既有 fail-closed；本片不削弱 |
| 脚本引擎 | 禁止 |

---

## 11. 可观察证据

| 证据 | 说明 |
| --- | --- |
| API error | `VARIABLE_COMPUTE_FAILED` + `messageKey=api.error.variable.computeFailed` + 变量 key + 表达式摘要 |
| DOCX/PDF 文本 | 求值后金额/日期/大写出现在产物；无裸表达式 |
| 管理端 UI | 即时校验错误；样例求值结果/失败 |
| 单测 | 每函数 ≥5 边界；SPELL_AMOUNT 边界表 |
| 金标 | `04-compute-variables`、`05-chinese-uppercase-amount` ACTIVE |
| ADR | DSL bounds Accepted |
| Gates | `mvn verify`；FE `lint` / `type-check` / `test` / `build`；E2E + UIUX（面板用户面） |

---

## 12. Traceability

| 源 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §3 CE-K03 | 计划目标与禁止项 |
| Task Master **#59** | 执行任务 |
| [ce-c01-c02-contract-strictness.md](./ce-c01-c02-contract-strictness.md) | `context.locale` 上游 |
| [ce-u03-testdata-schema-form.md](./ce-u03-testdata-schema-form.md) U03-C8 | compute 跳过录入弱耦合 |
| [ce-k07-golden-corpus-skeleton.md](./ce-k07-golden-corpus-skeleton.md) | 金标包充实责任 |
| 本片 DSL bounds ADR（待建，K03-C20） | 决策固化 |
| OpenAPI / `ApiErrorCodes` | 新增 `VARIABLE_COMPUTE_FAILED` |

---

## 13. Out of scope

- Groovy / JS / SpEL 完整引擎或用户自定义函数插件。
- 多币种 SPELL（非 CNY）或英文金额 spell。
- CE-K04 语义 diff、CE-K05 impact、CE-K06 保真 writer。
- Demo builder 清理；go-live；CD-3；正式 P-phase 发明。

---

## 14. BDD readiness

| Field | Value |
| --- | --- |
| **bdd_readiness** | **`ready`** |
| **open_questions** | _无（计划卡 + 用户 scope 已足够锁定；FILTER 四元形式与数值上限已在 K03-C8/C10 确认）_ |
| **owning_doc** | `docs/behavior/ce-k03-variable-compute-engine.md` |
| **task_ids** | `#59`（CE-K03） |
| **scenario_ids** | `BDD-CE-K03-001` … `BDD-CE-K03-030` |
| **next** | `plan-orchestrator` → 实现切片（TDD Red 起于本场景）；交付中落 DSL ADR（K03-C20） |

**Handoff note：** 行为已持久化于本文件；可进入计划分解与引擎单测 Red。实现前勿在 MAIN 写代码。
