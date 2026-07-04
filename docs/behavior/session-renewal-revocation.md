# BDD 行为规格：会话滑动续期与撤销（LR-B6）

**文件状态**: `ready`（会话策略已由用户于 **2026-07-04** 明确确认——「按这个实现吧」）
**BDD ID**: `BDD-LRP-SESSION-001`
**版本**: 1.0.0
**编写日期**: 2026-07-04
**来源任务**: [LRP Wave LR-B § LR-B6 — Session renewal + revocation](../plan/detail/LRP-B-runtime-scaleout-session.md)
**治理坑位**: [CD-PIT-13 — Hard JWT expiry loses authoring work](../plan/detail/CDP-industry-pitfall-registry.md)

---

## 0. 用户已确认的会话策略（2026-07-04，source of truth）

以下四条为**已确认需求**，本规格全部行为由此推导，语义不得偏离：

| # | 已确认策略 | 含义 |
| --- | --- | --- |
| P1 | **滑动续期** | 用户活动即自动展期；**不引入**显式 refresh-token 令牌对 |
| P2 | **访问令牌 TTL 30 分钟** | 维持现状 `docgen.jwt.access-token-ttl: PT30M`，不以延长 TTL 作为「修复」 |
| P3 | **绝对会话上限 8 小时** | 自**首次登录**起计；续期不得使会话超过该上限 |
| P4 | **Redis 撤销名单，fail-closed** | logout 写入撤销名单；令牌校验时 Redis 不可用 → **拒绝令牌（401）** |

由 P3「续期不得超过」推导的关键裁剪规则：**续期签发的新令牌过期时间 = min(now + 30min, sessionStartedAt + 8h)**；当 `now - sessionStartedAt ≥ 8h` 时拒绝续期。绝对上限完全由服务端时钟裁决。

---

## 1. 概述

当前管理端令牌为 `PT30M` 硬过期、无续期、logout 仅记审计（`ManagementAuthService.logout` L69–71 log-only），导致：编辑 30 分钟后任意请求 401、未保存工作丢失（CD-PIT-13）；且 logout 后旧令牌在剩余寿命内仍可重放。

本规格定义两个行为域：

| # | 行为域 | 摘要 |
| --- | --- | --- |
| D1 | **滑动续期** | 新增 `POST /api/management/v1/auth/renew`；活动用户在令牌临过期时由前端静默续期，不打断编辑；8 小时绝对上限拒绝续期并引导重新登录 |
| D2 | **撤销** | 管理令牌携带 `jti`；logout 与续期均将旧 `jti` 写入 Redis 撤销名单（TTL = 剩余令牌寿命）；`JwtAuthenticationFilter` 逐请求校验；Redis 不可用一律 fail-closed 401 |

边界锁定：仅覆盖**管理端本地账户会话**（ADR-0036 边界内）。运行时 API 凭证体系（`ApiCredentialAuthenticationFilter`）、SSO/OIDC、remember-me、并发会话数限制均不在范围内（见 §13）。

---

## 2. Actor / Role

会话行为对**全部管理端角色**一致生效（无角色差异）：

| Actor | 角色代码 | 说明 |
| --- | --- | --- |
| 全局管理员 | `GLOBAL_ADMIN` | 全部场景适用 |
| 分组管理员 | `GROUP_ADMIN` | 同上 |
| 母版设计人员 | `MASTER_DESIGNER` | 同上 |
| 模板编排人员 | `TEMPLATE_AUTHOR` | 长编辑会话的主要受益者（CD-PIT-13 主诉） |
| 测试人员 | `TEMPLATE_TESTER` | 同上 |
| 审批人员 | `TEMPLATE_APPROVER` | 同上 |
| 审计管理员 | `AUDIT_ADMIN` | 同上；另作为撤销/续期审计记录的查看方 |

非人类参与者：**前端续期调度器**（浏览器内定时评估，无独立身份）、**Redis 撤销名单**（基础设施）。

---

## 3. Goal（用户目标）

1. **持续工作不被打断**：只要用户保持活动且未达 8 小时上限，登录状态自动延续，编辑中的表单/编辑器状态不因令牌到期丢失。
2. **登出立即生效**：logout 后旧登录状态不可再用于任何管理操作（对抗令牌重放），而非仅记一条日志。
3. **上限可预期**：会话临近 8 小时上限时用户获得明确的保存-并-重新登录引导，而不是突然被踢出。
4. **失效安全**：撤销校验依赖（Redis）不可用时系统宁可拒绝也不放行（fail-closed，P4）。

---

## 4. Trigger（触发条件）

