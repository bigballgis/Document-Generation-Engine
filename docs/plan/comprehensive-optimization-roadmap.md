# Comprehensive System Optimization Roadmap

**Created:** 2026-06-23  
**Sources:** Repository-wide deep review (backend/runtime/docs), template & lifecycle
workflow review, frontend workflow & operation-experience review (OA design alignment).  
**Status model:** `Not Started` | `In Progress` | `Blocked` | `Done`  
**Relationship to other plans:**

| Document | Role |
| --- | --- |
| [optimization-plan.md](./optimization-plan.md) | Technical debt backlog (OPT-A…G); gate/coverage/architecture detail |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Prior UX waves (UX-A…G); historical Done claims — verify against this roadmap |
| [detail/P16–P20](./detail/) | Confirmed functional domains spawned from gap review |
| **This roadmap** | **Single prioritized execution map** across docs, backend, template workflow, frontend UX |

**Rules:**

1. Behavior-changing tasks require a behavior spec (or explicit `not-applicable`) before implementation.
2. TDD loop + green gates before `Done` (see `.cursor/rules/tdd-bdd-delivery-constitution.mdc`).
3. Doc sync in the same change set when status or behavior changes.
4. Only **one optimization wave** may be `In Progress` at a time (same discipline as phase plans).
5. Docker-only validation for manual acceptance (`http://localhost:4173`, `http://localhost:8080/healthz`).

---

## 0. Executive summary

The platform has a **runnable thin vertical slice** (P0–P11, P13, P16 partial, dashboard
consolidation) but three classes of gap remain:

| Class | Severity | Headline |
| --- | --- | --- |
| **Documentation drift** | High | Permission matrix, PRD IA, ledger, UX plan still describe workbenches / old routes / Done items that code no longer matches |
| **Contract & runtime correctness** | High | OpenAPI vs implementation (idempotency, output modes, async task states, batch partial failure) |
| **Product workflow completeness** | High | Template verifiability, publish gate, collaboration to-dos, controlled decision forms — PRD/P19 scope, not yet built |
| **Frontend workflow & OA UX** | High | Dashboard/workbench merge incomplete, deep links, pagination, error recovery, table/a11y, locale-aware formatting |

Recommended sequencing: **Wave COR-0 (docs truth)** → **COR-1 (API contract)** → **COR-2 (template workflow)** in parallel with **COR-3 (frontend UX)** → **COR-4 (performance/resilience)** → **COR-5 (tests/E2E)** → **COR-6 (P14/P18 large domains)**.

---

## 1. Active wave

| Wave | Scope | Status |
| --- | --- | --- |
| COR-0 | Documentation & plan-layer reconciliation | **Done** (2026-06-24; COR-D01–D09) |
| COR-1 | Runtime API contract & correctness | **Done** (2026-06-25; COR-B01–B12) |
| COR-2 | Template lifecycle, workflow & governance | **In Progress** — P19 active; thin slices Done (T05/T06 partial/T07 partial/T10–T14) |
| COR-3 | Frontend workflow & operation experience | **Done** (2026-06-25; F18 deferred to COR-L04) |
| COR-4 | Performance, resilience & architecture | **In Progress** (COR-P03/P04/P05/P06 Done 2026-06-25; P01/P02/P07/P08 open) |
| COR-5 | Test coverage & E2E journeys | **Done** (2026-06-25; COR-E01–E06) |
| COR-6 | Confirmed large domains (P14/P18+) | **Deferred** — activate one formal phase at a time (see §8) |

---

## 2. Wave COR-0 — Documentation & plan truth (do first)

