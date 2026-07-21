# SYS-NORM Wave 3 — E2E functional evidence

| Field | Value |
| --- | --- |
| Slice | `sys-norm-external-ops` (TM #147) |
| BDD | `docs/behavior/sys-norm-external-ops.md` |
| Spec | `frontend/e2e/SYS-NORM-W3-external-ops.spec.ts` |
| Config | `playwright.docker.config.ts` (baseURL `:4173`) |
| Result | **10 passed / 0 failed** (2026-07-21) |
| Report | `frontend/playwright-report/docker` |

## Scenario coverage

| Scenario | Test | Evidence PNG |
| --- | --- | --- |
| W3-001/003/016 | dashboard readiness + ops cards; not catalog | `SYS-NORM-W3-001-dashboard-ops.png` |
| W3-002 | alert → package settings | `SYS-NORM-W3-002-alert-to-settings.png` |
| W3-004/005/007 | invocations page + filters | `SYS-NORM-W3-004-invocations-page.png` |
| W3-006/016 | detail summary-only (when rows exist) | `SYS-NORM-W3-006-invocation-detail.png` |
| W3-008/009/015 | settings complete; no interim banner | `SYS-NORM-W3-008-package-settings.png` |
| W3-010 | unknown panel fail-closed | `SYS-NORM-W3-010-unknown-panel.png` |
| W3-011 | legacy redirects → settings | `SYS-NORM-W3-011-legacy-redirect-settings.png` |
| W3-012 | nav overview + invocations | `SYS-NORM-W3-012-nav-membership.png` |
| W3-013 | capability fail-closed | `SYS-NORM-W3-013-fail-closed.png` |
| W3-014 | GROUP_ADMIN CORP scope | `SYS-NORM-W3-014-group-scope.png` |

Not asserted in this functional suite (docs/process or UIUX stage): **W3-017** (i18n keys), **W3-018** (out-of-scope waves not claimed).

## Known gap (route to frontend-engineer)

`frontend/nginx.conf` has SPA `try_files` for `/api/policies` and `/api/packages`, but **not** `/api/invocations`. Hard refresh / `page.goto('/api/invocations')` hits the `/api/` backend proxy (JSON), not the Vue route. In-app navigation (nav click / `router.push`) works. Fail-closed for invocations was verified via in-app `$router.push`. Recommend adding:

```nginx
location = /api/invocations {
    try_files $uri $uri/ /index.html;
}
location ^~ /api/invocations/ {
    try_files $uri $uri/ /index.html;
}
```

Then redeploy frontend image for hard-refresh parity.
