# BDD 行为规格：包级 API 接入、约定配置与调用记录

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-03  
**BDD ID**: `BDD-API-PACKAGE-ACCESS-INVOCATION-001`  
**来源**: 产品讨论（包结构 API、约定大于配置、调用记录与可选文档留存）

---

## 1. 概述

本规格重构 **API 管理** 的产品语义与实现边界：

| 域 | 改造要点 |
| --- | --- |
| **D1 包结构 API** | 模板包首次发布自动 materialize 包级 `api_policy`；同时生效 **包 default API** 与 **release explicit API**；第二次及后续发布 **不静默改 default** |
| **D2 约定大于配置** | 管理 UI 默认只展示必要接入项；高级策略（输出模式、批量上限、加密等）平台约定默认，折叠为「高级设置」 |
| **D3 调用记录** | 独立于管理端审计的 **InvocationRecord**；包级配置是否留存生成文档、文档/记录各保留多久；调用方可查询参数与结果（在授权范围内） |

**与现有代码的关系（reuse / extend / replace）**

| 现有资产 | 本规格用法 |
| --- | --- |
| `ApiPolicyEntity` + `api_policy` | **Extend** — 增加留存策略字段；发布时 `ensure` 而非手工新建 |
| `ContractAssemblyService` | **Reuse** — default + explicit 路径组装逻辑不变 |
| `RuntimeGenerationAuditRecorder` + `runtime_generation_audit_event` | **Reuse** — 合规审计摘要（管理员）；**不**作为调用方查询面 |
| `GenerationIdempotencyEntity` + `IdempotencyConstants.RETENTION_SECONDS` (7d) | **Reuse** — 幂等窗口；可与调用记录留存解耦（幂等短、记录可更长） |
| `DocumentDownloadService` + MinIO `responseStorageKey` | **Extend** — 包级「保存文档」开启时延长 artifact 生命周期 |
| `ApiPolicyDetailView` / `ApiPolicyHomeView` | **Replace IA** — 收拢至包 Hub；独立列表降级或移除 |
| `PublishGateService.apiPolicyItem` | **Change** — 从「policy 是否存在」→「callable-ready + 首次发布 auto-materialize 后检查 AD Group 等」 |

---

## 2. Actor / Role

| Actor | 说明 | 权限 |
| --- | --- | --- |
| **分组/全局管理员** | 编辑包级接入配置、留存策略、default 路由、AD Group | `canManageApiPolicy` + 组范围 |
| **模板编排人员** | 包 Hub 只读查看契约与接入摘要 | 模板读 + 组范围 |
| **API 调用方** | 调用生成 API；查询**自身**调用记录与结果 | API 凭证 + AD Group + 模板级授权 |
| **审计管理员** | 管理端审计控制台（摘要、合规） | 审计角色；**不**替代调用记录 API |

---

## 3. Goal

1. **D1**：发布即有两条对外 API（default + explicit），配置是包的固定属性，不是「新建条目」流程。
2. **D2**：管理员打开包「对外接入」即可看到 **80% 场景必要项**；其余用平台约定默认。
3. **D3**：调用方可按 `requestId`/记录 ID 找回调用参数与生成结果（在留存期内）；平台可配置是否服务端备份文档及保留时长。

---

## 4. 已确认决策（2026-07-03 用户确认）

