# BDD 行为规格：IBL-E4 — Per-legal-entity document brand variants（F27 / PD-9）

> **SYS-NORM D1 supersession (2026-07-21):** Ongoing **DocumentBrand / LegalEntity product
> surfaces** are **superseded / withdrawn** by
> [ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)
> (**Accepted**). **Wave 6 runtime / API / catalog retirement SoT (BDD ready):**
> [sys-norm-d1-brands.md](./sys-norm-d1-brands.md) **`bdd_readiness: ready`** —
> **BDD-SYS-NORM-D1-001…020** (TM **#150** Done `64b0a650`; Wave 6 delivered).
> This file remains **historical** IBL-E4 delivery acceptance evidence; do **not** treat
> E4-C* dual-catalog management UX as an ongoing product requirement. New implementers
> follow Wave 6 + ADR-0071, not E4-C* catalog permissions.

| Field | Value |
| --- | --- |
| **文件状态** | `ready`（historical IBL-E4; product surface superseded by ADR-0071 / Wave 6） |
| **BDD ID 前缀** | `BDD-IBL-E4` |
| **编写日期** | 2026-07-20 |
| **程序 / 队列** | IBL Wave E · **IBL-E4** / F27（`ibl-e4-entity-document-brands`） |
| **Slice** | `ibl-e4-entity-document-brands` |
| **Branch** | `feat/ibl-e4-entity-document-brands` |
| **Worktree** | `D:/working/DGE-ibl-e4-entity-document-brands` |
| **Placement** | ISOLATED |
| **Base** | `3bd2cd87`（#130 IBL-E3 Done on main；sole-active cleared at E3 closeout） |
| **Task Master** | **#131** IBL-E4 — Batch Recommendation **solo**；`member_task_ids: ["131"]` |
| **Umbrella** | **#106** registry only（非本叶） |
| **Formal phase** | **None**（不发明 sole-active 正式 P-phase） |
| **Batch recommendation** | **solo**（`proposed_slice_id: ibl-e4-entity-document-brands`） |
| **Owning docs** | **本文件（本叶行为 SoT）**；[ADR-0065](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md) **Accepted**（doc-keeper Stage 3；Decision = E4-C*；Accepted ≠ impl Done）；对照 [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) F27 / IBL-E4 / **PD-9**；基线 [ADR-0013](../adr/api/0013-api-contract-visibility-audit-and-context.md) context 白名单（Amendment `legalEntityCode`）；[domain-model.md](../domain/domain-model.md)；[permission-matrix.md](../security/permission-matrix.md)；UI 主题正交 [PRD.md](../product/PRD.md) REDBC/GREENBC chrome；既有签章几何 [ibl-b5-seal-geometry.md](./ibl-b5-seal-geometry.md) |
| **Frontend UI** | **`frontend_ui_in_scope=true`**（owners = backend-engineer **+ frontend-engineer**；法人实体目录、文档品牌目录、实体→品牌选择、模板可选 allow-list、生成/预览解析回显为用户面；E2E/UIUX **required**） |

**完成声明约束：** 本叶关闭 F27「无 per-legal-entity **文档**品牌变体；REDBC/GREENBC = **仅**管理 UI 主题」中的 **文档品牌缺口**——法人实体可选择并绑定文档品牌变体，且该品牌在 generate / preview / test-generation 路径上驱动**文档产物**品牌资产（信头/logo/可选默认签章槽位等），**不**改写壳层 `REDBC`/`GREENBC` UI chrome。**SPECIMEN 水印不得在本叶移除**（PD-6 意图 ≠ E4 实现）。**禁止**实施 PD-7 授权字体嵌入；**禁止**激活 IBL-E5…E7 / #119；**禁止**翻转 checklist **#3b** / **#5a**；**禁止**宣称 go-live / Wave E / IBL 程序 Done。F27 中的 `effectiveFrom` 硬阻断 / bulk re-pin **OUT → IBL-E5**。

```
bdd_readiness: ready
frontend_ui_in_scope: true
open_questions: []
owning_doc: docs/behavior/ibl-e4-entity-document-brands.md
task_ids: ["131"]
suggested_adr: 0065 — docs/adr/template-lifecycle/0065-legal-entity-document-brand-variants.md (Accepted 2026-07-20; Decision = E4-C*; Accepted ≠ impl Done)
scenario_count: 17
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  member_task_ids: ["131"]
  proposed_slice_id: ibl-e4-entity-document-brands
  shared_acceptance_surface: >
    Document brand variants selectable per legal entity;
    runtime resolve + apply to document artifacts (not UI chrome);
    management UI for brand catalog + entity↔brand binding
  vetoes_applied:
    - IBL-E5-effectiveFrom-bulk
    - IBL-E6-nesting-governance
    - IBL-E7-RTL-spike
    - PD-6-specimen-removal
    - PD-7-licensed-fonts
    - IBL-B7-Word-host
    - umbrella-106-registry-only
    - checklist-3b-5a-go
    - rewrite-ui-redbc-greenbc-as-document-brands
    - invent-company-ldap-or-go-live-claims
  evidence_amortization: >
    mvn verify + FE lint/type-check/test/build + Playwright functional+UIUX
    + queued docker deploy (stages 5/10 as pipeline requires)
```

| IN（本叶） | OUT（明确禁止 / 后续叶） |
| --- | --- |
| ADR（建议 **0065**）Accepted；Decision = E4-C* | **PD-6** 去 SPECIMEN / true re-issue |
| 组范围 **DocumentBrand** 目录（文档品牌，≠ UI BrandPreset） | **PD-7** 授权字体；**#119** Word |
| 组范围 **LegalEntity** 目录；每实体**必选**一个 `documentBrandCode` | **IBL-E5** `effectiveFrom` 硬阻断 / bulk re-pin |
| Runtime `context.legalEntityCode`（可选白名单扩展）→ 解析文档品牌并应用于产物 | **IBL-E6** 嵌套；**IBL-E7** RTL |
| 模板可选 `allowedDocumentBrandCodes`；解析品牌不在名单 → fail-closed | 把 REDBC/GREENBC 壳层主题改造成文档品牌源 |
| 管理 UI：品牌目录 + 法人实体品牌选择 + 组默认品牌 + 解析回显 | 翻转 **#3b/#5a**；go-live；宣称 Wave E Done |
| 审计：记录非敏感 `legalEntityCode` + 解析后 `documentBrandCode` | 发明公司 LDAP 主机 / SLO / 外发渠道（PD-1） |
| Gates：BE verify + FE 四门 + E2E/UIUX + queued deploy | 像素回归（PD-2）；母版 Word 测量（PD-3） |

---

## 1. 概述

### 1.1 问题（F27 — 本叶覆盖半幅）

| 发现 | 证据 |
| --- | --- |
| `REDBC` / `GREENBC` 仅为管理 UI 主题（logo/色/壳层），**不**驱动信函产物品牌 | PRD 品牌 logo 槽位；`frontend/src/config/brands.ts`；F27 |
| 无 per-legal-entity **文档**品牌变体；跨国信函无法按签发法人切换信头/logo/默认签章资产 | 程序 F27；PD-9 Confirmed 2026-07-19 |
| F27 另含 `effectiveFrom` / bulk re-pin — **本叶不关闭** | IBL-E5 |

### 1.2 行为域

| 域 | 摘要 |
| --- | --- |
| **E4-S1 DocumentBrand catalog** | 组内可治理文档品牌：代码、显示名、状态、资产包（logo 必选；可选默认 seal；可选信头法定名称） |
| **E4-S2 LegalEntity catalog** | 组内法人实体：代码、显示名、状态、**必选**绑定一个 ACTIVE `documentBrandCode`（可改绑） |
| **E4-S3 Resolve** | Runtime / preview / test-generation：`legalEntityCode` → LegalEntity → DocumentBrand；省略时走组默认或平台种子默认 |
| **E4-S4 Apply** | 将解析品牌资产应用到文档产物品牌槽位（信头/logo/可选默认签章）；**不**改 `data-brand` UI chrome |
| **E4-S5 Template allow-list** | 可选包级允许品牌集合；解析结果不在集合 → 422 |
| **E4-S6 Manage UI** | 目录 CRUD、实体→品牌选择器、组默认、详情/预览解析回显（Bank OA + English-first） |
| **E4-S7 Audit + migrate** | 变更与调用摘要可审计；存量种子 `PLATFORM_DEFAULT`；UI REDBC/GREENBC 行为回归不变 |

---

## 2. Actor / Role

| Actor | 角色 / 能力 | 说明 |
| --- | --- | --- |
| **分组/全局管理员** | `GROUP_ADMIN` / `GLOBAL_ADMIN` | 维护 DocumentBrand / LegalEntity 目录；设置组默认文档品牌；改绑实体品牌 |
| **模板编排人员 / 母版设计人员** | `TEMPLATE_AUTHOR` / `MASTER_DESIGNER` | 可配置模板 `allowedDocumentBrandCodes`（若具备模板写权限）；只读查看解析品牌 |
| **API 调用方** | 已发布模板凭证 | 可选提交 `context.legalEntityCode`；消费解析后的文档品牌产物 |
| **测试人员 / 审批人** | `TEMPLATE_TESTER` / `TEMPLATE_APPROVER` / `LEGAL_REVIEWER` | 预览/测试路径与 runtime **同一解析器**；不获目录写权限（除非兼管理员） |
| **系统** | 品牌解析 / 渲染应用 / 审计 / UI 门禁 | Fail-closed 未知实体、非 ACTIVE、allow-list 不匹配；禁止用 UI chrome 冒充文档品牌 |

---

## 3. Goal

1. 关闭 F27 文档品牌缺口：法人实体可选择文档品牌变体，并在文档产物上生效。  
2. 严格区分 **DocumentBrand**（产物）与 **UI BrandPreset**（`REDBC`/`GREENBC` 壳层）——二者正交、互不覆盖。  
3. Runtime / preview / test-generation **同一解析与应用语义**；可审计。  
4. 管理 UI 可完成品牌目录、实体绑定与组默认配置。  
5. Formal phase **None**；不宣称 go-live；不翻转 #3b/#5a；不宣称 Wave E Done；不激活 E5–E7 / #119；不实施 PD-6/PD-7。  

---

## 4. 已确认决策 vs 非确认

### 4.1 本叶确认决策（SoT + PD-9 范围确认 + 保守银行级默认 — 无需再问产品二选一）

| ID | 决策 | 依据 |
| --- | --- | --- |
| **E4-C1** | **正交分离：** DocumentBrand ≠ UI BrandPreset。壳层 `REDBC`/`GREENBC` 主题切换**不得**改变文档产物品牌资产；文档品牌解析**不得**改写 `html[data-brand]` / 壳层 logo。 | PD-9「vs UI theming only」；F27 |
| **E4-C2** | **双目录：** 引入组范围 **DocumentBrand** 与 **LegalEntity** 两个可治理目录（逻辑删除惯例对齐平台）。不把品牌代码硬编码进模板正文。 | 「selectable per legal entity」 |
| **E4-C3** | **DocumentBrand 最小契约：** `documentBrandCode`（组内唯一、trim、大小写敏感稳定码，建议 max **64**）、显示名（English-first i18n 键或本地化字段，实现固定一种并写入 OpenAPI）、`status` ∈ {`ACTIVE`,`INACTIVE`}、**必选** `logoObjectRef`（已授权对象存储引用）、可选 `defaultSealObjectRef`、可选 `letterheadLegalName`（非敏感短文本，max **256**）。**不**在本叶定义完整视觉设计系统或授权字体包。 | 可测最小面；PD-7 OUT |
| **E4-C4** | **LegalEntity 最小契约：** `legalEntityCode`（组内唯一、trim、稳定码，建议 max **64**）、显示名、`status` ∈ {`ACTIVE`,`INACTIVE`}、**必填** `documentBrandCode`（必须引用同组存在的 DocumentBrand）。一实体同一时刻恰好绑定**一个**品牌；改绑 = 更新绑定（审计）。 | PD-9 核心验收 |
| **E4-C5** | **种子默认品牌：** 每组迁移/初始化具备 `documentBrandCode=PLATFORM_DEFAULT`（ACTIVE；平台占位 logo 资产；**不是** REDBC/GREENBC UI 码）。存量调用在未传法人时不静默借用 UI 主题。 | 向后兼容 + 诚实分离 |
| **E4-C6** | **组默认法人解析回落：** 组可配置 `defaultLegalEntityCode`（可选）。Runtime 省略 `context.legalEntityCode` 时：若组默认实体存在且 ACTIVE → 用其绑定品牌；否则 → `PLATFORM_DEFAULT` 文档品牌。**禁止**回落到 UI `REDBC`/`GREENBC`。 | 渐进接入；fail-soft 仅限已种子默认 |
| **E4-C7** | **Context 白名单扩展：** ADR-0013 白名单新增可选字符串 `legalEntityCode`。未知字段仍 `400 REQUEST_BODY_INVALID`。非空白值写入 `contextSummary`（非敏感）。**不**把客户名/账号塞进 context。 | 与 E1/E2 context 扩展同构 |
| **E4-C8** | **解析规则（确定性）：** (1) 请求 `legalEntityCode` 非空白 → 查同组 LegalEntity；(2) 缺失/INACTIVE/未知 → **422** 稳定码（如 `LEGAL_ENTITY_UNKNOWN` / `LEGAL_ENTITY_INACTIVE`，实现固定并写入 OpenAPI）；(3) 绑定品牌缺失/INACTIVE → **422**（如 `DOCUMENT_BRAND_INACTIVE`）；(4) 通过后得到 `ResolvedDocumentBrand`。省略 legalEntity → E4-C6。匹配 = trim 后 **exact**（大小写敏感，与稳定码一致）。 | fail-closed；银行可审计 |
| **E4-C9** | **应用面（产物）：** 解析成功后，在 generate / preview / test-generation **同一路径**将品牌资产应用到文档品牌槽位：logo → 信头/品牌 image 槽；若品牌提供 `defaultSealObjectRef` 且模板/绑定未显式覆盖 seal → 可作为默认 seal 源（显式 sealRef **优先**）；`letterheadLegalName` 注入受控信头文本槽（若母版/模板声明该槽）。**不**要求重写全部历史母版像素布局；无槽位时记录非阻断 fidelity warning（实现固定码），**不得** 500 或静默用 UI chrome 顶替。 | 「document brand variants」；B5 seal 正交 |
| **E4-C10** | **签章几何正交：** 若应用了 seal，仍执行 IBL-B5 声明式授权区校验；区外 → 既有 fail-closed。本叶不宣称 writer 绝对定位 Done。 | ibl-b5 |
| **E4-C11** | **模板 allow-list（可选）：** 模板包级可选 `allowedDocumentBrandCodes: string[]`。空/缺省 = 允许组内任一 ACTIVE 文档品牌（含 `PLATFORM_DEFAULT`）。非空时，解析品牌 ∉ 名单 → **422**（如 `DOCUMENT_BRAND_NOT_ALLOWED`）。可写窗口对齐模板基础元数据草稿规则（已发布线不可改或需新版本——与既有包元数据惯例一致，实现固定一种）。 | 国际信函模板可限制签发品牌 |
| **E4-C12** | **不自动选模板包：** 路径仍钉扎具体模板+版本（同 E1-C6 / E2-C12）。品牌解析只影响产物品牌槽位，不改路径选包。 | PRD 路径钉扎 |
| **E4-C13** | **管理 API：** DocumentBrand 与 LegalEntity 的 list/create/update（及按需 get）；组默认 `defaultLegalEntityCode` 读写；模板 detail 回显 `allowedDocumentBrandCodes`；生成/预览/测试摘要回显 `resolvedLegalEntityCode` + `resolvedDocumentBrandCode`（非敏感）。OpenAPI 同步。越权 403/404 惯例不变。 | API+UI 同契约 |
| **E4-C14** | **权限：** 目录写 = 管理员（`GROUP_ADMIN`/`GLOBAL_ADMIN`，组范围）。模板 allow-list 写 = 既有模板编排写边界。无新角色。permission-matrix 增补行为入口/能力说明（非第 9 角色）。 | 最小角色膨胀 |
| **E4-C15** | **审计：** 品牌/实体创建、改绑、停用、组默认变更写管理审计（含 code 与非敏感 diff）。Runtime 成功路径 invocation/audit 摘要含 `legalEntityCode`（若有）+ `documentBrandCode`（解析后）。禁止 variables / 客户明文 / 资产二进制。 | 可审计验收 |
| **E4-C16** | **导入/导出：** 模板导出可携带 `allowedDocumentBrandCodes`；**不**要求本叶导出完整品牌二进制资产包（品牌目录为组主数据）。导入后 allow-list 引用未知品牌 → 校验失败（422/发布门禁，实现固定一种稳定码）。 | 防悬空 allow-list |
| **E4-C17** | **管理 UI（必交付）：** (1) DocumentBrand 目录（列表/创建编辑/ACTIVE·INACTIVE）；(2) LegalEntity 目录 + **文档品牌选择器**；(3) 组默认法人/回落说明；(4) 模板高级区可选 allow-list；(5) 预览或详情展示 resolved brand codes。Bank OA + English-first i18n。壳层 brand switcher **保持** UI-only。 | `frontend_ui_in_scope=true` |
| **E4-C18** | **UI chrome 回归：** 现有 REDBC/GREENBC 切换、双品牌黄金截图语义**不得**被本叶破坏；E2E 须证明文档品牌配置与 UI 主题可独立变化。 | F27 诚实边界 |
| **E4-C19** | **SPECIMEN / PD-6 / PD-7 / Word：** 不改变再生水印；不嵌入授权字体；不发明 #119 Word 证据。 | 程序 §8 / 队列 veto |
| **E4-C20** | **ADR：** doc-keeper 以本文件 E4-C* 为 Decision 接受 **ADR-0065**（建议路径 `docs/adr/template-lifecycle/0065-legal-entity-document-brand-variants.md`）。ADR-0013 **Amendment**：context 增加可选 `legalEntityCode`。Accepted ≠ E4 impl Done。 | 程序 IBL-E4 验收 |
| **E4-C21** | **门禁：** BE `mvn verify`；FE lint/type-check/test/build；用户面 Playwright functional + UIUX；queued Docker deploy evidence。 | delivery constitution |
| **E4-C22** | **完成边界：** E4 Done ≠ Wave E Done；≠ go-live；#3b/#5a 保持 CONDITIONAL；E5–E7 / #119 不激活；F27 的 effectiveFrom/bulk 半幅仍开向 E5。 | 队列政策 |

### 4.2 明确非本叶确认（禁止当作已定产品事实）

| 项 | 状态 |
| --- | --- |
| `effectiveFrom` 硬阻断 / bulk re-pin / mass-migration | **IBL-E5** |
| 条款嵌套图治理 | **IBL-E6** |
| RTL / SPECIMEN 移除 / 授权字体 / Word | E7 / PD-6/7 / #119 — **OUT** |
| 将 UI `REDBC`/`GREENBC` 直接当作文档品牌码 | **拒绝**（E4-C1） |
| 按法人自动改路径选模板包 | **拒绝**（E4-C12） |
| 公司级全球品牌主数据 / LDAP 同步 | **拒绝本叶发明**（组范围目录即可） |
| 像素级视觉回归门禁 | **PD-2 pending** — OUT |
| Outbound delivery 品牌信纸印刷编排 | **PD-1 pending** — OUT |

### 4.3 ADR / 用户确认

| 问题 | 结论 |
| --- | --- |
| 是否还需用户再确认「要不要做 per-legal-entity 文档品牌」？ | **否** — PD-9 已确认 2026-07-19 |
| 是否还需用户再确认 E4-C1…C22 默认？ | **否（BDD ready）** — 由 PD-9「selectable per legal entity」+ UI/文档品牌分离 + 银行 fail-closed/可审计/路径钉扎惯例隐含；记入本 BDD；doc-keeper Accept ADR-0065 |
| ADR 文件状态何时 Accepted？ | **Accepted（doc-keeper Stage 3，2026-07-20）** — `docs/adr/template-lifecycle/0065-legal-entity-document-brand-variants.md`；Decision = E4-C\*；Accepted ≠ E4 impl Done |

---

## 5. Preconditions

- 操作者具备对应组范围权限（目录写 vs 模板写 vs 生成调用）。  
- PD-9 已 Confirmed；#131 为本交付叶（orchestrator 已激活本切片）。  
- 对象存储可托管已授权 logo/seal 资产引用（不抓取公网页）。  
- E1 locale / E2 inclusion / E3 approval matrix / B5 seal 行为保持可用且不被本叶破坏。  
- 管理 UI 壳层 REDBC/GREENBC switcher 已存在（回归基线）。

---

## 6. Trigger

- 管理员创建/更新 DocumentBrand 或 LegalEntity（含改绑 `documentBrandCode`）。  
- 管理员设置组 `defaultLegalEntityCode`。  
- 作者配置模板 `allowedDocumentBrandCodes`。  
- Runtime generate / preview / test-generation 携带或不携带 `context.legalEntityCode`。  
- 用户在管理 UI 打开品牌/实体目录或模板高级区 / 预览回显。

---

## 7. Primary journey

1. 管理员在组内创建文档品牌 `HK-RETAIL-LETTER`（ACTIVE logo + 可选 seal + letterheadLegalName）。  
2. 管理员创建法人实体 `LE-HK-001`，选择绑定 `documentBrandCode=HK-RETAIL-LETTER`。  
3. 作者可选限制某国际信函模板 `allowedDocumentBrandCodes=["HK-RETAIL-LETTER","PLATFORM_DEFAULT"]`。  
4. 上游调用已发布版本，`context.legalEntityCode="LE-HK-001"` → 解析品牌 `HK-RETAIL-LETTER` → 产物信头/logo（及默认 seal 若适用）使用该品牌资产；壳层 UI 仍可为操作者所选 REDBC/GREENBC，互不影响。  
5. 审计/invocation 摘要可见 `legalEntityCode=LE-HK-001`、`documentBrandCode=HK-RETAIL-LETTER`。  
6. 对照：省略 `legalEntityCode` → 组默认实体品牌或 `PLATFORM_DEFAULT`；未知实体 → **422**；allow-list 外品牌 → **422**。

---

## 8. System responses（成功路径）

- 品牌/实体持久化并在 list/detail/UI 回显。  
- 实体改绑后，后续解析立即使用新品牌（已生成历史产物不变）。  
- 合法解析后产物品牌槽位反映 DocumentBrand 资产；UI chrome 不变。  
- 摘要回显 resolved codes；审计可关联。  
- REDBC/GREENBC 壳层切换回归通过。

---

## 9. Acceptance scenarios（Given / When / Then）

### BDD-IBL-E4-001 — Create document brand catalog entry

**Given** 组内 `GROUP_ADMIN`；已授权 logo 对象引用  
**When** 创建 DocumentBrand `documentBrandCode=HK-RETAIL-LETTER`（ACTIVE + logoObjectRef）  
**Then** `200/201`；list/detail/UI 回显该品牌；**不**出现在壳层 brand switcher 选项中

### BDD-IBL-E4-002 — Bind legal entity to document brand

**Given** ACTIVE 文档品牌 `HK-RETAIL-LETTER` 已存在  
**When** 创建 LegalEntity `LE-HK-001` 并选择 `documentBrandCode=HK-RETAIL-LETTER`  
**Then** 持久化成功；实体详情展示所选文档品牌；缺品牌绑定的创建请求 → **422**

### BDD-IBL-E4-003 — Re-select brand per legal entity

**Given** `LE-HK-001` 当前绑定 `HK-RETAIL-LETTER`；另有 ACTIVE 品牌 `UK-CORP-LETTER`  
**When** 管理员将 `LE-HK-001.documentBrandCode` 改为 `UK-CORP-LETTER`  
**Then** `200`；审计记录改绑（旧→新 code）；后续解析使用 `UK-CORP-LETTER`

### BDD-IBL-E4-004 — Context accepts legalEntityCode

**Given** 可调用已发布模板版本；`LE-HK-001` ACTIVE  
**When** sync generate 提交 `context.legalEntityCode="LE-HK-001"`（及既有合法字段）  
**Then** `400` **不**因未知字段；请求进入后续处理；成功路径摘要含该 code 与解析品牌

### BDD-IBL-E4-005 — Unknown legal entity fail-closed

**Given** 可调用已发布模板版本  
**When** generate 提交 `context.legalEntityCode="NO-SUCH-ENTITY"`  
**Then** **422** `LEGAL_ENTITY_UNKNOWN`（或等价稳定码）；**不**生成；**不**回落 UI chrome

### BDD-IBL-E4-006 — Inactive entity or brand fail-closed

**Given** 实体或绑定品牌为 `INACTIVE`  
**When** generate 携带该 `legalEntityCode`  
**Then** **422**（`LEGAL_ENTITY_INACTIVE` 或 `DOCUMENT_BRAND_INACTIVE`）；状态/产物不产生成功文件

### BDD-IBL-E4-007 — Omit legalEntity uses group default or PLATFORM_DEFAULT

**Given** 组未设默认法人，或默认无效；种子 `PLATFORM_DEFAULT` ACTIVE  
**When** generate **省略** `context.legalEntityCode`  
**Then** 解析 `documentBrandCode=PLATFORM_DEFAULT`（或组默认实体之绑定品牌，若已配置且 ACTIVE）；产物不使用 UI `REDBC`/`GREENBC` 主题资产冒充

### BDD-IBL-E4-008 — Apply brand assets to document slots

**Given** `LE-HK-001` → `HK-RETAIL-LETTER`（含 logo；可选 defaultSeal；letterheadLegalName）  
**When** preview 或 generate 成功  
**Then** 产物品牌槽位使用该品牌 logo（及适用的默认 seal / 信头法定名称）；invocation/preview 摘要回显 `resolvedDocumentBrandCode=HK-RETAIL-LETTER`

### BDD-IBL-E4-009 — Explicit sealRef wins over brand default seal

**Given** 品牌含 `defaultSealObjectRef`；模板/绑定显式 sealRef 指向另一授权签章且在授权区内  
**When** generate  
**Then** 使用显式 sealRef；不因品牌默认 seal 覆盖显式引用；若仅品牌默认 seal 出区 → B5 fail-closed

### BDD-IBL-E4-010 — Template allow-list rejects disallowed brand

**Given** 模板 `allowedDocumentBrandCodes=["HK-RETAIL-LETTER"]`；`LE-UK-001` 绑定 `UK-CORP-LETTER`  
**When** generate `legalEntityCode=LE-UK-001`  
**Then** **422** `DOCUMENT_BRAND_NOT_ALLOWED`（或等价）；不生成

### BDD-IBL-E4-011 — Empty allow-list permits any ACTIVE brand

**Given** 模板未设置或空 `allowedDocumentBrandCodes`  
**When** generate 解析到任一同组 ACTIVE 文档品牌  
**Then** allow-list **不**阻断（其余门禁满足时可成功）

### BDD-IBL-E4-012 — UI chrome orthogonal to document brand

**Given** 操作者壳层主题为 `GREENBC`；生成使用 `LE-HK-001` → `HK-RETAIL-LETTER`  
**When** 完成管理 UI 操作并成功生成/预览  
**Then** `html[data-brand]`（或等价）仍为操作者 UI 主题；文档产物品牌为 `HK-RETAIL-LETTER`；切换壳层 REDBC/GREENBC **不**改变已配置的实体→文档品牌绑定

### BDD-IBL-E4-013 — Management UI entity brand selector

**Given** 授权管理员打开 LegalEntity 创建/编辑  
**When** 从文档品牌选择器选择 ACTIVE 品牌并保存  
**Then** UI 回显选择；INACTIVE 品牌不可被新绑定（或选择后保存 422）；English-first 文案可见

### BDD-IBL-E4-014 — Non-admin cannot mutate catalogs

**Given** 仅 `TEMPLATE_AUTHOR`（非管理员）会话  
**When** 调用 DocumentBrand/LegalEntity 写 API 或 UI 写入口  
**Then** **403/404**；目录不变

### BDD-IBL-E4-015 — Audit records bind and resolve

**Given** 管理员改绑实体品牌后，调用方成功 generate  
**When** 查阅管理审计与 invocation/audit 摘要  
**Then** 含非敏感实体/品牌 code 与改绑 diff；**无** variables/客户明文/资产二进制

### BDD-IBL-E4-016 — Same resolver on test generation path

**Given** 模板测试生成携带 `context.legalEntityCode`（或省略走默认）  
**When** 测试生成执行  
**Then** 与 runtime **同一**解析/allow-list/应用语义；失败码一致

### BDD-IBL-E4-017 — SPECIMEN / PD-6 / PD-7 / orthogonality / non-activation

**Given** CE-G06 regenerate、E1 locale、E2 inclusion、E3 审批矩阵、B5 seal 已存在  
**When** E4 变更合并后执行 regenerate / 查阅计划与 checklist  
**Then** 成功样件仍含 SPECIMEN；不嵌入授权字体包；E1–E3/B5 行为不被削弱；**不**激活 E5–E7；**不**翻转 #3b/#5a；**不**发明 #119 Word 证据；F27 bulk/`effectiveFrom` **仍**属 E5

---

## 10. Boundary / exception

| 边界 | 行为 |
| --- | --- |
| 未认证 / 无组权限 | 401/403/404 惯例 |
| 未知 / INACTIVE 法人或品牌 | 422 fail-closed |
| allow-list 不匹配 | 422 |
| 创建实体无品牌绑定 | 422 |
| 绑定到不存在品牌 | 422 |
| 未知 context 字段 | 400 REQUEST_BODY_INVALID |
| 无文档品牌槽位的母版 | 非阻断 warning（固定码）；不得用 UI chrome 顶替 |
| 品牌默认 seal 出授权区 | B5 既有 fail-closed |
| 非管理员写目录 | 403/404 |
| 用壳层品牌码当 `documentBrandCode` | 非种子别名；若误用且目录无此码 → 422 |
| PD-6/7 / Word / E5 bulk | OUT — 不提供 |

---

## 11. Observable evidence

| 证据 | 说明 |
| --- | --- |
| API | brand/entity CRUD；context `legalEntityCode`；422 稳定码；resolved codes 回显 |
| DB | 组范围品牌/实体表（或等价）+ 迁移种子 `PLATFORM_DEFAULT` |
| 产物 | 预览/生成文件品牌槽位随实体品牌变化（fixture 可断言 object ref / 嵌入标记） |
| UI / E2E | 目录 + 选择器 + 与壳层主题正交（Playwright + UIUX） |
| Docs | 本 BDD；ADR-0065（Accept）；ADR-0013 Amendment；domain/permission/OpenAPI 指针 |
| Gates | `mvn verify`；FE 四门；E2E+UIUX；deploy queue |
| 负向 | 无 SPECIMEN 移除；无 #3b/#5a 翻转；无 E5–E7 / #119；无 PD-7 字体包 |

---

## 12. Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#131** | IBL-E4 delivery leaf |
| IBL program **F27** / **PD-9** / §7 IBL-E4 acceptance | 范围 + Done 草案（文档品牌半幅） |
| ADR-0065 **Accepted** | 架构 Decision（= E4-C*；≠ impl Done） |
| ADR-0013 | context 白名单 Amendment |
| PRD / requirements 品牌 logo 槽位 | UI chrome 基线（正交） |
| domain-model / permission-matrix | 目录与权限指针（doc-keeper 同步） |
| IBL-B5 | seal 几何正交 |
| IBL-E1 / E2 / E3 | 正交；不本叶激活后续叶 |
| IBL-E5 | F27 剩余 effectiveFrom/bulk |

---

## 13. Implementation notes（非产品发明；供 TDD）

- 错误码以实现为准，须稳定并写入 OpenAPI/`messageKey`/i18n。  
- `documentBrandCode` / `legalEntityCode` 建议 `[A-Z0-9][A-Z0-9_-]{0,63}` 或等价；非法格式 422。  
- 资产引用复用既有对象存储 / imageRef·sealRef 授权模型；禁止公网页抓取入库。  
- FE 遵循 `.cursor/skills/frontend-oa-design` 与 `i18n-english-first`；实体品牌选择器用 searchable select（frontend-entity-display）。  
- 壳层 `BrandPreset` 类型保持 `REDBC` \| `GREENBC`；**不要**把 DocumentBrand 塞进同一 union。  
- doc-keeper stage 3：Accept ADR-0065；同步 ADR-0013 Amendment、domain、permission、PRD/requirements 指针、OpenAPI+contract stubs、docs 索引；**不**宣称 impl Done。

---

## 14. Handoff

```text
bdd_readiness: ready
frontend_ui_in_scope: true
scenario_count: 17
scenario_ids:
  - BDD-IBL-E4-001
  - BDD-IBL-E4-002
  - BDD-IBL-E4-003
  - BDD-IBL-E4-004
  - BDD-IBL-E4-005
  - BDD-IBL-E4-006
  - BDD-IBL-E4-007
  - BDD-IBL-E4-008
  - BDD-IBL-E4-009
  - BDD-IBL-E4-010
  - BDD-IBL-E4-011
  - BDD-IBL-E4-012
  - BDD-IBL-E4-013
  - BDD-IBL-E4-014
  - BDD-IBL-E4-015
  - BDD-IBL-E4-016
  - BDD-IBL-E4-017
open_questions: []
suggested_adr_number: "0065"
suggested_adr_path: docs/adr/template-lifecycle/0065-legal-entity-document-brand-variants.md
adr_status: Accepted (2026-07-20 doc-keeper Stage 3) — Decision = E4-C*; Accepted ≠ impl Done
recommended_next_stage: backend-engineer + frontend-engineer (TDD; FE E2E mandatory; frontend_ui_in_scope=true)
```