| # | 触发者 | 触发事件 | 进入行为 |
| --- | --- | --- | --- |
| T1 | 前端续期调度器 | 已认证 + 令牌剩余寿命 < 续期窗口（默认 5 分钟，`[ASSUMED-THRESHOLDS]`）+ 近期有用户活动 | 静默调用 `POST /auth/renew`（D1） |
| T2 | 后端 renew 端点 | 收到携有效未撤销令牌的续期请求且 `now - sessionStartedAt ≥ 8h` | 拒绝续期 401（D1） |
| T3 | 用户 | 点击退出登录（`POST /auth/logout`） | 写撤销名单（D2） |
| T4 | 后端 renew 端点 | 续期成功签发新令牌 | 旧 `jti` 写撤销名单（D2，防旧令牌复用） |
| T5 | `JwtAuthenticationFilter` | 任意携 Bearer 令牌的管理端请求 | 逐请求撤销校验（D2） |
| T6 | 前端会话状态评估 | `absoluteSessionExpiresAt - now` < 提醒窗口（默认 10 分钟，`[ASSUMED-THRESHOLDS]`） | 展示不可续期提醒并停止静默续期（D1） |

---

## 5. Preconditions（前置条件）

- 用户为启用中（enabled、未逻辑删除）的本地管理账户，已通过 `POST /auth/login` 建立会话（登录即签发含 `jti` + `sessionStartedAt` claim 的令牌）。
- 前端令牌存于 `localStorage`（key `docgen.accessToken`），axios 请求拦截器逐请求读取（现状，同源多 tab 共享）。
- dev/prod 拓扑中 Redis 可达（`docker-compose.yml` 已含 `docgen-redis`）；test profile 无 Redis（见 §9 边界 B7）。
- COR-F03 已交付：401 → 清会话 → `/login?sessionExpired=1&redirect=<原路径>`，登录后回跳原页面（`frontend/src/api/http.ts`）。

---

## 6. Primary journey（主旅程：模板编排人员的一个工作日）

1. **09:00** alice 登录。签发令牌：TTL 30 分钟、`jti=J1`、`sessionStartedAt=09:00`；会话视图含 `expiresAt=09:30`、`absoluteSessionExpiresAt=17:00`。
2. alice 在结构化编辑器中持续编辑。**09:26**（剩余 < 5 分钟且有活动）前端静默调用 renew → 新令牌 `J2`（09:56 过期，`sessionStartedAt` 仍为 09:00），`J1` 同时被撤销。编辑不中断、无任何 UI 打扰。
3. 全天重复步骤 2（约每 25–30 分钟一次），会话随活动滑动延续。
4. **16:50**（距绝对上限 < 10 分钟）前端停止静默续期，展示非阻断提醒：「会话即将结束——请保存工作后重新登录」。alice 保存工作。
5. **17:00 后** 首个请求/续期尝试返回 401（`SESSION_ABSOLUTE_LIMIT_REACHED`）→ 既有 401 流程跳转登录页（目的地保留）。alice 重新登录 → 新会话 `sessionStartedAt` 重置为当前时间，回到原页面。
6. （任意时刻）alice 点击退出登录 → `J*` 立即写入撤销名单；此后任何持旧令牌的重放请求均 401。

---

## 7. System responses（系统响应，成功路径）

| 触发 | 系统响应 |
| --- | --- |
| `POST /auth/login` 成功 | 200 统一信封；令牌含 `jti`（全新 UUID）+ `sessionStartedAt=now`；session 视图新增 `absoluteSessionExpiresAt = sessionStartedAt + 8h`；审计 `recordLoginSuccess`（现状保留） |
| `POST /auth/renew`（有效未撤销令牌，未达上限） | 200 统一信封，result 结构同 login（`accessToken`/`tokenType`/`session`）；新令牌 `jti` 换新、`sessionStartedAt` 继承、过期时间 = min(now+30min, 上限)；旧 `jti` 写撤销名单（TTL=旧令牌剩余寿命）；重新校验账户仍启用并重新派生角色/分组/路由 claims（`[ASSUMED-RENEW-REDERIVE]`）；审计续期事件（`[ASSUMED-RENEW-AUDIT]`） |
| `POST /auth/logout` | 204；`jti` 写入 Redis 撤销名单（TTL=剩余令牌寿命）；审计 `recordLogout`（现状保留） |
| 携未撤销有效令牌的任意管理请求 | 过滤器解析签名/有效期 → 查撤销名单未命中 → 放行（建立 `ManagementAuthentication`） |
| 前端静默续期成功 | 替换 `localStorage` 令牌；Pinia session 状态更新 `expiresAt`/`absoluteSessionExpiresAt`；**无路由跳转、无对话框、不触碰表单/编辑器状态** |
| 前端临上限评估命中 | 展示非阻断提醒（标题/正文/动作按钮，i18n key 见 §12）；停止后续静默续期调度 |

---

