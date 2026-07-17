# BDD 行为规格：AdGroupResolver SPI 解耦与诚实生产边界（PRR-B02）

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-18  
**BDD ID**: `BDD-PRR-B02-LDAP-SPI`  
**来源**: Task Master **#105** PRR-B02 · checklist **#5a** CONDITIONAL residual · [ADR-0054](../adr/authorization-security/0054-ad-group-resolver-production-boundary.md) · [ADR-0010](../adr/authorization-security/0010-ad-group-authorization-resolution.md) · prior [ops-ad-group-stub-close.md](./ops-ad-group-stub-close.md) / [prod-true-prod-contract.md](./prod-true-prod-contract.md)  
**程序 / 清单**: [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md)  
**Task / slice**: `prod-ldap-ad-group-resolver-spi` · `task_ids: ["105"]`  
**Worktree**: `D:/working/DGE-prod-ldap-ad-group-resolver-spi` · `feat/prod-ldap-ad-group-resolver-spi`  
**授权依据**: Parent Stage 1 handoff（2026-07-18）— 确认 SPI、运行时依赖接口、LAB stub 非生产、声称生产无真实目录适配器 → fail-closed；**禁止**虚构公司目录坐标；#5a 保持 **CONDITIONAL**；`frontend_ui_in_scope=false`  
**完成声明约束**: 本叶**仅**交付 SPI 契约确认 + 消费者解耦 + 诚实 fail-closed / 文档边界。**禁止**据此宣称 production go-live；**禁止**将 checklist **#5a** 标为 **GO**；**禁止**翻转 **#3b**；整体清单在其它 NO-GO 项未关闭前仍为 **NO-GO**。

---

## 1. 概述

运行时 AD Group 解析必须以 **`AdGroupResolver` SPI（接口）** 为唯一消费者依赖面。`ConfigAdGroupResolver`（YAML `account-groups`）是该 SPI 的 **LAB / local / dev / test** 实现，不得被 `ApiCredentialAuthenticationFilter`（及同类授权路径）以具体类型硬耦合。

声称生产路径在**没有**运维供给的真实公司目录适配器时，必须继续 **启动期 fail-closed**（既有 `AdGroupResolverGuard` / ADR-0054）。本叶**不**在仓库内发明 LDAP/AD 主机名、Bind DN、凭据或 registry 坐标；checklist **#5a** 在缺少公司目录可审计证据前保持 **CONDITIONAL**（非 GO）。

| 行为域 | 摘要 |
| --- | --- |
| **SPI-C1 Interface SoT** | `AdGroupResolver#resolveGroups(accessAccount)` 为解析 SPI 契约 |
| **SPI-C2 Consumer DI** | 运行时 filter（及任何具体耦合）依赖接口，不依赖 `ConfigAdGroupResolver` |
| **SPI-C3 Authz intersection** | 组交集授权判定在消费者侧（或共享 helper），不锁在 config 具体类上 |
| **SPI-C4 LAB stub bound** | `type=config` / `ConfigAdGroupResolver` = 非生产 only（ADR-0054 不变） |
| **SPI-C5 Claimed-prod refuse** | 无真实目录适配器 → 启动 fail-closed；不得静默 stub |
| **SPI-C6 #5a honesty** | SPI 解耦 ≠ #5a GO；公司坐标 **UNKNOWN** / **CONDITIONAL** |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 发明公司 LDAP/AD 主机名、DN schema、绑定凭据、公司 registry | **禁止** — 坐标 **UNKNOWN** |
| 在无公司坐标时实现并接通「假生产」LDAP 客户端 | **Out of scope** — 不得伪装为真实 AD 证据 |
| 将 checklist **#5a** 标为 **GO** | **禁止** — 缺公司目录实据时保持 **CONDITIONAL** |
| 翻转 checklist **#3b** / 宣称 go-live / 激活 CD-3 | **禁止** |
| 改写 ADR-0010 缓存 / `503 AD_GROUP_RESOLUTION_FAILED` 语义 | **禁止** |
| 改写 ADR-0054 LAB ONLY / prod refuse stub 决策 | **禁止** — 本叶消费并强化，不推翻 |
| 管理端 UI / Playwright / UIUX | **`frontend_ui_in_scope=false`** |
| permission-matrix「凭证 + AD Group」双重授权模型本身 | **禁止改变** — 仅锁定解析依赖面 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **运维 / 平台工程师** | Operator | 声称生产须提供真实目录适配器（当公司目录可用时）；否则接受 fail-closed；不得用 LAB stub 冒充生产 AD |
| **后端工程师** | Developer | 本地 / 测试使用 `ConfigAdGroupResolver`；实现/保持对 `AdGroupResolver` 接口的依赖注入 |
| **系统** | Spring Boot + `AdGroupResolver` SPI + `AdGroupResolverGuard` | 启动期按环境诚实拒绝；请求期按接口解析组并 fail-closed 授权 |
| **发布评审人** | Launch reviewer | #5a 在无公司目录证据时保持 **CONDITIONAL**；不因本叶 SPI 解耦签 GO / go-live |