| ID | 决策 |
| --- | --- |
| **C1** | 首次发布 release `R` 时：`api_policy.defaultRouteReleaseVersion = R`（选项 A） |
| **C2** | 首次发布同时 materialize：**包 default API** + **该 release 的 explicit API** |
| **C3** | 第二次及后续发布 **不自动** 修改 default；改 default 必须显式配置 + 影响预览 + 确认 + 审计 |
| **C4** | API 管理本质是 **配置编辑**，不是流程或 catalog 新建 |
| **C5** | 约定大于配置：默认 UI 不铺开全部策略域 |
| **C6** | **Q1 参数回读**：调用方查询**自己的** invocation 详情含 **完整** `variables`/`context`/batch items；管理端审计与管理 UI **仍仅摘要**（与现有 `RuntimeGenerationAudit` 一致） |
| **C7** | **Q2 留存策略**：包级 API 配置 **可配**；**约定默认** = 保存文档 **开**、调用记录 **90 天**、文档 **30 天**；UI 提供**预设选项**（非任意整数）；上限：调用记录 **最长 7 年**、文档 **最长 1 年** |
| **C8** | **Q3 文档先过期**：允许 **记录留存 > 文档留存**；文档到期清理 artifact 后，invocation 仍可查参数与 metadata，`download` 返回 **410** |
| **C9** | **Q4 批量记录**：**单表**存储（与单笔同结构）；逻辑上 batch 双层（`BATCH_ROOT` + `BATCH_ITEM`）；行上标 `invocationKind` / `batchId` / `parentInvocationId`；客户端 **两个查询维度**：**真实调用列表**（batch 聚合为父）与 **平铺调用列表**（含 item 行，同结构一次拉取） |
| **C10** | **P1 骨架 policy**：进入 `PENDING_RELEASE` 时 materialize 骨架 `api_policy`（`defaultRoute` 空）；**publish** 时写入 `defaultRouteReleaseVersion=R` |
| **C11** | **P2 flat 视图**：`view=flat` **不含** `BATCH_ROOT`，仅 `SINGLE` + `BATCH_ITEM`（+ 完成的 async 单行） |
| **C12** | **P3 参数存储**：`parametersStorage` **禁止**明文 encryption 密码；仅 enabled/permissions 摘要 |
| **C13** | **P4 幂等 replay**：`IDEMPOTENCY_REPLAYED` **不**新建 invocation；按 `requestId`/`idempotencyKey` 关联原记录 |
| **C14** | **P5 留存审计**：留存策略变更使用 `changedAreas` += `INVOCATION_RETENTION` |
| **C15** | **P6 管理摘要**：包 Hub L2 只读最近调用摘要（无 variables 明文）；合规仍走 audit 控制台 |

---

## 5. 方案迭代摘要（设计自洽三轮）

### 轮 1 — 问题分解

- 将「API 管理」拆为：**路由面**（派生自包+release）、**接入策略**（包级 policy）、**调用记录**（运行时 append-only）。
- 避免第三套「API 列表」导航；包 Hub 版本线 + 对外接入 Tab 为主入口。

### 轮 2 — 与代码对齐

- **不**把 `RuntimeGenerationAuditEvent` 直接暴露给调用方（字段为摘要、面向合规）。
- 新增 **`ApiInvocationRecord`**（或等价命名）在每次成功/失败生成时写入；与幂等记录 **关联但分离**（同一 `idempotencyKey`/`requestId` 可互链）。
- 留存策略挂在 **`api_policy`**（包级 1:1），与「模板包绑定策略」一致。
- 平台约定默认写入 `ApiPolicyEntity` 构造/seed：`DOCX+PDF`、`SYNC_STREAM`+`SYNC_DOWNLOAD_URL`+`ASYNC_TASK`、batch 上限 100/10000、加密默认关。

### 轮 3 — UI 信息架构（L1 / L2 / 高级）

**包 Hub → 对外接入 Tab（默认）**

| 区块 | L1 默认展示 |
| --- | --- |
| 路由 | 包 `externalId`；default 路径 + 当前指向 release；版本线表格各 release 的 explicit 路径 |
| 必要配置 | 允许调用的 AD Group；default 指向哪条 release（下拉，变更走影响预览） |
| 留存 | 是否保存生成文档；调用记录保留天数；文档保留天数 |
| 接入账号 | 凭证列表（按需创建） |
| 高级设置（折叠） | 输出格式/模式、批量、加密 — 显示「当前生效约定默认」+ 可选覆盖 |

**侧栏「对外服务」**：改为跨包 **监控/待办**（缺 AD Group、即将过期凭证），**不是**第二份模板 catalog。

---

## 6. 领域对象（草案）

### 6.1 `ApiPolicyEntity` 扩展字段

| 字段 | 类型 | 约定默认 | 说明 |
| --- | --- | --- | --- |
| `saveGeneratedDocuments` | boolean | `true` | 是否在服务端保留生成物供重取；L1 可关 |
| `invocationRecordRetentionDays` | int | `90` | 调用记录（含完整参数）保留天数；**max 2555（7×365）** |
| `documentRetentionDays` | int | `30` | 生成文档保留天数；仅 `save=true`；**max 365**；且 **≤ invocationRecordRetentionDays** |

