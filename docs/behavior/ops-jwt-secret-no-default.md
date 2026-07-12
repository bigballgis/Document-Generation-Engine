# BDD 行为规格：JWT_SECRET 显式供给 — 无 compose 默认回落

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-12  
**BDD ID**: `BDD-OPS-JWT-SECRET-001`  
**来源**: LR-E2 checklist item **#9** · LR-B6 security review **🟡#4** · SOR-S01 精神延续  
**程序 / 清单**: [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) · [launch-readiness-program.md](../plan/launch-readiness-program.md)  
**Task / slice**: `ops-jwt-secret-no-default`  
**Worktree**: `D:/working/DGE-ops-jwt-secret-no-default` · `feat/ops-jwt-secret-no-default`  
**授权依据**: 本会话用户明确确认的目标与 Required behavior（2026-07-12）  
**完成声明约束**: 本切片**仅**清除 checklist **#9** 阻塞；**禁止**据此宣称 production go-live。整体清单在其它 NO-GO 项未关闭前仍为 **NO-GO**。

---

## 1. 概述

验收 / 生产形态路径上的 `JWT_SECRET` 必须由运维**显式供给**（环境变量 / `.env` / Secret Manager / 集群 Secret）。仓库内的 **prod/acceptance compose** 与 **prod-shaped 运维脚本** 不得再烘焙或回落到已知不安全默认值。应用在非 `dev`/`local`/`test` 路径上对已知不安全 JWT 密钥 **fail-closed**（拒绝启动）。

| 行为域 | 摘要 |
| --- | --- |
| **JWT-C1 Compose** | `docker-compose.prod.yml` **禁止** `${JWT_SECRET:-…}` 形式默认；缺省时 compose/启动路径失败或拒绝带默认值起服务 |
| **JWT-C2 App guard** | `ProductionSecretGuard`（或等价）在验收/生产路径拒绝**已知不安全 JWT 密钥集合**（见 §4） |
| **JWT-C3 Scripts** | prod-shaped 脚本（如 `container-hardening-smoke.ps1`）不得静默回落到已知不安全默认 |
| **JWT-C4 Dev/test** | `dev` / `local` / `test` 仍可使用文档化本地/测试密钥（含 `application.yml` 本地默认，若仅在该路径生效） |
| **JWT-C5 Checklist** | 实现 + 证据后可将 checklist **#9** 翻为 **GO**；**不得**整体 go-live |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| Kafka 公司镜像 registry | **Out of scope**（checklist #10） |
| ADR-0042 / ADR-0043 / Word 残差 | **Out of scope**（#3b） |
| AD Group stub / paste↔binding | **Out of scope**（#5a / #5b） |
| `DGE-audit-governance` 工作树 | **禁止触碰** |
| 宣称 production go-live / 激活 CD-3 | **禁止** |
| 旋转/自动轮换 JWT（runbook 目标态） | **Out of scope** — 仅锁定「显式供给 + fail-closed」 |
| 顺带清除 Postgres/MinIO compose 其它默认口令 | **Out of scope** — 本切片仅 `JWT_SECRET`（除非实现时 guard 已覆盖的既有默认检测保持不回退） |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **运维 / 平台工程师** | Operator | 为验收/生产栈显式设置 `JWT_SECRET`；执行 compose / 队列部署 / 加固冒烟 |
| **开发者** | Developer | 本地 `dev`/`local`/`test` 使用文档化测试密钥 |
| **系统** | Compose + Spring Boot + `ProductionSecretGuard` | 缺密钥或已知不安全密钥时 fail-closed；合法密钥时允许启动 |
| **发布评审人** | Launch reviewer | 仅在 #9 证据齐全时将该行标 **GO**；不据此签整体 go-live |

---

## 3. Goal

1. 验收/生产 compose **无** `JWT_SECRET` 烘焙默认回落。  
2. 显式供给**非**已知不安全密钥时，验收/生产路径可正常启动（其它依赖健康前提下）。  
3. `JWT_SECRET` 缺失，或等于已知不安全默认时，验收/生产路径 **fail-closed**（不健康 / 拒绝启动 / 脚本拒绝）。  
4. `dev`/`local`/`test` 仍允许文档化测试密钥。  
5. 清除 LR-E2 checklist **#9** 阻塞；**不**宣称 go-live。

