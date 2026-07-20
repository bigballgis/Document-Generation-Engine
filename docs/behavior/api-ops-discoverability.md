# BDD 行为规格：API 运维可发现性（C10 前端对齐 + Overview 摘要 + PENDING_RELEASE 告警）

**文件状态**: `ready`  
**版本**: 1.0.1  
**编写日期**: 2026-07-14  
**BDD ID**: `BDD-API-OPS-DISCOVERABILITY-001`  
**Slice**: `api-ops-discoverability`  
**bdd_readiness**: **`ready`**  
**Worktree**: `D:/working/DGE-api-ops-discoverability` · `feat/api-ops-discoverability`  
**Formal phase**: **None**（非正式 P-phase；不 invent sole-active）  
**授权依据**: Parent / delivery-orchestrator Stage 1 — 本会话明确切片范围（P1–P4）

> **SYS-NORM Wave 2 navigation sync (2026-07-21):** Discoverability **semantics**
> (`PENDING_RELEASE` pre-provision, overview alerts, `adGroupsConfigured` warnings) remain in
> force. **Surface wording:** Hub External access / `?tab=apiAccess` is **retired**; operators
> reach package API settings via `/api/packages/:templateId/settings` (and legacy redirects).
> See [ADR-0040 Amendment 2026-07-21](../adr/api-management/0040-api-package-access-and-invocation-retention.md)
> and [sys-norm-hub-ia.md](./sys-norm-hub-ia.md). Historical scenarios below that name the hub
> tab remain acceptance for **C10 capability**; implementers map UI assertions to the settings
> shell (Wave 3 completes panels).

---

## 1. 概述

本切片关闭 **C10 骨架 policy** 与管理 UI / 跨包 Overview 之间的可发现性缺口：后端已允许在 `PENDING_RELEASE` 读写包级 `api_policy`（`ApiManagementAccessSupport.requirePublishedTemplate`），但前端曾把 External access Tab 绑在 `PUBLISHED`，导致待发布期无法预配 AD Group、深链失效；Overview 缺少轻量就绪摘要，告警也未覆盖待发布缺 AD Group 的包。

| 优先级 | 域 | 摘要 |
| --- | --- | --- |
| **P1 Critical** | C10 前端对齐 | `PENDING_RELEASE` + `canManageApiPolicy` 时包级 policy 可加载/可编辑；深链可用（Wave 2+: settings shell；historical: hub `?tab=apiAccess`） |
| **P2** | Overview 轻量就绪摘要 | `/api/policies`（`ApiPolicyHomeView`）展示范围内统计；遵守 **SCEN-ALERT-04**（告警表 + 可选摘要卡，非分页 catalog） |
| **P3** | 告警扩展 | `ApiAccessAlertQueryService` 将 **缺 AD Group** 的 `PENDING_RELEASE` 纳入告警；保留 GROUP_ADMIN 组范围 |
| **P4** | 发布 / 可调用语义 | 将 `adGroupsConfigured` 诊断暴露为可见警告；区分「已发布」与「运行时可调用」 |

**明确非目标（Out of scope）**

| 非目标 | 处理 |
| --- | --- |
| 独立 API 配置 catalog / 第二套编辑面 | **禁止** — 遵守 [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md) package-first：Wave 2+ 主入口为 `/api/packages/:templateId/settings`；`/api/policies` 仅为跨包监控 |
| Launch checklist **#3b** | **Out of scope** |
| Boot 4.1 / Java 平台升级 | **Out of scope**（已由 `boot-4-1-upgrade` 处理） |
| 改变运行时 fail-closed 授权（无 AD Group → deny） | **禁止** — 本切片只提升可发现性与预配能力 |
| 宣称 production go-live / 激活 CD-3 | **禁止** |

---

## 2. Actor / Role

