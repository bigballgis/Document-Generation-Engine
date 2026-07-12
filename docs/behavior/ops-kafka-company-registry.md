# BDD 行为规格：Kafka 镜像 — 公司批准 registry，禁止生产硬编码 Docker Hub

**文件状态**: `ready`  
**版本**: 1.0.0  
**编写日期**: 2026-07-12  
**BDD ID**: `BDD-OPS-KAFKA-REGISTRY-001`  
**来源**: LR-E2 checklist item **#10** · LR-B4 note（公司 registry，非 Docker Hub `bitnamilegacy`）  
**程序 / 清单**: [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) · [launch-readiness-program.md](../plan/launch-readiness-program.md)  
**Task / slice**: `ops-kafka-company-registry`（Task Master **#45**）  
**Worktree**: `D:/working/DGE-ops-kafka-company-registry` · `feat/ops-kafka-company-registry`  
**授权依据**: 本会话用户明确确认的方向（2026-07-12）— 可配置镜像坐标、未知公司 registry 时用占位 env + fail-closed 文档、禁止虚构私有 registry 主机名作为生产事实  
**完成声明约束**: 本切片**仅**处理 checklist **#10** 的可验证路径与诚实 verdict；**禁止**据此宣称 production go-live。整体清单在其它 NO-GO 项未关闭前仍为 **NO-GO**。

---

## 1. 概述

生产 / 验收形态路径上的 Kafka 容器镜像必须由运维**显式供给**公司批准的镜像坐标（环境变量 / compose override / Secret / 部署配置）。仓库**不得**把 Docker Hub 上的 `bitnamilegacy/kafka:3.7`（或任何未经验证的公共 Hub 标签）当作**唯一**或**宣称的生产**坐标。公司 registry URL **当前未知** — 实现与文档使用 **占位变量名**（如 `KAFKA_IMAGE`）+ fail-closed 运维指引；**禁止**编造假私有 registry 主机名并写成生产事实。

| 行为域 | 摘要 |
| --- | --- |
| **KFK-C1 Compose fail-closed** | 生产 / 验收相关 compose（含 `docker-compose.yml` 中 `docgen-kafka` 被 prod overlay 依赖的路径）**要求**显式 `KAFKA_IMAGE`（或等价变量）；缺失时 compose/config **fail-closed**，不得静默回落到 Hub `bitnamilegacy/…` |
| **KFK-C2 Dev example only** | 本地 / 开发可在 `.env.example` / runbook 中**文档化** `bitnamilegacy/kafka:3.7` **仅作为非生产示例**；不得声称该坐标为生产坐标 |
| **KFK-C3 Operator supply** | 生产运维必须提供公司批准 registry 的完整镜像引用（`registry/…/kafka:tag` 或公司等价形式） |
| **KFK-C4 Checklist honesty** | #10：**CONDITIONAL** = 仓库已消除硬编码 Hub 生产路径、运维可配置，但仍缺公司坐标实据；**GO** = 已有可指向的公司批准坐标证据（文档/配置/拉取证明，且非虚构主机名） |
| **KFK-C5 No invented registry** | 任何文档、compose 默认、示例**不得**把未确认的假私有 registry 主机名写成「生产事实」 |

**明确非目标（本切片不做）**

| 非目标 | 处理 |
| --- | --- |
| 发现 / 谈判真实公司 Kafka registry URL | **Out of scope** — 运维提供；仓库只留占位与 fail-closed |
| 更换异步拓扑（ADR-0044 in-process vs Kafka） | **Out of scope** — 本切片只锁镜像坐标供给 |
| JWT / 其它 compose 默认密钥 | **Out of scope**（#9 已清） |
| `DGE-audit-governance` 工作树 | **禁止触碰** |
| 宣称 production go-live / 激活 CD-3 | **禁止** |
| 强制本地开发禁止拉取 bitnamilegacy | **Out of scope** — 本地示例允许，须标注非生产 |

---

## 2. Actor / Role

| Actor | 角色 | 说明 |
| --- | --- | --- |
| **运维 / 平台工程师** | Operator | 为验收/生产栈显式设置 `KAFKA_IMAGE`（公司批准坐标）；执行 compose / 队列部署 |
| **开发者** | Developer | 本地开发可使用 `.env.example` 中文档化的非生产示例镜像（含历史 Hub 标签） |
| **系统** | Compose / deploy 配置 | 缺 `KAFKA_IMAGE` 时 fail-closed；有合法显式坐标时允许解析/启动（其它依赖健康前提下） |
| **发布评审人** | Launch reviewer | 按 §4 / S4 诚实规则将 checklist **#10** 标为 **CONDITIONAL** 或 **GO**；不据此签整体 go-live |

