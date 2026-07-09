# BDD 行为规格：CORE-FORTRESS Phase F4 — 生产渲染加固

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-09  
**BDD ID**: `BDD-CORE-FORTRESS-F4-001`  
**来源**: CORE-FORTRESS 纲领 F4 范围 + LRP Wave LR-A 剩余缺口（LR-A1 部分完成、LR-A7 子集）+ 用户确认（2026-07-09）

---

## 1. 概述

F4 将 **LibreOffice 生产转换路径** 从「能跑」加固到「可并发、可配置、可度量」——完成 LR-A1 并行回归验收、补齐 profile 隔离与池化配置证据、建立分页 delta 测量基线（**不承诺 Word 逐页一致**）。

| 工作流 | 改造要点 |
| --- | --- |
| **F4-A1 并行转换回归（LR-A1 收尾）** | ≥4 路并发经 **生产池化路径** 转换成功；真实 `soffice` 或 Docker 栈证据 |
| **F4-A2 池化/超时/清理可配置性** | `DocgenRenderingProperties` + `application.yml` 对外化；配置生效与清理回归可验证 |
| **F4-A3 Profile 隔离缺口补齐** | CLI / docker-exec / DOCX normalization 全路径 per-invocation profile；无泄漏 |
| **F4-A4 分页 delta 测量基线（LR-A7 子集）** | ≥5 封 P23 demo 语料表 + 测量规程；记录 Word vs PDF 页数 delta；**不**在运行时强制阻断 |

**与已完成工作的关系（仅交叉引用，F4 不重做）**

| 已有资产 | 状态 | F4 关系 |
| --- | --- | --- |
| `LibreOfficePdfConversionService` per-invocation `-env:UserInstallation` + CLI hardening | **LR-A1 partial** | F4-A1 补真实并行验收；F4-A3 审计 sibling 路径 |
| `DockerExecPdfConversionService` / `LibreOfficeDocxNormalizationService` profile 隔离 | **LR-A1 partial** | F4-A3 补 docker-exec 容器内 profile 清理与测试 |
| `LibreOfficePdfConversionServiceTest` profile URL + fake-script 并行 | **LR-A1 partial** | F4-A1 需 **真实 soffice** 或 Docker 栈并行证据 |
| CJK + metric fonts + `RenderingFontSmokeTest` | **LR-A2 Done** (2026-07-08) | **Out of scope** — 仅回归引用 |
| `OoxmlOutputValidationGateTest` | **LR-A6 partial** | **Out of scope** — 不在 F4 扩展 |
| F1 unified writer + fail-closed refs | **F1 Done** | **Out of scope** |

---

## 2. Actor / Role

| Actor | 说明 | 权限 / 关注点 |
| --- | --- | --- |
| **平台运维 / SRE** | 配置 PDF 转换池大小、超时、Docker 模式 | 部署 profile、`PDF_CONVERSION_POOL_SIZE` 等 env |
| **运行时 API 调用方** | 同步/批量生成 DOCX→PDF | 无直接 LO 配置权限；期望并发下稳定 PDF |
| **发布/测试人员** | 预览与批量测试触发 PDF 转换 | 间接使用池化路径 |
| **系统（转换池）** | `pdfConversionExecutor`  bounded pool | 饱和时 fail-fast（`PdfConversionCapacityExceededException`） |

---

## 3. Goal

1. **A1**：生产配置下 ≥4 路并发 PDF 转换全部成功，输出有效 PDF，无共享 profile 导致的间歇失败（CD-PIT-11）。
2. **A2**：池大小、超时、队列容量、分页预算属性 **可配置、有文档、有测试证据**。
3. **A3**：所有 `soffice` 启动点 per-invocation profile 隔离；转换后 profile/temp **不累积**。
4. **A4**：建立 ≥5 封银行信函语料的分页 delta 基线表与可重复测量规程；为 ADR-0042 提供数据，**不**承诺 Word 逐页一致。

---

## 4. 已确认决策（2026-07-09）

