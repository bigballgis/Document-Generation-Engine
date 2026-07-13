# BDD 行为规格：LR-D7 — Durable security audit events

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-11  
**BDD ID**: `BDD-LRP-D7-SECURITY-AUDIT-001`  
**来源任务**: [LRP Wave LR-D § LR-D7 — Durable security audit events](../plan/detail/LRP-D-ops-observability.md)  
**程序**: [launch-readiness-program.md](../plan/launch-readiness-program.md) · Wave **LR-D**  
**Task Master / slice**: plan `LR-D7` / Task Master **#36** / slice `lrp-d7-durable-security-audit`  
**Worktree**: `D:/working/DGE-lrp-d7-durable-security-audit` · `feat/lrp-d7-durable-security-audit`  
**依赖**: **LR-D1 Done**（ADR-0048 Accepted；`management_audit_event` Tier-1 **90 天**硬删）  
**关闭缝隙**: ledger 「Security forbidden-route audit」（COR-P06 residual）  
**Plan note**: Wave LR-D / LR-D7 计划行状态由 MAIN 上 `plan-orchestrator` 维护；本规格为行为权威。

---

## 1. 概述

权限矩阵 §13.3 / §11 要求安全相关事件写入**安全审计摘要**，且可按审计查看范围查询。现状：

| 现状 | 证据 |
| --- | --- |
| SLF4J 日志齐全 | `SecurityAuditSummaryService` |
| 部分落库骨架已存在（SOR） | `SecurityManagementAuditRecorder` → `management_audit_event`；login success/failure/logout 已调用 |
| **403 / forbidden-route 未真正落库** | `recordRouteAccessDenied` **无生产调用方**；前端路由守卫仅本地保存 `traceId`（`session.recordRouteDeny`） |
| **下载拒绝未写入管理安全审计** | `DocumentDownloadService` 仅成功路径写 runtime 审计；`SecurityAuditSummaryService.recordDocumentDownload` 未接线 |
| 持久化失败语义未锁定 | 登录路径无 fail-safe 包裹 |
| Ledger 缝隙仍开 | 「Security forbidden-route audit」→ Log-only in some paths |

本切片在**不破坏既有日志行**的前提下，把计划范围内安全事件**可靠持久化**到既有 `management_audit_event`，纳入 **LR-D1 / ADR-0048** 留存，并经既有 Activity log / management audit 查询路径按矩阵 §10 / §13.3 作用域暴露。

| 行为域 | 摘要 |
| --- | --- |
| **D7-C1 存储** | **扩展既有** `management_audit_event` + `SecurityManagementAuditRecorder`（**不**新建专用表） |
| **D7-C2 事件** | 登录成功/失败；403 / forbidden-route；下载授予/拒绝（见 §4） |
| **D7-C3 日志** | **保留**全部现有 SLF4J 行；持久化为附加写 |
| **D7-C4 Fail-safe** | 持久化失败**不得**阻断登录（及同类主路径）；记 warn + 保留日志 |
| **D7-C5 查询** | 既有 management audit query/export；角色/组范围按矩阵 §10 |
| **D7-C6 留存** | 行写入 `management_audit_event` → 自动受 ADR-0048 **90 天**硬删约束 |
| **D7-C7 缝隙** | 同变更集将 ledger 「Security forbidden-route audit」标为 closed（附证据） |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 新建独立 security_audit 表 / 旁路留存 | **禁止** — 必须挂管理审计 + LR-D1 |
| 密码、token、secret、模板变量明文、完整下载 URL | **禁止**落库/日志（矩阵 §11） |
| 依赖或修改 `DGE-audit-governance` 工作树 | **禁止** |
| 会话续期成功/拒绝持久化 | **Out of scope** — 保持现有 log-only（LR-B6）；可后续切片 |
| 改变审计 UI 列布局 / 新控制台 | **Out of scope** — 复用 Activity log `eventType` 筛选 |
| 改变 runtime_generation_audit 语义 | 成功下载可**继续**写 runtime 审计；本切片另确保管理安全事件完整 |
| Tier-2 归档 / ADR-0048 改写 | **禁止** |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **管理端用户** | 任意已登录管理角色 | 触发 forbidden-route / 登录 |
| **未认证调用方** | 匿名 / 错误口令 | 触发登录失败审计 |
| **运行时 API 调用方** | 凭证 + access account | 触发文档下载授予/拒绝 |
| **审计管理员** | `AUDIT_ADMIN` | 可查询全部管理安全审计行 |
| **全局管理员** | `GLOBAL_ADMIN` | 同 AUDIT_ADMIN 的审计查看范围 |
| **分组管理员** | `GROUP_ADMIN` | 仅 `group_code` ∈ 授权组的行；**不可见** `group_code IS NULL` 的平台级安全行 |
| **其他管理角色** | `MASTER_DESIGNER` 等 | 无 `readAudit` → 查询 fail-closed |
| **系统** | `SecurityAuditSummaryService` + recorder | 写日志 + 持久化 |

