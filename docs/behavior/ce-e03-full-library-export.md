# BDD 行为规格：CE-E03 — 全库导出（per-template bundles + manifest）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-E03` |
| **编写日期** | 2026-07-17 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7 Wave CE-E · CE-E03 |
| **Slice** | `ce-e03-full-library-export` |
| **Worktree** | **REMOVED** after merge（was `D:/working/DGE-ce-e03-full-library-export` · `feat/ce-e03-full-library-export`） |
| **Task Master** | **#80** → **Done**（MAIN merge `f1f02554` / feature `86e4ff10`） |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED（交付实现） |
| **上游** | CE-E01 (#78) **Done**（`template-export-bundle-v2-json` 自包含 ZIP：母版 DOCX + 条款快照 + render profile + 资产键清单） |
| **Owning docs** | 本文件（行为 SoT）；计划 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7；需求 [requirements-plan.md](../requirements/requirements-plan.md)「环境迁移」；产品 [PRD.md](../product/PRD.md) §10；领域 [domain-model.md](../domain/domain-model.md) §6；API [contract-outline.md](../api/contract-outline.md) + [openapi-v1.yaml](../api/openapi-v1.yaml)（`exportLibraryTemplates` / `LibraryExportManifestView`）；权限沿用矩阵 §5 导出（无新权限码） |
| **Frontend UI** | **Out of scope（API-first）** — 无新全库导出旅程、无管理端批量导出页、无 Playwright E2E/UIUX 义务（与 CE-E01 一致；计划 §7 未要求管理页，对比 CE-E02） |
| **下游扩展** | **SYS-NORM Wave 7** — [sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md)（**BDD-SYS-NORM-PP-006**）：body `dependencyClosure=PROMOTION` 时嵌套包为晋级包，根级可去重 `assets/{assetKey}`；默认路径仍 keys-only（E03-C10） |

**完成声明约束：** 本切片关闭「只能逐模板导出、无法一次带走授权范围内可导出模板集合 + 依赖目录」缺口的最小闭环（全库 ZIP = 根 manifest + 嵌套 E01 v2 per-template bundle + 去重母版/条款目录）；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-G05 / CE-O02；**不**交付全库导入；**默认不**嵌入资产二进制（Wave 7 promotion profile 另立）；**leave #50 alone**。

---

## 1. 概述

今日仅有单模板导出（含 CE-E01 v2 自包含 ZIP）。跨环境晋级整组/整库时，运维需反复调用单模板 API，且缺少集合级依赖清单（哪些母版 revision、哪些条款版本、哪些资产键被该批次引用）。CE 北星「可迁移」要求：授权主体可一次导出其范围内全部（或筛选后的）合格模板，载体复用 E01 per-template v2 bundle，并以根级 manifest 索引模板条目与去重后的母版/条款批量目录。

| 行为域 | 摘要 |
| --- | --- |
| **E03-S1 全库 ZIP 载体** | 新格式 `template-library-export-v1-zip`：根 `library-export-manifest.json` + `templates/{templateId}.zip`（每个为完整 E01 v2 ZIP）+ 去重批量目录 |
| **E03-S2 选择与授权范围** | 默认导出 actor 授权范围内全部**导出合格**模板；可选 `groupId` / `templateIds` 收窄 |
| **E03-S3 母版/条款批量目录** | 根级 `masters/` 与 `clauses/` 去重物化（便于人工检视/二次分发）；语义与各嵌套 E01 bundle 内嵌内容一致 |
| **E03-S4 兼容与非目标** | 单模板 E01 导出/导入不变；本片**仅导出**；全库导入 / FE / 资产二进制 / G05 / O02 / go-live / CD-3 / #50 均 OOS |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| 仅单模板 `GET …/templates/{id}/export` | OpenAPI `exportTemplateBundle`；E01 Done |
| 集合级 library export 契约已文档化 | OpenAPI `POST …/library/export` + contract-outline CE-E03；实现待 backend-engineer |
| E01 v2 可复用为嵌套单元 | [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md) |
| E02 资产库不改导出嵌二进制 | E02-C16：资产二进制仍非 E03 范围 |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **GLOBAL_ADMIN** | 全局管理员 | 导出全局范围内全部合格模板（或筛选子集） |
| **GROUP_ADMIN** | 分组管理员 | 仅被授权组范围内合格模板 |
| **TEMPLATE_AUTHOR** | 模板编排人员 | 仅自己负责的合格模板 |
| **平台工程师 / 运维** | API / 脚本消费者 | 一次下载全库 ZIP + 读 manifest 做晋级清单 |
| **系统** | Library export 服务 + 审计 | 组装 ZIP；逐模板复用 E01 导出；fail-closed 授权 |
| **（非本片）管理 UI 用户** | 既有单模板导出控件 | 本片不改旅程 |

母版设计人员、测试人员、审批人员、API 调用方、审计管理员**不因角色本身**获得全库导出权限（fail-closed，对齐矩阵 §5「导出模板」）。

---

## 3. Goal

1. 授权主体可请求 **全库导出 ZIP**：内含根 manifest、每个合格模板的 **E01 v2 自包含 ZIP**、以及去重后的母版 DOCX / 条款快照批量目录。
2. 选择范围默认 = actor 授权 ∩ 导出合格生命周期；可用 `groupId` 与/或显式 `templateIds` 收窄；越权 ID **不**出现在成功条目中（见边界）。
3. Manifest 可机读：模板条目状态（INCLUDED / SKIPPED / FAILED）、母版指纹目录、条款目录、聚合资产键清单（仅键）、计数汇总；**永不**含 secret / 凭证 / 测试数据变量明文 / 资产二进制。
4. 单模板 E01 导出/导入契约与行为保持；本片不交付全库导入、不交付管理端 UI。
5. 复用矩阵 §5 导出权限与审计族扩展；无新权限码。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **E03-C1** | **载体格式常量：** `template-library-export-v1-zip`（写在根 manifest `format` 字段）。响应 `Content-Type: application/zip`；`Content-Disposition` attachment。 | 计划「per-template bundle + manifest」 |
| **E03-C2** | **ZIP 固定相对路径：** (1) `library-export-manifest.json`；(2) `templates/{templateId}.zip` — 每个文件字节级等价于对该模板执行 E01 `bundleVersion=2&format=zip` 的产物；(3) `masters/{masterFileHash}.docx` — 去重母版字节（hash = 小写 SHA-256 hex）；(4) `clauses/{moduleCode}__{semanticVersion}.json` — 去重条款快照 JSON（字段对齐 E01 `clauseSnapshots[]` 单条）。禁止可执行条目；未知多余顶层条目在**导入**场景才校验——本片无导入。 | 复用 E01 + 「母版/条款批量导出」 |
| **E03-C3** | **Per-template 单元强制 v2：** 全库导出**只**嵌入 E01 v2 ZIP（含 `template-export-bundle.json` + `artifacts/master.docx`）。不提供「全库内嵌 v1」模式。单模板默认 v1 导出 API **不变**。 | 依赖 E01 Done |
| **E03-C4** | **API：** `POST /api/management/v1/library/export`（JSON body，非 multipart）。Body 字段：可选 `groupId`（UUID）、可选 `templateIds`（UUID 数组，上限见 E03-C12）、可选 `includeSkipped`（boolean，缺省 `true` — manifest 是否列出 SKIPPED）。成功 **200** + ZIP 字节（非 JSON envelope）。错误仍走统一 error envelope。 | 集合导出；避免超长 query |
| **E03-C5** | **候选集解析顺序：** (a) 若 `templateIds` 非空 → 仅这些 ID（再 ∩ 授权 ∩ 存在）；(b) 否则若 `groupId` 非空 → 该组内模板 ∩ 授权；(c) 否则 → actor 授权范围内全部模板。再过滤导出合格生命周期（同 E01-C8：`PENDING_RELEASE` \| `PUBLISHED` \| `STOPPED` \| `DEPRECATED`）。 | 计划「全库」+ 矩阵范围 |
| **E03-C6** | **条目结果分类（manifest `templates[]`）：** `INCLUDED` — 已写入 `templates/{id}.zip`；`SKIPPED` — 存在且授权但生命周期不合格（`reasonCode=EXPORT_NOT_ELIGIBLE`）；`FAILED` — 合格但组装失败（如钉扎母版对象缺失 `PINNED_MASTER_UNAVAILABLE`）；`FORBIDDEN_OMITTED` — 请求了 `templateIds` 中无权限或不存在的 ID：**不**在 manifest 泄露存在性（计数可进 `omittedUnauthorizedOrUnknownCount`，无 ID 列表）。 | fail-closed + 可运维 |
| **E03-C7** | **部分成功：** 只要 `includedCount ≥ 1`，HTTP **200** 返回 ZIP；FAILED/SKIPPED 仅记 manifest。若候选合格集为空且无 INCLUDED → **422** `api.error.library.exportEmpty`（`messageKey=api.error.library.exportEmpty`）。若请求后授权范围内零模板（含全被 FORBIDDEN_OMITTED）→ 同 422。 | 批量导出实用主义 |
| **E03-C8** | **母版批量目录：** 对每个 INCLUDED 模板，取其 E01 `masterPin.masterFileHash` 与内嵌 DOCX；按 hash 去重写入 `masters/{hash}.docx`。Manifest `masterCatalog[]`：`masterFileHash`、`masterRevisionId`（若有）、`revisionSequence`（若有）、`sourceTemplateIds[]`（引用该 hash 的模板）、`path`。与嵌套 ZIP 内 `artifacts/master.docx` **字节一致**（允许重复存储以便嵌套包自包含）。 | 计划「母版…批量导出」 |
| **E03-C9** | **条款批量目录：** 聚合所有 INCLUDED 的 `clauseSnapshots`；键 = `moduleCode` + `semanticVersion`；去重写入 `clauses/{moduleCode}__{semanticVersion}.json`（`__` 分隔；`moduleCode`/`semanticVersion` 中若含不安全路径字符则 percent-encode 或替换为 `_`，manifest 保留原始键）。Manifest `clauseCatalog[]`：`moduleCode`、`semanticVersion`、`sourceModuleId`、`path`、`sourceTemplateIds[]`。 | 计划「条款批量导出」 |
| **E03-C10** | **聚合资产键：** Manifest `assetKeyManifest[]` = 各 INCLUDED bundle 的键并集（键 + 用途枚举）；**不**嵌入资产二进制；**不**打包 CE-E02 对象字节（确认 E02-C16）。 | E01-C7 + E02-C16 |
| **E03-C11** | **根 manifest 必填字段：** `format`、`exportBatchId`（UUID）、`exportedAt`（UTC ISO-8601）、`bundleVersion`（恒 `2` 表示内嵌 per-template 为 v2）、`actor`（非敏感：userId/role 摘要）、`scope`（`ALL_AUTHORIZED` \| `GROUP` \| `TEMPLATE_IDS` + 回显非敏感过滤）、`counts`（`includedCount`/`skippedCount`/`failedCount`/`omittedUnauthorizedOrUnknownCount`/`uniqueMasterCount`/`uniqueClauseCount`/`uniqueAssetKeyCount`）、`templates[]`、`masterCatalog[]`、`clauseCatalog[]`、`assetKeyManifest[]`。 | 可测性 |
| **E03-C12** | **护栏（同步 M 量级）：** 单次 `templateIds` 最多 **500**；解析后尝试导出的合格候选最多 **500**（超出 → **422** `api.error.library.exportLimitExceeded`）。ZIP 组装内存/流式由实现选择，但须在超限时 fail-closed 而非截断静默。本片**不**引入异步 job / Kafka 导出任务。 | P3·M + 单路主机 |
| **E03-C13** | **权限：** 与矩阵 §5「导出模板」完全一致；无新权限码、无新 capability。逐模板授权：仅导出调用方对**该模板**有导出权的条目。越权边界见 E03-C6。 | 矩阵 §5 |
| **E03-C14** | **审计：** 成功（含部分成功 200）→ `LIBRARY_EXPORT`（摘要：`exportBatchId`、counts、scope、actor；**无**条款全文、**无** DOCX 字节、**无**变量值）。空集 422 / 越权 403 / 超限 422 亦写失败或拒绝类审计（实现钉死事件名，至少成功路径强制）。 | 可观测 |
| **E03-C15** | **全库导入：本片 out of scope。** 目标环境继续用 E01 单模板 import / dry-run（可脚本解开 `templates/*.zip`）。不提供 `POST …/library/import`。 | 计划标题「全库导出」；收敛 M |
| **E03-C16** | **FE / E2E / UIUX：本片 out of scope（API-first）。** 与 CE-E01 同模式；计划 §7 CE-E03 未要求管理页（对比 CE-E02）。OpenAPI 类型若需前端机械同步，无用户旅程。Done 主证据 = 后端测试 + `mvn verify` + 契约。 | Task #80；E01-C20 类比 |
| **E03-C17** | **明确非目标：** 全库导入；嵌入资产二进制；CE-G05；CE-O02；改 E01 单模板契约语义；改 runtime 生成；管理端批量导出 UI；go-live；CD-3；正式 P-phase；#50 Vitest 专项。 | handoff OOS |
| **E03-C18** | **英文优先 messageKey（稳定）：** `api.error.library.exportEmpty`、`api.error.library.exportLimitExceeded`；母版失败项复用 `api.error.rendering.pinnedMasterUnavailable`（条目级 `reasonCode`）；实现落入 `messages_en.properties`。 | i18n skill |
| **E03-C19** | **Idempotency：** 本片不要求 `Idempotency-Key`（只读组装导出；每次新 `exportBatchId`）。 | 导出幂等非必须 |
| **E03-C20** | **嵌套包自洽：** 每个 `templates/{id}.zip` 必须独立通过 E01 v2 ZIP 结构校验（含 `artifacts/master.docx` 与 hash 自洽）。根级 `masters/`/`clauses/` 为批量便利副本，**不**替代嵌套包自包含性。 | 复用 E01 |

---

## 5. 前置条件

- CE-E01 已合并：单模板 v2 ZIP 导出可用且绿。
- 矩阵 §5 导出权限与单模板导出审计基线可用。
- 对象存储可读取钉扎母版 DOCX。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- 授权用户 `POST /api/management/v1/library/export`（可选 body：`groupId` / `templateIds` / `includeSkipped`）。

---

## 7. Primary journey

1. 运维/管理员以授权会话请求全库导出（默认可不带过滤）。
2. 系统解析候选模板集 → 按授权与导出资格分类。
3. 对每个 INCLUDED 模板调用（或内联复用）E01 v2 ZIP 组装逻辑。
4. 系统按 hash/条款键去重写入 `masters/`、`clauses/`；写根 manifest；打包 ZIP。
5. 系统写 `LIBRARY_EXPORT` 审计，返回 `application/zip` 附件。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| ≥1 模板 INCLUDED | `200` `application/zip`；manifest counts 一致；审计 |
| 合格候选为空 | `422` `api.error.library.exportEmpty` |
| 超过 500 上限 | `422` `api.error.library.exportLimitExceeded` |
| 调用方无任何导出角色/权 | `403` |
| 单模板钉扎母版缺失 | 该条 `FAILED`；其它继续；若仍有 INCLUDED → 200 |
| Body 字段非法（非 UUID 等） | `400` 统一校验错误 |
| Secrets | ZIP/manifest 中不得出现 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-CE-E03-001 — 全库 ZIP 含 manifest 与嵌套 v2 模板包

**Given** 授权 `GROUP_ADMIN` 且其组内有 ≥2 个 `PUBLISHED` 模板（均已 K01 钉扎）  
**When** `POST /api/management/v1/library/export`（无 body 或空对象）  
**Then** 响应为 ZIP；含 `library-export-manifest.json`  
**And** manifest `format` = `template-library-export-v1-zip`  
**And** 每个 INCLUDED 模板存在 `templates/{templateId}.zip`  
**And** 每个嵌套 ZIP 含 `template-export-bundle.json`（`format=template-export-bundle-v2-json`）与 `artifacts/master.docx`  
**And** `counts.includedCount` ≥ 2  
**And** 存在 `LIBRARY_EXPORT` 审计

### BDD-CE-E03-002 — 母版与条款批量目录去重

**Given** 两模板共享同一 `masterFileHash` 且共享同一 `moduleCode+semanticVersion` 条款快照  
**When** 全库导出  
**Then** `masters/` 下该 hash 仅 **1** 个 DOCX 文件  
**And** `clauses/` 下该条款键仅 **1** 个 JSON 文件  
**And** manifest `masterCatalog` / `clauseCatalog` 的 `sourceTemplateIds` 含这两个模板  
**And** `counts.uniqueMasterCount` / `uniqueClauseCount` 反映去重

### BDD-CE-E03-003 — 聚合资产键无二进制

**Given** INCLUDED 模板的 v2 bundle 含资产键  
**When** 全库导出  
**Then** manifest `assetKeyManifest` 含这些键  
**And** ZIP 内无资产图片/签章二进制对象条目（无 `assets/` 二进制树）

### BDD-CE-E03-004 — 导出不含 secrets

**Given** 模板关联 API 策略与凭证  
**When** 全库导出  
**Then** 根 manifest、嵌套 bundle、条款 JSON 均无 secret / 凭证材料 / 测试数据变量值

### BDD-CE-E03-005 — 生命周期不合格记 SKIPPED

**Given** 授权范围内同时存在 `DRAFT` 与 `PUBLISHED` 模板  
**When** 全库导出且 `includeSkipped=true`  
**Then** `PUBLISHED` → `INCLUDED`  
**And** `DRAFT` → `SKIPPED` / `EXPORT_NOT_ELIGIBLE`  
**And** 无 `templates/{draftId}.zip`

### BDD-CE-E03-006 — templateIds 收窄

**Given** 授权可见模板 A、B、C 均为合格  
**When** body `templateIds=[A,B]`  
**Then** 仅 A、B 为 `INCLUDED`  
**And** C 不出现在 `templates[]`（除非实现选择不列出未请求项——**确认：未请求的合格模板不得 INCLUDED**）

### BDD-CE-E03-007 — 越权 templateId 不泄露

**Given** 调用方对模板 X 无导出权（或 X 不存在）  
**When** `templateIds` 含 X 与一个合法合格模板 Y  
**Then** Y `INCLUDED`；响应 200（若 Y 成功）  
**And** manifest **无** X 的 id  
**And** `omittedUnauthorizedOrUnknownCount ≥ 1`

### BDD-CE-E03-008 — 空结果 422

**Given** 授权范围内无任何导出合格模板（或 `templateIds` 全被省略/不合格）  
**When** 全库导出  
**Then** `422` `api.error.library.exportEmpty`  
**And** 无 ZIP body

### BDD-CE-E03-009 — 超限 422

**Given** `templateIds` 长度 > 500（或合格候选 > 500）  
**When** 全库导出  
**Then** `422` `api.error.library.exportLimitExceeded`

### BDD-CE-E03-010 — 单模板失败不阻断其它

**Given** 模板 A 钉扎母版对象缺失，模板 B 正常  
**When** 全库导出  
**Then** A 为 `FAILED` / `PINNED_MASTER_UNAVAILABLE`（或等价稳定码）  
**And** B `INCLUDED`  
**And** HTTP 200 且 ZIP 含 B 的嵌套包

### BDD-CE-E03-011 — GROUP_ADMIN 组范围

**Given** `GROUP_ADMIN` 仅授权组 G1；G2 有合格模板  
**When** 无过滤全库导出  
**Then** 结果仅含 G1 模板  
**And** 不含 G2

### BDD-CE-E03-012 — TEMPLATE_AUTHOR 仅自己负责的模板

**Given** `TEMPLATE_AUTHOR` 负责模板 A，不负责同组模板 B（B 合格）  
**When** 全库导出  
**Then** A 可 `INCLUDED`；B 不出现为 `INCLUDED`

### BDD-CE-E03-013 — 无导出权角色 403

**Given** 用户为 `TEMPLATE_TESTER`  
**When** `POST …/library/export`  
**Then** `403`

### BDD-CE-E03-014 — 嵌套包与 E01 单导出等价

**Given** 合格模板 T  
**When** 全库导出得到 `templates/{T}.zip`，并与 `GET …/templates/{T}/export?bundleVersion=2&format=zip` 比较  
**Then** 两 ZIP 内 `template-export-bundle.json` 的 `format`/`masterPin.masterFileHash`/`clauseSnapshots` 键集一致  
**And** `artifacts/master.docx` 字节 SHA-256 相同

### BDD-CE-E03-015 — groupId 过滤

**Given** `GLOBAL_ADMIN`；组 G1/G2 均有合格模板  
**When** body `groupId=G1`  
**Then** 仅 G1 模板可 `INCLUDED`

### BDD-CE-E03-016 — 单模板 E01 回归

**Given** E01 基线  
**When** 仍调用单模板 export/import/dry-run  
**Then** 行为与 CE-E01 一致（本片无回归破坏）

### BDD-CE-E03-017 — FE 非目标

**Given** 本切片范围  
**When** 验收 Done 标准  
**Then** 不要求管理端全库导出 UI 或 Playwright E2E/UIUX  
**And** 不实现全库导入、CE-G05、CE-O02、CD-3、go-live、#50

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| `includeSkipped=false` | manifest 可省略 SKIPPED 行，但 `skippedCount` 仍准确 |
| 空 `assetKeyManifest` | 合法 |
| 并发全库导出 | 允许；各有独立 `exportBatchId` |
| 超大单个母版 DOCX | 沿用 E01/上传大小护栏；单模板 FAILED，不截断其它 |
| 路径安全 | `moduleCode`/`templateId` 不得写出 `../` 穿越 |
| 全库导入 | **拒绝本片范围**；客户端可循环 E01 import |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| ZIP 条目列表 | manifest + templates/*.zip + masters/* + clauses/* |
| Manifest JSON | counts / catalogs / 无敏感明文 |
| 嵌套 ZIP | 通过 E01 结构与 hash 自洽断言 |
| 审计 | `LIBRARY_EXPORT` |
| 自动化 | 后端单元/切片测试覆盖 BDD-CE-E03-001…016；`mvn verify` |
| FE/E2E | **N/A**（E03-C16） |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7 CE-E03 | 计划卡 |
| Task Master **#80** | 执行叶 |
| [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md) | per-template v2 单元（必须复用） |
| [ce-e02-asset-library.md](./ce-e02-asset-library.md) E02-C16 | 确认不嵌资产二进制 |
| [requirements-plan.md](../requirements/requirements-plan.md) 环境迁移 | 需求确认扩展 |
| [PRD.md](../product/PRD.md) §10 | 产品确认扩展 |
| [domain-model.md](../domain/domain-model.md) §6 | 领域迁移规则 |
| [contract-outline.md](../api/contract-outline.md) / OpenAPI | 管理 API 契约 |
| [permission-matrix.md](../security/permission-matrix.md) §5 | 导出权限（无新码） |

---

## 13. TDD Red 映射（建议）

| 层 | 建议失败测试 |
| --- | --- |
| Backend happy | `libraryExport_zipContainsManifestAndNestedV2`；`libraryExport_dedupesMastersAndClauses`；`libraryExport_aggregatesAssetKeysWithoutBinaries` |
| Backend scope | `libraryExport_skipsDraft`；`libraryExport_templateIdsFilter`；`libraryExport_groupAdminScoped`；`libraryExport_authorOnlyOwned` |
| Backend fail-closed | `libraryExport_forbiddenForTester`；`libraryExport_omitsUnauthorizedIds`；`libraryExport_empty_422`；`libraryExport_limitExceeded_422` |
| Backend partial | `libraryExport_onePinnedMasterMissing_othersIncluded` |
| Parity | `nestedZip_matchesSingleTemplateE01Export` |
| Contract | OpenAPI `POST /library/export` + manifest schema |
| Frontend / E2E | **skip**（E03-C16） |

---

## 14. Handoff

```
bdd_readiness: ready
task_ids: ["80"]
slice: ce-e03-full-library-export
ce_id: CE-E03
behavior_doc: docs/behavior/ce-e03-full-library-export.md
frontend_ui_in_scope: false
e2e_required: false
uiux_required: false
api_first: true
depends_on: ["78"]  # CE-E01 Done
out_of_scope: ["CE-G05", "CE-O02", "CD-3", "go-live", "#50", "library-import", "asset-binaries", "FE"]
next: backend-engineer (API/TDD); FE/E2E stages N/A
formal_phase: None
open_questions: []
```

**Open questions:** 无（已由 CE §7 计划卡 + E01 模式 + E02-C16 + Task #80 API/bulk 表述裁定；E03-C1…C20）。

---

## 15. Downstream extension — SYS-NORM Wave 7 (library promotion ZIP)

本片默认（E03-C10：聚合资产**仅键**、无根级资产二进制树）保持不变。下游 Wave 7 **加法**：

| 扩展点 | Wave 7 规则 | 对本片默认路径 |
| --- | --- | --- |
| body `dependencyClosure` | `PROMOTION` → 每个 `templates/{id}.zip` 为 E01 晋级包；根级可去重 `assets/{assetKey}` | 省略 → 仍 keys-only |
| 导入 | 全库导入仍 out of scope；目标侧 unwrap 后走单模板 import（含 dry-run） | 不变 |
| UI | 全库导出 UI 仍 OOS；Import dry-run UI 归 Wave 7 单模板 Import 对话框 | 不变 |

权威：[sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md)；契约：[openapi-v1.yaml](../api/openapi-v1.yaml) `LibraryExportRequest.dependencyClosure`。
