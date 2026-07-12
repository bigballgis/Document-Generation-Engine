# BDD 行为规格：AD Group 解析 — 生产路径禁止 config stub 静默充当目录

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-12  
**BDD ID**: `BDD-OPS-AD-GROUP-STUB-001`  
**来源**: LR-E2 checklist item **#5a** · Ledger seam「AD Group resolution」· ADR-0010（缓存/fail-closed 语义；**未**裁定目录适配器 vs config stub）  
**程序 / 清单**: [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) · [launch-readiness-program.md](../plan/launch-readiness-program.md)  
**Task / slice**: `ops-ad-group-stub-close`  
**Worktree**: `D:/working/DGE-ops-ad-group-stub-close` · `feat/ops-ad-group-stub-close`  
**授权依据**: 本会话用户 / parent 明确确认的方向（2026-07-12）— ADR + fail-closed config 缝（镜像 JWT #9 / Kafka #10）；禁止虚构公司 LDAP/AD 主机名/schema/凭据；诚实 **CONDITIONAL**；整体仍 **NO-GO**；「自动继续」  
**完成声明约束**: 本切片**仅**处理 checklist **#5a** 的可验证路径与诚实 verdict；**禁止**据此宣称 production go-live。整体清单在其它 NO-GO 项未关闭前仍为 **NO-GO**。清除 #5a residual **alone ≠ go-live**。

---

## 1. 概述

生产 / 验收形态路径上的 AD Group 解析**不得**静默以 `docgen.ad-group-resolver.type=config`（YAML 账号→组映射 stub）冒充企业目录解析并对外服务流量。`ConfigAdGroupResolver` 仅允许用于 **dev / local / test**（含 demo / E2E）。验收/生产路径必须 **fail-closed**：拒绝以 stub 启动，或要求已实现且已配置的目录适配器 SPI；公司 LDAP/AD 坐标 **UNKNOWN** — **禁止编造**。

| 行为域 | 摘要 |
| --- | --- |
| **ADG-C1 Prod refuse stub** | 验收/生产路径上 `type=config`（或等价 stub）→ **启动期 fail-closed**；不得以「生产目录解析」身份提供服务 |
| **ADG-C2 Unimplemented type** | 验收/生产路径上显式非 `config` 类型，但适配器**未实现**或**未配置完整** → **启动期 fail-closed**（与 S1 同一硬拒绝语义） |
| **ADG-C3 Dev/test config OK** | `dev` / `local` / `test` 允许 `type=config` + `account-groups`；已知账号解析组；未知账号 → 空组 → 授权 deny（既有 fail-closed at auth） |
| **ADG-C4 ADR seam** | 新增 ADR（建议 **ADR-0054**，`authorization-security`）裁定：config resolver = 本地/测试 only；生产需目录适配器 SPI **或** fail-closed；不发明 LDAP 细节 |
| **ADG-C5 Checklist honesty** | #5a：**CONDITIONAL** = stub 静默生产路径已消除 + ADR/guard 到位，但仍缺真实 LDAP/AD 适配器与公司目录实据；**GO** 仅当另有非虚构目录适配器证据（本切片默认达不到） |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 发明真实公司 LDAP/AD 主机名、DN schema、绑定凭据 | **禁止** — 坐标 UNKNOWN，运维日后供给 |
| 实现完整生产 LDAP/AD 客户端 + 集成测（真实目录） | **Out of scope residual** — 本切片只关闭「stub 冒充生产」；residual 记入 #5a |
| ADR-0042 / ADR-0043 / Word 残差 | **Out of scope**（#3b） |
| Paste cleaning ↔ binding（#5b） | **Out of scope** |
| JWT / Kafka 镜像（#9 / #10） | **Out of scope**（已分别 GO / CONDITIONAL） |
| `DGE-audit-governance` 工作树 | **禁止触碰** |
| 宣称 production go-live / 激活 CD-3 | **禁止** |
| 改写 ADR-0010 已接受的缓存/503 语义 | **禁止** — 本切片补「适配器 vs stub」决策，不推翻缓存规则 |
| 改变 permission-matrix 中「凭证 + AD Group」双重授权模型 | **禁止** — 仅锁定解析来源合法性 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **运维 / 平台工程师** | Operator | 为验收/生产栈配置非 stub 解析路径（或接受 fail-closed 直至目录适配器可用）；执行 compose / 队列部署 |
| **开发者** | Developer | 本地 / 自动化测试使用 `type=config` + 文档化 demo 账号映射（`svc-caller` / `e2e-runtime-caller` 等） |
| **系统** | Spring Boot + `AdGroupResolver` SPI + 启动校验（`AdGroupResolverGuard` 或等价） | stub 在验收/生产路径拒绝启动；dev/test 允许 config；授权层未知账号仍 deny |
| **发布评审人** | Launch reviewer | 按 §4 / S4 诚实规则将 checklist **#5a** 标为 **CONDITIONAL**（本切片预期）或 **GO**（仅当有真实目录证据）；不据此签整体 go-live |