| Actor | 角色 | 权限 / 范围 |
| --- | --- | --- |
| **分组管理员** | `GROUP_ADMIN` | `canManageApiPolicy`；仅本授权 `groupCode` 内模板；fail-closed 无跨组泄漏 |
| **全局管理员** | `GLOBAL_ADMIN` | 同能力，跨组（`*`） |
| **发布人 / 团队负责人** | Publisher on `PENDING_RELEASE` | 可打开发布门禁；须能看到「可发布 ≠ 可调用」警告（若具备 policy 管理权则同 P1） |
| **模板编排只读** | Template reader | 无 `canManageApiPolicy` → **不**显示 External access 编辑 Tab；不得通过 Overview 告警越权 |
| **系统** | Hub + `ApiAccessAlertQueryService` + Publish gate diagnostics | 对齐 C10 生命周期；告警与摘要尊重组范围 |

---

## 3. Goal

1. **待发布即可预配**：`PENDING_RELEASE` 且具备 `canManageApiPolicy` 时，包 Hub External access Tab 可见、可加载、可编辑骨架 `api_policy`（与后端 C10 一致）。  
2. **深链可用**：`/api/policies/:id` → `/templates/:id?tab=apiAccess` 在 `PENDING_RELEASE` 下 Tab 已注册并激活（非整页空落）。  
3. **Overview 一眼就绪**：跨包页给出范围内轻量统计（已发布、需关注、待发布待配置），且 **不**变成第二份模板 catalog。  
4. **待发布缺 AD Group 可发现**：告警含 `PENDING_RELEASE` + 空 `allowedAdGroups`，深链进 Hub。  
5. **语义诚实**：UI 明确区分「lifecycle = PUBLISHED」与「runtime callable（AD Group 等已配）」；`adGroupsConfigured=false` 有可见警告。

---

## 4. Trigger

| Trigger | 说明 |
| --- | --- |
| 管理员打开包 Hub（`PENDING_RELEASE` 或 `PUBLISHED`） | P1 Tab / load |
| 访问 legacy `/api/policies/:templateId` 或 Overview 行深链 | P1 深链 |
| 打开 `/api/policies`（External services / API policy home） | P2 摘要 + P3 告警 |
| 打开发布门禁 / External access / Publish 相关面板 | P4 诊断可见性 |

---

## 5. Preconditions

| # | 前置条件 |
| --- | --- |
| PC1 | 包已进入 `PENDING_RELEASE` 时已按 **C10** materialize 骨架 `api_policy`（`defaultRoute` 可空）— 见 [api-package-access-and-invocation-records.md](./api-package-access-and-invocation-records.md) **C10** / R5 |
| PC2 | 后端 `ApiManagementAccessSupport.requirePublishedTemplate` **已**允许 `PUBLISHED` **与** `PENDING_RELEASE`（本切片不改该守卫语义，只对齐前端） |
| PC3 | 管理员会话具备 `canManageApiPolicy`；组范围由 `GroupAccessService` 解析 |
| PC4 | Overview 既有告警契约（[api-access-cross-package-alerts.md](./api-access-cross-package-alerts.md) SCEN-ALERT-01…05）仍成立；本切片 **扩展** 而非替换 |
| PC5 | Hub IA：`/api/policies/:id` redirect → `?tab=apiAccess`（SCEN-IA-01）已存在 |

---

## 6. Primary journey（成功路径）

1. 模板审批通过 → `PENDING_RELEASE`；骨架 `api_policy` 已存在（C10）。  
2. 具备 `canManageApiPolicy` 的管理员打开包 Hub → **External access** Tab 可见。  
3. 系统 `loadPolicyData` 成功拉取 policy（及凭证列表）；管理员配置 AD Group 并保存。  
4. 管理员打开 `/api/policies` → 见摘要卡（已发布数 / 需关注数 / 待发布待配置数）+ 告警表；缺 AD Group 的 `PENDING_RELEASE` 出现在告警中并可深链回 Hub。  
5. 发布前 / 发布门禁：`adGroupsConfigured=false` 以可见 WARNING 展示；文案区分「可发布」与「发布后运行时仍可能不可调用」。  
6. 发布成功 → lifecycle `PUBLISHED`；若 AD Group 已配则 runtime callable；若未配则仍 fail-closed（既有运行时行为不变）。

---

