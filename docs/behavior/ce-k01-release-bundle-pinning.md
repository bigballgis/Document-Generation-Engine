# CE-K01 发布包不可变快照（钉扎）— BDD

| Field | Value |
| --- | --- |
| **Slice** | `ce-k01-release-bundle-pinning` |
| **Plan task** | **CE-K01**（[core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) § CE-K01） |
| **Task Master** | **#57** |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-14 |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | ISOLATED `D:/working/DGE-ce-k01-release-bundle-pinning` · `feat/ce-k01-release-bundle-pinning` |
| **Scope of this slice** | 发布时钉扎 master revision + master file hash + render profile 快照；运行时装配读钉扎引用；被已发布 release 引用的 master revision 不可物理删除；存量 release 回填并标 `PINNED_RETROACTIVELY`。**不**改锚点提取逻辑、**不**改预览路径语义（预览仍跟随 dev 母版） |
| **Owning docs** | 本文件（行为 SoT）；计划映射 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md)；ADR-0019 [0019-structured-authoring-and-rendering-boundary.md](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md)（已声明 release 锁样式/引用，本片对齐 master revision 维度）；权限矩阵 [permission-matrix.md](../security/permission-matrix.md) |

---

## 1. 概述

本切片让任一**已发布 release** 在任意时刻重放生成时，产物与首发使用**同一份母版 revision 文件**（字体/页眉/锚点/正文不变），即使母版后续被替换为新 revision 文件、母版停用或被删除引用也保持稳定。审计可复现历史产出，符合 [ADR-0019](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) 已声明的"已发布模板锁定发布时使用的母版样式与引用"语义。

**现状证据（R2 审查）：**
- `DocumentGenerationAssemblySupport.generate()` 第 87 行 `masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())` 取**当前** master 实体，第 96 行 `objectStoragePort.get(master.getStorageKey())` 读 live storageKey。
- `TemplateVersionEntity` 持有 `renderProfileVersion`/`renderProfileJson`（已被 `RenderProfileService.lockForPublish` 快照）但**无** `master_revision_id`、**无** `master_file_hash`。
- 发布流程 `TemplateLifecycleApprovalFlowSupport.publish()` 调用 `renderProfileService.lockForPublish(version)`、`versionFidelityWarningService.snapshotOnPublish(...)`、`contentModuleReferenceService.lockReferencesForPublish(...)`，**唯独不钉扎 master revision**。
- `master_revision_line` 表（V32 迁移）已存在持久化 revision 行（含独立 `storage_key`、`revision_sequence`、`is_current`），为本片提供天然锚点。
- 母版 revision 当前无"被已发布 release 引用"的删除阻断。

**改动面（计划卡确认）：** `template`（发布流程写入钉扎字段 + Flyway 迁移加列）、`runtime`/`rendering` 装配读取钉扎 revision 而非 live master、`master` 删除/停用保护。

---

## 2. Actor / Role

| Actor | 说明 | 关注点 |
| --- | --- | --- |
| **Team Lead / Publisher** | 走 PENDING_RELEASE → PUBLISHED 发布流程 | 发布后产出可复现 |
| **Runtime 生成路径（sync / async / batch）** | `DocumentGenerationAssemblySupport` 装配 | 读取钉扎 revision 的 storageKey |
| **Master Designer / Master 管理员** | 替换母版文件、停用/删除旧 revision | 不影响已发布 release |
| **审计 / 法务** | 复现历史产出、按 release 圈定召回范围 | revision 指针 + file hash 可观察 |
| **平台工程师 / DBA** | 执行 Flyway 回填迁移 | 存量 release 标 `PINNED_RETROACTIVELY` |

---

## 3. Goal

1. 发布为 release 时在 `template_version` 行持久化 **`master_revision_id`**（指向 `master_revision_line.id`）、**`master_file_hash`**（母版 DOCX 字节哈希，算法固定 SHA-256）、并保留已有 **render profile 快照**（`renderProfileJson`/`renderProfileVersion`，由 `lockForPublish` 写入）。
2. 运行时装配（sync / async / batch 三条路径）**只读取钉扎 revision 的 `storage_key`**，不再读 live `master.getStorageKey()`。
3. 母版 revision 被**任一 PUBLISHED release** 引用时，**不可物理删除**；fail-closed 拒绝。
4. 存量已发布 release（无 `master_revision_id`）由 Flyway 回填为当前 `current_revision_line_id` + 计算 `master_file_hash`，并在执行账本（execution-sync-ledger 或专用迁移日志表）标记 **`PINNED_RETROACTIVELY`**。
5. **不改**锚点提取逻辑；**不改**预览路径语义（预览允许跟随 dev 母版）。
6. **D7 release-locked 语义保留：** 停用 master revision **不取消**在途异步生成任务；在途任务按其已读取的钉扎 revision 跑完。

