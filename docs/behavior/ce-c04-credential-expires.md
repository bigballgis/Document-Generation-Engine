# BDD 行为规格：CE-C04 — 凭证 `expires_at` 持久化 + 暴露

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-15  
**BDD ID 前缀**: `BDD-CE-C04`  
**来源**: [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) Wave CE-C · CE-C04  
**Slice**: `ce-c04-credential-expires`  
**Worktree**: `D:/working/DGE-ce-c04-credential-expires` · `feat/ce-c04-credential-expires`  
**Task Master**: **#69**  
**Formal phase**: **None**  
**完成声明约束**: 本切片结束 `ApiCredentialLifecycleSupport` **过渡态**（以 `createdAt+180d` 推导到期）；落地持久化 `expires_at`、发放/轮换写入、运行时摘要暴露与契约页 callable versions 可选展示字段；**不**宣称 go-live；**不**实现 CE-C05/C06；**不**触碰 CE-K06 / CE-U06；**不**新建凭证级 `ROTATING` 状态。

---

## 1. 概述

| 行为域 | 摘要 |
| --- | --- |
| **持久化** | `api_credential.expires_at`（TIMESTAMPTZ NOT NULL）经 Flyway 加列；既有行按 `created_at + 180d` 回填 |
| **发放 / 轮换写入** | 创建凭证写入 `expires_at`；轮换**不**重置到期（仅换 secret / generation），保存时列保持非空 |
| **有效状态** | 以**持久化** `expires_at` + 时钟推导有效状态；`EXPIRING_SOON` = 距到期 ≤ 30 天且未过期；废止 `createdAt+180` 推导 |
| **运行时摘要** | `RuntimeCredentialSummaryView` / 契约 `apiPolicy.credentialSummary` 暴露 `expiresAt` 与有效 `status`（含 `EXPIRING_SOON`） |
| **鉴权** | 有效状态 `ACTIVE` 或 `EXPIRING_SOON` 可调用；`EXPIRED` → `401 API_CREDENTIAL_EXPIRED`；`REVOKED` → `401 API_CREDENTIAL_REVOKED` |
| **契约 callable versions** | `CallableVersion` 增加可选 `deprecated` / `sunsetAt`（展示边界）；**不**改变可调用候选集规则（ADR-0017） |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| 过渡态：`resolveExpiresAt` = `createdAt + 180d`；类注释写明直至 `expires_at` 持久化 | `ApiCredentialLifecycleSupport` |
| 表无 `expires_at` 列 | Flyway `V7__api_management.sql` |
| `RuntimeCredentialSummaryView` 仅 `credentialExternalId` / `status` / `fingerprintSummary` | runtime API record |
| OpenAPI `CredentialSummary.expiresAt` 已声明；示例含 `expiresAt` | `openapi-v1.yaml` / `contract-response.json` |
| `CallableVersion` 仅 `releaseVersion` + `explicitVersionUrl` | OpenAPI + `CallableVersionView` |
| 鉴权过滤器仅允许持久化 `status == ACTIVE`（未按有效状态放行 `EXPIRING_SOON`） | `ApiCredentialAuthenticationFilter` |
| 管理端创建无请求体；默认发放路径无写到期列 | `ApiManagementCredentialController` / `ApiCredentialCommandSupport` |
| SoT：默认 180 / 最长 365 / `EXPIRING_SOON` 提醒窗 30 天 | ADR-0009；domain-model；requirements-plan；PRD |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **API 调用方** | Runtime caller | 持 API 凭证 + AD Group；读契约摘要中的非敏感到期/状态；生成调用受凭证有效性约束 |
| **全局 / 分组管理员** | Management admin | 发放、轮换、吊销凭证；看告警与凭证列表中的到期摘要；不得再读 secret 明文 |
| **系统** | ApiMgmt + Runtime auth + Contract assembly | 持久化到期、推导有效状态、鉴权 fail-closed、契约组装 |
| **契约消费者** | OpenAPI / 集成方 | 依赖 schema：`CredentialSummary.expiresAt`、`CallableVersion` 可选字段 |

