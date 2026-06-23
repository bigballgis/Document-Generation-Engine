# User-Interaction & Upgradeability Optimization Plan

**Created:** 2026-06-23
**Lens:** End-user usage & interaction completeness + system upgradeability/extensibility.
**Relationship to other plans:** Complements `optimization-plan.md` (which covers
technical debt: gates, coverage, backend architecture). This plan focuses on what a
real human operator (per role) can and cannot do through the UI, and on how cheaply
the system can grow (new roles, locales, brands, policy domains, environments,
content modules).
**Status model:** `Not Started` | `In Progress` | `Blocked` | `Done`
**Rule:** Behavior-changing tasks land with a behavior spec, failing-test-first loop,
green gates, and synced owning docs (per the delivery + doc-sync constitutions).

> Evidence-backed by read-only analysis on 2026-06-23. Source order on conflict:
> latest user confirmation > `requirements-plan.md` > `PRD.md` > `domain-model.md` >
> `permission-matrix.md` > ADRs. Every cited gap is a path/line reference; verify
> before acting. Items marked **(needs decision)** require maintainer confirmation
> before they become confirmed requirements.

---

## 0. Headline findings (user-interaction)

| # | Finding | Severity | Evidence |
| --- | --- | --- | --- |
| U1 | **Frontend role gating contradicts backend authorization** → users see buttons that 403. `TEMPLATE_AUTHOR` sees Upload master, Test decision, Approval decision, Publish; backend denies all four. `MASTER_DESIGNER` is authorized by backend but has no UI access. | **Critical** | `frontend/src/auth/roles.ts` L9–19, L84–90 vs `backend/.../GroupAccessService.java` L28–53 |
| U2 | **Dedicated operator roles have no journey**: `TEMPLATE_TESTER`, `TEMPLATE_APPROVER`, `AUDIT_ADMIN`, `MASTER_DESIGNER` have backend authorization and/or doc-confirmed duties but no landing page, nav, or task workbench. No seed users for several. | **High** | `RouteVisibilityService.java`; `V2__management_users.sql`; `permission-matrix.md` §4–6 |
| U3 | **Credential lifecycle is half-built in UI**: backend exposes `rotate` + `revoke`, but `frontend/src/api/apiPolicy.ts` only wraps `createCredential`/`listCredentials`. Created secret is returned but the one-time reveal flow is incomplete. | **High** | `apiPolicy.ts` L40–45 vs `ApiManagementController` rotate/revoke; `types/template.ts` secret field |
| U4 | **No template creation flow**: `createTemplate` API client exists but is wired to nothing; no "create from approved master" wizard despite the PRD step-wizard journey. | **High** | `frontend/src/api/templates.ts` `createTemplate` unused; `TemplateListView.vue` has no create button |
| U5 | **No global 401/403 handling**: `http.ts` has request-only interceptor; session expiry never auto-redirects, `api.error.authentication.sessionExpired` key is unused. | **High** | `frontend/src/api/http.ts`; `i18n/locales/en.ts` `sessionExpired` |
| U6 | **No confirmation dialogs anywhere**: destructive/irreversible actions (delete test data set, reject master/template, publish, future revoke) have no confirm step; matrix requires reason + secondary confirmation for admin exception interventions. | **High** | zero `ElMessageBox.confirm` usages; `permission-matrix.md` §3 |
| U7 | **Role-home workspaces are placeholders that leak internal identifiers** to end users (raw `routeKey`, raw URL path), plus "expanded in later phases" copy. | **Medium** | `RoleHomeView.vue` L44, L60–63; `en.ts` `home.placeholder` |
| U8 | **Lifecycle workbench is read-only past publish and ambiguous mid-flow**: no Stop/Deprecate/Restore actions, no version deactivate/restore; in `APPROVAL` status both "Submit for approval" and "Approve/Reject" render together; publish-gate "API policy" item is static text, not a live check. | **Medium** | `TemplateDetailView.vue` lifecycle section; `TemplateLifecycleStatus` enum merges two stages |
| U9 | **Read-only authoring surfaces**: variables/bindings tables are display-only; `TemplateRuleConfigurator` edits local state and validates but never persists; no edit of template name/description/master binding. | **Medium** | `TemplateAuthoringPanel.vue`, `TemplateRuleConfigurator.vue` (no save call) |
| U10 | **Navigation/links dead-end**: `masterId` in template summary not clickable; impact-analysis template IDs are plain text; audit has no pagination; lifecycle audit has no export. | **Low–Med** | `TemplateDetailView.vue` summary; `MasterImpactPanel.vue`; `AuditConsoleView.vue` |