---

## 4. 已确认决策（2026-07-14）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **K01-C1** | 钉扎字段加在 `template_version` 表：`master_revision_id UUID`（引用 `master_revision_line.id`，无 ON DELETE，应用层校验防止物理删除）、`master_file_hash VARCHAR(64)`（SHA-256 十六进制）、`pin_metadata_json TEXT`（含 `pinnedAt`/`pinnedBy`/`pinOrigin`）。 | 计划卡 + 用户确认 D1–D7 |
| **K01-C2** | `master_file_hash` 在发布时由后端读取钉扎 revision 的 DOCX 字节流计算 SHA-256；不依赖对象存储 ETag。 | 计划卡 |
| **K01-C3** | render profile 快照沿用既有 `RenderProfileService.lockForPublish`；本片**不重写**其语义，仅在本片 BDD 中显式断言"发布后 `renderProfileJson` 非空且运行时使用该快照"。 | 现状代码 + 计划卡 |
| **K01-C4** | 运行时装配从 `TemplateVersionEntity.masterRevisionId` 解析 `master_revision_line.storage_key`；当 `masterRevisionId` 为空（仅可能存在于未迁移完成的过渡窗口或非 PUBLISHED 版本）→ **fail-closed** `api.error.rendering.pinnedMasterUnavailable`，**禁止**回退到 live master。 | 计划卡"运行时装配只读取钉扎引用" |
| **K01-C5** | 三条生成路径（sync / async / batch）统一走同一装配入口，钉扎解析逻辑单一来源。 | 计划卡 |
| **K01-C6** | 物理删除 master revision（无论硬删 `master_revision_line` 行还是其对象存储对象）前必须查询是否存在 `template_version.master_revision_id = <revision.id>` 且 `lifecycle_status = PUBLISHED` 且 `template_version.deleted_at IS NULL` 的引用；存在则 **409 fail-closed**，错误码 `api.error.master.revisionInUseByPublishedRelease`。 | 计划卡"停用后的 master revision 不可被物理删除" |
| **K01-C7** | 软删除（logical delete）`master_revision_line`（设 `deleted_at`）同样受 K01-C6 约束，避免软删后对象存储被清理导致钉扎失效。 | 计划卡 + fail-closed 默认 |
| **K01-C8** | Flyway 回填迁移：对每个 `template_version.lifecycle_status = PUBLISHED` 且 `master_revision_id IS NULL` 的行，取其 `template.master_id` 的当前 `current_revision_line_id` 作为钉扎值，并计算该 revision DOCX 字节的 SHA-256 写入 `master_file_hash`；`pin_metadata_json.pinOrigin = 'PINNED_RETROACTIVELY'`；在 `execution-sync-ledger.md` 记录回填范围（受影响行数、迁移版本号）。 | 计划卡 + 用户确认 D1–D7 |
| **K01-C9** | 回填迁移 **幂等**：仅当 `master_revision_id IS NULL` 时写入；重复执行不覆盖已存在的钉扎值。 | Flyway 最佳实践 |
| **K01-C10** | **D7 release-locked：** 停用（status 置为非 ACTIVE）一个被已发布 release 钉扎的 master revision **不取消**在途异步任务；在途任务继续按其发布时已解析的钉扎 revision 跑完。停用仅阻止其被**新发布**的 release 钉扎。 | 用户确认 D7 |
| **K01-C11** | 停用（STOPPED）/弃用（DEPRECATED）一个 release **不解除**对其 `master_revision_id` 的引用，亦**不释放**该 revision 供物理删除（K01-C6 仍生效）。 | 计划卡 + 审计可复现要求 |
| **K01-C12** | **本片禁止：** 改锚点提取逻辑、改预览路径语义（预览继续跟随 dev 母版）、改 master revision 的 revision_sequence 编号策略、做 K04 语义级 diff、做 K05 真实影响分析。这些属后续切片。 | 计划卡"禁止"栏 |
| **K01-C13** | 本片**不**实现 CE-E01 自包含导出包 v2（其依赖 K01 数据），仅保证 DB 层钉扎字段可被 E01 后续消费。**下游：** [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md) E01-C4/C12 消费 `master_revision_id` / `master_file_hash`。 | 计划卡依赖图 |
| **K01-C14** | `pin_metadata_json` 至少包含 `pinOrigin` ∈ {`PUBLISHED`, `PINNED_RETROACTIVELY`}；`PUBLISHED` 由发布流程写入、`PINNED_RETROACTIVELY` 由 Flyway 回填写入。 | K01-C8 + 发布流程 |

