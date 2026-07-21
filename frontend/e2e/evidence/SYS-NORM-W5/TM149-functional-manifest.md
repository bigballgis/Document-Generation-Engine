# SYS-NORM Wave 5 / TM #149 — Functional E2E evidence

| Field | Value |
| --- | --- |
| Spec | `frontend/e2e/SYS-NORM-W5-roles.spec.ts` |
| BDD | `docs/behavior/sys-norm-roles.md` |
| Config | `playwright.docker.config.ts` (`:4173` + `:8080`) |
| Result | **7 passed / 0 failed** (2026-07-21) |
| Command | `pnpm exec playwright test e2e/SYS-NORM-W5-roles.spec.ts --config playwright.docker.config.ts --workers=1` |

## Coverage → artifacts

| Scenario | Assertion surface | Artifact |
| --- | --- | --- |
| ROLE-010 / 016 | DOCUMENT_AUTHOR seeds (10000003/05) session + dashboard | `TM149-ROLE-010-document-author-dashboard.png` |
| ROLE-011 / 016 | ex-approver 10000007 → GROUP_ADMIN + users admin | `TM149-ROLE-011-ex-approver-group-admin-users.png` |
| ROLE-004 / 016 | LEGAL_REVIEWER / AUDIT_ADMIN unchanged | (API assertion in spec) |
| ROLE-012 / 013 | Create-user role picker = six roles; interim Document author | `TM149-ROLE-012-role-picker-six-roles.png` |
| ROLE-014 | Author / group-admin / tester workflow journeys | `TM149-ROLE-014-*-journey.png` |
| ROLE-003 | DOCUMENT_AUTHOR `test-decision` → 403 | `TM149-ROLE-003-author-decideTests-403.json` |
| ROLE-005 | Retired roles → 422 `ROLE_NOT_ASSIGNABLE` | `TM149-ROLE-005-retired-role-not-assignable.json` |
| ROLE-015 | GROUP_ADMIN Approve CTA + complete; author no Approve | `TM149-ROLE-015-*.png` + `TM149-ROLE-015-approval-result.json` |

## Out of E2E scope (BE/docs gates)

ROLE-001/002/006–009/017/018 — migration/matrix/OpenAPI/governance (covered by verify + doc-keeper).