---

## 4. 已确认决策（confirmed）

| ID | 决策 |
| --- | --- |
| **JWT-C1** | **Compose**：`docker-compose.prod.yml` 中 `JWT_SECRET` 必须要求显式环境供给（例如 `${JWT_SECRET}` 无 `:-default`）。不得保留 `prod-change-me-32-bytes-minimum-secret` 作为 compose 默认。 |
| **JWT-C2** | **已知不安全 JWT 密钥集合（至少）**：<br>• `local-dev-only-change-me-please-32bytes-min`<br>• `prod-change-me-32-bytes-minimum-secret`<br>验收/生产路径上，解析后的 `docgen.security.jwt.secret` / `JWT_SECRET` 若等于上述任一值 → **拒绝启动**（`IllegalStateException` 或等价 fail-closed）。 |
| **JWT-C3** | **Guard 作用域**：验收/生产路径上 secret guard **必须生效**。不得仅因 compose 仍写 `SPRING_PROFILES_ACTIVE: prod,dev` 与/或 `APP_ENVIRONMENT: dev` 而绕过对已知不安全 JWT 密钥的拒绝。实现须使「prod-shaped / acceptance」启动在 JWT 维度上 fail-closed（调整环境标签、profiles、和/或独立于 `dev` profile 的 JWT 黑名单检测 — 由实现选择，行为以场景为准）。 |
| **JWT-C4** | **本地默认**：`application.yml` 中 `${JWT_SECRET:local-dev-only-change-me-please-32bytes-min}` **允许保留**，但仅在 `dev`/`local`/`test`（或等价本地路径）可用；不得在验收/生产路径生效为可接受运行值。 |
| **JWT-C5** | **脚本**：`scripts/container-hardening-smoke.ps1`（及其它同类 prod-shaped 入口）不得在未设置时回落到 `prod-change-me-32-bytes-minimum-secret`；须要求显式 `JWT_SECRET` 或 fail-closed 退出并给出可操作错误。 |
| **JWT-C6** | **Helm / K8s**：保持既有「Secret 必填、禁止进 ConfigMap」约定（`deploy/helm` `required` / `k8s-config-secrets.md`）；本切片不改 Helm 契约除非发现与 JWT-C1 冲突的默认值。 |
| **JWT-C7** | **Dev/test 例外**：当 Spring profiles 或 `docgen.environment` / `APP_ENVIRONMENT` 为 `dev` / `local` / `test`（真本地或自动化测试）时，允许文档化测试密钥；自动化测试可继续使用 ≥32 字节的测试专用密钥。 |
| **JWT-C8** | **清单**：实现与证据就绪后，由 doc-sync 将 [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) **#9** 标为 **GO** 并挂证据。**整体 verdict 仍 NO-GO**（其它阻塞项未关）。**禁止** go-live 声明。 |
| **JWT-C9** | **可观测证据**：失败路径须有明确启动失败原因（日志含 default/insecure JWT 拒绝语义，**不得**打印密钥明文）；成功路径 `/healthz` 可达。 |

### 4.1 上游现状（implementation 输入，非已验收行为）

| 发现 | 证据 |
| --- | --- |
| Compose 默认回落 | `docker-compose.prod.yml` ≈L36：`${JWT_SECRET:-prod-change-me-32-bytes-minimum-secret}` |
| 本地 yml 默认 | `application.yml`：`${JWT_SECRET:local-dev-only-change-me-please-32bytes-min}` |
| Guard 黑名单不全 | `ProductionSecretGuard` 仅拒 `local-dev-only-…`，**不**拒 `prod-change-me-…` |
| Guard 可能被绕过 | 同文件 `SPRING_PROFILES_ACTIVE: prod,dev` + `APP_ENVIRONMENT: dev` |
| 脚本回落 | `container-hardening-smoke.ps1` 回落 `prod-change-me-32-bytes-minimum-secret` |
| 清单 #9 | **NO-GO**（LR-B6 🟡#4） |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 运维带显式非默认 `JWT_SECRET` 启动 prod/acceptance compose（或队列部署） | 成功路径 |
| T2 | 未设置 `JWT_SECRET` 启动 prod/acceptance compose | Compose/部署 fail-closed |
| T3 | 设置 `JWT_SECRET` 为已知不安全默认后启动验收/生产路径应用 | App fail-closed |
| T4 | 本地/测试 profile 使用文档化测试密钥启动 | 允许 |
| T5 | 运行 `container-hardening-smoke.ps1`（或等价）且未导出安全 `JWT_SECRET` | 脚本 fail-closed |

