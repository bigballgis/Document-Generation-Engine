# Execution Sync Ledger

**Last synced:** 2026-06-25 (P19-T03 coverage computation + thresholds)  
**Purpose:** Cross-reference plan phases (P0–P11), epics (E01–E12), and milestones (M1–M14) after re-earning Done status with real code and green gates.

## Authority

| Layer | Source of truth | Scope |
| --- | --- | --- |
| Phase delivery | [master-plan.md](./master-plan.md) + [detail/](./detail/) | Exit criteria, task status |
| Epic ordering | [orchestration-high-level-plan.md](../architecture/orchestration-high-level-plan.md) | Delivery focus |
| Technical waves | [implementation-task-plan.md](../architecture/implementation-task-plan.md) | OpenAPI wave mapping |
| This ledger | Mirror + evidence pointers | Epic/milestone ↔ phase mapping |

On conflict between this ledger and a stale task-sheet row, **plan layer wins** until the task sheet is refreshed.

## Quality gate evidence (authoritative row)

| Gate | Command | Result | Notes |
| --- | --- | --- | --- |
| Backend (latest full verify) | `mvn -B -ntp -f backend/pom.xml verify` | Green — **256 tests**, 2026-06-25 | P19-T03 coverage + thresholds |
| Frontend lint | `pnpm -C frontend lint` | Green | |
| Frontend type-check | `pnpm -C frontend type-check` | Green | |
| Frontend test | `pnpm -C frontend test` | Green | **149 tests**, 2026-06-25 |
| Frontend build | `pnpm -C frontend build` | Green | |
| E2E Docker (4173) | `pnpm -C frontend test:e2e:docker` | Green — **6 tests**, 2026-06-25 | post P19/P20 batch + role-journeys row-click fix |

**Test count progression (not conflicting runs):** P13 slice verify **114** backend tests (2026-06-23);
Wave C UX **161** backend / **88** frontend; post OPT-E8/F3 full verify **189** backend;
COR-B02/F05 Batch B slice **193** backend / **104** frontend (2026-06-24);
COR-1/COR-3 sprint **198** backend / **106** frontend (2026-06-24);
COR-B04/B05 + COR-F02/F09 **200** backend / **108** frontend (2026-06-24);
COR-B07/B08 + COR-F08/F15 **201** backend / **112** frontend (2026-06-24);
COR-B09 + COR-F17 + prior uncommitted slice **205** backend / **114** frontend (2026-06-24);
COR-B11/B12 + COR-T01 binding gate + COR-E02 E2E **206** backend / **114** frontend (2026-06-24);
COR-E03/E04 + master file replace **222** backend / **114** frontend (2026-06-24);
COR-F12/E05/E06 template create validation + dashboard/tab/messageKey tests **121** frontend (2026-06-23);
COR-F11/F13 workflow banner CTA + governance 2-step confirm + publish messageKey tests **125** frontend (2026-06-23);
COR-F14/F16 LoadErrorPanel + audit filter validation + COR-T05 publish candidate **223** backend / **129** frontend (2026-06-23);
COR-T06/F21/E06 multi-version callability + table a11y baseline + audit messageKey **227** backend / **131** frontend (2026-06-23);
COR final batch (catalog package UX + publish gate apiPolicy + workflow filters + i18n/a11y polish) **238** backend / **139** frontend (2026-06-24);
COR-P04 + zh-CN api.error catalog + E06/F22 **238** backend / **144** frontend (2026-06-25);
P19-T07 + P20 primary zh-CN + Docker E2E **243** backend / **149** frontend (2026-06-25);
P19-T01 test data set governance **248** backend / **149** frontend (2026-06-25).
P19-T02 batch test generation **252** backend / **149** frontend (2026-06-25).
P19-T03 coverage computation + thresholds **256** backend / **149** frontend (2026-06-25).
Use the latest full-verify row above for gate claims; milestone blocks below are point-in-time snapshots.

## Phase status (plan layer)