---

## 5. 前置条件

- `master_revision_line` 表（V32）已存在并被 `MasterRevisionLineService` 维护。
- `template_version` 表已有 `render_profile_version`/`render_profile_json` 列（V31）并由 `lockForPublish` 写入。
- 发布流程 `TemplateLifecycleApprovalFlowSupport.publish` 现有 gate 链（`apiPolicyMaterializationService` / `publishGateService` / `versionFidelityWarningService.snapshotOnPublish` / `contentModuleReferenceService.lockReferencesForPublish`）已绿。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- Team Lead 在 PENDING_RELEASE 状态下调用发布接口（`POST .../templates/{id}/lifecycle:publish`），传入 `releaseVersion` 与 `fidelityViewedConfirmed`。
- 存量库执行 Flyway 升级到本片新增迁移版本。
- Runtime 调用任一已发布 release 的 sync/async/batch 生成。
- Master 管理员尝试删除/停用一个 master revision。

---

## 7. Primary journey（发布钉扎）

1. Team Lead 触发 publish；后端在事务内：
   - 既有 gate（`publishGateService.assertReady` / `apiPolicyMaterialization` / `lockReferencesForPublish` / `snapshotOnPublish` / `lockForPublish`）全部通过。
   - 解析 template 当前 master 的 `current_revision_line_id` → 读 `master_revision_line` 行得到 `storage_key`。
   - 读取该 storage_key 对应的 DOCX 字节流计算 SHA-256 → `master_file_hash`。
   - 在 `template_version` 行写入 `master_revision_id`、`master_file_hash`、`pin_metadata_json = {"pinOrigin":"PUBLISHED","pinnedAt":<ISO>, "pinnedBy":<username>}`。
   - 置 `lifecycle_status = PUBLISHED`、`release_version` 写入，事务提交。
2. 此后母版被替换为新 revision 文件（`current_revision_line_id` 变更、新 `master_revision_line` 行插入）。
3. Runtime 收到对该 releaseVersion 的生成请求 → 从 `template_version.master_revision_id` 解析 storage_key → 读该 revision DOCX 字节 → 装配 → 产出。
4. 产出与首发使用同一份母版文件，字体/页眉/锚点/正文一致。

---

## 8. System responses

| 情况 | 系统响应 |
| --- | --- |
| 发布成功 | `template_version` 行持久化钉扎三件套；返回 `TemplateDetailView`（既有契约不变） |
| 发布时解析当前 revision 失败（master 已删/无 revision 行） | **fail-closed**，发布事务回滚，错误码 `api.error.master.currentRevisionUnavailable` |
| Runtime 装配时 `masterRevisionId` 为空（仅可能为未迁移完成或异常状态） | **fail-closed** `api.error.rendering.pinnedMasterUnavailable`；**禁止**回退 live master |
| Runtime 装配时钉扎 revision 行存在但对象存储 404 | **fail-closed** `api.error.rendering.pinnedMasterUnavailable`，记录告警 |
| 物理删除/软删 master revision 且被任一 PUBLISHED release 引用 | **409 fail-closed** `api.error.master.revisionInUseByPublishedRelease`，附引用 release 列表 |
| 物理删除 master revision 且无任何 PUBLISHED release 引用 | 通过（既有行为不变） |
| 停用（status 改）被钉扎的 master revision | 允许；**不取消**在途异步任务；阻止其被新发布钉扎（若发布时 current 指向已停用 revision → 发布 fail-closed） |
| 停用/弃用 release | 不解除 `master_revision_id` 引用；该 revision 仍不可物理删除 |
| Flyway 回填存量 PUBLISHED release | 写入 `master_revision_id` = 当前 `current_revision_line_id` + 计算 `master_file_hash` + `pinOrigin = PINNED_RETROACTIVELY` |
| Flyway 重复执行 | 不覆盖已存在钉扎值（幂等） |

