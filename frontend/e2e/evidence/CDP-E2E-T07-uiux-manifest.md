# CDP-E2E-T07 UIUX Evidence Manifest

**Task:** CD-E2E-T07 / BDD-CDP-APIPOL-001-002 -- API policy OUTPUT_POLICY edit -> impact confirm -> save + DEFAULT_ROUTE hard-block finding  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport (primary):** **1920x1080** (desktop-first; CD-2 API policy slice)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080`  
**Placement:** ISOLATED `D:/working/DGE-cdp-e2e-t07-api-policy` / `feat/cdp-e2e-t07-api-policy`  
**Spec:** `frontend/e2e/CDP-E2E-T07-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T07-api-policy-edit-save.spec.ts` (Stage 6 -- 2 passed)  
**Verdict:** **PASS** (9/9 screenshots; REDBC + GREENBC; OUTPUT_POLICY confirm/save + hard-block reason/impact/advice; no Critical UIUX blockers)

## Capture method

Evidence captured via Playwright Chromium against Docker UI while walking Group Admin -> Template hub `?tab=apiAccess` -> Advanced settings OUTPUT_POLICY save (impact confirm) -> DEFAULT_ROUTE non-callable hard-block panel. Fixture: `ensureDemoFullFlowPublished` (`DEMO-FULL-FLOW-LETTER`). Brands via `switchBrand` (REDBC + GREENBC). Screenshots under `frontend/e2e/evidence/CDP-E2E-T07/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Group Admin (`10000002`) -- matches Stage 6 functional (handoff listed `10000001`; Group Admin is the API-policy edit role that passed Stage 6) |
| Brands | REDBC + GREENBC |
| Fixture | Demo Full-Flow published package |
| Surfaces in scope | External access / Advanced OUTPUT_POLICY + impact confirm dialog; DEFAULT_ROUTE hard-block finding (reason / impact / advice / error code) |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `CDP-E2E-T07-api-policy-edit-save.spec.ts --config playwright.docker.config.ts --workers=1` | **2/2 passed** (upstream handoff) |
| Stage 7: `pnpm -C frontend exec playwright test e2e/CDP-E2E-T07-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **2/2 passed** (~16.5s; 2026-07-11) |

## Screenshot inventory (9)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T07/screenshots/01-api-access-advanced-output-policy-redbc-1920x1080.png` | Advanced settings -- Output formats/modes + Save output settings | REDBC | 1920x1080 |
| 2 | `CDP-E2E-T07/screenshots/02-api-access-advanced-output-policy-greenbc-1920x1080.png` | External access shell after brand switch (Green Bank logo) | GREENBC | 1920x1080 |
| 3 | `CDP-E2E-T07/screenshots/03-output-policy-impact-confirm-redbc-1920x1080.png` | Confirm access change dialog (locator) | REDBC | 1920x1080 |
| 4 | `CDP-E2E-T07/screenshots/04-output-policy-impact-confirm-workspace-redbc-1920x1080.png` | Workspace + confirm dialog (INLINE mode staged) | REDBC | 1920x1080 |
| 5 | `CDP-E2E-T07/screenshots/05-output-policy-saved-version-redbc-1920x1080.png` | Post-save External access -- Settings version v4 + Red Bank logo | REDBC | 1920x1080 |
| 6 | `CDP-E2E-T07/screenshots/06-default-route-hard-block-finding-redbc-1920x1080.png` | Impact preview hard-block panel (reason/impact/advice/code) | REDBC | 1920x1080 |
| 7 | `CDP-E2E-T07/screenshots/07-default-route-hard-block-workspace-redbc-1920x1080.png` | Default route + hard-block in page context | REDBC | 1920x1080 |
| 8 | `CDP-E2E-T07/screenshots/08-default-route-hard-block-finding-greenbc-1920x1080.png` | Hard-block finding panel after GREENBC switch | GREENBC | 1920x1080 |
| 9 | `CDP-E2E-T07/screenshots/09-default-route-hard-block-workspace-greenbc-1920x1080.png` | Hard-block workspace -- Save AD groups brand-green | GREENBC | 1920x1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 02, 05 -- Green/Red Bank logo + brand switcher + Templates nav + contained package hub |
| OUTPUT_POLICY advanced + save confirm | **PASS** | 01, 03, 04 -- formats/modes + Confirm access change (REDBC OK CTA) |
| Post-save settings version | **PASS** | 05 -- Settings version v4 after OUTPUT_POLICY save |
| Hard-block finding (reason / impact / advice / error code) | **PASS** | 06-09 -- `DEFAULT_ROUTE_TARGET_UNAVAILABLE`; English copy complete |
| English-first i18n | **PASS** | All captured surfaces EN |
| No text overflow / overlap at 1920 | **PASS** | No clipping/overlap on dialog, finding panel, or shell frames |
| Tokens / no brand wash | **PASS** | Brand primary on CTAs / active nav; white OA baseline; hard-block uses danger tag |
| Entity display (no UUID primary) | **PASS** | Package title + external ID human-readable; no raw UUID columns |
| Dual-brand REDBC / GREENBC | **PASS** | REDBC 01/03-07; GREENBC 02/08/09 (logo + primary CTA color) |
| A11y spot check (hard-block / confirm) | **PASS** | Message-box title/actions; impact panel `aria-live`; finding labels present |

