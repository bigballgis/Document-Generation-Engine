# P21-T09 UIUX evidence manifest

| Item | Value |
| --- | --- |
| Task | P21-T09 Team-lead go-live journey |
| Viewport | 1440×900 desktop (REDBC) |
| Spec | `frontend/e2e/P21-T09-uiux-evidence.spec.ts` |
| Result | PASS (when stack running) |

## Screenshots

| File | Surface |
| --- | --- |
| `screenshots/01-dashboard-team-lead-journey-four-steps-redbc-1440x900.png` | Dashboard `#journey-section` — 4-step team-lead timeline |
| `screenshots/02-template-detail-team-lead-journey-timeline-redbc-1440x900.png` | Template detail `[data-journey-timeline]` above workflow banner |

## Checks

- GROUP_ADMIN session shows `#journey-section` with four steps and guidance.
- Detail page shows team-lead journey when `PENDING_RELEASE` + `publishTemplates`.
- No text overflow or overlap at 1440×900 on captured surfaces.
- Primary brand (REDBC) tokens applied via shared theme.