Reconcile docs before large implementation so acceptance criteria stay authoritative.

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-D01 | H | Sync permission matrix §13 with dashboard + entitlement IA | Matrix still lists `route.tester-workbench`, old home routes; code uses `route.dashboard-home`, `route.identity-administration` | §13 default/visible routes match `RouteVisibilityService`; workbench merge decision recorded | Done | OPT-A |
| COR-D02 | H | Update PRD & domain-model lifecycle state mapping | PRD §7 has 测试通过/待审批; code uses `APPROVAL` + `approvalSubState` | Add implementation mapping table; no silent rename of confirmed requirements | Done | P5 footnote |
| COR-D03 | H | Refresh execution-sync-ledger P13/E12/UX mirrors | Ledger cites `IdentityAdministrationView`, workbench Done | P13 paths `/entitlement/*`; E12 workbench disposition documented | Done | post-task-doc-sync |
| COR-D04 | H | Fix optimization-plan internal contradictions | §0 F3 "no Bucket4j"; §2 F1/F2 Done; Wave 3 Not Started | F3 headline amended; Wave 3 status matches F1–F3/E8 Done | Done | OPT-A |
| COR-D05 | H | Reconcile P20 Done vs P20-T06 Not Started | master-plan P20 Done; detail T06 open; zh-CN ~20% of en | P20 status honest: Done only when T06 + zh-CN parity met, or revert to In Progress | Done | P20 |
| COR-D06 | M | P5 thin-slice boundary vs P19 | P5 Done implies full publish gate | P5 doc footnote: state machine + basic UI only; P19 owns verifiability | Done | P5, P19 |
| COR-D07 | M | UXC1/UXD1–2 Done vs static publish gate & workbench redirect | UX plan Done; apiPolicy gate informational; workbench redirect | UX plan rows updated or re-opened with evidence | Done | UX plan |
| COR-D08 | M | Transitional seams index | Config AD stub, in-process async default, log-only security audit scattered | Single doc section listing seam + exit criteria | Done | ledger |
| COR-D09 | L | Gate evidence refresh after dashboard commit | Ledger test counts 189 vs 114 | Single authoritative gate row with date + test count | Done | ledger |

**Exit:** All rows above merged; `docs/README.md` index current; no conflicting Done claims for open code gaps.

---

## 3. Wave COR-1 — Runtime API contract & correctness

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-B01 | H | De-scope runtime v1 `SYNC_DOWNLOAD_URL` | ADR 0038; reject at sync validate | Enum retained; runtime rejects with outputModeUnsupported; ADR 0038 | **Done** (2026-06-24) | P7 |
| COR-B02 | H | Idempotency semantics alignment | 86400s TTL vs domain 7d; hash excludes encryption; headers CREATED vs IDEMPOTENCY_* | 7d TTL; hash includes encryption; IDEMPOTENCY_NEW/REPLAYED headers; conflict summary | **Done** (2026-06-24) | OPT-E, P7 |
| COR-B03 | H | Idempotency IN_PROGRESS race | Re-entrant generate without storage key re-runs engine | Second concurrent request waits or returns consistent conflict; test proves no double generate | **Done** (2026-06-24) | OPT-E |
| COR-B04 | H | Async task FAILED/PARTIAL/EXPIRED lifecycle | Enum exists; runner no try/catch; summary always all SUCCEEDED | Tasks transition on failure; cancel/query reflect real status | **Done** (2026-06-24) | P11 |
| COR-B05 | H | Sync batch partial failure envelope | Whole batch 500 on one item failure | Structured per-item status + error per domain-model | **Done** (2026-06-24) | P11 |
| COR-B06 | M | Sync generate output.mode policy check | Batch path validates mode; sync path does not | Same policy gate on sync and batch | **Done** (2026-06-24) | P6 |
| COR-B07 | M | Runtime generation audit persistence | Download SLF4J only; no sync/batch/async audit rows | Standard audit summary persisted per domain-model | **Done** (2026-06-24; V17 + RuntimeGenerationAuditRecorder) | P8 |
| COR-B08 | M | Audit query pagination | Unbounded list; GLOBAL_ADMIN lifecycle findAll | Pageable API + default page size; tests | **Done** (2026-06-24; page/size on audit APIs) | OPT-F4 |
| COR-B09 | M | Redis idempotency read path | Cache written but findExisting DB-only | Hot path uses cache with DB fallback; test | **Done** (2026-06-24; cache-first conflict + warm) | OPT-F |
| COR-B10 | M | Rate limit multi-instance + missing credential | Process-local Bucket; bypass without credential headers | Document single-instance limit or add shared limiter; fail-closed default documented | **Done** (2026-06-24; doc seam) | OPT-F1 |
| COR-B11 | L | Download response `oneTime` field | OpenAPI DownloadInfo.oneTime | Field present per contract or OpenAPI trimmed | **Done** (2026-06-24; `download.oneTime=false` header) | P10 |
| COR-B12 | L | Route type header EXPLICIT vs EXPLICIT_VERSION | Controller uses shortened enum | Align with OpenAPI or document mapping | **Done** (2026-06-24; `RouteType` constants + sync/batch/audit) | P7 |

