# CE-U12 契约页可复制示例 — BDD

| Field | Value |
| --- | --- |
| **Slice** | `ce-u12-contract-copyable-examples` |
| **Plan task** | **CE-U12**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U12） |
| **Task Master** | **#87** |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-15 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-u12-contract-copyable-examples` · `feat/ce-u12-contract-copyable-examples` |
| **Scope of this slice** | Caller contract 页 Examples：完整 **curl**（含 Auth + `Idempotency-Key`）+ 由**选定测试数据集**生成请求 payload JSON + **复制按钮**。替换/增强现状仅 token 字符串（如 `generate-sync-docx`）的 examples 列表。**不** go-live |
| **Owning docs** | 本文件（行为 SoT）；契约/调用基线 [api-ops-discoverability.md](./api-ops-discoverability.md) / contract panel；ADR [0031](../adr/api/0031-api-platform-hardening-baseline.md) Idempotency-Key；计划 §4 CE-U12 |

---

## 1. 概述

`ContractAssemblyService` / 契约页 `templates.contract.sections.examples` 目前只渲染 token 字符串列表（如 `generate-sync-docx`），集成方无法一键得到可运行的 curl 与真实 payload。

本切片在 **Template Caller Contract** 面板把 Examples 升级为可复制的操作示例。

| 行为域 | 摘要 |
| --- | --- |
| **CCE-01 完整 curl** | 含方法、URL（当前环境路径）、`Authorization` 头占位、`Idempotency-Key` 头、Content-Type、body 引用/内联 |
| **CCE-02 测试数据集 payload** | 从用户选定的模板测试数据集生成 generate 请求 JSON（变量值来自数据集） |
| **CCE-03 复制** | curl 与 payload 分别（或明确组合）提供 Copy；成功有英文-first 反馈 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 在 UI/示例中嵌入真实 API secret / JWT | **禁止** — 仅占位符 |
| 改变 runtime 契约校验 / CE-C01–C03 | Out of scope |
| 重做整页 contract 信息架构 | **禁止** — 仅 Examples 段升级 |
| 宣称 go-live | **禁止** |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **API 管理员 / 集成对接** | `canManageApiPolicy` 或既有契约页可见角色 | 复制 curl/payload 联调 |
| **模板作者** | 可维护测试数据集 | 提供 payload 数据源 |
| **系统** | `TemplateCallerContractPanel`（+ 可选轻量装配辅助） | 生成示例；clipboard |

---

## 3. Goal

1. Examples 区展示**至少一个**可运行形态的单笔 generate **curl** 示例（优先 explicit version 或 default 路径，与当前环境选择一致）。
2. curl **必须**包含：`Authorization: Bearer <ACCESS_TOKEN>`（或项目既有 credential 头占位约定，实现固定一种并测）以及 **`Idempotency-Key: <IDEMPOTENCY_KEY>`**（占位可替换为示例 UUID）。
3. 请求 **payload JSON** 由**当前选定测试数据集**的变量映射生成（符合既有 GenerateRequest 形状；路径字段不在 body 重复 `templateId`/`releaseVersion`）。
4. 提供 **Copy curl** 与 **Copy payload**（或等价两个控件）；复制成功有可见反馈。
5. 无可用测试数据集时：payload 区展示 schema 骨架或明确 empty 引导（英文-first），仍可复制 curl（body 用骨架/占位）。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U12-C1** | **主表面：** FE 契约页 Examples 从 token `<code>` 列表升级为示例卡片/块；可保留 token 作次要标签，但**不得**作为唯一示例面。 | 计划卡；用户 scope |
| **U12-C2** | **curl 必含头：** `Authorization`（Bearer 占位）+ `Idempotency-Key`（占位）；`Content-Type: application/json`；HTTP method `POST`；URL 使用面板当前 `environment` 与契约 paths/version 表中的可调用 URL。 | 计划卡；ADR-0031 |
| **U12-C3** | **密钥安全：** 示例中**禁止**填入真实 secret；占位符固定为 `<ACCESS_TOKEN>` 与 `<IDEMPOTENCY_KEY>`（或同义大写占位）；复制内容同样为占位。 | 安全基线 |
| **U12-C4** | **Payload 来源：** 面板提供测试数据集选择器（默认：该模板最新未锁定或列表首个可用集；无则空）。Payload = 选定集 variables → GenerateRequest body（含 `variables` / 既有字段；`output` 取契约允许的默认 format/mode）。 | 计划卡「按选定测试数据集」 |
| **U12-C5** | **Copy：** 至少两个动作 — Copy curl、Copy payload；使用既有 clipboard 工具；成功 toast/文案 English-first。 | 用户 scope |
| **U12-C6** | **多示例（最小）：** 至少覆盖 **sync single generate**；batch/async 可作为附加卡片（非阻塞，有则同样含 Auth + Idempotency-Key）。 | 计划卡 P1·S |
| **U12-C7** | **后端：** 本片允许纯 FE 由 contract + test-dataset API 组装；若需服务端 enriched examples，可扩展 contract 响应，但不得再只返回 token 字符串作为唯一 examples。 | 计划卡 |
| **U12-C8** | **禁止：** 泄露凭证；改 CE-C 契约严格性；go-live。 | 计划卡 |