## Findings

### Critical (must fix before merge)

_None._

### Suggestion (should improve)

1. **Brand switch loses Advanced settings scroll (frame 02)** -- After `switchBrand(GREENBC)`, viewport returns to the top of External access (AD groups / route summary) instead of the Advanced OUTPUT_POLICY region shown in frame 01. Dual-brand logo is still proven on 02/05/09; for stricter parity, re-expand Advanced + scroll before GREENBC capture. _Rule: dual-brand evidence parity. Non-blocking._ Spec: `CDP-E2E-T07-uiux-evidence.spec.ts`.

2. **Domain Save CTA hierarchy inconsistency** -- On the same External access surface, "Save AD groups" uses brand primary while "Save output settings" / "Save retention" / disabled "Save default route" use secondary/info styles. Prefer one primary-per-section pattern. _Rule: button hierarchy. Non-blocking._ Components: `ApiPolicyAdGroupsDefaultRouteSections.vue`, `ApiPolicyAdvancedSettingsCard.vue`.

3. **Active External access tab underline remains Element Plus default blue under REDBC** -- Same pattern as T05/T06/T08; optional brand-token alignment. _Rule: token consistency. Non-blocking._

4. **No-warning OUTPUT_POLICY confirm is generic** -- Soft path without warnings shows "Apply advanced setting changes immediately?" rather than a structured impact summary (version delta / changed areas). Hard-block path already has the rich panel. Optional: surface a compact impact summary in the confirm dialog when `warnings.length === 0`. _Rule: state completeness / hierarchy. Non-blocking._ `useApiPolicyDomainEditorActions.ts`.

### Nice to have

1. Frame 01 is a mid-page crop (no shell) -- acceptable for Advanced focus; optional full-shell REDBC twin of frame 02.
2. Focus-visible ring evidence on Confirm OK / hard-block panel.
3. Capture toast "Save is blocked..." alongside hard-block panel (functional asserts it; not in screenshot inventory).

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (9) |
| OUTPUT_POLICY preview/confirm + save version | **PASS** |
| Hard-block reason / impact / advice UI | **PASS** |
| Dual-brand REDBC + GREENBC | **PASS** |
| Critical UIUX / a11y blockers on CDP T07 surfaces | **None** |
| **Overall stage 7 (T07)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T07-api-policy-edit-save.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T07-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/lifecycle-ui.ts`, `content-modules-api.ts`, `uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*` + `CDP-E2E-T07`)
- Components: `ApiPolicyImpactPreviewPanel.vue`, `ApiPolicyAdGroupsDefaultRouteSections.vue`, `apiPolicyImpactFindings.ts`
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`