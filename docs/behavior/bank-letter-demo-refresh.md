# BDD 行为规格：Wave A — Bank letter demo content refresh

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-DEMO-REFRESH` |
| **编写日期** | 2026-07-20 |
| **Slice** | `bank-letter-demo-refresh` |
| **Branch** | `feat/bank-letter-demo-refresh` |
| **Worktree** | `D:/working/DGE-bank-letter-demo-refresh` |
| **Placement** | ISOLATED |
| **Integration base** | `main` (local tip `d4cbc0b9` at leaf provision; origin fetch may be stale) |
| **task_ids** | `["demo-refresh-wave-a"]` |
| **Batch recommendation** | **split** — Wave A = existing-package quality uplift + stack cleanup/reimport；Wave B expand → serial next leaf `bank-letter-demo-expand` |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [PRD §6.7](../product/PRD.md) 八类演示表；[demo-expansion-behavior-spec.md](../requirements/demo-expansion-behavior-spec.md)（P22）；[demo-typography-layout-behavior-spec.md](../requirements/demo-typography-layout-behavior-spec.md)（P23）；[deploy/demo-shared/README.md](../../deploy/demo-shared/README.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（无新管理面 UI；English-first 仅在既有包已双语处加强文案，不新增 UI 文案键） |

```
bdd_readiness: ready
frontend_ui_in_scope: false
open_questions: []
owning_doc: docs/behavior/bank-letter-demo-refresh.md
task_ids: ["demo-refresh-wave-a"]
scenario_count: 14
next_leaf: bank-letter-demo-expand  # Wave B — OUT OF SCOPE this leaf
```

**完成声明约束：** 本叶在深度系统整改之后，清理验收栈与 `deploy/demo-*` 中浅层/填充/测试味 demo 内容，并将**既有八包 + `DEMO-FULL-FLOW-LETTER`** 提升为 100% 仿真国际/公司银行信函质量；经既有 `import-all` → `publish-all` → `generate-all` 产出真实 DOCX 证据。**禁止**交付 Wave B 新信函族；**禁止**翻转 checklist **#3b/#5a GO**；**禁止** RTL；**禁止**宣称 CE-O02 Done；**禁止**编造 Word 主机证据。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: split
  rationale: >
    Caps exceeded for refresh + expand; Wave A amortizes one verify + one
    queued deploy + one import/generate evidence run on existing packages only.
  member_task_ids: ["demo-refresh-wave-a"]
  proposed_slice_id: bank-letter-demo-refresh
  shared_acceptance_surface: >
    import-all-demos.ps1 + publish-all-demos.ps1 + generate-all-demos.ps1
    + *MasterDocxAssetGeneratorTest regenerable assets
  vetoes_applied:
    - hard-cap-files
    - expand-wave-b
    - CE-O02
    - GO-flip
    - RTL
  on_red_split_hint: Peel by domain (CORP vs RETAIL vs TRADE) if red
```

---

## 1. Goal

验收栈与仓库演示包中的银行信函 demo **读起来像真实国际/公司银行对外信函**（仅当事人、账号、金额、日期等为虚构 mock），而非脚手架填充、元叙述或测试说明文字。Ops/demo 工程师可一键清理浅层数据、重导入、发布并生成全量 DOCX 证据。

---

## 2. Actor / role

| Actor | 角色 | 范围 |
| --- | --- | --- |
| **Ops / demo engineer** | 维护 `deploy/demo-*`、执行 cleanup / import / publish / generate | 仓库演示包 + Docker 验收栈（`:8080` / `:4173`） |
| **Acceptance reviewer** | 抽查生成 DOCX 是否达银行信函质量条 | `.tmp/generated_*.docx`、manifest、审计证据 |
| **Runtime caller**（既有） | `svc-caller` / `e2e-runtime-caller` 调已发布模板 generate | 既有 API policy AD group + credential；无新调用方字段 |
| **System** | 导入幂等、生命周期至 `PUBLISHED`、DOCX 组装 | 既有平台能力；本叶以**内容与编排证据**为主 |

---

## 3. Trigger

| 触发 | 说明 |
| --- | --- |
| **T1 — Post-remediation refresh** | 系统多轮深度整改后，用户要求清理测试味 demo 并倒入百分百仿真银行信函 |
| **T2 — Ops refresh run** | 工程师在健康 Docker 栈上执行 cleanup（可选）→ `import-all-demos.ps1` → `publish-all-demos.ps1` → `generate-all-demos.ps1` |
| **T3 — Build-time master regen** | `*MasterDocxAssetGeneratorTest` 再生 `deploy/demo-*/assets/*.docx` 锚点壳 |

