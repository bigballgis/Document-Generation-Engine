# BDD 行为规格：IBL-A3 — International amount-in-words + `en` locale

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-A3` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | IBL Wave A · **IBL-A3** / F3（`ibl-a3-amount-in-words`） |
| **Slice** | `ibl-a3-amount-in-words` |
| **Branch** | `feat/ibl-a3-amount-in-words` |
| **Worktree** | `D:/working/DGE-ibl-a3-amount-in-words` |
| **Base** | `38312c4f`（handoff） |
| **Placement** | ISOLATED |
| **Task Master** | **#109** IBL-A3 — Batch Recommendation **solo**；`member_task_ids: ["109"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-a3-amount-in-words`；vetoes: `different-acceptance-vs-A4-A5-A6`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F3 / IBL-A3；上游 DSL [ce-k03-variable-compute-engine.md](./ce-k03-variable-compute-engine.md)（K03-C11/C12 一元 CNY 中文大写保留）；上游币种 [ibl-a2-format-amount-currency.md](./ibl-a2-format-amount-currency.md)（ISO 第二参模式对齐）；边界 [ADR-0056](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md)（本叶实现同期修订 SPELL_AMOUNT 国际化）；契约 [openapi-v1.yaml](../api/openapi-v1.yaml) / [contract-outline.md](../api/contract-outline.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（本叶为 compute DSL / BE 求值契约；E2E/UIUX **N/A**） |

**完成声明约束：** 本叶关闭 F3——amount-in-words 至少支持 **en + USD**，并具备可扩展的 (language × currency) 框架；**CNY / zh** 路径保持正确；默认语言/locale 行为**显式文档化**，禁止静默错语言。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave A / 程序 Done；**禁止**把 A4–A6 并入本叶。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["109"]
  proposed_slice_id: ibl-a3-amount-in-words
  shared_acceptance_surface: amount-in-words en+USD + CNY/zh regression
  vetoes_applied:
    - different-acceptance-vs-A4-A5-A6
  evidence_amortization: mvn verify + docker
  on_red_split_hint: N/A
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| DSL：`SPELL_AMOUNT(value)` 一元 **CNY 中文大写兼容**（CE-K03 / 金标 `05-chinese-uppercase-amount`） | IBL-A4 `/contract`；A5 PII；A6 regenerate locale |
| DSL：`SPELL_AMOUNT(value, currencyCode)` 二元：ISO 币种 + `context.locale` 选语言 | 改变一元 CNY 中文语义为「跟 locale 静默换语言」 |
| 至少 **en + USD** 英文金额大写；框架可注册更多 (language, currency) | 本叶强制交付全部 ISO × 全部语言矩阵 |
| CNY/`zh` 回归（一元任意 locale + 二元 `CNY`+`zh*`） | FE Playwright / OA 视觉旅程 |
| 未支持 (language, currency) / 非法币种 / 非法 arity → fail-closed（不静默错语言） | 翻转 #3b/#5a；go-live |
| OpenAPI / contract-outline / ADR-0056 文档化 SPELL_AMOUNT 形态与默认行为 | 发明正式 P-phase |
| Gates：`mvn -B -ntp -f backend/pom.xml verify`；queued deploy 证据（行为验收面） | |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| `evalSpellAmount` 仅接受 **1** 参，且**固定**调用 `SpellAmountCn.spell` | `ComputeExpressionEvaluator.evalSpellAmount` + `requireOne` |
| `SpellAmountCn` 注释写明「fixed language, not locale-switched」 | `SpellAmountCn.java` |
| 引擎默认 locale `zh-CN`；`SPELL_AMOUNT` **不**读 locale 换语言 | `ComputeDslLimits.DEFAULT_LOCALE`；K03-C11 |
| 国际函件需要「USD One Thousand Only」类英文金额大写 — **今日不存在** | F3；IBL-A3 验收 |
| 若把一元改为「跟 locale 静默换语言」，既有 `SPELL_AMOUNT(${principal})` + `en-US` 会从中文突变为英文 → **静默错语言 / 破坏金标** | golden `05-chinese-uppercase-amount`；`VariableComputeEngineTest.SpellAmount` |
| A2 已建立「第二参 = ISO 币种、locale 来自 `context.locale`」契约模式 | [ibl-a2-format-amount-currency.md](./ibl-a2-format-amount-currency.md) |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **A3-S1 一元兼容** | `SPELL_AMOUNT(amount)` → **始终** CNY 中文大写（`SpellAmountCn` 语义）；**不**因 `context.locale=en-US` 改成英文 |
| **A3-S2 二元国际化** | `SPELL_AMOUNT(amount, currencyCode)`：币种身份 = ISO 4217；拼写语言 = `context.locale` 的 language；至少支持 **`en` + `USD`** 与 **`zh` + `CNY`** |
| **A3-S3 可扩展框架** | 实现为可注册的 (language × currency) speller 表/策略（或等价），本叶之外新增币种/语言不必改 DSL arity |
| **A3-S4 Fail-closed / 无静默错语言** | 未支持的 (language, currency)、非法 ISO、非法 arity、null/负数/超限金额 → `VARIABLE_COMPUTE_FAILED`；**禁止**回退成错误语言的「看似成功」串 |
| **A3-S5 契约文档** | OpenAPI + contract-outline + ADR-0056 记载一元/二元、默认行为、支持矩阵 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板作者** | 管理会话；编写 `computeExpression`；可选样例求值 API | 国际函件须显式二元 + 正确 locale；不得假设一元会跟 locale 变英文 |
| **Runtime API 调用方** | 有效凭证；generate / batch；`context.locale` | 产物金额大写语言/币种与表达式 + locale 一致 |
| **系统（compute）** | `VariableComputeService` → `ComputeExpressionEvaluator` | 解析 arity；选 speller；fail-closed |
| **系统（author APIs）** | `POST …/compute-expressions/validate|evaluate` | 二元表达式合法；evaluate 可预览 en/USD 与 zh/CNY |

---

## 3. Goal

1. 国际 amount-in-words：**至少** `SPELL_AMOUNT(amount, 'USD')` + `context.locale` 语言为 **en**（如 `en` / `en-US`）产出英文 USD 金额大写（可观测含 dollar/USD 身份与英文数词），满足 F3「USD One Thousand Only」能力缺口。  
2. **CNY / zh 保持正确：** 一元 `SPELL_AMOUNT` 与二元 `SPELL_AMOUNT(..., 'CNY')` + `zh*` 继续产出既有中文大写（零元/整元/角分/亿级边界表不回归）。  
3. **默认行为文档化、禁止静默错语言：** 一元固定 CNY 中文（与 locale 无关）；引擎默认 locale 仍为 `zh-CN`（CE-K03）；二元在未支持 pair 时失败，**不**静默回退中文 CNY 或英文 USD。  
4. **可扩展：** 代码结构允许后续注册更多 (language, currency)；本叶不要求交付全矩阵。  
5. OpenAPI / 文档 / ADR-0056 同步记载 SPELL_AMOUNT 形态。  
6. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **A3-C1** | **函数形态：** `SPELL_AMOUNT(value)` **或** `SPELL_AMOUNT(value, currency)`。Arity ∉ {1,2} → `VARIABLE_COMPUTE_FAILED`。 | 对齐 A2 arity 模式；解析器已支持多参；今日 `requireOne` 需放宽 |
| **A3-C2** | **第二参语义 = ISO 4217 字母币种码**（如 `'USD'`、`'CNY'`），**不是** locale 标签。拼写**语言**仅来自请求 `context.locale`（及 CE-K03 默认/回退 `zh-CN`）。 | 与 A2-C2 一致；避免第二参语义冲突 |
| **A3-C3** | **币种求值：** `currency` 可为字符串字面量或求值后为字符串的表达式（含 `${path}`）。求值结果 trim 后按 **大写** 规范化再解析 ISO（`usd` → `USD`）。 | DSL 一致性；A2-C3 |
| **A3-C4** | **一元兼容（missing currency）：** 仅 `SPELL_AMOUNT(value)` 时 **始终** 走 CNY 中文大写（`SpellAmountCn` 语义），**忽略** `context.locale` 的语言切换。文档必须写明：国际函件英文大写须用二元 + `en*` locale。 | K03-C11；金标 `05-chinese-uppercase-amount`；「no silent wrong language」 |
| **A3-C5** | **二元语言选择：** 取 `context.locale` 的 **primary language**（BCP-47 language subtag，大小写不敏感）。`en` / `en-US` / `en-GB` → language=`en`；`zh` / `zh-CN` / `zh-Hans` → language=`zh`。 | F3；可扩展框架 |
| **A3-C6** | **本叶必须支持的 (language, currency) 矩阵：** | IBL-A3 验收 |
| | `(zh, CNY)` — 与一元/`SpellAmountCn` **同一语义**（边界表 K03-C12） | |
| | `(en, USD)` — 英文 USD 金额大写（见 A3-C7） | |
| **A3-C7** | **en+USD 可观测契约（单测锁稳定断言）：** | F3「USD One Thousand Only」 |
| | 金额按分（cent）半入两位小数后拼写（与 CNY fen 尺度对齐的银行金额习惯）。 | |
| | 结果为 **英文**（拉丁字母数词/单位），**不得**含中文金额单位「元」「角」「分」「整」作为成功路径主体。 | |
| | 必须可观测 **USD/dollar 币种身份**（含 `dollar`/`dollars` 和/或 `USD`；大小写不敏感）。 | |
| | 至少锁定一个金标字面量（实现期固定，单测精确相等），推荐：`SPELL_AMOUNT(1000, 'USD')` + locale `en-US` → **`USD One Thousand Only`**（或实现选定的等价稳定串，但须在单测与文档中**同一字面量**钉死；F3 示例优先）。 | |
| | 另覆盖：非整美元（含 cents）、零元英文路径、与中文路径交叉对照。 | |
| **A3-C8** | **未支持 pair fail-closed：** 二元求值时，(language, currency) **不在**已注册支持表 → `VARIABLE_COMPUTE_FAILED`。**禁止**回退为一元中文 CNY，也**禁止**在 locale=`zh-CN` 时对 USD 静默输出英文（或相反）。 | 「no silent wrong language」 |
| **A3-C9** | **本叶明确未支持（失败，非成功）：** 例如 `(en, CNY)`、`(zh, USD)`、以及任意未注册币种（如 `'EUR'`）——除非实现额外注册（本叶 **不要求** 交付）。单测至少覆盖：`SPELL_AMOUNT(100, 'USD')` + `zh-CN` → 失败；`SPELL_AMOUNT(100, 'CNY')` + `en-US` → 失败。 | 缩小范围；fail-closed |
| **A3-C10** | **默认 locale 文档化：** `ComputeDslLimits.DEFAULT_LOCALE = zh-CN` 仍适用于 FORMAT_* 与「缺失 locale」回退。对 SPELL_AMOUNT：缺失 locale 时二元按 language=`zh` 解析 → 因此二元 USD 在缺省 locale 下 **失败**（A3-C8/C9），**不会**静默英/中错配。作者预览/runtime 国际函件必须显式传 `en`/`en-US`。 | handoff「default locale documented」 |
| **A3-C11** | **金额拒绝边界（一元与二元共用）：** null / 非数值 / **负数** / 超过 `MAX_SPELL_AMOUNT` → `VARIABLE_COMPUTE_FAILED`（与 CE-K03 一致）。 | `SpellAmountCn`；K03-C12 |
| **A3-C12** | **非法 ISO / 空白 currency（二元）：** null、trim 空串、非法字母码 → `VARIABLE_COMPUTE_FAILED`（不回退一元中文）。第二参误传 `'en-US'` → 非法 ISO → 失败。 | 对齐 A2-C7/C8 |
| **A3-C13** | **可扩展框架（强制结构，非全矩阵交付）：** 拼写器通过注册表/策略接口按 (language, currency) 分发；新增 pair 不改函数名/arity。本叶至少注册 A3-C6 两行。单测或包结构可证明「未注册 → 失败」而非硬编码 if-else 死胡同（允许内部 if，但扩展点须存在且文档记载）。 | IBL-A3「framework for more」 |
| **A3-C14** | **错误码：** 不新增顶层码；继续 `VARIABLE_COMPUTE_FAILED`（HTTP 422，与 CE-K03/A2 一致）。 | 缩小范围 |
| **A3-C15** | **文档面（强制）：** OpenAPI compute validate/evaluate 及相关 schema 注明 `SPELL_AMOUNT(value)`（CNY 中文，与 locale 无关）与 `SPELL_AMOUNT(value, currencyCode)`（ISO + locale language）；写明本叶支持矩阵与未支持 fail-closed；contract-outline 交叉引用；实现同期 **修订 ADR-0056** Decision（取消「SPELL_AMOUNT CNY-only / 不随 locale」的绝对表述，改为一元 CNY-fixed + 二元国际化矩阵）。 | IBL-A3；ADR 后果 |
| **A3-C16** | **与 CE-K03 关系：** K03-C11/C12/C21「SPELL 固定 CNY / 不多币种 SPELL」被本叶 **超集**：一元语义保留；二元为新确认需求。BDD-CE-K03-014…018 仍有效（一元）。 | 源序：本叶确认 > K03 历史范围 |
| **A3-C17** | **金标：** `05-chinese-uppercase-amount` 一元路径必须继续 GREEN。本叶不强制新建英文金标包（单测矩阵足够）；若增包则为可选增强。 | K03-C17 回归 |
| **A3-C18** | **FE：** `frontend_ui_in_scope=false`。允许修正客户端白名单/注释示例；不要求 Playwright。 | 交付范围 |
| **A3-C19** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`；行为变更验收面 → Stage 5/10 queued Docker deploy 证据；architecture review。 | delivery constitution |
| **A3-C20** | **完成边界：** Done ≠ Wave A 完备；≠ go-live；#3b/#5a 保持 CONDITIONAL。 | 队列政策 |

### 4.2 已确认（上游交付，本叶只消费）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **A3-U1** | 白名单含 `SPELL_AMOUNT`；locale 默认 `zh-CN` | CE-K03；ADR-0056 |
| **A3-U2** | 求值失败 → `VARIABLE_COMPUTE_FAILED` | CE-K03 |
| **A3-U3** | `FORMAT_AMOUNT(value, iso4217)` ISO 第二参模式已落地 | IBL-A2 / #108 |
| **A3-U4** | Formal phase None；非 go-live | IBL 队列政策 |

### 4.3 非确认假设（不得升格为需求）

| ID | 陈述 | 状态 |
| --- | --- | --- |
| **A3-N1** | 一元 `SPELL_AMOUNT` 随 `context.locale` 自动切换英文 | **非确认** — **明确拒绝**（A3-C4） |
| **A3-N2** | 本叶交付 `(en, CNY)` / `(zh, USD)` / EUR 等其它 pair | **非确认** — 未注册则失败（A3-C9） |
| **A3-N3** | 强制迁移全部既有一元表达式为二元 | **非确认** — 一元兼容 |
| **A3-N4** | 作者面板专用语言/币种下拉 UI | **非确认** — FE OOS |
| **A3-N5** | 第二参传入 locale 标签作为合法形态 | **非确认** — **明确拒绝**（A3-C2） |
| **A3-N6** | ICU/第三方完整多语言金额库作为强制依赖 | **非确认** — 允许平台自研 en/USD + 保留 SpellAmountCn |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | Runtime / preview / test-generate 装配前 compute 求值含 `SPELL_AMOUNT` | 主验收面 |
| T2 | 管理 `POST …/compute-expressions/evaluate`（可选 `locale`） | 作者预览 |
| T3 | 单元测试 / `mvn verify`（含金标一元回归） | 门禁 |

---

## 6. Preconditions

| # | 前置条件 |
| --- | --- |
| PC1 | 模板变量 schema 含合法 `computeExpression`（或 evaluate API 直接提交表达式） |
| PC2 | 绑定值可解析为非负数值金额（成功路径） |
| PC3 | `context.locale` 缺失时引擎按 CE-K03 默认 `zh-CN` |
| PC4 | 调用方已通过既有认证/授权（本叶不放宽） |
| PC5 | IBL-A2（#108）已提供 ISO 币种第二参模式可对齐（推荐依赖，非运行时硬阻塞） |

---

## 7. Primary journey（成功）

1. 作者为国际函件编写 `SPELL_AMOUNT(${principal}, 'USD')`，请求 `context.locale=en-US`。  
2. Runtime/preview 求值：金额 + ISO USD + language=`en` → 英文 USD 大写字符串写入装配变量。  
3. 既有中文模板继续使用一元 `SPELL_AMOUNT(${principal})`（任意 locale）或二元 `SPELL_AMOUNT(..., 'CNY')` + `zh-CN` → 中文大写不回归。  
4. 金标 `05-chinese-uppercase-amount` 一元路径仍成功。

---

## 8. System responses

### 8.1 Success

| 形态 | 响应 |
| --- | --- |
| 一元合法 | CNY 中文大写字符串（与 CE-K03 边界表一致） |
| 二元 `(en, USD)` 合法 | 英文 USD 金额大写字符串（A3-C7） |
| 二元 `(zh, CNY)` 合法 | 与一元相同的中文大写语义 |
| evaluate API | `success=true` + `result` 为上述字符串 |

### 8.2 Fail-closed

| 条件 | 行为 |
| --- | --- |
| Arity ∉ {1,2} | `VARIABLE_COMPUTE_FAILED`；无成功静默拼写 |
| 二元 currency null/空白/非法 ISO | 同上 |
| 二元 (language, currency) 未注册 | 同上（**不**回退一元中文） |
| amount null / 非数值 / 负 / 超限 | 同上（既有） |
| 认证/授权失败 | 既有 401/403；不改变 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-A3-001 — en + USD：英文金额大写（F3 主验收）

**Given** 表达式 `SPELL_AMOUNT(${principal}, 'USD')`，`principal=1000`  
**And** `context.locale=en-US`  
**When** compute 求值（引擎单测或 evaluate API）  
**Then** 结果为英文 USD 金额大写，且与实现钉死的金标字面量一致（优先 **`USD One Thousand Only`**）  
**And** 结果**不得**含「元」「角」「分」「整」中文金额单位

### BDD-IBL-A3-002 — en + USD：含 cents 路径

**Given** 表达式 `SPELL_AMOUNT(${principal}, 'USD')`，`principal=1.23`（或等价）  
**And** `context.locale=en`（或 `en-US`）  
**When** 求值  
**Then** 结果为英文，且可观测 dollar/USD 身份与 cent(s)（或实现钉死的等价小数拼写）  
**And** 结果不得呈现为中文 `壹元贰角叁分`

### BDD-IBL-A3-003 — 一元 CNY 中文：locale=en-US 仍不换语言

**Given** 表达式 `SPELL_AMOUNT(${principal})`（无第二参），`principal=100`  
**And** `context.locale=en-US`  
**When** 求值  
**Then** 结果等于 `壹佰元整`  
**And** **不得**变成英文 USD/dollar 拼写（证明一元不跟 locale 静默换语言）

### BDD-IBL-A3-004 — CNY + zh 二元回归

**Given** 表达式 `SPELL_AMOUNT(${principal}, 'CNY')`，`principal=100`  
**And** `context.locale=zh-CN`  
**When** 求值  
**Then** 结果等于 `壹佰元整`（与一元/`SpellAmountCn` 一致）

### BDD-IBL-A3-005 — 一元 CNY 边界表回归（零 / 角分 / 亿 / 负）

**Given** 一元 `SPELL_AMOUNT`  
**When** 分别求值 `0` → `零元整`；`1.23` → `壹元贰角叁分`；`100000000` → `壹亿元整`；`-1` → 失败  
**Then** 与 BDD-CE-K03-014…018 / `VariableComputeEngineTest.SpellAmount` 一致（不回归）

### BDD-IBL-A3-006 — 未支持 pair：USD + zh-CN → fail-closed（无静默错语言）

**Given** 表达式 `SPELL_AMOUNT(${principal}, 'USD')`，`principal=100`  
**And** `context.locale=zh-CN`（默认 locale 亦同）  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`  
**And** **不**返回中文「壹佰元整」，也**不**返回英文 USD 成功串

### BDD-IBL-A3-007 — 未支持 pair：CNY + en-US → fail-closed

**Given** 表达式 `SPELL_AMOUNT(${principal}, 'CNY')`，`principal=100`  
**And** `context.locale=en-US`  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`  
**And** **不**返回 `壹佰元整`（二元未注册 pair 不得回退成功）

### BDD-IBL-A3-008 — 非法 ISO / 空白 currency → fail-closed

**Given** 表达式 `SPELL_AMOUNT(${principal}, 'NOTACURRENCY')` 或第二参求值为 `null`/`''`  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`  
**And** 不回退一元中文成功串

### BDD-IBL-A3-009 — 非法 arity → fail-closed

**Given** 表达式 `SPELL_AMOUNT()`（0 参）或 `SPELL_AMOUNT(${principal}, 'USD', 'EUR')`（3 参）  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`

### BDD-IBL-A3-010 — 币种来自变量绑定（en+USD）

**Given** 表达式 `SPELL_AMOUNT(${principal}, ${ccy})`，`principal=1000`，`ccy='USD'`  
**And** `context.locale=en-US`  
**When** 求值  
**Then** 结果与 BDD-IBL-A3-001 同一金标英文串（或同一稳定字面量）

### BDD-IBL-A3-011 — 可扩展性：未注册币种失败（框架）

**Given** 表达式 `SPELL_AMOUNT(${principal}, 'EUR')`，`principal=100`  
**And** `context.locale=en-US`（本叶未注册 `(en, EUR)`）  
**When** 求值  
**Then** `VARIABLE_COMPUTE_FAILED`  
**And** 代码/文档可指出扩展点为注册新 (language, currency) speller，而非改 DSL 函数名

### BDD-IBL-A3-012 — 契约文档记载默认行为与支持矩阵

**Given** 本叶实现完成集  
**When** 审查 OpenAPI（compute validate/evaluate 相关 description）、contract-outline、ADR-0056  
**Then** 文档明确：  
- `SPELL_AMOUNT(value)` = CNY 中文大写，**不**随 locale 换语言  
- `SPELL_AMOUNT(value, currencyCode)` = ISO 币种 + `context.locale` language  
- 本叶支持 `(zh,CNY)` 与 `(en,USD)`；其它 pair fail-closed  
- 默认 locale `zh-CN` 对二元 USD 意味着需显式传 `en*`  
**And** 示例第二参为 `'USD'`/`'CNY'`，**不**把第二参描述为 locale

---

## 10. Boundary / exception

| 场景 | 行为 |
| --- | --- |
| 小写 `'usd'` | 规范化为大写后，在 `(en, USD)` 下成功 |
| 一元 + locale 缺失/`zh-CN`/`en-US` | 均为中文 CNY 大写（A3-C4） |
| 二元 + locale 缺失 | language=`zh` → USD 失败（A3-C6/C8）；CNY 成功 |
| 超过 `MAX_SPELL_AMOUNT` | 失败（既有） |
| 嵌套：`SPELL_AMOUNT(SUM(...), 'USD')` | 合法（第一参为表达式） |
| 第二参误传 `'en-US'` | 非法 ISO → 失败 |
| 授权失败 | 既有 fail-closed；本叶不放宽 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 单测矩阵 | 至少覆盖 BDD-IBL-A3-001…011（012 为文档审查） |
| API evaluate（可选） | `success` + `result` 字符串 |
| 金标 | `05-chinese-uppercase-amount` 一元路径 GREEN |
| 契约 | OpenAPI description + ADR-0056 修订 + contract-outline 交叉引用 |
| 门禁 | `mvn verify` GREEN；queued Docker deploy 证据 |
| Trace | 既有 `metadata.traceId` 保留 |

---

## 12. Traceability

| 项 | 引用 |
| --- | --- |
| Plan | [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) · F3 · **IBL-A3** |
| Task Master | **#109** |
| Related BDD | [ce-k03-variable-compute-engine.md](./ce-k03-variable-compute-engine.md)（一元）；[ibl-a2-format-amount-currency.md](./ibl-a2-format-amount-currency.md)（ISO 第二参模式） |
| Code anchors | `SpellAmountCn`；`ComputeExpressionEvaluator.evalSpellAmount`；`ComputeDslLimits.DEFAULT_LOCALE`；`VariableComputeEngineTest.SpellAmount`；golden `05-chinese-uppercase-amount` |
| API | [openapi-v1.yaml](../api/openapi-v1.yaml)；[contract-outline.md](../api/contract-outline.md) |
| ADR | [0056-whitelist-variable-compute-dsl-bounds.md](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md)（实现期修订） |
| Permissions | 无新 capability |

---

## 13. Out of scope（本叶）

- IBL-A4 / A5 / A6  
- `(en, CNY)` / `(zh, USD)` / EUR 等未注册 pair 的成功拼写  
- 一元随 locale 自动英文化  
- FE E2E/UIUX；作者语言/币种下拉  
- 翻转 #3b/#5a；go-live；Wave A Done  

---

## 14. Ready-for-implementation handoff

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-a3-amount-in-words.md
task_ids: ["109"]
plan_id: IBL-A3
frontend_ui_in_scope: false
acceptance_scenario_ids:
  - BDD-IBL-A3-001
  - BDD-IBL-A3-002
  - BDD-IBL-A3-003
  - BDD-IBL-A3-004
  - BDD-IBL-A3-005
  - BDD-IBL-A3-006
  - BDD-IBL-A3-007
  - BDD-IBL-A3-008
  - BDD-IBL-A3-009
  - BDD-IBL-A3-010
  - BDD-IBL-A3-011
  - BDD-IBL-A3-012
next_stage: plan-orchestrator (stage 2)
spell_amount_arity_decision: >
  SPELL_AMOUNT(value) = CNY Chinese uppercase always (locale-independent);
  SPELL_AMOUNT(value, iso4217) = amount-in-words for (locale.language, currency);
  required pairs this leaf: (zh,CNY) + (en,USD);
  unsupported pair / invalid currency / bad arity → VARIABLE_COMPUTE_FAILED
  (no silent wrong-language fallback).
```

**TDD Red 优先场景：** BDD-IBL-A3-001（en+USD 金标）、003（一元+en-US 仍中文）、004（CNY+zh）、006/007（未支持 pair fail-closed）、008/009（非法币种/arity）。