---

## 3. Goal

1. 结束过渡态：到期时间以 DB `expires_at` 为唯一真相，不再用 `createdAt + 180` 冒充持久化。  
2. 发放时写入默认到期（`now + 180d`）；轮换不延长/缩短到期。  
3. 运行时契约 `credentialSummary` 与 `RuntimeCredentialSummaryView` 暴露真实 `expiresAt` 与有效 `status`（含 `EXPIRING_SOON`）。  
4. 到期前 30 天有效状态为 `EXPIRING_SOON` 且**仍可调用**；过期/吊销 fail-closed 并返回已确认错误码。  
5. 跨包告警 `EXPIRING_CREDENTIAL` 继续可用，但到期时刻来自持久化列。  
6. 契约 `callableVersions[]` / `listCallableVersions` 支持可选 `deprecated` / `sunsetAt` 展示字段；可调用集规则不变。  
7. 不向日志/审计/契约泄露 secret 明文。

---

## 4. 已确认决策（confirmed）

### 4.1 产品 / 领域基线（既有 SoT，本片落地）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **EX-1** | 凭证必须有到期时间；默认 **180** 天；最长 **365** 天；管理员可设更短（本片管理创建默认 180；自定义天数见 S-9） | ADR-0009 |
| **EX-2** | 状态集：`ACTIVE`、`EXPIRING_SOON`、`EXPIRED`、`REVOKED`；无凭证级 `ROTATING` | ADR-0009 |
| **EX-3** | `EXPIRING_SOON` = 到期前提醒窗口；窗口 **30** 天 | ADR-0009；既有 `EXPIRING_SOON_WINDOW_DAYS` |
| **EX-4** | 过期请求 → `401 API_CREDENTIAL_EXPIRED`；吊销 → `401 API_CREDENTIAL_REVOKED` | ADR-0009 |
| **EX-5** | 调用方可通过契约查看自己凭证的非敏感状态与到期摘要；不主动推送到期提醒给调用方 | ADR-0009 |
| **EX-6** | 时间字段 ISO 8601 **带时区偏移**（与既有 API 时间约定一致） | domain-model / contract-outline |
| **EX-7** | 轮换：新 secret 立即生效；旧 secret 7 天宽限期（既有）；**不**因轮换重置 `expires_at` | ADR-0009 + 本片锁定 |

### 4.2 本片范围锁定（confirmed for this slice）

