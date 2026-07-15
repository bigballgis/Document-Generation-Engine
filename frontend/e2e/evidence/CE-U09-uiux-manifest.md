# CE-U09 UIUX Evidence Manifest — Master review reachability

**Task:** CE-U09 / Task Master **#84** — Hub review CTAs + dashboard approval deep link  
**Slice:** `ce-u09-master-review-reachability` (`feat/ce-u09-master-review-reachability`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-15  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (**UP**; CE-U09 worktree images)  
**Verdict:** **PASS** (no 🔴 Critical UIUX blockers; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional: `CE-U09-master-review-reachability.spec.ts` | **4/4 passed** (13.6s) |
| Stage 7 evidence: `CE-U09-master-review-reachability-uiux-evidence.spec.ts` | **2/2 passed** (14.4s) |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U09-master-review-reachability-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 2 passed (14.4s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-hub-submit-review-redbc-1920x1080.png` | REDBC | Hub DRAFT — Submit for review + journey CTA |
| 1b | `01b-brand-header-redbc-crop.png` | REDBC | Header brand crop |
| 2 | `02-hub-submit-review-greenbc-1920x1080.png` | GREENBC | Same Hub after brand switch |
| 2b | `02b-brand-header-greenbc-crop.png` | GREENBC | Header logo + Green Bank title |
| 3 | `03-hub-submit-dialog-redbc-1920x1080.png` | REDBC | Submit letterhead for review dialog |
| 4 | `04-hub-approve-reject-redbc-1920x1080.png` | REDBC | Hub PENDING — Approve / Reject rail |
| 5 | `05-hub-approve-reject-greenbc-1920x1080.png` | GREENBC | Same Hub decision rail |
| 6 | `06-dashboard-master-review-redbc-1920x1080.png` | REDBC | Masters to review / Review letterhead partition |
| 6b | `06b-master-review-partition-crop-redbc-1920x1080.png` | REDBC | Partition crop |
| 7 | `07-dashboard-master-review-greenbc-1920x1080.png` | GREENBC | Dashboard dual-brand |
| 8 | `08-approval-tab-deep-link-redbc-1920x1080.png` | REDBC | Revision approval tab after Open |
| 9 | `09-approval-tab-deep-link-greenbc-1920x1080.png` | GREENBC | Approval tab dual-brand |
| 9b | `09b-brand-header-greenbc-crop.png` | GREENBC | Header crop on approval |

Path prefix: `frontend/e2e/evidence/CE-U09/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC (#DB0011) + GREENBC (#00847F) @1920 | ✅ | 01–02, 04–05, 06–07, 08–09 + header crops |
| Logo switch (`BrandLogo` / header brand text) | ✅ | 01b, 02b, 09b |
| Hub Submit for review reachable in header actions | ✅ | 01, 02, 03 |
| Journey Primary CTA visible (`data-master-journey-cta`) | ✅ | 01, 02 |
| Hub Approve/Reject action rail on PENDING | ✅ | 04, 05 |
| Dashboard master-review partition + Open deep link | ✅ | 06, 06b, 07 |
| Approval tab active after deep link (`workspaceTab=approval`) | ✅ | 08, 09 |
| English-first i18n labels | ✅ | Spec assertions + screenshots |
| No text overflow / overlap @1920 | ✅ | Full-page + crops |
| Onboarding tour does not block brand switch / Open | ✅ | Spec dismiss helper |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | None | — |

## Notes

1. Helpers: `CE_U09_VIEWPORT` 1920×1080 + `captureCeU09Screenshot` / `captureCeU09LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-U09-master-review-reachability-uiux-evidence.spec.ts`.
3. Functional stage-6 evidence: `frontend/e2e/evidence/CE-U09-manifest.md`.
4. Also includes `docker-compose.prod.yml` healthcheck fix (`/dev/tcp` probe; Jammy image lacks wget).
5. No merge performed (stage 7 handoff only); **no-push** per session opt-out.