## 0b. Headline findings (upgradeability / extensibility)

| # | Finding | Severity | Evidence |
| --- | --- | --- | --- |
| X1 | **i18n is not multi-locale-ready in practice**: constitution mandates English-first + additive locales, but only `en` is registered and there is no locale switcher, no `lang` wiring, and several hardcoded strings. Adding a language today requires code edits beyond a new bundle. | **High** | `i18n/index.ts` registers only `en`; hardcoded strings in `LoginView`, `ManagementShell`, `theme/tokens.ts` |
| X2 | **Role/route authorization is dual-sourced** (backend `RouteVisibilityService` enum + frontend `roles.ts` manual override for master/template). Adding a role or route means editing both sides with no single source of truth → drift (root cause of U1). | **High** | `RouteVisibilityService.java` vs `routeKeys.ts` + `roles.ts` |
| X2b | **Role model is split**: `ManagementRole` enum has 4 values; permission matrix defines 8 roles; codes appear ad-hoc as strings in `GroupAccessService`. Extending roles is error-prone. | **Medium** | `ManagementRole.java`; `GroupAccessService.java` string literals |
| X3 | **API policy is monolithic** (single `PUT` upsert) vs the confirmed design of per-domain save + impact preview + `policyVersion`. Adding a new policy domain forces whole-form changes and loses per-domain audit/versioning. | **Medium** | `ApiManagementController` single PUT; `permission-matrix.md` §7 config domains |
| X4 | **Brand theming is code-bound**: REDBC/GREENBC presets live in `theme/tokens.ts`; adding a third brand requires code, not config. | **Low–Med** | `frontend/src/theme/tokens.ts` |
| X5 | **Environment is hardcoded** in the caller-contract panel (`environment = "dev"`); no environment selector despite `/api/{environment}/v1` design and export/import-across-environments requirement. | **Low–Med** | `TemplateDetailView.vue` contract panel |
| X6 | **Confirmed-but-unimplemented domains** block future growth as designed: clause/content modules (own lifecycle), collaboration to-dos + timeout escalation, template export/import, master delete. These are confirmed requirements with zero backend/UI. | **High (scope)** | `permission-matrix.md` §5.1, §5; `requirements-plan.md`; no controllers exist |

---

## 1. Themes & sequencing

| Theme | Title | Drivers | Suggested wave |
| --- | --- | --- | --- |
| UX-A | Role-gating truth & alignment | U1, U2, X2, X2b | Wave A (first) |
| UX-B | Close half-built interactions | U3, U4, U5, U6 | Wave A |
| UX-C | Lifecycle & authoring completeness | U8, U9, U10 | Wave B |
| UX-D | Role workbenches (tester/approver/audit/master-designer) | U2, U7 | Wave B |
| UX-E | Polish: placeholders, debug leakage, states, links | U7, U10 | Wave B |
| UX-F | Upgradeability foundations | X1, X3, X4, X5 | Wave C |
| UX-G | Confirmed large domains (scope/backlog) | X6 | Wave C+ (scoped separately) |

Recommended order: **Wave A** removes "buttons that lie" (U1) and finishes the
interactions that are already 80% built (U3–U6) — highest user-trust ROI. **Wave B**
gives every role a real journey and completes lifecycle/authoring. **Wave C** makes
the system cheap to extend (locales, policy domains, brands, environments) and frames
the big confirmed domains as their own delivery phases.