---

## 3. Goal

1. 去掉「生产/验收路径只能靠硬编码 `bitnamilegacy/kafka:3.7`」的唯一依赖。  
2. 未设置 `KAFKA_IMAGE`（或等价）时，生产/验收相关 compose **fail-closed**。  
3. 运维显式供给公司批准镜像引用后，Kafka 服务可按既有健康检查启动（其它依赖满足时）。  
4. 本地示例可保留 bitnamilegacy，**仅**作非生产文档。  
5. checklist **#10** 按证据诚实翻为 **CONDITIONAL** 或 **GO**；**不**宣称 go-live；**不**虚构公司 registry。

---

## 4. 已确认决策（confirmed）

| ID | 决策 |
| --- | --- |
| **KFK-C1** | **变量名**：以 `KAFKA_IMAGE` 为规范坐标变量（完整镜像引用，含 registry/repo/tag）。若实现选用等价名，须在 runbook / `.env.example` 单点说明并保持 fail-closed 语义一致。 |
| **KFK-C2** | **Compose**：`docgen-kafka.image` 必须来自 `${KAFKA_IMAGE:?…}`（或等价「必填、无 `:-bitnamilegacy…` 默认」）。**禁止**在共享/生产路径上保留 `image: bitnamilegacy/kafka:3.7` 硬编码作为可静默成功的生产坐标。 |
| **KFK-C3** | **Dev 示例**：允许在 `.env.example`（及 runbook「local only」小节）写明示例如 `KAFKA_IMAGE=bitnamilegacy/kafka:3.7`，并**明确标注** never use as claimed production coordinate / 仅本地。 |
| **KFK-C4** | **未知公司 URL**：文档与代码使用占位说明（「set to company-approved registry image」）；**禁止**编造 `registry.example.corp/…` 之类主机名并写成已验收生产事实。 |
| **KFK-C5** | **Prod overlay**：`docker-compose.prod.yml` 依赖 `docgen-kafka` 健康时，该 Kafka 镜像坐标仍须满足 KFK-C2（同一变量 / 同一 fail-closed）。 |
| **KFK-C6** | **Helm**：若 values 中存在硬编码 Hub Kafka 镜像，本切片应改为可配置且无假生产默认；若 Helm 不部署 Kafka broker（仅 bootstrap 地址），则文档注明 broker 镜像责任在外部平台 — **不得**因此在 compose 路径留下 Hub 硬编码生产漏洞。 |
| **KFK-C7** | **Checklist #10 诚实规则**：<br>• **NO-GO** — 仍硬编码 Hub 为唯一生产路径，或无运维可配置入口。<br>• **CONDITIONAL** — 仓库已 fail-closed + 可配置；运维路径与文档齐全；**但**尚无公司批准坐标的实据（URL/拉取证明仍未知）。<br>• **GO** — 除可配置路径外，另有**非虚构**的公司批准坐标证据（运维确认的真实引用、或经批准的环境清单摘录、或成功从公司 registry 拉取的可审计记录）。仅「占位变量名写好了」**不够** GO。 |
| **KFK-C8** | **整体 verdict**：无论 #10 为 CONDITIONAL 或 GO，**禁止**宣称 production go-live；其它阻塞项未关时 overall 仍 **NO-GO**。 |
| **KFK-C9** | **Async 可选性**：ADR-0044 下 v1 可 in-process；本规格在**启用 compose Kafka 服务 / prod depends_on kafka** 的路径上锁定镜像坐标。不强制改变 `ASYNC_TRANSPORT` 默认。 |

### 4.1 上游现状（implementation 输入，非已验收行为）

| 发现 | 证据 |
| --- | --- |
| Hub 硬编码 | `docker-compose.yml` ≈L36：`image: bitnamilegacy/kafka:3.7` + 注释要求生产用公司 registry |
| Prod 依赖 Kafka | `docker-compose.prod.yml` `depends_on: docgen-kafka`（healthy） |
| `.env.example` | 有 `KAFKA_PORT` / `KAFKA_BOOTSTRAP_SERVERS`，**无** `KAFKA_IMAGE` |
| 清单 #10 | **NO-GO** — 生产坐标未在仓内证实 |
| 公司 registry URL | **UNKNOWN** — 不得虚构 |