---

## 3. Goal

1. 确认并固化 `AdGroupResolver` 为 AD Group 解析 SPI（接口契约明确、可替换）。  
2. `ApiCredentialAuthenticationFilter`（及任何具体类型耦合）改为依赖 `AdGroupResolver`。  
3. LAB/config stub 继续明确为非生产 only。  
4. 声称生产在无真实公司目录适配器时 fail-closed（诚实路径）。  
5. 文档与清单对 #5a：**UNKNOWN** 坐标 / **CONDITIONAL**（非 GO）；不发明目录事实。  
6. `frontend_ui_in_scope=false` — 无前端交付。

---

## 4. 已确认决策（confirmed）

| ID | 决策 |
| --- | --- |
| **SPI-C1** | **SPI 契约：** `com.bank.docgen.apimgmt.service.AdGroupResolver` 为唯一解析 SPI。契约方法：`List<String> resolveGroups(String accessAccount)`。空白/null/`accessAccount` 未知 → 空列表（既有 fail-closed 授权语义）。 |
| **SPI-C2** | **消费者依赖接口：** `ApiCredentialAuthenticationFilter` 构造注入与字段类型必须为 `AdGroupResolver`，**不得**声明为 `ConfigAdGroupResolver`。其它生产代码若出现对 `ConfigAdGroupResolver` 的授权/解析耦合，须同样改为接口（本叶至少覆盖 filter）。 |
| **SPI-C3** | **授权交集不属 SPI：** `isAuthorized(accessAccount, allowedAdGroups)`（组交集）**不是** SPI 必需方法。交集判定留在 filter 或共享 helper（输入：`AdGroupResolver` + allowed groups）。`ConfigAdGroupResolver` 可保留实例方法供测试过渡，但生产消费者不得依赖该具体方法签名。 |
| **SPI-C4** | **Config = 非生产实现：** `ConfigAdGroupResolver` + `docgen.ad-group-resolver.type=config` + YAML `account-groups` **仅** LAB / `dev` / `local` / `test`（含 ADR-0054 Decision 7 LAB ONLY 显式覆盖）。行为边界以 [BDD-OPS-AD-GROUP-STUB-001](./ops-ad-group-stub-close.md) / ADR-0054 为准，本叶不放宽。 |
| **SPI-C5** | **声称生产无真实适配器 → fail-closed：** 当 enforce 路径激活（`prod` profile，或环境不在 soft `dev`/`local`/`test`）且：<br>• `type=config` 且未开 LAB ONLY → 启动拒绝；或<br>• `type` 为非 config（如预留 `ldap` / `directory`）但无已实现且已配置的目录适配器 Bean → 启动拒绝。<br>**不得**静默回退 YAML stub。与既有 `AdGroupResolverGuard` 一致。 |
| **SPI-C6** | **本叶不交付虚构 LDAP 客户端：** 因公司目录坐标 **UNKNOWN**，本叶**不**新增绑定到编造 hostname/DN/凭据的生产 LDAP/AD 适配器实现，也**不**把此类实现标为 #5a GO 证据。SPI 存在是为未来运维供给的真实适配器留替换点。 |
| **SPI-C7** | **预留 type 诚实：** 文档可提及预留类型名（如 `ldap` / `directory`）作为 SPI 标识，但必须标明「unimplemented until operator supplies adapter + coords」。测试可断言未实现 type 启动 fail-closed；测试**不得**引入假公司主机名并当作生产事实。 |
| **SPI-C8** | **Checklist #5a：** 本叶交付后 #5a **保持 CONDITIONAL**（或等价诚实残差表述）。**GO** 仅当另有：非虚构目录适配器实现 + 公司/运维确认的目录连通或批准集成证据。SPI 解耦 + guard 维持 **不够** GO。 |
| **SPI-C9** | **不推翻 ADR-0010 / ADR-0054：** 请求期缓存 TTL、失败不缓存、无可用缓存 → `503 AD_GROUP_RESOLUTION_FAILED`；启动 stub/unimplemented refuse；LAB ONLY WARN — 均保持。本叶补「消费者依赖 SPI」可验证行为。 |
| **SPI-C10** | **可观测证据：** 编译/单测证明 filter 注入接口；dev/test 下 config 实现仍可解析已知账号；prod-shaped 无适配器启动拒绝原因明确（无凭据明文）；文档索引本 BDD；#5a 文案含 UNKNOWN / CONDITIONAL。 |
| **SPI-C11** | **`frontend_ui_in_scope=false`：** 无 Vue/i18n/Playwright/UIUX 交付；E2E stages N/A。 |