---

## 3. Goal

1. 消除「验收/生产路径以 `ConfigAdGroupResolver` + demo YAML map 静默充当企业 AD 解析」的风险。  
2. 验收/生产路径上 `type=config` → **启动 fail-closed**。  
3. 验收/生产路径上声明了未实现/未配齐的非 config 类型 → **启动 fail-closed**。  
4. `dev`/`local`/`test` 保留 config 映射；未知账号空组 → 授权 deny。  
5. 通过新 ADR 明确 stub vs 目录适配器边界；公司 LDAP **不虚构**。  
6. checklist **#5a** 诚实翻为 **CONDITIONAL**（默认）；**不**宣称 go-live；overall 仍 **NO-GO**。

---

## 4. 已确认决策（confirmed）

| ID | 决策 |
| --- | --- |
| **ADG-C1** | **Config = 非生产**：`docgen.ad-group-resolver.type=config`（及 `ConfigAdGroupResolver`）**仅**允许在纯本地/自动化路径：`dev` / `local` / `test`（且不得因验收 compose 混用 `prod,dev` + `APP_ENVIRONMENT=dev` 而绕过生产拒绝 — 镜像 JWT-C3）。 |
| **ADG-C2** | **验收/生产拒绝 stub**：当进程处于验收/生产路径（`prod` profile 激活，**或** `docgen.environment` / `APP_ENVIRONMENT` 不在 `dev`/`local`/`test`）时，若解析器类型为 `config`（或缺省落到 config stub）→ **拒绝启动**（`IllegalStateException` 或等价）。**不得**对外提供「当作生产目录解析」的流量。 |
| **ADG-C3** | **未实现目录类型同样 fail-closed**：验收/生产路径上 `type` 为显式非 `config` 值（例如预留的 `ldap` / `directory` / 其它 SPI 名），但对应适配器 Bean **不存在**、或必填目录配置缺失 → **拒绝启动**（与 ADG-C2 同一启动硬拒绝；**不**退回静默加载 config map）。 |
| **ADG-C4** | **拒绝时机统一为启动期**：本切片选定 **startup refuse**（`AdGroupResolverGuard` / `ProductionSecretGuard` 风格 `InitializingBean` + `ApplicationReadyEvent`），避免「进程已健康对外、却在请求时才发现仍是 stub」。请求期 ADR-0010 的 `503 AD_GROUP_RESOLUTION_FAILED` 仍适用于**已接入的真实目录**瞬时故障 — 本切片不改该语义。 |
| **ADG-C5** | **Dev/test 行为保持**：`dev`/`local`/`test` 下 `type=config` + `account-groups` **允许**；已知 `accessAccount` 返回配置组；缺失/空白账号或未知账号 → 空列表；`isAuthorized` / 运行时授权 **fail-closed deny**（与现网一致）。`application.yml` / `application-test.yml` 中 demo 映射可保留，但须文档标明 **local/test only**。 |
| **ADG-C6** | **不发明 LDAP**：文档、配置示例、测试 **不得**写入未确认的公司 LDAP 主机名、Bind DN、密码或 schema，并标注为「已验收生产目录事实」。占位说明使用「operator supplies company directory when available」。 |
| **ADG-C7** | **新 ADR（建议 ADR-0054）** outline（doc-keeper 落盘；本 BDD 已确认决策，不替代 Accepted ADR 正文）：<br>• Topic：`authorization-security`<br>• Config-file resolver = **local/dev/test only**<br>• Production/acceptance requires a **directory adapter SPI** implementation that is configured **or** the process **fails closed** at startup<br>• Company LDAP/AD coordinates = **UNKNOWN** until operator provides — do not invent<br>• Relates to / does not supersede ADR-0010 cache & `503` rules<br>• Checklist #5a residual: real adapter + evidence when directory available |
| **ADG-C8** | **Checklist #5a 诚实规则**（镜像 Kafka #10）：<br>• **NO-GO** — 验收/生产仍可静默以 `type=config` stub 启动并服务，或无 ADR/guard 边界。<br>• **CONDITIONAL** — stub 静默生产路径已消除；ADR + 启动 guard 到位；运维/开发文档齐全；**但**尚无真实 LDAP/AD 适配器实现与公司目录可审计证据。<br>• **GO** — 除 fail-closed 缝外，另有**非虚构**的目录适配器实现 + 配置/连通证据（运维确认的真实目录、或经批准的集成证明）。仅「guard 拒 stub」**不够** GO。<br>本切片交付预期：**CONDITIONAL**（非 GO）。 |
| **ADG-C9** | **整体 verdict**：无论 #5a 为 CONDITIONAL 或 GO，**禁止**宣称 production go-live；#3b / #5b 等未关时 overall 仍 **NO-GO**。清除 #5a residual alone ≠ go-live。 |
| **ADG-C10** | **可观测证据**：启动拒绝须有明确原因（日志/异常指明 config stub 或 unimplemented/unconfigured directory resolver；**不得**打印目录凭据明文）；dev/test 成功路径解析与 deny 可单测证明。 |
| **ADG-C11** | **LAB ONLY 例外（唯一显式）**：`docgen.ad-group-resolver.allow-config-stub-on-prod-profile=true`（环境简写 `DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB`；compose/绑定 `DOCGEN_AD_GROUP_RESOLVER_ALLOW_CONFIG_STUB_ON_PROD_PROFILE`）允许在 **prod-shaped 本地验收栈**上继续使用 `type=config` stub（E2E / docker acceptance）。**声称生产**部署必须缺省或为 `false`。启用时 guard **MUST WARN** 日志标明「非生产目录解析」。此例外 **不是** 企业 AD/LDAP 解析，**不满足** checklist **#5a GO**。权威决策：[ADR-0054 Decision 7](../adr/authorization-security/0054-ad-group-resolver-production-boundary.md)。 |

