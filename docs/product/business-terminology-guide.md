# Business-Friendly Terminology Guide (English-first)

**Status:** Confirmed design (user, 2026-06-29) | **Implementation:** Not Started (delivered under **P21**)
**Owner phase:** [P21 — Role-journey frontend redesign & business-friendly terminology](../plan/detail/P21-role-journey-frontend-redesign.md)
**Primary persona source:** user confirmation 2026-06-29 (two rounds).

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
| API credentials | Access keys / Connection accounts | 接入账号 | (credential surfaces) | avoid "credential" |
| Access & identity | Users & permissions | 用户与权限 | `nav.groups.entitlement` | "entitlement" too IT |
| Identity administration | User management | 用户管理 | `nav.routes.identityAdministration` | |
| Master documents | Letterhead templates / Document masters | 母版文档 | `nav.items.masters` | bank-natural |
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
| APPROVAL | Waiting on my approval | 待我审批 | TEMPLATE_APPROVER, GROUP, GLOBAL |
| REMEDIATION | Waiting on my fixes | 待我修改 | TEMPLATE_AUTHOR, GROUP, GLOBAL |
| PENDING_RELEASE | Waiting to confirm go-live | 待确认上线 | GROUP, GLOBAL |
| ESCALATION | Overdue to follow up | 超时待跟进 | GROUP, GLOBAL |
| (master review) | Masters to review | 待审核母版 | GROUP, GLOBAL (+ MASTER_DESIGNER for own rework) |
| Collaboration timeout config | Reminder timing | 催办时限设置 | GROUP, GLOBAL |
| Escalation (concept) | Overdue reminder | 超时提醒 | notification, not system escalation |
| Exception intervention | Confirm on behalf (with audit trail) | 代为确认（留痕） | GROUP, GLOBAL |

### 4.4 Authoring & rendering

| Current IT label (en) | Business label (en) | Business label (zh-CN) | Note |
| --- | --- | --- | --- |
| Anchor catalog / anchor integrity | Layout placeholders / Placeholder check | 版式占位符 | "anchor" help text only |
| Render profile / fidelity | Output format check | 输出效果检查 | expandable L2 help |
| Orchestrate | Configure / Set up | 配置 | |
| Callable / runtime callers | Available to channel systems | 可供渠道系统调用 | |

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
