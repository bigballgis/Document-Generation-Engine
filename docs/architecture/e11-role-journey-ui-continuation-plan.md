# E11 Role-Journey UI Continuation Plan

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Scope

Continue login-first role journeys for GLOBAL_ADMIN, GROUP_ADMIN, TEMPLATE_AUTHOR
after P1 login foundation.

## Route visibility baseline

| routeKey | GLOBAL_ADMIN | GROUP_ADMIN | TEMPLATE_AUTHOR |
| --- | --- | --- | --- |
| route.global-governance-home | Allow | Deny | Deny |
| route.group-governance-home | Allow | Allow | Deny |
| route.template-authoring-home | Allow | Allow | Allow |
| route.api-policy-management | Allow | Scoped | Deny |
| route.audit-console | Allow | Scoped | Deny |

## Tasks

| ID | Scope | Status |
| --- | --- | --- |
| E11-T01 | Map routeKey to Vue Router paths | Done |
| E11-T02 | Post-login landing resolver | Done |
| E11-T03 | Navigation guard + forbidden page | Done |

## Pending questions

- Final URL mapping table (requires user confirmation)
- TEMPLATE_AUTHOR vs 姣嶇増璁捐浜哄憳 landing merge rule