---

## 4. Preconditions

1. Docker 验收栈健康（backend `/healthz`、frontend `:4173`）；部署仅经 `docker-deploy-queue.ps1`。
2. P22/P23 脚手架与编排脚本已存在：`deploy/import-all-demos.ps1`、`publish-all-demos.ps1`、`generate-all-demos.ps1`、`deploy/demo-shared/*`。
3. 八包目录契约镜像 `demo-fol`（`assets/`、`config/`、`sql/`、`import-*-demo.ps1`）。
4. Runtime generate 契约源：`deploy/demo-shared/demo-runtime-generate-manifest.json`（已禁止 `LOREM` / `PLACEHOLDER` / `TODO` / `{{` / `}}`）。
5. 演示用组与 API policy 与 [demo-shared README](../../deploy/demo-shared/README.md) 一致（`CORP_API` / `RETAIL_API` 等）。
6. Wave B 新信函族**未**纳入本叶（见 §9）。

---

## 5. Primary journey

1. **Survey / rewrite（仓库）** — 重写浅层 SQL/bindings/variables/test fixtures；FOL 替换 `lma-clause-library.ps1` 填充模板与 `001-fol-standard-clauses.sql` 中元叙述段落为真实 LMA 风格 schedule 正文。
2. **Bump markers** — 必要时 bump `catalogMarker` / `masterLayoutVersion` 以触发幂等重导入覆盖。
3. **Regenerate masters（可选但应可重复）** — 运行相关 `*MasterDocxAssetGeneratorTest`，确认锚点壳可再生。
4. **Ops-safe cleanup（验收栈）** — 优先使用既有脚本（如 `cleanup-fol-test-data-sets.ps1`、按需 `cleanup-catalog-except-fol.ps1`、`demo-import-shared.ps1` DRAFT reset）；**优先 re-import overwrite**，禁止鲁莽 `DROP` 整库。
5. **Import** — `.\deploy\import-all-demos.ps1`（八包；full-flow 经既有 seeder 路径）。
6. **Publish** — `.\deploy\publish-all-demos.ps1` → 全部目标 `externalId` 达 `PUBLISHED`；写出 `.tmp/credentials/<externalId>.json` 与 publish summary。
7. **Generate** — `.\deploy\generate-all-demos.ps1` → `.tmp/generated_<externalId>.docx` + `.tmp/evidence/generated-docx-manifest.json`；满足 size floor 与 content markers；无 forbidden patterns。
8. **Reviewer spot-check** — 按质量条抽查 CORP / RETAIL / TRADE / WEALTH 各至少一类。

---

## 6. System responses

### 6.1 Success

- 导入幂等覆盖成功；模板生命周期为 **PUBLISHED**（经既有 publish 脚本路径）。
- API policy / credential 包按既有编排写出，runtime generate 成功。
- 每个目标 `externalId` 产出 DOCX：字节数 ≥ manifest `minDocxBytes`；正文含约定 `contentMarkers`；不含 forbidden patterns。
- FOL schedule/条款模块正文为**可执行法律文书语气**，非「将在最终文档集中扩展」类元叙述。

### 6.2 Fail-closed / ops-safe

- 导入/发布/生成任一步失败 → 不宣称本叶 Done；保留失败证据（脚本输出、summary JSON）。
- Unauthorized runtime generate → 既有 fail-closed 授权（本叶不削弱）。
- Cleanup 不得删除非目标生产数据；默认不整库 DROP；`-WhatIf` 类预览在 FOL cleanup 脚本中可用则应保留。

---

## 7. In-scope package & template matrix（Wave A）

沿用 PRD §6.7 / P23 八类 + full-flow。**运行时 `externalId` 以 demo-shared 发布注册表为准**（FOL 运行时主键为 `CORP-FOL-OFFER`；PRD 历史行 `DEMO-FOL-WHOLESALE` 不在本叶改名或发明新产品名）。