### 4.1 上游现状（implementation 输入，非已验收行为）

| 发现 | 证据 |
| --- | --- |
| Config stub 即当前唯一实现 | `ConfigAdGroupResolver` implements `AdGroupResolver`；读 `docgen.ad-group-resolver.account-groups` |
| 默认 type=config | `AdGroupResolverProperties.type` default `"config"` |
| Demo 账号硬编码 | `application.yml`：`svc-caller` / `e2e-runtime-caller` → `RETAIL_API` / `CORP_API` |
| 空/未知账号 | `resolveGroups` → empty → auth deny（fail-closed at authorization） |
| ADR-0010 | 缓存 5min、失败不缓存、无缓存则 `503 AD_GROUP_RESOLUTION_FAILED`；**未**裁定 LDAP vs config |
| Ledger seam 退出标准（历史） | 「Production LDAP/AD adapter + integration tests」 |
| 清单 #5a | **NO-GO** — Production AD/LDAP adapter + tests not evidenced |
| 公司 LDAP/AD | **UNKNOWN** — 不得虚构 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 验收/生产路径以 `type=config`（或默认 stub）启动后端 | Fail-closed（S1） |
| T2 | 验收/生产路径以未实现/未配齐的非 config `type` 启动 | Fail-closed（S2） |
| T3 | `dev`/`local`/`test` 以 `type=config` + account-groups 启动并解析账号 | 允许（S3） |
| T4 | 发布评审人更新 checklist #5a | 按 ADG-C8 选 CONDITIONAL（本切片）或 GO |