**L1 预设选项（i18n 文案，存储为 days）**

| 调用记录 retention | 文档 retention（save=true 时） |
| --- | --- |
| 7 / 30 / 90 / 180 / 365 / 1095 / 2555 天 | 7 / 30 / 90 / 180 / 365 天 |

- 关闭 `saveGeneratedDocuments` 时隐藏/禁用文档 retention 控件；记录 retention 仍生效。
- 变更 retention 只影响**新产生**的 invocation/artifact 的 `*ExpiresAt`；已存在记录不 retroactive 延长/缩短（除非另定 `[ASSUMED]`）。
- 平台全局 **幂等窗口** 仍维持现有 7 天（`IdempotencyConstants`），与调用记录留存 **解耦**。

### 6.3 四层时钟（R3 — 实现必须区分）

| 层 | 字段/机制 | 默认 | 说明 |
| --- | --- | --- | --- |
| L0 下载 URL | `downloadExpiresAt` | **15 分钟** | 不变（requirements）；每次签发 download URL |
| L1 幂等 | `generation_idempotency.expiresAt` | **7 天** | 重复提交识别；与包级留存无关 |
| L2 文档 artifact | `documentExpiresAt` / MinIO | **包级** `documentRetentionDays` | 仅 `saveGeneratedDocuments=true` |
| L3 调用记录 | `recordExpiresAt` | **包级** `invocationRecordRetentionDays` | 含完整参数（C6，密码已 strip） |

L2 可早于 L3 结束（C8）；L0 可多次重签（requirements 允许在 artifact 保留期内重签 download）。

### 6.2 `ApiInvocationRecord`（新）

| 字段 | 说明 |
| --- | --- |
| `invocationId` | `INV-` + opaque token |
| `templateId` / `templateExternalId` | 包 |
| `credentialId` / `accessAccount` | 调用主体 |
| `requestId`, `idempotencyKey` | 追踪 |
| `routeType`, `requestedReleaseVersion`, `resolvedReleaseVersion` | 路由 |
| `outputFormat`, `outputMode`, `outcome`, `durationMs` | 结果摘要 |
| `parametersStorage` | **C6** — 加密-at-rest 完整请求 JSON；仅 invocation GET（本 credential）返回；审计不含明文 |
| `documentId`, `artifactStorageKey` | 结果引用；artifact 可选 |
| `recordExpiresAt`, `documentExpiresAt` | 清理时间 |
| `invocationKind` | `SINGLE` \| `BATCH_ROOT` \| `BATCH_ITEM` \| `ASYNC_TASK`（async 可映射为 ROOT，完成时更新） |
| `batchId`, `parentInvocationId`, `itemId` | batch 关联；**ITEM 与 SINGLE 同行结构** |
| `isBatch` | 派生或冗余列，便于平铺列表筛选（`BATCH_ROOT`/`BATCH_ITEM` → true） |

**同表原则（C9）**：单笔、batch 父、batch item、async 任务 **均写入 `api_invocation_record` 单表**，字段对齐，避免客户端解析多套 schema。

**与审计**：每次写入 InvocationRecord 时 **仍** 写 `RuntimeGenerationAuditEvent`（摘要）；两者通过 `auditId`/`requestId` 关联。

---

## 7. 系统行为

### 7.1 首次发布（D1）

**When** `TemplateLifecycleService.publish()` 成功，release = `R`  
**Then**:

1. 若不存在 `api_policy` → 创建，`policyVersion=1`，`defaultRouteReleaseVersion=R`，平台约定默认填充其余字段。
2. 契约/路径中立即可用：
   - `…/templates/{externalId}/default/generate` → 解析到 `R`
   - `…/templates/{externalId}/versions/R/generate`
3. 不创建凭证（凭证仍为按需）。

### 7.2 后续发布（C3）

**When** 发布新 release `R2`，已有 default=`R1`  
**Then**: 新增 explicit 路径 `versions/R2/generate`；**default 仍为 R1** 直至管理员显式变更。

### 7.3 改 default（高风险）

沿用 P17 `DEFAULT_ROUTE_TARGET` 域：影响预览 → 硬阻断/警告 → 确认 → `policyVersion++` → `API_POLICY_UPDATED` 审计。**禁止**发布流程、定时任务或导入 silently 修改 default。

### 7.4 调用与记录（D3）