| # | Package | Runtime externalId(s) | Group | Quality focus |
| --- | --- | --- | --- | --- |
| 1 | `deploy/demo-fol/` | `CORP-FOL-OFFER` | CORP | LMA-style FOL；schedules 1–n 真实 operative 内容；executive 规模保持 |
| 2 | `deploy/demo-retail-account/` | `DEMO-RETAIL-ACCOUNT-OPEN`, `DEMO-RETAIL-ACCOUNT-BALANCE` | RETAIL | 开户/余额确认信；fee schedule；分行页脚 |
| 3 | `deploy/demo-mortgage/` | `DEMO-MORTGAGE-APPROVAL` | RETAIL | 批核函 + 还款/摊销表 |
| 4 | `deploy/demo-credit-limit/` | `DEMO-CREDIT-LIMIT-CONFIRM` | CORP | 额度确认；defined terms；covenant 表 |
| 5 | `deploy/demo-trade-lc/` | `DEMO-TRADE-LC-NOTICE`, `DEMO-TRADE-GUARANTEE-NOTICE` | TRADE | LC/保函通知；UCP/URDG；单据清单 |
| 6 | `deploy/demo-collection/` | `DEMO-RATE-CHANGE-NOTICE`, `DEMO-OVERDUE-COLLECTION` | RETAIL | 利率变更 / 逾期催收；监管语气 |
| 7 | `deploy/demo-annual-review/` | `DEMO-ANNUAL-REVIEW`, `DEMO-FACILITY-RENEWAL` | CORP | 年审 / 续期；covenant loop |
| 8 | `deploy/demo-wealth/` | `DEMO-WEALTH-STATEMENT` | WEALTH | 私人银行结单；多表持仓 |
| 9 | full-flow（seeder） | `DEMO-FULL-FLOW-LETTER` | RETAIL | 与家族 demo 同级真实信函语气 |

**对齐面（每包）：** `*-variables.json`、`*-binding-overlays.json`、`sql/001-*-*.sql`、`*-demo-test-variables.json`、template/catalog/anchor configs、对应 `*MasterDocxAssetGeneratorTest`。

---

## 8. Bank-letter quality bar（confirmed）

每份目标信函（生成 DOCX 与其内容模块/绑定源）须同时满足：

| # | 要素 | 要求 |
| --- | --- | --- |
| Q1 | Parties | 清晰识别 lender/bank、borrower/customer（及 guarantor/agent 等适用角色） |
| Q2 | Defined terms | CORP/facility 类信函含定义术语块或等价引用 |
| Q3 | Operative clauses | 可执行义务/通知/确认条款，非脚手架说明 |
| Q4 | Covenants | 授信/年审/FOL 等适用处含 covenant 或监控条款 |
| Q5 | Schedules / tables | 适用处含 schedule、清单、摊销、持仓等结构化表 |
| Q6 | Signature | 授权签署区（`SignatureBlock` 或等价） |
| Q7 | Governing law | 适用法/司法管辖陈述（贸易函可引用 UCP/URDG 等规则集） |
| Q8 | No meta-padding | 禁止 `LOREM`/`PLACEHOLDER`/`TODO`；禁止「For the executive demonstration dataset…」「will be expanded in the final documentation set」等元叙述/填充 |

视觉版式基线仍遵循 P23 / `demo-bank-style-manifest.json`（本叶不 reopen P23 任务状态；内容 uplift 须保持 styleRef 契约）。

---

## 9. Out of scope（explicit — keeps BDD ready）

| Item | Disposition |
| --- | --- |
| **Wave B 新信函族**（commitment letter、KYC/CDD、account closure、facility amendment、covenant waiver、demand letter、insurance endorsement 等） | **OUT** → 下一串行叶 `bank-letter-demo-expand` |
| Checklist **#3b / #5a GO** 翻转 | **OUT** |
| RTL / bidi 产品实现 | **OUT**（ADR-0068） |
| CE-O02 | **仍 deferred / 不宣称 Done** |
| 编造 MS Word 主机视觉基线 / pixel Word 证据 | **OUT** |
| 新管理 UI / 新 i18n UI 键 | **OUT**（`frontend_ui_in_scope=false`） |
| 鲁莽整库 DROP / 第二套 Docker compose | **OUT** |

用户「各种信函都要有」在本叶解释为：**既有八类 + full-flow 全覆盖且真实**；**新增族**明确排入 Wave B，不阻塞本叶 `bdd_readiness: ready`。

---

## 10. Locale / bilingual

| Rule | Behavior |
| --- | --- |
| English-first | 平台 i18n 与任何本叶触及的 UI 字符串（预期无）保持 English-first |
| Existing bilingual | 若包已含中英混排（当前调查：FOL SQL/bindings 含 CJK），刷新后**保持并提升**双语质量，不得退化为纯填充英文字符串 |
| Monolingual packages | retail/mortgage/credit/trade/collection/annual-review/wealth 当前以英文银行信函为主 → 保持英文银行信函语气；**不强制**本叶新增全量中文译本 |
| New UI copy | 无 |