---

## 2. Task backlog

Each task: `ID | Pri | Title | Evidence | Acceptance | Status`. Priority **H/M/L**.

### UX-A Role-gating truth & alignment

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| UXA1 | H | Make frontend role gates match backend authorization | `roles.ts` L84–90 vs `GroupAccessService` L39–53 | Separate gates for `canDecideTests` (TESTER+admin), `canDecideApprovals` (APPROVER+admin), `canPublish` (admin only), `canManageMasters` (admin only); buttons only render when the backend would allow the action; no 403 from a visible control | Done |
| UXA2 | H | Single source of truth for role→capability mapping | dual-source X2/X2b | Backend emits per-session capability flags (or the existing `visibleRoutes` is extended with action capabilities); frontend consumes them instead of re-deriving from role strings; add contract test | Done |
| UXA3 | M | Reconcile role catalog to the confirmed 7 roles | `ManagementRole.java`, matrix §1; decision §4.4 | One authoritative role enum/list shared in claims covering `GLOBAL_ADMIN`/`GROUP_ADMIN`/`MASTER_DESIGNER`/`TEMPLATE_AUTHOR`/`TEMPLATE_TESTER`/`TEMPLATE_APPROVER`/`AUDIT_ADMIN`; seed user exists for each role; permission-matrix + ADR role catalog updated | Done |
| UXA4 | M | Remove `API_ADMIN`; fold API policy into admin roles (Confirmed §4.1) | `RouteVisibilityService` has no API_ADMIN routes; `roles.ts` L55–65 + `GroupAccessService` L55–59 include API_ADMIN | `API_ADMIN` removed from frontend `roles.ts`, backend `canManageApiPolicy`, claims, and any seed/config; API policy editable by GLOBAL/GROUP admin only; no orphaned API_ADMIN login path | Done |
| UXA5 | M | Hide unauthorized controls (Confirmed §4.2) | matrix §13 pending item resolved | Controls a role cannot use are not rendered (not disabled); direct-URL access still hits Forbidden; applied consistently across all views; tested | Done |

### UX-B Close half-built interactions

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| UXB1 | H | Credential rotate/revoke UI + one-time secret reveal | `apiPolicy.ts` lacks rotate/revoke; secret returned but not shown | API client wraps rotate+revoke; credential row actions with confirm; created/rotated secret shown once in a copyable, dismissible reveal with "won't be shown again" warning; revoked state reflected | Done |
| UXB2 | H | Template creation wizard from approved master | `createTemplate` unused; PRD step-wizard journey | "Create template" entry on TemplateList; wizard: pick approved master → name/description/group → create → land on detail; gated by `canAuthorTemplates`; tested | Done |
| UXB3 | H | Global 401/403 axios response interceptor | `http.ts` request-only; `sessionExpired` key unused | 401 → clear session + redirect to login with `sessionExpired` notice; 403 → forbidden handling; centralized envelope error parse reused by stores | Done |
| UXB4 | H | Confirmation dialogs for destructive/irreversible actions | no `ElMessageBox.confirm` usages | Reusable confirm composable; applied to delete test data set, reject master/template, publish, revoke; admin exception interventions capture a reason (matrix §3) | Done |
| UXB5 | M | Persist authoring edits (variables/bindings/rules) | `TemplateRuleConfigurator` validates but never saves; tables read-only | Edits call existing PUT endpoints (variables/bindings) + a rules-save path; optimistic/refetch update; success/error feedback; tested | Done |

