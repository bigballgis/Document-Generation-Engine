# CDP-E2E-T13 UIUX Evidence Manifest

**Task:** CD-E2E-T13 / BDD package materialize S1–S3 — Hub External access route summary + default-route impact preview  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport (primary):** **1920×1080** (desktop-first; CD-2 materialize / default-route slice)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080` (healthz/4173 **200**)  
**Placement:** ISOLATED `D:/working/DGE-cdp-e2e-t13-materialize` / `feat/cdp-e2e-t13-materialize`  
**Spec:** `frontend/e2e/CDP-E2E-T13-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T13-api-package-materialize.spec.ts` (Stage 6 — 3/3)  
**Verdict:** **PASS** (5/5 screenshots; REDBC @1920 Hub route summary + impact preview + after-change; bank OA OK; no Critical UIUX blockers)

## Capture method

GROUP_ADMIN → isolated template → publish `1.0.0` + second release `2.0.0` → `/templates/{id}?tab=apiAccess` → Route summary (S1: default still `1.0.0`) → change Default route to `2.0.0` → impact-preview confirm → reload after save (S3). Brand: REDBC via `switchBrand`. Screenshots under `frontend/e2e/evidence/CDP-E2E-T13/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Group Admin (`E2E_GROUP_ADMIN`) |
| Brands | REDBC (required); GREENBC optional — see Dual-brand note |
| Fixture | `createIsolatedTemplatePendingRelease` + `publishTemplateRelease` + `publishSecondReleaseFromClone` |
| Surfaces in scope | Hub External access tab — `RouteSummaryPanel`, Default route L1, impact-preview message-box |
| Cross-ref | [P13-EXTERNAL-SERVICES-uiux-manifest.md](./P13-EXTERNAL-SERVICES-uiux-manifest.md) (hub @1440 dual-brand); this manifest is CD-2 @1920 S1/S3 closeout |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `… CDP-E2E-T13-api-package-materialize.spec.ts --config playwright.docker.config.ts --workers=1` | **3/3 passed** (upstream) |
| Stage 7: `pnpm -C frontend exec playwright test e2e/CDP-E2E-T13-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (upstream; 5 screenshots @04:44) |
| Stage 7 visual review | Screenshots 01–05 inspected @1920 REDBC; stack **200** on `:4173` / `:8080`; no re-run required (artifacts fresh) |

## Screenshot inventory (5)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T13/screenshots/01-hub-api-access-route-summary-redbc-1920x1080.png` | Hub External access — package shell + Route summary after 1.0.0 + 2.0.0 publish | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T13/screenshots/02-route-summary-panel-detail-redbc-1920x1080.png` | Route summary panel detail — Default release **1.0.0** + explicit paths | REDBC | crop |
| 3 | `CDP-E2E-T13/screenshots/03-default-route-impact-preview-redbc-1920x1080.png` | Default route L1 (1.0.0 → 2.0.0) + impact-preview overlay | REDBC | 1920×1080 |
| 4 | `CDP-E2E-T13/screenshots/04-default-route-impact-preview-dialog-redbc-1920x1080.png` | Confirm access change — “Default route target will change” | REDBC | crop |
| 5 | `CDP-E2E-T13/screenshots/05-default-route-after-change-2-0-0-redbc-1920x1080.png` | After confirm + reload — Settings version **v2**; External access tab | REDBC | 1920×1080 |

## BDD traceability

| Scenario | Requirement | Evidence |
| --- | --- | --- |
| **S1** | Hub route summary after first/second publish; default still prior release | Frames **01–02** — Default generate path `/…/default/generate`; Default release badge **1.0.0**; explicit paths for **2.0.0** and **1.0.0** |
| **S3** | Impact preview + default route change | Frames **03–04** — Save default route → Confirm access change / “Default route target will change”; frame **05** — Settings version **v2** after save (functional assert `#policy-domain-DEFAULT_ROUTE_TARGET` contains `2.0.0`) |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01/05 — Red Bank logo + brand switcher + Templates active; contained package workspace |
| Route summary (S1) | **PASS** | 02 — Package external ID, default generate path, Default release **1.0.0** badge, explicit generate paths table |
| Default route L1 + Save | **PASS** | 03 — Current **1.0.0**, input **2.0.0**, primary “Save default route” (brand red) |
| Impact preview confirm (S3) | **PASS** | 04 — “Confirm access change” + warning copy + Cancel / OK |
| After-change persistence | **PASS** | 05 — Settings version **v2** (was v1 in 01/03); Live package + External access tab |
| English-first i18n | **PASS** | Route summary, External access, Confirm access change, Save default route — EN chrome |
| No text overflow / overlap at 1920 | **PASS** | No clipping on route paths, version table, or dialog actions (dialog position note → 🟡) |
| Density / spacing rhythm | **PASS** | Moderate OA density; white baseline; clear Version lines → tabs → Route summary → L1 sections |
| Tokens / no brand wash | **PASS** | Brand red on Create modification / Save AD groups / Save default route / OK; white OA baseline |
| Entity display (no UUID primary) | **PASS** | External ID `E2E-T13-MRFELKYJ` human-readable; package title uses fixture name |
| Dual-brand REDBC / GREENBC | **PASS (REDBC)** | REDBC required and captured; GREENBC **not required** for T13 — see note |
| A11y spot check (route + dialog) | **PASS** | Route summary headings; Default route labeled input; dialog title + Cancel / OK; contrast OK on white baseline |