---

## 11. Acceptance scenarios（Given / When / Then）

### BDD-DEMO-REFRESH-001 — Ops-safe cleanup before refresh

```gherkin
Given a healthy acceptance stack that may contain shallow or duplicate demo test data sets
When  the ops engineer runs the preferred cleanup path
      (existing scripts such as cleanup-fol-test-data-sets.ps1 and/or DRAFT reset via demo-import-shared,
       then import-all overwrite — not a reckless database DROP)
Then  obsolete/shallow/duplicate demo test data is removed or superseded
And   platform non-demo data required for stack health remains intact
And   cleanup is idempotent / re-runnable without destructive surprise
```

### BDD-DEMO-REFRESH-002 — Import-all overwrites with bank-grade packages

```gherkin
Given Wave A rewritten content exists under all eight deploy/demo-* packages
And   catalogMarker / masterLayoutVersion are bumped when content materially changes
When  the ops engineer runs deploy/import-all-demos.ps1 against the healthy stack
Then  all eight packages import successfully (idempotent overwrite)
And   DEMO-FULL-FLOW-LETTER remains available via the existing full-flow seed path
And   no package retains the pre-refresh shallow padding as the published content source
```

### BDD-DEMO-REFRESH-003 — Publish-all reaches PUBLISHED + credentials

```gherkin
Given successful import of the Wave A demo catalog
When  the ops engineer runs deploy/publish-all-demos.ps1
Then  every registered demo externalId reaches PUBLISHED via the existing lifecycle path
And   API policy AD-group bindings match deploy/demo-shared/README.md
And   credential bundles are written under .tmp/credentials/<externalId>.json
And   .tmp/evidence/all-demos-publish-summary.json records success per template
```

### BDD-DEMO-REFRESH-004 — Generate-all produces real DOCX evidence

```gherkin
Given all target templates are PUBLISHED with credentials available
When  the ops engineer runs deploy/generate-all-demos.ps1
Then  each template in demo-runtime-generate-manifest.json yields .tmp/generated_<externalId>.docx
And   each DOCX meets its minDocxBytes floor
And   each DOCX contains its declared contentMarkers
And   generated-docx-manifest.json and per-template audit records are written under .tmp/evidence/
And   DOCX text contains none of the forbiddenPatterns (LOREM, PLACEHOLDER, TODO, {{, }})
```

### BDD-DEMO-REFRESH-005 — FOL schedules are LMA-grade operative content

```gherkin
Given deploy/demo-fol SQL modules and/or lma-clause-library generators are the content source for FOL schedules
When  CORP-FOL-OFFER is imported, published, and generated
Then  Schedule / clause module prose reads as LMA-style facility documentation
      (parties, CP, representations, utilisation, fees, security principles, etc. as applicable)
And   the generated DOCX does not contain meta-padding such as
      "For the executive demonstration dataset" or "will be expanded in the final documentation set"
And   contentMarkers for CORP-FOL-OFFER (e.g. Pacific Rim Holdings, Meridian Global Banking Corporation, Borrower) are present
```

### BDD-DEMO-REFRESH-006 — CORP packages meet quality bar

```gherkin
Given packages demo-fol, demo-credit-limit, and demo-annual-review are Wave A refreshed
When  their PUBLISHED templates are generated with executive fixtures
Then  each DOCX satisfies Q1–Q8 in §8 (parties, defined terms, operative clauses,
      covenants where applicable, schedules/tables, signature, governing law, no meta-padding)
```

### BDD-DEMO-REFRESH-007 — RETAIL packages meet quality bar

```gherkin
Given packages demo-retail-account, demo-mortgage, demo-collection, and DEMO-FULL-FLOW-LETTER are Wave A refreshed
When  their PUBLISHED templates are generated with executive fixtures
Then  each DOCX satisfies Q1–Q8 as applicable to retail correspondence
And   mortgage includes an amortization / repayment schedule table
And   collection notices use regulatory-appropriate overdue / rate-change language (still mock data only)
```

### BDD-DEMO-REFRESH-008 — TRADE and WEALTH packages meet quality bar

```gherkin
Given packages demo-trade-lc and demo-wealth are Wave A refreshed
When  their PUBLISHED templates are generated with executive fixtures
Then  trade notices reference appropriate rule sets (e.g. UCP 600 / URDG 758) and document checklists
And   wealth statement includes multi-table holdings / totals consistent with private-bank statement practice
And   both satisfy Q1–Q8 as applicable (no meta-padding)
```

