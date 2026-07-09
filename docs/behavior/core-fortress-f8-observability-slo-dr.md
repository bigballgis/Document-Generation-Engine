# BDD 行为规格：CORE-FORTRESS Phase F8 — Observability, SLO, DR, Evidence Bundle

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-09  
**BDD ID**: `BDD-CORE-FORTRESS-F8-001`  
**来源**: CORE-FORTRESS 纲领 F8 + ADR-0030 + LR-D 子集（LR-D2/LR-D3 可交付切片）+ 用户确认（2026-07-09）

---

## 1. 概述

F8 是 CORE-FORTRESS **收官阶段**，在 F1–F7 核心加固完成后，交付 **可运维、可度量、可演练、可审计** 的生产证据闭环：

| 工作流 | 要点 |
| --- | --- |
| **F8-B1 SLO 指标（生成延迟 / 转换成功率）** | Micrometer timer + counter；Prometheus 可 scrape；告警规则与 NFR 提案对齐（draft 直至用户确认） |
| **F8-B2 Health / Readiness 深度** | `/readyz` 结构化组件检查；**流量门控仍仅 Postgres**（SOR-O06 不变） |
| **F8-B3 DR Runbook 章节** | 在 `docs/operations/runbook.md` 扩展 DR/备份/恢复演练规程；交叉引用 ADR-0030 RPO/RTO |
| **F8-B4 Release Evidence Bundle** | 扩展 `scripts/release-gate.ps1` 或伴生脚本 + checklist；归档 gate/health/metrics/git 证据 |

**与 LR-D / 已有资产的关系**

| 来源 | F8 覆盖 | F8 不覆盖 |
| --- | --- | --- |
| **LR-D2** Backup/restore runbook | F8-B3 **子集**：DR 章节 + 演练 checklist（**不要求** F8 内完成首次 drill 执行） | 完整 `backup-restore-runbook.md` 独立文件；年度 drill 证据 |
| **LR-D3** Metrics & alerting as code | F8-B1 实现 **生成/转换 SLO 核心 series** + 更新 `deploy/observability/` | 全量 SSE/DLT/429 面板；Prometheus 栈部署 |
| **LR-D5** NFR 数值 | 告警阈值 **引用 pending proposal**（同步 p95 ≤ 3s 等） | 将提案提升为 SLA 承诺 |
| **P15-T07** `/healthz` + `/readyz` | F8-B2 **加深响应体** | 变更 liveness 语义 |
| **SOR-A06** OTLP tracing | 交叉引用 traceId 关联 | 新增 tracing backend |
| **`scripts/release-gate.ps1`** | F8-B4 扩展证据包 | 替换 p0-gate / docker-deploy-gate |

---

## 2. Actor / Role

| Actor | 说明 | 权限 / 场景 |
| --- | --- | --- |
| **平台 SRE / 运维** | 配置 scrape、解读 `/readyz`、响应 Prometheus 告警 | 无业务数据写权限；可访问 `/actuator/prometheus` |
| **发布工程师** | 发版前执行 release gate + evidence bundle | 可运行 host 脚本；读取 `artifacts/` |
| **On-call 值班** | 事故 triage、DR 演练 | 遵循 runbook；手动 failover 确认（ADR-0030） |
| **后端运行时** | `DocumentGenerationEngine`、`PdfConversionService` | 自动记录 Micrometer 指标 |
| **编排器（K8s / Compose）** | liveness `/healthz`、readiness `/readyz` | 503 时停止路由（Postgres down） |

---

## 3. Goal

1. **B1**：同步文档生成与 PDF 转换路径暴露 **可 scrape 的 SLO 指标**；失败率与 p95 延迟可在 Prometheus/Grafana 中观测。
2. **B2**：`/readyz` 返回 **结构化组件健康**（Postgres、Redis、MinIO、Kafka）；运维可诊断部分依赖降级而 **不** 误杀整个 pod（Postgres up 即 ready）。
3. **B3**：运维文档含 **可执行的 DR/备份/恢复演练 checklist**；与 ADR-0030（RPO ≤ 15 min / RTO ≤ 30 min）和 blue-green 交叉链接。
4. **B4**：Release gate 产出 **标准化 evidence bundle**（日志 + health + metrics 样本 + git/docker 元数据 + checklist 勾选）。

---

## 4. 已确认决策（2026-07-09）