---

## 5. Trigger

| # | 触发 | 说明 |
| --- | --- | --- |
| T1 | 运维设置 `KAFKA_IMAGE=<company-approved ref>` 后启动含 `docgen-kafka` 的栈 | 成功路径（坐标维度） |
| T2 | 未设置 `KAFKA_IMAGE` 时解析/启动含 Kafka 的 compose | Fail-closed |
| T3 | 开发者按 `.env.example` 使用文档化非生产示例镜像启动本地栈 | 允许（标注非生产） |
| T4 | 发布评审人更新 checklist #10 | 按 KFK-C7 选 CONDITIONAL 或 GO |

---

## 6. Preconditions

- 工作树：`feat/ops-kafka-company-registry` / `D:/working/DGE-ops-kafka-company-registry`。  
- 运维持有（或稍后提供）公司批准的 Kafka 镜像完整引用；**当前会话可不提供真实 URL**。  
- Docker 验收栈仍经 `.\scripts\docker-deploy-queue.ps1` 串行（单宿主）。  
- 本规格只锁定 **Kafka 镜像坐标**；不改 JWT / DB / MinIO 密钥策略。

---

## 7. Primary journey（成功 — 坐标已供给）

1. Operator 在环境 / `.env` / Secret 中设置 `KAFKA_IMAGE=<company-approved full image ref>`。  
2. Operator 启动依赖 `docgen-kafka` 的 compose（含 prod overlay 时）。  
3. Compose 使用显式坐标拉取/运行 Kafka；**不**静默注入 Hub `bitnamilegacy/…`。  
4. Kafka healthcheck 按既有探针通过（镜像兼容既有 entrypoint/health 命令；若不兼容则实现须同步调整探针并文档化 — 属实现细节，行为仍为「健康可观测」）。  
5. （交付后）清单 #10：无公司坐标实据 → **CONDITIONAL**；有实据 → **GO**。整体仍 NO-GO（其它项）。

---

## 8. System responses

| 路径 | 系统响应 |
| --- | --- |
| 显式公司批准（或运维供给）`KAFKA_IMAGE` | Compose 可解析；Kafka 服务可启动（镜像可达且兼容时） |
| 缺 `KAFKA_IMAGE` | Compose config / up **失败**；operator 可诊断为缺变量（类似 JWT `${VAR:?msg}`） |
| 本地 `.env` 填入文档化非生产示例 | 允许本地开发；文档明确非生产 |
| 文档/清单 | 不把未知 registry 写成已证明的生产事实；#10 诚实 CONDITIONAL/GO |

---

## 9. Acceptance scenarios（Given / When / Then）

### S1 — 显式 `KAFKA_IMAGE`：compose 可解析并使用该坐标

**BDD-OPS-KAFKA-REGISTRY-001 / S1**

```
Given 运维已将 KAFKA_IMAGE 显式设置为某一完整镜像引用（公司批准或本地测试用显式值）
  And 该值不是通过 compose :-bitnamilegacy 默认注入的
When 执行 docker compose config（含 docker-compose.yml，及在验收路径上叠加 docker-compose.prod.yml）
Then 解析成功
  And docgen-kafka 服务的 image 等于环境中的 KAFKA_IMAGE 值
  And 解析结果中不出现「未设置变量却回落到 bitnamilegacy/kafka:3.7」的行为
```

### S2 — 缺失 `KAFKA_IMAGE`：fail-closed

**BDD-OPS-KAFKA-REGISTRY-001 / S2**

```
Given 环境中未设置 KAFKA_IMAGE（且无 .env / Secret 提供该键）
When 运维尝试 docker compose config 或 up（包含 docgen-kafka 定义的文件集）
Then 命令失败（非零退出或等价 fail-closed）
  And 错误可诊断为缺少 KAFKA_IMAGE（或文档约定的等价变量）
  And 不得静默以 bitnamilegacy/kafka:3.7 作为生产/共享路径默认镜像启动
```

### S3 — 本地非生产示例仅作文档，不作生产声称

**BDD-OPS-KAFKA-REGISTRY-001 / S3**

```
Given 仓库提供 .env.example 和/或 runbook 本地小节
When 查阅其中关于 Kafka 镜像的说明
Then 若出现 bitnamilegacy/kafka:3.7（或其它公共 Hub 标签），必须标注为 local/non-production example only
  And 不得将该标签表述为已批准的生产坐标
  And 不得编造未确认的私有 registry 主机名并标注为生产事实
```