**Exit:** OpenAPI contract test extended beyond operationId presence; critical enum/TTL behaviors green in `mvn verify`.

---

## 4. Wave COR-2 — Template lifecycle, workflow & governance

### 4.1 Backend & product workflow

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-T01 | H | Live publish gate (P19 core) | UI gate static; apiPolicy always ready | Server-side checklist blocks publish; UI reflects real blockers | **Partial** (2026-06-24; binding + apiPolicy server gate; UI fetches policy on publish panel) | P19-T06 |
| COR-T02 | H | Controlled test/approval opinion forms | Free-text commentSummary only | Structured forms per PRD §7; audit fields; fail returns to DRAFT | **Partial** (2026-06-25; fail/reject require reasonCategory + impactSummary + UI dialog) | P19-T07 |
| COR-T03 | H | GROUP_ADMIN exception intervention path | GROUP_ADMIN always canDecideTests/Approvals | Normal vs exception flows; reason + secondary confirm + audit marker | Not Started | PRD §7, permission-matrix |
| COR-T04 | H | Collaboration work items + optional timeout | PRD §7 668–672; no WorkItem entity | Role/group queue to-dos; timeout escalation without auto state change | Not Started | P14/P19, domain §2.9.4 |
| COR-T05 | M | Publish not hard-bound to dev version 1 | `findByTemplateIdAndDevVersionNumber(..., 1)` | Publish selects release candidate dev version; tests for multi-version | **Done** (2026-06-23; `requireReleaseCandidateVersion` + selection test) | P16 |
| COR-T06 | M | Multi release version callability | TemplateCallabilitySupport single release constraint | Per-version callable list matches deactivate/restore; runtime tests | **Done** (2026-06-23; per-version callability + contract list) | P16, P7 |
| COR-T07 | M | Publish permission doc + code alignment | Domain model says author can publish; code admin-only | Confirmed matrix entry; code matches decision | **Done** (2026-06-24; Batch B ADR) | COR-D02 |
| COR-T08 | M | Batch test + coverage thresholds | PRD §6.5; P19-T02/T03 Not Started | Multi-sample batch test + threshold blockers | **Partial** (2026-06-25; P19-T01/T02/T03 Done for batch + coverage slice; publish gate wiring pending in T06) | P19 |
| COR-T09 | M | Lifecycle panel context on detail | Approver cannot see test record summary inline | Integrated evidence panel (test, preview, diff, checklist) per role | Not Started | P19-T10 |
| COR-T10 | L | Semver publish UX | Manual text field default 1.0.0 | Level picker + conflict validation | **Done** (2026-06-24; major/minor/patch picker + semver utils) | PRD §7 |

### 4.2 Workflow routing & queues (backend + frontend)

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-T11 | H | Workbench vs Dashboard decision | Dashboard consolidation (Batch B default) | Decision ADR `decisions/2026-06-23-batch-b-workflow-defaults.md`; dead workbench views removed | **Done** (2026-06-24) | COR-D01 |
| COR-T12 | M | Dashboard rework tasks | No "return to author" tasks after reject | Task kinds cover draft rework + group filter/sort | **Done** (2026-06-24; master-rework + template-rework tasks) | PRD §7 |
| COR-T13 | M | Template list workflow filters | No "awaiting my test/approval/publish" filters | Filter chips or saved views per capability | **Done** (2026-06-24; workflow filter chips) | — |
| COR-T14 | M | Publish summary dialog | Confirm only; no release summary content | Dialog shows checklist + diff/test/coverage summaries per PRD | **Partial** (2026-06-24; publish summary dialog thin slice; full P19 evidence pending) | P19 |

**Exit:** Template can progress with auditable decisions; publish blocked by real gates; operator queues match PRD role model.

---

## 5. Wave COR-3 — Frontend workflow & operation experience

Aligned with `.cursor/skills/frontend-oa-design/SKILL.md` and `management-ui-constitution.md`.

### 5.1 Shell, navigation & session

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-F01 | H | In-shell brand switch (REDBC/GREENBC) | Brand only on login | Top bar brand switch; both presets verified | **Done** (2026-06-24) | P20, OA skill |
| COR-F02 | H | Breadcrumb / page context | No el-breadcrumb; entitlement URLs ambiguous | Unified breadcrumb from navStructure | **Done** (2026-06-24) | E11/E12 |
| COR-F03 | H | 401 redirect preserves destination | http.ts 401 → login without redirect query | Post-login return to expired page | **Done** (2026-06-24) | OPT-G1 |
| COR-F04 | M | Page layout primitive | Dashboard max-width 1200px; others full bleed | Shared layout: header + max-width + spacing scale | **Done** (2026-06-24; `AppPageLayout` + dashboard/api policy migration) | OA skill |