**When** 运行时生成 API 成功或失败  
**Then**:

1. 写 `ApiInvocationRecord`（生命周期 = `invocationRecordRetentionDays`）。
2. 若 `saveGeneratedDocuments=true` 且成功有 artifact → 持久化 MinIO，`documentExpiresAt` = now + `documentRetentionDays`。
3. 若 `saveGeneratedDocuments=false` → 仍写 invocation 记录；artifact **不**延长保留（仅幂等 7 天窗口 + 既有 15 分钟 download URL 规则）；`documentExpiresAt` 为空或即时不可用（**C8 不适用**）。
4. 调度任务清理过期 record / artifact（类似 `PreviewTempCleanupScheduler` 模式）。

### 7.5 调用方查询 API（新，OpenAPI 待增）

| 方法 | 路径（草案） | 说明 |
| --- | --- | --- |
| GET | `/api/{env}/v1/templates/{externalId}/invocations` | 分页；`view=logical`（默认）= **真实调用列表**（batch 仅 ROOT）；`view=flat` = **平铺列表**（含 BATCH_ITEM，与 SINGLE 同结构） |
| GET | `/api/{env}/v1/templates/{externalId}/invocations/{invocationId}` | 详情；ROOT 可内嵌或链接子 item 摘要 |
| GET | 现有 `…/documents/{documentId}/download` | 在 document 未过期且 save=true 时重取 |

---

## 8. Acceptance Scenarios（Given / When / Then）

### S1 — 首次发布 materialize 双 API

- **Given** 包 `PENDING_RELEASE`，无 `api_policy`
- **When** 发布 `1.0.0`
- **Then** 存在 policy 且 `defaultRouteReleaseVersion=1.0.0`；契约含 default 与 `versions/1.0.0` generate 路径；包 Hub 无「未配置 API」空状态

### S2 — 二次发布不改 default

- **Given** 已发布 `1.0.0`，default=`1.0.0`
- **When** 发布 `2.0.0`
- **Then**  callable 含 `2.0.0` explicit；default 仍解析到 `1.0.0`

### S3 — 显式改 default

- **Given** default=`1.0.0`
- **When** 管理员改 default→`2.0.0` 并确认影响预览
- **Then** `policyVersion` 递增；审计 `changedAreas` 含 `DEFAULT_ROUTE_TARGET`；**无**静默变更

### S4 — 约定默认 + 高级折叠

- **Given** 新 materialize 的 policy
- **When** 管理员打开包对外接入 Tab
- **Then** L1 见 AD Group、default、留存三项、路由摘要；高级域默认折叠且显示「使用平台默认」

### S5 — 调用记录写入与查询

- **Given** `saveGeneratedDocuments=true`，留存配置已设
- **When** 调用方成功调用 default generate
- **Then** 产生 `ApiInvocationRecord`；调用方 GET invocations 可见；详情含完整参数（C6）；document 在 retention 内可 download

### S6 — 记录与审计分离

- **Given** 一次成功调用
- **Then** 同时存在 `RuntimeGenerationAuditEvent`（摘要）与 `ApiInvocationRecord`（调用方可查）；审计管理员 **不能** 通过调用记录 API 越权查看其他凭证记录

### S7 — 平铺 vs 真实调用列表

- **Given** 一次 batch 含 3 个 item 均成功
- **When** GET invocations `view=logical`
- **Then** 返回 1 条 `BATCH_ROOT`（含 batch 汇总）
- **When** GET invocations `view=flat`
- **Then** 返回 **3 条** `BATCH_ITEM`（与 `SINGLE` 同结构；**不含** `BATCH_ROOT`）；每条 `isBatch=true`、`batchId` 相同；可按 `batchId` 自行聚合

### S8 — 留存配置可改

- **Given** 包 policy 默认 90/30 天
- **When** 管理员在 L1 改为记录 365 天、文档 180 天并保存
- **Then** `policyVersion++`；**新**调用按新 TTL 写 expires；UI 仅展示预设选项且 ≤ max（7y/1y）

---

## 9. 边界与异常

- 无 AD Group 时：包已发布、双路径可见，但运行时可调用性 fail-closed（现有 `policyNotConfigured` / AD Group 校验）。
- default 指向已停用 release：影响预览 hard-block（复用 P17）。
- 记录过期：GET invocation 返回 410；download 返回 410。
- 参数查询：跨凭证 `invocationId` → 403。

