# Evidence — fos-author-can-start (FOS-W2 / TM #172)

**Date:** 2026-07-26  
**Worktree:** `/home/ubuntu/DGE-fos-author-can-start`  
**Branch:** `feat/fos-author-can-start`

## Quality gates

| Gate | Result | Notes |
| --- | --- | --- |
| `pnpm -C frontend lint` | PASS | |
| `pnpm -C frontend type-check` | PASS | |
| `pnpm -C frontend test` | PASS | 286 files / 1745 tests |
| `pnpm -C frontend build` | PASS | |
| Backend `mvn verify` | N/A | FE-only leaf |
| Docker / E2E / UIUX | **BLOCKED** | host has no `docker` / `pwsh` |

## Scope

W2-1…W2-8: create dialog APPROVED fetch; lifecycle escapes guide; guide order variables→bindings; panel=routes; API settings breadcrumb; anchorId warn; single dismiss; workspace tab dirty leave.