---

## 6. Preconditions

- 工作树：`feat/ops-ad-group-stub-close` / `D:/working/DGE-ops-ad-group-stub-close`（base `ba0f1e11…`）。  
- Formal phase：**None**；不触碰 `DGE-audit-governance`。  
- Docker 验收栈仍经 `.\scripts\docker-deploy-queue.ps1` 串行（单宿主）。  
- 本规格只锁定 **AD Group 解析来源合法性**；不改 JWT / Kafka / paste↔binding / Word 残差。  
- 运维**当前会话可不提供**真实 LDAP URL；实现使用 SPI 占位 + fail-closed。

---

## 7. Primary journey（成功 — 本切片范围内）

1. Developer 在 `dev`/`local`/`test` 使用 `type=config` + 文档化 account-groups；已知账号解析成功；未知账号 deny。  
2. Operator / CI 在验收/生产路径**不得**依赖 config stub 启动；guard 拒绝 `type=config`。  
3. 若将来声明 `type=ldap`（或等价）但适配器未落地 → 同样拒绝启动（不静默回落 config）。  
4. doc-keeper 落盘建议 **ADR-0054**；清单 #5a → **CONDITIONAL** + residual「真实目录适配器待公司目录可用」。  
5. Overall checklist 仍 **NO-GO**；**禁止** go-live。

---

## 8. System responses

| 路径 | 系统响应 |
| --- | --- |
| 验收/生产 + `type=config` | 拒绝启动；明确 stub 不允许作为生产解析 |
| 验收/生产 + 非 config 但未实现/未配齐 | 拒绝启动；明确 resolver type unimplemented / not configured |
| Dev/local/test + `type=config` + 已知账号 | 启动成功；`resolveGroups` 返回配置组 |
| Dev/local/test + 未知/空账号 | 空组；授权 deny（非 503，除非未来真实目录瞬时失败路径） |
| 清单 / 文档 | #5a CONDITIONAL；不虚构 LDAP；不宣称 go-live |

---

## 9. Acceptance scenarios（Given / When / Then）

### S1 — 验收/生产路径 `type=config`：启动 fail-closed（默认）；LAB 例外见边界

**BDD-OPS-AD-GROUP-STUB-001 / S1**

```
Given 进程处于验收/生产路径
  （prod Spring profile 激活，或 docgen.environment / APP_ENVIRONMENT 不在 dev/local/test）
  And docgen.ad-group-resolver.type 为 config（或解析结果落到 ConfigAdGroupResolver stub）
  And allow-config-stub-on-prod-profile 缺省或为 false（无 LAB 例外）
When 后端执行 AdGroupResolverGuard（或等价启动校验）
Then 启动被拒绝（IllegalStateException 或等价 fail-closed）
  And 错误信息表明拒绝以 config-file AD Group stub 作为生产/验收目录解析
  And 进程不得以健康状态对外提供「生产 AD 解析」流量
  And 日志/错误中不包含目录凭据明文
```

**S1 LAB 边界（ADG-C11 / ADR-0054 Decision 7）**

```
Given 同上验收/生产路径 + type=config
  And docgen.ad-group-resolver.allow-config-stub-on-prod-profile=true
    （DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB / DOCGEN_AD_GROUP_RESOLVER_ALLOW_CONFIG_STUB_ON_PROD_PROFILE）
When 后端执行 AdGroupResolverGuard（或等价启动校验）
Then 启动不被拒绝（LAB ONLY 例外）
  And 必须发出 WARN：此路径不是生产目录解析
  And 此例外不构成企业 AD/LDAP；不满足 checklist #5a GO
  And residual / 运维文档须显式记录该 LAB 覆盖（compose/runbook）
```

### S2 — 验收/生产路径：未实现或未配齐的非 config 类型 → 启动 fail-closed

**BDD-OPS-AD-GROUP-STUB-001 / S2**

