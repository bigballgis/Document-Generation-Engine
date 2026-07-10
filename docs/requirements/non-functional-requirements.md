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

### LR-D5 NFR 数值提案（pending proposal — 待用户确认）

以下数值为 LR-D5 提案基线，按文档即代码宪法仅作 pending proposal 记录，**未确认**。
确认前系统不会据其阻断或告警。用户确认后，这些数值将移至上方「已确认」区段并据此
配置告警阈值（见 `deploy/observability/prometheus-alerts.yaml`）。

| 维度 | 提案基线 | 说明 |
| --- | --- | --- |
| 同步生成 p95 延迟 | ≤ 3s | 单文档 DOCX→PDF 生成（不含下载），99% 请求低于此值 |
| 预览 SSE 首事件延迟 | ≤ 5s | 从发起预览到首个 progress 事件到达浏览器 |
| 并发生成能力 | ≥ 20 | 同一时刻在途的同步生成请求数（PDF 转换池 size=2 时的稳态吞吐） |
| 模板容量 | ≥ 500 / 租户 | 单租户可管理的模板数量上限，列表查询 p95 ≤ 2s |
| 批量生成上限 | ≤ 50 / 请求 | 单次批量请求的最大条目数 |
| 下载 URL 有效期 | 15 分钟 | 已确认（见 P10），此处仅归口 |

**验证方式：** LR-D6 负载冒烟基线（≥20 并发同步 + SSE 预览 on Docker）将测量上述前
三项，测量结果决定提案是否上调或下调。确认前的所有 NFR 数值不进入 SLA 承诺。

## 已确认：生产渲染（CORE-FORTRESS F4 / LR-A7 子集）

> **分页承诺边界：** LibreOffice PDF 与 Microsoft Word **不承诺逐页一致**。本节仅记录
> 可重复测量的页数 delta 基线与提案预算；运行时强制告警/阻断留待 ADR-0042 用户确认后启用。

### 配置项（`docgen.rendering` / 环境变量）

| 属性 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `conversion-pool-size` | `PDF_CONVERSION_POOL_SIZE` | `2` | PDF 转换 bounded pool 大小（core=max） |
| `conversion-timeout-seconds` | `PDF_CONVERSION_TIMEOUT_SECONDS` | `120` | 单次转换超时（秒） |
| `conversion-queue-capacity` | `PDF_CONVERSION_QUEUE_CAPACITY` | `0` | 队列容量；`0` = fail-fast（SOR-P03） |
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
