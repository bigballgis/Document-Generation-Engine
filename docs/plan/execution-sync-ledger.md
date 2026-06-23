# Execution Sync Ledger

**Last synced:** 2026-06-23 (P13 identity & group administration Done; backend Wave A foundation + UX Wave A/B)  
**Purpose:** Cross-reference plan phases (P0–P11), epics (E01–E12), and milestones (M1–M14) after re-earning Done status with real code and green gates.

## Authority

| Layer | Source of truth | Scope |
| --- | --- | --- |
| Phase delivery | [master-plan.md](./master-plan.md) + [detail/](./detail/) | Exit criteria, task status |
| Epic ordering | [orchestration-high-level-plan.md](../architecture/orchestration-high-level-plan.md) | Delivery focus |
| Technical waves | [implementation-task-plan.md](../architecture/implementation-task-plan.md) | OpenAPI wave mapping |
| This ledger | Mirror + evidence pointers | Epic/milestone ↔ phase mapping |

On conflict between this ledger and a stale task-sheet row, **plan layer wins** until the task sheet is refreshed.

## Quality gate evidence (2026-06-23)

| Gate | Command | Result |
| --- | --- | --- |
| Backend | `mvn -B -ntp -f backend/pom.xml verify` | Green (71 tests, 2026-06-23). Now includes real Checkstyle + PMD + SpotBugs + JaCoCo `check` gates (OPT-B/B1–B2): 0 Checkstyle / PMD / SpotBugs violations; JaCoCo ratchet floors LINE ≥0.70 / BRANCH ≥0.45 (actual 0.746 / 0.473). Config under `backend/config/`; documented ratchet debts in `config/spotbugs/exclude.xml`. |
| Frontend lint | `pnpm -C frontend lint` | Green |
| Frontend type-check | `pnpm -C frontend type-check` | Green |
| Frontend test | `pnpm -C frontend test` | Green (40 tests) |
| Frontend build | `pnpm -C frontend build` | Green |

## Phase status (plan layer)

