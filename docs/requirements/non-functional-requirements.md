# 非功能需求

本文档用于归口已确认的非功能需求，包括安全、隐私、审计、可靠性、性能、可观测性、兼容性、可维护性、易用性和可访问性。功能需求仍归 [requirements-plan.md](requirements-plan.md) 和 [PRD.md](../product/PRD.md)；技术选型仍归 [technology-stack-decisions.md](../architecture/technology-stack-decisions.md) 和相关 ADR。

## 相关文档

- [文档索引](../README.md)
- [原始需求记录](requirements-plan.md)
- [产品需求说明](../product/PRD.md)
- [领域模型](../domain/domain-model.md)
- [权限矩阵](../security/permission-matrix.md)
- [技术选型日志](../architecture/technology-stack-decisions.md)
- [文档治理规则](../governance.md)

## 归口原则

- 只记录跨功能通用、面向质量属性和运行约束的需求。
- 不记录技术栈、框架、数据库或实现库的选择。
- 不记录单一功能流程中的业务步骤；这类内容仍归功能需求文档。
- 已确认的非功能需求必须与功能需求、产品行为和技术选型分开维护。

## 已确认：安全与隐私

- API 采用 API 凭证 + AD Group 双重认证授权。
- API 调用、授权、审计、错误响应、契约展示和管理界面必须执行脱敏规则。
- API 凭证 secret、DOCX/PDF 加密密码、完整请求体、完整下载地址、完整 AD Group 成员和未授权组详情不得明文持久化或展示。
- DOCX/PDF 动态加密参数、幂等摘要、请求摘要、审计摘要和配置差异摘要必须受控表达。
- 授权失败和依赖失败必须 fail-closed，且不得泄露未授权资源细节。

## 已确认：可靠性与可用性

- API 需要幂等、重放和冲突处理能力。
- 生成、批量生成、异步任务、下载取文件和 default 路由切换都必须可审计、可追溯。
- 生成结果、异步任务和下载地址都必须有明确保留期和过期行为。
- 失败与重试策略必须区分业务失败和临时系统失败。

## 已确认：性能与容量

- API 需支持批量生成与异步生成。
- 批量上限、下载有效期、缓存 TTL、幂等保留期和任务保留期均需显式控制。
- 查询、下载、生成、批量和契约查看的调用路径应支持稳定的超时、重试和降级边界。

## 已确认：可观测性与审计

- 所有文档生成类 API 必须记录审计。
- 审计需要覆盖调用、授权、幂等、生成、批量、下载、API 管理配置变更和凭证生命周期事件。
- TraceId、requestId、idempotencyKey、auditId 和配置版本必须可追踪。
- 日志、审计和管理界面不得保存敏感明文。

## 已确认：易用性与交互质量

- 模板创作需要分步向导、受控富文本、样例校验、差异摘要、保真警告和二次确认。
- API 契约查看需要提供非敏感版本对比、调用示例、错误码解释和策略摘要。
- API 管理需要提供影响预览、硬阻断、警告和立即生效确认。
- 后台治理与管理界面需要提供统一的双主题体验基线：支持 `REDBC` 红色主题和 `GREENBC` 绿色主题运行时切换，并保持跨页面一致的主题令牌系统。
- 后台治理与管理界面采用英文为主语言、白色背景和经典 OA 桌面优先布局；信息密度应可读、舒展，不得以过度紧凑换取所谓企业感。
- 品牌 logo 资产必须通过统一品牌资产配置层接入，并保持主题切换、logo 切换和品牌色切换的一致性；不得在页面中散落复制 logo 文件或自行变体化处理。

## 已确认：兼容性与演进

- API 路径、版本、错误码、枚举值和字段命名需要稳定且可演进。
- 模板、发布版本、条款/内容模块和 API 管理配置需要支持受控回滚与历史追踪。
- 技术实现可以演进，但不得破坏已确认的文档与契约边界。

## 待确认问题

### LR-D5 NFR 数值提案（proposed — awaiting confirmation）

