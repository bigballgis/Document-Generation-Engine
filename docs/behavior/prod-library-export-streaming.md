# BDD 行为规格：PRR-A04 — Library export streaming（有界内存全库导出）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-PRR-A04` |
| **编写日期** | 2026-07-17 |
| **程序 / 队列** | NON-CE PRR Wave A 后续叶（继 [prod-scale-bounded-queries.md](./prod-scale-bounded-queries.md)） |
| **Slice** | `prod-library-export-streaming` — **Done** (MAIN merge `5b705f56` / feature tip `834ca1a6`; worktree **REMOVED**) |
| **Branch** | `feat/prod-library-export-streaming` (merged) |
| **Worktree** | removed |
| **Placement** | ISOLATED → merged to MAIN |
| **Task Master** | **#101** PRR-A04 — Batch Recommendation **solo** **closed** → **Done** |
| **Formal phase** | **None**（可靠性加固叶；不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`member_task_ids: ["101"]`）**closed** |
| **上游行为** | [ce-e03-full-library-export.md](./ce-e03-full-library-export.md)（CE-E03 **Done**；契约 SoT） |
| **Owning docs** | **本文件（本叶行为 SoT）**；E03 对外契约仍以 CE-E03 为准；本叶**收紧**其「内存/流式由实现选择」为**强制有界** |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（API-first，同 E03） |

**完成声明约束：** 关闭全库导出路径上「整库 ZIP / 全部嵌套包字节常驻堆」导致的 OOM 风险；**不**改变 CE-E03 对外 ZIP 语义（manifest + 嵌套 E01 v2 + 去重 `masters/`/`clauses/`）；**不**引入异步 job / Kafka；**不**交付全库导入 / FE / E2E；**不**宣称 go-live；**不**翻转 checklist **#3b**；**不**激活 Wave B/C。

---

## 1. 概述

CE-E03 已交付 `POST /api/management/v1/library/export` → `template-library-export-v1-zip`。现状实现将每个合格模板的嵌套 ZIP、去重母版/条款字节与最终根 ZIP **全部累积为堆上 `byte[]` / `Map`**，再一次性写出响应。在接近 E03 上限（合格候选 ≤ 500）且母版 DOCX 较大时，峰值堆占用 ≈ Σ(嵌套包) + Σ(母版副本) + 根 ZIP，易触发 OOM。

| 行为域 | 摘要 |
| --- | --- |
| **A04-S1 有界组装** | 禁止「全库二进制常驻堆」；用临时文件与/或响应流组装根 ZIP |
| **A04-S2 契约兼容** | 对外路径、状态码、manifest 字段、ZIP 条目语义与 CE-E03 **完全兼容**（无新 format 常量） |
| **A04-S3 清理与失败** | 临时产物在成功/失败/取消后可靠删除；失败仍走 E03 错误 envelope |
| **A04-S4 非目标** | 异步导出 job；改 500 上限；改 E01 单模板导出；FE/E2E；全库导入 |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| 嵌套 ZIP / 母版 / 条款字节进 `Map<String, byte[]>` | `LibraryExportService`：`nestedZips` / `masterBytesByHash` / `clauseJsonByPath` |
| 根 ZIP 整包 `byte[]` 返回 | `assembleZip(…)` → `LibraryExportZipArtifact.content()` |
| Controller 一次性 `write` | `LibraryExportController`：`response.getOutputStream().write(artifact.content())` |
| E03 已允许实现选流式，但未强制有界 | E03-C12：「ZIP 组装内存/流式由实现选择」 |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **GLOBAL_ADMIN / GROUP_ADMIN / TEMPLATE_AUTHOR** | 与 E03 相同导出角色 | 仍一次下载全库 ZIP；感知不到内部流式/临时文件 |
| **平台工程师 / 运维** | API / 脚本消费者 | 大库导出不再因堆 OOM 失败；契约不变 |
| **系统** | `LibraryExportService` + controller | 有界组装；fail-closed 授权；可靠清理临时文件 |
| **（非本片）管理 UI 用户** | — | 本片无 UI |

