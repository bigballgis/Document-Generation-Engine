# BDD 行为规格：CORE-FORTRESS Phase F3 — 节点矩阵 + 表达式引擎

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-09  
**BDD ID**: `BDD-CORE-FORTRESS-F3-001`  
**来源**: 代码库深度审查 + CORE-FORTRESS 纲领 + 用户确认 F3-C1…F3-C7

---

## 1. 概述

本规格定义 **CORE-FORTRESS Phase F3** 的两项关联改造：将条件表达式从「仅 `${var} == true|false`」升级为 **安全子集表达式引擎**，并在节点矩阵校验层 **fail-closed** 拦截畸形表达式与未声明循环变量。

| 工作流 | 改造要点 |
| --- | --- |
| **F3-E1 共享表达式引擎** | 新建 `ConditionExpressionEvaluator`（单一实现）；解析、变量提取、求值逻辑不得重复 |
| **F3-E2 运行时条件求值** | `StructuredContentDocxWriter` 替换 `evaluateSimpleCondition()`；支持富条件；畸形表达式按 false 处理 |
| **F3-E3 节点矩阵校验加固** | `NodeMatrixValidationService` 校验 `conditionBlock.conditionExpression` 语法与变量引用；校验 `loopBlock.loopVariable` 与 schema |
| **F3-E4 模板规则表达式校验** | `TemplateRuleValidationService` 复用同一解析器校验 `conditionExpression` 语法（不仅提取 `${var}`） |

**与现有代码的关系**

| 现有资产 | 本规格用法 |
| --- | --- |
| `StructuredContentDocxWriter.evaluateSimpleCondition()` + `SIMPLE_CONDITION_PATTERN` | **Replace** — 委托 `ConditionExpressionEvaluator` |
| `NodeMatrixValidationService.walkNode()` | **Extend** — conditionBlock / loopBlock 分支校验 |
| `TemplateRuleValidationService.VARIABLE_REFERENCE` 仅提取引用 | **Extend** — 增加语法校验；仍校验变量声明 |
| `TemplateBindingConfigurationService.computeBindingStatus()` | **Reuse** — 已有 `nodeMatrixValidationService.validate()` 阻断发布路径 |
| `PublishGateService` anchor integrity / blocker status | **Reuse** — 畸形表达式经 binding `INCOMPATIBLE_CONTENT_TYPE` 阻断发布 |
| `FidelityWarningCode` enum | **Extend** — 新增 `INVALID_CONDITION_EXPRESSION` |

**明确不在 F3 范围**

- 模板规则运行时分支执行（规则引擎求值属既有 lifecycle 能力；F3 仅加固 **校验 + structured content 渲染求值**）
- SpEL / JavaScript / 任意脚本执行（**F3-C7 禁止**）
- LO 池化、字体、分页（F4）
- 前端表达式编辑器 UX（F7）

---

## 2. Actor / Role

| Actor | 说明 | 权限 |
| --- | --- | --- |
| **模板编排人员** | 在 structured content 中配置 `conditionBlock` / `loopBlock`；在模板规则中编写 `conditionExpression` | 模板写 + 组范围 |
| **模板测试人员** | 绑定校验、预览/测试生成，验证条件内容显隐 | 测试决策权限 |
| **运行时 API 调用方** | 传入变量数据触发生成 | API 凭证 + AD Group |
| **系统（发布门禁）** | 绑定校验 + 规则校验聚合，阻断不可发布版本 | `PublishGateService` |

---

## 3. Goal

1. **单一表达式真相源**：渲染与校验共用 `ConditionExpressionEvaluator`，消除 regex 漂移。
2. **富条件可用**：`${customerName} != null`、`${amount} >= 1000`、`${showNotice} == true`、`(${a} && ${b}) || !${c}` 等在 structured content 条件块中可正确求值。
3. **校验 fail-closed**：畸形表达式、未声明 `${var}`、`loopVariable` 未在 schema 声明 → 发布门禁阻断。
4. **运行时安全降级**：校验未覆盖路径或历史数据中的畸形表达式 → 条件块 **不输出子内容**（等同 false），仅 debug 日志，不抛错、不静默误显。
5. **向后兼容**：现有 `${showNotice} == true` 表达式行为不变。

---

## 4. 已确认决策（2026-07-09）

