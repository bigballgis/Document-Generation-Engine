# Business-Friendly Terminology Guide (English-first)

**Status:** Confirmed design (user, 2026-06-29) | **Implementation:** **Done** — P21 phase complete (2026-06-30); P21-X01 L1 sweep + **P21-X02** governance/docs wrap-up landed
**Owner phase:** [P21 — Role-journey frontend redesign & business-friendly terminology](../plan/detail/P21-role-journey-frontend-redesign.md) (**Done** 2026-06-30)
**Primary persona source:** user confirmation 2026-06-29 (two rounds).

### SYS-NORM Confirmed intent (2026-07-21) — Wave 5 Done; Wave 6 D1 docs-first

> Product direction locked by [system-normalization-program.md](../behavior/system-normalization-program.md) §2.8 / §2.5.
> **Do not claim L1 sweep Done** until Wave 8 `sys-norm-demo-seed-terms`.
> Wave 5 six-role runtime **Done** (`febb95b3`). Wave 6 D1 brand/entity retirement = TM **#150**
> (BDD **ready**; docs-first; impl pending — **not** program Done).

| Topic | Confirmed | Pending | Wave |
| --- | --- | --- | --- |
| L1 English primary object label | **Letterhead** (purge user-facing mixed “Master” on L1 primary surfaces) | — | Intent Wave 0; sweep **Wave 8** |
| L1 Chinese primary object label | **母版** | — | Intent Wave 0; sweep **Wave 8** |
| API / L3 identifiers | May keep `masterId`, `MasterDocument`, routes | — | Unchanged |
| Role `DOCUMENT_AUTHOR` | Role **ID** locked ([ADR-0070](../adr/authorization-security/0070-role-compression-six-roles.md); BDD [sys-norm-roles.md](../behavior/sys-norm-roles.md) ROLE-013) | EN/ZH **display label** finalizable — interim FE copy OK | Labels: Wave 5 interim / Wave 8 finalize |
| Role merge (catalog) | Six-role compression locked (ADR-0070); runtime catalog **Done** (`febb95b3`) | — | **Wave 5 Done** |
| Document brands / Legal entities (L1) | **Not required product surfaces** ([ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)); do **not** use as nav/module titles | Legacy L3 codes may linger until Wave 6 durable delete | **Wave 6** `sys-norm-d1-brands` |
| Logo / seal / letterhead legal name | Governed via **Letterhead** / **母版** (master flows) — not “Document brand” MDM | — | Wave 6+ |
| Shell brand themes | `REDBC` / `GREENBC` remain UI-only chrome labels — not document brand MDM | — | Unchanged |

§4.5 below remains the P21 canonical glossary baseline; SYS-NORM Wave 8 reconciles residual “Master” mix on L1.

> This guide is the **single source of truth (SSOT)** for user-facing label wording on L1
> primary surfaces. It governs message **values** only; it never changes stable i18n keys, API
> paths, enum codes, or audit field names. English is the baseline; zh-CN aligns to business
> semantics, not literal IT translation. See the i18n english-first constitution
> (`.cursor/rules/i18n-english-first-constitution.mdc`) and
> [i18n skill](../../.cursor/skills/i18n-english-first/SKILL.md).

## 1. Primary persona

| Dimension | Definition |
| --- | --- |
| Identity | Foreign-bank front/middle-office business & operations staff (template orchestration, compliance approval, channel/product operations, team leads) |
| IT literacy | Low–medium; comfortable with Word / email / approval flows; **not** with API / policy / lifecycle / anchor / semver |
| Work context | "I need to finish reviewing this letter template and put it live", "which templates are waiting on my approval", "can this template be called by the channel system" |
| Non-target users | Backend devs, DevOps, API integrators — their technical detail belongs in L2 help / tooltip / contract pages, not in primary navigation or primary button labels |
| Language | English baseline (en-first); zh-CN uses the same business plain-language |

Design implications:

- Lead with **what to do**, then **on which object** (action first, object second).
- States use **business stage names**, not enum codes.
- Navigation groups use **business functions**, not technical module names.
- Every behavior entry / journey step / empty / error state explains the next step in one line.

## 2. Three-layer copy model

| Layer | Where | Rule |
| --- | --- | --- |
| **L1 primary** | Navigation, page titles, buttons, task cards, journey steps, status chips | Business language only. **Forbidden as primary labels:** `policy`, `credential`, `lifecycle`, `gate`, `semver`, `anchor`, `escalation`, `render profile`, `orchestrate`, `entitlement`, `console`, `governance` |
| **L2 form / table** | Form field labels, table column headers, inline help | Business name + optional `(?)` tooltip explaining the technical meaning |
| **L3 contract / audit / developer** | Caller contract page, audit field names, API docs (GLOBAL/GROUP or read-only) | Precise technical terms allowed (API Policy, Render Profile, semver, anchor) |

