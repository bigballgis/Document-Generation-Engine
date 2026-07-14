# CE-K04 语义级变更对比 + release A/B — BDD

| Field | Value |
| --- | --- |
| **Slice** | `ce-k04-semantic-change-diff` |
| **Plan task** | **CE-K04**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §3 CE-K04） |
| **Task Master** | **#60** |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-15 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k04-semantic-change-diff` · `feat/ce-k04-semantic-change-diff` |
| **Scope of this slice** | 结构化内容树语义 diff（锚点 → 块路径）；`ChangeDiffService.computeBetween`；release A vs B 管理 API；前端 release 列表多选对比；审批/发布摘要展示句级可读 diff（非维度码表）。**依赖 K01**（对比对象为钉扎快照才有意义）。**禁止** CE-K05 impact 真化；**不** go-live；**不**激活 CD-3 |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；钉扎前提 [ce-k01-release-bundle-pinning.md](./ce-k01-release-bundle-pinning.md)；结构化内容 [ADR-0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) / domain § structured content |

---

## 1. 概述

现状 `ChangeDiffService` / `ChangeDiffDimensionSupport.diffContent` 仅比较 `masterCatalogVersion` 字符串；`diffAnchors` 仅比较 `bindingHash`；审批/发布摘要对话框把维度码与计数当「变更说明」。法务无法阅读「哪一句话变了」。亦无任意两已发布 release 的对比入口。

本切片把 CONTENT（及与结构化内容相关的可读条目）升级为**语义级树 diff**，抽出 `computeBetween(versionA, versionB)`，开放 release A/B API 与 UI，并让审批/发布摘要展示人类可读句级差异。

| 行为域 | 摘要 |
| --- | --- |
| **SCD-01 语义内容树 diff** | 按锚点 → 块路径逐节点比较；产出可读条目（含文字级前后文与条款版本升降） |
| **SCD-02 computeBetween** | `ChangeDiffService` 支持任意两 `TemplateVersionEntity`（含两 PUBLISHED release）对比 |
| **SCD-03 Release A/B API** | 管理 API：给定 templateId + releaseVersionA + releaseVersionB → `ChangeDiffView`（含语义条目） |
| **SCD-04 FE 多选对比** | Release 列表多选恰好两版 → 打开对比面板展示语义 diff |
| **SCD-05 审批摘要可读** | 提交审批 / 发布摘要对话框展示句级 diff，**不**以维度码表（如裸 `CONTENT`/`masterCatalogVersion`）作为主可读面 |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| CE-K05 母版 impactAnalysis 真化 / revision anchor 清单面板 | Out of scope — 后续 #61 |
| PreviewComparisonService 像素/绑定码全面重写 | Out of scope — 本片可复用语义条目到审批面；预览对比深化非本片必交付 |
| 像素级 DOCX 并排阅读器 | Out of scope |
| 未钉扎（K01 前）的历史可复现对比保证 | Out of scope — 依赖 K01；未钉扎 baseline 允许降级提示 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 关注点 |
| --- | --- | --- |
| **TEMPLATE_APPROVER / Team Lead** | `decideApprovals` / `publishTemplates` | 审批与发布前读懂「改了哪句话」 |
| **TEMPLATE_AUTHOR** | `authorTemplates` | 提交审批前自查变更摘要 |
| **法务 / 审计（只读可读面）** | 具备模板读权限的会话 | 句级 diff 可理解、可引用 |
| **系统** | `ChangeDiffService` + 管理 API + 审批/发布摘要 UI | 语义 diff 权威；fail-closed 授权 |

---

## 3. Goal