| ID | 决策 |
| --- | --- |
| **F3-C1** | 共享 `ConditionExpressionEvaluator` 位于 `com.bank.docgen.authoring.structured`（或 `com.bank.docgen.sharedkernel.expression`），由 **`StructuredContentDocxWriter` 与全部校验服务** 共用 |
| **F3-C2** | 支持运算符：`==`、`!=`、`>`、`>=`、`<`、`<=`（数字/字符串/布尔）；`!= null` / `== null`；逻辑 `&&`、`||`、`!` 与括号；操作数仅为 `${var}` 引用与字面量（数字、布尔、`null`） |
| **F3-C3** | 校验期畸形表达式 **fail-closed**（发布门禁阻断）；稳定码 `INVALID_CONDITION_EXPRESSION`；`FidelityWarningCode.INVALID_CONDITION_EXPRESSION`；messageKey `generation.warning.fidelity.invalidConditionExpression` |
| **F3-C4** | 运行时畸形表达式 → 条件块视为 **false**（不发射子内容）；**仅 debug 日志**，不抛 API 错误 |
| **F3-C5** | `NodeMatrixValidationService` 扩展：校验 `conditionBlock.conditionExpression` 语法与 `${var}` 引用；校验 `loopBlock.loopVariable` 在模板 schema 中声明；递归提取表达式内全部 `${var}` |
| **F3-C6** | 向后兼容：现有 `${showNotice} == true` / `false` 表达式继续工作 |
| **F3-C7** | **禁止** SpEL、脚本引擎、反射调用、任意代码执行；仅安全 AST 子集解释器 |

### 表达式语法子集（F3-C2 可操作定义）

```text
expression  := orExpr
orExpr      := andExpr ( '||' andExpr )*
andExpr     := unaryExpr ( '&&' unaryExpr )*
unaryExpr   := '!' unaryExpr | comparison
comparison  := '${' varName '}' compOp rhs
             | '(' expression ')'
compOp      := '==' | '!=' | '>' | '>=' | '<' | '<='
rhs         := 'null' | 'true' | 'false' | NUMBER | STRING_LITERAL
varName     := [A-Za-z][A-Za-z0-9_.-]*
```

- 变量引用 **必须** 使用 `${varName}` 形式；不支持裸标识符。
- 字符串字面量使用单引号：`'approved'`（用于与字符串变量比较）。
- 数字字面量：整数或小数（如 `100`、`1000.50`）。
- 比较遵循 Java 常规类型规则（见 §10 待确认项默认）。

---

## 5. 前置条件

- **CORE-FORTRESS F1 Done**：单一 `StructuredContentDocxWriter` 渲染路径。
- **CORE-FORTRESS F2 Done**：发布期 fidelity 警告缓存；本 Phase 新增的 `INVALID_CONDITION_EXPRESSION` **为 blocker**，不进入 warning 缓存。
- P18 节点矩阵：`conditionBlock`、`loopBlock` 已在 `StructuredContentNodeType` 登记。
- 模板变量 schema（`variable_schema`）为 `${var}` 与 `loopVariable` 校验的声明源。

---

## 6. 验收场景（Given / When / Then）

### F3-E1 — 共享表达式引擎

#### BDD-F3-E1-001 — 解析并提取变量引用

**Given** 表达式 `${customerName} != null && ${amount} >= 1000`  
**When** 调用 `ConditionExpressionEvaluator.extractVariableReferences(expression)`  
**Then** 返回 `["customerName", "amount"]`（去重、保序）  
**And** 无重复 regex 实现存在于 writer / validation 服务中

#### BDD-F3-E1-002 — 语法校验通过合法表达式

**Given** 表达式 `${showNotice} == true`  
**When** 调用 `ConditionExpressionEvaluator.validateSyntax(expression)`  
**Then** 返回空错误（valid）

#### BDD-F3-E1-003 — 语法校验拒绝非法表达式

**Given** 表达式 `${x} === true` 或 `${x} @ null` 或 `customerName != null`（缺 `${}`）  
**When** 调用 `validateSyntax(expression)`  
**Then** 返回语法错误  
**And** 错误可映射为 `INVALID_CONDITION_EXPRESSION`

#### BDD-F3-E1-004 — 布尔与 null 求值

**Given** 变量 `{ "showNotice": true, "customerName": "Alice", "optional": null }`  
**When** 求值 `${showNotice} == true`、`${customerName} != null`、`${optional} == null`  
**Then** 均返回 `true`

#### BDD-F3-E1-005 — 数值与字符串比较

**Given** 变量 `{ "amount": 1500, "status": "approved" }`  
**When** 求值 `${amount} >= 1000`、`${status} == 'approved'`  
**Then** 均返回 `true`

#### BDD-F3-E1-006 — 逻辑组合与括号

**Given** 变量 `{ "a": true, "b": false, "c": true }`  
**When** 求值 `(${a} && ${c}) || !${b}`  
**Then** 返回 `true`

#### BDD-F3-E1-007 — 禁止代码注入

**Given** 表达式 `${x}.class.getName()` 或 `T(java.lang.Runtime).getRuntime()`  
**When** 调用 `validateSyntax(expression)`  
**Then** 拒绝（语法错误或安全拒绝）  
**And** 不调用任何脚本/SpEL 引擎

---

### F3-E2 — 运行时 structured content 条件求值