权限：继续复用矩阵 §5「导出模板」；**无新权限码**（同 E03-C13）。

---

## 3. Goal

1. 全库导出在达到 E03 护栏（≤500 合格候选）时，**峰值堆占用有界**：不得同时持有「全部嵌套包字节 + 完整根 ZIP 字节」。
2. 对外仍返回 CE-E03 定义的 `application/zip` 附件，ZIP 内路径与 manifest 语义不变。
3. 授权、空集/超限 422、部分成功、审计 `LIBRARY_EXPORT` 行为与 E03 一致。
4. 临时文件（若使用）在请求结束后不残留；失败路径同样清理。
5. API-first；无 FE / Playwright 义务。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **A04-C1** | **对外契约冻结（兼容 E03）：** `POST /api/management/v1/library/export`；成功 **200** + `Content-Type: application/zip` + `Content-Disposition: attachment`；根 manifest `format=template-library-export-v1-zip`；ZIP 固定路径与条目分类（INCLUDED/SKIPPED/FAILED/FORBIDDEN_OMITTED）、空集/超限 messageKey、部分成功规则 **全部沿用** [ce-e03-full-library-export.md](./ce-e03-full-library-export.md) E03-C1…C20。本叶**不**引入新 format、新路由、新权限码、新错误码族（除非实现暴露既有 I/O 故障，见 A04-C8）。 | handoff + E03 |
| **A04-C2** | **强制有界内存：** 生产路径**禁止**将全部 `templates/*.zip` 字节与完整根 ZIP 同时常驻堆（禁止现状式 `Map<String,byte[]> nestedZips` + `byte[]` 整包返回作为成功路径）。允许的工作集：当前正在处理的**单个**模板导出产物（或等价小缓冲）+ 目录元数据（hash/path/`sourceTemplateIds` 等）+ ZIP 压缩流缓冲。 | handoff OOM |
| **A04-C3** | **组装策略（实现二选一或组合，测试只验有界）：** (a) **临时文件**：在系统临时目录（或配置的工作目录）写入完整根 ZIP，再流式写出响应；(b) **响应流**：`ZipOutputStream`（或等价）直接写入 `HttpServletResponse` 输出流，边组装边刷出。允许 (a)+(b)（先落盘再 `Files.copy` / transfer）。**禁止**「先全部进 `byte[]` 再写响应」作为成功路径。 | handoff |
| **A04-C4** | **嵌套包生命周期：** 对每个 INCLUDED 模板，复用 E01 v2 组装后，**写入根 ZIP（或临时根 ZIP）后立即丢弃**该模板的整包字节引用（允许 GC）。不得把所有嵌套包保留到最终 `assembleZip`。母版/条款去重：首次出现写入条目后，后续同 hash/键仅追加 catalog 元数据，**不得**再保留第二份完整二进制于堆 Map。 | A04-C2 |
| **A04-C5** | **目录元数据：** `masters`/`clauses`/`assetKeyManifest`/`templates[]` 的**非二进制**结构可继续用有界集合（≤500 模板量级）；条款 JSON / 母版 DOCX 的物化写入走流或临时文件，不走「全量 `Map<path,byte[]>` 攒齐再打包」。 | E03 catalogs |
| **A04-C6** | **HTTP 传输形态：** 客户端仍收到单一 ZIP 附件。允许 chunked transfer（无 `Content-Length`）或在临时文件完成后设置 `Content-Length`。**禁止**改 JSON envelope 成功体；**禁止**改为 multipart 或多文件下载 API。 | E03-C4 |
| **A04-C7** | **临时文件清理：** 凡创建临时文件/目录，必须在请求结束（成功写出、空集/超限早退、组装失败、响应写出失败）后删除；推荐 try/finally 或等价。进程崩溃残留可接受操作系统临时目录生命周期，但正常路径零残留。临时文件名不得含 secret / 凭证明文。 | ops hygiene |
| **A04-C8** | **失败语义：** 授权/空集/超限/校验错误在**开始写出 ZIP body 之前**失败 → 与 E03 相同（403 / 422 / 400 + error envelope）。若已开始流式写出 ZIP 后发生不可恢复 I/O，允许截断连接（客户端得到不完整 ZIP）；服务端记 error 日志 + 清理临时文件；**不**要求在半截 ZIP 后再发 JSON envelope。审计：成功（含部分成功 200）仍写 `LIBRARY_EXPORT`（同 E03-C14）；早退 422/403 失败审计策略不变。 | 流式现实 |
| **A04-C9** | **同步边界不变：** 仍为同步请求内完成；**不**引入异步 job / Kafka 导出任务；500 上限（E03-C12）**不**因本叶放宽或收紧。 | E03-C12 |
| **A04-C10** | **E01 / 单模板导出：** `GET …/templates/{id}/export` 契约与行为本叶不改。嵌套包语义仍须满足 E03-C3/C20（独立通过 E01 v2 结构校验）。 | E03 |
| **A04-C11** | **FE / E2E / UIUX：out of scope。** Done 主证据 = 后端测试（契约回归 + 有界组装断言）+ `mvn verify`。 | handoff |
| **A04-C12** | **明确非目标：** 全库导入；嵌入资产二进制；放宽 500；异步导出；管理端批量导出 UI；改 E01；Wave B/C；go-live；CD-3；checklist #3b；CE-G05 / CE-O02。 | handoff OOS |
| **A04-C13** | **可测性（有界）：** 自动化须证明成功路径**不再**经「整包 `byte[]` artifact 返回」或等价全量堆缓冲 API 作为唯一出口；并覆盖「多模板 INCLUDED」时逐模板写入后丢弃（可用测试替身/计数器/禁止全量 Map 的结构断言，或集成测试在可控体量下验证 ZIP 完整性）。不要求在 CI 真触发 JVM OOM。 | TDD |
| **A04-C14** | **Manifest 写入顺序：** 实现可先写嵌套包与 catalogs、最后写 `library-export-manifest.json`，或先写占位再回填——**对外 ZIP 内最终条目集合与 JSON 内容须与 E03 一致**。若采用「流式直接响应且 manifest 需最终 counts」，允许：先落临时根 ZIP（推荐），或两阶段临时文件；不得因此改变客户端可见的最终 ZIP 语义。 | 兼容 |
| **A04-C15** | **事务：** 导出仍为只读组装；不得因流式改为跨请求长事务持有 DB 连接至整包写完（实现应避免 `@Transactional` 覆盖整个流式写出；候选解析与逐模板导出的事务边界由实现钉死，测试不依赖具体注解，只要求不因本叶引入无界堆）。 | prod hygiene |