## 7. System responses

| 路径 | 系统响应 |
| --- | --- |
| **成功 P1** | `showPolicyPanel === true` when `lifecycleStatus ∈ {PUBLISHED, PENDING_RELEASE}` **and** `canManageApiPolicy`；`detailTabs` / Hub secondary tabs 含 `apiAccess`；`loadPolicyData` 发起 fetch |
| **成功 P2** | Overview 在告警表上方（或并列）展示轻量 summary；数据来自范围内模板/告警聚合，**非**全量分页 catalog |
| **成功 P3** | `listAlerts` 对范围内 `PENDING_RELEASE` 且 policy 存在且 `allowedAdGroups` 空 → `MISSING_AD_GROUP`；`hubDeepLinkPath` 仍为 `?tab=apiAccess`（可带 `#domain=AD_GROUP_AUTHORIZATION`） |
| **成功 P4** | Publish gate / Hub External access 将 `adGroupsConfigured`（或等价布尔）渲染为用户可见 warning（非仅埋在 summary 字符串） |
| **Fail-closed** | 无 `canManageApiPolicy` → 无编辑 Tab、Overview 告警 API 拒绝；GROUP_ADMIN 不可见他组模板/告警/统计 |

---

## 8. 已确认决策（本切片）

| ID | 决策 |
| --- | --- |
| **AOD-C1** | **P1 Tab 资格**：`showPolicyPanel`（及 Hub 等价开关）在 `PUBLISHED` **或** `PENDING_RELEASE` 且 `canManageApiPolicy` 时为 true。其它生命周期（`DRAFT` / `TESTING` / `APPROVAL` 等）保持 false。 |
| **AOD-C2** | **P1 深链**：`?tab=apiAccess` 必须出现在已注册 tabs 中；`PENDING_RELEASE` 下 deep link / redirect **不得**因 Tab 未注册而静默落到 overview 或空白。 |
| **AOD-C3** | **P1 加载**：当 `showPolicyPanel` 为 true 时，进入 Hub / 激活 apiAccess / 初次 load 路径须调用 `loadPolicyData`（与现 `PUBLISHED` 路径对称）。 |
| **AOD-C4** | **P2 摘要指标（最小集）**：Overview 展示至少：**(a)** 范围内 **PUBLISHED** 包数量；**(b)** **attention** 数量（当前告警涉及的 distinct 模板数，或等价「需关注」计数）；**(c)** **PENDING_RELEASE 且缺 AD Group / 待 setup** 数量。实现可用独立 summary API 或从 alerts + 轻量 count 派生；**禁止**为此引入分页模板列表。 |
| **AOD-C5** | **SCEN-ALERT-04 边界不变**：Overview = 监控（摘要卡可选 + 告警表 + 深链）；**不是**第二份 paginated template catalog。Browse templates 仅作为跳出链到模板管理。 |
| **AOD-C6** | **P3 告警扩展范围**：`MISSING_AD_GROUP` 覆盖范围内 **`PUBLISHED` ∪ `PENDING_RELEASE`**（policy 存在且 AD Group 空）。**凭证类告警**（`NO_CREDENTIALS` / `EXPIRING_CREDENTIAL`）本切片默认仍仅针对 **`PUBLISHED`**（待发布通常尚未发凭证；扩展凭证告警到 PENDING_RELEASE **不在**本切片必做范围）。 |
| **AOD-C7** | **组范围**：`GROUP_ADMIN` 仅本 `groupCode`；`GLOBAL_ADMIN`（`*`）全量；空组权限 → 空列表。与 SCEN-ALERT-05 一致并适用于摘要计数。 |
| **AOD-C8** | **P4 可见诊断**：`adGroupsConfigured=false`（publish gate item summary / diagnostic）必须在 Hub External access 和/或发布门禁 UI 以 **warning** 呈现，不能仅存在于机器可读 summary 字符串。 |
| **AOD-C9** | **P4 语义区分**：UI 文案区分 **Published**（lifecycle）与 **Runtime callable**（至少 AD Group 已配置；其它既有 callable-ready 规则可复用）。空 AD Group：**允许发布**（既有门禁「警告非硬阻断」语义保留），**运行时 fail-closed**（不变）。 |
| **AOD-C10** | **不新增独立 API catalog**；不改 ADR-0040 包级 policy / 四层时钟；不处理 #3b / Boot 4.1。 |