| ID | 决策 |
| --- | --- |
| **S-1** | Flyway：`api_credential.expires_at TIMESTAMPTZ NOT NULL`；既有行 `expires_at = created_at + 180 days` |
| **S-2** | 实体 / 仓储读写 `expiresAt`；`ApiCredentialLifecycleSupport.resolveExpiresAt` **只读持久化列**（禁止再推 `createdAt+180`）；类注释去掉 transitional 表述 |
| **S-3** | **创建**：`expires_at = createdAt + DEFAULT_EXPIRY_DAYS`（180）；审计记录含到期时间（既有审计要求） |
| **S-4** | **轮换**：更新 secret / `rotationGeneration`；**保持**原 `expires_at`；响应不要求新到期字段，但摘要查询须仍返回原到期 |
| **S-5** | **有效状态** `resolveEffectiveStatus(credential, now)`：`REVOKED`/`EXPIRED`（持久或推导）优先；否则若 `expires_at <= now` → `EXPIRED`；否则若 `expires_at <= now+30d` → `EXPIRING_SOON`；否则 `ACTIVE`（若持久状态已是 `EXPIRING_SOON` 且未过期，有效状态保持 `EXPIRING_SOON`） |
| **S-6** | `RuntimeCredentialSummaryView` **增加** `expiresAt`（`Instant` / JSON date-time）；`status` 为**有效状态**名；契约组装 CALLER/ADMIN 摘要均使用该字段；OpenAPI `CredentialSummary.expiresAt` 与实现对齐 |
| **S-7** | **鉴权**：有效状态 ∈ {`ACTIVE`,`EXPIRING_SOON`} 且 secret 匹配 → 通过；`EXPIRED` → `API_CREDENTIAL_EXPIRED`；`REVOKED` → `API_CREDENTIAL_REVOKED`；未知/错误 secret 仍 `INVALID_CREDENTIALS`（不泄露原因细节超出既有基线） |
| **S-8** | 管理端列表/吊销摘要与告警：`expiresAt` + 有效状态来自持久化列；`ApiAccessAlertQueryService` 的 `EXPIRING_CREDENTIAL` 继续依赖 `isExpiringCredential` / `resolveExpiresAt`（实现改为读列后行为不变） |
| **S-9** | **自定义 `expiryDays`（1..365）**：产品已确认（ADR-0009 / OpenAPI `CredentialCreateRequest.expiryDays`）。本片**管理创建**可保持无 UI 选天；后端默认 180 即可关闭过渡态。若同变更集为 create 增加可选 `expiryDays`（校验 `1 ≤ days ≤ 365`，否则 `400`），视为同片增强而非新范围；**禁止**超过 365 |
| **S-10** | **CallableVersion 可选字段**：OpenAPI + `CallableVersionView` + 契约/`listCallableVersions` 增加可选 `deprecated`（boolean）与 `sunsetAt`（date-time）。对当前可调用集（`PUBLISHED` 版本）：`deprecated=false`（或省略 false）、`sunsetAt` 省略。字段为**展示/发现**用途；**不得**借此把已停用/已废弃版本塞进可调用集 |
| **S-11** | **ADR 轻量修订（展示边界）**：同交付集由 `doc-keeper` 轻量修订 ADR-0003 / ADR-0017 —— 明确「契约/callable versions **可**暴露可选 deprecation/sunset **展示**元数据，不改变可调用候选集与恢复/废弃规则」。**不**改 ADR-0009 决策正文（仅实现收口） |
| **S-12** | 明确非目标：CE-C05 `originalBatchId`；CE-C06 DOCX permissions；CE-K06 / CE-U06；凭证主动推送提醒通道实现（管理员提醒若未落地另片）；管理 UI 新建「选到期天数」表单；E2E/UIUX（无管理 UI 行为变更时 **not-applicable**）；go-live / CD-3 |

### 4.3 与相邻切片边界

| ID | 决策 |
| --- | --- |
| **B-1** | CE-C03 已对齐 `fidelityWarnings`；本片不回退 |
| **B-2** | CE-U11 Done（依赖满足）；本片不改排障/召回 UI |
| **B-3** | CE-U12 契约示例页不强制本片改 FE；若仅 OpenAPI/runtime JSON 增可选字段，管理契约页可暂不渲染新列 |

---

## 5. Trigger

| # | 触发 |
| --- | --- |
| T1 | 管理员创建（发放）API 凭证 |
| T2 | 管理员轮换凭证 secret |
| T3 | 调用方读取契约（含 `credentialSummary`）或 `listCallableVersions` |
| T4 | 调用方使用即将到期 / 已过期 / 已吊销凭证调用运行时 API |
| T5 | 管理员打开跨包 API 告警（含 `EXPIRING_CREDENTIAL`） |
| T6 | 迁移部署：既有 `api_credential` 行回填 `expires_at` |

---

## 6. Preconditions

- 模板存在 API policy；管理员具备模板 API 管理权限（fail-closed）。  
- 调用方路径：有效 AD Group + 模板级授权（本片不改 AD 规则）。  
- ADR-0009 / OpenAPI `CredentialStatus` / `CredentialSummary` 为到期与状态权威。  
- 工作树：`feat/ce-c04-credential-expires`（`D:/working/DGE-ce-c04-credential-expires`）。  
- 上游：U11 **#86** Done；**禁止**改动 K06 / U06。

---

## 7. Primary journey（成功）

### 7.1 发放并在契约中可见到期