## 8. Acceptance scenarios（Given / When / Then）

共 **8** 个验收场景。错误信封一律遵循统一错误模型（`code` + `category` + `retryable` + `message`(en) + `messageKey`）。

### 8.1 SCEN-RENEW-01 — 正常滑动续期成功

```gherkin
Given alice（任一管理端角色）于 09:00 登录，令牌 jti=J1、sessionStartedAt=09:00、09:30 过期
And   当前时间 09:26（剩余 < 5 分钟），Redis 可用，J1 未被撤销
When  携 J1 调用 POST /api/management/v1/auth/renew
Then  响应 200 统一信封，result 含新 accessToken（jti=J2）
And   J2 的 sessionStartedAt 仍为 09:00（继承），过期时间 ≈ 09:56（min(now+30min, 17:00)）
And   J1 被写入撤销名单，Redis key TTL ≈ 4 分钟（J1 剩余寿命）
And   session 视图返回 expiresAt ≈ 09:56、absoluteSessionExpiresAt = 17:00
And   身份/角色/分组 claims 与账户当前状态一致
```

### 8.2 SCEN-RENEW-02 — 到达 8 小时绝对上限拒绝续期，前端引导重新登录

```gherkin
Given alice 自 09:00 首次登录起持续活动续期（sessionStartedAt 始终为 09:00）
And   当前时间 17:01（now - sessionStartedAt ≥ 8h），当前令牌本身仍在 30 分钟寿命内
When  前端调用 POST /auth/renew
Then  响应 401，error.code = SESSION_ABSOLUTE_LIMIT_REACHED，category = AUTHENTICATION，retryable = false
And   error.messageKey = api.error.authentication.sessionAbsoluteLimitReached（专用，独立于 sessionExpired）
And   前端进入既有 401 流程（COR-F03）：清空本地会话 → /login?sessionExpired=1&redirect=<原页面路径>
And   alice 重新登录后回到原页面，新会话 sessionStartedAt 重置为本次登录时间（上限重新起算）
```

### 8.3 SCEN-REVOKE-01 — logout 后旧令牌重放被 401

```gherkin
Given alice 已登录，令牌 jti=J1 剩余约 20 分钟寿命
When  alice 调用 POST /auth/logout（响应 204）
And   之后任何一方携 J1 请求任意受保护管理端点（如 GET /auth/session）
Then  J1 已存在于 Redis 撤销名单（key 前缀 docgen:session:revoked:，TTL ≈ 20 分钟）
And   重放请求响应 401，error.code = SESSION_REVOKED，messageKey = api.error.authentication.sessionRevoked
And   审计中存在 alice 的 logout 记录（现有 recordLogout 保留）——logout 从 log-only 变为真实失效
```

### 8.4 SCEN-REVOKE-02 — 续期后旧令牌重放被 401

```gherkin
Given alice 成功完成一次续期：旧令牌 jti=J1 → 新令牌 jti=J2
When  携 J1（签名有效、未过期）请求任意受保护管理端点
Then  响应 401，error.code = SESSION_REVOKED（J1 已随续期写入撤销名单）
And   携 J2 的同一请求响应 200
And   同一原始登录会话在任意时刻至多一个有效令牌（旧令牌不可与新令牌并行使用）
```

### 8.5 SCEN-FAILCLOSED-01 — Redis 不可用时任意携令牌请求被 401（fail-closed）

```gherkin
Given Redis 不可用（连接拒绝或超时），alice 持签名有效、未过期、从未被撤销的令牌
When  携该令牌请求任意受保护管理端点（含 POST /auth/renew）
Then  响应 401，error.code = SESSION_VALIDATION_UNAVAILABLE，category = AUTHENTICATION，retryable = true
And   error.messageKey = api.error.authentication.sessionValidationUnavailable（独立可观测，
      与 SESSION_EXPIRED / SESSION_REVOKED 可区分，供监控按 code 维度告警）
And   后端输出 WARN/ERROR 日志（含 traceId），不存在任何「撤销校验失败仍放行」的代码路径
And   （P4 用户确认 2026-07-04：宁可拒绝不放行；Redis 恢复后无需人工干预自动恢复放行）
```

### 8.6 SCEN-UX-01 — 静默续期不打断编辑（表单状态不丢）

```gherkin
Given alice 在结构化编辑器中有未保存的表单内容与光标位置
And   令牌剩余寿命 < 5 分钟，且近期存在用户活动（[ASSUMED-ACTIVITY-DEFINITION]）
When  前端续期调度器静默调用 renew 并成功
Then  localStorage 中 docgen.accessToken 被替换为新令牌，后续请求自动携新令牌
And   无路由跳转、无阻断性对话框、无焦点抢占；编辑器内容与光标状态逐字节不变
And   续期对用户完全无感（仅开发者工具网络面板可见 renew 请求）
```