---

## 3. Goal

1. 登录成功/失败产生可查询的耐久审计行（含主体摘要、outcome、traceId/auditId）。  
2. Forbidden-route / 管理端 403 产生耐久行（含主体摘要、`routeKey` 或入口标识、拒绝原因码、traceId）。  
3. 文档下载授予与拒绝产生耐久管理安全审计行（矩阵批准字段；无敏感明文）。  
4. 授权审计员可查；未授权角色不可见（fail-closed）。  
5. 持久化故障不阻断登录主路径。  
6. 新行自动进入 LR-D1 90 天留存；关闭 ledger 缝隙。

---

## 4. 已确认决策（confirmed-for-D7）

**授权依据：** LRP-D7 计划约束 + 权限矩阵 §10 / §11 / §13.3 + LR-D1/ADR-0048；本切片按「矩阵默认」冻结，**无阻塞待确认项**。

| ID | 决策 |
| --- | --- |
| **D7-C1** | **存储机制**：沿用 / 完善 `SecurityManagementAuditRecorder` → 表 `management_audit_event`（V9）。**不**新建 Flyway 业务表。若需枚举/索引注释可用下一 Flyway 版本，但**非**新事件表。 |
| **D7-C2** | **事件类型（UPPER_SNAKE_CASE）**：<br>• `SECURITY_LOGIN_SUCCESS`<br>• `SECURITY_LOGIN_FAILURE`<br>• `SECURITY_ROUTE_ACCESS_DENIED`<br>• `SECURITY_DOCUMENT_DOWNLOAD`（授予）<br>• `SECURITY_DOCUMENT_DOWNLOAD_DENIED`（拒绝 — **本切片须新增**）<br>既有 `SECURITY_LOGOUT` 若已落库则**保持**，不纳入本切片验收硬门槛。 |
| **D7-C3** | **字段基线（矩阵批准）**：`event_at`（UTC）、`event_type`、`actor_username`（V9 `VARCHAR(8)` 主体摘要 — 超长截断/消毒，与现 recorder 一致；v1 本地账号 ≤8）、`actor_summary`（固定短标签如 `Security audit` / `Runtime download`）、`status_summary`（outcome + 非敏感上下文，≤512）、`warning_codes` JSON 承载 `[traceId, auditId]`（及可选 `reasonCode`）、**禁止** password/token/secret/PII 超矩阵字段。 |
| **D7-C4** | **routeKey**：写入 `status_summary`（可读，如 `Route access denied: route.audit-console`）且/或结构化进 `warning_codes`；逻辑路由键优先于 raw path。 |
| **D7-C5** | **拒绝原因码（非敏感）**：路由 `ROUTE_NOT_VISIBLE`（前端守卫）/ `ACCESS_DENIED`（服务端 403）；下载 `DOWNLOAD_ACCESS_DENIED` / `DOWNLOAD_EXPIRED` / `DOWNLOAD_NOT_AVAILABLE`（对调用方保持既有 fail-closed 对外错误，不因审计码泄露资源存在性）。 |
| **D7-C6** | **`group_code` / `template_id`**：<br>• 登录 / 平台级路由拒绝：`group_code`/`template_id` = **null**（平台级，同 `AUDIT_RETENTION_PURGE` 可见性）。<br>• 下载授予/拒绝：若已知模板，写入 `template_id` + 模板 `group_code`，使 `GROUP_ADMIN` 在组范围内可见。 |
| **D7-C7** | **查询可见性（矩阵 §10）**：`AUDIT_ADMIN` / `GLOBAL_ADMIN` 可见全部安全事件；`GROUP_ADMIN` 仅见其 `groupScope` 匹配行（平台级 null 行**不可见**）；无 `readAudit` → 403/拒绝。导出同作用域 + 既有脱敏。 |
| **D7-C8** | **留存**：全部 `SECURITY_*` 行属 `management_audit_event` → **ADR-0048 / LR-D1 90 天硬删**；无需改 scheduler；测试锁定「新 event_type 仍被 cutoff 清理」。 |
| **D7-C9** | **Fail-safe（登录）**：`recordLoginSuccess` / `recordLoginFailure` 的**持久化**异常必须捕获：打 **warn**（含 event_type、traceId、异常类），**不**向上抛导致登录失败/成功被回滚；**SLF4J 审计日志行仍先写**。其它安全写路径（路由拒绝上报、下载审计）同样 fail-safe：审计写失败不得改变原业务 HTTP 结果（登录成功仍 200；下载拒绝仍原错误码）。 |
| **D7-C10** | **事务边界**：安全审计写使用**独立**短事务 / `REQUIRES_NEW`（或等价），避免与 `readOnly` 登录事务耦合导致意外回滚；实现锁定一种并测。 |
| **D7-C11** | **Forbidden-route 接线（关闭缝隙关键）**：<br>1. **前端**：路由守卫拒绝后（现有 `/forbidden` + `traceId`）必须调用已认证管理 API 上报（新建薄端点，如 `POST /api/management/v1/security-audit/route-access-denied`，body：`routeKey` + client `traceId`）；服务端用会话主体写 `SECURITY_ROUTE_ACCESS_DENIED`。<br>2. **服务端**：管理 API `AccessDeniedHandler` / 明确的授权 403 路径应调用同一 recorder（`routeKey` 可用 request path 摘要或 logical key；不得写入敏感 query）。<br>3. 上报 API 自身 fail-safe：失败返回 2xx 可忽略体或 204 + 服务端已 warn；**不得**因上报失败阻断 Forbidden 页展示。 |
| **D7-C12** | **下载授予/拒绝接线**：在 runtime 下载路径（`DocumentDownloadService` 及拒绝分支）调用 `SecurityAuditSummaryService`（或 recorder）写入 `SECURITY_DOCUMENT_DOWNLOAD` / `SECURITY_DOCUMENT_DOWNLOAD_DENIED`；**保留**现有 `RuntimeGenerationAuditRecorder.recordDocumentDownload` 成功路径。凭证仅指纹/externalId 摘要；access account 按 `VARCHAR(8)` 摘要规则。 |
| **D7-C13** | **日志不变**：现有 `security.audit.*` logger 消息格式保持；只追加 persistence。 |
| **D7-C14** | **前端 / E2E**：无新 UI 列 → **不强制** Playwright E2E / UIUX 作为 Done 硬门槛。须：前端 Vitest（守卫触发上报调用）+ 后端 `mvn verify`。可选 Docker Activity log 冒烟：`eventType=SECURITY_ROUTE_ACCESS_DENIED` 对 `AUDIT_ADMIN` 可见。 |
| **D7-C15** | **OpenAPI**：若新增 route-denied 上报端点，须进入 OpenAPI v1 + 统一 envelope；错误模型沿用现有。 |
| **D7-C16** | **Ledger**：实现验收后同变更集将 `execution-sync-ledger.md` 「Security forbidden-route audit」标 closed，exit evidence 指向本 BDD 场景 + 测试。 |

