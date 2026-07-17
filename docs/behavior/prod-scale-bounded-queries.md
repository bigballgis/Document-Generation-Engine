# BDD 行为规格：PRR Wave A — Prod-scale bounded queries

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-PRR-A01` / `BDD-PRR-A02` / `BDD-PRR-A03` |
| **编写日期** | 2026-07-17 |
| **Slice** | `prod-scale-bounded-queries` — **Done** (MAIN merge `4197770f` / feature tip `b107e9e6`; worktree **REMOVED**) |
| **Branch** | `feat/prod-scale-bounded-queries` (merged) |
| **Worktree** | removed |
| **Placement** | ISOLATED → merged to MAIN |
| **Task IDs** | Task Master **#98** (PRR-A01, leaf lead) · **#99** (PRR-A02) · **#100** (PRR-A03) — Batch Recommendation **merge** **closed** (`member_task_ids: ["98","99","100"]`) → **Done** |
| **`frontend_ui_in_scope`** | **`false`** |
| **Formal phase** | **None**（可靠性加固叶；不发明 sole-active 正式 P-phase） |
| **上游行为** | [lrp-d1-audit-retention.md](./lrp-d1-audit-retention.md)；[ce-g04-legal-hold.md](./ce-g04-legal-hold.md)；[ce-u18-batch-test-history.md](./ce-u18-batch-test-history.md)；[ce-u08-content-module-review-loop.md](./ce-u08-content-module-review-loop.md)；[template-testing-overhaul.md](./template-testing-overhaul.md) |
| **Owning docs** | **本文件（行为 SoT）**；ADR-0040 / ADR-0048 正文不改（豁免语义沿用 CE-G04） |

**完成声明约束：** 关闭三处无界 DB 加载带来的 OOM / 长事务 / 内存 TopN 风险；**不**改 FE UI；**不**做 Library export streaming（下一叶 solo）；**不**宣称 go-live；**不**激活 Wave B/C / CD-3。

---

## 1. 概述

生产体量下，若干后端路径仍以「一次查出全部候选再内存处理」运行，存在峰值内存与长事务风险。

| 任务 | 服务 | 现状缺口 | 目标 |
| --- | --- | --- | --- |
| **PRR-A01** | `AuditRetentionCleanupService`；`InvocationRetentionCleanupScheduler` | `findBy*Before` → 无界 `List` → 再过滤 legal-hold → `deleteAll` | **Pageable / LIMIT 分批**（批大小 **500–2000**）；短事务；**legal-hold 安全**（复用 CE-G04 / `LegalHoldExemptionService`） |
| **PRR-A02** | `PreviewGenerationService`；`BatchTestHistoryService` | `findByTemplateId…` 全量后 `.stream().limit(N)` | **TopN 在 DB**（Pageable / `LIMIT`）；对外仍返回最近 N 条，语义不变 |
| **PRR-A03** | `ContentModuleWorkflowService` | `findByReviewStateOrderByUpdatedAtDesc(SUBMITTED)` 无界 | **有界/分页**拉取 SUBMITTED 候选，再投影审批人可见 inbox |

**明确非目标（本叶不做）**

| 非目标 | 处理 |
| --- | --- |
| `LibraryExportService` 流式导出 | **Out of scope** — 下一叶 solo |
| Wave B（prod/LDAP 等）、Wave C（rendering ADR） | **Out of scope** |
| 前端 UI / Playwright E2E / UIUX | **`frontend_ui_in_scope=false`** |
| 修改 ADR-0040 / ADR-0048 默认窗口正文 | **禁止** — 仅加固加载形态 |
| 修改 CE-G04 hold 实体 / 管理 API / 管理页 | **禁止** — 仅要求清理路径继续调用豁免服务 |
| 修改 preview/batch-test **对外 limit 默认值**（preview **50**；batch-test 历史默认 **5**） | **禁止** — 仅把 TopN 下推到 DB |
| REWORK 路径 `findDraftVersionsWithRejectionReason` 无界加固 | **本叶非硬门槛**（建议同模式，可同 PR 顺手；验收以 SUBMITTED 为准） |
| 人工「立即 purge」UI / 改 retention 天数 | **Out of scope** |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **系统调度器** | `AuditRetentionCleanupScheduler` / `InvocationRetentionCleanupScheduler` | 定时清理；无交互用户 |
| **清理服务** | `AuditRetentionCleanupService` | 审计硬删 + purge-evidence（LR-D1） |
| **豁免服务** | `LegalHoldExemptionService` | CE-G04 唯一豁免入口；清理前必调 |
| **模板作者 / 读者** | 既有 preview / batch-test 授权 | 列表仍见最近 N 条；本叶无 UI 变更 |
| **条款审批人** | `decideContentModuleReviews` | Dashboard / workflow API 待审 inbox |
| **运维** | 配置批大小（可选） | 不得绕过 legal-hold 或 ShedLock |

---

## 3. Goal

1. 留存清理在超大候选集下仍以有界批次加载/删除，避免单次无界 `List` 与过长事务。  
2. ACTIVE legal hold 保护的行/产物**永不**被本叶清理删除（与 CE-G04 一致）。  
3. Preview / Batch-test 历史 TopN 在数据库侧完成；API 可见顺序与条数与现网语义一致。  
4. Content-module `SUBMITTED` 待审投影不再一次加载全表；峰值内存有界。  
5. 无前端变更；质量门以 `mvn verify` 为主。

---

## 4. 已确认决策（confirmed-for-this-leaf）

### 4.1 共享（PS-C*）

| ID | 决策 |
| --- | --- |
| **PS-C1** | **批大小窗口：** 留存清理每批加载/删除行数配置落在 **500–2000**（含端点）。默认 **1000**（`docgen.*.cleanup-batch-size` 或等价；实现锁定属性名）。低于 500 或高于 2000 → 启动失败或钳位到窗口（实现锁定一种；测试锁定默认 1000）。 |
| **PS-C2** | **短事务：** 每批删除（或产物清理更新）在独立短事务中提交；禁止「一次事务装载并删除全部超龄行」。单 tick 可循环多批直至无更多候选或达可选 per-tick 安全上限（实现可配；默认「直至耗尽本批查询为空」）。 |
| **PS-C3** | **Legal-hold 安全：** 删除/产物清理前必须经 `LegalHoldExemptionService`（G04-C9…C16）。豁免行跳过；零可删且有跳过 → 不写 purge-evidence（D1-C13 / G04-C16）。**禁止**先删后补。 |
| **PS-C4** | **可观测：** 每 tick / 每表日志含 `deleted`（或 cleaned）、`skippedLegalHold`、批次数；不得记录 variables / 凭证明文。 |
| **PS-C5** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify`。**无** FE lint/E2E 硬门槛。 |
| **PS-C6** | **测试：** 集成/单元须覆盖「候选集显著大于批大小」（例如 ≥ 批大小×3 或固定 ≥3000）时仍正确、且不依赖无界 `findAll` 式断言。 |

