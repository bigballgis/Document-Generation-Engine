# P1 — Login & Session (Detailed Plan)

**Phase status:** Done  
**Depends on:** P0

## Behavior goal

A management user logs in with a local account (eight-digit employee ID format for
test users), receives an authenticated session with roles and authorized group scope,
and lands on a role-appropriate home page. Unauthorized routes are blocked without
leaking sensitive data.

## Design tasks

| ID | Task | Status |
| --- | --- | --- |
| P1-D01 | Management user entity + seeded users (username, name, email, password hash, roles, group scope) | Done |
| P1-D02 | Session model: LOCAL auth source, issue/expiry, role + group summary | Done |
| P1-D03 | Login/logout/session API contract (management plane, not OpenAPI v1) | Done |
| P1-D04 | Route visibility matrix: GLOBAL_ADMIN, GROUP_ADMIN, TEMPLATE_AUTHOR | Done |
| P1-D05 | Forbidden-route UX + security audit summary on deny | Done |

## Implementation tasks

| ID | Task | Acceptance | Status |
| --- | --- | --- | --- |
| P1-T01 | POST login, POST logout, GET session endpoints | Fail-closed; no plaintext password stored | Done |
| P1-T02 | Seed data migration for test users per PRD | At least one user per first-wave role | Done |
| P1-T03 | Frontend login form + session store (Pinia) | English i18n keys only | Done |
| P1-T04 | Role-aware landing routes + navigation guard | Forbidden shows unified no-access view | Done |
| P1-T05 | Integration tests: login success, bad password, session expiry, forbidden route | All green | Done |

## Management API (local plane)

| Method | Path | Auth |
| --- | --- | --- |
| POST | `/api/management/v1/auth/login` | Public |
| POST | `/api/management/v1/auth/logout` | Bearer JWT |
| GET | `/api/management/v1/auth/session` | Bearer JWT |

## Seeded test users (password: `ChangeMe123!`)

| Username | Role | Group scope | Default route |
| --- | --- | --- | --- |
| 10000001 | GLOBAL_ADMIN | All (`*`) | `route.global-governance-home` |
| 10000002 | GROUP_ADMIN | RETAIL, CORP | `route.group-governance-home` |
| 10000003 | TEMPLATE_AUTHOR | RETAIL | `route.template-authoring-home` |

## Frontend route mapping (transitional)

| Logical route key | URL path |
| --- | --- |
| `route.global-governance-home` | `/home/global-governance` |
| `route.group-governance-home` | `/home/group-governance` |
| `route.template-authoring-home` | `/home/template-authoring` |
| `route.api-policy-management` | `/home/api-policy` |
| `route.audit-console` | `/home/audit` |

## SSO extension seam

`AuthSource` enum reserves `LOCAL` only for this phase. Company SSO/OIDC integration
remains a documented extension boundary — not claimed as delivered.

## Exit checklist

- [x] No role-selection simulation as login source of truth
- [x] SSO extension seam documented but not claimed delivered
- [x] Phase marked Done in master-plan.md