---

## 5. 前置条件

- CE-E03 已合并：`POST …/library/export` 与 `template-library-export-v1-zip` 可用。
- CE-E01 v2 单模板 ZIP 组装可用。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- 授权用户 `POST /api/management/v1/library/export`（可选 body：`groupId` / `templateIds` / `includeSkipped`）——与 E03 相同。

---

## 7. Primary journey

1. 运维/管理员以授权会话请求全库导出。
2. 系统解析候选集、授权与导出资格（同 E03）；空集/超限早退。
3. 系统打开临时根 ZIP 或响应 `ZipOutputStream`。
4. 逐模板：组装 E01 v2 → 写入 `templates/{id}.zip` → 丢弃该包字节；按需写入去重 `masters/` / `clauses/`。
5. 写入根 `library-export-manifest.json`；完成 ZIP；流式返回客户端。
6. 清理临时文件；写 `LIBRARY_EXPORT` 审计。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| ≥1 模板 INCLUDED | `200` `application/zip`；ZIP 语义同 E03；审计 |
| 合格候选为空 | `422` `api.error.library.exportEmpty`（无 ZIP body） |
| 超过 500 上限 | `422` `api.error.library.exportLimitExceeded` |
| 无导出权 | `403` |
| 单模板钉扎母版缺失 | 条目 `FAILED`；其它继续；若仍有 INCLUDED → 200 |
| 流式写出中途 I/O 失败 | 连接可截断；服务端清理临时文件；记错误日志 |
| Secrets | ZIP/manifest 中不得出现（同 E03） |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-PRR-A04-001 — 契约回归：manifest + 嵌套 v2