### 4.2 PRR-A01 — Retention bounded batch delete

| ID | 决策 |
| --- | --- |
| **A01-C1** | **范围：** (a) management + runtime 审计清理（`AuditRetentionCleanupService`）；(b) invocation 产物清理与记录硬删（`InvocationRetentionCleanupScheduler`）。 |
| **A01-C2** | **查询形态：** 以 `Pageable` / `LIMIT`（或等价 `Slice`）按 cutoff 取下一批候选；**退役**无界 `findByEventAtBefore` / `findByRecordExpiresAtBefore` / `findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull` 作为清理主路径（可保留仅用于测试辅助则须标注；生产路径禁止）。 |
| **A01-C3** | **审计：** 每批过滤豁免后 `deleteAll`（或等价 bulk delete by ids）；合计 `deletedCount > 0` 时仍按 LR-D1 写 **一条** per-table per-tick purge-evidence（`deletedCount` = 合计）。同 tick 自保护（D1-C12）保持。 |
| **A01-C4** | **Invocation：** 产物清理与记录删除均分批；豁免跳过产物删除与行删除；ShedLock 名与 skip-on-lock 语义不变（LR-B2）。 |
| **A01-C5** | **Cutoff / 窗口语义：** 不改 ADR-0040/0048 / LR-D1 天数与 `event_at < cutoff` / `recordExpiresAt` 谓词。 |

