# BDD 行为规格：CORE-FORTRESS Phase F5 — 异步耐久性 + 安全深度

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-09  
**BDD ID**: `BDD-CORE-FORTRESS-F5-001`  
**来源**: CORE-FORTRESS 纲领 F5 范围 + 代码库调查（`AsyncBatchTaskRunner`、`BatchGenerationService`、`KafkaAsyncBatchConfig`、`RuntimeGenerationAuditRecorder`、`InvocationParameterSanitizer`）+ ADR-0020 / NFR 已确认基线

---

## 1. 概述

F5 将 **异步批量生成路径** 从「能跑」加固到「崩溃可恢复、Kafka at-least-once 可证明、审计不含敏感明文、运行时拒绝可追踪」——**单后端 slice**，不覆盖全平台安全审计或分布式限流（留 F8 / ADR-0044 scale-out）。

| 工作流 | 改造要点 |
| --- | --- |
| **F5-B1 陈旧任务检测与崩溃恢复** | 修正 `PROCESSING` 终态误判；基于 `updated_at` 租约的 stale 检测；ShedLock 调度器回收并重派 |
| **F5-B2 Kafka 消费者幂等与 DLT 硬化** | 消费端与 DB 状态对齐；重试耗尽后进 DLT 的可测证据；进程内与 Kafka 路径行为一致 |
| **F5-B3 生成审计深度（无敏感明文）** | 调用记录与终态任务载荷变量哈希化；错误摘要稳定 messageKey；运行时审计不含变量明文 |
| **F5-B4 运行时安全深度（429 + 凭证审计关联）** | 429 写入运行时审计摘要；凭证轮换审计增加轮换代次关联字段 |

**与已完成工作的关系（F5 不重做）**

| 已有资产 | 状态 | F5 关系 |
| --- | --- | --- |
| F2 幂等 release 哈希缓存 | **Done** | **Out of scope** — 仅回归引用 |
| P10 下载 fail-closed + 15min 窗口 | **Done** | **Out of scope** — 回归断言 batch 异步产物下载仍走同一授权链 |
| LR-B5 优雅停机 + Kafka shutdown timeout | **Done** | **交叉引用** — F5-B1 回收调度与之互补 |
| LR-B7 幂等 digest hard-fail + 限流 filter 决策 | **Done** | **Out of scope** — F5-B4 仅补 429 审计，不改 Bucket4j 拓扑 |
| ADR-0044 单副本 in-process 限流 | **Accepted** | **Out of scope** — 分布式 Redis 限流留 scale-out |
| 管理面凭证轮换 `ManagementAuditRecorder` | **Done** | F5-B4 补深度字段，不重做轮换 API |

---

## 2. Actor / Role

| Actor | 说明 | 权限 / 关注点 |
| --- | --- | --- |
| **运行时 API 调用方** | 提交异步批量、轮询任务、下载产物 | API 凭证 + AD Group；期望崩溃后任务终态可预期 |
| **平台运维 / SRE** | 配置 Kafka transport、stale 阈值、DLT 监控 | `docgen.async.*`；DLT topic 深度告警（F8 指标列表引用 F5 证据） |
| **安全 / 审计查阅者** | 查询运行时与管理审计 | 不得看到变量明文、加密密码、完整请求体 |
| **系统（异步 worker）** | `@Async` 或 Kafka consumer 执行 `processTask` | at-least-once；幂等跳过已终态任务 |

---

## 3. Goal

1. **B1**：进程崩溃或 worker 失联后，卡在 `PROCESSING` 的任务在可配置 stale 阈值后被回收并 **至多一次** 成功完成或 **明确失败**（含审计）。
2. **B2**：Kafka 路径下重复投递、重试与 DLT 行为 **可测试、可文档化**；与 in-process 路径语义一致。
3. **B3**：持久化的调用记录与 **终态** 异步任务载荷符合 ADR-0020（变量哈希、无加密密码明文）；审计/error summary 不含客户数据。
4. **B4**：超配额 429 产生可追溯审计事件；凭证轮换审计含轮换代次，便于与运行时调用关联。