### UX-C Lifecycle & authoring completeness

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| UXC1 | M | Live publish-gate checklist | publish gate "API policy" is static text | Each gate item reflects real readiness (policy configured, tests passed, bindings valid); publish disabled until satisfied; reasons shown | Done |
| UXC2 | M | Disambiguate `APPROVAL` stage actions | both submit + decide render together | UI distinguishes "awaiting approval submission" vs "awaiting approval decision" (split status or stage flag); only the valid action shows per role | Done |
| UXC3 | M | Stop/Deprecate/Restore + version deactivate/restore (versioning + logical delete, §4.3) | matrix §5 confirms; no backend endpoints or UI | Backend lifecycle endpoints + UI actions with confirm + audit; STOPPED/DEPRECATED actionable; removal is logical delete only (no hard delete) **(needs backend work)** | Done |
| UXC4 | L | Master/template metadata edit via versioning (Confirmed §4.3) | only PATCH metadata exists; no re-upload; no delete | Metadata edit UI; content changes create new versions (no in-place re-upload); removal = logical delete; no hard-delete UI | Done |
| UXC5 | L | Cross-navigation links | `masterId` text not clickable; impact IDs plain | Template summary `masterId` links to master detail; impact-analysis template IDs link to template detail | Done |

### UX-D Role workbenches

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| UXD0 | H | Seed users + landing/visibleRoutes for all 7 roles (Confirmed §4.4) | no seed users for TESTER/APPROVER/MASTER_DESIGNER/AUDIT_ADMIN; `RouteVisibilityService` missing entries | Seed account per role; backend `defaultRoute` + `visibleRoutes` defined for each; login lands correctly; prerequisite for UXD1–UXD3 | Done |
| UXD1 | M | Tester workbench (TEMPLATE_TESTER) | role exists, no UI/landing | "My test queue" view: templates awaiting test, test-generate, pass/fail decision with comment; gated by `canDecideTests` | Done |
| UXD2 | M | Approver workbench (TEMPLATE_APPROVER) | role exists, no UI/landing | "My approval queue" view: templates awaiting approval, approve/reject with comment; gated by `canDecideApprovals` | Done |
| UXD3 | M | Master-designer journey (MASTER_DESIGNER) | backend authorizes, no nav | Landing + nav for `MASTER_DESIGNER`; access to master + template authoring per matrix §4–5 | Done |
| UXD4 | L | Replace governance role-home placeholders with real dashboards | `RoleHomeView` placeholders | Global/Group governance homes show actionable summaries (pending reviews, awaiting publish, recent audit) instead of placeholder cards | Done |

### UX-E Polish: placeholders, leakage, states, i18n strings

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| UXE1 | M | Remove internal-identifier leakage in Role-home | `RoleHomeView.vue` L44, L62 show routeKey/path | No raw route keys or URLs shown to users; replace with human labels | Done |
| UXE2 | M | i18n-ize remaining hardcoded strings | `LoginView` placeholder/aria, `ManagementShell` aria, export filename, raw status enums | All user-facing strings via message keys; status enums mapped to localized labels | Done |
| UXE3 | M | Standard empty/error/not-found states | detail views blank on load failure; no pagination | Shared not-found + load-error components for master/template detail; pagination on audit + lists | Done |
| UXE4 | L | Lifecycle audit export + parity | only management events export | Lifecycle tab gets export; consistent filenames via i18n | Done |

### UX-F Upgradeability foundations

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| UXF1 | H | Make i18n truly multi-locale-ready | only `en` registered; no switcher | Locale registry supports additive bundles + fallback to `en`; locale switcher in shell; `html lang` wired; date/number via i18n; adding a locale = drop-in bundle, no component edits | Done |
| UXF2 | M | Config-driven navigation from a single capability source | X2 | Nav + route guards derive from one backend-provided capability/route map; adding a route/role needs no frontend role-list edit | Done |
| UXF3 | M | Per-domain API policy save + impact preview + policyVersion | X3; matrix §7 | Policy edited/saved per domain (AD group, output, batch, encryption, default route) with impact preview and version; surfaces all fields currently submitted silently (outputFormats/modes/pdfEncryption) | Done (monolithic save + impact preview + full field UI; per-domain PUT → P17-T02) |
| UXF4 | L | Config-driven brand theming | X4 | Brands defined in config/data, not hardcoded tokens; adding a brand = config entry + assets | Done |
| UXF5 | L | Environment selector for contract/runtime views | X5; `environment="dev"` hardcoded | Environment chosen from allowed list (not hardcoded); used by caller-contract and future export/import | Done |