---

## 9. Acceptance scenarios（Given / When / Then）

### Priority 1 — C10 frontend alignment

#### SCEN-AOD-01 — PENDING_RELEASE 显示 External access Tab（required · Critical）

- **Given** 模板 `T` 的 `lifecycleStatus = PENDING_RELEASE`，且会话 `canManageApiPolicy = true`，骨架 `api_policy` 已存在（C10）
- **When** 管理员打开包 Hub `/templates/T`
- **Then** External access（`apiAccess`）Tab **已注册且可见**；`showPolicyPanel`（或 Hub 等价）为 true

#### SCEN-AOD-02 — PENDING_RELEASE 加载 policy 数据（required · Critical）

- **Given** 同 SCEN-AOD-01
- **When** 管理员激活 External access Tab，或 Hub 初次 load 且 panel 应显示
- **Then** 系统调用 `loadPolicyData`（fetch policy ± credentials）；成功时面板展示可编辑/可保存的接入配置（至少 AD Group 域）；失败时现有 LoadError + retry 语义保持

#### SCEN-AOD-03 — 深链在 PENDING_RELEASE 下激活 apiAccess（required · Critical）

- **Given** 同 SCEN-AOD-01
- **When** 管理员访问 `/api/policies/T`（redirect）或 `/templates/T?tab=apiAccess`
- **Then** 客户端落在 Hub 且 **apiAccess Tab 激活**；不因 Tab 未注册而回退到 overview/空白；不渲染独立 `ApiPolicyDetailView` 全屏编辑器（SCEN-IA-01 约束保持）

#### SCEN-AOD-04 — 无权限不暴露编辑面（boundary）

- **Given** 模板 `PENDING_RELEASE` 或 `PUBLISHED`，但会话 **无** `canManageApiPolicy`
- **When** 打开包 Hub
- **Then** External access **编辑** Tab 不出现；用户不能通过 UI 发起 policy 写操作

#### SCEN-AOD-05 — 非目标生命周期仍隐藏（boundary）

- **Given** 模板 `lifecycleStatus` 为 `DRAFT` / `TESTING` / `APPROVAL`（或其它非 `PUBLISHED`/`PENDING_RELEASE`）
- **When** 具备 `canManageApiPolicy` 的管理员打开 Hub
- **Then** `showPolicyPanel` 仍为 false；不注册 apiAccess 编辑 Tab（与本切片前 PUBLISHED-only 行为对非目标状态一致）

---

### Priority 2 — Overview lightweight API readiness summary

#### SCEN-AOD-06 — 摘要统计可见（required）

- **Given** 管理员有 `canManageApiPolicy`，范围内存在若干 `PUBLISHED` 与/或 `PENDING_RELEASE` 包
- **When** 打开 `/api/policies`（`ApiPolicyHomeView`）
- **Then** 页面展示轻量 readiness summary，至少包含：  
  - **published-in-scope** 计数  
  - **attention** 计数（需关注：有告警的 distinct 包，或产品等价定义）  
  - **pending-release needing setup** 计数（至少：`PENDING_RELEASE` 且缺 AD Group）

#### SCEN-AOD-07 — SCEN-ALERT-04 边界：非 catalog 重复（required · boundary）

- **Given** 范围内有大量已发布/待发布模板
- **When** Overview 打开
- **Then** 主内容为 **可选 summary 卡 + alerts 表 + 深链**；**不**渲染全量分页模板 catalog（复用并强化 [api-access-cross-package-alerts.md](./api-access-cross-package-alerts.md) **SCEN-ALERT-04**）；「Browse templates」仅为跳出导航

#### SCEN-AOD-08 — 摘要组范围（boundary）