---

## 4. 已确认决策（2026-07-09）

| ID | 决策 |
| --- | --- |
| **F5-C1** | **In-flight 例外**：任务处于 `ACCEPTED` 或 `PROCESSING` 且未过期时，`generation_async_task.request_payload_json` **可保留** 完整请求体以供执行与崩溃重试；保留期 **≤ `expires_at`**（与 P11 任务 TTL 一致） |
| **F5-C2** | 任务进入 **终态**（`SUCCEEDED` / `FAILED` / `PARTIAL_SUCCEEDED` / `CANCELLED` / `EXPIRED`）时，**必须 scrub** `request_payload_json`：变量替换为 `variablesHash`（批量项为 per-item hash + `itemsCount`），加密字段仅保留 `EncryptionSummaryView` 形态 |
| **F5-C3** | `InvocationParameterSanitizer` 持久化到 `api_invocation_record.parameters_json` 时 **不得含变量明文**；改为 `variablesHash` / 批量 `itemsHash`（SHA-256 hex，与预览 `hashVariables` 算法一致或共用 utility） |
| **F5-C4** | `AsyncBatchTaskRunner.summarizeFailure` 及审计 `error_summary` 使用 **稳定 messageKey 或异常类型名**，禁止写入未净化的 `Exception.getMessage()`（可能夹带变量片段） |
| **F5-C5** | Stale 阈值默认 **900 s（15 min）**，配置项 `docgen.async.stale-processing-threshold-seconds`；回收策略：**重置为 `ACCEPTED` 并重派**（in-process `@Async` 或 Kafka publish），累计 reclaim 次数上限 **3**，超限标记 `FAILED` + 安全 error summary |
| **F5-C6** | `isTerminalStatus` **不得** 包含 `PROCESSING`；`PROCESSING` + `updated_at` 在阈值内视为 **活跃租约**，consumer 跳过；超阈值视为 stale，允许 reclaim |
| **F5-C7** | Stale 回收调度器使用 **ShedLock**（与 LR-B2 一致），默认间隔 **300 s**，配置项 `docgen.async.stale-reclaim-interval-ms` |
| **F5-C8** | Kafka DLT：`DefaultErrorHandler` 保持 `FixedBackOff(1000ms, 3)`；新增集成测试证明第 4 次失败进入 `generation.async-batch-task.v1.dlt` |
| **F5-C9** | 429 审计事件类型 **`API_RATE_LIMIT_DENIED`**；字段：credential fingerprint、access account 摘要、environment、traceId、auditId；**无** 请求体 |
| **F5-C10** | 凭证轮换审计增加 **`rotationGeneration`**（单调递增整数，per credential）与 **`previousCredentialFingerprint`**；secret 仍仅创建/轮换响应一次性展示 |
| **F5-C11** | F5 **不含** 前端、E2E/UIUX、Redis 分布式限流、全量 penetration audit、Kafka prod compose 变更（LR-B4 已记录 branch (b) in-process v1） |
| **F5-C12** | 新增 reclaim 计数列 `processing_attempt_count`（Flyway）；每次 `markProcessing` 递增 |

---

## 5. 前置条件

- CORE-FORTRESS F1–F4 **Done**。
- P11 异步批量 API、任务查询/取消 **Done**。
- P10 下载授权链 **Done**（F5 仅回归）。
- ShedLock JDBC 已启用（LR-B2）。
- BDD readiness：**ready** — 无阻塞性开放问题；Flyway 列名与配置键在实现阶段冻结。

---

## 6. 主流程（Primary Journey）

### 6.1 异步提交 → 正常完成

1. 调用方 `POST .../batch-generate`（async mode）→ 任务 `ACCEPTED`，dispatch 至 worker。
2. Worker `markProcessing()` → 执行 batch → `SUCCEEDED` + scrub payload → 审计 COMPLETED + invocation 终态。
3. 调用方 `GET .../tasks/{taskId}` → 200 + batch result。