#### BDD-F3-E2-001 — 富条件块显隐（非 null）

**Given** structured content 含 `conditionBlock`，`conditionExpression` 为 `${customerName} != null`  
**And** 运行时变量 `customerName = "Alice"`  
**When** 生成 DOCX  
**Then** 条件块子内容出现在输出中

#### BDD-F3-E2-002 — 富条件块隐藏

**Given** 同上，`customerName` 为 null 或缺失  
**When** 生成 DOCX  
**Then** 条件块子内容 **不出现**

#### BDD-F3-E2-003 — 向后兼容布尔相等

**Given** `conditionExpression` 为 `${showNotice} == true`，`showNotice = true`  
**When** 生成 DOCX  
**Then** 与 F1 基线一致：子内容出现  
**And** `showNotice = false` 时子内容不出现

#### BDD-F3-E2-004 — 运行时畸形表达式 fail-safe

**Given** 绑定含 `conditionExpression: "${broken} === true"`（未经过校验的历史数据或测试注入）  
**When** 生成 DOCX  
**Then** 条件块子内容 **不出现**（等同 false）  
**And** 生成 **成功**（不抛 `DocxAssemblyException`）  
**And** debug 日志含表达式求值失败记录（无敏感变量值）

#### BDD-F3-E2-005 — loopBlock 不受表达式引擎影响

**Given** 合法 `loopBlock`，`loopVariable = "items"`，`items` 为含 2 元素的数组  
**When** 生成 DOCX  
**Then** 循环体渲染 2 次（F1 回归不退化）

---

### F3-E3 — 节点矩阵校验加固

#### BDD-F3-E3-001 — 畸形 conditionExpression 为 blocker

**Given** structured content 含 `conditionBlock`，`conditionExpression` 为 `${x} === true`  
**And** `x` 已在 schema 声明  
**When** `NodeMatrixValidationService.validate(json, declaredKeys)`  
**Then** `blockers` 含一项：`code = INVALID_CONDITION_EXPRESSION`  
**And** `messageKey = generation.warning.fidelity.invalidConditionExpression`  
**And** `location` 指向该 conditionBlock 节点路径

#### BDD-F3-E3-002 — 未声明变量引用为 blocker

**Given** `conditionExpression` 为 `${missingVar} != null`  
**And** `missingVar` **未**在 schema 声明  
**When** 节点矩阵校验  
**Then** blocker 为 `UNRESOLVED_VARIABLE`（变量校验优先或并列；至少一项 blocker 阻断发布）

#### BDD-F3-E3-003 — 合法富表达式通过校验

**Given** `conditionExpression` 为 `${customerName} != null && ${amount} >= 0`  
**And** 两变量均已声明  
**When** 节点矩阵校验  
**Then** 无 `INVALID_CONDITION_EXPRESSION` blocker

#### BDD-F3-E3-004 — 未声明 loopVariable 为 blocker

**Given** `loopBlock`，`loopVariable` 为 `undeclaredItems`  
**And** schema 无 `undeclaredItems`  
**When** 节点矩阵校验  
**Then** blocker：`UNRESOLVED_VARIABLE`（location 含 loopBlock 路径）

#### BDD-F3-E3-005 — 合法 loopVariable 通过

**Given** `loopBlock`，`loopVariable` 为 `items`，schema 已声明 `items`（类型 ARRAY/OBJECT）  
**When** 节点矩阵校验  
**Then** 无 loop 相关 blocker

#### BDD-F3-E3-006 — 绑定校验阻断发布

**Given** 模板版本含畸形 `conditionBlock` 绑定  
**When** `validateBindings` → `PublishGateService.evaluate`  
**Then** `ANCHOR_INTEGRITY` 或 `BLOCKER_STATUS` 为 blocked  
**And** 版本不可 transition 到 `PUBLISHED`

#### BDD-F3-E3-007 — 嵌套 conditionBlock 校验

**Given** `conditionBlock` 子节点内嵌套另一 `conditionBlock`，内层表达式畸形  
**When** 节点矩阵校验  
**Then** 内层 location 精确报告（如 `...children[0].children[1]`）

---

### F3-E4 — 模板规则表达式校验

#### BDD-F3-E4-001 — 合法规则表达式通过

**Given** 规则 `conditionExpression` 为 `${customerName} != null`，`customerName` 已声明  
**When** `TemplateRuleValidationService.validateRules`  
**Then** 该规则 `status = VALID`

#### BDD-F3-E4-002 — 畸形规则表达式为 MALFORMED_RULE

**Given** 规则 `conditionExpression` 为 `${customerName} === null`  
**When** 规则校验  
**Then** `status = MALFORMED_RULE`  
**And** `summary.blocking = true`  
**And** 发布门禁 `RULE_BOUNDS` 项 blocked

#### BDD-F3-E4-003 — 缺失变量仍为 MISSING_VARIABLE

