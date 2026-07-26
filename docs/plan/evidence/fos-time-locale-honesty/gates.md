# Evidence — fos-time-locale-honesty (FOS-W5 / TM #175)

**Date:** 2026-07-26  
**Worktree:** `/home/ubuntu/DGE-fos-time-locale-honesty`  
**Branch:** `feat/fos-time-locale-honesty`

## Quality gates

| Gate | Result | Notes |
| --- | --- | --- |
| `pnpm -C frontend lint` | PASS | |
| `pnpm -C frontend type-check` | PASS | |
| `pnpm -C frontend test` | PASS | 293 files / 1765 tests |
| `pnpm -C frontend build` | PASS | |
| Backend `mvn verify` | PASS | includes MessageResolverZhCnTest |
| Architecture review | PASS (inline) | locale honesty; Accept-Language uses existing Spring resolver |
| Docker deploy / E2E / UIUX | **BLOCKED** | 0 images on host |

## Scope

W5-1 localWallClockToUtcIso + picker format fix; W5-2 formatDateTime UTC+zone;
W5-3 ElConfigProvider; W5-4 messages_zh_CN + Accept-Language; W5-5 timeout formatDateTime.