### 5.2 Dashboard & work queues

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-F05 | H | Dashboard load error recoverable | el-alert only; stats may show stale zeros | LoadErrorPanel + retry; hide stats on failure | **Done** (2026-06-24) | — |
| COR-F06 | M | Stat cards permission-aware | 8 cards always; quick links filtered | Cards match visibleRoutes | **Done** (2026-06-24) | — |
| COR-F07 | M | Pending-actions card navigates to tasks | path `/dashboard` self-loop | Scroll/focus #tasks-section or filter table | **Done** (2026-06-24) | — |
| COR-F08 | M | Dashboard task table UX | No sort/sticky/keyboard row nav | Sort + sticky header + Enter opens detail | **Done** (2026-06-24; sticky scroll + Enter) | OA skill |

### 5.3 Template & master journeys

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-F09 | H | List pagination vs group sections | slice(0,10) then group by groupCode | Pagination preserves group semantics (design A or B) | **Done** (2026-06-24; group-first pagination) | — |
| COR-F10 | M | Template detail tab deep linking | activeDetailTab local only | Query/hash sync; workflow links to lifecycle tab | **Done** (2026-06-24) | COR-T11 |
| COR-F11 | M | Workflow banner action anchor | Banner text only | CTA scrolls/opens lifecycle/review panel | **Done** (2026-06-23; banner CTA + lifecycle panel scroll) | COR-T14 |
| COR-F12 | M | Template create dialog validation | Silent empty submit; errors not shown in dialog | el-form rules + inline/API errors | **Done** (2026-06-23; `TemplateCreateDialog` rules + alert + Vitest) | OPT-G8 |
| COR-F13 | M | Governance triple-dialog simplification | prompt → impact → confirm ×3 | ≤2 steps with reason + impact inline | **Done** (2026-06-23; governance + version actions merged to 2 steps) | — |
| COR-F14 | M | Unified recoverable errors | Lists/audit/release history use el-alert | LoadErrorPanel pattern everywhere async | **Done** (2026-06-23; audit, release history, governance home) | — |

### 5.4 Identity, audit, API policy

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-F15 | H | Identity list pagination UI | API page/size; UI always page 0 | el-pagination + total; E2E page 2 | **Done** (2026-06-24; users + groups panels) | P13 |
| COR-F16 | M | GROUP_ADMIN audit filter validation | Missing templateId → 422 at API | Proactive UI validation + message | **Done** (2026-06-23; client filter gate + Vitest) | P8 |
| COR-F17 | M | Audit console UX | No filter reset; client slice pagination | Reset + export scope dialog; server pagination plan | **Done** (2026-06-24; server page + reset + export confirm) | OPT-F4 |
| COR-F18 | M | API policy per-domain UI | Monolithic form in TemplateDetailView | Domain nav + save per P17/matrix §7 | Not Started | P17 |

### 5.5 i18n, a11y & polish

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-F19 | H | zh-CN parity + api.error catalog | en 939 lines vs zh-CN 184; 2 api.error keys | P20-T06 complete; locale switch no mass English fallback | **Done** (2026-06-25; api.error en/zh + primary journey zh-CN bundles) | P20-T06, OPT-G7 |
| COR-F20 | M | Locale-aware date/number formatting | toLocaleString() without app locale | Shared format helpers bound to i18n locale | **Done** (2026-06-24; `useLocaleFormatters` + audit/template detail) | P20 |
| COR-F21 | M | Table a11y baseline | row-click only navigation | Focus rings, keyboard row activation, sortable columns | **Done** (2026-06-24; AppDataTable + master/template list activatable rows) | OPT-B4 |
| COR-F22 | M | Design token cleanup in shell | Hardcoded hex in ManagementShell | CSS variables only; dual-brand check | **Done** (2026-06-25; shell/layout use design tokens only) | P20 |
| COR-F23 | L | Forbidden / error traceId copy | traceId text only | Copy reference control | **Done** (2026-06-24) | — |
| COR-F24 | L | Login locale + client validation | No locale on login; no username format check | Locale switch + inline validation | **Done** (2026-06-24) | P1 |

