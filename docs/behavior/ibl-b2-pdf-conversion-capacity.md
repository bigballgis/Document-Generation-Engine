# BDD 行为规格：IBL-B2 — PDF conversion capacity（关闭 DEF-LRP-D6-001）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-B2` |
| **编写日期** | 2026-07-19 |
| **程序 / 队列** | IBL Wave B · **IBL-B2** / F11（`ibl-b2-pdf-conversion-capacity`） |
| **Slice** | `ibl-b2-pdf-conversion-capacity` |
| **Branch** | `feat/ibl-b2-pdf-conversion-capacity` |
| **Worktree** | `D:/working/DGE-ibl-b2-pdf-conversion-capacity`（BDD 撰写时尚未创建 — 实现须在隔离 worktree） |
| **Base** | `origin/main`（provision 时 tip） |
| **Placement** | ISOLATED |
| **Task Master** | **#114** IBL-B2 — Batch Recommendation **solo**；`member_task_ids: ["114"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-b2-pdf-conversion-capacity`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F11 / IBL-B2；缺陷 [DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md)；历史证据 [lrp-d6-load-smoke/](../plan/evidence/lrp-d6-load-smoke/)；taxonomy 前序 [prod-ops-resilience-pdf-pool.md](./prod-ops-resilience-pdf-pool.md)（PRR-D01A / #104）；属性 [DocgenRenderingProperties](../../backend/src/main/java/com/bank/docgen/infrastructure/config/DocgenRenderingProperties.java)；NFR 属性表（**非** confirmed SLO）[non-functional-requirements.md](../requirements/non-functional-requirements.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（无 Vue / Playwright / UIUX 义务） |

**完成声明约束：** 本叶关闭 F11——交付**已文档化的 PDF 转换容量计划**（pool / queue / async offload），使 **agreed smoke** 下同步 PDF 路径**不再**呈现 LR-D6-class **≥8/10** 失败签名，并保留 queue/reject **可观测指标**；**DEF-LRP-D6-001** 以证据 **CLOSED** 或 **SUPERSEDED**。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 IBL Wave B / 程序 Done；**禁止**把 IBL-B3（veraPDF）/ B4（long-clause）/ B7（Word Path E）并入本叶；**禁止**发明 confirmed NFR SLOs（p95 等仍 §待确认）；**禁止**发明 Word baselines / Word host 测量。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["114"]
  proposed_slice_id: ibl-b2-pdf-conversion-capacity
  shared_acceptance_surface: >
    Documented PDF capacity plan (pool/queue/async offload);
    sync path no LR-D6-class 8/10 PDF failure under agreed smoke;
    queue/reject metrics; DEF-LRP-D6-001 closed or superseded
  vetoes_applied:
    - b3-verapdf
    - b4-long-clause
    - b7-word-path-e
    - invent-nfr-slos
    - word-host-baselines
    - umbrella-106-registry-only
  evidence_amortization: mvn verify + queued docker-deploy smoke (+ load-smoke harness)
