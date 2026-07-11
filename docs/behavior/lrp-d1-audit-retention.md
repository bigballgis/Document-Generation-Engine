# BDD 行为规格：LR-D1 — Audit data retention & archival

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-11  
**BDD ID**: `BDD-LRP-D1-AUDIT-RETENTION-001`  
**来源任务**: [LRP Wave LR-D § LR-D1 — Audit data retention & archival](../plan/detail/LRP-D-ops-observability.md)  
**程序**: [launch-readiness-program.md](../plan/launch-readiness-program.md) · Wave **LR-D**  
**Task Master / slice**: plan `LR-D1` / Task Master **#35** / slice `lrp-d1-audit-retention`  
**Worktree**: `D:/working/DGE-lrp-d1-audit-retention` · `feat/lrp-d1-audit-retention`  
**依赖**: LR-B2 ShedLock JDBC mutex (**Done**)  
**镜像模式**: `InvocationRetentionCleanupScheduler` + Flyway V43/V44 + [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md)  
**伴生 ADR**: [ADR-0048](../adr/operations/0048-audit-data-retention-policy.md)（**Accepted** 2026-07-11 — confirmed-for-D1）  
**Plan note**: Wave LR-D / LR-D1 计划行状态由 MAIN 上 `plan-orchestrator` 维护；本 worktree 内 `LRP-D-ops-observability.md` 可能仍显示 Not Started — **不以过期计划行覆盖本规格 / ADR-0048**。

---

## 1. 概述

平台管理审计表 `management_audit_event`（V9）与运行时生成审计表 `runtime_generation_audit_event`（V17）当前**无界增长**（CD-PIT-15）。调用记录表已有留存清理（ADR-0040 + `InvocationRetentionCleanupScheduler` + ShedLock），审计表尚未对齐。

本切片交付 **Tier-1 运营留存**：按确认窗口对超龄行执行 **硬删除**；每次成功清理写入 **purge-evidence** 管理审计行；调度器必须挂 **LR-B2 ShedLock**。**Tier-2 对象存储归档**（MinIO Parquet 等）**不在 D1 范围**。

| 行为域 | 摘要 |
| --- | --- |
| **D1 留存窗口** | management **90 天**；runtime **365 天**（confirmed-for-D1，见 §4） |
| **D2 处置方式** | **硬删除**（匹配 ADR-0040 `cleanExpiredRecords`）；无 soft-delete；无 v1 归档 |
| **D3 调度器** | `AuditRetentionCleanupScheduler`（命名以实现为准）；`@Scheduled` + `@SchedulerLock` |
| **D4 清理证据** | 每次成功 purge 写入 `management_audit_event`（系统主体）；含表名、窗口、删除计数、cutoff |
| **D5 可见性** | purge-evidence 仅 `AUDIT_ADMIN` / `GLOBAL_ADMIN` 可查；`GROUP_ADMIN` 不可见全局 purge 行 |
| **D6 边界** | 窗口内行保留；恰在 cutoff 边界的行保留；锁未获取则跳过本 tick |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| Tier-2 MinIO/Parquet 归档与无限/多年合规仓 | **Out of scope** — ADR-0048 Tier 2 Deferred；权限矩阵「5 年」已改写为 Tier-2 意图（见 §4.1） |
| 修改 `api_invocation_record` 留存（ADR-0040） | **禁止** — 本切片不改调用记录清理语义 |
| 人工「立即 purge」管理 UI/API | **Out of scope** — 仅调度器自动清理 |
| 改变审计查询/导出 API 契约（除新 event_type 可见） | 不扩展权限面；沿用矩阵 §10 |
| 触碰 `DGE-audit-governance` 工作树 | **禁止** |
| Soft-delete / `deleted_at` | **禁止** — 与 ADR-0040 / ADR-0048 一致采用硬删 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **系统调度器** | `AuditRetentionCleanupScheduler`（系统主体） | 按 cron/interval 触发清理；无交互用户 |
| **审计管理员** | `AUDIT_ADMIN` | 可在 Activity log 查看 purge-evidence 与剩余审计行 |
| **全局管理员** | `GLOBAL_ADMIN` | 同 AUDIT_ADMIN 的审计查看范围（最大权限） |
| **分组管理员** | `GROUP_ADMIN` | 仅组范围审计；**不能**查看无 `group_code` 的平台级 purge-evidence |
| **其他管理角色** | `MASTER_DESIGNER` 等 | **不**因本切片获得审计查看权（fail-closed） |
| **运维 / 部署** | 配置 `docgen.audit.*` | 可调窗口与开关；不得绕过 ShedLock |