| ID | 决策 |
| --- | --- |
| **F4-C1** | F4 **不重做** LR-A2 字体基线、`RenderingFontSmokeTest`、Dockerfile font 包 |
| **F4-C2** | F4 **不扩展** LR-A6 OOXML 验证门；现有 `OoxmlOutputValidationGateTest` 保持 |
| **F4-C3** | 并行回归须经 **生产池 bean 契约**（`pdfConversionExecutor` 或等价 `ThreadPoolTaskExecutor` 配置），非裸 `new Thread` |
| **F4-C4** | 真实 `soffice` 不可用时：测试 **skip**（与 `RenderingFontSmokeTest` 同模式）；Docker 栈 smoke 作为 **补充证据** 记入 ledger |
| **F4-C5** | `DocgenRenderingProperties.conversionPoolSize` 默认 **2**；`conversionTimeoutSeconds` 默认 **120**；`conversionQueueCapacity` 默认 **0**（fail-fast）— 与 COR-P02 / SOR-P03 一致 |
| **F4-C6** | `paginationDeltaBudgetPages` 默认 **1**（代码已存在）；F4 仅 **文档化 + 语料测量**；**运行时强制告警/阻断** 留待 ADR-0042 确认后单独 slice |
| **F4-C7** | 分页测量 **不承诺** Word 与 LibreOffice PDF 逐页一致；只记录 delta 与可接受预算提案 |
| **F4-C8** | F4 **不含** 前端变更、E2E/UIUX、PDF 页码加盖（`pdfPageNumberStampingEnabled` 仍默认 false）、DOCX normalization 默认启用 |
| **F4-C9** | LR-A1 在 F4 关闭时标记 **Done**（与 LRP-A 行同步）；LR-A7 在 F4-A4 完成后标记 **partial Done**（语料+规程；ADR-0042 定稿仍归 LR-A5/LR-A7 程序行） |

---

## 5. 前置条件

- CORE-FORTRESS F1–F3 **Done**（统一 writer、表达式引擎、运行时轻量化）。
- P23 **Done**：≥5 封银行级 demo master 可用于分页语料（见 §8 语料清单）。
- LR-A2 **Done**：Docker 镜像含 CJK + metric fonts；`RenderingFontSmokeTest` 在 `mvn verify` 中可 skip 或 green。
- 部署栈：`docker-deploy.ps1` 嵌入式 LibreOffice（`LIBREOFFICE_CONVERSION_MODE=cli`）或可选 `docker-exec` sidecar。

---

## 6. 验收场景（Given / When / Then）

### F4-A1 — 并行转换回归（LR-A1 收尾）

#### BDD-F4-A1-001 — 四路并发经池化路径成功

**Given** `soffice` 可用（本地或 Docker 后端镜像）且 `conversionPoolSize ≥ 2`  
**When** 同时提交 **4** 个 DOCX→PDF 转换请求，每个经 `LibreOfficePdfConversionService.convertWithResult` 且注入 **生产等价** 的 bounded executor（core=max=poolSize）  
**Then** 4 个 Future 均在 `conversionTimeoutSeconds` 内完成  
**And** 每个输出以 `%PDF` 开头且 PDFBox 可打开  
**And** 无 `dconf`/profile lock 类错误导致失败

#### BDD-F4-A1-002 — 并行后无 profile 目录泄漏

**Given** BDD-F4-A1-001 执行前后  
**When** 统计 `java.io.tmpdir` 下 `docgen-lo-profile-*` 目录数  
**Then** 执行后数量 **不增加**（best-effort；与现有 `cleansUpProfileDirectoryAfterConversion` 一致）

#### BDD-F4-A1-003 — Fake-script 单元回归保持绿

**Given** CI 无真实 `soffice`  
**When** `LibreOfficePdfConversionServiceTest` 运行  
**Then** `isolatesProfileAcrossParallelConversions` 等 LR-A1 单元测试仍 green（不删除，作为 fast path）

---

### F4-A2 — 池化/超时/配置证据

#### BDD-F4-A2-001 — application.yml 对外化渲染属性