| ID | 决策 |
| --- | --- |
| **F8-C1** | SLO 告警阈值采用 [non-functional-requirements.md](../requirements/non-functional-requirements.md) **LR-D5 pending proposal**（同步生成 p95 ≤ 3s；转换失败率告警）；规则 YAML 标注 `draft: true` 直至用户确认 NFR 数值 |
| **F8-C2** | **Readiness 门控不变（SOR-O06）**：仅 Postgres `SELECT 1` 决定 HTTP 200 vs 503；Redis/MinIO/Kafka 以 `checks.*.status` **报告** `UP` / `DOWN` / `DEGRADED` / `SKIPPED` |
| **F8-C3** | Micrometer 命名沿用现有点分风格：`docgen.generation.*`、`docgen.pdf.conversion.*`（与 `PdfConversionPoolMetrics` 一致）；**不**引入 vendor APM |
| **F8-C4** | 生成延迟 timer 标签：`outcome`（success/failure）、`format`（docx/pdf/both）、`mode`（sync/async 若可区分） |
| **F8-C5** | 转换成功率 = `docgen.pdf.conversion.outcome` counter（result=success/failure）+ 既有 pool gauge 保留 |
| **F8-C6** | Evidence bundle 输出目录：`artifacts/core-fortress-evidence/<timestamp>/`；与 `release-gate.ps1` 集成或由其调用 |
| **F8-C7** | DR 内容写入 **`docs/operations/runbook.md` 新章节**（非独立 ADR）；交叉链接 `deploy/blue-green-runbook.md` |
| **F8-C8** | F8 **依赖 F1–F7 Done**；在 F6∥F7 完成后启动；**无前端变更**、无 E2E/UIUX |
| **F8-C9** | F8 **不部署** Prometheus/Grafana 栈；仅提交 rules/dashboard JSON 与 scrape 参考 |
| **F8-C10** | 首次 DR drill **执行**归 LR-D2；F8 交付 checklist + 证据目录约定 |

---

## 5. 前置条件

- CORE-FORTRESS **F1–F7 Done**（渲染、运行时、LO 池、async 安全、前端 kernel、authoring UX）。
- Backend prod profile 已暴露 `/actuator/prometheus`（P9 / SOR-O01）。
- `ReadinessProbe` Postgres-only 基线存在（P15-T07c）。
- `PdfConversionPoolMetrics` 已注册 pool gauge（F4 / SOR-P03）。
- Host 可运行 `mvn verify` 与 `.\scripts\release-gate.ps1`。

---

## 6. 主旅程

### 6.1 SLO 指标采集

1. Runtime API 调用方发起同步 DOCX→PDF 生成。
2. `DocumentGenerationEngine` 记录 `docgen.generation.duration` timer（success/failure）。
3. `PdfConversionService` 记录 `docgen.pdf.conversion.duration` timer 与 `docgen.pdf.conversion.outcome` counter。
4. SRE scrape `/actuator/prometheus`；Prometheus 规则评估 p95 与失败率（draft 阈值）。

### 6.2 Readiness 深度诊断

1. K8s kubelet 调用 `GET /readyz`。
2. 后端执行 Postgres `SELECT 1`；并行 best-effort 探测 Redis ping、MinIO bucket head、Kafka admin（或 configured skip）。
3. 响应 JSON：`status` + `checks` map；Postgres down → **503**；Postgres up 且 Redis down → **200** + `checks.redis.status=DOWN`。

### 6.3 DR 演练（运维手册）

1. 运维按 runbook DR 章节：备份 Postgres + MinIO → 隔离栈 restore → Flyway noop 验证 → `/readyz` + 一次 sync 生成 smoke。
2. 记录 RPO/RTO 观测值至 `artifacts/dr-drill/<date>/`（F8 定义格式；执行归 LR-D2）。

### 6.4 Release evidence bundle

1. 发布工程师运行 `.\scripts\release-gate.ps1`（或 `-EvidenceBundle` 开关）。
2. 脚本：backend verify + frontend gates → 采集 `/healthz`、`/readyz` JSON、`/actuator/prometheus` 样本（含新 SLO series）、`git rev-parse`、compose 镜像 digest（若栈运行）。
3. 写入 `summary.json` + `CHECKLIST.md`；全部 PASS 方可 tag RC。

---

## 7. 验收场景（Given / When / Then）

### F8-B1 — SLO 指标

#### BDD-F8-B1-001 — 成功同步生成记录延迟