---

## 6. Preconditions

- 工作树：`feat/ops-jwt-secret-no-default` / `D:/working/DGE-ops-jwt-secret-no-default`。  
- 运维持有（或可生成）≥32 字节、**不在**已知不安全集合内的密钥。  
- Docker 验收栈仍经 `.\scripts\docker-deploy-queue.ps1` 串行（单宿主）。  
- 其它依赖（Postgres/Redis/MinIO/Kafka）按既有 compose 健康条件；本规格只锁定 JWT 维度。

---

## 7. Primary journey（成功）

1. Operator 在环境 / `.env` / Secret Manager 中设置 `JWT_SECRET=<non-default ≥32 bytes>`。  
2. Operator 启动 prod/acceptance compose（或 queued deploy）。  
3. Compose **不**注入已知不安全默认；后端使用显式密钥。  
4. `ProductionSecretGuard`（生效中）通过校验。  
5. Backend `/healthz` 健康；管理端可登录（既有路径）。  
6. （交付后）清单 #9 证据指向「无 compose 默认 + guard 覆盖已知不安全集合」；整体仍 NO-GO。

---

## 8. System responses

| 路径 | 系统响应 |
| --- | --- |
| 显式合法密钥 | 启动成功；健康检查通过 |
| Compose 缺 `JWT_SECRET` | 不启动带默认密钥的后端；operator 可见缺变量错误 |
| 已知不安全密钥（验收/生产路径） | 进程拒绝启动；错误信息指明 default/insecure secrets（无密钥明文） |
| Dev/local/test + 文档化密钥 | 允许启动 |
| 加固冒烟脚本无显式密钥 | 非零退出；不静默使用 `prod-change-me-…` |

---

## 9. Acceptance scenarios（Given / When / Then）

### S1 — 显式非默认密钥：验收/生产 compose 可启动

**BDD-OPS-JWT-SECRET-001 / S1**

```
Given 运维已将 JWT_SECRET 显式设置为不在已知不安全集合内、且长度 ≥ 32 字节的值
  And 使用 docker-compose.prod.yml（acceptance/prod profile）启动后端所依赖的栈
When 后端进程完成启动
Then 进程不以「default secrets」原因失败
  And GET /healthz 返回成功（在既有健康依赖满足时）
  And 运行中的 JWT_SECRET 不是 local-dev-only-change-me-please-32bytes-min
  And 运行中的 JWT_SECRET 不是 prod-change-me-32-bytes-minimum-secret
```

### S2a — 缺失 JWT_SECRET：compose / 部署 fail-closed

**BDD-OPS-JWT-SECRET-001 / S2a**

```
Given 环境中未设置 JWT_SECRET（且无 .env / Secret 提供该键）
When 运维尝试以 docker-compose.prod.yml 启动 acceptance/prod 后端服务
Then 不得通过 compose 默认语法注入 prod-change-me-32-bytes-minimum-secret
  And 启动路径失败或拒绝创建「带不安全默认 JWT」的运行配置（operator 可诊断为缺 JWT_SECRET）
```

### S2b — 已知不安全默认：应用 fail-closed

**BDD-OPS-JWT-SECRET-001 / S2b**

```
Given 验收/生产路径上 JWT_SECRET（或解析后的 jwt secret）等于已知不安全集合中的任一值
  （包括 local-dev-only-change-me-please-32bytes-min 与 prod-change-me-32-bytes-minimum-secret）
When 后端执行 ProductionSecretGuard（或等价启动校验）
Then 启动被拒绝（IllegalStateException 或等价 fail-closed）
  And 错误信息表明拒绝 default/insecure secrets
  And 错误与日志中不包含密钥明文
```