| Phase | Status | Evidence summary |
| --- | --- | --- |
| P0 | Done | Skeleton, compose, CI gates, contract harness |
| P1 | Done | Local auth, session, role landing, guards |
| P2 | Done | Master APIs + list/detail UI (`MasterListView`, `MasterDetailView`) |
| P3 | Done | Template wizard, variables, rules, test data sets |
| P4 | Done | DOCX/PDF render, preview records, comparison panel |
| P5 | Done | Lifecycle + publish gate UI (**thin slice** — state machine + checklist UI; live gates → P19) |
| P6 | Done | API policy, credentials, caller contract view |
| P7 | Done | Runtime sync/batch/async, idempotency, encryption |
| P8 | Done | Audit console, masked export |
| P9 | Done | Observability, local security gates, prod compose profile |
| P10 | Done | Secure download with expiry |
| P11 | Done | Batch + async task lifecycle |
| P12 | Not Started | Deferred enhancements catch-all (non-active, no single active slice) |
| P13 | Done | Identity & group administration (user + group management plane); green gates 2026-06-23 — see P13 mirror block |
| P14 | Spec Done (2026-06-23) | Confirmed large domains — behavior specs in P14; implementation Not Started |
| P15 | Not Started | Kubernetes deployment & container hardening — implements unrealized ADR-0030 K8s/container rows; _evidence pending_ (manifests/Helm, hardened images, CI `kubeconform`/`helm lint`) |
| P16 | Done (2026-06-23) | Template lifecycle governance + logical delete (T01–T08); gates: `mvn verify` 161 tests |
| P17 | In Progress | Impact-preview seam Done (T03/T09 partial); per-domain save/rollback open |
| P20 | In Progress (2026-06-24) | Wave C UXF1/4/5 Done; **P20-T06** (zh-CN + `api.error` catalog parity) open — see [P20 detail](./detail/P20-i18n-ui-upgradeability.md) |
| P18 | Not Started | Structured authoring & rendering-fidelity engine (gap G3: node matrix, style catalog, paste cleaning, render profile); _evidence pending_ |
| P19 | Not Started | Template verifiability, publish gate & decision forms (gaps G4/G5: coverage thresholds, batch test, live checklist, opinion forms, risk prompts); _evidence pending_ |

**Active phase:** None — no single active phase slice (P13 completed Done 2026-06-23).
P12 remains the non-active deferred-enhancements catch-all.

## Epic ↔ phase mapping

| Epic | Status | Maps to | Evidence |
| --- | --- | --- | --- |
| E01 | Done | P2, P3, P4 | `MasterDocumentService`, `TemplateService`, `PreviewGenerationService`, frontend template/master views |
| E02 | Done | P5 | `TemplateLifecycleService`, lifecycle UI in `TemplateDetailView` |
| E03 | Done | P6 | `ApiManagementService`, `ApiPolicyHomeView`, `TemplateCallerContractPanel` |
| E04 | Done | P8 | `AuditQueryService`, `AuditConsoleView` |
| E05 | Done (thin slice) | P0, P7, P9 | Flyway + JPA, Redis idempotency, MinIO, Kafka async; `ConfigAdGroupResolver` stub |
| E06 | Done | P1, P5, P6, P8 | `ManagementShell`, dual-brand tokens, role homes |
| E07 | Done (local gates) | P9 | `scripts/` release gate; external deploy evidence pending |
| E11 | Done | P1 | `routeKeys.ts`, `RoleHomeView`, `ForbiddenView`, router guards |
| E12 | Done (thin slice); Dashboard consolidation (2026-06-24) | P1, E11 | `useCapabilities`, `DashboardView` (`/dashboard`, `route.dashboard-home`), workflow tasks on dashboard; legacy workbench views redirect to dashboard — **COR-T11 decision pending**; see [e12-phase3-task-sheet.md](../architecture/e12-phase3-task-sheet.md) |
| E13 | Done | P13 | Identity & group administration (local account store as authorization authority, ADR 0036); `BusinessGroupService`, `UserManagementService`, `GroupManagementController` (`/api/management/v1/groups`), `UserManagementController` (`/api/management/v1/users`), fail-closed escalation guards, `IdentityAdministrationView`; green gates 2026-06-23 — see P13 mirror block |

### E05 / E07 outstanding (not blocking MVP Done)

