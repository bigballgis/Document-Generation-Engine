# BDD 行为规格：PRR-D01b — Actuator / nginx CSP / ADR-0044 honesty / IRC mapper

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-PRR-D01B` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | NON-CE PRR Wave D **split** residual leaf（`prod-ops-security-hardening`） |
| **Slice** | `prod-ops-security-hardening` |
| **Branch** | `feat/prod-ops-security-hardening` |
| **Worktree** | `D:/working/DGE-prod-ops-security-hardening` |
| **Placement** | ISOLATED |
| **Task Master** | **#135** PRR-D01b — Batch Recommendation **split**；本叶 `member_task_ids: ["135"]` |
| **Prior leaf** | **#104** PRR-D01a（`prod-ops-resilience-pdf-pool`）— ResilienceFailureMapper + PDF pool **Done** |
| **Formal phase** | **None**（运维安全加固叶；不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **split**（`member_task_ids: ["135"]`；`proposed_slice_id: prod-ops-security-hardening`；`vetoes_applied: ["dashboard FE"]`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [prod-ops-resilience-pdf-pool.md](./prod-ops-resilience-pdf-pool.md)；[ADR-0044 deployment topology](../adr/operations/0044-deployment-topology-v1.md)；[ADR-0044 multi-instance baseline](../adr/operations/0044-multi-instance-correctness-baseline.md)；[ADR-0031 security headers](../adr/api/0031-api-platform-hardening-baseline.md)；契约 [contract-outline.md](../api/contract-outline.md)；CE-U11 IRC [ce-u11-invocation-troubleshoot.md](./ce-u11-invocation-troubleshoot.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（无 Vue dashboard；下一叶） |

**完成声明约束：** 本叶关闭 Wave D 残差中的 **actuator 匿名暴露**、**nginx CSP/标准安全头**、**ADR-0044 文档诚实**、以及 **FailedSyncInvocationErrorMapper** 对 D01A 三码族的 IRC 对齐。**禁止**据此宣称 go-live；**禁止**翻转 checklist **#3b**（保持 **CONDITIONAL**）；**禁止**将 **#5a** 标为 **GO**；**禁止**交付本文件 §OUT 所列 dashboard FE 与其它后续叶范围。

**Supersession note (rate-limit residual only — 2026-07-23):** D01B-C9 / BDD-PRR-D01B-011 对 runtime rate-limit「进程内权威 / distributed 不得读成已交付」的诚实陈述，在 **PQH-F7 / TM #163** 文档契约下被**收窄**（非整叶作废）：默认仍 `distributed=false`（单副本诚实不变）；共享 Redis 限流为该叶 **accepted opt-in contract**（启用且验证后关闭「aspirational dead config」残差**仅限 rate-limit 行**）。权威行为 SoT：[pqh-f7-redis-rate-limit.md](./pqh-f7-redis-rate-limit.md)。**仍禁止**宣称 multi-instance / SSE / Redisson locks / Kafka 完整。本文件历史 D01B **Done** 声明不因本注记回写。

---

## 0. Batch / queue context

```text
batch_recommendation:
  decision: split
  member_task_ids: ["135"]
  proposed_slice_id: prod-ops-security-hardening
  vetoes_applied: ["dashboard FE"]
  rationale: >
    Wave D residual after D01A; dashboard Vue summary deferred to next serial leaf;
    this leaf amortizes actuator + nginx headers + ADR honesty + IRC mapper in one evidence run.