---

## 5. Preconditions / Trigger

**Preconditions**

- 模板可打开 Caller Contract 面板（已发布或既有契约可见条件）。
- 环境切换器可用（与现面板一致）。
- 可选：至少一条测试数据集以验证 payload 填充。

**Triggers**

- 打开契约页 Examples 段。
- 切换 environment / 选择测试数据集。
- 点击 Copy curl / Copy payload。

---

## 6. Primary journey

1. 管理员打开包契约页，环境选 `UAT`。
2. Examples 显示完整 curl（含 Auth 与 Idempotency-Key 占位）与 JSON payload。
3. 选择测试数据集「样例客户 A」→ payload 变量更新为该集数据。
4. Copy curl / Copy payload → 剪贴板含可粘贴文本；提示复制成功。

---

## 7. System responses（success / fail-closed）

| 情况 | 系统响应 |
| --- | --- |
| 有数据集 | payload 含该集变量值 |
| 无数据集 | 骨架或 empty 引导；curl 仍可用 |
| Copy 成功 | 可见成功反馈；剪贴板为占位符安全文本 |
| 无契约读权 | 既有面板不可见 / 403 |
| 环境切换 | URL 随 environment 更新后重生成 curl |

---

## 8. Acceptance scenarios

### BDD-CE-U12-CCE-001 — curl 含 Auth 与 Idempotency-Key

```gherkin
Given 用户打开 Caller Contract 面板且契约加载成功
When 查看 Examples 主示例 curl
Then 文本包含 Authorization 头与 Bearer 占位
And 包含 Idempotency-Key 头与占位值
And 包含 POST 与当前环境可调用 generate URL
And 不仅是 token 字符串 generate-sync-docx
```

### BDD-CE-U12-CCE-002 — payload 来自选定测试数据集

```gherkin
Given 模板有测试数据集 D，variables 含可辨认键值（如 customerName=Acme）
When 用户在 Examples 区选定 D
Then payload JSON 反映 D 的变量值
And body 不重复路径字段 templateId / releaseVersion
```

### BDD-CE-U12-CCE-003 — 切换数据集更新 payload

```gherkin
Given 存在数据集 D1 与 D2，变量值不同
When 从 D1 切换到 D2
Then 可见 payload 更新为 D2 值
And curl 中的 body（若内联）同步，或 Copy payload 取新值
```

### BDD-CE-U12-CCE-004 — Copy curl

```gherkin
Given Examples 已渲染完整 curl
When 用户点击 Copy curl
Then 剪贴板包含该 curl 全文（含 Auth 与 Idempotency-Key 占位）
And 显示成功反馈（English-first）
```

### BDD-CE-U12-CCE-005 — Copy payload

```gherkin
Given Examples 已渲染 payload
When 用户点击 Copy payload
Then 剪贴板为当前 JSON
And 显示成功反馈
And 内容不含真实 API secret
```

### BDD-CE-U12-CCE-006 — 无测试数据集边界

```gherkin
Given 模板无可用测试数据集
When 打开 Examples
Then payload 区为骨架或明确 empty 引导（非静默空白无说明）
And curl 仍可复制（body 用骨架/占位）
```

### BDD-CE-U12-CCE-007 — 环境切换更新 URL（回归）

```gherkin
Given 用户从环境 A 切换到环境 B
When curl 重新生成
Then URL 反映环境 B 的路径前缀/主机约定（与面板 paths 一致）
And Auth 与 Idempotency-Key 头仍在
```

---

## 9. Boundary / exception

- Clipboard API 不可用：显示可手动全选的 `<pre>`，并提示失败原因（英文-first）。
- 超大数据集：payload 可截断展示但 Copy 应为完整 JSON（或明确「已截断」）；实现固定一种。
- Batch 示例（若做）：独立 Idempotency-Key；仍禁止真实密钥。
- i18n：标签/按钮 English-first；curl 本身为技术英文。

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| UI | Examples 卡片；数据集选择；双 Copy |
| Clipboard / 测试 | Vitest mock clipboard；E2E 可见 curl 含头 |
| 安全 | 无真实 secret 快照 |
| Gates | FE lint/type-check/test/build；E2E+UIUX |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-U12 plan §4 | 目标行为 |
| Task Master **#87** | 执行任务 |
| ADR-0031 Idempotency-Key | 头传输基线 |
| contract-outline / OpenAPI generate | 请求形状 |
| 现状 `examples: string[]` tokens | 待替换主表面 |

---

## 12. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-u12-contract-copyable-examples.md
task_ids: ["#87", "CE-U12"]
scenario_ids:
  - BDD-CE-U12-CCE-001
  - BDD-CE-U12-CCE-002
  - BDD-CE-U12-CCE-003
  - BDD-CE-U12-CCE-004
  - BDD-CE-U12-CCE-005
  - BDD-CE-U12-CCE-006
  - BDD-CE-U12-CCE-007
```
