# BDD 行为规格：IBL-E5 — `effectiveFrom` 发布门禁 + bulk re-pin（F27 半幅）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-E5` |
| **编写日期** | 2026-07-20 |
| **程序 / 队列** | IBL Wave E · **IBL-E5** / F27（`effectiveFrom` + bulk 半幅；文档品牌半幅已由 E4 关闭） |
| **Slice** | `ibl-e5-effectivefrom-bulk-repin` |
| **Branch** | `feat/ibl-e5-effectivefrom-bulk-repin` |
| **Worktree** | `D:/working/DGE-ibl-e5-effectivefrom-bulk-repin` |
| **Placement** | ISOLATED |
| **Base** | `d9f02036`（IBL-E4 Done docs tip on main） |
| **Task Master** | **#132** IBL-E5 — Batch Recommendation **solo**；`member_task_ids: ["132"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-e5-effectivefrom-bulk-repin`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；[ADR-0066 Accepted](../adr/template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md)（Decision = E5-C*；doc-keeper stage 3 Accept 2026-07-20；Accepted ≠ impl Done）；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F27 / IBL-E5；CE-K08 [ce-k08-clause-legal-metadata.md](./ce-k08-clause-legal-metadata.md)（本叶**修正** K08-C6 / LM-011）；CE-U07 [ce-u07-clause-outdated-bump.md](./ce-u07-clause-outdated-bump.md)（单模板 bump；本叶跨模板 bulk）；E4 正交 [ibl-e4-entity-document-brands.md](./ibl-e4-entity-document-brands.md)；domain §2.9.2 / publish gate；API [contract-outline.md](../api/contract-outline.md) / [openapi-v1.yaml](../api/openapi-v1.yaml)；permission-matrix 既有 `authorTemplates` |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（owner = **backend-engineer**；API-first 发布门禁 + bulk 工具；管理 UI / Playwright **not required**；E2E/UIUX **N/A**） |

**完成声明约束：** 本叶关闭 F27 剩余半幅——(1) 未来 `effectiveFrom` 在模板发布时**硬阻断**；(2) 组内跨模板 **bulk re-pin** 管理 API（dry-run + 审计）。**不**交付延迟发布 / `SCHEDULED` 模板生命周期状态机。「scheduled」仅指作者可在 CM 版本上**预填**未来 `effectiveFrom`（CE-K08 写路径保留），发布必须等到窗口打开。**SPECIMEN 水印不得在本叶移除**（PD-6 **OUT**）。**PD-7** 授权字体 **OUT**。**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 go-live / Wave E / IBL 程序 Done；**禁止**激活 IBL-E6/E7 / #119。E4 文档品牌语义**不**改写。

```
bdd_readiness: ready
frontend_ui_in_scope: false
open_questions: []
owning_doc: docs/behavior/ibl-e5-effectivefrom-bulk-repin.md
task_ids: ["132"]
adr_status: Accepted — docs/adr/template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md (Decision = E5-C*; Accepted ≠ E5 impl Done)
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["132"]
  proposed_slice_id: ibl-e5-effectivefrom-bulk-repin
  shared_acceptance_surface: >
    Future effectiveFrom hard-blocks template publish;
    group-scoped bulk re-pin management API with dry-run + audit
  vetoes_applied:
    - IBL-E6-nesting-governance
    - IBL-E7-RTL-spike
    - PD-6-specimen-removal
    - PD-7-licensed-fonts
    - IBL-B7-Word-host
    - umbrella-106-registry-only
    - checklist-3b-5a-go
    - frontend-bulk-ui-required
    - deferred-scheduled-publish-lifecycle
  evidence_amortization: mvn verify + queued docker (FE gates only if incidental OpenAPI client types; no Playwright mandate)
```

| IN（本叶） | OUT（明确禁止 / 后续叶） |
| --- | --- |
| 发布硬门禁：引用 CM 版本 `effectiveFrom` 未到 → FAIL（新 check code） | **PD-6** 去 SPECIMEN / true re-issue |
| 修正 CE-K08「未来 effectiveFrom 不阻断」立场（见 §4 / CE-K08 修订指针） | **PD-7** 授权字体；**#119** Word |
| Bulk re-pin 管理 API：筛选 + dry-run + apply + 审计 | **IBL-E6** 嵌套图；**IBL-E7** RTL |
| 复用 `upsertReference` 语义于**草稿**模板版本 | 管理 UI bulk 控制台（residual；**非** Done 条件） |
| 权限复用 `authorTemplates`（无新角色） | 延迟发布 / `SCHEDULED` 生命周期状态 |
| Gates：`mvn verify` + queued deploy evidence；**无**强制 FE E2E | 翻转 **#3b/#5a**；go-live；宣称 Wave E Done |
| ADR-0066 Accepted；Decision = E5-C* | 改写 E4 DocumentBrand / LegalEntity |

