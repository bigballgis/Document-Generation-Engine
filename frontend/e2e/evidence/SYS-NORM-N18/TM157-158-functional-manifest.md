# SYS-NORM N18 + P-Q1 / TM #157 + #158 — Functional E2E evidence

| Field | Value |
| --- | --- |
| Spec | `frontend/e2e/SYS-NORM-N18-role-l1.spec.ts` |
| BDD | `docs/behavior/sys-norm-n18-role-l1.md` |
| Slice | `sys-norm-n18-role-l1` (`feat/sys-norm-n18-role-l1`) |
| Config | `playwright.docker.config.ts` (`:4173` + `:8080`) |
| Result | **5 passed / 0 failed** (2026-07-22) |
| Command | `pnpm exec playwright test e2e/SYS-NORM-N18-role-l1.spec.ts --config playwright.docker.config.ts --workers=1` |

## Coverage → artifacts

| Scenario | Assertion surface | Artifact |
| --- | --- | --- |
| BDD-N18-L1-001 / 003 / 004 | Created by = EntityLinkCell; username fallback; link `?q=` when identity permitted | `N18-L1-001-004-created-by-entity-link.png` |
| BDD-N18-L1-007 | Click Created by → Users management + `q` prefill; actor discoverable | `N18-L1-007-users-catalog-after-created-by.png` |
| BDD-N18-L1-002 | Mock list `createdByDisplayName` → label `Alice Author` | `N18-L1-002-display-name-label.png` |
| BDD-N18-L1-005 | Login session without `route.identity-administration` → plain text | `N18-L1-005-created-by-plain-text.png` |
| BDD-N18-L1-006 | Blank `createdByUsername` → `—`, no link | `N18-L1-006-empty-actor-em-dash.png` |
| BDD-N18-L1-008 / 012 | Role picker EN exactly `Document author` (no interim) | `N18-L1-008-role-picker-document-author-en.png` |
| BDD-N18-L1-009 | Role picker zh-CN exactly `文档作者` (no interim) | `N18-L1-009-role-picker-document-author-zh.png` |
| BDD-N18-L1-010 | Seed `10000003` session roles = `['DOCUMENT_AUTHOR']` | (API assertion in spec) |

## Out of E2E scope (docs / plan gates)

| Scenario | Why |
| --- | --- |
| BDD-N18-L1-011 | Checklist #3b/#5a, #53, CE-O02/#119 vetoes — plan/registry inspection (doc-keeper / verifier) |

## HTML report

`frontend/playwright-report/docker` (last docker-config run of this spec)