| Phase | Status | Evidence summary |
| --- | --- | --- |
| P0 | Done | Skeleton, compose, CI gates, contract harness |
| P1 | Done | Local auth, session, role landing, guards |
| P2 | Done | Master APIs + list/detail UI (`MasterListView`, `MasterDetailView`) |
| P3 | Done | Template wizard, variables, rules, test data sets |
| P4 | Done | DOCX/PDF render, preview records, comparison panel |
| P5 | Done | Lifecycle + publish gate UI (thin slice checklist) |
| P6 | Done | API policy, credentials, caller contract view |
| P7 | Done | Runtime sync/batch/async, idempotency, encryption |
| P8 | Done | Audit console, masked export |
| P9 | Done | Observability, local security gates, prod compose profile |
| P10 | Done | Secure download with expiry |
| P11 | Done | Batch + async task lifecycle |
| P12 | Not Started | Deferred enhancements catch-all (non-active, no single active slice) |
| P13 | Done | Identity & group administration (user + group management plane); green gates 2026-06-23 — see P13 mirror block |
| P14 | Not Started | Confirmed large domains (renumbered from P13) |
| P15 | Not Started | Kubernetes deployment & container hardening — implements unrealized ADR-0030 K8s/container rows; _evidence pending_ (manifests/Helm, hardened images, CI `kubeconform`/`helm lint`) |
| P16 | Not Started | Template & version lifecycle governance completeness (gap G1: stop/restore/deprecate + version deactivate/restore + impact preview); _evidence pending_ |
| P17 | Not Started | Per-domain API policy governance (gap G2: config-domain save + impact preview + policyVersion + rollback); _evidence pending_ |
| P18 | Not Started | Structured authoring & rendering-fidelity engine (gap G3: node matrix, style catalog, paste cleaning, render profile); _evidence pending_ |
| P19 | Not Started | Template verifiability, publish gate & decision forms (gaps G4/G5: coverage thresholds, batch test, live checklist, opinion forms, risk prompts); _evidence pending_ |
| P20 | Not Started | i18n multi-locale readiness & UI upgradeability (gap G6: locale registry/switcher/fallback, config-driven brand, environment selector); _evidence pending_ |

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
| E12 | Done (thin slice); UX Wave A/B extension in progress | P1, E11 | `useCapabilities`, `TesterWorkbenchView`, `ApproverWorkbenchView`, `TemplateCreateDialog`, credential rotate/revoke UI; see [e12-phase3-task-sheet.md](../architecture/e12-phase3-task-sheet.md) |
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
| Epic E13 (identity & group administration) | Done | `GroupDimension`, `BusinessGroupEntity`/`BusinessGroupRepository`, mutable `ManagementUserEntity` + extended repository, `BusinessGroupService`, `UserManagementService`, `GroupManagementController` (`/api/management/v1/groups`), `UserManagementController` (`/api/management/v1/users`); migrations `V14__management_role_seeds.sql` + `V15__business_group_management.sql`; frontend `IdentityAdministrationView` + `UserManagementPanel`/`GroupManagementPanel`, route `/home/identity` |
| Behavior + escalation guard | Done | Fail-closed `GROUP_SCOPE_OUT_OF_RANGE` / `ROLE_ASSIGNMENT_NOT_ALLOWED` / `USER_DELETE_NOT_ALLOWED` / `GROUP_MANAGEMENT_NOT_ALLOWED` (+ management-only `NOT_FOUND`/`CONFLICT`) wired via `GlobalExceptionHandler`/`ApiErrorCodes`; `ManagementRoute.IDENTITY_ADMINISTRATION` exposed to GLOBAL_ADMIN/GROUP_ADMIN by `RouteVisibilityService`; audit recorded; covered by `BusinessGroupServiceTest` (8), `UserManagementServiceTest` (12), `IdentityGroupAdministrationSliceTest` (17), `RouteVisibilityServiceTest` (7), `ManagementCapabilitiesServiceTest` (6) |
| Backend gate | Green | `mvn -B -ntp -f backend/pom.xml verify` = BUILD SUCCESS; Tests run: 114, Failures: 0, Errors: 0, Skipped: 1; JaCoCo "All coverage checks have been met"; Checkstyle 0 violations; SpotBugs no warnings |
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
| UX Wave B | UX-C + UX-D + UX-E (lifecycle, workbenches, polish) | In Progress (UXC3–UXC4, UXD4, UXE2–UXE4 open) | `TesterWorkbenchView`, `ApproverWorkbenchView`, live publish gate (UXC1), cross-links (UXC5), `V15` rules persist, `TemplateAuthoringPanel` / `TemplateRuleConfigurator` |
| UX Wave C | UX-F (upgradeability foundations) | Not Started | — |
| UX Wave D | UX-G → P14 confirmed large domains | Not Started | — |

**Backend gate evidence (UX Wave A/B, 2026-06-23):** `mvn -B -ntp -f backend/pom.xml verify "-Dspring-boot.repackage.skip=true"` — 114 tests green (1 skipped); includes `TemplatePlatformSliceTest#savesCompositionRulesAndReturnsThemOnTemplateDetail`, `ManagementCapabilitiesServiceTest`, `RouteVisibilityServiceTest`.

**Frontend gate evidence (UX Wave A/B, 2026-06-23):** `pnpm -C frontend lint` / `type-check` / `test` (81) / `build` — all green.

## Sync maintenance

When any task completes:

1. Update owning `docs/plan/detail/<phase>.md` task row.
2. Update `master-plan.md` if phase status changes.
3. Update epic/milestone row in this ledger (or owning task sheet).
4. Update [orchestration-high-level-plan.md](../architecture/orchestration-high-level-plan.md) / [implementation-task-plan.md](../architecture/implementation-task-plan.md) if backlog order changes.
5. Run post-task doc sync (see `.cursor/agents/post-task-doc-sync.md`).