### 4.1 上游现状（implementation 输入，非已验收「Done」声明）

| 发现 | 证据 |
| --- | --- |
| SPI 接口已存在 | `AdGroupResolver.java` — 仅 `resolveGroups` |
| 唯一实现 | `ConfigAdGroupResolver` `@Service` implements `AdGroupResolver`；另有 `isAuthorized` 具体方法 |
| Filter 硬耦合具体类 | `ApiCredentialAuthenticationFilter` 字段/构造为 `ConfigAdGroupResolver`；调用 `isAuthorized` + `resolveGroups` |
| 启动 guard 已存在 | `AdGroupResolverGuard` — prod refuse config；unimplemented non-config refuse；LAB ONLY WARN |
| LAB vs claimed-prod compose | #102 / [prod-true-prod-contract.md](./prod-true-prod-contract.md) — LAB knobs on `docker-compose.lab.yml` |
| #5a | **CONDITIONAL** — 缺真实 LDAP/AD + 公司目录证据；coords **UNKNOWN** |
| 公司 LDAP/AD | **UNKNOWN** — 不得虚构 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 后端模块装配 / 应用启动 | Guard 校验 resolver type vs 环境 |
| T2 | 运行时 API 请求经 `ApiCredentialAuthenticationFilter` | 凭证校验后解析 AD Group 并做授权交集 |
| T3 | 开发者替换/注入 `AdGroupResolver` 实现（测试或未来适配器） | 消费者仅见接口 |
| T4 | 发布评审核对 checklist #5a | 按 SPI-C8 诚实 verdict |

---

## 6. Preconditions

- ADR-0054 Accepted；`AdGroupResolverGuard` 行为已落地（#46）。  
- #102 claimed-prod vs LAB compose 诚实已落地。  
- 公司 LDAP/AD 坐标在仓库中仍为 **UNKNOWN**。  
- 本叶在隔离 worktree `DGE-prod-ldap-ad-group-resolver-spi` 交付。

---

## 7. Primary journey（成功路径 — LAB / local）

1. 进程以 `dev` / `local` / `test`（或 LAB ONLY 显式覆盖的验收栈）启动。  
2. Spring 将 `ConfigAdGroupResolver` 作为 `AdGroupResolver` 唯一实现注入 filter。  
3. 调用方携带有效 API 凭证 + `X-Access-Account`。  
4. Filter 经接口 `resolveGroups` 取得调用方组，与策略 `allowedAdGroups` 做交集。  
5. 交集非空 → 认证通过，会话 claims 含解析组；否则 403 `ACCESS_DENIED` / adGroupDenied。

---