| Item | Status | Document |
| --- | --- | --- |
| E05-T06 External validation evidence | Not Started | [e05-external-validation-evidence.md](../architecture/e05-external-validation-evidence.md) |
| E06 role-journey release evidence | Not Started | [e06-role-journey-release-evidence.md](../architecture/e06-role-journey-release-evidence.md) |
| M9-T02 Intranet SCA submission | Not Started | [m9-t02-closure-plan.md](../architecture/m9-t02-closure-plan.md) |
| M10–M11 Deferred security closure | Not Started | m10/m11 task sheets |

## Milestone ↔ wave mapping

| Milestone | Wave | Status | Phase / evidence |
| --- | --- | --- | --- |
| M1 | 0–1 | Done | P0, P7 contract endpoints |
| M2 | 2 | Done | P7 sync generate |
| M3 | 3 | Done | P11 batch/async |
| M4 | 4 | Done | P10 download |
| M5 | 5 | Done | P6 API management |
| M6 | 6 | Done | P5 lifecycle |
| M7 | 7 | Done | P7 integration tests (`TemplatePlatformSliceTest`) |
| M8 | 8 | Done (local) | P9 Checkstyle/PMD/SpotBugs in verify |
| M9 | 9 | In Progress | Local SBOM; intranet SCA not submitted |
| M10 | 10 | Not Started | Deferred security closure |
| M11 | 11 | Not Started | Intranet security baseline |
| M12 | 12 | Done | Runtime endpoint adapters |
| M13 | 13 | Done | Runtime HTTP transport + auth filter |
| M14 | 14 | Done | Kafka/in-process async batch transport |

## P13 Identity & group administration (mirror + evidence)

| Item | Status | Evidence (2026-06-23) |
| --- | --- | --- |
| Epic E13 (identity & group administration) | Done | `GroupDimension`, `BusinessGroupEntity`/`BusinessGroupRepository`, mutable `ManagementUserEntity` + extended repository, `BusinessGroupService`, `UserManagementService`, `GroupManagementController` (`/api/management/v1/groups`), `UserManagementController` (`/api/management/v1/users`); migrations `V14__management_role_seeds.sql` + `V15__business_group_management.sql`; frontend **`UserManagementView`** / **`GroupManagementPanel`** at **`/entitlement/users`** and **`/entitlement/groups`** (`route.identity-administration`); legacy `/home/identity` redirects to `/entitlement/users` |
| Behavior + escalation guard | Done | Fail-closed `GROUP_SCOPE_OUT_OF_RANGE` / `ROLE_ASSIGNMENT_NOT_ALLOWED` / `USER_DELETE_NOT_ALLOWED` / `GROUP_MANAGEMENT_NOT_ALLOWED` (+ management-only `NOT_FOUND`/`CONFLICT`) wired via `GlobalExceptionHandler`/`ApiErrorCodes`; `ManagementRoute.IDENTITY_ADMINISTRATION` exposed to GLOBAL_ADMIN/GROUP_ADMIN by `RouteVisibilityService`; audit recorded; covered by `BusinessGroupServiceTest` (8), `UserManagementServiceTest` (12), `IdentityGroupAdministrationSliceTest` (17), `RouteVisibilityServiceTest` (7), `ManagementCapabilitiesServiceTest` (6) |
| Backend gate | Green | P13 slice: `mvn verify` — **114** tests (point-in-time snapshot, 2026-06-23); latest full verify **189** — see authoritative gate row |
| Frontend lint | Green | `pnpm -C frontend lint` — 0 errors |
| Frontend type-check | Green | `pnpm -C frontend type-check` — pass |
| Frontend test | Green | `pnpm -C frontend test` — 24 files / 81 tests passed (43 identity-related all green) |
| Frontend build | Green | `pnpm -C frontend build` — success |
| Post-task doc sync | Done | This sync (2026-06-23): master-plan / plan README / P13 detail / this ledger / PROJECT-STATUS-RESET / docs README updated |

Detailed task rows: [detail/P13-identity-group-administration.md](./detail/P13-identity-group-administration.md).

## UX optimization waves (mirror)

Source: [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md).