---

## 9. 验收场景（Given / When / Then）

### A. 发布钉扎

#### BDD-CE-K01-001 — 发布写入 master_revision_id

**Given** 一个 template 处于 PENDING_RELEASE，其 master 的 `current_revision_line_id = R1`  
**When** Team Lead 调用 publish 发布为 release 1.0.0  
**Then** `template_version` 行的 `master_revision_id = R1`  
**And** `lifecycle_status = PUBLISHED`  
**And** 发布事务原子提交（钉扎与状态变更同生共死）

#### BDD-CE-K01-002 — 发布写入 master_file_hash（SHA-256）

**Given** 上述发布刚完成  
**When** 读取 `template_version` 行  
**Then** `master_file_hash` 为 64 字符十六进制 SHA-256  
**And** 该哈希等于发布时钉扎 revision 的 DOCX 字节流 SHA-256（独立重算可验证）

#### BDD-CE-K01-003 — 发布保留/写入 render profile 快照

**Given** 一个待发布 template  
**When** publish 完成  
**Then** `template_version.render_profile_json` 非空  
**And** `template_version.render_profile_version` 非空  
**And** Runtime 装配使用该快照解析 RenderProfile（既有 `RenderProfileService.resolveEffectiveProfile` 路径不变）

#### BDD-CE-K01-004 — pin_metadata_json 记录 pinOrigin=PUBLISHED

**Given** 上述发布刚完成  
**When** 读取 `template_version.pin_metadata_json`  
**Then** `pinOrigin = "PUBLISHED"`  
**And** `pinnedAt` 为 ISO-8601 UTC  
**And** `pinnedBy` 为发布者 username

#### BDD-CE-K01-005 — 发布时无法解析当前 revision → fail-closed

**Given** template 的 master 当前 `current_revision_line_id` 指向的 `master_revision_line` 行已被删除或不存在  
**When** 调用 publish  
**Then** 发布事务回滚  
**And** 返回错误码 `api.error.master.currentRevisionUnavailable`  
**And** 不写入任何钉扎字段，`lifecycle_status` 不变

---

### B. Runtime 装配使用钉扎 revision

#### BDD-CE-K01-006 — 生成读钉扎 revision 的 storage_key

**Given** release 1.0.0 已发布，`master_revision_id = R1`，其 `master_revision_line.storage_key = "masters/R1.docx"`  
**When** Runtime 调用 sync 生成 release 1.0.0  
**Then** `DocumentGenerationAssemblySupport` 读取的 master InputStream 来自 `"masters/R1.docx"`  
**And** **不**调用 `master.getStorageKey()`（live master 实体的 storageKey）

#### BDD-CE-K01-007 — 母版替换新 revision 后 release 产出不变

**Given** release 1.0.0 已发布并钉扎 R1  
**When** master designer 替换母版文件，`current_revision_line_id` 变为 R2，`master_revision_line` 新增 R2 行  
**And** 再次调用 release 1.0.0 生成  
**Then** Runtime 仍读 R1 的 storage_key  
**And** 产物的字体/页眉/锚点位置与首发一致（断言 DOCX 关键路径文本/属性，与金标护栏风格一致，**不**做像素比对）

#### BDD-CE-K01-008 — 钉扎 revision 缺失 → fail-closed

**Given** release 1.0.0 钉扎 R1，但 R1 的 `master_revision_line` 行被异常清除或对象存储对象 404  
**When** 调用 release 1.0.0 生成  
**Then** 返回 `api.error.rendering.pinnedMasterUnavailable`  
**And** **不**回退到 live master 的 storageKey  
**And** 审计/日志含 templateId / releaseVersion / masterRevisionId

#### BDD-CE-K01-009 — 三条生成路径统一走钉扎解析

**Given** release 1.0.0 已发布  
**When** 分别调用 sync、async、batch 生成  
**Then** 三条路径均通过同一钉扎解析入口得到 R1 storage_key  
**And** 无任一路径绕过钉扎读 live master