1. 管理员对已配置 API policy 的模板创建凭证。  
2. 系统持久化 `expires_at = createdAt + 180d`（或合法自定义天数），secret 明文仅返回一次。  
3. 调用方携带该凭证读取契约。  
4. `apiPolicy.credentialSummary` 含 `expiresAt` 与有效 `status`（初期多为 `ACTIVE`）；无 secret。

### 7.2 进入 EXPIRING_SOON 仍可调用

1. 凭证 `expires_at` 落在 `(now, now+30d]`。  
2. 有效状态为 `EXPIRING_SOON`；契约摘要与告警可见。  
3. 调用方仍可成功通过鉴权并完成授权允许的生成/契约读取。

### 7.3 轮换不改到期

1. 管理员轮换仍有效的凭证。  
2. 新 secret 一次展示；`expires_at` 与轮换前相同。  
3. 新 secret 可调用；摘要 `expiresAt` 不变。

---

## 8. System responses

### 8.1 成功

| 条件 | HTTP / 形态 |
| --- | --- |
| 创建凭证 | 201；创建视图含状态；DB `expires_at` 非空；secret 仅此响应 |
| 契约摘要（CALLER） | 200；`credentialSummary.expiresAt` + 有效 `status`；无 secret / 完整指纹策略保持既有脱敏 |
| `listCallableVersions` | 200；每项含既有 URL 字段；可选 `deprecated`/`sunsetAt` 按 S-10 |
| `EXPIRING_SOON` 调用 | 与 `ACTIVE` 相同成功路径（鉴权通过） |

### 8.2 失败 / 边界

| 条件 | 期望 |
| --- | --- |
| `expires_at <= now` 且未吊销 | `401` + `API_CREDENTIAL_EXPIRED` |
| 已吊销 | `401` + `API_CREDENTIAL_REVOKED` |
| secret 错误 | `401` + `INVALID_CREDENTIALS`（既有） |
| 自定义天数 > 365 或 < 1（若实现 S-9 可选体） | `400` 校验错误；不写入凭证 |
| 未授权管理操作 | 既有 fail-closed（403/404 不泄露跨组） |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-CE-C04-001 — Flyway 持久化列与回填

```gherkin
Given 库中已有创建于过去的 api_credential 行且升级前无 expires_at
When 应用本片 Flyway 迁移
Then api_credential.expires_at 为 NOT NULL
And 既有行 expires_at = created_at + 180 days（UTC/TIMESTAMPTZ 语义）
And 新插入行必须提供 expires_at
```

### BDD-CE-C04-002 — 发放写入默认 180 天到期

```gherkin
Given 管理员已授权管理模板 T 的 API 凭证
When 管理员创建一张新凭证（无自定义天数）
Then 持久化 expires_at ≈ created_at + 180 days
And 响应不包含可重复查询的 secret 明文（仅创建响应一次）
And 随后列表/摘要可读到该 expiresAt
```

### BDD-CE-C04-003 — 有效状态 EXPIRING_SOON（≤30 天）

```gherkin
Given 凭证未吊销且 expires_at 落在 (now, now+30d]
When 系统解析有效状态（契约摘要、告警、LifecycleSupport）
Then 有效 status = EXPIRING_SOON
And isExpiringCredential = true
And isActiveCredential = true
```

### BDD-CE-C04-004 — EXPIRING_SOON 仍可调用运行时 API

```gherkin
Given 凭证有效状态为 EXPIRING_SOON 且 secret 正确、AD Group 通过
When 调用方发起契约读取或已授权的生成请求
Then 鉴权通过（不因 EXPIRING_SOON 拒绝）
And 契约 credentialSummary.status 为 EXPIRING_SOON
And credentialSummary.expiresAt 等于持久化到期
```

### BDD-CE-C04-005 — 过期返回 API_CREDENTIAL_EXPIRED

```gherkin
Given 凭证未吊销且 expires_at <= now
When 调用方使用该凭证调用受保护运行时 API
Then HTTP 401
And error.code = API_CREDENTIAL_EXPIRED
And 不执行生成业务副作用
```

### BDD-CE-C04-006 — 吊销返回 API_CREDENTIAL_REVOKED