- **Given** `GROUP_ADMIN` 仅授权 `RETAIL`
- **When** Overview 加载 summary
- **Then** 三项计数仅含 `RETAIL`；无跨组膨胀

---

### Priority 3 — Alerts include PENDING_RELEASE missing AD Group

#### SCEN-AOD-09 — PENDING_RELEASE 缺 AD Group 告警（required）

- **Given** 范围内模板 `T` 为 `PENDING_RELEASE`，存在 `api_policy` 且 `allowedAdGroups` 为空
- **When** `ApiAccessAlertQueryService.listAlerts`（或 Overview 拉取 alerts）
- **Then** 返回 `MISSING_AD_GROUP` 告警行，含模板名 / externalId / 深链 `?tab=apiAccess`（可带 AD Group domain hash）

#### SCEN-AOD-10 — 已配置 AD Group 的 PENDING_RELEASE 不产生 MISSING_AD_GROUP（boundary）

- **Given** `PENDING_RELEASE` 模板已配置非空 `allowedAdGroups`
- **When** 拉取 alerts
- **Then** **不**因该模板产生 `MISSING_AD_GROUP`（其它告警类型按 AOD-C6）

#### SCEN-AOD-11 — GROUP_ADMIN 无跨组泄漏（boundary · 强化 SCEN-ALERT-05）

- **Given** `GROUP_ADMIN` 仅 `RETAIL`；另一组存在缺 AD Group 的 `PENDING_RELEASE` / `PUBLISHED`
- **When** 拉取 alerts
- **Then** 结果不含他组模板；与既有 SCEN-ALERT-05 一致

#### SCEN-AOD-12 — 既有 PUBLISHED 告警回归（regression）

- **Given** 已发布模板缺 AD Group / 临期凭证 / 零凭证（符合既有 SCEN-ALERT-01…03）
- **When** Overview 加载
- **Then** 原有告警行为保持；本切片不回归破坏

---

### Priority 4 — Publish vs runtime callable semantics

#### SCEN-AOD-13 — adGroupsConfigured 可见警告（required）

- **Given** 模板 `PENDING_RELEASE`（或发布门禁上下文），policy 存在且 AD Group 未配置（`adGroupsConfigured=false`）
- **When** 管理员查看 External access Tab 和/或发布门禁检查项
- **Then** UI 以 **可见 warning** 展示该诊断（i18n English-first）；不得仅埋在不可见的 `summary` 字符串中

#### SCEN-AOD-14 — 区分 Published 与 Runtime callable（required）

- **Given** 同上，或已 `PUBLISHED` 但 AD Group 仍空
- **When** 管理员查看 Hub External access 或发布相关说明
- **Then** 文案区分：**lifecycle 已发布/可发布** ≠ **runtime callable**；明确空 AD Group 时运行时将 fail-closed，同时不把「缺 AD Group」误标为发布硬阻断（除非既有产品规则另行硬阻断——本切片不引入新硬阻断）

#### SCEN-AOD-15 — 配置 AD Group 后警告消失（success）

- **Given** 管理员在 PENDING_RELEASE 通过 External access 保存了非空 AD Group
- **When** 重新打开门禁 / External access / Overview
- **Then** `adGroupsConfigured` 相关 warning 与 `MISSING_AD_GROUP` 告警对该模板消失（在传播/刷新后）

---

## 10. Boundary / exception behavior

| 场景 | 期望 |
| --- | --- |
| 无 `canManageApiPolicy` 调 alerts API | 拒绝（既有 `ApiManagementAccessDeniedException` / 等价） |
| GROUP_ADMIN 空组权限 | 摘要全 0；告警空列表 |
| Policy 行缺失的 PENDING_RELEASE（异常数据） | 不 500 Overview；`MISSING_AD_GROUP` 可不生成或按「待 setup」计入摘要 — 实现选稳妥 best-effort；正常路径依赖 C10 骨架存在 |
| Malformed `allowedAdGroupsJson` | 与现告警服务一致：按空列表处理，不 500 |
| 深链目标模板已删 / 无权限 | 既有 Hub 加载失败 / 403 语义；不新开 catalog |
| Runtime 调用无 AD Group | **不变**：fail-closed deny（本切片不改 runtime） |