### 8.7 SCEN-UX-02 — 临上限提醒展示与 i18n

```gherkin
Given alice 的 absoluteSessionExpiresAt - now < 10 分钟（会话临近 8 小时上限）
When  前端下一次会话状态评估触发
Then  展示非阻断提醒（notification/banner）：
      标题 = session.absoluteLimitReminder.title
      正文 = session.absoluteLimitReminder.message
      动作 = session.absoluteLimitReminder.action（重新登录入口）
And   en 环境展示英文基线文案，zh-CN 环境展示中文文案（逐 key 见 §12 L1 copy 表）
And   文案不出现 token / JWT 等技术词——一律使用「登录状态 / 会话」（en：sign-in session / session）
And   提醒不自动登出、不阻塞操作；前端同时停止静默续期调度，到达上限后由 8.2 的 401 流程接管
```

### 8.8 SCEN-CONCURRENT-01 — 双 tab 并发续期（已接受的边界行为）

```gherkin
Given alice 在两个 tab（A、B）打开管理端，共享同一 localStorage 令牌（jti=J1），均临近续期窗口
When  tab A 先成功续期（J1 → J2，J1 随即被撤销）
And   tab B 同时存在携 J1 的 in-flight 请求（或并发发出的 renew）
Then  tab B 的该请求响应 401（SESSION_REVOKED）
And   tab B 进入既有 401 流程（COR-F03）：清空共享会话并跳转 /login?sessionExpired=1&redirect=<tab B 路径>
      ——此竞态为**已接受**行为（本规格明确写入，不视为缺陷）；tab A 亦随共享会话清空而需重新登录
And   前端续期调度器 SHOULD 通过跨 tab 去重（如 localStorage 最近续期时间戳 + 抖动）压低竞态概率，
      但不承诺消除（[ASSUMED-NO-GRACE-WINDOW]：v1 不为旧 jti 设置撤销宽限期）
```

---

## 9. 边界与异常行为

| # | 场景 | 系统行为 |
| --- | --- | --- |
| B1 | renew 请求无 Authorization 头 | 401，error.code = SESSION_EXPIRED（现有认证入口点语义不变） |
| B2 | renew 携**已过期**令牌 | 401 SESSION_EXPIRED——滑动续期**不能复活**已过期会话；无活动放置 ≥30 分钟即等效「闲置登出」，必须重新登录（P1+P2 的自然推论） |
| B3 | renew 时账户已被停用/逻辑删除 | 401 SESSION_EXPIRED——续期**必须**重新校验账户仍启用（fail-closed 原则；否则滑动续期会把停用账户的暴露窗口从 ≤30 分钟拉长到 8 小时） |
| B4 | 续期临近上限的 TTL 裁剪 | 新令牌过期时间被裁剪为 `sessionStartedAt + 8h`（如 07:56h 处续期仅得 4 分钟令牌）；前端在提醒窗口内已停止静默续期，不产生续期忙循环 |
| B5 | logout 携已撤销/已过期令牌 | 401（对用户等效幂等：前端本就无条件清本地会话——现状保留） |
| B6 | logout 时 Redis 不可用（撤销写入失败） | 返回 5xx 统一信封（`[ASSUMED-LOGOUT-REDIS-DOWN]` 默认 503，retryable=true）；前端仍清空本地会话（现状保留）。残余风险有界：宕机期间校验侧 fail-closed 全拒（8.5），恢复后未写入撤销的旧令牌至多存活剩余 TTL ≤ 30 分钟 |
| B7 | test profile（无 Redis，`RedisAutoConfiguration` 已排除） | 撤销名单走同一端口接口的**内存实现**（沿用 idempotency `cache` 取值 `redis` / `memory` 的既有模式，配置建议 `docgen.session.revocation-store: memory`），**标注 transitional-test-only**；语义（TTL 淘汰、撤销命中）与 Redis 实现一致，fail-closed 路径以抛错桩单测覆盖。**禁止**在 prod 环境使用内存实现（LR-B6 Do-NOT：撤销必须挺过实例重启）；建议启动守卫：`docgen.environment=prod` 且 store=memory 时拒绝启动 |
| B8 | 发布切换期的存量旧令牌（无 `jti`/`sessionStartedAt` claim） | 视为无效 → 401 SESSION_EXPIRED，用户一次性重新登录即可（最大打扰窗口 = 旧令牌剩余寿命 ≤ 30 分钟）；不做兼容放行（`[ASSUMED-LEGACY-TOKEN]`） |
| B9 | 时钟基准 | 8 小时上限、TTL 裁剪、撤销 TTL 全部以**服务端时钟**裁决；前端提醒/续期窗口只依赖服务端下发的 `expiresAt` / `absoluteSessionExpiresAt`，不解析令牌、不信任本地时钟 |
| B10 | Redis 宕机期间的登录 | login 不查撤销名单，仍可签发；但下一个受保护请求即 401（8.5）——宕机期间管理端实际不可用，**by design**（P4），以 SESSION_VALIDATION_UNAVAILABLE 占比作为告警信号 |
| B11 | 401 后的登录页文案 | 静默续期失败（含 8.2 上限拒绝）走统一 401 流程，登录页横幅复用现有 `login.sessionExpired`；上限场景的主要用户沟通由 8.7 事前提醒承担，错误信封中的专用 messageKey 供 toast/观测区分 |