### S3 — Dev/test 文档化密钥仍允许

**BDD-OPS-JWT-SECRET-001 / S3**

```
Given Spring 处于 dev 或 local 或 test profile（真本地/自动化测试路径）
  And JWT_SECRET 为文档化本地默认或测试专用 ≥32 字节密钥
When 应用执行 secret guard
Then 不因「本地/测试文档化密钥」而拒绝启动
```

### S4 — 加固冒烟脚本无静默不安全回落（boundary）

**BDD-OPS-JWT-SECRET-001 / S4**

```
Given 主机环境未设置 JWT_SECRET
When 执行 scripts/container-hardening-smoke.ps1（prod-shaped 后端冒烟）
Then 脚本不得将 JWT_SECRET 静默设为 prod-change-me-32-bytes-minimum-secret
  And 脚本以非零退出失败，或要求运维先显式导出安全密钥后再继续
```

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 密钥长度 &lt; 32 字节 | 保持既有 JWT 库/配置约束（若已有校验则不回退）；本切片不新开长度 ADR |
| 仅改 compose、不扩 guard 黑名单 | **不满足** S2b（`prod-change-me-…` 仍可能被显式传入）— **禁止**只修一半 |
| 仅扩黑名单、仍留 compose `:-default` | **不满足** S2a / checklist #9 — **禁止** |
| 验收栈继续用 `APP_ENVIRONMENT=dev` 且因此跳过 JWT 拒绝 | **不满足** JWT-C3 / S2b — 必须修复 |
| 空字符串 / 空白 JWT_SECRET | 视为缺失或不安全 → fail-closed（与「必须显式合法密钥」一致） |
| 其它默认口令（DB/MinIO） | 本切片不强制清除 compose 默认；既有 guard 对 DB/MinIO 默认的行为**不得回退** |

---

## 11. Observable evidence

| 证据 | 用途 |
| --- | --- |
| `docker-compose.prod.yml` 无 `JWT_SECRET:-…` 默认 | 静态证明 #9 compose 侧 |
| `ProductionSecretGuard` + 单元测试覆盖两个已知不安全值 + 合法值放行 + dev/test 放行 | TDD Red→Green |
| 可选：未设 `JWT_SECRET` 时 compose/config 解析失败的命令输出摘录 | S2a |
| 加固脚本不再硬编码/回落 `prod-change-me-…` | S4 |
| checklist #9 → **GO** + 证据链接（实现后 doc-sync） | 关闭该阻塞行 |
| `/healthz` 在显式合法密钥下 OK | S1 |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) #9 | 阻塞项；本 BDD 为其验收规格 |
| [runbook.md](../operations/runbook.md) § Required environment variables | 运维 SoT：显式 `JWT_SECRET` |
| [deploy/k8s-config-secrets.md](../../deploy/k8s-config-secrets.md) / Helm | 集群 Secret 必填约定 |
| [execution-sync-ledger.md](../plan/execution-sync-ledger.md) LR-B6 🟡#4 | 历史处置：列为 LR-E2 前置 |
| SOR-S01（[system-optimization-review](../plan/system-optimization-review-2026-07.md)） | 默认密钥 fail-fast 精神；本切片补 compose/`prod-change-me` 缺口 |
| `.env.example` | 本地示例仍可为 local-dev 密钥，须标注 never reuse in real environments |

---

## 13. TDD Red 提示（交给 backend-engineer）

1. 扩展 `ProductionSecretGuardTest`：`prod-change-me-32-bytes-minimum-secret` 在 prod 环境必须抛错。  
2. 回归：合法非默认密钥仍通过；`test`/`dev`/`local` 仍允许本地默认。  
3. 静态/脚本侧：compose 文件断言无 `JWT_SECRET:-`；脚本无该字符串回落（可用轻量测试或评审清单，由实现选择）。  
4. 若调整 `APP_ENVIRONMENT` / profiles：增加「acceptance 路径下已知不安全密钥仍被拒」的锁定测试，防止再次被 `dev` 标签绕过。

---

## Change log

| Date | Change |
| --- | --- |
| 2026-07-12 | Initial BDD authored (`ready`) for slice `ops-jwt-secret-no-default`. |
