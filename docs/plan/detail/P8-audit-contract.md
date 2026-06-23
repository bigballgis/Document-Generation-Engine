# P8 — Audit & Contract Visibility (Detailed Plan)

**Phase status:** Done | **Depends on:** P5, P6, P7

## Key tasks

| ID | Task | Status |
| --- | --- | --- |
| P8-T01 | Audit search/filter with role + group scope | Done |
| P8-T02 | Masked audit export | Done |
| P8-T03 | Audit console UI (English i18n) | Done |
| P8-T04 | API caller contract page (version diff, error codes, examples) | Done |

**Exit:** Audit admin and scoped admins can query/export; callers see authorized contract view.

**Backend evidence:** `AuditController`, Flyway V9, `AuditControllerTest`, enhanced `ContractResultView` / `CallableVersionsResultView`.

**Frontend evidence:** `AuditConsoleView`, `src/api/audit.ts`, Vitest coverage.