---

## 11. Observable evidence

| 层 | 证据 |
| --- | --- |
| **Frontend unit** | `useTemplatePolicyCredentials` / Hub tab 注册：`PENDING_RELEASE` + `canManageApiPolicy` → panel true；非目标状态 false |
| **Frontend UI** | Hub `?tab=apiAccess` 在 PENDING_RELEASE 可见；Overview 有 summary 区 + alerts；warning 文案可见 |
| **Backend unit** | `ApiAccessAlertQueryServiceTest`：PENDING_RELEASE 缺 AD Group 出告警；GROUP_ADMIN 无跨组；PUBLISHED 回归 |
| **API** | alerts（及可选 summary）响应仅含授权范围内模板 |
| **E2E（若本切片做 FE 用户面）** | Docker 验收：PENDING_RELEASE Hub apiAccess；Overview 告警行深链；可选截图/断言 |
| **i18n** | 新文案 English-first（`en.ts`）+ `zh-CN` 同步 |

---

## 12. Traceability

| 文档 / 资产 | 关系 |
| --- | --- |
| [api-package-access-and-invocation-records.md](./api-package-access-and-invocation-records.md) **C10**, R5, §15 IA redirect | **父规格** — 骨架 policy @ PENDING_RELEASE；Hub 为主入口 |
| [api-access-cross-package-alerts.md](./api-access-cross-package-alerts.md) SCEN-ALERT-01…05 | Overview 告警基线；本切片扩展 P3 + 强化 SCEN-ALERT-04 |
| [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md) | 包级 policy；禁止独立 API config catalog |
| `ApiManagementAccessSupport.requirePublishedTemplate` | 后端已允许 PENDING_RELEASE — P1 对齐目标 |
| `ApiAccessAlertQueryService` | P3 扩展点 |
| `PublishGateCheckItemSupport` / `adGroupsConfigured=` | P4 诊断来源 |
| `frontend/.../useTemplatePolicyCredentials.ts` `showPolicyPanel` | P1 当前缺陷点（仅 PUBLISHED） |
| `frontend/.../ApiPolicyHomeView.vue` | P2 / P3 UI 面 |
| `docs/product/catalog-navigation-ux.md` External access | IA 约束 |
| Launch checklist **#3b** / Boot 4.1 | **Explicitly out of scope** |

---

## 13. 与既有场景 ID 映射

| 既有 ID | 本切片关系 |
| --- | --- |
| C10（package-access BDD） | P1 实现前端对齐；不改 C10 后端决策 |
| SCEN-IA-01…04 | 深链/redirect 保持；AOD-03 覆盖 PENDING_RELEASE |
| SCEN-ALERT-01…03 | 回归保留（AOD-12） |
| SCEN-ALERT-04 | AOD-07 强化（摘要卡允许，catalog 禁止） |
| SCEN-ALERT-05 | AOD-08 / AOD-11 强化至 PENDING_RELEASE + summary |

---

## 14. BDD 就绪声明

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/api-ops-discoverability.md
task_ids: [52, slice:api-ops-discoverability]
```

**Ready for implementation**（2026-07-14）— Task Master **#52** allocated (`in-progress`); formal phase **None**. P1–P4 行为已确认并落盘；无阻塞未决问题。TDD Red 测试应以 **SCEN-AOD-01…15** 为验收主键，优先 Critical：**AOD-01 / AOD-02 / AOD-03**。实现顺序：**P1 → P3 → P2 → P4**。

---

## 15. Handoff notes（下一阶段）

1. `plan-orchestrator` — 分解 FE（showPolicyPanel + Hub tabs + Overview summary + i18n）与 BE（alerts 查询 PENDING_RELEASE；可选 summary endpoint）。  
2. 实现顺序建议：**P1 Critical → P3 → P2 → P4**（P2 可消费 P3 数据）。  
3. 大范围 requirements/PRD 同步若需要，交 `doc-keeper`；本文件为切片行为 SoT。