### BDD-DEMO-REFRESH-009 — Variables, bindings, SQL, and test fixtures stay aligned

```gherkin
Given each demo package has variables, binding-overlays, SQL content modules, and *-demo-test-variables.json
When  an implementer changes a binding variable key or module reference during Wave A
Then  the matching variables JSON, SQL module pins/refs, and executive test fixture are updated in the same change set
And   generate-all using the package fixture succeeds without unresolved placeholder tokens
```

### BDD-DEMO-REFRESH-010 — Master DOCX shells remain regenerable

```gherkin
Given *MasterDocxAssetGeneratorTest classes exist for fol, retail-account, mortgage, credit-limit,
      trade-lc, collection, annual-review, wealth, and full-flow
When  those tests are executed in the feature worktree
Then  deploy/demo-*/assets/*.docx anchor shells regenerate successfully
And   shells remain compatible with demo-bank-style-manifest style keys (P23 baseline)
```

### BDD-DEMO-REFRESH-011 — Forbidden patterns gate across sources

```gherkin
Given Wave A content sources (SQL JSON bodies, bindings, demo test variables, generated DOCX)
When  a static or generate-time scan applies demo-runtime-generate-manifest forbiddenPatterns
      plus the meta-padding phrases listed in Q8
Then  no in-scope package source or generated DOCX contains those patterns
```

### BDD-DEMO-REFRESH-012 — Bilingual preserved where already present; English-first otherwise

```gherkin
Given FOL package sources already include Chinese characters in SQL/bindings
And   other Wave A packages are English bank-letter sources
When  Wave A refresh completes
Then  FOL retains (and improves) bilingual bank-letter quality where CJK was already present
And   other packages remain coherent English international/corporate bank letters
And   no new management-UI i18n keys are introduced by this leaf
```

### BDD-DEMO-REFRESH-013 — Wave B families explicitly deferred

```gherkin
Given the user wants comprehensive coverage of "all kinds of bank letters"
When  Wave A is planned and delivered
Then  coverage means the eight existing packages plus DEMO-FULL-FLOW-LETTER at bank-grade quality
And   new letter families (commitment, KYC/CDD, account closure, facility amendment,
      covenant waiver, demand, insurance endorsement, …) are documented as OUT OF SCOPE
And   those families are queued for the serial next leaf bank-letter-demo-expand
And   this leaf's bdd_readiness remains ready (not blocked on Wave B scope)
```

### BDD-DEMO-REFRESH-014 — Governance vetoes remain intact

```gherkin
Given launch-readiness and IBL governance constraints
When  Wave A demo refresh completes
Then  checklist items #3b and #5a are not flipped to GO by this leaf
And   no RTL product behavior is introduced
And   CE-O02 remains deferred (not claimed Done)
And   no MS Word host visual baseline evidence is invented
```

---

## 12. Boundary / exception behavior

| Case | Expected |
| --- | --- |
| Import conflict / marker not bumped | Import may no-op or fail honestly；工程师 bump marker 后重试；不静默留下旧浅层内容当成功 |
| Publish gate failure | Script fails；模板不得被宣称 PUBLISHED；无 generate 证据 |
| Generate size/marker miss | `generate-all` / contract tests fail；本叶不 Done |
| Accidental Wave B content added | Review reject；移出本叶或改排队 expand 叶 |
| Parallel docker-deploy | Forbidden；必须排队 |
| Unauthorized generate | Fail-closed（既有模型） |

---

## 13. Observable evidence

| Evidence | Path / signal |
| --- | --- |
| Publish summary | `.tmp/evidence/all-demos-publish-summary.json` |
| Credentials | `.tmp/credentials/<externalId>.json` |
| Generated DOCX | `.tmp/generated_<externalId>.docx` |
| Generate manifest | `.tmp/evidence/generated-docx-manifest.json` |
| Audit records | `.tmp/evidence/audit-records/<externalId>.json` |
| Contract tests | `DemoPublishOrchestrationContractTest`, `DemoGenerateOrchestrationContractTest`（既有 BDD-DEMO-TYP-011…013 对齐） |
| Master regen | `*MasterDocxAssetGeneratorTest` green |
| Human spot-check | Reviewer notes against §8 Q1–Q8（可复用 P23 typography checklist 的结构抽检，但本叶焦点是**文案真实性**） |

