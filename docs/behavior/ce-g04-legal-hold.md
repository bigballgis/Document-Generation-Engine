# BDD 行为规格：CE-G04 — Legal hold 最小实现

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-G04` |
| **编写日期** | 2026-07-16 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §6 Wave CE-G · CE-G04 |
| **Slice** | `ce-g04-legal-hold` |
| **Worktree** | `D:/working/DGE-ce-g04-legal-hold` · `feat/ce-g04-legal-hold` |
| **Task Master** | **#75** |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED |
| **上游** | CE-G03 (#74) **Done**；LR-D1 / ADR-0048 审计留存；ADR-0040 调用记录留存 |
| **Owning docs** | 本文件（行为 SoT）；[permission-matrix.md](../security/permission-matrix.md)；[domain-model.md](../domain/domain-model.md)；[ADR-0048](../adr/operations/0048-audit-data-retention-policy.md) / [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md)（豁免叠加，不改 ADR 正文） |

**完成声明约束：** 本切片关闭内控缺口「无 legal hold」的最小闭环（hold 实体 + 两路 retention 删除前豁免 + GLOBAL_ADMIN 管理页）；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-G05 年检；**不**实现诉讼导出包 / eDiscovery 全量；**不**修订 ADR-0040/0048 正文（以本规格叠加豁免）。

---

## 1. 概述

平台已有调用记录留存清理（ADR-0040）与管理/运行时审计留存清理（ADR-0048 / LR-D1）。诉讼或内控冻结场景下，超龄数据仍可能被调度器硬删，缺少可审计的 **legal hold** 豁免。

| 行为域 | 摘要 |
| --- | --- |
| **G04-S1 Hold 实体** | 支持两种范围：`TEMPLATE_WINDOW`（模板 + UTC 时间窗）或 `INVOCATION_SET`（显式 invocation external ID 集合） |
| **G04-S2 调用清理豁免** | `InvocationRetentionCleanupScheduler` 在清理产物与删除记录前跳过受 hold 保护的行 |
| **G04-S3 审计清理豁免** | `AuditRetentionCleanupService` 在硬删超龄管理/运行时审计前跳过受 hold 保护的行 |
| **G04-S4 管理面** | 仅 `GLOBAL_ADMIN` 可 list / create / release；English-first 管理页 |
| **G04-S5 审计** | create / release 写管理审计；**禁止**敏感 payload（无 variables、无凭证、无完整请求体） |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| Hold 表 / API / 管理页 | 工作树 WIP（`legalhold` 包、V66、FE `/governance/legal-holds`）— **未**宣称 gates Done |
| 调用 / 审计清理豁免 | 须由实现接 `LegalHoldExemptionService`；验收以 BDD-CE-G04-007…014 为准 |
| 文档（Stage 3） | domain §2.15.1、permission-matrix §13.1/§13.2、contract-outline / OpenAPI / `docs/README` 已对齐本 BDD；**不**改 ADR-0040/0048 正文 |

---

## 2. Source-of-truth 与裁定

| 来源 | 陈述 | 本切片裁定 |
| --- | --- | --- |
| **CE-G04 plan** | hold 实体（模板+时间窗 / invocation 集合）；两调度器删除前查豁免；GLOBAL_ADMIN 专属页 | **确认 SoT** |
| **ADR-0040 / ADR-0048** | 超龄硬删除 | **确认** — 默认语义不变；**ACTIVE hold** 叠加豁免，不改 ADR 正文 |
| **permission-matrix** | GLOBAL_ADMIN 平台级治理 | **确认** — hold 管理 **仅** GLOBAL_ADMIN；其他角色 fail-closed |
| **Handoff / plan card** | 审计 create/release；无敏感 payload；English-first OA | **确认** |

**Confirmed requirement：** hold 实体双范围 + 两路 retention 豁免 + GLOBAL_ADMIN 管理 API/页 + 管理审计。  
**Pending / out of scope：** 自动 eDiscovery、跨系统 hold 同步、GROUP_ADMIN 组范围 hold、归档到冷仓代替豁免、go-live / CD-3。

---

## 3. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **全局管理员** | `GLOBAL_ADMIN` | 唯一可创建 / 列表 / 释放 hold；可见管理页路由 |
| **其他管理角色** | `GROUP_ADMIN` / `AUDIT_ADMIN` / … | API **403**；路由不可见 → Forbidden |
| **系统调度器** | `InvocationRetentionCleanupScheduler` / `AuditRetentionCleanupScheduler` | 删除前查询 ACTIVE hold 豁免 |
| **审计读者** | 既有 `readAudit` | 可读 `LEGAL_HOLD_CREATED` / `LEGAL_HOLD_RELEASED` 摘要（无敏感体） |

---

## 4. Goal

1. GLOBAL_ADMIN 可创建 ACTIVE hold：`TEMPLATE_WINDOW` 或 `INVOCATION_SET`。  
2. GLOBAL_ADMIN 可释放 hold（`ACTIVE` → `RELEASED`）；已释放不再豁免。  
3. 调用记录产物清理与行删除跳过受保护 invocation。  
4. 管理/运行时审计超龄硬删跳过受保护行。  
5. 非 GLOBAL_ADMIN fail-closed；管理审计记录 create/release。  
6. 不宣称 go-live。

---

## 5. 已确认决策（confirmed）

### 5.1 Hold 实体

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G04-C1** | 表 `legal_hold`（Flyway **V66**）：`id` UUID PK；`hold_external_id` 稳定对外 ID；`scope_type` `TEMPLATE_WINDOW` \| `INVOCATION_SET`；`status` `ACTIVE` \| `RELEASED`；`reason` 可选 ≤512（允许进审计）；`template_id` / `template_external_id`（TEMPLATE_WINDOW）；`effective_from` / `effective_to`（UTC；TEMPLATE_WINDOW）；`created_at` / `created_by_username`；`released_at` / `released_by_username`（释放后填）。枚举 **UPPER_SNAKE_CASE**。 | plan |
| **G04-C2** | 子表 `legal_hold_invocation`：`(hold_id, invocation_external_id)`；仅 `INVOCATION_SET` 使用；至少 1 个、最多 **500** 个 ID。 | plan「invocation 集合」 |
| **G04-C3** | **互斥：** 创建时必须且只能一种 `scope_type`。`TEMPLATE_WINDOW` 禁止带 invocation 集合；`INVOCATION_SET` 禁止带 template/window 字段。 | 清晰契约 |
| **G04-C4** | **TEMPLATE_WINDOW：** `templateId` **或** `templateExternalId` 必填（实现解析为存在的模板；二者皆提供须一致）；`effectiveFrom` 必填；`effectiveTo` 可选（`null` = 开放结束，直至释放）；若提供则 `effectiveTo >= effectiveFrom`。 | plan |
| **G04-C5** | **时间窗匹配（半开右端可选闭合）：** 时刻 `t` 在窗内 iff `t >= effectiveFrom` **且**（`effectiveTo == null` **或** `t <= effectiveTo`）。调用记录用 `createdAt`；审计用 `eventAt`。 | 可测 |
| **G04-C6** | **仅 `ACTIVE` 提供豁免。** `RELEASED` 立即停止豁免（即使时间窗未过）。 | 最小 |
| **G04-C7** | **不** soft-delete hold 行；释放改状态。列表默认可含 ACTIVE+RELEASED；查询参数 `status` 可过滤。 | 审计可追溯 |
| **G04-C8** | Hold **不**修改 `record_expires_at` / 审计 cutoff 配置；仅阻止删除。 | 叠加语义 |

### 5.2 豁免规则（两调度器）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G04-C9** | **`LegalHoldExemptionService`（名以实现为准）** 为唯一豁免判定入口；两调度器/服务在删除前调用。 | plan |
| **G04-C10** | **Invocation — TEMPLATE_WINDOW：** ACTIVE hold 且 `template_id` 匹配且 `createdAt` 在窗内 → 豁免产物清理 **与** 行删除。 | plan |
| **G04-C11** | **Invocation — INVOCATION_SET：** ACTIVE hold 且 `invocationExternalId` ∈ 集合 → 豁免产物清理与行删除。 | plan |
| **G04-C12** | **Management audit — TEMPLATE_WINDOW：** ACTIVE hold 且行 `templateId` 匹配且 `eventAt` 在窗内 → 不删。`templateId` 为 null 的平台级行（如 purge-evidence）**不**因 TEMPLATE_WINDOW 豁免。 | 可测 |
| **G04-C13** | **Management audit — INVOCATION_SET：** v1 **不**按 invocation 集合豁免管理审计行（无 invocation 外键）。调度器仍调用豁免服务；对该 scope 返回 false（除非另有 TEMPLATE_WINDOW 命中）。 | 诚实边界 |
| **G04-C14** | **Runtime audit — TEMPLATE_WINDOW：** 同 C12（`templateId` + `eventAt`）。 | 对称 |
| **G04-C15** | **Runtime audit — INVOCATION_SET：** 若 `taskExternalId` **或** `documentId` 等于任一受 ACTIVE hold 保护的 `invocation_external_id` → 豁免。 | 最小关联 |
| **G04-C16** | 实现可将 `deleteOlderThan` 改为「查候选 → 过滤豁免 → 删除剩余」，或等价 SQL；**禁止**先删后补。零候选或全豁免 → 不写 purge-evidence（保持 D1-C13）。 | LR-D1 兼容 |
| **G04-C17** | 豁免跳过记 debug/info 日志（含 hold id / 计数即可）；**禁止**日志打印 variables / 参数明文。 | 安全 |

### 5.3 API / 授权 / 审计

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G04-C18** | 管理 API 前缀：`/api/management/v1/legal-holds`。统一 envelope。 | 惯例 |
| **G04-C19** | `GET /` 列表（分页）；`GET /{id}`；`POST /` 创建；`POST /{id}/release` 释放。无物理 DELETE。 | 最小 CRUD |
| **G04-C20** | **授权：** 仅 `GLOBAL_ADMIN`。其他已认证角色 → **403** `api.error.authorization.forbidden`（或既有等价）。未认证 → 401。 | plan |
| **G04-C21** | 创建成功写 `LEGAL_HOLD_CREATED`；释放成功写 `LEGAL_HOLD_RELEASED`。摘要：hold id / external id、scopeType、status、templateId（若有）、invocationCount（若有）、reason、actorUsername。**禁止** variables、凭证、完整 invocation 参数体。 | plan |
| **G04-C22** | 非法 scope / 校验失败 → **422** VALIDATION + 稳定 messageKey；不写成功审计。重复释放已 RELEASED → **409** 或幂等 200（实现锁定：**409** `LEGAL_HOLD_ALREADY_RELEASED`）。 | fail-closed |
| **G04-C23** | 模板不存在（TEMPLATE_WINDOW）→ **404** 或 422（实现锁定：**404** `TEMPLATE_NOT_FOUND` 等价既有键）。 | 惯例 |

### 5.4 前端

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G04-C24** | 路由 `route.legal-hold-administration` → `/governance/legal-holds`（路径以实现为准，须稳定）。仅 GLOBAL_ADMIN `visibleRoutes`。 | plan |
| **G04-C25** | 页面：列表 ACTIVE/RELEASED；创建对话框（两种 scope）；Release 确认。Bank OA + English-first i18n（en 主键 + zh-CN）。 | OA + i18n |
| **G04-C26** | 非 GLOBAL_ADMIN 不可见菜单；直链 → Forbidden（既有 route guard）。 | fail-closed |

### 5.5 范围锁定

| ID | 决策 |
| --- | --- |
| **G04-C27** | **非目标：** CE-G05；eDiscovery 导出；GROUP_ADMIN 范围 hold；改 ADR-0040/0048 正文；改默认 retention 天数；go-live / CD-3；正式 P-phase |
| **G04-C28** | Formal phase 保持 **None**；Done 后 §9.2 next head → **#90** CE-U14 |

---

## 6. Trigger

| # | 触发 |
| --- | --- |
| T1 | GLOBAL_ADMIN 创建 / 释放 hold（API 或 UI） |
| T2 | `InvocationRetentionCleanupScheduler` 清理产物或删除过期行 |
| T3 | `AuditRetentionCleanupScheduler` / Service purge management 或 runtime 审计 |
| T4 | 非 GLOBAL_ADMIN 访问 API 或路由 |

---

## 7. Preconditions

| # | 前置 |
| --- | --- |
| PC1 | 用户已登录；GLOBAL_ADMIN 路径具备会话 |
| PC2 | LR-D1 / ADR-0040 清理调度器已存在 |
| PC3 | TEMPLATE_WINDOW 时目标模板已存在 |
| PC4 | E2E：Docker `4173` / `8080` |

---

## 8. Primary journey（成功路径）

1. GLOBAL_ADMIN 打开 Legal Holds 页 → 创建 `TEMPLATE_WINDOW`（模板 + from/to）→ 列表出现 ACTIVE。  
2. 调度器运行：该模板时间窗内的过期 invocation / 匹配审计行被跳过；其他超龄行仍删。  
3. GLOBAL_ADMIN 创建 `INVOCATION_SET`（若干 invocation external IDs）→ 对应调用记录不被删。  
4. GLOBAL_ADMIN Release → 状态 RELEASED → 写审计 → 后续清理可删（若仍超龄）。  

---

## 9. System responses

### 9.1 Success

| 响应 | 证据 |
| --- | --- |
| 创建 hold | HTTP 201；DB ACTIVE；`LEGAL_HOLD_CREATED` |
| 释放 | HTTP 200；RELEASED；`LEGAL_HOLD_RELEASED` |
| 豁免 | 受保护行仍在；日志/计数显示 skipped |
| UI | GLOBAL_ADMIN 可见列表与操作 |

### 9.2 Fail-closed

| 条件 | 响应 |
| --- | --- |
| 非 GLOBAL_ADMIN | 403；无状态变更 |
| 校验失败 | 422；无成功审计 |
| 重复释放 | 409 |
| 未认证 | 401 |

---

## 10. Acceptance scenarios（Given / When / Then）

### Entity / API

#### BDD-CE-G04-001 — Create TEMPLATE_WINDOW hold

**Given** GLOBAL_ADMIN 已登录且模板 T 存在  
**When** `POST /legal-holds` 且 `scopeType=TEMPLATE_WINDOW`、`templateId`、`effectiveFrom`、`effectiveTo`  
**Then** 201；状态 ACTIVE；可 GET 回读

#### BDD-CE-G04-002 — Create INVOCATION_SET hold

**Given** GLOBAL_ADMIN  
**When** `POST` 且 `scopeType=INVOCATION_SET`、非空 `invocationExternalIds`  
**Then** 201；子表行数 = 集合大小

#### BDD-CE-G04-003 — Reject mixed scope payload

**Given** 请求同时带 template 字段与 invocation 集合，或 scope 与字段不匹配  
**When** create  
**Then** 422；不持久化

#### BDD-CE-G04-004 — Release hold

**Given** ACTIVE hold  
**When** `POST …/release`  
**Then** RELEASED；写 `LEGAL_HOLD_RELEASED`

#### BDD-CE-G04-005 — Non-GLOBAL_ADMIN forbidden

**Given** GROUP_ADMIN（或其他非 GLOBAL_ADMIN）  
**When** list/create/release  
**Then** 403

#### BDD-CE-G04-006 — Create audit has no sensitive payload

**Given** 成功 create  
**When** 读 `LEGAL_HOLD_CREATED`  
**Then** 含 hold/scope/template 或 count/reason；**无** variables / 凭证明文

### Invocation retention

#### BDD-CE-G04-007 — TEMPLATE_WINDOW blocks invocation record delete

**Given** ACTIVE TEMPLATE_WINDOW hold 覆盖模板 T 时间窗；invocation I 属 T 且 `createdAt` 在窗内且已过期  
**When** `cleanExpiredRecords`  
**Then** I **仍存在**

#### BDD-CE-G04-008 — INVOCATION_SET blocks artifact + record cleanup

**Given** ACTIVE INVOCATION_SET 含 I；I 产物与记录均已过期  
**When** artifact cleanup 与 record cleanup  
**Then** 产物键与行均保留（或产物清理跳过该行）

#### BDD-CE-G04-009 — Released hold no longer protects invocation

**Given** 曾保护 I 的 hold 已 RELEASED；I 仍过期  
**When** cleanup  
**Then** I 可被删除

#### BDD-CE-G04-010 — Unrelated expired invocation still deleted

**Given** hold 仅保护 T1；过期 invocation 属 T2  
**When** cleanup  
**Then** T2 行删除（既有行为）

### Audit retention

#### BDD-CE-G04-011 — TEMPLATE_WINDOW blocks management audit purge

**Given** ACTIVE TEMPLATE_WINDOW；management 审计行 `templateId=T` 且 `eventAt` 在窗内且超龄  
**When** `purgeManagementAudit`  
**Then** 该行保留；其他超龄非豁免行仍可删

#### BDD-CE-G04-012 — TEMPLATE_WINDOW blocks runtime audit purge

**Given** 同 C11 条件于 runtime 审计表  
**When** `purgeRuntimeAudit`  
**Then** 匹配行保留

#### BDD-CE-G04-013 — INVOCATION_SET protects runtime audit by task/document id

**Given** ACTIVE INVOCATION_SET 含 `inv-1`；runtime 行 `taskExternalId=inv-1`（或 `documentId=inv-1`）超龄  
**When** purge runtime  
**Then** 该行保留

#### BDD-CE-G04-014 — Platform purge-evidence not exempted by TEMPLATE_WINDOW

**Given** management 行 `templateId=null`（如 `AUDIT_RETENTION_PURGE`）超龄  
**When** purge  
**Then** 仍可按 D1 规则删除（不被 TEMPLATE_WINDOW 豁免）

### UI / E2E

#### BDD-CE-G04-015 — GLOBAL_ADMIN sees Legal Holds page

**Given** GLOBAL_ADMIN 会话  
**When** 打开 legal holds 路由  
**Then** 列表可见；可打开创建

#### BDD-CE-G04-016 — Non-admin cannot open page

**Given** 非 GLOBAL_ADMIN  
**When** 直链  
**Then** Forbidden；菜单无入口

#### BDD-CE-G04-017 — UI create + release journey

**Given** GLOBAL_ADMIN 在页面  
**When** 创建 TEMPLATE_WINDOW hold 再 Release  
**Then** 列表状态从 ACTIVE → RELEASED；后端可查审计事件

---

## 11. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| `effectiveTo` null | 开放结束，直至 RELEASED |
| 空 invocation 集合 | 422 |
| >500 IDs | 422 |
| hold 覆盖但行未过期 | 清理本就不删；豁免无害 |
| 多 ACTIVE hold | 任一命中即豁免（OR） |
| 并发释放 | 后写 409 或最后写获胜后第二请求 409 |

---

## 12. Observable evidence

| 层 | 证据 |
| --- | --- |
| DB | V66 `legal_hold` + `legal_hold_invocation` |
| API | CRUD envelope；403/422/409 |
| Schedulers | 单元/集成：007–014 |
| Audit | `LEGAL_HOLD_CREATED` / `LEGAL_HOLD_RELEASED` |
| UI | 管理页 + i18n；E2E 015–017 |
| Gates | `mvn verify`；FE lint/type-check/test/build；Playwright；queued docker-deploy |

---

## 13. Traceability

| 项 | 路径 / ID |
| --- | --- |
| Program | CE-G04 · [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) |
| Task Master | **#75** |
| Slice | `ce-g04-legal-hold` |
| BDD | 本文件 `BDD-CE-G04-001…017` |
| Security | permission-matrix legal hold 行 |
| Domain | domain-model legal hold 简述 |
| Retention | ADR-0040 / ADR-0048（叠加豁免） |
| Touchpoints | `InvocationRetentionCleanupScheduler`、`AuditRetentionCleanupService`、管理路由、`RouteVisibilityService` |

---

## 14. Open questions（非阻塞）

| # | 问题 | 本片默认 | 阻塞？ |
| --- | --- | --- | --- |
| Q1 | INVOCATION_SET 是否豁免 management audit？ | **否**（G04-C13） | 否 |
| Q2 | 释放 API 用 POST 还是 DELETE？ | **POST …/release** | 否 |
| Q3 | 开放 `effectiveTo` 是否自动到期释放？ | **否** — 须显式 release | 否 |

**无阻塞 pending questions。**

---

## 15. BDD readiness

```
bdd_readiness: ready
acceptance_scenario_count: 17
open_questions: [Q1, Q2, Q3]  # non-blocking defaults above
owning_doc: docs/behavior/ce-g04-legal-hold.md
task_ids: [CE-G04, #75, ce-g04-legal-hold]
next: plan-orchestrator → doc-keeper → backend-engineer + frontend-engineer
```

**禁止本片宣称 go-live。** Formal phase 保持 **None**。
