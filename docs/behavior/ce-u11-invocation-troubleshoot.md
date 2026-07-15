# CE-U11 调用排障 + 召回检索 — BDD

| Field | Value |
| --- | --- |
| **Slice** | `ce-u11-invocation-troubleshoot`（Task Master alt: `ce-u11-invocation-recall`） |
| **Plan task** | **CE-U11**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §4 CE-U11） |
| **Task Master** | **#86** |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-15 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-u11-invocation-troubleshoot` · `feat/ce-u11-invocation-troubleshoot` |
| **Scope of this slice** | 管理调用列表按 `resolvedReleaseVersion`（UI：releaseVersion）过滤；失败调用 detail + Drawer 暴露统一 error envelope；当前筛选结果 CSV 导出（召回圈定最小闭环）。扩展既有 P13 invocation history。**禁止**暴露 caller 变量明文（C6）；**不** go-live |
| **Owning docs** | 本文件（行为 SoT）；基线 [management-invocation-history.md](./management-invocation-history.md)；ADR [0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md)；计划 §4 CE-U11 |

---

## 1. 概述

P13 已交付分页调用历史 + status/kind/requestId 筛选 + summary drawer，但仍有排障与召回缺口：

| 缺口（现状证据） | 目标 |
| --- | --- |
| `ManagementInvocationFilters` / controller **无** `resolvedReleaseVersion`（实体有字段、predicates 未接） | 管理查询 + FE 筛选器支持按已解析 release 圈定 |
| `ManagementInvocationDetailView` / `InvocationSummaryDrawer` 无 `error.code` / `messageKey` 等 | 失败调用暴露统一 error envelope 字段 |
| 无列表导出 | 当前筛选结果可导出 CSV，支撑错函召回最小闭环 |

| 行为域 | 摘要 |
| --- | --- |
| **IRC-01 releaseVersion 过滤** | filters + repository + API query + FE 控件；按 `resolvedReleaseVersion` 精确匹配（semver 字符串） |
| **IRC-02 error envelope** | 失败记录持久化/映射统一 envelope；detail API + Drawer 展示 |
| **IRC-03 CSV 导出** | 导出**当前筛选条件**下的管理摘要行（召回圈定） |

**明确非目标**

| 非目标 | 处理 |
| --- | --- |
| 管理面暴露 `parameters` / variables 明文（C6） | **禁止** — CSV/detail 亦不得含 |
| 改变 runtime 调用方查询契约 | Out of scope — 本片管理 API/UI |
| 全站审计日志替代 / 新建第二套调用 catalog | **禁止** — 仍在包 Hub External access |
| 宣称 go-live / CD-3 | **禁止** |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **GROUP_ADMIN / GLOBAL_ADMIN** | `canManageApiPolicy`（组范围） | 排障、按版本召回、导出 |
| **运维 / 内控** | 同能力只读使用筛选+导出 | 圈定受影响调用 |
| **系统** | `ManagementInvocationQueryService` + `TemplateInvocationsPanel` + Drawer | 过滤、envelope、CSV；fail-closed |

---

## 3. Goal

1. 管理 `GET …/templates/{templateId}/api/invocations` 支持 query `resolvedReleaseVersion`（或文档化等价名；FE 标签 **Release version**）。
2. `ManagementInvocationFilters` + repository predicates **接通**该字段；与既有 status / kind / requestId / 时间 / credential 过滤器可组合。
3. 失败调用的 detail 与 Drawer 展示统一 error envelope：至少 **`code`**、**`category`**、**`messageKey`**、**`retryable`**（可选 resolved `message`）；成功调用这些字段为空/省略。
4. UI 提供 **Export CSV**，导出内容尊重**当前已应用筛选**（含 releaseVersion），列含召回最小集（见 U11-C6）；**无**参数明文。
5. 无 `canManageApiPolicy` → fail-closed。

---