---

## 1. 概述

### 1.1 问题（F27 剩余半幅）

| 发现 | 证据 |
| --- | --- |
| `effectiveFrom` 未来日期在发布时不强制 | CE-K08 K08-C6 / BDD-CE-K08-LM-011；程序 F27 |
| 无跨模板 bulk re-pin / mass-migration（仅单模板 CE-U07 bump / 单包 import-export） | F27；CE-U07 面板级 |
| 国际信函需：未生效条款不得随模板发布；条款升版后可安全批量改钉 | 程序 IBL-E5 验收草案 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **E5-S1 Publish not-started** | 发布门禁硬项：钉扎 CM 版本 `effectiveFrom != null && utcNow.isBefore(effectiveFrom)` → FAIL |
| **E5-S2 Orthogonal expiry** | 与既有 `CONTENT_MODULE_EFFECTIVE_EXPIRED`（`effectiveTo`）并存、正交 |
| **E5-S3 Bulk select** | 组范围内按 `contentModuleId` + 源/目标语义版本（或 latest approved）选择候选草稿引用 |
| **E5-S4 Dry-run** | `dryRun=true`：零持久化；返回将变更/跳过项清单 |
| **E5-S5 Apply + audit** | `dryRun=false`：对可写草稿逐条 re-pin；写管理审计（含 dryRun=false、前后 pin、结果） |
| **E5-S6 API-first** | 无强制管理 UI；无新 capability bit |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板编排人员 / 母版设计人员 / 组管 / 全局管** | `authorTemplates` 组范围 | 发布门禁可见失败项；调用 bulk re-pin API |
| **审批 / 测试 / 法务审阅** | 既有生命周期角色 | **无** bulk 写权（除非另具 `authorTemplates`） |
| **条款作者** | `authorContentModules` | 继续在草稿 CM 版本设置未来 `effectiveFrom`（「schedule」元数据） |
| **系统** | `PublishGateService` + bulk re-pin 服务 + 审计 | Fail-closed 门禁；dry-run 隔离；审计摘要 |

---

## 3. Goal