## Dual-brand note (honest scope)

- **T13 surface** = Hub External access materialize / default-route journey (S1/S3), not a dual-brand golden slice.
- **Precedent:** CD-E2E-T09 / T11 captured **REDBC-only** @1920; **T12** owns REDBC+GREENBC golden.
- **Prior hub dual-brand:** P13 already captured Hub External access REDBC + GREENBC @1440 (`P13-EXTERNAL-SERVICES-uiux-manifest.md` frames 01–03).
- **Judgment:** REDBC-only for CDP-E2E-T13 is **acceptable and consistent**; GREENBC re-capture of this journey is **optional / nice-to-have**, not a stage-7 blocker.

## Stable selectors (functional + UIUX)

| Selector | Purpose |
| --- | --- |
| `?tab=apiAccess` / External access tab | Hub External access surface |
| `data-testid="route-summary-panel"` | `RouteSummaryPanel` root |
| `.path-value` (within route summary) | Default / explicit generate path text |
| `#policy-domain-DEFAULT_ROUTE_TARGET` | Default route L1 section |
| `button` name `/save default route\|保存默认路由/i` | Save default route |
| `.el-message-box` + `/confirm access change/i` | Impact-preview confirm |
| `.el-message` + `/access setting saved\|访问设置已保存/i` | Post-save toast |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Impact-preview message-box position (frame 03)** — Full-page shot shows the confirm overlay biased toward the left edge of the viewport rather than a clear centered modal (same Element Plus message-box pattern noted on T11 Export confirm). Frame 04 proves title/warning/actions are readable and usable. Optional: ensure message-box uses standard centered overlay so Confirm access change does not visually collide with nav. _Non-blocking._ Components: `useApiPolicyDomainEditorActions.ts` + Element Plus `ElMessageBox`.

2. **Post-change Route summary detail crop missing (frame 05)** — After-change evidence is Settings version **v2** + functional assert on `#policy-domain-DEFAULT_ROUTE_TARGET` containing `2.0.0`. Optional: add a locator screenshot of `route-summary-panel` after reload showing Default release **2.0.0** badge for stronger S3 visual closeout. _Non-blocking._ Spec: `CDP-E2E-T13-uiux-evidence.spec.ts`.

### 🟢 Nice to have

1. GREENBC capture of Hub External access + impact dialog (optional; P13 already covers hub dual-brand @1440; T12 owns CD-2 dual-brand golden).
2. Frame 03 scrolls to Default route L1 — Route summary above fold is partially out of view; acceptable for S3 focus.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (5) |
| Hub route summary after publish (S1) | **PASS** (frames 01–02) |
| Impact preview + default change (S3) | **PASS** (frames 03–05) |
| Dual-brand REDBC (GREENBC optional) | **PASS** — REDBC captured; GREENBC N/A for T13 (same as T09/T11) |
| Critical UIUX / a11y blockers on CDP T13 surfaces | **None** |
| **Overall stage 7 (T13)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T13-api-package-materialize.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T13-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*`, task id `CDP-E2E-T13`), `content-modules-api.ts`
- Views/components: `RouteSummaryPanel.vue`, `ApiPolicyAdGroupsDefaultRouteSections.vue`, `useApiPolicyDomainEditorActions.ts`
- Prior hub UIUX: `frontend/e2e/evidence/P13-EXTERNAL-SERVICES-uiux-manifest.md`
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