---

## 3. Goal

1. 超龄 management / runtime 审计行按确认窗口被硬删除，表增长受控。  
2. 窗口内（含边界）行保持可查询。  
3. 每次成功清理留下可审计的 purge-evidence（计数 + 窗口 + cutoff + 目标表）。  
4. 多实例下同一清理逻辑至多一个副本执行（ShedLock）。  
5. 未授权角色看不到 purge-evidence；清理失败不得静默冒充成功。

---

## 4. 已确认决策（confirmed-for-D1）

**授权依据：** 用户 2026-07-11「按你建议继续」启动 Wave LR-D / LR-D1；建议基线对齐 [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md) 与 [ADR-0048](../adr/operations/0048-audit-data-retention-policy.md)（**Accepted**）。下列数值为本切片 **confirmed-for-D1** 交付基线（非「待确认提案」）。

| ID | 决策 |
| --- | --- |
| **D1-C1** | **目标表**：仅 `management_audit_event`（V9）与 `runtime_generation_audit_event`（V17）。 |
| **D1-C2** | **时钟字段**：两表均以 `event_at`（UTC `TIMESTAMPTZ`）判定年龄。 |
| **D1-C3** | **management 留存窗口**：**90 天**。理由：镜像 ADR-0040 调用记录默认 `invocationRecordRetentionDays=90`；覆盖近期生命周期排障，避免管理审计无限增长。 |
| **D1-C4** | **runtime 留存窗口**：**365 天**。理由：运行时生成审计用于用量趋势、客服与争议回溯，需长于调用记录默认 90 天；与 ADR-0040 文档 artifact 上限量级（`documentRetentionDays` max 365）对齐为运营上界，仍为有界留存（**禁止**无限留存）。 |
| **D1-C5** | **处置方式**：**硬删除**（`DELETE`）。匹配 ADR-0040 `InvocationRetentionCleanupScheduler.cleanExpiredRecords` → `repository.deleteAll(...)`。v1 **不做**归档导出。 |
| **D1-C6** | **Cutoff 语义**：`cutoff = nowUtc - retentionDays`。删除谓词：`event_at < cutoff`。`event_at == cutoff` 或更新 → **保留**（边界保留）。 |
| **D1-C7** | **可配置**：`docgen.audit.management-retention-days`（默认 **90**）、`docgen.audit.runtime-retention-days`（默认 **365**）、`docgen.audit.retention-enabled`（默认 **true** — 基线已确认）、`docgen.audit.retention-cron`（默认每日 **03:00** 服务本地/配置时区，实现冻结 ISO cron）。上限建议：management ≤ **2555**（7y）、runtime ≤ **2555**；低于下限（如 `<1`）启动失败或拒绝配置（实现锁定一种 fail-closed）。 |
| **D1-C8** | **调度器**：新建 `AuditRetentionCleanupScheduler`（或等价包路径）；模式镜像 `InvocationRetentionCleanupScheduler`。推荐两个 `@SchedulerLock` 名：`audit-retention-cleanup-management`、`audit-retention-cleanup-runtime`（或单一锁串行两表 — 实现锁定；**必须**使用 ShedLock JDBC，与 LR-B2 一致）。`lockAtMostFor` / `lockAtLeastFor` 量级对齐调用清理（如 `PT10M` / `PT20S`），可按实测微调。 |
| **D1-C9** | **锁未获取**：本 tick **跳过**；不写 purge-evidence；不报错为用户可见故障；日志可 debug/info。 |
| **D1-C10** | **`retention-enabled=false`**：调度方法直接 return；无删除、无 evidence。 |
| **D1-C11** | **Purge-evidence**：每次**至少删除 1 行**的成功 purge 后，向 `management_audit_event` **INSERT** 一行：<br>• `event_type` = `AUDIT_RETENTION_PURGE`（常量名以实现/OpenAPI 枚举为准，测试锁定）<br>• `actor_username` = 系统主体（如 `SYSTEM` / `scheduler`，≤8 字符约束时用 `SYSTEM`）<br>• `actor_summary` / `status_summary` 含：目标表逻辑名、`retentionDays`、`cutoff`（ISO-8601）、`deletedCount`<br>• `group_code` / `template_id` = **null**（平台级）<br>• `event_at` = purge 完成时刻<br>• **禁止**在 evidence 中写入被删行的业务明文、变量、凭证 secret |
| **D1-C12** | **同次运行自保护**：写入的 purge-evidence 行 **不得** 在同一事务/同一 tick 中被删除。 |
| **D1-C13** | **零删除**：若候选集为空，**不**写 evidence（避免噪声）；可打 debug 日志。 |
| **D1-C14** | **谁可见 purge-evidence**：`AUDIT_ADMIN`、`GLOBAL_ADMIN` 经既有管理审计查询可见 `AUDIT_RETENTION_PURGE`。`GROUP_ADMIN` **不可见**（无 `group_code` + 平台级事件；既有组范围规则 fail-closed）。其他角色无 `readAudit` → 不可见。 |
| **D1-C15** | **Evidence 自身亦受 90 天 management 留存约束**（最终会被后续 purge 删除）；这是有意设计，与「每次清理留痕、痕迹本身有界」一致。 |
| **D1-C16** | **批大小**：允许分批删除（如每批 N 行）直至本 tick 无超龄行或达安全上限；每批或每 tick 结束写 **一条** evidence（实现锁定：推荐 **每表每 tick 一条**，`deletedCount` 为合计）。 |
| **D1-C17** | **失败语义**：删除中途失败 → 事务回滚该批（或已提交批保留）；**不得**写「声称成功」的 evidence；记 warn/error 日志。部分成功时 evidence 只反映已提交删除数。 |
| **D1-C18** | **与调用记录清理隔离**：不修改 `InvocationRetentionCleanupScheduler`；两套调度独立锁名。 |
| **D1-C19** | **索引**：若缺 `event_at` 过滤索引则补齐（V9/V17 已有 `event_at DESC` 索引；实现确认足够即可，无需为 retention 强行加列）。**不**采用 ADR-0048 草案中「给审计表加 `retention_days` 列」——窗口为**平台配置**，非逐行字段（与 V43 包级字段不同；审计为全局策略）。 |
| **D1-C20** | **Flyway**：下一可用版本号增加配置文档/注释即可；**无需**为默认天数加表列。若需 seed 配置表则另议 — 默认 **仅 application 配置属性**。 |
| **D1-C21** | **可观测性**：日志记录每表 `deletedCount`；可选 Micrometer counter `audit.retention.deleted`（非本切片 Done 硬门槛；D3 可后续接入）。 |
| **D1-C22** | **门禁**：`mvn -B -ntp -f backend/pom.xml verify`。无前端 E2E 强制（无用户触发 UI；Activity log 可见新 event_type 为可选冒烟）。 |