1. 关闭 F27 剩余半幅：未来 `effectiveFrom` **正确阻断**发布；bulk re-pin **带 dry-run + 审计**。  
2. 修正 CE-K08「未来 effectiveFrom 不阻断」——以**新**硬项完成，不复用 `CONTENT_MODULE_EFFECTIVE_EXPIRED`。  
3. Bulk 仅改**草稿**钉扎；锁定/已发布版本跳过且可观测。  
4. API-first（`frontend_ui_in_scope=false`）。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave E Done；PD-6/7 **OUT**。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（E5-C* — 保守银行级默认 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **E5-C1** | **「blocked or scheduled」锁定为硬阻断 + 元数据预填：** 作者可在 CM **草稿**版本写入未来 `effectiveFrom`（既有 CE-K08 写路径 = 「schedule」意图）。模板发布时若任一钉扎版本尚未生效 → **硬阻断**。**拒绝**引入模板 `SCHEDULED` / 延迟激活生命周期状态。 | 程序验收「blocked or scheduled」；对齐 `effectiveTo` fail-closed 模式 |
| **E5-C2** | **未生效判定（UTC Instant）：** `effectiveFrom != null && utcNow.isBefore(effectiveFrom)` → 门禁 FAIL。`effectiveFrom == null` → 本项不失败（「无起始」= 已可发布）。`utcNow` **等于** `effectiveFrom` → **PASS**（与 CE-K08 `effectiveTo` 相等不过期同构）。 | K08-C4/C6 同构瞬时语义 |
| **E5-C3** | **新 check code：** `PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_NOT_STARTED`（名称以实现/OpenAPI 固定；稳定 messageKey）。**禁止**把未生效塞进 `CONTENT_MODULE_EFFECTIVE_EXPIRED`。 | 可观测正交 |
| **E5-C4** | **门禁作用面：** `GET …/publish-gate` 与执行 publish **同一**求值；硬项 FAIL → 禁止发布。汇总视图列出 offending `referenceKey` + CM id + semanticVersion + `effectiveFrom`。 | 既有 publish-gate UX/API |
| **E5-C5** | **与过期项 AND：** 同一版本可同时 FAIL `…_EXPIRED` 与 `…_NOT_STARTED`（若数据异常区间）；正常数据二者互斥。其它硬项（含 `CONTENT_MODULE_REFERENCES`）不变。 | fail-closed |
| **E5-C6** | **运行期：** 已发布锁定版本**不**因事后时钟越过/未越过 `effectiveFrom` 而新增 runtime 失败（钉扎不可变；与 CE-K08 LM-012 过期政策同构——阻断只作用于**新发布**）。 | K08 运行期稳定 |
| **E5-C7** | **CE-K08 修正：** K08-C6 / BDD-CE-K08-LM-011「未来 effectiveFrom 不阻断」**被本叶取代**（见 ce-k08 文件修订指针）。字段类型、目录筛选、`effectiveTo` 门禁**不变**。 | document-as-code 冲突消除 |
| **E5-C8** | **Bulk 目标：** 组范围内模板**草稿**版本上，将指定 `contentModuleId` 的钉扎从 `fromSemanticVersion`（可选：匹配当前 pin）改为 `toSemanticVersion`（或显式 `useLatestApproved=true`）。 | F27 mass-migration；CE-U07 扩展 |
| **E5-C9** | **API 形态（最小契约）：** 管理 API `POST /api/management/v1/content-module-references/bulk-repin`（路径以实现/OpenAPI 固定一种）。请求至少含：`groupCode`（或从会话组上下文派生——实现固定一种并写入 OpenAPI）、`contentModuleId`、目标版本选择（`toSemanticVersion` **异或** `useLatestApproved`）、可选 `fromSemanticVersion` 过滤、可选 `templateIds[]` 缩小范围、`dryRun`（布尔，**必填**）。 | API-first；防误写 |
| **E5-C10** | **候选范围：** 仅调用方 `authorTemplates` 可见且可写的**草稿**模板版本引用。已发布/锁定引用 → `SKIPPED_LOCKED`（不 500）。无匹配 pin → 不计为错误（`SKIPPED_NO_MATCH`）。目标版本非同模块 / 非 `APPROVED`+可引用 → 该项 `FAILED` 稳定码（如 `BULK_REPIN_TARGET_INVALID`）。 | CE-U07 锁定不可 bump |
| **E5-C11** | **写路径：** apply 时每条成功变更复用既有 `upsertReference`（或等价内部服务）语义与校验；**不**旁路发布锁定。 | 防第二写通道 |
| **E5-C12** | **Dry-run：** `dryRun=true` → **零** DB 变更、**零** pin 变更；响应含将 `WOULD_APPLY` / `SKIPPED_*` / `FAILED` 预览。`dryRun=false` → 持久化成功项。 | 验收「dry-run」 |
| **E5-C13** | **部分成功：** 按**模板版本**独立提交（一版本成败不影响其它）；响应含 per-item 结果 + 汇总计数。整请求鉴权失败 → 全拒绝（403/404）。 | 银行 bulk 工具惯例 |
| **E5-C14** | **审计：** 每次 API 调用写管理审计事件（建议 action `CONTENT_MODULE_BULK_REPIN`）：actor、group、`dryRun`、`contentModuleId`、from/to 选择、汇总计数；apply 成功项含 `templateId`/`templateVersionId`/`referenceKey`/beforePin/afterPin。禁止条款正文 / variables。Dry-run **也**记审计（标明 dryRun=true），便于合规回放「谁预演了什么」。 | 「audit」验收 |
| **E5-C15** | **权限：** 无新角色 / capability；调用 = `authorTemplates` 边界；越权 403/404 惯例不变。只读角色不可 apply 亦不可 dry-run（同一端点）。 | permission-matrix |
| **E5-C16** | **幂等：** 当前 pin 已等于目标 → `SKIPPED_ALREADY_AT_TARGET`（dry-run 与 apply 皆然；apply 不写无意义变更）。 | 可重复执行 |
| **E5-C17** | **Frontend：** **`frontend_ui_in_scope=false`**。CE-U07 单模板 UI **不**要求本叶扩展为跨模板控制台。 | owners = backend-engineer |
| **E5-C18** | **导入/导出：** 本叶**不**新增包格式字段；export 仍携带当前 pins。Bulk 是在线治理工具，非包迁移替代品。 | 范围控制 |
| **E5-C19** | **E1/E4 正交：** 不改 locale 变体、DocumentBrand/LegalEntity、`legalEntityCode`。 | E1/E4 Done |
| **E5-C20** | **SPECIMEN / PD-6 / PD-7：** 不改变再生/水印/字体政策。 | 程序 §8 |
| **E5-C21** | **ADR：** [ADR-0066](../adr/template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md) **Accepted**（2026-07-20；Decision = 本节 E5-C*）。Accepted ≠ E5 impl Done。 | 程序 IBL-E5 |
| **E5-C22** | **门禁：** BE `mvn verify`；queued Docker deploy evidence。无强制 Playwright。若改动 OpenAPI 生成前端类型，FE type-check/test 按需绿，仍非 UI 交付。 | delivery constitution + API-first |
| **E5-C23** | **完成边界：** E5 Done ≠ Wave E Done；≠ go-live；#3b/#5a 保持 CONDITIONAL；E6/E7 / #119 不激活；**不**翻转 #3b/#5a。 | 队列政策 |