---

## 14. Content-quality findings（must rewrite — survey 2026-07-20）

| Finding | Severity | Action in Wave A |
| --- | --- | --- |
| `deploy/demo-fol/lma-clause-library.ps1` emits padding paragraphs: “For the executive demonstration dataset…” and “will be expanded in the final documentation set…” | **Critical** | Replace generator templates with real LMA-style schedule operative prose；regen SQL |
| `deploy/demo-fol/sql/001-fol-standard-clauses.sql` (~945KB) repeats the same padding across Schedules 1–6+ | **Critical** | Regenerate / rewrite module bodies；eliminate meta-narrative |
| Non-FOL package SQL currently lacks those exact padding phrases, but user judges overall demo catalog still **shallow after deep remediations** | **High** | Uplift bindings + SQL + executive fixtures to full bank-letter quality bar (§8) across all eight packages + full-flow |
| Master DOCX assets are small anchor shells (≈4–6KB class historically) — content quality lives in bindings/SQL/generators | **Info** | Keep shells regenerable；do not mistake shell size for letter quality |
| `demo-runtime-generate-manifest.json` already forbids LOREM/PLACEHOLDER/TODO | **Reuse** | Extend enforcement mentally to meta-padding phrases in Q8 for this leaf |
| TM #1–#8 historically Done for scaffolds/typography — **does not** satisfy this content-realism ask | **Info** | Treat Wave A as new content SoT leaf；do not claim prior Done covers it |

---

## 15. Traceability

| Source | Link |
| --- | --- |
| User intent (authoritative) | 清理测试 demo → 倒入 100% 仿真银行信函；场景全面且真实（Wave A = 既有包；Wave B = 新族） |
| PRD demo table | [PRD §6.7](../product/PRD.md) 八类信函 |
| P22 behavior | [demo-expansion-behavior-spec.md](../requirements/demo-expansion-behavior-spec.md) |
| P23 typography | [demo-typography-layout-behavior-spec.md](../requirements/demo-typography-layout-behavior-spec.md)（版式基线；本叶不 reopen P23） |
| Ops orchestration | [deploy/demo-shared/README.md](../../deploy/demo-shared/README.md) |
| Runtime manifest | `deploy/demo-shared/demo-runtime-generate-manifest.json` |
| Requirements plan pointer | [requirements-plan.md](../requirements/requirements-plan.md) demo / P23 段 |
| Task / slice | `demo-refresh-wave-a` / `bank-letter-demo-refresh` |
| Next leaf | `bank-letter-demo-expand`（Wave B） |

---

## 16. BDD readiness declaration

| Field | Value |
| --- | --- |
| **bdd_readiness** | **`ready`** |
| **open_questions** | `[]` — Wave B 延后已在 §9 / BDD-DEMO-REFRESH-013 写明，不构成产品冲突 |
| **owning_doc** | `docs/behavior/bank-letter-demo-refresh.md` |
| **Handoff** | `plan-orchestrator` → implementers（content rewrite + ops evidence）in worktree `D:/working/DGE-bank-letter-demo-refresh` |
| **frontend_ui_in_scope** | `false`（E2E/UIUX product journeys **N/A** unless a later change adds UI；generate evidence is script/API） |

---

## 17. Scenario ID index

1. `BDD-DEMO-REFRESH-001` — Ops-safe cleanup  
2. `BDD-DEMO-REFRESH-002` — Import-all overwrite  
3. `BDD-DEMO-REFRESH-003` — Publish-all + credentials  
4. `BDD-DEMO-REFRESH-004` — Generate-all DOCX evidence  
5. `BDD-DEMO-REFRESH-005` — FOL LMA-grade schedules  
6. `BDD-DEMO-REFRESH-006` — CORP quality bar  
7. `BDD-DEMO-REFRESH-007` — RETAIL + full-flow quality bar  
8. `BDD-DEMO-REFRESH-008` — TRADE + WEALTH quality bar  
9. `BDD-DEMO-REFRESH-009` — Variables/bindings/SQL/fixtures alignment  
10. `BDD-DEMO-REFRESH-010` — MasterDocx regenerable  
11. `BDD-DEMO-REFRESH-011` — Forbidden / meta-padding gate  
12. `BDD-DEMO-REFRESH-012` — Bilingual / English-first  
13. `BDD-DEMO-REFRESH-013` — Wave B deferral  
14. `BDD-DEMO-REFRESH-014` — Governance vetoes  