### 4.1 与 SOR 部分落库的关系

SOR-S03 / `b0933ca` 已引入 recorder 与 login 调用，但 **forbidden-route 生产路径未接线**，ledger 仍开。本切片**完成并证明**耐久闭环，不以「类已存在」宣称 Done。

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | `POST .../auth/login` 成功/失败 | 写 SUCCESS / FAILURE |
| T2 | 前端路由守卫判定 `canAccessRoute=false` | 跳转 Forbidden + 上报 API → `SECURITY_ROUTE_ACCESS_DENIED` |
| T3 | 管理 API 授权 403 | AccessDeniedHandler / 等价路径写同事件类型 |
| T4 | Runtime `.../documents/{documentId}/download` 成功 | `SECURITY_DOCUMENT_DOWNLOAD`（+ 既有 runtime 审计） |
| T5 | 同下载路径授权失败 / 过期 / 不可用 | `SECURITY_DOCUMENT_DOWNLOAD_DENIED` |
| T6 | 审计员 Activity log 筛选 `eventType` | 按角色作用域返回行 |

---

## 6. Preconditions

1. 管理端本地账户 + JWT（ADR-0036）；审计查询角色仅 `AUDIT_ADMIN` / `GLOBAL_ADMIN` / `GROUP_ADMIN`（矩阵 §10）。  
2. `management_audit_event` 与 LR-D1 retention 已上线。  
3. 前端 `visibleRoutes` / `canAccessRoute` 行为已存在（P1）。  
4. 不依赖 `DGE-audit-governance`。