---

### C. 删除/停用保护（fail-closed）

#### BDD-CE-K01-010 — 物理删除被已发布 release 引用的 revision 被拒

**Given** release 1.0.0 钉扎 R1，`template_version.lifecycle_status = PUBLISHED`，`template_version.deleted_at IS NULL`  
**When** Master 管理员尝试物理删除 R1（硬删 `master_revision_line` 行或删除其对象存储对象）  
**Then** 返回 **409** `api.error.master.revisionInUseByPublishedRelease`  
**And** 错误体列出引用该 revision 的 PUBLISHED release（templateId + releaseVersion）  
**And** R1 行与对象存储对象保留

#### BDD-CE-K01-011 — 软删被已发布 release 引用的 revision 同样被拒

**Given** release 1.0.0 钉扎 R1  
**When** 尝试对 R1 设置 `master_revision_line.deleted_at`  
**Then** 返回 **409** `api.error.master.revisionInUseByPublishedRelease`  
**And** R1 的 `deleted_at` 保持为 NULL

#### BDD-CE-K01-012 — 无引用的 revision 可正常删除（无假阳性）

**Given** revision R3 存在，无任何 PUBLISHED release 钉扎 R3  
**When** Master 管理员删除 R3  
**Then** 删除成功（既有行为不变）  
**And** 不报 `revisionInUseByPublishedRelease`

#### BDD-CE-K01-013 — 停用/弃用 release 不解除引用

**Given** release 1.0.0 钉扎 R1，将 release 状态改为 STOPPED 或 DEPRECATED  
**When** 尝试物理删除 R1  
**Then** 仍返回 **409** `api.error.master.revisionInUseByPublishedRelease`  
**And** 钉扎引用关系不因 release 状态变化而解除

---

### D. D7 release-locked：在途异步任务

#### BDD-CE-K01-014 — 停用 master revision 不取消在途异步任务

**Given** 一个异步生成任务正在处理 release 1.0.0（已读取钉扎 R1 的 storage_key）  
**When** Master 管理员停用 R1（`status` 置为非 ACTIVE）  
**Then** 在途异步任务**不**被取消，继续按 R1 跑完  
**And** 任务最终产出与首发一致  
**And** 停用仅阻止 R1 被新发布 release 钉扎

#### BDD-CE-K01-015 — 发布时 current 指向已停用 revision 被拒

**Given** template 的 master `current_revision_line_id = R1` 且 R1 status 为非 ACTIVE（已停用）  
**When** 调用 publish  
**Then** 发布 fail-closed，错误码 `api.error.master.currentRevisionUnavailable`（或专用 `api.error.master.currentRevisionNotActive`）  
**And** 不写入钉扎字段

---

### E. 回填迁移（PINNED_RETROACTIVELY）

#### BDD-CE-K01-016 — Flyway 回填存量 PUBLISHED release

**Given** 升级前存在 `template_version` 行 `lifecycle_status = PUBLISHED` 且 `master_revision_id IS NULL`  
**When** Flyway 执行本片新增迁移  
**Then** 该行的 `master_revision_id` 被写入为其 `template.master_id` 的当前 `current_revision_line_id`  
**And** `master_file_hash` 被写入为该 revision DOCX 字节 SHA-256  
**And** `pin_metadata_json.pinOrigin = "PINNED_RETROACTIVELY"`  
**And** `execution-sync-ledger.md` 记录回填范围（受影响行数、迁移版本号）

#### BDD-CE-K01-017 — 回填迁移幂等

**Given** 某行已被回填写入 `master_revision_id = R1`  
**When** 同一迁移再次执行（如重新升级）  
**Then** 该行的 `master_revision_id`、`master_file_hash`、`pin_metadata_json` **不**被覆盖  
**And** 仅 `master_revision_id IS NULL` 的行才会被写入

#### BDD-CE-K01-018 — 回填后 runtime 行为与新发布一致

**Given** 存量 release 1.0.0 经回填后 `master_revision_id = R_legacy`  
**When** 调用 release 1.0.0 生成  
**Then** Runtime 读 R_legacy 的 storage_key（与 BDD-CE-K01-006 一致）  
**And** 替换母版后产物仍稳定（与 BDD-CE-K01-007 一致）  
**And** 审计可观察 `pinOrigin = PINNED_RETROACTIVELY`