### 4.2 明确非本叶确认（禁止当作已定产品事实）

| 项 | 状态 |
| --- | --- |
| 条款嵌套图治理 | **IBL-E6** |
| RTL spike | **IBL-E7** |
| SPECIMEN 移除 / 授权字体 / Word | PD-6/7 / #119 — **OUT** |
| 模板延迟发布状态机 / 定时自动发布 | **拒绝本叶**（E5-C1） |
| 跨组 bulk / 全局无视组边界 | **拒绝** |
| 管理 UI bulk 控制台作为 Done 条件 | **拒绝**（E5-C17） |
| Runtime 按 as-of 日期动态换 pin | **拒绝**（钉扎发布模型） |

### 4.3 ADR / 用户确认

| 问题 | 结论 |
| --- | --- |
| 是否还需用户再确认「要不要做 effectiveFrom/bulk」？ | **否** — F27 / IBL-E5 程序范围 + PD-* Wave E 边界已确认；本叶为 F27 剩余半幅 |
| 是否还需用户再确认 E5-C1…C23（硬阻断 vs 延迟发布）？ | **否（BDD ready）** — 程序「blocked or scheduled」按银行 fail-closed 锁定为 **硬阻断 + 元数据预填**；记入本 BDD |
| ADR 文件状态何时 Accepted？ | **Accepted（2026-07-20，doc-keeper stage 3）** |

---

## 5. Preconditions

- 操作者具备对应组范围 `authorTemplates`（发布评估 / bulk 调用时）。  
- #132 为本交付叶（orchestrator 已激活本切片）。  
- 模板版本存在 content-module references（钉扎语义版本）。  
- CE-K08 字段可写/可读；`effectiveTo` 门禁仍可用。  
- CE-U07 `upsertReference` 写路径可用。

---

## 6. Trigger

- 作者/编排人员评估或执行模板 publish（publish-gate）。  
- 治理调用方 `POST …/bulk-repin`（`dryRun=true|false`）。

---

## 7. Primary journey

1. 条款作者在 CM 草稿版本设置 `effectiveFrom` = 未来某 UTC Instant 并批准。  
2. 模板草稿钉扎该版本；编排人员打开 publish-gate → **CONTENT_MODULE_EFFECTIVE_NOT_STARTED** FAIL → 发布拒绝。  
3. 等待（或时钟测试）至 `utcNow >= effectiveFrom` → 该硬项 PASS（其它硬项亦满足时可发布）。  
4. 另：模块升版至新 `APPROVED` 版本后，编排人员对组内多模板草稿调用 bulk-repin `dryRun=true` 预览 → 再 `dryRun=false` 应用；审计可查。

---

## 8. System responses（成功路径）

- Publish-gate 在全部钉扎版本已生效（或 `effectiveFrom` null）且无过期时，本两项生效相关硬项 PASS。  
- Bulk dry-run 返回预览清单、无 pin 变更。  
- Bulk apply 更新可写草稿 pins；跳过锁定；审计含 dryRun=false 与 per-item 结果。

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-E5-001 — Future effectiveFrom hard-blocks publish