### UX-G Confirmed large domains — scheduled as the next phase (Wave D, §4.5)

Promoted from backlog into the next delivery phase; registered in `master-plan.md`.
Each is behavior-spec-first and gets its own slice plan under `docs/plan/detail/`.

| ID | Pri | Title | Evidence | Acceptance | Status |
| --- | --- | --- | --- | --- | --- |
| UXG1 | H | Clause / content module lifecycle | matrix §5.1; no module | Behavior spec + phase plan; backend module + UI for clause draft→approve→deprecate with group/share scope | Done (spec: P14-T01; implementation → P14) |
| UXG2 | M | Collaboration to-dos + timeout escalation | matrix §5; no API | Behavior spec + phase plan; to-do queue, timeout thresholds, escalation (no auto-decision) | Done (spec: P14-T02; implementation → P14) |
| UXG3 | M | Template export / import across environments | requirements §; no API | Behavior spec + phase plan; export approved template, import → restart from draft | Done (spec: P14-T03; implementation → P14) |

---

## 3. Done definition (per task)

A task is `Done` only when: behavior spec persisted in the owning doc (if
behavior-changing) → failing test first → smallest change to green → quality gates
green (`mvn -B -ntp -f backend/pom.xml verify` and/or `pnpm -C frontend lint &&
type-check && test && build`) → owning docs + this plan's status updated in the same
change set → post-task doc sync complete.

## 4. Confirmed decisions (2026-06-23, maintainer)

These are now confirmed and durable. Where they touch architecture or the role model
they must be reflected in the owning docs (permission-matrix §13 pending items, role
catalog, relevant ADRs) during each task's post-task doc sync — not silently.

1. **API_ADMIN → merged into admin roles.** `API_ADMIN` is **removed** as a distinct
   login role. API policy management is performed by `GLOBAL_ADMIN` / `GROUP_ADMIN`
   (group-scoped). Frontend `canManageApiPolicy` and backend `canManageApiPolicy`
   drop `API_ADMIN`. (Affects UXA1, UXA3, UXA4)
2. **Unauthorized controls → hidden.** Controls a role cannot use are **not rendered**
   (no disabled-with-tooltip). Forbidden pages remain for direct-URL access. (Affects UXA5)
3. **Master/template mutability → versioning + logical delete.** No hard delete and no
   in-place re-upload. Changes produce new versions; removal is logical delete only.
   (Affects UXC3, UXC4)
4. **Dedicated operator roles are first-class.** `TEMPLATE_TESTER`,
   `TEMPLATE_APPROVER`, `MASTER_DESIGNER` (and `AUDIT_ADMIN`) are real roles with real
   accounts and dedicated journeys/workbenches. Seed users must exist for each.
   (Confirms UXD1–UXD3; role catalog = 7 roles, API_ADMIN excluded)
5. **Large domains scheduled as the next phase.** Clause/content modules, collaboration
   to-dos + timeout escalation, and template export/import (UXG1–UXG3) are promoted
   from backlog into the **next delivery phase**, registered in `master-plan.md`.

### 4.1 Confirmed role catalog (7 roles)

`GLOBAL_ADMIN` · `GROUP_ADMIN` · `MASTER_DESIGNER` · `TEMPLATE_AUTHOR` ·
`TEMPLATE_TESTER` · `TEMPLATE_APPROVER` · `AUDIT_ADMIN`. (`API_ADMIN` removed.)

## 5. Active wave status

| Wave | Scope | Status |
| --- | --- | --- |
| Wave A | UX-A + UX-B | Done |
| Wave B | UX-C + UX-D + UX-E | Done (2026-06-23; gates: backend `mvn verify`, frontend lint/type-check/test/build) |
| Wave C | UX-F | Done (2026-06-23; UXF1/3/4/5 + P16-T08 + P17 impact-preview seam) |
| Wave D | UX-G (clause modules, collaboration to-dos, export/import) — spec in P14 | Spec Done (2026-06-23); implementation → P14 phase |