## 4. 已确认决策（2026-07-15）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **U11-C1** | **过滤字段：** API/query 使用 `resolvedReleaseVersion`（与实体列一致）；FE 文案 English-first「Release version」。精确字符串匹配；空/未传 = 不过滤。 | 计划卡；用户 scope |
| **U11-C2** | **过滤器贯通：** `ManagementInvocationFilters` 增加该字段 → repository custom query → controller `@RequestParam` → FE `filterDraft` + Apply。 | 计划卡 |
| **U11-C3** | **Error envelope 来源：** 运行时写入失败 invocation 时持久化统一错误字段（`error_code` / `error_category` / `error_message_key` / `error_retryable` 或等价 JSON 列）；存量失败行无字段时 Drawer 显示「—」/ unavailable，不得捏造。本片须覆盖**新失败写入可读**。 | 计划卡 GAP；排障目标 |
| **U11-C4** | **Detail + Drawer：** `ManagementInvocationDetailView` 扩展 envelope 字段；`InvocationSummaryDrawer` 在 outcome/status 失败时渲染 envelope 区块（code、category、messageKey、retryable；message 可选）。**禁止**展示 parameters。 | 计划卡 |
| **U11-C5** | **列表列（可选增强）：** 摘要表可显示 `resolvedReleaseVersion` 列（若尚未显示）；过滤控件必做。 | 召回可用性 |
| **U11-C6** | **CSV：** 列最小集：`invocationId`,`requestId`,`invocationKind`,`status`,`resolvedReleaseVersion`,`routeType`,`createdAt`,`accessAccountSummary`,`outcome`,`errorCode`,`errorMessageKey`（有则导出）。UTF-8；文件名含 templateId 与时间戳合理即可。导出范围 = **当前已 Apply 的筛选**（可分页拉全匹配或服务端 export 端点；实现固定一种并测，须完整覆盖筛选命中集，不得只导当前页却声称全量）。 | 计划卡「召回最小闭环」 |
| **U11-C7** | **C6 锁定不变：** CSV、detail、list **均不得**含 variables / parametersStorage 明文。 | ADR-0040 / HIST BDD |
| **U11-C8** | **授权：** 与既有 invocation 管理 API 相同；跨组不可见。 | permission-matrix |
| **U11-C9** | **禁止：** 改 runtime caller invocation API；暴露密钥；go-live。 | 计划卡 |

---

## 5. Preconditions / Trigger

**Preconditions**

- 模板包存在 `api_policy`；actor 有 `canManageApiPolicy`。
- 留存窗口内存在多版本 / 多状态调用记录（含失败样例）。
- 本片在隔离 worktree 交付。

**Triggers**

- 打开 Hub External access → Invocation history。
- Apply 含 Release version 的筛选。
- 打开失败行的 summary drawer。
- 点击 Export CSV。

---

## 6. Primary journey

1. 错函版本 `1.2.0` 已发布并产生失败调用。
2. 管理员打开包调用历史 → 筛选 Release version = `1.2.0`（可再加 status=FAILED）→ 仅见该版本行。
3. 打开失败行 Drawer → 见 `REQUEST_BODY_INVALID` + `messageKey` + category + retryable。
4. Export CSV → 下载文件仅含当前筛选命中行，供内控圈定召回范围。

---

## 7. System responses（success / fail-closed）

| 情况 | 系统响应 |
| --- | --- |
| 按 releaseVersion 筛选 | 仅 `resolvedReleaseVersion` 匹配行；分页 total 同步 |
| 失败 detail | envelope 字段完整（新写入）；UI 可见 |
| 成功 detail | envelope 空/省略；不显示假错误块 |
| CSV | 文件下载成功；无参数列 |
| 无权限 | 403；无列表/导出 |
| 无匹配 | 既有 empty 态 |

---

## 8. Acceptance scenarios

### BDD-CE-U11-IRC-001 — resolvedReleaseVersion 过滤（API）