---

## 7. Primary Journey

| # | 步骤 | 系统响应 |
| --- | --- | --- |
| 1 | 用户以错误密码登录 | 401 + 日志 + **耐久** `SECURITY_LOGIN_FAILURE`（username 摘要、outcome、traceId/auditId） |
| 2 | 用户登录成功 | 200 session + 日志 + **耐久** `SECURITY_LOGIN_SUCCESS` |
| 3 | 已登录无权限角色深链 `/audit` | Forbidden 页 + traceId；上报后 **耐久** `SECURITY_ROUTE_ACCESS_DENIED`（user + routeKey） |
| 4 | `AUDIT_ADMIN` 查询 management audit，`eventType=SECURITY_LOGIN_FAILURE` | 可见对应行 |
| 5 | `TEMPLATE_AUTHOR`（无 readAudit）查询审计 API | 拒绝（403 / 既有 access denied） |
| 6 | Runtime 下载被拒 | 既有错误响应 + **耐久** `SECURITY_DOCUMENT_DOWNLOAD_DENIED` |
| 7 | 超 90 天安全审计行 | LR-D1 cleanup 硬删（回归断言） |

---

## 8. System Responses

### 8.1 成功路径

- 每个范围内事件：先（或同步）写 SLF4J，再持久化一行 `management_audit_event`。  
- 查询：授权角色经既有 `AuditQueryService` / Activity log 可见匹配 `event_type`。  
- 导出：既有脱敏格式；安全事件不引入新明文列。

### 8.2 Fail-closed / Fail-safe

| 场景 | 行为 |
| --- | --- |
| 无 `readAudit` 查询 | 拒绝；不可枚举他人安全事件 |
| `GROUP_ADMIN` 查平台级安全行 | 不可见（`group_code` null） |
| DB 持久化失败 @ 登录 | 登录主路径成功/失败语义不变；warn 日志；SLF4J 安全摘要仍在 |
| 上报 API 失败 @ Forbidden | 用户仍见 Forbidden；不抛到 UI 致命错误 |
| 审计写失败 @ 下载拒绝 | 原下载错误码不变 |

---

## 9. Acceptance Scenarios（Given / When / Then）

### BDD-LRP-D7-001 — 登录失败耐久行 + 授权可见性