---

## 10. 可观测证据

| 证据类型 | 描述 |
| --- | --- |
| **HTTP 响应** | `POST /auth/renew` 200 信封：新 `accessToken` + `session.expiresAt`（滑动后）+ `session.absoluteSessionExpiresAt`（恒定 = 首次登录 + 8h） |
| **HTTP 响应** | 401 信封按场景携带可区分 code：`SESSION_REVOKED` / `SESSION_ABSOLUTE_LIMIT_REACHED` / `SESSION_VALIDATION_UNAVAILABLE` / `SESSION_EXPIRED`，均含 `metadata.traceId` |
| **Redis** | `redis-cli KEYS 'docgen:session:revoked:*'` 可见撤销条目；`TTL` 与旧令牌剩余寿命一致（logout 与续期两条路径均产生） |
| **后端日志** | Redis 不可用触发 fail-closed 时输出 WARN/ERROR（含 traceId）；可按 error.code 维度做监控告警 |
| **审计** | 登录/登出审计记录（现状保留）；续期审计事件（`[ASSUMED-RENEW-AUDIT]`），审计管理员可查 |
| **JWT claims** | 解码管理令牌可见 `jti`（每令牌唯一）与 `sessionStartedAt`（续期链上恒定）；`exp - sessionStartedAt ≤ 8h` 恒成立（B4 裁剪保证） |
| **前端状态** | 续期后 `localStorage['docgen.accessToken']` 值变化；Pinia session 的 `expiresAt` 前移；表单/编辑器 DOM 状态不变（8.6） |
| **前端 UI** | 临上限提醒可见且 en/zh-CN 文案与 §12 逐 key 一致（e2e-uiux-reviewer 取证对象） |
| **自动化测试** | 后端：`JwtTokenService` claim 测试、renew 端点测试（成功/上限/撤销/账户停用）、过滤器撤销 + fail-closed 测试、logout 撤销测试；前端：续期调度与提醒组件单测；E2E：Docker 4173 Playwright 会话旅程（`pnpm -C frontend exec playwright test <LR-B6 spec> --config playwright.docker.config.ts`） |

---

## 11. 待确认的 [ASSUMED] 假设项

以下为可调默认值/实现取向，**均不阻塞核心路径**；已按仓库现状选定默认，实施中如需变更须回写本规格：

| ID | 假设内容 | 默认推断 | 影响范围 |
| --- | --- | --- | --- |
| `[ASSUMED-THRESHOLDS]` | 续期窗口与提醒窗口具体数值（用户原话为「如剩 <5min」「如剩 <10min」，语义已确认、数值为建议值） | 续期窗口 5 分钟、提醒窗口 10 分钟，前端常量可配 | T1/T6、8.1/8.6/8.7 |
| `[ASSUMED-ACTIVITY-DEFINITION]` | 「用户有活动」的判定 | 最近 5 分钟内存在指针/键盘事件或业务 API 请求（路由跳转计入）；纯挂机不续期 | 8.6、B2 |
| `[ASSUMED-RENEW-REDERIVE]` | 续期是否重新派生全量授权 claims | 是——按 login 同路径重查 DB（账户启用校验为 MUST，见 B3；角色/分组/路由重新派生为默认取向，使会话中途的授权变更最迟 30 分钟内生效） | 8.1、B3 |
| `[ASSUMED-RENEW-AUDIT]` | 续期是否记审计 | 记——经 `SecurityAuditSummaryService` 与 login/logout 同通道（约每用户每 25–30 分钟一条，量可接受） | §10 审计 |
| `[ASSUMED-LOGOUT-REDIS-DOWN]` | logout 撤销写入失败的响应码 | 503 统一信封（retryable=true）；前端行为不变（仍清本地会话） | B6 |
| `[ASSUMED-NO-GRACE-WINDOW]` | 续期后旧 `jti` 是否留撤销宽限期 | 不留（v1 立即撤销，接受 8.8 竞态）；如实测多 tab 打扰显著，再议 30 秒级宽限并回写本规格 | 8.4/8.8 |
| `[ASSUMED-CONFIG-NAMES]` | 新配置项命名 | `docgen.session.absolute-ttl: PT8H`（env 可覆盖）、`docgen.session.revocation-store` 取值 `redis`（默认）/ `memory`、Redis 前缀 `docgen:session:revoked:` | §14 |
| `[ASSUMED-LEGACY-TOKEN]` | 发布切换期旧令牌处理 | 直接拒绝（401 SESSION_EXPIRED），一次性重登录 | B8 |