## 3. Phrasing rules

| Surface | Pattern | Example (en) |
| --- | --- | --- |
| Nav / menu | noun phrase | "API management", "Users & permissions" |
| Task / to-do | verb + object | "Templates waiting on my approval" |
| Button | verb-first | "Submit for testing", "Confirm go-live" (not "Submit for test" / "Publish") |
| Disabled / waiting state | reason + who is waiting | "Awaiting team-lead go-live" |
| Empty state | where it comes from + what you do | "Items submitted for your testing appear here. Open one to record a result." |

## 4. Terminology mapping (SSOT table)

Statuses, queues, and module names map to business language on L1. Extend this table as P21
sub-phases land; keep the i18n key column populated when a sweep touches a key.

### 4.1 Navigation & modules

| Current IT label (en) | Business label (en) | Business label (zh-CN) | i18n key (stable) | Note |
| --- | --- | --- | --- | --- |
| API policy / API policies | API management | API 管理 | `nav.items.apiPolicies` | mental model is "manage integration & calls" |
| API policy management | Manage API access | API 接入管理 | `apiPolicy.landing.title` | shorter page title |
| API access (nav group) | External services | 对外服务 | `nav.groups.apiAccess` | outward business capability |
| Package hub API tab | External access | 对外接入 | `templates.policy.title` / package hub tab | **Historical** hub-tab label (2026-07-03). **SYS-NORM Wave 2 Confirmed:** migrate off hub tab to package API **settings** route under External services ([sys-norm-hub-ia.md](../behavior/sys-norm-hub-ia.md)); L1 wording for settings shell may keep “API settings” / 接入设置 — Wave 3 completes settings home |
| Invocation records | Call history | 调用记录 | `apiPolicy.invocation.*` (TBD keys at implement) | caller backup + audit complement; not compliance audit console |
| API credentials | Access keys / Connection accounts | 接入账号 | (credential surfaces) | avoid "credential" |
| Access & identity | Users & permissions | 用户与权限 | `nav.groups.entitlement` | "entitlement" too IT |
| Identity administration | User management | 用户管理 | `nav.routes.identityAdministration` | |
| Master documents | Letterhead templates / Document masters | 母版文档 | `nav.items.masters` | bank-natural; **never** use 主文档 on L1 |
| Master (object) | Letterhead | 母版 | `masters.*`, dashboard stats | short form for the DOCX asset |
| Master package | Letterhead package | 母版包 | `packageCatalog.master`, `masters.hub` | catalog row / package hub |
| Master name | Letterhead name | 母版名称 | `masters.list.columns.name` | form/table L2 |
| Document author (role) | Document author (interim) | 文档作者（interim） | `roles.DOCUMENT_AUTHOR` | Role ID locked; L1 final Pending (P-Q1). Retired: `roles.MASTER_DESIGNER` / `TEMPLATE_AUTHOR` |
| Master review / approval | Letterhead review | 母版审核 | `masters.workflow.*`, `nav.behaviorItems.masterReview` | not 主文档审批 |
| Master ID (technical) | Master ID | 母版 ID | `templates.detail.masterId` | L2 field label only |
| Layout placeholder (was anchor) | Layout placeholder | 版式占位符 | `templates.authoring.*` | never expose "anchor" on L1 |
| Template authoring | Template design | 模板设计 | `home.templateAuthoring.title` | authoring/orchestrate too IT |
| Content modules | Standard clauses | 标准条款 | `nav.items.contentModules` | compliance/product familiar |
| Audit log / Audit console | Activity log / Audit trail | 操作记录 | `nav.items.audit`, `home.audit.title` | "console" too IT; audit role may keep "audit" |
| Governance overview | My overview | 工作概览 | `home.dashboard.title` | "governance" too abstract |

### 4.2 Workflow stages & actions

| Current IT label (en) | Business label (en) | Business label (zh-CN) | Note |
| --- | --- | --- | --- |
| Lifecycle (tab/panel) | Approval progress / Workflow status | 流转进度 | "lifecycle" key only, not label |
| DRAFT | Drafting | 编写中 | business stage name |
| TESTING | In testing | 测试中 | |
| APPROVAL (PENDING_SUBMIT) | Ready to submit for approval | 待提交审批 | dual sub-state |
| APPROVAL (PENDING_DECISION) | Awaiting approval | 待审批 | dual sub-state |
| PENDING_RELEASE | Awaiting go-live | 待上线 | |
| PUBLISHED | Live | 已上线 | |
| STOPPED | Paused | 已停用 | |
| DEPRECATED | Retired | 已废弃 | |
| Publish / publish gate | Confirm go-live / Pre-release checks | 确认上线 / 上线前检查 | "gate" internal only |
| Blocking impacts | Issues to fix before go-live | 上线前需修复的问题 | |
| Semver / version bump | Release version number | 发布版本号 | hide semver concept |
| Submit for test | Submit for testing | 提交测试 | verb-first |
| Submit for approval | Submit for approval | 提交审批 | |
| Stop / Restore / Deprecate | Pause / Resume / Retire | 停用 / 恢复 / 废弃 | |