```
Given 进程处于验收/生产路径
  And docgen.ad-group-resolver.type 被显式设为非 config 的值
    （例如预留的 ldap / directory / 其它 SPI 名）
  And 对应目录适配器尚未实现，或必填目录配置缺失
When 后端执行 AdGroupResolverGuard（或等价启动校验）
Then 启动被拒绝（IllegalStateException 或等价 fail-closed）
  And 不得静默回退到 ConfigAdGroupResolver + YAML account-groups 并继续服务
  And 错误可诊断为 unimplemented 或 not-configured directory resolver type
```

### S3 — Dev/local/test：`type=config` 允许；已知解析 / 未知 deny

**BDD-OPS-AD-GROUP-STUB-001 / S3**

```
Given Spring 处于 dev 或 local 或 test 路径（真本地/自动化测试；非验收绕过）
  And docgen.ad-group-resolver.type=config
  And account-groups 配置了至少一个已知账号（如 svc-caller → RETAIL_API）
When 应用启动并调用 AdGroupResolver.resolveGroups
Then 启动不因「使用 config resolver」而被拒绝
  And 已知 accessAccount 返回配置中的组列表
  And 未知或空白 accessAccount 返回空列表
  And 对空组的 API 授权判定为 deny（fail-closed at authorization；与现有行为一致）
```

### S4 — Checklist #5a 诚实规则（CONDITIONAL；overall NO-GO）

**BDD-OPS-AD-GROUP-STUB-001 / S4**

```
Given 本切片已消除验收/生产路径上 config stub 静默充当目录解析的能力
  And 已有 ADR（建议 ADR-0054）+ 启动 guard 证据 + 本 BDD
When 发布评审人更新 launch-readiness-checklist.md 第 #5a 行
Then 若仍无真实 LDAP/AD 适配器与公司目录可审计证据 → verdict 为 CONDITIONAL（不得标 GO）
  And residual 须写明：真实目录适配器待公司目录可用时再交付；坐标 UNKNOWN — 不虚构
  And 无论 CONDITIONAL 或 GO，均不得宣称 overall production go-live
  And overall checklist 在 #3b / #5b 等未关时仍为 NO-GO
  And 若验收/生产仍可静默以 type=config 启动 → 必须保持 #5a NO-GO
```

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 验收 compose `SPRING_PROFILES_ACTIVE=prod,dev` + `APP_ENVIRONMENT=dev` | **不得**因此跳过 ADG-C2（镜像 JWT-C3）；无 LAB 覆盖时 prod-shaped 路径仍拒 stub |
| **LAB ONLY** `allow-config-stub-on-prod-profile=true` / `DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB` | **允许** prod-shaped 本地验收继续用 config stub；**MUST WARN**；**不是**生产目录；**不满足** #5a GO；声称生产必须缺省/`false`（ADG-C11） |
| 仅改文档/注释、不加强制启动 guard | **不满足** S1 / S2 — **禁止** |
| 生产允许 type=config「只要 map 非空」 | **不满足** ADG-C2 — 非空 demo map **不是**目录；亦不得用无文档的隐式绕过替代 ADG-C11 |
| 请求期才发现仍是 stub、但 `/healthz` 已 UP | **不满足** ADG-C4 — 必须启动期拒绝（LAB 例外除外，仍须启动期 WARN） |
| 真实目录瞬时故障（未来适配器） | 仍按 ADR-0010：`503 AD_GROUP_RESOLUTION_FAILED` + 缓存规则；本切片不改 |
| 授权配置变更清缓存 | 保持 ADR-0010 / permission-matrix；本切片不改 |
| 公司 LDAP 未知 | 占位 + CONDITIONAL；**禁止**假主机名 |
| `application.yml` 保留 demo account-groups | 允许，但仅在 dev/test 路径生效；验收/生产不得依赖其启动，除非显式 LAB 覆盖（ADG-C11） |

---

## 11. Observable evidence