---

## 10. 用户确认记录（§11 — 全部完成）

| ID | 决策 |
| --- | --- |
| **Q1** | **C6** — 调用方全量参数（自己的记录）；管理端仅摘要 |
| **Q2** | **C7** — 默认 save 开 / 记录 90d / 文档 30d；可配；预设选项；max 7y / 1y |
| **Q3** | **C8** — 记录可长于文档；文档过期后 download 410，记录仍可查参数 |
| **Q4** | **C9** — 单表同结构；batch 双层；`view=logical` / `view=flat` |

---

## 12. 可追溯性

- `docs/product/catalog-navigation-ux.md` — 包 Hub IA
- `docs/requirements/requirements-plan.md` §动态 API、§API 管理边界
- `docs/domain/domain-model.md` §2.12 动态 API / ApiInvocationRecord
- `backend/.../ContractAssemblyService.java` — 双路径
- `backend/.../PublishGateService.java` — 发布门禁
- `backend/.../RuntimeGenerationAuditRecorder.java` — 审计
- `docs/adr/api-management/0040-api-package-access-and-invocation-retention.md` — 四层时钟、auto-materialize、invocation 查询
- `backend/.../GenerationIdempotencyEntity.java` — 幂等/artifact

---

## 13. BDD 就绪声明

**Ready for implementation** (2026-07-03) — BDD decisions C1–C15 confirmed; requirements, PRD, domain model, permission matrix, contract outline, and plan layer synced (doc-only pass; no code).

---

## 14. 设计审查（两轮）

### 14.1 第一轮 — 内部一致性与代码/需求冲突

| # | 严重度 | 发现 | 建议优化 |
| --- | --- | --- | --- |
| R1 | 🔴 | **发布门禁与 auto-materialize 死锁**：`publish()` 先调 `assertReady()`，其中 `apiPolicyItem` 要求 policy **已存在**；规格却在 publish 时才创建 policy | 在 `assertReady` **之前**执行 `ensureApiPolicy(releaseVersion)`；门禁改为 **callable-ready 警告**（空 AD Group = 发布可过、运行 fail-closed），或拆为「发布阻断」仅内容/测试项 |
| R2 | 🔴 | **全局 7 天 vs 包级 90/365 天**：`requirements-plan` §116、`IdempotencyConstants` 写生成结果 7 天；C7 允许文档最长 1 年 | 需求文档 **显式修订**：幂等窗口仍 7 天；**包级 documentRetention** 在 `save=true` 时 **覆盖** artifact 生命周期；download URL 仍 **15 分钟**（第三层，不变） |
| R3 | 🟡 | **三层时间线未写清**：`downloadExpiresAt`(15m)、`generation_idempotency.expiresAt`(7d)、`documentRetentionDays`、`invocationRecordRetentionDays` | 规格 §6.3 补充 **四层时钟** diagram；实现时 artifact TTL 写 MinIO lifecycle 或 DB `documentExpiresAt` |
| R4 | 🟡 | **平台约定默认与代码不一致**：规格写 DOCX+PDF；`ApiPolicyEntity` 构造仅 `["DOCX"]` + `SYNC_STREAM` | 新增 `ApiPolicyPlatformDefaults` 常量；materialize 与 import 均引用同一源 |
| R5 | 🟡 | **待发布期无法预配 AD Group**：policy 若仅 publish 创建，则 `PENDING_RELEASE` 期间无法走现有 `ApiManagementService` 编辑 | **推荐**：进入 `PENDING_RELEASE` 时 materialize **骨架** policy（default 空）；publish 时 **仅** 写入 `defaultRouteReleaseVersion=R`（仍不静默改已有 default） |
| R6 | 🟡 | **模板 import 可 silent 清空 default**：`TemplateImportService.applyPolicySnapshot` 传 `null` defaultRoute | Import 规则：**禁止**覆盖 defaultRoute；或走与 P17 相同的影响预览 |
| R7 | 🟡 | **S7 flat 视图曾写 4 条含 ROOT**：与「平铺同结构」矛盾 | **已修正**：flat = 仅 `SINGLE` + `BATCH_ITEM`（+ 完成的 async 单行）；logical = `SINGLE` + `BATCH_ROOT` + `ASYNC_TASK` |
| R8 | 🟢 | **规格文首/文末重复 §12、§7.4 残留 PENDING** | 已清理 |
| R9 | 🟢 | **`isBatch` 冗余**：可由 `invocationKind` 派生 | DB 可保留 generated column 或 API 层派生；flat 筛选需要索引时再冗余 |