**Exit:** Role journeys meet OA skill state completeness; Docker 4173 smoke covers login → dashboard → template lifecycle tab.

---

## 6. Wave COR-4 — Performance, resilience & architecture

Consolidates remaining [optimization-plan.md](./optimization-plan.md) items not covered above.

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-P01 | M | Stream new sync artifact generation | New path still byte[] | Stream from storage on create path | Not Started | OPT-F3 |
| COR-P02 | M | Offload LibreOffice from request thread | Inline PDF in request | Bounded pool + timeout | Not Started | OPT-F6 |
| COR-P03 | L | LibreOffice temp dir cleanup | No finally on temp dirs | No leak under load test | **Done** (2026-06-24; try/finally temp dir cleanup + test) | OPT-F7 |
| COR-P04 | M | EAGER fetch on master anchors | MasterDocumentEntity EAGER | LAZY + fetch join where needed | **Done** (2026-06-25; LAZY + batch count + EntityGraph detail) | OPT-F5 |
| COR-P05 | L | Redisson lock evaluation | Multi-instance idempotency/async owner | ADR decision + implementation if multi-instance | **Done** (2026-06-24; ADR-0039 evaluation) | OPT-F8 |
| COR-P06 | M | Declarative route authorization | RouteVisibilityService not enforced on API | Filter or documented service-layer pattern + gap test | **Done** (2026-06-24; ADR + ManagementAuthorizationContractTest) | OPT-D6 |
| COR-P07 | M | QueryDSL for audit/complex lists | JPQL + in-memory filter | Type-safe pageable queries | Not Started | OPT-D4 |
| COR-P08 | L | MapStruct opportunistic adoption | Hand-written mappers | MapStruct on touched services only | Not Started | OPT-D3 |

---

## 7. Wave COR-5 — Test coverage & E2E

| ID | Pri | Title | Evidence | Acceptance | Status | Maps |
| --- | --- | --- | --- | --- | --- | --- |
| COR-E01 | H | Docker-target Playwright journeys | playwright.config → 5173 dev | CI/local job against 4173 prod profile | **Done** (2026-06-24; `playwright.docker.config.ts` + `test:e2e:docker`) | OPT-C7 |
| COR-E02 | H | Role journey E2E minimum set | Only login a11y smoke | login→dashboard; forbidden; identity read; template lifecycle smoke | **Done** (2026-06-24; `role-journeys.spec.ts`) | OPT-C7, E06 |
| COR-E03 | M | AuditQueryService tests | 264 lines untested | Group scope + GLOBAL_ADMIN paths | **Done** (2026-06-24; AuditQueryServiceTest) | OPT-C5 |
| COR-E04 | M | Rendering PDF path tests | LibreOffice/DockerExec untested | Success/timeout/cleanup mocked | **Done** (2026-06-24; pipeline + LibreOffice tests) | OPT-C4 |
| COR-E05 | M | Frontend: DashboardView + tab router tests | No tests for new surfaces | Vitest for tasks, load error, tab query sync | **Done** (2026-06-23; dashboard tasks + `templateDetailTabs` + load error) | OPT-C6 |
| COR-E06 | M | messageKey → UI mapping tests | errorEnvelope without e2e UI | Store/view tests for catalog keys | **Done** (2026-06-25; store + catalog + ForbiddenView tests) | P20-T06 |

---

## 8. Wave COR-6 — Confirmed large domains (phase-aligned)

Execute as formal phases when selected; **do not mark Done without full P14/P18/P19 exit criteria**. Remaining COR-2/3 rows mapped here stay `Not Started` until the owning phase is activated.

| ID | Phase | Title | Status | Detail plan |
| --- | --- | --- | --- | --- |
| COR-L01 | P14 | Clause/content module lifecycle | **Deferred** | [P14](./detail/P14-confirmed-large-domains.md) |
| COR-L02 | P18 | Structured authoring & fidelity engine | **Deferred** | [P18](./detail/P18-structured-authoring-fidelity-engine.md) |
| COR-L03 | P19 | Full verifiability + publish gate + decision forms | **Deferred** | [P19](./detail/P19-verifiability-publish-gate.md) — owns COR-T01/T02/T03/T04/T08/T09/T14 remainder |
| COR-L04 | P17 | Per-domain API policy save/rollback | **Deferred** | [P17](./detail/P17-api-policy-domain-governance.md) — owns COR-F18 |
| COR-L05 | P16 | Lifecycle/version governance completeness | **Deferred** | [P16](./detail/P16-lifecycle-version-governance.md) |
| COR-L06 | P15 | Kubernetes & container hardening | **Deferred** | [P15](./detail/P15-kubernetes-deployment-container-hardening.md) |

