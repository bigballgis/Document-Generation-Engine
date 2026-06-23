# P13 — Identity & Group Administration (Detailed Plan)

**Phase status:** Done (2026-06-23)  
**Depends on:** P1  
**Confirmed:** 2026-06-23 (product owner; doc layer fixed by doc-keeper)

## Source of truth

- Confirmed requirement: [requirements-plan.md](../../requirements/requirements-plan.md)
- PRD §6.0.1 用户与分组管理: [PRD.md](../../product/PRD.md)
- Permission matrix §9.1 / §9.2 / §9.3: [permission-matrix.md](../../security/permission-matrix.md)
- Domain model §2.1.2 ManagementUser, §2.3 BusinessGroup: [domain-model.md](../../domain/domain-model.md)
- ADR 0036 (local account store = authorization authority, SSO = authentication only):
  [0036-local-account-store-authorization-authority.md](../../adr/authorization-security/0036-local-account-store-authorization-authority.md)

## Behavior goal

A GLOBAL_ADMIN administers the local account store (users and business groups) as the
authorization authority, and a GROUP_ADMIN manages users **only within its authorized
group scope**. Both operate through a management-plane console and API with fail-closed
privilege-escalation protection, full audit, and green quality gates. SSO remains
authentication-only — identity, roles, and group scope are authoritative locally
(ADR 0036).

## Roles & scope (summary, authority = permission-matrix §9)

| Capability | GLOBAL_ADMIN | GROUP_ADMIN (scoped) | Others |
| --- | --- | --- | --- |
| Create/edit business group | ✓ | ✗ | ✗ |
| Disable/enable business group | ✓ | ✗ | ✗ |
| Create/edit user, assign roles, assign group scope | ✓ | within own scope, restricted roles | ✗ |
| Disable/enable user | ✓ | within own scope | ✗ |
| Reset user password | ✓ | within own scope | ✗ |
| Logical-delete user | ✓ | ✗ | ✗ |

## Behavior spec (BDD)

**Actor/role:** GLOBAL_ADMIN, GROUP_ADMIN. **Trigger:** management-plane request to
manage users/groups. **Preconditions:** authenticated session with roles + authorized
group scope (P1).

### Acceptance scenarios

- **Given** a GLOBAL_ADMIN, **When** creating a business group with `groupCode`+`dimension`,
  **Then** the group is persisted; `groupCode` and `dimension` are immutable afterwards and
  only the display name may change; an audit event is recorded.
- **Given** a GROUP_ADMIN authorized for {RETAIL}, **When** creating a user with group scope
  {RETAIL}, **Then** it succeeds; **When** requesting group scope {RETAIL, CORP}, **Then** the
  request is rejected `GROUP_SCOPE_OUT_OF_RANGE` (403) and audited as a denied escalation.
- **Given** a GROUP_ADMIN, **When** assigning GLOBAL_ADMIN / AUDIT_ADMIN / GROUP_ADMIN to a
  user, **Then** the request is rejected `ROLE_ASSIGNMENT_NOT_ALLOWED` (403).
- **Given** a GROUP_ADMIN, **When** logically deleting a user, **Then** the request is rejected
  `USER_DELETE_NOT_ALLOWED` (403); only GLOBAL_ADMIN may logical-delete.
- **Given** a GROUP_ADMIN, **When** calling any group create/edit/disable/enable endpoint,
  **Then** the request is rejected `GROUP_MANAGEMENT_NOT_ALLOWED` (403).
- **Given** any management user, **When** fetching a missing user/group, **Then** `NOT_FOUND`;
  **When** creating a duplicate `username` or `groupCode`+`dimension`, **Then** `CONFLICT`.
  (`NOT_FOUND`/`CONFLICT` are used only on the management plane.)

### Boundary / exception

- Fail-closed: any scope/role check that cannot be positively satisfied denies the action.
- Logical delete and disable preserve audit history; deleted/disabled users cannot authenticate.
- Group-scope changes never widen a GROUP_ADMIN's own effective scope.

## Design tasks

| ID | Task | Status |
| --- | --- | --- |
| P13-D01 | BusinessGroup domain design: `dimension` (BUSINESS_LINE\|DEPARTMENT), `enabled`, logical delete/disable; `groupCode`+`dimension` immutable post-create, display-name editable | Done |
| P13-D02 | ManagementUser administration design: create/edit, role assignment, group-scope assignment, disable/enable, password reset, logical delete (align domain-model §2.1.2) | Done |
| P13-D03 | GROUP_ADMIN fail-closed escalation guard design: scope ⊆ own scope; forbid GLOBAL_ADMIN/AUDIT_ADMIN/GROUP_ADMIN assignment; logical delete GLOBAL-only | Done |
| P13-D04 | Management-plane error model: `NOT_FOUND`/`CONFLICT` (management-only) + 403 escalation codes (`GROUP_SCOPE_OUT_OF_RANGE`, `ROLE_ASSIGNMENT_NOT_ALLOWED`, `USER_DELETE_NOT_ALLOWED`, `GROUP_MANAGEMENT_NOT_ALLOWED`) | Done |
| P13-D05 | Audit event design for user/group mutations (create/edit/disable/enable/reset/delete + denied escalations) | Done |
| P13-D06 | Management routes for session-visible routing + frontend navigation (user admin, group admin) | Done |

## Implementation tasks