| 证据 | 用途 |
| --- | --- |
| `AdGroupResolverGuard`（或等价）+ 单元测试：prod 路径拒 `type=config`；拒 unimplemented type；dev/test 放行 | TDD Red→Green / S1–S3 |
| 建议 ADR-0054 Accepted（doc-keeper） | ADG-C7 / 追溯 |
| checklist #5a → **CONDITIONAL** + residual 文案（实现后 doc-sync） | S4 / ADG-C8 |
| 可选：验收路径误配 stub 时启动失败日志摘录（无凭据明文） | S1 |
| `/healthz` 仅在允许路径（dev/test 或未来合法目录配置）可达 | 对照 S1 反向证据 |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) **#5a** | 阻塞项；本 BDD 为其验收规格与诚实 verdict 规则 |
| [execution-sync-ledger.md](../plan/execution-sync-ledger.md) seams「AD Group resolution」 | 历史退出标准：Production LDAP/AD adapter + tests；本切片将「stub 冒充生产」改为 fail-closed 缝，residual 仍指向真实适配器 |
| [ADR-0010](../adr/authorization-security/0010-ad-group-authorization-resolution.md) | 缓存 / 503 / fail-closed **请求期**语义；本切片不推翻；补适配器 vs stub |
| 建议 **ADR-0054**（authorization-security） | config = local only；生产 SPI 或 fail-closed；LDAP UNKNOWN |
| [permission-matrix.md](../security/permission-matrix.md) | API 凭证 + AD Group 双重授权；解析失败与 deny 规则 |
| [ops-jwt-secret-no-default.md](./ops-jwt-secret-no-default.md) / [ops-kafka-company-registry.md](./ops-kafka-company-registry.md) | 同族 ops fail-closed + 诚实 CONDITIONAL/GO 模式 |
| E05-T06 / P6 | 历史实现上下文（`ConfigAdGroupResolver` thin slice） |

---

## 13. TDD Red 提示（交给 backend-engineer）

1. **Guard 单测（优先）**：验收/生产路径 + `type=config` → 抛 `IllegalStateException`（S1）。  
2. 验收/生产路径 + `type=<unimplemented>`（无 Bean / 缺配置）→ 抛错，且**不**加载 config map 继续（S2）。  
3. `dev`/`local`/`test` + `type=config`：不抛；已知账号组断言；未知账号 empty（S3）— 可复用/扩展现有 resolver 测。  
4. 锁定「`prod` profile 激活时即使 `APP_ENVIRONMENT=dev` 仍拒 stub」，防止验收绕过（对齐 `ProductionSecretGuard` JWT 策略）。  
5. **不要**在测试里写入假公司 LDAP 主机并标为 production GO 证据。

---

## 14. Handoff（plan-orchestrator / doc-keeper / backend-engineer）

| 下游 | 动作 |
| --- | --- |
| **plan-orchestrator** | 分配/激活 Task Master 任务（建议下一 id，formal phase 仍 None）；sole-active = `ops-ad-group-stub-close`；任务分解：ADR-0054 + Guard TDD + yml/docs 标注 + checklist #5a CONDITIONAL；**不**激活 CD-3 / #3b / #5b |
| **doc-keeper** | 起草并接受建议 **ADR-0054**（authorization-security）；更新 ledger seam 行措辞（stub 静默生产 → fail-closed；residual = 真实适配器）；runbook / `.env.example` 注明 config resolver local-only；**实现后**由 post-task-doc-sync 翻 #5a |
| **backend-engineer** | 在工作树内 TDD：`AdGroupResolverGuard`（或等价）满足 S1–S3；保持 ADR-0010 请求期语义；`mvn verify` GREEN；勿发明 LDAP |

---

## Change log

| Date | Change |
| --- | --- |
| 2026-07-12 | Initial BDD authored (`ready`) for slice `ops-ad-group-stub-close` (BDD-OPS-AD-GROUP-STUB-001 S1–S4). |
| 2026-07-12 | ADG-C11 + S1 LAB boundary: authorize unique LAB ONLY override `allow-config-stub-on-prod-profile` / `DOCGEN_AD_GROUP_ALLOW_CONFIG_STUB` (align ADR-0054 Decision 7); does not flip #5a. |