---

## 9. Cross-cutting: frontend workflow patterns to enforce

When implementing COR-3, apply these **non-negotiable patterns** across all management surfaces:

| Pattern | Required behavior | Anti-pattern today |
| --- | --- | --- |
| Async data | loading → empty → error (retry) → success | alert-only error on lists |
| Destructive action | confirm + reason where PRD requires | triple stacked dialogs |
| Workflow entry | deep link to tab/section + primary CTA | banner hint text only |
| Tables | pagination, sort where meaningful, sticky header | client slice breaks grouping |
| Permissions | hide controls backend would deny | show 0-count cards for forbidden modules |
| i18n | keys + locale-aware dates | English dates under zh-CN UI |
| Session | preserve redirect on 401 | drop user on dashboard after expiry |

---

## 10. Recommended execution order

```text
COR-0 (docs)     ──► parallel track A: COR-1 (API contract)
                 ──► parallel track B: COR-3 P0 (F01–F05, F09, F15, F19)
COR-2 (template) ──► depends on COR-0 D02/D06; P19 slices COR-T01–T04
COR-3 remainder  ──► after COR-0 D01 (workbench decision COR-T11)
COR-4            ──► after COR-1 B02/B04 stable
COR-5            ──► continuous; E01/E02 gate COR-3 Done claims
COR-6            ──► single active phase when product selects P16–P19
```

**Suggested first sprint (2 weeks):**

1. COR-D01, COR-D03, COR-D06, COR-D07 (documentation)
2. COR-T11 decision + COR-F05, COR-F10, COR-F11 (dashboard + workflow deep links)
3. COR-B02 (idempotency TTL/hash) + COR-F19 start (api.error catalog)

---

## 11. Done definition (this roadmap)

A task is `Done` only when:

1. Behavior spec updated in owning doc (if behavior-changing).
2. Failing test written first; smallest change to green.
3. Gates green: `mvn verify` and/or `pnpm lint && type-check && test && build`.
4. Docker rebuild verified for user-facing changes (`.\scripts\docker-deploy.ps1`).
5. This row + mapped OPT/UX/P* doc statuses updated in the same change set.
6. Frontend user-facing Done also requires Playwright functional smoke on 4173 (minimum COR-E02 subset).

---

## 12. Traceability matrix (review → roadmap)

| Review finding | Roadmap IDs |
| --- | --- |
| OpenAPI idempotency / SYNC_DOWNLOAD_URL | COR-B01, COR-B02, COR-B11 |
| Async/batch task states | COR-B04, COR-B05 |
| Audit pagination / runtime audit | COR-B07, COR-B08, COR-F17 |
| Template publish gate thin slice | COR-T01, COR-D06, COR-D07 |
| GROUP_ADMIN exception intervention | COR-T03 |
| Workbench vs dashboard | COR-T11, COR-D01, COR-D07 |
| Collaboration to-dos | COR-T04, COR-T12 |
| Multi-version publish/callability | COR-T05, COR-T06 |
| Permission matrix drift | COR-D01, COR-D02 |
| P20 / zh-CN / api.error | COR-D05, COR-F19, COR-E06 |
| Dashboard error/stats | COR-F05, COR-F06, COR-F07 |
| Identity pagination | COR-F15 |
| List grouping pagination | COR-F09 |
| OA shell breadcrumb/brand | COR-F01, COR-F02, COR-F22 |
| E2E Docker 4173 | COR-E01, COR-E02 |

---

## 13. Maintenance

When completing any COR-* task:

1. Update this file's Status column.
2. Update mapped row in `optimization-plan.md` or `ux-upgradeability-optimization-plan.md` if applicable.
3. Update phase detail plan (P16–P20) if scope overlaps.
4. Append gate evidence to [execution-sync-ledger.md](./execution-sync-ledger.md).
5. Run post-task doc sync per `.cursor/skills/post-task-doc-sync/SKILL.md`.

**Last reviewed:** 2026-06-25 (COR-P04, COR-F19 api.error zh-CN, COR-F22, COR-E06 Done; COR-1/COR-5 waves closed).
