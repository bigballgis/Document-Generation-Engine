# BDD 行为规格：CE-E01 — 自包含导出包 v2 + 导入 dry-run

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-E01` |
| **编写日期** | 2026-07-16 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7 Wave CE-E · CE-E01 |
| **Slice** | `ce-e01-export-bundle-v2` |
| **Worktree** | `D:/working/DGE-ce-e01-export-bundle-v2` · `feat/ce-e01-export-bundle-v2` |
| **Task Master** | **#78**（**In Progress** sole-active；**本片不标 Done** 直至实现 + 门禁 + MAIN doc-sync） |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED |
| **上游** | CE-K01 (#57) **Done**（`master_revision_id` + `master_file_hash` + render profile 快照可消费） |
| **Owning docs** | 本文件（行为 SoT）；计划 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7；需求 [requirements-plan.md](../requirements/requirements-plan.md)「环境迁移」；产品 [PRD.md](../product/PRD.md) §10；领域 [domain-model.md](../domain/domain-model.md) §6；API [contract-outline.md](../api/contract-outline.md) + [openapi-v1.yaml](../api/openapi-v1.yaml)；权限沿用矩阵 §5 导出/导入（无新权限码） |
| **Frontend UI** | **Out of scope（API-first）** — 无新导出/导入旅程、无 dry-run 报告页、无 Playwright E2E/UIUX 义务；既有 P14-T03 UI 继续走 v1 JSON 路径直至后续切片 |

**完成声明约束：** 本切片关闭「导出包不自包含 / 导入后半残」缺口的最小闭环（v2 ZIP 自包含载体 + 依赖预检 dry-run + 提交导入事务化）；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-E02 资产库管理面、CE-E03 全库导出、CE-O01 PDF/A；**不**交付管理端 dry-run UI。

---

## 1. 概述

今日 `template-export-bundle-v1-json` 仅携带元数据、变量、绑定、规则、内容模块**引用**与策略快照；跨环境晋级时目标侧常缺母版 revision 字节、条款正文或资产键，导入可能落成半残草稿。CE 北星「可迁移」要求：模板携带关键依赖证据；导入前 dry-run 给出依赖预检报告；真正提交时**全有或全无**，禁止半导入状态。

| 行为域 | 摘要 |
| --- | --- |
| **E01-S1 导出包 v2** | 新格式 `template-export-bundle-v2-json`；ZIP 自包含载体内嵌钉扎母版 DOCX + JSON 清单（指纹、条款正文快照、render profile、资产键清单） |
| **E01-S2 导入 dry-run** | `dryRun=true` 只返回依赖预检报告，**零**持久化变更 |
| **E01-S3 提交导入** | 校验通过后单事务落地 DRAFT；失败回滚；不得出现半残模板/半残条款 |
| **E01-S4 兼容** | v1 导出/导入行为保持；v2 为显式 `bundleVersion=2`（或等价）能力 |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| v1 bundle 无母版字节 / 无条款正文 | OpenAPI `TemplateExportBundleView`；`contract-outline` Bundle schema |
| ZIP 仅含 `template-export-bundle.json` | `GET …/export?format=zip` |
| 导入无 dry-run | `POST …/templates/import` → 直接 `201` DRAFT |
| K01 钉扎可消费 | `template_version.master_revision_id` + `master_file_hash` + `renderProfileJson` |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **GLOBAL_ADMIN** | 全局管理员 | 任意组模板导出 v2 / dry-run / 提交导入 |
| **GROUP_ADMIN** | 分组管理员 | 被授权组范围内同上 |
| **TEMPLATE_AUTHOR** | 模板编排人员 | 自己负责的模板导出；导入须满足组访问（与 P14 / 矩阵 §5 一致） |
| **平台工程师 / 运维** | API / 脚本消费者 | 跨环境 ZIP 晋级；先 dry-run 再 commit |
| **系统** | Export/Import 服务 + 审计 | 组装 v2；预检；事务提交；fail-closed 授权 |
| **（非本片）管理 UI 用户** | 既有 P14 导出/导入控件 | 本片不改旅程；可继续使用 v1 |

母版设计人员、测试人员、审批人员、API 调用方**不因角色本身**获得导出/导入/dry-run 权限（fail-closed，对齐矩阵 §5）。

---

## 3. Goal

1. 授权主体可将合格模板导出为 **v2 自包含 ZIP**：内嵌钉扎母版 DOCX 字节 + v2 JSON（母版 revision 指纹、条款正文快照、render profile 快照、资产键清单），且**永不**包含 secret / API 凭证 / 运行时凭证 / 测试数据变量明文。
2. 授权主体可对 v1 或 v2 bundle 执行 **import dry-run**，获得结构化依赖预检报告（缺失/不匹配项列表），且目标库**无任何**模板/条款/母版行被写入。
3. 授权主体提交 v2 导入时：依赖门禁通过则单事务创建/复位为 `DRAFT`；失败则整笔回滚；**禁止**半导入。
4. v1 契约与既有 P14 行为保持可用；本片不强制管理 UI 切换到 v2。
5. 复用既有导出/导入权限与审计事件族；dry-run 成功/失败亦写审计（不含敏感明文）。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **E01-C1** | 新 bundle 格式常量：`template-export-bundle-v2-json`。v1 常量 `template-export-bundle-v1-json` **保持**可导出（默认）与可导入。 | CE §7 + P14 兼容 |
| **E01-C2** | 导出查询：`bundleVersion=2`（缺省 `1`）选择 v2；`format=zip` 时 v2 ZIP 为自包含载体。JSON 导出 v2 **允许**（便于检视清单），但**不含**母版 DOCX 原始字节；完整自包含以 ZIP 为准。 | 计划「ZIP 内嵌」+ 载荷体积 |
| **E01-C3** | v2 ZIP 条目（固定相对路径）：`template-export-bundle.json`（v2 schema）+ `artifacts/master.docx`（钉扎 revision 的 DOCX 字节）。禁止其它可执行条目；未知多余条目 → 导入校验失败。 | 计划卡 |
| **E01-C4** | **母版指纹**取自导出时解析到的版本行 CE-K01 字段：`masterRevisionId`、`masterFileHash`（SHA-256 小写十六进制）、可选 `revisionSequence`；并写入 `masterPin` 对象。若导出对象为 PUBLISHED/STOPPED/DEPRECATED release 行则用该行钉扎；若为 PENDING_RELEASE 且尚未钉扎 → 导出时读取当前 `current_revision_line` 计算 hash（`pinOrigin=EXPORT_TIME`），不改 DB。 | K01 Done |
| **E01-C5** | **render profile**：导出 `renderProfile.version` + `renderProfile.json`（发布锁快照优先；无锁则导出版本上行现有 profile；皆空则对象可省略但 dry-run 记 `RENDER_PROFILE_ABSENT` 为 **INFO**）。 | 计划卡 |
| **E01-C6** | **条款正文快照** `clauseSnapshots[]`：对模板 `contentModuleReferences` 中每个引用，嵌入导出时刻的模块版本正文/结构（`moduleCode`、`moduleVersionId`、`versionNumber`、**完整 `semanticVersion`**、**`sourceModuleId`（导出时模块 UUID，仅用于跨环境关联，不作目标身份）**、`contentStructureJson` 或等价正文快照字段、法务元数据若存在）。引用锁定标志一并带上。**不**嵌入测试数据集。 | 计划「条款正文快照」 |
| **E01-C7** | **资产键清单** `assetKeyManifest[]`：从绑定/结构化内容中收集的对象存储或引用键（如 image `referenceKey` / storage key），仅键名 + 用途分类枚举（`IMAGE` \| `OTHER`），**不**嵌入资产二进制（CE-E02 范围）。 | 计划 + E02 out of scope |
| **E01-C8** | 导出资格生命周期与 v1 相同：`PENDING_RELEASE` \| `PUBLISHED` \| `STOPPED` \| `DEPRECATED`；否则 `422` `api.error.template.exportNotEligible`。 | P14 |
| **E01-C9** | 导出/导入/dry-run **权限**与矩阵 §5 完全一致；无新权限码。越权 `403` fail-closed。 | 矩阵 §5 |
| **E01-C10** | **Dry-run：** 请求字段 `dryRun: true`（JSON）或 multipart 同名字段。HTTP **200** + `result.dependencyReport`；`imported=false`；**零** DB/对象存储写入（含不写审计之外的业务表；审计事件仍写）。 | 计划「dry-run 预检报告」 |
| **E01-C11** | **Dependency report** 每项至少：`dependencyType`（`MASTER_PIN` \| `CLAUSE` \| `ASSET_KEY` \| `RENDER_PROFILE` \| `BUNDLE_FORMAT`）、`severity`（`OK` \| `MISSING` \| `MISMATCH` \| `WILL_MATERIALIZE` \| `INFO`）、`code`（稳定 UPPER_SNAKE）、`messageKey`、`detail`（非敏感）。汇总：`blockingCount`、`warningCount`、`infoCount`、`readyToCommit`（`blockingCount==0`）。 | 计划 + 可测性 |
| **E01-C12** | Dry-run / 提交对 **MASTER_PIN**：比较 bundle `masterPin.masterFileHash` 与请求 `masterId` 指向母版的可解析 revision 文件 hash。匹配 → `OK`；母版不存在/未批准/组不匹配 → 既有 `masterNotApproved` / `masterGroupMismatch` / not-found（提交与 dry-run 均 fail-closed，dry-run 以 report + 顶层错误表达，见 E01-C18）；hash 不匹配 → `MISMATCH` / `MASTER_FINGERPRINT_MISMATCH`（**blocking**）。v2 ZIP 缺 `artifacts/master.docx` → `MISSING` / `MASTER_DOCX_ABSENT`（**blocking** for v2 ZIP commit；JSON-only v2 dry-run 记 blocking 除非仅检视）。 | 自包含 + 既有 masterId 门禁 |
| **E01-C13** | **CLAUSE：** 目标环境已存在相同 `moduleCode`+兼容版本 → `OK`；不存在但 bundle 含对应 `clauseSnapshots` 条目 → `WILL_MATERIALIZE`（非 blocking）；缺快照且目标亦无 → `MISSING`（**blocking**）。预检按 **`moduleCode` / `sourceModuleId→snapshot` 关联**解析条款身份；**不得**把 `contentModuleReferences.moduleId` 源环境 UUID 当作目标 `findById` 身份（空目标上会导致假 `CLAUSE_MISSING`）。 | 自包含条款 |
| **E01-C14** | **ASSET_KEY：** 对 manifest 中每个键，探测目标对象存储是否存在（HEAD/等价）。存在 → `OK`；不存在 → `MISSING`（**blocking**）。本片**不**上传/创建资产库对象（E02）。 | 计划 + E02 OOS |
| **E01-C15** | **提交导入（`dryRun` 缺省/false）：** 若 `readyToCommit==false`（存在 blocking）→ **422** `api.error.template.importDependenciesUnsatisfied`，body 含完整 `dependencyReport`；**不**写入模板/条款。通过则**单事务**：必要时从 `clauseSnapshots` 物化 DRAFT 内容模块版本（保留完整 `semanticVersion`，禁止仅压成 major `N.0.0` 而引用仍为 `1.2.3`）+ 将 `contentModuleReferences` **重映射到目标新 moduleId/版本** + 写模板 DRAFT（冲突策略同 v1）+ **导入时允许引用同事务物化的 DRAFT 版本**（import-time referencable seam；发布门禁仍要求 APPROVED+ACTIVE）+ 应用 render profile 到新 dev 行；母版仍绑定请求 `masterId`（**本片不**从 DOCX 自动创建母版实体——嵌入 DOCX 用于校验/归档/后续人工上传）。任一步失败 → 整笔回滚。 | 「而非导入后半残」 |
| **E01-C16** | 提交成功：`201`，`importSummary` 含 `resolvedTemplateId`、`newDevelopmentVersion`、`importBatchId`、`bundleFormat`、`materializedClauseCount`；模板 `DRAFT`；须重新测试→审批→发布。 | P14 + v2 扩展字段 |
| **E01-C17** | **导入载体：** (a) 既有 JSON body（v1 或 v2 JSON）；(b) `multipart/form-data`：`file`=@ZIP（v2 自包含）、`masterId`、可选 `importConflictPolicy`、`dryRun`。`Content-Type: application/zip` 整包 POST 可作为等价实现，但 OpenAPI 以 multipart 为规范面。 | ZIP 内嵌 DOCX |
| **E01-C18** | 结构性错误（不支持 format、schema 无效、含 secret、冲突策略）保持 v1 messageKey；在能解析 bundle 后优先返回 dependency report。授权失败仍 `403`，不泄露未授权资源是否存在。 | fail-closed |
| **E01-C19** | **审计：** 导出成功 → 既有导出审计；dry-run → `TEMPLATE_IMPORT_DRY_RUN`（含 `readyToCommit`、blockingCount、bundleFormat、actor；**无**条款正文全文、**无** DOCX 字节）；提交导入 → 既有导入审计，可附加 `bundleFormat=v2`、`materializedClauseCount`。 | 可观测 |
| **E01-C20** | **FE / E2E / UIUX：本片 out of scope。** 契约与后端测试为 Done 主证据；OpenAPI 类型若需前端机械同步，无用户旅程。 | Task testStrategy；G06 模式 |
| **E01-C21** | **明确非目标：** CE-E02 资产库 UI/上传、CE-E03 全库导出、CE-O01 PDF/A、从嵌入 DOCX 自动建母版并跳过母版审批、改 runtime 生成路径、go-live、CD-3、正式 P-phase。 | handoff OOS |
| **E01-C22** | 英文优先 `messageKey`（稳定）：`api.error.template.importDependenciesUnsatisfied`、`api.error.template.importBundleUnsupportedFormat`（v2 未知 format 同族）、dry-run 项级 keys 形如 `api.error.template.dep.masterFingerprintMismatch` 等；导出钉扎母版不可用复用 `api.error.rendering.pinnedMasterUnavailable`；实现时落入 `messages_en.properties`。 | i18n skill |

---

## 5. 前置条件

- CE-K01 已合并：可读取 `master_revision_id` / `master_file_hash` / render profile 锁。
- P14-T03 导出/导入 API、权限、冲突策略已存在且绿。
- 对象存储可读取钉扎母版 DOCX；可对资产键做存在性探测。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- 授权用户 `GET /api/management/v1/templates/{templateId}/export?bundleVersion=2&format=zip`（或 JSON）。
- 授权用户 `POST /api/management/v1/templates/import` 带 `dryRun=true`（JSON 或 multipart ZIP）。
- 授权用户同路径 `dryRun=false`/省略 提交导入。

---

## 7. Primary journey

### 7.1 导出自包含 v2 ZIP

1. 授权主体选择合格模板，请求 `bundleVersion=2&format=zip`。
2. 系统解析导出版本行 → 读 K01 钉扎（或 EXPORT_TIME hash）→ 拉取母版 DOCX 字节。
3. 系统收集条款引用并快照正文；收集资产键；嵌入 render profile。
4. 系统组装 ZIP（`template-export-bundle.json` + `artifacts/master.docx`），剥离 secrets，写导出审计，返回 `application/zip` 附件。

### 7.2 目标环境 dry-run

1. 运维在目标环境上传 v2 ZIP + `masterId` + `dryRun=true`。
2. 系统解析 ZIP、校验 schema、跑 MASTER/CLAUSE/ASSET/RENDER 预检，**不**写业务表。
3. 返回 `200` + `dependencyReport`（含 `readyToCommit`）。

### 7.3 提交导入

1. 运维在 `readyToCommit=true` 后以相同 bundle 提交（`dryRun` 假）。
2. 系统再次预检；blocking → `422` + report，无写入。
3. 通过则事务内物化缺失条款快照为 DRAFT 模块版本、创建/复位模板 DRAFT、绑定 `masterId`、应用 render profile；返回 `201` + summary；写导入审计。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| v2 ZIP 导出成功 | `200` `application/zip`；条目齐全；审计 |
| v2 JSON 导出成功 | `200` envelope；`format=template-export-bundle-v2-json`；无 DOCX 字节 |
| 导出不合格状态 | `422` `api.error.template.exportNotEligible` |
| 钉扎母版对象缺失 | 导出 fail-closed；`error.code=PINNED_MASTER_UNAVAILABLE`，`messageKey=api.error.rendering.pinnedMasterUnavailable`（与 K01/G06 复用） |
| Dry-run 可解析 | `200` + report；无业务写入 |
| Dry-run / 提交越权 | `403` |
| 提交 blocking 依赖 | `422` `api.error.template.importDependenciesUnsatisfied` + report |
| 提交成功 | `201` DRAFT + summary |
| v1 导入 | 行为不变（无 v2 依赖门禁；无条款物化） |
| 事务中途失败 | 整笔回滚；无半残行 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-CE-E01-001 — 导出 v2 ZIP 含母版 DOCX 与指纹

**Given** 授权 `GROUP_ADMIN` 且模板为 `PUBLISHED`，release 行已 K01 钉扎  
**When** `GET …/export?bundleVersion=2&format=zip`  
**Then** 响应为 ZIP；含 `template-export-bundle.json` 与 `artifacts/master.docx`  
**And** JSON `format` = `template-export-bundle-v2-json`  
**And** `masterPin.masterFileHash` 等于钉扎 `master_file_hash`  
**And** `artifacts/master.docx` 字节 SHA-256 等于该 hash  
**And** 审计记录导出成功

### BDD-CE-E01-002 — 导出 v2 含条款快照与资产键清单与 render profile

**Given** 模板含 ≥1 内容模块引用且结构化内容含 ≥1 资产键  
**When** 导出 v2（ZIP 或 JSON）  
**Then** `clauseSnapshots` 含对应模块正文/结构快照  
**And** `assetKeyManifest` 列出那些键（无二进制）  
**And** `renderProfile` 非空（当版本存在 profile 时）

### BDD-CE-E01-003 — 导出不含 secrets

**Given** 模板关联 API 策略与凭证  
**When** 导出 v2  
**Then** bundle 与 ZIP 内无任何 secret / 凭证材料 / 测试数据变量值

### BDD-CE-E01-004 — 默认仍为 v1 兼容

**Given** 合格模板  
**When** `GET …/export` 不带 `bundleVersion`（可带 `format=zip`）  
**Then** `format` = `template-export-bundle-v1-json`  
**And** ZIP（若有）仅含 `template-export-bundle.json`（无 `artifacts/master.docx`）

### BDD-CE-E01-005 — 导出资格门禁不变

**Given** 模板为 `DRAFT`  
**When** 请求导出 v2  
**Then** `422` `api.error.template.exportNotEligible`

### BDD-CE-E01-006 — 导出授权 fail-closed

**Given** 用户为 `TEMPLATE_TESTER`（无导出权）  
**When** 请求导出 v2  
**Then** `403`

### BDD-CE-E01-007 — Dry-run 零写入

**Given** 目标库模板数与内容模块数已知基线  
**When** `POST …/import` 带合法 v2 ZIP + `dryRun=true` + 合法 `masterId`  
**Then** `200` 且 `imported=false`  
**And** 模板数与内容模块数不变  
**And** 存在 `TEMPLATE_IMPORT_DRY_RUN` 审计

### BDD-CE-E01-008 — Dry-run 母版指纹不匹配为 blocking

**Given** v2 bundle `masterPin.masterFileHash=H1`，目标 `masterId` 当前 revision hash=`H2`≠`H1`  
**When** dry-run  
**Then** report 含 `MASTER_PIN` / `MISMATCH` / `MASTER_FINGERPRINT_MISMATCH`  
**And** `readyToCommit=false`

### BDD-CE-E01-009 — Dry-run 缺失条款将物化

**Given** v2 bundle 含某 `moduleCode` 的 `clauseSnapshots`，目标无该模块  
**When** dry-run  
**Then** 对应项 `WILL_MATERIALIZE`  
**And** 该项不计入 `blockingCount`

### BDD-CE-E01-010 — Dry-run 缺失资产键为 blocking

**Given** manifest 含键 `k`，目标对象存储不存在 `k`  
**When** dry-run  
**Then** `ASSET_KEY` / `MISSING`  
**And** `readyToCommit=false`

### BDD-CE-E01-011 — Dry-run 全绿 readyToCommit

**Given** 母版 hash 匹配、条款均可 OK 或 WILL_MATERIALIZE、资产键均存在  
**When** dry-run  
**Then** `readyToCommit=true` 且 `blockingCount=0`

### BDD-CE-E01-012 — 提交在 blocking 时拒绝且无半残

**Given** dry-run 将为 `readyToCommit=false` 的同一 bundle  
**When** 提交导入（`dryRun` 假）  
**Then** `422` `api.error.template.importDependenciesUnsatisfied`  
**And** 响应含 `dependencyReport`  
**And** 无新模板 DRAFT、无新内容模块行

### BDD-CE-E01-013 — 提交成功物化条款并落地 DRAFT

**Given** `readyToCommit=true` 的 v2 ZIP，含将物化的条款快照  
**When** 提交导入  
**Then** `201`；模板 `DRAFT`  
**And** `materializedClauseCount` ≥ 1  
**And** 新内容模块为草稿态，可进入后续审批  
**And** 模板绑定请求 `masterId`  
**And** 导入审计含 `bundleFormat` 指示 v2

### BDD-CE-E01-014 — 提交事务失败整笔回滚

**Given** 预检通过，但条款物化或模板写入在事务中失败（注入故障）  
**When** 提交导入  
**Then** 错误返回  
**And** 无残留半创建模板或半物化条款

### BDD-CE-E01-015 — v1 导入回归

**Given** 合法 v1 JSON bundle  
**When** 提交导入（非 dry-run）  
**Then** 行为与 P14-T03 一致（`201` DRAFT；无 v2 资产键门禁）

### BDD-CE-E01-016 — 不支持的 bundle format

**Given** `format` 既非 v1 亦非 v2 常量  
**When** 导入或 dry-run  
**Then** `422` `api.error.template.importBundleUnsupportedFormat`

### BDD-CE-E01-017 — 跨组 master / 未批准 master

**Given** `masterId` 组不匹配或未批准  
**When** dry-run 或提交  
**Then** 既有 `api.error.template.masterGroupMismatch` / `masterNotApproved`（fail-closed）  
**And** 无业务写入

### BDD-CE-E01-018 — FE 非目标

**Given** 本切片范围  
**When** 验收 Done 标准  
**Then** 不要求管理端导出/导入 UI 变更或 Playwright E2E/UIUX  
**And** 不实现 CE-E02/E03/O01

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| v2 JSON 无 DOCX 字节 | 可 dry-run 列出 `MASTER_DOCX_ABSENT`；**不可**作为自包含提交载体（须 ZIP） |
| 空 `assetKeyManifest` | 合法；无 ASSET 检查 |
| 空条款引用 | `clauseSnapshots=[]` 合法 |
| 超大 DOCX | 沿用既有上传/导出大小护栏；超限 fail-closed（实现钉死限值与 messageKey） |
| 并发 dry-run | 允许；互不影响 |
| Idempotency | dry-run 不要求 idempotencyKey；提交导入沿用既有导入语义 |
| 敏感数据 | 报告/审计禁止条款全文、变量值、secret、DOCX 字节 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| ZIP 字节 / 条目列表 | `master.docx` 存在；hash 匹配 |
| API envelope | dry-run `dependencyReport`；提交 `importSummary` |
| DB | 提交后 DRAFT 模板 + 物化条款；dry-run 后计数不变 |
| 审计 | 导出、`TEMPLATE_IMPORT_DRY_RUN`、导入 |
| 自动化 | 后端单元/切片测试覆盖 BDD-CE-E01-001…017；`mvn verify` |
| FE/E2E | **N/A**（E01-C20） |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7 CE-E01 | 计划卡 |
| Task Master **#78** | 执行叶 |
| [ce-k01-release-bundle-pinning.md](./ce-k01-release-bundle-pinning.md) | 指纹上游（K01-C13 声明本片消费钉扎） |
| [requirements-plan.md](../requirements/requirements-plan.md) 环境迁移 | 需求确认扩展 |
| [PRD.md](../product/PRD.md) §10 | 产品确认扩展 |
| [domain-model.md](../domain/domain-model.md) §6 | 领域迁移规则 |
| [contract-outline.md](../api/contract-outline.md) / OpenAPI | 管理 API 契约 |
| [permission-matrix.md](../security/permission-matrix.md) §5 | 导出/导入权限（无新码） |
| P14-T03 | v1 基线 |

---

## 13. TDD Red 映射（建议）

| 层 | 建议失败测试 |
| --- | --- |
| Backend export | `exportV2Zip_embedsMasterDocxAndMatchingHash`；`exportDefault_stillV1`；`exportV2_includesClauseSnapshotsAndAssetKeys`；`exportV2_stripsSecrets` |
| Backend dry-run | `dryRun_noDbMutation`；`dryRun_masterHashMismatch_blocking`；`dryRun_missingClause_willMaterialize`；`dryRun_missingAsset_blocking`；`dryRun_allGreen_readyToCommit`；`dryRun_sourceModuleIdsUnknownOnTarget_butSnapshotsCover_readyToCommit`（Critical #1） |
| Backend commit | `commit_blocking_returns422_noPartial`；`commit_materializesClauses_andWiresRemappedRefs`（Critical #2 / BDD-013）；`commit_midTxWiringFailure_propagatesWithoutImportAudit`（BDD-014 seam） |
| Backend commit | `commit_blocking_returns422_noPartial`；`commit_materializesClauses_andDraft`；`commit_transactionRollback_onMidFailure`；`importV1_regression` |
| Authz | `exportImportDryRun_forbiddenForTester` |
| Contract | OpenAPI `bundleVersion`、v2 schema、dryRun、dependencyReport、multipart |
| Frontend / E2E | **skip**（E01-C20） |

---

## 14. Handoff

```
bdd_readiness: ready
task_ids: ["78"]
slice: ce-e01-export-bundle-v2
behavior_doc: docs/behavior/ce-e01-export-bundle-v2.md
frontend_ui_in_scope: false
next: plan-orchestrator → backend-engineer (API/TDD); FE/E2E stages N/A
formal_phase: None
```

**Open questions:** 无（已由 CE §7 + K01 Done + P14 基线裁定；E01-C1…C22）。