1. CONTENT 维度（及结构化内容相关条目）按**锚点 → 块路径**比较结构化内容树，产出人类可读变更条目（示例形态：`第 3 段：'贷款利率 4.9%' → '贷款利率 5.2%'`；条款引用版本升降单独成条）。
2. `ChangeDiffService` 抽出 **`computeBetween(versionA, versionB)`**（或等价签名），供 RC-vs-last-published 与任意两 version/release 复用同一引擎。
3. 管理 API 增加 **release A vs B** 端点（同一 template 下两个已发布 `releaseVersion`）。
4. 前端 release 列表支持**多选恰好两版**发起对比。
5. 审批/发布摘要主表面展示语义条目列表；维度码仅可作次要分组/技术标签，**不得**作为唯一可读说明。
6. 无读权限 → fail-closed（既有 `requireReadableTemplate` 语义）。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **K04-C1** | **对比对象：** 优先使用各 version 钉扎快照（K01：`master_revision_id` / 内容模块锁定引用 / 结构化内容 JSON）。候选与 baseline 均为 `TemplateVersionEntity`。 | 计划卡依赖 K01 |
| **K04-C2** | **语义 CONTENT diff：** 遍历锚点绑定下的结构化内容树；按稳定块路径对齐节点；分类至少覆盖 **added / removed / modified / moved（同锚点内重排）**；叶子文本变更产出前后文字片段（截断策略实现固定，建议单侧 ≤120 字符）。 | 计划卡 |
| **K04-C3** | **条款引用：** 同一引用 key 的 pinned semanticVersion 升降单独成可读条目（升/降/新增引用/移除引用）。 | 计划卡「条款版本升降」 |
| **K04-C4** | **`computeBetween`：** 公共入口接受两 version（或两 versionId）；现有 `compute` / `computeForVersion` 委托该入口。 | 计划卡 |
| **K04-C5** | **Release A/B API：** `GET`（或项目惯例等价）`/api/management/v1/templates/{templateId}/change-diff/releases?releaseVersionA=&releaseVersionB=`（最终 path 写入 OpenAPI）；A/B 均须存在且为 PUBLISHED（或带非空 `releaseVersion` 的已发布行）；A=B → 空变更或 422（实现固定一种并测）。无权限 → 403/隐藏。 | 计划卡 |
| **K04-C6** | **响应模型：** 在既有 `ChangeDiffView` / dimension 结构上扩展**人类可读条目**字段（如 `humanReadableEntries[]` 或 CONTENT `modified[].summary` 强制为可读句）；禁止仅返回 `masterCatalogVersion changed` 作为 CONTENT 唯一说明。 | 计划卡 + 审批可读目标 |
| **K04-C7** | **FE 多选：** Release 列表勾选 **恰好 2** 个 release 后启用「Compare」；1 或 >2 时控件禁用并有英文-first 提示。对比面板消费 A/B API。 | 计划卡 |
| **K04-C8** | **审批/发布摘要：** `TemplateSubmitForApprovalSummaryDialog` / publish summary（及等价）主列表渲染语义条目；`hasChanges`/`totalChangeCount` 可保留；**不得**只展示维度枚举码表。 | 计划卡 |
| **K04-C9** | **ANCHORS / VARIABLES / RULES / CONTRACT：** 本片至少保证 CONTENT 语义化；其他维度可保留既有哈希/键 diff，但不得阻断 CONTENT 可读面。后续深化非阻塞。 | 计划卡聚焦 CONTENT |
| **K04-C10** | **本片禁止：** K05 impact 真化；改 K01 钉扎语义；go-live；CD-3。 | 计划卡 |

---

## 5. Preconditions / Trigger

**Preconditions**

- K01 已交付：已发布 release 带钉扎字段；本片在隔离 worktree 交付。
- 模板至少有一个可对比的 baseline（上一 PUBLISHED）或用户选定的两个 release。
- 会话对目标 template 可读。

**Triggers**

- 打开提交审批 / 发布摘要对话框（加载 change-diff）。
- 调用既有 `GET …/change-diff`（RC vs last published）。
- 调用新 A/B release 对比 API。
- 在 release 列表多选两版并点 Compare。

---

## 6. Primary journey

1. 作者在草稿改写某锚点下一段落文字（如利率句），提交审批。
2. 审批人打开提交审批摘要 → 看到该句的语义 diff 条目（前后文），而非仅 `CONTENT` / `masterCatalogVersion` 码。
3. Team Lead 在 release 列表勾选 `1.0.0` 与 `1.1.0` → Compare → 面板展示两钉扎快照间的句级与条款版本差异。
4. 审计可凭 API 响应中的可读条目复述变更。

---

## 7. System responses（success / fail-closed）

| 情况 | 系统响应 |
| --- | --- |
| RC vs last published 有文字变更 | CONTENT（或可读条目集合）含 ≥1 条句级 modified；`hasChanges=true` |
| 两 release 结构相同 | `hasChanges=false`；条目空 |
| Release A 或 B 不存在 | 404 / 业务错误（OpenAPI 固定）；无半残 diff |
| 无读权限 | fail-closed 403 / access denied（既有模板读模型） |
| A=B | 空 diff 或 422（K04-C5） |
| 审批摘要加载成功 | UI 主表面为可读条目列表 |

---

## 8. Acceptance scenarios

### BDD-CE-K04-SCD-001 — 句级 CONTENT 语义 diff（引擎）

```gherkin
Given 模板存在 PUBLISHED baseline 与 in-flight release candidate
And candidate 相对 baseline 仅将某锚点下一段落文本从 "贷款利率 4.9%" 改为 "贷款利率 5.2%"
When ChangeDiffService.compute（或 computeBetween）对比二者
Then 结果 hasChanges=true
And 可读条目中至少一条描述该句前后文差异（含旧文与新文片段）
And 该条目不得仅以 "masterCatalogVersion changed" 或裸维度码作为唯一说明
```