```gherkin
Given 模板存在 resolvedReleaseVersion 分别为 "1.0.0" 与 "1.2.0" 的调用记录
And 会话 canManageApiPolicy
When GET invocations?resolvedReleaseVersion=1.2.0
Then 仅返回 1.2.0 行
And totalElements 与筛选一致
```

### BDD-CE-U11-IRC-002 — 过滤器可组合

```gherkin
Given 混合 status 与 releaseVersion 的记录
When 同时传 resolvedReleaseVersion=1.2.0 与 status=FAILED
Then 仅返回同时满足的行
```

### BDD-CE-U11-IRC-003 — FE Release version 筛选（E2E）

```gherkin
Given 管理员在 Invocation history 面板
When 输入/选择 Release version=1.2.0 并 Apply
Then 表格仅显示该版本行
And 请求 query 含 resolvedReleaseVersion=1.2.0
```

### BDD-CE-U11-IRC-004 — Drawer 展示 error envelope

```gherkin
Given 一条失败调用已持久化 code=REQUEST_BODY_INVALID、messageKey、category、retryable
When 管理员打开该行 summary drawer
Then 可见 code、category、messageKey、retryable
And 不可见 parameters / variables
```

### BDD-CE-U11-IRC-005 — 成功调用无假错误块

```gherkin
Given 一条成功调用
When 打开 drawer
Then 不展示错误 envelope 主区块（或字段为 — 且无伪造 code）
```

### BDD-CE-U11-IRC-006 — 新失败写入可排障

```gherkin
Given 运行时生成因契约错误失败
When 写入 api_invocation_record
Then 管理 detail 可读取统一 error envelope 字段
And 后续 Drawer 可展示（单测或集成）
```

### BDD-CE-U11-IRC-007 — CSV 导出尊重筛选

```gherkin
Given 已 Apply resolvedReleaseVersion=1.2.0（可含其它过滤）
When 管理员 Export CSV
Then 文件仅含筛选命中集（非未过滤全表）
And 含 invocationId、requestId、resolvedReleaseVersion、status、errorCode/errorMessageKey（若有）
And 不含 parameters / variables 列或单元格
```

### BDD-CE-U11-IRC-008 — 授权 fail-closed

```gherkin
Given 会话无 canManageApiPolicy
When 请求 invocations 列表、detail 或 CSV 导出
Then 拒绝（403）
And 无调用摘要泄漏
```

---

## 9. Boundary / exception

- 未知 / 空 `resolvedReleaseVersion` 历史行：仅在「不过滤」时出现；精确匹配不会命中 null。
- 存量失败无 envelope 列：Drawer 诚实显示不可用；不阻断列表。
- CSV 超大结果：允许异步或上限 + 提示（实现固定）；不得静默截断却无提示。
- `invocationKind` BATCH 行：导出规则与列表视图一致（管理默认 logical/flat 既有行为保留）。

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | query param + detail envelope JSON |
| UI | 筛选器、Drawer 错误区块、Export 按钮 |
| 文件 | CSV 内容抽检 |
| Gates | mvn verify + FE gates；E2E+UIUX；C6 回归（无参数） |

---

## 11. Traceability

| 来源 | 关系 |
| --- | --- |
| CE-U11 plan §4 / R3 GAP-07 | 目标行为 |
| Task Master **#86** | 执行任务（TM slice 名 `ce-u11-invocation-recall`） |
| [management-invocation-history.md](./management-invocation-history.md) | 基线 HIST 场景；本片扩展 |
| ADR-0040 | 留存与 C6 |
| 现状 Filters/Detail/Drawer | 待扩展行为 |

---

## 12. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-u11-invocation-troubleshoot.md
task_ids: ["#86", "CE-U11"]
scenario_ids:
  - BDD-CE-U11-IRC-001
  - BDD-CE-U11-IRC-002
  - BDD-CE-U11-IRC-003
  - BDD-CE-U11-IRC-004
  - BDD-CE-U11-IRC-005
  - BDD-CE-U11-IRC-006
  - BDD-CE-U11-IRC-007
  - BDD-CE-U11-IRC-008
```