**Given** 授权主体范围内有 ≥2 个导出合格模板（已 K01 钉扎）  
**When** `POST /api/management/v1/library/export`  
**Then** 响应 `200` `application/zip`  
**And** ZIP 含 `library-export-manifest.json` 且 `format=template-library-export-v1-zip`  
**And** 每个 INCLUDED 存在 `templates/{templateId}.zip`（E01 v2 结构）  
**And** `counts.includedCount` 与条目一致  
**And** 存在 `LIBRARY_EXPORT` 审计  

（对齐 BDD-CE-E03-001 最小子集；证明流式重构未破坏契约。）

### BDD-PRR-A04-002 — 母版/条款去重仍成立

**Given** 两模板共享同一 `masterFileHash` 与同一条款键  
**When** 全库导出  
**Then** `masters/` 与 `clauses/` 各仅一份去重文件  
**And** catalog `sourceTemplateIds` 含两模板  

（对齐 BDD-CE-E03-002。）

### BDD-PRR-A04-003 — 禁止全量堆缓冲成功路径

**Given** 可观测的导出服务协作点（测试替身 / 包可见装配钩子 / 禁止返回整包 `byte[]` 的 API 形状）  
**When** 多模板（≥2 INCLUDED）全库导出成功  
**Then** 成功路径**不**经「累积全部嵌套 `byte[]` 后再 `assembleZip` 返回整包 `byte[]`」的旧形态  
**And** 每个嵌套包在写入根 ZIP 后不再被全量 Map 持有至请求结束  

### BDD-PRR-A04-004 — 临时文件正常路径清理

**Given** 实现使用临时文件策略（若纯响应流且无临时文件，本场景 **N/A-skip** 并在测试中声明）  
**When** 导出成功完成  
**Then** 该请求创建的临时 ZIP/工作文件已被删除（或标记删除且不可再读）

### BDD-PRR-A04-005 — 早退失败不留临时垃圾

**Given** 导出将因空合格集返回 422（或超限 422）  
**When** 请求完成  
**Then** 响应为统一 error envelope（无 ZIP body）  
**And** 若曾创建临时文件则已清理

### BDD-PRR-A04-006 — 部分成功与 E03 一致

**Given** 模板 A 钉扎母版缺失，模板 B 正常  
**When** 全库导出  
**Then** A=`FAILED`，B=`INCLUDED`，HTTP 200  
**And** ZIP 含 B 的嵌套包  

（对齐 BDD-CE-E03-010。）

### BDD-PRR-A04-007 — 空集 / 超限 / 403 不变

**Given** 分别构造：无合格模板；`templateIds`>500；`TEMPLATE_TESTER`  
**When** 调用全库导出  
**Then** 分别为 `422` `api.error.library.exportEmpty`；`422` `api.error.library.exportLimitExceeded`；`403`  
**And** 均无成功 ZIP body

### BDD-PRR-A04-008 — 嵌套包与 E01 单导出等价

**Given** 合格模板 T  
**When** 全库导出得到 `templates/{T}.zip`，并与单模板 `bundleVersion=2&format=zip` 比较  
**Then** bundle 关键字段与 `artifacts/master.docx` SHA-256 一致  

（对齐 BDD-CE-E03-014。）

### BDD-PRR-A04-009 — FE 非目标

**Given** 本切片范围  
**When** 验收 Done 标准  
**Then** 不要求管理端 UI 或 Playwright E2E/UIUX  
**And** 不引入异步导出 job、不放宽 500、不交付全库导入

### BDD-PRR-A04-010 — 导出仍无 secrets

**Given** 模板关联 API 策略与凭证  
**When** 全库导出  
**Then** 根 manifest、嵌套 bundle、条款 JSON 均无 secret / 凭证材料 / 测试数据变量值  

