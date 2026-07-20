# BDD 行为规格：CE-G06 — 审计可复现最小集（受控再生）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-G06` |
| **编写日期** | 2026-07-16 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §6 Wave CE-G · CE-G06 |
| **Slice** | `ce-g06-audit-reproducible` |
| **Worktree** | removed after merge (`DGE-ce-g06-audit-reproducible`) |
| **Task Master** | **#76** → **Done**（merge `d8636232`） |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | MAIN（merged） |
| **上游** | CE-K01 (#57) **Done**（发布包钉扎）；CE-G02 (#73) **Done**（SPECIMEN 水印可复用） |
| **Owning docs** | 本文件（行为 SoT — **默认 SPECIMEN regenerate**）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；权限 [permission-matrix.md](../security/permission-matrix.md) §7 / §11；敏感数据例外 [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md)（修订 ADR-0020）；领域 [domain-model.md](../domain/domain-model.md) §2.12.2 / §2.17；钉扎上游 [ce-k01-release-bundle-pinning.md](./ce-k01-release-bundle-pinning.md)；水印上游 [ce-g02-specimen-watermark.md](./ce-g02-specimen-watermark.md)；管理端调用历史约束 [management-invocation-history.md](./management-invocation-history.md)；**下游扩展** [pd6-true-non-specimen-reissue.md](./pd6-true-non-specimen-reissue.md)（显式生产重发 opt-in；**不**改写本文件 G06-C* 默认语义） |

**完成声明约束：** 本切片关闭内控缺口「审计不可复现」的最小闭环（invocation 钉扎指纹 + 受控再生 API + SPECIMEN）；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-G04 legal hold / CE-G05 年检 / CE-E01 导出包；**不**交付管理端「再生」按钮或 E2E/UIUX（API-first）。

**下游（PD-6，2026-07-20）：** 同一 regenerate 入口可显式 `productionReissue=true` + `reason` 跳过 SPECIMEN（角色收窄至 `GLOBAL_ADMIN`/`GROUP_ADMIN`）。**默认 regenerate / preview / test 仍强制 SPECIMEN**——本文件 G06-C8/C13 默认路径继续有效；生产重发行为 SoT = [pd6-true-non-specimen-reissue.md](./pd6-true-non-specimen-reissue.md)。**无新 ADR**。

---

## 1. 概述

正式 runtime 生成后，调用记录仅有 `resolvedReleaseVersion` 等路由摘要，**未**持久化 CE-K01 发布包钉扎指纹，审计无法证明「当时用的是哪份母版 revision / 哈希」。内控需要在留存窗口内，由授权管理员按 invocation **受控重放**生成一份带 **SPECIMEN** 水印的审计样件（不可当作正式对外函件），并写入管理审计。

| 行为域 | 摘要 |
| --- | --- |
| **G06-S1 指纹落库** | 每次成功解析到已发布 release 的生成（含 sync / async / batch item）在 `api_invocation_record` 写入 `release_bundle_snapshot_id` + `release_bundle_hash` |
| **G06-S2 受控再生 API** | 管理端 `POST …/invocations/{invocationId}/regenerate`：权限 fail-closed、管理审计必写、产物 SPECIMEN 水印 |
| **G06-S3 水印复用** | 再生装配复用 CE-G02 DOCX 页眉页脚 + PDF 对角 `SPECIMEN` 模式；**正式 runtime 路径零改动** |
| **G06-S4 可观测** | 管理端 invocation 详情/摘要可返回快照 ID + hash（**仍禁止** parameters / variables 明文） |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| Invocation 无钉扎指纹列 | `ApiInvocationRecordEntity` / `V44__api_invocation_record.sql` 无 snapshot/hash |
| K01 钉扎在 `template_version` | `master_revision_id` + `master_file_hash` + `pin_metadata_json`（V57） |
| 管理端禁止暴露 parameters | [management-invocation-history.md](./management-invocation-history.md) C6；P13 HIST |
| SPECIMEN 仅预览路径 | CE-G02；`PreviewGenerationAssemblySupport`；runtime formal 无水印 |
| K01 将 drift 检测留给本片 | [ce-k01-release-bundle-pinning.md](./ce-k01-release-bundle-pinning.md) Q4 |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **Runtime API 调用方** | 凭证 + AD Group | 正常生成；invocation 自动落指纹；**无**再生 API 访问权 |
| **GLOBAL_ADMIN** | 全局管理员 | 可对任意模板 invocation 触发受控再生 |
| **GROUP_ADMIN** | 分组管理员 | 仅被授权组范围内模板可再生 |
| **AUDIT_ADMIN** | 审计管理员 | 可读模板范围内可触发再生（合规复现）；须具备对该模板的读审计/包可见边界（见 G06-C8） |
| **系统** | Invocation 持久化 + 再生装配 + ManagementAudit | 写指纹；再生 fail-closed；审计；SPECIMEN |
| **（间接）法务 / 内控** | 受益方 | 可复现历史装配输入指纹；再生件不可冒充正式函 |

---

## 3. Goal

1. 成功解析到 PUBLISHED release 的 runtime 生成，在对应 `api_invocation_record`（SINGLE / BATCH_ITEM / ASYNC 成功产物行；BATCH_ROOT 见 G06-C4）持久化 **release-bundle snapshot ID** 与 **bundle hash**。
2. 授权管理员可调用管理端受控再生 API，使用该 invocation 已存参数（内部读取 `parametersStorage`）+ 钉扎母版，产出 **SPECIMEN** 标记的 DOCX/PDF。
3. 再生成功/失败均写入管理审计事件；失败不落无水印成功件。
4. 正式 runtime 生成路径**不**因本片获得 SPECIMEN；既有 runtime 金标/护栏保持。
5. 管理端列表/详情/CSV **仍不得**返回 variables / parameters 明文；可返回 snapshot id + hash。
6. 本片 **不**交付管理 UI 再生按钮；E2E/UIUX **not-applicable**。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **G06-C1** | **`release_bundle_snapshot_id`** = 生成时解析到的 **`template_version.id`**（PUBLISHED release 行主键 UUID）。 | 计划「发布包快照 ID」+ K01 钉扎载体 |
| **G06-C2** | **`release_bundle_hash`** = 该 `template_version.master_file_hash` 的拷贝（64 字符小写十六进制 SHA-256）。生成时从钉扎行读取写入 invocation，**不**在落库时重算对象存储字节（避免与 runtime 热路径竞态）；再生时再做 drift 校验（G06-C11）。 | 计划「bundle hash」+ K01 `master_file_hash` |
| **G06-C3** | 写入时机：runtime 已成功解析 `TemplateVersionEntity` 且准备/完成写入 invocation 时；与 `resolved_release_version` 同源。 | 现有 `InvocationRecordService` |
| **G06-C4** | **哪些行写指纹：** `SINGLE`、`BATCH_ITEM`、`ASYNC_TASK`（有解析 release 的生成行）必须写。`BATCH_ROOT`：**不要求**写指纹（无单笔装配）；再生 API **仅**接受 `SINGLE` / `BATCH_ITEM` / `ASYNC_TASK`。 | 最小可复现单元 = 单笔产物 |
| **G06-C5** | 解析失败或未解析到 release 的失败记录：两字段保持 **NULL**；不可再生。 | fail-closed |
| **G06-C6** | **不** Flyway 回填历史 invocation 指纹（历史时刻哈希不可可靠重建）。仅本片上线后新写入的记录具备再生资格。 | 审计诚实性 |
| **G06-C7** | 管理端再生 API（建议路径）：`POST /api/management/v1/templates/{templateId}/api/invocations/{invocationId}/regenerate`；可选 body：`{ "outputFormat": "DOCX" \| "PDF" }`（缺省 = 原 invocation 的 `output_format`，若原为空则 `PDF`）。统一 envelope。 | 计划「管理端 API」 |
| **G06-C8** | **授权（fail-closed）：** 仅 `GLOBAL_ADMIN`、同组 `GROUP_ADMIN`、以及对模板可见的 `AUDIT_ADMIN`（沿用 `readAudit` + 模板组范围，与生成审计摘要可见边界对齐）。其他角色 → **403** `api.error.authorization.forbidden`（或既有管理端等价码）。跨组探测不得泄露 invocation 是否存在（对齐既有 404/403 惯例）。 | 计划「需权限」+ matrix §7/§10 |
| **G06-C9** | 再生**内部**读取 `parametersStorage` 还原 variables（及非密码加密摘要）；**响应与审计不得**回传 variables / 密码明文。成功响应仅含：`regenerationId`、`sourceInvocationId`、`releaseBundleSnapshotId`、`releaseBundleHash`、`outputFormat`、artifact 下载引用（短期 URL 或管理端 artifact 路径）、`specimen=true`。 | HIST C6 + **ADR-0057**（修订 ADR-0020 留存例外） |
| **G06-C10** | 原请求若启用了输出加密：再生件 **一律不加密**（密码已 strip，无法忠实复现密文）。响应/审计可记 `encryptionReapplied=false`。 | 加密密码不落库 |
| **G06-C11** | **Drift 检测（承接 K01-Q4）：** 再生前读取钉扎 `master_revision_id` 的对象字节，重算 SHA-256，与 invocation 上 `release_bundle_hash` 比对；不一致 → **fail-closed** `409` `error.code=RELEASE_BUNDLE_HASH_MISMATCH`，`messageKey=api.error.audit.releaseBundleHashMismatch`，`retryable=false`。一致则继续装配。 | K01 Q4 |
| **G06-C12** | 钉扎 revision / 对象缺失 → **fail-closed** 复用 K01 键 `api.error.rendering.pinnedMasterUnavailable`（`error.code=PINNED_MASTER_UNAVAILABLE`，category `RENDERING`）。**不**并行引入 `api.error.audit.pinnedMasterUnavailable`。 | K01-C4；契约钉死 |
| **G06-C13** | 再生装配：**复用 CE-G02** SPECIMEN（DOCX 眉脚字面量 `SPECIMEN` + PDFBox 对角 `SPECIMEN`）；水印失败 → fail-closed，不落无水印成功件（对齐 G02-C8）。存储前缀与 preview/runtime 分离，建议 `regenerations/{regenerationId}/output.docx|pdf`。 | 计划 + G02 |
| **G06-C14** | 再生**不得**写入新的 runtime 成功调用记录冒充调用方生成；**不得**消耗调用方幂等键。可写独立 regeneration 元数据表或仅对象存储 + 审计（实现选型，须可按 `regenerationId` 取回）。 | 审计边界 |
| **G06-C15** | **管理审计：** 事件类型 `INVOCATION_REGENERATED`（成功与失败终态均写；失败含 errorCode，不含 variables）。摘要字段至少：`sourceInvocationId`、`regenerationId`、`releaseBundleSnapshotId`、`releaseBundleHash`、`outputFormat`、`outcome`、`actorUsername`。 | 计划「+ 审计」 |
| **G06-C16** | 管理端 detail/summary **可**暴露 `releaseBundleSnapshotId` + `releaseBundleHash`；CSV 可选同列。**禁止**因此放宽 C6。 | 可观测最小集 |
| **G06-C17** | **FE 管理 UI 再生按钮 / Drawer CTA：本片 out of scope。** 无 Playwright E2E/UIUX 义务。契约与后端测试为 Done 主证据；若 FE 类型需跟 OpenAPI 字段，仅机械同步，无用户旅程。 | 计划「最小集」+ Task 「as applicable」 |
| **G06-C18** | Idempotency：再生 API **不**要求 `idempotencyKey`；每次调用可产生新 `regenerationId`（审计可复现操作本身可多次）。 | 最小集 |
| **G06-C19** | 仅未过期（`record_expires_at` 未到）且指纹非空的 invocation 可再生；过期/已清理 → **410** `INVOCATION_RECORD_EXPIRED`（`api.error.audit.invocationRecordExpired`）。 | ADR-0040；契约钉死（原 Q2） |
| **G06-C20** | **明确非目标：** CE-G04 legal hold、CE-G05 年检、CE-E01 导出包、像素比对、将再生件写入 runtime formal 下载、调用方自助再生、回填历史指纹、go-live、CD-3、正式 P-phase。 | 计划 out of scope |
| **G06-C21** | **`parameters_storage` 留存例外（ADR-0057）：** 已消毒 variables 可在调用记录 TTL 内落库，供调用方查询与本片再生内部重放；管理端/审计/日志/导出仍禁明文；TTL 与 invocation 行同生命周期清理；**本片不要求**列级 encryption-at-rest（deferred / pending KMS，见 ADR-0057）。 | Arch Critical 关闭；matrix §11 |

---

## 5. 前置条件

- CE-K01 已合并：PUBLISHED `template_version` 具备 `master_revision_id` + `master_file_hash`；runtime 装配读钉扎 revision。
- CE-G02 已合并：可复用 SPECIMEN stamper（或等价预览水印组件）于再生路径。
- `api_invocation_record` + `InvocationRecordService` + 管理端 invocation 查询已存在。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- Runtime 调用方触发 sync / async / batch 生成并写入 invocation。
- 授权管理员调用 regenerate API。
- 管理员打开管理端 invocation 详情（只读快照字段）。
- CI 执行 `mvn -B -ntp -f backend/pom.xml verify`。

---

## 7. Primary journey（指纹 + 受控再生）

1. 调用方对已发布 release 成功生成文档；系统写入 `api_invocation_record`，含 `release_bundle_snapshot_id = template_version.id`、`release_bundle_hash = master_file_hash`。
2. 审计/管理员定位该 `invocationId`（管理端历史或 API）。
3. 管理员 `POST …/regenerate`（可选指定 outputFormat）。
4. 系统校验权限 → 记录未过期 → 指纹非空 → 重算钉扎母版哈希并比对 → 内部还原 parameters → 钉扎装配 → SPECIMEN → 存 regenerations 前缀 → 写 `INVOCATION_REGENERATED` 审计 → 返回下载引用（无 variables）。
5. 管理员下载再生件：DOCX/PDF 可观察到 `SPECIMEN`；正式 runtime 再生成同模板仍无 SPECIMEN。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| Runtime 成功生成（有解析 release） | invocation 行两字段非空 |
| Runtime 失败且未解析 release | 两字段 NULL |
| 再生成功 | 200 + regeneration 摘要；artifact SPECIMEN；审计 SUCCESS |
| 无权限 | 403 fail-closed |
| 指纹缺失 / 历史未记录 | 409 或 422 `api.error.audit.releaseBundleSnapshotUnavailable` |
| Hash drift | 409 `RELEASE_BUNDLE_HASH_MISMATCH` |
| 钉扎母版不可用 | fail-closed pinnedMasterUnavailable |
| 水印失败 | fail-closed；无成功无水印件 |
| 记录过期 | 410 `INVOCATION_RECORD_EXPIRED` |
| BATCH_ROOT 再生 | 400/422 不支持（指引用 ITEM） |

---

## 9. 验收场景（Given / When / Then）

### A. 指纹持久化

#### BDD-CE-G06-001 — sync 成功写入 snapshot id

**Given** 模板存在 PUBLISHED release，其 `template_version.id = TV1` 且 `master_file_hash = H1`  
**When** 调用方对该 release 同步生成成功  
**Then** 对应 `SINGLE` invocation 的 `release_bundle_snapshot_id = TV1`  
**And** `release_bundle_hash = H1`

#### BDD-CE-G06-002 — hash 与 K01 钉扎一致

**Given** BDD-CE-G06-001 刚完成  
**When** 独立读取 `template_version` 行  
**Then** invocation.`release_bundle_hash` 等于该行 `master_file_hash`

#### BDD-CE-G06-003 — batch item 写入指纹

**Given** 同步或异步批量中至少一笔 ITEM 成功装配  
**When** 持久化该 `BATCH_ITEM`  
**Then** 该 ITEM 行含非空 snapshot id + hash  
**And** `BATCH_ROOT` 可不含指纹

#### BDD-CE-G06-004 — 解析失败不写指纹

**Given** 生成在解析 PUBLISHED release 之前失败  
**When** 写入失败 invocation（若有）  
**Then** `release_bundle_snapshot_id` 与 `release_bundle_hash` 均为 NULL

#### BDD-CE-G06-005 — 本片前历史记录保持 NULL

**Given** 上线前已存在的 invocation 行无指纹列值（迁移后为 NULL）  
**When** 管理员尝试 regenerate  
**Then** fail-closed `api.error.audit.releaseBundleSnapshotUnavailable`  
**And** 系统不猜测回填 hash

### B. 受控再生 API

#### BDD-CE-G06-006 — GLOBAL_ADMIN 再生成功且 SPECIMEN

**Given** 存在带非空指纹的成功 `SINGLE` invocation  
**And** 钉扎母版对象哈希仍等于 `release_bundle_hash`  
**When** GLOBAL_ADMIN 调用 regenerate（outputFormat=PDF）  
**Then** HTTP 200，返回 `regenerationId` 与 artifact 引用  
**And** PDF 文本抽取含 `SPECIMEN`  
**And** 响应不含 variables / 密码

#### BDD-CE-G06-007 — DOCX 再生含眉脚 SPECIMEN

**Given** 同上  
**When** regenerate outputFormat=DOCX  
**Then** DOCX header 与 footer 均含字面量 `SPECIMEN`（对齐 G02-C2）

#### BDD-CE-G06-008 — GROUP_ADMIN 组内可、跨组不可

**Given** invocation 属于组 A 模板  
**When** 组 A 的 GROUP_ADMIN 再生 → **Then** 允许（其它门禁通过时）  
**When** 组 B 的 GROUP_ADMIN 再生 → **Then** 403/404 fail-closed，不泄露存在性细节

#### BDD-CE-G06-009 — AUDIT_ADMIN 可再生（模板可见范围内）

**Given** AUDIT_ADMIN 对该模板具备 readAudit 可见性  
**When** 调用 regenerate  
**Then** 在其它门禁通过时允许  
**And** 写 `INVOCATION_REGENERATED` 审计

#### BDD-CE-G06-010 — TEMPLATE_AUTHOR 禁止再生

**Given** 调用者为 TEMPLATE_AUTHOR（或 TESTER / APPROVER / MASTER_DESIGNER）  
**When** 调用 regenerate  
**Then** 403 fail-closed  
**And** 不产生 regeneration artifact

#### BDD-CE-G06-011 — 审计事件必写

**Given** 任意 regenerate 终态（成功或业务失败）  
**When** 查询管理审计  
**Then** 存在 `INVOCATION_REGENERATED` 事件  
**And** 含 sourceInvocationId / actor / outcome；成功含 regenerationId；**不含** variables 明文

#### BDD-CE-G06-012 — 不新建 runtime 调用方 invocation

**Given** regenerate 成功  
**When** 按调用方 credential 查询 runtime invocations  
**Then** 无新增冒充该 credential 的 SUCCESS 生成记录  
**And** 原 invocation 行不被改写 outcome

#### BDD-CE-G06-013 — 原加密件再生不加密

**Given** 原请求 encryption.enabled=true（密码已从 parametersStorage strip）  
**When** regenerate 成功  
**Then** 再生件未加密（可打开无密码）  
**And** 元数据/审计可观察 `encryptionReapplied=false`（或等价）

### C. Drift / 边界

#### BDD-CE-G06-014 — bundle hash mismatch

**Given** invocation 指纹 H1，但钉扎 revision 对象字节哈希变为 H2≠H1  
**When** regenerate  
**Then** 409 `RELEASE_BUNDLE_HASH_MISMATCH`  
**And** `messageKey=api.error.audit.releaseBundleHashMismatch`  
**And** 无 artifact

#### BDD-CE-G06-015 — 钉扎母版缺失

**Given** `master_revision_id` 对象存储 404  
**When** regenerate  
**Then** fail-closed pinnedMasterUnavailable  
**And** 审计 outcome=FAILURE

#### BDD-CE-G06-016 — 水印失败 fail-closed

**Given** SPECIMEN stamper 抛错（测试替身）  
**When** regenerate  
**Then** 请求失败  
**And** 对象存储无「无水印成功件」

#### BDD-CE-G06-017 — BATCH_ROOT 拒绝

**Given** invocationKind=BATCH_ROOT  
**When** regenerate  
**Then** 4xx 业务错误（不支持 ROOT）  
**And** 指引使用 BATCH_ITEM

#### BDD-CE-G06-018 — 过期记录不可再生

**Given** `record_expires_at` 已过  
**When** regenerate  
**Then** 410 `INVOCATION_RECORD_EXPIRED`  
**And** `messageKey=api.error.audit.invocationRecordExpired`  
**And** 无 artifact

### D. 管理端可观测 + runtime 护栏

#### BDD-CE-G06-019 — detail 返回指纹、无 parameters

**Given** 带指纹的 invocation  
**When** GET management invocation detail  
**Then** 响应含 `releaseBundleSnapshotId` 与 `releaseBundleHash`  
**And** 不含 parameters / variables

#### BDD-CE-G06-020 — runtime formal 仍无 SPECIMEN

**Given** 本片已合并  
**When** 同一模板走正式 runtime sync 生成  
**Then** 产物 OOXML/PDF 文本不含 `SPECIMEN`  
**And** 既有 G02 runtime 护栏仍绿

#### BDD-CE-G06-021 — 未授权角色无 FE 义务

**Given** 本片范围  
**When** 验收 Done  
**Then** 无管理 UI 再生 CTA、无 CE-G06 Playwright E2E/UIUX 强制项  
**And** 门禁以 `mvn verify` + 契约/单测为主；FE gates仅在机械类型同步时 as applicable

---

## 10. Observable evidence

| 证据 | 说明 |
| --- | --- |
| DB | `api_invocation_record.release_bundle_snapshot_id` / `release_bundle_hash` |
| API | regenerate 200 + download；detail 字段；错误码 messageKey |
| Artifact | regenerations 前缀；DOCX/PDF 含 `SPECIMEN` |
| Audit | `INVOCATION_REGENERATED` 行 |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` GREEN；OpenAPI/contract-outline/messageKeys 对齐 |
| 非证据 | 像素比对；FE E2E 截图；历史 invocation 回填 |

---

## 11. Traceability

| 来源 | 引用 |
| --- | --- |
| Plan | CE §6 CE-G06；风险「审计不可复现」R3 |
| Task Master | **#76**；依赖 **#57** |
| Upstream BDD | K01（钉扎）；G02（SPECIMEN）；HIST C6（无 parameters） |
| Permission | [permission-matrix.md](../security/permission-matrix.md) §7 再生行 + **§11 ADR-0057 留存例外** |
| ADR | [ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md)（修订 [ADR-0020](../adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md)；对齐 [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md)） |
| Domain | [domain-model.md](../domain/domain-model.md) §2.12.2 / §2.17 |
| i18n | English-first `api.error.audit.*` messageKeys |

---

## 12. Out of scope（本片禁止）

- CE-G04 legal hold / CE-G05 annual review / CE-E01 export bundle。
- 管理端再生按钮、Drawer CTA、专用再生工作台、Playwright E2E/UIUX。
- 历史 invocation 指纹回填；调用方 runtime 自助再生。
- 再生件作为正式对外下载或去掉 SPECIMEN。
- 像素/视觉回归；go-live；CD-3；正式 P-phase。

---

## 13. Open questions（非阻塞）

| # | 问题 | 默认建议（本片采用） |
| --- | --- | --- |
| Q1 | `bundle_hash` 是否改为 composite（masterRevisionId + masterFileHash + renderProfileVersion）？ | **否**；采用 K01 `master_file_hash` 拷贝，简单可验证 |
| Q2 | 过期记录错误用 410 还是 404？ | **已钉死 410**（`INVOCATION_RECORD_EXPIRED`）；见 G06-C19 + contract-outline |
| Q3 | 是否持久化独立 `invocation_regeneration` 表？ | **建议是**（id、source、actor、created_at、storage keys、outcome）；允许首版仅审计+对象键若实现更小 |
| Q4 | FE 是否在 Drawer 只读展示 snapshot/hash？ | **本片不强制**；API 字段先落地，UI 展示可后续 CE-U 小片 |
| Q5 | AUDIT_ADMIN 是否必须排除出再生权、仅 GLOBAL/GROUP？ | **否**（对本片默认 SPECIMEN 路径）；默认含 AUDIT_ADMIN（合规复现主角色），仍受模板可见范围约束。**PD-6 澄清：** 生产重发（`productionReissue=true`）角色收窄至 GLOBAL/GROUP；AUDIT_ADMIN **禁止**无水印路径（见 [pd6-true-non-specimen-reissue.md](./pd6-true-non-specimen-reissue.md) PD6-C4） |
| Q6 | `parameters_storage` 是否必须列级/应用层 encryption-at-rest？ | **本片否**（ADR-0057 deferred）；补偿控制 = 访问边界 + 调用记录 TTL 清理 + 管理端不暴露。待 KMS 后再评估 |

---

## 14. FE management UI recommendation

| 项 | 本片 | 理由 |
| --- | --- | --- |
| **Regenerate CTA / 用户旅程** | **No** | 计划为「管理端 API」最小集；再生属高敏合规动作，宜 API + 审计先行；避免本片背负 E2E/UIUX |
| **Detail 只读展示 snapshot/hash** | **Optional / deferred** | 后端 detail 字段 **Yes**；FE 展示不阻塞 BDD ready / 后端 Done |
| **E2E / UIUX** | **not-applicable** | 无用户可见再生旅程 |

---

## 15. BDD readiness

```
bdd_readiness: ready
acceptance_scenario_count: 21
open_questions: [Q1, Q3, Q4, Q5, Q6]  # Q2 pinned 410; Q6 encryption-at-rest deferred per ADR-0057 (non-blocking)
owning_doc: docs/behavior/ce-g06-audit-reproducible.md
task_ids: [CE-G06, ce-g06-audit-reproducible, "#76"]
task_status: Done
merge_sha: d86362329e4a2ee496e82eedb7d4c83068574e43
fe_regenerate_ui_in_scope: no
docs_aligned: requirements / PRD / domain / permission-matrix §11 / ADR-0057 / OpenAPI / contract-outline (2026-07-16)
gates: mvn verify GREEN 1748; architecture PASS (ADR-0057); Stage 10 DEPLOY_OK Flyway V64; FE/E2E/UIUX N/A (G06-C17)
next: CE-E02 #79 pending/parked (do not activate until deliver)
```

**Handoff（Done）：** Task Master **#76** → **Done**（merge `d8636232`）。指纹落库、受控再生、SPECIMEN、审计、runtime 无水印护栏与 ADR-0057 已交付。正式 phase 保持 **None**；不宣称 go-live；不激活 CD-3。后继：**#78** CE-E01 → **Done**（merge `6ae57974`）；下一 queue head：**#79** CE-E02（pending，勿提前激活）。