---

## 12. L1 copy 表（en 基线 + zh-CN）

约束（i18n-english-first + business-terminology-guide）：key 稳定、en 为基线、zh-CN 追加；**L1 文案禁用 token / JWT**，一律「登录状态 / 会话」（en：sign-in session / session）。

### 12.1 后端错误 messageKey（`backend/src/main/resources/i18n/messages_en.properties`，每 code 一条稳定英文）

| messageKey | error.code | HTTP | retryable | en 基线 |
| --- | --- | --- | --- | --- |
| `api.error.authentication.sessionAbsoluteLimitReached` | `SESSION_ABSOLUTE_LIMIT_REACHED`（新增） | 401 | false | Your sign-in session has reached its maximum duration. Please sign in again. |
| `api.error.authentication.sessionRevoked` | `SESSION_REVOKED`（新增） | 401 | false | Your session is no longer valid. Please sign in again. |
| `api.error.authentication.sessionValidationUnavailable` | `SESSION_VALIDATION_UNAVAILABLE`（新增） | 401 | true | We are unable to verify your session right now. Please try again later. |
| `api.error.authentication.sessionExpired`（已有，复用） | `SESSION_EXPIRED` | 401 | false | Your session has expired. Please sign in again. |

### 12.2 前端 API 错误目录（`apiErrorEn.ts` / `apiErrorZhCn.ts` 各新增 3 条镜像）

| key（en/zh 目录同 key） | en 基线 | zh-CN |
| --- | --- | --- |
| `authentication.sessionAbsoluteLimitReached` | Your sign-in session has reached its maximum duration. Please sign in again. | 您的登录会话已达到最长使用时限，请重新登录。 |
| `authentication.sessionRevoked` | Your session is no longer valid. Please sign in again. | 您的登录状态已失效，请重新登录。 |
| `authentication.sessionValidationUnavailable` | We are unable to verify your session right now. Please try again later. | 当前无法核实您的登录状态，请稍后重试。 |

### 12.3 前端 UI copy（`frontend/src/i18n/locales/en.ts` / `zh-CN.ts`）

| key | en 基线 | zh-CN | Surface |
| --- | --- | --- | --- |
| `session.absoluteLimitReminder.title`（新增） | Session ending soon | 会话即将结束 | 临上限非阻断提醒标题（8.7） |
| `session.absoluteLimitReminder.message`（新增） | Your sign-in session is about to reach its time limit. Please save your work, then sign in again to continue. | 您的登录会话即将到达时长上限。请先保存当前工作，然后重新登录以继续使用。 | 提醒正文（8.7） |
| `session.absoluteLimitReminder.action`（新增） | Sign in again | 重新登录 | 提醒动作按钮（8.7） |
| `login.sessionExpired`（已有，复用） | Your session has expired. Please sign in again. | 会话已过期，请重新登录。 | 401 重定向后登录页横幅（8.2/8.8、B11） |

---

## 13. Out of scope（本规格明确不覆盖）

- **SSO / OIDC / 外部 IdP**——本地账户 + JWT 仍是 v1 认证边界（ADR-0036；launch-readiness-program §0.1）。
- **运行时 API 凭证体系**——`ApiCredentialAuthenticationFilter` 及 API 凭证 + AD Group 双重授权路径**不动**；`jti`/`sessionStartedAt`/撤销校验仅作用于管理令牌路径（`createManagementToken` / `JwtAuthenticationFilter`）。
- **remember-me / 长期免登录**。
- **并发会话数限制**（同一账户多处登录不互踢、不计数）。
- 编辑器脏表单守卫与本地草稿恢复——LR-C1/LR-C2 伴生任务，与本规格互补（本规格保证会话不断，LR-C1/C2 保证即便断了工作也不丢）。

---

## 14. 实现契约摘要（供 TDD 任务分解；机制可按仓库现状微调，语义不得偏离 §0）