**Given** 运维设置 `PDF_CONVERSION_POOL_SIZE=4`  
**When** Spring 上下文加载 `DocgenRenderingProperties`  
**Then** `conversionPoolSize == 4`  
**And** `PdfConversionExecutorConfig` 创建的 executor core/max == 4

#### BDD-F4-A2-002 — 超时配置生效

**Given** `conversionTimeoutSeconds=5` 且转换脚本故意超时  
**When** 发起 PDF 转换  
**Then** 在约 5s 内失败并抛出 `TemplateValidationException`（`api.error.generation.pdfConversionFailed`）

#### BDD-F4-A2-003 — 池饱和 fail-fast

**Given** `conversionPoolSize=1`、`conversionQueueCapacity=0`  
**When** 2 个转换同时提交且第一个占用 worker  
**Then** 第二个立即 rejected（`PdfConversionCapacityExceededException` 或等价 abort policy）— 与 SOR-P03 一致

#### BDD-F4-A2-004 — paginationDeltaBudgetPages 可配置

**Given** `docgen.rendering.pagination-delta-budget-pages` 或等价 env 设置为 `2`  
**When** 属性 bean 加载  
**Then** `getPaginationDeltaBudgetPages() == 2`  
**And** F4 **不**要求运行时据此阻断生成（仅配置就绪）

#### BDD-F4-A2-005 — 顺序转换无 temp 累积

**Given** 连续 **10** 次顺序 PDF 转换  
**When** 每次转换完成或失败  
**Then** `docgen-pdf-*` 与 `docgen-lo-profile-*` 临时目录数不随次数单调增长

---

### F4-A3 — Profile 隔离缺口

#### BDD-F4-A3-001 — CLI 路径 per-invocation UserInstallation

**Given** `conversion-mode=cli`  
**When** 单次转换  
**Then** `soffice` 命令行含 `-env:UserInstallation=file:///…` 且路径唯一  
**And** `--norestore --nolockcheck --nodefault --nologo`  present

#### BDD-F4-A3-002 — docker-exec 路径容器内唯一 profile

**Given** `conversion-mode=docker-exec`  
**When** 两次并发转换  
**Then** 两次 `docker exec … -env:UserInstallation=file:///tmp/docgen-lo-profile-<unique>` 路径 **不同**  
**And** 转换完成后容器内对应 profile 目录 **best-effort 清理**（F4 新增行为）

#### BDD-F4-A3-003 — DOCX normalization profile 清理

**Given** `docx-normalization-enabled=true`（测试 profile 仅）  
**When** normalization 成功或失败  
**Then** `docgen-lo-norm-profile-*` 目录被删除

---

### F4-A4 — 分页 delta 测量基线（LR-A7 子集）

#### BDD-F4-A4-001 — 语料表 ≥5 封

**Given** P23 demo masters 已就绪  
**When** 编制分页语料表  
**Then** 至少 **5** 封不同业务类型信函列入表（credit-limit、mortgage、trade-lc、collection、retail-account 等）  
**And** 每行含：master 路径、Word 基线页数（人工或 Word 打开记录）、Docker 栈 PDF 页数、delta、测量日期、栈版本/git SHA

#### BDD-F4-A4-002 — 可重复测量规程

**Given** 语料表已发布在 NFR §production rendering  
**When** 运维按规程执行（`docker-deploy.ps1` → 生成 PDF → PDFBox 计页）  
**Then** 可在新环境复现 delta 列（允许 ±0 页测量误差说明）  
**And** 规程 **不** 要求 Word 自动化批处理

#### BDD-F4-A4-003 — 预算提案记录

**Given** 5+ 语料 delta 已测量  
**When** 汇总 max/median delta  
**Then** 在 NFR 或 ADR-0042 草案中记录 **提案预算**（默认参考 `paginationDeltaBudgetPages=1`）  
**And** 明确标注 **pending user confirmation** 直至 ADR Accepted

---

## 7. 边界与异常行为