> **Governance:** Every value in this section is **«proposed — awaiting confirmation»**.
> None of these numbers are confirmed requirements, contractual SLAs, or alert-enforcement
> thresholds. Do **not** move them into «已确认» until the user explicitly confirms.
> Owner: doc-keeper (LR-D5 / Task Master #38). Authored: 2026-07-12.
> Fed by LR-D6 evidence (merge `56383eb`); smoke numbers remain **inputs**, not SLOs.

**Related (do not conflate):** Role task-time budgets live in
[usability-review.md](../product/usability-review.md) §CD-UX-T01 (non-SLA UX draft).
This section covers **generation / capacity / availability** NFRs only.
F8 draft alert thresholds:
[CORE-FORTRESS-f8 §8](../plan/detail/CORE-FORTRESS-f8-observability-slo-dr.md#8-slo-target-reference-draft--lr-d5-pending)
remain `draft: true` until confirmation here.

#### Shared environment assumptions (LR-D6 measurement context)

| Assumption | Value |
| --- | --- |
| Host | Windows host; Docker Desktop acceptance stack |
| Ports | Backend `8080` / Management UI `4173` |
| Stack / git | `stackVersion` **a262706** (see evidence JSON) |
| Template | `CORP-FOL-OFFER` (FOL demo) |
| Harness | JUnit load-smoke under `backend/src/test/.../runtime/loadsmoke/` (no vendor APM) |
| Metrics (ops) | Micrometer `docgen.generation.duration` / `docgen.pdf.conversion.*` (F8); Prometheus rules stay draft |
| Evidence | Historical LR-D6: [latest-summary.json](../plan/evidence/lrp-d6-load-smoke/latest-summary.json); [TRIAGE-pdf-422.md](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) (`DEF-LRP-D6-001` → **CLOSED**). IBL-B2 re-smoke: [ibl-b2-pdf-capacity/](../plan/evidence/ibl-b2-pdf-capacity/) |

**Evidence status legend:** `pre-measurement` = industry-norm / planning proposal, not yet measured (or not measured for that dimension); `measured-input` = LR-D6 (or named adjacent) observation used as proposal input only.

#### Proposal table

| Dimension | Proposed value (awaiting confirmation) | Evidence status | Measurement method | Source / notes | Launch gate (LR-E2) vs post-launch |
| --- | --- | --- | --- | --- | --- |
| Sync generation p95 — **with PDF** (end-to-end sync, excl. download) | **Interim observed envelope:** p95 ≈ **15939 ms**, p99 ≈ **16065 ms** under D6 FOL concurrent mix — propose confirming an ops baseline in this band **or** an explicit post-remediation target after `DEF-LRP-D6-001`. **Superseded pre-measurement industry-norm:** ≤ **3 s** — **not supported** by current smoke. F8 draft ≤10 s e2e likewise **not supported** by this FOL concurrent run. | **measured-input** (mixed-format success sample; see note) | LR-D6 Scenario A harness percentiles on successful responses | [latest-summary.json](../plan/evidence/lrp-d6-load-smoke/latest-summary.json) `scenarioA.p95Ms` / `p99Ms`; n=20 requested, success=12 | **Launch-gate:** User confirms either (a) interim ~16 s p95 envelope for FOL+PDF concurrent Docker smoke, or (b) remediation of concurrent-PDF path before treating ≤3 s / ≤10 s as launch bar. **Post-launch:** Tighten aspirational ≤3 s / ≤10 s after resilience/PDF-pool work. |
| Sync generation p95 — **DOCX only** (no PDF conversion) | **Pre-measurement aspirational:** ≤ **3 s** (industry-norm planning). **Measured adjacent:** In Scenario A all **DOCX** requests succeeded (10/10); format-split latency **not** published separately in `latest-summary.json` — do not invent DOCX-only p95. | **pre-measurement** (latency); **measured-input** (DOCX success only) | Re-run harness with DOCX-only cohort **or** filter Micrometer `docgen.generation.duration` by format when available | D6: DOCX OK vs PDF failures in [DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) | **Post-launch** tuning preferred until DOCX-only percentile is measured; launch may require only “DOCX sync succeeds under concurrency” honesty note. |
| SSE first-event latency | ≤ **5 s** from preview start to first progress/event in browser | **pre-measurement** | Playwright timing on preview journey **or** harness timestamp of first SSE event (not captured in D6 Scenario B summary) | Prior LR-D5 table; D6 did **not** record first-event ms | **Post-launch** (unless LR-E1 browser SSE proof expands measurement). |
| SSE stream integrity (no silent drop) | **Zero dropped streams** at ≥ **5** parallel preview streams; all streams reach a terminal event | **measured-input** | LR-D6 Scenario B | [latest-summary.json](../plan/evidence/lrp-d6-load-smoke/latest-summary.json) `scenarioB`: started=5, terminal=5, dropped=0 | **Launch-gate (LR-E2 / LR-E1 adjacency):** Preserve zero silent-drop bar; re-evidence if SSE path changes. |
| Concurrent sync generation capacity | Requested concurrency **≥ 20** in-flight sync generations. **Historical LR-D6 (pool=2/queue=0):** n=20, success=12, **errorRate=0.4** — concurrent PDF → `TEMPLATE_VALIDATION_FAILED` ([DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md)). **IBL-B2 (pool=4/queue=8; #114 Done):** Scenario A n=20 success=**20** errors=**0**; PDF failures **0/10**; `poolRejections=0` — [ibl-b2-pdf-capacity/](../plan/evidence/ibl-b2-pdf-capacity/). DEF **CLOSED**. Do **not** invent confirmed concurrent-PDF SLO from this smoke; p95 observed under B2 is higher (~41 s) — still **not** an NFR confirmation. | **measured-input** | LR-D6 + IBL-B2 Scenario A | Historical triage + IBL-B2 evidence; do not tune thresholds to green the smoke | **Launch-gate:** `DEF-LRP-D6-001` disposition **CLOSED** (IBL-B2). User may still confirm interim latency envelope separately. **Post-launch:** Steady-state concurrent PDF SLO after ops baseline confirmation. |
| Availability target | ≥ **99.5%** monthly for management API + sync generation path (planning) | **pre-measurement** | Platform `healthz` / structured `readyz` (Postgres-gated per SOR-O06) + deploy/uptime records — **no** vendor APM invented | Industry-norm planning; not measured in D6 | **Post-launch** (ops maturity); launch checklist may only require health endpoints green. |
| Max concurrent management sessions | ≤ **50** concurrent authenticated UI sessions per acceptance/single-host class (planning) | **pre-measurement** | Session/store metrics or controlled Playwright multi-session smoke (not run in D6) | Planning only | **Post-launch** capacity planning. |
| Max concurrent SSE connections | ≥ **5** parallel preview streams (integrity proven); higher caps (e.g. 20+) **unmeasured** | **measured-input** (≥5 integrity); **pre-measurement** (higher caps) | LR-D6 Scenario B for ≥5; scale tests later via same harness class | Scenario B zero drops | **Launch-gate:** ≥5 zero-drop integrity. **Post-launch:** Raise cap after measured soak. |
| Template capacity / catalog list p95 | ≥ **500** templates per tenant; catalog list p95 ≤ **2 s** (planning) | **pre-measurement** (as NFR); adjacent UX: LR-C5 catalog pagination evidence exists but is **not** promoted here as SLA | Catalog API timing / Playwright list journeys | Keep pending; LR-C5 p95 ~75 ms is feature evidence, not confirmed NFR | **Post-launch** / capacity; not LR-E2 blocker unless user elevates. |
| Batch generation limit | ≤ **50** items per batch request | **pre-measurement** | Contract/config review + batch API tests | Existing API policy surface; unmeasured as NFR | **Post-launch** unless product confirms as hard product limit earlier. |
| Download URL TTL | **15 minutes** | **already confirmed (P10)** — listed only for discoverability; **not** a LR-D5 proposal | — | P10 / confirmed reliability | N/A (confirmed elsewhere) |

**Percentile note (Scenario A):** Reported p50/p95/p99 are over the **12 successful** responses in a mixed DOCX/PDF cohort. They are **not** a clean “PDF-only p95” and **not** a DOCX-only p95. Use them as concurrent FOL smoke envelope only.

**Confirmation gate:** Until user confirmation, `deploy/observability/prometheus-alerts.yaml` keeps `draft: true`; F8 §8 draft thresholds that still cite ≤3 s / ≤10 s are **stale relative to D6 FOL concurrent smoke** and must be revised together with this table when confirmed. LR-D3 ops draft table + runbook annotation targets: [runbook § Draft alert thresholds](../operations/runbook.md#draft-alert-thresholds-lrd3--not-confirmed-slos) · [deploy/observability/README.md](../../deploy/observability/README.md).

## 已确认：生产渲染（CORE-FORTRESS F4 / LR-A7 子集）

> **分页承诺边界：** LibreOffice PDF 与 Microsoft Word **不承诺逐页一致**。本节仅记录
> 可重复测量的页数 delta 基线与提案预算；运行时强制告警/阻断留待 ADR-0042 用户确认后启用。

### 配置项（`docgen.rendering` / 环境变量）

| 属性 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `conversion-pool-size` | `PDF_CONVERSION_POOL_SIZE` | `4` | PDF 转换 bounded pool 大小（core=max）；**IBL-B2** 修订（原 D01A/F4 默认 `2`） |
| `conversion-timeout-seconds` | `PDF_CONVERSION_TIMEOUT_SECONDS` | `120` | 单次转换超时（秒） |
| `conversion-queue-capacity` | `PDF_CONVERSION_QUEUE_CAPACITY` | `8` | 有界队列；饱和仍 AbortPolicy fail-closed（SOR-P03）；**IBL-B2** 修订（原默认 `0` fail-fast）。运维可设回 `0`。**不是** confirmed 并发 SLO — 见 [pdf-conversion-capacity-plan.md](../operations/pdf-conversion-capacity-plan.md) |
| `pagination-delta-budget-pages` | `PAGINATION_DELTA_BUDGET_PAGES` | `1` | 分页 delta 提案预算（页）；**pending ADR-0042** |

### 分页语料表（P23 demo masters — LR-A7 测量基线）

测量日期、栈版本/git SHA 在每次复测时更新。Docker PDF 页数经 runtime `SYNC_STREAM` + host `pypdf` 计页（2026-07-10 / `9a40b48`）。

| # | Demo package | Master asset / externalId | 业务类型 | Word 页数 | Docker PDF 页数 | Delta | 测量日期 | Git SHA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | demo-credit-limit | `DEMO-CREDIT-LIMIT-CONFIRM` | 授信额度通知 | n/a[^word-baseline] | 6 | n/a[^word-baseline] | 2026-07-10 | `9a40b48` |
| 2 | demo-mortgage | `DEMO-MORTGAGE-APPROVAL` | 按揭批核 | n/a[^word-baseline] | 6 | n/a[^word-baseline] | 2026-07-10 | `9a40b48` |
| 3 | demo-trade-lc | `DEMO-TRADE-LC-NOTICE` | 贸易信用证 | n/a[^word-baseline] | 9 | n/a[^word-baseline] | 2026-07-10 | `9a40b48` |
| 4 | demo-collection | `DEMO-OVERDUE-COLLECTION` | 逾期催收 | n/a[^word-baseline] | 8 | n/a[^word-baseline] | 2026-07-10 | `9a40b48` |
| 5 | demo-retail-account | `DEMO-RETAIL-ACCOUNT-OPEN` | 零售开户 | n/a[^word-baseline] | 8 | n/a[^word-baseline] | 2026-07-10 | `9a40b48` |
| 6 | demo-fol（可选） | `CORP-FOL-OFFER` | 批发 FOL | n/a[^word-baseline] | 86 | n/a[^word-baseline] | 2026-07-10 | `9a40b48` |

[^word-baseline]: **Word 基线方法 = `ms-word-unavailable-on-host`。** 测量主机无 Microsoft Word；Word 页数与 Word-vs-LO delta **刻意保持 n/a**，禁止虚构数字。真 Word 基线须在装有 Word 的主机复测后回填。

**汇总（必测 5 封）：** max Docker PDF pages = **9**；median = **8**；max/median Word delta = **n/a**（Word 不可用）。

**证据：** 精简摘要 [`docs/evidence/lrp-a7-pagination/`](../evidence/lrp-a7-pagination/)；完整 PDF 工件见 worktree `.tmp/evidence/lrp-a7-pagination/`（不入库大二进制）。

**提案预算（pending user confirmation / ADR-0042）：** 参考 `paginationDeltaBudgetPages=1`；
Word-vs-LO delta 确认前 **不** 将预算升为 Accepted / 运行时强制。

### 可重复测量规程

1. **部署栈：** 自仓库根目录执行 `.\scripts\docker-deploy.ps1`（或 `-FOLDemo` 加载 demo 数据）。
2. **生成 PDF：** 对语料表中每个 master，经平台生成流程产出 PDF（管理 UI 预览/批量测试或 runtime API）。
3. **计页：** 使用 PDFBox（`PDDocument.getNumberOfPages()`）或 `pdfinfo` 读取 Docker 栈 PDF 页数。
4. **Word 基线：** 在同一 master DOCX 上用 Microsoft Word 打开，人工记录页数（不要求 Word 自动化批处理）。若主机无 Word，记录 `method=ms-word-unavailable-on-host` 并将 Word/delta 列填 **n/a**（禁止虚构）。
5. **记录 delta：** 仅当 Word 页数可得时计算 `delta = |pdfPages - wordPages|`；允许 ±0 页读数误差（单页文档边界）。
6. **更新表格：** 填写测量日期、`git rev-parse --short HEAD`、栈版本（`docker compose images`）。
7. **汇总：** 计算 max/median delta，与提案预算对比；结论写入 ADR-0042 草案（**不**在 F4 运行时阻断）。

**后端辅助：** `mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=LibreOfficeParallelConversionIntegrationTest`
在含 `soffice` 的环境验证 ≥4 路并发转换；无 `soffice` 时 skip（与 `RenderingFontSmokeTest` 同模式）。