## 8. System responses

| 路径 | 系统响应 |
| --- | --- |
| LAB/dev/test + config stub | 启动成功；接口解析 YAML 映射；未知账号空组 → 授权 deny |
| 声称生产 + config stub（无 LAB ONLY） | 启动 **fail-closed**（`IllegalStateException` 或等价） |
| 声称生产 + 未实现/未配置 directory type | 启动 **fail-closed**；不静默回退 config |
| 声称生产 + LAB ONLY override | 启动允许 config；**WARN**「非生产目录解析」（既有） |
| Filter 依赖面 | 仅编译期/运行期依赖 `AdGroupResolver` |
| #5a 本叶后 | **CONDITIONAL**；坐标 **UNKNOWN**；**非 GO** |

---

## 9. Acceptance scenarios（Given / When / Then）

### S1 — Filter 依赖 SPI 接口（BDD-PRR-B02-LDAP-SPI-001）

```gherkin
Given 代码库中存在 AdGroupResolver 接口与 ConfigAdGroupResolver 实现
When 审查 ApiCredentialAuthenticationFilter 的构造注入与字段类型
Then 依赖类型为 AdGroupResolver（接口）
  And 不得将 ConfigAdGroupResolver 作为生产消费者的声明类型
```

### S2 — 授权交集不绑定具体类（BDD-PRR-B02-LDAP-SPI-002）

```gherkin
Given ApiCredentialAuthenticationFilter 仅持有 AdGroupResolver
  And 策略 allowedAdGroups 非空
When 对已知映射账号调用运行时认证（dev/test + type=config）
Then 系统通过 resolveGroups 取得调用方组并完成交集判定
  And 交集命中时认证成功；未命中时 ACCESS_DENIED（既有语义）
  And 判定路径不要求调用 ConfigAdGroupResolver 特有方法作为唯一入口
```

### S3 — LAB/config stub 仍为非生产（BDD-PRR-B02-LDAP-SPI-003）

```gherkin
Given docgen.ad-group-resolver.type=config
  And 进程处于纯 dev/local/test（无 prod profile enforce）
When 应用启动并解析已知 accessAccount
Then ConfigAdGroupResolver 作为 AdGroupResolver 实现可用
  And 未知/空白账号 resolveGroups 返回空列表且授权 deny
```

### S4 — 声称生产无真实适配器 fail-closed（BDD-PRR-B02-LDAP-SPI-004）

```gherkin
Given 验收/生产 enforce 路径激活（prod profile 或非 soft 环境）
  And 未启用 LAB ONLY allow-config-stub-on-prod-profile
  And 不存在已配置的真实公司目录适配器 Bean
When 后端执行 AdGroupResolverGuard（或等价启动校验）
Then 进程拒绝启动（config stub 或 unimplemented/unconfigured type）
  And 不得静默以 ConfigAdGroupResolver + YAML 充当生产目录解析
```

### S5 — 未实现 directory type 不虚构坐标（BDD-PRR-B02-LDAP-SPI-005）

```gherkin
Given 验收/生产 enforce 路径激活
  And docgen.ad-group-resolver.type 为预留非 config 值（如 ldap/directory）
  And 仓库内无已接通的真实目录适配器（公司坐标 UNKNOWN）
When 后端执行启动校验
Then 启动 fail-closed
  And 文档/测试/配置不得写入编造的公司 LDAP 主机名或凭据并标为生产事实
```

### S6 — Checklist #5a 诚实 CONDITIONAL（BDD-PRR-B02-LDAP-SPI-006）

```gherkin
Given 本叶完成 SPI 确认与 filter 解耦且 gates 按管线通过
When 发布评审更新或核对 launch-readiness checklist #5a
Then #5a 保持 CONDITIONAL（或等价：仍缺公司目录实据）
  And 不得标为 GO
  And 须标明公司目录坐标 UNKNOWN / 禁止虚构
  And 不得宣称 production go-live 或翻转 #3b
```

### S7 — 前端不在范围（BDD-PRR-B02-LDAP-SPI-007）

