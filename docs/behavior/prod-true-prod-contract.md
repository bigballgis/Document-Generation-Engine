# BDD 行为规格：PRR-B01 Wave B item 5 — True production contract（非 LAB）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-PRR-B01-TPC` |
| **编写日期** | 2026-07-18 |
| **程序 / 队列** | NON-CE PRR Wave B — Task Master **#102** PRR-B01（本叶仅 **item 5 true prod contract**；LDAP/AD 适配器 **CONDITIONAL** 另排队） |
| **Slice** | `prod-true-prod-contract` |
| **Branch** | `feat/prod-true-prod-contract` |
| **Worktree** | `D:/working/DGE-prod-true-prod-contract` |
| **Placement** | ISOLATED |
| **Task Master** | **#102**（本叶 scope = Wave B item 5 ONLY；不交付真实 LDAP/AD） |
| **Formal phase** | **None** |
| **Owning docs** | **本文件（本叶行为 SoT）**；关联 [ops-jwt-secret-no-default.md](./ops-jwt-secret-no-default.md)、[ops-ad-group-stub-close.md](./ops-ad-group-stub-close.md)、[ops-kafka-company-registry.md](./ops-kafka-company-registry.md)、[ADR-0054](../adr/authorization-security/0054-ad-group-resolver-production-boundary.md)、[ADR-0044](../adr/operations/0044-deployment-topology-v1.md)、[launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（ops / backend / compose；无管理 UI） |

**完成声明约束：** 本叶仅关闭「声称生产」路径与 LAB 验收路径混用导致的契约诚实缺口（profiles / `APP_ENVIRONMENT` / Kafka async / demo classpath / AD stub **compose 默认** / `ProductionAsyncTransportGuard` 对齐）。**不**实现 LDAP/`AdGroupResolver` 目录适配器；**不**宣称 go-live；**不**翻转 checklist **#3b**；**不**将 #5a 标为 **GO**（仍 **CONDITIONAL**，等真实目录证据叶）。

---

## 1. 概述

当前 `docker-compose.prod.yml` 与若干 Production*Guard 的组合实际是 **LAB / 本地验收形态**（`SPRING_PROFILES_ACTIVE=prod,dev`、`APP_ENVIRONMENT=dev`、`ASYNC_TRANSPORT` 默认可 `in-process`、demo classpath 默认真、AD stub LAB 覆盖默认真），却挂在 **prod** 文件名下，易被误认为「声称生产」契约。

本叶把 **claimed production（真生产契约）** 与 **LAB acceptance（本地验收）** 分开：声称生产必须单 `prod` profile、`APP_ENVIRONMENT=prod`、强制 Kafka async、禁止 demo classpath 默认开启、禁止 AD stub LAB 覆盖默认开启，并将 `ProductionAsyncTransportGuard` 的强制作用域对齐 `ProductionSecretGuard` / `AdGroupResolverGuard`（`prod` profile 激活即 enforce）。

| 行为域 | 摘要 |
| --- | --- |
| **TPC-C1 Compose 真生产默认** | `docker-compose.prod.yml`（及同等「声称生产」入口）不得烘焙 LAB 默认 |
| **TPC-C2 单 prod profile** | 声称生产 `SPRING_PROFILES_ACTIVE` / Spring active profiles = **`prod` only**（不得 `prod,dev`） |
| **TPC-C3 APP_ENVIRONMENT=prod** | 声称生产 `APP_ENVIRONMENT` / `docgen.environment` = **`prod`** |
| **TPC-C4 Force Kafka async** | 声称生产 `ASYNC_TRANSPORT=kafka`（compose 强制；guard fail-closed） |
| **TPC-C5 No demo classpath default** | 声称生产不得默认启用 `DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED` |
| **TPC-C6 No AD stub default** | 声称生产不得默认 `allow-config-stub-on-prod-profile=true`（compose 默认关闭/省略；**不**交付 LDAP 适配器） |
| **TPC-C7 Guard 对齐** | `ProductionAsyncTransportGuard` 与 JWT / AD Guard 同诚实：`prod` profile → 强制 kafka |
| **TPC-C8 LAB 显式分离** | LAB/E2E  knobs 不得继续作为 `docker-compose.prod.yml` 的默认；须显式 LAB 覆盖/叠加文件并标注 LAB ONLY |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 真实 LDAP/AD 适配器 + 公司目录坐标 / 集成证据 | **Out of scope — CONDITIONAL residual**（#5a；另叶排队；**禁止虚构**主机/DN/凭据） |
| 改写 ADR-0054 / ADR-0010 已接受决策正文 | **禁止** — 仅消费其边界 |
| JWT / Kafka 镜像 registry / paste↔binding | **Out of scope**（已有独立 BDD） |
| ADR-0042 / ADR-0043 / checklist #3b | **Out of scope** |
| Frontend UI / Playwright E2E / UIUX | **Out of scope**（`frontend_ui_in_scope=false`） |
| 宣称 production go-live / 激活 CD-3 | **禁止** |
| 多副本 scale-out / Redis SSE registry | **Out of scope**（ADR-0044 v1 单副本约束保持） |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **运维 / 平台工程师** | Operator | 部署「声称生产」栈；显式供给 secrets；使用真生产 compose/Helm values |
| **开发者 / QA** | Developer | 本地 LAB/E2E 使用**显式标注**的 LAB 覆盖；不得把 LAB 默认当成生产 |
| **系统** | Compose + Spring Boot + Production*Guards | 真生产路径 fail-closed；LAB 仅在显式覆盖下允许验收 knobs |
| **发布评审人** | Launch reviewer | 本叶**不**把 #5a 翻 GO；不得据此签整体 go-live |

---

## 3. Goal

1. 「声称生产」入口（至少 `docker-compose.prod.yml` + 对齐的 Helm prod values）默认满足：单 `prod` profile、`APP_ENVIRONMENT=prod`、Kafka async、无 demo classpath 默认、无 AD stub LAB 默认。  
2. `ProductionAsyncTransportGuard` 在 `prod` profile 激活时强制 `docgen.async.transport=kafka`，**不得**因同时存在 `dev` profile 或历史 `APP_ENVIRONMENT=dev` 习惯而跳过。  
3. 纯 `dev` / `local` / `test`（无 `prod` profile）仍允许 in-process async 与文档化本地默认。  
4. LAB 验收能力保留为**显式**覆盖路径，且不得冒充真生产默认。  
5. **不**交付 LDAP；AD stub 在真生产路径上保持既有 fail-closed / 无默认 LAB 覆盖语义。  
6. API/ops 证据为主；无 FE/E2E 义务。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **TPC-C1** | **真生产 vs LAB：** `docker-compose.prod.yml` 是**声称生产**契约文件，其**默认环境**必须是真生产形态，不得再默认烘焙 LAB  knobs（`prod,dev`、`APP_ENVIRONMENT=dev`、`ASYNC_TRANSPORT=in-process`、demo classpath `true`、AD stub allow `true`）。 | handoff |
| **TPC-C2** | **单 prod profile：** 声称生产后端 `SPRING_PROFILES_ACTIVE`（或等价）必须为 **`prod`** only。禁止默认 `prod,dev`。 | handoff |
| **TPC-C3** | **`APP_ENVIRONMENT=prod`：** 声称生产必须设置 `APP_ENVIRONMENT=prod`（绑定 `docgen.environment=prod`）。禁止默认 `dev`。 | handoff |
| **TPC-C4** | **Force Kafka async（compose）：** 声称生产 compose 必须将 `ASYNC_TRANSPORT` 固定为 `kafka`（或 `${ASYNC_TRANSPORT:?…}` 且文档/校验要求值为 `kafka`；**禁止** `${ASYNC_TRANSPORT:-in-process}` 作为 prod 文件默认）。`KAFKA_BOOTSTRAP_SERVERS` 继续指向可解析的 broker（docker 网络内地址或运维供给）。 | handoff + ADR-0044 kafka-in-prod 诚实 |
| **TPC-C5** | **Force Kafka async（guard）：** `ProductionAsyncTransportGuard` 在验收/生产边界上若 transport ≠ `kafka`（大小写不敏感）→ 启动期 **fail-closed**（`IllegalStateException` 或等价，消息含 kafka 语义）。 | handoff + 现有 guard 意图 |
| **TPC-C6** | **Guard 作用域对齐（关键）：** `ProductionAsyncTransportGuard` 的 enforce 条件必须与 `ProductionSecretGuard` JWT / `AdGroupResolverGuard` **同一诚实模型**：<br>• **若** active profiles 含 **`prod`** → **必须 enforce**（即使历史上混有 `dev` profile，或 `APP_ENVIRONMENT` 仍为 `dev`）；<br>• **否则**若 profiles 为纯 `dev`/`local`/`test` → 不 enforce；<br>• **否则**按 `docgen.environment` / `APP_ENVIRONMENT`：不在 `dev`/`local`/`test` 时 enforce。<br>禁止继续使用「任意 `dev` profile 即整段跳过」导致 `prod,dev` 绕过 Kafka 强制。 | handoff + 代码差距 |
| **TPC-C7** | **No demo classpath default：** 声称生产不得默认 `DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED=true`。默认必须为 **false** / 省略（应用默认 false 已存在于 `application.yml`）。LAB 若需 demo classpath，仅显式 LAB 覆盖开启。 | handoff |
| **TPC-C7a** | **N23 / Wave 8：** demo classpath ≠ Asset Library。声称生产亦不得默认启用 **managed-asset demo/验收 seed**；产品默认零 managed 资产 = honest empty。Seed 与 classpath 均为显式非生产默认 — [demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md)；[sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md) W8-C2/C3/W8-004。 | SYS-NORM Wave 8 |
| **TPC-C8** | **No AD stub default（compose only）：** 声称生产不得默认 `DOCGEN_AD_GROUP_RESOLVER_ALLOW_CONFIG_STUB_ON_PROD_PROFILE=true`（或 `${…:-true}`）。默认必须为 **false** / 省略。既有 `AdGroupResolverGuard` + ADR-0054 LAB ONLY 语义保持；本叶**不**实现目录适配器。真生产在无真实适配器时 fail-closed（或运维显式开 LAB — 但那不是声称生产）属诚实行为。 | handoff；LDAP OOS |
| **TPC-C9** | **LAB 显式分离：** 本地 Docker 验收 / E2E 若仍需 `prod`-shaped + config stub / demo classpath /（可选）软环境标签，必须通过**显式**机制获得（实现二选一或组合，行为以「prod 文件默认不再是 LAB」为准）：(a) 独立 `docker-compose.lab.yml`（或等价 overlay）叠加并注释 **LAB ONLY**；(b) 部署脚本/文档要求导出 LAB 环境变量。LAB 覆盖**不得**静默成为 `docker-compose.prod.yml` 默认。 | handoff「not LAB」 |
| **TPC-C10** | **Helm 对齐：** `deploy/helm/docgen/values-prod.yaml` 已 `appEnvironment: prod` — 保持。声称生产安装路径不得把 `config.appEnvironment` 默认为 `dev` 同时声称 prod。`values.yaml` 基线若仍 `springProfilesActive: prod` + `appEnvironment: dev`，实现须修正为不误导（至少 prod overlay / 文档诚实；优先让「prod 安装」得到 `prod`/`prod`）。本叶不强制改 staging/dev overlays。 | 仓库现状 |
| **TPC-C11** | **Secrets / JWT 不回退：** 保持 [BDD-OPS-JWT-SECRET-001](./ops-jwt-secret-no-default.md)：`JWT_SECRET` 仍 `${JWT_SECRET:?…}`；`ProductionSecretGuard` 已知不安全密钥拒绝在 `prod` profile 下继续生效。本叶不扩大 Postgres/MinIO 默认口令清理范围。 | regression |
| **TPC-C12** | **Kafka 镜像坐标：** 继续遵守 [BDD-OPS-KAFKA-REGISTRY-001](./ops-kafka-company-registry.md)（`${KAFKA_IMAGE:?…}`；不发明公司 registry）。本叶只强制 async transport=kafka，不改镜像治理。 | OOS boundary |
| **TPC-C13** | **FE / E2E：** `frontend_ui_in_scope=false`。Done 主证据 = compose/配置断言 + guard 单测 + `mvn verify`；队列部署证据按管线（ops 栈）可选验证 healthz，**无** Playwright/UIUX 义务。 | handoff |
| **TPC-C14** | **Checklist 诚实：** 本叶**不**将 #5a 标 **GO**；#5a 仍 **CONDITIONAL**（缺真实 LDAP/AD）。整体 launch checklist 仍 **NO-GO**。禁止 go-live。 | #5a / handoff |
| **TPC-C15** | **可观测证据：** Kafka 拒绝须有明确启动失败原因（含 kafka / async transport 语义；**不得**打印 secrets）；成功路径 `/healthz` 可达（依赖健康前提下）。 | ops |

### 4.1 上游现状（implementation 输入，非已验收行为）

| 发现 | 证据 |
| --- | --- |
| Compose 混用 `prod,dev` | `docker-compose.prod.yml`：`SPRING_PROFILES_ACTIVE: prod,dev` |
| Compose `APP_ENVIRONMENT: dev` | 同文件 |
| Compose async 默认可 in-process | `ASYNC_TRANSPORT: ${ASYNC_TRANSPORT:-in-process}` |
| Compose AD stub LAB 默认 true | `DOCGEN_AD_GROUP_…: ${DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB:-true}` |
| Compose demo classpath 默认 true | `DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED: ${…:-true}` |
| Async guard 被 `dev` profile 绕过 | `ProductionAsyncTransportGuard.enforceGuard()`：`acceptsProfiles(dev,local,test)` → return false；故 **`prod,dev` 永不强制 kafka** |
| JWT / AD guards 已按 `prod` profile 强制 | `ProductionSecretGuard.enforceJwtSecretGuard` / `AdGroupResolverGuard.enforceAcceptanceOrProductionBoundary` |
| Helm prod overlay 已 `appEnvironment: prod` | `values-prod.yaml`；基线 `values.yaml` 仍 `appEnvironment: dev` |
| 应用默认 async=in-process、demo classpath=false | `application.yml` |
| LDAP 适配器未实现 | ADR-0054 / #5a CONDITIONAL |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 运维以声称生产入口（`docker-compose.prod.yml` / Helm prod）启动后端 | 成功路径须满足 TPC-C2…C8 |
| T2 | 声称生产路径上 `ASYNC_TRANSPORT=in-process`（或非 kafka）启动 | Guard fail-closed（S2） |
| T3 | 纯 `dev`/`local`/`test`（无 `prod`）使用 in-process | 允许（S3） |
| T4 | `prod` profile 激活且 transport=kafka、其它生产默认合规 | 允许启动（S1） |
| T5 | 有人误用历史 LAB 默认当作生产 | 默认已消除；仅显式 LAB 覆盖可恢复 LAB knobs（S4） |

---

## 6. Preconditions

- 工作树：`D:/working/DGE-prod-true-prod-contract` · `feat/prod-true-prod-contract`。  
- Formal phase：**None**；单宿主 Docker 仍经 `.\scripts\docker-deploy-queue.ps1`。  
- JWT / AD stub close / Kafka image 相关切片已在 main 历史中交付；本叶在其上对齐真生产默认与 async guard。  
- 公司 LDAP 坐标 **UNKNOWN** — 本叶不 consumable。  
- Kafka broker 在 prod compose `depends_on` 路径上可用（既有 `docgen-kafka`）。

---

## 7. Primary journey（成功 — 本切片范围内）

1. Operator 使用声称生产入口（更新后的 `docker-compose.prod.yml` 或 Helm prod values）。  
2. 环境呈现：`SPRING_PROFILES_ACTIVE=prod`、`APP_ENVIRONMENT=prod`、`ASYNC_TRANSPORT=kafka`、demo classpath 关闭、AD stub LAB 覆盖关闭/省略。  
3. `ProductionAsyncTransportGuard`（及既有 JWT/secret guards）通过；后端启动。  
4. `/healthz` 报告 UP（依赖健康前提下）。  
5. Developer 若需 LAB/E2E，显式叠加 LAB overlay / 导出 LAB 变量；不得依赖 prod 文件静默默认。

---

## 8. System responses

| 路径 | 系统响应 |
| --- | --- |
| **成功（声称生产）** | 进程启动；async=kafka；profiles=`prod`；`docgen.environment=prod`；demo classpath off；AD stub LAB off；healthz UP |
| **失败 — 非 kafka async（prod 边界）** | 启动拒绝；明确 IllegalStateException（或等价）；进程不对外服务 |
| **失败 — JWT 已知不安全 / 缺 JWT_SECRET** | 保持既有 JWT BDD（不回退） |
| **失败 — AD config stub 且无 LAB 覆盖（prod 边界）** | 保持既有 ADG fail-closed（本叶不改 SPI；仅去掉 compose 默认 true） |
| **LAB 显式覆盖** | 允许验收 knobs；日志/注释标明 **LAB ONLY**；**不得**称为声称生产 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-PRR-B01-TPC-001 — 声称生产 compose：单 `prod` profile

```gherkin
Given 运维使用 docker-compose.prod.yml（不叠加 LAB overlay）启动 docgen-backend
When 检查容器/进程 Spring 激活 profiles（或等价环境 SPRING_PROFILES_ACTIVE）
Then 激活 profiles 仅为 prod（不得默认包含 dev）
And 该默认不得再写死为 prod,dev
```

### BDD-PRR-B01-TPC-002 — 声称生产：`APP_ENVIRONMENT=prod`

```gherkin
Given 运维使用 docker-compose.prod.yml（不叠加 LAB overlay）启动 docgen-backend
When 检查 APP_ENVIRONMENT / docgen.environment
Then 值为 prod
And 不得默认保持为 dev
```

### BDD-PRR-B01-TPC-003 — 声称生产 compose：强制 Kafka async

```gherkin
Given docker-compose.prod.yml 作为声称生产入口
When 检查 ASYNC_TRANSPORT 默认绑定
Then 默认（或强制）为 kafka
And 不得再出现 ${ASYNC_TRANSPORT:-in-process} 作为该文件的生产默认
And KAFKA_BOOTSTRAP_SERVERS 指向 compose 网络内可解析 broker（或运维供给）
```

### BDD-PRR-B01-TPC-004 — 声称生产：无 demo classpath 默认开启

```gherkin
Given docker-compose.prod.yml 作为声称生产入口（无 LAB overlay）
When 检查 DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED
Then 默认关闭（false 或省略后应用默认 false）
And 不得 ${…:-true}
```

### BDD-PRR-B01-TPC-005 — 声称生产：无 AD stub LAB 默认开启

```gherkin
Given docker-compose.prod.yml 作为声称生产入口（无 LAB overlay）
When 检查 DOCGEN_AD_GROUP_RESOLVER_ALLOW_CONFIG_STUB_ON_PROD_PROFILE（或等价）
Then 默认关闭（false 或省略）
And 不得 ${DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB:-true}
And 本场景不要求已实现 LDAP 适配器（fail-closed 于 stub 是可接受诚实结果）
```

### BDD-PRR-B01-TPC-006 — Async guard：`prod` profile 拒绝 in-process（对齐其它 Production*Guards）

```gherkin
Given Spring active profiles 包含 prod
And docgen.async.transport = in-process
When ProductionAsyncTransportGuard.verifyOrThrow 执行
Then 抛出 IllegalStateException（或等价）且消息含 kafka 语义
And 即使历史上 APP_ENVIRONMENT=dev 或曾混用 dev profile，只要 prod 在作用域模型中要求 enforce，仍必须拒绝
```

### BDD-PRR-B01-TPC-007 — Async guard：`prod` + kafka 允许启动校验通过

```gherkin
Given Spring active profiles 包含 prod
And docgen.environment = prod
And docgen.async.transport = kafka
When ProductionAsyncTransportGuard.verifyOrThrow 执行
Then 不抛出异常
```

### BDD-PRR-B01-TPC-008 — 纯本地路径仍允许 in-process

```gherkin
Given Spring active profiles 为纯 dev 或 local 或 test（不含 prod）
And docgen.environment 为 dev 或 local 或 test
And docgen.async.transport = in-process
When ProductionAsyncTransportGuard.verifyOrThrow 执行
Then 不抛出异常
```

### BDD-PRR-B01-TPC-009 — LAB 覆盖显式且非 prod 文件默认

```gherkin
Given 仓库提供 LAB/E2E 所需 knobs（若仍需要）
When 检查 docker-compose.prod.yml 默认环境块
Then 其中不再默认开启 AD stub LAB / demo classpath / APP_ENVIRONMENT=dev / prod,dev / in-process
And LAB knobs 仅能通过显式 overlay 或显式环境变量获得，并标注 LAB ONLY
```

### BDD-PRR-B01-TPC-010 — Helm 声称生产环境标签诚实

```gherkin
Given 使用 Helm prod values（values-prod.yaml 或等价声称生产安装）
When 检查 config.springProfilesActive 与 config.appEnvironment（或渲染后的 ConfigMap）
Then springProfilesActive = prod
And appEnvironment = prod
And 不得在声称生产安装路径上默认 appEnvironment=dev
```

### BDD-PRR-B01-TPC-011 — 回归：JWT 显式供给与不安全密钥拒绝不回退

```gherkin
Given prod profile 激活
When JWT_SECRET 缺失于 compose 要求，或等于已知不安全默认集合中的值
Then 启动路径仍 fail-closed（BDD-OPS-JWT-SECRET-001 语义保持）
```

### BDD-PRR-B01-TPC-012 — 成功路径可观测（ops）

```gherkin
Given 声称生产入口配置合规（prod-only、APP_ENVIRONMENT=prod、ASYNC_TRANSPORT=kafka、显式安全 JWT_SECRET、依赖健康）
And AD 路径满足既有 guard（真实适配器或诚实 fail-closed — LDAP 非本叶）
When 后端完成启动
Then /healthz 在依赖就绪后可达 UP
And 异步传输为 kafka（配置/ready 检查可证明）
```

---

## 10. Boundary / exception

| 边界 | 期望 |
| --- | --- |
| `prod` + `in-process` | **拒绝启动**（TPC-C5/C6） |
| `prod` + 非 kafka 其它值 | **拒绝启动** |
| 纯 `dev`/`local`/`test` + in-process | **允许** |
| 声称生产 + AD stub 且无 LAB 覆盖 | **拒绝启动**（既有 ADG；本叶只去默认 true） |
| 显式 LAB overlay 开 stub | **允许**（LAB ONLY WARN 保持）；**≠** 声称生产 |
| 缺 `JWT_SECRET` | compose/guard fail-closed（既有） |
| 公司 LDAP 未提供 | **不编造**；#5a 保持 CONDITIONAL |
| Frontend 管理 UI | **无行为变更义务** |

---

## 11. Observable evidence

| 证据 | 用途 |
| --- | --- |
| `docker-compose.prod.yml`（及可选 LAB overlay）diff | TPC-C1…C5/C7/C8/C9 |
| `ProductionAsyncTransportGuard` + 单测 | TPC-C6；BDD-PRR-B01-TPC-006…008 |
| Helm `values-prod.yaml` / 基线诚实修正 | TPC-C10；TPC-010 |
| `mvn -B -ntp -f backend/pom.xml verify` | 后端门禁 |
| 可选：队列部署后 `/healthz` | TPC-012 |
| **无** Playwright / UIUX 包 | `frontend_ui_in_scope=false` |

---

## 12. Traceability

| 项 | 链接 |
| --- | --- |
| Task Master | **#102** PRR-B01（本叶 = Wave B item 5 true prod contract） |
| Slice | `prod-true-prod-contract` |
| Checklist | [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) — #5 / #5a 诚实；本叶不翻 #5a GO |
| Related BDD | [ops-jwt-secret-no-default.md](./ops-jwt-secret-no-default.md)、[ops-ad-group-stub-close.md](./ops-ad-group-stub-close.md)、[ops-kafka-company-registry.md](./ops-kafka-company-registry.md) |
| ADR | [ADR-0054](../adr/authorization-security/0054-ad-group-resolver-production-boundary.md)、[ADR-0044](../adr/operations/0044-deployment-topology-v1.md) |
| Prior PRR | [prod-library-export-streaming.md](./prod-library-export-streaming.md)（#101 Done） |
| 后续 CONDITIONAL | LDAP/`AdGroupResolver` 真实目录适配器证据叶（另排；禁止虚构坐标） |

---

## 13. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/prod-true-prod-contract.md
task_ids: ["102"]
slice_id: prod-true-prod-contract
frontend_ui_in_scope: false
scenario_ids:
  - BDD-PRR-B01-TPC-001
  - BDD-PRR-B01-TPC-002
  - BDD-PRR-B01-TPC-003
  - BDD-PRR-B01-TPC-004
  - BDD-PRR-B01-TPC-005
  - BDD-PRR-B01-TPC-006
  - BDD-PRR-B01-TPC-007
  - BDD-PRR-B01-TPC-008
  - BDD-PRR-B01-TPC-009
  - BDD-PRR-B01-TPC-010
  - BDD-PRR-B01-TPC-011
  - BDD-PRR-B01-TPC-012
out_of_scope:
  - LDAP / AdGroupResolver directory adapter (CONDITIONAL residual #5a)
  - frontend UI / Playwright / UIUX
  - go-live / checklist #3b / CD-3
next: plan-orchestrator
```

**Handoff note for plan-orchestrator / implementers：** TDD Red 优先锁定 `ProductionAsyncTransportGuard` 在 `prod`（及 `prod`+历史混用）下拒绝 `in-process`；再改 `docker-compose.prod.yml` 默认与可选 LAB overlay；Helm prod 环境标签诚实；**不要**在本叶实现 LDAP。
