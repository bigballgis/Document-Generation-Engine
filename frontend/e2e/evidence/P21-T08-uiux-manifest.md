# P21-T08 UIUX evidence manifest

| Item | Value |
| --- | --- |
| Task | P21-T08 Approver journey frontend |
| Viewport | 1440×900 desktop (REDBC) |
| Spec | `frontend/e2e/P21-T08-uiux-evidence.spec.ts` |
| Result | PASS (when stack running) |

## Screenshots

| File | Surface |
| --- | --- |
| `screenshots/01-dashboard-template-approver-journey-three-steps-redbc-1440x900.png` | Dashboard `#journey-section` — 3-step approver timeline |
| `screenshots/02-template-detail-approver-journey-timeline-redbc-1440x900.png` | Template detail `[data-journey-timeline]` above workflow banner |

## Checks

- Approver-only session shows `#journey-section` with three steps and guidance.
- Detail page shows approver journey when `APPROVAL` + `PENDING_DECISION`.
- No text overflow or overlap at 1440×900 on captured surfaces.
- Primary brand (REDBC) tokens applied via shared theme.
