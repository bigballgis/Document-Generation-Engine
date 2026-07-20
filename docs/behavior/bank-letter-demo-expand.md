# BDD 行为规格：Wave B — Bank letter demo expand (new families)

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-DEMO-EXPAND` |
| **编写日期** | 2026-07-20 |
| **Slice** | `bank-letter-demo-expand` |
| **Branch** | `feat/bank-letter-demo-expand` |
| **Worktree** | `D:/working/DGE-bank-letter-demo-expand` |
| **Placement** | ISOLATED |
| **Integration base** | `origin/main` (Wave A #141 Done on MAIN) |
| **task_ids** | `["142"]` / alias `bank-letter-demo-expand` |
| **Batch recommendation** | **solo** — single leaf for seven new families + registry expansion evidence (amortize one verify + one queued deploy + one import/publish/generate run) |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [PRD §6.7](../product/PRD.md)（既有八类契约保留，本叶为目录扩展）；[bank-letter-demo-refresh.md](./bank-letter-demo-refresh.md)（Wave A Done）；[demo-expansion-behavior-spec.md](../requirements/demo-expansion-behavior-spec.md)（P22）；[demo-typography-layout-behavior-spec.md](../requirements/demo-typography-layout-behavior-spec.md)（P23）；[deploy/demo-shared/README.md](../../deploy/demo-shared/README.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（无新管理面 UI；English-first 仅约束演示文案/既有包双语模式；不新增 UI 文案键） |

```
bdd_readiness: ready
frontend_ui_in_scope: false
open_questions: []
owning_doc: docs/behavior/bank-letter-demo-expand.md
task_ids: ["142"]
scenario_count: 16
prior_leaf: bank-letter-demo-refresh  # Wave A Done — do not reopen
```

**完成声明约束：** 本叶在 Wave A 既有八包 + `DEMO-FULL-FLOW-LETTER` 已达银行信函质量条之后，**新增七个真实银行实务信函族**（包 + 变量 + 绑定 + SQL 模块 + executive 测试数据 + `*MasterDocxAssetGeneratorTest`），并扩展 `import-all` / `publish-all` / `generate-all` 注册表与 runtime generate manifest，产出真实 DOCX 证据。**禁止**重命名或替换 PRD §6.7 既有八类产品契约；**禁止**把 Commitment 伪装成 FOL / `DEMO-FOL-WHOLESALE` / `CORP-FOL-OFFER`；**禁止**翻转 checklist **#3b/#5a GO**；**禁止** RTL；**禁止**宣称 CE-O02 Done；**禁止**编造 Word 主机证据；**禁止**宣称 go-live / IBL program Done。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  rationale: >
    Seven new demo families share one registry expansion surface
    (import-all / publish-all / generate-all / generate-manifest).
    One leaf amortizes fixed pipeline cost; do not fold into Wave A.
  member_task_ids: ["142"]
  proposed_slice_id: bank-letter-demo-expand
  shared_acceptance_surface: >
    import-all-demos.ps1 + publish-all-demos.ps1 + generate-all-demos.ps1
    + expanded demo-runtime-generate-manifest.json
    + new *MasterDocxAssetGeneratorTest regenerable assets
  vetoes_applied:
    - CE-O02
    - GO-flip
    - RTL
    - rename-prd-eight
    - pretend-commitment-is-fol
  on_red_split_hint: Peel by domain (CORP vs RETAIL packages) if red
```

---

## 1. Goal

验收栈与仓库演示目录覆盖**常见银行实务信函族**中 Wave A 尚未具备的七类：facility amendment、KYC/CDD、account closure、commitment（独立于 FOL）、formal demand、covenant waiver、insurance endorsement。每类为可导入/可发布/可生成的真实国际银行信函（仅当事人、账号、金额、日期等为虚构 mock），银行品牌与当事人命名对齐既有演示中的 **Meridian Global Banking Corporation** / Meridian Retail Banking / Meridian 业务线用语。Ops 经既有编排一键导入扩展注册表并生成全量 DOCX 证据。