### 14.2 第二轮 — 产品、安全、运维与 API 设计

| # | 严重度 | 发现 | 建议优化 |
| --- | --- | --- | --- |
| R10 | 🔴 | **C6 完整参数含 encryption 密码**：存 `parametersStorage` 全量 JSON 会持久化 `openPassword`/`ownerPassword`，违反 requirements 禁止明文 | 写入前 **strip** encryption 子树密码字段，或仅存 `encryption.enabled` + permissions 摘要；详情 API 文档声明 |
| R11 | 🟡 | **幂等 REPLAY 是否记新 invocation**：未定义 | **约定**：`IDEMPOTENCY_REPLAYED` **不**新建 invocation；GET 按 `idempotencyKey`/`requestId` 指向原记录；响应头带 `invocationId` |
| R12 | 🟡 | **缺少按 requestId 查询**：用户目标提到 requestId | 增加 `GET …/invocations?requestId=` 或 detail by requestId（credential  scoped） |
| R13 | 🟡 | **管理端调用记录面缺失**：只有 caller API；管理员合规排查只能看 audit 摘要 | 包 Hub L2 **只读**「最近调用」列表（摘要、无 variables 明文）；完整参数仍仅 caller + 审计 |
| R14 | 🟡 | **留存变更 `changedAreas`**：C7 新字段是否独立配置域 | 新增 `INVOCATION_RETENTION`（或并入 `OUTPUT_POLICY` 不推荐）；变更 bump `policyVersion` + 影响预览（提示仅影响新调用） |
| R15 | 🟡 | **`save=false` 时 flat 列表语义**：无 document 可下，记录仍有参数 | 记录 `artifactSaved=false`；download 始终 410；flat 仍列出条目供对账 |
| R16 | 🟡 | **async 与 batch 统一**：`GenerationAsyncTaskEntity` 已有 `requestPayloadJson`、`expiresAt` | invocation 表 **引用** `taskExternalId`；ASYNC_TASK 行在 ACCEPTED 创建、终态更新；避免双写 payload |
| R17 | 🟡 | **跨模板查询**：多模板凭证需多次 GET | v2 可选 `GET /api/{env}/v1/invocations`（credential 全局）；v1 先 per-template 与现有路由一致 |
| R18 | 🟢 | **侧栏监控待办**：缺 AD Group、凭证将过期 | 与 P21 task hub 模式对齐；Dashboard 卡片而非 catalog |
| R19 | 🟢 | **历史包 backfill**：已发布无 policy 的模板 | Flyway 数据迁移 + 一次性 seeder；default 指向 **当前最新 published release**（仅 backfill，非静默业务规则） |
| R20 | 🟢 | **审计 5 年 vs 调用记录 7 年**：不冲突 | audit 摘要长期；invocation 是产品记录；到期策略独立 |
| R21 | 🟢 | **存储成本**：7 年 × save=true × 高调用量 | 高级设置保留「关闭 save」；监控 MinIO bucket；preset 上限已 cap |

### 14.3 审查结论已纳入规格（C10–C15）

| 原建议 | 规格 ID |
| --- | --- |
| P1 骨架 policy @ PENDING_RELEASE | **C10** |
| P2 flat 不含 ROOT | **C11** |
| P3 strip encryption 密码 | **C12** |
| P4 replay 不 duplicate | **C13** |
| P5 INVOCATION_RETENTION | **C14** |
| P6 管理端只读摘要 | **C15** |

### 14.4 推荐实现顺序（修订）

1. **R1/R5** — publish/PENDING_RELEASE materialize + 门禁调整  
2. **R4** — platform defaults 单源  
3. **D2 UI** — 包 Hub 对外接入（L1 + 高级折叠）  
4. **R2/R3** — invocation 表 + 四层 TTL + 清理 job  
5. **R10-R12** — 写入 sanitization + caller GET API  
6. **R13** — 管理端只读摘要  
7. **R19** — backfill  