（对齐 BDD-CE-E03-004。）

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 单模板 E01 仍返回 `byte[]` | 允许；本叶约束在**全库聚合层** |
| 纯流式无 `Content-Length` | 允许 |
| 流式中途失败 | 截断连接 + 清理；不伪造成功 envelope |
| 并发导出 | 允许；各有独立 `exportBatchId` 与独立临时文件 |
| 磁盘满 | 组装失败；清理尽力；不返回残缺成功声明 |
| `includeSkipped` / groupId / templateIds | 同 E03 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| ZIP 条目 + manifest | 与 E03 一致 |
| 代码/测试形态 | 无全量 `nestedZips`+整包 `byte[]` 成功路径 |
| 临时文件 | 成功/早退后不残留（若使用） |
| 审计 | `LIBRARY_EXPORT` |
| 自动化 | 后端测试覆盖 BDD-PRR-A04-001…010（009 为范围断言）；`mvn verify` |
| FE/E2E | **N/A**（A04-C11） |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| Task Master **#101** | 执行叶 PRR-A04 |
| [ce-e03-full-library-export.md](./ce-e03-full-library-export.md) | 对外契约 SoT（本叶兼容收紧内存） |
| [prod-scale-bounded-queries.md](./prod-scale-bounded-queries.md) | 上叶 OOS 指向本叶 |
| [permission-matrix.md](../security/permission-matrix.md) §5 | 导出权限（无新码） |
| [contract-outline.md](../api/contract-outline.md) / OpenAPI | 路由与 schema 不变；传输可为流式 |
| [requirements-plan.md](../requirements/requirements-plan.md) 环境迁移 | 沿用 E03；本叶为可靠性加固 |

---

## 13. TDD Red 映射（建议）

| 层 | 建议失败测试 |
| --- | --- |
| Contract regression | `libraryExport_streaming_preservesManifestAndNestedV2`；`libraryExport_streaming_dedupesMastersAndClauses` |
| Memory shape | `libraryExport_doesNotRetainAllNestedZipsOnHeap`；`libraryExport_artifactNotFullByteArrayAssemble`（或控制器/服务出口改为 Stream/Path） |
| Cleanup | `libraryExport_tempFileDeletedOnSuccess`；`libraryExport_tempFileDeletedOnEmpty422` |
| Fail-closed | `libraryExport_empty_422`；`libraryExport_limitExceeded_422`；`libraryExport_forbiddenForTester` |
| Partial | `libraryExport_onePinnedMasterMissing_othersIncluded` |
| Parity | `nestedZip_matchesSingleTemplateE01Export` |
| Frontend / E2E | **skip**（A04-C11） |

---

## 14. Handoff

```
bdd_readiness: ready
task_ids: ["101"]
slice: prod-library-export-streaming
prr_id: PRR-A04
behavior_doc: docs/behavior/prod-library-export-streaming.md
scenario_ids:
  - BDD-PRR-A04-001
  - BDD-PRR-A04-002
  - BDD-PRR-A04-003
  - BDD-PRR-A04-004
  - BDD-PRR-A04-005
  - BDD-PRR-A04-006
  - BDD-PRR-A04-007
  - BDD-PRR-A04-008
  - BDD-PRR-A04-009
  - BDD-PRR-A04-010
frontend_ui_in_scope: false
e2e_required: false
uiux_required: false
api_first: true
preserves_contract: docs/behavior/ce-e03-full-library-export.md
depends_on: ["80"]  # CE-E03 Done
out_of_scope: ["async-export-job", "raise-500-limit", "library-import", "FE", "E2E", "Wave-B", "Wave-C", "go-live", "CD-3", "checklist-3b"]
next: plan-orchestrator → backend-engineer (TDD); FE/E2E stages N/A
formal_phase: None
open_questions: []
```

**Open questions:** 无。对外契约以 E03 为准；本叶仅强制有界内存（临时文件与/或流式响应），实现细节在 A04-C3 内自由，测试按 A04-C13 锁定可观测有界性。
