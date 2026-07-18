# BDD 行为规格：PRR-D01a — ResilienceFailureMapper + PDF conversion pool

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-PRR-D01A` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | NON-CE PRR Wave D **split** leaf（`prod-ops-resilience-pdf-pool`） |
| **Slice** | `prod-ops-resilience-pdf-pool` |
| **Branch** | `feat/prod-ops-resilience-pdf-pool` |
| **Worktree** | `D:/working/DGE-prod-ops-resilience-pdf-pool` |
| **Placement** | ISOLATED |
| **Task Master** | **#104** PRR-D01 — Batch Recommendation **split**；本叶 `member_task_ids: ["104"]`（叶 lead；Wave D 其余 bag 串行后续叶） |
| **Formal phase** | **None**（可靠性加固叶；不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **split**（`member_task_ids: ["104"]`；`proposed_slice_id: prod-ops-resilience-pdf-pool`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；契约对照 [contract-outline.md](../api/contract-outline.md) / [openapi-v1.yaml](../api/openapi-v1.yaml)；缺陷 [DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md)；池容量 [NFR §production rendering](../requirements/non-functional-requirements.md) + SOR-P03 / F4 |
| **Frontend UI** | **`frontend_ui_in_scope=false`** |

**完成声明约束：** 本叶仅关闭 Resilience4j / timeout / bulkhead 失败的**稳定错误 taxonomy**，并确认 PDF 转换池 **bounded + 饱和 fail-closed**（与既有 NFR/SOR-P03 对齐）。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b**（保持 **CONDITIONAL**）；**禁止**将 **#5a** 标为 **GO**；**禁止**交付本文件 §OUT 所列后续叶范围。

---

## 0. Batch / queue context

```text
batch_recommendation:
  decision: split
  member_task_ids: ["104"]
  proposed_slice_id: prod-ops-resilience-pdf-pool
  rationale: Wave D bag split; this leaf = resilience mapper + PDF pool only
