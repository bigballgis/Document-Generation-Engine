# CE-U07 Functional Evidence Manifest — Clause outdated bump

**Task:** CE-U07 / Task Master **#82** — clause out-of-date badge + one-click bump + dashboard author todo  
**Slice:** `ce-u07-clause-outdated-bump` (`feat/ce-u07-clause-outdated-bump`)  
**BDD:** [docs/behavior/ce-u07-clause-outdated-bump.md](../../../docs/behavior/ce-u07-clause-outdated-bump.md) (`ready`)  
**Date:** 2026-07-15  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (healthz **UP**)  
**Verdict:** **PASS**

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U07-clause-outdated-bump.spec.ts` — BDD-CE-U07-COB-001/002 | **passed** |
| `CE-U07-clause-outdated-bump.spec.ts` — BDD-CE-U07-COB-004 | **passed** |
| `CE-U07-clause-outdated-bump-uiux-evidence.spec.ts` | **passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U07-clause-outdated-bump.spec.ts `
  e2e/CE-U07-clause-outdated-bump-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts
# 3 passed (7.7s)
```

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| COB-001 out-of-date badge | Spec 001/002 — `clause-reference-outdated-badge` visible |
| COB-002 one-click bump | Spec 001/002 — bump clears badge; pin shows `1.1.0` |
| COB-004 dashboard author todo deep link | Spec 004 — `#tasks-section` → Open → `/dev/{id}?…designTab=contentModules` + badge |

## Notes

1. Authors with Overview-only tabs still open the task hub via `#tasks-section` (`useDashboardTabs.forceTasksFromHash`).
2. Hash-only navigation after login refreshes author-workflow todos (`useDashboardDataLoader` watch) so fixtures created after first paint appear.
3. Backend container healthcheck may report unhealthy while `/healthz` is **200**; FE restarted with `--no-deps` when needed.