**Given** 模板草稿钉扎 CM 版本且 `effectiveFrom` 严格晚于 utcNow，`effectiveTo` 为空或未过期  
**When** 评估 publish-gate 或执行 publish  
**Then** 硬项 `CONTENT_MODULE_EFFECTIVE_NOT_STARTED` **FAIL**；发布被拒绝  
**And** **不**以 `CONTENT_MODULE_EFFECTIVE_EXPIRED` 表达该失败

### BDD-IBL-E5-002 — Null effectiveFrom does not trip not-started

**Given** 钉扎版本 `effectiveFrom == null` 且未过 `effectiveTo`  
**When** 评估 publish-gate  
**Then** `CONTENT_MODULE_EFFECTIVE_NOT_STARTED` **PASS**（本项）

### BDD-IBL-E5-003 — effectiveFrom equal now passes

**Given** 钉扎版本 `effectiveFrom == utcNow`（测试固定时钟）  
**When** 评估 publish-gate  
**Then** `CONTENT_MODULE_EFFECTIVE_NOT_STARTED` **PASS**

### BDD-IBL-E5-004 — Past effectiveFrom passes not-started

**Given** 钉扎版本 `effectiveFrom` 严格早于 utcNow，且未过 `effectiveTo`  
**When** 评估 publish-gate  
**Then** `CONTENT_MODULE_EFFECTIVE_NOT_STARTED` **PASS**

### BDD-IBL-E5-005 — Expiry gate remains orthogonal

**Given** 钉扎版本 `effectiveTo` 已过（`utcNow.isAfter(effectiveTo)`）  
**When** 评估 publish-gate  
**Then** `CONTENT_MODULE_EFFECTIVE_EXPIRED` **FAIL**（CE-K08 不变）  
**And** 不因本叶移除或合并该码

### BDD-IBL-E5-006 — Summary lists offending pins

**Given** BDD-IBL-E5-001 场景  
**When** GET publish-gate  
**Then** 失败摘要含至少一条 offending 引用（`referenceKey` + 模块/版本标识 + `effectiveFrom`）

### BDD-IBL-E5-007 — Runtime published version unaffected by clock

**Given** 模板版本已发布且钉扎在发布时合法的 CM 版本  
**When** 之后 utcNow 变化（或仅前进）且调用 runtime generate  
**Then** **不**因 `effectiveFrom`/`effectiveTo` 时钟新规则而新增本叶 runtime 失败（既有其它失败模式除外）

### BDD-IBL-E5-008 — Bulk dry-run previews without mutation

**Given** 组内 ≥2 个可写草稿模板版本钉扎模块 M@1.0.0，且存在可引用目标 1.1.0  
**When** `POST bulk-repin` 且 `dryRun=true`、`contentModuleId=M`、`toSemanticVersion=1.1.0`（或 `useLatestApproved=true`）  
**Then** 响应列出对应项 `WOULD_APPLY`（或等价）  
**And** 所有 pins 仍为 1.0.0  
**And** 审计事件存在且 `dryRun=true`

### BDD-IBL-E5-009 — Bulk apply re-pins drafts

**Given** 与 008 相同前置  
**When** 同一选择 `dryRun=false`  
**Then** 可写草稿项 pin → 1.1.0  
**And** 审计事件 `dryRun=false` 含 before/after pin  
**And** 响应汇总 `applied >= 1`

### BDD-IBL-E5-010 — Locked / published pins skipped

**Given** 已发布锁定模板版本钉扎 M@1.0.0，且存在目标 1.1.0  
**When** bulk-repin apply（同模块目标）  
**Then** 该项 `SKIPPED_LOCKED`；pin **不变**  
**And** 请求不因仅存在锁定项而 500

### BDD-IBL-E5-011 — Already at target skipped

**Given** 草稿已钉扎目标版本  
**When** bulk-repin dry-run 或 apply  
**Then** `SKIPPED_ALREADY_AT_TARGET`；无多余写

### BDD-IBL-E5-012 — Invalid target fail-closed per item