### 5.1 Wave A/B delivery evidence (2026-06-23)

Backend gates green: `mvn -B -ntp -f backend/pom.xml verify "-Dspring-boot.repackage.skip=true"` (134 tests, 1 skipped, 2026-06-23).

Frontend gates green: `pnpm -C frontend lint` / `type-check` / `test` (81 tests) / `build`.

| Module | Evidence |
| --- | --- |
| Backend role catalog | `ManagementRole` 7 roles; `V14__management_role_seeds.sql`; `TestManagementUserSeeder` |
| Backend capabilities | `ManagementCapabilitiesView`, `ManagementCapabilitiesService`, session `capabilities` field |
| Backend route visibility | `ManagementRoute` +4 routes; `RouteVisibilityService` default/visible per role; `API_ADMIN` removed from `canManageApiPolicy` |
| Session capabilities | `ManagementCapabilities` types, `useCapabilities` composable, `ManagementSessionView` wiring |
| Route gating | `canAccessRoute` uses `visibleRoutes` only; `ManagementShell` nav from session routes |
| Role helpers | `roles.ts` — `API_ADMIN` removed; capability-based helpers with role fallback |
| Workbenches | `route.tester-workbench` → `TesterWorkbenchView`; `route.approver-workbench` → `ApproverWorkbenchView` |
| Template authoring | `TemplateCreateDialog` + create button on `TemplateListView`; granular gates on `TemplateDetailView` |
| API credentials | `apiPolicy.rotateCredential` / `revokeCredential`; rotate/revoke UI with confirm |
| Session expiry | `http.ts` 401 interceptor → login redirect with `sessionExpired` |
| Confirmations | `useConfirmAction` composable applied to destructive actions |
| Role home | `RoleHomeView` actionable summary (replaces placeholder cards) |
| Authoring persist | `V15__template_version_rules.sql`; `PUT /templates/{id}/rules`; `TemplateAuthoringPanel` variable/binding CRUD; `TemplateRuleConfigurator` save/validate; `TemplatePlatformSliceTest#savesCompositionRulesAndReturnsThemOnTemplateDetail` |
| Publish gate | `TemplateDetailView` live binding-validation gate before publish |
| Cross-links | Template `masterId` → master detail; `MasterImpactPanel` template IDs → template detail |
| Template governance | `POST .../lifecycle/stop|restore|deprecate`; `TemplateCallabilitySupport`; runtime honors STOPPED/DEPRECATED/version STOPPED; `TemplateDetailView` governance panel |
| Shell a11y | `ManagementShell` nav `aria-label` i18n key |
| i18n | New keys in `frontend/src/i18n/locales/en.ts` (authoring, rules, publishGate, common) |

### 5.2 Wave C/D delivery evidence (2026-06-23)

Backend gates: `mvn -B -ntp -f backend/pom.xml verify "-Dspring-boot.repackage.skip=true"` — 161 tests.

Frontend gates: `pnpm -C frontend lint` / `type-check` / `test` (88) / `build`.

| Module | Evidence |
| --- | --- |
| P16-T08 logical delete | `TemplateDeleteService`; `DELETE /templates/{id}`; `deleteTemplates` capability; excluded from lists/runtime |
| UXF3 policy preview | `ApiPolicyImpactPreviewService`; `POST .../api/policy/impact-preview`; full policy form + preview dialog |
| UXF1 locale | `localeRegistry.ts`; `zh-CN` bundle; shell switcher; `html lang` |
| UXF4 brand | `config/brands.ts`; registry-driven tokens |
| UXF5 environment | `config/environments.ts`; contract panel selector |
| Wave D spec | P14 behavior specs confirmed; UXG1–3 → implementation deferred to P14 activation |