### 4.3 PRR-A02 — Preview / BatchTest history TopN at DB

| ID | 决策 |
| --- | --- |
| **A02-C1** | **Preview：** `listPreviews` 仍返回该模板最近 **`PREVIEW_HISTORY_LIMIT`（50）** 条，按 `createdAt DESC`。加载必须使用 Pageable/`LIMIT`（`PageRequest.of(0, 50)` 或 repo `find…(templateId, Pageable)`）；**禁止**全表 `findByTemplateIdOrderByCreatedAtDesc` 再 `stream().limit(50)`。 |
| **A02-C2** | **Batch test history：** `listRecentRuns(templateId, limit, …)` 在 DB 取 `hidden=false` 且按 `createdAt DESC` 的 TopN；**优先复用**既有 Pageable repo 方法，缺失则新增。**禁止**全量 `findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc` 再内存 limit。 |
| **A02-C3** | **API 契约：** 响应形状、授权（`requireReadableSnapshot`）、默认 limit（batch-test 调用方默认 5）、CE-U18 `sampleResults` 暴露语义 **不变**。 |
| **A02-C4** | **正确性：** 当历史行数 ≫ N 时，返回集合大小 ≤ N，且为全局最新 N 条（非「某内存子集的前 N」）。 |

### 4.4 PRR-A03 — ContentModule SUBMITTED bounded query

| ID | 决策 |
| --- | --- |
| **A03-C1** | **主路径：** `projectPendingReviewTasks` **不得**调用无界 `findByReviewStateOrderByUpdatedAtDesc(SUBMITTED)`。改为 Pageable/`LIMIT` 按 `updatedAt DESC` 分批拉取 SUBMITTED 版本。 |
| **A03-C2** | **投影语义：** 仍「每模块一条、最新匹配版本优先」+ 组访问过滤（现有 `canAccessModule`）。在扫描预算内须尽量覆盖审批人可见 SUBMITTED（对齐 CE-U08「不得静默只显示第一页」——即 **必须翻页**，不能只取 page 0）。 |
| **A03-C3** | **安全上限（confirmed default）：** 单次 `listWorkflowTasks` 返回的 `PENDING_REVIEW` 任务数默认上限 **500**（可配）；扫描候选上限默认 **批大小×页数** 直至满足上限或无更多页。触达上限后停止扫描，**打 warn 日志**（含 returned/scanned）；返回已投影的最新任务。本叶 API 若仍为裸 `List`，不强制新增 truncation 字段（FE 不在范围）。 |
| **A03-C4** | **授权：** 无 `canBrowseContentModuleCatalog` → 既有 deny；无 `canDecideContentModuleReviews` → 不投影 PENDING_REVIEW。不因本叶放宽跨组可见性。 |
| **A03-C5** | **批大小：** SUBMITTED 扫描页大小默认与 PS-C1 对齐（**1000**，窗口 500–2000），或实现选用更小 inbox 页（≥100）——测试不依赖具体页大小，只要求有界且翻页。 |

---

## 5. Trigger

| # | 触发 |
| --- | --- |
| T1 | Audit / invocation retention 调度 tick（获 ShedLock） |
| T2 | 集成测试直接调用 purge / cleanup 方法（大候选集） |
| T3 | 管理 API：`listPreviews` / `GET …/batch-tests?limit=` |
| T4 | 管理 API：content-module workflow tasks（Dashboard 待审投影后端路径） |

---

## 6. Preconditions

| # | 前置 |
| --- | --- |
| PC1 | LR-D1 / ADR-0048 审计清理与 CE-G04 legal-hold 豁免已存在 |
| PC2 | ADR-0040 invocation 清理与 ShedLock 已存在 |
| PC3 | Preview / batch-test 历史表有数据时可验证 TopN |
| PC4 | 存在 `SUBMITTED` 内容模块版本时可验证 inbox 投影 |
| PC5 | 测试可注入 Clock / 造大量超龄行或历史行 |

---

## 7. Primary journey

### 7.1 Retention（PRR-A01）