### 4.3 Collaboration queues & behavior entries

| Queue / trigger code | Behavior entry label (en) | Behavior entry label (zh-CN) | Visible to |
| --- | --- | --- | --- |
| TEST | Waiting on my testing | 待我测试 | TEMPLATE_TESTER, GROUP, GLOBAL |
| APPROVAL | Waiting on my approval | 待我审批 | GROUP, GLOBAL (ex-`TEMPLATE_APPROVER` absorbed) |
| REMEDIATION | Waiting on my fixes | 待我修改 | DOCUMENT_AUTHOR, GROUP, GLOBAL |
| PENDING_RELEASE | Waiting to confirm go-live | 待确认上线 | GROUP, GLOBAL |
| ESCALATION | Overdue to follow up | 超时待跟进 | GROUP, GLOBAL |
| (master review) | Letterheads to review | 待审核母版 | GROUP, GLOBAL (+ DOCUMENT_AUTHOR for own rework) |
| Collaboration timeout config | Reminder timing | 催办时限设置 | GROUP, GLOBAL |
| Escalation (concept) | Overdue reminder | 超时提醒 | notification, not system escalation |
| Exception intervention | Confirm on behalf (with audit trail) | 代为确认（留痕） | GROUP, GLOBAL |

### 4.4 Authoring & rendering

| Current IT label (en) | Business label (en) | Business label (zh-CN) | Note |
| --- | --- | --- | --- |
| Anchor catalog / anchor integrity | Layout placeholders / Placeholder check | 版式占位符 | "anchor" help text only |
| Render profile / fidelity | Output format check | 输出效果检查 | expandable L2 help |
| Orchestrate | Configure / Set up | 配置 | |
| Global governance | Bank-wide administration | 全行管理 | `nav.routes.globalGovernance` | P21-X01 |
| Group governance | Group administration | 分组管理 | `nav.routes.groupGovernance` | P21-X01 |
| Lifecycle actions | Workflow actions | 工作流操作 | `templates.lifecycle.title` | P21-X01 |
| Publish gate checklist | Pre-release checks | 上线前检查 | `templates.publishGate.title` | P21-X01 |
| Submit-for-approval summary | Review before submit for approval | 提交审批前确认 | `templates.submitApprovalSummary.title` | P12-AUD-B10 |
| Submit gate blocked (API L1) | Submit blocked until checks pass | 检查通过前无法提交审批 | `api.error.template.submitForApprovalGateBlocked` | P12-AUD-B10 |
| Post-publish governance | Post-publish controls | 上线后管控 | `templates.governance.title` | P21-X01 |
| Anchor bindings / integrity | Layout placeholder bindings / check | 版式占位符绑定 / 检查 | `templates.authoring.bindingsTitle`, `templates.publishGate.checkCodes.ANCHOR_INTEGRITY` | P21-X01 |
| Content modules (list) | Standard clauses | 标准条款 | `contentModules.list.title`, `packageCatalog.contentModule.noticeTitle` | P21-X01 |
| Lifecycle impact preview | Impact preview | 影响预览 | `contentModules.lifecycle.impactTitle` | P21-X01 |
| Semantic version (L1 label) | Version number | 版本号 | `contentModules.version.semanticVersion` | hide semver concept |
| Audit lifecycle tab export | Template workflow export | 模板工作流导出 | `audit.export.lifecycleSuccess` | P21-X01 |

### 4.5 Master / letterhead / 母版（canonical glossary）

**Problem:** zh-CN had mixed **主文档** and **母版** on L1 surfaces, causing confusion. English had mixed
**master document**, **letterhead**, and **master** for the same domain object (`MasterDocument` in code).

**Rule:** User-facing L1 copy uses one business term per language. Internal identifiers (`master`,
`MasterDocument`, `/masters`, `masterId`, audit fields) stay unchanged.