### 4.1 与权限矩阵「默认保留 5 年」的关系（已对齐）

[permission-matrix.md](../security/permission-matrix.md) §10 已改写为 Tier-1 / Tier-2 分层（doc-keeper 2026-07-11）：

| 层级 | D1 状态 | 含义 |
| --- | --- | --- |
| **Tier-1 热库** | **Confirmed** 90 / 365 天硬删 | 运营查询面；本切片交付 |
| **Tier-2 归档** | **Deferred**（ADR-0048） | 未来承载多年/监管留存（含历史「5 年」意图与 Basel/SOX 叙述） |

PRD / requirements-plan / domain-model 中同主题措辞已同步为同一分层，避免与矩阵静默冲突。ADR-0030「180 天 + 3 年」Accepted 决策正文**未改写**；对两张审计表 Tier-1 以 ADR-0048 为准。

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 调度 cron / fixedDelay 到期 | `retention-enabled=true` 且获得 ShedLock |
| T2 | 集成测试直接调用清理方法 | 注入时钟/`event_at` 造数 |
| T3 | 配置变更 | 仅影响**之后**的 cutoff 计算；不回写历史行 |

---

## 6. Preconditions

1. LR-B2 ShedLock JDBC 已启用（`V46__shedlock.sql` + `SchedulerLockConfig`）。  
2. `management_audit_event` / `runtime_generation_audit_event` 表存在且可写。  
3. `docgen.audit.retention-enabled=true`（交付默认）。  
4. 系统时钟 UTC 一致（测试可冻结 `Clock`/`Instant`）。  
5. 不依赖 `DGE-audit-governance` 任何产物。