1. 调度器获锁 → 按 cutoff 以批大小 N 拉取候选。  
2. 对每批调用 legal-hold 豁免过滤 → 删除/清理非豁免行 → 提交短事务。  
3. 重复直至无候选或达 tick 上限。  
4. 审计路径：若合计删除 > 0 → 写一条 purge-evidence。  

### 7.2 History TopN（PRR-A02）

1. 授权通过后，以 Pageable(0, N) 查询。  
2. 映射为既有 summary view（含 CE-U18 sampleResults）。  
3. 客户端仍见最多 N 条最新记录。  

### 7.3 Workflow inbox（PRR-A03）

1. 审批人请求 workflow tasks。  
2. 服务分页扫描 SUBMITTED → 过滤可见模块 → 每模块一条。  
3. 达到返回上限或无更多页则停止；返回任务列表。  

---

## 8. System responses

### 8.1 Success

| 路径 | 响应 |
| --- | --- |
| Retention | 超龄非豁免行被删/产物被清；豁免行保留；日志含 deleted/skipped/batches |
| History | HTTP 200；≤N 条；顺序 `createdAt DESC` |
| Workflow | HTTP 200；PENDING_REVIEW 为可见 SUBMITTED 投影（有界） |

### 8.2 Fail-closed / 边界

| 条件 | 响应 |
| --- | --- |
| Legal hold ACTIVE 命中 | 跳过删除/产物清理；不得硬删 |
| ShedLock 未获取 | 跳过 tick（既有） |
| `retention-enabled=false` | 审计清理跳过（既有） |
| 无 preview/batch-test 读权限 | 既有 403/404 |
| 无 CM catalog / decide 能力 | 既有 deny / 不投影 |
| 候选 ≫ 批大小 | 多批完成；单批加载 ≤ 配置批大小 |
| Inbox 触达返回上限 | 返回最新 ≤500；warn 日志；不 500 |

---

## 9. Acceptance scenarios（Given / When / Then）

### PRR-A01 — Audit + Invocation retention

#### BDD-PRR-A01-001 — Audit management 分批硬删（大候选集）

**Given** management 留存启用，cutoff 前存在 **≥ 批大小 × 3** 条非豁免超龄 `management_audit_event`  
**And** 清理批大小配置为默认窗口内值（如 1000）  
**When** `purgeManagementAudit`（或调度等价入口）执行完成  
**Then** 所有上述超龄非豁免行被硬删除  
**And** 任一时刻单次候选加载不超过配置批大小（由 Pageable/LIMIT 或测试探针可证）  
**And** 存在一条 `AUDIT_RETENTION_PURGE` evidence，`deletedCount` 等于合计删除数

#### BDD-PRR-A01-002 — Audit runtime 分批硬删

**Given** runtime 表存在 **≥ 批大小 × 3** 条非豁免超龄行  
**When** `purgeRuntimeAudit` 执行完成  
**Then** 超龄非豁免行全部删除  
**And** 有对应 runtime purge-evidence（若 deletedCount > 0）

#### BDD-PRR-A01-003 — Legal-hold 审计行不被删

**Given** 超龄 management/runtime 审计行命中 ACTIVE `TEMPLATE_WINDOW`（或 runtime `INVOCATION_SET` 关联）豁免  
**And** 另有同批非豁免超龄行  
**When** 清理执行  
**Then** 豁免行仍存在  
**And** 非豁免行被删除  
**And** purge-evidence 的 `deletedCount` **不含**豁免行

#### BDD-PRR-A01-004 — Invocation 记录分批硬删 + legal-hold

**Given** `recordExpiresAt < now` 的 invocation 行 **≥ 批大小 × 3**，其中部分命中 ACTIVE hold  
**When** `cleanExpiredRecords` 执行完成  
**Then** 非豁免过期行全部删除  
**And** 豁免行仍存在  
**And** 生产路径未使用无界 `findByRecordExpiresAtBefore()` 一次装载全部

#### BDD-PRR-A01-005 — Invocation 产物分批清理 + legal-hold

**Given** 多条过期产物记录（`documentExpiresAt < now` 且 storage key 非空），部分豁免  
**When** `cleanExpiredDocumentArtifacts` 执行完成  
**Then** 非豁免产物被清理并标记；豁免记录的产物保留  
**And** 候选以有界批次加载