```gherkin
Given 凭证已吊销（status=REVOKED）
When 调用方使用该凭证调用受保护运行时 API
Then HTTP 401
And error.code = API_CREDENTIAL_REVOKED
```

### BDD-CE-C04-007 — 轮换不重置 expires_at

```gherkin
Given 凭证 V 的 expires_at = E 且有效状态为 ACTIVE 或 EXPIRING_SOON
When 管理员轮换 V 的 secret
Then 持久化 expires_at 仍为 E
And 新 secret 可鉴权成功
And 旧 secret 按既有 7 日宽限期规则（本片不改宽限期天数）
```

### BDD-CE-C04-008 — RuntimeCredentialSummaryView / 契约暴露 expiresAt

```gherkin
Given 调用方凭证已持久化 expires_at = E
When 调用方 GET 契约摘要（CALLER audience）
Then apiPolicy.credentialSummary 含 expiresAt = E（ISO 8601 带时区）
And status 为有效状态名
And 响应不含 secret 明文
And OpenAPI CredentialSummary 与实现字段一致（含 expiresAt）
```

### BDD-CE-C04-009 — 过渡态推导已废止

```gherkin
Given 凭证 created_at 很早但 expires_at 被显式设为未来较远日期（或反之）
When resolveExpiresAt / 有效状态计算执行
Then 只使用持久化 expires_at
And 结果不得等于「无视 expires_at 仅用 created_at+180」的旧过渡行为
```

### BDD-CE-C04-010 — 跨包 EXPIRING_CREDENTIAL 告警用持久化到期

```gherkin
Given PUBLISHED 模板存在有效状态 EXPIRING_SOON 的凭证
When 授权管理员查询跨包 API access alerts
Then 存在 EXPIRING_CREDENTIAL 告警
And 告警 expiresAt 等于该凭证持久化 expires_at
And deep link 指向既有 hub 凭证区域（既有行为）
```

### BDD-CE-C04-011 — CallableVersion 可选 deprecated / sunsetAt（展示）

```gherkin
Given 模板存在可调用发布版本集合 C（按既有 PUBLISHED 规则）
When 调用方 GET listCallableVersions 或契约 callableVersions
Then 返回集合与升级前可调用集 C 一致（不增不减）
And schema/响允许每项带可选 deprecated、sunsetAt
And 对当前可调用项：deprecated 为 false 或省略；sunsetAt 省略
And 文档/ADR 展示边界说明：这些字段不改变可调用候选集
```

### BDD-CE-C04-012 — 自定义到期上限（若实现可选 expiryDays）

```gherkin
Given 管理创建支持可选 expiryDays
When 管理员以 expiryDays=366 创建
Then 请求失败（400）；不创建凭证
When 管理员以 expiryDays=90 创建
Then expires_at ≈ created_at + 90 days
```

---

## 10. Boundary / exception behavior