| 场景 | 期望行为 |
| --- | --- |
| `soffice` 不可用（本地 dev） | 集成测试 skip；单元测试 fake-script 仍覆盖逻辑 |
| 转换超时 | fail-closed；`TemplateValidationException`；temp/profile best-effort 清理 |
| 池饱和 | fail-fast；不阻塞 servlet 线程无限等待 |
| 分页 delta 超预算 | F4 **仅记录**；不阻断生成、不新增 fidelity warning（留 ADR-0042 后续） |
| docker-exec 容器不可达 | 现有 resilience + 转换失败错误；不泄露容器内部路径细节 |

---

## 8. 分页语料清单（初始 — 测量时以 P23 masters 为准）

| # | Demo package | Master asset | 业务类型 |
| --- | --- | --- | --- |
| 1 | demo-credit-limit | `credit-limit-master.docx` | 授信额度通知 |
| 2 | demo-mortgage | `mortgage-approval-master.docx` | 按揭批核 |
| 3 | demo-trade-lc | `trade-lc-notice-master.docx` | 贸易信用证 |
| 4 | demo-collection | `overdue-collection-master.docx` | 逾期催收 |
| 5 | demo-retail-account | `retail-account-open-master.docx` | 零售开户 |
| 6 | demo-fol | `wholesale-fol-master.docx` | 批发 FOL（可选第 6 条） |

---

## 9. 可观察证据

| 证据 | 证明内容 |
| --- | --- |
| `LibreOfficeParallelConversionIntegrationTest`（或等价）green / skip 日志 | A1 真实并行转换 |
| `LibreOfficePdfConversionServiceTest` green | A1 fast-path + 清理回归 |
| `PdfConversionExecutorConfig` / properties 测试 green | A2 配置绑定 |
| `DockerExecPdfConversionServiceTest`（新增或扩展） | A3 docker-exec profile |
| NFR §production rendering 语料表 + 规程 | A4 分页基线 |
| `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS | 全部门禁 |
| Docker deploy + 可选并行 smoke 截图/ledger 行 | 生产栈验收 |
| Ledger LR-A1 → Done；LR-A7 partial | 程序交叉引用 |

---

## 10. 待确认问题

| ID | 问题 | 默认 / 建议 |
| --- | --- | --- |
| **F4-Q1** | ADR-0042 分页预算最终数值？ | 提案 **±1 页**（与 `paginationDeltaBudgetPages` 默认一致）；F4 用测量数据支撑 |
| **F4-Q2** | docker-exec 模式是否必须在容器内 `rm -rf` profile？ | **是** — F4-A3-002 采用 best-effort 容器内清理，防 sidecar 磁盘累积 |
| **F4-Q3** | 并行集成测试最低 pool size？ | 测试内 **临时** 设 `conversionPoolSize=4`；生产默认仍为 2 |

---

## 11. 追溯

| 文档 | 关系 |
| --- | --- |
| [CORE-FORTRESS program roadmap](../plan/detail/CORE-FORTRESS-program-roadmap.md) | F4 程序位 |
| [CORE-FORTRESS F4 detail plan](../plan/detail/CORE-FORTRESS-f4-production-rendering-hardening.md) | 任务分解 F4-T01… |
| [LRP-A rendering trust](../plan/detail/LRP-A-rendering-trust-hardening.md) | LR-A1/A7 源任务 |
| [P23 demo typography](../plan/detail/P23-demo-typography-layout-excellence.md) | 语料与字体基线 |
| [CDP pitfall CD-PIT-11](../plan/detail/CDP-industry-pitfall-registry.md) | 共享 profile 风险 |
| ADR-0042（草案，LR-A5） | 分页预算决策 — F4  feeding data only |
| `DocgenRenderingProperties` | A2 配置 SSOT |
| `non-functional-requirements.md` | A4 语料表持久化目标 |

---

## 12. BDD readiness

**Readiness:** `ready`  
**Handoff:** `plan-orchestrator` / `backend-engineer`（F4-T02 起 TDD）  
**Blocking questions:** F4-Q1（预算数值）不阻塞实现；F4-Q2/Q3 已有默认决策可执行