#### BDD-PRR-A01-006 — 全豁免不写虚假成功 evidence

**Given** 所有超龄 audit 候选均被 legal-hold 豁免  
**When** audit purge 执行  
**Then** 无行删除  
**And** **不**写入声称成功删除的 purge-evidence

#### BDD-PRR-A01-007 — 批大小配置窗口

**Given** 批大小配置为 500 或 2000（窗口端点）  
**When** 清理在大候选集上运行  
**Then** 行为与 BDD-PRR-A01-001 等价正确（可删尽非豁免超龄行）  
**And** 单批加载不超过该配置值

---

### PRR-A02 — Preview / BatchTest history TopN

#### BDD-PRR-A02-001 — Preview 历史 TopN 在 DB

**Given** 模板 T 存在 **> 50** 条 preview 记录  
**When** 授权用户调用 `listPreviews(T)`  
**Then** 返回恰好 **50** 条（或 `min(50, total)`）  
**And** 为 `createdAt` 全局最新的 50 条  
**And** 查询路径使用 Pageable/`LIMIT`，而非全量列表再 `stream().limit`

#### BDD-PRR-A02-002 — Batch-test 历史 TopN 在 DB

**Given** 模板 T 存在 **> limit** 条 `hidden=false` 的 batch-test runs（例如 total=20，`limit=5`）  
**When** `listRecentRuns(T, 5, session)`  
**Then** 返回 5 条，按 `createdAt DESC` 为最新 5 条  
**And** 加载使用 Pageable/`LIMIT`（可复用 `findTopValid…` 风格或新增 hidden=false 的 Pageable 方法）  
**And** 摘要字段与 CE-U18 `sampleResults` 行为保持兼容

#### BDD-PRR-A02-003 — 历史不足 N 条

**Given** 模板仅 3 条 preview / batch-test 历史  
**When** 列表 API 以 N=50 / N=5 调用  
**Then** 返回 3 条；不报错

#### BDD-PRR-A02-004 — 授权 fail-closed 不变

**Given** 会话对模板 T 无可读快照权限  
**When** 调用 preview 或 batch-test 历史列表  
**Then** 既有 403/404 fail-closed；不泄露他组数据

---

### PRR-A03 — ContentModule SUBMITTED bounded query

#### BDD-PRR-A03-001 — SUBMITTED 有界分页扫描

**Given** 系统中存在 **≥ 批大小 × 2** 条 `reviewState=SUBMITTED` 版本（可跨多模块）  
**When** 具备 `decideContentModuleReviews` 的用户调用 `listWorkflowTasks`  
**Then** 服务以 Pageable/`LIMIT` 分批读取 SUBMITTED，**不**一次装载全部  
**And** 返回的 `PENDING_REVIEW` 任务每模块至多一条，且对应较新 `updatedAt` 版本

#### BDD-PRR-A03-002 — 翻页覆盖可见待审（非静默首页）

**Given** 第 1 页 SUBMITTED 候选全部属于用户**不可见**组  
**And** 后续页存在用户可见组的 SUBMITTED 模块  
**When** 该用户调用 `listWorkflowTasks`  
**Then** 返回中**包含**可见模块的 PENDING_REVIEW（在扫描/返回上限内）  
**And** 不得因只读 page 0 而静默得到空列表

#### BDD-PRR-A03-003 — 返回上限诚实截断

**Given** 用户可见的 distinct SUBMITTED 模块数 **> 500**（默认返回上限）  
**When** `listWorkflowTasks`  
**Then** 返回的 `PENDING_REVIEW` 条数 ≤ 500  
**And** 为按版本 `updatedAt DESC` 投影后的最新集合  
**And** 存在 warn 级日志指示 truncation / scan cap（可测）

#### BDD-PRR-A03-004 — 无审批能力不投影 PENDING_REVIEW

**Given** 用户可浏览 CM catalog 但 **无** `decideContentModuleReviews`  
**And** 存在 SUBMITTED 版本  
**When** `listWorkflowTasks`  
**Then** 结果中无 `PENDING_REVIEW`（REWORK 投影按既有 author 能力，本场景不强制）

#### BDD-PRR-A03-005 — 无目录浏览权限 fail-closed