### 6.2 崩溃恢复

1. Worker 在 `PROCESSING` 中崩溃（`updated_at` 停止更新）。
2. Stale 调度器或 Kafka 重投检测到 `updated_at + threshold < now()`。
3. 若 `processing_attempt_count < 3`：重置 `ACCEPTED`，重派；否则 `FAILED` + 安全 summary。
4. 调用方轮询得到终态；幂等键 replay 行为不变（F2）。

### 6.3 Kafka DLT

1. `processTask` 抛出未捕获异常（非业务校验）。
2. Consumer 重试 3 次（1 s 间隔）→ 第 4 次失败 → 消息发布至 DLT topic。
3. 任务 DB 状态由 runner 标记 `FAILED`（若尚未终态）并 scrub。

---

## 7. 验收场景（Given / When / Then）

### BDD-F5-B1-001 — PROCESSING 非终态，活跃租约内跳过

**Given** 任务 `PROCESSING`，`updated_at` 在 stale 阈值内  
**When** Kafka consumer 或 runner 再次收到同一 `taskId`  
**Then** 不重新执行 batch  
**And** 状态仍为 `PROCESSING`

### BDD-F5-B1-002 — Stale PROCESSING 回收并重派

**Given** 任务 `PROCESSING`，`updated_at` 早于 `now - threshold`，`processing_attempt_count < 3`  
**When** stale reclaim 调度器 tick  
**Then** 状态变为 `ACCEPTED`  
**And** dispatcher 再次 dispatch  
**And** 产生审计摘要含 `STALE_TASK_RECLAIMED`（或等价 stable code）

### BDD-F5-B1-003 —  reclaim 次数超限失败

**Given** 任务 `processing_attempt_count >= 3` 且仍 stale  
**When** reclaim 调度器 tick  
**Then** 状态 `FAILED`  
**And** `error_summary` 为稳定 messageKey（非异常明文）  
**And** `request_payload_json` 已 scrub

### BDD-F5-B2-001 — Kafka 重试后进 DLT

**Given** `docgen.async.transport=kafka`，EmbeddedKafka 含 main + DLT topic  
**When** mock runner 连续 4 次抛出 `RuntimeException`  
**Then** 第 4 次后 DLT topic 收到 1 条消息  
**And** 主 topic consumer 不再无限重试

### BDD-F5-B2-002 — 终态任务 Kafka 重复投递幂等

**Given** 任务已 `SUCCEEDED`  
**When** consumer 再次消费同一 message  
**Then** `processTask` 不调用 `batchExecutionService.execute`  
**And** 状态保持 `SUCCEEDED`

### BDD-F5-B3-001 — 终态任务载荷 scrub

**Given** 异步 batch 含变量 `{ "accountNo": "1234567890" }`  
**When** 任务 `SUCCEEDED`  
**Then** `request_payload_json` 不含 `1234567890`  
**And** 含 `variablesHash` 或 per-item hash 字段

### BDD-F5-B3-002 — 调用记录无变量明文

**Given** 同步或异步 batch 完成  
**When** 查询 `api_invocation_record.parameters_json`  
**Then** JSON 不含变量原始值  
**And** 含 `variablesHash` / `itemsHash`

### BDD-F5-B3-003 — 失败审计 error_summary 安全

**Given** batch 执行抛出含敏感片段的消息  
**When** 记录 `API_GENERATION_BATCH_ASYNC_COMPLETED`  
**Then** `error_summary` 仅为 messageKey 或异常类名  
**And** 长度 ≤ 512

### BDD-F5-B4-001 — 429 产生运行时审计

**Given** 某 credential 耗尽 rate limit bucket  
**When** 下一次 runtime API 请求  
**Then** HTTP 429 + `RATE_LIMIT_EXCEEDED`  
**And** `runtime_generation_audit_event` 含 `API_RATE_LIMIT_DENIED`  
**And** 含 credential fingerprint，无请求体