```gherkin
Given frontend_ui_in_scope=false
When 本叶交付范围裁定
Then 无管理端 UI / Playwright / UIUX 强制门禁
  And 后端行为以 mvn verify 与启动 guard / filter 单测为可观测证据
```

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| null/blank `accessAccount` | `resolveGroups` → 空列表 → 授权 deny |
| 空 `allowedAdGroups` | 授权 deny（既有） |
| 真实目录瞬时故障（未来适配器） | ADR-0010：`503 AD_GROUP_RESOLUTION_FAILED`；本叶不改 |
| LAB ONLY 覆盖 | WARN；≠ 生产 AD；≠ #5a GO |
| 多实现 Bean | 本叶默认仍单实现 config；未来真实适配器须由 type/配置显式选择，禁止静默双绑定歧义（实现期由 plan/architect 约束） |

---

## 11. Observable evidence

| 证据 | 用途 |
| --- | --- |
| Filter 源码类型为 `AdGroupResolver` | S1 |
| 单元/切片测试：接口注入 + 交集授权 | S2–S3 |
| `AdGroupResolverGuardTest`（回归）prod refuse / unimplemented | S4–S5 |
| `docs/behavior/prod-ldap-ad-group-resolver-spi.md` + `docs/README.md` 索引 | 可追溯 |
| checklist #5a 文案含 CONDITIONAL + UNKNOWN | S6 |
| `mvn -B -ntp -f backend/pom.xml verify` GREEN（实现阶段） | 质量门禁 |
| FE/E2E | N/A（S7） |

---

## 12. Traceability

| 项 | 链接 |
| --- | --- |
| Task Master | **#105** PRR-B02 |
| Slice | `prod-ldap-ad-group-resolver-spi` |
| Prior | **#46** ops-ad-group-stub-close；**#102** prod-true-prod-contract |
| Checklist | [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) **#5a** — 保持 **CONDITIONAL** |
| ADR | [ADR-0054](../adr/authorization-security/0054-ad-group-resolver-production-boundary.md)、[ADR-0010](../adr/authorization-security/0010-ad-group-authorization-resolution.md) |
| Related BDD | [ops-ad-group-stub-close.md](./ops-ad-group-stub-close.md)、[prod-true-prod-contract.md](./prod-true-prod-contract.md) |
| Permission model | [permission-matrix.md](../security/permission-matrix.md) — 凭证 + AD Group（不改模型） |

---

## 13. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/prod-ldap-ad-group-resolver-spi.md
task_ids: ["105"]
slice_id: prod-ldap-ad-group-resolver-spi
frontend_ui_in_scope: false
scenario_ids:
  - BDD-PRR-B02-LDAP-SPI-001
  - BDD-PRR-B02-LDAP-SPI-002
  - BDD-PRR-B02-LDAP-SPI-003
  - BDD-PRR-B02-LDAP-SPI-004
  - BDD-PRR-B02-LDAP-SPI-005
  - BDD-PRR-B02-LDAP-SPI-006
  - BDD-PRR-B02-LDAP-SPI-007
out_of_scope:
  - Invented company LDAP/AD hostname / DN / credentials / registry
  - In-repo fake "production" LDAP client presented as #5a GO evidence
  - checklist #5a → GO
  - checklist #3b / go-live / CD-3
  - frontend UI / Playwright / UIUX
  - ADR-0010 cache/503 rewrite; ADR-0054 decision rewrite
next: plan-orchestrator
```

**Handoff note for plan-orchestrator / implementers：**

1. TDD Red：filter 构造/字段改为 `AdGroupResolver`；将交集判定移出对 `ConfigAdGroupResolver.isAuthorized` 的硬依赖。  
2. 保持 `AdGroupResolverGuard` + ADR-0054 LAB/prod 语义；勿放宽 claimed-prod。  
3. **不要**编造 LDAP 主机或接通假目录客户端并宣称 #5a GO。  
4. 文档/清单同步时 #5a = **CONDITIONAL** + coords **UNKNOWN**。  
5. `frontend_ui_in_scope=false` — 跳过 FE/E2E/UIUX。  
6. `mvn verify` 在隔离 worktree 执行。
