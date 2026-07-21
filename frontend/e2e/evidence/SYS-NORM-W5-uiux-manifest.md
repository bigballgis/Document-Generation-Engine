# SYS-NORM-W5-UIUX Evidence Manifest — Six-role compression

**Task:** SYS-NORM Wave 5 / Task Master **#149** — Six-role compression (ADR-0070)  
**Slice:** `sys-norm-roles` (`feat/sys-norm-roles`)  
**Worktree:** `D:/working/DGE-sys-norm-roles`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-21  
**Viewport:** 1440×900 (standard)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (Critical = 0; dual-brand evidence complete; suggestions non-blocking)

## Surfaces checked

| # | Surface | Route / state | Brands |
| --- | --- | --- | --- |
| 1 | Create-user role picker — six roles; Document author interim; no retired labels | `/entitlement/users` → Create user → Roles | REDBC + GREENBC |
| 2 | DOCUMENT_AUTHOR dashboard + authoring journey | `/dashboard` + `?tab=workflow` | REDBC + GREENBC |
| 3 | Remapped GROUP_ADMIN (ex-approver) users admin + team-lead journey | `/entitlement/users` + workflow tab | REDBC + GREENBC |
| 4 | TEMPLATE_TESTER tasks queue + testing journey | `/dashboard?queue=TEST` + workflow tab | REDBC + GREENBC |
| 5 | Users admin shell density + English-first | `/entitlement/users` (GLOBAL_ADMIN) | REDBC |
| 6 | Session switch DOCUMENT_AUTHOR → GROUP_ADMIN (brand persist) | re-login + users | GREENBC |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `SYS-NORM-W5-roles.spec.ts` | **7/7 passed** (handoff) |
| Stage 7 evidence: `SYS-NORM-W5-uiux-evidence.spec.ts` | **6/6 passed** |
| `a11y-smoke.spec.ts` (combined run) | **9/9 passed** |
| Combined wall | **15 passed (4.0m)** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/SYS-NORM-W5-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 15 passed (4.0m)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-role-picker-redbc-1440x900.png` | REDBC | Create user + Roles dropdown open |
| 1b | `01b-role-picker-options-redbc-crop.png` | REDBC | Six role options crop |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `01-role-picker-greenbc-1440x900.png` | GREENBC | Create user dual-brand |
| 2b | `01b-role-picker-options-greenbc-crop.png` | GREENBC | Six options dual-brand |
| 2c | `01c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `02-document-author-dashboard-*-1440x900.png` | both | DOCUMENT_AUTHOR My tasks |
| 3b | `02b-brand-header-*-crop.png` | both | Brand header crops |
| 4 | `03-document-author-journey-*-1440x900.png` | both | Document authoring workflow (6 steps) |
| 4b | `03b-document-author-journey-*-crop.png` | both | Journey card crop |
| 5 | `04-group-admin-users-*-1440x900.png` | both | Remapped GROUP_ADMIN users admin |
| 5b | `04b-brand-header-*-crop.png` | both | Brand header crops |
| 6 | `05-group-admin-journey-*-1440x900.png` | both | Team-lead go-live workflow (4 steps) |
| 6b | `05b-group-admin-journey-*-crop.png` | both | Journey card crop |
| 7 | `06-tester-tasks-queue-*-1440x900.png` | both | Waiting on my testing queue |
| 7b | `06b-tester-tasks-*-crop.png` | both | Tasks section crop |
| 8 | `07-tester-journey-*-1440x900.png` | both | Template testing workflow (3 steps) |
| 8b | `07b-tester-journey-*-crop.png` | both | Journey card crop |
| 9 | `08-users-admin-shell-redbc-1440x900.png` | REDBC | Users admin shell density |
| 9b | `08b-users-page-header-redbc-crop.png` | REDBC | Page header crop |
| 10 | `09-session-switch-group-admin-greenbc-1440x900.png` | GREENBC | After re-login brand persist |
| 10b | `09b-brand-header-after-relogin-greenbc-crop.png` | GREENBC | Header after session switch |

Path prefix: `frontend/e2e/evidence/SYS-NORM-W5/screenshots/` (**34** files on disk)  
Spec: `frontend/e2e/SYS-NORM-W5-uiux-evidence.spec.ts`  
Helpers: `SYS_NORM_W5_VIEWPORT` + `captureSysNormW5Screenshot` in `frontend/e2e/helpers/uiux-evidence.ts`  
Manifest (this file): `frontend/e2e/evidence/SYS-NORM-W5-uiux-manifest.md`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Fluid layout (`--fluid`, no `__inner`) | ✅ | Spec assert on users / dashboards |
| Role picker = six assignable roles only | ✅ | 01b; count=6; Global/Group/Document author/Tester/Legal/Audit |
| Interim Document author label (not Template author) | ✅ | 01b — `Document author`; no `Template author` option |
| No retired roles in assignable UI | ✅ | Spec asserts TEMPLATE_APPROVER / MASTER_DESIGNER / TEMPLATE_AUTHOR absent |
| DOCUMENT_AUTHOR dashboard + 6-step authoring journey | ✅ | 02 / 03; no retired role strings |
| GROUP_ADMIN remapped journey (team-lead, 4 steps) | ✅ | 04 / 05; header shows Group Admin |
| TEMPLATE_TESTER queue + 3-step testing journey | ✅ | 06 / 07; Waiting on my testing selected |
| Dual-brand REDBC + GREENBC | ✅ | 01–07 pairs; `--brand-primary` asserted |
| Logo / brand header switch | ✅ | 01c Red Bank ↔ Green Bank |
| Session switch keeps GREENBC | ✅ | 09 / 09b |
| No horizontal page overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke (critical axe) | ✅ | **9/9** |
| English-first copy | ✅ | User management / Create user / journey titles / role labels |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Users table **Roles** tags are compact `el-tag` chips; at a glance they can under-read vs Display name / Authorized groups emphasis. Consider slightly stronger tag contrast or wrapping so multi-role rows remain scannable at 1440. | OA density / `UserManagementListSection` Roles column — not a merge blocker |
| 🟡 Suggestion | Stage 6 functional PNG `TM149-ROLE-010-document-author-dashboard.png` captured skeleton loaders; Stage 7 dual-brand set waits for content (02) — prefer content-ready captures in functional specs going forward. | Evidence timing — functional only |
| 🟢 Nice to have | Header locale control truncates to `Engli…` beside brand switcher on some sessions; full “English” still readable via control value — optional min-width polish. | Shell header locale select |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. i18n key `identity.roles.DOCUMENT_AUTHOR` = `Document author` (interim naming per ADR-0070 / BDD ROLE-013); UI does **not** append a literal “(interim label)” suffix — that phrase is BDD vocabulary.
2. Functional Stage 6 artifacts under `evidence/SYS-NORM-W5/TM149-ROLE-*.png` remain complementary; Stage 7 dual-brand set lives in `screenshots/`.
3. Filter-by-role uses `AppSearchSelect` with static six-role options (enum catalog) — acceptable; Create-user Roles uses native `el-select` multi.

## Stage 7 gate

**PASS** — ready for Stage 8 architecture-reviewer (no UIUX blockers for merge of #149).