- **Given** 系统正常；用户名 `10000001`（或种子账号）密码错误；`AUDIT_ADMIN` 与无 `readAudit` 角色均可用于查询对照  
- **When** 登录失败发生  
- **Then** 存在 `management_audit_event` 行：`event_type=SECURITY_LOGIN_FAILURE`，`actor_username` 为主体摘要（对应该用户名规则），`status_summary` 表明 failure，`warning_codes`（或等价）含 `traceId`  
- **And** `AUDIT_ADMIN`（或 `GLOBAL_ADMIN`）经 management audit 查询可见该行  
- **And** 无 `readAudit` 的角色查询被拒绝或不可见该行  
- **And** 现有 `security.audit.login.failure` 日志行仍出现

### BDD-LRP-D7-002 — 登录成功耐久行

- **Given** 合法管理账号  
- **When** 登录成功  
- **Then** 写入 `SECURITY_LOGIN_SUCCESS` 耐久行（username 摘要、outcome、traceId/auditId）  
- **And** 登录响应成功不被审计写阻塞

### BDD-LRP-D7-003 — Forbidden-route 耐久行（缝隙关闭）

- **Given** 已登录 `TEMPLATE_AUTHOR`（或其它无 `route.audit-console` 的角色）  
- **When** 深链进入审计控制台路由并被守卫拒绝（或等价触发上报）  
- **Then** 写入 `SECURITY_ROUTE_ACCESS_DENIED`，含主体摘要 + `routeKey`（如 `route.audit-console`）+ 原因码 + traceId  
- **And** `AUDIT_ADMIN` 可查询到该行  
- **And** ledger 「Security forbidden-route audit」在交付变更集中标为 **closed**，证据引用本场景测试

### BDD-LRP-D7-004 — 服务端管理 403 亦落库

- **Given** 已认证会话调用其无权的管理 API（触发 AccessDenied / 403）  
- **When** 403 响应写出  
- **Then** 写入 `SECURITY_ROUTE_ACCESS_DENIED`（或同类型入口拒绝事件），含主体与入口摘要 + `ACCESS_DENIED`  
- **And** 响应体仍为统一无权消息（不泄露资源细节）

### BDD-LRP-D7-005 — 下载授予耐久行

- **Given** 授权 runtime 会话与未过期 `documentId`  
- **When** 下载成功  
- **Then** 存在 `SECURITY_DOCUMENT_DOWNLOAD` 管理审计行（凭证指纹摘要、access account 摘要、documentId、template 标识、traceId）；无完整下载 URL / secret  
- **And** 若模板已知，`group_code` 已填充

### BDD-LRP-D7-006 — 下载拒绝耐久行

- **Given** 跨模板或过期或不存在的下载尝试（按现有 fail-closed）  
- **When** 下载被拒绝  
- **Then** 存在 `SECURITY_DOCUMENT_DOWNLOAD_DENIED`，含 reasonCode 族（`DOWNLOAD_ACCESS_DENIED` / `DOWNLOAD_EXPIRED` / `DOWNLOAD_NOT_AVAILABLE`）与 traceId  
- **And** 对外错误语义与现网一致（不因审计增强而泄露未授权文档存在性）

### BDD-LRP-D7-007 — 持久化失败不阻断登录（fail-safe）

- **Given** recorder/repository 被注入为抛错（测试桩）  
- **When** 登录成功或失败路径执行  
- **Then** 登录业务结果仍按口令对错返回（成功签发 / 失败 401）  
- **And** 出现 warn 级持久化失败日志  
- **And** SLF4J `security.audit.login.*` 行仍写出

### BDD-LRP-D7-008 — 组范围 fail-closed

- **Given** 平台级 `SECURITY_LOGIN_FAILURE`（`group_code` null）与一带 `group_code=G1` 的下载安全行  
- **When** `GROUP_ADMIN`（仅授权 G1）带 `groupScope=G1` 查询  
- **Then** 可见 G1 下载安全行（若测试造数）  
- **And** 不可见平台级 login 行  
- **When** 无 `readAudit` 角色查询  
- **Then** 拒绝

### BDD-LRP-D7-009 — 纳入 LR-D1 留存

- **Given** `SECURITY_ROUTE_ACCESS_DENIED`（或任一 `SECURITY_*`）行的 `event_at` 早于 management cutoff  
- **When** LR-D1 cleanup 运行  
- **Then** 该行被硬删；窗口内安全行保留

