# 权限矩阵

## 1. 文档目的

本文档用于整理金融信函低代码文档生成平台的角色、分组、对象权限和权限边界。当前内容仅基于已确认需求，不自行扩展未确认权限。

> **SYS-NORM Wave 5 — Six-role catalog (Done 2026-07-21, merge `febb95b3`):**
> Management assignable catalog is **six roles** per
> [ADR-0070 Accepted](../adr/authorization-security/0070-role-compression-six-roles.md) and Wave 5 BDD
> **ready**/delivered [sys-norm-roles.md](../behavior/sys-norm-roles.md) (**BDD-SYS-NORM-ROLE-001…018**).
> This file’s §3 / §13 tables are the **Confirmed permission SoT**. Production `ManagementRole` /
> Flyway / FE enums landed with TM **#149** → **Done**. `DOCUMENT_AUTHOR` L1 EN/ZH
> display labels remain **Pending** finalize (non-blocking; interim FE copy OK per ROLE-013).
>
> **Migration remap (locked):** `TEMPLATE_APPROVER` → `GROUP_ADMIN` (privilege accept);
> `MASTER_DESIGNER` ∪ `TEMPLATE_AUTHOR` → `DOCUMENT_AUTHOR` (capability union). Retired codes are
> **not** assignable — fail-closed **422** `ROLE_NOT_ASSIGNABLE` (ROLE-005).
>
> **ADR-0071 / SYS-NORM Wave 6 (D1 — Done 2026-07-21; TM #150 `64b0a650`):** DocumentBrand /
> LegalEntity **catalog read/write product surfaces are retired** — no management nav, no
> catalog CRUD permission rows as live product capabilities, **no new role bits**. Logo / seal /
> letterhead legal presentation are governed via **Letterhead (master)** only. **Legal holds**
> remain (unchanged). Shell `REDBC`/`GREENBC` stay UI-only (orthogonal). Wave 1 nav hide
> (**#145**) remains; Wave 6 hard-retires APIs/routes/runtime
> ([sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md) **BDD-SYS-NORM-D1-001…020**).
> Do **not** flip **#3b/#5a**; do **not** mark **#53** Done.

## 相关文档

- [文档索引](../README.md)
- [原始需求记录](../requirements/requirements-plan.md)
- [产品需求说明](../product/PRD.md)
- [领域模型](../domain/domain-model.md)
- [文档治理规则](../governance.md)
- [ADR-0070 Role compression (six roles)](../adr/authorization-security/0070-role-compression-six-roles.md)（Accepted — Wave 5 impl **Done** `febb95b3`）
- [SYS-NORM Wave 5 roles BDD](../behavior/sys-norm-roles.md)（**ready**/delivered — **BDD-SYS-NORM-ROLE-001…018**）
- [System Normalization program](../plan/system-normalization-program-2026-07.md)（Waves **0–6 Done** `#150` `64b0a650`; Waves **7–8 Not Started**）
- [ADR-0071 Retire document brand / legal entity surfaces](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)（Accepted — Wave 1 nav hide Done `#145`; Wave 6 runtime SoT [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md)）
- [SYS-NORM Wave 6 D1 brands BDD](../behavior/sys-norm-d1-brands.md)（**ready/Done** — **BDD-SYS-NORM-D1-001…020**；TM **#150** `64b0a650`）
- [ADR-0048 Audit Data Retention & Archival Policy](../adr/operations/0048-audit-data-retention-policy.md)（Accepted — Tier-1 90/365）
- [LR-D1 行为规格](../behavior/lrp-d1-audit-retention.md)
- [CE-G04 Legal hold 行为规格](../behavior/ce-g04-legal-hold.md)（BDD-CE-G04；#75 — retention 豁免叠加，不改 ADR-0040/0048 正文）
- [CE-G05 模板年检 + 条款正文全文检索](../behavior/ce-g05-annual-review-fts.md)（BDD-CE-G05；#77 — 无新 capability；复用 `authorTemplates` / §5.1 目录浏览）
- [PRR-D01c Dashboard summary API](../behavior/prod-dashboard-summary-api.md)（BDD-PRR-D01C；#136 — 无新 capability；会话认证 + catalog 同款 group-scope；§13.1.3）
- [IBL-E3 法务→合规审批矩阵](../behavior/ibl-e3-legal-approval-matrix.md)（BDD-IBL-E3；#130 — `LEGAL_REVIEWER` + `decideLegalApprovals`；COMPLIANCE / 单级正常路径 = `GROUP_ADMIN`（吸收原 `TEMPLATE_APPROVER`）；[ADR-0064 Accepted](../adr/template-lifecycle/0064-legal-compliance-approval-matrix.md)）
- [IBL-E4 法人实体文档品牌变体](../behavior/ibl-e4-entity-document-brands.md)（historical IBL-E4；产品面由 ADR-0071 / Wave 6 退役；**无新角色**）
- [IBL-E5 effectiveFrom 发布门禁 + bulk re-pin](../behavior/ibl-e5-effectivefrom-bulk-repin.md)（BDD-IBL-E5；#132 — **无新角色 / capability**；bulk-repin 与发布门禁评估复用 `authorTemplates`；[ADR-0066 Accepted](../adr/template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md)）
- [IBL-E6 条款嵌套模块图治理](../behavior/ibl-e6-clause-nesting-governance.md)（BDD-IBL-E6；#133 — **无新角色 / capability**；结构写复用 `authorContentModules`；深度 where-used 复用 §5.1 目录浏览；发布嵌套硬项复用既有模板编排边界；[ADR-0067 Accepted](../adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md)）

## 2. 权限设计原则

已确认原则：

- 后台治理与管理界面用户必须先通过已确认的管理登录能力建立已认证会话，再进入角色与分组范围内的管理功能。
- 本迭代后台治理与管理界面采用本地管理账户认证；未来公司 SSO 集成保留为扩展方向，不作为本次已交付能力。
- 管理登录密码只允许以密码哈希形式持久化，不允许保存或展示明文密码。
- 全局管理员拥有系统最大权限。
- 管理员范围包括全局管理员和分组管理员。
- 审计管理员可查看全部审计记录。
- 分组管理员的管理范围由显式授权的一个或多个组决定，不要求等同于用户自身所属组；分组管理员仅在被授权组范围内拥有管理员权限。
- **文档作者**（`DOCUMENT_AUTHOR`）需要分组；承担原母版设计与模板编排的能力并集（letterhead + template + clause authoring per matrix）。
- 母版本身需要严格按组隔离。
- 模板本身需要分组隔离。
- 模板只能在所属或被授权组范围内使用和维护。
- 不同组之间不允许复用模板。
- 分组隔离采用混合隔离模型。
- 当前确认的分组维度包括业务条线、部门/团队。
- 用户可以属于多个组，并在所属或被授权组范围内访问多个组的模板/母版。
- 测试人员按分组配置；正常 `decideTests` 仅测试人员（+管理员）持有（SoD）。
- **合规 / 单级审批**正常路径由分组管理员在被授权组范围内承担（吸收原 `TEMPLATE_APPROVER`；特权扩展已接受）。法务审阅人按分组配置，仅多级 LEGAL 阶段。
- API 采用 API 凭证 + AD Group 双重认证授权。
- API 管理由全局管理员和分组管理员承担，不设置独立 API 管理员角色。
- 全局管理员可管理全部 API 管理配置；分组管理员只能管理被授权组范围内的 API 管理配置。

## 3. 角色清单

> **Assignable management catalog = exactly six roles** ([ADR-0070](../adr/authorization-security/0070-role-compression-six-roles.md);
> [sys-norm-roles.md](../behavior/sys-norm-roles.md) ROLE-001…018).
> Non-management identity **API 调用方** remains outside the six-role assignable catalog.

| 角色 | 角色标识 | 已确认说明 |
| --- | --- | --- |
| 全局管理员 | `GLOBAL_ADMIN` | 拥有系统最大权限；可分配全部六个管理角色；可改写种子。 |
| 分组管理员 | `GROUP_ADMIN` | 管理范围由显式授权组决定；在被授权组内拥有管理员权限；**吸收**原 `TEMPLATE_APPROVER` 的合规/单级审批与 SEAL 上传等特权（特权扩展已接受）；可分配运营类角色 `DOCUMENT_AUTHOR` / `TEMPLATE_TESTER` / `LEGAL_REVIEWER`。 |
| 文档作者 | `DOCUMENT_AUTHOR` | 原 `MASTER_DESIGNER` ∪ `TEMPLATE_AUTHOR` 能力并集：letterhead/母版作者 + 模板编排（+ 条款/内容模块作者，按本矩阵）。**不**因合并获得 `decideTests`、正常 `decideApprovals`、`reviewMasters`、例外干预或 `publishTemplates`（纯作者仍只提交待发布，管理员发布 — Batch B / BDD §5.2）。L1 EN/ZH **显示名 Pending**（interim i18n OK）。 |
| 测试人员 | `TEMPLATE_TESTER` | 按分组配置；只执行测试通过/不通过判定（正常 `decideTests`）；不获得额外母版/模板编辑权限。SoD：不得并入文档作者。 |
| 法务审阅人 | `LEGAL_REVIEWER` | 按分组配置；只执行多级模式下 **LEGAL** 阶段判定（`decideLegalApprovals`）；不获得额外母版/模板编辑权限；不得代批 COMPLIANCE / `SINGLE_TRACK`（403）。压缩不改动本角色（ADR-0064）。 |
| 审计管理员 | `AUDIT_ADMIN` | 可查看全部审计记录；压缩不改动本角色。 |
| API 调用方 | （非管理目录） | 通过 API 凭证 + AD Group 授权；只调用已授权 API；不获得后台母版/模板操作权限、完整审计查看权限或 API 凭证自助管理权限。 |

### 3.1 退役角色（不可分配）

| 退役角色标识 | 迁移目标 | 赋值 API |
| --- | --- | --- |
| `TEMPLATE_APPROVER` | `GROUP_ADMIN` | **422** `ROLE_NOT_ASSIGNABLE`（fail-closed；不静默映射） |
| `MASTER_DESIGNER` | `DOCUMENT_AUTHOR` | 同上 |
| `TEMPLATE_AUTHOR` | `DOCUMENT_AUTHOR` | 同上 |

未知管理角色 token 同样 fail-closed（ROLE-005）。与分组管理员提权拒绝 **403** `ROLE_ASSIGNMENT_NOT_ALLOWED` 区分。

## 4. 母版权限矩阵

| 操作 | 全局管理员 | 分组管理员 | 文档作者 | 测试人员 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 上传/创建 DOCX 母版 | 是 | 被授权组范围内 | 是 | 否 | 文档作者承担原母版设计创建权；非主责角色不获得额外母版编辑权限。 |
| 维护锚点 | 是 | 被授权组范围内 | 是 | 否 | 锚点由文档作者在 DOCX 中预先定义。 |
| 修改基础版式 | 是 | 被授权组范围内 | 是 | 否 | 文档作者负责基础版式维护。 |
| 提交母版审核 | 是 | 被授权组范围内 | 是 | 否 | 提交前必须执行锚点完整性校验并填写变更说明，校验失败不得提交审核。 |
| 审核母版 | 是 | 被授权组范围内 | 否 | 否 | 母版审核由管理员执行（`reviewMasters`）；审核通过后母版才允许被新建或更新模板引用。 |
| 母版审核、变更和影响分析审计 | 是 | 被授权组范围内 | 否 | 否 | 其他业务角色不因自身角色获得审计查看权限。 |
| 删除母版 | 是 | 被授权组范围内 | 是 | 否 | 文档作者可删除母版（原设计师权）。 |
| 更新母版基础信息 | 是 | 被授权组范围内 | 是 | 否 | 文档作者可更新母版基础信息。 |

## 5. 模板权限矩阵

| 操作 | 全局管理员 | 分组管理员 | 文档作者 | 测试人员 | 法务审阅人 | 说明 |
| --- | --- | --- | --- | --- | --- | --- |
| 选择母版创建模板 | 是 | 被授权组范围内 | 是 | 否 | 否 | 只能选择审核通过的母版创建或更新模板；文档作者 = 原设计师∪编排者创建权。 |
| 配置锚点内容 | 是 | 被授权组范围内 | 是 | 否 | 否 | 粘贴清洗 `POST .../paste-clean` 与绑定 upsert/validate 携带非敏感 `pasteCleaningEvidence` **复用本行权限**（`authorTemplates`），**不**新增独立端点或角色位（ops-paste-binding-seam）。 |
| 配置模板变量 | 是 | 被授权组范围内 | 是 | 否 | 否 | 可选 `piiCategory`（CE-G03）读写**复用本行**，不新增角色位。 |
| 配置条件/循环规则 | 是 | 被授权组范围内 | 是 | 否 | 否 | **IBL-E2 / ADR-0063：** Composition Inclusion Rules **复用本行** / `authorTemplates`；**无**新角色或 capability bit。 |
| 维护模板测试数据集 | 是 | 被授权组范围内 | 是 | 否 | 否 | 全局管理员、分组管理员、文档作者可按范围创建/编辑/复制/派生/删除；测试人员与法务审阅人只能在材料中查看和判定。CE-G03：`SYNTHETIC` / `EXPLICIT_SENSITIVE` 闸门**复用本行维护权**。 |
| 维护测试/审批意见模板和风险提示文案 | 是 | 被授权组范围内（模板级覆盖） | 否 | 否 | 否 | 全局管理员维护全局默认；分组管理员在模板创建/详情中维护可选模板级覆盖；配置变更必须记录审计。 |
| 查看协作待办和状态提示 | 是 | 被授权组范围内 | 是 | 是 | 是 | 文档作者查看提交/整改相关待办；测试人员查看测试队列；分组管理员查看合规/单级审批队列与超时升级；法务审阅人查看 LEGAL 队列（§5.2 / §13.1.2）。待办展示不授予额外编辑/判定/发布权限。 |
| 维护协作待办超时阈值 | 是 | 被授权组范围内 | 否 | 否 | 否 | 全局管理员维护全局默认，分组管理员维护组级覆盖。 |
| 测试生成 DOCX/PDF | 是 | 被授权组范围内 | 是 | 否 | 否 | 测试人员只执行测试通过/不通过判定。 |
| 查看生成预览和变更差异摘要 | 是 | 被授权组范围内 | 是 | 是 | 是 | 测试人员、法务审阅人、分组管理员（审批材料）可在其分组范围内查看，不获得额外模板编辑权限。 |
| 提交测试 | 是 | 被授权组范围内 | 是 | 否 | 否 | 仅草稿或测试通过状态可提交测试；测试人员只执行测试判定。 |
| 测试通过 | 是 | 例外干预，被授权组范围内 | 否 | 是 | 否 | 正常判定由测试人员执行；分组管理员仅例外干预（原因+二次确认+单独审计标记）。**文档作者无 `decideTests`（SoD）。** |
| 测试不通过 | 是 | 例外干预，被授权组范围内 | 否 | 是 | 否 | 同上；测试不通过后回到草稿。 |
| 提交审批 | 是 | 被授权组范围内 | 是 | 否 | 否 | 测试通过后手动提交审批；材料含测试记录、覆盖率、预览、差异与发布前检查清单等。 |
| 审批通过（合规/单级） | 是 | 被授权组范围内（正常判定；同人例外干预仍须二次确认） | 否 | 否 | 否 | **`SINGLE_TRACK`：** 一级审批由 `GROUP_ADMIN`（+`GLOBAL_ADMIN`）判定 → 待发布。**`LEGAL_THEN_COMPLIANCE`：** 本行指 **COMPLIANCE** 阶段（`PENDING_COMPLIANCE_DECISION`）；LEGAL 见 §5.2。原 `TEMPLATE_APPROVER` 已吸收。纯 `LEGAL_REVIEWER` / 纯 `DOCUMENT_AUTHOR` / 纯 `TEMPLATE_TESTER` → 403。 |
| 审批不通过（合规/单级） | 是 | 被授权组范围内（正常判定；同人例外干预仍须二次确认） | 否 | 否 | 否 | 同上分轨；拒绝后回到草稿。LEGAL 阶段拒绝见 §5.2。 |
| 发布模板 | 是 | 被授权组范围内 | 否 | 否 | 否 | `publishTemplates` = 仅管理员（Batch B / BDD §5.2）。文档作者提交待发布后由管理员发布；待发布状态下发布前必须执行检查清单与二次确认。 |
| 停用模板 | 是 | 被授权组范围内 | 是 | 否 | 否 | 文档作者可停用自己负责范围内模板（原编排者权）；模板停用后所有版本停用。 |
| 恢复模板 | 是 | 被授权组范围内 | 否 | 否 | 否 | 仅管理员；恢复前需要影响预览、二次确认和审计。 |
| 废弃模板 | 是 | 被授权组范围内 | 否 | 否 | 否 | 仅管理员；废弃前必须先停用且无可调用发布版本。 |
| 停用版本 | 是 | 被授权组范围内 | 否 | 否 | 否 | 仅管理员。 |
| 恢复版本 | 是 | 被授权组范围内 | 否 | 否 | 否 | 仅管理员。 |
| 导出模板 | 是 | 被授权组范围内 | 自己负责的模板 | 否 | 否 | `exportTemplates` 含文档作者（原编排者导出权）；**CE-E01/E03** 与 **SYS-NORM Wave 7** 晋级包导出（`dependencyClosure=PROMOTION`）共用本行；无新权限码。 |
| 导入模板 | 是 | 被授权组范围内 | 自己负责的模板 | 否 | 否 | 导入生产后从草稿阶段重新走流程；遇相同模板 ID 时保留 ID 并创建新开发版本。含 CE-E01 / Wave 7 **dry-run**（`dryRun=true`）与提交导入；管理端 Import dry-run UI 同权；无新权限码。`TEMPLATE_TESTER` / `LEGAL_REVIEWER` / `AUDIT_ADMIN`（仅该角色）→ 403 / UI 隐藏。 |
| 删除模板 | 是 | 否 | 否 | 否 | 否 | 普通模板删除仅由全局管理员执行。 |
| 更新模板基础信息 | 是 | 被授权组范围内 | 否 | 否 | 否 | 普通模板基础信息更新由管理员执行。 |
| 完成模板年检 / 查看年到期待办（CE-G05） | 是 | 被授权组范围内 | 是 | 否 | 否 | **无新 capability bit。** 复用 `authorTemplates`。测试人员 / 法务审阅人默认不可见待办、不可 complete（403）。行为：[ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md)。 |
| 配置审批矩阵模式 `approvalMatrixMode`（IBL-E3） | 是 | 被授权组范围内 | 是 | 否 | 否 | 包级 `SINGLE_TRACK` \| `LEGAL_THEN_COMPLIANCE`；仅 `DRAFT` 或 `APPROVAL`+`PENDING_SUBMIT` 可写。测试/法务角色无配置权。多级阶段判定见 §5.2。 |
| 配置模板文档品牌 allow-list `allowedDocumentBrandCodes`（IBL-E4 → **retired Wave 6**） | — | — | — | — | — | **产品面退役（ADR-0071 / D1）。** Generate 不再按 allow-list 门禁；FE 不再提供品牌 allow-list 编辑器；管理写 fail-closed 或 strip（见 §5.3 / OpenAPI）。**无新角色 / capability。** |
| 批量改钉内容模块引用 bulk-repin（IBL-E5） | 是 | 被授权组范围内 | 是 | 否 | 否 | **无新 capability bit。** 复用 `authorTemplates`；测试/法务角色无调用权。 |

### 5.1 条款或内容模块权限矩阵

条款或内容模块不新增专门维护角色，复用六角色后台体系。条款或内容模块默认按所属组隔离；全局管理员和分组管理员可配置授权组共享范围；模板只能引用同组或已授权共享范围内已批准且未停用、未废弃的具体模块版本。

条款或内容模块停用或废弃后，已发布且已锁定该模块版本的模板仍按发布时锁定内容生成；模块停用或废弃只阻止后续新的模板发布候选引用该模块版本。需要立即停止使用包含问题模块的已发布模板时，必须通过停用对应模板或发布版本来阻断生成和 API 调用。

| 操作 | 全局管理员 | 分组管理员 | 文档作者 | 测试人员 | 法务审阅人 | 说明 |
| --- | --- | --- | --- | --- | --- | --- |
| 创建条款或内容模块 | 是 | 被授权组范围内 | 所属或被授权组范围内 | 否 | 否 | 创建后进入草稿；创建人不获得越组访问权限。 |
| 编辑草稿或创建新版本 | 是 | 被授权组范围内 | 所属或被授权组范围内 | 否 | 否 | 已批准版本不直接修改；内容变更需要创建新版本。 |
| 提交模块审批 | 是 | 被授权组范围内 | 所属或被授权组范围内 | 否 | 否 | 提交审批后进入待审批；提交材料需要包含变更说明、适用范围和影响摘要。 |
| 审批通过 | 是 | 被授权组范围内（正常判定；同人例外干预仍须二次确认） | 否 | 否 | 否 | 正常审批判定由分组管理员执行（吸收原审批人员）；`decideContentModuleReviews` = GLOBAL/GROUP。 |
| 审批不通过 | 是 | 被授权组范围内（正常判定；同人例外干预仍须二次确认） | 否 | 否 | 否 | 审批不通过必须填写退回原因；不通过后该模块版本回到草稿。 |
| 引用模块版本 | 是 | 被授权组范围内 | 所属或被授权组范围内 | 否 | 否 | 只能引用可访问范围内已批准且未停用、未废弃的具体模块版本；测试人员与法务审阅人不得在模板编排中新增或变更模块引用。 |
| 查看被引用模块内容（测试/审批材料） | 是 | 被授权组范围内 | 是 | 是 | 是 | 测试人员、法务审阅人、分组管理员只在测试/审批材料、生成预览中只读查看已被引用的模块版本。 |
| 配置共享范围 | 是 | 被授权组范围内 | 否 | 否 | 否 | 默认同组隔离；共享范围变更需要影响分析、二次确认和审计。 |
| 停用模块或版本 | 是 | 被授权组范围内 | 否 | 否 | 否 | 停用由管理员执行；执行前必须影响分析、二次确认并记录审计。 |
| 恢复模块或版本 | 是 | 被授权组范围内 | 否 | 否 | 否 | 恢复由管理员执行。 |
| 废弃模块或版本 | 是 | 被授权组范围内 | 否 | 否 | 否 | 废弃由管理员执行。 |
| 导出模块 | 是 | 被授权组范围内 | 所属或被授权组范围内 | 否 | 否 | 导出范围不得越过角色、分组和对象访问范围。 |
| 查看模块审计 | 是 | 被授权组范围内 | 否 | 否 | 否 | 模块审计查看沿用后台审计查看边界；审计管理员可按审计权限查看全部审计记录。 |
| 浏览目录 / 正文全文检索 / where-used（CE-G05 / IBL-E6） | 是 | 被授权组范围内 | 是 | 否 | 否 | **无新 capability bit。** 浏览边界：`GLOBAL_ADMIN` / `GROUP_ADMIN` / `DOCUMENT_AUTHOR`（原设计师∪编排者∪原审批浏览权由 GROUP 吸收）。`TEMPLATE_TESTER` **无**目录浏览 → **403**。跨组仅共享/授权可见模块；where-used **不得**返回调用方不可见模板。行为：[ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md)；[ibl-e6-clause-nesting-governance.md](../behavior/ibl-e6-clause-nesting-governance.md)。 |

### 5.2 IBL-E3 法务→合规多级审批（ADR-0064）

**已确认（2026-07-20 / PD-8 / BDD-IBL-E3；Wave 5 角色压缩修正 2026-07-21）：** 模板生命周期审批轨可配置多级矩阵；母版审核与条款独立审批轨**保持单级**。法务元数据（CE-K08）**仍可选**。Accepted ADR ≠ impl Done；**不**翻转 #3b/#5a。

| 操作 | 全局管理员 | 分组管理员 | 法务审阅人 (`LEGAL_REVIEWER`) | 说明 |
| --- | --- | --- | --- | --- |
| LEGAL 阶段通过/不通过（`PENDING_LEGAL_DECISION`） | 是（正常判定，组范围） | 是（正常判定，被授权组） | 是（正常判定，组内） | 通过 → `PENDING_COMPLIANCE_DECISION`；拒绝 → `DRAFT`。须结构化意见 + 保真已查看；CE-G01 同人阻断每阶段生效；审计含 `approvalStage=LEGAL`。纯合规路径角色不得代批 LEGAL（除非兼 `LEGAL_REVIEWER`）。 |
| COMPLIANCE 阶段通过/不通过（`PENDING_COMPLIANCE_DECISION`） | 是 | 是 | **否**（403，除非兼管理员） | 通过 → `PENDING_RELEASE`；拒绝 → `DRAFT`。正常合规判定 = `GROUP_ADMIN`（吸收原 `TEMPLATE_APPROVER`）。审计含 `approvalStage=COMPLIANCE`。 |
| `SINGLE_TRACK` 一级审批判定（`PENDING_DECISION`） | 是 | 是 | **否**（403） | 与 ADR-0021 兼容；不得跳过 LEGAL「一键双批」。 |
| 错阶段 / 跳级 | — | — | — | 错阶段 payload → **409/422** `APPROVAL_STAGE_MISMATCH`；跳过 LEGAL → 4xx；状态不变。 |

Capability：`decideLegalApprovals` = {`GLOBAL_ADMIN`,`GROUP_ADMIN`,`LEGAL_REVIEWER`}；`decideApprovals` = {`GLOBAL_ADMIN`,`GROUP_ADMIN`}（COMPLIANCE / 单级；**无**独立 `TEMPLATE_APPROVER`）。行为 SoT：[ibl-e3-legal-approval-matrix.md](../behavior/ibl-e3-legal-approval-matrix.md)；角色压缩：[sys-norm-roles.md](../behavior/sys-norm-roles.md)。

### 5.3 DocumentBrand / LegalEntity 产品面退役（ADR-0071 / Wave 6）与历史 IBL-E4

**已确认（2026-07-21 / ADR-0071 Accepted / Wave 6 BDD ready）：** DocumentBrand 与 LegalEntity
**不再**是所需产品目录（nav / 管理 API / runtime 目录依赖）。**无新角色**、**无新 capability bit**。
Logo / seal / 信头法定呈现资产由 **Letterhead（master）** 治理。壳层 `REDBC`/`GREENBC` 主题切换权不变（UI-only）。
**Legal holds** 权限与行为**保持**（见 CE-G04 / Security nav）。**不**翻转 #3b/#5a；**不**将 #53 标 Done。

| 操作 | 全局管理员 | 分组管理员 | 文档作者 | 测试/法务 | 说明 |
| --- | --- | --- | --- | --- | --- |
| DocumentBrand 目录 list/get/create/update | **否**（表面退役） | **否** | **否** | **否** | 管理 API **404/410** + `DOCUMENT_BRAND_SURFACE_RETIRED`；无可用目录载荷。历史 IBL-E4 证据见 ADR-0065。 |
| LegalEntity 目录 list/get/create/update | **否**（表面退役） | **否** | **否** | **否** | 管理 API **404/410** + `LEGAL_ENTITY_SURFACE_RETIRED`；不得再持久化实体↔品牌绑定。 |
| 组 `defaultLegalEntityCode` 读写 | **否**（表面退役） | **否** | **否** | **否** | 同 LegalEntity 退役家族码；不得再作为 DocumentBrand 回落配置面。 |
| 模板 `allowedDocumentBrandCodes` 配置 | **否**（产品编辑退役） | **否** | **否** | **否** | Generate **忽略**历史 allow-list；FE 编辑器移除；写 fail-closed 或 strip（OpenAPI 对齐）。 |
| Letterhead（master）logo / seal 治理 | 是 | 被授权组范围内 | 所属/授权组（既有母版写边界） | 否 | **D1 后唯一**信头/logo/seal 产品治理路径；复用既有 master 权限行，**不**新增 capability。 |
| Legal holds 创建/查看/释放 | 是 | 被授权组范围内 | 否 | 否（除非兼管理员） | **Keep** — 不在 D1 退役范围；见 CE-G04 / §Security。 |
| Runtime/preview `context.legalEntityCode` | — | — | — | — | 白名单可选不透明字段（ADR-0013）；**不**驱动目录解析；**不**产生退役目录 422（`LEGAL_ENTITY_*` / `DOCUMENT_BRAND_*` catalog 族）。非目录写权限。 |
| 壳层 `REDBC`/`GREENBC` 主题切换 | 是（已认证会话） | 是 | 是 | 是 | UI-only chrome；**不是** DocumentBrand MDM。 |

**Historical (IBL-E4 Done — not ongoing product requirement):** [ibl-e4-entity-document-brands.md](../behavior/ibl-e4-entity-document-brands.md)；[ADR-0065](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md)（Decision 正文保留；产品面由 ADR-0071 取代）。  
**Wave 6 SoT:** [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md)；决策：[ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)。

## 6. API 权限矩阵

| 操作 | API 凭证 | AD Group | 模板授权 | 发布版本 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 调用模板生成 API | 必须有效 | 访问账号需命中 API 管理中配置的 AD Group | 模板级别授权 | 显式发布版本路径或 default 路径解析出的目标发布版本 | API 采用 API 凭证 + AD Group 双重认证授权；default 路径必须由 API 管理显式配置到某个未停用发布版本；所有文档生成类 API 必须传入 `requestId` 和 `idempotencyKey`。 |
| 调用 DOCX 输出 | 必须有效 | 必须校验 | 模板级别授权 | 显式发布版本路径或 default 路径解析出的目标发布版本 | 不按输出格式单独控制权限；调用方获得模板权限后，可以按 API 管理配置生成 DOCX。DOCX 支持根据 API 参数动态加密。 |
| 调用 PDF 输出 | 必须有效 | 必须校验 | 模板级别授权 | 显式发布版本路径或 default 路径解析出的目标发布版本 | 不按输出格式单独控制权限；调用方获得模板权限后，可以按 API 管理配置生成 PDF。PDF 支持根据 API 参数动态加密。 |
| 调用加密输出 | 必须有效 | 必须校验 | 模板级别授权 | 显式发布版本路径或 default 路径解析出的目标发布版本 | DOCX/PDF 动态加密能力必须受 API 管理配置控制；API 可以直接传入密码；密码不落库、不进日志，只在本次生成过程中使用；加密参数采用 enabled、openPassword、ownerPassword、permissions 标准模型；`enabled=true` 时 `openPassword` 必填、`ownerPassword` 可选；`permissions` 采用统一抽象权限枚举且 **v1 仅对 PDF 映射生效**（CE-C06）；DOCX + 非空 `permissions` 结构合法时成功并警告 `DOCX_PERMISSIONS_NOT_APPLIED`（`messageKey=generation.warning.fidelity.docxPermissionsNotApplied`；不 400）；传入 `permissions` 时必须同时传入 `ownerPassword`；密码最少 12 字符、最长 128 字符，open/owner 同时传入时必须不同；加密参数错误返回 `400 ENCRYPTION_PARAMETER_INVALID`；加密处理失败返回 `500 ENCRYPTION_FAILED` 且 `retryable=true`；加密策略摘要记录是否启用加密、输出格式、是否传入 openPassword、是否传入 ownerPassword、permissions 权限摘要。 |
| 批量生成 | 必须有效 | 必须校验 | 模板级别授权 | 显式发布版本路径或 default 路径解析出的目标发布版本 | 已确认支持批量生成；支持批次级统一输出和加密配置，也允许单笔覆盖；每笔记录必须传入同批唯一的 `items[].itemId`；重复 `items[].itemId` 返回 `400 ITEM_ID_DUPLICATED` 且不创建批次或异步任务；异步批量允许部分成功并返回明细，同步批量全部成功或全部失败；同步批量整体失败时返回每笔失败明细并记录非重试幂等结果；异步失败项重试使用新批次和新的 `idempotencyKey` 并通过 `originalBatchId` 或等效字段关联原批次；默认同步最多 100 条，默认异步最多 10,000 条，API 管理可配置更低上限。 |
| 查询异步任务 | 必须有效 | 必须校验 | 模板级别授权 | 任务对应的显式发布版本路径或 default 路径解析出的目标发布版本 | 查询结果返回任务状态、响应元数据、成功结果或统一错误明细；异步批量任务返回批次汇总和单笔成功/失败明细；不返回进度百分比，异步批量通过 `batch.summary` 返回进度摘要；接口可选接受 `requestId` 作为附加追踪标识并写入审计，不参与任务定位和幂等判断。 |
| 取消异步任务 | 必须有效 | 必须校验 | 模板级别授权 | 任务对应的显式发布版本路径或 default 路径解析出的目标发布版本 | v1 支持受控取消，路径为 `POST /api/{environment}/v1/templates/{templateId}/tasks/{taskId}/cancel`；仅未完成且未过期任务可取消；取消成功后任务状态为 `CANCELLED`，不返回已生成结果、下载地址或异步批量单笔成功结果；不可取消状态返回 `409 ASYNC_TASK_CANCELLATION_NOT_ALLOWED`；取消操作必须记录审计。 |
| 获取下载地址文件 | 必须有效 | 必须校验 | 模板级别授权 | 下载地址对应的显式发布版本路径或 default 路径解析出的目标发布版本 | 同步下载地址和异步结果下载地址固定有效期为 15 分钟，不允许配置覆盖；下载时需要二次授权，校验 API 凭证、AD Group、模板级授权、下载地址有效期和结果有效性；下载时不重新校验发布版本可调用状态；有效期内允许多次下载，不允许配置为一次性下载；下载地址取文件接口在地址过期后不重新签发；相同 `idempotencyKey` 重复命中同步下载地址成功结果时优先返回原下载地址，若原地址过期且结果仍在保留期内可在重复命中响应中重新签发；`download.expiresAt` 等时间字段采用 ISO 8601 带时区偏移格式；API 响应返回可用下载地址，日志、审计、管理界面和契约示例必须脱敏展示；生成结果到期清理前不主动通知，仅记录审计。 |
| 获取 API 契约信息 | API 调用方需被授权访问对应模板 | 访问账号需命中 API 管理中配置的 AD Group | 模板级别授权 | 可查看授权模板的可调用版本 | 管理员、文档作者、以及被授权的 API 调用方可以查看；API 调用方只能查看自己被授权模板的 API 契约。后台 API 契约页调用方视图可展示授权模板的契约版本对比、错误码说明、调用示例、可调用版本列表、API 策略摘要、自己凭证非敏感状态、保真警告码目录、字段含义、JSON 示例、文件流响应头说明，以及授权范围内的非敏感调用结果警告摘要和 `traceId` 或 `auditId` 定位标识；契约版本对比由页面基于已授权契约数据计算，不新增 `ContractResponse` 专门字段；不得展示 API 凭证 secret、完整请求体、模板变量原值、客户数据、完整生成内容或完整审计明细；v1 不提供独立开发者门户或 API 凭证自助管理。 |
| 查看可调用版本列表 | API 调用方需被授权访问对应模板 | 访问账号需命中 API 管理中配置的 AD Group | 模板级别授权 | 可查看授权模板的可调用版本 | 管理员、文档作者、以及被授权的 API 调用方可以查看；API 调用方只能查看自己被授权模板的可调用版本列表。 |
| 查询调用记录 | API 调用方需被授权访问对应模板 | 访问账号需命中 API 管理中配置的 AD Group | 模板级别授权；**仅本 credential** | 可查看授权模板的 invocation 列表/详情 | `GET …/invocations` 支持 `view=logical|flat`；详情含完整 parameters（encryption 密码不返回）；`IDEMPOTENCY_REPLAYED` 不新建记录；记录/artifact TTL 受包级留存配置约束。 |

API 管理配置当前按模板级绑定；一个模板对应一组 API 管理配置，适用于该模板下所有未停用的发布版本。模板停用或废弃时所有发布版本不可调用，单个发布版本停用时仅该版本不可调用；模板或发布版本恢复后，恢复对象重新进入可调用候选范围，但仍受模板状态、发布版本状态和模板级 API 管理配置约束。

## 7. API 管理权限

API 管理由全局管理员和分组管理员承担，不设置独立 API 管理员角色。全局管理员可管理全部 API 管理配置；分组管理员只能管理被授权组范围内的 API 管理配置。API 管理配置与模板绑定，不与单个发布版本绑定；配置变更对该模板下所有未停用发布版本生效。**主配置面** 为模板包 Hub「对外接入」Tab（约定大于配置 L1 + 高级折叠）；独立 API 策略 catalog 页面降级为跨包监控入口（2026-07-03，BDD-API-PACKAGE-ACCESS-INVOCATION-001）。

API 管理配置字段控件基线：AD Group 授权使用可搜索 AD Group 选择器和授权范围摘要，不展示完整成员或未授权组详情；输出方式使用输出格式和输出模式勾选；批量上限使用同步/异步数值输入并展示上限含义；DOCX/PDF 动态加密使用启用开关和能力项选择，不保存加密密码；default 路径目标发布版本使用发布版本选择器，并展示版本状态、契约摘要和影响提示。

API 管理配置按配置域独立保存；每个配置域操作动线为编辑候选配置、执行影响预览、处理硬阻断或确认警告、管理员确认立即生效；候选配置变更后必须重新执行影响预览，保存成功生成新的 `policyVersion` 和审计记录。API 管理配置引入 `policyVersion`；每次配置域变更成功生效后生成新的配置版本，用于契约展示、审计、影响预览和回滚关联。影响预览需要区分硬阻断和警告；违反已确认策略或会导致候选配置不可生效的硬阻断必须阻止保存，风险提示类警告允许管理员确认后继续。硬阻断和警告文案采用固定结构：原因、影响、处理建议；影响信息至少包含受影响发布版本或调用方范围摘要和预期错误码。硬阻断文案必须明确无法保存，警告文案必须明确确认继续后会立即生效并记录审计。

| 操作 | 全局管理员 | 分组管理员 | 文档作者 | API 调用方 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 管理 API 凭证 | 是 | 被授权组范围内 | 否 | 否 | API 凭证对象是调用方级身份，可授权到多个模板 API；全局管理员可管理全部 API 凭证，分组管理员只能管理被授权组范围内的 API 凭证；创建和轮换时 secret 明文只展示一次，平台只保存不可逆摘要或指纹；凭证必须设置有效期，默认 180 天，最长 365 天；轮换时新 secret 立即可用，旧 secret 保留 7 天宽限期；吊销立即阻断该凭证所有后续 API 操作；生命周期操作需要强审计并填写原因。 |
| 配置 API AD Group 授权 | 是 | 被授权组范围内 | 否 | 否 | AD Group 授权配置属于 API 管理功能，不属于模板编排或模板提交功能；作为独立配置域保存，配置变更需要影响预览、硬阻断/警告判断和审计，只支持立即生效，并清理相关授权缓存，不主动通知调用方或管理员；回滚按一次新的受控变更处理。 |
| 配置 API 输出方式和批量上限 | 是 | 被授权组范围内 | 否 | 否 | 同步文件流、同步下载地址、异步任务和批量上限属于 API 管理配置；输出方式和批量上限按配置域独立保存，配置变更需要影响预览、硬阻断/警告判断和审计，只支持立即生效，不主动通知调用方或管理员；回滚按一次新的受控变更处理。 |
| 配置 DOCX/PDF 动态加密能力 | 是 | 被授权组范围内 | 否 | 否 | DOCX/PDF 动态加密配置属于 API 管理功能，不属于模板编排或模板提交功能；API 调用方只能在 API 管理配置允许时传入加密参数；作为独立配置域保存，配置变更需要影响预览、硬阻断/警告判断和审计，只支持立即生效，不主动通知调用方或管理员；回滚按一次新的受控变更处理。 |
| 配置 default 路径目标发布版本 | 是 | 被授权组范围内 | 否 | 否 | default 路径目标发布版本属于 API 管理配置，必须显式指向未停用发布版本；作为独立配置域保存，配置变更只支持立即生效，不支持未来定时生效或待生效变更；配置变更和回滚都需要影响预览、硬阻断/警告判断和审计，不主动通知调用方或管理员。 |
| 配置调用记录与文档留存 | 是 | 被授权组范围内 | 否 | 否 | 包级 `saveGeneratedDocuments`、`invocationRecordRetentionDays`、`documentRetentionDays`（预设选项；max 7y/1y）；`changedAreas` 含 `INVOCATION_RETENTION`；仅影响新产生的记录 TTL。 |
| 查看包级调用记录摘要 | 是 | 被授权组范围内 | 是（只读摘要） | 否 | 包 Hub L2 最近调用列表；**无** variables 明文；合规明细仍仅审计角色。 |
| 查看包级生成审计摘要 | 是 | 被授权组范围内 | 否 | 否 | `GET /api/management/v1/audit/generation?templateExternalId=`；需 `readAudit` + 可读模板组范围；返回 `eventAt/eventType/requestId/outcome/status/accessAccountSummary` 等非敏感摘要；**不得**返回变量明文、完整请求体或下载地址。 |
| 按 invocation 受控再生（审计样件） | 是 | 被授权组范围内 | 否 | 否（`AUDIT_ADMIN`：是，模板可见/`readAudit` 范围内） | CE-G06：`POST …/templates/{templateId}/api/invocations/{invocationId}/regenerate`（默认 / `productionReissue` 缺省或 `false`）；仅指纹齐全且未过期的 `SINGLE`/`BATCH_ITEM`/`ASYNC_TASK`；产物强制 SPECIMEN 水印；**禁止**响应/审计返回 variables 或加密密码；写管理审计 `INVOCATION_REGENERATED`；不新建调用方 runtime SUCCESS 记录。BDD：[ce-g06-audit-reproducible.md](../behavior/ce-g06-audit-reproducible.md)。 |
| 按 invocation 生产重发（无 SPECIMEN） | 是 | 被授权组范围内 | 否 | 否（`AUDIT_ADMIN`：**否** — 403 fail-closed） | PD-6：同一 regenerate 路径；显式 `productionReissue=true` **且** `reason` trim 后非空（建议 max 500）；装配**跳过** SPECIMEN；响应/元数据 `specimen=false`；审计须含 `productionReissue` + `reason` + `specimen`；**无**新 capability bit；缺 reason → 400 `PRODUCTION_REISSUE_REASON_REQUIRED`；preview/test-generate **不可**走此模式。BDD：[pd6-true-non-specimen-reissue.md](../behavior/pd6-true-non-specimen-reissue.md)。**不**翻转 checklist #3b/#5a。 |

## 8. API 授权规则

已确认：

- API 调用权限是模板级别。
- API 采用 API 凭证 + AD Group 双重认证授权。
- AD Group 解析规则适用于所有需要 AD Group 授权的 API 操作，包括生成、批量生成、异步任务查询、异步任务取消、下载取文件、API 契约查看、可调用版本列表和 **调用记录查询**。
- API 管理中，一个模板的 API 授权可以绑定多个 AD Group。
- API 管理中，不同模板的 API 授权可以绑定不同 AD Group。
- AD Group 授权配置属于 API 管理功能，不属于模板编排或模板提交功能。
- AD Group 成功解析结果按 `accessAccount` + `environment` 缓存 5 分钟；不缓存解析失败结果。
- AD Group 解析失败时，如果存在未过期缓存，则使用未过期缓存继续授权；如果不存在未过期缓存，则返回 `503 AD_GROUP_RESOLUTION_FAILED`，`retryable=true`。
- AD Group 授权不得使用过期缓存兜底；过期缓存不能作为授权依据。
- API 管理中的 AD Group 授权配置变更立即生效，并清理相关授权缓存；不等待 5 分钟缓存自然过期。
- 目录中的 AD Group 成员变更在目录同步完成且平台缓存过期后生效；平台需要在 API 契约或管理界面说明最多可能存在 5 分钟平台缓存延迟，不承诺消除外部目录同步延迟。
- 需要读取访问账号的 AD Group。
- 访问账号可以是 service account，也可以是 user account。
- API 分环境。
- API 通过环境变量读取当前环境。
- API 路径统一采用 `/api/{environment}/v1` 前缀；平台运行时仍通过环境变量读取当前部署环境，并校验路径中的 `{environment}` 与当前部署环境一致。
- API 路由需要支持显式发布版本路径；调用方通过路径选择目标模板和发布版本。
- API 需要支持 default 路径；default 路径由 API 管理配置显式路由到某个未停用发布版本。
- 显式发布版本单笔生成路径为 `/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/generate`。
- default 单笔生成路径为 `/api/{environment}/v1/templates/{templateId}/default/generate`。
- 显式发布版本批量生成路径为 `/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/batch-generate`。
- default 批量生成路径为 `/api/{environment}/v1/templates/{templateId}/default/batch-generate`。
- 异步任务查询路径为 `/api/{environment}/v1/templates/{templateId}/tasks/{taskId}`。
- 下载地址取文件路径为 `/api/{environment}/v1/documents/{documentId}/download`；下载时仍需要通过 `documentId` 关联模板并执行模板级二次授权。
- default 路径不得隐式指向最新版本，必须由全局管理员或分组管理员在 API 管理中显式配置目标发布版本。
- default 路径目标版本变更只支持立即生效，不支持未来定时生效或待生效变更。
- default 路径目标版本变更和回滚不主动通知调用方或管理员，仅记录审计；回滚按一次新的受控变更处理。
- API 授权只到模板级别；调用方获得模板权限后，可以调用该模板下所有未停用的发布版本。
- API 凭证授权、AD Group 授权和模板访问授权共同形成模板级 API 访问授权，不对发布版本单独授权。
- API 管理配置当前按模板级绑定；一个模板对应一组 API 管理配置，适用于该模板下所有未停用的发布版本。
- API 管理配置变更需要审计，但不改变已发布版本锁定的模板内容、变量或规则。
- API 不需要按输出格式单独控制权限；调用方获得模板权限后，可以按 API 管理配置生成 DOCX 或 PDF。
- API 不需要按输出模式单独控制权限；调用方获得模板权限后，可以使用 API 管理配置允许的同步文件流、同步下载地址、异步任务或批量生成模式。
- 所有文档生成类 API 必须传入 `requestId` 和 `idempotencyKey`；异步任务查询和下载地址取文件不适用 `idempotencyKey` 要求。
- 幂等唯一性范围为调用方、环境、模板和解析后的发布版本；幂等记录保留 7 天，过期后同一 `idempotencyKey` 可按新请求处理。
- default 路径目标版本切换后，如果重复提交命中旧幂等记录，应返回幂等冲突错误，不按新的 default 目标版本生成文档。
- 相同 `idempotencyKey` 命中已成功的同步文件流请求时，允许重放原文件流结果与对应响应头元数据。
- 相同 `idempotencyKey` 命中已成功的同步下载地址请求时，优先返回原下载地址；若原下载地址已过期且结果仍在保留期内，重复命中响应可重新签发新的下载地址。
- 相同 `idempotencyKey` 命中单笔异步请求时，重复命中响应返回原任务完整状态对象，而不是仅返回 `taskId`。
- 相同 `idempotencyKey` 的原请求失败后，仅系统类临时故障且 `retryable=true` 的场景允许自动重执行；业务校验、授权和策略类失败不自动重执行，重复命中返回原失败结果。
- 生成类 API 的成功和错误响应都回显 `idempotencyKey`；同步文件流通过响应头回显。
- 重复命中成功场景固定返回 `originalRequestAt`，采用 ISO 8601 带时区偏移格式。
- 幂等冲突响应只允许返回安全差异摘要，不得返回旧请求或新请求的业务变量原值、加密密码、完整请求体或敏感配置明文。
- 过期 `idempotencyKey` 复用按新请求处理，API 响应不提示历史复用信息，仅在审计中记录 `reusedExpiredIdempotencyKey` 等字段。
- API 认证、授权和策略拒绝错误使用统一错误模型：细分 `error.code`、`error.category`、英文 `error.message`、`error.messageKey` 和必填 `error.retryable`。
- API 错误类别采用 10 类固定集合；认证、授权、版本路由、API 管理策略、幂等、参数校验、模板契约、生成、加密、批量、异步任务和下载取文件场景均需要映射到 v1 基线错误码清单。
- `error.messageKey` 命名规则采用 `api.error.<category>.<camelCaseCode>`；英文消息必须简洁可读且不泄露 API 凭证、密码、内部配置或未授权资源细节。
- `error.message` 保持错误码级别的通用安全英文文案；同一 `error.code` 不因具体业务场景临时返回不同 `message`，也不新增 `resolutionHint`、`developerMessage` 等提示字段。
- 错误响应示例采用重点场景覆盖，重点覆盖授权与 AD Group、版本与 default 路由、API 管理策略、异步与下载结果、生成与加密失败、批量单笔失败；授权类示例不得泄露未授权资源细节。
- API 错误响应 HTTP 状态码用于表达错误大类，精确失败原因以稳定 `error.code` 为主；认证错误返回 401，授权错误返回 403，AD Group 解析失败返回 503。
- 输出格式、输出模式、批量上限、加密能力等 API 管理策略拒绝返回 400；下载地址或生成结果过期返回 410。
- v1 请求采用严格字段校验，契约 Schema 之外的未知字段返回 `400 REQUEST_BODY_INVALID`；模板标识和发布版本号只通过路径表达，生成请求体不得重复传入。
- 加密参数错误包括缺少必需密码、不支持的权限组合、`permissions` 缺少 `ownerPassword`、`enabled=false` 或未传 `enabled` 时仍传入加密子字段、密码长度不符合 12 到 128 字符基线、open/owner 密码相同；这些错误返回 `400 ENCRYPTION_PARAMETER_INVALID`。
- 加密参数合法但实际加密处理失败时，返回 `500 ENCRYPTION_FAILED`，`retryable=true`；错误响应、日志和审计不得返回密码、内部加密细节或敏感配置值。
- JSON 响应采用统一 `metadata`、`result`、`error` envelope；`metadata` 可包含审计、追踪、请求、幂等、模板、路由和输出摘要，但不得包含 API 凭证 secret、加密密码、未授权资源细节或敏感配置明文。
- `templateId` 可以采用可读稳定模板键，但不得包含客户、个人、账号、金额或其他敏感业务信息；`taskId`、`batchId`、`documentId` 采用资源前缀 + 不透明随机 token，不得承载日期、序号、模板、客户或业务变量含义。
- v1 API 枚举值采用英文 `UPPER_SNAKE_CASE`；加密权限枚举为 `ALLOW_PRINT`、`ALLOW_COPY`、`ALLOW_EDIT`、`ALLOW_ANNOTATE`、`ALLOW_FORM_FILL`。
- `context` 采用安全白名单，v1 仅允许 `sourceSystem`、`channel`、`businessRequestId`、`upstreamTraceId`、`scenario`、`locale`；字段值均为字符串。`context` 不得包含客户姓名、证件号、账号、金额、密码、模板变量原值、完整请求体、API secret、完整下载地址或完整 AD Group 成员等敏感内容；未知 `context` 字段返回 `400 REQUEST_BODY_INVALID`。
- API 管理配置展示字段 v1 基线为 `apiPolicy.policyVersion`、`apiPolicy.updatedAt`、`apiPolicy.updatedBy`、`apiPolicy.allowedOutputFormats`、`apiPolicy.allowedOutputModes`、`apiPolicy.batchLimits.syncMaxItems`、`apiPolicy.batchLimits.asyncMaxItems`、`apiPolicy.encryptionCapabilities`、`apiPolicy.adGroupAuthorizationSummary`、`apiPolicy.credentialSummary`；不得展示 API 凭证 secret、完整 AD Group 成员、未授权组详情、加密密码、历史密文或其他敏感配置明文。
- 字段级错误原因 `fieldErrors[].reason` 采用通用枚举集合；字段路径继续使用点路径和数组下标。
- 批量请求支持批次级统一输出和加密配置，也允许单笔记录单独覆盖输出格式、输出模式和加密参数；每笔覆盖都必须受模板级 API 管理配置约束。
- 批量 JSON 响应必须按请求顺序返回全量单笔明细，每个输入对应一条明细，回显 `itemId`，并包含单笔状态、最终输出配置、加密策略摘要以及成功结果或错误信息。
- 批量请求中每笔记录必须传入 `items[].itemId`，且同一批次内必须唯一；重复 `items[].itemId` 导致整批请求校验失败，不创建批次或异步任务。
- 同步批量中任一记录因参数校验或 API 管理策略失败时，整批失败且不生成任何文件；响应需要返回每笔失败明细，并按非重试幂等结果记录。
- 异步批量部分成功后的失败项重试必须使用新批次和新的 `idempotencyKey`，并通过 `originalBatchId` 或等效关联字段关联原批次。
- **`originalBatchId` 校验（CE-C05）：** 字段出现时仅允许关联**当前 API 凭证**下已存在的原 `BATCH_ROOT`；他凭证或不存在统一 `404 ORIGINAL_BATCH_NOT_FOUND`（不泄露资源是否存在）；审计必须记录该关联。行为规格：[ce-c05-original-batch-id.md](../behavior/ce-c05-original-batch-id.md)。
- API 凭证代表调用系统/应用；请求中同时识别实际访问账号，并读取该访问账号的 AD Group。
- API v1 请求头字段确认为 `X-Api-Credential-Id`、`X-Api-Credential-Secret`、`X-Access-Account`；可选追踪请求头为 `X-Trace-Id`。
- `X-Api-Credential-Secret` 不得进入日志、审计、响应、契约展示或管理界面；`X-Trace-Id` 传入时平台沿用该值作为响应和审计中的 `traceId`，未传入时由平台生成。
- API 凭证对象是调用方级身份，可授权到多个模板 API；模板调用仍必须同时满足 API 凭证授权、AD Group 授权和模板级授权。
- API 凭证由全局管理员和分组管理员管理；全局管理员可管理全部 API 凭证，分组管理员只能管理被授权组范围内的 API 凭证。
- API 凭证创建和轮换时，secret 明文只展示一次；平台只保存不可逆摘要或指纹，不允许管理员后续重新查看 secret 明文。
- API 凭证必须设置有效期；默认有效期为 180 天，最长 365 天，管理员可设置更短有效期。
- API 凭证状态集合确认为 `ACTIVE`、`EXPIRING_SOON`、`EXPIRED`、`REVOKED`；轮换状态由当前 secret 与旧 secret 7 天宽限期表达。
- API 凭证吊销立即生效，阻断该凭证的所有后续 API 操作，包括新生成、异步任务查询、异步任务取消和下载取文件；已受理的后台生成任务可继续完成，但调用方不能再使用被吊销凭证获取结果。
- API 凭证过期后返回 `401 API_CREDENTIAL_EXPIRED`；API 凭证吊销后返回 `401 API_CREDENTIAL_REVOKED`。
- API 凭证到期前 30 天、7 天和 1 天提醒全局管理员和对应分组管理员；不主动提醒 API 调用方，调用方可查看自己凭证的非敏感状态和到期摘要。

## 9. 分组隔离规则

已确认：

- 文档作者（`DOCUMENT_AUTHOR`）需要分组（承担原母版设计与模板编排分组要求）。
- 母版本身需要严格按组隔离。
- 模板本身需要分组隔离。
- 模板只能在所属或被授权组范围内使用和维护。
- 不同组之间不允许复用模板。
- 分组隔离采用混合隔离模型。
- 混合分组隔离维度包括业务条线、部门/团队。
- 用户可以属于多个组，并在所属或被授权组范围内访问多个组的模板/母版。
- 分组管理员的管理范围由显式授权的一个或多个组决定，不要求等同于用户自身所属组。
- 母版只能在所属或被授权组范围内查看、使用和维护。
- 不同组之间不允许复用母版。

### 9.1 用户管理权限矩阵

用户管理覆盖用户全生命周期。本地账户库长期作为授权权威源，SSO 仅负责认证（见 [ADR 0036](../adr/authorization-security/0036-local-account-store-authorization-authority.md)）。角色标识与本矩阵角色名映射为：`GLOBAL_ADMIN`=全局管理员、`GROUP_ADMIN`=分组管理员、`DOCUMENT_AUTHOR`=文档作者（L1 显示名 Pending）、`TEMPLATE_TESTER`=测试人员、`LEGAL_REVIEWER`=法务审阅人、`AUDIT_ADMIN`=审计管理员。退役不可分配：`TEMPLATE_APPROVER`、`MASTER_DESIGNER`、`TEMPLATE_AUTHOR`（**422** `ROLE_NOT_ASSIGNABLE`）。

用户口令只允许以 Argon2id 哈希持久化；重置密码采用管理员传入新口令模式，平台只存哈希，不返回一次性临时口令，不在响应、日志或审计中回显口令明文或哈希。

| 操作 | 全局管理员 | 分组管理员 | 说明 |
| --- | --- | --- | --- |
| 查看用户列表 | 是（全部） | 被授权组范围内 | 可按 group/role 过滤、分页；分组管理员只能看到被授权组范围内用户，不泄露范围外用户存在性。 |
| 创建用户 | 是 | 被授权组范围内 | 分组管理员只能创建其 `authorizedGroupCodes` 子集范围内用户；分配的组范围必须 ⊆ 自身被授权组范围，越权返回 `403 GROUP_SCOPE_OUT_OF_RANGE`。 |
| 查看用户详情 | 是 | 被授权组范围内 | 范围外用户按不可见处理，返回 `404 USER_NOT_FOUND`，不泄露存在性。 |
| 编辑用户（显示名、邮箱） | 是 | 被授权组范围内 | 分组管理员只能编辑被授权组范围内用户。 |
| 分配角色 | 是（全部六个管理角色） | 仅运营类角色，被授权组范围内 | 分组管理员只能分配 `DOCUMENT_AUTHOR`、`TEMPLATE_TESTER`、`LEGAL_REVIEWER`；不得分配 `GLOBAL_ADMIN`、`AUDIT_ADMIN`、`GROUP_ADMIN`，越权返回 `403 ROLE_ASSIGNMENT_NOT_ALLOWED`（防提权）。退役角色代码 → **422** `ROLE_NOT_ASSIGNABLE`。 |
| 分配被授权组范围 | 是 | 被授权组范围内 | 分配的组范围必须 ⊆ 分组管理员自身被授权组范围，越权返回 `403 GROUP_SCOPE_OUT_OF_RANGE`。 |
| 停用/启用用户 | 是 | 被授权组范围内 | 分组管理员可在被授权组范围内停用/启用用户。 |
| 重置密码 | 是 | 被授权组范围内 | 分组管理员可在被授权组范围内重置密码；管理员传入新口令，平台只存哈希。 |
| 逻辑删除用户 | 是 | 否 | 逻辑删除（`deleted_at` 标记）仅全局管理员可执行；分组管理员越权返回 `403 USER_DELETE_NOT_ALLOWED`。已逻辑删除用户不再可登录、不再参与授权判定。 |

### 9.2 分组管理权限矩阵

分组升级为一等可运营对象，带维度 `dimension`（`BUSINESS_LINE` 业务条线 / `DEPARTMENT` 部门），与混合隔离模型（业务条线、部门/团队）保持一致。分组编码 `group_code` 创建时确定、全局唯一、不可修改；维度创建时确定；编辑只修改显示名。分组不提供逻辑删除或物理删除。

| 操作 | 全局管理员 | 分组管理员 | 说明 |
| --- | --- | --- | --- |
| 查看分组列表 | 是（全部） | 只读，被授权组范围内 | 分组管理员对分组为只读，仅可见被授权组范围内分组。 |
| 查看分组详情 | 是 | 只读，被授权组范围内 | 范围外分组按不可见处理，返回 `404 GROUP_NOT_FOUND`，不泄露存在性。 |
| 创建分组 | 是 | 否 | 仅全局管理员；分组管理员越权返回 `403 GROUP_MANAGEMENT_NOT_ALLOWED`；分组编码重复返回 `409 GROUP_CODE_ALREADY_EXISTS`。 |
| 编辑分组（仅显示名） | 是 | 否 | 仅全局管理员；只修改显示名，不改 `group_code` 或 `dimension`；分组管理员越权返回 `403 GROUP_MANAGEMENT_NOT_ALLOWED`。 |
| 停用/启用分组 | 是 | 否 | 仅全局管理员；已停用分组不可作为新的用户被授权组范围或母版/模板归属组使用；分组管理员越权返回 `403 GROUP_MANAGEMENT_NOT_ALLOWED`。 |

### 9.3 分组管理员在被授权组范围内管理用户（新权限点与越权防护）

这是权限矩阵此前没有的新权限点，必须显式启用并强制审计，全部按 fail-closed 处理：

- 分组管理员只能创建/编辑其 `authorizedGroupCodes` 子集范围内的用户；为用户分配的组范围必须 ⊆ 自身被授权组范围（`403 GROUP_SCOPE_OUT_OF_RANGE`）。
- 分组管理员只能分配运营类角色（`DOCUMENT_AUTHOR`、`TEMPLATE_TESTER`、`LEGAL_REVIEWER`）；不得分配 `GLOBAL_ADMIN`、`AUDIT_ADMIN`、`GROUP_ADMIN`（`403 ROLE_ASSIGNMENT_NOT_ALLOWED`，防提权）。退役角色赋值 → **422** `ROLE_NOT_ASSIGNABLE`。
- 逻辑删除用户、分组的创建/编辑/停用/启用仅全局管理员可执行；分组管理员对分组只读，仅可见被授权组范围内分组（`403 USER_DELETE_NOT_ALLOWED` / `403 GROUP_MANAGEMENT_NOT_ALLOWED`）。
- 分组管理员可在被授权组范围内停用/启用与重置密码用户。
- 任何越权请求一律 fail-closed，返回统一安全错误码与通用安全消息，不泄露未授权资源是否存在、未授权组详情或敏感明文（口令、口令哈希等）。
- 管理面接口前缀为 `/api/management/v1`，沿用统一 envelope 与错误模型；完整接口清单与错误码基线见 [需求记录 - 身份与分组管理](../requirements/requirements-plan.md)。

## 10. 审计权限

已确认审计范围：

- 模板创建/编辑。
- 提交测试、测试通过、测试不通过；测试判定审计包含结构化结果、测试意见摘要、原因分类、影响范围、风险提示确认、关联测试数据集、批量测试摘要、覆盖率摘要和生成预览摘要。
- 提交审批、审批通过、审批不通过；审批判定审计包含结构化结果、审批意见摘要、理由摘要、退回原因分类、影响范围、风险提示确认以及关联测试记录、变更差异摘要和发布前检查清单摘要。
- 测试/审批意见模板和风险提示文案配置变更。
- 协作待办创建、解决、超时升级和超时阈值配置变更。
- 分组管理员例外干预测试或审批判定时，审计需要记录强制原因、二次确认结果和单独例外干预标记。
- 发布、停用、恢复、废弃。
- 版本停用、版本恢复。
- API 管理侧的 AD Group 授权配置变更。
- API 管理侧的 DOCX/PDF 动态加密配置变更。
- API 管理侧的输出模式和批量上限配置变更。
- API 管理侧的 AD Group 授权、DOCX/PDF 动态加密能力、输出模式和批量上限配置回滚。
- API 管理侧的 default 路径目标发布版本配置变更。
- API 管理侧的 default 路径目标发布版本回滚。
- API 生成结果到期清理。
- 导出/导入。
- 母版提交审核、审核通过、审核不通过、变更和影响分析。
- API 调用。
- 用户管理：用户创建、编辑（含角色变更、组范围变更）、停用/启用、逻辑删除、重置密码。
- 分组管理：分组创建、编辑（显示名）、停用、启用。

用户管理与分组管理审计不得记录口令明文或口令哈希；重置密码只记录事件、操作者和范围，不记录口令内容。分组管理员管理用户的操作需额外记录“被授权组范围摘要”（`actorAuthorizedGroupScopeSummary`）。

API 调用审计至少记录 `requestId`、`idempotencyKey` 或其摘要、幂等处理状态。批量调用还需记录 `batchId`、`items[].itemId` 或其摘要、失败项重试关联的 `originalBatchId` 或等效关联字段。过期 `idempotencyKey` 复用时，需要记录 `reusedExpiredIdempotencyKey`、`previousIdempotencyExpiredAt`、`previousRequestAt`、`previousResolvedReleaseVersion`。API 传入的 DOCX/PDF 加密密码不得进入审计记录；审计中涉及下载地址时必须脱敏，不记录完整可用下载地址。

API 调用和 API 管理配置变更审计采用标准摘要对象，字段基线包括 `auditId`、`eventType`、`eventAt`、操作者或系统主体摘要、API 凭证或指纹摘要、访问账号、环境、模板、发布版本、解析后发布版本、路由类型、`requestId`、`idempotencyKey` 摘要、幂等状态、`taskId`、`batchId`、`itemId`（或其安全摘要）、`contextSummary`、输出摘要、加密摘要、批量摘要、资源 ID、结果摘要、保真警告摘要、错误摘要、耗时和配置差异摘要。标准审计摘要不得记录模板变量原值、加密密码、完整请求体、API 凭证 secret、完整下载地址、完整 AD Group 成员、未授权组详情、历史密文或敏感配置明文。API 调用方在后台契约页调用方视图中只能查看授权范围内的非敏感保真警告摘要和定位标识，不因该视图获得完整审计查看权限。

API 管理配置变更统一使用审计事件 `API_POLICY_UPDATED`，并通过 `changedAreas` 表达变更配置域；`changedAreas` 取值基线为 `AD_GROUP_AUTHORIZATION`、`OUTPUT_POLICY`、`BATCH_LIMIT`、`ENCRYPTION_CAPABILITY`、`DEFAULT_ROUTE_TARGET`。API 管理配置变更审计需要记录 `policyVersion`、上一配置版本、变更配置域、配置差异摘要、影响预览摘要、硬阻断和警告摘要、确认结果、是否回滚以及回滚来源版本；不得记录敏感配置明文。

模板可验证性相关记录包括测试数据集摘要、测试生成记录、批量测试摘要、样例覆盖率摘要、模板综合覆盖率摘要、生成预览摘要、最终 DOCX/PDF 产物引用、预览对比摘要、变更差异摘要、发布前检查清单结果、审批摘要、保真警告摘要、保真警告摘要已查看确认和阻断项状态。测试、审批、发布摘要和协作待办按输出策略锁定生成预览记录引用、最终产物引用、对比摘要、覆盖率摘要和非敏感摘要；预览文件、最终产物和并排对比视图按权限受控查看。相关审计、协作待办或可追溯记录不得保存模板变量测试值、客户数据、完整请求体或完整生成内容中的敏感明文。

API 凭证生命周期审计需要覆盖创建、轮换、吊销、过期、到期提醒和凭证摘要查看；审计至少记录操作者、时间、操作原因、管理范围、状态变化、到期时间、凭证标识或指纹摘要和受影响授权范围，不记录 secret 明文。

AD Group 解析、缓存命中、缓存失效、解析失败和授权拒绝需要记录审计摘要；审计不得记录完整 AD Group 成员清单或未授权组详情。

已确认审计查看权限：

- 审计管理员可查看全部审计记录。
- 全局管理员按已确认最大权限范围查看审计记录。
- 分组管理员只能查看被授权组范围内的审计明细。
- 分组管理员不能查看未授权组审计明细，也不能查看全局审计明细。
- 文档作者、测试人员、法务审阅人、API 调用方不因该角色本身获得审计查看权限。

已确认后台审计读取/导出接口约束（E04 当前实现）：

- 后台审计读取与管理审计导出接口 `actorRole` 有效选项仅允许 `AUDIT_ADMIN`、`GLOBAL_ADMIN`、`GROUP_ADMIN`。
- 当 `actorRole=GROUP_ADMIN` 时，管理审计读取、管理审计导出与生命周期审计读取请求都必须同时提供 `groupScope` 与 `templateId`，否则返回 `422`。
- 审计时间窗口过滤参数 `eventAtFrom`、`eventAtTo` 可选；若任一参数提供必须是 ISO-8601 时间戳，且当两者同时提供时必须满足 `eventAtFrom <= eventAtTo`，否则返回 `422 INVALID_TIME_WINDOW`。
- 管理审计导出成功响应返回固定脱敏格式 `management-audit-export-v1-json`，并对操作者摘要与凭证指纹执行脱敏输出。

已确认审计导出权限：

- 审计记录允许导出。
- 全局管理员和审计管理员可导出全部审计记录。
- 分组管理员只能导出被授权组范围内的审计记录。
- 文档作者、测试人员、法务审阅人、API 调用方不因该角色本身获得审计导出权限。

已确认审计保留规则（Tier-1 / Tier-2 — [ADR-0048](../adr/operations/0048-audit-data-retention-policy.md) / [LR-D1 BDD](../behavior/lrp-d1-audit-retention.md)）：

**Confirmed — Tier-1 在线热库（PostgreSQL；LR-D1 交付）：**

- `management_audit_event`：默认保留 **90 天**；超龄行 **硬删除**（`event_at < cutoff`）；可通过 `docgen.audit.management-retention-days` 配置。
- `runtime_generation_audit_event`：默认保留 **365 天**；超龄行 **硬删除**；可通过 `docgen.audit.runtime-retention-days` 配置。
- 处置方式与 [ADR-0040](../adr/api-management/0040-api-package-access-and-invocation-retention.md) 调用记录清理一致：**硬删除**，无 soft-delete；v1 **不做**热库归档导出。
- 平台级 purge-evidence（`AUDIT_RETENTION_PURGE`）仅 **审计管理员 / 全局管理员** 可查；**分组管理员** 不可见无 `group_code` 的平台级 purge 行。
- **CE-G04 Legal hold（2026-07-16）：** ACTIVE legal hold 在 retention 硬删前提供豁免（模板+时间窗 和/或 invocation 集合）。**仅 GLOBAL_ADMIN** 可创建/列表/释放 hold；其他角色 fail-closed **403**。行为 SoT：[ce-g04-legal-hold.md](../behavior/ce-g04-legal-hold.md)。不改变 ADR-0040/0048 默认窗口正文。

**Deferred — Tier-2 归档（对象存储；非 D1）：**

- 历史表述「默认保留 **5 年**」表示 **多年合规 / 监管留存意图**，由 **Tier-2 归档**承担（待建）；**不得**将「5 年」解读为 Tier-1 PostgreSQL 热数据默认窗口。
- [ADR-0030](../adr/operations/0030-operational-platform-baseline.md) 中「DB 180 天 + 对象存储 3 年」为平台级早期基线行；对上述两张审计表的 **Tier-1 运营窗口**以本矩阵 + ADR-0048 为准（不静默改写 ADR-0030 Accepted 决策正文）。

**Pending（非阻塞）：**

- Tier-2 归档格式、确切年限（5 年 / 3 年 / 7 年叙述）与取回流程 — 待未来切片确认。

## 11. 统一授权与敏感数据保护

已确认：

- v1 采用统一授权判定基线，覆盖 API、下载/任务、契约查看、API 管理、审计查看/导出和后台模板/母版操作。
- 统一授权判定在执行受保护操作或返回敏感响应前完成；授权依赖不可用且没有已确认可用缓存时按 fail-closed 处理。
- 统一授权判定按入口类型组合校验身份、角色、分组范围、API 凭证、访问账号、AD Group、模板级授权、对象归属、环境、资源状态和 API 管理配置。
- API 入口必须同时满足 API 凭证授权、AD Group 授权和模板级授权；后台入口必须同时满足角色权限、分组范围和对象归属规则。
- 任务查询、任务取消和下载取文件必须通过 `taskId` 或 `documentId` 解析到关联模板，并执行模板级二次授权。
- API 契约查看和可调用版本列表只能返回当前授权视角下可见的内容。
- 审计查看和导出必须沿用审计权限范围；分组管理员只能查看和导出被授权组范围内的审计记录。
- 授权拒绝或授权依赖失败只返回已确认的安全错误码和通用安全消息，不泄露未授权资源是否存在、未授权组详情、完整成员列表、API secret、加密密码或内部配置细节。
- 授权判定和授权拒绝需要记录安全审计摘要，包含主体摘要、入口、环境、对象范围摘要、判定结果、拒绝原因码或依赖失败原因；不得记录敏感明文。
- 敏感数据分级处理基线为禁止明文持久化/展示、允许摘要或指纹、授权响应例外。
- 禁止明文持久化或展示的内容包括 API 凭证 secret、DOCX/PDF 加密密码、模板变量原值、模板测试数据敏感值、完整请求体、完整下载地址、完整 AD Group 成员、未授权组详情、历史密文、敏感配置明文、内部渲染诊断明文和未授权生成文档内容；保真警告不得包含模板变量原值、粘贴原文、客户数据、完整请求体或生成文档敏感内容。
- **CE-G03：** 测试数据集存储中的授权测试值（经 `SYNTHETIC` / `EXPLICIT_SENSITIVE`）不视为对维护者的「未授权展示」；审计/契约/导出仍禁明文。见 [ce-g03-testdata-pii.md](../behavior/ce-g03-testdata-pii.md)。
- **CE-G06 / ADR-0057（2026-07-16；IBL-A5 Amendment 2026-07-18）：** `api_invocation_record.parameters_storage` 在调用记录留存窗口内可持久化**已消毒且按 PII 分类收窄后**的模板变量，用途仅限：(1) 调用方 reconciliation（ADR-0040）；(2) 受控再生内部重放。**明文留存仅覆盖**版本 `VariableSchema.piiCategory = NONE`（或缺省等价）的字段；`piiCategory ≠ NONE` 与未知/未分类 key **禁止**明文落库（省略或稳定哨兵；可选 `redactedVariableKeys` 仅键名）。加密密码仍禁止落库。留存 TTL = 调用记录 retention；行清理时一并销毁。**管理端列表/详情/CSV、管理审计、日志、导出、契约示例仍禁止**返回或展示 variables / 密码明文（HIST C6 **不**放宽）。列级/应用层 encryption-at-rest **暂缓**（对齐 ADR-0045；待 KMS）。权威决策：[ADR-0057](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md) Amendment 2026-07-18；行为：[ce-g06-audit-reproducible.md](../behavior/ce-g06-audit-reproducible.md)、[ibl-a5-pii-retention-redaction.md](../behavior/ibl-a5-pii-retention-redaction.md)。
- 允许以摘要或指纹表达的内容包括 API 凭证标识或指纹摘要、`idempotencyKey` 摘要、请求语义 hash、`variablesHash`、`itemsHash`、加密策略摘要、AD Group 授权摘要、下载地址脱敏值、`contextSummary`、`fidelityWarnings` 非敏感摘要、`policyVersion`、`changedAreas` 和配置差异摘要。
- 授权响应例外仅限已确认安全场景：API 凭证创建或轮换时 secret 明文只展示一次；授权 API 响应可返回可用 `download.url`；同步文件流和下载取文件可在授权通过后返回生成文档内容；`task.queryPath` 只是相对查询路径，不授予额外访问能力；调用方 invocation 详情可按 ADR-0040/0057（含 IBL-A5 收窄）返回**脱敏后** parameters（无禁止类明文）；**管理端** invocation API 仍不得返回 variables。
- 脱敏规则适用于日志、审计、管理界面、API 契约展示、契约示例、错误响应、导出文件和支持排查材料；未知或未分类字段默认按敏感处理（含 `parameters_storage` 写路径）。

## 12. 待确认权限设计议题

以下议题来自文档一致性、可行性和可用性审查，不作为已确认权限。

当前权限矩阵暂无其他已识别的待确认权限议题。

相关 API 契约议题详见 [API 文档索引](../api/README.md) 与 [OpenAPI v1](../api/openapi-v1.yaml)。

## 13. 登录起点角色旅程与路由可见性

**已确认（2026-06-23 维护者决策；Wave 5 ADR-0070 压缩修正 2026-07-21）：**

- 管理角色目录为 **6 个角色**：`GLOBAL_ADMIN`、`GROUP_ADMIN`、`DOCUMENT_AUTHOR`、
  `TEMPLATE_TESTER`、`LEGAL_REVIEWER`、`AUDIT_ADMIN`
  （[ADR-0070](../adr/authorization-security/0070-role-compression-six-roles.md)；
  Wave 5 BDD [sys-norm-roles.md](../behavior/sys-norm-roles.md)）。
- **不设置独立 `API_ADMIN` 角色**；API 管理由全局管理员和分组管理员承担（与 §2 一致）。
- **无权限控件隐藏**（不渲染），不采用禁用置灰；直链访问仍走 Forbidden fail-closed。
- 会话 **`capabilities`** 与 **`visibleRoutes`** 由后端统一下发；前端不得自行推导 master/template
  路由权限。
- 母版/模板变更采用 **版本化 + 逻辑删除**（无硬删除、无原地重传）。
- 退役角色 `TEMPLATE_APPROVER` / `MASTER_DESIGNER` / `TEMPLATE_AUTHOR` **不得**出现在可分配目录或
  角色选择器（ROLE-012）；旅程/onboarding 映射见 ROLE-014。

### 13.1 角色到默认 landing 与可见路由

**实现来源（历史）：** `RouteVisibilityService.resolveDefaultRoute` /
`resolveVisibleRoutes`；前端 canonical 路径见 `frontend/src/routing/routeKeys.ts`。
Wave 5 实现须将可见性表对齐下表（六角色）。

| 逻辑路由标识 | GLOBAL | GROUP | DOCUMENT_AUTHOR | TEMPLATE_TESTER | LEGAL_REVIEWER | AUDIT_ADMIN |
| --- | --- | --- | --- | --- | --- | --- |
| `route.dashboard-home` | ✓ default | ✓ default | ✓ default | ✓ default | ✓ default | — |
| `route.identity-administration` | ✓ | ✓ | — | — | — | — |
| `route.master-management` | ✓ | ✓ | ✓ | — | — | — |
| `route.template-management` | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| `route.content-module-management` | ✓ | ✓ | ✓ | — | ✓ | — |
| `route.api-policy-management` | ✓ | ✓ | — | — | — | — |
| `route.asset-library-management` | ✓ | ✓ | ✓ | ✓ | — | — |
| `route.legal-hold-administration` | ✓ | — | — | — | — | — |
| `route.audit-console` | ✓ | ✓ | — | — | — | ✓ default |

**Canonical 前端路径：** `route.dashboard-home` → `/dashboard`（`DashboardView`）；
`route.identity-administration` → `/entitlement/users` 与 `/entitlement/groups`
（`UserManagementView` / `GroupManagementView`）；`route.asset-library-management` → `/library/assets`；
`route.legal-hold-administration` → `/governance/legal-holds`（**仅 GLOBAL_ADMIN**）；其余可见路由见 `ROUTE_PATH_BY_KEY`。

### 13.1.1 Dashboard 合并（COR-T11 Done，2026-06-24）

下列逻辑路由键仍保留于 `ManagementRoute` 与前端 `routeKeys.ts`，但 **不再** 由
`RouteVisibilityService` 下发为可见路由；`/home/*` 别名重定向至 Dashboard 或 canonical 路径：

| 逻辑路由标识 | 重定向目标 | 说明 |
| --- | --- | --- |
| `route.global-governance-home` | `/dashboard` | 治理摘要已并入 Dashboard |
| `route.group-governance-home` | `/dashboard` | 同上 |
| `route.template-authoring-home` | `/dashboard` | 同上 |
| `route.tester-workbench` | `/dashboard` | 测试队列由 Dashboard 任务区承载 |
| `route.approver-workbench` | `/dashboard` | 审批队列由 Dashboard 任务区承载（原审批人员旅程 → `GROUP_ADMIN`） |

决策：`docs/adr/decisions/2026-06-23-batch-b-workflow-defaults.md`；dead workbench 视图已移除。
文档层以 §13.1 表格为准；不得将上表中的过渡键记为当前默认 landing。

### 13.1.2 行为型导航入口可见性（P21，确认设计 / 实现 Done；Wave 5 角色列更新）

**状态：** 确认设计（2026-06-29）；实现 **Done**（2026-06-30）— [P21](../plan/detail/P21-role-journey-frontend-redesign.md) T01/T01a + X02 治理收尾。
决策：[behavior-typed IA + business terminology](../adr/decisions/2026-06-29-behavior-typed-ia-business-terminology.md)
（扩展 Batch B / COR-T11；单一任务台仍为权威入口）。
Wave 5：可见性列对齐六角色（审批队列 → `GROUP_ADMIN`；整改/作者队列 → `DOCUMENT_AUTHOR`）。

行为型入口是 **任务台（`/dashboard`）的按队列过滤视图**，不是独立 workbench 页；可见性对齐
协作工作项队列可见性（`CollaborationWorkItemAccessSupport`）。入口标签使用业务用语
（见 [business-terminology-guide.md](../product/business-terminology-guide.md)），i18n key 保持稳定。

| 行为型入口 | 来源队列 | GLOBAL | GROUP | DOCUMENT_AUTHOR | TEMPLATE_TESTER | LEGAL_REVIEWER | AUDIT_ADMIN |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 待我测试（Waiting on my testing） | TEST | ✓ | ✓ | — | ✓ | — | — |
| 待我法务审阅（Waiting on my legal review） | LEGAL | ✓ | ✓ | — | — | ✓ | — |
| 待我审批（Waiting on my approval） | APPROVAL | ✓ | ✓ | — | — | — | — |
| 待我修改（Waiting on my fixes） | REMEDIATION | ✓ | ✓ | ✓ | — | — | — |
| 待确认上线（Waiting to confirm go-live） | PENDING_RELEASE | ✓ | ✓ | — | — | — | — |
| 超时待跟进（Overdue to follow up） | ESCALATION | ✓ | ✓ | — | — | — | — |
| 待审核母版（Masters to review） | master review（非协作队列） | ✓ | ✓ | (本人返工) | — | — | — |

说明：

- 展示行为型入口 **不授予** 额外编辑/判定/发布权限；处置仍在模板/母版详情的受控决策表单完成。
- **IBL-E3：**「待我法务审阅」仅对具备 `decideLegalApprovals` 的会话可见；列表仅含 LEGAL 队列（`PENDING_LEGAL_DECISION`）。「待我审批」仅展示 COMPLIANCE / 单级 `PENDING_DECISION` 项，**不含** LEGAL 待办；正常合规路径角色为 `GROUP_ADMIN`（吸收原 `TEMPLATE_APPROVER`）。
- `ESCALATION/超时提醒` 仅管理员（GLOBAL/GROUP）可见；交互强调"可见性提醒"，不代为完成、不改变模板状态。
- 文档作者走到 `PENDING_RELEASE` 显示"等待组长确认上线"，无发布主按钮（COR-T07 重申；`publishTemplates` 仅管理员）。
- **实现说明（2026-06-30）：** 既有协作触发已发射工作项；**IBL-E3** 扩展 LEGAL 队列发射/关闭环（实现叶交付）。

### 13.1.3 Dashboard Overview 汇总 API（PRR-D01c）

| 操作 | 全局管理员 | 分组管理员 | 文档作者 | 测试人员 | 法务审阅人 | 说明 |
| --- | --- | --- | --- | --- | --- | --- |
| 查看 Dashboard Overview 汇总计数 | 是 | 被授权组范围内 | 是 | 是 | 是 | **`GET /api/management/v1/dashboard/summary`（OpenAPI `getDashboardSummary`）。无新 capability bit；非 object-scope。** 复用管理会话认证 + 与 catalog list（LR-C5）相同的 group-access：计数仅含会话可访问组内母版/模板分桶与目录总数；无组授权 → 全 0（fail-closed，不泄露他组）；未认证 → 401。UI 统计卡仍受 §13.1 `visibleRoutes` 过滤；本端点**不**替代 collaboration / workflow inbox，**不**授予额外编辑/判定/发布权。行为：[prod-dashboard-summary-api.md](../behavior/prod-dashboard-summary-api.md)（BDD-PRR-D01C / D01C-C4）；契约：[contract-outline.md](../api/contract-outline.md) «Dashboard summary 契约（PRR-D01c）»。**非** go-live；不翻转 checklist **#3b** / **#5a**。 |

### 13.2 会话 capabilities（后端下发）

| capability | 角色 |
| --- | --- |
| `manageMasters` | GLOBAL, GROUP, DOCUMENT_AUTHOR |
| `reviewMasters` | GLOBAL, GROUP |
| `authorTemplates` | GLOBAL, GROUP, DOCUMENT_AUTHOR |
| `decideTests` | GLOBAL, GROUP, TEMPLATE_TESTER |
| `decideLegalApprovals` | GLOBAL, GROUP, LEGAL_REVIEWER |
| `decideApprovals` | GLOBAL, GROUP |
| `publishTemplates` | GLOBAL, GROUP only (Batch B default; authors submit for release, admins publish) |
| `exportTemplates` | GLOBAL, GROUP, DOCUMENT_AUTHOR |
| `viewCollaborationWorkItems` | GLOBAL, GROUP, DOCUMENT_AUTHOR, TEMPLATE_TESTER, LEGAL_REVIEWER |
| `maintainCollaborationTimeoutConfig` | GLOBAL, GROUP |
| `authorContentModules` | GLOBAL, GROUP, DOCUMENT_AUTHOR |
| `decideContentModuleReviews` | GLOBAL, GROUP |
| `manageContentModuleLifecycle` | GLOBAL, GROUP |
| `manageApiPolicy` | GLOBAL, GROUP |
| `manageAssetLibrary` | GLOBAL, GROUP, DOCUMENT_AUTHOR, TEMPLATE_TESTER |
| `readAudit` | GLOBAL, GROUP, AUDIT_ADMIN |

**IBL-E3 / ADR-0064 + Wave 5：** `decideLegalApprovals` 不变。`decideApprovals` **不**授予 LEGAL 阶段，且**不再**含 `TEMPLATE_APPROVER`（已吸收至 `GROUP_ADMIN`）。阶段错位 → 403/409/422 稳定码见 §5.2。

**Reminder timing settings IA（2026-07-21 / TM #153 — capability 不变，仅澄清 UI 可见性）：**

- **无新 capability bit / 无新角色。** 仍为 `maintainCollaborationTimeoutConfig` → GLOBAL, GROUP；API `GET`/`PUT /api/management/v1/collaboration-timeout-config` 授权语义不变。
- **System settings（系统设置）导航：** 仅 `GLOBAL_ADMIN` **且** `maintainCollaborationTimeoutConfig=true` 可见；落地全页 Canonical `/system/settings/reminder-timing`，仅编辑 **Global default**。`GROUP_ADMIN` 不得见该导航；深链同路由 → Forbidden / 路由守卫 fail-closed。
- **Team settings（团队设置）控件：** 仅 `GROUP_ADMIN` **且** capability=true，出现在 Groups/team 表面（`/entitlement/groups` 等 Entitlement 团队表面）页头/顶栏；打开对话框仅编辑 **Group override**。**不**出现在 Dashboard Overview / Tasks。
- **Dashboard Overview：** 不得挂载催办时限配置面板；协作待办/超时跟进队列可见性仍由 `viewCollaborationWorkItems` + §13.1.2 行为型入口规则决定。
- 无 capability 角色：导航与控件均隐藏；直链仍 Forbidden。行为 SoT：[reminder-timing-settings-ia.md](../behavior/reminder-timing-settings-ia.md)；导航：[catalog-navigation-ux.md](../product/catalog-navigation-ux.md)。

**CE-E02 资产库管理面（2026-07-16；Wave 5 角色修正）+ ALGI 组隔离（2026-07-22 / TM #154）：**

- 逻辑路由 `route.asset-library-management` → canonical `/library/assets`；capability `manageAssetLibrary`（上表）仍门禁路由可见性。
- **组作用域（已确认，supersedes CE-E02 平台共享目录 E02-C12）：** 每条资产归属唯一业务 `groupCode`；自然身份 `(groupCode, assetKey)`；v1 **硬隔离**（无跨组共享/只读池）。服务层对列表 / 上传 / 停用强制 **动作 ∩ 授权组**（fail-closed）；`GROUP_ADMIN` **不等同**于跨组 `GLOBAL_ADMIN`。
- **动作细粒度（服务层强制，非仅 capability；SEAL 角色门禁保留且组内生效）：**
  - 列表（含显式 `DISABLED`/`ALL` 查询）→ GLOBAL（全部组，可选 `groupCode` 过滤）/ GROUP / DOCUMENT_AUTHOR（仅授权组；未授权 `groupCode` 过滤 → 空页不泄露）/ TEMPLATE_TESTER（仅授权组 **ACTIVE**）。
  - 上传 `IMAGE`/`OTHER` → GLOBAL / GROUP / DOCUMENT_AUTHOR，且 multipart **必须**带目标 `groupCode`（GLOBAL 任意组；其余仅授权组）；缺省/空白 → `422` `api.error.assetLibrary.groupCodeRequired`；越权组 → `403`。
  - 上传 `SEAL` → 仅 GLOBAL / GROUP（吸收原 `TEMPLATE_APPROVER` SEAL 特权），且同样组作用域。
  - 停用 → 仅 GLOBAL / GROUP，目标身份 `(groupCode, assetKey)`，且对 `groupCode` 有授权；越权 → `403`（无存在性 oracle）。
- `AUDIT_ADMIN` / `LEGAL_REVIEWER` **无**资产库路由；经 §10 审计查询看 `ASSET_LIBRARY_*`（含 `ASSET_LIBRARY_MIGRATE_QUARANTINE`）事件。
- 渲染解析：模板 `imageRef`/`sealRef`（裸 `assetKey`）仅在该模板拥有组内存在 **ACTIVE** `(groupCode, assetKey)` 时成功；跨组 ACTIVE **不得**满足解析（fail-closed，既有 not-found `messageKey` 家族）。
- **本切片非目标：** Binding editor 重排；Auto `referenceKey`。
- 行为 SoT：[asset-library-group-isolation.md](../behavior/asset-library-group-isolation.md) `BDD-ALGI-001…018`（权威）；历史基线：[ce-e02-asset-library.md](../behavior/ce-e02-asset-library.md) §15 Amendment ALGI + `BDD-CE-E02-001…022`。

**CE-G01 同人审批阻断 / 例外干预（2026-07-14；Wave 5 角色修正）：**

- 同人阻断对**全部**角色生效（含 `GLOBAL_ADMIN` 自提自批）：模板 `recordApprovalDecision`（**含 IBL-E3 每一审批阶段**）、母版 `decideReview`、条款 `APPROVE_REVIEW`/`REJECT_REVIEW` 在决策执行人 username 与最近一次 `SUBMIT_FOR_APPROVAL`（或对应提交）提交人 username 精确相等时 fail-closed 返回 `403 SELF_APPROVAL_FORBIDDEN`（`api.error.lifecycle.selfApprovalForbidden`）。LEGAL 通过**不**重置提交人——COMPLIANCE 仍比对该次提交审批的 submitter。
- **例外干预权仅** `GROUP_ADMIN` / `GLOBAL_ADMIN`；必须 `exceptionIntervention=true` + 非空 `exceptionReason` + `secondaryConfirmed=true`。`LEGAL_REVIEWER` / `DOCUMENT_AUTHOR` / `TEMPLATE_TESTER` **无**例外权。原纯审批人员因迁移为 `GROUP_ADMIN` 而**获得**例外权（ADR-0070 已接受的特权扩展）。
- 例外成功决策在生命周期审计行永久保留 `selfApprovalException=true` + `exceptionReason`；读取权限沿用既有 `AUDIT_ADMIN` / `GLOBAL_ADMIN` / `GROUP_ADMIN(+groupScope+templateId)` 矩阵（§10），本片不放宽。
- 不做四眼双人复核（ADR-0021 维持；用户 D1 拍板）。

**CE-G03 测试数据 PII 治理（2026-07-15）：**

- **无新角色 / 无新 capability bit。** `piiCategory` 读写复用「配置模板变量」；测试集 `piiHandling`（`SYNTHETIC` 或 `EXPLICIT_SENSITIVE`）复用「维护模板测试数据集」。
- `EXPLICIT_SENSITIVE` **不**要求 `GROUP_ADMIN`（与 G01 例外干预不同）；凡具备测试集维护权的角色均可走确认路径。
- 测试人员 / 法务审阅人仍只读；不可维护 schema PII 标签或测试集。
- `EXPLICIT_SENSITIVE` 成功审计摘要可读性沿用既有 `readAudit` + 组范围；审计**不得**含变量明文。行为 SoT：[ce-g03-testdata-pii.md](../behavior/ce-g03-testdata-pii.md)。

**CE-G06 受控再生（2026-07-16）：**

- **无新 capability bit。** 再生授权复用管理员矩阵 + `readAudit` 可见边界：`GLOBAL_ADMIN`、同组 `GROUP_ADMIN`、模板可见范围内 `AUDIT_ADMIN`。
- `DOCUMENT_AUTHOR` / `TEMPLATE_TESTER` / `LEGAL_REVIEWER` / 调用方 **禁止** regenerate（403 fail-closed）。
- 再生**内部**可读 `parametersStorage`（ADR-0057 授权的留存例外，含 IBL-A5 收窄后形态）；响应、审计、管理 UI **仍禁止** variables / 加密密码明文（HIST C6 不放宽）。见 §11 ADR-0057 条。
- 行为 SoT：[ce-g06-audit-reproducible.md](../behavior/ce-g06-audit-reproducible.md)；ADR：[0057-invocation-parameters-retention-for-regenerate.md](../adr/authorization-security/0057-invocation-parameters-retention-for-regenerate.md)。

**PD-6 真·无 SPECIMEN 生产重发（2026-07-20；扩展 CE-G06，非新 ADR）：**

- **无新 capability bit / 无新角色。** 仍走同一 regenerate API；默认模式（无 opt-in）权限与 SPECIMEN 强制**不变**（G06-C8 / G06-C13）。
- **生产重发角色收窄：** 仅 `GLOBAL_ADMIN` 与同组 `GROUP_ADMIN`。`AUDIT_ADMIN` 可继续默认 SPECIMEN regenerate，但 `productionReissue=true` → **403** fail-closed。
- **Fail-closed opt-in：** `productionReissue=true` 时必须非空 `reason`；否则 400 `PRODUCTION_REISSUE_REASON_REQUIRED`（`api.error.audit.productionReissueReasonRequired`）。缺 flag / `false` → 审计样件路径。
- Preview / test-generate / runtime formal generate：**零放宽**（preview/test 仍强制 SPECIMEN；formal 仍无水印且本叶不改）。
- 行为 SoT：[pd6-true-non-specimen-reissue.md](../behavior/pd6-true-non-specimen-reissue.md) `BDD-PD6-001…018`；契约：[contract-outline.md](../api/contract-outline.md) «审计可复现受控再生» / OpenAPI `ManagementInvocationRegenerateRequest`。**不**翻转 #3b/#5a；**不**宣称 go-live。

**IBL-A5 PII 留存脱敏（2026-07-18）：**

- **无新角色 / 无新 capability bit。** 写路径按版本 `piiCategory` 收窄 ADR-0057 Store；再生权限矩阵不变。
- 禁止类明文不落库；调用方 detail 仅见脱敏后 parameters；管理端仍禁 parameters（HIST C6）。
- 不翻转 checklist **#3b** / **#5a**；encryption-at-rest 仍 deferred。
- 行为 SoT：[ibl-a5-pii-retention-redaction.md](../behavior/ibl-a5-pii-retention-redaction.md)；见 §11 ADR-0057 Amendment。

**CE-G04 Legal hold（2026-07-16）：**

- 新逻辑路由 `route.legal-hold-administration` → `/governance/legal-holds`（§13.1 表）；**仅 GLOBAL_ADMIN** 可见与可调用。
- **无新 capability bit**（角色硬闸门）：`GROUP_ADMIN` / `AUDIT_ADMIN` / 其他运营角色 → API **403**；路由不可见 → Forbidden。
- Hold 范围：`TEMPLATE_WINDOW`（模板 + UTC 时间窗）或 `INVOCATION_SET`（invocation external ID 集合，≤500）。
- **ACTIVE 豁免（叠加，不改 ADR-0040/0048 默认窗口正文）：** 调用记录产物/行删除；management/runtime 审计硬删按 BDD 匹配规则。**Confirmed 边界：** INVOCATION_SET **不**豁免 management 审计行；`templateId == null` 的平台级 purge-evidence **不**因 TEMPLATE_WINDOW 豁免。
- 管理审计：`LEGAL_HOLD_CREATED` / `LEGAL_HOLD_RELEASED`；禁止 variables / 凭证 / 完整参数体。
- **Out of scope：** GROUP_ADMIN 组范围 hold；eDiscovery 导出；CE-G05；go-live / CD-3。
- 行为 SoT：[ce-g04-legal-hold.md](../behavior/ce-g04-legal-hold.md) `BDD-CE-G04-001…017`；领域：[domain-model.md](../domain/domain-model.md) §2.15.1；契约：[contract-outline.md](../api/contract-outline.md) «Legal hold 管理契约（CE-G04）」。

**CE-G05 模板年检 + 条款正文 FTS（2026-07-17；Wave 5 角色修正）：**

- **无新角色 / 无新 capability bit / 无独立年检治理路由。**
- **年检：** 完成与到期待办列表要求组范围模板访问 **且** `authorTemplates`（`GLOBAL_ADMIN` / `GROUP_ADMIN` / `DOCUMENT_AUTHOR`）；对齐 CE-U07。`TEMPLATE_TESTER` / `LEGAL_REVIEWER`（默认无 `authorTemplates`）→ 待办不可见 / complete **403**。
- **FTS / where-used：** 与 §5.1 条款目录 list/get 浏览边界相同（`DOCUMENT_AUTHOR` + 管理员；**不含** `TEMPLATE_TESTER`）。跨组 fail-closed；where-used 不得泄露不可见模板行。
- **IBL-E6 深度 where-used / 嵌套写路径：** **无新 capability bit。** where-used 授权与上同；CM 结构写含嵌套校验仍复用 `authorContentModules`；publish 嵌套硬项复用既有模板编排边界。impl **Done** (#133 `dcc42c81`)。[ibl-e6-clause-nesting-governance.md](../behavior/ibl-e6-clause-nesting-governance.md)；[ADR-0067](../adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md)。
- 管理审计：`TEMPLATE_ANNUAL_REVIEW_COMPLETED`；禁止 variables / 凭证 / 条款全文。
- **Out of scope：** CD-3；CE-O02；go-live；#50；协作新 `queue_type`；独立 `/governance/annual-review` 路由；中文分词插件；高亮 snippet 作为 Done 门槛。
- 行为 SoT：[ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md) `BDD-CE-G05-001…019`；领域：[domain-model.md](../domain/domain-model.md) §2.7 / §2.9.2；契约：[contract-outline.md](../api/contract-outline.md) «模板年检与条款正文全文检索（CE-G05）」。

**IBL-E6 条款嵌套模块图治理（2026-07-20 / ADR-0067 Accepted）：**

- **无新角色 / capability。** 结构写 = `authorContentModules`；深度 where-used = §5.1 目录浏览（同 CE-G05）；发布嵌套硬项评估 = 既有模板编排 / publish-gate 边界。
- **不**因嵌套深度命中放宽跨组可见性；`TEMPLATE_TESTER` where-used 仍 **403**。
- 行为 SoT：[ibl-e6-clause-nesting-governance.md](../behavior/ibl-e6-clause-nesting-governance.md) `BDD-IBL-E6-001…018`；领域：[domain-model.md](../domain/domain-model.md) §2.9.2；契约：[contract-outline.md](../api/contract-outline.md) IBL-E6 节；[ADR-0067](../adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md)。impl **Done** (`dcc42c81` / `0e542c03`)；**不**翻转 #3b/#5a。

**PRR-D01c Dashboard summary（2026-07-18）：**

- **无新 capability bit / 无新角色 / 非 object-scope。** `GET /api/management/v1/dashboard/summary` 仅要求已认证管理会话；组范围与 catalog list 一致（§13.1.3）。
- 空组 → 零计数；跨组 fail-closed；未认证 → 401。不授予额外编辑/判定/发布权；不替代 collaboration / workflow inbox。
- 行为 SoT：[prod-dashboard-summary-api.md](../behavior/prod-dashboard-summary-api.md)；OpenAPI `getDashboardSummary`；契约：[contract-outline.md](../api/contract-outline.md) «Dashboard summary 契约（PRR-D01c）»。

### 13.3 禁止路由访问（forbidden-route）行为基线

- 已登录但无目标路由权限时返回禁止访问结果（前端阻断 + 统一无权访问反馈），并采用 fail-closed。
- 禁止访问响应不得泄露未授权资源存在性细节、未授权组详情或敏感配置明文。
- 禁止访问事件必须写入安全审计摘要，包含主体摘要、入口路由标识、判定结果和拒绝原因码。
- **耐久化（LR-D7 confirmed）：** 登录成功/失败、forbidden-route / 管理端 403、文档下载授予/拒绝须持久化为 `management_audit_event` 行（`SECURITY_*` event types），经既有 Activity log / management audit 查询按 §10 作用域可见；保留 SLF4J 摘要日志；持久化失败不得阻断登录主路径。权威场景见 [lrp-d7-durable-security-audit.md](../behavior/lrp-d7-durable-security-audit.md)。

### 13.4 已解决的前 T01 待确认项（历史）与 Wave 5 修正

| 议题 | 决策 |
| --- | --- |
| `TEMPLATE_AUTHOR` 与母版设计人员映射（历史） | 原为独立角色对；**Wave 5 / ADR-0070** 合并为 `DOCUMENT_AUTHOR`（能力并集） |
| 无权控件策略 | 隐藏（§13 已确认） |
| API 管理员角色 | 并入 GLOBAL/GROUP 管理员（§13 已确认） |
| 八角色目录 | **退役** — 可分配目录压缩为六角色（ADR-0070） |

### 13.5 管理会话治理：滑动续期与撤销（LR-B6，2026-07-04 确认并交付）

**来源：** [BDD-LRP-SESSION-001](../behavior/session-renewal-revocation.md)（会话策略由用户 2026-07-04 确认）。
边界维持 [ADR-0036](../adr/authorization-security/0036-local-account-store-authorization-authority.md)：本地账户 + JWT，**无 SSO/OIDC**；本节仅作用于管理端令牌路径，运行时 API 凭证体系不受影响。会话行为对全部 **六个** 管理端角色一致生效。

**滑动续期语义：**

- 用户活动即续期：前端在令牌临过期且近期有活动时静默调用 `POST /api/management/v1/auth/renew`，重新签发访问令牌，不打断编辑。
- 访问令牌 TTL 30 分钟（`docgen.jwt.access-token-ttl: PT30M`，env 可覆盖 `JWT_ACCESS_TOKEN_TTL`）。
- 绝对会话上限 8 小时（`docgen.session.absolute-ttl: PT8H`，env 可覆盖 `SESSION_ABSOLUTE_TTL`），自首次登录 `sessionStartedAt` 起计；新令牌过期时间 = min(now + TTL, sessionStartedAt + 8h)，达到上限后拒绝续期（`401 SESSION_ABSOLUTE_LIMIT_REACHED`），用户重新登录后上限重新起算。
- 续期必须重新校验账号仍启用（enabled、未逻辑删除），并按 login 同路径重新派生角色/分组/路由 claims——授权变更最迟 30 分钟内生效；账号停用后续期被拒绝（fail-closed）。

**撤销语义：**

- 管理令牌携带唯一 `jti`；logout 与续期成功都会将旧 `jti` 写入 Redis 撤销名单（key 前缀 `docgen:session:revoked:`，TTL = 旧令牌剩余寿命，自过期无需清理任务）。
- `JwtAuthenticationFilter` 逐请求校验撤销名单；撤销命中返回 `401 SESSION_REVOKED`——logout 从 log-only 升级为真实失效，旧令牌不可重放。

**Fail-closed：**

- 撤销名单（Redis）不可用时**拒绝请求**（`401 SESSION_VALIDATION_UNAVAILABLE`，retryable=true）；不存在「撤销校验失败仍放行」的代码路径；logout/renew 时撤销写入失败返回 503（fail-closed，前端仍清空本地会话）。
- 撤销存储 `docgen.session.revocation-store` 取值 `redis`（默认）/ `memory`（transitional-test-only）；`docgen.environment=prod` 且 store=memory 时**拒绝启动**（启动守卫已实现）。

**发布切换期存量旧令牌（legacy token）：**

- 无 `jti`/`sessionStartedAt` claim 的旧令牌一律视为无效（`401 SESSION_EXPIRED`），用户一次性重新登录即可；不做兼容放行。

实现偏差与已接受竞态（并发 renew、跨 tab 去重）见 [BDD-LRP-SESSION-001 §Implementation deviations](../behavior/session-renewal-revocation.md)。
