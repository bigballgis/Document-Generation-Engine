# Execution Sync Ledger

**Last synced:** 2026-06-23  
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
| Backend | `mvn -B -ntp -f backend/pom.xml verify` | Green |
| Frontend lint | `pnpm -C frontend lint` | Green |
| Frontend type-check | `pnpm -C frontend type-check` | Green |
| Frontend test | `pnpm -C frontend test` | Green (36 tests) |
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

**Active phase:** P12 (deferred enhancements — no single active slice).

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
| E12 | Done (thin slice) | P1, E11 | Role home pages, Playwright a11y smoke (`e2e/a11y-smoke.spec.ts`) |

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

## Sync maintenance

When any task completes:

1. Update owning `docs/plan/detail/<phase>.md` task row.
2. Update `master-plan.md` if phase status changes.
3. Update epic/milestone row in this ledger (or owning task sheet).
4. Update [orchestration-high-level-plan.md](../architecture/orchestration-high-level-plan.md) / [implementation-task-plan.md](../architecture/implementation-task-plan.md) if backlog order changes.
5. Run post-task doc sync (see `.cursor/agents/post-task-doc-sync.md`).