---

## 2. Actor / role

| Actor | 角色 | 范围 |
| --- | --- | --- |
| **Ops / demo engineer** | 新增 `deploy/demo-*` 包；扩展 import/publish/generate 注册表；跑证据编排 | 仓库演示包 + Docker 验收栈（`:8080` / `:4173`） |
| **Acceptance reviewer** | 抽查新族 DOCX 是否达银行信函质量条，且与 FOL/催收等既有族区分清晰 | `.tmp/generated_*.docx`、manifest、审计证据 |
| **Runtime caller**（既有） | `svc-caller` / `e2e-runtime-caller` 调已发布模板 generate | 既有 API policy AD group + credential；无新调用方请求字段 |
| **System** | 导入幂等、生命周期至 `PUBLISHED`、DOCX 组装 | 既有平台能力；本叶以**新包内容 + 注册表扩展证据**为主 |

---

## 3. Trigger

| 触发 | 说明 |
| --- | --- |
| **T1 — Wave B activate** | Wave A (#141) Done 后，用户要求补齐常见银行信函族（综合覆盖） |
| **T2 — Ops expand run** | 工程师在健康 Docker 栈上执行 `import-all-demos.ps1` → `publish-all-demos.ps1` → `generate-all-demos.ps1`（注册表含新 externalId） |
| **T3 — Build-time master regen** | 新包 `*MasterDocxAssetGeneratorTest` 再生 `deploy/demo-*/assets/*.docx` 锚点壳 |

---

## 4. Preconditions

1. Wave A (#141) **Done** — 既有八包 + `DEMO-FULL-FLOW-LETTER` 已达 Wave A 质量条；本叶**不** reopen Wave A 内容 uplift 任务。
2. Docker 验收栈健康（backend `/healthz`、frontend `:4173`）；部署仅经 `docker-deploy-queue.ps1`。
3. P22/P23 脚手架与编排脚本已存在：`deploy/import-all-demos.ps1`、`publish-all-demos.ps1`、`generate-all-demos.ps1`、`deploy/demo-shared/*`。
4. 新包目录契约镜像 `demo-fol` / Wave A 包：`assets/`、`config/`、`sql/`、`import-*-demo.ps1`。
5. Runtime generate 契约源：`deploy/demo-shared/demo-runtime-generate-manifest.json`（禁止 `LOREM` / `PLACEHOLDER` / `TODO` / `{{` / `}}`）。
6. 演示用组与 API policy 与 [demo-shared README](../../deploy/demo-shared/README.md) 一致（`CORP_API` / `RETAIL_API` 等）。
7. PRD §6.7 既有八类 `externalId` 契约**保留**；运行时 FOL 主键仍为 `CORP-FOL-OFFER`（不发明替代 FOL 产品名）。

---

## 5. Primary journey

1. **Author new packages（仓库）** — 为 §7 七族各建 `deploy/demo-<family>/`：variables、binding-overlays、SQL content modules、demo-test-variables、template/catalog/anchor configs、import 脚本、`*MasterDocxAssetGeneratorTest`。
2. **Brand / party alignment** — lender/bank 文案使用 Meridian Global Banking Corporation（CORP）或 Meridian Retail Banking / 既有 Meridian 业务线命名（RETAIL）；borrower/customer 使用与 Wave A 一致的虚构当事人风格（如 Pacific Rim Holdings 类 CORP 借款人；零售客户 mock）。
3. **Registry expand** — 更新 `import-all-demos.ps1`、publish externalId 列表、`demo-runtime-generate-manifest.json`（及任何镜像注册表）纳入七个新 `externalId`；Wave A 既有 **13** 个 ID **全部保留**。
4. **Bump markers** — 新包自带初始 `catalogMarker` / `masterLayoutVersion`；既有包仅在为注册表接线所必需时改动（避免无关 Wave A 重写）。
5. **Regenerate masters** — 运行新包 `*MasterDocxAssetGeneratorTest`，确认锚点壳可再生且兼容 `demo-bank-style-manifest` style keys。
6. **Import** — `.\deploy\import-all-demos.ps1`（八包 + 七新包；full-flow 经既有 seeder）。
7. **Publish** — `.\deploy\publish-all-demos.ps1` → 全部注册 `externalId`（既有 13 + 新 7 = **20**）达 `PUBLISHED`；写出 credentials 与 publish summary。
8. **Generate** — `.\deploy\generate-all-demos.ps1` → 每个注册 ID 的 `.tmp/generated_<externalId>.docx` + generate manifest；size floor + content markers；无 forbidden / meta-padding。
9. **Reviewer spot-check** — 七新族各至少抽查一份；并确认 Commitment ≠ FOL、Demand ≠ 普通逾期催收函。

---

## 6. System responses

### 6.1 Success

- 七新包导入幂等成功；模板生命周期为 **PUBLISHED**。
- 注册表从 Wave A 的 **13** 个 runtime `externalId` **扩展为 20**（+7）；既有 13 个仍可 import/publish/generate。
- API policy / credential 按组绑定：CORP 新族 → `CORP_API`；RETAIL 新族 → `RETAIL_API`。
- 每个新 `externalId` 产出 DOCX：字节数 ≥ manifest `minDocxBytes`；含约定 `contentMarkers`；无 forbidden patterns；满足 §8 质量条。
- Commitment 包生成物在标题/operative 语气上可识别为 **commitment / offer to commit**，且 **不是** FOL / wholesale facility offer documentation 的别名或裁剪伪装。

### 6.2 Fail-closed / ops-safe

- 导入/发布/生成任一步失败 → 不宣称本叶 Done；保留失败证据。
- Unauthorized runtime generate → 既有 fail-closed 授权（本叶不削弱）。
- Cleanup 不得整库 DROP；优先 re-import overwrite；禁止第二套 Compose / 并行 docker-deploy。
- 不得通过改名 PRD 八类或 FOL 运行时 ID「假装」完成扩展。

---

## 7. In-scope new package matrix（Wave B — minimum required）

**命名规则（confirmed）：**

- 新运行时主键一律使用 `DEMO-*` 前缀（与多数 Wave A 包一致）。
- **不**使用或改写 `DEMO-FOL-WHOLESALE` / `CORP-FOL-OFFER` 作为 Commitment。
- **不**发明与 PRD §6.7 八行冲突的「产品类型改名」；本叶是 **catalogue expand**（新增行），不是替换八类。
- 银行品牌：CORP → **Meridian Global Banking Corporation**（及适用业务线后缀）；RETAIL → **Meridian Retail Banking**（mortgage-adjacent 保险函可引用 Meridian 按揭/保险通知业务线，仍属 Meridian 家族）。

| # | Family (real banking practice) | Package path | Runtime externalId | Group | API AD group | Distinctness / notes |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Facility amendment / variation letter | `deploy/demo-facility-amendment/` | `DEMO-FACILITY-AMENDMENT` | CORP | `CORP_API` | 修订额度/期限/定价/条款；引用原 facility；含 variation 表/schedule |
| 2 | KYC / CDD customer due diligence notice | `deploy/demo-kyc-cdd/` | `DEMO-KYC-CDD-NOTICE` | RETAIL | `RETAIL_API` | 尽职调查/信息更新通知；资料清单；监管语气；非营销函 |
| 3 | Account closure notice | `deploy/demo-account-closure/` | `DEMO-ACCOUNT-CLOSURE` | RETAIL | `RETAIL_API` | 销户确认/通知；余额处理、最后结单、关闭生效日 |
| 4 | Commitment letter / offer commitment | `deploy/demo-commitment/` | `DEMO-COMMITMENT-LETTER` | CORP | `CORP_API` | **独立于 FOL**：短式 commitment / offer to lend commitment + CPs；**不是** `CORP-FOL-OFFER` 别名 |
| 5 | Formal demand letter | `deploy/demo-formal-demand/` | `DEMO-FORMAL-DEMAND` | CORP | `CORP_API` | Facility 项下正式催告/还款要求（可含加速权引用）；**升级于**零售 `DEMO-OVERDUE-COLLECTION`，不替换之 |
| 6 | Covenant waiver / consent letter | `deploy/demo-covenant-waiver/` | `DEMO-COVENANT-WAIVER` | CORP | `CORP_API` | 一次性豁免/同意；保留权；限定 waiver 范围与期限 |
| 7 | Insurance endorsement / security insurance notice | `deploy/demo-insurance-endorsement/` | `DEMO-INSURANCE-ENDORSEMENT` | RETAIL | `RETAIL_API` | 按揭担保物保险批单/保险要求通知（mortgage-adjacent）；利益注明银行 |

**每包交付物（mandatory）：**

| Artifact | Requirement |
| --- | --- |
| `*-variables.json` | 变量定义齐全，与 bindings/fixtures 对齐 |
| `*-binding-overlays.json` | 结构化绑定；`styleRef` 使用 demo-bank-style-manifest keys |
| `sql/001-*-*.sql` | 可执行法律文书语气的 content modules（非脚手架） |
| `*-demo-test-variables.json` | Executive 级 mock 测试数据（Meridian + 虚构客户） |
| template / catalog / anchor configs | 镜像既有 demo 包契约；可 import |
| `import-*-demo.ps1` | 纳入 `import-all-demos.ps1` |
| `*MasterDocxAssetGeneratorTest` | 锚点壳可再生 |

**对齐既有组：** CORP / RETAIL / TRADE / WEALTH — 本叶最低集仅新增 **CORP** 与 **RETAIL** 族（不强制新增 TRADE/WEALTH 包）。

**注册表目标计数：** Wave A **13** + Wave B **7** = **20** runtime `externalId`s（除非实现时某族合理拆成多模板——本叶 **confirmed 最低**为上表每族 **1** 个 ID；若拆分须仍满足质量条且全部进入 registry）。

---

## 8. Bank-letter quality bar（confirmed — same as Wave A §8）

每份新族目标信函（生成 DOCX 与其内容模块/绑定源）须同时满足：

| # | 要素 | 要求 |
| --- | --- | --- |
| Q1 | Parties | 清晰识别 lender/bank、borrower/customer（及 guarantor/agent/insurer 等适用角色） |
| Q2 | Defined terms | CORP / facility 类信函含定义术语块或等价引用 |
| Q3 | Operative clauses | 可执行义务/通知/确认/豁免/催告条款，非脚手架说明 |
| Q4 | Covenants | 授信修订/waiver/demand/commitment 等适用处含 covenant、监控或保留权条款 |
| Q5 | Schedules / tables | 适用处含 variation 表、资料清单、还款要求明细、保险明细等 |
| Q6 | Signature | 授权签署区（`SignatureBlock` 或等价） |
| Q7 | Governing law | 适用法/司法管辖陈述 |
| Q8 | No meta-padding | 禁止 `LOREM`/`PLACEHOLDER`/`TODO`；禁止「For the executive demonstration dataset…」「will be expanded in the final documentation set」等元叙述 |

视觉版式基线仍遵循 P23 / `demo-bank-style-manifest.json`（本叶不 reopen P23 任务状态）。

### 8.1 Family-specific operative expectations（confirmed）

| externalId | Must include (beyond Q1–Q8 as applicable) |
| --- | --- |
| `DEMO-FACILITY-AMENDMENT` | 原 facility 引用；amendment/variation 生效机制；修订条款表或 schedule |
| `DEMO-KYC-CDD-NOTICE` | 尽职调查目的；需补充资料清单；后果/时限（监管适当语气）；mock only |
| `DEMO-ACCOUNT-CLOSURE` | 账户识别；关闭生效日；余额/利息处理；最后结单或后续步骤 |
| `DEMO-COMMITMENT-LETTER` | commitment 金额/币种；可用性/有效期；Conditions Precedent；**明确非 FOL 全文** |
| `DEMO-FORMAL-DEMAND` | 欠款/违约引用；正式要求付款或补救；时限；保留加速/强制执行权利（设施语境） |
| `DEMO-COVENANT-WAIVER` | 被豁免 covenant 识别；waiver 范围/期限；非弃权其余权利；consent 条件 |
| `DEMO-INSURANCE-ENDORSEMENT` | 担保物/保单识别；银行作为 loss payee / interested party；保险金额或要求；按揭相邻语境 |

---

## 9. Out of scope（explicit — keeps BDD ready）

| Item | Disposition |
| --- | --- |
| **RTL / bidi 产品实现** | **OUT**（ADR-0068） |
| **CE-O02** | **仍 deferred / 不宣称 Done** |
| Checklist **#3b / #5a GO** 翻转 | **OUT** |
| 编造 MS Word 主机视觉基线 / pixel Word 证据 | **OUT** |
| 新管理 UI / 新 i18n UI 键 | **OUT**（`frontend_ui_in_scope=false`） |
| 重写 Wave A 八包正文（除非注册表接线必需的最小改动） | **OUT** — Wave A Done |
| 将 Commitment 实现为 FOL 裁剪/别名，或改名 `CORP-FOL-OFFER` | **OUT** |
| 改写 PRD §6.7 既有八行产品名 / 历史 `DEMO-FOL-WHOLESALE` 产品族表述为新产品 | **OUT**（可在 PRD 增加「Wave B 扩展」指针，由 doc-keeper 处理） |
| 强制新增 TRADE / WEALTH 新族 | **OUT**（本叶最低集未要求） |
| 鲁莽整库 DROP / 第二套 Docker compose | **OUT** |
| 宣称 go-live / IBL / CE program Done | **OUT** |

---

## 10. Locale / bilingual

| Rule | Behavior |
| --- | --- |
| English-first | 平台 i18n 与任何本叶触及的 UI 字符串（预期无）保持 English-first |
| Bilingual where pattern fits | CORP 设施类若对齐 FOL/credit 既有中英混排模式，**允许**在 SQL/bindings 中提供同等质量双语；不强制七包全量中文译本 |
| Default for new packages | 以 **English international/corporate bank letter** 为默认正文；质量优先于强行双语 |
| New UI copy | 无 |

---

## 11. Acceptance scenarios（Given / When / Then）

### BDD-DEMO-EXPAND-001 — Seven new packages exist with FOL-mirrored contracts

```gherkin
Given Wave A existing demo packages remain available
When  Wave B implementation completes for the seven families in §7
Then  each family has deploy/demo-<family>/ with assets/, config/, sql/, and import-*-demo.ps1
And   each package declares a unique DEMO-* templateExternalId from the §7 matrix
And   package contracts mirror the demo-fol / Wave A demo package layout
```

### BDD-DEMO-EXPAND-002 — Import-all registry includes Wave A + Wave B

```gherkin
Given the seven new packages and Wave A eight packages (+ full-flow seed path) are in the repo
When  the ops engineer runs deploy/import-all-demos.ps1 against a healthy stack
Then  all Wave A packages import successfully
And   all seven Wave B packages import successfully (idempotent)
And   DEMO-FULL-FLOW-LETTER remains available via the existing full-flow seed path
And   no Wave A externalId is removed to “make room” for Wave B
```

### BDD-DEMO-EXPAND-003 — Publish-all reaches PUBLISHED for expanded registry (20 IDs)

```gherkin
Given successful import of Wave A + Wave B demo catalogs
When  the ops engineer runs deploy/publish-all-demos.ps1
Then  every registered demo externalId reaches PUBLISHED via the existing lifecycle path
And   the publish registry count is Wave A 13 + Wave B 7 = 20
And   CORP Wave B IDs bind to CORP_API and RETAIL Wave B IDs bind to RETAIL_API
And   credential bundles are written under .tmp/credentials/<externalId>.json for each new ID
And   .tmp/evidence/all-demos-publish-summary.json records success per template including the seven new IDs
```

### BDD-DEMO-EXPAND-004 — Generate-all produces DOCX evidence for all new IDs

```gherkin
Given all target templates (20) are PUBLISHED with credentials available
And   demo-runtime-generate-manifest.json lists all seven new externalIds with minDocxBytes and contentMarkers
When  the ops engineer runs deploy/generate-all-demos.ps1
Then  each new template yields .tmp/generated_<externalId>.docx
And   each new DOCX meets its minDocxBytes floor
And   each new DOCX contains its declared contentMarkers
And   generated-docx-manifest.json and per-template audit records include the seven new IDs
And   DOCX text contains none of the forbiddenPatterns (LOREM, PLACEHOLDER, TODO, {{, }})
And   DOCX text contains none of the Q8 meta-padding phrases
```

### BDD-DEMO-EXPAND-005 — Facility amendment meets quality bar

```gherkin
Given package demo-facility-amendment is authored to bank-letter quality
When  DEMO-FACILITY-AMENDMENT is imported, published, and generated
Then  the DOCX satisfies Q1–Q8
And   parties identify Meridian Global Banking Corporation (or clear Meridian CORP lender style) and the borrower
And   operative content references the existing facility and states the variation/amendment terms
And   a variation schedule or equivalent structured table is present
```

### BDD-DEMO-EXPAND-006 — KYC/CDD notice meets quality bar

```gherkin
Given package demo-kyc-cdd is authored to bank-letter quality
When  DEMO-KYC-CDD-NOTICE is imported, published, and generated
Then  the DOCX satisfies Q1–Q8 as applicable to retail regulatory correspondence
And   the letter states a due-diligence / information-update purpose
And   a document or information checklist schedule is present
And   tone is regulatory-appropriate (mock data only; not marketing fluff)
```

### BDD-DEMO-EXPAND-007 — Account closure notice meets quality bar

```gherkin
Given package demo-account-closure is authored to bank-letter quality
When  DEMO-ACCOUNT-CLOSURE is imported, published, and generated
Then  the DOCX satisfies Q1–Q8 as applicable to retail account correspondence
And   the letter identifies the account and closure effective date
And   operative clauses cover balance handling and final statement / next steps
And   Meridian Retail Banking (or clear Meridian retail lender style) appears as the bank party
```

### BDD-DEMO-EXPAND-008 — Commitment letter is distinct from FOL

```gherkin
Given CORP-FOL-OFFER (Wave A FOL) remains the wholesale facility offer documentation template
And   package demo-commitment provides DEMO-COMMITMENT-LETTER
When  both templates are generated with their executive fixtures
Then  DEMO-COMMITMENT-LETTER reads as a commitment / offer-to-commit letter
      (commitment amount, validity, conditions precedent, signature, governing law)
And   DEMO-COMMITMENT-LETTER is not a rename, alias, or trimmed copy presented as FOL
And   CORP-FOL-OFFER externalId and package path remain unchanged
And   PRD §6.7 FOL product-family naming is not replaced by the commitment product name
```

### BDD-DEMO-EXPAND-009 — Formal demand meets quality bar and escalates beyond retail overdue

```gherkin
Given DEMO-OVERDUE-COLLECTION remains the retail overdue collection notice
And   package demo-formal-demand provides DEMO-FORMAL-DEMAND
When  DEMO-FORMAL-DEMAND is imported, published, and generated
Then  the DOCX satisfies Q1–Q8 for a corporate formal demand under a facility
And   operative clauses state the sum demanded, deadline, and reservation of enforcement/acceleration rights
And   the letter is distinguishable from DEMO-OVERDUE-COLLECTION (facility formal demand vs retail overdue notice)
And   DEMO-OVERDUE-COLLECTION is not removed or repurposed
```

### BDD-DEMO-EXPAND-010 — Covenant waiver / consent meets quality bar

```gherkin
Given package demo-covenant-waiver is authored to bank-letter quality
When  DEMO-COVENANT-WAIVER is imported, published, and generated
Then  the DOCX satisfies Q1–Q8
And   the waived or consented covenant is specifically identified
And   waiver scope and duration (or one-off nature) are stated
And   reservation-of-rights / non-waiver of other defaults language is present
```

### BDD-DEMO-EXPAND-011 — Insurance endorsement / security insurance notice meets quality bar

```gherkin
Given package demo-insurance-endorsement is authored to bank-letter quality
When  DEMO-INSURANCE-ENDORSEMENT is imported, published, and generated
Then  the DOCX satisfies Q1–Q8 as applicable to mortgage-adjacent insurance correspondence
And   the secured property / policy is identified
And   the bank’s interest (e.g. loss payee / interested party) is stated
And   insurance requirements or endorsement particulars appear in operative text or a schedule
```

### BDD-DEMO-EXPAND-012 — Variables, bindings, SQL, and fixtures stay aligned

```gherkin
Given each Wave B package has variables, binding-overlays, SQL content modules, and *-demo-test-variables.json
When  an implementer changes a binding variable key or module reference
Then  the matching variables JSON, SQL module pins/refs, and executive test fixture are updated in the same change set
And   generate-all using the package fixture succeeds without unresolved placeholder tokens
```

### BDD-DEMO-EXPAND-013 — Master DOCX shells remain regenerable for new packages

```gherkin
Given *MasterDocxAssetGeneratorTest classes exist for each of the seven Wave B packages
When  those tests are executed in the feature worktree
Then  deploy/demo-*/assets/*.docx anchor shells for the seven packages regenerate successfully
And   shells remain compatible with demo-bank-style-manifest style keys (P23 baseline)
```

### BDD-DEMO-EXPAND-014 — Forbidden patterns gate across new sources

```gherkin
Given Wave B content sources (SQL JSON bodies, bindings, demo test variables, generated DOCX)
When  a static or generate-time scan applies demo-runtime-generate-manifest forbiddenPatterns
      plus the meta-padding phrases listed in Q8
Then  no in-scope Wave B package source or generated DOCX contains those patterns
```

### BDD-DEMO-EXPAND-015 — Bilingual optional; English-first; no new UI keys

```gherkin
Given frontend_ui_in_scope is false for this leaf
When  Wave B packages are authored
Then  default letter prose is coherent English international/corporate bank correspondence
And   bilingual CJK may be added only where it fits existing CORP demo patterns and remains bank-grade
And   no new management-UI i18n keys are introduced by this leaf
```

### BDD-DEMO-EXPAND-016 — Governance vetoes remain intact

```gherkin
Given launch-readiness and IBL governance constraints
When  Wave B demo expand completes
Then  checklist items #3b and #5a are not flipped to GO by this leaf
And   no RTL product behavior is introduced
And   CE-O02 remains deferred (not claimed Done)
And   no MS Word host visual baseline evidence is invented
And   go-live / IBL program Done is not claimed
```

---

## 12. Boundary / exception behavior

| Case | Expected |
| --- | --- |
| Import conflict / marker collision | Fail honestly or idempotent overwrite after marker bump；不静默留下空壳包当成功 |
| Publish gate failure | Script fails；新模板不得被宣称 PUBLISHED；无 generate 证据 |
| Generate size/marker miss | `generate-all` / contract tests fail；本叶不 Done |
| Commitment implemented as FOL alias | Review **reject**；必须独立 package + `DEMO-COMMITMENT-LETTER` |
| Accidental removal of Wave A IDs from registry | Review **reject**；registry 必须 13+7 |
| Parallel docker-deploy | Forbidden；必须排队 |
| Unauthorized generate | Fail-closed（既有模型） |
| Same-release republish | 遵循 Wave A / lifecycle：prior `PUBLISHED` 同版本 → `STOPPED` + `DEACTIVATE_VERSION` audit（既有行为；本叶不削弱） |

---

## 13. Observable evidence

| Evidence | Path / signal |
| --- | --- |
| Publish summary | `.tmp/evidence/all-demos-publish-summary.json`（含 7 新 ID） |
| Credentials | `.tmp/credentials/<externalId>.json`（含 7 新 ID） |
| Generated DOCX | `.tmp/generated_<externalId>.docx`（7 新文件 + 既有 13 仍可生成） |
| Generate manifest | `.tmp/evidence/generated-docx-manifest.json` |
| Audit records | `.tmp/evidence/audit-records/<externalId>.json` |
| Registry sources | `import-all-demos.ps1`、publish ID helper、`demo-runtime-generate-manifest.json` |
| Contract tests | `DemoPublishOrchestrationContractTest` / `DemoGenerateOrchestrationContractTest` 扩展覆盖新 ID（或等价契约更新） |
| Master regen | 七个新 `*MasterDocxAssetGeneratorTest` green |
| Human spot-check | Reviewer notes against §8 / §8.1；Commitment ≠ FOL；Demand ≠ overdue |

---

## 14. Traceability

| Source | Link |
| --- | --- |
| User intent (authoritative) | Wave B：补齐真实银行实务常见信函族；Meridian 命名；CORP/RETAIL/TRADE/WEALTH 组模型；非 PRD 冲突产品名 |
| Prior Wave A BDD | [bank-letter-demo-refresh.md](./bank-letter-demo-refresh.md)（Done；本叶不 reopen） |
| PRD demo table | [PRD §6.7](../product/PRD.md) — 既有八类保留；本叶 catalogue expand |
| P22 behavior | [demo-expansion-behavior-spec.md](../requirements/demo-expansion-behavior-spec.md) |
| P23 typography | [demo-typography-layout-behavior-spec.md](../requirements/demo-typography-layout-behavior-spec.md)（版式基线；不 reopen） |
| Ops orchestration | [deploy/demo-shared/README.md](../../deploy/demo-shared/README.md) |
| Runtime manifest | `deploy/demo-shared/demo-runtime-generate-manifest.json` |
| Task / slice | TM **#142** / `bank-letter-demo-expand` |

---

## 15. BDD readiness declaration

| Field | Value |
| --- | --- |
| **bdd_readiness** | **`ready`** |
| **open_questions** | `[]` — 七族矩阵、externalId、组归属、Commitment≠FOL、注册表 13→20、vetoes 均已确认 |
| **owning_doc** | `docs/behavior/bank-letter-demo-expand.md` |
| **Handoff** | `plan-orchestrator` → doc-keeper / rendering-engineer（新包 + 注册表）in worktree `D:/working/DGE-bank-letter-demo-expand` |
| **frontend_ui_in_scope** | `false`（E2E/UIUX product journeys **N/A**；generate evidence is script/API） |

---

## 16. Scenario ID index

1. `BDD-DEMO-EXPAND-001` — Seven packages + FOL-mirrored contracts  
2. `BDD-DEMO-EXPAND-002` — Import-all registry (Wave A + Wave B)  
3. `BDD-DEMO-EXPAND-003` — Publish-all expanded registry (20 IDs)  
4. `BDD-DEMO-EXPAND-004` — Generate-all DOCX evidence for new IDs  
5. `BDD-DEMO-EXPAND-005` — Facility amendment quality bar  
6. `BDD-DEMO-EXPAND-006` — KYC/CDD quality bar  
7. `BDD-DEMO-EXPAND-007` — Account closure quality bar  
8. `BDD-DEMO-EXPAND-008` — Commitment distinct from FOL  
9. `BDD-DEMO-EXPAND-009` — Formal demand quality + distinct from overdue  
10. `BDD-DEMO-EXPAND-010` — Covenant waiver quality bar  
11. `BDD-DEMO-EXPAND-011` — Insurance endorsement quality bar  
12. `BDD-DEMO-EXPAND-012` — Variables/bindings/SQL/fixtures alignment  
13. `BDD-DEMO-EXPAND-013` — MasterDocx regenerable (7 packages)  
14. `BDD-DEMO-EXPAND-014` — Forbidden / meta-padding gate  
15. `BDD-DEMO-EXPAND-015` — English-first / optional bilingual / no UI keys  
16. `BDD-DEMO-EXPAND-016` — Governance vetoes (RTL, CE-O02, GO flips)
