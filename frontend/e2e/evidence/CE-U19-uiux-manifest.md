# CE-U19 UIUX Evidence Manifest — Package Hub Dependencies tab (read-only)

**Task:** CE-U19 — Dependency read-only view (Package Hub Dependencies tab)  
**Slice:** `ce-u19-dependency-readonly-view` (`feat/ce-u19-dependency-readonly-view`)  
**Worktree:** `D:/working/DGE-ce-u19-dependency-readonly-view`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-17  
**Viewport:** 1920×1080 (desktop-first; CE series convention)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (Critical = 0; dual-brand Dependencies tab artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `CE-U19-dependency-readonly-view.spec.ts` | **11/11 passed** (handoff) |
| Stage 7 evidence: `CE-U19-dependency-readonly-view-uiux-evidence.spec.ts` | **2/2 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (Dependencies panel REDBC/GREENBC × published + empty draft) | **0 critical** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U19-dependency-readonly-view-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 11 passed (1.0m)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-dependencies-published-redbc-1920x1080.png` | REDBC | Hub Dependencies — published pin + clauses + release lines |
| 1b | `01b-dependencies-panel-crop-redbc-1920x1080.png` | REDBC | `[data-testid=template-dependencies-panel]` crop |
| 1c | `01c-master-pin-section-crop-redbc-1920x1080.png` | REDBC | Master revision / pinned section |
| 1d | `01d-clauses-section-crop-redbc-1920x1080.png` | REDBC | Clause versions table |
| 1e | `01e-release-lines-section-crop-redbc-1920x1080.png` | REDBC | Release lines summary (nav links) |
| 1f | `01f-brand-header-redbc-crop.png` | REDBC | Header — Red Bank logo (`RGB≈215,0,37`) |
| 2 | `02-dependencies-published-greenbc-1920x1080.png` | GREENBC | Published Dependencies dual-brand |
| 2b | `02b-dependencies-panel-crop-greenbc-1920x1080.png` | GREENBC | Panel crop |
| 2c | `02c-master-pin-section-crop-greenbc-1920x1080.png` | GREENBC | Master pin section |
| 2d | `02d-clauses-section-crop-greenbc-1920x1080.png` | GREENBC | Clauses section |
| 2e | `02e-release-lines-section-crop-greenbc-1920x1080.png` | GREENBC | Release lines (teal primary links) |
| 2f | `02f-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank logo (`RGB≈0,151,145`) |
| 3 | `03-dependencies-empty-draft-redbc-1920x1080.png` | REDBC | Empty draft — not pinned + anchors empty |
| 3b | `03b-dependencies-panel-crop-redbc-1920x1080.png` | REDBC | Panel crop |
| 3c | `03c-not-pinned-section-crop-redbc-1920x1080.png` | REDBC | Not pinned until publish |
| 3d | `03d-anchors-empty-crop-redbc-1920x1080.png` | REDBC | Anchors honest empty state |
| 3e | `03e-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 4 | `04-dependencies-empty-draft-greenbc-1920x1080.png` | GREENBC | Empty draft dual-brand |
| 4b | `04b-dependencies-panel-crop-greenbc-1920x1080.png` | GREENBC | Panel crop |
| 4c | `04c-not-pinned-section-crop-greenbc-1920x1080.png` | GREENBC | Not pinned section |
| 4d | `04d-anchors-empty-crop-greenbc-1920x1080.png` | GREENBC | Anchors empty |
| 4e | `04e-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |

Path prefix: `frontend/e2e/evidence/CE-U19/screenshots/` (**22** files on disk)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–02 published; 03–04 empty draft |
| Logo / brand header switch | ✅ | 01f Red Bank; 02f / 04e Green Bank; pixel brand hits on full pages |
| Dependencies tab shell (WorkspaceTabShell secondary tabs) | ✅ | Spec deep-link `?tab=dependencies` + panel visible |
| Master pin / not-pinned states | ✅ | 01c / 02c pinned; 03c / 04c not pinned |
| Anchors empty honest state | ✅ | 03d / 04d — EmptyStatePanel |
| Clause versions read-only table | ✅ | 01d / 02d; no upsert/bump CTAs (spec assert) |
| Release lines summary + brand-colored nav | ✅ | 01e / 02e primary-link pixels; no clone/abandon in panel |
| No horizontal page overflow @1920 | ✅ | Spec `assertNoViewportOverflow` on all four brand×state captures |
| a11y smoke + panel critical axe | ✅ | 9/9 + inline critical axe = 0 |
| English-first copy | ✅ | Dependencies / Master revision / Clause versions / Release lines / Not pinned until publish |
| Contained hub detail width | ✅ | Panel crops ~1440px within 1920 viewport (detail/workspace pattern) |
| Read-only (no write CTAs in panel) | ✅ | Spec `assertReadOnlyNoWriteCtas` |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Clause **Module** column labels with `row.moduleId` via `EntityLinkCell` (UUID primary text). BDD U19-D7 allows `moduleId`, but entity-display prefers human-readable name/code as label + id subtitle when API can supply it. | `TemplateDependenciesPanel.vue` clauses table — `frontend-entity-display` anti-pattern; not a data leak |
| 🟡 Suggestion | Pin **Revision id** shows full UUID (technical CE-K01 fingerprint). Acceptable for pin identity; consider monospace truncate + copy affordance if operators complain about wrap density. | `template-dependencies-pin-revision-id` — polish only |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Shared vocabulary used: `AppDataTable`, `EntityLinkCell`, `EmptyStatePanel`, `LoadErrorPanel`, `SectionPanelHeader` — no raw Element Plus table rebuild.
2. Master name resolved via `getMaster` → `EntityLinkCell` label (fallback `masterId` only on name load failure).
3. Brand primary verified: REDBC full-page red hits ≈264; GREENBC teal hits ≈268; header crops distinct logos.
4. Helpers: `CE_U19_VIEWPORT` 1920×1080 + `captureCeU19Screenshot` / `captureCeU19LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
5. Spec: `frontend/e2e/CE-U19-dependency-readonly-view-uiux-evidence.spec.ts`.
6. No merge / no new deploy / no product Done claim (stage 7 handoff only).

## Next

**Stage 8 — `architecture-reviewer`**