#### BDD-CE-K01-019 — 回填时 master 已删 → 标记并报告，不静默

**Given** 存量 PUBLISHED release 对应的 `template.master_id` 已被删除或 `current_revision_line_id` 指向的 revision 行不存在  
**When** Flyway 回填迁移执行  
**Then** 迁移**不**为该行写入伪造 `master_revision_id`  
**And** 迁移日志/ledger 显式记录该异常行（templateId / releaseVersion）  
**And** 后续 Runtime 对该 release 的生成按 BDD-CE-K01-008 fail-closed

---

### F. 非目标边界（防止范围蔓延）

#### BDD-CE-K01-020 — 预览路径不钉扎（语义保留）

**Given** 任意 template 处于编辑/审批中  
**When** 调用预览接口  
**Then** 预览继续使用 dev 母版（live master 当前 storage_key）  
**And** **不**读 `template_version.master_revision_id`  
**And** 预览路径行为与发布前一致（计划卡"禁止：不动预览路径语义"）

#### BDD-CE-K01-021 — 锚点提取逻辑不变

**Given** 本切片交付完成  
**When** 对比锚点提取相关代码路径（master anchor 解析）  
**Then** 锚点提取行为与首发前一致（无新规则、无新字段）  
**And** K04/K05 的语义 diff 与影响分析属后续切片，本片不实现

#### BDD-CE-K01-022 — 不实现 CE-E01 自包含导出包 v2

**Given** 本切片交付完成  
**When** 检查导出包功能  
**Then** 本片**不**新增导出包内嵌母版/revision DOCX 的能力  
**And** 仅保证 DB 层钉扎字段可被 CE-E01 后续消费  
**And** CE-E01 行为规格见 [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md)（BDD ready 2026-07-16；Task **#78** **Done** merge `6ae57974` 已消费本片钉扎）

---

## 10. Boundary / exception（汇总）

| 边界 | 行为 |
| --- | --- |
| 发布时当前 revision 不可解析 | fail-closed，事务回滚 |
| Runtime 钉扎 revision 缺失 | fail-closed，禁止回退 live master |
| 物理删除/软删被 PUBLISHED release 引用的 revision | 409 fail-closed + 引用列表 |
| 无引用的 revision 删除 | 既有行为，无假阳性 |
| 停用 release（STOPPED/DEPRECATED） | 不解除引用，仍受删除保护 |
| 停用 master revision（D7 release-locked） | 不取消在途异步任务；阻止新发布钉扎 |
| 回填迁移重复执行 | 幂等，不覆盖已存在钉扎 |
| 回填时 master 已删 | 不伪造；显式记录；后续 Runtime fail-closed |
| 预览路径 | 不钉扎，跟随 dev 母版 |
| 锚点提取 | 本片不改 |
| 像素比对 | 不引入（与 K07 风格一致） |
| K04 语义 diff / K05 影响分析 / E01 导出包 | 本片不实现 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| Flyway 迁移 | 新增 `template_version` 列 `master_revision_id` / `master_file_hash` / `pin_metadata_json` + 回填脚本 |
| 发布回归测试 | publish → 断言三件套字段；母版替换 → 重放生成 → 断言 storage key 为钉扎 revision |
| Runtime 装配测试 | mock `master_revision_line` 与对象存储；断言读钉扎 storage_key 而非 live master；缺失场景 fail-closed |
| 删除保护测试 | 构造 PUBLISHED 引用 → 删除 revision → 断言 409 + 引用列表；无引用场景删除成功 |
| 回填迁移测试 | 存量 PUBLISHED 行 → 跑迁移 → 断言字段写入 + `PINNED_RETROACTIVELY`；重复执行幂等 |
| 在途异步任务测试（D7） | 构造进行中 async 任务 → 停用 revision → 断言任务完成 + 产物稳定 |
| Ledger 记录 | `docs/plan/execution-sync-ledger.md` 增 CE-K01 证据行（迁移版本、回填行数、绿门禁） |
| 非目标 | 无前端 E2E；无 Docker 部署门禁硬依赖（除非删除保护接口走 Docker 验收栈） |

---

## 12. Traceability

