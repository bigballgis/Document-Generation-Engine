# BDD 行为规格：CE-E02 — 资产库管理面

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-CE-E02` |
| **编写日期** | 2026-07-16 |
| **程序** | [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7 Wave CE-E · CE-E02 |
| **Slice** | `ce-e02-asset-library` |
| **Worktree** | 已移除（merge `5bd3611e` 后） |
| **Task Master** | **#79**（**Done** — merge `5bd3611e`；MAIN doc-sync 本片） |
| **Formal phase** | **None**（CE 程序切片；不发明 sole-active 正式 P-phase） |
| **Placement** | MAIN（merge 后） |
| **上游** | CE-E01 (#78) **Done**（资产键清单 / 导入 ASSET_KEY 探测可消费本片 ACTIVE 对象）；F1-A3 `StructuredContentImageResolver` 已生产化 |
| **Owning docs** | 本文件（行为 SoT）；计划 [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7；需求 [requirements-plan.md](../requirements/requirements-plan.md)；产品 [PRD.md](../product/PRD.md)；领域 [domain-model.md](../domain/domain-model.md)；权限 [permission-matrix.md](../security/permission-matrix.md)；API [contract-outline.md](../api/contract-outline.md) + [openapi-v1.yaml](../api/openapi-v1.yaml)（实现时扩展） |
| **Frontend UI** | **In scope** — 管理端资产库页 + Playwright E2E/UIUX（用户可见面） |

**完成声明约束：** 本切片关闭「资产库无管理面」缺口（MinIO 目录 API 上传/列表/停用 + 键名约定固化 + 管理页 + 印章类上传审批角色门禁）；**不**宣称 go-live；**不**激活 CD-3；**不**实现 CE-E03 全库导出、CE-O01 PDF/A；**不**修改 `StructuredContentImageResolver` 对外协议与解析顺序。

---

## 1. 概述

今日图片/签章引用（`imageRef` / `sealRef`）依赖对象存储中与引用键一致的对象，但**无**管理面目录：无法受控上传、列表、停用；键名约定未固化；印章类资产缺少审批角色门禁。CE 北星「可迁移 / 契约诚信」要求：平台具备可审计的资产目录，且渲染解析器协议保持稳定，使 E01 资产键探测与运行时解析继续成立。

| 行为域 | 摘要 |
| --- | --- |
| **E02-S1 键名约定** | 固化逻辑 `assetKey` ≡ MinIO 可解析对象键；类前缀建议；校验规则 |
| **E02-S2 目录 API** | 管理 API：上传 / 分页列表 / 停用 |
| **E02-S3 印章门禁** | `assetClass=SEAL` 上传需审批角色（或管理员） |
| **E02-S4 管理页** | 银行 OA 管理端资产库页：列表、上传、停用 |
| **E02-S5 解析器不变** | `StructuredContentImageResolver` 协议与解析顺序 **不变** |

**现状证据（implementation 输入，非已验收行为）**

| 发现 | 证据 |
| --- | --- |
| Resolver 按引用键直接查 MinIO（可选 `.png`/`.jpg`/`.jpeg`） | `StructuredContentImageResolver.candidateStorageKeys` |
| 演示键 `IMG-1` / `SEAL-1` | demo 绑定 + F1 测试 |
| 无 library/assets 管理 API / 路由 | OpenAPI / `routeKeys.ts` 无资产库面 |
| E01 ASSET_KEY 仅探测存在性 | `ce-e01-export-bundle-v2.md` E01-C14 |

---

## 2. Actor / Role

| Actor | 角色 | 关注点 |
| --- | --- | --- |
| **GLOBAL_ADMIN** | 全局管理员 | 上传任意类、列表、停用；看见路由 |
| **GROUP_ADMIN** | 分组管理员 | 同上（平台级共享库；本片无组分片） |
| **MASTER_DESIGNER** | 母版设计人员 | 列表；上传 **IMAGE**（非 SEAL） |
| **TEMPLATE_AUTHOR** | 模板编排人员 | 列表；上传 **IMAGE**；在绑定中引用 `assetKey` |
| **TEMPLATE_APPROVER** | 审批人员 | 列表；上传 **SEAL**（印章类）；不可停用（除非兼管理员） |
| **TEMPLATE_TESTER** | 测试人员 | **只读列表** ACTIVE（便于核对引用键）；不可上传/停用 |
| **AUDIT_ADMIN** | 审计管理员 | **无**资产库路由；经既有审计查询看上传/停用事件 |
| **系统** | 目录服务 + MinIO + 审计 | 写对象与目录行；停用后移除可解析键；fail-closed 授权 |
| **渲染引擎** | `StructuredContentImageResolver` | **消费方不变**：仍按引用键查对象存储 |

---

## 3. Goal

1. 授权主体可将图片/签章二进制以**固化键名**上传到对象存储，并登记目录元数据（类、状态、大小、哈希、上传者、时间）。
2. 授权主体可分页列表目录（按类/状态/键搜索过滤）；默认仅 ACTIVE。
3. 管理员可**停用**资产：目录标记 `DISABLED`，并从可解析对象键移除字节，使解析器 fail-closed（与缺失键同形）。
4. **印章类（`SEAL`）上传**仅 `TEMPLATE_APPROVER` / `GLOBAL_ADMIN` / `GROUP_ADMIN`；其它角色 `403` fail-closed。
5. 管理端提供资产库页（上传对话框、列表、停用确认）；英文优先 i18n；银行 OA 视觉。
6. **`StructuredContentImageResolver` 的对外方法、解析顺序、错误码协议不变**；本片只保证写入的对象键可被既有协议解析。

---

## 4. 已确认决策（confirmed）

| ID | 决策 | 来源 |
| --- | --- | --- |
| **E02-C1** | **逻辑键 = 存储键。** 目录主键字段 `assetKey`（trim 后）即写入 MinIO 的对象键；**禁止**引入 `library/` 等解析器当前不会查找的强制前缀（否则需改 resolver，本片禁止）。可选扩展名：若上传文件带 `.png`/`.jpg`/`.jpeg`，允许 `assetKey` 含扩展名，或存无扩展名键并由解析器候选后缀命中——实现须保证至少一种候选键 `exists==true`。 | F1-A3 + 「协议不变」 |
| **E02-C2** | **键名语法（固化）：** `assetKey` 匹配 `^[A-Za-z][A-Za-z0-9._-]{0,127}$`（1–128 字符；首字符字母；允许字母数字 `._-`）。拒绝空白、路径分隔 `/` `\`、`..`、控制字符 → `422` `api.error.assetLibrary.assetKeyInvalid`。 | 计划「键名约定固化」+ 现有 `IMG-1`/`SEAL-1` |
| **E02-C3** | **类前缀惯例（非强制语法，UI/文档推荐）：** `IMAGE` → `IMG-…`；`SEAL` → `SEAL-…`；`OTHER` → 自由合法键。类与前缀不一致时：**警告可记审计 INFO，不阻断**（避免破坏已有非前缀键）。 | 演示惯例 + 可测性 |
| **E02-C4** | **`assetClass` 枚举：** `IMAGE` \| `SEAL` \| `OTHER`（`UPPER_SNAKE_CASE`）。`SEAL` = 印章/签名图片资产（非密码学电子印章）。 | PRD 签章占位 + 计划卡 |
| **E02-C5** | **状态：** `ACTIVE` \| `DISABLED`。上传成功 → `ACTIVE`；停用 → `DISABLED`（单向；本片**不**提供独立 re-enable API——若需恢复，对同键重新上传覆盖并置 `ACTIVE`）。 | 计划「停用」 |
| **E02-C6** | **停用语义：** 更新目录 `DISABLED` + **删除** MinIO 上该 `assetKey` 及其实现写入的候选扩展键（若曾写）；随后 `StructuredContentImageResolver` 对引用该键的渲染 → 既有 `IMAGE_ASSET_NOT_FOUND` / `SEAL_ASSET_NOT_FOUND`。不改 resolver。 | 协议不变 + fail-closed |
| **E02-C7** | **上传冲突：** 同 `assetKey` 已 `ACTIVE` → `409` `api.error.assetLibrary.assetKeyConflict`。同键 `DISABLED` → 允许重新上传：覆盖对象、目录复位 `ACTIVE`、写审计 `ASSET_LIBRARY_REUPLOAD`。 | 可运维 |
| **E02-C8** | **MIME / 大小：** 允许 `image/png`、`image/jpeg`（含 `.jpg`/`.jpeg`）；其它 → `422` `api.error.assetLibrary.contentTypeUnsupported`。应用层单文件上限 **5 MiB** → `422` `api.error.assetLibrary.payloadTooLarge`（与 OpenAPI 一致；nginx/Spring multipart 边界超限仍可能为 413，须可读可翻译）。魔数与声明 content-type 不一致 → `422` `api.error.assetLibrary.contentTypeMismatch`。 | 银行图片资产 + LR-A3 思路缩小 |
| **E02-C9** | **API（管理前缀）：** `GET /api/management/v1/library/assets`（分页 `page`/`size`，过滤 `assetClass`/`status`/`q`）；`POST /api/management/v1/library/assets`（`multipart/form-data`：`file`、`assetKey`、`assetClass`）；`POST /api/management/v1/library/assets/{assetKey}/disable`。统一 envelope。`Idempotency-Key` 于 POST 上传为**预留头、本片不强制/不生效**（可传、服务端忽略；无 claim/replay/dedup）。 | 计划卡 upload/list/disable |
| **E02-C10** | **列表默认：** `status=ACTIVE`（省略时）；`status=DISABLED` 或 `ALL` 显式查询。返回项至少：`assetKey`、`assetClass`、`status`、`contentType`、`sizeBytes`、`contentSha256`（小写 hex）、`originalFileName`、`uploadedBy`、`uploadedAt`。**不**返回二进制。 | 可测 |
| **E02-C11** | **权限 / 角色矩阵（本片确认）：** 见 §4.1；新 capability `manageAssetLibrary` + 路由 `route.asset-library-management`；印章上传额外角色门禁在服务层 fail-closed。 | 计划「印章类上传需审批角色」 |
| **E02-C12** | **作用域：** 本片资产库为**平台共享目录**（无 `groupId` 分片）。后续组分片不在本片。`GROUP_ADMIN` 与 `GLOBAL_ADMIN` 对目录操作权等同（停用/全类上传）。 | M 量级收敛 |
| **E02-C13** | **`StructuredContentImageResolver` 不变：** 不得修改 `resolveImageRef` / `resolveSealRef` 签名、`ResolvedImage` 形态、MinIO→demo classpath→fail-closed 顺序、错误码/`messageKey`。本片测试须含回归：既有 `IMG-1`/`SEAL-1` 解析与缺失 fail-closed 仍绿。 | handoff hard constraint |
| **E02-C13a** | **N23 / Wave 8（docs lock，不改本片 API）：** classpath `rendering/demo-images/`（及 `DOCGEN_DEMO_CLASSPATH_IMAGE_TIER_ENABLED`）仅为 LAB/test **渲染回退**，**不是** Asset Library 目录内容。管理页 `/library/assets` **仅**列出 managed `library_asset` 行。产品默认零资产 = **honest empty**；可选 demo/验收 managed-asset seed 见 [demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md) 与 [sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md)（W8-C1…C3）。 | SYS-NORM Wave 8 / N23 |
| **E02-C14** | **管理页：** 新路由 canonical `/library/assets`；逻辑键 `route.asset-library-management`；列表 + 上传对话框（类选择、键输入、文件）+ 停用确认；英文优先；隐藏无权限控件；直链无权限 → Forbidden。 | 计划「管理页」+ 矩阵 §13 |
| **E02-C15** | **审计：** 成功上传 → `ASSET_LIBRARY_UPLOAD`；停用 → `ASSET_LIBRARY_DISABLE`；DISABLED 键再上传 → `ASSET_LIBRARY_REUPLOAD`。摘要含 `assetKey`/`assetClass`/`actor`/`contentSha256`；**无**文件字节。失败授权不泄露对象是否存在（统一 403）。 | 可观测 |
| **E02-C16** | **E01 兼容：** ACTIVE 键对 E01 ASSET_KEY 探测为存在；DISABLED 后为缺失（blocking）。本片**不**改导出包嵌入资产二进制（仍属未来/非 E03 本片范围外）。 | E01-C14 |
| **E02-C17** | **明确非目标：** CE-E03 全库导出；CE-O01 PDF/A；改 resolver 协议；密码学电子签章；病毒扫描引擎（可记 pending ops）；硬删除历史归档 UI；组分片资产库；并行 multitask。 | handoff OOS |
| **E02-C18** | **FE / E2E / UIUX：本片 in scope。** 至少覆盖：授权用户打开资产库页并看到列表；IMAGE 上传成功行出现；SEAL 上传对 AUTHOR 拒绝/对 APPROVER 成功；停用后列表状态与后续渲染缺失一致（API 或 UI 可观测）。 | Task testStrategy |

### 4.1 权限矩阵（confirmed）

| 动作 | GLOBAL | GROUP | MASTER_DESIGNER | TEMPLATE_AUTHOR | TEMPLATE_TESTER | TEMPLATE_APPROVER | AUDIT_ADMIN |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 列表（含 DISABLED 查询） | ✓ | ✓ | ✓ ACTIVE 默认；可查 DISABLED | ✓ ACTIVE 默认；可查 DISABLED | ✓ **仅 ACTIVE** | ✓ ACTIVE 默认；可查 DISABLED | — |
| 上传 `IMAGE` / `OTHER` | ✓ | ✓ | ✓ | ✓ | — | — | — |
| 上传 `SEAL` | ✓ | ✓ | — | — | — | ✓ | — |
| 停用 | ✓ | ✓ | — | — | — | — | — |
| 路由 `route.asset-library-management` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |

**Capability：** `manageAssetLibrary=true` 当角色 ∈ {GLOBAL_ADMIN, GROUP_ADMIN, MASTER_DESIGNER, TEMPLATE_AUTHOR, TEMPLATE_TESTER, TEMPLATE_APPROVER}（用于路由可见与列表入口）。**细粒度动作**仍按上表在服务层强制（测试人员有 capability 但上传/停用 403）。

---

## 5. 前置条件

- CE-E01 (#78) 已合并（软依赖满足）：导入 ASSET_KEY 探测可用。
- F1-A3：`StructuredContentImageResolver` + `ObjectStoragePort` 可用。
- MinIO（或等价）在验收环境可写。
- 本切片在隔离 worktree 交付；不在 MAIN 实现。

---

## 6. Trigger

- 授权用户打开管理端 **Asset library**（`/library/assets`）。
- 授权用户 `POST …/library/assets` 上传；`GET …/library/assets` 列表；`POST …/library/assets/{assetKey}/disable` 停用。
- 模板绑定 `imageRef`/`sealRef` 引用 `assetKey` 后预览/生成（消费既有解析器，验证本片写入兼容）。

---

## 7. Primary journey

### 7.1 上传 IMAGE 并在列表可见

1. `TEMPLATE_AUTHOR`（或管理员/母版设计）打开资产库页 → 上传对话框选 `IMAGE`、键 `IMG-LOGO-BANK`、PNG 文件。
2. 系统校验键/MIME/大小 → 写 MinIO（键可被 resolver 命中）→ 目录 `ACTIVE` → 审计 `ASSET_LIBRARY_UPLOAD`。
3. 列表刷新出现该行；模板 `imageRef=IMG-LOGO-BANK` 预览可嵌入图片。

### 7.2 审批角色上传 SEAL

1. `TEMPLATE_APPROVER` 上传 `SEAL` / `SEAL-BRANCH-01`。
2. 系统允许并登记；`TEMPLATE_AUTHOR` 对同请求 → `403`。

### 7.3 停用

1. 管理员对 ACTIVE 键停用并确认。
2. 目录 `DISABLED`；对象键不可再被 resolver/`exists` 命中；审计 `ASSET_LIBRARY_DISABLE`。
3. 使用该键的渲染 → 既有 not-found 错误。

---

## 8. System responses（success）

| 操作 | HTTP | 结果要点 |
| --- | --- | --- |
| 上传 | `201` | `result` 含目录视图；对象可 `exists` |
| 列表 | `200` | `PageView`；默认 ACTIVE |
| 停用 | `200` | `status=DISABLED`；对象不可解析 |

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-CE-E02-001 — IMAGE 上传成功且可被解析器命中

**Given** 授权 `TEMPLATE_AUTHOR` 会话；键 `IMG-E02-001` 不存在  
**When** `POST …/library/assets` multipart `assetClass=IMAGE`、合法 PNG ≤5MiB  
**Then** `201`；目录 `ACTIVE`；`ObjectStoragePort.exists("IMG-E02-001")` 或候选扩展键为 true  
**And** `StructuredContentImageResolver.resolveImageRef("IMG-E02-001")` 返回非空字节  
**And** 审计 `ASSET_LIBRARY_UPLOAD`

### BDD-CE-E02-002 — 键名非法拒绝

**Given** 授权上传主体  
**When** `assetKey` 含 `/` 或为空或超长  
**Then** `422` `api.error.assetLibrary.assetKeyInvalid`  
**And** 无 MinIO 写入、无目录行

### BDD-CE-E02-003 — 不支持 content-type

**Given** 授权上传主体  
**When** 上传 `application/pdf` 或非 png/jpeg  
**Then** `422` `api.error.assetLibrary.contentTypeUnsupported`

### BDD-CE-E02-004 — 超限拒绝

**Given** 授权上传主体  
**When** 文件 > 5 MiB（应用层校验）  
**Then** `422` `api.error.assetLibrary.payloadTooLarge`（与 OpenAPI 一致；nginx/Spring 边界超限仍可能为 413）

### BDD-CE-E02-005 — ACTIVE 键冲突

**Given** `IMG-E02-005` 已 `ACTIVE`  
**When** 再次上传同键  
**Then** `409` `api.error.assetLibrary.assetKeyConflict`

### BDD-CE-E02-006 — DISABLED 键允许再上传激活

**Given** `IMG-E02-006` 为 `DISABLED` 且对象已移除  
**When** 再次上传同键合法 PNG  
**Then** `201`；`ACTIVE`；对象可解析  
**And** 审计 `ASSET_LIBRARY_REUPLOAD`

### BDD-CE-E02-007 — SEAL 上传需审批角色

**Given** 仅 `TEMPLATE_AUTHOR` 会话  
**When** 上传 `assetClass=SEAL`  
**Then** `403` fail-closed（不写对象）

### BDD-CE-E02-008 — SEAL 上传审批角色成功

**Given** `TEMPLATE_APPROVER`（或 GLOBAL/GROUP admin）会话  
**When** 上传 `SEAL` / `SEAL-E02-008`  
**Then** `201`；`resolveSealRef("SEAL-E02-008")` 成功

### BDD-CE-E02-009 — 管理员上传 SEAL 成功

**Given** `GLOBAL_ADMIN` 或 `GROUP_ADMIN`  
**When** 上传 `SEAL`  
**Then** `201`（管理员不受审批角色限制）

### BDD-CE-E02-010 — 列表默认 ACTIVE 分页

**Given** 库中同时有 ACTIVE 与 DISABLED 行  
**When** `GET …/library/assets` 不带 `status`  
**Then** `200`；仅 ACTIVE；分页元数据正确

### BDD-CE-E02-011 — 列表过滤 class 与 q

**Given** 多条不同 class/key  
**When** `assetClass=SEAL&q=SEAL-E02`  
**Then** 仅匹配项返回

### BDD-CE-E02-012 — 测试人员只读 ACTIVE

**Given** `TEMPLATE_TESTER`  
**When** 列表  
**Then** `200` 仅 ACTIVE  
**When** 上传或停用  
**Then** `403`

### BDD-CE-E02-013 — 停用移除可解析对象

**Given** ACTIVE `IMG-E02-013` 可解析  
**When** 管理员 `POST …/disable`  
**Then** `200`；目录 `DISABLED`  
**And** resolver `resolveImageRef` → `IMAGE_ASSET_NOT_FOUND`  
**And** 审计 `ASSET_LIBRARY_DISABLE`

### BDD-CE-E02-014 — 非管理员停用拒绝

**Given** `TEMPLATE_AUTHOR` 或 `TEMPLATE_APPROVER`  
**When** 停用  
**Then** `403`

### BDD-CE-E02-015 — 无路由角色直链 Forbidden

**Given** `AUDIT_ADMIN` 会话  
**When** 打开 `/library/assets` 或调用 list API  
**Then** 前端 Forbidden / API `403`；无目录数据泄露

### BDD-CE-E02-016 — Resolver 协议回归（不变）

**Given** 本片代码合并后  
**When** 运行既有 `StructuredContentImageResolverTest` 与缺失键 fail-closed 用例  
**Then** 全部保持绿；方法签名与错误 `messageKey` 未改

### BDD-CE-E02-017 — E01 ASSET_KEY 探测兼容

**Given** ACTIVE 键已上传；另一键已 DISABLED  
**When** E01 风格存在性探测（或等价 `exists`）  
**Then** ACTIVE → 存在；DISABLED → 不存在

### BDD-CE-E02-018 — 管理页 IMAGE 上传旅程（E2E）

**Given** 授权作者浏览器会话（Docker 验收栈）  
**When** 打开 Asset library → 上传 IMAGE → 确认  
**Then** 列表出现新行；无控制台致命错误；双品牌 UIUX Critical=0（抽样）

### BDD-CE-E02-019 — 管理页 SEAL 门禁（E2E）

**Given** 作者会话打开上传对话框选 SEAL  
**When** 尝试提交  
**Then** UI 拒绝或展示无权错误，列表不新增 SEAL 行  
**Given** 切换审批员会话  
**When** 上传同一 SEAL  
**Then** 列表出现 SEAL 行

### BDD-CE-E02-020 — 管理页停用旅程（E2E）

**Given** 管理员会话与一条 ACTIVE 行  
**When** 停用并确认  
**Then** 行状态为 DISABLED（或默认列表不再显示）；API 侧对象不可解析

### BDD-CE-E02-021 — 越权上传 OTHER 拒绝（测试员）

**Given** `TEMPLATE_TESTER`  
**When** `POST` 上传 `OTHER`  
**Then** `403`

### BDD-CE-E02-022 — 魔数与 Content-Type 不一致

**Given** 授权上传主体  
**When** 声明 `image/png` 但字节非 PNG/JPEG 魔数  
**Then** `422` `api.error.assetLibrary.contentTypeMismatch`

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 并发上传同键 | 一胜一 `409`；不得双 ACTIVE 目录行 |
| 停用已 DISABLED | **已确认**幂等 `200`：目录保持 `DISABLED`；可解析对象再检查删除 |
| 停用不存在键 | `404` `api.error.assetLibrary.assetNotFound`（对有权管理员） |
| 无权限探测键 | `403`，不区分存在与否 |
| 空文件 0 字节 | `422` |
| 解析器 demo classpath tier | 本片不改；生产 profile 保持关闭；**≠** Asset Library 目录（N23 — Wave 8） |
| 零 managed 资产 | 产品默认 honest empty（Wave 8）；可选 demo/验收 seed 见 ops 文档 |
| 模板仍引用已停用键 | 渲染/预览 fail-closed（既有错误）；本片不自动改绑定 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API envelope | 上传/列表/停用 result 字段 |
| MinIO / `exists` | ACTIVE 真；DISABLED 假 |
| Resolver | 字节嵌入或 not-found |
| DB 目录表 | 状态与元数据 |
| 审计 | UPLOAD / DISABLE / REUPLOAD |
| 自动化 | 后端测试 BDD-001…017/021/022；FE Vitest；Playwright E2E 018–020；`mvn verify` + frontend gates |
| UI | `/library/assets` 列表与对话框 |

---

## 12. Traceability

| 文档 | 关系 |
| --- | --- |
| [core-excellence-program-2026-07.md](../plan/core-excellence-program-2026-07.md) §7 CE-E02 | 计划卡 |
| Task Master **#79** | 执行叶 |
| [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md) | 上游 ASSET_KEY 探测 |
| [core-fortress-f1-rendering-correctness.md](./core-fortress-f1-rendering-correctness.md) F1-A3 | Resolver 协议基线 |
| [requirements-plan.md](../requirements/requirements-plan.md) | 需求确认扩展 |
| [PRD.md](../product/PRD.md) | 产品确认扩展 |
| [domain-model.md](../domain/domain-model.md) | 领域资产目录 |
| [permission-matrix.md](../security/permission-matrix.md) | 路由/capability/动作矩阵 |
| [contract-outline.md](../api/contract-outline.md) / [openapi-v1.yaml](../api/openapi-v1.yaml) | 管理 API（CE-E02 paths + schemas） |

---

## 13. TDD Red 映射（建议）

| 层 | 建议失败测试 |
| --- | --- |
| Backend upload | `uploadImage_storesObjectAndCatalog_active`；`upload_invalidKey_422`；`upload_unsupportedType_422`；`upload_tooLarge`；`upload_activeConflict_409`；`upload_disabledKey_reactivates`；`upload_magicMismatch_422` |
| Backend seal authz | `uploadSeal_forbiddenForAuthor`；`uploadSeal_allowedForApprover`；`uploadSeal_allowedForAdmin` |
| Backend list/disable | `list_defaultsActive`；`list_filterClassAndQuery`；`tester_listActiveOnly_uploadForbidden`；`disable_removesResolvableObject`；`disable_forbiddenForAuthor` |
| Resolver regression | 既有 `StructuredContentImageResolverTest` 全绿（无签名变更） |
| Contract | OpenAPI library assets paths + enums |
| Frontend | 资产库页加载；上传表单校验；停用确认 |
| E2E | `CE-E02-asset-library.spec.ts`：list/upload IMAGE；SEAL gate；disable |

---

## 14. Handoff

```
bdd_readiness: ready
task_ids: ["79"]
slice: ce-e02-asset-library
behavior_doc: docs/behavior/ce-e02-asset-library.md
status: Done
merge: 5bd3611ee7e03b385caf3003296aee1bd604222e
frontend_ui_in_scope: true
structured_content_image_resolver: UNCHANGED
formal_phase: None
next_sole_active_recommend: #81 CE-O01 (pending/parked — do not activate in this sync)
```

**Handoff（Done）：** Task Master **#79** → **Done**（merge `5bd3611e`）。资产库 MinIO 目录 API + 管理端 `/library/assets` 已交付；resolver 协议不变。正式 phase 保持 **None**；不宣称 go-live；不激活 CD-3。下一 sole-active 建议：**#81** CE-O01（pending，勿提前激活）。