| 边界 | 行为 |
| --- | --- |
| `expires_at` 恰等于 `now` | 视为已过期（`!expiresAt.isAfter(now)` → `EXPIRED`） |
| `expires_at` 恰等于 `now+30d` | `EXPIRING_SOON`（`!expiresAt.isAfter(now+30d)`） |
| DB 持久 `status=EXPIRED` 但列仍在未来 | 有效状态保持 `EXPIRED`（持久终态优先） |
| DB 持久 `status=ACTIVE` 但已过期 | 有效状态 `EXPIRED`；鉴权拒绝 |
| 轮换时凭证非 ACTIVE（既有） | 保持既有 `credentialNotActive` 校验；本片不放宽对已过期行的轮换 |
| CallableVersion `deprecated=true` | 本片对可调用集不强制产出 true；禁止用该字段绕过停用/废弃门槛 |
| 秘密与指纹 | 契约/告警/审计仅指纹或脱敏标识；禁止 secret |
| 授权失败 | fail-closed；不泄露未授权模板是否存在 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| **DB** | `\d api_credential` / 查询含 `expires_at`；回填后无 NULL |
| **API JSON** | 契约 `credentialSummary.expiresAt` / `status`；callableVersions 可选字段 |
| **HTTP 错误** | 过期/吊销错误码与 ADR-0009 一致 |
| **告警** | `EXPIRING_CREDENTIAL.expiresAt` 对齐列值 |
| **OpenAPI / examples** | `CredentialSummary`、`CallableVersion`、`contract-response.json` / callable-versions 示例 |
| **ADR** | ADR-0003 / 0017 展示边界轻量修订（doc-keeper） |
| **Gates** | `mvn -B -ntp -f backend/pom.xml verify`（TDD Red→Green）；无 FE 变更则前端门禁 N/A |
| **非证据** | 管理 UI 截图 / Playwright E2E（无 FE 行为变更时） |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §5 CE-C04 | 计划卡 / P1·M |
| Task Master **#69** | 执行任务（依赖 **#86** U11 Done） |
| [ADR-0009](../adr/api-management/0009-api-credential-lifecycle.md) | 到期/状态/错误码权威 |
| [ADR-0003](../adr/api/0003-api-routing-and-batch-overrides.md) | 契约展示边界轻量修订（callable metadata） |
| [ADR-0017](../adr/template-lifecycle/0017-template-lifecycle-recovery-deprecation-import.md) | 可调用集 / 废弃规则不变；展示边界轻量修订 |
| [openapi-v1.yaml](../api/openapi-v1.yaml) `CredentialSummary` / `CredentialStatus` / `CallableVersion` | 正式 schema |
| [contract-outline.md](../api/contract-outline.md) | 凭证摘要与状态说明 |
| [domain-model.md](../domain/domain-model.md) / [requirements-plan.md](../requirements/requirements-plan.md) / [PRD.md](../product/PRD.md) | 180/365/`EXPIRING_SOON` |
| [permission-matrix.md](../security/permission-matrix.md) | 凭证状态集与鉴权 |
| [api-access-cross-package-alerts.md](./api-access-cross-package-alerts.md) | SCEN-ALERT-02 到期告警 |
| `ApiCredentialLifecycleSupport` | 过渡态收口点 |

---

## 13. TDD Red 提示（给 implementer，非本片执行）

优先失败测试方向（backend）：

1. 创建后 DB/实体 `expiresAt` 非空且约为 `now+180d`。  
2. `resolveExpiresAt` 在「篡改 createdAt 但不改 expiresAt」时仍返回列值。  
3. 窗口内有效状态 `EXPIRING_SOON` 且鉴权通过。  
4. 过期 → `API_CREDENTIAL_EXPIRED`；吊销 → `API_CREDENTIAL_REVOKED`。  
5. 轮换前后 `expiresAt` 不变。  
6. 契约组装 / `RuntimeCredentialSummaryView` JSON 含 `expiresAt`。  
7. `CallableVersion` schema/序列化含可选字段且可调用集势不变。

---

## 14. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/ce-c04-credential-expires.md
task_ids: [#69]
frontend_management_ui_in_scope: false
e2e_uiux: not-applicable
doc_keeper_needed_before_implement: yes — OpenAPI CallableVersion + CredentialSummary alignment; light ADR-0003/0017 display-boundary amend; contract-outline/examples as needed
adr_touch: light amend ADR-0003 + ADR-0017 (display only); ADR-0009 decision text unchanged
```

**Handoff**: `plan-orchestrator`（可选任务分解）→ `backend-engineer` TDD；并行/先后 `doc-keeper` 做 OpenAPI + ADR 展示边界；FE E2E **not-applicable** unless 意外改管理 UI。

---

## 15. Out of scope（复述）

- Go-live / CD-3 / 正式 P-phase 激活  
- CE-C05 / CE-C06 / CE-K06 / CE-U06  
- 管理 UI「选择到期天数」表单与相关 E2E  
- 调用方主动到期推送；管理员提醒通道若未实现则另片  
- 改变 7 日轮换宽限期或新增 `ROTATING`  
- 用 `deprecated`/`sunsetAt` 改变停用/废弃/可调用集语义  