**Given** 请求 `toSemanticVersion` 指向不存在、非同模块、或非可引用批准版本  
**When** bulk-repin  
**Then** 该项 `FAILED` + 稳定码（如 `BULK_REPIN_TARGET_INVALID`）；其它合法草稿项仍可按 E5-C13 独立成功

### BDD-IBL-E5-013 — Unauthorized caller rejected

**Given** 调用方无目标组 `authorTemplates`  
**When** bulk-repin（无论 dryRun）  
**Then** `403` 或 `404`（既有惯例）；无 pin 变更；无成功 apply 审计

### BDD-IBL-E5-014 — fromSemanticVersion filter

**Given** 草稿 A pin M@1.0.0，草稿 B pin M@1.0.1，目标 1.1.0  
**When** bulk-repin 带 `fromSemanticVersion=1.0.0`、`dryRun=true`  
**Then** 仅 A 进入 `WOULD_APPLY`；B 为 `SKIPPED_NO_MATCH`（或等价）

### BDD-IBL-E5-015 — dryRun required

**Given** 合法调用方  
**When** bulk-repin 请求体省略 `dryRun`  
**Then** `400` / `422` 校验失败；无 pin 变更

### BDD-IBL-E5-016 — No deferred publish lifecycle

**Given** 模板草稿钉扎未来 `effectiveFrom` 的 CM 版本  
**When** 任意发布相关 API  
**Then** 行为为 **硬阻断**（E5-001）；**不**出现「已发布但 scheduled/未激活」的新生命周期状态

### BDD-IBL-E5-017 — CE-K08 LM-011 superseded

**Given** 与历史 BDD-CE-K08-LM-011 相同数据（未来 `effectiveFrom`）  
**When** 评估 publish-gate（本叶实现后）  
**Then** 因 `CONTENT_MODULE_EFFECTIVE_NOT_STARTED` **FAIL**（取代「不阻断」期望）  
**And** CE-K08 文档修订指针指向本规格 / ADR-0066

---

## 10. Boundary / exception

| 场景 | 期望 |
| --- | --- |
| 同时未生效 + 已过期（脏数据） | 两项硬门禁均可 FAIL |
| 空 `templateIds` 且无候选 | 200 + 空结果 / 零计数（实现固定一种）；非 500 |
| 并发双 apply | 最终 pin 为目标或校验失败；不损坏引用完整性 |
| 跨组 templateId 混入 | 不可见项 skip 或整单 403/404（实现固定；**禁止**跨组静默改写） |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| Publish-gate JSON | 新 check code FAIL/PASS + offending 摘要 |
| Publish HTTP | 硬项 FAIL 时拒绝发布 |
| Bulk API 响应 | per-item status + 汇总；dry-run 前后 pin 一致 |
| Management audit | `CONTENT_MODULE_BULK_REPIN`（或等价）含 dryRun / pins |
| `mvn verify` | 门禁 + bulk 服务/Web 测试覆盖 BDD-IBL-E5-001…017 |
| Deploy evidence | queued docker stage 5/10（API 验收表面） |

---

## 12. Traceability

| 来源 | 关系 |
| --- | --- |
| Task Master **#132** / IBL-E5 | 本叶 |
| F27（剩余半幅） | 关闭目标 |
| CE-K08 | 字段保留；K08-C6 / LM-011 **修正** |
| CE-U07 | 单模板 bump；本叶跨模板 bulk |
| ADR-0066 | Decision = E5-C* |
| E4 / ADR-0065 | 正交（品牌半幅已闭） |
| PD-6 / PD-7 / #3b / #5a | **OUT** / 不翻转 |

---

## 13. TDD Red 提示（给 backend-engineer）

1. PublishGate：未来 `effectiveFrom` → 新 check FAIL；null/equal/past → PASS；与 EXPIRED 正交。  
2. Bulk：dry-run 无变更 + 审计；apply 改草稿；锁定 skip；非法目标 per-item fail；缺 dryRun 4xx；无权限 403/404。  
3. 回归：CE-K08 expiry 场景仍绿；CE-U07 单模板 bump 仍绿。

---

## 14. Handoff

```
bdd_readiness: ready
frontend_ui_in_scope: false
owner: backend-engineer
next: backend-engineer (TDD Red → implement publish gate + bulk-repin; OpenAPI already synced)
scenarios: BDD-IBL-E5-001…017
e5_c_locked: E5-C1…C23
adr: docs/adr/template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md (Accepted)
```