### BDD-CE-K04-SCD-002 — 增 / 删 / 移动 / 嵌套块

```gherkin
Given baseline 与 candidate 的结构化内容树在同一锚点下分别发生：新增块、删除块、同级移动、嵌套子块文本修改
When 引擎 computeBetween(baseline, candidate)
Then 各类变更均可观察为独立可读条目（或明确归类的 added/removed/modified/moved）
And 单测矩阵覆盖增/删/改/移动/嵌套至少各 1 例
```

### BDD-CE-K04-SCD-003 — 条款引用版本升降

```gherkin
Given candidate 将某 content-module 引用的 pinned semanticVersion 从 1.0.0 升至 1.1.0
When 计算 change-diff
Then 出现可读条目标明该引用 key 的版本升降（1.0.0 → 1.1.0）
```

### BDD-CE-K04-SCD-004 — computeBetween 复用

```gherkin
Given 任意两 TemplateVersionEntity versionA 与 versionB（同一 template）
When 调用 ChangeDiffService.computeBetween(versionA, versionB)
Then 返回与 compute/computeForVersion 同构的 ChangeDiffView（含语义可读条目）
And 既有 RC-vs-last-published 路径委托同一引擎
```

### BDD-CE-K04-SCD-005 — Release A vs B API

```gherkin
Given 模板存在已发布 release "1.0.0" 与 "1.1.0"，且 1.1.0 相对 1.0.0 有句级内容差异
And 会话可读取该模板
When 调用 release A/B change-diff API（A=1.0.0, B=1.1.0）
Then 200 + envelope 内 ChangeDiffView 含对应语义条目
And baseline/candidate（或 A/B）releaseVersion 字段可观察
```

### BDD-CE-K04-SCD-006 — A/B API 授权 fail-closed

```gherkin
Given 会话对模板无读权限
When 调用 release A/B change-diff API
Then 拒绝（403 或既有 access-denied 模型）
And 不返回 diff 正文
```

### BDD-CE-K04-SCD-007 — FE release 多选对比

```gherkin
Given 用户在模板 release 列表可见至少两个已发布版本
When 勾选恰好两个 release 并激活 Compare
Then 打开对比面板并展示语义可读条目（消费 A/B API）
When 仅勾选 1 个或超过 2 个
Then Compare 不可用（禁用）且有英文-first 提示
```

### BDD-CE-K04-SCD-008 — 审批摘要展示句级 diff（E2E）

```gherkin
Given 作者将条款/段落文字改一句并进入可提交审批状态
When 审批人（或作者）打开提交审批摘要对话框
Then 主可读面展示该句语义 diff
And 不以维度码表作为唯一变更说明
```

### BDD-CE-K04-SCD-009 — 发布摘要同等可读（回归）

```gherkin
Given 发布摘要对话框加载 change-diff
When 存在语义变更
Then 发布摘要同样展示可读条目（与审批摘要同构信息密度）
```

---

## 9. Boundary / exception

- 无 baseline（首发 RC）：CONTENT 可为空维度；不虚构 diff。
- 超长文本：条目截断但必须仍含可辨认前后片段。
- 非 CONTENT 维度保持可用；CONTENT 语义化失败 → fail-closed 错误（不可静默回退到仅 masterCatalogVersion）。
- A/B 跨模板：拒绝。

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 单元测试 | diff 引擎增/删/改/移动/嵌套；computeBetween；条款升降 |
| API | 既有 change-diff + 新 A/B；OpenAPI 更新 |
| UI / E2E | 改一句 → 审批摘要可见；release 多选对比 |
| 权限 | 无读权限无 diff 正文 |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-K04 plan §3 | 目标行为 |
| Task Master **#60** | 执行任务 |
| CE-K01 / #57 | 钉扎依赖 |
| ADR-0019 / domain structured content | 内容树模型 |
| P19-T04 ChangeDiff | 既有维度壳升级为本片语义面 |

---

## 12. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-k04-semantic-change-diff.md
task_ids: ["#60", "CE-K04"]
scenario_ids:
  - BDD-CE-K04-SCD-001
  - BDD-CE-K04-SCD-002
  - BDD-CE-K04-SCD-003
  - BDD-CE-K04-SCD-004
  - BDD-CE-K04-SCD-005
  - BDD-CE-K04-SCD-006
  - BDD-CE-K04-SCD-007
  - BDD-CE-K04-SCD-008
  - BDD-CE-K04-SCD-009
```