| ID | Task | Acceptance | Status |
| --- | --- | --- | --- |
| P13-T01 | BusinessGroup first-class object: entity + repository + Flyway migration (`dimension`, `enabled`, logical delete/disable; immutable `groupCode`+`dimension`) | Migration applies; `groupCode`+`dimension` unique; immutability enforced; only display name editable | Done |
| P13-T02 | Group management Service + Controller (create/edit/disable/enable = GLOBAL_ADMIN only; GROUP_ADMIN read-only within authorized scope) | GROUP_ADMIN write → `GROUP_MANAGEMENT_NOT_ALLOWED` (403); reads scoped; audited | Done |
| P13-T03 | User management Service + Controller (create/edit/assign roles/assign group scope/disable/enable/reset-password/logical-delete) | Full lifecycle persisted; no plaintext password; logical delete preserves audit | Done |
| P13-T04 | GROUP_ADMIN fail-closed escalation guard (scope ⊆ own; forbid GLOBAL_ADMIN/AUDIT_ADMIN/GROUP_ADMIN assignment; logical delete GLOBAL-only) | Denials return correct 403 codes; no privilege widening; covered by tests | Done |
| P13-T05 | Audit events for all user/group mutations + denied escalations | Each mutating/denied action produces an audit record | Done |
| P13-T06 | Management-plane error codes wired into unified envelope (`NOT_FOUND`/`CONFLICT` management-only + 403 escalation codes) with English messageKeys | Envelope/error model matches OpenAPI + i18n constitution | Done |
| P13-T07 | Frontend user-management console (Vue3+TS+Element Plus+Pinia): list/create/edit/role/scope/disable/enable/reset/delete | English-first i18n keys; role-gated; fail-closed no-access feedback | Done |
| P13-T08 | Frontend group-management console (list/create/edit/disable/enable) | GLOBAL-only write controls hidden/disabled for others; English-first i18n | Done |
| P13-T09 | Session-visible management routes + navigation entries (user admin, group admin) | Routes gated by role; forbidden shows unified no-access view | Done |
| P13-T10 | Integration + unit tests (scope subset, forbidden role assignment, delete-not-allowed, group-management-not-allowed, NOT_FOUND/CONFLICT, immutability) | All green; coverage ≥ thresholds for security-critical paths | Done |

## Management API (local plane) — prefix `/api/management/v1`

Unified envelope (`metadata`/`result`/`error`). All require Bearer JWT.

| Method | Path | Auth |
| --- | --- | --- |
| GET | `/users` | GLOBAL_ADMIN; GROUP_ADMIN scoped |
| POST | `/users` | GLOBAL_ADMIN; GROUP_ADMIN within scope (restricted roles) |
| GET | `/users/{id}` | GLOBAL_ADMIN; GROUP_ADMIN scoped |
| PUT | `/users/{id}` | GLOBAL_ADMIN; GROUP_ADMIN within scope (restricted roles) |
| POST | `/users/{id}/disable` | GLOBAL_ADMIN; GROUP_ADMIN within scope |
| POST | `/users/{id}/enable` | GLOBAL_ADMIN; GROUP_ADMIN within scope |
| POST | `/users/{id}/reset-password` | GLOBAL_ADMIN; GROUP_ADMIN within scope |
| DELETE | `/users/{id}` | GLOBAL_ADMIN only (logical delete) |
| GET | `/groups` | GLOBAL_ADMIN; GROUP_ADMIN read-only scoped |
| POST | `/groups` | GLOBAL_ADMIN only |
| GET | `/groups/{id}` | GLOBAL_ADMIN; GROUP_ADMIN read-only scoped |
| PUT | `/groups/{id}` | GLOBAL_ADMIN only (display name only) |
| POST | `/groups/{id}/disable` | GLOBAL_ADMIN only |
| POST | `/groups/{id}/enable` | GLOBAL_ADMIN only |

### Management-plane error codes

| Code | HTTP | Meaning |
| --- | --- | --- |
| `NOT_FOUND` | 404 | Target user/group does not exist (management plane only) |
| `CONFLICT` | 409 | Duplicate `username` or `groupCode`+`dimension` (management plane only) |
| `GROUP_SCOPE_OUT_OF_RANGE` | 403 | Requested group scope not ⊆ actor's authorized scope |
| `ROLE_ASSIGNMENT_NOT_ALLOWED` | 403 | GROUP_ADMIN attempted to assign GLOBAL_ADMIN/AUDIT_ADMIN/GROUP_ADMIN |
| `USER_DELETE_NOT_ALLOWED` | 403 | Non-GLOBAL_ADMIN attempted logical delete |
| `GROUP_MANAGEMENT_NOT_ALLOWED` | 403 | Non-GLOBAL_ADMIN attempted group create/edit/disable/enable |

## Exit checklist

- [x] BusinessGroup is a first-class persisted object with immutable `groupCode`+`dimension`
- [x] User/group management lifecycle works on real persistence (not in-memory/mock)
- [x] GROUP_ADMIN fail-closed escalation guard verified by tests (scope subset, role/delete/group denials)
- [x] Audit events recorded for every mutation and denied escalation
- [x] Management-plane error codes + English messageKeys conform to OpenAPI + i18n constitution
- [x] Role-gated user + group consoles with fail-closed no-access feedback
- [x] Backend `mvn -B -ntp -f backend/pom.xml verify` green (Tests run: 114, Failures: 0, Errors: 0, Skipped: 1; JaCoCo all checks met; Checkstyle 0; SpotBugs clean — 2026-06-23)
- [x] Frontend `pnpm -C frontend lint` / `type-check` / `test` (24 files / 81 tests) / `build` green (2026-06-23)
- [x] Post-task doc sync complete; execution-sync-ledger evidence backfilled
- [x] Phase marked Done in master-plan.md only after the above