### BDD-F5-B4-002 — 凭证轮换审计深度

**Given** ACTIVE credential 已轮换一次  
**When** 管理面再次 rotate  
**Then** 审计事件 `rotationGeneration == 2`  
**And** `previousCredentialFingerprint` 指向上一次 external id fingerprint

### BDD-F5-REG-001 — P10 下载链回归（batch 异步产物）

**Given** 异步 batch 成功且 item 含 `documentId`  
**When** 调用方在窗口内 `GET .../documents/{documentId}/download`  
**Then** 200 流式响应  
**And** 模板 credential 不匹配时 403（fail-closed）

---

## 8. 边界与异常行为

| 场景 | 期望行为 |
| --- | --- |
| 任务 `CANCELLED` / `EXPIRED` | reclaim **不得** 重派；保持终态 |
| Stale 调度与 consumer 并发 reclaim | DB 乐观更新（`updated_at` 或 version）**至多一个** winner 重派 |
| `processTask` 业务校验失败（`TemplateValidationException`） | 标记 `FAILED`，不重试 Kafka（若 classified non-retryable — 实现时区分） |
| 幂等 replay（相同 idempotencyKey + hash） | F2 行为不变；返回已有 task summary |
| ShedLock 未拿到锁 | 跳过 tick；下一 interval 再试 |
| 审计依赖不可用 | fail-closed：**不得** 因审计失败而跳过 scrub；scrub 与终态同事务 |

---

## 9. 可观察证据

| 证据 | 证明内容 |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | 全量质量门 |
| 新增/扩展测试类 | `AsyncBatchTaskRunnerTest`, `AsyncBatchTaskStaleReclaimSchedulerTest`, `AsyncBatchTaskKafkaDltIntegrationTest`, `InvocationParameterSanitizerTest`, `RuntimeRateLimitFilterTest`, `ApiManagementServiceTest` |
| Flyway migration | `processing_attempt_count`；可选 `rotation_generation` on credential |
| 配置键 | `docgen.async.stale-processing-threshold-seconds`, `docgen.async.stale-reclaim-interval-ms` |
| 审计表行 | `API_RATE_LIMIT_DENIED`, scrubbed payload, `rotationGeneration` |

---

## 10. 追溯性（Source-of-Truth）

| 文档 | 关系 |
| --- | --- |
| [requirements-plan.md](../requirements/requirements-plan.md) | 异步任务状态枚举；幂等规则 |
| [non-functional-requirements.md](../requirements/non-functional-requirements.md) | 无敏感明文持久化；fail-closed |
| [ADR-0020](../adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md) | 变量/请求体分类基线 |
| [ADR-0031](../adr/api/0031-api-platform-hardening-baseline.md) | 429 / rate limit 基线 |
| [ADR-0044](../adr/operations/0044-deployment-topology-v1.md) | in-process 限流 v1 范围 |
| [P11 batch-async](../plan/detail/P11-batch-async.md) | 异步 API 行为 |
| [P10 runtime-download](../plan/detail/P10-runtime-download.md) | 下载授权 |
| [CORE-FORTRESS program roadmap](../plan/detail/CORE-FORTRESS-program-roadmap.md) | F5 程序位 |
| [LRP-B runtime scaleout](../plan/detail/LRP-B-runtime-scaleout-session.md) | LR-B5/B7 交叉引用 |

---

## 11. 待确认问题（Pending）

**无阻塞项。** 以下为实现细节，由 backend-engineer 在 TDD 中冻结：

| ID | 问题 | 默认（若未另行确认） |
| --- | --- | --- |
| F5-Q1 | 非 retryable 业务异常是否进 DLT | **否** — 标记 FAILED，consumer ack |
| F5-Q2 | `variablesHash` 算法 | 复用 `PreviewGenerationService.hashVariables` 或提取 `VariableHashSupport` |

---

## 12. BDD Readiness

**`ready`** — 规格完整，可 hand off 至 `plan-orchestrator` / `backend-engineer` TDD。