```

| IN（本叶） | OUT（后续 / 明确禁止） |
| --- | --- |
| 容量计划文档（pool / queue / async offload 分层） | IBL-B3 veraPDF |
| 同步 PDF 路径：有界 queue 吸收 agreed smoke 并发（关闭 F11 默认 2/0 导致的即时饱和面） | IBL-B4 long-clause / theme 08 |
| 饱和 fail-closed：`PDF_CONVERSION_CAPACITY_EXCEEDED`（503, retryable） | IBL-B7 Word Path E / #3b GO |
| 既有 `ASYNC_TASK` 作为客户端溢出路径（文档 + 冒烟/回归证明可用） | 发明 confirmed NFR SLOs（p95/并发 OK 宣称） |
| Micrometer：queue gauges + rejection counter（名称钉死） | k6 / IBL-D3；chaos / IBL-D4（可引用本叶指标） |
| DEF-LRP-D6-001 CLOSED 或 SUPERSEDED + 证据目录 | Word host / Word baselines |
| Gates：`mvn verify` + queued Docker deploy smoke | Playwright / OA 旅程；翻转 #3b/#5a；go-live |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| 产品默认 `conversion-pool-size=2`、`conversion-queue-capacity=0`（fail-fast Abort） | `DocgenRenderingProperties`；`application.yml`；F4-C5；D01A-C9；NFR 属性表 |
| LR-D6 Scenario A：n=20（DOCX/PDF 交替）success=12；**8× PDF** 失败；DOCX 全成功；`poolRejections=0` | [latest-summary.json](../plan/evidence/lrp-d6-load-smoke/latest-summary.json) |
| 命名缺陷：并发 PDF → 错标 `TEMPLATE_VALIDATION_FAILED` / `serviceUnavailable`（CB/timeout 映射） | [TRIAGE-pdf-422.md](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) **DEF-LRP-D6-001** |
| **PRR-D01A（#104）已修 taxonomy**：CB/timeout → `GENERATION_*`；池饱和 → `PDF_CONVERSION_CAPACITY_EXCEEDED`；**冻结过**默认 2/0 | [prod-ops-resilience-pdf-pool.md](./prod-ops-resilience-pdf-pool.md) |
| F11 / IBL-B2 仍要求：**容量计划** + sync 不再呈现 **8/10** 类失败 + metrics + DEF 关闭/ supersede | IBL 计划验收 |
| 同步转换已走内部 offload：`PdfConversionOffloadSupport` + `pdfConversionExecutor` | rendering 包 |
| 客户端异步模式已存在：`output.mode=ASYNC_TASK` → HTTP **202** | contract-outline / ADR-0008 |
| 池指标已存在：`docgen.pdf.conversion.pool.{active,queue.size,queue.remaining,rejections}` | `PdfConversionPoolMetrics` / `PdfConversionPoolRejectionMetrics` |

### 1.2 与 D01A 的分工（勿重做）

| 叶 | 关闭什么 |
| --- | --- |
| **PRR-D01A** | 错误 **taxonomy**（禁止 CB/timeout 伪装 `TEMPLATE_VALIDATION_FAILED`）；池饱和码分离；当时冻结 2/0 |
| **IBL-B2（本叶）** | **容量行为**：有界 queue + 文档化 async offload；agreed smoke 下消除 LR-D6-class **≥8/10 PDF 失败**；DEF 证据闭环。**修订** D01A-C9 / F4-C5 的「产品默认必须 2/0」——改为本文件 §4 的单机有界默认（仍 **禁止无界**） |

### 1.3 行为域

| 域 | 摘要 |
| --- | --- |
| **B2-S1 Capacity plan doc** | 持久化运维/架构可读的容量计划（sync pool/queue、reject、async 溢出、指标、Docker 验收钉） |
| **B2-S2 Sync absorption** | 同步 PDF：`poolSize + queueCapacity` 覆盖 agreed smoke 的 PDF 并发槽位；servlet **可**在队列内等待 worker（有界），**不得**无界排队 |
| **B2-S3 Reject fail-closed** | 槽位耗尽 → 立即 `PDF_CONVERSION_CAPACITY_EXCEEDED`（503），递增 rejection 指标 |
| **B2-S4 Async offload** | 持续高并发：调用方使用既有 `ASYNC_TASK`（202）；与 sync **共享** `pdfConversionExecutor`（本叶不发明第二 LO 集群） |
| **B2-S5 Metrics** | queue / reject（及既有 outcome）名称稳定、可刮取 |
| **B2-S6 DEF disposition** | DEF-LRP-D6-001 CLOSED 或 SUPERSEDED + 新证据 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **Runtime API 调用方** | 同步 `SYNC_*` 或异步 `ASYNC_TASK` 生成（含 PDF） | 期望并发 PDF 不再出现 LR-D6-class 8/10 崩；饱和时拿到可重试 503 |
| **平台运维 / SRE** | 读容量计划 + Micrometer / Prometheus | 区分 queue 深度 vs reject vs CB/timeout |
| **系统** | `pdfConversionExecutor` / `PdfConversionOffloadSupport` / Resilience / async task 生命周期 | 有界执行；fail-closed；指标诚实 |
| **（非本片）管理 UI 用户** | — | `frontend_ui_in_scope=false` |

权限：本叶**无新权限码**；既有 runtime 授权不变。

---

## 3. Goal

1. 关闭 F11：交付**文档化容量计划**，并将同步路径默认从「pool=2 / queue=0 即时 fail-fast」升级为**单机有界吸收类**（仍禁止无界）。  
2. 在 **agreed smoke** 下，同步 PDF **不得**再现 LR-D6-class **≥8/10** 失败签名。  
3. 饱和路径保持 **fail-closed** + 正确 envelope + rejection 指标。  
4. 明确 **sync vs async**：sync 吸收突发（有界队列）；持续溢出走 `ASYNC_TASK`。  
5. **DEF-LRP-D6-001** 以证据 CLOSED/SUPERSEDED。  
6. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不发明 NFR SLOs；不碰 Word host。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（仓库事实裁决 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **B2-C1** | **容量计划文档（强制交付物）：** 新增或更新一份可索引文档（推荐 `docs/operations/pdf-conversion-capacity-plan.md`，并从 `docs/operations/runbook.md` / `docs/README.md` 链接），至少包含：sync pool/queue 默认与 env 名、饱和 reject 语义、async 溢出指导、指标名、Docker 验收钉、与 D01A taxonomy 的关系、**非** SLO 声明。 | IBL-B2 验收「Documented capacity plan」 |
| **B2-C2** | **产品默认（单机有界吸收类）修订：** `docgen.rendering.conversion-pool-size` 默认 **4**（`PDF_CONVERSION_POOL_SIZE`）；`conversion-queue-capacity` 默认 **8**（`PDF_CONVERSION_QUEUE_CAPACITY`）；`core=max=poolSize`；`RejectedExecutionHandler=AbortPolicy`。同步槽位 = **pool + queue = 12** ≥ agreed smoke 的 **10** 路并发 PDF。**禁止**无界队列（`Integer.MAX_VALUE` / 无界 `LinkedBlockingQueue`）。运维可用 env **有界**调高/调低（含刻意回到 2/0 的 fail-fast 环境）。此决策 **修订** D01A-C9 / F4-C5 / NFR 属性表默认值；**不是** confirmed p95/并发 SLO。 | F11；smoke 10× PDF；SOR-P03「有界」精神保留 |
| **B2-C3** | **Sync 路径行为：** 同步 PDF（`SYNC_STREAM` / `SYNC_DOWNLOAD_URL`）经既有 `PdfConversionOffloadSupport` 提交到 `pdfConversionExecutor`。当 active < max 或 queue 有剩余容量时：**接受**任务；调用方线程在 `future.get(timeout)` 上等待（timeout = `conversion-timeout-seconds` + 既有 buffer，默认 120+5s）。当 active==max **且** queue remaining==0：**立即**失败，不无限阻塞等槽位。 | 现有 offload；吸收突发 |
| **B2-C4** | **Reject 语义（不变 envelope）：** 饱和 → HTTP **503**；`error.code=PDF_CONVERSION_CAPACITY_EXCEEDED`；`category=GENERATION`；`retryable=true`；`messageKey=api.error.generation.pdfConversionCapacityExceeded`。不得伪装为 `TEMPLATE_VALIDATION_FAILED` / 422。 | D01A-C10；contract-outline |
| **B2-C5** | **Async offload（客户端溢出，非新 API）：** 持续并发超过 sync 预算时，调用方应使用既有 `output.mode=ASYNC_TASK` → HTTP **202 Accepted** + task 查询（ADR-0008）。本叶**不**新增第三种 mode；**不**在饱和时自动把 sync 请求静默改写为 async。容量计划必须写明：async 与 sync **共享**同一 `pdfConversionExecutor` / LO 转换路径，故 async **不能**无限绕过池上限——仅释放 HTTP 同步等待。 | 计划「queue + async offload」；既有契约 |
| **B2-C6** | **指标名（强制，沿用既有）：** (1) Counter `docgen.pdf.conversion.pool.rejections` — Abort/预检饱和时递增；(2) Gauge `docgen.pdf.conversion.pool.active`；(3) Gauge `docgen.pdf.conversion.pool.queue.size`；(4) Gauge `docgen.pdf.conversion.pool.queue.remaining`；(5) 保留 `docgen.pdf.conversion.outcome` / `docgen.pdf.conversion.duration`。本叶须保证 Docker 验收栈可刮取 (1)–(4)；单元/集成覆盖饱和时 (1) 递增。本叶**不**强制改 alert 阈值（仍 draft / 待确认）。 | LR-D3；D01A-C11；IBL-B2「metrics for queue/reject」 |
| **B2-C7** | **Agreed smoke（本叶验收定义）：** 在 queued Docker 验收栈（8080）上，复用 LR-D6 harness 同类条件：FOL（或等价已发布 demo）同步 `generate`，`syncConcurrency≥20`，DOCX/PDF 交替（PDF 半部 = **10**），或等价 **10× 并行同步 PDF** 探针。证据写入 `docs/plan/evidence/ibl-b2-pdf-capacity/`（及/或更新 lrp-d6 指针 + supersession 注记）。 | IBL-B2；lrp-d6 harness |
| **B2-C8** | **「不再呈现 LR-D6-class 8/10」硬条：** 在 B2-C7 跑次中，PDF 请求失败数 **< 8**（即不得再现 ≥8/10 PDF 失败签名）。进一步：在默认 4+8 槽位下，该 smoke 的 PDF 并发 **不得**以 `PDF_CONVERSION_CAPACITY_EXCEEDED` Abort 风暴为主因（期望 `poolRejectionCount` 对应该 smoke ≈ **0**）。若仍有失败，必须为正确 taxonomy（`GENERATION_SERVICE_UNAVAILABLE` / `GENERATION_TIMEOUT` / 真实渲染失败等），**禁止** `TEMPLATE_VALIDATION_FAILED` 承载容量/CB/timeout。DOCX 半部保持成功（回归）。 | F11；DEF 关闭条 |
| **B2-C9** | **DEF-LRP-D6-001 处置：** 实现完成后更新 triage：状态 **CLOSED**（taxonomy 已由 D01A + 容量由本叶共同关闭）或 **SUPERSEDED**（指向本叶证据 + 残余明确归属，如 IBL-D4 chaos）。不得仅改文案、无 smoke 证据。 | 计划验收 |
| **B2-C10** | **禁止削弱 CB 以刷绿：** 不得为通过 smoke 关闭/放飞 Resilience4j CB 或把 timeout 调到无意义值。容量吸收靠 pool/queue；CB/timeout 语义保持 D01A。 | LR-D6 triage 纪律 |
| **B2-C11** | **转换内容失败不变：** soffice 非零等仍走既有 `pdfConversionFailed` / rendering 失败路径；非本叶主角。 | D01A-C14 |
| **B2-C12** | **Docker 验收钉：** queued `docker-deploy-queue` 栈必须实际加载 B2-C2 默认（或显式等价 env）。若 compose 曾依赖隐式旧默认 2/0，本叶须同步文档/示例/override，使 Stage 5/10 smoke 与 B2-C2 一致。 | 计划 gates |
| **B2-C13** | **`frontend_ui_in_scope=false`：** 无 Vue / i18n UI / Playwright / UIUX。 | handoff |
| **B2-C14** | **门禁：** `mvn -B -ntp -f backend/pom.xml verify` **GREEN**；Stage 5/10 **queued** Docker deploy + B2-C7/C8 smoke 证据；architecture review。E2E/UIUX N/A。 | delivery constitution |
| **B2-C15** | **完成边界：** B2 Done ≠ Wave B 完备；≠ go-live；#3b/#5a 保持 CONDITIONAL；≠ B3/B4/B7；≠ confirmed NFR SLO；≠ Word baselines。 | 队列政策 |

### 4.2 Sync vs Async（对照表 — 实现对齐用）

| 维度 | Sync（`SYNC_STREAM` / `SYNC_DOWNLOAD_URL`） | Async（`ASYNC_TASK`） |
| --- | --- | --- |
| HTTP | 阻塞至完成或失败（流/URL） | **202** + task id；轮询/查询完成 |
| PDF 转换执行 | `pdfConversionExecutor` via `PdfConversionOffloadSupport` | **同一**转换池 / LO 路径（共享容量） |
| 突发吸收 | 有界 queue（默认 8）+ pool（默认 4） | 不占用调用方 HTTP 线程等待 LO；仍占池槽位 |
| 饱和 | 立即 **503** `PDF_CONVERSION_CAPACITY_EXCEEDED` | 任务可能延迟/失败；池饱和时转换阶段同码；受理 202 不表示转换已占槽 |
| 适用 | 低/中突发、需要同步字节/URL | 持续高并发、可接受最终一致 |

### 4.3 指标名（验收钉死）

| Metric | Type | 含义 |
| --- | --- | --- |
| `docgen.pdf.conversion.pool.rejections` | Counter | 池/队列饱和拒绝次数 |
| `docgen.pdf.conversion.pool.active` | Gauge | 活跃转换线程数 |
| `docgen.pdf.conversion.pool.queue.size` | Gauge | 队列中等待任务数 |
| `docgen.pdf.conversion.pool.queue.remaining` | Gauge | 队列剩余容量 |
| `docgen.pdf.conversion.outcome` | Counter | success/failure（既有） |
| `docgen.pdf.conversion.duration` | Timer | 转换耗时（既有） |

### 4.4 Open questions

**无阻塞项。** 默认 4/8、agreed smoke 定义、async=既有 `ASYNC_TASK`、指标名均由本 BDD 确认。实现可调整 Docker 钉载方式（compose env vs application 默认），但数值与语义不得偏离 B2-C2/C3/C4/C8。

```text
open_questions: []
```

---

## 5. Trigger / Preconditions

### Trigger

- Runtime 同步 generate（PDF）进入 LibreOffice 转换 / `PdfConversionOffloadSupport`。  
- Runtime `ASYNC_TASK` PDF 生成进入同一转换池。  
- 池饱和预检 / `RejectedExecutionException`。  
- Agreed smoke harness 对 Docker 8080 发起并发。  
- 运维刮取 Micrometer / Prometheus 池指标。

### Preconditions

- PRR-D01A taxonomy 行为仍在（或等价）：CB/timeout **不是** `TEMPLATE_VALIDATION_FAILED`。  
- FOL（或等价）demo 已导入并发布；runtime 凭证可用（同 LR-D6 harness）。  
- 验收栈经 `docker-deploy-queue.ps1`；单 Docker host。  
- LibreOffice 在栈内可用（真实转换，非跳过）。

---

## 6. Primary journey

1. 运维/实现按 B2-C2 部署（默认 pool=4、queue=8），容量计划文档已索引。  
2. 调用方发起同步 PDF generate（单请求）→ 200/流成功；指标 active 短暂上升后回落。  
3. Agreed smoke：≥20 路交替 DOCX/PDF（或 10× PDF）并发 → PDF 失败数 **< 8**；queue gauges 可观测；无 Abort 风暴。  
4. 人工或测试将池打满（例如测试中缩小 pool/queue）→ 下一 PDF sync 立即 **503** `PDF_CONVERSION_CAPACITY_EXCEEDED`；`pool.rejections` +1。  
5. 高并发调用方改用 `ASYNC_TASK` → **202**；任务完成可查询；不发明新 API。  
6. 更新 DEF triage + 证据目录 → CLOSED/SUPERSEDED。

---

## 7. System responses

### 7.1 Success

| 形态 | 响应 |
| --- | --- |
| Sync PDF（容量内） | 既有成功流/URL；转换经 offload 池 |
| Async PDF 受理 | HTTP **202** + task 摘要（既有） |
| Agreed smoke | PDF 失败数 < 8；DOCX 成功；证据落盘 |
| Metrics | queue/active/remaining/rejections 可刮取 |

### 7.2 Fail-closed

| 条件 | 行为 |
| --- | --- |
| 池+队列饱和 | **503** `PDF_CONVERSION_CAPACITY_EXCEEDED`；`retryable=true`；rejection counter++ |
| CB open | **503** `GENERATION_SERVICE_UNAVAILABLE`（D01A；非本叶改码） |
| Generation timeout | **504** `GENERATION_TIMEOUT`（D01A） |
| LO 内容失败 | 既有 pdfConversionFailed / rendering 失败 |
| 未授权 | 既有 401/403；本叶不放宽 |

---

## 8. Acceptance scenarios（Given / When / Then）

### BDD-IBL-B2-001 — 容量计划文档存在且可索引

**Given** 本叶交付完成  
**When** 打开容量计划文档（B2-C1）及 `docs/README.md` / runbook 链接  
**Then** 文档写明：默认 pool=4、queue=8、env 名、Abort 饱和语义、`ASYNC_TASK` 溢出指导、§4.3 指标名、与 D01A taxonomy 关系  
**And** 明确 **未**将任何 p95/errorRate 提升为 confirmed NFR SLO

### BDD-IBL-B2-002 — 产品默认 pool/queue 绑定

**Given** 未设置 `PDF_CONVERSION_POOL_SIZE` / `PDF_CONVERSION_QUEUE_CAPACITY`  
**When** 绑定 `DocgenRenderingProperties`（或启动验收配置）  
**Then** `conversionPoolSize == 4`  
**And** `conversionQueueCapacity == 8`  
**And** executor `core=max=4`、`AbortPolicy`、队列有界

### BDD-IBL-B2-003 — Sync 有界队列吸收（单元/集成）

**Given** 测试配置 `poolSize=2`、`queueCapacity=2`（有界缩小夹具）  
**And** 2 个转换占满 active  
**When** 再提交 2 个同步转换  
**Then** 后 2 个进入队列（不立即 reject）  
**And** `docgen.pdf.conversion.pool.queue.size` 反映等待  
**When** 再提交第 5 个（active 满且 queue 满）  
**Then** 立即 `PdfConversionCapacityExceededException` / API **503** `PDF_CONVERSION_CAPACITY_EXCEEDED`  
**And** `docgen.pdf.conversion.pool.rejections` 递增

### BDD-IBL-B2-004 — 饱和 envelope（回归 D01A）

**Given** 同步 PDF 路径池已饱和  
**When** 调用方收到错误  
**Then** HTTP **503**  
**And** `error.code=PDF_CONVERSION_CAPACITY_EXCEEDED`  
**And** `error.category=GENERATION`  
**And** `error.retryable=true`  
**And** `error.messageKey=api.error.generation.pdfConversionCapacityExceeded`  
**And** **不是** `TEMPLATE_VALIDATION_FAILED` / HTTP 422

### BDD-IBL-B2-005 — Agreed smoke：无 LR-D6-class 8/10

**Given** Docker 验收栈 healthy（queued deploy）且 FOL（或等价）可 generate  
**And** 栈加载 B2-C2 容量（4+8 或证明等价）  
**When** 运行 B2-C7 agreed smoke（≥20 交替或 10× 并行 PDF）  
**Then** PDF 失败数 **< 8**  
**And** 不得出现以 `TEMPLATE_VALIDATION_FAILED` 承载的容量/CB/timeout  
**And** 对应该 smoke，`poolRejectionCount` ≈ **0**（无 Abort 风暴）  
**And** 证据写入 `docs/plan/evidence/ibl-b2-pdf-capacity/`（机器可读 summary + 人类可读 mirror）

### BDD-IBL-B2-006 — DOCX 半部不回归

**Given** 同一 agreed smoke  
**When** 统计 DOCX 请求  
**Then** DOCX 全部成功（与历史 LR-D6 DOCX 全成功一致）

### BDD-IBL-B2-007 — Async offload 路径仍可用

**Given** 合法 runtime 凭证与已发布模板  
**When** `generate` 使用 `output.format=PDF` 且 `output.mode=ASYNC_TASK`  
**Then** HTTP **202** 且返回 task id/查询方式（既有契约）  
**And** 任务可完成或失败可查询（本叶不新增强制 SLA）  
**And** 容量计划文档写明 async 与 sync 共享转换池

### BDD-IBL-B2-008 — 指标可观测（queue + reject）

**Given** 非 test profile 的运行中应用（Docker 验收或等价）  
**When** 刮取 Micrometer /actuator 允许的 metrics 端点（按既有安全约束）  
**Then** 存在 `docgen.pdf.conversion.pool.active`  
**And** 存在 `docgen.pdf.conversion.pool.queue.size`  
**And** 存在 `docgen.pdf.conversion.pool.queue.remaining`  
**And** 存在 `docgen.pdf.conversion.pool.rejections`  
**And** 在 BDD-IBL-B2-003 饱和场景下 rejections 计数增加

### BDD-IBL-B2-009 — 禁止无界队列「修复」

**Given** 产品默认与 Docker 验收配置  
**When** 审查 `PdfConversionExecutorConfig` / 部署 env  
**Then** `conversion-queue-capacity` 为有限非负整数  
**And** **不**使用无界队列作为默认或验收钉

### BDD-IBL-B2-010 — 禁止为刷绿削弱 CB

**Given** 本叶变更集  
**When** 审查 Resilience4j PDF/generation CB 与 timeout 配置  
**Then** 无「关闭 CB / 无限 timeout」类改动仅为通过 smoke  
**And** CB open / timeout 仍映射 D01A 码族

### BDD-IBL-B2-011 — DEF-LRP-D6-001 关闭或 supersede

**Given** BDD-IBL-B2-005 证据已落盘  
**When** 更新 [TRIAGE-pdf-422.md](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md)（或继任 triage）  
**Then** 状态为 **CLOSED** 或 **SUPERSEDED**  
**And** 链接本叶证据目录与 D01A taxonomy 关闭说明  
**And** 若 SUPERSEDED，残余项指向明确后续（如 IBL-D4），不得模糊悬空

### BDD-IBL-B2-012 — 治理冻结

**Given** 本叶 Done  
**When** 检查 launch-readiness checklist / 程序声明  
**Then** **不**翻转 #3b / #5a  
**And** **不**宣称 go-live 或 IBL 程序 Done  
**And** Wave B 仍可 In Progress（仅 B2 行 Done）  
**And** Formal phase 仍为 **None**

---

## 9. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| env 将 pool/queue 调回 2/0 | 允许（fail-fast 环境）；agreed smoke **必须**在 4+8（或等价 ≥10 PDF 槽位）配置下跑 |
| 并发 ≫ pool+queue | 合法 503 容量拒绝；指标反映；**不是**缺陷 |
| Async 与 sync 同时打满池 | 共享池；后到者转换阶段可 503/失败；文档已说明 |
| 单次转换超时 | 既有 timeout → 渲染失败或 `GENERATION_TIMEOUT`（按 D01A/既有路径）；不改为无限等 |
| Test profile | 测试可用缩小夹具；不得让 `mvn verify` 依赖真实 Docker LO（smoke 旗标门控，同 LR-D6） |

---

## 10. Observable evidence

| 证据 | 用途 |
| --- | --- |
| 容量计划 md + docs 索引链接 | B2-C1 |
| `DocgenRenderingProperties` 默认 + 绑定测试 | B2-C2 |
| 池饱和单测 / IT | B2-003 / B2-004 / B2-008 |
| `docs/plan/evidence/ibl-b2-pdf-capacity/*` | B2-005 / B2-006 / B2-011 |
| DEF triage 状态更新 | B2-011 |
| `mvn verify` 日志 | 门禁 |
| queued Docker deploy + healthz | Stage 5/10 |
| actuator/metrics 刮取（验收栈） | queue/reject |

---

## 11. Traceability

| 项 | 引用 |
| --- | --- |
| Program task | IBL-B2 / Task Master **#114** |
| Finding | F11 |
| Defect | DEF-LRP-D6-001 |
| Prior taxonomy | PRR-D01A / #104 / `prod-ops-resilience-pdf-pool.md` |
| Historical smoke | `docs/plan/evidence/lrp-d6-load-smoke/` |
| Properties | `DocgenRenderingProperties`；`PDF_CONVERSION_*` env |
| NFR | 属性默认可更新；§待确认 SLO **不**因本叶变为 confirmed |
| OUT | B3 / B4 / B7；Word；#3b/#5a；go-live |

---

## 12. BDD readiness

```text
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ibl-b2-pdf-conversion-capacity.md
task_ids: ["114", "IBL-B2"]
frontend_ui_in_scope: false
next: plan-orchestrator → rendering-engineer + backend-engineer (+ build-deploy-agent for smoke)
```

**Handoff notes for implementers**

1. 先落容量计划文档与默认 4/8 + 绑定测试（Red→Green）。  
2. 饱和/指标回归对齐 D01A envelope。  
3. queued Docker deploy 后跑 agreed smoke；证据目录 + DEF triage。  
4. 勿改 CB「刷绿」；勿发明 SLO；勿动 B3/B4/B7 / Word。  
5. Worktree：`../DGE-ibl-b2-pdf-conversion-capacity`（BDD 时尚未创建 — stage 0 必须先 provision）。