**Given** 表达式 `${missing} != null`，语法合法但变量未声明  
**When** 规则校验  
**Then** `status = MISSING_VARIABLE`（语法通过、变量检查失败）

#### BDD-F3-E4-004 — 与节点矩阵共用解析器

**Given** 同一表达式 `${amount} >= 100`  
**When** 分别经 `ConditionExpressionEvaluator.validateSyntax` 与规则/节点矩阵校验路径  
**Then** 解析结果一致（无 divergent regex）

---

### F3-E5 — 安全与 i18n

#### BDD-F3-E5-001 — i18n 英文基线

**Given** `messages_en.properties`  
**When** 查找 `generation.warning.fidelity.invalidConditionExpression`  
**Then** 存在稳定英文文案  
**And** 不含运行时变量值占位

#### BDD-F3-E5-002 — 校验摘要不含敏感数据

**Given** 条件表达式引用变量 `customerName`，检测摘要生成  
**When** 输出 blocker `detectionSummary`  
**Then** 仅含变量 **键名** 与表达式片段，不含测试数据明文值

---

## 7. 边界与异常行为

| 场景 | 期望行为 |
| --- | --- |
| `conditionExpression` 为空或空白 | 校验：**blocker**（`INVALID_CONDITION_EXPRESSION`）；运行时：false |
| 变量缺失于运行时 payload | 视为 `null` 参与比较（与现有 variable 节点行为一致） |
| 非布尔非 null 比较 `== true` | 使用 `Boolean.parseBoolean(String.valueOf(value))` 兼容 F3-C6 |
| 数字与字符串比较 | 见 §10 F3-Q1 默认：先尝试数值解析，失败则字符串 lexicographic |
| `loopBlock` 变量运行时非集合 | 现有 writer 行为不变（不渲染或空迭代——F1 回归） |
| 预览 vs 最终生成 | 条件求值行为 **一致** |
| 模板规则仅校验不渲染 | F3 不改变规则运行时执行语义；仅加固保存前/门禁校验 |

---

## 8. 可观测证据

| 证据类型 | 路径 / 命令 |
| --- | --- |
| 表达式引擎单元测试 | `ConditionExpressionEvaluatorTest`（新建） |
| Writer 回归 | 扩展 `StructuredContentDocxWriterTest`（富条件 + 畸形 fail-safe） |
| 节点矩阵 | 扩展 `NodeMatrixValidationServiceTest` |
| 规则校验 | 扩展 `TemplateRuleValidationServiceLogicTest` |
| 发布门禁 | 扩展 `PublishGateServiceTest` / `TemplateBindingConfigurationServiceTest` |
| 门禁 | `mvn -B -ntp -f backend/pom.xml verify` |
| 计划 | `docs/plan/detail/CORE-FORTRESS-f3-node-matrix-expression.md` |

---

## 9. 追溯性

| 文档 | 关联 |
| --- | --- |
| `docs/behavior/core-fortress-f1-rendering-correctness.md` | F1-C8 将表达式引擎推迟至 F3 |
| `docs/behavior/core-fortress-f2-runtime-lightweight.md` | F2 完成后的下一 Phase |
| `docs/plan/detail/CORE-FORTRESS-program-roadmap.md` | F3 程序路线图 |
| `docs/plan/master-plan.md` | 正式 Phase `CORE-FORTRESS-F3-NODE-MATRIX-EXPRESSION` |
| `docs/architecture/module-boundaries.md` | 表达式放 sharedkernel vs authoring 的包边界 |
| ADR-0019 / P18 节点矩阵 | `StructuredContentNodeType` conditionBlock / loopBlock |
| `TemplatePlatformSliceTest` | 已有 `${customerName} != null` 规则保存用例 |

---

## 10. 待确认问题

| ID | 问题 | 默认（若无异议） |
| --- | --- | --- |
| **F3-Q1** | 混合类型比较规则（数字 vs 数字字符串）？ | 双方可解析为 `BigDecimal` 则数值比较；否则字符串比较 |
| **F3-Q2** | 是否支持双引号字符串字面量？ | 仅单引号（与 SQL/银行模板习惯一致） |
| **F3-Q3** | `ConditionExpressionEvaluator` 最终包名？ | `com.bank.docgen.authoring.structured.expression`；若 rendering→authoring 依赖违规则迁至 `sharedkernel.expression` |
| **F3-Q4** | 表格 `loopRow.loopVariable` 是否纳入 F3 校验？ | **是** — `TableComponentService.validateStructuredContent` 委托同一变量声明检查 |
| **F3-Q5** | 规则校验畸形表达式用 `MALFORMED_RULE` 还是新 `RuleValidationStatus`？ | 保持 `MALFORMED_RULE`；structured content 用 `INVALID_CONDITION_EXPRESSION` fidelity code |