| 来源 | 关系 |
| --- | --- |
| [CE-K01 计划条目](../plan/core-excellence-program-2026-07.md) | 本片直接交付其"目标行为"每一条 |
| [ADR-0019 结构化创作与渲染边界](../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) | 已声明"已发布模板锁定发布时使用的母版样式与引用"——本片将 master revision 维度对齐该 ADR |
| 用户拍板 D1–D7（2026-07-14） | D7 release-locked 直接驱动 BDD-CE-K01-014/015 |
| Task Master **#57** | 本切片任务登记 |
| CE-K07 金标骨架 | 本片交付时按 K07 约定充实 `nested-clauses` 等样本的母版 revision 钉扎断言（若可行） |
| CE-K04 / CE-K05 / CE-E01 | 依赖本片钉扎数据；本片不实现它们的能力。**CE-E01 消费面（2026-07-16）：** Task Master **#78** · [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md) — 导出自包含 v2 读取 `master_revision_id` / `master_file_hash` / render profile；导入 dry-run 比对 `masterPin.masterFileHash` |
| `permission-matrix.md` | 发布与母版删除权限角色不变（本片不改权限模型） |

---

## 13. Explicit non-goals（本片）

- 不改锚点提取逻辑、不改预览路径语义。
- 不实现 K04 语义级 diff、K05 真实影响分析、K02 母版样式权威。
- 不实现 CE-E01 自包含导出包 v2（仅保证 DB 字段可被其消费）。
- 不引入像素/视觉回归。
- 不强制前端 E2E / UIUX（无管理 UI 用户可见变更；删除保护错误码若前端有渲染则由前端片处理）。
- 不宣称生产 go-live；不激活 CD-3；不发明正式 plan phase。
- 不改 master revision 的 `revision_sequence` 编号策略。
- 不改既有 render profile lockForPublish 语义（仅断言其行为）。

---

## 14. Open questions（非阻塞）

下列问题 **不阻塞** `bdd_readiness: ready`；实现阶段由工程师按默认建议执行，若需改默认再回写本文件。

| # | 问题 | 默认建议（本片可采用） |
| --- | --- | --- |
| Q1 | `pin_metadata_json` 是否额外存储 `renderProfileSnapshotHash`？ | **否**；render profile 快照已由 `render_profile_json` 列承载，本片不重复 |
| Q2 | 删除保护接口是否对 Master 管理员返回引用 release 的 releaseVersion 明文？ | **是**；最小必要信息便于运维定位，权限沿用既有 MASTER_ADMIN |
| Q3 | 回填迁移是否拆为单独 Flyway 版本与列添加迁移分开？ | **建议**：列添加 + 数据回填同一迁移版本（保证原子性），版本号由 backend-engineer 按现有 ratchet 选定 |
| Q4 | `master_file_hash` 是否在每次生成时重算并与持久化值比对（drift detection）？ | **本片不实现** drift detection；仅持久化供审计。Drift 检测由 **CE-G06** 落地（再生前比对 invocation.`release_bundle_hash`；见 [ce-g06-audit-reproducible.md](./ce-g06-audit-reproducible.md) G06-C11 / BDD-CE-G06-014） |
| Q5 | 发布时若 master 当前 revision 的对象存储对象已 404（罕见），是否阻止发布？ | **是**；与 BDD-CE-K01-005 一致，`api.error.master.currentRevisionUnavailable` |

---

## 15. BDD readiness

```
bdd_readiness: ready
acceptance_scenario_count: 22
open_questions: [Q1, Q2, Q3, Q4, Q5]  # non-blocking defaults above
owning_doc: docs/behavior/ce-k01-release-bundle-pinning.md
task_ids: [CE-K01, ce-k01-release-bundle-pinning, "#57"]
next: plan-orchestrator → backend-engineer (TDD Red on Flyway + publish + assembly + delete protection + backfill)
```

**Handoff：** 规格已就绪；可进入计划拆解与实现。实现方必须以失败测试先行覆盖 BDD-CE-K01-001…022，再写最小代码使发布钉扎、runtime 装配读钉扎 revision、删除保护 fail-closed、回填迁移幂等四条主线绿灯。门禁：`mvn -B -ntp -f backend/pom.xml verify` 全绿；遵守计划卡"禁止"栏与 K01-C12 中列出的非目标。