**Given** 用户无 `canBrowseContentModuleCatalog`  
**When** `listWorkflowTasks`  
**Then** 既有 access denied（不 500；不泄露 SUBMITTED 存在性细节 beyond 既有行为）

---

## 10. 边界与异常

| 场景 | 期望 |
| --- | --- |
| 清理中途 DB 错误 | 失败批回滚；已提交批保留；无虚假成功 purge-evidence（D1-C17） |
| Hold 在 tick 中释放 | 后续批可删；不要求同 tick 重扫已跳过行 |
| Preview/batch 并发写入新行 | 列表为查询时刻最新 N；可接受短暂竞态 |
| Inbox 扫描期状态变更 | 最终一致性可接受；不 500 |
| 批大小配置非法 | 启动失败或钳位（PS-C1）；不得静默用无界查询 |

---

## 11. 可观察证据

| 证据 | 证明内容 |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | 质量门绿 |
| 单元/集成测试 | 覆盖 BDD-PRR-A01-001…007、A02-001…004、A03-001…005（硬门槛至少 A01-001/003/004、A02-001/002、A03-001/002） |
| Repo 方法签名 | 清理与 TopN / SUBMITTED 主路径可见 Pageable 或 LIMIT |
| 日志 | retention deleted/skipped/batches；inbox truncation warn |
| FE / E2E | **N/A**（`frontend_ui_in_scope=false`） |

---

## 12. 追溯性（Source-of-Truth）

| 文档 | 关系 |
| --- | --- |
| **本文件** | 本叶行为权威 |
| [lrp-d1-audit-retention.md](./lrp-d1-audit-retention.md) | 审计硬删、evidence、分批意图（D1-C16）；本叶落实有界加载 |
| [ce-g04-legal-hold.md](./ce-g04-legal-hold.md) | 删除前豁免；G04-C9…C16 |
| [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md) | Invocation 硬删语义（不改正文） |
| [ADR-0048](../adr/operations/0048-audit-data-retention-policy.md) | 审计留存政策（不改正文） |
| [ce-u18-batch-test-history.md](./ce-u18-batch-test-history.md) / [template-testing-overhaul.md](./template-testing-overhaul.md) | 历史条数与 sampleResults 语义 |
| [ce-u08-content-module-review-loop.md](./ce-u08-content-module-review-loop.md) | 待审投影完整性意图；本叶以有界翻页 + 返回上限落实 |
| Task Master `#98`/`#99`/`#100`（PRR-A01/A02/A03） | 合并叶范围与 Batch Recommendation |

---

## 13. 待确认问题（Pending）

**无阻塞项。** 下列为已选默认（不阻断 `ready`）：

| ID | 问题 | 默认 |
| --- | --- | --- |
| PS-Q1 | 精确默认批大小 | **1000**（窗口 500–2000） |
| PS-Q2 | Inbox 返回上限 | **500** PENDING_REVIEW；warn 截断 |
| PS-Q3 | REWORK 无界查询 | **本叶非硬门槛**；建议同模式 |
| PS-Q4 | 配置属性确切前缀 | 实现锁定（如 `docgen.audit.cleanup-batch-size` / `docgen.invocation.cleanup-batch-size`）；测试可读 `@Value` 默认 |

---

## 14. BDD Readiness

**`bdd_readiness: ready`**  
**`frontend_ui_in_scope: false`**

规格完整、可测、已与 LR-D1 / CE-G04 / CE-U08 / CE-U18 对齐。**交付已关闭（2026-07-17）：** Task Master **#98/#99/#100** → **Done**；MAIN merge `4197770f`；`mvn verify` **1925** GREEN；Stage 10 **DEPLOY_OK**；sole-active **cleared**；下一队列头 **#101** PRR-A04（solo，未激活）。

1. `plan-orchestrator` — **Done**  
2. `backend-engineer` — **Done**（成员 `#98+#99+#100`）  
3. FE / e2e / uiux — **N/A**（本叶）  
4. `post-task-doc-sync` — **Done**（MAIN）

**stage_done_definition:** BDD persisted ready; scenarios Given/When/Then; legal-hold safe; batch size 500–2000; no FE UI in scope；gates green；merged + doc-sync