```

| IN（本叶） | OUT（后续串行叶） |
| --- | --- |
| `ResilienceFailureMapper` → 稳定 API error envelope（code / category / retryable / messageKey），不泄露 internals | Actuator exposure hardening |
| PDF conversion pool capacity / 饱和 fail-closed（对齐既有 NFR；禁止发明无界池） | nginx CSP |
| 与 DEF-LRP-D6-001 taxonomy 错标相关的可测修复 | Dashboard summary API |
| | Doc hygiene ADR-0044 notes / listAll / knip |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| CB open / `TimeoutException` 被映射为 `TemplateValidationException("api.error.generation.serviceUnavailable")` | `ResilienceFailureMapper.java` |
| 该异常经 `TemplateExceptionAdvice` 恒定发出 **HTTP 422** + `TEMPLATE_VALIDATION_FAILED`（category `TEMPLATE`） | `TemplateExceptionAdvice` + [DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) |
| 契约已定义正确码族：`GENERATION_SERVICE_UNAVAILABLE`（503）/ `GENERATION_TIMEOUT`（504） | [contract-outline.md](../api/contract-outline.md) §错误码表 |
| PDF 池饱和路径**已有**独立码：`PDF_CONVERSION_CAPACITY_EXCEEDED`（503, `retryable=true`） | `PdfConversionCapacityExceededException` + `ErrorEnvelopeFactory.pdfConversionCapacityExceeded` |
| 池默认 **bounded**：`conversion-pool-size=2`，`conversion-queue-capacity=0`，`AbortPolicy` | `application.yml` / `PdfConversionExecutorConfig` / NFR / SOR-P03 |
| OpenAPI / contract-outline **尚未**列出 `PDF_CONVERSION_CAPACITY_EXCEEDED`（运行时已存在） | `docs/api/` grep 无匹配 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **D01A-S1 Resilience taxonomy** | Resilience4j CB open、timeout、bulkhead full（及 cause chain 中同类）映射到契约稳定 envelope；**禁止**再标为 `TEMPLATE_VALIDATION_FAILED` |
| **D01A-S2 No internals leak** | 对外 `error.message` / `messageKey` / `code` 不得包含 Resilience4j 类名、堆栈、breaker 名、内部线程名 |
| **D01A-S3 Preserve business exceptions** | `TemplateValidationException` / `RenderingOperationException` / `PdfConversionCapacityExceededException` 透传，不被 mapper 改写为「服务不可用」 |
| **D01A-S4 PDF pool fail-closed** | 饱和时立即拒绝（fail-fast）；**禁止**无界队列；默认池/队列保持 NFR 值；继续 503 + `PDF_CONVERSION_CAPACITY_EXCEEDED` |
| **D01A-S5 Taxonomy separation** | 池容量耗尽 ≠ CB open ≠ generation timeout；三者可被 ops / load-smoke 区分 |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **Runtime API 调用方** | 同步生成（含 PDF）消费者 | 收到可重试、可分类的稳定错误码；不为 CB/timeout 误判为模板校验失败 |
| **平台运维 / SRE** | Operator | 区分 `PDF_CONVERSION_CAPACITY_EXCEEDED` vs `GENERATION_SERVICE_UNAVAILABLE` vs `GENERATION_TIMEOUT`；池 rejection 指标仍可用 |
| **系统** | `ResilienceFailureMapper` + `ResilienceSupport` + `pdfConversionExecutor` / `PdfConversionOffloadSupport` | 映射稳定；池有界；饱和 fail-closed |
| **（非本片）管理 UI 用户** | — | `frontend_ui_in_scope=false` |

权限：本叶**无新权限码**；既有 runtime / management 授权不变。

---

## 3. Goal

1. 修复 DEF-LRP-D6-001：**Resilience / timeout / bulkhead** 失败不再伪装为 `TEMPLATE_VALIDATION_FAILED`。
2. 对外错误符合统一 envelope：`code` + `category` + `retryable` + `messageKey` + 安全英文 `message`（English-first）。
3. PDF 转换池保持 **bounded** 与 **饱和 fail-closed**；容量错误与 resilience 错误可区分。
4. 不泄露内部实现细节；不发明无界池；不扩大 Wave D 其它 bag 范围。
5. API-first；无 FE / Playwright 义务。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **D01A-C1** | **CB open → `GENERATION_SERVICE_UNAVAILABLE`：** cause chain 中出现 `CallNotPermittedException`（或等价「circuit open / call not permitted」）时，API 必须返回：`error.code=GENERATION_SERVICE_UNAVAILABLE`，`error.category=GENERATION`，`error.retryable=true`，`error.messageKey=api.error.generation.generationServiceUnavailable`，HTTP **503**。英文 message 对齐契约（Document generation service is temporarily unavailable.）。**禁止**使用 `TEMPLATE_VALIDATION_FAILED` / HTTP 422。 | contract-outline + DEF-LRP-D6-001 |
| **D01A-C2** | **Timeout → `GENERATION_TIMEOUT`：** cause chain 中出现 `java.util.concurrent.TimeoutException`（含 Resilience4j TimeLimiter / future timeout 经 mapper 归一的同类）且**不属于**已单独映射的业务异常时，API 必须返回：`error.code=GENERATION_TIMEOUT`，`error.category=GENERATION`，`error.retryable=true`，`error.messageKey=api.error.generation.generationTimeout`，HTTP **504**。 | contract-outline |
| **D01A-C3** | **Bulkhead full → `GENERATION_SERVICE_UNAVAILABLE`：** cause chain 中出现 Resilience4j `BulkheadFullException`（若运行时出现）时，映射同 **D01A-C1**（503 / `GENERATION_SERVICE_UNAVAILABLE` / retryable=true）。本叶**不要求**新增 bulkhead 配置；但 mapper **必须**识别该类，避免落入错误默认或泄露类名。 | handoff scope + envelope stability |
| **D01A-C4** | **不再经 `TemplateValidationException` 承载服务不可用：** mapper **不得**再把 CB/timeout/bulkhead/未知 resilience 失败包装为 `TemplateValidationException`（该类型在 advice 层固定为 `TEMPLATE_VALIDATION_FAILED`）。实现可选用专用 domain 异常 + advice/factory，或等价路径；验收只看对外 envelope。 | DEF-LRP-D6-001 |
| **D01A-C5** | **Legacy messageKey 退役（对外）：** 对外成功路径错误键以契约为准：`api.error.generation.generationServiceUnavailable` / `api.error.generation.generationTimeout`。既有 `api.error.generation.serviceUnavailable` **不得**再作为 CB/timeout 对外 messageKey（可保留 bundle 兼容期，但新映射与测试断言契约键）。 | contract-outline vs `messages_en.properties` |
| **D01A-C6** | **透传业务 / 渲染 / 池容量异常：** mapper 遇到 `TemplateValidationException`、`RenderingOperationException`、`PdfConversionCapacityExceededException`（及 cause 中同类）**原样抛出/返回**，不得改写成 service-unavailable / timeout。 | 现有 `ResilienceFailureMapper` + pool 路径 |
| **D01A-C7** | **默认 / 未知失败：** cause chain 无法识别且非 Runtime 业务异常时，对外仍为 **`GENERATION_SERVICE_UNAVAILABLE`**（503, retryable=true），**不是** `TEMPLATE_VALIDATION_FAILED`，**不是** `INTERNAL_ERROR`（避免把临时不可用升级为 500，除非另有已确认安全要求——本叶确认用 503）。 | DEF-LRP-D6-001 精神 + contract 503 族 |
| **D01A-C8** | **禁止泄露 internals：** 对外 `error.message`、日志对调用方可见的字段、以及任何写入 envelope 的 details **不得**包含：`CallNotPermittedException`、`BulkheadFullException`、`TimeoutException` 类名、circuit/breaker/bulkhead 实例名、线程名、堆栈片段。服务端日志可保留诊断信息。 | bank-grade envelope |
| **D01A-C9** | **PDF 池有界（本叶冻结时 NFR 默认）：** `conversion-pool-size` 默认 **2**；`conversion-queue-capacity` 默认 **0**；core=max=poolSize；`AbortPolicy`。**禁止**无界队列。运维可有界调大，产品默认当时保持 fail-fast。 | NFR + SOR-P03 + F4-C5 |
| **D01A-C9 supersession** | **IBL-B2 / #114（2026-07-19）修订产品默认**为 pool **4** / queue **8**（仍 AbortPolicy、仍禁止无界）。taxonomy / 饱和码（D01A-C10…C12）不变。权威容量计划：[pdf-conversion-capacity-plan.md](../operations/pdf-conversion-capacity-plan.md)；行为 SoT：[ibl-b2-pdf-conversion-capacity.md](./ibl-b2-pdf-conversion-capacity.md)。 | IBL-B2 B2-C2 |
| **D01A-C10** | **池饱和 fail-closed：** 当池 active 已满且队列无剩余容量（含 queueCapacity=0）时，同步 PDF 转换路径立即失败，返回：`error.code=PDF_CONVERSION_CAPACITY_EXCEEDED`，`error.category=GENERATION`，`error.retryable=true`，`error.messageKey=api.error.generation.pdfConversionCapacityExceeded`，HTTP **503**。不得无限阻塞 servlet 线程等待池位。 | 现有 `PdfConversionOffloadSupport` / advice |
| **D01A-C11** | **池拒绝可观测：** 饱和拒绝须继续递增 Micrometer `docgen.pdf.conversion.pool.rejections`（既有 `PdfConversionPoolRejectionMetrics`）；池 gauge（active / queue size / remaining）保持注册。本叶不改 alert 阈值语义。 | LR-D3 / SOR-P03 |
| **D01A-C12** | **容量 vs resilience 分离：** 同一并发场景下，池 Abort/预检饱和 → **仅** `PDF_CONVERSION_CAPACITY_EXCEEDED`；CB open → **仅** `GENERATION_SERVICE_UNAVAILABLE`；generation timeout → **仅** `GENERATION_TIMEOUT`。调用方 / load-smoke 可用 `error.code` 区分（不得再混为 `TEMPLATE_VALIDATION_FAILED`）。 | DEF-LRP-D6-001 + load-smoke harness |
| **D01A-C13** | **契约文档对齐（本叶最小）：** 将运行时已确认的 `PDF_CONVERSION_CAPACITY_EXCEEDED` 补入 OpenAPI `ErrorCode` 枚举与 contract-outline 错误码表（category `GENERATION`，retryable true，HTTP 503）。**不**借此改其它错误码族。 | 代码已有 + docs gap |
| **D01A-C14** | **不扩大转换语义：** 本叶不改 LibreOffice profile 隔离、分页 delta、PDF/A、加密互斥、异步 202 路径。池 offload + timeout buffer 行为以现有 `PdfConversionOffloadSupport` 为准；转换**内容**失败仍走既有 `pdfConversionFailed` / `RENDERING_FAILED` 路径（非本叶 taxonomy 主角）。 | handoff IN/OUT |
| **D01A-C15** | **`frontend_ui_in_scope=false`：** 无 Vue / i18n UI / Playwright / UIUX；Done 主证据 = 后端单测/契约断言 + `mvn verify`。 | handoff |
| **D01A-C16** | **治理冻结：** 不翻转 #3b；不标 #5a GO；不宣称 go-live；不激活 CD-3；Formal phase 保持 None。 | handoff |

### 4.1 映射表（对外验收权威）

| 触发（cause chain） | HTTP | `error.code` | `error.category` | `retryable` | `messageKey` |
| --- | --- | --- | --- | --- | --- |
| `CallNotPermittedException` / circuit open | 503 | `GENERATION_SERVICE_UNAVAILABLE` | `GENERATION` | `true` | `api.error.generation.generationServiceUnavailable` |
| `BulkheadFullException` | 503 | `GENERATION_SERVICE_UNAVAILABLE` | `GENERATION` | `true` | `api.error.generation.generationServiceUnavailable` |
| `TimeoutException`（resilience / mapped timeout） | 504 | `GENERATION_TIMEOUT` | `GENERATION` | `true` | `api.error.generation.generationTimeout` |
| PDF pool saturated / `RejectedExecutionException` → capacity | 503 | `PDF_CONVERSION_CAPACITY_EXCEEDED` | `GENERATION` | `true` | `api.error.generation.pdfConversionCapacityExceeded` |
| 真实模板校验失败（非 resilience） | 422 | `TEMPLATE_VALIDATION_FAILED` | `TEMPLATE` | per existing | 业务 messageKey（不变） |

---

## 5. 前置条件

- Resilience4j 已用于 PDF conversion / MinIO 等路径（`ResilienceSupport` + CB/Retry）。
- PDF 池与 `PdfConversionCapacityExceededException` → 503 路径已存在（SOR-P03）。
- 契约表已列出 `GENERATION_SERVICE_UNAVAILABLE` / `GENERATION_TIMEOUT`。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 同步 runtime 生成请求走受 `ResilienceSupport` / CB 保护的 PDF（或存储）路径，且 CB open / timeout / bulkhead full | 错误经 `ResilienceFailureMapper` |
| T2 | 同步 PDF 转换在 `pdfConversionExecutor` 饱和时入队/提交 | 池 fail-closed |
| T3 | 自动化测试直接调用 `ResilienceFailureMapper.map(...)` / offload 饱和替身 | TDD Red/Green |

---

## 7. Primary journey

1. 调用方发起合法同步生成（含 PDF）或测试注入 resilience 失败。
2. 系统在 resilience 装饰层捕获失败，进入 `ResilienceFailureMapper`（或池饱和短路）。
3. 系统按 §4.1 发出统一 error envelope；不泄露 internals。
4. 池饱和时同时记录 rejection 指标；调用方可按 `retryable=true` 退避重试。
5. 真实业务校验失败仍走原 `TEMPLATE_VALIDATION_FAILED` 路径，不被本叶改写。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| CB open | 503 `GENERATION_SERVICE_UNAVAILABLE` |
| Bulkhead full | 503 `GENERATION_SERVICE_UNAVAILABLE` |
| Resilience / mapped timeout | 504 `GENERATION_TIMEOUT` |
| PDF pool saturated | 503 `PDF_CONVERSION_CAPACITY_EXCEEDED` + rejection counter++ |
| 业务 `TemplateValidationException` | 422 `TEMPLATE_VALIDATION_FAILED`（不变） |
| 成功转换（池有容量且 CB closed） | 既有成功路径（本叶不改成功体） |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-PRR-D01A-001 — Circuit open 不再标为模板校验失败

**Given** PDF（或经 `ResilienceSupport` 的）路径上 circuit breaker 处于 open，下一次调用产生 `CallNotPermittedException`  
**When** 失败经 `ResilienceFailureMapper` 并到达 API error envelope  
**Then** HTTP **503**；`error.code=GENERATION_SERVICE_UNAVAILABLE`；`error.category=GENERATION`；`error.retryable=true`；`error.messageKey=api.error.generation.generationServiceUnavailable`  
**And** `error.code` **不是** `TEMPLATE_VALIDATION_FAILED`；HTTP **不是** 422

### BDD-PRR-D01A-002 — Timeout 映射为 GENERATION_TIMEOUT

**Given** cause chain 含 `TimeoutException`（无更高优先级业务异常）  
**When** `ResilienceFailureMapper.map`（或经 `ResilienceSupport`）处理该失败  
**Then** 对外 HTTP **504**；`error.code=GENERATION_TIMEOUT`；`error.category=GENERATION`；`error.retryable=true`；`error.messageKey=api.error.generation.generationTimeout`

### BDD-PRR-D01A-003 — BulkheadFullException 映射为服务不可用

**Given** cause 为 Resilience4j `BulkheadFullException`（或 cause chain 含该类）  
**When** mapper 处理该失败  
**Then** 对外同 **D01A-C1 / D01A-001**（503 / `GENERATION_SERVICE_UNAVAILABLE` / retryable=true）  
**And** envelope 文本不含 `BulkheadFullException` 字样

### BDD-PRR-D01A-004 — 业务校验异常透传

**Given** decorated supplier 抛出 `TemplateValidationException("api.error.generation.pdfConversionFailed")`（或其它业务 messageKey）  
**When** 经 `ResilienceSupport.execute`  
**Then** 仍为该 `TemplateValidationException` / 既有业务 envelope；**不得**改写为 `GENERATION_SERVICE_UNAVAILABLE`

### BDD-PRR-D01A-005 — RenderingOperationException 透传

**Given** cause 为 `RenderingOperationException`  
**When** mapper 处理  
**Then** 返回/抛出同一 `RenderingOperationException`（既有 rendering advice 语义不变）

### BDD-PRR-D01A-006 — 池容量异常不被 mapper 改写

**Given** cause 为 `PdfConversionCapacityExceededException`（或经 offload 包装后 cause 为该类）  
**When** 失败冒泡到 API  
**Then** HTTP **503**；`error.code=PDF_CONVERSION_CAPACITY_EXCEEDED`；`retryable=true`；`messageKey=api.error.generation.pdfConversionCapacityExceeded`  
**And** **不是** `GENERATION_SERVICE_UNAVAILABLE` / `TEMPLATE_VALIDATION_FAILED`

### BDD-PRR-D01A-007 — PDF 池饱和 fail-closed

**Given** `pdfConversionExecutor` 配置为生产等价有界池（默认 size=2、queueCapacity=0，或测试中更小但有界），且池已满  
**When** 再提交一次同步 PDF 转换 offload  
**Then** 立即以 `PdfConversionCapacityExceededException` / API **D01A-006** 失败  
**And** 不无限期阻塞等待池位；`docgen.pdf.conversion.pool.rejections` 计数增加

### BDD-PRR-D01A-008 — 默认池配置保持有界 fail-fast

**Given** 未覆盖 env 的默认 `DocgenRenderingProperties` / `application.yml`  
**When** 读取 `conversion-pool-size` 与 `conversion-queue-capacity`  
**Then** 分别为 **2** 与 **0**  
**And** `PdfConversionExecutorConfig` 使用 AbortPolicy（或测试断言拒绝策略为 fail-fast，非无界排队）

### BDD-PRR-D01A-009 — 禁止无界池作为本叶「修复」

**Given** 本叶变更集  
**When** 审查默认 `conversion-queue-capacity` 与 executor 队列实现  
**Then** 默认不得变为无界队列；不得删除饱和预检 / AbortPolicy 而导致请求在池满时无界堆积

### BDD-PRR-D01A-010 — 对外消息不泄露 internals

**Given** 任一 D01A-001…003 失败路径  
**When** 检查 `error.message` 与 `error.messageKey`  
**Then** 仅为契约安全英文句 + 稳定 messageKey  
**And** 不包含 Resilience4j 异常类名、breaker/bulkhead 名称、堆栈

### BDD-PRR-D01A-011 — 未知 resilience 失败默认服务不可用

**Given** mapper 收到无法识别的受检失败（非业务 Runtime 透传路径）且需归一  
**When** 映射为 API envelope  
**Then** `GENERATION_SERVICE_UNAVAILABLE` / 503 / retryable=true（**不是** `TEMPLATE_VALIDATION_FAILED`）

### BDD-PRR-D01A-012 — 契约文档列出容量码

**Given** 本叶完成  
**When** 检查 `docs/api/openapi-v1.yaml` 与 `docs/api/contract-outline.md`  
**Then** `PDF_CONVERSION_CAPACITY_EXCEEDED` 出现在 ErrorCode / 错误码表，并标明 category `GENERATION`、retryable true、HTTP 503 适用

### BDD-PRR-D01A-013 — 治理非目标不被本叶关闭

**Given** 本叶 Done  
**When** 检查 launch checklist / 计划状态  
**Then** **#3b** 仍为 **CONDITIONAL**（非 GO）；**#5a** 非因本叶变为 GO；不宣称 go-live；Formal phase 仍为 None

---

## 10. Boundary / exception behavior

| 边界 | 行为 |
| --- | --- |
| Cause chain 嵌套（Retry 包装 CB） | 沿 cause 链识别 **D01A-C1…C3**；先匹配专用类型再默认 |
| 池饱和与 CB open 同时可能 | 以**实际抛出的异常类型**为准；不得把 capacity 改标为 serviceUnavailable |
| MinIO 路径经同一 mapper | 适用同一 taxonomy（本叶不单独发明存储错误码；存储业务异常若已是 Runtime 透传则保持） |
| 转换内容失败（soffice 非零等） | 仍走既有 `pdfConversionFailed` / rendering 失败语义；**不**强制改为 `GENERATION_SERVICE_UNAVAILABLE` |
| 授权失败 | 本叶不改变；fail-closed 授权优先于本 taxonomy |
| FE / E2E | N/A |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 单元测试 | `ResilienceFailureMapperTest`（及 advice/factory 集成测）覆盖 D01A-001…005、010、011 |
| 池测试 | 既有/扩展 `PdfConversionOffloadSupportTest` 等覆盖 D01A-006…009 |
| API envelope | HTTP + `error.code` / `category` / `retryable` / `messageKey` |
| Metrics | `docgen.pdf.conversion.pool.rejections` 在饱和时 > 0 |
| 契约 | OpenAPI + contract-outline 含 `PDF_CONVERSION_CAPACITY_EXCEEDED` |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` GREEN；FE gates N/A |
| 非证据 | 不得以「调大池/削弱 CB 使 load-smoke 全绿」作为本叶 Done 条件（与 D6 triage 一致） |