**Given** Docker 栈 healthy 且 Micrometer registry 已绑定  
**When** 一次 sync 生成（DOCX+PDF）成功完成  
**Then** `/actuator/prometheus` 暴露 `docgen_generation_duration_seconds`（或等价 dot 名）且 `_count ≥ 1`  
**And** 最近样本 `outcome=success`

#### BDD-F8-B1-002 — 失败生成记录 failure outcome

**Given** 可触发 fail-closed 生成失败（如无效 template version）  
**When** sync 生成返回业务错误  
**Then** `docgen.generation.duration` 仍记录一次 observation  
**And** `outcome=failure` 标签存在

#### BDD-F8-B1-003 — PDF 转换成功/失败 counter

**Given** PDF 转换路径可用  
**When** 一次转换成功且另一次转换失败（fake timeout 或 invalid input）  
**Then** `docgen.pdf.conversion.outcome` counter 分别递增 `result=success` 与 `result=failure`

#### BDD-F8-B1-004 — 告警规则与 NFR 提案对齐（draft）

**Given** `deploy/observability/prometheus-alerts.yaml` 已更新  
**When** 运维审阅 `HighGenerationLatencyP95` 规则  
**Then** expr 引用 `docgen.generation.duration` histogram  
**And** 阈值注释链接 LR-D5 pending proposal（3s / 10s 分层：HTTP vs generation）  
**And** 规则 metadata 含 `draft: true` 直至 NFR 确认

#### BDD-F8-B1-005 — 单元测试注册 metrics

**Given** CI 无 Docker  
**When** `GenerationMetricsTest` / `PdfConversionMetricsTest` 运行  
**Then** 指标名与 tag 在 `SimpleMeterRegistry` 中断言存在

---

### F8-B2 — Readiness 深度

#### BDD-F8-B2-001 — Postgres down → 503

**Given** Postgres 不可达（测试 stub 或 Testcontainers stop）  
**When** `GET /readyz`  
**Then** HTTP **503**  
**And** body `status=DOWN`  
**And** `checks.postgres.status=DOWN`

#### BDD-F8-B2-002 — Postgres up、Redis down → 200 degraded detail

**Given** Postgres 可达；Redis 不可达  
**When** `GET /readyz`  
**Then** HTTP **200**  
**And** `status=UP`  
**And** `checks.redis.status=DOWN`（或 `DEGRADED` per F8-C2 枚举）

#### BDD-F8-B2-003 — 全组件 up

**Given** Docker compose prod profile 全依赖 healthy  
**When** `GET /readyz`  
**Then** HTTP 200  
**And** `checks.postgres|redis|minio|kafka` 均为 `UP`（或 kafka `SKIPPED` 当 `ASYNC_TRANSPORT!=kafka`）

#### BDD-F8-B2-004 — Liveness 不变

**Given** 任意依赖状态  
**When** `GET /healthz`  
**Then** HTTP 200 `{"status":"UP"}` — **无** 深度检查

#### BDD-F8-B2-005 — K8s 探针文档同步

**Given** F8-B2 实现 merged  
**When** 运维阅读 `deploy/k8s-health-probes.md`  
**Then** 文档描述 `/readyz` 结构化 JSON 与 SOR-O06 门控 rationale

---

### F8-B3 — DR Runbook

#### BDD-F8-B3-001 — Runbook DR 章节可执行

**Given** 运维持有 repo + Docker  
**When** 按 `docs/operations/runbook.md` § Disaster Recovery 逐步执行（桌面演练，可不实际 destroy 生产）  
**Then** 每步有明确命令或交叉链接（backup、restore、smoke、RPO/RTO 记录模板）

#### BDD-F8-B3-002 — ADR-0030 交叉引用

**Given** runbook DR 章节  
**When** 审阅 RPO/RTO 声明  
**Then** 链接 [ADR-0030](../adr/operations/0030-operational-platform-baseline.md) 且数值一致（RPO ≤ 15 min，RTO ≤ 30 min）

#### BDD-F8-B3-003 — Blue-green 与 Flyway  forward-only

**Given** DR 章节  
**When** 运维查找 schema 回滚策略  
**Then** 明确 **Flyway forward-only** + blue-green color revert；**无** 隐含 down migration

---

### F8-B4 — Evidence Bundle

#### BDD-F8-B4-001 — Gate PASS 产出 evidence 目录

**Given** host gates 可绿  
**When** 运行 evidence bundle 脚本（standalone 或 release-gate 集成）  
**Then** 创建 `artifacts/core-fortress-evidence/<timestamp>/`  
**And** 含 `backend-verify.log`、`frontend-gates.log`（若运行）、`summary.json`