| 变更点 | 内容 |
| --- | --- |
| 令牌 claims | `createManagementToken` 增发标准 `jti`（UUID）+ 自定义 `sessionStartedAt`；`parseManagementToken` 回读两者（test-only 的 `createAccessToken(subject)` 不涉） |
| 新端点 | `POST /api/management/v1/auth/renew`——鉴权：有效未撤销管理令牌；响应结构同 login（`accessToken`/`tokenType`/`session`）；401 分支见 8.2/8.4/8.5/B1–B3 |
| 会话视图 | `ManagementSessionView` 新增 `absoluteSessionExpiresAt`（服务端计算下发，前端不自行推算） |
| 撤销端口 | `SessionRevocationStore`（`revoke(jti, expiresAt)` / `isRevoked(jti)`）+ `RedisSessionRevocationStore`（默认）+ `InMemorySessionRevocationStore`（transitional-test-only，模式对齐 `IdempotencyCachePort`/`RedisIdempotencyCache`） |
| 过滤器 | `JwtAuthenticationFilter` 解析成功后校验撤销名单；撤销命中/名单不可用时以可区分错误（SESSION_REVOKED / SESSION_VALIDATION_UNAVAILABLE）经认证入口点回信封 |
| logout | `ManagementAuthService.logout` 由 log-only 升级为「写撤销名单 + 审计」 |
| 错误模型 | `ApiErrorCodes` 新增 3 个 code；`messages_en.properties` 新增 3 个 messageKey（§12.1） |
| 配置 | `docgen.session.absolute-ttl: PT8H`、`docgen.session.revocation-store` 取值 `redis`（默认）/ `memory`（prod 禁 memory，见 B7）；`docgen.jwt.access-token-ttl: PT30M` 不变 |
| 前端 | 续期调度（composable，如 `useSessionRenewal`：活动追踪 + 剩余寿命评估 + 跨 tab 去重 SHOULD）；临上限提醒组件；i18n 新 key（§12.2/12.3）；401 流程沿用 `http.ts`（COR-F03 不改语义） |
| 测试门禁 | 后端 `mvn -B -ntp -f backend/pom.xml verify`；前端 lint/type-check/test/build；Docker 4173 Playwright 会话旅程 + e2e-uiux-reviewer 提醒 UI 取证（LR-B6 Gates 原文） |

---

## 14.1 Implementation deviations（实现偏差回写，2026-07-04）

LR-B6 实施完成后的事实回写。语义均未偏离 §0 已确认策略；以下为与 §8/§14 原措辞的机制差异与已接受残余：

| # | 偏差 | 事实 |
| --- | --- | --- |
| ① | 契约字段位置 | login/renew result **顶层**新增 `accessTokenExpiresAt` + `sessionAbsoluteDeadline`（`LoginResult`），供前端续期调度直接读取；session 视图同时携带 `expiresAt` + `absoluteSessionExpiresAt`（`ManagementSessionView`）。§14 原措辞仅定义 `accessToken`/`tokenType`/`session` 三字段 + session 视图内新增字段 |
| ② | renew 服务归属 | 续期逻辑收敛进 `ManagementAuthService`（`renew` 方法），**无**独立 `SessionRenewalService`；撤销端口仍按 §14 独立（`SessionRevocationStore` + Redis/InMemory 实现） |
| ③ | 会话异常处理 | 会话类 401/503 异常走独立 `@RestControllerAdvice`（`ManagementSessionExceptionHandler`），不并入既有 `GlobalExceptionHandler` 映射 |
| ④ | renew 时撤销写失败 | 与 logout 同语义：503 统一信封 fail-closed（`[ASSUMED-LOGOUT-REDIS-DOWN]` 语义扩展至 renew 路径——旧 jti 无法写入撤销名单时不发新令牌） |
| ⑤ | 并发 renew 未串行化 | 同一旧 `jti` 的两个并发 renew **均可成功**（各发新令牌）——8.4「任意时刻至多一个有效令牌」的字面偏差；受 30min TTL + `sessionStartedAt` 锚点约束（任何令牌 `exp ≤ sessionStartedAt + 8h` 恒成立），与 8.8 已接受竞态同族，风险有界；后续可用原子轮换（如 Redis SETNX/Lua 或 DB 乐观锁）收敛，届时回写本节 |
| ⑥ | 8.8 跨 tab SHOULD 去重 | 未实现 localStorage 跨 tab 续期去重；已实现 tab 内 `renewInFlight` 单飞 + **客户端 logout 竞态护栏**（logout 后迟到的 renew 响应被丢弃，不复活已登出会话——安全评审 🟡#2 修复，含回归测试） |

---

## 15. BDD 就绪声明

**状态**: `ready`