| Layer | English (L1) | 中文（L1） | When to use |
| --- | --- | --- | --- |
| Module / nav / page title | **Letterhead templates** | **母版文档** | Side nav, list page title, quick links (`nav.items.masters`, `masters.list.title`) |
| Short object name | **Letterhead** | **母版** | Sentences, buttons, task cards, errors (`打开母版`, `Approve letterhead`) |
| Package container | **Letterhead package** | **母版包** | Catalog rows, hub pages (`packageCatalog.master.*`, `masters.hub.*`) |
| Revision snapshot | **Revision line** | **修订线** | Immutable upload/replace history (`MasterRevisionLine`) |
| Layout slot in DOCX | **Layout placeholder** | **版式占位符** | Bindings, integrity checks — not **锚点** on L1 |
| Role | **Document author** (interim) | **文档作者**（interim） | `DOCUMENT_AUTHOR` — L1 final Pending (P-Q1); historical letterhead-designer label retired from assignable catalog |
| Behavior queue | **Letterheads to review** | **待审核母版** | Dashboard / nav behavior entry |

**Forbidden on L1 (zh-CN):** **主文档** — do not use in any user-facing bundle value.

**Forbidden on L1 (en):** **master document** / **master documents** as primary labels — prefer
**letterhead** (object) or **letterhead templates** (module).

**i18n keys touched by this glossary (representative):**

| Key area | en baseline | zh-CN |
| --- | --- | --- |
| `masters.list.title` | Letterhead templates | 母版文档 |
| `masters.upload.open` | New letterhead package | 新建母版包 |
| `dashboard.quickLinks.masters` | Letterhead templates | 母版文档 |
| `dashboard.stats.masterPendingReview.title` | Letterheads awaiting review | 待审核母版 |
| `nav.behaviorItems.masterReview` | Letterheads to review | 待审核母版 |
| `api.error.master.*` (L1 message) | …this letterhead… | …此母版… |
| `roles.DOCUMENT_AUTHOR` | Document author (interim) | 文档作者（interim） |

**Code / docs (L3 — unchanged):** `MasterDocument`, `master_document`, `GET /masters`, Flyway table
names, audit event codes, OpenAPI schema `MasterDocumentResponse`.

### 4.6 DocumentBrand / LegalEntity — retired product terms (ADR-0071 / Wave 6)

**Confirmed:** Document brands and Legal entities are **not** required L1 nav modules, catalog
pages, or primary business labels going forward. Operators manage letterhead / logo / seal via
**Letterhead** / **母版** (see §4.5). **Legal holds** remain a Security surface (keep the term).

| Do not use on L1 (going forward) | Prefer | Notes |
| --- | --- | --- |
| Document brands / 文档品牌（as catalog/nav） | Letterhead / 母版 | Logo/seal SoT = Letterhead (master) |
| Legal entities / 法人实体（as catalog/nav） | — (surface retired) | Opaque `legalEntityCode` may remain L3 API context only |
| Brand allow-list editor copy | Remove | Wave 6 FE hard retire |

**L3 / historical:** `DocumentBrand`, `LegalEntity`, OpenAPI retired paths, ADR-0065 Decision body
remain for audit; product direction follows [ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)
and [sys-norm-d1-brands.md](../behavior/sys-norm-d1-brands.md). Full L1 English/母版 residual sweep
is still **Wave 8** (do not claim terminology program Done here).

## 5. Acceptance (per P21 sub-phase)

- **5-second readability:** a random 10 L1 labels — a non-IT person states the meaning within 5
  seconds (UIUX checklist + E2E copy assertions).
- **Grep audit:** primary-journey L1 surfaces contain no `policy` / `credential` (as noun menu) /
  `lifecycle` / `semver` / `gate` / `anchor integrity` (contract/audit pages excepted).
- **Key stability:** i18n keys, API paths, enum codes, audit field names unchanged (diff review).
- **Bilingual parity:** zh-CN value conveys the same business meaning as the en baseline (no
  literal IT translation).

## 6. Maintenance

- This file is the SSOT. When a P21 sub-phase changes user-facing copy, update the relevant
  mapping row in the same change set, with the i18n key recorded.
- Conflicts resolve per [docs/README.md](../README.md) source-of-truth order; surface conflicts
  rather than silently choosing.

## 7. Related documents

- [P21 detailed plan](../plan/detail/P21-role-journey-frontend-redesign.md)
- [Catalog navigation UX](./catalog-navigation-ux.md) — hybrid IA + business-terminology navigation contract
- [Permission matrix §13.3](../security/permission-matrix.md) — behavior-typed entry visibility
- [ADR: behavior-typed IA + business terminology](../adr/decisions/2026-06-29-behavior-typed-ia-business-terminology.md)
- [Frontend OA design system](../../.cursor/skills/frontend-oa-design/SKILL.md)
- i18n bundles: `frontend/src/i18n/locales/en.ts`, `frontend/src/i18n/locales/zh-CN.ts`
