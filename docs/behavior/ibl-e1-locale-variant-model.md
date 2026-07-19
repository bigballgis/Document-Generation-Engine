# BDD 行为规格：IBL-E1 — Locale-variant template/clause model（F24 / PD-4）

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-IBL-E1` |
| **编写日期** | 2026-07-19 |
| **程序 / 队列** | IBL Wave E · **IBL-E1** / F24（`ibl-e1-locale-variant-model`） |
| **Slice** | `ibl-e1-locale-variant-model` |
| **Branch** | `feat/ibl-e1-locale-variant-model` |
| **Worktree** | `D:/working/DGE-ibl-e1-locale-variant-model` |
| **Placement** | ISOLATED |
| **Task Master** | **#128** IBL-E1 — Batch Recommendation **solo**；`member_task_ids: ["128"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-e1-locale-variant-model`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；Accepted ADR [0062-locale-variant-template-clause-model.md](../adr/template-lifecycle/0062-locale-variant-template-clause-model.md)；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F24 / IBL-E1 / **PD-4**；domain [domain-model.md](../domain/domain-model.md) §2.7 / §2.9.2；API [contract-outline.md](../api/contract-outline.md)；invocation locale [ibl-a6-regenerate-locale-replay.md](./ibl-a6-regenerate-locale-replay.md) / ADR-0056 |
| **Frontend UI** | **`frontend_ui_in_scope=true`**（管理端 locale 声明、目录筛选、变体家族导航；E2E/UIUX **required**） |

**完成声明约束：** 本叶关闭 F24「单正文 / 无 locale 变体」——模板包与内容模块具备可治理的 locale-variant 模型（数据模型 + API + UI，按 ADR）。**SPECIMEN 水印不得在本叶移除**（PD-6 意图 ≠ E1 实现）。**禁止**激活 IBL-E2…E7 / #119；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 go-live / Wave E / IBL 程序 Done。

```
bdd_readiness: ready
frontend_ui_in_scope: true
open_questions: []
owning_doc: docs/behavior/ibl-e1-locale-variant-model.md
task_ids: ["128"]
adr_status: Accepted (ADR-0062, 2026-07-19) — PD-4 + BDD-IBL-E1-001…018 / E1-C* lock; no remaining product fork for this leaf; impl still In Progress
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["128"]
  proposed_slice_id: ibl-e1-locale-variant-model
  shared_acceptance_surface: >
    Locale-variant template/clause model (ADR + data model + API + UI);
    no undocumented single-body assumption
  vetoes_applied:
    - IBL-E2-jurisdiction-engine
    - IBL-E3-multistage-approval
    - IBL-E4-entity-brands
    - IBL-E5-effectiveFrom-bulk
    - IBL-E6-nesting-governance
    - IBL-E7-RTL-spike
    - PD-6-specimen-removal
    - PD-7-licensed-fonts
    - IBL-B7-Word-host
    - umbrella-106-registry-only
    - checklist-3b-5a-go
  evidence_amortization: mvn verify + FE gates + Playwright + queued docker
```

| IN（本叶） | OUT（明确禁止 / 后续叶） |
| --- | --- |
| ADR-0062 **Accepted**；locale 声明于 Template / Content Module（impl In Progress） | **PD-6** 去 SPECIMEN / true re-issue |
| 变体家族 `localeVariantFamilyId`；同组同家族 locale 唯一 | **PD-7** 授权字体嵌入 |
| 管理 API create/update/list/detail 暴露 locale（及家族）；目录筛选 | **IBL-E2** 辖区/产品/渠道组合引擎 |
| 发布门禁：模板引用的内容模块与模板 locale 语言兼容 | **IBL-E3** 多级法务审批 / legal-reviewer |
| Runtime：路径仍钉扎具体模板版本；`context.locale` 仍驱动 compute；双方均有 locale 时语言兼容 fail-closed | **IBL-E4** 法人品牌变体 |
| 管理 UI：创建/编辑 locale；目录 locale 筛选；家族内兄弟变体导航 | **IBL-E5** `effectiveFrom` 硬阻断 / bulk re-pin |
| 存量迁移回填 `zh-CN`（对齐 compute 默认） | **IBL-E6** 条款嵌套治理；**IBL-E7** RTL |
| Gates：`mvn verify` + FE lint/type-check/test/build + E2E/UIUX + queued deploy | **#119** Word；翻转 **#3b/#5a**；go-live |

---

## 1. 概述

### 1.1 问题（F24）

| 发现 | 证据 |
| --- | --- |
| 无 locale/language 模板或条款变体——一包一文 | `TemplateEntity` / `CreateTemplateRequest` 无 locale；CM 同理 |
| Runtime `context.locale` 仅影响 compute 格式化（金额/日期/拼写语言），**不**选择正文语种资产 | ADR-0056；IBL-A2/A3/A6 |
| 国际银行信函需中英等**不同正文**各自版本化、审批、发布 | PD-4 Confirmed 2026-07-19；程序 IBL-E1 验收草案 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **E1-S1 Declare** | 模板包 / 内容模块声明 BCP-47 `locale`（正文语种） |
| **E1-S2 Family** | 可选 `localeVariantFamilyId` 将翻译兄弟编组；同组同家族内 locale 唯一 |
| **E1-S3 Govern** | 每个 locale 变体仍是独立包/模块，沿用既有版本线 / 审批 / 发布（不共享隐式正文） |
| **E1-S4 Publish gate** | 模板发布时，引用 CM 与模板 locale 须语言兼容，否则硬阻断 |
| **E1-S5 Runtime pin + check** | 调用方仍按路径钉扎模板；有请求 locale 时与模板声明语言兼容，否则 fail-closed |
| **E1-S6 Manage UI** | 作者可见/可设 locale；目录可筛；家族兄弟可导航 |
| **E1-S7 Migrate** | 存量无 locale → 回填 `zh-CN`；文档化「单正文」退出 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **模板编排人员 / 母版设计人员** | 组范围内创建/编辑模板与内容模块 | 声明 locale；创建/加入变体家族 |
| **分组管理员 / 全局管理员** | 组或全局治理 | 同左；可修基础信息含 locale（规则见权限矩阵既有更新边界） |
| **审批 / 测试人员** | 既有生命周期角色 | 按**单一 locale 变体**测试与审批（不跨语种合并审批） |
| **Runtime API 调用方** | 有效凭证；generate 路径钉扎模板+版本；可选 `context.locale` | 正文来自钉扎模板；locale 用于 compute + 兼容校验 |
| **系统** | 发布门禁 / 兼容校验 / 迁移回填 | Fail-closed；审计既有模板/CM 事件可附带 locale 摘要（非强制新事件类型） |

---

## 3. Goal

1. 消除「一包一文且无语种声明」的未文档化假设：每个模板包与内容模块**显式声明**正文 `locale`。  
2. 支持同一业务信函/条款的多语种变体作为**可独立治理**的兄弟资产（家族编组），而非运行时自动翻译。  
3. 发布与运行时在「声明 locale 与请求/引用 locale 冲突」时 **fail-closed**。  
4. 管理 UI 可完成声明、筛选与家族导航（`frontend_ui_in_scope=true`）。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave E Done。

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（SoT + PD-4 范围确认 + 保守银行级默认 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **E1-C1** | **Locale 语义：** `locale` = 资产**正文语种**的 BCP-47 语言标签字符串（与 runtime `context.locale` 同一标签空间）。**不是** UI i18n；**不是** CE-K08 `jurisdiction`。 | PRD/API `context.locale`；F24；PD-4 |
| **E1-C2** | **挂载点：** `locale` 挂在 **Template 包行**与 **Content Module 包行**（非单次 version 行）。版本线/审批/发布仍是对该语种正文的演进。 | 包导航 hub 模型；CM 实体+版本两轴已有 |
| **E1-C3** | **必填（写入后）：** 创建模板/内容模块时 `locale` **必填**（非空白）。非法/不可解析标签 → `422` VALIDATION（不得静默改写）。 | Fail-closed；对齐 A2/A3 非法币种风格 |
| **E1-C4** | **存量迁移：** Flyway（或等价）将既有行回填 `locale = zh-CN`（`ComputeDslLimits.DEFAULT_LOCALE`）。迁移后字段非空。 | 既有 compute 默认；诚实标注迁移非「业务曾声明中文」 |
| **E1-C5** | **变体家族：** 可选 `localeVariantFamilyId`（UUID）。空 = 独立资产（无兄弟）。非空时：同一 `groupCode` 内 `(localeVariantFamilyId, locale)` **唯一**；冲突 → `409`（稳定错误码，实现命名，如 `LOCALE_VARIANT_CONFLICT`）。 | 多语种兄弟；组隔离 |
| **E1-C6** | **身份不变：** `externalId` / `moduleCode` 仍全局（组内）唯一；调用方继续路径钉扎**具体**模板。家族**不**替代路径选择，也**不**引入「按 locale 自动选包」runtime 路由（避免静默选错正文）。 | PRD 路径钉扎；银行可审计 |
| **E1-C7** | **生命周期正交：** 每个 locale 变体独立走 ADR-0021 / CM 审批生命周期。家族成员发布状态互不自动传播。 | 既有治理；反自动翻译 |
| **E1-C8** | **语言兼容：** 比较 **primary language subtag**（大小写不敏感）；`en`≡`en-US`≡`en-GB`；`zh`≡`zh-CN`；`en`↛`zh`。脚本/region 差异不单独阻断。 | 保守默认；与 A3 locale-language 选取同构 |
| **E1-C9** | **发布门禁：** 模板发布时，每个锁定引用的内容模块 `locale` 须与模板 `locale` 语言兼容（E1-C8）；否则硬阻断（新 `PublishGateCheckCode`，如 `CONTENT_MODULE_LOCALE_MISMATCH`）。与 `CONTENT_MODULE_EFFECTIVE_EXPIRED` 正交。 | 银行级正文一致 |
| **E1-C10** | **Runtime 兼容：** 请求提供非空白 `context.locale` 且模板声明 locale 时，须语言兼容；否则 **422**（稳定码，如 `TEMPLATE_LOCALE_MISMATCH`），**不**静默换模板。请求 locale 缺失/空白 → **不**做兼容校验（compute 仍按 A3/A6 默认 `zh-CN`）。 | 路径钉扎 + 可选校验；兼容既有省略 locale 调用 |
| **E1-C11** | **无自动翻译 / 无隐式正文合并。** | PD-4 模型边界 |
| **E1-C12** | **管理 API：** create/update（允许的基础信息更新）与 summary/detail 视图包含 `locale` 与 `localeVariantFamilyId`；`GET /templates` 与 `GET /content-modules` 支持可选 `locale` 精确筛选（与既有 filters **AND**）。 | 程序验收 API+UI |
| **E1-C13** | **管理 UI：** 创建向导/表单必填 locale；目录展示 locale 列/徽章并可筛；模板 hub / 内容模块详情展示同家族其他 locale 变体入口（只读导航到兄弟资产）。Bank OA 风格 + English-first i18n。 | E1 owners 含 FE；`frontend_ui_in_scope=true` |
| **E1-C14** | **权限：** 不新增角色；创建/更新 locale 与创建模板/CM、更新基础信息同一权限边界；fail-closed 403/404 惯例不变。 | permission-matrix 既有 |
| **E1-C15** | **导入/导出：** 本叶最小要求——导出 metadata 携带 `locale`（及家族 id 若有）；导入保留并校验 E1-C3/C5。完整跨环境家族重绑若超范围可记 residual，但不得丢 locale。 | CE-E01 方向；防单正文回流 |
| **E1-C16** | **审计：** 不强制新 audit event 类型；既有 create/update 审计载荷宜含 `locale` 标签字符串（无正文/无 variables）。 | 最小可观测 |
| **E1-C17** | **SPECIMEN / PD-6：** 本叶**不**改变再生/水印政策。 | 程序 §8 |
| **E1-C18** | **ADR：** ADR-0062 **Accepted**（2026-07-19；Decision = 本节 E1-C*）。PD-4 确认范围；BDD-IBL-E1-001…018 锁定默认；无剩余产品二选一。Accepted ≠ E1 impl Done。 | 程序 IBL-E1 验收 |
| **E1-C19** | **门禁：** BE `mvn verify`；FE lint/type-check/test/build；用户面 E2E + UIUX；queued Docker deploy evidence。 | delivery constitution |
| **E1-C20** | **完成边界：** E1 Done ≠ Wave E Done；≠ go-live；#3b/#5a 保持 CONDITIONAL。 | 队列政策 |

### 4.2 明确非本叶确认（禁止当作已定产品事实）

| 项 | 状态 |
| --- | --- |
| 辖区/产品/渠道组合引擎 | **IBL-E2** / PD-5（已确认范围，**未**激活本叶） |
| 多级法务审批矩阵 | **IBL-E3** |
| 法人文档品牌变体 | **IBL-E4** |
| 去 SPECIMEN / 授权字体 / Word / RTL | PD-6/7、#119、E7 — **OUT** |
| Runtime「只传 locale、不传模板 id」的自动选包 | **拒绝**（E1-C6） |

### 4.3 ADR 用户确认

| 问题 | 结论 |
| --- | --- |
| 是否还需用户再确认「要不要做 locale-variant」？ | **否** — PD-4 已确认 |
| 是否还需用户再确认 E1-C1…C20 默认？ | **否（BDD ready）** — 已由既有 locale/包版本化模式隐含；记入本 BDD + **Accepted** ADR-0062 |
| ADR 文件状态何时 Accepted？ | **已 Accepted**（2026-07-19；doc-keeper stage-3 follow-up；PD-4 + BDD-IBL-E1-001…018） |

---

## 5. Preconditions

- 操作者具备对应组范围的模板/内容模块创建或基础信息更新权限。  
- PD-4 已 Confirmed；#128 为 sole-active。  
- 既有模板包 hub / CM 生命周期可用。  
- Runtime 调用方持有可调用已发布版本的凭证与路径。

---

## 6. Trigger

- 作者创建或更新模板/内容模块并提交 `locale`（及可选家族 id）。  
- 作者在目录按 locale 筛选或从 hub 打开兄弟变体。  
- 发布候选进入发布门禁（含 CM 引用）。  
- Runtime generate（含 batch/async 等价路径）携带可选 `context.locale`。

---

## 7. Primary journey

1. 作者创建英文模板：`locale=en-US`，获得新 `localeVariantFamilyId`（或加入已有家族）。  
2. 作者创建同家族中文模板：`locale=zh-CN`，相同 `localeVariantFamilyId`。  
3. 各模板独立编排正文、引用**同语种兼容**的内容模块、测试、审批、发布。  
4. 管理目录按 `locale=en-US` 筛选可见英文包；hub 展示兄弟 `zh-CN` 入口。  
5. 上游系统调用英文已发布版本路径，并传 `context.locale=en-US` → 生成成功；金额/日期按 en-US compute。  
6. 若误传 `context.locale=zh-CN` 到英文模板 → **422** `TEMPLATE_LOCALE_MISMATCH`（不换包）。

---

## 8. System responses（成功路径）

- Create/update 持久化 `locale` / `localeVariantFamilyId`；响应视图回显。  
- 目录筛选与家族导航返回授权范围内兄弟摘要（id、externalId/moduleCode、name、locale、lifecycle 摘要）。  
- 发布门禁通过时无 locale mismatch。  
- Runtime 兼容时生成行为与既有钉扎版本一致（本叶不改渲染引擎正文选择逻辑，除兼容校验）。

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-E1-001 — Create template requires locale

**Given** 授权模板编排人员在组 `G`  
**When** `POST` 创建模板且 body **省略**或空白 `locale`  
**Then** `422` VALIDATION；不落库

### BDD-IBL-E1-002 — Create template persists locale and family

**Given** 授权用户  
**When** 创建模板 `locale=en-US` 且提供新 `localeVariantFamilyId`（或服务端在「创建变体」流中生成——实现可选；若客户端省略家族 id 则保持 null）  
**Then** `201/200`；detail/summary 含 `locale=en-US` 与家族字段（若提供）

### BDD-IBL-E1-003 — Create content module requires locale

**Given** 授权用户  
**When** 创建内容模块省略/空白 `locale`  
**Then** `422` VALIDATION；不落库

### BDD-IBL-E1-004 — Family uniqueness within group

**Given** 组 `G` 已存在家族 `F` 的 `en-US` 模板  
**When** 再创建同组同家族 `locale=en-US` 模板  
**Then** `409` `LOCALE_VARIANT_CONFLICT`（或等价稳定码）；不覆盖已有包

### BDD-IBL-E1-005 — Independent lifecycle per locale variant

**Given** 同家族 `en-US` 已 `PUBLISHED`，`zh-CN` 仍为 `DRAFT`  
**When** 查询两包生命周期  
**Then** 状态彼此独立；发布英文**不**自动发布中文

### BDD-IBL-E1-006 — Catalog filter by locale

**Given** 组内存在 `en-US` 与 `zh-CN` 模板（及内容模块）  
**When** `GET …/templates?locale=en-US`（及 `GET …/content-modules?locale=en-US`）  
**Then** 仅返回该 locale（与 search/groupCode/status 等 **AND**）；非法 locale 查询值 → 空页或 `422`（实现二选一须在 OpenAPI 固定；**推荐空页**对齐 CE-U20 非法 status）

### BDD-IBL-E1-007 — Publish gate CM locale mismatch

**Given** 模板 `locale=en-US` 引用内容模块 `locale=zh-CN`  
**When** 尝试发布该模板  
**Then** 发布门禁失败，含 `CONTENT_MODULE_LOCALE_MISMATCH`（硬阻断）；不得发布

### BDD-IBL-E1-008 — Publish gate CM locale compatible

**Given** 模板 `locale=en-US` 引用模块 `locale=en-GB`（同 primary `en`）  
**When** 其他门禁已通过时发布  
**Then** locale 门禁通过（不因 region 差异阻断）

### BDD-IBL-E1-009 — Runtime locale mismatch fail-closed

**Given** 已发布模板 `locale=en-US`  
**When** generate 路径钉扎该版本且 `context.locale=zh-CN`  
**Then** `422` `TEMPLATE_LOCALE_MISMATCH`；不生成；不改钉扎目标

### BDD-IBL-E1-010 — Runtime matching locale succeeds

**Given** 已发布模板 `locale=en-US`，变量含 locale 敏感 compute  
**When** generate 且 `context.locale=en-US`  
**Then** `200` 成功路径；compute 按 en-US（回归对齐 IBL-A2/A3）

### BDD-IBL-E1-011 — Runtime omitted locale skips compatibility check

**Given** 已发布模板 `locale=en-US`  
**When** generate **不传** `context.locale`（或空白）  
**Then** **不**因 E1-C10 拒绝；compute 默认 `zh-CN`（既有引擎行为）；本叶不强制调用方必传 locale

### BDD-IBL-E1-012 — Migration backfill zh-CN

**Given** 迁移前存量模板/CM 无 locale  
**When** 应用 E1 迁移  
**Then** 所有存量行 `locale=zh-CN`；应用可启动；目录可筛选 `zh-CN`

### BDD-IBL-E1-013 — Management UI create requires locale

**Given** 授权用户打开创建模板（或创建内容模块）表单  
**When** 未选/未填 locale 尝试提交  
**Then** 客户端阻断或展示校验错误；不得发出省略 locale 的成功创建

### BDD-IBL-E1-014 — Management UI catalog locale filter

**Given** 目录存在多 locale 资产  
**When** 用户选择 locale 筛选 `en-US`  
**Then** 列表仅显示匹配项（与服务器 `locale` query 一致）

### BDD-IBL-E1-015 — Management UI family sibling navigation

**Given** 模板 hub（或 CM 详情）属于非空家族且存在兄弟 locale  
**When** 用户查看变体家族区域  
**Then** 可见兄弟摘要（含 locale）并可导航至兄弟资产；无权限的兄弟不出现

### BDD-IBL-E1-016 — Unauthorized create/update fail-closed

**Given** 用户无目标组模板/CM 写权限  
**When** 尝试创建或更新 locale  
**Then** `403` 或 `404`（既有惯例）；无写入

### BDD-IBL-E1-017 — No silent single-body assumption in docs

**Given** 本叶文档落地后  
**When** 阅读 domain §2.7 / §2.9.2 与 ADR-0062  
**Then** 明确「每包一文种声明 + 可选家族」；不再描述「平台仅支持单一正文且无语种字段」为现行模型

### BDD-IBL-E1-018 — SPECIMEN / PD-6 unchanged

**Given** CE-G06 regenerate 路径  
**When** E1 变更合并后执行 regenerate  
**Then** 成功样件仍含 SPECIMEN；本叶无去水印代码路径

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 非法 locale 标签（create/update） | `422` VALIDATION |
| 家族内重复 locale | `409` conflict |
| CM/模板语言不兼容（发布） | 硬门禁失败 |
| Runtime 语言不兼容 | `422` mismatch；不换模板 |
| 省略 runtime locale | 跳过兼容校验 |
| 跨组家族 id 碰撞 | 唯一性作用域为 **groupCode**；不得跨组共享写 |
| 自动翻译请求 | 不提供；OUT |
| 权限不足 | fail-closed |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | create/detail/list 字段；409/422 码；publish gate code；runtime 422 |
| DB | `locale` / `locale_variant_family_id` 列 + 迁移 |
| UI | 表单、筛选、家族导航（Playwright） |
| Docs | ADR-0062 **Accepted**（2026-07-19）；domain/API 指针；本 BDD |
| Gates | `mvn verify`；FE 四门；E2E+UIUX；deploy queue |
| 负向 | 无 SPECIMEN 移除；无 #3b/#5a 翻转 |

---

## 12. Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#128** | IBL-E1 sole-active |
| IBL program **F24** / **PD-4** / §7 IBL-E1 acceptance | 范围 + Done 草案 |
| ADR-0062 | 架构 Decision（**Accepted** 2026-07-19） |
| domain-model §2.7 / §2.9.2 | 领域确认指针 |
| contract-outline / OpenAPI | API 契约 |
| IBL-A2/A3/A6 · ADR-0056 | invocation locale / compute 正交 |

---

## 13. Implementation notes（非产品发明；供 TDD）

- 错误码与 `PublishGateCheckCode` 枚举名以实现为准，但须稳定、可测、写入 OpenAPI/`messageKey`。  
- `locale` 存储建议 `varchar` 长度与 `context.locale` / CE-K08 短文本字段同级（如 ≤32 或 ≤64）；OpenAPI 同步。  
- 「创建变体」可复用 clone-from-release 后改 locale + 设同一 family——若做，须测 E1-C5 唯一性；非强制独立 endpoint。  
- FE 遵循 `.cursor/skills/frontend-oa-design` 与 `i18n-english-first`。

---

## 14. Handoff

```text
bdd_readiness: ready
frontend_ui_in_scope: true
scenario_ids:
  - BDD-IBL-E1-001
  - BDD-IBL-E1-002
  - BDD-IBL-E1-003
  - BDD-IBL-E1-004
  - BDD-IBL-E1-005
  - BDD-IBL-E1-006
  - BDD-IBL-E1-007
  - BDD-IBL-E1-008
  - BDD-IBL-E1-009
  - BDD-IBL-E1-010
  - BDD-IBL-E1-011
  - BDD-IBL-E1-012
  - BDD-IBL-E1-013
  - BDD-IBL-E1-014
  - BDD-IBL-E1-015
  - BDD-IBL-E1-016
  - BDD-IBL-E1-017
  - BDD-IBL-E1-018
open_questions: []
recommended_next_stage: backend-engineer + frontend-engineer (ADR-0062 Accepted; OpenAPI/contract pointers aligned)
```