```

| IN（本叶） | OUT（后续串行叶 / 明确禁止） |
| --- | --- |
| Actuator：`/actuator/metrics` + `/actuator/prometheus` 在 claimed-prod / acceptance-hardening 路径**不得**匿名开放；scrape auth 诚实 | Dashboard summary API / Vue 管理看板（下一叶） |
| 保留今日 `/healthz`（及既有 `/readyz`）公开探针路径 | 公司 IdP / SSO 接入 scrape |
| nginx：CSP + 标准安全响应头（docker acceptance / prod compose 所用 frontend edge） | knip / listAll 大扫除（非本叶主角） |
| ADR-0044：分布式/死配置残差显式诚实（单副本 / sticky SSE / rate-limit 进程内） | 宣称 multi-instance / Redis rate-limit 已完整交付 |
| `FailedSyncInvocationErrorMapper` → D01A 三异常稳定 IRC envelope | 翻转 #3b GO / #5a GO / go-live |

---

## 1. 概述

### 1.1 问题（现状证据 — implementation 输入）

| 发现 | 证据 |
| --- | --- |
| `/actuator/prometheus` 与 `/actuator/metrics(**)` 在 `SecurityConfig` 中与 `/healthz` 一并 `permitAll()` | `SecurityConfig.managementSecurityFilterChain` |
| claimed-prod 暴露 prometheus/metrics（`application-prod.yml` `exposure.include`）但**无** scrape 认证 | `application-prod.yml` `management.endpoints` |
| 前端 nginx edge **无** CSP / 标准安全头 | `frontend/nginx.conf`（仅 body size、SSE、proxy；无 `add_header` 安全族） |
| ADR-0044 multi-instance 文案曾暗示 prod 默认 distributed rate-limit；运行时 prod 默认 `RUNTIME_RATE_LIMIT_DISTRIBUTED:false`，进程内 Bucket4j 仍为权威 | `0044-multi-instance-correctness-baseline.md` vs `application-prod.yml` + `RuntimeRateLimitFilter` 注释 |
| 拓扑 ADR 已确认 v1 单 serving 副本 + sticky SSE + 进程内限流 deferral，但需与「分布式已完成」叙事去歧义 | `0044-deployment-topology-v1.md` |
| D01A 已让 API advice 发出 `GENERATION_SERVICE_UNAVAILABLE` / `GENERATION_TIMEOUT` / `PDF_CONVERSION_CAPACITY_EXCEEDED`；`FailedSyncInvocationErrorMapper` **未**识别这三类，失败同步调用持久化 envelope 可能落空（`return null`） | `FailedSyncInvocationErrorMapper.java` vs D01A / `ErrorEnvelopeFactory` |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **D01B-S1 Actuator scrape hardening** | claimed-prod / acceptance-hardening 路径下 metrics/prometheus **禁止匿名**；公开探针仅保留既有 healthz/readyz（及今日已公开的 health 探针语义，见确认决策） |
| **D01B-S2 Honest scrape auth** | 采用 **HTTP Basic**（env/secrets 凭据）作为最小耐久 scrape 模型；网络策略仅作纵深说明，**不得**单独充当「已加固」声明；不发明公司 IdP |
| **D01B-S3 nginx security headers** | docker acceptance / prod compose 使用的 frontend nginx edge 对 SPA 响应发出 CSP + 标准安全头（对齐 ADR-0031 基线意图） |
| **D01B-S4 ADR-0044 honesty** | 文档明确：v1 单副本、SSE sticky、rate-limit 进程内；任何 distributed 开关/死配置不得被读成「多实例正确性已完成」 |
| **D01B-S5 IRC mapper taxonomy** | 同步失败持久化映射与 D01A HTTP taxonomy 三码族对齐 |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **匿名客户端 / 互联网侧扫描** | 未认证调用方 | 不得拉取 metrics/prometheus 正文 |
| **平台运维 / SRE（Prometheus）** | scrape 主体 | 使用文档化的 HTTP Basic 凭据拉取 `/actuator/prometheus`；探针仍用 `/healthz` |
| **编排器（Compose / K8s）** | liveness/readiness | `/healthz`、`/readyz` 匿名可达（与今日一致） |
| **Runtime 同步调用路径** | 系统内部 | 失败写入 invocation error envelope 时含稳定 code/category/retryable/messageKey |
| **文档读者 / 架构评审** | Operator / reviewer | ADR-0044 不夸大多实例能力 |
| **（非本片）管理 UI 用户** | — | `frontend_ui_in_scope=false`；dashboard 下一叶 |

权限：本叶**无新业务权限码**；scrape 使用 **ops secret Basic**（非 JWT 业务角色）。管理 API JWT 路径不变。

---

## 3. Goal

1. claimed-prod / acceptance-hardening 路径下，`/actuator/metrics` 与 `/actuator/prometheus` **不可匿名**访问。
2. scrape 认证模型诚实、可运维：HTTP Basic + secrets/env；文档可说明网络策略为纵深，但不替代应用层拒绝匿名。
3. 保留今日公开探针：`GET /healthz`（及既有 `/readyz`）匿名成功。
4. docker acceptance / prod compose 所用 frontend nginx edge 响应带 CSP + 标准安全头。
5. ADR-0044（及相关 ops 说明）诚实陈述单副本 / sticky SSE / 进程内限流残差，不宣称 multi-instance complete。
6. `FailedSyncInvocationErrorMapper` 将 D01A 三类异常映射为与 HTTP taxonomy 一致的 IRC envelope 字段。
7. 无 Vue dashboard / Playwright 义务；不翻转 #3b / #5a；不宣称 go-live。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **D01B-C1** | **Metrics/Prometheus 禁止匿名（hardening 路径）：** 在 **claimed-prod** 与 **acceptance-hardening** 运行路径（`prod` profile 及 docker acceptance / prod compose 所部署的后端安全配置）下，未认证 `GET /actuator/prometheus`、`GET /actuator/metrics`、`GET /actuator/metrics/**` **必须失败**（HTTP **401** Unauthorized；不得 200 返回 scrape/metric 正文）。 | handoff + `SecurityConfig` 现状 |
| **D01B-C2** | **Scrape auth = HTTP Basic（最小耐久）：** 合法 scrape 使用 **HTTP Basic**，用户名/密码来自 **env / secrets**（镜像与仓库**无**可用默认口令）。Prometheus scrape 文档/示例须与该模型一致（可更新 `deploy/observability/prometheus-scrape.yaml` 注释或等价 ops 说明）。**不**引入公司 IdP / OAuth / mTLS 作为本叶验收条件。 | handoff「basic auth… minimal durable」+ 现有单端口 `SecurityConfig` |
| **D01B-C3** | **网络策略 / 管理端口：** 文档可记载「建议仅内网 / NetworkPolicy 放行 scrape」作为**纵深**；**不得**以「仅网络策略、应用仍 permitAll」宣称本叶 Done。独立 management port **不作为本叶必选实现**（避免双端口 compose  churn）；若实现另选 management port，须同等满足「匿名不可达 metrics/prometheus」+ 文档诚实，且不破坏 `/healthz` 验收端口约定。 | handoff 三选一 → 选定 Basic 为主 |
| **D01B-C4** | **公开探针保留：** `GET /healthz` 与 `GET /readyz` 保持今日匿名可达语义。`/actuator/health`（及 `/actuator/health/**`）若今日为 probe/permitAll，本叶**不强制关闭**（非 metrics 泄露面）；本叶验收主角是 metrics/prometheus。 | handoff「healthz public path preserved」 |
| **D01B-C5** | **非 hardening 本地开发路径：** `dev` / 本地非 claimed-prod 路径可保持便于调试的暴露策略，但**不得**弱化 prod / acceptance-hardening 路径的 C1。验收证据以 hardening 路径为准。 | 与 PRR claimed-prod 惯例一致 |
| **D01B-C6** | **nginx CSP + 标准安全头：** docker acceptance / prod compose 使用的 frontend edge 配置（至少 `frontend/nginx.conf`；若 `nginx-main.conf` 影响该路径则一并）对 HTML/SPA 成功响应发出至少：`Content-Security-Policy`、`X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`。CSP 须为**明确策略字符串**（可 `default-src 'self'` 为基线，并允许本应用已知的 pdf.js / 同域 API 所需指令；**禁止**空头或仅注释「TODO CSP」冒充完成）。 | ADR-0031 + handoff |
| **D01B-C7** | **HSTS：** 在 **纯 HTTP** acceptance（如 `:4173`）上 **不强制** `Strict-Transport-Security`（避免误伤本地 HTTP）。若 edge 终止 TLS 的 prod 路径适用，可另加 HSTS；本叶验收不因缺 HSTS on HTTP 失败。 | ADR-0031 意图 + 本地验收现实 |
| **D01B-C8** | **安全头不破坏功能：** SSE progress proxy、`/api/` 反代、`/healthz` 静态 ok、pdf.js `.mjs` 类型行为保持可用；CSP 不得无故阻断同域 SPA 引导与既有管理 UI 资源加载（本叶无 FE 功能改动，仅 edge 头）。 | 既有 `nginx.conf` 行为 |
| **D01B-C9** | **ADR-0044 诚实陈述（文档事实）：** 更新/对齐 ADR-0044 家族（topology + multi-instance baseline，及必要的 ops 交叉引用）使读者明确：**(a)** v1 **单 serving backend 副本**；**(b)** SSE **sticky sessions required**（无 Redis pub/sub relay 则不算多实例完整）；**(c)** runtime rate-limit：**默认**进程内权威（`distributed` 开关默认 false）；启用 Redis shared limiter 前不得声称「分布式限流已交付」——**PQH-F7（2026-07-23）收窄本条 (c)**：见文件头 supersession note 与 [pqh-f7-redis-rate-limit.md](./pqh-f7-redis-rate-limit.md)（默认仍 false；opt-in 为 accepted leaf contract）；**(d)** 任何「dead / aspirational / deferred」配置必须标为残差，**禁止**声称 multi-instance correctness complete。 | handoff + ADR-0044 + `application-prod.yml` |
| **D01B-C10** | **不翻转 Accepted ADR 决策核：** 诚实补丁是澄清后果/现状/残差，不是把「v1 单副本」改写成「已水平扩展」。 | document-as-code |
| **D01B-C11** | **IRC mapper — 三异常对齐 D01A：** `FailedSyncInvocationErrorMapper.from` 在 throwable（及 **cause chain**）上识别： | D01A §4.1 + handoff |
| | → `GenerationServiceUnavailableException` ⇒ `code=GENERATION_SERVICE_UNAVAILABLE`，`category=GENERATION`，`retryable=true`，`messageKey=api.error.generation.generationServiceUnavailable` | |
| | → `GenerationTimeoutException` ⇒ `code=GENERATION_TIMEOUT`，`category=GENERATION`，`retryable=true`，`messageKey=api.error.generation.generationTimeout` | |
| | → `PdfConversionCapacityExceededException` ⇒ `code=PDF_CONVERSION_CAPACITY_EXCEEDED`，`category=GENERATION`，`retryable=true`，`messageKey=api.error.generation.pdfConversionCapacityExceeded` | |
| **D01B-C12** | **IRC message 解析：** 与既有 mapper 一致，经 `MessageResolver` 解析英文 message；**禁止**把异常类名、堆栈、Resilience4j 内部名写入 envelope message。 | D01A-C8 + CE-U11 IRC-006 |
| **D01B-C13** | **既有映射优先序不变：** 已识别的 `TemplateValidationException` / rendering / encryption / batch 等分支保持；仅在未命中既有分支时（或按 cause 链在适当时机）命中 C11 三类。不得把真实模板校验改标为 generation unavailable。 | `FailedSyncInvocationErrorMapper` 现状 |
| **D01B-C14** | **`frontend_ui_in_scope=false`：** 无 Vue / i18n UI / Playwright / UIUX；dashboard summary **OUT**。Done 主证据 = 后端安全/映射测试 + nginx 配置静态断言或 curl 头检查 + ADR 文档 diff + `mvn verify`（及切片所需的配置/文档门禁）。 | handoff veto |
| **D01B-C15** | **治理冻结：** 不翻转 #3b；不标 #5a GO；不宣称 go-live；不激活 CD-3 / IBL；Formal phase 保持 None。 | handoff |

### 4.1 Actuator 访问矩阵（hardening 路径验收权威）

| 路径 | 匿名 | 合法 Basic scrape | 错误 Basic |
| --- | --- | --- | --- |
| `GET /healthz` | **200**（保留） | 200 | 200（仍公开） |
| `GET /readyz` | **200**（保留） | 200 | 200 |
| `GET /actuator/prometheus` | **401**（无正文指标） | **200**（Prometheus text） | **401** |
| `GET /actuator/metrics` | **401** | **200**（或框架等价成功体） | **401** |
| `GET /actuator/metrics/**` | **401** | 按授权成功或 404（资源不存在）；**不得**因匿名而 200 | **401** |

### 4.2 IRC 映射表（与 D01A HTTP taxonomy 对齐）

| 触发（类型或 cause chain） | 持久化 `code` | `category` | `retryable` | `messageKey` |
| --- | --- | --- | --- | --- |
| `GenerationServiceUnavailableException` | `GENERATION_SERVICE_UNAVAILABLE` | `GENERATION` | `true` | `api.error.generation.generationServiceUnavailable` |
| `GenerationTimeoutException` | `GENERATION_TIMEOUT` | `GENERATION` | `true` | `api.error.generation.generationTimeout` |
| `PdfConversionCapacityExceededException` | `PDF_CONVERSION_CAPACITY_EXCEEDED` | `GENERATION` | `true` | `api.error.generation.pdfConversionCapacityExceeded` |

（HTTP 状态码由 API advice 路径保证；IRC 持久化字段与上表一致即可，本叶不要求改写 HTTP advice。）

### 4.3 nginx 安全头最小集（acceptance / prod edge）

| Header | 要求 |
| --- | --- |
| `Content-Security-Policy` | 非空、可解析策略；至少限制默认源（如 `default-src 'self'` + 本应用必需例外） |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` 或 `SAMEORIGIN`（二选一，文档化） |
| `Referrer-Policy` | 明确值（如 `strict-origin-when-cross-origin`） |
| `Strict-Transport-Security` | HTTP acceptance **可选**；TLS edge **建议** |

---

## 5. 前置条件

- D01A（#104）已合并：API 侧三码族与 PDF 池 fail-closed 已存在。
- `SecurityConfig` 当前将 prometheus/metrics 与 healthz 一并 permitAll（待本叶收紧）。
- Frontend 由 nginx 容器服务 SPA；compose acceptance `:4173` / prod compose 使用该 edge。
- ADR-0044 Accepted；v1 单副本决策已存在，需诚实补丁而非新决策核。
- 本切片在隔离 worktree 交付；不在 MAIN 实现行为代码。

---

## 6. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 匿名或带 Basic 的 `GET` 打到 `/actuator/prometheus` / `/actuator/metrics`（hardening 配置） | Actuator 加固 |
| T2 | 编排器/验收脚本 `GET /healthz` | 探针回归 |
| T3 | 客户端请求 frontend nginx 服务的 SPA 文档（`GET /` 或 `index.html`） | 安全头 |
| T4 | 同步 runtime 生成失败且 cause 为 D01A 三类异常，经 `FailedSyncInvocationErrorMapper` | IRC 映射 |
| T5 | 读者打开 ADR-0044 文档 | 诚实陈述 |

---

## 7. Primary journey

1. 验收栈以 claimed-prod / acceptance-hardening 安全配置启动后端 + frontend nginx。
2. 匿名拉取 prometheus/metrics → 被拒绝；使用配置的 Basic 凭据 → scrape 成功。
3. `/healthz`（及 `/readyz`）匿名仍 200。
4. 浏览器/curl 访问 SPA → 响应含 CSP + 标准安全头。
5. 同步失败路径若抛出 D01A 三类异常 → invocation 持久化 envelope 含稳定三码字段。
6. ADR-0044 文档明确单副本 / sticky SSE / 进程内限流残差，无「多实例已完成」误导。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| 匿名 metrics/prometheus（hardening） | **401**；无指标正文 |
| 合法 Basic scrape | **200** + Prometheus/metrics 体 |
| 错误 Basic | **401** |
| 匿名 `/healthz` / `/readyz` | **200**（不变） |
| SPA `GET /`（edge） | **200** + §4.3 头 |
| IRC：`GenerationServiceUnavailableException` | envelope 按 §4.2 |
| IRC：`GenerationTimeoutException` | envelope 按 §4.2 |
| IRC：`PdfConversionCapacityExceededException` | envelope 按 §4.2 |
| ADR 阅读 | 残差显式；无 multi-instance complete 声称 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-PRR-D01B-001 — 匿名 Prometheus 拒绝（hardening）

**Given** 后端以 claimed-prod / acceptance-hardening 安全配置运行，且未提供 scrape Basic 凭据  
**When** 匿名 `GET /actuator/prometheus`  
**Then** HTTP **401**  
**And** 响应体**不是**可用的 Prometheus exposition（不得 200 返回 metric samples）

### BDD-PRR-D01B-002 — 匿名 metrics 拒绝（hardening）

**Given** 同 **D01B-001** 配置  
**When** 匿名 `GET /actuator/metrics`（或 `/actuator/metrics/jvm.memory.used` 一类子路径）  
**Then** HTTP **401**  
**And** 不得因匿名而返回指标 JSON/正文

### BDD-PRR-D01B-003 — 合法 Basic 可 scrape Prometheus

**Given** hardening 配置已设置 scrape Basic 用户名/密码（测试用 secrets / env）  
**When** `GET /actuator/prometheus` 携带正确 `Authorization: Basic …`  
**Then** HTTP **200**  
**And** 响应含 Prometheus text exposition（可含 `# HELP` / `# TYPE` 或既有 Micrometer 格式样本）

### BDD-PRR-D01B-004 — 错误 Basic 拒绝

**Given** 同 **D01B-003** 配置  
**When** `GET /actuator/prometheus` 携带错误 Basic 凭据  
**Then** HTTP **401**

### BDD-PRR-D01B-005 — healthz 公开路径保留

**Given** 同 hardening 配置（metrics 已收紧）  
**When** 匿名 `GET /healthz`  
**Then** HTTP **200**（与今日公开探针语义一致）  
**And** 匿名 `GET /readyz` 仍成功（既有 readiness 公开语义）

### BDD-PRR-D01B-006 — scrape 模型文档诚实

**Given** ops / observability 文档（含 scrape 示例或 runbook 注记）已随本叶更新  
**When** 读者查看如何拉取 `/actuator/prometheus`  
**Then** 文档写明 **HTTP Basic**（凭据来自 env/secrets）  
**And** 若提及 NetworkPolicy / 内网，则标注为纵深，**不是**「应用仍匿名开放」  
**And** **不**要求公司 IdP

### BDD-PRR-D01B-007 — nginx CSP 头存在

**Given** docker acceptance / prod compose 路径使用的 frontend nginx 配置已部署或可静态解析  
**When** 对 SPA 文档响应（如 `GET /`）检查响应头（或等价配置断言）  
**Then** 存在非空 `Content-Security-Policy`  
**And** 策略至少包含限制性 `default-src`（或等价基线）且不是占位 TODO

### BDD-PRR-D01B-008 — nginx 标准安全头存在

**Given** 同 **D01B-007**  
**When** 检查同一 SPA 成功响应  
**Then** 存在 `X-Content-Type-Options: nosniff`  
**And** 存在 `X-Frame-Options`（`DENY` 或 `SAMEORIGIN`）  
**And** 存在明确的 `Referrer-Policy`

### BDD-PRR-D01B-009 — 安全头不破坏探针与 API 反代配置面

**Given** 更新后的 `frontend/nginx.conf`（及必要的 main 配置）  
**When** 审查 location 行为  
**Then** `/healthz`、`/readyz`、`/api/` proxy、progress-stream SSE 反代配置仍然存在且语义不被本叶删除  
**And** `.mjs` JavaScript 类型规则保留

### BDD-PRR-D01B-010 — ADR-0044 单副本诚实

**Given** ADR-0044 家族文档（topology 与/或 multi-instance baseline）已按本叶更新  
**When** 读者查找 v1 部署拓扑结论  
**Then** 明确陈述 **single serving backend replica**（v1）  
**And** **不**声称 backend 多实例水平扩展已完成或可安全默认开启

### BDD-PRR-D01B-011 — ADR-0044 sticky SSE / rate-limit 残差诚实

**Given** 同 **D01B-010** 文档集  
**When** 读者查找 SSE 与 rate-limit 多实例前提  
**Then** 文档明确：**SSE sticky sessions required**（无 Redis pub/sub relay 则 multi-pod SSE 不完整）  
**And** runtime rate-limit：**默认** process-local 为权威；`distributed` 默认 false  
**And**（**PQH-F7 收窄，非本叶原验收改写**）读者可在 [pqh-f7-redis-rate-limit.md](./pqh-f7-redis-rate-limit.md) / ADR-0044 家族看到：opt-in Redis shared limiter 为 **accepted leaf contract**（启用+验证后关闭 rate-limit 行「aspirational dead config」残差）；**不得**把默认 off 读成「多实例已完整」  
**And** 明确 **not** multi-instance correctness complete（SSE / locks / Kafka 仍开）

### BDD-PRR-D01B-012 — IRC：GenerationServiceUnavailableException

**Given** 同步失败 throwable 为 `GenerationServiceUnavailableException`（或 cause chain 含该类，且无更高优先级既有业务映射）  
**When** `FailedSyncInvocationErrorMapper.from(...)`  
**Then** 返回非 null envelope：`code=GENERATION_SERVICE_UNAVAILABLE`，`category=GENERATION`，`retryable=true`，`messageKey=api.error.generation.generationServiceUnavailable`  
**And** `message` 不含异常类名

### BDD-PRR-D01B-013 — IRC：GenerationTimeoutException

**Given** throwable / cause 为 `GenerationTimeoutException`  
**When** mapper 处理  
**Then** `code=GENERATION_TIMEOUT`，`category=GENERATION`，`retryable=true`，`messageKey=api.error.generation.generationTimeout`

### BDD-PRR-D01B-014 — IRC：PdfConversionCapacityExceededException

**Given** throwable / cause 为 `PdfConversionCapacityExceededException`  
**When** mapper 处理  
**Then** `code=PDF_CONVERSION_CAPACITY_EXCEEDED`，`category=GENERATION`，`retryable=true`，`messageKey=api.error.generation.pdfConversionCapacityExceeded`

### BDD-PRR-D01B-015 — IRC 不破坏既有模板校验映射

**Given** throwable 为 `TemplateValidationException`（业务 messageKey）  
**When** mapper 处理  
**Then** 仍为既有 `TEMPLATE_VALIDATION_FAILED`（或 request-body 校验分支）语义  
**And** **不是** `GENERATION_SERVICE_UNAVAILABLE`

### BDD-PRR-D01B-016 — 治理冻结（文档/完成声明）

**Given** 本叶完成同步材料  
**When** 检查 checklist / 完成声明  
**Then** **#3b** 保持 **CONDITIONAL**（非 GO）  
**And** **#5a** 非 GO  
**And** **不**宣称 go-live  
**And** **不**将 dashboard FE 标为本叶 Done

---

## 10. Boundary / exception behavior

| 边界 | 行为 |
| --- | --- |
| 匿名 vs 错误凭据 | 均不得 200 返回 metrics 正文；统一 401 族 |
| JWT 业务登录用户访问 actuator | 本叶**不要求**以 JWT 代替 Basic scrape；允许仅 Basic 为 scrape 成功路径（实现若额外允许已认证管理主体，不得重新开放匿名） |
| Lab / dev 宽松暴露 | 允许，但不得污染 prod / acceptance-hardening 验收 |
| CSP 过严导致 SPA 白屏 | 视为本叶缺陷；须调整 CSP 例外，不得删除安全头交差 |
| Cause chain 包装 D01A 异常 | mapper 须沿 cause 识别 C11 三类 |
| mapper 未知异常 | 保持既有 `null`/调用方回退语义（本叶不强制改 INTERNAL_ERROR 策略） |
| 授权失败（管理 API） | 本叶不改变；fail-closed |
| FE / E2E / UIUX | N/A（`frontend_ui_in_scope=false`） |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| 安全测试 | Spring Security / MockMvc（或等价）覆盖 D01B-001…005 |
| IRC 单测 | 扩展 `RuntimeTemplateSyncSupportFailedInvocationTest`（或 mapper 专测）覆盖 D01B-012…015 |
| nginx | 配置静态断言与/或 acceptance curl 头检查（D01B-007…009） |
| 文档 | ADR-0044 诚实补丁 diff；scrape Basic 说明（D01B-006/010/011） |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` GREEN；FE lint/type-check/test/build 仅当 nginx/前端树变更触发仓库惯例时执行；E2E/UIUX **N/A** |
| Deploy | 若行为面需栈证：queued `docker-deploy-queue` → `:8080/healthz` 200 + 匿名 prometheus 非 200 + Basic scrape 200（实现阶段） |
| 非证据 | dashboard UI 截图；#3b/#5a GO；公司 IdP 集成 |

---

## 12. Traceability

| 项 | 引用 |
| --- | --- |
| Task Master | **#135**（本叶）；先验 **#104** D01A |
| Prior BDD | [prod-ops-resilience-pdf-pool.md](./prod-ops-resilience-pdf-pool.md)（**BDD-PRR-D01A-***） |
| IRC baseline | [ce-u11-invocation-troubleshoot.md](./ce-u11-invocation-troubleshoot.md)（IRC-006） |
| ADR | [0044-deployment-topology-v1.md](../adr/operations/0044-deployment-topology-v1.md)；[0044-multi-instance-correctness-baseline.md](../adr/operations/0044-multi-instance-correctness-baseline.md)；[0031-api-platform-hardening-baseline.md](../adr/api/0031-api-platform-hardening-baseline.md) |
| Security | `SecurityConfig.java`；`application-prod.yml` management exposure |
| Edge | `frontend/nginx.conf`；`frontend/nginx-main.conf` |
| API codes | `ApiErrorCodes` / `ErrorEnvelopeFactory` / D01A §4.1 |
| Checklist | **#3b** CONDITIONAL；**#5a** 不因本叶 GO |

---

## 13. OUT of scope（显式）

- Dashboard summary API / Vue 管理看板（**veto: dashboard FE** — 下一叶）  
- 公司 IdP / OAuth / mTLS scrape  
- 强制独立 management port（可选，非必选）  
- 开启 backend HPA / 多副本 / Redis SSE relay / 默认 Redis rate-limit 完整交付  
- knip / 大范围 listAll 卫生（除非与 ADR 诚实补丁直接相关的最小交叉引用）  
- 前端功能/i18n/Playwright/UIUX  
- go-live / #3b GO / #5a GO / CD-3 / IBL 激活 / Formal phase 激活  

---

## 14. BDD readiness

| Field | Value |
| --- | --- |
| **bdd_readiness** | `ready` |
| **open_questions** | _（无阻塞项）_ |
| **owning_doc** | `docs/behavior/prod-ops-security-hardening.md`（本文件；worktree 路径） |
| **task_ids** | `["135"]` |
| **acceptance_scenario_ids** | `BDD-PRR-D01B-001` … `BDD-PRR-D01B-016` |
| **frontend_ui_in_scope** | `false` |
| **next** | `plan-orchestrator` → backend / deploy-doc implementers（TDD Red first；无 FE dashboard） |

### 非阻塞实现备注（非 pending questions）

- Basic 用户属性名/env 键由实现选择（如 `DOCGEN_ACTUATOR_SCRAPE_USERNAME` / `PASSWORD`）；验收只要求 secrets 驱动且无镜像默认口令。  
- SecurityFilterChain 拆分（Order）属实现细节；验收以 §4.1 矩阵为准。  
- CSP 具体指令表可在实现时按 pdf.js / Element Plus 静态资源需要微调，但不得回退到无 CSP。  
- `docs/README.md` 行为索引条目可 defer 至 stage 12 `post-task-doc-sync`（本 handoff 允许）。