---

## 7. Primary Journey（主路径）

| # | Actor | 动作 | 系统响应 |
| --- | --- | --- | --- |
| 1 | 调度器 | Tick 到达；尝试获取 management 锁 | 获锁 → 计算 management cutoff |
| 2 | 调度器 | `DELETE` 所有 `event_at < cutoff` 的 management 行（可分批） | 超龄行消失；窗口内保留 |
| 3 | 调度器 | 若 `deletedCount > 0` | INSERT `AUDIT_RETENTION_PURGE` evidence（management） |
| 4 | 调度器 | 释放锁；尝试 runtime 锁（若分锁） | 同理清理 runtime 表并写 evidence |
| 5 | AUDIT_ADMIN | 打开 Activity log / 管理审计 API，筛 `AUDIT_RETENTION_PURGE` | 可见含 count/window 的摘要行 |
| 6 | GROUP_ADMIN | 同筛 | **不可见**平台级 purge 行（或空结果；非 500） |

---

## 8. System Responses

### 成功

- 超龄行硬删除；边界与更新行保留。  
- `deletedCount > 0` 时存在对应 purge-evidence。  
- 日志含表名与计数。  
- 多实例下同一 lock name 同时仅一副本执行删除。

### Fail-closed / 降级

| 条件 | 响应 |
| --- | --- |
| 未获 ShedLock | 跳过；无删；无 evidence |
| `retention-enabled=false` | 跳过 |
| DB 错误 | 回滚失败批；无成功虚假 evidence；error 日志 |
| 无 `readAudit` | 查询/导出 403；不泄露是否存在 purge 行 |

---

## 9. Acceptance Scenarios（Given / When / Then）

### BDD-LRP-D1-001 — Management 超龄硬删除

**Given** `retention-enabled=true`，management 窗口 **90** 天  
**And** 存在 `event_at < now-90d` 的 management 行 A 与 `event_at >= now-90d` 的行 B  
**When** management 清理 tick 获锁执行  
**Then** A 被硬删除，B 仍存在  
**And** 存在 `AUDIT_RETENTION_PURGE` evidence，`deletedCount >= 1`，摘要含 management 表与 cutoff

### BDD-LRP-D1-002 — Runtime 超龄硬删除

**Given** runtime 窗口 **365** 天  
**And** 存在 `event_at < now-365d` 的 runtime 行 C 与窗口内行 D  
**When** runtime 清理 tick 获锁执行  
**Then** C 删除，D 保留  
**And** 存在对应 purge-evidence（目标表 = runtime）

### BDD-LRP-D1-003 — 窗口边界保留

**Given** 一行 `event_at` **恰好等于** cutoff（`now - retentionDays`）  
**When** 清理执行  
**Then** 该行 **保留**（`event_at < cutoff` 才删）

### BDD-LRP-D1-004 — 窗口内行不动

**Given** 仅存在窗口内审计行  
**When** 清理执行  
**Then** 行数不变  
**And** **不**写入 purge-evidence

### BDD-LRP-D1-005 — ShedLock 跳过

**Given** 另一实例已持有同名 `@SchedulerLock`  
**When** 本实例 tick 触发  
**Then** 不删除任何行  
**And** 不写 evidence

### BDD-LRP-D1-006 — 开关关闭

**Given** `docgen.audit.retention-enabled=false`  
**And** 存在超龄行  
**When** tick 触发  
**Then** 超龄行仍在  
**And** 无 evidence

### BDD-LRP-D1-007 — Purge-evidence 同 tick 自保护

**Given** 将产生 `deletedCount > 0` 的清理  
**When** 同一次 tick 完成  
**Then** 新建的 `AUDIT_RETENTION_PURGE` 行仍存在（未被同 tick 删除）

### BDD-LRP-D1-008 — Purge-evidence 授权可见性