### BDD-LRP-D7-010 — 敏感字段禁令

- **Given** 任意本切片安全事件写路径  
- **When** 检查持久化载荷与日志参数  
- **Then** 不含 password、raw token/secret、完整下载 URL、模板变量原值或未授权组详情

---

## 10. Boundary / Exception

| 边界 | 行为 |
| --- | --- |
| 空/未知 username 登录失败 | `actor_username` 使用消毒占位（现有 `00000000` 规则可保留） |
| `actor_username` > 8 字符 | 截断至 8（V9 约束）；不以扩列为本切片范围 |
| 重复 Forbidden 上报 | 允许每拒绝一次一行（不要求去重） |
| 未认证调用上报 API | 401；不写「伪造成功」审计 |
| Session renewal 事件 | 仍仅日志（非 D7 验收） |

---

## 11. Observable evidence

| 证据 | 证明内容 |
| --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | 门禁绿；含 001–009 类测试 |
| 集成/单测 | 登录失败行、route denied 行、下载拒绝行、fail-safe、组范围、retention |
| 前端 Vitest | 守卫拒绝触发上报 API（mock） |
| Activity log（可选冒烟） | `AUDIT_ADMIN` 筛选新 `eventType` |
| Ledger 行 | 「Security forbidden-route audit」→ closed |
| SLF4J | 既有 `security.audit.*` 模式保留 |

---

## 12. Frontend / E2E 要求

| 项 | 要求 |
| --- | --- |
| 前端代码 | **需要**（Forbidden 上报调用；无新可见控件/列则不做 UIUX 切片） |
| Playwright E2E | **不强制**（计划：仅当 UI columns 变更才加 audit console smoke + LR-C 门禁） |
| UIUX reviewer | **不强制** |
| 后端门禁 | **强制** `mvn verify` |

---

## 13. 追溯性（Source-of-Truth）

| 文档 | 关系 |
| --- | --- |
| **本文件** | **行为权威**（D7 confirmed） |
| [permission-matrix.md](../security/permission-matrix.md) §10 / §11 / §13.3 | 审计查看范围、敏感字段、forbidden-route 摘要义务 |
| [ADR-0048](../adr/operations/0048-audit-data-retention-policy.md) | management 90 天硬删 |
| [lrp-d1-audit-retention.md](./lrp-d1-audit-retention.md) | 留存行为；本切片事件自动加入 |
| [LRP-D-ops-observability.md](../plan/detail/LRP-D-ops-observability.md) § LR-D7 | 计划验收提纲 |
| [execution-sync-ledger.md](../plan/execution-sync-ledger.md) | 缝隙关闭目标 |
| [audit-admin-query-journey.md](./audit-admin-query-journey.md) | Activity log 查询既有行为（不重开） |
| V9 `management_audit_event` | 物理模型 |
| `SecurityAuditSummaryService` / `SecurityManagementAuditRecorder` | 实现锚点 |

---

## 14. 待确认问题（Pending）

**无阻塞项。** 下列为实现对齐项（不阻断 `ready`）：

| ID | 问题 | 默认（confirmed-for-D7） |
| --- | --- | --- |
| D7-Q1 | 上报 API 精确路径/动词 | `POST /api/management/v1/security-audit/route-access-denied`（实现可微调，测试锁定） |
| D7-Q2 | `warning_codes` vs 专用 JSON 列 | **不扩列**；继续用 `warning_codes` / `status_summary` |
| D7-Q3 | 服务端 403 的 routeKey 来源 | request servlet path 脱敏摘要或 logical header；禁止敏感 query |
| D7-Q4 | logout / renewal 是否升级耐久 | logout 已有则保持；renewal **仍 log-only** |

---

## 15. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/lrp-d7-durable-security-audit.md
task_ids: [LR-D7, #36]
```

**Handoff:** `backend-engineer`（+ 少量 frontend 上报接线）在 feature worktree 实施；勿触碰 MAIN / `DGE-audit-governance`。
