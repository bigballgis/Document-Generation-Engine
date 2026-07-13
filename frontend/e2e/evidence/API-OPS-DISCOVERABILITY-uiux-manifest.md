# API-OPS-DISCOVERABILITY UIUX Evidence Manifest

**Task / slice:** `api-ops-discoverability` (Task Master **#52**; BDD-API-OPS-DISCOVERABILITY-001)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-14  
**Viewport:** **1440×900** (`API_OPS_DISCOVERABILITY_VIEWPORT` via `uiux-evidence.ts`)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (LIVE)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on overview summary/alerts, Hub External access tab, or AD-groups / published≠callable warnings)

## Test execution

| Command / method | Result |
| --- | --- |
| Stage 6: `pnpm exec playwright test e2e/API-OPS-DISCOVERABILITY.spec.ts --config playwright.docker.config.ts --workers=1` | **5/5 PASS** (upstream) |
| Stage 7: `pnpm exec playwright test e2e/API-OPS-DISCOVERABILITY-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 PASS** (~35 s capture) |
| `e2e/a11y-smoke.spec.ts` (same docker config) | **8/9 PASS** — 1 unrelated strict-mode failure on content-modules empty CTA (see 🟡) |

### Surface coverage

| BDD / focus | Surface | Evidence frames |
| --- | --- | --- |
| SCEN-AOD-06/07 | `/api/policies` — 3 summary cards + Attention items table; no catalog sprawl | `01`–`03`, `09` |
| Dual brand | REDBC ↔ GREENBC header + overview + Hub | `04`, `09`–`11` |
| SCEN-AOD-01 | PENDING_RELEASE Hub — External access tab present / selected | `05`, `11` |
| SCEN-AOD-13/14 | `ad-groups-not-configured-warning` + `published-vs-callable-hint` | `06`, `06b` (crops; below fold on full page — see notes) |
| Publish gate | `publish-gate-ad-groups-warning` on Approval → Publish readiness | `07`, `08` |

## Screenshot inventory (12)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `API-OPS-DISCOVERABILITY/screenshots/01-overview-summary-alerts-REDBC.png` | Overview — summary cards + alerts viewport | REDBC | en |
| 2 | `…/02-overview-summary-cards-REDBC.png` | API readiness summary section crop | REDBC | en |
| 3 | `…/03-overview-alerts-table-REDBC.png` | Attention items table crop | REDBC | en |
| 4 | `…/04-brand-header-REDBC.png` | Shell brand logo (Red Bank) | REDBC | en |
| 5 | `…/05-hub-external-access-warnings-REDBC.png` | Hub External access @ PENDING_RELEASE (route summary in viewport) | REDBC | en |
| 6 | `…/06-hub-ad-groups-warning-REDBC.png` | Warning crop — “Not yet runtime-callable” | REDBC | en |
| 7 | `…/06b-hub-published-vs-callable-hint-REDBC.png` | Info crop — published ≠ runtime-callable | REDBC | en |
| 8 | `…/07-publish-gate-ad-groups-warning-REDBC.png` | Approval publish readiness + AD warning | REDBC | en |
| 9 | `…/08-publish-gate-warning-crop-REDBC.png` | Publish-gate AD warning crop | REDBC | en |
| 10 | `…/09-overview-summary-alerts-GREENBC.png` | Overview dual-brand parity | GREENBC | en |
| 11 | `…/10-brand-header-GREENBC.png` | Shell brand logo (Green Bank) | GREENBC | en |
| 12 | `…/11-hub-external-access-warnings-GREENBC.png` | Hub External access tab @ GREENBC | GREENBC | en |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Overview density — 3 KPI cards + alerts only; monitoring copy; no “Published packages” catalog | ✅ | Frames 01–03, 09; section copy “monitoring only — it is not a template catalog”; no pagination on alerts |
| `AppPageLayout` fluid + shared `AppDataTable` / `EntityLinkCell` / `EmptyStatePanel` | ✅ | `ApiPolicyHomeView.vue`; package column = name + externalId subtitle (no raw UUID as primary) |
| English-first i18n (`apiPolicy.home.*`, `templates.policy.runtimeCallable.*`, `templates.publishGate.adGroups*`) | ✅ | All frames en; keys in `en.ts` |
| Hub External access tab on `PENDING_RELEASE` + `Awaiting go-live` | ✅ | Frames 05, 11 — tab selected; package status badge |
| AD groups / published≠callable warnings present | ✅ | Crops 06 / 06b / 08; functional asserts in Stage 6 |
| Dual-brand REDBC + GREENBC (logo + nav active tint) | ✅ | Frames 01 vs 09; headers 04 vs 10; Hub 05 vs 11 |
| No harmful overlap / clipping @ 1440×900 on new surfaces | ✅ with notes | Detail column ellipsis (🟡); Hub warnings below fold on first paint (🟡) |
| Tokens / CSS vars (summary counts `--text-primary`, muted descriptions) | ✅ | `ApiPolicyHomeView.vue` scoped SCSS |
| a11y smoke | ⚠️ Partial | 8/9; content-modules dual CTA strict-mode — **pre-existing / out of slice** |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Hub AD-groups warnings below the fold @ 1440×900** (`TemplateDetailApiAccessTab.vue`) — Full-page frames `05` / `11` show Route summary + “No explicit paths” empty state; the `ad-groups-not-configured-warning` and `published-vs-callable-hint` sit **below** that block and only appear in crops `06` / `06b`. For discoverability of P4 semantics, consider placing the warning pair **above** `RouteSummaryPanel`, or auto-scroll into view when `?tab=apiAccess` and AD groups are empty.  
   Rule: frontend-oa-design §State completeness / quality bar (critical guidance visible without scroll when space allows).

2. **Attention items Detail column truncates** (`ApiPolicyHomeView.vue` — Detail `min-width="240"`) — Frames `03` / `09` show ellipsis (“…has no authorized AD …”). Prefer `show-overflow-tooltip` (or slightly wider `min-width`) so the full English detail is reachable without opening the hub.  
   Rule: frontend-oa-design §Quality bar (no harmful text clipping); entity-display — Issue + Package already carry the primary signal.

3. **a11y-smoke content-modules strict-mode** (`a11y-smoke.spec.ts`) — `getByRole('button', { name: /new content module/i })` matches **two** primaries when empty state + header CTA coexist. Unrelated to this slice; harden locator with `.first()` / `getByTestId` in a follow-up.  
   Rule: e2e-uiux-reviewer a11y smoke green — residual only.

### 🟢 Nice to have

1. Evidence spec already dismisses onboarding tour before `switchBrand` — keep that pattern for Group Admin captures.
2. Optional: scroll Hub warning into view before full-page golden so frames `05`/`11` show both tab chrome and warnings in one audit shot.
3. zh-CN spot-check of summary card titles / “Missing authorized AD group” (en sufficient for this slice).

## Files for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/API-OPS-DISCOVERABILITY-uiux-evidence.spec.ts` | Stage 7 capture journeys |
| `frontend/e2e/helpers/uiux-evidence.ts` | `API_OPS_DISCOVERABILITY_*` helpers + `switchBrand` |
| `frontend/e2e/evidence/API-OPS-DISCOVERABILITY/screenshots/*.png` | Twelve golden frames |
| `frontend/e2e/evidence/API-OPS-DISCOVERABILITY-uiux-manifest.md` | This manifest |
| `frontend/src/views/api/ApiPolicyHomeView.vue` | Overview summary + alerts |
| `frontend/src/views/templates/detail/TemplateDetailApiAccessTab.vue` | Hub warnings |
| `frontend/src/views/templates/detail/TemplateDetailApprovalPublishPane.vue` | Publish-gate AD warning |

## Verdict rationale

Overview readiness cards, Attention items monitoring table (no catalog sprawl), PENDING_RELEASE External access tab, English-first copy, and dual-brand REDBC/GREENBC shells meet bank OA bar. AD-groups / published≠callable warnings are durable and cropped; notes are below-fold discoverability, Detail ellipsis, and an unrelated a11y-smoke locator flake — **not** merge blockers. Stage 6 functional **5/5 PASS** remains authoritative for behavior.