**Given** 已存在 `AUDIT_RETENTION_PURGE` 行（`group_code` null）  
**When** `AUDIT_ADMIN` 或 `GLOBAL_ADMIN` 查询管理审计  
**Then** 可见该行摘要（无敏感明文）  
**When** `GROUP_ADMIN`（仅组范围）查询  
**Then** **不可见**该平台级 purge 行  
**When** 无 `readAudit` 角色查询  
**Then** **403** fail-closed

### BDD-LRP-D1-009 — 配置覆盖窗口

**Given** `management-retention-days=30`  
**And** 一行年龄 45 天、一行年龄 10 天  
**When** management 清理执行  
**Then** 45 天行删除；10 天行保留  
**And** evidence 摘要反映 `retentionDays=30`

### BDD-LRP-D1-010 — 与 invocation 清理隔离

**Given** 存在未过期与已过期的 `api_invocation_record`  
**When** **仅** audit retention tick 运行  
**Then** `api_invocation_record` 行集 **不变**（由既有 invocation scheduler 负责）

---

## 10. 边界与异常行为

| 场景 | 期望 |
| --- | --- |
| 超大超龄集 | 分批删除；避免单事务锁表过久；evidence 记合计 |
| 时钟回拨 | 以服务 `Instant.now()`（或注入 Clock）为准；不回填历史 |
| Evidence 超过 90 天后 | 可被后续 management purge 删除（D1-C15） |
| 并发业务 INSERT | 新插入 `event_at≈now` 的行不受本 tick 删除 |
| 监控计数 | 日志必有；Micrometer 可选 |

---

## 11. 可观察证据

| 证据 | 证明内容 |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | 质量门绿 |
| 单元/集成测试 | 覆盖 BDD-LRP-D1-001…010（至少 001–008 为硬门槛） |
| `@SchedulerLock` 注解测试 | 锁名存在（镜像 `SchedulerLockAnnotationTest` 模式） |
| 管理审计查询 | `AUDIT_RETENTION_PURGE` 对 AUDIT_ADMIN 可见 |
| 应用配置 | `docgen.audit.*` 默认值与文档一致 |
| ADR-0048 | **Accepted**（2026-07-11）；confirmed-for-D1 窗口已写入 |

---

## 12. 追溯性（Source-of-Truth）

| 文档 | 关系 |
| --- | --- |
| 本文件 | **行为权威**（D1 confirmed baselines） |
| [ADR-0048](../adr/operations/0048-audit-data-retention-policy.md) | 政策 ADR；**Accepted**（confirmed-for-D1） |
| [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md) | 硬删 + 90 天默认镜像来源 |
| [permission-matrix.md](../security/permission-matrix.md) §10 | 审计查看/导出；Tier-1 90/365 Confirmed；「5 年」= Tier-2 Deferred |
| [LRP-D-ops-observability.md](../plan/detail/LRP-D-ops-observability.md) § LR-D1 | 计划任务与验收提纲（状态以 MAIN plan-orchestrator 为准） |
| V9 / V17 migrations | 表结构与 `event_at` |
| `InvocationRetentionCleanupScheduler` | 实现模式 |
| Task Master **#35** | 活动切片 |

---

## 13. 待确认问题（Pending）

**无阻塞项。** 下列为实现冻结项（不阻断 `ready`）：

| ID | 问题 | 默认（若未另行确认） |
| --- | --- | --- |
| D1-Q1 | 单锁串行两表 vs 双锁并行 | **双锁**（management / runtime 独立名），镜像 invocation 双方法 |
| D1-Q2 | cron 默认时区 | 与现有 `@Scheduled` / Spring 时区配置一致；测试用固定 Instant |
| D1-Q3 | `actor_username` 精确字符串 | **`SYSTEM`**（适配 VARCHAR(8)） |
| D1-Q4 | 分批大小 N | 实现选 500 或 1000；测试不依赖具体 N |

---

## 14. BDD Readiness

**`bdd_readiness: ready`**

规格完整、confirmed-for-D1 窗口与硬删/证据/锁/可见性已落盘；ADR-0048 **Accepted**；权限矩阵 Tier-1/Tier-2 已对齐。可 hand off：

1. ~~`doc-keeper` — 晋升 ADR-0048 Accepted + 对齐 permission-matrix~~ **Done（本 worktree）**
2. `backend-engineer` — TDD Red → 调度器 + 配置 + 测试（本 worktree）

**stage_done_definition:** Docs ready for backend-engineer TDD