- [x] 会话策略四要素（P1–P4）由用户 2026-07-04 明确确认（「按这个实现吧」），非假设
- [x] 全部管理端角色为 Actor，行为一致
- [x] 8 个 Given/When/Then 场景覆盖：正常续期、8h 上限拒绝+引导重登录、logout 重放拒绝、续期后旧令牌重放拒绝、Redis 宕机 fail-closed、静默续期不打断编辑、临上限提醒 i18n、双 tab 并发续期
- [x] 边界 11 项（含 test profile 无 Redis 的 transitional-test-only 内存实现、发布切换期旧令牌、logout 期 Redis 宕机残余风险界定）
- [x] 8 个 `[ASSUMED]` 项均已给默认值且不阻塞核心路径
- [x] L1 copy en 基线 + zh-CN 齐备，key 稳定，无 token/JWT 技术词
- [x] 可观测证据与实现契约摘要可直接供 TDD Red 测试取材

**移交**: `plan-orchestrator` 解锁 LR-B6（Blocked → 可开工）→ `backend-engineer` + `frontend-engineer` TDD 实施。实施同 change set 内由 doc-keeper/实施者按 LR-B6 步骤 5 更新 `docs/security/permission-matrix.md`（新增会话语义小节）与 `docs/architecture/security-view.md`（fail-closed 基线追加会话撤销条目）。

**交付回执（2026-07-04）**: LR-B6 **Done**——全部 8 场景由后端/前端测试 + Docker Playwright（Part A/B）+ UIUX 取证覆盖；权限矩阵 §13.5 与 security-view fail-closed 条目已落盘；实现偏差见 §14.1。

---

## 16. 可追溯性

| 文档 / 代码 | 路径 | 关联内容 |
| --- | --- | --- |
| 任务卡 | `docs/plan/detail/LRP-B-runtime-scaleout-session.md` § LR-B6 | Read first / Do NOT / Gates / 验收原文 |
| 坑位登记 | `docs/plan/detail/CDP-industry-pitfall-registry.md` § CD-PIT-13 | 症状/根因/缓解 |
| 用户确认 | 本规格 §0（2026-07-04 会话原话「按这个实现吧」） | source-of-truth 首位：最新用户明确确认 |
| 权限矩阵 | `docs/security/permission-matrix.md` §2/§13 | 管理登录会话原则；实施时新增会话续期/撤销语义小节（owning doc，LR-B6 步骤 5） |
| 安全视图 | `docs/architecture/security-view.md` § Fail-Closed Baseline | 实施时追加「撤销名单不可用 → 拒绝」条目（owning doc） |
| 认证边界 ADR | `docs/adr/authorization-security/0036-local-account-store-authorization-authority.md` | 本地账户 + JWT v1 边界，无 SSO/OIDC |
| 后端令牌服务 | `backend/src/main/java/com/bank/docgen/sharedkernel/security/JwtTokenService.java` | claims 增发点（现无 jti/续期） |
| 后端过滤器 | `backend/src/main/java/com/bank/docgen/authorization/management/web/JwtAuthenticationFilter.java` | 撤销校验插入点 |
| 后端认证服务 | `backend/src/main/java/com/bank/docgen/authorization/management/service/ManagementAuthService.java` L69–71 | logout log-only 现状（本规格 D2 消除） |
| 后端认证端点 | `backend/src/main/java/com/bank/docgen/authorization/management/web/ManagementAuthController.java` | login/logout/session 现状；renew 新增处 |
| 错误模型 | `backend/src/main/java/com/bank/docgen/sharedkernel/api/ApiErrorCodes.java` + `GlobalExceptionHandler.java` + `infrastructure/config/ManagementSecurityHandlers.java` | 401 信封与入口点映射 |
| 撤销存储模式参考 | `backend/src/main/java/com/bank/docgen/runtime/service/IdempotencyCachePort.java` + `RedisIdempotencyCache.java`（`docgen.idempotency.cache` 取值 `redis` / `memory`） | test profile 内存实现的既有先例 |
| 前端 401 流程 | `frontend/src/api/http.ts`（COR-F03 Done） | 401 → 清会话 → 登录页带 redirect（8.2/8.8 依赖，不改语义） |
| 前端会话存储 | `frontend/src/stores/session.ts` + `localStorage['docgen.accessToken']` | 令牌替换点、跨 tab 共享事实（8.8 前提） |
| i18n 基线 | `backend/src/main/resources/i18n/messages_en.properties`；`frontend/src/i18n/catalogs/apiErrorEn.ts` / `apiErrorZhCn.ts`；`frontend/src/i18n/locales/en.ts` / `zh-CN.ts` | §12 全部 key 的落点 |
| 伴生任务 | `docs/plan/detail/LRP-C-usability-deepening.md` § LR-C1/LR-C2 | 脏表单守卫 / 本地草稿（工作不丢的另一半） |
| 程序总纲 | `docs/plan/launch-readiness-program.md` §0.1 | No SSO/OIDC 约束来源 |

---

*本文档由 behavior-spec-author 子 Agent 生成于 2026-07-04。§0 为用户已确认策略；§11 的 `[ASSUMED]` 项为带默认值的实现取向，变更须回写本规格。*