| Wave | Scope | Status | Evidence |
| --- | --- | --- | --- |
| UX Wave A | UX-A + UX-B (role gating, half-built interactions) | Done | Backend: `ManagementCapabilitiesService`, `RouteVisibilityService`, `V14` seeds; Frontend: `useCapabilities`, `roles.ts`, `TemplateCreateDialog`, `apiPolicy` rotate/revoke, `http.ts` 401/403, `useConfirmAction`, authoring persist (UXB5) |
| UX Wave B | UX-C + UX-D + UX-E (lifecycle, workbenches, polish) | Done (2026-06-23) | UXC3–UXC5, UXC4 metadata edit, UXD4 governance dashboard, UXE2–UXE4 polish; gates: `mvn verify`, `pnpm lint/type-check/test/build` |
| UX Wave C | UX-F (upgradeability foundations) | Done (2026-06-23) | UXF1/4/5 + P16-T08 delete + P17 impact-preview seam; backend 161 tests, frontend 88 tests |
| UX Wave D | UX-G → P14 confirmed large domains | Spec Done (2026-06-23) | Behavior specs in P14 + matrix + ADR-0019; zero implementation until P14 activation |

**Backend gate evidence (UX Wave A/B, 2026-06-23):** `mvn -B -ntp -f backend/pom.xml verify "-Dspring-boot.repackage.skip=true"` — 134 tests green (1 skipped); includes `TemplateLifecycleGovernanceServiceTest`, `TemplatePlatformSliceTest#stopRestoreAndDeprecatePublishedTemplate`.

**Frontend gate evidence (UX Wave A/B, 2026-06-23):** `pnpm -C frontend lint` / `type-check` / `test` (81) / `build` — all green.

## Transitional implementation seams (2026-06-24)

Known gaps between ADR/guardrails and current code — **not** production-complete closure.
Each row lists exit criteria; remove from this index when closed.

| Seam | Current behavior | Exit criteria | Tracked in |
| --- | --- | --- | --- |
| AD Group resolution | `ConfigAdGroupResolver` — config-file stub, fail-closed | Production LDAP/AD adapter + integration tests | E05-T06, P6 |
| Async batch transport | Default in-process `@Async`; Kafka optional via `ASYNC_TRANSPORT=kafka` | Production profile uses Kafka + DLT; in-process dev-only documented | P11, M14 |
| Security forbidden-route audit | Log-only in some paths | Durable security audit event per matrix §13.3 | COR-P06 |
| QueryDSL / MapStruct / Redisson | Plain JPA + hand mappers + Lettuce | ADR-0037 scheduled items implemented or ADR amended | OPT-D, COR-P05 |
| Publish gate checklist | UI checklist + binding validation; API policy item partly static | Server-side live gate blocks publish (P19) — **binding + apiPolicy enforced server-side (2026-06-24)**; full P19 checklist remains | COR-T01, P19 |
| Runtime rate limit | Process-local Bucket4j; requests without credential headers bypass filter (auth layer rejects later) | Shared Redis limiter or documented fail-closed at filter; ADR 0031 alignment | COR-B10, OPT-F8 |
| Workbench vs Dashboard | **Done** — dead workbench views removed; routes redirect to `/dashboard` | COR-T11 decision recorded | COR-T11 |
| zh-CN / `api.error` catalog | **Done (2026-06-25)** — en/zh `api.error` + primary journey zh-CN bundles | Residual non-primary keys may en-fallback until touched | P20-T06 Done |
| P19 verifiability | **In Progress (2026-06-25)** — T06/T07 partial; publish gate binding+apiPolicy | Full batch test/coverage/checklist per P19 exit | P19, COR-L03 |
| Service-layer authorization | Route visibility not enforced at API filter | **Documented pattern + contract test (2026-06-24)** — ADR-0001 | COR-P06 |
| Redisson multi-instance locks | Lettuce cache only | **ADR-0039 evaluation recorded (2026-06-24)**; implement when multi-instance | COR-P05 |

## Sync maintenance

When any task completes:

1. Update owning `docs/plan/detail/<phase>.md` task row.
2. Update `master-plan.md` if phase status changes.
3. Update epic/milestone row in this ledger (or owning task sheet).
4. Update [orchestration-high-level-plan.md](../architecture/orchestration-high-level-plan.md) / [implementation-task-plan.md](../architecture/implementation-task-plan.md) if backlog order changes.
5. Run post-task doc sync (see `.cursor/agents/post-task-doc-sync.md`).