---

## 12. Traceability

| 项 | 引用 |
| --- | --- |
| Task Master | **#104**（leaf lead of split Wave D） |
| Defect | [DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) |
| API contract | [contract-outline.md](../api/contract-outline.md) `GENERATION_SERVICE_UNAVAILABLE` / `GENERATION_TIMEOUT`；OpenAPI ErrorCode |
| NFR / pool | [non-functional-requirements.md](../requirements/non-functional-requirements.md) `conversion-pool-*`；SOR-P03 |
| Prior BDD | [core-fortress-f4-production-rendering-hardening.md](./core-fortress-f4-production-rendering-hardening.md) F4-A2 / 池饱和 |
| Load smoke | [lrp-d6-load-smoke.md](./lrp-d6-load-smoke.md)（观测输入；本叶修 taxonomy，不重开 D6） |
| Checklist | **#3b** CONDITIONAL；**#5a** 不因本叶 GO |

---

## 13. OUT of scope（显式）

- Actuator exposure hardening  
- nginx CSP  
- Dashboard summary API  
- Doc hygiene（ADR-0044 notes / listAll / knip）  
- 调高默认池大小或削弱 circuit breaker 阈值以「刷绿」并发 smoke  
- 发明无界 PDF 队列  
- 前端 UI / Playwright / UIUX  
- go-live / #3b GO / #5a GO / CD-3 / Formal phase 激活  

---

## 14. BDD readiness

| Field | Value |
| --- | --- |
| **bdd_readiness** | `ready` |
| **open_questions** | _（无阻塞项）_ |
| **owning_doc** | `docs/behavior/prod-ops-resilience-pdf-pool.md`（本文件；worktree 路径） |
| **task_ids** | `["104"]` |
| **acceptance_scenario_ids** | `BDD-PRR-D01A-001` … `BDD-PRR-D01A-013` |
| **frontend_ui_in_scope** | `false` |
| **next** | `plan-orchestrator` → backend/rendering implementers（TDD Red first） |

### 非阻塞实现备注（非 pending questions）

- 专用异常类型命名与 advice 挂载点由实现选择；验收以 §4.1 envelope 为准。  
- 本叶可不引入新的 Resilience4j Bulkhead Bean；仅要求 mapper 识别 `BulkheadFullException`。  
- `ApiErrorCodes` 需新增 `GENERATION_SERVICE_UNAVAILABLE` / `GENERATION_TIMEOUT` 常量（若尚未存在）以匹配契约——属实现细节，行为已由契约确认。