### S4 — Checklist #10 诚实规则（CONDITIONAL vs GO）

**BDD-OPS-KAFKA-REGISTRY-001 / S4**

```
Given 本切片已消除硬编码 Hub 生产唯一路径，并实现 KAFKA_IMAGE 必填 fail-closed + 运维文档
When 发布评审人更新 launch-readiness-checklist.md 第 #10 行
Then 若仍无公司批准镜像坐标的可审计证据 → verdict 为 CONDITIONAL（不得标 GO）
  And 仅当存在非虚构的公司批准坐标证据（运维确认引用、批准清单、或公司 registry 拉取证明）→ 才可将 #10 标为 GO
  And 无论 CONDITIONAL 或 GO，均不得宣称 overall production go-live
  And 若硬编码 Hub 生产路径仍在 → 必须保持 NO-GO
```

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 公司 registry URL 未知 | 占位变量 + 文档；#10 → **CONDITIONAL**（路径修好后）；**禁止**假主机名 |
| 仅改注释、仍硬编码 `image: bitnamilegacy/…` | **不满足** S2 / KFK-C2 — **禁止**只改注释 |
| 使用 `${KAFKA_IMAGE:-bitnamilegacy/kafka:3.7}` | **不满足** S2 — 静默回落禁止 |
| 本地开发不想每次 export | 通过复制 `.env.example` 到本地 `.env`（gitignore）填入示例值；**不是** compose 内硬编码生产默认 |
| Prod `ASYNC_TRANSPORT=in-process` 仍 depends_on kafka | 既有 LR-B/ADR-0044 拓扑问题不在本切片强制重开；镜像坐标仍须 fail-closed（只要服务定义存在） |
| Helm 不部署 broker | 文档澄清外部供给；compose 路径仍须满足本 BDD |
| 镜像与现有 Bitnami healthcheck 脚本路径不兼容 | 实现须同步探针或选用兼容镜像，并在 runbook 记录；不得为「跑起来」退回未声明的 Hub 硬编码 |

---

## 11. Observable evidence

| 证据 | 用途 |
| --- | --- |
| `docker-compose.yml`：`image: ${KAFKA_IMAGE:?…}`（或等价必填） | 静态证明 S2 / KFK-C2 |
| 未设 `KAFKA_IMAGE` 时 `docker compose config` 失败输出摘录 | S2 |
| 设置后 `docker compose config` 显示 image == 供给值 | S1 |
| `.env.example` + runbook：示例标注 non-production；无虚构生产 registry | S3 |
| checklist #10 → **CONDITIONAL** 或 **GO** + 证据链接（实现后 doc-sync） | S4 / KFK-C7 |
| （GO 额外）公司批准坐标实据链接 | 仅 GO 需要 |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) #10 | 阻塞项；本 BDD 为其验收规格与诚实 verdict 规则 |
| [launch-readiness-program.md](../plan/launch-readiness-program.md) LR-B4 | 历史：Hub 标签迁移 + 生产须公司 registry |
| [execution-sync-ledger.md](../plan/execution-sync-ledger.md) Async batch / LR-B4 | 镜像修复与生产坐标缺口 |
| [runbook.md](../operations/runbook.md) | 运维 SoT：须增加 `KAFKA_IMAGE` 必填说明（实现期 doc-keeper / doc-sync） |
| `.env.example` | 非生产示例坐标 + 警告 |
| ADR-0044 | 异步拓扑；本切片不改分支决策 |
| tech-stack-guardrails / dependency policy | 公司批准制品源精神 |

---

## 13. TDD / 验证 Red 提示（交给 implementer）

本切片以 **ops/compose 行为**为主，轻量验证即可（不必强行 Java 单测）：

1. 静态断言：共享 compose 中 `docgen-kafka` **无**裸 `bitnamilegacy/kafka:3.7` 硬编码（示例仅允许出现在 `.env.example` / 文档）。  
2. 命令级：清空 `KAFKA_IMAGE` → `docker compose -f docker-compose.yml config` **失败**。  
3. 命令级：`KAFKA_IMAGE=test.local/kafka:ci` → config 成功且 image 为该值。  
4. 文档评审：S3 / S4 文案与 checklist 规则。  
5. 勿在测试或文档中写入假公司 registry 并标记为 production GO 证据。

---

## Change log

| Date | Change |
| --- | --- |
| 2026-07-12 | Initial light BDD authored (`ready`) for slice `ops-kafka-company-registry`. |