#### BDD-F8-B4-002 — Health + metrics 快照

**Given** Docker 栈在 `localhost:8080` 运行  
**When** bundle 脚本 `-IncludeRuntimeSnapshots`（或默认 prod compose up）  
**Then** 目录含 `healthz.txt`、`readyz.json`、`prometheus-sample.txt`（含 `docgen.generation` 或 `docgen_generation` series 行）

#### BDD-F8-B4-003 — Git 与版本元数据

**When** bundle 完成  
**Then** `summary.json` 含 `gitSha`、`gateVersion`（`core-fortress-evidence-v1`）、`timestamp` UTC

#### BDD-F8-B4-004 — Checklist 人工项

**Given** `CHECKLIST.md` 模板  
**When** 发布工程师审阅  
**Then** 含可勾选项：gates green、readyz UP、SLO series present、DR runbook reviewed、docker smoke（可选）

#### BDD-F8-B4-005 — Gate FAIL 非零退出

**Given** backend verify 失败  
**When** 运行 bundle  
**Then** 脚本 exit code **非 0**；`summary.json` `status=FAILED`

---

## 8. 边界与异常

| 场景 | 期望 |
| --- | --- |
| `/actuator/prometheus` scrape 超时 | 不影响 API Serving；SRE 重试 scrape |
| MinIO/Kafka 探测超时 | `checks.*.status=DOWN`；**不** 将 `/readyz` 降为 503（Postgres up） |
| 测试 profile | 深度探测可 `@Profile("!test")` 或 stub；单元测试用 mock clients |
| 无 Docker 的 CI | metrics 单元测试绿；scrape smoke **skip** 并记入 ledger |
| NFR 未确认 | 告警 firing **不** 视为 SLA 违约；仅 draft 监控 |
| F5 DLT depth | F8 **不** 阻塞于 DLT gauge；可选 tag 留 F8-T02 扩展位 |

---

## 9. 可观测证据

| 证据 | 说明 |
| --- | --- |
| `mvn verify` | Metrics + readiness 单元/集成测试 |
| Prometheus sample | `artifacts/core-fortress-evidence/*/prometheus-sample.txt` |
| Readiness JSON | 集成测试或 curl 快照 |
| Runbook diff | `docs/operations/runbook.md` DR 章节 |
| Alert rules | `deploy/observability/prometheus-alerts.yaml` promtool 或 documented lint |
| Release checklist | `docs/operations/core-fortress-release-checklist.md` 或 runbook 内嵌 |

---

## 10. 追溯

| 文档 | 用途 |
| --- | --- |
| [F8 详细计划](../plan/detail/CORE-FORTRESS-f8-observability-slo-dr.md) | 任务分解 |
| [CORE-FORTRESS 纲领](../plan/detail/CORE-FORTRESS-program-roadmap.md) | F1–F8 程序 |
| [ADR-0030](../adr/operations/0030-operational-platform-baseline.md) | RPO/RTO、health 策略 |
| [non-functional-requirements.md](../requirements/non-functional-requirements.md) | LR-D5 SLO 提案 |
| [LRP-D ops observability](../plan/detail/LRP-D-ops-observability.md) | LR-D2/D3 全量 backlog |
| [deploy/observability/README.md](../../deploy/observability/README.md) | Metrics 草案 |
| [deploy/k8s-health-probes.md](../../deploy/k8s-health-probes.md) | 探针契约 |
| [docs/operations/runbook.md](../operations/runbook.md) | 运维主 runbook |

---

## 11. BDD readiness

**`ready`** — 规格完整；F8-C1–C10 已裁决；scope 为 achievable slice（SLO core + readiness depth + DR 章节 + evidence bundle）。待 **F1–F7 Done** 后 hand off `backend-engineer` + `deploy-engineer`/`doc-keeper` + `build-deploy-agent`。

**Pending questions（不阻塞 F8-T02+ 实现）**

| ID | 问题 | 默认 |
| --- | --- | --- |
| **F8-Q1** | NFR 数值何时从 pending → confirmed？ | 保持 draft 告警直至 LR-D5 用户确认 |
| **F8-Q2** | Kafka 探测在 `ASYNC_TRANSPORT=local` 是否 `SKIPPED`？ | **是**（F8-C2） |
| **F8-Q3** | 首次 DR drill 是否在 F8 内执行？ | **否** — LR-D2；F8 仅 checklist |
